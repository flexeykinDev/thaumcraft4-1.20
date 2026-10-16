package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blockentity.EssentiaBlockEntity;
import thaumcraft.common.items.MirrorItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Магическое зеркало и зеркало эссенции — перенос BlockMirror, TileMirror и TileMirrorEssentia оригинала. Зеркала
 * связываются попарно (см. MirrorItem). Предмет, коснувшийся связанного магического зеркала, выпадает из его пары,
 * пусть даже в другом мире; частая переброска раскачивает зеркала, и тогда они отдают вещи всё медленнее, пока
 * не успокоятся сами или не выпьют порядок из сети вис. Зеркало эссенции даёт инфузионному алтарю брать эссенцию из
 * банок, стоящих перед его парой.
 */
public class MirrorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final boolean essentia;

    public MirrorBlock(boolean essentia, Properties properties) {
        super(properties);
        this.essentia = essentia;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public boolean isEssentia() {
        return essentia;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** Зеркало висит на прочной грани соседнего блока, позади себя. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos from) {
        return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, from);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> Block.box(0, 15, 0, 16, 16, 16);
            case UP -> Block.box(0, 0, 0, 16, 1, 16);
            case NORTH -> Block.box(0, 0, 15, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 1);
            case WEST -> Block.box(15, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 1, 16, 16);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // предмет должен оказаться в самом зеркале: тот, что лишь касается его краем (например, только что выпавший
        // из него и легший рядом), не забирается — иначе вещь бесконечно скакала бы между зеркалами
        if (!essentia && !level.isClientSide && entity instanceof ItemEntity item && item.isAlive() && !item.isOnPortalCooldown()
                && BlockPos.containing(item.getBoundingBox().getCenter()).equals(pos) && level.getBlockEntity(pos) instanceof Mirror mirror) {
            mirror.transport(item);
        }
    }

    /** Связанное зеркало, если его сломать, выпадает связанным и разрывает связь у пары. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Mirror mirror && mirror.linked) {
            MirrorItem.setLink(drop, mirror.linkPos, mirror.linkDim);
            mirror.invalidateLink();
        }
        return List.of(drop);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    /** Чёрная дымка у зеркала, сквозь которое прошёл предмет. */
    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id != 1) {
            return super.triggerEvent(state, level, pos, id, param);
        }
        if (level.isClientSide) {
            Direction face = state.getValue(FACING);
            double x = pos.getX() + 0.33 + level.random.nextFloat() * 0.33f - face.getStepX() / 2.0;
            double y = pos.getY() + 0.33 + level.random.nextFloat() * 0.33f - face.getStepY() / 2.0;
            double z = pos.getZ() + 0.33 + level.random.nextFloat() * 0.33f - face.getStepZ() / 2.0;
            level.addParticle(ParticleTypes.ENTITY_EFFECT, x, y, z, 0.0, 0.0, 0.0);
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Mirror(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MIRROR.get(), Mirror::tick);
    }

    public static class Mirror extends BlockEntity {
        public boolean linked;
        public BlockPos linkPos = BlockPos.ZERO;
        public ResourceLocation linkDim = Level.OVERWORLD.location();
        public int instability;
        private int count;
        private int inc = 40;
        private final List<ItemStack> outputStacks = new ArrayList<>();
        private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (stack.isEmpty() || essentia()) {
                    return stack;
                }
                if (!simulate) {
                    insert(stack.copy());
                }
                return ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return !essentia();
            }
        });

        public Mirror(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MIRROR.get(), pos, state);
        }

        public boolean essentia() {
            return getBlockState().getBlock() instanceof MirrorBlock block && block.essentia;
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        // ---------- связь ----------

        @Nullable
        private ServerLevel targetLevel() {
            if (!(level instanceof ServerLevel server)) {
                return null;
            }
            return server.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, linkDim));
        }

        /** Зеркало-пара того же рода, если оно стоит там, куда указывает связь. */
        @Nullable
        private Mirror target() {
            ServerLevel target = targetLevel();
            if (target == null || !target.isInWorldBounds(linkPos)) {
                return null;
            }
            return target.getBlockEntity(linkPos) instanceof Mirror mirror && mirror.essentia() == essentia() ? mirror : null;
        }

        private boolean pointsAt(Mirror other) {
            return other.linkPos.equals(worldPosition) && other.linkDim.equals(level.dimension().location());
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        /** isLinkValid: пара на месте и смотрит на нас; иначе связь у этого зеркала гаснет. */
        public boolean isLinkValid() {
            if (!linked) {
                return false;
            }
            Mirror other = target();
            if (other != null && other.linked && pointsAt(other)) {
                return true;
            }
            if (other != null || targetLevel() != null) {
                linked = false;
                sync();
            }
            return false;
        }

        private boolean isLinkValidSimple() {
            if (!linked) {
                return false;
            }
            Mirror other = target();
            return other != null && other.linked && pointsAt(other);
        }

        /** restoreLink: если пара свободна, зеркала снова связываются друг с другом. */
        public void restoreLink() {
            Mirror other = target();
            if (other == null) {
                return;
            }
            if (!other.isLinkValid()) {
                other.linked = true;
                other.linkPos = worldPosition;
                other.linkDim = level.dimension().location();
                other.sync();
                linked = true;
                sync();
            }
        }

        public void invalidateLink() {
            ServerLevel target = targetLevel();
            if (target != null && target.isLoaded(linkPos) && target.getBlockEntity(linkPos) instanceof Mirror other) {
                other.linked = false;
                other.sync();
            }
        }

        // ---------- предметы ----------

        void transport(ItemEntity item) {
            if (!linked || !isLinkValid()) {
                return;
            }
            Mirror other = target();
            if (other == null) {
                return;
            }
            other.addStack(item.getItem().copy());
            addInstability(null, item.getItem().getCount());
            item.discard();
            setChanged();
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        }

        /** Вещь из воронки уходит в пару; если пары нет — падает перед зеркалом. */
        private void insert(ItemStack stack) {
            Mirror other = linked ? target() : null;
            if (other != null) {
                other.addStack(stack);
                addInstability(null, stack.getCount());
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            } else {
                spawnItem(stack);
            }
        }

        /** Сколько вещей ждёт, пока зеркало их отдаст. */
        public int pending() {
            return outputStacks.stream().mapToInt(ItemStack::getCount).sum();
        }

        void addStack(ItemStack stack) {
            outputStacks.add(stack);
            setChanged();
        }

        private void eject() {
            if (outputStacks.isEmpty() || count <= 20) {
                return;
            }
            int index = level.random.nextInt(outputStacks.size());
            ItemStack stack = outputStacks.get(index);
            if (stack.isEmpty()) {
                outputStacks.remove(index);
                return;
            }
            ItemStack one = stack.copyWithCount(1);
            if (spawnItem(one)) {
                stack.shrink(1);
                addInstability(null, 1);
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
                if (stack.isEmpty()) {
                    outputStacks.remove(index);
                }
                setChanged();
            }
        }

        private boolean spawnItem(ItemStack stack) {
            Direction face = facing();
            ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5 - face.getStepX() * 0.3, worldPosition.getY() + 0.5
                    - face.getStepY() * 0.3, worldPosition.getZ() + 0.5 - face.getStepZ() * 0.3, stack);
            item.setDeltaMovement(face.getStepX() * 0.15, face.getStepY() * 0.15, face.getStepZ() * 0.15);
            item.setPortalCooldown(20);
            return level.addFreshEntity(item);
        }

        /** Отдать вещь из ручного зеркала: она сразу выпадает перед этим зеркалом. */
        public void receiveFromHand(ItemStack stack) {
            spawnItem(stack);
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        }

        private void addInstability(@Nullable ServerLevel targetLevel, int amount) {
            instability += amount;
            if (targetLevel != null && targetLevel.getBlockEntity(linkPos) instanceof Mirror other) {
                other.instability = Math.max(0, other.instability + amount);
                other.setChanged();
            }
        }

        private void checkInstability() {
            if (instability > 0 && count % 20 == 0) {
                instability--;
                sync();
            }
            if (instability > 0) {
                int drained = VisNetHandler.drainVis(level, worldPosition, Aspect.ORDER, Math.min(instability, 1));
                if (drained > 0) {
                    addInstability(targetLevel(), -drained);
                }
            }
        }

        static void tick(Level level, BlockPos pos, BlockState state, Mirror mirror) {
            if (!mirror.essentia()) {
                int rate = mirror.instability / 50;
                if (rate == 0 || mirror.count % (rate * rate) == 0) {
                    mirror.eject();
                }
                mirror.checkInstability();
            }
            if (mirror.count++ % mirror.inc == 0) {
                if (!mirror.isLinkValidSimple()) {
                    if (mirror.inc < 600) {
                        mirror.inc += 20;
                    }
                    mirror.restoreLink();
                } else {
                    mirror.inc = 40;
                }
            }
        }

        // ---------- эссенция ----------

        /**
         * Зеркало эссенции как источник для алтаря: берёт единицу из банок перед своей парой — в восьми блоках впереди
         * неё и в восьми в стороны (EssentiaHandler.drainEssentia оригинала).
         */
        public boolean takeEssentia(Aspect aspect) {
            if (!essentia() || !isLinkValid()) {
                return false;
            }
            Mirror other = target();
            if (other == null || !(other.level instanceof ServerLevel server)) {
                return false;
            }
            Direction direction = other.facing();
            for (int aa = -8; aa <= 8; aa++) {
                for (int bb = -8; bb <= 8; bb++) {
                    for (int cc = 0; cc < 8; cc++) {
                        if (aa == 0 && bb == 0 && cc == 0) {
                            continue;
                        }
                        BlockPos at = switch (direction.getAxis()) {
                            case Y -> other.worldPosition.offset(aa, cc * direction.getStepY(), bb);
                            case Z -> other.worldPosition.offset(aa, bb, cc * direction.getStepZ());
                            case X -> other.worldPosition.offset(cc * direction.getStepX(), aa, bb);
                        };
                        net.minecraft.world.level.block.entity.BlockEntity source = server.getBlockEntity(at);
                        if (source instanceof EssentiaBlockEntity jar && jar.isJar() && jar.take(aspect, 1)
                                || source instanceof EssentiaReservoirBlock.Reservoir reservoir && reservoir.take(aspect, 1)) {
                            FxPacket.send(server, FxPacket.ESSENTIA, Vec3.atCenterOf(at), Vec3.atCenterOf(other.worldPosition), aspect.getColor(), 0);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        // ---------- сохранение ----------

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("linked", linked);
            tag.putInt("linkX", linkPos.getX());
            tag.putInt("linkY", linkPos.getY());
            tag.putInt("linkZ", linkPos.getZ());
            tag.putString("linkDim", linkDim.toString());
            tag.putInt("instability", instability);
            ListTag items = new ListTag();
            for (ItemStack stack : outputStacks) {
                if (!stack.isEmpty()) {
                    items.add(stack.save(new CompoundTag()));
                }
            }
            tag.put("Items", items);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            linked = tag.getBoolean("linked");
            linkPos = new BlockPos(tag.getInt("linkX"), tag.getInt("linkY"), tag.getInt("linkZ"));
            ResourceLocation dim = ResourceLocation.tryParse(tag.getString("linkDim"));
            linkDim = dim == null ? Level.OVERWORLD.location() : dim;
            instability = tag.getInt("instability");
            outputStacks.clear();
            for (Tag item : tag.getList("Items", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.of((CompoundTag) item);
                if (!stack.isEmpty()) {
                    outputStacks.add(stack);
                }
            }
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = super.getUpdateTag();
            tag.putBoolean("linked", linked);
            tag.putInt("instability", instability);
            return tag;
        }

        @Override
        public void handleUpdateTag(CompoundTag tag) {
            linked = tag.getBoolean("linked");
            instability = tag.getInt("instability");
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this, entity -> getUpdateTag());
        }

        @Override
        public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket packet) {
            if (packet.getTag() != null) {
                handleUpdateTag(packet.getTag());
            }
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER && !essentia()) {
                return handler.cast();
            }
            return super.getCapability(cap, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            handler.invalidate();
        }
    }
}

package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;

import java.util.List;

/**
 * Продвинутая алхимическая печь — перенос BlockAlchemyFurnace, TileAlchemyFurnaceAdvanced и TileAlchemyFurnaceAdvancedNozzle
 * оригинала. Собирается жезлом из алхимической печи, обложенной восемью продвинутыми алхимическими конструкциями,
 * с четырьмя конструкциями и четырьмя перегонными кубами ярусом выше. Брошенное сверху сразу растворяется в эссенцию
 * (до 500 единиц), если хватает жара (вис Ignis из сети) и сил (вис Perditio и Aqua); эссенцию забирают трубы через
 * четыре сопла по бокам. Сломанная — рассыпается обратно в исходные блоки.
 */
public class AdvancedAlchemyFurnaceBlock extends BaseEntityBlock {
    /** 0 — ядро (бывшая печь), 1 — сопло, 2 — перегонный куб на углу, 3 — конструкция сверху, 4 — продвинутая конструкция на углу. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 4);
    /** Свечение ядра, как в оригинале: жар/500·12, если жар выше 100. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 12);
    private static final VoxelShape LOW = box(0, 0, 0, 16, 11.2, 16);

    public AdvancedAlchemyFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(GLOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, GLOW);
    }

    public static int light(BlockState state) {
        return state.getValue(GLOW);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == 0 ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    /** Ядро для всего, кроме живых, ниже обычного: брошенный предмет ложится в чан. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == 0 && context instanceof EntityCollisionContext entity && !(entity.getEntity() instanceof LivingEntity)) {
            return LOW;
        }
        return Shapes.block();
    }

    // ---------- сборка и распад ----------

    /** Чем был блок до сборки. */
    public static BlockState original(int part) {
        return switch (part) {
            case 0 -> ModBlocks.ALCHEMICAL_FURNACE.get().defaultBlockState();
            case 2 -> ModBlocks.ALEMBIC.get().defaultBlockState();
            case 3 -> ModBlocks.ALCHEMICAL_CONSTRUCT.get().defaultBlockState();
            default -> ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get().defaultBlockState();
        };
    }

    /** createAdvancedAlchemicalFurnace оригинала: ищет печь вокруг блока, которого коснулся жезл, и проверяет, что вокруг неё. */
    @Nullable
    public static BlockPos find(Level level, BlockPos clicked) {
        for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-1, -1, -1), clicked.offset(1, 1, 1))) {
            if (level.getBlockState(pos).is(ModBlocks.ALCHEMICAL_FURNACE.get()) && fits(level, pos.immutable())) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean fits(Level level, BlockPos core) {
        for (int a = -1; a <= 1; a++) {
            for (int c = -1; c <= 1; c++) {
                BlockState low = level.getBlockState(core.offset(a, 0, c));
                BlockState high = level.getBlockState(core.offset(a, 1, c));
                boolean centre = a == 0 && c == 0;
                boolean corner = a != 0 && c != 0;
                if (!centre && !low.is(ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get())) {
                    return false;
                }
                if (corner && !high.is(ModBlocks.ALEMBIC.get()) || !corner && !centre && !high.is(ModBlocks.ALCHEMICAL_CONSTRUCT.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void assemble(ServerLevel level, BlockPos core) {
        BlockState base = ModBlocks.ADVANCED_ALCHEMY_FURNACE.get().defaultBlockState();
        for (int a = -1; a <= 1; a++) {
            for (int c = -1; c <= 1; c++) {
                boolean corner = a != 0 && c != 0;
                boolean centre = a == 0 && c == 0;
                level.setBlock(core.offset(a, 0, c), base.setValue(PART, centre ? 0 : corner ? 4 : 1), 3);
                if (!centre) {
                    level.setBlock(core.offset(a, 1, c), base.setValue(PART, corner ? 2 : 3), 3);
                }
            }
        }
        for (int a = -1; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                for (int c = -1; c <= 1; c++) {
                    thaumcraft.common.network.FxPacket.blockSparkle(level, core.offset(a, b, c), -9999, 5);
                }
            }
        }
        level.playSound(null, core, ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
    }

    /** Ядро рядом (сосед в пределах куба 3×3×3). */
    @Nullable
    private static BlockPos coreNear(Level level, BlockPos pos) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(near);
            if (state.getBlock() instanceof AdvancedAlchemyFurnaceBlock && state.getValue(PART) == 0) {
                return near.immutable();
            }
        }
        return null;
    }

    private static void restore(Level level, BlockPos core) {
        for (int a = -1; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                for (int c = -1; c <= 1; c++) {
                    BlockPos pos = core.offset(a, b, c);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof AdvancedAlchemyFurnaceBlock) {
                        level.setBlock(pos, original(state.getValue(PART)), 3);
                    }
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this) && !level.isClientSide) {
            super.onRemove(state, level, pos, newState, moving);
            if (state.getValue(PART) == 0) {
                restore(level, pos);
            } else {
                BlockPos core = coreNear(level, pos);
                if (core != null) {
                    // как в оригинале: ядро распадётся на следующем такте
                    level.scheduleTick(core, this, 1);
                }
            }
            return;
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == 0) {
            level.setBlock(pos, original(0), 3);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(original(state.getValue(PART)).getBlock()));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(original(state.getValue(PART)).getBlock());
    }

    /** onEntityCollidedWithBlock оригинала: брошенное в чан растворяется по одному. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || state.getValue(PART) != 0 || !(entity instanceof ItemEntity item)
                || !(level.getBlockEntity(pos) instanceof Core core)) {
            return;
        }
        ItemStack stack = item.getItem();
        if (core.process(stack)) {
            stack.shrink(1);
            level.playSound(null, entity, ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2f, 1f + level.random.nextFloat() * 0.4f);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Nozzle nozzle && nozzle.furnace() != null) {
            Core core = nozzle.furnace();
            return Mth.floor(core.vis / (float) Core.MAX_VIS * 14f) + (core.vis > 0 ? 1 : 0);
        }
        return 0;
    }

    /** randomDisplayTick оригинала: в чане и в перегонных кубах пузырится лиловое варево. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != 0 || !(level.getBlockEntity(pos) instanceof Core core) || core.vis <= 0) {
            return;
        }
        thaumcraft.client.fx.Fx.slimyBubble(pos.getX() + random.nextFloat(), pos.getY() + 1, pos.getZ() + random.nextFloat(),
                0.06f + random.nextFloat() * 0.06f, 0.6f - random.nextFloat() * 0.2f, 0f, 0.6f + random.nextFloat() * 0.2f);
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos.getX() + random.nextFloat(), pos.getY() + 1, pos.getZ() + random.nextFloat(), SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS, 0.1f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.15f, false);
        }
        int q = random.nextInt(2);
        int w = random.nextInt(2);
        thaumcraft.client.fx.Fx.slimyBubble(pos.getX() - 0.6 + random.nextFloat() * 0.2 + q * 2, pos.getY() + 2,
                pos.getZ() - 0.6 + random.nextFloat() * 0.2 + w * 2, 0.06f + random.nextFloat() * 0.06f, 0.6f - random.nextFloat() * 0.2f, 0f,
                0.6f + random.nextFloat() * 0.2f);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (state.getValue(PART)) {
            case 0 -> new Core(pos, state);
            case 1 -> new Nozzle(pos, state);
            default -> null;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.ADVANCED_ALCHEMY_FURNACE.get(), (l, p, s, core) -> core.serverTick());
    }

    /** TileAlchemyFurnaceAdvanced оригинала. */
    public static class Core extends BlockEntity implements AspectContainer {
        public static final int MAX_VIS = 500;
        public static final int MAX_POWER = 500;
        public AspectList aspects = new AspectList();
        public int vis;
        public int heat;
        public int power1;
        public int power2;
        private int count;
        private int processed;

        public Core(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ADVANCED_ALCHEMY_FURNACE.get(), pos, state);
        }

        void serverTick() {
            count++;
            if (processed > 0) {
                processed--;
            }
            if (count % 5 == 0) {
                int before = heat--;
                if (heat <= MAX_POWER) {
                    heat += VisNetHandler.drainVis(level, worldPosition, Aspect.FIRE, 50);
                }
                if (power1 <= MAX_POWER) {
                    power1 += VisNetHandler.drainVis(level, worldPosition, Aspect.ENTROPY, 50);
                }
                if (power2 <= MAX_POWER) {
                    power2 += VisNetHandler.drainVis(level, worldPosition, Aspect.WATER, 50);
                }
                heat = Math.max(0, heat);
                if (before / 50 != heat / 50) {
                    int glow = heat > 100 ? Math.min(12, (int) (heat / (float) MAX_POWER * 12f)) : 0;
                    if (getBlockState().getValue(GLOW) != glow) {
                        level.setBlock(worldPosition, getBlockState().setValue(GLOW, glow), 3);
                    }
                    sync();
                }
            }
        }

        public boolean process(ItemStack stack) {
            if (processed != 0 || stack.isEmpty()) {
                return false;
            }
            AspectList found = ItemAspectManager.INSTANCE.getAspects(stack);
            if (found == null || found.isEmpty()) {
                return false;
            }
            int size = found.visSize();
            if (size + aspects.visSize() > MAX_VIS || size * 2 > heat || size > power1 || size > power2) {
                return false;
            }
            heat -= size * 2;
            power1 -= size;
            power2 -= size;
            processed += (int) (5f + Math.max(0f, (1f - heat / (float) MAX_POWER) * 100f));
            aspects.add(found);
            vis = aspects.visSize();
            sync();
            return true;
        }

        boolean take(Aspect aspect, int amount) {
            if (aspects.getAmount(aspect) < amount) {
                return false;
            }
            aspects.reduce(aspect, amount);
            vis = aspects.visSize();
            sync();
            return true;
        }

        void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public AspectList getContainedAspects() {
            return aspects;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            aspects.save(tag);
            tag.putShort("vis", (short) vis);
            tag.putShort("heat", (short) heat);
            tag.putShort("power1", (short) power1);
            tag.putShort("power2", (short) power2);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspects = AspectList.load(tag);
            vis = tag.getShort("vis");
            heat = tag.getShort("heat");
            power1 = tag.getShort("power1");
            power2 = tag.getShort("power2");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
        }
    }

    /** TileAlchemyFurnaceAdvancedNozzle оригинала: отдаёт эссенцию печи в трубу снаружи. */
    public static class Nozzle extends BlockEntity implements EssentiaTransport, AspectContainer {
        public Nozzle(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ADVANCED_ALCHEMY_FURNACE_NOZZLE.get(), pos, state);
        }

        @Nullable
        Core furnace() {
            if (level == null) {
                return null;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(worldPosition.relative(side)) instanceof Core core) {
                    return core;
                }
            }
            return null;
        }

        @Nullable
        private Direction facing() {
            if (level == null) {
                return null;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(worldPosition.relative(side)) instanceof Core) {
                    return side.getOpposite();
                }
            }
            return null;
        }

        @Override
        public AspectList getContainedAspects() {
            Core core = furnace();
            return core == null ? null : core.aspects;
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return false;
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return side == facing();
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
        }

        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            Core core = furnace();
            return canOutputTo(side) && core != null && core.take(aspect, amount) ? amount : 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            Core core = furnace();
            return core == null || core.aspects.isEmpty() ? null : core.aspects.getAspects().get(0);
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            Core core = furnace();
            return core == null || core.aspects.isEmpty() ? 0 : core.aspects.getAmount(core.aspects.getAspects().get(0));
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }
    }
}

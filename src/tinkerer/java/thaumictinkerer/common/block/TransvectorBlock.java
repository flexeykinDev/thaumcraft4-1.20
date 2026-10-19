package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;
import thaumictinkerer.common.registry.TTBlockEntities;

import java.util.List;

/**
 * Трансвекторы (BlockTransvectorInterface и BlockTransvectorDislocator оригинала). Оба маскируются под другие блоки
 * и привязываются к блоку-цели связующим щупом. Интерфейс в пределах четырёх блоков выдаёт себя за цель: трубы,
 * хопперы и прочее работают с ним, как с самой целью. Смещатель по сигналу красного камня меняет местами цель
 * (до 16 блоков) и блок перед своей лицевой стороной вместе с содержимым и стоящими там существами.
 */
public abstract class TransvectorBlock extends BaseEntityBlock {
    public static final BooleanProperty CAMO = CamoBlockEntity.CAMO;

    protected TransvectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return CamoBlockEntity.useCamo(state, level, pos, player, hand);
    }

    /** TileTransvector оригинала: маскировка и привязанная цель. */
    public abstract static class Tile extends CamoBlockEntity {
        @Nullable
        private BlockPos target;

        protected Tile(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public abstract int getMaxDistance();

        /** Нужна ли у цели своя сущность блока (интерфейсу нужна, смещателю — нет). */
        public abstract boolean needsTile();

        public void setTarget(@Nullable BlockPos target) {
            this.target = target;
            sync();
        }

        public boolean inRange(BlockPos pos) {
            return Math.abs(pos.getX() - worldPosition.getX()) <= getMaxDistance() && Math.abs(pos.getY() - worldPosition.getY()) <= getMaxDistance()
                    && Math.abs(pos.getZ() - worldPosition.getZ()) <= getMaxDistance();
        }

        /** Цель, если она на месте и в пределах досягаемости (getTile оригинала). */
        @Nullable
        public BlockPos target() {
            if (target == null || level == null || !level.isLoaded(target) || !inRange(target)) {
                return null;
            }
            return !needsTile() || level.getBlockEntity(target) != null ? target : null;
        }

        @Nullable
        protected BlockEntity targetTile() {
            BlockPos pos = target();
            BlockEntity tile = pos == null ? null : level.getBlockEntity(pos);
            return tile instanceof Interface.Tile ? null : tile;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (target != null) {
                tag.putLong("Target", target.asLong());
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
        }
    }

    // ======================================================================================== интерфейс
    public static class Interface extends TransvectorBlock {
        public Interface(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(CAMO, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(CAMO);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Tile(pos, state);
        }

        /** TileTransvectorInterface оригинала. */
        public static class Tile extends TransvectorBlock.Tile implements EssentiaTransport, AspectContainer {
            public Tile(BlockPos pos, BlockState state) {
                super(TTBlockEntities.TRANSVECTOR_INTERFACE.get(), pos, state);
            }

            @Override
            public int getMaxDistance() {
                return 4;
            }

            @Override
            public boolean needsTile() {
                return true;
            }

            // предметы, жидкости, энергия — всё, что цель отдаёт через возможности Forge
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                BlockEntity tile = targetTile();
                return tile != null ? tile.getCapability(cap, side) : super.getCapability(cap, side);
            }

            @Nullable
            private EssentiaTransport transport() {
                return targetTile() instanceof EssentiaTransport transport ? transport : null;
            }

            @Override
            public boolean isConnectable(Direction side) {
                EssentiaTransport t = transport();
                return t != null && t.isConnectable(side);
            }

            @Override
            public boolean canInputFrom(Direction side) {
                EssentiaTransport t = transport();
                return t != null && t.canInputFrom(side);
            }

            @Override
            public boolean canOutputTo(Direction side) {
                EssentiaTransport t = transport();
                return t != null && t.canOutputTo(side);
            }

            @Override
            public void setSuction(@Nullable Aspect aspect, int amount) {
                EssentiaTransport t = transport();
                if (t != null) {
                    t.setSuction(aspect, amount);
                }
            }

            @Nullable
            @Override
            public Aspect getSuctionType(@Nullable Direction side) {
                EssentiaTransport t = transport();
                return t == null ? null : t.getSuctionType(side);
            }

            @Override
            public int getSuctionAmount(@Nullable Direction side) {
                EssentiaTransport t = transport();
                return t == null ? 0 : t.getSuctionAmount(side);
            }

            @Override
            public int takeEssentia(Aspect aspect, int amount, Direction side) {
                EssentiaTransport t = transport();
                return t == null ? 0 : t.takeEssentia(aspect, amount, side);
            }

            @Override
            public int addEssentia(Aspect aspect, int amount, Direction side) {
                EssentiaTransport t = transport();
                return t == null ? 0 : t.addEssentia(aspect, amount, side);
            }

            @Nullable
            @Override
            public Aspect getEssentiaType(@Nullable Direction side) {
                EssentiaTransport t = transport();
                return t == null ? null : t.getEssentiaType(side);
            }

            @Override
            public int getEssentiaAmount(@Nullable Direction side) {
                EssentiaTransport t = transport();
                return t == null ? 0 : t.getEssentiaAmount(side);
            }

            @Override
            public int getMinimumSuction() {
                EssentiaTransport t = transport();
                return t == null ? 0 : t.getMinimumSuction();
            }

            @Nullable
            @Override
            public AspectList getContainedAspects() {
                return targetTile() instanceof AspectContainer container ? container.getContainedAspects() : null;
            }
        }
    }

    // ======================================================================================== смещатель
    public static class Dislocator extends TransvectorBlock {
        public static final DirectionProperty FACING = DirectionalBlock.FACING;
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        public Dislocator(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(CAMO, false).setValue(FACING, Direction.NORTH).setValue(POWERED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(CAMO, FACING, POWERED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        }

        /** Жезлом лицевая сторона поворачивается к той грани, по которой щёлкнули. */
        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (player.getItemInHand(hand).getItem() instanceof WandItem) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(FACING, hit.getDirection()), Block.UPDATE_ALL);
                    level.playSound(null, pos, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.6f, 1f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return super.use(state, level, pos, player, hand, hit);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
            boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
            if (powered && !state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(pos) instanceof Tile tile) {
                    tile.pulse();
                }
            } else if (!powered && state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_CLIENTS);
            }
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Tile(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.TRANSVECTOR_DISLOCATOR.get(), (l, p, s, t) -> t.tick());
        }

        /** TileTransvectorDislocator оригинала: после перестановки — пауза в полсекунды; сигнал в паузе откладывается. */
        public static class Tile extends TransvectorBlock.Tile {
            private int cooldown;
            private boolean pulseStored;

            public Tile(BlockPos pos, BlockState state) {
                super(TTBlockEntities.TRANSVECTOR_DISLOCATOR.get(), pos, state);
            }

            @Override
            public int getMaxDistance() {
                return 16;
            }

            @Override
            public boolean needsTile() {
                return false;
            }

            void tick() {
                cooldown = Math.max(0, cooldown - 1);
                if (cooldown == 0 && pulseStored) {
                    pulseStored = false;
                    pulse();
                }
            }

            private static boolean movable(Level level, BlockPos pos) {
                BlockState state = level.getBlockState(pos);
                return state.getDestroySpeed(level, pos) >= 0 && !state.is(Blocks.MOVING_PISTON) && !state.is(Blocks.PISTON_HEAD);
            }

            public void pulse() {
                BlockPos a = target();
                if (a == null) {
                    return;
                }
                if (cooldown > 0) {
                    pulseStored = true;
                    return;
                }
                BlockPos b = worldPosition.relative(getBlockState().getValue(FACING));
                if (a.equals(worldPosition) || b.equals(a)) {
                    return;
                }
                if (movable(level, a) && movable(level, b)) {
                    BlockState stateA = level.getBlockState(a);
                    BlockState stateB = level.getBlockState(b);
                    BlockEntity tileA = level.getBlockEntity(a);
                    BlockEntity tileB = level.getBlockEntity(b);
                    CompoundTag dataA = tileA == null ? null : tileA.saveWithoutMetadata();
                    CompoundTag dataB = tileB == null ? null : tileB.saveWithoutMetadata();
                    // содержимое переезжает вместе с блоком — старые сущности убираем, чтобы ничего не высыпалось
                    level.removeBlockEntity(a);
                    level.removeBlockEntity(b);
                    level.setBlock(a, stateB, Block.UPDATE_ALL);
                    level.setBlock(b, stateA, Block.UPDATE_ALL);
                    load(a, dataB);
                    load(b, dataA);
                }
                List<Entity> atA = level.getEntitiesOfClass(Entity.class, new AABB(a));
                List<Entity> atB = level.getEntitiesOfClass(Entity.class, new AABB(b));
                Vec3 shift = Vec3.atLowerCornerOf(b.subtract(a));
                for (Entity entity : atA) {
                    move(entity, shift);
                }
                for (Entity entity : atB) {
                    move(entity, shift.reverse());
                }
                cooldown = 10;
            }

            private void load(BlockPos pos, @Nullable CompoundTag data) {
                BlockEntity tile = level.getBlockEntity(pos);
                if (tile != null && data != null) {
                    tile.load(data);
                    tile.setChanged();
                }
            }

            private static void move(Entity entity, Vec3 shift) {
                if (entity instanceof ServerPlayer player) {
                    player.teleportTo((ServerLevel) player.level(), player.getX() + shift.x, player.getY() + shift.y, player.getZ() + shift.z,
                            player.getYRot(), player.getXRot());
                } else {
                    entity.setPos(entity.position().add(shift));
                }
            }
        }
    }
}

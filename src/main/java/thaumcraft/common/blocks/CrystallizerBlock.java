package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.items.CrystalEssenceItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModItems;

/**
 * Кристаллизатор эссенции (BlockTube оригинала, мета 7). Тянет единицу эссенции с той стороны, к которой приставлен,
 * за тысячу тиков уплотняет её в кристалл и выбрасывает с противоположной стороны — либо кладёт в сундук, если он там.
 */
public class CrystallizerBlock extends BaseEntityBlock {
    /** Сторона, с которой подходит труба. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = box(3, 3, 3, 13, 13, 13);

    public CrystallizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Crystallizer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CRYSTALLIZER.get(),
                level.isClientSide ? Crystallizer::clientTick : Crystallizer::serverTick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    /** TileEssentiaCrystalizer оригинала. */
    public static class Crystallizer extends BlockEntity implements EssentiaTransport, thaumcraft.api.aspects.AspectContainer {
        private static final int PROGRESS_MAX = 200;

        @Nullable
        private Aspect aspect;
        private int count;
        private int progress;
        // только на клиенте: вращение кристаллов и их цвет, плавно подстраивающийся под аспект
        private float spin;
        private float spinSpeed;
        private final float[] color = {1f, 1f, 1f};

        public Crystallizer(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CRYSTALLIZER.get(), pos, state);
        }

        private Direction facing() {
            return getBlockState().getValue(FACING);
        }

        private boolean powered() {
            return level != null && level.hasNeighborSignal(worldPosition);
        }

        @Override
        public thaumcraft.api.aspects.AspectList getContainedAspects() {
            thaumcraft.api.aspects.AspectList list = new thaumcraft.api.aspects.AspectList();
            if (aspect != null) {
                list.add(aspect, 1);
            }
            return list;
        }

        public float getSpin(float partialTick) {
            return spin + spinSpeed * partialTick;
        }

        public float[] getColor() {
            return color;
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Crystallizer tile) {
            if (++tile.count % 5 == 0 && !tile.powered()) {
                if (tile.aspect == null) {
                    tile.fillReservoir(level);
                    tile.progress = 0;
                } else {
                    // вис земли из сети реле ускоряет работу: каждая сотая доля даёт два шага сверх обычного
                    tile.progress += 1 + thaumcraft.api.visnet.VisNetHandler.drainVis(level, pos, Aspect.EARTH,
                            Math.min(20, Math.max(1, (PROGRESS_MAX - tile.progress) / 2))) * 2;
                }
            }
            if (tile.aspect != null && tile.progress >= PROGRESS_MAX) {
                tile.eject((ServerLevel) level);
                tile.aspect = null;
                tile.progress = 0;
                tile.sync();
            }
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Crystallizer tile) {
            int target = tile.aspect == null ? 0xdcdcdc : tile.aspect.getColor();
            float[] wanted = {(target >> 16 & 255) / 220f, (target >> 8 & 255) / 220f, (target & 255) / 220f};
            for (int i = 0; i < 3; i++) {
                if (tile.color[i] < wanted[i]) {
                    tile.color[i] = Math.min(wanted[i], tile.color[i] + 0.05f);
                } else if (tile.color[i] > wanted[i]) {
                    tile.color[i] = Math.max(wanted[i], tile.color[i] - 0.05f);
                }
            }
            tile.spin += tile.spinSpeed;
            if (tile.spin > 360f) {
                tile.spin -= 360f;
            }
            boolean powered = tile.powered();
            if (tile.aspect != null && !powered) {
                tile.spinSpeed = Math.min(20f, tile.spinSpeed + 0.1f);
            } else {
                tile.spinSpeed = Math.max(0f, tile.spinSpeed - 0.2f);
            }
        }

        private void fillReservoir(Level level) {
            Direction facing = facing();
            EssentiaTransport source = EssentiaTransport.neighbour(level, worldPosition, facing);
            if (source == null || !source.canOutputTo(facing.getOpposite())) {
                return;
            }
            int suction = getSuctionAmount(facing);
            if (source.getEssentiaAmount(facing.getOpposite()) > 0 && source.getSuctionAmount(facing.getOpposite()) < suction
                    && suction >= source.getMinimumSuction()) {
                Aspect wanted = source.getEssentiaType(facing.getOpposite());
                if (wanted != null && source.takeEssentia(wanted, 1, facing.getOpposite()) == 1) {
                    aspect = wanted;
                    sync();
                }
            }
        }

        /** Готовый кристалл уходит в сундук с обратной стороны, а если его нет — выпадает туда же. */
        private void eject(ServerLevel level) {
            Direction out = facing().getOpposite();
            ItemStack stack = CrystalEssenceItem.of(ModItems.CRYSTAL_ESSENCE.get(), aspect);
            if (level.getBlockEntity(worldPosition.relative(out)) instanceof Container container) {
                stack = HopperBlockEntity.addItem(null, container, stack, facing());
            }
            if (!stack.isEmpty()) {
                ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5 + out.getStepX() * 0.65,
                        worldPosition.getY() + 0.5 + out.getStepY() * 0.65, worldPosition.getZ() + 0.5 + out.getStepZ() * 0.65, stack);
                item.setDeltaMovement(out.getStepX() * 0.04, out.getStepY() * 0.04, out.getStepZ() * 0.04);
                level.addFreshEntity(item);
                // облачко пара у выхода
                for (int i = 0; i < 7; i++) {
                    FxPacket.send(level, FxPacket.VENT, Vec3.atCenterOf(worldPosition).add(out.getStepX() / 2.1, out.getStepY() / 2.1,
                            out.getStepZ() / 2.1), Vec3.ZERO, 0xffffff, 0);
                }
            }
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f,
                    2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Aspect suctionAspect, int amount) {
        }

        @Nullable
        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return powered() ? 0 : side == facing() && aspect == null ? 128 : 64;
        }

        @Nullable
        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            return aspect;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return aspect == null ? 0 : 1;
        }

        @Override
        public int takeEssentia(Aspect taken, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect added, int amount, Direction side) {
            if (canInputFrom(side) && aspect == null && amount > 0) {
                aspect = added;
                sync();
                return 1;
            }
            return 0;
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (aspect != null) {
                tag.putString("Aspect", aspect.getTag());
            }
            tag.putInt("Progress", progress);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspect = Aspect.byTag(tag.getString("Aspect"));
            progress = tag.getInt("Progress");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}

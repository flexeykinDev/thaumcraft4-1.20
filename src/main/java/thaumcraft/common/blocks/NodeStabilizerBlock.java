package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Стабилизатор узла (BlockStoneDevice оригинала, мета 9 и 10). Ставится прямо под узел ауры: узел перестаёт
 * тянуть вис из соседей и сам становится недоступен для них, зато восстанавливается вдвое (обычный) или
 * в двадцать раз (улучшенный) медленнее. Сигнал красного камня отключает стабилизатор.
 */
public class NodeStabilizerBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 12, 16);
    private final boolean advanced;

    public NodeStabilizerBlock(boolean advanced, Properties properties) {
        super(properties);
        this.advanced = advanced;
    }

    /** Сила удержания: 1 у обычного, 2 у улучшенного. */
    public int getLock() {
        return advanced ? 2 : 1;
    }

    /** Стабилизатор в этой клетке, если он есть и не выключен красным камнем; иначе 0. */
    public static int lockAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof NodeStabilizerBlock block && !level.hasNeighborSignal(pos) ? block.getLock() : 0;
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
        return new Stabilizer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.NODE_STABILIZER.get(), Stabilizer::clientTick) : null;
    }

    /** TileNodeStabilizer оригинала: на клиенте считает, насколько выдвинуты «лапы», держащие узел. */
    public static class Stabilizer extends BlockEntity {
        public int count;

        public Stabilizer(BlockPos pos, BlockState state) {
            super(ModBlockEntities.NODE_STABILIZER.get(), pos, state);
        }

        public int getLock() {
            return getBlockState().getBlock() instanceof NodeStabilizerBlock block ? block.getLock() : 1;
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Stabilizer tile) {
            boolean holding = level.getBlockEntity(pos.above()) instanceof NodeBlockEntity && !level.hasNeighborSignal(pos);
            if (holding) {
                if (tile.count < 37) {
                    tile.count++;
                }
            } else if (tile.count > 0) {
                tile.count--;
            }
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition, worldPosition.offset(1, 2, 1));
        }
    }
}

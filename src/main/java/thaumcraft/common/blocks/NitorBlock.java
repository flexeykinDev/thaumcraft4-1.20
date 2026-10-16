package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Нитор (BlockAiry оригинала, мета 1): вечный огонёк, висящий в воздухе. Светит как факел, греет тигель,
 * сам блок невидим — видно только пламя из огоньков.
 */
public class NitorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(5, 5, 5, 11, 11, 11);

    public NitorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Flame(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.NITOR.get(), Flame::clientTick) : null;
    }

    /** TileNitor оригинала: ничего не хранит, только каждый тик рисует пламя. */
    public static class Flame extends BlockEntity {
        public Flame(BlockPos pos, BlockState state) {
            super(ModBlockEntities.NITOR.get(), pos, state);
        }

        /** Те же шансы и размеры, что в оригинале при обычных настройках частиц: красный огонёк и маленький жёлтый. */
        public static void clientTick(Level level, BlockPos pos, BlockState state, Flame flame) {
            RandomSource random = level.random;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            if (random.nextInt(7) == 0) {
                thaumcraft.client.fx.Fx.wispTo(x, y, z, pos.getX() + 0.3 + random.nextFloat() * 0.4, y,
                        pos.getZ() + 0.3 + random.nextFloat() * 0.4, 0.5f, 4, true, -0.025f);
            }
            if (random.nextInt(11) == 0) {
                thaumcraft.client.fx.Fx.wispTo(x, y, z, pos.getX() + 0.4 + random.nextFloat() * 0.2, y,
                        pos.getZ() + 0.4 + random.nextFloat() * 0.2, 0.25f, 1, true, -0.02f);
            }
        }
    }
}

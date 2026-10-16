package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Обсидиановый тотем (blockCosmeticSolid:0 оригинала). Столб из тотемов выглядит единым: у верхнего блока на
 * гранях вырезаны лики, у нижних — затенённое основание, одинокий тотем — простое основание
 * (BlockCosmeticSolid.getIcon). Заряженный тотем (мета 8) считается частью того же столба.
 */
public class ObsidianTotemBlock extends Block {
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public ObsidianTotemBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN);
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(UP, level.getBlockState(pos.above()).getBlock() instanceof ObsidianTotemBlock)
                .setValue(DOWN, level.getBlockState(pos.below()).getBlock() instanceof ObsidianTotemBlock);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction.getAxis() == Direction.Axis.Y) {
            return state.setValue(direction == Direction.UP ? UP : DOWN, neighbour.getBlock() instanceof ObsidianTotemBlock);
        }
        return state;
    }

    /** Ставит тотем с правильными связями (для генерации мира). */
    public static BlockState placed(Block block, LevelAccessor level, BlockPos pos) {
        return ((ObsidianTotemBlock) block).connect(block.defaultBlockState(), level, pos);
    }
}

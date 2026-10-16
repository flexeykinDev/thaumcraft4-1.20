package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Деревянный стол (BlockTable оригинала, мета 0 и 1). Сам по себе это мебель; жезл превращает его
 * в магический верстак (см. WandItem).
 */
public class TableBlock extends Block {
    /** Вдоль какой оси стоит перекладина между ножками. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape TOP = box(0, 12, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_X = Shapes.or(TOP, box(0, 0, 4, 16, 4, 12), box(2, 4, 6, 6, 12, 10), box(10, 4, 6, 14, 12, 10));
    private static final VoxelShape SHAPE_Z = Shapes.or(TOP, box(4, 0, 0, 12, 4, 16), box(6, 4, 2, 10, 12, 6), box(6, 4, 10, 10, 12, 14));

    public TableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // перекладина ложится поперёк взгляда игрока
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }
}

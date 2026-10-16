package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import thaumcraft.common.registry.ModBlocks;

/**
 * Столп инфузии (BlockStoneDevice оригинала, мета 3 и 4). В него превращаются столбы алтаря, когда жезл
 * пробуждает матрицу: нижний блок (бывшие кирпичи) рисует всю двухблочную модель, верхний (бывший магический
 * камень) невидим. Если убрать одну половину, вторая снова становится обычным камнем.
 */
public class InfusionPillarBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** Угол алтаря, как orientation оригинала минус 2: 0 — (x-, z-), 1 — (x-, z+), 2 — (x+, z-), 3 — (x+, z+). */
    public static final IntegerProperty CORNER = IntegerProperty.create("corner", 0, 3);

    public InfusionPillarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(CORNER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, CORNER);
    }

    /** Во что превращается половина, когда столп распадается. */
    public static BlockState original(BlockState state) {
        return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? ModBlocks.ARCANE_STONE_BRICKS : ModBlocks.ARCANE_STONE)
                .get().defaultBlockState();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                  BlockPos neighborPos) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (side == (lower ? Direction.UP : Direction.DOWN)
                && (!neighbor.is(this) || neighbor.getValue(HALF) == state.getValue(HALF))) {
            return original(state);
        }
        return state;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    /** Превращает столб из кирпичей и камня в столп. */
    public static void form(Level level, BlockPos bricks, int corner) {
        BlockState pillar = ModBlocks.INFUSION_PILLAR.get().defaultBlockState().setValue(CORNER, corner);
        // сначала верх, потом низ с флагом без обновления соседей: иначе половина сразу «не найдёт» вторую и рассыплется
        level.setBlock(bricks.above(), pillar.setValue(HALF, DoubleBlockHalf.UPPER), 2 | 16);
        level.setBlock(bricks, pillar.setValue(HALF, DoubleBlockHalf.LOWER), 2 | 16);
    }
}

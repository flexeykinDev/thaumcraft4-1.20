package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Кристаллическая гроздь (BlockCrystal оригинала, виды 0–6): шесть кристаллов одной стихии или по одному каждой,
 * растущие из грани соседнего блока. Светится, искрит, успокаивает наполнение и при разрушении
 * рассыпается на шесть осколков.
 */
public class CrystalClusterBlock extends Block implements EntityBlock {
    /** Куда смотрят кристаллы: от блока, к которому гроздь прикреплена. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** BlockCustomOreItem.colors оригинала: воздух, огонь, вода, земля, порядок, хаос. */
    public static final int[] COLORS = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction dir : Direction.values()) {
            // гроздь занимает середину блока и прижата к основанию
            double lo = 3;
            double hi = 13;
            SHAPES[dir.get3DDataValue()] = box(
                    dir == Direction.EAST ? 0 : dir == Direction.WEST ? 4 : lo, dir == Direction.UP ? 0 : dir == Direction.DOWN ? 4 : lo,
                    dir == Direction.SOUTH ? 0 : dir == Direction.NORTH ? 4 : lo,
                    dir == Direction.WEST ? 16 : dir == Direction.EAST ? 12 : hi, dir == Direction.DOWN ? 16 : dir == Direction.UP ? 12 : hi,
                    dir == Direction.NORTH ? 16 : dir == Direction.SOUTH ? 12 : hi);
        }
    }

    /** 0–5 — стихия, 6 — смешанная гроздь. */
    public final int kind;

    public CrystalClusterBlock(Properties properties, int kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    private static boolean attached(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos base = pos.relative(facing.getOpposite());
        return level.getBlockState(base).isFaceSturdy(level, base, facing);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return attached(level, pos, state.getValue(FACING));
    }

    /** Гроздь ставится на ту грань, по которой щёлкнули, а если она не держит — на любую твёрдую рядом. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (attached(context.getLevel(), context.getClickedPos(), context.getClickedFace())) {
            return defaultBlockState().setValue(FACING, context.getClickedFace());
        }
        for (Direction dir : Direction.values()) {
            if (attached(context.getLevel(), context.getClickedPos(), dir)) {
                return defaultBlockState().setValue(FACING, dir);
            }
        }
        return null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
                                  BlockPos neighbourPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** Цвет случайной искры и кристаллов: у смешанной грозди — любая из шести стихий. */
    public int colorOf(RandomSource random) {
        return COLORS[kind < 6 ? kind : random.nextInt(6)];
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(17) == 0) {
            int color = colorOf(random);
            thaumcraft.client.fx.Fx.spark(pos.getX() + 0.3 + random.nextFloat() * 0.4f, pos.getY() + 0.3 + random.nextFloat() * 0.4f,
                    pos.getZ() + 0.3 + random.nextFloat() * 0.4f, 0.2f + random.nextFloat() * 0.1f,
                    (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 0.8f);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Crystal(pos, state);
    }

    /** Данных не хранит: нужен только затем, чтобы гроздь рисовал CrystalRenderer. */
    public static class Crystal extends BlockEntity {
        public Crystal(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CRYSTAL.get(), pos, state);
        }
    }
}

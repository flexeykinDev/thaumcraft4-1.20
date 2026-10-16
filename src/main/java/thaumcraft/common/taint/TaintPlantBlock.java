package thaumcraft.common.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порченая трава, светящееся порченое растение и споровый стебель (BlockTaintFibres оригинала, виды 1–4). Вырастают
 * вместо волокон на твёрдой земле под открытым верхом, ведут себя как волокна и вне порченой земли гибнут.
 */
public class TaintPlantBlock extends Block {
    /** У спорового стебля: на нём сидит созревшая спора (вид 4 оригинала). */
    public static final BooleanProperty RIPE = BooleanProperty.create("ripe");
    private static final VoxelShape SHAPE = box(3.2, 0, 3.2, 12.8, 12.8, 12.8);

    private final boolean stalk;

    public TaintPlantBlock(Properties properties, boolean stalk) {
        super(properties);
        this.stalk = stalk;
        registerDefaultState(stateDefinition.any().setValue(RIPE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RIPE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
                                  BlockPos neighbourPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Taint.biomeSpread(level, pos, random);
        if (!Taint.isTainted(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        Taint.growTick(level, pos, random);
        if (!stalk) {
            return;
        }
        // на пустом стебле под открытым верхом изредка созревает спора; если спора пропала, стебель снова пуст
        if (!state.getValue(RIPE)) {
            if (random.nextInt(10) == 0 && level.isEmptyBlock(pos.above())) {
                level.setBlock(pos, state.setValue(RIPE, true), 3);
                thaumcraft.common.entity.TaintSpore spore = thaumcraft.common.registry.ModEntities.TAINT_SPORE.get().create(level);
                if (spore != null) {
                    spore.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0f, 0f);
                    level.addFreshEntity(spore);
                }
            }
        } else if (level.getEntitiesOfClass(thaumcraft.common.entity.TaintSpore.class,
                new net.minecraft.world.phys.AABB(pos.above())).isEmpty()) {
            level.setBlock(pos, state.setValue(RIPE, false), 3);
        }
    }

    public boolean isStalk() {
        return stalk;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        TaintFibresBlock.poison(level, entity);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return true;
    }
}

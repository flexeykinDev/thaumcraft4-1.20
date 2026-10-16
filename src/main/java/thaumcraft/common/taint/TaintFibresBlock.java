package thaumcraft.common.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModEffects;

import java.util.Map;

/**
 * Волокна порчи (BlockTaintFibres оригинала, вид 0): тонкая плёнка, которая затягивает все твёрдые грани вокруг
 * своей клетки. Растёт только на порченой земле и сама её расширяет; сквозь волокна можно ходить, но они изредка
 * отравляют. Без опоры, вне порченой земли или окружённые одной порчей волокна исчезают.
 */
public class TaintFibresBlock extends Block {
    public static final Map<Direction, BooleanProperty> FACES = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape[] FACE_SHAPES = {
            box(0, 0, 0, 16, 1, 16), box(0, 15, 0, 16, 16, 16), box(0, 0, 0, 16, 16, 1),
            box(0, 0, 15, 16, 16, 16), box(0, 0, 0, 1, 16, 16), box(15, 0, 0, 16, 16, 16)};

    public TaintFibresBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty face : FACES.values()) {
            state = state.setValue(face, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        FACES.values().forEach(builder::add);
    }

    /** Плёнка ложится на грань, если сосед с той стороны твёрдый и сам не корка или порченая почва. */
    private static boolean covers(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        BlockState state = level.getBlockState(other);
        return state.isFaceSturdy(level, other, dir.getOpposite()) && !(state.getBlock() instanceof TaintBlock);
    }

    /** Волокна для этого места: с плёнкой на всех подходящих гранях. */
    public static BlockState stateFor(BlockGetter level, BlockPos pos) {
        BlockState state = thaumcraft.common.registry.ModBlocks.TAINT_FIBRES.get().defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(FACES.get(dir), covers(level, pos, dir));
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Taint.isTainted(context.getLevel(), context.getClickedPos()) ? stateFor(context.getLevel(), context.getClickedPos()) : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !Taint.onlyAdjacentToTaint(level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
                                  BlockPos neighbourPos) {
        if (Taint.onlyAdjacentToTaint(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state.setValue(FACES.get(direction), covers(level, pos, direction));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Taint.biomeSpread(level, pos, random);
        if (Taint.onlyAdjacentToTaint(level, pos) || !Taint.isTainted(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        Taint.growTick(level, pos, random);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        poison(level, entity);
    }

    /** Живое, задевшее порчу, изредка получает порченый яд: игрок — раз из тысячи, прочие — раз из пятисот. */
    static void poison(Level level, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.getMobType() != MobType.UNDEAD) {
            if (entity instanceof Player && level.random.nextInt(1000) == 0) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 80, 0, false, true));
            } else if (!(entity instanceof Player) && level.random.nextInt(500) == 0) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 160, 0, false, true));
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        for (Direction dir : Direction.values()) {
            if (state.getValue(FACES.get(dir))) {
                shape = Shapes.or(shape, FACE_SHAPES[dir.get3DDataValue()]);
            }
        }
        return shape.isEmpty() ? FACE_SHAPES[0] : shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return true;
    }
}

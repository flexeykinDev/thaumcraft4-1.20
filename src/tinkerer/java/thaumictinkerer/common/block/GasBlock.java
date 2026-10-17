package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumictinkerer.common.item.BrightNitorItem;

/**
 * Газ (BlockGas оригинала): невидимый блок, сквозь который ходят и который заменяется чем угодно. Выпущенный из склянки
 * газ расползается на четыре шага во все стороны и оседает. Светящийся газ светит, теневой гасит свет; газ нитора
 * висит над головой владельца гиперзаряженного нитора и пропадает, когда тот уходит.
 */
public class GasBlock extends Block {
    public static final IntegerProperty SPREAD = IntegerProperty.create("spread", 0, 4);

    public enum Kind { LIGHT, SHADOW, NITOR }

    private final Kind kind;

    public GasBlock(Kind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(SPREAD, 0));
    }

    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().instabreak()
                .pushReaction(PushReaction.DESTROY).noOcclusion();
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPREAD);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return kind != Kind.SHADOW;
    }

    /** Теневой газ не пропускает свет (setLightOpacity(215) оригинала). */
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return kind == Kind.SHADOW ? 15 : 0;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && (kind == Kind.NITOR || state.getValue(SPREAD) > 0)) {
            level.scheduleTick(pos, this, tickDelay(level));
        }
    }

    private int tickDelay(LevelReader level) {
        if (kind != Kind.NITOR) {
            return 10;
        }
        return level instanceof Level world && world.dimension() == Level.NETHER ? 60 : 20;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (kind == Kind.NITOR) {
            // свет держится, пока рядом есть игрок с гиперзаряженным нитором
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(1))) {
                if (BrightNitorItem.carries(player)) {
                    level.scheduleTick(pos, this, tickDelay(level));
                    return;
                }
            }
            level.removeBlock(pos, false);
            return;
        }
        int spread = state.getValue(SPREAD);
        if (spread > 0) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (level.isEmptyBlock(next)) {
                    level.setBlock(next, defaultBlockState().setValue(SPREAD, spread - 1), Block.UPDATE_CLIENTS);
                }
            }
            level.setBlock(pos, state.setValue(SPREAD, 0), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        thaumictinkerer.client.TTClientHooks.gasParticles(kind, pos, random);
    }
}

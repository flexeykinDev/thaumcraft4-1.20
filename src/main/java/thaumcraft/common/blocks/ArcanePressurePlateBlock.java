package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.OwnedBlockEntity;

import java.util.List;

/**
 * Чародейская нажимная плита — перенос blockWoodenDevice:2/3 с TileArcanePressurePlate оригинала. Хозяин (и тот, у
 * кого золотой ключ) щелчком выбирает, на кого она срабатывает: на всех, на всех кроме своих или только на своих.
 * Связанные с теми же людьми чародейские двери открываются, пока на плиту давят.
 */
public class ArcanePressurePlateBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty SETTING = IntegerProperty.create("setting", 0, 2);
    private static final VoxelShape UP = Block.box(1, 0, 1, 15, 1, 15);
    private static final VoxelShape DOWN = Block.box(1, 0, 1, 15, 0.5, 15);
    private static final AABB TOUCH = new AABB(0.125, 0.0, 0.125, 0.875, 0.25, 0.875);

    public ArcanePressurePlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(SETTING, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, SETTING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(POWERED) ? DOWN : UP;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return canSupportRigidBlock(level, below) || canSupportCenter(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos from) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, from);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OwnedBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && !level.isClientSide && level.getBlockEntity(pos) instanceof OwnedBlockEntity owned) {
            owned.setOwner(player);
        }
    }

    /** Щелчок своего меняет, на кого срабатывает плита. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof OwnedBlockEntity owned && owned.manages(player)) {
            owned.setting = (byte) ((owned.setting + 1) % 3);
            owned.sync();
            level.setBlock(pos, state.setValue(SETTING, (int) owned.setting), 3);
            player.displayClientMessage(Component.translatable("thaumcraft.plate.setting." + owned.setting), true);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.1f, 0.9f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && !state.getValue(POWERED)) {
            check(level, pos, state);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            check(level, pos, state);
        }
    }

    /** setStateIfMobInteractsWithPlate оригинала. */
    private void check(Level level, BlockPos pos, BlockState state) {
        OwnedBlockEntity owned = level.getBlockEntity(pos) instanceof OwnedBlockEntity entity ? entity : null;
        int setting = owned == null ? 0 : owned.setting;
        boolean pressed = false;
        List<? extends Entity> entities = setting == 2 ? level.getEntitiesOfClass(Player.class, TOUCH.move(pos))
                : level.getEntities((Entity) null, TOUCH.move(pos));
        for (Entity entity : entities) {
            if (entity.isIgnoringBlockTriggers()) {
                continue;
            }
            boolean friend = entity instanceof Player player && owned != null && owned.allows(player);
            if (setting == 1 && friend || setting == 2 && !friend) {
                continue;
            }
            pressed = true;
            break;
        }
        boolean was = state.getValue(POWERED);
        if (pressed != was) {
            level.setBlock(pos, state.setValue(POWERED, pressed), 3);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
            level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.2f, pressed ? 0.6f : 0.5f);
        }
        if (pressed) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!moving && !state.is(newState.getBlock()) && state.getValue(POWERED)) {
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) && direction == Direction.UP ? 15 : 0;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
    }
}

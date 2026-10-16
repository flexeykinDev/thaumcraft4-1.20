package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.CrucibleBlockEntity;
import thaumcraft.common.registry.ModBlockEntities;

/** Тигель (BlockMetalDevice оригинала, мета 0): котёл, которого коснулся жезл. */
public class CrucibleBlock extends BaseEntityBlock {
    /** Как в оригинале: дно толщиной 5/16 и стенки толщиной 1/8, не доходящие до верха блока. */
    private static final VoxelShape COLLISION = Shapes.or(box(0, 0, 0, 16, 5, 16), box(0, 0, 0, 2, 13.6, 16),
            box(0, 0, 0, 16, 13.6, 2), box(14, 0, 0, 16, 13.6, 16), box(0, 0, 14, 16, 13.6, 16));
    private static final VoxelShape OUTLINE = Shapes.join(Shapes.block(), box(2, 4, 2, 14, 16, 14), BooleanOp.ONLY_FIRST);

    public CrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CRUCIBLE.get(),
                level.isClientSide ? CrucibleBlockEntity::clientTick : CrucibleBlockEntity::serverTick);
    }

    /** Ведро воды наполняет тигель целиком. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.WATER_BUCKET) || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && crucible.fill(CrucibleBlockEntity.CAPACITY) > 0) {
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.33f,
                    1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Предметы в кипящем тигле растворяются, а всё живое обжигается. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) || !crucible.isBoiling()) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            if (item.isAlive() && !item.getPersistentData().getBoolean(CrucibleBlockEntity.OUTPUT_TAG)) {
                crucible.attemptSmelt(item);
            }
        } else if (entity instanceof LivingEntity && entity.tickCount % 10 == 0) {
            entity.hurt(level.damageSources().inFire(), 1f);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f,
                    2f + level.random.nextFloat() * 0.4f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0 && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible && crucible.isBoiling()) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                    0.1f + random.nextFloat() * 0.1f, 1.2f + random.nextFloat() * 0.2f, false);
        }
    }

    /** Сигнал компаратора показывает, насколько тигель заполнен аспектами. */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            int total = crucible.aspectTotal();
            return Math.min(15, Mth.floor(total / (float) CrucibleBlockEntity.MAX_ASPECTS * 14f) + (total > 0 ? 1 : 0));
        }
        return 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            crucible.spillRemnants();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}

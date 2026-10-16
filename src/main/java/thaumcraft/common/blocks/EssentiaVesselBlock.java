package thaumcraft.common.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.EssentiaBlockEntity;
import thaumcraft.common.items.LabelItem;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

/**
 * Перегонный куб (BlockMetalDevice оригинала, мета 1) и запечатанная банка (BlockJar, мета 0).
 * Оба хранят эссенцию одного аспекта; колбой и банкой их наполняют и опустошают сами предметы
 * (см. PhialItem и JarBlockItem), здесь — ярлыки и действия пустой рукой.
 */
public class EssentiaVesselBlock extends BaseEntityBlock {
    private static final VoxelShape JAR_SHAPE = box(3, 0, 3, 13, 12, 13);
    private static final VoxelShape ALEMBIC_SHAPE = box(2, 0, 2, 14, 16, 14);

    private final boolean jar;

    public EssentiaVesselBlock(boolean jar, Properties properties) {
        super(properties);
        this.jar = jar;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return jar ? JAR_SHAPE : ALEMBIC_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return jar ? EssentiaBlockEntity.jar(pos, state) : EssentiaBlockEntity.alembic(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return jar && !level.isClientSide ? createTickerHelper(type, ModBlockEntities.JAR.get(), EssentiaBlockEntity::jarTick) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EssentiaBlockEntity vessel)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        // Shift: сначала снимается ярлык, если он есть
        if (player.isShiftKeyDown() && vessel.getFilter() != null) {
            if (!level.isClientSide) {
                Direction side = vessel.getLabelSide();
                vessel.setFilter(null, side);
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5 + side.getStepX() / 3.0, pos.getY() + 0.5,
                        pos.getZ() + 0.5 + side.getStepZ() / 3.0, new ItemStack(ModItems.LABEL.get())));
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1f, 1.1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ярлык в руке: наклеить
        if (held.getItem() instanceof LabelItem && vessel.getFilter() == null) {
            Aspect onLabel = LabelItem.getAspect(held);
            // пустой ярлык на пустой сосуд ничего не значит
            Aspect chosen = vessel.getAmount() > 0 ? vessel.getAspect() : onLabel;
            if (chosen == null) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                Direction side = hit.getDirection().getAxis().isHorizontal() ? hit.getDirection() : player.getDirection().getOpposite();
                vessel.setFilter(chosen, side);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1f, 0.9f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            // Shift и пустая рука: вылить всё
            if (!level.isClientSide && vessel.getAmount() > 0) {
                vessel.clear();
                level.playSound(null, pos, jar ? ModSounds.JAR.get() : ModSounds.ALEMBIC_KNOCK.get(), SoundSource.BLOCKS,
                        jar ? 0.5f : 0.2f, 1f);
                level.playSound(null, pos, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 0.5f,
                        1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (jar) {
            // постучать по банке
            if (level.isClientSide) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.JAR.get(), SoundSource.BLOCKS,
                        0.2f, 1f, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // постучать по кубу: на слух понятно, насколько он полон
        if (!level.isClientSide) {
            float full = vessel.getAmount() / (float) vessel.getCapacity();
            int message = vessel.getAmount() == 0 ? 1 : full < 0.4f ? 2 : full < 0.8f ? 3 : full < 1f ? 4 : 5;
            player.displayClientMessage(Component.translatable("tile.alembic.msg." + message).withStyle(ChatFormatting.DARK_AQUA), true);
            level.playSound(null, pos, ModSounds.ALEMBIC_KNOCK.get(), SoundSource.BLOCKS, 0.2f, 1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Ярлык с разбитого куба выпадает; банка уносит ярлык с собой (он записан в её данных). */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!jar && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EssentiaBlockEntity vessel
                && vessel.getFilter() != null) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(ModItems.LABEL.get())));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EssentiaBlockEntity vessel && vessel.getAmount() > 0) {
            return Mth.floor(vessel.getAmount() / (float) vessel.getCapacity() * 14f) + 1;
        }
        return 0;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}

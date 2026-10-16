package thaumcraft.common.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.OwnedBlockEntity;
import thaumcraft.common.registry.ModSounds;

/**
 * Чародейская дверь — перенос BlockArcaneDoor оригинала. Открывается только хозяину и тем, кому он дал ключ;
 * обычный красный камень на неё не действует — только чародейские нажимные плиты, связанные с теми же людьми.
 * Её не берут взрывы и не ломают иссушитель с драконом.
 */
public class ArcaneDoorBlock extends DoorBlock implements EntityBlock {
    public ArcaneDoorBlock(Properties properties) {
        super(properties, BlockSetType.IRON);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OwnedBlockEntity(pos, state);
    }

    @Nullable
    private static OwnedBlockEntity owned(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof OwnedBlockEntity owned ? owned : null;
    }

    /** Хозяином обеих половин становится тот, кто поставил дверь. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && !level.isClientSide) {
            for (BlockPos half : new BlockPos[]{pos, pos.above()}) {
                OwnedBlockEntity owned = owned(level, half);
                if (owned != null) {
                    owned.setOwner(player);
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        OwnedBlockEntity owned = owned(level, pos);
        if (owned != null && !owned.allows(player)) {
            player.displayClientMessage(Component.translatable("thaumcraft.door.refuses").withStyle(ChatFormatting.DARK_PURPLE,
                    ChatFormatting.ITALIC), true);
            level.playSound(null, pos, ModSounds.DOOR_FAIL.get(), SoundSource.BLOCKS, 0.66f, 1f);
            return InteractionResult.SUCCESS;
        }
        state = state.cycle(OPEN);
        level.setBlock(pos, state, 10);
        level.playSound(null, pos, level.random.nextBoolean() ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                SoundSource.BLOCKS, 1f, level.random.nextFloat() * 0.1f + 0.9f);
        return InteractionResult.SUCCESS;
    }

    /**
     * Обычный сигнал двери безразличен. Соседняя чародейская плита, связанная хоть с кем-то из допущенных к двери,
     * открывает её, пока на плиту давят, и закрывает, когда отпускают.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!(block instanceof ArcanePressurePlateBlock)) {
            return;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        OwnedBlockEntity door = owned(level, lower);
        if (door == null) {
            return;
        }
        int open = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos at = pos.relative(side);
            BlockState plate = level.getBlockState(at);
            OwnedBlockEntity owned = owned(level, at);
            if (!(plate.getBlock() instanceof ArcanePressurePlateBlock) || owned == null
                    || door.users().stream().noneMatch(user -> owned.owner.equals(user) || owned.knows(user))) {
                continue;
            }
            if (plate.getValue(ArcanePressurePlateBlock.POWERED)) {
                open = 1;
                break;
            }
            open = -1;
        }
        if (open != 0 && state.getValue(OPEN) != (open == 1)) {
            level.setBlock(pos, state.setValue(OPEN, open == 1), 10);
            level.levelEvent(null, open == 1 ? 1005 : 1011, pos, 0);
        }
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
    }
}

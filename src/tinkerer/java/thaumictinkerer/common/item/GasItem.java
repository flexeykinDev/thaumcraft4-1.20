package thaumictinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumictinkerer.common.block.GasBlock;

import java.util.function.Supplier;

/** Склянка газа (ItemGas оригинала): щелчок выпускает газ у головы, и он расползается вокруг. */
public class GasItem extends Item {
    private final Supplier<Block> gas;

    public GasItem(Supplier<Block> gas) {
        super(new Item.Properties());
        this.gas = gas;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos pos = BlockPos.containing(player.getX(), player.getY() + 1, player.getZ());
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f,
                0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide && level.isEmptyBlock(pos)) {
            level.setBlock(pos, gas.get().defaultBlockState().setValue(GasBlock.SPREAD, 4), Block.UPDATE_CLIENTS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

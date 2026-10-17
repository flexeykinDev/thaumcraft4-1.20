package thaumictinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumictinkerer.common.block.GasBlock;

/** Рассеиватель испарений (ItemGasRemover оригинала): Shift+щелчок убирает весь газ в кубе 6×6×6 вокруг. */
public class GasRemoverItem extends Item {
    public GasRemoverItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos centre = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-3, -3, -3), centre.offset(2, 2, 2))) {
            if (level.getBlockState(pos).getBlock() instanceof GasBlock gas) {
                if (level.isClientSide) {
                    thaumictinkerer.client.TTClientHooks.gasRemoved(gas.kind(), pos.immutable());
                } else {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), thaumcraft.common.registry.ModSounds.WAND.get(),
                SoundSource.PLAYERS, 0.2f, 1f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

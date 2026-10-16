package thaumcraft.common.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entity.BottleTaintEntity;

/** Порча в бутылке (ItemBottleTaint оригинала): бросается, как зелье; в стопке не больше восьми. */
public class BottleTaintItem extends Item {
    public BottleTaintItem() {
        super(new Item.Properties().stacksTo(8));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide) {
            BottleTaintEntity thrown = new BottleTaintEntity(level, player);
            thrown.setItem(stack);
            // как зелье: медленно и навесом
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20f, 0.5f, 1f);
            level.addFreshEntity(thrown);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

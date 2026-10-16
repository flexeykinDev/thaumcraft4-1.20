package thaumcraft.common.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.AlumentumEntity;

/** Алюментум (ItemResource оригинала, мета 0): топливо в четыре раза лучше угля и метательная взрывчатка. */
public class AlumentumItem extends Item {
    public AlumentumItem() {
        super(new Item.Properties());
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return 6400;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.3f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide) {
            AlumentumEntity thrown = new AlumentumEntity(level, player);
            thrown.setItem(stack);
            // скорость вдвое ниже снежка, как в оригинале
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 0.75f, 1f);
            level.addFreshEntity(thrown);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

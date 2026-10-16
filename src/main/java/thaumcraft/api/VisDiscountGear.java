package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

/** Надетая вещь, которая удешевляет заклинания жезла (IVisDiscountGear оригинала). */
public interface VisDiscountGear {
    /** Скидка в процентах; aspect == null — вопрос «вообще», для подсказки предмета. */
    int getVisDiscount(ItemStack stack, @Nullable Player player, @Nullable Aspect aspect);
}

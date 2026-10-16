package thaumcraft.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Шлем, показывающий содержимое сосудов значками над блоком (IGoggles оригинала). */
public interface Goggles {
    boolean showIngamePopups(ItemStack stack, LivingEntity wearer);

    /** Надет ли на голову предмет, показывающий значки. */
    static boolean wornBy(Player player) {
        ItemStack head = player.getInventory().getArmor(3);
        return head.getItem() instanceof Goggles goggles && goggles.showIngamePopups(head, player);
    }
}

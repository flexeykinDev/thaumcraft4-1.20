package thaumcraft.api.nodes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Шлем, сквозь который видны узлы ауры и лучи сети вис (IRevealer оригинала). */
public interface Revealer {
    boolean showNodes(ItemStack stack, LivingEntity wearer);

    /** Надет ли на голову предмет, показывающий узлы. */
    static boolean wornBy(Player player) {
        ItemStack head = player.getInventory().getArmor(3);
        return head.getItem() instanceof Revealer revealer && revealer.showNodes(head, player);
    }
}

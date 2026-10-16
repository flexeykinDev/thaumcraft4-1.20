package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * IWarpingGear оригинала: вещь, которая, пока её держат в руке, носят как броню или амулет, добавляет
 * владельцу временное искажение. В подсказке такой вещи пишется «Искажение N».
 */
public interface WarpingGear {
    int getWarp(ItemStack stack, Player player);
}

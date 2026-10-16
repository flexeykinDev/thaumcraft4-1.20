package thaumcraft.api;

import net.minecraft.world.item.ItemStack;

/** Надетая вещь, дающая рунный щит (IRunicArmor оригинала). */
public interface RunicArmor {
    /** Сколько единиц щита даёт вещь сама по себе. */
    int getRunicCharge(ItemStack stack);
}

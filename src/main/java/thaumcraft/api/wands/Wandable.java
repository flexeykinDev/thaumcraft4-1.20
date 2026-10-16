package thaumcraft.api.wands;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/** Устройство, которое что-то делает от щелчка жезлом (IWandable оригинала): например, поворачивается. */
public interface Wandable {
    InteractionResult onWand(UseOnContext context);
}

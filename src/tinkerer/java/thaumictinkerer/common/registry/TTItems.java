package thaumictinkerer.common.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import thaumictinkerer.ThaumicTinkerer;

/** Предметы Thaumic Tinkerer. */
public final class TTItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ThaumicTinkerer.MODID);

    private TTItems() {
    }
}

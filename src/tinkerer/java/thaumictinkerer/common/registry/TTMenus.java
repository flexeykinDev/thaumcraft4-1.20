package thaumictinkerer.common.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.AspectAnalyzerBlock;

/** Окна Thaumic Tinkerer. */
public final class TTMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ThaumicTinkerer.MODID);

    public static final RegistryObject<MenuType<AspectAnalyzerBlock.Menu>> ASPECT_ANALYZER = MENUS.register("aspect_analyzer",
            () -> IForgeMenuType.create((id, inventory, data) -> new AspectAnalyzerBlock.Menu(id, inventory, data.readBlockPos())));

    private TTMenus() {
    }
}

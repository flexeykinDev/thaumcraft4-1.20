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

    public static final RegistryObject<MenuType<thaumictinkerer.common.block.EnchanterBlock.Menu>> ENCHANTER = MENUS.register("enchanter",
            () -> IForgeMenuType.create((id, inventory, data) -> new thaumictinkerer.common.block.EnchanterBlock.Menu(id, inventory,
                    data.readBlockPos())));

    public static final RegistryObject<MenuType<thaumictinkerer.common.block.MagnetBlock.Menu>> MOB_MAGNET = MENUS.register("mob_magnet",
            () -> IForgeMenuType.create((id, inventory, data) -> new thaumictinkerer.common.block.MagnetBlock.Menu(id, inventory, data.readBlockPos())));
    public static final RegistryObject<MenuType<thaumictinkerer.common.block.RemotePlacerBlock.Menu>> REMOTE_PLACER = MENUS.register("remote_placer",
            () -> IForgeMenuType.create((id, inventory, data) -> new thaumictinkerer.common.block.RemotePlacerBlock.Menu(id, inventory, data.readBlockPos())));
    public static final RegistryObject<MenuType<thaumictinkerer.common.block.AnimationTabletBlock.Menu>> ANIMATION_TABLET = MENUS.register("animation_tablet",
            () -> IForgeMenuType.create((id, inventory, data) -> new thaumictinkerer.common.block.AnimationTabletBlock.Menu(id, inventory, data.readBlockPos())));

    public static final RegistryObject<MenuType<thaumictinkerer.common.item.kami.IchorPouchItem.Menu>> ICHOR_POUCH = MENUS.register("ichor_pouch",
            () -> IForgeMenuType.create((id, inventory, data) -> new thaumictinkerer.common.item.kami.IchorPouchItem.Menu(id, inventory, data.readVarInt())));

    public static final RegistryObject<MenuType<thaumictinkerer.common.block.WarpGateBlock.Menu>> WARP_GATE = MENUS.register("warp_gate",
            () -> IForgeMenuType.create((id, inventory, data) -> new thaumictinkerer.common.block.WarpGateBlock.Menu(id, inventory, data.readBlockPos())));

    private TTMenus() {
    }
}

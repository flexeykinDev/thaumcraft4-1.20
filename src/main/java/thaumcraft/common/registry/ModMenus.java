package thaumcraft.common.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.menu.ResearchTableMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Thaumcraft.MODID);

    public static final RegistryObject<MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH = MENUS.register("arcane_workbench",
            () -> IForgeMenuType.create((id, inventory, data) -> new ArcaneWorkbenchMenu(id, inventory, data.readBlockPos())));

    public static final RegistryObject<MenuType<ResearchTableMenu>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> IForgeMenuType.create((id, inventory, data) -> new ResearchTableMenu(id, inventory, data.readBlockPos())));

    public static final RegistryObject<MenuType<thaumcraft.common.menu.AlchemicalFurnaceMenu>> ALCHEMICAL_FURNACE =
            MENUS.register("alchemical_furnace", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.AlchemicalFurnaceMenu(id, inventory, data.readBlockPos())));

    public static final RegistryObject<MenuType<thaumcraft.common.menu.FocusPouchMenu>> FOCUS_POUCH =
            MENUS.register("focus_pouch", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.FocusPouchMenu(id, inventory, data.readVarInt())));

    public static final RegistryObject<MenuType<thaumcraft.common.menu.FocalManipulatorMenu>> FOCAL_MANIPULATOR =
            MENUS.register("focal_manipulator", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.FocalManipulatorMenu(id, inventory, data.readBlockPos())));

    public static final RegistryObject<MenuType<thaumcraft.common.menu.HoverHarnessMenu>> HOVER_HARNESS =
            MENUS.register("hover_harness", () -> IForgeMenuType.create(thaumcraft.common.menu.HoverHarnessMenu::new));
    public static final RegistryObject<MenuType<thaumcraft.common.menu.ArcaneBoreMenu>> ARCANE_BORE =
            MENUS.register("arcane_bore", () -> IForgeMenuType.create(thaumcraft.common.menu.ArcaneBoreMenu::new));
    public static final RegistryObject<MenuType<thaumcraft.common.menu.HandMirrorMenu>> HAND_MIRROR =
            MENUS.register("hand_mirror", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.HandMirrorMenu(id, inventory)));
    public static final RegistryObject<MenuType<thaumcraft.common.menu.SpaMenu>> SPA =
            MENUS.register("spa", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.SpaMenu(id, inventory, data.readBlockPos())));
    public static final RegistryObject<MenuType<thaumcraft.common.menu.DeconstructionTableMenu>> DECONSTRUCTION_TABLE =
            MENUS.register("deconstruction_table", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.DeconstructionTableMenu(id, inventory, data.readBlockPos())));
    public static final RegistryObject<MenuType<thaumcraft.common.menu.ThaumatoriumMenu>> THAUMATORIUM =
            MENUS.register("thaumatorium", () -> IForgeMenuType.create((id, inventory, data) ->
                    new thaumcraft.common.menu.ThaumatoriumMenu(id, inventory, data.readBlockPos())));
    public static final RegistryObject<MenuType<thaumcraft.common.menu.TrunkMenu>> TRUNK =
            MENUS.register("trunk", () -> IForgeMenuType.create((id, inventory, data) -> new thaumcraft.common.menu.TrunkMenu(id, inventory,
                    (thaumcraft.common.entity.golem.TravelingTrunk) inventory.player.level().getEntity(data.readVarInt()), data.readVarInt())));
    /** Окно голема: по сети передаётся номер существа. */
    public static final RegistryObject<MenuType<thaumcraft.common.menu.GolemMenu>> GOLEM =
            MENUS.register("golem", () -> IForgeMenuType.create((id, inventory, data) -> new thaumcraft.common.menu.GolemMenu(id, inventory,
                    (thaumcraft.common.entity.golem.Golem) inventory.player.level().getEntity(data.readVarInt()))));
    /** Торг с печом: по сети передаётся номер существа. */
    public static final RegistryObject<MenuType<thaumcraft.common.menu.PechMenu>> PECH =
            MENUS.register("pech", () -> IForgeMenuType.create((id, inventory, data) -> new thaumcraft.common.menu.PechMenu(id, inventory,
                    inventory.player.level().getEntity(data.readVarInt()) instanceof thaumcraft.common.entity.Pech pech ? pech : null)));

    private ModMenus() {
    }
}

package thaumictinkerer.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.client.gui.AspectAnalyzerScreen;
import thaumictinkerer.client.render.FunnelRenderer;
import thaumictinkerer.client.render.PlatformRenderer;
import thaumictinkerer.client.render.RepairerRenderer;
import thaumictinkerer.common.item.CleansingTalismanItem;
import thaumictinkerer.common.item.SpellClothItem;
import thaumictinkerer.common.item.XpTalismanItem;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTItems;
import thaumictinkerer.common.registry.TTMenus;

/** Клиентская регистрация аддона: отрисовщики блоков, окна, цвета и состояния предметов. */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TTClient {
    private TTClient() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (Boolean.getBoolean("thaumcraft.autoshot")) {
            thaumictinkerer.client.debug.TTAutoShot.register();
            thaumictinkerer.client.debug.TTAutoShot2.register();
        }
        event.enqueueWork(() -> {
            MenuScreens.register(TTMenus.ASPECT_ANALYZER.get(), AspectAnalyzerScreen::new);
            MenuScreens.register(TTMenus.ENCHANTER.get(), thaumictinkerer.client.gui.EnchanterScreen::new);
            // включённые талисманы выглядят иначе (вторая иконка оригинала)
            ItemProperties.register(TTItems.XP_TALISMAN.get(), ThaumicTinkerer.id("active"),
                    (stack, level, entity, seed) -> XpTalismanItem.isAbsorbing(stack) ? 1f : 0f);
            ItemProperties.register(TTItems.CLEANSING_TALISMAN.get(), ThaumicTinkerer.id("active"),
                    (stack, level, entity, seed) -> CleansingTalismanItem.isEnabled(stack) ? 1f : 0f);
        });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TTBlockEntities.REPAIRER.get(), RepairerRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.FUNNEL.get(), FunnelRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.PLATFORM.get(), PlatformRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.ENCHANTER.get(), thaumictinkerer.client.render.EnchanterRenderer::new);
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0 ? SpellClothItem.color(stack) : -1, TTItems.SPELL_CLOTH.get());
    }
}

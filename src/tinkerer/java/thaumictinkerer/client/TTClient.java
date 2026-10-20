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
            thaumictinkerer.client.debug.TTAutoShot2b.register();
            thaumictinkerer.client.debug.TTAutoShot2c.register();
            thaumictinkerer.client.debug.TTAutoShot2d.register();
            thaumictinkerer.client.debug.TTAutoShot3a.register();
            thaumictinkerer.client.debug.TTAutoShot3b.register();
        }
        event.enqueueWork(() -> {
            MenuScreens.register(TTMenus.ASPECT_ANALYZER.get(), AspectAnalyzerScreen::new);
            MenuScreens.register(TTMenus.ICHOR_POUCH.get(), thaumictinkerer.client.gui.IchorPouchScreen::new);
            MenuScreens.register(TTMenus.ENCHANTER.get(), thaumictinkerer.client.gui.EnchanterScreen::new);
            MenuScreens.register(TTMenus.MOB_MAGNET.get(), thaumictinkerer.client.gui.MachineScreens.MobMagnet::new);
            MenuScreens.register(TTMenus.REMOTE_PLACER.get(), thaumictinkerer.client.gui.MachineScreens.RemotePlacer::new);
            MenuScreens.register(TTMenus.ANIMATION_TABLET.get(), thaumictinkerer.client.gui.MachineScreens.AnimationTablet::new);
            // включённые талисманы выглядят иначе (вторая иконка оригинала)
            ItemProperties.register(TTItems.XP_TALISMAN.get(), ThaumicTinkerer.id("active"),
                    (stack, level, entity, seed) -> XpTalismanItem.isAbsorbing(stack) ? 1f : 0f);
            ItemProperties.register(TTItems.CLEANSING_TALISMAN.get(), ThaumicTinkerer.id("active"),
                    (stack, level, entity, seed) -> CleansingTalismanItem.isEnabled(stack) ? 1f : 0f);
            // аспект души: номер аспекта выбирает картинку (aspect = номер + 1, 0 — без аспекта)
            for (var item : java.util.List.of(TTItems.SOUL_ASPECT.get(), TTItems.CONDENSED_SOUL_ASPECT.get(), TTItems.INFUSED_SOUL_ASPECT.get())) {
                ItemProperties.register(item, ThaumicTinkerer.id("aspect"), (stack, level, entity, seed) ->
                        thaumictinkerer.common.item.SoulAspectItem.ASPECTS.indexOf(thaumictinkerer.common.item.SoulAspectItem.aspect(stack)) + 1);
            }
            // наполненные семена: раскраска по аспекту (0 — аер … 6 — прочие)
            ItemProperties.register(TTItems.INFUSED_SEEDS.get(), ThaumicTinkerer.id("seed"), (stack, level, entity, seed) ->
                    thaumictinkerer.common.block.InfusedGrainBlock.texture(thaumictinkerer.common.item.InfusedSeedsItem.aspect(stack)));
            // пробуждённые ихорные инструменты: картинка по режиму, кольцо «Чёрная дыра» — включено или нет
            for (var tool : java.util.List.of(TTItems.ICHOR_PICK_GEM, TTItems.ICHOR_SHOVEL_GEM, TTItems.ICHOR_AXE_GEM, TTItems.ICHOR_SWORD_GEM)) {
                ItemProperties.register(tool.get(), ThaumicTinkerer.id("mode"),
                        (stack, level, entity, seed) -> thaumictinkerer.common.item.kami.IchorTools.mode(stack));
            }
            ItemProperties.register(TTItems.BLOCK_TALISMAN.get(), ThaumicTinkerer.id("active"),
                    (stack, level, entity, seed) -> thaumictinkerer.common.item.kami.BlockTalismanItem.isActive(stack) ? 1f : 0f);
            ItemProperties.register(TTItems.BLOOD_SWORD.get(), ThaumicTinkerer.id("active"),
                    (stack, level, entity, seed) -> thaumictinkerer.common.item.BloodSwordItem.isHarvesting(stack) ? 1f : 0f);
        });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TTBlockEntities.REPAIRER.get(), RepairerRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.FUNNEL.get(), FunnelRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.PLATFORM.get(), PlatformRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.ENCHANTER.get(), thaumictinkerer.client.render.EnchanterRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.MAGNET.get(), thaumictinkerer.client.render.MagnetRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.ANIMATION_TABLET.get(), thaumictinkerer.client.render.AnimationTabletRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.TRANSVECTOR_INTERFACE.get(), PlatformRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.TRANSVECTOR_DISLOCATOR.get(), PlatformRenderer::new);
        event.registerBlockEntityRenderer(TTBlockEntities.REMOTE_PLACER.get(), PlatformRenderer::new);
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0 ? SpellClothItem.color(stack) : -1, TTItems.SPELL_CLOTH.get());
    }
}

package thaumcraft.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import thaumcraft.common.items.ThaumometerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.client.render.NodeRenderer;
import thaumcraft.client.render.ArcaneWorkbenchRenderer;
import thaumcraft.client.gui.ArcaneWorkbenchScreen;
import thaumcraft.client.gui.ResearchTableScreen;
import thaumcraft.common.items.ResearchNotesItem;
import net.minecraft.client.renderer.item.ItemProperties;
import thaumcraft.common.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.common.registry.ModBlockEntities;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import thaumcraft.client.gui.HudOverlay;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.AspectTooltip;
import thaumcraft.common.aspects.AspectBonus;
import thaumcraft.common.research.ScanManager;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ClientEvents {

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        /** Смена фокуса жезла. В оригинале это F, но в 1.20 на F стоит смена рук, поэтому по умолчанию R. */
        public static final net.minecraft.client.KeyMapping CHANGE_FOCUS = new net.minecraft.client.KeyMapping(
                "key.thaumcraft.change_focus", org.lwjgl.glfw.GLFW.GLFW_KEY_R, "key.categories.thaumcraft");

        /** Переключатель жезла (G, как в оригинале): размеры области для улучшения «Архитектор». */
        public static final net.minecraft.client.KeyMapping MISC_TOGGLE = new net.minecraft.client.KeyMapping(
                "key.thaumcraft.misc_toggle", org.lwjgl.glfw.GLFW.GLFW_KEY_G, "key.categories.thaumcraft");

        /** Парение летательной упряжи (H, как в оригинале). */
        public static final net.minecraft.client.KeyMapping HOVER = new net.minecraft.client.KeyMapping(
                "key.thaumcraft.hover", org.lwjgl.glfw.GLFW.GLFW_KEY_H, "key.categories.thaumcraft");

        @SubscribeEvent
        public static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(CHANGE_FOCUS);
            event.register(MISC_TOGGLE);
            event.register(HOVER);
            // событие приходит при старте клиента — самое время завести позу руки с жезлом
            thaumcraft.client.render.WandArmPose.init();
        }

        @SubscribeEvent
        public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(AspectTooltip.class, AspectTooltip.Renderer::new);
        }

        /** Защищённый блок получает модель, повторяющую чужой блок. */
        @SubscribeEvent
        public static void replaceModels(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
            event.getModels().put(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(
                    ModBlocks.WARDED.get().defaultBlockState()), new thaumcraft.client.render.WardedModel());
            // защищённое стекло: рамка, общая для соседних стёкол
            event.getModels().put(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(
                    ModBlocks.WARDED_GLASS.get().defaultBlockState()), new thaumcraft.client.render.WardedGlassModel());
            // камни Запредельных земель: вид грани зависит от места
            eldritchStone(event, ModBlocks.ANCIENT_STONE.get(), thaumcraft.client.render.EldritchStoneModel.Mode.RANDOM, "es_1", "es_2", "es_3", "es_4");
            eldritchStone(event, ModBlocks.ANCIENT_STONE_SEALED.get(), thaumcraft.client.render.EldritchStoneModel.Mode.RANDOM, "es_1", "es_2", "es_3", "es_4");
            eldritchStone(event, ModBlocks.ANCIENT_ROCK.get(), thaumcraft.client.render.EldritchStoneModel.Mode.PARITY, "er_1", "er_2", "er_3", "er_4");
            eldritchStone(event, ModBlocks.RUNED_STONE.get(), thaumcraft.client.render.EldritchStoneModel.Mode.RANDOM, "es_5", "es_6", "es_7", "es_8");
            eldritchStone(event, ModBlocks.GLOWING_CRUSTED_STONE.get(), thaumcraft.client.render.EldritchStoneModel.Mode.INSET, "es_i_1");
            eldritchStone(event, ModBlocks.GLYPHED_STONE.get(), thaumcraft.client.render.EldritchStoneModel.Mode.INSET, "es_i_2");
        }

        /** Оборачивает модели блока (в мире и в руке) в {@link thaumcraft.client.render.EldritchStoneModel}. */
        private static void eldritchStone(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event, net.minecraft.world.level.block.Block block,
                                          thaumcraft.client.render.EldritchStoneModel.Mode mode, String... textures) {
            var models = event.getModels();
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
            java.util.List<net.minecraft.resources.ResourceLocation> keys = new java.util.ArrayList<>();
            for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
                keys.add(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state));
            }
            keys.add(new net.minecraft.client.resources.model.ModelResourceLocation(id, "inventory"));
            for (net.minecraft.resources.ResourceLocation key : keys) {
                net.minecraft.client.resources.model.BakedModel original = models.get(key);
                if (original != null) {
                    models.put(key, new thaumcraft.client.render.EldritchStoneModel(original, mode, textures));
                }
            }
        }

        /** Механизм летательной упряжи на спине у игроков обоих телосложений. */
        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void addLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
                if (renderer != null) {
                    renderer.addLayer(new thaumcraft.client.render.HoverHarnessLayer(renderer));
                }
            }
        }

        /** Небо Запредельных земель. */
        @SubscribeEvent
        public static void registerDimensionEffects(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
            event.register(thaumcraft.Thaumcraft.id("outer_lands"), new thaumcraft.client.OuterLandsEffects());
        }

        /** Модели, которые рисуют рендереры блоков, а не сами блоки. */
        @SubscribeEvent
        public static void registerModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
            event.register(thaumcraft.client.render.CrystallizerRenderer.BODY);
            event.register(thaumcraft.client.render.CrystallizerRenderer.CRYSTAL);
            event.register(thaumcraft.client.render.FrostShardRenderer.MODEL);
            event.register(thaumcraft.client.render.GolemRenderer.BUCKET);
            event.register(thaumcraft.client.render.MirrorRenderer.FRAME);
            event.register(thaumcraft.client.render.HoverHarnessLayer.BACK);
            event.register(thaumcraft.client.render.FluxScrubberRenderer.CAP);
            event.register(thaumcraft.client.render.FluxScrubberRenderer.TIP);
            event.register(thaumcraft.client.render.EssentiaReservoirRenderer.FRAME);
            event.register(thaumcraft.client.render.EldritchObeliskRenderer.CAP);
            event.register(thaumcraft.client.render.EldritchObeliskRenderer.CAP_OUTER);
            event.register(thaumcraft.client.render.EldritchObeliskRenderer.CAP_ALTAR);
            event.register(thaumcraft.client.render.CrustedOpeningRenderer.VENT);
            event.register(thaumcraft.client.render.StrangeCrystalsRenderer.BASE);
            event.register(thaumcraft.client.render.StrangeCrystalsRenderer.CRYSTAL);
            event.register(thaumcraft.client.render.AdvancedAlchemyFurnaceRenderer.BASE);
            event.register(thaumcraft.client.render.AdvancedAlchemyFurnaceRenderer.BASE_ON);
            event.register(thaumcraft.client.render.AdvancedAlchemyFurnaceRenderer.TANK);
            event.register(thaumcraft.client.render.AdvancedAlchemyFurnaceRenderer.TANK_ON);
            event.register(thaumcraft.client.render.MirrorRenderer.FRAME_ESSENTIA);
            for (net.minecraft.resources.ResourceLocation model : thaumcraft.client.render.VisNetRenderers.MODELS) {
                event.register(model);
            }
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.NODE.get(), NodeRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.MANA_POD.get(), thaumcraft.client.render.ManaPodRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ETHEREAL_BLOOM.get(), thaumcraft.client.render.EtherealBloomRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.HUNGRY_CHEST.get(), thaumcraft.client.render.HungryChestRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.MIRROR.get(), thaumcraft.client.render.MirrorRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_BORE.get(), thaumcraft.client.render.ArcaneBoreRenderer.Bore::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_BORE_BASE.get(), thaumcraft.client.render.ArcaneBoreRenderer.Base::new);
            event.registerBlockEntityRenderer(ModBlockEntities.THAUM_BANNER.get(), thaumcraft.client.render.ThaumBannerRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.FLUX_SCRUBBER.get(), thaumcraft.client.render.FluxScrubberRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ESSENTIA_RESERVOIR.get(), thaumcraft.client.render.EssentiaReservoirRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.NODE_JAR.get(), thaumcraft.client.render.NodeJarRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ELDRITCH_ALTAR.get(), thaumcraft.client.render.EldritchObeliskRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ELDRITCH_OBELISK.get(), thaumcraft.client.render.EldritchObeliskRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ELDRITCH_CAP.get(), thaumcraft.client.render.EldritchObeliskRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CRUSTED_OPENING.get(), thaumcraft.client.render.CrustedOpeningRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ELDRITCH_VOID.get(), thaumcraft.client.render.EldritchVoidRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ELDRITCH_PORTAL.get(), thaumcraft.client.render.EldritchPortalRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ANCIENT_LOCK.get(), thaumcraft.client.render.AncientLockRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.STRANGE_CRYSTALS.get(), thaumcraft.client.render.StrangeCrystalsRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_ALCHEMY_FURNACE.get(), thaumcraft.client.render.AdvancedAlchemyFurnaceRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.BRAIN_JAR.get(), thaumcraft.client.render.BrainJarRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.THAUMATORIUM.get(), thaumcraft.client.render.ThaumatoriumRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.DECONSTRUCTION_TABLE.get(),
                    thaumcraft.client.render.DeconstructionTableRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_WORKBENCH.get(), ArcaneWorkbenchRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CRUCIBLE.get(), thaumcraft.client.render.CrucibleRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.JAR.get(), thaumcraft.client.render.JarRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ALEMBIC.get(), thaumcraft.client.render.JarRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.TUBE.get(), thaumcraft.client.render.TubeRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CENTRIFUGE.get(), thaumcraft.client.render.CentrifugeRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.BELLOWS.get(), thaumcraft.client.render.BellowsRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CRYSTALLIZER.get(), thaumcraft.client.render.CrystallizerRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.HOLE.get(), thaumcraft.client.render.HoleRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.FOCAL_MANIPULATOR.get(),
                    thaumcraft.client.render.FocalManipulatorRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.NODE_STABILIZER.get(), thaumcraft.client.render.VisNetRenderers.Stabilizer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.NODE_TRANSDUCER.get(), thaumcraft.client.render.VisNetRenderers.Transducer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.VIS_RELAY.get(), thaumcraft.client.render.VisNetRenderers.Relay::new);
            event.registerBlockEntityRenderer(ModBlockEntities.WARDED.get(), thaumcraft.client.render.WardedRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL.get(), thaumcraft.client.render.PedestalRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.CRYSTAL.get(), thaumcraft.client.render.CrystalRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.VIS_CHARGE_RELAY.get(), thaumcraft.client.render.VisNetRenderers.ChargeRelay::new);
            event.registerBlockEntityRenderer(ModBlockEntities.WAND_PEDESTAL.get(), thaumcraft.client.render.WandPedestalRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.INFUSION_MATRIX.get(),
                    thaumcraft.client.render.InfusionMatrixRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.MIND_SPIDER.get(),
                    thaumcraft.client.render.MindSpiderRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ALUMENTUM.get(),
                    net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.BOTTLE_TAINT.get(),
                    net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.FIRE_BAT.get(),
                    thaumcraft.client.render.FireBatRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.FOCUS_ORB.get(),
                    thaumcraft.client.render.FocusOrbRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINT_SPIDER.get(),
                    thaumcraft.client.render.TaintRenderers.Spider::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINT_SPORE.get(),
                    thaumcraft.client.render.TaintRenderers.Spore::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINT_SPORE_SWARMER.get(),
                    thaumcraft.client.render.TaintRenderers.Swarmer::new);
            // рой порчи тела не имеет: его видно только по мошкам
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINT_SWARM.get(),
                    net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTED_COW.get(), thaumcraft.client.render.TaintBeastRenderers.Cow::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTED_PIG.get(), thaumcraft.client.render.TaintBeastRenderers.Pig::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTED_SHEEP.get(), thaumcraft.client.render.TaintBeastRenderers.Sheep::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTED_CHICKEN.get(), thaumcraft.client.render.TaintBeastRenderers.Chicken::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTED_VILLAGER.get(), thaumcraft.client.render.TaintBeastRenderers.Villager::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTED_CREEPER.get(), thaumcraft.client.render.TaintBeastRenderers.Creeper::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTACLE.get(), thaumcraft.client.render.TaintBeastRenderers.TaintacleRenderer::big);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTACLE_SMALL.get(), thaumcraft.client.render.TaintBeastRenderers.TaintacleRenderer::small);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get(), thaumcraft.client.render.TaintBeastRenderers.Slime::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ASPECT_ORB.get(), thaumcraft.client.render.AspectOrbRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.BRAINY_ZOMBIE.get(), thaumcraft.client.render.BrainyZombieRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ELDRITCH_CRAB.get(), thaumcraft.client.render.EldritchCrabRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.INHABITED_ZOMBIE.get(), thaumcraft.client.render.CultistRenderers.Zombie::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.CULTIST_KNIGHT.get(), thaumcraft.client.render.CultistRenderers.Cultist::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.CULTIST_CLERIC.get(), thaumcraft.client.render.CultistRenderers.Cultist::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.CULTIST_LEADER.get(), thaumcraft.client.render.CultistRenderers.Cultist::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.GOLEM_ORB.get(), thaumcraft.client.render.CultistRenderers.Orb::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get(), thaumcraft.client.render.EldritchGuardianRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ELDRITCH_ORB.get(), thaumcraft.client.render.EldritchOrbRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ELDRITCH_WARDEN.get(), thaumcraft.client.render.EldritchGuardianRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.ELDRITCH_GOLEM.get(), thaumcraft.client.render.EldritchGolemRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.CULTIST_PORTAL.get(), thaumcraft.client.render.CultistPortalRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TAINTACLE_GIANT.get(),
                    context -> (net.minecraft.client.renderer.entity.EntityRenderer) thaumcraft.client.render.TaintBeastRenderers.TaintacleRenderer.giant(context));
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.GIANT_BRAINY_ZOMBIE.get(), thaumcraft.client.render.BrainyZombieRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.WISP.get(), thaumcraft.client.render.WispRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.PECH.get(), thaumcraft.client.render.PechRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.GOLEM.get(), thaumcraft.client.render.GolemRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.TRAVELING_TRUNK.get(), thaumcraft.client.render.TrunkRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.PRIMAL_ARROW.get(),
                    thaumcraft.client.render.PrimalArrowRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.FOLLOWING_ITEM.get(),
                    net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.GOLEM_DART.get(),
                    context -> new net.minecraft.client.renderer.entity.ArrowRenderer<thaumcraft.common.entity.golem.GolemDart>(context) {
                        @Override
                        public net.minecraft.resources.ResourceLocation getTextureLocation(thaumcraft.common.entity.golem.GolemDart dart) {
                            return net.minecraft.client.renderer.entity.TippableArrowRenderer.NORMAL_ARROW_LOCATION;
                        }
                    });
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.GOLEM_BOBBER.get(),
                    thaumcraft.client.render.GolemBobberRenderer::new);
            // сгусток печа и первозданная сфера состоят из одних частиц
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.PECH_BLAST.get(),
                    net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.PRIMAL_ORB.get(),
                    net.minecraft.client.renderer.entity.NoopRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.EMBER.get(), thaumcraft.client.render.EmberRenderer::new);
            event.registerEntityRenderer(thaumcraft.common.registry.ModEntities.FROST_SHARD.get(),
                    thaumcraft.client.render.FrostShardRenderer::new);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.ARCANE_WORKBENCH.get(), ArcaneWorkbenchScreen::new);
                MenuScreens.register(ModMenus.PECH.get(), thaumcraft.client.gui.PechScreen::new);
                MenuScreens.register(ModMenus.SPA.get(), thaumcraft.client.gui.SpaScreen::new);
                MenuScreens.register(ModMenus.HAND_MIRROR.get(), thaumcraft.client.gui.HandMirrorScreen::new);
                MenuScreens.register(ModMenus.ARCANE_BORE.get(), thaumcraft.client.gui.ArcaneBoreScreen::new);
                MenuScreens.register(ModMenus.HOVER_HARNESS.get(), thaumcraft.client.gui.HoverHarnessScreen::new);
                MenuScreens.register(ModMenus.GOLEM.get(), thaumcraft.client.gui.GolemScreen::new);
                MenuScreens.register(ModMenus.TRUNK.get(), thaumcraft.client.gui.TrunkScreen::new);
                // голем-предмет показывает накладками, что он продвинутый и что в нём есть ядро
                for (RegistryObject<Item> golem : ModItems.GOLEMS) {
                    ItemProperties.register(golem.get(), Thaumcraft.id("advanced"),
                            (stack, level, entity, seed) -> thaumcraft.common.items.golem.GolemPlacerItem.isAdvanced(stack) ? 1f : 0f);
                    ItemProperties.register(golem.get(), Thaumcraft.id("core"),
                            (stack, level, entity, seed) -> thaumcraft.common.items.golem.GolemPlacerItem.hasCore(stack) ? 1f : 0f);
                }
                MenuScreens.register(ModMenus.THAUMATORIUM.get(), thaumcraft.client.gui.ThaumatoriumScreen::new);
                MenuScreens.register(ModMenus.DECONSTRUCTION_TABLE.get(), thaumcraft.client.gui.DeconstructionTableScreen::new);
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(thaumcraft.common.registry.ModFluids.PURIFYING.get(),
                        net.minecraft.client.renderer.RenderType.translucent());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(thaumcraft.common.registry.ModFluids.PURIFYING_FLOWING.get(),
                        net.minecraft.client.renderer.RenderType.translucent());
                MenuScreens.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
                MenuScreens.register(ModMenus.ALCHEMICAL_FURNACE.get(), thaumcraft.client.gui.AlchemicalFurnaceScreen::new);
                MenuScreens.register(ModMenus.FOCUS_POUCH.get(), thaumcraft.client.gui.FocusPouchScreen::new);
                MenuScreens.register(ModMenus.FOCAL_MANIPULATOR.get(), thaumcraft.client.gui.FocalManipulatorScreen::new);
                ItemProperties.register(ModItems.LABEL.get(), Thaumcraft.id("filled"),
                        (stack, level, entity, seed) -> thaumcraft.common.items.LabelItem.getAspect(stack) != null ? 1f : 0f);
                // полная колба рисуется с эссенцией внутри
                // костяной лук натягивается, как обычный: «натянут ли» и «насколько»
                ItemProperties.register(ModItems.BONE_BOW.get(), new net.minecraft.resources.ResourceLocation("pulling"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
                ItemProperties.register(ModItems.BONE_BOW.get(), new net.minecraft.resources.ResourceLocation("pull"),
                        (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0f
                                : (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20f);
                ItemProperties.register(ModItems.SINISTER_STONE.get(), thaumcraft.common.items.SinisterStoneItem.ACTIVE,
                        (stack, level, entity, seed) -> thaumcraft.common.items.SinisterStoneItem.isActive() ? 1f : 0f);
                ItemProperties.register(ModItems.PHIAL.get(), Thaumcraft.id("filled"),
                        (stack, level, entity, seed) -> thaumcraft.common.items.PhialItem.getAspect(stack) != null ? 1f : 0f);
                // решённая заметка выглядит как свиток-открытие
                // связанное зеркало: в стекле видна бездна
                for (var mirror : List.of(ModItems.MAGIC_MIRROR, ModItems.ESSENTIA_MIRROR)) {
                    ItemProperties.register(mirror.get(), Thaumcraft.id("linked"),
                            (stack, level, entity, seed) -> thaumcraft.common.items.MirrorItem.isLinked(stack) ? 1f : 0f);
                }
                ItemProperties.register(ModItems.RESEARCH_NOTES.get(), Thaumcraft.id("complete"),
                        (stack, level, entity, seed) -> ResearchNotesItem.isComplete(stack) ? 1f : 0f);
            });
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("hud", new HudOverlay());
            event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.PLAYER_HEALTH.id(), "runic_shield",
                    new thaumcraft.client.gui.RunicOverlay());
            // тьма по краям экрана во время приступа безумия — под интерфейсом, как ванильная виньетка
            event.registerBelowAll("warp_vignette", (gui, graphics, partialTick, width, height) ->
                    thaumcraft.client.ClientWarp.renderVignette(graphics, width, height));
        }

        // Цвета стихий из BlockCustomOreItem.colors оригинала: воздух, огонь, вода, земля, порядок, энтропия
        /** Цвет травы порченой земли. */
        private static final int TAINT_GRASS_COLOR = 0x6d4189;
        private static final int[] ELEMENT_COLORS = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};

        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
            // мантии: ткань окрашена, отделка нет
            event.register((stack, layer) -> layer == 0 && stack.getItem() instanceof thaumcraft.common.items.armor.RobeArmorItem robe
                            ? robe.getColor(stack) : 0xffffff, ModItems.ROBE_CHESTPLATE.get(), ModItems.ROBE_LEGGINGS.get(),
                    ModItems.ROBE_BOOTS.get());
            event.register((stack, layer) -> layer == 0 && stack.getItem() instanceof thaumcraft.common.items.armor.VoidRobeArmorItem robe
                            ? robe.getColor(stack) : 0xffffff, ModItems.VOID_ROBE_HELMET.get(), ModItems.VOID_ROBE_CHESTPLATE.get(),
                    ModItems.VOID_ROBE_LEGGINGS.get());
            // сальная свеча: воск окрашен, фитиль нет
            for (RegistryObject<Item> candle : ModItems.TALLOW_CANDLES) {
                event.register((stack, layer) -> layer == 0 && stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                        && item.getBlock() instanceof thaumcraft.common.blocks.TallowCandleBlock block ? block.getWaxColor() : 0xffffff, candle.get());
            }
            // зерно маны окрашено в цвет своего аспекта; без аспекта — переливается всеми по очереди
            event.register((stack, layer) -> {
                thaumcraft.api.aspects.Aspect aspect = thaumcraft.common.items.ManaBeanItem.getAspect(stack);
                if (aspect == null) {
                    List<thaumcraft.api.aspects.Aspect> all = List.copyOf(thaumcraft.api.aspects.Aspect.all());
                    aspect = all.get((int) (System.currentTimeMillis() / 500L % all.size()));
                }
                return aspect.getColor();
            }, ModItems.MANA_BEAN.get());
            // эфирная эссенция окрашена так же
            event.register((stack, layer) -> {
                thaumcraft.api.aspects.Aspect aspect = thaumcraft.common.items.WispEssenceItem.getAspect(stack);
                if (aspect == null) {
                    List<thaumcraft.api.aspects.Aspect> all = List.copyOf(thaumcraft.api.aspects.Aspect.all());
                    aspect = all.get((int) (System.currentTimeMillis() / 500L % all.size()));
                }
                return aspect.getColor();
            }, ModItems.WISP_ESSENCE.get());
            List<RegistryObject<Item>> shards = List.of(ModItems.SHARD_AIR, ModItems.SHARD_FIRE, ModItems.SHARD_WATER,
                    ModItems.SHARD_EARTH, ModItems.SHARD_ORDER, ModItems.SHARD_ENTROPY);
            List<RegistryObject<Item>> stones = List.of(ModItems.INFUSED_STONE_AIR, ModItems.INFUSED_STONE_FIRE,
                    ModItems.INFUSED_STONE_WATER, ModItems.INFUSED_STONE_EARTH, ModItems.INFUSED_STONE_ORDER,
                    ModItems.INFUSED_STONE_ENTROPY);
            List<RegistryObject<Item>> deepStones = List.of(ModItems.DEEPSLATE_INFUSED_STONE_AIR, ModItems.DEEPSLATE_INFUSED_STONE_FIRE, ModItems.DEEPSLATE_INFUSED_STONE_WATER,
                    ModItems.DEEPSLATE_INFUSED_STONE_EARTH, ModItems.DEEPSLATE_INFUSED_STONE_ORDER, ModItems.DEEPSLATE_INFUSED_STONE_ENTROPY);
            List<RegistryObject<Item>> rings = List.of(ModItems.APPRENTICE_RING_AIR, ModItems.APPRENTICE_RING_FIRE,
                    ModItems.APPRENTICE_RING_WATER, ModItems.APPRENTICE_RING_EARTH, ModItems.APPRENTICE_RING_ORDER,
                    ModItems.APPRENTICE_RING_ENTROPY);
            for (int i = 0; i < ELEMENT_COLORS.length; i++) {
                int color = ELEMENT_COLORS[i];
                event.register((stack, layer) -> color, shards.get(i).get());
                // кольцо ученика — железное кольцо, окрашенное в цвет своей стихии
                event.register((stack, layer) -> color, rings.get(i).get());
                // у камня красится только светящийся слой (tintindex 0), сам камень остаётся серым
                event.register((stack, layer) -> layer == 0 ? color : 0xffffff, stones.get(i).get(), deepStones.get(i).get());
            }
            event.register((stack, layer) -> FoliageColor.getDefaultColor(), ModItems.GREATWOOD_LEAVES.get());
            event.register((stack, layer) -> layer == 0 ? TAINT_GRASS_COLOR : 0xffffff, ModItems.TAINTED_SOIL.get(),
                    ModItems.TAINT_FIBRES.get(), ModItems.TAINT_GRASS.get(), ModItems.TAINT_PLANT.get(), ModItems.SPORE_STALK.get());
            // кристалл эссенции целиком в цвете аспекта
            event.register((stack, layer) -> {
                thaumcraft.api.aspects.Aspect aspect = thaumcraft.common.items.CrystalEssenceItem.getAspect(stack);
                return aspect != null ? aspect.getColor() : 0xffffff;
            }, ModItems.CRYSTAL_ESSENCE.get());
            // значок на ярлыке — в цвете аспекта
            event.register((stack, layer) -> {
                thaumcraft.api.aspects.Aspect aspect = thaumcraft.common.items.LabelItem.getAspect(stack);
                return layer == 1 && aspect != null ? aspect.getColor() : 0xffffff;
            }, ModItems.LABEL.get());
            // эссенция в колбе — в цвете своего аспекта
            event.register((stack, layer) -> {
                thaumcraft.api.aspects.Aspect aspect = thaumcraft.common.items.PhialItem.getAspect(stack);
                return layer == 1 && aspect != null ? aspect.getColor() : 0xffffff;
            }, ModItems.PHIAL.get());
            // лента на заметке окрашена в цвет главного аспекта исследования
            event.register((stack, layer) -> layer == 1 && stack.hasTag() ? stack.getTag().getInt("color") : 0xffffff,
                    ModItems.RESEARCH_NOTES.get());
        }

        @SubscribeEvent
        public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
            for (var candle : thaumcraft.common.registry.ModBlocks.TALLOW_CANDLES) {
                event.register((state, level, pos, tint) -> tint == 0 && state.getBlock() instanceof thaumcraft.common.blocks.TallowCandleBlock block
                        ? block.getWaxColor() : 0xffffff, candle.get());
            }
            // защищённый блок красится так же, как блок, который он изображает (трава, листва)
            event.register((state, level, pos, layer) -> level != null && pos != null
                            && level.getBlockEntity(pos) instanceof thaumcraft.common.blocks.WardedBlock.Warded warded
                            ? event.getBlockColors().getColor(warded.getMimic(), level, pos, layer) : 0xffffff,
                    ModBlocks.WARDED.get());
            List<RegistryObject<Block>> stones = List.of(ModBlocks.INFUSED_STONE_AIR, ModBlocks.INFUSED_STONE_FIRE,
                    ModBlocks.INFUSED_STONE_WATER, ModBlocks.INFUSED_STONE_EARTH, ModBlocks.INFUSED_STONE_ORDER,
                    ModBlocks.INFUSED_STONE_ENTROPY);
            List<RegistryObject<Block>> deepBlocks = List.of(ModBlocks.DEEPSLATE_INFUSED_STONE_AIR, ModBlocks.DEEPSLATE_INFUSED_STONE_FIRE, ModBlocks.DEEPSLATE_INFUSED_STONE_WATER,
                    ModBlocks.DEEPSLATE_INFUSED_STONE_EARTH, ModBlocks.DEEPSLATE_INFUSED_STONE_ORDER, ModBlocks.DEEPSLATE_INFUSED_STONE_ENTROPY);
            for (int i = 0; i < ELEMENT_COLORS.length; i++) {
                int color = ELEMENT_COLORS[i];
                event.register((state, level, pos, layer) -> layer == 0 ? color : 0xffffff, stones.get(i).get(),
                        deepBlocks.get(i).get());
            }
            event.register((state, level, pos, layer) -> layer != 0 ? 0xffffff : level != null && pos != null
                            ? BiomeColors.getAverageGrassColor(level, pos) : TAINT_GRASS_COLOR,
                    ModBlocks.TAINTED_SOIL.get(), ModBlocks.TAINT_FIBRES.get(), ModBlocks.TAINT_GRASS.get(), ModBlocks.TAINT_PLANT.get(),
                    ModBlocks.SPORE_STALK.get());
            // лента на свитке, лежащем на столе исследований
            event.register((state, level, pos, layer) -> layer == 0 && level != null && pos != null
                    && level.getBlockEntity(pos) instanceof thaumcraft.common.blockentity.ResearchTableBlockEntity table
                    ? table.getNoteColor() : 0xffffff, ModBlocks.RESEARCH_TABLE.get());
            // листва великого древа красится под биом, как ванильная; у серебряного свой цвет в текстуре
            event.register((state, level, pos, layer) -> level != null && pos != null
                    ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor(),
                    ModBlocks.GREATWOOD_LEAVES.get());
        }
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void beforeLiving(net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
            thaumcraft.client.render.WandItemRenderer.renderedEntity = event.getEntity();
        }

        @SubscribeEvent
        public static void afterLiving(net.minecraftforge.client.event.RenderLivingEvent.Post<?, ?> event) {
            thaumcraft.client.render.WandItemRenderer.renderedEntity = null;
        }

        /** Клавиша смены фокуса: с приседанием снимает фокус, иначе открывает круговое меню. */
        @SubscribeEvent
        public static void clientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || mc.player == null) {
                return;
            }
            while (ModBus.HOVER.consumeClick()) {
                if (mc.screen == null && mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem()
                        instanceof thaumcraft.common.items.armor.HoverHarnessItem) {
                    thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.HoverPacket());
                }
            }
            while (ModBus.MISC_TOGGLE.consumeClick()) {
                if (mc.screen == null && (thaumcraft.common.items.wands.Architect.isActive(mc.player.getMainHandItem())
                        || mc.player.getMainHandItem().getItem() instanceof thaumcraft.common.items.tools.ElementalShovelItem)) {
                    thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.ToggleMiscPacket());
                }
            }
            while (ModBus.CHANGE_FOCUS.consumeClick()) {
                net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
                if (mc.screen == null && held.getItem() instanceof thaumcraft.common.items.golem.GolemBellItem) {
                    thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.ChangeFocusPacket(
                            thaumcraft.common.network.ChangeFocusPacket.BELL_RESET));
                    continue;
                }
                if (mc.screen != null || !(held.getItem() instanceof thaumcraft.common.items.wands.WandItem)
                        || thaumcraft.common.items.wands.WandItem.isSceptre(held)) {
                    continue;
                }
                if (mc.player.isShiftKeyDown()) {
                    thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.ChangeFocusPacket(
                            thaumcraft.common.items.wands.FocusManager.REMOVE));
                } else if (!thaumcraft.client.gui.FocusRadialScreen.collect(mc.player).isEmpty()
                        || !thaumcraft.common.items.wands.WandItem.getFocusStack(held).isEmpty()) {
                    mc.setScreen(new thaumcraft.client.gui.FocusRadialScreen(ModBus.CHANGE_FOCUS, false));
                }
            }
        }

        /** Как в оригинале: аспекты видны в подсказке, пока зажат Shift, и только у изученных таумометром предметов. */
        @SubscribeEvent
        public static void gatherTooltip(RenderTooltipEvent.GatherComponents event) {
            ItemStack stack = event.getItemStack();
            // с таумометром на курсоре (сканирование в инвентаре) аспекты видны и без Shift
            Minecraft mc = Minecraft.getInstance();
            boolean scanning = mc.player != null
                    && mc.player.containerMenu.getCarried().getItem() instanceof ThaumometerItem;
            if (stack.isEmpty() || !Screen.hasShiftDown() && !scanning) {
                return;
            }
            if (!ClientKnowledge.get().hasScanned(ScanManager.itemKey(stack))) {
                return;
            }
            AspectList aspects = AspectBonus.apply(stack, ClientItemAspects.get(stack.getItem()));
            if (!aspects.isEmpty()) {
                event.getTooltipElements().add(Either.right(new AspectTooltip(aspects)));
            }
        }
    }

    private ClientEvents() {
    }
}

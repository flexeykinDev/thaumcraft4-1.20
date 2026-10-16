package thaumcraft.common;

import net.minecraft.core.BlockPos;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.world.NodeGenerator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.network.ScanTargetPacket;
import thaumcraft.common.research.ScanManager;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.SyncItemAspectsPacket;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.network.SyncResearchPacket;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.research.ResearchNote;
import thaumcraft.common.research.ResearchRegistry;
import thaumcraft.common.research.PlayerKnowledge;

public final class CommonEvents {

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerKnowledge.class);
        }

        /** Порченые земли в генерации обычного мира: область TerraBlender, см. TaintRegion. */
        @SubscribeEvent
        public static void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
            event.enqueueWork(() -> terrablender.api.Regions.register(new thaumcraft.common.world.TaintRegion()));
            event.enqueueWork(thaumcraft.common.crafting.SmeltingBonuses::register);
            // мантии отмываются от краски в котле с водой, как кожаная броня
            event.enqueueWork(() -> {
                for (var robe : java.util.List.of(thaumcraft.common.registry.ModItems.ROBE_CHESTPLATE,
                        thaumcraft.common.registry.ModItems.ROBE_LEGGINGS, thaumcraft.common.registry.ModItems.ROBE_BOOTS,
                        thaumcraft.common.registry.ModItems.VOID_ROBE_HELMET, thaumcraft.common.registry.ModItems.VOID_ROBE_CHESTPLATE,
                        thaumcraft.common.registry.ModItems.VOID_ROBE_LEGGINGS)) {
                    net.minecraft.core.cauldron.CauldronInteraction.WATER.put(robe.get(),
                            net.minecraft.core.cauldron.CauldronInteraction.DYED_ITEM);
                }
            });
        }

        @SubscribeEvent
        public static void registerAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
            event.put(thaumcraft.common.registry.ModEntities.MIND_SPIDER.get(),
                    thaumcraft.common.entity.MindSpider.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.FIRE_BAT.get(),
                    thaumcraft.common.entity.FireBat.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINT_SPIDER.get(), thaumcraft.common.entity.TaintSpider.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINT_SPORE.get(), thaumcraft.common.entity.TaintSpore.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINT_SPORE_SWARMER.get(),
                    thaumcraft.common.entity.TaintSporeSwarmer.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINT_SWARM.get(), thaumcraft.common.entity.TaintSwarm.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.PECH.get(), thaumcraft.common.entity.Pech.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.ELDRITCH_CRAB.get(), thaumcraft.common.entity.EldritchCrab.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.INHABITED_ZOMBIE.get(), thaumcraft.common.entity.InhabitedZombie.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.CULTIST_KNIGHT.get(), thaumcraft.common.entity.CultistKnight.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.CULTIST_CLERIC.get(), thaumcraft.common.entity.CultistCleric.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.CULTIST_LEADER.get(), thaumcraft.common.entity.CultistLeader.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get(), thaumcraft.common.entity.EldritchGuardian.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.ELDRITCH_WARDEN.get(), thaumcraft.common.entity.EldritchWarden.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.ELDRITCH_GOLEM.get(), thaumcraft.common.entity.EldritchGolem.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.CULTIST_PORTAL.get(), thaumcraft.common.entity.CultistPortal.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTACLE_GIANT.get(), thaumcraft.common.entity.TaintacleGiant.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.GOLEM.get(), thaumcraft.common.entity.golem.Golem.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TRAVELING_TRUNK.get(),
                    thaumcraft.common.entity.golem.TravelingTrunk.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.WISP.get(), thaumcraft.common.entity.Wisp.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.BRAINY_ZOMBIE.get(), thaumcraft.common.entity.BrainyZombie.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.GIANT_BRAINY_ZOMBIE.get(), thaumcraft.common.entity.BrainyZombie.Giant.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTED_COW.get(), thaumcraft.common.entity.TaintedBeasts.Cow.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTED_PIG.get(), thaumcraft.common.entity.TaintedBeasts.Pig.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTED_SHEEP.get(), thaumcraft.common.entity.TaintedBeasts.Sheep.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTED_CHICKEN.get(), thaumcraft.common.entity.TaintedBeasts.Chicken.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTED_VILLAGER.get(), thaumcraft.common.entity.TaintedBeasts.Villager.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTED_CREEPER.get(), thaumcraft.common.entity.TaintCreeper.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTACLE.get(), thaumcraft.common.entity.Taintacle.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.TAINTACLE_SMALL.get(), thaumcraft.common.entity.TaintacleSmall.createAttributes().build());
            event.put(thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get(), thaumcraft.common.entity.ThaumicSlime.createAttributes().build());
        }

        /** Где может появиться моб сам по себе; сами места появления заданы датапаком (forge/biome_modifier). */
        @SubscribeEvent
        public static void registerSpawnPlacements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
            event.register(thaumcraft.common.registry.ModEntities.PECH.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    thaumcraft.common.entity.Pech::canSpawn,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.WISP.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    thaumcraft.common.entity.Wisp::canSpawn,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.BRAINY_ZOMBIE.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.GIANT_BRAINY_ZOMBIE.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.TAINTACLE.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    thaumcraft.common.entity.Taintacle::canSpawn,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.INHABITED_ZOMBIE.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(thaumcraft.common.registry.ModEntities.FIRE_BAT.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    thaumcraft.common.entity.FireBat::canSpawn,
                    net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class ForgeBus {

        /** Сделки тауматурга и менялы. */
        @SubscribeEvent
        public static void villagerTrades(net.minecraftforge.event.village.VillagerTradesEvent event) {
            thaumcraft.common.registry.ModVillagers.trades(event);
        }

        /** Башня мага и дом менялы появляются в деревнях. */
        @SubscribeEvent
        public static void villageHouses(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
            thaumcraft.common.registry.ModVillagers.addVillageHouses(event.getServer());
        }

        /** Дорожные сундуки, которые были рядом с хозяином, когда тот шагнул в другое измерение. */
        private static final java.util.Map<java.util.UUID, java.util.List<thaumcraft.common.entity.golem.TravelingTrunk>> FOLLOWERS =
                new java.util.HashMap<>();

        /** Хозяин уходит в другое измерение: запоминаем его сундуки, которые идут следом и стоят неподалёку. */
        @SubscribeEvent
        public static void ownerLeaves(net.minecraftforge.event.entity.EntityTravelToDimensionEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                java.util.List<thaumcraft.common.entity.golem.TravelingTrunk> near = player.level().getEntitiesOfClass(
                        thaumcraft.common.entity.golem.TravelingTrunk.class, player.getBoundingBox().inflate(32.0),
                        trunk -> trunk.isAlive() && !trunk.getStay() && player.getUUID().equals(trunk.getOwnerUUID()));
                if (near.isEmpty()) {
                    FOLLOWERS.remove(player.getUUID());
                } else {
                    FOLLOWERS.put(player.getUUID(), near);
                }
            }
        }

        /** Хозяин прибыл: сундуки переносятся к нему (travelToDimension оригинала). */
        @SubscribeEvent
        public static void ownerArrives(PlayerEvent.PlayerChangedDimensionEvent event) {
            java.util.List<thaumcraft.common.entity.golem.TravelingTrunk> trunks = FOLLOWERS.remove(event.getEntity().getUUID());
            if (trunks == null || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            for (thaumcraft.common.entity.golem.TravelingTrunk trunk : trunks) {
                if (trunk.isAlive()) {
                    trunk.teleportTo(player.serverLevel(), player.getX(), player.getY() + 0.25, player.getZ(), java.util.Set.of(),
                            trunk.getYRot(), trunk.getXRot());
                }
            }
        }

        /** Соли для ванн, пролежав в воде десять секунд, превращают её в очищающую жидкость. */
        @SubscribeEvent
        public static void itemExpire(net.minecraftforge.event.entity.item.ItemExpireEvent event) {
            if (event.getEntity().getItem().getItem() instanceof thaumcraft.common.items.BathSaltsItem) {
                thaumcraft.common.items.BathSaltsItem.dissolve(event.getEntity());
            }
        }

        /** Полный блок жидкой смерти можно зачерпнуть пустым ведром (fillBucket оригинала). */
        @SubscribeEvent
        public static void fillBucket(net.minecraftforge.event.entity.player.FillBucketEvent event) {
            if (!event.getEmptyBucket().is(net.minecraft.world.item.Items.BUCKET)
                    || !(event.getTarget() instanceof net.minecraft.world.phys.BlockHitResult hit)
                    || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return;
            }
            // сквозь жидкость луч проходит, поэтому смотрим и сам блок, и тот, что перед ним
            for (net.minecraft.core.BlockPos pos : new net.minecraft.core.BlockPos[]{hit.getBlockPos(), hit.getBlockPos().relative(hit.getDirection())}) {
                net.minecraft.world.level.block.state.BlockState state = event.getLevel().getBlockState(pos);
                if (state.is(thaumcraft.common.registry.ModBlocks.LIQUID_DEATH.get()) && state.getValue(thaumcraft.common.blocks.FluxBlock.LEVEL) == 7) {
                    event.getLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    event.setFilledBucket(new net.minecraft.world.item.ItemStack(thaumcraft.common.registry.ModItems.BUCKET_DEATH.get()));
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                    return;
                }
            }
        }

        /** Обычный и магический верстаки: за создание «тёмных» предметов начисляется искажение. */
        @SubscribeEvent
        public static void itemCrafted(PlayerEvent.ItemCraftedEvent event) {
            thaumcraft.common.research.Warp.onCrafted(event.getEntity(), event.getCrafting());
        }

        @SubscribeEvent
        public static void addReloadListeners(AddReloadListenerEvent event) {
            ItemAspectManager.INSTANCE.setContext(event.getServerResources().getRecipeManager(), event.getRegistryAccess());
            event.addListener(ItemAspectManager.INSTANCE);
            event.addListener(new ResearchRegistry.Loader());
        }

        /** Самопроверка для gradlew runGameTestServer: печатает аспекты набора предметов в лог. */
        @SubscribeEvent
        public static void serverStarted(ServerStartedEvent event) {
            if (!Boolean.getBoolean("thaumcraft.selfcheck")) {
                return;
            }
            for (String name : System.getProperty("thaumcraft.selfcheck.items", "").split(",")) {
                Item item = ForgeRegistries.ITEMS.getValue(thaumcraft.common.Res.parse(name.trim()));
                AspectList aspects = ItemAspectManager.INSTANCE.getAspects(new ItemStack(item));
                StringBuilder line = new StringBuilder();
                for (Aspect aspect : aspects.getAspectsSortedByName()) {
                    line.append(' ').append(aspect.getTag()).append(':').append(aspects.getAmount(aspect));
                }
                Thaumcraft.LOGGER.info("SELFCHECK {} ={}", name.trim(), line);
            }
            java.util.Map<Item, AspectList> all = ItemAspectManager.INSTANCE.resolveAll();
            StringBuilder missing = new StringBuilder();
            for (Item item : ForgeRegistries.ITEMS) {
                if (!all.containsKey(item)) {
                    missing.append(' ').append(ForgeRegistries.ITEMS.getKey(item).getPath());
                }
            }
            Thaumcraft.LOGGER.info("SELFCHECK предметов с аспектами: {} из {}", all.size(), ForgeRegistries.ITEMS.getKeys().size());
            Thaumcraft.LOGGER.info("SELFCHECK без аспектов:{}", missing);
            selfCheckTrees(event.getServer().overworld());
            selfCheckNodes(event.getServer().overworld());
            selfCheckArcane(event.getServer().overworld());
            selfCheckMetals(event.getServer().overworld());
            selfCheckNotes();
            selfCheckCrucible(event.getServer().overworld());
            selfCheckOres(event.getServer().overworld());
            selfCheckFlux(event.getServer().overworld());
            selfCheckDistillation(event.getServer().overworld());
            selfCheckInfusion(event.getServer().overworld());
            selfCheckTubes(event.getServer().overworld());
            selfCheckMachinery(event.getServer().overworld());
            selfCheckRods(event.getServer().overworld());
        }

        /** У каждого стержня должны быть предмет, рецепт инфузии и исследование; стержни стихий заряжают свою стихию. */
        private static void selfCheckRods(ServerLevel level) {
            StringBuilder line = new StringBuilder();
            for (String tag : new String[]{"wood", "greatwood", "silverwood", "obsidian", "ice", "quartz", "reed", "blaze", "bone"}) {
                thaumcraft.api.wands.WandRod rod = thaumcraft.api.wands.WandRod.byTag(tag);
                if (rod == null || !ForgeRegistries.ITEMS.containsKey(rod.getItemId())) {
                    Thaumcraft.LOGGER.error("SELFCHECK стержень {}: нет описания или предмета", tag);
                    continue;
                }
                boolean recipe = tag.equals("wood") || tag.equals("greatwood") || level.getRecipeManager()
                        .byKey(Thaumcraft.id("infusion/wand_rod_" + tag)).isPresent();
                boolean research = tag.equals("wood") || ResearchRegistry.get("ROD_" + tag) != null;
                if (!recipe || !research) {
                    Thaumcraft.LOGGER.error("SELFCHECK стержень {}: рецепт={} исследование={}", tag, recipe, research);
                }
                line.append(' ').append(tag).append(':').append(rod.getCapacity())
                        .append(rod.getRechargeAspect() == null ? "" : "+" + rod.getRechargeAspect().getTag());
            }
            Thaumcraft.LOGGER.info("SELFCHECK стержни:{}", line);

            // фокусы: предмет, рецепт и исследование для каждого; цена и пауза — как в оригинале
            StringBuilder fociLine = new StringBuilder();
            for (String focus : new String[]{"fire", "shock", "frost", "excavation", "primal", "trade"}) {
                Item item = ForgeRegistries.ITEMS.getValue(Thaumcraft.id("focus_" + focus));
                boolean recipe = level.getRecipeManager().byKey(Thaumcraft.id("focus_" + focus)).isPresent();
                boolean research = ResearchRegistry.get("FOCUS" + focus.toUpperCase(java.util.Locale.ROOT)) != null;
                if (!(item instanceof thaumcraft.common.items.wands.FocusItem focusItem) || !recipe || !research) {
                    Thaumcraft.LOGGER.error("SELFCHECK фокус {}: предмет={} рецепт={} исследование={}", focus, item, recipe, research);
                    continue;
                }
                ItemStack focusStack = new ItemStack(item);
                fociLine.append(' ').append(focus).append(" [").append(describe(focusItem.getVisCost(focusStack))).append(" /100, пауза ")
                        .append(focusItem.getActivationCooldown(focusStack)).append(" мс]");
            }
            Thaumcraft.LOGGER.info("SELFCHECK фокусы:{}", fociLine);

            // посохи, скипетр и медный набалдашник: сборка в верстаке
            StringBuilder staves = new StringBuilder();
            for (thaumcraft.api.wands.WandRod rod : thaumcraft.api.wands.WandRod.all()) {
                if (rod.isStaff()) {
                    boolean item = ForgeRegistries.ITEMS.containsKey(rod.getItemId());
                    boolean research = ResearchRegistry.get("ROD_" + rod.getTag()) != null;
                    if (!item || !research) {
                        Thaumcraft.LOGGER.error("SELFCHECK посох {}: предмет={} исследование={}", rod.getTag(), item, research);
                    }
                    staves.append(' ').append(rod.getTag()).append(':').append(rod.getCapacity());
                }
            }
            net.minecraft.world.inventory.TransientCraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(
                    new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                        @Override
                        public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
                            return ItemStack.EMPTY;
                        }

                        @Override
                        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                            return false;
                        }
                    }, 3, 3);
            Item cap = ForgeRegistries.ITEMS.getValue(Thaumcraft.id("wand_cap_copper"));
            grid.setItem(1, new ItemStack(cap));
            grid.setItem(5, new ItemStack(cap));
            grid.setItem(6, new ItemStack(cap));
            grid.setItem(4, new ItemStack(ForgeRegistries.ITEMS.getValue(Thaumcraft.id("wand_rod_silverwood"))));
            grid.setItem(2, new ItemStack(ForgeRegistries.ITEMS.getValue(Thaumcraft.id("primal_charm"))));
            var sceptreRecipe = level.getRecipeManager().getRecipeFor(thaumcraft.common.registry.ModRecipes.ARCANE.get(), grid, level);
            ItemStack sceptre = sceptreRecipe.map(r -> r.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
            String sceptreCost = sceptreRecipe.map(r -> describe(r.getAspects(grid))).orElse("");
            grid.clearContent();
            grid.setItem(2, new ItemStack(cap));
            grid.setItem(6, new ItemStack(cap));
            grid.setItem(4, new ItemStack(ForgeRegistries.ITEMS.getValue(Thaumcraft.id("wand_rod_greatwood_staff"))));
            var staffRecipe = level.getRecipeManager().getRecipeFor(thaumcraft.common.registry.ModRecipes.ARCANE.get(), grid, level);
            ItemStack staff = staffRecipe.map(r -> r.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);
            Thaumcraft.LOGGER.info("SELFCHECK посохи:{}; скипетр: {} вмещает {} (ожидается 15000), цена{}; посох: {} вмещает {} (ожидается 12500), это посох={}",
                    staves, sceptre.isEmpty() ? "нет" : sceptre.getHoverName().getString(),
                    sceptre.isEmpty() ? 0 : thaumcraft.common.items.wands.WandItem.getMaxVis(sceptre),
                    sceptreCost,
                    staff.isEmpty() ? "нет" : staff.getHoverName().getString(),
                    staff.isEmpty() ? 0 : thaumcraft.common.items.wands.WandItem.getMaxVis(staff),
                    !staff.isEmpty() && thaumcraft.common.items.wands.WandItem.isStaff(staff));
        }

        /** Центрифуга делит составную эссенцию на компоненты; мехи ускоряют тигель и дают тягу буферу. */
        private static void selfCheckMachinery(ServerLevel level) {
            net.minecraft.core.BlockPos base = new net.minecraft.core.BlockPos(320, 150, 320);
            // банка с 16 victus (aqua + terra) -> труба -> центрифуга -> труба -> буфер с мехами
            level.setBlock(base, thaumcraft.common.registry.ModBlocks.ALEMBIC.get().defaultBlockState(), 3);
            level.setBlock(base.above(), thaumcraft.common.registry.ModBlocks.CENTRIFUGE.get().defaultBlockState(), 3);
            level.setBlock(base.above(2), thaumcraft.common.registry.ModBlocks.TUBE_BUFFER.get().defaultBlockState(), 3);
            level.setBlock(base.above(2).east(), thaumcraft.common.registry.ModBlocks.BELLOWS.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BellowsBlock.FACING, net.minecraft.core.Direction.WEST), 3);
            var source = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(base);
            var centrifuge = (thaumcraft.common.blockentity.CentrifugeBlockEntity) level.getBlockEntity(base.above());
            var buffer = (thaumcraft.common.blockentity.EssentiaBufferBlockEntity) level.getBlockEntity(base.above(2));
            Aspect victus = Aspect.byTag("victus");
            source.add(victus, 8);
            for (int i = 0; i < 1200; i++) {
                thaumcraft.common.blockentity.CentrifugeBlockEntity.serverTick(level, base.above(), level.getBlockState(base.above()), centrifuge);
                thaumcraft.common.blockentity.EssentiaBufferBlockEntity.serverTick(level, base.above(2), level.getBlockState(base.above(2)), buffer);
            }
            Thaumcraft.LOGGER.info("SELFCHECK центрифуга: в кубе осталось {} victus, в буфере{} (ожидается 8 единиц из {} и {}), тяга буфера с мехами {}",
                    source.getAmount(), describe(buffer.getAspects()), victus.getFirstComponent().getTag(), victus.getSecondComponent().getTag(),
                    buffer.getSuctionAmount(net.minecraft.core.Direction.DOWN));

            // тигель с двумя мехами закипает впятеро быстрее
            net.minecraft.core.BlockPos pos = base.offset(6, 0, 0);
            level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            level.setBlock(pos, thaumcraft.common.registry.ModBlocks.CRUCIBLE.get().defaultBlockState(), 3);
            level.setBlock(pos.east(), thaumcraft.common.registry.ModBlocks.BELLOWS.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BellowsBlock.FACING, net.minecraft.core.Direction.WEST), 3);
            level.setBlock(pos.west(), thaumcraft.common.registry.ModBlocks.BELLOWS.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BellowsBlock.FACING, net.minecraft.core.Direction.EAST), 3);
            var crucible = (thaumcraft.common.blockentity.CrucibleBlockEntity) level.getBlockEntity(pos);
            crucible.fill(1000);
            int ticks = 0;
            while (!crucible.isBoiling() && ticks < 1000) {
                thaumcraft.common.blockentity.CrucibleBlockEntity.serverTick(level, pos, level.getBlockState(pos), crucible);
                ticks++;
            }
            Thaumcraft.LOGGER.info("SELFCHECK мехи: тигель с двумя мехами закипел за {} тиков (ожидается 31; без мехов 151)", ticks);
            for (net.minecraft.core.BlockPos clear : net.minecraft.core.BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(8, 3, 1))) {
                level.setBlock(clear, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }

        /**
         * Куб с 32 metallum, от него труба в три блока вбок и вниз к двум банкам: одна с ярлыком metallum,
         * другая с ярлыком ignis. Вся эссенция должна прийти в первую, во вторую — ничего.
         */
        private static void selfCheckTubes(ServerLevel level) {
            var blocks = thaumcraft.common.registry.ModBlocks.TUBE.get().defaultBlockState();
            net.minecraft.core.BlockPos alembicPos = new net.minecraft.core.BlockPos(280, 152, 280);
            level.setBlock(alembicPos, thaumcraft.common.registry.ModBlocks.ALEMBIC.get().defaultBlockState(), 3);
            for (int i = 1; i <= 4; i++) {
                level.setBlock(alembicPos.east(i), blocks, 3);
            }
            net.minecraft.core.BlockPos jarA = alembicPos.east(2).below();
            net.minecraft.core.BlockPos jarB = alembicPos.east(4).below();
            level.setBlock(jarA, thaumcraft.common.registry.ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            level.setBlock(jarB, thaumcraft.common.registry.ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            var alembic = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(alembicPos);
            var a = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(jarA);
            var b = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(jarB);
            alembic.add(Aspect.byTag("metallum"), 32);
            a.setFilter(Aspect.byTag("metallum"), net.minecraft.core.Direction.NORTH);
            b.setFilter(Aspect.byTag("ignis"), net.minecraft.core.Direction.NORTH);
            for (int i = 1; i <= 4; i++) {
                thaumcraft.common.blocks.TubeBlock.refresh(level, alembicPos.east(i));
            }
            StringBuilder arms = new StringBuilder();
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                if (level.getBlockState(alembicPos.east(2)).getValue(thaumcraft.common.blocks.TubeBlock.ARMS.get(side))) {
                    arms.append(side.getName()).append(' ');
                }
            }
            int ticks = 0;
            while (alembic.getAmount() > 0 && ticks < 6000) {
                for (int i = 1; i <= 4; i++) {
                    net.minecraft.core.BlockPos pos = alembicPos.east(i);
                    thaumcraft.common.blockentity.TubeBlockEntity.serverTick(level, pos, level.getBlockState(pos),
                            (thaumcraft.common.blockentity.TubeBlockEntity) level.getBlockEntity(pos));
                }
                thaumcraft.common.blockentity.EssentiaBlockEntity.jarTick(level, jarA, level.getBlockState(jarA), a);
                thaumcraft.common.blockentity.EssentiaBlockEntity.jarTick(level, jarB, level.getBlockState(jarB), b);
                ticks++;
            }
            for (int extra = 0; extra < 200; extra++) {
                for (int i = 1; i <= 4; i++) {
                    net.minecraft.core.BlockPos pos = alembicPos.east(i);
                    thaumcraft.common.blockentity.TubeBlockEntity.serverTick(level, pos, level.getBlockState(pos),
                            (thaumcraft.common.blockentity.TubeBlockEntity) level.getBlockEntity(pos));
                }
                thaumcraft.common.blockentity.EssentiaBlockEntity.jarTick(level, jarA, level.getBlockState(jarA), a);
                thaumcraft.common.blockentity.EssentiaBlockEntity.jarTick(level, jarB, level.getBlockState(jarB), b);
            }
            Thaumcraft.LOGGER.info("SELFCHECK трубы: отводы средней трубы [{}] (ожидается down west east), куб опустел за {} тиков, "
                            + "в банке metallum {} (ожидается 32), в банке ignis {} (ожидается 0)",
                    arms.toString().trim(), ticks, a.getAmount(), b.getAmount());
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(alembicPos.offset(-1, -2, -1), alembicPos.offset(5, 1, 1))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }

        /** Собирает алтарь, кладёт рецепт тауминиевого набалдашника и 20 раз проводит инфузию до конца. */
        private static void selfCheckInfusion(ServerLevel level) {
            net.minecraft.core.BlockPos centre = new net.minecraft.core.BlockPos(240, 150, 240);
            net.minecraft.core.BlockPos matrixPos = centre.above(2);
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(centre.offset(-4, -1, -4), centre.offset(4, -1, 4))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            }
            level.setBlock(centre, thaumcraft.common.registry.ModBlocks.PEDESTAL.get().defaultBlockState(), 3);
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    level.setBlock(centre.offset(dx, 0, dz), thaumcraft.common.registry.ModBlocks.ARCANE_STONE_BRICKS.get().defaultBlockState(), 3);
                    level.setBlock(centre.offset(dx, 1, dz), thaumcraft.common.registry.ModBlocks.ARCANE_STONE.get().defaultBlockState(), 3);
                }
            }
            level.setBlock(matrixPos, thaumcraft.common.registry.ModBlocks.INFUSION_MATRIX.get().defaultBlockState(), 3);
            net.minecraft.core.BlockPos[] around = {centre.offset(3, 0, 0), centre.offset(-3, 0, 0), centre.offset(0, 0, 3), centre.offset(0, 0, -3)};
            for (net.minecraft.core.BlockPos pos : around) {
                level.setBlock(pos, thaumcraft.common.registry.ModBlocks.PEDESTAL.get().defaultBlockState(), 3);
            }
            net.minecraft.core.BlockPos jarA = centre.offset(4, 0, 4);
            net.minecraft.core.BlockPos jarB = centre.offset(-4, 0, 4);
            level.setBlock(jarA, thaumcraft.common.registry.ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            level.setBlock(jarB, thaumcraft.common.registry.ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            if (!(level.getBlockEntity(matrixPos) instanceof thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix)) {
                Thaumcraft.LOGGER.error("SELFCHECK инфузия: матрица не создала свою сущность");
                return;
            }
            boolean whole = matrix.validLocation();
            level.setBlock(centre.offset(1, 1, 1), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            boolean broken = matrix.validLocation();
            level.setBlock(centre.offset(1, 1, 1), thaumcraft.common.registry.ModBlocks.ARCANE_STONE.get().defaultBlockState(), 3);
            Thaumcraft.LOGGER.info("SELFCHECK инфузия: целый алтарь годен = {} (true), без одного блока столба = {} (false)", whole, broken);
            matrix.wake();

            PlayerKnowledge novice = new PlayerKnowledge();
            PlayerKnowledge adept = new PlayerKnowledge();
            for (ResearchEntry entry : ResearchRegistry.entries()) {
                adept.completeResearch(entry.key);
            }
            Item salis = ForgeRegistries.ITEMS.getValue(Thaumcraft.id("salis_mundus"));
            Item inert = ForgeRegistries.ITEMS.getValue(Thaumcraft.id("wand_cap_thaumium_inert"));
            Item charged = ForgeRegistries.ITEMS.getValue(Thaumcraft.id("wand_cap_thaumium"));
            int success = 0;
            int failed = 0;
            int events = 0;
            long cycles = 0;
            int startInstability = 0;
            for (int run = 0; run < 20; run++) {
                ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(centre)).setItem(new ItemStack(inert));
                for (int i = 0; i < around.length; i++) {
                    ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(around[i]))
                            .setItem(i < 3 ? new ItemStack(salis) : ItemStack.EMPTY);
                }
                var a = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(jarA);
                var b = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(jarB);
                a.clear();
                b.clear();
                a.add(Aspect.byTag("potentia"), 64);
                b.add(Aspect.byTag("auram"), 64);
                if (run == 0) {
                    matrix.craftingStart(novice);
                    Thaumcraft.LOGGER.info("SELFCHECK инфузия без исследования: началась = {} (false)", matrix.isCrafting());
                }
                matrix.craftingStart(adept);
                if (run == 0) {
                    startInstability = matrix.getInstability();
                }
                int ticks = 0;
                while (matrix.isCrafting() && ticks < 40000) {
                    thaumcraft.common.blockentity.InfusionMatrixBlockEntity.serverTick(level, matrixPos, level.getBlockState(matrixPos), matrix);
                    ticks++;
                }
                cycles += ticks;
                ItemStack out = ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(centre)).getItem();
                if (out.is(charged)) {
                    success++;
                } else {
                    failed++;
                }
                // жижа и газ над пьедесталами — следы нестабильности
                for (net.minecraft.core.BlockPos pos : around) {
                    if (!level.getBlockState(pos.above()).isAir()) {
                        events++;
                        level.setBlock(pos.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                if (run == 0) {
                    Thaumcraft.LOGGER.info("SELFCHECK инфузия, первый прогон: на пьедестале {}, в банках осталось {} и {} (52 и 58, если без происшествий)",
                            ForgeRegistries.ITEMS.getKey(out.getItem()), a.getAmount(), b.getAmount());
                }
            }
            Thaumcraft.LOGGER.info("SELFCHECK инфузия: нестабильность на старте {}, 20 прогонов: удачных {}, сорванных {}, следов флюса {}, в среднем {} тиков",
                    startInstability, success, failed, events, cycles / 20);
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(centre.offset(-5, -1, -5), centre.offset(5, 4, 5))) {
                if (level.getBlockEntity(pos) instanceof thaumcraft.common.blockentity.PedestalBlockEntity pedestal) {
                    pedestal.setItem(ItemStack.EMPTY);
                }
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(12)).forEach(net.minecraft.world.entity.Entity::discard);
        }

        /** Печь с двумя кубами над ней: 6 слитков железа и уголь должны дать 24 metallum, разлитые по кубам. */
        private static void selfCheckDistillation(ServerLevel level) {
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(200, 150, 200);
            var blocks = thaumcraft.common.registry.ModBlocks.ALCHEMICAL_FURNACE;
            level.setBlock(pos, blocks.get().defaultBlockState(), 3);
            level.setBlock(pos.above(), thaumcraft.common.registry.ModBlocks.ALEMBIC.get().defaultBlockState(), 3);
            level.setBlock(pos.above(2), thaumcraft.common.registry.ModBlocks.ALEMBIC.get().defaultBlockState(), 3);
            if (!(level.getBlockEntity(pos) instanceof thaumcraft.common.blockentity.AlchemicalFurnaceBlockEntity furnace)
                    || !(level.getBlockEntity(pos.above()) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity first)
                    || !(level.getBlockEntity(pos.above(2)) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity second)) {
                Thaumcraft.LOGGER.error("SELFCHECK перегонка: блоки не создали свои сущности");
                return;
            }
            furnace.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 6));
            furnace.setItem(1, new ItemStack(net.minecraft.world.item.Items.COAL, 2));
            int lit = 0;
            for (int i = 0; i < 3000; i++) {
                thaumcraft.common.blockentity.AlchemicalFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
                if (level.getBlockState(pos).getValue(thaumcraft.common.blocks.AlchemicalFurnaceBlock.LIT)) {
                    lit++;
                }
            }
            Thaumcraft.LOGGER.info("SELFCHECK перегонка: осталось железа {}, угля {}, в печи{}, куб 1: {} x{}, куб 2: {} x{}, горела {} тиков",
                    furnace.getItem(0).getCount(), furnace.getItem(1).getCount(), describe(furnace.getAspects()),
                    first.getAspect(), first.getAmount(), second.getAspect(), second.getAmount(), lit);
            level.removeBlock(pos.above(2), false);
            level.removeBlock(pos.above(), false);
            furnace.clearContent();
            level.removeBlock(pos, false);
        }

        /** Флюс — конечная жидкость: доли не должны ни появляться, ни пропадать, жижа течёт вниз, газ вверх. */
        private static void selfCheckFlux(ServerLevel level) {
            net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(160, 150, 160);
            // каменная чаша 7x7 с бортами, в середине на высоте три блока по 8 долей жижи
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(origin, origin.offset(6, 6, 6))) {
                boolean wall = pos.getX() == origin.getX() || pos.getX() == origin.getX() + 6 || pos.getZ() == origin.getZ()
                        || pos.getZ() == origin.getZ() + 6 || pos.getY() == origin.getY() || pos.getY() == origin.getY() + 6;
                level.setBlock(pos, (wall ? net.minecraft.world.level.block.Blocks.STONE
                        : net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(), 2);
            }
            var goo = thaumcraft.common.registry.ModBlocks.FLUX_GOO.get();
            var gas = thaumcraft.common.registry.ModBlocks.FLUX_GAS.get();
            for (int i = 0; i < 3; i++) {
                level.setBlock(origin.offset(3, 3 + i, 3), goo.defaultBlockState().setValue(thaumcraft.common.blocks.FluxBlock.LEVEL, 7), 3);
            }
            level.setBlock(origin.offset(1, 1, 1), gas.defaultBlockState().setValue(thaumcraft.common.blocks.FluxBlock.LEVEL, 5), 3);
            RandomSource random = RandomSource.create(5);
            for (int round = 0; round < 400; round++) {
                for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(origin, origin.offset(6, 6, 6))) {
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof thaumcraft.common.blocks.FluxBlock) {
                        state.tick(level, pos.immutable(), random);
                    }
                }
            }
            int gooQuanta = 0;
            int gasQuanta = 0;
            int gooFloor = 0;
            int gasCeiling = 0;
            int minLevel = 9;
            int maxLevel = 0;
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(origin, origin.offset(6, 6, 6))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.is(goo)) {
                    int quanta = state.getValue(thaumcraft.common.blocks.FluxBlock.LEVEL) + 1;
                    gooQuanta += quanta;
                    if (pos.getY() == origin.getY() + 1) {
                        gooFloor += quanta;
                        minLevel = Math.min(minLevel, quanta);
                        maxLevel = Math.max(maxLevel, quanta);
                    }
                } else if (state.is(gas)) {
                    int quanta = state.getValue(thaumcraft.common.blocks.FluxBlock.LEVEL) + 1;
                    gasQuanta += quanta;
                    if (pos.getY() == origin.getY() + 5) {
                        gasCeiling += quanta;
                    }
                }
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            Thaumcraft.LOGGER.info("SELFCHECK флюс: жижи {} долей (ожидается 24), на дне {} слоем от {} до {}; газа {} (ожидается 6), под потолком {}",
                    gooQuanta, gooFloor, minLevel, maxLevel, gasQuanta, gasCeiling);
        }

        /**
         * Складывает слой камня на слой глубинного сланца и запускает в нём генераторы руд мода:
         * в камне должны появиться каменные руды, в сланце — только сланцевые.
         */
        private static void selfCheckOres(ServerLevel level) {
            net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(96, 150, 96);
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(origin, origin.offset(15, 31, 15))) {
                level.setBlock(pos, (pos.getY() < origin.getY() + 16 ? net.minecraft.world.level.block.Blocks.DEEPSLATE
                        : net.minecraft.world.level.block.Blocks.STONE).defaultBlockState(), 2);
            }
            RandomSource random = RandomSource.create(11);
            var features = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
            for (String name : new String[]{"cinnabar_ore", "amber_ore", "infused_stone"}) {
                var feature = features.get(Thaumcraft.id(name));
                if (feature == null) {
                    Thaumcraft.LOGGER.error("SELFCHECK нет генератора {}", name);
                    continue;
                }
                for (int i = 0; i < 150; i++) {
                    feature.place(level, level.getChunkSource().getGenerator(), random,
                            origin.offset(2 + random.nextInt(12), 2 + random.nextInt(28), 2 + random.nextInt(12)));
                }
            }
            java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
            int misplaced = 0;
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(origin, origin.offset(15, 31, 15))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                if (id.getNamespace().equals(Thaumcraft.MODID)) {
                    counts.merge(id.getPath(), 1, Integer::sum);
                    if (id.getPath().startsWith("deepslate_") != pos.getY() < origin.getY() + 16) {
                        misplaced++;
                    }
                }
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            Thaumcraft.LOGGER.info("SELFCHECK руды: {}, не в своей породе: {} (ожидается 0)", counts, misplaced);
        }

        private static String describe(AspectList aspects) {
            StringBuilder line = new StringBuilder();
            for (Aspect aspect : aspects.getAspectsSortedByName()) {
                line.append(' ').append(aspect.getTag()).append(':').append(aspects.getAmount(aspect));
            }
            return line.toString();
        }

        /** Ставит тигель над лавой, греет его и проверяет растворение, рецепты, запрет без исследования и выкипание. */
        private static void selfCheckCrucible(ServerLevel level) {
            var recipes = level.getRecipeManager().getAllRecipesFor(thaumcraft.common.registry.ModRecipes.CRUCIBLE.get());
            Thaumcraft.LOGGER.info("SELFCHECK тигель: рецептов {}", recipes.size());
            for (var recipe : recipes) {
                if (recipe.getCatalyst().isEmpty() || recipe.getResult().isEmpty()
                        || !recipe.getResearch().isEmpty() && ResearchRegistry.get(recipe.getResearch()) == null) {
                    Thaumcraft.LOGGER.error("SELFCHECK тигель: неисправный рецепт {}", recipe.getId());
                }
            }
            for (String name : new String[]{"alumentum", "nitor", "thaumium_ingot", "tallow", "thaumium_nugget",
                    "thaumium_block", "cluster_iron", "cluster_gold", "thaumium_pickaxe", "thaumium_chestplate", "crucible"}) {
                Thaumcraft.LOGGER.info("SELFCHECK аспекты {} ={}", name, describe(ItemAspectManager.INSTANCE.getAspects(
                        new ItemStack(ForgeRegistries.ITEMS.getValue(Thaumcraft.id(name))))));
            }

            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(40, 200, 40);
            level.setBlock(pos.below(2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            level.setBlock(pos, thaumcraft.common.registry.ModBlocks.CRUCIBLE.get().defaultBlockState(), 3);
            if (!(level.getBlockEntity(pos) instanceof thaumcraft.common.blockentity.CrucibleBlockEntity crucible)) {
                Thaumcraft.LOGGER.error("SELFCHECK тигель: блок не создал свою сущность");
                return;
            }
            for (int i = 0; i < 300; i++) {
                thaumcraft.common.blockentity.CrucibleBlockEntity.serverTick(level, pos, crucible.getBlockState(), crucible);
            }
            Thaumcraft.LOGGER.info("SELFCHECK тигель без воды: кипит={} (ожидается false)", crucible.isBoiling());
            crucible.fill(thaumcraft.common.blockentity.CrucibleBlockEntity.CAPACITY);
            int ticks = 0;
            while (!crucible.isBoiling() && ticks < 1000) {
                thaumcraft.common.blockentity.CrucibleBlockEntity.serverTick(level, pos, crucible.getBlockState(), crucible);
                ticks++;
            }
            Thaumcraft.LOGGER.info("SELFCHECK тигель над лавой: закипел за {} тиков (ожидается 151)", ticks);

            PlayerKnowledge novice = new PlayerKnowledge();
            PlayerKnowledge adept = new PlayerKnowledge();
            for (ResearchEntry entry : ResearchRegistry.entries()) {
                adept.completeResearch(entry.key);
            }
            java.util.function.BiFunction<ItemStack, PlayerKnowledge, String> drop = (stack, knowledge) -> {
                net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, stack.copy());
                java.util.List<net.minecraft.world.entity.item.ItemEntity> before = level.getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2));
                crucible.attemptSmelt(entity, knowledge);
                StringBuilder out = new StringBuilder();
                for (net.minecraft.world.entity.item.ItemEntity made : level.getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2))) {
                    if (!before.contains(made)) {
                        out.append(' ').append(made.getItem().getCount()).append('x')
                                .append(ForgeRegistries.ITEMS.getKey(made.getItem().getItem()).getPath());
                        made.discard();
                    }
                }
                return "осталось " + (entity.isAlive() ? entity.getItem().getCount() : 0) + ", вышло:" + out
                        + ", в тигле:" + describe(crucible.getAspects()) + ", воды " + crucible.getWater();
            };
            ItemStack shards = new ItemStack(ForgeRegistries.ITEMS.getValue(Thaumcraft.id("shard_balanced")), 4);
            Thaumcraft.LOGGER.info("SELFCHECK тигель, 4 осколка: {}", drop.apply(shards, novice));
            ItemStack iron = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2);
            Thaumcraft.LOGGER.info("SELFCHECK тигель, железо без исследования: {}", drop.apply(iron, novice));
            crucible.spillRemnants();
            crucible.fill(thaumcraft.common.blockentity.CrucibleBlockEntity.CAPACITY);
            Thaumcraft.LOGGER.info("SELFCHECK тигель, 4 осколка заново: {}", drop.apply(shards, novice));
            Thaumcraft.LOGGER.info("SELFCHECK тигель, железо с исследованием: {}", drop.apply(iron, adept));
            Thaumcraft.LOGGER.info("SELFCHECK тигель, железо без бросившего: {}", drop.apply(iron, null));
            Thaumcraft.LOGGER.info("SELFCHECK тигель, предмет без аспектов: {}",
                    drop.apply(new ItemStack(net.minecraft.world.item.Items.BEDROCK), adept));
            for (int i = 0; i < 3000; i++) {
                thaumcraft.common.blockentity.CrucibleBlockEntity.serverTick(level, pos, crucible.getBlockState(), crucible);
            }
            Thaumcraft.LOGGER.info("SELFCHECK тигель после 3000 тиков кипения: в тигле:{}, воды {}",
                    describe(crucible.getAspects()), crucible.getWater());
            int stored = crucible.aspectTotal();
            for (int i = 0; i < 40; i++) {
                drop.apply(new ItemStack(net.minecraft.world.item.Items.DIAMOND_BLOCK), null);
            }
            int overfull = crucible.aspectTotal();
            for (int i = 0; i < 2000; i++) {
                thaumcraft.common.blockentity.CrucibleBlockEntity.serverTick(level, pos, crucible.getBlockState(), crucible);
            }
            Thaumcraft.LOGGER.info("SELFCHECK тигель, перелив: было {}, налили до {}, после 2000 тиков {} (ожидается на 400 меньше: по 1 за 5 тиков)",
                    stored, overfull, crucible.aspectTotal());
            level.removeBlock(pos, false);
            level.removeBlock(pos.below(), false);
        }

        /** Создаёт заметку для каждого первичного исследования и проверяет, что поле собрано правильно. */
        private static void selfCheckNotes() {
            RandomSource random = RandomSource.create(7);
            int made = 0;
            int impossible = 0;
            int broken = 0;
            StringBuilder noAspects = new StringBuilder();
            for (ResearchEntry entry : ResearchRegistry.entries()) {
                if (entry.virtual || entry.autoUnlock || entry.isPurchasable()) {
                    continue;
                }
                ResearchNote note = ResearchNote.create(entry, random);
                if (note == null) {
                    impossible++;
                    noAspects.append(' ').append(entry.key);
                    continue;
                }
                made++;
                long fixed = note.hexes.values().stream().filter(e -> e.type() == ResearchNote.FIXED).count();
                // каждая закреплённая клетка должна иметь хотя бы одного соседа, иначе её не с чем связать
                boolean isolated = false;
                for (var e : note.hexes.entrySet()) {
                    if (e.getValue().type() != ResearchNote.FIXED) {
                        continue;
                    }
                    int neighbours = 0;
                    for (int n = 0; n < 6; n++) {
                        if (note.hexes.containsKey(e.getKey().neighbour(n))) {
                            neighbours++;
                        }
                    }
                    isolated |= neighbours == 0;
                }
                if (fixed != entry.aspects.size() || isolated) {
                    broken++;
                    Thaumcraft.LOGGER.error("SELFCHECK заметка {}: закреплено {} из {}, отрезанные клетки: {}",
                            entry.key, fixed, entry.aspects.size(), isolated);
                }
            }
            Thaumcraft.LOGGER.info("SELFCHECK заметки: создано {}, с ошибками {}, без аспектов (нельзя завершить) {}:{}",
                    made, broken, impossible, noAspects);
            // координаты клетки должны однозначно восстанавливаться из её центра на экране
            int mismatched = 0;
            for (int q = -4; q <= 4; q++) {
                for (int r = -4; r <= 4; r++) {
                    ResearchNote.Hex hex = new ResearchNote.Hex(q, r);
                    if (!hex.equals(ResearchNote.Hex.fromPixel(hex.pixelX(9), hex.pixelY(9), 9))) {
                        mismatched++;
                    }
                }
            }
            Thaumcraft.LOGGER.info("SELFCHECK сетка: клеток с неверным обратным преобразованием: {}", mismatched);

        }

        /**
         * Олово, серебро и свинец: в чистой игре их нет, поэтому их исследования и рецепты скрыты; переплавка с
         * результатом по тегу проверяется на железе.
         */
        private static void selfCheckMetals(ServerLevel level) {
            var recipes = level.getRecipeManager();
            boolean hiddenRecipes = recipes.byKey(Thaumcraft.id("crucible/pure_tin")).isEmpty()
                    && recipes.byKey(Thaumcraft.id("infusion/wand_cap_silver")).isEmpty()
                    && recipes.byKey(Thaumcraft.id("cluster_lead_smelting")).isEmpty();
            boolean hiddenResearch = thaumcraft.common.research.ResearchRegistry.get("PURETIN") == null
                    && thaumcraft.common.research.ResearchRegistry.get("CAP_silver") == null
                    && thaumcraft.common.research.ResearchRegistry.get("PURECOPPER") != null;
            var iron = new thaumcraft.common.crafting.TagSmeltingRecipe(Thaumcraft.id("selfcheck_iron"),
                    net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.RAW_IRON),
                    thaumcraft.common.Res.parse("forge:ingots/iron"), 2, 0f, 200);
            ItemStack out = iron.getResultItem(level.registryAccess());
            Thaumcraft.LOGGER.info("SELFCHECK металлы чужих модов: олова/серебра/свинца нет — рецепты скрыты {}, исследования скрыты {}, "
                    + "переплавка по тегу forge:ingots/iron даёт {} (ждём 2 minecraft:iron_ingot)", hiddenRecipes, hiddenResearch, out);
        }

        /** Раскладывает в сетке 3x3 несколько магических рецептов и печатает результат и цену. */
        private static void selfCheckArcane(ServerLevel level) {
            Map<String, String[]> layouts = new java.util.LinkedHashMap<>();
            String gold = "minecraft:gold_nugget";
            layouts.put("золотой набалдашник", new String[]{gold, gold, gold, gold, "", gold, "", "", ""});
            layouts.put("стержень великого древа", new String[]{"", "thaumcraft:greatwood_log", "", "thaumcraft:greatwood_log", "", "", "", "", ""});
            layouts.put("жезл золото+великое древо", new String[]{"", "", "thaumcraft:wand_cap_gold", "", "thaumcraft:wand_rod_greatwood", "", "thaumcraft:wand_cap_gold", "", ""});
            layouts.put("жезл железо+дерево (не должен собираться)", new String[]{"", "", "thaumcraft:wand_cap_iron", "", "minecraft:stick", "", "thaumcraft:wand_cap_iron", "", ""});
            layouts.put("жезл с лишним предметом (не должен)", new String[]{gold, "", "thaumcraft:wand_cap_gold", "", "thaumcraft:wand_rod_greatwood", "", "thaumcraft:wand_cap_gold", "", ""});
            layouts.forEach((name, layout) -> {
                net.minecraft.world.inventory.TransientCraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(
                        new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                            @Override
                            public ItemStack quickMoveStack(Player player, int index) {
                                return ItemStack.EMPTY;
                            }

                            @Override
                            public boolean stillValid(Player player) {
                                return false;
                            }
                        }, 3, 3);
                for (int i = 0; i < 9; i++) {
                    if (!layout[i].isEmpty()) {
                        grid.setItem(i, new ItemStack(ForgeRegistries.ITEMS.getValue(thaumcraft.common.Res.parse(layout[i]))));
                    }
                }
                var recipe = level.getRecipeManager().getRecipeFor(thaumcraft.common.registry.ModRecipes.ARCANE.get(), grid, level);
                if (recipe.isEmpty()) {
                    Thaumcraft.LOGGER.info("SELFCHECK верстак, {}: рецепта нет", name);
                    return;
                }
                ItemStack out = recipe.get().assemble(grid, level.registryAccess());
                AspectList cost = recipe.get().getAspects(grid);
                StringBuilder line = new StringBuilder();
                for (Aspect aspect : cost.getAspects()) {
                    line.append(' ').append(aspect.getTag()).append(':').append(cost.getAmount(aspect));
                }
                Thaumcraft.LOGGER.info("SELFCHECK верстак, {}: {} x{} {} цена ={}", name, ForgeRegistries.ITEMS.getKey(out.getItem()),
                        out.getCount(), out.hasTag() ? out.getTag() : "", line);
            });
        }

        /** Создаёт несколько узлов рядом, даёт им поработать и печатает, что получилось. */
        private static void selfCheckNodes(ServerLevel level) {
            RandomSource random = RandomSource.create(42);
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                BlockPos pos = new BlockPos(2000 + i * 2, 120, 2000);
                level.getChunk(pos);
                NodeGenerator.createRandomNode(level, pos, random, false);
                positions.add(pos);
            }
            for (int tick = 0; tick < 2400; tick++) {
                for (BlockPos pos : positions) {
                    if (level.getBlockEntity(pos) instanceof NodeBlockEntity node) {
                        NodeBlockEntity.serverTick(level, pos, level.getBlockState(pos), node);
                    }
                }
            }
            for (BlockPos pos : positions) {
                if (!(level.getBlockEntity(pos) instanceof NodeBlockEntity node)) {
                    Thaumcraft.LOGGER.info("SELFCHECK узел {}: исчез", pos.getX());
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (Aspect aspect : node.getAspects()) {
                    line.append(' ').append(aspect.getTag()).append(':').append(node.getAmount(aspect))
                            .append('/').append(node.getBase(aspect));
                }
                Thaumcraft.LOGGER.info("SELFCHECK узел {}: {} {} ={}", pos.getX(), node.getNodeType(), node.getNodeModifier(), line);
            }
        }

        /** Сажает оба дерева по нескольку раз на площадке из травы и считает, что получилось. */
        private static void selfCheckTrees(ServerLevel level) {
            for (String name : new String[]{"greatwood_tree", "greatwood_sapling_tree", "silverwood_tree"}) {
                var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .get(Thaumcraft.id(name));
                if (feature == null) {
                    Thaumcraft.LOGGER.error("SELFCHECK нет генератора {}", name);
                    continue;
                }
                for (int attempt = 0; attempt < 3; attempt++) {
                    BlockPos origin = new BlockPos(1000 + attempt * 64, 100, name.hashCode() % 1000 * 64);
                    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-20, -3, -20), origin.offset(20, 40, 20))) {
                        level.setBlock(pos, pos.getY() < origin.getY()
                                ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                    }
                    boolean placed = feature.place(level, level.getChunkSource().getGenerator(),
                            RandomSource.create(attempt * 31L + 7), origin);
                    int logs = 0;
                    int leaves = 0;
                    int loose = 0;
                    int height = 0;
                    int radius = 0;
                    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-20, -3, -20), origin.offset(20, 40, 20))) {
                        BlockState state = level.getBlockState(pos);
                        if (state.is(BlockTags.LOGS)) {
                            logs++;
                        } else if (state.is(BlockTags.LEAVES)) {
                            leaves++;
                            if (state.getValue(LeavesBlock.DISTANCE) == 7) {
                                loose++;
                            }
                        } else {
                            continue;
                        }
                        height = Math.max(height, pos.getY() - origin.getY() + 1);
                        radius = Math.max(radius, Math.max(Math.abs(pos.getX() - origin.getX()), Math.abs(pos.getZ() - origin.getZ())));
                    }
                    Thaumcraft.LOGGER.info("SELFCHECK {} #{}: посажено={} брёвен={} листвы={} оторванной листвы={} высота={} радиус={}",
                            name, attempt, placed, logs, leaves, loose, height, radius);
                }
            }
        }

        /** Срабатывает при входе игрока и после /reload. */
        @SubscribeEvent
        public static void syncDatapack(OnDatapackSyncEvent event) {
            SyncItemAspectsPacket packet = new SyncItemAspectsPacket(ItemAspectManager.INSTANCE.resolveAll());
            SyncResearchPacket researchPacket = new SyncResearchPacket(ResearchRegistry.toJson());
            if (event.getPlayer() != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), packet);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), researchPacket);
            } else {
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), researchPacket);
            }
        }

        @SubscribeEvent
        public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(Thaumcraft.id("knowledge"), new PlayerKnowledge.Provider());
            }
        }

        /** Знания переживают смерть и возвращение из Края. */
        @SubscribeEvent
        public static void clonePlayer(PlayerEvent.Clone event) {
            Player original = event.getOriginal();
            original.reviveCaps();
            PlayerKnowledge.get(event.getEntity()).load(PlayerKnowledge.get(original).save());
            original.invalidateCaps();
        }

        @SubscribeEvent
        public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            sync(event.getEntity());
        }

        @SubscribeEvent
        public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
            sync(event.getEntity());
        }

        @SubscribeEvent
        public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            sync(event.getEntity());
        }

        /**
         * Неестественный голод (finishedUsingItem оригинала): обычная еда его не утоляет,
         * гнилая плоть ослабляет на уровень и сокращает на полминуты.
         */
        @SubscribeEvent
        public static void finishedUsingItem(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            var hunger = thaumcraft.common.registry.ModEffects.UNNATURAL_HUNGER.get();
            var effect = player.getEffect(hunger);
            if (effect == null) {
                return;
            }
            if (!event.getItem().is(net.minecraft.world.item.Items.ROTTEN_FLESH)) {
                if (event.getItem().isEdible()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("warp.text.hunger.1")
                            .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC), false);
                }
                return;
            }
            int amplifier = effect.getAmplifier() - 1;
            int duration = effect.getDuration() - 600;
            player.removeEffect(hunger);
            if (duration > 0 && amplifier >= 0) {
                var weaker = new net.minecraft.world.effect.MobEffectInstance(hunger, duration, amplifier, true, true);
                weaker.setCurativeItems(new java.util.ArrayList<>(java.util.List.of(new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH))));
                player.addEffect(weaker);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("warp.text.hunger.2")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN, net.minecraft.ChatFormatting.ITALIC), false);
        }

        /** Раз в несколько тиков сообщает клиенту, на что смотрит игрок с таумометром: для показаний на экране. */
        @SubscribeEvent
        public static void playerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
                return;
            }
            // безумие: бросок раз в 2000 тиков, если нет защиты от искажения (EventHandlerEntity оригинала)
            if (player.tickCount > 0 && player.tickCount % 2000 == 0
                    && !player.hasEffect(thaumcraft.common.registry.ModEffects.WARP_WARD.get())) {
                thaumcraft.common.research.WarpEvents.check(player);
            }
            if (player.tickCount % 10 == 0) {
                thaumcraft.common.research.WarpEvents.checkDeathGaze(player);
            }
            if (player.tickCount % 4 != 0) {
                return;
            }
            boolean holding = player.getMainHandItem().getItem() instanceof ThaumometerItem
                    || player.getOffhandItem().getItem() instanceof ThaumometerItem;
            ScanManager.Target target = holding ? ScanManager.findTarget(player) : null;
            boolean scanned = target != null && PlayerKnowledge.get(player).hasScanned(target.key());
            // запас узла меняется на глазах, поэтому он тоже входит в подпись
            String signature = target == null ? "" : target.key() + '|' + scanned + '|' + target.display().visSize();
            if (signature.equals(SENT_TARGETS.getOrDefault(player, ""))) {
                return;
            }
            SENT_TARGETS.put(player, signature);
            ScanTargetPacket packet = target == null ? ScanTargetPacket.none()
                    : new ScanTargetPacket(true, target.name(), scanned, scanned ? target.display() : new AspectList());
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }

        private static final Map<ServerPlayer, String> SENT_TARGETS = new WeakHashMap<>();

        private static void sync(Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                // базовые исследования известны каждому; заодно подхватываются новые после обновления мода
                ResearchHelper.grantAutoUnlocks(PlayerKnowledge.get(serverPlayer));
                SyncKnowledgePacket.send(serverPlayer);
            }
        }
    }

    private CommonEvents() {
    }
}

package thaumcraft.client.debug;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.client.ClientEvents;
import thaumcraft.client.gui.ResearchBrowserScreen;
import thaumcraft.client.gui.ResearchPageScreen;
import thaumcraft.common.blockentity.ResearchTableBlockEntity;
import thaumcraft.common.blocks.ResearchTableBlock;
import thaumcraft.common.items.ResearchNotesItem;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.research.ResearchNote;
import thaumcraft.common.research.ResearchRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Самосъёмка для проверки внешнего вида без человека: с ключом -Dthaumcraft.autoshot=true клиент сам создаёт
 * плоский творческий мир, расставляет сцены, открывает окна, сохраняет снимки в run/screenshots и выходит.
 * В обычной игре ничего не делает.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class AutoShot {
    private static final boolean ENABLED = Boolean.getBoolean("thaumcraft.autoshot");
    private static final String WORLD = "autoshot";

    private record Step(int delay, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int state;
    private static int index;
    private static int timer;

    // ---------- сцены ----------

    private static void script() {
        if ("architect".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            // быстрый режим: разметка «Архитектора» и искры наэлектризованной земли
            cmd("time set noon");
            cmd("fill 198 -60 3 202 -57 3 minecraft:polished_andesite");
            cmd("tp @s 200.5 -60 0.5 0 -5");
            server(5, (server, player) -> {
                ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                        thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, true);
                ItemStack ward = new ItemStack(ModItems.FOCUS_WARDING.get());
                thaumcraft.common.items.wands.FocusItem.applyUpgrade(ward, thaumcraft.api.wands.FocusUpgrade.FRUGAL, 1);
                thaumcraft.common.items.wands.FocusItem.applyUpgrade(ward, thaumcraft.api.wands.FocusUpgrade.ARCHITECT, 2);
                thaumcraft.common.items.wands.WandItem.setFocus(wand, ward);
                wand.getOrCreateTag().putInt("areax", 1);
                wand.getOrCreateTag().putInt("areay", 0);
                wand.getOrCreateTag().putInt("areaz", 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            });
            shot(20, "architect_only");
            cmd("tp @s 197.5 -58 0.5 -40 10");
            shot(20, "architect_only_side");
            cmd("fill 198 -60 3 202 -57 3 minecraft:air");
            cmd("fill 199 -60 4 201 -60 6 thaumcraft:electrified_ground");
            cmd("tp @s 200.5 -60 0.5 0 15");
            for (int i = 0; i < 4; i++) {
                client(i == 0 ? 10 : 2, mc -> {
                    for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(199, -60, 4), new BlockPos(201, -60, 6))) {
                        var state = mc.level.getBlockState(pos);
                        if (state.is(ModBlocks.ELECTRIFIED_GROUND.get())) {
                            state.getBlock().animateTick(state, mc.level, pos.immutable(), mc.level.random);
                        }
                    }
                });
            }
            shot(1, "sparks_only");
            client(60, mc -> {
            });
            return;
        }
        if ("pages".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            // быстрый режим: страницы-чертежи составных конструкций
            cmd("thaumcraft discover all");
            cmd("thaumcraft research all");
            client(20, mc -> {
            });
            for (String key : new String[]{"SCEPTRE", "INFUSIONENCHANTMENT", "INFUSION"}) {
                screen(2, () -> new ResearchPageScreen(ResearchRegistry.get(key)));
                for (int turn = 0; turn < 5; turn++) {
                    int number = turn;
                    client(4, mc -> thaumcraft.Thaumcraft.LOGGER.info("AUTOSHOT PAGE {} {}", key, number));
                    shot(4, "construct_" + key + "_" + turn);
                    client(1, mc -> mc.screen.mouseScrolled(0, 0, -1));
                }
            }
            close();
            return;
        }
        if ("bugs".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            // быстрый режим: глубинные руды вблизи и град алюментума днём
            cmd("time set noon");
            cmd("gamemode creative");
            cmd("difficulty normal");
            cmd("tp @s 320.5 -60 0.5 0 -10");
            server(10, (server, player) -> {
                ServerLevel level = player.serverLevel();
                thaumcraft.api.aspects.Aspect[] kinds = {thaumcraft.api.aspects.Aspect.FIRE, thaumcraft.api.aspects.Aspect.WATER,
                        thaumcraft.api.aspects.Aspect.ENTROPY};
                for (int i = 0; i < kinds.length; i++) {
                    thaumcraft.common.entity.Wisp wisp = thaumcraft.common.registry.ModEntities.WISP.get().create(level);
                    wisp.moveTo(318.5 + i * 2, -58.5, 4.5, 0f, 0f);
                    wisp.setAspect(kinds[i]);
                    wisp.setNoAi(true);
                    level.addFreshEntity(wisp);
                }
            });
            shot(40, "bugs_wisps");
            cmd("kill @e[type=thaumcraft:wisp]");
            cmd("difficulty peaceful");
            cmd("tp @s 300.5 -60 0.5 0 10");
            String[] ores = {"cinnabar_ore", "amber_ore", "infused_stone_air", "infused_stone_fire", "infused_stone_water",
                    "infused_stone_earth", "infused_stone_order", "infused_stone_entropy"};
            for (int i = 0; i < ores.length; i++) {
                cmd("setblock " + (297 + i) + " -60 3 thaumcraft:deepslate_" + ores[i]);
                cmd("setblock " + (297 + i) + " -59 3 thaumcraft:" + ores[i]);
            }
            cmd("setblock 296 -60 3 minecraft:deepslate");
            cmd("setblock 305 -60 3 minecraft:deepslate");
            cmd("tp @s 301.0 -60 0.0 0 0");
            shot(30, "bugs_ores");
            cmd("tp @s 299.0 -60 1.6 0 5");
            shot(20, "bugs_ores_near");
            cmd("tp @s 303.2 -60 1.6 0 5");
            shot(20, "bugs_ores_near2");
            cmd("fill 296 -60 3 305 -59 3 minecraft:air");
            cmd("tp @s 300.5 -60 20.5 0 0");
            for (int tick = 0; tick < 60; tick++) {
                server(1, (server, player) -> {
                    for (int i = 0; i < 4; i++) {
                        thaumcraft.common.entity.AlumentumEntity thrown = new thaumcraft.common.entity.AlumentumEntity(player.serverLevel(), player);
                        thrown.setItem(new ItemStack(ModItems.ALUMENTUM.get()));
                        thrown.shootFromRotation(player, player.getXRot() + player.getRandom().nextInt(30) - 5,
                                player.getYRot() + player.getRandom().nextInt(60) - 30, 0f, 0.75f, 1f);
                        player.serverLevel().addFreshEntity(thrown);
                    }
                });
                if (tick == 30) {
                    shot(1, "bugs_alumentum");
                }
            }
            client(100, mc -> {
            });
            server(1, (server, player) -> check("град из 240 брошенных алюментумов игра пережила", true));
            shot(5, "bugs_alumentum_after");
            return;
        }
        if ("leftovers".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            leftovers();
            client(60, mc -> {
            });
            return;
        }
        if ("crimson".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            crimson();
            client(60, mc -> {
            });
            return;
        }
        if ("outer".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            outerLands();
            client(60, mc -> {
            });
            return;
        }
        if ("eldritch".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            eldritchGear();
            client(60, mc -> {
            });
            return;
        }
        if ("villages".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            villages();
            client(60, mc -> {
            });
            return;
        }
        if ("worldgen".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            worldgen();
            client(60, mc -> {
            });
            return;
        }
        if ("furnace".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            furnace();
            client(60, mc -> {
            });
            return;
        }
        if ("sundries".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            sundries();
            client(60, mc -> {
            });
            return;
        }
        if ("wear".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            wear();
            client(60, mc -> {
            });
            return;
        }
        if ("bows".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            bows();
            client(60, mc -> {
            });
            return;
        }
        if ("devices".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            devices();
            client(60, mc -> {
            });
            return;
        }
        if ("tools".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            tools();
            client(60, mc -> {
            });
            return;
        }
        if ("golems".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            golems();
            client(60, mc -> {
            });
            return;
        }
        if ("beasts".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            beasts();
            client(60, mc -> {
            });
            return;
        }
        if ("taintworld".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            taintWorld();
            client(60, mc -> {
            });
            return;
        }
        if ("taint".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            taint();
            client(60, mc -> {
            });
            return;
        }
        if ("runic".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            runic();
            client(60, mc -> {
            });
            return;
        }
        if ("champions".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            champions();
            client(60, mc -> {
            });
            return;
        }
        if ("recharge".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            cmd("time set noon");
            cmd("thaumcraft discover all");
            recharge();
            client(60, mc -> {
            });
            return;
        }
        if ("goggles".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            cmd("time set noon");
            cmd("thaumcraft discover all");
            goggles();
            client(60, mc -> {
            });
            return;
        }
        if ("shock".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            // быстрый режим: ровно тот кусок полного прогона, где огненный шар и земной разряд бьют по мишени
            cmd("difficulty normal");
            cmd("gamemode creative");
            cmd("time set noon");
            server(5, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, thaumcraft.common.items.wands.WandItem.create(
                    ModItems.WAND.get(), thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, true)));
            upgradedFoci();
            client(20, mc -> {
            });
            return;
        }
        if ("items".equals(System.getProperty("thaumcraft.autoshot.only"))) {
            // быстрый режим: все предметы вкладки мода одной сеткой, как их рисует интерфейс
            client(5, mc -> net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
                    mc.player.connection.enabledFeatures(), true, mc.level.registryAccess()));
            screen(5, ItemGridScreen::new);
            shot(20, "items_grid");
            close();
            return;
        }
        cmd("time set noon");
        cmd("gamerule doDaylightCycle false");
        cmd("weather clear");
        cmd("thaumcraft discover all");
        cmd("thaumcraft research all");

        // тигли: пустой, с водой, с растворёнными предметами; рядом нитор и блок тауминия
        cmd("fill -6 -61 3 6 -61 3 minecraft:stone");
        cmd("setblock 0 -61 3 minecraft:lava");
        cmd("setblock 2 -61 3 minecraft:lava");
        cmd("setblock -2 -60 3 thaumcraft:crucible");
        cmd("setblock 0 -60 3 thaumcraft:crucible{Water:1000s,Heat:200s}");
        cmd("setblock 2 -60 3 thaumcraft:crucible{Water:1000s,Heat:200s}");
        cmd("setblock -4 -59 3 thaumcraft:nitor");
        cmd("setblock 4 -60 3 thaumcraft:thaumium_block");
        cmd("setblock -6 -60 3 minecraft:cauldron");
        cmd("summon item 2 -59 3 {Item:{id:\"minecraft:diamond_block\",Count:3b}}");
        cmd("tp @s 0.5 -59 -0.5 0 35");
        shot(60, "crucibles");
        cmd("tp @s -3.5 -59.5 1.0 20 10");
        shot(30, "nitor_flame");
        cmd("tp @s 1.0 -58.2 2.0 0 60");
        shot(20, "crucibles_top");

        // игровые действия от лица игрока: жезл по котлу, ведро воды, бросок предметов в кипящий тигель
        BlockPos cauldron = new BlockPos(-6, -60, 3);
        BlockPos hot = new BlockPos(0, -60, 3);
        cmd("tp @s -5.5 -60 1.5 0 30");
        server(5, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, thaumcraft.common.items.wands.WandItem.create(
                ModItems.WAND.get(), thaumcraft.api.wands.WandRod.WOOD, thaumcraft.api.wands.WandCap.IRON, true)));
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false)));
        server(10, (server, player) -> check("жезл превращает котёл в тигель",
                player.serverLevel().getBlockState(cauldron).is(ModBlocks.CRUCIBLE.get())));
        server(2, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET)));
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false)));
        server(10, (server, player) -> check("ведро наполняет тигель", player.serverLevel().getBlockEntity(cauldron)
                instanceof thaumcraft.common.blockentity.CrucibleBlockEntity crucible && crucible.getWater() == 1000));
        server(2, (server, player) -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            toss(player, hot, new ItemStack(ModItems.SHARD_BALANCED.get(), 8));
        });
        server(30, (server, player) -> {
            check("осколки растворились", player.serverLevel().getBlockEntity(hot)
                    instanceof thaumcraft.common.blockentity.CrucibleBlockEntity crucible
                    && crucible.getAspects().getAmount(thaumcraft.api.aspects.Aspect.byTag("praecantatio")) == 8);
            toss(player, hot, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2));
        });
        server(40, (server, player) -> {
            int thaumium = 0;
            for (net.minecraft.world.entity.item.ItemEntity item : player.serverLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(hot).inflate(1.5))) {
                if (item.getItem().is(ModItems.THAUMIUM_INGOT.get())) {
                    thaumium += item.getItem().getCount();
                }
            }
            check("два слитка железа стали двумя слитками тауминия (получено " + thaumium + ")", thaumium == 2);
        });
        cmd("tp @s 0.5 -59 0.5 0 25");
        shot(40, "crucible_output");
        cmd("kill @e[type=item]");

        // предметы в инвентаре и броня на игроке
        for (String item : new String[]{"alumentum", "nitor", "tallow", "thaumium_ingot", "thaumium_nugget", "cluster_iron",
                "cluster_gold", "crucible", "thaumium_block", "thaumium_sword", "thaumium_pickaxe", "thaumium_axe",
                "thaumium_shovel", "thaumium_hoe", "scribing_tools", "thaumometer", "thaumonomicon", "amber", "quicksilver"}) {
            cmd("give @s thaumcraft:" + item);
        }
        cmd("item replace entity @s armor.head with thaumcraft:thaumium_helmet");
        cmd("item replace entity @s armor.chest with thaumcraft:thaumium_chestplate");
        cmd("item replace entity @s armor.legs with thaumcraft:thaumium_leggings");
        cmd("item replace entity @s armor.feet with thaumcraft:thaumium_boots");
        screen(10, () -> new InventoryScreen(Minecraft.getInstance().player));
        shot(10, "inventory");
        close();
        cmd("tp @s 0.5 -60 -3.5 0 10");
        client(5, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(20, "armor");
        client(1, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

        // книга
        screen(5, ResearchBrowserScreen::new);
        shot(10, "book_map");
        for (String key : new String[]{"CRUCIBLE", "THAUMIUM", "NITOR", "ALCHEMICALDUPLICATION", "ASPECTS", "CAP_gold"}) {
            screen(2, () -> new ResearchPageScreen(ResearchRegistry.get(key)));
            shot(8, "page_" + key + "_1");
            client(1, mc -> mc.screen.mouseScrolled(0, 0, -1));
            shot(8, "page_" + key + "_2");
        }
        close();

        // стол исследований с заметкой
        server(1, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos main = new BlockPos(-2, -60, -2);
            level.setBlock(main, ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
                    .setValue(ResearchTableBlock.FACING, Direction.WEST).setValue(ResearchTableBlock.MAIN, true), 3);
            level.setBlock(main.west(), ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
                    .setValue(ResearchTableBlock.FACING, Direction.EAST).setValue(ResearchTableBlock.MAIN, false), 3);
            if (level.getBlockEntity(main) instanceof ResearchTableBlockEntity table) {
                table.getItems().set(ResearchTableBlockEntity.TOOLS_SLOT, new ItemStack(ModItems.SCRIBING_TOOLS.get()));
                table.getItems().set(ResearchTableBlockEntity.NOTES_SLOT, ResearchNotesItem.create(
                        ModItems.RESEARCH_NOTES.get(),
                        ResearchNote.create(ResearchRegistry.get("THAUMIUM"), RandomSource.create(3))));
                table.contentsChanged();
            }
        });
        cmd("tp @s -1.0 -59 1.0 150 35");
        shot(20, "research_table_block");
        cmd("tp @s -4.5 -59.3 -4.0 -50 35");
        shot(20, "research_table_block2");
        client(5, mc -> {
            BlockPos pos = new BlockPos(-2, -60, -2);
            mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        });
        shot(20, "research_table_gui");
        close();
        // магический верстак с жезлом и раскладкой золотого набалдашника
        BlockPos bench = new BlockPos(2, -60, -2);
        server(1, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(bench, ModBlocks.ARCANE_WORKBENCH.get().defaultBlockState(), 3);
            if (level.getBlockEntity(bench) instanceof thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity workbench) {
                for (int slot : new int[]{0, 1, 2, 3, 5}) {
                    workbench.getItems().set(slot, new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET));
                }
                workbench.setWand(thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                        thaumcraft.api.wands.WandRod.GREATWOOD, thaumcraft.api.wands.WandCap.GOLD, true));
                workbench.contentsChanged();
            }
        });
        cmd("tp @s 1.0 -59 1.0 200 35");
        shot(20, "workbench_block");
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(bench), Direction.UP, bench, false)));
        shot(20, "workbench_gui");
        server(1, (server, player) -> check("верстак предлагает золотой набалдашник",
                player.containerMenu.getSlot(0).getItem().is(ModItems.WAND_CAP_GOLD.get())
                        || player.containerMenu.slots.stream().anyMatch(slot -> slot.getItem().is(ModItems.WAND_CAP_GOLD.get()))));
        close();

        // руды: каменные и их варианты в глубинном сланце
        String[] ores = {"cinnabar_ore", "amber_ore", "infused_stone_air", "infused_stone_fire", "infused_stone_water",
                "infused_stone_earth", "infused_stone_order", "infused_stone_entropy"};
        for (int i = 0; i < ores.length; i++) {
            cmd("setblock " + (30 + i) + " -60 3 thaumcraft:" + ores[i]);
            cmd("setblock " + (30 + i) + " -59 3 thaumcraft:deepslate_" + ores[i]);
        }
        cmd("give @s thaumcraft:deepslate_cinnabar_ore");
        cmd("give @s thaumcraft:deepslate_infused_stone_fire");
        cmd("give @s thaumcraft:cluster_copper");
        cmd("tp @s 34.0 -60 -1.5 0 5");
        shot(40, "ores");

        // флюс: жижа разной глубины и газ; игрок в жиже получает лихорадку
        for (int i = 0; i < 8; i++) {
            cmd("setblock " + (40 + i) + " -61 10 minecraft:stone");
            cmd("fill " + (40 + i) + " -60 9 " + (40 + i) + " -60 11 minecraft:glass");
            cmd("setblock " + (40 + i) + " -60 10 thaumcraft:flux_goo[level=" + i + "]");
        }
        cmd("fill 39 -60 9 39 -60 11 minecraft:glass");
        cmd("fill 48 -60 9 48 -60 11 minecraft:glass");
        cmd("fill 50 -60 9 52 -57 11 minecraft:glass hollow");
        cmd("setblock 51 -59 10 thaumcraft:flux_gas[level=7]");
        cmd("setblock 51 -58 10 thaumcraft:flux_gas[level=7]");
        cmd("tp @s 45.5 -58 6.5 0 30");
        shot(40, "flux");
        cmd("gamemode survival");
        cmd("tp @s 47.5 -60 10.5 0 30");
        server(30, (server, player) -> check("жижа даёт флюсовую лихорадку",
                player.hasEffect(thaumcraft.common.registry.ModEffects.VIS_EXHAUSTION.get())));
        screen(5, () -> new InventoryScreen(Minecraft.getInstance().player));
        shot(10, "flux_effect");
        close();
        cmd("gamemode creative");
        cmd("effect clear @s");

        // перегонка: горящая печь с кубом, банки с эссенцией, колбы
        BlockPos furnacePos = new BlockPos(60, -60, 10);
        BlockPos jarPos = new BlockPos(62, -60, 10);
        cmd("setblock 60 -60 10 thaumcraft:alchemical_furnace[facing=north]");
        cmd("setblock 60 -59 10 thaumcraft:alembic");
        cmd("setblock 62 -60 10 thaumcraft:warded_jar{Aspect:\"ignis\",Amount:40s}");
        cmd("setblock 63 -60 10 thaumcraft:warded_jar{Aspect:\"aqua\",Amount:64s}");
        cmd("setblock 64 -60 10 thaumcraft:warded_jar");
        cmd("setblock 58 -60 10 thaumcraft:alembic");
        cmd("setblock 57 -60 10 thaumcraft:arcane_stone");
        server(2, (server, player) -> {
            if (player.serverLevel().getBlockEntity(furnacePos)
                    instanceof thaumcraft.common.blockentity.AlchemicalFurnaceBlockEntity furnace) {
                furnace.setItem(0, new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 16));
                furnace.setItem(1, new ItemStack(net.minecraft.world.item.Items.COAL, 8));
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PHIAL.get(), 4));
        });
        cmd("tp @s 61.0 -59.2 6.5 0 15");
        shot(200, "distillation");
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(furnacePos), Direction.NORTH, furnacePos, false)));
        shot(20, "furnace_gui");
        close();
        cmd("gamemode survival");
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(jarPos), Direction.NORTH, jarPos, false)));
        server(10, (server, player) -> {
            boolean filled = player.getInventory().items.stream().anyMatch(stack ->
                    thaumcraft.common.items.PhialItem.getAspect(stack) == thaumcraft.api.aspects.Aspect.FIRE);
            check("колба набирает 8 эссенции из банки", filled && player.serverLevel().getBlockEntity(jarPos)
                    instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar && jar.getAmount() == 32);
            check("печь перегнала золото в куб над собой", player.serverLevel().getBlockEntity(furnacePos.above())
                    instanceof thaumcraft.common.blockentity.EssentiaBlockEntity alembic && alembic.getAmount() > 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WARDED_JAR.get()));
        });
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(furnacePos.above()), Direction.NORTH, furnacePos.above(), false)));
        server(10, (server, player) -> check("банка в руке набирает эссенцию из куба",
                thaumcraft.common.items.JarBlockItem.getAmount(player.getMainHandItem()) > 0));
        screen(5, () -> new InventoryScreen(Minecraft.getInstance().player));
        shot(10, "phials");
        close();
        cmd("gamemode creative");

        // алтарь инфузии: игрок жезлом пробуждает матрицу и запускает рецепт серебряного стержня
        BlockPos altar = new BlockPos(80, -60, 10);
        cmd("setblock 80 -60 10 thaumcraft:pedestal");
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                cmd("setblock " + (80 + dx) + " -60 " + (10 + dz) + " thaumcraft:arcane_stone_bricks");
                cmd("setblock " + (80 + dx) + " -59 " + (10 + dz) + " thaumcraft:arcane_stone");
            }
        }
        cmd("setblock 80 -58 10 thaumcraft:infusion_matrix");
        int[][] ring = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, -2}, {2, -2}, {-2, 2}};
        for (int[] offset : ring) {
            cmd("setblock " + (80 + offset[0]) + " -60 " + (10 + offset[1]) + " thaumcraft:pedestal");
        }
        String[] essentia = {"aer", "ignis", "aqua", "terra", "ordo", "perditio", "praecantatio"};
        for (int i = 0; i < essentia.length; i++) {
            cmd("setblock " + (77 + i) + " -60 15 thaumcraft:warded_jar{Aspect:\"" + essentia[i] + "\",Amount:64s}");
        }
        server(2, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar))
                    .setItem(new ItemStack(ModItems.SILVERWOOD_LOG.get()));
            net.minecraft.world.item.Item[] shards = {ModItems.SHARD_BALANCED.get(), ModItems.SHARD_AIR.get(),
                    ModItems.SHARD_FIRE.get(), ModItems.SHARD_WATER.get(), ModItems.SHARD_EARTH.get(),
                    ModItems.SHARD_ORDER.get(), ModItems.SHARD_ENTROPY.get()};
            for (int i = 0; i < shards.length; i++) {
                ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar.offset(ring[i][0], 0, ring[i][1])))
                        .setItem(new ItemStack(shards[i]));
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.GREATWOOD, thaumcraft.api.wands.WandCap.GOLD, true));
        });
        cmd("effect clear @s");
        cmd("gamemode survival");
        cmd("tp @s 80.5 -59 5.5 0 5");
        shot(20, "altar_idle");
        BlockPos matrixPos = altar.above(2);
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.NORTH, matrixPos, false)));
        server(10, (server, player) -> check("жезл пробуждает матрицу и платит по 25 вис",
                player.serverLevel().getBlockEntity(matrixPos) instanceof thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix
                        && matrix.isActive()
                        && thaumcraft.common.items.wands.WandItem.getVis(player.getMainHandItem(), thaumcraft.api.aspects.Aspect.AIR) == 2500));
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.NORTH, matrixPos, false)));
        server(10, (server, player) -> check("второе касание запускает инфузию",
                player.serverLevel().getBlockEntity(matrixPos) instanceof thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix
                        && matrix.isCrafting()));
        shot(100, "altar_crafting");
        cmd("gamemode creative");
        server(1500, (server, player) -> {
            ItemStack out = ((thaumcraft.common.blockentity.PedestalBlockEntity) player.serverLevel().getBlockEntity(altar)).getItem();
            boolean finished = player.serverLevel().getBlockEntity(matrixPos)
                    instanceof thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix && !matrix.isCrafting();
            // исход случаен: нестабильность может сбросить предмет с пьедестала, и тогда инфузия ждёт игрока
            Thaumcraft.LOGGER.info("AUTOSHOT инфузия: завершена = {}, на центральном пьедестале {}", finished, out);
        });
        shot(5, "altar_done");
        screen(2, () -> new ResearchPageScreen(ResearchRegistry.get("ROD_silverwood")));
        client(1, mc -> mc.screen.mouseScrolled(0, 0, -1));
        shot(8, "page_infusion");
        close();

        // трубы: печь с кубом, от куба трубы к банкам с ярлыками, все виды труб в ряд
        cmd("setblock 140 -60 10 thaumcraft:alchemical_furnace[facing=north]");
        cmd("setblock 140 -59 10 thaumcraft:alembic{Aspect:\"metallum\",Amount:32s}");
        cmd("setblock 141 -59 10 thaumcraft:tube");
        cmd("setblock 142 -59 10 thaumcraft:tube");
        cmd("setblock 143 -59 10 thaumcraft:tube_filter");
        cmd("setblock 144 -59 10 thaumcraft:tube");
        cmd("setblock 142 -60 10 thaumcraft:warded_jar{Filter:\"metallum\",LabelSide:2b}");
        cmd("setblock 144 -60 10 thaumcraft:warded_jar{Filter:\"ignis\",LabelSide:2b}");
        cmd("setblock 144 -58 10 thaumcraft:tube");
        cmd("setblock 145 -58 10 thaumcraft:tube_valve");
        cmd("setblock 146 -58 10 thaumcraft:tube_restrict");
        cmd("setblock 147 -58 10 thaumcraft:tube_oneway");
        cmd("setblock 148 -58 10 thaumcraft:tube");
        cmd("give @s thaumcraft:label{Aspect:\"ignis\"}");
        cmd("give @s thaumcraft:label");
        cmd("tp @s 143.5 -59 6.0 0 10");
        cmd("setblock 145 -57 10 minecraft:redstone_block");
        shot(300, "tubes");
        cmd("tp @s 145.5 -58.6 7.5 0 0");
        shot(10, "tube_valve");
        server(2, (server, player) -> check("эссенция дошла по трубам до банки с ярлыком",
                player.serverLevel().getBlockEntity(new BlockPos(142, -60, 10))
                        instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar && jar.getAmount() > 10));

        // центрифуга на кубе, буфер с мехами, конструкция
        cmd("setblock 160 -60 10 thaumcraft:alembic{Aspect:\"victus\",Amount:32s}");
        cmd("setblock 160 -59 10 thaumcraft:centrifuge");
        cmd("setblock 160 -58 10 thaumcraft:tube_buffer");
        cmd("setblock 161 -58 10 thaumcraft:bellows[facing=west]");
        cmd("setblock 159 -58 10 thaumcraft:tube");
        cmd("setblock 158 -58 10 thaumcraft:tube");
        cmd("setblock 158 -59 10 thaumcraft:warded_jar");
        cmd("setblock 163 -60 10 thaumcraft:alchemical_construct");
        cmd("setblock 165 -60 10 thaumcraft:bellows[facing=north]");
        cmd("setblock 167 -60 10 thaumcraft:alembic{Aspect:\"ignis\",Amount:32s}");
        cmd("setblock 167 -59 10 thaumcraft:crystallizer[facing=down]");
        cmd("setblock 169 -59 10 thaumcraft:crystallizer[facing=west]");
        cmd("give @s thaumcraft:crystallizer");
        cmd("give @s thaumcraft:crystal_essence{Aspect:\"aqua\"}");
        cmd("give @s thaumcraft:bellows");
        cmd("give @s thaumcraft:centrifuge");
        // буфер с мехами сам держит эссенцию; чтобы труба могла её забрать, сторону к трубе надо «придушить» дважды
        server(5, (server, player) -> {
            if (player.serverLevel().getBlockEntity(new BlockPos(160, -58, 10))
                    instanceof thaumcraft.common.blockentity.EssentiaBufferBlockEntity buffer) {
                buffer.cycleChoke(Direction.WEST);
                buffer.cycleChoke(Direction.WEST);
            }
        });
        cmd("tp @s 161.0 -59 5.5 0 5");
        shot(300, "machinery");
        server(2, (server, player) -> check("центрифуга разделила эссенцию, и она дошла до банки",
                player.serverLevel().getBlockEntity(new BlockPos(158, -59, 10))
                        instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar && jar.getAmount() > 0
                        && jar.getAspect() != null && jar.getAspect().isPrimal()));

        cmd("tp @s 167.5 -59 6.5 0 5");
        shot(20, "crystallizer");
        server(1100, (server, player) -> {
            boolean made = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(167, -59, 10)).inflate(3)).stream().anyMatch(item ->
                    thaumcraft.common.items.CrystalEssenceItem.getAspect(item.getItem()) == thaumcraft.api.aspects.Aspect.FIRE);
            check("кристаллизатор выдал кристалл огненной эссенции", made);
        });

        // жезл, скипетр и посохи в руке и на земле
        cmd("tp @s 180.5 -60 0.5 0 10");
        server(2, (server, player) -> {
            ItemStack sceptre = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, true);
            thaumcraft.common.items.wands.WandItem.setSceptre(sceptre);
            player.setItemInHand(InteractionHand.MAIN_HAND, thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.PRIMAL_STAFF, thaumcraft.api.wands.WandCap.THAUMIUM, true));
            player.setItemInHand(InteractionHand.OFF_HAND, sceptre);
            ItemStack[] shown = {
                    thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.BLAZE,
                            thaumcraft.api.wands.WandCap.COPPER, true),
                    sceptre.copy(),
                    thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.OBSIDIAN_STAFF,
                            thaumcraft.api.wands.WandCap.GOLD, true)};
            for (int i = 0; i < shown.length; i++) {
                net.minecraft.world.entity.decoration.ItemFrame frame = new net.minecraft.world.entity.decoration.ItemFrame(
                        player.serverLevel(), new BlockPos(179 + i, -59, 3), Direction.NORTH);
                player.serverLevel().setBlock(new BlockPos(179 + i, -59, 4), net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
                frame.setItem(shown[i]);
                player.serverLevel().addFreshEntity(frame);
            }
        });
        shot(20, "staves_hand");
        client(1, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(10, "staves_third");
        client(1, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

        // фокусы: струя огня, молния и ледяной осколок по зомби-мишени, затем круговое меню
        cmd("difficulty easy");
        cmd("tp @s 200.5 -60 0.5 0 0");
        cmd("summon minecraft:husk 200.5 -60 6.5 {NoAI:1b,PersistenceRequired:1b,Health:20f}");
        float[] health = new float[2];
        server(5, (server, player) -> {
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, true);
            thaumcraft.common.items.wands.WandItem.setFocus(wand, new ItemStack(ModItems.FOCUS_FIRE.get()));
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            player.getInventory().add(new ItemStack(ModItems.FOCUS_SHOCK.get()));
            player.getInventory().add(new ItemStack(ModItems.FOCUS_FROST.get()));
        });
        shot(10, "focus_hand");
        // кнопку «использовать» держит сам клиент — так проверяется весь путь от нажатия до заклинания
        client(1, mc -> mc.options.keyUse.setDown(true));
        shot(16, "focus_fire");
        client(1, mc -> mc.options.keyUse.setDown(false));
        server(2, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(12));
            health[0] = husks.isEmpty() ? -1 : husks.get(0).getHealth();
            check("струя огня обжигает и поджигает мишень (здоровье " + health[0] + " из 20)",
                    !husks.isEmpty() && health[0] < 20f && husks.get(0).isOnFire());
            husks.forEach(h -> {
                h.clearFire();
                h.setHealth(20f);
            });
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BL");
            check("фокус сменился на разряд, прежний вернулся в инвентарь",
                    thaumcraft.common.items.wands.WandItem.getFocusStack(player.getMainHandItem()).is(ModItems.FOCUS_SHOCK.get())
                            && player.getInventory().contains(new ItemStack(ModItems.FOCUS_FIRE.get())));
        });
        // пауза, чтобы у мишени прошла неуязвимость после прошлого удара
        client(30, mc -> mc.options.keyUse.setDown(true));
        shot(9, "focus_shock");
        client(1, mc -> mc.options.keyUse.setDown(false));
        server(2, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(12));
            health[1] = husks.isEmpty() ? -1 : husks.get(0).getHealth();
            check("молния бьёт мишень (здоровье " + health[1] + " из 20)", !husks.isEmpty() && health[1] < 20f);
            husks.forEach(h -> h.setHealth(20f));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BF");
        });
        cmd("tp @s 200.5 -60 0.5 0 -7");
        client(30, mc -> mc.options.keyUse.setDown(true));
        shot(4, "focus_frost");
        client(1, mc -> mc.options.keyUse.setDown(false));
        server(15, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(12));
            float left = husks.isEmpty() ? -1 : husks.get(0).getHealth();
            check("ледяной осколок ранит мишень (здоровье " + left + " из 20)", !husks.isEmpty() && left < 20f);
        });
        // сумка для фокусов на поясе: жезл берёт фокус из неё и туда же убирает прежний
        server(5, (server, player) -> {
            ItemStack pouch = new ItemStack(ModItems.FOCUS_POUCH.get());
            java.util.List<ItemStack> inside = new java.util.ArrayList<>(thaumcraft.common.items.wands.FocusPouchItem.getInventory(pouch));
            inside.set(0, new ItemStack(ModItems.FOCUS_SHOCK.get()));
            thaumcraft.common.items.wands.FocusPouchItem.setInventory(pouch, inside);
            player.getInventory().clearOrCountMatchingItems(st -> st.getItem() instanceof thaumcraft.common.items.wands.FocusItem,
                    -1, player.inventoryMenu.getCraftSlots());
            boolean[] worn = new boolean[1];
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("belt").ifPresent(belt -> {
                        belt.getStacks().setStackInSlot(0, pouch);
                        worn[0] = true;
                    }));
            check("у игрока есть ячейка пояса, сумка надета", worn[0]);
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BL");
            var pouches = thaumcraft.common.items.wands.FocusPouchItem.findPouches(player);
            boolean frostInPouch = !pouches.isEmpty() && thaumcraft.common.items.wands.FocusPouchItem.getInventory(pouches.get(0))
                    .stream().anyMatch(st -> st.is(ModItems.FOCUS_FROST.get()));
            check("жезл взял разряд из сумки на поясе, а мороз убрал в неё",
                    thaumcraft.common.items.wands.WandItem.getFocusStack(player.getMainHandItem()).is(ModItems.FOCUS_SHOCK.get())
                            && frostInPouch);
            player.getInventory().add(new ItemStack(ModItems.FOCUS_FIRE.get()));
        });
        screen(5, () -> new thaumcraft.client.gui.FocusRadialScreen(ClientEvents.ModBus.CHANGE_FOCUS, true));
        shot(10, "focus_radial");
        close();

        // фокус раскопок: луч сверлит камень, появляются трещины, блок выпадает предметом
        cmd("tp @s 200.5 -60 0.5 0 0");
        cmd("setblock 200 -59 4 minecraft:stone");
        server(5, (server, player) -> {
            player.getInventory().add(new ItemStack(ModItems.FOCUS_EXCAVATION.get()));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BE");
        });
        client(5, mc -> mc.options.keyUse.setDown(true));
        shot(5, "focus_excavation");
        client(25, mc -> mc.options.keyUse.setDown(false));
        server(5, (server, player) -> {
            boolean dropped = !player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(200, -59, 4)).inflate(2),
                    e -> e.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)).isEmpty();
            check("луч раскопок разрушил камень и выронил булыжник", dropped);
        });
        // проклятие печа и первозданная сфера по мишени
        server(5, (server, player) -> {
            player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, player.getBoundingBox().inflate(12))
                    .forEach(h -> h.setHealth(20f));
            player.getInventory().add(new ItemStack(ModItems.FOCUS_PECH.get()));
            player.getInventory().add(new ItemStack(ModItems.FOCUS_PRIMAL.get()));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "PP");
        });
        client(30, mc -> mc.options.keyUse.setDown(true));
        shot(3, "focus_pech");
        // несколько сгустков подряд: яд на нежить не действует, а эффект выпадает наугад
        client(30, mc -> mc.options.keyUse.setDown(false));
        server(12, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(12));
            boolean cursed = !husks.isEmpty() && (husks.get(0).hasEffect(net.minecraft.world.effect.MobEffects.POISON)
                    || husks.get(0).hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    || husks.get(0).hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS));
            check("проклятие печа ранит мишень и накладывает эффект", cursed && husks.get(0).getHealth() < 20f);
            husks.forEach(h -> h.setHealth(20f));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "FP");
        });
        client(30, mc -> mc.options.keyUse.setDown(true));
        client(3, mc -> mc.options.keyUse.setDown(false));
        shot(4, "focus_primal");
        server(30, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(16));
            check("первозданная сфера взрывается у мишени", husks.isEmpty() || husks.get(0).getHealth() < 20f);
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BE");
        });
        shot(2, "focus_primal_after");

        // портативная дыра: тоннель 3x3 сквозь стену толщиной в три блока, потом стена возвращается
        cmd("fill 197 -60 3 203 -56 5 minecraft:stone_bricks");
        server(5, (server, player) -> {
            player.getInventory().add(new ItemStack(ModItems.FOCUS_PORTABLE_HOLE.get()));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BPH");
        });
        shot(10, "focus_hole_before");
        client(1, mc -> mc.options.keyUse.setDown(true));
        client(3, mc -> mc.options.keyUse.setDown(false));
        shot(12, "focus_hole");
        server(1, (server, player) -> {
            boolean through = true;
            for (int z = 3; z <= 5; z++) {
                through &= player.serverLevel().getBlockState(new BlockPos(200, -59, z)).is(thaumcraft.common.registry.ModBlocks.HOLE.get());
            }
            check("портативная дыра пробила стену насквозь", through
                    && player.serverLevel().getBlockState(new BlockPos(199, -60, 4)).is(thaumcraft.common.registry.ModBlocks.HOLE.get()));
        });
        server(130, (server, player) -> {
            boolean restored = true;
            for (int z = 3; z <= 5; z++) {
                restored &= player.serverLevel().getBlockState(new BlockPos(200, -59, z)).is(net.minecraft.world.level.block.Blocks.STONE_BRICKS);
            }
            check("через шесть секунд стена вернулась на место", restored);
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BE");
        });
        cmd("fill 197 -60 3 203 -56 5 minecraft:air");

        // девять кругов ада: призванная огненная мышь летит к мишени и жжёт её
        server(5, (server, player) -> {
            player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, player.getBoundingBox().inflate(16))
                    .forEach(h -> {
                        h.setHealth(20f);
                        h.clearFire();
                    });
            player.getInventory().add(new ItemStack(ModItems.FOCUS_HELLBAT.get()));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "HH");
        });
        client(10, mc -> mc.options.keyUse.setDown(true));
        client(3, mc -> mc.options.keyUse.setDown(false));
        shot(8, "focus_hellbat");
        server(1, (server, player) -> check("фокус призвал огненную летучую мышь", !player.serverLevel().getEntitiesOfClass(
                thaumcraft.common.entity.FireBat.class, player.getBoundingBox().inflate(16)).isEmpty()));
        server(80, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(16));
            check("огненная мышь добралась до мишени и ранила или подожгла её",
                    husks.isEmpty() || husks.get(0).getHealth() < 20f || husks.get(0).isOnFire());
            player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.FireBat.class, player.getBoundingBox().inflate(32))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BE");
        });

        // равноценный обмен: приседание запоминает доски, правая кнопка меняет на них каменную стенку 3x3
        cmd("fill 199 -60 3 201 -58 3 minecraft:stone");
        cmd("setblock 203 -60 2 minecraft:oak_planks");
        cmd("tp @s 203.5 -60 0.5 0 35");
        server(5, (server, player) -> {
            player.getInventory().add(new ItemStack(ModItems.FOCUS_TRADE.get()));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BT");
            player.setShiftKeyDown(true);
        });
        client(2, mc -> {
            mc.options.keyShift.setDown(true);
            mc.options.keyUse.setDown(true);
        });
        client(4, mc -> {
            mc.options.keyUse.setDown(false);
            mc.options.keyShift.setDown(false);
        });
        server(5, (server, player) -> check("приседание и правая кнопка запомнили доски",
                thaumcraft.common.items.wands.foci.TradeFocus.getPickedBlock(player.getMainHandItem())
                        .is(net.minecraft.world.item.Items.OAK_PLANKS)));
        cmd("tp @s 200.5 -60 0.5 0 0");
        client(10, mc -> mc.options.keyUse.setDown(true));
        client(3, mc -> mc.options.keyUse.setDown(false));
        shot(5, "focus_trade");
        server(40, (server, player) -> {
            int planks = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(199, -60, 3), new BlockPos(201, -58, 3))) {
                if (player.serverLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)) {
                    planks++;
                }
            }
            check("обмен превратил всю каменную стенку в доски (" + planks + " из 9)", planks == 9);
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BE");
        });
        shot(2, "focus_trade_done");
        cmd("fill 199 -60 3 203 -58 3 minecraft:air");
        cmd("setblock 203 -60 2 minecraft:air");

        // защита: два блока травы и камень становятся неразрушимыми, взрыв их не берёт, хозяин снимает защиту
        cmd("fill 199 -60 3 201 -60 3 minecraft:grass_block");
        cmd("setblock 200 -59 3 minecraft:glowstone");
        cmd("tp @s 200.5 -60 0.5 0 0");
        server(5, (server, player) -> {
            player.getInventory().add(new ItemStack(ModItems.FOCUS_WARDING.get()));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BWA");
            for (BlockPos pos : new BlockPos[]{new BlockPos(199, -60, 3), new BlockPos(200, -60, 3), new BlockPos(200, -59, 3)}) {
                thaumcraft.common.blocks.WardedBlock.ward(player.serverLevel(), pos, player);
            }
        });
        // четвёртый блок защищается настоящим нажатием правой кнопки (взгляд вниз-вправо, на траву)
        cmd("tp @s 200.5 -60 0.5 -22 28");
        client(10, mc -> mc.options.keyUse.setDown(true));
        client(3, mc -> mc.options.keyUse.setDown(false));
        cmd("tp @s 200.5 -60 0.5 0 10");
        shot(15, "focus_warding");
        server(1, (server, player) -> {
            int warded = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(199, -60, 3), new BlockPos(201, -59, 3))) {
                if (player.serverLevel().getBlockState(pos).is(thaumcraft.common.registry.ModBlocks.WARDED.get())) {
                    warded++;
                }
            }
            check("защищено четыре блока, один из них — нажатием кнопки (" + warded + " из 4)", warded == 4);
            check("защищённый светокамень по-прежнему светится",
                    player.serverLevel().getBlockState(new BlockPos(200, -59, 3)).getLightEmission(player.serverLevel(), new BlockPos(200, -59, 3)) == 15);
            player.serverLevel().explode(null, 200.5, -59.5, 4.5, 4f, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        });
        server(10, (server, player) -> {
            int warded = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(199, -60, 3), new BlockPos(201, -59, 3))) {
                if (player.serverLevel().getBlockState(pos).is(thaumcraft.common.registry.ModBlocks.WARDED.get())) {
                    warded++;
                }
            }
            check("взрыв рядом не разрушил защищённые блоки (" + warded + " из 4)", warded == 4);
            check("хозяин снимает защиту, и блок становится прежним",
                    thaumcraft.common.blocks.WardedBlock.unward(player.serverLevel(), new BlockPos(200, -59, 3), player)
                            && player.serverLevel().getBlockState(new BlockPos(200, -59, 3)).is(net.minecraft.world.level.block.Blocks.GLOWSTONE));
            thaumcraft.common.items.wands.FocusManager.changeFocus(player, "BE");
        });
        cmd("fill 197 -62 1 203 -58 6 minecraft:air replace thaumcraft:warded");
        cmd("fill 199 -60 3 201 -59 3 minecraft:air");

        // сеть вис: стабилизатор, узел, преобразователь под сигналом, два реле и прибор, тянущий вис
        cmd("tp @s 220.5 -60 0.5 0 0");
        cmd("setblock 220 -60 5 thaumcraft:node_stabilizer");
        cmd("setblock 220 -58 5 thaumcraft:node_transducer");
        cmd("setblock 221 -58 5 minecraft:redstone_block");
        cmd("setblock 223 -60 5 thaumcraft:vis_relay[facing=up]");
        cmd("setblock 226 -60 5 thaumcraft:vis_relay[facing=up]");
        server(5, (server, player) -> {
            BlockPos nodePos = new BlockPos(220, -59, 5);
            thaumcraft.common.world.NodeGenerator.createRandomNode(player.serverLevel(), nodePos, player.getRandom(), false);
            if (player.serverLevel().getBlockEntity(nodePos) instanceof thaumcraft.common.blockentity.NodeBlockEntity node) {
                thaumcraft.api.aspects.AspectList aspects = new thaumcraft.api.aspects.AspectList()
                        .add(thaumcraft.api.aspects.Aspect.EARTH, 64).add(thaumcraft.api.aspects.Aspect.FIRE, 36);
                node.setAspects(aspects);
                node.setNodeType(thaumcraft.api.nodes.NodeType.NORMAL);
                node.setNodeModifier(null);
                node.nodeChange();
            }
            // зарядка длится 50 секунд; для проверки счётчик преобразователя подводится почти к концу
            if (player.serverLevel().getBlockEntity(new BlockPos(220, -58, 5)) instanceof thaumcraft.common.blocks.NodeTransducerBlock.Transducer t) {
                t.checkStatus();
                check("преобразователь над стабилизированным узлом под сигналом начал зарядку", t.status == 1);
                t.count = thaumcraft.common.blocks.NodeTransducerBlock.Transducer.FULL - 60;
            }
        });
        shot(30, "visnet_charging");
        server(120, (server, player) -> {
            BlockPos nodePos = new BlockPos(220, -59, 5);
            boolean energized = player.serverLevel().getBlockEntity(nodePos) instanceof thaumcraft.common.blockentity.NodeBlockEntity node
                    && node.isEnergized();
            check("узел стал заряженным", energized);
            int out = player.serverLevel().getBlockEntity(nodePos) instanceof thaumcraft.common.blockentity.NodeBlockEntity node
                    ? node.getVisOutput().getAmount(thaumcraft.api.aspects.Aspect.EARTH) : -1;
            check("выход по земле равен корню из предела: 8 (получено " + out + ")", out == 8);
            boolean linked = player.serverLevel().getBlockEntity(new BlockPos(226, -60, 5)) instanceof thaumcraft.common.blocks.VisRelayBlock.Relay relay
                    && relay.isConnected();
            check("дальнее реле подключилось к сети через ближнее", linked);
            int near = thaumcraft.api.visnet.VisNetHandler.drainVis(player.serverLevel(), new BlockPos(228, -60, 5),
                    thaumcraft.api.aspects.Aspect.EARTH, 5);
            int far = thaumcraft.api.visnet.VisNetHandler.drainVis(player.serverLevel(), new BlockPos(245, -60, 5),
                    thaumcraft.api.aspects.Aspect.EARTH, 5);
            check("прибор рядом с дальним реле получает вис земли (" + near + " из 5), а вдали от сети — нет (" + far + ")", near == 5 && far == 0);
        });
        cmd("tp @s 223.5 -60 1.5 0 5");
        shot(10, "visnet");

        // фокальный манипулятор рядом с реле: «Мощь» на фокус огня — за опыт и вис из сети
        cmd("setblock 225 -60 3 thaumcraft:focal_manipulator");
        server(5, (server, player) -> {
            BlockPos nodePos = new BlockPos(220, -59, 5);
            if (player.serverLevel().getBlockEntity(nodePos) instanceof thaumcraft.common.blockentity.NodeBlockEntity node) {
                // узел посильнее и со всеми шестью аспектами: цена «Мощи» раскладывается на несколько первичных
                thaumcraft.api.aspects.AspectList strong = new thaumcraft.api.aspects.AspectList();
                for (thaumcraft.api.aspects.Aspect primal : thaumcraft.api.aspects.Aspect.primals()) {
                    strong.add(primal, 400);
                }
                node.setAspects(strong);
                node.energize();
            }
            if (player.serverLevel().getBlockEntity(new BlockPos(225, -60, 3)) instanceof thaumcraft.common.blocks.FocalManipulatorBlock.Manipulator table) {
                table.setItem(0, new ItemStack(ModItems.FOCUS_FIRE.get()));
                check("манипулятор принял заказ на «Мощь» для фокуса огня",
                        table.startCraft(thaumcraft.api.wands.FocusUpgrade.POTENCY.id, player));
                Thaumcraft.LOGGER.info("AUTOSHOT манипулятор: нужно вис {}", table.getSize() / 100f);
                net.minecraftforge.network.NetworkHooks.openScreen(player, table, table.getBlockPos());
            }
        });
        shot(25, "focal_manipulator_gui");
        close();
        cmd("tp @s 225.5 -60 0.5 0 15");
        shot(10, "focal_manipulator");
        server(160, (server, player) -> {
            boolean upgraded = player.serverLevel().getBlockEntity(new BlockPos(225, -60, 3)) instanceof thaumcraft.common.blocks.FocalManipulatorBlock.Manipulator table
                    && thaumcraft.common.items.wands.FocusItem.getUpgradeLevel(table.getItem(0), thaumcraft.api.wands.FocusUpgrade.POTENCY) == 1
                    && table.getSize() == 0;
            check("вис из сети набраны, на фокусе появилось улучшение «Мощь»", upgraded);
            if (player.serverLevel().getBlockEntity(new BlockPos(225, -60, 3)) instanceof thaumcraft.common.blocks.FocalManipulatorBlock.Manipulator table) {
                net.minecraftforge.network.NetworkHooks.openScreen(player, table, table.getBlockPos());
            }
        });
        shot(15, "focal_manipulator_gui_done");
        close();
        cmd("setblock 225 -60 3 minecraft:air");
        cmd("kill @e[type=item,distance=..40]");
        cmd("setblock 220 -60 5 minecraft:air");
        server(40, (server, player) -> check("без стабилизатора заряженный узел взрывается",
                !(player.serverLevel().getBlockEntity(new BlockPos(220, -59, 5)) instanceof thaumcraft.common.blockentity.NodeBlockEntity)));
        cmd("fill 214 -64 0 232 -52 12 minecraft:air replace thaumcraft:flux_goo");
        cmd("fill 214 -64 0 232 -52 12 minecraft:air replace thaumcraft:flux_gas");
        cmd("fill 220 -60 5 226 -58 5 minecraft:air");
        cmd("tp @s 200.5 -60 0.5 0 0");

        upgradedFoci();

        // «Архитектор»: защита отмеченной области 3x3x1 одним нажатием, с разметкой перед этим
        cmd("fill 198 -60 3 202 -57 3 minecraft:polished_andesite");
        cmd("tp @s 200.5 -60 0.5 0 -5");
        server(5, (server, player) -> {
            ItemStack ward = new ItemStack(ModItems.FOCUS_WARDING.get());
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(ward, thaumcraft.api.wands.FocusUpgrade.FRUGAL, 1);
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(ward, thaumcraft.api.wands.FocusUpgrade.ARCHITECT, 2);
            ItemStack wand = player.getMainHandItem();
            thaumcraft.common.items.wands.WandItem.setFocus(wand, ward);
            // область: один блок в стороны по ширине и высоте грани, без глубины; для боковой грани, как в оригинале,
            // ширину задаёт areaz, высоту — areax, глубину — areay
            wand.getOrCreateTag().putInt("areax", 1);
            wand.getOrCreateTag().putInt("areay", 0);
            wand.getOrCreateTag().putInt("areaz", 1);
            check("фокус-архитектор на жезле распознан", thaumcraft.common.items.wands.Architect.isActive(wand));
        });
        shot(15, "architect_overlay");
        client(1, mc -> mc.options.keyUse.setDown(true));
        client(3, mc -> mc.options.keyUse.setDown(false));
        server(10, (server, player) -> {
            int warded = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(198, -60, 3), new BlockPos(202, -57, 3))) {
                if (player.serverLevel().getBlockState(pos).is(thaumcraft.common.registry.ModBlocks.WARDED.get())) {
                    warded++;
                }
            }
            check("одно нажатие защитило область 3x3 (" + warded + " из 9)", warded == 9);
        });
        shot(5, "architect_warded");
        cmd("fill 196 -62 1 204 -55 6 minecraft:air replace thaumcraft:warded");
        cmd("fill 198 -60 3 202 -57 3 minecraft:air");
        cmd("tp @s 200.5 -60 0.5 0 0");
        server(2, (server, player) -> thaumcraft.common.items.wands.WandItem.setFocus(player.getMainHandItem(),
                new ItemStack(ModItems.FOCUS_EXCAVATION.get())));

        // наклон жезла со стороны: рука вытянута, жезл смотрит вперёд
        cmd("setblock 200 -59 9 minecraft:obsidian");
        client(5, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUse.setDown(true);
        });
        shot(12, "wand_tilt_back");
        // взгляд вбок и вверх, чтобы луч от вершины жезла был виден со спины
        cmd("tp @s 200.5 -60 0.5 -35 -20");
        shot(10, "wand_tilt_side");
        cmd("tp @s 200.5 -60 0.5 0 0");
        client(5, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(6, "wand_tilt_front");
        client(1, mc -> {
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        cmd("setblock 200 -59 9 minecraft:air");
        cmd("kill @e[type=minecraft:husk]");
        cmd("difficulty peaceful");

        // эффекты: молния и колышущийся луч
        cmd("tp @s 100.5 -60 0.5 0 0");
        cmd("setblock 100 -60 5 minecraft:stone");
        for (int i = 0; i < 12; i++) {
            server(i == 0 ? 20 : 2, (server, player) -> {
                thaumcraft.common.network.FxPacket.bolt(player.serverLevel(), new Vec3(98.5, -57.5, 6.5), new Vec3(98.5, -59.5, 3.5));
                thaumcraft.common.network.FxPacket.send(player.serverLevel(), thaumcraft.common.network.FxPacket.FLOATY_BEAM,
                        new Vec3(103.5, -57.5, 6.5), new Vec3(101.0, -59.2, 2.5), 0xffff7e, 0);
                thaumcraft.common.network.FxPacket.blockSparkle(player.serverLevel(), new BlockPos(100, -60, 5), -9999, 8);
            });
        }
        client(1, mc -> mc.getToasts().clear());
        client(1, mc -> Screenshot.grab(mc.gameDirectory, "fx_bolt_beam.png", mc.getMainRenderTarget(), message -> { }));

        // искажение: запретное исследование оставляет его навсегда
        server(5, (server, player) -> {
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            int before = knowledge.getWarpPerm();
            thaumcraft.common.research.Warp.add(player, 3, false);
            thaumcraft.common.research.Warp.add(player, 2, true);
            thaumcraft.common.research.Warp.addSticky(player, 1);
            check("искажение копится: постоянное +3, временное +2, липкое +1",
                    knowledge.getWarpPerm() == before + 3 && knowledge.getWarpTemp() >= 2 && knowledge.getWarpSticky() >= 1);
        });

        // приступы безумия: 60 бросков при большом искажении, затем пауки-видения
        cmd("difficulty easy");
        cmd("tp @s 120.5 -60 0.5 0 10");
        cmd("gamemode survival");
        server(5, (server, player) -> {
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            knowledge.addWarpPerm(60);
            int fired = 0;
            java.util.Set<String> effects = new java.util.TreeSet<>();
            for (int i = 0; i < 60; i++) {
                knowledge.setWarpCounter(100);
                int before = knowledge.getWarpCounter();
                thaumcraft.common.research.WarpEvents.check(player);
                if (knowledge.getWarpCounter() < before) {
                    fired++;
                }
                player.getActiveEffects().forEach(e -> effects.add(net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                        .getKey(e.getEffect()).getPath()));
            }
            int spiders = player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.MindSpider.class,
                    player.getBoundingBox().inflate(40)).size();
            Thaumcraft.LOGGER.info("AUTOSHOT безумие: приступов {} из 60, эффекты {}, пауков {}, открыто ELDRITCHMAJOR: {}",
                    fired, effects, spiders, knowledge.isResearchComplete("ELDRITCHMAJOR"));
            check("приступы безумия случаются и накладывают эффекты", fired > 0 && !effects.isEmpty());
            player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.MindSpider.class,
                    player.getBoundingBox().inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
            player.removeAllEffects();
        });
        cmd("effect clear @s");
        cmd("gamemode creative");
        for (int i = 0; i < 6; i++) {
            cmd("summon thaumcraft:mind_spider " + (119 + i * 0.6) + " -60 3.5 {NoAI:1b}");
        }
        server(5, (server, player) -> thaumcraft.common.network.FxPacket.sendTo(player, thaumcraft.common.network.FxPacket.WARP_VIGNETTE,
                Vec3.ZERO, Vec3.ZERO, 0, 0));
        cmd("tp @s 120.5 -60 2.2 0 35");
        shot(90, "warp_spiders_vignette");
        cmd("kill @e[type=thaumcraft:mind_spider]");
        cmd("difficulty peaceful");
        cmd("effect give @s thaumcraft:death_gaze 20 0 true");
        shot(40, "warp_death_gaze");
        cmd("effect clear @s");
        client(1, mc -> thaumcraft.client.ClientWarp.reset());
        cmd("tp @s 0.5 -59 -0.5 0 35");
        cmd("effect give @s thaumcraft:blurred_vision 20 0 true");
        shot(20, "warp_blurred");
        cmd("effect clear @s");
        cmd("effect give @s thaumcraft:unnatural_hunger 20 0 true");
        shot(20, "warp_hunger");
        cmd("effect clear @s");
        server(5, (server, player) -> thaumcraft.common.network.FxPacket.sendTo(player, thaumcraft.common.network.FxPacket.MIST,
                Vec3.ZERO, Vec3.ZERO, 2400, 0));
        shot(140, "warp_mist");
        client(1, mc -> thaumcraft.client.ClientWarp.reset());

        // деревья и узел ауры; таумометр и жезл в руках
        cmd("place feature thaumcraft:greatwood_sapling_tree 12 -60 -14");
        cmd("place feature thaumcraft:silverwood_sapling_tree -10 -60 -14");
        cmd("tp @s 0.5 -60 4.5 180 -15");
        shot(40, "trees");
        cmd("tp @s 20.5 -60 20.5 0 0");
        cmd("thaumcraft node");
        server(5, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THAUMOMETER.get())));
        cmd("tp @s 20.5 -60 17.5 0 -10");
        shot(40, "node_thaumometer");
        server(5, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, thaumcraft.common.items.wands.WandItem.create(
                ModItems.WAND.get(), thaumcraft.api.wands.WandRod.WOOD, thaumcraft.api.wands.WandCap.IRON, true)));
        shot(20, "node_wand");

        goggles();
        recharge();
        champions();
        runic();
        taint();
        beasts();
        leftovers();
        golems();
        tools();
        devices();
        bows();
        wear();
        sundries();
        furnace();
        worldgen();
        villages();
        eldritchGear();
        crimson();
        outerLands();

        // снимки сохраняются в фоне: даём им дописаться до выхода
        client(60, mc -> {
        });
    }

    /** Порча: заражение земли, рост волокон, превращение земли и брёвен, оседание корки. */
    /** Улучшенные фокусы: огненный шар и земной разряд по свежей мишени (общий кусок полного прогона и режима «shock»). */
    private static void upgradedFoci() {
        // сцены выше роют здесь ямы: разряд ищет землю не глубже четырёх блоков, поэтому пол под мишенью восстанавливаем
        cmd("fill 190 -63 -4 210 -62 16 minecraft:dirt");
        cmd("fill 190 -61 -4 210 -61 16 minecraft:grass_block");
        cmd("fill 190 -60 -4 210 -54 16 minecraft:air replace minecraft:fire");
        // улучшенные фокусы: огненный шар и земной разряд по свежей мишени (прежнюю могли убить сцены выше)
        cmd("tp @s 200.5 -60 0.5 0 0");
        cmd("kill @e[type=minecraft:husk]");
        cmd("kill @e[type=item]");
        cmd("summon minecraft:husk 200.5 -60 6.5 {NoAI:1b,PersistenceRequired:1b,Health:20f}");
        server(5, (server, player) -> {
            player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, player.getBoundingBox().inflate(16))
                    .forEach(h -> {
                        h.setHealth(20f);
                        h.clearFire();
                    });
            ItemStack fire = new ItemStack(ModItems.FOCUS_FIRE.get());
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(fire, thaumcraft.api.wands.FocusUpgrade.POTENCY, 1);
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(fire, thaumcraft.api.wands.FocusUpgrade.POTENCY, 2);
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(fire, thaumcraft.api.wands.FocusUpgrade.FIREBALL, 3);
            player.getInventory().clearOrCountMatchingItems(st -> st.getItem() instanceof thaumcraft.common.items.wands.FocusItem,
                    -1, player.inventoryMenu.getCraftSlots());
            thaumcraft.common.items.wands.WandItem.setFocus(player.getMainHandItem(), fire);
        });
        client(30, mc -> mc.options.keyUse.setDown(true));
        client(2, mc -> mc.options.keyUse.setDown(false));
        shot(2, "focus_fireball");
        server(20, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(16));
            check("огненный шар взрывается у мишени и ранит её", !husks.isEmpty() && husks.get(0).getHealth() < 20f);
            husks.forEach(h -> {
                h.setHealth(20f);
                h.clearFire();
            });
            // улучшенный фокус ставится прямо на жезл: смена по ключу взяла бы обычный «Разряд» из сумки фокусов
            ItemStack shock = new ItemStack(ModItems.FOCUS_SHOCK.get());
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(shock, thaumcraft.api.wands.FocusUpgrade.POTENCY, 1);
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(shock, thaumcraft.api.wands.FocusUpgrade.POTENCY, 2);
            thaumcraft.common.items.wands.FocusItem.applyUpgrade(shock, thaumcraft.api.wands.FocusUpgrade.EARTH_SHOCK, 3);
            thaumcraft.common.items.wands.WandItem.setFocus(player.getMainHandItem(), shock);
        });
        client(30, mc -> mc.options.keyUse.setDown(true));
        client(2, mc -> mc.options.keyUse.setDown(false));
        // шар летит и разрывается за долю секунды: следим каждые два тика и запоминаем, сколько земли было наэлектризовано
        int[] seen = new int[2];
        for (int t = 0; t < 20; t++) {
            server(2, (server, player) -> {
                int now = 0;
                for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(192, -62, -2), new BlockPos(208, -56, 14))) {
                    if (player.serverLevel().getBlockState(pos).is(thaumcraft.common.registry.ModBlocks.ELECTRIFIED_GROUND.get())) {
                        now++;
                    }
                }
                seen[0] = Math.max(seen[0], now);
                seen[1] = Math.max(seen[1], player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.FocusOrbEntity.class,
                        player.getBoundingBox().inflate(40)).size());
            });
        }
        server(1, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(16));
            int ground = seen[0];
            // подробности на случай сбоя: проверка изредка не проходит, и без них причину не найти
            ItemStack held = player.getMainHandItem();
            int orbs = player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.FocusOrbEntity.class,
                    player.getBoundingBox().inflate(40)).size();
            check("земной разряд ранит мишень и электризует землю (участков " + ground + ", шар был " + (seen[1] > 0) + ", земной разряд на фокусе "
                            + thaumcraft.common.items.wands.FocusItem.isUpgradedWith(thaumcraft.common.items.wands.WandItem.getFocusStack(held),
                            thaumcraft.api.wands.FocusUpgrade.EARTH_SHOCK) + ", мишеней " + husks.size()
                            + (husks.isEmpty() ? "" : ", здоровье " + husks.get(0).getHealth()) + ", шаров в полёте " + orbs + ", в руке "
                            + held.getItem() + ", фокус " + thaumcraft.common.items.wands.WandItem.getFocusStack(held).getItem()
                            + ", воздуха в жезле " + thaumcraft.common.items.wands.WandItem.getVis(held, thaumcraft.api.aspects.Aspect.AIR) + ")",
                    !husks.isEmpty() && husks.get(0).getHealth() < 20f && ground > 0);
        });
        for (int i = 0; i < 4; i++) {
            client(2, mc -> {
                for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(192, -62, -2), new BlockPos(208, -56, 14))) {
                    var state = mc.level.getBlockState(pos);
                    if (state.is(ModBlocks.ELECTRIFIED_GROUND.get())) {
                        state.getBlock().animateTick(state, mc.level, pos.immutable(), mc.level.random);
                    }
                }
            });
        }
        shot(1, "focus_earthshock");
        cmd("fill 190 -62 -4 210 -54 16 minecraft:air replace thaumcraft:electrified_ground");
        server(2, (server, player) -> {
            player.getInventory().clearOrCountMatchingItems(st -> st.getItem() instanceof thaumcraft.common.items.wands.FocusItem,
                    -1, player.inventoryMenu.getCraftSlots());
            thaumcraft.common.items.wands.WandItem.setFocus(player.getMainHandItem(), new ItemStack(ModItems.FOCUS_EXCAVATION.get()));
        });
    }

    private static void taint() {
        cmd("tp @s 600.5 -60 0.5 0 20");
        client(40, mc -> {
        });
        cmd("gamemode creative");
        cmd("time set noon");
        BlockPos centre = new BlockPos(600, -60, 6);
        cmd("tp @s 600.5 -60 6.5 0 20");
        cmd("thaumcraft taint");
        cmd("tp @s 600.5 -57 -2.5 0 25");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean here = thaumcraft.common.taint.Taint.isTainted(level, centre);
            boolean far = thaumcraft.common.taint.Taint.isTainted(level, centre.offset(40, 0, 0));
            check("земля вокруг заражена, а в сорока блоках — нет (здесь " + here + ", там " + far + ")", here && !far);
            // три блока корки и бревно рядом: волокна должны разрастись, бревно — покрыться коркой, земля — стать почвой
            level.setBlock(centre, ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            level.setBlock(centre.east(), ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            level.setBlock(centre.west(), ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            level.setBlock(centre.north(), net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), 3);
            // корка на высоком столбе из камня: под ней пусто только сбоку, она должна остаться; подвешенная — упасть
            level.setBlock(centre.offset(4, 5, 0), ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            // ком земли, с трёх сторон зажатый коркой: порчи вокруг достаточно, чтобы он стал порченой почвой
            // весь ком с коркой стоит внутри порченой земли: на её границе корка растекается жижей
            BlockPos lump = centre.offset(-2, 0, 3);
            level.setBlock(lump, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(lump.east(), ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            level.setBlock(lump.west(), ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
            level.setBlock(lump.south(), ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
        });
        // ускоряем время: много случайных тиков по блокам порчи вокруг
        for (int round = 0; round < 40; round++) {
            server(1, (server, player) -> {
                ServerLevel level = player.serverLevel();
                for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-6, -3, -6), centre.offset(6, 6, 6))) {
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                    if (thaumcraft.common.taint.Taint.isTaint(state)) {
                        for (int i = 0; i < 6; i++) {
                            net.minecraft.world.level.block.state.BlockState now = level.getBlockState(pos);
                            if (now.isRandomlyTicking()) {
                                now.randomTick(level, pos.immutable(), level.random);
                            }
                        }
                    }
                }
            });
        }
        server(20, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int fibres = 0;
            int plants = 0;
            int soil = 0;
            int crust = 0;
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-8, -4, -8), centre.offset(8, 8, 8))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.TAINT_FIBRES.get())) {
                    fibres++;
                } else if (state.getBlock() instanceof thaumcraft.common.taint.TaintPlantBlock) {
                    plants++;
                } else if (state.is(ModBlocks.TAINTED_SOIL.get())) {
                    soil++;
                } else if (state.is(ModBlocks.TAINT_CRUST.get())) {
                    crust++;
                }
            }
            boolean fell = level.getBlockState(centre.offset(4, 5, 0)).isAir();
            boolean outside = false;
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-14, -1, -14), centre.offset(14, 3, 14))) {
                if (thaumcraft.common.taint.Taint.isTaint(level.getBlockState(pos)) && !thaumcraft.common.taint.Taint.isTainted(level, pos)
                        && level.getBlockState(pos).is(ModBlocks.TAINT_FIBRES.get())) {
                    outside = true;
                }
            }
            check("порча разрослась: волокон " + fibres + ", растений " + plants + ", порченой почвы " + soil + ", корки " + crust
                            + "; подвешенная корка упала (" + fell + ")",
                    fibres >= 10 && soil >= 1 && fell);
            check("за пределами порченой земли волокна не растут", !outside);
        });
        shot(20, "taint");
        cmd("tp @s 598.5 -59.5 2.5 -20 15");
        shot(20, "taint_near");
        // порождения порчи: спора на стебле, рой спор, рой и паучки из лопнувшей споры
        cmd("difficulty normal");
        BlockPos stalk = centre.offset(3, 0, -3);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(stalk, ModBlocks.SPORE_STALK.get().defaultBlockState(), 3);
            for (int i = 0; i < 200 && !level.getBlockState(stalk).getValue(thaumcraft.common.taint.TaintPlantBlock.RIPE); i++) {
                level.getBlockState(stalk).randomTick(level, stalk, level.random);
            }
            thaumcraft.common.entity.TaintSporeSwarmer swarmer = thaumcraft.common.registry.ModEntities.TAINT_SPORE_SWARMER.get().create(level);
            swarmer.moveTo(centre.getX() - 2.5, centre.getY(), centre.getZ() - 3.5, 0f, 0f);
            level.addFreshEntity(swarmer);
            thaumcraft.common.entity.TaintSwarm swarm = thaumcraft.common.registry.ModEntities.TAINT_SWARM.get().create(level);
            swarm.moveTo(centre.getX() + 0.5, centre.getY() + 2, centre.getZ() - 2.5, 0f, 0f);
            level.addFreshEntity(swarm);
            thaumcraft.common.entity.TaintSpider spider = thaumcraft.common.registry.ModEntities.TAINT_SPIDER.get().create(level);
            spider.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 4.5, 0f, 0f);
            spider.setNoAi(true);
            level.addFreshEntity(spider);
        });
        cmd("tp @s 600.5 -59 -1.5 0 10");
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var spores = level.getEntitiesOfClass(thaumcraft.common.entity.TaintSpore.class, new net.minecraft.world.phys.AABB(stalk.above()));
            boolean ripe = level.getBlockState(stalk).getValue(thaumcraft.common.taint.TaintPlantBlock.RIPE);
            check("на споровом стебле созрела спора и сидит на месте (стебель зрелый " + ripe + ", спор " + spores.size() + ")",
                    ripe && spores.size() == 1 && spores.get(0).isAlive());
        });
        shot(40, "taint_mobs");
        cmd("tp @s 603.5 -60 0.2 0 5");
        shot(20, "taint_spore");
        cmd("tp @s 597.5 -60 -1.0 0 5");
        shot(20, "taint_swarmer");
        cmd("tp @s 600.5 -59 -1.5 0 10");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (var spore : level.getEntitiesOfClass(thaumcraft.common.entity.TaintSpore.class, new net.minecraft.world.phys.AABB(stalk.above()))) {
                spore.setSporeSize(9);
                spore.hurt(player.damageSources().playerAttack(player), 5f);
            }
        });
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int spiders = level.getEntitiesOfClass(thaumcraft.common.entity.TaintSpider.class,
                    new net.minecraft.world.phys.AABB(stalk).inflate(8)).size();
            boolean ripe = level.getBlockState(stalk).getValue(thaumcraft.common.taint.TaintPlantBlock.RIPE);
            int spores = level.getEntitiesOfClass(thaumcraft.common.entity.TaintSpore.class, new net.minecraft.world.phys.AABB(stalk.above())).size();
            // спора девятого размера даёт от трёх до семи паучков; ещё один стоял в сцене заранее
            check("убитая спора лопнула паучками, стебель опустел (паучков " + spiders + ", стебель зрелый " + ripe + ", спор " + spores + ")",
                    spiders >= 4 && spiders <= 8 && !ripe && spores == 0);
            int swarms = level.getEntitiesOfClass(thaumcraft.common.entity.TaintSwarm.class, new net.minecraft.world.phys.AABB(centre).inflate(24)).size();
            check("рой порчи жив и держится рядом (роёв " + swarms + ")", swarms >= 1);
        });
        cmd("kill @e[type=!player]");
        cmd("difficulty peaceful");
        client(5, mc -> check("клиент видит порченую землю (трава окрашена цветом порчи)",
                thaumcraft.common.taint.Taint.isTainted(mc.level, centre)
                        && net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(mc.level, centre) != net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(mc.level, centre.offset(40, 0, 0))));
    }

    /** Порченые животные, житель, крипер, тентакли и таумический слизень: вид и главные механики. */
    /** Убирает всё, кроме игрока, бесследно: убитый командой слизень распался бы на маленьких. */
    /** Стихийные инструменты: топор, лопата, мотыга, кирка и меч. */
    private static void tools() {
        cmd("gamemode survival");
        cmd("time set noon");
        cmd("difficulty normal");
        cmd("effect give @s minecraft:resistance 600 4 true");
        cmd("effect give @s minecraft:saturation 600 4 true");
        BlockPos centre = new BlockPos(1400, -60, 20);
        cmd("tp @s 1400.5 -60 10.5 0 0");
        client(40, mc -> {
        });
        purge(centre);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"infusion/elemental_axe", "infusion/elemental_pickaxe", "infusion/elemental_sword",
                    "infusion/elemental_shovel", "infusion/elemental_hoe", "infusion/golem_core_lumber"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты пяти стихийных инструментов и ядра рубки загружены (нет:" + missing + ")", missing.length() == 0);
            player.getInventory().clearContent();
            for (int y = -60; y <= -55; y++) {
                level.setBlock(new BlockPos(1400, y, 12), net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), 3);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELEMENTAL_AXE.get()));
        });
        // топор: три удара по комлю снимают три бревна с макушки
        for (int i = 0; i < 3; i++) {
            server(4, (server, player) -> player.gameMode.destroyBlock(new BlockPos(1400, -60, 12)));
        }
        server(2, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int left = 0;
            for (int y = -60; y <= -55; y++) {
                if (level.getBlockState(new BlockPos(1400, y, 12)).is(net.minecraft.world.level.block.Blocks.OAK_LOG)) {
                    left++;
                }
            }
            int flying = level.getEntitiesOfClass(thaumcraft.common.entity.FollowingItem.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(30)).size();
            check("топор потока снял три бревна с макушки, комель цел (брёвен осталось " + left + ", верхнее "
                    + level.getBlockState(new BlockPos(1400, -55, 12)).getBlock() + "), износ " + player.getMainHandItem().getDamageValue()
                    + ", летящих предметов " + flying + ", в сумке " + player.getInventory().countItem(net.minecraft.world.item.Items.OAK_LOG),
                    left == 3 && level.getBlockState(new BlockPos(1400, -60, 12)).is(net.minecraft.world.level.block.Blocks.OAK_LOG)
                            && level.getBlockState(new BlockPos(1400, -55, 12)).isAir() && player.getMainHandItem().getDamageValue() == 3
                            && flying + player.getInventory().countItem(net.minecraft.world.item.Items.OAK_LOG) == 3);
        });
        shot(3, "tools_axe");
        server(40, (server, player) -> check("срубленные брёвна сами прилетели к игроку (в сумке "
                + player.getInventory().countItem(net.minecraft.world.item.Items.OAK_LOG) + ")",
                player.getInventory().countItem(net.minecraft.world.item.Items.OAK_LOG) == 3));
        // зажатый топор притягивает лежащие предметы
        server(2, (server, player) -> {
            for (int i = 0; i < 3; i++) {
                var item = new net.minecraft.world.entity.item.ItemEntity(player.serverLevel(), 1396.5 + i * 3, -59.5, 6.5 - i,
                        new ItemStack(net.minecraft.world.item.Items.BRICK));
                item.setDeltaMovement(0, 0, 0);
                item.setPickUpDelay(0);
                player.serverLevel().addFreshEntity(item);
            }
        });
        cmd("tp @s 1400.5 -60 10.5 0 -60");
        client(10, mc -> mc.options.keyUse.setDown(true));
        client(60, mc -> mc.options.keyUse.setDown(false));
        server(5, (server, player) -> check("зажатый топор притянул три кирпича, лежавших в шести блоках (в сумке "
                + player.getInventory().countItem(net.minecraft.world.item.Items.BRICK) + ")",
                player.getInventory().countItem(net.minecraft.world.item.Items.BRICK) == 3));

        // лопата: копает три на три и кладёт три на три
        cmd("tp @s 1400.5 -60 20.5 0 90");
        server(10, (server, player) -> {
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELEMENTAL_SHOVEL.get()));
            player.gameMode.destroyBlock(new BlockPos(1400, -61, 20));
        });
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int dug = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (level.getBlockState(new BlockPos(1400 + dx, -61, 20 + dz)).isAir()) {
                        dug++;
                    }
                }
            }
            check("лопата землекопа выкопала девять блоков одним ударом (выкопано " + dug + ", износ "
                    + player.getMainHandItem().getDamageValue() + ")", dug == 9 && player.getMainHandItem().getDamageValue() == 8);
            level.setBlock(new BlockPos(1406, -60, 20), net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState(), 3);
            player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
            ItemStack shovel = player.getMainHandItem();
            shovel.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(1406.5, -59, 20.5), Direction.UP, new BlockPos(1406, -60, 20), false)));
            int laid = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (level.getBlockState(new BlockPos(1406 + dx, -59, 20 + dz)).is(net.minecraft.world.level.block.Blocks.COBBLESTONE)) {
                        laid++;
                    }
                }
            }
            check("щелчок лопатой по булыжнику положил девять булыжников из сумки (положено " + laid + ", в сумке осталось "
                    + player.getInventory().countItem(net.minecraft.world.item.Items.COBBLESTONE) + ")",
                    laid == 9 && player.getInventory().countItem(net.minecraft.world.item.Items.COBBLESTONE) == 55);
        });
        client(3, mc -> thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.ToggleMiscPacket()));
        server(5, (server, player) -> {
            ItemStack shovel = player.getMainHandItem();
            byte orientation = thaumcraft.common.items.tools.ElementalShovelItem.getOrientation(shovel);
            shovel.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(1406.5, -58, 20.5), Direction.UP, new BlockPos(1406, -59, 20), false)));
            ServerLevel level = player.serverLevel();
            int wall = 0;
            for (int dy = 0; dy <= 1; dy++) {
                for (int d = -1; d <= 1; d++) {
                    if (level.getBlockState(new BlockPos(1406 + d, -58 + dy, 20)).is(net.minecraft.world.level.block.Blocks.COBBLESTONE)) {
                        wall++;
                    }
                }
            }
            check("клавиша переключила лопату на кладку стеной (режим " + orientation + ", в стене булыжников " + wall + ")",
                    orientation == 1 && wall == 6);
        });
        cmd("tp @s 1403.5 -60 15.5 -30 20");
        shot(10, "tools_shovel");

        // мотыга: пашет три на три и растит саженцы
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELEMENTAL_HOE.get()));
            ItemStack hoe = player.getMainHandItem();
            hoe.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(1412.5, -60, 20.5), Direction.UP, new BlockPos(1412, -61, 20), false)));
            int tilled = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (level.getBlockState(new BlockPos(1412 + dx, -61, 20 + dz)).is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                        tilled++;
                    }
                }
            }
            check("мотыга роста вспахала девять блоков (вспахано " + tilled + ", износ " + hoe.getDamageValue() + ")",
                    tilled == 9 && hoe.getDamageValue() == 9);
            BlockPos wheat = new BlockPos(1412, -60, 20);
            level.setBlock(wheat, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(), 3);
            hoe.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(wheat), Direction.UP, wheat, false)));
            int age = level.getBlockState(wheat).hasProperty(net.minecraft.world.level.block.CropBlock.AGE)
                    ? level.getBlockState(wheat).getValue(net.minecraft.world.level.block.CropBlock.AGE) : -1;
            check("мотыга подрастила пшеницу, как костная мука (возраст " + age + ", износ " + hoe.getDamageValue() + ")",
                    age > 0 && hoe.getDamageValue() == 10);
            BlockPos sapling = new BlockPos(1420, -60, 24);
            level.setBlock(sapling, ModBlocks.GREATWOOD_SAPLING.get().defaultBlockState(), 3);
            hoe.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(sapling), Direction.UP, sapling, false)));
            check("мотыга сразу вырастила великое дерево из саженца за 5 единиц прочности (на месте саженца "
                    + level.getBlockState(sapling).getBlock() + ", износ " + hoe.getDamageValue() + ")",
                    !level.getBlockState(sapling).is(ModBlocks.GREATWOOD_SAPLING.get()) && hoe.getDamageValue() == 15);
        });
        cmd("tp @s 1412.5 -60 14.5 20 15");
        shot(10, "tools_hoe");

        // кирка: чутьё руд, грозди и огонь
        cmd("tp @s 1400.5 -60 38.5 0 30");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELEMENTAL_PICKAXE.get()));
            for (int i = 0; i < 40; i++) {
                level.setBlock(new BlockPos(1420 + i, -60, 44), net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 3);
            }
            level.setBlock(new BlockPos(1399, -62, 42), net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(1400, -63, 42), net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(1401, -62, 42), net.minecraft.world.level.block.Blocks.GOLD_ORE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(1402, -63, 43), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 3);
            level.setBlock(new BlockPos(1398, -63, 43), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        });
        client(10, mc -> mc.player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(1400.5, -60, 41.5), Direction.UP, new BlockPos(1400, -61, 41), false))));
        client(8, mc -> check("чутьё кирки ядра отметило руды, воду и лаву под землёй (отмечено клеток "
                + thaumcraft.client.render.OreScanOverlay.marked() + ": " + thaumcraft.client.render.OreScanOverlay.values() + ")",
                thaumcraft.client.render.OreScanOverlay.marked() >= 5));
        cmd("kill @e[type=minecraft:slime]");
        cmd("tp @s 1400.5 -60 39.5 0 55");
        cmd("time set midnight");
        shot(10, "tools_scan");
        cmd("time set noon");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (int i = 0; i < 40; i++) {
                player.gameMode.destroyBlock(new BlockPos(1420 + i, -60, 44));
            }
            int clusters = 0;
            int raw = 0;
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(1418, -62, 42, 1462, -56, 46))) {
                if (item.getItem().is(ModItems.CLUSTER_IRON.get())) {
                    clusters += item.getItem().getCount();
                } else if (item.getItem().is(net.minecraft.world.item.Items.RAW_IRON)) {
                    raw += item.getItem().getCount();
                }
            }
            check("кирка ядра выбила из сорока блоков железной руды часть самородными гроздьями (гроздей " + clusters + ", сырого железа "
                    + raw + ")", clusters >= 1 && clusters <= 25 && clusters + raw == 40);
            var cow = net.minecraft.world.entity.EntityType.COW.create(level);
            cow.setNoAi(true);
            cow.moveTo(1400.5, -60, 40.5, 0f, 0f);
            level.addFreshEntity(cow);
            player.attack(cow);
            check("удар киркой ядра поджёг корову", cow.isOnFire());
            cow.discard();
        });

        // меч: круговой удар и вихрь
        cmd("tp @s 1400.5 -60 50.5 0 0");
        net.minecraft.world.entity.monster.Husk[] husks = new net.minecraft.world.entity.monster.Husk[3];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELEMENTAL_SWORD.get()));
            for (int i = 0; i < 3; i++) {
                husks[i] = net.minecraft.world.entity.EntityType.HUSK.create(level);
                husks[i].setNoAi(true);
                husks[i].moveTo(1399.5 + i, -60, 52.5, 180f, 0f);
                level.addFreshEntity(husks[i]);
            }
        });
        server(30, (server, player) -> {
            player.attack(husks[1]);
            String health = husks[0].getHealth() + "/" + husks[1].getHealth() + "/" + husks[2].getHealth();
            check("удар мечом зефира по среднему кадавру ранил в полную силу и обоих соседей (здоровье " + health + ", износ "
                    + player.getMainHandItem().getDamageValue() + ")", husks[0].getHealth() <= 14f && husks[1].getHealth() <= 14f
                    && husks[2].getHealth() <= 14f && player.getMainHandItem().getDamageValue() == 3);
        });
        double[] startY = new double[1];
        Vec3[] huskAt = new Vec3[1];
        server(10, (server, player) -> {
            for (var husk : husks) {
                husk.discard();
            }
            var cow = net.minecraft.world.entity.EntityType.COW.create(player.serverLevel());
            cow.moveTo(1401.5, -60, 51.5, 0f, 0f);
            cow.setNoAi(false);
            player.serverLevel().addFreshEntity(cow);
            huskAt[0] = cow.position();
        });
        cmd("tp @s 1400.5 -60 50.5 0 -40");
        client(10, mc -> {
            startY[0] = mc.player.getY();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keyUse.setDown(true);
        });
        shot(22, "tools_whirl");
        client(8, mc -> {
            check("зажатый меч зефира поднял игрока вихрем (высота " + (mc.player.getY() - startY[0]) + ")",
                    mc.player.getY() - startY[0] > 1.0);
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        server(5, (server, player) -> {
            var cows = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(60));
            double moved = cows.isEmpty() ? -1 : cows.get(0).position().distanceTo(huskAt[0]);
            check("вихрь оттолкнул стоявшую рядом корову (сдвиг " + moved + ")", moved > 1.0);
            check("игрок жив после проверки инструментов (здоровье " + player.getHealth() + ")", player.isAlive());
        });
        cmd("gamemode creative");
        purge(centre);
    }

    /** Устройства артефактов: лампы, мостовые, решётка, ухо и левитатор. */
    private static void devices() {
        cmd("gamemode creative");
        cmd("time set midnight");
        cmd("difficulty normal");
        BlockPos centre = new BlockPos(1620, -60, 20);
        cmd("tp @s 1622.5 -59 10.5 0 20");
        client(40, mc -> {
        });
        purge(centre);
        net.minecraft.world.entity.animal.Cow[] cows = new net.minecraft.world.entity.animal.Cow[2];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"arcane_lamp", "infusion/lamp_growth", "infusion/lamp_fertility", "travel_paving", "warding_paving",
                    "grate", "arcane_ear", "levitator", "amber_block", "amber_bricks", "amber_from_block"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты ламп, мостовых, решётки, уха, левитатора и янтаря загружены (нет:" + missing + ")", missing.length() == 0);
            var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            var facing = thaumcraft.common.blocks.ArcaneLampBlock.FACING;
            // три чародейские лампы: на полу, на стене, под потолком
            level.setBlock(new BlockPos(1600, -60, 20), ModBlocks.ARCANE_LAMP.get().defaultBlockState(), 3);
            level.setBlock(new BlockPos(1602, -59, 20), stone, 3);
            level.setBlock(new BlockPos(1602, -59, 19), ModBlocks.ARCANE_LAMP.get().defaultBlockState().setValue(facing, Direction.SOUTH), 3);
            level.setBlock(new BlockPos(1604, -57, 20), stone, 3);
            level.setBlock(new BlockPos(1604, -58, 20), ModBlocks.ARCANE_LAMP.get().defaultBlockState().setValue(facing, Direction.UP), 3);
            // лампа роста над банкой Herba среди ростков пшеницы
            BlockPos growth = new BlockPos(1610, -59, 20);
            level.setBlock(growth.below(), ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(growth.below()) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar) {
                jar.add(thaumcraft.api.aspects.Aspect.PLANT, 8);
            }
            level.setBlock(growth, ModBlocks.LAMP_GROWTH.get().defaultBlockState(), 3);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
                        level.setBlock(new BlockPos(1610 + dx, -61, 20 + dz), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7), 3);
                        level.setBlock(new BlockPos(1610 + dx, -60, 20 + dz), net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState(), 3);
                    }
                }
            }
            // лампа плодородия над банкой Victus и две коровы
            BlockPos fertility = new BlockPos(1620, -59, 26);
            level.setBlock(fertility.below(), ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(fertility.below()) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar) {
                jar.add(thaumcraft.api.aspects.Aspect.LIFE, 8);
            }
            level.setBlock(fertility, ModBlocks.LAMP_FERTILITY.get().defaultBlockState(), 3);
            for (int i = 0; i < 2; i++) {
                cows[i] = net.minecraft.world.entity.EntityType.COW.create(level);
                cows[i].setNoAi(true);
                cows[i].moveTo(1618.5 + i * 4, -60, 28.5, 0f, 0f);
                level.addFreshEntity(cows[i]);
            }
            // мостовые путника (дорожка) и защиты
            for (int dz = 12; dz <= 18; dz++) {
                level.setBlock(new BlockPos(1630, -61, dz), ModBlocks.TRAVEL_PAVING.get().defaultBlockState(), 3);
            }
            level.setBlock(new BlockPos(1634, -61, 20), ModBlocks.WARDING_PAVING.get().defaultBlockState(), 3);
            // решётки: открытая и закрытая
            level.setBlock(new BlockPos(1638, -60, 20), ModBlocks.GRATE.get().defaultBlockState(), 3);
            level.setBlock(new BlockPos(1640, -60, 20), ModBlocks.GRATE.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.GrateBlock.OPEN, false), 3);
            for (int i = 0; i < 2; i++) {
                var item = new net.minecraft.world.entity.item.ItemEntity(level, 1638.5 + i * 2, -58.5, 20.5,
                        new ItemStack(net.minecraft.world.item.Items.APPLE));
                item.setDeltaMovement(0, 0, 0);
                level.addFreshEntity(item);
            }
            // ухо и нотный блок на камне, оба на ноте 5
            level.setBlock(new BlockPos(1644, -61, 20), stone, 3);
            level.setBlock(new BlockPos(1644, -60, 20), ModBlocks.ARCANE_EAR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(new BlockPos(1644, -60, 20)) instanceof thaumcraft.common.blocks.ArcaneEarBlock.Sensor sensor) {
                sensor.note = 5;
            }
            level.setBlock(new BlockPos(1647, -61, 20), stone, 3);
            level.setBlock(new BlockPos(1647, -60, 20), net.minecraft.world.level.block.Blocks.NOTE_BLOCK.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.NoteBlock.NOTE, 5)
                    .setValue(net.minecraft.world.level.block.NoteBlock.INSTRUMENT,
                            net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM), 3);
            // левитатор и предмет над ним
            level.setBlock(new BlockPos(1651, -61, 20), ModBlocks.LEVITATOR.get().defaultBlockState(), 3);
            var feather = new net.minecraft.world.entity.item.ItemEntity(level, 1651.5, -59.8, 20.5, new ItemStack(net.minecraft.world.item.Items.FEATHER));
            feather.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(feather);
        });
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.blockEvent(new BlockPos(1647, -60, 20), net.minecraft.world.level.block.Blocks.NOTE_BLOCK, 0, 0);
        });
        server(4, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockState ear = level.getBlockState(new BlockPos(1644, -60, 20));
            check("чародейское ухо услышало свою ноту с нотного блока и дало сигнал (сила " + level.getBestNeighborSignal(new BlockPos(1644, -60, 21))
                    + ")", ear.getValue(thaumcraft.common.blocks.ArcaneEarBlock.POWERED));
            var feathers = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(1650, -62, 19, 1653, -40, 22));
            check("левитатор поднимает перо (высота " + (feathers.isEmpty() ? "нет" : feathers.get(0).getY()) + ", включён "
                    + level.getBlockState(new BlockPos(1651, -61, 20)).getValue(thaumcraft.common.blocks.LevitatorBlock.ACTIVE) + ")",
                    !feathers.isEmpty() && feathers.get(0).getY() > -59.0);
            var apples = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(1637, -62, 19, 1642, -57, 22));
            double open = 99;
            double closed = -99;
            for (var apple : apples) {
                if (apple.getX() < 1639) {
                    open = apple.getY();
                } else {
                    closed = apple.getY();
                }
            }
            check("яблоко провалилось сквозь открытую решётку и легло на закрытую (высоты " + open + " и " + closed + ")",
                    open < -59.2 && closed >= -59.05);
        });
        cmd("tp @s 1630.5 -60 11.5 0 10");
        cmd("gamemode survival");
        client(10, mc -> mc.options.keyUp.setDown(true));
        client(15, mc -> mc.options.keyUp.setDown(false));
        server(2, (server, player) -> check("шаги по мостовой путника дали скорость и прыгучесть (скорость "
                + player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED) + ", прыгучесть "
                + player.hasEffect(net.minecraft.world.effect.MobEffects.JUMP) + ")",
                player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED) && player.hasEffect(net.minecraft.world.effect.MobEffects.JUMP)));
        cmd("gamemode creative");
        server(200, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int lights = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1584, -64, 4), new BlockPos(1620, -40, 36))) {
                if (level.getBlockState(pos).is(ModBlocks.LAMP_LIGHT.get())) {
                    lights++;
                }
            }
            check("чародейские лампы расставили в темноте невидимые огоньки (" + lights + ")", lights > 5);
            int grown = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    var state = level.getBlockState(new BlockPos(1610 + dx, -60, 20 + dz));
                    if (state.is(net.minecraft.world.level.block.Blocks.WHEAT)) {
                        grown += state.getValue(net.minecraft.world.level.block.CropBlock.AGE);
                    }
                }
            }
            int herba = level.getBlockEntity(new BlockPos(1610, -60, 20)) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar
                    ? jar.getAmount() : -1;
            check("лампа роста взяла Herba из банки и подрастила пшеницу (сумма возрастов " + grown + ", в банке осталось " + herba + ", горит "
                    + level.getBlockState(new BlockPos(1610, -59, 20)).getValue(thaumcraft.common.blocks.EssentiaLampBlock.LIT) + ")",
                    grown > 0 && herba < 8);
            check("лампа плодородия свела двух коров (влюблены " + cows[0].isInLove() + "/" + cows[1].isInLove() + ")",
                    cows[0].isInLove() && cows[1].isInLove() || cows[0].getAge() != 0);
            var aura = level.getBlockState(new BlockPos(1634, -60, 20));
            var husk = net.minecraft.world.entity.EntityType.HUSK.create(level);
            husk.moveTo(1634.5, -60, 21.5, 0f, 0f);
            boolean blocksMob = aura.is(ModBlocks.WARD_AURA.get()) && !aura.getCollisionShape(level, new BlockPos(1634, -60, 20),
                    net.minecraft.world.phys.shapes.CollisionContext.of(husk)).isEmpty();
            boolean letsPlayer = aura.is(ModBlocks.WARD_AURA.get()) && aura.getCollisionShape(level, new BlockPos(1634, -60, 20),
                    net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty();
            check("над мостовой защиты встала невидимая стена: мобу не пройти, игроку можно (стена " + aura.getBlock() + ")",
                    blocksMob && letsPlayer && level.getBlockState(new BlockPos(1634, -59, 20)).is(ModBlocks.WARD_AURA.get()));
        });
        // зеркала: связка, переброска предмета, эссенция и ручное зеркало
        net.minecraft.world.entity.item.ItemEntity[] appleRef = new net.minecraft.world.entity.item.ItemEntity[1];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            var facing = thaumcraft.common.blocks.MirrorBlock.FACING;
            for (int x : new int[]{1660, 1666, 1672, 1678}) {
                level.setBlock(new BlockPos(x, -60, 21), stone, 3);
                level.setBlock(new BlockPos(x, -59, 21), stone, 3);
            }
            boolean[] ok = {true, true};
            for (int pair = 0; pair < 2; pair++) {
                var block = pair == 0 ? ModBlocks.MAGIC_MIRROR.get() : ModBlocks.ESSENTIA_MIRROR.get();
                var item = pair == 0 ? ModItems.MAGIC_MIRROR.get() : ModItems.ESSENTIA_MIRROR.get();
                BlockPos first = new BlockPos(1660 + pair * 12, -60, 20);
                level.setBlock(first, block.defaultBlockState().setValue(facing, Direction.NORTH), 3);
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                item.onItemUseFirst(player.getMainHandItem(), new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(first), Direction.NORTH, first, false)));
                ItemStack linked = ItemStack.EMPTY;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    if (thaumcraft.common.items.MirrorItem.isLinked(player.getInventory().getItem(slot))) {
                        linked = player.getInventory().getItem(slot).copy();
                    }
                }
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, linked);
                BlockPos wall = new BlockPos(1666 + pair * 12, -60, 21);
                linked.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(wall), Direction.NORTH, wall, false)));
                var a = level.getBlockEntity(first);
                var b = level.getBlockEntity(wall.north());
                ok[pair] = a instanceof thaumcraft.common.blocks.MirrorBlock.Mirror ma && ma.linked
                        && b instanceof thaumcraft.common.blocks.MirrorBlock.Mirror mb && mb.linked && ma.isLinkValid();
            }
            check("связанное зеркало, поставленное в другом месте, стало парой первому (магические " + ok[0] + ", эссенции " + ok[1] + ")",
                    ok[0] && ok[1]);
            var apple = new net.minecraft.world.entity.item.ItemEntity(level, 1660.5, -59.6, 20.6, new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE));
            apple.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(apple);
            appleRef[0] = apple;
            BlockPos jarPos = new BlockPos(1678, -60, 17);
            level.setBlock(jarPos, ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(jarPos) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar) {
                jar.add(thaumcraft.api.aspects.Aspect.FIRE, 5);
            }
            boolean took = level.getBlockEntity(new BlockPos(1672, -60, 20)) instanceof thaumcraft.common.blocks.MirrorBlock.Mirror em
                    && em.takeEssentia(thaumcraft.api.aspects.Aspect.FIRE);
            int left = level.getBlockEntity(jarPos) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar ? jar.getAmount() : -1;
            check("зеркало эссенции взяло Ignis из банки перед своей парой (взято " + took + ", в банке осталось " + left + ")",
                    took && left == 4);
            ItemStack hand = new ItemStack(ModItems.HAND_MIRROR.get());
            thaumcraft.common.items.MirrorItem.setLink(hand, new BlockPos(1666, -60, 20), level.dimension().location());
            check("ручное зеркало отправило алмаз в магическое зеркало",
                    thaumcraft.common.items.HandMirrorItem.transport(hand, new ItemStack(net.minecraft.world.item.Items.DIAMOND), player));
        });
        server(3, (server, player) -> Thaumcraft.LOGGER.info("AUTOSHOT DIAG яблоко: жив {} в {} задержка портала {}, нестабильность зеркала {}",
                appleRef[0].isAlive(), appleRef[0].position(), appleRef[0].getPortalCooldown(),
                player.serverLevel().getBlockEntity(new BlockPos(1660, -60, 20)) instanceof thaumcraft.common.blocks.MirrorBlock.Mirror m ? m.instability : -1));
        server(60, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean apple = false;
            boolean diamond = false;
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(1664, -62, 16, 1669, -57, 22))) {
                apple |= item.getItem().is(net.minecraft.world.item.Items.GOLDEN_APPLE);
                diamond |= item.getItem().is(net.minecraft.world.item.Items.DIAMOND);
            }
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(1650, -64, 10, 1690, -50, 30))) {
                Thaumcraft.LOGGER.info("AUTOSHOT DIAG предмет у зеркал: {} в {}", item.getItem(), item.position());
            }
            int queued = level.getBlockEntity(new BlockPos(1666, -60, 20)) instanceof thaumcraft.common.blocks.MirrorBlock.Mirror mb ? mb.pending() : -1;
            check("золотое яблоко ушло в зеркало (" + !appleRef[0].isAlive() + ") и выпало из его пары (" + apple + ", в очереди " + queued
                    + "), алмаз из ручного зеркала выпал (" + diamond + ")", !appleRef[0].isAlive() && apple && diamond);
        });
        cmd("tp @s 1669.5 -59.5 14.5 10 10");
        shot(20, "devices_mirrors");
        cmd("tp @s 1634.5 -59.5 17.5 0 35");
        server(2, (server, player) -> {
            var husk = net.minecraft.world.entity.EntityType.HUSK.create(player.serverLevel());
            husk.setNoAi(true);
            husk.moveTo(1635.5, -60, 20.5, 0f, 0f);
            player.serverLevel().addFreshEntity(husk);
        });
        shot(30, "devices_ward");
        // мозг в банке, чародейская дверь с ключами и плитой, защищённое стекло
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(new BlockPos(1690, -60, 20), ModBlocks.BRAIN_JAR.get().defaultBlockState(), 3);
            var orb = new net.minecraft.world.entity.ExperienceOrb(level, 1693.5, -59.5, 20.5, 10);
            level.addFreshEntity(orb);
            // дверь ставит игрок: он и хозяин
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARCANE_DOOR.get()));
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(1700.5, -60, 20.5), Direction.UP, new BlockPos(1700, -61, 20), false)));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARCANE_PRESSURE_PLATE.get()));
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(1701.5, -60, 20.5), Direction.UP, new BlockPos(1701, -61, 20), false)));
            for (int dx = 0; dx < 3; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(new BlockPos(1706 + dx, -60 + dy, 22), ModBlocks.WARDED_GLASS.get().defaultBlockState(), 3);
                }
            }
        });
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos door = new BlockPos(1700, -60, 20);
            var stranger = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.fromString("11111111-2222-3333-4444-555555555555"), "stranger"));
            stranger.moveTo(1700.5, -60, 18.5);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(door), Direction.NORTH, door, false);
            level.getBlockState(door).use(level, stranger, InteractionHand.MAIN_HAND, hit);
            boolean refused = !level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
            level.getBlockState(door).use(level, player, InteractionHand.MAIN_HAND, hit);
            boolean opened = level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
            level.getBlockState(door).use(level, player, InteractionHand.MAIN_HAND, hit);
            Thaumcraft.LOGGER.info("AUTOSHOT DIAG дверь после закрытия хозяином: открыта {}", level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN));
            check("чародейская дверь не поддалась чужаку (" + refused + "), а хозяину открылась (" + opened + ")", refused && opened);
            // хозяин нарезает железный ключ и отдаёт чужаку; тот щёлкает им по двери и получает доступ
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KEY_IRON.get()));
            ModItems.KEY_IRON.get().onItemUseFirst(player.getMainHandItem(), new net.minecraft.world.item.context.UseOnContext(player,
                    InteractionHand.MAIN_HAND, hit));
            ItemStack cut = ItemStack.EMPTY;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).is(ModItems.KEY_IRON.get()) && player.getInventory().getItem(slot).hasTag()) {
                    cut = player.getInventory().getItem(slot).copy();
                }
            }
            StringBuilder inv = new StringBuilder();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (!player.getInventory().getItem(slot).isEmpty()) {
                    inv.append(slot).append('=').append(player.getInventory().getItem(slot)).append(player.getInventory().getItem(slot).getTag()).append(' ');
                }
            }
            Thaumcraft.LOGGER.info("AUTOSHOT DIAG сумка после нарезки: {} дверь открыта {}", inv, level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN));
            boolean gotKey = !cut.isEmpty();
            stranger.setItemInHand(InteractionHand.MAIN_HAND, cut);
            if (!cut.isEmpty()) {
                ModItems.KEY_IRON.get().onItemUseFirst(stranger.getMainHandItem(), new net.minecraft.world.item.context.UseOnContext(stranger,
                        InteractionHand.MAIN_HAND, hit));
            }
            level.getBlockState(door).use(level, stranger, InteractionHand.MAIN_HAND, hit);
            boolean keyed = level.getBlockState(door).getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
            level.getBlockState(door).use(level, player, InteractionHand.MAIN_HAND, hit);
            check("нарезанный железный ключ открыл дверь чужаку (ключ " + gotKey + ", открыл " + keyed + ", ключ израсходован " + cut.isEmpty() + ")",
                    gotKey && keyed && cut.isEmpty());
            var apple = new net.minecraft.world.entity.item.ItemEntity(level, 1701.5, -59.7, 20.5, new ItemStack(net.minecraft.world.item.Items.APPLE));
            apple.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(apple);
            level.explode(null, 1707.5, -58, 20.5, 2.5f, Level.ExplosionInteraction.BLOCK);
        });
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean pressed = level.getBlockState(new BlockPos(1701, -60, 20)).getValue(thaumcraft.common.blocks.ArcanePressurePlateBlock.POWERED);
            boolean open = level.getBlockState(new BlockPos(1700, -60, 20)).getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
            check("яблоко на чародейской плите открыло соседнюю дверь того же хозяина (плита " + pressed + ", дверь " + open + ")",
                    pressed && open);
            int glass = 0;
            for (int dx = 0; dx < 3; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    if (level.getBlockState(new BlockPos(1706 + dx, -60 + dy, 22)).is(ModBlocks.WARDED_GLASS.get())) {
                        glass++;
                    }
                }
            }
            check("защищённое стекло пережило взрыв (цело " + glass + " из 9)", glass == 9);
            int xp = level.getBlockEntity(new BlockPos(1690, -60, 20)) instanceof thaumcraft.common.blocks.BrainJarBlock.Brain brain ? brain.xp : -1;
            check("мозг в банке подтянул и съел сферу опыта (" + xp + ")", xp >= 10);
        });
        cmd("tp @s 1701.5 -58.5 15.5 10 20");
        shot(20, "devices_warded");
        cmd("tp @s 1690.5 -59.3 18.3 0 25");
        shot(10, "devices_brain");
        cmd("tp @s 1606.5 -57.5 13.5 -20 25");
        shot(20, "devices_lamps");
        cmd("tp @s 1641.5 -57.5 14.5 0 30");
        shot(20, "devices_misc");
        cmd("time set noon");
        purge(centre);
    }

    /** Костяной лук и первозданные стрелы. */
    private static void bows() {
        cmd("gamemode survival");
        cmd("time set midnight");
        cmd("difficulty normal");
        cmd("effect give @s minecraft:resistance 600 4 true");
        BlockPos centre = new BlockPos(1800, -60, 20);
        cmd("tp @s 1800.5 -60 10.5 0 0");
        client(40, mc -> {
        });
        purge(centre);
        net.minecraft.world.entity.monster.Husk[] husks = new net.minecraft.world.entity.monster.Husk[7];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"bone_bow", "primal_arrow_air", "primal_arrow_fire", "primal_arrow_water", "primal_arrow_earth",
                    "primal_arrow_order", "primal_arrow_entropy"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты костяного лука и шести первозданных стрел загружены (нет:" + missing + ")", missing.length() == 0);
            // семь кадавров в алмазной броне: шесть под стрелы стихий и один под обычную
            for (int i = 0; i < 7; i++) {
                husks[i] = net.minecraft.world.entity.EntityType.HUSK.create(level);
                husks[i].setNoAi(true);
                husks[i].moveTo(1794.5 + i * 2, -60, 16.5, 180f, 0f);
                for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                        net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                        net.minecraft.world.entity.EquipmentSlot.FEET}) {
                    husks[i].setItemSlot(slot, new ItemStack(switch (slot) {
                        case HEAD -> net.minecraft.world.item.Items.DIAMOND_HELMET;
                        case CHEST -> net.minecraft.world.item.Items.DIAMOND_CHESTPLATE;
                        case LEGS -> net.minecraft.world.item.Items.DIAMOND_LEGGINGS;
                        default -> net.minecraft.world.item.Items.DIAMOND_BOOTS;
                    }));
                }
                level.addFreshEntity(husks[i]);
            }
            for (int i = 0; i < 7; i++) {
                net.minecraft.world.entity.projectile.AbstractArrow arrow = i < 6
                        ? new thaumcraft.common.entity.PrimalArrow(level, player, i)
                        : new net.minecraft.world.entity.projectile.Arrow(level, player);
                arrow.moveTo(1794.5 + i * 2, -59, 12.5, 0f, 0f);
                arrow.setDeltaMovement(0, 0, 2.0);
                level.addFreshEntity(arrow);
            }
        });
        server(20, (server, player) -> {
            StringBuilder health = new StringBuilder();
            for (var husk : husks) {
                health.append(husk.getHealth()).append(' ');
            }
            check("воздушная и упорядоченная стрелы бьют сквозь алмазную броню сильнее обычной (здоровье " + health + ")",
                    husks[0].getHealth() < husks[6].getHealth() && husks[4].getHealth() < husks[6].getHealth());
            check("огненная стрела подожгла, водяная замедлила, упорядоченная ослабила, энтропийная иссушила",
                    husks[1].isOnFire() && husks[2].hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                            && husks[4].hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                            && husks[5].hasEffect(net.minecraft.world.effect.MobEffects.WITHER));
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BONE_BOW.get()));
            player.getInventory().add(new ItemStack(ModItems.PRIMAL_ARROWS.get(3).get(), 4));
        });
        cmd("tp @s 1800.5 -60 10.5 180 -5");
        client(10, mc -> mc.options.keyUse.setDown(true));
        client(11, mc -> mc.options.keyUse.setDown(false));
        server(2, (server, player) -> {
            var arrows = player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.PrimalArrow.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(40), a -> a.getOwner() == player && a.tickCount < 10);
            double speed = arrows.isEmpty() ? -1 : arrows.get(0).getDeltaMovement().length();
            check("костяной лук за 11 тактов натянулся до полной силы и выпустил земляную стрелу (стрел " + arrows.size() + ", скорость "
                    + speed + ", урон " + (arrows.isEmpty() ? -1 : arrows.get(0).getBaseDamage()) + ", осталось стрел "
                    + player.getInventory().countItem(ModItems.PRIMAL_ARROWS.get(3).get()) + ")",
                    arrows.size() == 1 && speed > 3.3 && Math.abs(arrows.get(0).getBaseDamage() - 2.6) < 0.01
                            && player.getInventory().countItem(ModItems.PRIMAL_ARROWS.get(3).get()) == 3);
        });
        cmd("tp @s 1800.5 -59 11.5 0 10");
        server(2, (server, player) -> {
            for (int i = 0; i < 6; i++) {
                var arrow = new thaumcraft.common.entity.PrimalArrow(player.serverLevel(), player, i);
                arrow.moveTo(1795.5 + i * 2, -58.5, 13.5, 0f, 0f);
                arrow.setDeltaMovement(0, 0.0, 0.05);
                arrow.setNoGravity(true);
                player.serverLevel().addFreshEntity(arrow);
            }
        });
        shot(6, "bows_arrows");
        cmd("time set noon");
        cmd("gamemode creative");
        purge(centre);
    }

    /** Доспехи и одежда: крепостной комплект с маской, мантии, сапоги странника, летательная упряжь. */
    private static void wear() {
        cmd("gamemode survival");
        cmd("time set noon");
        cmd("difficulty peaceful");
        BlockPos centre = new BlockPos(1900, -60, 20);
        cmd("tp @s 1900.5 -60 20.5 0 0");
        client(40, mc -> {
        });
        purge(centre);
        // прежние сцены могли оставить украшения, эффекты и рунный щит — броню проверяем на чистом игроке
        server(2, (server, player) -> {
            player.removeAllEffects();
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getCurios().values().forEach(slots -> {
                        for (int i = 0; i < slots.getSlots(); i++) {
                            slots.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }));
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            thaumcraft.common.runic.RunicShield.setCharge(player, 0);
        });
        float[] before = new float[1];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"infusion/fortress_helmet", "infusion/fortress_chestplate", "infusion/fortress_leggings",
                    "infusion/fortress_goggles", "infusion/mask_grinning_devil", "infusion/mask_angry_ghost", "infusion/mask_sipping_fiend",
                    "robe_chestplate", "robe_leggings", "robe_boots", "infusion/traveller_boots", "infusion/hover_harness",
                    "infusion/hover_girdle"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты доспехов, масок, мантий, сапог, упряжи и пояса загружены (нет:" + missing + ")", missing.length() == 0);
            player.getInventory().clearContent();
            ItemStack helm = new ItemStack(ModItems.FORTRESS_HELMET.get());
            helm.getOrCreateTag().putInt("mask", 0);
            helm.getTag().putBoolean("goggles", true);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helm);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.FORTRESS_CHESTPLATE.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.FORTRESS_LEGGINGS.get()));
            player.setHealth(20f);
            player.setAbsorptionAmount(0f);
            player.removeAllEffects();
            thaumcraft.common.runic.RunicShield.setCharge(player, 0);
            player.invulnerableTime = 0;
            String gear = "броня " + player.getArmorValue() + ", прочность " + player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)
                    + ", рунный щит " + thaumcraft.common.runic.RunicShield.getCharge(player) + "/" + thaumcraft.common.runic.RunicShield.getMax(player)
                    + ", эффекты " + player.getActiveEffects().size();
            player.hurt(level.damageSources().magic(), 10f);
            float magic = 20f - player.getHealth();
            player.setHealth(20f);
            player.invulnerableTime = 0;
            player.hurt(level.damageSources().inFire(), 4f);
            float fire = 20f - player.getHealth();
            check("полный крепостной комплект с маской смягчил магию (из 10 прошло " + magic + ") и погасил огонь (из 4 прошло " + fire + "; " + gear + ")",
                    magic > 3.5f && magic < 4.6f && fire < 0.01f);
            check("очки на крепостном шлеме показывают узлы и значки сосудов",
                    thaumcraft.api.nodes.Revealer.class.isInstance(helm.getItem())
                            && ((thaumcraft.api.nodes.Revealer) helm.getItem()).showNodes(helm, player)
                            && thaumcraft.api.Goggles.wornBy(player));
            ItemStack robe = new ItemStack(ModItems.ROBE_CHESTPLATE.get());
            int plain = ((thaumcraft.common.items.armor.RobeArmorItem) robe.getItem()).getColor(robe);
            ItemStack dyed = net.minecraft.world.item.DyeableLeatherItem.dyeArmor(robe, java.util.List.of(
                    (net.minecraft.world.item.DyeItem) net.minecraft.world.item.Items.RED_DYE));
            int red = ((thaumcraft.common.items.armor.RobeArmorItem) dyed.getItem()).getColor(dyed);
            check("мантия лиловая (" + Integer.toHexString(plain) + "), красится красителем (" + Integer.toHexString(red)
                    + ") и отмывается в котле", plain == 6961280 && red != plain
                    && net.minecraft.core.cauldron.CauldronInteraction.WATER.containsKey(ModItems.ROBE_CHESTPLATE.get()));
        });
        cmd("tp @s 1900.5 -60 20.5 180 0");
        cmd("time set midnight");
        client(5, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(10, "wear_fortress");
        client(2, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        // сапоги странника: шаг через блок и бег быстрее
        double[] start = new double[1];
        server(5, (server, player) -> {
            player.getInventory().clearContent();
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS}) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.TRAVELLER_BOOTS.get()));
        });
        cmd("tp @s 1900.5 -60 20.5 0 0");
        client(10, mc -> {
            start[0] = mc.player.getZ();
            mc.options.keyUp.setDown(true);
        });
        client(20, mc -> {
            var step = mc.player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
            double walked = mc.player.getZ() - start[0];
            mc.options.keyUp.setDown(false);
            check("в сапогах странника за секунду пройдено " + walked + " блока (без них ~4.3) и шаг стал через блок (прибавка "
                    + (step == null ? -1 : step.getValue()) + ")", walked > 5.0 && step != null && step.getValue() >= 0.39);
        });
        // летательная упряжь: банка Potentia, клавиша парения, полёт
        server(5, (server, player) -> {
            ItemStack harness = new ItemStack(ModItems.HOVER_HARNESS.get());
            ItemStack jar = new ItemStack(ModItems.WARDED_JAR.get());
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putString("Aspect", thaumcraft.api.aspects.Aspect.ENERGY.getTag());
            data.putShort("Amount", (short) 10);
            jar.getOrCreateTag().put("BlockEntityTag", data);
            thaumcraft.common.items.armor.HoverHarnessItem.setJar(harness, jar);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, harness);
        });
        cmd("tp @s 1910.5 -59 20.5 180 0");
        client(5, mc -> thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.HoverPacket()));
        client(5, mc -> mc.options.keyJump.setDown(true));
        client(15, mc -> mc.options.keyJump.setDown(false));
        server(10, (server, player) -> {
            ItemStack harness = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            check("клавиша парения подняла игрока в упряжи (парит " + thaumcraft.common.items.armor.HoverHarnessItem.isHovering(player)
                    + ", летит " + player.getAbilities().flying + ", над землёй " + (player.getY() + 60) + ", заряд "
                    + (harness.hasTag() ? harness.getTag().getShort("charge") : -1) + ", Potentia "
                    + thaumcraft.common.items.armor.HoverHarnessItem.getFuel(harness) + ")",
                    thaumcraft.common.items.armor.HoverHarnessItem.isHovering(player) && player.getAbilities().flying && player.getY() > -59.5
                            && harness.getTag().getShort("charge") > 0);
        });
        client(5, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(10, "wear_harness");
        client(2, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        client(5, mc -> thaumcraft.common.network.ModNetwork.CHANNEL.sendToServer(new thaumcraft.common.network.HoverPacket()));
        server(15, (server, player) -> check("повторное нажатие выключило парение (парит " + thaumcraft.common.items.armor.HoverHarnessItem.isHovering(player)
                + ", может летать " + player.getAbilities().mayfly + ")", !thaumcraft.common.items.armor.HoverHarnessItem.isHovering(player)
                && !player.getAbilities().mayfly));
        cmd("time set noon");
        cmd("gamemode creative");
        server(2, (server, player) -> {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        });
        cmd("difficulty normal");
        purge(centre);
    }

    /** Знамёна, зловещий камень и очиститель скверны. */
    private static void sundries() {
        cmd("gamemode creative");
        cmd("time set noon");
        BlockPos centre = new BlockPos(2000, -60, 20);
        cmd("tp @s 2000.5 -60 14.5 0 10");
        client(40, mc -> {
        });
        purge(centre);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"thaum_banner_white", "thaum_banner_purple", "thaum_banner_black", "infusion/sinister_stone",
                    "flux_scrubber"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты знамён, зловещего камня и очистителя загружены (нет:" + missing + ")", missing.length() == 0);
            // знамёна: культа, лиловое с аспектом, на стене
            for (int i = 0; i < 3; i++) {
                ItemStack banner = new ItemStack(ModItems.THAUM_BANNER.get());
                if (i > 0) {
                    banner.getOrCreateTag().putByte("color", (byte) (i == 1 ? 10 : 14));
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, banner);
                BlockPos ground = new BlockPos(1997 + i * 3, -61, 20);
                Direction face = Direction.UP;
                if (i == 2) {
                    level.setBlock(new BlockPos(2003, -60, 21), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(2003, -59, 21), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                    ground = new BlockPos(2003, -60, 21);
                    face = Direction.NORTH;
                }
                banner.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(ground), face, ground, false)));
            }
            BlockPos purple = new BlockPos(2000, -60, 20);
            ItemStack phial = thaumcraft.common.items.PhialItem.filled(ModItems.PHIAL.get(), thaumcraft.api.aspects.Aspect.MAGIC);
            player.setItemInHand(InteractionHand.MAIN_HAND, phial);
            level.getBlockState(purple).use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(purple), Direction.NORTH,
                    purple, false));
            boolean painted = level.getBlockEntity(purple) instanceof thaumcraft.common.blocks.ThaumBannerBlock.Banner b
                    && b.color == 10 && b.aspect == thaumcraft.api.aspects.Aspect.MAGIC;
            check("лиловое знамя встало и приняло знак Praecantatio из фиала (" + painted + "), знамя на стене встало ("
                    + level.getBlockState(new BlockPos(2003, -60, 20)).is(ModBlocks.THAUM_BANNER.get()) + ")",
                    painted && level.getBlockState(new BlockPos(2003, -60, 20)).is(ModBlocks.THAUM_BANNER.get()));
            // очиститель скверны среди слизи; заряд вис ему даём сами
            level.setBlock(new BlockPos(2010, -60, 20), ModBlocks.FLUX_SCRUBBER.get().defaultBlockState(), 3);
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = 3; dz <= 9; dz++) {
                    level.setBlock(new BlockPos(2010 + dx, -60, 20 + dz), ModBlocks.FLUX_GOO.get().defaultBlockState(), 3);
                }
            }
        });
        server(60, (server, player) -> {
            ServerLevel level = player.serverLevel();
            if (level.getBlockEntity(new BlockPos(2010, -60, 20)) instanceof thaumcraft.common.blocks.FluxScrubberBlock.Scrubber scrubber) {
                scrubber.power = 1000;
            }
        });
        server(200, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int goo = 0;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = 3; dz <= 9; dz++) {
                    if (level.getBlockState(new BlockPos(2010 + dx, -60, 20 + dz)).is(ModBlocks.FLUX_GOO.get())) {
                        goo++;
                    }
                }
            }
            check("очиститель скверны вычистил часть слизи вокруг себя (осталось " + goo + " из 77)", goo < 77);
            // тёмный узел и зловещий камень
            level.setBlock(new BlockPos(2000, -55, 40), ModBlocks.AURA_NODE.get().defaultBlockState(), 3);
            if (level.getBlockEntity(new BlockPos(2000, -55, 40)) instanceof thaumcraft.common.blockentity.NodeBlockEntity node) {
                node.setNodeType(thaumcraft.api.nodes.NodeType.DARK);
                node.setChanged();
                level.sendBlockUpdated(node.getBlockPos(), node.getBlockState(), node.getBlockState(), 3);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SINISTER_STONE.get()));
        });
        cmd("tp @s 2000.5 -60 30.5 0 -5");
        client(80, mc -> check("зловещий камень разгорелся, когда игрок посмотрел на тёмный узел",
                thaumcraft.common.items.SinisterStoneItem.isActive()));
        // чародейский бур: основание, бур на нём, фокус и кирка, сигнал красного камня, каменная стена впереди
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos basePos = new BlockPos(2030, -60, 20);
            level.setBlock(basePos, ModBlocks.ARCANE_BORE_BASE.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.ArcaneBoreBaseBlock.FACING, Direction.EAST), 3);
            level.setBlock(basePos.above(), ModBlocks.ARCANE_BORE.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.ArcaneBoreBlock.FACING, Direction.NORTH), 3);
            level.setBlock(basePos.east(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(basePos.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 4; dy++) {
                    for (int dz = 1; dz <= 3; dz++) {
                        level.setBlock(new BlockPos(2030 + dx, -59 + dy, 20 - 2 - dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                    }
                }
            }
            if (level.getBlockEntity(basePos.above()) instanceof thaumcraft.common.blocks.ArcaneBoreBlock.Bore bore) {
                bore.setItem(0, new ItemStack(ModItems.FOCUS_EXCAVATION.get()));
                bore.setItem(1, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
            }
        });
        cmd("tp @s 2033.5 -58 23.5 135 20");
        shot(200, "sundries_bore");
        server(400, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int dug = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 4; dy++) {
                    for (int dz = 1; dz <= 3; dz++) {
                        if (level.getBlockState(new BlockPos(2030 + dx, -59 + dy, 20 - 2 - dz)).isAir()) {
                            dug++;
                        }
                    }
                }
            }
            int stored = 0;
            if (level.getBlockEntity(new BlockPos(2031, -60, 20)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    stored += chest.getItem(slot).getCount();
                }
            }
            int wear = level.getBlockEntity(new BlockPos(2030, -59, 20)) instanceof thaumcraft.common.blocks.ArcaneBoreBlock.Bore bore
                    ? bore.getItem(1).getDamageValue() : -1;
            check("чародейский бур прогрыз камень впереди (вынуто " + dug + " блоков), добыча легла в сундук у носика (" + stored
                    + "), кирка изнашивается (" + wear + ")", dug >= 3 && stored >= 1 && wear >= 1);
        });
        cmd("tp @s 2001.5 -59 14.5 0 15");
        shot(10, "sundries");
        purge(centre);
    }

    /** Адская печь: сборка жезлом, плавка с бонусами, мехи и сопло с Ignis, распад с ифритом. */
    private static void furnace() {
        cmd("gamemode creative");
        cmd("time set noon");
        BlockPos centre = new BlockPos(2100, -60, 20);
        cmd("tp @s 2101.5 -60 14.5 0 10");
        client(40, mc -> {
        });
        purge(centre);
        BlockPos origin = new BlockPos(2100, -60, 20);
        BlockPos core = origin.offset(1, 1, 1);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    for (int x = 0; x < 3; x++) {
                        BlockPos pos = origin.offset(x, y, z);
                        net.minecraft.world.level.block.state.BlockState state;
                        if (x == 1 && z == 1) {
                            state = y == 0 ? net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()
                                    : y == 1 ? net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState()
                                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                        } else if (x != 1 && z != 1) {
                            state = net.minecraft.world.level.block.Blocks.NETHER_BRICKS.defaultBlockState();
                        } else if (y == 1 && x == 1 && z == 0) {
                            state = net.minecraft.world.level.block.Blocks.IRON_BARS.defaultBlockState();
                        } else {
                            state = net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState();
                        }
                        level.setBlock(pos, state, 3);
                    }
                }
            }
            thaumcraft.common.research.PlayerKnowledge.get(player).completeResearch("INFERNALFURNACE");
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.SILVERWOOD,
                    thaumcraft.api.wands.WandCap.THAUMIUM, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            BlockPos clicked = origin.offset(0, 2, 1);
            wand.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
            net.minecraft.world.level.block.state.BlockState formed = level.getBlockState(core);
            int furnaceBlocks = 0;
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(2, 2, 2))) {
                if (level.getBlockState(pos).is(ModBlocks.INFERNAL_FURNACE.get())) {
                    furnaceBlocks++;
                }
            }
            boolean coreOk = formed.is(ModBlocks.INFERNAL_FURNACE.get()) && thaumcraft.common.blocks.InfernalFurnaceBlock.isCore(formed);
            check("жезл собрал адскую печь (" + furnaceBlocks + " блоков из 26, ядро на месте: " + coreOk + ")", furnaceBlocks == 26 && coreOk);
            // три меха через блок от ядра (запад, восток, юг) и банка с Ignis под соплом в дне
            level.setBlock(core.west(2), ModBlocks.BELLOWS.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BellowsBlock.FACING, Direction.EAST), 3);
            level.setBlock(core.east(2), ModBlocks.BELLOWS.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BellowsBlock.FACING, Direction.WEST), 3);
            level.setBlock(core.south(2), ModBlocks.BELLOWS.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.BellowsBlock.FACING, Direction.NORTH), 3);
            level.setBlock(origin.offset(1, -1, 1), ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(origin.offset(1, -1, 1)) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar) {
                jar.add(thaumcraft.api.aspects.Aspect.FIRE, 16);
            }
            // в отверстие сверху: сырое железо, говядина и палки (не плавятся — сгорят)
            for (ItemStack stack : new ItemStack[]{new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 8),
                    new ItemStack(net.minecraft.world.item.Items.BEEF, 4), new ItemStack(net.minecraft.world.item.Items.STICK, 2)}) {
                net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, core.getX() + 0.5,
                        core.getY() + 1.6, core.getZ() + 0.5, stack);
                item.setDeltaMovement(0, 0, 0);
                level.addFreshEntity(item);
            }
        });
        cmd("tp @s 2101.5 -60 15.5 0 15");
        shot(60, "furnace_front");
        server(1, (server, player) -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        cmd("tp @s 2104.5 -56 17.5 45 40");
        shot(10, "furnace_top");
        cmd("tp @s 2101.5 -60 13.5 0 10");
        server(400, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int ingots = 0;
            int cooked = 0;
            int nuggets = 0;
            int sticks = 0;
            for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(core).inflate(6))) {
                ItemStack stack = item.getItem();
                if (stack.is(net.minecraft.world.item.Items.IRON_INGOT)) {
                    ingots += stack.getCount();
                } else if (stack.is(net.minecraft.world.item.Items.COOKED_BEEF)) {
                    cooked += stack.getCount();
                } else if (stack.is(net.minecraft.world.item.Items.IRON_NUGGET) || stack.is(ModItems.NUGGET_BEEF.get())) {
                    nuggets += stack.getCount();
                } else if (stack.is(net.minecraft.world.item.Items.STICK)) {
                    sticks += stack.getCount();
                }
            }
            int left = level.getBlockEntity(core) instanceof thaumcraft.common.blocks.InfernalFurnaceBlock.Core furnace ? furnace.queued() : -1;
            int ignis = level.getBlockEntity(origin.offset(1, -1, 1)) instanceof thaumcraft.common.blockentity.EssentiaBlockEntity jar
                    ? jar.getAmount() : -1;
            check("печь переплавила сырое железо (" + ingots + " слитков из 8) и говядину (" + cooked + " из 4), бонусных самородков и кусочков "
                    + nuggets + ", палки сгорели (" + sticks + " осталось, в печи " + left + "), сопло вытянуло Ignis из банки (" + ignis + " из 16)",
                    ingots == 8 && cooked == 4 && nuggets > 0 && sticks == 0 && left == 0 && ignis < 16);
        });
        shot(5, "furnace_spit");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.destroyBlock(origin, true, player);
            int blazes = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Blaze.class,
                    new net.minecraft.world.phys.AABB(core).inflate(4)).size();
            boolean restored = level.getBlockState(origin.offset(1, 0, 0)).is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                    && level.getBlockState(origin.offset(2, 2, 2)).is(net.minecraft.world.level.block.Blocks.NETHER_BRICKS)
                    && level.getBlockState(origin.offset(1, 1, 0)).is(net.minecraft.world.level.block.Blocks.IRON_BARS)
                    && level.getBlockState(core).isAir();
            check("сломанный угол обрушил печь: блоки вернулись в обсидиан, кирпич и решётку, лава пропала (" + restored
                    + "), вырвался ифрит (" + blazes + ")", restored && blazes == 1);
        });
        purge(centre);
    }


    /** Постройки мира: курган, каменный круг на холме, тотем; урны и ящики; эссенции из разбитого узла. */
    private static void worldgen() {
        cmd("gamemode creative");
        cmd("time set noon");
        BlockPos centre = new BlockPos(2200, -60, 20);
        cmd("tp @s 2200.5 -60 0.5 0 10");
        client(40, mc -> {
        });
        purge(centre);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7);
            // курган: под ним нужна толща земли — насыпаем площадку повыше плоского мира
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2190, -61, 10), new BlockPos(2215, -32, 35))) {
                level.setBlock(pos, pos.getY() == -32 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 2);
            }
            boolean mound = thaumcraft.common.world.ThaumcraftStructuresFeature.mound(level, random, new BlockPos(2193, -41, 13));
            int spawners = 0;
            int loot = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2193, -45, 13), new BlockPos(2211, -25, 31))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.is(net.minecraft.world.level.block.Blocks.SPAWNER)) {
                    spawners++;
                }
                if (state.getBlock() instanceof thaumcraft.common.blocks.LootBlock) {
                    loot++;
                }
            }
            check("курган насыпан (" + mound + "), в склепе два рассадника (" + spawners + ") и две урны или ящика (" + loot + ")",
                    mound && spawners == 2 && loot == 2);
            // каменный круг на вершине: нужен холм выше 85-го блока
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2240, 85, 15), new BlockPos(2250, 89, 25))) {
                level.setBlock(pos, pos.getY() == 89 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            }
            boolean hill = thaumcraft.common.world.ThaumcraftStructuresFeature.hilltopStones(level, random, new BlockPos(2245, 89, 20));
            boolean wispSpawner = level.getBlockState(new BlockPos(2245, 89, 20)).is(net.minecraft.world.level.block.Blocks.SPAWNER);
            boolean chest = level.getBlockState(new BlockPos(2245, 91, 20)).is(net.minecraft.world.level.block.Blocks.CHEST);
            int totems = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2241, 90, 16), new BlockPos(2249, 95, 24))) {
                if (level.getBlockState(pos).is(ModBlocks.OBSIDIAN_TOTEM.get())) {
                    totems++;
                }
            }
            check("каменный круг на холме (" + hill + "): рассадник виспов (" + wispSpawner + "), сундук (" + chest + "), тотемов в кольце "
                    + totems, hill && wispSpawner && chest && totems >= 12);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, new BlockPos(2245, 94, 20), random, false, true);
            // тотем на плоском мире
            boolean totem = false;
            for (int attempt = 0; attempt < 5 && !totem; attempt++) {
                totem = thaumcraft.common.world.ThaumcraftStructuresFeature.totem(level, random, new BlockPos(2225 + attempt, 0, 5));
            }
            BlockPos charged = null;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2225, -60, 5), new BlockPos(2230, -54, 5))) {
                if (level.getBlockState(pos).is(ModBlocks.CHARGED_OBSIDIAN_TOTEM.get())) {
                    charged = pos.immutable();
                }
            }
            boolean dark = charged != null && level.getBlockEntity(charged) instanceof thaumcraft.common.blockentity.NodeBlockEntity node
                    && node.getNodeType() == thaumcraft.api.nodes.NodeType.DARK && node.visSize() > 0;
            check("обсидиановый тотем встал (" + totem + "), на вершине заряженный тотем с тёмным узлом (" + dark + ")", totem && dark);
            // урны трёх редкостей и ящики разбиваются в добычу
            int drops = 0;
            net.minecraft.world.level.block.Block[] lootBlocks = {ModBlocks.LOOT_URN_COMMON.get(), ModBlocks.LOOT_URN_UNCOMMON.get(), ModBlocks.LOOT_URN_RARE.get(),
                    ModBlocks.LOOT_CRATE_COMMON.get(), ModBlocks.LOOT_CRATE_UNCOMMON.get(), ModBlocks.LOOT_CRATE_RARE.get()};
            for (int i = 0; i < lootBlocks.length; i++) {
                BlockPos pos = new BlockPos(2196 + i, -60, 3);
                level.setBlock(pos, lootBlocks[i].defaultBlockState(), 3);
            }
            for (int i = 0; i < lootBlocks.length; i++) {
                BlockPos pos = new BlockPos(2196 + i, -60, 6);
                level.setBlock(pos, lootBlocks[i].defaultBlockState(), 3);
                level.destroyBlock(pos, true);
            }
            for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(2196, -60, 6)).inflate(8, 3, 1))) {
                drops += item.getItem().getCount();
            }
            check("шесть разбитых урн и ящиков высыпали добычу (" + drops + " предметов)", drops >= 6);
        });
        // узел, разбитый игроком в выживании, рассыпается эфирными эссенциями
        cmd("gamemode survival");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = new BlockPos(2200, -58, 8);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, pos, level.random, false);
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            state.getBlock().playerWillDestroy(level, pos, state, player);
            level.removeBlock(pos, false);
            int essences = 0;
            for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2))) {
                if (item.getItem().is(ModItems.WISP_ESSENCE.get())) {
                    essences += item.getItem().getCount();
                }
            }
            check("разбитый узел рассыпался эфирными эссенциями (" + essences + ")", essences > 0);
        });
        cmd("gamemode creative");
        cmd("effect give @s minecraft:night_vision 60 0 true");
        server(10, (server, player) -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        cmd("tp @s 2198.5 -60 0.5 -20 5");
        shot(40, "worldgen_loot");
        cmd("tp @s 2202.5 -24 6.5 0 20");
        server(2, (server, player) -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        shot(40, "worldgen_mound");
        cmd("tp @s 2203.5 -40.4 22.5 90 10");
        shot(20, "worldgen_crypt");
        cmd("tp @s 2245.5 95 12.5 0 30");
        server(2, (server, player) -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        shot(40, "worldgen_hilltop");
        cmd("tp @s 2227.5 -57 0.5 0 5");
        server(2, (server, player) -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        shot(40, "worldgen_totem");
        purge(centre);
    }

    /** Деревни: башня мага и дом менялы из шаблонов, их жители, сделки и место построек в наборах деревенских домов. */
    private static void villages() {
        cmd("gamemode creative");
        cmd("time set noon");
        BlockPos centre = new BlockPos(2300, -60, 20);
        cmd("tp @s 2300.5 -60 10.5 0 10");
        client(40, mc -> {
        });
        purge(centre);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            String[] names = {"plains/wizard_tower", "plains/banker_home_1", "desert/wizard_tower", "desert/banker_home_2"};
            int placed = 0;
            for (int i = 0; i < names.length; i++) {
                var template = server.getStructureManager().get(Thaumcraft.id("village/" + names[i]));
                if (template.isPresent()) {
                    BlockPos pos = new BlockPos(2292 + i * 7, -61, 18);
                    template.get().placeInWorld(level, pos, pos, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                            level.random, 2);
                    placed++;
                }
            }
            int thaumaturges = 0;
            int moneychangers = 0;
            int offers = 0;
            for (net.minecraft.world.entity.npc.Villager villager : level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(30, 20, 20))) {
                var profession = villager.getVillagerData().getProfession();
                if (profession == thaumcraft.common.registry.ModVillagers.THAUMATURGE.get()) {
                    thaumaturges++;
                } else if (profession == thaumcraft.common.registry.ModVillagers.MONEYCHANGER.get()) {
                    moneychangers++;
                }
                offers += villager.getOffers().size();
            }
            boolean chest = level.getBlockEntity(new BlockPos(2294, -55, 20))
                    instanceof net.minecraft.world.level.block.entity.ChestBlockEntity;
            boolean bench = level.getBlockState(new BlockPos(2294, -60, 22)).is(ModBlocks.ARCANE_WORKBENCH.get());
            check("постройки встали из шаблонов (" + placed + " из 4), в них тауматурги (" + thaumaturges + ") и менялы (" + moneychangers
                    + ") с товаром (" + offers + " сделок), в башне сундук (" + chest + ") и магический верстак (" + bench + ")",
                    placed == 4 && thaumaturges == 2 && moneychangers == 2 && offers >= 8 && chest && bench);
            var pools = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
            int found = 0;
            for (String village : new String[]{"plains", "desert", "savanna", "taiga", "snowy"}) {
                var pool = pools.get(new net.minecraft.resources.ResourceLocation("minecraft", "village/" + village + "/houses"));
                if (pool != null && pool.getShuffledTemplates(level.random).stream().anyMatch(e -> e.toString().contains("thaumcraft:village"))) {
                    found++;
                }
            }
            check("башня мага и дом менялы есть в наборах домов всех пяти видов деревень (" + found + ")", found == 5);
        });
        server(2, (server, player) -> {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        cmd("tp @s 2304.5 -54 8.5 0 25");
        shot(40, "villages");
        purge(centre);
    }

    /** Пустотный металл, мантия, дробитель, резервуар, продвинутая печь, узел в банке, жемчужина, проверка рассудка. */
    private static void eldritchGear() {
        cmd("gamemode creative");
        cmd("time set noon");
        BlockPos centre = new BlockPos(2400, -60, 20);
        cmd("tp @s 2400.5 -60 10.5 0 10");
        client(40, mc -> {
        });
        purge(centre);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // пустотные вещи: искажение от снаряжения, самопочинка, истощение
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.VOID_ROBE_HELMET.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.VOID_ROBE_CHESTPLATE.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.VOID_ROBE_LEGGINGS.get()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.VOID_BOOTS.get()));
            ItemStack sword = new ItemStack(ModItems.VOID_SWORD.get());
            sword.setDamageValue(20);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            int warp = thaumcraft.common.research.WarpEvents.getWarpFromGear(player);
            check("пустотная мантия и снаряжение дают искажение (" + warp + ", ждём 2+2+2+1+1 = 8)", warp == 8);
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(2402.5, -60, 12.5);
            level.addFreshEntity(pig);
            sword.hurtEnemy(pig, player);
            check("пустотный меч ослабляет (слабость: " + pig.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) + ")",
                    pig.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS));
            float hurt = 10f;
            player.setHealth(20f);
            player.hurt(level.damageSources().magic(), hurt);
            check("пустотная мантия защищает от магии (из 10 урона дошло " + (20f - player.getHealth()) + ")", 20f - player.getHealth() < 6f);
            player.setHealth(20f);
            check("капюшон пустотной мантии показывает узлы и значки (" + thaumcraft.api.nodes.Revealer.wornBy(player) + ", "
                    + thaumcraft.api.Goggles.wornBy(player) + ")", thaumcraft.api.nodes.Revealer.wornBy(player) && thaumcraft.api.Goggles.wornBy(player));
            // первозданный дробитель: стена 3×3
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2405, -60, 20), new BlockPos(2407, -58, 20))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            }
        });
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ItemStack sword = player.getMainHandItem();
            check("пустотный меч сам чинится (повреждение " + sword.getDamageValue() + " из 20)", sword.getDamageValue() < 20);
            ItemStack crusher = new ItemStack(ModItems.PRIMAL_CRUSHER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, crusher);
        });
        cmd("tp @s 2406.5 -59.5 17.5 0 0");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.gameMode.destroyBlock(new BlockPos(2406, -59, 20));
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            int left = 0;
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2405, -60, 20), new BlockPos(2407, -58, 20))) {
                if (!level.getBlockState(pos).isAir()) {
                    left++;
                }
            }
            check("первозданный дробитель снёс стену 3×3 одним ударом (осталось " + left + " из 9)", left == 0);
        });
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // резервуар эссенции: тянет из банки через трубу, отдаёт матрице наполнения
            BlockPos reservoir = new BlockPos(2410, -60, 25);
            level.setBlock(reservoir, ModBlocks.ESSENTIA_RESERVOIR.get().defaultBlockState(), 3);
            if (level.getBlockEntity(reservoir) instanceof thaumcraft.common.blocks.EssentiaReservoirBlock.Reservoir r) {
                r.facing = Direction.UP;
                int accepted = r.addEssentia(thaumcraft.api.aspects.Aspect.MAGIC, 40, Direction.UP)
                        + r.addEssentia(thaumcraft.api.aspects.Aspect.FIRE, 30, Direction.UP);
                int refused = r.addEssentia(thaumcraft.api.aspects.Aspect.WATER, 10, Direction.NORTH);
                boolean taken = r.take(thaumcraft.api.aspects.Aspect.FIRE, 5);
                check("резервуар принимает эссенцию разных аспектов с подключённой грани (" + accepted + " из 70), с чужой — нет (" + refused
                        + "), и отдаёт её как источник (" + taken + ")", accepted == 70 && refused == 0 && taken);
            }
            // продвинутая алхимическая печь
            BlockPos core = new BlockPos(2415, -60, 25);
            for (int a = -1; a <= 1; a++) {
                for (int c = -1; c <= 1; c++) {
                    boolean corner = a != 0 && c != 0;
                    level.setBlock(core.offset(a, 0, c), (a == 0 && c == 0 ? ModBlocks.ALCHEMICAL_FURNACE : ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT).get()
                            .defaultBlockState(), 3);
                    if (a != 0 || c != 0) {
                        level.setBlock(core.offset(a, 1, c), (corner ? ModBlocks.ALEMBIC : ModBlocks.ALCHEMICAL_CONSTRUCT).get().defaultBlockState(), 3);
                    }
                }
            }
            var knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            knowledge.completeResearch("ADVALCHEMYFURNACE");
            knowledge.completeResearch("NODEJAR");
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.SILVERWOOD,
                    thaumcraft.api.wands.WandCap.VOID, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            wand.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(core), Direction.UP, core, false)));
            boolean formed = level.getBlockEntity(core) instanceof thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Core;
            check("жезл с пустотными набалдашниками собрал продвинутую алхимическую печь (" + formed + ")", formed);
            if (level.getBlockEntity(core) instanceof thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Core furnace) {
                furnace.heat = 500;
                furnace.power1 = 500;
                furnace.power2 = 500;
            }
            for (int i = 0; i < 4; i++) {
                net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, core.getX() + 0.5,
                        core.getY() + 1.2, core.getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 2));
                item.setDeltaMovement(0, 0, 0);
                level.addFreshEntity(item);
            }
            level.setBlock(core.north(2), ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            level.setBlock(core.north(2).below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            // узел в банке: стеклянный короб с плитами вокруг узла
            BlockPos node = new BlockPos(2421, -59, 26);
            for (BlockPos pos : BlockPos.betweenClosed(node.offset(-1, -1, -1), node.offset(1, 2, 1))) {
                level.setBlock(pos, pos.getY() == node.getY() + 2 ? net.minecraft.world.level.block.Blocks.OAK_SLAB.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3);
            }
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, node, level.random, false);
            wand.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(node.above(2)), Direction.UP, node.above(2), false)));
            boolean jarred = level.getBlockState(node).is(ModBlocks.NODE_JAR.get()) && level.getBlockState(node.above()).isAir()
                    && level.getBlockEntity(node) instanceof thaumcraft.common.blocks.NodeJarBlock.Jar jar && !jar.aspects.isEmpty();
            check("жезл запер узел в банку, стекло и плиты исчезли (" + jarred + ")", jarred);
        });
        cmd("tp @s 2417.5 -58 18.5 30 25");
        shot(60, "eldritch_furnace");
        server(100, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos reservoir = new BlockPos(2410, -60, 25);
            int stored = level.getBlockEntity(reservoir) instanceof thaumcraft.common.blocks.EssentiaReservoirBlock.Reservoir r ? r.essentia.visSize() : -1;
            check("резервуар хранит 65 единиц после выдачи (" + stored + ")", stored == 65);
            BlockPos core = new BlockPos(2415, -60, 25);
            int vis = level.getBlockEntity(core) instanceof thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock.Core furnace ? furnace.vis : -1;
            check("продвинутая печь растворила гнилую плоть в эссенцию (" + vis + ")", vis > 0);
            // узел из банки: разбить, поставить, выпустить
            BlockPos node = new BlockPos(2421, -59, 26);
            ItemStack drop = level.getBlockEntity(node) instanceof thaumcraft.common.blocks.NodeJarBlock.Jar jar ? jar.toItem() : ItemStack.EMPTY;
            level.removeBlock(node, false);
            BlockPos placed = new BlockPos(2421, -60, 30);
            player.setItemInHand(InteractionHand.MAIN_HAND, drop);
            drop.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(placed.below()), Direction.UP, placed.below(), false)));
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.SILVERWOOD,
                    thaumcraft.api.wands.WandCap.THAUMIUM, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            boolean jarPlaced = level.getBlockState(placed).is(ModBlocks.NODE_JAR.get());
            wand.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(placed), Direction.UP, placed, false)));
            boolean released = level.getBlockEntity(placed) instanceof thaumcraft.common.blockentity.NodeBlockEntity n && n.visSize() > 0;
            check("банку с узлом унесли и поставили в другом месте (" + jarPlaced + "), жезл выпустил узел (" + released + ")",
                    jarPlaced && released);
            // первозданная жемчужина о узел
            ItemStack pearl = new ItemStack(ModItems.PRIMORDIAL_PEARL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, pearl);
            int before = level.getBlockEntity(placed) instanceof thaumcraft.common.blockentity.NodeBlockEntity n ? n.getAspects().size() : 0;
            pearl.onItemUseFirst(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(placed), Direction.UP, placed, false)));
            int after = level.getBlockEntity(placed) instanceof thaumcraft.common.blockentity.NodeBlockEntity n ? n.getAspects().size() : 0;
            check("первозданная жемчужина перекроила узел (аспектов было " + before + ", стало " + after + ") и израсходовалась (" + pearl.isEmpty() + ")",
                    pearl.isEmpty() && after >= before);
            thaumcraft.common.research.PlayerKnowledge.get(player).addWarpSticky(30);
            thaumcraft.common.research.PlayerKnowledge.get(player).addWarpTemp(15);
            thaumcraft.common.network.SyncKnowledgePacket.send(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SANITY_CHECKER.get()));
        });
        cmd("tp @s 2410.5 -59 21.5 0 25");
        shot(30, "eldritch_sanity");
        server(2, (server, player) -> {
            var knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            knowledge.addWarpSticky(-30);
            knowledge.addWarpTemp(-15);
            thaumcraft.common.network.SyncKnowledgePacket.send(player);
            for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        });
        purge(centre);
    }

    private static void purge(BlockPos centre) {
        server(2, (server, player) -> player.serverLevel().getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(centre).inflate(48), e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(net.minecraft.world.entity.Entity::discard));
        client(3, mc -> {
        });
    }

    /** Всё, что нужно знать о големе, который не сделал своего дела: где он, куда идёт и чем занят. */
    private static String describe(thaumcraft.common.entity.golem.Golem golem) {
        var path = golem.getNavigation().getPath();
        var target = golem.getTarget();
        return "[голем " + golem.getGolemType().id() + " в " + golem.position().toString() + ", жив " + golem.isAlive() + ", здоровье "
                + golem.getHealth() + ", стоит " + golem.paused + ", скован " + golem.inactive + ", без ИИ " + golem.isNoAi() + ", на земле "
                + golem.onGround() + ", скорость " + golem.getSpeed() + ", цель " + (target == null ? "нет" : target.getType().toShortString()
                + " в " + target.blockPosition().toShortString() + " видна " + golem.getSensing().hasLineOfSight(target)) + ", путь "
                + (path == null ? "нет" : "к " + path.getTarget().toShortString() + " узел " + path.getNextNodeIndex() + "/" + path.getNodeCount()
                + (golem.getNavigation().isDone() ? " пройден" : " идёт")) + ", задачи "
                + golem.goalSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList() + ", поиск целей "
                + golem.targetSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList() + "]";
    }

    /** Големы: восемь материалов, ядра, улучшения и бой. */
    private static final net.minecraft.world.entity.LivingEntity[] dartTarget = new net.minecraft.world.entity.LivingEntity[1];

    /** Багровый культ: алтарь созывает клириков на обряд и рыцарей охраны, претор, обряды и багровый клинок. */
    private static void crimson() {
        cmd("gamemode creative");
        cmd("difficulty normal");
        cmd("time set noon");
        BlockPos altar = new BlockPos(2800, -60, 20);
        cmd("tp @s 2800.5 -60 10.5 0 10");
        client(40, mc -> {
        });
        purge(altar);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-12, 0, -12), altar.offset(12, 6, 12))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(altar, ModBlocks.ELDRITCH_ALTAR.get().defaultBlockState(), 3);
            thaumcraft.common.blocks.EldritchObeliskBlock.place(level, altar.above(2));
            if (level.getBlockEntity(altar) instanceof thaumcraft.common.blocks.EldritchAltarBlock.Altar a) {
                a.spawner = true;
                a.spawnType = 0;
                a.setChanged();
            }
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        // через шесть секунд (первый вызов алтаря — на 120-м тике) алтарь зовёт четырёх клириков на обряд
        server(140, (server, player) -> {
            var clerics = player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.CultistCleric.class, new net.minecraft.world.phys.AABB(altar).inflate(8));
            long ritual = clerics.stream().filter(thaumcraft.common.entity.CultistCleric::isRitualist).count();
            check("алтарь созвал клириков на обряд (" + clerics.size() + ", у алтаря " + ritual + ")", clerics.size() >= 3 && ritual >= 3);
            clerics.forEach(c -> c.addTag("autoshot_crimson"));
        });
        shot(30, "crimson_ritual");
        // пока жив хоть один клирик, алтарь зовёт рыцарей охраны
        server(130, (server, player) -> {
            var knights = player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.CultistKnight.class, new net.minecraft.world.phys.AABB(altar).inflate(16));
            check("алтарь призвал рыцарей охраны (" + knights.size() + ")", !knights.isEmpty());
            boolean armored = !knights.isEmpty() && knights.get(0).getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem()
                    instanceof thaumcraft.common.items.armor.CultistArmorItem;
            check("рыцарь в латах культа", armored);
        });
        shot(5, "crimson_knights");
        // претор — отдельно, без разума, чтобы его рассмотреть
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(altar).inflate(24),
                    e -> e instanceof thaumcraft.common.entity.Cultist).forEach(net.minecraft.world.entity.Entity::discard);
            if (level.getBlockEntity(altar) instanceof thaumcraft.common.blocks.EldritchAltarBlock.Altar a) {
                a.spawner = false;
                a.setChanged();
            }
            var leader = thaumcraft.common.registry.ModEntities.CULTIST_LEADER.get().create(level);
            leader.moveTo(2800.5, -60, 16.5, 180f, 0f);
            leader.finalizeSpawn(level, level.getCurrentDifficultyAt(leader.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
            leader.setNoAi(true);
            leader.setYHeadRot(180f);
            leader.yBodyRot = 180f;
            leader.addTag("autoshot_crimson");
            level.addFreshEntity(leader);
            player.connection.teleport(2800.5, -60, 11.5, 0f, 5f);
        });
        server(10, (server, player) -> {
            var leaders = player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.CultistLeader.class, new net.minecraft.world.phys.AABB(altar).inflate(16));
            check("претор культа появился чемпионом с титулом и багровым клинком (" + (leaders.isEmpty() ? "нет" : leaders.get(0).getName().getString()) + ")",
                    leaders.size() == 1 && leaders.get(0).hasCustomName() && thaumcraft.common.champion.Champions.get(leaders.get(0)) >= 0
                            && leaders.get(0).getMainHandItem().is(ModItems.CRIMSON_BLADE.get()));
        });
        shot(20, "crimson_leader");
        // багровые обряды открывают исследование, клинок ослабляет и морит голодом
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ItemStack rites = new ItemStack(ModItems.CRIMSON_RITES.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, rites);
            rites.use(level, player, InteractionHand.MAIN_HAND);
            check("багровые обряды открыли исследование CRIMSON", thaumcraft.common.research.PlayerKnowledge.get(player).isResearchComplete("CRIMSON"));
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(2803.5, -60, 12.5);
            level.addFreshEntity(pig);
            ItemStack blade = new ItemStack(ModItems.CRIMSON_BLADE.get());
            blade.hurtEnemy(pig, player);
            check("багровый клинок ослабляет и морит голодом (" + pig.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) + ", "
                    + pig.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER) + ")",
                    pig.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) && pig.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER));
            pig.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        server(2, (server, player) -> player.serverLevel().getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(altar).inflate(32), e -> e.getTags().contains("autoshot_crimson")
                        || e instanceof thaumcraft.common.entity.Cultist).forEach(net.minecraft.world.entity.Entity::discard));
        cmd("difficulty peaceful");
    }

    /** Запредельные земли: кольцо Элдрича, портал из алтаря, лабиринт, его обитатели, хранители и замок зала. */
    private static void outerLands() {
        cmd("gamemode creative");
        cmd("time set noon");
        cmd("effect give @s minecraft:night_vision 600 0 true");
        BlockPos centre = new BlockPos(2600, -61, 20);
        cmd("tp @s 2600.5 -60 8.5 0 15");
        client(40, mc -> {
        });
        purge(centre);
        int[] ring = new int[1];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-5, 1, -5), centre.offset(5, 10, 5))) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            boolean built = thaumcraft.common.world.EldritchRing.generate(level, level.random, centre, 11, 11);
            boolean altar = level.getBlockState(centre.above()).is(ModBlocks.ELDRITCH_ALTAR.get());
            boolean obelisk = level.getBlockState(centre.above(3)).is(ModBlocks.ELDRITCH_OBELISK.get());
            int caps = 0;
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-3, 1, -3), centre.offset(3, 1, 3))) {
                if (level.getBlockState(pos).is(ModBlocks.ELDRITCH_CAPSTONE.get())) {
                    caps++;
                }
            }
            ring[0] = caps;
            // кольцо строится только там, где ещё нет лабиринта; при повторном прогоне старое кольцо уже стоит
            check("кольцо Элдрича: алтарь " + altar + ", обелиск " + obelisk + ", колпаков " + caps + " (новое: " + built + ")",
                    altar && obelisk && caps == 8);
            check("под кольцом заложен лабиринт Запредельных земель", thaumcraft.common.world.outer.Labyrinth.get(server)
                    .mazesInRange(centre.getX() >> 4, centre.getZ() >> 4, 1, 1));
            if (level.getBlockEntity(centre.above()) instanceof thaumcraft.common.blocks.EldritchAltarBlock.Altar a) {
                a.spawner = false;
                a.eyes = 0;
                a.open = false;
                a.setChanged();
            }
        });
        shot(40, "outer_ring");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos altarPos = centre.above();
            // четыре ока в алтарь
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELDRITCH_EYE.get(), 4));
            for (int i = 0; i < 4; i++) {
                level.getBlockState(altarPos).use(level, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(altarPos), Direction.UP, altarPos, false));
            }
            int eyes = level.getBlockEntity(altarPos) instanceof thaumcraft.common.blocks.EldritchAltarBlock.Altar a ? a.eyes : -1;
            check("в алтарь вставлены четыре ока (" + eyes + ")", eyes == 4);
            // тёмный узел над алтарём и посох с висом
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, altarPos.above(), level.random, false);
            if (level.getBlockEntity(altarPos.above()) instanceof thaumcraft.common.blockentity.NodeBlockEntity node) {
                node.setNodeType(thaumcraft.api.nodes.NodeType.DARK);
            }
            thaumcraft.common.research.ResearchHelper.complete(player, "OCULUS");
            player.setItemInHand(InteractionHand.MAIN_HAND, thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.PRIMAL_STAFF, thaumcraft.api.wands.WandCap.THAUMIUM, true));
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(altarPos), Direction.UP, altarPos, false)));
            check("жезл раскрыл тёмный узел над алтарём порталом в Запредельные земли",
                    level.getBlockState(altarPos.above()).is(ModBlocks.ELDRITCH_PORTAL.get()));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        shot(60, "outer_portal");
        // шагнуть в портал
        server(5, (server, player) -> player.teleportTo(centre.getX() + 0.5, centre.getY() + 2, centre.getZ() + 0.5));
        server(60, (server, player) -> {
            boolean outer = thaumcraft.common.world.outer.OuterLands.is(player.level());
            check("портал перенёс игрока в Запредельные земли (" + player.level().dimension().location() + ")", outer);
            check("исследование ENTEROUTER открылось при входе", thaumcraft.common.research.PlayerKnowledge.get(player)
                    .isResearchComplete("ENTEROUTER"));
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int stone = 0;
            int portals = 0;
            for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-8, -4, -8), player.blockPosition().offset(8, 9, 8))) {
                var state = level.getBlockState(pos);
                if (state.is(ModBlocks.ANCIENT_STONE.get()) || state.is(ModBlocks.ANCIENT_STONE_SEALED.get()) || state.is(ModBlocks.ANCIENT_STONE_STAIRS.get())) {
                    stone++;
                }
                if (state.is(ModBlocks.ELDRITCH_PORTAL.get())) {
                    portals++;
                }
            }
            check("вокруг — зал-портал лабиринта из древнего камня (" + stone + " блоков, порталов " + portals + ")", stone > 50 && portals == 1);
            player.connection.teleport(player.getX(), player.getY() + 1, player.getZ() - 4, 0f, 20f);
        });
        shot(40, "outer_portal_room");
        server(5, (server, player) -> player.connection.teleport(player.getX(), player.getY() + 4, player.getZ() - 2, 180f, 35f));
        shot(40, "outer_portal_room_up");
        // обитатели и хранители — по очереди посреди зала-портала, перед игроком
        cmd("difficulty normal");
        BlockPos[] room = new BlockPos[1];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-16, -10, -16), player.blockPosition().offset(16, 10, 16))) {
                if (level.getBlockState(pos).is(ModBlocks.ELDRITCH_PORTAL.get())) {
                    room[0] = pos.offset(-8, -3, -8);
                    break;
                }
            }
            check("зал-портал найден (" + room[0] + ")", room[0] != null);
        });
        String[][] mobs = {{"eldritch_guardian", "страж Элдрича"}, {"eldritch_warden", "надзиратель Элдрича"}, {"eldritch_golem", "конструкт Элдрича"},
                {"cultist_portal", "багровый портал"}, {"taintacle_giant", "гигантское щупальце"}, {"inhabited_zombie", "одержимый зомби"},
                {"eldritch_crab", "краб Элдрича"}};
        for (String[] mob : mobs) {
            server(5, (server, player) -> {
                ServerLevel level = player.serverLevel();
                BlockPos r = room[0];
                boolean tall = mob[0].equals("taintacle_giant") || mob[0].equals("eldritch_warden") || mob[0].equals("eldritch_golem");
                // сбоку от колпака с порталом: крупные хранители не должны задевать блоки (иначе их душит, и они краснеют)
                player.connection.teleport(r.getX() + 11.5, r.getY() + 2, r.getZ() + 5.5, 90f, tall ? -12f : 12f);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(Thaumcraft.id(mob[0]));
                var entity = type.create(level);
                entity.moveTo(r.getX() + 6.0, r.getY() + 2, r.getZ() + 5.5, -90f, 0f);
                if (entity instanceof net.minecraft.world.entity.Mob m) {
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
                    m.setNoAi(true);
                    m.setYHeadRot(-90f);
                    m.yBodyRot = -90f;
                }
                entity.addTag("autoshot_outer");
                level.addFreshEntity(entity);
            });
            shot(mob[0].equals("eldritch_warden") || mob[0].equals("eldritch_golem") ? 170 : 40, "outer_" + mob[0]);
            server(2, (server, player) -> {
                var found = player.serverLevel().getEntities((net.minecraft.world.entity.Entity) null, player.getBoundingBox().inflate(16),
                        e -> e.getTags().contains("autoshot_outer"));
                check(mob[1] + " появился и жив (" + found.size() + ")", found.size() == 1 && found.get(0).isAlive());
                if (!found.isEmpty() && found.get(0) instanceof thaumcraft.common.entity.EldritchGolem golem) {
                    golem.hurt(player.serverLevel().damageSources().playerAttack(player), 1000f);
                    check("конструкт от смертельного удара лишился головы, но жив (" + golem.isHeadless() + ")", golem.isHeadless() && golem.isAlive());
                }
                if (!found.isEmpty() && found.get(0) instanceof thaumcraft.common.entity.ThaumcraftBoss boss) {
                    check("хранитель стал чемпионом и получил имя (" + boss.getName().getString() + ")",
                            thaumcraft.common.champion.Champions.get(boss) >= 0 && boss.hasCustomName());
                }
            });
            if (mob[0].equals("eldritch_golem")) {
                shot(120, "outer_eldritch_golem_headless");
            }
            server(2, (server, player) -> player.serverLevel().getEntities((net.minecraft.world.entity.Entity) null, player.getBoundingBox().inflate(32),
                    e -> e.getTags().contains("autoshot_outer")).forEach(net.minecraft.world.entity.Entity::discard));
        }
        // замок зала хранителя: скрижаль — и через пять секунд зал просыпается
        BlockPos[] lockPos = new BlockPos[1];
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // замок — в стороне от зала-портала, на каменной площадке над пустотой: зал хранителя перестраивает округу
            BlockPos lock = room[0].offset(56, 5, 4);
            lockPos[0] = lock;
            for (BlockPos pos : BlockPos.betweenClosed(lock.offset(-26, -4, -26), lock.offset(26, -4, 26))) {
                level.setBlock(pos, ModBlocks.ANCIENT_STONE.get().defaultBlockState(), 2);
            }
            for (BlockPos pos : BlockPos.betweenClosed(lock.offset(-2, -2, 0), lock.offset(2, 2, 0))) {
                level.setBlock(pos, ModBlocks.IMPASSABLE.get().defaultBlockState(), 3);
            }
            level.setBlock(lock, ModBlocks.ANCIENT_LOCK.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.AncientLockBlock.FACING, Direction.SOUTH), 3);
            player.connection.teleport(lock.getX() + 0.5, lock.getY() - 2, lock.getZ() + 7.5, 180f, -15f);
        });
        shot(30, "outer_lock");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos lock = lockPos[0];
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RUNED_TABLET.get()));
            level.getBlockState(lock).use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(lock), Direction.SOUTH, lock, false));
            boolean started = level.getBlockEntity(lock) instanceof thaumcraft.common.blocks.AncientLockBlock.Lock l && l.count >= 0;
            check("рунная скрижаль запустила замок", started);
        });
        shot(40, "outer_lock_opening");
        server(100, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos lock = lockPos[0];
            boolean gone = level.getBlockState(lock).isAir() && !level.getBlockState(lock.above()).is(ModBlocks.IMPASSABLE.get());
            var bosses = level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(lock).inflate(48),
                    e -> e instanceof thaumcraft.common.entity.ThaumcraftBoss || e instanceof thaumcraft.common.entity.CultistPortal
                            || e instanceof thaumcraft.common.entity.Taintacle);
            check("замок с полем исчезли (" + gone + "), в зале проснулся хранитель (" + bosses.size() + ")", gone && !bosses.isEmpty());
            bosses.forEach(e -> e.addTag("autoshot_outer"));
            if (!bosses.isEmpty()) {
                player.connection.teleport(bosses.get(0).getX(), bosses.get(0).getY() + 3, bosses.get(0).getZ() + 8, 180f, 20f);
            }
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        shot(40, "outer_boss_room");
        server(2, (server, player) -> player.serverLevel().getEntities((net.minecraft.world.entity.Entity) null, player.getBoundingBox().inflate(64),
                e -> e.getTags().contains("autoshot_outer") || e instanceof thaumcraft.common.entity.Cultist).forEach(net.minecraft.world.entity.Entity::discard));
        cmd("difficulty peaceful");
        // домой через тот же портал
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos portal = null;
            for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-40, -10, -40), player.blockPosition().offset(40, 10, 40))) {
                if (level.getBlockState(pos).is(ModBlocks.ELDRITCH_PORTAL.get())) {
                    portal = pos.immutable();
                    break;
                }
            }
            if (room[0] != null) {
                portal = room[0].offset(8, 3, 8);
            }
            if (portal != null) {
                player.setPortalCooldown(0);
                player.teleportTo(portal.getX() + 0.5, portal.getY(), portal.getZ() + 0.5);
            }
        });
        server(60, (server, player) -> {
            check("портал Запредельных земель вернул игрока домой, к алтарю (" + player.level().dimension().location() + ", "
                    + player.blockPosition() + ")", player.level().dimension() == Level.OVERWORLD && player.blockPosition().distManhattan(centre) < 8);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        cmd("tp @s 2600.5 -58 13.5 0 20");
        shot(40, "outer_back_home");
    }

    private static void golems() {
        cmd("gamemode creative");
        cmd("time set noon");
        cmd("difficulty normal");
        BlockPos centre = new BlockPos(1000, -60, 8);
        cmd("tp @s 1000.5 -59 -1.5 0 10");
        client(40, mc -> {
        });
        purge(centre);
        thaumcraft.common.entity.golem.Golem[] row = new thaumcraft.common.entity.golem.Golem[8];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"crucible/golem_straw", "crucible/golem_wood", "crucible/golem_tallow", "crucible/golem_clay",
                    "crucible/golem_flesh", "crucible/golem_stone", "crucible/golem_iron", "crucible/golem_thaumium", "golem_core_blank",
                    "crucible/golem_core_gather", "crucible/golem_core_fill", "crucible/golem_core_empty", "crucible/golem_core_harvest",
                    "crucible/golem_core_guard", "crucible/golem_core_butcher", "crucible/golem_core_liquid", "infusion/golem_core_alchemy",
                    "infusion/golem_core_sorting", "infusion/golem_core_fishing", "infusion/golem_core_use", "golem_upgrade_air",
                    "golem_upgrade_earth", "golem_upgrade_fire", "golem_upgrade_water", "golem_upgrade_order", "golem_upgrade_entropy",
                    "tallow_block", "tallow_from_block", "crucible/sanity_soap"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты големов, ядер, улучшений и блока сала загружены (нет:" + missing + ")", missing.length() == 0);

            StringBuilder stats = new StringBuilder();
            boolean right = true;
            for (int i = 0; i < 8; i++) {
                var type = thaumcraft.common.entity.golem.GolemType.values()[i];
                ItemStack stack = new ItemStack(ModItems.GOLEMS.get(i).get());
                var item = (thaumcraft.common.items.golem.GolemPlacerItem) stack.getItem();
                var golem = item.spawn(level, 993.5 + i * 2, -60, 8.5, net.minecraft.core.Direction.UP, stack, player);
                golem.setYRot(180f);
                golem.yBodyRot = 180f;
                golem.yHeadRot = 180f;
                row[i] = golem;
                stats.append(' ').append(type.id()).append('=').append((int) golem.getMaxHealth()).append('/').append(golem.getArmorValue())
                        .append('/').append(golem.getUpgradeSlots());
                right &= golem.getMaxHealth() == type.health && golem.getArmorValue() == type.armor && golem.getUpgradeSlots() == type.upgrades
                        && golem.getCore() == -1 && player.getUUID().equals(golem.getOwnerUUID())
                        && golem.getHome().equals(new BlockPos(993 + i * 2, -60, 8));
            }
            check("восемь големов из предметов: здоровье, броня и число улучшений по материалу (" + stats + "), без ядра, с хозяином и домом",
                    right);
        });
        shot(30, "golems_row");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // тауминиевый голем: ядро стража и улучшения
            var golem = row[7];
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_CORES.get(thaumcraft.common.items.golem.GolemCoreItem.GUARD).get()));
            var cored = golem.interact(player, InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_CORES.get(0).get()));
            var second = golem.interact(player, InteractionHand.MAIN_HAND);
            // второе ядро не вставилось, а открыло окно голема: закрываем его
            boolean window = player.containerMenu instanceof thaumcraft.common.menu.GolemMenu;
            player.closeContainer();
            int placed = 0;
            for (int i = 0; i < 3; i++) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_UPGRADES.get(thaumcraft.common.entity.golem.Golem.UPGRADE_EARTH).get()));
                if (golem.interact(player, InteractionHand.MAIN_HAND).consumesAction()) {
                    placed++;
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            double damage = golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            check("ядро вставляется один раз (" + cored + ", второе " + second + " открыло окно " + window + ", ядро " + golem.getCore() + "), улучшений земли входит два из трёх ("
                            + placed + "), урон тауминиевого стража 2+6+2 (" + damage + "), ноша 32+16*2 (" + golem.getCarryLimit() + ")",
                    cored.consumesAction() && window && golem.getCore() == thaumcraft.common.items.golem.GolemCoreItem.GUARD
                            && placed == 2 && damage == 10.0 && golem.getCarryLimit() == 64);

            // сохранение и загрузка
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            golem.addAdditionalSaveData(saved);
            var copy = thaumcraft.common.registry.ModEntities.GOLEM.get().create(level);
            copy.readAdditionalSaveData(saved);
            check("голем переживает сохранение: материал " + copy.getGolemType() + ", ядро " + copy.getCore() + ", улучшений земли "
                            + copy.getUpgradeAmount(1) + ", дом " + copy.getHome().toShortString(),
                    copy.getGolemType() == thaumcraft.common.entity.golem.GolemType.THAUMIUM && copy.getCore() == 4 && copy.getUpgradeAmount(1) == 2
                            && copy.getHome().equals(golem.getHome()) && player.getUUID().equals(copy.getOwnerUUID()));
            copy.discard();

            // огонь: глиняный не горит, соломенный горит
            row[3].setSecondsOnFire(5);
            row[0].setSecondsOnFire(5);
            boolean clayHurt = row[3].hurt(level.damageSources().inFire(), 2f);
            check("глиняный голем не боится огня (горит " + row[3].isOnFire() + ", урон прошёл " + clayHurt + "), соломенный горит ("
                    + row[0].isOnFire() + ")", !row[3].isOnFire() && !clayHurt && row[0].isOnFire());
            row[0].clearFire();
            golem.hurt(level.damageSources().generic(), 12f);
            golem.setNoAi(true);
            golem.setYRot(180f);
            golem.yBodyRot = 180f;
            golem.yHeadRot = 180f;
        });
        // тауминиевый страж спереди и со спины: ядро, улучшения и трещины
        cmd("tp @s 1007.5 -60 6.7 0 10");
        shot(30, "golems_front");
        cmd("tp @s 1007.5 -60 10.3 180 10");
        shot(15, "golems_back");
        cmd("tp @s 1004.5 -59 3.5 -40 10");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var golem = row[7];
            golem.setNoAi(false);
            // зомби перед стражем
            var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            zombie.moveTo(golem.getX(), -60, golem.getZ() - 4, 0f, 0f);
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
            level.addFreshEntity(zombie);
        });
        shot(30, "golems_guard");
        server(120, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int zombies = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(24)).size();
            StringBuilder left = new StringBuilder();
            for (var zombie : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(24))) {
                left.append(' ').append(zombie.getType().toShortString()).append(" в ").append(zombie.blockPosition().toShortString())
                        .append(" здоровье ").append(zombie.getHealth());
            }
            check("голем со стражьим ядром убил зомби у своего дома (осталось " + zombies + left + "), а големы без ядра стоят на месте ("
                            + row[1].blockPosition().toShortString() + ") " + (zombies == 0 ? "" : describe(row[7])),
                    zombies == 0 && row[7].isAlive() && row[1].blockPosition().equals(new BlockPos(995, -60, 8)));
        });
        shot(40, "golems_after");

        // колокольчик: привязка, отметки, снятие голема в предмет и возвращение
        cmd("setblock 1003 -60 12 minecraft:chest");
        cmd("tp @s 1001.5 -59 6.5 -20 15");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var golem = row[2];
            BlockPos chest = new BlockPos(1003, -60, 12);
            ItemStack bell = new ItemStack(ModItems.GOLEM_BELL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bell);
            boolean recipe = level.getRecipeManager().byKey(Thaumcraft.id("golem_bell")).isPresent();
            bell.getItem().interactLivingEntity(bell, player, golem, InteractionHand.MAIN_HAND);
            bell = player.getMainHandItem();
            boolean linked = golem.getUUID().equals(thaumcraft.common.items.golem.GolemBellItem.getGolemId(bell))
                    && golem.getHome().equals(thaumcraft.common.items.golem.GolemBellItem.getGolemHome(bell));
            thaumcraft.common.items.golem.GolemBellItem.changeMarkers(bell, player, level, chest, net.minecraft.core.Direction.UP);
            thaumcraft.common.items.golem.GolemBellItem.changeMarkers(bell, player, level, chest, net.minecraft.core.Direction.NORTH);
            int two = golem.getMarkers().size();
            thaumcraft.common.items.golem.GolemBellItem.changeMarkers(bell, player, level, chest, net.minecraft.core.Direction.NORTH);
            int one = golem.getMarkers().size();
            // без улучшения порядка цвет не меняется; с ним повторный щелчок красит отметку
            golem.setUpgrade(0, (byte) thaumcraft.common.entity.golem.Golem.UPGRADE_ORDER);
            thaumcraft.common.items.golem.GolemBellItem.changeMarkers(bell, player, level, chest, net.minecraft.core.Direction.UP);
            int colour = golem.getMarkers().isEmpty() ? -99 : golem.getMarkers().get(0).color();
            thaumcraft.common.items.golem.GolemBellItem.changeMarkers(bell, player, level, chest.above(3), net.minecraft.core.Direction.DOWN);
            check("колокольчик: рецепт есть (" + recipe + "), привязался к голему (" + linked + "), отметки ставятся и снимаются (" + two + " -> "
                            + one + "), с улучшением порядка повторный щелчок красит отметку (цвет " + colour + "), у голема их "
                            + golem.getMarkers().size() + ", на колокольчике " + thaumcraft.common.items.golem.GolemBellItem.getMarkers(bell).size(),
                    recipe && linked && two == 2 && one == 1 && colour == 0 && golem.getMarkers().size() == 2
                            && thaumcraft.common.items.golem.GolemBellItem.getMarkers(bell).size() == 2);
        });
        shot(30, "golems_markers");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // снимаем тауминиевого стража и ставим его обратно
            var golem = row[7];
            ItemStack item = thaumcraft.common.items.golem.GolemBellItem.pickUp(golem, false);
            var tag = item.getTag();
            boolean packed = tag != null && tag.getByte("core") == 4 && tag.getByteArray("upgrades").length == 2
                    && tag.getByteArray("upgrades")[0] == 1 && !golem.isAlive();
            var again = ((thaumcraft.common.items.golem.GolemPlacerItem) item.getItem()).spawn(level, 1007.5, -60, 8.5,
                    net.minecraft.core.Direction.UP, item, player);
            boolean restored = again.getCore() == 4 && again.getUpgradeAmount(1) == 2
                    && again.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 10.0;
            // разборка: ядро выпадает отдельно, голем остаётся пустым
            ItemStack bare = thaumcraft.common.items.golem.GolemBellItem.pickUp(again, true);
            int cores = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(centre).inflate(16),
                    e -> e.getItem().getItem() instanceof thaumcraft.common.items.golem.GolemCoreItem).size();
            check("голем снимается в предмет с ядром и улучшениями (" + packed + "), ставится обратно таким же (" + restored
                            + "), а при разборке ядро выпадает отдельно (ядер на земле " + cores + ", у предмета данных "
                            + (bare.getTag() == null ? "нет" : bare.getTag().toString()) + ")",
                    packed && restored && cores == 1 && bare.getTag() == null);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        cmd("setblock 1003 -60 12 minecraft:air");
        purge(centre);

        // носильщики: сбор, опустошение и наполнение
        cmd("setblock 1000 -60 20 minecraft:chest");
        cmd("setblock 1010 -60 20 minecraft:chest");
        cmd("setblock 1016 -60 20 minecraft:chest");
        cmd("setblock 1010 -60 30 minecraft:chest");
        cmd("setblock 1016 -60 30 minecraft:chest");
        cmd("tp @s 1008.5 -53 8.5 0 30");
        thaumcraft.common.entity.golem.Golem[] workers = new thaumcraft.common.entity.golem.Golem[3];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            String dim = level.dimension().location().toString();
            int[] types = {1, 3, 5};
            int[] cores = {thaumcraft.common.items.golem.GolemCoreItem.GATHER, thaumcraft.common.items.golem.GolemCoreItem.EMPTY,
                    thaumcraft.common.items.golem.GolemCoreItem.FILL};
            BlockPos[] homes = {new BlockPos(1000, -60, 20), new BlockPos(1010, -60, 20), new BlockPos(1010, -60, 30)};
            for (int i = 0; i < 3; i++) {
                ItemStack stack = new ItemStack(ModItems.GOLEMS.get(types[i]).get());
                stack.getOrCreateTag().putByte("core", (byte) cores[i]);
                BlockPos at = homes[i].north();
                workers[i] = ((thaumcraft.common.items.golem.GolemPlacerItem) stack.getItem()).spawn(level, at.getX() + 0.5, at.getY(),
                        at.getZ() + 0.5, net.minecraft.core.Direction.NORTH, stack, player);
            }
            // сборщик: вокруг разбросан булыжник
            for (int i = 0; i < 5; i++) {
                var item = new net.minecraft.world.entity.item.ItemEntity(level, 996.5 + i * 2, -60, 15.5,
                        new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 2));
                item.setNoPickUpDelay();
                level.addFreshEntity(item);
            }
            // опустошитель: из домашнего сундука в отмеченный
            ((net.minecraft.world.Container) level.getBlockEntity(homes[1])).setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 16));
            workers[1].setMarkers(java.util.List.of(new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1016, -60, 20), dim, (byte) 1, (byte) -1)));
            // наполнитель: держать дома ровно двенадцать золотых слитков, беря их из отмеченного сундука
            ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1016, -60, 30))).setItem(0,
                    new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 32));
            workers[2].setMarkers(java.util.List.of(new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1016, -60, 30), dim, (byte) 1, (byte) -1)));
            var menu = new thaumcraft.common.menu.GolemMenu(0, player.getInventory(), workers[2]);
            menu.setCarried(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 12));
            menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            boolean kept = menu.getCarried().getCount() == 12;
            menu.setCarried(ItemStack.EMPTY);
            boolean paused = workers[2].paused;
            menu.removed(player);
            check("окно голема: образец запоминается, не отнимая предмет у игрока (" + kept + ", образец " + workers[2].inventory.getItem(0)
                            + "), ячеек у наполнителя " + menu.ghostSlots + ", пока окно открыто, голем стоит (" + paused + " -> " + workers[2].paused + ")",
                    kept && workers[2].inventory.getItem(0).getCount() == 12 && menu.ghostSlots == 6 && paused && !workers[2].paused);
        });
        shot(100, "golems_carry");
        cmd("tp @s 1012.5 -60 26.5 30 10");
        server(5, (server, player) -> net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new thaumcraft.common.menu.GolemMenu(id, inventory, workers[2]), workers[2].getDisplayName()),
                buf -> buf.writeVarInt(workers[2].getId())));
        shot(15, "golems_gui");
        close();
        cmd("tp @s 1008.5 -53 8.5 0 30");
        server(650, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int cobble = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1000, -60, 20))).countItem(net.minecraft.world.item.Items.COBBLESTONE);
            int left = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1010, -60, 20))).countItem(net.minecraft.world.item.Items.IRON_INGOT);
            int moved = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1016, -60, 20))).countItem(net.minecraft.world.item.Items.IRON_INGOT);
            int gold = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1010, -60, 30))).countItem(net.minecraft.world.item.Items.GOLD_INGOT);
            int source = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1016, -60, 30))).countItem(net.minecraft.world.item.Items.GOLD_INGOT);
            int carrying = workers[0].getCarried().getCount();
            int lying = 0;
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(1000, -60, 18)).inflate(20))) {
                if (item.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)) {
                    lying += item.getItem().getCount();
                }
            }
            check("голем с ядром сбора подобрал разбросанный булыжник и носит его в домашний сундук (в сундуке " + cobble + ", в руках "
                    + carrying + " из 10; на земле " + lying + ", голем жив " + workers[0].isAlive() + " в " + workers[0].blockPosition().toShortString()
                    + ", идёт " + !workers[0].getNavigation().isDone() + ", стоит " + workers[0].paused + ", без ИИ " + workers[0].isNoAi()
                    + ", на земле " + workers[0].onGround() + ", скорость " + workers[0].getSpeed() + ", движение " + workers[0].getDeltaMovement()
                    + ", цель пути " + (workers[0].getNavigation().getPath() == null ? "нет" : workers[0].getNavigation().getPath().getTarget().toShortString()
                    + " узел " + workers[0].getNavigation().getPath().getNextNodeIndex() + "/" + workers[0].getNavigation().getPath().getNodeCount())
                    + ", задачи " + workers[0].goalSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList() + ")", cobble >= 6 && cobble + carrying == 10);
            check("голем с ядром опустошения перенёс слитки из домашнего сундука в отмеченный (дома " + left + ", в отмеченном " + moved + ")",
                    left == 0 && moved == 16);
            check("голем с ядром наполнения принёс домой ровно столько золота, сколько велено (дома " + gold + " из 12, в источнике осталось "
                    + source + ", в руках " + workers[2].getCarried() + ")", gold == 12 && source + workers[2].getCarried().getCount() == 20);
        });
        shot(5, "golems_carry_done");
        cmd("fill 996 -60 18 1018 -60 32 minecraft:air");
        purge(centre.offset(8, 0, 16));

        // работники: урожай, рубка, рыбалка и ядро использования
        cmd("fill 999 -61 39 1001 -61 41 minecraft:farmland[moisture=7]");
        cmd("fill 999 -60 39 1001 -60 41 minecraft:wheat[age=7]");
        cmd("fill 1012 -60 40 1012 -57 40 minecraft:oak_log");
        cmd("fill 1011 -56 39 1013 -56 41 minecraft:oak_leaves");
        cmd("fill 1020 -64 38 1028 -61 46 minecraft:stone");
        cmd("fill 1021 -63 39 1027 -61 45 minecraft:water");
        cmd("setblock 1036 -60 37 minecraft:chest");
        cmd("setblock 1036 -60 41 minecraft:composter");
        cmd("tp @s 1018.5 -52 28.5 0 40");
        thaumcraft.common.entity.golem.Golem[] hands = new thaumcraft.common.entity.golem.Golem[4];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            String dim = level.dimension().location().toString();
            int[] types = {5, 6, 7, 3};
            int[] cores = {thaumcraft.common.items.golem.GolemCoreItem.HARVEST, thaumcraft.common.items.golem.GolemCoreItem.LUMBER,
                    thaumcraft.common.items.golem.GolemCoreItem.FISHING, thaumcraft.common.items.golem.GolemCoreItem.USE};
            BlockPos[] spots = {new BlockPos(1000, -60, 37), new BlockPos(1012, -60, 37), new BlockPos(1024, -60, 37), new BlockPos(1036, -60, 38)};
            for (int i = 0; i < 4; i++) {
                ItemStack stack = new ItemStack(ModItems.GOLEMS.get(types[i]).get());
                stack.getOrCreateTag().putByte("core", (byte) cores[i]);
                if (i == 0) {
                    stack.getTag().putByteArray("upgrades", new byte[]{(byte) thaumcraft.common.entity.golem.Golem.UPGRADE_ORDER});
                }
                // последний голем стоит у сундука: его дом — сам сундук
                hands[i] = ((thaumcraft.common.items.golem.GolemPlacerItem) stack.getItem()).spawn(level, spots[i].getX() + 0.5, spots[i].getY(),
                        spots[i].getZ() + 0.5, i == 3 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.UP, stack, player);
            }
            ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1036, -60, 37))).setItem(0,
                    new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 16));
            hands[3].setMarkers(java.util.List.of(new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1036, -60, 41), dim, (byte) 1, (byte) -1)));
            // улов: сто забросов подряд должны дать в основном рыбу
            var goal = new thaumcraft.common.entity.golem.GolemFishGoal(hands[2]);
            int fish = 0;
            int other = 0;
            for (int i = 0; i < 100; i++) {
                ItemStack caught = goal.catchAt(new BlockPos(1024, -61, 42));
                if (caught.is(net.minecraft.tags.ItemTags.FISHES)) {
                    fish++;
                } else if (!caught.isEmpty()) {
                    other++;
                }
            }
            check("улов голема-рыбака берётся из таблиц рыбалки: из ста забросов рыбы " + fish + ", прочего " + other, fish >= 70 && fish + other == 100);
        });
        shot(60, "golems_work");
        int[] casts = new int[1];
        for (int wait = 0; wait < 12; wait++) {
            server(20, (server, player) -> {
                if (!player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.golem.GolemBobber.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(1024, -60, 40)).inflate(8)).isEmpty()) {
                    casts[0]++;
                }
            });
        }
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var bobbers = level.getEntitiesOfClass(thaumcraft.common.entity.golem.GolemBobber.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(1024, -60, 40)).inflate(8));
            boolean floating = !bobbers.isEmpty() && level.getFluidState(bobbers.get(0).blockPosition()).is(net.minecraft.tags.FluidTags.WATER)
                    || !bobbers.isEmpty() && level.getFluidState(bobbers.get(0).blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER);
            check("голем-рыбак забросил поплавок, и тот держится на воде (поплавков " + bobbers.size() + (bobbers.isEmpty() ? ""
                    : ", место " + bobbers.get(0).blockPosition().toShortString()) + ", за двенадцать осмотров раз в секунду был виден " + casts[0]
                    + " раз из 12)", casts[0] >= 4 && (bobbers.isEmpty() || floating));
        });
        cmd("tp @s 1024.5 -59 34.5 0 20");
        shot(20, "golems_fishing");
        cmd("tp @s 1018.5 -52 28.5 0 40");
        server(450, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int ripe = 0;
            int young = 0;
            for (BlockPos pos : BlockPos.betweenClosed(999, -60, 39, 1001, -60, 41)) {
                var state = level.getBlockState(pos);
                if (state.is(net.minecraft.world.level.block.Blocks.WHEAT)) {
                    if (state.getValue(net.minecraft.world.level.block.CropBlock.AGE) == 7) {
                        ripe++;
                    } else {
                        young++;
                    }
                }
            }
            int wheat = 0;
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(1000, -60, 40)).inflate(8))) {
                if (item.getItem().is(net.minecraft.world.item.Items.WHEAT)) {
                    wheat += item.getItem().getCount();
                }
            }
            check("голем с ядром урожая собрал поле и, имея улучшение порядка, засеял его снова (спелых осталось " + ripe + ", молодых " + young
                    + ", пшеницы на земле " + wheat + ")", ripe == 0 && young == 9 && wheat == 9);
            int logs = 0;
            for (BlockPos pos : BlockPos.betweenClosed(1012, -60, 40, 1012, -57, 40)) {
                if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS)) {
                    logs++;
                }
            }
            check("голем с ядром рубки срубил дерево целиком (брёвен осталось " + logs + " из 4)", logs == 0);
            int seeds = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1036, -60, 37))).countItem(net.minecraft.world.item.Items.WHEAT_SEEDS)
                    + hands[3].getCarried().getCount();
            int compost = level.getBlockState(new BlockPos(1036, -60, 41)).getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL);
            check("голем с ядром использования берёт семена из домашнего сундука и щёлкает ими по отмеченному компостеру (семян осталось "
                    + seeds + " из 16, компостер заполнен на " + compost + ")", seeds < 16 && compost > 0);
        });
        shot(5, "golems_work_done");
        cmd("fill 1020 -64 38 1028 -62 46 minecraft:dirt");
        cmd("fill 996 -61 36 1040 -61 46 minecraft:grass_block");
        cmd("fill 996 -60 36 1040 -55 44 minecraft:air");
        purge(new BlockPos(1018, -60, 40));
        cmd("kill @e[type=item,distance=..60]");

        // сортировщик и водонос
        cmd("setblock 1000 -60 60 minecraft:chest");
        cmd("setblock 996 -60 64 minecraft:chest");
        cmd("setblock 1004 -60 64 minecraft:chest");
        cmd("setblock 1016 -60 60 thaumcraft:arcane_spa");
        cmd("fill 1015 -62 65 1017 -61 65 minecraft:stone");
        cmd("fill 1015 -61 65 1016 -61 65 minecraft:water");
        cmd("tp @s 1008.5 -54 52.5 0 40");
        thaumcraft.common.entity.golem.Golem[] pair = new thaumcraft.common.entity.golem.Golem[2];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            String dim = level.dimension().location().toString();
            ItemStack sorter = new ItemStack(ModItems.GOLEMS.get(5).get());
            sorter.getOrCreateTag().putByte("core", (byte) thaumcraft.common.items.golem.GolemCoreItem.SORTING);
            pair[0] = ((thaumcraft.common.items.golem.GolemPlacerItem) sorter.getItem()).spawn(level, 1000.5, -60, 61.5,
                    net.minecraft.core.Direction.SOUTH, sorter, player);
            var home = (net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1000, -60, 60));
            home.setItem(0, new ItemStack(net.minecraft.world.item.Items.REDSTONE, 8));
            home.setItem(1, new ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI, 8));
            home.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIRT, 4));
            ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(996, -60, 64))).setItem(0, new ItemStack(net.minecraft.world.item.Items.REDSTONE));
            ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1004, -60, 64))).setItem(0, new ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI));
            pair[0].setMarkers(java.util.List.of(
                    new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(996, -60, 64), dim, (byte) 1, (byte) -1),
                    new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1004, -60, 64), dim, (byte) 1, (byte) -1)));

            ItemStack carrier = new ItemStack(ModItems.GOLEMS.get(6).get());
            carrier.getOrCreateTag().putByte("core", (byte) thaumcraft.common.items.golem.GolemCoreItem.LIQUID);
            pair[1] = ((thaumcraft.common.items.golem.GolemPlacerItem) carrier.getItem()).spawn(level, 1016.5, -60, 61.5,
                    net.minecraft.core.Direction.SOUTH, carrier, player);
            pair[1].setMarkers(java.util.List.of(
                    new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1015, -61, 65), dim, (byte) 1, (byte) -1),
                    new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1016, -61, 65), dim, (byte) 1, (byte) -1)));
        });
        cmd("tp @s 1013.5 -59 63.5 -100 15");
        shot(45, "golems_bucket");
        cmd("tp @s 1000.5 -58 66.5 180 20");
        shot(25, "golems_sorting");
        cmd("tp @s 1008.5 -54 52.5 0 40");
        server(330, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var home = (net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1000, -60, 60));
            int redstone = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(996, -60, 64))).countItem(net.minecraft.world.item.Items.REDSTONE);
            int lapis = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1004, -60, 64))).countItem(net.minecraft.world.item.Items.LAPIS_LAZULI);
            int strayed = ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(996, -60, 64))).countItem(net.minecraft.world.item.Items.LAPIS_LAZULI)
                    + ((net.minecraft.world.Container) level.getBlockEntity(new BlockPos(1004, -60, 64))).countItem(net.minecraft.world.item.Items.REDSTONE);
            int dirt = home.countItem(net.minecraft.world.item.Items.DIRT);
            check("голем с ядром сортировки разложил предметы по сундукам, где такие уже лежат (редстоуна " + redstone + " из 9, лазурита "
                            + lapis + " из 9, перепутано " + strayed + "), а то, чему места нет, оставил дома (земли " + dirt + " из 4)",
                    redstone == 9 && lapis == 9 && strayed == 0 && dirt == 4);
            var spa = (thaumcraft.common.blocks.ArcaneSpaBlock.Spa) level.getBlockEntity(new BlockPos(1016, -60, 60));
            int water = spa.tank.getFluidAmount() + pair[1].fluidCarried.getAmount();
            boolean drained = level.getFluidState(new BlockPos(1015, -61, 65)).isEmpty() && level.getFluidState(new BlockPos(1016, -61, 65)).isEmpty();
            check("голем с ядром переливания вычерпал отмеченные источники воды и носит её в домашний бак (в баке " + spa.tank.getFluidAmount()
                    + ", в ведре " + pair[1].fluidCarried.getAmount() + ", источники вычерпаны " + drained + "; голем в "
                    + pair[1].blockPosition().toShortString() + ", идёт " + !pair[1].getNavigation().isDone() + ", ищет "
                    + (pair[1].fluidWatched != null) + ") " + (water == 2000 && drained ? "" : describe(pair[1])), water == 2000 && drained
                    && spa.tank.getFluidAmount() >= 1000);
        });
        // голодный сундук
        cmd("setblock 1046 -60 60 thaumcraft:hungry_chest[facing=north]");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HUNGRY_CHEST.get()));
            var apples = new net.minecraft.world.entity.item.ItemEntity(level, 1046.5, -58.5, 60.5, new ItemStack(net.minecraft.world.item.Items.APPLE, 5));
            apples.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(apples);
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var chest = (thaumcraft.common.blocks.HungryChestBlock.Chest) level.getBlockEntity(new BlockPos(1046, -60, 60));
            int eaten = chest.countItem(net.minecraft.world.item.Items.APPLE);
            int lying = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(1046, -60, 60)).inflate(3)).size();
            boolean recipe = level.getRecipeManager().byKey(Thaumcraft.id("hungry_chest")).isPresent();
            check("голодный сундук: рецепт есть (" + recipe + "), упавшие на него яблоки проглочены (в сундуке " + eaten
                    + " из 5, на земле предметов " + lying + ")", recipe && eaten == 5 && lying == 0);
            player.openMenu(chest);
        });
        cmd("tp @s 1046.5 -59.5 57.5 0 25");
        shot(12, "golems_hungry_chest");
        close();
        server(2, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        cmd("setblock 1046 -60 60 minecraft:air");
        cmd("kill @e[type=item,distance=..20]");

        // дорожный сундук
        cmd("tp @s 1060.5 -60 60.5 0 10");
        thaumcraft.common.entity.golem.TravelingTrunk[] trunks = new thaumcraft.common.entity.golem.TravelingTrunk[2];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean recipe = level.getRecipeManager().byKey(Thaumcraft.id("infusion/traveling_trunk")).isPresent();
            ItemStack item = new ItemStack(ModItems.TRAVELING_TRUNK.get());
            var trunk = thaumcraft.common.items.golem.TrunkSpawnerItem.spawn(level, 1060.5, -60, 64.5, item, player);
            trunks[0] = trunk;
            // улучшение земли даёт четвёртый ряд, второе улучшение не вставляется
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_UPGRADES.get(thaumcraft.common.entity.golem.Golem.UPGRADE_EARTH).get()));
            trunk.interact(player, InteractionHand.MAIN_HAND);
            int rows = trunk.getRows();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_UPGRADES.get(thaumcraft.common.entity.golem.Golem.UPGRADE_FIRE).get()));
            trunk.interact(player, InteractionHand.MAIN_HAND);
            if (player.containerMenu instanceof thaumcraft.common.menu.TrunkMenu) {
                player.closeContainer();
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemStack left = trunk.insert(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 40));
            var menu = new thaumcraft.common.menu.TrunkMenu(0, player.getInventory(), trunk);
            boolean open = trunk.isOpen();
            menu.clickMenuButton(player, 1);
            boolean stay = trunk.getStay();
            menu.clickMenuButton(player, 1);
            menu.removed(player);
            check("дорожный сундук: рецепт есть (" + recipe + "), хозяин записан (" + player.getUUID().equals(trunk.getOwnerUUID())
                            + "), улучшение земли даёт четвёртый ряд (" + rows + ", улучшение осталось " + trunk.getUpgrade() + "), вещи кладутся ("
                            + trunk.inventory.countItem(net.minecraft.world.item.Items.DIAMOND) + ", не влезло " + left.getCount()
                            + "), окно открывает крышку (" + open + " -> " + trunk.isOpen() + "), кнопка велит стоять (" + stay + " -> "
                            + trunk.getStay() + ")",
                    recipe && player.getUUID().equals(trunk.getOwnerUUID()) && rows == 4 && trunk.getUpgrade() == 1
                            && trunk.inventory.countItem(net.minecraft.world.item.Items.DIAMOND) == 40 && left.isEmpty() && open && !trunk.isOpen()
                            && stay && !trunk.getStay());
        });
        shot(20, "golems_trunk");
        // хозяин отходит на десять блоков: сундук скачет следом; на сорок — переносится
        cmd("tp @s 1060.5 -60 50.5 0 10");
        server(100, (server, player) -> {
            double near = trunks[0].distanceTo(player);
            check("дорожный сундук прыжками догоняет отошедшего хозяина (до него " + near + " блоков, было 14)", near < 8.0);
        });
        cmd("tp @s 1060.5 -60 10.5 0 10");
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            double near = trunks[0].distanceTo(player);
            // снимаем колокольчиком: без улучшения порядка вещи высыпаются, улучшение едет в предмете
            ItemStack packed = thaumcraft.common.items.golem.GolemBellItem.pickUpTrunk(trunks[0], false);
            int spilled = 0;
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(8))) {
                if (item.getItem().is(net.minecraft.world.item.Items.DIAMOND)) {
                    spilled += item.getItem().getCount();
                }
            }
            check("отставший на сорок блоков сундук перенёсся к хозяину (до него " + near + "), а снятый колокольчиком стал предметом с "
                            + "улучшением (" + packed.getTag() + ") и высыпал вещи (алмазов на земле " + spilled + ")",
                    near < 6.0 && packed.getTag() != null && packed.getTag().getByte("upgrade") == 1 && !packed.getTag().contains("inventory")
                            && spilled == 40 && !trunks[0].isAlive());
        });
        cmd("kill @e[type=item,distance=..20]");
        cmd("tp @s 1036.5 -59 56.5 0 20");

        // алхимик, украшения, дротикомёт, оковы и продвинутый голем
        cmd("setblock 1030 -60 60 thaumcraft:alembic");
        cmd("setblock 1034 -60 64 thaumcraft:warded_jar");
        cmd("setblock 1040 -61 60 thaumcraft:golem_fetter");
        cmd("setblock 1040 -61 61 minecraft:redstone_block");
        cmd("tp @s 1036.5 -59 56.5 0 20");
        thaumcraft.common.entity.golem.Golem[] cast = new thaumcraft.common.entity.golem.Golem[3];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            String dim = level.dimension().location().toString();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"golem_fetter", "golem_deco_tophat", "golem_deco_fez", "golem_deco_bowtie", "golem_deco_glasses",
                    "golem_deco_dart", "golem_deco_visor", "golem_deco_armor", "golem_deco_mace", "infusion/advanced_golem"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            var advanced = (thaumcraft.common.crafting.InfusionRecipe) level.getRecipeManager().byKey(Thaumcraft.id("infusion/advanced_golem")).orElse(null);
            ItemStack plain = new ItemStack(ModItems.GOLEMS.get(1).get());
            plain.getOrCreateTag().putByte("core", (byte) 2);
            ItemStack upgraded = advanced == null ? ItemStack.EMPTY : advanced.getResult(plain);
            check("рецепты оков, украшений и продвинутого голема загружены (нет:" + missing + "); наполнение делает голема продвинутым, не трогая "
                            + "его ядро (" + upgraded.getTag() + "), а уже продвинутого не берёт (простого берёт "
                            + (advanced != null && advanced.centreMatches(plain)) + ", продвинутого " + (advanced != null && advanced.centreMatches(upgraded))
                            + ", вид рецепта " + (advanced == null ? "нет" : advanced.getClass().getSimpleName()) + ")", missing.length() == 0 && advanced != null
                            && advanced.centreMatches(plain) && upgraded.is(plain.getItem()) && upgraded.getTag().getByte("core") == 2
                            && upgraded.getTag().contains("advanced") && !advanced.centreMatches(upgraded));

            // алхимик: дом — перегонный куб, цель — отмеченная банка
            ((thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(new BlockPos(1030, -60, 60))).add(thaumcraft.api.aspects.Aspect.FIRE, 16);
            ItemStack alchemist = new ItemStack(ModItems.GOLEMS.get(2).get());
            alchemist.getOrCreateTag().putByte("core", (byte) thaumcraft.common.items.golem.GolemCoreItem.ALCHEMY);
            cast[0] = ((thaumcraft.common.items.golem.GolemPlacerItem) alchemist.getItem()).spawn(level, 1030.5, -60, 61.5,
                    net.minecraft.core.Direction.SOUTH, alchemist, player);
            cast[0].setMarkers(java.util.List.of(new thaumcraft.common.entity.golem.GolemMarker(new BlockPos(1034, -60, 64), dim, (byte) 1, (byte) -1)));

            // продвинутый тауминиевый страж в украшениях
            ItemStack fancy = new ItemStack(ModItems.GOLEMS.get(7).get());
            fancy.getOrCreateTag().putByte("core", (byte) thaumcraft.common.items.golem.GolemCoreItem.GUARD);
            fancy.getTag().putByte("advanced", (byte) 1);
            var golem = ((thaumcraft.common.items.golem.GolemPlacerItem) fancy.getItem()).spawn(level, 1036.5, -60, 60.5,
                    net.minecraft.core.Direction.UP, fancy, player);
            cast[1] = golem;
            String[] order = {"tophat", "fez", "armor", "bowtie", "glasses", "visor", "mace", "dart"};
            StringBuilder worn = new StringBuilder();
            for (String name : order) {
                int index = java.util.Arrays.asList(thaumcraft.common.items.golem.GolemDecorationItem.NAMES).indexOf(name);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLEM_DECORATIONS.get(index).get()));
                golem.interact(player, InteractionHand.MAIN_HAND);
                if (player.containerMenu instanceof thaumcraft.common.menu.GolemMenu) {
                    player.closeContainer();
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            worn.append(golem.getGolemDecoration());
            double damage = golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            check("украшения: надеты цилиндр, латы, очки, булава и дротикомёт, а феска, бабочка и визор отвергнуты как несовместимые (" + worn
                            + "); здоровье 40+5 (" + golem.getMaxHealth() + "), броня 15+4 (" + golem.getArmorValue() + "), урон 2+4+2 (" + damage
                            + "), дальность продвинутого в очках (" + golem.getRange() + ")",
                    worn.toString().equals("HPGMR") && golem.getMaxHealth() == 45f && golem.getArmorValue() == 19 && damage == 8.0
                            && Math.abs(golem.getRange() - 21.12f) < 0.01f);
            golem.setNoAi(true);
            golem.setYRot(180f);
            golem.yBodyRot = 180f;
            golem.yHeadRot = 180f;

            // оковы под сигналом
            ItemStack bound = new ItemStack(ModItems.GOLEMS.get(5).get());
            bound.getOrCreateTag().putByte("core", (byte) thaumcraft.common.items.golem.GolemCoreItem.GUARD);
            cast[2] = ((thaumcraft.common.items.golem.GolemPlacerItem) bound.getItem()).spawn(level, 1040.5, -60, 60.5,
                    net.minecraft.core.Direction.UP, bound, player);
        });
        shot(30, "golems_fancy");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            cast[1].setNoAi(false);
            boolean powered = level.getBlockState(new BlockPos(1040, -61, 60)).getValue(thaumcraft.common.blocks.GolemFetterBlock.POWERED);
            check("оковы под сигналом красного камня включились (" + powered + "), и голем на них замер (" + cast[2].inactive + ", скорость "
                    + cast[2].getSpeed() + "), а соседний нет (" + cast[1].inactive + ")", powered && cast[2].inactive && cast[2].getSpeed() == 0f
                    && !cast[1].inactive);
            // зомби в двенадцати блоках: страж с дротикомётом должен достать его издали
            var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            zombie.moveTo(1036.5, -60, 72.5, 180f, 0f);
            zombie.setNoAi(true);
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
            level.addFreshEntity(zombie);
            dartTarget[0] = zombie;
        });
        int[] darts = new int[1];
        for (int wait = 0; wait < 10; wait++) {
            server(8, (server, player) -> {
                // дротик, попав в цель, исчезает сразу, поэтому считаем и летящие дротики, и раны от них
                darts[0] += player.serverLevel().getEntitiesOfClass(thaumcraft.common.entity.golem.GolemDart.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(1036, -60, 66)).inflate(16)).size();
                for (var zombie : player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(1036, -60, 66)).inflate(24))) {
                    var source = zombie.getLastDamageSource();
                    if (source != null && source.getDirectEntity() instanceof thaumcraft.common.entity.golem.GolemDart) {
                        darts[0]++;
                    }
                }
            });
        }
        shot(2, "golems_darts");
        server(120, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int zombies = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,
                    new net.minecraft.world.phys.AABB(new BlockPos(1036, -60, 66)).inflate(24)).size();
            // дротик мог убить зомби между замерами: тогда его выдаёт последняя рана мёртвого зомби
            if (dartTarget[0] != null && dartTarget[0].getLastDamageSource() != null
                    && dartTarget[0].getLastDamageSource().getDirectEntity() instanceof thaumcraft.common.entity.golem.GolemDart) {
                darts[0]++;
            }
            // дротики слабые, а голем иногда промахивается: добить зомби за шесть секунд он успевает не всегда — хватает раны
            float left = dartTarget[0] == null ? 20f : dartTarget[0].getHealth();
            check("страж с дротикомётом обстрелял зомби издали (дротиков и ран от них замечено " + darts[0] + ") и ранил или добил его (зомби осталось "
                    + zombies + ", здоровье " + left + ")", darts[0] > 0 && (zombies == 0 || left < dartTarget[0].getMaxHealth()));
            var jar = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(new BlockPos(1034, -60, 64));
            var alembic = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(new BlockPos(1030, -60, 60));
            check("голем с ядром алхимии перенёс эссенцию из перегонного куба в отмеченную банку (в банке " + jar.getAmount() + " "
                            + (jar.getAspect() == null ? "" : jar.getAspect().getTag()) + ", в кубе " + alembic.getAmount() + ", в руках "
                            + cast[0].essentiaAmount + ")",
                    jar.getAmount() + cast[0].essentiaAmount == 16 && jar.getAmount() >= 8 && alembic.getAmount() == 0
                            && jar.getAspect() == thaumcraft.api.aspects.Aspect.FIRE);
        });
        cmd("fill 1028 -60 58 1042 -60 74 minecraft:air");
        cmd("fill 1040 -61 60 1040 -61 61 minecraft:grass_block");
        purge(new BlockPos(1036, -60, 64));
        cmd("fill 994 -60 58 1018 -60 66 minecraft:air");
        cmd("fill 1015 -62 65 1017 -61 65 minecraft:dirt");
        cmd("fill 1015 -61 65 1017 -61 65 minecraft:grass_block");
        purge(new BlockPos(1008, -60, 62));
        cmd("kill @e[type=item,distance=..60]");
        cmd("kill @e[type=item,distance=..40]");
        cmd("difficulty peaceful");
    }

    private static void beasts() {
        cmd("tp @s 700.5 -60 0.5 0 20");
        client(40, mc -> {
        });
        cmd("gamemode creative");
        cmd("time set noon");
        cmd("difficulty normal");
        BlockPos centre = new BlockPos(700, -60, 8);
        cmd("tp @s 700.5 -60 8.5 0 20");
        cmd("thaumcraft taint");
        cmd("tp @s 700.5 -58.5 1.5 0 15");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.removeBlock(centre, false);
            java.util.List<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> row = java.util.List.of(
                    thaumcraft.common.registry.ModEntities.TAINTED_COW.get(), thaumcraft.common.registry.ModEntities.TAINTED_PIG.get(),
                    thaumcraft.common.registry.ModEntities.TAINTED_SHEEP.get(), thaumcraft.common.registry.ModEntities.TAINTED_CHICKEN.get(),
                    thaumcraft.common.registry.ModEntities.TAINTED_VILLAGER.get(), thaumcraft.common.registry.ModEntities.TAINTED_CREEPER.get(),
                    thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get());
            for (int i = 0; i < row.size(); i++) {
                net.minecraft.world.entity.Mob mob = row.get(i).create(level);
                mob.moveTo(centre.getX() - 4.5 + i * 1.6, centre.getY(), centre.getZ() - 1.5, 180f, 0f);
                mob.setYBodyRot(180f);
                mob.setYHeadRot(180f);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                if (mob instanceof thaumcraft.common.entity.ThaumicSlime slime) {
                    slime.setSlimeSize(4);
                }
                level.addFreshEntity(mob);
            }
            thaumcraft.common.entity.Taintacle taintacle = thaumcraft.common.registry.ModEntities.TAINTACLE.get().create(level);
            taintacle.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 3.5, 180f, 0f);
            taintacle.setPersistenceRequired();
            level.addFreshEntity(taintacle);
        });
        shot(8, "beasts_arise");
        shot(60, "beasts");
        cmd("tp @s 697.5 -59.5 4.0 0 10");
        shot(15, "beasts_left");
        cmd("tp @s 702.5 -59.5 4.0 0 10");
        shot(15, "beasts_right");
        cmd("tp @s 700.5 -58.5 7.5 0 0");
        shot(15, "beasts_taintacle");
        // стрижка порченой овцы
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var sheep = level.getEntitiesOfClass(thaumcraft.common.entity.TaintedBeasts.Sheep.class,
                    new net.minecraft.world.phys.AABB(centre).inflate(12)).get(0);
            ItemStack shears = new ItemStack(net.minecraft.world.item.Items.SHEARS);
            boolean could = sheep.isShearable(shears, level, sheep.blockPosition());
            java.util.List<ItemStack> wool = sheep.onSheared(player, shears, level, sheep.blockPosition(), 0);
            check("порченая овца стрижётся на лиловую шерсть (шерсти " + wool.size() + ", острижена " + sheep.isSheared() + ")",
                    could && !wool.isEmpty() && wool.size() <= 3 && wool.get(0).is(net.minecraft.world.item.Items.PURPLE_WOOL)
                            && sheep.isSheared() && !sheep.isShearable(shears, level, sheep.blockPosition()));
        });
        cmd("tp @s 698.5 -59.5 4.0 0 10");
        shot(15, "beasts_sheared");
        purge(centre);
        cmd("kill @e[type=item]");
        // смерть от порченого яда: корова становится порченой коровой, житель — порченым жителем, волк — слизнем
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            java.util.List<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity>> victims = java.util.List.of(
                    net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.VILLAGER,
                    net.minecraft.world.entity.EntityType.WOLF, net.minecraft.world.entity.EntityType.CREEPER,
                    net.minecraft.world.entity.EntityType.PIG, net.minecraft.world.entity.EntityType.SHEEP,
                    net.minecraft.world.entity.EntityType.CHICKEN);
            for (int i = 0; i < victims.size(); i++) {
                net.minecraft.world.entity.LivingEntity victim = victims.get(i).create(level);
                victim.moveTo(centre.getX() - 6.5 + i * 2, centre.getY(), centre.getZ() + 0.5, 0f, 0f);
                level.addFreshEntity(victim);
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        thaumcraft.common.registry.ModEffects.TAINT_POISON.get(), 200, 0));
                victim.hurt(level.damageSources().magic(), 1000f);
            }
            // а корова без яда умирает как обычно
            net.minecraft.world.entity.animal.Cow plain = net.minecraft.world.entity.EntityType.COW.create(level);
            plain.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 6.5, 0f, 0f);
            level.addFreshEntity(plain);
            plain.hurt(level.damageSources().magic(), 1000f);
        });
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(centre).inflate(16);
            int cows = level.getEntitiesOfClass(thaumcraft.common.entity.TaintedBeasts.Cow.class, box).size();
            int villagers = level.getEntitiesOfClass(thaumcraft.common.entity.TaintedBeasts.Villager.class, box).size();
            int creepers = level.getEntitiesOfClass(thaumcraft.common.entity.TaintCreeper.class, box).size();
            int pigs = level.getEntitiesOfClass(thaumcraft.common.entity.TaintedBeasts.Pig.class, box).size();
            int sheep = level.getEntitiesOfClass(thaumcraft.common.entity.TaintedBeasts.Sheep.class, box).size();
            int chickens = level.getEntitiesOfClass(thaumcraft.common.entity.TaintedBeasts.Chicken.class, box).size();
            var slimes = level.getEntitiesOfClass(thaumcraft.common.entity.ThaumicSlime.class, box);
            int vanilla = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, box, a -> a.isAlive()).size();
            check("умершие от порченого яда превратились: коров " + cows + ", жителей " + villagers + ", криперов " + creepers
                            + ", свиней " + pigs + ", овец " + sheep + ", кур " + chickens + ", слизней " + slimes.size()
                            + (slimes.isEmpty() ? "" : " (размер " + slimes.get(0).getSlimeSize() + ")") + ", обычных животных осталось " + vanilla,
                    cows == 1 && villagers == 1 && creepers == 1 && pigs == 1 && sheep == 1 && chickens == 1 && slimes.size() == 1
                            && slimes.get(0).getSlimeSize() == 1 && vanilla == 0);
        });
        cmd("tp @s 700.5 -57.5 2.5 0 25");
        shot(20, "beasts_converted");
        purge(centre);
        cmd("kill @e[type=item]");
        // взрыв порченого крипера: отравляет соседей и не ломает блоки
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(centre.offset(1, 0, 0), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3);
            thaumcraft.common.entity.TaintCreeper creeper = thaumcraft.common.registry.ModEntities.TAINTED_CREEPER.get().create(level);
            creeper.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 180f, 0f);
            creeper.setNoAi(true);
            creeper.setSwellDir(1);
            level.addFreshEntity(creeper);
            net.minecraft.world.entity.animal.IronGolem golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
            golem.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 3.5, 180f, 0f);
            golem.setNoAi(true);
            level.addFreshEntity(golem);
        });
        shot(24, "beasts_creeper_swell");
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(centre).inflate(16);
            int creepers = level.getEntitiesOfClass(thaumcraft.common.entity.TaintCreeper.class, box).size();
            var golems = level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, box);
            boolean poisoned = !golems.isEmpty() && golems.get(0).hasEffect(thaumcraft.common.registry.ModEffects.TAINT_POISON.get());
            boolean glass = level.getBlockState(centre.offset(1, 0, 0)).is(net.minecraft.world.level.block.Blocks.GLASS);
            check("порченый крипер взорвался: голем рядом отравлен (" + poisoned + "), стекло цело (" + glass + "), криперов " + creepers,
                    creepers == 0 && poisoned && glass);
            level.removeBlock(centre.offset(1, 0, 0), false);
        });
        purge(centre);
        // тентакль: до жертвы в шести блоках не дотягивается и выращивает у него под ногами малое щупальце
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-3, -1, -2), centre.offset(3, -1, 9))) {
                level.setBlock(pos, ModBlocks.TAINTED_SOIL.get().defaultBlockState(), 3);
            }
            thaumcraft.common.entity.Taintacle taintacle = thaumcraft.common.registry.ModEntities.TAINTACLE.get().create(level);
            taintacle.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(taintacle);
            // жертва должна стоять на земле, поэтому она не «без ИИ», а просто обездвижена и живуча
            net.minecraft.world.entity.animal.Cow cow = net.minecraft.world.entity.EntityType.COW.create(level);
            cow.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 6.5, 180f, 0f);
            cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
            cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
            cow.setHealth(100f);
            level.addFreshEntity(cow);
        });
        cmd("tp @s 705.5 -58.5 4.5 60 10");
        shot(50, "beasts_tendril");
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(centre).inflate(16);
            int small = level.getEntitiesOfClass(thaumcraft.common.entity.TaintacleSmall.class, box).size();
            var cows = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class, box);
            float health = cows.isEmpty() ? -1 : cows.get(0).getHealth();
            check("тентакль вырастил малые щупальца под дальней целью (щупалец " + small + "), и они её бьют (здоровье жертвы " + health + " из 100)",
                    small >= 1 && health >= 0 && health < 100f);
        });
        // малое щупальце живёт десять секунд
        server(230, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(centre).inflate(16);
            level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class, box).forEach(net.minecraft.world.entity.Entity::discard);
        });
        server(260, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(centre).inflate(16);
            int small = level.getEntitiesOfClass(thaumcraft.common.entity.TaintacleSmall.class, box).size();
            int big = level.getEntitiesOfClass(thaumcraft.common.entity.Taintacle.class, box, t -> !t.isSmall()).size();
            check("малые щупальца отмерли сами, большое живо (малых " + small + ", больших " + big + ")", small == 0 && big == 1);
        });
        purge(centre);
        // слизень: большой распадается на маленьких, а лужа флюсовой жижи сама собирается в слизня
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.entity.ThaumicSlime big = thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get().create(level);
            big.setSlimeSize(9);
            big.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(big);
            boolean sized = big.getMaxHealth() == 9f && big.getHealth() == 9f;
            big.hurt(level.damageSources().magic(), 1000f);
            check("слизень девятого размера имеет девять единиц здоровья", sized);
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(centre).inflate(16);
            var slimes = level.getEntitiesOfClass(thaumcraft.common.entity.ThaumicSlime.class, box);
            boolean allSmall = slimes.stream().allMatch(s -> s.getSlimeSize() == 1);
            check("убитый слизень девятого размера распался на трёх маленьких (слизней " + slimes.size() + ")", slimes.size() == 3 && allSmall);
            slimes.forEach(net.minecraft.world.entity.Entity::discard);
            BlockPos puddle = centre.offset(0, 0, 4);
            net.minecraft.world.level.block.state.BlockState goo = ModBlocks.FLUX_GOO.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.FluxBlock.LEVEL, 7);
            int born = 0;
            for (int i = 0; i < 3000 && born == 0; i++) {
                // над распадающейся жижей появляется газ, а под газом она уже ни во что не превращается
                level.removeBlock(puddle.above(), false);
                level.setBlock(puddle, goo, 3);
                goo.randomTick(level, puddle, level.random);
                born = level.getEntitiesOfClass(thaumcraft.common.entity.ThaumicSlime.class, box).size();
            }
            var born2 = level.getEntitiesOfClass(thaumcraft.common.entity.ThaumicSlime.class, box);
            check("лужа флюсовой жижи собралась в слизня второго размера, сама лужа исчезла",
                    born2.size() == 1 && born2.get(0).getSlimeSize() == 2 && level.getBlockState(puddle).isAir());
        });
        purge(centre);
        // правило появления тентаклей: ночью, на волокнах порчи в порченой земле
        cmd("time set midnight");
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos fibres = centre.offset(0, 0, 2);
            level.setBlock(fibres, thaumcraft.common.taint.TaintFibresBlock.stateFor(level, fibres), 3);
            BlockPos clean = centre.offset(60, 0, 0);
            int here = 0;
            int there = 0;
            for (int i = 0; i < 200; i++) {
                if (thaumcraft.common.entity.Taintacle.canSpawn(thaumcraft.common.registry.ModEntities.TAINTACLE.get(), level,
                        net.minecraft.world.entity.MobSpawnType.NATURAL, fibres, level.random)) {
                    here++;
                }
                if (thaumcraft.common.entity.Taintacle.canSpawn(thaumcraft.common.registry.ModEntities.TAINTACLE.get(), level,
                        net.minecraft.world.entity.MobSpawnType.NATURAL, clean, level.random)) {
                    there++;
                }
            }
            check("тентакль может появиться ночью на волокнах порчи и не может на чистой земле (удачных попыток " + here + " и " + there + " из 200)",
                    here > 0 && there == 0);
        });
        cmd("time set noon");
        purge(centre);
        cmd("difficulty peaceful");
    }

    /** Хвосты: сферы аспектов, узлы, ступени и всё, что раньше оставалось непроверенным в порче. */
    private static void leftovers() {
        cmd("tp @s 800.5 -60 0.5 0 20");
        client(40, mc -> {
        });
        cmd("gamemode creative");
        cmd("time set noon");
        cmd("difficulty normal");
        BlockPos centre = new BlockPos(800, -60, 8);
        net.minecraft.world.phys.AABB[] box = {new net.minecraft.world.phys.AABB(centre).inflate(20)};
        // --- сферы аспектов: из убитых игроком выпадают и заряжают жезл, из убитых иначе — нет ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.getInventory().clearContent();
            player.getInventory().setItem(0, thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, false));
            for (int i = 0; i < 4; i++) {
                net.minecraft.world.entity.monster.Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
                zombie.moveTo(centre.getX() + 0.5 + i, centre.getY(), centre.getZ() + 20.5, 0f, 0f);
                zombie.setNoAi(true);
                level.addFreshEntity(zombie);
                zombie.hurt(level.damageSources().magic(), 1000f);
            }
        });
        server(30, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int orbs = level.getEntitiesOfClass(thaumcraft.common.entity.AspectOrbEntity.class, box[0].inflate(20)).size();
            check("из существ, убитых не игроком, сферы аспектов не выпадают (сфер " + orbs + ")", orbs == 0);
            for (int i = 0; i < 8; i++) {
                net.minecraft.world.entity.monster.Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
                zombie.moveTo(centre.getX() - 3.5 + i, centre.getY(), centre.getZ() - 3.5, 0f, 0f);
                zombie.setNoAi(true);
                level.addFreshEntity(zombie);
                zombie.hurt(level.damageSources().playerAttack(player), 1000f);
            }
        });
        int[] seen = new int[1];
        server(8, (server, player) -> seen[0] = player.serverLevel()
                .getEntitiesOfClass(thaumcraft.common.entity.AspectOrbEntity.class, box[0]).size());
        cmd("tp @s 800.5 -59 0.5 0 15");
        shot(4, "leftovers_orbs");
        server(120, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ItemStack wand = player.getInventory().getItem(0);
            int vis = 0;
            for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                vis += thaumcraft.common.items.wands.WandItem.getVis(wand, aspect);
            }
            int left = level.getEntitiesOfClass(thaumcraft.common.entity.AspectOrbEntity.class, box[0]).size();
            check("из убитых игроком существ выпали сферы аспектов (" + seen[0] + "), слетелись к жезлу на панели и зарядили его (вис в жезле "
                    + vis + " сотых, сфер осталось " + left + ")", seen[0] > 0 && vis > 0 && left < seen[0]);
        });
        purge(centre);
        // --- нестабильный узел выбрасывает сферы, голодный поглощает убитых ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = centre.offset(0, 2, 0);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, pos, level.random, false);
            thaumcraft.common.blockentity.NodeBlockEntity node = (thaumcraft.common.blockentity.NodeBlockEntity) level.getBlockEntity(pos);
            node.setNodeType(thaumcraft.api.nodes.NodeType.UNSTABLE);
            int before = node.visSize();
            for (int i = 0; i < 1500; i++) {
                thaumcraft.common.blockentity.NodeBlockEntity.serverTick(level, pos, level.getBlockState(pos), node);
            }
            int orbs = level.getEntitiesOfClass(thaumcraft.common.entity.AspectOrbEntity.class, box[0]).size();
            check("нестабильный узел выбросил сферы аспектов (сфер " + orbs + ", вис в узле было " + before + ", стало " + node.visSize() + ")",
                    orbs >= 1);
            level.removeBlock(pos, false);
            // голодный узел, осушённый досуха: убитая курица обязана его пополнить или расширить
            BlockPos hungry = centre.offset(6, 1, 0);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, hungry, level.random, false);
            thaumcraft.common.blockentity.NodeBlockEntity eater = (thaumcraft.common.blockentity.NodeBlockEntity) level.getBlockEntity(hungry);
            eater.setNodeType(thaumcraft.api.nodes.NodeType.HUNGRY);
            for (thaumcraft.api.aspects.Aspect aspect : java.util.List.copyOf(eater.getAspects())) {
                eater.takeFromContainer(aspect, eater.getAmount(aspect));
            }
            net.minecraft.world.entity.animal.Chicken chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(level);
            chicken.moveTo(hungry.getX() + 0.5, hungry.getY() + 0.2, hungry.getZ() + 0.5, 0f, 0f);
            chicken.setNoAi(true);
            level.addFreshEntity(chicken);
            int total = eater.visSize() + eater.baseSize();
            int ticks = 0;
            while (chicken.isAlive() && ticks++ < 400) {
                chicken.invulnerableTime = 0;
                thaumcraft.common.blockentity.NodeBlockEntity.serverTick(level, hungry, level.getBlockState(hungry), eater);
            }
            int after = eater.visSize() + eater.baseSize();
            check("голодный узел убил курицу и поглотил её аспект (курица жива " + chicken.isAlive() + ", вис и предел узла в сумме " + total
                    + " -> " + after + ")", !chicken.isAlive() && after >= total + 1);
            level.removeBlock(hungry, false);
        });
        purge(centre);
        // --- заражённый узел заражает землю вокруг ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = centre.offset(0, 1, 30);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, pos, level.random, false);
            thaumcraft.common.blockentity.NodeBlockEntity node = (thaumcraft.common.blockentity.NodeBlockEntity) level.getBlockEntity(pos);
            node.setNodeType(thaumcraft.api.nodes.NodeType.TAINTED);
            boolean clean = !thaumcraft.common.taint.Taint.isTainted(level, pos);
            for (int i = 0; i < 4000; i++) {
                thaumcraft.common.blockentity.NodeBlockEntity.serverTick(level, pos, level.getBlockState(pos), node);
            }
            int cells = 0;
            int fibres = 0;
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-8, -2, -8), pos.offset(8, 3, 8))) {
                if (near.getY() == pos.getY() && thaumcraft.common.taint.Taint.isTainted(level, near)) {
                    cells++;
                }
                if (level.getBlockState(near).is(ModBlocks.TAINT_FIBRES.get())) {
                    fibres++;
                }
            }
            check("заражённый узел заразил землю вокруг себя (сначала чисто " + clean + ", заражённых блоков " + cells + ", волокон " + fibres + ")",
                    clean && cells >= 16);
            level.removeBlock(pos, false);
        });
        // --- выплеск порчи (первозданная сфера, порченый крипер) ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = centre.offset(60, 0, 0);
            boolean clean = !thaumcraft.common.taint.Taint.isTainted(level, pos);
            for (int i = 0; i < 12; i++) {
                thaumcraft.common.taint.Taint.splash(level, pos, 6f, true);
            }
            int cells = 0;
            int fibres = 0;
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 1, 8))) {
                if (near.getY() == pos.getY() && thaumcraft.common.taint.Taint.isTainted(level, near)) {
                    cells++;
                }
                if (level.getBlockState(near).is(ModBlocks.TAINT_FIBRES.get())) {
                    fibres++;
                }
            }
            check("выплеск порчи заражает землю и растит волокна (сначала чисто " + clean + ", заражённых блоков " + cells + ", волокон " + fibres + ")",
                    clean && cells >= 16 && fibres >= 1);
        });
        // --- порченая овца «пасётся»: на траве вырастают волокна, земля заражается ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = centre.offset(-40, 0, 0);
            thaumcraft.common.entity.TaintedBeasts.Sheep sheep = thaumcraft.common.registry.ModEntities.TAINTED_SHEEP.get().create(level);
            sheep.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(sheep);
            boolean clean = !thaumcraft.common.taint.Taint.isTainted(level, pos);
            for (var wrapped : sheep.goalSelector.getAvailableGoals()) {
                net.minecraft.world.entity.ai.goal.Goal goal = wrapped.getGoal();
                if (goal.getClass().getSimpleName().equals("ConvertGrassGoal")) {
                    goal.start();
                    for (int i = 0; i < 40; i++) {
                        goal.tick();
                    }
                }
            }
            check("порченая овца, попасшись, заразила землю и оставила волокна (сначала чисто " + clean + ")",
                    clean && thaumcraft.common.taint.Taint.isTainted(level, pos) && level.getBlockState(pos).is(ModBlocks.TAINT_FIBRES.get()));
            sheep.discard();
        });
        // --- корка порчи под открытым небом становится роем спор; лужа жижи заражает землю ---
        cmd("tp @s 800.5 -60 8.5 0 20");
        cmd("thaumcraft taint");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = centre.offset(1, 0, 1);
            int tries = 0;
            while (tries++ < 6000 && level.getEntitiesOfClass(thaumcraft.common.entity.TaintSporeSwarmer.class, box[0]).isEmpty()) {
                if (!level.getBlockState(pos).is(ModBlocks.TAINT_CRUST.get())) {
                    level.setBlock(pos, ModBlocks.TAINT_CRUST.get().defaultBlockState(), 3);
                }
                level.getBlockState(pos).randomTick(level, pos, level.random);
            }
            int swarmers = level.getEntitiesOfClass(thaumcraft.common.entity.TaintSporeSwarmer.class, box[0]).size();
            check("корка порчи под открытым небом превратилась в рой спор (попыток " + tries + ", роёв спор " + swarmers + ")",
                    swarmers == 1 && !level.getBlockState(pos).is(ModBlocks.TAINT_CRUST.get()));
            BlockPos puddle = centre.offset(0, 0, 90);
            boolean clean = !thaumcraft.common.taint.Taint.isTainted(level, puddle);
            net.minecraft.world.level.block.state.BlockState goo = ModBlocks.FLUX_GOO.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.FluxBlock.LEVEL, 7);
            int rounds = 0;
            while (rounds++ < 6000 && !thaumcraft.common.taint.Taint.isTainted(level, puddle)) {
                // распадаясь, жижа выпускает над собой газ, а под газом она уже ни во что не превращается
                level.removeBlock(puddle.above(), false);
                level.setBlock(puddle, goo, 3);
                goo.randomTick(level, puddle, level.random);
            }
            check("полная лужа флюсовой жижи заразила землю и стала волокнами (сначала чисто " + clean + ", попыток " + rounds + ")",
                    clean && thaumcraft.common.taint.Taint.isTainted(level, puddle) && level.getBlockState(puddle).is(ModBlocks.TAINT_FIBRES.get()));
        });
        purge(centre);
        server(5, (server, player) -> player.serverLevel().getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(centre.offset(0, 0, 90)).inflate(24), e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(net.minecraft.world.entity.Entity::discard));
        // --- порченая слизь в инвентаре отравляет владельца ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 4.5, 0f, 0f);
            level.addFreshEntity(pig);
            ItemStack goo = new ItemStack(ModItems.TAINT_GOO.get(), 64);
            int ticks = 0;
            while (ticks++ < 4000 && !pig.hasEffect(thaumcraft.common.registry.ModEffects.TAINT_POISON.get())) {
                goo.inventoryTick(level, pig, 0, false);
            }
            check("стопка порченой слизи отравила своего носителя порченым ядом (тиков " + ticks + ")",
                    pig.hasEffect(thaumcraft.common.registry.ModEffects.TAINT_POISON.get()));
            pig.discard();
        });
        // --- тентакли: биом порченых земель знает их, а правило появления зарегистрировано ---
        cmd("time set midnight");
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos fibres = centre.offset(0, 0, 2);
            level.setBlock(fibres, thaumcraft.common.taint.TaintFibresBlock.stateFor(level, fibres), 3);
            boolean listed = level.getBiome(fibres).value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap()
                    .stream().anyMatch(data -> data.type == thaumcraft.common.registry.ModEntities.TAINTACLE.get());
            int ok = 0;
            for (int i = 0; i < 200; i++) {
                if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(thaumcraft.common.registry.ModEntities.TAINTACLE.get(), level,
                        net.minecraft.world.entity.MobSpawnType.NATURAL, fibres, level.random)) {
                    ok++;
                }
            }
            boolean clean = net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(thaumcraft.common.registry.ModEntities.TAINTACLE.get(), level,
                    net.minecraft.world.entity.MobSpawnType.NATURAL, centre.offset(0, 0, 60), level.random);
            check("игра сама может породить тентакля: он в списке монстров порченых земель (" + listed + "), правило появления пропускает его на волокнах ("
                    + ok + " из 200) и не пропускает на чистой земле (" + clean + ")", listed && ok > 0 && !clean);
        });
        cmd("time set noon");
        // --- слизни: большой плюётся в игрока, маленькие сливаются ---
        cmd("gamemode survival");
        cmd("effect give @s minecraft:resistance 60 4 true");
        // игрок стоит на столбе: слизень до него не допрыгнет и будет плеваться
        cmd("setblock 800 -54 0 minecraft:glass");
        cmd("tp @s 800.5 -53 0.5 0 40");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.entity.ThaumicSlime big = thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get().create(level);
            big.setSlimeSize(9);
            big.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 1.5, 180f, 0f);
            level.addFreshEntity(big);
        });
        shot(125, "leftovers_slime_spit");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var slimes = level.getEntitiesOfClass(thaumcraft.common.entity.ThaumicSlime.class, box[0]);
            int biggest = slimes.stream().mapToInt(thaumcraft.common.entity.ThaumicSlime::getSlimeSize).max().orElse(0);
            check("слизень девятого размера плюнул в игрока маленьким и сам похудел (слизней " + slimes.size() + ", самый большой " + biggest + ")",
                    slimes.size() >= 2 && biggest == 8);
        });
        cmd("gamemode creative");
        cmd("effect clear @s");
        cmd("setblock 800 -54 0 minecraft:air");
        cmd("tp @s 800.5 -60 0.5 0 5");
        purge(centre);
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (int i = 0; i < 2; i++) {
                thaumcraft.common.entity.ThaumicSlime small = thaumcraft.common.registry.ModEntities.THAUMIC_SLIME.get().create(level);
                small.setSlimeSize(1);
                small.moveTo(centre.getX() + 0.5 + i * 2, centre.getY(), centre.getZ() + 0.5, 0f, 0f);
                small.tickCount = 200;
                small.setPersistenceRequired();
                level.addFreshEntity(small);
            }
        });
        server(400, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var slimes = level.getEntitiesOfClass(thaumcraft.common.entity.ThaumicSlime.class, box[0]);
            check("два маленьких слизня сползлись и слились в одного второго размера (слизней " + slimes.size() + ")",
                    slimes.size() == 1 && slimes.get(0).getSlimeSize() == 2);
        });
        purge(centre);
        // --- деревня злится на того, кто бьёт порченого жителя ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            net.minecraft.world.entity.npc.Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
            villager.moveTo(centre.getX() + 3.5, centre.getY(), centre.getZ() + 0.5, 0f, 0f);
            villager.setNoAi(true);
            level.addFreshEntity(villager);
            thaumcraft.common.entity.TaintedBeasts.Villager tainted = thaumcraft.common.registry.ModEntities.TAINTED_VILLAGER.get().create(level);
            tainted.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0f, 0f);
            tainted.setNoAi(true);
            level.addFreshEntity(tainted);
            int before = villager.getPlayerReputation(player);
            tainted.hurt(level.damageSources().playerAttack(player), 1f);
            int after = villager.getPlayerReputation(player);
            check("ударив порченого жителя, игрок испортил себе репутацию у жителей рядом (" + before + " -> " + after + ")", after < before);
        });
        purge(centre);
        // --- подсказка от сканирования существа; аспекты предметов, сделанных инфузией ---
        server(10, (server, player) -> {
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            boolean known = knowledge.isResearchComplete("FOCUSHELLBAT") || knowledge.isResearchComplete("@FOCUSHELLBAT");
            boolean clue = thaumcraft.common.research.ResearchHelper.createClue(player, "entity:thaumcraft:fire_bat",
                    new thaumcraft.api.aspects.AspectList());
            check("сканирование огненной летучей мыши даёт подсказку к скрытому исследованию фокуса (уже известно " + known + ", подсказка " + clue + ")",
                    known || clue && knowledge.isResearchComplete("@FOCUSHELLBAT"));
            thaumcraft.api.aspects.AspectList amulet = thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getAspects(
                    new ItemStack(ModItems.VIS_AMULET.get()));
            StringBuilder line = new StringBuilder();
            for (thaumcraft.api.aspects.Aspect aspect : amulet.getAspectsSortedByName()) {
                line.append(' ').append(aspect.getTag()).append(':').append(amulet.getAmount(aspect));
            }
            check("у предмета, сделанного инфузией, есть аспекты, и в них вошла цена рецепта (амулет вис:" + line + ")",
                    !amulet.isEmpty() && amulet.getAmount(thaumcraft.api.aspects.Aspect.byTag("praecantatio")) > 0);
        });
        // --- порча в бутылке: рецепт в тигле и бросок ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ItemStack phial = thaumcraft.common.items.PhialItem.filled(ModItems.PHIAL.get(), thaumcraft.api.aspects.Aspect.byTag("vitium"));
            ItemStack wrong = thaumcraft.common.items.PhialItem.filled(ModItems.PHIAL.get(), thaumcraft.api.aspects.Aspect.byTag("aqua"));
            var recipe = level.getRecipeManager().byKey(Thaumcraft.id("crucible/bottle_taint"));
            boolean matches = recipe.isPresent() && recipe.get() instanceof thaumcraft.common.crafting.CrucibleRecipe crucible
                    && crucible.getCatalyst().test(phial) && !crucible.getCatalyst().test(wrong)
                    && !crucible.getCatalyst().test(new ItemStack(ModItems.PHIAL.get()));
            check("рецепт порчи в бутылке принимает только флакон с эссенцией порчи", matches);
            BlockPos target = centre.offset(40, 0, 40);
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(target.getX() + 2.5, target.getY(), target.getZ() + 0.5, 0f, 0f);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            thaumcraft.common.entity.BottleTaintEntity bottle = new thaumcraft.common.entity.BottleTaintEntity(level, player);
            bottle.setPos(target.getX() + 0.5, target.getY() + 3, target.getZ() + 0.5);
            bottle.setDeltaMovement(0, -0.5, 0);
            level.addFreshEntity(bottle);
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos target = centre.offset(40, 0, 40);
            var pigs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, new net.minecraft.world.phys.AABB(target).inflate(6));
            boolean poisoned = !pigs.isEmpty() && pigs.get(0).hasEffect(thaumcraft.common.registry.ModEffects.TAINT_POISON.get());
            int bottles = level.getEntitiesOfClass(thaumcraft.common.entity.BottleTaintEntity.class, new net.minecraft.world.phys.AABB(target).inflate(8)).size();
            check("брошенная порча в бутылке разбилась и отравила свинью рядом (отравлена " + poisoned + ", бутылок в полёте " + bottles + ")",
                    poisoned && bottles == 0);
            pigs.forEach(net.minecraft.world.entity.Entity::discard);
        });
        // --- рой порчи жалит и ослабляет ---
        cmd("tp @s 800.5 -60 8.5 0 5");
        cmd("gamemode survival");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.setHealth(20f);
            thaumcraft.common.entity.TaintSwarm swarm = thaumcraft.common.registry.ModEntities.TAINT_SWARM.get().create(level);
            swarm.moveTo(player.getX(), player.getY() + 1, player.getZ() + 2, 0f, 0f);
            level.addFreshEntity(swarm);
        });
        server(100, (server, player) -> {
            boolean weak = player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
            check("рой порчи напал на игрока: ранил и ослабил (здоровье " + player.getHealth() + " из 20, слабость " + weak + ")",
                    player.getHealth() < 20f && weak);
            player.setHealth(20f);
            player.removeAllEffects();
        });
        cmd("gamemode creative");
        purge(centre);
        // --- «Отвод узла» и «Сохранение узла» ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = new BlockPos(800, -59, 4);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, pos, level.random, false);
            thaumcraft.common.blockentity.NodeBlockEntity node = (thaumcraft.common.blockentity.NodeBlockEntity) level.getBlockEntity(pos);
            thaumcraft.api.aspects.AspectList all = new thaumcraft.api.aspects.AspectList();
            for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                all.add(aspect, 50);
            }
            node.setAspects(all);
            player.teleportTo(level, 800.5, -60, 1.5, 0f, 0f);
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            boolean had1 = knowledge.isResearchComplete("NODETAPPER1");
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, false);
            wand.getOrCreateTag().putInt("IIUX", pos.getX());
            wand.getOrCreateTag().putInt("IIUY", pos.getY());
            wand.getOrCreateTag().putInt("IIUZ", pos.getZ());
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            java.util.function.IntSupplier inNode = () -> node.visSize();
            int start = inNode.getAsInt();
            wand.getItem().onUseTick(level, player, wand, 72000 - 5);
            int plain = start - inNode.getAsInt();
            knowledge.completeResearch("NODETAPPER1");
            knowledge.completeResearch("NODETAPPER2");
            int mid = inNode.getAsInt();
            wand.getItem().onUseTick(level, player, wand, 72000 - 10);
            int tapped = mid - inNode.getAsInt();
            // «Сохранение узла»: в каждом аспекте по две единицы — жезл может взять только по одной
            knowledge.completeResearch("NODEPRESERVE");
            thaumcraft.api.aspects.AspectList low = new thaumcraft.api.aspects.AspectList();
            for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                low.add(aspect, 2);
            }
            node.setAspects(low);
            for (int i = 0; i < 40; i++) {
                wand.getItem().onUseTick(level, player, wand, 72000 - 15 - i * 5);
            }
            int least = Integer.MAX_VALUE;
            for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                least = Math.min(least, node.getAmount(aspect));
            }
            check("жезл берёт из узла 1 вис за раз" + (had1 ? " (исследование уже было)" : "") + ", с «Отводом узла» — 3 (взято " + plain + " и " + tapped
                            + "), а «Сохранение узла» оставляет в каждом аспекте по единице (осталось не меньше " + least + ", всего " + inNode.getAsInt() + ")",
                    (had1 || plain == 1) && tapped == 3 && least == 1 && inNode.getAsInt() == 6);
            level.removeBlock(pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        // --- «Опытный исследователь»: стёртые аспекты часто возвращаются ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            thaumcraft.common.research.ResearchEntry entry = null;
            for (thaumcraft.common.research.ResearchEntry candidate : thaumcraft.common.research.ResearchRegistry.entries()) {
                if (!candidate.aspects.isEmpty() && !candidate.isPurchasable()) {
                    entry = candidate;
                    break;
                }
            }
            thaumcraft.common.research.ResearchNote note = thaumcraft.common.research.ResearchNote.create(entry, level.random);
            thaumcraft.common.research.ResearchNote.Hex free = null;
            for (var hex : note.hexes.entrySet()) {
                if (hex.getValue().type() == thaumcraft.common.research.ResearchNote.EMPTY) {
                    free = hex.getKey();
                }
            }
            BlockPos pos = centre.offset(0, 3, 0);
            thaumcraft.common.blockentity.ResearchTableBlockEntity table = new thaumcraft.common.blockentity.ResearchTableBlockEntity(pos,
                    ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
            table.setLevel(level);
            table.getItems().set(thaumcraft.common.blockentity.ResearchTableBlockEntity.NOTES_SLOT,
                    thaumcraft.common.items.ResearchNotesItem.create(ModItems.RESEARCH_NOTES.get(), note));
            thaumcraft.api.aspects.Aspect air = thaumcraft.api.aspects.Aspect.byTag("aer");
            knowledge.discover(air);
            knowledge.completeResearch("RESEARCHER1");
            knowledge.completeResearch("RESEARCHER2");
            knowledge.addPool(air, 200 - knowledge.getPool(air));
            int cycles = 0;
            for (int i = 0; i < 60 && free != null; i++) {
                table.getItems().set(thaumcraft.common.blockentity.ResearchTableBlockEntity.TOOLS_SLOT, new ItemStack(ModItems.SCRIBING_TOOLS.get()));
                table.placeAspect(player, free.q(), free.r(), air);
                table.placeAspect(player, free.q(), free.r(), null);
                cycles++;
            }
            int spent = 200 - knowledge.getPool(air);
            // без исследования ушло бы ровно 60; с ним — около 60 * 0.9 - 60 * 0.5 = 24
            check("«Опытный исследователь» бережёт аспекты: за " + cycles + " раз «положил и стёр» ушло " + spent + " вместо " + cycles,
                    cycles == 60 && spent > 5 && spent < 45);
        });
        // --- злой и яростный зомби, мозг зомби ---
        cmd("fill 796 -57 20 804 -57 24 minecraft:smooth_stone_slab");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.entity.BrainyZombie angry = thaumcraft.common.registry.ModEntities.BRAINY_ZOMBIE.get().create(level);
            angry.moveTo(798.5, -60, 22.5, 180f, 0f);
            angry.setNoAi(true);
            angry.setBaby(false);
            level.addFreshEntity(angry);
            thaumcraft.common.entity.BrainyZombie.Giant furious = thaumcraft.common.registry.ModEntities.GIANT_BRAINY_ZOMBIE.get().create(level);
            furious.moveTo(802.5, -60, 22.5, 180f, 0f);
            furious.setNoAi(true);
            furious.setBaby(false);
            level.addFreshEntity(furious);
            float width = furious.getBbWidth();
            for (int i = 0; i < 5; i++) {
                furious.invulnerableTime = 0;
                furious.hurt(level.damageSources().magic(), 1f);
            }
            check("злой зомби: 25 здоровья и 5 брони (" + angry.getMaxHealth() + ", " + angry.getArmorValue() + "); яростный: 60 здоровья ("
                            + furious.getMaxHealth() + "), от пяти ударов злость 1 -> " + furious.getAnger() + ", он вырос (" + width + " -> "
                            + furious.getBbWidth() + ")",
                    angry.getMaxHealth() == 25f && angry.getArmorValue() == 5 && furious.getMaxHealth() == 60f
                            && Math.abs(furious.getAnger() - 1.5f) < 0.01f && furious.getBbWidth() > width);
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            int before = knowledge.getWarpTemp() + knowledge.getWarpSticky();
            new ItemStack(ModItems.ZOMBIE_BRAIN.get()).finishUsingItem(level, player);
            int after = knowledge.getWarpTemp() + knowledge.getWarpSticky();
            check("съеденный мозг зомби добавляет искажения (" + before + " -> " + after + ")", after > before && after <= before + 3);
            knowledge.addWarpTemp(-knowledge.getWarpTemp());
            knowledge.addWarpSticky(-knowledge.getWarpSticky());
        });
        cmd("tp @s 800.5 -60 17.5 0 0");
        shot(20, "leftovers_zombies");
        purge(centre);
        cmd("fill 796 -57 20 804 -57 24 minecraft:air");
        cmd("tp @s 800.5 -60 0.5 0 5");
        // --- тёмный узел: жуткие земли вокруг и яростные зомби ночью; чистый узел очищает порчу ---
        cmd("time set midnight");
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos pos = centre.offset(0, 1, -40);
            player.teleportTo(level, pos.getX() + 0.5, pos.getY() - 1, pos.getZ() + 8.5, 180f, 0f);
            thaumcraft.common.world.NodeGenerator.createRandomNode(level, pos, level.random, false);
            thaumcraft.common.blockentity.NodeBlockEntity node = (thaumcraft.common.blockentity.NodeBlockEntity) level.getBlockEntity(pos);
            node.setNodeType(thaumcraft.api.nodes.NodeType.DARK);
            boolean clean = !thaumcraft.common.world.ModBiomes.is(level, pos, thaumcraft.common.world.ModBiomes.EERIE);
            for (int i = 0; i < 4000; i++) {
                thaumcraft.common.blockentity.NodeBlockEntity.serverTick(level, pos, level.getBlockState(pos), node);
            }
            int cells = 0;
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-12, 0, -12), pos.offset(12, 0, 12))) {
                if (thaumcraft.common.world.ModBiomes.is(level, near, thaumcraft.common.world.ModBiomes.EERIE)) {
                    cells++;
                }
            }
            int giants = level.getEntitiesOfClass(thaumcraft.common.entity.BrainyZombie.Giant.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(12)).size();
            check("тёмный узел превратил землю вокруг в жуткие земли (сначала обычная " + clean + ", блоков " + cells
                    + ") и ночью призвал яростных зомби, не больше четырёх (" + giants + ")", clean && cells >= 16 && giants >= 1 && giants <= 4);
            level.removeBlock(pos, false);
        });
        shot(20, "leftovers_eerie");
        cmd("time set noon");
        purge(centre.offset(0, 0, -40));
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // чистый узел посреди порченой земли (сцена выше заразила участок у centre) и такой же — на обычной земле
            BlockPos inTaint = centre.offset(0, 2, 0);
            BlockPos inPlain = centre.offset(0, 2, 120);
            int[] forest = new int[2];
            int[] taint = new int[2];
            BlockPos[] spots = {inTaint, inPlain};
            for (int n = 0; n < 2; n++) {
                BlockPos pos = spots[n];
                thaumcraft.common.world.NodeGenerator.createRandomNode(level, pos, level.random, false);
                thaumcraft.common.blockentity.NodeBlockEntity node = (thaumcraft.common.blockentity.NodeBlockEntity) level.getBlockEntity(pos);
                node.setNodeType(thaumcraft.api.nodes.NodeType.PURE);
                for (int i = 0; i < 6000; i++) {
                    thaumcraft.common.blockentity.NodeBlockEntity.serverTick(level, pos, level.getBlockState(pos), node);
                }
                for (BlockPos near : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 0, 8))) {
                    if (thaumcraft.common.world.ModBiomes.is(level, near, thaumcraft.common.world.ModBiomes.MAGICAL_FOREST)) {
                        forest[n]++;
                    }
                    if (thaumcraft.common.taint.Taint.isTainted(level, near)) {
                        taint[n]++;
                    }
                }
                level.removeBlock(pos, false);
            }
            check("чистый узел превратил порченую землю вокруг в волшебный лес (волшебного леса " + forest[0] + " блоков, порчи осталось "
                            + taint[0] + "), а обычную землю не тронул (" + forest[1] + ")",
                    forest[0] >= 48 && taint[0] < 96 && forest[1] == 0);
        });
        cmd("tp @s 800.5 -57 -2.5 0 30");
        shot(20, "leftovers_magical_patch");
        cmd("tp @s 800.5 -60 0.5 0 5");
        // --- стручки маны, зёрна маны и вишрум ---
        cmd("tp @s 800.5 -60 60.5 0 0");
        cmd("fill 796 -57 66 804 -57 66 minecraft:oak_log[axis=x]");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (int x = 796; x <= 804; x += 4) {
                thaumcraft.common.world.ModBiomes.set(level, new BlockPos(x, -58, 66), thaumcraft.common.world.ModBiomes.MAGICAL_FOREST);
            }
            net.minecraft.world.level.block.state.BlockState pod = ModBlocks.MANA_POD.get().defaultBlockState();
            BlockPos first = new BlockPos(797, -58, 66);
            // посадка зерном: щелчок по нижней грани бревна
            ItemStack bean = thaumcraft.common.items.ManaBeanItem.of(ModItems.MANA_BEAN.get(), thaumcraft.api.aspects.Aspect.FIRE);
            player.setItemInHand(InteractionHand.MAIN_HAND, bean);
            bean.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(first), Direction.DOWN, first.above(), false)));
            boolean planted = level.getBlockState(first).is(ModBlocks.MANA_POD.get())
                    && level.getBlockEntity(first) instanceof thaumcraft.common.blocks.ManaPodBlock.Pod p && p.aspect == thaumcraft.api.aspects.Aspect.FIRE;
            // рядом — стручок воздуха: на третьей стадии они скрещиваются
            BlockPos second = first.east();
            level.setBlock(second, pod, 3);
            ((thaumcraft.common.blocks.ManaPodBlock.Pod) level.getBlockEntity(second)).aspect = thaumcraft.api.aspects.Aspect.AIR;
            // и одинокий стручок без аспекта
            BlockPos lone = new BlockPos(803, -58, 66);
            level.setBlock(lone, pod, 3);
            for (int i = 0; i < 4000; i++) {
                for (BlockPos pos : new BlockPos[]{first, second, lone}) {
                    level.getBlockState(pos).randomTick(level, pos, level.random);
                }
            }
            thaumcraft.common.blocks.ManaPodBlock.Pod a = (thaumcraft.common.blocks.ManaPodBlock.Pod) level.getBlockEntity(first);
            thaumcraft.common.blocks.ManaPodBlock.Pod b = (thaumcraft.common.blocks.ManaPodBlock.Pod) level.getBlockEntity(second);
            thaumcraft.common.blocks.ManaPodBlock.Pod c = (thaumcraft.common.blocks.ManaPodBlock.Pod) level.getBlockEntity(lone);
            java.util.Set<thaumcraft.api.aspects.Aspect> crossed = java.util.Set.of(thaumcraft.api.aspects.Aspect.FIRE,
                    thaumcraft.api.aspects.Aspect.AIR, thaumcraft.api.aspects.Aspect.combine(thaumcraft.api.aspects.Aspect.FIRE,
                            thaumcraft.api.aspects.Aspect.AIR));
            check("зерно маны сажается под бревном (" + planted + "); стручки выросли до зрелости (" + a.getAge() + ", " + b.getAge() + ", "
                            + c.getAge() + "), соседи скрестились (" + a.aspect.getTag() + ", " + b.aspect.getTag()
                            + "), одинокий получил свой аспект (" + (c.aspect == null ? "нет" : c.aspect.getTag()) + ")",
                    planted && a.getAge() == 7 && b.getAge() == 7 && c.getAge() == 7 && crossed.contains(a.aspect) && crossed.contains(b.aspect)
                            && c.aspect != null && (c.aspect.isPrimal() || c.aspect == thaumcraft.api.aspects.Aspect.PLANT));
            List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(lone), level, lone, c);
            boolean beans = !drops.isEmpty() && drops.size() <= 2 && thaumcraft.common.items.ManaBeanItem.getAspect(drops.get(0)) == c.aspect;
            thaumcraft.api.aspects.AspectList inBean = thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getAspects(drops.isEmpty() ? ItemStack.EMPTY : drops.get(0));
            check("зрелый стручок даёт одно-два зерна своего аспекта (зёрен " + drops.size() + "), и этот аспект виден в зерне",
                    beans && inBean.getAmount(c.aspect) >= 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            // вишрум дурманит
            BlockPos shroom = new BlockPos(800, -60, 63);
            level.setBlock(shroom, ModBlocks.VISHROOM.get().defaultBlockState(), 3);
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(800.5, -60, 63.5, 0f, 0f);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            level.getBlockState(shroom).entityInside(level, shroom, pig);
            check("вишрум дурманит того, кто через него идёт, и светится",
                    pig.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION) && level.getBlockState(shroom).getLightEmission(level, shroom) == 8);
            pig.discard();
            // все рецепты зачарования наполнением загрузились
            int enchants = 0;
            for (var recipe : level.getRecipeManager().getAllRecipesFor(thaumcraft.common.registry.ModRecipes.INFUSION.get())) {
                if (recipe instanceof thaumcraft.common.crafting.InfusionEnchantRecipe) {
                    enchants++;
                }
            }
            check("загружены все 24 рецепта зачарования наполнением (" + enchants + ")", enchants == 24);
        });
        cmd("tp @s 800.5 -59 62.5 0 -15");
        shot(30, "leftovers_mana_pods");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos lone = new BlockPos(803, -58, 66);
            level.removeBlock(lone.above(), false);
        });
        server(10, (server, player) -> check("без бревна над собой стручок отваливается",
                player.serverLevel().getBlockState(new BlockPos(803, -58, 66)).isAir()));
        cmd("fill 796 -60 63 804 -57 66 minecraft:air");
        cmd("kill @e[type=item]");
        cmd("tp @s 800.5 -60 0.5 0 5");
        // --- висп: аспект, молния по игроку, эфирная эссенция ---
        cmd("gamemode survival");
        cmd("tp @s 800.5 -60 0.5 0 -10");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.setHealth(20f);
            // висп за девять секунд способен убить игрока, и тогда весь остаток прогона идёт прахом: даём стойкость
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 400, 3));
            thaumcraft.common.entity.Wisp wisp = thaumcraft.common.registry.ModEntities.WISP.get().create(level);
            wisp.moveTo(800.5, -57, 5.5, 0f, 0f);
            wisp.setPersistenceRequired();
            level.addFreshEntity(wisp);
            // удар игрока заставляет виспа ответить
            wisp.hurt(level.damageSources().playerAttack(player), 1f);
        });
        shot(30, "leftovers_wisp");
        // удар виспа ловим всё время ожидания: обидчик помнится лишь пять секунд, а под стойкостью здоровье успевает восстановиться
        boolean[] struck = new boolean[1];
        for (int t = 0; t < 17; t++) {
            server(10, (server, player) -> struck[0] |= player.getHealth() < 20f
                    || player.getLastHurtByMob() instanceof thaumcraft.common.entity.Wisp);
        }
        server(1, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var wisps = level.getEntitiesOfClass(thaumcraft.common.entity.Wisp.class, box[0].inflate(30));
            thaumcraft.api.aspects.Aspect aspect = wisps.isEmpty() ? null : wisps.get(0).getAspect();
            thaumcraft.api.aspects.AspectList scanned = wisps.isEmpty() ? new thaumcraft.api.aspects.AspectList()
                    : thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getEntityAspects(wisps.get(0));
            check("висп получил аспект (" + (aspect == null ? "нет" : aspect.getTag()) + "), бьёт задевшего его игрока молнией (здоровье "
                            + player.getHealth() + " из 20) и состоит из своего аспекта, магии и воздуха",
                    // здоровье могло успеть восстановиться, поэтому засчитывается и сам факт удара виспа
                    aspect != null && struck[0]
                            && scanned.getAmount(aspect) >= 2
                            && scanned.getAmount(thaumcraft.api.aspects.Aspect.MAGIC) >= 1);
            player.setHealth(20f);
            for (var wisp : wisps) {
                wisp.hurt(level.damageSources().playerAttack(player), 1000f);
            }
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean essence = false;
            for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box[0].inflate(30))) {
                if (thaumcraft.common.items.WispEssenceItem.getAspect(item.getItem()) != null
                        && thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getAspects(item.getItem())
                        .getAmount(thaumcraft.common.items.WispEssenceItem.getAspect(item.getItem())) >= 2) {
                    essence = true;
                }
            }
            boolean inInventory = false;
            for (ItemStack stack : player.getInventory().items) {
                inInventory |= thaumcraft.common.items.WispEssenceItem.getAspect(stack) != null;
            }
            check("из убитого виспа выпала эфирная эссенция с двумя единицами его аспекта", essence || inInventory);
            player.getInventory().clearContent();
            boolean rule = false;
            for (var data : level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .getOrThrow(thaumcraft.common.world.ModBiomes.MAGICAL_FOREST).getMobSettings()
                    .getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap()) {
                rule |= data.type == thaumcraft.common.registry.ModEntities.WISP.get();
            }
            check("виспы значатся среди обитателей волшебного леса", rule);
        });
        cmd("gamemode creative");
        purge(centre);
        cmd("kill @e[type=item]");
        // --- печи: три вида, подбор, приручение, торг, злость ---
        int[] pechId = new int[1];
        cmd("tp @s 800.5 -60 60.5 0 0");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.getInventory().clearContent();
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.WOOD,
                    thaumcraft.api.wands.WandCap.IRON, false);
            thaumcraft.common.items.wands.WandItem.setFocus(wand, new ItemStack(ModItems.FOCUS_PECH.get()));
            ItemStack[] tools = {new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE), wand, new ItemStack(net.minecraft.world.item.Items.BOW)};
            StringBuilder names = new StringBuilder();
            boolean aspects = true;
            for (int type = 0; type < 3; type++) {
                thaumcraft.common.entity.Pech pech = thaumcraft.common.registry.ModEntities.PECH.get().create(level);
                pech.moveTo(798.5 + type * 2, -60, 64.5, 180f, 0f);
                pech.setYBodyRot(180f);
                pech.setYHeadRot(180f);
                pech.setPechType(type);
                pech.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, tools[type]);
                pech.setNoAi(true);
                pech.setPersistenceRequired();
                level.addFreshEntity(pech);
                names.append(pech.getName().getString()).append("; ");
                thaumcraft.api.aspects.AspectList scanned = thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getEntityAspects(pech);
                aspects &= scanned.getAmount(thaumcraft.api.aspects.Aspect.EXCHANGE) == 2
                        && scanned.getAmount(thaumcraft.api.aspects.Aspect.MAGIC) == (type == 2 ? 4 : 2);
            }
            check("три вида печей с разными именами и аспектами: " + names, aspects && names.toString().split("; ").length == 3
                    && !names.toString().contains("entity."));
        });
        shot(30, "leftovers_pechs");
        purge(centre.offset(0, 0, 56));
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.entity.Pech pech = thaumcraft.common.registry.ModEntities.PECH.get().create(level);
            pech.moveTo(800.5, -60, 66.5, 180f, 0f);
            pech.setPersistenceRequired();
            level.addFreshEntity(pech);
            pechId[0] = pech.getId();
            // булыжник рядом печ должен подобрать сам и унести в тюк
            net.minecraft.world.entity.item.ItemEntity stone = new net.minecraft.world.entity.item.ItemEntity(level, 800.5, -60, 69.5,
                    new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8));
            stone.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(stone);
            boolean plain = !thaumcraft.common.entity.Pech.isValued(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
            int emerald = thaumcraft.common.entity.Pech.getValue(new ItemStack(net.minecraft.world.item.Items.EMERALD));
            int bean = thaumcraft.common.entity.Pech.getValue(thaumcraft.common.items.ManaBeanItem.of(ModItems.MANA_BEAN.get(), thaumcraft.api.aspects.Aspect.FIRE));
            check("печ ценит изумруд в 5, зерно маны в 1, а булыжник не ценит вовсе (" + emerald + ", " + bean + ", " + plain + ")",
                    emerald == 5 && bean == 1 && plain);
        });
        server(160, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.entity.Pech pech = (thaumcraft.common.entity.Pech) level.getEntity(pechId[0]);
            int stones = pech.loot.countItem(net.minecraft.world.item.Items.COBBLESTONE);
            check("печ сам подобрал лежавший рядом булыжник в тюк (в тюке " + stones + " из 8)", stones == 8);
            // подарки: каждый изумруд располагает печа с шансом один к двум
            ItemStack emeralds = new ItemStack(net.minecraft.world.item.Items.EMERALD, 64);
            int given = 0;
            while (!pech.isTamed() && !emeralds.isEmpty()) {
                pech.pickupItem(emeralds);
                given++;
            }
            check("печ принял изумруды и перестал бояться игрока (ушло изумрудов " + given + ", приручен " + pech.isTamed() + ")",
                    pech.isTamed() && given >= 1 && emeralds.getCount() == 64 - given);
            // торг: за алмаз печ выдаёт товары, а булыжник ничего не стоит
            thaumcraft.common.menu.PechMenu menu = new thaumcraft.common.menu.PechMenu(0, player.getInventory(), pech);
            menu.getSlot(0).set(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
            boolean junk = menu.canRoll();
            int deals = 0;
            int goods = 0;
            StringBuilder got = new StringBuilder();
            for (int round = 0; round < 6; round++) {
                menu.getSlot(0).set(new ItemStack(net.minecraft.world.item.Items.DIAMOND));
                if (!menu.canRoll()) {
                    break;
                }
                menu.clickMenuButton(player, 0);
                if (menu.getSlot(0).getItem().isEmpty()) {
                    deals++;
                }
                for (int i = 1; i < 5; i++) {
                    ItemStack out = menu.getSlot(i).getItem();
                    if (!out.isEmpty()) {
                        goods += out.getCount();
                        got.append(out.getCount()).append(' ').append(out.getItem()).append(", ");
                        menu.getSlot(i).set(ItemStack.EMPTY);
                    }
                }
                pech.setTamed(true);
            }
            check("торг с печом: булыжник не принимается (" + !junk + "), за шесть алмазов шесть сделок (" + deals + ") и товары: " + got,
                    !junk && deals == 6 && goods >= 6);
            menu.removed(player);
            check("после закрытия окна печ снова свободен", !pech.trading);
        });
        // окно торга на экране: игрок подходит к печу вплотную
        server(5, (server, player) -> {
            net.minecraft.world.entity.Entity pech = player.serverLevel().getEntity(pechId[0]);
            ((thaumcraft.common.entity.Pech) pech).setNoAi(true);
            player.teleportTo(player.serverLevel(), pech.getX(), pech.getY(), pech.getZ() - 2.0, 0f, 10f);
        });
        client(10, mc -> {
            net.minecraft.world.entity.Entity pech = mc.level.getEntity(pechId[0]);
            if (pech != null) {
                mc.gameMode.interact(mc.player, pech, InteractionHand.MAIN_HAND);
            }
        });
        client(15, mc -> {
            boolean open = mc.screen instanceof thaumcraft.client.gui.PechScreen;
            check("щелчок по приручённому печу открывает окно торга", open);
        });
        server(2, (server, player) -> {
            if (player.containerMenu instanceof thaumcraft.common.menu.PechMenu menu) {
                menu.getSlot(0).set(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 3));
            }
        });
        shot(10, "leftovers_pech_trade");
        close();
        // злость: удар по одному печу ссорит игрока со всеми, маг и искатель стреляют
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.entity.Pech first = (thaumcraft.common.entity.Pech) level.getEntity(pechId[0]);
            thaumcraft.common.entity.Pech mage = thaumcraft.common.registry.ModEntities.PECH.get().create(level);
            mage.moveTo(804.5, -60, 66.5, 180f, 0f);
            mage.setPechType(thaumcraft.common.entity.Pech.MAGE);
            mage.setTamed(true);
            level.addFreshEntity(mage);
            thaumcraft.common.entity.Pech stalker = thaumcraft.common.registry.ModEntities.PECH.get().create(level);
            stalker.moveTo(796.5, -60, 66.5, 180f, 0f);
            stalker.setPechType(thaumcraft.common.entity.Pech.STALKER);
            stalker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.BOW));
            level.addFreshEntity(stalker);
            first.hurt(level.damageSources().playerAttack(player), 1f);
            boolean angry = first.getAnger() > 0 && mage.getAnger() > 0 && stalker.getAnger() > 0 && !first.isTamed() && !mage.isTamed();
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(800.5, -60, 72.5, 0f, 0f);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            mage.performRangedAttack(pig, 1f);
            stalker.performRangedAttack(pig, 1f);
            net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(centre.offset(0, 0, 58)).inflate(16);
            int blasts = level.getEntitiesOfClass(thaumcraft.common.entity.PechBlastEntity.class, area).size();
            int arrows = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class, area).size();
            check("удар по печу разозлил всех печей вокруг и отменил приручение (" + angry + "); маг пускает проклятие (" + blasts
                    + "), искатель — стрелу (" + arrows + ")", angry && blasts == 1 && arrows == 1);
        });
        purge(centre.offset(0, 0, 56));
        cmd("kill @e[type=item]");
        cmd("tp @s 800.5 -60 0.5 0 5");
        // --- зачарование наполнением: подбор рецепта и цена уровней ---
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            knowledge.completeResearch("INFUSIONENCHANTMENT");
            ItemStack salt = new ItemStack(ModItems.SALIS_MUNDUS.get());
            ItemStack sword = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            ItemStack bow = new ItemStack(net.minecraft.world.item.Items.BOW);
            List<ItemStack> sharp = List.of(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD), salt);
            List<ItemStack> push = List.of(new ItemStack(net.minecraft.world.item.Items.PISTON), salt);
            thaumcraft.api.aspects.Aspect weapon = thaumcraft.api.aspects.Aspect.byTag("telum");
            var first = thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, sword, sharp);
            ItemStack once = first == null ? ItemStack.EMPTY : first.getResult(sword);
            var second = thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, once, sharp);
            ItemStack maxed = sword.copy();
            maxed.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 5);
            ItemStack smiting = sword.copy();
            smiting.enchant(net.minecraft.world.item.enchantment.Enchantments.SMITE, 1);
            var onSword = thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, sword, push);
            var onBow = thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, bow, push);
            String swordEnch = onSword instanceof thaumcraft.common.crafting.InfusionEnchantRecipe r ? r.getEnchantment().getDescriptionId() : "нет";
            String bowEnch = onBow instanceof thaumcraft.common.crafting.InfusionEnchantRecipe r ? r.getEnchantment().getDescriptionId() : "нет";
            check("зачарование наполнением: меч получает остроту I за " + (first == null ? -1 : first.getAspects(sword).getAmount(weapon))
                            + " оружия и " + (first == null ? -1 : first.getXpCost(sword)) + " ур. опыта, остроту II — за "
                            + (second == null ? -1 : second.getAspects(once).getAmount(weapon)) + " и " + (second == null ? -1 : second.getXpCost(once))
                            + "; предел и несовместимое зачарование не проходят; поршень даёт мечу " + swordEnch + ", луку " + bowEnch,
                    first != null && once.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS) == 1
                            && first.getAspects(sword).getAmount(weapon) == 8 && first.getXpCost(sword) == 1
                            && second != null && second.getAspects(once).getAmount(weapon) == 16 && second.getXpCost(once) == 2
                            && second.getResult(once).getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS) == 2
                            && thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, maxed, sharp) == null
                            && thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, smiting, sharp) == null
                            && thaumcraft.common.crafting.InfusionRecipe.find(level, knowledge, salt, sharp) == null
                            && swordEnch.equals("enchantment.minecraft.knockback") && bowEnch.equals("enchantment.minecraft.punch"));
        });
        // --- то же в настоящем алтаре: с платой опытом и с искажением за «тёмный» предмет в конце не связано ---
        BlockPos altar = new BlockPos(800, -60, 40);
        cmd("tp @s 800.5 -59 31.5 0 10");
        cmd("setblock 800 -60 40 thaumcraft:pedestal");
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                cmd("setblock " + (800 + dx) + " -60 " + (40 + dz) + " thaumcraft:arcane_stone_bricks");
                cmd("setblock " + (800 + dx) + " -59 " + (40 + dz) + " thaumcraft:arcane_stone");
            }
        }
        cmd("setblock 800 -58 40 thaumcraft:infusion_matrix");
        cmd("setblock 803 -60 40 thaumcraft:pedestal");
        cmd("setblock 797 -60 40 thaumcraft:pedestal");
        cmd("setblock 799 -60 45 thaumcraft:warded_jar{Aspect:\"telum\",Amount:64s}");
        cmd("setblock 801 -60 45 thaumcraft:warded_jar{Aspect:\"praecantatio\",Amount:64s}");
        // черепа симметричными рядами гасят нестабильность, чтобы исход не зависел от случая
        cmd("fill 794 -60 34 806 -60 34 minecraft:skeleton_skull");
        cmd("fill 794 -60 46 806 -60 46 minecraft:skeleton_skull");
        cmd("xp set @s 5 levels");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar))
                    .setItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
            ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar.offset(3, 0, 0)))
                    .setItem(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar.offset(-3, 0, 0)))
                    .setItem(new ItemStack(ModItems.SALIS_MUNDUS.get()));
            thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix =
                    (thaumcraft.common.blockentity.InfusionMatrixBlockEntity) level.getBlockEntity(altar.above(2));
            boolean woke = matrix.wake();
            matrix.onWand(player);
            check("алтарь для зачарования собран и запущен (пробуждён " + woke + ", идёт вливание " + matrix.isCrafting()
                    + ", нестабильность " + matrix.getInstability() + ")", woke && matrix.isCrafting() && matrix.getInstability() <= 0);
        });
        shot(60, "leftovers_enchant_altar");
        server(400, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ItemStack out = ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar)).getItem();
            thaumcraft.common.blockentity.InfusionMatrixBlockEntity matrix =
                    (thaumcraft.common.blockentity.InfusionMatrixBlockEntity) level.getBlockEntity(altar.above(2));
            int sharpness = out.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
            boolean used = ((thaumcraft.common.blockentity.PedestalBlockEntity) level.getBlockEntity(altar.offset(3, 0, 0))).getItem().isEmpty();
            check("алтарь зачаровал меч на остроту " + sharpness + ", забрав уровень опыта (осталось " + player.experienceLevel
                            + " из 5) и предметы с пьедесталов (" + used + "); вливание окончено (" + !matrix.isCrafting() + ")",
                    sharpness == 1 && player.experienceLevel == 4 && used && !matrix.isCrafting());
        });
        cmd("fill 794 -60 34 806 -57 47 minecraft:air");
        cmd("xp set @s 0 levels");
        cmd("tp @s 800.5 -60 0.5 0 5");
        // --- «Починка» и «Спешка» ---
        cmd("gamemode survival");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            player.getInventory().clearContent();
            ItemStack pick = new ItemStack(ModItems.THAUMIUM_PICKAXE.get());
            pick.enchant(thaumcraft.common.registry.ModEnchantments.REPAIR.get(), 2);
            pick.setDamageValue(100);
            ItemStack plain = new ItemStack(ModItems.THAUMIUM_PICKAXE.get());
            plain.setDamageValue(100);
            ItemStack iron = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
            player.getInventory().setItem(0, pick);
            player.getInventory().setItem(1, plain);
            player.getInventory().setItem(8, thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, true));
            boolean rules = thaumcraft.common.registry.ModEnchantments.REPAIR.get().canEnchant(pick)
                    && !thaumcraft.common.registry.ModEnchantments.REPAIR.get().canEnchant(iron)
                    && thaumcraft.common.registry.ModEnchantments.HASTE.get().canEnchant(new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS))
                    && !thaumcraft.common.registry.ModEnchantments.HASTE.get().canEnchant(new ItemStack(net.minecraft.world.item.Items.IRON_HELMET))
                    && !thaumcraft.common.registry.ModEnchantments.REPAIR.get().isCompatibleWith(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
            check("«Починка» ложится только на вещи мода и не дружит с «Прочностью», «Спешка» — только на обувь", rules);
        });
        server(130, (server, player) -> {
            ItemStack pick = player.getInventory().getItem(0);
            ItemStack plain = player.getInventory().getItem(1);
            int vis = 0;
            for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                vis += thaumcraft.common.items.wands.WandItem.getVis(player.getInventory().getItem(8), aspect);
            }
            int full = 6 * thaumcraft.common.items.wands.WandItem.getMaxVis(player.getInventory().getItem(8));
            check("кирка с «Починкой II» сама чинится за вис из жезла (износ 100 -> " + pick.getDamageValue() + ", вис потрачено "
                            + (full - vis) + " сотых), кирка без неё — нет (износ " + plain.getDamageValue() + ")",
                    pick.getDamageValue() < 100 && pick.getDamageValue() >= 90 && plain.getDamageValue() == 100 && vis < full);
            player.getInventory().clearContent();
        });
        cmd("gamemode creative");
        // --- искажение за создание «тёмных» предметов ---
        server(10, (server, player) -> {
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            int before = knowledge.getWarpSticky();
            thaumcraft.common.research.Warp.onCrafted(player, new ItemStack(ModItems.ARCANE_STONE_SLAB.get()));
            int plain = knowledge.getWarpSticky();
            net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player, new ItemStack(ModItems.BOTTLE_TAINT.get()),
                    new net.minecraft.world.SimpleContainer(1));
            int dark = knowledge.getWarpSticky();
            check("создание порчи в бутылке даёт единицу липкого искажения, а обычного предмета — нет (" + before + " -> " + plain + " -> " + dark + ")",
                    plain == before && dark == before + 1);
            thaumcraft.common.research.Warp.addSticky(player, -1);
        });
        // --- ступени и плиты ---
        cmd("fill 796 -60 12 804 -58 14 minecraft:air");
        cmd("setblock 797 -60 13 thaumcraft:greatwood_stairs[facing=north]");
        cmd("setblock 798 -60 13 thaumcraft:greatwood_slab");
        cmd("setblock 799 -60 13 thaumcraft:silverwood_stairs[facing=north]");
        cmd("setblock 800 -60 13 thaumcraft:silverwood_slab");
        cmd("setblock 801 -60 13 thaumcraft:arcane_stone_stairs[facing=north]");
        cmd("setblock 802 -60 13 thaumcraft:arcane_stone_slab");
        cmd("setblock 803 -60 13 thaumcraft:greatwood_slab[type=double]");
        cmd("tp @s 800.0 -59 10.0 0 30");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"greatwood_stairs", "silverwood_stairs", "arcane_stone_stairs", "greatwood_slab", "silverwood_slab"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()
                        || !level.getBlockState(new BlockPos(797, -60, 13)).is(ModBlocks.GREATWOOD_STAIRS.get())) {
                    missing.append(' ').append(name);
                }
            }
            check("ступени и плиты из большого и серебряного дерева и магического камня ставятся и крафтятся (нет:" + missing + ")",
                    missing.length() == 0);
        });
        shot(20, "leftovers_stairs");
        cmd("fill 796 -60 12 804 -58 14 minecraft:air");

        // сальные свечи всех цветов, таумометр и чернила
        cmd("fill 792 -60 13 807 -60 13 minecraft:air");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (int color = 0; color < 16; color++) {
                level.setBlockAndUpdate(new BlockPos(792 + color, -60, 13), ModBlocks.TALLOW_CANDLES.get(color).get().defaultBlockState());
            }
            // одна свеча без опоры: должна осыпаться
            level.setBlockAndUpdate(new BlockPos(800, -58, 14), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(800, -57, 14), ModBlocks.TALLOW_CANDLES.get(0).get().defaultBlockState());
            level.destroyBlock(new BlockPos(800, -58, 14), false);
        });
        cmd("tp @s 800.0 -59 9.5 0 25");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"thaumometer", "scribing_tools_phial", "tallow_candle_white", "tallow_candle_red",
                    "tallow_candle_black", "tallow_candle_bleach"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты таумометра, чернил из флакона и сальных свечей загружены (нет:" + missing + ")", missing.length() == 0);
            int placed = 0;
            for (int color = 0; color < 16; color++) {
                BlockPos pos = new BlockPos(792 + color, -60, 13);
                var state = level.getBlockState(pos);
                if (state.getBlock() instanceof thaumcraft.common.blocks.TallowCandleBlock && state.getLightEmission(level, pos) == 14
                        && thaumcraft.common.blockentity.InfusionMatrixBlockEntity.isStabiliser(state)) {
                    placed++;
                }
            }
            check("сальные свечи 16 цветов стоят, светят на 14 и стабилизируют наполнение (" + placed + ")", placed == 16);
            check("свеча без опоры осыпается", level.getBlockState(new BlockPos(800, -57, 14)).isAir());
        });
        shot(20, "leftovers_candles");
        cmd("fill 792 -60 13 807 -57 14 minecraft:air");
        cmd("kill @e[type=item,distance=..30]");

        // банка пустоты, резонатор, мыло здравомыслия и эфирный цветок
        BlockPos bloomPos = new BlockPos(800, -60, 60);
        int[] taintedAtStart = new int[1];
        cmd("tp @s 800.5 -60 56.5 0 10");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"void_jar", "resonator", "crucible/sanity_soap", "crucible/ethereal_bloom"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты банки пустоты, резонатора, мыла и эфирного цветка загружены (нет:" + missing + ")", missing.length() == 0);

            BlockPos voidPos = bloomPos.offset(-2, 0, 0);
            BlockPos jarPos = bloomPos.offset(2, 0, 0);
            level.setBlockAndUpdate(voidPos, ModBlocks.VOID_JAR.get().defaultBlockState());
            level.setBlockAndUpdate(jarPos, ModBlocks.WARDED_JAR.get().defaultBlockState());
            var voidJar = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(voidPos);
            var jar = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(jarPos);
            int voidLeft = voidJar.add(thaumcraft.api.aspects.Aspect.FIRE, 100) + voidJar.add(thaumcraft.api.aspects.Aspect.FIRE, 10);
            int jarLeft = jar.add(thaumcraft.api.aspects.Aspect.FIRE, 100);
            check("банка пустоты глотает излишек (осталось " + voidLeft + ", в банке " + voidJar.getAmount() + "), обычная возвращает (" + jarLeft
                            + "); полная пустотная всё ещё тянет (" + voidJar.getSuctionAmount(net.minecraft.core.Direction.UP) + "), обычная нет ("
                            + jar.getSuctionAmount(net.minecraft.core.Direction.UP) + ")",
                    voidLeft == 0 && voidJar.getAmount() == 64 && jarLeft == 36 && voidJar.getSuctionAmount(net.minecraft.core.Direction.UP) == 32
                            && jar.getSuctionAmount(net.minecraft.core.Direction.UP) == 0);

            // резонатор по банке
            ItemStack resonator = new ItemStack(ModItems.RESONATOR.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, resonator);
            var result = resonator.getItem().onItemUseFirst(resonator, new net.minecraft.world.item.context.UseOnContext(player,
                    InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(voidPos),
                    net.minecraft.core.Direction.UP, voidPos, false)));
            var miss = resonator.getItem().onItemUseFirst(resonator, new net.minecraft.world.item.context.UseOnContext(player,
                    InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(voidPos.below()),
                    net.minecraft.core.Direction.UP, voidPos.below(), false)));
            check("резонатор отзывается на сосуд с эссенцией (" + result + ") и молчит на простом блоке (" + miss + ")",
                    result == net.minecraft.world.InteractionResult.SUCCESS && miss == net.minecraft.world.InteractionResult.PASS);

            // мыло: временное искажение смывается целиком
            var knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            thaumcraft.common.research.Warp.add(player, 5, true);
            int before = knowledge.getWarpTemp();
            ItemStack soap = new ItemStack(ModItems.SANITY_SOAP.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, soap);
            soap.getItem().releaseUsing(soap, level, player, 100);
            int early = knowledge.getWarpTemp();
            soap.getItem().releaseUsing(soap, level, player, 0);
            check("мыло здравомыслия: брошенное на полпути не действует (" + before + " -> " + early + "), домытое смывает временное искажение ("
                    + knowledge.getWarpTemp() + ") и тратится (" + soap.getCount() + ")", before >= 5 && early == before
                    && knowledge.getWarpTemp() == 0 && soap.getCount() == 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            // эфирный цветок посреди порчи
            for (int dx = -8; dx <= 8; dx += 4) {
                for (int dz = -8; dz <= 8; dz += 4) {
                    thaumcraft.common.world.ModBiomes.set(level, bloomPos.offset(dx, 0, dz), thaumcraft.common.taint.Taint.BIOME);
                }
            }
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (thaumcraft.common.world.ModBiomes.is(level, bloomPos.offset(dx, 0, dz), thaumcraft.common.taint.Taint.BIOME)) {
                        taintedAtStart[0]++;
                    }
                }
            }
            level.setBlockAndUpdate(bloomPos, ModBlocks.ETHEREAL_BLOOM.get().defaultBlockState());
        });
        shot(140, "leftovers_bloom");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int taintedBefore = 0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (thaumcraft.common.world.ModBiomes.is(level, bloomPos.offset(dx, 0, dz), thaumcraft.common.taint.Taint.BIOME)) {
                        taintedBefore++;
                    }
                }
            }
            boolean alive = level.getBlockEntity(bloomPos) instanceof thaumcraft.common.blocks.EtherealBloomBlock.Bloom;
            if (alive) {
                var bloom = (thaumcraft.common.blocks.EtherealBloomBlock.Bloom) level.getBlockEntity(bloomPos);
                // сорок минут работы цветка разом
                for (int tick = 0; tick < 48000; tick++) {
                    thaumcraft.common.blocks.EtherealBloomBlock.Bloom.tick(level, bloomPos, level.getBlockState(bloomPos), bloom);
                }
            }
            int taintedAfter = 0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (thaumcraft.common.world.ModBiomes.is(level, bloomPos.offset(dx, 0, dz), thaumcraft.common.taint.Taint.BIOME)) {
                        taintedAfter++;
                    }
                }
            }
            check("эфирный цветок стоит (" + alive + "), светит (" + level.getBlockState(bloomPos).getLightEmission(level, bloomPos)
                    + ") и вычищает порчу вокруг себя (порченых столбцов было " + taintedAtStart[0] + ", за семь секунд осталось " + taintedBefore + ", в итоге " + taintedAfter + ")",
                    alive && level.getBlockState(bloomPos).getLightEmission(level, bloomPos) == 15 && taintedAtStart[0] == 81 && taintedAfter == 0);
        });
        shot(20, "leftovers_bloom_clean");
        cmd("fill 796 -60 58 804 -58 62 minecraft:air");

        // жидкая смерть, очищающая жидкость, соли для ванн и магическая купальня
        BlockPos spaPos = new BlockPos(840, -60, 60);
        BlockPos deathPos = new BlockPos(830, -60, 60);
        BlockPos waterPos = new BlockPos(850, -61, 60);
        cmd("fill 837 -60 57 843 -60 63 minecraft:stone_bricks");
        cmd("fill 837 -59 57 843 -59 63 minecraft:glass");
        cmd("fill 838 -59 58 842 -59 62 minecraft:air");
        cmd("setblock 840 -60 60 thaumcraft:arcane_spa");
        cmd("setblock 850 -61 60 minecraft:water");
        cmd("tp @s 840.5 -55 52.5 0 35");
        net.minecraft.world.entity.Entity[] victim = new net.minecraft.world.entity.Entity[1];
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            StringBuilder missing = new StringBuilder();
            for (String name : new String[]{"arcane_spa", "crucible/bath_salts", "crucible/bucket_death"}) {
                if (level.getRecipeManager().byKey(Thaumcraft.id(name)).isEmpty()) {
                    missing.append(' ').append(name);
                }
            }
            check("рецепты купальни, солей для ванн и жидкой смерти загружены (нет:" + missing + ")", missing.length() == 0);

            // ведро смерти выливается полным блоком
            ItemStack bucket = new ItemStack(ModItems.BUCKET_DEATH.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
            bucket.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(deathPos.below()),
                            net.minecraft.core.Direction.UP, deathPos.below(), false)));
            var poured = level.getBlockState(deathPos);
            check("ведро жидкой смерти выливает полный блок (" + poured + ")", poured.is(ModBlocks.LIQUID_DEATH.get())
                    && poured.getValue(thaumcraft.common.blocks.FluxBlock.LEVEL) == 7);
            // и обратно: полный блок зачерпывается пустым ведром (луч проходит сквозь жидкость и упирается в землю под ней)
            var scooped = net.minecraftforge.event.ForgeEventFactory.onBucketUse(player, level, new ItemStack(net.minecraft.world.item.Items.BUCKET),
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(deathPos.below()),
                            net.minecraft.core.Direction.UP, deathPos.below(), false));
            check("полный блок жидкой смерти зачерпывается пустым ведром (" + (scooped == null ? "нет" : scooped.getObject()) + ", блок стал "
                    + level.getBlockState(deathPos) + ")", scooped != null && scooped.getResult().consumesAction()
                    && scooped.getObject().is(player.getAbilities().instabuild ? net.minecraft.world.item.Items.BUCKET : ModItems.BUCKET_DEATH.get())
                    && level.getBlockState(deathPos).isAir());
            level.setBlockAndUpdate(deathPos, ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(thaumcraft.common.blocks.FluxBlock.LEVEL, 7));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var cow = net.minecraft.world.entity.EntityType.COW.create(level);
            // корова в яме с полным блоком смерти (без ИИ существа не замечают, в чём стоят)
            BlockPos pit = deathPos.offset(0, -1, 5);
            level.setBlockAndUpdate(pit, ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(thaumcraft.common.blocks.FluxBlock.LEVEL, 7));
            cow.moveTo(pit.getX() + 0.5, pit.getY(), pit.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(cow);
            victim[0] = cow;

            // соли в воде
            var salts = new net.minecraft.world.entity.item.ItemEntity(level, waterPos.getX() + 0.5, waterPos.getY() + 0.3, waterPos.getZ() + 0.5,
                    new ItemStack(ModItems.BATH_SALTS.get()));
            salts.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(salts);
            check("брошенные соли для ванн лежат всего десять секунд (" + salts.lifespan + ")", salts.lifespan == 200);

            // купальня: вода и соли
            var spa = (thaumcraft.common.blocks.ArcaneSpaBlock.Spa) level.getBlockEntity(spaPos);
            spa.tank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            spa.setItem(0, new ItemStack(ModItems.BATH_SALTS.get(), 8));
        });
        server(60, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int quanta = 0;
            for (BlockPos pos : BlockPos.betweenClosed(deathPos.offset(-6, 0, -6), deathPos.offset(6, 0, 6))) {
                var state = level.getBlockState(pos);
                if (state.is(ModBlocks.LIQUID_DEATH.get())) {
                    quanta += state.getValue(thaumcraft.common.blocks.FluxBlock.LEVEL) + 1;
                }
            }
            var cow = (net.minecraft.world.entity.LivingEntity) victim[0];
            var source = cow.getLastDamageSource();
            check("жидкая смерть растекается, не прибывая и не убывая (долей " + quanta + "), и растворяет живое (здоровье коровы "
                            + cow.getHealth() + ", урон " + (source == null ? "нет" : source.getMsgId()) + ")",
                    quanta == 8 && cow.getHealth() < cow.getMaxHealth() && (source == null || source.getMsgId().equals("dissolve")));
        });
        shot(5, "leftovers_death");
        server(200, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean converted = level.getBlockState(waterPos).is(ModBlocks.PURIFYING_FLUID.get());
            check("соли для ванн, растворившись, превратили воду в очищающую жидкость (" + level.getBlockState(waterPos) + ")", converted);
            player.removeEffect(thaumcraft.common.registry.ModEffects.WARP_WARD.get());

            var spa = (thaumcraft.common.blocks.ArcaneSpaBlock.Spa) level.getBlockEntity(spaPos);
            int sources = 0;
            for (BlockPos pos : BlockPos.betweenClosed(spaPos.offset(-2, 1, -2), spaPos.offset(2, 1, 2))) {
                var state = level.getBlockState(pos);
                if (state.is(ModBlocks.PURIFYING_FLUID.get()) && state.getFluidState().isSource()) {
                    sources++;
                }
            }
            check("купальня смешивает воду с солями и разливает очищающую жидкость над собой (источников " + sources + ", в баке "
                            + spa.tank.getFluidAmount() + ", солей " + spa.getItem(0).getCount() + ")",
                    sources >= 4 && sources == 5 - spa.tank.getFluidAmount() / 1000 && spa.getItem(0).getCount() == 8 - sources);
            boolean before = spa.getMix();
            new thaumcraft.common.menu.SpaMenu(0, player.getInventory(), spa).clickMenuButton(player, 1);
            check("переключатель купальни меняет режим (" + before + " -> " + spa.getMix() + ")", before && !spa.getMix());
            spa.toggleMix();
            spa.tank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            spa.setItem(0, ItemStack.EMPTY);
            spa.setItem(0, new ItemStack(ModItems.BATH_SALTS.get(), 3));
            net.minecraftforge.network.NetworkHooks.openScreen(player, spa, spaPos);
        });
        shot(20, "leftovers_spa_gui");
        close();
        shot(10, "leftovers_spa");
        cmd("tp @s 850.5 -61 60.5 0 35");
        server(15, (server, player) -> {
            ServerLevel level = player.serverLevel();
            boolean warded = player.hasEffect(thaumcraft.common.registry.ModEffects.WARP_WARD.get());
            check("игрок, вошедший в очищающую жидкость, получает защиту от искажения (" + warded + "), а источник расходуется ("
                    + level.getBlockState(waterPos) + ")", warded && level.getBlockState(waterPos).isAir());
            player.removeEffect(thaumcraft.common.registry.ModEffects.WARP_WARD.get());
            if (victim[0] != null) {
                victim[0].discard();
            }
        });
        cmd("fill 824 -60 54 836 -60 66 minecraft:air");
        cmd("setblock 830 -61 65 minecraft:grass_block");
        cmd("fill 837 -60 57 843 -59 63 minecraft:air");
        cmd("setblock 850 -61 60 minecraft:grass_block");
        cmd("kill @e[type=item,distance=..40]");

        // стол разборки
        BlockPos deconPos = new BlockPos(860, -60, 60);
        cmd("setblock 860 -60 60 thaumcraft:deconstruction_table");
        cmd("tp @s 860.5 -59.5 58.2 0 40");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var table = (thaumcraft.common.blocks.DeconstructionTableBlock.Table) level.getBlockEntity(deconPos);
            table.setItem(0, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 64));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DECONSTRUCTION_TABLE.get()));
        });
        server(100, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var table = (thaumcraft.common.blocks.DeconstructionTableBlock.Table) level.getBlockEntity(deconPos);
            int left = table.getItem(0).getCount();
            boolean recipe = level.getRecipeManager().byKey(Thaumcraft.id("deconstruction_table")).isPresent();
            // добиваемся точки исследования: шанс зависит от аспектов предмета
            int tries = 0;
            while (table.aspect == null && tries++ < 5000) {
                if (table.getItem(0).isEmpty()) {
                    table.setItem(0, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 64));
                }
                table.breakItem();
            }
            var found = table.aspect;
            int stuck = table.getItem(0).getCount();
            table.breakItem();
            boolean waits = table.getItem(0).getCount() == stuck;
            var knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            int before = found == null ? 0 : knowledge.getPool(found);
            new thaumcraft.common.menu.DeconstructionTableMenu(0, player.getInventory(), deconPos).clickMenuButton(player, 1);
            int after = found == null ? 0 : knowledge.getPool(found);
            check("стол разборки: рецепт есть (" + recipe + "), за пять секунд разобрал предметы (осталось " + left + " из 64), добыл точку "
                            + "первичного аспекта (" + (found == null ? "нет" : found.getTag()) + " с " + tries + "-й попытки), с ней на столе не работает ("
                            + waits + "), игрок её забрал (" + before + " -> " + after + ")",
                    recipe && left < 64 && left >= 60 && found != null && found.isPrimal() && waits && after == before + 1 && table.aspect == null);
            table.setItem(0, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 64));
            tries = 0;
            while (table.aspect == null && tries++ < 5000) {
                table.breakItem();
                if (table.getItem(0).isEmpty()) {
                    table.setItem(0, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 64));
                }
            }
        });
        shot(20, "leftovers_decon");
        server(5, (server, player) -> net.minecraftforge.network.NetworkHooks.openScreen(player,
                (thaumcraft.common.blocks.DeconstructionTableBlock.Table) player.serverLevel().getBlockEntity(deconPos), deconPos));
        shot(20, "leftovers_decon_gui");
        close();
        server(1, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        cmd("setblock 860 -60 60 minecraft:air");
        cmd("kill @e[type=item,distance=..40]");

        // тауматорий и мнемоническая матрица
        BlockPos thaumPos = new BlockPos(880, -58, 60);
        cmd("setblock 880 -60 60 thaumcraft:nitor");
        cmd("setblock 880 -59 60 thaumcraft:crucible");
        cmd("setblock 880 -58 60 thaumcraft:alchemical_construct");
        cmd("setblock 880 -57 60 thaumcraft:alchemical_construct");
        cmd("setblock 880 -58 59 minecraft:chest");
        cmd("setblock 879 -58 60 thaumcraft:warded_jar");
        cmd("setblock 879 -57 60 thaumcraft:tube");
        cmd("tp @s 882.5 -59 55.5 35 5");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(), thaumcraft.api.wands.WandRod.SILVERWOOD,
                    thaumcraft.api.wands.WandCap.THAUMIUM, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            var click = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(thaumPos.above()),
                            net.minecraft.core.Direction.NORTH, thaumPos.above(), false));
            boolean unknown = knowledge.isResearchComplete("THAUMATORIUM");
            var early = unknown ? null : wand.useOn(click);
            boolean untouched = level.getBlockState(thaumPos).is(ModBlocks.ALCHEMICAL_CONSTRUCT.get());
            knowledge.completeResearch("THAUMATORIUM");
            knowledge.completeResearch("TALLOW");
            var result = wand.useOn(click);
            var state = level.getBlockState(thaumPos);
            var top = level.getBlockState(thaumPos.above());
            check("жезл собирает тауматорий из двух конструкций над тиглем: без исследования нет (" + early + ", " + untouched + "), с ним да ("
                            + result + ", " + state + ", верх " + top + ")",
                    (unknown || untouched) && state.is(ModBlocks.THAUMATORIUM.get())
                            && state.getValue(thaumcraft.common.blocks.ThaumatoriumBlock.FACING) == net.minecraft.core.Direction.NORTH
                            && top.is(ModBlocks.THAUMATORIUM.get()) && top.getValue(thaumcraft.common.blocks.ThaumatoriumBlock.TOP));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!(level.getBlockEntity(thaumPos) instanceof thaumcraft.common.blocks.ThaumatoriumBlock.Core core)) {
                return;
            }
            ((thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(new BlockPos(879, -58, 60)))
                    .add(thaumcraft.api.aspects.Aspect.MAGIC, 32);
            core.setItem(0, new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 3));
            var menu = new thaumcraft.common.menu.ThaumatoriumMenu(0, player.getInventory(), core);
            var all = thaumcraft.common.blocks.ThaumatoriumBlock.allRecipes(level);
            int tallow = -1;
            int alumentum = -1;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getId().equals(Thaumcraft.id("crucible/tallow"))) {
                    tallow = i;
                }
                if (all.get(i).getId().equals(Thaumcraft.id("crucible/alumentum"))) {
                    alumentum = i;
                }
            }
            boolean foreign = menu.clickMenuButton(player, alumentum);
            boolean chosen = menu.clickMenuButton(player, tallow);
            menu.removed(player);
            check("в окне тауматория виден рецепт сала для гнилой плоти (" + menu.recipes.size() + "), его можно задать (" + chosen
                    + ", задано " + core.recipes + "), а неподходящий к катализатору нельзя (" + foreign + ")",
                    chosen && !foreign && core.recipes.size() == 1);
            level.setBlockAndUpdate(thaumPos.east(), ModBlocks.MNEMONIC_MATRIX.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.MnemonicMatrixBlock.FACING, net.minecraft.core.Direction.WEST));
            level.setBlockAndUpdate(thaumPos.above().south(), ModBlocks.MNEMONIC_MATRIX.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.MnemonicMatrixBlock.FACING, net.minecraft.core.Direction.UP));
        });
        shot(160, "leftovers_thaumatorium");
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(thaumPos) instanceof thaumcraft.common.blocks.ThaumatoriumBlock.Core core)) {
                check("тауматорий уцелел", false);
                return;
            }
            var chest = (net.minecraft.world.Container) level.getBlockEntity(new BlockPos(880, -58, 59));
            int made = chest.countItem(ModItems.TALLOW.get());
            var jar = (thaumcraft.common.blockentity.EssentiaBlockEntity) level.getBlockEntity(new BlockPos(879, -58, 60));
            check("тауматорий на огне сам тянет эссенцию по трубе и варит сало в сундук перед собой (сала " + made + ", плоти осталось "
                            + core.getItem(0).getCount() + ", магии в банке " + jar.getAmount() + " из 32)",
                    made == 3 && core.getItem(0).isEmpty() && jar.getAmount() == 32 - 3 * 2);
            check("одна правильно приставленная мнемоническая матрица даёт тауматорию ещё два рецепта, повёрнутая не туда — нет ("
                    + core.maxRecipes + ")", core.maxRecipes == 3);
            core.setItem(0, new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH, 5));
            net.minecraftforge.network.NetworkHooks.openScreen(player, core, thaumPos);
        });
        shot(12, "leftovers_thaumatorium_gui");
        close();
        cmd("setblock 880 -59 60 minecraft:air");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            check("без тигля тауматорий распадается обратно на алхимические конструкции (" + level.getBlockState(thaumPos) + ", "
                            + level.getBlockState(thaumPos.above()) + ")",
                    level.getBlockState(thaumPos).is(ModBlocks.ALCHEMICAL_CONSTRUCT.get())
                            && level.getBlockState(thaumPos.above()).is(ModBlocks.ALCHEMICAL_CONSTRUCT.get()));
        });
        cmd("fill 878 -60 58 882 -56 62 minecraft:air");
        cmd("kill @e[type=item,distance=..40]");
        cmd("difficulty peaceful");
    }

    /** Обычный мир: как часто встречаются порченые земли и как выглядит естественный очаг. */
    private static void taintWorld() {
        cmd("gamemode creative");
        cmd("time set noon");
        cmd("weather clear");
        cmd("thaumcraft biomes");
        BlockPos[] found = new BlockPos[1];
        server(20, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var pair = level.findClosestBiome3d(holder -> holder.is(thaumcraft.common.taint.Taint.BIOME), player.blockPosition(), 6400, 32, 64);
            check("в обычном мире есть порченые земли в 6400 блоках от начала" + (pair == null ? "" : " (" + pair.getFirst().toShortString() + ")"),
                    pair != null);
            if (pair != null) {
                found[0] = pair.getFirst();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(level, found[0].getX() + 0.5, 180, found[0].getZ() + 0.5, 0f, 60f);
            }
        });
        server(300, (server, player) -> {
            if (found[0] == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            // от края биома — на 24 блока вглубь, в ту сторону, где порчи больше
            BlockPos best = found[0];
            int bestCount = -1;
            for (int dx = -32; dx <= 32; dx += 16) {
                for (int dz = -32; dz <= 32; dz += 16) {
                    int count = 0;
                    for (int ax = -16; ax <= 16; ax += 8) {
                        for (int az = -16; az <= 16; az += 8) {
                            BlockPos probe = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    found[0].offset(dx + ax, 0, dz + az));
                            if (thaumcraft.common.taint.Taint.isTainted(level, probe)) {
                                count++;
                            }
                        }
                    }
                    if (count > bestCount) {
                        bestCount = count;
                        best = found[0].offset(dx, 0, dz);
                    }
                }
            }
            found[0] = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, best);
            player.teleportTo(level, found[0].getX() + 0.5, found[0].getY() + 14, found[0].getZ() - 14.5, 0f, 40f);
        });
        server(200, (server, player) -> {
            if (found[0] == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            int fibres = 0;
            int crust = 0;
            int logs = 0;
            for (BlockPos pos : BlockPos.betweenClosed(found[0].offset(-24, -8, -24), found[0].offset(24, 24, 24))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.TAINT_FIBRES.get())) {
                    fibres++;
                } else if (state.is(ModBlocks.TAINT_CRUST.get())) {
                    crust++;
                } else if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                    logs++;
                }
            }
            check("в естественном очаге порчи выросли волокна и деревья (волокон " + fibres + ", корки " + crust + ", брёвен " + logs + ")",
                    fibres >= 10 && logs > 0);
        });
        shot(60, "taintworld");
        server(5, (server, player) -> {
            if (found[0] != null) {
                player.teleportTo(player.serverLevel(), found[0].getX() + 0.5, found[0].getY() + 3, found[0].getZ() + 0.5, 30f, 10f);
            }
        });
        shot(80, "taintworld_ground");
        server(20, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var pair = level.findClosestBiome3d(holder -> holder.is(thaumcraft.common.world.ModBiomes.MAGICAL_FOREST),
                    new BlockPos(0, 64, 0), 6400, 32, 64);
            check("в обычном мире есть волшебный лес в 6400 блоках от начала" + (pair == null ? "" : " (" + pair.getFirst().toShortString() + ")"),
                    pair != null);
            found[0] = pair == null ? null : pair.getFirst();
            if (pair != null) {
                player.teleportTo(level, found[0].getX() + 0.5, 180, found[0].getZ() + 0.5, 0f, 60f);
            }
        });
        server(300, (server, player) -> {
            if (found[0] == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockPos best = found[0];
            int bestCount = -1;
            for (int dx = -48; dx <= 48; dx += 16) {
                for (int dz = -48; dz <= 48; dz += 16) {
                    int count = 0;
                    for (int ax = -16; ax <= 16; ax += 8) {
                        for (int az = -16; az <= 16; az += 8) {
                            BlockPos probe = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    found[0].offset(dx + ax, 0, dz + az));
                            if (thaumcraft.common.world.ModBiomes.is(level, probe, thaumcraft.common.world.ModBiomes.MAGICAL_FOREST)) {
                                count++;
                            }
                        }
                    }
                    if (count > bestCount) {
                        bestCount = count;
                        best = found[0].offset(dx, 0, dz);
                    }
                }
            }
            found[0] = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, best);
            // камера высоко над лесом: ниже она оказалась бы внутри кроны или холма
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, found[0].getX() + 0.5, found[0].getY() + 30, found[0].getZ() - 24.5, 0f, 50f);
        });
        server(200, (server, player) -> {
            if (found[0] == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            int great = 0;
            int silver = 0;
            int oak = 0;
            int flowers = 0;
            int pods = 0;
            int shrooms = 0;
            int[] spots = new int[1];
            for (BlockPos pos : BlockPos.betweenClosed(found[0].offset(-40, -30, -40), found[0].offset(40, 20, 40))) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.GREATWOOD_LOG.get())) {
                    great++;
                } else if (state.is(ModBlocks.SILVERWOOD_LOG.get())) {
                    silver++;
                } else if (state.is(net.minecraft.world.level.block.Blocks.OAK_LOG)) {
                    oak++;
                } else if (state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS)) {
                    flowers++;
                } else if (state.is(ModBlocks.MANA_POD.get())) {
                    pods++;
                } else if (state.is(ModBlocks.VISHROOM.get())) {
                    shrooms++;
                }
                if (state.isAir() && pos.getY() > 64 && level.getBlockState(pos.below()).isAir()
                        && level.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.LOGS)) {
                    spots[0]++;
                }
            }
            // стручки редки: чтобы проверить сам генератор, прогоняем его по тем же чанкам ещё десять раз
            int extra = 0;
            for (int pass = 0; pass < 10; pass++) {
                for (int cx = -2; cx <= 2; cx++) {
                    for (int cz = -2; cz <= 2; cz++) {
                        BlockPos corner = new BlockPos((found[0].getX() >> 4) + cx << 4, level.getMinBuildHeight(), (found[0].getZ() >> 4) + cz << 4);
                        thaumcraft.common.registry.ModFeatures.MAGICAL_PLANTS.get().place(
                                net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE, level,
                                level.getChunkSource().getGenerator(), level.random, corner);
                    }
                }
            }
            for (BlockPos pos : BlockPos.betweenClosed(found[0].offset(-56, -30, -56), found[0].offset(56, 30, 56))) {
                if (level.getBlockState(pos).is(ModBlocks.MANA_POD.get())) {
                    extra++;
                }
            }
            Thaumcraft.LOGGER.info("AUTOSHOT стручков после десяти дополнительных проходов генератора: {}", extra);
            check("в естественном волшебном лесу растут дубы и цветы (дубовых брёвен " + oak + ", цветов " + flowers
                    + "; брёвен великого дерева " + great + ", серебряного " + silver + "; стручков маны " + pods + " при " + spots[0] + " подходящих местах, вишрумов " + shrooms + ")",
                    oak > 0 && flowers >= 10 && pods + shrooms > 0);
        });
        shot(60, "magicworld");
    }

    /** Рунный щит: набор заряда за вис, поглощение удара, полоска на экране, кольцо ученика. */
    private static void runic() {
        cmd("tp @s 500.5 -60 0.5 0 5");
        client(40, mc -> {
        });
        cmd("gamemode survival");
        cmd("difficulty normal");
        cmd("effect clear @s");
        cmd("clear @s");
        thaumcraft.api.aspects.Aspect air = thaumcraft.api.aspects.Aspect.byTag("aer");
        server(5, (server, player) -> {
            player.setHealth(20f);
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getStacksHandler("ring").ifPresent(slots -> {
                    slots.getStacks().setStackInSlot(0, new ItemStack(ModItems.RUNIC_RING.get()));
                    slots.getStacks().setStackInSlot(1, new ItemStack(ModItems.APPRENTICE_RING_AIR.get()));
                });
                handler.getStacksHandler("necklace").ifPresent(slots ->
                        slots.getStacks().setStackInSlot(0, new ItemStack(ModItems.RUNIC_AMULET.get())));
            });
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.GOLD, true);
            player.getInventory().add(wand);
            float discount = thaumcraft.common.items.wands.WandItem.getTotalVisDiscount(player, air);
            check("кольцо ученика воздуха даёт скидку 1% на вис воздуха (скидка " + discount + ")", Math.abs(discount - 0.01f) < 1.0e-4f);
        });
        server(60, (server, player) -> {
            int max = thaumcraft.common.runic.RunicShield.getMax(player);
            int vis = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof thaumcraft.common.items.wands.WandItem) {
                    vis = thaumcraft.common.items.wands.WandItem.getVis(stack, air);
                }
            }
            int charge = thaumcraft.common.runic.RunicShield.getCharge(player);
            check("рунное кольцо и амулет дают щит на 13 единиц, он набирается за вис из жезла (предел " + max + ", заряд "
                    + charge + ", воздуха в жезле " + vis / 100f + ")", max == 13 && charge >= 1 && vis < 10000);
            thaumcraft.common.runic.RunicShield.setCharge(player, 13);
        });
        client(20, mc -> check("клиент знает заряд щита для полоски на экране (заряд "
                + thaumcraft.client.gui.RunicOverlay.charge + " из " + thaumcraft.client.gui.RunicOverlay.max + ")",
                thaumcraft.client.gui.RunicOverlay.charge == 13 && thaumcraft.client.gui.RunicOverlay.max == 13));
        shot(5, "runic_hud");
        cmd("summon minecraft:husk 502.5 -60 2.5 {NoAI:1b,PersistenceRequired:1b}");
        client(10, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server(10, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(8));
            float before = player.getHealth();
            if (!husks.isEmpty()) {
                player.hurt(player.damageSources().mobAttack(husks.get(0)), 6f);
            }
            int charge = thaumcraft.common.runic.RunicShield.getCharge(player);
            check("щит поглотил удар на 6 единиц целиком (здоровье " + before + " -> " + player.getHealth() + ", заряд " + charge + ")",
                    !husks.isEmpty() && player.getHealth() == before && charge == 7);
        });
        client(6, mc -> Screenshot.grab(mc.gameDirectory, "runic_shield.png", mc.getMainRenderTarget(), message -> { }));
        server(20, (server, player) -> {
            float before = player.getHealth();
            // за прошедшую секунду щит мог восстановить единицу, поэтому остаток берётся перед самым ударом
            int shield = thaumcraft.common.runic.RunicShield.getCharge(player);
            player.hurt(player.damageSources().magic(), 10f);
            int charge = thaumcraft.common.runic.RunicShield.getCharge(player);
            check("удар сильнее остатка щита пробивает его: щит " + shield + ", удар 10 (здоровье " + before + " -> "
                    + player.getHealth() + ", заряд " + charge + ")",
                    charge == 0 && Math.abs(before - player.getHealth() - (10f - shield)) < 0.01f);
        });
        client(1, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        // особые украшения: кольцо исцеления, амулет экстренной защиты и кинетический пояс срабатывают, когда щит пробит
        server(5, (server, player) -> {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getStacksHandler("ring").ifPresent(slots -> {
                    slots.getStacks().setStackInSlot(0, new ItemStack(ModItems.RUNIC_RING_HEALING.get()));
                    slots.getStacks().setStackInSlot(1, new ItemStack(ModItems.RUNIC_RING_CHARGED.get()));
                });
                handler.getStacksHandler("necklace").ifPresent(slots ->
                        slots.getStacks().setStackInSlot(0, new ItemStack(ModItems.RUNIC_AMULET_EMERGENCY.get())));
                handler.getStacksHandler("belt").ifPresent(slots ->
                        slots.getStacks().setStackInSlot(0, new ItemStack(ModItems.RUNIC_GIRDLE_KINETIC.get())));
            });
            player.setHealth(20f);
            player.removeAllEffects();
        });
        server(50, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int max = thaumcraft.common.runic.RunicShield.getMax(player);
            thaumcraft.common.runic.RunicShield.setCharge(player, 1);
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            pig.moveTo(player.getX() + 1.5, player.getY(), player.getZ(), 0f, 0f);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            player.hurt(player.damageSources().magic(), 4f);
            int charge = thaumcraft.common.runic.RunicShield.getCharge(player);
            boolean regen = player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            check("пробитый щит с особыми украшениями: предел " + max + ", экстренная защита вернула заряд (" + charge
                            + "), кольцо дало регенерацию (" + regen + "), взрыв пояса задел свинью (здоровье " + pig.getHealth() + " из 10)",
                    max >= 8 && charge == 8 && regen && pig.getHealth() < 10f);
            pig.discard();
            player.removeAllEffects();
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("belt").ifPresent(slots -> slots.getStacks().setStackInSlot(0, ItemStack.EMPTY)));
        });
        server(5, (server, player) -> top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getStacksHandler("ring").ifPresent(slots -> {
                slots.getStacks().setStackInSlot(0, ItemStack.EMPTY);
                slots.getStacks().setStackInSlot(1, ItemStack.EMPTY);
            });
            handler.getStacksHandler("necklace").ifPresent(slots -> slots.getStacks().setStackInSlot(0, ItemStack.EMPTY));
        }));
        cmd("kill @e[type=!player]");
        cmd("clear @s");
        cmd("gamemode creative");
        cmd("difficulty peaceful");

        // «Рунное усиление»: цена растёт с уже имеющимся щитом вещи, результат — та же вещь с лишней единицей
        cmd("thaumcraft research all");
        server(5, (server, player) -> {
            thaumcraft.common.research.PlayerKnowledge knowledge = thaumcraft.common.research.PlayerKnowledge.get(player);
            ItemStack helmet = new ItemStack(ModItems.THAUMIUM_HELMET.get());
            ItemStack diamond = new ItemStack(net.minecraft.world.item.Items.DIAMOND);
            ItemStack salt = new ItemStack(ModItems.SALIS_MUNDUS.get());
            thaumcraft.common.crafting.InfusionRecipe plain = thaumcraft.common.crafting.InfusionRecipe.find(player.level(), knowledge,
                    helmet, List.of(diamond, salt));
            ItemStack ring = new ItemStack(ModItems.RUNIC_RING.get());
            List<ItemStack> forRing = new ArrayList<>(List.of(diamond));
            for (int i = 0; i < 6; i++) {
                forRing.add(salt);
            }
            thaumcraft.common.crafting.InfusionRecipe strong = thaumcraft.common.crafting.InfusionRecipe.find(player.level(), knowledge,
                    ring, forRing);
            thaumcraft.common.crafting.InfusionRecipe wrong = thaumcraft.common.crafting.InfusionRecipe.find(player.level(), knowledge,
                    new ItemStack(net.minecraft.world.item.Items.IRON_HELMET), List.of(diamond, salt));
            thaumcraft.api.aspects.Aspect energy = thaumcraft.api.aspects.Aspect.byTag("potentia");
            int plainCost = plain == null ? -1 : plain.getAspects(helmet).getAmount(energy);
            int strongCost = strong == null ? -1 : strong.getAspects(ring).getAmount(energy);
            int made = plain == null ? -1 : thaumcraft.common.runic.RunicShield.getFinalCharge(plain.getResult(helmet));
            int madeRing = strong == null ? -1 : thaumcraft.common.runic.RunicShield.getFinalCharge(strong.getResult(ring));
            check("рунное усиление: шлем из тауминия 0 -> " + made + " за " + plainCost + " потенции, рунное кольцо 5 -> " + madeRing
                            + " за " + strongCost + ", железный шлем не годится (" + (wrong == null) + ")",
                    made == 1 && plainCost == 32 && madeRing == 6 && strongCost == 1024 && wrong == null);
        });
    }

    /** Особые мобы всех тринадцати видов, добыча с убитого и мешок с добычей. */
    private static void champions() {
        cmd("tp @s 400.5 -60 0.5 0 5");
        client(40, mc -> {
        });
        cmd("difficulty normal");
        cmd("time set noon");
        cmd("kill @e[type=!player]");
        for (int i = 0; i < thaumcraft.common.champion.Champions.MODS.length; i++) {
            int type = i;
            int x = 394 + i;
            cmd("summon minecraft:husk " + x + ".5 -60 " + (i % 2 == 0 ? 7 : 9) + ".5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
            server(2, (server, player) -> {
                for (net.minecraft.world.entity.monster.Husk husk : player.serverLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Husk.class, new net.minecraft.world.phys.AABB(x, -61, 5, x + 1, -56, 11))) {
                    thaumcraft.common.champion.Champions.makeChampion(husk, type, true);
                }
            });
        }
        server(10, (server, player) -> {
            var husks = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(24));
            int champions = 0;
            boolean named = true;
            boolean strong = true;
            for (var husk : husks) {
                if (thaumcraft.common.champion.Champions.get(husk) >= 0) {
                    champions++;
                    named &= husk.hasCustomName();
                    strong &= husk.getMaxHealth() == 50f;
                }
            }
            check("тринадцать особых мобов: у каждого своё имя и 50 здоровья вместо 20 (найдено " + champions + ")",
                    champions == 13 && named && strong);
        });
        client(20, mc -> {
            int seen = 0;
            for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof net.minecraft.world.entity.monster.Husk husk && thaumcraft.common.champion.Champions.get(husk) >= 0) {
                    seen++;
                }
            }
            check("клиент знает свойства особых мобов и рисует их частицы (видит " + seen + ")", seen == 13);
        });
        shot(40, "champions");
        cmd("tp @s 400.5 -60 4.5 0 0");
        shot(20, "champions_near");
        // убитый игроком особый моб оставляет мешок
        server(2, (server, player) -> {
            for (var husk : player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    player.getBoundingBox().inflate(24))) {
                if (thaumcraft.common.champion.Champions.get(husk) == thaumcraft.common.champion.Champions.BOLD) {
                    husk.hurt(player.damageSources().playerAttack(player), 1000f);
                }
            }
        });
        server(30, (server, player) -> {
            int bags = 0;
            for (net.minecraft.world.entity.item.ItemEntity item : player.serverLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(24))) {
                if (item.getItem().getItem() instanceof thaumcraft.common.items.LootBagItem) {
                    bags += item.getItem().getCount();
                }
            }
            check("убитый игроком особый моб оставил мешок с добычей (мешков " + bags + ")", bags == 1);
        });
        cmd("kill @e[type=!player]");
        // убитые командой мобы роняют свою добычу чуть позже: её тоже надо убрать
        client(40, mc -> {
        });
        cmd("kill @e[type=item]");
        cmd("kill @e[type=experience_orb]");
        // мешок высыпает от 8 до 12 предметов
        cmd("clear @s");
        server(5, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOOT_BAG_RARE.get())));
        client(15, mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        server(10, (server, player) -> {
            int stacks = 0;
            int coins = 0;
            StringBuilder names = new StringBuilder();
            for (net.minecraft.world.entity.item.ItemEntity item : player.serverLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(6))) {
                stacks++;
                if (item.getItem().is(ModItems.COIN.get())) {
                    coins += item.getItem().getCount();
                }
                names.append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem()).getPath()).append(' ');
            }
            // предметы высыпаются под ноги и тут же подбираются, поэтому считаем и инвентарь
            for (ItemStack held : player.getInventory().items) {
                if (!held.isEmpty() && !(held.getItem() instanceof thaumcraft.common.items.LootBagItem)) {
                    stacks++;
                    if (held.is(ModItems.COIN.get())) {
                        coins += held.getCount();
                    }
                    names.append(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem()).getPath()).append(' ');
                }
            }
            check("редкий мешок с добычей высыпал предметы (стопок " + stacks + ", монет " + coins + ": " + names.toString().trim() + ")",
                    stacks >= 1 && coins >= 3);
        });
        cmd("kill @e[type=item]");
        // судьба обычных монстров: за тысячу появлений особыми становятся около одного из ста
        server(5, (server, player) -> {
            int made = 0;
            for (int i = 0; i < 1000; i++) {
                net.minecraft.world.entity.monster.Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(player.serverLevel());
                zombie.moveTo(400.5, -60, 12.5);
                player.serverLevel().addFreshEntity(zombie);
                if (thaumcraft.common.champion.Champions.get(zombie) >= 0) {
                    made++;
                }
                zombie.discard();
            }
            check("из тысячи зомби особыми стали примерно десять (стало " + made + ")", made >= 2 && made <= 30);
        });
        cmd("kill @e[type=!player]");
        cmd("difficulty peaceful");

        // фрагмент знаний прибавляет очки первичных аспектов
        cmd("clear @s");
        thaumcraft.api.aspects.Aspect air = thaumcraft.api.aspects.Aspect.byTag("aer");
        int[] before = new int[1];
        server(5, (server, player) -> {
            before[0] = thaumcraft.common.research.PlayerKnowledge.get(player).getPool(air);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()));
        });
        client(15, mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        server(10, (server, player) -> {
            int gained = thaumcraft.common.research.PlayerKnowledge.get(player).getPool(air) - before[0];
            check("фрагмент знаний прибавил 1–2 очка аспекта воздуха (прибавилось " + gained + ")", gained >= 1 && gained <= 2);
        });
        // «неизвестное знание»: либо заметка по скрытому исследованию, либо 7–9 фрагментов обратно
        cmd("thaumcraft research reset");
        server(5, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND,
                thaumcraft.common.items.ResearchNotesItem.unknown(ModItems.RESEARCH_NOTES.get())));
        client(15, mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        server(10, (server, player) -> {
            ItemStack held = player.getMainHandItem();
            thaumcraft.common.research.ResearchNote note = thaumcraft.common.research.ResearchNote.read(held);
            int fragments = player.getInventory().countItem(ModItems.KNOWLEDGE_FRAGMENT.get());
            for (net.minecraft.world.entity.item.ItemEntity item : player.serverLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(6))) {
                if (item.getItem().is(ModItems.KNOWLEDGE_FRAGMENT.get())) {
                    fragments += item.getItem().getCount();
                }
            }
            check("неизвестное знание раскрылось (" + (note != null ? "заметка " + note.key : "фрагментов обратно " + fragments) + ")",
                    !thaumcraft.common.items.ResearchNotesItem.isUnknown(held) && (note != null || fragments >= 7));
        });
        cmd("thaumcraft research all");
        cmd("clear @s");
        cmd("kill @e[type=item]");
        // сундук сокровищницы: в среднем около двух предметов мода
        server(5, (server, player) -> {
            net.minecraft.world.level.storage.loot.LootTable table = server.getLootData().getLootTable(
                    thaumcraft.common.Res.parse("minecraft:chests/simple_dungeon"));
            int mod = 0;
            int bags = 0;
            int chests = 400;
            for (int i = 0; i < chests; i++) {
                net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(
                        player.serverLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        player.position()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                for (ItemStack stack : table.getRandomItems(params)) {
                    if (net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("thaumcraft")) {
                        mod++;
                        if (stack.getItem() instanceof thaumcraft.common.items.LootBagItem) {
                            bags++;
                        }
                    }
                }
            }
            float perChest = mod / (float) chests;
            check("в сундуке сокровищницы в среднем около двух предметов мода (" + perChest + " на сундук, мешков " + bags
                    + " на " + chests + " сундуков)", perChest > 1.5f && perChest < 2.5f && bags > 0);
        });
    }

    /** Кристаллические гроздья, пьедестал подзарядки с фокусом и амулет вис у реле. */
    private static void recharge() {
        String[] kinds = {"air", "fire", "water", "earth", "order", "entropy", "mixed"};
        // сначала сам игрок: в незагруженном месте блоки не ставятся
        cmd("tp @s 300.5 -60 3.5 0 5");
        client(40, mc -> {
        });
        cmd("fill 296 -60 8 306 -57 8 minecraft:stone");
        for (int i = 0; i < kinds.length; i++) {
            cmd("setblock " + (297 + i) + " -60 7 thaumcraft:crystal_" + kinds[i] + "[facing=up]");
        }
        cmd("setblock 299 -58 7 thaumcraft:crystal_mixed[facing=north]");
        cmd("setblock 301 -58 7 thaumcraft:crystal_fire[facing=north]");
        cmd("tp @s 300.5 -60 3.5 0 5");
        shot(30, "crystals");
        BlockPos crystal = new BlockPos(297, -60, 7);
        server(2, (server, player) -> player.serverLevel().destroyBlock(crystal, true));
        server(10, (server, player) -> {
            int shards = 0;
            for (net.minecraft.world.entity.item.ItemEntity item : player.serverLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(crystal).inflate(2))) {
                if (item.getItem().is(ModItems.SHARD_AIR.get())) {
                    shards += item.getItem().getCount();
                }
            }
            check("гроздь воздуха рассыпалась на шесть осколков (получено " + shards + ")", shards == 6);
        });
        cmd("setblock 303 -61 7 minecraft:air");
        server(5, (server, player) -> check("гроздь без опоры осыпается",
                player.serverLevel().getBlockState(new BlockPos(303, -60, 7)).isAir()));
        cmd("kill @e[type=item]");
        cmd("fill 296 -61 7 306 -57 8 minecraft:air");
        cmd("fill 296 -61 7 306 -61 8 minecraft:grass_block");

        // пьедестал подзарядки: пустой жезл тянет вис из узла рядом
        BlockPos pedestal = new BlockPos(300, -60, 7);
        cmd("setblock 300 -60 7 thaumcraft:wand_pedestal");
        cmd("tp @s 302.5 -60 9.5 0 0");
        cmd("thaumcraft node");
        cmd("tp @s 300.5 -60 3.5 0 5");
        server(5, (server, player) -> {
            // узел с известным запасом: два первичных аспекта и один составной (его берёт только фокус)
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(300, -61, 7), new BlockPos(304, -56, 11))) {
                if (player.serverLevel().getBlockEntity(pos) instanceof thaumcraft.common.blockentity.NodeBlockEntity node) {
                    node.setAspects(new thaumcraft.api.aspects.AspectList()
                            .add(thaumcraft.api.aspects.Aspect.byTag("aer"), 12)
                            .add(thaumcraft.api.aspects.Aspect.byTag("ignis"), 12)
                            .add(thaumcraft.api.aspects.Aspect.byTag("victus"), 12));
                    node.nodeChange();
                }
            }
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.SILVERWOOD, thaumcraft.api.wands.WandCap.THAUMIUM, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        });
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pedestal), Direction.UP, pedestal, false)));
        server(10, (server, player) -> check("пьедестал подзарядки принял жезл из руки",
                player.serverLevel().getBlockEntity(pedestal) instanceof thaumcraft.common.blocks.WandPedestalBlock.Charger charger
                && charger.getItem().getItem() instanceof thaumcraft.common.items.wands.WandItem));
        server(2, (server, player) -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        shot(60, "wand_pedestal");
        // в узле по 12 единиц двух первичных аспектов; последнюю единицу каждого пьедестал узлу оставляет
        server(140, (server, player) -> {
            int vis = 0;
            if (player.serverLevel().getBlockEntity(pedestal) instanceof thaumcraft.common.blocks.WandPedestalBlock.Charger charger) {
                for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                    vis += thaumcraft.common.items.wands.WandItem.getVis(charger.getItem(), aspect);
                }
            }
            int signal = player.serverLevel().getBlockState(pedestal).getAnalogOutputSignal(player.serverLevel(), pedestal);
            check("жезл на пьедестале набирает вис из узла (набрано " + vis / 100 + " из 22 доступных, сигнал компаратора "
                    + signal + ")", vis >= 2000 && vis <= 2400 && signal >= 1);
        });
        cmd("setblock 300 -59 7 thaumcraft:wand_pedestal_focus");
        shot(40, "wand_pedestal_focus");
        server(60, (server, player) -> {
            int vis = 0;
            if (player.serverLevel().getBlockEntity(pedestal) instanceof thaumcraft.common.blocks.WandPedestalBlock.Charger charger) {
                for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                    vis += thaumcraft.common.items.wands.WandItem.getVis(charger.getItem(), aspect);
                }
            }
            check("с фокусом пьедестал берёт и составной аспект узла (теперь набрано " + vis / 100 + ")", vis >= 3000);
        });
        client(5, mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pedestal.above()), Direction.NORTH, pedestal.above(), false)));
        server(10, (server, player) -> check("щелчок по фокусу забирает жезл с пьедестала под ним",
                player.getMainHandItem().getItem() instanceof thaumcraft.common.items.wands.WandItem
                        && player.serverLevel().getBlockEntity(pedestal) instanceof thaumcraft.common.blocks.WandPedestalBlock.Charger charger
                        && charger.getItem().isEmpty()));

        // амулет вис: надетый, заряжает жезл в руке
        server(5, (server, player) -> {
            ItemStack amulet = new ItemStack(ModItems.VIS_AMULET.get());
            for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                thaumcraft.common.items.VisAmuletItem.storeVis(amulet, aspect, 5000);
            }
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.WOOD, thaumcraft.api.wands.WandCap.IRON, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("necklace").ifPresent(slots -> slots.getStacks().setStackInSlot(0, amulet)));
        });
        server(60, (server, player) -> {
            int vis = thaumcraft.common.items.wands.WandItem.getVis(player.getMainHandItem(), thaumcraft.api.aspects.Aspect.byTag("aer"));
            var worn = thaumcraft.common.compat.Baubles.find(player, s -> s.getItem() instanceof thaumcraft.common.items.VisAmuletItem);
            int left = worn.isEmpty() ? -1 : thaumcraft.common.items.VisAmuletItem.getVis(worn.get(0),
                    thaumcraft.api.aspects.Aspect.byTag("aer"));
            check("надетый амулет вис заряжает жезл в руке (в жезле " + vis + ", в амулете " + left + ")",
                    vis > 0 && left >= 0 && vis + left == 5000);
        });
        server(2, (server, player) -> {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("necklace").ifPresent(slots -> slots.getStacks().setStackInSlot(0, ItemStack.EMPTY)));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        cmd("fill 296 -60 5 306 -56 12 minecraft:air");

        // зарядное реле: жезл в магическом верстаке под ним заряжается от заряженного узла
        BlockPos bench = new BlockPos(300, -60, 7);
        cmd("setblock 300 -60 7 thaumcraft:arcane_workbench");
        cmd("setblock 300 -59 7 thaumcraft:vis_charge_relay");
        cmd("tp @s 303.5 -59 7.5 0 0");
        cmd("thaumcraft node");
        cmd("tp @s 300.5 -60 3.5 0 5");
        server(5, (server, player) -> {
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(301, -61, 5), new BlockPos(305, -55, 9))) {
                if (player.serverLevel().getBlockEntity(pos) instanceof thaumcraft.common.blockentity.NodeBlockEntity node) {
                    thaumcraft.api.aspects.AspectList all = new thaumcraft.api.aspects.AspectList();
                    for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                        all.add(aspect, 64);
                    }
                    node.setAspects(all);
                    node.energize();
                }
            }
            if (player.serverLevel().getBlockEntity(bench) instanceof thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity workbench) {
                workbench.setWand(thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                        thaumcraft.api.wands.WandRod.GREATWOOD, thaumcraft.api.wands.WandCap.GOLD, false));
            }
        });
        shot(60, "vis_charge_relay");
        server(40, (server, player) -> {
            int vis = 0;
            boolean linked = player.serverLevel().getBlockEntity(bench.above())
                    instanceof thaumcraft.common.blocks.VisChargeRelayBlock.ChargeRelay relay && relay.isConnected();
            if (player.serverLevel().getBlockEntity(bench) instanceof thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity workbench) {
                for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                    vis += thaumcraft.common.items.wands.WandItem.getVis(workbench.getWand(), aspect);
                }
            }
            check("зарядное реле заряжает жезл в верстаке от сети вис (связь " + linked + ", в жезле " + vis / 100f + ")",
                    linked && vis > 0);
            // пустой амулет вис на шее: рядом с реле он должен набирать вис из сети
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("necklace").ifPresent(slots ->
                            slots.getStacks().setStackInSlot(0, new ItemStack(ModItems.VIS_AMULET.get()))));
        });
        cmd("tp @s 300.5 -60 5.5 0 5");
        server(80, (server, player) -> {
            var worn = thaumcraft.common.compat.Baubles.find(player, s -> s.getItem() instanceof thaumcraft.common.items.VisAmuletItem);
            int vis = 0;
            if (!worn.isEmpty()) {
                for (thaumcraft.api.aspects.Aspect aspect : thaumcraft.api.aspects.Aspect.primals()) {
                    vis += thaumcraft.common.items.VisAmuletItem.getVis(worn.get(0), aspect);
                }
            }
            check("надетый амулет вис заряжается от реле рядом (в амулете " + vis / 100f + ")", vis > 0);
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("necklace").ifPresent(slots -> slots.getStacks().setStackInSlot(0, ItemStack.EMPTY)));
        });
        cmd("fill 296 -60 5 306 -55 12 minecraft:air");
    }

    /** Очки откровения: узел сквозь стену, значки над сосудами, очки на голове и скидка на вис. */
    private static void goggles() {
        // истощение вис от прежних сцен удорожило бы заклинания и исказило проверку скидки
        cmd("effect clear @s");
        cmd("tp @s 20.5 -60 20.5 0 0");
        cmd("thaumcraft node");
        cmd("item replace entity @s armor.head with thaumcraft:goggles");
        server(2, (server, player) -> {
            ItemStack wand = thaumcraft.common.items.wands.WandItem.create(ModItems.WAND.get(),
                    thaumcraft.api.wands.WandRod.WOOD, thaumcraft.api.wands.WandCap.IRON, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand);
            thaumcraft.api.aspects.Aspect air = thaumcraft.api.aspects.Aspect.byTag("aer");
            float discount = thaumcraft.common.items.wands.WandItem.getTotalVisDiscount(player, air);
            float with = thaumcraft.common.items.wands.WandItem.getConsumptionModifier(wand, player, air, false);
            float without = thaumcraft.common.items.wands.WandItem.getConsumptionModifier(wand, null, air, false);
            check("очки откровения дают скидку 5% на вис (скидка " + discount + ", множитель " + without + " -> " + with + ")",
                    Math.abs(discount - 0.05f) < 1.0e-4f && Math.abs(without - with - 0.05f) < 1.0e-4f);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        // узел за каменной стеной виден сквозь неё
        cmd("fill 18 -60 19 22 -56 19 minecraft:stone");
        cmd("tp @s 20.5 -60 15.5 0 -10");
        shot(30, "goggles_node");
        cmd("fill 18 -60 19 22 -56 19 minecraft:air");
        // значки над банкой, тиглем и узлом
        cmd("setblock 24 -60 13 thaumcraft:warded_jar{Aspect:\"ignis\",Amount:40s}");
        cmd("tp @s 24.5 -60 11.2 0 30");
        client(20, mc -> check("взгляд в очках направлен на банку с эссенцией", mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof thaumcraft.api.aspects.AspectContainer container
                && container.getContainedAspects().getAmount(thaumcraft.api.aspects.Aspect.byTag("ignis")) == 40));
        shot(30, "goggles_jar");
        cmd("setblock 24 -60 13 minecraft:note_block[note=7]");
        shot(20, "goggles_note");
        cmd("setblock 24 -60 13 minecraft:air");
        cmd("tp @s 20.5 -60 17.5 0 2");
        client(20, mc -> check("взгляд в очках направлен на узел ауры", mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof thaumcraft.common.blockentity.NodeBlockEntity));
        shot(30, "goggles_node_tags");
        cmd("tp @s 20.5 -60 15.5 0 0");
        client(5, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        shot(20, "goggles_worn");
        client(1, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        cmd("item replace entity @s armor.head with minecraft:air");
    }

    // ---------- кирпичики сценария ----------

    private static void client(int wait, Consumer<Minecraft> action) {
        STEPS.add(new Step(wait, () -> action.accept(Minecraft.getInstance())));
    }

    private interface ServerAction {
        void run(MinecraftServer server, ServerPlayer player);
    }

    private static void server(int wait, ServerAction action) {
        client(wait, mc -> {
            MinecraftServer server = mc.getSingleplayerServer();
            server.execute(() -> action.run(server, server.getPlayerList().getPlayers().get(0)));
        });
    }

    private static void cmd(String command) {
        server(2, (server, player) -> server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command));
    }

    private static void check(String what, boolean passed) {
        if (passed) {
            Thaumcraft.LOGGER.info("AUTOSHOT CHECK ok: {}", what);
        } else {
            Thaumcraft.LOGGER.error("AUTOSHOT CHECK FAIL: {}", what);
        }
    }

    /** Предмет, брошенный игроком, появляется прямо над тиглем. */
    private static void toss(ServerPlayer player, BlockPos crucible, ItemStack stack) {
        net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                player.serverLevel(), crucible.getX() + 0.5, crucible.getY() + 1.2, crucible.getZ() + 0.5, stack);
        item.setDeltaMovement(0, 0, 0);
        item.setThrower(player.getUUID());
        player.serverLevel().addFreshEntity(item);
    }

    private static void screen(int wait, Supplier<Screen> screen) {
        client(wait, mc -> mc.setScreen(screen.get()));
    }

    private static void close() {
        client(3, mc -> {
            if (mc.player != null) {
                mc.player.closeContainer();
            }
            mc.setScreen(null);
        });
    }

    private static void shot(int wait, String name) {
        client(wait, mc -> {
            mc.getToasts().clear();
            // бывает, что скрытое окно теряет размер и снимки выходят в один пиксель: возвращаем его
            if (mc.getMainRenderTarget().width < 640) {
                Thaumcraft.LOGGER.warn("AUTOSHOT окно потеряло размер ({}x{}), восстанавливаю", mc.getMainRenderTarget().width,
                        mc.getMainRenderTarget().height);
                mc.getWindow().setWindowed(1280, 720);
                mc.resizeDisplay();
            }
        });
        client(2, mc -> Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> Thaumcraft.LOGGER.info("AUTOSHOT {}", name)));
    }

    /** Все предметы вкладки мода сеткой: проверка того, как модели лежат в ячейках интерфейса. */
    private static final class ItemGridScreen extends Screen {
        ItemGridScreen() {
            super(net.minecraft.network.chat.Component.empty());
        }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xff8b8b8b);
            int columns = (width - 4) / 18;
            if (columns <= 0) {
                return;
            }
            int i = 0;
            for (ItemStack stack : thaumcraft.common.registry.ModCreativeTabs.MAIN.get().getDisplayItems()) {
                int x = 2 + i % columns * 18;
                int y = 2 + i / columns * 18;
                graphics.fill(x, y, x + 16, y + 16, 0xff555555);
                graphics.renderItem(stack, x, y);
                if (!logged) {
                    Thaumcraft.LOGGER.info("AUTOSHOT ITEM {} {} {}", i / columns, i % columns,
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath());
                }
                i++;
            }
            logged = true;
        }

        private boolean logged;
    }

    // ---------- исполнение ----------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            if (state == 0 && mc.screen instanceof TitleScreen) {
                state = 1;
                mc.options.pauseOnLostFocus = false;
                mc.options.hideGui = false;
                mc.getWindow().setWindowed(1280, 720);
                mc.options.guiScale().set(2);
                // скрытое окно продолжает рисовать кадры полного размера, но не появляется на экране и не забирает фокус
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                mc.resizeDisplay();
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                        new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow("taintworld".equals(System.getProperty("thaumcraft.autoshot.only"))
                                        ? WorldPresets.NORMAL : WorldPresets.FLAT).value().createWorldDimensions());
            } else if (state == 1 && mc.player != null && mc.level != null && mc.screen == null) {
                if (++timer > 60) {
                    state = 2;
                    timer = 0;
                    script();
                    // мышь не должна наводиться на предметы и менять подсказки
                    InputConstants.grabOrReleaseMouse(mc.getWindow().getWindow(), InputConstants.CURSOR_NORMAL, 0, 0);
                }
            } else if (state == 2) {
                if (index >= STEPS.size()) {
                    state = 3;
                    Thaumcraft.LOGGER.info("AUTOSHOT готово, снимков: {}", STEPS.size());
                    mc.stop();
                    return;
                }
                Step step = STEPS.get(index);
                if (++timer >= step.delay()) {
                    timer = 0;
                    index++;
                    step.action().run();
                }
            }
        } catch (Throwable error) {
            Thaumcraft.LOGGER.error("AUTOSHOT сбой на шаге " + index, error);
            state = 3;
            mc.stop();
        }
    }

    private AutoShot() {
    }
}

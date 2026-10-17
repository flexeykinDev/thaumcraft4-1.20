package thaumictinkerer.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.debug.AutoShot;
import thaumcraft.common.blockentity.EssentiaBlockEntity;
import thaumcraft.common.items.JarBlockItem;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchRegistry;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.FunnelBlock;
import thaumictinkerer.common.block.GasBlock;
import thaumictinkerer.common.block.PlatformBlock;
import thaumictinkerer.common.block.RepairerBlock;
import thaumictinkerer.common.item.XpTalismanItem;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.client;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка аддона: gradlew runClient -Pautoshot=tinkerer (tools/autoshot.ps1 -Only tinkerer). */
public final class TTAutoShot {
    private TTAutoShot() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer", TTAutoShot::script);
    }

    private static int countGas(ServerLevel level, BlockPos centre, int radius) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).getBlock() instanceof GasBlock) {
                count++;
            }
        }
        return count;
    }

    private static void script() {
        cmd("time set noon");
        cmd("gamemode creative");
        cmd("clear @s");
        cmd("effect clear @s");
        cmd("fill 1396 -60 -4 1412 -54 12 minecraft:air");
        cmd("tp @s 1404.5 -60 0.5 0 20");
        // ---------- данные: исследования, рецепты
        server(10, (server, player) -> {
            String[] keys = {"DARK_QUARTZ", "SHARE_TOME", "GASEOUS_LIGHT", "GASEOUS_SHADOW", "GAS_REMOVER", "BRIGHT_NITOR", "FUNNEL",
                    "REPAIRER", "SPELL_CLOTH", "XP_TALISMAN", "CLEANSING_TALISMAN", "REVEALING_HELM", "ASPECT_ANALYZER", "PLATFORM"};
            int found = 0;
            for (String key : keys) {
                if (ResearchRegistry.get(key) != null && "TT_CATEGORY".equals(ResearchRegistry.get(key).category)) {
                    found++;
                }
            }
            check("Thaumic Tinkerer: исследований первой части в книге " + found + " из " + keys.length, found == keys.length);
            var knowledge = PlayerKnowledge.get(player);
            check("закоптившийся кварц и том обмена знаниями известны сразу",
                    knowledge.isResearchComplete("DARK_QUARTZ") && knowledge.isResearchComplete("SHARE_TOME"));
            long recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> recipe.getId().getNamespace().equals(ThaumicTinkerer.MODID)).count();
            check("рецептов аддона загружено " + recipes, recipes == 20);
            // заклинательная ткань снимает чары и изнашивается
            TransientCraftingContainer grid = new TransientCraftingContainer(player.containerMenu, 3, 3);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 3);
            grid.setItem(0, sword);
            grid.setItem(4, new ItemStack(TTItems.SPELL_CLOTH.get()));
            var recipe = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, player.level());
            ItemStack result = recipe.map(r -> r.assemble(grid, server.registryAccess())).orElse(ItemStack.EMPTY);
            ItemStack cloth = recipe.map(r -> r.getRemainingItems(grid).get(4)).orElse(ItemStack.EMPTY);
            check("заклинательная ткань: меч без чар (" + result.isEnchanted() + "), ткань изношена на " + cloth.getDamageValue(),
                    result.is(Items.IRON_SWORD) && !result.isEnchanted() && cloth.getDamageValue() == 1);
        });
        // ---------- газы и гиперзаряженный нитор
        BlockPos gasAt = new BlockPos(1404, -59, 0);
        server(5, (server, player) -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TTItems.GASEOUS_LIGHT.get()));
            TTItems.GASEOUS_LIGHT.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        });
        server(80, (server, player) -> {
            ServerLevel level = player.serverLevel();
            int gas = countGas(level, gasAt, 5);
            int light = level.getBrightness(LightLayer.BLOCK, gasAt.offset(2, 0, 0));
            check("светящийся газ расползся (" + gas + " блоков) и светит (" + light + ")", gas > 20 && light >= 10);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TTItems.GAS_REMOVER.get()));
            TTItems.GAS_REMOVER.get().use(level, player, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
            int left = 0;
            for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-3, -3, -3), player.blockPosition().offset(2, 2, 2))) {
                if (level.getBlockState(pos).getBlock() instanceof GasBlock) {
                    left++;
                }
            }
            check("рассеиватель убрал газ вокруг (осталось рядом " + left + ")", left == 0);
            for (BlockPos pos : BlockPos.betweenClosed(gasAt.offset(-6, -1, -6), gasAt.offset(6, 5, 6))) {
                if (level.getBlockState(pos).getBlock() instanceof GasBlock) {
                    level.removeBlock(pos, false);
                }
            }
            player.getInventory().add(new ItemStack(TTItems.BRIGHT_NITOR.get()));
        });
        server(20, (server, player) -> {
            BlockPos head = BlockPos.containing(player.getX(), player.getY() + 1, player.getZ());
            check("гиперзаряженный нитор светит над головой", player.level().getBlockState(head).is(TTBlocks.NITOR_GAS.get()));
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(TTItems.BRIGHT_NITOR.get()), 64,
                    player.inventoryMenu.getCraftSlots());
        });
        server(60, (server, player) -> {
            BlockPos head = BlockPos.containing(player.getX(), player.getY() + 1, player.getZ());
            check("без нитора свет гаснет", !player.level().getBlockState(head).is(TTBlocks.NITOR_GAS.get()));
        });
        // ---------- воронка: из банки-предмета по хопперу в банку-блок
        BlockPos hopper = new BlockPos(1400, -60, 4);
        BlockPos target = new BlockPos(1401, -60, 4);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(target, ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            level.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST), 3);
            level.setBlock(hopper.above(), TTBlocks.FUNNEL.get().defaultBlockState(), 3);
            ItemStack jar = new ItemStack(ModItems.WARDED_JAR.get());
            JarBlockItem.setContents(jar, Aspect.FIRE, 12);
            ((FunnelBlock.Tile) level.getBlockEntity(hopper.above())).setItem(0, jar);
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            var jarBlock = (EssentiaBlockEntity) level.getBlockEntity(target);
            ItemStack left = ((FunnelBlock.Tile) level.getBlockEntity(hopper.above())).getJar();
            check("воронка перелила эссенцию: в банке " + jarBlock.getAmount() + " ignis, в банке-предмете осталось "
                    + JarBlockItem.getAmount(left), jarBlock.getAspect() == Aspect.FIRE && jarBlock.getAmount() == 12
                    && JarBlockItem.getAmount(left) == 0);
            // для снимка — банка с остатком
            ItemStack shown = new ItemStack(ModItems.WARDED_JAR.get());
            JarBlockItem.setContents(shown, Aspect.WATER, 40);
            ((FunnelBlock.Tile) level.getBlockEntity(hopper.above())).setItem(0, shown);
            level.setBlock(target, ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
        });
        // ---------- восстановитель: банка с инструментом -> труба -> восстановитель
        BlockPos repairer = new BlockPos(1404, -60, 4);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(repairer, TTBlocks.REPAIRER.get().defaultBlockState().setValue(RepairerBlock.FACING, Direction.EAST), 3);
            level.setBlock(repairer.east(), ModBlocks.TUBE.get().defaultBlockState(), 3);
            level.setBlock(repairer.east().below(), ModBlocks.WARDED_JAR.get().defaultBlockState(), 3);
            ((EssentiaBlockEntity) level.getBlockEntity(repairer.east().below())).add(Aspect.TOOL, 64);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            pick.setDamageValue(200);
            ((RepairerBlock.Tile) level.getBlockEntity(repairer)).setItem(0, pick);
        });
        server(100, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ItemStack pick = ((RepairerBlock.Tile) level.getBlockEntity(repairer)).getItem();
            int signal = level.getSignal(repairer, Direction.WEST);
            check("восстановитель чинит инструментом из трубы: износ 200 -> " + pick.getDamageValue() + ", сигнал " + signal,
                    pick.getDamageValue() < 200 && signal == 15);
        });
        // ---------- платформа под маскировкой
        BlockPos platform = new BlockPos(1408, -60, 4);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(platform, TTBlocks.PLATFORM.get().defaultBlockState(), 3);
            level.setBlock(platform.east(), TTBlocks.PLATFORM.get().defaultBlockState(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MOSSY_COBBLESTONE));
            level.getBlockState(platform).use(level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(platform), Direction.UP, platform, false));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var camo = ((PlatformBlock.Tile) level.getBlockEntity(platform)).getCamo();
            check("платформа замаскирована под замшелый булыжник", camo != null && camo.is(Blocks.MOSSY_COBBLESTONE)
                    && level.getBlockState(platform).getValue(PlatformBlock.CAMO));
            var below = net.minecraft.world.entity.EntityType.PIG.create(level);
            below.setPos(platform.getX() + 0.5, platform.getY() - 1.5, platform.getZ() + 0.5);
            var above = net.minecraft.world.entity.EntityType.PIG.create(level);
            above.setPos(platform.getX() + 0.5, platform.getY() + 1.0, platform.getZ() + 0.5);
            boolean blocksBelow = !level.getBlockState(platform).getCollisionShape(level, platform, CollisionContext.of(below)).isEmpty();
            boolean holdsAbove = !level.getBlockState(platform).getCollisionShape(level, platform, CollisionContext.of(above)).isEmpty();
            check("платформа держит стоящего сверху (" + holdsAbove + ") и пропускает снизу (" + !blocksBelow + ")",
                    holdsAbove && !blocksBelow);
            // анализатор аспектов рядом
            level.setBlock(new BlockPos(1406, -60, 6), TTBlocks.ASPECT_ANALYZER.get().defaultBlockState(), 3);
        });
        // ---------- талисманы, шлем, том
        server(5, (server, player) -> {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            ItemStack xp = new ItemStack(TTItems.XP_TALISMAN.get());
            xp.getOrCreateTag().putBoolean("absorbing", true);
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("necklace").ifPresent(slots -> slots.getStacks().setStackInSlot(0, xp)));
            ServerLevel level = player.serverLevel();
            for (int i = 0; i < 5; i++) {
                level.addFreshEntity(new ExperienceOrb(level, player.getX() + 1, player.getY() + 0.5, player.getZ(), 7));
            }
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(TTItems.REVEALING_HELM.get()));
        });
        server(40, (server, player) -> {
            var found = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(TTItems.XP_TALISMAN.get()).map(r -> r.stack()).orElse(ItemStack.EMPTY))
                    .orElse(ItemStack.EMPTY);
            check("талисман удержания собрал опыт: " + XpTalismanItem.getXp(found), XpTalismanItem.getXp(found) >= 30);
            if (!found.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, found.copy());
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
                int before = XpTalismanItem.getXp(player.getMainHandItem());
                TTItems.XP_TALISMAN.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                check("талисман разлил 10 опыта в пузырёк (" + before + " -> " + XpTalismanItem.getXp(player.getMainHandItem()) + ")",
                        XpTalismanItem.getXp(player.getMainHandItem()) == before - 10
                                && player.getInventory().countItem(Items.EXPERIENCE_BOTTLE) == 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            check("шлем откровения показывает узлы и даёт скидку", thaumcraft.api.nodes.Revealer.wornBy(player)
                    && thaumcraft.api.Goggles.wornBy(player));
            // том обмена знаниями: записать, забыть, вспомнить
            var knowledge = PlayerKnowledge.get(player);
            knowledge.completeResearch("GASEOUS_LIGHT");
            ItemStack tome = new ItemStack(TTItems.SHARE_TOME.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, tome);
            TTItems.SHARE_TOME.get().use(player.level(), player, InteractionHand.MAIN_HAND);
            boolean written = tome.hasTag() && tome.getTag().getList("research", 8).size() > 3;
            check("том записал исследования (" + written + ")", written);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            // талисман исцеления — в ту же ячейку амулета вместо талисмана удержания
            ItemStack cleansing = new ItemStack(TTItems.CLEANSING_TALISMAN.get());
            cleansing.getOrCreateTag().putBoolean("enabled", true);
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getStacksHandler("necklace").ifPresent(slots -> slots.getStacks().setStackInSlot(0, cleansing)));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        });
        server(45, (server, player) -> {
            ItemStack cleansing = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(TTItems.CLEANSING_TALISMAN.get()).map(r -> r.stack()).orElse(ItemStack.EMPTY))
                    .orElse(ItemStack.EMPTY);
            check("талисман исцеления снял отравление (" + !player.hasEffect(MobEffects.POISON) + ", износ " + cleansing.getDamageValue()
                    + ")", !cleansing.isEmpty() && !player.hasEffect(MobEffects.POISON) && cleansing.getDamageValue() == 1);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        });
        // ---------- снимки
        cmd("tp @s 1404.5 -59 -1.5 0 25");
        shot(30, "tt_devices");
        cmd("tp @s 1402.5 -59.5 2.5 0 50");
        shot(20, "tt_funnel_repairer_close");
        client(2, mc -> mc.setScreen(new ItemGrid()));
        shot(10, "tt_items");
        client(2, mc -> mc.setScreen(null));
        cmd("thaumcraft research all");
        client(20, mc -> {
            thaumcraft.client.gui.ResearchBrowserScreen.selectCategory("TT_CATEGORY");
            mc.setScreen(new thaumcraft.client.gui.ResearchBrowserScreen());
        });
        shot(15, "tt_book");
        client(2, mc -> mc.setScreen(new thaumcraft.client.gui.ResearchPageScreen(ResearchRegistry.get("REPAIRER"))));
        shot(10, "tt_page_repairer");
        AutoShot.close();
        // анализатор: открыть окно с изученным железным слитком
        server(5, (server, player) -> {
            PlayerKnowledge.get(player).addScanned(thaumcraft.common.research.ScanManager.itemKey(new ItemStack(Items.IRON_INGOT)));
            thaumcraft.common.network.SyncKnowledgePacket.send(player);
            BlockPos analyzer = new BlockPos(1406, -60, 6);
            var tile = (thaumictinkerer.common.block.AspectAnalyzerBlock.Tile) player.level().getBlockEntity(analyzer);
            tile.setItem(0, new ItemStack(Items.IRON_INGOT));
            net.minecraftforge.network.NetworkHooks.openScreen(player, tile, analyzer);
        });
        shot(15, "tt_analyzer");
        AutoShot.close();
    }

    /** Все предметы аддона сеткой. */
    private static final class ItemGrid extends Screen {
        ItemGrid() {
            super(Component.empty());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xff8b8b8b);
            int i = 0;
            for (var item : TTItems.ITEMS.getEntries()) {
                int x = 10 + i % 10 * 22;
                int y = 10 + i / 10 * 22;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff555555);
                graphics.renderItem(new ItemStack(item.get()), x, y);
                i++;
            }
            graphics.drawString(Minecraft.getInstance().font, "Thaumic Tinkerer: " + i, 10, height - 20, 0xffffff);
        }
    }
}

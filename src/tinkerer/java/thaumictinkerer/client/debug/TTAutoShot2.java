package thaumictinkerer.client.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.client.debug.AutoShot;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchRegistry;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.EnchanterBlock;
import thaumictinkerer.common.enchantment.EnchanterData;
import thaumictinkerer.common.enchantment.TTEnchantments;
import thaumictinkerer.common.item.focus.TTFoci;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка второй части аддона: фокусы, чары и осмотический зачарователь (-Only tinkerer2). */
public final class TTAutoShot2 {
    private TTAutoShot2() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer2", TTAutoShot2::script);
    }

    private static ItemStack wand(net.minecraft.world.item.Item focus) {
        ItemStack wand = WandItem.create(ModItems.WAND.get(), WandRod.SILVERWOOD, WandCap.THAUMIUM, true);
        WandItem.setFocus(wand, new ItemStack(focus));
        return wand;
    }

    private static void script() {
        cmd("time set noon");
        cmd("gamemode survival");
        cmd("clear @s");
        cmd("fill 1496 -60 -6 1512 -48 10 minecraft:air");
        cmd("tp @s 1504.5 -60 0.5 0 20");
        server(10, (server, player) -> {
            int found = 0;
            for (String key : new String[]{"FOCUS_SMELT", "FOCUS_FLIGHT", "FOCUS_TELEKINESIS", "FOCUS_DEFLECT", "FOCUS_DISLOCATION",
                    "FOCUS_HEAL", "FOCUS_ENDER_CHEST", "ENCHANTER", "TTENCH_VAMPIRISM", "TTENCH_FINAL"}) {
                found += ResearchRegistry.get(key) != null ? 1 : 0;
            }
            long recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(recipe -> recipe.getId().getNamespace().equals(ThaumicTinkerer.MODID)).count();
            check("часть 2: исследования на месте (" + found + " из 10), рецептов аддона " + recipes, found == 10 && recipes == 28);
            ServerLevel level = player.serverLevel();
            // полёт
            ItemStack flight = wand(TTItems.FOCUS_FLIGHT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, flight);
            player.setXRot(-60);
            ((FocusItem) TTItems.FOCUS_FLIGHT.get()).onFocusRightClick(flight, level, player, InteractionHand.MAIN_HAND);
            check("фокус подъёма бросает вверх (скорость " + String.format("%.2f", player.getDeltaMovement().y) + ")",
                    player.getDeltaMovement().y > 0.4);
            player.setXRot(20);
            player.setDeltaMovement(Vec3.ZERO);
            // лечение
            ItemStack heal = wand(TTItems.FOCUS_HEAL.get());
            player.setHealth(10f);
            for (int i = 0; i < 40; i++) {
                ((FocusItem) TTItems.FOCUS_HEAL.get()).onUsingFocusTick(heal, player, i);
            }
            check("фокус лечения вернул здоровье (" + player.getHealth() + ")", player.getHealth() > 10f);
            // переплавка булыжника в камень
            BlockPos cobble = player.blockPosition().relative(Direction.SOUTH, 2).above();
            level.setBlock(cobble, Blocks.COBBLESTONE.defaultBlockState(), 3);
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(cobble));
            ItemStack smelt = wand(TTItems.FOCUS_SMELT.get());
            for (int i = 0; i < 25; i++) {
                ((FocusItem) TTItems.FOCUS_SMELT.get()).onUsingFocusTick(smelt, player, i);
            }
            check("пламя ифрита переплавило булыжник в камень (" + level.getBlockState(cobble).getBlock() + ")",
                    level.getBlockState(cobble).is(Blocks.STONE));
            // перемещение сундука с содержимым
            BlockPos chest = cobble;
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 3));
            ItemStack dislocation = wand(TTItems.FOCUS_DISLOCATION.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, dislocation);
            ((FocusItem) TTItems.FOCUS_DISLOCATION.get()).onFocusRightClick(dislocation, level, player, InteractionHand.MAIN_HAND);
            boolean picked = level.isEmptyBlock(chest) && TTFoci.Dislocation.picked(WandItem.getFocusStack(dislocation)) != null;
            level.setBlock(chest, Blocks.STONE.defaultBlockState(), 3);
            ((FocusItem) TTItems.FOCUS_DISLOCATION.get()).onFocusRightClick(dislocation, level, player, InteractionHand.MAIN_HAND);
            BlockPos placed = chest.relative(Direction.NORTH);
            boolean back = level.getBlockState(placed).getBlock() instanceof ChestBlock
                    && level.getBlockEntity(placed) instanceof ChestBlockEntity box && box.getItem(0).getCount() == 3;
            check("фокус перемещения забрал сундук (" + picked + ") и поставил с алмазами внутри (" + back + ")", picked && back);
            level.setBlock(placed, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
            // телекинез
            ItemEntity item = new ItemEntity(level, player.getX() + 4, player.getY() + 1, player.getZ() + 4, new ItemStack(Items.APPLE));
            level.addFreshEntity(item);
            ItemStack telekinesis = wand(TTItems.FOCUS_TELEKINESIS.get());
            player.setShiftKeyDown(true);
            ((FocusItem) TTItems.FOCUS_TELEKINESIS.get()).onUsingFocusTick(telekinesis, player, 1);
            player.setShiftKeyDown(false);
            Vec3 pull = item.getDeltaMovement();
            check("телекинез тянет яблоко к игроку", pull.x < 0 && pull.z < 0);
            item.discard();
            // отражение стрелы
            Arrow arrow = new Arrow(EntityType.ARROW, level);
            arrow.setPos(player.getX() + 2, player.getY() + 1, player.getZ());
            arrow.setDeltaMovement(-1, 0, 0);
            level.addFreshEntity(arrow);
            ItemStack deflect = wand(TTItems.FOCUS_DEFLECT.get());
            ((FocusItem) TTItems.FOCUS_DEFLECT.get()).onUsingFocusTick(deflect, player, 1);
            check("отражение развернуло стрелу (скорость по x " + String.format("%.2f", arrow.getDeltaMovement().x) + ")",
                    arrow.getDeltaMovement().x > 0);
            arrow.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        // зачарователь с шестью столбами
        BlockPos table = new BlockPos(1504, -60, 4);
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            level.setBlock(table, TTBlocks.ENCHANTER.get().defaultBlockState(), 3);
            int[][] spots = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {-4, 0}, {4, 0}};
            for (int[] spot : spots) {
                for (int h = 0; h < 3; h++) {
                    level.setBlock(table.offset(spot[0], h, spot[1]), ModBlocks.OBSIDIAN_TOTEM.get().defaultBlockState(), 3);
                }
                level.setBlock(table.offset(spot[0], 3, spot[1]), ModBlocks.NITOR.get().defaultBlockState(), 3);
            }
            var tile = (EnchanterBlock.Tile) level.getBlockEntity(table);
            tile.setItem(0, new ItemStack(Items.IRON_SWORD));
            ItemStack wand = WandItem.create(ModItems.WAND.get(), WandRod.SILVERWOOD, WandCap.THAUMIUM, true);
            tile.setItem(1, wand);
            PlayerKnowledge.get(player).completeResearch("TTENCH_VAMPIRISM");
            tile.click(player, EnchanterBlock.ADD + EnchanterData.indexOf(Enchantments.SHARPNESS));
            tile.click(player, EnchanterBlock.EDIT + 1 + 1);
            tile.click(player, EnchanterBlock.ADD + EnchanterData.indexOf(TTEnchantments.VAMPIRISM.get()));
            check("зачарователь: выбраны острота " + tile.levels.get(0) + " и вампиризм, цена " + tile.totalAspects.visSize() + " вис",
                    tile.enchantments.size() == 2 && tile.levels.get(0) == 2 && tile.checkPillars());
            tile.click(player, EnchanterBlock.START);
        });
        server(40, (server, player) -> {
            var tile = (EnchanterBlock.Tile) player.level().getBlockEntity(table);
            check("зачарователь работает: идёт (" + tile.working + "), столбы (" + tile.checkPillars() + "), собрано "
                    + tile.currentAspects.visSize() + " из " + tile.totalAspects.visSize(), tile.working && tile.currentAspects.visSize() > 0);
            net.minecraftforge.network.NetworkHooks.openScreen(player, tile, table);
        });
        cmd("tp @s 1504.5 -59 -1.5 0 30");
        shot(15, "tt2_enchanter_gui");
        AutoShot.close();
        shot(10, "tt2_enchanter_pillars");
        server(300, (server, player) -> {
            var tile = (EnchanterBlock.Tile) player.level().getBlockEntity(table);
            ItemStack sword = tile.tool();
            check("меч зачарован: острота " + sword.getEnchantmentLevel(Enchantments.SHARPNESS) + ", вампиризм "
                            + sword.getEnchantmentLevel(TTEnchantments.VAMPIRISM.get()),
                    sword.getEnchantmentLevel(Enchantments.SHARPNESS) == 2 && sword.getEnchantmentLevel(TTEnchantments.VAMPIRISM.get()) == 1);
            // вампиризм лечит того, кто бьёт
            player.setHealth(10f);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword.copy());
            var pig = EntityType.PIG.create(player.level());
            pig.setPos(player.getX() + 1, player.getY(), player.getZ());
            player.level().addFreshEntity(pig);
            player.attack(pig);
            check("вампиризм вылечил атакующего (" + player.getHealth() + ")", player.getHealth() > 10f);
            pig.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
    }
}

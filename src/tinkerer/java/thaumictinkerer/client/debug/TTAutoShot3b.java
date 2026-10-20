package thaumictinkerer.client.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.debug.AutoShot;
import thaumictinkerer.common.item.kami.BlockTalismanItem;
import thaumictinkerer.common.item.kami.PlacementMirrorItem;
import thaumictinkerer.common.item.kami.SoulHearts;
import thaumictinkerer.common.registry.TTItems;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка подпартии 3б: пробуждённые инструменты, сердца души, протоглина, кольцо и зеркало (-Only tinkerer3b). */
public final class TTAutoShot3b {
    private TTAutoShot3b() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer3b", TTAutoShot3b::script);
    }

    private static ItemStack tool(net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item, int mode) {
        ItemStack stack = new ItemStack(item.get());
        stack.getOrCreateTag().putInt("mode", mode);
        return stack;
    }

    private static boolean air(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static void script() {
        cmd("time set noon");
        cmd("gamemode survival");
        cmd("difficulty easy");
        cmd("fill 1996 -60 -6 2016 -48 14 minecraft:air");
        cmd("fill 1996 -61 -6 2016 -61 14 minecraft:grass_block");
        cmd("tp @s 2004.5 -60 -2.5 0 0");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2000, -60, 0), new BlockPos(2008, -55, 12))) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
            BlockPos face = new BlockPos(2004, -59, 0);
            // протоглина: кирка в руке меняется на лопату, когда бьём землю
            level.setBlock(face, Blocks.DIRT.defaultBlockState(), 3);
            player.getInventory().clearContent();
            player.getInventory().setItem(0, tool(TTItems.ICHOR_PICK_GEM, 0));
            player.getInventory().setItem(5, tool(TTItems.ICHOR_SHOVEL_GEM, 0));
            ItemStack clay = new ItemStack(TTItems.PROTOCLAY.get());
            player.getInventory().setItem(8, clay);
            player.getInventory().selected = 0;
            player.swinging = true;
            clay.inventoryTick(level, player, 8, false);
            player.swinging = false;
            check("протоглина подменила кирку лопатой", player.getMainHandItem().is(TTItems.ICHOR_SHOVEL_GEM.get()));
            level.setBlock(face, Blocks.STONE.defaultBlockState(), 3);
            // зеркало: плоскость 3×3 камня перед стеной
            player.getInventory().clearContent();
            ItemStack mirror = new ItemStack(TTItems.PLACEMENT_MIRROR.get());
            player.getInventory().setItem(0, mirror);
            player.getInventory().setItem(1, new ItemStack(Items.STONE, 20));
            player.setShiftKeyDown(true);
            mirror.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(face), Direction.NORTH, face, false)));
            player.setShiftKeyDown(false);
            int planned = PlacementMirrorItem.targets(mirror, player).size();
            mirror.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(face), Direction.NORTH, face, false)));
            boolean wall = !air(level, new BlockPos(2003, -59, -1), new BlockPos(2005, -57, -1));
            check("зеркало поставило " + planned + " камня, осталось " + player.getInventory().getItem(1).getCount(),
                    planned == 9 && wall && player.getInventory().getItem(1).getCount() == 11);
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2003, -59, -1), new BlockPos(2005, -57, -1))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        });
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos face = new BlockPos(2004, -59, 0);
            // кирка: квадрат 5×5, затем линия в 10 блоков
            player.getInventory().clearContent();
            player.getInventory().setItem(0, tool(TTItems.ICHOR_PICK_GEM, 1));
            player.gameMode.destroyBlock(face);
            check("пробуждённая кирка выломала квадрат 5×5", air(level, new BlockPos(2002, -60, 0), new BlockPos(2006, -56, 0))
                    && !level.getBlockState(new BlockPos(2004, -59, 1)).isAir());
            player.getInventory().setItem(0, tool(TTItems.ICHOR_PICK_GEM, 2));
            player.gameMode.destroyBlock(face.south());
            check("пробуждённая кирка пробила линию в 10 блоков", air(level, new BlockPos(2004, -59, 1), new BlockPos(2004, -59, 10))
                    && !level.getBlockState(new BlockPos(2004, -59, 11)).isAir());
            // топор: всё дерево
            for (int y = -60; y <= -54; y++) {
                level.setBlock(new BlockPos(2012, y, 4), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
            level.setBlock(new BlockPos(2013, -55, 4), Blocks.OAK_LOG.defaultBlockState(), 3);
            player.getInventory().setItem(0, tool(TTItems.ICHOR_AXE_GEM, 2));
            player.gameMode.destroyBlock(new BlockPos(2012, -60, 4));
            check("пробуждённый топор свалил дерево целиком", air(level, new BlockPos(2012, -60, 4), new BlockPos(2013, -54, 4)));
            // лопата: столб земли
            for (int y = -60; y <= -50; y++) {
                level.setBlock(new BlockPos(2014, y, 8), Blocks.DIRT.defaultBlockState(), 3);
            }
            player.getInventory().setItem(0, tool(TTItems.ICHOR_SHOVEL_GEM, 2));
            player.gameMode.destroyBlock(new BlockPos(2014, -55, 8));
            check("пробуждённая лопата выкопала столб", air(level, new BlockPos(2014, -60, 8), new BlockPos(2014, -50, 8)));
            // меч: по площади задевает второго зомби, в режиме души даёт сердце
            Zombie first = EntityType.ZOMBIE.create(level);
            Zombie second = EntityType.ZOMBIE.create(level);
            first.setPos(player.getX(), player.getY(), player.getZ() - 2);
            second.setPos(player.getX() + 1, player.getY(), player.getZ() - 2.5);
            level.addFreshEntity(first);
            level.addFreshEntity(second);
            player.getInventory().setItem(0, tool(TTItems.ICHOR_SWORD_GEM, 1));
            player.attack(first);
            check("меч по площади ранил и второго зомби (" + second.getHealth() + ")", second.getHealth() < second.getMaxHealth());
            first.discard();
            second.discard();
            Pig pig = EntityType.PIG.create(level);
            pig.setPos(player.getX(), player.getY(), player.getZ() - 2);
            level.addFreshEntity(pig);
            SoulHearts.set(player, 0);
            player.getInventory().setItem(0, tool(TTItems.ICHOR_SWORD_GEM, 2));
            player.attack(pig);
            int hearts = SoulHearts.get(player);
            float health = player.getHealth();
            player.hurt(player.damageSources().generic(), 1f);
            check("меч души дал сердце (" + hearts + "), оно приняло удар", hearts == 1 && SoulHearts.get(player) == 0 && player.getHealth() == health);
            pig.discard();
            // кольцо: запоминает булыжник и вбирает лишние стопки
            ItemStack ring = new ItemStack(TTItems.BLOCK_TALISMAN.get());
            BlockPos cobble = new BlockPos(2000, -60, -4);
            level.setBlock(cobble, Blocks.COBBLESTONE.defaultBlockState(), 3);
            player.getInventory().setItem(0, ring);
            ring.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cobble), Direction.UP, cobble, false)));
            ring.getOrCreateTag().putBoolean("active", true);
            player.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(11, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(12, new ItemStack(Items.COBBLESTONE, 10));
            ((BlockTalismanItem) ring.getItem()).curioTick(new top.theillusivec4.curios.api.SlotContext("ring", player, 0, false, true), ring);
            int stacks = 0;
            for (ItemStack stack : player.getInventory().items) {
                stacks += stack.is(Items.COBBLESTONE) ? 1 : 0;
            }
            check("кольцо вобрало булыжник: в кольце " + BlockTalismanItem.count(ring) + ", стопок " + stacks,
                    BlockTalismanItem.count(ring) == 74 && stacks == 1);
            // для снимка: сердца души и зеркало с предпросмотром
            SoulHearts.set(player, 7);
            player.getInventory().clearContent();
            ItemStack shown = new ItemStack(TTItems.PLACEMENT_MIRROR.get());
            shown.getOrCreateTag().put("blockState", net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.GLASS.defaultBlockState()));
            player.getInventory().setItem(0, shown);
            player.getInventory().setItem(1, tool(TTItems.ICHOR_PICK_GEM, 1));
            player.getInventory().setItem(2, tool(TTItems.ICHOR_SHOVEL_GEM, 2));
            player.getInventory().setItem(3, tool(TTItems.ICHOR_AXE_GEM, 0));
            player.getInventory().setItem(4, tool(TTItems.ICHOR_SWORD_GEM, 1));
            player.getInventory().setItem(5, new ItemStack(TTItems.ICHOR_PICK.get()));
            player.getInventory().setItem(6, ring);
            player.getInventory().setItem(7, new ItemStack(TTItems.PROTOCLAY.get()));
            player.getInventory().selected = 0;
        });
        cmd("tp @s 2001.5 -60 -2.5 0 10");
        shot(30, "tt3b_tools");
        cmd("gamemode creative");
    }
}

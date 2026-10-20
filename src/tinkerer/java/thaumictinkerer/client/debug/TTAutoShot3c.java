package thaumictinkerer.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.debug.AutoShot;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModItems;
import thaumictinkerer.client.gui.WarpGateScreens;
import thaumictinkerer.common.block.WarpGateBlock;
import thaumictinkerer.common.item.kami.IchorArmor;
import thaumictinkerer.common.item.kami.Kami;
import thaumictinkerer.common.item.kami.KamiFoci;
import thaumictinkerer.common.item.kami.SkyPearlItem;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка подпартии 3в: пробуждённая мантия, врата искривления, фокусы Ками (-Only tinkerer3c). */
public final class TTAutoShot3c {
    private static final BlockPos GATE_A = new BlockPos(2104, -61, 2);
    private static final BlockPos GATE_B = new BlockPos(2114, -61, 10);

    private TTAutoShot3c() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer3c", TTAutoShot3c::script);
    }

    private static ItemStack wand() {
        ItemStack wand = WandItem.create(ModItems.WAND.get(), Kami.ROD, Kami.CAP, true);
        return wand;
    }

    private static void script() {
        cmd("time set noon");
        cmd("gamemode survival");
        cmd("difficulty easy");
        cmd("tp @s 2104.5 -60 -2.5 0 20");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2096, -60, -6), new BlockPos(2120, -50, 16))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(GATE_A, TTBlocks.WARP_GATE.get().defaultBlockState(), 3);
            level.setBlock(GATE_B, TTBlocks.WARP_GATE.get().defaultBlockState(), 3);
            // пробуждённая мантия целиком
            player.getInventory().clearContent();
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TTItems.ICHORCLOTH_HELM_GEM.get()));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TTItems.ICHORCLOTH_CHEST_GEM.get()));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(TTItems.ICHORCLOTH_LEGS_GEM.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(TTItems.ICHORCLOTH_BOOTS_GEM.get()));
            IchorArmor.setEnabled(player, true);
            // жемчужина врат B лежит во вратах A
            ItemStack pearl = new ItemStack(TTItems.SKY_PEARL.get());
            SkyPearlItem.attune(pearl, GATE_B, level.dimension());
            ((WarpGateBlock.Tile) level.getBlockEntity(GATE_A)).items.setItem(0, pearl);
            ItemStack back = new ItemStack(TTItems.SKY_PEARL.get());
            SkyPearlItem.attune(back, GATE_A, level.dimension());
            back.setHoverName(net.minecraft.network.chat.Component.literal("Дом"));
            ((WarpGateBlock.Tile) level.getBlockEntity(GATE_A)).items.setItem(1, back);
        });
        server(20, (server, player) -> {
            ServerLevel level = player.serverLevel();
            check("мантия стратосферы дала полёт", player.getAbilities().mayfly);
            check("поножи дали огнестойкость", player.hasEffect(MobEffects.FIRE_RESISTANCE));
            check("сапоги дали спешку и шаг на блок", player.hasEffect(MobEffects.DIG_SPEED)
                    && player.getAttributeValue(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get()) >= 0.5);
            player.setHealth(10f);
            player.hurt(player.damageSources().inFire(), 2f);
            check("огонь лечит владельца поножей (" + player.getHealth() + ")", player.getHealth() > 10f);
            IchorArmor.setEnabled(player, false);
            // врата: встать на A и перенестись по жемчужине к B
            player.teleportTo(GATE_A.getX() + 0.5, GATE_A.getY() + 1, GATE_A.getZ() + 0.5);
            ((WarpGateBlock.Tile) level.getBlockEntity(GATE_A)).teleport(player, 0);
            check("врата перенесли игрока к вратам B (" + player.blockPosition().toShortString() + ")",
                    player.blockPosition().closerThan(GATE_B.above(), 1.5));
            ((WarpGateBlock.Tile) level.getBlockEntity(GATE_A)).locked = true;
            player.teleportTo(GATE_B.getX() + 0.5, GATE_B.getY() + 1, GATE_B.getZ() + 0.5);
            boolean refused = !WarpGateBlock.teleport(player, GATE_A);
            check("запертые врата не принимают гостей", refused);
            ((WarpGateBlock.Tile) level.getBlockEntity(GATE_A)).locked = false;
            // высасывание опыта: 15 очков — 5 вис первоэлемента
            ItemStack drain = WandItem.create(ModItems.WAND.get(), Kami.ROD, Kami.CAP, false);
            WandItem.setFocus(drain, new ItemStack(TTItems.FOCUS_XP_DRAIN.get()));
            player.giveExperiencePoints(100);
            int before = player.totalExperience;
            for (int i = 0; i < 3; i++) {
                ((KamiFoci.XpDrain) TTItems.FOCUS_XP_DRAIN.get()).onUsingFocusTick(drain, player, 0);
            }
            int vis = 0;
            for (Aspect aspect : Aspect.primals()) {
                vis += WandItem.getVis(drain, aspect);
            }
            check("высасывание опыта: вис " + vis + ", опыта " + before + " -> " + player.totalExperience, vis == 1500);
            // теневой луч ранит зомби впереди
            Zombie zombie = EntityType.ZOMBIE.create(level);
            zombie.setPos(player.getX(), player.getY(), player.getZ() + 4);
            zombie.setNoAi(true);
            level.addFreshEntity(zombie);
            player.setYRot(0);
            player.setXRot(0);
            KamiFoci.Shadowbeam.fire(level, player);
            check("теневой луч ранил зомби (" + zombie.getHealth() + ")", zombie.getHealth() < zombie.getMaxHealth());
            zombie.discard();
            // возврат: жемчужина врат A в хотбаре, удерживаем жезл
            ItemStack recall = wand();
            WandItem.setFocus(recall, new ItemStack(TTItems.FOCUS_RECALL.get()));
            ItemStack home = new ItemStack(TTItems.SKY_PEARL.get());
            SkyPearlItem.attune(home, GATE_A, level.dimension());
            player.getInventory().setItem(0, recall);
            player.getInventory().setItem(1, home);
            player.getInventory().selected = 0;
            player.teleportTo(GATE_B.getX() + 3.5, GATE_B.getY() + 1, GATE_B.getZ() + 0.5);
            player.getInventory().getItem(0).use(level, player, InteractionHand.MAIN_HAND);
        });
        server(80, (server, player) -> {
            ItemStack recall = player.getInventory().getItem(0);
            check("возврат перенёс к вратам A и взял вис (" + player.blockPosition().toShortString() + ", аер "
                    + WandItem.getVis(recall, Aspect.AIR) / 100 + ")", player.blockPosition().closerThan(GATE_A.above(), 1.5)
                    && WandItem.getVis(recall, Aspect.AIR) < WandItem.getMaxVis(recall)
                    && WandItem.getVis(recall, Aspect.AIR) >= WandItem.getMaxVis(recall) - 4000);
            IchorArmor.setEnabled(player, true);
            player.getInventory().setItem(2, new ItemStack(TTItems.FOCUS_SHADOWBEAM.get()));
            player.getInventory().setItem(3, new ItemStack(TTItems.FOCUS_XP_DRAIN.get()));
            player.getInventory().setItem(4, new ItemStack(TTItems.WARP_GATE.get()));
            player.teleportTo(GATE_A.getX() + 0.5, GATE_A.getY() + 1, GATE_A.getZ() - 2.5);
        });
        shot(20, "tt3c_armor");
        AutoShot.screen(5, () -> new WarpGateScreens.Destinations((WarpGateBlock.Tile) Minecraft.getInstance().level.getBlockEntity(GATE_A)));
        shot(20, "tt3c_destinations");
        AutoShot.close();
        cmd("gamemode creative");
    }
}

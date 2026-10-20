package thaumictinkerer.client.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.debug.AutoShot;
import thaumcraft.common.items.wands.FocusPouchItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchRegistry;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.kami.Kami;
import thaumictinkerer.common.registry.TTItems;

import java.util.ArrayList;
import java.util.List;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка подпартии 3а: ихор, ихоровый жезл, бездонный подсумок, котячий амулет (-Only tinkerer3a). */
public final class TTAutoShot3a {
    private TTAutoShot3a() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer3a", TTAutoShot3a::script);
    }

    private static void script() {
        cmd("time set noon");
        cmd("gamemode survival");
        cmd("difficulty easy");
        cmd("fill 1898 -60 -6 1912 -54 12 minecraft:air");
        cmd("tp @s 1904.5 -60 -2.5 0 20");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            ResearchEntry ichor = ResearchRegistry.get("ICHOR");
            boolean all = ichor != null && ichor.parentsHidden.size() > 150 && ichor.parentsHidden.contains("ENCHANTER")
                    && ichor.parentsHidden.contains("INFUSION") && !ichor.parentsHidden.contains("ICHOR_CLOTH");
            check("ихор требует все обычные исследования (" + (ichor == null ? "нет" : ichor.parentsHidden.size()) + ")", all);
            List<String> missing = new ArrayList<>();
            for (String id : List.of("infusion/ichor", "ichor_cloth", "ichorium", "ichorium_nugget", "ichorium_cap", "infusion/ichorcloth_rod",
                    "infusion/ichor_pouch", "infusion/cat_amulet")) {
                if (server.getRecipeManager().byKey(ThaumicTinkerer.id(id)).isEmpty()) {
                    missing.add(id);
                }
            }
            check("рецепты 3а загружены " + missing, missing.isEmpty());
            ItemStack wand = WandItem.create(ModItems.WAND.get(), Kami.ROD, Kami.CAP, true);
            check("ихоровый жезл: " + WandItem.getMaxVis(wand) / 100 + " вис, цена " + WandItem.getCap(wand).getCostModifier(Aspect.FIRE),
                    WandItem.getMaxVis(wand) == 100000 && WandItem.getCap(wand).getCostModifier(Aspect.FIRE) == 0.7f);
            player.setItemInHand(InteractionHand.OFF_HAND, wand);
            // подсумок: 117 ячеек, в нём что угодно, фокусы видит жезл
            ItemStack pouch = new ItemStack(TTItems.ICHOR_POUCH.get());
            List<ItemStack> inside = new ArrayList<>(FocusPouchItem.getInventory(pouch));
            inside.set(116, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 64));
            inside.set(3, new ItemStack(ModItems.FOCUS_FIRE.get()));
            FocusPouchItem.setInventory(pouch, inside);
            player.getInventory().setItem(0, pouch);
            player.getInventory().selected = 0;
            List<ItemStack> read = FocusPouchItem.getInventory(FocusPouchItem.findPouches(player).get(0));
            check("подсумок: ячеек " + read.size() + ", алмазы в последней, фокус на месте",
                    read.size() == 117 && read.get(116).getCount() == 64 && read.get(3).is(ModItems.FOCUS_FIRE.get()));
            // амулет кошки: зомби теряет цель, крипер гасит запал
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler("necklace")
                    .ifPresent(slots -> slots.getStacks().setStackInSlot(0, new ItemStack(TTItems.CAT_AMULET.get()))));
            Zombie zombie = EntityType.ZOMBIE.create(level);
            zombie.setPos(player.getX() + 3, player.getY(), player.getZ() + 4);
            level.addFreshEntity(zombie);
            zombie.setTarget(player);
            Creeper creeper = EntityType.CREEPER.create(level);
            creeper.setPos(player.getX() - 3, player.getY(), player.getZ() + 4);
            level.addFreshEntity(creeper);
            creeper.setTarget(player);
        });
        server(40, (server, player) -> {
            ServerLevel level = player.serverLevel();
            List<Zombie> zombies = level.getEntitiesOfClass(Zombie.class, new AABB(player.blockPosition()).inflate(12));
            List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, new AABB(player.blockPosition()).inflate(12));
            check("котячий амулет: зомби не преследует игрока", !zombies.isEmpty() && zombies.stream().noneMatch(z -> z.getTarget() == player));
            check("котячий амулет: крипер не взорвался и не целится", !creepers.isEmpty()
                    && creepers.stream().allMatch(c -> c.getTarget() == null && c.getSwellDir() <= 0));
            zombies.forEach(net.minecraft.world.entity.Entity::discard);
            creepers.forEach(net.minecraft.world.entity.Entity::discard);
            for (var item : List.of(TTItems.ICHOR, TTItems.ICHOR_CLOTH, TTItems.ICHORIUM, TTItems.ICHORIUM_NUGGET, TTItems.ICHORIUM_CAP,
                    TTItems.ICHORCLOTH_ROD, TTItems.NETHER_SHARD, TTItems.ENDER_SHARD)) {
                player.getInventory().add(new ItemStack(item.get()));
            }
            player.getInventory().add(new ItemStack(TTItems.CAT_AMULET.get()));
            // открыть подсумок, как ПКМ
            TTItems.ICHOR_POUCH.get().use(level, player, InteractionHand.MAIN_HAND);
        });
        shot(20, "tt3a_pouch");
        AutoShot.close();
        shot(15, "tt3a_items");
        cmd("gamemode creative");
    }
}

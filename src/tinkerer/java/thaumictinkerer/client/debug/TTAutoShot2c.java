package thaumictinkerer.client.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.debug.AutoShot;
import thaumcraft.common.blockentity.PedestalBlockEntity;
import thaumcraft.common.registry.ModBlocks;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.SummonBlock;
import thaumictinkerer.common.item.SoulAspectItem;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка подпартии 2в: кровавый меч, аспекты души, призыв (-Only tinkerer2c). */
public final class TTAutoShot2c {
    private TTAutoShot2c() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer2c", TTAutoShot2c::script);
    }

    private static void script() {
        BlockPos o = new BlockPos(1704, -60, 4);
        cmd("time set noon");
        cmd("gamemode survival");
        cmd("fill 1698 -60 -4 1710 -55 10 minecraft:air");
        cmd("tp @s 1704.5 -60 -1.5 0 30");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // меч в режиме сбора: свинья роняет аспекты своей души
            ItemStack sword = new ItemStack(TTItems.BLOOD_SWORD.get());
            sword.getOrCreateTag().putBoolean("Activated", true);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            Pig pig = EntityType.PIG.create(level);
            pig.setPos(player.getX(), player.getY(), player.getZ() + 1.5);
            level.addFreshEntity(pig);
            float health = player.getHealth();
            pig.hurt(player.damageSources().playerAttack(player), 1000f);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pig.blockPosition()).inflate(2));
            long souls = drops.stream().filter(d -> d.getItem().getItem() instanceof SoulAspectItem).count();
            check("кровавый меч собрал душу свиньи (аспектов " + souls + " из " + drops.size() + " вещей)", souls == 3 && drops.size() == 3);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            // сгущение девяти аспектов
            TransientCraftingContainer grid = new TransientCraftingContainer(player.containerMenu, 3, 3);
            for (int i = 0; i < 9; i++) {
                grid.setItem(i, SoulAspectItem.of(SoulAspectItem.Kind.PLAIN, Aspect.BEAST));
            }
            ItemStack condensed = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                    .map(r -> r.assemble(grid, server.registryAccess())).orElse(ItemStack.EMPTY);
            check("девять аспектов души сгустились (" + condensed + ", " + SoulAspectItem.aspect(condensed) + ")",
                    condensed.is(TTItems.CONDENSED_SOUL_ASPECT.get()) && SoulAspectItem.aspect(condensed) == Aspect.BEAST);
            boolean infusion = server.getRecipeManager().byKey(new ResourceLocation(ThaumicTinkerer.MODID, "infusion/infused_soul_aspect_bestia"))
                    .isPresent();
            check("рецепт наполненного аспекта души загружен", infusion);
            // призыв: три пьедестала с душой свиньи
            level.setBlock(o, TTBlocks.SUMMON.get().defaultBlockState(), 3);
            BlockPos[] spots = {o.offset(-2, 0, 0), o.offset(2, 0, 0), o.offset(0, 0, 2)};
            Aspect[] pigSoul = {Aspect.BEAST, Aspect.EARTH, Aspect.TRAVEL};
            for (int i = 0; i < 3; i++) {
                level.setBlock(spots[i], ModBlocks.PEDESTAL.get().defaultBlockState(), 3);
                ((PedestalBlockEntity) level.getBlockEntity(spots[i])).setItem(SoulAspectItem.of(SoulAspectItem.Kind.PLAIN, pigSoul[i]));
            }
            int before = level.getEntitiesOfClass(Pig.class, new AABB(o).inflate(4)).size();
            boolean summoned = ((SummonBlock.Tile) level.getBlockEntity(o)).summon();
            int after = level.getEntitiesOfClass(Pig.class, new AABB(o).inflate(4)).size();
            check("пентакль призвал свинью (" + before + " -> " + after + "), пьедесталы опустели",
                    summoned && after == before + 1 && ((PedestalBlockEntity) level.getBlockEntity(spots[0])).getItem().isEmpty());
            for (int i = 0; i < 3; i++) {
                ((PedestalBlockEntity) level.getBlockEntity(spots[i])).setItem(SoulAspectItem.of(SoulAspectItem.Kind.INFUSED, pigSoul[i]));
            }
            player.getInventory().add(new ItemStack(TTItems.BLOOD_SWORD.get()));
            for (var aspect : SoulAspectItem.ASPECTS) {
                player.getInventory().add(SoulAspectItem.of(SoulAspectItem.Kind.CONDENSED, aspect));
            }
        });
        shot(30, "tt2c_summon");
        cmd("gamemode creative");
    }
}

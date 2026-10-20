package thaumictinkerer.client.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.debug.AutoShot;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.InfusedFarmlandBlock;
import thaumictinkerer.common.block.InfusedGrainBlock;
import thaumictinkerer.common.item.InfusedSeedsItem;
import thaumictinkerer.common.potion.TTEffects;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import java.util.List;
import java.util.function.Supplier;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка подпартии 2г: стихийные огни, наполненные культуры и зелья (-Only tinkerer2d). */
public final class TTAutoShot2d {
    private TTAutoShot2d() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer2d", TTAutoShot2d::script);
    }

    /** Тикает огонь, пока сосед не изменится (у огня шанс 1/20 за тик). */
    private static BlockState burn(ServerLevel level, BlockPos fire, Supplier<net.minecraft.world.level.block.Block> kind, BlockPos target) {
        BlockState before = level.getBlockState(target);
        for (int i = 0; i < 400 && level.getBlockState(target) == before; i++) {
            level.setBlock(fire, kind.get().defaultBlockState(), 3);
            level.getBlockState(fire).tick(level, fire, level.random);
        }
        return level.getBlockState(target);
    }

    private static void script() {
        BlockPos o = new BlockPos(1804, -60, 4);
        cmd("time set noon");
        cmd("weather clear");
        cmd("gamemode survival");
        cmd("fill 1796 -60 -6 1812 -54 12 minecraft:air");
        cmd("fill 1796 -61 -6 1812 -61 12 minecraft:grass_block");
        cmd("tp @s 1804.5 -60 -2.5 0 25");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // огни: аква студит лаву, ордо сгущает руду в блок металла, пердицио гасит стихийный огонь
            BlockPos fire = o.offset(-6, 0, 0);
            level.setBlock(fire.east(), Blocks.LAVA.defaultBlockState(), 3);
            check("огонь аквы превратил лаву в обсидиан", burn(level, fire, TTBlocks.FIRE_WATER::get, fire.east()).is(Blocks.OBSIDIAN));
            level.setBlock(fire.east(), Blocks.IRON_ORE.defaultBlockState(), 3);
            BlockState iron = Blocks.IRON_ORE.defaultBlockState();
            for (int i = 0; i < 20 && !iron.is(Blocks.IRON_BLOCK); i++) {
                level.setBlock(fire.east(), Blocks.IRON_ORE.defaultBlockState(), 3);
                iron = burn(level, fire, TTBlocks.FIRE_ORDER::get, fire.east());
            }
            check("огонь ордо сгустил железную руду в железный блок (" + iron.getBlock() + ")", iron.is(Blocks.IRON_BLOCK));
            level.setBlock(fire.east(), TTBlocks.FIRE_AIR.get().defaultBlockState(), 3);
            check("огонь пердицио погасил стихийный огонь до обычного",
                    burn(level, fire, TTBlocks.FIRE_CHAOS::get, fire.east()).is(Blocks.FIRE));
            level.setBlock(fire.east(), Blocks.SAND.defaultBlockState(), 3);
            check("огонь терры превратил песок в землю", burn(level, fire, TTBlocks.FIRE_EARTH::get, fire.east()).is(Blocks.DIRT));
            level.setBlock(fire, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(fire.east(), Blocks.AIR.defaultBlockState(), 3);
            boolean recipes = List.of("crucible/fire_aer", "crucible/fire_ignis", "infusion/infused_seeds_aer", "crucible/infused_potion_aqua")
                    .stream().allMatch(id -> server.getRecipeManager().byKey(ThaumicTinkerer.id(id)).isPresent());
            check("рецепты огней, семян и зелий загружены", recipes);

            // семена: посадка на пашню делает её наполненной
            BlockPos soil = o.offset(0, -1, 0);
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 3);
            ItemStack seeds = InfusedSeedsItem.of(Aspect.AIR, new AspectList());
            player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
            seeds.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false)));
            boolean planted = level.getBlockState(soil).is(TTBlocks.INFUSED_FARMLAND.get())
                    && level.getBlockEntity(o) instanceof InfusedGrainBlock.Tile tile && tile.aspect == Aspect.AIR;
            check("семена аера посажены, пашня стала наполненной", planted);
            // спелое зерно аера даёт семена, зерно аера и след в пашне
            level.setBlock(o, level.getBlockState(o).setValue(CropBlock.AGE, 7), 3);
            level.destroyBlock(o, false);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(o).inflate(1));
            boolean gotSeeds = drops.stream().anyMatch(d -> InfusedSeedsItem.aspect(d.getItem()) == Aspect.AIR);
            boolean gotGrain = drops.stream().anyMatch(d -> d.getItem().is(TTItems.INFUSED_GRAIN_AIR.get()));
            InfusedFarmlandBlock.Tile farm = (InfusedFarmlandBlock.Tile) level.getBlockEntity(soil);
            check("спелое зерно: семена " + gotSeeds + ", зерно аера " + gotGrain + ", след в пашне " + farm.aspects.getAmount(Aspect.AIR),
                    gotSeeds && gotGrain && farm.aspects.getAmount(Aspect.AIR) >= 1);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            // скрещивание: игнис на пашне с сильным следом аера даёт семена люкса
            farm.aspects = new AspectList().add(Aspect.AIR, 5);
            level.setBlock(o, TTBlocks.INFUSED_GRAIN.get().defaultBlockState().setValue(CropBlock.AGE, 7)
                    .setValue(InfusedGrainBlock.TEXTURE, InfusedGrainBlock.texture(Aspect.FIRE)), 3);
            ((InfusedGrainBlock.Tile) level.getBlockEntity(o)).setup(Aspect.FIRE, new AspectList());
            level.destroyBlock(o, false);
            drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(o).inflate(1));
            boolean lux = drops.stream().anyMatch(d -> InfusedSeedsItem.aspect(d.getItem()) == Aspect.LIGHT);
            check("игнис на следе аера дал семена люкса", lux);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            AspectList grainAspects = ItemAspectManager.INSTANCE.getAspects(new ItemStack(TTItems.INFUSED_GRAIN_WATER.get()));
            check("у зерна аквы аспекты " + grainAspects.getAspects(), grainAspects.contains(Aspect.WATER) && grainAspects.contains(Aspect.CROP));

            // зелья: аква студит лаву вокруг, терра ставит стену, игнис поджигает врага
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.addEffect(new MobEffectInstance(TTEffects.WATER.get(), 200));
            level.setBlock(player.blockPosition().offset(1, 0, 1), Blocks.LAVA.defaultBlockState(), 3);
            player.addEffect(new MobEffectInstance(TTEffects.EARTH.get(), 200));
            player.addEffect(new MobEffectInstance(TTEffects.FIRE.get(), 200));
            Pig pig = EntityType.PIG.create(level);
            pig.setPos(player.getX(), player.getY(), player.getZ() + 3);
            level.addFreshEntity(pig);
            player.attack(pig);
            int walls = 0;
            for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-3, -1, -1), player.blockPosition().offset(3, 5, 4))) {
                if (level.getBlockState(pos).is(TTBlocks.FORCEFIELD.get())) {
                    walls++;
                }
            }
            check("зелье терры поставило силовую стену (" + walls + " блоков)", walls > 10);
            // грядка для снимка: семь раскрасок по стадиям и огни
            for (int i = 0; i < 7; i++) {
                BlockPos at = o.offset(i - 3, 0, 3);
                level.setBlock(at.below(), TTBlocks.INFUSED_FARMLAND.get().defaultBlockState(), 3);
                level.setBlock(at, TTBlocks.INFUSED_GRAIN.get().defaultBlockState().setValue(InfusedGrainBlock.TEXTURE, i)
                        .setValue(CropBlock.AGE, Math.min(7, i + 1)), 3);
            }
        });
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            check("зелье аквы застудило лаву", level.getBlockState(player.blockPosition().offset(1, 0, 1)).is(Blocks.OBSIDIAN));
            List<Pig> pigs = level.getEntitiesOfClass(Pig.class, new AABB(player.blockPosition()).inflate(6));
            check("зелье игниса подожгло свинью", pigs.stream().anyMatch(net.minecraft.world.entity.Entity::isOnFire));
            pigs.forEach(net.minecraft.world.entity.Entity::discard);
            BlockPos row = o.offset(-3, 0, 6);
            List<Supplier<net.minecraft.world.level.block.Block>> fires = List.of(TTBlocks.FIRE_AIR::get, TTBlocks.FIRE_IGNIS::get,
                    TTBlocks.FIRE_WATER::get, TTBlocks.FIRE_EARTH::get, TTBlocks.FIRE_ORDER::get, TTBlocks.FIRE_CHAOS::get);
            for (int i = 0; i < fires.size(); i++) {
                level.setBlock(row.offset(i, -1, 0), Blocks.SAND.defaultBlockState(), 3);
                level.setBlock(row.offset(i, -1, 1), Blocks.LAVA.defaultBlockState(), 3);
                level.setBlock(row.offset(i, 0, 0), fires.get(i).get().defaultBlockState(), 3);
            }
            for (int i = 0; i < 7; i++) {
                player.getInventory().add(InfusedSeedsItem.of(i < 6 ? Aspect.primals().get(i) : Aspect.LIGHT, new AspectList()));
            }
            player.getInventory().add(new ItemStack(TTItems.INFUSED_POTION_AIR.get()));
            player.getInventory().add(new ItemStack(TTItems.INFUSED_GRAIN_FIRE.get()));
        });
        shot(30, "tt2d_crops");
        cmd("gamemode creative");
    }
}

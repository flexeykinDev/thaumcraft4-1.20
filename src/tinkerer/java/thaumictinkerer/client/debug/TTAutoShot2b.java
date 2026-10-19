package thaumictinkerer.client.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import thaumcraft.client.debug.AutoShot;
import thaumictinkerer.common.block.AnimationTabletBlock;
import thaumictinkerer.common.block.MagnetBlock;
import thaumictinkerer.common.block.RemotePlacerBlock;
import thaumictinkerer.common.block.TransvectorBlock;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTItems;

import static thaumcraft.client.debug.AutoShot.check;
import static thaumcraft.client.debug.AutoShot.cmd;
import static thaumcraft.client.debug.AutoShot.server;
import static thaumcraft.client.debug.AutoShot.shot;

/** Самосъёмка подпартии 2б: магниты, трансвекторы, левитатор, установщик, табличка (-Only tinkerer2b). */
public final class TTAutoShot2b {
    private TTAutoShot2b() {
    }

    public static void register() {
        AutoShot.EXTRA_SCRIPTS.put("tinkerer2b", TTAutoShot2b::script);
    }

    private static void script() {
        BlockPos o = new BlockPos(1604, -60, 4);
        cmd("time set noon");
        cmd("gamemode creative");
        cmd("fill 1594 -60 -6 1616 -54 16 minecraft:air");
        cmd("tp @s 1604.5 -60 -2.5 0 25");
        server(10, (server, player) -> {
            ServerLevel level = player.serverLevel();
            // интерфейс: предмет, вставленный в интерфейс, попадает в сундук
            BlockPos chest = o.offset(-4, 0, 0);
            BlockPos iface = o.offset(-4, 0, 2);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(iface, TTBlocks.TRANSVECTOR_INTERFACE.get().defaultBlockState(), 3);
            ((TransvectorBlock.Tile) level.getBlockEntity(iface)).setTarget(chest);
            ItemStack left = level.getBlockEntity(iface).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                    .map(handler -> handler.insertItem(0, new ItemStack(Items.APPLE, 5), false)).orElse(new ItemStack(Items.APPLE, 5));
            check("трансвекторный интерфейс передал яблоки в сундук (" + ((ChestBlockEntity) level.getBlockEntity(chest)).getItem(0) + ")",
                    left.isEmpty() && ((ChestBlockEntity) level.getBlockEntity(chest)).getItem(0).getCount() == 5);
            // смещатель: меняет местами камень-цель и землю перед собой
            BlockPos disl = o.offset(-1, 0, 0);
            BlockPos far = o.offset(-1, 0, 3);
            level.setBlock(disl, TTBlocks.TRANSVECTOR_DISLOCATOR.get().defaultBlockState()
                    .setValue(TransvectorBlock.Dislocator.FACING, Direction.UP), 3);
            level.setBlock(disl.above(), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(far, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            ((TransvectorBlock.Tile) level.getBlockEntity(disl)).setTarget(far);
            ((TransvectorBlock.Dislocator.Tile) level.getBlockEntity(disl)).pulse();
            check("смещатель поменял местами землю и золото", level.getBlockState(disl.above()).is(Blocks.GOLD_BLOCK)
                    && level.getBlockState(far).is(Blocks.DIRT));
            // магнит под сигналом тянет предмет
            BlockPos magnet = o.offset(2, 0, 0);
            level.setBlock(magnet, TTBlocks.MAGNET.get().defaultBlockState(), 3);
            level.setBlock(magnet.offset(0, 0, -1), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            ItemEntity item = new ItemEntity(level, magnet.getX() + 0.5, magnet.getY() + 0.2, magnet.getZ() + 4.5, new ItemStack(Items.STICK));
            item.setPickUpDelay(400);
            level.addFreshEntity(item);
            // установщик: блок над собой через одну клетку
            BlockPos placer = o.offset(5, 0, 0);
            level.setBlock(placer, TTBlocks.REMOTE_PLACER.get().defaultBlockState().setValue(RemotePlacerBlock.FACING, Direction.UP), 3);
            RemotePlacerBlock.Tile tile = (RemotePlacerBlock.Tile) level.getBlockEntity(placer);
            tile.setItem(0, new ItemStack(Blocks.STONE, 4));
            tile.setDistance(2);
            tile.place();
            check("установщик поставил камень через клетку и потратил блок", level.getBlockState(placer.above(2)).is(Blocks.STONE)
                    && level.isEmptyBlock(placer.above()) && tile.getItem(0).getCount() == 3);
            // табличка с киркой разбивает камень перед собой
            BlockPos tablet = o.offset(7, 0, 2);
            level.setBlock(tablet, TTBlocks.ANIMATION_TABLET.get().defaultBlockState().setValue(AnimationTabletBlock.FACING, Direction.SOUTH), 3);
            level.setBlock(tablet.south(), Blocks.COBBLESTONE.defaultBlockState(), 3);
            ((AnimationTabletBlock.Tile) level.getBlockEntity(tablet)).setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
            // левитатор между ретрансляторами
            BlockPos relayA = o.offset(0, 0, 6);
            level.setBlock(relayA, TTBlocks.RELAY.get().defaultBlockState(), 3);
            level.setBlock(relayA.offset(6, 0, 0), TTBlocks.RELAY.get().defaultBlockState(), 3);
            level.setBlock(relayA.offset(2, 0, 0), TTBlocks.MOBILIZER.get().defaultBlockState(), 3);
            level.setBlock(relayA.offset(2, 1, 0), Blocks.PUMPKIN.defaultBlockState(), 3);
        });
        server(60, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos magnet = o.offset(2, 0, 0);
            var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(magnet).inflate(6));
            double distance = items.isEmpty() ? 99 : Math.sqrt(items.get(0).distanceToSqr(magnet.getX() + 0.5, magnet.getY(), magnet.getZ() + 0.5));
            check("магнит притянул палку (до неё " + String.format("%.1f", distance) + " блока)", distance < 2.5);
            items.forEach(net.minecraft.world.entity.Entity::discard);
            BlockPos tablet = o.offset(7, 0, 2);
            var tabletTile = (AnimationTabletBlock.Tile) level.getBlockEntity(tablet);
            check("табличка с киркой разбила булыжник (замах " + tabletTile.swing + ", вещь " + tabletTile.getItem(0) + ", перед ней "
                    + level.getBlockState(tablet.south()).getBlock() + ")", level.isEmptyBlock(tablet.south()));
            var magnetTile = (MagnetBlock.Tile) level.getBlockEntity(magnet);
            check("сила сигнала у магнита " + magnetTile.power(), magnetTile.power() == 15);
        });
        cmd("tp @s 1604.5 -59 -1.5 0 20");
        shot(20, "tt2b_devices");
        server(240, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos start = o.offset(2, 0, 6);
            boolean moved = !level.getBlockState(start).is(TTBlocks.MOBILIZER.get());
            boolean cargo = false;
            for (int dx = 0; dx <= 6; dx++) {
                if (level.getBlockState(o.offset(dx, 0, 6)).is(TTBlocks.MOBILIZER.get())
                        && level.getBlockState(o.offset(dx, 1, 6)).is(Blocks.PUMPKIN)) {
                    cargo = true;
                }
            }
            check("левитатор сдвинулся (" + moved + ") и везёт тыкву (" + cargo + ")", moved && cargo);
            BlockPos tablet = o.offset(7, 0, 2);
            player.teleportTo(tablet.getX() + 0.5, tablet.getY(), tablet.getZ() - 2.5);
            net.minecraftforge.network.NetworkHooks.openScreen(player, (AnimationTabletBlock.Tile) level.getBlockEntity(tablet), tablet);
        });
        shot(15, "tt2b_tablet_gui");
        AutoShot.close();
        server(5, (server, player) -> {
            ServerLevel level = player.serverLevel();
            BlockPos mob = o.offset(3, 0, 3);
            level.setBlock(mob, TTBlocks.MOB_MAGNET.get().defaultBlockState(), 3);
            ((MagnetBlock.Tile) level.getBlockEntity(mob)).setItem(0, new ItemStack(TTItems.SOUL_MOULD.get()));
        });
        server(20, (server, player) -> {
            BlockPos mob = o.offset(3, 0, 3);
            player.teleportTo(mob.getX() + 0.5, mob.getY(), mob.getZ() - 2.5);
            net.minecraftforge.network.NetworkHooks.openScreen(player, (MagnetBlock.Tile) player.level().getBlockEntity(mob), mob);
        });
        shot(15, "tt2b_mob_magnet_gui");
        AutoShot.close();
    }
}

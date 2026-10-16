"""Переносит генераторы комнат Запредельных земель (lib/world/dim/Gen*.java оригинала) почти дословно.

Запуск из корня проекта:  python tools/convert_outer_gen.py

Старые обращения к миру (World, ConfigBlocks, ForgeDirection, BlockUtils...) заменяются на OuterWorld — тонкий
переходник, который переводит «блок и мету» 1.7.10 в блоки 1.20. Места с тайлами и существами правятся руками в
OUTER_FIXES ниже: их немного, и там нужна логика 1.20.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'reference', 'tc4', 'src', 'thaumcraft', 'common', 'lib', 'world', 'dim')
DST = os.path.join(ROOT, 'src', 'main', 'java', 'thaumcraft', 'common', 'world', 'outer')
FILES = ['GenCommon', 'GenPortal', 'GenPassage', 'GenKeyRoom', 'GenNestRoom', 'GenLibraryRoom', 'GenBossRoom', 'Gen2x2']

BLOCKS = {
    'ConfigBlocks.blockEldritchNothing': 'Old.ELDRITCH_NOTHING',
    'ConfigBlocks.blockEldritchPortal': 'Old.ELDRITCH_PORTAL',
    'ConfigBlocks.blockEldritch': 'Old.ELDRITCH',
    'ConfigBlocks.blockCosmeticSolid': 'Old.COSMETIC_SOLID',
    'ConfigBlocks.blockStairsEldritch': 'Old.STAIRS_ELDRITCH',
    'ConfigBlocks.blockSlabStone': 'Old.SLAB_STONE',
    'ConfigBlocks.blockAiry': 'Old.AIRY',
    'ConfigBlocks.blockCrystal': 'Old.CRYSTAL',
    'ConfigBlocks.blockLootUrn': 'Old.LOOT_URN',
    'ConfigBlocks.blockLootCrate': 'Old.LOOT_CRATE',
    'ConfigBlocks.blockTaintFibres': 'Old.TAINT_FIBRES',
    'ConfigBlocks.blockTaint': 'Old.TAINT',
    'Blocks.bedrock': 'Old.BEDROCK',
    'Blocks.air': 'Old.AIR',
    'Blocks.web': 'Old.WEB',
    'Blocks.mob_spawner': 'Old.MOB_SPAWNER',
}

HEADER = '''package thaumcraft.common.world.outer;

import thaumcraft.common.world.outer.OuterWorld.ChunkCoordinates;
import thaumcraft.common.world.outer.OuterWorld.Dir;
import thaumcraft.common.world.outer.OuterWorld.Old;

import java.util.ArrayList;
import java.util.Random;

/** Перенос %s оригинала (tools/convert_outer_gen.py): %s */
'''

ABOUT = {
    'GenCommon': 'общие кирпичики залов — какой блок ставится по коду узора, переходы между клетками и украшения.',
    'GenPortal': 'зал с порталом домой: колпак, портал и парящий над ним обелиск.',
    'GenPassage': 'обычные коридоры лабиринта, с ловушками, гнёздами порчи и пауков.',
    'GenKeyRoom': 'зал с рунной скрижалью на колпаке, под охраной стражей Элдрича.',
    'GenNestRoom': 'гнездо крабов Элдрича: корка, странные кристаллы и добыча.',
    'GenLibraryRoom': 'библиотека: пьедесталы, камни с глифами и полки с урнами.',
    'GenBossRoom': 'вход в зал хранителя — древний проём с замком, который открывает рунная скрижаль.',
    'Gen2x2': 'четыре четверти большого зала хранителя.',
}


def convert(name):
    text = open(os.path.join(SRC, name + '.java'), encoding='utf-8').read()
    body = text[text.index('public class') if 'public class' in text else text.index('class'):]
    body = body.replace('public class', 'class', 1)
    for old, new in BLOCKS.items():
        body = body.replace(old, new)
    body = body.replace('ForgeDirection', 'Dir')
    body = body.replace('World world', 'OuterWorld world')
    body = re.sub(r'world\.getBlock\(([^()]*)\)\.isOpaqueCube\(\)', r'world.isOpaqueCube(\1)', body)
    body = re.sub(r'BlockUtils\.countExposedSides\(world, ', 'world.countExposedSides(', body)
    body = re.sub(r'BlockUtils\.isBlockAdjacentToAtleast\(world, ([^;]*?), Old\.ELDRITCH, 32767, 1\)',
                  r'world.isBlockAdjacentToAtleast(\1, Old.ELDRITCH, 1)', body)
    body = re.sub(r'BlockUtils\.isAdjacentToSolidBlock\(world, ', 'world.isAdjacentToSolidBlock(', body)
    body = body.replace('MathHelper.getRandomIntegerInRange(', 'OuterWorld.getRandomIntegerInRange(')
    body = body.replace('new ArrayList()', 'new ArrayList<>()')
    body = re.sub(r'\bBlock(\s+\w+\s*[;=])', r'Old\1', body)
    # обращения к тайлам и существам переносятся руками
    for old, new in OUTER_FIXES.get(name, []):
        assert body.count(old) == 1, (name, old[:80])
        body = body.replace(old, new)
    out = HEADER % (name, ABOUT[name]) + body
    open(os.path.join(DST, name + '.java'), 'w', encoding='utf-8').write(out)


OUTER_FIXES = {
    'GenCommon': [
        ('''                     world.setBlock(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ, Old.CRYSTAL, 7, 3);
                     TileCrystal te = (TileCrystal)world.getTileEntity(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ);
                     te.orientation = (short)dir.ordinal();''',
         '''                     world.setCrystal(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ, dir);'''),
        ('''            world.setBlock(cc.posX, cc.posY, cc.posZ, Old.ELDRITCH, 9, 3);
            TileEntity te = world.getTileEntity(cc.posX, cc.posY, cc.posZ);
            if (te != null && te instanceof TileEldritchCrabSpawner) {
               for(Dir dir : Dir.VALID_DIRECTIONS) {
                  if (world.isAirBlock(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ)) {
                     ((TileEldritchCrabSpawner)te).setFacing((byte)dir.ordinal());
                     break;
                  }
               }
            }''',
         '''            Dir facing = Dir.UP;
            for(Dir dir : Dir.VALID_DIRECTIONS) {
               if (world.isAirBlock(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ)) {
                  facing = dir;
                  break;
               }
            }
            world.level.setBlock(new net.minecraft.core.BlockPos(cc.posX, cc.posY, cc.posZ), thaumcraft.common.registry.ModBlocks.CRUSTED_OPENING.get()
                  .defaultBlockState().setValue(thaumcraft.common.blocks.CrustedOpeningBlock.FACING, facing.toDirection()), 2);'''),
        ('''      if (block != null) {
         world.setBlock(x, y, z, block, meta, block != Old.ELDRITCH_NOTHING && block != Old.BEDROCK && block != Old.AIR ? 3 : 0);
      }''', '''      if (block != null) {
         world.setBlock(x, y, z, block, meta, block != Old.ELDRITCH_NOTHING && block != Old.BEDROCK && block != Old.AIR ? 3 : 0);
         if (block == Old.ELDRITCH && meta == 8 && dir != Dir.UNKNOWN) {
            // замок смотрит наружу из зала (setFacing в GenBossRoom оригинала)
            world.level.setBlock(new net.minecraft.core.BlockPos(x, y, z), thaumcraft.common.registry.ModBlocks.ANCIENT_LOCK.get().defaultBlockState()
                  .setValue(thaumcraft.common.blocks.AncientLockBlock.FACING, dir.toDirection()), 2);
         }
      }'''),
    ],
    'GenBossRoom': [
        ('''                  placeBlock(world, xx, y + 2 + b, zz, 16, cell);
                  TileEntity t = world.getTileEntity(xx, y + 2 + b, zz);
                  if (t != null && t instanceof TileEldritchLock) {
                     ((TileEldritchLock)t).setFacing((byte)dir.ordinal());
                  }''', '''                  placeBlock(world, xx, y + 2 + b, zz, 16, dir, cell);'''),
        # у четверти зала без выхода оригинал рисовал проём в точке (0, 0) мира — такую четверть пропускаем
        ('''               dir = Dir.WEST;
            }

            switch (PAT_DOORWAY[a][b]) {''', '''               dir = Dir.WEST;
            }

            if (dir == Dir.UNKNOWN) {
               continue;
            }

            switch (PAT_DOORWAY[a][b]) {'''),
    ],
    'GenPassage': [
        ('''                     Utils.setBiomeAt(world, x + 8 + w, z + 8 + j, ThaumcraftWorldGenerator.biomeTaint);''',
         '''                     world.setBiomeAt(x + 8 + w, z + 8 + j, thaumcraft.common.taint.Taint.BIOME);'''),
        ('''                  TileEntityMobSpawner var12 = (TileEntityMobSpawner)world.getTileEntity(x + 8, y + 4, z + 8);
                  if (var12 != null) {
                     var12.func_145881_a().setEntityName("Thaumcraft.MindSpider");
                  }''', '''                  if (world.getTileEntity(x + 8, y + 4, z + 8) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
                     spawner.setEntityId(thaumcraft.common.registry.ModEntities.MIND_SPIDER.get(), world.level.getRandom());
                  }'''),
    ],
    'GenNestRoom': [
        ('''               world.setBlock(x + a, y + 8, z + b, Old.CRYSTAL, 7, 3);
               TileCrystal te = (TileCrystal)world.getTileEntity(x + a, y + 8, z + b);
               te.orientation = (short)Dir.DOWN.ordinal();''', '''               world.setCrystal(x + a, y + 8, z + b, Dir.DOWN);'''),
    ],
    'GenKeyRoom': [
        ('''      EntityPermanentItem entityitem = new EntityPermanentItem(world, (double)x + 8.5D, (double)y + 3.5D, (double)z + 8.5D, new ItemStack(ConfigItems.itemEldritchObject, 1, 2));
      entityitem.motionY = 0.0D;
      entityitem.motionX = 0.0D;
      entityitem.motionZ = 0.0D;
      world.spawnEntityInWorld(entityitem);
      int zz = 2 + (world.difficultySetting == EnumDifficulty.HARD ? 2 : (world.difficultySetting == EnumDifficulty.NORMAL ? 1 : 0));''',
         '''      // EntityPermanentItem: рунная скрижаль лежит на колпаке вечно и не горит
      net.minecraft.world.entity.item.ItemEntity entityitem = new net.minecraft.world.entity.item.ItemEntity(world.level.getLevel(),
            (double)x + 8.5D, (double)y + 3.5D, (double)z + 8.5D,
            new net.minecraft.world.item.ItemStack(thaumcraft.common.registry.ModItems.RUNED_TABLET.get()), 0.0D, 0.0D, 0.0D);
      entityitem.setUnlimitedLifetime();
      entityitem.setInvulnerable(true);
      world.spawnEntityInWorld(entityitem);
      int zz = 2 + (world.difficulty() == net.minecraft.world.Difficulty.HARD ? 2 : (world.difficulty() == net.minecraft.world.Difficulty.NORMAL ? 1 : 0));'''),
        ('''         EntityEldritchGuardian eg = new EntityEldritchGuardian(world);''',
         '''         thaumcraft.common.entity.EldritchGuardian eg = thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get().create(world.level.getLevel());'''),
        ('''         eg.setPosition(i1, j1, k1);
         eg.onSpawnWithEgg((IEntityLivingData)null);
         eg.setHomeArea(x + 8, y + 2, z + 8, 16);
         if (qq == 0 && zz >= 4) {
            EntityUtils.makeChampion(eg, true);
         }''', '''         eg.setPos(i1, j1, k1);
         eg.setPersistenceRequired();
         eg.finalizeSpawn((net.minecraft.world.level.ServerLevelAccessor) world.level, world.level.getCurrentDifficultyAt(eg.blockPosition()),
               net.minecraft.world.entity.MobSpawnType.STRUCTURE, null, null);
         eg.restrictTo(new net.minecraft.core.BlockPos(x + 8, y + 2, z + 8), 16);
         if (qq == 0 && zz >= 4) {
            thaumcraft.common.champion.Champions.makeChampion(eg, true);
         }'''),
    ],
}

if __name__ == '__main__':
    for name in FILES:
        convert(name)
        print('перенесено:', name)

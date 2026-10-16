package thaumcraft.common.world.outer;

import thaumcraft.common.world.outer.OuterWorld.ChunkCoordinates;
import thaumcraft.common.world.outer.OuterWorld.Dir;
import thaumcraft.common.world.outer.OuterWorld.Old;

import java.util.ArrayList;
import java.util.Random;

/** Перенос GenKeyRoom оригинала (tools/convert_outer_gen.py): зал с рунной скрижалью на колпаке, под охраной стражей Элдрича. */
class GenKeyRoom extends GenCommon {
   static void generateRoom(OuterWorld world, Random random, int cx, int cz, int y, Cell cell) {
      int x = cx * 16;
      int z = cz * 16;

      for(int a = 1; a <= 15; ++a) {
         for(int b = 1; b <= 15; ++b) {
            for(int c = 0; c < 13; ++c) {
               if (a == 1 || a == 15 || b == 1 || b == 15) {
                  placeBlock(world, x + a, y + c, z + b, 1, cell);
               }
            }
         }
      }

      for(int a = 2; a <= 14; ++a) {
         for(int b = 2; b <= 14; ++b) {
            for(int c = 1; c < 12; ++c) {
               if ((a == 2 || a == 14 || b == 2 || b == 14) && (a != 2 || b <= 3 || b >= 12 || !cell.west || c >= 10) && (a != 14 || b <= 3 || b >= 12 || !cell.east || c >= 10) && (b != 2 || a <= 3 || a >= 12 || !cell.north || c >= 10) && (b != 14 || a <= 3 || a >= 12 || !cell.south || c >= 10)) {
                  placeBlock(world, x + a, y + c, z + b, 8, cell);
               }
            }
         }
      }

      for(int a = 3; a <= 13; ++a) {
         for(int b = 3; b <= 13; ++b) {
            for(int c = 2; c < 11; ++c) {
               if (a == 3 || a == 13 || b == 3 || b == 13) {
                  if (c > 3 && c < 9 && (a == 8 || b == 8) || c > 4 && c < 8 && (a == 7 || b == 7 || a == 9 || b == 9)) {
                     if (a != 8 && b != 8 || c != 6) {
                        placeBlock(world, x + a, y + c, z + b, 19, cell);
                     }
                  } else {
                     placeBlock(world, x + a, y + c, z + b, 18, cell);
                  }
               }
            }
         }
      }

      for(int a = 2; a <= 14; ++a) {
         for(int b = 2; b <= 14; ++b) {
            placeBlock(world, x + a, y - 1, z + b, 1, cell);
            placeBlock(world, x + a, y, z + b, 8, cell);
            placeBlock(world, x + a, y + 1, z + b, 2, cell);
            placeBlock(world, x + a, y + 13, z + b, 1, cell);
            placeBlock(world, x + a, y + 12, z + b, 8, cell);
            placeBlock(world, x + a, y + 11, z + b, 2, cell);
            if (a > 1 && a < 15 && b > 1 && b < 15) {
               int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));

               for(int g = 0; g < q - 1; ++g) {
                  placeBlock(world, x + a, y + 1 + g, z + b, 2, cell);
               }
            }

            if (a > 3 && a < 13 && b > 3 && b < 13) {
               int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));

               for(int g = 0; g < q; ++g) {
                  placeBlock(world, x + a, y + 11 - g, z + b, 2, cell);
               }
            }
         }
      }

      for(int g = 0; g < 5; ++g) {
         placeBlock(world, x + 6 + g, y + 2, z + 4, 10, Dir.NORTH, cell);
         placeBlock(world, x + 6 + g, y + 2, z + 12, 10, Dir.SOUTH, cell);
         placeBlock(world, x + 12, y + 2, z + 6 + g, 10, Dir.EAST, cell);
         placeBlock(world, x + 4, y + 2, z + 6 + g, 10, Dir.WEST, cell);
      }

      GenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);
      world.setBlock(x + 8, y + 2, z + 8, Old.ELDRITCH, 3, 3);
      // EntityPermanentItem: рунная скрижаль лежит на колпаке вечно и не горит
      net.minecraft.world.entity.item.ItemEntity entityitem = new net.minecraft.world.entity.item.ItemEntity(world.level.getLevel(),
            (double)x + 8.5D, (double)y + 3.5D, (double)z + 8.5D,
            new net.minecraft.world.item.ItemStack(thaumcraft.common.registry.ModItems.RUNED_TABLET.get()), 0.0D, 0.0D, 0.0D);
      entityitem.setUnlimitedLifetime();
      entityitem.setInvulnerable(true);
      world.spawnEntityInWorld(entityitem);
      int zz = 2 + (world.difficulty() == net.minecraft.world.Difficulty.HARD ? 2 : (world.difficulty() == net.minecraft.world.Difficulty.NORMAL ? 1 : 0));

      for(int qq = 0; qq < zz; ++qq) {
         thaumcraft.common.entity.EldritchGuardian eg = thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get().create(world.level.getLevel());
         double i1 = (double)x + 8.5D + (double)(OuterWorld.getRandomIntegerInRange(world.rand, 1, 3) * OuterWorld.getRandomIntegerInRange(world.rand, -1, 1));
         double j1 = (double)(y + 2);
         double k1 = (double)z + 8.5D + (double)(OuterWorld.getRandomIntegerInRange(world.rand, 1, 3) * OuterWorld.getRandomIntegerInRange(world.rand, -1, 1));
         eg.setPos(i1, j1, k1);
         eg.setPersistenceRequired();
         eg.finalizeSpawn((net.minecraft.world.level.ServerLevelAccessor) world.level, world.level.getCurrentDifficultyAt(eg.blockPosition()),
               net.minecraft.world.entity.MobSpawnType.STRUCTURE, null, null);
         eg.restrictTo(new net.minecraft.core.BlockPos(x + 8, y + 2, z + 8), 16);
         if (qq == 0 && zz >= 4) {
            thaumcraft.common.champion.Champions.makeChampion(eg, true);
         }

         world.spawnEntityInWorld(eg);
      }

   }
}

package thaumcraft.common.world.outer;

import thaumcraft.common.world.outer.OuterWorld.ChunkCoordinates;
import thaumcraft.common.world.outer.OuterWorld.Dir;
import thaumcraft.common.world.outer.OuterWorld.Old;

import java.util.ArrayList;
import java.util.Random;

/** Перенос GenCommon оригинала (tools/convert_outer_gen.py): общие кирпичики залов — какой блок ставится по коду узора, переходы между клетками и украшения. */
class GenCommon {
   static ArrayList<ChunkCoordinates> decoCommon = new ArrayList<>();
   static ArrayList<ChunkCoordinates> crabSpawner = new ArrayList<>();
   static ArrayList<ChunkCoordinates> decoUrn = new ArrayList<>();
   static final int BEDROCK = 1;
   static final int BEDROCK_REPL = 99;
   static final int STONE = 2;
   static final int VOID = 8;
   static final int AIR_REPL = 9;
   static final int STAIR_DIRECTIONAL = 10;
   static final int STAIR_DIRECTIONAL_INV = 11;
   static final int SLAB = 12;
   static final int DOOR_BLOCK = 15;
   static final int DOOR_LOCK = 16;
   static final int VOID_DOOR = 17;
   static final int ROCK = 18;
   static final int STONE_NOSPAWN = 19;
   static final int STONE_TRAPPED = 20;
   static final int CRUST = 21;
   static final int[][] PAT_CONNECT = new int[][]{{0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}, {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1}, {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1}, {1, 8, 2, 5, 9, 9, 9, 6, 2, 8, 1}, {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1}, {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1}, {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1}, {1, 8, 2, 3, 9, 9, 9, 4, 2, 8, 1}, {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1}, {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1}, {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}};

   static void placeBlock(OuterWorld world, int i, int j, int k, int l, Cell cell) {
      placeBlock(world, i, j, k, l, Dir.UNKNOWN, cell);
   }

   static void placeBlock(OuterWorld world, int x, int y, int z, int b, Dir dir, Cell cell) {
      Old block;
      int meta;
      label233: {
         block = null;
         meta = 0;
         switch (b) {
            case 1:
               if (world.isAirBlock(x, y, z)) {
                  block = Old.BEDROCK;
               }
               break label233;
            case 2:
               if (cell.feature == 7 && world.rand.nextInt(3) == 0) {
                  break;
               }

               if (world.getBlock(x, y, z) != Old.ELDRITCH_NOTHING) {
                  if (world.rand.nextInt(25) == 0) {
                     boolean crab = cell.feature == 7 ? true : world.rand.nextInt(50) == 0;
                     if ((!crab || cell.feature != 0) && (!crab || cell.feature != 7)) {
                        decoCommon.add(new ChunkCoordinates(x, y, z));
                     } else {
                        crabSpawner.add(new ChunkCoordinates(x, y, z));
                     }
                  }

                  block = Old.COSMETIC_SOLID;
                  meta = 11;
               }
               break label233;
            case 3:
               if ((double)world.rand.nextFloat() < 0.005D) {
                  decoUrn.add(new ChunkCoordinates(x, y, z));
               }

               block = Old.STAIRS_ELDRITCH;
               switch (dir.ordinal()) {
                  case 2:
                  case 3:
                     meta = 1;
                     break label233;
                  case 4:
                  case 5:
                     meta = 3;
                  default:
                     break label233;
               }
            case 4:
               if ((double)world.rand.nextFloat() < 0.005D) {
                  decoUrn.add(new ChunkCoordinates(x, y, z));
               }

               block = Old.STAIRS_ELDRITCH;
               switch (dir.ordinal()) {
                  case 2:
                  case 3:
                     meta = 0;
                     break label233;
                  case 4:
                  case 5:
                     meta = 2;
                  default:
                     break label233;
               }
            case 5:
               block = Old.STAIRS_ELDRITCH;
               switch (dir.ordinal()) {
                  case 2:
                  case 3:
                     meta = 5;
                     break label233;
                  case 4:
                  case 5:
                     meta = 7;
                  default:
                     break label233;
               }
            case 6:
               block = Old.STAIRS_ELDRITCH;
               switch (dir.ordinal()) {
                  case 2:
                  case 3:
                     meta = 4;
                     break label233;
                  case 4:
                  case 5:
                     meta = 6;
                  default:
                     break label233;
               }
            case 7:
               block = Old.ELDRITCH;
               meta = 4;
               break label233;
            case 8:
               block = Old.ELDRITCH_NOTHING;
               break label233;
            case 9:
               block = Old.AIR;
               decoCommon.remove(new ChunkCoordinates(x, y, z));
               crabSpawner.remove(new ChunkCoordinates(x, y, z));
               decoUrn.remove(new ChunkCoordinates(x, y, z));
               break label233;
            case 10:
               block = Old.STAIRS_ELDRITCH;
               switch (dir) {
                  case NORTH:
                     meta = 3;
                     break label233;
                  case SOUTH:
                     meta = 2;
                     break label233;
                  case EAST:
                     meta = 0;
                     break label233;
                  case WEST:
                     meta = 1;
                  default:
                     break label233;
               }
            case 11:
               block = Old.STAIRS_ELDRITCH;
               switch (dir) {
                  case NORTH:
                     meta = 7;
                     break label233;
                  case SOUTH:
                     meta = 6;
                     break label233;
                  case EAST:
                     meta = 4;
                     break label233;
                  case WEST:
                     meta = 5;
                  default:
                     break label233;
               }
            case 15:
               block = Old.ELDRITCH;
               meta = 7;
               decoCommon.remove(new ChunkCoordinates(x, y, z));
               crabSpawner.remove(new ChunkCoordinates(x, y, z));
               decoUrn.remove(new ChunkCoordinates(x, y, z));
               break label233;
            case 16:
               block = Old.ELDRITCH;
               meta = 8;
               decoCommon.remove(new ChunkCoordinates(x, y, z));
               crabSpawner.remove(new ChunkCoordinates(x, y, z));
               decoUrn.remove(new ChunkCoordinates(x, y, z));
               break label233;
            case 17:
               block = Old.AIRY;
               meta = 12;
               break label233;
            case 18:
               if (world.getBlock(x, y, z) != Old.ELDRITCH_NOTHING) {
                  block = Old.COSMETIC_SOLID;
                  meta = 12;
               }
               break label233;
            case 19:
               if (world.getBlock(x, y, z) != Old.ELDRITCH_NOTHING) {
                  block = Old.COSMETIC_SOLID;
                  meta = 13;
               }
               break label233;
            case 20:
               if (world.getBlock(x, y, z) != Old.ELDRITCH_NOTHING) {
                  block = Old.ELDRITCH;
                  meta = 10;
               }
               break label233;
            case 21:
               break;
            case 99:
               block = Old.BEDROCK;
            default:
               break label233;
         }

         if (world.getBlock(x, y, z) != Old.ELDRITCH_NOTHING) {
            block = Old.COSMETIC_SOLID;
            meta = 14;
            if (world.rand.nextInt(25) == 0) {
               block = Old.ELDRITCH;
               meta = 4;
            } else if (world.rand.nextInt(25) == 0) {
               boolean crab = cell.feature == 7 ? true : (cell.feature == 12 && world.rand.nextBoolean() ? true : world.rand.nextInt(25) == 0);
               if (crab && cell.feature == 0 || crab && cell.feature == 7 || crab && cell.feature == 12) {
                  crabSpawner.add(new ChunkCoordinates(x, y, z));
               }
            }
         }
      }

      if (block != null) {
         world.setBlock(x, y, z, block, meta, block != Old.ELDRITCH_NOTHING && block != Old.BEDROCK && block != Old.AIR ? 3 : 0);
         if (block == Old.ELDRITCH && meta == 8 && dir != Dir.UNKNOWN) {
            // замок смотрит наружу из зала (setFacing в GenBossRoom оригинала)
            world.level.setBlock(new net.minecraft.core.BlockPos(x, y, z), thaumcraft.common.registry.ModBlocks.ANCIENT_LOCK.get().defaultBlockState()
                  .setValue(thaumcraft.common.blocks.AncientLockBlock.FACING, dir.toDirection()), 2);
         }
      }

   }

   public static void genObelisk(OuterWorld world, int x, int y, int z) {
      world.setBlock(x, y, z, Old.ELDRITCH, 1, 3);
      world.setBlock(x, y + 1, z, Old.ELDRITCH, 2, 3);
      world.setBlock(x, y + 2, z, Old.ELDRITCH, 2, 3);
      world.setBlock(x, y + 3, z, Old.ELDRITCH, 2, 3);
      world.setBlock(x, y + 4, z, Old.ELDRITCH, 2, 3);
   }

   static void processDecorations(OuterWorld world) {
      for(ChunkCoordinates cc : decoUrn) {
         if (world.isAirBlock(cc.posX, cc.posY + 1, cc.posZ)) {
            world.setBlock(cc.posX, cc.posY, cc.posZ, Old.COSMETIC_SOLID, 15, 3);
            float rr = world.rand.nextFloat();
            int meta = rr < 0.025F ? 2 : (rr < 0.1F ? 1 : 0);
            world.setBlock(cc.posX, cc.posY + 1, cc.posZ, Old.LOOT_URN, meta, 3);
         }
      }

      for(ChunkCoordinates cc : decoCommon) {
         int exp = world.countExposedSides(cc.posX, cc.posY, cc.posZ);
         if (exp > 0 && (exp == 1 || !isBedrockShowing(world, cc.posX, cc.posY, cc.posZ)) && !world.isBlockAdjacentToAtleast(cc.posX, cc.posY, cc.posZ, Old.ELDRITCH, 1)) {
            int meta = world.rand.nextInt(3) != 0 ? 4 : (world.rand.nextInt(8) != 0 ? 5 : 10);
            world.setBlock(cc.posX, cc.posY, cc.posZ, Old.ELDRITCH, meta, 3);
            if (meta == 4 && world.rand.nextInt(12) == 0) {
               for(Dir dir : Dir.VALID_DIRECTIONS) {
                  if (world.isAirBlock(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ)) {
                     world.setCrystal(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ, dir);
                     break;
                  }
               }
            }
         }
      }

      for(ChunkCoordinates cc : crabSpawner) {
         int exp = world.countExposedSides(cc.posX, cc.posY, cc.posZ);
         if (exp == 1 && !world.isBlockAdjacentToAtleast(cc.posX, cc.posY, cc.posZ, Old.ELDRITCH, 1)) {
            Dir facing = Dir.UP;
            for(Dir dir : Dir.VALID_DIRECTIONS) {
               if (world.isAirBlock(cc.posX + dir.offsetX, cc.posY + dir.offsetY, cc.posZ + dir.offsetZ)) {
                  facing = dir;
                  break;
               }
            }
            world.level.setBlock(new net.minecraft.core.BlockPos(cc.posX, cc.posY, cc.posZ), thaumcraft.common.registry.ModBlocks.CRUSTED_OPENING.get()
                  .defaultBlockState().setValue(thaumcraft.common.blocks.CrustedOpeningBlock.FACING, facing.toDirection()), 2);
         }
      }

      decoCommon.clear();
      crabSpawner.clear();
      decoUrn.clear();
   }

   static boolean isBedrockShowing(OuterWorld world, int x, int y, int z) {
      for(Dir dir : Dir.VALID_DIRECTIONS) {
         if (!world.isOpaqueCube(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ) && (world.getBlock(x + dir.getOpposite().offsetX, y + dir.getOpposite().offsetY, z + dir.getOpposite().offsetZ) == Old.BEDROCK || world.getBlock(x + dir.getOpposite().offsetX, y + dir.getOpposite().offsetY, z + dir.getOpposite().offsetZ) == Old.ELDRITCH_NOTHING)) {
            return true;
         }
      }

      return false;
   }

   static void generateConnections(OuterWorld world, Random random, int cx, int cz, int y, Cell cell, int depth, boolean justthetip) {
      int x = cx * 16;
      int z = cz * 16;
      if (cell.north) {
         for(int d = 0; d <= depth; ++d) {
            for(int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++w) {
               for(int h = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); h < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++h) {
                  if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                     placeBlock(world, x + 3 + w, y + 10 - h, z + d, PAT_CONNECT[h][w], Dir.NORTH, cell);
                  }
               }
            }
         }
      }

      if (cell.south) {
         for(int d = 0; d <= depth; ++d) {
            for(int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++w) {
               for(int h = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); h < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++h) {
                  if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                     placeBlock(world, x + 3 + w, y + 10 - h, z + 16 - d, PAT_CONNECT[h][w], Dir.SOUTH, cell);
                  }
               }
            }
         }
      }

      if (cell.east) {
         for(int d = 0; d <= depth; ++d) {
            for(int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++w) {
               for(int h = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); h < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++h) {
                  if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                     placeBlock(world, x + 16 - d, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], Dir.EAST, cell);
                  }
               }
            }
         }
      }

      if (cell.west) {
         for(int d = 0; d <= depth; ++d) {
            for(int w = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); w < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++w) {
               for(int h = d == depth && justthetip ? 2 : (d == depth - 1 && justthetip ? 1 : 0); h < (d == depth && justthetip ? 9 : (d == depth - 1 && justthetip ? 10 : 11)); ++h) {
                  if (d != depth || !justthetip || PAT_CONNECT[h][w] != 8) {
                     placeBlock(world, x + d, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], Dir.WEST, cell);
                  }
               }
            }
         }
      }

   }
}

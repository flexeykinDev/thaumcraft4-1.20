package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.EldritchAltarBlock;
import thaumcraft.common.blocks.ObsidianTotemBlock;
import thaumcraft.common.blocks.ThaumBannerBlock;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.world.outer.Labyrinth;

/**
 * WorldGenEldritchRing оригинала: площадка 7×7 из обсидиана и обсидиановых тотемов, на ней — алтарь Элдрича,
 * над ним парит обелиск, по краям — восемь колпаков. Каждое второе кольцо охраняет культ: алтарь призовёт
 * клириков на обряд, а вокруг стоят знамёна Багрового культа; иное — стражей Элдрича. Под кольцом в Запредельных
 * землях закладывается лабиринт размером width×height клеток (MazeThread оригинала).
 */
public final class EldritchRing {
    private EldritchRing() {
    }

    /** LocationIsValidSpawn оригинала: открытая земля, камень, песок, гравий или плотный лёд. */
    private static boolean validGround(WorldGenLevel level, BlockPos pos) {
        int toAir = 0;
        while (!level.getBlockState(pos.above(toAir)).isAir()) {
            toAir++;
            if (toAir > 2) {
                return false;
            }
        }
        BlockPos top = pos.above(toAir - 1);
        if (!level.getBlockState(top.above()).isAir()) {
            return false;
        }
        BlockState here = level.getBlockState(top);
        if (ground(here)) {
            return true;
        }
        return (here.is(Blocks.SNOW) || here.is(Blocks.GRASS) || here.is(Blocks.FERN)) && ground(level.getBlockState(top.below()));
    }

    private static boolean ground(BlockState state) {
        return state.is(Blocks.STONE) || state.is(BlockTags.SAND) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT);
    }

    public static boolean generate(WorldGenLevel level, RandomSource random, BlockPos centre, int width, int height) {
        int i = centre.getX();
        int j = centre.getY();
        int k = centre.getZ();
        for (int[] d : new int[][]{{-3, -3}, {0, 0}, {3, 0}, {3, 3}, {0, 3}}) {
            if (!validGround(level, new BlockPos(i + d[0], j, k + d[1]))) {
                return false;
            }
        }
        if (level.getServer() == null) {
            return false;
        }
        Labyrinth maze = Labyrinth.get(level.getServer());
        int chunkX = i >> 4;
        int chunkZ = k >> 4;
        if (maze.mazesInRange(chunkX, chunkZ, width, height)) {
            return false;
        }
        Block totem = ModBlocks.OBSIDIAN_TOTEM.get();
        for (int x = i - 3; x <= i + 3; x++) {
            for (int z = k - 3; z <= k + 3; z++) {
                if ((x == i - 3 || x == i + 3) && (z == k - 3 || z == k + 3)) {
                    continue;
                }
                for (int q = -4; q < 5; q++) {
                    BlockPos pos = new BlockPos(x, j + q, z);
                    if (q <= 0) {
                        level.setBlock(pos, random.nextInt(4) == 0 ? Blocks.OBSIDIAN.defaultBlockState() : totem.defaultBlockState(), 2);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                if (x == i && z == k) {
                    BlockPos altarPos = new BlockPos(x, j + 1, z);
                    level.setBlock(new BlockPos(x, j, z), totem.defaultBlockState(), 2);
                    level.setBlock(altarPos, ModBlocks.ELDRITCH_ALTAR.get().defaultBlockState(), 2);
                    int r = random.nextInt(10);
                    if (level.getBlockEntity(altarPos) instanceof EldritchAltarBlock.Altar altar) {
                        if (r >= 1 && r <= 4) {
                            // культ: клирики на обряд и четыре знамени вокруг
                            altar.spawner = true;
                            altar.spawnType = 0;
                            for (Direction dir : Direction.Plane.HORIZONTAL) {
                                BlockPos banner = new BlockPos(x - dir.getStepX() * 3, j + 1, z + dir.getStepZ() * 3);
                                int face = switch (dir) {
                                    case NORTH -> 8;
                                    case WEST -> 12;
                                    case EAST -> 4;
                                    default -> 0;
                                };
                                level.setBlock(banner, ModBlocks.THAUM_BANNER.get().defaultBlockState().setValue(ThaumBannerBlock.ROTATION, face), 2);
                            }
                        } else if (r == 6 || r == 7) {
                            altar.spawner = true;
                            altar.spawnType = 1;
                        }
                        altar.setChanged();
                    }
                    level.setBlock(new BlockPos(x, j + 3, z), ModBlocks.ELDRITCH_OBELISK.get().defaultBlockState(), 2);
                    for (int s = 4; s <= 7; s++) {
                        level.setBlock(new BlockPos(x, j + s, z), ModBlocks.ELDRITCH_OBELISK_SEGMENT.get().defaultBlockState(), 2);
                    }
                } else if (((x == i - 3 || x == i + 3) && Math.abs((z - k) % 2) == 1 || (z == k - 3 || z == k + 3) && Math.abs((x - i) % 2) == 1)
                        && Math.abs(x - i) != Math.abs(z - k)) {
                    level.setBlock(new BlockPos(x, j, z), totem.defaultBlockState(), 2);
                    level.setBlock(new BlockPos(x, j + 1, z), ModBlocks.ELDRITCH_CAPSTONE.get().defaultBlockState(), 2);
                }
            }
        }
        // тотемы, поставленные один на другой, срастаются в столб
        for (BlockPos pos : BlockPos.betweenClosed(i - 3, j - 4, k - 3, i + 3, j, k + 3)) {
            if (level.getBlockState(pos).is(totem)) {
                level.setBlock(pos, ObsidianTotemBlock.placed(totem, level, pos), 2);
            }
        }
        maze.generate(chunkX, chunkZ, width, height, random.nextLong());
        return true;
    }
}

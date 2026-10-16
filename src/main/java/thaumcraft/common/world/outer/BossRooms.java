package thaumcraft.common.world.outer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.champion.Champions;
import thaumcraft.common.entity.CultistPortal;
import thaumcraft.common.entity.EldritchGolem;
import thaumcraft.common.entity.EldritchWarden;
import thaumcraft.common.entity.Taintacle;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.world.outer.OuterWorld.Old;

import java.util.Random;

/**
 * Залы хранителей Запредельных земель — doBossSpawn и spawn*BossRoom из TileEldritchLock оригинала. Когда в замке
 * зала растворяется скрижаль, зал обставляется под очередного хранителя (по кругу: голем, надзиратель, культ,
 * порча; изредка очередь перескакивает) и хранитель появляется; поле, запиравшее проём, и сам замок исчезают.
 */
public final class BossRooms {
    /** Ступени пьедестала хранителя (мета ступеней; -1 — светящийся камень посередине). */
    private static final int[][] PED = {{2, 2, 2}, {0, -1, 1}, {3, 3, 3}};

    private BossRooms() {
    }

    /** MapBossData оригинала: сколько залов уже открыто в мире — от этого зависит, кто проснётся следующим. */
    public static final class Count extends SavedData {
        int bossCount;

        static Count get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(tag -> {
                Count data = new Count();
                data.bossCount = tag.getInt("bossCount");
                return data;
            }, Count::new, "thaumcraft_boss_map");
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("bossCount", bossCount);
            return tag;
        }
    }

    public static void open(ServerLevel level, BlockPos lock) {
        int cx = lock.getX() >> 4;
        int cz = lock.getZ() >> 4;
        int centerx = cx;
        int centerz = cz;
        int exit = 0;
        Labyrinth maze = Labyrinth.get(level.getServer());
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                Cell c = maze.get(cx + a, cz + b);
                if (c != null && c.feature == 2) {
                    centerx = cx + a;
                    centerz = cz + b;
                }
                if (c != null && c.feature >= 2 && c.feature <= 5 && (c.north || c.south || c.east || c.west)) {
                    exit = c.feature;
                }
            }
        }
        Count count = Count.get(level);
        count.bossCount++;
        if (level.random.nextFloat() < 0.25f) {
            count.bossCount++;
        }
        count.setDirty();
        OuterWorld world = new OuterWorld(level, new Random(level.random.nextLong()));
        switch (count.bossCount % 4) {
            case 0 -> golemRoom(world, level, lock, centerx, centerz, exit);
            case 1 -> wardenRoom(world, level, lock, centerx, centerz, exit);
            case 2 -> cultistRoom(world, level, lock, centerx, centerz);
            default -> taintRoom(world, level, lock, centerx, centerz);
        }
        for (BlockPos at : BlockPos.betweenClosed(lock.offset(-2, -2, -2), lock.offset(2, 2, 2))) {
            if (level.getBlockState(at).is(ModBlocks.IMPASSABLE.get())) {
                FxPacket.blockSparkle(level, at, 4194368, 1);
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.setBlock(lock, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void announce(ServerLevel level, BlockPos lock, String key) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(lock.getX(), lock.getY(), lock.getZ()) < 300.0) {
                player.sendSystemMessage(Component.translatable(key));
            }
        }
    }

    /** Хранитель смотрит на замок, через который войдут. */
    private static void faceLock(Mob boss, BlockPos lock, double x, double y, double z) {
        double d0 = lock.getX() - x;
        double d1 = lock.getY() - (y + boss.getEyeHeight());
        double d2 = lock.getZ() - z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float yaw = (float) (Mth.atan2(d2, d0) * 180.0 / Math.PI) - 90f;
        float pitch = (float) -(Mth.atan2(d1, d3) * 180.0 / Math.PI);
        boss.moveTo(x, y, z, yaw, pitch);
        boss.setYHeadRot(yaw);
    }

    private static void pedestal(OuterWorld world, int x, int y, int z) {
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                if (PED[a][b] < 0) {
                    world.setBlock(x - 1 + b, y, z - 1 + a, Old.ELDRITCH, 4, 3);
                } else {
                    world.setBlock(x - 1 + b, y, z - 1 + a, Old.STAIRS_ELDRITCH, PED[a][b], 3);
                }
            }
        }
    }

    private static int[] corner(int exit) {
        return switch (exit) {
            case 2 -> new int[]{8, 8};
            case 3 -> new int[]{-8, 8};
            case 4 -> new int[]{8, -8};
            case 5 -> new int[]{-8, -8};
            default -> new int[]{0, 0};
        };
    }

    private static void wardenRoom(OuterWorld world, ServerLevel level, BlockPos lock, int cx, int cz, int exit) {
        announce(level, lock, "tc.boss.warden");
        int x = cx * 16 + 16;
        int y = OuterLands.FLOOR;
        int z = cz * 16 + 16;
        int[] d = corner(exit);
        int x2 = x + d[0];
        int z2 = z + d[1];
        thaumcraft.common.blocks.EldritchObeliskBlock.place(level, new BlockPos(x2, y + 4, z));
        thaumcraft.common.blocks.EldritchObeliskBlock.place(level, new BlockPos(x, y + 4, z2));
        world.setBlock(x2, y + 2, z, Old.ELDRITCH, 3, 3);
        world.setBlock(x, y + 2, z2, Old.ELDRITCH, 3, 3);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a != 0 && b != 0 && world.rand.nextFloat() < 0.9f) {
                    float rr = world.rand.nextFloat();
                    world.setBlock(x2 + a, y + 2, z + b, Old.LOOT_URN, rr < 0.1f ? 2 : rr < 0.3f ? 1 : 0, 3);
                }
                if (a != 0 && b != 0 && world.rand.nextFloat() < 0.9f) {
                    float rr = world.rand.nextFloat();
                    world.setBlock(x + a, y + 2, z2 + b, Old.LOOT_URN, rr < 0.1f ? 2 : rr < 0.3f ? 1 : 0, 3);
                }
            }
        }
        for (int[] c : new int[][]{{-2, -2}, {-2, 2}, {2, 2}, {2, -2}}) {
            world.setBlock(x + c[0], y + 3, z + c[1], Old.ELDRITCH, 10, 3);
            world.setBlock(x + c[0], y + 2, z + c[1], Old.COSMETIC_SOLID, 15, 3);
        }
        pedestal(world, x2, y + 2, z2);
        EldritchWarden boss = ModEntities.ELDRITCH_WARDEN.get().create(level);
        if (boss != null) {
            faceLock(boss, lock, x2 + 0.5, y + 3, z2 + 0.5);
            boss.finalizeSpawn(level, level.getCurrentDifficultyAt(lock), MobSpawnType.STRUCTURE, null, null);
            boss.restrictTo(new BlockPos(x, y + 2, z), 32);
            level.addFreshEntity(boss);
        }
    }

    private static void golemRoom(OuterWorld world, ServerLevel level, BlockPos lock, int cx, int cz, int exit) {
        announce(level, lock, "tc.boss.golem");
        int x = cx * 16 + 16;
        int y = OuterLands.FLOOR;
        int z = cz * 16 + 16;
        int[] d = corner(exit);
        int x2 = d[0];
        int z2 = d[1];
        for (int[] c : new int[][]{{x + x2, z + z2}, {x - x2, z + z2}, {x + x2, z - z2}}) {
            thaumcraft.common.blocks.EldritchObeliskBlock.place(level, new BlockPos(c[0], y + 4, c[1]));
            world.setBlock(c[0], y + 2, c[1], Old.ELDRITCH, 3, 3);
        }
        pedestal(world, x, y + 2, z);
        for (int a = -10; a <= 10; a++) {
            for (int b = -10; b <= 10; b++) {
                if ((a < -2 && b < -2 || a > 2 && b > 2 || a < -2 && b > 2 || a > 2 && b < -2) && world.rand.nextFloat() < 0.15f
                        && world.isAirBlock(x + a, y + 2, z + b)) {
                    float rr = world.rand.nextFloat();
                    int md = rr < 0.05f ? 2 : rr < 0.2f ? 1 : 0;
                    world.setBlock(x + a, y + 2, z + b, world.rand.nextFloat() < 0.3f ? Old.LOOT_CRATE : Old.LOOT_URN, md, 3);
                }
            }
        }
        EldritchGolem boss = ModEntities.ELDRITCH_GOLEM.get().create(level);
        if (boss != null) {
            faceLock(boss, lock, x + 0.5, y + 3, z + 0.5);
            boss.finalizeSpawn(level, level.getCurrentDifficultyAt(lock), MobSpawnType.STRUCTURE, null, null);
            level.addFreshEntity(boss);
        }
    }

    private static void cultistRoom(OuterWorld world, ServerLevel level, BlockPos lock, int cx, int cz) {
        announce(level, lock, "tc.boss.crimson");
        int x = cx * 16 + 16;
        int y = OuterLands.FLOOR;
        int z = cz * 16 + 16;
        for (int a = -4; a <= 4; a++) {
            for (int b = -4; b <= 4; b++) {
                if ((Math.abs(a) != 2 && Math.abs(b) != 2 || !world.rand.nextBoolean())
                        && (Math.abs(a) != 3 && Math.abs(b) != 3 || !(world.rand.nextFloat() > 0.33f))
                        && (Math.abs(a) != 4 && Math.abs(b) != 4 || !(world.rand.nextFloat() > 0.25f))) {
                    world.setBlock(x + b, y + 1, z + a, Old.ELDRITCH, 7, 3);
                }
            }
        }
        for (int a = 0; a < 5; a++) {
            for (int b = 0; b < 5; b++) {
                if (a == 0 || a == 4 || b == 0 || b == 4) {
                    int px = x - 8 + b * 4;
                    int pz = z - 8 + a * 4;
                    world.setBlock(px, y + 2, pz, Old.COSMETIC_SOLID, 11, 3);
                    world.setBlock(px, y + 3, pz, Old.ELDRITCH, 5, 3);
                    world.setBlock(px, y + 4, pz, Old.SLAB_STONE, 1, 3);
                    world.setBlock(px, y + 10, pz, Old.COSMETIC_SOLID, 11, 3);
                    world.setBlock(px, y + 9, pz, Old.ELDRITCH, 5, 3);
                    world.setBlock(px, y + 8, pz, Old.SLAB_STONE, 9, 3);
                }
            }
        }
        CultistPortal boss = ModEntities.CULTIST_PORTAL.get().create(level);
        if (boss != null) {
            boss.moveTo(x + 0.5, y + 2, z + 0.5, 0f, 0f);
            level.addFreshEntity(boss);
        }
    }

    private static void taintRoom(OuterWorld world, ServerLevel level, BlockPos lock, int cx, int cz) {
        announce(level, lock, "tc.boss.taint");
        int x = cx * 16 + 16;
        int y = OuterLands.FLOOR;
        int z = cz * 16 + 16;
        for (int a = -12; a <= 12; a++) {
            for (int b = -12; b <= 12; b++) {
                world.setBiomeAt(x + b, z + a, thaumcraft.common.taint.Taint.BIOME);
                for (int c = 0; c < 9; c++) {
                    if (world.isAirBlock(x + b, y + 2 + c, z + a) && world.isAdjacentToSolidBlock(x + b, y + 2 + c, z + a) && world.rand.nextInt(3) != 0) {
                        world.setBlock(x + b, y + 2 + c, z + a, Old.TAINT_FIBRES, world.rand.nextInt(4) == 0 ? 1 : 0, 3);
                    }
                }
                if (world.rand.nextFloat() < 0.15) {
                    world.setBlock(x + b, y + 2, z + a, Old.TAINT, 0, 3);
                    if (world.rand.nextFloat() < 0.2) {
                        world.setBlock(x + b, y + 3, z + a, Old.TAINT, 0, 3);
                    }
                }
                if ((Math.abs(a) != 4 && Math.abs(b) != 4 || !world.rand.nextBoolean())
                        && (Math.abs(a) < 5 && Math.abs(b) < 5 || !(world.rand.nextFloat() > 0.33f))
                        && (Math.abs(a) < 7 && Math.abs(b) < 7 || !(world.rand.nextFloat() > 0.25f))) {
                    world.setBlock(x + b, y + 1, z + a, Old.TAINT, 1, 3);
                }
            }
        }
        // посередине — гигант (на лёгкой и обычной сложности простое щупальце), вокруг ещё четыре, вперемешку
        boolean giant1 = level.getDifficulty() == Difficulty.HARD;
        boolean giant2 = level.random.nextBoolean();
        boolean giant4 = level.random.nextBoolean();
        tentacle(level, giant1, x + 0.5, y + 3, z + 0.5);
        tentacle(level, giant2, x + 3.5, y + 3, z + 3.5);
        tentacle(level, !giant2, x - 2.5, y + 3, z + 3.5);
        tentacle(level, giant4, x + 3.5, y + 3, z - 2.5);
        tentacle(level, !giant4, x - 2.5, y + 3, z - 2.5);
    }

    private static void tentacle(ServerLevel level, boolean giant, double x, double y, double z) {
        Taintacle t = (giant ? ModEntities.TAINTACLE_GIANT : ModEntities.TAINTACLE).get().create(level);
        if (t == null) {
            return;
        }
        t.moveTo(x, y, z, 0f, 0f);
        t.finalizeSpawn(level, level.getCurrentDifficultyAt(t.blockPosition()), MobSpawnType.STRUCTURE, null, null);
        if (!giant) {
            Champions.makeChampion(t, true);
        }
        level.addFreshEntity(t);
    }
}

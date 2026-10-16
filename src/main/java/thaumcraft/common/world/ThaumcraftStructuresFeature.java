package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.ObsidianTotemBlock;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEntities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Наземные постройки Thaumcraft (generateSurface и generateTotem оригинала) — раз на чанк выбирается одна из них:
 * курган с гробницей (1/150), кольцо Элдрича (1/66), каменный круг на вершине холма (1/40); если ничего не
 * встало, может появиться обсидиановый тотем с узлом (1/360). Каждой постройке достаётся свой тёмный узел.
 */
public class ThaumcraftStructuresFeature extends Feature<NoneFeatureConfiguration> {
    private record Placement(int x, int y, int z, BlockState state) {
    }

    private static List<Placement> mound;

    public ThaumcraftStructuresFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos chunk = new BlockPos(context.origin().getX() & ~15, 0, context.origin().getZ() & ~15);
        boolean built = false;
        // постройки шириной до 19 блоков: начало берём не дальше 12 блоков от угла чанка, чтобы не выйти из соседних
        int x = chunk.getX() + random.nextInt(13);
        int z = chunk.getZ() + random.nextInt(13);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 9;
        if (random.nextInt(150) == 0) {
            BlockPos origin = new BlockPos(x, y, z);
            if (mound(level, random, origin)) {
                built = true;
                NodeGenerator.createRandomNode(level, origin.offset(9, 8, 9), random, false, true);
            }
        } else if (random.nextInt(66) == 0) {
            int width = 11 + random.nextInt(6) * 2;
            int height = 11 + random.nextInt(6) * 2;
            BlockPos centre = new BlockPos(x, y + 8, z);
            if (EldritchRing.generate(level, random, centre, width, height)) {
                built = true;
                NodeGenerator.createRandomNode(level, centre.above(2), random, false, true);
            }
        } else if (random.nextInt(40) == 0) {
            BlockPos centre = new BlockPos(x + 3, y + 9, z + 3);
            if (hilltopStones(level, random, centre)) {
                built = true;
                NodeGenerator.createRandomNode(level, centre.above(5), random, false, true);
            }
        }
        if (!built && random.nextInt(360) == 0) {
            built = totem(level, random, new BlockPos(chunk.getX() + 4 + random.nextInt(8), 0, chunk.getZ() + 4 + random.nextInt(8)));
        }
        return built;
    }

    // ---------- общее ----------

    /** LocationIsValidSpawn оригинала: поверхность из камня, травы или земли не глубже двух блоков, над ней воздух. */
    private static boolean validGround(WorldGenLevel level, BlockPos pos) {
        int toAir = 0;
        while (!level.getBlockState(pos.above(toAir)).isAir()) {
            toAir++;
            if (toAir > 2) {
                return false;
            }
        }
        BlockPos top = pos.above(toAir - 1);
        BlockState here = level.getBlockState(top);
        if (!level.getBlockState(top.above()).isAir()) {
            return false;
        }
        if (ground(here)) {
            return true;
        }
        return (here.is(Blocks.SNOW) || here.is(Blocks.GRASS) || here.is(Blocks.FERN)) && ground(level.getBlockState(top.below()));
    }

    private static boolean ground(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }

    /** Сундук с добычей подземелья (ChestGenHooks "dungeonChest"), times раз подряд. */
    private static void fillChest(WorldGenLevel level, RandomSource random, BlockPos pos, int times) {
        if (!(level.getBlockEntity(pos) instanceof Container chest) || level.getServer() == null) {
            return;
        }
        var table = level.getServer().getLootData().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        for (int i = 0; i < times; i++) {
            LootParams params = new LootParams.Builder(level.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .create(LootContextParamSets.CHEST);
            table.fill(chest, params, random.nextLong());
        }
    }

    private static void spawner(WorldGenLevel level, RandomSource random, BlockPos pos, EntityType<?> type) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, random);
        }
    }

    /** Урна (70%) или ящик (30%) случайной редкости: редкая — 10%, необычная — 23%. */
    public static BlockState lootBlock(RandomSource random) {
        float roll = random.nextFloat();
        int tier = roll < 0.1f ? 2 : roll < 0.33f ? 1 : 0;
        boolean crate = random.nextFloat() < 0.3f;
        Block[] urns = {ModBlocks.LOOT_URN_COMMON.get(), ModBlocks.LOOT_URN_UNCOMMON.get(), ModBlocks.LOOT_URN_RARE.get()};
        Block[] crates = {ModBlocks.LOOT_CRATE_COMMON.get(), ModBlocks.LOOT_CRATE_UNCOMMON.get(), ModBlocks.LOOT_CRATE_RARE.get()};
        return (crate ? crates : urns)[tier].defaultBlockState();
    }

    // ---------- курган ----------

    private static List<Placement> moundBlocks() {
        if (mound == null) {
            List<Placement> list = new ArrayList<>();
            try (InputStream in = ThaumcraftStructuresFeature.class.getResourceAsStream("/data/thaumcraft/thaumcraft_structures/mound.txt");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split(" ", 4);
                    if (parts.length < 4) {
                        continue;
                    }
                    BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), parts[3], false).blockState();
                    list.add(new Placement(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), state));
                }
            } catch (Exception e) {
                throw new IllegalStateException("не прочитан курган", e);
            }
            mound = list;
        }
        return mound;
    }

    /** WorldGenMound оригинала: заросший травой холм, внутри — склеп с сундуком, урнами и двумя рассадниками нежити. */
    public static boolean mound(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int i = origin.getX();
        int j = origin.getY();
        int k = origin.getZ();
        if (!validGround(level, new BlockPos(i + 9, j + 9, k + 9)) || !validGround(level, new BlockPos(i, j + 9, k))
                || !validGround(level, new BlockPos(i + 18, j + 9, k)) || !validGround(level, new BlockPos(i + 18, j + 9, k + 18))
                || !validGround(level, new BlockPos(i, j + 9, k + 18))) {
            return false;
        }
        for (Placement p : moundBlocks()) {
            level.setBlock(origin.offset(p.x, p.y, p.z), p.state, 2);
        }
        level.setBlock(origin.offset(9, 1, 7), lootBlock(random), 2);
        level.setBlock(origin.offset(9, 1, 11), lootBlock(random), 2);
        BlockPos chest = origin.offset(10, 1, 9);
        if (random.nextInt(3) == 0) {
            level.setBlock(chest, Blocks.TRAPPED_CHEST.defaultBlockState(), 2);
            level.setBlock(origin.offset(10, -1, 9), Blocks.TNT.defaultBlockState(), 2);
        } else {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        }
        fillChest(level, random, chest, random.nextInt(5) == 0 ? 2 : 1);
        spawner(level, random, origin.offset(4, 5, 4), EntityType.SKELETON);
        spawner(level, random, origin.offset(4, 5, 14), EntityType.ZOMBIE);
        return true;
    }

    // ---------- каменный круг на холме ----------

    /** WorldGenHilltopStones оригинала: обсидиановая площадка выше 85-го блока, кольцо тотемов, в центре сундук над рассадником виспов. */
    public static boolean hilltopStones(WorldGenLevel level, RandomSource random, BlockPos centre) {
        int i = centre.getX();
        int j = centre.getY();
        int k = centre.getZ();
        if (j < 85) {
            return false;
        }
        for (int[] d : new int[][]{{-2, -2}, {0, 0}, {2, 0}, {2, 2}, {0, 2}}) {
            if (!validGround(level, new BlockPos(i + d[0], j, k + d[1]))) {
                return false;
            }
        }
        var biome = level.getBiome(centre);
        BlockState replace = biome.value().coldEnoughToSnow(centre) ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        boolean vines = !biome.value().coldEnoughToSnow(centre);
        Block totem = ModBlocks.OBSIDIAN_TOTEM.get();
        for (int x = i - 3; x <= i + 3; x++) {
            for (int z = k - 3; z <= k + 3; z++) {
                if ((x == i - 3 || x == i + 3) && (z == k - 3 || z == k + 3)) {
                    continue;
                }
                level.setBlock(new BlockPos(x, j, z), random.nextBoolean() ? ModBlocks.OBSIDIAN_TILE.get().defaultBlockState()
                        : Blocks.OBSIDIAN.defaultBlockState(), 2);
                boolean stop = false;
                for (int y = 1; y < 5; y++) {
                    if (j - y < level.getMinBuildHeight()) {
                        continue;
                    }
                    BlockState below = level.getBlockState(new BlockPos(x, j - y, z));
                    if (below.is(Blocks.SNOW) || below.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) || below.is(Blocks.GRASS) || below.isAir()) {
                        level.setBlock(new BlockPos(x, j - y, z), replace, 2);
                    }
                    if (x == i && z == k && y == 1) {
                        level.setBlock(new BlockPos(x, j + 1, z), ModBlocks.OBSIDIAN_TILE.get().defaultBlockState(), 2);
                        BlockPos chest = new BlockPos(x, j + 2, z);
                        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
                        fillChest(level, random, chest, 2);
                        spawner(level, random, new BlockPos(x, j, z), ModEntities.WISP.get());
                    }
                    boolean edge = (x == i - 3 || x == i + 3) && Math.abs((z - k) % 2) == 1 || (z == k - 3 || z == k + 3) && Math.abs((x - i) % 2) == 1;
                    if (!stop && edge) {
                        BlockPos pos = new BlockPos(x, j + y, z);
                        level.setBlock(pos, totem.defaultBlockState(), 2);
                        if (y >= 2 && random.nextBoolean()) {
                            stop = true;
                            if (vines) {
                                for (Direction side : Direction.Plane.HORIZONTAL) {
                                    if (random.nextInt(3) == 0 && level.isEmptyBlock(pos.relative(side))) {
                                        growVines(level, pos.relative(side), side.getOpposite());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        reconnectTotems(level, centre, 4, 6);
        return true;
    }

    /** Лоза свисает до четырёх блоков вниз, держась за грань тотема. */
    private static void growVines(WorldGenLevel level, BlockPos pos, Direction towards) {
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towards), true);
        level.setBlock(pos, vine, 2);
        for (int n = 0; n < 4; n++) {
            pos = pos.below();
            if (!level.isEmptyBlock(pos)) {
                return;
            }
            level.setBlock(pos, vine, 2);
        }
    }

    /** Тотемы ставились по одному; пересчитываем их связи, чтобы столбы выглядели целыми. */
    private static void reconnectTotems(WorldGenLevel level, BlockPos centre, int radius, int height) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, 0, -radius), centre.offset(radius, height, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ObsidianTotemBlock) {
                level.setBlock(pos, ObsidianTotemBlock.placed(state.getBlock(), level, pos), 2);
            }
        }
    }

    // ---------- тотем ----------

    /** generateTotem оригинала: на плитке столб из тотемов, на вершине — заряженный тотем с тёмным узлом. */
    public static boolean totem(WorldGenLevel level, RandomSource random, BlockPos column) {
        int x = column.getX();
        int z = column.getZ();
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
        if (level.getBlockState(top).is(net.minecraft.tags.BlockTags.LEAVES)) {
            do {
                top = top.below();
            } while (!level.getBlockState(top).is(Blocks.GRASS_BLOCK) && top.getY() > level.getMinBuildHeight() + 40);
        }
        BlockState ground = level.getBlockState(top);
        if (ground.is(Blocks.SNOW) || ground.is(Blocks.GRASS) || ground.is(Blocks.FERN)) {
            top = top.below();
            ground = level.getBlockState(top);
        }
        if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.SAND) || ground.is(Blocks.DIRT) || ground.is(Blocks.STONE)
                || ground.is(Blocks.NETHERRACK))) {
            return false;
        }
        int count = 1;
        while (free(level, top.above(count)) && count < 3) {
            count++;
        }
        if (count < 2) {
            return false;
        }
        level.setBlock(top, ModBlocks.OBSIDIAN_TILE.get().defaultBlockState(), 2);
        boolean charged = false;
        count = 1;
        while (free(level, top.above(count)) && count < 5) {
            BlockPos pos = top.above(count);
            level.setBlock(pos, ModBlocks.OBSIDIAN_TOTEM.get().defaultBlockState(), 2);
            if (count > 1 && random.nextInt(4) == 0) {
                level.setBlock(pos, ModBlocks.CHARGED_OBSIDIAN_TOTEM.get().defaultBlockState(), 2);
                NodeGenerator.fill(level, pos, random, false, true, false);
                count = 5;
                charged = true;
            }
            count++;
            if (count >= 5 && !charged) {
                BlockPos cap = top.above(5);
                level.setBlock(cap, ModBlocks.CHARGED_OBSIDIAN_TOTEM.get().defaultBlockState(), 2);
                NodeGenerator.fill(level, cap, random, false, true, false);
                charged = true;
            }
        }
        reconnectTotems(level, top, 0, 6);
        return true;
    }

    private static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.GRASS) || state.is(Blocks.FERN);
    }
}

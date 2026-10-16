package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.common.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Великое древо — перенос WorldGenGreatwoodTrees оригинала: алгоритм большого дуба со стволом 2x2,
 * выполненный дважды (второй ярус кроны шире и растёт от вершины первого ствола).
 * Параметр probability — шанс логова пауков под корнями (спавнер, паутина и сундук); у саженца он равен нулю.
 */
public class GreatwoodTreeFeature extends Feature<ProbabilityFeatureConfiguration> {

    public GreatwoodTreeFeature(Codec<ProbabilityFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean spiders = random.nextFloat() < context.config().probability;

        Generator generator = new Generator(level, RandomSource.create(random.nextLong()));
        if (!generator.generate(origin)) {
            return false;
        }
        if (spiders) {
            placeSpiderNest(level, random, origin);
        }
        return true;
    }

    private static void placeSpiderNest(WorldGenLevel level, RandomSource random, BlockPos origin) {
        BlockPos spawnerPos = origin.below();
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3);
        BlockEntity spawner = level.getBlockEntity(spawnerPos);
        if (!(spawner instanceof SpawnerBlockEntity spawnerEntity)) {
            return;
        }
        spawnerEntity.setEntityId(EntityType.CAVE_SPIDER, random);
        for (int i = 0; i < 50; i++) {
            BlockPos pos = origin.offset(random.nextInt(14) - 7, random.nextInt(10), random.nextInt(14) - 7);
            if (level.isEmptyBlock(pos) && touchesTree(level, pos)) {
                level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }
        BlockPos chestPos = origin.below(2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        RandomizableContainerBlockEntity.setLootTable(level, random, chestPos, BuiltInLootTables.SIMPLE_DUNGEON);
    }

    private static boolean touchesTree(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            var state = level.getBlockState(pos.relative(direction));
            if (state.is(ModBlocks.GREATWOOD_LEAVES.get()) || state.is(ModBlocks.GREATWOOD_LOG.get())) {
                return true;
            }
        }
        return false;
    }

    /** Состояние одной постройки дерева. Имена полей и ход вычислений повторяют оригинал. */
    private static final class Generator {
        private static final double HEIGHT_ATTENUATION = 0.618;
        private static final double BRANCH_SLOPE = 0.38;
        private static final double LEAF_DENSITY = 0.9;
        private static final int TRUNK_SIZE = 2;
        private static final int HEIGHT_LIMIT_LIMIT = 11;
        private static final int LEAF_DISTANCE_LIMIT = 4;

        private final WorldGenLevel level;
        private final RandomSource rand;
        private final TreeBuilder builder;
        private int baseX;
        private int baseY;
        private int baseZ;
        private int heightLimit;
        private int height;
        private double scaleWidth = 1.2;
        /** Узлы листвы: x, y, z и высота, на которой ветка отходит от ствола. */
        private List<int[]> leafNodes = List.of();

        Generator(WorldGenLevel level, RandomSource rand) {
            this.level = level;
            this.rand = rand;
            this.builder = new TreeBuilder(level, ModBlocks.GREATWOOD_LOG.get(), ModBlocks.GREATWOOD_LEAVES.get());
        }

        boolean generate(BlockPos origin) {
            baseX = origin.getX();
            baseY = origin.getY();
            baseZ = origin.getZ();
            heightLimit = HEIGHT_LIMIT_LIMIT + rand.nextInt(HEIGHT_LIMIT_LIMIT);

            // ствол 2x2 может встать с любым смещением на блок, лишь бы под всеми четырьмя была земля
            boolean valid = false;
            search:
            for (int a = -1; a < 2; a++) {
                next:
                for (int b = -1; b < 2; b++) {
                    for (int dx = 0; dx < TRUNK_SIZE; dx++) {
                        for (int dz = 0; dz < TRUNK_SIZE; dz++) {
                            if (!validTreeLocation(dx + a, dz + b)) {
                                continue next;
                            }
                        }
                    }
                    valid = true;
                    baseX += a;
                    baseZ += b;
                    break search;
                }
            }
            if (!valid) {
                return false;
            }

            buildTier();
            // второй ярус: шире и от вершины первого ствола
            scaleWidth = 1.66;
            baseX = origin.getX();
            baseY = origin.getY() + height;
            baseZ = origin.getZ();
            buildTier();

            builder.finish();
            return true;
        }

        private void buildTier() {
            generateLeafNodeList();
            for (int[] node : leafNodes) {
                generateLeafNode(node[0], node[1], node[2]);
            }
            generateLeafNodeBases();
            generateTrunk();
        }

        private boolean validTreeLocation(int dx, int dz) {
            int x = baseX + dx;
            int z = baseZ + dz;
            if (!builder.isSoil(new BlockPos(x, baseY - 1, z))) {
                return false;
            }
            int clear = checkBlockLine(new int[]{x, baseY, z}, new int[]{x, baseY + heightLimit - 1, z});
            if (clear == -1) {
                return true;
            }
            if (clear < 6) {
                return false;
            }
            heightLimit = clear;
            return true;
        }

        private void generateLeafNodeList() {
            height = (int) (heightLimit * HEIGHT_ATTENUATION);
            if (height >= heightLimit) {
                height = heightLimit - 1;
            }
            int nodesPerLayer = Math.max(1, (int) (1.382 + Math.pow(LEAF_DENSITY * heightLimit / 13.0, 2.0)));
            List<int[]> nodes = new ArrayList<>();
            int y = baseY + heightLimit - LEAF_DISTANCE_LIMIT;
            int trunkTop = baseY + height;
            int layer = y - baseY;
            nodes.add(new int[]{baseX, y, baseZ, trunkTop});
            y--;

            while (layer >= 0) {
                float size = layerSize(layer);
                if (size >= 0f) {
                    for (int i = 0; i < nodesPerLayer; i++) {
                        double radius = scaleWidth * size * (rand.nextFloat() + 0.328);
                        double angle = rand.nextFloat() * 2.0 * Math.PI;
                        int x = Mth.floor(radius * Math.sin(angle) + baseX + 0.5);
                        int z = Mth.floor(radius * Math.cos(angle) + baseZ + 0.5);
                        int[] node = {x, y, z};
                        int[] above = {x, y + LEAF_DISTANCE_LIMIT, z};
                        if (checkBlockLine(node, above) != -1) {
                            continue;
                        }
                        double horizontal = Math.sqrt(Math.pow(Math.abs(baseX - x), 2.0) + Math.pow(Math.abs(baseZ - z), 2.0));
                        double drop = horizontal * BRANCH_SLOPE;
                        int branchY = y - drop > trunkTop ? trunkTop : (int) (y - drop);
                        if (checkBlockLine(new int[]{baseX, branchY, baseZ}, node) == -1) {
                            nodes.add(new int[]{x, y, z, branchY});
                        }
                    }
                }
                y--;
                layer--;
            }
            leafNodes = nodes;
        }

        /** Радиус кроны на заданной высоте: нижние 30% голые, выше — полусфера. */
        private float layerSize(int layer) {
            if (layer < heightLimit * 0.3) {
                return -1.618f;
            }
            float half = heightLimit / 2f;
            float offset = half - layer;
            float size;
            if (offset == 0f) {
                size = half;
            } else if (Math.abs(offset) >= half) {
                size = 0f;
            } else {
                size = (float) Math.sqrt(half * half - offset * offset);
            }
            return size * 0.5f;
        }

        private void generateLeafNode(int x, int y, int z) {
            for (int layer = 0; layer < LEAF_DISTANCE_LIMIT; layer++) {
                float size = layer != 0 && layer != LEAF_DISTANCE_LIMIT - 1 ? 3f : 2f;
                generateLeafLayer(x, y + layer, z, size);
            }
        }

        private void generateLeafLayer(int x, int y, int z, float size) {
            int radius = (int) (size + 0.618);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = Math.pow(Math.abs(dx) + 0.5, 2.0) + Math.pow(Math.abs(dz) + 0.5, 2.0);
                    if (distance <= size * size && builder.isAirOrOwnLeaves(pos.set(x + dx, y, z + dz))) {
                        builder.placeLeaves(pos);
                    }
                }
            }
        }

        private void generateLeafNodeBases() {
            for (int[] node : leafNodes) {
                // ветки растут только из верхних 80% ствола
                if (node[3] - baseY >= heightLimit * 0.2) {
                    placeBlockLine(new int[]{baseX, node[3], baseZ}, new int[]{node[0], node[1], node[2]});
                }
            }
        }

        private void generateTrunk() {
            int top = baseY + height;
            for (int dx = 0; dx < TRUNK_SIZE; dx++) {
                for (int dz = 0; dz < TRUNK_SIZE; dz++) {
                    placeBlockLine(new int[]{baseX + dx, baseY, baseZ + dz}, new int[]{baseX + dx, top, baseZ + dz});
                }
            }
        }

        /** Линия из брёвен; бревно ложится вдоль той горизонтальной оси, по которой ушло дальше от начала. */
        private void placeBlockLine(int[] from, int[] to) {
            int[] delta = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
            int main = mainAxis(delta);
            if (delta[main] == 0) {
                return;
            }
            int a = (main + 1) % 3;
            int b = (main + 2) % 3;
            int step = delta[main] > 0 ? 1 : -1;
            double slopeA = (double) delta[a] / delta[main];
            double slopeB = (double) delta[b] / delta[main];
            int[] point = new int[3];
            for (int i = 0; i != delta[main] + step; i += step) {
                point[main] = Mth.floor(from[main] + i + 0.5);
                point[a] = Mth.floor(from[a] + i * slopeA + 0.5);
                point[b] = Mth.floor(from[b] + i * slopeB + 0.5);
                int offsetX = Math.abs(point[0] - from[0]);
                int offsetZ = Math.abs(point[2] - from[2]);
                int max = Math.max(offsetX, offsetZ);
                Direction.Axis axis = Direction.Axis.Y;
                if (max > 0) {
                    axis = offsetX == max ? Direction.Axis.X : Direction.Axis.Z;
                }
                builder.placeLog(new BlockPos(point[0], point[1], point[2]), axis);
            }
        }

        /** @return -1, если вся линия свободна (воздух или своя листва), иначе расстояние до первого препятствия */
        private int checkBlockLine(int[] from, int[] to) {
            int[] delta = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
            int main = mainAxis(delta);
            if (delta[main] == 0) {
                return -1;
            }
            int a = (main + 1) % 3;
            int b = (main + 2) % 3;
            int step = delta[main] > 0 ? 1 : -1;
            double slopeA = (double) delta[a] / delta[main];
            double slopeB = (double) delta[b] / delta[main];
            int[] point = new int[3];
            int end = delta[main] + step;
            int i;
            for (i = 0; i != end; i += step) {
                point[main] = from[main] + i;
                point[a] = Mth.floor(from[a] + i * slopeA);
                point[b] = Mth.floor(from[b] + i * slopeB);
                if (!builder.isAirOrOwnLeaves(new BlockPos(point[0], point[1], point[2]))) {
                    break;
                }
            }
            return i == end ? -1 : Math.abs(i);
        }

        private static int mainAxis(int[] delta) {
            int main = 0;
            for (int i = 1; i < 3; i++) {
                if (Math.abs(delta[i]) > Math.abs(delta[main])) {
                    main = i;
                }
            }
            return main;
        }
    }
}

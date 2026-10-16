package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import thaumcraft.common.registry.ModBlocks;

/**
 * Серебряное дерево — перенос WorldGenSilverwoodTrees оригинала: ствол крестом, корни, развилка под кроной
 * и шар листвы. Параметр probability — шанс на россыпь мерцалиста вокруг (при генерации мира 1, у саженца 0).
 */
public class SilverwoodTreeFeature extends Feature<ProbabilityFeatureConfiguration> {
    private static final int MIN_HEIGHT = 7;
    private static final int RANDOM_HEIGHT = 4;

    public SilverwoodTreeFeature(Codec<ProbabilityFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        int height = random.nextInt(RANDOM_HEIGHT) + MIN_HEIGHT;
        TreeBuilder builder = new TreeBuilder(level, ModBlocks.SILVERWOOD_LOG.get(), ModBlocks.SILVERWOOD_LEAVES.get());

        if (y < level.getMinBuildHeight() + 1 || y + height + 1 > level.getMaxBuildHeight()) {
            return false;
        }
        // место должно быть свободно: узко у ствола, широко под крону
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int yy = y + 1; yy <= y + 1 + height; yy++) {
            int spread = yy >= y + 1 + height - 2 ? 3 : 1;
            for (int xx = x - spread; xx <= x + spread; xx++) {
                for (int zz = z - spread; zz <= z + spread; zz++) {
                    if (!builder.isReplaceable(pos.set(xx, yy, zz))) {
                        return false;
                    }
                }
            }
        }
        if (!builder.isSoil(origin.below())) {
            return false;
        }
        level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), 19);

        // крона
        int start = y + height - 5;
        int end = y + height + 3 + random.nextInt(3);
        for (int yy = start; yy <= end; yy++) {
            int centerY = Mth.clamp(yy, y + height - 3, y + height);
            for (int xx = x - 5; xx <= x + 5; xx++) {
                for (int zz = z - 5; zz <= z + 5; zz++) {
                    double dx = xx - x;
                    double dy = yy - centerY;
                    double dz = zz - z;
                    if (dx * dx + dy * dy + dz * dz < 10 + random.nextInt(8) && builder.isReplaceable(pos.set(xx, yy, zz))) {
                        builder.placeLeaves(pos);
                    }
                }
            }
        }

        // ствол крестом
        // изредка в сердцевине попадается сучок с чистым узлом ауры; два подряд не бывает
        int knotChance = (int) (height * 1.5);
        boolean lastWasKnot = false;
        int top = 0;
        for (; top < height; top++) {
            if (builder.isReplaceable(pos.set(x, y + top, z))) {
                if (top > 0 && !lastWasKnot && random.nextInt(knotChance) == 0) {
                    BlockPos knot = new BlockPos(x, y + top, z);
                    builder.placeTrunkBlock(knot, ModBlocks.SILVERWOOD_KNOT.get().defaultBlockState());
                    NodeGenerator.fill(level, knot, random, true, false);
                    knotChance += height;
                    lastWasKnot = true;
                } else {
                    log(builder, x, y + top, z);
                    lastWasKnot = false;
                }
                log(builder, x - 1, y + top, z);
                log(builder, x + 1, y + top, z);
                log(builder, x, y + top, z - 1);
                log(builder, x, y + top, z + 1);
            }
        }
        log(builder, x, y + top, z);

        // корни
        for (int[] corner : CORNERS) {
            log(builder, x + corner[0], y, z + corner[1]);
            if (random.nextInt(3) != 0) {
                log(builder, x + corner[0], y + 1, z + corner[1]);
            }
        }
        builder.placeLog(new BlockPos(x - 2, y, z), Direction.Axis.X);
        builder.placeLog(new BlockPos(x + 2, y, z), Direction.Axis.X);
        builder.placeLog(new BlockPos(x, y, z - 2), Direction.Axis.Z);
        builder.placeLog(new BlockPos(x, y, z + 2), Direction.Axis.Z);
        log(builder, x - 2, y - 1, z);
        log(builder, x + 2, y - 1, z);
        log(builder, x, y - 1, z - 2);
        log(builder, x, y - 1, z + 2);

        // развилка под кроной
        for (int[] corner : CORNERS) {
            log(builder, x + corner[0], y + height - 4, z + corner[1]);
            if (random.nextInt(3) == 0) {
                log(builder, x + corner[0], y + height - 5, z + corner[1]);
            }
        }
        builder.placeLog(new BlockPos(x - 2, y + height - 4, z), Direction.Axis.X);
        builder.placeLog(new BlockPos(x + 2, y + height - 4, z), Direction.Axis.X);
        builder.placeLog(new BlockPos(x, y + height - 4, z - 2), Direction.Axis.Z);
        builder.placeLog(new BlockPos(x, y + height - 4, z + 2), Direction.Axis.Z);

        builder.finish();

        if (random.nextFloat() < context.config().probability) {
            placeShimmerleaf(level, random, origin);
        }
        return true;
    }

    private static final int[][] CORNERS = {{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};

    private static void log(TreeBuilder builder, int x, int y, int z) {
        builder.placeLog(new BlockPos(x, y, z), Direction.Axis.Y);
    }

    /** WorldGenCustomFlowers оригинала: 18 попыток в радиусе 8 блоков, только на траве. */
    private static void placeShimmerleaf(WorldGenLevel level, RandomSource random, BlockPos origin) {
        for (int i = 0; i < 18; i++) {
            BlockPos pos = origin.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
            if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, ModBlocks.SHIMMERLEAF.get().defaultBlockState(), 2);
            }
        }
    }
}

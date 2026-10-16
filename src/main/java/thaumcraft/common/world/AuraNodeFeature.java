package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Дикий узел ауры (generateWildNodes оригинала): появляется в воздухе, на высоте до трёх блоков над точкой,
 * которую выбрало размещение. Как часто и где — в data/thaumcraft/worldgen/placed_feature.
 */
public class AuraNodeFeature extends Feature<NoneFeatureConfiguration> {

    public AuraNodeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos pos = context.origin();

        BlockPos raised = pos.above(random.nextInt(4));
        if (level.getBlockState(raised).isAir() || level.getBlockState(raised).canBeReplaced()) {
            pos = raised;
        }
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).canBeReplaced()
                || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        return NodeGenerator.createRandomNode(level, pos, random, false);
    }
}

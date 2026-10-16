package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.taint.Taint;
import thaumcraft.common.taint.TaintFibresBlock;

/**
 * Волокна порчи при создании порченых земель (BiomeGenTaint.decorateSpecial оригинала): в каждом чанке десять попыток
 * вырастить волокна на траве и ещё восемь — где угодно на поверхности рядом с твёрдым блоком. Дальше порча
 * разрастается уже сама.
 */
public class TaintFibresFeature extends Feature<NoneFeatureConfiguration> {

    public TaintFibresFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean place(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.isAir() || state.canBeReplaced()) || !state.getFluidState().isEmpty() || !level.getBiome(pos).is(Taint.BIOME)) {
            return false;
        }
        level.setBlock(pos, TaintFibresBlock.stateFor(level, pos), 2);
        return true;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placed = false;
        for (int a = 0; a < 10; a++) {
            BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin.offset(random.nextInt(16), 0, random.nextInt(16))).below();
            if (level.getBlockState(top).is(Blocks.GRASS_BLOCK)) {
                placed |= place(level, top.above());
            } else if (level.getBlockState(top).canBeReplaced() && level.getBlockState(top.below()).is(Blocks.GRASS_BLOCK)) {
                placed |= place(level, top);
            }
        }
        for (int a = 0; a < 8; a++) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, origin.offset(random.nextInt(16), 0, random.nextInt(16)));
            if (level.getBlockState(pos).isAir() && Taint.adjacentToSolid(level, pos)) {
                placed |= place(level, pos);
            }
        }
        return placed;
    }
}

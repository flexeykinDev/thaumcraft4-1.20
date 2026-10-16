package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.blocks.ManaPodBlock;
import thaumcraft.common.registry.ModBlocks;

/**
 * Стручки маны и вишрумы волшебного леса (WorldGenManaPods и конец BiomeGenMagicalForest.decorate оригинала):
 * в каждом чанке десять попыток повесить стручок под бревном и восемь — вырастить вишрум на траве у ствола.
 */
public class MagicalPlantsFeature extends Feature<NoneFeatureConfiguration> {

    public MagicalPlantsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean nearLog(WorldGenLevel level, BlockPos pos) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(near).is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placed = false;
        BlockState pod = ModBlocks.MANA_POD.get().defaultBlockState();
        for (int a = 0; a < 10; a++) {
            int startX = origin.getX() + random.nextInt(16);
            int startZ = origin.getZ() + random.nextInt(16);
            int x = startX;
            int z = startZ;
            // поиск снизу вверх, от уровня моря: первое пустое место с пустотой под ним и бревном над ним
            for (int y = level.getSeaLevel() + 1; y < level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below())) {
                    if (pod.canSurvive(level, pos)) {
                        level.setBlock(pos, pod.setValue(ManaPodBlock.AGE, 3 + random.nextInt(5)), 2);
                        if (level.getBlockEntity(pos) instanceof ManaPodBlock.Pod grown) {
                            // стручок появляется уже подросшим, так что аспект у него определяется сразу
                            grown.aspect = random.nextInt(8) == 0 ? thaumcraft.api.aspects.Aspect.PLANT
                                    : thaumcraft.api.aspects.Aspect.primals().get(random.nextInt(thaumcraft.api.aspects.Aspect.primals().size()));
                        }
                        placed = true;
                        break;
                    }
                } else {
                    // не дальше трёх блоков от исходной точки, чтобы не выйти за пределы уже созданных чанков
                    x = startX + random.nextInt(4) - random.nextInt(4);
                    z = startZ + random.nextInt(4) - random.nextInt(4);
                }
            }
        }
        for (int a = 0; a < 8; a++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
            while (cursor.getY() > 50 && !level.getBlockState(cursor).is(Blocks.GRASS_BLOCK)) {
                cursor.move(0, -1, 0);
            }
            BlockPos above = cursor.above();
            if (level.getBlockState(cursor).is(Blocks.GRASS_BLOCK) && level.getBlockState(above).canBeReplaced()
                    && level.getFluidState(above).isEmpty() && nearLog(level, above)) {
                level.setBlock(above, ModBlocks.VISHROOM.get().defaultBlockState(), 2);
                placed = true;
            }
        }
        return placed;
    }
}

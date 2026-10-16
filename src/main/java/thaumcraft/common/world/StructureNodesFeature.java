package thaumcraft.common.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import thaumcraft.Thaumcraft;

import java.util.Map;

/**
 * Узел над храмами (generateSurface оригинала): над каждой пустынной пирамидой, храмом в джунглях и хижиной
 * ведьмы висит узел ауры — в трёх блоках над землёй. Какие постройки считаются, задаёт тег
 * thaumcraft:has_aura_node (в 1.20 к ним добавлено иглу — такая же одиночная постройка).
 */
public class StructureNodesFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<Structure> HAS_NODE = TagKey.create(Registries.STRUCTURE, Thaumcraft.id("has_aura_node"));

    public StructureNodesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        boolean placed = false;
        for (Map.Entry<Structure, StructureStart> entry : level.getChunk(context.origin()).getAllStarts().entrySet()) {
            if (!entry.getValue().isValid() || !registry.wrapAsHolder(entry.getKey()).is(HAS_NODE)) {
                continue;
            }
            BlockPos centre = entry.getValue().getBoundingBox().getCenter();
            BlockPos pos = new BlockPos(centre.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centre.getX(), centre.getZ()) + 3,
                    centre.getZ());
            if (level.isEmptyBlock(pos)) {
                placed |= NodeGenerator.createRandomNode(level, pos, context.random(), false);
            }
        }
        return placed;
    }
}

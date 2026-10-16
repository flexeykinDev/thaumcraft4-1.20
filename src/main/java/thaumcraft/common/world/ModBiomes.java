package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import thaumcraft.Thaumcraft;

import java.util.List;

/**
 * Биомы мода и их смена на ходу (Utils.setBiomeAt оригинала). В новых версиях биом хранится клетками 4x4x4,
 * поэтому меняется сразу весь столбец 4x4 блока.
 */
public final class ModBiomes {
    /** Жуткие земли: сами не генерируются, их создают вокруг себя тёмные узлы. */
    public static final ResourceKey<Biome> EERIE = ResourceKey.create(Registries.BIOME, Thaumcraft.id("eerie"));
    /** Волшебный лес: встречается в мире сам, а ещё растёт вокруг серебряных деревьев и чистых узлов. */
    public static final ResourceKey<Biome> MAGICAL_FOREST = ResourceKey.create(Registries.BIOME, Thaumcraft.id("magical_forest"));

    private ModBiomes() {
    }

    /** Какой биом записан в клетке хранения, без сглаживания границ. */
    public static boolean is(LevelReader level, BlockPos pos, ResourceKey<Biome> biome) {
        return level.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())).is(biome);
    }

    /** Меняет биом во всём столбце 4x4, куда попадает точка, и сообщает об этом игрокам рядом. */
    public static void set(ServerLevel level, BlockPos pos, ResourceKey<Biome> biome) {
        Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome);
        ChunkAccess chunk = level.getChunk(pos);
        int qx = QuartPos.fromBlock(pos.getX());
        int qz = QuartPos.fromBlock(pos.getZ());
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> x == qx && z == qz ? holder : chunk.getNoiseBiome(x, y, z),
                level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
    }
}

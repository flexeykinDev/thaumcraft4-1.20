package thaumcraft.common.world.outer;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * Лабиринт Запредельных земель — MazeHandler.generateEldritch оригинала: каждый чанк измерения, для которого в
 * {@link Labyrinth} есть клетка, застраивается её залом — порталом, четвертью зала хранителя, залом скрижали,
 * гнездом, библиотекой или коридором. Остальное — пустота.
 */
public class OuterLandsFeature extends Feature<NoneFeatureConfiguration> {
    public OuterLandsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getServer() == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        Cell cell = Labyrinth.get(level.getServer()).get(chunk.x, chunk.z);
        if (cell == null) {
            return false;
        }
        long seed = level.getSeed() ^ (chunk.x * 341873128712L + chunk.z * 132897987541L);
        Random random = new Random(seed);
        OuterWorld world = new OuterWorld(level, new Random(seed * 31L + 17L));
        int y = OuterLands.FLOOR;
        // списки украшений у генераторов общие, поэтому чанки строятся по одному
        synchronized (GenCommon.class) {
            switch (cell.feature) {
                case 1 -> GenPortal.generatePortal(world, random, chunk.x, chunk.z, y, cell);
                case 2, 3, 4, 5 -> GenBossRoom.generateRoom(world, random, chunk.x, chunk.z, y, cell);
                case 6 -> GenKeyRoom.generateRoom(world, random, chunk.x, chunk.z, y, cell);
                case 7 -> GenNestRoom.generateRoom(world, random, chunk.x, chunk.z, y, cell);
                case 8 -> GenLibraryRoom.generateRoom(world, random, chunk.x, chunk.z, y, cell);
                default -> GenPassage.generateDefaultPassage(world, random, chunk.x, chunk.z, y, cell);
            }
            GenCommon.processDecorations(world);
        }
        return true;
    }
}

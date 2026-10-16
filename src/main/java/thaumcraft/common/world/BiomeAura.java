package thaumcraft.common.world;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.List;

/**
 * Сила ауры и стихия биома — таблица BiomeHandler оригинала. Там она была привязана к типам
 * BiomeDictionary; здесь те же строки привязаны к тегам биомов 1.20.1.
 */
public final class BiomeAura {
    private record Info(TagKey<Biome> tag, int aura, @Nullable Aspect aspect) {
    }

    private static final List<Info> TABLE = List.of(
            new Info(Tags.Biomes.IS_WATER, 100, Aspect.WATER),
            new Info(BiomeTags.IS_OCEAN, 120, Aspect.WATER),
            new Info(BiomeTags.IS_RIVER, 100, Aspect.WATER),
            new Info(Tags.Biomes.IS_WET, 80, Aspect.WATER),
            new Info(Tags.Biomes.IS_HOT, 100, Aspect.FIRE),
            new Info(Tags.Biomes.IS_DESERT, 100, Aspect.FIRE),
            new Info(BiomeTags.IS_NETHER, 120, Aspect.FIRE),
            new Info(BiomeTags.IS_BADLANDS, 80, Aspect.FIRE),
            new Info(Tags.Biomes.IS_DENSE, 100, Aspect.ORDER),
            new Info(Tags.Biomes.IS_SNOWY, 80, Aspect.ORDER),
            new Info(Tags.Biomes.IS_COLD, 80, Aspect.ORDER),
            new Info(Tags.Biomes.IS_MUSHROOM, 140, Aspect.ORDER),
            new Info(Tags.Biomes.IS_CONIFEROUS, 100, Aspect.EARTH),
            new Info(BiomeTags.IS_FOREST, 120, Aspect.EARTH),
            new Info(Tags.Biomes.IS_SANDY, 80, Aspect.EARTH),
            new Info(BiomeTags.IS_BEACH, 80, Aspect.EARTH),
            new Info(BiomeTags.IS_SAVANNA, 80, Aspect.AIR),
            new Info(BiomeTags.IS_MOUNTAIN, 100, Aspect.AIR),
            new Info(BiomeTags.IS_HILL, 120, Aspect.AIR),
            new Info(Tags.Biomes.IS_PLAINS, 80, Aspect.AIR),
            new Info(Tags.Biomes.IS_DRY, 80, Aspect.ENTROPY),
            new Info(Tags.Biomes.IS_SPARSE, 80, Aspect.ENTROPY),
            new Info(Tags.Biomes.IS_SWAMP, 120, Aspect.ENTROPY),
            new Info(Tags.Biomes.IS_WASTELAND, 80, Aspect.ENTROPY),
            new Info(BiomeTags.IS_JUNGLE, 100, Aspect.PLANT),
            new Info(Tags.Biomes.IS_LUSH, 100, Aspect.PLANT),
            new Info(Tags.Biomes.IS_MAGICAL, 100, null),
            new Info(BiomeTags.IS_END, 80, Aspect.VOID),
            new Info(Tags.Biomes.IS_SPOOKY, 80, Aspect.SOUL),
            new Info(Tags.Biomes.IS_DEAD, 50, Aspect.DEATH));

    private static List<Info> matching(Holder<Biome> biome) {
        List<Info> out = new ArrayList<>();
        for (Info info : TABLE) {
            if (biome.is(info.tag())) {
                out.add(info);
            }
        }
        return out;
    }

    /** Средняя сила ауры по всем подходящим строкам; 100, если биом ни под одну не попал. */
    public static int getAura(Holder<Biome> biome) {
        List<Info> infos = matching(biome);
        if (infos.isEmpty()) {
            return 100;
        }
        int total = 0;
        for (Info info : infos) {
            total += info.aura();
        }
        return total / infos.size();
    }

    /** Стихия случайной подходящей строки; null, если биом ни под одну не попал или строка без стихии. */
    @Nullable
    public static Aspect getRandomAspect(Holder<Biome> biome, RandomSource random) {
        List<Info> infos = matching(biome);
        return infos.isEmpty() ? null : infos.get(random.nextInt(infos.size())).aspect();
    }

    private BiomeAura() {
    }
}

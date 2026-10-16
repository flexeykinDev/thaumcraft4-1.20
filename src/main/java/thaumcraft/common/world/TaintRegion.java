package thaumcraft.common.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import thaumcraft.Thaumcraft;
import thaumcraft.common.taint.Taint;

import java.util.function.Consumer;

/**
 * Порченые земли в обычном мире. В оригинале биом порчи попадал в общий список тёплых и прохладных биомов с малым
 * весом (2 против 10 у леса или равнины). В новых версиях биомы расставляет климатическая карта, и добавить свой
 * можно только через TerraBlender: мир делится на области, и в «нашей» области часть обычных биомов подменяется.
 * Здесь подменяются редкие разновидности леса и равнины, так что порченые земли встречаются пятнами обычного
 * для биома размера и остаются редкостью.
 */
public class TaintRegion extends Region {
    /** Вес области: у обычного мира он 10, так что наша занимает WEIGHT / (10 + WEIGHT) карты. */
    public static final int WEIGHT = 5;

    public TaintRegion() {
        super(Thaumcraft.id("overworld"), RegionType.OVERWORLD, WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.FLOWER_FOREST, Taint.BIOME);
            builder.replaceBiome(Biomes.SUNFLOWER_PLAINS, Taint.BIOME);
            // волшебный лес в оригинале встречался в два с половиной раза чаще порченых земель; здесь он занимает
            // место берёзовых лесов той же области
            builder.replaceBiome(Biomes.BIRCH_FOREST, ModBiomes.MAGICAL_FOREST);
            builder.replaceBiome(Biomes.OLD_GROWTH_BIRCH_FOREST, ModBiomes.MAGICAL_FOREST);
        });
    }
}

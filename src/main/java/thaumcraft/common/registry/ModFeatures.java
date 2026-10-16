package thaumcraft.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import thaumcraft.common.world.AuraNodeFeature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.GreatwoodTreeFeature;
import thaumcraft.common.world.SilverwoodTreeFeature;

/** Генераторы мира. Где и как часто они срабатывают, описано в data/thaumcraft/worldgen и forge/biome_modifier. */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Thaumcraft.MODID);

    public static final RegistryObject<Feature<ProbabilityFeatureConfiguration>> GREATWOOD_TREE =
            FEATURES.register("greatwood_tree", () -> new GreatwoodTreeFeature(ProbabilityFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<ProbabilityFeatureConfiguration>> SILVERWOOD_TREE =
            FEATURES.register("silverwood_tree", () -> new SilverwoodTreeFeature(ProbabilityFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> AURA_NODE =
            FEATURES.register("aura_node", () -> new AuraNodeFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TAINT_FIBRES =
            FEATURES.register("taint_fibres", () -> new thaumcraft.common.world.TaintFibresFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MAGICAL_PLANTS =
            FEATURES.register("magical_plants", () -> new thaumcraft.common.world.MagicalPlantsFeature(NoneFeatureConfiguration.CODEC));

    /** Курганы, каменные круги, кольца Элдрича и обсидиановые тотемы. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STRUCTURES =
            FEATURES.register("structures", () -> new thaumcraft.common.world.ThaumcraftStructuresFeature(NoneFeatureConfiguration.CODEC));

    /** Узлы над храмами и хижинами ведьм. */
    /** Залы лабиринта Запредельных земель. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OUTER_LANDS_MAZE =
            FEATURES.register("outer_lands_maze", () -> new thaumcraft.common.world.outer.OuterLandsFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STRUCTURE_NODES =
            FEATURES.register("structure_nodes", () -> new thaumcraft.common.world.StructureNodesFeature(NoneFeatureConfiguration.CODEC));

    /** Деревья из саженцев: без паучьего логова и без мерцалиста. */
    public static final AbstractTreeGrower GREATWOOD_GROWER = grower("greatwood_sapling_tree");
    public static final AbstractTreeGrower SILVERWOOD_GROWER = grower("silverwood_sapling_tree");

    private static AbstractTreeGrower grower(String feature) {
        ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registries.CONFIGURED_FEATURE, Thaumcraft.id(feature));
        return new AbstractTreeGrower() {
            @Override
            protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
                return key;
            }
        };
    }

    private ModFeatures() {
    }
}

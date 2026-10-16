package thaumcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModCreativeTabs;
import thaumcraft.common.registry.ModFeatures;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModMenus;
import thaumcraft.common.registry.ModRecipes;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

@Mod(Thaumcraft.MODID)
public class Thaumcraft {
    public static final String MODID = "thaumcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    // конструктор без аргументов и FMLJavaModLoadingContext.get(): конструктор с контекстом понимает только Forge 47.3+,
    // а NeoForge 1.20.1 (ответвление от 47.1) такой мод не создаёт
    @SuppressWarnings("removal")
    public Thaumcraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        thaumcraft.common.registry.ModFluids.TYPES.register(modBus);
        thaumcraft.common.registry.ModFluids.FLUIDS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        thaumcraft.common.registry.ModEnchantments.ENCHANTMENTS.register(modBus);
        thaumcraft.common.champion.Champions.ATTRIBUTES.register(modBus);
        thaumcraft.common.champion.ChestLootModifier.SERIALIZERS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        thaumcraft.common.registry.ModVillagers.POI_TYPES.register(modBus);
        thaumcraft.common.registry.ModVillagers.PROFESSIONS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModNetwork.register();

        LOGGER.info("Thaumcraft: зарегистрировано аспектов: {}", Aspect.all().size());
    }

    public static ResourceLocation id(String path) {
        return thaumcraft.common.Res.of(MODID, path);
    }
}

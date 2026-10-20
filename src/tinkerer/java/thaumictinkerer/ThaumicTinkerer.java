package thaumictinkerer;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import thaumictinkerer.common.registry.TTBlocks;
import thaumictinkerer.common.registry.TTCreativeTabs;
import thaumictinkerer.common.registry.TTItems;

/**
 * Thaumic Tinkerer (pixlepix, nekosune) — аддон к Thaumcraft 4. Отдельный мод поверх порта Thaumcraft: свои предметы
 * и блоки, а исследования, рецепты и тексты — данными, которые Thaumcraft подхватывает из всех модов.
 */
@Mod(ThaumicTinkerer.MODID)
public class ThaumicTinkerer {
    public static final String MODID = "thaumictinkerer";
    public static final Logger LOGGER = LogUtils.getLogger();

    // как и у Thaumcraft: конструктор без аргументов, чтобы мод создавался и на NeoForge 1.20.1
    @SuppressWarnings("removal")
    public ThaumicTinkerer() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        thaumictinkerer.common.item.kami.Kami.init();
        thaumictinkerer.common.network.TTNetwork.init();
        TTBlocks.BLOCKS.register(modBus);
        TTItems.ITEMS.register(modBus);
        thaumictinkerer.common.registry.TTBlockEntities.BLOCK_ENTITIES.register(modBus);
        thaumictinkerer.common.registry.TTMenus.MENUS.register(modBus);
        thaumictinkerer.common.registry.TTRecipes.SERIALIZERS.register(modBus);
        thaumictinkerer.common.enchantment.TTEnchantments.ENCHANTMENTS.register(modBus);
        thaumictinkerer.common.potion.TTEffects.EFFECTS.register(modBus);
        TTCreativeTabs.TABS.register(modBus);
        LOGGER.info("Thaumic Tinkerer: предметов {}, блоков {}", TTItems.ITEMS.getEntries().size(), TTBlocks.BLOCKS.getEntries().size());
    }

    public static ResourceLocation id(String path) {
        return thaumcraft.common.Res.of(MODID, path);
    }
}

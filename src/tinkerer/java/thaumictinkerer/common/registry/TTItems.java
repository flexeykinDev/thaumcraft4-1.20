package thaumictinkerer.common.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.BrightNitorItem;
import thaumictinkerer.common.item.CleansingTalismanItem;
import thaumictinkerer.common.item.GasItem;
import thaumictinkerer.common.item.GasRemoverItem;
import thaumictinkerer.common.item.RevealingHelmItem;
import thaumictinkerer.common.item.ShareTomeItem;
import thaumictinkerer.common.item.SpellClothItem;
import thaumictinkerer.common.item.XpTalismanItem;

/** Предметы Thaumic Tinkerer (порядок регистрации — порядок во вкладке творческого режима). */
public final class TTItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ThaumicTinkerer.MODID);

    public static final RegistryObject<Item> DARK_QUARTZ = ITEMS.register("dark_quartz", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DARK_QUARTZ_BLOCK = block(TTBlocks.DARK_QUARTZ_BLOCK);
    public static final RegistryObject<Item> CHISELED_DARK_QUARTZ_BLOCK = block(TTBlocks.CHISELED_DARK_QUARTZ_BLOCK);
    public static final RegistryObject<Item> DARK_QUARTZ_PILLAR = block(TTBlocks.DARK_QUARTZ_PILLAR);
    public static final RegistryObject<Item> DARK_QUARTZ_SLAB = block(TTBlocks.DARK_QUARTZ_SLAB);
    public static final RegistryObject<Item> DARK_QUARTZ_STAIRS = block(TTBlocks.DARK_QUARTZ_STAIRS);
    public static final RegistryObject<Item> SHARE_TOME = ITEMS.register("share_tome", ShareTomeItem::new);
    public static final RegistryObject<Item> GASEOUS_LIGHT = ITEMS.register("gaseous_light", () -> new GasItem(TTBlocks.GASEOUS_LIGHT));
    public static final RegistryObject<Item> GASEOUS_SHADOW = ITEMS.register("gaseous_shadow", () -> new GasItem(TTBlocks.GASEOUS_SHADOW));
    public static final RegistryObject<Item> GAS_REMOVER = ITEMS.register("gas_remover", GasRemoverItem::new);
    public static final RegistryObject<Item> BRIGHT_NITOR = ITEMS.register("bright_nitor", BrightNitorItem::new);
    public static final RegistryObject<Item> FUNNEL = block(TTBlocks.FUNNEL);
    public static final RegistryObject<Item> REPAIRER = ITEMS.register("repairer", () -> new BlockItem(TTBlocks.REPAIRER.get(),
            new Item.Properties()) {
        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
            consumer.accept(thaumictinkerer.client.TTClientHooks.repairerItem());
        }
    });
    public static final RegistryObject<Item> SPELL_CLOTH = ITEMS.register("spell_cloth", SpellClothItem::new);
    public static final RegistryObject<Item> XP_TALISMAN = ITEMS.register("xp_talisman", XpTalismanItem::new);
    public static final RegistryObject<Item> CLEANSING_TALISMAN = ITEMS.register("cleansing_talisman", CleansingTalismanItem::new);
    public static final RegistryObject<Item> REVEALING_HELM = ITEMS.register("revealing_helm", RevealingHelmItem::new);
    public static final RegistryObject<Item> ASPECT_ANALYZER = block(TTBlocks.ASPECT_ANALYZER);
    public static final RegistryObject<Item> PLATFORM = block(TTBlocks.PLATFORM);

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private TTItems() {
    }
}

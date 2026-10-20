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
    // часть 2: фокусы и зачарователь
    public static final RegistryObject<Item> FOCUS_SMELT = ITEMS.register("focus_smelt", thaumictinkerer.common.item.focus.TTFoci.Smelt::new);
    public static final RegistryObject<Item> FOCUS_FLIGHT = ITEMS.register("focus_flight", thaumictinkerer.common.item.focus.TTFoci.Flight::new);
    public static final RegistryObject<Item> FOCUS_DEFLECT = ITEMS.register("focus_deflect", thaumictinkerer.common.item.focus.TTFoci.Deflect::new);
    public static final RegistryObject<Item> FOCUS_TELEKINESIS = ITEMS.register("focus_telekinesis",
            thaumictinkerer.common.item.focus.TTFoci.Telekinesis::new);
    public static final RegistryObject<Item> FOCUS_DISLOCATION = ITEMS.register("focus_dislocation",
            thaumictinkerer.common.item.focus.TTFoci.Dislocation::new);
    public static final RegistryObject<Item> FOCUS_HEAL = ITEMS.register("focus_heal", thaumictinkerer.common.item.focus.TTFoci.Heal::new);
    public static final RegistryObject<Item> FOCUS_ENDER_CHEST = ITEMS.register("focus_ender_chest",
            thaumictinkerer.common.item.focus.TTFoci.EnderChest::new);
    public static final RegistryObject<Item> ENCHANTER = block(TTBlocks.ENCHANTER);
    // часть 2б
    public static final RegistryObject<Item> MAGNET = magnet(TTBlocks.MAGNET, false);
    public static final RegistryObject<Item> MOB_MAGNET = magnet(TTBlocks.MOB_MAGNET, true);
    public static final RegistryObject<Item> SOUL_MOULD = ITEMS.register("soul_mould", thaumictinkerer.common.item.SoulMouldItem::new);
    public static final RegistryObject<Item> TRANSVECTOR_INTERFACE = block(TTBlocks.TRANSVECTOR_INTERFACE);
    public static final RegistryObject<Item> CONNECTOR = ITEMS.register("connector", thaumictinkerer.common.item.ConnectorItem::new);
    public static final RegistryObject<Item> TRANSVECTOR_DISLOCATOR = block(TTBlocks.TRANSVECTOR_DISLOCATOR);
    public static final RegistryObject<Item> MOBILIZER = block(TTBlocks.MOBILIZER);
    public static final RegistryObject<Item> RELAY = block(TTBlocks.RELAY);
    public static final RegistryObject<Item> ANIMATION_TABLET = block(TTBlocks.ANIMATION_TABLET);
    public static final RegistryObject<Item> REMOTE_PLACER = block(TTBlocks.REMOTE_PLACER);
    // часть 2в
    public static final RegistryObject<Item> BLOOD_SWORD = ITEMS.register("blood_sword", thaumictinkerer.common.item.BloodSwordItem::new);
    public static final RegistryObject<Item> SUMMON = block(TTBlocks.SUMMON);
    public static final RegistryObject<Item> SOUL_ASPECT = ITEMS.register("soul_aspect",
            () -> new thaumictinkerer.common.item.SoulAspectItem(thaumictinkerer.common.item.SoulAspectItem.Kind.PLAIN));
    public static final RegistryObject<Item> CONDENSED_SOUL_ASPECT = ITEMS.register("condensed_soul_aspect",
            () -> new thaumictinkerer.common.item.SoulAspectItem(thaumictinkerer.common.item.SoulAspectItem.Kind.CONDENSED));
    public static final RegistryObject<Item> INFUSED_SOUL_ASPECT = ITEMS.register("infused_soul_aspect",
            () -> new thaumictinkerer.common.item.SoulAspectItem(thaumictinkerer.common.item.SoulAspectItem.Kind.INFUSED));

    private static RegistryObject<Item> magnet(RegistryObject<Block> block, boolean mobs) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override
            public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
                consumer.accept(thaumictinkerer.client.TTClientHooks.magnetItem(mobs));
            }
        });
    }
    // часть 2г
    public static final RegistryObject<Item> FIRE_AIR = block(TTBlocks.FIRE_AIR);
    public static final RegistryObject<Item> FIRE_IGNIS = block(TTBlocks.FIRE_IGNIS);
    public static final RegistryObject<Item> FIRE_WATER = block(TTBlocks.FIRE_WATER);
    public static final RegistryObject<Item> FIRE_EARTH = block(TTBlocks.FIRE_EARTH);
    public static final RegistryObject<Item> FIRE_ORDER = block(TTBlocks.FIRE_ORDER);
    public static final RegistryObject<Item> FIRE_CHAOS = block(TTBlocks.FIRE_CHAOS);
    public static final RegistryObject<Item> INFUSED_SEEDS = ITEMS.register("infused_seeds", thaumictinkerer.common.item.InfusedSeedsItem::new);
    public static final RegistryObject<Item> INFUSED_GRAIN_AIR = ITEMS.register("infused_grain_aer",
            () -> new thaumictinkerer.common.item.InfusedGrainItem(thaumcraft.api.aspects.Aspect.AIR));
    public static final RegistryObject<Item> INFUSED_GRAIN_FIRE = ITEMS.register("infused_grain_ignis",
            () -> new thaumictinkerer.common.item.InfusedGrainItem(thaumcraft.api.aspects.Aspect.FIRE));
    public static final RegistryObject<Item> INFUSED_GRAIN_EARTH = ITEMS.register("infused_grain_terra",
            () -> new thaumictinkerer.common.item.InfusedGrainItem(thaumcraft.api.aspects.Aspect.EARTH));
    public static final RegistryObject<Item> INFUSED_GRAIN_WATER = ITEMS.register("infused_grain_aqua",
            () -> new thaumictinkerer.common.item.InfusedGrainItem(thaumcraft.api.aspects.Aspect.WATER));
    public static final RegistryObject<Item> INFUSED_POTION_AIR = ITEMS.register("infused_potion_aer",
            () -> new thaumictinkerer.common.item.InfusedPotionItem(thaumcraft.api.aspects.Aspect.AIR, thaumictinkerer.common.potion.TTEffects.AIR));
    public static final RegistryObject<Item> INFUSED_POTION_FIRE = ITEMS.register("infused_potion_ignis",
            () -> new thaumictinkerer.common.item.InfusedPotionItem(thaumcraft.api.aspects.Aspect.FIRE, thaumictinkerer.common.potion.TTEffects.FIRE));
    public static final RegistryObject<Item> INFUSED_POTION_EARTH = ITEMS.register("infused_potion_terra",
            () -> new thaumictinkerer.common.item.InfusedPotionItem(thaumcraft.api.aspects.Aspect.EARTH, thaumictinkerer.common.potion.TTEffects.EARTH));
    public static final RegistryObject<Item> INFUSED_POTION_WATER = ITEMS.register("infused_potion_aqua",
            () -> new thaumictinkerer.common.item.InfusedPotionItem(thaumcraft.api.aspects.Aspect.WATER, thaumictinkerer.common.potion.TTEffects.WATER));

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private TTItems() {
    }
}

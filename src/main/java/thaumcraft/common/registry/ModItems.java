package thaumcraft.common.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.AlumentumItem;
import thaumcraft.common.items.JarBlockItem;
import thaumcraft.common.items.PhialItem;
import thaumcraft.common.items.ResearchNotesItem;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.items.ThaumonomiconItem;
import thaumcraft.common.items.wands.WandItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Thaumcraft.MODID);

    // Осколки (в оригинале ItemShard, мета 0-6)
    public static final RegistryObject<Item> SHARD_AIR = simple("shard_air");
    public static final RegistryObject<Item> SHARD_FIRE = simple("shard_fire");
    public static final RegistryObject<Item> SHARD_WATER = simple("shard_water");
    public static final RegistryObject<Item> SHARD_EARTH = simple("shard_earth");
    public static final RegistryObject<Item> SHARD_ORDER = simple("shard_order");
    public static final RegistryObject<Item> SHARD_ENTROPY = simple("shard_entropy");
    public static final RegistryObject<Item> SHARD_BALANCED = simple("shard_balanced");

    public static final RegistryObject<Item> THAUMOMETER = ITEMS.register("thaumometer", ThaumometerItem::new);

    public static final RegistryObject<Item> THAUMONOMICON = ITEMS.register("thaumonomicon", ThaumonomiconItem::new);

    public static final RegistryObject<Item> SCRIBING_TOOLS = ITEMS.register("scribing_tools", ScribingToolsItem::new);
    public static final RegistryObject<Item> RESEARCH_NOTES = ITEMS.register("research_notes", ResearchNotesItem::new);

    // Жезлы (в оригинале ItemWandCasting и ItemWandCap)
    public static final RegistryObject<Item> WAND = ITEMS.register("wand", WandItem::new);
    public static final RegistryObject<Item> WAND_CAP_IRON = simple("wand_cap_iron");
    public static final RegistryObject<Item> WAND_CAP_GOLD = simple("wand_cap_gold");
    public static final RegistryObject<Item> WAND_ROD_GREATWOOD = simple("wand_rod_greatwood");
    public static final RegistryObject<Item> WAND_ROD_SILVERWOOD = simple("wand_rod_silverwood");
    public static final RegistryObject<Item> WAND_ROD_OBSIDIAN = simple("wand_rod_obsidian");
    public static final RegistryObject<Item> WAND_ROD_ICE = simple("wand_rod_ice");
    public static final RegistryObject<Item> WAND_ROD_QUARTZ = simple("wand_rod_quartz");
    public static final RegistryObject<Item> WAND_ROD_REED = simple("wand_rod_reed");
    public static final RegistryObject<Item> WAND_ROD_BLAZE = simple("wand_rod_blaze");
    public static final RegistryObject<Item> WAND_ROD_BONE = simple("wand_rod_bone");
    public static final RegistryObject<Item> WAND_ROD_GREATWOOD_STAFF = simple("wand_rod_greatwood_staff");
    public static final RegistryObject<Item> WAND_ROD_OBSIDIAN_STAFF = simple("wand_rod_obsidian_staff");
    public static final RegistryObject<Item> WAND_ROD_SILVERWOOD_STAFF = simple("wand_rod_silverwood_staff");
    public static final RegistryObject<Item> WAND_ROD_ICE_STAFF = simple("wand_rod_ice_staff");
    public static final RegistryObject<Item> WAND_ROD_QUARTZ_STAFF = simple("wand_rod_quartz_staff");
    public static final RegistryObject<Item> WAND_ROD_REED_STAFF = simple("wand_rod_reed_staff");
    public static final RegistryObject<Item> WAND_ROD_BLAZE_STAFF = simple("wand_rod_blaze_staff");
    public static final RegistryObject<Item> WAND_ROD_BONE_STAFF = simple("wand_rod_bone_staff");
    public static final RegistryObject<Item> WAND_ROD_PRIMAL_STAFF = simple("wand_rod_primal_staff");
    public static final RegistryObject<Item> WAND_CAP_COPPER = simple("wand_cap_copper");
    // новая механика: в ванили есть медь, но нет медного самородка (в оригинале ItemNugget мета 1)
    public static final RegistryObject<Item> COPPER_NUGGET = simple("copper_nugget");
    public static final RegistryObject<Item> PRIMAL_CHARM = simple("primal_charm");
    // заготовки украшений (ItemBaubleBlanks оригинала): сами ничего не делают, идут в рецепты
    public static final RegistryObject<Item> BAUBLE_AMULET = ITEMS.register("bauble_amulet", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BAUBLE_RING = ITEMS.register("bauble_ring", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BAUBLE_BELT = ITEMS.register("bauble_belt", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FOCUS_POUCH = ITEMS.register("focus_pouch", thaumcraft.common.items.wands.FocusPouchItem::new);
    public static final RegistryObject<Item> FOCUS_FIRE = ITEMS.register("focus_fire", thaumcraft.common.items.wands.foci.FireFocus::new);
    public static final RegistryObject<Item> FOCUS_SHOCK = ITEMS.register("focus_shock", thaumcraft.common.items.wands.foci.ShockFocus::new);
    public static final RegistryObject<Item> FOCUS_EXCAVATION = ITEMS.register("focus_excavation",
            thaumcraft.common.items.wands.foci.ExcavationFocus::new);
    public static final RegistryObject<Item> FOCUS_PECH = ITEMS.register("focus_pech", thaumcraft.common.items.wands.foci.PechFocus::new);
    public static final RegistryObject<Item> FOCUS_PRIMAL = ITEMS.register("focus_primal", thaumcraft.common.items.wands.foci.PrimalFocus::new);
    public static final RegistryObject<Item> FOCUS_PORTABLE_HOLE = ITEMS.register("focus_portable_hole",
            thaumcraft.common.items.wands.foci.PortableHoleFocus::new);
    public static final RegistryObject<Item> FOCUS_HELLBAT = ITEMS.register("focus_hellbat",
            thaumcraft.common.items.wands.foci.HellbatFocus::new);
    public static final RegistryObject<Item> FOCUS_TRADE = ITEMS.register("focus_trade", thaumcraft.common.items.wands.foci.TradeFocus::new);
    public static final RegistryObject<Item> FOCUS_WARDING = ITEMS.register("focus_warding",
            thaumcraft.common.items.wands.foci.WardingFocus::new);
    public static final RegistryObject<Item> FOCUS_FROST = ITEMS.register("focus_frost", thaumcraft.common.items.wands.foci.FrostFocus::new);
    public static final RegistryObject<Item> WAND_CAP_THAUMIUM_INERT = simple("wand_cap_thaumium_inert");
    public static final RegistryObject<Item> WAND_CAP_THAUMIUM = simple("wand_cap_thaumium");
    public static final RegistryObject<Item> WAND_CAP_VOID_INERT = simple("wand_cap_void_inert");
    public static final RegistryObject<Item> WAND_CAP_VOID = simple("wand_cap_void");

    // Ресурсы (в оригинале ItemResource и ItemNugget)
    public static final RegistryObject<Item> AMBER = simple("amber");
    public static final RegistryObject<Item> QUICKSILVER = simple("quicksilver");
    public static final RegistryObject<Item> QUICKSILVER_NUGGET = simple("quicksilver_nugget");
    // новая механика руд: киноварь выпадает сырьём, на которое действует Удача (в оригинале выпадал блок)
    public static final RegistryObject<Item> RAW_CINNABAR = simple("raw_cinnabar");

    // Алхимия (в оригинале ItemResource мета 0, 1, 2, 4 и ItemNugget мета 6, 16, 31)
    public static final RegistryObject<Item> ALUMENTUM = ITEMS.register("alumentum", AlumentumItem::new);
    public static final RegistryObject<Item> NITOR = block(ModBlocks.NITOR);
    public static final RegistryObject<Item> TALLOW = simple("tallow");
    public static final RegistryObject<Item> THAUMIUM_INGOT = simple("thaumium_ingot");
    public static final RegistryObject<Item> THAUMIUM_NUGGET = simple("thaumium_nugget");
    public static final RegistryObject<Item> CLUSTER_IRON = simple("cluster_iron");
    public static final RegistryObject<Item> CLUSTER_GOLD = simple("cluster_gold");
    // в оригинале медь появлялась, только если её добавлял другой мод; теперь она есть в самой игре
    public static final RegistryObject<Item> CLUSTER_COPPER = simple("cluster_copper");
    // олово, серебро и свинец (itemNugget меты 2–4 и 18–20): есть в игре, только если металл даёт другой мод
    public static final RegistryObject<Item> CLUSTER_TIN = simple("cluster_tin");
    public static final RegistryObject<Item> CLUSTER_SILVER = simple("cluster_silver");
    public static final RegistryObject<Item> CLUSTER_LEAD = simple("cluster_lead");
    public static final RegistryObject<Item> TIN_NUGGET = simple("tin_nugget");
    public static final RegistryObject<Item> SILVER_NUGGET = simple("silver_nugget");
    public static final RegistryObject<Item> LEAD_NUGGET = simple("lead_nugget");
    public static final RegistryObject<Item> WAND_CAP_SILVER_INERT = simple("wand_cap_silver_inert");
    public static final RegistryObject<Item> WAND_CAP_SILVER = simple("wand_cap_silver");
    public static final RegistryObject<Item> CLUSTER_CINNABAR = simple("cluster_cinnabar");
    public static final RegistryObject<Item> CRUCIBLE = block(ModBlocks.CRUCIBLE);
    public static final RegistryObject<Item> FLUX_GOO = block(ModBlocks.FLUX_GOO);
    // Перегонка эссенции (в оригинале ItemEssence, ItemResource мета 8)
    public static final RegistryObject<Item> PHIAL = ITEMS.register("phial", PhialItem::new);
    public static final RegistryObject<Item> FILTER = simple("filter");
    public static final RegistryObject<Item> LABEL = ITEMS.register("label", thaumcraft.common.items.LabelItem::new);
    public static final RegistryObject<Item> TUBE = block(ModBlocks.TUBE);
    public static final RegistryObject<Item> TUBE_VALVE = block(ModBlocks.TUBE_VALVE);
    public static final RegistryObject<Item> TUBE_FILTER = block(ModBlocks.TUBE_FILTER);
    public static final RegistryObject<Item> TUBE_RESTRICT = block(ModBlocks.TUBE_RESTRICT);
    public static final RegistryObject<Item> TUBE_ONEWAY = block(ModBlocks.TUBE_ONEWAY);
    public static final RegistryObject<Item> TUBE_BUFFER = block(ModBlocks.TUBE_BUFFER);
    public static final RegistryObject<Item> CENTRIFUGE = block(ModBlocks.CENTRIFUGE);
    public static final RegistryObject<Item> ALCHEMICAL_CONSTRUCT = block(ModBlocks.ALCHEMICAL_CONSTRUCT);
    public static final RegistryObject<Item> BELLOWS = block(ModBlocks.BELLOWS);
    public static final RegistryObject<Item> CRYSTALLIZER = block(ModBlocks.CRYSTALLIZER);
    public static final RegistryObject<Item> CRYSTAL_ESSENCE = ITEMS.register("crystal_essence",
            thaumcraft.common.items.CrystalEssenceItem::new);
    // Инфузия (в оригинале ItemResource мета 14)
    public static final RegistryObject<Item> SALIS_MUNDUS = simple("salis_mundus");
    public static final RegistryObject<Item> ARCANE_STONE_BRICKS = block(ModBlocks.ARCANE_STONE_BRICKS);
    public static final RegistryObject<Item> PEDESTAL = block(ModBlocks.PEDESTAL);
    public static final RegistryObject<Item> INFUSION_MATRIX = block(ModBlocks.INFUSION_MATRIX);
    public static final RegistryObject<Item> ARCANE_STONE = block(ModBlocks.ARCANE_STONE);
    public static final RegistryObject<Item> ALCHEMICAL_FURNACE = block(ModBlocks.ALCHEMICAL_FURNACE);
    public static final RegistryObject<Item> ALEMBIC = block(ModBlocks.ALEMBIC);
    public static final RegistryObject<Item> WARDED_JAR = ITEMS.register("warded_jar",
            () -> new JarBlockItem(ModBlocks.WARDED_JAR.get()));
    public static final RegistryObject<Item> VOID_JAR = ITEMS.register("void_jar", () -> new JarBlockItem(ModBlocks.VOID_JAR.get()));
    public static final RegistryObject<Item> RESONATOR = ITEMS.register("resonator", thaumcraft.common.items.ResonatorItem::new);
    public static final RegistryObject<Item> SANITY_SOAP = ITEMS.register("sanity_soap", thaumcraft.common.items.SanitySoapItem::new);
    public static final RegistryObject<Item> ETHEREAL_BLOOM = block(ModBlocks.ETHEREAL_BLOOM);
    public static final RegistryObject<Item> BATH_SALTS = ITEMS.register("bath_salts", thaumcraft.common.items.BathSaltsItem::new);
    public static final RegistryObject<Item> BUCKET_DEATH = ITEMS.register("bucket_death", thaumcraft.common.items.LiquidDeathBucketItem::new);
    public static final RegistryObject<Item> BUCKET_PURE = ITEMS.register("bucket_pure",
            () -> new net.minecraft.world.item.BucketItem(thaumcraft.common.registry.ModFluids.PURIFYING,
                    new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)
                            .rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> ARCANE_SPA = block(ModBlocks.ARCANE_SPA);
    public static final RegistryObject<Item> DECONSTRUCTION_TABLE = block(ModBlocks.DECONSTRUCTION_TABLE);
    public static final RegistryObject<Item> MNEMONIC_MATRIX = block(ModBlocks.MNEMONIC_MATRIX);
    public static final RegistryObject<Item> TALLOW_BLOCK = block(ModBlocks.TALLOW_BLOCK);

    // Големы (ItemGolemPlacer, ItemGolemCore и ItemGolemUpgrade оригинала)
    public static final java.util.List<RegistryObject<Item>> GOLEMS = java.util.Arrays.stream(thaumcraft.common.entity.golem.GolemType.values())
            .map(type -> ITEMS.register("golem_" + type.id(), () -> (Item) new thaumcraft.common.items.golem.GolemPlacerItem(type))).toList();
    public static final RegistryObject<Item> GOLEM_BELL = ITEMS.register("golem_bell", thaumcraft.common.items.golem.GolemBellItem::new);
    public static final RegistryObject<Item> GOLEM_FETTER = block(ModBlocks.GOLEM_FETTER);
    public static final RegistryObject<Item> HUNGRY_CHEST = block(ModBlocks.HUNGRY_CHEST);
    public static final RegistryObject<Item> ARCANE_LAMP = block(ModBlocks.ARCANE_LAMP);
    public static final RegistryObject<Item> TRAVEL_PAVING = block(ModBlocks.TRAVEL_PAVING);
    public static final RegistryObject<Item> WARDING_PAVING = block(ModBlocks.WARDING_PAVING);
    public static final RegistryObject<Item> GRATE = block(ModBlocks.GRATE);
    public static final RegistryObject<Item> ARCANE_BORE_BASE = ITEMS.register("arcane_bore_base",
            () -> new thaumcraft.common.items.ArcaneBoreItem(ModBlocks.ARCANE_BORE_BASE.get()));
    public static final RegistryObject<Item> ARCANE_BORE = ITEMS.register("arcane_bore",
            () -> new thaumcraft.common.items.ArcaneBoreItem(ModBlocks.ARCANE_BORE.get()));
    public static final RegistryObject<Item> OBSIDIAN_TILE = block(ModBlocks.OBSIDIAN_TILE);
    public static final RegistryObject<Item> OBSIDIAN_TOTEM = block(ModBlocks.OBSIDIAN_TOTEM);
    public static final RegistryObject<Item> CHARGED_OBSIDIAN_TOTEM = block(ModBlocks.CHARGED_OBSIDIAN_TOTEM);
    public static final RegistryObject<Item> LOOT_URN_COMMON = lootBlock(ModBlocks.LOOT_URN_COMMON);
    public static final RegistryObject<Item> LOOT_URN_UNCOMMON = lootBlock(ModBlocks.LOOT_URN_UNCOMMON);
    public static final RegistryObject<Item> LOOT_URN_RARE = lootBlock(ModBlocks.LOOT_URN_RARE);
    public static final RegistryObject<Item> LOOT_CRATE_COMMON = lootBlock(ModBlocks.LOOT_CRATE_COMMON);
    public static final RegistryObject<Item> LOOT_CRATE_UNCOMMON = lootBlock(ModBlocks.LOOT_CRATE_UNCOMMON);
    public static final RegistryObject<Item> LOOT_CRATE_RARE = lootBlock(ModBlocks.LOOT_CRATE_RARE);

    private static RegistryObject<Item> lootBlock(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(),
                () -> new thaumcraft.common.items.LootBlockItem((thaumcraft.common.blocks.LootBlock) block.get()));
    }

    public static final RegistryObject<Item> THAUM_BANNER = ITEMS.register("thaum_banner",
            () -> new thaumcraft.common.items.ThaumBannerItem(ModBlocks.THAUM_BANNER.get()));
    public static final RegistryObject<Item> ADVANCED_ALCHEMICAL_CONSTRUCT = block(ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT);
    public static final RegistryObject<Item> NODE_JAR = ITEMS.register("node_jar",
            () -> new thaumcraft.common.items.NodeJarItem(ModBlocks.NODE_JAR.get()));
    public static final RegistryObject<Item> ESSENTIA_RESERVOIR = ITEMS.register("essentia_reservoir",
            () -> new thaumcraft.common.items.EssentiaReservoirItem(ModBlocks.ESSENTIA_RESERVOIR.get()));
    public static final RegistryObject<Item> FLUX_SCRUBBER = ITEMS.register("flux_scrubber",
            () -> new thaumcraft.common.items.FluxScrubberItem(ModBlocks.FLUX_SCRUBBER.get()));
    public static final RegistryObject<Item> SINISTER_STONE = ITEMS.register("sinister_stone", thaumcraft.common.items.SinisterStoneItem::new);
    public static final RegistryObject<Item> BONE_BOW = ITEMS.register("bone_bow", thaumcraft.common.items.tools.BoneBowItem::new);
    /** Первозданные стрелы: воздух, огонь, вода, земля, порядок, энтропия. */
    public static final java.util.List<RegistryObject<Item>> PRIMAL_ARROWS = java.util.stream.IntStream.range(0, 6)
            .mapToObj(type -> ITEMS.register("primal_arrow_" + thaumcraft.common.entity.PrimalArrow.NAMES[type],
                    () -> (Item) new thaumcraft.common.items.tools.PrimalArrowItem(type))).toList();
    public static final RegistryObject<Item> ARCANE_DOOR = ITEMS.register("arcane_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.ARCANE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_PRESSURE_PLATE = block(ModBlocks.ARCANE_PRESSURE_PLATE);
    public static final RegistryObject<Item> WARDED_GLASS = block(ModBlocks.WARDED_GLASS);
    public static final RegistryObject<Item> KEY_IRON = ITEMS.register("key_iron", () -> new thaumcraft.common.items.KeyItem(0));
    public static final RegistryObject<Item> KEY_GOLD = ITEMS.register("key_gold", () -> new thaumcraft.common.items.KeyItem(1));
    public static final RegistryObject<Item> BRAIN_JAR = ITEMS.register("brain_jar",
            () -> new thaumcraft.common.items.BrainJarItem(ModBlocks.BRAIN_JAR.get()));
    public static final RegistryObject<Item> MIRRORED_GLASS = simple("mirrored_glass");
    public static final RegistryObject<Item> MAGIC_MIRROR = ITEMS.register("magic_mirror",
            () -> new thaumcraft.common.items.MirrorItem(ModBlocks.MAGIC_MIRROR.get()));
    public static final RegistryObject<Item> ESSENTIA_MIRROR = ITEMS.register("essentia_mirror",
            () -> new thaumcraft.common.items.MirrorItem(ModBlocks.ESSENTIA_MIRROR.get()));
    public static final RegistryObject<Item> HAND_MIRROR = ITEMS.register("hand_mirror", thaumcraft.common.items.HandMirrorItem::new);
    public static final RegistryObject<Item> ARCANE_EAR = block(ModBlocks.ARCANE_EAR);
    public static final RegistryObject<Item> LEVITATOR = block(ModBlocks.LEVITATOR);
    public static final RegistryObject<Item> LAMP_GROWTH = block(ModBlocks.LAMP_GROWTH);
    public static final RegistryObject<Item> LAMP_FERTILITY = block(ModBlocks.LAMP_FERTILITY);
    public static final RegistryObject<Item> AMBER_BLOCK = block(ModBlocks.AMBER_BLOCK);
    public static final RegistryObject<Item> AMBER_BRICKS = block(ModBlocks.AMBER_BRICKS);
    public static final RegistryObject<Item> TRAVELING_TRUNK = ITEMS.register("traveling_trunk", thaumcraft.common.items.golem.TrunkSpawnerItem::new);
    public static final java.util.List<RegistryObject<Item>> GOLEM_DECORATIONS = java.util.stream.IntStream
            .range(0, thaumcraft.common.items.golem.GolemDecorationItem.NAMES.length)
            .mapToObj(index -> ITEMS.register("golem_deco_" + thaumcraft.common.items.golem.GolemDecorationItem.NAMES[index],
                    () -> (Item) new thaumcraft.common.items.golem.GolemDecorationItem(index))).toList();
    public static final RegistryObject<Item> GOLEM_CORE_BLANK = ITEMS.register("golem_core_blank",
            () -> new thaumcraft.common.items.golem.GolemCoreItem(thaumcraft.common.items.golem.GolemCoreItem.BLANK));
    public static final java.util.List<RegistryObject<Item>> GOLEM_CORES = java.util.stream.IntStream
            .range(0, thaumcraft.common.items.golem.GolemCoreItem.NAMES.length)
            .mapToObj(core -> ITEMS.register("golem_core_" + thaumcraft.common.items.golem.GolemCoreItem.NAMES[core],
                    () -> (Item) new thaumcraft.common.items.golem.GolemCoreItem(core))).toList();
    public static final java.util.List<RegistryObject<Item>> GOLEM_UPGRADES = java.util.stream.IntStream
            .range(0, thaumcraft.common.items.golem.GolemUpgradeItem.NAMES.length)
            .mapToObj(upgrade -> ITEMS.register("golem_upgrade_" + thaumcraft.common.items.golem.GolemUpgradeItem.NAMES[upgrade],
                    () -> (Item) new thaumcraft.common.items.golem.GolemUpgradeItem(upgrade))).toList();
    public static final RegistryObject<Item> FLUX_GAS = block(ModBlocks.FLUX_GAS);
    public static final RegistryObject<Item> THAUMIUM_BLOCK = block(ModBlocks.THAUMIUM_BLOCK);

    // Тауминиевые инструменты и броня. Урон и скорость заданы как у железных в 1.20:
    // прибавка материала к урону у тауминия та же, что у железа (+2).
    public static final RegistryObject<Item> THAUMIUM_SWORD = ITEMS.register("thaumium_sword",
            () -> new SwordItem(ModMaterials.THAUMIUM_TOOL, 3, -2.4f, new Item.Properties()));
    public static final RegistryObject<Item> THAUMIUM_SHOVEL = ITEMS.register("thaumium_shovel",
            () -> new ShovelItem(ModMaterials.THAUMIUM_TOOL, 1.5f, -3f, new Item.Properties()));
    public static final RegistryObject<Item> THAUMIUM_PICKAXE = ITEMS.register("thaumium_pickaxe",
            () -> new PickaxeItem(ModMaterials.THAUMIUM_TOOL, 1, -2.8f, new Item.Properties()));
    public static final RegistryObject<Item> THAUMIUM_AXE = ITEMS.register("thaumium_axe",
            () -> new AxeItem(ModMaterials.THAUMIUM_TOOL, 6f, -3.1f, new Item.Properties()));
    public static final RegistryObject<Item> THAUMIUM_HOE = ITEMS.register("thaumium_hoe",
            () -> new HoeItem(ModMaterials.THAUMIUM_TOOL, -2, -1f, new Item.Properties()));
    // Стихийные инструменты (ItemElemental* оригинала); числа урона и скорости — как у алмазных в 1.20
    public static final RegistryObject<Item> ELEMENTAL_SWORD = ITEMS.register("elemental_sword",
            thaumcraft.common.items.tools.ElementalSwordItem::new);
    public static final RegistryObject<Item> ELEMENTAL_SHOVEL = ITEMS.register("elemental_shovel",
            thaumcraft.common.items.tools.ElementalShovelItem::new);
    public static final RegistryObject<Item> ELEMENTAL_PICKAXE = ITEMS.register("elemental_pickaxe",
            thaumcraft.common.items.tools.ElementalPickaxeItem::new);
    public static final RegistryObject<Item> ELEMENTAL_AXE = ITEMS.register("elemental_axe",
            thaumcraft.common.items.tools.ElementalAxeItem::new);
    public static final RegistryObject<Item> ELEMENTAL_HOE = ITEMS.register("elemental_hoe",
            thaumcraft.common.items.tools.ElementalHoeItem::new);
    public static final RegistryObject<Item> THAUMIUM_HELMET = armor("thaumium_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> THAUMIUM_CHESTPLATE = armor("thaumium_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> THAUMIUM_LEGGINGS = armor("thaumium_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> THAUMIUM_BOOTS = armor("thaumium_boots", ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> TRAVELLER_BOOTS = ITEMS.register("traveller_boots",
            thaumcraft.common.items.armor.TravellerBootsItem::new);
    public static final RegistryObject<Item> HOVER_HARNESS = ITEMS.register("hover_harness",
            thaumcraft.common.items.armor.HoverHarnessItem::new);
    public static final RegistryObject<Item> HOVER_GIRDLE = ITEMS.register("hover_girdle",
            thaumcraft.common.items.armor.HoverGirdleItem::new);
    public static final RegistryObject<Item> ROBE_CHESTPLATE = ITEMS.register("robe_chestplate",
            () -> new thaumcraft.common.items.armor.RobeArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> ROBE_LEGGINGS = ITEMS.register("robe_leggings",
            () -> new thaumcraft.common.items.armor.RobeArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> ROBE_BOOTS = ITEMS.register("robe_boots",
            () -> new thaumcraft.common.items.armor.RobeArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> FORTRESS_HELMET = ITEMS.register("fortress_helmet",
            () -> new thaumcraft.common.items.armor.FortressArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> FORTRESS_CHESTPLATE = ITEMS.register("fortress_chestplate",
            () -> new thaumcraft.common.items.armor.FortressArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> FORTRESS_LEGGINGS = ITEMS.register("fortress_leggings",
            () -> new thaumcraft.common.items.armor.FortressArmorItem(ArmorItem.Type.LEGGINGS));

    // Блоки
    public static final RegistryObject<Item> CINNABAR_ORE = block(ModBlocks.CINNABAR_ORE);
    public static final RegistryObject<Item> INFUSED_STONE_AIR = block(ModBlocks.INFUSED_STONE_AIR);
    public static final RegistryObject<Item> INFUSED_STONE_FIRE = block(ModBlocks.INFUSED_STONE_FIRE);
    public static final RegistryObject<Item> INFUSED_STONE_WATER = block(ModBlocks.INFUSED_STONE_WATER);
    public static final RegistryObject<Item> INFUSED_STONE_EARTH = block(ModBlocks.INFUSED_STONE_EARTH);
    public static final RegistryObject<Item> INFUSED_STONE_ORDER = block(ModBlocks.INFUSED_STONE_ORDER);
    public static final RegistryObject<Item> INFUSED_STONE_ENTROPY = block(ModBlocks.INFUSED_STONE_ENTROPY);
    public static final RegistryObject<Item> AMBER_ORE = block(ModBlocks.AMBER_ORE);
    public static final RegistryObject<Item> DEEPSLATE_CINNABAR_ORE = block(ModBlocks.DEEPSLATE_CINNABAR_ORE);
    public static final RegistryObject<Item> DEEPSLATE_INFUSED_STONE_AIR = block(ModBlocks.DEEPSLATE_INFUSED_STONE_AIR);
    public static final RegistryObject<Item> DEEPSLATE_INFUSED_STONE_FIRE = block(ModBlocks.DEEPSLATE_INFUSED_STONE_FIRE);
    public static final RegistryObject<Item> DEEPSLATE_INFUSED_STONE_WATER = block(ModBlocks.DEEPSLATE_INFUSED_STONE_WATER);
    public static final RegistryObject<Item> DEEPSLATE_INFUSED_STONE_EARTH = block(ModBlocks.DEEPSLATE_INFUSED_STONE_EARTH);
    public static final RegistryObject<Item> DEEPSLATE_INFUSED_STONE_ORDER = block(ModBlocks.DEEPSLATE_INFUSED_STONE_ORDER);
    public static final RegistryObject<Item> DEEPSLATE_INFUSED_STONE_ENTROPY = block(ModBlocks.DEEPSLATE_INFUSED_STONE_ENTROPY);
    public static final RegistryObject<Item> DEEPSLATE_AMBER_ORE = block(ModBlocks.DEEPSLATE_AMBER_ORE);

    public static final RegistryObject<Item> TABLE = block(ModBlocks.TABLE);
    public static final RegistryObject<Item> GREATWOOD_LOG = block(ModBlocks.GREATWOOD_LOG);
    public static final RegistryObject<Item> SILVERWOOD_LOG = block(ModBlocks.SILVERWOOD_LOG);
    public static final RegistryObject<Item> GREATWOOD_PLANKS = block(ModBlocks.GREATWOOD_PLANKS);
    public static final RegistryObject<Item> SILVERWOOD_PLANKS = block(ModBlocks.SILVERWOOD_PLANKS);
    public static final RegistryObject<Item> GREATWOOD_LEAVES = block(ModBlocks.GREATWOOD_LEAVES);
    public static final RegistryObject<Item> SILVERWOOD_LEAVES = block(ModBlocks.SILVERWOOD_LEAVES);
    public static final RegistryObject<Item> GREATWOOD_SAPLING = block(ModBlocks.GREATWOOD_SAPLING);
    public static final RegistryObject<Item> SILVERWOOD_SAPLING = block(ModBlocks.SILVERWOOD_SAPLING);
    public static final RegistryObject<Item> SHIMMERLEAF = block(ModBlocks.SHIMMERLEAF);
    public static final RegistryObject<Item> CINDERPEARL = block(ModBlocks.CINDERPEARL);
    public static final RegistryObject<Item> VISHROOM = block(ModBlocks.VISHROOM);
    public static final java.util.List<RegistryObject<Item>> TALLOW_CANDLES = ModBlocks.TALLOW_CANDLES.stream().map(ModItems::block).toList();
    public static final RegistryObject<Item> WISP_ESSENCE = ITEMS.register("wisp_essence", thaumcraft.common.items.WispEssenceItem::new);
    public static final RegistryObject<Item> MANA_BEAN = ITEMS.register("mana_bean", thaumcraft.common.items.ManaBeanItem::new);
    /** Проверка рассудка (ItemSanityChecker): в руке показывает шкалу искажения. */
    public static final RegistryObject<Item> SANITY_CHECKER = ITEMS.register("sanity_checker",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    // пустотный металл: семя пустоты, слиток, самородок, инструменты и броня
    public static final RegistryObject<Item> VOID_SEED = ITEMS.register("void_seed", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VOID_METAL_INGOT = ITEMS.register("void_metal_ingot", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VOID_METAL_NUGGET = ITEMS.register("void_metal_nugget", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VOID_SWORD = ITEMS.register("void_sword", thaumcraft.common.items.tools.VoidSwordItem::new);
    public static final RegistryObject<Item> VOID_SHOVEL = ITEMS.register("void_shovel", thaumcraft.common.items.tools.VoidShovelItem::new);
    public static final RegistryObject<Item> VOID_PICKAXE = ITEMS.register("void_pickaxe", thaumcraft.common.items.tools.VoidPickaxeItem::new);
    public static final RegistryObject<Item> VOID_AXE = ITEMS.register("void_axe", thaumcraft.common.items.tools.VoidAxeItem::new);
    public static final RegistryObject<Item> VOID_HOE = ITEMS.register("void_hoe", thaumcraft.common.items.tools.VoidHoeItem::new);
    public static final RegistryObject<Item> VOID_HELMET = ITEMS.register("void_helmet",
            () -> new thaumcraft.common.items.armor.VoidArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> VOID_CHESTPLATE = ITEMS.register("void_chestplate",
            () -> new thaumcraft.common.items.armor.VoidArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> VOID_LEGGINGS = ITEMS.register("void_leggings",
            () -> new thaumcraft.common.items.armor.VoidArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> VOID_BOOTS = ITEMS.register("void_boots",
            () -> new thaumcraft.common.items.armor.VoidArmorItem(ArmorItem.Type.BOOTS));
    // предметы Элдрича
    public static final RegistryObject<Item> ELDRITCH_EYE = ITEMS.register("eldritch_eye",
            () -> new thaumcraft.common.items.EldritchObjectItem(thaumcraft.common.items.EldritchObjectItem.Kind.EYE));
    public static final RegistryObject<Item> CRIMSON_RITES = ITEMS.register("crimson_rites",
            () -> new thaumcraft.common.items.EldritchObjectItem(thaumcraft.common.items.EldritchObjectItem.Kind.RITES));
    public static final RegistryObject<Item> RUNED_TABLET = ITEMS.register("runed_tablet",
            () -> new thaumcraft.common.items.EldritchObjectItem(thaumcraft.common.items.EldritchObjectItem.Kind.TABLET));
    public static final RegistryObject<Item> OBELISK_PLACER = ITEMS.register("obelisk_placer",
            () -> new thaumcraft.common.items.EldritchObjectItem(thaumcraft.common.items.EldritchObjectItem.Kind.PLACER));
    public static final RegistryObject<Item> PRIMORDIAL_PEARL = ITEMS.register("primordial_pearl", thaumcraft.common.items.PrimordialPearlItem::new);
    // облачение Багрового культа и багровый клинок
    public static final RegistryObject<Item> CULTIST_ROBE_HELMET = ITEMS.register("cultist_robe_helmet",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.ROBE, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> CULTIST_ROBE_CHESTPLATE = ITEMS.register("cultist_robe_chestplate",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.ROBE, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> CULTIST_ROBE_LEGGINGS = ITEMS.register("cultist_robe_leggings",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.ROBE, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> CULTIST_PLATE_HELMET = ITEMS.register("cultist_plate_helmet",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.PLATE, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> CULTIST_PLATE_CHESTPLATE = ITEMS.register("cultist_plate_chestplate",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.PLATE, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> CULTIST_PLATE_LEGGINGS = ITEMS.register("cultist_plate_leggings",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.PLATE, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> CULTIST_LEADER_HELMET = ITEMS.register("cultist_leader_helmet",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.LEADER, ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> CULTIST_LEADER_CHESTPLATE = ITEMS.register("cultist_leader_chestplate",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.LEADER, ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> CULTIST_LEADER_LEGGINGS = ITEMS.register("cultist_leader_leggings",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.LEADER, ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> CULTIST_BOOTS = ITEMS.register("cultist_boots",
            () -> new thaumcraft.common.items.armor.CultistArmorItem(thaumcraft.common.items.armor.CultistArmorItem.Kind.BOOTS, ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> CRIMSON_BLADE = ITEMS.register("crimson_blade", thaumcraft.common.items.tools.CrimsonBladeItem::new);
    public static final RegistryObject<Item> PRIMAL_CRUSHER = ITEMS.register("primal_crusher", thaumcraft.common.items.tools.PrimalCrusherItem::new);
    public static final RegistryObject<Item> VOID_ROBE_HELMET = ITEMS.register("void_robe_helmet",
            () -> new thaumcraft.common.items.armor.VoidRobeArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> VOID_ROBE_CHESTPLATE = ITEMS.register("void_robe_chestplate",
            () -> new thaumcraft.common.items.armor.VoidRobeArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> VOID_ROBE_LEGGINGS = ITEMS.register("void_robe_leggings",
            () -> new thaumcraft.common.items.armor.VoidRobeArmorItem(ArmorItem.Type.LEGGINGS));
    // мясные кусочки — бонус адской печи — и лакомство из трёх разных кусочков с сахаром
    public static final RegistryObject<Item> NUGGET_CHICKEN = ITEMS.register("nugget_chicken", thaumcraft.common.items.MeatNuggetItem::new);
    public static final RegistryObject<Item> NUGGET_BEEF = ITEMS.register("nugget_beef", thaumcraft.common.items.MeatNuggetItem::new);
    public static final RegistryObject<Item> NUGGET_PORK = ITEMS.register("nugget_pork", thaumcraft.common.items.MeatNuggetItem::new);
    public static final RegistryObject<Item> NUGGET_FISH = ITEMS.register("nugget_fish", thaumcraft.common.items.MeatNuggetItem::new);
    public static final RegistryObject<Item> TRIPLE_MEAT_TREAT = ITEMS.register("triple_meat_treat", () -> new Item(new Item.Properties()
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationMod(0.8f).meat().alwaysEat()
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 0), 0.66f)
                    .build())));

    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModMaterials.THAUMIUM_ARMOR, type, new Item.Properties()));
    }

    public static final RegistryObject<Item> FOCAL_MANIPULATOR = block(ModBlocks.FOCAL_MANIPULATOR);
    // яйца призыва (ItemSpawnerEgg оригинала), с его цветами
    public static final RegistryObject<Item> BRAINY_ZOMBIE_SPAWN_EGG = spawnEgg("brainy_zombie", ModEntities.BRAINY_ZOMBIE, 16761087, 32768);
    public static final RegistryObject<Item> GIANT_BRAINY_ZOMBIE_SPAWN_EGG = spawnEgg("giant_brainy_zombie", ModEntities.GIANT_BRAINY_ZOMBIE, 16761087, 16384);
    public static final RegistryObject<Item> WISP_SPAWN_EGG = spawnEgg("wisp", ModEntities.WISP, 16761087, 16777215);
    public static final RegistryObject<Item> FIRE_BAT_SPAWN_EGG = spawnEgg("fire_bat", ModEntities.FIRE_BAT, 16761087, 12582912);
    public static final RegistryObject<Item> PECH_SPAWN_EGG = spawnEgg("pech", ModEntities.PECH, 16761087, 4194368);
    public static final RegistryObject<Item> MIND_SPIDER_SPAWN_EGG = spawnEgg("mind_spider", ModEntities.MIND_SPIDER, 11184810, 4210752);
    public static final RegistryObject<Item> ELDRITCH_GUARDIAN_SPAWN_EGG = spawnEgg("eldritch_guardian", ModEntities.ELDRITCH_GUARDIAN, 2236962, 4210752);
    public static final RegistryObject<Item> ELDRITCH_WARDEN_SPAWN_EGG = spawnEgg("eldritch_warden", ModEntities.ELDRITCH_WARDEN, 5579298, 4210752);
    public static final RegistryObject<Item> CULTIST_KNIGHT_SPAWN_EGG = spawnEgg("cultist_knight", ModEntities.CULTIST_KNIGHT, 16732245, 128);
    public static final RegistryObject<Item> CULTIST_CLERIC_SPAWN_EGG = spawnEgg("cultist_cleric", ModEntities.CULTIST_CLERIC, 16732245, 8388608);
    public static final RegistryObject<Item> CULTIST_LEADER_SPAWN_EGG = spawnEgg("cultist_leader", ModEntities.CULTIST_LEADER, 16732245, 5263440);
    public static final RegistryObject<Item> CULTIST_PORTAL_SPAWN_EGG = spawnEgg("cultist_portal", ModEntities.CULTIST_PORTAL, 16732245, 16732415);
    public static final RegistryObject<Item> ELDRITCH_GOLEM_SPAWN_EGG = spawnEgg("eldritch_golem", ModEntities.ELDRITCH_GOLEM, 5592405, 4210752);
    public static final RegistryObject<Item> ELDRITCH_CRAB_SPAWN_EGG = spawnEgg("eldritch_crab", ModEntities.ELDRITCH_CRAB, 5592405, 5570560);
    public static final RegistryObject<Item> INHABITED_ZOMBIE_SPAWN_EGG = spawnEgg("inhabited_zombie", ModEntities.INHABITED_ZOMBIE, 5601109, 5570560);
    public static final RegistryObject<Item> THAUMIC_SLIME_SPAWN_EGG = spawnEgg("thaumic_slime", ModEntities.THAUMIC_SLIME, 16761087, 16744703);
    public static final RegistryObject<Item> TAINT_SPIDER_SPAWN_EGG = spawnEgg("taint_spider", ModEntities.TAINT_SPIDER, 16761087, 4210752);
    public static final RegistryObject<Item> TAINTACLE_SPAWN_EGG = spawnEgg("taintacle", ModEntities.TAINTACLE, 16761087, 8388736);
    public static final RegistryObject<Item> TAINTACLE_SMALL_SPAWN_EGG = spawnEgg("taintacle_small", ModEntities.TAINTACLE_SMALL, 16761087, 8388752);
    public static final RegistryObject<Item> TAINT_SPORE_SPAWN_EGG = spawnEgg("taint_spore", ModEntities.TAINT_SPORE, 16761087, 8388720);
    public static final RegistryObject<Item> TAINT_SPORE_SWARMER_SPAWN_EGG = spawnEgg("taint_spore_swarmer", ModEntities.TAINT_SPORE_SWARMER, 16761087, 8388704);
    public static final RegistryObject<Item> TAINT_SWARM_SPAWN_EGG = spawnEgg("taint_swarm", ModEntities.TAINT_SWARM, 16761087, 8388688);
    public static final RegistryObject<Item> TAINTED_CHICKEN_SPAWN_EGG = spawnEgg("tainted_chicken", ModEntities.TAINTED_CHICKEN, 16761087, 12632256);
    public static final RegistryObject<Item> TAINTED_COW_SPAWN_EGG = spawnEgg("tainted_cow", ModEntities.TAINTED_COW, 16761087, 8272443);
    public static final RegistryObject<Item> TAINTED_CREEPER_SPAWN_EGG = spawnEgg("tainted_creeper", ModEntities.TAINTED_CREEPER, 16761087, 65280);
    public static final RegistryObject<Item> TAINTED_PIG_SPAWN_EGG = spawnEgg("tainted_pig", ModEntities.TAINTED_PIG, 16761087, 15702511);
    public static final RegistryObject<Item> TAINTED_SHEEP_SPAWN_EGG = spawnEgg("tainted_sheep", ModEntities.TAINTED_SHEEP, 16761087, 8421504);
    public static final RegistryObject<Item> TAINTED_VILLAGER_SPAWN_EGG = spawnEgg("tainted_villager", ModEntities.TAINTED_VILLAGER, 16761087, 65535);
    public static final RegistryObject<Item> TAINTACLE_GIANT_SPAWN_EGG = spawnEgg("taintacle_giant", ModEntities.TAINTACLE_GIANT, 16761087, 8421504);

    @SuppressWarnings("unchecked")
    private static RegistryObject<Item> spawnEgg(String name, RegistryObject<? extends net.minecraft.world.entity.EntityType<?>> type, int background,
                                                 int spots) {
        return ITEMS.register(name + "_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                () -> (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>) type.get(), background, spots, new Item.Properties()));
    }

    public static final RegistryObject<Item> ARCANE_STONE_SLAB = block(ModBlocks.ARCANE_STONE_SLAB);
    // камни Запредельных земель
    public static final RegistryObject<Item> ANCIENT_STONE = block(ModBlocks.ANCIENT_STONE);
    public static final RegistryObject<Item> ANCIENT_ROCK = block(ModBlocks.ANCIENT_ROCK);
    public static final RegistryObject<Item> ANCIENT_STONE_SEALED = block(ModBlocks.ANCIENT_STONE_SEALED);
    public static final RegistryObject<Item> CRUSTED_STONE = block(ModBlocks.CRUSTED_STONE);
    public static final RegistryObject<Item> ANCIENT_PEDESTAL = block(ModBlocks.ANCIENT_PEDESTAL);
    public static final RegistryObject<Item> ANCIENT_STONE_STAIRS = block(ModBlocks.ANCIENT_STONE_STAIRS);
    public static final RegistryObject<Item> ANCIENT_STONE_SLAB = block(ModBlocks.ANCIENT_STONE_SLAB);
    public static final RegistryObject<Item> GLOWING_CRUSTED_STONE = block(ModBlocks.GLOWING_CRUSTED_STONE);
    public static final RegistryObject<Item> GLYPHED_STONE = block(ModBlocks.GLYPHED_STONE);
    public static final RegistryObject<Item> ANCIENT_DOORWAY = block(ModBlocks.ANCIENT_DOORWAY);
    public static final RegistryObject<Item> CRUSTED_OPENING = block(ModBlocks.CRUSTED_OPENING);
    public static final RegistryObject<Item> RUNED_STONE = block(ModBlocks.RUNED_STONE);
    public static final RegistryObject<Item> ANCIENT_LOCK = block(ModBlocks.ANCIENT_LOCK);
    public static final RegistryObject<Item> AURA_NODE = ITEMS.register("aura_node",
            () -> new thaumcraft.common.items.AuraNodeItem(ModBlocks.AURA_NODE.get()));
    public static final RegistryObject<Item> STRANGE_CRYSTALS = block(ModBlocks.STRANGE_CRYSTALS);
    public static final RegistryObject<Item> ARCANE_STONE_STAIRS = block(ModBlocks.ARCANE_STONE_STAIRS);
    public static final RegistryObject<Item> GREATWOOD_STAIRS = block(ModBlocks.GREATWOOD_STAIRS);
    public static final RegistryObject<Item> SILVERWOOD_STAIRS = block(ModBlocks.SILVERWOOD_STAIRS);
    public static final RegistryObject<Item> GREATWOOD_SLAB = block(ModBlocks.GREATWOOD_SLAB);
    public static final RegistryObject<Item> SILVERWOOD_SLAB = block(ModBlocks.SILVERWOOD_SLAB);
    public static final RegistryObject<Item> NODE_STABILIZER = block(ModBlocks.NODE_STABILIZER);
    public static final RegistryObject<Item> ADVANCED_NODE_STABILIZER = block(ModBlocks.ADVANCED_NODE_STABILIZER);
    public static final RegistryObject<Item> NODE_TRANSDUCER = block(ModBlocks.NODE_TRANSDUCER);
    public static final RegistryObject<Item> VIS_RELAY = block(ModBlocks.VIS_RELAY);
    public static final RegistryObject<Item> GOGGLES = ITEMS.register("goggles", thaumcraft.common.items.armor.GogglesItem::new);
    public static final RegistryObject<Item> CRYSTAL_AIR = ITEMS.register("crystal_air",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_AIR.get()));
    public static final RegistryObject<Item> CRYSTAL_FIRE = ITEMS.register("crystal_fire",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_FIRE.get()));
    public static final RegistryObject<Item> CRYSTAL_WATER = ITEMS.register("crystal_water",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_WATER.get()));
    public static final RegistryObject<Item> CRYSTAL_EARTH = ITEMS.register("crystal_earth",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_EARTH.get()));
    public static final RegistryObject<Item> CRYSTAL_ORDER = ITEMS.register("crystal_order",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_ORDER.get()));
    public static final RegistryObject<Item> CRYSTAL_ENTROPY = ITEMS.register("crystal_entropy",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_ENTROPY.get()));
    public static final RegistryObject<Item> CRYSTAL_MIXED = ITEMS.register("crystal_mixed",
            () -> new thaumcraft.common.items.CrystalBlockItem(ModBlocks.CRYSTAL_MIXED.get()));
    public static final RegistryObject<Item> WAND_PEDESTAL = block(ModBlocks.WAND_PEDESTAL);
    public static final RegistryObject<Item> TAINT_GOO = ITEMS.register("taint_goo", thaumcraft.common.items.TaintItem::new);
    public static final RegistryObject<Item> ZOMBIE_BRAIN = ITEMS.register("zombie_brain", thaumcraft.common.items.ZombieBrainItem::new);
    public static final RegistryObject<Item> BOTTLE_TAINT = ITEMS.register("bottle_taint", thaumcraft.common.items.BottleTaintItem::new);
    public static final RegistryObject<Item> TAINT_TENDRIL = ITEMS.register("taint_tendril", thaumcraft.common.items.TaintItem::new);
    public static final RegistryObject<Item> TAINT_CRUST = block(ModBlocks.TAINT_CRUST);
    public static final RegistryObject<Item> TAINTED_SOIL = block(ModBlocks.TAINTED_SOIL);
    public static final RegistryObject<Item> FLESH_BLOCK = block(ModBlocks.FLESH_BLOCK);
    public static final RegistryObject<Item> TAINT_FIBRES = block(ModBlocks.TAINT_FIBRES);
    public static final RegistryObject<Item> TAINT_GRASS = block(ModBlocks.TAINT_GRASS);
    public static final RegistryObject<Item> TAINT_PLANT = block(ModBlocks.TAINT_PLANT);
    public static final RegistryObject<Item> SPORE_STALK = block(ModBlocks.SPORE_STALK);
    // золотая монета и три мешка с добычей, выпадающие из особых мобов
    public static final RegistryObject<Item> COIN = ITEMS.register("coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENCHANTED_FABRIC = ITEMS.register("enchanted_fabric", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> APPRENTICE_RING_AIR = ITEMS.register("apprentice_ring_air",
            () -> new thaumcraft.common.items.ApprenticeRingItem("aer"));
    public static final RegistryObject<Item> APPRENTICE_RING_FIRE = ITEMS.register("apprentice_ring_fire",
            () -> new thaumcraft.common.items.ApprenticeRingItem("ignis"));
    public static final RegistryObject<Item> APPRENTICE_RING_WATER = ITEMS.register("apprentice_ring_water",
            () -> new thaumcraft.common.items.ApprenticeRingItem("aqua"));
    public static final RegistryObject<Item> APPRENTICE_RING_EARTH = ITEMS.register("apprentice_ring_earth",
            () -> new thaumcraft.common.items.ApprenticeRingItem("terra"));
    public static final RegistryObject<Item> APPRENTICE_RING_ORDER = ITEMS.register("apprentice_ring_order",
            () -> new thaumcraft.common.items.ApprenticeRingItem("ordo"));
    public static final RegistryObject<Item> APPRENTICE_RING_ENTROPY = ITEMS.register("apprentice_ring_entropy",
            () -> new thaumcraft.common.items.ApprenticeRingItem("perditio"));
    public static final RegistryObject<Item> RUNIC_RING_LESSER = ITEMS.register("runic_ring_lesser", () -> new thaumcraft.common.items.RunicBaubleItem(
            1, thaumcraft.common.items.RunicBaubleItem.Kind.PLAIN, net.minecraft.world.item.Rarity.UNCOMMON));
    public static final RegistryObject<Item> RUNIC_RING = ITEMS.register("runic_ring", () -> new thaumcraft.common.items.RunicBaubleItem(
            5, thaumcraft.common.items.RunicBaubleItem.Kind.PLAIN, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> RUNIC_RING_CHARGED = ITEMS.register("runic_ring_charged", () -> new thaumcraft.common.items.RunicBaubleItem(
            4, thaumcraft.common.items.RunicBaubleItem.Kind.CHARGED, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> RUNIC_RING_HEALING = ITEMS.register("runic_ring_healing", () -> new thaumcraft.common.items.RunicBaubleItem(
            4, thaumcraft.common.items.RunicBaubleItem.Kind.HEALING, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> RUNIC_AMULET = ITEMS.register("runic_amulet", () -> new thaumcraft.common.items.RunicBaubleItem(
            8, thaumcraft.common.items.RunicBaubleItem.Kind.PLAIN, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> RUNIC_AMULET_EMERGENCY = ITEMS.register("runic_amulet_emergency", () -> new thaumcraft.common.items.RunicBaubleItem(
            7, thaumcraft.common.items.RunicBaubleItem.Kind.EMERGENCY, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> RUNIC_GIRDLE = ITEMS.register("runic_girdle", () -> new thaumcraft.common.items.RunicBaubleItem(
            10, thaumcraft.common.items.RunicBaubleItem.Kind.PLAIN, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> RUNIC_GIRDLE_KINETIC = ITEMS.register("runic_girdle_kinetic", () -> new thaumcraft.common.items.RunicBaubleItem(
            9, thaumcraft.common.items.RunicBaubleItem.Kind.KINETIC, net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> KNOWLEDGE_FRAGMENT = ITEMS.register("knowledge_fragment",
            thaumcraft.common.items.KnowledgeFragmentItem::new);
    public static final RegistryObject<Item> LOOT_BAG_COMMON = ITEMS.register("loot_bag_common", () -> new thaumcraft.common.items.LootBagItem(0));
    public static final RegistryObject<Item> LOOT_BAG_UNCOMMON = ITEMS.register("loot_bag_uncommon", () -> new thaumcraft.common.items.LootBagItem(1));
    public static final RegistryObject<Item> LOOT_BAG_RARE = ITEMS.register("loot_bag_rare", () -> new thaumcraft.common.items.LootBagItem(2));
    public static final RegistryObject<Item> VIS_CHARGE_RELAY = block(ModBlocks.VIS_CHARGE_RELAY);
    public static final RegistryObject<Item> WAND_PEDESTAL_FOCUS = block(ModBlocks.WAND_PEDESTAL_FOCUS);
    // камень вис вмещает 25 единиц каждой стихии, амулет хранения — 250 (в сотых долях, как у жезла)
    public static final RegistryObject<Item> VIS_STONE = ITEMS.register("vis_stone", () -> new thaumcraft.common.items.VisAmuletItem(2500, true));
    public static final RegistryObject<Item> VIS_AMULET = ITEMS.register("vis_amulet", () -> new thaumcraft.common.items.VisAmuletItem(25000, false));

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private ModItems() {
    }
}

"""Строит data/thaumcraft/thaumcraft_research/research.json из ConfigResearch.java оригинала.

Запуск из корня проекта:  python tools/gen_research.py

Дерево исследований (ключи, категории, положение на карте, связи, цена в аспектах, флаги и страницы) берётся
из оригинала автоматически. Вручную задано только соответствие старых предметов новым id (LEGACY_ITEMS)
и старых имён рецептов новым id рецептов (RECIPES). Чего в порте ещё нет, остаётся в файле с пометкой
"legacy" и в игре показывается заглушкой — дерево от этого не ломается.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, 'reference', 'tc4', 'src', 'thaumcraft', 'common', 'config', 'ConfigResearch.java')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'thaumcraft', 'thaumcraft_research', 'research.json')

LATIN = {
    'AIR': 'aer', 'EARTH': 'terra', 'FIRE': 'ignis', 'WATER': 'aqua', 'ORDER': 'ordo', 'ENTROPY': 'perditio',
    'VOID': 'vacuos', 'LIGHT': 'lux', 'WEATHER': 'tempestas', 'MOTION': 'motus', 'COLD': 'gelum',
    'CRYSTAL': 'vitreus', 'LIFE': 'victus', 'POISON': 'venenum', 'ENERGY': 'potentia', 'EXCHANGE': 'permutatio',
    'METAL': 'metallum', 'DEATH': 'mortuus', 'FLIGHT': 'volatus', 'DARKNESS': 'tenebrae', 'SOUL': 'spiritus',
    'HEAL': 'sano', 'TRAVEL': 'iter', 'ELDRITCH': 'alienis', 'MAGIC': 'praecantatio', 'AURA': 'auram',
    'TAINT': 'vitium', 'SLIME': 'limus', 'PLANT': 'herba', 'TREE': 'arbor', 'BEAST': 'bestia', 'FLESH': 'corpus',
    'UNDEAD': 'exanimis', 'MIND': 'cognitio', 'SENSES': 'sensus', 'MAN': 'humanus', 'CROP': 'messis',
    'MINE': 'perfodio', 'TOOL': 'instrumentum', 'HARVEST': 'meto', 'WEAPON': 'telum', 'ARMOR': 'tutamen',
    'HUNGER': 'fames', 'GREED': 'lucrum', 'CRAFT': 'fabrico', 'CLOTH': 'pannus', 'MECHANISM': 'machina',
    'TRAP': 'vinculum',
}

# Старое обозначение предмета ("Поле@мета", мета 0 по умолчанию) -> id в порте. Пополняется по мере переноса.
LEGACY_ITEMS = {
    'ConfigBlocks.blockAiry@5': 'thaumcraft:aura_node',
    'ConfigBlocks.blockEldritch@5': 'thaumcraft:glyphed_stone',
    'ConfigBlocks.blockEldritch@10': 'thaumcraft:runed_stone',
    'wand': 'thaumcraft:wand',
    'ConfigItems.itemWandCasting@0': 'thaumcraft:wand',
    'ConfigItems.itemWandCap@0': 'thaumcraft:wand_cap_iron',
    'ConfigItems.itemWandCap@1': 'thaumcraft:wand_cap_gold',
    'ConfigItems.itemWandRod@0': 'thaumcraft:wand_rod_greatwood',
    'ConfigItems.itemWandRod@2': 'thaumcraft:wand_rod_silverwood',
    'ConfigItems.itemWandRod@1': 'thaumcraft:wand_rod_obsidian',
    'ConfigItems.itemWandRod@3': 'thaumcraft:wand_rod_ice',
    'ConfigItems.itemWandRod@4': 'thaumcraft:wand_rod_quartz',
    'ConfigItems.itemWandRod@5': 'thaumcraft:wand_rod_reed',
    'ConfigItems.itemWandRod@6': 'thaumcraft:wand_rod_blaze',
    'ConfigItems.itemWandRod@7': 'thaumcraft:wand_rod_bone',
    'ConfigItems.itemWandCap@2': 'thaumcraft:wand_cap_thaumium',
    'ConfigItems.itemWandCap@7': 'thaumcraft:wand_cap_void',
    'ConfigItems.itemWandCap@8': 'thaumcraft:wand_cap_void_inert',
    'ConfigItems.itemWandCap@3': 'thaumcraft:wand_cap_copper',
    'ConfigItems.itemNugget@1': 'thaumcraft:copper_nugget',
    'ConfigItems.itemResource@15': 'thaumcraft:primal_charm',
    'ConfigItems.itemResource@16': 'thaumcraft:void_metal_ingot',
    'ConfigItems.itemResource@17': 'thaumcraft:void_seed',
    'ConfigItems.itemNugget@7': 'thaumcraft:void_metal_nugget',
    'ConfigItems.itemFocusFire@0': 'thaumcraft:focus_fire',
    'ConfigBlocks.blockStoneDevice@13': 'thaumcraft:focal_manipulator',
    'ConfigBlocks.blockSlabStone@0': 'thaumcraft:arcane_stone_slab',
    'ConfigBlocks.blockStairsArcaneStone@0': 'thaumcraft:arcane_stone_stairs',
    'ConfigBlocks.blockStairsGreatwood@0': 'thaumcraft:greatwood_stairs',
    'ConfigBlocks.blockStairsSilverwood@0': 'thaumcraft:silverwood_stairs',
    'ConfigBlocks.blockSlabWood@0': 'thaumcraft:greatwood_slab',
    'ConfigBlocks.blockSlabWood@1': 'thaumcraft:silverwood_slab',
    'ConfigBlocks.blockStoneDevice@9': 'thaumcraft:node_stabilizer',
    'ConfigBlocks.blockStoneDevice@10': 'thaumcraft:advanced_node_stabilizer',
    'ConfigBlocks.blockStoneDevice@11': 'thaumcraft:node_transducer',
    'ConfigBlocks.blockMetalDevice@14': 'thaumcraft:vis_relay',
    'ConfigItems.itemFocusWarding@0': 'thaumcraft:focus_warding',
    'ConfigItems.itemFocusTrade@0': 'thaumcraft:focus_trade',
    'ConfigItems.itemFocusHellbat@0': 'thaumcraft:focus_hellbat',
    'ConfigItems.itemFocusPortableHole@0': 'thaumcraft:focus_portable_hole',
    'ConfigItems.itemFocusPech@0': 'thaumcraft:focus_pech',
    'ConfigItems.itemFocusPrimal@0': 'thaumcraft:focus_primal',
    'ConfigItems.itemFocusExcavation@0': 'thaumcraft:focus_excavation',
    'ConfigItems.itemFocusPouch@0': 'thaumcraft:focus_pouch',
    'ConfigItems.itemBaubleBlanks@0': 'thaumcraft:bauble_amulet',
    'ConfigItems.itemBaubleBlanks@1': 'thaumcraft:bauble_ring',
    'ConfigItems.itemBaubleBlanks@2': 'thaumcraft:bauble_belt',
    'ConfigItems.itemFocusShock@0': 'thaumcraft:focus_shock',
    'ConfigItems.itemFocusFrost@0': 'thaumcraft:focus_frost',
    'ConfigItems.itemWandRod@50': 'thaumcraft:wand_rod_greatwood_staff',
    'ConfigItems.itemWandRod@51': 'thaumcraft:wand_rod_obsidian_staff',
    'ConfigItems.itemWandRod@52': 'thaumcraft:wand_rod_silverwood_staff',
    'ConfigItems.itemWandRod@53': 'thaumcraft:wand_rod_ice_staff',
    'ConfigItems.itemWandRod@54': 'thaumcraft:wand_rod_quartz_staff',
    'ConfigItems.itemWandRod@55': 'thaumcraft:wand_rod_reed_staff',
    'ConfigItems.itemWandRod@56': 'thaumcraft:wand_rod_blaze_staff',
    'ConfigItems.itemWandRod@57': 'thaumcraft:wand_rod_bone_staff',
    'ConfigItems.itemWandRod@100': 'thaumcraft:wand_rod_primal_staff',
    'ConfigItems.itemWandCap@6': 'thaumcraft:wand_cap_thaumium_inert',
    'ConfigItems.itemResource@14': 'thaumcraft:salis_mundus',
    'ConfigBlocks.blockCosmeticSolid@7': 'thaumcraft:arcane_stone_bricks',
    'ConfigBlocks.blockStoneDevice@1': 'thaumcraft:pedestal',
    'ConfigBlocks.blockStoneDevice@2': 'thaumcraft:infusion_matrix',
    'ConfigItems.itemThaumometer@0': 'thaumcraft:thaumometer',
    'ConfigItems.itemSanityChecker@0': 'thaumcraft:sanity_checker',
    'ConfigItems.itemGoggles@0': 'thaumcraft:goggles',
    'ConfigBlocks.blockCrystal@0': 'thaumcraft:crystal_air',
    'ConfigBlocks.blockCrystal@1': 'thaumcraft:crystal_fire',
    'ConfigBlocks.blockCrystal@2': 'thaumcraft:crystal_water',
    'ConfigBlocks.blockCrystal@3': 'thaumcraft:crystal_earth',
    'ConfigBlocks.blockCrystal@4': 'thaumcraft:crystal_order',
    'ConfigBlocks.blockCrystal@5': 'thaumcraft:crystal_entropy',
    'ConfigBlocks.blockCrystal@6': 'thaumcraft:crystal_mixed',
    'ConfigBlocks.blockStoneDevice@5': 'thaumcraft:wand_pedestal',
    'ConfigItems.itemResource@18': 'thaumcraft:coin',
    'ConfigItems.itemResource@11': 'thaumcraft:taint_goo',
    'ConfigItems.itemResource@12': 'thaumcraft:taint_tendril',
    'ConfigItems.itemResource@7': 'thaumcraft:enchanted_fabric',
    'ConfigItems.itemBaubleBlanks@3': 'thaumcraft:apprentice_ring_air',
    'ConfigItems.itemBaubleBlanks@4': 'thaumcraft:apprentice_ring_fire',
    'ConfigItems.itemBaubleBlanks@5': 'thaumcraft:apprentice_ring_water',
    'ConfigItems.itemBaubleBlanks@6': 'thaumcraft:apprentice_ring_earth',
    'ConfigItems.itemBaubleBlanks@7': 'thaumcraft:apprentice_ring_order',
    'ConfigItems.itemBaubleBlanks@8': 'thaumcraft:apprentice_ring_entropy',
    'ConfigItems.itemRingRunic@0': 'thaumcraft:runic_ring_lesser',
    'ConfigItems.itemRingRunic@1': 'thaumcraft:runic_ring',
    'ConfigItems.itemRingRunic@2': 'thaumcraft:runic_ring_charged',
    'ConfigItems.itemRingRunic@3': 'thaumcraft:runic_ring_healing',
    'ConfigItems.itemAmuletRunic@0': 'thaumcraft:runic_amulet',
    'ConfigItems.itemAmuletRunic@1': 'thaumcraft:runic_amulet_emergency',
    'ConfigItems.itemGirdleRunic@0': 'thaumcraft:runic_girdle',
    'ConfigItems.itemGirdleRunic@1': 'thaumcraft:runic_girdle_kinetic',
    'ConfigItems.itemResource@9': 'thaumcraft:knowledge_fragment',
    'ConfigItems.itemLootbag@0': 'thaumcraft:loot_bag_common',
    'ConfigItems.itemLootbag@1': 'thaumcraft:loot_bag_uncommon',
    'ConfigItems.itemLootbag@2': 'thaumcraft:loot_bag_rare',
    'ConfigBlocks.blockMetalDevice@2': 'thaumcraft:vis_charge_relay',
    'ConfigBlocks.blockStoneDevice@8': 'thaumcraft:wand_pedestal_focus',
    'ConfigItems.itemAmuletVis@0': 'thaumcraft:vis_stone',
    'ConfigItems.itemAmuletVis@1': 'thaumcraft:vis_amulet',
    'ConfigItems.itemThaumonomicon@0': 'thaumcraft:thaumonomicon',
    'ConfigItems.itemInkwell@0': 'thaumcraft:scribing_tools',
    'ConfigBlocks.blockCandle@0': 'thaumcraft:tallow_candle_white',
    'ConfigBlocks.blockCandle@1': 'thaumcraft:tallow_candle_orange',
    'ConfigBlocks.blockCandle@2': 'thaumcraft:tallow_candle_magenta',
    'ConfigBlocks.blockCandle@3': 'thaumcraft:tallow_candle_light_blue',
    'ConfigBlocks.blockCandle@4': 'thaumcraft:tallow_candle_yellow',
    'ConfigBlocks.blockCandle@5': 'thaumcraft:tallow_candle_lime',
    'ConfigBlocks.blockCandle@6': 'thaumcraft:tallow_candle_pink',
    'ConfigBlocks.blockCandle@7': 'thaumcraft:tallow_candle_gray',
    'ConfigBlocks.blockCandle@8': 'thaumcraft:tallow_candle_light_gray',
    'ConfigBlocks.blockCandle@9': 'thaumcraft:tallow_candle_cyan',
    'ConfigBlocks.blockCandle@10': 'thaumcraft:tallow_candle_purple',
    'ConfigBlocks.blockCandle@11': 'thaumcraft:tallow_candle_blue',
    'ConfigBlocks.blockCandle@12': 'thaumcraft:tallow_candle_brown',
    'ConfigBlocks.blockCandle@13': 'thaumcraft:tallow_candle_green',
    'ConfigBlocks.blockCandle@14': 'thaumcraft:tallow_candle_red',
    'ConfigBlocks.blockCandle@15': 'thaumcraft:tallow_candle_black',
    'ConfigItems.itemResearchNotes@0': 'thaumcraft:research_notes',
    'ConfigItems.itemShard@0': 'thaumcraft:shard_air',
    'ConfigItems.itemShard@1': 'thaumcraft:shard_fire',
    'ConfigItems.itemShard@2': 'thaumcraft:shard_water',
    'ConfigItems.itemShard@3': 'thaumcraft:shard_earth',
    'ConfigItems.itemShard@4': 'thaumcraft:shard_order',
    'ConfigItems.itemShard@5': 'thaumcraft:shard_entropy',
    'ConfigItems.itemShard@6': 'thaumcraft:shard_balanced',
    'ConfigItems.itemResource@0': 'thaumcraft:alumentum',
    'ConfigItems.itemResource@1': 'thaumcraft:nitor',
    'ConfigItems.itemResource@2': 'thaumcraft:thaumium_ingot',
    'ConfigItems.itemResource@4': 'thaumcraft:tallow',
    'ConfigItems.itemNugget@0': 'minecraft:iron_nugget',
    'ConfigItems.itemNugget@6': 'thaumcraft:thaumium_nugget',
    'ConfigItems.itemNugget@16': 'thaumcraft:cluster_iron',
    'ConfigItems.itemNugget@21': 'thaumcraft:cluster_cinnabar',
    'ConfigItems.itemNugget@17': 'thaumcraft:cluster_copper',
    'ConfigItems.itemNugget@18': 'thaumcraft:cluster_tin',
    'ConfigItems.itemNugget@19': 'thaumcraft:cluster_silver',
    'ConfigItems.itemNugget@20': 'thaumcraft:cluster_lead',
    'ConfigItems.itemNugget@2': 'thaumcraft:tin_nugget',
    'ConfigItems.itemNugget@3': 'thaumcraft:silver_nugget',
    'ConfigItems.itemNugget@4': 'thaumcraft:lead_nugget',
    'ConfigItems.itemWandCap@4': 'thaumcraft:wand_cap_silver',
    'ConfigItems.itemWandCap@5': 'thaumcraft:wand_cap_silver_inert',
    'ConfigItems.itemNugget@31': 'thaumcraft:cluster_gold',
    'ConfigBlocks.blockMetalDevice@0': 'thaumcraft:crucible',
    'ConfigBlocks.blockMetalDevice@1': 'thaumcraft:alembic',
    'ConfigBlocks.blockStoneDevice@0': 'thaumcraft:alchemical_furnace',
    'ConfigBlocks.blockJar@0': 'thaumcraft:warded_jar',
    'ConfigBlocks.blockJar@3': 'thaumcraft:void_jar',
    'ConfigBlocks.blockStoneDevice@12': 'thaumcraft:arcane_spa',
    'ConfigItems.itemBathSalts@0': 'thaumcraft:bath_salts',
    'ConfigItems.itemBucketDeath@0': 'thaumcraft:bucket_death',
    'ConfigItems.itemBucketPure@0': 'thaumcraft:bucket_pure',
    'ConfigBlocks.blockCustomPlant@4': 'thaumcraft:ethereal_bloom',
    'ConfigItems.itemResonator@0': 'thaumcraft:resonator',
    'ConfigItems.itemSanitySoap@0': 'thaumcraft:sanity_soap',
    'ConfigBlocks.blockCosmeticSolid@6': 'thaumcraft:arcane_stone',
    'ConfigItems.itemEssence@0': 'thaumcraft:phial',
    'ConfigItems.itemBottleTaint@0': 'thaumcraft:bottle_taint',
    'ConfigItems.itemZombieBrain@0': 'thaumcraft:zombie_brain',
    'ConfigItems.itemWispEssence@0': 'thaumcraft:wisp_essence',
    'ConfigItems.itemResource@8': 'thaumcraft:filter',
    'ConfigItems.itemResource@13': 'thaumcraft:label',
    'ConfigBlocks.blockTube@0': 'thaumcraft:tube',
    'ConfigBlocks.blockTube@1': 'thaumcraft:tube_valve',
    'ConfigBlocks.blockTube@3': 'thaumcraft:tube_filter',
    'ConfigBlocks.blockTube@5': 'thaumcraft:tube_restrict',
    'ConfigBlocks.blockTube@6': 'thaumcraft:tube_oneway',
    'ConfigBlocks.blockTube@4': 'thaumcraft:tube_buffer',
    'ConfigBlocks.blockTube@7': 'thaumcraft:crystallizer',
    'ConfigItems.itemCrystalEssence@0': 'thaumcraft:crystal_essence',
    'ConfigBlocks.blockTube@2': 'thaumcraft:centrifuge',
    'ConfigBlocks.blockMetalDevice@9': 'thaumcraft:alchemical_construct',
    'ConfigBlocks.blockMetalDevice@12': 'thaumcraft:mnemonic_matrix',
    'ConfigBlocks.blockCosmeticSolid@5': 'thaumcraft:tallow_block',
    'ConfigItems.itemGolemCore@100': 'thaumcraft:golem_core_blank',
    'ConfigBlocks.blockCosmeticSolid@9': 'thaumcraft:golem_fetter',
    'ConfigBlocks.blockChestHungry@0': 'thaumcraft:hungry_chest',
    'ConfigBlocks.blockMetalDevice@7': 'thaumcraft:arcane_lamp',
    'ConfigBlocks.blockMetalDevice@5': 'thaumcraft:grate',
    'ConfigItems.itemHelmetFortress@0': 'thaumcraft:fortress_helmet',
    'ConfigBlocks.blockWoodenDevice@4': 'thaumcraft:arcane_bore_base',
    'ConfigBlocks.blockWoodenDevice@5': 'thaumcraft:arcane_bore',
    'ConfigBlocks.blockWoodenDevice@8': 'thaumcraft:thaum_banner',
    'is': 'thaumcraft:thaum_banner',
    'ConfigBlocks.blockStoneDevice@14': 'thaumcraft:flux_scrubber',
    'ConfigItems.itemCompassStone@1': 'thaumcraft:sinister_stone',
    'ConfigItems.itemCompassStone@0': 'thaumcraft:sinister_stone',
    'ConfigItems.itemBootsTraveller@0': 'thaumcraft:traveller_boots',
    'ConfigItems.itemHoverHarness@0': 'thaumcraft:hover_harness',
    'ConfigItems.itemGirdleHover@0': 'thaumcraft:hover_girdle',
    'ConfigItems.itemChestRobe@0': 'thaumcraft:robe_chestplate',
    'ConfigBlocks.blockJar@2': 'thaumcraft:node_jar',
    'ConfigItems.itemJarNode@0': 'thaumcraft:node_jar',
    'ConfigBlocks.blockMetalDevice@3': 'thaumcraft:advanced_alchemical_construct',
    'ConfigBlocks.blockEssentiaReservoir@0': 'thaumcraft:essentia_reservoir',
    'ConfigItems.itemEldritchObject@0': 'thaumcraft:eldritch_eye',
    'ConfigItems.itemEldritchObject@1': 'thaumcraft:crimson_rites',
    'ConfigItems.itemEldritchObject@2': 'thaumcraft:runed_tablet',
    'ConfigItems.itemPrimalCrusher@0': 'thaumcraft:primal_crusher',
    'ConfigItems.itemEldritchObject@3': 'thaumcraft:primordial_pearl',
    'ConfigItems.itemHelmetVoidRobe@0': 'thaumcraft:void_robe_helmet',
    'ConfigItems.itemLegsRobe@0': 'thaumcraft:robe_leggings',
    'ConfigItems.itemBootsRobe@0': 'thaumcraft:robe_boots',
    'ConfigItems.itemChestFortress@0': 'thaumcraft:fortress_chestplate',
    'ConfigItems.itemLegsFortress@0': 'thaumcraft:fortress_leggings',
    'ConfigItems.itemBowBone@0': 'thaumcraft:bone_bow',
    'ConfigItems.itemPrimalArrow@32767': 'thaumcraft:primal_arrow_air',
    'ConfigItems.itemArcaneDoor@0': 'thaumcraft:arcane_door',
    'ConfigItems.itemKey@0': 'thaumcraft:key_iron',
    'ConfigItems.itemKey@1': 'thaumcraft:key_gold',
    'ConfigBlocks.blockWoodenDevice@2': 'thaumcraft:arcane_pressure_plate',
    'ConfigBlocks.blockCosmeticOpaque@2': 'thaumcraft:warded_glass',
    'ConfigBlocks.blockJar@1': 'thaumcraft:brain_jar',
    'ConfigBlocks.blockMirror@0': 'thaumcraft:magic_mirror',
    'ConfigBlocks.blockMirror@6': 'thaumcraft:essentia_mirror',
    'ConfigItems.itemHandMirror@0': 'thaumcraft:hand_mirror',
    'ConfigItems.itemResource@10': 'thaumcraft:mirrored_glass',
    'ConfigBlocks.blockWoodenDevice@1': 'thaumcraft:arcane_ear',
    'ConfigBlocks.blockLifter@0': 'thaumcraft:levitator',
    'ConfigBlocks.blockCosmeticSolid@2': 'thaumcraft:travel_paving',
    'ConfigBlocks.blockCosmeticSolid@3': 'thaumcraft:warding_paving',
    'ConfigBlocks.blockMetalDevice@8': 'thaumcraft:lamp_growth',
    'ConfigBlocks.blockMetalDevice@13': 'thaumcraft:lamp_fertility',
    'ConfigBlocks.blockCosmeticOpaque@0': 'thaumcraft:amber_block',
    'ConfigBlocks.blockCosmeticOpaque@1': 'thaumcraft:amber_bricks',
    'ConfigItems.itemTrunkSpawner@0': 'thaumcraft:traveling_trunk',
    'ConfigItems.itemGolemDecoration@0': 'thaumcraft:golem_deco_tophat',
    'ConfigItems.itemGolemDecoration@1': 'thaumcraft:golem_deco_glasses',
    'ConfigItems.itemGolemDecoration@2': 'thaumcraft:golem_deco_bowtie',
    'ConfigItems.itemGolemDecoration@3': 'thaumcraft:golem_deco_fez',
    'ConfigItems.itemGolemDecoration@4': 'thaumcraft:golem_deco_dart',
    'ConfigItems.itemGolemDecoration@5': 'thaumcraft:golem_deco_visor',
    'ConfigItems.itemGolemDecoration@6': 'thaumcraft:golem_deco_armor',
    'ConfigItems.itemGolemDecoration@7': 'thaumcraft:golem_deco_mace',
    'ConfigItems.itemGolemBell@0': 'thaumcraft:golem_bell',
    'ConfigItems.itemGolemPlacer@0': 'thaumcraft:golem_straw',
    'ConfigItems.itemGolemPlacer@1': 'thaumcraft:golem_wood',
    'ConfigItems.itemGolemPlacer@2': 'thaumcraft:golem_tallow',
    'ConfigItems.itemGolemPlacer@3': 'thaumcraft:golem_clay',
    'ConfigItems.itemGolemPlacer@4': 'thaumcraft:golem_flesh',
    'ConfigItems.itemGolemPlacer@5': 'thaumcraft:golem_stone',
    'ConfigItems.itemGolemPlacer@6': 'thaumcraft:golem_iron',
    'ConfigItems.itemGolemPlacer@7': 'thaumcraft:golem_thaumium',
    'ConfigItems.itemGolemCore@0': 'thaumcraft:golem_core_fill',
    'ConfigItems.itemGolemCore@1': 'thaumcraft:golem_core_empty',
    'ConfigItems.itemGolemCore@2': 'thaumcraft:golem_core_gather',
    'ConfigItems.itemGolemCore@3': 'thaumcraft:golem_core_harvest',
    'ConfigItems.itemGolemCore@4': 'thaumcraft:golem_core_guard',
    'ConfigItems.itemGolemCore@5': 'thaumcraft:golem_core_liquid',
    'ConfigItems.itemGolemCore@6': 'thaumcraft:golem_core_alchemy',
    'ConfigItems.itemGolemCore@7': 'thaumcraft:golem_core_lumber',
    'ConfigItems.itemAxeElemental@0': 'thaumcraft:elemental_axe',
    'ConfigItems.itemPickElemental@0': 'thaumcraft:elemental_pickaxe',
    'ConfigItems.itemSwordElemental@0': 'thaumcraft:elemental_sword',
    'ConfigItems.itemShovelElemental@0': 'thaumcraft:elemental_shovel',
    'ConfigItems.itemHoeElemental@0': 'thaumcraft:elemental_hoe',
    'ConfigItems.itemGolemCore@8': 'thaumcraft:golem_core_use',
    'ConfigItems.itemGolemCore@9': 'thaumcraft:golem_core_butcher',
    'ConfigItems.itemGolemCore@10': 'thaumcraft:golem_core_sorting',
    'ConfigItems.itemGolemCore@11': 'thaumcraft:golem_core_fishing',
    'ConfigItems.itemGolemUpgrade@0': 'thaumcraft:golem_upgrade_air',
    'ConfigItems.itemGolemUpgrade@1': 'thaumcraft:golem_upgrade_earth',
    'ConfigItems.itemGolemUpgrade@2': 'thaumcraft:golem_upgrade_fire',
    'ConfigItems.itemGolemUpgrade@3': 'thaumcraft:golem_upgrade_water',
    'ConfigItems.itemGolemUpgrade@4': 'thaumcraft:golem_upgrade_order',
    'ConfigItems.itemGolemUpgrade@5': 'thaumcraft:golem_upgrade_entropy',
    'ConfigBlocks.blockWoodenDevice@0': 'thaumcraft:bellows',
    'ConfigBlocks.blockCosmeticSolid@4': 'thaumcraft:thaumium_block',
    'ConfigItems.itemResource@3': 'thaumcraft:quicksilver',
    'ConfigItems.itemResource@6': 'thaumcraft:amber',
    'ConfigBlocks.blockCustomOre@0': 'thaumcraft:cinnabar_ore',
    'ConfigBlocks.blockCustomOre@32767': 'thaumcraft:cinnabar_ore',
    'ConfigBlocks.blockTable@1': 'thaumcraft:research_table',
    'ConfigBlocks.blockCustomOre@1': 'thaumcraft:infused_stone_air',
    'ConfigBlocks.blockCustomOre@7': 'thaumcraft:amber_ore',
    'ConfigBlocks.blockMagicalLog@0': 'thaumcraft:greatwood_log',
    'ConfigBlocks.blockMagicalLog@1': 'thaumcraft:silverwood_log',
    'ConfigBlocks.blockCustomPlant@0': 'thaumcraft:greatwood_sapling',
    'ConfigBlocks.blockCustomPlant@1': 'thaumcraft:silverwood_sapling',
    'ConfigBlocks.blockCustomPlant@2': 'thaumcraft:shimmerleaf',
    'ConfigBlocks.blockCustomPlant@5': 'thaumcraft:vishroom',
    'ConfigItems.itemManaBean@0': 'thaumcraft:mana_bean',
    'ConfigBlocks.blockCustomPlant@3': 'thaumcraft:cinderpearl',
    'ConfigBlocks.blockTable@0': 'thaumcraft:table',
    'ConfigBlocks.blockTable@14': 'thaumcraft:deconstruction_table',
    'ConfigBlocks.blockTable@15': 'thaumcraft:arcane_workbench',
    'ConfigBlocks.blockWoodenDevice@6': 'thaumcraft:greatwood_planks',
    'ConfigBlocks.blockWoodenDevice@7': 'thaumcraft:silverwood_planks',
}

# Имя рецепта в ConfigRecipes оригинала -> id рецепта в порте.
RECIPES = {
    'WandCapIron': 'thaumcraft:wand_cap_iron',
    'WandBasic': 'thaumcraft:wand',
    'WandCapGold': 'thaumcraft:wand_cap_gold',
    'WandRodGreatwood': 'thaumcraft:wand_rod_greatwood',
    'Table': 'thaumcraft:table',
    'Deconstructor': 'thaumcraft:deconstruction_table',
    'MnemonicMatrix': 'thaumcraft:mnemonic_matrix',
    'BlockTallow': 'thaumcraft:tallow_block',
    'BlockFlesh': 'thaumcraft:flesh_block',
    'CoreBlank': 'thaumcraft:golem_core_blank',
    'AdvancedGolem': 'thaumcraft:infusion/advanced_golem',
    'GolemFetter': 'thaumcraft:golem_fetter',
    'HungryChest': 'thaumcraft:hungry_chest',
    'ArcaneLamp': 'thaumcraft:arcane_lamp',
    'PaveTravel': 'thaumcraft:travel_paving',
    'PaveWard': 'thaumcraft:warding_paving',
    'Grate': 'thaumcraft:grate',
    'ThaumiumFortressHelm': 'thaumcraft:infusion/fortress_helmet',
    'ArcaneBore': 'thaumcraft:infusion/arcane_bore',
    'ArcaneBoreBase': 'thaumcraft:arcane_bore_base',
    'FluxScrubber': 'thaumcraft:flux_scrubber',
    'SinStone': 'thaumcraft:infusion/sinister_stone',
    'BootsTraveller': 'thaumcraft:infusion/traveller_boots',
    'HoverHarness': 'thaumcraft:infusion/hover_harness',
    'HoverGirdle': 'thaumcraft:infusion/hover_girdle',
    'RobeChest': 'thaumcraft:robe_chestplate',
    'RobeLegs': 'thaumcraft:robe_leggings',
    'RobeBoots': 'thaumcraft:robe_boots',
    'ThaumiumFortressChest': 'thaumcraft:infusion/fortress_chestplate',
    'ThaumiumFortressLegs': 'thaumcraft:infusion/fortress_leggings',
    'HelmGoggles': 'thaumcraft:infusion/fortress_goggles',
    'MaskGrinningDevil': 'thaumcraft:infusion/mask_grinning_devil',
    'MaskAngryGhost': 'thaumcraft:infusion/mask_angry_ghost',
    'MaskSippingFiend': 'thaumcraft:infusion/mask_sipping_fiend',
    'BoneBow': 'thaumcraft:bone_bow',
    'ArcaneDoor': 'thaumcraft:arcane_door',
    'WardedGlass': 'thaumcraft:warded_glass',
    'IronKey': 'thaumcraft:key_iron',
    'GoldKey': 'thaumcraft:key_gold',
    'ArcanePressurePlate': 'thaumcraft:arcane_pressure_plate',
    'JarBrain': 'thaumcraft:infusion/brain_jar',
    'MirrorGlass': 'thaumcraft:mirrored_glass',
    'Mirror': 'thaumcraft:infusion/magic_mirror',
    'MirrorHand': 'thaumcraft:infusion/hand_mirror',
    'MirrorEssentia': 'thaumcraft:infusion/essentia_mirror',
    'ArcaneEar': 'thaumcraft:arcane_ear',
    'Levitator': 'thaumcraft:levitator',
    'LampGrowth': 'thaumcraft:infusion/lamp_growth',
    'LampFertility': 'thaumcraft:infusion/lamp_fertility',
    'TravelTrunk': 'thaumcraft:infusion/traveling_trunk',
    'TinyHat': 'thaumcraft:golem_deco_tophat',
    'TinyGlasses': 'thaumcraft:golem_deco_glasses',
    'TinyBowtie': 'thaumcraft:golem_deco_bowtie',
    'TinyFez': 'thaumcraft:golem_deco_fez',
    'TinyDart': 'thaumcraft:golem_deco_dart',
    'TinyVisor': 'thaumcraft:golem_deco_visor',
    'TinyArmor': 'thaumcraft:golem_deco_armor',
    'TinyHammer': 'thaumcraft:golem_deco_mace',
    'GolemBell': 'thaumcraft:golem_bell',
    'GolemStraw': 'thaumcraft:crucible/golem_straw',
    'GolemWood': 'thaumcraft:crucible/golem_wood',
    'GolemTallow': 'thaumcraft:crucible/golem_tallow',
    'GolemClay': 'thaumcraft:crucible/golem_clay',
    'GolemFlesh': 'thaumcraft:crucible/golem_flesh',
    'GolemStone': 'thaumcraft:crucible/golem_stone',
    'GolemIron': 'thaumcraft:crucible/golem_iron',
    'GolemThaumium': 'thaumcraft:crucible/golem_thaumium',
    'CoreGather': 'thaumcraft:crucible/golem_core_gather',
    'CoreFill': 'thaumcraft:crucible/golem_core_fill',
    'CoreEmpty': 'thaumcraft:crucible/golem_core_empty',
    'CoreHarvest': 'thaumcraft:crucible/golem_core_harvest',
    'CoreGuard': 'thaumcraft:crucible/golem_core_guard',
    'CoreButcher': 'thaumcraft:crucible/golem_core_butcher',
    'CoreLiquid': 'thaumcraft:crucible/golem_core_liquid',
    'CoreAlchemy': 'thaumcraft:infusion/golem_core_alchemy',
    'CoreSorting': 'thaumcraft:infusion/golem_core_sorting',
    'CoreFishing': 'thaumcraft:infusion/golem_core_fishing',
    'CoreLumber': 'thaumcraft:infusion/golem_core_lumber',
    'ElementalAxe': 'thaumcraft:infusion/elemental_axe',
    'ElementalPick': 'thaumcraft:infusion/elemental_pickaxe',
    'ElementalSword': 'thaumcraft:infusion/elemental_sword',
    'ElementalShovel': 'thaumcraft:infusion/elemental_shovel',
    'ElementalHoe': 'thaumcraft:infusion/elemental_hoe',
    'CoreUse': 'thaumcraft:infusion/golem_core_use',
    'UpgradeAir': 'thaumcraft:golem_upgrade_air',
    'UpgradeEarth': 'thaumcraft:golem_upgrade_earth',
    'UpgradeFire': 'thaumcraft:golem_upgrade_fire',
    'UpgradeWater': 'thaumcraft:golem_upgrade_water',
    'UpgradeOrder': 'thaumcraft:golem_upgrade_order',
    'UpgradeEntropy': 'thaumcraft:golem_upgrade_entropy',
    'Scribe2': 'thaumcraft:scribing_tools',
    'Scribe1': 'thaumcraft:scribing_tools_phial',
    'Thaumometer': 'thaumcraft:thaumometer',
    'TallowCandle': 'thaumcraft:tallow_candle_white',
    'Scribe3': 'thaumcraft:scribing_tools_refill',
    'PlankGreatwood': 'thaumcraft:greatwood_planks',
    'PlankSilverwood': 'thaumcraft:silverwood_planks',
    'Phial': 'thaumcraft:phial',
    'JarLabel': 'thaumcraft:label',
    'JarLabelNull': 'thaumcraft:label_clear',
    'Tube': 'thaumcraft:tube',
    'TubeValve': 'thaumcraft:tube_valve',
    'TubeFilter': 'thaumcraft:tube_filter',
    'TubeRestrict': 'thaumcraft:tube_restrict',
    'TubeOneway': 'thaumcraft:tube_oneway',
    'TubeBuffer': 'thaumcraft:tube_buffer',
    'EssentiaCrystalizer': 'thaumcraft:crystallizer',
    'Centrifuge': 'thaumcraft:centrifuge',
    'AlchemicalConstruct': 'thaumcraft:alchemical_construct',
    'Bellows': 'thaumcraft:bellows',
    'ArcaneStone2': 'thaumcraft:arcane_stone_bricks',
    'InfusionMatrix': 'thaumcraft:infusion_matrix',
    'ArcanePedestal': 'thaumcraft:pedestal',
    'WandCapThaumiumInert': 'thaumcraft:wand_cap_thaumium_inert',
    'WandCapThaumium': 'thaumcraft:infusion/wand_cap_thaumium',
    'WandRodSilverwood': 'thaumcraft:infusion/wand_rod_silverwood',
    'WandCapCopper': 'thaumcraft:wand_cap_copper',
    'TransCopper': 'thaumcraft:crucible/trans_copper',
    'PrimalCharm': 'thaumcraft:primal_charm',
    'FocusFire': 'thaumcraft:focus_fire',
    'FocalManipulator': 'thaumcraft:focal_manipulator',
    'ArcaneStone4': 'thaumcraft:arcane_stone_slab',
    'ArcaneStone3': 'thaumcraft:arcane_stone_stairs',
    'NodeStabilizer': 'thaumcraft:node_stabilizer',
    'NodeTransducer': 'thaumcraft:node_transducer',
    'NodeRelay': 'thaumcraft:vis_relay',
    'NodeStabilizerAdv': 'thaumcraft:infusion/advanced_node_stabilizer',
    'FocusWarding': 'thaumcraft:infusion/focus_warding',
    'FocusTrade': 'thaumcraft:focus_trade',
    'FocusHellbat': 'thaumcraft:infusion/focus_hellbat',
    'FocusPortableHole': 'thaumcraft:infusion/focus_portable_hole',
    'FocusPrimal': 'thaumcraft:focus_primal',
    'FocusExcavation': 'thaumcraft:focus_excavation',
    'FocusPouch': 'thaumcraft:focus_pouch',
    'MundaneAmulet': 'thaumcraft:bauble_amulet',
    'MundaneRing': 'thaumcraft:bauble_ring',
    'MundaneBelt': 'thaumcraft:bauble_belt',
    'FocusShock': 'thaumcraft:focus_shock',
    'FocusFrost': 'thaumcraft:focus_frost',
    'WandRodPrimalStaff': 'thaumcraft:infusion/wand_rod_primal_staff',
    'WandRodGreatwoodStaff': 'thaumcraft:wand_rod_greatwood_staff',
    'WandRodObsidianStaff': 'thaumcraft:wand_rod_obsidian_staff',
    'WandRodSilverwoodStaff': 'thaumcraft:wand_rod_silverwood_staff',
    'WandRodIceStaff': 'thaumcraft:wand_rod_ice_staff',
    'WandRodQuartzStaff': 'thaumcraft:wand_rod_quartz_staff',
    'WandRodReedStaff': 'thaumcraft:wand_rod_reed_staff',
    'WandRodBlazeStaff': 'thaumcraft:wand_rod_blaze_staff',
    'WandRodBoneStaff': 'thaumcraft:wand_rod_bone_staff',
    'WandRodObsidian': 'thaumcraft:infusion/wand_rod_obsidian',
    'WandRodIce': 'thaumcraft:infusion/wand_rod_ice',
    'WandRodQuartz': 'thaumcraft:infusion/wand_rod_quartz',
    'WandRodReed': 'thaumcraft:infusion/wand_rod_reed',
    'WandRodBlaze': 'thaumcraft:infusion/wand_rod_blaze',
    'WandRodBone': 'thaumcraft:infusion/wand_rod_bone',
    'ArcaneStone1': 'thaumcraft:arcane_stone',
    'WardedJar': 'thaumcraft:warded_jar',
    'JarVoid': 'thaumcraft:void_jar',
    'ArcaneSpa': 'thaumcraft:arcane_spa',
    'BathSalts': 'thaumcraft:crucible/bath_salts',
    'LiquidDeath': 'thaumcraft:crucible/bucket_death',
    'Resonator': 'thaumcraft:resonator',
    'SaneSoap': 'thaumcraft:crucible/sanity_soap',
    'EtherealBloom': 'thaumcraft:crucible/ethereal_bloom',
    'Filter': 'thaumcraft:filter',
    'AlchemyFurnace': 'thaumcraft:alchemical_furnace',
    'Alembic': 'thaumcraft:alembic',
    'Alumentum': 'thaumcraft:crucible/alumentum',
    'BottleTaint': 'thaumcraft:crucible/bottle_taint',
    'Nitor': 'thaumcraft:crucible/nitor',
    'Thaumium': 'thaumcraft:crucible/thaumium',
    'VoidMetal': 'thaumcraft:crucible/void_metal_ingot',
    'AdvAlchemyConstruct': 'thaumcraft:advanced_alchemical_construct',
    'EssentiaReservoir': 'thaumcraft:infusion/essentia_reservoir',
    'PrimalCrusher': 'thaumcraft:infusion/primal_crusher',
    'EldritchEye': 'thaumcraft:infusion/eldritch_eye',
    'VoidRobeHelm': 'thaumcraft:infusion/void_robe_helmet',
    'VoidRobeChest': 'thaumcraft:infusion/void_robe_chestplate',
    'VoidRobeLegs': 'thaumcraft:infusion/void_robe_leggings',
    'SanityCheck': 'thaumcraft:infusion/sanity_checker',
    'WandCapVoidInert': 'thaumcraft:wand_cap_void_inert',
    'WandCapVoid': 'thaumcraft:infusion/wand_cap_void',
    'VoidSeed': 'thaumcraft:crucible/void_seed',
    'VoidAxe': 'thaumcraft:void_axe',
    'VoidSword': 'thaumcraft:void_sword',
    'VoidPick': 'thaumcraft:void_pickaxe',
    'VoidShovel': 'thaumcraft:void_shovel',
    'VoidHoe': 'thaumcraft:void_hoe',
    'VoidHelm': 'thaumcraft:void_helmet',
    'VoidChest': 'thaumcraft:void_chestplate',
    'VoidLegs': 'thaumcraft:void_leggings',
    'VoidBoots': 'thaumcraft:void_boots',
    'Tallow': 'thaumcraft:crucible/tallow',
    'AltGunpowder': 'thaumcraft:crucible/alt_gunpowder',
    'AltSlime': 'thaumcraft:crucible/alt_slime',
    'AltClay': 'thaumcraft:crucible/alt_clay',
    'AltGlowstone': 'thaumcraft:crucible/alt_glowstone',
    'AltInk': 'thaumcraft:crucible/alt_ink',
    'AltWeb': 'thaumcraft:crucible/alt_web',
    'AltMossyCobble': 'thaumcraft:crucible/alt_mossy_cobble',
    'AltIce': 'thaumcraft:crucible/alt_ice',
    'AltCrackedBrick': 'thaumcraft:crucible/alt_cracked_brick',
    'AltBonemeal': 'thaumcraft:crucible/alt_bonemeal',
    'PureIron': 'thaumcraft:crucible/pure_iron',
    'PureGold': 'thaumcraft:crucible/pure_gold',
    'PureCopper': 'thaumcraft:crucible/pure_copper',
    'PureTin': 'thaumcraft:crucible/pure_tin',
    'PureSilver': 'thaumcraft:crucible/pure_silver',
    'PureLead': 'thaumcraft:crucible/pure_lead',
    'TransTin': 'thaumcraft:crucible/trans_tin',
    'TransSilver': 'thaumcraft:crucible/trans_silver',
    'TransLead': 'thaumcraft:crucible/trans_lead',
    'WandCapSilver': 'thaumcraft:infusion/wand_cap_silver',
    'WandCapSilverInert': 'thaumcraft:wand_cap_silver_inert',
    'TransIron': 'thaumcraft:crucible/trans_iron',
    'TransGold': 'thaumcraft:crucible/trans_gold',
    'BlockThaumium': 'thaumcraft:thaumium_block_from_thaumium_ingot',
    'ThaumiumAxe': 'thaumcraft:thaumium_axe',
    'ThaumiumSword': 'thaumcraft:thaumium_sword',
    'ThaumiumPick': 'thaumcraft:thaumium_pickaxe',
    'ThaumiumShovel': 'thaumcraft:thaumium_shovel',
    'ThaumiumHoe': 'thaumcraft:thaumium_hoe',
    'ThaumiumHelm': 'thaumcraft:thaumium_helmet',
    'Goggles': 'thaumcraft:goggles',
    'Clusters0': 'thaumcraft:crystal_air',
    'Clusters1': 'thaumcraft:crystal_fire',
    'Clusters2': 'thaumcraft:crystal_water',
    'Clusters3': 'thaumcraft:crystal_earth',
    'Clusters4': 'thaumcraft:crystal_order',
    'Clusters5': 'thaumcraft:crystal_entropy',
    'Clusters6': 'thaumcraft:crystal_mixed',
    'WandPed': 'thaumcraft:infusion/wand_pedestal',
    'KnowFrag': 'thaumcraft:unknown_knowledge',
    'EnchantedFabric': 'thaumcraft:enchanted_fabric',
    'RunicAmulet': 'thaumcraft:infusion/runic_amulet',
    'RunicAmuletEmergency': 'thaumcraft:infusion/runic_amulet_emergency',
    'RunicRing': 'thaumcraft:infusion/runic_ring',
    'RunicRingCharged': 'thaumcraft:infusion/runic_ring_charged',
    'RunicRingHealing': 'thaumcraft:infusion/runic_ring_healing',
    'RunicGirdle': 'thaumcraft:infusion/runic_girdle',
    'RunicGirdleKinetic': 'thaumcraft:infusion/runic_girdle_kinetic',
    'RunicGirdleKinetic_2': 'thaumcraft:infusion/runic_girdle_kinetic',
    'NodeChargeRelay': 'thaumcraft:vis_charge_relay',
    'WandPedFocus': 'thaumcraft:infusion/wand_pedestal_focus',
    'VisAmulet': 'thaumcraft:infusion/vis_amulet',
    'ThaumiumChest': 'thaumcraft:thaumium_chestplate',
    'ThaumiumLegs': 'thaumcraft:thaumium_leggings',
    'ThaumiumBoots': 'thaumcraft:thaumium_boots',
}

PAGE_TYPES = {
    'IRecipe': 'crafting', 'IArcaneRecipe': 'arcane', 'CrucibleRecipe': 'crucible', 'InfusionRecipe': 'infusion',
    'InfusionEnchantmentRecipe': 'infusion_enchantment', 'List': 'compound', 'ItemStack': 'smelting',
}


def match_paren(text, start):
    """Индекс закрывающей скобки для открывающей в позиции start; строки и вложенность учитываются."""
    depth = 0
    in_string = False
    i = start
    while i < len(text):
        c = text[i]
        if in_string:
            if c == '\\':
                i += 1
            elif c == '"':
                in_string = False
        elif c == '"':
            in_string = True
        elif c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError('незакрытая скобка')


def split_args(text):
    args = []
    depth = 0
    in_string = False
    current = ''
    i = 0
    while i < len(text):
        c = text[i]
        if in_string:
            current += c
            if c == '\\':
                current += text[i + 1]
                i += 1
            elif c == '"':
                in_string = False
        elif c == '"':
            in_string = True
            current += c
        elif c in '([{':
            depth += 1
            current += c
        elif c in ')]}':
            depth -= 1
            current += c
        elif c == ',' and depth == 0:
            args.append(current.strip())
            current = ''
        else:
            current += c
        i += 1
    if current.strip():
        args.append(current.strip())
    return args


def aspects(expr):
    return {LATIN[name]: int(amount) for name, amount in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)', expr)}


def item(expr):
    """ItemStack оригинала -> {'item': id} либо {'legacy': обозначение}, если предмет ещё не перенесён."""
    expr = expr.strip()
    m = re.match(r'new ItemStack\(([\w.]+)(?:, \d+)?(?:, (\d+))?\)$', expr)
    if m:
        name, meta = m.group(1), m.group(2) or '0'
        if name.startswith('Items.') or name.startswith('Blocks.'):
            return {'item': 'minecraft:' + name.split('.')[1]}
        key = name + '@' + meta
    else:
        key = expr
    if key in LEGACY_ITEMS:
        return {'item': LEGACY_ITEMS[key]}
    return {'legacy': key}


# значки-переменные из ConfigResearch: предмет с метками
VARIABLE_ICONS = {
    'sceptre3': {'item': 'thaumcraft:wand', 'nbt': '{rod:"silverwood",cap:"thaumium",sceptre:1b}'},
    'ag': {'item': 'thaumcraft:golem_straw', 'nbt': '{advanced:1b}'},
    'jarIs': {'item': 'thaumcraft:node_jar', 'nbt': '{Aspects:{Aspects:[{key:"aer",amount:40},{key:"ignis",amount:40},'
                                                    '{key:"aqua",amount:40},{key:"terra",amount:40}]},type:0b,modifier:-1b}'},
}


def icon(expr):
    if expr.strip() in VARIABLE_ICONS:
        return dict(VARIABLE_ICONS[expr.strip()])
    m = re.match(r'new ResourceLocation\("(\w+)", "([^"]+)"\)$', expr.strip())
    if m:
        path = m.group(2).replace('textures/items/', 'textures/item/').replace('textures/blocks/', 'textures/block/')
        return {'texture': m.group(1) + ':' + path.lower()}
    return item(expr)


# имена 1.7.10, которые в 1.20.1 называются иначе или не имеют предмета
CONSTRUCT_RENAMES = {'minecraft:nether_brick': 'minecraft:nether_bricks', 'minecraft:lava': 'minecraft:lava_bucket'}


def compound(name):
    """Составная конструкция из ConfigRecipes: цена в вис, размеры и блоки послойно, сверху вниз."""
    text = open(os.path.join(os.path.dirname(SOURCE), 'ConfigRecipes.java'), encoding='utf-8').read()
    head = 'recipes.put("%s", Arrays.asList(' % name
    if head not in text:
        return None
    start = text.index(head) + len(head)
    depth, i = 1, start
    while depth:
        depth += (text[i] == '(') - (text[i] == ')')
        i += 1
    args = split_args(text[start:i - 1])
    aspects = {LATIN[a]: int(n) for a, n in re.findall(r'add\(Aspect\.(\w+), (\d+)\)', args[0])}
    listing = args[4].strip()
    blocks = []
    for arg in split_args(listing[listing.index('(') + 1:-1]):
        arg = arg.strip()
        if arg in ('null', 'empty'):
            blocks.append(None)
        elif arg == 'basicWand':
            blocks.append('thaumcraft:wand')
        elif arg == '"slabWood"':
            blocks.append('minecraft:oak_slab')
        else:
            found = item(arg)
            if 'item' not in found:
                print('   конструкция %s: нет предмета для %s' % (name, found.get('legacy')))
            blocks.append(CONSTRUCT_RENAMES.get(found.get('item'), found.get('item')))
    return {'aspects': aspects, 'size': [int(args[1]), int(args[2]), int(args[3])], 'blocks': blocks}


def page(expr):
    inner = expr[expr.index('(') + 1:-1].strip()
    args = split_args(inner)
    if len(args) == 1 and args[0].startswith('"'):
        return {'type': 'text', 'text': args[0].strip('"')}
    if len(args) == 2 and args[0].startswith('"') and args[1].startswith('"'):
        return {'type': 'text_concealed', 'research': args[0].strip('"'), 'text': args[1].strip('"')}
    if args[0].startswith('new ResourceLocation'):
        out = {'type': 'image', 'image': icon(args[0])['texture']}
        if len(args) > 1:
            out['text'] = args[1].strip('"')
        return out
    if args[0].startswith('Aspect.'):
        return {'type': 'aspects', 'aspect': LATIN[args[0].split('.')[1]]}
    m = re.match(r'\((\w+)(\[\])?\)\s*\(?recipes\.get\("([^"]+)"\)\)?$', args[0])
    if m:
        kind, name = PAGE_TYPES.get(m.group(1), m.group(1)), m.group(3)
        out = {'type': kind}
        if kind == 'infusion_enchantment':
            import enchant_recipes
            for recipe in enchant_recipes.recipes():
                if recipe['legacy'] == name:
                    out['recipe'] = 'thaumcraft:infusion/' + recipe['name']
                    return out
        if kind == 'compound' and compound(name):
            out.update(compound(name))
            return out
        if name in RECIPES:
            out['recipe'] = RECIPES[name]
        else:
            out['legacy'] = name
        return out
    if args[0].startswith('new ItemStack'):
        out = {'type': 'smelting'}
        out.update(item(args[0]))
        return out
    if args[0].startswith('(InfusionRecipe[])raug.toArray'):
        # «Рунное усиление»: один рецепт, цена которого зависит от вещи
        return {'type': 'infusion', 'recipe': 'thaumcraft:infusion/runic_augment'}
    if args[0].startswith('(IRecipe[])rc.toArray'):
        # семь рецептов кристаллических гроздьев на одной странице
        return {'type': 'crafting', 'recipes': ['thaumcraft:crystal_' + e for e in
                                                 ('air', 'fire', 'water', 'earth', 'order', 'entropy', 'mixed')]}
    if args[0].startswith('(CrucibleRecipe[])cruc'):
        # шесть рецептов уравновешенного осколка на одной странице
        return {'type': 'crucible', 'recipes': ['thaumcraft:crucible/balanced_shard_' + e for e in
                                                 ('air', 'fire', 'water', 'earth', 'order', 'entropy')]}
    if args[0].startswith('(IArcaneRecipe[])rcbb'):
        # шесть первозданных стрел на одной странице
        return {'type': 'arcane', 'recipes': ['thaumcraft:primal_arrow_' + e for e in
                                               ('air', 'fire', 'water', 'earth', 'order', 'entropy')]}
    if args[0].startswith('(IArcaneRecipe[])banners'):
        # шестнадцать знамён на одной странице
        return {'type': 'arcane', 'recipes': ['thaumcraft:thaum_banner_' + c for c in (
            'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
            'brown', 'green', 'red', 'black')]}
    if args[0].startswith('(IArcaneRecipe[])scer'):
        # «Скипетр»: три образца, которые книга собирает сама (рецепт настоящий — любой набалдашник и стержень)
        return {'type': 'sceptre'}
    return {'type': 'unknown', 'legacy': inner[:120]}


def strings(expr):
    return re.findall(r'"([^"]*)"', expr)


def main():
    text = open(SOURCE, encoding='utf-8').read()

    categories = []
    for key, tex, background in re.findall(
            r'registerCategory\("(\w+)", new ResourceLocation\("thaumcraft", "([^"]+)"\), '
            r'new ResourceLocation\("thaumcraft", "([^"]+)"\)\)', text):
        categories.append({
            'key': key,
            'icon': 'thaumcraft:' + tex.replace('textures/items/', 'textures/item/').lower(),
            'background': 'thaumcraft:' + background.lower(),
        })

    entries = []
    unknown_flags = set()
    pos = 0
    while True:
        start = text.find('new ResearchItem(', pos)
        if start < 0:
            break
        open_paren = text.index('(', start)
        close_paren = match_paren(text, open_paren)
        args = split_args(text[open_paren + 1:close_paren])
        entry = {'key': args[0].strip('"'), 'category': args[1].strip('"')}
        if len(args) >= 7:
            entry['aspects'] = aspects(args[2])
            entry['column'] = int(args[3])
            entry['row'] = int(args[4])
            entry['complexity'] = max(1, min(3, int(args[5])))
            entry['icon'] = icon(args[6])
        else:
            entry['virtual'] = True

        # цепочка вызовов .setX(...) до registerResearchItem()
        i = close_paren + 1
        while True:
            m = re.match(r'\)*\s*\.(\w+)\(', text[i:])
            if not m:
                break
            name = m.group(1)
            call_open = i + m.end() - 1
            call_close = match_paren(text, call_open)
            body = text[call_open + 1:call_close]
            if name == 'registerResearchItem':
                break
            if name == 'setPages':
                entry['pages'] = [page(p) for p in split_args(body)]
            elif name == 'setEntityTriggers':
                # старые имена существ -> id 1.20.1; зомби с мозгами появятся вместе с жуткими землями
                entry['entity_triggers'] = [{
                    'Thaumcraft.Firebat': 'thaumcraft:fire_bat', 'Enderman': 'minecraft:enderman',
                    'Thaumcraft.BrainyZombie': 'thaumcraft:brainy_zombie',
                    'Thaumcraft.GiantBrainyZombie': 'thaumcraft:giant_brainy_zombie',
                    'Thaumcraft.PrimalOrb': 'thaumcraft:primal_orb'}[old] for old in strings(body)]
            elif name in ('setParents', 'setParentsHidden', 'setSiblings'):
                key = {'setParents': 'parents', 'setParentsHidden': 'parents_hidden', 'setSiblings': 'siblings'}[name]
                entry[key] = strings(body)
            elif name == 'setItemTriggers':
                entry['item_triggers'] = [item(a) for a in split_args(body)]
            elif name == 'setAspectTriggers':
                entry['aspect_triggers'] = [LATIN[a] for a in re.findall(r'Aspect\.(\w+)', body)]
            elif name in ('setSpecial', 'setStub', 'setLost', 'setConcealed', 'setHidden', 'setVirtual',
                          'setAutoUnlock', 'setRound', 'setSecondary'):
                entry[name[3].lower() + name[4:]] = True
            elif name == 'setComplexity':
                entry['complexity'] = int(body)
            else:
                unknown_flags.add(name)
            i = call_close + 1
        # исследования, которые в оригинале появлялись только при чужих модах с оловом, серебром и свинцом:
        # в игре они есть, только если эти теги чем-то заполнены
        requires = {
            'PURETIN': ['forge:ores/tin', 'forge:ingots/tin'], 'PURESILVER': ['forge:ores/silver', 'forge:ingots/silver'],
            'PURELEAD': ['forge:ores/lead', 'forge:ingots/lead'], 'TRANSTIN': ['forge:ingots/tin'],
            'TRANSSILVER': ['forge:ingots/silver'], 'TRANSLEAD': ['forge:ingots/lead'], 'CAP_silver': ['forge:ingots/silver'],
        }.get(entry['key'])
        if requires:
            entry['requires'] = requires
        entries.append(entry)
        pos = close_paren

    warp = {key: int(amount) for key, amount in re.findall(r'addWarpToResearch\("(\w+)", (\d+)\)', text)}
    for entry in entries:
        if entry['key'] in warp:
            entry['warp'] = warp[entry['key']]

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as fh:
        fh.write('{\n  "categories": [\n')
        fh.write(',\n'.join('    ' + json.dumps(c) for c in categories))
        fh.write('\n  ],\n  "entries": [\n')
        fh.write(',\n'.join('    ' + json.dumps(e) for e in entries))
        fh.write('\n  ]\n}\n')

    legacy_icons = sum(1 for e in entries if 'legacy' in e.get('icon', {}))
    pages = [p for e in entries for p in e.get('pages', [])]
    legacy_pages = sum(1 for p in pages if 'legacy' in p)
    print('категорий:', len(categories), 'исследований:', len(entries))
    print('иконок-заглушек:', legacy_icons, 'страниц:', len(pages), 'из них рецептов без переноса:', legacy_pages)
    # что ещё не перенесено: исследования с иконкой-заглушкой или страницей рецепта без переноса
    unfinished = {}
    for e in entries:
        if 'legacy' in e.get('icon', {}) or any('legacy' in p for p in e.get('pages', [])):
            unfinished.setdefault(e.get('category', '?'), []).append(e['key'])
    for category, keys in sorted(unfinished.items()):
        print('  не перенесено в', category, '(%d):' % len(keys), ' '.join(keys))
    by_type = {}
    for p in pages:
        by_type[p['type']] = by_type.get(p['type'], 0) + 1
    print('страницы по видам:', by_type)
    if unknown_flags:
        print('неразобранные вызовы:', sorted(unknown_flags))


if __name__ == '__main__':
    main()

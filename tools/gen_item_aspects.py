"""Строит data/thaumcraft/thaumcraft_aspects/vanilla.json из оригинального ConfigAspects.java.

Запуск из корня проекта:  python tools/gen_item_aspects.py

Числа аспектов берутся из оригинала автоматически; вручную задано только соответствие
старых имён (OreDictionary, Blocks.x, Items.x@мета) новым id и тегам 1.20.1.
Ключ с '#' — тег. Старые записи без соответствия печатаются в конце, чтобы ничего не терялось молча.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, 'reference', 'tc4', 'src', 'thaumcraft', 'common', 'config', 'ConfigAspects.java')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'thaumcraft', 'thaumcraft_aspects', 'vanilla.json')

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


def mc(*names):
    return ['minecraft:' + n for n in names]


def tc(*names):
    return ['thaumcraft:' + n for n in names]


MAP = {
    # OreDictionary -> теги
    '"stone"': ['#forge:stone'],
    '"cobblestone"': ['#forge:cobblestone'],
    '"logWood"': ['#minecraft:logs'],
    '"plankWood"': ['#minecraft:planks'],
    '"slabWood"': ['#minecraft:wooden_slabs'],
    '"stairWood"': ['#minecraft:wooden_stairs'],
    '"stickWood"': ['#forge:rods/wooden'],
    '"treeSapling"': ['#minecraft:saplings'],
    '"treeLeaves"': ['#minecraft:leaves'],
    'dyes[i]': ['#forge:dyes'],
    '"oreLapis"': ['#forge:ores/lapis'],
    '"oreDiamond"': ['#forge:ores/diamond'],
    '"gemDiamond"': ['#forge:gems/diamond'],
    '"oreRedstone"': ['#forge:ores/redstone'],
    '"oreEmerald"': ['#forge:ores/emerald'],
    '"gemEmerald"': ['#forge:gems/emerald'],
    '"oreQuartz"': ['#forge:ores/quartz'],
    '"nuggetIron"': ['#forge:nuggets/iron'],
    '"oreIron"': ['#forge:ores/iron'],
    '"dustIron"': ['#forge:dusts/iron'],
    '"oreGold"': ['#forge:ores/gold'],
    '"dustGold"': ['#forge:dusts/gold'],
    '"dustRedstone"': ['#forge:dusts/redstone'],
    '"dustGlowstone"': ['#forge:dusts/glowstone'],
    '"glowstone"': mc('glowstone'),
    '"nuggetCopper"': ['#forge:nuggets/copper'],
    '"ingotCopper"': ['#forge:ingots/copper'],
    '"dustCopper"': ['#forge:dusts/copper'],
    '"oreCopper"': ['#forge:ores/copper'],
    '"nuggetTin"': ['#forge:nuggets/tin'],
    '"ingotTin"': ['#forge:ingots/tin'],
    '"dustTin"': ['#forge:dusts/tin'],
    '"oreTin"': ['#forge:ores/tin'],
    '"nuggetSilver"': ['#forge:nuggets/silver'],
    '"ingotSilver"': ['#forge:ingots/silver'],
    '"dustSilver"': ['#forge:dusts/silver'],
    '"oreSilver"': ['#forge:ores/silver'],
    '"nuggetLead"': ['#forge:nuggets/lead'],
    '"ingotLead"': ['#forge:ingots/lead'],
    '"dustLead"': ['#forge:dusts/lead'],
    '"oreLead"': ['#forge:ores/lead'],

    # ванильные предметы и блоки
    'Items.quartz': mc('quartz'),
    'Items.gold_nugget': mc('gold_nugget'),
    'Items.iron_ingot': mc('iron_ingot'),
    'Items.gold_ingot': mc('gold_ingot'),
    'Blocks.coal_ore': ['#forge:ores/coal'],
    'Items.coal@*': mc('coal', 'charcoal'),
    'Blocks.bedrock': mc('bedrock'),
    'Blocks.dirt@*': mc('dirt', 'coarse_dirt', 'rooted_dirt'),
    'Blocks.dirt@2': mc('podzol'),
    'Blocks.farmland@*': mc('farmland'),
    'Blocks.sand@*': ['#minecraft:sand'],
    'Blocks.grass': mc('grass_block'),
    'Items.clay_ball@*': mc('clay_ball'),
    'Blocks.end_stone': mc('end_stone'),
    'Blocks.gravel': mc('gravel'),
    'Blocks.mycelium': mc('mycelium'),
    'Blocks.clay@*': mc('clay'),
    'Blocks.hardened_clay@*': mc('terracotta'),
    'Blocks.stained_hardened_clay@*': ['#minecraft:terracotta'],
    'Items.brick@*': mc('brick'),
    'Items.netherbrick@*': mc('nether_brick'),
    'Blocks.soul_sand@*': mc('soul_sand', 'soul_soil'),
    'Blocks.netherrack@*': mc('netherrack'),
    'Blocks.nether_brick': mc('nether_bricks'),
    'Blocks.glass@*': mc('glass'),
    'Blocks.stained_glass@*': ['#forge:glass'],
    'Blocks.mossy_cobblestone@*': mc('mossy_cobblestone'),
    'Blocks.obsidian@*': mc('obsidian'),
    'Blocks.monster_egg@*': mc('infested_stone', 'infested_cobblestone', 'infested_stone_bricks',
                               'infested_mossy_stone_bricks', 'infested_cracked_stone_bricks',
                               'infested_chiseled_stone_bricks', 'infested_deepslate'),
    'Blocks.stonebrick@*': mc('stone_bricks'),
    'Blocks.stonebrick@1': mc('mossy_stone_bricks'),
    'Blocks.stonebrick@2': mc('cracked_stone_bricks'),
    'Blocks.stonebrick@3': mc('chiseled_stone_bricks'),
    'Blocks.sandstone@*': mc('sandstone'),
    'Blocks.sandstone@1': mc('chiseled_sandstone'),
    'Blocks.sandstone@2': mc('cut_sandstone'),
    'Blocks.tallgrass@*': mc('grass', 'fern'),
    'Blocks.double_plant@*': mc('tall_grass', 'large_fern', 'sunflower', 'lilac', 'rose_bush', 'peony'),
    'Blocks.waterlily@*': mc('lily_pad'),
    'Blocks.deadbush@*': mc('dead_bush'),
    'Blocks.vine@*': mc('vine'),
    'Items.wheat_seeds@*': mc('wheat_seeds'),
    'Items.melon_seeds@*': mc('melon_seeds'),
    'Items.pumpkin_seeds@*': mc('pumpkin_seeds'),
    'Items.melon@*': mc('melon_slice'),
    'Items.nether_wart': mc('nether_wart'),
    'Blocks.red_flower@*': ['#minecraft:small_flowers'],
    'Blocks.yellow_flower@*': mc('dandelion'),
    'Blocks.cactus': mc('cactus'),
    'Blocks.brown_mushroom': mc('brown_mushroom'),
    'Blocks.red_mushroom': mc('red_mushroom'),
    'Blocks.brown_mushroom_block@*': mc('brown_mushroom_block'),
    'Blocks.red_mushroom_block@*': mc('red_mushroom_block'),
    'Items.reeds': mc('sugar_cane'),
    'Items.wheat': mc('wheat'),
    'Items.apple': mc('apple'),
    'Items.carrot': mc('carrot'),
    'Items.potato': mc('potato'),
    'Items.baked_potato': mc('baked_potato'),
    'Items.poisonous_potato': mc('poisonous_potato'),
    'Blocks.pumpkin@*': mc('pumpkin', 'carved_pumpkin'),
    'Blocks.melon_block@*': mc('melon'),
    'Items.name_tag@*': mc('name_tag'),
    'Items.iron_horse_armor@*': mc('iron_horse_armor'),
    'Items.golden_horse_armor@*': mc('golden_horse_armor'),
    'Items.diamond_horse_armor@*': mc('diamond_horse_armor'),
    'Blocks.ice@*': mc('ice'),
    'Blocks.packed_ice@*': mc('packed_ice'),
    'Items.snowball@*': mc('snowball'),
    'Items.cookie@*': mc('cookie'),
    'Items.potionitem': mc('potion', 'splash_potion', 'lingering_potion'),
    'Blocks.torch@*': mc('torch'),
    'Blocks.web@*': mc('cobweb'),
    'Items.flint@*': mc('flint'),
    'Items.string@*': mc('string'),
    'Items.slime_ball': mc('slime_ball'),
    'Items.leather@*': mc('leather'),
    'Items.rotten_flesh@*': mc('rotten_flesh'),
    'Items.feather@*': mc('feather'),
    'Items.bone@*': mc('bone'),
    'Items.egg@*': mc('egg'),
    'Items.spider_eye@*': mc('spider_eye'),
    'Items.gunpowder@*': mc('gunpowder'),
    'Blocks.wool@*': ['#minecraft:wool'],
    'Items.fish@*': mc('cod', 'salmon', 'tropical_fish', 'pufferfish'),
    'Items.cooked_fish@*': mc('cooked_cod', 'cooked_salmon'),
    'Items.chicken@*': mc('chicken'),
    'Items.cooked_chicken@*': mc('cooked_chicken'),
    'Items.porkchop@*': mc('porkchop'),
    'Items.cooked_porkchop@*': mc('cooked_porkchop'),
    'Items.beef@*': mc('beef'),
    'Items.cooked_beef@*': mc('cooked_beef'),
    'Items.blaze_rod@*': mc('blaze_rod'),
    'Items.saddle@*': mc('saddle'),
    'Items.ender_pearl@*': mc('ender_pearl'),
    'Items.ghast_tear@*': mc('ghast_tear'),
    'Items.skull@0': mc('skeleton_skull'),
    'Items.skull@1': mc('wither_skeleton_skull'),
    'Items.skull@2': mc('zombie_head'),
    'Items.skull@3': mc('player_head'),
    'Items.skull@4': mc('creeper_head'),
    'Items.record_11': mc('music_disc_11'),
    'Items.record_13': mc('music_disc_13'),
    'Items.record_cat': mc('music_disc_cat'),
    'Items.record_chirp': mc('music_disc_chirp'),
    'Items.record_far': mc('music_disc_far'),
    'Items.record_mall': mc('music_disc_mall'),
    'Items.record_mellohi': mc('music_disc_mellohi'),
    'Items.record_stal': mc('music_disc_stal'),
    'Items.record_strad': mc('music_disc_strad'),
    'Items.record_ward': mc('music_disc_ward'),
    'Items.record_blocks': mc('music_disc_blocks'),
    'Items.record_wait': mc('music_disc_wait'),
    'Items.nether_star': mc('nether_star'),
    'Items.chainmail_helmet@*': mc('chainmail_helmet'),
    'Items.chainmail_chestplate@*': mc('chainmail_chestplate'),
    'Items.chainmail_leggings@*': mc('chainmail_leggings'),
    'Items.chainmail_boots@*': mc('chainmail_boots'),
    'Items.paper': mc('paper'),
    'Items.book': mc('book'),
    'Blocks.bookshelf': mc('bookshelf'),
    'Blocks.dragon_egg': mc('dragon_egg'),
    'Blocks.end_portal_frame@*': mc('end_portal_frame'),
    'Blocks.mob_spawner@*': mc('spawner'),
    'Items.ender_eye': mc('ender_eye'),
    'Items.arrow': mc('arrow'),
    'Items.glass_bottle': mc('glass_bottle'),
    'Blocks.chest@*': mc('chest'),
    'Items.golden_apple@0': mc('golden_apple'),
    'Items.golden_apple@1': mc('enchanted_golden_apple'),
    'Items.bowl': mc('bowl'),
    'Items.mushroom_stew': mc('mushroom_stew'),
    'Items.minecart': mc('minecart'),
    'Items.iron_door': mc('iron_door'),
    'Items.wooden_door': ['#minecraft:wooden_doors'],
    'Items.boat': ['#minecraft:boats'],
    'Items.flint_and_steel@*': mc('flint_and_steel'),
    'Items.fishing_rod@*': mc('fishing_rod'),
    'Items.bucket': mc('bucket'),
    'Items.water_bucket': mc('water_bucket'),
    'Items.lava_bucket': mc('lava_bucket'),
    'Items.milk_bucket': mc('milk_bucket'),
    'Items.cake, 32767': mc('cake'),
    'Items.brewing_stand': mc('brewing_stand'),
    'Blocks.stone_button': mc('stone_button'),
    'Blocks.rail@*': mc('rail'),
    'Blocks.detector_rail@*': mc('detector_rail'),
    'Blocks.golden_rail@*': mc('powered_rail'),
    'Blocks.activator_rail@*': mc('activator_rail'),
    'Blocks.fence_gate@*': ['#minecraft:fence_gates'],
    'Blocks.wooden_pressure_plate@*': ['#minecraft:wooden_pressure_plates'],
    'Blocks.stone_pressure_plate@*': mc('stone_pressure_plate'),
    'Blocks.light_weighted_pressure_plate@*': mc('light_weighted_pressure_plate'),
    'Blocks.heavy_weighted_pressure_plate@*': mc('heavy_weighted_pressure_plate'),
    'Blocks.lever@*': mc('lever'),
    'Blocks.redstone_torch@*': mc('redstone_torch'),
    'Blocks.piston@*': mc('piston'),
    'Blocks.sticky_piston@*': mc('sticky_piston'),
    'Blocks.jukebox': mc('jukebox'),
    'Blocks.noteblock': mc('note_block'),
    'Blocks.trapdoor@*': ['#minecraft:wooden_trapdoors'],
    'Blocks.furnace@*': mc('furnace'),
    'Blocks.enchanting_table': mc('enchanting_table'),
    'Blocks.crafting_table': mc('crafting_table'),
    'Items.clock': mc('clock'),
    'Blocks.anvil), new int[]{0@2}': mc('anvil', 'chipped_anvil', 'damaged_anvil'),
    'Blocks.beacon': mc('beacon'),
    'Blocks.wooden_button@*': ['#minecraft:wooden_buttons'],
    'Items.carrot_on_a_stick@*': mc('carrot_on_a_stick'),
    'Items.flower_pot': mc('flower_pot'),
    'Items.golden_carrot': mc('golden_carrot'),
    'Blocks.ender_chest@*': mc('ender_chest'),
    'Blocks.hopper@*': mc('hopper'),
    'Blocks.dropper@*': mc('dropper'),
    'Blocks.dispenser@*': mc('dispenser'),
    'Blocks.tripwire_hook@*': mc('tripwire_hook'),
    'Blocks.daylight_detector@*': mc('daylight_detector'),

    # предметы самого Thaumcraft (пополняется по мере появления в порте)
    'ConfigItems.itemShard@0': tc('shard_air'),
    'ConfigItems.itemShard@1': tc('shard_fire'),
    'ConfigItems.itemShard@2': tc('shard_water'),
    'ConfigItems.itemShard@3': tc('shard_earth'),
    'ConfigItems.itemShard@4': tc('shard_order'),
    'ConfigItems.itemShard@5': tc('shard_entropy'),
    'ConfigBlocks.blockCustomOre@0': tc('cinnabar_ore', 'deepslate_cinnabar_ore'),
    'ConfigBlocks.blockCustomOre@1': tc('infused_stone_air', 'deepslate_infused_stone_air'),
    'ConfigBlocks.blockCustomOre@2': tc('infused_stone_fire', 'deepslate_infused_stone_fire'),
    'ConfigBlocks.blockCustomOre@3': tc('infused_stone_water', 'deepslate_infused_stone_water'),
    'ConfigBlocks.blockCustomOre@4': tc('infused_stone_earth', 'deepslate_infused_stone_earth'),
    'ConfigBlocks.blockCustomOre@5': tc('infused_stone_order', 'deepslate_infused_stone_order'),
    'ConfigBlocks.blockCustomOre@6': tc('infused_stone_entropy', 'deepslate_infused_stone_entropy'),
    'ConfigBlocks.blockCustomOre@7': tc('amber_ore', 'deepslate_amber_ore'),
    'ConfigItems.itemResource@3': tc('quicksilver'),
    'ConfigBlocks.blockMagicalLog@0': tc('greatwood_log'),
    'ConfigBlocks.blockMagicalLog@1': tc('silverwood_log'),
    'ConfigBlocks.blockMagicalLeaves@0': tc('greatwood_leaves'),
    'ConfigBlocks.blockMagicalLeaves@1': tc('silverwood_leaves'),
    'ConfigBlocks.blockCustomPlant@0': tc('greatwood_sapling'),
    'ConfigBlocks.blockCustomPlant@1': tc('silverwood_sapling'),
    'ConfigBlocks.blockCustomPlant@2': tc('shimmerleaf'),
    'ConfigBlocks.blockCustomPlant@4': tc('ethereal_bloom'),
    'ConfigBlocks.blockJar@3': tc('void_jar'),
    'ConfigBlocks.blockCosmeticSolid@5': tc('tallow_block'),
    'ConfigItems.itemGolemCore@100': tc('golem_core_blank'),
    'ConfigBlocks.blockCosmeticSolid@9': tc('golem_fetter'),
    'ConfigBlocks.blockChestHungry@0': tc('hungry_chest'),
    'ConfigItems.itemTrunkSpawner@0': tc('traveling_trunk'),
    'ConfigItems.itemGolemDecoration@0': tc('golem_deco_tophat'),
    'ConfigItems.itemGolemDecoration@1': tc('golem_deco_glasses'),
    'ConfigItems.itemGolemDecoration@2': tc('golem_deco_bowtie'),
    'ConfigItems.itemGolemDecoration@3': tc('golem_deco_fez'),
    'ConfigItems.itemGolemDecoration@4': tc('golem_deco_dart'),
    'ConfigItems.itemGolemDecoration@5': tc('golem_deco_visor'),
    'ConfigItems.itemGolemDecoration@6': tc('golem_deco_armor'),
    'ConfigItems.itemGolemDecoration@7': tc('golem_deco_mace'),
    'ConfigItems.itemGolemBell@0': tc('golem_bell'),
    'ConfigItems.itemGolemPlacer@0': tc('golem_straw'),
    'ConfigItems.itemGolemPlacer@1': tc('golem_wood'),
    'ConfigItems.itemGolemPlacer@2': tc('golem_tallow'),
    'ConfigItems.itemGolemPlacer@3': tc('golem_clay'),
    'ConfigItems.itemGolemPlacer@4': tc('golem_flesh'),
    'ConfigItems.itemGolemPlacer@5': tc('golem_stone'),
    'ConfigItems.itemGolemPlacer@6': tc('golem_iron'),
    'ConfigItems.itemGolemPlacer@7': tc('golem_thaumium'),
    'ConfigItems.itemGolemCore@0': tc('golem_core_fill'),
    'ConfigItems.itemGolemCore@1': tc('golem_core_empty'),
    'ConfigItems.itemGolemCore@2': tc('golem_core_gather'),
    'ConfigItems.itemGolemCore@3': tc('golem_core_harvest'),
    'ConfigItems.itemGolemCore@4': tc('golem_core_guard'),
    'ConfigItems.itemGolemCore@5': tc('golem_core_liquid'),
    'ConfigItems.itemGolemCore@6': tc('golem_core_alchemy'),
    'ConfigItems.itemGolemCore@7': tc('golem_core_lumber'),
    'ConfigItems.itemGolemCore@8': tc('golem_core_use'),
    'ConfigItems.itemGolemCore@9': tc('golem_core_butcher'),
    'ConfigItems.itemGolemCore@10': tc('golem_core_sorting'),
    'ConfigItems.itemGolemCore@11': tc('golem_core_fishing'),
    'ConfigItems.itemGolemUpgrade@0': tc('golem_upgrade_air'),
    'ConfigItems.itemGolemUpgrade@1': tc('golem_upgrade_earth'),
    'ConfigItems.itemGolemUpgrade@2': tc('golem_upgrade_fire'),
    'ConfigItems.itemGolemUpgrade@3': tc('golem_upgrade_water'),
    'ConfigItems.itemGolemUpgrade@4': tc('golem_upgrade_order'),
    'ConfigItems.itemGolemUpgrade@5': tc('golem_upgrade_entropy'),
    'ConfigBlocks.blockMetalDevice@12': tc('mnemonic_matrix'),
    'ConfigBlocks.blockTable@14': tc('deconstruction_table'),
    'ConfigBlocks.blockStoneDevice@12': tc('arcane_spa'),
    'ConfigItems.itemBathSalts@0': tc('bath_salts'),
    'ConfigItems.itemBucketDeath@0': tc('bucket_death'),
    'ConfigItems.itemBucketPure@0': tc('bucket_pure'),
    'ConfigItems.itemResonator@0': tc('resonator'),
    'ConfigItems.itemSanitySoap@0': tc('sanity_soap'),
    'ConfigBlocks.blockCandle@0': tc('tallow_candle_white', 'tallow_candle_orange', 'tallow_candle_magenta', 'tallow_candle_light_blue', 'tallow_candle_yellow', 'tallow_candle_lime', 'tallow_candle_pink', 'tallow_candle_gray', 'tallow_candle_light_gray', 'tallow_candle_cyan', 'tallow_candle_purple', 'tallow_candle_blue', 'tallow_candle_brown', 'tallow_candle_green', 'tallow_candle_red', 'tallow_candle_black'),
    'ConfigBlocks.blockCustomPlant@5': tc('vishroom'),
    'ConfigBlocks.blockCustomPlant@3': tc('cinderpearl'),
    'ConfigItems.itemResource@6': tc('amber'),
    'ConfigItems.itemNugget@5': tc('quicksilver_nugget'),
    'ConfigItems.itemNugget@6': tc('thaumium_nugget'),
    'ConfigItems.itemNugget@16': tc('cluster_iron'),
    'ConfigBlocks.blockTable': tc('table'),
    'ConfigBlocks.blockTable@15': tc('arcane_workbench'),
    'ConfigBlocks.blockTable@2': tc('research_table'),
    'ConfigBlocks.blockTaint@0': tc('taint_crust'),
    'ConfigBlocks.blockTaint@1': tc('tainted_soil'),
    'ConfigBlocks.blockTaintFibres@0': tc('taint_fibres'),
    'ConfigBlocks.blockTaintFibres@1': tc('taint_grass'),
    'ConfigBlocks.blockTaintFibres@2': tc('taint_plant'),
    'ConfigBlocks.blockTaintFibres@3': tc('spore_stalk'),
    'ConfigItems.itemZombieBrain': tc('zombie_brain'),
    'ConfigItems.itemResource@9': tc('knowledge_fragment'),
    'ConfigItems.itemResource@11': tc('taint_goo'),
    'ConfigItems.itemResource@12': tc('taint_tendril'),
    'ConfigItems.itemResource@18': tc('coin'),
    'ConfigItems.itemLootbag@0': tc('loot_bag_common'),
    'ConfigItems.itemLootbag@1': tc('loot_bag_uncommon'),
    'ConfigItems.itemLootbag@2': tc('loot_bag_rare'),
    'ConfigBlocks.blockCandle': tc(*['tallow_candle_' + c for c in ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink',
                                                                     'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green',
                                                                     'red', 'black')]),
    'ConfigItems.itemThaumonomicon@*': tc('thaumonomicon'),
    'ConfigBlocks.blockAlchemyFurnace@*': tc('alchemical_furnace'),
    'ConfigItems.itemEssence@0': tc('phial'),
    'ConfigItems.itemWispEssence@0': tc('wisp_essence'),
    'ConfigItems.itemPrimalArrow': tc(*['primal_arrow_' + a for a in ('air', 'fire', 'water', 'earth', 'order', 'entropy')]),
    'ConfigBlocks.blockWoodenDevice@1': tc('arcane_ear'),
    # кольца подмастерья (мета 3..8) в оригинале с аспектом только у первого; даём его всем шести
    'ConfigItems.itemBaubleBlanks@3': tc(*['apprentice_ring_' + a for a in ('air', 'fire', 'water', 'earth', 'order', 'entropy')]),
    'ConfigItems.itemFocusPech': tc('focus_pech'),
    'ConfigItems.itemEldritchObject@0': tc('eldritch_eye'),
    'ConfigItems.itemNugget@18': tc('cluster_tin'),
    'ConfigItems.itemNugget@19': tc('cluster_silver'),
    'ConfigItems.itemNugget@20': tc('cluster_lead'),
    'ConfigItems.itemChestCultistPlate@*': tc('cultist_plate_chestplate'),
    'ConfigItems.itemChestCultistRobe@*': tc('cultist_robe_chestplate'),
    'ConfigItems.itemChestCultistLeaderPlate@*': tc('cultist_leader_chestplate'),
    'ConfigItems.itemLegsCultistPlate@*': tc('cultist_plate_leggings'),
    'ConfigItems.itemLegsCultistLeaderPlate@*': tc('cultist_leader_leggings'),
    'ConfigItems.itemLegsCultistRobe@*': tc('cultist_robe_leggings'),
    'ConfigItems.itemHelmetCultistPlate@*': tc('cultist_plate_helmet'),
    'ConfigItems.itemHelmetCultistLeaderPlate@*': tc('cultist_leader_helmet'),
    'ConfigItems.itemHelmetCultistRobe@*': tc('cultist_robe_helmet'),
    'ConfigItems.itemBootsCultist@*': tc('cultist_boots'),
    'ConfigBlocks.blockEldritch@*': tc('ancient_doorway', 'ancient_lock', 'crusted_opening', 'runed_stone'),
    'ConfigBlocks.blockEldritch@4': tc('glowing_crusted_stone'),
    'ConfigBlocks.blockEldritch@5': tc('glyphed_stone'),
    # мета 6 в оригинале не встречается ни в мире, ни в рецептах
    'ConfigBlocks.blockEldritch@6': [],
    'ConfigBlocks.blockCosmeticSolid@11': tc('ancient_stone', 'ancient_stone_sealed', 'ancient_stone_stairs', 'ancient_pedestal'),
    'ConfigBlocks.blockCosmeticSolid@12': tc('ancient_rock'),
    'ConfigItems.itemEldritchObject@1': tc('crimson_rites'),
    'ConfigItems.itemEldritchObject@2': tc('runed_tablet'),
    'ConfigItems.itemEldritchObject@3': tc('primordial_pearl'),
    'ConfigItems.itemNuggetBeef@*': tc('nugget_beef'),
    'ConfigItems.itemNuggetChicken@*': tc('nugget_chicken'),
    'ConfigItems.itemNuggetPork@*': tc('nugget_pork'),
    'ConfigItems.itemNuggetFish@*': tc('nugget_fish'),
    'ConfigItems.itemTripleMeatTreat@*': tc('triple_meat_treat'),
    'ConfigBlocks.blockWoodenDevice@8': tc('thaum_banner'),
    'ConfigItems.itemNugget@21': tc('cluster_cinnabar'),
    'ConfigItems.itemNugget@17': tc('cluster_copper'),
    'ConfigItems.itemNugget@31': tc('cluster_gold'),
    'ConfigBlocks.blockMetalDevice': tc('crucible'),
    'ConfigBlocks.blockCosmeticSolid@6': tc('arcane_stone'),
    'ConfigBlocks.blockCosmeticSolid@7': tc('arcane_stone_bricks'),
    'ConfigItems.itemResource@14': tc('salis_mundus'),
    'Items.cauldron@*': mc('cauldron'),  # только как основа для тигля
    'ConfigItems.itemGoggles@*': tc('goggles'),
}

# Блоки без предмета: аспекты привязываются к самому блоку и нужны при сканировании.
BLOCKS = {
    'ConfigBlocks.blockEldritch@3': tc('eldritch_capstone'),
    'ConfigBlocks.blockEldritchPortal': tc('eldritch_portal'),
    'Blocks.fire@*': mc('fire', 'soul_fire'),
    'Blocks.water@*': mc('water'),
    'Blocks.lava@*': mc('lava'),
    'Blocks.portal@*': mc('nether_portal'),
    'Blocks.end_portal@*': mc('end_portal'),
}

# Зажжённые варианты в 1.20.1 — состояния того же блока.
SKIP = {'Blocks.lit_redstone_ore', 'Blocks.lit_furnace@*'}

# Старые имена существ -> id 1.20.1. Ключ "Имя|nbt" — вариант с условием по NBT в оригинале.
ENTITIES = {
    'Zombie': mc('zombie', 'husk', 'drowned', 'zombie_villager'),
    'Giant': mc('giant'),
    'Skeleton': mc('skeleton', 'stray'),
    'Skeleton|SkeletonType': mc('wither_skeleton'),
    'Creeper': mc('creeper'),
    'Creeper|powered': [('minecraft:creeper', '{powered:1b}')],
    'EntityHorse': mc('horse', 'donkey', 'mule'),
    'Pig': mc('pig'),
    'XPOrb': mc('experience_orb'),
    'Sheep': mc('sheep'),
    'Cow': mc('cow'),
    'MushroomCow': mc('mooshroom'),
    'SnowMan': mc('snow_golem'),
    'Ozelot': mc('ocelot', 'cat'),
    'Chicken': mc('chicken'),
    'Squid': mc('squid'),
    'Wolf': mc('wolf'),
    'Bat': mc('bat'),
    'Boat': mc('boat', 'chest_boat'),
    'Spider': mc('spider'),
    'Slime': mc('slime'),
    'Ghast': mc('ghast'),
    'PigZombie': mc('zombified_piglin'),
    'Enderman': mc('enderman'),
    'CaveSpider': mc('cave_spider'),
    'Silverfish': mc('silverfish'),
    'Blaze': mc('blaze'),
    'LavaSlime': mc('magma_cube'),
    'EnderDragon': mc('ender_dragon'),
    'WitherBoss': mc('wither'),
    'Witch': mc('witch'),
    'Villager': mc('villager'),
    'VillagerGolem': mc('iron_golem'),
    'MinecartRideable': mc('minecart'),
    'MinecartChest': mc('chest_minecart'),
    'MinecartFurnace': mc('furnace_minecart'),
    'MinecartTNT': mc('tnt_minecart'),
    'MinecartHopper': mc('hopper_minecart'),
    'MinecartSpawner': mc('spawner_minecart'),
    'EnderCrystal': mc('end_crystal'),
    'ItemFrame': mc('item_frame', 'glow_item_frame'),
    'Painting': mc('painting'),
    # существа самого мода
    'Thaumcraft.Firebat': ['thaumcraft:fire_bat'],
    'Thaumcraft.MindSpider': ['thaumcraft:mind_spider'],
    'Thaumcraft.ThaumSlime': ['thaumcraft:thaumic_slime'],
    'Thaumcraft.Taintacle': ['thaumcraft:taintacle'],
    'Thaumcraft.TaintacleTiny': ['thaumcraft:taintacle_small'],
    'Thaumcraft.TaintSpider': ['thaumcraft:taint_spider'],
    'Thaumcraft.TaintSpore': ['thaumcraft:taint_spore'],
    'Thaumcraft.TaintSwarmer': ['thaumcraft:taint_spore_swarmer'],
    'Thaumcraft.TaintSwarm': ['thaumcraft:taint_swarm'],
    'Thaumcraft.TaintedPig': ['thaumcraft:tainted_pig'],
    'Thaumcraft.TaintedSheep': ['thaumcraft:tainted_sheep'],
    'Thaumcraft.TaintedCow': ['thaumcraft:tainted_cow'],
    'Thaumcraft.TaintedChicken': ['thaumcraft:tainted_chicken'],
    'Thaumcraft.TaintedVillager': ['thaumcraft:tainted_villager'],
    'Thaumcraft.TaintedCreeper': ['thaumcraft:tainted_creeper'],
    'Thaumcraft.PrimalOrb': ['thaumcraft:primal_orb'],
    'Thaumcraft.EldritchGuardian': ['thaumcraft:eldritch_guardian'],
    'Thaumcraft.EldritchOrb': ['thaumcraft:eldritch_orb'],
    'Thaumcraft.CultistKnight': ['thaumcraft:cultist_knight'],
    'Thaumcraft.CultistCleric': ['thaumcraft:cultist_cleric'],
    'Thaumcraft.BrainyZombie': ['thaumcraft:brainy_zombie'],
    'Thaumcraft.GiantBrainyZombie': ['thaumcraft:giant_brainy_zombie'],
}

# Записи, которые в оригинале заданы кодом, а не одной строкой.
EXTRA = [
    {'item': 'minecraft:enchanted_book', 'base': 'minecraft:book', 'aspects': {}},
    # в 1.7.10 это были красители (dyeXxx = sensus 1); теперь они отдельные предметы и в тег красителей не входят
    {'item': 'minecraft:bone_meal', 'aspects': {'sensus': 1}},
    {'item': 'minecraft:ink_sac', 'aspects': {'sensus': 1}},
    {'item': 'minecraft:cocoa_beans', 'aspects': {'sensus': 1}},
    {'item': 'minecraft:lapis_lazuli', 'aspects': {'sensus': 1}},
]


def parse():
    text = open(SOURCE, encoding='utf-8').read()
    text = text[text.index('registerItemAspects() {'):]
    rows = []
    for line in text.splitlines():
        line = line.strip()
        m = re.match(r'ThaumcraftApi\.register(Complex)?ObjectTag\((.*?), \(new AspectList\((.*?)\)\)(.*)\);$', line)
        if not m:
            continue
        key = m.group(2).replace('new ItemStack(', '').rstrip(')').replace(', 1, 32767', '@*').replace(', 1, ', '@')
        base = m.group(3).replace('new ItemStack(', '').replace(')', '').replace(', 1, 32767', '@*')
        ops = re.findall(r'\.(add|remove|merge)\(Aspect\.(\w+)(?:, (\d+))?\)', m.group(4))
        rows.append((key, base, bool(m.group(1)), ops))
    return rows


def apply_ops(aspects, ops):
    for op, name, amount in ops:
        tag = LATIN[name]
        if op == 'remove':
            if amount:
                left = aspects.get(tag, 0) - int(amount)
                if left > 0:
                    aspects[tag] = left
                else:
                    aspects.pop(tag, None)
            else:
                aspects.pop(tag, None)
        elif op == 'merge':
            aspects[tag] = max(aspects.get(tag, 0), int(amount))
        else:
            aspects[tag] = aspects.get(tag, 0) + int(amount)
    return aspects


def main():
    entries = []
    known = {}      # старый ключ -> аспекты, для записей вида "как у X, но ..."
    unmapped = []
    for key, base, is_complex, ops in parse():
        start = {}
        inherit = None
        if base:
            start = dict(known.get(base) or known.get(base + '@*') or {})
            base_targets = MAP.get(base) or MAP.get(base + '@*') or []
            if len(base_targets) == 1 and not base_targets[0].startswith('#'):
                # аспекты базы выводятся в игре (в т.ч. из рецепта), поэтому наследуем, а не считаем заранее
                inherit = base_targets[0]
        aspects = apply_ops(start, ops)
        known[key] = aspects
        if key in SKIP:
            continue
        if key in BLOCKS:
            for target in BLOCKS[key]:
                entries.append({'block': target, 'aspects': aspects})
            continue
        targets = MAP.get(key)
        if targets is None:
            unmapped.append(key)
            continue
        for target in targets:
            entry = {'tag': target[1:]} if target.startswith('#') else {'item': target}
            if is_complex:
                entry['complex'] = True
            if inherit:
                entry['base'] = inherit
                removed = {LATIN[n]: int(c) if c else 0 for o, n, c in ops if o == 'remove'}
                if removed:
                    entry['remove'] = removed
                entry['aspects'] = apply_ops({}, [o for o in ops if o[0] != 'remove'])
            else:
                entry['aspects'] = aspects
            entries.append(entry)
    entries.extend(EXTRA)

    text = open(SOURCE, encoding='utf-8').read()
    for line in text.splitlines():
        m = re.match(r'\s*ThaumcraftApi\.registerEntityTag\("([^"]+)", \(new AspectList\(\)\)(.*)\);$', line)
        if not m:
            continue
        nbt = re.search(r'EntityTagsNBT\("(\w+)"', m.group(2))
        key = m.group(1) + ('|' + nbt.group(1) if nbt else '')
        ops = re.findall(r'\.(add|remove|merge)\(Aspect\.(\w+)(?:, (\d+))?\)', m.group(2))
        targets = ENTITIES.get(key)
        if targets is None:
            unmapped.append('entity ' + key)
            continue
        for target in targets:
            entry = {'entity': target} if isinstance(target, str) else {'entity': target[0], 'nbt': target[1]}
            entry['aspects'] = apply_ops({}, ops)
            entries.append(entry)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as fh:
        fh.write('{\n  "entries": [\n')
        fh.write(',\n'.join('    ' + json.dumps(e) for e in entries))
        fh.write('\n  ]\n}\n')
    print('записей:', len(entries))
    print('без соответствия (%d):' % len(unmapped))
    for key in unmapped:
        print('  ', key)


if __name__ == '__main__':
    main()

"""Переносит ассеты оригинального Thaumcraft 4 из reference/tc4/src в src/main/resources.

Запуск из корня проекта:  python tools/import_assets.py

- текстуры: items -> item, blocks -> block (так требует атлас 1.20.1), имена в нижний регистр
- звуки: .ogg копируются как есть, sounds.json переводится в новый формат
- локализация: .lang -> .json; оригинальные ключи сохраняются, новые id получают перевод через KEYMAP
"""
import json
import os
import re
import shutil
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'reference', 'tc4', 'src', 'assets', 'thaumcraft')
DST = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'thaumcraft')

# новый ключ перевода -> ключ из оригинального .lang
KEYMAP = {
    'item.thaumcraft.shard_air': 'item.ItemShard.0.name',
    'item.thaumcraft.shard_fire': 'item.ItemShard.1.name',
    'item.thaumcraft.shard_water': 'item.ItemShard.2.name',
    'item.thaumcraft.shard_earth': 'item.ItemShard.3.name',
    'item.thaumcraft.shard_order': 'item.ItemShard.4.name',
    'item.thaumcraft.shard_entropy': 'item.ItemShard.5.name',
    'item.thaumcraft.shard_balanced': 'item.ItemShard.6.name',
    'item.thaumcraft.thaumometer': 'item.ItemThaumometer.name',
    'item.thaumcraft.goggles': 'item.ItemGoggles.name',
    'block.thaumcraft.crystal_air': 'tile.blockCrystal.0.name',
    'block.thaumcraft.crystal_fire': 'tile.blockCrystal.1.name',
    'block.thaumcraft.crystal_water': 'tile.blockCrystal.2.name',
    'block.thaumcraft.crystal_earth': 'tile.blockCrystal.3.name',
    'block.thaumcraft.crystal_order': 'tile.blockCrystal.4.name',
    'block.thaumcraft.crystal_entropy': 'tile.blockCrystal.5.name',
    'block.thaumcraft.crystal_mixed': 'tile.blockCrystal.6.name',
    'block.thaumcraft.wand_pedestal': 'tile.blockStoneDevice.5.name',
    'item.thaumcraft.coin': 'item.ItemResource.18.name',
    'block.thaumcraft.infernal_furnace': 'tile.blockArcaneFurnace.name',
    'item.thaumcraft.void_seed': 'item.ItemResource.17.name',
    'block.thaumcraft.ancient_lock': 'tile.blockEldritch.8.name',
    'block.thaumcraft.aura_node': 'tile.blockAiry.0.name',
    'block.thaumcraft.strange_crystals': 'tile.blockCrystal.7.name',
    'block.thaumcraft.eldritch_portal': 'tile.blockPortalEldritch.name',
    'block.thaumcraft.impassable': 'tile.blockAiry.12.name',
    'block.thaumcraft.sapping_field': 'tile.blockAiry.11.name',
    'entity.thaumcraft.cultist_portal': 'entity.Thaumcraft.CultistPortal.name',
    'entity.thaumcraft.taintacle_giant': 'entity.Thaumcraft.TaintacleGiant.name',
    'block.thaumcraft.eldritch_altar': 'tile.blockEldritch.0.name',
    'block.thaumcraft.eldritch_obelisk': 'tile.blockEldritch.1.name',
    'block.thaumcraft.eldritch_obelisk_segment': 'tile.blockEldritch.2.name',
    'block.thaumcraft.eldritch_capstone': 'tile.blockEldritch.3.name',
    'block.thaumcraft.glowing_crusted_stone': 'tile.blockEldritch.4.name',
    'block.thaumcraft.glyphed_stone': 'tile.blockEldritch.5.name',
    'block.thaumcraft.ancient_doorway': 'tile.blockEldritch.7.name',
    'block.thaumcraft.crusted_opening': 'tile.blockEldritch.9.name',
    'block.thaumcraft.runed_stone': 'tile.blockEldritch.10.name',
    'block.thaumcraft.ancient_stone': 'tile.blockCosmeticSolid.11.name',
    'block.thaumcraft.ancient_rock': 'tile.blockCosmeticSolid.12.name',
    'block.thaumcraft.ancient_stone_sealed': 'tile.blockCosmeticSolid.13.name',
    'block.thaumcraft.crusted_stone': 'tile.blockCosmeticSolid.14.name',
    'block.thaumcraft.ancient_pedestal': 'tile.blockCosmeticSolid.15.name',
    'block.thaumcraft.ancient_stone_stairs': 'tile.blockStairsEldritch.name',
    'block.thaumcraft.ancient_stone_slab': 'tile.blockCosmeticSlabStone.eldritch.name',
    'entity.thaumcraft.eldritch_guardian': 'entity.Thaumcraft.EldritchGuardian.name',
    'entity.thaumcraft.eldritch_orb': 'entity.Thaumcraft.EldritchOrb.name',
    'item.thaumcraft.cultist_robe_helmet': 'item.ItemHelmetCultistRobe.name',
    'item.thaumcraft.cultist_robe_chestplate': 'item.ItemChestplateCultistRobe.name',
    'item.thaumcraft.cultist_robe_leggings': 'item.ItemLeggingsCultistRobe.name',
    'item.thaumcraft.cultist_plate_helmet': 'item.ItemHelmetCultistPlate.name',
    'item.thaumcraft.cultist_plate_chestplate': 'item.ItemChestplateCultistPlate.name',
    'item.thaumcraft.cultist_plate_leggings': 'item.ItemLeggingsCultistPlate.name',
    'item.thaumcraft.cultist_leader_helmet': 'item.ItemHelmetCultistLeaderPlate.name',
    'item.thaumcraft.cultist_leader_chestplate': 'item.ItemChestplateCultistLeaderPlate.name',
    'item.thaumcraft.cultist_leader_leggings': 'item.ItemLeggingsCultistLeaderPlate.name',
    'item.thaumcraft.cultist_boots': 'item.ItemBootsCultist.name',
    'item.thaumcraft.crimson_blade': 'item.ItemSwordCrimson.name',
    'entity.thaumcraft.golem_orb': 'entity.Thaumcraft.GolemOrb.name',
    'entity.thaumcraft.eldritch_crab': 'entity.Thaumcraft.EldritchCrab.name',
    'entity.thaumcraft.inhabited_zombie': 'entity.Thaumcraft.InhabitedZombie.name',
    'entity.thaumcraft.cultist_knight': 'entity.Thaumcraft.CultistKnight.name',
    'entity.thaumcraft.cultist_cleric': 'entity.Thaumcraft.CultistCleric.name',
    'block.thaumcraft.node_jar': 'tile.blockJar.2.name',
    'block.thaumcraft.advanced_alchemical_construct': 'tile.blockMetalDevice.3.name',
    'block.thaumcraft.advanced_alchemy_furnace': 'tile.blockAlchemyFurnace.name',
    'block.thaumcraft.essentia_reservoir': 'tile.blockEssentiaReservoir.name',
    'item.thaumcraft.eldritch_eye': 'item.ItemEldritchObject.0.name',
    'item.thaumcraft.crimson_rites': 'item.ItemEldritchObject.1.name',
    'item.thaumcraft.runed_tablet': 'item.ItemEldritchObject.2.name',
    'item.thaumcraft.primal_crusher': 'item.ItemPrimalCrusher.name',
    'item.thaumcraft.primordial_pearl': 'item.ItemEldritchObject.3.name',
    'item.thaumcraft.obelisk_placer': 'item.ItemEldritchObject.4.name',
    'item.thaumcraft.void_robe_helmet': 'item.ItemHelmetVoidRobe.name',
    'item.thaumcraft.void_robe_chestplate': 'item.ItemChestplateVoidRobe.name',
    'item.thaumcraft.void_robe_leggings': 'item.ItemLeggingsVoidRobe.name',
    'item.thaumcraft.sanity_checker': 'item.ItemSanityChecker.name',
    'item.thaumcraft.wand_cap_void': 'item.WandCap.7.name',
    'item.thaumcraft.wand_cap_void_inert': 'item.WandCap.8.name',
    'item.thaumcraft.void_metal_ingot': 'item.ItemResource.16.name',
    'item.thaumcraft.void_metal_nugget': 'item.ItemNugget.7.name',
    'item.thaumcraft.void_sword': 'item.ItemSwordVoid.name',
    'item.thaumcraft.void_shovel': 'item.ItemShovelVoid.name',
    'item.thaumcraft.void_pickaxe': 'item.ItemPickVoid.name',
    'item.thaumcraft.void_axe': 'item.ItemAxeVoid.name',
    'item.thaumcraft.void_hoe': 'item.ItemHoeVoid.name',
    'item.thaumcraft.void_helmet': 'item.ItemHelmetVoid.name',
    'item.thaumcraft.void_chestplate': 'item.ItemChestplateVoid.name',
    'item.thaumcraft.void_leggings': 'item.ItemLeggingsVoid.name',
    'item.thaumcraft.void_boots': 'item.ItemBootsVoid.name',
    'block.thaumcraft.obsidian_totem': 'tile.blockCosmeticSolid.0.name',
    'block.thaumcraft.obsidian_tile': 'tile.blockCosmeticSolid.1.name',
    'block.thaumcraft.charged_obsidian_totem': 'tile.blockCosmeticSolid.8.name',
    'block.thaumcraft.loot_urn_common': 'tile.blockLootUrn.name',
    'block.thaumcraft.loot_urn_uncommon': 'tile.blockLootUrn.name',
    'block.thaumcraft.loot_urn_rare': 'tile.blockLootUrn.name',
    'block.thaumcraft.loot_crate_common': 'tile.blockLootCrate.name',
    'block.thaumcraft.loot_crate_uncommon': 'tile.blockLootCrate.name',
    'block.thaumcraft.loot_crate_rare': 'tile.blockLootCrate.name',
    'item.thaumcraft.nugget_chicken': 'item.ItemNuggetChicken.name',
    'item.thaumcraft.nugget_beef': 'item.ItemNuggetBeef.name',
    'item.thaumcraft.nugget_pork': 'item.ItemNuggetPork.name',
    'item.thaumcraft.nugget_fish': 'item.ItemNuggetFish.name',
    'item.thaumcraft.triple_meat_treat': 'item.TripleMeatTreat.name',
    'item.thaumcraft.enchanted_fabric': 'item.ItemResource.7.name',
    'item.thaumcraft.apprentice_ring': 'item.ItemBaubleBlanks.3.name',
    'item.thaumcraft.runic_ring_lesser': 'item.ItemRingRunic.0.name',
    'item.thaumcraft.runic_ring': 'item.ItemRingRunic.1.name',
    'item.thaumcraft.runic_ring_charged': 'item.ItemRingRunic.2.name',
    'item.thaumcraft.runic_ring_healing': 'item.ItemRingRunic.3.name',
    'item.thaumcraft.runic_amulet': 'item.ItemAmuletRunic.0.name',
    'item.thaumcraft.runic_amulet_emergency': 'item.ItemAmuletRunic.1.name',
    'item.thaumcraft.runic_girdle': 'item.ItemGirdleRunic.0.name',
    'item.thaumcraft.runic_girdle_kinetic': 'item.ItemGirdleRunic.1.name',
    'item.thaumcraft.knowledge_fragment': 'item.ItemResource.9.name',
    'item.thaumcraft.loot_bag_common': 'item.ItemLootBag.0.name',
    'item.thaumcraft.loot_bag_uncommon': 'item.ItemLootBag.1.name',
    'item.thaumcraft.loot_bag_rare': 'item.ItemLootBag.2.name',
    'block.thaumcraft.vis_charge_relay': 'tile.blockMetalDevice.2.name',
    'block.thaumcraft.wand_pedestal_focus': 'tile.blockStoneDevice.8.name',
    'item.thaumcraft.vis_stone': 'item.ItemAmuletVis.0.name',
    'item.thaumcraft.vis_amulet': 'item.ItemAmuletVis.1.name',
    'item.thaumcraft.thaumonomicon': 'item.ItemThaumonomicon.name',
    'item.thaumcraft.scribing_tools': 'item.ItemInkwell.name',
    'item.thaumcraft.research_notes': 'item.researchnotes.name',
    'block.thaumcraft.research_table': 'tile.blockTable.research.name',
    'item.thaumcraft.amber': 'item.ItemResource.6.name',
    'item.thaumcraft.wand_cap_iron': 'item.WandCap.0.name',
    'item.thaumcraft.wand_cap_gold': 'item.WandCap.1.name',
    'item.thaumcraft.wand_rod_greatwood': 'item.WandRod.0.name',
    'block.thaumcraft.table': 'tile.blockTable.0.name',
    'block.thaumcraft.deconstruction_table': 'tile.blockTable.14.name',
    'block.thaumcraft.arcane_workbench': 'tile.blockTable.15.name',
    'item.thaumcraft.wand': 'item.Wand.wand.obj',
    'item.thaumcraft.quicksilver': 'item.ItemResource.3.name',
    'item.thaumcraft.quicksilver_nugget': 'item.ItemNugget.5.name',
    'block.thaumcraft.cinnabar_ore': 'tile.blockCustomOre.0.name',
    'block.thaumcraft.infused_stone_air': 'tile.blockCustomOre.1.name',
    'block.thaumcraft.infused_stone_fire': 'tile.blockCustomOre.2.name',
    'block.thaumcraft.infused_stone_water': 'tile.blockCustomOre.3.name',
    'block.thaumcraft.infused_stone_earth': 'tile.blockCustomOre.4.name',
    'block.thaumcraft.infused_stone_order': 'tile.blockCustomOre.5.name',
    'block.thaumcraft.infused_stone_entropy': 'tile.blockCustomOre.6.name',
    'block.thaumcraft.amber_ore': 'tile.blockCustomOre.7.name',
    'block.thaumcraft.greatwood_log': 'tile.blockMagicalLog.greatwood.name',
    'block.thaumcraft.silverwood_log': 'tile.blockMagicalLog.silverwood.name',
    'block.thaumcraft.greatwood_leaves': 'tile.blockMagicalLeaves.greatwood.name',
    'block.thaumcraft.silverwood_leaves': 'tile.blockMagicalLeaves.silverwood.name',
    'block.thaumcraft.greatwood_planks': 'tile.blockWoodenDevice.6.name',
    'block.thaumcraft.silverwood_planks': 'tile.blockWoodenDevice.7.name',
    'block.thaumcraft.greatwood_sapling': 'tile.blockCustomPlant.0.name',
    'block.thaumcraft.silverwood_sapling': 'tile.blockCustomPlant.1.name',
    'block.thaumcraft.shimmerleaf': 'tile.blockCustomPlant.2.name',
    'block.thaumcraft.tallow_candle_white': 'tile.blockCandle.0.name',
    'block.thaumcraft.tallow_candle_orange': 'tile.blockCandle.1.name',
    'block.thaumcraft.tallow_candle_magenta': 'tile.blockCandle.2.name',
    'block.thaumcraft.tallow_candle_light_blue': 'tile.blockCandle.3.name',
    'block.thaumcraft.tallow_candle_yellow': 'tile.blockCandle.4.name',
    'block.thaumcraft.tallow_candle_lime': 'tile.blockCandle.5.name',
    'block.thaumcraft.tallow_candle_pink': 'tile.blockCandle.6.name',
    'block.thaumcraft.tallow_candle_gray': 'tile.blockCandle.7.name',
    'block.thaumcraft.tallow_candle_light_gray': 'tile.blockCandle.8.name',
    'block.thaumcraft.tallow_candle_cyan': 'tile.blockCandle.9.name',
    'block.thaumcraft.tallow_candle_purple': 'tile.blockCandle.10.name',
    'block.thaumcraft.tallow_candle_blue': 'tile.blockCandle.11.name',
    'block.thaumcraft.tallow_candle_brown': 'tile.blockCandle.12.name',
    'block.thaumcraft.tallow_candle_green': 'tile.blockCandle.13.name',
    'block.thaumcraft.tallow_candle_red': 'tile.blockCandle.14.name',
    'block.thaumcraft.tallow_candle_black': 'tile.blockCandle.15.name',
    'block.thaumcraft.vishroom': 'tile.blockCustomPlant.5.name',
    'item.thaumcraft.mana_bean': 'item.ItemManaBean.name',
    'block.thaumcraft.silverwood_knot': 'tile.blockMagicalLog.silverwood.name',
    'block.thaumcraft.cinderpearl': 'tile.blockCustomPlant.3.name',
    'block.thaumcraft.crucible': 'tile.blockMetalDevice.0.name',
    'block.thaumcraft.flux_goo': 'tile.blockFluxGoo.name',
    'block.thaumcraft.liquid_death': 'tile.blockFluidDeath.name',
    'block.thaumcraft.purifying_fluid': 'tile.blockFluidPure.name',
    'block.thaumcraft.arcane_spa': 'tile.blockStoneDevice.12.name',
    'item.thaumcraft.bath_salts': 'item.ItemBathSalts.name',
    'item.thaumcraft.bucket_death': 'item.ItemBucketDeath.name',
    'item.thaumcraft.bucket_pure': 'item.ItemBucketPure.name',
    'block.thaumcraft.taint_crust': 'tile.blockTaint.0.name',
    'block.thaumcraft.tainted_soil': 'tile.blockTaint.1.name',
    'block.thaumcraft.flesh_block': 'tile.blockTaint.2.name',
    'block.thaumcraft.taint_fibres': 'tile.blockTaintFibres.0.name',
    'block.thaumcraft.taint_grass': 'tile.blockTaintFibres.1.name',
    'block.thaumcraft.taint_plant': 'tile.blockTaintFibres.2.name',
    'block.thaumcraft.spore_stalk': 'tile.blockTaintFibres.3.name',
    'block.thaumcraft.arcane_stone': 'tile.blockCosmeticSolid.6.name',
    'block.thaumcraft.arcane_stone_bricks': 'tile.blockCosmeticSolid.7.name',
    'block.thaumcraft.pedestal': 'tile.blockStoneDevice.1.name',
    'block.thaumcraft.infusion_matrix': 'tile.blockStoneDevice.2.name',
    'block.thaumcraft.infusion_pillar': 'tile.blockStoneDevice.3.name',
    'item.thaumcraft.salis_mundus': 'item.ItemResource.14.name',
    'item.thaumcraft.wand_rod_silverwood': 'item.WandRod.2.name',
    'item.thaumcraft.wand_rod_obsidian': 'item.WandRod.1.name',
    'item.thaumcraft.wand_rod_ice': 'item.WandRod.3.name',
    'item.thaumcraft.wand_rod_quartz': 'item.WandRod.4.name',
    'item.thaumcraft.wand_rod_reed': 'item.WandRod.5.name',
    'item.thaumcraft.wand_rod_blaze': 'item.WandRod.6.name',
    'item.thaumcraft.wand_rod_bone': 'item.WandRod.7.name',
    'item.thaumcraft.wand_cap_thaumium': 'item.WandCap.2.name',
    'item.thaumcraft.wand_cap_copper': 'item.WandCap.3.name',
    'item.thaumcraft.copper_nugget': 'item.ItemNugget.1.name',
    'item.thaumcraft.primal_charm': 'item.ItemResource.15.name',
    'item.thaumcraft.focus_fire': 'item.FocusFire.name',
    'block.thaumcraft.focal_manipulator': 'tile.blockStoneDevice.13.name',
    'block.thaumcraft.arcane_stone_slab': 'tile.blockCosmeticSlabStone.arcane.name',
    'block.thaumcraft.arcane_stone_stairs': 'tile.blockStairsArcaneStone.name',
    'block.thaumcraft.greatwood_stairs': 'tile.blockStairsGreatwood.name',
    'block.thaumcraft.silverwood_stairs': 'tile.blockStairsSilverwood.name',
    'block.thaumcraft.greatwood_slab': 'tile.blockCosmeticSlabWood.greatwood.name',
    'block.thaumcraft.silverwood_slab': 'tile.blockCosmeticSlabWood.silverwood.name',
    'entity.thaumcraft.aspect_orb': 'entity.Thaumcraft.AspectOrb.name',
    'block.thaumcraft.node_stabilizer': 'tile.blockStoneDevice.9.name',
    'block.thaumcraft.advanced_node_stabilizer': 'tile.blockStoneDevice.10.name',
    'block.thaumcraft.node_transducer': 'tile.blockStoneDevice.11.name',
    'block.thaumcraft.vis_relay': 'tile.blockMetalDevice.14.name',
    'item.thaumcraft.focus_warding': 'item.FocusWarding.name',
    'item.thaumcraft.focus_trade': 'item.FocusTrade.name',
    'item.thaumcraft.focus_hellbat': 'item.FocusHellbat.name',
    'entity.thaumcraft.fire_bat': 'entity.Thaumcraft.Firebat.name',
    'item.thaumcraft.focus_portable_hole': 'item.FocusPortableHole.name',
    'item.thaumcraft.focus_pech': 'item.FocusPech.name',
    'item.thaumcraft.focus_primal': 'item.FocusPrimal.name',
    'item.thaumcraft.focus_excavation': 'item.FocusExcavation.name',
    'item.thaumcraft.focus_pouch': 'item.FocusPouch.name',
    'item.thaumcraft.bauble_amulet': 'item.ItemBaubleBlanks.0.name',
    'item.thaumcraft.bauble_ring': 'item.ItemBaubleBlanks.1.name',
    'item.thaumcraft.bauble_belt': 'item.ItemBaubleBlanks.2.name',
    'item.thaumcraft.focus_shock': 'item.FocusShock.name',
    'item.thaumcraft.focus_frost': 'item.FocusFrost.name',
    'item.thaumcraft.wand_rod_greatwood_staff': 'item.WandRod.50.name',
    'item.thaumcraft.wand_rod_obsidian_staff': 'item.WandRod.51.name',
    'item.thaumcraft.wand_rod_silverwood_staff': 'item.WandRod.52.name',
    'item.thaumcraft.wand_rod_ice_staff': 'item.WandRod.53.name',
    'item.thaumcraft.wand_rod_quartz_staff': 'item.WandRod.54.name',
    'item.thaumcraft.wand_rod_reed_staff': 'item.WandRod.55.name',
    'item.thaumcraft.wand_rod_blaze_staff': 'item.WandRod.56.name',
    'item.thaumcraft.wand_rod_bone_staff': 'item.WandRod.57.name',
    'item.thaumcraft.wand_rod_primal_staff': 'item.WandRod.100.name',
    'item.thaumcraft.wand_cap_thaumium_inert': 'item.WandCap.6.name',
    'block.thaumcraft.alchemical_furnace': 'tile.blockStoneDevice.0.name',
    'block.thaumcraft.alembic': 'tile.blockMetalDevice.1.name',
    'block.thaumcraft.warded_jar': 'tile.blockJar.0.name',
    'block.thaumcraft.brain_jar': 'tile.blockJar.1.name',
    'block.thaumcraft.void_jar': 'tile.blockJar.3.name',
    'block.thaumcraft.ethereal_bloom': 'tile.blockCustomPlant.4.name',
    'item.thaumcraft.resonator': 'item.ItemResonator.name',
    'item.thaumcraft.sanity_soap': 'item.ItemSanitySoap.name',
    'item.thaumcraft.phial': 'item.ItemEssence.0.name',
    'item.thaumcraft.phial.filled': 'item.ItemEssence.1.name',
    'item.thaumcraft.filter': 'item.ItemResource.8.name',
    'item.thaumcraft.label': 'item.ItemResource.13.name',
    'block.thaumcraft.tube': 'tile.blockTube.0.name',
    'block.thaumcraft.tube_valve': 'tile.blockTube.1.name',
    'block.thaumcraft.tube_filter': 'tile.blockTube.3.name',
    'block.thaumcraft.tube_restrict': 'tile.blockTube.5.name',
    'block.thaumcraft.tube_oneway': 'tile.blockTube.6.name',
    'block.thaumcraft.tube_buffer': 'tile.blockTube.4.name',
    'block.thaumcraft.centrifuge': 'tile.blockTube.2.name',
    'block.thaumcraft.alchemical_construct': 'tile.blockMetalDevice.9.name',
    'block.thaumcraft.tallow_block': 'tile.blockCosmeticSolid.5.name',
    'entity.thaumcraft.golem': 'entity.Thaumcraft.Golem.name',
    'block.thaumcraft.golem_fetter': 'tile.blockCosmeticSolid.9.name',
    'block.thaumcraft.hungry_chest': 'tile.blockChestHungry.name',
    'block.thaumcraft.arcane_lamp': 'tile.blockMetalDevice.7.name',
    'block.thaumcraft.travel_paving': 'tile.blockCosmeticSolid.2.name',
    'block.thaumcraft.warding_paving': 'tile.blockCosmeticSolid.3.name',
    'block.thaumcraft.ward_aura': 'tile.blockAiry.4.name',
    'block.thaumcraft.grate': 'tile.blockMetalDevice.5.name',
    'block.thaumcraft.arcane_bore_base': 'tile.blockWoodenDevice.4.name',
    'block.thaumcraft.arcane_bore': 'tile.blockWoodenDevice.5.name',
    'block.thaumcraft.thaum_banner': 'tile.blockWoodenDevice.8.name',
    'block.thaumcraft.thaum_banner.0': 'tile.blockWoodenDevice.8.0.name',
    'block.thaumcraft.thaum_banner.1': 'tile.blockWoodenDevice.8.1.name',
    'block.thaumcraft.thaum_banner.2': 'tile.blockWoodenDevice.8.2.name',
    'block.thaumcraft.thaum_banner.3': 'tile.blockWoodenDevice.8.3.name',
    'block.thaumcraft.thaum_banner.4': 'tile.blockWoodenDevice.8.4.name',
    'block.thaumcraft.thaum_banner.5': 'tile.blockWoodenDevice.8.5.name',
    'block.thaumcraft.thaum_banner.6': 'tile.blockWoodenDevice.8.6.name',
    'block.thaumcraft.thaum_banner.7': 'tile.blockWoodenDevice.8.7.name',
    'block.thaumcraft.thaum_banner.8': 'tile.blockWoodenDevice.8.8.name',
    'block.thaumcraft.thaum_banner.9': 'tile.blockWoodenDevice.8.9.name',
    'block.thaumcraft.thaum_banner.10': 'tile.blockWoodenDevice.8.10.name',
    'block.thaumcraft.thaum_banner.11': 'tile.blockWoodenDevice.8.11.name',
    'block.thaumcraft.thaum_banner.12': 'tile.blockWoodenDevice.8.12.name',
    'block.thaumcraft.thaum_banner.13': 'tile.blockWoodenDevice.8.13.name',
    'block.thaumcraft.thaum_banner.14': 'tile.blockWoodenDevice.8.14.name',
    'block.thaumcraft.thaum_banner.15': 'tile.blockWoodenDevice.8.15.name',

    'block.thaumcraft.flux_scrubber': 'tile.blockStoneDevice.14.name',
    'item.thaumcraft.sinister_stone': 'item.ItemCompassStone.name',
    'item.thaumcraft.traveller_boots': 'item.BootsTraveller.name',
    'item.thaumcraft.hover_harness': 'item.HoverHarness.name',
    'item.thaumcraft.hover_girdle': 'item.ItemGirdleHover.name',
    'item.thaumcraft.fortress_helmet': 'item.ItemHelmetFortress.name',
    'item.thaumcraft.robe_chestplate': 'item.ItemChestplateRobe.name',
    'item.thaumcraft.robe_leggings': 'item.ItemLeggingsRobe.name',
    'item.thaumcraft.robe_boots': 'item.ItemBootsRobe.name',
    'item.thaumcraft.fortress_chestplate': 'item.ItemChestplateFortress.name',
    'item.thaumcraft.fortress_leggings': 'item.ItemLeggingsFortress.name',
    'item.HelmetFortress.mask.0': 'item.HelmetFortress.mask.0',
    'item.HelmetFortress.mask.1': 'item.HelmetFortress.mask.1',
    'item.HelmetFortress.mask.2': 'item.HelmetFortress.mask.2',
    'item.thaumcraft.bone_bow': 'item.ItemBowBone.name',
    'item.thaumcraft.primal_arrow_air': 'item.ItemPrimalArrow.0.name',
    'item.thaumcraft.primal_arrow_fire': 'item.ItemPrimalArrow.1.name',
    'item.thaumcraft.primal_arrow_water': 'item.ItemPrimalArrow.2.name',
    'item.thaumcraft.primal_arrow_earth': 'item.ItemPrimalArrow.3.name',
    'item.thaumcraft.primal_arrow_order': 'item.ItemPrimalArrow.4.name',
    'item.thaumcraft.primal_arrow_entropy': 'item.ItemPrimalArrow.5.name',

    'block.thaumcraft.arcane_door': 'tile.blockArcaneDoor.name',
    'item.thaumcraft.arcane_door': 'item.ItemArcaneDoor.name',
    'block.thaumcraft.arcane_pressure_plate': 'tile.blockWoodenDevice.2.name',
    'block.thaumcraft.warded_glass': 'tile.blockCosmeticOpaque.2.name',
    'item.thaumcraft.key_iron': 'item.ArcaneDoorKey.0.name',
    'item.thaumcraft.key_gold': 'item.ArcaneDoorKey.1.name',
    'tc.key1': 'tc.key1',
    'tc.key2': 'tc.key2',
    'tc.key3': 'tc.key3',
    'tc.key4': 'tc.key4',
    'tc.key5': 'tc.key5',
    'tc.key6': 'tc.key6',
    'tc.key7': 'tc.key7',
    'tc.key8': 'tc.key8',
    'tc.key9': 'tc.key9',
    'tc.key10': 'tc.key10',
    'tc.key11': 'tc.key11',

    'block.thaumcraft.magic_mirror': 'tile.blockMirror.0.name',
    'block.thaumcraft.essentia_mirror': 'tile.blockMirror.6.name',
    'item.thaumcraft.hand_mirror': 'item.HandMirror.name',
    'item.thaumcraft.mirrored_glass': 'item.ItemResource.10.name',
    'thaumcraft.handmirror.linked': 'tc.handmirrorlinked',
    'thaumcraft.handmirror.error': 'tc.handmirrorerror',
    'thaumcraft.handmirror.linked_to': 'tc.handmirrorlinkedto',
    'block.thaumcraft.arcane_ear': 'tile.blockWoodenDevice.1.name',
    'block.thaumcraft.levitator': 'tile.blockLifter.name',
    'block.thaumcraft.lamp_growth': 'tile.blockMetalDevice.8.name',
    'block.thaumcraft.lamp_fertility': 'tile.blockMetalDevice.13.name',
    'block.thaumcraft.lamp_light': 'tile.blockAiry.3.name',
    'block.thaumcraft.amber_block': 'tile.blockCosmeticOpaque.0.name',
    'block.thaumcraft.amber_bricks': 'tile.blockCosmeticOpaque.1.name',
    'item.thaumcraft.traveling_trunk': 'item.TrunkSpawner.name',
    'entity.thaumcraft.traveling_trunk': 'entity.Thaumcraft.TravelingTrunk.name',
    'item.thaumcraft.golem_deco_tophat': 'item.ItemGolemDecoration.0.name',
    'item.thaumcraft.golem_deco_glasses': 'item.ItemGolemDecoration.1.name',
    'item.thaumcraft.golem_deco_bowtie': 'item.ItemGolemDecoration.2.name',
    'item.thaumcraft.golem_deco_fez': 'item.ItemGolemDecoration.3.name',
    'item.thaumcraft.golem_deco_dart': 'item.ItemGolemDecoration.4.name',
    'item.thaumcraft.golem_deco_visor': 'item.ItemGolemDecoration.5.name',
    'item.thaumcraft.golem_deco_armor': 'item.ItemGolemDecoration.6.name',
    'item.thaumcraft.golem_deco_mace': 'item.ItemGolemDecoration.7.name',
    'entity.thaumcraft.golem_bobber': 'entity.Thaumcraft.GolemBobber.name',
    'item.thaumcraft.golem_bell': 'item.GolemBell.name',
    'item.thaumcraft.golem_straw': 'item.ItemGolemPlacer.0.name',
    'item.thaumcraft.golem_wood': 'item.ItemGolemPlacer.1.name',
    'item.thaumcraft.golem_tallow': 'item.ItemGolemPlacer.2.name',
    'item.thaumcraft.golem_clay': 'item.ItemGolemPlacer.3.name',
    'item.thaumcraft.golem_flesh': 'item.ItemGolemPlacer.4.name',
    'item.thaumcraft.golem_stone': 'item.ItemGolemPlacer.5.name',
    'item.thaumcraft.golem_iron': 'item.ItemGolemPlacer.6.name',
    'item.thaumcraft.golem_thaumium': 'item.ItemGolemPlacer.7.name',
    'item.thaumcraft.golem_upgrade_air': 'item.ItemGolemUpgrade.0.name',
    'item.thaumcraft.golem_upgrade_earth': 'item.ItemGolemUpgrade.1.name',
    'item.thaumcraft.golem_upgrade_fire': 'item.ItemGolemUpgrade.2.name',
    'item.thaumcraft.golem_upgrade_water': 'item.ItemGolemUpgrade.3.name',
    'item.thaumcraft.golem_upgrade_order': 'item.ItemGolemUpgrade.4.name',
    'item.thaumcraft.golem_upgrade_entropy': 'item.ItemGolemUpgrade.5.name',
    'item.thaumcraft.golem_core_blank': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_fill': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_empty': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_gather': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_harvest': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_guard': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_liquid': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_alchemy': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_lumber': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_use': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_butcher': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_sorting': 'item.ItemGolemCore.name',
    'item.thaumcraft.golem_core_fishing': 'item.ItemGolemCore.name',
    'block.thaumcraft.thaumatorium': 'tile.blockMetalDevice.10.name',
    'block.thaumcraft.mnemonic_matrix': 'tile.blockMetalDevice.12.name',
    'block.thaumcraft.bellows': 'tile.blockWoodenDevice.0.name',
    'block.thaumcraft.crystallizer': 'tile.blockTube.7.name',
    'item.thaumcraft.crystal_essence': 'item.ItemCrystalEssence.name',
    'block.thaumcraft.flux_gas': 'tile.blockFluxGas.name',
    'effect.thaumcraft.vis_exhaustion': 'potion.visexhaust',
    'enchantment.thaumcraft.haste': 'enchantment.haste',
    'enchantment.thaumcraft.repair': 'enchantment.repair',
    'effect.thaumcraft.taint_poison': 'potion.fluxtaint',
    'effect.thaumcraft.thaumarhia': 'potion.thaumarhia',
    'effect.thaumcraft.unnatural_hunger': 'potion.unhunger',
    'effect.thaumcraft.blurred_vision': 'potion.blurred',
    'effect.thaumcraft.sun_scorned': 'potion.sunscorned',
    'effect.thaumcraft.infectious_vis_exhaustion': 'potion.infvisexhaust',
    'effect.thaumcraft.death_gaze': 'potion.deathgaze',
    'effect.thaumcraft.warp_ward': 'potion.warpward',
    'entity.thaumcraft.mind_spider': 'entity.Thaumcraft.MindSpider.name',
    'entity.thaumcraft.taint_spider': 'entity.Thaumcraft.TaintSpider.name',
    'entity.thaumcraft.taint_spore': 'entity.Thaumcraft.TaintSpore.name',
    'entity.thaumcraft.taint_spore_swarmer': 'entity.Thaumcraft.TaintSwarmer.name',
    'entity.thaumcraft.taint_swarm': 'entity.Thaumcraft.TaintSwarm.name',
    'entity.thaumcraft.tainted_cow': 'entity.Thaumcraft.TaintedCow.name',
    'entity.thaumcraft.tainted_pig': 'entity.Thaumcraft.TaintedPig.name',
    'entity.thaumcraft.tainted_sheep': 'entity.Thaumcraft.TaintedSheep.name',
    'entity.thaumcraft.tainted_chicken': 'entity.Thaumcraft.TaintedChicken.name',
    'entity.thaumcraft.tainted_villager': 'entity.Thaumcraft.TaintedVillager.name',
    'entity.thaumcraft.tainted_creeper': 'entity.Thaumcraft.TaintedCreeper.name',
    'entity.thaumcraft.taintacle': 'entity.Thaumcraft.Taintacle.name',
    'entity.thaumcraft.taintacle_small': 'entity.Thaumcraft.TaintacleTiny.name',
    'entity.thaumcraft.thaumic_slime': 'entity.Thaumcraft.ThaumSlime.name',
    'item.thaumcraft.taint_goo': 'item.ItemResource.11.name',
    'item.thaumcraft.bottle_taint': 'item.ItemBottleTaint.name',
    'entity.thaumcraft.bottle_taint': 'entity.Thaumcraft.BottleTaint.name',
    'item.thaumcraft.taint_tendril': 'item.ItemResource.12.name',
    'item.thaumcraft.zombie_brain': 'item.ItemZombieBrain.name',
    'item.thaumcraft.wisp_essence': 'item.ItemWispEssence.name',
    'entity.thaumcraft.wisp': 'entity.Thaumcraft.Wisp.name',
    'entity.thaumcraft.pech': 'entity.Thaumcraft.Pech.name',
    'entity.thaumcraft.pech.mage': 'entity.Thaumcraft.Pech.1.name',
    'entity.thaumcraft.pech.stalker': 'entity.Thaumcraft.Pech.2.name',
    'entity.thaumcraft.brainy_zombie': 'entity.Thaumcraft.BrainyZombie.name',
    'entity.thaumcraft.giant_brainy_zombie': 'entity.Thaumcraft.GiantBrainyZombie.name',
    'block.thaumcraft.nitor': 'item.ItemResource.1.name',
    'block.thaumcraft.thaumium_block': 'tile.blockCosmeticSolid.4.name',
    'item.thaumcraft.alumentum': 'item.ItemResource.0.name',
    'item.thaumcraft.thaumium_ingot': 'item.ItemResource.2.name',
    'item.thaumcraft.tallow': 'item.ItemResource.4.name',
    'item.thaumcraft.thaumium_nugget': 'item.ItemNugget.6.name',
    'item.thaumcraft.cluster_iron': 'item.ItemNugget.16.name',
    'item.thaumcraft.cluster_cinnabar': 'item.ItemNugget.21.name',
    'item.thaumcraft.cluster_gold': 'item.ItemNugget.31.name',
    'item.thaumcraft.cluster_copper': 'item.ItemNugget.17.name',
    'item.thaumcraft.cluster_tin': 'item.ItemNugget.18.name',
    'item.thaumcraft.cluster_silver': 'item.ItemNugget.19.name',
    'item.thaumcraft.cluster_lead': 'item.ItemNugget.20.name',
    'item.thaumcraft.tin_nugget': 'item.ItemNugget.2.name',
    'item.thaumcraft.silver_nugget': 'item.ItemNugget.3.name',
    'item.thaumcraft.lead_nugget': 'item.ItemNugget.4.name',
    'item.thaumcraft.wand_cap_silver': 'item.WandCap.4.name',
    'item.thaumcraft.wand_cap_silver_inert': 'item.WandCap.5.name',
    'item.thaumcraft.thaumium_helmet': 'item.ItemHelmetThaumium.name',
    'item.thaumcraft.thaumium_chestplate': 'item.ItemChestplateThaumium.name',
    'item.thaumcraft.thaumium_leggings': 'item.ItemLeggingsThaumium.name',
    'item.thaumcraft.thaumium_boots': 'item.ItemBootsThaumium.name',
    'item.thaumcraft.thaumium_shovel': 'item.ItemShovelThaumium.name',
    'item.thaumcraft.thaumium_axe': 'item.ItemAxeThaumium.name',
    'item.thaumcraft.thaumium_sword': 'item.ItemSwordThaumium.name',
    'item.thaumcraft.thaumium_pickaxe': 'item.ItemPickThaumium.name',
    'item.thaumcraft.thaumium_hoe': 'item.ItemHoeThaumium.name',
    'item.thaumcraft.elemental_axe': 'item.ItemAxeElemental.name',
    'item.thaumcraft.elemental_pickaxe': 'item.ItemPickaxeElemental.name',
    'item.thaumcraft.elemental_sword': 'item.ItemSwordElemental.name',
    'item.thaumcraft.elemental_shovel': 'item.ItemShovelElemental.name',
    'item.thaumcraft.elemental_hoe': 'item.ItemHoeElemental.name',
    'entity.thaumcraft.alumentum': 'item.ItemResource.0.name',
}

# Строки, которых не было в оригинале. Для остальных языков берётся английский вариант.
EXTRA_LANG = {
    'en_us': {
        'item.thaumcraft.obelisk_placer.creative': 'Creative Mode Only',
        'thaumcraft.spawn_egg': '%s Spawn Egg',
        'block.thaumcraft.eldritch_void': 'Void',
        'block.thaumcraft.eldritch_void_open': 'Void',
        'entity.thaumcraft.eldritch_warden': 'Eldritch Warden',
        'entity.thaumcraft.eldritch_golem': 'Eldritch Construct',
        'dimension.thaumcraft.outer_lands': 'The Outer Lands',
        'thaumcraft.bore.width': 'Width: %s',
        'tc.rarity.0': 'Common',
        'tc.rarity.1': 'Uncommon',
        'tc.rarity.2': 'Rare',
        'entity.thaumcraft.cultist_leader': 'Crimson Praetor',
        'entity.minecraft.villager.thaumcraft.thaumaturge': 'Thaumaturge',
        'entity.minecraft.villager.thaumcraft.moneychanger': 'Moneychanger',
        'thaumcraft.bore.speed': 'Speed: +%s',
        'thaumcraft.bore.other': 'Other properties:',
        'thaumcraft.bore.clusters': 'Native Clusters',
        'thaumcraft.bore.fortune': 'Fortune %s',
        'thaumcraft.bore.silk': 'Silk Touch',
        'death.attack.airarrow': '%1$s was pierced by a gust of air shot by %2$s',
        'death.attack.firearrow': '%1$s was set ablaze by an arrow shot by %2$s',
        'death.attack.orderarrow': '%1$s was unmade by an arrow of order shot by %2$s',
        'thaumcraft.door.refuses': 'The door refuses to budge.',
        'thaumcraft.plate.setting.0': 'It will now trigger on everything.',
        'thaumcraft.plate.setting.1': 'It will now trigger on everything except you.',
        'thaumcraft.plate.setting.2': 'It will now trigger on just you.',
        'thaumcraft.mirror.already_linked': 'That mirror is already linked to a valid destination.',
        'gui.thaumcraft.golem.any_color': 'Any color',
        'entity.thaumcraft.golem_dart': 'Dart',
        'gui.thaumcraft.golem.monsters': 'Monsters',
        'gui.thaumcraft.golem.animals': 'Animals',
        'gui.thaumcraft.golem.players': 'Players',
        'gui.thaumcraft.golem.creepers': 'Creepers',
        'gui.thaumcraft.golem.precise': 'Precise amount',
        'gui.thaumcraft.golem.any_amount': 'Any amount',
        'gui.thaumcraft.golem.block': 'Block',
        'gui.thaumcraft.golem.empty_space': 'Empty space',
        'gui.thaumcraft.golem.right_click': 'Right click',
        'gui.thaumcraft.golem.left_click': 'Left click',
        'gui.thaumcraft.golem.not_sneaking': 'Not sneaking',
        'gui.thaumcraft.golem.sneaking': 'Sneaking',
        'gui.thaumcraft.golem.ore_dict': 'Use item tags',
        'gui.thaumcraft.golem.ignore_damage': 'Ignore item damage',
        'gui.thaumcraft.golem.ignore_nbt': 'Ignore NBT values',
        'tc.invscan.scanning': 'Scanning',
        'biome.thaumcraft.tainted_land': 'Tainted Land',
        'biome.thaumcraft.eerie': 'Eerie',
        'block.thaumcraft.mana_pod': 'Mana Pod',
        'biome.thaumcraft.magical_forest': 'Magical Forest',
        'biome.thaumcraft.eldritch_lands': 'Outer Lands',
        'tc.invscan.tooltip': 'Inventory Scanning available',
        'tc.invscan.tooltip.more1': 'Hover items with the Thaumometer',
        'tc.invscan.tooltip.more2': 'on the cursor to scan.',
        'tc.arcane.insufficient': 'Insufficient vis',
        'tc.infusion.levels': '%s levels',
        'key.thaumcraft.change_focus': 'Change Wand Focus',
        'key.thaumcraft.misc_toggle': 'Misc Wand Toggle',
        'key.thaumcraft.hover': 'Toggle Hover Harness',
        'key.categories.thaumcraft': 'Thaumcraft',
        'tc.research.needtable': 'This research needs a research note and a research table (not ported yet)',
        'tc.research.notported': 'This recipe page is not ported yet.',
        'item.thaumcraft.raw_cinnabar': 'Raw Cinnabar',
    },
    'ru_ru': {
        'item.thaumcraft.obelisk_placer.creative': 'Только для творческого режима',
        'thaumcraft.spawn_egg': 'Яйцо призыва: %s',
        'block.thaumcraft.eldritch_void': 'Пустота',
        'block.thaumcraft.eldritch_void_open': 'Пустота',
        'entity.thaumcraft.eldritch_warden': 'Надзиратель Элдрича',
        'entity.thaumcraft.eldritch_golem': 'Конструкт Элдрича',
        'dimension.thaumcraft.outer_lands': 'Запредельные земли',
        'thaumcraft.bore.width': 'Ширина: %s',
        'tc.rarity.0': 'Обычный',
        'tc.rarity.1': 'Необычный',
        'tc.rarity.2': 'Редкий',
        'entity.thaumcraft.cultist_leader': 'Багровый претор',
        'entity.minecraft.villager.thaumcraft.thaumaturge': 'Тауматург',
        'entity.minecraft.villager.thaumcraft.moneychanger': 'Меняла',
        'thaumcraft.bore.speed': 'Скорость: +%s',
        'thaumcraft.bore.other': 'Особые свойства:',
        'thaumcraft.bore.clusters': 'Самородные грозди',
        'thaumcraft.bore.fortune': 'Удача %s',
        'thaumcraft.bore.silk': 'Шёлковое касание',
        'death.attack.airarrow': '%1$s пронзён(а) воздушной стрелой игрока %2$s',
        'death.attack.firearrow': '%1$s сгорел(а) от огненной стрелы игрока %2$s',
        'death.attack.orderarrow': '%1$s рассыпался(-ась) от стрелы порядка игрока %2$s',
        'thaumcraft.door.refuses': 'Дверь не поддаётся.',
        'thaumcraft.plate.setting.0': 'Теперь плита срабатывает на всех.',
        'thaumcraft.plate.setting.1': 'Теперь плита срабатывает на всех, кроме своих.',
        'thaumcraft.plate.setting.2': 'Теперь плита срабатывает только на своих.',
        'thaumcraft.mirror.already_linked': 'Это зеркало уже связано с действующей парой.',
        'gui.thaumcraft.golem.any_color': 'Любой цвет',
        'entity.thaumcraft.golem_dart': 'Дротик',
        'gui.thaumcraft.golem.monsters': 'Монстры',
        'gui.thaumcraft.golem.animals': 'Животные',
        'gui.thaumcraft.golem.players': 'Игроки',
        'gui.thaumcraft.golem.creepers': 'Криперы',
        'gui.thaumcraft.golem.precise': 'Точное количество',
        'gui.thaumcraft.golem.any_amount': 'Любое количество',
        'gui.thaumcraft.golem.block': 'Блок',
        'gui.thaumcraft.golem.empty_space': 'Пустое место',
        'gui.thaumcraft.golem.right_click': 'Правый щелчок',
        'gui.thaumcraft.golem.left_click': 'Левый щелчок',
        'gui.thaumcraft.golem.not_sneaking': 'Не крадучись',
        'gui.thaumcraft.golem.sneaking': 'Крадучись',
        'gui.thaumcraft.golem.ore_dict': 'Сравнивать по тегам',
        'gui.thaumcraft.golem.ignore_damage': 'Не смотреть на износ',
        'gui.thaumcraft.golem.ignore_nbt': 'Не смотреть на данные',
        'key.thaumcraft.change_focus': 'Сменить фокус жезла',
        'key.thaumcraft.misc_toggle': 'Переключатель жезла',
        'key.thaumcraft.hover': 'Включить упряжь парения',
        'key.categories.thaumcraft': 'Thaumcraft',
        'tc.invscan.scanning': 'Сканирование',
        'biome.thaumcraft.tainted_land': 'Порченые земли',
        'biome.thaumcraft.eerie': 'Жуткие земли',
        'block.thaumcraft.mana_pod': 'Стручок маны',
        'biome.thaumcraft.magical_forest': 'Волшебный лес',
        'biome.thaumcraft.eldritch_lands': 'Запредельные земли',
        'tc.invscan.tooltip': 'Доступно сканирование в инвентаре',
        'tc.invscan.tooltip.more1': 'Возьмите таумометр на курсор и наведите',
        'tc.invscan.tooltip.more2': 'на предмет, чтобы изучить его.',
        'tc.arcane.insufficient': 'Недостаточно вис',
        'tc.infusion.levels': 'Уровней опыта: %s',
        'tc.research.needtable': 'Для этого исследования нужны заметка и стол исследований (ещё не перенесены)',
        'tc.research.notported': 'Эта страница с рецептом ещё не перенесена.',
        'item.thaumcraft.raw_cinnabar': 'Сырая киноварь',
    },
}

# Руды в глубинном сланце: имя строится из оригинального имени руды.
DEEPSLATE_NAMES = {'en_us': 'Deepslate %s', 'ru_ru': '%s в глубинном сланце'}
DEEPSLATE_ORES = ['cinnabar_ore', 'amber_ore'] + ['infused_stone_' + e for e in
                                                   ('air', 'fire', 'water', 'earth', 'order', 'entropy')]

TEXTURE_DIRS = {'items': 'item', 'blocks': 'block'}


def clean(name):
    return re.sub(r'[^a-z0-9/._-]', '_', name.lower())


def copy_textures():
    count = 0
    base = os.path.join(SRC, 'textures')
    for root, _, files in os.walk(base):
        rel = os.path.relpath(root, base).replace('\\', '/')
        parts = rel.split('/')
        parts[0] = TEXTURE_DIRS.get(parts[0], parts[0])
        out_dir = os.path.join(DST, 'textures', clean('/'.join(parts)))
        os.makedirs(out_dir, exist_ok=True)
        for name in files:
            target_dir = out_dir
            if parts[0] in TEXTURE_DIRS.values() and name.endswith('.png') and not atlas_friendly(os.path.join(root, name)):
                # всё из item/ и block/ попадает в общий атлас; текстура нестандартного размера
                # (6x6, 8x8) снизила бы детализацию мип-уровней у всей игры, поэтому кладём её отдельно
                target_dir = os.path.join(DST, 'textures', 'unstitched', clean('/'.join(parts)))
                os.makedirs(target_dir, exist_ok=True)
            shutil.copyfile(os.path.join(root, name), os.path.join(target_dir, clean(name)))
            count += 1
    return count


def atlas_friendly(path):
    with open(path, 'rb') as fh:
        header = fh.read(24)
    width = struct.unpack('>I', header[16:20])[0]
    return width >= 16 and width & (width - 1) == 0


def copy_sounds():
    out_dir = os.path.join(DST, 'sounds')
    os.makedirs(out_dir, exist_ok=True)
    count = 0
    for name in os.listdir(os.path.join(SRC, 'sounds')):
        shutil.copyfile(os.path.join(SRC, 'sounds', name), os.path.join(out_dir, clean(name)))
        count += 1

    with open(os.path.join(SRC, 'sounds.json'), encoding='utf-8') as fh:
        old = json.load(fh)
    new = {}
    for event, data in old.items():
        sounds = []
        for sound in data['sounds']:
            if isinstance(sound, str):
                sounds.append('thaumcraft:' + clean(sound))
            else:
                entry = dict(sound)
                entry['name'] = 'thaumcraft:' + clean(entry['name'])
                sounds.append(entry)
        new[clean(event)] = {'sounds': sounds}
    with open(os.path.join(DST, 'sounds.json'), 'w', encoding='utf-8') as fh:
        json.dump(new, fh, indent=2)
    return count, sorted(new)


def fix_format(value):
    """В оригинале имя подставлялось через replaceAll("%n"); в 1.20.1 это обычные позиционные аргументы."""
    if '%CAP' in value or '%ROD' in value or '%OBJ' in value:
        # название жезла собирается из трёх частей; порядок частей в каждом языке свой
        return value.replace('%CAP', '%1$s').replace('%ROD', '%2$s').replace('%OBJ', '%3$s')
    if '%n' not in value:
        return value
    index = [0]

    def repl(match):
        index[0] += 1
        return '%' + str(index[0]) + '$s'
    return re.sub(r'%[sn]', repl, value)


def copy_models():
    """OBJ-модели, которые в 1.20.1 грузит forge:obj. Модель центрируется и ужимается до размера блока."""
    out_dir = os.path.join(DST, 'models', 'item')
    os.makedirs(out_dir, exist_ok=True)
    NL = chr(10)
    lines = ['mtllib scanner.mtl' + NL]
    with open(os.path.join(SRC, 'textures', 'models', 'scanner.obj')) as fh:
        for line in fh:
            parts = line.split()
            if parts and parts[0] == 'v':
                x, y, z = (float(v) for v in parts[1:4])
                lines.append('v %.5f %.5f %.5f' % (x * 0.35 + 0.5, (y - 0.1) * 0.35 + 0.5, z * 0.35 + 0.5) + NL)
            elif parts and parts[0] in ('mtllib', 'usemtl'):
                continue
            elif parts and parts[0] in ('g', 'o'):
                lines.append(line)
                lines.append('usemtl scanner' + NL)
            else:
                lines.append(line)
    # Линза: в оригинале это отдельный квадрат 2.5x2.5 с текстурой scanscreen на высоте 0.11,
    # который рисовал рендерер. Здесь он часть модели, с двух сторон, чтобы был виден и снизу.
    vertex_count = sum(1 for line in lines if line.startswith('v '))
    uv_count = sum(1 for line in lines if line.startswith('vt '))
    normal_count = sum(1 for line in lines if line.startswith('vn '))
    lens_y = (0.11 - 0.1) * 0.35 + 0.5
    for x, z in ((-1.25, -1.25), (-1.25, 1.25), (1.25, 1.25), (1.25, -1.25)):
        lines.append('v %.5f %.5f %.5f' % (x * 0.35 + 0.5, lens_y, z * 0.35 + 0.5) + NL)
    for u, v in ((0, 0), (1, 0), (1, 1), (0, 1)):
        lines.append('vt %d %d' % (u, v) + NL)
    lines.append('vn 0 1 0' + NL + 'vn 0 -1 0' + NL)
    v0, t0, n0 = vertex_count + 1, uv_count + 1, normal_count + 1
    lines.append('g lens' + NL + 'usemtl lens' + NL)
    lines.append('f %d/%d/%d %d/%d/%d %d/%d/%d %d/%d/%d' % (
        v0, t0, n0, v0 + 1, t0 + 1, n0, v0 + 2, t0 + 2, n0, v0 + 3, t0 + 3, n0) + NL)
    lines.append('f %d/%d/%d %d/%d/%d %d/%d/%d %d/%d/%d' % (
        v0 + 3, t0 + 3, n0 + 1, v0 + 2, t0 + 2, n0 + 1, v0 + 1, t0 + 1, n0 + 1, v0, t0, n0 + 1) + NL)

    with open(os.path.join(out_dir, 'scanner.obj'), 'w') as fh:
        fh.writelines(lines)
    with open(os.path.join(out_dir, 'scanner.mtl'), 'w') as fh:
        fh.write('newmtl scanner' + NL + 'map_Kd thaumcraft:item/scanner' + NL)
        fh.write('newmtl lens' + NL + 'map_Kd thaumcraft:item/scanscreen' + NL)
    shutil.copyfile(os.path.join(SRC, 'textures', 'models', 'scanscreen.png'),
                    os.path.join(DST, 'textures', 'item', 'scanscreen.png'))
    shutil.copyfile(os.path.join(SRC, 'textures', 'models', 'scanner.png'),
                    os.path.join(DST, 'textures', 'item', 'scanner.png'))


def copy_obj(name, keep=None, raw=False, out=None, tint=False, texture=None):
    """OBJ-модель блока из оригинала. Там ось Z модели смотрит вверх, а начало координат — в середине низа блока
    (рендереры делали glTranslate(0.5, 0, 0.5) и поворот на -90 градусов вокруг X). keep — какие части оставить.
    raw — оставить координаты как есть (модель рисует рендерер и сам её ставит); out — имя результата;
    tint — разрешить окрашивание модели цветом из кода."""
    out = out or name
    NL = chr(10)
    lines = ['mtllib %s.mtl' % out + NL]
    group = None
    with open(os.path.join(SRC, 'textures', 'models', name + '.obj')) as fh:
        for line in fh:
            parts = line.split()
            if not parts or parts[0] in ('mtllib', 'usemtl', '#', 's'):
                continue
            if parts[0] == 'v':
                x, y, z = (float(v) for v in parts[1:4])
                lines.append(('v %.5f %.5f %.5f' % ((x, y, z) if raw else (x + 0.5, z, 0.5 - y))) + NL)
            elif parts[0] == 'vn':
                x, y, z = (float(v) for v in parts[1:4])
                lines.append(('vn %.5f %.5f %.5f' % ((x, y, z) if raw else (x, z, -y))) + NL)
            elif parts[0] == 'vt':
                lines.append('vt %s %s' % (parts[1], parts[2]) + NL)
            elif parts[0] in ('g', 'o'):
                group = parts[1] if len(parts) > 1 else None
                if keep is None or group in keep:
                    lines.append('o %s' % group + NL + 'usemtl %s' % name + NL)
            elif parts[0] == 'f' and (keep is None or group in keep):
                lines.append(line.strip() + NL)
    out_dir = os.path.join(DST, 'models', 'block')
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, out + '.obj'), 'w') as fh:
        fh.writelines(lines)
    with open(os.path.join(out_dir, out + '.mtl'), 'w') as fh:
        fh.write('newmtl %s' % name + NL + 'map_Kd thaumcraft:block/%s' % (texture or name) + NL)
        if tint:
            fh.write('forge_TintIndex 0' + NL)
    if texture is None:
        shutil.copyfile(os.path.join(SRC, 'textures', 'models', name + '.png'),
                        os.path.join(DST, 'textures', 'block', name + '.png'))


def copy_block_model_textures():
    """Текстуры моделей, которые в 1.20.1 стали обычными моделями блоков и должны лежать в атласе."""
    for name in ('table', 'worktable', 'restable', 'restable2', 'infuser', 'bellows', 'centrifuge',
                 'node_stabilizer', 'node_stabilizer_over', 'node_converter', 'node_converter_over', 'wandtable', 'decontable', 'chesthungry', 'trunk'):
        shutil.copyfile(os.path.join(SRC, 'textures', 'models', name + '.png'),
                        os.path.join(DST, 'textures', 'block', name + '.png'))
    shutil.copyfile(os.path.join(SRC, 'textures', 'misc', 'parchment.png'),
                    os.path.join(DST, 'textures', 'block', 'parchment.png'))


def convert_lang():
    out_dir = os.path.join(DST, 'lang')
    os.makedirs(out_dir, exist_ok=True)

    def read(path):
        table = {}
        with open(path, encoding='utf-8-sig', errors='replace') as fh:
            for line in fh:
                line = line.rstrip('\r\n')
                if not line or line.startswith('#') or '=' not in line:
                    continue
                key, value = line.split('=', 1)
                table[key.strip()] = fix_format(value)
        return table

    english = read(os.path.join(SRC, 'lang', 'en_US.lang'))
    final_english = {}
    count = 0
    for name in os.listdir(os.path.join(SRC, 'lang')):
        if not name.endswith('.lang'):
            continue
        table = read(os.path.join(SRC, 'lang', name))
        for new_key, old_key in KEYMAP.items():
            value = table.get(old_key, english.get(old_key))
            if value is None:
                raise KeyError('нет ключа в оригинале: ' + old_key)
            # в оригинале имя стихии подставлялось на место %TYPE; у игры для этого есть свой знак
            table[new_key] = value.replace('%TYPE', '%s')
        # в некоторых переводах оригинала свойства чемпионов записаны без «.mod.» (champion.bold), и игра их не находила
        for key in [k for k in table if k.startswith('champion.') and not k.startswith('champion.mod.')]:
            table.setdefault('champion.mod.' + key[len('champion.'):], table[key])
        for boss in ('entity.Thaumcraft.EldritchWarden.name', 'entity.Thaumcraft.CultistLeader.name'):
            if boss in table:
                table[boss] = table[boss].replace('%d', '%s')
        pattern = DEEPSLATE_NAMES.get(name[:-5].lower(), DEEPSLATE_NAMES['en_us'])
        for ore in DEEPSLATE_ORES:
            table['block.thaumcraft.deepslate_' + ore] = pattern % table['block.thaumcraft.' + ore]
        table.update(EXTRA_LANG['en_us'])
        table.update(EXTRA_LANG.get(name[:-5].lower(), {}))
        name_spawn_eggs(table)
        with open(os.path.join(out_dir, name[:-5].lower() + '.json'), 'w', encoding='utf-8') as fh:
            json.dump(table, fh, ensure_ascii=False, indent=2, sort_keys=True)
        count += 1
        if name == 'en_US.lang':
            final_english = dict(table)
    # украинского в оригинале нет: перевод лежит частями в tools/lang/uk, поверх английского
    uk_dir = os.path.join(ROOT, 'tools', 'lang', 'uk')
    if os.path.isdir(uk_dir):
        table = dict(final_english)
        for part in sorted(os.listdir(uk_dir)):
            if part.endswith('.json'):
                with open(os.path.join(uk_dir, part), encoding='utf-8') as fh:
                    table.update(json.load(fh))
        name_spawn_eggs(table)
        untranslated = sorted(k for k in table if k in final_english and table[k] == final_english[k]
                              and any(c.isalpha() for c in table[k]) and not table[k].startswith(('Salis', 'Thaum')))
        if untranslated:
            print('uk_ua: без перевода', len(untranslated), untranslated[:10])
        with open(os.path.join(out_dir, 'uk_ua.json'), 'w', encoding='utf-8') as fh:
            json.dump(table, fh, ensure_ascii=False, indent=2, sort_keys=True)
        count += 1
    return count


SPAWN_EGG_MOBS = ['brainy_zombie', 'giant_brainy_zombie', 'wisp', 'fire_bat', 'pech', 'mind_spider', 'eldritch_guardian', 'eldritch_warden', 'cultist_knight', 'cultist_cleric', 'cultist_leader', 'cultist_portal', 'eldritch_golem', 'eldritch_crab', 'inhabited_zombie', 'thaumic_slime', 'taint_spider', 'taintacle', 'taintacle_small', 'taint_spore', 'taint_spore_swarmer', 'taint_swarm', 'tainted_chicken', 'tainted_cow', 'tainted_creeper', 'tainted_pig', 'tainted_sheep', 'tainted_villager', 'taintacle_giant']


def name_spawn_eggs(table):
    # яйца призыва называются по существу: «Яйцо призыва: висп»
    for mob in SPAWN_EGG_MOBS:
        table['item.thaumcraft.%s_spawn_egg' % mob] = table['thaumcraft.spawn_egg'] % table.get('entity.thaumcraft.' + mob, mob)


if __name__ == '__main__':
    print('текстур:', copy_textures())
    sounds, events = copy_sounds()
    print('звуков:', sounds, 'событий:', len(events))
    print('языков:', convert_lang())
    copy_models()
    copy_obj('alembic', ('Pot', 'Panel', 'Legs', 'TubeMain'))
    copy_obj('thaumatorium')
    copy_obj('pillar')
    copy_obj('crystalizer', raw=True)
    copy_obj('bucket', ('Bucket',), raw=True, out='golem_bucket')
    copy_obj('hoverharness', raw=True, out='hover_harness_back', texture='hoverharness2')
    copy_obj('obelisk_cap', ('Cap',), raw=True, out='flux_scrubber_cap', texture='fluxscrubber')
    copy_obj('reservoir', raw=True, out='essentia_reservoir_frame')
    copy_obj('adv_alch_furnace', ('Base',), raw=True, out='adv_furnace_base', texture='alch_furnace')
    copy_obj('adv_alch_furnace', ('Base',), raw=True, out='adv_furnace_base_on', texture='alch_furnace_on')
    copy_obj('adv_alch_furnace', ('Tank',), raw=True, out='adv_furnace_tank', texture='alch_furnace_tank')
    copy_obj('adv_alch_furnace', ('Tank',), raw=True, out='adv_furnace_tank_on', texture='alch_furnace_tank_on')
    for texture in ('alch_furnace', 'alch_furnace_on', 'alch_furnace_tank', 'alch_furnace_tank_on'):
        shutil.copyfile(os.path.join(SRC, 'textures', 'models', texture + '.png'), os.path.join(DST, 'textures', 'block', texture + '.png'))
    copy_obj('obelisk_cap', ('Tip',), raw=True, out='flux_scrubber_tip', texture='fluxscrubber')
    shutil.copyfile(os.path.join(SRC, 'textures', 'models', 'fluxscrubber.png'),
                    os.path.join(DST, 'textures', 'block', 'fluxscrubber.png'))
    shutil.copyfile(os.path.join(SRC, 'textures', 'models', 'hoverharness2.png'),
                    os.path.join(DST, 'textures', 'block', 'hoverharness2.png'))
    # сеть вис: части стабилизатора и преобразователя (одна модель, разные текстуры) и кольца реле
    copy_obj('node_stabilizer', ('lock',), raw=True, out='node_stabilizer_lock', texture='node_stabilizer')
    copy_obj('node_stabilizer', ('piston',), raw=True, out='node_stabilizer_piston', texture='node_stabilizer')
    copy_obj('node_stabilizer', ('piston',), raw=True, out='node_stabilizer_piston_over', texture='node_stabilizer_over', tint=True)
    copy_obj('node_stabilizer', ('lock',), raw=True, out='node_converter_lock', texture='node_converter')
    copy_obj('node_stabilizer', ('lock',), raw=True, out='node_converter_lock_over', texture='node_converter_over', tint=True)
    copy_obj('node_stabilizer', ('piston',), raw=True, out='node_converter_piston', texture='node_converter')
    copy_obj('node_stabilizer', ('piston',), raw=True, out='node_converter_piston_over', texture='node_converter_over', tint=True)
    copy_obj('vis_relay', ('RingBase',), raw=True, out='vis_relay_base', texture='vis_relay')
    copy_obj('vis_relay', ('RingFloat',), raw=True, out='vis_relay_float', texture='vis_relay')
    copy_obj('vis_relay', ('Support',), raw=True, out='vis_relay_support', texture='vis_relay')
    # ледяной осколок: текстура лежит среди блоков оригинала
    copy_obj('orb', raw=True, out='frost_shard', texture='frostshard')
    copy_obj('vis_relay', ('Crystal',), raw=True, out='vis_relay_crystal', tint=True)
    # колпаки обелиска Элдрича: обычный, тёмный (Запредельные земли) и алтарный
    for out, texture in (('eldritch_cap', 'obelisk_cap'), ('eldritch_cap_2', 'obelisk_cap_2'), ('eldritch_cap_altar', 'obelisk_cap_altar')):
        copy_obj('obelisk_cap', ('Cap',), raw=True, out=out, texture=texture)
        shutil.copyfile(os.path.join(SRC, 'textures', 'models', texture + '.png'), os.path.join(DST, 'textures', 'block', texture + '.png'))
    copy_obj('crabvent', raw=True, out='crab_vent')
    # странные кристаллы: основание из корки и сама друза
    copy_obj('vcrystal', ('Base',), raw=True, out='strange_crystals_base', texture='crust')
    copy_obj('vcrystal', ('Crystal',), raw=True, out='strange_crystals_crystal', texture='vcrystal')
    shutil.copyfile(os.path.join(SRC, 'textures', 'models', 'vcrystal.png'), os.path.join(DST, 'textures', 'block', 'vcrystal.png'))
    copy_block_model_textures()

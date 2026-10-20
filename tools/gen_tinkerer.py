"""Строит ресурсы и данные аддона Thaumic Tinkerer (src/tinkerer/resources) из оригинала в reference/tt.

Запуск из корня проекта:  python tools/gen_tinkerer.py

Что делает:
  * копирует текстуры оригинала под новыми именами (TEXTURES);
  * переводит .lang оригинала в json: названия предметов и блоков — по ITEM_NAMES, тексты исследований —
    в ключи книги порта (tc.research_name / tc.research_text), страницы оставляет под своими ключами;
    украинский берётся из tools/lang/tt_uk.json, там же наши собственные строки (tools/lang/tt_extra_*.json);
  * пишет исследования (RESEARCH), рецепты (RECIPES), модели, состояния блоков, таблицы добычи и метки.
Добавляя очередную часть аддона, дописывают словари ниже — остальное генератор делает сам.
"""
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'reference', 'tt', 'assets', 'ttinkerer')
OUT = os.path.join(ROOT, 'src', 'tinkerer', 'resources')
ASSETS = os.path.join(OUT, 'assets', 'thaumictinkerer')
DATA = os.path.join(OUT, 'data', 'thaumictinkerer')
NS = 'thaumictinkerer'

# ---------------------------------------------------------------- текстуры: новое имя -> файл оригинала
TEXTURES = {
    'item/dark_quartz': 'items/darkQuartzItem.png',
    'item/spell_cloth': 'items/spellCloth.png',
    'item/xp_talisman': 'items/xpTalisman0.png',
    'item/xp_talisman_active': 'items/xpTalisman1.png',
    'item/cleansing_talisman': 'items/cleansingTalisman0.png',
    'item/cleansing_talisman_active': 'items/cleansingTalisman1.png',
    'item/bright_nitor': 'items/brightNitor.png',
    'item/gaseous_light': 'items/gaseousLightItem.png',
    'item/gaseous_shadow': 'items/gaseousShadowItem.png',
    'item/gas_remover': 'items/gasRemover.png',
    'item/share_tome': 'items/shareBook.png',
    'item/revealing_helm': 'items/revealingHelm.png',
    'block/dark_quartz_block_side': 'blocks/darkQuartz0.png',
    'block/dark_quartz_block_top': 'blocks/darkQuartz1.png',
    'block/dark_quartz_block_bottom': 'blocks/darkQuartz2.png',
    'block/chiseled_dark_quartz_block': 'blocks/chiseledDarkQuartz0.png',
    'block/chiseled_dark_quartz_block_top': 'blocks/chiseledDarkQuartz1.png',
    'block/dark_quartz_pillar': 'blocks/pillarDarkQuartz0.png',
    'block/dark_quartz_pillar_top': 'blocks/pillarDarkQuartz1.png',
    'block/aspect_analyzer_top': 'blocks/aspectAnalyzer0.png',
    'block/aspect_analyzer_north': 'blocks/aspectAnalyzer1.png',
    'block/aspect_analyzer_south': 'blocks/aspectAnalyzer2.png',
    'block/aspect_analyzer_west': 'blocks/aspectAnalyzer3.png',
    'block/aspect_analyzer_east': 'blocks/aspectAnalyzer4.png',
    'block/funnel_top': 'blocks/funnel0.png',
    'block/funnel_side': 'blocks/funnel1.png',
    'block/platform': 'blocks/platform.png',
    'models/repairer': 'model/repairer.png',
    'models/armor/revealing_layer_1': 'model/revealing.png',
    'misc/repair': 'misc/repair.png',
    'misc/repair_off': 'misc/repairOff.png',
    'gui/aspect_analyzer': 'gui/aspectAnalyzer.png',
    # часть 2а: фокусы и зачарователь
    'item/focus_smelt': 'items/focusSmelt.png', 'item/focus_smelt_orn': 'items/focusSmeltOrn.png',
    'item/focus_flight': 'items/focusFlight.png', 'item/focus_flight_orn': 'items/focusFlightOrn.png',
    'item/focus_deflect': 'items/focusDeflect.png',
    'item/focus_telekinesis': 'items/focusTelekinesis.png', 'item/focus_telekinesis_orn': 'items/focusTelekinesisOrn.png',
    'item/focus_dislocation': 'items/focusDislocation.png', 'item/focus_dislocation_orn': 'items/focusDislocationOrn.png',
    'item/focus_heal': 'items/focusHeal.png', 'unstitched/item/focus_heal_depth': 'items/focusHealDepth.png',
    'item/focus_ender_chest': 'items/focusEnderChest.png', 'unstitched/item/focus_ender_chest_depth': 'items/focusEnderChestDepth.png',
    'block/enchanter_bottom': 'blocks/enchanter0.png', 'block/enchanter_top': 'blocks/enchanter1.png',
    'block/enchanter_side': 'blocks/enchanter2.png', 'gui/enchanter': 'gui/enchanter.png',
}
# значки чар: имя порта -> файл оригинала
ENCHANT_ICONS = {
    'protection': 'protection', 'fire_protection': 'fireProtection', 'feather_falling': 'featherFalling',
    'blast_protection': 'blastProtection', 'projectile_protection': 'projectileProtection', 'respiration': 'respiration',
    'aqua_affinity': 'aquaAffinity', 'thorns': 'thorns', 'sharpness': 'sharpness', 'smite': 'smite',
    'bane_of_arthropods': 'baneOfAwfulTurds', 'knockback': 'knockback', 'fire_aspect': 'fireAspect', 'looting': 'looting',
    'efficiency': 'efficiency', 'silk_touch': 'silkTouch', 'unbreaking': 'unbreaking', 'fortune': 'fortune', 'power': 'power',
    'punch': 'punch', 'flame': 'flame', 'infinity': 'infinity', 'lure': 'lure', 'luck_of_the_sea': 'luckOfTheSea',
    'haste': 'haste', 'repair': 'repair', 'ascent_boost': 'ascentBoost', 'slow_fall': 'slowFall', 'auto_smelt': 'autoSmelt',
    'desintegrate': 'desintegrate', 'quick_draw': 'quickDraw', 'vampirism': 'vamprisim', 'focused_strikes': 'focusedStrikes',
    'dispersed_strikes': 'dispersedStrikes', 'valiance': 'valiance', 'final_strike': 'finalStrike', 'tunnel': 'tunnel',
    'shatter': 'shatter', 'shockwave': 'shockwave', 'pounce': 'pounce',
}
TEXTURES.update({'enchants/' + k: 'enchants/%s.png' % v for k, v in ENCHANT_ICONS.items()})
# часть 2б
TEXTURES.update({
    'models/magnet_n': 'model/magnetN.png', 'models/magnet_s': 'model/magnetS.png',
    'models/mob_magnet_n': 'model/mobMagnetN.png', 'models/mob_magnet_s': 'model/mobMagnetS.png',
    'item/soul_mould': 'items/soulMould.png', 'item/connector': 'items/connector.png',
    'block/transvector_interface': 'blocks/interface.png',
    'block/transvector_dislocator': 'blocks/dislocator0.png', 'block/transvector_dislocator_face': 'blocks/dislocator1.png',
    'block/remote_placer': 'blocks/remotePlacer0.png', 'block/remote_placer_face': 'blocks/remotePlacer1.png',
    'block/mobilizer_bottom': 'blocks/Levitational Locomotive0.png', 'block/mobilizer_top': 'blocks/Levitational Locomotive1.png',
    'block/mobilizer_side': 'blocks/Levitational Locomotive2.png',
    'block/mobilizer_relay_bottom': 'blocks/Levitational Locomotive Relay0.png',
    'block/mobilizer_relay_top': 'blocks/Levitational Locomotive Relay1.png',
    'block/mobilizer_relay_side': 'blocks/Levitational Locomotive Relay2.png',
    'block/animation_tablet_bottom': 'blocks/animationTablet0.png', 'block/animation_tablet_top': 'blocks/animationTablet1.png',
    'block/animation_tablet_side': 'blocks/animationTablet2.png',
    'misc/at_center': 'misc/atCenter.png', 'misc/at_left': 'misc/atLeft.png', 'misc/at_right': 'misc/atRight.png',
    'misc/at_facing_indent': 'misc/atFacingIndent.png',
    'gui/mob_magnet': 'gui/mobMagnet.png', 'gui/remote_placer': 'gui/remotePlacer.png', 'gui/animation_tablet': 'gui/animationTablet.png',
})

# ---------------------------------------------------------------- названия: наш id -> ключ .lang оригинала
ITEM_NAMES = {
    'item.thaumictinkerer.dark_quartz': 'item.darkQuartzItem.name',
    'block.thaumictinkerer.dark_quartz_block': 'tile.darkQuartz.name',
    'block.thaumictinkerer.chiseled_dark_quartz_block': 'tile.darkQuartzChiseled.name',
    'block.thaumictinkerer.dark_quartz_pillar': 'tile.darkQuartzPillar.name',
    'block.thaumictinkerer.dark_quartz_slab': 'tile.darkQuartzSlab.name',
    'block.thaumictinkerer.dark_quartz_stairs': 'tile.darkQuartzStairs.name',
    'item.thaumictinkerer.spell_cloth': 'item.spellCloth.name',
    'item.thaumictinkerer.xp_talisman': 'item.xpTalisman.name',
    'item.thaumictinkerer.cleansing_talisman': 'item.cleansingTalisman.name',
    'item.thaumictinkerer.bright_nitor': 'item.brightNitor.name',
    'item.thaumictinkerer.gaseous_light': 'item.gaseousLightItem.name',
    'item.thaumictinkerer.gaseous_shadow': 'item.gaseousShadowItem.name',
    'item.thaumictinkerer.gas_remover': 'item.gasRemover.name',
    'item.thaumictinkerer.share_tome': 'item.shareBook.name',
    'item.thaumictinkerer.revealing_helm': 'item.revealingHelm.name',
    'block.thaumictinkerer.funnel': 'tile.funnel.name',
    'block.thaumictinkerer.repairer': 'tile.repairer.name',
    'block.thaumictinkerer.aspect_analyzer': 'tile.aspectAnalyzer.name',
    'block.thaumictinkerer.platform': 'tile.platform.name',
    'item.thaumictinkerer.focus_smelt': 'item.focusSmelt.name',
    'item.thaumictinkerer.focus_flight': 'item.focusFlight.name',
    'item.thaumictinkerer.focus_deflect': 'item.focusDeflect.name',
    'item.thaumictinkerer.focus_telekinesis': 'item.focusTelekinesis.name',
    'item.thaumictinkerer.focus_dislocation': 'item.focusDislocation.name',
    'item.thaumictinkerer.focus_heal': 'item.focusHeal.name',
    'item.thaumictinkerer.focus_ender_chest': 'item.focusEnderChest.name',
    'block.thaumictinkerer.enchanter': 'tile.enchanter.name',
}
# часть 2в: аспекты души по порядку SoulAspectItem.ASPECTS
SOUL_ASPECTS = ['aqua', 'humanus', 'aer', 'volatus', 'ignis', 'praecantatio', 'exanimis', 'corpus', 'bestia', 'venenum', 'terra',
                'alienis', 'iter', 'metallum', 'limus', 'pannus', 'messis']
TEXTURES.update({'item/soul/' + a: 'items/%s.png' % a for a in SOUL_ASPECTS})
TEXTURES.update({'item/soul/%s_condensed' % a: 'items/%s_condensed.png' % a for a in SOUL_ASPECTS})
TEXTURES.update({'item/blood_sword': 'items/bloodSword0.png', 'item/blood_sword_active': 'items/bloodSword1.png',
                 'block/summon_top': 'blocks/spawner1.png', 'block/summon_side': 'blocks/spawner2.png'})
ITEM_NAMES.update({
    'item.thaumictinkerer.blood_sword': 'item.bloodSword.name', 'block.thaumictinkerer.summon': 'tile.summon.name',
    'item.thaumictinkerer.soul_aspect': 'item.mobAspect.name',
    'item.thaumictinkerer.condensed_soul_aspect': 'item.mobAspect.condensed.name',
    'item.thaumictinkerer.infused_soul_aspect': 'item.mobAspect.infused.name',
})
ITEM_NAMES.update({
    'block.thaumictinkerer.magnet': 'tile.magnet.name', 'block.thaumictinkerer.mob_magnet': 'tile.mobMagnet.name',
    'item.thaumictinkerer.soul_mould': 'item.soulMould.name', 'item.thaumictinkerer.connector': 'item.connector.name',
    'block.thaumictinkerer.transvector_interface': 'tile.interface.name',
    'block.thaumictinkerer.transvector_dislocator': 'tile.dislocator.name',
    'block.thaumictinkerer.mobilizer': 'tile.Levitational Locomotive.name',
    'block.thaumictinkerer.mobilizer_relay': 'tile.Levitational Locomotive Relay.name',
    'block.thaumictinkerer.remote_placer': 'tile.remotePlacer.name',
    'block.thaumictinkerer.animation_tablet': 'tile.animationTablet.name',
})
for ours, theirs in {'ascent_boost': 'ascentBoost', 'slow_fall': 'slowFall', 'auto_smelt': 'autoSmelt',
                     'desintegrate': 'desintegrate', 'quick_draw': 'quickDraw', 'vampirism': 'vampirism',
                     'dispersed_strikes': 'dispersedStrike', 'focused_strikes': 'focusedStrike', 'tunnel': 'tunnel',
                     'final_strike': 'finalStrike', 'pounce': 'pounce', 'shatter': 'shatter', 'shockwave': 'shockwave',
                     'valiance': 'valiance'}.items():
    ITEM_NAMES['enchantment.thaumictinkerer.' + ours] = 'enchantment.ttinkerer:' + theirs
# в разных языках оригинала один и тот же предмет назван разными ключами
ITEM_NAME_ALIASES = {'item.darkQuartzItem.name': ['item.darkQuartz.name']}

LANGS = {'en_us': 'en_US', 'ru_ru': 'ru_RU', 'de_de': 'de_DE', 'fr_fr': 'fr_Fr', 'ko_kr': 'ko_KR', 'zh_cn': 'zh_CN'}

# ---------------------------------------------------------------- исследования
P = lambda kind, recipe: {'type': kind, 'recipe': NS + ':' + recipe}


def text(key, *pages):
    return [{'type': 'text', 'text': 'ttresearch.page.%s.%d' % (key, n)} for n in pages]


RESEARCH = [
    dict(key='DARK_QUARTZ', column=-2, row=2, complexity=0, icon='dark_quartz', stub=True, autoUnlock=True, round=True,
         pages=text('DARK_QUARTZ', 0) + [P('crafting', r) for r in (
             'dark_quartz', 'dark_quartz_block', 'dark_quartz_slab', 'chiseled_dark_quartz_block', 'dark_quartz_pillar',
             'dark_quartz_stairs')]),
    dict(key='SHARE_TOME', column=0, row=-1, complexity=0, icon='share_tome', stub=True, autoUnlock=True, round=True,
         pages=text('SHARE_TOME', 0) + [P('crafting', 'share_tome')]),
    dict(key='GASEOUS_LIGHT', aspects={'lux': 2, 'aer': 1}, column=0, row=-3, complexity=1, icon='gaseous_light',
         parents=['NITOR'], pages=text('GASEOUS_LIGHT', 0) + [P('crucible', 'crucible/gaseous_light')]),
    dict(key='GASEOUS_SHADOW', aspects={'tenebrae': 2, 'aer': 1, 'motus': 4}, column=-1, row=-5, complexity=2,
         icon='gaseous_shadow', secondary=True, parents=['GASEOUS_LIGHT'],
         pages=text('GASEOUS_SHADOW', 0) + [P('crucible', 'crucible/gaseous_shadow')]),
    dict(key='GAS_REMOVER', aspects={'tenebrae': 2, 'lux': 2}, column=-2, row=-7, complexity=0, icon='gas_remover',
         round=True, parents=['GASEOUS_SHADOW'], pages=text('GAS_REMOVER', 0) + [P('arcane', 'gas_remover')]),
    dict(key='BRIGHT_NITOR', aspects={'lux': 2, 'ignis': 1, 'potentia': 1, 'aer': 1}, column=1, row=-5, complexity=2,
         icon='bright_nitor', concealed=True, secondary=True, parents=['GASEOUS_LIGHT'],
         pages=text('BRIGHT_NITOR', 0) + [P('crucible', 'crucible/bright_nitor')]),
    dict(key='FUNNEL', aspects={'instrumentum': 1, 'iter': 2}, column=0, row=-7, complexity=1, icon='funnel',
         concealed=True, secondary=True, parents=['BRIGHT_NITOR'], parents_hidden=['DISTILESSENTIA'],
         pages=text('FUNNEL', 0) + [P('arcane', 'funnel')]),
    dict(key='REPAIRER', aspects={'instrumentum': 2, 'fabrico': 1, 'ordo': 1, 'praecantatio': 1}, column=-1, row=-9,
         complexity=3, icon='repairer', concealed=True, parents=['FUNNEL'], parents_hidden=['THAUMIUM', 'ENCHFABRIC'],
         pages=text('REPAIRER', 0) + [P('infusion', 'infusion/repairer')]),
    dict(key='SPELL_CLOTH', aspects={'praecantatio': 2, 'pannus': 1}, column=3, row=2, complexity=2, icon='spell_cloth',
         parents_hidden=['ENCHFABRIC'], pages=text('SPELL_CLOTH', 0) + [P('crucible', 'crucible/spell_cloth')]),
    dict(key='XP_TALISMAN', aspects={'lucrum': 1, 'praecantatio': 1, 'humanus': 1}, column=4, row=-1, complexity=2,
         icon='xp_talisman', concealed=True, secondary=True, parents=['JARBRAIN', 'SPELL_CLOTH'],
         pages=text('XP_TALISMAN', 0) + [P('infusion', 'infusion/xp_talisman')]),
    dict(key='CLEANSING_TALISMAN', aspects={'sano': 2, 'ordo': 1, 'venenum': 1}, column=-3, row=4, complexity=3,
         icon='cleansing_talisman', secondary=True, parents=['DARK_QUARTZ'],
         pages=text('CLEANSING_TALISMAN', 0) + [P('infusion', 'infusion/cleansing_talisman')]),
    dict(key='REVEALING_HELM', aspects={'auram': 2, 'tutamen': 1}, column=0, row=0, complexity=1, icon='revealing_helm',
         parents=['GOGGLES'], parents_hidden=['THAUMIUM'],
         pages=text('REVEALING_HELM', 0) + [P('arcane', 'revealing_helm')]),
    # в оригинале родитель — «Периферия» ComputerCraft; без неё анализатор открывается после очков и таумия
    dict(key='ASPECT_ANALYZER', aspects={'machina': 2, 'sensus': 1, 'cognitio': 1}, column=0, row=1, complexity=2,
         icon='aspect_analyzer', concealed=True, round=True, parents_hidden=['GOGGLES', 'THAUMIUM'],
         pages=text('ASPECT_ANALYZER', 0) + [P('arcane', 'aspect_analyzer')]),
    dict(key='PLATFORM', aspects={'sensus': 2, 'arbor': 1, 'motus': 1}, column=-2, row=6, complexity=3, icon='platform',
         concealed=True, secondary=True, parents=['CLEANSING_TALISMAN'], pages=text('PLATFORM', 0) + [P('arcane', 'platform')]),
    # часть 2а
    dict(key='FOCUS_SMELT', aspects={'ignis': 2, 'potentia': 1, 'praecantatio': 1}, column=-2, row=-2, complexity=2,
         icon='focus_smelt', concealed=True, parents=['FOCUSEXCAVATION'], pages=text('FOCUS_SMELT', 0) + [P('arcane', 'focus_smelt')]),
    dict(key='FOCUS_FLIGHT', aspects={'motus': 1, 'praecantatio': 1, 'aer': 2}, column=-3, row=-4, complexity=2,
         icon='focus_flight', concealed=True, parents=['FOCUS_SMELT'], pages=text('FOCUS_FLIGHT', 0) + [P('infusion', 'infusion/focus_flight')]),
    dict(key='FOCUS_TELEKINESIS', aspects={'alienis': 2, 'praecantatio': 1, 'motus': 1}, column=-4, row=-6, complexity=2,
         icon='focus_telekinesis', concealed=True, secondary=True, parents=['FOCUS_FLIGHT'],
         pages=text('FOCUS_TELEKINESIS', 0) + [P('infusion', 'infusion/focus_telekinesis')]),
    dict(key='FOCUS_DEFLECT', aspects={'motus': 2, 'aer': 1, 'ordo': 1, 'mortuus': 1}, column=-4, row=-3, complexity=3,
         icon='focus_deflect', concealed=True, secondary=True, parents=['FOCUS_SMELT'],
         pages=text('FOCUS_DEFLECT', 0) + [P('infusion', 'infusion/focus_deflect')]),
    dict(key='FOCUS_DISLOCATION', aspects={'alienis': 2, 'praecantatio': 1, 'permutatio': 1}, column=-5, row=-5, complexity=2,
         icon='focus_dislocation', concealed=True, secondary=True, parents=['FOCUS_FLIGHT'],
         pages=text('FOCUS_DISLOCATION', 0, 1) + [P('infusion', 'infusion/focus_dislocation')]),
    dict(key='FOCUS_HEAL', aspects={'sano': 2, 'spiritus': 1, 'praecantatio': 1}, column=-6, row=-4, complexity=2,
         icon='focus_heal', concealed=True, secondary=True, parents=['FOCUS_DEFLECT'],
         pages=text('FOCUS_HEAL', 0) + [P('infusion', 'infusion/focus_heal')]),
    dict(key='FOCUS_ENDER_CHEST', aspects={'alienis': 2, 'vacuos': 1, 'praecantatio': 1}, column=-6, row=-2, complexity=2,
         icon='focus_ender_chest', concealed=True, warp=1, parents=['FOCUS_DEFLECT'],
         pages=text('FOCUS_ENDER_CHEST', 0) + [P('arcane', 'focus_ender_chest')]),
    dict(key='ENCHANTER', aspects={'praecantatio': 2, 'auram': 1, 'alienis': 1, 'tenebrae': 1, 'cognitio': 1}, column=5, row=4,
         complexity=5, icon='enchanter', parents=['SPELL_CLOTH'], pages=text('ENCHANTER', 0, 1, 2) + [P('infusion', 'infusion/enchanter')]),
]
# часть 2б
RESEARCH += [
    dict(key='INTERFACE', aspects={'perditio': 4, 'ordo': 4}, column=-4, row=2, complexity=1, icon='transvector_interface',
         parents=['DARK_QUARTZ'], pages=text('INTERFACE', 0) + [P('arcane', 'transvector_interface')] + text('INTERFACE', 1)
         + [P('arcane', 'connector')] + text('INTERFACE', 2)),
    dict(key='DISLOCATOR', aspects={'iter': 2, 'machina': 1, 'alienis': 1}, column=-6, row=1, complexity=3,
         icon='transvector_dislocator', concealed=True, secondary=True, parents=['INTERFACE'], parents_hidden=['MIRROR'],
         pages=text('DISLOCATOR', 0) + [P('arcane', 'transvector_dislocator')]),
    dict(key='MAGNETS', aspects={'machina': 2, 'motus': 1, 'sensus': 1}, column=-6, row=3, complexity=3, icon='magnet',
         concealed=True, parents=['INTERFACE'], parents_hidden=['FOCUS_TELEKINESIS'],
         pages=text('MAGNETS', 0, 1) + [P('arcane', 'magnet'), P('arcane', 'mob_magnet'), P('crucible', 'crucible/soul_mould')]),
    dict(key='LEVITATOR', aspects={'motus': 2, 'ordo': 2}, column=-7, row=5, complexity=3, icon='mobilizer', secondary=True,
         parents=['MAGNETS'], pages=text('LEVITATOR', 0) + [P('infusion', 'infusion/mobilizer'), P('arcane', 'mobilizer_relay')]),
    dict(key='ANIMATION_TABLET', aspects={'machina': 2, 'metallum': 1, 'motus': 1, 'potentia': 1}, column=-8, row=2, complexity=4,
         icon='animation_tablet', warp=1, parents=['MAGNETS'], pages=text('ANIMATION_TABLET', 0) + [P('arcane', 'animation_tablet')]),
    # в оригинале стоит в той же клетке, что и «Магниты»; сдвинуто, чтобы значки не накладывались
    dict(key='REMOTE_PLACER', aspects={'machina': 2, 'motus': 1, 'sensus': 1}, column=-9, row=3, complexity=3, icon='remote_placer',
         concealed=True, parents=['ANIMATION_TABLET'], pages=text('REMOTE_PLACER', 0, 1) + [P('arcane', 'remote_placer')]),
]
RESEARCH += [
    dict(key='BLOOD_SWORD', aspects={'fames': 2, 'telum': 1, 'corpus': 1, 'spiritus': 1}, column=-4, row=6, complexity=3,
         icon='blood_sword', warp=1, secondary=True, parents=['CLEANSING_TALISMAN'],
         pages=text('BLOOD_SWORD', 0) + [P('infusion', 'infusion/blood_sword')] + text('BLOOD_SWORD', 1)),
    dict(key='SUMMON', aspects={'telum': 1, 'bestia': 3, 'praecantatio': 3}, column=-5, row=8, complexity=3, icon='summon', warp=3,
         parents=['BLOOD_SWORD'], pages=text('SUMMON', 0) + [P('arcane', 'summon'), P('infusion', 'infusion/infused_soul_aspect_bestia')]
         + text('SUMMON', 1)),
]
# чары зачарователя: ключ исследования, значок, место на карте, цена, родители
for key, icon, col, row, aspects, parents in [
        ('TTENCH_ASCENT_BOOST', 'ascent_boost', 6, 2, {'aer': 1, 'motus': 1, 'praecantatio': 2}, ['ENCHANTER']),
        ('TTENCH_SLOW_FALL', 'slow_fall', 7, 3, {'aer': 1, 'motus': 1, 'praecantatio': 2}, ['ENCHANTER']),
        ('TTENCH_AUTO_SMELT', 'auto_smelt', 7, 5, {'ignis': 1, 'perditio': 1, 'praecantatio': 2}, ['ENCHANTER']),
        ('TTENCH_DESINTEGRATE', 'desintegrate', 6, 6, {'perditio': 1, 'vacuos': 1, 'praecantatio': 2}, ['ENCHANTER']),
        ('TTENCH_QUICK_DRAW', 'quick_draw', 4, 6, {'sensus': 1, 'telum': 1, 'praecantatio': 2}, ['ENCHANTER']),
        ('TTENCH_VAMPIRISM', 'vampirism', 3, 5, {'fames': 1, 'telum': 1, 'praecantatio': 2}, ['ENCHANTER']),
        ('TTENCH_FOCUSED', 'focused_strikes', 2, 7, {'ordo': 1, 'telum': 1, 'praecantatio': 2}, ['TTENCH_VAMPIRISM']),
        ('TTENCH_DISPERSED', 'dispersed_strikes', 1, 6, {'perditio': 1, 'telum': 1, 'praecantatio': 2}, ['TTENCH_VAMPIRISM']),
        ('TTENCH_FINAL', 'final_strike', 0, 8, {'perditio': 3, 'ordo': 3, 'telum': 3, 'praecantatio': 2},
         ['TTENCH_FOCUSED', 'TTENCH_DISPERSED']),
        ('TTENCH_POUNCE', 'pounce', 7, 0, {'aer': 3, 'ordo': 3, 'tutamen': 3, 'praecantatio': 2}, ['TTENCH_ASCENT_BOOST']),
        ('TTENCH_SHATTER', 'shatter', 5, 8, {'terra': 3, 'perditio': 3, 'instrumentum': 1, 'praecantatio': 2}, ['TTENCH_DESINTEGRATE']),
        ('TTENCH_SHOCKWAVE', 'shockwave', 9, 2, {'aer': 3, 'perditio': 3, 'tutamen': 1, 'praecantatio': 2}, ['TTENCH_SLOW_FALL']),
        ('TTENCH_TUNNEL', 'tunnel', 9, 6, {'terra': 3, 'ordo': 3, 'instrumentum': 1, 'praecantatio': 2}, ['TTENCH_AUTO_SMELT']),
        ('TTENCH_VALIANCE', 'valiance', 1, 4, {'telum': 3, 'sano': 3, 'praecantatio': 2}, ['TTENCH_VAMPIRISM'])]:
    RESEARCH.append(dict(key=key, aspects=aspects, column=col, row=row, complexity=2, texture='enchants/' + icon,
                         secondary=True, parents=parents, pages=text(key, 0)))

# ---------------------------------------------------------------- рецепты
I = lambda item: {'item': item if ':' in item else NS + ':' + item}
T = lambda tag: {'tag': tag}
PRIMALS5 = {'aer': 5, 'ignis': 5, 'aqua': 5, 'terra': 5, 'ordo': 5, 'perditio': 5}


def shaped(result, pattern, key, count=1):
    return {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': key, 'result': dict(I(result), count=count)}


def arcane(result, research, pattern, key, aspects, count=1):
    return {'type': 'thaumcraft:arcane_shaped', 'pattern': pattern, 'key': key, 'result': dict(I(result), count=count),
            'aspects': aspects, 'research': research}


def crucible(result, research, catalyst, aspects, count=1):
    return {'type': 'thaumcraft:crucible', 'catalyst': catalyst, 'aspects': aspects,
            'result': dict(I(result), count=count), 'research': research}


def infusion(result, research, central, components, aspects, instability):
    return {'type': 'thaumcraft:infusion', 'central': central, 'components': components, 'aspects': aspects,
            'instability': instability, 'result': I(result), 'research': research}


RECIPES = {
    'dark_quartz': shaped('dark_quartz', ['Q Q', ' C ', 'Q Q'], {'Q': I('minecraft:quartz'), 'C': T('minecraft:coals')}, 4),
    'dark_quartz_block': shaped('dark_quartz_block', ['QQ', 'QQ'], {'Q': I('dark_quartz')}),
    'dark_quartz_slab': shaped('dark_quartz_slab', ['QQQ'], {'Q': I('dark_quartz_block')}, 6),
    'chiseled_dark_quartz_block': shaped('chiseled_dark_quartz_block', ['Q', 'Q'], {'Q': I('dark_quartz_slab')}),
    'dark_quartz_pillar': shaped('dark_quartz_pillar', ['Q', 'Q'], {'Q': I('dark_quartz_block')}, 2),
    'dark_quartz_stairs': shaped('dark_quartz_stairs', ['Q  ', 'QQ ', 'QQQ'], {'Q': I('dark_quartz_block')}, 4),
    'share_tome': shaped('share_tome', [' S ', 'PTP', ' P '], {'S': I('thaumcraft:scribing_tools'),
                                                              'T': I('thaumcraft:thaumonomicon'), 'P': I('minecraft:paper')}),
    'crucible/gaseous_light': crucible('gaseous_light', 'GASEOUS_LIGHT', I('thaumcraft:phial'), {'lux': 16, 'aer': 10, 'motus': 8}),
    'crucible/gaseous_shadow': crucible('gaseous_shadow', 'GASEOUS_SHADOW', I('thaumcraft:phial'),
                                        {'tenebrae': 16, 'aer': 10, 'motus': 8}),
    'gas_remover': arcane('gas_remover', 'GAS_REMOVER', ['DDD', 'L S', 'QQQ'],
                          {'D': I('dark_quartz'), 'L': I('gaseous_light'), 'S': I('gaseous_shadow'), 'Q': I('minecraft:quartz')},
                          {'aer': 2, 'ordo': 2}),
    'crucible/bright_nitor': crucible('bright_nitor', 'BRIGHT_NITOR', I('thaumcraft:nitor'),
                                      {'potentia': 25, 'lux': 25, 'aer': 10, 'ignis': 10}),
    'funnel': arcane('funnel', 'FUNNEL', ['STS'], {'S': I('minecraft:stone'), 'T': I('thaumcraft:thaumium_ingot')},
                     {'ordo': 1, 'perditio': 1}),
    'infusion/repairer': infusion('repairer', 'REPAIRER', I('thaumcraft:thaumium_block'),
                                  [I('minecraft:iron_ingot'), I('minecraft:gold_ingot'), I('minecraft:diamond'),
                                   I('minecraft:cobblestone'), T('minecraft:planks'), I('minecraft:leather'),
                                   I('thaumcraft:enchanted_fabric'), I('thaumcraft:thaumium_ingot')],
                                  {'instrumentum': 15, 'fabrico': 20, 'ordo': 10, 'praecantatio': 15}, 8),
    'crucible/spell_cloth': crucible('spell_cloth', 'SPELL_CLOTH', I('thaumcraft:enchanted_fabric'),
                                     {'praecantatio': 10, 'perditio': 6, 'permutatio': 4}),
    'spell_cloth_disenchant': {'type': NS + ':spell_cloth'},
    'infusion/xp_talisman': infusion('xp_talisman', 'XP_TALISMAN', I('minecraft:gold_ingot'),
                                     [I('minecraft:quartz'), I('minecraft:quartz'), I('thaumcraft:zombie_brain'), I('minecraft:diamond')],
                                     {'lucrum': 20, 'permutatio': 10, 'bestia': 10, 'machina': 5}, 6),
    'infusion/cleansing_talisman': infusion('cleansing_talisman', 'CLEANSING_TALISMAN', I('minecraft:ender_pearl'),
                                            [I('dark_quartz')] * 4 + [I('minecraft:ghast_tear'), I('thaumcraft:nitor')],
                                            {'sano': 10, 'instrumentum': 10, 'humanus': 20, 'victus': 10}, 5),
    'revealing_helm': arcane('revealing_helm', 'REVEALING_HELM', ['GH'], {'G': I('thaumcraft:goggles'),
                                                                         'H': I('thaumcraft:thaumium_helmet')}, PRIMALS5),
    'aspect_analyzer': arcane('aspect_analyzer', 'ASPECT_ANALYZER', ['TWT', 'WMW', 'TWT'],
                              {'W': I('thaumcraft:greatwood_planks'), 'M': I('thaumcraft:thaumometer'),
                               'T': I('thaumcraft:thaumium_ingot')}, {'ordo': 1, 'perditio': 1}),
    'platform': arcane('platform', 'PLATFORM', [' S ', 'G G'], {'G': I('thaumcraft:greatwood_planks'),
                                                                'S': I('thaumcraft:silverwood_planks')},
                       {'aer': 2, 'perditio': 4}, 2),
    'focus_smelt': arcane('focus_smelt', 'FOCUS_SMELT', ['FNE'], {'F': I('thaumcraft:focus_fire'), 'N': I('thaumcraft:nitor'),
                                                                 'E': I('thaumcraft:focus_excavation')}, {'ignis': 10, 'ordo': 5, 'perditio': 6}),
    'infusion/focus_flight': infusion('focus_flight', 'FOCUS_FLIGHT', I('minecraft:ender_pearl'),
                                      [I('minecraft:quartz')] * 4 + [I('minecraft:feather')] * 2 + [I('thaumcraft:shard_air')],
                                      {'aer': 15, 'motus': 20, 'iter': 10}, 3),
    'infusion/focus_telekinesis': infusion('focus_telekinesis', 'FOCUS_TELEKINESIS', I('minecraft:ender_pearl'),
                                           [I('minecraft:quartz')] * 4 + [I('minecraft:iron_ingot'), I('minecraft:gold_ingot'),
                                                                          I('thaumcraft:shard_air')],
                                           {'motus': 10, 'aer': 20, 'perditio': 20, 'cognitio': 10}, 5),
    'infusion/focus_deflect': infusion('focus_deflect', 'FOCUS_DEFLECT', I('focus_flight'),
                                       [I('thaumcraft:mirrored_glass')] * 2 + [I('thaumcraft:warding_paving'), I('thaumcraft:shard_order')],
                                       {'aer': 15, 'tutamen': 5, 'ordo': 20}, 5),
    'infusion/focus_dislocation': infusion('focus_dislocation', 'FOCUS_DISLOCATION', I('minecraft:ender_pearl'),
                                           [I('minecraft:quartz')] * 4 + [I('thaumcraft:amber')] * 3 + [I('minecraft:diamond')],
                                           {'alienis': 20, 'tenebrae': 10, 'vacuos': 25, 'praecantatio': 20, 'vitium': 5}, 8),
    'infusion/focus_heal': infusion('focus_heal', 'FOCUS_HEAL', I('thaumcraft:focus_pech'),
                                    [I('minecraft:golden_carrot')] + [I('minecraft:gold_nugget')] * 3,
                                    {'sano': 10, 'spiritus': 10, 'victus': 15}, 4),
    'focus_ender_chest': arcane('focus_ender_chest', 'FOCUS_ENDER_CHEST', ['M', 'E', 'P'],
                                {'M': I('thaumcraft:magic_mirror'), 'E': I('minecraft:ender_eye'), 'P': I('thaumcraft:focus_portable_hole')},
                                {'ordo': 10, 'perditio': 10}),
    'transvector_interface': arcane('transvector_interface', 'INTERFACE', ['BRB', 'LEL', 'BRB'],
                                    {'B': I('thaumcraft:arcane_stone'), 'E': I('minecraft:ender_pearl'), 'L': I('minecraft:lapis_lazuli'),
                                     'R': I('minecraft:redstone')}, {'ordo': 12, 'perditio': 16}),
    'connector': arcane('connector', 'INTERFACE', [' I ', ' WI', 'S  '],
                        {'I': I('minecraft:iron_ingot'), 'W': I('minecraft:stick'), 'S': I('thaumcraft:shard_order')}, {'ordo': 2}),
    'transvector_dislocator': arcane('transvector_dislocator', 'DISLOCATOR', ['M', 'I', 'C'],
                                     {'M': I('thaumcraft:mirrored_glass'), 'I': I('transvector_interface'), 'C': I('minecraft:comparator')},
                                     {'terra': 5, 'perditio': 5}),
    'magnet': arcane('magnet', 'MAGNETS', [' I ', 'SIs', 'WFW'],
                     {'I': I('minecraft:iron_ingot'), 's': I('thaumcraft:shard_earth'), 'S': I('thaumcraft:shard_air'),
                      'W': I('thaumcraft:greatwood_log'), 'F': I('focus_telekinesis')}, {'aer': 20, 'ordo': 5, 'terra': 15, 'perditio': 5}),
    'mob_magnet': arcane('mob_magnet', 'MAGNETS', [' G ', 'SGs', 'WFW'],
                         {'G': T('forge:ingots/gold'), 's': I('thaumcraft:shard_earth'), 'S': I('thaumcraft:shard_air'),
                          'W': I('thaumcraft:greatwood_log'), 'F': I('focus_telekinesis')}, {'aer': 20, 'ordo': 5, 'terra': 15, 'perditio': 5}),
    'crucible/soul_mould': crucible('soul_mould', 'MAGNETS', I('minecraft:ender_pearl'), {'bestia': 4, 'cognitio': 8, 'sensus': 8}),
    'infusion/mobilizer': infusion('mobilizer', 'LEVITATOR', I('thaumcraft:levitator'),
                                   [I('minecraft:iron_ingot'), I('minecraft:feather'), I('minecraft:iron_ingot'), I('thaumcraft:obsidian_tile')],
                                   {'motus': 15, 'ordo': 20, 'praecantatio': 15}, 4),
    'mobilizer_relay': arcane('mobilizer_relay', 'LEVITATOR', ['WFW', 'SIs', 'WFW'],
                              {'I': I('minecraft:iron_ingot'), 's': I('thaumcraft:shard_earth'), 'S': I('thaumcraft:shard_air'),
                               'W': I('thaumcraft:greatwood_log'), 'F': I('minecraft:glass')}, {'aer': 20, 'ordo': 5, 'terra': 15}),
    'animation_tablet': arcane('animation_tablet', 'ANIMATION_TABLET', ['GIG', 'ICI'],
                               {'G': I('minecraft:gold_ingot'), 'I': I('minecraft:iron_ingot'), 'C': I('thaumcraft:golem_core_blank')},
                               {'aer': 25, 'ordo': 15, 'ignis': 10}),
    'remote_placer': arcane('remote_placer', 'REMOTE_PLACER', ['ses', 'sds', 'sss'],
                            {'s': I('thaumcraft:pedestal'), 'e': I('minecraft:ender_pearl'), 'd': I('minecraft:dispenser')},
                            {'aer': 20, 'ordo': 5, 'terra': 15, 'perditio': 5}),
    'infusion/blood_sword': infusion('blood_sword', 'BLOOD_SWORD', I('thaumcraft:thaumium_sword'),
                                     [I('minecraft:rotten_flesh'), I('minecraft:porkchop'), I('minecraft:beef'), I('minecraft:bone'),
                                      I('minecraft:diamond'), I('minecraft:ghast_tear')],
                                     {'fames': 20, 'tenebrae': 5, 'spiritus': 10, 'humanus': 6}, 6),
    'summon': arcane('summon', 'SUMMON', ['WWW', 'SSS'], {'W': I('thaumcraft:obsidian_tile'), 'S': I('minecraft:stone')},
                     {'ordo': 50, 'perditio': 50}),
    'condense_soul': {'type': NS + ':condense_soul'},
    'infusion/enchanter': infusion('enchanter', 'ENCHANTER', I('minecraft:enchanting_table'),
                                   [I('thaumcraft:obsidian_tile')] * 5 + [I('thaumcraft:thaumium_ingot')] * 2 + [I('spell_cloth')],
                                   {'praecantatio': 50, 'potentia': 20, 'alienis': 20, 'vacuos': 20, 'cognitio': 10}, 15),
}

# наполненные аспекты души: из девяти сгущённых, по одному рецепту на аспект
def soul(item, aspect):
    return {'type': 'forge:nbt', 'item': NS + ':' + item, 'nbt': '{aspect:"%s"}' % aspect, 'count': 1}


for aspect in SOUL_ASPECTS:
    RECIPES['infusion/infused_soul_aspect_' + aspect] = {
        'type': 'thaumcraft:infusion', 'central': soul('condensed_soul_aspect', aspect),
        'components': [soul('condensed_soul_aspect', aspect)] * 8, 'aspects': {aspect: 10}, 'instability': 4,
        'result': {'item': NS + ':infused_soul_aspect', 'nbt': '{aspect:"%s"}' % aspect}, 'research': 'SUMMON'}

# ---------------------------------------------------------------- часть 2г: стихийные огни, наполненные культуры
# наш суффикс -> (имя огня в оригинале, осколок, аспекты тигля)
FIRES = {
    'aer': ('Air', 'shard_air', {'ignis': 5, 'praecantatio': 5, 'aer': 5}),
    'ignis': ('Fire', 'shard_fire', {'ignis': 10, 'aer': 5}),
    'aqua': ('Water', 'shard_water', {'ignis': 5, 'praecantatio': 5, 'aqua': 5}),
    'terra': ('Earth', 'shard_earth', {'ignis': 5, 'praecantatio': 5, 'terra': 5}),
    'ordo': ('Order', 'shard_order', {'ignis': 5, 'praecantatio': 5, 'ordo': 5}),
    'perditio': ('Chaos', 'shard_entropy', {'ignis': 5, 'praecantatio': 5, 'perditio': 5}),
}
CROP_TEXTURES = ['aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio', 'generic']
GRAINS = ['aer', 'ignis', 'terra', 'aqua']
EFFECTS = {'aer': 'air_imbued', 'ignis': 'fire_imbued', 'terra': 'earth_imbued', 'aqua': 'water_imbued'}
for name, (orig, shard, aspects) in FIRES.items():
    for layer in (0, 1):
        TEXTURES['block/fire_%s_layer_%d' % (name, layer)] = 'blocks/fire%s_layer_%d.png' % (orig, layer)
    TEXTURES['item/fire_' + name] = 'blocks/%sFire.png' % name
    ITEM_NAMES['block.thaumictinkerer.fire_' + name] = 'tile.fire%s.name' % orig
    RECIPES['crucible/fire_' + name] = crucible('fire_' + name, 'FIRE_' + name.upper(), I('thaumcraft:' + shard), aspects)
for name in CROP_TEXTURES:
    for stage in range(4):
        TEXTURES['block/crop/%s_%d' % (name, stage)] = 'blocks/crop_%s_%d.png' % (name, stage)
    TEXTURES['item/seed/' + name] = 'items/seed_%s.png' % name
TEXTURES['block/infused_farmland'] = 'blocks/infusedFarmland.png'
for name in GRAINS:
    TEXTURES['item/infused_grain_' + name] = 'items/fruit_%s.png' % name
    TEXTURES['item/infused_potion_' + name] = 'items/potion_%s.png' % name
    TEXTURES['mob_effect/' + EFFECTS[name]] = 'items/potion_%s.png' % name
    ITEM_NAMES['item.thaumictinkerer.infused_grain_' + name] = 'item.infusedGrain.name'
    ITEM_NAMES['item.thaumictinkerer.infused_potion_' + name] = 'item.infusedPotion.%s.name' % name.capitalize()
    RECIPES['crucible/infused_potion_' + name] = crucible('infused_potion_' + name, 'INFUSED_POTIONS', I('infused_grain_' + name),
                                                          {'auram': 5, name: 5})
ITEM_NAMES.update({'item.thaumictinkerer.infused_seeds': 'item.infusedSeeds.name', 'block.thaumictinkerer.forcefield': 'tile.forcefield.name'})
for name, (orig, shard, aspects) in FIRES.items():
    RECIPES['infusion/infused_seeds_' + name] = {
        'type': 'thaumcraft:infusion', 'central': I('minecraft:wheat_seeds'), 'components': [I('thaumcraft:' + shard)] * 4,
        'aspects': {'messis': 32, 'meto': 32}, 'instability': 5,
        'result': {'item': NS + ':infused_seeds', 'nbt': '{aspect:"%s"}' % name}, 'research': 'INFUSED_POTIONS'}
# место на карте и сложность — как в оригинале
for name, col, row, complexity, aspects in [('aer', 3, -7, 2, {'ignis': 5, 'aer': 5}), ('perditio', 2, -8, 3, {'ignis': 5, 'perditio': 5}),
                                            ('terra', 4, -6, 2, {'ignis': 5, 'terra': 5}), ('ignis', 4, -4, 2, {'ignis': 10}),
                                            ('ordo', 3, -3, 2, {'ignis': 5, 'ordo': 5}), ('aqua', 2, -2, 2, {'ignis': 5, 'aqua': 5})]:
    key = 'FIRE_' + name.upper()
    RESEARCH.append(dict(key=key, aspects=aspects, column=col, row=row, complexity=complexity, icon='fire_' + name, concealed=True,
                         secondary=True, parents=['BRIGHT_NITOR'], pages=text(key, 0) + [P('crucible', 'crucible/fire_' + name)]))
RESEARCH.append(dict(key='INFUSED_POTIONS', aspects={'aqua': 5, 'perditio': 5}, column=7, row=-5, complexity=2, texture='item/seed/ignis',
                     concealed=True, parents=['FIRE_' + n.upper() for n in ('perditio', 'ordo', 'ignis', 'terra', 'aer', 'aqua')],
                     parents_hidden=['INFUSION'],
                     pages=text('INFUSED_POTIONS', 0, 1, 2, 3, 4, 5) + [P('infusion', 'infusion/infused_seeds_' + n) for n in FIRES]
                     + [P('crucible', 'crucible/infused_potion_' + n) for n in GRAINS]))
FLAT_2D = ['fire_' + n for n in FIRES] + ['infused_grain_' + n for n in GRAINS] + ['infused_potion_' + n for n in GRAINS]
SHOVEL = ['infused_farmland']


def gen_models_2d(models, states):
    cut = 'minecraft:cutout'
    for name in FIRES:
        parts = {}
        for layer in (0, 1):
            tex = {'fire': ns('block/fire_%s_layer_%d' % (name, layer)), 'particle': ns('block/fire_%s_layer_%d' % (name, layer))}
            for kind in ('floor', 'side', 'side_alt'):
                model = 'fire_%s_%s%d' % (name, kind, layer)
                write(os.path.join(models, 'block', model + '.json'),
                      {'parent': 'minecraft:block/template_fire_' + kind, 'textures': tex, 'render_type': cut})
                parts.setdefault(kind, []).append({'model': ns('block/' + model)})
        sides = parts['side'] + parts['side_alt']
        write(os.path.join(states, 'fire_%s.json' % name), {'multipart': [{'apply': parts['floor']}] + [
            {'apply': [dict(m, y=y) if y else m for m in sides]} for y in (0, 90, 180, 270)]})
    variants = {}
    for t, name in enumerate(CROP_TEXTURES):
        for stage in range(4):
            write(os.path.join(models, 'block', 'crop', '%s_%d.json' % (name, stage)),
                  {'parent': 'minecraft:block/crop', 'textures': {'crop': ns('block/crop/%s_%d' % (name, stage))}, 'render_type': cut})
        for age in range(8):
            stage = 3 if age == 7 else (5 if age == 6 else age) >> 1
            variants['age=%d,texture=%d' % (age, t)] = {'model': ns('block/crop/%s_%d' % (name, stage))}
    write(os.path.join(states, 'infused_grain.json'), {'variants': variants})
    write(os.path.join(models, 'block', 'infused_farmland.json'), {'parent': 'minecraft:block/template_farmland', 'textures': {
        'dirt': 'minecraft:block/dirt', 'top': ns('block/infused_farmland')}})
    write(os.path.join(states, 'infused_farmland.json'),
          {'variants': {'moisture=%d' % m: {'model': ns('block/infused_farmland')} for m in range(8)}})
    write(os.path.join(states, 'forcefield.json'), {'variants': {'': {'model': ns('block/empty')}}})
    overrides = []
    for t, name in enumerate(CROP_TEXTURES):
        write(os.path.join(models, 'item', 'seed', name + '.json'),
              {'parent': 'minecraft:item/generated', 'textures': {'layer0': ns('item/seed/' + name)}})
        if t:
            overrides.append({'predicate': {ns('seed'): t}, 'model': ns('item/seed/' + name)})
    write(os.path.join(models, 'item', 'infused_seeds.json'), {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': ns('item/seed/aer')}, 'overrides': overrides})
    # зерно несёт свой аспект и урожай, как IEssentiaContainerItem оригинала
    write(os.path.join(DATA, 'thaumcraft_aspects', 'infused_grain.json'), {'entries': [
        {'item': ns('infused_grain_' + n), 'aspects': {n: 1, 'messis': 1}} for n in GRAINS]})
    write(os.path.join(DATA, 'loot_tables', 'blocks', 'infused_farmland.json'), {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:dirt'}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    write(os.path.join(OUT, 'data', 'minecraft', 'tags', 'blocks', 'mineable', 'shovel.json'),
          {'replace': False, 'values': [ns(b) for b in SHOVEL]})


# ---------------------------------------------------------------- часть 3а: Ками — ихор и первые вещи из него
# наш id -> (картинка оригинала, ключ .lang оригинала)
KAMI_ITEMS = {
    'ichor': 'ichor', 'ichor_cloth': 'ichorCloth', 'ichorium': 'ichorium', 'ichorium_nugget': 'ichorNugget',
    'ichorium_cap': 'ichorCap', 'ichorcloth_rod': 'ichorclothRod', 'nether_shard': 'netherShard', 'ender_shard': 'enderShard',
    'ichor_pouch': 'ichorPouch', 'cat_amulet': 'catAmulet',
}
for ours, theirs in KAMI_ITEMS.items():
    TEXTURES['item/' + ours] = 'items/%s.png' % theirs
    ITEM_NAMES['item.thaumictinkerer.' + ours] = 'item.%s.name' % theirs
TEXTURES.update({'models/wand_cap_ichor': 'model/capIchor.png', 'models/wand_rod_ichorcloth': 'model/rodIchorcloth.png',
                 'gui/ichor_pouch': 'gui/ichorPouch.png'})
ITEM_NAMES.update({'item.Wand.ichor.cap': 'item.Wand.ICHOR.cap', 'item.Wand.ichorcloth.rod': 'item.Wand.ICHORCLOTH.rod'})
# исследования набалдашника и стержня называются по их тегу, как CAP_gold у Thaumcraft
RESEARCH_LANG_ALIAS = {'CAP_ICHOR': 'CAP_ichor', 'ROD_ICHORCLOTH': 'ROD_ichorcloth'}
# KamiResearchItem: скрыто требует все обычные исследования этих разделов
KAMI_ALL = {'categories': ['TT_CATEGORY', 'BASICS', 'GOLEMANCY', 'ARTIFICE', 'ALCHEMY', 'THAUMATURGY'], 'exclude': ['MINILITH']}
PRIMALS = lambda n: {a: n for a in ('aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio')}


def kami(key, aspects, col, row, icon, parents=None, hidden=None, warp=0, pages=None, page_key=None):
    return dict(key=key, aspects=aspects, column=col, row=row, complexity=5, icon=icon, concealed=True, warp=warp,
                parents=parents or [], parents_hidden=hidden or [], requires_all=KAMI_ALL,
                pages=text(page_key or key, 0) + (pages or []))


FLAT_2D += list(KAMI_ITEMS)
RESEARCH += [
    dict(key='DIMENSION_SHARDS', aspects={}, column=7, row=8, complexity=0, icon='ender_shard', stub=True, autoUnlock=True, round=True,
         concealed=True, pages=text('DIMENSION_SHARDS', 0)),
    kami('ICHOR', {'humanus': 1, 'lux': 2, 'spiritus': 1, 'vitium': 1}, 9, 8, 'ichor', warp=5, pages=[P('infusion', 'infusion/ichor')]),
    kami('ICHOR_CLOTH', {'pannus': 2, 'lux': 1, 'fabrico': 1, 'sensus': 1}, 11, 7, 'ichor_cloth', ['ICHOR'],
         pages=[P('arcane', 'ichor_cloth')]),
    kami('ICHORIUM', {'metallum': 2, 'lux': 1, 'fabrico': 1, 'instrumentum': 1}, 11, 9, 'ichorium', ['ICHOR'], ['ICHOR_CLOTH'],
         pages=[P('arcane', 'ichorium')]),
    kami('CAP_ichor', {'instrumentum': 2, 'metallum': 1, 'lux': 1, 'praecantatio': 1}, 11, 11, 'ichorium_cap', ['ICHORIUM'], warp=2,
         pages=[P('arcane', 'ichorium_cap')], page_key='CAP_ICHOR'),
    kami('ROD_ichorcloth', {'instrumentum': 2, 'pannus': 1, 'lux': 1, 'praecantatio': 1}, 14, 2, 'ichorcloth_rod', ['ICHOR_CLOTH'],
         ['CAP_ichor'], warp=1, pages=[P('infusion', 'infusion/ichorcloth_rod')], page_key='ROD_ICHORCLOTH'),
    kami('ICHOR_POUCH', {'vacuos': 2, 'pannus': 1, 'alienis': 1, 'humanus': 1}, 13, 6, 'ichor_pouch', ['ICHOR_CLOTH'],
         pages=[P('infusion', 'infusion/ichor_pouch')]),
    kami('CAT_AMULET', {'cognitio': 2, 'ordo': 1, 'tenebrae': 1, 'mortuus': 1}, 13, 10, 'cat_amulet', ['ICHORIUM'],
         pages=[P('infusion', 'infusion/cat_amulet')]),
]
RECIPES.update({
    'infusion/ichor': dict(infusion('ichor', 'ICHOR', I('minecraft:nether_star'),
                                    [I('minecraft:diamond'), I('ender_shard'), I('minecraft:ender_eye'), I('nether_shard')],
                                    {'humanus': 32, 'lux': 32, 'spiritus': 64}, 7), result=dict(I('ichor'), count=8)),
    'ichor_cloth': arcane('ichor_cloth', 'ICHOR_CLOTH', ['CCC', 'III', 'DDD'], {
        'C': I('thaumcraft:enchanted_fabric'), 'I': I('ichor'), 'D': I('minecraft:diamond')}, PRIMALS(125), 3),
    'ichorium': arcane('ichorium', 'ICHORIUM', [' T ', 'IDI', ' I '], {
        'T': I('thaumcraft:thaumium_ingot'), 'I': I('ichor'), 'D': I('minecraft:diamond')}, PRIMALS(100)),
    'ichorium_nugget': {'type': 'minecraft:crafting_shapeless', 'ingredients': [I('ichorium')], 'result': dict(I('ichorium_nugget'), count=9)},
    'ichorium_cap': arcane('ichorium_cap', 'CAP_ichor', ['ICI', ' M ', 'ICI'], {
        'M': I('ichorium'), 'I': I('ichor'), 'C': I('thaumcraft:wand_cap_thaumium')}, PRIMALS(100), 2),
    'infusion/ichorcloth_rod': infusion('ichorcloth_rod', 'ROD_ichorcloth', I('thaumcraft:wand_rod_silverwood'),
                                        [I('ichor'), I('ichor_cloth'), I('thaumcraft:salis_mundus'), I('minecraft:ghast_tear'),
                                         I('thaumcraft:salis_mundus'), I('ichor_cloth')],
                                        {'praecantatio': 100, 'lux': 32, 'instrumentum': 32}, 9),
    'infusion/ichor_pouch': infusion('ichor_pouch', 'ICHOR_POUCH', I('thaumcraft:focus_pouch'),
                                     [I('ichor_cloth'), I('thaumcraft:focus_portable_hole'), I('minecraft:diamond'), I('ichor_cloth'),
                                      I('thaumcraft:hungry_chest'), I('thaumcraft:void_jar')],
                                     {'vacuos': 64, 'humanus': 32, 'pannus': 32, 'alienis': 32, 'aer': 64}, 9),
    'infusion/cat_amulet': infusion('cat_amulet', 'CAT_AMULET', I('minecraft:quartz_block'),
                                    [I('ichor'), I('minecraft:gold_ingot'), I('minecraft:gold_ingot'), I('minecraft:cocoa_beans'),
                                     I('minecraft:jungle_leaves'), I('minecraft:cod')],
                                    {'tenebrae': 16, 'ordo': 32, 'cognitio': 16}, 8),
})

# ---------------------------------------------------------------- часть 3б: ихорные инструменты, протоглина, кольцо, зеркало
TOOLS_3B = {'pick': 'Pick', 'shovel': 'Shovel', 'axe': 'Axe', 'sword': 'Sword'}
for tool, orig in TOOLS_3B.items():
    TEXTURES['item/ichor_' + tool] = 'items/ichor%s.png' % orig
    ITEM_NAMES['item.thaumictinkerer.ichor_' + tool] = 'item.ichor%s.name' % orig
    ITEM_NAMES['item.thaumictinkerer.ichor_%s_gem' % tool] = 'item.ichor%sGem.name' % orig
    for mode in range(3):
        TEXTURES['item/ichor_%s_gem_%d' % (tool, mode)] = 'items/ichor%sGem%d.png' % (orig, mode)
for ours, theirs in {'protoclay': 'protoclay', 'block_talisman': 'blockTalisman', 'placement_mirror': 'placementMirror'}.items():
    ITEM_NAMES['item.thaumictinkerer.' + ours] = 'item.%s.name' % theirs
TEXTURES.update({'item/protoclay': 'items/protoclay.png', 'item/block_talisman': 'items/blockTalisman0.png',
                 'item/block_talisman_active': 'items/blockTalisman1.png', 'item/placement_mirror_0': 'items/placementMirror0.png',
                 'item/placement_mirror_1': 'items/placementMirror1.png', 'gui/soul_hearts': 'gui/soulHearts.png'})
FLAT_2D.append('protoclay')
ROD = I('thaumcraft:wand_rod_silverwood')
CLUSTERS = [I('thaumcraft:cluster_cinnabar'), I('thaumcraft:cluster_iron'), I('thaumcraft:cluster_gold')]
RECIPES.update({
    'ichor_pick': arcane('ichor_pick', 'ICHOR_TOOLS', ['III', ' R ', ' R '], {'I': I('ichorium'), 'R': ROD}, {'ignis': 75}),
    'ichor_shovel': arcane('ichor_shovel', 'ICHOR_TOOLS', [' I ', ' R ', ' R '], {'I': I('ichorium'), 'R': ROD}, {'terra': 75}),
    'ichor_axe': arcane('ichor_axe', 'ICHOR_TOOLS', ['II ', 'IR ', ' R '], {'I': I('ichorium'), 'R': ROD}, {'aqua': 75}),
    'ichor_sword': arcane('ichor_sword', 'ICHOR_TOOLS', [' I ', ' I ', ' R '], {'I': I('ichorium'), 'R': ROD}, {'aer': 75}),
})
for tool, special, extra, aspects in [
        ('pick', I('thaumcraft:focus_excavation'), I('minecraft:tnt'),
         {'ignis': 50, 'perfodio': 64, 'metallum': 32, 'terra': 32, 'meto': 32, 'lucrum': 16, 'sensus': 16}),
        ('shovel', I('thaumcraft:focus_excavation'), I('minecraft:tnt'),
         {'terra': 82, 'perfodio': 64, 'instrumentum': 32, 'meto': 32, 'vinculum': 16, 'sensus': 16}),
        ('axe', I('thaumcraft:focus_excavation'), I('minecraft:tnt'),
         {'aqua': 50, 'perfodio': 64, 'instrumentum': 32, 'arbor': 32, 'meto': 32, 'messis': 16, 'sensus': 16}),
        ('sword', I('thaumcraft:focus_frost'), I('minecraft:cactus'),
         {'aer': 50, 'fames': 64, 'spiritus': 32, 'telum': 32, 'potentia': 32, 'ordo': 16, 'vitreus': 16})]:
    elemental = I('thaumcraft:elemental_' + ('pickaxe' if tool == 'pick' else tool))
    RECIPES['infusion/ichor_%s_gem' % tool] = infusion(
        'ichor_%s_gem' % tool, 'ICHOR_%s_GEM' % tool.upper(), I('ichor_' + tool),
        [I('ichorium'), I('ichor'), elemental, special, extra] + CLUSTERS + [I('minecraft:diamond'), special, elemental, I('ichor_cloth')],
        aspects, 15)
RECIPES.update({
    'infusion/protoclay': infusion('protoclay', 'PROTOCLAY', I('minecraft:clay_ball'),
                                   [I('minecraft:dirt'), I('minecraft:stone'), I('minecraft:oak_log'), I('ender_shard')],
                                   {'perfodio': 16, 'instrumentum': 16}, 4),
    'infusion/block_talisman': infusion('block_talisman', 'BLOCK_TALISMAN', I('thaumcraft:focus_portable_hole'),
                                        [I('ichor'), I('minecraft:ender_chest'), I('minecraft:diamond'), I('ichor'),
                                         I('thaumcraft:taint_goo'), I('thaumcraft:void_jar')],
                                        {'vacuos': 65, 'tenebrae': 32, 'praecantatio': 50, 'alienis': 32}, 9),
    'infusion/placement_mirror': infusion('placement_mirror', 'PLACEMENT_MIRROR', I('block_talisman'),
                                          [I('ichor'), I('minecraft:dropper'), I('minecraft:diamond'), I('minecraft:glass'),
                                           I('minecraft:blaze_powder'), I('ichor')],
                                          {'fabrico': 65, 'vitreus': 32, 'praecantatio': 50, 'cognitio': 32}, 12),
})
RESEARCH += [
    kami('ICHOR_TOOLS', {'instrumentum': 2, 'telum': 1, 'metallum': 1, 'fabrico': 1}, 13, 12, 'ichor_pick', ['ICHORIUM'],
         ['ROD_ichorcloth'], warp=2, pages=[P('arcane', 'ichor_' + t) for t in TOOLS_3B]),
    kami('ICHOR_PICK_GEM', {'ignis': 2, 'instrumentum': 1, 'perfodio': 1, 'terra': 1}, 13, 15, 'ichor_pick_gem', ['ICHOR_TOOLS'],
         pages=[P('infusion', 'infusion/ichor_pick_gem')] + text('ICHOR_PICK_GEM', 1)),
    kami('ICHOR_SHOVEL_GEM', {'terra': 3, 'instrumentum': 1, 'perfodio': 1}, 15, 15, 'ichor_shovel_gem', ['ICHOR_TOOLS'],
         pages=[P('infusion', 'infusion/ichor_shovel_gem')]),
    kami('ICHOR_AXE_GEM', {'aqua': 2, 'instrumentum': 1, 'arbor': 1, 'messis': 1}, 16, 14, 'ichor_axe_gem', ['ICHOR_TOOLS'],
         pages=[P('infusion', 'infusion/ichor_axe_gem')]),
    kami('ICHOR_SWORD_GEM', {'aer': 2, 'telum': 1, 'spiritus': 1, 'fames': 1}, 16, 12, 'ichor_sword_gem', ['ICHOR_TOOLS'],
         pages=[P('infusion', 'infusion/ichor_sword_gem')] + text('ICHOR_SWORD_GEM', 1)),
    kami('PROTOCLAY', {'instrumentum': 2, 'perfodio': 1, 'humanus': 1, 'machina': 1}, 12, 17, 'protoclay', ['ICHOR_PICK_GEM'],
         ['ICHOR_SHOVEL_GEM'], pages=[P('infusion', 'infusion/protoclay')]),
    kami('BLOCK_TALISMAN', {'vacuos': 2, 'tenebrae': 1, 'alienis': 1, 'praecantatio': 1}, 14, 17, 'block_talisman',
         ['ICHOR_PICK_GEM', 'ICHOR_SHOVEL_GEM'], pages=[P('infusion', 'infusion/block_talisman')]),
    kami('PLACEMENT_MIRROR', {'fabrico': 2, 'vitreus': 1, 'alienis': 1, 'cognitio': 1}, 17, 16, 'placement_mirror', ['BLOCK_TALISMAN'],
         pages=[P('infusion', 'infusion/placement_mirror')]),
]


def gen_models_3b(models, states):
    for tool in TOOLS_3B:
        write(os.path.join(models, 'item', 'ichor_%s.json' % tool),
              {'parent': 'minecraft:item/handheld', 'textures': {'layer0': ns('item/ichor_' + tool)}})
        for mode in (1, 2):
            write(os.path.join(models, 'item', 'ichor_%s_gem_%d.json' % (tool, mode)),
                  {'parent': 'minecraft:item/handheld', 'textures': {'layer0': ns('item/ichor_%s_gem_%d' % (tool, mode))}})
        write(os.path.join(models, 'item', 'ichor_%s_gem.json' % tool), {
            'parent': 'minecraft:item/handheld', 'textures': {'layer0': ns('item/ichor_%s_gem_0' % tool)},
            'overrides': [{'predicate': {ns('mode'): m}, 'model': ns('item/ichor_%s_gem_%d' % (tool, m))} for m in (1, 2)]})
    write(os.path.join(models, 'item', 'block_talisman_active.json'),
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': ns('item/block_talisman_active')}})
    write(os.path.join(models, 'item', 'block_talisman.json'), {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': ns('item/block_talisman')}, 'overrides': [{'predicate': {ns('active'): 1}, 'model': ns('item/block_talisman_active')}]})
    write(os.path.join(models, 'item', 'placement_mirror.json'), {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': ns('item/placement_mirror_0'), 'layer1': ns('item/placement_mirror_1')}})


# ---------------------------------------------------------------- часть 3в: мантия, врата искривления, фокусы Ками
ARMOR_3C = {'helm': 'Helm', 'chest': 'Chest', 'legs': 'Legs', 'boots': 'Boots'}
for piece, orig in ARMOR_3C.items():
    TEXTURES['item/ichorcloth_' + piece] = 'items/ichorcloth%s.png' % orig
    TEXTURES['item/ichorcloth_%s_gem' % piece] = 'items/ichorcloth%sGem.png' % orig
    ITEM_NAMES['item.thaumictinkerer.ichorcloth_' + piece] = 'item.ichorcloth%s.name' % orig
    ITEM_NAMES['item.thaumictinkerer.ichorcloth_%s_gem' % piece] = 'item.ichorcloth%sGem.name' % orig
    FLAT_2D += ['ichorcloth_' + piece, 'ichorcloth_%s_gem' % piece]
TEXTURES.update({
    'models/armor/ichor_layer_1': 'model/ichor1.png', 'models/armor/ichor_layer_2': 'model/ichor2.png',
    'models/armor/ichor_gem_layer_1': 'model/ichorGem1.png', 'models/armor/ichor_gem_layer_2': 'model/ichorGem2.png',
    'item/sky_pearl': 'items/skyPearl.png', 'block/warp_gate_top': 'blocks/warpGate0.png', 'block/warp_gate_side': 'blocks/warpGate1.png',
    'gui/warp_gate': 'gui/warpGate.png',
    'item/focus_recall': 'items/focusRecall.png', 'unstitched/item/focus_recall_depth': 'items/focusRecallDepth.png',
    'item/focus_shadowbeam': 'items/focusShadowbeam.png', 'item/focus_shadowbeam_orn': 'items/focusShadowbeamOrn.png',
    'item/focus_xp_drain': 'items/focusXPDrain.png',
})
ITEM_NAMES.update({'item.thaumictinkerer.sky_pearl': 'item.skyPearl.name', 'block.thaumictinkerer.warp_gate': 'tile.warpGate.name',
                   'item.thaumictinkerer.focus_recall': 'item.focusRecall.name',
                   'item.thaumictinkerer.focus_shadowbeam': 'item.focusShadowbeam.name',
                   'item.thaumictinkerer.focus_xp_drain': 'item.focusXPDrain.name'})
FLAT_2D += ['sky_pearl', 'focus_recall', 'focus_shadowbeam', 'focus_xp_drain']
ITEM_NAMES.update({'key.thaumictinkerer.armor': 'ttmisc.toggleArmor', 'key.categories.thaumictinkerer': 'ttmisc.keyCategory'})
CLOTH = {'C': I('ichor_cloth')}
RECIPES.update({
    'ichorcloth_helm': arcane('ichorcloth_helm', 'ICHORCLOTH_ARMOR', ['CCC', 'C C'], CLOTH, {'aqua': 75}),
    'ichorcloth_chest': arcane('ichorcloth_chest', 'ICHORCLOTH_ARMOR', ['C C', 'CCC', 'CCC'], CLOTH, {'aer': 75}),
    'ichorcloth_legs': arcane('ichorcloth_legs', 'ICHORCLOTH_ARMOR', ['CCC', 'C C', 'C C'], CLOTH, {'ignis': 75}),
    'ichorcloth_boots': arcane('ichorcloth_boots', 'ICHORCLOTH_ARMOR', ['C C', 'C C'], CLOTH, {'terra': 75}),
})


def potion(name):
    return {'type': 'forge:nbt', 'item': 'minecraft:potion', 'nbt': '{Potion:"minecraft:%s"}' % name}


GEM_BASE = [I('minecraft:diamond'), I('ichor'), I('ichor')]
for piece, extra, aspects in [
        ('helm', [I('thaumcraft:thaumonomicon'), I('thaumcraft:focus_primal'), I('minecraft:golden_helmet'), potion('night_vision'),
                  I('thaumcraft:goggles'), I('minecraft:ghast_tear'), I('minecraft:cod'), I('minecraft:cake'), I('minecraft:ender_eye')],
         {'aqua': 50, 'tutamen': 32, 'fames': 32, 'auram': 32, 'lux': 64, 'corpus': 16, 'cognitio': 16}),
        ('chest', [I('thaumcraft:focus_primal'), I('thaumcraft:thaumonomicon'), I('minecraft:golden_chestplate'), I('focus_flight'),
                   I('thaumcraft:hover_harness'), I('focus_deflect'), I('minecraft:feather'), I('minecraft:ghast_tear'), I('minecraft:arrow')],
         {'aer': 50, 'tutamen': 32, 'volatus': 32, 'ordo': 32, 'lux': 64, 'alienis': 16, 'sensus': 16}),
        ('legs', [I('thaumcraft:focus_primal'), I('thaumcraft:thaumonomicon'), I('minecraft:golden_chestplate'), potion('fire_resistance'),
                  I('focus_smelt'), I('bright_nitor'), I('minecraft:lava_bucket'), I('minecraft:fire_charge'), I('minecraft:blaze_rod')],
         {'ignis': 50, 'tutamen': 32, 'sano': 32, 'potentia': 32, 'lux': 64, 'lucrum': 16, 'alienis': 16}),
        ('boots', [I('thaumcraft:thaumonomicon'), I('thaumcraft:focus_primal'), I('minecraft:golden_boots'), I('minecraft:grass_block'),
                   I('thaumcraft:arcane_bore'), I('thaumcraft:lamp_growth'), I('minecraft:wheat_seeds'), I('minecraft:white_wool'),
                   I('minecraft:lead')],
         {'terra': 50, 'tutamen': 32, 'perfodio': 32, 'motus': 32, 'lux': 64, 'herba': 16, 'iter': 16})]:
    RECIPES['infusion/ichorcloth_%s_gem' % piece] = infusion('ichorcloth_%s_gem' % piece, 'ICHORCLOTH_%s_GEM' % piece.upper(),
                                                             I('ichorcloth_' + piece), GEM_BASE + extra, aspects, 13)
RECIPES.update({
    'infusion/warp_gate': infusion('warp_gate', 'WARP_GATE', I('thaumcraft:travel_paving'),
                                   [I('ichor'), I('ender_shard'), I('transvector_dislocator'), I('nether_shard'), I('minecraft:diamond'),
                                    I('minecraft:feather')], {'iter': 64, 'alienis': 50, 'volatus': 50}, 8),
    'infusion/sky_pearl': dict(infusion('sky_pearl', 'WARP_GATE', I('minecraft:ender_pearl'),
                                        [I('ichor'), I('ender_shard'), I('minecraft:lapis_block'), I('minecraft:diamond')],
                                        {'iter': 32, 'alienis': 32, 'volatus': 32, 'aer': 16}, 6), result=dict(I('sky_pearl'), count=2)),
    'infusion/focus_recall': infusion('focus_recall', 'FOCUS_RECALL', I('sky_pearl'),
                                      [I('ichor'), I('ichor'), I('minecraft:ender_pearl'), I('minecraft:diamond'), I('thaumcraft:magic_mirror'),
                                       I('thaumcraft:focus_portable_hole')], {'iter': 100, 'alienis': 64, 'praecantatio': 50}, 10),
    'infusion/focus_shadowbeam': infusion('focus_shadowbeam', 'FOCUS_SHADOWBEAM', I('thaumcraft:focus_shock'),
                                          [I('ichor'), I('minecraft:arrow'), I('minecraft:diamond'), I('thaumcraft:focus_excavation'),
                                           I('focus_deflect'), I('ichor')], {'tenebrae': 65, 'alienis': 32, 'praecantatio': 50, 'telum': 32}, 12),
    'infusion/focus_xp_drain': infusion('focus_xp_drain', 'FOCUS_XP_DRAIN', I('minecraft:ender_pearl'),
                                        [I('ichor'), I('minecraft:experience_bottle'), I('minecraft:diamond'), I('xp_talisman'),
                                         I('minecraft:enchanting_table'), I('ichor')],
                                        {'cognitio': 65, 'vitium': 16, 'praecantatio': 50, 'auram': 32}, 12),
})
RESEARCH += [
    kami('ICHORCLOTH_ARMOR', {'tutamen': 2, 'pannus': 1, 'lux': 1, 'fabrico': 1}, 17, 5, 'ichorcloth_chest', ['ICHOR_CLOTH'], warp=4,
         pages=[P('arcane', 'ichorcloth_' + piece) for piece in ARMOR_3C]),
    kami('ICHORCLOTH_HELM_GEM', {'aqua': 2, 'sano': 1, 'fames': 1, 'auram': 1}, 18, 3, 'ichorcloth_helm_gem', ['ICHORCLOTH_ARMOR'],
         pages=[P('infusion', 'infusion/ichorcloth_helm_gem')]),
    kami('ICHORCLOTH_CHEST_GEM', {'aer': 2, 'motus': 1, 'volatus': 1, 'alienis': 1}, 17, 7, 'ichorcloth_chest_gem', ['ICHORCLOTH_ARMOR'],
         pages=[P('infusion', 'infusion/ichorcloth_chest_gem')]),
    kami('ICHORCLOTH_LEGS_GEM', {'ignis': 2, 'sano': 1, 'lucrum': 1, 'potentia': 1}, 17, 9, 'ichorcloth_legs_gem', ['ICHORCLOTH_ARMOR'],
         pages=[P('infusion', 'infusion/ichorcloth_legs_gem')] + text('ICHORCLOTH_LEGS_GEM', 1)),
    kami('ICHORCLOTH_BOOTS_GEM', {'terra': 2, 'iter': 1, 'perfodio': 1, 'herba': 1}, 15, 10, 'ichorcloth_boots_gem', ['ICHORCLOTH_ARMOR'],
         pages=[P('infusion', 'infusion/ichorcloth_boots_gem')]),
    kami('WARP_GATE', {'iter': 2, 'alienis': 1, 'volatus': 1, 'machina': 1}, 19, 6, 'warp_gate', ['ICHORCLOTH_CHEST_GEM'],
         ['ICHORCLOTH_BOOTS_GEM'], pages=[P('infusion', 'infusion/warp_gate')] + text('WARP_GATE', 1) + [P('infusion', 'infusion/sky_pearl')]),
    kami('FOCUS_RECALL', {'iter': 2, 'alienis': 1, 'volatus': 1, 'praecantatio': 1}, 20, 8, 'focus_recall', ['WARP_GATE'],
         ['ROD_ichorcloth'], pages=[P('infusion', 'infusion/focus_recall')]),
    kami('FOCUS_SHADOWBEAM', {'tenebrae': 2, 'praecantatio': 1, 'alienis': 1, 'vitium': 1}, 14, 4, 'focus_shadowbeam', ['ROD_ichorcloth'],
         pages=[P('infusion', 'infusion/focus_shadowbeam')]),
    kami('FOCUS_XP_DRAIN', {'cognitio': 2, 'praecantatio': 1, 'auram': 1, 'humanus': 1}, 12, 3, 'focus_xp_drain', ['ROD_ichorcloth'],
         pages=[P('infusion', 'infusion/focus_xp_drain')]),
]


def gen_models_3c(models, states):
    write(os.path.join(models, 'block', 'warp_gate.json'), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': ns('block/warp_gate_top'), 'side': ns('block/warp_gate_side'), 'bottom': ns('block/warp_gate_side')}})
    write(os.path.join(models, 'item', 'warp_gate.json'), {'parent': ns('block/warp_gate')})
    write(os.path.join(states, 'warp_gate.json'), {'variants': {'': {'model': ns('block/warp_gate')}}})


# ---------------------------------------------------------------- модели предметов и блоков
FLAT_ITEMS = ['dark_quartz', 'spell_cloth', 'bright_nitor', 'gaseous_light', 'gaseous_shadow', 'gas_remover', 'share_tome',
              'revealing_helm', 'focus_smelt', 'focus_flight', 'focus_deflect', 'focus_telekinesis', 'focus_dislocation',
              'focus_heal', 'focus_ender_chest', 'soul_mould', 'connector']
# предметы с двумя видами: включён (свойство thaumictinkerer:active = 1) и выключен
TOGGLED_ITEMS = ['xp_talisman', 'cleansing_talisman']
# блоки, у которых предмет — та же модель
CUBE_BLOCKS = {
    'dark_quartz_block': {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'side': 'block/dark_quartz_block_side', 'top': 'block/dark_quartz_block_top', 'bottom': 'block/dark_quartz_block_bottom'}},
    'chiseled_dark_quartz_block': {'parent': 'minecraft:block/cube_column', 'textures': {
        'side': 'block/chiseled_dark_quartz_block', 'end': 'block/chiseled_dark_quartz_block_top'}},
    'aspect_analyzer': {'parent': 'minecraft:block/cube', 'textures': {
        'particle': 'block/aspect_analyzer_north', 'up': 'block/aspect_analyzer_top', 'down': 'block/aspect_analyzer_top',
        'north': 'block/aspect_analyzer_north', 'south': 'block/aspect_analyzer_south',
        'west': 'block/aspect_analyzer_west', 'east': 'block/aspect_analyzer_east'}},
    'platform': {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'block/platform'}},
}
for name in ('mobilizer', 'mobilizer_relay'):
    CUBE_BLOCKS[name] = {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'side': 'block/%s_side' % name, 'top': 'block/%s_top' % name, 'bottom': 'block/%s_bottom' % name}}
# зачарователь — плита высотой 12 точек
CUBE_BLOCKS['enchanter'] = {
    'parent': 'minecraft:block/block', 'textures': {'particle': 'block/enchanter_side', 'top': 'block/enchanter_top',
                                                    'bottom': 'block/enchanter_bottom', 'side': 'block/enchanter_side'},
    'elements': [{'from': [0, 0, 0], 'to': [16, 12, 16], 'faces': {
        'down': {'texture': '#bottom', 'cullface': 'down'}, 'up': {'texture': '#top'},
        'north': {'texture': '#side', 'uv': [0, 4, 16, 16], 'cullface': 'north'},
        'south': {'texture': '#side', 'uv': [0, 4, 16, 16], 'cullface': 'south'},
        'west': {'texture': '#side', 'uv': [0, 4, 16, 16], 'cullface': 'west'},
        'east': {'texture': '#side', 'uv': [0, 4, 16, 16], 'cullface': 'east'}}}]}
# блоки без предмета (газы): состояние с пустой моделью
GAS_BLOCKS = ['gaseous_light', 'gaseous_shadow', 'nitor_gas']
# блоки, которые выпадают сами собой
SELF_DROP_2B = ['summon', 'magnet', 'mob_magnet', 'transvector_interface', 'transvector_dislocator', 'mobilizer', 'mobilizer_relay',
                'remote_placer', 'animation_tablet']
SELF_DROP = SELF_DROP_2B + ['warp_gate', 'dark_quartz_block', 'chiseled_dark_quartz_block', 'dark_quartz_pillar', 'dark_quartz_stairs', 'funnel',
             'repairer', 'aspect_analyzer', 'platform', 'enchanter']
PICKAXE = ['warp_gate', 'dark_quartz_block', 'chiseled_dark_quartz_block', 'dark_quartz_pillar', 'dark_quartz_slab', 'dark_quartz_stairs',
           'funnel', 'repairer', 'aspect_analyzer', 'enchanter', 'transvector_interface', 'transvector_dislocator', 'mobilizer',
           'mobilizer_relay', 'remote_placer', 'animation_tablet', 'summon']
AXE = ['platform', 'magnet', 'mob_magnet']
NECKLACES = ['xp_talisman', 'cleansing_talisman']


def ns(path):
    return NS + ':' + path


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


def prefixed(textures):
    return {k: v if ':' in v else ns(v) for k, v in textures.items()}


def gen_textures():
    for new, old in TEXTURES.items():
        dst = os.path.join(ASSETS, 'textures', new + '.png')
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(os.path.join(SRC, 'textures', old), dst)
        if os.path.exists(os.path.join(SRC, 'textures', old + '.mcmeta')):
            shutil.copyfile(os.path.join(SRC, 'textures', old + '.mcmeta'), dst + '.mcmeta')


def read_lang(name):
    out = {}
    with open(os.path.join(SRC, 'lang', name + '.lang'), encoding='utf-8', errors='replace') as f:
        for line in f:
            line = line.rstrip('\n').rstrip('\r')
            if '=' in line and not line.startswith('#'):
                k, v = line.split('=', 1)
                out[k.strip()] = v
    return out


def gen_lang():
    def tool_lang(name):
        # name.json и его продолжения name_2.json, name_3.json… (по частям аддона)
        import glob
        base = os.path.join(ROOT, 'tools', 'lang', name)
        out = {}
        for path in sorted(glob.glob(base[:-5] + '.json') + glob.glob(base[:-5] + '_[0-9]*.json')):
            out.update(json.load(open(path, encoding='utf-8')))
        return out

    research_keys = {r['key'] for r in RESEARCH}
    english = None
    for code, original in list(LANGS.items()) + [('uk_ua', None)]:
        src = read_lang(original) if original else {}
        table = {}
        for ours, theirs in ITEM_NAMES.items():
            for key in [theirs] + ITEM_NAME_ALIASES.get(theirs, []):
                if key in src:
                    table[ours] = src[key]
                    break
        for k, v in list(src.items()):
            for theirs, ours in RESEARCH_LANG_ALIAS.items():
                for part in ('name', 'lore'):
                    if k == 'ttresearch.%s.%s' % (part, theirs):
                        src['ttresearch.%s.%s' % (part, ours)] = v
        for k, v in src.items():
            if k.startswith('ttmisc.') or k.startswith('ttresearch.page.') or k == 'ttresearch.prefix':
                table[k] = v
            elif k.startswith('ttresearch.name.') and k[16:] in research_keys:
                table['tc.research_name.' + k[16:]] = v
            elif k.startswith('ttresearch.lore.') and k[16:] in research_keys:
                table['tc.research_text.' + k[16:]] = v
            elif k.startswith('tc.research_category.TT_'):
                table[k] = v
        table.update(tool_lang('tt_extra_%s.json' % code))
        if code == 'uk_ua':
            # украинского в оригинале нет: английская основа, поверх — наш перевод
            table = dict(english, **tool_lang('tt_uk.json'))
        # картинки в тексте книги: пути оригинала -> наши
        for new_path, old_path in TEXTURES.items():
            table = {k: v.replace('ttinkerer:textures/' + old_path, NS + ':textures/' + new_path + '.png') for k, v in table.items()}
        # в текстах оригинала одиночный % ломал форматирование книги
        table = {k: v for k, v in sorted(table.items())}
        write(os.path.join(ASSETS, 'lang', code + '.json'), table)
        if code == 'en_us':
            english = table
    missing = [k for k in english if k not in tool_lang('tt_uk.json')
               and (k.startswith('item.') or k.startswith('block.') or k.startswith('tc.research_')
                    or any(k == 'ttresearch.page.%s.%d' % (r, n) for r in research_keys for n in range(8)))]
    if missing:
        print('без украинского перевода:', len(missing), missing[:8])


def gen_research():
    entries = []
    for r in RESEARCH:
        e = {'key': r['key'], 'category': 'TT_CATEGORY', 'aspects': r.get('aspects', {}), 'column': r['column'],
             'row': r['row'], 'complexity': r['complexity'], 'pages': r['pages'],
             'icon': {'texture': ns('textures/%s.png' % r['texture'])} if 'texture' in r else {'item': ns(r['icon'])}}
        for flag in ('stub', 'autoUnlock', 'round', 'secondary', 'concealed', 'hidden'):
            if r.get(flag):
                e[flag] = True
        for field in ('parents', 'parents_hidden'):
            if r.get(field):
                e[field] = r[field]
        if r.get('requires_all'):
            e['requires_all'] = r['requires_all']
        if r.get('warp'):
            e['warp'] = r['warp']
        entries.append(e)
    write(os.path.join(DATA, 'thaumcraft_research', 'research.json'), {'categories': [
        {'key': 'TT_CATEGORY', 'icon': ns('textures/misc/r_enchanting.png'),
         'background': 'thaumcraft:textures/gui/gui_researchback.png'}], 'entries': entries})


def gen_recipes():
    for name, recipe in RECIPES.items():
        write(os.path.join(DATA, 'recipes', name + '.json'), recipe)


def gen_models():
    models = os.path.join(ASSETS, 'models')
    states = os.path.join(ASSETS, 'blockstates')
    for item in FLAT_ITEMS + FLAT_2D:
        write(os.path.join(models, 'item', item + '.json'),
              {'parent': 'minecraft:item/generated', 'textures': {'layer0': ns('item/' + item)}})
    for item in TOGGLED_ITEMS:
        write(os.path.join(models, 'item', item + '_active.json'),
              {'parent': 'minecraft:item/generated', 'textures': {'layer0': ns('item/%s_active' % item)}})
        write(os.path.join(models, 'item', item + '.json'),
              {'parent': 'minecraft:item/generated', 'textures': {'layer0': ns('item/' + item)},
               'overrides': [{'predicate': {ns('active'): 1}, 'model': ns('item/%s_active' % item)}]})
    for block, model in CUBE_BLOCKS.items():
        write(os.path.join(models, 'block', block + '.json'), dict(model, textures=prefixed(model['textures'])))
        write(os.path.join(models, 'item', block + '.json'), {'parent': ns('block/' + block)})
        write(os.path.join(states, block + '.json'), {'variants': {'': {'model': ns('block/' + block)}}})
    # столб: как у кварцевого столба, по трём осям
    pillar = {'side': ns('block/dark_quartz_pillar'), 'end': ns('block/dark_quartz_pillar_top')}
    write(os.path.join(models, 'block', 'dark_quartz_pillar.json'), {'parent': 'minecraft:block/cube_column', 'textures': pillar})
    write(os.path.join(models, 'block', 'dark_quartz_pillar_horizontal.json'),
          {'parent': 'minecraft:block/cube_column_horizontal', 'textures': pillar})
    write(os.path.join(models, 'item', 'dark_quartz_pillar.json'), {'parent': ns('block/dark_quartz_pillar')})
    write(os.path.join(states, 'dark_quartz_pillar.json'), {'variants': {
        'axis=y': {'model': ns('block/dark_quartz_pillar')},
        'axis=z': {'model': ns('block/dark_quartz_pillar_horizontal'), 'x': 90},
        'axis=x': {'model': ns('block/dark_quartz_pillar_horizontal'), 'x': 90, 'y': 90}}})
    # плита и ступени из кварца
    q = {'bottom': ns('block/dark_quartz_block_top'), 'top': ns('block/dark_quartz_block_top'),
         'side': ns('block/dark_quartz_block_side')}
    write(os.path.join(models, 'block', 'dark_quartz_slab.json'), {'parent': 'minecraft:block/slab', 'textures': q})
    write(os.path.join(models, 'block', 'dark_quartz_slab_top.json'), {'parent': 'minecraft:block/slab_top', 'textures': q})
    write(os.path.join(models, 'item', 'dark_quartz_slab.json'), {'parent': ns('block/dark_quartz_slab')})
    write(os.path.join(states, 'dark_quartz_slab.json'), {'variants': {
        'type=bottom': {'model': ns('block/dark_quartz_slab')}, 'type=top': {'model': ns('block/dark_quartz_slab_top')},
        'type=double': {'model': ns('block/dark_quartz_block')}}})
    for kind in ('', '_inner', '_outer'):
        write(os.path.join(models, 'block', 'dark_quartz_stairs%s.json' % kind),
              {'parent': 'minecraft:block/%sstairs' % kind.lstrip('_').replace('inner', 'inner_').replace('outer', 'outer_'),
               'textures': q})
    write(os.path.join(models, 'item', 'dark_quartz_stairs.json'), {'parent': ns('block/dark_quartz_stairs')})
    write(os.path.join(states, 'dark_quartz_stairs.json'), stairs_state(ns('block/dark_quartz_stairs')))
    for gas in GAS_BLOCKS:
        write(os.path.join(states, gas + '.json'), {'variants': {'': {'model': ns('block/empty')}}})
    write(os.path.join(models, 'block', 'empty.json'), {'textures': {'particle': 'minecraft:block/glass'}, 'elements': []})
    # воронка: тонкая плита на воронке-хоппере, сверху рамка
    write(os.path.join(models, 'block', 'funnel.json'), {
        'parent': 'minecraft:block/block', 'textures': {'particle': ns('block/funnel_top'), 'top': ns('block/funnel_top'),
                                                        'side': ns('block/funnel_side')},
        'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16], 'faces': {
            'down': {'texture': '#top', 'cullface': 'down'}, 'up': {'texture': '#top'},
            'north': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 14, 16, 16]},
            'west': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'east': {'texture': '#side', 'uv': [0, 14, 16, 16]}}}]})
    write(os.path.join(models, 'item', 'funnel.json'), {'parent': ns('block/funnel')})
    write(os.path.join(states, 'funnel.json'), {'variants': {'': {'model': ns('block/funnel')}}})
    # восстановитель рисуется целиком отрисовщиком блока; модель нужна для частиц и предмета
    write(os.path.join(models, 'block', 'repairer.json'), {'textures': {'particle': ns('block/platform')}, 'elements': []})
    write(os.path.join(states, 'repairer.json'), {'variants': {
        'facing=%s' % f: {'model': ns('block/repairer')} for f in ('north', 'south', 'west', 'east')}})
    write(os.path.join(models, 'item', 'repairer.json'), {'parent': 'minecraft:builtin/entity', 'display': BLOCK_DISPLAY})
    gen_models_2b(models, states)
    gen_models_2c(models, states)
    gen_models_2d(models, states)
    gen_models_3b(models, states)
    gen_models_3c(models, states)
    # платформа под маскировкой не рисует своей модели — вместо неё отрисовщик блока показывает выбранный блок
    write(os.path.join(states, 'platform.json'), {'variants': {
        'camo=false': {'model': ns('block/platform')}, 'camo=true': {'model': ns('block/empty')}}})


FACINGS6 = {'north': (0, 0), 'south': (0, 180), 'west': (0, 270), 'east': (0, 90), 'up': (270, 0), 'down': (90, 0)}


def gen_models_2c(models, states):
    for kind, suffix in (('soul_aspect', ''), ('condensed_soul_aspect', '_condensed'), ('infused_soul_aspect', '')):
        overrides = []
        for i, aspect in enumerate(SOUL_ASPECTS):
            name = '%s_%s' % (kind, aspect)
            write(os.path.join(models, 'item', 'soul', name + '.json'),
                  {'parent': 'minecraft:item/generated', 'textures': {'layer0': ns('item/soul/%s%s' % (aspect, suffix))}})
            overrides.append({'predicate': {ns('aspect'): i + 1}, 'model': ns('item/soul/' + name)})
        write(os.path.join(models, 'item', kind + '.json'), {'parent': 'minecraft:item/generated', 'textures': {
            'layer0': ns('item/soul/aer' + suffix)}, 'overrides': overrides})
    write(os.path.join(models, 'item', 'blood_sword_active.json'),
          {'parent': 'minecraft:item/handheld', 'textures': {'layer0': ns('item/blood_sword_active')}})
    write(os.path.join(models, 'item', 'blood_sword.json'), {'parent': 'minecraft:item/handheld', 'textures': {
        'layer0': ns('item/blood_sword')}, 'overrides': [{'predicate': {ns('active'): 1}, 'model': ns('item/blood_sword_active')}]})
    write(os.path.join(models, 'block', 'summon.json'), {
        'parent': 'minecraft:block/block', 'textures': {'particle': ns('block/summon_side'), 'top': ns('block/summon_top'),
                                                        'bottom': 'minecraft:block/obsidian', 'side': ns('block/summon_side')},
        'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16], 'faces': {
            'down': {'texture': '#bottom', 'cullface': 'down'}, 'up': {'texture': '#top'},
            'north': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 14, 16, 16]},
            'west': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'east': {'texture': '#side', 'uv': [0, 14, 16, 16]}}}]})
    write(os.path.join(models, 'item', 'summon.json'), {'parent': ns('block/summon')})
    write(os.path.join(states, 'summon.json'), {'variants': {'': {'model': ns('block/summon')}}})


def gen_models_2b(models, states):
    # магниты рисует отрисовщик блока; предмет — им же
    for name in ('magnet', 'mob_magnet'):
        write(os.path.join(models, 'block', name + '.json'), {'textures': {'particle': ns('block/platform')}, 'elements': []})
        write(os.path.join(states, name + '.json'), {'variants': {'pull=true': {'model': ns('block/' + name)},
                                                                   'pull=false': {'model': ns('block/' + name)}}})
        write(os.path.join(models, 'item', name + '.json'), {'parent': 'minecraft:builtin/entity', 'display': BLOCK_DISPLAY})
    # интерфейс: куб, под маскировкой пуст
    write(os.path.join(models, 'block', 'transvector_interface.json'),
          {'parent': 'minecraft:block/cube_all', 'textures': {'all': ns('block/transvector_interface')}})
    write(os.path.join(models, 'item', 'transvector_interface.json'), {'parent': ns('block/transvector_interface')})
    write(os.path.join(states, 'transvector_interface.json'), {'variants': {
        'camo=false': {'model': ns('block/transvector_interface')}, 'camo=true': {'model': ns('block/empty')}}})
    # смещатель и установщик: особая лицевая грань (вращение — как у раздатчика)
    for name in ('transvector_dislocator', 'remote_placer'):
        write(os.path.join(models, 'block', name + '.json'), {'parent': 'minecraft:block/cube',
                                                              'textures': {'particle': ns('block/' + name),
                                                                           'north': ns('block/%s_face' % name), 'south': ns('block/' + name),
                                                                           'east': ns('block/' + name), 'west': ns('block/' + name),
                                                                           'up': ns('block/' + name), 'down': ns('block/' + name)}})
        write(os.path.join(models, 'item', name + '.json'), {'parent': ns('block/' + name)})
        variants = {}
        for facing, (x, y) in FACINGS6.items():
            for camo in ('false', 'true'):
                for powered in ('false', 'true'):
                    v = {'model': ns('block/' + name) if camo == 'false' else ns('block/empty')}
                    if x:
                        v['x'] = x
                    if y:
                        v['y'] = y
                    variants['camo=%s,facing=%s,powered=%s' % (camo, facing, powered)] = v
        write(os.path.join(states, name + '.json'), {'variants': variants})
    # анимационная табличка — плита высотой 2 точки
    write(os.path.join(models, 'block', 'animation_tablet.json'), {
        'parent': 'minecraft:block/block', 'textures': {'particle': ns('block/animation_tablet_side'), 'top': ns('block/animation_tablet_top'),
                                                        'bottom': ns('block/animation_tablet_bottom'), 'side': ns('block/animation_tablet_side')},
        'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16], 'faces': {
            'down': {'texture': '#bottom', 'cullface': 'down'}, 'up': {'texture': '#top'},
            'north': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 14, 16, 16]},
            'west': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'east': {'texture': '#side', 'uv': [0, 14, 16, 16]}}}]})
    write(os.path.join(models, 'item', 'animation_tablet.json'), {'parent': ns('block/animation_tablet')})
    write(os.path.join(states, 'animation_tablet.json'), {'variants': {
        'facing=%s,powered=%s' % (f, p): {'model': ns('block/animation_tablet')}
        for f in ('north', 'south', 'west', 'east') for p in ('false', 'true')}})


BLOCK_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}


def stairs_state(model):
    """Состояния ступеней — как у ванильных (generated by data generators)."""
    variants = {}
    rot = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    for facing, y in rot.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                m = model + ('' if shape == 'straight' else '_inner' if shape.startswith('inner') else '_outer')
                yy = y
                if shape in ('inner_left', 'outer_left') and half == 'bottom':
                    yy -= 90
                if shape in ('inner_right', 'outer_right') and half == 'top':
                    yy += 90
                v = {'model': m}
                if half == 'top':
                    v['x'] = 180
                if yy % 360:
                    v['y'] = yy % 360
                if half == 'top' or yy % 360:
                    v['uvlock'] = True
                variants['facing=%s,half=%s,shape=%s' % (facing, half, shape)] = v
    return {'variants': variants}


def gen_loot_and_tags():
    for block in SELF_DROP:
        write(os.path.join(DATA, 'loot_tables', 'blocks', block + '.json'), {
            'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': ns(block)}],
                                                  'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    write(os.path.join(DATA, 'loot_tables', 'blocks', 'dark_quartz_slab.json'), {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': ns('dark_quartz_slab'), 'functions': [{
                'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
                    'condition': 'minecraft:block_state_property', 'block': ns('dark_quartz_slab'),
                    'properties': {'type': 'double'}}]}, {'function': 'minecraft:explosion_decay'}]}]}]})
    tags = os.path.join(OUT, 'data')
    write(os.path.join(tags, 'minecraft', 'tags', 'blocks', 'mineable', 'pickaxe.json'),
          {'replace': False, 'values': [ns(b) for b in PICKAXE]})
    write(os.path.join(tags, 'minecraft', 'tags', 'blocks', 'mineable', 'axe.json'),
          {'replace': False, 'values': [ns(b) for b in AXE]})
    write(os.path.join(tags, 'minecraft', 'tags', 'blocks', 'slabs.json'), {'replace': False, 'values': [ns('dark_quartz_slab')]})
    write(os.path.join(tags, 'minecraft', 'tags', 'blocks', 'stairs.json'), {'replace': False, 'values': [ns('dark_quartz_stairs')]})
    write(os.path.join(tags, 'minecraft', 'tags', 'items', 'slabs.json'), {'replace': False, 'values': [ns('dark_quartz_slab')]})
    write(os.path.join(tags, 'minecraft', 'tags', 'items', 'stairs.json'), {'replace': False, 'values': [ns('dark_quartz_stairs')]})
    write(os.path.join(tags, 'curios', 'tags', 'items', 'necklace.json'), {'replace': False, 'values': [ns(i) for i in NECKLACES + ['cat_amulet']]})
    write(os.path.join(tags, 'curios', 'tags', 'items', 'belt.json'), {'replace': False, 'values': [ns('ichor_pouch')]})
    write(os.path.join(tags, 'curios', 'tags', 'items', 'ring.json'), {'replace': False, 'values': [ns('block_talisman')]})


if __name__ == '__main__':
    gen_textures()
    gen_lang()
    gen_research()
    gen_recipes()
    gen_models()
    gen_loot_and_tags()
    print('готово:', len(RESEARCH), 'исследований,', len(RECIPES), 'рецептов,', len(TEXTURES), 'текстур')

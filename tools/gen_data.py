"""Генерирует однотипные JSON для блоков и предметов мода: состояния, модели, добычу, рецепты, теги, генерацию мира.

Запуск из корня проекта:  python tools/gen_data.py

Всё, что здесь описано, перезаписывается при каждом запуске — править руками нужно этот скрипт, а не результат.
"""
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'thaumcraft')
DATA = os.path.join(RES, 'data')

ELEMENTS = ['air', 'fire', 'water', 'earth', 'order', 'entropy']
INFUSED = ['infused_stone_' + e for e in ELEMENTS]

tags = {}   # (реестр, пространство имён, путь) -> значения


def write(base, path, data):
    full = os.path.join(base, *path.split('/'))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8') as fh:
        json.dump(data, fh, indent=2)
        fh.write('\n')


def tag(registry, name, *values):
    namespace, path = name.split(':')
    tags.setdefault((registry, namespace, path), []).extend(values)


def tc(name):
    return 'thaumcraft:' + name


# ---------- модели ----------

def simple_block(name, texture):
    write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
    write(ASSETS, 'models/block/%s.json' % name,
          {'parent': 'minecraft:block/cube_all', 'textures': {'all': tc('block/' + texture)}})
    write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})


def infused_stone(name, stone=None):
    """Камень, поверх которого лежит светящийся слой в цвете стихии (tintindex 0)."""
    faces = ['down', 'up', 'north', 'south', 'west', 'east']
    write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
    write(ASSETS, 'models/block/%s.json' % name, {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout',  # вырез, а не полупрозрачность: иначе рамка выделения блока рисуется с разрывами
        'textures': {
            'particle': stone or tc('block/infusedorestone'),
            'stone': stone or tc('block/infusedorestone'),
            'ore': tc('block/infusedore'),
        },
        'elements': [
            {'from': [0, 0, 0], 'to': [16, 16, 16],
             'faces': {f: {'texture': '#stone', 'cullface': f} for f in faces}},
            # в оригинале слой рисовался с яркостью не ниже 160 из 240
            {'from': [0, 0, 0], 'to': [16, 16, 16],
             'faces': {f: {'texture': '#ore', 'cullface': f, 'tintindex': 0,
                           'forge_data': {'block_light': 10, 'sky_light': 10}} for f in faces}},
        ],
    })
    write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})


def simple_item(name, texture):
    write(ASSETS, 'models/item/%s.json' % name,
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('item/' + texture)}})


def log_block(name, side, top):
    model = tc('block/' + name)
    write(ASSETS, 'blockstates/%s.json' % name, {'variants': {
        'axis=y': {'model': model},
        'axis=z': {'model': model + '_horizontal', 'x': 90},
        'axis=x': {'model': model + '_horizontal', 'x': 90, 'y': 90},
    }})
    textures = {'end': tc('block/' + top), 'side': tc('block/' + side)}
    write(ASSETS, 'models/block/%s.json' % name, {'parent': 'minecraft:block/cube_column', 'textures': textures})
    write(ASSETS, 'models/block/%s_horizontal.json' % name,
          {'parent': 'minecraft:block/cube_column_horizontal', 'textures': textures})
    write(ASSETS, 'models/item/%s.json' % name, {'parent': model})


def leaves_block(name, texture):
    write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
    write(ASSETS, 'models/block/%s.json' % name, {
        'parent': 'minecraft:block/leaves', 'render_type': 'minecraft:cutout_mipped',
        'textures': {'all': tc('block/' + texture)}})
    write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})


def cross_block(name, texture):
    write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
    write(ASSETS, 'models/block/%s.json' % name, {
        'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout',
        'textures': {'cross': tc('block/' + texture)}})
    write(ASSETS, 'models/item/%s.json' % name,
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('block/' + texture)}})


# ---------- добыча ----------

SILK_TOUCH = {'condition': 'minecraft:match_tool',
              'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}


def drop_self(name):
    write(DATA, 'thaumcraft/loot_tables/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc(name)}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    })


def ore_drop(name, item, min_count, max_count):
    """Шёлковое касание даёт сам блок; иначе предмет, удача добавляет от 0 до уровня штук — как в оригинале."""
    functions = []
    if (min_count, max_count) != (1, 1):
        functions.append({'function': 'minecraft:set_count',
                          'count': {'type': 'minecraft:uniform', 'min': min_count, 'max': max_count}})
    functions.append({'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune',
                      'formula': 'minecraft:uniform_bonus_count', 'parameters': {'bonusMultiplier': 1}})
    functions.append({'function': 'minecraft:explosion_decay'})
    write(DATA, 'thaumcraft/loot_tables/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc(name), 'conditions': [SILK_TOUCH]},
            {'type': 'minecraft:item', 'name': tc(item), 'functions': functions},
        ]}]}],
    })


def leaves_drop(name, sapling, chance):
    """Ножницы и шёлковое касание дают листву; иначе изредка саженец с тем же шансом, что в оригинале."""
    shears = {'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}}
    write(DATA, 'thaumcraft/loot_tables/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc(name),
             'conditions': [{'condition': 'minecraft:any_of', 'terms': [shears, SILK_TOUCH]}]},
            {'type': 'minecraft:item', 'name': tc(sapling), 'conditions': [
                {'condition': 'minecraft:survives_explosion'},
                {'condition': 'minecraft:random_chance', 'chance': chance}]},
        ]}]}],
    })


def shapeless(name, ingredient, result, count=1):
    result_json = {'item': result}
    if count != 1:
        result_json['count'] = count
    write(DATA, 'thaumcraft/recipes/%s.json' % name, {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': tc(ingredient)}],
        'result': result_json})


# ---------- рецепты ----------

def smelting(name, ingredient, result, xp):
    for kind, time in (('smelting', 200), ('blasting', 100)):
        write(DATA, 'thaumcraft/recipes/%s_from_%s.json' % (name, kind), {
            'type': 'minecraft:' + kind, 'category': 'misc', 'ingredient': {'item': tc(ingredient)},
            'result': tc(result), 'experience': xp, 'cookingtime': time,
        })


def compress(name, small, big):
    write(DATA, 'thaumcraft/recipes/%s_from_%s.json' % (big, small), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['###', '###', '###'],
        'key': {'#': {'item': tc(small)}}, 'result': {'item': tc(big)},
    })
    write(DATA, 'thaumcraft/recipes/%s_from_%s.json' % (small, big), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': tc(big)}],
        'result': {'item': tc(small), 'count': 9},
    })


# ---------- генерация мира ----------

def ore_targets(block):
    return [
        {'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'},
         'state': {'Name': tc(block)}},
        # в глубинном сланце ставится сланцевый вариант той же руды, как у ванильных руд
        {'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'},
         'state': {'Name': tc('deepslate_' + block)}},
    ]


def worldgen():
    features = []

    # Киноварь: в оригинале 18 одиночных блоков на чанк в нижней пятой части мира (y 0..51).
    # Мир стал глубже на 64 блока, поэтому диапазон растянут вниз, а попыток больше — плотность та же.
    write(DATA, 'thaumcraft/worldgen/configured_feature/cinnabar_ore.json', {
        'type': 'minecraft:replace_single_block', 'config': {'targets': ore_targets('cinnabar_ore')}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/cinnabar_ore.json', {
        'feature': tc('cinnabar_ore'),
        'placement': [
            {'type': 'minecraft:count', 'count': 30},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {
                'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 0}, 'max_inclusive': {'absolute': 20}}},
            {'type': 'minecraft:biome'},
        ]})
    features.append(tc('cinnabar_ore'))

    # Янтарь: 20 одиночных блоков на чанк, не глубже 25 блоков от поверхности.
    write(DATA, 'thaumcraft/worldgen/configured_feature/amber_ore.json', {
        'type': 'minecraft:replace_single_block', 'config': {'targets': ore_targets('amber_ore')}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/amber_ore.json', {
        'feature': tc('amber_ore'),
        'placement': [
            {'type': 'minecraft:count', 'count': 20},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'},
            # одно смещение ограничено 16 блоками, поэтому их два
            {'type': 'minecraft:random_offset', 'xz_spread': 0,
             'y_spread': {'type': 'minecraft:uniform', 'value': {'min_inclusive': -12, 'max_inclusive': 0}}},
            {'type': 'minecraft:random_offset', 'xz_spread': 0,
             'y_spread': {'type': 'minecraft:uniform', 'value': {'min_inclusive': -13, 'max_inclusive': -1}}},
            {'type': 'minecraft:biome'},
        ]})
    features.append(tc('amber_ore'))

    # Заряженный камень: 8 жил по 6 блоков на чанк от дна до поверхности, стихия случайная.
    # TODO: в оригинале в трети случаев стихия зависит от биома (BiomeHandler) — добавить вместе с аурой биомов.
    for name in INFUSED:
        write(DATA, 'thaumcraft/worldgen/configured_feature/%s.json' % name, {
            'type': 'minecraft:ore',
            'config': {'size': 6, 'discard_chance_on_air_exposure': 0.0, 'targets': ore_targets(name)}})
    write(DATA, 'thaumcraft/worldgen/configured_feature/infused_stone.json', {
        'type': 'minecraft:simple_random_selector',
        'config': {'features': [{'feature': tc(name), 'placement': []} for name in INFUSED]}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/infused_stone.json', {
        'feature': tc('infused_stone'),
        'placement': [
            {'type': 'minecraft:count', 'count': 14},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {
                'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 4}, 'max_inclusive': {'absolute': 58}}},
            {'type': 'minecraft:biome'},
        ]})
    features.append(tc('infused_stone'))

    write(DATA, 'thaumcraft/forge/biome_modifier/ores.json', {
        'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld', 'features': features,
        'step': 'underground_ores'})


def tree_placement(rarity):
    return [
        {'type': 'minecraft:rarity_filter', 'chance': rarity},
        {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
        {'type': 'minecraft:biome'},
    ]


def trees():
    for wood in ('greatwood', 'silverwood'):
        log_block(wood + '_log', wood + 'side', wood + 'top')
        leaves_block(wood + '_leaves', wood + 'leaves')
        simple_block(wood + '_planks', 'planks_' + wood)
        cross_block(wood + '_sapling', wood + 'sapling')
        drop_self(wood + '_log')
        drop_self(wood + '_planks')
        drop_self(wood + '_sapling')
        shapeless(wood + '_planks', wood + '_log', tc(wood + '_planks'), 4)
        for registry in ('blocks', 'items'):
            tag(registry, 'minecraft:logs_that_burn', tc(wood + '_log'))
            tag(registry, 'minecraft:leaves', tc(wood + '_leaves'))
            tag(registry, 'minecraft:planks', tc(wood + '_planks'))
            tag(registry, 'minecraft:saplings', tc(wood + '_sapling'))
        tag('blocks', 'minecraft:mineable/axe', tc(wood + '_log'), tc(wood + '_planks'))
        tag('blocks', 'minecraft:mineable/hoe', tc(wood + '_leaves'))
    # шансы из оригинала: 1 к 200 и 1 к 250
    leaves_drop('greatwood_leaves', 'greatwood_sapling', 0.005)
    leaves_drop('silverwood_leaves', 'silverwood_sapling', 0.004)

    cross_block('shimmerleaf', 'shimmerleaf')
    cross_block('vishroom', 'manashroom')
    drop_self('vishroom')
    # стручок маны: первые стадии — черенок под бревном, дальше поверх него рисуется сам стручок
    for stem in (0, 1, 2):
        write(ASSETS, 'models/block/mana_pod_%d.json' % stem, {
            'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout',
            'textures': {'cross': tc('block/manapod_stem_%d' % stem)}})
    write(ASSETS, 'blockstates/mana_pod.json', {'variants': {
        'age=%d' % age: {'model': tc('block/mana_pod_%d' % min(age, 2))} for age in range(8)}})
    simple_item('mana_bean', 'mana_bean')
    cross_block('cinderpearl', 'cinderpearl')
    drop_self('shimmerleaf')
    drop_self('cinderpearl')
    shapeless('quicksilver_from_shimmerleaf', 'shimmerleaf', tc('quicksilver'))
    shapeless('blaze_powder_from_cinderpearl', 'cinderpearl', 'minecraft:blaze_powder')

    # probability: у великого древа — шанс паучьего логова, у серебряного — шанс россыпи мерцалиста
    for name, feature, probability in (
            ('greatwood_tree', 'greatwood_tree', 0.125), ('greatwood_sapling_tree', 'greatwood_tree', 0.0),
            ('silverwood_tree', 'silverwood_tree', 1.0), ('silverwood_sapling_tree', 'silverwood_tree', 0.0)):
        write(DATA, 'thaumcraft/worldgen/configured_feature/%s.json' % name,
              {'type': tc(feature), 'config': {'probability': probability}})

    # Великое древо: в оригинале раз в 25 чанков, умноженное на пригодность биома (лес 1.0; равнины, саванна,
    # болото и хвойные 0.2).
    write(DATA, 'thaumcraft/worldgen/placed_feature/greatwood_tree.json',
          {'feature': tc('greatwood_tree'), 'placement': tree_placement(25)})
    write(DATA, 'thaumcraft/worldgen/placed_feature/greatwood_tree_rare.json',
          {'feature': tc('greatwood_tree'), 'placement': tree_placement(125)})
    # Серебряное дерево: в оригинале раз в 60 чанков в лесистых и берёзовых холмах. Этих биомов больше нет
    # (холмы стали частью обычного леса), поэтому берём сами леса, но реже.
    write(DATA, 'thaumcraft/worldgen/placed_feature/silverwood_tree.json',
          {'feature': tc('silverwood_tree'), 'placement': tree_placement(90)})

    def modifier(name, biomes, feature):
        write(DATA, 'thaumcraft/forge/biome_modifier/%s.json' % name, {
            'type': 'forge:add_features', 'biomes': biomes, 'features': [tc(feature)], 'step': 'vegetal_decoration'})

    modifier('greatwood_forest', '#minecraft:is_forest', 'greatwood_tree')
    modifier('greatwood_taiga', '#minecraft:is_taiga', 'greatwood_tree_rare')
    modifier('greatwood_savanna', '#minecraft:is_savanna', 'greatwood_tree_rare')
    modifier('greatwood_plains', ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow'],
             'greatwood_tree_rare')
    modifier('greatwood_swamp', ['minecraft:swamp', 'minecraft:mangrove_swamp'], 'greatwood_tree_rare')
    modifier('silverwood', ['minecraft:forest', 'minecraft:flower_forest', 'minecraft:birch_forest',
                            'minecraft:old_growth_birch_forest'], 'silverwood_tree')

    # Огнежемчуг: в оригинале раз в 30 чанков в жарких песчаных биомах, россыпью до 18 штук.
    write(DATA, 'thaumcraft/worldgen/configured_feature/cinderpearl.json', {
        'type': 'minecraft:random_patch',
        'config': {'tries': 18, 'xz_spread': 7, 'y_spread': 3, 'feature': {
            'feature': {'type': 'minecraft:simple_block', 'config': {
                'to_place': {'type': 'minecraft:simple_state_provider', 'state': {'Name': tc('cinderpearl')}}}},
            'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {
                'type': 'minecraft:all_of', 'predicates': [
                    {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'},
                    {'type': 'minecraft:matching_block_tag', 'offset': [0, -1, 0], 'tag': 'minecraft:sand'}]}}]}}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/cinderpearl.json',
          {'feature': tc('cinderpearl'), 'placement': tree_placement(30)})
    modifier('cinderpearl', ['minecraft:desert', 'minecraft:badlands', 'minecraft:eroded_badlands',
                             'minecraft:wooded_badlands'], 'cinderpearl')


def nodes():
    # сам блок узла невидим; модель нужна только ради частиц при разрушении
    write(ASSETS, 'blockstates/aura_node.json', {'variants': {'': {'model': tc('block/aura_node')}}})
    write(ASSETS, 'models/block/aura_node.json', {'textures': {'particle': tc('block/infusedore')}})

    log_block('silverwood_knot', 'silverwoodknotside', 'silverwoodknottop')
    # сучок падает обычным бревном: узел при разрушении пропадает
    write(DATA, 'thaumcraft/loot_tables/blocks/silverwood_knot.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('silverwood_log')}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tag('blocks', 'minecraft:logs_that_burn', tc('silverwood_knot'))
    tag('blocks', 'minecraft:mineable/axe', tc('silverwood_knot'))

    # Дикие узлы: в оригинале один на 36 чанков (Config.nodeRarity), у поверхности; в Нижнем мире — в толще.
    write(DATA, 'thaumcraft/worldgen/configured_feature/aura_node.json', {'type': tc('aura_node'), 'config': {}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/aura_node.json', {
        'feature': tc('aura_node'),
        'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': 36},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'},
            {'type': 'minecraft:biome'},
        ]})
    write(DATA, 'thaumcraft/worldgen/placed_feature/aura_node_nether.json', {
        'feature': tc('aura_node'),
        'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': 36},
            {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {
                'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 32}, 'max_inclusive': {'absolute': 96}}},
            {'type': 'minecraft:biome'},
        ]})
    # последним шагом, чтобы деревья и трава не затёрли узел
    # курганы, каменные круги, кольца Элдрича и тотемы — один выбор на чанк, внутри ThaumcraftStructuresFeature
    for name in ('structures', 'structure_nodes'):
        write(DATA, 'thaumcraft/worldgen/configured_feature/%s.json' % name, {'type': tc(name), 'config': {}})
        write(DATA, 'thaumcraft/worldgen/placed_feature/%s.json' % name, {'feature': tc(name), 'placement': [{'type': 'minecraft:biome'}]})
    write(DATA, 'thaumcraft/forge/biome_modifier/structures.json', {
        'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld', 'features': [tc('structures'), tc('structure_nodes')],
        'step': 'surface_structures'})
    write(DATA, 'thaumcraft/tags/worldgen/structure/has_aura_node.json', {
        'replace': False, 'values': ['minecraft:desert_pyramid', 'minecraft:jungle_pyramid', 'minecraft:swamp_hut', 'minecraft:igloo']})
    write(DATA, 'thaumcraft/forge/biome_modifier/aura_nodes.json', {
        'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld', 'features': [tc('aura_node')],
        'step': 'top_layer_modification'})
    write(DATA, 'thaumcraft/forge/biome_modifier/aura_nodes_nether.json', {
        'type': 'forge:add_features', 'biomes': '#minecraft:is_nether', 'features': [tc('aura_node_nether')],
        'step': 'top_layer_modification'})


def wands():
    simple_item('wand_cap_iron', 'wand_cap_iron')
    # Жезл рисует WandItemRenderer; здесь только положения в руках и в интерфейсе — как у обычного инструмента.
    write(ASSETS, 'models/item/wand.json', {
        'parent': 'minecraft:builtin/entity',
        'gui_light': 'front',
        'display': {
            # Масштаб меньше, чем у меча (0.85 и 0.68): жезл — короткая палочка, которую держат за нижний конец
            'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4, 0.5], 'scale': [0.55, 0.55, 0.55]},
            'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4, 0.5], 'scale': [0.55, 0.55, 0.55]},
            'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.45, 0.45, 0.45]},
            'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.45, 0.45, 0.45]},
            'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 180, 0]},
            'gui': {'scale': [0.95, 0.95, 0.95]},
        }})
    # рецепты оригинала: набалдашник из пяти железных самородков, жезл — палка и два набалдашника по диагонали
    write(DATA, 'thaumcraft/recipes/wand_cap_iron.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['NNN', 'N N'],
        'key': {'N': {'tag': 'forge:nuggets/iron'}}, 'result': {'item': tc('wand_cap_iron')}})
    write(DATA, 'thaumcraft/recipes/wand.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['  I', ' S ', 'I  '],
        'key': {'I': {'item': tc('wand_cap_iron')}, 'S': {'tag': 'forge:rods/wooden'}},
        'result': {'item': tc('wand'), 'nbt': {'rod': 'wood', 'cap': 'iron'}}})


def box_model(texture, tex_w, tex_h, boxes):
    """Модель блока из коробок старой модели сущности (ModelRenderer).

    Коробка задана как (u, v, x, y, z, ширина, высота, глубина) в координатах старой модели, где ось Y
    направлена вниз, а начало — в середине верхней грани блока. Развёртка — стандартная для коробок Minecraft.
    """
    def uv(u0, v0, u1, v1):
        return [u0 * 16 / tex_w, v0 * 16 / tex_h, u1 * 16 / tex_w, v1 * 16 / tex_h]

    elements = []
    for u, v, x, y, z, w, h, d in boxes:
        elements.append({
            'from': [8 + x, 16 - y - h, 8 - z - d],
            'to': [8 + x + w, 16 - y, 8 - z],
            'faces': {
                'up': {'uv': uv(u + d, v, u + d + w, v + d), 'texture': '#all'},
                'down': {'uv': uv(u + d + w, v, u + d + w + w, v + d), 'texture': '#all'},
                'south': {'uv': uv(u + d, v + d, u + d + w, v + d + h), 'texture': '#all'},
                'north': {'uv': uv(u + d + w + d, v + d, u + d + w + d + w, v + d + h), 'texture': '#all'},
                'east': {'uv': uv(u + d + w, v + d, u + d + w + d, v + d + h), 'texture': '#all'},
                'west': {'uv': uv(u, v + d, u + d, v + d + h), 'texture': '#all'},
            },
        })
    return {
        'parent': 'minecraft:block/block',
        'textures': {'all': tc('block/' + texture), 'particle': tc('block/' + texture)},
        'elements': elements,
    }


def workbench():
    # коробки из ModelTable и ModelArcaneWorkbench оригинала
    write(ASSETS, 'models/block/table.json', box_model('table', 64, 32, [
        (0, 0, -8, 0, -8, 16, 4, 16),      # столешница
        (0, 20, 2, 4, -2, 4, 8, 4),        # ножки
        (0, 20, -6, 4, -2, 4, 8, 4),
        (16, 20, -8, 12, -4, 16, 4, 8),    # перекладина
    ]))
    write(ASSETS, 'blockstates/table.json', {'variants': {
        'axis=x': {'model': tc('block/table')},
        'axis=z': {'model': tc('block/table'), 'y': 90},
    }})
    write(ASSETS, 'models/item/table.json', {'parent': tc('block/table')})

    write(ASSETS, 'models/block/arcane_workbench.json', box_model('worktable', 128, 64, [
        (0, 0, -8, 0, -8, 16, 8, 16),      # столешница
        (0, 32, -8, 12, -8, 16, 4, 16),    # основание
        (72, 0, 3, 8, -7, 4, 4, 4),        # ножки
        (72, 0, -7, 8, 3, 4, 4, 4),
        (72, 0, 3, 8, 3, 4, 4, 4),
        (72, 0, -7, 8, -7, 4, 4, 4),
    ]))
    write(ASSETS, 'blockstates/arcane_workbench.json', {'variants': {'': {'model': tc('block/arcane_workbench')}}})

    # стол разборки: та же модель, что у магического верстака, со своей текстурой
    write(ASSETS, 'models/block/deconstruction_table.json', box_model('decontable', 128, 64, [
        (0, 0, -8, 0, -8, 16, 8, 16),
        (0, 32, -8, 12, -8, 16, 4, 16),
        (72, 0, 3, 8, -7, 4, 4, 4),
        (72, 0, -7, 8, 3, 4, 4, 4),
        (72, 0, 3, 8, 3, 4, 4, 4),
        (72, 0, -7, 8, -7, 4, 4, 4),
    ]))
    write(ASSETS, 'blockstates/deconstruction_table.json', {'variants': {'': {'model': tc('block/deconstruction_table')}}})
    # в руке и в инвентаре стол показывается вместе с таумометром на столешнице, как в оригинале
    write(ASSETS, 'models/item/deconstruction_table.json', {
        'parent': 'minecraft:block/block', 'loader': 'forge:composite',
        'textures': {'particle': tc('block/decontable')},
        'children': {
            'table': {'parent': tc('block/deconstruction_table')},
            'thaumometer': {'parent': tc('item/thaumometer'),
                            'transform': {'translation': [0, 0.51, 0], 'scale': 0.7, 'origin': 'center'}}}})
    drop_self('deconstruction_table')
    tag('blocks', 'minecraft:mineable/axe', tc('deconstruction_table'))

    drop_self('table')
    # верстак разбирается обратно в стол; жезл и вещи с него выпадают отдельно
    write(DATA, 'thaumcraft/loot_tables/blocks/arcane_workbench.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('table')}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tag('blocks', 'minecraft:mineable/axe', tc('table'), tc('arcane_workbench'))

    # рецепты оригинала
    write(DATA, 'thaumcraft/recipes/table.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SSS', 'W W'],
        'key': {'S': {'tag': 'minecraft:wooden_slabs'}, 'W': {'tag': 'minecraft:planks'}},
        'result': {'item': tc('table')}})

    simple_item('wand_cap_gold', 'wand_cap_gold')
    simple_item('wand_rod_greatwood', 'wand_rod_greatwood')
    write(DATA, 'thaumcraft/recipes/wand_cap_gold.json', {
        'type': tc('arcane_shaped'), 'pattern': ['NNN', 'N N'], 'key': {'N': {'tag': 'forge:nuggets/gold'}},
        'result': {'item': tc('wand_cap_gold')},
        'aspects': {'ordo': 3, 'ignis': 3, 'aer': 3}, 'research': 'CAP_gold'})
    write(DATA, 'thaumcraft/recipes/wand_rod_greatwood.json', {
        'type': tc('arcane_shaped'), 'pattern': [' G', 'G '], 'key': {'G': {'item': tc('greatwood_log')}},
        'result': {'item': tc('wand_rod_greatwood')},
        'aspects': {'perditio': 3}, 'research': 'ROD_greatwood'})
    write(DATA, 'thaumcraft/recipes/wand_assembly.json', {'type': tc('arcane_wand')})


def research_table():
    simple_item('scribing_tools', 'inkwell')
    # таумометр: золото, стекло и два любых осколка
    write(DATA, 'thaumcraft/recipes/thaumometer.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': [' 1 ', 'IGI', ' 1 '],
        'key': {'I': {'tag': 'forge:ingots/gold'}, 'G': {'item': 'minecraft:glass'}, '1': {'tag': tc('shards')}},
        'result': {'item': tc('thaumometer')}})
    # письменные принадлежности можно собрать и из пустого флакона вместо бутылочки
    write(DATA, 'thaumcraft/recipes/scribing_tools_phial.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
        'ingredients': [{'item': tc('phial')}, {'item': 'minecraft:feather'},
                        [{'item': 'minecraft:ink_sac'}, {'tag': 'forge:dyes/black'}]],
        'result': {'item': tc('scribing_tools')}})

    # --- сальные свечи: шестнадцать цветов ---
    CANDLE_COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
                     'purple', 'blue', 'brown', 'green', 'red', 'black']
    wax = {'texture': '#wax', 'tintindex': 0}
    drips = [[], [[7, 0, 5, 8, 2, 6]], [[10, 0, 8, 11, 3, 9], [5, 0, 7, 6, 1, 8]], [[8, 0, 10, 9, 2, 11], [7, 0, 5, 8, 3, 6], [5, 0, 8, 6, 1, 9]]]
    for n, boxes in enumerate(drips):
        elements = [{'from': [6, 0, 6], 'to': [10, 8, 10], 'faces': {f: dict(wax) for f in ('down', 'up', 'north', 'south', 'west', 'east')}},
                    {'from': [7.6, 8, 7.6], 'to': [8.4, 10, 8.4],
                     'faces': {f: {'texture': '#wick'} for f in ('up', 'north', 'south', 'west', 'east')}}]
        # потёки воска у основания: у разных свечей разные
        for x0, y0, z0, x1, y1, z1 in boxes:
            elements.append({'from': [x0, y0, z0], 'to': [x1, y1, z1],
                             'faces': {f: dict(wax) for f in ('down', 'up', 'north', 'south', 'west', 'east')}})
        write(ASSETS, 'models/block/tallow_candle_%d.json' % n, {
            'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'particle': tc('block/candle'), 'wax': tc('block/candle'), 'wick': tc('block/candlestub')},
            'elements': elements,
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 3, 0], 'scale': [1.2, 1.2, 1.2]},
                        'ground': {'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
                        'fixed': {'translation': [0, 4, 0], 'scale': [1, 1, 1]},
                        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 4, 1], 'scale': [0.6, 0.6, 0.6]},
                        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 4, 0], 'scale': [0.7, 0.7, 0.7]}}})
    for color in CANDLE_COLORS:
        name = 'tallow_candle_' + color
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': [{'model': tc('block/tallow_candle_%d' % n)} for n in range(4)]}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/tallow_candle_0')})
        drop_self(name)
        tag('items', 'thaumcraft:tallow_candles', tc(name))
        if color != 'white':
            write(DATA, 'thaumcraft/recipes/%s.json' % name, {
                'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                'ingredients': [{'tag': 'forge:dyes/' + color}, {'item': tc('tallow_candle_white')}],
                'result': {'item': tc(name)}})
    write(DATA, 'thaumcraft/recipes/tallow_candle_white.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['S', 'T', 'T'],
        'key': {'S': {'item': 'minecraft:string'}, 'T': {'item': tc('tallow')}},
        'result': {'item': tc('tallow_candle_white'), 'count': 3}})
    # любую свечу можно отбелить костной мукой
    write(DATA, 'thaumcraft/recipes/tallow_candle_bleach.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'item': 'minecraft:bone_meal'}, {'tag': tc('tallow_candles')}],
        'result': {'item': tc('tallow_candle_white')}})
    # заметка: свиток с лентой в цвете аспекта; решённая заметка — «открытие»
    write(ASSETS, 'models/item/research_notes.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': tc('item/researchnotes'), 'layer1': tc('item/researchnotesoverlay')},
        'overrides': [{'predicate': {tc('complete'): 1}, 'model': tc('item/research_notes_complete')}]})
    write(ASSETS, 'models/item/research_notes_complete.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': tc('item/discovery'), 'layer1': tc('item/discoveryoverlay')}})

    # Модель из ModelResearchTable оригинала: главная половина рисует весь стол на два блока (вдоль +X),
    # вторая половина пуста. Чернильница с пером и свиток включаются свойствами блока.
    table = box_model('restable', 128, 64, [
        (0, 0, -8, 0, -8, 32, 4, 16),      # столешница на два блока
        (0, 24, -6, 4, -6, 4, 12, 4),      # ножки
        (0, 24, -6, 4, 2, 4, 12, 4),
        (0, 24, 18, 4, -6, 4, 12, 4),
        (0, 24, 18, 4, 2, 4, 12, 4),
        (24, 24, -4, 10, -2, 24, 4, 4),    # перекладина
    ])
    table['textures']['paper'] = tc('block/parchment')
    # стопка пергамента посреди стола, листы чуть сдвинуты друг относительно друга
    for i, (dx, dz) in enumerate(((0, 0), (0.6, -0.4), (-0.3, 0.5))):
        table['elements'].append({
            'from': [12 + dx, 16.02 + i * 0.03, 3.5 + dz], 'to': [20 + dx, 16.02 + i * 0.03, 12.5 + dz],
            'rotation': {'origin': [16, 16, 8], 'axis': 'y', 'angle': 22.5 if i != 1 else 0},
            'faces': {'up': {'uv': [0, 0, 16, 16], 'texture': '#paper'}}})
    table['render_type'] = 'minecraft:cutout'
    write(ASSETS, 'models/block/research_table.json', table)

    inkwell = box_model('restable', 128, 64, [(0, 44, -6, -2, 3, 3, 2, 3)])
    inkwell['textures']['quill'] = tc('block/tablequill')
    # перо торчит из чернильницы: плоскость, видимая с обеих сторон
    inkwell['elements'].append({
        'from': [1.25, 17, 3.5], 'to': [5.75, 23, 3.5],
        'rotation': {'origin': [3.5, 17, 3.5], 'axis': 'y', 'angle': 22.5},
        'faces': {'north': {'uv': [16, 0, 0, 16], 'texture': '#quill'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#quill'}}})
    inkwell['render_type'] = 'minecraft:cutout'
    write(ASSETS, 'models/block/research_table_inkwell.json', inkwell)

    scroll = box_model('restable2', 128, 64, [(0, 0, 14, -2, -5, 8, 2, 2), (0, 4, 17.5, -2.2, -5.2, 1, 2.4, 2.4)])
    for face in scroll['elements'][1]['faces'].values():
        face['tintindex'] = 0
    for element in scroll['elements']:
        element['rotation'] = {'origin': [26, 16, 12], 'axis': 'y', 'angle': 22.5}
    write(ASSETS, 'models/block/research_table_scroll.json', scroll)
    write(ASSETS, 'models/block/research_table_empty.json', {'textures': {'particle': tc('block/restable')}})

    multipart = [{'when': {'main': 'false'}, 'apply': {'model': tc('block/research_table_empty')}}]
    for facing, y in (('east', 0), ('south', 90), ('west', 180), ('north', 270)):
        for model, extra in (('research_table', {}), ('research_table_inkwell', {'ink': 'true'}),
                             ('research_table_scroll', {'note': 'true'})):
            when = {'facing': facing, 'main': 'true'}
            when.update(extra)
            apply = {'model': tc('block/' + model)}
            if y:
                apply['y'] = y
            multipart.append({'when': when, 'apply': apply})
    write(ASSETS, 'blockstates/research_table.json', {'multipart': multipart})
    # каждая половина при разрушении отдаёт свой стол
    write(DATA, 'thaumcraft/loot_tables/blocks/research_table.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('table')}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tag('blocks', 'minecraft:mineable/axe', tc('research_table'))

    # рецепты оригинала: бутылочка, перо и чернильный мешок; пустую чернильницу можно заправить
    ink = [{'item': 'minecraft:ink_sac'}, {'tag': 'forge:dyes/black'}]
    write(DATA, 'thaumcraft/recipes/scribing_tools.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
        'ingredients': [{'item': 'minecraft:glass_bottle'}, {'item': 'minecraft:feather'}, ink],
        'result': {'item': tc('scribing_tools')}})
    write(DATA, 'thaumcraft/recipes/scribing_tools_refill.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
        'ingredients': [{'item': tc('scribing_tools')}, ink],
        'result': {'item': tc('scribing_tools')}})


def crucible_recipe(name, catalyst, aspects, result, research, count=1):
    result_json = {'item': result}
    if count != 1:
        result_json['count'] = count
    write(DATA, 'thaumcraft/recipes/crucible/%s.json' % name, {
        'type': tc('crucible'), 'catalyst': catalyst, 'aspects': aspects, 'result': result_json,
        'research': research})


def shaped(name, pattern, key, result, category='misc'):
    write(DATA, 'thaumcraft/recipes/%s.json' % name, {
        'type': 'minecraft:crafting_shaped', 'category': category, 'pattern': pattern, 'key': key,
        'result': {'item': tc(result)}})


def alchemy():
    # --- тигель: коробка с дырой сверху, внутренние стенки и дно отдельными плоскостями, как в оригинале ---
    inner = 0.123 * 16
    sides = ['north', 'south', 'west', 'east']
    faces = {f: {'texture': '#side', 'cullface': f} for f in sides}
    faces['up'] = {'texture': '#top', 'cullface': 'up'}
    faces['down'] = {'texture': '#bottom', 'cullface': 'down'}
    write(ASSETS, 'models/block/crucible.json', {
        'parent': 'minecraft:block/block',
        'render_type': 'minecraft:cutout',
        'textures': {'particle': tc('block/crucible3'), 'top': tc('block/crucible1'), 'bottom': tc('block/crucible2'),
                     'side': tc('block/crucible3'), 'inner': tc('block/crucible5'), 'floor': tc('block/crucible6')},
        'elements': [
            {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces},
            {'from': [inner, 0, 0], 'to': [inner, 16, 16], 'faces': {'east': {'texture': '#inner'}}},
            {'from': [16 - inner, 0, 0], 'to': [16 - inner, 16, 16], 'faces': {'west': {'texture': '#inner'}}},
            {'from': [0, 0, inner], 'to': [16, 16, inner], 'faces': {'south': {'texture': '#inner'}}},
            {'from': [0, 0, 16 - inner], 'to': [16, 16, 16 - inner], 'faces': {'north': {'texture': '#inner'}}},
            {'from': [0, 4, 0], 'to': [16, 4, 16], 'faces': {'up': {'texture': '#floor'}}},
        ]})
    write(ASSETS, 'blockstates/crucible.json', {'variants': {'': {'model': tc('block/crucible')}}})
    write(ASSETS, 'models/item/crucible.json', {'parent': tc('block/crucible')})
    drop_self('crucible')

    # --- нитор: сам блок невидим, модель нужна только ради частиц при разрушении ---
    write(ASSETS, 'blockstates/nitor.json', {'variants': {'': {'model': tc('block/nitor')}}})
    write(ASSETS, 'models/block/nitor.json', {'textures': {'particle': tc('item/nitor')}})
    simple_item('nitor', 'nitor')
    drop_self('nitor')

    simple_block('thaumium_block', 'thaumiumblock')
    drop_self('thaumium_block')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('crucible'), tc('thaumium_block'))
    tag('blocks', 'minecraft:needs_stone_tool', tc('thaumium_block'))
    tag('blocks', 'minecraft:beacon_base_blocks', tc('thaumium_block'))

    for name, texture in (('alumentum', 'alumentum'), ('tallow', 'tallow'), ('thaumium_ingot', 'thaumiumingot'),
                          ('thaumium_nugget', 'nuggetthaumium'), ('cluster_iron', 'clusteriron'),
                          ('cluster_gold', 'clustergold'), ('cluster_copper', 'clustercopper'),
                          ('cluster_cinnabar', 'clustercinnabar'), ('thaumium_helmet', 'thaumiumhelm'),
                          ('thaumium_chestplate', 'thaumiumchest'), ('thaumium_leggings', 'thaumiumlegs'),
                          ('thaumium_boots', 'thaumiumboots')):
        simple_item(name, texture)
    for name, texture in (('thaumium_sword', 'thaumiumsword'), ('thaumium_pickaxe', 'thaumiumpick'),
                          ('thaumium_axe', 'thaumiumaxe'), ('thaumium_shovel', 'thaumiumshovel'),
                          ('thaumium_hoe', 'thaumiumhoe'), ('elemental_sword', 'elementalsword'),
                          ('elemental_pickaxe', 'elementalpick'), ('elemental_axe', 'elementalaxe'),
                          ('elemental_shovel', 'elementalshovel'), ('elemental_hoe', 'elementalhoe')):
        write(ASSETS, 'models/item/%s.json' % name,
              {'parent': 'minecraft:item/handheld', 'textures': {'layer0': tc('item/' + texture)}})

    # очки откровения: предмет, рецепт в магическом верстаке; текстуру на голове задаёт сам предмет
    simple_item('goggles', 'gogglesrevealing')
    arcane('goggles', ['LGL', 'L L', 'TGT'],
           {'T': {'item': tc('thaumometer')}, 'G': {'tag': 'forge:ingots/gold'}, 'L': {'tag': 'forge:leather'}},
           'goggles', {'aer': 5, 'ignis': 5, 'aqua': 5, 'terra': 5, 'perditio': 3, 'ordo': 3}, 'GOGGLES')
    tag('items', 'forge:armors/helmets', tc('goggles'))
    tag('items', 'forge:armors', tc('goggles'))

    # --- кристаллические гроздья: блок рисует CrystalRenderer, предмет — он же; шесть осколков при разрушении ---
    crystal_display = {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 2.5, 0], 'scale': [0.9, 0.9, 0.9]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.4, 0.4, 0.4]},
        'fixed': {'scale': [0.75, 0.75, 0.75]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.55, 0.55, 0.55]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.55, 0.55, 0.55]}}
    for kind in ELEMENTS + ['mixed']:
        name = 'crystal_' + kind
        write(ASSETS, 'models/block/%s.json' % name, {'textures': {'particle': tc('block/crystal')}})
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': 'minecraft:builtin/entity', 'gui_light': 'side',
                                                     'textures': {'particle': tc('block/crystal')}, 'display': crystal_display})
        shards = ELEMENTS if kind == 'mixed' else [kind] * 6
        counts = {}
        for shard in shards:
            counts[shard] = counts.get(shard, 0) + 1
        write(DATA, 'thaumcraft/loot_tables/blocks/%s.json' % name, {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('shard_' + shard), 'functions': [
                {'function': 'minecraft:set_count', 'count': count}]}]} for shard, count in counts.items()]})
        write(DATA, 'thaumcraft/recipes/%s.json' % name, {
            'type': 'minecraft:crafting_shapeless', 'category': 'building',
            'ingredients': [{'item': tc('shard_' + shard)} for shard in shards], 'result': {'item': tc(name)}})

    # --- пьедестал подзарядки и его фокус: BlockStoneDeviceRenderer оригинала ---
    def faces(side, top, bottom):
        out = {f: {'texture': side} for f in ('north', 'south', 'west', 'east')}
        out['up'] = {'texture': top}
        out['down'] = {'texture': bottom}
        return out
    write(ASSETS, 'models/block/wand_pedestal.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': tc('block/wandpedestal_side'), 'side': tc('block/wandpedestal_side'),
                     'top': tc('block/wandpedestal_top'), 'bottom': tc('block/pedestal_top')},
        'elements': [{'from': [x0, y0, x0], 'to': [x1, y1, x1], 'faces': faces('#side', '#top', '#bottom')}
                     for x0, y0, x1, y1 in ((0, 0, 16, 4), (2, 4, 14, 8), (4, 8, 12, 16))]})
    write(ASSETS, 'models/block/wand_pedestal_focus.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': tc('block/wandpedestal_focus_side'), 'side': tc('block/wandpedestal_focus_side'),
                     'top': tc('block/wandpedestal_focus_top'), 'bottom': tc('block/wandpedestal_focus_bot')},
        'elements': [{'from': list(a), 'to': list(b), 'faces': faces('#side', '#top', '#bottom')} for a, b in (
            ((5, 0, 5), (11, 1, 11)), ((1, 0, 7), (5, 1, 9)), ((11, 0, 7), (15, 1, 9)), ((7, 0, 1), (9, 1, 5)),
            ((7, 0, 11), (9, 1, 15)), ((1, 1, 7), (3, 7, 9)), ((7, 1, 1), (9, 7, 3)), ((13, 1, 7), (15, 7, 9)),
            ((7, 1, 13), (9, 7, 15)))]})
    for name in ('wand_pedestal', 'wand_pedestal_focus'):
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        drop_self(name)
    tag('blocks', 'minecraft:mineable/pickaxe', tc('wand_pedestal'), tc('wand_pedestal_focus'))
    charm = {'item': tc('primal_charm')}
    diamond = {'tag': 'forge:gems/diamond'}
    infusion_recipe('wand_pedestal', {'item': tc('pedestal')}, [{'tag': 'forge:ingots/gold'}, diamond, charm, diamond],
                    {'auram': 10, 'praecantatio': 15, 'permutatio': 15}, 3, 'wand_pedestal', 'WANDPED')
    earth = {'item': tc('shard_earth')}
    vis_filter = {'item': tc('filter')}
    infusion_recipe('wand_pedestal_focus', {'item': 'minecraft:comparator'}, [earth, vis_filter] * 4,
                    {'ordo': 10, 'praecantatio': 15, 'permutatio': 10}, 4, 'wand_pedestal_focus', 'WANDPEDFOC')

    # --- зарядное реле верстака: в мире его рисует VisNetRenderers.ChargeRelay, предмет собран из тех же частей ---
    import math

    def matmul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]

    def rot(axis, degrees):
        c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
        return {'x': [[1, 0, 0], [0, c, -s], [0, s, c]], 'z': [[c, -s, 0], [s, c, 0], [0, 0, 1]]}[axis]

    def obj_part(model, turns, offset, extra=None):
        # Загрузчик obj применяет преобразование вокруг центра блока: чтобы часть встала, как в рендерере
        # (поворот вокруг начала координат и сдвиг в offset), к сдвигу добавляется повёрнутый центр.
        m = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
        for axis, degrees in turns:
            m = matmul(m, rot(axis, degrees))
        centre = [sum(m[i][k] * 0.5 for k in range(3)) for i in range(3)]
        part = {'loader': 'forge:obj', 'model': tc('models/block/%s.obj' % model), 'flip_v': True,
                'textures': {'particle': tc('block/vis_relay')},
                'transform': {'translation': [round(offset[i] - 0.5 + centre[i], 4) for i in range(3)],
                              'rotation': [{axis: degrees} for axis, degrees in turns], 'origin': 'corner'}}
        part.update(extra or {})
        return part
    hang = [('x', 90), ('z', 45)]
    children = {
        'float': obj_part('vis_relay_float', hang, (0.5, 0.5, 0.5)),
        'crystal': obj_part('vis_relay_crystal', hang, (0.5, 0.5, 0.5), {'render_type': 'minecraft:translucent'})}
    for a in range(4):
        children['support%d' % a] = obj_part('vis_relay_support', hang + [('x', 180), ('z', 90 * a)], (0.5, 1.0, 0.5))
    charge_display = json.loads(json.dumps(json.load(open(os.path.join(ASSETS, 'models/item/node_stabilizer.json')))['display']))
    # опоры свисают на блок ниже кольца: в ячейке предмет уменьшен, чтобы поместиться целиком
    charge_display['gui']['scale'] = [0.6, 0.6, 0.6]
    charge_display['gui']['translation'] = [0, 1.2, 0]
    write(ASSETS, 'models/item/vis_charge_relay.json', {
        'loader': 'forge:composite', 'textures': {'particle': tc('block/vis_relay')}, 'children': children,
        'item_render_order': ['support0', 'support1', 'support2', 'support3', 'float', 'crystal'], 'display': charge_display})
    write(ASSETS, 'models/block/vis_relay_support.json', {
        'loader': 'forge:obj', 'model': tc('models/block/vis_relay_support.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/vis_relay')}})
    write(ASSETS, 'models/block/vis_charge_relay.json', {'textures': {'particle': tc('block/vis_relay')}})
    write(ASSETS, 'blockstates/vis_charge_relay.json', {'variants': {'': {'model': tc('block/vis_charge_relay')}}})
    drop_self('vis_charge_relay')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('vis_charge_relay'))
    arcane('vis_charge_relay', [' R ', 'W W', 'I I'],
           {'I': {'tag': 'forge:ingots/iron'}, 'R': {'item': tc('vis_relay')}, 'W': {'item': tc('wand_rod_greatwood')}},
           'vis_charge_relay', {'ignis': 16, 'ordo': 16, 'aer': 16}, 'VISCHARGERELAY')

    # --- особые мобы: кто может стать особым (championWhiteList оригинала и их прямые родичи в новых версиях) ---
    tag('entity_types', 'thaumcraft:champion_whitelist', *['minecraft:' + n for n in (
        'zombie', 'husk', 'drowned', 'zombie_villager', 'zombified_piglin', 'spider', 'cave_spider', 'blaze', 'enderman',
        'skeleton', 'stray', 'wither_skeleton', 'witch')])
    tag('entity_types', 'thaumcraft:champion_whitelist', *[tc(n) for n in (
        'pech', 'cultist_knight', 'cultist_cleric', 'cultist_leader', 'eldritch_warden', 'eldritch_golem')])
    simple_item('coin', 'coin')
    # --- рунная защита: зачарованная ткань, кольца ученика, рунные кольца, амулеты и пояса ---
    simple_item('enchanted_fabric', 'cloth')
    arcane('enchanted_fabric', [' S ', 'SCS', ' S '], {'S': {'tag': 'forge:string'}, 'C': {'tag': 'minecraft:wool'}},
           'enchanted_fabric', {a: 1 for a in ('aer', 'terra', 'ignis', 'aqua', 'ordo', 'perditio')}, 'ENCHFABRIC')
    for element in ELEMENTS:
        simple_item('apprentice_ring_' + element, 'bauble_ring_iron')
        tag('items', 'curios:ring', tc('apprentice_ring_' + element))
    for name, texture, slot in (
            ('runic_ring_lesser', 'runic_ring_lesser', 'ring'), ('runic_ring', 'runic_ring', 'ring'),
            ('runic_ring_charged', 'runic_ring_charged', 'ring'), ('runic_ring_healing', 'runic_ring_regen', 'ring'),
            ('runic_amulet', 'runic_amulet', 'necklace'), ('runic_amulet_emergency', 'runic_amulet_emergency', 'necklace'),
            ('runic_girdle', 'runic_girdle', 'belt'), ('runic_girdle_kinetic', 'runic_girdle_kinetic', 'belt')):
        simple_item(name, texture)
        tag('items', 'curios:' + slot, tc(name))
    # вещи без собственного щита, которым его даёт «Рунное усиление»
    tag('items', 'thaumcraft:runic_armor', *[tc(n) for n in (
        'goggles', 'thaumium_helmet', 'thaumium_chestplate', 'thaumium_leggings', 'thaumium_boots', 'bauble_amulet',
        'fortress_helmet', 'fortress_chestplate', 'fortress_leggings', 'robe_chestplate', 'robe_leggings', 'robe_boots',
        'traveller_boots', 'hover_harness', 'hover_girdle',
        'bauble_ring', 'bauble_belt', 'vis_stone', 'vis_amulet')] + [tc('apprentice_ring_' + e) for e in ELEMENTS])
    write(DATA, 'thaumcraft/recipes/infusion/runic_augment.json', {'type': tc('runic_augment'), 'research': 'RUNICAUGMENTATION'})
    r_charm = {'item': tc('primal_charm')}
    r_amber = {'item': tc('amber')}
    r_fabric = {'item': tc('enchanted_fabric')}
    r_nitor = {'item': tc('nitor')}
    r_ink = {'item': tc('scribing_tools')}
    r_balanced = {'item': tc('shard_balanced')}

    def potion(item, kind):
        return {'type': 'forge:partial_nbt', 'item': 'minecraft:' + item, 'nbt': {'Potion': 'minecraft:' + kind}}

    def four(shard):
        return [{'item': tc('shard_' + shard)}] * 2
    infusion_recipe('runic_ring', {'item': tc('bauble_ring')}, [r_charm, r_amber, r_fabric, r_nitor, r_ink],
                    {'tutamen': 10, 'praecantatio': 25, 'potentia': 25}, 3, 'runic_ring', 'RUNICARMOR')
    infusion_recipe('runic_amulet', {'item': tc('bauble_amulet')}, [r_charm, r_amber, r_fabric, r_nitor, r_nitor, r_ink],
                    {'tutamen': 20, 'praecantatio': 35, 'potentia': 35}, 4, 'runic_amulet', 'RUNICARMOR')
    infusion_recipe('runic_girdle', {'item': tc('bauble_belt')}, [r_charm, r_amber, r_fabric, r_nitor, r_nitor, r_nitor, r_ink],
                    {'tutamen': 30, 'praecantatio': 50, 'potentia': 50}, 4, 'runic_girdle', 'RUNICARMOR')
    infusion_recipe('runic_ring_charged', {'item': tc('runic_ring')},
                    [r_balanced] + four('fire') + [potion('potion', 'strong_swiftness')] + four('fire'),
                    {'tutamen': 16, 'praecantatio': 16, 'potentia': 64}, 6, 'runic_ring_charged', 'RUNICCHARGED')
    infusion_recipe('runic_ring_healing', {'item': tc('runic_ring')},
                    [r_balanced] + four('water') + [potion('potion', 'long_regeneration')] + four('water'),
                    {'tutamen': 16, 'praecantatio': 16, 'aqua': 32, 'sano': 32}, 6, 'runic_ring_healing', 'RUNICHEALING')
    infusion_recipe('runic_amulet_emergency', {'item': tc('runic_amulet')},
                    [r_balanced] + four('earth') + [potion('potion', 'strong_strength')] + four('earth'),
                    {'tutamen': 20, 'praecantatio': 35, 'terra': 32, 'vacuos': 32}, 7, 'runic_amulet_emergency', 'RUNICEMERGENCY')
    infusion_recipe('runic_girdle_kinetic', {'item': tc('runic_girdle')},
                    [r_balanced] + four('air') + [potion('splash_potion', 'strong_harming')] + four('air'),
                    {'tutamen': 33, 'praecantatio': 55, 'aer': 64}, 7, 'runic_girdle_kinetic', 'RUNICKINETIC')
    # фрагмент знаний; девять фрагментов — «неизвестное знание» (заметки с пометкой unknown)
    simple_item('knowledge_fragment', 'knowledgefragment')
    write(DATA, 'thaumcraft/recipes/unknown_knowledge.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['KKK', 'KKK', 'KKK'],
        'key': {'K': {'item': tc('knowledge_fragment')}},
        'result': {'item': tc('research_notes'), 'nbt': {'unknown': True}}})

    # --- добыча мода в сундуках мира: ChestGenHooks оригинала. Число бросков и общий вес ванильного списка —
    # из таблиц 1.7.10; tiers — веса одного предмета из обычного, необычного и редкого набора ---
    chests = (
        ('simple_dungeon', 8, 8, 120, [5, 4, 1], []),
        ('desert_pyramid', 2, 7, 82, [5, 4, 1], []),
        ('jungle_temple', 2, 7, 73, [5, 4, 1], []),
        ('abandoned_mineshaft', 3, 7, 80, [4, 3, 1], []),
        ('stronghold_corridor', 2, 4, 100, [4, 3, 1], []),
        ('stronghold_crossing', 1, 5, 62, [4, 3, 1], []),
        ('stronghold_library', 1, 5, 47, [4, 3, 1], [{'item': tc('knowledge_fragment'), 'weight': 20, 'min': 3, 'max': 6}]),
        ('village/village_weaponsmith', 3, 9, 94, [0, 0, 0], [{'item': tc('thaumium_ingot'), 'weight': 10, 'min': 1, 'max': 3}]))
    modifiers = []
    for table, rolls_min, rolls_max, vanilla, tiers, extra in chests:
        name = 'chest_' + table.split('/')[-1]
        modifiers.append(tc(name))
        write(DATA, 'thaumcraft/loot_modifiers/%s.json' % name, {
            'type': tc('chest_loot'),
            'conditions': [{'condition': 'forge:loot_table_id', 'loot_table_id': 'minecraft:chests/' + table}],
            'rolls_min': rolls_min, 'rolls_max': rolls_max, 'vanilla_weight': vanilla, 'tiers': tiers, 'extra': extra})
    # самородные грозди из-под кирки ядра
    write(DATA, 'thaumcraft/loot_modifiers/native_clusters.json', {'type': tc('native_clusters'), 'conditions': []})
    modifiers.append(tc('native_clusters'))
    write(DATA, 'forge/loot_modifiers/global_loot_modifiers.json', {'replace': False, 'entries': modifiers})
    simple_item('loot_bag_common', 'lootbag')
    simple_item('loot_bag_uncommon', 'lootbagunc')
    simple_item('loot_bag_rare', 'lootbagrare')

    # --- порча: биом порченой земли, корка, почва, плоть, волокна и растения ---
    write(DATA, 'thaumcraft/worldgen/biome/tainted_land.json', {
        'temperature': 0.5, 'downfall': 0.5, 'has_precipitation': True,
        'effects': {'sky_color': 0x7c44ff, 'fog_color': 12638463, 'water_color': 0xcc1188, 'water_fog_color': 0x440a2c,
                    'grass_color': 0x6d4189, 'foliage_color': 0x7c6d87,
                    'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}},
        # в порченых землях не водится никто, кроме тентаклей
        'spawners': dict({group: [] for group in ('ambient', 'axolotls', 'creature', 'misc', 'underground_water_creature',
                                                  'water_ambient', 'water_creature')},
                         monster=[{'type': tc('taintacle'), 'weight': 1, 'minCount': 1, 'maxCount': 1}]),
        'spawn_costs': {},
        'carvers': {'air': ['minecraft:cave', 'minecraft:cave_extra_underground', 'minecraft:canyon']},
        # то же, что в обычном лесу, но без цветов и тростника; деревьев по два на чанк, как в оригинале,
        # плюс глыбы корки и волокна порчи
        'features': [
            [],
            ['minecraft:lake_lava_underground', 'minecraft:lake_lava_surface'],
            ['minecraft:amethyst_geode', tc('taint_crust_blob')],
            ['minecraft:monster_room', 'minecraft:monster_room_deep'],
            [], [],
            ['minecraft:ore_dirt', 'minecraft:ore_gravel', 'minecraft:ore_granite_upper', 'minecraft:ore_granite_lower',
             'minecraft:ore_diorite_upper', 'minecraft:ore_diorite_lower', 'minecraft:ore_andesite_upper',
             'minecraft:ore_andesite_lower', 'minecraft:ore_tuff', 'minecraft:ore_coal_upper', 'minecraft:ore_coal_lower',
             'minecraft:ore_iron_upper', 'minecraft:ore_iron_middle', 'minecraft:ore_iron_small', 'minecraft:ore_gold',
             'minecraft:ore_gold_lower', 'minecraft:ore_redstone', 'minecraft:ore_redstone_lower', 'minecraft:ore_diamond',
             'minecraft:ore_diamond_large', 'minecraft:ore_diamond_buried', 'minecraft:ore_lapis', 'minecraft:ore_lapis_buried',
             'minecraft:ore_copper', 'minecraft:underwater_magma', 'minecraft:disk_sand', 'minecraft:disk_clay',
             'minecraft:disk_gravel'],
            [],
            ['minecraft:spring_water', 'minecraft:spring_lava'],
            ['minecraft:glow_lichen', tc('taint_trees'), 'minecraft:patch_grass_forest', 'minecraft:brown_mushroom_normal',
             'minecraft:red_mushroom_normal', 'minecraft:patch_pumpkin', tc('taint_fibres')],
            ['minecraft:freeze_top_layer']]})
    tag('worldgen/biome', 'minecraft:is_overworld', tc('tainted_land'))

    # --- жуткие земли и волшебный лес ---
    ORES = ['minecraft:ore_dirt', 'minecraft:ore_gravel', 'minecraft:ore_granite_upper', 'minecraft:ore_granite_lower',
            'minecraft:ore_diorite_upper', 'minecraft:ore_diorite_lower', 'minecraft:ore_andesite_upper',
            'minecraft:ore_andesite_lower', 'minecraft:ore_tuff', 'minecraft:ore_coal_upper', 'minecraft:ore_coal_lower',
            'minecraft:ore_iron_upper', 'minecraft:ore_iron_middle', 'minecraft:ore_iron_small', 'minecraft:ore_gold',
            'minecraft:ore_gold_lower', 'minecraft:ore_redstone', 'minecraft:ore_redstone_lower', 'minecraft:ore_diamond',
            'minecraft:ore_diamond_large', 'minecraft:ore_diamond_buried', 'minecraft:ore_lapis', 'minecraft:ore_lapis_buried',
            'minecraft:ore_copper', 'minecraft:underwater_magma', 'minecraft:disk_sand', 'minecraft:disk_clay',
            'minecraft:disk_gravel']

    def spawn(kind, weight, low, high):
        return {'type': kind, 'weight': weight, 'minCount': low, 'maxCount': high}

    # то, что водится в любом обычном лесу
    MONSTERS = [spawn('minecraft:spider', 100, 4, 4), spawn('minecraft:zombie', 95, 4, 4),
                spawn('minecraft:zombie_villager', 5, 1, 1), spawn('minecraft:skeleton', 100, 4, 4),
                spawn('minecraft:creeper', 100, 4, 4), spawn('minecraft:slime', 100, 4, 4),
                spawn('minecraft:enderman', 10, 1, 4), spawn('minecraft:witch', 5, 1, 1)]
    ANIMALS = [spawn('minecraft:sheep', 12, 4, 4), spawn('minecraft:pig', 10, 4, 4), spawn('minecraft:chicken', 10, 4, 4),
               spawn('minecraft:cow', 8, 4, 4)]

    def biome(name, temperature, downfall, effects, creatures, monsters, local, vegetal):
        effects = dict(effects, fog_color=12638463, water_fog_color=329011,
                       mood_sound={'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0})
        write(DATA, 'thaumcraft/worldgen/biome/%s.json' % name, {
            'temperature': temperature, 'downfall': downfall, 'has_precipitation': True, 'effects': effects,
            'spawners': {'ambient': [spawn('minecraft:bat', 10, 8, 8)], 'axolotls': [], 'creature': creatures, 'misc': [],
                         'monster': monsters, 'underground_water_creature': [spawn('minecraft:glow_squid', 10, 4, 6)],
                         'water_ambient': [], 'water_creature': []},
            'spawn_costs': {},
            'carvers': {'air': ['minecraft:cave', 'minecraft:cave_extra_underground', 'minecraft:canyon']},
            'features': [[], ['minecraft:lake_lava_underground', 'minecraft:lake_lava_surface'],
                         ['minecraft:amethyst_geode'] + local, ['minecraft:monster_room', 'minecraft:monster_room_deep'],
                         [], [], ORES, [], ['minecraft:spring_water', 'minecraft:spring_lava'],
                         ['minecraft:glow_lichen'] + vegetal, ['minecraft:freeze_top_layer']]})
        tag('worldgen/biome', 'minecraft:is_overworld', tc(name))
        tag('worldgen/biome', 'forge:is_magical', tc(name))

    def placed(name, feature, count, heightmap='MOTION_BLOCKING', tree=False):
        steps = [{'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'}]
        if tree:
            steps += [{'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
                      {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
                      {'type': 'minecraft:block_predicate_filter', 'predicate': {
                          'type': 'minecraft:would_survive',
                          'state': {'Name': 'minecraft:oak_sapling', 'Properties': {'stage': '0'}}}}]
        else:
            steps.append({'type': 'minecraft:heightmap', 'heightmap': heightmap})
        steps.append({'type': 'minecraft:biome'})
        write(DATA, 'thaumcraft/worldgen/placed_feature/%s.json' % name, {'feature': feature, 'placement': steps})

    # Жуткие земли: сами не генерируются — их создают тёмные узлы. Обычный редкий лес, где животных нет, а к обычным
    # монстрам добавлены ведьмы, эндермены и злые зомби. Виспы и древние стражи добавятся вместе с самими мобами.
    write(DATA, 'thaumcraft/worldgen/configured_feature/eerie_trees.json', {
        'type': 'minecraft:random_selector', 'config': {
            'default': {'feature': 'minecraft:oak', 'placement': []},
            'features': [{'chance': 0.1, 'feature': {'feature': 'minecraft:fancy_oak', 'placement': []}}]}})
    placed('eerie_trees', tc('eerie_trees'), 2, tree=True)
    placed('eerie_flowers', 'minecraft:flower_default', 1)
    placed('eerie_grass', 'minecraft:patch_grass', 2, 'WORLD_SURFACE_WG')
    biome('eerie', 0.5, 0.5, {'sky_color': 2237081, 'water_color': 3035999, 'grass_color': 4212800, 'foliage_color': 4215616},
          [], MONSTERS + [spawn('minecraft:witch', 8, 1, 1), spawn('minecraft:enderman', 4, 1, 1),
                          spawn(tc('brainy_zombie'), 32, 1, 1), spawn(tc('giant_brainy_zombie'), 8, 1, 1),
                          spawn(tc('wisp'), 3, 1, 1), spawn(tc('eldritch_guardian'), 1, 1, 1)],
          [], [tc('eerie_trees'), tc('eerie_flowers'), tc('eerie_grass'), 'minecraft:brown_mushroom_normal',
               'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin'])
    tag('worldgen/biome', 'forge:is_spooky', tc('eerie'))

    # Волшебный лес: большие дубы, каждое четырнадцатое дерево серебряное, каждое десятое из остальных — великое;
    # море цветов и травы, кувшинки, грибы (в том числе гигантские) и замшелые валуны.
    # Печи и виспы добавятся в список обитателей вместе с самими мобами; стручки маны и вишрумы — вместе с этими растениями.
    write(DATA, 'thaumcraft/worldgen/configured_feature/magical_trees.json', {
        'type': 'minecraft:random_selector', 'config': {
            'default': {'feature': 'minecraft:fancy_oak', 'placement': []},
            'features': [{'chance': 1 / 14.0, 'feature': {'feature': tc('silverwood_tree'), 'placement': []}},
                         {'chance': 0.1, 'feature': {'feature': tc('greatwood_tree'), 'placement': []}}]}})
    placed('magical_trees', tc('magical_trees'), 2, tree=True)
    placed('magical_rocks', 'minecraft:forest_rock', {'type': 'minecraft:uniform', 'value': {'min_inclusive': 0, 'max_inclusive': 2}})
    # шестнадцать мест на чанк, в каждом гигантский гриб вырастает с шансом 1 из 40
    placed('magical_huge_mushrooms', 'minecraft:mushroom_island_vegetation',
           {'type': 'minecraft:weighted_list', 'distribution': [{'data': 0, 'weight': 3}, {'data': 1, 'weight': 2}]})
    placed('magical_flowers', 'minecraft:flower_flower_forest', 10)
    placed('magical_grass', 'minecraft:patch_grass_jungle', 12, 'WORLD_SURFACE_WG')
    placed('magical_waterlily', 'minecraft:patch_waterlily', 6, 'WORLD_SURFACE_WG')
    placed('magical_brown_mushrooms', 'minecraft:patch_brown_mushroom', 2)
    placed('magical_red_mushrooms', 'minecraft:patch_red_mushroom', 1)
    biome('magical_forest', 0.7, 0.6, {'sky_color': 7907327, 'water_color': 30702, 'grass_color': 5635969, 'foliage_color': 6750149},
          ANIMALS + [spawn('minecraft:wolf', 5, 4, 4), spawn('minecraft:wolf', 2, 1, 3), spawn('minecraft:horse', 2, 1, 3)],
          MONSTERS + [spawn('minecraft:witch', 3, 1, 1), spawn('minecraft:enderman', 3, 1, 1), spawn(tc('wisp'), 10, 1, 2),
                      spawn(tc('pech'), 10, 1, 2)],
          [tc('magical_rocks'), tc('magical_huge_mushrooms')],
          [tc('magical_trees'), tc('magical_flowers'), tc('magical_grass'), tc('magical_waterlily'),
           tc('magical_brown_mushrooms'), tc('magical_red_mushrooms'), 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin',
           tc('magical_plants')])
    write(DATA, 'thaumcraft/worldgen/configured_feature/magical_plants.json', {'type': tc('magical_plants'), 'config': {}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/magical_plants.json', {'feature': tc('magical_plants'), 'placement': []})
    tag('worldgen/biome', 'minecraft:is_forest', tc('magical_forest'))

    # злой и яростный зомби: гнилая плоть, с шансом один к двум — мозг; с яростного изредка ещё что-нибудь ценное
    def flesh(rolls, count):
        return {'rolls': rolls, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:rotten_flesh',
                                             'functions': [{'function': 'minecraft:set_count', 'count': count}]}],
                'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.5}]}
    brain = {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('zombie_brain')}],
             'conditions': [{'condition': 'minecraft:random_chance_with_looting', 'chance': 0.5, 'looting_multiplier': 0.1}]}
    write(DATA, 'thaumcraft/loot_tables/entities/brainy_zombie.json', {'type': 'minecraft:entity', 'pools': [flesh(3, 1), brain]})
    write(DATA, 'thaumcraft/loot_tables/entities/giant_brainy_zombie.json', {'type': 'minecraft:entity', 'pools': [
        flesh(12, 2), brain,
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': name} for name in (
            tc('thaumium_ingot'), 'minecraft:carrot', 'minecraft:potato', tc('amber'))],
         'conditions': [{'condition': 'minecraft:killed_by_player'},
                        {'condition': 'minecraft:random_chance_with_looting', 'chance': 0.025, 'looting_multiplier': 0.01}]}]})
    simple_item('zombie_brain', 'brain')
    simple_item('wisp_essence', 'wispessence')
    # печи появляются во всех волшебных биомах (в порченых землях их не пускает само правило появления);
    # добыча печа собирается кодом из его тюка, таблица пуста
    write(DATA, 'thaumcraft/forge/biome_modifier/pech.json', {
        'type': 'forge:add_spawns', 'biomes': '#forge:is_magical', 'spawners': spawn(tc('pech'), 10, 1, 1)})
    write(DATA, 'thaumcraft/loot_tables/entities/pech.json', {'type': 'minecraft:entity', 'pools': []})
    # виспы появляются ещё и в Нижнем мире, а злые зомби — в обычных лесах, равнинах, тайге и джунглях
    # (в оригинале — в биомах, где может появиться игрок)
    write(DATA, 'thaumcraft/forge/biome_modifier/wisp_nether.json', {
        'type': 'forge:add_spawns', 'biomes': '#minecraft:is_nether', 'spawners': spawn(tc('wisp'), 5, 1, 1)})
    write(DATA, 'thaumcraft/forge/biome_modifier/brainy_zombie.json', {
        'type': 'forge:add_spawns', 'biomes': ['minecraft:forest', 'minecraft:plains', 'minecraft:taiga', 'minecraft:jungle'],
        'spawners': spawn(tc('brainy_zombie'), 10, 1, 1)})
    tag('worldgen/biome', 'forge:is_magical', tc('tainted_land'))
    tag('worldgen/biome', 'forge:is_wasteland', tc('tainted_land'))
    # глыбы корки порчи: до двух на чанк, как замшелые валуны в тайге
    write(DATA, 'thaumcraft/worldgen/configured_feature/taint_crust_blob.json', {
        'type': 'minecraft:forest_rock', 'config': {'state': {'Name': tc('taint_crust')}}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/taint_crust_blob.json', {
        'feature': tc('taint_crust_blob'), 'placement': [
            {'type': 'minecraft:count', 'count': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 0, 'max_inclusive': 2}}},
            {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'},
            {'type': 'minecraft:biome'}]})
    # деревья: обычные дубы, примерно каждый пятый — большой (в оригинале каждое восьмое дерево было «большим
    # волшебным», а из остальных каждое десятое — большим дубом)
    write(DATA, 'thaumcraft/worldgen/configured_feature/taint_trees.json', {
        'type': 'minecraft:random_selector', 'config': {
            'default': {'feature': 'minecraft:oak', 'placement': []},
            'features': [{'chance': 0.2125, 'feature': {'feature': 'minecraft:fancy_oak', 'placement': []}}]}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/taint_trees.json', {
        'feature': tc('taint_trees'), 'placement': [
            {'type': 'minecraft:count', 'count': 2}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
            {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
            {'type': 'minecraft:block_predicate_filter', 'predicate': {
                'type': 'minecraft:would_survive', 'state': {'Name': 'minecraft:oak_sapling', 'Properties': {'stage': '0'}}}},
            {'type': 'minecraft:biome'}]})
    write(DATA, 'thaumcraft/worldgen/configured_feature/taint_fibres.json', {'type': tc('taint_fibres'), 'config': {}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/taint_fibres.json', {'feature': tc('taint_fibres'), 'placement': []})
    simple_block('taint_crust', 'taint_crust')
    simple_block('flesh_block', 'fleshblock')
    write(ASSETS, 'blockstates/tainted_soil.json', {'variants': {'': {'model': tc('block/tainted_soil')}}})
    write(ASSETS, 'models/block/tainted_soil.json', {'parent': 'minecraft:block/leaves', 'textures': {'all': tc('block/taint_soil')}})
    write(ASSETS, 'models/item/tainted_soil.json', {'parent': tc('block/tainted_soil')})
    write(DATA, 'thaumcraft/loot_tables/blocks/tainted_soil.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:dirt'}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    silk = {'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch',
                                                                                  'levels': {'min': 1}}]}}
    write(DATA, 'thaumcraft/loot_tables/blocks/flesh_block.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc('flesh_block'), 'conditions': [silk]},
            {'type': 'minecraft:item', 'name': 'minecraft:rotten_flesh',
             'functions': [{'function': 'minecraft:set_count', 'count': 9}]}]}]}]})
    write(DATA, 'thaumcraft/recipes/flesh_block.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['FFF', 'FFF', 'FFF'],
        'key': {'F': {'item': 'minecraft:rotten_flesh'}}, 'result': {'item': tc('flesh_block')}})

    # волокна: плёнка на каждой твёрдой грани вокруг; на одной грани из двадцати — светящийся узор
    def fibre_face(overlay=None):
        elements = [{'from': [0, 0, 0.08], 'to': [16, 16, 0.08], 'shade': False,
                     'faces': {s: {'texture': '#fibres', 'uv': [0, 0, 16, 16], 'tintindex': 0} for s in ('north', 'south')}}]
        textures = {'particle': tc('block/taint_fibres'), 'fibres': tc('block/taint_fibres')}
        if overlay:
            textures['over'] = tc('block/' + overlay)
            elements.append({'from': [0, 0, 0.32], 'to': [16, 16, 0.32], 'shade': False,
                             'forge_data': {'block_light': 12, 'sky_light': 12},
                             'faces': {s: {'texture': '#over', 'uv': [0, 0, 16, 16]} for s in ('north', 'south')}})
        return {'render_type': 'minecraft:translucent', 'ambientocclusion': False, 'textures': textures, 'elements': elements}
    write(ASSETS, 'models/block/taint_fibres.json', fibre_face())
    for n in (1, 2, 3):
        write(ASSETS, 'models/block/taint_fibres_over_%d.json' % n, fibre_face('taint_over_%d' % n))
    turns = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    multipart = []
    for side, turn in turns.items():
        variants = [dict(turn, model=tc('block/taint_fibres'), weight=57)]
        variants += [dict(turn, model=tc('block/taint_fibres_over_%d' % n), weight=1) for n in (1, 2, 3)]
        multipart.append({'when': {side: 'true'}, 'apply': variants})
    write(ASSETS, 'blockstates/taint_fibres.json', {'multipart': multipart})
    write(ASSETS, 'models/item/taint_fibres.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('block/taint_fibres')}})
    for name, texture in (('taint_grass', 'taintgrass1'), ('taint_plant', 'taintgrass2')):
        write(ASSETS, 'models/block/%s.json' % name, {'parent': 'minecraft:block/tinted_cross', 'render_type': 'minecraft:cutout',
                                                      'textures': {'cross': tc('block/' + texture)}})
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('block/' + texture)}})
    for suffix, texture in (('', 'taint_spore_stalk_1'), ('_ripe', 'taint_spore_stalk_2')):
        write(ASSETS, 'models/block/spore_stalk%s.json' % suffix, {
            'parent': 'minecraft:block/tinted_cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': tc('block/' + texture)}})
    write(ASSETS, 'blockstates/spore_stalk.json', {'variants': {'ripe=false': {'model': tc('block/spore_stalk')},
                                                               'ripe=true': {'model': tc('block/spore_stalk_ripe')}}})
    write(ASSETS, 'models/item/spore_stalk.json', {'parent': 'minecraft:item/generated',
                                                   'textures': {'layer0': tc('block/taint_spore_stalk_1')}})
    tag('blocks', 'thaumcraft:taint', tc('taint_crust'), tc('tainted_soil'), tc('flesh_block'), tc('taint_fibres'),
        tc('taint_grass'), tc('taint_plant'), tc('spore_stalk'))
    tag('blocks', 'thaumcraft:taint_to_crust', '#minecraft:logs', 'minecraft:pumpkin', 'minecraft:carved_pumpkin',
        'minecraft:jack_o_lantern', 'minecraft:melon', 'minecraft:cactus')
    tag('blocks', 'thaumcraft:taint_to_soil', '#minecraft:dirt', '#minecraft:sand', 'minecraft:gravel', 'minecraft:clay',
        'minecraft:farmland', 'minecraft:dirt_path', 'minecraft:soul_sand', 'minecraft:soul_soil')
    tag('blocks', 'minecraft:mineable/shovel', tc('tainted_soil'))
    tag('blocks', 'minecraft:mineable/hoe', tc('taint_crust'), tc('flesh_block'), tc('taint_fibres'), tc('taint_grass'),
        tc('taint_plant'), tc('spore_stalk'))

    # --- порождения порчи: добыча ---
    simple_item('taint_goo', 'taint_slime')
    # порча в бутылке: флакон эссенции порчи в тигле с порчей и магией
    simple_item('bottle_taint', 'bottle_taint')
    # предметы, создание которых даёт искажение; остальные из списка оригинала добавятся вместе с самими предметами
    tag('items', 'thaumcraft:warping', tc('bottle_taint'), tc('focus_hellbat'), tc('focus_primal'), tc('wand_rod_primal_staff'))
    crucible_recipe('bottle_taint', {'type': 'forge:partial_nbt', 'item': tc('phial'), 'nbt': {'Aspect': 'vitium'}},
                    {'vitium': 8, 'praecantatio': 8}, tc('bottle_taint'), 'BOTTLETAINT')
    simple_item('taint_tendril', 'taint_tendril')

    def taint_drop(chance=None, rolls=1):
        pool = {'rolls': rolls, 'entries': [{'type': 'minecraft:item', 'name': tc('taint_goo')},
                                           {'type': 'minecraft:item', 'name': tc('taint_tendril')}]}
        if chance is not None:
            pool['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
        return {'type': 'minecraft:entity', 'pools': [pool]}
    write(DATA, 'thaumcraft/loot_tables/entities/taint_spider.json', taint_drop(1 / 6.0))
    write(DATA, 'thaumcraft/loot_tables/entities/taint_spore.json', taint_drop())
    write(DATA, 'thaumcraft/loot_tables/entities/taint_spore_swarmer.json', taint_drop(rolls=2))
    # порченые животные и прочие: слизь или усик, у каждого со своей вероятностью
    write(DATA, 'thaumcraft/loot_tables/entities/tainted_cow.json', taint_drop())
    write(DATA, 'thaumcraft/loot_tables/entities/tainted_creeper.json', taint_drop())
    write(DATA, 'thaumcraft/loot_tables/entities/taintacle.json', taint_drop())
    write(DATA, 'thaumcraft/loot_tables/entities/tainted_pig.json', taint_drop(1 / 3.0))
    write(DATA, 'thaumcraft/loot_tables/entities/tainted_sheep.json', taint_drop(1 / 3.0))
    write(DATA, 'thaumcraft/loot_tables/entities/tainted_chicken.json', {'type': 'minecraft:entity', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('taint_goo'), 'weight': 1},
                                {'type': 'minecraft:item', 'name': tc('taint_tendril'), 'weight': 3}]}]})
    villager = taint_drop(0.5)
    villager['pools'].append({'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('coin')}],
                              'conditions': [{'condition': 'minecraft:random_chance_with_looting', 'chance': 1 / 13.0,
                                              'looting_multiplier': 1 / 13.0}]})
    write(DATA, 'thaumcraft/loot_tables/entities/tainted_villager.json', villager)
    write(DATA, 'thaumcraft/loot_tables/entities/taintacle_small.json', {'type': 'minecraft:entity', 'pools': []})
    write(DATA, 'thaumcraft/loot_tables/entities/thaumic_slime.json', {'type': 'minecraft:entity', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('taint_goo')}],
        'conditions': [{'condition': 'minecraft:random_chance', 'chance': 1 / 3.0}]}]})
    write(DATA, 'thaumcraft/loot_tables/entities/taint_swarm.json', {'type': 'minecraft:entity', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('taint_goo')}],
        'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.5}]}]})

    # --- камень вис и амулет хранения вис: украшения на шею ---
    simple_item('vis_stone', 'vis_amulet_lesser')
    simple_item('vis_amulet', 'vis_amulet')
    tag('items', 'curios:necklace', tc('vis_stone'), tc('vis_amulet'))
    mixed = {'item': tc('crystal_mixed')}
    infusion_recipe('vis_amulet', {'item': tc('bauble_amulet')}, [charm, mixed, mixed, charm, mixed, mixed],
                    {'auram': 24, 'potentia': 64, 'praecantatio': 64, 'vacuos': 24}, 6, 'vis_amulet', 'VISAMULET')

    # текстуры брони лежат там, где их ищет игра по имени материала
    source = os.path.join(ROOT, 'reference', 'tc4', 'src', 'assets', 'thaumcraft', 'textures', 'models')
    target = os.path.join(ASSETS, 'textures', 'models', 'armor')
    os.makedirs(target, exist_ok=True)
    for layer in (1, 2):
        shutil.copyfile(os.path.join(source, 'thaumium_%d.png' % layer),
                        os.path.join(target, 'thaumium_layer_%d.png' % layer))

    tag('items', 'forge:ingots/thaumium', tc('thaumium_ingot'))
    tag('items', 'forge:ingots', tc('thaumium_ingot'))
    tag('items', 'forge:nuggets/thaumium', tc('thaumium_nugget'))
    tag('items', 'forge:nuggets', tc('thaumium_nugget'))
    tag('items', 'forge:storage_blocks/thaumium', tc('thaumium_block'))
    tag('items', 'forge:storage_blocks', tc('thaumium_block'))
    tag('blocks', 'forge:storage_blocks/thaumium', tc('thaumium_block'))
    tag('blocks', 'forge:storage_blocks', tc('thaumium_block'))
    tag('items', 'minecraft:beacon_payment_items', tc('thaumium_ingot'))
    tag('items', 'minecraft:swords', tc('thaumium_sword'), tc('elemental_sword'))
    tag('items', 'minecraft:pickaxes', tc('thaumium_pickaxe'), tc('elemental_pickaxe'))
    tag('items', 'minecraft:axes', tc('thaumium_axe'), tc('elemental_axe'))
    tag('items', 'minecraft:shovels', tc('thaumium_shovel'), tc('elemental_shovel'))
    tag('items', 'minecraft:hoes', tc('thaumium_hoe'), tc('elemental_hoe'))
    tag('items', 'minecraft:trimmable_armor', *[tc('thaumium_' + p) for p in ('helmet', 'chestplate', 'leggings', 'boots')])

    compress('thaumium', 'thaumium_nugget', 'thaumium_ingot')
    compress('thaumium', 'thaumium_ingot', 'thaumium_block')

    ingot = {'tag': 'forge:ingots/thaumium'}
    stick = {'tag': 'forge:rods/wooden'}
    shaped('thaumium_helmet', ['III', 'I I'], {'I': ingot}, 'thaumium_helmet', 'equipment')
    shaped('thaumium_chestplate', ['I I', 'III', 'III'], {'I': ingot}, 'thaumium_chestplate', 'equipment')
    shaped('thaumium_leggings', ['III', 'I I', 'I I'], {'I': ingot}, 'thaumium_leggings', 'equipment')
    shaped('thaumium_boots', ['I I', 'I I'], {'I': ingot}, 'thaumium_boots', 'equipment')
    shaped('thaumium_shovel', ['I', 'S', 'S'], {'I': ingot, 'S': stick}, 'thaumium_shovel', 'equipment')
    shaped('thaumium_pickaxe', ['III', ' S ', ' S '], {'I': ingot, 'S': stick}, 'thaumium_pickaxe', 'equipment')
    shaped('thaumium_axe', ['II', 'SI', 'S '], {'I': ingot, 'S': stick}, 'thaumium_axe', 'equipment')
    shaped('thaumium_sword', ['I', 'I', 'S'], {'I': ingot, 'S': stick}, 'thaumium_sword', 'equipment')
    shaped('thaumium_hoe', ['II', 'S ', 'S '], {'I': ingot, 'S': stick}, 'thaumium_hoe', 'equipment')

    # самородная руда переплавляется в два слитка
    for cluster, result in (('cluster_iron', 'minecraft:iron_ingot'), ('cluster_gold', 'minecraft:gold_ingot'),
                            ('cluster_copper', 'minecraft:copper_ingot'), ('cluster_cinnabar', tc('quicksilver'))):
        write(DATA, 'thaumcraft/recipes/%s_smelting.json' % cluster, {
            'type': 'minecraft:smelting', 'category': 'misc', 'ingredient': {'item': tc(cluster)},
            'result': {'item': result, 'count': 2}, 'experience': 1.0, 'cookingtime': 200})

    # --- рецепты тигля, все числа из ConfigRecipes оригинала ---
    primals = ['aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio']
    for element, own in zip(ELEMENTS, primals):
        crucible_recipe('balanced_shard_' + element, {'item': tc('shard_' + element)},
                        {a: 2 for a in primals if a != own}, tc('shard_balanced'), 'CRUCIBLE')
    crucible_recipe('alumentum', [{'item': 'minecraft:coal'}, {'item': 'minecraft:charcoal'}],
                    {'potentia': 3, 'ignis': 3, 'perditio': 3}, tc('alumentum'), 'ALUMENTUM')
    crucible_recipe('nitor', {'tag': 'forge:dusts/glowstone'}, {'potentia': 3, 'ignis': 3, 'lux': 3}, tc('nitor'), 'NITOR')
    crucible_recipe('thaumium', {'item': 'minecraft:iron_ingot'}, {'praecantatio': 4}, tc('thaumium_ingot'), 'THAUMIUM')
    crucible_recipe('tallow', {'item': 'minecraft:rotten_flesh'}, {'praecantatio': 2}, tc('tallow'), 'TALLOW')
    crucible_recipe('alt_gunpowder', {'item': 'minecraft:gunpowder'}, {'ignis': 4, 'perditio': 4},
                    'minecraft:gunpowder', 'ALCHEMICALDUPLICATION', 2)
    crucible_recipe('alt_slime', {'item': 'minecraft:slime_ball'}, {'aqua': 2, 'victus': 2},
                    'minecraft:slime_ball', 'ALCHEMICALDUPLICATION', 2)
    crucible_recipe('alt_clay', {'item': 'minecraft:clay_ball'}, {'aqua': 1, 'terra': 2},
                    'minecraft:clay_ball', 'ALCHEMICALDUPLICATION', 2)
    crucible_recipe('alt_glowstone', {'tag': 'forge:dusts/glowstone'}, {'lux': 3, 'sensus': 1},
                    'minecraft:glowstone_dust', 'ALCHEMICALDUPLICATION', 2)
    crucible_recipe('alt_ink', {'item': 'minecraft:ink_sac'}, {'aqua': 2, 'sensus': 2},
                    'minecraft:ink_sac', 'ALCHEMICALDUPLICATION', 2)
    crucible_recipe('alt_web', {'item': 'minecraft:string'}, {'vinculum': 2, 'pannus': 2},
                    'minecraft:cobweb', 'ALCHEMICALMANUFACTURE')
    crucible_recipe('alt_mossy_cobble', {'item': 'minecraft:cobblestone'}, {'herba': 2, 'praecantatio': 1},
                    'minecraft:mossy_cobblestone', 'ALCHEMICALMANUFACTURE')
    crucible_recipe('alt_ice', {'item': 'minecraft:snow_block'}, {'ordo': 1, 'gelum': 1},
                    'minecraft:ice', 'ALCHEMICALMANUFACTURE')
    crucible_recipe('alt_cracked_brick', {'item': 'minecraft:stone_bricks'}, {'perditio': 2},
                    'minecraft:cracked_stone_bricks', 'ENTROPICPROCESSING')
    crucible_recipe('alt_bonemeal', {'item': 'minecraft:bone'}, {'perditio': 1},
                    'minecraft:bone_meal', 'ENTROPICPROCESSING', 4)
    # В оригинале очищался блок руды. С 1.17 руда при добыче даёт сырой металл, поэтому годится и он
    # (решение владельца проекта: с Удачей выход выше, чем был в оригинале).
    for metal in ('iron', 'gold', 'copper'):
        crucible_recipe('pure_' + metal, [{'tag': 'forge:ores/' + metal}, {'tag': 'forge:raw_materials/' + metal}],
                        {'metallum': 1, 'ordo': 1}, tc('cluster_' + metal), 'PURE' + metal.upper())
    crucible_recipe('trans_iron', {'tag': 'forge:nuggets/iron'}, {'metallum': 2},
                    'minecraft:iron_nugget', 'TRANSIRON', 3)
    crucible_recipe('trans_gold', {'tag': 'forge:nuggets/gold'}, {'metallum': 2, 'lucrum': 1},
                    'minecraft:gold_nugget', 'TRANSGOLD', 3)


def flux():
    """Жижа — столбик по числу долей, газ — полупрозрачный куб; оба рисуются как полупрозрачные."""
    faces = ['down', 'up', 'north', 'south', 'west', 'east']
    variants = {}
    for level in range(8):
        height = 2 * (level + 1)
        write(ASSETS, 'models/block/flux_goo_%d.json' % level, {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
            'textures': {'all': tc('block/fluxgoo'), 'particle': tc('block/fluxgoo')},
            'elements': [{'from': [0, 0, 0], 'to': [16, height, 16], 'faces': {
                f: ({'texture': '#all', 'cullface': f} if f != 'up' or level == 7 else {'texture': '#all'})
                if f in ('up', 'down') else {'uv': [0, 16 - height, 16, 16], 'texture': '#all', 'cullface': f}
                for f in faces}}]})
        variants['level=%d' % level] = {'model': tc('block/flux_goo_%d' % level)}
    write(ASSETS, 'blockstates/flux_goo.json', {'variants': variants})
    write(ASSETS, 'models/block/flux_gas.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
        'textures': {'all': tc('block/fluxgas'), 'particle': tc('block/fluxgas')},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16],
                      'faces': {f: {'texture': '#all', 'cullface': f} for f in faces}}]})
    write(ASSETS, 'blockstates/flux_gas.json', {'variants': {'': {'model': tc('block/flux_gas')}}})
    write(ASSETS, 'models/item/flux_goo.json', {'parent': tc('block/flux_goo_7')})

    # --- жидкая смерть: тот же столбик долей, что у флюса ---
    variants = {}
    for level in range(8):
        height = 2 * (level + 1)
        write(ASSETS, 'models/block/liquid_death_%d.json' % level, {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
            'textures': {'all': tc('block/fluiddeath'), 'particle': tc('block/fluiddeath')},
            'elements': [{'from': [0, 0, 0], 'to': [16, height, 16], 'faces': {
                f: ({'texture': '#all', 'cullface': f} if f != 'up' or level == 7 else {'texture': '#all'})
                if f in ('up', 'down') else {'uv': [0, 16 - height, 16, 16], 'texture': '#all', 'cullface': f}
                for f in faces}}]})
        variants['level=%d' % level] = {'model': tc('block/liquid_death_%d' % level)}
    write(ASSETS, 'blockstates/liquid_death.json', {'variants': variants})
    simple_item('bucket_death', 'bucket_death')
    crucible_recipe('bucket_death', {'item': 'minecraft:bucket'}, {'mortuus': 32, 'venenum': 32, 'perditio': 32},
                    tc('bucket_death'), 'LIQUIDDEATH')
    write(DATA, 'thaumcraft/damage_type/dissolve.json',
          {'message_id': 'dissolve', 'scaling': 'when_caused_by_living_non_player', 'exhaustion': 0.1})
    tag('damage_type', 'minecraft:bypasses_armor', tc('dissolve'))

    # --- очищающая жидкость, соли и купальня ---
    write(ASSETS, 'models/block/purifying_fluid.json', {'textures': {'particle': tc('block/fluidpure')}})
    write(ASSETS, 'blockstates/purifying_fluid.json', {'variants': {'': {'model': tc('block/purifying_fluid')}}})
    tag('fluids', 'thaumcraft:purifying', tc('purifying_fluid'), tc('flowing_purifying_fluid'))
    simple_item('bucket_pure', 'bucket_pure')
    simple_item('bath_salts', 'bath_salts')
    crucible_recipe('bath_salts', {'item': tc('salis_mundus')}, {'cognitio': 6, 'auram': 6, 'ordo': 6, 'sano': 6},
                    tc('bath_salts'), 'BATHSALTS')
    write(ASSETS, 'models/block/arcane_spa.json', {
        'parent': 'minecraft:block/cube_bottom_top',
        'textures': {'bottom': tc('block/pedestal_top'), 'top': tc('block/spa_top'), 'side': tc('block/spa_side')}})
    write(ASSETS, 'blockstates/arcane_spa.json', {'variants': {'': {'model': tc('block/arcane_spa')}}})
    write(ASSETS, 'models/item/arcane_spa.json', {'parent': tc('block/arcane_spa')})
    drop_self('arcane_spa')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('arcane_spa'))
    write(ASSETS, 'models/item/flux_gas.json', {'parent': tc('block/flux_gas')})


def raw_ore_drop(name, item):
    """Шёлковое касание даёт блок; иначе сырьё, которое Удача умножает как у ванильных руд."""
    write(DATA, 'thaumcraft/loot_tables/blocks/%s.json' % name, {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc(name), 'conditions': [SILK_TOUCH]},
            {'type': 'minecraft:item', 'name': tc(item), 'functions': [
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                {'function': 'minecraft:explosion_decay'}]},
        ]}]}],
    })


def arcane(name, pattern, key, result, aspects, research, count=1):
    result_json = {'item': tc(result)}
    if count != 1:
        result_json['count'] = count
    write(DATA, 'thaumcraft/recipes/%s.json' % name, {
        'type': tc('arcane_shaped'), 'pattern': pattern, 'key': key, 'result': result_json,
        'aspects': aspects, 'research': research})


def essentia():
    faces = ['down', 'up', 'north', 'south', 'west', 'east']
    drop_self('arcane_stone')

    # --- алхимическая печь ---
    variants = {}
    for lit in ('false', 'true'):
        write(ASSETS, 'models/block/alchemical_furnace%s.json' % ('_on' if lit == 'true' else ''), {
            'parent': 'minecraft:block/orientable',
            'textures': {'top': tc('block/al_furnace_top'), 'side': tc('block/al_furnace_side'),
                         'front': tc('block/al_furnace_front_' + ('on' if lit == 'true' else 'off'))}})
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            variant = {'model': tc('block/alchemical_furnace' + ('_on' if lit == 'true' else ''))}
            if y:
                variant['y'] = y
            variants['facing=%s,lit=%s' % (facing, lit)] = variant
    write(ASSETS, 'blockstates/alchemical_furnace.json', {'variants': variants})
    write(ASSETS, 'models/item/alchemical_furnace.json', {'parent': tc('block/alchemical_furnace')})
    drop_self('alchemical_furnace')

    # --- перегонный куб: модель OBJ из оригинала (её готовит import_assets) ---
    write(ASSETS, 'models/block/alembic.json', {
        'loader': 'forge:obj', 'model': tc('models/block/alembic.obj'), 'flip_v': True,
        'render_type': 'minecraft:cutout', 'textures': {'particle': tc('block/alembic')},
        'display': {
            'gui': {'rotation': [30, 225, 0], 'scale': [0.7, 0.7, 0.7]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
    write(ASSETS, 'blockstates/alembic.json', {'variants': {'': {'model': tc('block/alembic')}}})
    write(ASSETS, 'models/item/alembic.json', {'parent': tc('block/alembic')})
    drop_self('alembic')

    # --- банка: стеклянный корпус 10x12x10 и горлышко, как в BlockJarRenderer ---
    def jar_box(x0, y0, x1, y1):
        box_faces = {f: {'texture': '#side'} for f in ('north', 'south', 'west', 'east')}
        box_faces['up'] = {'texture': '#top'}
        box_faces['down'] = {'texture': '#bottom'}
        return {'from': [x0, y0, x0], 'to': [x1, y1, x1], 'faces': box_faces}
    write(ASSETS, 'models/block/warded_jar.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
        'textures': {'particle': tc('block/jar_side'), 'side': tc('block/jar_side'), 'top': tc('block/jar_top'),
                     'bottom': tc('block/jar_bottom')},
        'elements': [jar_box(3, 0, 13, 12), jar_box(5, 12, 11, 14)]})
    write(ASSETS, 'blockstates/warded_jar.json', {'variants': {'': {'model': tc('block/warded_jar')}}})
    write(ASSETS, 'models/item/warded_jar.json', {'parent': tc('block/warded_jar')})
    # банка пустоты: та же банка с обсидиановой крышкой
    write(ASSETS, 'models/block/void_jar.json', {
        'parent': tc('block/warded_jar'),
        'textures': {'particle': tc('block/jar_side_void'), 'side': tc('block/jar_side_void'), 'top': tc('block/jar_top_void')}})
    write(ASSETS, 'blockstates/void_jar.json', {'variants': {'': {'model': tc('block/void_jar')}}})
    write(ASSETS, 'models/item/void_jar.json', {'parent': tc('block/void_jar')})
    # сломанная банка сохраняет эссенцию
    write(DATA, 'thaumcraft/loot_tables/blocks/warded_jar.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('warded_jar'), 'functions': [
            {'function': 'minecraft:copy_nbt', 'source': 'block_entity', 'ops': [
                {'source': 'Aspect', 'target': 'BlockEntityTag.Aspect', 'op': 'replace'},
                {'source': 'Amount', 'target': 'BlockEntityTag.Amount', 'op': 'replace'},
                {'source': 'Filter', 'target': 'BlockEntityTag.Filter', 'op': 'replace'},
                {'source': 'LabelSide', 'target': 'BlockEntityTag.LabelSide', 'op': 'replace'}]}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    void_loot = {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('void_jar'), 'functions': [
            {'function': 'minecraft:copy_nbt', 'source': 'block_entity', 'ops': [
                {'source': 'Aspect', 'target': 'BlockEntityTag.Aspect', 'op': 'replace'},
                {'source': 'Amount', 'target': 'BlockEntityTag.Amount', 'op': 'replace'},
                {'source': 'Filter', 'target': 'BlockEntityTag.Filter', 'op': 'replace'},
                {'source': 'LabelSide', 'target': 'BlockEntityTag.LabelSide', 'op': 'replace'}]}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
    write(DATA, 'thaumcraft/loot_tables/blocks/void_jar.json', void_loot)
    tag('blocks', 'minecraft:mineable/pickaxe', tc('arcane_stone'), tc('alchemical_furnace'), tc('alembic'))

    # --- предметы ---
    simple_item('filter', 'filter')
    write(ASSETS, 'models/item/phial.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('item/phial')},
        'overrides': [{'predicate': {tc('filled'): 1}, 'model': tc('item/phial_filled')}]})
    write(ASSETS, 'models/item/phial_filled.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': tc('item/phial'), 'layer1': tc('item/phialoverlay')}})
    tag('items', 'thaumcraft:shards', *[tc('shard_' + e) for e in ELEMENTS + ['balanced']])

    # --- рецепты оригинала ---
    write(DATA, 'thaumcraft/recipes/phial.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' C ', 'G G', ' G '],
        'key': {'G': {'item': 'minecraft:glass'}, 'C': {'item': 'minecraft:clay_ball'}},
        'result': {'item': tc('phial'), 'count': 8}})
    arcane('arcane_stone', ['SSS', 'SCS', 'SSS'], {'S': {'tag': 'forge:stone'}, 'C': {'tag': tc('shards')}},
           'arcane_stone', {'terra': 1, 'ignis': 1}, 'ARCANESTONE', 9)
    arcane('warded_jar', ['GWG', 'G G', 'GGG'], {'G': {'tag': 'forge:glass_panes'}, 'W': {'tag': 'minecraft:wooden_slabs'}},
           'warded_jar', {'aqua': 1}, 'DISTILESSENTIA')
    arcane('void_jar', ['O', 'J', 'P'], {'O': {'item': 'minecraft:obsidian'}, 'J': {'item': tc('warded_jar')},
                                         'P': {'item': 'minecraft:blaze_powder'}}, 'void_jar', {'aqua': 5, 'perditio': 15}, 'JARVOID')
    arcane('resonator', ['I I', 'INI', ' S '], {'I': {'tag': 'forge:ingots/iron'}, 'N': {'tag': 'forge:gems/quartz'},
                                                'S': {'tag': 'forge:rods/wooden'}}, 'resonator', {'aqua': 5, 'aer': 5}, 'TUBES')
    simple_item('resonator', 'resonator')

    # --- блок сала ---
    write(ASSETS, 'models/block/tallow_block.json', {
        'parent': 'minecraft:block/cube_column', 'textures': {'end': tc('block/tallowblock_top'), 'side': tc('block/tallowblock')}})
    write(ASSETS, 'blockstates/tallow_block.json', {'variants': {'': {'model': tc('block/tallow_block')}}})
    write(ASSETS, 'models/item/tallow_block.json', {'parent': tc('block/tallow_block')})
    drop_self('tallow_block')
    write(DATA, 'thaumcraft/recipes/tallow_block.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['KKK', 'KKK', 'KKK'],
        'key': {'K': {'item': tc('tallow')}}, 'result': {'item': tc('tallow_block')}})
    write(DATA, 'thaumcraft/recipes/tallow_from_block.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': tc('tallow_block')}],
        'result': {'item': tc('tallow'), 'count': 9}})

    # --- големы: предметы, ядра и улучшения ---
    for golem, catalyst, amount in (('straw', 'minecraft:hay_block', 4), ('wood', tc('greatwood_log'), 4), ('tallow', tc('tallow_block'), 8),
                                    ('clay', 'minecraft:bricks', 4), ('flesh', tc('flesh_block'), 8), ('stone', 'minecraft:stone_bricks', 4),
                                    ('iron', 'minecraft:iron_block', 4), ('thaumium', tc('thaumium_block'), 8)):
        name = 'golem_' + golem
        base = {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('item/' + name)}}
        overrides = []
        for adv, core in ((1, 0), (0, 1), (1, 1)):
            variant = name + ('_adv' if adv else '') + ('_core' if core else '')
            layers = {'layer0': tc('item/' + name)}
            if adv:
                layers['layer1'] = tc('item/golem_over_adv')
            if core:
                layers['layer%d' % (1 + adv)] = tc('item/golem_over_core')
            write(ASSETS, 'models/item/%s.json' % variant, {'parent': 'minecraft:item/generated', 'textures': layers})
            overrides.append({'predicate': {tc('advanced'): adv, tc('core'): core}, 'model': tc('item/' + variant)})
        base['overrides'] = overrides
        write(ASSETS, 'models/item/%s.json' % name, base)
        crucible_recipe(name, {'item': catalyst}, {'humanus': amount, 'motus': amount, 'spiritus': amount}, tc(name),
                        'GOLEM' + golem.upper())
    tag('items', 'thaumcraft:golems', *[tc('golem_' + g) for g in ('straw', 'wood', 'tallow', 'clay', 'flesh', 'stone', 'iron',
                                                                    'thaumium')])
    # продвинутый голем: любой голем остаётся собой и получает метку
    write(DATA, 'thaumcraft/recipes/infusion/advanced_golem.json', {
        'type': tc('infusion_nbt'), 'central': {'tag': tc('golems')},
        'components': [{'item': 'minecraft:redstone'}, {'item': 'minecraft:glowstone_dust'}, {'item': 'minecraft:gunpowder'},
                       {'item': tc('warded_jar')}, {'item': tc('zombie_brain')}],
        'aspects': {'cognitio': 8, 'sensus': 8, 'victus': 8}, 'instability': 3, 'nbt': '{advanced:1b}', 'research': 'ADVANCEDGOLEM'})
    blank = {'item': tc('golem_core_blank')}
    simple_item('golem_core_blank', 'golem_core_blank')
    simple_item('golem_bell', 'ironbell')

    # --- оковы голема: под сигналом верх загорается ---
    for power, top in (('false', 'golem_stone_top'), ('true', 'golem_stone_top_active')):
        write(ASSETS, 'models/block/golem_fetter_%s.json' % power, {
            'parent': 'minecraft:block/cube_bottom_top',
            'textures': {'bottom': tc('block/arcane_stone'), 'top': tc('block/' + top), 'side': tc('block/golem_stone_side')}})
    write(ASSETS, 'blockstates/golem_fetter.json', {'variants': {
        'powered=false': {'model': tc('block/golem_fetter_false')}, 'powered=true': {'model': tc('block/golem_fetter_true')}}})
    write(ASSETS, 'models/item/golem_fetter.json', {'parent': tc('block/golem_fetter_false')})
    drop_self('golem_fetter')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('golem_fetter'))
    arcane('golem_fetter', ['SSS', 'IRI', 'BBB'], {'S': {'item': tc('arcane_stone')}, 'I': {'tag': 'forge:ingots/iron'},
                                                   'B': {'item': tc('arcane_stone_bricks')}, 'R': {'item': 'minecraft:redstone_block'}},
           'golem_fetter', {'terra': 5, 'ordo': 5}, 'GOLEMFETTER')

    # --- голодный сундук: в мире его рисует рендерер, в руке — та же старая модель сундука коробками ---
    write(ASSETS, 'models/block/hungry_chest.json', {'textures': {'particle': tc('block/woodplain')}})
    write(ASSETS, 'blockstates/hungry_chest.json', {'variants': {'': {'model': tc('block/hungry_chest')}}})
    chest = box_model('chesthungry', 64, 64, [
        (0, 19, -7, 6, -7, 14, 10, 14),    # ящик
        (0, 0, -7, 2, -7, 14, 5, 14),      # крышка
        (0, 0, -1, 5, -8, 2, 4, 1),        # замок
    ])
    chest['parent'] = 'minecraft:block/block'
    write(ASSETS, 'models/item/hungry_chest.json', chest)
    drop_self('hungry_chest')
    tag('blocks', 'minecraft:mineable/axe', tc('hungry_chest'))
    arcane('hungry_chest', ['WTW', 'W W', 'WWW'], {'W': {'tag': 'minecraft:planks'}, 'T': {'tag': 'minecraft:wooden_trapdoors'}},
           'hungry_chest', {'aer': 5, 'ordo': 3, 'perditio': 3}, 'HUNGRYCHEST')

    # --- дорожный сундук: предмет выглядит как сам сундук ---
    trunk = box_model('trunk', 64, 64, [
        (0, 19, -7, 6, -7, 14, 10, 14),
        (0, 0, -7, 2, -7, 14, 5, 14),
        (0, 0, -1, 5, -8, 2, 4, 1),
    ])
    trunk['parent'] = 'minecraft:block/block'
    write(ASSETS, 'models/item/traveling_trunk.json', trunk)
    planks = {'item': tc('greatwood_planks')}
    infusion_recipe('traveling_trunk', {'item': tc('hungry_chest')},
                    [{'tag': 'forge:ingots/iron'}, planks, {'item': tc('golem_wood')}, planks],
                    {'motus': 4, 'spiritus': 4, 'iter': 4, 'vacuos': 16}, 3, 'traveling_trunk', 'TRAVELTRUNK')

    # --- украшения голема ---
    iron = {'tag': 'forge:ingots/iron'}
    black = {'item': 'minecraft:black_wool'}
    for deco, pattern, key, aspects, research in (
            ('tophat', [' C ', ' G ', 'CCC'], {'C': black, 'G': {'tag': 'forge:ingots/gold'}}, {'ordo': 8, 'ignis': 8}, 'TINYHAT'),
            ('fez', ['CCS', 'CCS', '  S'], {'C': {'item': 'minecraft:red_wool'}, 'S': {'item': 'minecraft:string'}},
             {'aqua': 4, 'terra': 4}, 'TINYFEZ'),
            ('bowtie', ['CSC', 'C C'], {'C': black, 'S': {'item': 'minecraft:string'}}, {'aer': 4, 'ordo': 4}, 'TINYBOWTIE'),
            ('glasses', ['GIG'], {'G': {'item': 'minecraft:glass'}, 'I': iron}, {'aer': 4, 'aqua': 4}, 'TINYGLASSES'),
            ('dart', ['AIA', 'ADA', 'AIA'], {'I': iron, 'D': {'item': 'minecraft:dispenser'}, 'A': {'item': 'minecraft:arrow'}},
             {'aer': 4, 'ignis': 4}, 'TINYDART'),
            ('visor', ['IHI'], {'I': iron, 'H': {'item': 'minecraft:iron_helmet'}}, {'terra': 4, 'aqua': 4}, 'TINYVISOR'),
            ('armor', ['I I', 'IAI'], {'I': iron, 'A': {'item': 'minecraft:iron_chestplate'}}, {'terra': 8}, 'TINYARMOR'),
            ('mace', ['III', 'III', ' I '], {'I': iron}, {'terra': 4, 'ignis': 4}, 'TINYHAMMER')):
        simple_item('golem_deco_' + deco, 'golemdeco' + deco)
        arcane('golem_deco_' + deco, pattern, key, 'golem_deco_' + deco, aspects, research)
    # ведро голема с ядром переливания: модель ставит рендерер голема
    write(ASSETS, 'models/block/golem_bucket.json', {
        'loader': 'forge:obj', 'model': tc('models/block/golem_bucket.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/bucket')}})
    arcane('golem_bell', [' QQ', ' QQ', 'S  '], {'Q': {'tag': 'forge:gems/quartz'}, 'S': {'tag': 'forge:rods/wooden'}},
           'golem_bell', {'ordo': 5}, 'GOLEMBELL')
    arcane('golem_core_blank', [' C ', 'CNC', ' C '], {'C': {'item': 'minecraft:brick'}, 'N': {'item': tc('nitor')}},
           'golem_core_blank', {'ordo': 5, 'ignis': 5}, 'COREGATHER')
    for core, texture in (('fill', 'fill'), ('empty', 'empty'), ('gather', 'gather'), ('harvest', 'harvest'), ('guard', 'guard'),
                          ('liquid', 'liquid'), ('alchemy', 'essentia'), ('lumber', 'lumber'), ('use', 'use'), ('butcher', 'butcher'),
                          ('sorting', 'sorting'), ('fishing', 'fish')):
        simple_item('golem_core_' + core, 'golem_core_' + texture)
    for core, catalyst, aspects in (('gather', blank, {'lucrum': 5, 'terra': 5}), ('fill', blank, {'fames': 5, 'vacuos': 5}),
                                    ('empty', blank, {'lucrum': 5, 'vacuos': 5}), ('harvest', blank, {'meto': 5, 'messis': 5}),
                                    ('guard', blank, {'telum': 5, 'vinculum': 5}),
                                    ('butcher', {'item': tc('golem_core_guard')}, {'corpus': 5, 'bestia': 5}),
                                    ('liquid', blank, {'aqua': 5, 'vacuos': 5})):
        crucible_recipe('golem_core_' + core, catalyst, aspects, tc('golem_core_' + core), 'CORE' + core.upper())
    potion = {'item': 'minecraft:potion'}
    infusion_recipe('golem_core_alchemy', {'item': tc('golem_core_liquid')}, [{'item': tc('warded_jar')}, potion, potion, potion],
                    {'praecantatio': 15, 'aqua': 15, 'motus': 15}, 2, 'golem_core_alchemy', 'COREALCHEMY')
    infusion_recipe('golem_core_sorting', {'item': tc('zombie_brain')},
                    [{'item': tc('golem_core_fill')}, {'item': 'minecraft:comparator'}, {'item': tc('golem_core_empty')},
                     {'item': 'minecraft:paper'}],
                    {'vacuos': 16, 'permutatio': 16, 'fames': 16, 'lucrum': 16}, 3, 'golem_core_sorting', 'CORESORTING')
    infusion_recipe('golem_core_fishing', {'item': tc('golem_core_harvest')},
                    [{'item': 'minecraft:fishing_rod'}, {'item': 'minecraft:cod'}, {'item': 'minecraft:pufferfish'},
                     {'item': 'minecraft:salmon'}],
                    {'aqua': 16, 'meto': 16, 'bestia': 16}, 3, 'golem_core_fishing', 'COREFISHING')
    infusion_recipe('golem_core_use', {'item': tc('golem_core_empty')},
                    [{'item': 'minecraft:comparator'}, {'item': 'minecraft:flint_and_steel'}, {'item': 'minecraft:shears'},
                     {'item': 'minecraft:lever'}],
                    {'instrumentum': 20, 'machina': 20, 'humanus': 20}, 3, 'golem_core_use', 'COREUSE')
    # --- чародейская лампа, янтарный блок и янтарный кирпич ---
    lamp_box = {'from': [4, 2, 4], 'to': [12, 14, 12], 'faces': dict(
        {side: {'uv': [4, 2, 12, 14], 'texture': '#side'} for side in ('north', 'south', 'east', 'west')},
        up={'uv': [4, 4, 12, 12], 'texture': '#top'}, down={'uv': [4, 4, 12, 12], 'texture': '#top'})}
    lamp_textures = {'side': tc('block/lamp_side'), 'top': tc('block/lamp_top'), 'particle': tc('block/lamp_side')}
    write(ASSETS, 'models/block/arcane_lamp.json', {'parent': 'minecraft:block/block', 'textures': lamp_textures,
                                                    'elements': [lamp_box]})
    # ножка, которой лампа держится за соседний блок (здесь — за блок снизу)
    write(ASSETS, 'models/block/arcane_lamp_mount.json', {'textures': lamp_textures, 'elements': [
        {'from': [6, 0, 6], 'to': [10, 3, 10], 'faces': dict(
            {side: {'uv': [6, 13, 10, 16], 'texture': '#side'} for side in ('north', 'south', 'east', 'west')},
            down={'uv': [6, 6, 10, 10], 'texture': '#side'})}]})
    mounts = {'down': {}, 'up': {'x': 180}, 'north': {'x': 90, 'y': 180}, 'south': {'x': 90}, 'west': {'x': 90, 'y': 90},
              'east': {'x': 90, 'y': 270}}
    write(ASSETS, 'blockstates/arcane_lamp.json', {'multipart': [{'apply': {'model': tc('block/arcane_lamp')}}] + [
        {'when': {'facing': facing}, 'apply': dict({'model': tc('block/arcane_lamp_mount')}, **turn)}
        for facing, turn in mounts.items()]})
    write(ASSETS, 'models/item/arcane_lamp.json', {'parent': tc('block/arcane_lamp')})
    # лампы роста и плодородия: без эссенции у них погасшие текстуры
    for name, short in (('lamp_growth', 'grow'), ('lamp_fertility', 'fert')):
        for lit, suffix in ((True, ''), (False, '_off')):
            write(ASSETS, 'models/block/%s%s.json' % (name, suffix), {'parent': tc('block/arcane_lamp'), 'textures': {
                'side': tc('block/lamp_%s_side%s' % (short, suffix)), 'top': tc('block/lamp_%s_top%s' % (short, suffix)),
                'particle': tc('block/lamp_%s_side%s' % (short, suffix))}})
        write(ASSETS, 'blockstates/%s.json' % name, {'multipart': [
            {'when': {'lit': 'true'}, 'apply': {'model': tc('block/' + name)}},
            {'when': {'lit': 'false'}, 'apply': {'model': tc('block/%s_off' % name)}}] + [
            {'when': {'facing': facing}, 'apply': dict({'model': tc('block/arcane_lamp_mount')}, **turn)}
            for facing, turn in mounts.items()]})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        drop_self(name)
        tag('blocks', 'minecraft:mineable/pickaxe', tc(name))
    gold = {'item': 'minecraft:gold_ingot'}
    infusion_recipe('lamp_growth', {'item': tc('arcane_lamp')},
                    [gold, {'item': 'minecraft:bone_meal'}, {'item': tc('shard_earth')}, gold, {'item': 'minecraft:bone_meal'},
                     {'item': tc('shard_earth')}], {'herba': 16, 'lux': 8, 'victus': 16}, 4, 'lamp_growth', 'LAMPGROWTH')
    infusion_recipe('lamp_fertility', {'item': tc('arcane_lamp')},
                    [gold, {'item': 'minecraft:wheat'}, {'item': tc('shard_fire')}, gold, {'item': 'minecraft:carrot'},
                     {'item': tc('shard_fire')}], {'bestia': 16, 'victus': 16, 'lux': 8}, 4, 'lamp_fertility', 'LAMPFERTILITY')
    # --- мостовые путника и защиты, решётка ---
    simple_block('travel_paving', 'paving_stone_travel')
    simple_block('warding_paving', 'paving_stone_warding')
    write(ASSETS, 'blockstates/ward_aura.json', {'variants': {'': {'model': 'minecraft:block/air'}}})
    bricks = {'item': tc('arcane_stone_bricks')}
    for name, shards, aspects, research in (('travel_paving', ('air', 'earth'), {'terra': 10, 'aer': 10}, 'PAVETRAVEL'),
                                            ('warding_paving', ('fire', 'order'), {'ignis': 10, 'ordo': 10}, 'PAVEWARD')):
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        drop_self(name)
        tag('blocks', 'minecraft:mineable/pickaxe', tc(name))
        arcane(name, ['SAS', 'SBS'], {'S': bricks, 'A': {'item': tc('shard_' + shards[0])}, 'B': {'item': tc('shard_' + shards[1])}},
               name, aspects, research, 4)
    grate_faces = {side: {'texture': '#grate'} for side in ('north', 'south', 'east', 'west', 'up', 'down')}
    grate_elements = [{'from': [0, 13, 0], 'to': [16, 16, 16], 'faces': grate_faces}]
    # прутья внутри решётки: в оригинале это грани самой плиты, сдвинутые внутрь блока
    for at in (3, 7, 9, 13):
        grate_elements.append({'from': [at, 13, 0], 'to': [at, 16, 16], 'faces': {
            'east': {'texture': '#grate'}, 'west': {'texture': '#grate'}}})
        grate_elements.append({'from': [0, 13, at], 'to': [16, 16, at], 'faces': {
            'north': {'texture': '#grate'}, 'south': {'texture': '#grate'}}})
    write(ASSETS, 'models/block/grate.json', {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
        'textures': {'grate': tc('block/grate'), 'particle': tc('block/grate')}, 'elements': grate_elements})
    write(ASSETS, 'models/block/grate_closed.json', {'parent': tc('block/grate'), 'textures': {'hatch': tc('block/grate_hatch')},
        'elements': grate_elements + [{'from': [1, 14, 1], 'to': [15, 15, 15], 'faces': {
            side: {'texture': '#hatch'} for side in ('north', 'south', 'east', 'west', 'up', 'down')}}]})
    write(ASSETS, 'blockstates/grate.json', {'variants': {'open=true': {'model': tc('block/grate')},
                                                        'open=false': {'model': tc('block/grate_closed')}}})
    write(ASSETS, 'models/item/grate.json', {'parent': tc('block/grate')})
    drop_self('grate')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('grate'))
    shaped('grate', ['#', 'T'], {'#': {'item': 'minecraft:iron_bars'}, 'T': {'tag': 'minecraft:wooden_trapdoors'}}, 'grate')
    # --- чародейское ухо: подставка, стойка и четыре раструба ---
    def ear_box(frm, to, top, side, bottom):
        return {'from': frm, 'to': to, 'faces': dict({s: {'texture': side} for s in ('north', 'south', 'east', 'west')},
                                                    up={'texture': top}, down={'texture': bottom})}
    ear = [ear_box([0, 0, 0], [16, 3, 16], '#top', '#side', '#bottom'), ear_box([4, 3, 4], [12, 16, 12], '#top', '#side', '#bottom')]
    for frm, to in (([4, 8, 1], [12, 16, 3]), ([5, 8, 3], [11, 15, 4]), ([1, 8, 4], [3, 16, 12]), ([3, 8, 5], [4, 15, 11]),
                    ([4, 8, 13], [12, 16, 15]), ([5, 8, 12], [11, 15, 13]), ([13, 8, 4], [15, 16, 12]), ([12, 8, 5], [13, 15, 11])):
        ear.append(ear_box(frm, to, '#bell_top', '#bell_side', '#bell_top'))
    for state in ('on', 'off'):
        write(ASSETS, 'models/block/arcane_ear_%s.json' % state, {'parent': 'minecraft:block/block', 'textures': {
            'top': tc('block/arcaneeartop' + state), 'side': tc('block/arcaneearside' + state), 'bottom': tc('block/arcaneearbottom'),
            'bell_top': tc('block/arcaneearbelltop'), 'bell_side': tc('block/arcaneearbellside'),
            'particle': tc('block/arcaneearside' + state)}, 'elements': ear})
    write(ASSETS, 'blockstates/arcane_ear.json', {'variants': {'powered=true': {'model': tc('block/arcane_ear_on')},
                                                             'powered=false': {'model': tc('block/arcane_ear_off')}}})
    write(ASSETS, 'models/item/arcane_ear.json', {'parent': tc('block/arcane_ear_off')})
    # --- левитатор: сквозь прорези в корпусе видно сияние, у работающего — в полную силу ---
    for state, light in (('', 0), ('_active', 15)):
        glow = {'texture': '#glow', 'forge_data': {'color': 'FFDD11FF', 'block_light': light, 'sky_light': light}}
        write(ASSETS, 'models/block/levitator%s.json' % state, {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'top': tc('block/liftertop'), 'side': tc('block/lifterside'), 'bottom': tc('block/arcaneearbottom'),
                         'glow': tc('block/animatedglow'), 'particle': tc('block/lifterside')},
            'elements': [{'from': [0.16, 0.16, 0.16], 'to': [15.84, 15.84, 15.84], 'shade': False, 'faces': dict(
                {s: glow for s in ('north', 'south', 'east', 'west')},
                up=dict(glow, forge_data=dict(glow['forge_data'], color='FF00A000')))},
                {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': dict(
                    {s: {'texture': '#side', 'cullface': s} for s in ('north', 'south', 'east', 'west')},
                    up={'texture': '#top', 'cullface': 'up'}, down={'texture': '#bottom', 'cullface': 'down'})}]})
    write(ASSETS, 'blockstates/levitator.json', {'variants': {'active=true': {'model': tc('block/levitator_active')},
                                                            'active=false': {'model': tc('block/levitator')}}})
    write(ASSETS, 'models/item/levitator.json', {'parent': tc('block/levitator_active')})
    planks = {'item': tc('greatwood_planks')}
    for name in ('arcane_ear', 'levitator'):
        drop_self(name)
        tag('blocks', 'minecraft:mineable/axe', tc(name))
    arcane('arcane_ear', ['GIG', 'GBG', 'WRW'], {'G': {'item': 'minecraft:gold_ingot'}, 'I': {'item': 'minecraft:iron_ingot'},
                                                'B': {'item': tc('zombie_brain')}, 'W': planks, 'R': {'item': 'minecraft:redstone'}},
           'arcane_ear', {'aer': 10, 'ordo': 10}, 'ARCANEEAR')
    arcane('levitator', ['WEW', 'BNB', 'WAW'], {'W': planks, 'E': {'item': tc('shard_earth')}, 'A': {'item': tc('shard_air')},
                                               'N': {'item': tc('nitor')}, 'B': {'item': 'minecraft:iron_ingot'}},
           'levitator', {'aer': 10, 'terra': 5}, 'LEVITATOR')
    # --- зеркала ---
    simple_item('mirrored_glass', 'mirrorglass')
    write(DATA, 'thaumcraft/recipes/mirrored_glass.json', {
        'type': tc('arcane_shapeless'), 'ingredients': [{'item': tc('quicksilver')}, {'item': 'minecraft:glass_pane'}],
        'result': {'item': tc('mirrored_glass')}, 'aspects': {'ignis': 10, 'terra': 10}, 'research': 'BASICARTIFACE'})
    for name, frame in (('magic_mirror', 'mirrorframe'), ('essentia_mirror', 'mirrorframe2')):
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        write(ASSETS, 'models/block/%s.json' % name, {'textures': {'particle': tc('block/' + frame)}})
        write(ASSETS, 'models/item/%s_linked.json' % name, {'parent': 'minecraft:item/generated', 'textures': {
            'layer0': tc('block/' + frame), 'layer1': tc('block/mirrorpaneopen')}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': 'minecraft:item/generated', 'textures': {
            'layer0': tc('block/' + frame), 'layer1': tc('block/mirrorpane')},
            'overrides': [{'predicate': {tc('linked'): 1}, 'model': tc('item/%s_linked' % name)}]})
    write(ASSETS, 'models/item/mirror_frame.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('block/mirrorframe')}})
    write(ASSETS, 'models/item/mirror_frame_essentia.json', {'parent': 'minecraft:item/generated',
                                                            'textures': {'layer0': tc('block/mirrorframe2')}})
    simple_item('hand_mirror', 'mirrorhand')
    ender = {'item': 'minecraft:ender_pearl'}
    glass = {'item': tc('mirrored_glass')}
    gold_ingot = {'item': 'minecraft:gold_ingot'}
    iron_ingot = {'item': 'minecraft:iron_ingot'}
    infusion_recipe('magic_mirror', glass, [gold_ingot, gold_ingot, gold_ingot, ender],
                    {'iter': 8, 'tenebrae': 8, 'permutatio': 8}, 1, 'magic_mirror', 'MIRROR')
    infusion_recipe('hand_mirror', {'item': tc('magic_mirror')},
                    [{'item': 'minecraft:stick'}, {'item': 'minecraft:compass'}, {'item': 'minecraft:filled_map'}],
                    {'instrumentum': 16, 'iter': 16}, 5, 'hand_mirror', 'MIRRORHAND')
    infusion_recipe('essentia_mirror', glass, [iron_ingot, iron_ingot, iron_ingot, ender],
                    {'iter': 8, 'aqua': 8, 'permutatio': 8}, 2, 'essentia_mirror', 'MIRRORESSENTIA')
    # --- мозг в банке: банка та же, мозг и рассол рисует BrainJarRenderer ---
    write(ASSETS, 'blockstates/brain_jar.json', {'variants': {'': {'model': tc('block/warded_jar')}}})
    write(ASSETS, 'models/item/brain_jar.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'display': {
        'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
    drop_self('brain_jar')
    infusion_recipe('brain_jar', {'item': tc('warded_jar')},
                    [{'item': tc('zombie_brain')}, {'item': 'minecraft:spider_eye'}, {'item': 'minecraft:water_bucket'},
                     {'item': 'minecraft:spider_eye'}], {'cognitio': 10, 'sensus': 10, 'exanimis': 20}, 4, 'brain_jar', 'JARBRAIN')
    # --- защищённая магия ---
    door_models = {}
    for half, part in (('lower', 'bottom'), ('upper', 'top')):
        for hinge in ('left', 'right'):
            for opened in ('', '_open'):
                name = 'arcane_door_%s_%s%s' % (part, hinge, opened)
                write(ASSETS, 'models/block/%s.json' % name, {'parent': 'minecraft:block/door_%s_%s%s' % (part, hinge, opened),
                      'render_type': 'minecraft:cutout',
                      'textures': {'bottom': tc('block/adoorbot'), 'top': tc('block/adoortop')}})
                door_models[(half, hinge, opened)] = name
    door_variants = {}
    for facing, base in (('east', 0), ('south', 90), ('west', 180), ('north', 270)):
        for half in ('lower', 'upper'):
            for hinge in ('left', 'right'):
                for is_open in ('false', 'true'):
                    turn = (base + ((90 if hinge == 'left' else 270) if is_open == 'true' else 0)) % 360
                    variant = {'model': tc('block/' + door_models[(half, hinge, '_open' if is_open == 'true' else '')])}
                    if turn:
                        variant['y'] = turn
                    door_variants['facing=%s,half=%s,hinge=%s,open=%s' % (facing, half, hinge, is_open)] = variant
    write(ASSETS, 'blockstates/arcane_door.json', {'variants': door_variants})
    simple_item('arcane_door', 'arcanedoor')
    write(DATA, 'thaumcraft/loot_tables/blocks/arcane_door.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1,
        'entries': [{'type': 'minecraft:item', 'name': tc('arcane_door'), 'conditions': [{'condition': 'minecraft:block_state_property',
                     'block': tc('arcane_door'), 'properties': {'half': 'lower'}}]}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('arcane_door'))
    plate_variants = {}
    for setting in range(3):
        for pressed, parent in (('false', 'pressure_plate_up'), ('true', 'pressure_plate_down')):
            name = 'arcane_pressure_plate_%d%s' % (setting, '_down' if pressed == 'true' else '')
            write(ASSETS, 'models/block/%s.json' % name, {'parent': 'minecraft:block/' + parent,
                                                         'textures': {'texture': tc('block/applate%d' % (setting + 1))}})
            plate_variants['powered=%s,setting=%d' % (pressed, setting)] = {'model': tc('block/' + name)}
    write(ASSETS, 'blockstates/arcane_pressure_plate.json', {'variants': plate_variants})
    write(ASSETS, 'models/item/arcane_pressure_plate.json', {'parent': tc('block/arcane_pressure_plate_0')})
    drop_self('arcane_pressure_plate')
    tag('blocks', 'minecraft:mineable/axe', tc('arcane_pressure_plate'))
    write(ASSETS, 'blockstates/warded_glass.json', {'variants': {'': {'model': tc('block/warded_glass')}}})
    write(ASSETS, 'models/block/warded_glass.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                                    'textures': {'all': tc('block/warded_glass_1')}})
    write(ASSETS, 'models/item/warded_glass.json', {'parent': tc('block/warded_glass')})
    drop_self('warded_glass')
    # все 47 рамок защищённого стекла должны попасть в атлас блоков, хотя модель-заготовка ссылается на одну
    write(os.path.join(os.path.dirname(ASSETS), 'minecraft'),
          'atlases/blocks.json', {'sources': [{'type': 'single', 'resource': tc('block/warded_glass_%d' % i)} for i in range(1, 48)]})
    simple_item('key_iron', 'keyiron')
    simple_item('key_gold', 'keygold')
    planks_gw = {'item': tc('greatwood_planks')}
    brain = {'item': tc('zombie_brain')}
    thaumium = {'tag': 'forge:ingots/thaumium'}
    arcane('arcane_door', ['TDT', 'DBD', 'TDT'], {'T': thaumium, 'B': brain, 'D': planks_gw}, 'arcane_door',
           {'aqua': 20, 'ordo': 10, 'terra': 10, 'ignis': 5}, 'WARDEDARCANA')
    arcane('warded_glass', ['GGG', 'WBW', 'GGG'], {'G': {'item': 'minecraft:glass'}, 'B': brain, 'W': planks_gw}, 'warded_glass',
           {'aqua': 5, 'ordo': 10, 'terra': 5, 'ignis': 5}, 'WARDEDARCANA', 8)
    arcane('key_iron', ['NNI', 'N  '], {'I': {'item': 'minecraft:iron_ingot'}, 'N': {'tag': 'forge:nuggets/iron'}}, 'key_iron',
           {'aqua': 5, 'ordo': 5}, 'WARDEDARCANA', 2)
    arcane('key_gold', ['NNI', 'N  '], {'I': {'item': 'minecraft:gold_ingot'}, 'N': {'item': 'minecraft:gold_nugget'}}, 'key_gold',
           {'aqua': 5, 'ordo': 5}, 'WARDEDARCANA', 2)
    arcane('arcane_pressure_plate', [' B ', 'TDT'], {'T': {'item': tc('thaumium_ingot')}, 'B': brain, 'D': planks_gw},
           'arcane_pressure_plate', {'aqua': 20, 'ordo': 10, 'ignis': 5, 'terra': 10}, 'WARDEDARCANA')
    # --- костяной лук и первозданные стрелы ---
    write(ASSETS, 'models/item/bone_bow.json', {'parent': 'minecraft:item/bow', 'textures': {'layer0': tc('item/bonebow')},
        'overrides': [{'predicate': {'pulling': 1}, 'model': tc('item/bone_bow_pulling_0')},
                      {'predicate': {'pulling': 1, 'pull': 0.4}, 'model': tc('item/bone_bow_pulling_1')},
                      {'predicate': {'pulling': 1, 'pull': 0.65}, 'model': tc('item/bone_bow_pulling_2')}]})
    for stage in range(3):
        write(ASSETS, 'models/item/bone_bow_pulling_%d.json' % stage, {'parent': tc('item/bone_bow'),
              'textures': {'layer0': tc('item/bonebow_pulling_%d' % stage)}})
    tag('items', 'minecraft:bows', tc('bone_bow'))
    arcane('bone_bow', ['SB ', 'SEB', 'SB '], {'E': {'item': tc('shard_entropy')}, 'B': {'item': 'minecraft:bone'},
                                            'S': {'item': 'minecraft:string'}}, 'bone_bow', {'aer': 16, 'perditio': 32}, 'BONEBOW')
    for shard, aspect in (('air', 'aer'), ('fire', 'ignis'), ('water', 'aqua'), ('earth', 'terra'), ('order', 'ordo'),
                          ('entropy', 'perditio')):
        simple_item('primal_arrow_' + shard, 'el_arrow_' + shard)
        tag('items', 'minecraft:arrows', tc('primal_arrow_' + shard))
        arcane('primal_arrow_' + shard, ['AAA', 'ASA', 'AAA'], {'A': {'item': 'minecraft:arrow'}, 'S': {'item': tc('shard_' + shard)}},
               'primal_arrow_' + shard, {aspect: 8}, 'PRIMALARROW', 8)
    for name, tags in (('airarrow', ['bypasses_armor', 'is_projectile', 'witch_resistant_to']),
                       ('firearrow', ['is_fire', 'is_projectile']),
                       ('orderarrow', ['bypasses_armor', 'is_projectile', 'witch_resistant_to'])):
        write(DATA, 'thaumcraft/damage_type/%s.json' % name,
              {'message_id': name, 'scaling': 'when_caused_by_living_non_player', 'exhaustion': 0.1})
        for damage_tag in tags:
            tag('damage_type', 'minecraft:' + damage_tag, tc(name))
    # --- крепостные доспехи, очки и маски ---
    fa_thaumium = {'item': tc('thaumium_ingot')}
    fa_gold = {'item': 'minecraft:gold_ingot'}
    fa_leather = {'item': 'minecraft:leather'}
    for piece, texture, central, extra, armor in (
            ('helmet', 'thaumiumfortresshelm', 'thaumium_helmet', [fa_thaumium, fa_thaumium, fa_gold, fa_gold,
                                                                 {'item': 'minecraft:emerald'}], 16),
            ('chestplate', 'thaumiumfortresschest', 'thaumium_chestplate', [fa_thaumium] * 4 + [fa_gold, fa_leather], 24),
            ('leggings', 'thaumiumfortresslegs', 'thaumium_leggings', [fa_thaumium] * 3 + [fa_gold, fa_leather], 20)):
        simple_item('fortress_' + piece, texture)
        tag('items', 'minecraft:trimmable_armor', tc('fortress_' + piece))
        infusion_recipe('fortress_' + piece, {'item': tc(central)}, extra, {'metallum': 24, 'tutamen': armor, 'praecantatio': 16}, 3,
                        'fortress_' + piece, 'ARMORFORTRESS')
    fa_helm = {'item': tc('fortress_helmet')}
    fa_iron = {'item': 'minecraft:iron_ingot'}
    for name, components, aspects, nbt, research in (
            ('fortress_goggles', [{'item': 'minecraft:slime_ball'}, {'item': tc('goggles')}],
             {'sensus': 32, 'auram': 16, 'tutamen': 16}, '{goggles:1b}', 'HELMGOGGLES'),
            ('mask_grinning_devil', [{'item': 'minecraft:ink_sac'}, fa_iron, fa_leather, {'item': tc('shimmerleaf')},
                                     {'item': tc('zombie_brain')}, fa_iron],
             {'cognitio': 64, 'sano': 64, 'tutamen': 16}, '{mask:0}', 'MASKGRINNINGDEVIL'),
            ('mask_angry_ghost', [{'item': 'minecraft:bone_meal'}, fa_iron, fa_leather, {'item': 'minecraft:poisonous_potato'},
                                  {'item': 'minecraft:wither_skeleton_skull'}, fa_iron],
             {'perditio': 64, 'mortuus': 64, 'tutamen': 16}, '{mask:1}', 'MASKANGRYGHOST'),
            ('mask_sipping_fiend', [{'item': 'minecraft:red_dye'}, fa_iron, fa_leather, {'item': 'minecraft:ghast_tear'},
                                    {'item': 'minecraft:milk_bucket'}, fa_iron],
             {'exanimis': 64, 'victus': 64, 'tutamen': 16}, '{mask:2}', 'MASKSIPPINGFIEND')):
        write(DATA, 'thaumcraft/recipes/infusion/%s.json' % name, {
            'type': tc('infusion_nbt'), 'central': fa_helm, 'components': components, 'aspects': aspects,
            'instability': 5 if name == 'fortress_goggles' else 8, 'nbt': nbt, 'research': research})
    # --- мантии из зачарованной ткани ---
    fabric = {'item': tc('enchanted_fabric')}
    for piece, texture, pattern, aspects in (('chestplate', 'clothchest', ['I I', 'III', 'III'], {'aer': 5}),
                                             ('leggings', 'clothlegs', ['III', 'I I', 'I I'], {'aqua': 5}),
                                             ('boots', 'clothboots', ['I I', 'I I'], {'terra': 3})):
        write(ASSETS, 'models/item/robe_%s.json' % piece, {'parent': 'minecraft:item/generated', 'textures': {
            'layer0': tc('item/' + texture), 'layer1': tc('item/%sover' % texture)}})
        tag('items', 'minecraft:dyeable', tc('robe_' + piece))
        arcane('robe_' + piece, pattern, {'I': fabric}, 'robe_' + piece, aspects, 'ENCHFABRIC')
    # --- сапоги странника, летательная упряжь и пояс парения ---
    simple_item('traveller_boots', 'bootstraveler')
    simple_item('hover_harness', 'hoverharness')
    simple_item('hover_girdle', 'hovergirdle')
    tag('items', 'curios:belt', tc('hover_girdle'))
    write(ASSETS, 'models/block/hover_harness_back.json', {
        'loader': 'forge:obj', 'model': tc('models/block/hover_harness_back.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/hoverharness2')}})
    air_shard = {'item': tc('shard_air')}
    infusion_recipe('traveller_boots', {'item': 'minecraft:leather_boots'},
                    [air_shard, air_shard, {'item': tc('enchanted_fabric')}, {'item': tc('enchanted_fabric')},
                     {'item': 'minecraft:feather'}, {'tag': 'minecraft:fishes'}], {'volatus': 25, 'iter': 25}, 1,
                    'traveller_boots', 'BOOTSTRAVELLER')
    gw = {'item': tc('greatwood_planks')}
    infusion_recipe('hover_harness', {'item': 'minecraft:leather_chestplate'},
                    [air_shard, air_shard, gw, gw, {'item': 'minecraft:comparator'}, {'item': 'minecraft:gold_ingot'},
                     {'item': 'minecraft:gold_ingot'}, {'item': 'minecraft:iron_ingot'}, {'item': 'minecraft:iron_ingot'}],
                    {'volatus': 32, 'potentia': 32, 'machina': 32, 'iter': 16}, 6, 'hover_harness', 'HOVERHARNESS')
    infusion_recipe('hover_girdle', {'item': tc('bauble_belt')},
                    [air_shard, {'item': 'minecraft:feather'}, {'item': 'minecraft:gold_ingot'}, {'item': tc('shard_earth')},
                     {'item': 'minecraft:feather'}, {'item': 'minecraft:gold_ingot'}],
                    {'volatus': 16, 'potentia': 32, 'aer': 32, 'iter': 16}, 8, 'hover_girdle', 'HOVERGIRDLE')
    # --- зловещий камень ---
    simple_item('sinister_stone_active', 'sinister_stone_active')
    write(ASSETS, 'models/item/sinister_stone.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('item/sinister_stone')},
        'overrides': [{'predicate': {tc('active'): 1}, 'model': tc('item/sinister_stone_active')}]})
    infusion_recipe('sinister_stone', {'item': 'minecraft:flint'},
                    [{'item': tc('nitor')}, {'item': tc('shard_order')}, {'item': tc('knowledge_fragment')}, {'item': tc('shard_entropy')}],
                    {'sensus': 8, 'tenebrae': 8, 'alienis': 8, 'auram': 8}, 5, 'sinister_stone', 'SINSTONE')
    # --- очиститель скверны ---
    for part in ('cap', 'tip'):
        write(ASSETS, 'models/block/flux_scrubber_%s.json' % part, {
            'loader': 'forge:obj', 'model': tc('models/block/flux_scrubber_%s.obj' % part), 'flip_v': True,
            'textures': {'particle': tc('block/fluxscrubber')}})
    write(ASSETS, 'blockstates/flux_scrubber.json', {'variants': {'': {'model': tc('block/flux_scrubber_particle')}}})
    write(ASSETS, 'models/block/flux_scrubber_particle.json', {'textures': {'particle': tc('block/fluxscrubber')}})
    write(ASSETS, 'models/item/flux_scrubber.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'display': {
        'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
    drop_self('flux_scrubber')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('flux_scrubber'))
    arcane('flux_scrubber', [' B ', 'GOG', 'STS'], {'B': {'item': tc('bellows')}, 'G': {'item': 'minecraft:iron_bars'},
                                                   'O': {'item': tc('filter')}, 'T': {'item': tc('tube')},
                                                   'S': {'item': tc('arcane_stone_bricks')}},
           'flux_scrubber', {'aqua': 16, 'ordo': 16, 'aer': 8}, 'FLUXSCRUB')
    # --- знамёна ---
    write(ASSETS, 'blockstates/thaum_banner.json', {'variants': {'': {'model': tc('block/thaum_banner')}}})
    write(ASSETS, 'models/block/thaum_banner.json', {'textures': {'particle': 'minecraft:block/white_wool'}})
    write(ASSETS, 'models/item/thaum_banner.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'front', 'display': {
        'gui': {'rotation': [0, 0, 0], 'translation': [0, -3, 0], 'scale': [0.4, 0.4, 0.4]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'translation': [0, -3, 0], 'scale': [0.4, 0.4, 0.4]},
        'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 0, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, -2, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, -2, 0], 'scale': [0.4, 0.4, 0.4]}}})
    tag('blocks', 'minecraft:mineable/axe', tc('thaum_banner'))
    for index, dye in enumerate(('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
                                 'purple', 'blue', 'brown', 'green', 'red', 'black')):
        write(DATA, 'thaumcraft/recipes/thaum_banner_%s.json' % dye, {
            'type': tc('arcane_shaped'), 'pattern': ['WS', 'WS', 'WB'],
            'key': {'W': {'item': 'minecraft:%s_wool' % dye}, 'S': {'tag': 'forge:rods/wooden'}, 'B': {'tag': 'minecraft:wooden_slabs'}},
            'result': {'item': tc('thaum_banner'), 'nbt': '{color:%db}' % index},
            'aspects': {'aqua': 5, 'terra': 5}, 'research': 'BANNERS'})
    # --- чародейский бур ---
    for name in ('arcane_bore', 'arcane_bore_base'):
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/%s' % name)}}})
        write(ASSETS, 'models/block/%s.json' % name, {'textures': {'particle': tc('block/planks_greatwood')}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'display': {
            'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
        drop_self(name)
        tag('blocks', 'minecraft:mineable/axe', tc(name))
    gwp = {'item': tc('greatwood_planks')}
    arcane('arcane_bore_base', ['WIW', 'IDI', 'WIW'], {'W': gwp, 'I': {'item': 'minecraft:iron_ingot'}, 'D': {'item': 'minecraft:dispenser'}},
           'arcane_bore_base', {'aer': 10, 'ordo': 10}, 'ARCANEBORE')
    infusion_recipe('arcane_bore', {'item': 'minecraft:piston'},
                    [gwp, gwp, {'item': 'minecraft:gold_ingot'}, {'item': 'minecraft:gold_ingot'}, {'item': 'minecraft:diamond_pickaxe'},
                     {'item': 'minecraft:diamond_shovel'}, {'item': tc('shard_air')}, {'item': tc('shard_earth')}],
                    {'potentia': 16, 'perfodio': 32, 'machina': 32, 'vacuos': 16, 'motus': 16}, 4, 'arcane_bore', 'ARCANEBORE')
    # адская печь: у каждого места куба свои плитки, грань с решёткой — свои (BlockArcaneFurnace.calculateTexture)
    fronts = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    furnace_variants = {}
    for fr in fronts:
        bars_pos = {'north': 10, 'south': 16, 'west': 12, 'east': 14}[fr]
        for p in range(27):
            key = 'facing=%s,pos=%d' % (fr, p)
            if p == 13:
                furnace_variants[key] = {'model': tc('block/infernal_furnace/core')}
                continue
            if p == bars_pos:
                furnace_variants[key] = {'model': tc('block/infernal_furnace/bars'), 'y': fronts[fr]}
                continue
            fx, fz, fy = p % 3, p // 3 % 3, p // 9
            level = {2: 0, 1: 9, 0: 18}[fy]
            plain = 7 if level != 9 else 6
            in_front = {'north': fz == 0, 'south': fz == 2, 'west': fx == 0, 'east': fx == 2}[fr]

            def side(face, edge, col):
                if not edge:
                    return plain
                return col + level + (3 if in_front and face == fr else 0)
            tiles = {'north': side('north', fz == 0, 2 - fx), 'south': side('south', fz == 2, fx),
                     'west': side('west', fx == 0, fz), 'east': side('east', fx == 2, 2 - fz),
                     'up': fx + 9 * fz, 'down': fx + 9 * fz}
            textures = {face: tc('block/furnace%d' % t) for face, t in tiles.items()}
            textures['particle'] = textures['north']
            model = 'block/infernal_furnace/%d_%s' % (p, fr)
            write(ASSETS, 'models/%s.json' % model, {'parent': 'minecraft:block/cube', 'textures': textures})
            furnace_variants[key] = {'model': tc(model)}
    write(ASSETS, 'blockstates/infernal_furnace.json', {'variants': furnace_variants})
    write(ASSETS, 'models/block/infernal_furnace/core.json', {'parent': 'minecraft:block/cube_all',
                                                             'textures': {'all': 'minecraft:block/lava_still'}})
    # решётка утоплена на 10 пикселей, за ней тёмная задняя стенка и пламя в полтора блока высотой
    write(ASSETS, 'models/block/infernal_furnace/bars.json', {
        'render_type': 'minecraft:cutout', 'ambientocclusion': False,
        'textures': {'grill': tc('block/furnace13'), 'back': tc('block/furnace15'), 'fire': 'minecraft:block/fire_0',
                     'particle': 'minecraft:block/iron_bars'},
        'elements': [
            {'from': [0, 0, 14.4], 'to': [16, 24, 14.4], 'shade': False,
             'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#fire'}}},
            {'from': [0, 0, 12.8], 'to': [16, 16, 12.8], 'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#back'}}},
            {'from': [0, 0, 10], 'to': [16, 16, 10], 'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#grill'}}},
        ]})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('infernal_furnace'))
    for meat in ('chicken', 'beef', 'pork', 'fish'):
        simple_item('nugget_' + meat, 'nugget' + meat)
    simple_item('triple_meat_treat', 'tripletreat')
    sweets = {'chicken': {'item': tc('nugget_chicken')}, 'beef': {'item': tc('nugget_beef')},
              'pork': {'item': tc('nugget_pork')}, 'fish': {'item': tc('nugget_fish')}}
    for i, trio in enumerate((('beef', 'chicken', 'pork'), ('beef', 'chicken', 'fish'), ('beef', 'fish', 'pork'),
                              ('fish', 'chicken', 'pork'))):
        write(DATA, 'thaumcraft/recipes/triple_meat_treat_%d.json' % (i + 1), {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': [{'item': 'minecraft:sugar'}] + [sweets[m] for m in trio],
            'result': {'item': tc('triple_meat_treat')}})
    # обсидиановая плитка, тотемы: у верхнего блока столба на гранях лики, у нижних — затенённое основание
    simple_block('obsidian_tile', 'obsidiantile')
    drop_self('obsidian_tile')
    write(DATA, 'thaumcraft/recipes/obsidian_tile.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['##', '##'],
        'key': {'#': {'item': 'minecraft:obsidian'}}, 'result': {'item': tc('obsidian_tile'), 'count': 4}})

    def totem_model(name, sides):
        textures = {'up': tc('block/obsidiantile'), 'down': tc('block/obsidiantile'), 'particle': tc('block/obsidiantile')}
        textures.update(sides)
        write(ASSETS, 'models/block/%s.json' % name, {'parent': 'minecraft:block/cube', 'textures': textures})
    all_sides = ('north', 'south', 'west', 'east')
    totem_model('obsidian_totem_base', {f: tc('block/obsidiantotembase') for f in all_sides})
    totem_model('obsidian_totem_shaded', {f: tc('block/obsidiantotembaseshaded') for f in all_sides})
    for k in range(4):
        totem_model('obsidian_totem_carved_%d' % k, {f: tc('block/obsidiantotem%d' % ((i + k) % 4 + 1)) for i, f in enumerate(all_sides)})
    for totem in ('obsidian_totem', 'charged_obsidian_totem'):
        variants = {}
        for up in ('false', 'true'):
            for down in ('false', 'true'):
                if up == 'true':
                    model = {'model': tc('block/obsidian_totem_shaded')}
                elif down == 'true':
                    model = [{'model': tc('block/obsidian_totem_carved_%d' % k)} for k in range(4)]
                else:
                    model = {'model': tc('block/obsidian_totem_base')}
                variants['down=%s,up=%s' % (down, up)] = model
        write(ASSETS, 'blockstates/%s.json' % totem, {'variants': variants})
        write(ASSETS, 'models/item/%s.json' % totem, {'parent': tc('block/obsidian_totem_base')})
        tag('blocks', 'minecraft:mineable/pickaxe', tc(totem))
    tag('blocks', 'minecraft:mineable/pickaxe', tc('obsidian_tile'))
    drop_self('obsidian_totem')
    write(DATA, 'thaumcraft/loot_tables/blocks/charged_obsidian_totem.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('obsidian_tile')}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    for beacon in ('obsidian_tile', 'obsidian_totem', 'charged_obsidian_totem', 'tallow_block', 'arcane_stone',
                   'arcane_stone_bricks', 'golem_fetter', 'travel_paving', 'warding_paving'):
        tag('blocks', 'minecraft:beacon_base_blocks', tc(beacon))
    # урны и ящики: у каждой редкости свой рисунок на боках
    for tier, rarity in enumerate(('common', 'uncommon', 'rare')):
        urn, crate = 'loot_urn_' + rarity, 'loot_crate_' + rarity
        side, top = tc('block/urn_side_%d' % tier), tc('block/urn_top')
        faces = {f: {'texture': '#side'} for f in all_sides}
        faces.update({'up': {'texture': '#top'}, 'down': {'texture': '#top'}})
        write(ASSETS, 'models/block/%s.json' % urn, {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'side': side, 'top': top, 'particle': side},
            'elements': [{'from': [3, 0, 3], 'to': [13, 1, 13], 'faces': faces},
                         {'from': [2, 1, 2], 'to': [14, 13, 14], 'faces': faces},
                         {'from': [4, 13, 4], 'to': [12, 16, 12], 'faces': faces}]})
        side, top = tc('block/crate_side_%d' % tier), tc('block/crate_top')
        write(ASSETS, 'models/block/%s.json' % crate, {
            'parent': 'minecraft:block/block', 'textures': {'side': side, 'top': top, 'particle': side},
            'elements': [{'from': [1, 0, 1], 'to': [15, 14, 15], 'faces': faces}]})
        for name in (urn, crate):
            write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
            write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        tag('blocks', 'minecraft:mineable/axe', tc(crate))
        tag('blocks', 'minecraft:mineable/pickaxe', tc(urn))
    # сундук в башне мага (towerChestContents оригинала): от 4 до 9 вещей
    def tower_entry(item, weight, low, high):
        entry = {'type': 'minecraft:item', 'name': item, 'weight': weight}
        if (low, high) != (1, 1):
            entry['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': low, 'max': high}}]
        return entry
    write(DATA, 'thaumcraft/loot_tables/chests/wizard_tower.json', {'type': 'minecraft:chest', 'pools': [{
        'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 9},
        'entries': [tower_entry('minecraft:glowstone_dust', 3, 1, 3), tower_entry('minecraft:glass_bottle', 10, 1, 5),
                    tower_entry('minecraft:gold_nugget', 5, 1, 3), tower_entry('minecraft:fire_charge', 5, 1, 1),
                    tower_entry('minecraft:skeleton_skull', 3, 1, 1), tower_entry(tc('knowledge_fragment'), 20, 1, 3),
                    tower_entry(tc('alumentum'), 5, 1, 1), tower_entry(tc('nitor'), 5, 1, 1), tower_entry(tc('thaumium_ingot'), 5, 1, 2),
                    tower_entry(tc('thaumonomicon'), 20, 1, 1)]}]})
    tag('point_of_interest_type', 'minecraft:acquirable_job_site', tc('thaumaturge'), tc('moneychanger'))
    # пустотный металл: семя пустоты из семян пшеницы, слиток из семени, самородки, инструменты и броня
    simple_item('void_seed', 'voidseed')
    simple_item('void_metal_ingot', 'voidingot')
    simple_item('void_metal_nugget', 'nuggetvoid')
    tag('items', 'forge:ingots/void', tc('void_metal_ingot'))
    tag('items', 'forge:ingots', tc('void_metal_ingot'))
    tag('items', 'forge:nuggets/void', tc('void_metal_nugget'))
    tag('items', 'forge:nuggets', tc('void_metal_nugget'))
    crucible_recipe('void_seed', {'item': 'minecraft:wheat_seeds'}, {'tenebrae': 8, 'vacuos': 8, 'alienis': 2}, tc('void_seed'), 'VOIDMETAL')
    crucible_recipe('void_metal_ingot', {'item': tc('void_seed')}, {'metallum': 8}, tc('void_metal_ingot'), 'VOIDMETAL')
    write(DATA, 'thaumcraft/recipes/void_metal_nugget.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': tc('void_metal_ingot')}],
        'result': {'item': tc('void_metal_nugget'), 'count': 9}})
    write(DATA, 'thaumcraft/recipes/void_metal_ingot_from_nuggets.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['###', '###', '###'],
        'key': {'#': {'item': tc('void_metal_nugget')}}, 'result': {'item': tc('void_metal_ingot')}})
    void_ingot = {'tag': 'forge:ingots/void'}
    stick = {'tag': 'forge:rods/wooden'}
    for name, texture, pattern, category in (
            ('void_helmet', 'voidhelm', ['III', 'I I'], 'equipment'), ('void_chestplate', 'voidchest', ['I I', 'III', 'III'], 'equipment'),
            ('void_leggings', 'voidlegs', ['III', 'I I', 'I I'], 'equipment'), ('void_boots', 'voidboots', ['I I', 'I I'], 'equipment'),
            ('void_shovel', 'voidshovel', ['I', 'S', 'S'], 'equipment'), ('void_pickaxe', 'voidpick', ['III', ' S ', ' S '], 'equipment'),
            ('void_axe', 'voidaxe', ['II', 'SI', 'S '], 'equipment'), ('void_hoe', 'voidhoe', ['II', 'S ', 'S '], 'equipment'),
            ('void_sword', 'voidsword', ['I', 'I', 'S'], 'equipment')):
        if name.endswith(('shovel', 'pickaxe', 'axe', 'hoe', 'sword')):
            write(ASSETS, 'models/item/%s.json' % name, {'parent': 'minecraft:item/handheld', 'textures': {'layer0': tc('item/' + texture)}})
        else:
            simple_item(name, texture)
        key = {'I': void_ingot}
        if 'S' in ''.join(pattern):
            key['S'] = stick
        write(DATA, 'thaumcraft/recipes/%s.json' % name, {'type': 'minecraft:crafting_shaped', 'category': category,
                                                         'pattern': pattern, 'key': key, 'result': {'item': tc(name)}})
    for armor in ('helmet', 'chestplate', 'leggings', 'boots'):
        tag('items', 'minecraft:trimmable_armor', tc('void_' + armor))
    # пустотный набалдашник: craftCost 9 — мёртвая заготовка за 27 энтропии и порядка и 18 огня и воздуха, затем наполнение
    simple_item('wand_cap_void_inert', 'wand_cap_void_inert')
    simple_item('wand_cap_void', 'wand_cap_void')
    arcane('wand_cap_void_inert', ['NNN', 'N N'], {'N': {'tag': 'forge:nuggets/void'}}, 'wand_cap_void_inert',
           {'perditio': 27, 'ordo': 27, 'ignis': 18, 'aer': 18}, 'CAP_void')
    infusion_recipe('wand_cap_void', {'item': tc('wand_cap_void_inert')}, [{'item': tc('salis_mundus')}] * 4,
                    {'potentia': 18, 'vacuos': 18, 'alienis': 18, 'auram': 18}, 8, 'wand_cap_void', 'CAP_void')
    simple_item('sanity_checker', 'sanitychecker')
    infusion_recipe('sanity_checker', {'item': tc('thaumometer')}, [{'item': tc('mirrored_glass')}, {'item': tc('zombie_brain')},
                                                                    {'item': 'minecraft:diamond'}],
                    {'cognitio': 24, 'sensus': 24, 'alienis': 8}, 4, 'sanity_checker', 'SANITYCHECK')
    # пустотная мантия: окрашиваемая ткань (слой 0) и неокрашенная отделка (слой 1)
    for piece, layer0, layer1 in (('helmet', 'voidrobehelm', 'blank'), ('chestplate', 'voidrobechestover', 'voidrobechest'),
                                  ('leggings', 'voidrobelegsover', 'voidrobelegs')):
        write(ASSETS, 'models/item/void_robe_%s.json' % piece, {'parent': 'minecraft:item/generated', 'textures': {
            'layer0': tc('item/' + layer0), 'layer1': tc('item/' + layer1)}})
        tag('items', 'minecraft:dyeable', tc('void_robe_' + piece))
    salis = {'item': tc('salis_mundus')}
    fabric = {'item': tc('enchanted_fabric')}
    infusion_recipe('void_robe_helmet', {'item': tc('void_helmet')}, [{'item': tc('goggles')}, fabric, fabric, salis, fabric, fabric],
                    {'metallum': 16, 'sensus': 16, 'tutamen': 16, 'pannus': 16, 'praecantatio': 16, 'alienis': 16, 'vacuos': 16}, 6,
                    'void_robe_helmet', 'ARMORVOIDFORTRESS')
    for piece, robe, amount in (('chestplate', 'robe_chestplate', 24), ('leggings', 'robe_leggings', 20)):
        infusion_recipe('void_robe_' + piece, {'item': tc('void_' + piece)},
                        [{'item': tc(robe)}, {'item': tc('void_metal_ingot')}, {'item': tc('thaumium_ingot')}, salis, fabric,
                         {'item': 'minecraft:leather'}],
                        {'metallum': amount, 'tutamen': amount, 'pannus': amount, 'praecantatio': 16, 'alienis': 16, 'vacuos': amount}, 6,
                        'void_robe_' + piece, 'ARMORVOIDFORTRESS')
    write(ASSETS, 'models/item/primal_crusher.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': tc('item/primal_crusher')}})
    tag('blocks', 'thaumcraft:mineable/primal_crusher', '#minecraft:mineable/pickaxe', '#minecraft:mineable/shovel')
    charm = {'item': tc('primal_charm')}
    infusion_recipe('primal_crusher', {'item': tc('primordial_pearl')},
                    [charm, {'item': tc('void_pickaxe')}, {'item': tc('void_shovel')}, charm, {'item': tc('elemental_pickaxe')},
                     {'item': tc('elemental_shovel')}],
                    {'perfodio': 24, 'instrumentum': 24, 'perditio': 16, 'vacuos': 16, 'telum': 16, 'alienis': 16, 'lucrum': 16}, 6,
                    'primal_crusher', 'PRIMALCRUSHER')
    simple_item('eldritch_eye', 'eldritch_object')
    simple_item('crimson_rites', 'crimson_rites')
    simple_item('runed_tablet', 'eldritch_object_2')
    simple_item('primordial_pearl', 'eldritch_object_3')
    simple_item('obelisk_placer', 'ob_placer')
    for mob in ['brainy_zombie', 'giant_brainy_zombie', 'wisp', 'fire_bat', 'pech', 'mind_spider', 'eldritch_guardian', 'eldritch_warden', 'cultist_knight', 'cultist_cleric', 'cultist_leader', 'cultist_portal', 'eldritch_golem', 'eldritch_crab', 'inhabited_zombie', 'thaumic_slime', 'taint_spider', 'taintacle', 'taintacle_small', 'taint_spore', 'taint_spore_swarmer', 'taint_swarm', 'tainted_chicken', 'tainted_cow', 'tainted_creeper', 'tainted_pig', 'tainted_sheep', 'tainted_villager', 'taintacle_giant']:
        write(ASSETS, 'models/item/%s_spawn_egg.json' % mob, {'parent': 'minecraft:item/template_spawn_egg'})
    # око Элдрича: око Края, семя пустоты и золото
    infusion_recipe('eldritch_eye', {'item': 'minecraft:ender_eye'}, [{'item': tc('void_seed')}, {'tag': 'forge:ingots/gold'}],
                    {'alienis': 64, 'vacuos': 16, 'tenebrae': 16, 'iter': 16}, 5, 'eldritch_eye', 'OCULUS')
    # резервуар эссенции: стеклянный куб (сам блок), каркас и жидкость рисует EssentiaReservoirRenderer
    write(ASSETS, 'models/block/essentia_reservoir_frame.json', {
        'loader': 'forge:obj', 'model': tc('models/block/essentia_reservoir_frame.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/reservoir')}})
    write(ASSETS, 'models/block/essentia_reservoir.json', {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
        'textures': {'glass': tc('block/essentiareservoir'), 'particle': tc('block/essentiareservoir')},
        'elements': [{'from': [2, 2, 2], 'to': [14, 14, 14], 'faces': {f: {'texture': '#glass'} for f in
                                                                       ('north', 'south', 'west', 'east', 'up', 'down')}}]})
    write(ASSETS, 'blockstates/essentia_reservoir.json', {'variants': {'': {'model': tc('block/essentia_reservoir')}}})
    write(ASSETS, 'models/item/essentia_reservoir.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'display': {
        'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
    drop_self('essentia_reservoir')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('essentia_reservoir'))
    jar = {'item': tc('warded_jar')}
    void_ingot = {'item': tc('void_metal_ingot')}
    infusion_recipe('essentia_reservoir', {'item': tc('tube_buffer')}, [void_ingot, jar, jar, void_ingot, jar, jar],
                    {'aqua': 8, 'vacuos': 8, 'praecantatio': 8, 'permutatio': 8}, 6, 'essentia_reservoir', 'ESSENTIARESERVOIR')
    # продвинутая алхимическая печь: конструкция — обычный блок, сама печь рисуется от ядра AdvancedAlchemyFurnaceRenderer
    simple_block('advanced_alchemical_construct', 'alchemyblockadv')
    drop_self('advanced_alchemical_construct')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('advanced_alchemical_construct'), tc('advanced_alchemy_furnace'))
    for part in ('base', 'base_on', 'tank', 'tank_on'):
        write(ASSETS, 'models/block/adv_furnace_%s.json' % part, {
            'loader': 'forge:obj', 'model': tc('models/block/adv_furnace_%s.obj' % part), 'flip_v': True,
            'textures': {'particle': tc('block/metalbase')}})
    write(ASSETS, 'models/block/advanced_alchemy_furnace.json', {'textures': {'particle': tc('block/metalbase')}})
    write(ASSETS, 'blockstates/advanced_alchemy_furnace.json', {'variants': {'': {'model': tc('block/advanced_alchemy_furnace')}}})
    arcane('advanced_alchemical_construct', ['VAV', 'APA', 'VAV'],
           {'A': {'item': tc('alchemical_construct')}, 'V': {'item': tc('void_metal_ingot')}, 'P': {'item': tc('primordial_pearl')}},
           'advanced_alchemical_construct', {'aqua': 10, 'ordo': 30, 'terra': 10}, 'ADVALCHEMYFURNACE', 4)
    # узел в банке: та же стеклянная банка, узел рисует NodeJarRenderer
    write(ASSETS, 'models/block/node_jar.json', {'parent': tc('block/warded_jar')})
    write(ASSETS, 'blockstates/node_jar.json', {'variants': {'': {'model': tc('block/node_jar')}}})
    write(ASSETS, 'models/item/node_jar.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, -1, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
    # облачение Багрового культа и багровый клинок
    for name, texture in (('cultist_robe_helmet', 'cultistrobehelm'), ('cultist_robe_chestplate', 'cultistrobechest'),
                          ('cultist_robe_leggings', 'cultistrobelegs'), ('cultist_plate_helmet', 'cultistplatehelm'),
                          ('cultist_plate_chestplate', 'cultistplatechest'), ('cultist_plate_leggings', 'cultistplatelegs'),
                          ('cultist_leader_helmet', 'cultistplateleaderhelm'), ('cultist_leader_chestplate', 'cultistplateleaderchest'),
                          ('cultist_leader_leggings', 'cultistplateleaderlegs'), ('cultist_boots', 'cultistboots')):
        simple_item(name, texture)
    write(ASSETS, 'models/item/crimson_blade.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': tc('item/crimson_blade')}})
    # --- Запредельные земли: алтарь, обелиск, колпак и камни ---
    for name, particle in (('eldritch_altar', 'obsidiantile'), ('eldritch_obelisk', 'obsidiantile'),
                           ('eldritch_obelisk_segment', 'obsidiantile'), ('eldritch_capstone', 'obsidiantile')):
        # рисует EldritchObeliskRenderer; у блоков только частицы
        write(ASSETS, 'models/block/%s.json' % name, {'textures': {'particle': tc('block/' + particle)}})
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        tag('blocks', 'minecraft:mineable/pickaxe', tc(name))
    for out, texture in (('eldritch_cap', 'obelisk_cap'), ('eldritch_cap_2', 'obelisk_cap_2'), ('eldritch_cap_altar', 'obelisk_cap_altar')):
        write(ASSETS, 'models/block/%s.json' % out, {
            'loader': 'forge:obj', 'model': tc('models/block/%s.obj' % out), 'flip_v': True,
            'textures': {'particle': tc('block/' + texture)}})
    write(ASSETS, 'models/block/crab_vent.json', {
        'loader': 'forge:obj', 'model': tc('models/block/crab_vent.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/crabvent')}})
    # простые кубы; у древнего камня, скалы, рунного камня и светящихся камней грани подменяет EldritchStoneModel
    for name, texture in (('ancient_stone', 'es_1'), ('ancient_stone_sealed', 'es_1'), ('ancient_rock', 'er_1'), ('crusted_stone', 'crust'),
                          ('runed_stone', 'es_5'), ('glowing_crusted_stone', 'es_i_1'), ('glyphed_stone', 'es_i_2'),
                          ('ancient_doorway', 'deco_3'), ('crusted_opening', 'crust')):
        simple_block(name, texture)
        tag('blocks', 'minecraft:mineable/pickaxe', tc(name))
    for name in ('ancient_stone', 'ancient_stone_sealed', 'ancient_rock', 'crusted_stone', 'ancient_pedestal', 'glowing_crusted_stone',
                 'crusted_opening', 'runed_stone'):
        drop_self(name)
    # отверстие смотрит в любую сторону, но сам куб одинаков со всех сторон
    write(ASSETS, 'blockstates/crusted_opening.json', {'variants': {
        'facing=%s' % f: {'model': tc('block/crusted_opening')} for f in ('down', 'up', 'north', 'south', 'west', 'east')}})
    write(ASSETS, 'models/block/ancient_pedestal.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': tc('block/es_1'), 'bottom': tc('block/es_1'), 'side': tc('block/es_p')}})
    write(ASSETS, 'blockstates/ancient_pedestal.json', {'variants': {'': {'model': tc('block/ancient_pedestal')}}})
    write(ASSETS, 'models/item/ancient_pedestal.json', {'parent': tc('block/ancient_pedestal')})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('ancient_pedestal'))
    # испещрённый глифами камень даёт фрагмент знания (шёлковое касание — сам камень)
    write(DATA, 'thaumcraft/loot_tables/blocks/glyphed_stone.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc('glyphed_stone'), 'conditions': [SILK_TOUCH]},
            {'type': 'minecraft:item', 'name': tc('knowledge_fragment')},
        ]}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    })
    # плита из древнего камня
    es = tc('block/es_1')
    write(ASSETS, 'models/block/ancient_stone_slab.json',
          {'parent': 'minecraft:block/slab', 'textures': {'bottom': es, 'top': es, 'side': es}})
    write(ASSETS, 'models/block/ancient_stone_slab_top.json',
          {'parent': 'minecraft:block/slab_top', 'textures': {'bottom': es, 'top': es, 'side': es}})
    write(ASSETS, 'models/block/ancient_stone_full.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': es}})
    write(ASSETS, 'blockstates/ancient_stone_slab.json', {'variants': {
        'type=bottom': {'model': tc('block/ancient_stone_slab')}, 'type=top': {'model': tc('block/ancient_stone_slab_top')},
        'type=double': {'model': tc('block/ancient_stone_full')}}})
    write(ASSETS, 'models/item/ancient_stone_slab.json', {'parent': tc('block/ancient_stone_slab')})
    write(DATA, 'thaumcraft/loot_tables/blocks/ancient_stone_slab.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': tc('ancient_stone_slab'),
            'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
                'condition': 'minecraft:block_state_property', 'block': tc('ancient_stone_slab'), 'properties': {'type': 'double'}}]},
                {'function': 'minecraft:explosion_decay'}]}]}]})
    write(DATA, 'thaumcraft/recipes/ancient_stone_slab.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['KKK'],
        'key': {'K': {'item': tc('ancient_stone')}}, 'result': {'item': tc('ancient_stone_slab'), 'count': 6}})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('ancient_stone_slab'))
    for registry in ('blocks', 'items'):
        tag(registry, 'minecraft:slabs', tc('ancient_stone_slab'))
    # --- Запредельные земли: измерение-пустота, где по клеткам лабиринта строятся залы ---
    write(DATA, 'thaumcraft/worldgen/configured_feature/outer_lands_maze.json', {'type': tc('outer_lands_maze'), 'config': {}})
    write(DATA, 'thaumcraft/worldgen/placed_feature/outer_lands_maze.json', {'feature': tc('outer_lands_maze'), 'placement': []})
    write(DATA, 'thaumcraft/worldgen/biome/eldritch_lands.json', {
        'temperature': 0.5, 'downfall': 0.0, 'has_precipitation': False,
        'effects': {'sky_color': 0, 'fog_color': 1577767, 'water_color': 4159204, 'water_fog_color': 329011,
                    'mood_sound': {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}},
        'spawners': {'ambient': [], 'axolotls': [], 'creature': [], 'misc': [], 'underground_water_creature': [], 'water_ambient': [],
                     'water_creature': [],
                     'monster': [{'type': tc('inhabited_zombie'), 'weight': 1, 'minCount': 1, 'maxCount': 1},
                                 {'type': tc('eldritch_guardian'), 'weight': 1, 'minCount': 1, 'maxCount': 1}]},
        'spawn_costs': {}, 'carvers': {},
        'features': [[], [], [], [], [], [], [], [], [], [tc('outer_lands_maze')], []]})
    write(DATA, 'thaumcraft/dimension_type/outer_lands.json', {
        'ultrawarm': False, 'natural': False, 'coordinate_scale': 1.0, 'has_skylight': False, 'has_ceiling': False,
        'ambient_light': 0.0, 'fixed_time': 6000, 'monster_spawn_light_level': {
            'type': 'minecraft:uniform', 'value': {'min_inclusive': 0, 'max_inclusive': 7}},
        'monster_spawn_block_light_limit': 0, 'piglin_safe': False, 'bed_works': False, 'respawn_anchor_works': False,
        'has_raids': False, 'logical_height': 256, 'min_y': 0, 'height': 256,
        'infiniburn': '#minecraft:infiniburn_overworld', 'effects': tc('outer_lands')})
    write(DATA, 'thaumcraft/dimension/outer_lands.json', {
        'type': tc('outer_lands'),
        'generator': {'type': 'minecraft:flat', 'settings': {
            # плоский мир из одного невидимого и проницаемого слоя: совсем пустой (void) мир не ставит фич биома
            'biome': tc('eldritch_lands'), 'layers': [{'block': 'minecraft:structure_void', 'height': 1}], 'lakes': False,
            'features': True, 'structure_overrides': []}}})
    # новые блоки Запредельных земель
    for name, particle in (('eldritch_void', 'blank'), ('eldritch_void_open', 'blank'), ('eldritch_portal', 'blank'), ('impassable', 'blank'), ('sapping_field', 'blank'),
                           ('strange_crystals', 'crust')):
        write(ASSETS, 'models/block/%s.json' % name, {'textures': {'particle': tc('block/' + particle)}})
    for name in ('eldritch_void', 'eldritch_void_open', 'eldritch_portal', 'impassable', 'sapping_field'):
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
    write(ASSETS, 'blockstates/strange_crystals.json', {'variants': {
        'facing=%s' % f: {'model': tc('block/strange_crystals')} for f in ('down', 'up', 'north', 'south', 'west', 'east')}})
    write(DATA, 'thaumcraft/loot_tables/blocks/strange_crystals.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc('strange_crystals'), 'conditions': [SILK_TOUCH]},
            {'type': 'minecraft:item', 'name': tc('shard_balanced')}]}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    for out, group, texture in (('strange_crystals_base', 'Base', 'crust'), ('strange_crystals_crystal', 'Crystal', 'vcrystal')):
        write(ASSETS, 'models/block/%s.json' % out, {
            'loader': 'forge:obj', 'model': tc('models/block/%s.obj' % out), 'flip_v': True, 'render_type': 'minecraft:translucent',
            'textures': {'particle': tc('block/' + texture)}})
    # в руке друзу рисует StrangeCrystalsRenderer.ITEM той же отрисовкой, что и блок: obj-модель оригинала
    # отцентрована в нуле, и как обычная модель предмета она уезжала из ячейки
    write(ASSETS, 'models/item/strange_crystals.json', {
        'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': tc('block/vcrystal')}, 'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 1, 0], 'scale': [0.9, 0.9, 0.9]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.4, 0.4, 0.4]},
            'fixed': {'scale': [0.75, 0.75, 0.75]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_lefthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]}}})
    # замок: лицо с механизмом смотрит наружу, остальное — древний проём
    write(ASSETS, 'models/block/ancient_lock.json', {'parent': 'minecraft:block/orientable', 'textures': {
        'front': tc('block/deco_2'), 'side': tc('block/deco_3'), 'top': tc('block/deco_3')}})
    write(ASSETS, 'blockstates/ancient_lock.json', {'variants': {
        'facing=north': {'model': tc('block/ancient_lock')}, 'facing=east': {'model': tc('block/ancient_lock'), 'y': 90},
        'facing=south': {'model': tc('block/ancient_lock'), 'y': 180}, 'facing=west': {'model': tc('block/ancient_lock'), 'y': 270}}})
    write(ASSETS, 'models/item/ancient_lock.json', {'parent': tc('block/ancient_lock')})
    # узел ауры в руке рисует NodeJarRenderer.NODE_ITEM
    write(ASSETS, 'models/item/aura_node.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'front', 'display': {
        'gui': {'scale': [0.625, 0.625, 0.625]}, 'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'thirdperson_righthand': {'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'scale': [0.4, 0.4, 0.4]}, 'firstperson_lefthand': {'scale': [0.4, 0.4, 0.4]}}})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('strange_crystals'))
    tag('blocks', 'minecraft:dragon_immune', *[tc(n) for n in ('eldritch_void', 'eldritch_void_open', 'eldritch_portal', 'ancient_lock', 'ancient_doorway', 'impassable')])
    tag('blocks', 'minecraft:wither_immune', *[tc(n) for n in ('eldritch_void', 'eldritch_void_open', 'eldritch_portal', 'ancient_lock', 'ancient_doorway', 'impassable')])
    # --- олово, серебро и свинец: как в оригинале, только если металл даёт другой мод (условия Forge по тегам) ---
    def needs(path, *tags):
        full = os.path.join(DATA, *('thaumcraft/recipes/' + path + '.json').split('/'))
        recipe = json.load(open(full, encoding='utf-8'))
        recipe['conditions'] = [{'type': 'forge:not', 'value': {'type': 'forge:tag_empty', 'tag': t}} for t in tags]
        write(DATA, 'thaumcraft/recipes/' + path + '.json', recipe)

    for metal, extra in (('tin', 'vitreus'), ('silver', 'lucrum'), ('lead', 'ordo')):
        simple_item('cluster_' + metal, 'cluster' + metal)
        simple_item(metal + '_nugget', 'nugget' + metal)
        tag('items', 'forge:nuggets/' + metal, tc(metal + '_nugget'))
        tag('items', 'forge:nuggets', tc(metal + '_nugget'))
        # очистка руды в тигле: руда -> самородная гроздь
        crucible_recipe('pure_' + metal, {'tag': 'forge:ores/' + metal}, {'metallum': 1, 'ordo': 1}, tc('cluster_' + metal),
                        'PURE' + metal.upper())
        needs('crucible/pure_' + metal, 'forge:ores/' + metal, 'forge:ingots/' + metal)
        # преобразование: самородок -> три самородка
        crucible_recipe('trans_' + metal, {'tag': 'forge:nuggets/' + metal}, {'metallum': 2, extra: 1}, tc(metal + '_nugget'),
                        'TRANS' + metal.upper(), 3)
        needs('crucible/trans_' + metal, 'forge:ingots/' + metal)
        # гроздь плавится в два слитка того мода, что дал металл; девять самородков — в слиток через теги других модов
        write(DATA, 'thaumcraft/recipes/cluster_%s_smelting.json' % metal, {
            'type': tc('tag_smelting'), 'ingredient': {'item': tc('cluster_' + metal)}, 'result_tag': 'forge:ingots/' + metal,
            'count': 2, 'experience': 1.0, 'cookingtime': 200,
            'conditions': [{'type': 'forge:not', 'value': {'type': 'forge:tag_empty', 'tag': 'forge:ingots/' + metal}}]})
    # серебряный набалдашник: craftCost 4
    simple_item('wand_cap_silver_inert', 'wand_cap_silver_inert')
    simple_item('wand_cap_silver', 'wand_cap_silver')
    arcane('wand_cap_silver_inert', ['NNN', 'N N'], {'N': {'tag': 'forge:nuggets/silver'}},
           'wand_cap_silver_inert', {'ordo': 4, 'ignis': 4, 'aer': 4}, 'CAP_silver')
    needs('wand_cap_silver_inert', 'forge:ingots/silver')
    salis_silver = {'item': tc('salis_mundus')}
    infusion_recipe('wand_cap_silver', {'item': tc('wand_cap_silver_inert')}, [salis_silver, salis_silver],
                    {'potentia': 8, 'auram': 4}, 4, 'wand_cap_silver', 'CAP_silver')
    needs('infusion/wand_cap_silver', 'forge:ingots/silver')
    write(ASSETS, 'blockstates/lamp_light.json', {'variants': {'': {'model': 'minecraft:block/air'}}})
    drop_self('arcane_lamp')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('arcane_lamp'))
    write(ASSETS, 'blockstates/amber_block.json', {'variants': {'': {'model': tc('block/amber_block')}}})
    write(ASSETS, 'models/block/amber_block.json', {'parent': 'minecraft:block/cube_column', 'render_type': 'minecraft:translucent',
                                                    'textures': {'end': tc('block/amberblock_top'), 'side': tc('block/amberblock')}})
    write(ASSETS, 'blockstates/amber_bricks.json', {'variants': {'': {'model': tc('block/amber_bricks')}}})
    write(ASSETS, 'models/block/amber_bricks.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                                     'textures': {'all': tc('block/amberbrick')}})
    for name in ('amber_block', 'amber_bricks'):
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        drop_self(name)
    shaped('amber_block', ['##', '##'], {'#': {'item': tc('amber')}}, 'amber_block', 'building')
    shaped('amber_bricks', ['##', '##'], {'#': {'item': tc('amber_block')}}, 'amber_bricks', 'building')
    write(DATA, 'thaumcraft/recipes/amber_bricks.json', dict(
        json.load(open(os.path.join(DATA, 'thaumcraft/recipes/amber_bricks.json'), encoding='utf-8')),
        result={'item': tc('amber_bricks'), 'count': 4}))
    shapeless('amber_from_block', 'amber_block', tc('amber'), 4)
    arcane('arcane_lamp', [' S ', 'IAI', ' N '], {'A': {'item': tc('amber_block')}, 'S': {'item': 'minecraft:daylight_detector'},
                                                 'N': {'item': tc('nitor')}, 'I': {'tag': 'forge:ingots/iron'}},
           'arcane_lamp', {'ignis': 8, 'aer': 8, 'aqua': 4, 'perditio': 4}, 'ARCANELAMP')
    iron_axe = {'item': 'minecraft:iron_axe'}
    infusion_recipe('golem_core_lumber', {'item': tc('golem_core_harvest')},
                    [{'item': tc('elemental_axe')}, iron_axe, iron_axe, iron_axe],
                    {'instrumentum': 16, 'arbor': 16, 'meto': 16}, 2, 'golem_core_lumber', 'CORELUMBER')

    # --- стихийные инструменты (ConfigRecipes оригинала) ---
    diamond = {'item': 'minecraft:diamond'}
    greatwood = {'item': tc('greatwood_log')}
    for tool, shards, aspects in (
            ('axe', ('water', 'water'), {'aqua': 16, 'arbor': 8}),
            ('pickaxe', ('fire', 'fire'), {'ignis': 8, 'perfodio': 8, 'sensus': 8}),
            ('sword', ('air', 'air'), {'aer': 8, 'motus': 8, 'potentia': 8}),
            ('shovel', ('earth', 'earth'), {'terra': 16, 'fabrico': 8}),
            ('hoe', ('order', 'entropy'), {'meto': 8, 'herba': 8, 'terra': 8})):
        infusion_recipe('elemental_' + tool, {'item': tc('thaumium_' + tool)},
                        [{'item': tc('shard_' + shards[0])}, {'item': tc('shard_' + shards[1])}, diamond, greatwood],
                        aspects, 1, 'elemental_' + tool, 'ELEMENTAL' + {'pickaxe': 'PICK'}.get(tool, tool.upper()))
    for upgrade, aspect in (('air', 'aer'), ('earth', 'terra'), ('fire', 'ignis'), ('water', 'aqua'), ('order', 'ordo'),
                            ('entropy', 'perditio')):
        simple_item('golem_upgrade_' + upgrade, 'golem_upgrade_' + upgrade)
        arcane('golem_upgrade_' + upgrade, ['NNN', 'NCN', 'NNN'], {'N': {'tag': 'forge:nuggets/gold'}, 'C': {'item': tc('shard_' + upgrade)}},
               'golem_upgrade_' + upgrade, {aspect: 10}, 'UPGRADE' + upgrade.upper())
    arcane('mnemonic_matrix', ['IAI', 'ABA', 'IAI'], {'I': {'tag': 'forge:ingots/iron'}, 'A': {'item': tc('amber')},
                                                      'B': {'item': tc('zombie_brain')}},
           'mnemonic_matrix', {'ignis': 5, 'aqua': 5, 'ordo': 5}, 'THAUMATORIUM')
    arcane('deconstruction_table', [' S ', 'ATP'], {'T': {'item': tc('table')}, 'S': {'item': tc('thaumometer')},
                                                    'P': {'item': 'minecraft:golden_pickaxe'}, 'A': {'item': 'minecraft:golden_axe'}},
           'deconstruction_table', {'perditio': 20}, 'DECONSTRUCTOR')
    arcane('arcane_spa', ['QIQ', 'SJS', 'SPS'], {'Q': {'item': 'minecraft:quartz_block'}, 'I': {'item': 'minecraft:iron_bars'},
                                                 'S': {'item': tc('arcane_stone')}, 'J': {'item': tc('warded_jar')},
                                                 'P': {'item': 'minecraft:piston'}},
           'arcane_spa', {'aqua': 16, 'ordo': 8, 'terra': 4}, 'ARCANESPA')
    simple_item('sanity_soap', 'soap')
    crucible_recipe('sanity_soap', {'item': tc('tallow_block')}, {'cognitio': 16, 'alienis': 16, 'ordo': 16, 'sano': 16},
                    tc('sanity_soap'), 'SANESOAP')
    # эфирный цветок: блок рисует свой рендерер, модель нужна только для частиц; в руке — семя
    write(ASSETS, 'models/block/ethereal_bloom.json', {'textures': {'particle': tc('block/purifier_seed')}})
    write(ASSETS, 'blockstates/ethereal_bloom.json', {'variants': {'': {'model': tc('block/ethereal_bloom')}}})
    write(ASSETS, 'models/item/ethereal_bloom.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('block/purifier_seed')}})
    drop_self('ethereal_bloom')
    crucible_recipe('ethereal_bloom', {'item': tc('shimmerleaf')}, {'praecantatio': 16, 'herba': 16, 'sano': 16, 'vitium': 8},
                    tc('ethereal_bloom'), 'ETHEREALBLOOM')
    arcane('filter', ['GWG'], {'G': {'tag': 'forge:ingots/gold'}, 'W': {'item': tc('silverwood_planks')}},
           'filter', {'ordo': 5, 'aqua': 5}, 'DISTILESSENTIA', 2)
    arcane('alchemical_furnace', ['SCS', 'SFS', 'SSS'],
           {'S': {'item': tc('arcane_stone')}, 'C': {'item': tc('crucible')}, 'F': {'item': 'minecraft:furnace'}},
           'alchemical_furnace', {'ignis': 5, 'aqua': 5}, 'DISTILESSENTIA')
    arcane('alembic', ['FIG', 'IBI', 'I I'],
           {'I': {'tag': 'forge:ingots/iron'}, 'B': {'item': 'minecraft:bucket'}, 'G': {'tag': 'forge:ingots/gold'},
            'F': {'item': tc('filter')}}, 'alembic', {'aer': 5, 'aqua': 5}, 'DISTILESSENTIA')


def infusion_recipe(name, central, components, aspects, instability, result, research):
    write(DATA, 'thaumcraft/recipes/infusion/%s.json' % name, {
        'type': tc('infusion'), 'central': central, 'components': components, 'aspects': aspects,
        'instability': instability, 'result': {'item': tc(result)}, 'research': research})


def infusion_enchantments():
    """Зачарование наполнением: 24 рецепта оригинала, разобранные прямо из его исходника."""
    import enchant_recipes
    for recipe in enchant_recipes.recipes():
        write(DATA, 'thaumcraft/recipes/infusion/%s.json' % recipe['name'], {
            'type': tc('infusion_enchantment'), 'enchantment': recipe['enchantment'], 'instability': recipe['instability'],
            'aspects': recipe['aspects'], 'components': recipe['components'], 'research': recipe['research']})
    # вещи мода, которые чинятся зачарованием «Починка» (IRepairable оригинала)
    tag('items', 'thaumcraft:repairable', tc('goggles'), *[tc('thaumium_' + part) for part in (
        'sword', 'shovel', 'pickaxe', 'axe', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots')])


def infusion():
    infusion_enchantments()
    # в оригинале гладкий магический камень использует текстуру верха пьедестала, а кирпичи — arcane_stone
    simple_block('arcane_stone', 'pedestal_top')
    simple_block('arcane_stone_bricks', 'arcane_stone')
    drop_self('arcane_stone_bricks')
    write(DATA, 'thaumcraft/recipes/arcane_stone_bricks.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['SS', 'SS'],
        'key': {'S': {'item': tc('arcane_stone')}}, 'result': {'item': tc('arcane_stone_bricks'), 'count': 4}})

    # пьедестал: основание, ножка и столешница (BlockStoneDeviceRenderer оригинала)
    def part(x0, y0, x1, y1):
        faces = {f: {'texture': '#side'} for f in ('north', 'south', 'west', 'east')}
        faces['up'] = {'texture': '#top'}
        faces['down'] = {'texture': '#top'}
        return {'from': [x0, y0, x0], 'to': [x1, y1, x1], 'faces': faces}
    write(ASSETS, 'models/block/pedestal.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': tc('block/pedestal_side'), 'side': tc('block/pedestal_side'), 'top': tc('block/pedestal_top')},
        'elements': [part(0, 0, 16, 4), part(4, 4, 12, 12), part(2, 12, 14, 16)]})
    write(ASSETS, 'blockstates/pedestal.json', {'variants': {'': {'model': tc('block/pedestal')}}})
    write(ASSETS, 'models/item/pedestal.json', {'parent': tc('block/pedestal')})
    drop_self('pedestal')

    # матрица: восемь кубиков из ModelCube оригинала. В мире её рисует InfusionMatrixRenderer (она вращается),
    # эта модель — для предмета в руке и в инвентаре и для частиц при разрушении.
    whole = box_model('infuser', 64, 64, [(0, 0, -8, 0, -8, 16, 16, 16)])
    faces = whole['elements'][0]['faces']
    whole['elements'] = [
        {'from': [x, y, z], 'to': [x + 7.2, y + 7.2, z + 7.2], 'faces': faces}
        for x in (0.4, 8.4) for y in (0.4, 8.4) for z in (0.4, 8.4)]
    write(ASSETS, 'models/block/infusion_matrix.json', whole)
    write(ASSETS, 'blockstates/infusion_matrix.json', {'variants': {'': {'model': tc('block/infusion_matrix')}}})
    write(ASSETS, 'models/item/infusion_matrix.json', {'parent': tc('block/infusion_matrix')})
    drop_self('infusion_matrix')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('arcane_stone_bricks'), tc('pedestal'), tc('infusion_matrix'))

    # столп инфузии: нижняя половина рисует всю модель оригинала, повёрнутую к середине алтаря; верхняя пуста
    write(ASSETS, 'models/block/infusion_pillar.json', {
        'loader': 'forge:obj', 'model': tc('models/block/pillar.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/pillar')}})
    write(ASSETS, 'models/block/infusion_pillar_top.json', {'textures': {'particle': tc('block/pillar')}})
    variants = {}
    # orientation оригинала: 2 — без поворота, 3 — на 90, 4 — на 270, 5 — на 180 градусов (против часовой стрелки)
    for corner, y in ((0, 0), (1, 270), (2, 90), (3, 180)):
        variant = {'model': tc('block/infusion_pillar')}
        if y:
            variant['y'] = y
        variants['corner=%d,half=lower' % corner] = variant
        variants['corner=%d,half=upper' % corner] = {'model': tc('block/infusion_pillar_top')}
    write(ASSETS, 'blockstates/infusion_pillar.json', {'variants': variants})
    # как в оригинале: низ столпа отдаёт кирпичи, верх — гладкий камень
    half = lambda value: {'condition': 'minecraft:block_state_property', 'block': tc('infusion_pillar'),
                          'properties': {'half': value}}
    write(DATA, 'thaumcraft/loot_tables/blocks/infusion_pillar.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': tc('arcane_stone_bricks'), 'conditions': [half('lower')]},
            {'type': 'minecraft:item', 'name': tc('arcane_stone')}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('infusion_pillar'))

    simple_item('salis_mundus', 'dust')
    simple_item('wand_rod_silverwood', 'wand_rod_silverwood')
    simple_item('wand_cap_thaumium_inert', 'wand_cap_thaumium_inert')
    simple_item('wand_cap_thaumium', 'wand_cap_thaumium')

    # соль мира: переплавка уравновешенного осколка
    write(DATA, 'thaumcraft/recipes/salis_mundus.json', {
        'type': 'minecraft:smelting', 'category': 'misc', 'ingredient': {'item': tc('shard_balanced')},
        'result': tc('salis_mundus'), 'experience': 1.0, 'cookingtime': 200})
    arcane('infusion_matrix', ['SBS', 'BEB', 'SBS'],
           {'S': {'item': tc('arcane_stone')}, 'E': {'item': 'minecraft:ender_pearl'}, 'B': {'tag': tc('shards')}},
           'infusion_matrix', {'ordo': 40}, 'INFUSION')
    arcane('pedestal', ['SSS', ' S ', 'SSS'], {'S': {'item': tc('arcane_stone')}}, 'pedestal', {'aer': 5}, 'INFUSION', 2)
    # цена набалдашника — craftCost тауминия (6), как в оригинале
    arcane('wand_cap_thaumium_inert', ['NNN', 'N N'], {'N': {'tag': 'forge:nuggets/thaumium'}},
           'wand_cap_thaumium_inert', {'ordo': 6, 'ignis': 6, 'aer': 6}, 'CAP_thaumium')
    salis = {'item': tc('salis_mundus')}
    infusion_recipe('wand_cap_thaumium', {'item': tc('wand_cap_thaumium_inert')}, [salis, salis, salis],
                    {'potentia': 12, 'auram': 6}, 5, 'wand_cap_thaumium', 'CAP_thaumium')
    # стержни стихий: craftCost 6 — удвоенный аспект стихии, магия и третий аспект; середина и два осколка
    for name, element, own, third, central in (
            ('obsidian', 'earth', 'terra', 'tenebrae', 'minecraft:obsidian'),
            ('ice', 'water', 'aqua', 'gelum', 'minecraft:ice'),
            ('quartz', 'order', 'ordo', 'vitreus', 'minecraft:quartz_block'),
            ('reed', 'air', 'aer', 'motus', 'minecraft:sugar_cane'),
            ('blaze', 'fire', 'ignis', 'bestia', 'minecraft:blaze_rod'),
            ('bone', 'entropy', 'perditio', 'exanimis', 'minecraft:bone')):
        simple_item('wand_rod_' + name, 'wand_rod_' + name)
        infusion_recipe('wand_rod_' + name, {'item': central},
                        [{'item': tc('shard_balanced')}, {'item': tc('shard_' + element)}],
                        {own: 12, 'praecantatio': 6, third: 6}, 3, 'wand_rod_' + name, 'ROD_' + name)
    # --- медь: самородок (9 из слитка), набалдашник и утроение самородков в тигле ---
    simple_item('copper_nugget', 'nuggetcopper')
    simple_item('wand_cap_copper', 'wand_cap_copper')
    write(DATA, 'thaumcraft/recipes/copper_ingot_from_nuggets.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['###', '###', '###'],
        'key': {'#': {'tag': 'forge:nuggets/copper'}}, 'result': {'item': 'minecraft:copper_ingot'}})
    write(DATA, 'thaumcraft/recipes/copper_nugget.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'tag': 'forge:ingots/copper'}],
        'result': {'item': tc('copper_nugget'), 'count': 9}})
    tag('items', 'forge:nuggets/copper', tc('copper_nugget'))
    tag('items', 'forge:nuggets', tc('copper_nugget'))
    arcane('wand_cap_copper', ['NNN', 'N N'], {'N': {'tag': 'forge:nuggets/copper'}},
           'wand_cap_copper', {'ordo': 2, 'ignis': 2, 'aer': 2}, 'CAP_copper')
    crucible_recipe('trans_copper', {'tag': 'forge:nuggets/copper'}, {'metallum': 2, 'permutatio': 1},
                    tc('copper_nugget'), 'TRANSCOPPER', 3)

    # --- первозданный амулет: шесть осколков стихий вокруг уравновешенного, по бокам золото ---
    simple_item('primal_charm', 'charm')
    arcane('primal_charm', ['123', 'ISI', '456'],
           {'S': {'item': tc('shard_balanced')}, 'I': {'tag': 'forge:ingots/gold'}, '1': {'item': tc('shard_air')},
            '2': {'item': tc('shard_fire')}, '3': {'item': tc('shard_water')}, '4': {'item': tc('shard_earth')},
            '5': {'item': tc('shard_order')}, '6': {'item': tc('shard_entropy')}},
           'primal_charm', {a: 25 for a in ('terra', 'ignis', 'aer', 'aqua', 'ordo', 'perditio')}, 'BASICARTIFACE')

    # --- украшения: заготовки амулета, кольца и пояса (ячейки даёт Curios), сумка для фокусов ---
    for name, texture, slot, pattern, key in (
            ('bauble_amulet', 'bauble_amulet', 'necklace', [' S ', 'S S', ' I '],
             {'S': {'tag': 'forge:string'}, 'I': {'tag': 'forge:ingots/gold'}}),
            ('bauble_ring', 'bauble_ring', 'ring', [' N ', 'N N', ' N '], {'N': {'tag': 'forge:nuggets/gold'}}),
            ('bauble_belt', 'bauble_belt', 'belt', [' L ', 'L L', ' I '],
             {'L': {'tag': 'forge:leather'}, 'I': {'tag': 'forge:ingots/gold'}})):
        simple_item(name, texture)
        write(DATA, 'thaumcraft/recipes/%s.json' % name, {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
            'result': {'item': tc(name)}})
        tag('items', 'curios:' + slot, tc(name))
    simple_item('focus_pouch', 'focuspouch')
    tag('items', 'curios:belt', tc('focus_pouch'))
    arcane('focus_pouch', ['LGL', 'LBL', 'LLL'],
           {'L': {'tag': 'forge:leather'}, 'G': {'tag': 'forge:ingots/gold'}, 'B': {'item': tc('bauble_belt')}},
           'focus_pouch', {'terra': 10, 'ordo': 10, 'perditio': 10}, 'FOCUSPOUCH')
    # ячейки игрока, как в Baubles оригинала: амулет, два кольца и пояс
    write(DATA, 'thaumcraft/curios/entities/player.json', {'entities': ['player'], 'slots': ['necklace', 'ring', 'belt']})
    write(DATA, 'thaumcraft/curios/slots/ring.json', {'size': 2})

    # --- фокусы жезла: кварц, четыре осколка стихии и сердцевина ---
    for focus, core, shard, cost in (
            ('fire', {'item': 'minecraft:fire_charge'}, 'shard_fire', {'ignis': 20, 'perditio': 10}),
            ('frost', {'tag': 'forge:gems/diamond'}, 'shard_water', {'aqua': 10, 'ordo': 10, 'perditio': 10}),
            ('shock', {'item': 'minecraft:potato'}, 'shard_air', {'aer': 10, 'ordo': 10, 'perditio': 10}),
            ('excavation', {'tag': 'forge:gems/emerald'}, 'shard_earth', {'terra': 20, 'perditio': 5, 'ordo': 5})):
        simple_item('focus_' + focus, 'focus_' + focus)
        arcane('focus_' + focus, ['CQC', 'Q#Q', 'CQC'],
               {'#': core, 'Q': {'tag': 'forge:gems/quartz'}, 'C': {'item': tc(shard)}},
               'focus_' + focus, cost, 'FOCUS' + focus.upper())
    simple_item('focus_pech', 'focus_pech')
    simple_item('focus_primal', 'focus_primal')
    arcane('focus_primal', ['CQC', 'Q#Q', 'CQC'],
           {'#': {'item': tc('primal_charm')}, 'Q': {'tag': 'forge:gems/quartz'}, 'C': {'tag': 'forge:gems/diamond'}},
           'focus_primal', {a: 25 for a in ('terra', 'perditio', 'ordo', 'aer', 'ignis', 'aqua')}, 'FOCUSPRIMAL')
    # портативная дыра: фокус (инфузия) и сам блок дыры — невидимый, его стенки рисует HoleRenderer
    simple_item('focus_portable_hole', 'focus_portablehole')
    infusion_recipe('focus_portable_hole', {'item': 'minecraft:ender_pearl'},
                    [{'tag': 'forge:gems/quartz'}, {'item': tc('shard_earth')}, {'tag': 'forge:gems/quartz'},
                     {'item': tc('shard_air')}, {'tag': 'forge:gems/quartz'}, {'item': tc('shard_entropy')}],
                    {'iter': 25, 'alienis': 10, 'permutatio': 10, 'perditio': 25}, 3,
                    'focus_portable_hole', 'FOCUSPORTABLEHOLE')
    write(ASSETS, 'models/block/hole.json', {'textures': {'particle': 'minecraft:block/obsidian'}})
    write(ASSETS, 'blockstates/hole.json', {'variants': {'': {'model': tc('block/hole')}}})
    tag('blocks', 'thaumcraft:portable_hole_blacklist', '#minecraft:beds', '#minecraft:doors', 'minecraft:piston',
        'minecraft:sticky_piston', 'minecraft:piston_head', 'minecraft:moving_piston')
    # девять кругов ада: значок фокуса двухслойный (украшение под кристаллом), рецепт — инфузия
    write(ASSETS, 'models/item/focus_hellbat.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': tc('item/focus_hellbat_orn'), 'layer1': tc('item/focus_hellbat')}})
    infusion_recipe('focus_hellbat', {'item': 'minecraft:magma_cream'},
                    [{'tag': 'forge:gems/quartz'}, {'item': tc('shard_fire')}, {'tag': 'forge:gems/quartz'},
                     {'item': tc('shard_air')}, {'tag': 'forge:gems/quartz'}, {'item': tc('shard_entropy')}],
                    {'ignis': 25, 'aer': 15, 'bestia': 15, 'perditio': 25}, 3, 'focus_hellbat', 'FOCUSHELLBAT')
    # огненная летучая мышь: порох с диких, появляется в Нижнем мире с тем же весом, что в оригинале
    write(DATA, 'thaumcraft/loot_tables/entities/fire_bat.json', {
        'type': 'minecraft:entity', 'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': 'minecraft:gunpowder',
            'functions': [{'function': 'minecraft:set_count', 'count': {'min': 0, 'max': 2}},
                          {'function': 'minecraft:looting_enchant', 'count': {'min': 0, 'max': 1}}]}]}]})
    write(DATA, 'thaumcraft/forge/biome_modifier/fire_bat.json', {
        'type': 'forge:add_spawns', 'biomes': '#minecraft:is_nether',
        'spawners': {'type': tc('fire_bat'), 'weight': 10, 'minCount': 1, 'maxCount': 2}})
    # равноценный обмен: двухслойный значок, ртуть в середине и уравновешенные осколки по углам
    write(ASSETS, 'models/item/focus_trade.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': tc('item/focus_trade_orn'), 'layer1': tc('item/focus_trade')}})
    arcane('focus_trade', ['CQC', 'Q#Q', 'CQC'],
           {'#': {'item': tc('quicksilver')}, 'Q': {'tag': 'forge:gems/quartz'}, 'C': {'item': tc('shard_balanced')}},
           'focus_trade', {'ordo': 15, 'perditio': 15, 'terra': 10}, 'FOCUSTRADE')
    # защита: двухслойный значок фокуса, инфузия со звездой Нижнего мира; модель блока подменяется в коде
    write(ASSETS, 'models/item/focus_warding.json', {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': tc('item/focus_warding_orn'), 'layer1': tc('item/focus_warding')}})
    infusion_recipe('focus_warding', {'item': 'minecraft:nether_star'},
                    [{'item': tc('quicksilver')}, {'item': tc('shard_earth')}, {'tag': 'forge:gems/quartz'}, {'item': tc('shard_order')},
                     {'item': tc('quicksilver')}, {'item': tc('shard_earth')}, {'tag': 'forge:gems/quartz'}, {'item': tc('shard_order')}],
                    {'terra': 25, 'tutamen': 25, 'ordo': 25, 'cognitio': 10}, 4, 'focus_warding', 'FOCUSWARDING')
    write(ASSETS, 'models/block/warded.json', {'textures': {'particle': 'minecraft:block/stone'}})
    write(ASSETS, 'blockstates/warded.json', {'variants': {'': {'model': tc('block/warded')}}})
    tag('blocks', 'minecraft:dragon_immune', tc('warded'))
    tag('blocks', 'minecraft:wither_immune', tc('warded'))
    tag('blocks', 'thaumcraft:portable_hole_blacklist', tc('warded'))
    # --- сеть вис: стабилизаторы узла, преобразователь и реле; модели ставят рендереры ---
    for part, texture in (('node_stabilizer_lock', 'node_stabilizer'), ('node_stabilizer_piston', 'node_stabilizer'),
                          ('node_stabilizer_piston_over', 'node_stabilizer_over'), ('node_converter_lock', 'node_converter'),
                          ('node_converter_lock_over', 'node_converter_over'), ('node_converter_piston', 'node_converter'),
                          ('node_converter_piston_over', 'node_converter_over'), ('vis_relay_base', 'vis_relay'),
                          ('vis_relay_float', 'vis_relay')):
        write(ASSETS, 'models/block/%s.json' % part, {
            'loader': 'forge:obj', 'model': tc('models/block/%s.obj' % part), 'flip_v': True,
            'textures': {'particle': tc('block/' + texture)}})
    for name, particle, lock_model, transform in (
            ('node_stabilizer', 'node_stabilizer', 'node_stabilizer_lock', {'translation': [0.5, 0.125, -0.5], 'rotation': [-90, 0, 0], 'origin': 'corner'}),
            ('advanced_node_stabilizer', 'node_stabilizer', 'node_stabilizer_lock', {'translation': [0.5, 0.125, -0.5], 'rotation': [-90, 0, 0], 'origin': 'corner'}),
            ('node_transducer', 'node_converter', 'node_converter_lock', {'translation': [0.5, -0.125, 0.5], 'rotation': [90, 0, 0], 'origin': 'corner'}),
            ('vis_relay', 'vis_relay', 'vis_relay_float', {'translation': [0.5, -1.0, 0.26], 'rotation': [90, 0, 0], 'origin': 'corner'})):
        write(ASSETS, 'models/block/%s.json' % name, {'textures': {'particle': tc('block/' + particle)}})
        if name == 'vis_relay':
            write(ASSETS, 'blockstates/vis_relay.json', {'variants': {'': {'model': tc('block/vis_relay')}}})
        else:
            write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        # предмет: основная часть модели, поставленная в клетку
        write(ASSETS, 'models/item/%s.json' % name, {
            'loader': 'forge:obj', 'model': tc('models/block/%s.obj' % lock_model), 'flip_v': True,
            'textures': {'particle': tc('block/' + particle)}, 'transform': transform,
            'display': {
                'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
                'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                'fixed': {'scale': [0.5, 0.5, 0.5]},
                'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
                'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
                'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}})
        drop_self(name)
    # предмет реле собран, как блок в мире (VisNetRenderers.Relay, реле на полу): основание, кольцо и кристалл.
    # Загрузчик obj применяет преобразование вокруг центра блока, сдвиги заданы с поправкой на это.
    relay_turn = [{'x': -90}, {'z': 45}]
    relay_display = json.loads(json.dumps(json.load(open(os.path.join(ASSETS, 'models/item/node_stabilizer.json')))['display']))
    # реле маленькое и стоит на полу клетки: в ячейке оно увеличено и поднято в середину
    relay_display['gui']['scale'] = [1.7, 1.7, 1.7]
    relay_display['gui']['translation'] = [0, 6.5, 0]
    write(ASSETS, 'models/item/vis_relay.json', {
        'loader': 'forge:composite', 'textures': {'particle': tc('block/vis_relay')},
        'children': {
            'base': {'loader': 'forge:obj', 'model': tc('models/block/vis_relay_base.obj'), 'flip_v': True,
                     'textures': {'particle': tc('block/vis_relay')},
                     'transform': {'translation': [0.0, 0.255, -0.5303], 'rotation': relay_turn, 'scale': 0.75, 'origin': 'corner'}},
            'float': {'loader': 'forge:obj', 'model': tc('models/block/vis_relay_float.obj'), 'flip_v': True,
                      'textures': {'particle': tc('block/vis_relay')},
                      'transform': {'translation': [0.0, 0.5, -0.7071], 'rotation': relay_turn, 'origin': 'corner'}},
            'crystal': {'loader': 'forge:obj', 'model': tc('models/block/vis_relay_crystal.obj'), 'flip_v': True,
                        'render_type': 'minecraft:translucent', 'textures': {'particle': tc('block/crystalizer')},
                        'transform': {'translation': [0.0, 0.5, -0.7071], 'rotation': relay_turn, 'origin': 'corner'}}},
        'item_render_order': ['base', 'float', 'crystal'],
        'display': relay_display})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('node_stabilizer'), tc('advanced_node_stabilizer'), tc('node_transducer'),
        tc('vis_relay'))
    arcane('node_stabilizer', [' G ', 'QPQ', 'SNS'],
           {'S': {'item': tc('arcane_stone_bricks')}, 'G': {'tag': 'forge:ingots/gold'}, 'P': {'item': 'minecraft:piston'},
            'Q': {'item': 'minecraft:quartz_block'}, 'N': {'item': tc('nitor')}},
           'node_stabilizer', {'aqua': 32, 'terra': 32, 'ordo': 32}, 'NODESTABILIZER')
    arcane('node_transducer', ['RCR', 'ISI', 'RAR'],
           {'S': {'item': tc('node_stabilizer')}, 'C': {'item': 'minecraft:comparator'}, 'I': {'tag': 'forge:ingots/iron'},
            'R': {'item': 'minecraft:redstone_block'}, 'A': {'item': tc('nitor')}},
           'node_transducer', {'ignis': 32, 'aer': 32, 'perditio': 32}, 'VISPOWER')
    arcane('vis_relay', [' I ', 'ISI', ' I '], {'I': {'tag': 'forge:ingots/iron'}, 'S': {'item': tc('shard_balanced')}},
           'vis_relay', {'ignis': 8, 'ordo': 8}, 'VISPOWER', 2)
    infusion_recipe('advanced_node_stabilizer', {'item': tc('node_stabilizer')},
                    [{'item': tc('nitor')}, {'item': 'minecraft:redstone_block'}, {'item': tc('alumentum')},
                     {'item': 'minecraft:redstone_block'}, {'item': tc('nitor')}, {'item': 'minecraft:redstone_block'},
                     {'item': tc('alumentum')}, {'item': 'minecraft:redstone_block'}],
                    {'auram': 32, 'praecantatio': 16, 'ordo': 16, 'potentia': 16}, 10,
                    'advanced_node_stabilizer', 'NODESTABILIZERADV')
    # --- фокальный манипулятор: тот же стол, что у магического верстака, со своей текстурой ---
    write(ASSETS, 'models/block/focal_manipulator.json', box_model('wandtable', 128, 64, [
        (0, 0, -8, 0, -8, 16, 8, 16),
        (0, 32, -8, 12, -8, 16, 4, 16),
        (72, 0, 3, 8, -7, 4, 4, 4),
        (72, 0, -7, 8, 3, 4, 4, 4),
        (72, 0, 3, 8, 3, 4, 4, 4),
        (72, 0, -7, 8, -7, 4, 4, 4),
    ]))
    write(ASSETS, 'blockstates/focal_manipulator.json', {'variants': {'': {'model': tc('block/focal_manipulator')}}})
    write(ASSETS, 'models/item/focal_manipulator.json', {'parent': tc('block/focal_manipulator')})
    drop_self('focal_manipulator')
    arcane('focal_manipulator', ['IQI', 'SPS', 'GTG'],
           {'Q': {'item': tc('arcane_stone_slab')}, 'S': {'item': tc('arcane_stone')}, 'T': {'item': tc('table')},
            'I': {'tag': 'forge:ingots/iron'}, 'G': {'tag': 'forge:ingots/gold'}, 'P': {'item': tc('primal_charm')}},
           'focal_manipulator', {a: 32 for a in ('ignis', 'aer', 'perditio', 'terra', 'aqua', 'ordo')}, 'FOCALMANIPULATION')
    # плита из магического камня: шесть штук из трёх кирпичных блоков
    brick = tc('block/arcane_stone')
    write(ASSETS, 'models/block/arcane_stone_slab.json',
          {'parent': 'minecraft:block/slab', 'textures': {'bottom': brick, 'top': brick, 'side': brick}})
    write(ASSETS, 'models/block/arcane_stone_slab_top.json',
          {'parent': 'minecraft:block/slab_top', 'textures': {'bottom': brick, 'top': brick, 'side': brick}})
    write(ASSETS, 'blockstates/arcane_stone_slab.json', {'variants': {
        'type=bottom': {'model': tc('block/arcane_stone_slab')}, 'type=top': {'model': tc('block/arcane_stone_slab_top')},
        'type=double': {'model': tc('block/arcane_stone_bricks')}}})
    write(ASSETS, 'models/item/arcane_stone_slab.json', {'parent': tc('block/arcane_stone_slab')})
    write(DATA, 'thaumcraft/loot_tables/blocks/arcane_stone_slab.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': tc('arcane_stone_slab'),
            'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
                'condition': 'minecraft:block_state_property', 'block': tc('arcane_stone_slab'), 'properties': {'type': 'double'}}]},
                {'function': 'minecraft:explosion_decay'}]}]}]})
    write(DATA, 'thaumcraft/recipes/arcane_stone_slab.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['KKK'],
        'key': {'K': {'item': tc('arcane_stone_bricks')}}, 'result': {'item': tc('arcane_stone_slab'), 'count': 6}})
    tag('blocks', 'minecraft:mineable/pickaxe', tc('focal_manipulator'), tc('arcane_stone_slab'))
    tag('blocks', 'minecraft:slabs', tc('arcane_stone_slab'))
    tag('items', 'minecraft:slabs', tc('arcane_stone_slab'))
    # --- ступени и плиты: большое дерево, серебряное дерево, магический камень ---
    def stairs(name, texture, base, tool, wooden):
        tex = {'bottom': tc('block/' + texture), 'top': tc('block/' + texture), 'side': tc('block/' + texture)}
        for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
            write(ASSETS, 'models/block/%s%s.json' % (name, suffix), {'parent': 'minecraft:block/' + parent, 'textures': tex})
        turn = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
        variants = {}
        for facing, base_y in turn.items():
            for half in ('bottom', 'top'):
                for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                    suffix = '' if shape == 'straight' else '_' + shape.split('_')[0]
                    if half == 'bottom':
                        y = base_y - 90 if shape.endswith('left') else base_y
                    else:
                        y = base_y + 90 if shape.endswith('right') else base_y
                    variant = {'model': tc('block/' + name + suffix)}
                    if half == 'top':
                        variant['x'] = 180
                    if y % 360:
                        variant['y'] = y % 360
                    if len(variant) > 1:
                        variant['uvlock'] = True
                    variants['facing=%s,half=%s,shape=%s' % (facing, half, shape)] = variant
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': variants})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        drop_self(name)
        write(DATA, 'thaumcraft/recipes/%s.json' % name, {
            'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['K  ', 'KK ', 'KKK'],
            'key': {'K': {'item': tc(base)}}, 'result': {'item': tc(name), 'count': 4}})
        tag('blocks', 'minecraft:mineable/' + tool, tc(name))
        for registry in ('blocks', 'items'):
            tag(registry, 'minecraft:stairs', tc(name))
            if wooden:
                tag(registry, 'minecraft:wooden_stairs', tc(name))

    stairs('arcane_stone_stairs', 'arcane_stone', 'arcane_stone_bricks', 'pickaxe', False)
    stairs('ancient_stone_stairs', 'es_1', 'ancient_stone', 'pickaxe', False)
    for wood in ('greatwood', 'silverwood'):
        stairs(wood + '_stairs', 'planks_' + wood, wood + '_planks', 'axe', True)
        slab = wood + '_slab'
        plank = tc('block/planks_' + wood)
        write(ASSETS, 'models/block/%s.json' % slab,
              {'parent': 'minecraft:block/slab', 'textures': {'bottom': plank, 'top': plank, 'side': plank}})
        write(ASSETS, 'models/block/%s_top.json' % slab,
              {'parent': 'minecraft:block/slab_top', 'textures': {'bottom': plank, 'top': plank, 'side': plank}})
        write(ASSETS, 'blockstates/%s.json' % slab, {'variants': {
            'type=bottom': {'model': tc('block/' + slab)}, 'type=top': {'model': tc('block/' + slab + '_top')},
            'type=double': {'model': tc('block/' + wood + '_planks')}}})
        write(ASSETS, 'models/item/%s.json' % slab, {'parent': tc('block/' + slab)})
        write(DATA, 'thaumcraft/loot_tables/blocks/%s.json' % slab, {
            'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
                'type': 'minecraft:item', 'name': tc(slab),
                'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False, 'conditions': [{
                    'condition': 'minecraft:block_state_property', 'block': tc(slab), 'properties': {'type': 'double'}}]},
                    {'function': 'minecraft:explosion_decay'}]}]}]})
        write(DATA, 'thaumcraft/recipes/%s.json' % slab, {
            'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['KKK'],
            'key': {'K': {'item': tc(wood + '_planks')}}, 'result': {'item': tc(slab), 'count': 6}})
        tag('blocks', 'minecraft:mineable/axe', tc(slab))
        for registry in ('blocks', 'items'):
            tag(registry, 'minecraft:slabs', tc(slab))
            tag(registry, 'minecraft:wooden_slabs', tc(slab))
    write(ASSETS, 'models/block/electrified_ground.json', {'textures': {'particle': tc('block/crystalizer')}})
    write(ASSETS, 'blockstates/electrified_ground.json', {'variants': {'': {'model': tc('block/electrified_ground')}}})
    # ледяной осколок: модель orb.obj оригинала, её ставит FrostShardRenderer
    write(ASSETS, 'models/block/frost_shard.json', {
        'loader': 'forge:obj', 'model': tc('models/block/frost_shard.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/frostshard')}})

    # --- сердцевины посохов: два стержня и первозданный амулет; цена — craftCost посоха в порядке ---
    for material in ('greatwood', 'obsidian', 'silverwood', 'ice', 'quartz', 'reed', 'blaze', 'bone'):
        cost = 8 if material == 'greatwood' else 24 if material == 'silverwood' else 14
        simple_item('wand_rod_%s_staff' % material, 'staff_rod_' + material)
        arcane('wand_rod_%s_staff' % material, ['  S', ' G ', 'G  '],
               {'S': {'item': tc('primal_charm')}, 'G': {'item': tc('wand_rod_' + material)}},
               'wand_rod_%s_staff' % material, {'ordo': cost}, 'ROD_%s_staff' % material)
    # первозданный посох: серебряный стержень, шесть стержней стихий и два амулета; craftCost 32
    simple_item('wand_rod_primal_staff', 'staff_rod_primal')
    infusion_recipe('wand_rod_primal_staff', {'item': tc('wand_rod_silverwood')},
                    [{'item': tc('primal_charm')}, {'item': tc('wand_rod_obsidian')}, {'item': tc('wand_rod_ice')},
                     {'item': tc('wand_rod_quartz')}, {'item': tc('primal_charm')}, {'item': tc('wand_rod_reed')},
                     {'item': tc('wand_rod_blaze')}, {'item': tc('wand_rod_bone')}],
                    dict({a: 32 for a in ('aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio')}, praecantatio=64), 8,
                    'wand_rod_primal_staff', 'ROD_primal_staff')

    # стержень: craftCost серебряного дерева 9 на каждый первичный аспект и магию
    infusion_recipe('wand_rod_silverwood', {'item': tc('silverwood_log')},
                    [{'item': tc('shard_' + e)} for e in ['balanced'] + ELEMENTS],
                    {a: 9 for a in ('aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio', 'praecantatio')}, 5,
                    'wand_rod_silverwood', 'ROD_silverwood')


def warp():
    """Искажения зрения от безумия: те же шейдеры, что в оригинале, на ванильных программах 1.20."""
    def post(name, passes):
        write(ASSETS, 'shaders/post/%s.json' % name, {'targets': ['swap'], 'passes': passes})
    blit = {'name': 'blit', 'intarget': 'swap', 'outtarget': 'minecraft:main'}
    post('desaturate', [{'name': 'color_convolve', 'intarget': 'minecraft:main', 'outtarget': 'swap',
                         'uniforms': [{'name': 'Saturation', 'values': [0.8]}]}, blit])
    post('hunger', [{'name': 'color_convolve', 'intarget': 'minecraft:main', 'outtarget': 'swap',
                     'uniforms': [{'name': 'ColorScale', 'values': [1.0, 0.8, 0.8]}, {'name': 'Saturation', 'values': [1.1]}]}, blit])
    # размытие в два прохода (по X и по Y) через две промежуточные цели и в конце перенос в кадр — как ванильные цепочки
    write(ASSETS, 'shaders/post/blur.json', {'targets': ['swap', 'swap2'], 'passes': [
        {'name': 'thaumcraft:blur', 'intarget': 'minecraft:main', 'outtarget': 'swap',
         'uniforms': [{'name': 'BlurDir', 'values': [1.0, 0.0]}, {'name': 'Radius', 'values': [10.0]}]},
        {'name': 'thaumcraft:blur', 'intarget': 'swap', 'outtarget': 'swap2',
         'uniforms': [{'name': 'BlurDir', 'values': [0.0, 1.0]}, {'name': 'Radius', 'values': [10.0]}]},
        {'name': 'blit', 'intarget': 'swap2', 'outtarget': 'minecraft:main'}]})

    # своя программа размытия: ванильная оставляет в кадре нулевую непрозрачность, и экран чернеет
    write(ASSETS, 'shaders/program/blur.json', {
        'blend': {'func': 'add', 'srcrgb': 'one', 'dstrgb': 'zero'},
        'vertex': 'sobel', 'fragment': 'thaumcraft:blur', 'attributes': ['Position'],
        'samplers': [{'name': 'DiffuseSampler'}],
        'uniforms': [
            {'name': 'ProjMat', 'type': 'matrix4x4', 'count': 16,
             'values': [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0]},
            {'name': 'InSize', 'type': 'float', 'count': 2, 'values': [1.0, 1.0]},
            {'name': 'OutSize', 'type': 'float', 'count': 2, 'values': [1.0, 1.0]},
            {'name': 'BlurDir', 'type': 'float', 'count': 2, 'values': [1.0, 1.0]},
            {'name': 'Radius', 'type': 'float', 'count': 1, 'values': [5.0]}]})
    full = os.path.join(ASSETS, 'shaders', 'program', 'blur.fsh')
    with open(full, 'w', encoding='utf-8', newline=chr(10)) as fh:
        fh.write(BLUR_FSH)


BLUR_FSH = """#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;

out vec4 fragColor;

// Размытие вдоль одной оси: ближние точки весят больше дальних. Два прохода (по X и по Y) дают мягкое пятно.
void main() {
    vec3 blurred = vec3(0.0);
    float total = 0.0;
    for (float r = -Radius; r <= Radius; r += 1.0) {
        float strength = 1.0 - abs(r) / (Radius + 1.0);
        blurred += texture(DiffuseSampler, texCoord + oneTexel * r * BlurDir).rgb * strength;
        total += strength;
    }
    fragColor = vec4(blurred / total, 1.0);
}
"""


def tubes():
    """Трубы: тонкие отводы 2x2 пикселя к соседям и узел посередине (BlockTubeRenderer оригинала)."""
    # отводы доходят до самой середины блока: у прямого отрезка узла нет, и половинки должны смыкаться
    sides = {'down': ([7, 0, 7], [9, 8, 9]), 'up': ([7, 8, 7], [9, 16, 9]), 'north': ([7, 7, 0], [9, 9, 8]),
             'south': ([7, 7, 8], [9, 9, 16]), 'west': ([0, 7, 7], [8, 9, 9]), 'east': ([8, 7, 7], [16, 9, 9])}
    faces = ['down', 'up', 'north', 'south', 'west', 'east']

    def cube(lo, hi, texture):
        return {'from': [lo, lo, lo], 'to': [hi, hi, hi], 'faces': {f: {'texture': texture} for f in faces}}

    for side, (start, end) in sides.items():
        for texture in ('pipe_1', 'pipe_restrict'):
            write(ASSETS, 'models/block/tube_arm_%s_%s.json' % (texture, side), {
                'parent': 'minecraft:block/block', 'textures': {'all': tc('block/' + texture), 'particle': tc('block/' + texture)},
                'elements': [{'from': start, 'to': end, 'faces': {f: {'texture': '#all'} for f in faces if f != side}}]})
    # узлы: обычный 4x4, фильтр с ажурной оболочкой и сердцевиной, сужение, клапан с кольцом, вентиль с рукоятью
    joints = {
        '': [cube(6, 10, '#a')],
        '_valve': [cube(6, 10, '#a')],  # рукоять рисует TubeRenderer: она вращается
        '_filter': [cube(5.5, 10.5, '#b'), cube(6.5, 9.5, '#c')],
        '_restrict': [cube(6, 10, '#d')],
        '_oneway': [cube(6, 10, '#a'), cube(5.5, 10.5, '#e')],
        '_buffer': [cube(4, 12, '#f')],
    }
    textures = {'particle': tc('block/pipe_2'), 'a': tc('block/pipe_2'), 'b': tc('block/pipe_valve'),
                'c': tc('block/pipe_filter_core'), 'd': tc('block/pipe_restrict'), 'e': tc('block/pipe_oneway'),
                'f': tc('block/pipe_buffer')}
    textures_filter = dict(textures, b=tc('block/pipe_filter'))
    for suffix, elements in joints.items():
        name = 'tube' + suffix
        model = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                 'textures': textures_filter if suffix == '_filter' else textures, 'elements': elements}
        write(ASSETS, 'models/block/%s_joint.json' % name, model)
        arm = 'pipe_restrict' if suffix == '_restrict' else 'pipe_1'
        multipart = []
        # у особых труб узел есть всегда, у обычной — кроме прямого отрезка
        joint = {'model': tc('block/%s_joint' % name)}
        multipart.append({'when': {'joint': 'true'}, 'apply': joint} if suffix == '' else {'apply': joint})
        for side in sides:
            multipart.append({'when': {side: 'true'}, 'apply': {'model': tc('block/tube_arm_%s_%s' % (arm, side))}})
        write(ASSETS, 'blockstates/%s.json' % name, {'multipart': multipart})
        # в инвентаре — узел с двумя отводами
        item = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
                'textures': dict(model['textures'], arm=tc('block/' + arm)),
                'elements': elements + [
                    {'from': sides['west'][0], 'to': sides['west'][1], 'faces': {f: {'texture': '#arm'} for f in faces}},
                    {'from': sides['east'][0], 'to': sides['east'][1], 'faces': {f: {'texture': '#arm'} for f in faces}}]}
        write(ASSETS, 'models/item/%s.json' % name, item)
        drop_self(name)
        tag('blocks', 'minecraft:mineable/pickaxe', tc(name))

    # ярлык: чистый и со значком аспекта
    write(ASSETS, 'models/item/label.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('item/label')},
        'overrides': [{'predicate': {tc('filled'): 1}, 'model': tc('item/label_filled')}]})
    write(ASSETS, 'models/item/label_filled.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': tc('item/label'), 'layer1': tc('item/label_over')}})

    # рецепты оригинала
    write(DATA, 'thaumcraft/recipes/label.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'tag': 'forge:dyes/black'}, {'tag': 'forge:slimeballs'}] + [{'item': 'minecraft:paper'}] * 4,
        'result': {'item': tc('label'), 'count': 4}})
    write(DATA, 'thaumcraft/recipes/label_aspect.json', {'type': tc('label'), 'category': 'misc'})
    write(DATA, 'thaumcraft/recipes/label_clear.json', {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': tc('label')}],
        'result': {'item': tc('label')}})
    arcane('tube', [' Q ', 'IGI', ' B '],
           {'I': {'tag': 'forge:ingots/iron'}, 'B': {'tag': 'forge:nuggets/gold'}, 'G': {'tag': 'forge:glass'},
            'Q': {'tag': 'forge:nuggets/quicksilver'}}, 'tube', {'aqua': 5, 'ordo': 5}, 'TUBES', 8)

    def arcane_shapeless(name, extra, aspects, research):
        write(DATA, 'thaumcraft/recipes/%s.json' % name, {
            'type': tc('arcane_shapeless'), 'ingredients': [{'item': tc('tube')}, extra],
            'result': {'item': tc(name)}, 'aspects': aspects, 'research': research})
    arcane_shapeless('tube_valve', {'item': 'minecraft:lever'}, {'aqua': 5, 'ordo': 5}, 'TUBES')
    arcane_shapeless('tube_filter', {'item': tc('filter')}, {'aqua': 5, 'ordo': 16}, 'TUBEFILTER')
    arcane_shapeless('tube_restrict', {'tag': 'forge:stone'}, {'aqua': 5, 'terra': 16}, 'TUBEFILTER')
    arcane_shapeless('tube_oneway', {'tag': 'forge:dyes/blue'}, {'aqua': 5, 'ordo': 8, 'perditio': 8}, 'TUBEFILTER')


def machinery():
    """Мехи, центрифуга, алхимическая конструкция и буфер эссенции."""
    simple_block('alchemical_construct', 'alchemyblock')
    drop_self('alchemical_construct')

    # --- тауматорий: модель OBJ из оригинала высотой в два блока, рисуется нижней половиной ---
    write(ASSETS, 'models/block/thaumatorium.json', {
        'loader': 'forge:obj', 'model': tc('models/block/thaumatorium.obj'), 'flip_v': True,
        'render_type': 'minecraft:cutout', 'textures': {'particle': tc('block/thaumatorium')}})
    write(ASSETS, 'models/block/thaumatorium_top.json', {'textures': {'particle': tc('block/thaumatorium')}})
    variants = {}
    for facing, turn in (('west', 0), ('north', 90), ('east', 180), ('south', 270)):
        variants['facing=%s,top=false' % facing] = {'model': tc('block/thaumatorium'), 'y': turn}
        variants['facing=%s,top=true' % facing] = {'model': tc('block/thaumatorium_top')}
    write(ASSETS, 'blockstates/thaumatorium.json', {'variants': variants})
    # обе половины, сломавшись, снова становятся алхимической конструкцией
    write(DATA, 'thaumcraft/loot_tables/blocks/thaumatorium.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': tc('alchemical_construct')}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    # --- мнемоническая матрица: ящик с выводом в сторону того, к чему приставлена ---
    def brain_box(x0, y0, z0, x1, y1, z1):
        return {'from': [x0, y0, z0], 'to': [x1, y1, z1],
                'faces': {f: {'texture': '#all'} for f in ('down', 'up', 'north', 'south', 'west', 'east')}}
    write(ASSETS, 'models/block/mnemonic_matrix.json', {
        'parent': 'minecraft:block/block', 'textures': {'all': tc('block/brainbox'), 'particle': tc('block/brainbox')},
        'elements': [brain_box(3, 3, 3, 13, 13, 13), brain_box(6, 0, 6, 10, 3, 10)]})
    write(ASSETS, 'blockstates/mnemonic_matrix.json', {'variants': {
        'facing=down': {'model': tc('block/mnemonic_matrix')},
        'facing=up': {'model': tc('block/mnemonic_matrix'), 'x': 180},
        'facing=south': {'model': tc('block/mnemonic_matrix'), 'x': 90},
        'facing=north': {'model': tc('block/mnemonic_matrix'), 'x': 90, 'y': 180},
        'facing=west': {'model': tc('block/mnemonic_matrix'), 'x': 90, 'y': 90},
        'facing=east': {'model': tc('block/mnemonic_matrix'), 'x': 90, 'y': 270}}})
    write(ASSETS, 'models/item/mnemonic_matrix.json', {'parent': tc('block/mnemonic_matrix')})
    drop_self('mnemonic_matrix')
    tag('blocks', 'minecraft:mineable/pickaxe', tc('thaumatorium'), tc('mnemonic_matrix'))
    # мехи и центрифугу в мире рисуют их рендереры (они движутся); эти модели — для предмета и частиц
    write(ASSETS, 'models/block/bellows.json', box_model('bellows', 128, 64, [
        (0, 0, -6, 2, -6, 12, 2, 12), (0, 0, -6, 7, -6, 12, 2, 12), (0, 0, -6, 12, -6, 12, 2, 12),
        (0, 36, -2, 6, 6, 4, 4, 2)]))
    write(ASSETS, 'models/block/centrifuge.json', box_model('centrifuge', 64, 32, [
        (20, 16, -4, 0, -4, 8, 4, 8), (20, 16, -4, 12, -4, 8, 4, 8), (0, 0, -1.5, 4, -1.5, 3, 8, 3),
        (16, 0, -4, 7, -1, 8, 2, 2), (0, 16, 4, 5, -2, 4, 6, 4), (0, 16, -8, 5, -2, 4, 6, 4)]))
    for name in ('bellows', 'centrifuge'):
        write(ASSETS, 'blockstates/%s.json' % name, {'variants': {'': {'model': tc('block/' + name)}}})
        write(ASSETS, 'models/item/%s.json' % name, {'parent': tc('block/' + name)})
        drop_self(name)
    tag('blocks', 'minecraft:mineable/pickaxe', tc('alchemical_construct'), tc('centrifuge'), tc('crystallizer'))

    # кристаллизатор: корпус и кристаллы — модели оригинала, их ставит и вращает CrystallizerRenderer
    for name in ('crystalizer', 'vis_relay_crystal'):
        write(ASSETS, 'models/block/%s.json' % name, {
            'loader': 'forge:obj', 'model': tc('models/block/%s.obj' % name), 'flip_v': True,
            'textures': {'particle': tc('block/crystalizer')}})
    write(ASSETS, 'models/block/crystallizer.json', {'textures': {'particle': tc('block/crystalizer')}})
    write(ASSETS, 'blockstates/crystallizer.json', {'variants': {'': {'model': tc('block/crystallizer')}}})
    # предмет: тот же корпус, поставленный в середину клетки стоймя
    write(ASSETS, 'models/item/crystallizer.json', {
        'loader': 'forge:obj', 'model': tc('models/block/crystalizer.obj'), 'flip_v': True,
        'textures': {'particle': tc('block/crystalizer')},
        # загрузчик obj применяет преобразование вокруг центра блока, поэтому сдвиг задан с поправкой на это
        'transform': {'translation': [0.5, 0.0, -0.5], 'rotation': [-90, 0, 0], 'origin': 'corner'},
        'display': {
            'gui': {'rotation': [30, 225, 0], 'scale': [0.8, 0.8, 0.8]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
            'fixed': {'scale': [0.6, 0.6, 0.6]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.45, 0.45, 0.45]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.45, 0.45, 0.45]}}})
    drop_self('crystallizer')
    simple_item('crystal_essence', 'crystalessence')
    tag('blocks', 'minecraft:mineable/axe', tc('bellows'))

    planks = {'tag': 'minecraft:planks'}
    arcane('bellows', ['WW ', 'LCI', 'WW '],
           {'W': planks, 'C': {'item': tc('shard_air')}, 'I': {'tag': 'forge:ingots/iron'}, 'L': {'tag': 'forge:leather'}},
           'bellows', {'aer': 10, 'ordo': 5}, 'BELLOWS')
    arcane('alchemical_construct', ['VTF', 'TWT', 'FTV'],
           {'W': {'item': tc('greatwood_planks')}, 'V': {'item': tc('tube_valve')}, 'T': {'item': tc('tube')},
            'F': {'item': tc('filter')}}, 'alchemical_construct', {'aqua': 5, 'ordo': 5}, 'DISTILESSENTIA')
    arcane('tube_buffer', ['PVP', 'T T', 'PRP'],
           {'T': {'item': tc('tube')}, 'V': {'item': tc('tube_valve')}, 'R': {'item': tc('tube_restrict')},
            'P': {'item': tc('phial')}}, 'tube_buffer', {'aqua': 5, 'ordo': 5}, 'CENTRIFUGE')
    arcane('crystallizer', ['IDI', 'QCQ', 'WTW'],
           {'T': {'item': tc('tube')}, 'D': {'item': 'minecraft:dispenser'}, 'Q': {'item': tc('shard_balanced')},
            'I': {'tag': 'forge:ingots/iron'}, 'W': planks, 'C': {'item': tc('alchemical_construct')}},
           'crystallizer', {'aqua': 5, 'terra': 15, 'ordo': 5}, 'ESSENTIACRYSTAL')
    arcane('centrifuge', [' T ', 'ACP', ' T '],
           {'T': {'item': tc('tube')}, 'P': {'item': 'minecraft:piston'}, 'A': {'item': tc('alembic')},
            'C': {'item': tc('alchemical_construct')}}, 'centrifuge', {'aqua': 5, 'ordo': 5, 'perditio': 5}, 'CENTRIFUGE')


def main():
    tubes()
    machinery()
    warp()
    essentia()
    infusion()
    flux()
    alchemy()
    research_table()
    simple_item('thaumonomicon', 'thaumonomicon')
    wands()
    workbench()
    trees()
    nodes()
    simple_block('cinnabar_ore', 'cinnibar')
    simple_block('amber_ore', 'amberore')
    for name in INFUSED:
        infused_stone(name)
    simple_block('deepslate_cinnabar_ore', 'deepslate_cinnabar')
    simple_block('deepslate_amber_ore', 'deepslate_amber')
    for name in INFUSED:
        infused_stone('deepslate_' + name, 'minecraft:block/deepslate')
    simple_item('amber', 'amber')
    simple_item('quicksilver', 'quicksilver')
    simple_item('quicksilver_nugget', 'nuggetquicksilver')

    raw_ore_drop('cinnabar_ore', 'raw_cinnabar')
    simple_item('raw_cinnabar', 'raw_cinnabar')
    smelting('quicksilver_raw', 'raw_cinnabar', 'quicksilver', 1.0)
    tag('items', 'forge:raw_materials/cinnabar', tc('raw_cinnabar'))
    tag('items', 'forge:raw_materials', tc('raw_cinnabar'))
    ore_drop('amber_ore', 'amber', 1, 1)
    for element, name in zip(ELEMENTS, INFUSED):
        ore_drop(name, 'shard_' + element, 1, 2)

    raw_ore_drop('deepslate_cinnabar_ore', 'raw_cinnabar')
    ore_drop('deepslate_amber_ore', 'amber', 1, 1)
    for element, name in zip(ELEMENTS, INFUSED):
        ore_drop('deepslate_' + name, 'shard_' + element, 1, 2)
    smelting('quicksilver_deepslate', 'deepslate_cinnabar_ore', 'quicksilver', 1.0)
    smelting('amber_deepslate', 'deepslate_amber_ore', 'amber', 1.0)
    smelting('quicksilver', 'cinnabar_ore', 'quicksilver', 1.0)
    smelting('amber', 'amber_ore', 'amber', 1.0)
    compress('quicksilver', 'quicksilver_nugget', 'quicksilver')

    ores = ['cinnabar_ore', 'amber_ore'] + INFUSED
    ores += ['deepslate_' + name for name in ores]
    tag('blocks', 'minecraft:mineable/pickaxe', *map(tc, ores))
    # в оригинале киноварь и янтарь требуют железную кирку, заряженный камень — любую
    tag('blocks', 'minecraft:needs_iron_tool', tc('cinnabar_ore'), tc('amber_ore'),
        tc('deepslate_cinnabar_ore'), tc('deepslate_amber_ore'))
    for registry in ('blocks', 'items'):
        tag(registry, 'forge:ores', *map(tc, ores))
        tag(registry, 'forge:ores/cinnabar', tc('cinnabar_ore'), tc('deepslate_cinnabar_ore'))
        tag(registry, 'forge:ores/amber', tc('amber_ore'), tc('deepslate_amber_ore'))
        for element, name in zip(ELEMENTS, INFUSED):
            tag(registry, 'forge:ores/infused_' + element, tc(name), tc('deepslate_' + name))
        tag(registry, 'forge:ores_in_ground/stone', *[tc(o) for o in ores if not o.startswith('deepslate_')])
        tag(registry, 'forge:ores_in_ground/deepslate', *[tc(o) for o in ores if o.startswith('deepslate_')])
    tag('items', 'forge:gems/amber', tc('amber'))
    tag('items', 'forge:gems', tc('amber'))
    tag('items', 'forge:nuggets/quicksilver', tc('quicksilver_nugget'))
    tag('items', 'forge:nuggets', tc('quicksilver_nugget'))

    worldgen()

    for (registry, namespace, path), values in tags.items():
        write(DATA, '%s/tags/%s/%s.json' % (namespace, registry, path), {'replace': False, 'values': values})
    print('готово, тегов:', len(tags))


if __name__ == '__main__':
    main()

"""Строит data/thaumcraft/thaumcraft_aspects/modern.json: аспекты предметов, появившихся после 1.7.10.

Запуск из корня проекта:  python tools/gen_modern_aspects.py

В оригинале этих предметов не было, значения подобраны в его духе. Здесь только "сырьё":
всё, что из него крафтится, получает аспекты автоматически из рецептов.
Файл назван modern, чтобы грузиться раньше vanilla.json: при совпадении побеждают записи оригинала.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'thaumcraft', 'thaumcraft_aspects', 'modern.json')


def a(text):
    return {k: int(v) for k, v in (part.split(':') for part in text.split())}


ITEMS = {
    # металлы и минералы
    'raw_iron': 'metallum:3 terra:1',
    'raw_gold': 'metallum:2 lucrum:1 terra:1',
    'raw_copper': 'metallum:2 permutatio:1 terra:1',
    # сырая киноварь: как руда оригинала без камня
    'thaumcraft:raw_cinnabar': 'metallum:3 venenum:1 permutatio:2',
    'ancient_debris': 'terra:1 metallum:3 ignis:2 tenebrae:2',
    'netherite_scrap': 'metallum:3 ignis:2 tenebrae:1',
    'amethyst_shard': 'vitreus:3 sensus:1',
    'budding_amethyst': 'vitreus:6 terra:2 victus:2',
    'small_amethyst_bud': 'vitreus:1',
    'medium_amethyst_bud': 'vitreus:2',
    'large_amethyst_bud': 'vitreus:3',
    'amethyst_cluster': 'vitreus:4 sensus:1 lux:1',
    'echo_shard': 'tenebrae:2 sensus:4 spiritus:2 vacuos:2',
    'prismarine_shard': 'aqua:1 vitreus:1',
    'prismarine_crystals': 'aqua:1 vitreus:1 lux:1',

    # камень
    'calcite': 'terra:2 vitreus:1',
    'dripstone_block': 'terra:2 aqua:1',
    'pointed_dripstone': 'terra:1 aqua:1',
    'mud': 'terra:1 aqua:1',
    'basalt': 'terra:2 ignis:1',
    'smooth_basalt': 'terra:2 ignis:1',
    'blackstone': 'terra:2 tenebrae:1',
    'gilded_blackstone': 'terra:2 tenebrae:1 lucrum:2',
    'crying_obsidian': 'terra:2 ignis:2 tenebrae:1 aqua:1',
    'chiseled_deepslate': 'terra:1 ordo:1',
    'cracked_deepslate_bricks': 'terra:1 perditio:1',
    'cracked_deepslate_tiles': 'terra:1 perditio:1',
    'cracked_polished_blackstone_bricks': 'terra:1 tenebrae:1 perditio:1',
    'reinforced_deepslate': 'terra:8 tenebrae:4 alienis:4',
    'end_stone_bricks': 'terra:1 tenebrae:1',
    'purpur_block': 'terra:1 alienis:1',
    'dirt_path': 'terra:2',
    'suspicious_gravel': 'terra:2',
    'bone_block': 'mortuus:6 corpus:3',

    # растения
    'crimson_nylium': 'terra:1 herba:1 ignis:1',
    'warped_nylium': 'terra:1 herba:1 alienis:1',
    'crimson_fungus': 'herba:1 tenebrae:1 ignis:1',
    'warped_fungus': 'herba:1 tenebrae:1 alienis:1',
    'crimson_roots': 'herba:1 ignis:1',
    'warped_roots': 'herba:1 alienis:1',
    'nether_sprouts': 'herba:1 alienis:1',
    'weeping_vines': 'herba:1 ignis:1',
    'twisting_vines': 'herba:1 alienis:1',
    'warped_wart_block': 'herba:2 alienis:1',
    'shroomlight': 'lux:4 herba:1',
    'mushroom_stem': 'herba:2 tenebrae:1',
    'seagrass': 'herba:1 aqua:1',
    'kelp': 'herba:1 aqua:1',
    'dried_kelp': 'herba:1 fames:1',
    'sea_pickle': 'herba:1 aqua:1 lux:1',
    'moss_block': 'herba:3 terra:1',
    'glow_lichen': 'herba:1 lux:1',
    'hanging_roots': 'herba:1',
    'big_dripleaf': 'herba:2 aqua:1',
    'small_dripleaf': 'herba:1 aqua:1',
    'spore_blossom': 'herba:2 aer:1 sensus:1',
    'pink_petals': 'herba:1 sensus:1',
    'pitcher_plant': 'herba:2 victus:1 sensus:1',
    'pitcher_pod': 'herba:1',
    'torchflower_seeds': 'herba:1',
    'mangrove_roots': 'arbor:2 aqua:1',
    'bamboo': 'herba:1 arbor:1',
    'chorus_fruit': 'herba:1 alienis:2 iter:1',
    'popped_chorus_fruit': 'herba:1 alienis:1 ignis:1',
    'chorus_plant': 'herba:2 alienis:2',
    'chorus_flower': 'herba:2 alienis:2 victus:1',
    'sweet_berries': 'herba:1 fames:1',
    'glow_berries': 'herba:1 fames:1 lux:1',
    'beetroot': 'messis:1 fames:1',
    'beetroot_seeds': 'herba:1',
    'sugar': 'herba:1 fames:1',

    # море
    'sponge': 'vacuos:2 bestia:1',
    'wet_sponge': 'aqua:3 vacuos:1 bestia:1',
    'nautilus_shell': 'aqua:2 tutamen:1 vitreus:1',
    'heart_of_the_sea': 'aqua:8 praecantatio:4 lucrum:4',
    'trident': 'telum:5 aqua:4 metallum:4',
    'scute': 'bestia:1 tutamen:2 aqua:1',
    'turtle_egg': 'bestia:1 victus:2 aqua:1',
    'frogspawn': 'limus:1 victus:1 aqua:1',

    # существа и добыча
    'rabbit': 'corpus:2 victus:1 bestia:1',
    'cooked_rabbit': 'fabrico:1 corpus:3 fames:3',
    'mutton': 'corpus:3 victus:1 bestia:1',
    'cooked_mutton': 'fabrico:1 corpus:3 fames:3',
    'rabbit_foot': 'bestia:2 motus:2 praecantatio:1',
    'rabbit_hide': 'bestia:1 pannus:1',
    'glow_ink_sac': 'sensus:1 lux:1',
    'honeycomb': 'limus:1 bestia:1 ordo:1',
    'honey_bottle': 'limus:2 fames:2 sano:1',
    'bee_nest': 'bestia:2 limus:2 arbor:2',
    'phantom_membrane': 'volatus:2 exanimis:2 tenebrae:1',
    'shulker_shell': 'alienis:2 tutamen:2 bestia:1',
    'dragon_breath': 'alienis:4 ignis:2 aer:2 praecantatio:2',
    'dragon_head': 'mortuus:4 spiritus:4 alienis:4',
    'piglin_head': 'mortuus:4 spiritus:4 lucrum:4',
    'sniffer_egg': 'bestia:2 victus:2 terra:1',
    'goat_horn': 'bestia:2 sensus:2 aer:2',
    'totem_of_undying': 'sano:8 victus:8 praecantatio:4 lucrum:2',
    'elytra': 'volatus:8 motus:4 alienis:4 pannus:2',
    'experience_bottle': 'cognitio:5 praecantatio:1 vitreus:1',

    # скалк
    'sculk': 'tenebrae:2 spiritus:1',
    'sculk_vein': 'tenebrae:1',
    'sculk_catalyst': 'tenebrae:4 spiritus:4 alienis:2',
    'sculk_shrieker': 'tenebrae:4 spiritus:2 sensus:4 alienis:2',
    'sculk_sensor': 'tenebrae:2 sensus:4 machina:2',

    # прочее
    'end_rod': 'lux:3 alienis:1',
    'soul_torch': 'lux:1 spiritus:1',
    'candle': 'lux:2 limus:1',
    'bell': 'metallum:8 lucrum:4 sensus:4 aer:2',
    'disc_fragment_5': 'sensus:1 tenebrae:1',
    'ochre_froglight': 'lux:6 limus:1',
    'verdant_froglight': 'lux:6 limus:1',
    'pearlescent_froglight': 'lux:6 limus:1',
}

# "как у X, но с добавкой"
BASED = {
    'exposed_copper': ('copper_block', ''),
    'weathered_copper': ('copper_block', ''),
    'oxidized_copper': ('copper_block', ''),
    'powder_snow_bucket': ('bucket', 'gelum:4'),
    'cod_bucket': ('water_bucket', 'corpus:2 victus:1'),
    'salmon_bucket': ('water_bucket', 'corpus:2 victus:1'),
    'pufferfish_bucket': ('water_bucket', 'corpus:2 victus:1'),
    'tropical_fish_bucket': ('water_bucket', 'corpus:2 victus:1'),
    'axolotl_bucket': ('water_bucket', 'bestia:2 victus:1'),
    'tadpole_bucket': ('water_bucket', 'bestia:1 victus:1'),
    # незеритовые вещи делаются в кузнечном столе из алмазных
    'netherite_sword': ('diamond_sword', 'metallum:4 ignis:1'),
    'netherite_shovel': ('diamond_shovel', 'metallum:4 ignis:1'),
    'netherite_pickaxe': ('diamond_pickaxe', 'metallum:4 ignis:1'),
    'netherite_axe': ('diamond_axe', 'metallum:4 ignis:1'),
    'netherite_hoe': ('diamond_hoe', 'metallum:4 ignis:1'),
    'netherite_helmet': ('diamond_helmet', 'metallum:4 ignis:1'),
    'netherite_chestplate': ('diamond_chestplate', 'metallum:4 ignis:1'),
    'netherite_leggings': ('diamond_leggings', 'metallum:4 ignis:1'),
    'netherite_boots': ('diamond_boots', 'metallum:4 ignis:1'),
}

TAGS = {
    'forge:glass_panes': 'vitreus:1',
}

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
          'purple', 'blue', 'brown', 'green', 'red', 'black']
CORALS = ['tube', 'brain', 'bubble', 'fire', 'horn']


def main():
    entries = []
    for tag, aspects in TAGS.items():
        entries.append({'tag': tag, 'aspects': a(aspects)})
    for name, aspects in ITEMS.items():
        entries.append({'item': name if ':' in name else 'minecraft:' + name, 'aspects': a(aspects)})
    for name, (base, aspects) in BASED.items():
        entries.append({'item': 'minecraft:' + name, 'base': 'minecraft:' + base, 'aspects': a(aspects)})
    for color in COLORS:
        entries.append({'item': 'minecraft:%s_concrete' % color, 'aspects': a('terra:2 aqua:1 ordo:1 sensus:1')})
        # перекраска шалкерового ящика — особый рецепт без списка ингредиентов
        entries.append({'item': 'minecraft:%s_shulker_box' % color, 'base': 'minecraft:shulker_box',
                        'aspects': a('sensus:1')})
    for coral in CORALS:
        entries.append({'item': 'minecraft:%s_coral_block' % coral, 'aspects': a('aqua:2 victus:2 terra:1')})
        entries.append({'item': 'minecraft:dead_%s_coral_block' % coral, 'aspects': a('terra:2 mortuus:1')})
        for kind in ('coral', 'coral_fan'):
            entries.append({'item': 'minecraft:%s_%s' % (coral, kind), 'aspects': a('aqua:1 victus:1 herba:1')})
            entries.append({'item': 'minecraft:dead_%s_%s' % (coral, kind), 'aspects': a('terra:1 mortuus:1')})

    lines = ',\n'.join('    ' + json.dumps(e) for e in entries)
    with open(OUT, 'w', encoding='utf-8') as fh:
        fh.write('{\n  "entries": [\n' + lines + '\n  ]\n}\n')
    print('записей:', len(entries))


if __name__ == '__main__':
    main()

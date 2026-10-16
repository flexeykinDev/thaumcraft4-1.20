#!/usr/bin/env python3
"""Рецепты зачарования наполнением из ConfigRecipes оригинала: общий разбор для gen_data и gen_research."""
import os
import re

import gen_research

SOURCE = os.path.join(os.path.dirname(gen_research.SOURCE), 'ConfigRecipes.java')

# поле Enchantment оригинала -> id зачарования в 1.20.1
ENCHANTMENTS = {
    'protection': 'minecraft:protection', 'fireProtection': 'minecraft:fire_protection',
    'blastProtection': 'minecraft:blast_protection', 'projectileProtection': 'minecraft:projectile_protection',
    'featherFalling': 'minecraft:feather_falling', 'respiration': 'minecraft:respiration',
    'aquaAffinity': 'minecraft:aqua_affinity', 'thorns': 'minecraft:thorns', 'sharpness': 'minecraft:sharpness',
    'smite': 'minecraft:smite', 'baneOfArthropods': 'minecraft:bane_of_arthropods', 'knockback': 'minecraft:knockback',
    'fireAspect': 'minecraft:fire_aspect', 'looting': 'minecraft:looting', 'efficiency': 'minecraft:efficiency',
    'silkTouch': 'minecraft:silk_touch', 'unbreaking': 'minecraft:unbreaking', 'fortune': 'minecraft:fortune',
    'power': 'minecraft:power', 'punch': 'minecraft:punch', 'flame': 'minecraft:flame', 'infinity': 'minecraft:infinity',
    'enchantRepair': 'thaumcraft:repair', 'enchantHaste': 'thaumcraft:haste',
}
# предметы, которые с 1.7.10 переименованы
RENAMES = {'minecraft:reeds': 'minecraft:sugar_cane', 'minecraft:deadbush': 'minecraft:dead_bush',
           'minecraft:web': 'minecraft:cobweb'}


def recipes():
    """Список словарей: legacy (имя в оригинале), name (имя файла рецепта), enchantment, instability, aspects, components."""
    text = open(SOURCE, encoding='utf-8').read()
    out = []
    head = 'ThaumcraftApi.addInfusionEnchantmentRecipe('
    for m in re.finditer(r'recipes\.put\("(InfEnch\w+)", ' + re.escape(head), text):
        start = m.end()
        depth, i = 1, start
        while depth:
            depth += (text[i] == '(') - (text[i] == ')')
            i += 1
        args = gen_research.split_args(text[start:i - 1])
        field = re.search(r'(\w+)\]?$', args[1].strip()).group(1)
        enchantment = ENCHANTMENTS[field]
        aspects = {gen_research.LATIN[a]: int(n) for a, n in re.findall(r'add\(Aspect\.(\w+), (\d+)\)', args[3])}
        listing = args[4].strip()
        components = []
        for arg in gen_research.split_args(listing[listing.index('{') + 1:listing.rindex('}')]):
            found = gen_research.item(arg.strip())
            if 'item' not in found:
                raise SystemExit('зачарование %s: нет предмета для %s' % (m.group(1), found.get('legacy')))
            components.append({'item': RENAMES.get(found['item'], found['item'])})
        out.append({'legacy': m.group(1), 'name': 'enchant_' + enchantment.split(':')[1], 'enchantment': enchantment,
                    'research': args[0].strip().strip('"'), 'instability': int(args[2]), 'aspects': aspects,
                    'components': components})
    return out


if __name__ == '__main__':
    for recipe in recipes():
        print(recipe)

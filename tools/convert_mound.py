"""Переводит WorldGenMound.java оригинала (2448 вызовов setBlock) в список блоков для MoundFeature.

Запуск из корня проекта:  python tools/convert_mound.py
Сундук, ловушка с динамитом, рассадники и урны ставит сам MoundFeature — здесь они пропускаются.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, 'reference', 'tc4', 'src', 'thaumcraft', 'common', 'lib', 'world', 'WorldGenMound.java')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'thaumcraft', 'thaumcraft_structures', 'mound.txt')

STAIRS = {0: 'east', 1: 'west', 2: 'south', 3: 'north'}
LADDER = {2: 'north', 3: 'south', 4: 'west', 5: 'east'}
SKIP = {'chest', 'trapped_chest', 'tnt', 'mob_spawner'}


def state(block, meta):
    if block == 'stone_stairs':
        return 'minecraft:cobblestone_stairs[facing=%s,half=%s]' % (STAIRS[meta & 3], 'top' if meta & 4 else 'bottom')
    if block == 'ladder':
        return 'minecraft:ladder[facing=%s]' % LADDER[meta]
    if block == 'stonebrick':
        return 'minecraft:' + ['stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks'][meta]
    if block == 'tallgrass':
        return 'minecraft:grass'
    if block == 'grass':
        return 'minecraft:grass_block'
    return 'minecraft:' + block


def main():
    text = open(SOURCE, encoding='utf-8').read()
    pattern = re.compile(r'setBlock\(i \+ (\d+), j ([+-]) (\d+), k \+ (\d+), Blocks\.(\w+)(?:, (\d+), \d+)?\)')
    rows = []
    for m in pattern.finditer(text):
        block = m.group(5)
        if block in SKIP:
            continue
        y = int(m.group(3)) * (1 if m.group(2) == '+' else -1)
        rows.append('%s %d %s %s' % (m.group(1), y, m.group(4), state(block, int(m.group(6) or 0))))
    with open(OUT, 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(rows) + '\n')
    print('блоков:', len(rows))


if __name__ == '__main__':
    main()

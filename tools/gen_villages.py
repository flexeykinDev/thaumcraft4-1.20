"""Строит шаблоны деревенских построек из ComponentWizardTower и ComponentBankerHome оригинала.

Запуск из корня проекта:  python tools/gen_villages.py

Для каждого вида деревни (равнины, пустыня, саванна, тайга, снежная) — башня мага и дом менялы
(две разновидности: высокий и низкий). Как в оригинале у пустынной деревни, булыжник, доски и брёвна
заменяются местными блоками. Перед дверью — блок-соединитель с улицей (jigsaw), внутри — житель своей профессии
и его рабочее место (в 1.20 профессию держит рабочее место): у мага — магический верстак, у менялы — стол.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'thaumcraft', 'structures', 'village')
DATA_VERSION = 3465

# булыжник, доски, бревно, ступени — по видам деревень
BIOMES = {
    'plains': ('minecraft:cobblestone', 'minecraft:oak_planks', 'minecraft:oak_log', 'minecraft:cobblestone_stairs', 'minecraft:oak_door',
               'minecraft:oak_trapdoor'),
    'desert': ('minecraft:sandstone', 'minecraft:smooth_sandstone', 'minecraft:sandstone', 'minecraft:sandstone_stairs',
               'minecraft:oak_door', 'minecraft:oak_trapdoor'),
    'savanna': ('minecraft:cobblestone', 'minecraft:acacia_planks', 'minecraft:acacia_log', 'minecraft:cobblestone_stairs',
                'minecraft:acacia_door', 'minecraft:acacia_trapdoor'),
    'taiga': ('minecraft:cobblestone', 'minecraft:spruce_planks', 'minecraft:spruce_log', 'minecraft:cobblestone_stairs',
              'minecraft:spruce_door', 'minecraft:spruce_trapdoor'),
    'snowy': ('minecraft:cobblestone', 'minecraft:spruce_planks', 'minecraft:spruce_log', 'minecraft:cobblestone_stairs',
              'minecraft:spruce_door', 'minecraft:spruce_trapdoor'),
}


class Template:
    def __init__(self, size):
        self.size = size
        self.blocks = {}
        self.extra = {}
        self.entities = []

    def set(self, x, y, z, state, data=None):
        self.blocks[(x, y, z)] = state
        if data is not None:
            self.extra[(x, y, z)] = data
        else:
            self.extra.pop((x, y, z), None)

    def fill(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.set(x, y, z, state)

    def villager(self, x, y, z, profession, biome):
        data = {'id': ('string', 'minecraft:villager'),
                'VillagerData': ('compound', {'profession': ('string', 'thaumcraft:' + profession), 'level': ('int', 1),
                                              'type': ('string', 'minecraft:' + biome)})}
        self.entities.append({'pos': ('list', ('double', [x + 0.5, float(y), z + 0.5])),
                              'blockPos': ('list', ('int', [x, y, z])), 'nbt': ('compound', data)})

    def save(self, path):
        palette = []
        index = {}
        blocks = []
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            state = self.blocks[pos]
            if state not in index:
                index[state] = len(palette)
                palette.append(state)
            entry = {'pos': ('list', ('int', list(pos))), 'state': ('int', index[state])}
            if pos in self.extra:
                entry['nbt'] = ('compound', self.extra[pos])
            blocks.append(entry)
        value = {
            'DataVersion': ('int', DATA_VERSION),
            'size': ('list', ('int', list(self.size))),
            'palette': ('list', ('compound', [parse_state(s) for s in palette])),
            'blocks': ('list', ('compound', blocks)),
            'entities': ('list', ('compound', self.entities)),
        }
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbt.save(path, value)


def parse_state(state):
    if '[' not in state:
        return {'Name': ('string', state)}
    name, props = state[:-1].split('[')
    return {'Name': ('string', name),
            'Properties': ('compound', {k: ('string', v) for k, v in (p.split('=') for p in props.split(','))})}


def entrance(t, x, z, step, facing_out):
    """Соединитель с улицей перед дверью; после постройки на его месте остаётся ступенька."""
    t.set(x, 0, z, 'minecraft:jigsaw[orientation=%s_up]' % facing_out, {
        'id': ('string', 'minecraft:jigsaw'), 'name': ('string', 'minecraft:building_entrance'),
        'target': ('string', 'minecraft:building_entrance'), 'pool': ('string', 'minecraft:empty'),
        'joint': ('string', 'aligned'),
        'final_state': ('string', '%s[facing=%s,half=bottom,shape=straight,waterlogged=false]' % (step, opposite(facing_out)))})


def opposite(side):
    return {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}[side]


def door(t, x, y, z, door_block, facing):
    t.set(x, y, z, '%s[facing=%s,half=lower,hinge=left,open=false,powered=false]' % (door_block, facing))
    t.set(x, y + 1, z, '%s[facing=%s,half=upper,hinge=left,open=false,powered=false]' % (door_block, facing))


def wizard_tower(biome):
    """ComponentWizardTower: каменная башня 5×5 в три этажа с лестницей, люком на крышу и сундуком мага."""
    cobble, planks, _, stairs, door_block, trapdoor = BIOMES[biome]
    t = Template((6, 12, 6))
    t.fill(2, 1, 2, 4, 11, 4, 'minecraft:air')
    t.fill(2, 0, 2, 4, 0, 4, planks)
    t.fill(2, 5, 2, 4, 5, 4, planks)
    t.fill(2, 10, 2, 4, 10, 4, planks)
    t.fill(1, 0, 2, 1, 11, 4, cobble)
    t.fill(2, 0, 1, 4, 11, 1, cobble)
    t.fill(5, 0, 2, 5, 11, 4, cobble)
    t.fill(2, 0, 5, 4, 11, 5, cobble)
    for y in (0, 5, 10):
        for x, z in ((1, 1), (1, 5), (5, 1), (5, 5)):
            t.set(x, y, z, cobble)
    pane = 'minecraft:glass_pane[east=true,north=false,south=false,waterlogged=false,west=true]'
    for x, y, z in ((3, 7, 1), (3, 8, 1), (3, 7, 5), (3, 8, 5), (3, 2, 5), (3, 3, 5)):
        t.set(x, y, z, pane)
    for y in range(1, 10):
        t.set(4, y, 3, 'minecraft:ladder[facing=west,waterlogged=false]')
    t.set(4, 10, 3, '%s[facing=west,half=top,open=false,powered=false,waterlogged=false]' % trapdoor)
    t.set(3, 5, 3, 'minecraft:glowstone')
    t.set(2, 6, 2, 'minecraft:chest[facing=south,type=single,waterlogged=false]', {
        'id': ('string', 'minecraft:chest'), 'LootTable': ('string', 'thaumcraft:chests/wizard_tower')})
    door(t, 3, 1, 1, door_block, 'south')
    entrance(t, 3, 0, stairs, 'north')
    # рабочее место мага (в оригинале профессию не нужно было поддерживать)
    t.set(2, 1, 4, 'thaumcraft:arcane_workbench')
    t.villager(3, 1, 3, 'thaumaturge', biome)
    return t


def banker_home(biome, tall, table):
    """ComponentBankerHome: бревенчатый домик 4×5 с зарешёченными окнами и прилавком менялы."""
    cobble, planks, log, stairs, door_block, _ = BIOMES[biome]
    t = Template((4, 6, 6))
    z0 = 1  # в оригинале ступенька стоит на z = -1, здесь всё сдвинуто на клетку
    t.fill(1, 1, 1 + z0, 3, 5, 4 + z0, 'minecraft:air')
    t.fill(0, 0, 0 + z0, 3, 0, 4 + z0, cobble)
    t.fill(1, 0, 1 + z0, 2, 0, 3 + z0, 'minecraft:dirt')
    roof = 4 if tall else 5
    t.fill(1, roof, 1 + z0, 2, roof, 3 + z0, log)
    for x, z in ((1, 0), (2, 0), (1, 4), (2, 4)):
        t.set(x, 4, z + z0, log + '[axis=y]' if log.endswith('_log') else log)
    for x, z in ((0, 1), (0, 2), (0, 3), (3, 1), (3, 2), (3, 3)):
        t.set(x, 4, z + z0, log + '[axis=y]' if log.endswith('_log') else log)
    for x, z in ((0, 0), (3, 0), (0, 4), (3, 4)):
        t.fill(x, 1, z + z0, x, 3, z + z0, log + '[axis=y]' if log.endswith('_log') else log)
    t.fill(0, 1, 1 + z0, 0, 3, 3 + z0, planks)
    t.fill(3, 1, 1 + z0, 3, 3, 3 + z0, planks)
    t.fill(1, 1, 0 + z0, 2, 3, 0 + z0, planks)
    t.fill(1, 1, 4 + z0, 2, 3, 4 + z0, planks)
    bars_ns = 'minecraft:iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]'
    t.set(0, 2, 2 + z0, bars_ns)
    t.set(3, 2, 2 + z0, bars_ns)
    # прилавок менялы: в оригинале стенка с нажимной плитой, в 1.20 — стол, его рабочее место
    t.set(table, 1, 3 + z0, 'thaumcraft:table')
    door(t, 1, 1, 0 + z0, door_block, 'south')
    entrance(t, 1, 0, stairs, 'north')
    t.villager(1, 1, 2 + z0, 'moneychanger', biome)
    return t


def main():
    count = 0
    for biome in BIOMES:
        wizard_tower(biome).save(os.path.join(OUT, biome, 'wizard_tower.nbt'))
        banker_home(biome, True, 1).save(os.path.join(OUT, biome, 'banker_home_1.nbt'))
        banker_home(biome, False, 2).save(os.path.join(OUT, biome, 'banker_home_2.nbt'))
        count += 3
    print('шаблонов:', count)


if __name__ == '__main__':
    main()

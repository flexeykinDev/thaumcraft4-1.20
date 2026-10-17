"""Достижения-обучение Thaumcraft: ведут игрока от первого жезла до Запредельных земель и подсказывают,
что делать дальше (то, чего в оригинале не было: там всё приходилось вычитывать в Таумономиконе).

Пишет data/thaumcraft/advancements/*.json и тексты: tools/lang/extra_en_us.json, extra_ru_ru.json и
tools/lang/uk/part10.json (их подхватывает import_assets.py).
Достижения с триггером minecraft:impossible выдаёт код (thaumcraft.common.ModAdvancements).

Запуск: python tools/gen_advancements.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'thaumcraft', 'advancements')
LANG = os.path.join(ROOT, 'tools', 'lang')


def has(item):
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ['thaumcraft:' + item]}]}}


def kill(entity):
    return {'trigger': 'minecraft:player_killed_entity',
            'conditions': {'entity': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                                       'predicate': {'type': 'thaumcraft:' + entity}}]}}


CODE = {'trigger': 'minecraft:impossible'}
WAND = {'item': 'thaumcraft:wand', 'nbt': '{rod:"wood",cap:"iron"}'}

# (id, родитель, значок, рамка, критерий, скрыто, {язык: (название, описание)})
ADVANCEMENTS = [
    ('root', None, {'item': 'thaumcraft:thaumonomicon'}, 'task', {'trigger': 'minecraft:tick'}, False, {
        'en_us': ('Thaumcraft', 'Magic hides all around you. The path begins with a humble wand.'),
        'ru_ru': ('Thaumcraft', 'Магия скрыта повсюду. Путь начинается со скромного жезла.'),
        'uk_ua': ('Thaumcraft', 'Магія прихована всюди. Шлях починається зі скромного жезла.')}),
    ('wand', 'root', WAND, 'task', has('wand'), False, {
        'en_us': ('A Humble Wand', 'Make iron wand caps from nuggets, then a wand: cap, stick and cap in a column'),
        'ru_ru': ('Скромный жезл', 'Сделайте железные наконечники из самородков, затем жезл: наконечник, палка и наконечник столбиком'),
        'uk_ua': ('Скромний жезл', 'Зробіть залізні наконечники із самородків, а тоді жезл: наконечник, палиця й наконечник стовпчиком')}),
    ('thaumonomicon', 'wand', {'item': 'thaumcraft:thaumonomicon'}, 'task', has('thaumonomicon'), False, {
        'en_us': ('Book of Secrets', 'Right-click a bookshelf with your wand to get the Thaumonomicon'),
        'ru_ru': ('Книга тайн', 'Нажмите ПКМ жезлом по книжной полке — получите Таумономикон'),
        'uk_ua': ('Книга таємниць', 'Натисніть ПКМ жезлом по книжковій полиці — отримаєте Таумономікон')}),
    ('thaumometer', 'thaumonomicon', {'item': 'thaumcraft:thaumometer'}, 'task', has('thaumometer'), False, {
        'en_us': ('Measure of Magic', 'Craft a Thaumometer: its recipe is on the first pages of the Thaumonomicon'),
        'ru_ru': ('Мера магии', 'Сделайте таумометр: рецепт на первых страницах Таумономикона'),
        'uk_ua': ('Міра магії', 'Зробіть таумометр: рецепт на перших сторінках Таумономікона')}),
    ('scan', 'thaumometer', {'item': 'thaumcraft:thaumometer'}, 'task', CODE, False, {
        'en_us': ('What Is It Made Of?', 'Hold right-click with the Thaumometer on a block, item or creature to learn its aspects'),
        'ru_ru': ('Из чего это сделано?', 'Зажмите ПКМ с таумометром, глядя на блок, предмет или существо, — узнаете его аспекты'),
        'uk_ua': ('З чого це зроблено?', 'Затисніть ПКМ із таумометром, дивлячись на блок, предмет чи істоту, — дізнаєтеся його аспекти')}),
    ('research_table', 'scan', {'item': 'thaumcraft:scribing_tools'}, 'task', CODE, False, {
        'en_us': ('A Place to Think', 'Put two Thaumcraft tables side by side and right-click them with Scribing Tools'),
        'ru_ru': ('Место для раздумий', 'Поставьте рядом два стола Thaumcraft и нажмите по ним ПКМ чернильницей с пером'),
        'uk_ua': ('Місце для роздумів', 'Поставте поруч два столи Thaumcraft і натисніть по них ПКМ каламарем із пером')}),
    ('notes', 'research_table', {'item': 'thaumcraft:research_notes'}, 'task', has('research_notes'), False, {
        'en_us': ('Taking Notes', 'With Scribing Tools and paper in your inventory, click an available research in the Thaumonomicon'),
        'ru_ru': ('Записи', 'С чернильницей и бумагой в инвентаре щёлкните в Таумономиконе по доступному исследованию'),
        'uk_ua': ('Нотатки', 'З каламарем і папером в інвентарі клацніть у Таумономіконі по доступному дослідженню')}),
    ('discovery', 'notes', {'item': 'thaumcraft:thaumonomicon'}, 'goal', CODE, False, {
        'en_us': ('Eureka!', 'Link all aspects on the notes at the Research Table, then right-click the finished notes'),
        'ru_ru': ('Эврика!', 'Соедините все аспекты в записях на столе исследований и нажмите ПКМ готовыми записями'),
        'uk_ua': ('Еврика!', 'Поєднайте всі аспекти в нотатках на дослідницькому столі й натисніть ПКМ готовими нотатками')}),
    ('arcane_workbench', 'thaumonomicon', {'item': 'thaumcraft:table'}, 'task', CODE, False, {
        'en_us': ('Arcane Crafting', 'Right-click a Thaumcraft table with your wand to make an Arcane Workbench'),
        'ru_ru': ('Магический верстак', 'Нажмите ПКМ жезлом по столу Thaumcraft — он станет магическим верстаком'),
        'uk_ua': ('Чародійний верстак', 'Натисніть ПКМ жезлом по столу Thaumcraft — він стане чародійним верстаком')}),
    ('arcane_craft', 'arcane_workbench', {'item': 'thaumcraft:goggles'}, 'task', CODE, False, {
        'en_us': ('Paid in Vis', 'Craft something on the Arcane Workbench; the wand in its slot pays the vis'),
        'ru_ru': ('Плата висом', 'Создайте предмет на магическом верстаке: вис платит жезл в его ячейке'),
        'uk_ua': ('Плата вісом', 'Створіть предмет на чародійному верстаку: віс платить жезл у його комірці')}),
    ('node', 'wand', {'item': 'thaumcraft:aura_node'}, 'task', CODE, False, {
        'en_us': ('Drawing Power', 'Find an aura node and hold right-click on it with your wand to draw vis'),
        'ru_ru': ('Черпая силу', 'Найдите узел ауры и зажмите ПКМ жезлом, глядя на него, — жезл наберёт вис'),
        'uk_ua': ('Черпаючи силу', 'Знайдіть вузол аури й затисніть ПКМ жезлом, дивлячись на нього, — жезл набере віс')}),
    ('crucible', 'thaumonomicon', {'item': 'thaumcraft:crucible'}, 'task', CODE, False, {
        'en_us': ('Bubble, Bubble', 'Right-click a cauldron with your wand to turn it into a Crucible'),
        'ru_ru': ('Буль-буль', 'Нажмите ПКМ жезлом по котлу — он станет тиглем'),
        'uk_ua': ('Буль-буль', 'Натисніть ПКМ жезлом по казану — він стане тиглем')}),
    ('alchemy', 'crucible', {'item': 'thaumcraft:nitor'}, 'task', CODE, False, {
        'en_us': ('First Transmutation', 'Fill the Crucible with water, heat it from below, dissolve items and throw in a catalyst'),
        'ru_ru': ('Первое превращение', 'Налейте в тигель воды, нагрейте снизу, растворите предметы и бросьте катализатор'),
        'uk_ua': ('Перше перетворення', 'Налийте в тигель води, нагрійте знизу, розчиніть предмети й киньте каталізатор')}),
    ('thaumium', 'alchemy', {'item': 'thaumcraft:thaumium_ingot'}, 'task', has('thaumium_ingot'), False, {
        'en_us': ('Thaumium', 'Brew the magical metal in the Crucible'),
        'ru_ru': ('Таумий', 'Сварите магический металл в тигле'),
        'uk_ua': ('Таумій', 'Зваріть магічний метал у тиглі')}),
    ('infusion', 'arcane_craft', {'item': 'thaumcraft:infusion_matrix'}, 'goal', CODE, False, {
        'en_us': ('Infused', 'Complete an infusion ritual on the Runic Matrix altar'),
        'ru_ru': ('Наполнение', 'Завершите ритуал наполнения на алтаре с рунической матрицей'),
        'uk_ua': ('Насичення', 'Завершіть ритуал насичення на вівтарі з рунічною матрицею')}),
    ('golem', 'discovery', {'item': 'thaumcraft:golem_straw'}, 'task', CODE, False, {
        'en_us': ('Helping Hands', 'Place a golem with a core: it will work for you'),
        'ru_ru': ('Помощники', 'Поставьте голема с сердцем — он будет работать на вас'),
        'uk_ua': ('Помічники', 'Поставте голема з ядром — він працюватиме на вас')}),
    ('warp', 'discovery', {'item': 'thaumcraft:sanity_checker'}, 'task', CODE, True, {
        'en_us': ('Something Is Wrong', 'Gain permanent warp. Forbidden knowledge has a price'),
        'ru_ru': ('Что-то не так', 'Получите постоянное искажение. У запретных знаний есть цена'),
        'uk_ua': ('Щось не так', 'Отримайте постійне викривлення. Заборонені знання мають ціну')}),
    ('tainted_land', 'root', {'item': 'thaumcraft:bottle_taint'}, 'task',
     {'trigger': 'minecraft:location', 'conditions': {'player': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                                                                    'predicate': {'location': {'biome': 'thaumcraft:tainted_land'}}}]}}, False, {
        'en_us': ('Where Magic Rots', 'Find a Tainted Land. Keep your distance from what lives there'),
        'ru_ru': ('Где гниёт магия', 'Найдите порченые земли. Держитесь подальше от тех, кто там живёт'),
        'uk_ua': ('Де гниє магія', 'Знайдіть осквернені землі. Тримайтеся подалі від тих, хто там живе')}),
    ('crimson', 'discovery', {'item': 'thaumcraft:crimson_rites'}, 'task', has('crimson_rites'), True, {
        'en_us': ('Crimson Rites', 'Take the book of the Crimson Cult from one of its members'),
        'ru_ru': ('Багровые обряды', 'Заберите книгу Багрового культа у одного из его членов'),
        'uk_ua': ('Багряні обряди', 'Заберіть книгу Багряного культу в одного з його членів')}),
    ('oculus', 'crimson', {'item': 'thaumcraft:eldritch_eye'}, 'challenge', CODE, True, {
        'en_us': ('Apertis Oculis', 'Place four Eldritch Eyes on the altar under a sinister node and pay 100 of each primal vis'),
        'ru_ru': ('Apertis Oculis', 'Положите четыре Глаза Древних на алтарь под зловещим узлом и заплатите по 100 первичного виса'),
        'uk_ua': ('Apertis Oculis', 'Покладіть чотири Ока Елдріча на вівтар під зловісним вузлом і заплатіть по 100 первісного віса')}),
    ('outer_lands', 'oculus', {'item': 'thaumcraft:eldritch_eye'}, 'goal',
     {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': 'thaumcraft:outer_lands'}}, True, {
        'en_us': ('Beyond the Veil', 'Step through the portal into the Outer Lands'),
        'ru_ru': ('За гранью', 'Шагните через портал в Запредельные земли'),
        'uk_ua': ('За межею', 'Ступіть крізь портал у Позамежні землі')}),
    ('eldritch_warden', 'outer_lands', {'item': 'thaumcraft:void_metal_ingot'}, 'challenge', kill('eldritch_warden'), True, {
        'en_us': ('Warden No More', 'Defeat the Eldritch Warden'),
        'ru_ru': ('Конец надзора', 'Победите Надзирателя Элдрича'),
        'uk_ua': ('Кінець нагляду', 'Переможіть Наглядача Елдріча')}),
    ('eldritch_golem', 'outer_lands', {'item': 'thaumcraft:primal_charm'}, 'challenge', kill('eldritch_golem'), True, {
        'en_us': ('Unmade', 'Defeat the Eldritch Construct'),
        'ru_ru': ('Разобранный', 'Победите Конструкта Элдрича'),
        'uk_ua': ('Розібраний', 'Переможіть Конструкта Елдріча')}),
    ('cultist_leader', 'outer_lands', {'item': 'thaumcraft:crimson_blade'}, 'challenge', kill('cultist_leader'), True, {
        'en_us': ('Fallen Praetor', 'Defeat the Crimson Praetor'),
        'ru_ru': ('Павший претор', 'Победите Багрового претора'),
        'uk_ua': ('Полеглий претор', 'Переможіть Багряного претора')}),
    ('taintacle_giant', 'outer_lands', {'item': 'thaumcraft:bottle_taint'}, 'challenge', kill('taintacle_giant'), True, {
        'en_us': ('Root of the Rot', 'Defeat the Giant Taintacle'),
        'ru_ru': ('Корень гнили', 'Победите гигантское заражённое щупальце'),
        'uk_ua': ('Корінь гнилі', 'Переможіть гігантське щупальце скверни')}),
]


def main():
    os.makedirs(DATA, exist_ok=True)
    texts = {'en_us': {}, 'ru_ru': {}, 'uk_ua': {}}
    for key, parent, icon, frame, criterion, hidden, lang in ADVANCEMENTS:
        display = {'icon': icon, 'title': {'translate': 'advancements.thaumcraft.%s.title' % key},
                   'description': {'translate': 'advancements.thaumcraft.%s.description' % key},
                   'frame': frame, 'show_toast': key != 'root', 'announce_to_chat': key != 'root', 'hidden': hidden}
        if parent is None:
            display['background'] = 'thaumcraft:textures/block/arcane_stone.png'
        data = {'display': display, 'criteria': {'done': criterion}}
        if parent is not None:
            data = {'parent': 'thaumcraft:' + parent, **data}
        with open(os.path.join(DATA, key + '.json'), 'w', encoding='utf-8') as fh:
            json.dump(data, fh, ensure_ascii=False, indent=2)
        for code, (title, description) in lang.items():
            texts[code]['advancements.thaumcraft.%s.title' % key] = title
            texts[code]['advancements.thaumcraft.%s.description' % key] = description
    for code in ('en_us', 'ru_ru'):
        with open(os.path.join(LANG, 'extra_%s.json' % code), 'w', encoding='utf-8') as fh:
            json.dump(texts[code], fh, ensure_ascii=False, indent=0, sort_keys=True)
    with open(os.path.join(LANG, 'uk', 'part10.json'), 'w', encoding='utf-8') as fh:
        json.dump(texts['uk_ua'], fh, ensure_ascii=False, indent=0, sort_keys=True)
    print('достижений:', len(ADVANCEMENTS))


if __name__ == '__main__':
    main()

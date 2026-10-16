"""Текстуры, которых не было в оригинале: руды Thaumcraft в глубинном сланце.

Запуск из корня проекта:  python tools/gen_textures.py

Цветные вкрапления берутся из оригинальной текстуры руды (всё, что заметно отличается от серого камня)
и накладываются на ванильный глубинный сланец из клиента Minecraft, скачанного ForgeGradle.
"""
import glob
import os
import struct
import zipfile
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCKS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'thaumcraft', 'textures', 'block')


def decode(data):
    """PNG -> (ширина, высота, список строк из пикселей RGBA). Только 8 бит на канал, без чересстрочности."""
    pos = 8
    idat = b''
    palette = transparency = None
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, color = struct.unpack('>IIBB', chunk[:10])
        elif kind == b'PLTE':
            palette = chunk
        elif kind == b'tRNS':
            transparency = chunk
        elif kind == b'IDAT':
            idat += chunk
    if depth != 8:
        raise ValueError('поддерживается только глубина 8 бит')
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    raw = zlib.decompress(idat)
    stride = width * bpp
    rows = []
    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        kind = raw[offset]
        line = bytearray(raw[offset + 1:offset + 1 + stride])
        offset += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = previous[x]
            c = previous[x - bpp] if x >= bpp else 0
            if kind == 1:
                line[x] = (line[x] + a) & 255
            elif kind == 2:
                line[x] = (line[x] + b) & 255
            elif kind == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif kind == 4:
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        previous = line
        pixels = []
        for x in range(width):
            p = line[x * bpp:x * bpp + bpp]
            if color == 6:
                pixels.append(tuple(p))
            elif color == 2:
                pixels.append((p[0], p[1], p[2], 255))
            elif color == 0:
                pixels.append((p[0], p[0], p[0], 255))
            elif color == 4:
                pixels.append((p[0], p[0], p[0], p[1]))
            else:
                i = p[0]
                alpha = transparency[i] if transparency and i < len(transparency) else 255
                pixels.append((palette[i * 3], palette[i * 3 + 1], palette[i * 3 + 2], alpha))
        rows.append(pixels)
    return width, height, rows


def encode(width, height, rows):
    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body) & 0xffffffff)

    raw = b''.join(b'\x00' + bytes(v for pixel in row for v in pixel) for row in rows)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def vanilla(path):
    jars = glob.glob(os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'forge_gradle', 'minecraft_repo',
                                  'versions', '1.20.1', 'client-extra.jar'))
    if not jars:
        raise SystemExit('не найден клиент Minecraft 1.20.1 в кэше Gradle: сначала запустите gradlew build')
    with zipfile.ZipFile(jars[0]) as jar:
        return decode(jar.read('assets/minecraft/textures/' + path))


def deepslate_ore(source, target):
    width, height, ore = decode(open(os.path.join(BLOCKS, source + '.png'), 'rb').read())
    base_width, base_height, base = vanilla('block/deepslate.png')
    if (width, height) != (base_width, base_height):
        raise SystemExit('%s: размер %dx%d не совпадает с ванильным' % (source, width, height))
    rows = []
    kept = 0
    for y in range(height):
        row = []
        for x in range(width):
            r, g, b, a = ore[y][x]
            # серый камень почти не насыщен цветом, вкрапления руды — насыщены
            if a > 128 and max(r, g, b) - min(r, g, b) > 28:
                row.append((r, g, b, 255))
                kept += 1
            else:
                row.append(base[y][x])
        rows.append(row)
    with open(os.path.join(BLOCKS, target + '.png'), 'wb') as fh:
        fh.write(encode(width, height, rows))
    print('%s: пикселей руды %d из %d' % (target, kept, width * height))


def crop(source, u, v, width, height, target):
    _, _, rows = decode(open(source, 'rb').read())
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, 'wb') as fh:
        fh.write(encode(width, height, [row[u:u + width] for row in rows[v:v + height]]))


def recolor(vanilla_path, target, tint):
    """Ванильная текстура, перекрашенная в оттенок: яркость пикселя сохраняется."""
    width, height, rows = vanilla(vanilla_path)
    out = []
    for row in rows:
        line = []
        for r, g, b, a in row:
            light = max(r, g, b) / 255.0
            line.append((min(255, int(tint[0] * light)), min(255, int(tint[1] * light)), min(255, int(tint[2] * light)), a))
        out.append(line)
    with open(target, 'wb') as fh:
        fh.write(encode(width, height, out))


def glow_layer(source, target):
    """Светящийся слой: в оригинале он рисовался поверх с прибавлением яркости, поэтому его фон чёрный.
    Здесь чёрное становится прозрачным, чтобы слой можно было положить обычной накладкой."""
    width, height, rows = decode(open(source, 'rb').read())
    out = [[(r, g, b, 255 if max(r, g, b) > 40 else 0) for r, g, b, a in row] for row in rows]
    with open(target, 'wb') as fh:
        fh.write(encode(width, height, out))


if __name__ == '__main__':
    deepslate_ore('cinnibar', 'deepslate_cinnabar')
    deepslate_ore('amberore', 'deepslate_amber')
    textures = os.path.dirname(BLOCKS)
    # значок флюсовой лихорадки: ячейка (5, 1) листа зелий оригинала
    crop(os.path.join(textures, 'misc', 'potions.png'), 90, 216, 18, 18,
         os.path.join(textures, 'mob_effect', 'vis_exhaustion.png'))
    # значок порченого яда: ячейка (3, 1)
    crop(os.path.join(textures, 'misc', 'potions.png'), 54, 216, 18, 18,
         os.path.join(textures, 'mob_effect', 'taint_poison.png'))
    # значки остальных эффектов: (столбец, строка) на листе зелий оригинала
    for name, column, row in (('thaumarhia', 7, 2), ('unnatural_hunger', 7, 1), ('blurred_vision', 5, 2),
                              ('sun_scorned', 6, 2), ('infectious_vis_exhaustion', 6, 1), ('death_gaze', 4, 2),
                              ('warp_ward', 3, 2)):
        crop(os.path.join(textures, 'misc', 'potions.png'), column * 18, 198 + row * 18, 18, 18,
             os.path.join(textures, 'mob_effect', name + '.png'))
    # сырая киноварь: ванильное сырое железо в цвете киновари
    recolor('item/raw_iron.png', os.path.join(textures, 'item', 'raw_cinnabar.png'), (235, 60, 50))

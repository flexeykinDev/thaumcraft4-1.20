"""Рисует оформление README (docs/*.svg) в тёмном пиксельном стиле: ступенчатые углы, цифры из точек 5×7,
зерно и один лавандовый акцент. GitHub не пускает CSS в .md, поэтому всё оформление — картинки SVG.

Запуск: python tools/gen_readme_art.py
"""
import os
import random
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'docs')
TEXTURES = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'thaumcraft', 'textures')

BG, PANEL, PANEL2 = '#09090b', '#0f0f12', '#131317'
LINE, LINE_HI = '#2a2a31', '#3a3a44'
TEXT, MUTED, DIM = '#ececf1', '#85858f', '#55555e'
ACCENT, ACCENT_DEEP, ACCENT_HI = '#b9a8ff', '#7a68d6', '#d6cbff'
DOT_OFF = 'rgba(255,255,255,.055)'
SANS = "Inter, 'Segoe UI', system-ui, -apple-system, Roboto, Helvetica, Arial, sans-serif"
MONO = "'JetBrains Mono', Consolas, 'SFMono-Regular', ui-monospace, monospace"

FONT = {
    '0': ['01110', '10001', '10011', '10101', '11001', '10001', '01110'],
    '1': ['00100', '01100', '00100', '00100', '00100', '00100', '01110'],
    '2': ['01110', '10001', '00001', '00010', '00100', '01000', '11111'],
    '3': ['11111', '00010', '00100', '00010', '00001', '10001', '01110'],
    '4': ['00010', '00110', '01010', '10010', '11111', '00010', '00010'],
    '5': ['11111', '10000', '11110', '00001', '00001', '10001', '01110'],
    '6': ['00110', '01000', '10000', '11110', '10001', '10001', '01110'],
    '7': ['11111', '00001', '00010', '00100', '01000', '01000', '01000'],
    '8': ['01110', '10001', '10001', '01110', '10001', '10001', '01110'],
    '9': ['01110', '10001', '10001', '01111', '00001', '00010', '01100'],
    '-': ['00000', '00000', '00000', '11111', '00000', '00000', '00000'],
    '.': ['00000', '00000', '00000', '00000', '00000', '01100', '01100'],
    '/': ['00001', '00001', '00010', '00100', '01000', '10000', '10000'],
    'A': ['01110', '10001', '10001', '11111', '10001', '10001', '10001'],
    'C': ['01110', '10001', '10000', '10000', '10000', '10001', '01110'],
    'F': ['11111', '10000', '10000', '11110', '10000', '10000', '10000'],
    'H': ['10001', '10001', '10001', '11111', '10001', '10001', '10001'],
    'M': ['10001', '11011', '10101', '10101', '10001', '10001', '10001'],
    'R': ['11110', '10001', '10001', '11110', '10100', '10010', '10001'],
    'T': ['11111', '00100', '00100', '00100', '00100', '00100', '00100'],
    'U': ['10001', '10001', '10001', '10001', '10001', '10001', '01110'],
}


def notch(x, y, w, h, s):
    """Многоугольник прямоугольника со ступенчатыми «пиксельными» углами (две ступеньки по s)."""
    pts = [(0, 2 * s), (s, 2 * s), (s, s), (2 * s, s), (2 * s, 0),
           (w - 2 * s, 0), (w - 2 * s, s), (w - s, s), (w - s, 2 * s), (w, 2 * s),
           (w, h - 2 * s), (w - s, h - 2 * s), (w - s, h - s), (w - 2 * s, h - s), (w - 2 * s, h),
           (2 * s, h), (2 * s, h - s), (s, h - s), (s, h - 2 * s), (0, h - 2 * s)]
    return ' '.join('%g,%g' % (x + px, y + py) for px, py in pts)


def px_panel(x, y, w, h, s=3, line=LINE, fill=PANEL):
    """Рамка 1px со ступенчатыми углами: снаружи цвет рамки, внутри с отступом 1px — заливка."""
    return ('<polygon points="%s" fill="%s"/><polygon points="%s" fill="%s"/>'
            % (notch(x, y, w, h, s), line, notch(x + 1, y + 1, w - 2, h - 2, s), fill))


def dots(text, x, y, d, g, on=TEXT, gap=None):
    """Строка из точечных глифов 5×7; «выключенные» точки тоже рисуются, еле заметно."""
    gap = d + 1 if gap is None else gap
    out = []
    for ch in text:
        if ch == ' ':
            x += 3 * (d + g)
            continue
        rows = FONT[ch]
        for r, row in enumerate(rows):
            for c, bit in enumerate(row):
                out.append('<rect x="%g" y="%g" width="%g" height="%g" fill="%s"/>'
                           % (x + c * (d + g), y + r * (d + g), d, d, on if bit == '1' else DOT_OFF))
        x += 5 * d + 4 * g + gap
    return ''.join(out), x


def dots_width(text, d, g, gap=None):
    gap = d + 1 if gap is None else gap
    width = 0
    for ch in text:
        width += 3 * (d + g) if ch == ' ' else 5 * d + 4 * g + gap
    return width - gap


def grain(w, h, clips):
    """Зерно только поверх панелей: в промежутках между карточками виден фон страницы."""
    polygons = ''.join('<polygon points="%s"/>' % c for c in clips)
    return ('<defs><filter id="n" x="0" y="0" width="100%%" height="100%%"><feTurbulence type="fractalNoise" baseFrequency=".9" '
            'numOctaves="2" stitchTiles="stitch"/><feColorMatrix values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 0 0 .9 0"/></filter>'
            '<clipPath id="c">%s</clipPath></defs>'
            '<rect width="%d" height="%d" filter="url(#n)" opacity=".11" clip-path="url(#c)" style="mix-blend-mode:overlay"/>'
            % (polygons, w, h))


def dust(w, h, count, seed):
    rnd = random.Random(seed)
    out = []
    for i in range(count):
        out.append('<rect x="%d" y="%d" width="2" height="2" fill="%s"/>'
                   % (rnd.randrange(8, w - 8), rnd.randrange(8, h - 8), '#3b3550' if i % 3 == 0 else '#2c2c33'))
    return ''.join(out)


def marker(x, y, color=ACCENT):
    return '<rect x="%g" y="%g" width="6" height="6" fill="%s"/>' % (x, y, color)


def caps(x, y, text, size=9, color=MUTED, anchor='start', spacing='.18em', weight=600):
    return ('<text x="%g" y="%g" font-family="%s" font-size="%g" font-weight="%d" letter-spacing="%s" fill="%s" text-anchor="%s">%s</text>'
            % (x, y, SANS, size, weight, spacing, color, anchor, esc(text)))


def label(x, y, text, size=13, color=TEXT, weight=400, family=SANS, anchor='start'):
    return ('<text x="%g" y="%g" font-family="%s" font-size="%g" font-weight="%d" fill="%s" text-anchor="%s">%s</text>'
            % (x, y, family, size, weight, color, anchor, esc(text)))


def chip(x, y, text, accent=False):
    w = 14 + len(text) * 6.6
    color = ACCENT if accent else MUTED
    return (px_panel(x, y, w, 22, 2, ACCENT_DEEP if accent else LINE_HI, 'rgba(70,58,120,.35)' if accent else PANEL2)
            + label(x + w / 2, y + 15, text, 11, color, 500, MONO, 'middle')), w


def esc(text):
    return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def svg(w, h, body, title):
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d" role="img" aria-label="%s">'
            '<title>%s</title>%s</svg>\n' % (w, h, w, h, esc(title), esc(title), body))


def read_png(path):
    """Минимальное чтение PNG (8 бит, RGBA/RGB/палитра) без сторонних библиотек."""
    data = open(path, 'rb').read()
    pos, idat, palette, alpha = 8, b'', None, None
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        if kind == b'IHDR':
            w, h, depth, color = struct.unpack('>IIBB', chunk[:10])
        elif kind == b'PLTE':
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif kind == b'tRNS':
            alpha = list(chunk)
        elif kind == b'IDAT':
            idat += chunk
        pos += 12 + length
    assert depth == 8, path
    bpp = {6: 4, 2: 3, 3: 1, 4: 2, 0: 1}[color]
    raw = zlib.decompress(idat)
    stride = w * bpp
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        filt, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if filt == 1:
                line[x] = (line[x] + a) & 255
            elif filt == 2:
                line[x] = (line[x] + b) & 255
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    pixels = []
    for line in rows:
        row = []
        for x in range(w):
            px = line[x * bpp:(x + 1) * bpp]
            if color == 6:
                row.append(tuple(px))
            elif color == 2:
                row.append(tuple(px) + (255,))
            elif color == 3:
                row.append(palette[px[0]] + ((alpha[px[0]] if alpha and px[0] < len(alpha) else 255),))
            elif color == 4:
                row.append((px[0], px[0], px[0], px[1]))
            else:
                row.append((px[0], px[0], px[0], 255))
        pixels.append(row)
    return pixels


def mosaic(path, x, y, cell, gap, seed):
    """Текстура мода мозаикой: каждый пиксель — квадрат, слегка затемнённый наугад, часть пикселей «битая»."""
    rnd = random.Random(seed)
    out = []
    for r, row in enumerate(read_png(path)):
        for c, (red, green, blue, a) in enumerate(row):
            if a < 40 or rnd.random() < 0.04:
                continue
            k = rnd.uniform(0.72, 1.0)
            out.append('<rect x="%d" y="%d" width="%d" height="%d" fill="#%02x%02x%02x"/>'
                       % (x + c * (cell + gap), y + r * (cell + gap), cell, cell, int(red * k), int(green * k), int(blue * k)))
    return ''.join(out)


def banner():
    w, h = 880, 290
    body = [px_panel(0, 0, w, h, 4, LINE, BG), dust(w, h, 28, 7)]
    body.append(caps(36, 44, 'THAUMCRAFT 4  ·  1.7.10 → 1.20.1', 9, DIM))
    c, cw = chip(36, 60, 'UNOFFICIAL PORT')
    body.append(c)
    c, _ = chip(36 + cw + 8, 60, 'FORGE 1.20.1', True)
    body.append(c)
    title, end = dots('THAUMCRAFT', 36, 108, 6, 1, TEXT)
    body.append(title)
    four, _ = dots('4', end + 10, 108, 6, 1, ACCENT)
    body.append(four)
    body.append(label(36, 200, 'Research, alchemy, golems, infusion and the Outer Lands —', 15, MUTED))
    body.append(label(36, 222, 'the classic magic mod, rebuilt for Minecraft 1.20.1.', 15, MUTED))
    x = 36
    for i, word in enumerate(['RESEARCH', 'ALCHEMY', 'ARTIFICE', 'GOLEMANCY', 'ELDRITCH']):
        body.append(marker(x, 252, ACCENT if i == 0 else DIM))
        body.append(caps(x + 12, 258, word, 9, MUTED))
        x += 12 + len(word) * 7.6 + 22
    # справа — Таумономикон мозаикой в своей рамке
    size = 32 * 6 + 31
    fx, fy = w - size - 52, (h - size) // 2 - 4
    body.append(px_panel(fx - 14, fy - 14, size + 28, size + 28, 3, LINE, PANEL))
    body.append(mosaic(os.path.join(TEXTURES, 'item', 'thaumonomicon.png'), fx, fy, 6, 1, 3))
    body.append(grain(w, h, [notch(0, 0, w, h, 4)]))
    return svg(w, h, ''.join(body), 'Thaumcraft 4 — unofficial port to Minecraft 1.20.1')


def ring(cx, cy, r_out, lit, total=44, color=ACCENT):
    out = []
    for k in range(total):
        out.append('<rect x="%g" y="%g" width="3" height="7" fill="%s" transform="rotate(%g %g %g)"/>'
                   % (cx - 1.5, cy - r_out, color if k < lit else '#24242a', 360 / total * k, cx, cy))
    return ''.join(out)


def stats(lang):
    labels = {
        'en': ('AUTOMATED CHECKS', [('201', 'RESEARCH ENTRIES'), ('248', 'ITEMS'), ('186', 'BLOCKS'), ('29', 'CREATURES'), ('23', 'LANGUAGES')]),
        'ru': ('АВТОПРОВЕРОК', [('201', 'ИССЛЕДОВАНИЕ'), ('248', 'ПРЕДМЕТОВ'), ('186', 'БЛОКОВ'), ('29', 'СУЩЕСТВ'), ('23', 'ЯЗЫКА')]),
    }[lang]
    w, h = 880, 168
    body = []
    clips = [notch(0, 0, 168, h, 3)]
    # кольцо: все проверки зелёные — горят все засечки
    body.append(px_panel(0, 0, 168, h, 3))
    body.append(ring(84, 74, 58, 44))
    num = '290'
    d, g = 3, 1
    t, _ = dots(num, 84 - dots_width(num, d, g) / 2, 60, d, g, TEXT)
    body.append(t)
    body.append(caps(84, 104, '/ 290', 8, DIM, 'middle'))
    body.append(caps(84, 150, labels[0], 8, MUTED, 'middle'))
    x, gap = 176, 8
    cw = (w - x - gap * 4) / 5
    for i, (value, text) in enumerate(labels[1]):
        cx = x + i * (cw + gap)
        body.append(px_panel(cx, 0, cw, h, 3))
        clips.append(notch(cx, 0, cw, h, 3))
        body.append(marker(cx + 14, 16, ACCENT if i == 0 else DIM))
        d, g = 5, 1
        t, _ = dots(value, cx + 14, 52, d, g, ACCENT if i == 0 else TEXT)
        body.append(t)
        body.append(caps(cx + 14, 136, text, 8, MUTED))
    body.append(grain(w, h, clips))
    return svg(w, h, ''.join(body), 'Port in numbers')


STEPS = {
    'en': [('INSTALL FORGE', 'Minecraft 1.20.1 + Forge 47.x', 'or NeoForge 1.20.1 — one jar for both'),
           ('DROP IN THE MODS', 'Thaumcraft + Curios + TerraBlender', 'into the .minecraft/mods folder'),
           ('PLAY', 'Start the Forge profile, then', 'right-click a bookshelf with a wand')],
    'ru': [('ПОСТАВЬТЕ FORGE', 'Minecraft 1.20.1 + Forge 47.x', 'или NeoForge 1.20.1 — jar один на оба'),
           ('ПОЛОЖИТЕ МОДЫ', 'Thaumcraft + Curios + TerraBlender', 'в папку .minecraft/mods'),
           ('ИГРАЙТЕ', 'Запустите профиль Forge и нажмите', 'ПКМ жезлом по книжной полке')],
}


def steps(lang):
    w, h, gap = 880, 176, 8
    cw = (w - gap * 2) / 3
    body, clips = [], []
    for i, (title, line1, line2) in enumerate(STEPS[lang]):
        x = i * (cw + gap)
        body.append(px_panel(x, 0, cw, h, 3, ACCENT_DEEP if i == 0 else LINE, PANEL))
        clips.append(notch(x, 0, cw, h, 3))
        t, _ = dots('%02d' % (i + 1), x + 18, 20, 4, 1, ACCENT)
        body.append(t)
        if i < 2:
            body.append(caps(x + cw - 18, 32, '→', 12, DIM, 'end', '0'))
        body.append(marker(x + 18, 92, ACCENT if i == 0 else DIM))
        body.append(label(x + 32, 99, title, 13, TEXT, 700))
        body.append(label(x + 18, 128, line1, 12.5, MUTED))
        body.append(label(x + 18, 148, line2, 12.5, MUTED))
    body.append(grain(w, h, clips))
    return svg(w, h, ''.join(body), 'Install in three steps' if lang == 'en' else 'Установка в три шага')


def main():
    os.makedirs(OUT, exist_ok=True)
    files = {'banner.svg': banner()}
    for lang in ('en', 'ru'):
        files['stats-%s.svg' % lang] = stats(lang)
        files['steps-%s.svg' % lang] = steps(lang)
    for name, content in files.items():
        with open(os.path.join(OUT, name), 'w', encoding='utf-8') as fh:
            fh.write(content)
    print('docs:', len(files), 'svg')


if __name__ == '__main__':
    main()

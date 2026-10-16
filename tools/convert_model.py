"""Переводит модель существа 1.7.10 (наследник ModelBase с полями ModelRenderer) в Java-поля для 1.20.

Запуск из корня проекта:  python tools/convert_model.py ModelEldritchCrab

Печатает объявления деталей вида
    final ModelPart torso = ModelParts.part(u, v, mirror, texW, texH, px, py, pz, rx, ry, rz, x, y, z, w, h, d, grow, ...);
Детали, прикреплённые через addChild, печатаются следом строками parent.addChild(...).
Движение (setRotationAngles) переносится вручную — его формулы у каждой модели свои.
"""
import glob
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import convert_armor as ca  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def camel(name):
    return name[0].lower() + name[1:]


def convert(model):
    path = glob.glob(os.path.join(ROOT, 'reference', '**', model + '.java'), recursive=True)[0]
    text = open(path, encoding='utf-8').read()
    tex = re.search(r'textureWidth = (\d+);.*?textureHeight = (\d+);', text, re.S)
    tw, th = (int(tex.group(1)), int(tex.group(2))) if tex else (64, 32)
    parts, _ = ca.parse(text)
    out = []
    for name, p in parts.items():
        boxes = []
        for box in p['boxes']:
            x, y, z, w, h, d = box[:6]
            grow = box[6] if len(box) > 6 else 0.0
            boxes += [x, y, z, w, h, d, grow]
        args = [p['u'], p['v'], 'true' if p['mirror'] else 'false', '%df' % tw, '%df' % th]
        args += [ca.fmt(v) for v in p['pivot']] + [ca.fmt(v) for v in p['rot']] + [ca.fmt(v) for v in boxes]
        out.append('    final ModelPart %s = ModelParts.part(%s);' % (camel(name), ', '.join(str(a) for a in args)))
    links = re.findall(r'this\.(\w+)\.addChild\(this\.(\w+)\);', text)
    if links:
        out.append('')
        out.append('    {')
        for parent, child in links:
            out.append('        ModelParts.attach(%s, "%s", %s);' % (camel(parent), child.lower(), camel(child)))
        out.append('    }')
    return '\n'.join(out)


if __name__ == '__main__':
    print(convert(sys.argv[1]))

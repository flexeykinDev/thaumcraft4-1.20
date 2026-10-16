"""Переводит модель брони 1.7.10 (наследник ModelBiped с коробками ModelRenderer) в код слоя модели 1.20.

Разбирает конструктор модели: создание частей, коробки, точки вращения, повороты, зеркальность и то, к какой части
тела (голова, туловище, руки, ноги) каждая деталь прикреплена. На выходе — тело метода, который строит MeshDefinition:
детали становятся дочерними частями head/body/right_arm/left_arm/right_leg/left_leg стандартной модели человека.
"""
import re
import sys

PARENTS = {'bipedHead': 'head', 'bipedHeadwear': 'hat', 'bipedBody': 'body', 'bipedRightArm': 'right_arm',
           'bipedLeftArm': 'left_arm', 'bipedRightLeg': 'right_leg', 'bipedLeftLeg': 'left_leg'}


def num(text):
    text = text.strip().replace('(float)', '').replace('(double)', '').replace('Math.PI', '3.141592653589793')
    text = re.sub(r'(\d+(?:\.\d*)?)[FfDd]', lambda m: m.group(1), text)
    return float(eval(text))


def fmt(value):
    return ('%.7f' % value).rstrip('0').rstrip('.') + 'f' if value != int(value) else '%df' % int(value)


def parse(source, expand=None):
    """expand — словарь {имя массива: число}: части, созданные в цикле for(a < N), раскрываются в имя_0, имя_1, ..."""
    parts = {}
    order = []
    children = []
    # раскрыть циклы for (int a = 0; a < N; ++a) { ... this.X[a] ... }
    def unroll(match):
        count = int(match.group(1))
        body = match.group(2)
        out = []
        for a in range(count):
            out.append(re.sub(r'\[a\]', '_%d' % a, body).replace('a *', '%d *' % a).replace('* a', '* %d' % a))
        return '\n'.join(out)
    source = re.sub(r'for\s*\(int a = 0; a < (\d+); \+\+a\)\s*\{(.*?)\n\s*\}', unroll, source, flags=re.S)
    source = re.sub(r'\[(\d)\]', r'_\1', source)
    for line in source.splitlines():
        line = line.strip()
        m = re.match(r'this\.(\w+) = new ModelRenderer\(this, ([^,]+), ([^)]+)\);', line)
        if m:
            name = m.group(1)
            parts[name] = {'u': int(num(m.group(2))), 'v': int(num(m.group(3))), 'mirror': False, 'boxes': [],
                           'pivot': (0.0, 0.0, 0.0), 'rot': (0.0, 0.0, 0.0)}
            order.append(name)
            continue
        m = re.match(r'this\.(\w+)\.mirror = true;', line)
        if m and m.group(1) in parts:
            parts[m.group(1)]['mirror'] = True
            continue
        m = re.match(r'this\.(\w+)\.addBox\((.*)\);', line)
        if m and m.group(1) in parts:
            values = [num(v) for v in m.group(2).split(',')]
            parts[m.group(1)]['boxes'].append(values)
            continue
        m = re.match(r'this\.(\w+)\.setRotationPoint\((.*)\);', line)
        if m and m.group(1) in parts:
            parts[m.group(1)]['pivot'] = tuple(num(v) for v in m.group(2).split(','))
            continue
        m = re.match(r'this\.setRotation\(this\.(\w+), (.*)\);', line)
        if m and m.group(1) in parts:
            parts[m.group(1)]['rot'] = tuple(num(v) for v in m.group(2).split(','))
            continue
        m = re.match(r'this\.(bipedHead|bipedHeadwear|bipedBody|bipedRightArm|bipedLeftArm|bipedRightLeg|bipedLeftLeg)\.addChild\(this\.(\w+)\);', line)
        if m:
            children.append((PARENTS[m.group(1)], m.group(2)))
    return parts, children


def java(parts, children, parent_var='parts'):
    lines = []
    for parent, name in children:
        p = parts[name]
        builder = 'CubeListBuilder.create().texOffs(%d, %d)' % (p['u'], p['v'])
        if p['mirror']:
            builder += '.mirror()'
        for box in p['boxes']:
            x, y, z, w, h, d = box[:6]
            grow = box[6] if len(box) > 6 else 0.0
            builder += '.addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))' % (fmt(x), fmt(y), fmt(z), fmt(w), fmt(h), fmt(d), fmt(grow))
        px, py, pz = p['pivot']
        rx, ry, rz = p['rot']
        lines.append('        %s.get("%s").addOrReplaceChild("%s", %s,\n                PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));'
                     % (parent_var, parent, name.lower(), builder, fmt(px), fmt(py), fmt(pz), fmt(rx), fmt(ry), fmt(rz)))
    return '\n'.join(lines)


if __name__ == '__main__':
    path = sys.argv[1]
    text = open(path, encoding='utf-8').read()
    parts, children = parse(text)
    print(java(parts, children))

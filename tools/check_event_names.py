import os, re, collections
root = 'src/main/java'
seen = collections.defaultdict(list)
for dp, _, fs in os.walk(root):
    for f in fs:
        if not f.endswith('.java'):
            continue
        p = os.path.join(dp, f)
        text = open(p, encoding='utf-8').read()
        pkg = re.search(r'^package ([\w.]+);', text, re.M).group(1)
        # класс, в котором лежит метод: ближайшее объявление class выше
        for m in re.finditer(r'@SubscribeEvent[^\n]*\n\s*public static void (\w+)\(([\w.<>?, ]+?) \w+\)', text):
            before = text[:m.start()]
            cls = re.findall(r'class (\w+)', before)[-1]
            event = m.group(2).split('.')[-1].split('<')[0]
            seen[(pkg, cls, m.group(1), event)].append(p)
for k, v in seen.items():
    if len(v) > 1:
        print(k, v)

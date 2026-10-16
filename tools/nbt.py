"""Мини-библиотека для NBT (сжатые gzip файлы шаблонов построек): чтение и запись.

Значение представлено парой (тип, данные); для удобства есть конструкторы Byte/Int/String/... и функции
load/save. Список хранится как ('list', (тип_элементов, [данные...])).
"""
import gzip
import struct

TYPES = ['end', 'byte', 'short', 'int', 'long', 'float', 'double', 'byte_array', 'string', 'list', 'compound',
         'int_array', 'long_array']


def _read(buf, pos, kind):
    if kind == 'byte':
        return struct.unpack_from('>b', buf, pos)[0], pos + 1
    if kind == 'short':
        return struct.unpack_from('>h', buf, pos)[0], pos + 2
    if kind == 'int':
        return struct.unpack_from('>i', buf, pos)[0], pos + 4
    if kind == 'long':
        return struct.unpack_from('>q', buf, pos)[0], pos + 8
    if kind == 'float':
        return struct.unpack_from('>f', buf, pos)[0], pos + 4
    if kind == 'double':
        return struct.unpack_from('>d', buf, pos)[0], pos + 8
    if kind == 'string':
        n = struct.unpack_from('>H', buf, pos)[0]
        return buf[pos + 2:pos + 2 + n].decode('utf-8'), pos + 2 + n
    if kind in ('byte_array', 'int_array', 'long_array'):
        n = struct.unpack_from('>i', buf, pos)[0]
        pos += 4
        sub = {'byte_array': 'byte', 'int_array': 'int', 'long_array': 'long'}[kind]
        out = []
        for _ in range(n):
            v, pos = _read(buf, pos, sub)
            out.append(v)
        return out, pos
    if kind == 'list':
        sub = TYPES[buf[pos]]
        n = struct.unpack_from('>i', buf, pos + 1)[0]
        pos += 5
        out = []
        for _ in range(n):
            v, pos = _read(buf, pos, sub)
            out.append(v)
        return (sub, out), pos
    if kind == 'compound':
        out = {}
        while True:
            t = TYPES[buf[pos]]
            pos += 1
            if t == 'end':
                return out, pos
            name, pos = _read(buf, pos, 'string')
            v, pos = _read(buf, pos, t)
            out[name] = (t, v)
    raise ValueError(kind)


def load(path):
    buf = gzip.open(path).read()
    assert buf[0] == 10
    _, pos = _read(buf, 1, 'string')
    value, _ = _read(buf, pos, 'compound')
    return value


def _write(out, kind, v):
    if kind == 'byte':
        out += struct.pack('>b', v)
    elif kind == 'short':
        out += struct.pack('>h', v)
    elif kind == 'int':
        out += struct.pack('>i', v)
    elif kind == 'long':
        out += struct.pack('>q', v)
    elif kind == 'float':
        out += struct.pack('>f', v)
    elif kind == 'double':
        out += struct.pack('>d', v)
    elif kind == 'string':
        data = v.encode('utf-8')
        out += struct.pack('>H', len(data)) + data
    elif kind in ('byte_array', 'int_array', 'long_array'):
        sub = {'byte_array': 'byte', 'int_array': 'int', 'long_array': 'long'}[kind]
        out += struct.pack('>i', len(v))
        for x in v:
            _write(out, sub, x)
    elif kind == 'list':
        sub, items = v
        out += bytes([TYPES.index(sub if items else (sub or 'end'))]) + struct.pack('>i', len(items))
        for x in items:
            _write(out, sub, x)
    elif kind == 'compound':
        for name, (t, x) in v.items():
            out += bytes([TYPES.index(t)])
            _write(out, 'string', name)
            _write(out, t, x)
        out += b'\x00'


def save(path, value):
    out = bytearray([10])
    _write(out, 'string', '')
    _write(out, 'compound', value)
    with gzip.open(path, 'wb') as fh:
        fh.write(bytes(out))


def plain(value, kind='compound'):
    """Превращает (тип, данные) в обычные словари и списки — чтобы смотреть глазами."""
    if kind == 'compound':
        return {k: plain(x, t) for k, (t, x) in value.items()}
    if kind == 'list':
        sub, items = value
        return [plain(x, sub) for x in items]
    return value

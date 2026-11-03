#!/usr/bin/env python3
"""Writes the GameTest structure templates (gzip NBT) used by the port's GameTests: empty platforms with a stone floor.

Usage: python3 -I tools/gen_test_structures.py
"""
import gzip
import io
import os
import struct

PORT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def w_str(b, s):
    e = s.encode("utf-8")
    b.write(struct.pack(">H", len(e)))
    b.write(e)


def w_payload(b, t, v):
    if t == TAG_INT:
        b.write(struct.pack(">i", v))
    elif t == TAG_STRING:
        w_str(b, v)
    elif t == TAG_LIST:
        et, items = v
        b.write(struct.pack(">bi", et if items else TAG_END, len(items)))
        for i in items:
            w_payload(b, et, i)
    elif t == TAG_COMPOUND:
        for k, (tt, vv) in v.items():
            b.write(struct.pack(">b", tt))
            w_str(b, k)
            w_payload(b, tt, vv)
        b.write(struct.pack(">b", TAG_END))


def structure(sx, sy, sz):
    blocks = []
    for x in range(sx):
        for z in range(sz):
            blocks.append({"pos": (TAG_LIST, (TAG_INT, [x, 0, z])), "state": (TAG_INT, 0)})
            for y in range(1, sy):
                blocks.append({"pos": (TAG_LIST, (TAG_INT, [x, y, z])), "state": (TAG_INT, 1)})
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": (TAG_LIST, (TAG_INT, [sx, sy, sz])),
        "palette": (TAG_LIST, (TAG_COMPOUND, [{"Name": (TAG_STRING, "minecraft:stone")}, {"Name": (TAG_STRING, "minecraft:air")}])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, blocks)),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
    }
    b = io.BytesIO()
    b.write(struct.pack(">b", TAG_COMPOUND))
    w_str(b, "")
    w_payload(b, TAG_COMPOUND, root)
    return gzip.compress(b.getvalue())


for mod in ("rotarycraft",):
    out = os.path.join(PORT, mod, "src/main/resources/data", mod, "structure")
    os.makedirs(out, exist_ok=True)
    for sx, sy, sz in ((9, 4, 9), (9, 8, 9)):
        with open(os.path.join(out, "platform_%dx%dx%d.nbt" % (sx, sy, sz)), "wb") as f:
            f.write(structure(sx, sy, sz))
    print("written", out)

# ReactorCraft: the same two platforms plus a long one for the 11 block reactor generator (turbine + 9 rotor cores + generator) and a
# 48x12x48 one for the tokamak (ring of radius 14 with the injector frames reaching 20 blocks out)
out = os.path.join(PORT, "reactorcraft", "src/main/resources/data", "reactorcraft", "structure")
os.makedirs(out, exist_ok=True)
for sx, sy, sz in ((9, 4, 9), (9, 8, 9), (16, 8, 9), (48, 12, 48)):
    with open(os.path.join(out, "platform_%dx%dx%d.nbt" % (sx, sy, sz)), "wb") as f:
        f.write(structure(sx, sy, sz))
print("written", out)

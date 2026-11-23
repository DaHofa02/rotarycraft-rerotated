#!/usr/bin/env python3
"""Draws the OWN textures and writes the model, blockstate, lang, recipe and tag files of the rotaryme addon (RotaryCraft ME Kinetics).
Everything is drawn procedurally here. Usage: python3 -I tools/gen_rotaryme_art.py
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "rotaryme/src/main/resources")
A = os.path.join(ROOT, "assets/rotaryme")
D = os.path.join(ROOT, "data")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)


def save(img, rel):
    p = os.path.join(A, "textures", rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def noise(img, box, base, var, seed):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            d = rnd.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))


def border(img, box, col):
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        img.putpixel((x, y0), col + (255,)); img.putpixel((x, y1 - 1), col + (255,))
    for y in range(y0, y1):
        img.putpixel((x0, y), col + (255,)); img.putpixel((x1 - 1, y), col + (255,))


def machine_faces(name, body, glow, seed):
    """side (ME-port look: a lit grid), front (the shaft port: a ring) and top for a kinetic machine."""
    side = Image.new("RGBA", (16, 16))
    noise(side, (0, 0, 16, 16), body, 8, seed)
    border(side, (0, 0, 16, 16), tuple(c // 2 for c in body))
    border(side, (3, 3, 13, 13), (40, 40, 48))
    for i in range(4, 12, 3):
        for k in range(4, 12):
            side.putpixel((i, k), glow + (255,)); side.putpixel((k, i), glow + (255,))
    save(side, "block/%s_side.png" % name)
    front = Image.new("RGBA", (16, 16))
    noise(front, (0, 0, 16, 16), body, 8, seed + 1)
    border(front, (0, 0, 16, 16), tuple(c // 2 for c in body))
    for x in range(16):
        for y in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 3 <= r < 5:
                front.putpixel((x, y), (180, 182, 188, 255))
            elif r < 3:
                front.putpixel((x, y), (30, 30, 34, 255))
    save(front, "block/%s_front.png" % name)
    top = Image.new("RGBA", (16, 16))
    noise(top, (0, 0, 16, 16), tuple(c - 15 for c in body), 6, seed + 2)
    border(top, (0, 0, 16, 16), tuple(c // 2 for c in body))
    border(top, (5, 5, 11, 11), glow)
    save(top, "block/%s_top.png" % name)
    write(os.path.join(A, "blockstates/%s.json" % name), {"variants": {"": {"model": "rotaryme:block/%s" % name}}})
    # orientation lives in the block entity, so the model shows the same faces all round (the shaft port on both ends of the long axis)
    write(os.path.join(A, "models/block/%s.json" % name), {"parent": "minecraft:block/cube", "textures": {
        "particle": "rotaryme:block/%s_side" % name, "north": "rotaryme:block/%s_side" % name, "south": "rotaryme:block/%s_side" % name,
        "east": "rotaryme:block/%s_front" % name, "west": "rotaryme:block/%s_front" % name,
        "up": "rotaryme:block/%s_top" % name, "down": "rotaryme:block/%s_top" % name}})
    write(os.path.join(A, "models/item/%s.json" % name), {"parent": "rotaryme:block/%s" % name})


def item(name, draw, seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw(img, seed)
    save(img, "item/%s.png" % name)
    write(os.path.join(A, "models/item/%s.json" % name), {"parent": "minecraft:item/generated", "textures": {"layer0": "rotaryme:item/%s" % name}})


def energy(img, seed):
    for x in range(16):
        for y in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if r < 6.5:
                a = int(255 * (1 - r / 6.5) ** 0.5)
                img.putpixel((x, y), (255, 170 + int(60 * (1 - r / 6.5)), 60, a))
    for t in range(-4, 5):                                       # a swirl to suggest rotation
        img.putpixel((8 + t, 8 - abs(t) // 2), (255, 245, 220, 255))


machine_faces("kinetic_input", (70, 78, 96), (120, 200, 255), 11)
machine_faces("kinetic_output", (96, 74, 70), (255, 170, 80), 21)
machine_faces("bundled_bus", (80, 84, 92), (230, 60, 60), 27)
item("kinetic_energy", energy, 51)
# The cells and their housing are recoloured from AE2's art by gen_kinetic_cell_art.py (models with AE2's LED as layer1), not drawn here.
TIERS = ["1k", "4k", "16k", "64k", "256k"]

write(os.path.join(A, "lang/en_us.json"), {
    "itemGroup.rotaryme": "RotaryCraft ME Kinetics",
    "block.rotaryme.kinetic_input": "ME Kinetic Input",
    "block.rotaryme.kinetic_output": "ME Kinetic Output",
    "block.rotaryme.bundled_bus": "Bundled Bus",
    "item.rotaryme.kinetic_cell_housing": "Kinetic Cell Housing",
    "item.rotaryme.kinetic_energy": "Kinetic Energy",
    **{"item.rotaryme.kinetic_cell_%s" % t: "%s ME Kinetic Storage Cell" % t.upper() for t in TIERS},
    "key.rotaryme.kinetic": "Kinetic Energy",
    "tooltip.rotaryme.cell_capacity": "Holds up to %s",
    "gui.rotaryme.set": "Set",
    "gui.rotaryme.cards_input": "ME Kinetic Input (1 % less loss each)",
    "gui.rotaryme.cards_output": "ME Kinetic Output (less lubricant)",
    "gui.rotaryme.lube.use": "Lube use: %s mB/s (%s %%)",
    "gui.rotaryme.lube.tank": "Own tank: %s mB",
    "gui.rotaryme.lube.me": "ME: %s mB lubricant",
    "gui.rotaryme.lube.me.empty": "ME: no lubricant stored",
    "gui.rotaryme.lube.me.none": "ME: no network/channel",
    "gui.rotaryme.lube.from.tank": "From: own tank, then ME",
    "gui.rotaryme.lube.from.me": "From: ME network",
    "gui.rotaryme.state.0": "Running (or switched off)",
    "gui.rotaryme.state.1": "No ME network or channel",
    "gui.rotaryme.state.2": "Not enough kinetic energy",
    "gui.rotaryme.state.3": "Not enough lubricant",
    "gui.rotaryme.state.4": "Stopped by redstone",
    "gui.rotaryme.redstone.on": "RS: on",
    "gui.rotaryme.redstone.off": "RS: off",
    "gui.rotaryme.redstone.on.tip": "Runs while it gets a redstone signal (click to invert)",
    "gui.rotaryme.redstone.off.tip": "Runs while it gets no redstone signal (click to invert)",
})


def recipe(name, pattern, key, out, count=1):
    write(os.path.join(D, "rotaryme/recipe/%s.json" % name), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
          "key": {k: {"item": v} for k, v in key.items()}, "result": {"id": out, "count": count}})


def shapeless(name, items, out):
    write(os.path.join(D, "rotaryme/recipe/%s.json" % name), {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": [{"item": i} for i in items], "result": {"id": out, "count": 1}})


B = "rotarycraft:bedingot"
recipe("kinetic_input", ["BqB", "cIc", "BqB"], {"B": B, "q": "ae2:certus_quartz_dust", "c": "rotarycraft:cctv", "I": "ae2:interface"}, "rotaryme:kinetic_input")
recipe("kinetic_output", ["BfB", "cIc", "BfB"], {"B": B, "f": "ae2:formation_core", "c": "rotarycraft:cctv", "I": "ae2:interface"}, "rotaryme:kinetic_output")
recipe("bundled_bus", ["SrS", "cIc", "SPS"], {"S": "rotarycraft:steelingot", "r": "minecraft:redstone_block", "c": "rotarycraft:pcb", "I": "ae2:interface", "P": "rotarycraft:basepanel"}, "rotaryme:bundled_bus")
recipe("kinetic_cell_housing", ["QRQ", "R R", "SSS"], {"Q": "ae2:quartz_glass", "R": "minecraft:redstone", "S": "rotarycraft:steelingot"}, "rotaryme:kinetic_cell_housing")
for t in TIERS:
    shapeless("kinetic_cell_" + t, ["rotaryme:kinetic_cell_housing", "ae2:cell_component_" + t], "rotaryme:kinetic_cell_" + t)
write(os.path.join(D, "minecraft/tags/block/mineable/pickaxe.json"), {"replace": False, "values": ["rotaryme:kinetic_input", "rotaryme:kinetic_output", "rotaryme:bundled_bus"]})
print("rotaryme art written")

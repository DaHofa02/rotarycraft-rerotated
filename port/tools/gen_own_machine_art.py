#!/usr/bin/env python3
"""Draws the port's OWN textures and model files for machines ported without the original's art (2026-11-17 onwards).

Everything here is drawn procedurally (no original pixels). Output:
  rotarycraft/src/main/resources/assets/rotarycraft/textures/machine/own/<name>.png - model textures (UV layout of the client/model/Model*.java)
  rotarycraft/src/main/resources/assets/rotarycraft/textures/block/own/<name>.png   - 16x16 particle textures
  blockstates / models/block / models/item JSON for those machines
Usage: python3 -I tools/gen_own_machine_art.py
"""
import json
import os
import random

from PIL import Image

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "rotarycraft/src/main/resources/assets/rotarycraft")


def metal(img, box, base, var=10, seed=0, brushed=True):
    """Fills box (x0, y0, x1, y1) with a brushed-metal look around the base colour."""
    rnd = random.Random(seed)
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        row = rnd.randint(-var, var) if brushed else 0
        for x in range(x0, x1):
            d = row // 2 + rnd.randint(-var // 2, var // 2)
            img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))


def frame(img, box, col):
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        img.putpixel((x, y0), col + (255,))
        img.putpixel((x, y1 - 1), col + (255,))
    for y in range(y0, y1):
        img.putpixel((x0, y), col + (255,))
        img.putpixel((x1 - 1, y), col + (255,))


def stripes(img, box, col, every=4):
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        for y in range(y0, y1):
            if (x + y) % every == 0:
                img.putpixel((x, y), col + (255,))


def box_uv(u, v, w, h, d):
    """Areas of a ModelPart box at texOffs(u, v): returns the whole footprint."""
    return (u, v, u + 2 * (d + w), v + d + h)


def save(img, rel):
    p = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def advanced_gear(name, housing_col, band_col, seed):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    metal(img, box_uv(0, 0, 16, 2, 16), (70, 72, 78), seed=seed)            # base plate
    frame(img, box_uv(0, 0, 16, 2, 16), (45, 46, 50))
    metal(img, box_uv(0, 42, 10, 10, 10), housing_col, seed=seed + 1)      # worm housing
    metal(img, box_uv(64, 24, 6, 3, 6), housing_col, seed=seed + 2)        # worm cap
    metal(img, box_uv(0, 18, 12, 12, 12), housing_col, seed=seed + 3)      # high-ratio housing
    metal(img, box_uv(64, 36, 13, 2, 13), band_col, var=6, seed=seed + 4)  # bands
    stripes(img, box_uv(64, 36, 13, 2, 13), tuple(c // 2 for c in band_col), 5)
    metal(img, box_uv(64, 0, 16, 2, 2), (190, 192, 196), var=8, seed=seed + 5, brushed=False)  # shaft
    stripes(img, box_uv(64, 0, 16, 2, 2), (120, 122, 126), 6)
    metal(img, box_uv(64, 8, 1, 5, 5), (150, 120, 60), var=8, seed=seed + 6)  # collars
    save(img, "textures/machine/own/%s.png" % name)
    part = Image.new("RGBA", (16, 16))
    metal(part, (0, 0, 16, 16), housing_col, seed=seed + 7)
    frame(part, (0, 0, 16, 16), tuple(c // 2 for c in housing_col))
    save(part, "textures/block/own/%s.png" % name)


ITEM_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}


def rendered_machine(block_id, particle):
    """A machine drawn by its block entity renderer: particle-only block model, builtin/entity item model."""
    for rel, data in (("blockstates/%s.json" % block_id, {"variants": {"": {"model": "rotarycraft:block/%s" % block_id}}}),
                      ("models/block/%s.json" % block_id, {"textures": {"particle": "rotarycraft:block/own/%s" % particle}}),
                      ("models/item/%s.json" % block_id, {"parent": "minecraft:builtin/entity", "display": ITEM_DISPLAY})):
        p = os.path.join(RES, rel)
        with open(p, "w") as f:
            json.dump(data, f, indent=2)


def cube_machine(block_id, side_col, top_col, accent, seed):
    """A full-block machine with a plain cube model: own side and top textures."""
    side = Image.new("RGBA", (16, 16))
    metal(side, (0, 0, 16, 16), side_col, seed=seed)
    frame(side, (0, 0, 16, 16), tuple(c // 2 for c in side_col))
    for x in range(4, 12):          # a slot-like opening with an accent bar
        for y in range(6, 10):
            side.putpixel((x, y), (20, 20, 22, 255) if y in (7, 8) else accent + (255,))
    save(side, "textures/block/own/%s_side.png" % block_id)
    top = Image.new("RGBA", (16, 16))
    metal(top, (0, 0, 16, 16), top_col, seed=seed + 1, brushed=False)
    frame(top, (0, 0, 16, 16), tuple(c // 2 for c in top_col))
    frame(top, (5, 5, 11, 11), accent)
    save(top, "textures/block/own/%s_top.png" % block_id)
    for rel, data in (("blockstates/%s.json" % block_id, {"variants": {"": {"model": "rotarycraft:block/%s" % block_id}}}),
                      ("models/block/%s.json" % block_id, {"parent": "minecraft:block/cube_bottom_top", "textures": {
                          "side": "rotarycraft:block/own/%s_side" % block_id, "top": "rotarycraft:block/own/%s_top" % block_id,
                          "bottom": "rotarycraft:block/own/%s_top" % block_id}}),
                      ("models/item/%s.json" % block_id, {"parent": "rotarycraft:block/%s" % block_id})):
        with open(os.path.join(RES, rel), "w") as f:
            json.dump(data, f, indent=2)


def flat_item(item_id, base, accent, seed):
    """A 16x16 item texture (a framed card with a grid mark) and its generated item model."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    metal(img, (2, 1, 14, 15), base, var=6, seed=seed)
    frame(img, (2, 1, 14, 15), tuple(c // 2 for c in base))
    for i in (5, 8, 11):
        for k in range(4, 13):
            img.putpixel((i, k), accent + (255,))
            img.putpixel((k - 0, i - 0), accent + (255,))
    save(img, "textures/item/own/%s.png" % item_id)
    with open(os.path.join(RES, "models/item/%s.json" % item_id), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "rotarycraft:item/own/%s" % item_id}}, f, indent=2)


flat_item("craft_pattern", (196, 186, 150), (90, 70, 40), 71)
cube_machine("auto_crafter", (110, 112, 120), (130, 104, 70), (200, 170, 60), 67)
cube_machine("item_filter", (120, 124, 132), (96, 100, 108), (70, 160, 90), 61)
advanced_gear("worm_gear", (92, 104, 122), (150, 150, 150), 11)
advanced_gear("high_gear", (58, 60, 66), (210, 120, 40), 23)
advanced_gear("industrial_coil", (150, 92, 48), (205, 140, 70), 37)     # copper windings
advanced_gear("bedrock_coil", (40, 40, 44), (120, 120, 128), 41)
advanced_gear("cvt", (176, 160, 70), (60, 60, 60), 53)                  # yellow housing, dark belt bands
rendered_machine("worm_gear", "worm_gear")
rendered_machine("cvt", "cvt")
rendered_machine("industrial_coil", "industrial_coil")
rendered_machine("bedrock_coil", "bedrock_coil")
rendered_machine("high_gear", "high_gear")
print("own art written")

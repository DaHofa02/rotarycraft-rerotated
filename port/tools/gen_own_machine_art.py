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
cube_machine("drop_processor", (104, 98, 92), (84, 80, 76), (190, 90, 50), 73)
cube_machine("music_box", (120, 84, 56), (100, 70, 48), (230, 210, 90), 79)
cube_machine("terraformer", (92, 116, 84), (120, 96, 64), (90, 170, 220), 97)
cube_machine("item_filter", (120, 124, 132), (96, 100, 108), (70, 160, 90), 61)
advanced_gear("worm_gear", (92, 104, 122), (150, 150, 150), 11)
advanced_gear("high_gear", (58, 60, 66), (210, 120, 40), 23)
advanced_gear("industrial_coil", (150, 92, 48), (205, 140, 70), 37)     # copper windings
advanced_gear("bedrock_coil", (40, 40, 44), (120, 120, 128), 41)
advanced_gear("cvt", (176, 160, 70), (60, 60, 60), 53)                  # yellow housing, dark belt bands
def cctv():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    metal(img, box_uv(0, 0, 6, 1, 6), (60, 62, 68), seed=81)         # base plate
    metal(img, box_uv(0, 8, 2, 5, 2), (90, 92, 98), seed=82)         # post
    metal(img, box_uv(24, 0, 4, 4, 8), (214, 214, 206), seed=83)     # camera body
    metal(img, box_uv(24, 16, 3, 3, 2), (40, 40, 46), seed=84)       # lens hood
    for x in range(26, 29):
        for y in range(18, 21):
            img.putpixel((x, y), (40, 90, 170, 255))                  # glass
    save(img, "textures/machine/own/cctv.png")
    part = Image.new("RGBA", (16, 16))
    metal(part, (0, 0, 16, 16), (214, 214, 206), seed=85)
    save(part, "textures/block/own/cctv.png")


cctv()


def portal_shaft():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    metal(img, box_uv(0, 0, 10, 3, 10), (54, 50, 62), seed=91)       # base
    metal(img, box_uv(0, 14, 8, 9, 8), (70, 64, 84), seed=92)        # bearing block
    rnd = random.Random(93)
    x0, y0, x1, y1 = box_uv(40, 0, 5, 5, 2)                          # glowing collar: purple swirl
    for x in range(x0, x1):
        for y in range(y0, y1):
            v = rnd.randint(0, 60)
            img.putpixel((x, y), (120 + v, 40 + v // 2, 200 + v // 4 if 200 + v // 4 < 256 else 255, 255))
    metal(img, box_uv(0, 32, 2, 2, 16), (180, 180, 186), var=8, seed=94, brushed=False)  # axle
    save(img, "textures/machine/own/portal_shaft.png")
    part = Image.new("RGBA", (16, 16))
    metal(part, (0, 0, 16, 16), (70, 64, 84), seed=95)
    save(part, "textures/block/own/portal_shaft.png")


portal_shaft()
rendered_machine("portal_shaft", "portal_shaft")


def projector():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    metal(img, box_uv(0, 0, 6, 1, 6), (50, 52, 60), seed=101)
    metal(img, box_uv(0, 8, 2, 5, 2), (80, 82, 90), seed=102)
    metal(img, box_uv(24, 0, 4, 4, 8), (60, 70, 110), seed=103)     # body, dark blue
    metal(img, box_uv(24, 16, 3, 3, 2), (30, 30, 34), seed=104)     # lens tube
    for x in range(26, 29):
        for y in range(18, 21):
            img.putpixel((x, y), (250, 240, 190, 255))               # lit lens
    save(img, "textures/machine/own/projector.png")
    part = Image.new("RGBA", (16, 16))
    metal(part, (0, 0, 16, 16), (60, 70, 110), seed=105)
    save(part, "textures/block/own/projector.png")


projector()
rendered_machine("projector", "projector")
rendered_machine("cctv", "cctv")
rendered_machine("worm_gear", "worm_gear")
rendered_machine("cvt", "cvt")
rendered_machine("industrial_coil", "industrial_coil")
rendered_machine("bedrock_coil", "bedrock_coil")
rendered_machine("high_gear", "high_gear")
print("own art written")


def converter(name, housing_col, band_col, terminal_col, seed):
    """Texture for client/model/ModelConverter (magnetostatic engine, dynamo)."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    metal(img, box_uv(0, 0, 16, 2, 16), (70, 72, 78), seed=seed)
    frame(img, box_uv(0, 0, 16, 2, 16), (45, 46, 50))
    metal(img, box_uv(0, 18, 11, 12, 12), housing_col, seed=seed + 1)
    metal(img, box_uv(64, 36, 2, 13, 13), band_col, var=6, seed=seed + 2)
    stripes(img, box_uv(64, 36, 2, 13, 13), tuple(c * 3 // 4 for c in band_col), 3)
    metal(img, box_uv(64, 24, 2, 6, 6), terminal_col, var=6, seed=seed + 3)
    frame(img, box_uv(64, 24, 2, 6, 6), (30, 30, 32))
    metal(img, box_uv(64, 0, 3, 2, 2), (190, 192, 196), var=8, seed=seed + 4, brushed=False)
    metal(img, box_uv(64, 8, 1, 5, 5), (150, 120, 60), var=8, seed=seed + 5)
    save(img, "textures/machine/own/%s.png" % name)
    part = Image.new("RGBA", (16, 16))
    metal(part, (0, 0, 16, 16), housing_col, seed=seed + 6)
    frame(part, (0, 0, 16, 16), tuple(c // 2 for c in housing_col))
    save(part, "textures/block/own/%s.png" % name)


def upgrade_item(item_id, base, accent, pips, seed):
    """A 16x16 circuit-board style upgrade card with 'pips' lit dots and its generated item model."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    metal(img, (1, 3, 15, 13), base, var=6, seed=seed)
    frame(img, (1, 3, 15, 13), tuple(c // 2 for c in base))
    for x in range(3, 13):
        img.putpixel((x, 8), (200, 170, 70, 255))            # a trace across the card
    for i in range(pips):
        x = 3 + i * 2
        img.putpixel((x, 5), accent + (255,))
        img.putpixel((x, 6), accent + (255,))
    for x in range(10, 14):
        for y in range(10, 12):
            img.putpixel((x, y), (30, 30, 34, 255))           # a chip
    save(img, "textures/item/own/%s.png" % item_id)
    with open(os.path.join(RES, "models/item/%s.json" % item_id), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "rotarycraft:item/own/%s" % item_id}}, f, indent=2)


converter("magnetic_engine", (150, 40, 40), (200, 120, 60), (60, 60, 70), 131)
rendered_machine("magnetic_engine", "magnetic_engine")
converter("dynamo", (60, 70, 130), (200, 120, 60), (170, 40, 40), 137)
rendered_machine("dynamo", "dynamo")
for t in range(1, 6):
    upgrade_item("upgrade_magnet_%d" % t, (40, 90, 50), (230, 60, 60), t, 140 + t)
upgrade_item("upgrade_efficiency", (40, 90, 50), (90, 220, 90), 1, 151)
upgrade_item("upgrade_flux", (40, 60, 110), (240, 200, 60), 1, 152)
cube_machine("purifier", (150, 146, 136), (120, 116, 108), (220, 120, 40), 161)
converter("fuel_engine", (90, 90, 96), (60, 60, 64), (200, 160, 40), 171)
rendered_machine("fuel_engine", "fuel_engine")
converter("steam_turbine", (170, 172, 178), (120, 122, 128), (90, 140, 200), 173)
rendered_machine("steam_turbine", "steam_turbine")
cube_machine("distillery", (110, 120, 110), (90, 100, 90), (180, 140, 60), 175)
cube_machine("boiler", (140, 70, 50), (110, 60, 46), (230, 230, 230), 177)
converter("air_compressor", (70, 110, 150), (60, 90, 120), (200, 200, 210), 181)
rendered_machine("air_compressor", "air_compressor")
converter("pneumatic_engine", (90, 130, 160), (60, 90, 120), (230, 200, 60), 183)
rendered_machine("pneumatic_engine", "pneumatic_engine")
cube_machine("force_field", (70, 90, 130), (60, 76, 110), (120, 180, 255), 191)
cube_machine("containment", (120, 70, 80), (100, 60, 66), (255, 90, 110), 193)
cube_machine("air_gun", (130, 136, 140), (110, 114, 118), (180, 220, 240), 195)
cube_machine("landmine", (90, 96, 70), (80, 84, 62), (200, 60, 40), 197)
cube_machine("self_destruct", (140, 40, 40), (110, 30, 30), (250, 220, 60), 201)
cube_machine("sonic_weapon", (80, 80, 96), (64, 64, 80), (160, 120, 240), 203)
cube_machine("emp", (60, 70, 90), (50, 56, 72), (120, 230, 250), 205)
cube_machine("machine_gun", (70, 74, 70), (56, 60, 56), (210, 190, 120), 207)
cube_machine("tnt_cannon", (110, 80, 70), (90, 66, 58), (230, 70, 50), 209)
cube_machine("block_cannon", (100, 100, 110), (84, 84, 92), (140, 200, 120), 211)
cube_machine("railgun", (90, 96, 110), (70, 76, 90), (120, 200, 255), 213)
cube_machine("freeze_gun", (150, 180, 200), (120, 150, 170), (230, 250, 255), 215)
cube_machine("laser_gun", (110, 60, 60), (90, 50, 50), (255, 60, 40), 217)
cube_machine("multi_cannon", (96, 100, 90), (76, 80, 72), (220, 200, 120), 219)
cube_machine("anti_air_gun", (90, 110, 80), (72, 90, 64), (230, 230, 120), 221)
cube_machine("flame_turret", (120, 80, 50), (100, 66, 42), (255, 150, 40), 223)


def bedrock_noise(img, pts, seed):
    """Bedrock look: dark grey with lighter and darker speckles, on the given pixels."""
    rnd = random.Random(seed)
    for (x, y) in pts:
        v = rnd.choice((40, 52, 64, 76, 96))
        img.putpixel((x, y), (v, v, v + 4, 255))


def tool_icon(item_id, head, seed):
    """A 16x16 tool: a wooden handle on the diagonal and a head given as a set of pixels (bedrock speckle)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(3, 12):
        img.putpixel((i, 15 - i), (120, 84, 46, 255))
        img.putpixel((i + 1, 15 - i), (92, 62, 34, 255))
    bedrock_noise(img, head, seed)
    save(img, "textures/item/own/%s.png" % item_id)
    with open(os.path.join(RES, "models/item/%s.json" % item_id), "w") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "rotarycraft:item/own/%s" % item_id}}, f, indent=2)


def armour_icon(item_id, rows, seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pts = [(x, y) for y, (a, b) in rows for x in range(a, b)]
    bedrock_noise(img, pts, seed)
    for (x, y) in pts[:3]:
        img.putpixel((x, y), (150, 150, 160, 255))
    save(img, "textures/item/own/%s.png" % item_id)
    with open(os.path.join(RES, "models/item/%s.json" % item_id), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "rotarycraft:item/own/%s" % item_id}}, f, indent=2)


def armour_layers():
    """Worn armour textures (64x32, the vanilla armour layout), bedrock speckle where the vanilla layers have cover."""
    for layer, boxes in ((1, [(0, 0, 32, 16), (16, 16, 56, 32), (40, 16, 56, 32), (0, 16, 16, 32)]), (2, [(0, 16, 40, 32), (16, 16, 40, 32)])):
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        for (x0, y0, x1, y1) in boxes:
            bedrock_noise(img, [(x, y) for x in range(x0, x1) for y in range(y0, y1)], 300 + layer + x0)
        save(img, "textures/models/armor/bedrock_layer_%d.png" % layer)


pick = [(x, 2) for x in range(4, 13)] + [(3, 3), (13, 3), (2, 4), (14, 4)] + [(x, 3) for x in range(5, 12)]
tool_icon("bedrock_pickaxe", pick, 231)
tool_icon("bedrock_axe", [(x, y) for x in range(9, 14) for y in range(2, 7) if not (x == 9 and y in (2, 6))], 232)
tool_icon("bedrock_shovel", [(x, y) for x in range(10, 14) for y in range(2, 6)] + [(11, 1), (12, 1)], 233)
tool_icon("bedrock_sword", [(i, 13 - i) for i in range(4, 14)] + [(i + 1, 13 - i) for i in range(4, 13)] + [(3, 10), (6, 13)], 234)
tool_icon("bedrock_hoe", [(x, 3) for x in range(8, 14)] + [(8, 4), (9, 4)], 235)
tool_icon("bedrock_scythe", [(x, 2) for x in range(3, 13)] + [(2, 3), (3, 3), (2, 4)] + [(x, 3) for x in range(9, 13)], 236)
tool_icon("bedrock_paxel", pick + [(x, y) for x in range(10, 13) for y in range(4, 7)], 237)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))                     # shears: two blades
bedrock_noise(img, [(x, x) for x in range(3, 10)] + [(x + 1, x) for x in range(3, 9)] + [(12 - x, x) for x in range(3, 10)], 238)
for (x, y) in ((10, 11), (11, 12), (12, 12), (3, 12), (4, 12), (5, 11)):
    img.putpixel((x, y), (170, 40, 40, 255))
save(img, "textures/item/own/bedrock_shears.png")
with open(os.path.join(RES, "models/item/bedrock_shears.json"), "w") as f:
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "rotarycraft:item/own/bedrock_shears"}}, f, indent=2)
armour_icon("bedrock_helmet", [(4, (3, 13)), (5, (3, 13)), (6, (3, 5)), (6, (11, 13)), (7, (3, 5)), (7, (11, 13))], 241)
armour_icon("bedrock_chestplate", [(y, (3, 13)) if y > 4 else (y, (3, 6)) for y in range(2, 14)] + [(y, (10, 13)) for y in range(2, 5)], 242)
armour_icon("bedrock_leggings", [(2, (4, 12)), (3, (4, 12))] + [(y, (4, 7)) for y in range(4, 14)] + [(y, (9, 12)) for y in range(4, 14)], 243)
armour_icon("bedrock_boots", [(y, (3, 6)) for y in range(8, 13)] + [(y, (10, 13)) for y in range(8, 13)] + [(12, (2, 6)), (12, (10, 14))], 244)
armour_layers()
upgrade_item("upgrade_lodestone", (60, 60, 70), (200, 200, 220), 3, 245)

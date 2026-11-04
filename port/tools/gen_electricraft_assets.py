#!/usr/bin/env python3
"""Generates the ElectriCraft resources of the port from the original jar: textures, lang, block states and models.

Wires (original WireRenderer, ISBRH): a core cube of 1/4 block (1/3 insulated) with the wire texture, and an arm to every connected
side with the "_end" texture, as a multipart model on the six connection properties.

Ores (ElectriOres, copper excluded - vanilla) and ingots (ElectriItems.INGOTS, cut from the item sprite sheet, index 16+meta).
Data: loot tables (ores and wires drop themselves), c:/minecraft: tags, worldgen (ore configured/placed features and a
neoforge:add_features biome modifier for the overworld; the placement is electricraft:ore_control, see OreControlPlacement).

Usage: /usr/bin/python3 tools/gen_electricraft_assets.py <extracted ElectriCraft jar dir> <original ElectriCraft source dir>
(needs PIL for the ingot sprites)
"""
import json
import os
import shutil
import sys

JAR, SRC = sys.argv[1], sys.argv[2]
PORT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(PORT, "electricraft/src/main/resources/assets/electricraft")
LANGS = {"en_US": "en_us", "de_DE": "de_de", "ru_RU": "ru_ru", "zh_CN": "zh_cn"}
WIRES = ["steel", "tin", "nickel", "aluminum", "copper", "silver", "gold", "platinum"]


def read_lang(code):
    d = {}
    raw = open(os.path.join(JAR, "assets/electricraft/lang", code + ".lang"), "rb").read()
    try:
        text = raw.decode("utf-8-sig")
    except UnicodeDecodeError:
        text = raw.decode("cp1252")
    if True:
        for line in text.splitlines():
            line = line.rstrip("\r\n")
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                d[k.strip()] = v.strip()
    return d


ORIG = {c: read_lang(c) for c in LANGS}
NEW = {c: {} for c in LANGS}


def put_lang(key, orig_key):
    for c in LANGS:
        v = ORIG[c].get(orig_key) or ORIG["en_US"][orig_key]
        NEW[c][key] = v


for sub in ("blockstates", "models/block", "models/item", "textures/block/wire", "textures/machine", "lang"):
    os.makedirs(os.path.join(RES, sub), exist_ok=True)
for fn in os.listdir(os.path.join(JAR, "assets/electricraft/textures/blocks/wire")):
    shutil.copyfile(os.path.join(JAR, "assets/electricraft/textures/blocks/wire", fn), os.path.join(RES, "textures/block/wire", fn))
for fn in ("generatortex.png", "elecmotortex.png", "relay.png", "resistor.png", "metertex.png", "fusetex.png", "fusetex-hot.png",
           "fusetex-hot2.png", "fusetex-hot3.png", "fusetex-burn.png", "transformertex.png"):
    shutil.copyfile(os.path.join(SRC, "Textures", fn), os.path.join(RES, "textures/machine", fn))

DIRS = {"down": (1, -1), "up": (1, 1), "north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1)}
FACE = {(0, -1): "west", (0, 1): "east", (1, -1): "down", (1, 1): "up", (2, -1): "north", (2, 1): "south"}


def box(lo, hi, faces):
    return {"from": lo, "to": hi, "faces": faces}


for w in WIRES:
    for ins in (False, True):
        name = ("insulated_" if ins else "") + w + "_wire"
        tex = "electricraft:block/wire/" + w + ("_ins" if ins else "")
        size = 16 / 3 if ins else 4
        lo, hi = 8 - size / 2, 8 + size / 2
        textures = {"wire": tex, "end": tex + "_end", "particle": tex}
        core = box([lo] * 3, [hi] * 3, {f: {"texture": "#wire"} for f in DIRS})
        os.makedirs(os.path.join(RES, "models/block", name), exist_ok=True)
        with open(os.path.join(RES, "models/block", name, "core.json"), "w") as f:
            json.dump({"textures": textures, "elements": [core]}, f)
        multipart = [{"apply": {"model": "electricraft:block/%s/core" % name}}]
        for d, (axis, s_) in DIRS.items():
            a, b = ([hi, 16] if s_ > 0 else [0, lo])
            l = [lo] * 3
            h = [hi] * 3
            l[axis], h[axis] = a, b
            faces = {FACE[(ax, sg)]: {"texture": "#end"} for ax in range(3) if ax != axis for sg in (-1, 1)}
            with open(os.path.join(RES, "models/block", name, "arm_" + d + ".json"), "w") as f:
                json.dump({"textures": textures, "elements": [box(l, h, faces)]}, f)
            multipart.append({"when": {d: "true"}, "apply": {"model": "electricraft:block/%s/arm_%s" % (name, d)}})
        with open(os.path.join(RES, "blockstates", name + ".json"), "w") as f:
            json.dump({"multipart": multipart}, f, indent=1)
        with open(os.path.join(RES, "models/block", name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:block/block", "textures": textures, "elements": [core]}, f)
        with open(os.path.join(RES, "models/item", name + ".json"), "w") as f:
            json.dump({"parent": "electricraft:block/" + name}, f)
        put_lang("block.electricraft." + name, ("wire.insulated." if ins else "wire.") + w)

for mid, key in (("generator", "machine.electrigenerator"), ("motor", "machine.electrimotor"), ("resistor", "machine.electriresistor"),
                 ("relay", "machine.electrirelay"), ("meter", "machine.wiremeter"), ("fuse", "machine.fuse"), ("precise_resistor", "machine.precresistor"),
                 ("transformer", "machine.transformer")):
    with open(os.path.join(RES, "blockstates", mid + ".json"), "w") as f:
        json.dump({"variants": {"": {"model": "electricraft:block/" + mid}}}, f)
    with open(os.path.join(RES, "models/block", mid + ".json"), "w") as f:
        json.dump({"textures": {"particle": "rotarycraft:block/steel"}}, f)
    with open(os.path.join(RES, "models/item", mid + ".json"), "w") as f:
        json.dump({"parent": "minecraft:builtin/entity", "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]}}}, f)
    put_lang("block.electricraft." + mid, key)
os.makedirs(os.path.join(RES, "textures/gui"), exist_ok=True)
shutil.copyfile(os.path.join(SRC, "Textures/GUI/transformergui.png"), os.path.join(RES, "textures/gui/transformergui.png"))

# ---------------- ores and ingots
from PIL import Image

DATA = os.path.join(PORT, "electricraft/src/main/resources/data")
ORES = [("copper", 32, 64, 6, 8, 0.5, 1), ("tin", 48, 72, 8, 8, 0.2, 1), ("silver", 0, 32, 6, 4, 0.8, 2),
        ("nickel", 16, 48, 8, 6, 0.3, 1), ("aluminum", 60, 128, 8, 10, 0.4, 1), ("platinum", 0, 16, 4, 2, 1.0, 2)]
os.makedirs(os.path.join(RES, "textures/item"), exist_ok=True)
sheet = Image.open(os.path.join(SRC, "Textures/Items/items.png")).convert("RGBA")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)


def tag(ns, kind, name, values):
    write(os.path.join(DATA, ns, "tags", kind, name + ".json"), {"replace": False, "values": values})


def self_drop(name):
    write(os.path.join(DATA, "electricraft/loot_table/blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "electricraft:" + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": "electricraft:blocks/" + name})


ported = []
for meta, (m, ymin, ymax, size, count, xp, level) in enumerate(ORES):
    if m == "copper":
        continue
    ported.append(m)
    ore = m + "_ore"
    shutil.copyfile(os.path.join(JAR, "assets/electricraft/textures/blocks", "ore" + m.capitalize() + ".png"), os.path.join(RES, "textures/block", ore + ".png"))
    idx = 16 + meta
    sheet.crop(((idx % 16) * 16, (idx // 16) * 16, (idx % 16) * 16 + 16, (idx // 16) * 16 + 16)).save(os.path.join(RES, "textures/item", m + "_ingot.png"))
    write(os.path.join(RES, "blockstates", ore + ".json"), {"variants": {"": {"model": "electricraft:block/" + ore}}})
    write(os.path.join(RES, "models/block", ore + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "electricraft:block/" + ore}})
    write(os.path.join(RES, "models/item", ore + ".json"), {"parent": "electricraft:block/" + ore})
    write(os.path.join(RES, "models/item", m + "_ingot.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "electricraft:item/" + m + "_ingot"}})
    put_lang("block.electricraft." + ore, "ore." + m)
    put_lang("item.electricraft." + m + "_ingot", "ingot." + m)
    self_drop(ore)
    for kind in ("block", "item"):
        tag("c", kind, "ores/" + m, ["electricraft:" + ore])
    tag("c", "item", "ingots/" + m, ["electricraft:" + m + "_ingot"])
    write(os.path.join(DATA, "electricraft/worldgen/configured_feature", ore + ".json"), {"type": "minecraft:ore", "config": {
        "size": size, "discard_chance_on_air_exposure": 0.0,
        "targets": [{"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:stone"}, "state": {"Name": "electricraft:" + ore}}]}})
    write(os.path.join(DATA, "electricraft/worldgen/placed_feature", ore + ".json"),
          {"feature": "electricraft:" + ore, "placement": [{"type": "electricraft:ore_control", "ore": m}]})
for kind in ("block", "item"):
    tag("c", kind, "ores", ["#c:ores/" + m for m in ported])
tag("c", "item", "ingots", ["#c:ingots/" + m for m in ported])
tag("minecraft", "block", "mineable/pickaxe", ["electricraft:" + m + "_ore" for m in ported] + ["electricraft:rf_battery", "electricraft:rf_cable"]
    + ["electricraft:%s_wireless_pad" % t for t in ["basic", "improved", "advanced", "hightech", "superconducting"]] + ["electricraft:%s_battery" % b for b in
    ["redstone", "glowstone", "lapis", "ender", "diamond", "star"]]
    # Original ElectriBlocks.getBlockMaterial: everything but the ores is Material.iron, so the machines are mined at pickaxe speed too
    # (added 2026-11-03, see PORTING_NOTES.md). tag() writes the file per call: all entries must be in this one call.
    + ["electricraft:" + m for m in ("generator", "motor", "transformer", "meter", "relay", "resistor", "precise_resistor", "fuse")])
tag("minecraft", "block", "needs_stone_tool", ["electricraft:" + o[0] + "_ore" for o in ORES if o[0] in ported and o[6] == 1])
tag("minecraft", "block", "needs_iron_tool", ["electricraft:" + o[0] + "_ore" for o in ORES if o[0] in ported and o[6] == 2])
write(os.path.join(DATA, "electricraft/neoforge/biome_modifier/ores.json"), {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
      "features": ["electricraft:" + m + "_ore" for m in ported], "step": "underground_ores"})
for w in WIRES:
    for ins in ("", "insulated_"):
        self_drop(ins + w + "_wire")

# ---------------- batteries (BlockElectricBattery + BatteryRenderer), crystals and dusts (item sprite sheet: crafting 32+, crystals 48+)
BATTERIES = ["redstone", "glowstone", "lapis", "ender", "diamond", "star"]
os.makedirs(os.path.join(RES, "textures/block/battery"), exist_ok=True)
for fn in ["_top.png", "_bottom.png"] + [b + ".png" for b in BATTERIES] + [b + "_glow.png" for b in BATTERIES]:
    shutil.copyfile(os.path.join(JAR, "assets/electricraft/textures/blocks/battery", fn), os.path.join(RES, "textures/block/battery", fn))


def sprite(idx, out):
    sheet.crop(((idx % 16) * 16, (idx // 16) * 16, (idx % 16) * 16 + 16, (idx // 16) * 16 + 16)).save(os.path.join(RES, "textures/item", out + ".png"))
    write(os.path.join(RES, "models/item", out + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "electricraft:item/" + out}})


for meta, b in enumerate(BATTERIES):
    name = b + "_battery"
    for charged in (False, True):
        glow = {"texture": "#glow"}
        if charged:  # original: the glow layer at full brightness while energy > 0
            glow["neoforge_data"] = {"block_light": 15, "sky_light": 15, "ambient_occlusion": False}
        faces = {f: dict(glow, cullface=f) for f in ("north", "south", "west", "east")}
        write(os.path.join(RES, "models/block", name + ("_charged" if charged else "") + ".json"), {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"side": "electricraft:block/battery/" + b, "top": "electricraft:block/battery/_top", "bottom": "electricraft:block/battery/_bottom",
                         "glow": "electricraft:block/battery/" + b + "_glow", "particle": "electricraft:block/battery/" + b},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#top", "cullface": "up"},
                 "north": {"texture": "#side", "cullface": "north"}, "south": {"texture": "#side", "cullface": "south"},
                 "west": {"texture": "#side", "cullface": "west"}, "east": {"texture": "#side", "cullface": "east"}}},
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]})
    write(os.path.join(RES, "blockstates", name + ".json"), {"variants": {"charged=false": {"model": "electricraft:block/" + name},
                                                                     "charged=true": {"model": "electricraft:block/" + name + "_charged"}}})
    write(os.path.join(RES, "models/item", name + ".json"), {"parent": "electricraft:block/" + name})
    put_lang("block.electricraft." + name, "battery." + b)
    write(os.path.join(DATA, "electricraft/loot_table/blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "electricraft:" + name, "functions": [{"function": "minecraft:copy_components",
            "source": "block_entity", "include": ["electricraft:battery_energy"]}]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": "electricraft:blocks/" + name})
    sprite(48 + meta, b + "_crystal")
    put_lang("item.electricraft." + b + "_crystal", "energycrystal." + b)
for meta, d in enumerate(["bluedust", "diamonddust", "quartzdust", "crystaldust"]):
    sprite(32 + meta, d)
    put_lang("item.electricraft." + d, "electricrafting." + d)
tag("c", "item", "dusts/lapis", ["electricraft:bluedust"])
tag("c", "item", "dusts/diamond", ["electricraft:diamonddust"])
tag("c", "item", "dusts/quartz", ["electricraft:quartzdust"])
tag("c", "item", "dusts", ["#c:dusts/lapis", "#c:dusts/diamond", "#c:dusts/quartz", "electricraft:crystaldust"])

# ---------------- RF (FE) parts: cable (CableRenderer: core 1/4 + arms, animated rf textures), battery (RF side), RF crystal (sprite 54)
for fn in ("rf.png", "rf.png.mcmeta", "rf_end.png", "rf_end.png.mcmeta"):
    shutil.copyfile(os.path.join(JAR, "assets/electricraft/textures/blocks", fn), os.path.join(RES, "textures/block", fn.replace("rf", "rf_cable", 1)))
textures = {"wire": "electricraft:block/rf_cable", "end": "electricraft:block/rf_cable_end", "particle": "electricraft:block/rf_cable"}
lo, hi = 6, 10
core = box([lo] * 3, [hi] * 3, {f: {"texture": "#wire"} for f in DIRS})
write(os.path.join(RES, "models/block/rf_cable/core.json"), {"textures": textures, "elements": [core]})
multipart = [{"apply": {"model": "electricraft:block/rf_cable/core"}}]
for d, (axis, s_) in DIRS.items():
    a, b = ([hi, 16] if s_ > 0 else [0, lo])
    l, h = [lo] * 3, [hi] * 3
    l[axis], h[axis] = a, b
    faces = {FACE[(ax, sg)]: {"texture": "#end"} for ax in range(3) if ax != axis for sg in (-1, 1)}
    write(os.path.join(RES, "models/block/rf_cable/arm_" + d + ".json"), {"textures": textures, "elements": [box(l, h, faces)]})
    multipart.append({"when": {d: "true"}, "apply": {"model": "electricraft:block/rf_cable/arm_" + d}})
write(os.path.join(RES, "blockstates/rf_cable.json"), {"multipart": multipart})
write(os.path.join(RES, "models/block/rf_cable.json"), {"parent": "minecraft:block/block", "textures": textures, "elements": [core]})
write(os.path.join(RES, "models/item/rf_cable.json"), {"parent": "electricraft:block/rf_cable"})
put_lang("block.electricraft.rf_cable", "machine.rfcable")
self_drop("rf_cable")
shutil.copyfile(os.path.join(JAR, "assets/electricraft/textures/blocks/battery/rf.png"), os.path.join(RES, "textures/block/battery/rf.png"))
write(os.path.join(RES, "models/block/rf_battery.json"), {"parent": "minecraft:block/cube_bottom_top", "render_type": "minecraft:cutout",
      "textures": {"side": "electricraft:block/battery/rf", "top": "electricraft:block/battery/_top", "bottom": "electricraft:block/battery/_bottom"}})
write(os.path.join(RES, "blockstates/rf_battery.json"), {"variants": {"": {"model": "electricraft:block/rf_battery"}}})
write(os.path.join(RES, "models/item/rf_battery.json"), {"parent": "electricraft:block/rf_battery"})
put_lang("block.electricraft.rf_battery", "machine.rfbattery")
write(os.path.join(DATA, "electricraft/loot_table/blocks/rf_battery.json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": "electricraft:rf_battery", "functions": [{"function": "minecraft:copy_components",
        "source": "block_entity", "include": ["electricraft:battery_energy"]}]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    "random_sequence": "electricraft:blocks/rf_battery"})
sprite(54, "rf_crystal")
put_lang("item.electricraft.rf_crystal", "energycrystal.rf")
# wireless pads (BlockChargePad: front, back, side texture per tier), one block per tier with a facing state; front modelled on north
os.makedirs(os.path.join(RES, "textures/block/wireless"), exist_ok=True)
for fn in os.listdir(os.path.join(JAR, "assets/electricraft/textures/blocks/wireless")):
    shutil.copyfile(os.path.join(JAR, "assets/electricraft/textures/blocks/wireless", fn), os.path.join(RES, "textures/block/wireless", fn))
ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
for i, t in enumerate(["basic", "improved", "advanced", "hightech", "superconducting"]):
    name = t + "_wireless_pad"
    write(os.path.join(RES, "models/block", name + ".json"), {"parent": "minecraft:block/cube", "textures": {
        "north": "electricraft:block/wireless/front", "south": "electricraft:block/wireless/back", "east": "electricraft:block/wireless/side_%d" % i,
        "west": "electricraft:block/wireless/side_%d" % i, "up": "electricraft:block/wireless/side_%d" % i, "down": "electricraft:block/wireless/side_%d" % i,
        "particle": "electricraft:block/wireless/side_%d" % i}})
    write(os.path.join(RES, "blockstates", name + ".json"), {"variants": {"facing=" + d: dict({"model": "electricraft:block/" + name}, **r) for d, r in ROT.items()}})
    write(os.path.join(RES, "models/item", name + ".json"), {"parent": "electricraft:block/" + name})
    for c in LANGS:
        NEW[c]["block.electricraft." + name] = (ORIG[c].get("electrichargepad." + t) or ORIG["en_US"]["electrichargepad." + t]) + " " + \
            (ORIG[c].get("machine.wirelesspad") or ORIG["en_US"]["machine.wirelesspad"])
    self_drop(name)
shutil.copyfile(os.path.join(SRC, "Textures/GUI/rfcablegui.png"), os.path.join(RES, "textures/gui/rfcablegui.png"))

for c in LANGS:
    NEW[c]["itemGroup.electricraft"] = "ElectriCraft"
    with open(os.path.join(RES, "lang", LANGS[c] + ".json"), "w", encoding="utf-8") as f:
        json.dump(dict(sorted(NEW[c].items())), f, indent=2, ensure_ascii=False)
print("electricraft assets written:", len(NEW["en_US"]), "lang keys")

#!/usr/bin/env python3
"""Generates the ReactorCraft resources of the port from the original jar and sources (first slice: ores and materials).

* Ore and fluorite textures from the jar, raw/fluorite/ingot items cut from the item sprite sheet (RAW 4+, FLUORITE 16+, INGOTS 32+).
* .lang -> JSON (en, de, pt, ru, zh), block states, models, tags, worldgen (features + biome modifiers), recipes (smelting, storage).
* Drops are code (ReactorOreBlock / FluoriteBlock), so there are no loot tables for these blocks.

Usage: /usr/bin/python3 tools/gen_reactorcraft_assets.py <extracted ReactorCraft jar dir> <original ReactorCraft source dir>
"""
import json
import os
import shutil
import sys

from PIL import Image

JAR, SRC = sys.argv[1], sys.argv[2]
PORT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(PORT, "reactorcraft/src/main/resources/assets/reactorcraft")
DATA = os.path.join(PORT, "reactorcraft/src/main/resources/data")
LANGS = {"en_US": "en_us", "de_DE": "de_de", "pt_BR": "pt_br", "ru_RU": "ru_ru", "zh_CN": "zh_cn"}


def read_lang(code):
    raw = open(os.path.join(JAR, "assets/reactorcraft/lang", code + ".lang"), "rb").read()
    try:
        text = raw.decode("utf-8-sig")
    except UnicodeDecodeError:
        text = raw.decode("cp1252")
    d = {}
    for line in text.splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            d[k.strip()] = v.strip()
    return d


ORIG = {c: read_lang(c) for c in LANGS}
NEW = {c: {} for c in LANGS}


def tr(c, key):
    return ORIG[c].get(key) or ORIG["en_US"][key]


def put_lang(key, orig_key, fmt="%s"):
    for c in LANGS:
        NEW[c][key] = fmt % tr(c, orig_key)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)


TAGS = {}


def tag(ns, kind, name, values):
    """Collects the values of all calls for the same tag (written once per call, so the file always holds the union)."""
    key = os.path.join(DATA, ns, "tags", kind, name + ".json")
    TAGS.setdefault(key, [])
    TAGS[key] += [v for v in values if v not in TAGS[key]]
    write(key, {"replace": False, "values": TAGS[key]})


for sub in ("textures/block", "textures/item"):
    os.makedirs(os.path.join(RES, sub), exist_ok=True)
sheet = Image.open(os.path.join(SRC, "Textures/Items/items1.png")).convert("RGBA")


def sprite(idx, out):
    sheet.crop(((idx % 16) * 16, (idx // 16) * 16, (idx % 16) * 16 + 16, (idx // 16) * 16 + 16)).save(os.path.join(RES, "textures/item", out + ".png"))
    write(os.path.join(RES, "models/item", out + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "reactorcraft:item/" + out}})


def cube(name, tex, extra_states=None):
    write(os.path.join(RES, "models/block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/" + tex}})
    variants = {s: {"model": "reactorcraft:block/" + name} for s in (extra_states or [""])}
    write(os.path.join(RES, "blockstates", name + ".json"), {"variants": variants})
    write(os.path.join(RES, "models/item", name + ".json"), {"parent": "reactorcraft:block/" + name})


# (name, minY, maxY, vein, count, dim, level, xp, dict, product id, product kind)
ORES = [("pitchblende", 1, 1.0, "uranium", "reactorcraft:uranium_ingot"), ("cadmium", 2, 0.7, "cadmium", "reactorcraft:cadmium_ingot"),
        ("indium", 2, 1.0, "indium", "reactorcraft:indium_ingot"), ("silver", 2, 0.5, "silver", "reactorcraft:silver_ingot"),
        ("endblende", 1, 1.0, "uranium", "reactorcraft:uranium_ingot"), ("ammonium", 1, 0.8, "ammonium", "reactorcraft:ammonium"),
        ("calcite", 0, 0.4, "calcite", "reactorcraft:calcite"), ("magnetite", 1, 0.8, "magnetite", "reactorcraft:magnetite"),
        ("thorium", 2, 0.8, "thorium", "reactorcraft:thordust")]
VEIN = {"fluorite": 8, "pitchblende": 16, "cadmium": 9, "indium": 7, "silver": 9, "endblende": 16, "ammonium": 8, "calcite": 4, "magnetite": 16, "thorium": 24}
TARGET = {"endblende": "minecraft:end_stone", "ammonium": "minecraft:netherrack", "thorium": "minecraft:netherrack"}
COLORS = ["blue", "pink", "orange", "magenta", "green", "red", "white", "yellow"]
RAW = ["hf", "fueldust", "depdust", "ammonium", "lime", "calcite", "magnetite", "thordust", "emeralddust", "wastedust"]
INGOTS = ["uranium", "cadmium", "indium", "silver"]

recipes = os.path.join(DATA, "reactorcraft/recipe")
if os.path.isdir(recipes):
    shutil.rmtree(recipes)


def smelt(name, inp, out, xp):
    write(os.path.join(recipes, name + ".json"), {"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": inp},
                                                  "result": {"id": out}, "experience": xp, "cookingtime": 200})


def feature(name, block, target, size):
    write(os.path.join(DATA, "reactorcraft/worldgen/configured_feature", name + ".json"), {"type": "minecraft:ore", "config": {
        "size": size, "discard_chance_on_air_exposure": 0.0,
        "targets": [{"target": {"predicate_type": "minecraft:block_match", "block": target}, "state": {"Name": block}}]}})


for name, level, xp, dict_name, product in ORES:
    ore = name + "_ore"
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/ore", name + ".png"), os.path.join(RES, "textures/block", ore + ".png"))
    cube(ore, ore)
    put_lang("block.reactorcraft." + ore, "ore." + ("pitchblende" if name == "endblende" else name))
    for kind in ("block", "item"):
        tag("c", kind, "ores/" + dict_name, ["reactorcraft:" + ore])
    smelt(ore + "_smelting", "reactorcraft:" + ore, product, xp)
    feature(ore, "reactorcraft:" + ore, TARGET.get(name, "minecraft:stone"), VEIN[name])
    write(os.path.join(DATA, "reactorcraft/worldgen/placed_feature", ore + ".json"),
          {"feature": "reactorcraft:" + ore, "placement": [{"type": "reactorcraft:ore", "ore": name}]})
for c in COLORS:
    ore, blk = c + "_fluorite_ore", c + "_fluorite_block"
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/ore", "fluorite_" + c + ".png"), os.path.join(RES, "textures/block", ore + ".png"))
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/mat", "floblock_" + c + ".png"), os.path.join(RES, "textures/block", blk + ".png"))
    cube(ore, ore, ["activated=false", "activated=true"])
    cube(blk, blk, ["activated=false", "activated=true"])
    name = c[0].upper() + c[1:]
    put_lang("block.reactorcraft." + ore, "ore.fluorite", name + " %s")
    put_lang("block.reactorcraft." + blk, "block.fluorite", name + " %s")
    put_lang("item.reactorcraft." + c + "_fluorite", "item.fluorite", name + " %s")
    smelt(ore + "_smelting", "reactorcraft:" + ore, "reactorcraft:" + c + "_fluorite", 0.4)
    feature(ore, "reactorcraft:" + ore, "minecraft:stone", VEIN["fluorite"])
    write(os.path.join(DATA, "reactorcraft/worldgen/placed_feature", ore + ".json"),
          {"feature": "reactorcraft:" + ore, "placement": [{"type": "reactorcraft:ore", "ore": "fluorite", "color": c}]})
    write(os.path.join(recipes, blk + ".json"), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["CCC", "CCC", "CCC"],
          "key": {"C": {"item": "reactorcraft:" + c + "_fluorite"}}, "result": {"id": "reactorcraft:" + blk, "count": 1}})
    write(os.path.join(recipes, c + "_fluorite_from_block.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": [{"item": "reactorcraft:" + blk}], "result": {"id": "reactorcraft:" + c + "_fluorite", "count": 9}})
for kind in ("block", "item"):
    tag("c", kind, "ores/fluorite", ["reactorcraft:" + c + "_fluorite_ore" for c in COLORS])
    tag("c", kind, "ores", ["#c:ores/" + d for d in sorted({o[3] for o in ORES} | {"fluorite"})])
tag("c", "item", "gems/fluorite", ["reactorcraft:" + c + "_fluorite" for c in COLORS])
tag("c", "item", "gems", ["#c:gems/fluorite"])
for i in INGOTS:
    tag("c", "item", "ingots/" + i, ["reactorcraft:" + i + "_ingot"])
tag("c", "item", "ingots", ["#c:ingots/" + i for i in INGOTS])
tag("c", "item", "dusts/thorium", ["reactorcraft:thordust"])
tag("c", "item", "dusts", ["#c:dusts/thorium"])
all_ores = ["reactorcraft:" + o[0] + "_ore" for o in ORES] + ["reactorcraft:" + c + "_fluorite_ore" for c in COLORS]
tag("minecraft", "block", "mineable/pickaxe", all_ores + ["reactorcraft:" + c + "_fluorite_block" for c in COLORS] + ["reactorcraft:calcite_block"])
tag("minecraft", "block", "needs_stone_tool", ["reactorcraft:" + o[0] + "_ore" for o in ORES if o[1] == 1])
tag("minecraft", "block", "needs_iron_tool", ["reactorcraft:" + o[0] + "_ore" for o in ORES if o[1] == 2])

# biome modifiers: overworld (stone), pitchblende only in oceans, rivers and mushroom islands, End, Nether
BM = os.path.join(DATA, "reactorcraft/neoforge/biome_modifier")


def add(name, biomes, feats):
    write(os.path.join(BM, name + ".json"), {"type": "neoforge:add_features", "biomes": biomes,
                                            "features": ["reactorcraft:" + f for f in feats], "step": "underground_ores"})


add("overworld_ores", "#minecraft:is_overworld", [c + "_fluorite_ore" for c in COLORS] + ["cadmium_ore", "indium_ore", "silver_ore", "calcite_ore", "magnetite_ore"])
add("pitchblende", {"type": "neoforge:or", "values": ["#minecraft:is_ocean", "#minecraft:is_river", ["minecraft:mushroom_fields"]]}, ["pitchblende_ore"])
add("end_ores", "#minecraft:is_end", ["endblende_ore"])
add("nether_ores", "#minecraft:is_nether", ["ammonium_ore", "thorium_ore"])

# items
for i, r in enumerate(RAW):
    sprite(4 + i, r)
    put_lang("item.reactorcraft." + r, "raw." + r)
for i, c in enumerate(COLORS):
    sprite(16 + i, c + "_fluorite")
for i, ing in enumerate(INGOTS):
    sprite(32 + i, ing + "_ingot")
    put_lang("item.reactorcraft." + ing + "_ingot", "item." + ing)
smelt("lime_from_calcite", "reactorcraft:calcite", "reactorcraft:lime", 0.2)
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/mat/calcite.png"), os.path.join(RES, "textures/block/calcite_block.png"))
cube("calcite_block", "calcite_block")
put_lang("block.reactorcraft.calcite_block", "block.calcite")
write(os.path.join(recipes, "calcite_block.json"), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["CCC", "CCC", "CCC"],
      "key": {"C": {"item": "reactorcraft:calcite"}}, "result": {"id": "reactorcraft:calcite_block", "count": 1}})
write(os.path.join(recipes, "calcite_from_block.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
      "ingredients": [{"item": "reactorcraft:calcite_block"}], "result": {"id": "reactorcraft:calcite", "count": 9}})

# fluids (textures with their animation .mcmeta), canisters (sprites 48+, ReactorNames.canNames order), heavy water bucket (sprite 3)
FLUIDS = {"heavy_water": "heavy water", "hydrofluoric_acid": "hydrofluoric acid", "uranium_hexafluoride": "uranium hexafluoride", "ammonia": "ammonia",
          "sodium": "sodium", "chlorine": "chlorine", "oxygen": "oxygen", "liquid_oxygen": None, "deuterium": "deuterium", "tritium": "tritium",
          "fusion_plasma": "fusion plasma", "low_pressure_ammonia": "lowpammonia", "low_pressure_water": "lowpwater", "hot_sodium": "hotsodium",
          "warm_sodium": "warmsodium", "co2": "co2", "hot_co2": "hot co2", "corium": None, "nuclear_waste": "nuclear waste", "lithium": "lithium",
          "lifbe": "lifbe", "lifbe_fuel": "lifbe fuel", "lifbe_fuel_preheat": "lifbe fuel preheat", "hot_lifbe": "hot lifbe"}
os.makedirs(os.path.join(RES, "textures/block/fluid"), exist_ok=True)
for fn in os.listdir(os.path.join(JAR, "assets/reactorcraft/textures/blocks/fluid")):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/fluid", fn), os.path.join(RES, "textures/block/fluid", fn))
for fid, orig in FLUIDS.items():
    if orig is not None:
        put_lang("fluid_type.reactorcraft." + fid, "fluid.rc " + orig)
# no original names: liquid oxygen (shares the oxygen entry's text "Oxygen Gas" otherwise) and corium (block.slag: Corium)
put_lang("fluid_type.reactorcraft.corium", "block.slag")
for c in LANGS:
    NEW[c]["fluid_type.reactorcraft.liquid_oxygen"] = "Liquid Oxygen"
CANS = ["empty", "uf6", "hf", "nh3", "na", "h2", "h3", "cl", "o", "co2", "hco2", "hna", "li", "lifbe", "hlifbe", "lifbefuel"]
for i, cn in enumerate(CANS):
    sprite(48 + i, cn + "_canister")
    put_lang("item.reactorcraft." + cn + "_canister", "can." + cn)
sprite(3, "heavy_water_bucket")
put_lang("item.reactorcraft.heavy_water_bucket", "item.heavybucket")

# crafting items (CraftingItems, sprites 144+; the empty slot of SHIELD is the original's), concrete and graphite blocks, recipes
CRAFT = ["canister", "rod", "tank", "alloy", "backing", "magnetic", "magnetcore", "coolant", "wire", "shield", "ferroingot", "hysteresis",
         "hysteresisring", "graphite", "udust", "fabric", "carbideflakes", "carbide", "turbcore"]
for i, cn in enumerate(CRAFT):
    iid = "fuel_canister" if cn == "canister" else cn
    sprite(144 + i, iid)
    put_lang("item.reactorcraft." + iid, "crafting." + cn)
for b, tex, key in (("concrete", "concrete", "block.concrete"), ("graphite_block", "graphite", "block.graphite")):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/mat", tex + ".png"), os.path.join(RES, "textures/block", b + ".png"))
    cube(b, b)
    put_lang("block.reactorcraft." + b, key)
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:concrete", "reactorcraft:graphite_block"])


def rc(i):
    return {"item": i if ":" in i else "reactorcraft:" + i}


def shaped(name, out, pattern, key, count=1, cond=None):
    d = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
         "result": {"id": out if ":" in out else "reactorcraft:" + out, "count": count}}
    if cond:
        d["neoforge:conditions"] = cond
    write(os.path.join(recipes, name + ".json"), d)


PARTCRAFT = (6, 3, 2)
shaped("tank", "tank", ["OOO", "O O", "OOO"], {"O": "rotarycraft:blast_glass"})
shaped("fuel_canister", "fuel_canister", [" S ", "SCS", " S "], {"S": "alloy", "C": "minecraft:chest"})
shaped("rod", "rod", ["SAS", "ACA", "SAS"], {"S": "rotarycraft:steelingot", "A": "alloy", "C": "graphite"})
shaped("magnetic", "magnetic", ["SSS"], {"S": "ferroingot"}, 3)
shaped("magnetcore", "magnetcore", ["CCC", "C C", "CCC"], {"C": "magnetic"})
shaped("hysteresisring", "hysteresisring", ["CCC", "C C", "CCC"], {"C": "hysteresis"})
shaped("wire", "wire", ["  G", " G ", "G  "], {"G": "minecraft:gold_ingot"}, 2)
for d, n in zip((1, 2, 3), PARTCRAFT):
    shaped("hysteresis_difficulty_%d" % d, "hysteresis", ["ISI"], {"I": "minecraft:iron_ingot", "S": "rotarycraft:steelingot"}, n,
           [{"type": "rotarycraft:difficulty", "value": d}])
shaped("coolant", "coolant", ["SPS", "S S", "SpS"], {"S": "rotarycraft:steelingot", "P": "minecraft:glass_pane", "p": "rotarycraft:pipe"})
shaped("fabric_lead", "fabric", ["LDL", "LDL", "LDL"], {"D": {"tag": "c:ingots/lead"}, "L": "minecraft:leather"}, 2,
       [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": "c:ingots/lead"}}])
write(os.path.join(recipes, "carbideflakes.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
      "ingredients": [{"item": "rotarycraft:coaldust"}, {"item": "rotarycraft:tungstenflakes"}], "result": {"id": "reactorcraft:carbideflakes", "count": 1}})
shaped("turbcore", "turbcore", ["CCC", "CTC", "CCC"], {"C": "carbide", "T": "rotarycraft:compoundturb"})
shaped("graphite_block", "graphite_block", ["CCC", "CCC", "CCC"], {"C": "graphite"})
write(os.path.join(recipes, "graphite_from_block.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
      "ingredients": [{"item": "reactorcraft:graphite_block"}], "result": {"id": "reactorcraft:graphite", "count": 9}})
# concrete: clay, sand, gravel and water (original ShapelessFluidInputRecipe; here a water bucket, which is given back)
write(os.path.join(recipes, "concrete.json"), {"type": "minecraft:crafting_shapeless", "category": "building",
      "ingredients": [{"item": "minecraft:clay"}, {"item": "minecraft:sand"}, {"item": "minecraft:gravel"}, {"item": "minecraft:water_bucket"}],
      "result": {"id": "reactorcraft:concrete", "count": 4}})
# RotaryCraft machines: blast furnace alloys (shapeless in the 3x3 grid), grinder, pulse jet furnace, friction heater
write(os.path.join(recipes, "alloy.json"), {"type": "rotarycraft:blast_crafting_shapeless", "category": "misc",
      "ingredients": [{"tag": "c:ingots/cadmium"}, {"tag": "c:ingots/indium"}, {"tag": "c:ingots/silver"}], "result": {"id": "reactorcraft:alloy", "count": 3},
      "temperature": 1600, "speed": 1, "xp": 0.8, "alloy": True})
write(os.path.join(recipes, "ferroingot.json"), {"type": "rotarycraft:blast_crafting_shapeless", "category": "misc",
      "ingredients": [{"item": "rotarycraft:steelingot"}, {"item": "minecraft:iron_ingot"}, {"item": "reactorcraft:magnetite"}],
      "result": {"id": "reactorcraft:ferroingot", "count": 1}, "temperature": 1200, "speed": 1, "xp": 1.0, "alloy": True})
d = {"type": "rotarycraft:blast_crafting_shapeless", "category": "misc",
     "ingredients": [{"item": "rotarycraft:steelingot"}, {"tag": "c:ingots/nickel"}, {"item": "reactorcraft:magnetite"}],
     "result": {"id": "reactorcraft:ferroingot", "count": 1}, "temperature": 1200, "speed": 1, "xp": 1.0, "alloy": True,
     "neoforge:conditions": [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": "c:ingots/nickel"}}]}
write(os.path.join(recipes, "ferroingot_nickel.json"), d)
write(os.path.join(recipes, "udust_grinding.json"), {"type": "rotarycraft:grinder", "ingredient": {"tag": "c:ingots/uranium"},
      "result": {"id": "reactorcraft:udust", "count": 1}})
write(os.path.join(recipes, "carbide_pulse_jet.json"), {"type": "rotarycraft:pulse_jet", "ingredient": {"item": "reactorcraft:carbideflakes"},
      "result": {"id": "reactorcraft:carbide", "count": 1}, "temperature": 400})
write(os.path.join(recipes, "graphite_friction.json"), {"type": "rotarycraft:friction_heater", "ingredient": {"item": "rotarycraft:coaldust"},
      "result": {"id": "reactorcraft:graphite", "count": 1}, "temperature": 400, "duration": 100})

# machines (block entity renderer; block model only for particles, item drawn by the RotaryCraft machine item renderer)
os.makedirs(os.path.join(RES, "textures/machine"), exist_ok=True)
TABLE = [{"type": "rotarycraft:config", "option": "table_machines"}]


def machine(mid, lang_key, textures, pattern, key):
    for t in textures:
        shutil.copyfile(os.path.join(SRC, "Textures/TileEntity", t), os.path.join(RES, "textures/machine", t))
    write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"": {"model": "reactorcraft:block/" + mid}}})
    write(os.path.join(RES, "models/block", mid + ".json"), {"textures": {"particle": "rotarycraft:block/steel"}})
    write(os.path.join(RES, "models/item", mid + ".json"), {"parent": "minecraft:builtin/entity", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]}}})
    put_lang("block.reactorcraft." + mid, lang_key)
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)


machine("fluid_extractor", "machine.heavypump", ["heavypump.png"], ["PpP", "GIG", "PSP"],
        {"P": "rotarycraft:basepanel", "p": "rotarycraft:pipe", "G": "minecraft:glass", "I": "rotarycraft:impeller", "S": "rotarycraft:steel_shaft"})

machine("processor", "machine.processor", ["processor.png"], ["POP", "OMO"],
        {"O": "reactorcraft:tank", "M": "rotarycraft:mixer", "P": "rotarycraft:pipe"})
machine("synthesizer", "machine.synthesizer", [], ["SpS", "pMp", "ShS"],
        {"S": "rotarycraft:steelingot", "M": "rotarycraft:mixer", "p": "rotarycraft:basepanel", "h": "rotarycraft:igniter"})
# the synthesizer is a plain textured block (ReactorBlocks.MACHINE, end-textured: top and bottom synthesizer_top)
for t in ("synthesizer", "synthesizer_top"):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", t + ".png"), os.path.join(RES, "textures/block", t + ".png"))
write(os.path.join(RES, "models/block/synthesizer.json"), {"parent": "minecraft:block/cube_column",
      "textures": {"side": "reactorcraft:block/synthesizer", "end": "reactorcraft:block/synthesizer_top"}})
write(os.path.join(RES, "models/item/synthesizer.json"), {"parent": "reactorcraft:block/synthesizer"})
machine("isocentrifuge", "machine.isocentrifuge", ["centrifuge.png"], ["SPS", "B B", "PGP"],
        {"B": "rotarycraft:bedingot", "P": "rotarycraft:basepanel", "S": "rotarycraft:steelingot", "G": "rotarycraft:bedrock_unit16"})
# like every other ReactorCraft machine (metal, original Material.iron): mined with a pickaxe (added 2026-11-03, see PORTING_NOTES.md)
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:processor", "reactorcraft:synthesizer", "reactorcraft:isocentrifuge", "reactorcraft:fluid_extractor"])
os.makedirs(os.path.join(RES, "textures/gui"), exist_ok=True)
for g in ("processor.png", "synthesizer.png", "centrifuge.png"):
    shutil.copyfile(os.path.join(SRC, "Textures/GUI", g), os.path.join(RES, "textures/gui", g))

# fuel, depleted uranium, nuclear waste (sprites 1, 2, 0), radiation sickness, radiation damage
for idx, iid, key in ((1, "fuel", "item.fuel"), (2, "depleted", "item.depleted"), (0, "waste", "item.waste")):
    sprite(idx, iid)
    put_lang("item.reactorcraft." + iid, key)
for c in LANGS:
    NEW[c]["effect.reactorcraft.radiation"] = "Radiation Sickness"   # original setPotionName, English only
    NEW[c]["death.attack.reactorcraft.radiation"] = "%1$s died of radiation poisoning"   # original RadiationDamage, English only
    NEW[c]["entity.reactorcraft.radiation"] = "Radiation"
    NEW[c]["entity.reactorcraft.nuclear_waste"] = NEW[c]["item.reactorcraft.waste"]
write(os.path.join(DATA, "reactorcraft/damage_type/radiation.json"), {"message_id": "reactorcraft.radiation", "exhaustion": 0.0, "scaling": "never"})
for t in ("bypasses_armor", "bypasses_effects", "bypasses_enchantments"):
    tag("minecraft", "damage_type", t, ["reactorcraft:radiation"])
write(os.path.join(recipes, "fuel.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["dd", "dd"],
      "key": {"d": {"item": "reactorcraft:fueldust"}}, "result": {"id": "reactorcraft:fuel", "count": 2}})
write(os.path.join(recipes, "depleted.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["dd", "dd"],
      "key": {"d": {"item": "reactorcraft:depdust"}}, "result": {"id": "reactorcraft:depleted", "count": 2}})
shaped("fabric_depleted", "fabric", ["LDL", "LDL", "LDL"], {"D": "depleted", "L": "minecraft:leather"}, 3)

# fuel core and coolant cell: textured blocks with a texture state (fuel: side by column position, top/bottom #4; coolant: liquid)
for i in range(5):
    for t in ("fuel", "coolant"):
        shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "%s_#%d.png" % (t, i)), os.path.join(RES, "textures/block", "%s_%d.png" % (t, i)))
variants_f, variants_c = {}, {}
for i in range(5):
    write(os.path.join(RES, "models/block/fuel_rod_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/fuel_%d" % min(i, 3), "end": "reactorcraft:block/fuel_4"}})
    write(os.path.join(RES, "models/block/coolant_cell_%d.json" % i), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/coolant_%d" % i}})
    variants_f["state=%d" % i] = {"model": "reactorcraft:block/fuel_rod_%d" % i}
    variants_c["state=%d" % i] = {"model": "reactorcraft:block/coolant_cell_%d" % i}
write(os.path.join(RES, "blockstates/fuel_rod.json"), {"variants": variants_f})
write(os.path.join(RES, "blockstates/coolant_cell.json"), {"variants": variants_c})
write(os.path.join(RES, "models/item/fuel_rod.json"), {"parent": "reactorcraft:block/fuel_rod_0"})
write(os.path.join(RES, "models/item/coolant_cell.json"), {"parent": "reactorcraft:block/coolant_cell_0"})
put_lang("block.reactorcraft.fuel_rod", "machine.fuel")
put_lang("block.reactorcraft.coolant_cell", "machine.coolant")
for mid, pattern, key in (("fuel_rod", ["SHS", "PCP", "SCS"], {"P": "rotarycraft:basepanel", "S": "rotarycraft:steelingot", "C": "fuel_canister", "H": "minecraft:hopper"}),
                          ("coolant_cell", ["SPS", "GRG", "SPS"], {"S": "rotarycraft:steelingot", "P": "rotarycraft:pipe", "G": "minecraft:glass", "R": "rotarycraft:reservoir"})):
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:fuel_rod", "reactorcraft:coolant_cell"])
# corium as a world fluid (original BlockCoriumFlowing: slag top, flowing slag sides); texture from the jar's mat/slag
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/mat/slag.png"), os.path.join(RES, "textures/block/slag.png"))
write(os.path.join(RES, "blockstates/corium.json"), {"variants": {"": {"model": "reactorcraft:block/corium"}}})
write(os.path.join(RES, "models/block/corium.json"), {"textures": {"particle": "reactorcraft:block/slag"}})
put_lang("block.reactorcraft.corium", "block.slag")
shutil.copyfile(os.path.join(SRC, "Textures/GUI/fuelrod.png"), os.path.join(RES, "textures/gui/fuelrod.png"))
for c in LANGS:
    NEW[c]["entity.reactorcraft.neutron"] = "Neutron"

# control rods (modelled, drawn by RenderControl) and the reactor CPU (a textured block), CPU GUI, control/scram sounds
machine("control_rod", "machine.control", ["control.png"], ["SGS", "RRR", "PPP"],
        {"S": "rotarycraft:steelingot", "P": "rotarycraft:basepanel", "R": "reactorcraft:rod", "G": "rotarycraft:steel_unit2"})
machine("cpu", "machine.cpu", [], ["SCS", "CGC", "SCS"], {"S": "rotarycraft:basepanel", "C": "rotarycraft:pcb", "G": "rotarycraft:steel_unit2"})
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/cpu.png"), os.path.join(RES, "textures/block/cpu.png"))
write(os.path.join(RES, "models/block/cpu.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/cpu"}})
write(os.path.join(RES, "models/item/cpu.json"), {"parent": "reactorcraft:block/cpu"})
shutil.copyfile(os.path.join(SRC, "Textures/GUI/control2.png"), os.path.join(RES, "textures/gui/control2.png"))
os.makedirs(os.path.join(RES, "sounds"), exist_ok=True)
for snd in ("control", "scram"):
    shutil.copyfile(os.path.join(SRC, "Sounds", snd + ".ogg"), os.path.join(RES, "sounds", snd + ".ogg"))
write(os.path.join(RES, "sounds.json"), {snd: {"sounds": [{"name": "reactorcraft:" + snd}]} for snd in ("control", "scram")})
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:control_rod", "reactorcraft:cpu"])

# power generation: reactor boiler (column texture state like the fuel core, top/bottom #0), steam line (black wool pipe, tinted
# 0.5 in the world, grey wool as item), steam grate and steam turbine (modelled), the steam block, turbine sound
for i in range(4):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "boiler_#%d.png" % i), os.path.join(RES, "textures/block", "boiler_%d.png" % i))
variants_b = {}
for i in range(5):
    write(os.path.join(RES, "models/block/reactor_boiler_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/boiler_%d" % (i if i < 4 else 0), "end": "reactorcraft:block/boiler_0"}})
    variants_b["state=%d" % i] = {"model": "reactorcraft:block/reactor_boiler_%d" % i}
write(os.path.join(RES, "blockstates/reactor_boiler.json"), {"variants": variants_b})
write(os.path.join(RES, "models/item/reactor_boiler.json"), {"parent": "reactorcraft:block/reactor_boiler_0"})
put_lang("block.reactorcraft.reactor_boiler", "machine.reactorboiler")
for mid, pattern, key in (("reactor_boiler", ["SPS", "PrP", "SPS"], {"r": "rotarycraft:reservoir", "P": "rotarycraft:basepanel", "S": "rotarycraft:steelingot"}),):
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)

lo, hi = 8 - 8 / 3, 8 + 8 / 3
FACES = ("down", "up", "north", "south", "west", "east")
line_tex = {"line": "minecraft:block/black_wool", "particle": "minecraft:block/black_wool"}
os.makedirs(os.path.join(RES, "models/block/steam_line"), exist_ok=True)
write(os.path.join(RES, "models/block/steam_line/core.json"), {"textures": line_tex, "elements": [
    {"from": [lo] * 3, "to": [hi] * 3, "faces": {f: {"texture": "#line", "tintindex": 0} for f in FACES}}]})
AX = {"down": (1, -1), "up": (1, 1), "north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1)}
OPP = {"down": "up", "up": "down", "north": "south", "south": "north", "west": "east", "east": "west"}
multipart = [{"apply": {"model": "reactorcraft:block/steam_line/core"}}]
for d, (axis, sg) in AX.items():
    l, h = [lo] * 3, [hi] * 3
    l[axis], h[axis] = ([hi, 16] if sg > 0 else [0, lo])
    faces = {f: {"texture": "#line", "tintindex": 0} for f in FACES if f not in (d, OPP[d])}
    write(os.path.join(RES, "models/block/steam_line/arm_%s.json" % d), {"textures": line_tex, "elements": [{"from": l, "to": h, "faces": faces}]})
    multipart.append({"when": {d: "true"}, "apply": {"model": "reactorcraft:block/steam_line/arm_" + d}})
write(os.path.join(RES, "blockstates/steam_line.json"), {"multipart": multipart})
s3 = 6.0  # original item render: 0.375 cubes (core and six arms) of grey wool
write(os.path.join(RES, "models/item/steam_line.json"), {"parent": "minecraft:block/block", "textures": {"line": "minecraft:block/gray_wool",
      "particle": "minecraft:block/gray_wool"}, "elements": [{"from": [8 - s3 / 2 + dx * s3 for dx in v], "to": [8 + s3 / 2 + dx * s3 for dx in v],
      "faces": {f: {"texture": "#line"} for f in FACES}} for v in ([0, 0, 0], [1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1])]})
put_lang("block.reactorcraft.steam_line", "machine.steamline")
write(os.path.join(recipes, "steam_line.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["NPN", "NPN", "NPN"],
      "key": {"N": {"tag": "minecraft:wool"}, "P": rc("rotarycraft:pipe")}, "result": {"id": "reactorcraft:steam_line", "count": 3}})
write(os.path.join(DATA, "reactorcraft/loot_table/blocks/steam_line.json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
      "entries": [{"type": "minecraft:item", "name": "reactorcraft:steam_line"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

machine("steam_grate", "machine.grate", ["steamgrate.png"], ["SIS", "p p", "SPS"],
        {"p": "rotarycraft:basepanel", "P": "rotarycraft:pipe", "S": "rotarycraft:steelingot", "I": "minecraft:iron_bars"})
machine("turbine_core", "machine.turbine", ["turbine.png"], ["BBB", "BCB", "BBB"], {"B": "rotarycraft:prop", "C": "reactorcraft:turbcore"})

# condenser; its recipe needs the RotaryCraft cooling fin, which is not ported yet (the recipe switches itself on once it exists)
machine("condenser", "machine.condenser", ["condenser.png"], ["SPS", "pRp", "FFF"],
        {"F": "rotarycraft:cooling_fin", "S": "rotarycraft:steelingot", "P": "rotarycraft:basepanel", "p": "rotarycraft:pipe", "R": "rotarycraft:reservoir"})
for fn in ("condenser.json", "condenser_worktable.json"):
    rp = os.path.join(recipes, fn)
    with open(rp) as f:
        r = json.load(f)
    r["neoforge:conditions"] = r.get("neoforge:conditions", []) + [{"type": "neoforge:item_exists", "item": "rotarycraft:cooling_fin"}]
    write(rp, r)
# waste: container (textured block, end textured), storage (modelled), decayer (column texture like the fuel core, top/bottom #4); GUIs
machine("waste_container", "machine.wastecontainer", [], ["SCS", "CcC", "SCS"],
        {"S": "rotarycraft:steelingot", "C": "rotarycraft:cooling_fin", "c": "minecraft:chest"})
for t in ("wastecontainer", "wastecontainer_top"):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", t + ".png"), os.path.join(RES, "textures/block", t + ".png"))
write(os.path.join(RES, "models/block/waste_container.json"), {"parent": "minecraft:block/cube_column",
      "textures": {"side": "reactorcraft:block/wastecontainer", "end": "reactorcraft:block/wastecontainer_top"}})
write(os.path.join(RES, "models/item/waste_container.json"), {"parent": "reactorcraft:block/waste_container"})
machine("waste_storage", "machine.storage", ["storage.png"], ["SPS", "PCP", "SPS"],
        {"S": "rotarycraft:steelingot", "P": "rotarycraft:basepanel", "C": "minecraft:chest"})
for i in range(5):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "wastedecayer_#%d.png" % i), os.path.join(RES, "textures/block", "wastedecayer_%d.png" % i))
variants_d = {}
for i in range(5):
    write(os.path.join(RES, "models/block/waste_decayer_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/wastedecayer_%d" % min(i, 3), "end": "reactorcraft:block/wastedecayer_4"}})
    variants_d["state=%d" % i] = {"model": "reactorcraft:block/waste_decayer_%d" % i}
write(os.path.join(RES, "blockstates/waste_decayer.json"), {"variants": variants_d})
write(os.path.join(RES, "models/item/waste_decayer.json"), {"parent": "reactorcraft:block/waste_decayer_0"})
put_lang("block.reactorcraft.waste_decayer", "machine.wastedecayer")
data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SHS", "GPG", "SHS"], "key": {"H": {"item": "minecraft:hopper"},
        "G": {"item": "rotarycraft:blast_glass"}, "P": {"item": "reactorcraft:waste_container"}, "S": {"item": "rotarycraft:silumin"}},
        "result": {"id": "reactorcraft:waste_decayer", "count": 1}}
wt = json.loads(json.dumps(data))
wt["type"] = "rotarycraft:worktable_shaped"
write(os.path.join(recipes, "waste_decayer_worktable.json"), wt)
data["neoforge:conditions"] = TABLE
write(os.path.join(recipes, "waste_decayer.json"), data)
for g in ("wastecontainer2.png", "wastestorage.png", "wastedecayer.png"):
    shutil.copyfile(os.path.join(SRC, "Textures/GUI", g), os.path.join(RES, "textures/gui", g))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:waste_container", "reactorcraft:waste_storage", "reactorcraft:waste_decayer"])
# breeder: breeder fuel and plutonium (sprites 98, 96), breeder core (column texture like the fuel core, ends #4), sodium heater (like the boiler)
for idx, iid, key in ((96, "plutonium", "item.plutonium"), (98, "breeder_fuel", "item.breeder")):
    sprite(idx, iid)
    put_lang("item.reactorcraft." + iid, key)
write(os.path.join(recipes, "breeder_fuel.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" D ", "DED", " D "],
      "key": {"D": {"item": "reactorcraft:depleted"}, "E": {"item": "reactorcraft:fuel"}}, "result": {"id": "reactorcraft:breeder_fuel", "count": 4}})
for i in range(5):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "breeder_#%d.png" % i), os.path.join(RES, "textures/block", "breeder_%d.png" % i))
for i in range(4):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "sodiumboiler_#%d.png" % i), os.path.join(RES, "textures/block", "sodiumboiler_%d.png" % i))
vb, vs = {}, {}
for i in range(5):
    write(os.path.join(RES, "models/block/breeder_core_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/breeder_%d" % min(i, 3), "end": "reactorcraft:block/breeder_4"}})
    write(os.path.join(RES, "models/block/sodium_heater_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/sodiumboiler_%d" % (i if i < 4 else 0), "end": "reactorcraft:block/sodiumboiler_0"}})
    vb["state=%d" % i] = {"model": "reactorcraft:block/breeder_core_%d" % i}
    vs["state=%d" % i] = {"model": "reactorcraft:block/sodium_heater_%d" % i}
write(os.path.join(RES, "blockstates/breeder_core.json"), {"variants": vb})
write(os.path.join(RES, "blockstates/sodium_heater.json"), {"variants": vs})
write(os.path.join(RES, "models/item/breeder_core.json"), {"parent": "reactorcraft:block/breeder_core_0"})
write(os.path.join(RES, "models/item/sodium_heater.json"), {"parent": "reactorcraft:block/sodium_heater_0"})
put_lang("block.reactorcraft.breeder_core", "machine.breedercore")
put_lang("block.reactorcraft.sodium_heater", "machine.sodiumboiler")
for mid, pattern, key in (("breeder_core", ["SPS", "PCP", "SPS"], {"P": "rotarycraft:basepanel", "S": "rotarycraft:steelingot", "C": "reactorcraft:fuel_rod"}),
                          ("sodium_heater", [" i ", "ibi", " i "], {"b": "reactorcraft:reactor_boiler", "i": "minecraft:iron_ingot"})):
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:breeder_core", "reactorcraft:sodium_heater"])
# tritizer (column texture like the breeder core, ends #4)
vt = {}
for i in range(5):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "tritizer_#%d.png" % i), os.path.join(RES, "textures/block", "tritizer_%d.png" % i))
for i in range(5):
    write(os.path.join(RES, "models/block/tritizer_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/tritizer_%d" % min(i, 3), "end": "reactorcraft:block/tritizer_4"}})
    vt["state=%d" % i] = {"model": "reactorcraft:block/tritizer_%d" % i}
write(os.path.join(RES, "blockstates/tritizer.json"), {"variants": vt})
write(os.path.join(RES, "models/item/tritizer.json"), {"parent": "reactorcraft:block/tritizer_0"})
put_lang("block.reactorcraft.tritizer", "machine.tritizer")
data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SPS", "GPG", "SPS"],
        "key": {"G": rc("rotarycraft:blast_glass"), "P": rc("rotarycraft:pipe"), "S": rc("rotarycraft:steelingot")}, "result": {"id": "reactorcraft:tritizer", "count": 1}}
wt = json.loads(json.dumps(data))
wt["type"] = "rotarycraft:worktable_shaped"
write(os.path.join(recipes, "tritizer_worktable.json"), wt)
data["neoforge:conditions"] = TABLE
write(os.path.join(recipes, "tritizer.json"), data)
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:tritizer"])
# HTGR: fuel pebble (sprite 102, 25 steps) and spent pebble (103), pebble bed (column texture, ends #4), CO2 heater (like the boiler), GUI
for idx, iid, key in ((102, "pellet", "item.pellet"), (103, "depleted_pellet", "item.depletedpellet")):
    sprite(idx, iid)
    put_lang("item.reactorcraft." + iid, key)
write(os.path.join(recipes, "pellet.json"), {"type": "rotarycraft:blast_crafting", "category": "misc", "pattern": [" G ", "GUG", " G "],
      "key": {"G": {"item": "reactorcraft:graphite"}, "U": {"item": "reactorcraft:udust"}}, "result": {"id": "reactorcraft:pellet", "count": 4},
      "temperature": 750, "speed": 1, "xp": 1})
shaped("fabric_pellet", "fabric", ["LDL", "LDL", "LDL"], {"D": "depleted_pellet", "L": "minecraft:leather"}, 1)
for i in range(5):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "pebblebed_#%d.png" % i), os.path.join(RES, "textures/block", "pebblebed_%d.png" % i))
for i in range(4):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "co2heater_#%d.png" % i), os.path.join(RES, "textures/block", "co2heater_%d.png" % i))
vp, vc = {}, {}
for i in range(5):
    write(os.path.join(RES, "models/block/pebble_bed_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/pebblebed_%d" % min(i, 3), "end": "reactorcraft:block/pebblebed_4"}})
    write(os.path.join(RES, "models/block/co2_heater_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/co2heater_%d" % (i if i < 4 else 0), "end": "reactorcraft:block/co2heater_0"}})
    vp["state=%d" % i] = {"model": "reactorcraft:block/pebble_bed_%d" % i}
    vc["state=%d" % i] = {"model": "reactorcraft:block/co2_heater_%d" % i}
write(os.path.join(RES, "blockstates/pebble_bed.json"), {"variants": vp})
write(os.path.join(RES, "blockstates/co2_heater.json"), {"variants": vc})
write(os.path.join(RES, "models/item/pebble_bed.json"), {"parent": "reactorcraft:block/pebble_bed_0"})
write(os.path.join(RES, "models/item/co2_heater.json"), {"parent": "reactorcraft:block/co2_heater_0"})
put_lang("block.reactorcraft.pebble_bed", "machine.pebblebed")
put_lang("block.reactorcraft.co2_heater", "machine.co2heater")
for mid, pattern, key in (("pebble_bed", ["SHS", "PCP", "SHS"], {"H": "minecraft:hopper", "P": "rotarycraft:basepanel", "S": "rotarycraft:steelingot", "C": "reactorcraft:fuel_rod"}),
                          ("co2_heater", [" i ", "ibi", " i "], {"b": "reactorcraft:reactor_boiler", "i": "rotarycraft:basepanel"})):
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)
shutil.copyfile(os.path.join(SRC, "Textures/GUI/pebblegui.png"), os.path.join(RES, "textures/gui/pebblegui.png"))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:pebble_bed", "reactorcraft:co2_heater"])
# thorium: thorium core (column texture, ends #4), emergency fuel dump (own top/bottom), waste pipe (RotaryCraft pipe body with concrete
# frame and spruce leaf windows, original TileEntityWastePipe icons), thorium fuel layers (LiFBe fuel texture), fuel pool GUI
for i in range(5):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", "thorium_#%d.png" % i), os.path.join(RES, "textures/block", "thorium_%d.png" % i))
for t in ("fueldump", "fueldump_top", "fueldump_bottom"):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", t + ".png"), os.path.join(RES, "textures/block", t + ".png"))
vt, vf = {}, {}
for i in range(5):
    write(os.path.join(RES, "models/block/thorium_core_%d.json" % i), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "reactorcraft:block/thorium_%d" % min(i, 3), "end": "reactorcraft:block/thorium_4"}})
    vt["state=%d" % i] = {"model": "reactorcraft:block/thorium_core_%d" % i}
    vf["state=%d" % i] = {"model": "reactorcraft:block/fuel_dump"}
write(os.path.join(RES, "models/block/fuel_dump.json"), {"parent": "minecraft:block/cube_bottom_top",
      "textures": {"side": "reactorcraft:block/fueldump", "top": "reactorcraft:block/fueldump_top", "bottom": "reactorcraft:block/fueldump_bottom"}})
write(os.path.join(RES, "blockstates/thorium_core.json"), {"variants": vt})
write(os.path.join(RES, "blockstates/fuel_dump.json"), {"variants": vf})
write(os.path.join(RES, "models/item/thorium_core.json"), {"parent": "reactorcraft:block/thorium_core_0"})
write(os.path.join(RES, "models/item/fuel_dump.json"), {"parent": "reactorcraft:block/fuel_dump"})
put_lang("block.reactorcraft.thorium_core", "machine.thorium")
put_lang("block.reactorcraft.fuel_dump", "machine.fueldump")
put_lang("block.reactorcraft.waste_pipe", "machine.wastepipe")
# neutron reflector (ReactorTiles.REFLECTOR, ReactorBlocks.REACTOR: icon reflector, recipe GGG/GSG/GGG graphite around a steel block via
# ReactorTiles.addCrafting = Worktable, crafting table only with table_machines). Added 2026-11-04, see PORTING_NOTES.md.
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/reflector.png"), os.path.join(RES, "textures/block/reflector.png"))
write(os.path.join(RES, "blockstates/neutron_reflector.json"), {"variants": {"": {"model": "reactorcraft:block/neutron_reflector"}}})
write(os.path.join(RES, "models/block/neutron_reflector.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/reflector"}})
write(os.path.join(RES, "models/item/neutron_reflector.json"), {"parent": "reactorcraft:block/neutron_reflector"})
put_lang("block.reactorcraft.neutron_reflector", "machine.reflector")
reflector = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "GSG", "GGG"],
             "key": {"G": {"item": "reactorcraft:graphite"}, "S": {"item": "rotarycraft:steel_block"}}, "result": {"id": "reactorcraft:neutron_reflector", "count": 1}}
write(os.path.join(recipes, "neutron_reflector_worktable.json"), dict(reflector, type="rotarycraft:worktable_shaped"))
write(os.path.join(recipes, "neutron_reflector.json"), dict(reflector, **{"neoforge:conditions": TABLE}))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:neutron_reflector"])
# turbine dynamometer (ReactorTiles.TURBINEMETER, ReactorBlocks.MACHINE: getTextureState 0 bottom, 1 sides, 2 top -> turbinemeter_#0..2;
# recipe SrS/PGP/PCP via ReactorTiles.addCrafting). Added 2026-11-09, see PORTING_NOTES.md.
for i in range(3):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/turbinemeter_#%d.png" % i), os.path.join(RES, "textures/block/turbinemeter_%d.png" % i))
write(os.path.join(RES, "blockstates/turbine_meter.json"), {"variants": {"": {"model": "reactorcraft:block/turbine_meter"}}})
write(os.path.join(RES, "models/block/turbine_meter.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
      "bottom": "reactorcraft:block/turbinemeter_0", "side": "reactorcraft:block/turbinemeter_1", "top": "reactorcraft:block/turbinemeter_2"}})
write(os.path.join(RES, "models/item/turbine_meter.json"), {"parent": "reactorcraft:block/turbine_meter"})
put_lang("block.reactorcraft.turbine_meter", "machine.turbinemeter")
meter = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SrS", "PGP", "PCP"],
         "key": {"S": {"item": "rotarycraft:steelingot"}, "r": {"item": "minecraft:redstone"}, "P": {"item": "rotarycraft:basepanel"},
                 "G": {"item": "minecraft:glowstone"}, "C": {"item": "rotarycraft:pcb"}}, "result": {"id": "reactorcraft:turbine_meter", "count": 1}}
write(os.path.join(recipes, "turbine_meter_worktable.json"), dict(meter, type="rotarycraft:worktable_shaped"))
write(os.path.join(recipes, "turbine_meter.json"), dict(meter, **{"neoforge:conditions": TABLE}))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:turbine_meter"])
# steam diffuser (ReactorTiles.DIFFUSER, BlockReactorTileModelled, RenderSteamDiffuser with ModelDiffuser and diffuser.png; recipe BBB/DPD/BBB).
# Added 2026-11-09, see PORTING_NOTES.md.
machine("steam_diffuser", "machine.steamdiffuser", ["diffuser.png"], ["BBB", "DPD", "BBB"],
        {"B": "rotarycraft:basepanel", "D": "rotarycraft:diffuser", "P": "rotarycraft:pipe"})
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:steam_diffuser"])
for c in LANGS:
    NEW[c]["block.reactorcraft.thorium_fuel"] = "Thorium Fuel"
for mid, pattern, key in (("thorium_core", ["aSa", "PCP", "tPt"], {"t": "rotarycraft:tungsteningot", "a": "rotarycraft:silumin", "P": "rotarycraft:basepanel",
                                                                     "S": "rotarycraft:steelingot", "C": "reactorcraft:fuel_rod"}),
                          ("fuel_dump", ["pIp", "BPB", "pbp"], {"b": "minecraft:iron_bars", "p": "rotarycraft:pipe", "P": "rotarycraft:bedrock_pipe",
                                                                "B": "rotarycraft:basepanel", "I": "rotarycraft:impeller"})):
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {k: rc(v) if isinstance(v, str) else v for k, v in key.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)
for d, n in zip((1, 2, 3), (32, 16, 8)):   # DifficultyEffects.PIPECRAFT
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["CbC", "CGC", "CbC"], "key": {"C": {"item": "reactorcraft:concrete"},
            "b": {"item": "minecraft:iron_bars"}, "G": {"item": "minecraft:glass"}}, "result": {"id": "reactorcraft:waste_pipe", "count": n}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    wt["neoforge:conditions"] = [{"type": "rotarycraft:difficulty", "value": d}]
    write(os.path.join(recipes, "waste_pipe_difficulty_%d_worktable.json" % d), wt)
    data["neoforge:conditions"] = TABLE + [{"type": "rotarycraft:difficulty", "value": d}]
    write(os.path.join(recipes, "waste_pipe_difficulty_%d.json" % d), data)
with open(os.path.join(PORT, "rotarycraft/src/main/resources/assets/rotarycraft/blockstates/pipe.json")) as f:
    mp = json.load(f)["multipart"]
os.makedirs(os.path.join(RES, "models/block/waste_pipe"), exist_ok=True)
wtex = {"pipe": "reactorcraft:block/concrete", "glass": "minecraft:block/spruce_leaves", "particle": "reactorcraft:block/concrete"}
inv_els = []
for part in mp:
    name = part["apply"]["model"].split("/")[-1]
    with open(os.path.join(PORT, "rotarycraft/src/main/resources/assets/rotarycraft/models/block/pipe", name + ".json")) as f:
        m = json.load(f)
    m["textures"] = wtex
    write(os.path.join(RES, "models/block/waste_pipe", name + ".json"), m)
    part["apply"]["model"] = "reactorcraft:block/waste_pipe/" + name
    if all(v == "false" for v in part["when"].values()):
        inv_els += m["elements"]
write(os.path.join(RES, "blockstates/waste_pipe.json"), {"multipart": mp})
write(os.path.join(RES, "models/block/waste_pipe.json"), {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": wtex, "elements": inv_els})
write(os.path.join(RES, "models/item/waste_pipe.json"), {"parent": "reactorcraft:block/waste_pipe"})
write(os.path.join(DATA, "reactorcraft/loot_table/blocks/waste_pipe.json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
      "entries": [{"type": "minecraft:item", "name": "reactorcraft:waste_pipe"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
vth = {}
for i in range(8):
    write(os.path.join(RES, "models/block/thorium_fuel_%d.json" % i), {"parent": "minecraft:block/block", "textures": {"fuel": "reactorcraft:block/fluid/lifbe_fuel",
          "particle": "reactorcraft:block/fluid/lifbe_fuel"}, "elements": [{"from": [0, 0, 0], "to": [16, 2 * (i + 1), 16],
          "faces": {f: {"texture": "#fuel"} for f in ("down", "up", "north", "south", "west", "east")}}]})
    vth["level=%d" % i] = {"model": "reactorcraft:block/thorium_fuel_%d" % i}
write(os.path.join(RES, "blockstates/thorium_fuel.json"), {"variants": vth})
write(os.path.join(RES, "models/item/thorium_fuel.json"), {"parent": "reactorcraft:block/thorium_fuel_7"})
shutil.copyfile(os.path.join(SRC, "Textures/GUI/fuelpool.png"), os.path.join(RES, "textures/gui/fuelpool.png"))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:thorium_core", "reactorcraft:fuel_dump", "reactorcraft:waste_pipe"])
# radiation tools: hazmat suit (sprites 112-115, armour layers haz_1/haz_2), goggles (64, no recipe in the original), Geiger counter
# (116), radiation cloud texture for the goggles view
for idx, iid, key in ((112, "hazmat_helmet", "item.hazhelmet"), (113, "hazmat_chestplate", "item.hazchest"), (114, "hazmat_leggings", "item.hazlegs"),
                      (115, "hazmat_boots", "item.hazboots"), (64, "goggles", "item.goggles"), (116, "geiger", "item.geiger")):
    sprite(idx, iid)
    put_lang("item.reactorcraft." + iid, key)
os.makedirs(os.path.join(RES, "textures/models/armor"), exist_ok=True)
for n in (1, 2):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/models/armor/haz_%d.png" % n), os.path.join(RES, "textures/models/armor/haz_layer_%d.png" % n))
os.makedirs(os.path.join(RES, "textures/entity"), exist_ok=True)
shutil.copyfile(os.path.join(SRC, "Textures/radiation2.png"), os.path.join(RES, "textures/entity/radiation2.png"))
for iid, pattern in (("hazmat_helmet", ["FFF", "F F"]), ("hazmat_chestplate", ["F F", "FFF", "FFF"]), ("hazmat_leggings", ["FFF", "F F", "F F"]),
                     ("hazmat_boots", ["F F", "F F"])):
    shaped(iid, iid, pattern, {"F": "fabric"})
shaped("geiger", "geiger", [" r ", "sSs", "sgs"], {"g": "rotarycraft:steel_gear", "s": "rotarycraft:steelingot", "S": "rotarycraft:screen", "r": "rotarycraft:radar"})
# radiation cleaner (sprite 99) and iron finder (117)
for idx, iid, key in ((99, "cleaner", "item.cleaner"), (117, "ironfinder", "item.ironfinder")):
    sprite(idx, iid)
    put_lang("item.reactorcraft." + iid, key)
shaped("cleaner", "cleaner", [" sp", "sbs", "ss "], {"b": "minecraft:water_bucket", "s": "rotarycraft:steelingot", "p": "rotarycraft:pipe"})
shaped("ironfinder", "ironfinder", ["L L", "S S", "SSS"], {"S": "rotarycraft:steelingot", "L": "reactorcraft:magnetite"})
# CPU remote control (sprite 101)
sprite(101, "remotecpu")
put_lang("item.reactorcraft.remotecpu", "item.remotecpu")
shaped("remotecpu", "remotecpu", ["SES", "BCB", "BPB"], {"S": "rotarycraft:steelingot", "P": "rotarycraft:basepanel", "B": "minecraft:stone_button",
                                                         "E": "minecraft:ender_pearl", "C": "rotarycraft:pcb"})
tag("minecraft", "item", "head_armor", ["reactorcraft:hazmat_helmet"])
tag("minecraft", "item", "chest_armor", ["reactorcraft:hazmat_chestplate"])
tag("minecraft", "item", "leg_armor", ["reactorcraft:hazmat_leggings"])
tag("minecraft", "item", "foot_armor", ["reactorcraft:hazmat_boots"])
machine("heat_exchanger", "machine.exchanger", ["exchanger2.png"], ["FPF", "GIG", "FPF"],
        {"P": "rotarycraft:pipe", "I": "rotarycraft:impeller", "G": "minecraft:gold_ingot", "F": "rotarycraft:cooling_fin"})
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:heat_exchanger"])
# high pressure turbine (modelled core) and its multiblock parts (multi/turbine_0..2 unformed; formed parts are invisible, icon 5)
machine("hip_turbine", "machine.bigturbine", ["bigturbine.png"], ["BBB", "BCB", "BBB"], {"B": "rotarycraft:prop", "C": "reactorcraft:turbine_core"})
os.makedirs(os.path.join(RES, "textures/block/multi"), exist_ok=True)
for i in range(6):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/multi/turbine_%d.png" % i), os.path.join(RES, "textures/block/multi/turbine_%d.png" % i))
for v, mid in enumerate(("turbine_blades", "turbine_housing", "steam_injector")):
    write(os.path.join(RES, "models/block", mid + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/multi/turbine_%d" % v}})
    write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"formed=false": {"model": "reactorcraft:block/" + mid}, "formed=true": {"model": "reactorcraft:block/" + mid}}})
    write(os.path.join(RES, "models/item", mid + ".json"), {"parent": "reactorcraft:block/" + mid})
    put_lang("block.reactorcraft." + mid, "multiblock.turbine.%d" % v)
shaped("turbine_blades", "turbine_blades", ["sis", "ici", "sis"], {"s": "rotarycraft:prop", "c": "rotarycraft:springtungsten", "i": "rotarycraft:silumin"})
shaped("turbine_housing", "turbine_housing", ["BBB", "SSS", "ppp"], {"B": "rotarycraft:basepanel", "S": "rotarycraft:steelingot", "p": "rotarycraft:prop"})
# the steam injector needs the RotaryCraft pipe pump; the recipe stays guarded by item_exists
shaped("steam_injector", "steam_injector", ["PbP", "bIb", "PbP"], {"P": "rotarycraft:pipe", "b": "rotarycraft:basepanel", "I": "rotarycraft:pipe_pump"},
       cond=[{"type": "neoforge:item_exists", "item": "rotarycraft:pipe_pump"}])
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:hip_turbine", "reactorcraft:turbine_blades", "reactorcraft:turbine_housing", "reactorcraft:steam_injector"])
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/steam.png"), os.path.join(RES, "textures/block/steam.png"))
write(os.path.join(RES, "models/block/steam.json"), {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
      "textures": {"all": "reactorcraft:block/steam"}})
write(os.path.join(RES, "blockstates/steam.json"), {"variants": {"": {"model": "reactorcraft:block/steam"}}})
for c in LANGS:
    NEW[c]["block.reactorcraft.steam"] = "Steam"
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:reactor_boiler", "reactorcraft:steam_grate", "reactorcraft:turbine_core", "reactorcraft:condenser"])
shutil.copyfile(os.path.join(SRC, "Sounds", "turbine-vol.ogg"), os.path.join(RES, "sounds", "turbine-vol.ogg"))
write(os.path.join(RES, "sounds.json"), {"control": {"sounds": [{"name": "reactorcraft:control"}]}, "scram": {"sounds": [{"name": "reactorcraft:scram"}]},
      "turbine": {"sounds": [{"name": "reactorcraft:turbine-vol"}]}, "gen_rf": {"sounds": [{"name": "reactorcraft:gen_rf"}]},
      "gen_elc": {"sounds": [{"name": "reactorcraft:gen_elc"}]}})
# reactor generator (modelled, one texture per output mode) and its multiblock parts (multi/generator_*: 0 core, 1 windings, 2 housing,
# 5 steam bypass; 9 formed = invisible; 10..12 the unformed generator per mode). The EU mode's sound is not needed (no IC2).
machine("reactor_generator", "machine.reactorgenerator", ["generator0.png", "generator1.png", "generator2.png"], ["RGR", "GFG", "RGR"],
        {"G": "reactorcraft:wire", "R": "minecraft:redstone", "F": "reactorcraft:magnetcore"})
for snd in ("gen_rf", "gen_elc"):
    shutil.copyfile(os.path.join(SRC, "Sounds", snd + ".ogg"), os.path.join(RES, "sounds", snd + ".ogg"))
for i in range(13):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/multi/generator_%d.png" % i), os.path.join(RES, "textures/block/multi/generator_%d.png" % i))
for v, (mid, t) in enumerate((("generator_core", 0), ("generator_winding", 1), ("generator_housing", 2), ("steam_bypass", 5))):
    write(os.path.join(RES, "models/block", mid + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/multi/generator_%d" % t}})
    write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"formed=false": {"model": "reactorcraft:block/" + mid}, "formed=true": {"model": "reactorcraft:block/" + mid}}})
    write(os.path.join(RES, "models/item", mid + ".json"), {"parent": "reactorcraft:block/" + mid})
    put_lang("block.reactorcraft." + mid, "multiblock.generator.%d" % v)
shaped("generator_core", "generator_core", ["SsS", "scs", "SsS"], {"c": "rotarycraft:bedrock_shaftcore", "S": "rotarycraft:steelingot", "s": "rotarycraft:steel_shaft"})
shaped("generator_winding", "generator_winding", ["SSS", "SBS", "SSS"], {"S": "reactorcraft:wire", "B": "rotarycraft:steelingot"})
shaped("generator_housing", "generator_housing", ["SSS", "MMM", "SSS"], {"M": "reactorcraft:wire", "S": "rotarycraft:steelingot"})
shaped("steam_bypass", "steam_bypass", ["W W", " S ", "W W"], {"W": "minecraft:black_wool", "S": "rotarycraft:steelingot"})
# electrolyzer (modelled, GUI; runs on Van de Graaff discharges)
machine("electrolyzer", "machine.electrolyzer", ["electrolyzer.png"], ["SPS", "PRP", "BPB"],
        {"P": "rotarycraft:pipe", "B": "rotarycraft:basepanel", "S": "rotarycraft:steelingot", "R": "rotarycraft:reservoir"})
shutil.copyfile(os.path.join(SRC, "Textures/GUI", "electrolyzer.png"), os.path.join(RES, "textures/gui", "electrolyzer.png"))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:electrolyzer"])
# solar exchanger and sodium solar receiver (modelled; the receiver's flare is an animated block texture)
machine("solar_exchanger", "machine.solarexchange", ["solar.png"], ["sPs", "pEp", "sPs"],
        {"p": "rotarycraft:pipe", "E": "reactorcraft:heat_exchanger", "P": "rotarycraft:basepanel", "s": "rotarycraft:steelingot"})
machine("solar_top", "machine.solartop", ["solartop.png"], ["aPa", "tct", "sPs"],
        {"a": "reactorcraft:alloy", "t": "rotarycraft:tungsteningot", "c": "rotarycraft:condenser", "P": "rotarycraft:basepanel", "s": "rotarycraft:steelingot"})
for fn in ("solarflare.png", "solarflare.png.mcmeta"):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", fn), os.path.join(RES, "textures/block", fn))
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:solar_exchanger", "reactorcraft:solar_top"])
# turbine flywheel (modelled) and its ring (multi/flywheel_0..2; 3 formed = invisible). The recipes take RotaryCraft's steel block
# (BlockRegistry.DECO, ItemStacks.steelblock - the exact block as in the original, ported 2026-11-03)
STEELBLOCK = {"item": "rotarycraft:steel_block"}
machine("turbine_flywheel", "machine.turbinewheel", ["flywheel.png"], ["BBB", "STS", "BBB"],
        {"B": STEELBLOCK, "S": "rotarycraft:steel_shaft", "T": "rotarycraft:tungsten_shaftcore"})
for i in range(4):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/multi/flywheel_%d.png" % i), os.path.join(RES, "textures/block/multi/flywheel_%d.png" % i))
for v, mid in enumerate(("flywheel_core", "flywheel_dampener", "flywheel_frame")):
    write(os.path.join(RES, "models/block", mid + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/multi/flywheel_%d" % v}})
    write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"formed=false": {"model": "reactorcraft:block/" + mid}, "formed=true": {"model": "reactorcraft:block/" + mid}}})
    write(os.path.join(RES, "models/item", mid + ".json"), {"parent": "reactorcraft:block/" + mid})
    put_lang("block.reactorcraft." + mid, "multiblock.flywheel.%d" % v)
shaped("flywheel_core", "flywheel_core", ["sSs", "SCS", "sSs"], {"s": "rotarycraft:springtungsten", "S": STEELBLOCK, "C": "rotarycraft:bedrock_unit16"})
shaped("flywheel_dampener", "flywheel_dampener", ["WWW", "SSS", "WWW"], {"W": {"tag": "minecraft:wool"}, "S": "rotarycraft:steelingot"})
shaped("flywheel_frame", "flywheel_frame", ["sSs", "SSS", "sSs"], {"s": "rotarycraft:basepanel", "S": "rotarycraft:springingot"})
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:turbine_flywheel", "reactorcraft:flywheel_core", "reactorcraft:flywheel_dampener",
                                              "reactorcraft:flywheel_frame"])
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:reactor_generator", "reactorcraft:generator_core", "reactorcraft:generator_winding",
                                              "reactorcraft:generator_housing", "reactorcraft:steam_bypass"])


# ---------------------------------------------------------------- fusion reactor
# modelled machines: toroid magnet, solenoid, gas collector, fusion marker (all with the 4 "facing" block states of FusionMachineBlock)
def fusion_machine(mid, lang_key, textures, pattern, key):
    machine(mid, lang_key, textures, pattern, key)
    write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"facing=%s" % f: {"model": "reactorcraft:block/" + mid} for f in ("north", "south", "west", "east")}})
fusion_machine("toroid_magnet", "machine.magnet", ["magnet.png"], ["MCM", "CHC", "MCM"],
               {"H": "reactorcraft:hysteresisring", "M": "reactorcraft:magnetcore", "C": "reactorcraft:coolant"})
fusion_machine("solenoid", "machine.solenoid", ["solenoid.png"], ["SPS", "MCM", "IGI"],
               {"S": "rotarycraft:steelingot", "P": "rotarycraft:basepanel", "M": "reactorcraft:magnetic", "C": "reactorcraft:magnetcore",
                "I": "reactorcraft:ferroingot", "G": "rotarycraft:tungsten_unit16"})
fusion_machine("gas_collector", "machine.collector", ["co2collector.png"], [" p ", "SpS", "PpP"],
               {"p": "rotarycraft:pipe", "P": "rotarycraft:basepanel", "S": "rotarycraft:steelingot"})
fusion_machine("fusion_marker", "machine.fusionmarker", [], ["F", "R"], {"F": "reactorcraft:blue_fluorite", "R": "minecraft:redstone_torch"})
# textured machines (ReactorBlocks.REACTOR): injector (injector_#0 on its facing side, #2 elsewhere), preheater, neutron absorber
for k in range(3):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/injector_#%d.png" % k), os.path.join(RES, "textures/block/injector_%d.png" % k))
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/heater.png"), os.path.join(RES, "textures/block/fusion_heater.png"))
shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/absorber.png"), os.path.join(RES, "textures/block/neutron_absorber.png"))
write(os.path.join(RES, "models/block/fusion_injector.json"), {"parent": "minecraft:block/cube", "textures": {"particle": "reactorcraft:block/injector_2",
      "north": "reactorcraft:block/injector_0", "south": "reactorcraft:block/injector_2", "east": "reactorcraft:block/injector_2",
      "west": "reactorcraft:block/injector_2", "up": "reactorcraft:block/injector_2", "down": "reactorcraft:block/injector_2"}})
write(os.path.join(RES, "blockstates/fusion_injector.json"), {"variants": {"facing=%s" % f: {"model": "reactorcraft:block/fusion_injector", "y": r}
      for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
for mid in ("fusion_heater", "neutron_absorber"):
    write(os.path.join(RES, "models/block", mid + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/" + mid}})
    write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"facing=%s" % f: {"model": "reactorcraft:block/" + mid} for f in ("north", "south", "west", "east")}})
for mid, key, pattern, k in (("fusion_injector", "machine.injector", ["PMP", "M M", "PMP"], {"P": "reactorcraft:magnetic_pipe", "M": "reactorcraft:magnetic"}),
                             ("fusion_heater", "machine.fusionheater", ["MPM", "P P", "MPM"], {"M": "reactorcraft:ferroingot", "P": "rotarycraft:blast_glass"}),
                             ("neutron_absorber", "machine.absorber", [" P ", "PCP", " P "], {"C": STEELBLOCK, "P": "reactorcraft:depleted"})):
    write(os.path.join(RES, "models/item", mid + ".json"), {"parent": "reactorcraft:block/" + mid})
    put_lang("block.reactorcraft." + mid, key)
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": {a: rc(v) if isinstance(v, str) else v for a, v in k.items()},
            "result": {"id": "reactorcraft:" + mid, "count": 1}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    write(os.path.join(recipes, mid + "_worktable.json"), wt)
    data["neoforge:conditions"] = TABLE
    write(os.path.join(recipes, mid + ".json"), data)
data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "PCP", "PPP"], "key": {"C": STEELBLOCK, "P": rc("reactorcraft:depleted_pellet")},
        "result": {"id": "reactorcraft:neutron_absorber", "count": 1}}
wt = json.loads(json.dumps(data))
wt["type"] = "rotarycraft:worktable_shaped"
write(os.path.join(recipes, "neutron_absorber_pellets_worktable.json"), wt)
data["neoforge:conditions"] = TABLE
write(os.path.join(recipes, "neutron_absorber_pellets.json"), data)
# magnetic containment pipe: gold frame (glowing gold while charged), blast glass windows; DifficultyEffects.PIPECRAFT
for fn in ("glowgold.png", "glowgold.png.mcmeta"):
    shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks", fn), os.path.join(RES, "textures/block", fn))
with open(os.path.join(PORT, "rotarycraft/src/main/resources/assets/rotarycraft/blockstates/pipe.json")) as f:
    mp0 = json.load(f)["multipart"]
mp = []
inv_els = []
for charged, sub, frame in ((False, "magnetic_pipe", "minecraft:block/gold_block"), (True, "magnetic_pipe_charged", "reactorcraft:block/glowgold")):
    os.makedirs(os.path.join(RES, "models/block", sub), exist_ok=True)
    mtex = {"pipe": frame, "glass": "rotarycraft:block/obsidiglass", "particle": "minecraft:block/gold_block"}
    for part in mp0:
        name = part["apply"]["model"].split("/")[-1]
        with open(os.path.join(PORT, "rotarycraft/src/main/resources/assets/rotarycraft/models/block/pipe", name + ".json")) as f:
            m = json.load(f)
        m["textures"] = mtex
        write(os.path.join(RES, "models/block", sub, name + ".json"), m)
        when = dict(part["when"])
        when["charged"] = "true" if charged else "false"
        mp.append({"when": when, "apply": {"model": "reactorcraft:block/%s/%s" % (sub, name)}})
        if not charged and all(v == "false" for v in part["when"].values()):
            inv_els += m["elements"]
write(os.path.join(RES, "blockstates/magnetic_pipe.json"), {"multipart": mp})
write(os.path.join(RES, "models/block/magnetic_pipe.json"), {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
      "textures": {"pipe": "minecraft:block/gold_block", "glass": "rotarycraft:block/obsidiglass", "particle": "minecraft:block/gold_block"}, "elements": inv_els})
write(os.path.join(RES, "models/item/magnetic_pipe.json"), {"parent": "reactorcraft:block/magnetic_pipe"})
write(os.path.join(DATA, "reactorcraft/loot_table/blocks/magnetic_pipe.json"), {"type": "minecraft:block", "pools": [{"rolls": 1,
      "entries": [{"type": "minecraft:item", "name": "reactorcraft:magnetic_pipe"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
put_lang("block.reactorcraft.magnetic_pipe", "machine.magnetpipe")
for d, n in zip((1, 2, 3), (32, 16, 8)):
    data = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["CGC", "CGC", "CGC"], "key": {"C": {"item": "minecraft:gold_ingot"},
            "G": {"item": "rotarycraft:blast_glass"}}, "result": {"id": "reactorcraft:magnetic_pipe", "count": n}}
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped"
    wt["neoforge:conditions"] = [{"type": "rotarycraft:difficulty", "value": d}]
    write(os.path.join(recipes, "magnetic_pipe_difficulty_%d_worktable.json" % d), wt)
    data["neoforge:conditions"] = TABLE + [{"type": "rotarycraft:difficulty", "value": d}]
    write(os.path.join(recipes, "magnetic_pipe_difficulty_%d.json" % d), data)
# fusion multiblock parts (multi/solenoid_*, injector_*, heater_*): the original item textures per side; formed solenoid parts multi/solenoid_10
SOL = ["solenoid_base", "solenoid_linkage", "solenoid_central_magnet", "solenoid_auxiliary_magnet", "solenoid_hysteresis_rod", "solenoid_hub"]
INJ = ["injector_base", "injector_lower_corner", "injector_side_panel", "injector_top", "injector_upper_corner", "injector_induction_coil", "injector_column",
       "injector_hysteresis_core"]
HEA = ["heater_lens", "heater_insulation_core", "heater_housing_corner", "heater_housing_edge", "heater_housing_face"]
def sol_tex(m, side):   # BlockSolenoidMulti.getItemTextureIndex
    if side < 2:
        return 3 if (m < 4 or m == 5) else 9
    return {2: 11, 3: 2, 5: 6, 4: 8}.get(m, m)
def inj_tex(m, side):   # BlockInjectorMulti.getItemTextureIndex
    if m == 0: return 9 if side == 0 else 0 if side == 1 else 4
    if m == 1: return 0 if side == 1 else 9 if side == 0 else 28
    if m == 4: return 0 if side == 0 else 9 if side == 1 else 27
    if m == 3: return 9 if side == 1 else 0 if side == 0 else 3
    if m == 6: return 21 if side < 2 else 26
    return {2: 9, 7: 0, 5: 22}[m]
def hea_tex(m, side):   # BlockHeaterMulti.getItemTextureIndex
    return {2: 11, 3: 12, 4: 10}.get(m, m)
for base, names, fn, n in (("solenoid", SOL, sol_tex, 12), ("injector", INJ, inj_tex, 29), ("heater", HEA, hea_tex, 13)):
    for i in range(n):
        shutil.copyfile(os.path.join(JAR, "assets/reactorcraft/textures/blocks/multi/%s_%d.png" % (base, i)), os.path.join(RES, "textures/block/multi/%s_%d.png" % (base, i)))
    for v, mid in enumerate(names):
        t = lambda side: "reactorcraft:block/multi/%s_%d" % (base, fn(v, side))
        model = {"parent": "minecraft:block/cube", "textures": {"particle": t(2), "down": t(0), "up": t(1), "north": t(2), "south": t(3), "west": t(4), "east": t(5)}}
        if base == "heater" and v == 0:
            model["render_type"] = "minecraft:cutout"
        write(os.path.join(RES, "models/block", mid + ".json"), model)
        formed = "reactorcraft:block/" + mid
        if base == "solenoid":
            formed = "reactorcraft:block/solenoid_formed"
        write(os.path.join(RES, "blockstates", mid + ".json"), {"variants": {"formed=false": {"model": "reactorcraft:block/" + mid}, "formed=true": {"model": formed}}})
        write(os.path.join(RES, "models/item", mid + ".json"), {"parent": "reactorcraft:block/" + mid})
        put_lang("block.reactorcraft." + mid, "multiblock.%s.%d" % (base, v))
write(os.path.join(RES, "models/block/solenoid_formed.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "reactorcraft:block/multi/solenoid_10"}})
INSUL = "reactorcraft:heater_insulation_core"
shaped("heater_lens", "heater_lens", ["SBS", "BLB", "SBS"], {"B": "rotarycraft:blast_glass", "S": "rotarycraft:tungsteningot", "L": "rotarycraft:lens"})
shaped("heater_insulation_core", "heater_insulation_core", ["WWW", "WSW", "WWW"], {"W": {"tag": "minecraft:wool"}, "S": "rotarycraft:steelingot"})
shaped("heater_housing_corner", "heater_housing_corner", ["SOS", "OSO", "SOS"], {"O": INSUL, "S": "rotarycraft:steelingot"})
shaped("heater_housing_edge", "heater_housing_edge", ["OSO", "SSS", "OSO"], {"O": INSUL, "S": "rotarycraft:steelingot"})
shaped("heater_housing_face", "heater_housing_face", ["SSS", "SOS", "SSS"], {"O": INSUL, "S": "rotarycraft:steelingot"})
for mid, pattern, hyst in (("injector_base", ["WWW", "HHH", "MMM"], "hysteresis"), ("injector_lower_corner", ["MWM", "MHM", "MMM"], "hysteresis"),
                           ("injector_side_panel", ["WMW", "MHM", "WMW"], "hysteresis"), ("injector_top", ["MMM", "HHH", "WWW"], "hysteresis"),
                           ("injector_upper_corner", ["MMM", "MHM", "MWM"], "hysteresis"), ("injector_induction_coil", ["MWM", "HHH", "MWM"], "wire"),
                           ("injector_column", ["MWM", "MHM", "MWM"], "hysteresis"), ("injector_hysteresis_core", ["HWH", "WMW", "HWH"], "hysteresis")):
    shaped(mid, mid, pattern, {"H": "reactorcraft:" + hyst, "M": "reactorcraft:magnetic", "W": INSUL})
shaped("solenoid_base", "solenoid_base", ["SSS", "SSS", "SSS"], {"S": "reactorcraft:ferroingot"})
shaped("solenoid_linkage", "solenoid_linkage", ["SSS", "SBS", "SSS"], {"S": "reactorcraft:ferroingot", "B": "rotarycraft:steelingot"})
shaped("solenoid_central_magnet", "solenoid_central_magnet", ["SSS", "MMM", "SSS"], {"M": "reactorcraft:magnet_7", "S": "reactorcraft:magnetic"})
shaped("solenoid_auxiliary_magnet", "solenoid_auxiliary_magnet", ["SSS", "MMM", "SSS"], {"M": "reactorcraft:magnet_6", "S": "reactorcraft:magnetic"})
shaped("solenoid_hysteresis_rod", "solenoid_hysteresis_rod", ["SSS", "MMM", "SSS"], {"M": "reactorcraft:ferroingot", "S": "reactorcraft:hysteresis"})
shaped("solenoid_hub", "solenoid_hub", ["SSS", "WPW", "SSS"], {"W": "reactorcraft:wire", "P": "reactorcraft:magnetic", "S": "reactorcraft:ferroingot"})
# permanent magnets (ReactorItems.MAGNET, one sprite: 100)
for i in range(8):
    sprite(100, "magnet_%d" % i)
    put_lang("item.reactorcraft.magnet_%d" % i, "item.magnet")
# plasma and fusion flash sprites, the fusion sound, the plasma damage type
os.makedirs(os.path.join(RES, "textures/entity"), exist_ok=True)
for fn in ("plasma7.png", "fusion2.png"):
    shutil.copyfile(os.path.join(SRC, "Textures", fn), os.path.join(RES, "textures/entity", fn))
shutil.copyfile(os.path.join(SRC, "Sounds", "fusion.ogg"), os.path.join(RES, "sounds", "fusion.ogg"))
with open(os.path.join(RES, "sounds.json")) as f:
    snd = json.load(f)
snd["fusion"] = {"sounds": [{"name": "reactorcraft:fusion", "attenuation_distance": 32}]}
write(os.path.join(RES, "sounds.json"), snd)
write(os.path.join(DATA, "reactorcraft/damage_type/fusion.json"), {"message_id": "reactorcraft.fusion", "exhaustion": 0.0, "scaling": "never"})
for c in LANGS:
    NEW[c]["death.attack.reactorcraft.fusion"] = "%1$s jumped in a Fusion Reactor"
    NEW[c]["entity.reactorcraft.plasma"] = "Plasma"
    NEW[c]["entity.reactorcraft.fusion"] = "Fusion"
tag("minecraft", "block", "mineable/pickaxe", ["reactorcraft:" + m for m in ["toroid_magnet", "solenoid", "gas_collector", "fusion_marker", "fusion_injector",
    "fusion_heater", "neutron_absorber", "magnetic_pipe"] + SOL + INJ + HEA])

for c in LANGS:
    NEW[c]["itemGroup.reactorcraft_items"] = "ReactorCraft Items"
    write(os.path.join(RES, "lang", LANGS[c] + ".json"), dict(sorted(NEW[c].items())))
print("reactorcraft assets written:", len(NEW["en_US"]), "lang keys,", len(os.listdir(recipes)), "recipes")

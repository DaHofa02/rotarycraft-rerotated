#!/usr/bin/env python3
"""Converts original RotaryCraft (1.7.10) assets into the 1.21.1 resource layout.

Inputs (read-only): the original source tree and the original JAR (extracted).
Outputs:
  rotarycraft/src/main/resources/assets/rotarycraft/textures/item/*.png  - 16x16 tiles cut pixel-exactly from the sprite sheets
  rotarycraft/src/main/resources/assets/rotarycraft/textures/machine/*.png - model textures, copied unchanged
  rotarycraft/src/main/resources/assets/rotarycraft/sounds/*.ogg - copied unchanged
  rotarycraft/src/main/resources/assets/rotarycraft/models/item/*.json
  rotarycraft/src/main/resources/assets/rotarycraft/lang/*.json - original texts under the new keys
  rotarycraft/src/main/java/reika/rotarycraft/content/item/CraftingPart.java - the crafting item table

Usage: python3 -I tools/gen_rotarycraft_assets.py <original RotaryCraft dir> <extracted RotaryCraft jar dir>
"""
import json
import os
import shutil
import sys

from PIL import Image

ORIG, JAR = sys.argv[1], sys.argv[2]
PORT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(PORT, "rotarycraft/src/main/resources/assets/rotarycraft")
JAVA = os.path.join(PORT, "rotarycraft/src/main/java/reika/rotarycraft")

LANGS = {"en_US": "en_us", "de_DE": "de_de", "pl_PL": "pl_pl", "pt_BR": "pt_br", "ru_RU": "ru_ru", "zh_CN": "zh_cn"}


def read_lang(code):
    d = {}
    p = os.path.join(JAR, "assets/rotarycraft/lang", code + ".lang")
    with open(p, encoding="utf-8-sig") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                d[k.strip()] = v.strip()
    return d


ORIG_LANG = {code: read_lang(code) for code in LANGS}
NEW_LANG = {code: {} for code in LANGS}


def put_lang(new_key, parts):
    """parts: list of original lang keys or literal strings (prefixed '=') joined with spaces. Missing translations fall back to English."""
    for code in LANGS:
        out = []
        for p in parts:
            if p.startswith("="):
                out.append(p[1:])
            else:
                v = ORIG_LANG[code].get(p) or ORIG_LANG["en_US"].get(p)
                if v is None:
                    raise KeyError(p)
                out.append(v)
        NEW_LANG[code][new_key] = " ".join(out)


SHEETS = {}


def sheet(name):
    if name not in SHEETS:
        SHEETS[name] = Image.open(os.path.join(ORIG, "Textures/Items", name)).convert("RGBA")
    return SHEETS[name]


def cut(sheet_name, index, out_id):
    img = sheet(sheet_name)
    x, y = (index % 16) * 16, (index // 16) * 16
    tile = img.crop((x, y, x + 16, y + 16))
    os.makedirs(os.path.join(RES, "textures/item"), exist_ok=True)
    tile.save(os.path.join(RES, "textures/item", out_id + ".png"))


def item_model(out_id, texture=None):
    os.makedirs(os.path.join(RES, "models/item"), exist_ok=True)
    with open(os.path.join(RES, "models/item", out_id + ".json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "rotarycraft:item/" + (texture or out_id)}}, f, indent=2)


# --- Crafting parts (original ItemMulti families). sprite = 16*(type%16 + dmg/16) + dmg%16, see ItemMulti.getItemSpriteIndex.
# (family, type index, sheet, names per metadata from RotaryNames, ItemStacks field names per metadata)
FAMILIES = [
    ("SHAFTCRAFT", 0, "items.png",
     {0: ("crafting.panel", "basepanel"), 1: ("crafting.ingot", "steelingot"), 3: ("crafting.mount", "mount"),
      9: ("crafting.scrap", "scrap"), 10: ("crafting.ironscrap", "ironscrap"), 15: ("crafting.worm", "wormgear")}),
    ("ENGINECRAFT", 1, "items.png",
     {0: ("crafting.impeller", "impeller"), 1: ("crafting.compressor", "compressor"), 2: ("crafting.turbine", "turbine"),
      3: ("crafting.diffuser", "diffuser"), 4: ("crafting.combustor", "combustor"), 5: ("crafting.cylinder", "cylinder"),
      6: ("crafting.radiator", "radiator"), 7: ("crafting.condenser", "condenser"), 8: ("crafting.goldcoil", "goldcoil"),
      13: ("crafting.paddle", "waterplate"), 15: ("crafting.ignition", "igniter"), 17: ("crafting.compoundcompress", "compoundcompress"),
      18: ("crafting.alucylinder", "aluminumcylinder"), 19: ("crafting.highcombustor", "highcombustor")}),
    ("MISCCRAFT", 2, "items.png",
     {0: ("crafting.barrel", "barrel"), 1: ("crafting.lens", "lens"), 2: ("crafting.power", "power"), 3: ("crafting.heatcore", "bulb"),
      4: ("crafting.lim", "lim"), 5: ("crafting.prop", "prop"), 6: ("crafting.hub", "hub"), 7: ("crafting.mirror", "mirror"),
      8: ("crafting.generator", "generator"), 9: ("crafting.accel", "railhead"), 10: ("crafting.turretbase", "railbase"),
      11: ("crafting.aiming", "railaim"), 12: ("crafting.compound", "compoundturb"), 13: ("crafting.bedcoil", "bedrockcoil"),
      14: ("crafting.chain", "chain"), 15: ("crafting.beddrill", "bedrockdrill")}),
    ("BORECRAFT", 3, "items.png",
     {0: ("crafting.drill", "drill"), 1: ("crafting.pressure", "presshead"), 2: ("crafting.radar", "radar"), 3: ("crafting.sonar", "sonar"),
      4: ("crafting.pcb", "pcb"), 5: ("crafting.screen", "screen"), 6: ("crafting.mixer", "mixer"), 7: ("crafting.saw", "saw"),
      9: ("crafting.belt", "belt"), 10: ("crafting.ballbearing", "ballbearing"), 11: ("crafting.brake", "brake"), 12: ("crafting.coil", "tenscoil")}),
    ("EXTRACTS", 4, "items.png",
     {0: ("extract.coaldust", "coaloredust"),
      1: ("extract.irondust", "ironoredust"),
      2: ("extract.golddust", "goldoredust"),
      3: ("extract.reddust", "redoredust"),
      4: ("extract.bluedust", "lapisoredust"),
      5: ("extract.diadust", "diamondoredust"),
      6: ("extract.greendust", "emeraldoredust"),
      7: ("extract.quartzdust", "netherquartzdust"),
      8: ("extract.coalslurry", "coaloreslurry"),
      9: ("extract.ironslurry", "ironoreslurry"),
      10: ("extract.goldslurry", "goldoreslurry"),
      11: ("extract.redslurry", "redoreslurry"),
      12: ("extract.blueslurry", "lapisoreslurry"),
      13: ("extract.diaslurry", "diamondoreslurry"),
      14: ("extract.greenslurry", "emeraldoreslurry"),
      15: ("extract.quartzslurry", "quartzslurry"),
      16: ("extract.coalsolu", "coalsolution"),
      17: ("extract.ironsolu", "ironsolution"),
      18: ("extract.goldsolu", "goldsolution"),
      19: ("extract.redsolu", "redsolution"),
      20: ("extract.bluesolu", "lapissolution"),
      21: ("extract.diasolu", "diamondsolution"),
      22: ("extract.greensolu", "emeraldsolution"),
      23: ("extract.quartzsolu", "quartzsolution"),
      24: ("extract.coalflakes", "coaloreflakes"),
      25: ("extract.ironflakes", "ironoreflakes"),
      26: ("extract.goldflakes", "goldoreflakes"),
      27: ("extract.redflakes", "redoreflakes"),
      28: ("extract.blueflakes", "lapisoreflakes"),
      29: ("extract.diaflakes", "diamondoreflakes"),
      30: ("extract.greenflakes", "emeraldoreflakes"),
      31: ("extract.quartzflakes", "quartzflakes"),
      32: ("extract.silverflakes", "silverflakes"),
      33: ("extract.tungstenflakes", "tungstenflakes")}),
    ("COMPACTS", 6, "items.png",
     {0: ("misc.anthra", "anthracite"), 1: ("misc.prisma", "prismane"), 2: ("misc.lonsda", "lonsda"), 3: ("misc.bedingot", "bedingot"),
      4: ("misc.alingot", "aluminumingot"), 5: ("misc.wingot", "tungsteningot"), 6: ("misc.redgoldingot", "redgoldingot"),
      7: ("misc.agingot", "silveringot"), 8: ("misc.coke", "coke"), 9: ("misc.springingot", "springingot"), 10: ("misc.silicon", "silicon"),
      11: ("misc.alualloy", "silumin"), 12: ("misc.tungalloy", "springtungsten")}),
    ("POWDERS", 8, "items.png",
     {0: ("misc.netherdust", "netherrackdust"), 1: ("misc.tar", "tar"), 2: ("misc.sludge", "sludge"), 3: ("misc.sawdust", "sawdust"),
      4: ("misc.beddust", "bedrockdust"), 5: ("misc.salt", "salt"), 6: ("misc.nh4", "nitrate"), 7: ("misc.agi", "silveriodide"),
      8: ("misc.alum", "aluminumpowder"), 9: ("misc.flour", "flour"), 10: ("misc.coaldust", "coaldust"), 11: ("misc.dryice", "dryice"),
      12: ("misc.redgolddust", "redgolddust"), 13: ("misc.compost", "compost"), 14: ("misc.silicondust", "silicondust"),
      15: ("misc.mulch", "mulch"), 16: ("misc.cleansludge", "cleansludge")}),
]


def sprite_index(family, type_idx, dmg):
    if family == "EXTRACTS" and dmg > 31:
        return 16 * 9 + dmg - 32
    row = type_idx % 16 + dmg // 16
    if family == "ENGINECRAFT" and dmg >= 16:
        row += 9
    if family == "POWDERS" and dmg >= 16:
        row += 3
    return 16 * row + dmg % 16


parts = []  # (java constant, id, original family, metadata)
for family, type_idx, sheet_name, entries in FAMILIES:
    for dmg, (lang_key, field) in sorted(entries.items()):
        cut(sheet_name, sprite_index(family, type_idx, dmg), field)
        item_model(field)
        put_lang("item.rotarycraft." + field, [lang_key])
        parts.append((field.upper(), field, family, dmg))

# Gear parts: GEARCRAFT on items4.png, sprite = dmg = metaOffset*16 + part. Name = material + " " + part (RotaryNames.getGearPartName).
GEAR_MATERIALS = [("WOOD", 1), ("STONE", 2), ("STEEL", 0), ("TUNGSTEN", 7), ("DIAMOND", 3), ("BEDROCK", 4)]
GEAR_PARTS = [("SHAFT", "crafting.shaft"), ("GEAR", "crafting.gear"), ("UNIT2", "crafting.gear2x"), ("UNIT4", "crafting.gear4x"),
              ("UNIT8", "crafting.gear8x"), ("UNIT16", "crafting.gear16x"), ("BEARING", "crafting.bearing"), ("SHAFTCORE", "crafting.shaftcore")]
gear_parts = []
for mat, row in GEAR_MATERIALS:
    for pi, (part, lang_key) in enumerate(GEAR_PARTS):
        gid = mat.lower() + "_" + part.lower()
        cut("items4.png", row * 16 + pi, gid)
        item_model(gid)
        put_lang("item.rotarycraft." + gid, ["material." + mat.lower(), lang_key])
        gear_parts.append((mat, part, gid))

# Flywheel cores: FLYWHEELCRAFT(208), sprite 208+type on items.png. Name = flywheel name + " " + "Flywheel Core".
FLYWHEELS = ["WOOD", "STONE", "IRON", "GOLD", "TUNGSTEN", "DEPLETEDU", "BEDROCK"]
for i, fw in enumerate(FLYWHEELS):
    fid = fw.lower() + "_flywheel_core"
    cut("items.png", 208 + i, fid)
    item_model(fid)
    put_lang("item.rotarycraft." + fid, ["flywheel." + fw.lower(), "crafting.flycore"])

# --- Java table of crafting parts
java = ['/*******************************************************************************',
        ' * GENERATED by tools/gen_rotarycraft_assets.py from the original RotaryCraft item tables. Do not edit by hand.',
        ' * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.',
        ' ******************************************************************************/',
        'package reika.rotarycraft.content.item;',
        '',
        '/** Plain crafting items, one per former ItemMulti metadata (original family and metadata noted per entry). */',
        'public enum CraftingPart {',
        '']
for const, pid, family, dmg in parts:
    java.append('\t%s("%s"), // %s:%d' % (const, pid, family, dmg))
java[-1] = java[-1].replace('), //', '); //', 1)
java += ['',
         '\tpublic final String id;',
         '',
         '\tCraftingPart(String id) {',
         '\t\tthis.id = id;',
         '\t}',
         '}',
         '']
with open(os.path.join(JAVA, "content/item/CraftingPart.java"), "w") as f:
    f.write("\n".join(java))

# --- Machine model textures and sounds: copied unchanged (lower-cased names; see docs/04 §3).
tex_root = os.path.join(ORIG, "Textures/TileEntityTex")
for dirpath, _, fns in os.walk(tex_root):
    rel = os.path.relpath(dirpath, tex_root).lower().replace(" ", "_")
    dst = os.path.join(RES, "textures/machine", "" if rel == "." else rel)
    os.makedirs(dst, exist_ok=True)
    for fn in fns:
        if fn.lower().endswith(".png"):
            shutil.copyfile(os.path.join(dirpath, fn), os.path.join(dst, fn.lower().replace(" ", "_")))

# GUI textures, unchanged (Textures/GUI -> textures/gui).
gui_root = os.path.join(ORIG, "Textures/GUI")
for dirpath, _, fns in os.walk(gui_root):
    rel = os.path.relpath(dirpath, gui_root).lower()
    dst = os.path.join(RES, "textures/gui", "" if rel == "." else rel)
    os.makedirs(dst, exist_ok=True)
    for fn in fns:
        if fn.lower().endswith(".png"):
            shutil.copyfile(os.path.join(dirpath, fn), os.path.join(dst, fn.lower().replace(" ", "_")))

# Plain items that only had a sprite (ItemBasic): (id, sheet, index, original lang key)
for iid, sheet_name, idx, key in [("yeast", "items2.png", 32, "item.yeast"), ("ethanol", "items2.png", 64, "item.ethanol"),
                                  ("screwdriver", "items2.png", 0, "item.screwdriver"), ("spring", "items2.png", 96, "item.spring"), ("strongcoil", "items2.png", 99, "item.strongcoil"), ("meter", "items2.png", 16, "item.meter")]:
    cut(sheet_name, idx, iid)
    item_model(iid)
    put_lang("item.rotarycraft." + iid, [key])
# Canola (ItemRegistry.CANOLA, items2.png 80 + metadata) and the fluid buckets (ItemRegistry.BUCKET, items2.png 104 + metadata;
# name = fluid name + "Bucket", RotaryNames.getBucketName).
for iid, idx, key in [("canola_seeds", 80, "canola.seed"), ("dense_canola_seeds", 81, "canola.dense"), ("canola_husks", 82, "canola.husk")]:
    cut("items2.png", idx, iid)
    item_model(iid)
    put_lang("item.rotarycraft." + iid, [key])
for iid, idx, fluid in [("lubricant_bucket", 104, "fluid.rc lubricant"), ("jet_fuel_bucket", 105, "fluid.rc jet fuel"), ("ethanol_bucket", 106, "fluid.rc ethanol"),
                        ("liquid_nitrogen_bucket", 107, "fluid.rc liquid nitrogen")]:
    cut("items2.png", idx, iid)
    item_model(iid)
    put_lang("item.rotarycraft." + iid, [fluid, "item.rcbucket"])
# Canola crop (BlockCanola: render type 6 = crop, texture canola/<stage>)
canola_variants = {}
os.makedirs(os.path.join(RES, "models/block/canola"), exist_ok=True)
for age in range(10):
    with open(os.path.join(RES, "models/block/canola", "stage%d.json" % age), "w") as f:
        json.dump({"parent": "minecraft:block/crop", "render_type": "minecraft:cutout", "textures": {"crop": "rotarycraft:block/canola/%d" % age}}, f)
    canola_variants["age=%d" % age] = {"model": "rotarycraft:block/canola/stage%d" % age}
with open(os.path.join(RES, "blockstates", "canola.json"), "w") as f:
    json.dump({"variants": canola_variants}, f, indent=1)
put_lang("block.rotarycraft.canola", ["block.canola"])
put_lang("block.rotarycraft.grinder", ["machine.grinder"])
# Engine upgrades (ItemEngineUpgrade, ItemRegistry.UPGRADE: items2.png, sprite 240 + Upgrades ordinal). Only the upgrades whose
# target machines are ported are registered: PERFORMANCE (0), AFTERBURNER (6), REDSTONE (9).
for iid, ordinal, key in [("upgrade_performance", 0, "upgrade.gasperf"), ("upgrade_afterburner", 6, "upgrade.afterburn"),
                          ("upgrade_redstone", 9, "upgrade.redstone")]:
    cut("items2.png", 240 + ordinal, iid)
    item_model(iid)
    put_lang("item.rotarycraft." + iid, [key])
# Integrated gearbox upgrade (ItemIntegratedGearbox, ItemRegistry.GEARUPGRADE: items2.png 65 = frame, 66 = with gears, 67 = full of
# lubricant, 68 = full of liquid nitrogen; one name for all). The sprite follows the "rotarycraft:fill" item property.
cut("items2.png", 65, "gear_upgrade")
item_model("gear_upgrade")
put_lang("item.rotarycraft.gear_upgrade", ["item.gearupgrade"])
for idx, tex in ((66, "gear_upgrade_gears"), (67, "gear_upgrade_lubricant"), (68, "gear_upgrade_nitrogen")):
    cut("items2.png", idx, tex)
    item_model(tex)
for lvl in range(1, 5):
    iid = "gear_upgrade_%d" % (1 << lvl)
    with open(os.path.join(RES, "models/item", iid + ".json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "rotarycraft:item/gear_upgrade_gears"},
                   "overrides": [{"predicate": {"rotarycraft:fill": 0.5}, "model": "rotarycraft:item/gear_upgrade_lubricant"},
                                 {"predicate": {"rotarycraft:fill": 1.0}, "model": "rotarycraft:item/gear_upgrade_nitrogen"}]}, f, indent=2)
    put_lang("item.rotarycraft." + iid, ["item.gearupgrade"])
os.makedirs(os.path.join(RES, "sounds/music"), exist_ok=True)
for sub in ("", "music"):
    for fn in os.listdir(os.path.join(ORIG, "Sounds", sub)):
        if fn.endswith(".ogg"):
            shutil.copyfile(os.path.join(ORIG, "Sounds", sub, fn), os.path.join(RES, "sounds", sub, fn.lower()))

# sounds.json from the original SoundRegistry entries ("#" = volume follows the machine/engine volume config; notes live in music/).
SOUNDS = [("ELECTRIC", "#elecengine"), ("WIND", "#windengine"), ("STEAM", "#steamengine"), ("CAR", "#gasengine"), ("HYDRO", "#hydroengine"),
          ("MICRO", "#microengine"), ("JET", "#jetengine"), ("KNOCKBACK", "knockback"), ("PULSEJET", "#pulsejet"), ("PUMP", "#pump"),
          ("PILEDRIVER", "piledriver"), ("SMOKE", "smokealarm"), ("SPRINKLER", "#sprinkler"), ("FLYWHEEL", "#flywheel"), ("PROJECTOR", "projector"),
          ("LOWBASS", "basslo"), ("BASS", "bass"), ("HIBASS", "basshi"), ("LOWHARP", "harplo"), ("HARP", "harp"), ("HIHARP", "harphi"),
          ("LOWPLING", "plinglo"), ("PLING", "pling"), ("HIPLING", "plinghi"), ("FRICTION", "#friction"), ("CRAFT", "#craft"),
          ("AIRCOMP", "#compress"), ("PNEUMATIC", "#pneu"), ("LINEBUILDER", "linebuild"), ("JETPACK", "pack"), ("DIESEL", "#diesel"),
          ("BELT", "#belt"), ("FAN", "#fan"), ("SPARK", "spark"), ("DYNAMO", "#dynamo"), ("INGESTION", "ingest_short"), ("FRIDGE", "#fridge"),
          ("JETSTART", "#jetstart"), ("SONIC", "#sonic"), ("SHORTJET", "shortjet"), ("AFTERBURN", "afterburner"), ("RUMBLE", "rumble2"),
          ("COIL", "#coil"), ("GATLINGRELOAD", "gatlingreload"), ("GATLING", "gatling"), ("FLAMETURRET", "flameturret")]
sounds_json = {}
for const, f in SOUNDS:
    name = f.lstrip("#")
    note = any(n in const for n in ("HARP", "BASS", "PLING"))
    path = ("music/" if note else "") + name
    if not os.path.exists(os.path.join(RES, "sounds", path + ".ogg")):
        print("MISSING SOUND FILE", path)
    sounds_json[const.lower()] = {"sounds": [{"name": "rotarycraft:" + path, "attenuation_distance": 40 if const in ("JET", "JETSTART") else 16, "preload": const == "JETSTART"}]}
with open(os.path.join(RES, "sounds.json"), "w") as f:
    json.dump(sounds_json, f, indent=2)

# --- Machine names (MachineRegistry / ItemRegistry names) for the ported machines.
MACHINES = {
    "shaft": ["machine.shaft"], "gearbox": ["machine.gearbox"], "flywheel": ["machine.flywheel"], "bevel_gears": ["machine.bevel"],
    "clutch": ["machine.clutch"], "dynamometer": ["machine.dyna"], "cooling_fin": ["machine.coolingfin"], "fuel_enhancer": ["machine.fuelenhancer"], "solar_mirror": ["machine.mirror"], "solar_tower": ["machine.solartower"], "pipe_pump": ["machine.pipepump"], "van_de_graff": ["machine.vandegraff"], "drying_bed": ["machine.drying"], "winder": ["machine.winder"], "refrigerator": ["machine.refrigerator"], "heat_ray": ["machine.heatray"], "compactor": ["machine.compactor"], "filling_station": ["machine.fillingstation"], "shaft_junction": ["machine.splitter"],
}
for mid, keys in MACHINES.items():
    put_lang("block.rotarycraft." + mid, keys)
put_lang("item.rotarycraft.shaft_cross", ["shaft.cross"])
for lbl in ["temperature", "pressure", "speed", "torque", "power", "range", "damaged", "fuel", "operationtime"]:
    put_lang("label.rotarycraft." + lbl, ["label." + lbl])
for msg in ["drowning", "noair", "minpower", "mintorque", "minspeed"]:
    put_lang("message.rotarycraft." + msg, ["message." + msg])
for mat in ["wood", "stone", "steel", "tungsten", "diamond", "bedrock"]:
    put_lang("material.rotarycraft." + mat, ["material." + mat])
for fw in FLYWHEELS:
    put_lang("flywheel.rotarycraft." + fw.lower(), ["flywheel." + fw.lower()])
for eng in ["dc", "wind", "steam", "gas", "ac", "sport", "hydro", "micro", "jet"]:
    put_lang("block.rotarycraft." + eng + "_engine", ["engine." + eng])

# Block textures from the original JAR (assets/rotarycraft/textures/blocks -> textures/block), names made resource-location safe.
jar_blocks = os.path.join(JAR, "assets/rotarycraft/textures/blocks")
for dirpath, _, fns in os.walk(jar_blocks):
    rel = os.path.relpath(dirpath, jar_blocks).lower()
    dst = os.path.join(RES, "textures/block", "" if rel == "." else rel)
    os.makedirs(dst, exist_ok=True)
    for fn in fns:
        out = fn.lower().replace("#", "").replace(" ", "_")
        shutil.copyfile(os.path.join(dirpath, fn), os.path.join(dst, out))

# Engine Control Unit (BlockMachine icons: bottom steel, top ecu_top, sides ecu_side, or ecu_color/<dye> once dyed).
# blockstate "color": 0 = none, 1-16 = original ReikaDyeHelper ordinal + 1.
os.makedirs(os.path.join(RES, "models/block"), exist_ok=True)
os.makedirs(os.path.join(RES, "blockstates"), exist_ok=True)
ecu_variants = {}
for c in range(17):
    side = "rotarycraft:block/ecu_side" if c == 0 else "rotarycraft:block/ecu_color/%d" % (c - 1)
    name = "ecu" if c == 0 else "ecu_color_%d" % (c - 1)
    with open(os.path.join(RES, "models/block", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/cube", "textures": {"particle": side, "down": "rotarycraft:block/steel", "up": "rotarycraft:block/ecu_top",
                                                                   "north": side, "south": side, "east": side, "west": side}}, f, indent=2)
    ecu_variants["color=%d" % c] = {"model": "rotarycraft:block/" + name}
with open(os.path.join(RES, "blockstates", "ecu.json"), "w") as f:
    json.dump({"variants": ecu_variants}, f, indent=2)
with open(os.path.join(RES, "models/item", "ecu.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/ecu"}, f, indent=2)
put_lang("block.rotarycraft.ecu", ["machine.ecu"])

# Liquid Spiller (BlockMachine icons: steel on every side, the spiller grate below).
with open(os.path.join(RES, "blockstates", "spiller.json"), "w") as f:
    json.dump({"variants": {"": {"model": "rotarycraft:block/spiller"}}}, f, indent=2)
with open(os.path.join(RES, "models/block", "spiller.json"), "w") as f:
    json.dump({"parent": "minecraft:block/cube_bottom_top", "textures": {"side": "rotarycraft:block/steel", "top": "rotarycraft:block/steel",
                                                                        "bottom": "rotarycraft:block/spiller"}}, f, indent=2)
with open(os.path.join(RES, "models/item", "spiller.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/spiller"}, f, indent=2)
put_lang("block.rotarycraft.spiller", ["machine.spiller"])

# Blast Furnace (BlockIMachine: blastfurn_front on the facing side, _active while working, blastfurn_side elsewhere) and Worktable.
for active in (False, True):
    name = "blast_furnace" + ("_active" if active else "")
    front = "rotarycraft:block/blastfurn_front" + ("_active" if active else "")
    with open(os.path.join(RES, "models/block", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/orientable_with_bottom", "textures": {"particle": "rotarycraft:block/blastfurn_side", "front": front,
                   "side": "rotarycraft:block/blastfurn_side", "top": "rotarycraft:block/blastfurn_side", "bottom": "rotarycraft:block/blastfurn_side"}}, f, indent=2)
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
with open(os.path.join(RES, "blockstates", "blast_furnace.json"), "w") as f:
    json.dump({"variants": {"active=%s,facing=%s" % (str(a).lower(), d): dict({"model": "rotarycraft:block/blast_furnace" + ("_active" if a else "")}, **({"y": r} if r else {}))
                            for a in (False, True) for d, r in ROT.items()}}, f, indent=2)
with open(os.path.join(RES, "models/item", "blast_furnace.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/blast_furnace"}, f, indent=2)
# Fermenter (BlockIMachine: steel, ferm_front on the facing side, _active while working, ferm_back opposite)
for active in (False, True):
    name = "fermenter" + ("_active" if active else "")
    front = "rotarycraft:block/ferm_front" + ("_active" if active else "")
    with open(os.path.join(RES, "models/block", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/cube", "textures": {"particle": "rotarycraft:block/steel", "north": front, "south": "rotarycraft:block/ferm_back",
                   "east": "rotarycraft:block/steel", "west": "rotarycraft:block/steel", "up": "rotarycraft:block/steel", "down": "rotarycraft:block/steel"}}, f, indent=2)
with open(os.path.join(RES, "blockstates", "fermenter.json"), "w") as f:
    json.dump({"variants": {"active=%s,facing=%s" % (str(a).lower(), d): dict({"model": "rotarycraft:block/fermenter" + ("_active" if a else "")}, **({"y": r} if r else {}))
                            for a in (False, True) for d, r in ROT.items()}}, f, indent=2)
with open(os.path.join(RES, "models/item", "fermenter.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/fermenter"}, f, indent=2)
put_lang("block.rotarycraft.fermenter", ["machine.fermenter"])
put_lang("block.rotarycraft.fan", ["machine.fan"])
put_lang("block.rotarycraft.mob_harvester", ["machine.mobharvester"])
put_lang("block.rotarycraft.aggregator", ["machine.aggregator"])
put_lang("block.rotarycraft.centrifuge", ["machine.centrifuge"])
put_lang("block.rotarycraft.fractionator", ["machine.fractionator"])
put_lang("block.rotarycraft.pump", ["machine.pump"])
put_lang("block.rotarycraft.reservoir", ["machine.reservoir"])
put_lang("block.rotarycraft.pulse_jet_furnace", ["machine.pulsejet"])
# Blast glass (BlockBlastGlass: obsidiglass icon; connected edge frames not ported)
with open(os.path.join(RES, "blockstates", "blast_glass.json"), "w") as f:
    json.dump({"variants": {"": {"model": "rotarycraft:block/blast_glass"}}}, f)
with open(os.path.join(RES, "models/block", "blast_glass.json"), "w") as f:
    json.dump({"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": "rotarycraft:block/obsidiglass"}}, f)
with open(os.path.join(RES, "models/item", "blast_glass.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/blast_glass"}, f)
put_lang("block.rotarycraft.blast_glass", ["block.blastglass"])
# Decorative storage blocks (BlockDeco, metadata 0-5; icons RotaryCraft:steel, anthra, lons, shield, bedrock, coke). Added 2026-11-03,
# see PORTING_NOTES.md; loot tables, tags and recipes are written by hand / gen_rotarycraft_recipes.py.
for bid, tex, key in (("steel_block", "steel", "block.steel"), ("anthracite_block", "anthra", "block.anthra"), ("lonsdaleite_block", "lons", "block.lonsda"),
                      ("shield_block", "shield", "block.shield"), ("bedrock_ingot_block", "bedrock", "block.bedrockingot"), ("coke_block", "coke", "block.coke")):
    with open(os.path.join(RES, "blockstates", bid + ".json"), "w") as f:
        json.dump({"variants": {"": {"model": "rotarycraft:block/" + bid}}}, f)
    with open(os.path.join(RES, "models/block", bid + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/cube_all", "textures": {"all": "rotarycraft:block/" + tex}}, f)
    with open(os.path.join(RES, "models/item", bid + ".json"), "w") as f:
        json.dump({"parent": "rotarycraft:block/" + bid}, f)
    put_lang("block.rotarycraft." + bid, [key])
with open(os.path.join(RES, "models/block", "worktable.json"), "w") as f:
    json.dump({"parent": "minecraft:block/cube_bottom_top", "textures": {"particle": "rotarycraft:block/worktable", "side": "rotarycraft:block/worktable",
               "top": "rotarycraft:block/worktable_top", "bottom": "rotarycraft:block/worktable_bottom"}}, f, indent=2)
with open(os.path.join(RES, "blockstates", "worktable.json"), "w") as f:
    json.dump({"variants": {"": {"model": "rotarycraft:block/worktable"}}}, f, indent=2)
with open(os.path.join(RES, "models/item", "worktable.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/worktable"}, f, indent=2)
put_lang("block.rotarycraft.blast_furnace", ["machine.blastfurnace"])
put_lang("block.rotarycraft.worktable", ["machine.worktable"])
put_lang("block.rotarycraft.friction_heater", ["machine.friction"])
put_lang("block.rotarycraft.bedrock_breaker", ["machine.bedrock"])
put_lang("block.rotarycraft.extractor", ["machine.extractor"])

# Bedrock slice (BlockBedrockSlice): vanilla bedrock texture, cut by level/16 from the side facing the machine (BlockBounds.cut).
CUT = {"down": lambda a: ([0, a, 0], [16, 16, 16]), "up": lambda a: ([0, 0, 0], [16, 16 - a, 16]), "east": lambda a: ([0, 0, 0], [16 - a, 16, 16]),
       "west": lambda a: ([a, 0, 0], [16, 16, 16]), "north": lambda a: ([0, 0, a], [16, 16, 16]), "south": lambda a: ([0, 0, 0], [16, 16, 16 - a])}
slice_variants = {}
os.makedirs(os.path.join(RES, "models/block/bedrock_slice"), exist_ok=True)
for d, cut in CUT.items():
    for lvl in range(16):
        frm, to = cut(lvl)
        faces = {f: {"texture": "#all", "cullface": f} if lvl == 0 or f != d else {"texture": "#all"} for f in ("down", "up", "north", "south", "east", "west")}
        name = "bedrock_slice/%s_%d" % (d, lvl)
        with open(os.path.join(RES, "models/block", name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:block/block", "textures": {"all": "minecraft:block/bedrock", "particle": "minecraft:block/bedrock"},
                       "elements": [{"from": frm, "to": to, "faces": faces}]}, f)
        slice_variants["facing=%s,level=%d" % (d, lvl)] = {"model": "rotarycraft:block/" + name}
with open(os.path.join(RES, "blockstates", "bedrock_slice.json"), "w") as f:
    json.dump({"variants": slice_variants}, f, indent=1)
put_lang("block.rotarycraft.bedrock_slice", ["block.bedrockslice"])
# Borer (BlockDMachine: borer_front on the side it digs towards, _active while powered enough, borer_back opposite, steel elsewhere) and
# the Mining Pipe it leaves behind (BlockMiningPipe: bars 0.33-0.67 along x/y/z, full blocks for metadata 3 and 4; minepipe/minepipe2).
for active in (False, True):
    name = "borer" + ("_active" if active else "")
    front = "rotarycraft:block/borer_front" + ("_active" if active else "")
    with open(os.path.join(RES, "models/block", name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/cube", "textures": {"particle": "rotarycraft:block/steel", "north": front, "south": "rotarycraft:block/borer_back",
                   "east": "rotarycraft:block/steel", "west": "rotarycraft:block/steel", "up": "rotarycraft:block/steel", "down": "rotarycraft:block/steel"}}, f, indent=2)
with open(os.path.join(RES, "blockstates", "borer.json"), "w") as f:
    json.dump({"variants": {"active=%s,facing=%s" % (str(a).lower(), d): dict({"model": "rotarycraft:block/borer" + ("_active" if a else "")}, **({"y": r} if r else {}))
                            for a in (False, True) for d, r in ROT.items()}}, f, indent=2)
with open(os.path.join(RES, "models/item", "borer.json"), "w") as f:
    json.dump({"parent": "rotarycraft:block/borer"}, f, indent=2)
PIPE_BOX = {"x": ([0, 5.28, 5.28], [16, 10.72, 10.72]), "y": ([5.28, 0, 5.28], [10.72, 16, 10.72]), "z": ([5.28, 5.28, 0], [10.72, 10.72, 16]),
            "full": ([0, 0, 0], [16, 16, 16]), "pile": ([0, 0, 0], [16, 16, 16])}
for shape, (frm, to) in PIPE_BOX.items():
    tex = "rotarycraft:block/minepipe2" if shape == "pile" else "rotarycraft:block/minepipe"
    with open(os.path.join(RES, "models/block", "mining_pipe_" + shape + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {"all": tex, "particle": tex},
                   "elements": [{"from": frm, "to": to, "faces": {fc: {"texture": "#all"} for fc in ("down", "up", "north", "south", "east", "west")}}]}, f, indent=2)
with open(os.path.join(RES, "blockstates", "mining_pipe.json"), "w") as f:
    json.dump({"variants": {"shape=" + s: {"model": "rotarycraft:block/mining_pipe_" + s} for s in PIPE_BOX}}, f, indent=2)
put_lang("block.rotarycraft.borer", ["machine.borer"])
put_lang("block.rotarycraft.mining_pipe", ["block.miningpipe"])

# Misc textures used by renderers (Friction Heater: glowing furnace front)
os.makedirs(os.path.join(RES, "textures/misc"), exist_ok=True)
for fn in ("hotfurnace_0.png", "hotfurnace_1.png", "hotfurnace_2.png"):
    shutil.copyfile(os.path.join(ORIG, "Textures/Misc", fn), os.path.join(RES, "textures/misc", fn))

# Machines: drawn by their block entity renderer. The block model only supplies the break particle (original default icon:
# the HSLA steel block, RotaryCraftTileEntity.getIconForSide); the item uses the same renderer (builtin/entity, original ItemMachineRenderer).
MACHINE_IDS = ["shaft", "gearbox", "flywheel", "bevel_gears", "clutch", "dynamometer", "shaft_junction",
               "dc_engine", "wind_engine", "steam_engine", "gas_engine", "ac_engine", "sport_engine", "micro_engine", "hydro_engine", "jet_engine", "friction_heater", "bedrock_breaker", "extractor", "grinder", "centrifuge", "fan", "mob_harvester", "aggregator", "fractionator", "pump", "reservoir", "pulse_jet_furnace", "cooling_fin", "fuel_enhancer", "solar_mirror", "solar_tower", "pipe_pump", "van_de_graff", "drying_bed", "winder", "refrigerator", "heat_ray", "compactor", "filling_station"]
os.makedirs(os.path.join(RES, "blockstates"), exist_ok=True)
os.makedirs(os.path.join(RES, "models/block"), exist_ok=True)
for mid in MACHINE_IDS:
    with open(os.path.join(RES, "blockstates", mid + ".json"), "w") as f:
        json.dump({"variants": {"": {"model": "rotarycraft:block/" + mid}}}, f, indent=2)
    with open(os.path.join(RES, "models/block", mid + ".json"), "w") as f:
        json.dump({"textures": {"particle": "rotarycraft:block/steel"}}, f, indent=2)
    with open(os.path.join(RES, "models/item", mid + ".json"), "w") as f:
        json.dump({"parent": "minecraft:builtin/entity", "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.40, 0.40, 0.40]}}}, f, indent=2)

# Pipes (original PipeBodyRenderer, ISBRH): a 12-px core (2..14) framed by 2-px bars of the pipe material with glass windows
# (4..12); every connected side gets a 2-px arm out to the block edge, and the frame/glass of the faces next to it opens up.
# As a multipart block model on the six connection properties. Glass is the vanilla glass texture for every pipe (the
# bedrock pipe's blast glass icon is commented out in the original). The fluid inside is drawn by PipeFluidRenderer.
PIPES = {"hose": "minecraft:block/oak_planks", "pipe": "rotarycraft:block/steel", "fuel_line": "minecraft:block/obsidian",
         "bedrock_pipe": "rotarycraft:block/bedrock", "valve": "minecraft:block/redstone_block", "bypass": "minecraft:block/sandstone_top",
         "separation": "minecraft:block/lapis_block", "suction": "minecraft:block/nether_bricks"}
PDIRS = {"down": (1, -1), "up": (1, 1), "north": (2, -1), "south": (2, 1), "west": (0, -1), "east": (0, 1)}
def pdir(axis, sign):
    return [k for k, v in PDIRS.items() if v == (axis, sign)][0]
def corner(sign):
    return (12, 14) if sign > 0 else (2, 4)
def pelem(lo, hi, faces, tex, uv=None):
    fs = {}
    for fc in faces:
        fs[fc] = {"texture": tex}
        if uv:
            fs[fc]["uv"] = uv
    return {"from": lo, "to": hi, "faces": fs}
def pbar(axis, rng, sb, sc):
    b, c = [a for a in range(3) if a != axis]
    lo = [0, 0, 0]; hi = [0, 0, 0]
    lo[axis], hi[axis] = rng
    lo[b], hi[b] = corner(sb)
    lo[c], hi[c] = corner(sc)
    return pelem(lo, hi, [pdir(b, sb), pdir(c, sc)], "#pipe")
def pflat(axis, sign, rngs, tex, uv=None):
    lo = [0, 0, 0]; hi = [0, 0, 0]
    lo[axis] = hi[axis] = 14 if sign > 0 else 2
    for a, r in rngs.items():
        lo[a], hi[a] = r
    return pelem(lo, hi, [pdir(axis, sign)], tex, uv)
PIPE_PARTS = []  # (name, elements, when)
for axis in range(3):
    b, c = [a for a in range(3) if a != axis]
    for sb in (-1, 1):
        for sc in (-1, 1):
            PIPE_PARTS.append(("bar_%d_%s_%s" % (axis, pdir(b, sb), pdir(c, sc)), [pbar(axis, (2, 14), sb, sc)],
                               {pdir(b, sb): "false", pdir(c, sc): "false"}))
for dname, (axis, s_) in PDIRS.items():
    b, c = [a for a in range(3) if a != axis]
    PIPE_PARTS.append(("window_" + dname, [pflat(axis, s_, {b: (4, 12), c: (4, 12)}, "#glass", [2, 2, 14, 14])], {dname: "false"}))
    arm = (14, 16) if s_ > 0 else (0, 2)
    els = [pbar(axis, arm, sb, sc) for sb in (-1, 1) for sc in (-1, 1)]
    for wa in (b, c):
        other = c if wa == b else b
        for sw in (-1, 1):
            els.append(pflat(wa, sw, {axis: arm, other: (4, 12)}, "#glass"))
    PIPE_PARTS.append(("arm_" + dname, els, {dname: "true"}))
    for gname, (gaxis, t) in PDIRS.items():
        if gaxis == axis:
            continue
        other = [a for a in range(3) if a not in (axis, gaxis)][0]
        PIPE_PARTS.append(("open_%s_%s" % (dname, gname), [pflat(axis, s_, {gaxis: (12, 14) if t > 0 else (2, 4), other: (4, 12)}, "#glass")],
                           {dname: "false", gname: "true"}))
for pid, ptex in PIPES.items():
    textures = {"pipe": ptex, "glass": "minecraft:block/glass", "particle": ptex}
    os.makedirs(os.path.join(RES, "models/block", pid), exist_ok=True)
    multipart = []
    inventory = []
    for name, els, when in PIPE_PARTS:
        with open(os.path.join(RES, "models/block", pid, name + ".json"), "w") as f:
            json.dump({"render_type": "minecraft:cutout", "textures": textures, "elements": els}, f)
        multipart.append({"when": when, "apply": {"model": "rotarycraft:block/%s/%s" % (pid, name)}})
        if all(v == "false" for v in when.values()):
            inventory += els
    with open(os.path.join(RES, "blockstates", pid + ".json"), "w") as f:
        json.dump({"multipart": multipart}, f, indent=1)
    # Original renderInventoryBlock: all six faces closed.
    with open(os.path.join(RES, "models/block", pid + ".json"), "w") as f:
        json.dump({"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": textures, "elements": inventory}, f)
    with open(os.path.join(RES, "models/item", pid + ".json"), "w") as f:
        json.dump({"parent": "rotarycraft:block/" + pid}, f)
put_lang("block.rotarycraft.hose", ["machine.hose"])
put_lang("block.rotarycraft.pipe", ["machine.pipe"])
put_lang("block.rotarycraft.fuel_line", ["machine.fuelline"])
put_lang("block.rotarycraft.bedrock_pipe", ["machine.bedpipe"])
for pid, key in (("valve", "machine.valve"), ("bypass", "machine.bypass"), ("separation", "machine.separation"), ("suction", "machine.suction")):
    put_lang("block.rotarycraft." + pid, [key])

for fl, key in [("lubricant", "rc lubricant"), ("ethanol", "rc ethanol"), ("jet_fuel", "rc jet fuel"), ("liquid_nitrogen", "rc liquid nitrogen")]:
    for code in LANGS:
        NEW_LANG[code]["fluid_type.rotarycraft." + fl] = ORIG_LANG[code].get("fluid." + key) or ORIG_LANG["en_US"].get("fluid." + key) or {"lubricant": "Lubricant", "ethanol": "Ethanol", "jet_fuel": "Jet Fuel", "liquid_nitrogen": "Liquid Nitrogen"}[fl]

os.makedirs(os.path.join(RES, "lang"), exist_ok=True)
for code, out in LANGS.items():
    path = os.path.join(RES, "lang", out + ".json")
    # Hand-written keys (texts the original built in code, GUI labels added by the port) live in tools/lang_extra.
    existing = {}
    extra = os.path.join(PORT, "tools/lang_extra/rotarycraft", out + ".json")
    if os.path.exists(extra):
        with open(extra, encoding="utf-8") as f:
            existing = json.load(f)
    existing.update(NEW_LANG[code])
    with open(path, "w", encoding="utf-8") as f:
        json.dump(dict(sorted(existing.items())), f, indent=2, ensure_ascii=False)

print("parts:", len(parts), "gear parts:", len(gear_parts), "flywheel cores:", len(FLYWHEELS))
print("lang keys (en_us):", len(NEW_LANG["en_US"]))

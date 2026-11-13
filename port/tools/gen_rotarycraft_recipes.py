#!/usr/bin/env python3
"""Generates RotaryCraft crafting recipes (data/rotarycraft/recipe/*.json) from a transcription of the original
RotaryRecipes.addCraftItems / addMachines / addMultiTypes calls.

* Ore-dictionary inputs become tags (plankWood -> minecraft:planks, ingotTin -> c:ingots/tin, ...). Recipes the original only
  added when an ore-dictionary entry existed get a neoforge:not/tag_empty condition.
* Outputs whose count depends on DifficultyEffects get one recipe per difficulty with a rotarycraft:difficulty condition.
* Machine recipes went to the Worktable, and to the crafting table only with "Crafting Table can Make Machines"
  (TABLEMACHINES, default off); they get the rotarycraft:config condition table_machines. The Worktable itself is ported in step 4.
* Recipes that need items or machines not ported yet are listed in SKIPPED and reported.

Usage: python3 -I tools/gen_rotarycraft_recipes.py
"""
import json
import os

PORT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(PORT, "rotarycraft/src/main/resources/data/rotarycraft/recipe")

DIFF = {"PARTCRAFT": (6, 3, 2), "SMALLERCRAFT": (2, 2, 1), "BELTCRAFT": (16, 8, 2), "PIPECRAFT": (32, 16, 8)}

TAGS = {"plankWood": "minecraft:planks", "ingotTin": "c:ingots/tin", "ingotElectrum": "c:ingots/electrum",
        "ingotCopper": "c:ingots/copper", "ingotSilver": "c:ingots/silver", "dyeBlack": "c:dyes/black"}

# Gear parts: <material>_<part>
MATS = ["wood", "stone", "steel", "tungsten", "diamond", "bedrock"]


def rc(i):
    return "rotarycraft:" + i


def ing(x):
    if x in TAGS:
        return {"tag": TAGS[x]}
    if ":" in x:
        return {"item": x}
    return {"item": rc(x)}


recipes = {}
SKIPPED = []


def put(name, data, conditions=None):
    if conditions:
        data["neoforge:conditions"] = conditions
    if name in recipes:
        raise ValueError("duplicate recipe " + name)
    recipes[name] = data


def result(item, count=1, components=None):
    r = {"id": item if ":" in item else rc(item), "count": count}
    if components:
        r["components"] = components
    return r


def worktable_twin(name, data, conditions):
    """Machine recipes (TABLE condition) and ItemRegistry recipes also went to the Worktable (WorktableRecipes.addRecipe)."""
    wt = json.loads(json.dumps(data))
    wt["type"] = "rotarycraft:worktable_shaped" if data["type"] == "minecraft:crafting_shaped" else "rotarycraft:worktable_shapeless"
    rest = [c for c in (conditions or []) if c not in TABLE]
    put(name + "_worktable", wt, rest or None)


def shaped(name, out, pattern, key, count=1, components=None, conditions=None, category="misc", worktable=False):
    data = {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
            "key": {k: ing(v) for k, v in key.items()}, "result": result(out, count, components)}
    if worktable or (conditions and any(c in TABLE for c in conditions)):
        if isinstance(count, str):
            for d, n in zip((1, 2, 3), DIFF[count]):
                dd = json.loads(json.dumps(data))
                dd["result"]["count"] = n
                worktable_twin("%s_difficulty_%d" % (name, d), dd, (conditions or []) + [{"type": "rotarycraft:difficulty", "value": d}])
        else:
            worktable_twin(name, data, conditions)
    if isinstance(count, str):
        for d, n in zip((1, 2, 3), DIFF[count]):
            dd = json.loads(json.dumps(data))
            dd["result"]["count"] = n
            put("%s_difficulty_%d" % (name, d), dd, (conditions or []) + [{"type": "rotarycraft:difficulty", "value": d}])
        return
    put(name, data, conditions)


def shapeless(name, out, inputs, count=1, conditions=None):
    put(name, {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [ing(i) for i in inputs],
               "result": result(out, count)}, conditions)


def if_tag(tag_key):
    return [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": TAGS[tag_key]}}]


TABLE = [{"type": "rotarycraft:config", "option": "table_machines"}]
TAGS["ingotAluminum"] = "c:ingots/aluminum"

# ---------------- addCraftItems
shaped("impeller", "impeller", [" S ", "SGS", " S "], {"S": "steelingot", "G": "steel_gear"})
shaped("impeller_tin", "impeller", [" S ", "SGS", " S "], {"S": "ingotTin", "G": "steel_gear"}, conditions=if_tag("ingotTin"))
shaped("compressor", "compressor", ["SSS", "SGS", "SSS"], {"S": "steelingot", "G": "steel_gear"})
shaped("turbine", "turbine", ["sss", "sGs", "sss"], {"s": "prop", "G": "compressor"})
shaped("diffuser", "diffuser", [" SS", "S  ", " SS"], {"S": "steelingot"})
shaped("radiator", "radiator", ["GGG", "PPP", "SSS"], {"G": "minecraft:gold_ingot", "S": "steelingot", "P": "pipe"})
shaped("condenser", "condenser", ["SPS", "PSP", "SPS"], {"S": "steelingot", "P": "pipe"}, count="SMALLERCRAFT")
shaped("goldcoil", "goldcoil", ["GGG", "GSG", "GGG"], {"S": "steelingot", "G": "minecraft:gold_ingot"})
shaped("goldcoil_electrum", "goldcoil", ["GGG", "GSG", "GGG"], {"S": "steelingot", "G": "ingotElectrum"}, conditions=if_tag("ingotElectrum"))
shaped("combustor", "combustor", ["SSS", "SRS", "SGS"], {"S": "steelingot", "G": "igniter", "R": "minecraft:redstone"})
# highcombustor, bedrock bearing/shaft/gear, diamond gear: Blast Furnace crafting, see below
shaped("cylinder", "cylinder", ["SSS", "S S", "SSS"], {"S": "steelingot"}, count=2)
shaped("aluminumcylinder", "aluminumcylinder", ["SSS", "S S", "SSS"], {"S": "silumin"}, count=2)
shaped("compoundturb", "compoundturb", [" tS", "tst", "St "], {"S": "turbine", "s": "tungsten_shaftcore", "t": "tungsteningot"})
shaped("compoundcompress", "compoundcompress", [" tS", "tst", "St "], {"S": "compressor", "s": "tungsten_shaftcore", "t": "tungsteningot"})
# shaft cores: "  s", " S ", "s  " with S = gear.getBaseItem(), s = gear.getShaftUnitItem()
BASE = {"wood": "plankWood", "stone": "minecraft:stone", "steel": "steelingot", "tungsten": "springtungsten", "diamond": "minecraft:diamond", "bedrock": "bedingot"}
UNIT = {m: ("minecraft:stick" if m == "wood" else m + "_shaft") for m in MATS}
for m in MATS:
    shaped(m + "_shaftcore", m + "_shaftcore", ["  s", " S ", "s  "], {"S": BASE[m], "s": UNIT[m]})
shaped("igniter", "igniter", ["G G", "SRS", "SSS"], {"S": "steelingot", "R": "minecraft:redstone", "G": "minecraft:gold_ingot"})
shaped("igniter_electrum", "igniter", ["G G", "SRS", "SSS"], {"S": "steelingot", "R": "minecraft:redstone", "G": "ingotElectrum"}, conditions=if_tag("ingotElectrum"))
shaped("waterplate", "waterplate", ["PPP", "PPP", "SSS"], {"P": "basepanel", "S": "springingot"})
shaped("prop", "prop", [" S ", " I ", " P "], {"P": "basepanel", "S": "steel_shaft", "I": "steelingot"})
shaped("prop_2", "prop", [" P ", " I ", " S "], {"P": "basepanel", "S": "steel_shaft", "I": "steelingot"})
shaped("hub", "hub", ["  B", " C ", "G  "], {"G": "steel_gear", "B": "steel_bearing", "C": "steel_shaftcore"})
shaped("mirror", "mirror", ["GGG", "III"], {"G": "minecraft:glass", "I": "minecraft:iron_ingot"})
shaped("mirror_silver", "mirror", ["GGG", "III"], {"G": "minecraft:glass", "I": "ingotSilver"}, conditions=if_tag("ingotSilver"))
shaped("railhead", "railhead", ["LLL", "LGL", "LLL"], {"G": "power", "L": "lim"})
shaped("railbase", "railbase", [" S ", "PGP"], {"P": "basepanel", "G": "steel_unit2", "S": "steel_gear"})
shaped("basepanel", "basepanel", ["SSS"], {"S": "steelingot"}, count="PARTCRAFT")
shaped("mount", "mount", ["S S", "SBS"], {"B": "basepanel", "S": "steelingot"})
shaped("mount_tin", "mount", ["S S", "SBS"], {"B": "basepanel", "S": "ingotTin"}, conditions=if_tag("ingotTin"))
shaped("drill", "drill", ["SSS", "SSS", " S "], {"S": "steelingot"})
shaped("presshead", "presshead", ["SOD", "ODB", "DBB"], {"S": "steelingot", "D": "minecraft:diamond", "O": "minecraft:obsidian", "B": "bedrockdust"})
shaped("screen", "screen", ["SGS", "SCS"], {"S": "steelingot", "C": "pcb", "G": "minecraft:glass_pane"})
shaped("mixer", "mixer", [" S ", "SIS", " S "], {"S": "steelingot", "I": "impeller"})
shaped("saw", "saw", ["S S", " C ", "S S"], {"S": "steelingot", "C": "steel_gear"})
shaped("pcb", "pcb", ["PGP", "RER", "GPG"], {"P": "steelingot", "G": "minecraft:gold_ingot", "R": "minecraft:redstone", "E": "minecraft:ender_pearl"}, count=2)
shaped("pcb_silicon", "pcb", ["PGP", "RER", "GPG"], {"P": "steelingot", "G": "minecraft:gold_ingot", "R": "minecraft:redstone", "E": "silicon"}, count=3)
shaped("pcb_electrum", "pcb", ["PGP", "RER", "GPG"], {"P": "steelingot", "G": "ingotElectrum", "R": "minecraft:redstone", "E": "ingotCopper"}, conditions=if_tag("ingotElectrum"))
shaped("sonar", "sonar", [" S ", "SNS", "RCR"], {"S": "steelingot", "R": "minecraft:redstone", "N": "minecraft:note_block", "C": "pcb"})
shaped("belt", "belt", ["LLL", "LSL", "LLL"], {"L": "minecraft:leather", "S": "steelingot"}, count="BELTCRAFT")
shaped("stone_bearing", "stone_bearing", ["LLL", "LSL", "LLL"], {"L": "ballbearing", "S": "stone_gear"})
shaped("steel_bearing", "steel_bearing", ["LLL", "LSL", "LLL"], {"L": "ballbearing", "S": "steelingot"})
shaped("tungsten_bearing", "tungsten_bearing", ["LLL", "LSL", "LLL"], {"L": "ballbearing", "S": "springtungsten"})
shaped("diamond_bearing", "diamond_bearing", ["LLL", "LSL", "LLL"], {"L": "ballbearing", "S": "diamond_gear"})
shapeless("ballbearing", "ballbearing", ["steelingot"], count=4)
shaped("ballbearing_16", "ballbearing", ["SS", "SS"], {"S": "steelingot"}, count=16)
shaped("brake", "brake", [" g ", "SBS", " G "], {"g": "steel_unit2", "G": "steel_gear", "S": "steel_shaft", "B": "steel_bearing"})
SKIPPED.append("tenscoil, bedrockcoil: need the spring/strong coil tools (step 5)")
shaped("lens", "lens", [" D ", "DGD", " D "], {"D": "minecraft:diamond", "G": "blast_glass"})
shaped("barrel", "barrel", ["OOO", "gtG", "OOO"], {"t": "tungsteningot", "O": "minecraft:obsidian", "G": "blast_glass", "g": "minecraft:glowstone"})
shaped("power", "power", ["RER", "GGG", "SSS"], {"R": "minecraft:redstone", "G": "minecraft:gold_ingot", "E": "minecraft:ender_eye", "S": "steelingot"}, count=2)
shaped("power_silicon", "power", ["RER", "GGG", "SSS"], {"R": "minecraft:redstone", "G": "minecraft:gold_ingot", "E": "silicon", "S": "steelingot"}, count=3)
shaped("power_electrum", "power", ["RER", "GGG", "SSS"], {"R": "minecraft:redstone", "G": "ingotElectrum", "E": "ingotCopper", "S": "steelingot"}, conditions=if_tag("ingotElectrum"))
shaped("bulb", "bulb", ["GGG", "BDB", "BRB"], {"D": "minecraft:nether_star", "G": "minecraft:glowstone", "R": "minecraft:redstone", "B": "minecraft:blaze_rod"})
for m in MATS:
    sh, g = UNIT[m], m + "_gear"
    shaped(m + "_unit2", m + "_unit2", [" GB", "BG "], {"B": sh, "G": g})
    shaped(m + "_unit4", m + "_unit4", [" GB", "BG "], {"B": sh, "G": m + "_unit2"})
    shaped(m + "_unit8", m + "_unit8", [" gB", "BG "], {"B": sh, "G": m + "_unit4", "g": m + "_unit2"})
    shaped(m + "_unit8_2", m + "_unit8", [" gB", "BG "], {"B": sh, "G": m + "_unit2", "g": m + "_unit4"})
    shaped(m + "_unit16", m + "_unit16", [" gB", "BG "], {"B": sh, "G": m + "_unit8", "g": m + "_unit2"})
    shaped(m + "_unit16_2", m + "_unit16", [" gB", "BG "], {"B": sh, "G": m + "_unit2", "g": m + "_unit8"})
    shaped(m + "_unit16_3", m + "_unit16", [" GB", "BG "], {"B": sh, "G": m + "_unit4"})
shaped("wood_gear", "wood_gear", [" W ", "WWW", " W "], {"W": "plankWood"})
shaped("stone_gear", "stone_gear", [" W ", "WWW", " W "], {"W": "minecraft:stone"}, count=2)
shaped("steel_gear", "steel_gear", [" B ", "BBB", " B "], {"B": "steelingot"}, count="PARTCRAFT")
for d, n in zip((1, 2, 3), DIFF["PARTCRAFT"]):
    put("tungsten_gear_difficulty_%d" % d, {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" W ", "WWW", " W "],
        "key": {"W": ing("springtungsten")}, "result": result("tungsten_gear", 5 * n // 3)}, [{"type": "rotarycraft:difficulty", "value": d}])
shaped("stone_shaft", "stone_shaft", ["  B", " B ", "B  "], {"B": "minecraft:stone"}, count=2)
shaped("steel_shaft", "steel_shaft", ["  B", " B ", "B  "], {"B": "steelingot"}, count="PARTCRAFT")
shaped("tungsten_shaft", "tungsten_shaft", ["  B", " B ", "B  "], {"B": "springtungsten"}, count="PARTCRAFT")
shaped("diamond_shaft", "diamond_shaft", ["  B", " B ", "B  "], {"B": "minecraft:diamond"}, count="PARTCRAFT")
shaped("wormgear", "wormgear", ["S  ", " G ", "  S"], {"S": "steel_shaft", "G": "steel_gear"})
shaped("lim", "lim", ["WRW", "NNN"], {"W": "goldcoil", "N": "steelingot", "R": "minecraft:redstone"})
shaped("generator", "generator", ["  G", " C ", "G  "], {"G": "goldcoil", "C": "steel_shaftcore"})
shaped("chain", "chain", ["s s", " s ", "s s"], {"s": "steelingot"}, count=4)

# ---------------- machines (Worktable, or crafting table with TABLEMACHINES)
MOUNT = {"wood": "plankWood", "stone": "minecraft:smooth_stone_slab"}
for m in MATS:
    comp = {"rotarycraft:shaft_material": m}
    if m in MOUNT:
        shaped("shaft_" + m, "shaft", ["BSB", "BBB"], {"B": MOUNT[m], "S": UNIT[m]}, count=8, components=comp, conditions=TABLE)
    else:
        shaped("shaft_" + m, "shaft", ["S", "M"], {"M": "mount", "S": UNIT[m]}, count=8, components=comp, conditions=TABLE)
shaped("shaft_cross", "shaft", [" S ", "SSS", " M "], {"M": "mount", "S": "steel_shaft"}, components={"rotarycraft:shaft_cross": True, "rotarycraft:shaft_material": "steel"}, conditions=TABLE)
shaped("bevel_gears", "bevel_gears", ["ISB", "SGB", "BBB"], {"B": "basepanel", "I": "steelingot", "S": "steel_shaft", "G": "steel_gear"}, count=4, conditions=TABLE)
shaped("shaft_junction", "shaft_junction", ["ISP", "SGP", "ISP"], {"P": "basepanel", "I": "steelingot", "S": "steel_shaft", "G": "steel_gear"}, count=2, conditions=TABLE)
shaped("clutch", "clutch", ["S", "M", "R"], {"M": "mount", "S": "steel_shaft", "R": "minecraft:redstone"}, conditions=TABLE)
shaped("fuel_enhancer", "fuel_enhancer", ["PGP", "gMg", "PGP"], {"G": "minecraft:glass_pane", "M": "mixer", "P": "basepanel", "g": "minecraft:gold_ingot"}, conditions=TABLE)
# solar plant (original RotaryRecipes: MIRROR.addCrafting, SOLARTOWER.addOreRecipe with dyeBlack)
shaped("solar_mirror", "solar_mirror", ["bmb", " g ", "pcp"], {"b": "blast_glass", "p": "basepanel", "c": "pcb", "m": "mirror", "g": "steel_gear"}, conditions=TABLE)
shaped("solar_tower", "solar_tower", ["pPp", "iPi", "pPp"], {"p": "basepanel", "P": "pipe", "i": "dyeBlack"}, conditions=TABLE)
shaped("pipe_pump", "pipe_pump", ["BBB", "PIP", "BBB"], {"B": "steelingot", "I": "impeller", "P": "pipe"}, conditions=TABLE)
shaped("van_de_graff", "van_de_graff", ["shs", "gbg", "php"], {"h": "hub", "p": "basepanel", "b": "belt", "g": "minecraft:glass_pane", "s": "steelingot"}, conditions=TABLE)
shaped("drying_bed", "drying_bed", ["S S", "SPS", "S S"], {"P": "basepanel", "S": "steelingot"}, conditions=TABLE)
shaped("winder", "winder", [" ss", " hg", "ppp"], {"h": "steel_shaft", "s": "steelingot", "g": "steel_unit2", "p": "basepanel"}, conditions=TABLE)
shaped("spring", "spring", [" S ", "S S", " S "], {"S": "steelingot"})
shaped("filling_station", "filling_station", ["ppS", " iR", "ppB"], {"B": "basepanel", "S": "steelingot", "i": "impeller", "p": "pipe", "R": "reservoir"}, conditions=TABLE)
shaped("compactor", "compactor", ["SPS", "PGP", "#P#"], {"#": "basepanel", "S": "steelingot", "P": "presshead", "G": "tungsten_unit16"}, conditions=TABLE)
shaped("heat_ray", "heat_ray", ["OOO", "BLb", "#P#"], {"#": "basepanel", "B": "bulb", "b": "barrel", "P": "power", "L": "lens", "O": "minecraft:obsidian"}, conditions=TABLE)
shaped("refrigerator", "refrigerator", ["SPS", "CcD", "pPp"], {"p": "basepanel", "P": "pipe", "D": "diffuser", "C": "compressor", "c": "condenser", "S": "steelingot"}, conditions=TABLE)
shaped("cooling_fin", "cooling_fin", ["SSS", "SSS", "PPP"], {"P": "basepanel", "S": "steel_shaft"}, count=3, conditions=TABLE)
shaped("cooling_fin_tin", "cooling_fin", ["SSS", "SSS", "PPP"], {"P": "ingotTin", "S": "ingotCopper"}, count=2, conditions=TABLE)
shaped("dynamometer", "dynamometer", [" S ", " E ", " Ms"], {"s": "screen", "M": "mount", "S": "steel_shaft", "E": "minecraft:ender_pearl"}, count=2, conditions=TABLE)
shaped("dynamometer_silicon", "dynamometer", [" S ", " E ", " Ms"], {"s": "screen", "M": "mount", "S": "steel_shaft", "E": "silicon"}, count=4, conditions=TABLE)
FLY = ["wood", "stone", "iron", "gold", "tungsten", "depletedu", "bedrock"]
for f in FLY:
    shaped("flywheel_" + f, "flywheel", ["W", "M"], {"W": f + "_flywheel_core", "M": "mount"}, components={"rotarycraft:flywheel_type": f}, conditions=TABLE)
for m in MATS:
    for r in (2, 4, 8, 16):
        comp = {"rotarycraft:gearbox_type": m, "rotarycraft:gear_ratio": r, "rotarycraft:gearbox_data": {"damage": 0, "lube": 0, "bearing": m}}
        if m in MOUNT:
            shaped("gearbox_%s_%d" % (m, r), "gearbox", ["MGM", "MMM"], {"M": MOUNT[m], "G": "%s_unit%d" % (m, r)}, components=comp, conditions=TABLE)
        else:
            shaped("gearbox_%s_%d" % (m, r), "gearbox", ["G", "M"], {"M": "mount", "G": "%s_unit%d" % (m, r)}, components=comp, conditions=TABLE)
shaped("dc_engine", "dc_engine", ["SSS", "SRs", "PRP"], {"S": "steelingot", "R": "minecraft:redstone", "P": "basepanel", "s": "steel_shaft"}, conditions=TABLE)
shaped("wind_engine", "wind_engine", ["SSS", "SHS", "SSS"], {"S": "prop", "H": "hub"}, count=2, conditions=TABLE)
shaped("hydro_engine", "hydro_engine", ["PPP", "PGP", "PPP"], {"P": "waterplate", "G": "diamond_shaftcore"}, conditions=TABLE)
shaped("steam_engine", "steam_engine", ["ccc", "CIs", "PGP"], {"c": "minecraft:cobblestone", "I": "impeller", "P": "basepanel", "s": "steel_shaft", "G": "minecraft:gold_ingot", "C": "condenser"}, conditions=TABLE)
shaped("steam_engine_copper", "steam_engine", ["ccc", "CIs", "PGP"], {"c": "minecraft:cobblestone", "I": "impeller", "P": "basepanel", "s": "steel_shaft", "G": "ingotCopper", "C": "condenser"}, conditions=TABLE + if_tag("ingotCopper"))
shaped("gas_engine", "gas_engine", ["CgC", "SGs", "PIP"], {"g": "minecraft:gold_ingot", "S": "igniter", "I": "impeller", "P": "basepanel", "s": "steel_shaft", "G": "steel_unit2", "C": "cylinder"}, conditions=TABLE)
shaped("ac_engine", "ac_engine", ["SSS", "SGs", "PRP"], {"S": "minecraft:gold_ingot", "R": "minecraft:redstone", "P": "basepanel", "s": "steel_shaft", "G": "goldcoil"}, conditions=TABLE)
shaped("ac_engine_electrum", "ac_engine", ["SSS", "SGs", "PRP"], {"S": "ingotElectrum", "R": "minecraft:redstone", "P": "basepanel", "s": "steel_shaft", "G": "goldcoil"}, conditions=TABLE + if_tag("ingotElectrum"))
shaped("sport_engine", "sport_engine", ["CrC", "SGs", "PIP"], {"C": "aluminumcylinder", "S": "igniter", "I": "impeller", "P": "basepanel", "s": "steel_shaft", "r": "radiator", "G": "steel_unit2"}, conditions=TABLE)
shaped("micro_engine", "micro_engine", ["CSS", "cTs", "PPP"], {"S": "silumin", "C": "compressor", "c": "highcombustor", "T": "turbine", "P": "basepanel", "s": "steel_shaft"}, conditions=TABLE)
shaped("jet_engine", "jet_engine", ["DCS", "ScS", "PTs"], {"S": "silumin", "D": "diffuser", "C": "compoundcompress", "c": "highcombustor", "T": "compoundturb", "P": "basepanel", "s": "steel_shaft"}, conditions=TABLE)
# ECU (machine recipe); the electrum/copper variants were added when those ores existed.
shaped("ecu", "ecu", ["IPI", "IGI", "IRI"], {"I": "steelingot", "G": "minecraft:gold_ingot", "P": "pcb", "R": "minecraft:redstone"}, conditions=TABLE)
shaped("ecu_electrum", "ecu", ["IPI", "IGI", "IRI"], {"I": "steelingot", "G": "ingotElectrum", "P": "pcb", "R": "minecraft:redstone"}, conditions=TABLE + if_tag("ingotElectrum"))
shaped("ecu_copper", "ecu", ["IPI", "IGI", "IRI"], {"I": "steelingot", "G": "ingotCopper", "P": "pcb", "R": "minecraft:redstone"}, conditions=TABLE + if_tag("ingotCopper") + [{"type": "neoforge:tag_empty", "tag": TAGS["ingotElectrum"]}])

# Pipes (machine recipes, PIPECRAFT 32/16/8 per difficulty; "Rotate Hose/Pipe/Fuel Line Recipes" turns the pattern by 90 degrees)
ROTATE = {"type": "rotarycraft:config", "option": "rotate_hose"}
for pid, mat in [("hose", "plankWood"), ("pipe", "steelingot"), ("fuel_line", "minecraft:obsidian")]:
    shaped(pid, pid, ["WGW", "WGW", "WGW"], {"G": "minecraft:glass", "W": mat}, count="PIPECRAFT", conditions=TABLE + [{"type": "neoforge:not", "value": ROTATE}])
    shaped(pid + "_rotated", pid, ["WWW", "GGG", "WWW"], {"G": "minecraft:glass", "W": mat}, count="PIPECRAFT", conditions=TABLE + [ROTATE])
for rot, pat in (("", ["BGB", "BGB", "BGB"]), ("_rotated", ["BBB", "GGG", "BBB"])):
    shaped("bedrock_pipe" + rot, "bedrock_pipe", pat, {"G": "blast_glass", "B": "bedingot"}, count="PIPECRAFT",
           conditions=TABLE + ([ROTATE] if rot else [{"type": "neoforge:not", "value": ROTATE}]))

# Engine upgrades (ItemRegistry.UPGRADE.addMetaRecipe - crafting table, not machine recipes).
# REDSTONE (9): without Project Red the latch is a clock, without BuildCraft silicon the pulsating chipset is an ender pearl.
shaped("upgrade_redstone", "upgrade_redstone", ["rQr", "qsq", "rEr"], {"r": "minecraft:redstone", "s": "steelingot", "q": "minecraft:quartz", "E": "minecraft:ender_pearl", "Q": "minecraft:clock"}, worktable=True)
# PERFORMANCE (0)
shaped("upgrade_performance", "upgrade_performance", ["sRs", "gGg", " b "], {"s": "silumin", "b": "basepanel", "R": "radiator", "G": "steel_unit2", "g": "minecraft:gold_ingot"}, worktable=True)
# AFTERBURNER (6)
shaped("upgrade_afterburner", "upgrade_afterburner", ["SEI", "ERE", "SEI"], {"R": "compoundturb", "S": "highcombustor", "I": "igniter", "E": "bedrockdust"}, worktable=True)
# Items that use an engine as ingredient (addCraftItems: radar uses the DC engine)
shaped("radar", "radar", ["SSS", " G ", "RMR"], {"S": "steelingot", "R": "minecraft:redstone", "G": "minecraft:gold_ingot", "M": "dc_engine"})
shaped("railaim", "railaim", ["sds", "CRC", "sgs"], {"R": "radar", "C": "pcb", "s": "steelingot", "d": "minecraft:diamond", "g": "generator"})

# ---------------- tools (addToolItems)
TAGS["stickWood"] = "c:rods/wooden"
shaped("screwdriver_iron", "screwdriver", ["I  ", " S ", "  W"], {"S": "stickWood", "I": "minecraft:iron_ingot", "W": "plankWood"}, worktable=True)
shaped("screwdriver", "screwdriver", ["I  ", " S ", "  W"], {"S": "stickWood", "I": "steelingot", "W": "plankWood"}, worktable=True)
shaped("meter", "meter", [" W ", "WEW", "SSS"], {"S": "steelingot", "E": "minecraft:redstone", "W": "plankWood"}, worktable=True)

# ---------------- Blast Furnace and Worktable themselves (addPostLoadRecipes: addOreRecipeToBoth / addRecipeToBoth, no gating by default)
shaped("blast_furnace", "blast_furnace", ["BBB", "BrB", "BBB"], {"B": "minecraft:stone_bricks", "r": "minecraft:redstone"}, worktable=True)
shaped("worktable", "worktable", [" C ", "SBS", "srs"], {"r": "minecraft:redstone", "S": "steelingot", "B": "minecraft:bricks", "C": "minecraft:crafting_table", "s": "minecraft:smooth_stone_slab"}, worktable=True)

# ---------------- flywheel cores (addPostLoadRecipes, crafting table): raw material around a steel gear
for f, raw in [("wood", "plankWood"), ("stone", "minecraft:stone"), ("iron", "minecraft:iron_ingot"), ("gold", "minecraft:gold_ingot"),
               ("tungsten", "springtungsten"), ("bedrock", "bedingot")]:
    shaped(f + "_flywheel_core", f + "_flywheel_core", ["WWW", "WGW", "WWW"], {"W": raw, "G": "steel_gear"})
SKIPPED.append("depleted uranium flywheel core: needs a depleted uranium ingot (ReactorCraft)")

# ---------------- Bedrock Breaker (machine recipe; tungsten ingots come from the Extractor, not ported yet)
shaped("bedrock_breaker", "bedrock_breaker", ["BDt", "BSO", "BDt"], {"t": "tungsteningot", "S": "steelingot", "D": "minecraft:diamond", "O": "minecraft:obsidian", "B": "basepanel"}, conditions=TABLE)
# ---------------- Borer (machine recipe: SSS/DGC/BBB - steel ingots, drill, steel 2:1 gear unit, PCB, base panels)
shaped("borer", "borer", ["SSS", "DGC", "BBB"], {"B": "basepanel", "S": "steelingot", "D": "drill", "G": "steel_unit2", "C": "pcb"}, conditions=TABLE)

# ---------------- Friction Heater (machine recipe) and its furnace recipes (RecipesFrictionHeater)
shaped("friction_heater", "friction_heater", ["S  ", "Sss", "SPP"], {"P": "basepanel", "S": "steelingot", "s": "steel_shaft"}, conditions=TABLE)
put("friction_heater/silicon", {"type": "rotarycraft:friction_heater", "ingredient": ing("silicondust"), "result": result("silicon", 1), "temperature": 800, "duration": 200})
put("friction_heater/tungsten", {"type": "rotarycraft:friction_heater", "ingredient": ing("tungstenflakes"), "result": result("tungsteningot", 1), "temperature": 1350, "duration": 600})

# ---------------- Extractor (machine recipe) and flake smelting (RotaryRecipes.addSmelting)
shaped("extractor", "extractor", ["SWS", "siD", "PIN"], {"D": "drill", "P": "basepanel", "S": "steelingot", "I": "steel_shaft", "s": "minecraft:stone", "i": "impeller", "N": "minecraft:netherrack", "W": "plankWood"}, conditions=TABLE)
for flake, out, n, xp in [("coaloreflakes", "minecraft:coal", 1, 0.1), ("ironoreflakes", "minecraft:iron_ingot", 1, 0.7), ("goldoreflakes", "minecraft:gold_ingot", 1, 1),
                          ("redoreflakes", "minecraft:redstone", 4, 0.5), ("lapisoreflakes", "minecraft:lapis_lazuli", 6, 0.6), ("diamondoreflakes", "minecraft:diamond", 1, 1),
                          ("emeraldoreflakes", "minecraft:emerald", 1, 1), ("quartzflakes", "minecraft:quartz", 1, 1), ("silverflakes", "silveringot", 1, 1)]:
    put("smelting/" + flake, {"type": "minecraft:smelting", "category": "misc", "ingredient": ing(flake), "result": result(out, n), "experience": xp, "cookingtime": 200})

# ---------------- Grinder (machine recipe) and RecipesGrinder. Ore-dictionary inputs (logWood, plankWood) become the vanilla tags;
# items the original listed one by one stay single items (1.7.10 blocks without metadata = the oak/plain variant).
shaped("grinder", "grinder", ["B B", "SGS", "PPP"], {"B": "steelingot", "G": "steel_gear", "P": "basepanel", "S": "saw"}, conditions=TABLE)
def grind(name, inp, out, n=1):
    i = {"tag": inp[1:]} if inp.startswith("#") else ing(inp)
    put("grinder/" + name, {"type": "rotarycraft:grinder", "ingredient": i, "result": result(out, n)})
MC = "minecraft:"
for name, inp, out, n in [
        ("stone", MC + "stone", MC + "cobblestone", 1), ("cobblestone", MC + "cobblestone", MC + "gravel", 1), ("gravel", MC + "gravel", MC + "sand", 1),
        ("glass", MC + "glass", MC + "sand", 1), ("sandstone", MC + "sandstone", MC + "sand", 1), ("sandstone_stairs", MC + "sandstone_stairs", MC + "sand", 6),
        ("glowstone", MC + "glowstone", MC + "glowstone_dust", 4), ("stone_bricks", MC + "stone_bricks", MC + "cobblestone", 1),
        ("furnace", MC + "furnace", MC + "cobblestone", 8), ("bricks", MC + "bricks", MC + "clay_ball", 4), ("clay", MC + "clay", MC + "clay_ball", 4),
        ("brick_stairs", MC + "brick_stairs", MC + "clay_ball", 6), ("brick", MC + "brick", MC + "clay_ball", 1),
        ("cobblestone_stairs", MC + "cobblestone_stairs", MC + "gravel", 2), ("stone_brick_stairs", MC + "stone_brick_stairs", MC + "cobblestone", 2),
        ("netherrack", MC + "netherrack", "netherrackdust", 1), ("soul_sand", MC + "soul_sand", "tar", 1), ("wheat", MC + "wheat", "flour", 3),
        ("bedingot", "bedingot", "bedrockdust", 4), ("aluminumingot", "aluminumingot", "aluminumpowder", 1),
        ("sugar_cane", MC + "sugar_cane", MC + "sugar", 3), ("bone", MC + "bone", MC + "bone_meal", 9), ("blaze_rod", MC + "blaze_rod", MC + "blaze_powder", 6),
        ("ice", MC + "ice", MC + "snowball", 4),
        ("logs", "#minecraft:logs", "sawdust", 16), ("planks", "#minecraft:planks", "sawdust", 4),
        ("note_block", MC + "note_block", "sawdust", 32), ("jukebox", MC + "jukebox", "sawdust", 32), ("oak_fence", MC + "oak_fence", "sawdust", 4),
        ("oak_stairs", MC + "oak_stairs", "sawdust", 6), ("birch_stairs", MC + "birch_stairs", "sawdust", 6), ("spruce_stairs", MC + "spruce_stairs", "sawdust", 6),
        ("jungle_stairs", MC + "jungle_stairs", "sawdust", 6), ("acacia_stairs", MC + "acacia_stairs", "sawdust", 6), ("dark_oak_stairs", MC + "dark_oak_stairs", "sawdust", 6),
        ("chest", MC + "chest", "sawdust", 32), ("crafting_table", MC + "crafting_table", "sawdust", 16), ("ladder", MC + "ladder", "sawdust", 4),
        ("oak_pressure_plate", MC + "oak_pressure_plate", "sawdust", 8), ("stone_pressure_plate", MC + "stone_pressure_plate", MC + "cobblestone", 2),
        ("bowl", MC + "bowl", "sawdust", 12), ("oak_door", MC + "oak_door", "sawdust", 24), ("oak_sign", MC + "oak_sign", "sawdust", 24),
        ("stick", MC + "stick", "sawdust", 2), ("oak_trapdoor", MC + "oak_trapdoor", "sawdust", 24), ("oak_fence_gate", MC + "oak_fence_gate", "sawdust", 16),
        ("coal", MC + "coal", "coaldust", 1), ("canola_seeds", "canola_seeds", "canola_husks", 1),
        # addFlowerDyes: 6 dye per flower
        ("dandelion", MC + "dandelion", MC + "yellow_dye", 6), ("poppy", MC + "poppy", MC + "red_dye", 6), ("blue_orchid", MC + "blue_orchid", MC + "light_blue_dye", 6),
        ("allium", MC + "allium", MC + "magenta_dye", 6), ("azure_bluet", MC + "azure_bluet", MC + "light_gray_dye", 6), ("red_tulip", MC + "red_tulip", MC + "red_dye", 6),
        ("orange_tulip", MC + "orange_tulip", MC + "orange_dye", 6), ("white_tulip", MC + "white_tulip", MC + "light_gray_dye", 6), ("pink_tulip", MC + "pink_tulip", MC + "pink_dye", 6),
        ("oxeye_daisy", MC + "oxeye_daisy", MC + "light_gray_dye", 6), ("lilac", MC + "lilac", MC + "magenta_dye", 6), ("rose_bush", MC + "rose_bush", MC + "red_dye", 6),
        ("peony", MC + "peony", MC + "pink_dye", 6)]:
    grind(name, inp, out, n)
SKIPPED.append("grinder: other mods' recipes (obsidian/ender dust, AE2, Botania, Ex Nihilo, HarvestCraft, Hexcraft, Thermal rods)")
# Dense canola seeds (addCraftItems)
shaped("dense_canola_seeds", "dense_canola_seeds", ["DDD", "DDD", "DDD"], {"D": "canola_seeds"})
shapeless("canola_seeds_from_dense", "canola_seeds", ["dense_canola_seeds"], count=9)

# ---------------- Other furnace recipes (addFurnace, addCraftItems)
for name, inp, out, n, xp in [("aluminumpowder", "aluminumpowder", "aluminumingot", 1, 0.4), ("cleansludge", "cleansludge", "ethanol", 1, 0.5),
                              ("ironscrap", "ironscrap", "minecraft:iron_ingot", 1, 0.4), ("flour", "flour", "minecraft:bread", 1, 0.2)]:
    put("smelting/" + name, {"type": "minecraft:smelting", "category": "misc", "ingredient": ing(inp), "result": result(out, n), "experience": xp, "cookingtime": 200})

# ---------------- Fermenter and Centrifuge (machine recipes) and RecipesCentrifuge (chances in percent, rolled one by one)
shaped("fermenter", "fermenter", ["BPB", "PIP", "BPB"], {"B": "steelingot", "I": "impeller", "P": "basepanel"}, conditions=TABLE)
shaped("fermenter_tin", "fermenter", ["BPB", "PIP", "BPB"], {"B": "ingotTin", "I": "impeller", "P": "basepanel"}, conditions=TABLE + if_tag("ingotTin"))
# Fan (MachineRegistry.FAN.addOreRecipe, W = plankWood)
shaped("fan", "fan", ["WWW", "WIW", "#s#"], {"#": "basepanel", "W": "plankWood", "I": "impeller", "s": "steel_shaft"}, conditions=TABLE)
# Mob Harvester (MachineRegistry.MOBHARVESTER.addCrafting)
shaped("mob_harvester", "mob_harvester", ["shs", "sps"], {"h": "igniter", "p": "minecraft:ender_pearl", "s": "basepanel"}, conditions=TABLE)
shaped("aggregator", "aggregator", ["SPS", "GCG", "SsS"], {"s": "steel_shaft", "G": "minecraft:glass_pane", "S": "steelingot", "P": "basepanel", "C": "compressor"}, conditions=TABLE)
shaped("spiller", "spiller", ["sps", "s s"], {"p": "pipe", "s": "steelingot"}, conditions=TABLE)
shaped("filler", "filler", ["sss", "sps", "s s"], {"p": "minecraft:chest", "s": "steelingot"}, conditions=TABLE)
shaped("smoke_detector", "smoke_detector", [" S ", "RRR", " N "], {"S": "minecraft:smooth_stone_slab", "R": "minecraft:redstone", "N": "minecraft:note_block"}, conditions=TABLE)
shaped("player_detector", "player_detector", ["LRL", "OGO", "OPO"], {"L": "minecraft:lapis_lazuli", "R": "radar", "O": "minecraft:obsidian", "P": "basepanel", "G": "minecraft:gold_ingot"}, conditions=TABLE)
shaped("mob_radar", "mob_radar", [" rs", " g ", "pcp"], {"r": "radar", "s": "screen", "c": "pcb", "g": "steel_unit2", "p": "basepanel"}, conditions=TABLE)
shaped("lava_maker", "lava_maker", ["SRS", "PGP", "SsS"], {"s": "steel_shaft", "S": "steelingot", "R": "reservoir", "P": "basepanel", "G": "steel_gear"}, conditions=TABLE)
shaped("heater", "heater", ["sBs", "prp", "scs"], {"r": "tungsteningot", "s": "steelingot", "B": "minecraft:iron_bars", "p": "basepanel", "c": "combustor"}, conditions=TABLE)
shaped("composter", "composter", [" S ", "S S", "BBB"], {"B": "basepanel", "S": "steelingot"}, conditions=TABLE)
shaped("crystallizer", "crystallizer", ["SFS", "FIF", "BBB"], {"S": "steelingot", "B": "basepanel", "F": "cooling_fin", "I": "impeller"}, conditions=TABLE)
shaped("lamp", "lamp", ["SGS", "GgG", "SGS"], {"S": "steelingot", "G": "minecraft:glass", "g": "minecraft:glowstone"}, conditions=TABLE)
shaped("hydrator", "hydrator", ["sls", "p p", "PpP"], {"s": "steelingot", "l": "minecraft:ladder", "p": "plankWood", "P": "basepanel"}, conditions=TABLE)
shaped("wetter", "wetter", ["S S", "gmg", "SPS"], {"S": "steelingot", "g": "minecraft:glass_pane", "m": "mixer", "P": "basepanel"}, conditions=TABLE)
shaped("particle_emitter", "particle_emitter", ["SDS", "PCP", "SIS"], {"S": "steelingot", "D": "minecraft:dispenser", "P": "basepanel", "C": "pcb", "I": "impeller"}, count=4, conditions=TABLE)
shaped("multi_clutch", "multi_clutch", ["PSP", "SGS", "RSR"], {"R": "minecraft:redstone", "S": "steel_shaft", "G": "steel_unit2", "P": "basepanel"}, conditions=TABLE)
shaped("big_furnace", "big_furnace", ["SFS", "FRF", "SRS"], {"S": "basepanel", "F": "minecraft:furnace", "R": "reservoir"}, conditions=TABLE)
shaped("spillway", "spillway", ["S  ", "PSP", "PpP"], {"S": "steelingot", "P": "basepanel", "p": "pipe"}, conditions=TABLE)
shaped("fluid_compressor", "fluid_compressor", ["SIS", "PRP", "PPP"], {"P": "basepanel", "S": "steelingot", "I": "impeller", "R": "reservoir"}, conditions=TABLE)
shaped("line_builder", "line_builder", ["sbs", "sps", "PgP"], {"g": "steel_unit2", "p": "minecraft:piston", "P": "basepanel", "b": "bedingot", "s": "steelingot"}, conditions=TABLE)
shaped("magnetizer", "magnetizer", ["p p", "gmg", "prp"], {"r": "minecraft:redstone", "p": "basepanel", "m": "mount", "g": "goldcoil"}, conditions=TABLE)
shaped("beam_mirror", "beam_mirror", [" m ", " s ", " p "], {"p": "basepanel", "m": "mirror", "s": "steelingot"}, conditions=TABLE)
shaped("item_refresher", "item_refresher", ["ses", "epe", "ses"], {"p": "minecraft:ender_pearl", "s": "steelingot", "e": "minecraft:lapis_lazuli"}, conditions=TABLE)
shaped("grindstone", "grindstone", ["S S", "sBs", "ppp"], {"p": "basepanel", "s": "steel_shaft", "S": "steelingot", "B": "minecraft:stone"}, conditions=TABLE)
shaped("bucket_filler", "bucket_filler", ["SPS", "PCP", "SPS"], {"P": "pipe", "S": "steelingot", "C": "minecraft:chest"}, conditions=TABLE)
shaped("sorting", "sorting", ["SHS", " C ", "P P"], {"P": "basepanel", "S": "steelingot", "H": "minecraft:hopper", "C": "pcb"}, conditions=TABLE)
shaped("fertilizer", "fertilizer", ["PIP", " S ", "BCB"], {"P": "pipe", "S": "steel_shaft", "I": "impeller", "C": "minecraft:chest", "B": "basepanel"}, conditions=TABLE)
shaped("centrifuge", "centrifuge", ["SGS", "S S", "PgP"], {"P": "basepanel", "g": "steel_unit4", "S": "steelingot", "G": "minecraft:glass_pane"}, conditions=TABLE)
def centrifuge(name, inp, outs, fluid=None, fluid_chance=100, max_stack=1, conditions=None):
    d = {"type": "rotarycraft:centrifuge", "ingredient": ing(inp), "outputs": [{"item": result(o, n), "chance": c} for o, n, c in outs]}
    if fluid:
        d["fluid"] = {"id": fluid[0], "amount": fluid[1]}
        d["fluid_chance"] = fluid_chance
    if max_stack != 1:
        d["max_stack"] = max_stack
    put("centrifuge/" + name, d, conditions)
centrifuge("magma_cream", MC + "magma_cream", [(MC + "slime_ball", 1, 100), (MC + "blaze_powder", 1, 100)])
centrifuge("melon", MC + "melon_slice", [(MC + "melon_seeds", 4, 100)])
centrifuge("pumpkin", MC + "pumpkin", [(MC + "pumpkin_seeds", 12, 100)])
centrifuge("wheat", MC + "wheat", [(MC + "wheat_seeds", 4, 100)])
centrifuge("gravel", MC + "gravel", [(MC + "flint", 1, 50), (MC + "sand", 1, 75)])
centrifuge("netherrackdust", "netherrackdust", [(MC + "glowstone_dust", 1, 25), (MC + "gunpowder", 1, 80)])
centrifuge("dirt", MC + "dirt", [(MC + "sand", 1, 80), (MC + "clay", 1, 10), (MC + "wheat_seeds", 1, 2), (MC + "pumpkin_seeds", 1, 0.125),
                                 (MC + "melon_seeds", 1, 0.125), (MC + "oak_sapling", 1, 0.03125), (MC + "short_grass", 1, 0.0625)])
centrifuge("blaze_powder", MC + "blaze_powder", [(MC + "gunpowder", 1, 100)])
centrifuge("sludge", "sludge", [("cleansludge", 1, 80), ("cleansludge", 1, 20), ("compost", 1, 25)], max_stack=2)
# canola husks: roundUpToX(10, 0.75 * average DifficultyEffects.CANOLA) mB lubricant per difficulty
for d, amt in ((1, 160), (2, 90), (3, 30)):
    centrifuge("canola_husks_difficulty_%d" % d, "canola_husks", [], fluid=("rotarycraft:lubricant", amt), conditions=[{"type": "rotarycraft:difficulty", "value": d}])
SKIPPED.append("centrifuge: sulfur outputs (netherrack dust, blaze powder; mod ore item), Forestry/MagicBees/TiCo/galena recipes")

# ---------------- Flow-control pipes (machine recipes, 4 each)
shaped("valve", "valve", ["sGs", "OGO", "sGs"], {"O": "minecraft:redstone_block", "G": "minecraft:glass", "s": "steelingot"}, count=4, conditions=TABLE)
shaped("bypass", "bypass", ["OGO", "OGO", "OGO"], {"O": "minecraft:sandstone", "G": "minecraft:glass"}, count=4, conditions=TABLE)
shaped("separation", "separation", ["sGs", "OGO", "sGs"], {"O": "minecraft:lapis_block", "G": "minecraft:glass", "s": "steelingot"}, count=4, conditions=TABLE)
shaped("suction", "suction", ["SGS", "SGS", "SGS"], {"S": "minecraft:nether_bricks", "G": "minecraft:glass"}, count=4, conditions=TABLE)

# ---------------- Pump (machine recipe)
shaped("pump", "pump", ["SGS", "pIp", "PpP"], {"P": "basepanel", "p": "pipe", "I": "impeller", "G": "minecraft:glass_pane", "S": "steelingot"}, conditions=TABLE)

# ---------------- Reservoir (machine recipes; the glass pane variant comes covered)
shaped("reservoir", "reservoir", ["B B", "B B", "BBB"], {"B": "basepanel"}, conditions=TABLE)
shaped("reservoir_covered", "reservoir", ["BPB", "B B", "BBB"], {"B": "basepanel", "P": "minecraft:glass_pane"}, components={"rotarycraft:reservoir_data": {"cover": True}}, conditions=TABLE)

# ---------------- Pulse Jet Furnace (machine recipe) and RecipesPulseFurnace. Temperatures without an explicit value follow
# getDefaultMeltingTemp: half the DragonAPI ItemMaterialController melting point, clamped to 400-850 (iron items 780, gold items
# 530, items without a material 400).
shaped("pulse_jet_furnace", "pulse_jet_furnace", ["OCD", "PcO", "BBB"], {"B": "basepanel", "O": "minecraft:obsidian", "C": "compressor", "D": "diffuser", "c": "combustor", "P": "pipe"}, conditions=TABLE)
def pulse(name, inp, out, n, temp):
    put("pulse_jet/" + name, {"type": "rotarycraft:pulse_jet", "ingredient": ing(inp), "result": result(out, n), "temperature": temp})
pulse("blast_glass", MC + "obsidian", "blast_glass", 1, 850)
pulse("steel", MC + "iron_ingot", "steelingot", 2, 900)
pulse("redgold", "redgolddust", "redgoldingot", 1, 400)
IRONMAT = {"iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "iron_hoe", "iron_shovel", "iron_axe", "iron_pickaxe", "iron_sword",
           "flint_and_steel", "bucket", "minecart", "iron_door", "cauldron", "detector_rail"}
GOLDMAT = {"golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots", "golden_axe", "golden_sword", "golden_shovel", "golden_pickaxe",
           "golden_hoe", "powered_rail"}
for item, out, n in [("chainmail_helmet", "iron_ingot", 3), ("chainmail_boots", "iron_ingot", 2), ("chainmail_leggings", "iron_ingot", 4),
                     ("chainmail_chestplate", "iron_ingot", 5), ("iron_helmet", "iron_ingot", 5), ("iron_chestplate", "iron_ingot", 8),
                     ("iron_leggings", "iron_ingot", 7), ("iron_boots", "iron_ingot", 4), ("iron_hoe", "iron_ingot", 2), ("iron_shovel", "iron_ingot", 1),
                     ("iron_axe", "iron_ingot", 3), ("iron_pickaxe", "iron_ingot", 3), ("iron_sword", "iron_ingot", 2),
                     ("golden_helmet", "gold_ingot", 5), ("golden_chestplate", "gold_ingot", 8), ("golden_leggings", "gold_ingot", 7),
                     ("golden_boots", "gold_ingot", 4), ("golden_axe", "gold_ingot", 3), ("golden_sword", "gold_ingot", 2), ("golden_shovel", "gold_ingot", 1),
                     ("golden_pickaxe", "gold_ingot", 3), ("golden_hoe", "gold_ingot", 2),
                     ("diamond_helmet", "diamond", 5), ("diamond_chestplate", "diamond", 8), ("diamond_leggings", "diamond", 7), ("diamond_boots", "diamond", 4),
                     ("diamond_axe", "diamond", 3), ("diamond_sword", "diamond", 2), ("diamond_shovel", "diamond", 1), ("diamond_pickaxe", "diamond", 3),
                     ("diamond_hoe", "diamond", 2), ("iron_horse_armor", "iron_ingot", 7), ("diamond_horse_armor", "diamond", 7), ("golden_horse_armor", "gold_ingot", 7),
                     ("flint_and_steel", "iron_ingot", 1), ("bucket", "iron_ingot", 3), ("water_bucket", "iron_ingot", 3), ("lava_bucket", "iron_ingot", 3),
                     ("milk_bucket", "iron_ingot", 3), ("minecart", "iron_ingot", 5), ("iron_door", "iron_ingot", 6), ("cauldron", "iron_ingot", 7),
                     ("hopper_minecart", "iron_ingot", 10), ("detector_rail", "iron_ingot", 1), ("powered_rail", "gold_ingot", 1)]:
    temp = 780 if item in IRONMAT else 530 if item in GOLDMAT else 400
    pulse("recycle_" + item, MC + item, MC + out, n, temp)
SKIPPED.append("pulse jet: steel tool/armour recycling (items not ported), other mods' recipes")

# ---------------- Fractionation Unit (machine recipe)
shaped("fractionator", "fractionator", ["GFG", "GIG", "GPG"], {"P": "basepanel", "I": "mixer", "G": "minecraft:gold_ingot", "F": "fuel_line"}, conditions=TABLE)

# ---------------- Blast Furnace (RecipesBlastFurnace)
SAND = [{"item": "minecraft:sand"}, {"item": "minecraft:red_sand"}]  # original: Blocks.sand, any metadata


def add(item, chance, count=1):
    return {"ingredient": item if isinstance(item, list) else ing(item), "chance": chance, "count": count}


def blast(name, main, results, temperature, primary=None, secondary=None, tertiary=None, main_count=1, exact=False, bonus=0, xp=0.0, needs_empty=False, alloy=False):
    data = {"type": "rotarycraft:blast_furnace", "main": main if isinstance(main, list) else ing(main), "main_count": main_count, "exact": exact,
            "results": [result(i, c) for i, c in results], "bonus": bonus, "xp": xp, "temperature": temperature,
            "needs_empty_output": needs_empty, "alloy": alloy}
    for k, v in (("primary", primary), ("secondary", secondary), ("tertiary", tertiary)):
        if v:
            data[k] = v
    put("blast_furnace/" + name, data)


SMELT, BEDROCK, SMELT_XP = 600, 1450, 0.6
blast("hsla_coal", "minecraft:iron_ingot", [("steelingot", 1)], SMELT, add("minecraft:coal", 100), add("minecraft:gunpowder", 3.6), add(SAND, 0.2), xp=SMELT_XP)
blast("hsla_charcoal", "minecraft:iron_ingot", [("steelingot", 1)], SMELT, add("minecraft:charcoal", 100), add("minecraft:gunpowder", 3.2), add(SAND, 0.2), xp=SMELT_XP)
blast("hsla_coke", "minecraft:iron_ingot", [("steelingot", 1)], SMELT, add("coke", 100), add("minecraft:gunpowder", 1.8), add(SAND, 0.1), bonus=1, xp=SMELT_XP)
# Original output "steel block" is special-cased into 2 steel, 3 charcoal and 5 iron per iron block (BlastFurnace.make)
blast("hsla_block", "minecraft:iron_block", [("steelingot", 2), ("minecraft:charcoal", 3), ("minecraft:iron_ingot", 5)], SMELT,
      add("coke", 100), add("minecraft:gunpowder", 1.8 * 9), add(SAND, 0.1 * 9), xp=SMELT_XP * 9, needs_empty=True)
blast("bedrock_alloy", "steelingot", [("bedingot", 1)], BEDROCK, add("bedrockdust", 100, 4), exact=True, alloy=True)
blast("scrap", "scrap", [("steelingot", 1)], SMELT, main_count=9, exact=True)
blast("coke", "minecraft:coal", [("coke", 1)], 400)
blast("silicon", SAND, [("silicondust", 1)], 700, add("aluminumpowder", 25), add("minecraft:blaze_powder", 2.5), bonus=0.8)
blast("spring_steel", "steelingot", [("springingot", 1)], 1000, add("coke", 75), add("minecraft:redstone", 40))
blast("silumin", "ingotAluminum", [("silumin", 1)], 900, add("silicondust", 20))
blast("spring_tungsten", "springingot", [("springtungsten", 1)], 1100, add("tungstenflakes", 5), None, add("minecraft:obsidian", 20))


def blast_crafting(name, out, count, temperature, speed, pattern, key, conditions=None):
    data = {"type": "rotarycraft:blast_crafting", "category": "misc", "pattern": pattern, "key": {k: ing(v) for k, v in key.items()},
            "result": result(out, count), "temperature": temperature, "speed": speed, "xp": 0}
    put("blast_crafting/" + name, data, conditions)


# integrated gearbox upgrade: the frame in the blast furnace, the gears added by hand (RotaryRecipes GEARUPGRADE)
blast_crafting("gear_upgrade", "gear_upgrade", 1, 1200, 2, ["sSS", "SsS", "SSG"], {"S": "steelingot", "G": "minecraft:glass_pane", "s": "bedrock_shaft"})
for lvl in range(1, 5):
    shapeless("gear_upgrade_%d" % (1 << lvl), "gear_upgrade_%d" % (1 << lvl), ["gear_upgrade", "bedrock_unit%d" % (1 << lvl)])
blast_crafting("highcombustor", "highcombustor", 1, 1100, 1, ["SiS", "iRi", "SGS"], {"i": "redgoldingot", "S": "steelingot", "G": "igniter", "R": "minecraft:redstone"})
blast_crafting("bedrock_bearing", "bedrock_bearing", 1, 1000, 2, ["LLL", "LSL", "LLL"], {"L": "bedrockdust", "S": "steel_bearing"})
for d, n in zip((1, 2, 3), DIFF["PARTCRAFT"]):
    blast_crafting("diamond_gear_difficulty_%d" % d, "diamond_gear", 6 * n // 3, 1400, 1, [" W ", "WGW", " W "], {"W": "minecraft:diamond", "G": "tungsten_gear"},
                   [{"type": "rotarycraft:difficulty", "value": d}])
blast_crafting("bedrock_shaft", "bedrock_shaft", 4, 1000, 2, [" D ", "DSD", " D "], {"D": "bedrockdust", "S": "steel_shaft"})
blast_crafting("bedrock_gear", "bedrock_gear", 8, 1000, 2, ["bWb", "WWW", "bWb"], {"b": "bedrockdust", "W": "steelingot"})
blast_crafting("bedrockdrill", "bedrockdrill", 1, 1000, 8, ["BBB", "BBB", " B "], {"B": "bedingot"})
blast_crafting("strongcoil", "strongcoil", 1, 1000, 4, ["SDS", "BCB", "SDS"], {"S": "springingot", "C": "spring", "B": "bedrockdust", "D": "minecraft:diamond"})
SKIPPED.append("bedrock tools/armour, magnetostatic upgrades (Blast Furnace): items not ported yet")

# ---------------- Worktable recycling (RecyclingRecipe: parts into scrap)


def recycling(name, inputs, scrap, conditions=None):
    put("recycling/" + name, {"type": "rotarycraft:recycling", "ingredients": [ing(i) for i in inputs], "scrap": scrap}, conditions)


def recycling_diff(name, inputs, value):
    for d, p in zip((1, 2, 3), DIFF["PARTCRAFT"]):
        recycling("%s_difficulty_%d" % (name, d), inputs, value(p), [{"type": "rotarycraft:difficulty", "value": d}])


recycling_diff("steel_gear", ["steel_gear"], lambda p: 45 // p)
recycling("steel_unit2", ["steel_unit2"], 48)
recycling("steel_unit4", ["steel_unit4"], 114)
recycling("steel_unit8", ["steel_unit8"], 180)
recycling("steel_unit16", ["steel_unit16"], 244)
recycling_diff("steel_shaft", ["steel_shaft"], lambda p: 27 // p)
recycling_diff("basepanel", ["basepanel"], lambda p: 27 // p)
recycling_diff("wormgear", ["wormgear"], lambda p: 45 // p + 2 * 27 // p)
recycling("ballbearing", ["ballbearing"] * 4, 9)
recycling("mount", ["mount"], 9)
recycling("steel_shaftcore", ["steel_shaftcore"], 15)
recycling("waterplate", ["waterplate"], 81)
# ---------------- Decorative storage blocks (BlockDeco; RotaryRecipes.addCraftItems): 3x3 ingots/gems <-> block, borer shield block.
# Crafting table (GameRegistry.addRecipe / addShapelessRecipe); the steel block also went to the Worktable (RecipeLevel.PROTECTED).
for block, item in (("steel_block", "steelingot"), ("anthracite_block", "anthracite"), ("lonsdaleite_block", "lonsda"),
                    ("bedrock_ingot_block", "bedingot"), ("coke_block", "coke")):
    shaped(block, block, ["BBB", "BBB", "BBB"], {"B": item}, worktable=(block == "steel_block"))
    shapeless(item + "_from_" + block, item, [block], count=9)
shaped("shield_block", "shield_block", [" S ", "SOS", " S "], {"S": "steelingot", "O": "minecraft:obsidian"}, count=4)

os.makedirs(OUT, exist_ok=True)
for root, _, fns in os.walk(OUT):
    for fn in fns:
        if fn.endswith(".json"):
            os.remove(os.path.join(root, fn))
for name, data in recipes.items():
    os.makedirs(os.path.dirname(os.path.join(OUT, name + ".json")), exist_ok=True)
    with open(os.path.join(OUT, name + ".json"), "w") as f:
        json.dump(data, f, indent=2)
print(len(recipes), "recipes written")
for s in SKIPPED:
    print("  not yet:", s)

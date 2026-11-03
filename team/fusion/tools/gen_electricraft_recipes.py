#!/usr/bin/env python3
"""Generates the ElectriCraft crafting recipes (data/electricraft/recipe/*.json), transcribed from ElectriRecipes.addRecipes and
WireType.addCrafting.

* Like all machine recipes of the original, they went to the RotaryCraft Worktable and to the crafting table only with
  "Crafting Table can Make Machines" (RotaryCraft TABLEMACHINES): rotarycraft:config table_machines condition + worktable twin.
* Wire output is DifficultyEffects.PIPECRAFT (32/16/8): one recipe per difficulty (rotarycraft:difficulty condition).
* The wire ingots of the original are ElectriCraft's own ingot or any ore-dictionary ingot. ElectriCraft ores/ingots are not ported
  yet, so the port uses the c:ingots/<metal> tag (vanilla copper and every mod's ingots); recipes whose tag is empty are disabled.

Usage: python3 -I tools/gen_electricraft_recipes.py
"""
import json
import os

PORT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(PORT, "electricraft/src/main/resources/data/electricraft/recipe")
PIPECRAFT = (32, 16, 8)
TABLE = {"type": "rotarycraft:config", "option": "table_machines"}
WIRES = {"steel": {"item": "rotarycraft:steelingot"}, "tin": {"tag": "c:ingots/tin"}, "nickel": {"tag": "c:ingots/nickel"},
         "aluminum": {"tag": "c:ingots/aluminum"}, "copper": {"tag": "c:ingots/copper"}, "silver": {"tag": "c:ingots/silver"},
         "gold": {"item": "minecraft:gold_ingot"}, "platinum": {"tag": "c:ingots/platinum"}}

os.makedirs(OUT, exist_ok=True)
for f in os.listdir(OUT):
    os.remove(os.path.join(OUT, f))
n = 0


def put(name, pattern, key, out, count, conditions):
    global n
    for kind, typ, cond in (("", "minecraft:crafting_shaped", conditions + [TABLE]), ("_worktable", "rotarycraft:worktable_shaped", conditions)):
        data = {"type": typ, "category": "misc", "pattern": pattern, "key": key, "result": {"id": out, "count": count}}
        if cond:
            data["neoforge:conditions"] = cond
        with open(os.path.join(OUT, name + kind + ".json"), "w") as f:
            json.dump(data, f, indent=1)
        n += 1


def tagged(i):
    return [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": i["tag"]}}] if "tag" in i else []


for w, ingot in WIRES.items():
    for d, c in zip((1, 2, 3), PIPECRAFT):
        cond = tagged(ingot) + [{"type": "rotarycraft:difficulty", "value": d}]
        put("%s_wire_difficulty_%d" % (w, d), ["I", "I", "I"], {"I": ingot}, "electricraft:%s_wire" % w, c, cond)
        put("insulated_%s_wire_difficulty_%d" % (w, d), ["WIW", "WIW", "WIW"], {"I": ingot, "W": {"tag": "minecraft:wool"}},
            "electricraft:insulated_%s_wire" % w, c, cond)

put("generator", ["gts", "iGn", "ppp"], {"n": {"tag": "c:ingots/nickel"}, "t": {"tag": "c:ingots/tin"}, "p": {"item": "rotarycraft:basepanel"},
    "g": {"tag": "c:ingots/copper"}, "s": {"item": "rotarycraft:steelingot"}, "G": {"item": "rotarycraft:generator"},
    "i": {"item": "rotarycraft:impeller"}}, "electricraft:generator", 1, tagged({"tag": "c:ingots/nickel"}) + tagged({"tag": "c:ingots/tin"}))
#original key also defines 'S' (steel), which the pattern never uses
put("motor", ["scs", "gCg", "BcB"], {"g": {"item": "rotarycraft:goldcoil"}, "c": {"tag": "c:ingots/copper"}, "s": {"tag": "c:ingots/silver"},
    "B": {"item": "rotarycraft:basepanel"}, "C": {"item": "rotarycraft:steel_shaftcore"}}, "electricraft:motor", 1, tagged({"tag": "c:ingots/silver"}))
#ElectriRecipes: relay, resistor (x4), meter, precision resistor
put("relay", ["SCS", "CPC"], {"C": {"tag": "c:ingots/copper"}, "P": {"item": "rotarycraft:basepanel"}, "S": {"item": "rotarycraft:steelingot"}},
    "electricraft:relay", 4, [])
put("resistor", ["SCS", "PCP"], {"C": {"item": "rotarycraft:coaldust"}, "S": {"item": "rotarycraft:steelingot"}, "P": {"item": "rotarycraft:basepanel"}},
    "electricraft:resistor", 4, [])
put("meter", ["SsS", "wCw", "SbS"], {"S": {"item": "rotarycraft:steelingot"}, "w": {"item": "electricraft:silver_wire"}, "C": {"item": "rotarycraft:pcb"},
    "s": {"item": "rotarycraft:screen"}, "b": {"item": "rotarycraft:basepanel"}}, "electricraft:meter", 1, [])
put("precise_resistor", ["aaa", "tRt", "PPP"], {"a": {"item": "rotarycraft:silumin"}, "t": {"item": "rotarycraft:springtungsten"},
    "R": {"item": "electricraft:resistor"}, "P": {"item": "rotarycraft:basepanel"}}, "electricraft:precise_resistor", 1, [])
put("transformer", ["SSS", "I I", "SSS"], {"S": {"item": "rotarycraft:basepanel"}, "I": {"item": "rotarycraft:redgoldingot"}},
    "electricraft:transformer", 1, [])
#fuses (x8): coal dust 32 A, steel 128 A, copper 1024 A, gold 8192 A (TileEntityFuse.TIERS, item component instead of NBT)
for name, ingot, lim in (("coal", "rotarycraft:coaldust", 32), ("steel", "rotarycraft:steelingot", 128), ("copper", "minecraft:copper_ingot", 1024),
                         ("gold", "minecraft:gold_ingot", 8192)):
    for kind, typ, cond in (("", "minecraft:crafting_shaped", [TABLE]), ("_worktable", "rotarycraft:worktable_shaped", [])):
        data = {"type": typ, "category": "misc", "pattern": [" G ", "GCG", "PPP"], "key": {"G": {"item": "minecraft:glass"}, "C": {"item": ingot},
                "P": {"item": "rotarycraft:basepanel"}}, "result": {"id": "electricraft:fuse", "count": 8, "components": {"electricraft:fuse_limit": lim}}}
        if cond:
            data["neoforge:conditions"] = cond
        with open(os.path.join(OUT, "fuse_" + name + kind + ".json"), "w") as f:
            json.dump(data, f, indent=1)
        n += 1

# BatteryType.addCrafting: machine recipes
BATT = [("redstone", {"tag": "c:ingots/copper"}, "rotarycraft:basepanel"), ("glowstone", {"tag": "c:ingots/silver"}, "rotarycraft:basepanel"),
        ("lapis", {"tag": "c:ingots/silver"}, "rotarycraft:silumin"), ("ender", {"item": "rotarycraft:redgoldingot"}, "rotarycraft:basepanel"),
        ("diamond", {"item": "rotarycraft:redgoldingot"}, "rotarycraft:tungsteningot"), ("star", {"item": "rotarycraft:redgoldingot"}, "rotarycraft:bedingot")]
for b, top, bottom in BATT:
    put(b + "_battery", ["ScS", "WCW", "SPS"], {"W": {"tag": "minecraft:wool"}, "c": top, "C": {"item": "electricraft:%s_crystal" % b},
        "P": {"item": bottom}, "S": {"item": "rotarycraft:steelingot"}}, "electricraft:%s_battery" % b, 1, tagged(top))


def plain(name, data):
    global n
    with open(os.path.join(OUT, name + ".json"), "w") as f:
        json.dump(data, f, indent=1)
    n += 1


# ElectriRecipes: crystal dust (2), redstone crystal from smelting the dust, each crystal from the one before (crafting table)
plain("crystaldust", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"tag": "c:dusts/lapis"}, {"tag": "c:dusts/diamond"},
      {"tag": "c:dusts/quartz"}, {"tag": "c:dusts/glowstone"}] + [{"tag": "c:dusts/redstone"}] * 4, "result": {"id": "electricraft:crystaldust", "count": 2}})
plain("redstone_crystal_from_smelting", {"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": "electricraft:crystaldust"},
      "result": {"id": "electricraft:redstone_crystal"}, "experience": 1.0, "cookingtime": 200})
CTR = ["minecraft:glowstone", "minecraft:lapis_block", "minecraft:ender_eye", "minecraft:emerald_block", "minecraft:nether_star"]
for i in range(1, len(BATT)):
    plain(BATT[i][0] + "_crystal", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["RCR", "CIC", "RCR"],
          "key": {"R": {"item": "minecraft:redstone"}, "C": {"item": "electricraft:%s_crystal" % BATT[i - 1][0]}, "I": {"item": CTR[i - 1]}},
          "result": {"id": "electricraft:%s_crystal" % BATT[i][0], "count": 1}})
# ElectriRecipes: RotaryCraft grinder recipes for the dusts
for src, out in (("minecraft:diamond", "diamonddust"), ("minecraft:lapis_lazuli", "bluedust"), ("minecraft:quartz", "quartzdust")):
    plain("grinding_" + out, {"type": "rotarycraft:grinder", "ingredient": {"item": src}, "result": {"id": "electricraft:" + out, "count": 1}})

# ElectriRecipes.addPostLoadRecipes, RF part (in 1.21 NeoForge always has FE, the successor of RF): cable (PIPECRAFT), RF battery, RF crystal
for d, c in zip((1, 2, 3), PIPECRAFT):
    put("rf_cable_difficulty_%d" % d, ["RDR", "BGB", "RER"], {"D": {"item": "minecraft:diamond"}, "R": {"item": "minecraft:redstone_block"},
        "G": {"item": "minecraft:gold_block"}, "E": {"item": "minecraft:ender_pearl"}, "B": {"item": "rotarycraft:blast_glass"}}, "electricraft:rf_cable", c,
        [{"type": "rotarycraft:difficulty", "value": d}])
put("rf_battery", ["ScS", "WCW", "tPt"], {"t": {"item": "rotarycraft:tungsteningot"}, "W": {"tag": "minecraft:wool"}, "c": {"item": "rotarycraft:redgoldingot"},
    "C": {"item": "electricraft:rf_crystal"}, "P": {"item": "rotarycraft:bedingot"}, "S": {"item": "rotarycraft:steelingot"}}, "electricraft:rf_battery", 1, [])
plain("rf_crystal", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["RRR", "RCR", "RRR"],
      "key": {"R": {"item": "minecraft:redstone_block"}, "C": {"item": "electricraft:star_crystal"}}, "result": {"id": "electricraft:rf_crystal", "count": 1}})

# ChargerTiers.getRecipe (plain crafting table): glass/blast glass sides, core redstone, gold, diamond, red gold, enderium (or bedrock ingot)
SIDES = ["minecraft:glass"] * 3 + ["rotarycraft:blast_glass"] * 2
CORES = [{"item": "minecraft:redstone"}, {"item": "minecraft:gold_ingot"}, {"item": "minecraft:diamond"}, {"item": "rotarycraft:redgoldingot"},
         [{"tag": "c:ingots/enderium"}, {"item": "rotarycraft:bedingot"}]]
for i, t in enumerate(["basic", "improved", "advanced", "hightech", "superconducting"]):
    plain(t + "_wireless_pad", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["AEA", "SCS", "PRP"],
          "key": {"A": {"item": "rotarycraft:steelingot"}, "E": {"item": "minecraft:ender_pearl"}, "P": {"item": "rotarycraft:basepanel"},
                  "R": {"item": "rotarycraft:silicon"}, "S": {"item": SIDES[i]}, "C": CORES[i]},
          "result": {"id": "electricraft:%s_wireless_pad" % t, "count": 1}})

# ElectriRecipes: ores smelt to ingots with the ore's XP (copper: vanilla)
for m, xp in (("tin", 0.2), ("silver", 0.8), ("nickel", 0.3), ("aluminum", 0.4), ("platinum", 1.0)):
    with open(os.path.join(OUT, m + "_ingot_from_smelting.json"), "w") as f:
        json.dump({"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": "electricraft:%s_ore" % m},
                   "result": {"id": "electricraft:%s_ingot" % m}, "experience": xp, "cookingtime": 200}, f, indent=1)
    n += 1
print(n, "electricraft recipes written")

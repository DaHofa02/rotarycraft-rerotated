#!/usr/bin/env python3
"""
Static resource check of the three ports (RotaryCraft, ReactorCraft, ElectriCraft) - version 1.

Works without Gradle, Minecraft or network access, so it can run where the build cannot (e.g. a sandbox without
maven.neoforged.net). It complements the in-game checks (rotarycraft/client/TextureAudit, ResourceCompletenessTest, PickBlockTests),
which need the game; it does not replace them.

Checks, per mod:
  1. every .json / .mcmeta parses;
  2. resource paths are valid 1.21 resource locations (lower case, [a-z0-9_.-/]);
  3. blockstates -> block models exist (own namespace); multipart and variants;
  4. models -> parent models and textures exist (own namespace; '#' references resolved through the parent chain);
  5. item models: every item lang key has an item model and every item model has a name (lang key or block lang key);
  6. blockstates: every block has a name; blocks without a loot table are listed (may be intended: fluids, multiblock parts);
  7. recipes, loot tables and tags reference only existing items/blocks/tags of the three mods;
  8. sounds.json -> .ogg files exist;
  9. animated textures (.png.mcmeta) have a frame strip whose height is a multiple of the width;
 10. lang files of other languages have no keys that en_us lacks, and list how many en_us keys they miss;
 11. classic resource packs: every modern texture has its original (and vice versa).
References into minecraft:/neoforge:/c: are only counted (the vanilla jar is not available offline).

    python3 tools/check_resources_v1.py [--verbose]
Exit code 0 when no ERROR was found (WARN and INFO do not fail).
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS = ["rotarycraft", "reactorcraft", "electricraft"]
VERBOSE = "--verbose" in sys.argv
VALID = re.compile(r"^[a-z0-9_.\-/]+$")
BUILTIN_MODELS = {"builtin/entity", "builtin/generated", "item/generated", "item/handheld", "block/block", "block/cube", "block/cube_all"}

errors, warns, infos = [], [], []


def err(m):
    errors.append(m)


def warn(m):
    warns.append(m)


def info(m):
    infos.append(m)


def res(mod):
    return os.path.join(ROOT, mod, "src", "main", "resources")


def load(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def split_rl(rl, default_ns):
    if ":" in rl:
        ns, path = rl.split(":", 1)
    else:
        ns, path = default_ns, rl
    return ns, path


# ---------------------------------------------------------------------------------------------------------------- indexing
models, textures, blockstates, item_models, lang, sounds, loot, recipes, tags = {}, {}, {}, {}, {}, {}, {}, {}, {}
external_refs = {}


def ext(kind, rl):
    external_refs.setdefault(kind, set()).add(rl)


for mod in MODS:
    r = res(mod)
    a = os.path.join(r, "assets", mod)
    # 1 + 2: parse and path names
    for base in (os.path.join(r, "assets"), os.path.join(r, "data")):
        for dp, _, fs in os.walk(base):
            for f in fs:
                p = os.path.join(dp, f)
                rel = os.path.relpath(p, r).replace(os.sep, "/")
                if not VALID.match(rel):
                    err(f"{mod}: invalid resource path (1.21 needs lower case [a-z0-9_.-/]): {rel}")
                if f.endswith(".json") or f.endswith(".mcmeta"):
                    try:
                        load(p)
                    except Exception as e:  # noqa: BLE001 - report every parse problem
                        err(f"{mod}: JSON does not parse: {rel}: {e}")
    for dp, _, fs in os.walk(os.path.join(a, "models")):
        for f in fs:
            if f.endswith(".json"):
                rel = os.path.relpath(os.path.join(dp, f), os.path.join(a, "models")).replace(os.sep, "/")[:-5]
                models[(mod, rel)] = os.path.join(dp, f)
    for dp, _, fs in os.walk(os.path.join(a, "textures")):
        for f in fs:
            if f.endswith(".png"):
                rel = os.path.relpath(os.path.join(dp, f), os.path.join(a, "textures")).replace(os.sep, "/")[:-4]
                textures[(mod, rel)] = os.path.join(dp, f)
    bs = os.path.join(a, "blockstates")
    if os.path.isdir(bs):
        for f in os.listdir(bs):
            if f.endswith(".json"):
                blockstates[(mod, f[:-5])] = os.path.join(bs, f)
    for (m, rel), p in list(models.items()):
        if m == mod and rel.startswith("item/") and rel.count("/") == 1:
            item_models[(mod, rel[5:])] = p
    for lf in ("en_us.json",):
        p = os.path.join(a, "lang", lf)
        if os.path.isfile(p):
            lang[mod] = load(p)
    sp = os.path.join(a, "sounds.json")
    if os.path.isfile(sp):
        sounds[mod] = load(sp)
    d = os.path.join(r, "data")
    for ns in os.listdir(d) if os.path.isdir(d) else []:
        for kind, store in (("loot_table", loot), ("recipe", recipes)):
            kd = os.path.join(d, ns, kind)
            for dp, _, fs in os.walk(kd):
                for f in fs:
                    if f.endswith(".json"):
                        store[(ns, os.path.relpath(os.path.join(dp, f), kd).replace(os.sep, "/")[:-5])] = os.path.join(dp, f)
        td = os.path.join(d, ns, "tags")
        for dp, _, fs in os.walk(td):
            for f in fs:
                if f.endswith(".json"):
                    rel = os.path.relpath(os.path.join(dp, f), td).replace(os.sep, "/")[:-5]
                    kind, _, name = rel.partition("/")
                    tags.setdefault((kind, ns + ":" + name), []).append(os.path.join(dp, f))
        # pre-1.21 folder names are silently ignored by 1.21
        for old in ("loot_tables", "recipes", "advancements", "structures", "functions", "predicates", "item_modifiers"):
            if os.path.isdir(os.path.join(d, ns, old)):
                err(f"{mod}: data/{ns}/{old} uses the pre-1.21 plural folder name; 1.21 ignores it")
        for old in ("blocks", "items", "fluids", "entity_types"):
            if os.path.isdir(os.path.join(td, old)):
                err(f"{mod}: data/{ns}/tags/{old} uses the pre-1.21 plural folder name; 1.21 ignores it")

own_items = set()     # mod:name of every item we can see (item model, lang item/block key, blockstate)
for (mod, name) in item_models:
    own_items.add(f"{mod}:{name}")
for mod, l in lang.items():
    for k in l:
        m = re.match(r"^(item|block)\.(\w+)\.([a-z0-9_.]+)$", k)
        if m and m.group(2) == mod:
            own_items.add(f"{mod}:{m.group(3)}")
own_blocks = {f"{m}:{n}" for (m, n) in blockstates}
own_fluids = set()   # mod:name of every fluid type with a name (fluid_type.<mod>.<name>)
for mod, l in lang.items():
    for k in l:
        m = re.match(r"^fluid_type\.(\w+)\.([a-z0-9_]+)$", k)
        if m:
            own_fluids.add(f"{m.group(1)}:{m.group(2)}")
            own_fluids.add(f"{m.group(1)}:flowing_{m.group(2)}")
# translation keys that the Java code uses itself (display names of item variants, e.g. item.rotarycraft.shaft_cross)
java_keys = set()
for mod in MODS:
    for dp, _, fs in os.walk(os.path.join(ROOT, mod, "src", "main", "java")):
        for f in fs:
            if f.endswith(".java"):
                with open(os.path.join(dp, f), encoding="utf-8") as fh:
                    java_keys.update(re.findall(r'"((?:item|block)\.[a-z_]+\.[a-z0-9_.]+)"', fh.read()))
# item models that are only targets of other item models' overrides (property variants), not items of their own
override_targets = set()
for (mod, rel), p in list(models.items()):
    if rel.startswith("item/"):
        for o in load(p).get("overrides", []):
            ns, path = split_rl(o.get("model", ""), mod)
            if path.startswith("item/"):
                override_targets.add((ns, path[5:]))


# ---------------------------------------------------------------------------------------------------------------- 3 + 4: models
def model_exists(rl, default_ns):
    ns, path = split_rl(rl, default_ns)
    if ns in MODS:
        return (ns, path) in models
    ext("model", f"{ns}:{path}")
    return True


def collect_textures(key, seen=None):
    """Texture variables of a model including its parents (own namespace only)."""
    seen = seen or set()
    if key in seen or key not in models:
        return {}, None
    seen.add(key)
    j = load(models[key])
    parent_tex, root = {}, None
    if "parent" in j:
        pns, ppath = split_rl(j["parent"], key[0])
        if pns in MODS:
            parent_tex, root = collect_textures((pns, ppath), seen)
        else:
            root = f"{pns}:{ppath}"
    t = dict(parent_tex)
    t.update(j.get("textures", {}))
    return t, root


used_textures = set()
for key, p in models.items():
    mod = key[0]
    j = load(p)
    if "parent" in j and not model_exists(j["parent"], mod):
        err(f"{mod}: model {key[1]}: parent {j['parent']} does not exist")
    tex, _ = collect_textures(key)
    for var, val in j.get("textures", {}).items():
        if not isinstance(val, str):
            continue
        cur, hops = val, 0
        while cur.startswith("#") and hops < 10:
            cur = tex.get(cur[1:], cur)
            hops += 1
        if cur.startswith("#"):
            # resolved by a child model; fine for templates, wrong for leaf models used by blockstates/items
            continue
        ns, path = split_rl(cur, mod)
        if ns in MODS:
            used_textures.add((ns, path))
            if (ns, path) not in textures:
                err(f"{mod}: model {key[1]}: texture {ns}:{path} (#{var}) does not exist")
        else:
            ext("texture", f"{ns}:{path}")
    for el in j.get("elements", []):
        for face, fd in el.get("faces", {}).items():
            ref = fd.get("texture", "")
            if ref.startswith("#") and ref[1:] not in tex and ref[1:] != "missingno":
                err(f"{mod}: model {key[1]}: face {face} uses {ref}, which is not defined in the model or its parents")
    # neoforge loaders with nested models / fluid container textures
    for k2 in ("base", "fluid_mask", "cover_mask"):
        if isinstance(j.get("textures", {}).get(k2), str):
            pass


def walk_blockstate_models(j):
    if "variants" in j:
        for v in j["variants"].values():
            for e in (v if isinstance(v, list) else [v]):
                yield e.get("model")
    if "multipart" in j:
        for part in j["multipart"]:
            a = part.get("apply")
            for e in (a if isinstance(a, list) else [a]):
                if e:
                    yield e.get("model")


for (mod, name), p in blockstates.items():
    j = load(p)
    if "variants" not in j and "multipart" not in j:
        err(f"{mod}: blockstate {name} has neither variants nor multipart")
    for m in walk_blockstate_models(j):
        if m is None:
            err(f"{mod}: blockstate {name}: a variant without model")
        elif not model_exists(m, mod):
            err(f"{mod}: blockstate {name}: model {m} does not exist")
        else:
            ns, path = split_rl(m, mod)
            if ns in MODS:
                tex, root = collect_textures((ns, path))
                if not tex.get("particle") and root not in ("minecraft:block/cube_all", "minecraft:block/cube", "minecraft:block/cube_column",
                                                              "minecraft:block/orientable", "minecraft:block/cross", "minecraft:block/crop",
                                                              "minecraft:block/cube_bottom_top", "minecraft:block/leaves",
                                                              "minecraft:block/orientable_with_bottom", "minecraft:block/cube_column_horizontal"):
                    info(f"{mod}: blockstate {name}: model {m} has no particle texture (breaking particles show the missing texture "
                         f"unless the block supplies them in code)")

# ---------------------------------------------------------------------------------------------------------------- 5 + 6: names, item models, loot
for (mod, name), p in item_models.items():
    l = lang.get(mod, {})
    if f"item.{mod}.{name}" not in l and f"block.{mod}.{name}" not in l and (mod, name) not in override_targets:
        warn(f"{mod}: item model {name} has no en_us name (item.{mod}.{name} / block.{mod}.{name})")
    j = load(p)
    if "parent" not in j and "loader" not in j and "elements" not in j:
        err(f"{mod}: item model {name} has neither parent, loader nor elements")
for mod, l in lang.items():
    for k in l:
        m = re.match(rf"^item\.{mod}\.([a-z0-9_]+)$", k)
        if m and (mod, m.group(1)) not in item_models and k not in java_keys:
            err(f"{mod}: item {m.group(1)} has a name but no item model models/item/{m.group(1)}.json (missing model in 1.21)")
for (mod, name) in blockstates:
    l = lang.get(mod, {})
    if f"block.{mod}.{name}" not in l:
        warn(f"{mod}: block {name} has no en_us name block.{mod}.{name}")
    if (mod, "blocks/" + name) not in loot:
        info(f"{mod}: block {name} has no loot table (drops nothing when broken unless the code drops it)")
    if (mod, name) not in item_models:
        info(f"{mod}: block {name} has no item model (fine if it has no item)")


# ---------------------------------------------------------------------------------------------------------------- 7: ids in data files
def check_id(rl, where, kind="item"):
    ns, path = split_rl(rl, "minecraft")
    if ns in MODS:
        pool = own_items if kind == "item" else own_blocks
        if f"{ns}:{path}" not in pool and f"{ns}:{path}" not in own_items:
            err(f"{where}: unknown {kind} {ns}:{path}")
    else:
        ext(kind, f"{ns}:{path}")


def check_tag(rl, where, kind="item"):
    ns, path = split_rl(rl, "minecraft")
    if ns in MODS or ns == "c":
        if (kind, f"{ns}:{path}") not in tags:
            (err if ns in MODS else info)(f"{where}: tag #{ns}:{path} ({kind}) is not defined by the ports" +
                                         ("" if ns in MODS else " (may come from NeoForge's c: tags)"))
    else:
        ext("tag/" + kind, f"{ns}:{path}")


def scan(obj, where):
    """Item/tag references anywhere in a recipe or loot table."""
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ("fluid", "fluid_output", "fluid_input") and isinstance(v, dict) and isinstance(v.get("id", v.get("fluid")), str):
                fid = v.get("id", v.get("fluid"))
                if fid.split(":")[0] in MODS and fid not in own_fluids:
                    err(f"{where}: unknown fluid {fid}")
                elif fid.split(":")[0] not in MODS:
                    ext("fluid", fid)
                continue
            if k in ("item", "id") and isinstance(v, str) and ":" in v and not v.startswith("#"):
                if obj.get("type") in ("minecraft:item", None) or k == "id" or k == "item":
                    if k == "id" and v.split(":")[0] not in MODS and v.split(":")[0] != "minecraft":
                        ext("item", v)
                    elif not (k == "item" and obj.get("type") and obj.get("type") != "minecraft:item"):
                        check_id(v, where)
            elif k == "tag" and isinstance(v, str):
                check_tag(v, where)
            elif k == "name" and isinstance(v, str) and obj.get("type") == "minecraft:item":
                check_id(v, where)
            else:
                scan(v, where)
    elif isinstance(obj, list):
        for v in obj:
            scan(v, where)


for (ns, name), p in recipes.items():
    j = load(p)
    scan({k: v for k, v in j.items() if k not in ("type", "neoforge:conditions", "conditions")}, f"recipe {ns}:{name}")
for (ns, name), p in loot.items():
    scan(load(p), f"loot_table {ns}:{name}")
for (kind, rl), ps in tags.items():
    for p in ps:
        j = load(p)
        for v in j.get("values", []):
            req = True
            if isinstance(v, dict):
                req = v.get("required", True)
                v = v.get("id")
            if not isinstance(v, str):
                continue
            if v.startswith("#"):
                if req:
                    check_tag(v[1:], f"tag #{rl}", "item" if kind == "item" else "block")
            elif kind in ("item", "block") and req:
                check_id(v, f"tag #{rl}", kind)

# data maps (neoforge:furnace_fuels, rotarycraft:mulch_value ...): keys must be existing items or tags
for mod in MODS:
    d = os.path.join(res(mod), "data")
    for dp, _, fs in os.walk(d):
        if "/data_maps/" not in dp.replace(os.sep, "/") + "/":
            continue
        for f in fs:
            if f.endswith(".json"):
                where = "data map " + os.path.relpath(os.path.join(dp, f), d).replace(os.sep, "/")
                for k in load(os.path.join(dp, f)).get("values", {}):
                    (check_tag(k[1:], where) if k.startswith("#") else check_id(k, where))

# ---------------------------------------------------------------------------------------------------------------- 8: sounds
for mod, sj in sounds.items():
    for ev, d in sj.items():
        for s in d.get("sounds", []):
            nm = s if isinstance(s, str) else s.get("name")
            if isinstance(s, dict) and s.get("type") == "event":
                continue
            ns, path = split_rl(nm, mod)
            if ns in MODS:
                f = os.path.join(res(ns), "assets", ns, "sounds", path + ".ogg")
                if not os.path.isfile(f):
                    err(f"{mod}: sound event {ev}: file sounds/{path}.ogg does not exist")
            else:
                ext("sound", f"{ns}:{path}")

# ---------------------------------------------------------------------------------------------------------------- 9: animations
try:
    from PIL import Image
except ImportError:  # the check still runs without Pillow
    Image = None
for (mod, rel), p in textures.items():
    meta = p + ".mcmeta"
    if Image is None:
        break
    with Image.open(p) as im:
        w, h = im.size
    if os.path.isfile(meta):
        j = load(meta)
        if "animation" in j and (h % w != 0 or h == w) and not j["animation"].get("width") and not j["animation"].get("height"):
            err(f"{mod}: animated texture {rel}: {w}x{h} is not a vertical strip of square frames")
    elif h > w and h % w == 0 and rel.startswith(("block/", "item/")) and (mod, rel) in used_textures:
        warn(f"{mod}: texture {rel} is a {w}x{h} strip used by a model but has no .mcmeta (shows squashed)")
    if w > 512 or h > 4096:
        warn(f"{mod}: texture {rel} is {w}x{h} (unusually large)")

# ---------------------------------------------------------------------------------------------------------------- 10: other languages
for mod in MODS:
    ld = os.path.join(res(mod), "assets", mod, "lang")
    if not os.path.isdir(ld) or mod not in lang:
        continue
    for f in sorted(os.listdir(ld)):
        if f == "en_us.json" or not f.endswith(".json"):
            continue
        other = load(os.path.join(ld, f))
        extra = [k for k in other if k not in lang[mod]]
        if extra:
            warn(f"{mod}: lang/{f} has {len(extra)} keys en_us lacks, e.g. {extra[:3]}")
        miss = len([k for k in lang[mod] if k not in other])
        if miss:
            info(f"{mod}: lang/{f} lacks {miss} of {len(lang[mod])} en_us keys (falls back to English)")

# ---------------------------------------------------------------------------------------------------------------- 11: classic packs
for mod in MODS:
    live = os.path.join(res(mod), "assets", mod, "textures")
    classic = os.path.join(res(mod), "resourcepacks", "classic", "assets", mod, "textures")
    if not os.path.isdir(classic):
        warn(f"{mod}: no classic texture pack")
        continue
    lset, cset = set(), set()
    for base, s in ((live, lset), (classic, cset)):
        for dp, _, fs in os.walk(base):
            if "/ctm" in dp.replace(os.sep, "/"):
                continue
            for f in fs:
                if f.endswith(".png"):
                    s.add(os.path.relpath(os.path.join(dp, f), base).replace(os.sep, "/"))
    for x in sorted(lset - cset):
        warn(f"{mod}: texture {x} has no original in the classic pack")
    for x in sorted(cset - lset):
        warn(f"{mod}: classic texture {x} has no modern counterpart")

# ---------------------------------------------------------------------------------------------------------------- 12: textures named in Java
# Renderers and screens build texture paths in code (tex("x.png") helpers for textures/machine/, literal "textures/..." paths). A literal
# that names a missing file is an ERROR (the game shows the missing texture); a literal prefix of a concatenated name ("generator"+mode)
# must match at least one file; names built only from variables are counted as not checkable.
java_tex_checked = java_tex_unchecked = 0
HELPERS = {"MachineRenderer.tex": "rotarycraft", "ReactorClient.tex": "reactorcraft", "FusionRenderers.tex": "reactorcraft",
           "ElectriRenderers.tex": "electricraft"}
for mod in MODS:
    for dp, _, fs in os.walk(os.path.join(ROOT, mod, "src", "main", "java")):
        for f in fs:
            if not f.endswith(".java"):
                continue
            src = open(os.path.join(dp, f), encoding="utf-8").read()
            where = os.path.relpath(os.path.join(dp, f), ROOT)
            refs = []  # (namespace, path or prefix, is_prefix)
            for m in re.finditer(r'(?<![\w.])((?:MachineRenderer|ReactorClient|FusionRenderers|ElectriRenderers)\.)?tex\(\s*(?:"([^"]*)"\s*(\+)?|([^)]*))\)?', src):
                if "static ResourceLocation tex(" in src[max(0, m.start() - 40):m.end()]:
                    continue  # the helper's own declaration
                helper = (m.group(1) or "")
                ns = HELPERS.get(helper + "tex", mod) if helper else mod
                if m.group(2) is None:
                    java_tex_unchecked += 1
                    continue
                refs.append((ns, "machine/" + m.group(2), bool(m.group(3))))
            for m in re.finditer(r'fromNamespaceAndPath\(\s*([A-Za-z_.]+|"[a-z_]+")\s*,\s*"textures/([^"]+)"\s*(\+)?', src):
                owner = m.group(1).strip('"')  # "rotarycraft", RotaryCraft.MOD_ID, reika.reactorcraft.ReactorCraft.MOD_ID or MOD_ID
                parts = owner.split(".")
                if owner in MODS:
                    ns = owner
                elif len(parts) >= 2 and parts[-1] == "MOD_ID" and parts[-2].lower() in MODS:
                    ns = parts[-2].lower()
                else:
                    ns = mod  # a bare MOD_ID: the file's own mod
                refs.append((ns, m.group(2), bool(m.group(3))))
            for ns, path, prefix in refs:
                base = os.path.join(res(ns), "assets", ns, "textures")
                if prefix or path.endswith("/"):
                    d, _, start = path.rpartition("/")
                    folder = os.path.join(base, d)
                    if not (os.path.isdir(folder) and any(x.startswith(start) for x in os.listdir(folder))):
                        err(f"{where}: no texture matches {ns}:textures/{path}*")
                elif not path.endswith(".png"):
                    java_tex_unchecked += 1
                    continue
                elif not os.path.isfile(os.path.join(base, path)):
                    err(f"{where}: texture {ns}:textures/{path} does not exist")
                java_tex_checked += 1
info(f"textures named in Java: {java_tex_checked} checked, {java_tex_unchecked} built from variables only (not checkable)")

# ---------------------------------------------------------------------------------------------------------------- report
print(f"Indexed: {len(blockstates)} blockstates, {len(models)} models ({len(item_models)} item models), {len(textures)} textures, "
      f"{len(recipes)} recipes, {len(loot)} loot tables, {sum(len(v) for v in tags.values())} tag files, {len(own_items)} item ids")
for kind in sorted(external_refs):
    print(f"External {kind} references (not checkable offline): {len(external_refs[kind])}")
for label, lst in (("ERROR", errors), ("WARN", warns), ("INFO", infos)):
    print(f"{label}: {len(lst)}")
    for m in (lst if (VERBOSE or label != "INFO") else lst[:15]):
        print(f"  {label} {m}")
    if label == "INFO" and not VERBOSE and len(lst) > 15:
        print(f"  ... {len(lst) - 15} more (--verbose)")
sys.exit(1 if errors else 0)

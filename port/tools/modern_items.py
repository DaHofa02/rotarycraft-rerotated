"""
Material items of the private ports drawn in GregTech CEu Modern's style (used by modernize_textures.py; not part of the original mods).

GregTech draws every material item from a grey template per item kind (ingot, dust, gear, rod ...) and icon set (dull, metallic, shiny,
rough, diamond ...), multiplied with the material's colour, with an untinted overlay on top. This does the same for the ports' items:
the template comes from tools/gt_templates (GregTech's own, LGPL), the colour from the original item (its typical colour), so a steel
gear stays steel-coloured and a gold coil gold. Items without a matching kind (tools, engine parts, magnets ...) return None and keep
the general filter.
"""
import os
import re

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SETS = os.path.join(HERE, "gt_templates", "material_sets")

# (pattern on the item name, GregTech kind or "units"/"cell", icon set or None for the material's set)
RULES = [
    (r"_unit(2|4|8|16)$", "units", None),
    (r"oreslurry$|slurry$", "crushed_purified", "dull"),
    (r"oreflakes$|flakes$", "crushed_refined", "dull"),
    (r"oredust$", "crushed", "dull"),
    (r"solution$", "dust_pure", "dull"),
    (r"_canister$", "cell", None),
    (r"(_|^)gear$|wormgear$", "gear", None),
    (r"^hub$", "gear_small", None),
    (r"_shaftcore$", "rod", None),
    (r"_shaft$|^rod$", "rod_long", None),
    (r"coil$|^spring$", "spring", None),
    (r"^saw$", "tool_head_buzz_saw", None),
    (r"drill$", "tool_head_drill", None),
    (r"^magnetite$", "crushed", "metallic"),
    (r"pellet$|^fuel$", "round", None),
    (r"ingot$|^carbide$|^alloy$|^silumin$|^springtungsten$", "ingot", None),
    (r"fluorite$|^lonsda$|^prismane$", "gem", None),
    (r"^coke$|^anthracite$|^graphite$", "gem", "lignite"),
    (r"^lens$", "lens", "glass"),
    (r"^wire$", "wire_fine", None),
    (r"^basepanel$|^mirror$", "plate", None),
    (r"^shield$", "plate_dense", "metallic"),
    (r"^impeller$", "rotor", None),
    (r"^turbine$|^compoundturb$", "turbine_blade", None),
    (r"^dryice$", "gem_chipped", "glass"),
    (r"dust$|powder$|^flour$|^salt$|^nitrate$|^lime$|^calcite$|^ammonium$|^hf$|^yeast$|^mulch$|^compost$|^tar$|^silveriodide$|^sawdust$",
     "dust", None),
]


def icon_set(name):
    """GregTech icon set for the item's material, from its name."""
    if re.search(r"gold|silver|platinum|redgold", name):
        return "shiny"
    if re.search(r"steel|iron|tungsten|aluminum|nickel|tin_|^tin|cadmium|indium|ferro|spring|uranium|plutonium|bedingot|alloy|carbide", name):
        return "metallic"
    if re.search(r"^wood|^stone|^bedrock|compost|mulch|tar", name):
        return "rough"
    if re.search(r"diamond|lonsda|prismane|emerald", name):
        return "diamond"
    if re.search(r"quartz|fluorite|crystal", name):
        return "quartz"
    if re.search(r"flour|salt|sawdust|yeast|lime|calcite|nitrate", name):
        return "fine"
    return "dull"


def template(kind, iset):
    """GregTech's template and overlay for the kind in that set, falling back as GregTech does (to metallic, then dull)."""
    for s in (iset, "metallic" if iset == "shiny" else None, "dull"):
        if s is None:
            continue
        p = os.path.join(SETS, s, kind + ".png")
        if os.path.isfile(p):
            base = Image.open(p).convert("RGBA")
            o = os.path.join(SETS, s, kind + "_overlay.png")
            over = Image.open(o).convert("RGBA") if os.path.isfile(o) else None
            return base.crop((0, 0, 16, 16)), (over.crop((0, 0, 16, 16)) if over else None)
    return None, None


# Materials whose items share one colour, taken from a reference item of the material (so all steel parts match, and items whose original
# icon is only the placeholder - e.g. wood_shaft - get their material's colour).
MATERIALS = {"wood": "wood_gear", "stone": "stone_gear", "steel": "steelingot", "diamond": "diamond_gear", "bedrock": "bedrock_gear"}
# Items whose original texture is only the placeholder (four violet corner marks): their colour. Neutron shielding: a grey lead-like plate.
PLACEHOLDER_COLOURS = {"shield": (112, 118, 128)}


def placeholder(orig):
    a = np.asarray(orig.convert("RGBA"))
    return (a[..., 3] > 0).sum() < 24


def material_colour(name, orig, ref_dir):
    if name in PLACEHOLDER_COLOURS:
        return np.array(PLACEHOLDER_COLOURS[name], dtype=float)
    for mat, ref in MATERIALS.items():
        if name.startswith(mat + "_") and ref_dir:
            p = os.path.join(ref_dir, ref + ".png")
            if os.path.isfile(p):
                return colour(Image.open(p))
    return colour(orig)


def colour(orig):
    """The original item's typical colour: the median of its mid-bright opaque pixels, a little brightened (GregTech's templates are
    light greys that the colour darkens)."""
    a = np.asarray(orig.convert("RGBA")).astype(float)
    px = a[a[..., 3] > 128][:, :3]
    if len(px) == 0:
        return np.array([200.0, 200.0, 200.0])
    l = px @ np.array([0.2126, 0.7152, 0.0722])
    lo, hi = np.percentile(l, 25), np.percentile(l, 85)
    mid = px[(l >= lo) & (l <= hi)]
    c = np.median(mid if len(mid) else px, axis=0)
    m = max(c.max(), 1)
    c = c * min(1.35, 235 / m)
    l = c @ np.array([0.2126, 0.7152, 0.0722])
    if l < 70:  # very dark materials (coal, anthracite) stay readable, as GregTech's coal colour
        c = c + (70 - l)
    return np.clip(c, 0, 255)


def tint(base, col):
    a = np.asarray(base).astype(float)
    a[..., :3] = a[..., :3] * col / 255.0
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def draw(kind, iset, col):
    base, over = template(kind, iset)
    if base is None:
        return None
    img = tint(base, col)
    if over is not None:
        img = Image.alpha_composite(img, over)
    return img


def units(n, iset, col):
    """Gear units: 2 - one small gear, 4 - one gear, 8 - a gear and a small one, 16 - two gears (the originals show growing gear sets)."""
    gear = draw("gear", iset, col)
    small = draw("gear_small", iset, col)
    if gear is None:
        return None
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    if n == 2:
        return small
    if n == 4:
        return gear
    if n == 8:
        g = gear.resize((13, 13), Image.NEAREST)
        out.alpha_composite(g, (0, 0))
        out.alpha_composite(small.resize((10, 10), Image.NEAREST), (6, 6))
        return out
    g = gear.resize((11, 11), Image.NEAREST)
    out.alpha_composite(g, (0, 0))
    out.alpha_composite(g, (5, 5))
    return out


def cell(orig, name):
    """Canisters as GregTech's fluid cell: the cell, its window filled with the canister's contents colour (empty: no fluid)."""
    d = os.path.join(HERE, "gt_templates", "fluid_cell")
    base = Image.open(os.path.join(d, "base.png")).convert("RGBA").crop((0, 0, 16, 16))
    over = Image.open(os.path.join(d, "overlay.png")).convert("RGBA").crop((0, 0, 16, 16))
    if name.startswith("empty"):
        return base
    a = np.asarray(orig.convert("RGBA")).astype(float)
    centre = a[4:12, 5:11]
    px = centre[centre[..., 3] > 128][:, :3]
    col = np.median(px, axis=0) if len(px) else np.array([120.0, 160.0, 220.0])
    fl = np.asarray(over).astype(float)
    fl[..., :3] = fl[..., :3] * col / 255.0
    return Image.alpha_composite(base, Image.fromarray(np.clip(fl, 0, 255).astype(np.uint8)))


def render(name, orig, ref_dir=None):
    """The item in GregTech's style, or None if it is not a material item of a known kind. ref_dir: the folder of the original item
    textures (for the material colours)."""
    for pat, kind, iset in RULES:
        m = re.search(pat, name)
        if not m:
            continue
        s = iset or icon_set(name)
        col = material_colour(name, orig, ref_dir)
        if kind == "units":
            return units(int(m.group(1)), s, col)
        if kind == "cell":
            return cell(orig, name)
        return draw(kind, s, col)
    return None

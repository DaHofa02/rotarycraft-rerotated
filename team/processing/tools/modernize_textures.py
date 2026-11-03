#!/usr/bin/env python3
"""
Restyles the textures of the private ports in a modern (Minecraft 1.14+ / GregTech CEu Modern) look. Not part of the original mods.

The original textures are kept unchanged in each mod's built-in resource pack "classic"
(<mod>/src/main/resources/resourcepacks/classic/assets/<mod>/textures); this script reads them from there and writes the restyled ones
to <mod>/src/main/resources/assets/<mod>/textures. It is deterministic: running it again gives the same files.

What it does (no texture is drawn anew; every step is a filter on the original):
  * tone: one global curve, measured once against GregTech CEu Modern's casings (luminance percentiles), lifts the crushed blacks and
    tames pure white - modern textures avoid both;
  * calm: edge-preserving smoothing of 1-2 step pixel noise (1.7.10 textures are noisier than modern ones), then each 16x16 texture is
    reduced to a few clean tones per hue (like modern textures);
  * frame: block tiles that already have a frame (machine casings, panels) get a crisp dark outline and a bevel - light top/left,
    dark bottom/right - as GregTech's casings; for each such tile 15 more variants go to <dir>/ctm/<name>/<mask>.png, with the frame
    opened on the sides in the mask (1 top, 2 right, 4 bottom, 8 left) - the connected textures that
    rotarycraft/client/ConnectedTextures picks when the same block stands next to it;
  * material items (ingots, dusts, ore stages, gears, rods, springs, cells ...): drawn from GregTech CEu Modern's own grey templates
    (tools/gt_templates, LGPL) tinted with the original item's colour, see modern_items.py;
  * other items: a little more colour, light from the top left (lifted inner edge top/left, shaded bottom/right) and a dark outline in their
    own colour where the silhouette has none.
Model atlases (machine/) get the tone curve, the noise calming and - where the box model they belong to is found surely
(model_faces.py) - a defined edge on every model face; animated strips only tone and calming.
GUI textures stay as they are (vanilla 1.21 still uses the same GUI look).

    python3 tools/modernize_textures.py [--init] [--only PATH_SUBSTRING]
      --init  first copy the current textures into the classic packs (once, before the first run)

tools/modernized_textures.sha1 lists every texture this script wrote. A texture in assets/ that is not listed there (or differs) was
put there by something else - e.g. the gen_*_assets.py tools, which copy original textures - and is taken over into the classic pack as
the new original before restyling, so running the generators and then this script is always safe.
"""
import hashlib
import os
import shutil
import sys

import numpy as np
from PIL import Image

import model_faces
import modern_items

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS = ["rotarycraft", "reactorcraft", "electricraft"]

# Luminance curve: points (from, to). Measured: the ports' block textures have 25% of their pixels below 9 (GregTech casings: 1% below
# 17, 5% below 34) and their brightest 5% at 245-255 (GregTech: 229-255). Lift the floor, keep the order, ease the top.
CURVE = [(0, 28), (16, 38), (48, 60), (96, 96), (160, 148), (220, 190), (255, 218)]


# Items: their tones already lie close to GregTech's (5%/50%/95%: 25/96/219 against 21/97/229) - only the hard white is eased and the
# deepest black slightly lifted, so their small dark details (holes, gaps) keep their contrast.
ITEM_CURVE = [(0, 12), (32, 36), (128, 128), (220, 206), (255, 228)]


def curve(l, points=CURVE):
    xs = np.array([p[0] for p in points], dtype=float)
    ys = np.array([p[1] for p in points], dtype=float)
    return np.interp(l, xs, ys)


def lum(rgb):
    return 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]


def tone(rgb, points=CURVE):
    """Applies the curve to the luminance and keeps hue and (relative) saturation; dark pixels are lifted by adding, not scaling."""
    l = lum(rgb)
    t = curve(l, points)
    scale = np.where(l > 24, t / np.maximum(l, 1), 1.0)[..., None]
    add = np.where(l > 24, 0.0, t - l)[..., None]
    out = rgb * scale + add
    # a touch less saturation in the very bright and very dark ends, as modern textures
    g = lum(out)[..., None]
    k = np.clip(0.92 + 0.08 * np.sin(np.clip(g / 255.0, 0, 1) * np.pi), 0, 1)
    out = g + (out - g) * k
    return np.clip(out, 0, 255)


def calm(rgb, mask):
    """Pulls each pixel halfway to its 3x3 median when it differs only a little (noise), keeping real edges."""
    h, w = mask.shape
    l = lum(rgb)
    out = rgb.copy()
    for y in range(h):
        for x in range(w):
            if not mask[y, x]:
                continue
            ys, xs = slice(max(0, y - 1), min(h, y + 2)), slice(max(0, x - 1), min(w, x + 2))
            m = mask[ys, xs]
            if m.sum() < 4:
                continue
            nb = rgb[ys, xs][m]
            med = np.median(nb, axis=0)
            d = abs(l[y, x] - lum(med))
            if d < 14:
                out[y, x] = rgb[y, x] * 0.45 + med * 0.55
    return out


def posterize(rgb, mask, colors):
    """Reduces the opaque pixels to a few tones (median cut), as modern textures use. A pixel the reduction would change a lot (small
    details with few pixels - holes, gaps, lamps) keeps its colour."""
    px = rgb[mask].astype(np.uint8)
    if len(px) < 2:
        return rgb
    n = min(colors, len(np.unique(px.reshape(-1, 3), axis=0)))
    if n < 2:
        return rgb
    strip = Image.fromarray(px.reshape(1, -1, 3))
    q = strip.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGB")
    qp = np.asarray(q).reshape(-1, 3).astype(float)
    orig = rgb[mask]
    far = (np.abs(lum(qp) - lum(orig)) > 18) | (np.abs(qp - orig).max(axis=1) > 40)
    qp[far] = orig[far]
    out = rgb.copy()
    out[mask] = qp
    return out


NATURAL = ("ore", "stone", "gravel", "sand", "dirt", "log", "leaves", "plank", "canola", "fluid", "flower", "bedrock", "grass", "gem")


def framed(rgb, mask, rel=""):
    """True for opaque tiles whose border ring is calm (a frame or panel edge), not for natural textures (stone, ores, plants)."""
    name = rel.rsplit("/", 1)[-1]
    if any(n in name for n in NATURAL):
        return False
    if not mask.all():
        return False
    l = lum(rgb)
    ring = np.concatenate([l[0, :], l[-1, :], l[:, 0], l[:, -1]])
    inner = l[2:-2, 2:-2]
    return ring.std() < 14 and abs(ring.mean() - inner.mean()) > 6


TOP, RIGHT, BOTTOM, LEFT = 1, 2, 4, 8
BAND = 2


def frame(rgb, connected=0):
    """Dark outline ring, highlight on the top/left inner edge, shade on the bottom/right one (GregTech casing look) - on the sides not
    connected to a neighbour of the same block (connected: bit mask TOP|RIGHT|BOTTOM|LEFT)."""
    out = rgb.copy()
    t, r, b, l = (not connected & TOP), (not connected & RIGHT), (not connected & BOTTOM), (not connected & LEFT)
    y0, y1 = (1 if t else 0), (15 if b else 16)
    x0, x1 = (1 if l else 0), (15 if r else 16)
    if t:
        out[1, x0:x1] = np.minimum(255, out[1, x0:x1] * 1.15 + 14)
    if l:
        out[y0:y1, 1] = np.minimum(255, out[y0:y1, 1] * 1.15 + 14)
    if b:
        out[-2, x0:x1] *= 0.74
    if r:
        out[y0:y1, -2] *= 0.74
    if t:
        out[0, :] = rgb[0, :] * 0.42
    if b:
        out[-1, :] = rgb[-1, :] * 0.42
    if l:
        out[:, 0] = rgb[:, 0] * 0.42
    if r:
        out[:, -1] = rgb[:, -1] * 0.42
    return out


def open_edges(rgb, connected):
    """Removes the original frame band on the connected sides by continuing the inside of the tile outwards, so neighbours join."""
    out = rgb.copy()
    if connected & TOP:
        out[:BAND, :] = out[BAND, :]
    if connected & BOTTOM:
        out[-BAND:, :] = out[-BAND - 1, :]
    if connected & LEFT:
        out[:, :BAND] = out[:, BAND:BAND + 1]
    if connected & RIGHT:
        out[:, -BAND:] = out[:, -BAND - 1:-BAND]
    return out


def face_edges(rgb, mask, faces):
    """Modern model look (vanilla 1.14+, Create): every box face of the model gets a defined edge - a darker rim, a light inner line on its
    top/left and a shaded one on its bottom/right - so the machine's parts read as solid bodies with crisp edges."""
    out = rgb.copy()
    h, w = mask.shape
    done = np.zeros(mask.shape, dtype=bool)
    for (x, y, fw, fh) in faces:
        x0, y0, x1, y1 = int(round(x)), int(round(y)), int(round(x + fw)), int(round(y + fh))
        x0, y0, x1, y1 = max(0, x0), max(0, y0), min(w, x1), min(h, y1)
        if x1 - x0 < 4 or y1 - y0 < 4:
            continue
        ring = np.zeros(mask.shape, dtype=bool)
        ring[y0, x0:x1] = ring[y1 - 1, x0:x1] = True
        ring[y0:y1, x0] = ring[y0:y1, x1 - 1] = True
        ring &= mask & ~done
        out[ring] = rgb[ring] * 0.78
        hi = np.zeros(mask.shape, dtype=bool)
        hi[y0 + 1, x0 + 1:x1 - 1] = hi[y0 + 1:y1 - 1, x0 + 1] = True
        hi &= mask & ~ring & ~done
        out[hi] = np.minimum(255, rgb[hi] * 1.07 + 4)
        lo = np.zeros(mask.shape, dtype=bool)
        lo[y1 - 2, x0 + 1:x1 - 1] = lo[y0 + 1:y1 - 1, x1 - 2] = True
        lo &= mask & ~ring & ~hi & ~done
        out[lo] = rgb[lo] * 0.9
        done |= ring | hi | lo
    return out


def outline(rgb, mask):
    """Darkens silhouette pixels next to transparency that are not dark already (items get a defined edge, a darker shade of their own
    colour rather than black, as modern sprites)."""
    out = rgb.copy()
    h, w = mask.shape
    l = lum(rgb)
    for y in range(h):
        for x in range(w):
            if not mask[y, x]:
                continue
            opaque = 0
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < h and 0 <= xx < w and mask[yy, xx]:
                    opaque += 1
            # an edge pixel of a body (3 opaque neighbours), not a thin spike or tooth (1-2), which would vanish into the outline
            if opaque == 3 and l[y, x] > 55:
                out[y, x] = rgb[y, x] * 0.6
    return out


def saturate(rgb, mask, k):
    g = lum(rgb)[..., None]
    out = np.where(mask[..., None], g + (rgb - g) * k, rgb)
    return np.clip(out, 0, 255)


def light(rgb, mask):
    """Light from the top left, as modern item sprites: the inner pixels next to the outline on the top/left are lifted, those on the
    bottom/right shaded."""
    out = rgb.copy()
    h, w = mask.shape

    def inside(y, x):
        return 0 <= y < h and 0 <= x < w and mask[y, x]

    edge = np.zeros_like(mask)
    for y in range(h):
        for x in range(w):
            if mask[y, x] and not all(inside(y + dy, x + dx) for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1))):
                edge[y, x] = True
    for y in range(h):
        for x in range(w):
            if not mask[y, x] or edge[y, x]:
                continue
            up_left = (inside(y - 1, x) and edge[y - 1, x]) or (inside(y, x - 1) and edge[y, x - 1])
            down_right = (inside(y + 1, x) and edge[y + 1, x]) or (inside(y, x + 1) and edge[y, x + 1])
            if up_left and not down_right:
                out[y, x] = np.minimum(255, rgb[y, x] * 1.16 + 8)
            elif down_right and not up_left:
                out[y, x] = rgb[y, x] * 0.84
    return out


def kind(rel):
    parts = rel.split("/")
    if parts[0] == "gui":
        return "gui"
    if parts[0] in ("item",):
        return "item"
    if parts[0] in ("block",):
        return "block"
    return "model"


def process(src, dst, rel):
    im = Image.open(src)
    has_alpha = im.mode in ("RGBA", "LA", "P") and "transparency" in im.info or im.mode in ("RGBA", "LA")
    im = im.convert("RGBA")
    a = np.asarray(im).astype(float)
    rgb, alpha = a[..., :3], a[..., 3]
    mask = alpha > 0
    k = kind(rel)
    w, h = im.size
    if k == "gui":
        shutil.copyfile(src, dst)
        return
    if k == "item" and (w, h) == (16, 16) and rel.count("/") == 1:
        gt = modern_items.render(os.path.basename(rel)[:-4], im, os.path.dirname(src))
        if gt is not None:
            gt.save(dst)
            return
    tiles = []
    if w == 16 and h % 16 == 0 and h > 16:  # animation strip: each frame on its own
        tiles = [(0, y) for y in range(0, h, 16)]
    elif w == 16 and h == 16:
        tiles = [(0, 0)]
    if tiles:
        for (x0, y0) in tiles:
            sl = (slice(y0, y0 + 16), slice(x0, x0 + 16))
            t, m = rgb[sl], mask[sl]
            t = tone(t, ITEM_CURVE if k == "item" else CURVE)
            t = calm(t, m)
            t = posterize(t, m, 10 if k == "item" else 8)
            if k == "block" and h == 16 and framed(rgb[sl], m, rel):
                variants = os.path.join(os.path.dirname(dst), "ctm", os.path.basename(dst)[:-4])
                os.makedirs(variants, exist_ok=True)
                for mask in range(1, 16):
                    v = frame(open_edges(t, mask), mask)
                    Image.fromarray(np.dstack([np.clip(v, 0, 255), alpha[sl]]).astype(np.uint8)).convert("RGB").save(
                        os.path.join(variants, str(mask) + ".png"))
                t = frame(t)
            if k == "item":
                t = saturate(t, m, 1.12)
                t = light(t, m)
                t = outline(t, m)
            rgb[sl] = t
    else:  # model atlas or large texture: tone and calm, and defined edges on every face of the model it belongs to
        rgb = tone(rgb)
        rgb = calm(rgb, mask)
        if k == "model":
            found = model_faces.best(mask, rel)
            if found:
                rgb = face_edges(rgb, mask, found[1])
    out = np.dstack([np.clip(rgb, 0, 255), alpha]).astype(np.uint8)
    img = Image.fromarray(out)
    if not has_alpha and (alpha == 255).all():
        img = img.convert("RGB")
    img.save(dst)


MANIFEST = os.path.join(ROOT, "tools", "modernized_textures.sha1")


def sha1(path):
    with open(path, "rb") as f:
        return hashlib.sha1(f.read()).hexdigest()


def main():
    manifest = {}
    if os.path.isfile(MANIFEST):
        with open(MANIFEST) as f:
            for line in f:
                h, _, rel = line.rstrip("\n").partition(" ")
                manifest[rel] = h
    only = None
    if "--only" in sys.argv:
        only = sys.argv[sys.argv.index("--only") + 1]
    for mod in MODS:
        res = os.path.join(ROOT, mod, "src", "main", "resources")
        live = os.path.join(res, "assets", mod, "textures")
        classic = os.path.join(res, "resourcepacks", "classic", "assets", mod, "textures")
        if "--init" in sys.argv and not os.path.isdir(classic):
            shutil.copytree(live, classic)
        if not os.path.isdir(classic):
            sys.exit("no classic textures for " + mod + " (run with --init once)")
        for dirpath, dirs, _ in os.walk(live):  # connected-texture variants are output only: made anew below
            if "ctm" in dirs and not only:
                shutil.rmtree(os.path.join(dirpath, "ctm"))
        adopted = 0
        for dirpath, _, files in os.walk(live):
            if "/ctm" in dirpath.replace(os.sep, "/"):
                continue
            for f in files:
                if f.endswith(".png"):
                    p = os.path.join(dirpath, f)
                    rel = mod + "/" + os.path.relpath(p, live).replace(os.sep, "/")
                    if manifest.get(rel) != sha1(p) and "--init" not in sys.argv:
                        dst = os.path.join(classic, os.path.relpath(p, live))
                        os.makedirs(os.path.dirname(dst), exist_ok=True)
                        shutil.copyfile(p, dst)
                        adopted += 1
        if adopted:
            print(mod, adopted, "new original textures taken over into the classic pack")
        n = 0
        for dirpath, _, files in os.walk(classic):
            for f in files:
                if not f.endswith(".png"):
                    continue
                src = os.path.join(dirpath, f)
                rel = os.path.relpath(src, classic).replace(os.sep, "/")
                if only and only not in rel:
                    continue
                dst = os.path.join(live, rel)
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                process(src, dst, rel)
                manifest[mod + "/" + rel] = sha1(dst)
                n += 1
        print(mod, n, "textures")
    with open(MANIFEST, "w") as f:
        for rel in sorted(manifest):
            f.write(manifest[rel] + " " + rel + "\n")


if __name__ == "__main__":
    main()

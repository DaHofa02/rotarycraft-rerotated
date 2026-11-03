"""
Where the faces of the ports' box models lie in their texture atlases (used by modernize_textures.py; not part of the original mods).

The model classes (converted 1:1 from the originals by convert_models.py) define every box as texOffs(u, v).addBox(x, y, z, w, h, d) on
an atlas of LayerDefinition.create(mesh, W, H). A box's six faces lie in the atlas in the standard net (as ModelPart unfolds it):
    top (u+d, v, w, d), bottom (u+d+w, v, w, d),
    sides (u, v+d, d, h), (u+d, v+d, w, h), (u+d+w, v+d, d, h), (u+2d+w, v+d, w, h).
Which model a texture belongs to is chosen in the renderers' code (some by machine type), so it is found here from the texture itself:
the model of the same atlas size whose faces cover the texture's opaque pixels best.
"""
import glob
import os
import re

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NUM = r"(-?[0-9.]+)[FfDd]?"
BOX = re.compile(r"texOffs\((\d+),\s*(\d+)\)[^;]*?addBox\(\s*" + NUM + r",\s*" + NUM + r",\s*" + NUM + r",\s*" + NUM + r",\s*" + NUM + r",\s*" + NUM)
SIZE = re.compile(r"LayerDefinition\.create\(\s*\w+,\s*(\d+),\s*(\d+)\)")

_models = None


def models():
    """All box models of the three mods: (name, (W, H), [faces as (x, y, w, h)])."""
    global _models
    if _models is not None:
        return _models
    _models = []
    for f in sorted(glob.glob(os.path.join(ROOT, "*", "src", "main", "java", "**", "client", "model", "*.java"), recursive=True)):
        src = open(f, encoding="utf-8").read()
        sizes = SIZE.findall(src)
        if not sizes:
            continue
        W, H = int(sizes[0][0]), int(sizes[0][1])
        faces = []
        for m in BOX.finditer(src):
            u, v = int(m.group(1)), int(m.group(2))
            w, h, d = (int(float(m.group(i))) for i in (6, 7, 8))
            for (x, y, fw, fh) in ((u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h),
                                   (u + 2 * d + w, v + d, w, h)):
                if fw > 0 and fh > 0:
                    faces.append((x, y, fw, fh))
        if faces:
            _models.append((os.path.basename(f)[:-5], (W, H), faces))
    return _models


def coverage(faces, mask, scale):
    """Share of the opaque pixels inside the faces, and share of the face pixels that are opaque."""
    h, w = mask.shape
    cov = np.zeros_like(mask)
    for (x, y, fw, fh) in faces:
        x0, y0, x1, y1 = int(x * scale), int(y * scale), int((x + fw) * scale), int((y + fh) * scale)
        cov[max(0, y0):min(h, y1), max(0, x0):min(w, x1)] = True
    op = mask.sum()
    if op == 0 or cov.sum() == 0:
        return 0.0, 0.0
    return (mask & cov).sum() / op, (mask & cov).sum() / cov.sum()


def best(mask, rel=""):
    """(model name, faces scaled to the texture) of the model the texture surely belongs to, or None. Sure: its faces are painted almost
    completely (>= 95%), they hold at least 40% of the painted pixels (atlases often carry unused paint), and the next best model is clearly
    worse (by 0.05) or has the same faces (two models sharing one atlas layout). Models whose faces fill most of their atlas (over 60%) fit
    nearly any densely painted texture: for them the texture's path must also name the model (flywheel/wood.png - ModelFlywheel)."""
    h, w = mask.shape
    sc = []
    for name, (W, H), faces in models():
        if W * h != H * w:
            continue
        scale = w / W
        a, b = coverage(faces, mask, scale)
        if a >= 0.4:
            sc.append((b, name, sorted(faces), scale))
    if not sc:
        return None
    sc.sort(key=lambda t: -t[0])
    b, name, faces, scale = sc[0]
    if b < 0.95:
        return None
    if len(sc) > 1 and b - sc[1][0] < 0.05 and sc[1][2] != faces:
        return None
    W = mask.shape[1] / scale
    if sum(fw * fh for (x, y, fw, fh) in faces) / (W * W * mask.shape[0] / mask.shape[1]) > 0.6:
        token = name[len("Model"):].lower()
        if token not in rel.lower().replace("_", ""):
            return None
    return name, [(x * scale, y * scale, fw * scale, fh * scale) for (x, y, fw, fh) in faces]

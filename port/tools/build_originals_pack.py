#!/usr/bin/env python3
"""Builds a PRIVATE resource pack that puts the original RotaryCraft models and textures back on the machines and tools that the port
draws with its own art. The pack is made on your machine from your copy of the original mod and must not be shared: RotaryCraft's
licence (RotaryCraft/License.txt) only allows derivative works for private use. The output folder is git-ignored.

What it does:
  - Original Techne model classes (Java source in the RotaryCraft repository) are read, their renderAll() is run by a small interpreter
    (GL translate/rotate/scale, push/pop, for/if, helper methods) with the animation angles at rest, and every part drawn is written
    as one element of a standard JSON block model (assets/rotarycraft/models/block/original/<id>.json). Standard JSON only allows
    element rotations of 0/22.5/45 degrees on one axis, so its from/to/rotation are a best fit, good for viewing in Blockbench; the
    exact box, pivot, rotation, texture offset and renderAll matrix are kept in each element under "rotarycraft:part", and the mod's
    renderer (client/OriginalModels) draws those exactly like the original did.
  - Machines that are plain cubes in the port but had a model in the original get a particle-only block model and a builtin/entity
    item model in the pack, so only the original model (drawn by the mod) is seen.
  - Cube machines without an original model get the original block icons; tools and armour get their original item icons (cut from
    the original sprite sheet items2.png).

Usage: python3 -I tools/build_originals_pack.py <RotaryCraft source dir> <extracted original RotaryCraft jar> <out dir> [zip file]
"""
import json
import math
import os
import re
import shutil
import sys
import zipfile

import numpy as np
from PIL import Image

SRC, JAR, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
ZIP = sys.argv[4] if len(sys.argv) > 4 else None
TEX = os.path.join(JAR, "Reika/RotaryCraft/Textures")
ICONS = os.path.join(JAR, "assets/rotarycraft/textures/blocks")
A = os.path.join(OUT, "assets/rotarycraft")

# port block id -> (model class, texture under Textures/TileEntityTex, renderAll arguments)
REST = {"phi": 0.0, "theta": 0.0, "li": [False]}
RENDERED = {
    "worm_gear": ("ModelWorm", "Transmission/Shaft/shafttex.png"),
    "cvt": ("ModelCVT", "Transmission/cvttex.png"),
    "industrial_coil": ("ModelCoil", "Transmission/coiltex.png"),
    "bedrock_coil": ("ModelCoil", "Transmission/coiltex_bed.png"),
    "high_gear": ("ModelHighGear", "Transmission/highgeartex.png"),
    "magnetic_engine": ("ModelMagnetic", "Converter/magneticmotortex.png"),
    "dynamo": ("ModelDynamo2", "Converter/dynamotex.png"),
    "fuel_engine": ("ModelFuelEngine", "Converter/fuelenginetex.png"),
    "steam_turbine": ("ModelSteamTurbine", "Converter/steamturbtex.png"),
    "air_compressor": ("ModelCompressor", "Converter/airtex.png", {"phi": 0.5}),  # piston half way; at 0 the jacket has no height
    "pneumatic_engine": ("ModelPneumatic", "Converter/pneutex.png"),
    "portal_shaft": ("ModelShaft", "Transmission/Shaft/shafttex.png"),
    "projector": ("ModelProjector", "projtex.png"),
    "cctv": ("ModelCCTV", "cctvtex.png"),
}
# cubes in the port, models in the original: the mod's OriginalModels renderer draws them when this pack is on
CUBE_MODELS = {
    "tnt_cannon": ("ModelCannon", "tntcannontex.png"),
    "block_cannon": ("ModelCannon", "blockcannontex.png"),
    "sonic_weapon": ("ModelSonicWeapon", "sonictex.png"),
    "railgun": ("ModelRailGun", "railguntex.png"),
    "freeze_gun": ("ModelFreezeGun", "freezeguntex.png"),
    "laser_gun": ("ModelLaserGun", "lasertex.png"),
    "landmine": ("ModelLandmine", "landminetex.png"),
    "emp": ("ModelEMP", "emptex.png"),
    "air_gun": ("ModelAirGun", "airguntex.png"),
    "distillery": ("ModelDistillery", "distillertex.png"),
    "flame_turret": ("ModelFlameTurret", "flameturrettex.png"),
    "anti_air_gun": ("ModelAAGun", "aagun.png"),
    "multi_cannon": ("ModelMultiCannon", "multicannontex.png"),
    "containment": ("ModelDomeEmitter", "containtex.png"),
    "boiler": ("ModelBoiler", "Converter/boilertex.png"),
}
# cube machines without an original model: face -> original block icon (faces not listed keep the port's texture)
CUBE_ICONS = {
    "music_box": {"all": "musicbox"},
    "purifier": {"all": "purifier"},
    "self_destruct": {"all": "destruct"},
    "drop_processor": {"side": "drops_side", "north": "drops_front", "south": "drops_back"},
    "auto_crafter": {"top": "crafter_top"},
    "item_filter": {"top": "filter_top"},
    "machine_gun": {"north": "gun_front", "south": "gun_back"},
}
# port item id -> cell in the original items2.png (16x16 cells), from the original ItemRegistry
ITEMS = {"bedrock_pickaxe": 101, "bedrock_axe": 100, "bedrock_shovel": 102, "bedrock_sword": 23, "bedrock_hoe": 21,
         "bedrock_shears": 25, "bedrock_scythe": 36, "bedrock_helmet": 7, "bedrock_chestplate": 9, "bedrock_leggings": 10,
         "bedrock_boots": 8, "handbook": 208}

ITEM_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}

NUM = r"-?[0-9]*\.?[0-9]+(?:[eE]-?[0-9]+)?[FfDdL]?"
warnings = []


def find_model(name):
    for root, _, files in os.walk(SRC):
        if name + ".java" in files:
            return os.path.join(root, name + ".java")
    raise FileNotFoundError(name)


def num(s):
    return float(s.rstrip("FfDdL"))


def block_body(text, start):
    """The text between the brace at or after start and its matching closing brace."""
    i = text.index("{", start)
    depth = 0
    for j in range(i, len(text)):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                return text[i + 1:j]
    raise ValueError("unbalanced braces")


def parse_parts(text):
    """Constructor data of every part: texture offset, boxes, pivot, rotation, mirror, texture size."""
    tw = int(num(re.search(r"textureWidth\s*=\s*(" + NUM + ")", text).group(1))) if "textureWidth" in text else 64
    th = int(num(re.search(r"textureHeight\s*=\s*(" + NUM + ")", text).group(1))) if "textureHeight" in text else 32
    parts = {}
    for m in re.finditer(r"(\w+)\s*=\s*new\s+(?:LODModelPart|ModelRenderer)\(\s*this\s*,\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*\)", text):
        parts[m.group(1)] = {"u": int(num(m.group(2))), "v": int(num(m.group(3))), "boxes": [], "pivot": [0, 0, 0], "rot": [0, 0, 0],
                             "mirror": False, "tex": [tw, th]}
    for m in re.finditer(r"(\w+)\.addBox\(([^)]*)\)", text):
        if m.group(1) in parts:
            parts[m.group(1)]["boxes"].append([num(a.strip()) for a in m.group(2).split(",")][:6])
    for m in re.finditer(r"(\w+)\.setRotationPoint\(([^)]*)\)", text):
        if m.group(1) in parts:
            parts[m.group(1)]["pivot"] = [num(a.strip()) for a in m.group(2).split(",")]
    for m in re.finditer(r"(\w+)\.setTextureSize\(([^)]*)\)", text):
        if m.group(1) in parts:
            parts[m.group(1)]["tex"] = [int(num(a.strip())) for a in m.group(2).split(",")]
    for m in re.finditer(r"(\w+)\.mirror\s*=\s*(true|false)", text):
        if m.group(1) in parts:
            parts[m.group(1)]["mirror"] = m.group(2) == "true"
    for m in re.finditer(r"setRotation\(\s*(\w+)\s*,([^)]*)\)", text):
        if m.group(1) in parts:
            parts[m.group(1)]["rot"] = [num(a.strip()) for a in m.group(2).split(",")]
    for m in re.finditer(r"(\w+)\.rotateAngle([XYZ])\s*=\s*(" + NUM + ")", text):
        if m.group(1) in parts:
            parts[m.group(1)]["rot"]["XYZ".index(m.group(2))] = num(m.group(3))
    return parts


def rot_matrix(angle_deg, x, y, z):
    a = math.radians(angle_deg)
    n = math.sqrt(x * x + y * y + z * z) or 1
    x, y, z = x / n, y / n, z / n
    c, s, t = math.cos(a), math.sin(a), 1 - math.cos(a)
    r = np.identity(4)
    r[:3, :3] = [[t * x * x + c, t * x * y - s * z, t * x * z + s * y],
                 [t * x * y + s * z, t * y * y + c, t * y * z - s * x],
                 [t * x * z - s * y, t * y * z + s * x, t * z * z + c]]
    return r


class Interp:
    """Runs the GL part of a Java render method: a matrix stack in block units and the list of (part, matrix) drawn."""

    def __init__(self, text, parts, args):
        self.text, self.parts = text, parts
        self.m = np.identity(4)
        self.stack = []
        self.drawn = []
        self.args = args
        self.methods = {}
        for mm in re.finditer(r"(?:public|private|protected)\s+void\s+(\w+)\s*\(([^)]*)\)\s*\{", text):
            params = [p.strip().split()[-1] for p in mm.group(2).split(",") if p.strip()]
            self.methods[mm.group(1)] = (params, block_body(text, mm.start()))

    # --- expressions: Java -> Python, evaluated with the method's variables
    def ev(self, expr, env):
        e = expr.strip()
        e = re.sub(r"\((?:double|float|int|long|Boolean|boolean)\)", "", e)
        e = re.sub(r"(\d)[FfDdL]\b", r"\1", e)
        e = e.replace("&&", " and ").replace("||", " or ").replace("Math.", "math.").replace("ReikaMathLibrary.", "")
        e = re.sub(r"!(?!=)", " not ", e)
        e = e.replace("true", "True").replace("false", "False")
        e = re.sub(r"(\w+)\.get\((\d+)\)", r"\1[\2]", e)
        return eval(e, {"math": math, "__builtins__": {}}, env)

    def run_method(self, name, argvals):
        params, body = self.methods[name]
        env = dict(zip(params, argvals))
        env.update({k: v for k, v in self.args.items() if k not in env})
        self.block(body, env)

    def block(self, body, env):
        stmts = self.split(body)
        i = 0
        while i < len(stmts):
            s = stmts[i].strip()
            i += 1
            if not s:
                continue
            m = re.match(r"for\s*\((.*?);(.*?);(.*?)\)\s*(\{.*\}|.*)$", s, re.S)
            if m:
                init, cond, inc, body2 = m.groups()
                body2 = body2.strip()
                body2 = body2[1:-1] if body2.startswith("{") else body2
                self.assign(init, env)
                guard = 0
                while self.ev(cond, env) and guard < 1000:
                    self.block(body2, env)
                    self.assign(inc, env)
                    guard += 1
                continue
            m = re.match(r"if\s*\((.*)\)\s*(\{.*\}|[^{].*)$", s, re.S)
            if m:
                cond, rest = self.split_cond(s)
                then, els = self.split_else(rest)
                if self.ev(cond, env):
                    self.block(then, env)
                elif els is not None:
                    self.block(els, env)
                continue
            self.simple(s, env)

    def split_cond(self, s):
        """'if (cond) rest' -> cond, rest (parentheses balanced)."""
        i = s.index("(")
        depth = 0
        for j in range(i, len(s)):
            depth += s[j] == "("
            depth -= s[j] == ")"
            if depth == 0:
                return s[i + 1:j], s[j + 1:].strip()
        raise ValueError(s)

    def split_else(self, rest):
        if rest.startswith("{"):
            body = block_body(rest, 0)
            after = rest[len(body) + 2:].strip()
        else:
            k = rest.index(";")
            body, after = rest[:k + 1], rest[k + 1:].strip()
        if after.startswith("else"):
            e = after[4:].strip()
            return body, (block_body(e, 0) if e.startswith("{") else e)
        return body, None

    def split(self, body):
        """Top-level statements; a for/if keeps its block (and else) in one statement."""
        out, cur, depth, paren = [], "", 0, 0
        i = 0
        while i < len(body):
            c = body[i]
            cur += c
            if c == "(":
                paren += 1
            elif c == ")":
                paren -= 1
            elif c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0 and paren == 0:
                    nxt = body[i + 1:].lstrip()
                    if not nxt.startswith("else"):
                        out.append(cur)
                        cur = ""
            elif c == ";" and depth == 0 and paren == 0:
                st = cur.strip()
                nxt = body[i + 1:].lstrip()
                if re.match(r"if\s*\(", st) and nxt.startswith("else"):
                    pass
                else:
                    out.append(cur)
                    cur = ""
            i += 1
        if cur.strip():
            out.append(cur)
        return out

    def assign(self, s, env):
        s = s.strip()
        if not s:
            return
        m = re.match(r"(?:final\s+)?(?:double|float|int|long|boolean|var)\s+(\w+)\s*=\s*(.*)$", s, re.S)
        if m:
            env[m.group(1)] = self.ev(m.group(2), env)
            return
        m = re.match(r"(\w+)\s*([+\-*/])=\s*(.*)$", s, re.S)
        if m:
            env[m.group(1)] = self.ev("%s %s (%s)" % (m.group(1), m.group(2), m.group(3)), env)
            return
        m = re.match(r"(\w+)\s*(\+\+|--)$", s)
        if m:
            env[m.group(1)] += 1 if m.group(2) == "++" else -1
            return
        m = re.match(r"(\w+)\s*=\s*(.*)$", s, re.S)
        if m:
            env[m.group(1)] = self.ev(m.group(2), env)
            return
        raise ValueError("cannot assign: " + s)

    def simple(self, s, env):
        s = s.rstrip(";").strip()
        if s.startswith("{"):
            self.block(s[1:-1], env)
            return
        m = re.match(r"GL11\.gl(Translate|Rotate|Scale)[df]?\((.*)\)$", s, re.S)
        if m:
            a = [self.ev(x, env) for x in self.args_of(m.group(2))]
            if m.group(1) == "Translate":
                t = np.identity(4)
                t[:3, 3] = a
                self.m = self.m @ t
            elif m.group(1) == "Rotate":
                self.m = self.m @ rot_matrix(*a)
            else:
                self.m = self.m @ np.diag([a[0], a[1], a[2], 1.0])
            return
        if re.match(r"GL11\.glPushMatrix\(\)", s):
            self.stack.append(self.m.copy())
            return
        if re.match(r"GL11\.glPopMatrix\(\)", s):
            self.m = self.stack.pop()
            return
        if s.startswith("GL11.") or s.startswith("return") or "Tessellator" in s or "ReikaRenderHelper" in s or "bindTexture" in s:
            return
        m = re.match(r"(\w+)\.render\(\s*(?:te\s*,\s*)?f5\s*\)$", s)
        if m and m.group(1) in self.parts:
            p = self.parts[m.group(1)]
            self.drawn.append((m.group(1), self.m.copy(), list(p["pivot"]), list(p["rot"])))
            return
        m = re.match(r"(\w+)\.setRotationPoint\((.*)\)$", s, re.S)
        if m and m.group(1) in self.parts:  # some models move a part between two renders
            self.parts[m.group(1)]["pivot"] = [float(self.ev(x, env)) for x in self.args_of(m.group(2))]
            return
        m = re.match(r"(\w+)\.rotateAngle([XYZ])\s*=\s*(.*)$", s, re.S)
        if m and m.group(1) in self.parts:
            self.parts[m.group(1)]["rot"]["XYZ".index(m.group(2))] = float(self.ev(m.group(3), env))
            return
        m = re.match(r"(?:final\s+)?(?:double|float|int|long|boolean)\s+(\w+)$", s)
        if m:
            env[m.group(1)] = 0
            return
        m = re.match(r"(?:this\.)?(\w+)\((.*)\)$", s, re.S)
        if m and m.group(1) in self.methods:
            self.run_method(m.group(1), [self.ev(x, env) if x.strip() not in ("te",) else None for x in self.args_of(m.group(2))])
            return
        try:
            self.assign(s, env)
        except Exception:
            warnings.append("skipped: " + s[:80])

    @staticmethod
    def args_of(s):
        out, cur, depth = [], "", 0
        for c in s:
            if c == "," and depth == 0:
                out.append(cur)
                cur = ""
                continue
            depth += c == "("
            depth -= c == ")"
            cur += c
        if cur.strip():
            out.append(cur)
        return out


def drawn_parts(model, overrides):
    text = open(find_model(model)).read()
    text = re.sub(r"//[^\n]*", "", text)
    parts = parse_parts(text)
    args = dict(REST)
    args.update(overrides or {})
    it = Interp(text, parts, args)
    try:
        it.run_method("renderAll", [None, args["li"], args["phi"], args["theta"]])
    except Exception as e:  # fall back: every part once, at rest
        warnings.append("%s: renderAll not run (%s), every part drawn once" % (model, e))
        it.drawn = [(n, np.identity(4), list(p["pivot"]), list(p["rot"])) for n, p in parts.items()]
    return parts, it.drawn


# --- standard JSON (best fit) -------------------------------------------------------------------------------------------------------

FACES = {  # face name -> (outward normal in part space, Techne UV rectangle as a function of u, v, w, h, d)
    "east": ((1, 0, 0), lambda u, v, w, h, d: (u + d + w, v + d, u + d + w + d, v + d + h)),
    "west": ((-1, 0, 0), lambda u, v, w, h, d: (u, v + d, u + d, v + d + h)),
    "up": ((0, -1, 0), lambda u, v, w, h, d: (u + d, v, u + d + w, v + d)),
    "down": ((0, 1, 0), lambda u, v, w, h, d: (u + d + w, v, u + d + w + w, v + d)),
    "front": ((0, 0, -1), lambda u, v, w, h, d: (u + d, v + d, u + d + w, v + d + h)),
    "back": ((0, 0, 1), lambda u, v, w, h, d: (u + d + w + d, v + d, u + d + w + d + w, v + d + h)),
}
WORLD = np.diag([1.0, -1.0, -1.0])        # model space (y down, z flipped by the renderer's scale(1,-1,-1)) -> block space
WORLD_OFF = np.array([8.0, 24.0, 8.0])     # in pixels
AXES = {"x": np.array([1, 0, 0]), "y": np.array([0, 1, 0]), "z": np.array([0, 0, 1])}
DIRS = {"east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0), "south": (0, 0, 1), "north": (0, 0, -1)}


def part_matrix(p):
    """Part space (pixels) -> model space (pixels): pivot, then rotation Z*Y*X, as ModelRenderer does."""
    rx, ry, rz = p["rot"]
    m = np.identity(4)
    m[:3, 3] = p["pivot"]
    return m @ rot_matrix(math.degrees(rz), 0, 0, 1) @ rot_matrix(math.degrees(ry), 0, 1, 0) @ rot_matrix(math.degrees(rx), 1, 0, 0)


def to_block(gl, pm, pts):
    """Part-space points -> block-space pixels (the renderAll matrix gl is in block units)."""
    g = gl.copy()
    g[:3, 3] *= 16
    out = []
    for q in pts:
        v = g @ pm @ np.array([q[0], q[1], q[2], 1.0])
        out.append(WORLD @ v[:3] + WORLD_OFF)
    return np.array(out)


def axis_rot(axis, deg):
    return rot_matrix(deg, *AXES[axis])[:3, :3]


def best_fit(corners, origin, true_vol):
    best = None
    for axis in "xyz":
        for q in (-45, -22.5, 0, 22.5, 45):
            r = axis_rot(axis, -q)
            pts = (r @ (corners - origin).T).T + origin
            lo, hi = pts.min(axis=0), pts.max(axis=0)
            err = np.prod(np.maximum(hi - lo, 1e-6)) - true_vol
            if best is None or err < best[0] - 1e-6 or (abs(err - best[0]) < 1e-6 and q == 0):
                best = (err, axis, q, lo, hi)
    return best


def element(name, p, box, gl):
    x, y, z, w, h, d = box
    pm = part_matrix(p)
    corners = [(x + i * w, y + j * h, z + k * d) for i in (0, 1) for j in (0, 1) for k in (0, 1)]
    wc = to_block(gl, pm, corners)
    origin = to_block(gl, pm, [(0, 0, 0)])[0]
    err, axis, q, lo, hi = best_fit(wc, origin, max(w * h * d, 1e-6))
    tw, th = p["tex"]
    u, v = p["u"], p["v"]
    rot_q = axis_rot(axis, -q)
    lin = (WORLD @ (gl[:3, :3] @ pm[:3, :3]))
    faces = {}
    for fname, (n, uvf) in FACES.items():
        wn = rot_q @ (lin @ np.array(n, float))
        best_dir = max(DIRS, key=lambda dn: np.dot(wn, DIRS[dn]))
        u1, v1, u2, v2 = uvf(u, v, w, h, d)
        if p["mirror"]:
            u1, u2 = u2, u1
        faces[best_dir] = {"uv": [round(u1 * 16 / tw, 4), round(v1 * 16 / th, 4), round(u2 * 16 / tw, 4), round(v2 * 16 / th, 4)],
                           "texture": "#0"}
    if abs(err) > 0.01 * max(w * h * d, 1):
        warnings.append("%s: rotation approximated in the JSON view" % name)
    el = {"name": name, "from": [round(c, 4) for c in lo], "to": [round(c, 4) for c in hi], "faces": faces,
          "rotarycraft:part": {"u": u, "v": v, "box": box, "pivot": p["pivot"], "rotation": p["rot"], "mirror": p["mirror"],
                               "texture_size": p["tex"], "matrix": [round(c, 6) for c in gl.T.flatten()]}}
    if q != 0:
        el["rotation"] = {"angle": q, "axis": axis, "origin": [round(c, 4) for c in origin]}
    return el


def write(rel, data):
    path = os.path.join(A, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)


def copy_tex(src, name):
    dst = os.path.join(A, "textures/block/original", name + ".png")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    return "rotarycraft:block/original/" + name


def model(block_id, spec):
    cls, tex = spec[0], spec[1]
    parts, drawn = drawn_parts(cls, spec[2] if len(spec) > 2 else None)
    texname = os.path.splitext(tex.replace("/", "_").lower())[0]
    texref = copy_tex(os.path.join(TEX, "TileEntityTex", tex), texname)
    els = []
    for name, gl, pivot, rot in drawn:
        state = dict(parts[name], pivot=pivot, rot=rot)  # pivot and rotation as they were when this part was drawn
        for box in state["boxes"]:
            els.append(element(name, state, box, gl))
    write("models/block/original/%s.json" % block_id, {
        "credit": "Original RotaryCraft model %s by Reika Kalseki, converted for private use (tools/build_originals_pack.py)" % cls,
        "textures": {"0": texref, "particle": texref},
        "rotarycraft:texture": texref.replace("rotarycraft:", "rotarycraft:textures/") + ".png",
        "elements": els})
    return texref, len(els)


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(A)
    with open(os.path.join(OUT, "pack.mcmeta"), "w") as f:
        json.dump({"pack": {"pack_format": 34, "description": "RotaryCraft original models (private use only)"}}, f, indent=1)
    with open(os.path.join(OUT, "README.txt"), "w") as f:
        f.write("Original RotaryCraft models and textures, (c) Reika Kalseki, https://github.com/ReikaKalseki/RotaryCraft\n"
                "Converted on this machine from the original mod for PRIVATE USE ONLY (see RotaryCraft/License.txt).\n"
                "Do not share or upload this pack.\n")
    count = 0
    for bid, spec in RENDERED.items():
        _, n = model(bid, spec)
        count += n
    for bid, spec in CUBE_MODELS.items():
        texref, n = model(bid, spec)
        count += n
        write("models/block/%s.json" % bid, {"textures": {"particle": texref}})
        write("models/item/%s.json" % bid, {"parent": "minecraft:builtin/entity", "display": ITEM_DISPLAY})
    for bid, faces in CUBE_ICONS.items():
        tex = {f: "rotarycraft:block/own/%s_%s" % (bid, "top" if f in ("up", "down") else "side") for f in DIRS}
        for f, icon in faces.items():
            ref = copy_tex(os.path.join(ICONS, icon + ".png"), "icon_" + icon)
            for t in (DIRS if f == "all" else ("north", "south", "east", "west") if f == "side" else ("up", "down") if f == "top" else (f,)):
                tex[t] = ref
        write("models/block/%s.json" % bid, {"parent": "minecraft:block/cube", "textures": dict(tex, particle=tex["north"])})
    sheet = Image.open(os.path.join(TEX, "Items/items2.png")).convert("RGBA")
    for iid, idx in ITEMS.items():
        cell = sheet.crop((idx % 16 * 16, idx // 16 * 16, idx % 16 * 16 + 16, idx // 16 * 16 + 16))
        dst = os.path.join(A, "textures/item/own/%s.png" % iid)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        cell.save(dst)
    if ZIP:
        with zipfile.ZipFile(ZIP, "w", zipfile.ZIP_DEFLATED) as z:
            for root, _, files in os.walk(OUT):
                for fn in files:
                    full = os.path.join(root, fn)
                    z.write(full, os.path.relpath(full, OUT))
    print("%d models (%d elements), %d icon cubes, %d item icons" % (len(RENDERED) + len(CUBE_MODELS), count, len(CUBE_ICONS), len(ITEMS)))
    approx = [w for w in warnings if "approximated" in w]
    print("  %d elements with rotations the JSON view can only approximate (exact in game)" % len(approx))
    for w in sorted(set(warnings) - set(approx)):
        print("  note:", w)


main()

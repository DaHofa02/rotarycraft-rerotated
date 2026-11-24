#!/usr/bin/env python3
"""Replaces fully qualified class names in Java code with imports where the simple name is unambiguous in the file.
Only code is touched (not comments, javadoc or strings). Usage: fqn_to_imports.py <file.java>... ; prints a summary."""
import os
import re
import sys

PKG = r"(?:reika|net\.minecraft|net\.neoforged|com\.mojang|com\.simibubi|appeng|mod\.chiselsandbits|java\.util|java\.io|org\.joml|javax\.annotation)"
FQN = re.compile(r"(?<![\w.])(" + PKG + r"(?:\.[a-z_][a-z0-9_]*)*)\.([A-Z][A-Za-z0-9_]*)")
JAVA_LANG = set("Object String Math Integer Long Double Float Boolean Byte Short Character Number Class System Thread Runnable Iterable "
                "Override Deprecated SuppressWarnings FunctionalInterface Exception RuntimeException Error Throwable Enum Record Void "
                "StringBuilder IllegalStateException IllegalArgumentException NullPointerException Comparable CharSequence Process "
                "Module Package Runtime SafeVarargs".split())


def code_spans(src):
    """(start, end) ranges of code outside comments, strings and char literals."""
    spans, i, start, n = [], 0, 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith("//", i) or src.startswith("/*", i) or c in "\"'":
            spans.append((start, i))
            if src.startswith("//", i):
                j = src.find("\n", i)
                i = n if j < 0 else j
            elif src.startswith("/*", i):
                j = src.find("*/", i + 2)
                i = n if j < 0 else j + 2
            elif src.startswith('"""', i):
                j = src.find('"""', i + 3)
                i = n if j < 0 else j + 3
            else:
                q = c
                i += 1
                while i < n and src[i] != q:
                    i += 2 if src[i] == "\\" else 1
                i += 1
            start = i
        else:
            i += 1
    spans.append((start, n))
    # package and import lines are not code to rewrite
    skip = [(m.start(), m.end()) for m in re.finditer(r"^(?:package|import)\s[^;\n]*;", src, re.M)]
    out = []
    for a, b in spans:
        for s, e in skip:
            if s < b and e > a:
                if a < s:
                    out.append((a, s))
                a = max(a, e)
        if a < b:
            out.append((a, b))
    return out


def process(path):
    src = open(path).read()
    pkg_m = re.search(r"^package\s+([\w.]+);", src, re.M)
    pkg = pkg_m.group(1) if pkg_m else ""
    imports = re.findall(r"^import\s+(static\s+)?([\w.]+);", src, re.M)
    imported = {}  # simple -> fqn
    for st, imp in imports:
        if not st:
            imported[imp.rsplit(".", 1)[1]] = imp
    same_pkg = {f[:-5] for f in os.listdir(os.path.dirname(path)) if f.endswith(".java")}
    declared = set(re.findall(r"\b(?:class|interface|enum|record)\s+([A-Z]\w*)", src)) | set(re.findall(r"<\s*([A-Z]\w*)\s+extends", src))
    spans = code_spans(src)
    found = {}  # simple -> set of qualified
    for a, b in spans:
        for m in FQN.finditer(src, a, b):
            found.setdefault(m.group(2), set()).add(m.group(1) + "." + m.group(2))
    ok = {}
    for simple, quals in found.items():
        if len(quals) != 1:
            continue
        q = next(iter(quals))
        if q.rsplit(".", 1)[0] == pkg:
            ok[simple] = q  # same package: no import needed
            continue
        if simple in JAVA_LANG or simple in declared:
            continue
        if simple in imported and imported[simple] != q:
            continue
        if simple in same_pkg:
            continue
        ok[simple] = q
    if not ok:
        return 0
    out, last, count = [], 0, 0
    for a, b in spans:
        out.append(src[last:a])
        seg = src[a:b]

        def rep(m):
            nonlocal count
            if m.group(2) in ok and ok[m.group(2)] == m.group(1) + "." + m.group(2):
                count += 1
                return m.group(2)
            return m.group(0)
        out.append(FQN.sub(rep, seg))
        last = b
    out.append(src[last:])
    new = "".join(out)
    need = sorted({q for s, q in ok.items() if q.rsplit(".", 1)[0] != pkg and imported.get(s) != q})
    lines = new.split("\n")
    for q in need:
        top = q.split(".")[0] + "." + q.split(".")[1]
        idx = [i for i, l in enumerate(lines) if l.startswith("import ") and not l.startswith("import static")]
        grp = [i for i in idx if lines[i][7:].startswith(top)]
        if grp:  # keep the group sorted
            pos = next((i for i in grp if lines[i][7:-1] > q), grp[-1] + 1)
        elif idx:
            pos = idx[-1] + 1
            lines.insert(pos, "")
            pos += 1
        else:
            pos = next(i for i, l in enumerate(lines) if l.startswith("package ")) + 1
            lines.insert(pos, "")
            pos += 1
        lines.insert(pos, "import %s;" % q)
    open(path, "w").write("\n".join(lines))
    return count


total = 0
for p in sys.argv[1:]:
    total += process(p)
print("replaced", total)

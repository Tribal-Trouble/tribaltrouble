"""Round-trip gate for the Tribal Trouble Blender addon: import files, re-export them, diff numerically.

Run headless from the repo root, with either a skeleton plus clips or one or more mesh files:

    blender -b --factory-startup --python tools/blender/validate_roundtrip.py -- \
        assets/geometry/vikings/peon/peon_skeleton.xml assets/geometry/vikings/peon/peon_run.xml [more clips...]
    blender -b --factory-startup --python tools/blender/validate_roundtrip.py -- assets/geometry/vikings/warrior/warrior_mesh.xml
--factory-startup keeps an installed copy of the addon from loading next to the one under test.

Exit code 0 when every value matches within tolerance (1e-4 for positions, UVs, weights and matrices; 1.5e-2 for
normal components, since Blender stores custom split normals at reduced precision), 1 otherwise. The exported files
are left in a temporary directory that is printed, for inspection.
"""
import importlib.util
import os
import sys
import tempfile
import xml.etree.ElementTree as ET

import bpy

TOLERANCE = 1e-4

addon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "io_tribaltrouble.py")
spec = importlib.util.spec_from_file_location("io_tribaltrouble", addon_path)
addon = importlib.util.module_from_spec(spec)
spec.loader.exec_module(addon)
addon.register()
bpy.ops.wm.read_factory_settings(use_empty=True)

args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
if not args:
    print(__doc__)
    sys.exit(2)
skeleton = os.path.abspath(args[0])
clips = [os.path.abspath(a) for a in args[1:]]


def matrices(path):
    """(frame index or 'rest', bone) -> 16 floats; skeleton files also carry the parent map under ('parents', '')."""
    root = ET.parse(path).getroot()
    out = {}
    if root.tag == "skeleton":
        for t in root.find("init_pose"):
            out[("rest", t.get("name"))] = [float(t.get(f"m{c}{r}")) for c in range(4) for r in range(4)]
        out[("parents", "")] = sorted((b.get("name"), b.get("parent")) for b in root.find("bones"))
    elif root.tag == "animation":
        for f in root.iter("frame"):
            for t in f.iter("transform"):
                out[(int(f.get("index")), t.get("name"))] = [float(t.get(f"m{c}{r}"))
                                                             for c in range(4) for r in range(4)]
    else:
        raise ValueError(f"{path}: not a skeleton or animation file")
    return out


def compare(original, exported):
    a, b = matrices(original), matrices(exported)
    if a.keys() != b.keys():
        missing = sorted(set(a) - set(b))[:3]
        extra = sorted(set(b) - set(a))[:3]
        return float("inf"), f"entries differ (missing {missing}, extra {extra})"
    if a.get(("parents", "")) != b.get(("parents", "")):
        return float("inf"), "parent map differs"
    worst = 0.0
    where = ""
    for key, va in a.items():
        if key[0] == "parents":
            continue
        d = max(abs(x - y) for x, y in zip(va, b[key]))
        if d > worst:
            worst, where = d, f"at {key}"
    return worst, where


# Blender stores custom split normals at reduced precision, coarsest for normals nearly perpendicular to their face;
# a handful of building corners come back up to 0.6 degrees off. Invisible in game.
NORMAL_TOLERANCE = 1.5e-2


def unit(n):
    length = sum(c * c for c in n) ** 0.5
    return tuple(c / length for c in n) if length > 0 else n


def polygons(path):
    """Per polygon, per corner: (position, unit normal, uv, colour, {bone: weight}).

    Source files carry normals of any length (the game normalises at load) and zero-weight skin entries; both are
    reduced here so a faithful export compares equal.
    """
    root = ET.parse(path).getroot()
    out = []
    for polygon in root.find("polygons"):
        corners = []
        for v in polygon:
            skins = {}
            for s in v:  # a bone listed twice for one vertex counts twice, as the game sums entries
                skins[s.get("bone")] = skins.get(s.get("bone"), 0.0) + float(s.get("weight"))
            corners.append((
                tuple(float(v.get(k)) for k in ("x", "y", "z")),
                unit(tuple(float(v.get(k)) for k in ("nx", "ny", "nz"))),
                tuple(float(v.get(k) or 0.0) for k in ("u", "v", "u2", "v2")),
                tuple(float(v.get(k)) for k in ("r", "g", "b", "a")),
                {bone: w for bone, w in skins.items() if w > 0.0},
            ))
        out.append(corners)
    return out


def compare_meshes(original, exported):
    """Max differences (geometry, normals) between two mesh files, polygon by polygon in file order."""
    a, b = polygons(original), polygons(exported)
    if len(a) != len(b):
        return float("inf"), float("inf"), f"polygon count {len(a)} vs {len(b)}"
    worst = worst_normal = 0.0
    where = ""
    for i, (pa, pb) in enumerate(zip(a, b)):
        for ca, cb in zip(pa, pb):
            d = max(abs(x - y) for va, vb in zip(ca[:1] + ca[2:4], cb[:1] + cb[2:4]) for x, y in zip(va, vb))
            if ca[4].keys() != cb[4].keys():
                d = float("inf")
            else:
                d = max(d, max(abs(ca[4][k] - cb[4][k]) for k in ca[4]))
            if d > worst:
                worst, where = d, f"at polygon {i}"
            worst_normal = max(worst_normal, max(abs(x - y) for x, y in zip(ca[1], cb[1])))
    return worst, worst_normal, where


out_dir = tempfile.mkdtemp(prefix="tt_roundtrip_")
failed = False

is_mesh = ET.parse(skeleton).getroot().tag == "mesh"
if is_mesh:
    for original in [skeleton] + clips:
        bpy.ops.object.select_all(action="DESELECT")
        bpy.ops.import_mesh.tt_xml(filepath=original, load_textures=False)
        exported = os.path.join(out_dir, os.path.basename(original))
        bpy.ops.export_mesh.tt_xml(filepath=exported)
        worst, worst_normal, where = compare_meshes(original, exported)
        ok = worst <= TOLERANCE and worst_normal <= NORMAL_TOLERANCE
        failed |= not ok
        print(f"{'PASS' if ok else 'FAIL'} {os.path.basename(original)}: max abs diff {worst:.2e} {where}, "
              f"normals {worst_normal:.2e}")
else:
    directory = os.path.dirname(skeleton)
    files = [{"name": os.path.basename(skeleton)}] + [{"name": os.path.basename(c)} for c in clips]
    if any(os.path.dirname(c) != directory for c in clips):
        print("All clips must sit in the skeleton's directory")
        sys.exit(2)
    bpy.ops.import_scene.tt_skeleton(directory=directory, files=files, bind_selected=False)
    out_skeleton = os.path.join(out_dir, os.path.basename(skeleton))
    bpy.ops.export_scene.tt_skeleton(filepath=out_skeleton, export_clips=bool(clips))
    for original in [skeleton] + clips:
        exported = os.path.join(out_dir, os.path.basename(original))
        if not os.path.isfile(exported):
            print(f"FAIL {os.path.basename(original)}: not exported")
            failed = True
            continue
        worst, where = compare(original, exported)
        ok = worst <= TOLERANCE
        failed |= not ok
        print(f"{'PASS' if ok else 'FAIL'} {os.path.basename(original)}: max abs diff {worst:.2e} {where}")

print(f"Exported files in {out_dir}")
print("ROUND TRIP", "OK" if not failed else "FAILED")
sys.exit(1 if failed else 0)

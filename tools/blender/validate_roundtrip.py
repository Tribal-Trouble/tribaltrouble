"""Round-trip gate for the Tribal Trouble Blender addon: import a skeleton and clips, re-export, diff numerically.

Run headless from the repo root:

    blender -b --python tools/blender/validate_roundtrip.py -- \
        assets/geometry/vikings/peon/peon_skeleton.xml assets/geometry/vikings/peon/peon_run.xml [more clips...]

Exit code 0 when every matrix and the parent map match within TOLERANCE, 1 otherwise. The exported files are left in
a temporary directory that is printed, for inspection.
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


out_dir = tempfile.mkdtemp(prefix="tt_roundtrip_")
directory = os.path.dirname(skeleton)
files = [{"name": os.path.basename(skeleton)}] + [{"name": os.path.basename(c)} for c in clips]
if any(os.path.dirname(c) != directory for c in clips):
    print("All clips must sit in the skeleton's directory")
    sys.exit(2)
bpy.ops.import_scene.tt_skeleton(directory=directory, files=files, bind_selected=False)
arm = bpy.context.active_object
out_skeleton = os.path.join(out_dir, os.path.basename(skeleton))
bpy.ops.export_scene.tt_skeleton(filepath=out_skeleton, export_clips=bool(clips))

failed = False
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

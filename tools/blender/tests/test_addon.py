"""Headless tests for the Tribal Trouble Blender addon. Run from the repo root:

    blender -b --factory-startup --python tools/blender/tests/test_addon.py

--factory-startup keeps an installed copy of the addon from loading next to the one under test. Everything is done
in a temporary copy of assets/geometry, so the repo is never written to. Exit code 0 when every test passes.

The shapes and images the tests make are throwaway fixtures inside that temporary folder. They are never art for
the game and must never be copied into the repo.
"""
import glob
import importlib.util
import os
import re
import shutil
import subprocess
import sys
import tempfile
import traceback
import types
import xml.etree.ElementTree as ET

import bpy
import numpy as np

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
TEMP = tempfile.mkdtemp(prefix="tt_addon_test_")
GEOMETRY = os.path.join(TEMP, "assets", "geometry")
MODELS = os.path.join(TEMP, "assets", "textures", "models")
DECALS = os.path.join(TEMP, "assets", "textures", "teamdecals")

shutil.copytree(os.path.join(REPO, "assets", "geometry"), GEOMETRY)
os.makedirs(MODELS)
os.makedirs(DECALS)
for texture in ("viking_warrior_rock", "viking_warrior_iron", "viking_warrior_rubber", "native_warrior_rock",
                "viking_buildings_hi", "viking_peon_hammer"):
    shutil.copy(os.path.join(REPO, "assets", "textures", "models", texture + ".png"), MODELS)
    shutil.copy(os.path.join(REPO, "assets", "textures", "teamdecals", texture + "_team.png"), DECALS)

# A developer may keep uncommitted test sprites (named *_dev_*) in the working tree; results must not depend on them.
registry_path = os.path.join(GEOMETRY, "geometry.xml")
with open(registry_path, "rb") as f:
    registry_text = f.read().decode("utf-8")

# The tests use the warrior's axe as the item painted on its unit's atlas, as it was before it got textures of its
# own, so the temp copy gets that axe back.
FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")
for level in ("warrior_axe_held", "warrior_axe_held_lo"):
    shutil.copy(os.path.join(FIXTURES, level + "_shared.xml"), os.path.join(GEOMETRY, "vikings", "warrior", level + ".xml"))
for tier in ("rock", "iron", "rubber"):
    registry_text = registry_text.replace(f'name="viking_warrior_axe_held_{tier}" team="viking_warrior_axe_held_{tier}_team"',
                                          f'name="viking_warrior_{tier}" team="viking_warrior_{tier}_team"')
registry_text = re.sub(r'[ \t]*<sprite name="[^"]*_dev_[^"]*".*?</sprite>\r?\n', "", registry_text, flags=re.S)
with open(registry_path, "wb") as f:
    f.write(registry_text.encode("utf-8"))
PRISTINE = registry_text

ADDON_DIR = os.path.join(REPO, "tools", "blender", "io_tribaltrouble")
spec = importlib.util.spec_from_file_location("io_tribaltrouble", os.path.join(ADDON_DIR, "__init__.py"),
                                              submodule_search_locations=[ADDON_DIR])
package = importlib.util.module_from_spec(spec)
sys.modules["io_tribaltrouble"] = package
spec.loader.exec_module(package)
package.register()
# Every module's names in one place, so a test need not know which module holds what.
addon = types.SimpleNamespace(**{name: value for module_name in sorted(sys.modules)
                                 if module_name.split(".")[0] == "io_tribaltrouble"
                                 for name, value in vars(sys.modules[module_name]).items()
                                 if not name.startswith("__")})
wm = bpy.context.window_manager
results = []


def test(fn):
    try:
        results.append((fn.__name__, "PASS", str(fn() or "")))
    except Exception:
        results.append((fn.__name__, "FAIL", " | ".join(traceback.format_exc().strip().splitlines()[-3:])))
    return fn


def arm():
    return next(o for o in bpy.data.objects if o.type == "ARMATURE" and o.get(addon.BROWSER_TAG))


def load(group, sprite):
    assert bpy.ops.wm.tt_load_unit(group=group, sprite=sprite) == {"FINISHED"}
    bpy.context.view_layer.objects.active = arm()
    return arm()


def raise_errors(kind, message):
    if "ERROR" in kind:
        raise RuntimeError(message)


def save_items():
    return {"FINISHED"} if addon.publish_items(bpy.context, arm(), raise_errors) is not None else {"CANCELLED"}


def save_clip(clip_name, kind, wpc):
    a = arm()
    return {"FINISHED"} if addon.publish_clip(bpy.context, a, a.animation_data.action, clip_name, kind, wpc,
                                              raise_errors) else {"CANCELLED"}


def entry(group, name):
    return next((s for s in addon.read_registry(TEMP) if s["group"] == group and s["name"] == name), None)


def items():
    return {slot: sorted((o["tt_sprite"], not o.hide_get()) for o in objs)
            for slot, objs in addon.unit_items(arm()).items()}


def select_only(obj):
    for o in bpy.context.selected_objects:
        o.select_set(False)
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj


def fixture_image(name, size=64):
    image = bpy.data.images.new(name, size, size)  # lives in memory only until the addon exports it
    image.pixels = [1.0, 0.5, 0.0, 1.0] * (size * size)
    return image


def fixture_mesh(name, image, z=1.7, kind="cone"):
    if kind == "cone":
        bpy.ops.mesh.primitive_cone_add(radius1=0.2, depth=0.3, location=(0, 0, z))
    else:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, z))
    obj = bpy.context.active_object
    obj.name = name
    if image is not None:
        material = bpy.data.materials.new(name + "_mat")
        material.use_nodes = True
        material.node_tree.nodes.new("ShaderNodeTexImage").image = image
        obj.data.materials.append(material)
    return obj


def put_on_head(obj):
    next(s for s in arm().tt_attachments if s.point == "HEAD").obj = obj
    bpy.context.view_layer.objects.active = arm()


def expect_error(call, fragment):
    try:
        call()
    except RuntimeError as e:
        assert fragment in str(e), e
        return
    raise AssertionError(f"expected an error containing: {fragment}")


@test
def repo_folder_and_model_list():
    wm.tt_repo_root = os.path.join(TEMP, "assets")  # pointing one level too deep must still resolve
    assert addon.repo_root(bpy.context) == TEMP
    units = [u.name for u in wm.tt_units]
    assert "vikings / warrior" in units and "vikings / warrior_axe_held" not in units
    wm.tt_category = "ALL"
    everything = len(wm.tt_units)
    wm.tt_category = "UNITS"
    assert everything > len(units)
    return f"{len(units)} units, {everything} models"


@test
def the_category_picks_which_kind_of_model_is_listed():
    listed = {}
    for category, _, _ in addon.CATEGORY_ITEMS:
        wm.tt_category = category
        listed[category] = {u.name for u in wm.tt_units}
    wm.tt_category = "UNITS"
    expected = {
        "UNITS": ["vikings / warrior", "natives / peon", "vikings / chieftain", "misc / chicken"],
        "BUILDINGS": ["vikings / quarters", "vikings / quarters_halfbuilt", "natives / ship_start", "natives / tower"],
        "RESOURCES": ["misc / rock_1", "misc / wood_2", "misc / treasure_5"],
        "NATURE": ["misc / oak_tree_crown", "misc / palm_trunk", "misc / plant_1", "misc / viking_plant_4"],
        "OTHER": ["vikings / rally_point", "vikings / axe", "natives / spear", "misc / icon"],
    }
    for category, names in expected.items():
        for name in names:
            others = [c for c, found in listed.items() if name in found and c not in (category, "ALL")]
            assert name in listed[category] and not others, (name, category, others)
    assert "natives / rock_resource" not in listed["ALL"], "a carried item was listed as a model"
    assert sum(len(v) for c, v in listed.items() if c != "ALL") == len(listed["ALL"])
    return ", ".join(f"{c.lower()} {len(v)}" for c, v in listed.items())


@test
def load_unit_with_default_attachment():
    a = load("vikings", "warrior")
    assert items() == {"weapon": [("warrior_axe_held", True)]}, items()
    assert a.animation_data.action.name.endswith("idle")
    return f"{len(a.data.bones)} bones, {len(addon.armature_actions(a))} clips, axe on by default"


@test
def publishing_leaves_untouched_registry_items_alone():
    a = load("vikings", "peon")
    rubber = next(o for o in addon.unit_items(a)[addon.CARRY_SLOT] if o["tt_sprite"] == "rubber_resource")
    assert bpy.ops.object.tt_show_item(item=rubber.name) == {"FINISHED"}
    os.utime(rubber["tt_source"], (1, 1))
    assert bpy.ops.export_mesh.tt_to_repo() == {"FINISHED"}
    assert os.path.getmtime(rubber["tt_source"]) == 1, "an unchanged item was written again"
    rubber.location.z += 0.5
    assert bpy.ops.export_mesh.tt_to_repo() == {"FINISHED"}
    assert os.path.getmtime(rubber["tt_source"]) != 1, "a moved item was not written"


@test
def a_new_item_never_overwrites_a_file_already_in_the_unit_folder():
    load("vikings", "warrior")
    path = os.path.join(GEOMETRY, "vikings", "warrior", "warrior_skeleton.xml")
    before = open(path, "rb").read()
    put_on_head(fixture_mesh("warrior_skeleton", fixture_image("clash_tex")))
    expect_error(save_items, "already has a file named after warrior_skeleton")
    assert open(path, "rb").read() == before
    obj = bpy.data.objects["warrior_skeleton"]
    next(s for s in arm().tt_attachments if s.point == "HEAD").obj = None
    assert "tt_bone" not in obj, "a mesh taken out of its slot still exports skinned to the bone"
    bpy.data.objects.remove(obj)


@test
def a_mirrored_mesh_keeps_its_faces_pointing_out():
    cube = fixture_mesh("test_mirror", None, 0, "cube")
    cube.scale = (-1.0, 1.0, 1.0)
    bpy.context.view_layer.update()
    record = addon.mesh_record_from_mesh(cube.data, cube, cube.matrix_world, False, None, False)
    for i, (a, b, c) in enumerate(record.faces):
        pa, pb, pc = (addon.Vector(record.verts[v]) for v in (a, b, c))
        winding = (pb - pa).cross(pc - pa)
        assert winding.dot(addon.Vector(record.loop_normals[3 * i])) > 0, f"face {i} is inside out"
    bpy.data.objects.remove(cube)


@test
def loading_another_unit_replaces_the_first_but_keeps_user_objects():
    keep = fixture_mesh("my_own_cube", None, kind="cube")
    load("natives", "warrior")
    names = [o.name for o in bpy.data.objects]
    assert "my_own_cube" in names and not any(n.startswith(("warrior_mesh", "warrior_axe")) for n in names), names
    bpy.data.objects.remove(keep)


@test
def clip_buttons_switch_the_action_and_fit_the_timeline():
    a = load("vikings", "warrior")
    actions = sorted(addon.armature_actions(a), key=lambda x: x.name)
    labels = addon.short_labels([x.name for x in actions])
    assert "run" in labels and "idle" in labels, labels
    assert not any("." in label for label in labels), f"clips from an earlier load were left behind: {labels}"
    run = actions[labels.index("run")]
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    assert a.animation_data.action == run
    assert bpy.context.scene.frame_end == int(round(run.frame_range[1]))
    return f"labels {labels}"


@test
def tier_buttons_swap_the_texture_on_the_unit_and_its_attachments():
    a = load("vikings", "warrior")
    assert addon.short_labels(addon.unit_tiers(a)) == ["rock", "iron", "rubber"], addon.unit_tiers(a)
    assert bpy.ops.object.tt_set_tier(index=1) == {"FINISHED"}
    shown = {o.name: o.data.materials[0].name for o in addon.unit_meshes(a)}
    assert set(shown.values()) == {"tt_viking_warrior_iron"}, shown
    return str(shown)


@test
def team_color_preview_blends_through_the_decal():
    load("vikings", "warrior")
    material = bpy.data.materials["tt_viking_warrior_rock"]
    nodes = material.node_tree.nodes
    assert addon.TEAM_MIX in nodes and addon.TEAM_DECAL in nodes, "team nodes missing although the decal exists"
    base_color = nodes["Principled BSDF"].inputs["Base Color"]
    assert base_color.links[0].from_node.type == "TEX_IMAGE", "preview must start switched off"
    wm.tt_team_color = (0.0, 0.0, 1.0)
    wm.tt_team_preview = True
    assert base_color.links[0].from_node.name == addon.TEAM_MIX
    assert tuple(round(c, 3) for c in nodes[addon.TEAM_COLOR].outputs[0].default_value[:3]) == (0.0, 0.0, 1.0)
    mix = nodes[addon.TEAM_MIX]
    assert mix.inputs[addon.MIX_A].links[0].from_node.type == "TEX_IMAGE"
    assert mix.inputs[addon.MIX_B].links[0].from_node.name == addon.TEAM_COLOR
    assert mix.inputs[addon.MIX_FACTOR].links[0].from_node.name == addon.TEAM_DECAL
    wm.tt_team_preview = False
    assert base_color.links[0].from_node.type == "TEX_IMAGE"


@test
def new_hat_exports_with_its_texture_and_registers():
    load("natives", "warrior")
    put_on_head(fixture_mesh("pumpkin", fixture_image("pumpkin_tex")))
    assert bpy.ops.object.tt_preflight() == {"FINISHED"}
    levels = [c.level for c in wm.tt_checks]
    assert "ERROR" not in levels and "INFO" in levels, [(c.level, c.name) for c in wm.tt_checks]
    assert bpy.ops.export_mesh.tt_to_repo() == {"FINISHED"}
    mesh = ET.parse(os.path.join(GEOMETRY, "natives", "warrior", "pumpkin.xml")).getroot()
    assert mesh.get("texture") == "pumpkin_tex"
    assert {s.get("bone") for s in mesh.iter("skin")} == {"Head"}
    assert os.path.getsize(os.path.join(MODELS, "pumpkin_tex.png")) > 0
    assert bpy.data.images["pumpkin_tex"].filepath_raw == "", "the image must not be left pointing into the repo"
    assert bpy.ops.object.tt_add_to_registry() == {"FINISHED"}
    e = entry("natives", "warrior_pumpkin")
    assert e["slot"] == "hat" and e["base"] == "warrior" and e["models"] == ["natives/warrior/pumpkin.xml"], e
    text = open(registry_path, encoding="utf-8").read()
    assert '<texture name="pumpkin_tex"/>' in text, "no decal file, so no team attribute"
    assert addon.append_registry_entries(registry_path, "natives", addon.registry_entries(bpy.context, arm(), "warrior")) == 0


@test
def repainting_updates_the_png_in_the_repo():
    png = os.path.join(MODELS, "pumpkin_tex.png")
    before = open(png, "rb").read()
    image = bpy.data.images["pumpkin_tex"]
    image.pixels = [0.1, 0.8, 0.1, 1.0] * (64 * 64)
    image.update()
    bpy.context.view_layer.objects.active = arm()
    assert bpy.ops.export_mesh.tt_to_repo() == {"FINISHED"}
    assert open(png, "rb").read() != before


@test
def two_hats_share_a_slot_and_show_one_at_a_time():
    put_on_head(fixture_mesh("witch", fixture_image("witch_tex")))
    assert bpy.ops.export_mesh.tt_to_repo() == {"FINISHED"}
    assert bpy.ops.object.tt_add_to_registry() == {"FINISHED"}
    load("natives", "warrior")
    assert items()["hat"] == [("warrior_pumpkin", False), ("warrior_witch", False)], items()
    by_sprite = {o["tt_sprite"]: o for o in addon.unit_items(arm())["hat"]}
    assert by_sprite["warrior_pumpkin"].parent_bone == "Head"
    bpy.ops.object.tt_show_item(item=by_sprite["warrior_pumpkin"].name)
    bpy.ops.object.tt_show_item(item=by_sprite["warrior_witch"].name)
    assert items()["hat"] == [("warrior_pumpkin", False), ("warrior_witch", True)], items()
    bpy.ops.object.tt_show_item(item=by_sprite["warrior_witch"].name)
    assert items()["hat"] == [("warrior_pumpkin", False), ("warrior_witch", False)], items()


@test
def checks_catch_what_breaks_in_game_and_block_the_export():
    load("natives", "warrior")
    bad = fixture_mesh("my hat", None)
    while bad.data.uv_layers:
        bad.data.uv_layers.remove(bad.data.uv_layers[0])
    bad.scale = (-1.0, 1.0, 1.0)
    colors = bad.data.color_attributes.new(name="Col", type="FLOAT_COLOR", domain="CORNER")
    for c in colors.data:
        c.color = (0.5, 0.5, 0.5, 1.0)
    bpy.context.view_layer.update()
    put_on_head(bad)
    bpy.context.view_layer.update()
    expect_error(bpy.ops.object.tt_preflight, "problem(s) to fix")
    text = " / ".join(c.name for c in wm.tt_checks)
    for fragment in ("letters, digits and underscores", "no UV map", "no Image Texture", "tint"):
        assert fragment in text, f"missing '{fragment}' in: {text}"
    expect_error(bpy.ops.export_mesh.tt_to_repo, "Not published")
    assert not os.path.exists(os.path.join(GEOMETRY, "natives", "warrior", "my hat.xml"))
    odd = fixture_mesh("odd", fixture_image("odd_tex", 100))
    found = [text for _, text in addon.check_mesh(odd, True, 0)]
    assert any("square power of two" in t for t in found), found
    bpy.data.objects.remove(bad)
    bpy.data.objects.remove(odd)
    return text


@test
def remove_from_registry_keeps_files_and_protects_bases():
    assert bpy.ops.object.tt_remove_from_registry(group="natives", sprite="warrior_witch") == {"FINISHED"}
    assert entry("natives", "warrior_witch") is None and entry("natives", "warrior_pumpkin") is not None
    assert os.path.isfile(os.path.join(GEOMETRY, "natives", "warrior", "witch.xml")), "files must stay on disk"
    expect_error(lambda: bpy.ops.object.tt_remove_from_registry(group="natives", sprite="warrior"), "is the base of")
    expect_error(lambda: bpy.ops.object.tt_remove_from_registry(group="natives", sprite="nope"), "No sprite named")


@test
def register_a_building_with_all_three_stages():
    addon.clear_browser_objects()
    hi, lo = fixture_image("test_hut_hi"), fixture_image("test_hut_lo", 32)
    built = fixture_mesh("hut", hi, 0, "cube")
    far = fixture_mesh("hut_far", lo, 0, "cube")
    half = fixture_mesh("hut_half", hi, 3, "cube")
    site = fixture_mesh("hut_site", hi, 6, "cube")
    select_only(built)
    assert bpy.ops.object.tt_register_model(mesh=built.name, sprite_name="test_hut", group="vikings", low_detail=far.name,
                                            half_built=half.name, start=site.name) == {"FINISHED"}
    assert entry("vikings", "test_hut")["models"] == ["vikings/test_hut/test_hut.xml", "vikings/test_hut/test_hut_lo.xml"]
    assert entry("vikings", "test_hut_halfbuilt")["models"] == ["vikings/test_hut/test_hut_halfbuilt.xml"]
    assert entry("vikings", "test_hut_start")["models"] == ["vikings/test_hut/test_hut_start.xml"]
    assert all(os.path.isfile(os.path.join(MODELS, t + ".png")) for t in ("test_hut_hi", "test_hut_lo"))
    select_only(built)
    expect_error(lambda: bpy.ops.object.tt_register_model(mesh=built.name, sprite_name="test_hut", group="vikings"), "already has")
    expect_error(lambda: bpy.ops.object.tt_register_model(mesh=built.name, sprite_name="bad name", group="vikings"), "letters")
    wm.tt_category = "BUILDINGS"
    assert "vikings / test_hut_start" in [u.name for u in wm.tt_units]
    wm.tt_category = "UNITS"
    assert bpy.ops.wm.tt_load_unit(group="vikings", sprite="test_hut") == {"FINISHED"}
    for o in (built, far, half, site):
        bpy.data.objects.remove(o)


@test
def new_decoration_writes_its_terrain_count_and_event():
    addon.clear_browser_objects()
    patch = fixture_mesh("pumpkin_patch", fixture_image("test_patch_tex"), 0, "cube")
    plain = fixture_mesh("test_stones", fixture_image("test_stones_tex"), 3, "cube")
    assert bpy.ops.object.tt_new_event(event_name=" Halloween") == {"FINISHED"}
    assert bpy.ops.object.tt_register_model(mesh=patch.name, sprite_name="test_patch", scatter=True, grass=True,
                                            dirt=True, beach=False, snow=False, land=False, count=12,
                                            event="halloween") == {"FINISHED"}
    assert bpy.ops.object.tt_register_model(mesh=plain.name, sprite_name="test_stones", scatter=True,
                                            land=True) == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="test_patch" decoration="grass,dirt" count="12" event="halloween">' in text
    assert '<sprite name="test_stones" decoration="land" count="20">' in text
    assert entry("misc", "test_patch")["models"] == ["misc/test_patch/test_patch.xml"]
    assert os.path.isfile(os.path.join(MODELS, "test_patch_tex.png"))
    expect_error(lambda: bpy.ops.object.tt_register_model(mesh=patch.name, sprite_name="test_patch", scatter=True),
                 "already has")
    expect_error(lambda: bpy.ops.object.tt_new_event(event_name="bad name"), "letters")
    assert "kind" not in bpy.ops.object.tt_register_model.get_rna_type().properties.keys()
    wm.tt_category = "DECORATIONS"
    assert [u.name for u in wm.tt_units] == ["misc / test_patch", "misc / test_stones"], [u.name for u in wm.tt_units]
    wm.tt_category = "UNITS"
    for o in (patch, plain):
        bpy.data.objects.remove(o)


@test
def register_a_unit_on_an_existing_rig():
    load("natives", "warrior")
    body = next(o for o in addon.unit_meshes(arm()) if not o.get("tt_slot"))
    variant = body.copy()
    variant.data = body.data.copy()
    variant.name = "variant"
    del variant[addon.BROWSER_TAG]
    bpy.context.collection.objects.link(variant)
    select_only(variant)
    assert bpy.ops.object.tt_register_model(mesh=variant.name, sprite_name="warrior_variant") == {"FINISHED"}
    e = entry("natives", "warrior_variant")
    assert e["base"] == "warrior" and not e["skeleton"] and not e["slot"], e
    assert "natives / warrior_variant" in [u.name for u in wm.tt_units], "a unit on a borrowed rig is still a unit"
    a = load("natives", "warrior_variant")
    assert len(a.data.bones) == 30 and len(addon.armature_actions(a)) > 0
    bpy.data.objects.remove(variant)


@test
def register_a_unit_with_its_own_rig():
    addon.clear_browser_objects()
    src = os.path.join(GEOMETRY, "vikings", "peon")
    mesh = addon.import_mesh_file(bpy.context, os.path.join(src, "peon_mesh.xml"), False, False, lambda k, m: None)
    mesh.name = "goblin"
    mesh["tt_texture"] = "native_warrior_rock"
    parents, rest = addon.read_skeleton(os.path.join(src, "peon_skeleton.xml"))
    rig = addon.build_armature(bpy.context, "goblin", parents, rest)  # no tt_skeleton: a rig the registry never saw
    addon.bind_meshes(rig, [mesh])
    for clip in ("run", "attack"):
        addon.apply_clip(bpy.context, rig, "goblin_" + clip, addon.read_animation(os.path.join(src, f"peon_{clip}.xml")))
    select_only(mesh)
    assert bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="goblin", group="misc") == {"FINISHED"}
    e = entry("misc", "goblin")
    assert e["skeleton"] == "misc/goblin/goblin_skeleton.xml", e
    assert sorted(e["clips"]) == ["misc/goblin/goblin_attack.xml", "misc/goblin/goblin_run.xml"], e
    text = open(registry_path, encoding="utf-8").read()
    assert '<animation name="run" wpc="1" type="loop">' in text and '<animation name="attack" wpc="1" type="plain">' in text
    a = load("misc", "goblin")
    assert len(a.data.bones) == 28


@test
def update_button_appears_only_for_a_newer_repo_copy():
    assert addon.update_available(bpy.context) is None, "the copy under test IS the repo copy"
    source = os.path.join(ADDON_DIR, "__init__.py")
    assert addon.version_from_source(source) == tuple(addon.bl_info["version"])
    repo_copy = os.path.join(TEMP, "tools", "blender", "io_tribaltrouble")
    shutil.copytree(ADDON_DIR, repo_copy, ignore=shutil.ignore_patterns("__pycache__"))
    text = open(source, encoding="utf-8").read()
    newer = re.sub(r'"version": \(\d+, \d+, \d+\)', '"version": (99, 0, 0)', text, count=1)
    open(os.path.join(repo_copy, "__init__.py"), "w", encoding="utf-8").write(newer)
    open(os.path.join(repo_copy, "new_module.py"), "w", encoding="utf-8").write("")
    addons = os.path.join(TEMP, "addons")
    installed = os.path.join(addons, "io_tribaltrouble")
    shutil.copytree(ADDON_DIR, installed, ignore=shutil.ignore_patterns("__pycache__"))
    old_single_file = os.path.join(addons, "io_tribaltrouble.py")
    shutil.copy(source, old_single_file)
    update = sys.modules[addon.update_available.__module__]
    real_dir, update.ADDON_DIR = update.ADDON_DIR, installed
    try:
        assert addon.update_available(bpy.context) == (99, 0, 0)
        assert addon.install_repo_addon(bpy.context) == (99, 0, 0)
        installed_version = addon.version_from_source(os.path.join(installed, "__init__.py"))
        assert installed_version == (99, 0, 0), "the installed add-on was not replaced"
        assert os.path.isfile(os.path.join(installed, "new_module.py")), "every module of the add-on is copied"
        assert not os.path.exists(old_single_file), "Blender cannot load the old single-file add-on next to the package"
        open(os.path.join(repo_copy, "__init__.py"), "w", encoding="utf-8").write(text)
        os.utime(os.path.join(repo_copy, "__init__.py"), (1, 1))
        assert addon.update_available(bpy.context) is None, "an equal or older repo copy must not offer an update"
    finally:
        update.ADDON_DIR = real_dir


def load_building(group, sprite):
    wm.tt_category = "BUILDINGS"
    assert bpy.ops.wm.tt_load_unit(group=group, sprite=sprite) == {"FINISHED"}
    return addon.prop_body(bpy.context)


@test
def carried_items_load_on_the_peon_and_leave_the_model_list():
    wm.tt_category = "UNITS"
    addon.refresh_units(bpy.context)
    units = [u.name for u in wm.tt_units]
    assert "natives / peon" in units and "natives / rubber_resource" not in units, units
    load("natives", "peon")
    carried = items()[addon.CARRY_SLOT]
    assert [name for name, _ in carried] == ["left_paddle", "right_paddle", "rock_resource", "rubber_resource",
                                             "wood_resource"], carried
    assert not any(shown for _, shown in carried), "carried items start hidden"
    rubber = next(o for o in addon.unit_items(arm())[addon.CARRY_SLOT] if o["tt_sprite"] == "rubber_resource")
    assert bpy.ops.object.tt_show_item(item=rubber.name) == {"FINISHED"}
    assert [name for name, shown in items()[addon.CARRY_SLOT] if shown] == ["rubber_resource"]
    wood = next(o for o in addon.unit_items(arm())[addon.CARRY_SLOT] if o["tt_sprite"] == "wood_resource")
    assert bpy.ops.object.tt_show_item(item=wood.name) == {"FINISHED"}
    assert [name for name, shown in items()[addon.CARRY_SLOT] if shown] == ["rubber_resource", "wood_resource"]
    assert bpy.ops.object.tt_show_item(item=wood.name) == {"FINISHED"}
    assert [name for name, shown in items()[addon.CARRY_SLOT] if shown] == ["rubber_resource"]
    assert rubber.parent == arm() and rubber.parent_bone == "peon Ponytail1", (rubber.parent_type, rubber.parent_bone)
    source = os.path.join(GEOMETRY, "misc", "rubber_fragment_native.xml")
    assert bpy.ops.export_mesh.tt_to_repo() == {"FINISHED"}
    skins = set(re.findall(r'<skin bone="([^"]+)"', open(source).read()))
    assert skins == {"peon Ponytail1"}, skins
    assert entry("natives", "rubber_resource") is not None


@test
def one_press_saves_a_new_item_and_turns_it_into_a_button():
    a = load("natives", "peon")
    crown = fixture_mesh("test_crown", fixture_image("test_crown_tex"))
    put_on_head(crown)
    assert save_items() == {"FINISHED"}
    assert not any("Add To Registry" in check.name for check in wm.tt_checks), [check.name for check in wm.tt_checks]
    e = entry("natives", "peon_test_crown")
    assert e is not None and e["base"] == "peon" and e["slot"] == "hat", e
    assert os.path.isfile(os.path.join(GEOMETRY, "natives", "peon", "test_crown.xml"))
    assert os.path.isfile(os.path.join(MODELS, "test_crown_tex.png"))
    assert all(slot.obj is None for slot in a.tt_attachments), "the row was not handed over"
    assert crown.parent == a and crown.parent_bone == "peon Head", "the saved item stopped following the head"
    assert ("peon_test_crown", True) in items()["hat"], items()
    second = fixture_mesh("test_crown", fixture_image("test_crown_tex_b"))
    second.name = "test_crown_b"
    put_on_head(second)
    assert save_items() == {"FINISHED"}
    assert sorted(items()["hat"]) == [("peon_test_crown", False), ("peon_test_crown_b", True)], items()["hat"]
    a = load("natives", "peon")
    assert [name for name, _ in items()["hat"]] == ["peon_test_crown", "peon_test_crown_b"], "lost on reload"
    bare = fixture_mesh("test_bare_hat", None)
    put_on_head(bare)
    expect_error(save_items, "problem(s)")
    assert entry("natives", "peon_test_bare_hat") is None, "a refused item still reached the registry"
    bpy.data.objects.remove(bare)


@test
def a_new_item_can_belong_to_an_event_and_be_on_by_default():
    a = load("natives", "peon")
    pumpkin = fixture_mesh("test_pumpkin_hat", fixture_image("test_pumpkin_hat_tex"))
    assert bpy.ops.object.tt_new_event(event_name="halloween") == {"FINISHED"}
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_item(point="HEAD", mesh=pumpkin.name, event="halloween",
                                      on_by_default=True) == {"FINISHED"}
    assert save_items() == {"FINISHED"}
    put_on_head(fixture_mesh("test_plain_hat", fixture_image("test_plain_hat_tex")))
    assert save_items() == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="peon_test_pumpkin_hat" base="peon" slot="hat" event="halloween" default="true">' in text
    assert '<sprite name="peon_test_plain_hat" base="peon" slot="hat">' in text
    assert not entry("natives", "peon_test_plain_hat")["default"] and entry("natives", "peon_test_plain_hat")["event"] == ""


@test
def put_it_there_places_and_shrinks_an_oversized_mesh():
    a = load("natives", "peon")
    height = addon.unit_height(a)
    assert 1.5 < height < 3.0, height
    bpy.ops.mesh.primitive_monkey_add(size=10.0, location=(7.0, -4.0, 0.0))
    monkey = bpy.context.active_object
    monkey.name = "test_monkey"
    put_on_head(monkey)
    warnings = [text for level, text in addon.preflight(bpy.context, a) if level == "WARNING"]
    assert any("bigger than the unit" in text for text in warnings), warnings
    assert bpy.ops.object.tt_put_on_bone(point="HEAD") == {"FINISHED"}
    low, high = addon.world_box(monkey)
    assert abs(max(high - low) - addon.FIT_SHARE * height) < 0.02, (max(high - low), height)
    top = addon.top_of_part(bpy.context, a, "peon Head")
    body_top = addon.world_box(addon.browsed_unit(a))[1].z
    joint = (a.matrix_world @ a.pose.bones["peon Head"].head).z  # the native peon's tall hair is not the head
    assert joint < top.z <= body_top, (joint, top.z, body_top)
    assert abs((low.x + high.x) / 2 - top.x) < 0.01 and abs((low.y + high.y) / 2 - top.y) < 0.01
    assert abs(low.z - top.z) < 0.01, (low.z, top.z)
    assert monkey.parent == a and monkey.parent_bone == "peon Head"
    before = addon.world_box(monkey)[0].copy()
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    bpy.context.scene.frame_set(5)
    bpy.context.view_layer.update()
    assert (addon.world_box(monkey)[0] - before).length > 0.01, "it stopped following the head"
    small = fixture_mesh("test_grip", fixture_image("test_grip_tex"), kind="cube")
    small.scale = (0.1, 0.1, 0.1)
    next(s for s in a.tt_attachments if s.point == "HAND_R").obj = small
    bpy.context.view_layer.objects.active = a
    bpy.context.view_layer.update()
    size_before = max(addon.world_box(small)[1] - addon.world_box(small)[0])
    assert bpy.ops.object.tt_put_on_bone(point="HAND_R") == {"FINISHED"}
    hand = a.matrix_world @ a.pose.bones[next(s for s in a.tt_attachments if s.point == "HAND_R").bone].head
    assert (small.matrix_world.translation - hand).length < 0.01
    size_after = max(addon.world_box(small)[1] - addon.world_box(small)[0])
    assert abs(size_after - size_before) < 1e-4, ("a small item was rescaled", size_before, size_after)
    for s in a.tt_attachments:
        s.obj = None
    bpy.data.objects.remove(monkey)
    bpy.data.objects.remove(small)


@test
def one_button_gives_a_bare_mesh_a_texture_the_game_accepts():
    a = load("vikings", "warrior")
    horn = fixture_mesh("My Horn", None)
    horn.data.uv_layers.remove(horn.data.uv_layers[0])
    plain = bpy.data.materials.new("plain_red")
    plain.use_nodes = True
    next(n for n in plain.node_tree.nodes if n.type == "BSDF_PRINCIPLED").inputs["Base Color"].default_value = (0.8, 0.1, 0.1, 1.0)
    horn.data.materials.append(plain)
    assert any(level == "ERROR" for level, _ in addon.check_mesh(horn, True, 0))
    assert bpy.ops.object.tt_make_texture(target=horn.name, size="128") == {"FINISHED"}
    assert horn.name == "my_horn", horn.name
    image = addon.mesh_texture_image(horn)
    assert image is not None and image.name == "my_horn" and tuple(image.size) == (128, 128), (image.name, tuple(image.size))
    assert abs(image.pixels[0] - 0.8) < 0.02 and abs(image.pixels[1] - 0.1) < 0.02, list(image.pixels[:4])
    assert len(horn.data.uv_layers) == 1
    assert not [text for level, text in addon.check_mesh(horn, True, 0) if level == "ERROR"]
    put_on_head(horn)
    assert save_items() == {"FINISHED"}
    assert os.path.isfile(os.path.join(MODELS, "my_horn.png"))
    assert entry("vikings", "warrior_my_horn")["slot"] == "hat"
    expect_cancel = bpy.ops.object.tt_make_texture(target=horn.name, size="128")
    assert expect_cancel == {"CANCELLED"}, "a mesh that has a texture got a second one"
    assert bpy.ops.object.tt_paint_item(target=horn.name) == {"FINISHED"}
    assert bpy.context.mode == "PAINT_TEXTURE" and bpy.context.active_object == horn
    assert bpy.context.scene.tool_settings.image_paint.canvas == image
    assert bpy.ops.object.tt_done_painting() == {"FINISHED"}
    assert bpy.context.mode == "OBJECT"
    assert addon.active_armature(bpy.context) == a, "the unit's panels would vanish after painting"


@test
def the_new_item_form_attaches_snaps_and_makes_a_texture_when_asked():
    a = load("natives", "peon")
    hat = fixture_mesh("test_form_hat", None, z=6.0)
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_item(point="HEAD", mesh=hat.name) == {"FINISHED"}
    head = next(s for s in a.tt_attachments if s.point == "HEAD")
    assert head.obj == hat and hat.parent == a and hat.parent_bone == head.bone
    low, _ = addon.world_box(hat)
    assert abs(low.z - addon.top_of_part(bpy.context, a, head.bone).z) < 0.01, "it was not snapped"
    assert addon.mesh_texture_image(hat) is not None, "no texture was made"
    stick = fixture_mesh("test_form_stick", None, z=6.0, kind="cube")
    before = stick.matrix_world.translation.copy()
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_item(point="HAND_R", mesh=stick.name, snap=False, make_texture=False) == {"FINISHED"}
    hand = next(s for s in a.tt_attachments if s.point == "HAND_R")
    assert hand.obj == stick and stick.parent_bone == hand.bone
    assert (stick.matrix_world.translation - before).length < 1e-4, "it moved without Snap"
    assert addon.mesh_texture_image(stick) is None, "a texture was made without asking"
    rubber = next(o for o in addon.unit_items(a)[addon.CARRY_SLOT] if o["tt_sprite"] == "rubber_resource")
    expect_error(lambda: bpy.ops.object.tt_new_item(point="HEAD", mesh=rubber.name), "own meshes")
    assert head.obj == hat
    for s in a.tt_attachments:
        s.obj = None
    bpy.data.objects.remove(hat)
    bpy.data.objects.remove(stick)


@test
def paint_it_splits_the_largest_3d_view_only_when_no_image_view_shows():
    area = lambda kind, w, h: type("Area", (), {"type": kind, "width": w, "height": h})()
    small, big = area("VIEW_3D", 400, 300), area("VIEW_3D", 1600, 900)
    assert addon.area_to_split([area("OUTLINER", 2000, 2000), small, big]) is big
    assert addon.area_to_split([big, area("IMAGE_EDITOR", 10, 10)]) is None
    assert addon.area_to_split([area("PROPERTIES", 300, 900)]) is None


@test
def the_items_list_filters_by_slot_and_text_and_holds_a_hundred():
    a = load("natives", "peon")
    template = next(o for o in addon.unit_items(a)[addon.CARRY_SLOT] if o["tt_sprite"] == "wood_resource")
    existing = sum(len(v) for v in addon.unit_items(a).values())
    made = []
    for i in range(100):
        hat = template.copy()
        hat["tt_slot"], hat["tt_sprite"] = "hat", f"peon_hat_{i:03d}"
        bpy.context.collection.objects.link(hat)
        made.append(hat)
    objects = list(bpy.data.objects)
    shown, order = addon.item_rows(objects, a, "")
    assert sum(shown) == existing + 100, (sum(shown), existing)
    listed = [objects[i]["tt_sprite"] for i in sorted(range(len(objects)), key=lambda i: order[i]) if shown[i]]
    assert listed[:5] == ["left_paddle", "right_paddle", "rock_resource", "rubber_resource", "wood_resource"], listed[:5]
    hats = [name for name in listed if name.startswith("peon_hat_")]
    assert hats == sorted(hats) and len(hats) == 100
    shown, _ = addon.item_rows(objects, a, "hat_04")
    assert sorted(objects[i]["tt_sprite"] for i in range(len(objects)) if shown[i]) == [f"peon_hat_04{d}" for d in range(10)]
    shown, _ = addon.item_rows(objects, a, "carried")
    assert sum(shown) == 5
    assert not any(addon.item_rows(objects, None, "")[0]), "items listed with no unit active"
    for hat in made:
        bpy.data.objects.remove(hat)


@test
def the_search_field_above_the_items_list_filters_it():
    load("natives", "peon")
    fake = type("List", (), {"bitflag_filter_item": 1 << 30})()  # the list's own flag only exists while drawn
    wm.tt_item_search = "paddle"
    flags, _ = addon.TT_UL_items.filter_items(fake, bpy.context, bpy.data, "objects")
    listed = sorted(o["tt_sprite"] for o, flag in zip(bpy.data.objects, flags) if flag)
    wm.tt_item_search = ""
    assert listed == ["left_paddle", "right_paddle"], listed
    assert addon.VIEW3D_PT_tt_attachments.bl_label == "Props"


@test
def an_item_hidden_in_the_clip_showing_points_to_the_clips_it_shows_in():
    a = load("natives", "chieftain")
    item = lambda name: next(o for o in sum(addon.unit_items(a).values(), []) if o["tt_sprite"] == name)
    prop, always = item("chieftain_magic_pot"), item("chieftain_club")
    names = lambda clips: [addon.clip_short_name(a, x) for x in clips]
    every = sorted(addon.armature_actions(a), key=lambda x: x.name)
    assert names(addon.item_clips(a, prop)) == ["magic"], names(addon.item_clips(a, prop))
    assert addon.item_clips(a, always) == every and addon.item_hidden_here(a, always) is None
    idle = next(x for x in every if addon.clip_short_name(a, x) == "idle")
    assert bpy.ops.object.tt_set_clip(clip=idle.name) == {"FINISHED"}
    assert names(addon.item_hidden_here(a, prop)) == ["magic"]
    tip = addon.ShowItemClip.description(bpy.context, type("P", (), {"item": prop.name})())
    assert tip == "Hidden in idle. Only visible in: magic", tip
    assert bpy.ops.object.tt_show_item_clip(item=prop.name) == {"FINISHED"}
    assert names([a.animation_data.action]) == ["magic"] and addon.item_hidden_here(a, prop) is None
    assert bpy.context.scene.frame_end == int(round(a.animation_data.action.frame_range[1]))
    return f"clips {names(every)}"


@test
def point_rows_only_take_the_artists_own_meshes():
    a = load("natives", "peon")
    head = next(slot for slot in a.tt_attachments if slot.point == "HEAD")
    rubber = next(o for o in addon.unit_items(a)[addon.CARRY_SLOT] if o["tt_sprite"] == "rubber_resource")
    assert not addon.attachment_obj_poll(head, rubber), "a registry item could be picked into a point row"
    assert not addon.attachment_obj_poll(head, addon.browsed_unit(a)), "the unit's own body could be picked"
    own = fixture_mesh("own_hat", fixture_image("own_hat_tex"))
    assert addon.attachment_obj_poll(head, own)
    bpy.data.objects.remove(own)


def clip_lines(group, sprite):
    return list(entry(group, sprite)["clip_info"].items())


@test
def a_new_clip_is_saved_last_on_the_unit_and_on_everything_it_carries():
    a = load("vikings", "peon")
    before = clip_lines("vikings", "peon")
    assert bpy.ops.object.tt_set_clip(clip=next(x.name for x in addon.armature_actions(a) if x.name.endswith("idle"))) == {"FINISHED"}
    assert bpy.ops.object.tt_new_clip(clip_name="dance", start="COPY") == {"FINISHED"}
    action = a.animation_data.action
    assert action.name == "peon_dance" and "tt_clip" not in action, action.name
    expect_error(lambda: bpy.ops.object.tt_new_clip(clip_name="dance"), "already has a clip called dance")
    head = a.pose.bones["peon Head"]
    head.rotation_mode = "QUATERNION"
    head.rotation_quaternion = (0.9, 0.0, 0.0, 0.43)
    head.keyframe_insert("rotation_quaternion", frame=5)
    assert save_clip(clip_name="dance", kind="loop", wpc=1.0) == {"FINISHED"}
    path = os.path.join(GEOMETRY, "vikings", "peon", "peon_dance.xml")
    assert os.path.isfile(path)
    idle = ET.parse(os.path.join(GEOMETRY, "vikings", "peon", "peon_idle.xml")).getroot()
    dance = ET.parse(path).getroot()
    assert len(dance.findall("frame")) == len(idle.findall("frame"))
    assert len(dance.find("frame").findall("transform")) == len(idle.find("frame").findall("transform"))
    lines = clip_lines("vikings", "peon")
    assert lines[:-1] == before, "existing clip numbers moved"
    assert lines[-1] == ("dance", ("1", "loop", "vikings/peon/peon_dance.xml")), lines[-1]
    for sprite in ("rock_resource", "wood_resource", "rubber_resource", "left_paddle", "right_paddle"):
        e = entry("vikings", sprite)
        assert e["base"] == "peon" and e["slot"] == addon.CARRY_SLOT and not e["clip_info"], (sprite, e)
    assert "dance" not in entry("natives", "peon")["clip_info"], "the other race's peon was touched"
    a = load("vikings", "peon")
    assert "peon_dance" in [x.name for x in addon.armature_actions(a)], "the new clip has no button after a reload"


@test
def deleting_clips_unsaved_then_saved_and_only_from_the_end():
    a = load("vikings", "peon")
    assert "dance" in entry("vikings", "peon")["clip_info"], "needs the clip saved by the earlier test"
    assert bpy.ops.object.tt_new_clip(clip_name="scratch") == {"FINISHED"}
    assert bpy.ops.object.tt_delete_clip() == {"FINISHED"}
    assert "peon_scratch" not in bpy.data.actions and a.animation_data.action.name.endswith("idle")
    expect_error(lambda: bpy.ops.object.tt_delete_clip(clip="peon_run"), "Only the last clip (dance)")
    assert os.path.isfile(os.path.join(GEOMETRY, "vikings", "peon", "peon_run.xml"))
    assert bpy.ops.object.tt_delete_clip(clip="peon_dance") == {"FINISHED"}
    assert "dance" not in entry("vikings", "peon")["clip_info"]
    assert len(entry("vikings", "peon")["clip_info"]) == 8
    assert not os.path.exists(os.path.join(GEOMETRY, "vikings", "peon", "peon_dance.xml"))
    assert "peon_dance" not in bpy.data.actions


@test
def a_clip_started_from_a_pose_has_two_keys_and_keeps_the_pose():
    a = load("vikings", "warrior")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    bpy.context.scene.frame_set(6)
    bpy.context.view_layer.update()
    posed = {pb.name: pb.matrix.copy() for pb in a.pose.bones}
    assert bpy.ops.object.tt_new_clip(clip_name="wave", length=30) == {"FINISHED"}
    action = a.animation_data.action
    assert action.name.endswith("wave") and tuple(action.frame_range) == (1.0, 30.0), tuple(action.frame_range)
    assert bpy.context.scene.frame_end == 30
    for frame in (1, 15, 30):
        bpy.context.scene.frame_set(frame)
        bpy.context.view_layer.update()
        worst = max(abs(x - y) for pb in a.pose.bones for r1, r2 in zip(pb.matrix, posed[pb.name]) for x, y in zip(r1, r2))
        assert worst < 1e-4, (frame, worst)
    assert bpy.ops.object.tt_delete_clip() == {"FINISHED"}


@test
def an_unsaved_clip_goes_with_its_unit_and_never_onto_another_rig_of_the_same_name():
    load("vikings", "peon")
    assert bpy.ops.object.tt_new_clip(clip_name="scribble") == {"FINISHED"}
    a = load("natives", "peon")
    assert a.name == "peon" and "peon_scribble" not in bpy.data.actions, [x.name for x in addon.armature_actions(a)]


@test
def saving_an_existing_clip_replaces_it_in_place():
    a = load("vikings", "warrior")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    before = clip_lines("vikings", "warrior")
    wpc, kind, relative = dict(before)["run"]
    path = os.path.join(GEOMETRY, relative)
    frames = len(ET.parse(path).getroot().findall("frame"))
    os.remove(path)
    assert save_clip(clip_name="run", kind=kind, wpc=float(wpc)) == {"FINISHED"}
    assert len(ET.parse(path).getroot().findall("frame")) == frames
    assert clip_lines("vikings", "warrior") == before
    assert save_clip(clip_name="run", kind="loop", wpc=4.5) == {"FINISHED"}
    assert dict(clip_lines("vikings", "warrior"))["run"] == ("4.5", "loop", relative)
    assert [n for n, _ in clip_lines("vikings", "warrior")] == [n for n, _ in before], "clip order changed"
    assert save_clip(clip_name="run", kind=kind, wpc=float(wpc)) == {"FINISHED"}


@test
def a_unit_on_a_borrowed_rig_cannot_change_the_clips():
    load("natives", "warrior_variant")
    expect_error(lambda: save_clip(clip_name="wave", kind="loop", wpc=1.0), "borrows the warrior rig")


def publish_prop(obj, event="", make_texture=True):
    if event:
        assert bpy.ops.object.tt_new_event(event_name=event) == {"FINISHED"}
    return bpy.ops.object.tt_new_prop(mesh=obj.name, event=event.strip().lower() or "ALL_YEAR",
                                      make_texture=make_texture)


@test
def props_on_a_building_save_register_and_come_back_on_reload():
    body = load_building("vikings", "quarters")
    assert body is not None and body["tt_sprite"] == "quarters"
    flag = fixture_mesh("test_flag", fixture_image("test_flag_tex"), z=8.0, kind="cube")
    lantern = fixture_mesh("test_lantern", fixture_image("test_lantern_tex"), z=2.0, kind="cube")
    lantern.location.x = 4.0
    assert publish_prop(lantern, " Halloween") == {"FINISHED"}
    assert publish_prop(flag) == {"FINISHED"}
    seasonal, always = entry("vikings", "quarters_test_lantern"), entry("vikings", "quarters_test_flag")
    assert seasonal["base"] == "quarters" and seasonal["slot"] == "prop" and seasonal["event"] == "halloween"
    assert always["event"] == ""
    assert seasonal["models"][0] == "vikings/quarters/test_lantern.xml"
    assert os.path.isfile(os.path.join(GEOMETRY, "vikings", "quarters", "test_lantern.xml"))
    assert os.path.isfile(os.path.join(MODELS, "test_lantern_tex.png"))
    skins = set(re.findall(r'<skin bone="([^"]+)"', open(os.path.join(GEOMETRY, "vikings", "quarters", "test_lantern.xml")).read()))
    assert skins == {addon.STATIC_BONE}, skins
    body = load_building("vikings", "quarters")
    props = {o["tt_sprite"]: o for o in addon.building_props(body)}
    assert sorted(props) == ["quarters_test_flag", "quarters_test_lantern"], sorted(props)
    assert props["quarters_test_lantern"]["tt_event"] == "halloween"
    assert [addon.prop_tag(o) for o in addon.building_props(body)] == ["Built, All year", "Built, halloween"]
    xs = [v.co.x for v in props["quarters_test_lantern"].data.vertices]
    assert 3.4 < min(xs) and max(xs) < 4.6, "the prop did not keep its place next to the building"


@test
def a_prop_with_a_taken_name_or_no_texture_is_refused():
    body = load_building("vikings", "quarters")
    for o in addon.building_props(body):
        bpy.data.objects.remove(o)  # frees the object name; the registry entry stays
    expect_error(lambda: publish_prop(fixture_mesh("test_flag", fixture_image("test_flag_tex2"), kind="cube")),
                 "already has a sprite named quarters_test_flag")
    expect_error(lambda: publish_prop(fixture_mesh("test_bare", None, kind="cube"), make_texture=False),
                 "no Image Texture")


@test
def a_prop_picked_on_a_stage_is_published_on_that_stage_with_its_event():
    body = load_building("vikings", "quarters_halfbuilt")
    assert addon.building_stage(body["tt_sprite"]) == ("quarters", "Half built")
    assert addon.building_stage("quarters_start") == ("quarters", "Start")
    assert addon.building_props(body) == []
    registered = fixture_mesh("test_crane", fixture_image("test_crane_tex"), z=6.0, kind="cube")
    expect_error(lambda: publish_prop(body), "own meshes")
    expect_error(lambda: publish_prop(registered, "Bad Event"), "letters, digits")
    assert entry("vikings", "quarters_halfbuilt_test_crane") is None
    assert publish_prop(registered, "harvest") == {"FINISHED"}
    e = entry("vikings", "quarters_halfbuilt_test_crane")
    assert (e["base"], e["slot"], e["event"]) == ("quarters_halfbuilt", "prop", "harvest"), e
    assert [addon.prop_tag(o) for o in addon.building_props(body)] == ["Half built, harvest"]
    assert not addon.attachment_obj_poll(None, registered), "a published prop is still offered by the picker"


def polygons(path):
    return open(path).read().count("<polygon>")


def mtimes():
    return {os.path.join(d, f): os.stat(os.path.join(d, f)).st_mtime_ns for d, _, fs in os.walk(GEOMETRY) for f in fs}


@test
def a_unit_and_its_items_load_their_low_detail_mesh_hidden():
    a = load("vikings", "warrior")
    body = addon.browsed_unit(a)
    low = bpy.data.objects["warrior_mesh_lod1"]
    assert low["tt_source"].endswith("warrior_low_poly_mesh.xml") and low["tt_detail"] == 1
    assert low.parent == a and any(m.type == "ARMATURE" and m.object == a for m in low.modifiers)
    assert low.hide_get() and not body.hide_get()
    assert "tt_export_hash" in body and "tt_export_hash" in low
    axe_low = bpy.data.objects["warrior_axe_held_lod1"]
    assert axe_low["tt_source"].endswith("warrior_axe_held_lo.xml") and axe_low.hide_get()
    assert items()["weapon"] == [("warrior_axe_held", True)], items()


@test
def the_detail_toggle_swaps_which_mesh_shows():
    a = load("vikings", "warrior")
    body, low = addon.browsed_unit(a), bpy.data.objects["warrior_mesh_lod1"]
    axe, axe_low = bpy.data.objects["warrior_axe_held"], bpy.data.objects["warrior_axe_held_lod1"]
    assert addon.has_low_detail()
    wm.tt_detail = "LOW"
    assert body.hide_get() and not low.hide_get() and axe.hide_get() and not axe_low.hide_get()
    assert bpy.ops.object.tt_show_item(item=axe.name) == {"FINISHED"}
    assert axe.hide_get() and axe_low.hide_get(), "hiding an item in low detail left its low mesh showing"
    wm.tt_detail = "HIGH"
    assert not body.hide_get() and low.hide_get() and axe.hide_get() and axe_low.hide_get()


@test
def publishing_without_edits_writes_no_unit_mesh():
    load("vikings", "warrior")
    files = [bpy.data.objects[n]["tt_source"] for n in ("warrior_mesh", "warrior_mesh_lod1", "warrior_axe_held",
                                                        "warrior_axe_held_lod1")]
    for path in files:
        os.utime(path, (1, 1))
    assert save_items() == {"FINISHED"}
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert all(os.path.getmtime(path) == 1 for path in files), "an unchanged mesh was written again"


@test
def splitting_the_body_rewrites_both_detail_levels():
    a = load("vikings", "warrior")
    body, low = addon.browsed_unit(a), bpy.data.objects["warrior_mesh_lod1"]
    before = {o: polygons(o["tt_source"]) for o in (body, low)}
    wm.tt_detail = "LOW"
    for o in (body, low):
        select_only(o)
        assert bpy.ops.object.tt_split_by_bone(bone="warrior  Pelvis") == {"FINISHED"}
        assert bpy.context.active_object.get(addon.BROWSER_TAG) is None
    wm.tt_detail = "HIGH"
    assert body["tt_source"] and body.get(addon.BROWSER_TAG) and "tt_export_hash" in body
    bpy.context.view_layer.objects.active = a
    assert save_items() == {"FINISHED"}
    after = {o: polygons(o["tt_source"]) for o in (body, low)}
    assert all(after[o] < before[o] for o in (body, low)), (before, after)
    return f"high {before[body]} -> {after[body]}, low {before[low]} -> {after[low]} polygons"


@test
def a_horn_blended_over_three_bones_splits_whole_with_its_weights():
    # The viking chieftain's body from before its lur was split out, with the horn on Prop1, Prop2 and Prop3.
    path = os.path.join(TEMP, "viking_chief_unsplit.xml")
    with open(path, "wb") as f:
        f.write(subprocess.run(["git", "show", "20efb3a49:assets/geometry/vikings/chieftain/viking_chief.xml"],
                               cwd=REPO, capture_output=True, check=True).stdout)
    body = addon.import_mesh_file(bpy.context, path, False, False, lambda k, m: None)
    select_only(body)
    assert bpy.ops.object.tt_split_by_bone(bone="Prop1, Prop2, Prop3") == {"FINISHED"}
    horn = bpy.context.active_object
    assert horn.name == "viking_chief_unsplit_Prop1", horn.name
    assert (len(horn.data.polygons), len(body.data.polygons)) == (82, 720)
    props = {horn.vertex_groups[n].index for n in ("Prop2", "Prop3")}
    assert any(sum(g.group in props for g in v.groups) == 2 for v in horn.data.vertices), "blended weights were lost"
    assert not any(g.name.startswith("Prop") for g in body.vertex_groups if any(
        w.group == g.index for v in body.data.vertices for w in v.groups))
    select_only(body)
    assert bpy.ops.object.tt_split_by_bone(bone="Spine", rule="MOSTLY", part_name="chief_back") == {"FINISHED"}
    assert bpy.context.active_object.name == "chief_back"
    return f"horn {len(horn.data.polygons)} faces, body {len(body.data.polygons)}"


@test
def an_edited_low_detail_axe_is_written_and_its_high_mesh_is_not():
    load("vikings", "warrior")
    axe, axe_low = bpy.data.objects["warrior_axe_held"], bpy.data.objects["warrior_axe_held_lod1"]
    os.utime(axe["tt_source"], (1, 1))
    os.utime(axe_low["tt_source"], (1, 1))
    axe_low.data.vertices[0].co.z += 0.1
    assert save_items() == {"FINISHED"}
    assert os.path.getmtime(axe["tt_source"]) == 1 and os.path.getmtime(axe_low["tt_source"]) != 1


@test
def a_building_publishes_only_the_detail_level_that_changed():
    body = load_building("vikings", "quarters")
    low = bpy.data.objects["viking_main_built_lod1"]
    assert low.hide_get() and not body.hide_get() and low["tt_source"].endswith("viking_main_built_lo.xml")
    wm.tt_detail = "LOW"
    assert body.hide_get() and not low.hide_get()
    wm.tt_detail = "HIGH"
    os.utime(body["tt_source"], (1, 1))
    os.utime(low["tt_source"], (1, 1))
    low.data.vertices[0].co.z += 0.1
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert os.path.getmtime(body["tt_source"]) == 1 and os.path.getmtime(low["tt_source"]) != 1


@test
def an_edited_rock_publishes_exactly_its_own_file():
    assert bpy.ops.wm.tt_load_unit(group="misc", sprite="rock_1") == {"FINISHED"}
    rock = bpy.data.objects[addon.loaded_models()[0].name]
    assert not addon.has_low_detail()
    before = mtimes()
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert mtimes() == before, "publishing an untouched rock wrote a file"
    rock.data.vertices[0].co.z += 0.1
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    changed = [path for path, stamp in mtimes().items() if before.get(path) != stamp]
    assert changed == [os.path.normpath(rock["tt_source"])], changed


def x_range(objs):
    return addon.x_extent(bpy.context, objs)


@test
def add_to_scene_puts_a_movable_model_beside_the_rest_that_is_never_saved_or_cleared_by_loading():
    wm.tt_category = "ALL"

    def pick(group, sprite):
        index = wm.tt_unit_index
        assert bpy.ops.wm.tt_add_to_scene(group=group, sprite=sprite) == {"FINISHED"}
        assert wm.tt_unit_index == index, "adding a row changed the loaded model"
        return next(o for o in addon.references() if o.type == "MESH" and o[addon.REFERENCE_TAG] == f"{group} / {sprite}")

    assert bpy.ops.wm.tt_pick_unit(group="vikings", sprite="armory") == {"FINISHED"}
    body = addon.prop_body(bpy.context)
    assert body["tt_sprite"] == "armory" and wm.tt_units[wm.tt_unit_index].sprite == "armory"

    class Layout:
        def __init__(self):
            self.ops = []

        def separator(self):
            pass

        def operator(self, idname, **kwargs):
            self.ops.append((idname, kwargs.get("text"), types.SimpleNamespace()))
            return self.ops[-1][2]

    clicked = types.SimpleNamespace(bl_rna=types.SimpleNamespace(identifier="WM_OT_tt_pick_unit"), group="vikings",
                                    sprite="quarters")
    menu = types.SimpleNamespace(layout=Layout())
    addon.units_list_menu(menu, types.SimpleNamespace(button_operator=clicked, window_manager=wm))
    (add_id, add_text, add), (remove_id, _, _) = menu.layout.ops
    assert (add_id, add_text, add.group, add.sprite) == ("wm.tt_add_to_scene", "Add To Scene: quarters", "vikings",
                                                         "quarters"), menu.layout.ops
    assert remove_id == "wm.tt_remove_added"
    other = types.SimpleNamespace(layout=Layout())
    addon.units_list_menu(other, types.SimpleNamespace(button_operator=None))
    assert other.layout.ops == [], "the menu showed on a button that is not a Models row"
    expect_error(lambda: bpy.ops.wm.tt_add_to_scene(group="vikings", sprite="nope"), "No model")

    hut = pick("vikings", "quarters")
    assert x_range([body])[1] < x_range([hut])[0], (x_range([body]), x_range([hut]))
    peon = pick("natives", "peon")
    assert x_range([hut])[1] < x_range([peon])[0], (x_range([hut]), x_range([peon]))
    assert peon.parent is not None and peon.parent.get(addon.REFERENCE_TAG)
    assert not peon.hide_select and not peon.parent.hide_select and not hut.hide_select
    hut.location.y += 5.0  # the artist moves it
    assert bpy.context.active_object is not None and not bpy.context.active_object.get(addon.REFERENCE_TAG)
    a = load("vikings", "peon")
    assert hut.name in bpy.data.objects and peon.name in bpy.data.objects
    assert all(not o.get(addon.REFERENCE_TAG) for o in addon.loaded_models())
    assert addon.attachment_obj_poll(None, hut) is False and not addon.item_rows([hut], a, "")[0][0]
    before = mtimes()
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"} and mtimes() == before
    select_only(hut)
    expect_error(lambda: bpy.ops.export_mesh.tt_xml(filepath=os.path.join(TEMP, "ref.xml")), "never exported")
    assert not os.path.exists(os.path.join(TEMP, "ref.xml"))
    select_only(peon.parent)
    assert bpy.ops.object.delete() == {"FINISHED"}, "plain Delete on an added rig"
    assert bpy.ops.wm.tt_load_unit(group="vikings", sprite="peon") == {"FINISHED"}, "loading after a Delete"
    assert bpy.ops.wm.tt_remove_added() == {"FINISHED"}
    assert not addon.references() and not any(x.get(addon.REFERENCE_TAG) for x in bpy.data.actions)
    assert not any(d.get(addon.REFERENCE_TAG) for d in list(bpy.data.meshes) + list(bpy.data.armatures))
    assert "ref_quarters" not in bpy.data.meshes and not bpy.ops.wm.tt_remove_added.poll()


@test
def geometry_xml_is_only_ever_appended_to():
    text = open(registry_path, "rb").read().decode("utf-8")
    assert text.startswith(PRISTINE[:PRISTINE.index("<geometry>")]), "DOCTYPE was touched"
    assert ("\r\n" in text) == ("\r\n" in PRISTINE), "line endings changed"
    ET.fromstring(text.encode("utf-8"))
    for line in PRISTINE.splitlines():
        assert line in text, f"an original line went missing: {line[:60]}"


TIERS = ("rock", "iron", "rubber")
AXE_TEXTURES = [f"viking_warrior_axe_held_{tier}" for tier in TIERS]


def save_pattern(path, size, blue):
    """A throwaway atlas whose every pixel is unique: red and green are x and y, blue carries the rest."""
    y, x = np.mgrid[0:size, 0:size]
    rgba = np.stack([x % 256, y % 256, blue(x, y), np.full_like(x, 255)], -1).astype(np.float32) / 255.0
    image = bpy.data.images.new("tt_test_pattern", size, size, alpha=True)
    image.pixels.foreach_set(rgba.ravel())
    image.filepath_raw, image.file_format = path, "PNG"
    image.save()
    bpy.data.images.remove(image)


def pixels(image):
    w, h = image.size
    out = np.empty(w * h * 4, np.float32)
    image.pixels.foreach_get(out)
    return out.reshape(h, w, 4)


def atlas_xy(pixel):
    r, g, b = (int(round(c * 255)) for c in pixel[:3])
    return r + 256 * (b % 16 // 4), g + 256 * (b % 4)


def uv_arrays(objs):
    out = []
    for o in objs:
        for layer in o.data.uv_layers:
            uv = np.empty(2 * len(layer.data), np.float32)
            layer.data.foreach_get("uv", uv)
            out.append(uv.reshape(-1, 2))
    return out


def texel(image_pixels, uv):
    h, w = image_pixels.shape[:2]
    return image_pixels[np.minimum((uv[:, 1] * h).astype(int), h - 1), np.minimum((uv[:, 0] * w).astype(int), w - 1)]


def expect_copy(new, source, x0, y0, xe, ye):
    """new holds source[y0..ye, x0..xe] at its origin, its last row and column repeated to fill the rest."""
    size = new.shape[0]
    rows = y0 + np.minimum(np.arange(size), ye - y0)
    cols = x0 + np.minimum(np.arange(size), xe - x0)
    assert np.array_equal(new, source[rows][:, cols]), "the new image is not an exact copy of the atlas area"


@test
def own_texture_refuses_a_name_that_is_taken():
    a = load("vikings", "warrior")
    axe = bpy.data.objects["warrior_axe_held"]
    assert addon.shares_unit_texture(a, axe)
    assert not addon.shares_unit_texture(a, addon.browsed_unit(a))
    taken = bpy.data.images.new("viking_warrior_axe_held_iron", 4, 4)
    expect_error(lambda: bpy.ops.object.tt_own_texture(target=axe.name), "viking_warrior_axe_held_iron already exists")
    bpy.data.images.remove(taken)
    on_disk = os.path.join(MODELS, "viking_warrior_axe_held_rubber.png")
    shutil.copy(os.path.join(MODELS, "viking_warrior_rock.png"), on_disk)
    expect_error(lambda: bpy.ops.object.tt_own_texture(target=axe.name), "viking_warrior_axe_held_rubber already exists")
    os.remove(on_disk)
    assert axe["tt_texture"] == "viking_warrior_rock,viking_warrior_iron,viking_warrior_rubber"
    assert axe.data.materials[0].name == "tt_viking_warrior_rock"


@test
def own_texture_copies_the_axe_out_of_every_tier_and_publishes_it():
    for i, tier in enumerate(TIERS):
        save_pattern(os.path.join(MODELS, f"viking_warrior_{tier}.png"), 1024,
                     lambda x, y, i=i: x // 256 * 4 + y // 256 + 16 * i)
        save_pattern(os.path.join(DECALS, f"viking_warrior_{tier}_team.png"), 256, lambda x, y, i=i: 100 + i + 0 * x)
    for image in bpy.data.images:
        if image.filepath and os.path.abspath(bpy.path.abspath(image.filepath)).startswith(TEMP):
            image.reload()
    a = load("vikings", "warrior")
    body, body_low = addon.browsed_unit(a), bpy.data.objects["warrior_mesh_lod1"]
    axe, axe_low = bpy.data.objects["warrior_axe_held"], bpy.data.objects["warrior_axe_held_lod1"]
    atlases = [pixels(bpy.data.images.load(os.path.join(MODELS, f"viking_warrior_{t}.png"), check_existing=True))
               for t in TIERS]
    decals = [pixels(bpy.data.images.load(os.path.join(DECALS, f"viking_warrior_{t}_team.png"), check_existing=True))
              for t in TIERS]
    before_uvs = uv_arrays([axe, axe_low])
    body_files = {o["tt_source"]: os.path.getmtime(o["tt_source"]) for o in (body, body_low)}
    for path in body_files:
        os.utime(path, (1, 1))
    atlas_bytes = {t: open(os.path.join(MODELS, f"viking_warrior_{t}.png"), "rb").read() for t in TIERS}
    registry_before = open(registry_path, "rb").read().decode("utf-8")

    assert bpy.ops.object.tt_own_texture(target=axe.name) == {"FINISHED"}
    assert axe["tt_texture"] == axe_low["tt_texture"] == ",".join(AXE_TEXTURES), axe["tt_texture"]
    assert axe["tt_file_texture"] == ",".join(AXE_TEXTURES), "the file keeps its list of one texture per tier"
    assert all(addon.mesh_texture_image(o).name == AXE_TEXTURES[0] for o in (axe, axe_low))
    after_uvs = uv_arrays([axe, axe_low])
    for i, name in enumerate(AXE_TEXTURES):
        new = pixels(bpy.data.images[name])
        size = new.shape[0]
        assert size & (size - 1) == 0 and size < 1024 and new.shape[1] == size, new.shape
        (x0, y0), (xe, ye) = atlas_xy(new[0, 0]), atlas_xy(new[-1, -1])
        assert int(round(new[0, 0, 2] * 255)) // 16 == i, "cropped from the wrong tier"
        expect_copy(new, atlases[i], x0, y0, xe, ye)
        for before, after in zip(before_uvs, after_uvs):
            assert (before[:, 0] * 1024 >= x0).all() and (before[:, 0] * 1024 <= xe + 1).all()
            assert (before[:, 1] * 1024 >= y0).all() and (before[:, 1] * 1024 <= ye + 1).all()
            assert np.array_equal(texel(atlases[i], before), texel(new, after)), "a UV now shows another pixel"
        team = pixels(bpy.data.images[name + "_team"])
        assert team.shape[0] == size // 4, team.shape
        dx0, dy0 = (int(round(c * 255)) for c in team[0, 0, :2])
        dxe, dye = (int(round(c * 255)) for c in team[-1, -1, :2])
        assert (dx0 * 4, dy0 * 4) == (x0, y0) and int(round(team[0, 0, 2] * 255)) == 100 + i
        expect_copy(team, decals[i], dx0, dy0, dxe, dye)
        for before, after in zip(before_uvs, after_uvs):
            assert np.array_equal(texel(decals[i], before), texel(team, after)), "the decal moved against the UVs"
    assert body["tt_texture"] == "viking_warrior_rock,viking_warrior_iron,viking_warrior_rubber"
    assert body.data.materials[0].name == "tt_viking_warrior_rock"

    assert bpy.ops.object.tt_set_tier(index=2) == {"FINISHED"}
    assert all(addon.mesh_texture_image(o).name == AXE_TEXTURES[2] for o in (axe, axe_low))
    assert body.data.materials[0].name == "tt_viking_warrior_rubber"
    assert bpy.ops.object.tt_set_tier(index=0) == {"FINISHED"}

    bpy.context.view_layer.objects.active = a
    assert save_items() == {"FINISHED"}
    for name in AXE_TEXTURES:
        for folder, png in ((MODELS, name), (DECALS, name + "_team")):
            saved = bpy.data.images.load(os.path.join(folder, png + ".png"))
            assert np.array_equal(pixels(saved), pixels(bpy.data.images[png])), f"{png}.png differs from the copy"
            bpy.data.images.remove(saved)
    e = entry("vikings", "warrior_axe_held")
    assert e["textures"] == [[(n, "") for n in AXE_TEXTURES]] * 2, e["textures"]
    text = open(registry_path, "rb").read().decode("utf-8")
    for name in AXE_TEXTURES:
        assert f'<texture name="{name}" team="{name}_team"/>' in text
    spans = [{(g, n): (s, t) for g, n, s, t in addon.sprite_blocks(x)} for x in (registry_before, text)]
    (s0, e0), (s1, e1) = (span[("vikings", "warrior_axe_held")] for span in spans)
    assert registry_before[:s0] == text[:s1] and registry_before[e0:] == text[e1:], "another sprite's lines changed"
    mesh = ET.parse(axe["tt_source"]).getroot()
    assert mesh.get("texture") == ",".join(AXE_TEXTURES)
    assert all(os.path.getmtime(path) == 1 for path in body_files), "the unit's mesh was written"
    assert all(open(os.path.join(MODELS, f"viking_warrior_{t}.png"), "rb").read() == atlas_bytes[t] for t in TIERS)

    a = load("vikings", "warrior")
    axe = bpy.data.objects["warrior_axe_held"]
    assert axe["tt_texture"] == ",".join(AXE_TEXTURES) and not addon.shares_unit_texture(a, axe)
    assert addon.mesh_texture_image(axe).name == AXE_TEXTURES[0]
    return f"{len(AXE_TEXTURES)} tiers, {bpy.data.images[AXE_TEXTURES[0]].size[0]} px"


@test
def a_new_mesh_on_the_units_texture_gets_one_named_after_it():
    a = load("vikings", "warrior")
    visor = fixture_mesh("test_visor", None)
    visor.data.materials.append(bpy.data.materials["tt_viking_warrior_rock"])
    put_on_head(visor)
    assert addon.shares_unit_texture(a, visor)
    assert bpy.ops.object.tt_own_texture(target=visor.name) == {"FINISHED"}
    assert visor["tt_texture"] == "test_visor" and addon.mesh_texture_image(visor).name == "test_visor"
    assert save_items() == {"FINISHED"}
    assert os.path.isfile(os.path.join(MODELS, "test_visor.png"))
    assert os.path.isfile(os.path.join(DECALS, "test_visor_team.png"))
    assert entry("vikings", "warrior_test_visor")["textures"] == [[("test_visor", "")]]
    assert '<texture name="test_visor" team="test_visor_team"/>' in open(registry_path, encoding="utf-8").read()


def evaluated_points(obj):
    evaluated = obj.evaluated_get(bpy.context.evaluated_depsgraph_get())
    mesh = evaluated.to_mesh()
    try:
        return np.array([tuple(obj.matrix_world @ v.co) for v in mesh.vertices])
    finally:
        evaluated.to_mesh_clear()


@test
def an_item_on_several_bones_deforms_with_all_of_them_and_keeps_its_weights():
    # Fixture: the native chieftain's rigid magic pot, reweighted to hang three quarters off Prop1, a quarter Prop2.
    folder = os.path.join(GEOMETRY, "natives", "chieftain")
    path = os.path.join(folder, "chieftain_test_blend.xml")
    text = open(os.path.join(folder, "chieftain_magic_pot.xml"), encoding="utf-8").read()
    text = re.sub(r'<mesh texture="[^"]*">', "<mesh>", text).replace(
        '<skin bone="Prop1" weight="1"/>', '<skin bone="Prop1" weight="0.75"/><skin bone="Prop2" weight="0.25"/>')
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    save_pattern(os.path.join(MODELS, "native_chieftain.png"), 1024, lambda x, y: x // 256 * 4 + y // 256)
    save_pattern(os.path.join(DECALS, "native_chieftain_team.png"), 256, lambda x, y: 100 + 0 * x)
    addon.append_registry_entries(registry_path, "natives", [("chieftain_test_blend", addon.sprite_text(
        [("name", "chieftain_test_blend"), ("base", "chieftain"), ("slot", "test_blend"), ("default", "true")],
        [("natives/chieftain/chieftain_test_blend.xml", [("native_chieftain", ' team="native_chieftain_team"')])]))])

    a = load("natives", "chieftain")
    blend, rigid = bpy.data.objects["chieftain_test_blend"], bpy.data.objects["chieftain_magic_pot"]
    assert blend.parent == a and blend.parent_type == "OBJECT" and "tt_bone" not in blend
    assert any(m.type == "ARMATURE" and m.object == a for m in blend.modifiers)
    assert rigid.parent_type == "BONE" and rigid.parent_bone == "Prop1"
    assert addon.item_bones(blend) == ["Prop1", "Prop2"] and addon.item_bones(rigid) == ["Prop1"]
    assert items()["test_blend"] == [("chieftain_test_blend", True)]

    every = sorted(addon.armature_actions(a), key=lambda x: x.name)
    assert addon.item_clips(a, blend) == every, "Prop2 shows in every clip, so the item does too"
    idle = next(x for x in every if addon.clip_short_name(a, x) == "idle")
    assert bpy.ops.object.tt_set_clip(clip=idle.name) == {"FINISHED"}
    assert addon.item_hidden_here(a, rigid) and addon.item_hidden_here(a, blend) is None

    a.animation_data.action = None
    for pose_bone in a.pose.bones:
        pose_bone.matrix_basis.identity()
    bpy.context.view_layer.update()
    before = evaluated_points(blend), evaluated_points(rigid)
    a.pose.bones["Prop2"].location = (0.0, 0.4, 0.0)
    bpy.context.view_layer.update()
    shift = np.linalg.norm(evaluated_points(blend) - before[0], axis=1)
    assert np.allclose(shift, 0.1, atol=1e-4), (shift.min(), shift.max())
    assert np.allclose(evaluated_points(rigid), before[1])
    a.pose.bones["Prop2"].location = (0.0, 0.0, 0.0)
    addon.assign_action(a, idle)

    os.utime(path, (1, 1))
    bpy.context.view_layer.objects.active = a
    assert save_items() == {"FINISHED"}
    assert os.path.getmtime(path) == 1, "publishing an untouched item on several bones wrote it"

    assert addon.shares_unit_texture(a, blend)
    assert bpy.ops.object.tt_own_texture(target=blend.name) == {"FINISHED"}
    assert blend["tt_texture"] == "native_chieftain_test_blend"
    bpy.context.view_layer.objects.active = a
    assert save_items() == {"FINISHED"}
    old, new = ET.fromstring(text.encode("utf-8")), ET.parse(path).getroot()
    assert new.get("texture") == "native_chieftain_test_blend"
    corners = lambda root: [tuple(float(v.get(k)) for k in "xyz") for v in root.iter("vertex")]
    assert corners(new) == corners(old), "the item moved on export"
    skins = {tuple((s.get("bone"), s.get("weight")) for s in v.iter("skin")) for v in new.iter("vertex")}
    assert skins == {(("Prop1", "0.75"), ("Prop2", "0.25"))}, skins
    assert entry("natives", "chieftain_test_blend")["textures"] == [[("native_chieftain_test_blend", "")]]
    assert os.path.isfile(os.path.join(MODELS, "native_chieftain_test_blend.png"))

    a = load("natives", "chieftain")
    blend = bpy.data.objects["chieftain_test_blend"]
    assert blend.parent_type == "OBJECT" and not addon.shares_unit_texture(a, blend)
    return f"{len(blend.data.polygons)} faces on {', '.join(addon.item_bones(blend))}"


WARRIOR_FILES = [os.path.join(GEOMETRY, "vikings", "warrior", f) for f in ("warrior_mesh.xml",
                                                                           "warrior_low_poly_mesh.xml")]
WARRIOR_TEXTURES = [(f"viking_warrior_{tier}", "") for tier in TIERS]


def file_bytes(paths):
    return {p: open(p, "rb").read() for p in paths}


def changed_files(before):
    return sorted(p for p, stamp in mtimes().items() if before.get(p) != stamp)


def save_skin(name):
    assert bpy.ops.object.tt_new_skin(skin_name=name) == {"FINISHED"}
    return bpy.ops.object.tt_save_skin()


@test
def a_texture_only_skin_reuses_the_stock_meshes():
    a = load("vikings", "warrior")
    body = addon.browsed_unit(a)
    assert addon.VIEW3D_PT_tt_skins.poll(bpy.context)
    before, stock = mtimes(), file_bytes(WARRIOR_FILES)
    material = bpy.data.materials.new("test_gold_mat")
    material.use_nodes = True
    material.node_tree.nodes.new("ShaderNodeTexImage").image = fixture_image("warrior_gold_rock")
    body.data.materials.clear()
    body.data.materials.append(material)
    assert save_skin("gold") == {"FINISHED"}
    e, stock_entry = entry("vikings", "warrior_gold"), entry("vikings", "warrior")
    assert (e["skin"], e["replaces"], e["base"]) == ("gold", "warrior", "warrior"), e
    assert e["models"] == stock_entry["models"], e["models"]
    assert e["textures"] == [[("warrior_gold_rock", "")] + WARRIOR_TEXTURES[1:], WARRIOR_TEXTURES], e["textures"]
    assert changed_files(before) == [os.path.normpath(registry_path)], changed_files(before)
    assert os.path.isfile(os.path.join(MODELS, "warrior_gold_rock.png"))
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="warrior_gold" skin="gold" replaces="warrior" base="warrior">' in text
    assert '<texture name="warrior_gold_rock" team="viking_warrior_rock_team"/>' in text
    assert file_bytes(WARRIOR_FILES) == stock
    assert body.data.materials[0].name == "tt_viking_warrior_rock" and "tt_skin" not in body


@test
def a_mesh_skin_writes_both_detail_levels_and_never_the_stock_body():
    a = load("vikings", "warrior")
    body, low = addon.browsed_unit(a), bpy.data.objects["warrior_mesh_lod1"]
    stock, hashes = file_bytes(WARRIOR_FILES), {o: o["tt_export_hash"] for o in (body, low)}
    for o in (body, low):
        o.data.vertices[0].co.z += 0.1
    assert save_skin("bald") == {"FINISHED"}
    e = entry("vikings", "warrior_bald")
    assert e["models"] == ["vikings/warrior/warrior_bald.xml", "vikings/warrior/warrior_bald_lo.xml"], e["models"]
    assert e["base"] == "warrior" and e["textures"] == [WARRIOR_TEXTURES] * 2, e
    for model in e["models"]:
        mesh = ET.parse(os.path.join(GEOMETRY, model)).getroot()
        bones = {s.get("bone") for s in mesh.iter("skin")}
        assert len(bones) > 1 and addon.STATIC_BONE not in bones, bones
        assert mesh.get("texture") == ",".join(t for t, _ in WARRIOR_TEXTURES)
    assert file_bytes(WARRIOR_FILES) == stock
    assert all(o["tt_export_hash"] == hashes[o] for o in (body, low)), "the body did not go back to stock"
    for path in WARRIOR_FILES:
        os.utime(path, (1, 1))
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert all(os.path.getmtime(path) == 1 for path in WARRIOR_FILES), "Publish wrote the stock body"


@test
def a_building_stage_skin_is_static_and_has_no_base():
    body = load_building("vikings", "quarters_halfbuilt")
    stock_entry = entry("vikings", "quarters_halfbuilt")
    stock = file_bytes([os.path.join(GEOMETRY, m) for m in stock_entry["models"]])
    body.data.vertices[0].co.z += 0.1
    assert save_skin("stone") == {"FINISHED"}
    e = entry("vikings", "quarters_halfbuilt_stone")
    assert (e["skin"], e["replaces"], e["base"]) == ("stone", "quarters_halfbuilt", ""), e
    assert e["models"] == ["vikings/quarters/quarters_halfbuilt_stone.xml", stock_entry["models"][1]], e["models"]
    assert e["textures"] == stock_entry["textures"]
    mesh = ET.parse(os.path.join(GEOMETRY, e["models"][0])).getroot()
    assert {s.get("bone") for s in mesh.iter("skin")} == {addon.STATIC_BONE} and mesh.get("texture") is None
    assert '<sprite name="quarters_halfbuilt_stone" skin="stone" replaces="quarters_halfbuilt">' in \
           open(registry_path, encoding="utf-8").read()
    assert file_bytes(stock) == stock


def converter_skins(geometry, listing="skins.txt"):
    """skins.txt, or another listing, as the game's geometry converter writes it for this geometry folder; None
    without a built converter and a JDK that runs it."""
    classes = [os.path.join(REPO, p, "build", "classes", "java", "main") for p in ("tools", "common")]
    jars = [j for j in glob.glob(os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1",
                                              "org.joml", "joml", "*", "*", "joml-*.jar"))
            if not j.endswith(("-sources.jar", "-javadoc.jar"))]
    javas = [os.path.join(os.environ.get("JAVA_HOME", ""), "bin", "java"), "C:/Program Files/Java/jdk-26/bin/java",
             shutil.which("java") or ""]
    if not jars or not all(os.path.isdir(c) for c in classes):
        return None
    out = os.path.join(TEMP, "geometry_bin")
    for java in [j for j in javas if os.path.isfile(j) or os.path.isfile(j + ".exe")]:
        # An older JDK refuses the converter's class files; the next one may run them.
        if subprocess.run([java, "-cp", os.pathsep.join(classes + jars[-1:]), "com.oddlabs.converter.ConvertToBinary",
                           "geometry.xml", geometry, out], cwd=geometry, capture_output=True).returncode == 0:
            return open(os.path.join(out, listing), encoding="utf-8").read().splitlines()
    return None


@test
def one_skin_name_covers_several_models():
    for group, sprite in (("vikings", "warrior"), ("vikings", "quarters")):
        if sprite == "warrior":
            body = addon.browsed_unit(load(group, sprite))
        else:
            body = load_building(group, sprite)
        material = bpy.data.materials.new(f"test_{sprite}_harvest_mat")
        material.use_nodes = True
        material.node_tree.nodes.new("ShaderNodeTexImage").image = fixture_image(f"{sprite}_harvest_tex")
        body.data.materials.clear()
        body.data.materials.append(material)
        assert save_skin("harvest") == {"FINISHED"}
    skins = {(s["name"], s["replaces"]) for s in addon.read_registry(TEMP) if s["skin"] == "harvest"}
    assert skins == {("warrior_harvest", "warrior"), ("quarters_harvest", "quarters")}, skins
    expect_error(lambda: save_skin("harvest"), "quarters already has a harvest skin")
    lines = converter_skins(GEOMETRY)
    if lines is None:
        return "skins.txt skipped: no built converter or JDK"
    harvest = sorted(line.rsplit(" ", 2)[0] for line in lines if line.split(" ")[1] == "harvest")
    assert harvest == ["vikings harvest quarters quarters_harvest", "vikings harvest warrior warrior_harvest"], lines


@test
def a_skin_is_refused_for_a_taken_or_bad_name_a_file_on_disk_or_no_change():
    a = load("vikings", "warrior")
    registry_before = open(registry_path, "rb").read()
    expect_error(lambda: save_skin("gold"), "warrior already has a gold skin")
    expect_error(lambda: save_skin("axe_held"), "already has a sprite named warrior_axe_held")
    expect_error(lambda: save_skin("go ld"), "letters, digits and underscores")
    expect_error(lambda: save_skin("plain"), "nothing differs from the default model")
    taken = os.path.join(GEOMETRY, "vikings", "warrior", "warrior_taken.xml")
    with open(taken, "w") as f:
        f.write("keep")
    addon.browsed_unit(a).data.vertices[0].co.z += 0.1
    expect_error(lambda: save_skin("taken"), "warrior_taken.xml is already on disk")
    assert open(taken).read() == "keep"
    os.remove(taken)
    assert open(registry_path, "rb").read() == registry_before


@test
def preview_shows_a_skin_and_stock_puts_the_model_back():
    a = load("vikings", "warrior")
    body, low = addon.browsed_unit(a), bpy.data.objects["warrior_mesh_lod1"]
    hashes, texture = {o: o["tt_export_hash"] for o in (body, low)}, body["tt_texture"]
    assert bpy.ops.object.tt_show_skin(skin="bald") == {"FINISHED"}
    assert body["tt_skin"] == low["tt_skin"] == "warrior_bald"
    for o, name in ((body, "warrior_bald.xml"), (low, "warrior_bald_lo.xml")):
        record = addon.mesh_record_from_xml(ET.parse(os.path.join(GEOMETRY, "vikings", "warrior", name)).getroot(),
                                            False)
        assert np.allclose([v.co[:] for v in o.data.vertices], record.verts), f"{o.name} is not showing {name}"
    for path in WARRIOR_FILES:
        os.utime(path, (1, 1))
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert all(os.path.getmtime(path) == 1 for path in WARRIOR_FILES), "Publish wrote a previewed skin as stock"
    assert bpy.ops.object.tt_show_skin(skin="gold") == {"FINISHED"}
    assert body.data.materials[0].name == "tt_warrior_gold_rock" and body["tt_texture"].startswith("warrior_gold_rock")
    assert low.data.materials[0].name == "tt_viking_warrior_rock"
    assert bpy.ops.object.tt_new_skin(skin_name="again") == {"FINISHED"}
    assert body["tt_skin_editing"] == "again" and "tt_skin" not in body, "a new skin did not start from Default"
    assert bpy.ops.object.tt_show_skin(skin="") == {"FINISHED"}
    assert "tt_skin_editing" not in body
    assert all("tt_skin" not in o and o["tt_export_hash"] == hashes[o] for o in (body, low))
    assert body["tt_texture"] == texture and body.data.materials[0].name == "tt_viking_warrior_rock"


@test
def new_skin_suggests_used_names_and_cancel_restores_default_and_writes_nothing():
    a = load("vikings", "warrior")
    body = addon.browsed_unit(a)
    assert not bpy.ops.object.tt_save_skin.poll(), "Save Skin shows before New Skin"
    assert "gold" in addon.skin_name_search(None, bpy.context, "go")
    expect_error(lambda: bpy.ops.object.tt_new_skin(skin_name="gold"), "warrior already has a gold skin")
    expect_error(lambda: bpy.ops.object.tt_new_skin(skin_name="go ld"), "letters, digits")
    assert "tt_skin_editing" not in body
    before, hashes = mtimes(), {o: o["tt_export_hash"] for o in addon.model_levels(body)}
    assert bpy.ops.object.tt_new_skin(skin_name="spotted") == {"FINISHED"}
    assert body["tt_skin_editing"] == "spotted" and bpy.ops.object.tt_save_skin.poll()
    body.data.vertices[0].co.z += 0.1
    assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"}
    assert "tt_skin_editing" not in body and not bpy.ops.object.tt_save_skin.poll()
    assert all(o["tt_export_hash"] == h for o, h in hashes.items()), "Cancel kept the edit"
    assert changed_files(before) == [] and entry("vikings", "warrior_spotted") is None
    assert bpy.ops.object.tt_new_skin(skin_name="spotted") == {"FINISHED"}
    body.data.vertices[0].co.z += 0.1
    assert bpy.ops.object.tt_save_skin() == {"FINISHED"}
    assert "tt_skin_editing" not in body and entry("vikings", "warrior_spotted")["skin"] == "spotted"


@test
def paint_on_the_stock_texture_goes_to_a_copy():
    a = load("vikings", "warrior")
    image = addon.mesh_texture_image(addon.browsed_unit(a))
    stock = file_bytes([os.path.join(MODELS, "viking_warrior_rock.png")])
    image.pixels[0] = 0.25
    if not image.is_dirty:
        return "skipped: setting pixels does not mark the image dirty in this Blender"
    assert save_skin("painted") == {"FINISHED"}
    assert os.path.isfile(os.path.join(MODELS, "viking_warrior_rock_painted.png"))
    assert file_bytes(stock) == stock and not image.is_dirty
    assert entry("vikings", "warrior_painted")["textures"][0][0] == ("viking_warrior_rock_painted", "")


@test
def removing_a_skin_takes_out_only_its_entry():
    before = open(registry_path, "rb").read().decode("utf-8")
    start, end = next((s, e) for g, n, s, e in addon.sprite_blocks(before) if (g, n) == ("vikings", "warrior_gold"))
    assert bpy.ops.object.tt_remove_from_registry(group="vikings", sprite="warrior_gold") == {"FINISHED"}
    after = open(registry_path, "rb").read().decode("utf-8")
    line_start = before.rfind("\n", 0, start) + 1
    line_end = before.index("\n", end) + 1
    assert after == before[:line_start] + before[line_end:], "more than the skin's entry changed"
    assert entry("vikings", "warrior_bald") is not None and os.path.isfile(os.path.join(MODELS, "warrior_gold_rock.png"))


def hammer():
    return next(o for o in bpy.data.objects if o.get("tt_sprite") == "peon_hammer" and not o.get("tt_detail"))


def open_hammer():
    wm.tt_item_index = list(bpy.data.objects).index(hammer())
    assert addon.open_item(bpy.context) == hammer(), "clicking the row did not open the item"


HAMMER_FILES = [os.path.join(GEOMETRY, "vikings", "peon", f) for f in ("peon_hammer.xml", "peon_hammer_lo.xml")] + \
               [os.path.join(MODELS, "viking_peon_hammer.png")]


@test
def clicking_an_item_opens_it_and_back_or_another_unit_closes_it():
    load("vikings", "peon")
    assert addon.open_item(bpy.context) is None
    open_hammer()
    assert bpy.context.view_layer.objects.active == hammer() and addon.active_armature(bpy.context) == arm()
    assert bpy.ops.object.tt_close_item() == {"FINISHED"}
    assert addon.open_item(bpy.context) is None and wm.tt_open_item == ""
    open_hammer()
    load("vikings", "warrior")
    assert wm.tt_open_item == "" and addon.open_item(bpy.context) is None, "the item stayed open on another unit"


@test
def an_item_skin_from_the_detail_view_repaints_the_item_and_leaves_it_alone():
    load("vikings", "peon")
    open_hammer()
    before, stock = mtimes(), file_bytes(HAMMER_FILES)
    assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="gold") == {"FINISHED"}
    assert hammer()["tt_skin_editing"] == "gold" and bpy.ops.object.tt_save_skin.poll()
    expect_error(lambda: bpy.ops.object.tt_new_skin(skin_name="other"), "Save or cancel skin 'gold' for peon_hammer")
    material = bpy.data.materials.new("test_hammer_gold_mat")
    material.use_nodes = True
    material.node_tree.nodes.new("ShaderNodeTexImage").image = fixture_image("viking_peon_hammer_gold")
    hammer().data.materials.clear()
    hammer().data.materials.append(material)
    assert bpy.ops.object.tt_save_skin() == {"FINISHED"}
    e, stock_entry = entry("vikings", "peon_hammer_gold"), entry("vikings", "peon_hammer")
    assert (e["base"], e["slot"], e["skin"], e["replaces"]) == ("peon", "weapon", "gold", "peon_hammer"), e
    assert e["models"] == stock_entry["models"] and e["textures"][0] == [("viking_peon_hammer_gold", "")], e
    assert '<sprite name="peon_hammer_gold" base="peon" slot="weapon" skin="gold" replaces="peon_hammer">' in \
           open(registry_path, encoding="utf-8").read()
    assert changed_files(before) == [os.path.normpath(registry_path)], changed_files(before)
    assert os.path.isfile(os.path.join(MODELS, "viking_peon_hammer_gold.png"))
    assert file_bytes(HAMMER_FILES) == stock and "tt_skin_editing" not in hammer()
    assert hammer().data.materials[0].name == "tt_viking_peon_hammer"
    expect_error(lambda: bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="gold"),
                 "peon_hammer already has a gold skin")
    assert "peon_hammer_gold" not in {o.get("tt_sprite") for o in bpy.data.objects}, "the skin loaded as an item"


@test
def an_item_skin_can_take_its_shape_from_the_artists_own_mesh():
    a = load("vikings", "peon")
    open_hammer()
    stock, bone = file_bytes(HAMMER_FILES), hammer()["tt_bone"]
    own = fixture_mesh("big_hammer", None, kind="cube")
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="big", mesh=own.name) == {"FINISHED"}
    assert own.parent == a and own.get("tt_bone") == bone and addon.mesh_texture_image(own) is not None
    assert not addon.item_shown(hammer()), "the default item still shows beside the new shape"
    assert bpy.ops.object.tt_save_skin() == {"FINISHED"}
    e = entry("vikings", "peon_hammer_big")
    assert (e["base"], e["slot"], e["skin"], e["replaces"]) == ("peon", "weapon", "big", "peon_hammer"), e
    assert e["models"] == ["vikings/peon/peon_hammer_big.xml"], e["models"]
    assert e["textures"] == [[("viking_peon_hammer_big", "")]], e["textures"]
    mesh = ET.parse(os.path.join(GEOMETRY, e["models"][0])).getroot()
    assert {s.get("bone") for s in mesh.iter("skin")} == {bone} and mesh.get("texture") == "viking_peon_hammer_big"
    assert os.path.isfile(os.path.join(MODELS, "viking_peon_hammer_big.png"))
    assert file_bytes(HAMMER_FILES) == stock
    assert own.parent is None and own.hide_get() and "tt_skin_mesh" not in own and addon.item_shown(hammer())


@test
def cancel_takes_the_artists_mesh_back_off_and_writes_nothing():
    a = load("vikings", "peon")
    own = fixture_mesh("spare_hammer", fixture_image("spare_hammer_tex"), kind="cube")
    bpy.context.view_layer.objects.active = a
    before = mtimes()
    assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="spare", mesh=own.name) == {"FINISHED"}
    assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"}
    assert own.parent is None and not own.hide_get() and addon.item_shown(hammer())
    assert changed_files(before) == [] and entry("vikings", "peon_hammer_spare") is None


@test
def a_skin_preview_swaps_the_body_and_the_items_it_covers():
    a = load("vikings", "peon")
    body = addon.browsed_unit(a)
    body.data.vertices[0].co.z += 0.1
    assert save_skin("gold") == {"FINISHED"}
    assert bpy.ops.object.tt_show_skin(skin="gold") == {"FINISHED"}
    assert body["tt_skin"] == "peon_gold" and hammer()["tt_skin"] == "peon_hammer_gold"
    assert hammer().data.materials[0].name == "tt_viking_peon_hammer_gold"
    assert bpy.ops.object.tt_show_skin(skin="big") == {"FINISHED"}
    assert "tt_skin" not in body and hammer()["tt_skin"] == "peon_hammer_big"
    assert bpy.ops.object.tt_show_skin(skin="", item=hammer().name) == {"FINISHED"}
    assert "tt_skin" not in hammer() and hammer().data.materials[0].name == "tt_viking_peon_hammer"
    skins = converter_skins(GEOMETRY)
    if skins is None:
        return "converter listings skipped: no built converter or JDK"
    attachments = converter_skins(GEOMETRY, "attachments.txt")
    assert "vikings gold peon_hammer peon_hammer_gold 1 -" in skins, skins
    assert "vikings big peon_hammer peon_hammer_big 1 -" in skins, skins
    assert not [line for line in attachments if "peon_hammer_gold" in line or "peon_hammer_big" in line], attachments

class Recorder:
    """Stands in for a panel's layout and records the buttons and labels drawn on it."""

    def __init__(self, log=None):
        self.log = [] if log is None else log

    def __getattr__(self, name):
        return lambda *args, **kwargs: Recorder(self.log)

    def template_popup_confirm(self, idname, text=None, **kwargs):
        self.log.append(("confirm", idname, text, self.__dict__.get("enabled", True)))
        return types.SimpleNamespace()

    def operator(self, idname, **kwargs):
        self.log.append((idname, kwargs.get("text")))
        return types.SimpleNamespace()

    def label(self, text="", **kwargs):
        self.log.append(("label", text))

    def template_list(self, list_type, list_id, data, prop, active_data, active_prop, **kwargs):
        self.log.append(("list", prop))


def drawn(panel):
    layout = Recorder()
    panel.draw(types.SimpleNamespace(layout=layout), bpy.context)
    return layout.log


@test
def the_items_panel_shows_the_list_or_one_item_with_its_skins():
    load("vikings", "peon")
    listed = [idname for idname, _ in drawn(addon.VIEW3D_PT_tt_attachments)]
    assert "object.tt_new_item" in listed and "object.tt_close_item" not in listed, listed
    open_hammer()
    detail = drawn(addon.VIEW3D_PT_tt_attachments)
    ops = [idname for idname, _ in detail]
    assert ops[0] == "object.tt_close_item" and "object.tt_paint_item" in ops, ops
    assert "object.tt_new_item" not in ops, detail
    assert ("object.tt_new_skin", None) in detail and ("list", "tt_item_skins") in detail, detail
    assert ("list", "tt_item_skins") in detail and "gold" in [row.name for row in wm.tt_item_skins], detail
    assert ("list", "tt_skins") in drawn(addon.VIEW3D_PT_tt_skins)
    assert ("gold (+ peon_hammer)", "Owned") in [(row.name, row.tag) for row in wm.tt_skins]
    assert not [row for row in wm.tt_skins if row.name.startswith("big")], "an item-only skin is in the Skins panel"
    assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="red") == {"FINISHED"}
    assert ("label", "Editing skin 'red' for peon_hammer") in drawn(addon.VIEW3D_PT_tt_attachments)
    assert ("label", "Editing skin 'red' for peon_hammer") in drawn(addon.VIEW3D_PT_tt_skins)
    assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"}


@test
def a_skin_preview_from_texture_paint_shows_straight_away():
    load("vikings", "peon")
    select_only(hammer())
    bpy.ops.object.mode_set(mode="TEXTURE_PAINT")
    assert bpy.ops.object.tt_show_skin(skin="gold", item=hammer().name) == {"FINISHED"}
    assert bpy.context.mode == "OBJECT" and hammer().data.materials[0].name == "tt_viking_peon_hammer_gold"
    assert bpy.context.scene.tool_settings.image_paint.canvas == addon.mesh_texture_image(hammer())
    assert bpy.ops.object.tt_show_skin(skin="", item=hammer().name) == {"FINISHED"}


@test
def an_event_skin_is_written_with_its_event_and_scenery_skins_need_one():
    body = load_building("vikings", "quarters")
    assert addon.skins_owned(bpy.context)
    assert bpy.ops.object.tt_new_event(event_name="halloween") == {"FINISHED"}
    assert bpy.ops.object.tt_new_skin(skin_name="halloween", event="halloween") == {"FINISHED"}
    material = bpy.data.materials.new("test_quarters_halloween_mat")
    material.use_nodes = True
    material.node_tree.nodes.new("ShaderNodeTexImage").image = fixture_image("test_quarters_halloween")
    body.data.materials.clear()
    body.data.materials.append(material)
    assert bpy.ops.object.tt_save_skin() == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="quarters_halloween" skin="halloween" replaces="quarters" event="halloween">' in text
    assert entry("vikings", "quarters_halloween")["event"] == "halloween" and "tt_skin_event" not in body
    assert entry("vikings", "quarters_harvest")["event"] == "", "an owned skin got an event"
    assert bpy.ops.wm.tt_load_unit(group="misc", sprite="oak_tree_crown") == {"FINISHED"}
    tree = addon.loaded_body()
    assert addon.VIEW3D_PT_tt_skins.poll(bpy.context) and not addon.skins_owned(bpy.context)
    assert "ALL_YEAR" not in [i[0] for i in addon.event_items(bpy.context, addon.skins_owned(bpy.context))]
    tree.data.vertices[0].co.z += 0.1
    assert bpy.ops.object.tt_new_skin(skin_name="halloween", event="halloween") == {"FINISHED"}
    assert bpy.ops.object.tt_save_skin() == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="oak_tree_crown_halloween" skin="halloween" replaces="oak_tree_crown" event="halloween">' in text
    assert entry("misc", "oak_tree_crown_halloween")["models"][0] == "misc/oak_tree_crown_halloween.xml"


@test
def the_skins_panel_on_a_building_says_who_gets_each_skin():
    load_building("vikings", "quarters")
    assert addon.VIEW3D_PT_tt_skins.poll(bpy.context)
    drawn_rows = drawn(addon.VIEW3D_PT_tt_skins)
    assert ("list", "tt_skins") in drawn_rows and ("object.tt_new_skin", None) in drawn_rows, drawn_rows
    rows = [(row.name, row.tag) for row in wm.tt_skins]
    assert rows[0] == ("Default", "") and ("harvest", "Owned") in rows and ("halloween", "halloween") in rows, rows
    for index, row in enumerate(wm.tt_skins):
        layout = Recorder()
        addon.TT_UL_skins.draw_item(None, bpy.context, layout, wm, row, 0, wm, "tt_skin_index", index)
        remove = [("object.tt_remove_from_registry", "")] if row.skin else [("label", "")]
        assert layout.log == [("object.tt_show_skin", ""), ("label", row.name), ("label", row.tag),
                              ("object.tt_paint_skin", "")] + remove, layout.log


@test
def picking_a_skin_row_shows_it_and_paint_and_the_bin_act_on_it():
    body = load_building("vikings", "quarters")
    assert wm.tt_skin_index == 0 and not body.get("tt_skin")
    wm.tt_skin_index = [row.skin for row in wm.tt_skins].index("harvest")
    assert body["tt_skin"] == "quarters_harvest" and wm.tt_skins[wm.tt_skin_index].skin == "harvest"
    assert bpy.ops.object.tt_pick_skin(skin="") == {"FINISHED"}
    assert not body.get("tt_skin") and wm.tt_skin_index == 0
    assert bpy.ops.object.tt_pick_skin(skin="halloween") == {"FINISHED"}
    assert wm.tt_skins[wm.tt_skin_index].skin == "halloween"
    assert bpy.ops.object.tt_new_skin(skin_name="red") == {"FINISHED"}
    assert wm.tt_skin_index == 0, "a new skin starts from Default"
    assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"}
    before = len(wm.tt_skins)
    wm.tt_skin_index = [row.skin for row in wm.tt_skins].index("harvest")
    assert bpy.ops.object.tt_remove_from_registry(group="vikings", sprite="quarters_harvest") == {"FINISHED"}
    assert len(wm.tt_skins) == before - 1 and "harvest" not in [row.skin for row in wm.tt_skins]
    assert not body.get("tt_skin") and wm.tt_skin_index == 0, "the removed skin still shows"
    load("vikings", "peon")
    open_hammer()
    assert [row.skin for row in wm.tt_item_skins][0] == "" and wm.tt_item_skin_index == 0
    wm.tt_item_skin_index = [row.skin for row in wm.tt_item_skins].index("gold")
    assert hammer()["tt_skin"] == "peon_hammer_gold" and not addon.loaded_body().get("tt_skin")
    assert bpy.ops.object.tt_close_item() == {"FINISHED"} and not len(wm.tt_item_skins)


@test
def paint_on_a_skin_goes_to_its_own_texture_and_never_the_default_one():
    body = load_building("vikings", "quarters")
    png = os.path.join(MODELS, "test_quarters_halloween.png")
    before = open(png, "rb").read()
    assert bpy.ops.object.tt_paint_skin(skin="halloween") == {"FINISHED"}
    assert bpy.context.mode == "PAINT_TEXTURE" and body["tt_skin"] == "quarters_halloween"
    image = addon.mesh_texture_image(body)
    assert addon.image_texture_name(image) == "test_quarters_halloween", image.name
    image.pixels[0] = 0.25
    assert bpy.ops.object.tt_done_painting() == {"FINISHED"}
    note = ""
    if image.is_dirty:
        assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
        assert open(png, "rb").read() != before and not image.is_dirty
    else:
        note = "publish skipped: setting pixels does not mark the image dirty in this Blender"
    load_building("vikings", "quarters_halfbuilt")
    expect_error(lambda: bpy.ops.object.tt_paint_skin(skin="stone"), "uses the default texture viking_buildings_hi")
    assert bpy.context.mode == "OBJECT"
    return note


@test
def props_sit_on_a_tree_trunk_or_crown_and_on_the_chicken():
    assert bpy.ops.wm.tt_load_unit(group="misc", sprite="oak_tree_crown") == {"FINISHED"}
    assert addon.VIEW3D_PT_tt_attachments.poll(bpy.context)
    assert [i[0] for i in addon.prop_base_items(None, bpy.context)] == ["oak_tree_crown", "oak_tree_trunk"]
    lantern = fixture_mesh("test_tree_lantern", fixture_image("test_tree_lantern_tex"), z=2.0, kind="cube")
    assert bpy.ops.object.tt_new_prop(mesh=lantern.name, base="oak_tree_trunk", event="halloween") == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="oak_tree_trunk_test_tree_lantern" base="oak_tree_trunk" slot="prop" event="halloween">' in text
    assert bpy.ops.wm.tt_load_unit(group="misc", sprite="oak_tree_trunk") == {"FINISHED"}
    props = addon.building_props(addon.prop_body(bpy.context))
    assert [o["tt_sprite"] for o in props] == ["oak_tree_trunk_test_tree_lantern"], props
    assert addon.prop_tag(props[0]) == "halloween"
    load("misc", "chicken")
    put_on_head(fixture_mesh("test_chicken_hat", fixture_image("test_chicken_hat_tex")))
    assert save_items() == {"FINISHED"}
    e = entry("misc", "chicken_test_chicken_hat")
    assert (e["base"], e["slot"]) == ("chicken", "prop"), e


@test
def the_event_dropdown_offers_registry_events_and_new_ones():
    registry_events = addon.known_events(bpy.context)
    assert "halloween" in registry_events and "easter_2" not in registry_events, registry_events
    optional = [i[0] for i in addon.event_items(bpy.context, True)]
    assert optional[0] == "ALL_YEAR" and optional[1:] == registry_events, optional
    required = [i[0] for i in addon.event_items(bpy.context, False)]
    assert required == registry_events, required
    assert addon.chosen_event(types.SimpleNamespace(event="ALL_YEAR")) == ""
    assert addon.chosen_event(types.SimpleNamespace(event="halloween")) == "halloween"
    assert bpy.ops.object.tt_new_event(event_name=" Easter_2 ") == {"FINISHED"}
    assert "easter_2" in [i[0] for i in addon.event_items(bpy.context, False)]


@test
def new_event_reopens_its_form_with_the_values_it_had_and_the_new_event_picked():
    addon._reopen["object.tt_new_prop"] = {"mesh": "test_fence", "event": "midsummer", "make_texture": False}
    form = types.SimpleNamespace(bl_idname="object.tt_new_prop", mesh="", event="ALL_YEAR", make_texture=True)
    popped = []
    context = types.SimpleNamespace(window_manager=types.SimpleNamespace(invoke_popup=popped.append))
    addon.open_form(form, context)
    assert popped == [form] and (form.mesh, form.event, form.make_texture) == ("test_fence", "midsummer", False)
    assert not addon._reopen


@test
def a_form_greys_out_ok_until_it_is_valid():
    load_building("vikings", "quarters_start")
    fence = fixture_mesh("test_form_fence", fixture_image("test_form_fence_tex"), z=1.0, kind="cube")

    def form(mesh):
        layout = Recorder()
        addon.NewProp.draw(types.SimpleNamespace(layout=layout, mesh=mesh, event="ALL_YEAR", make_texture=True,
                                                 base="quarters_start",
                                                 bl_idname=addon.NewProp.bl_idname, bl_label=addon.NewProp.bl_label,
                                                 properties=types.SimpleNamespace(
                                                     bl_rna=bpy.ops.object.tt_new_prop.get_rna_type())), bpy.context)
        return layout.log

    blank, typo, picked = form(""), form("no_such_mesh"), form(fence.name)
    assert ("confirm", "object.tt_new_prop", "OK", False) in blank, blank
    assert not [entry for entry in blank if entry[0] == "label" and "Pick" in entry[1]], "an obvious gap was explained"
    assert ("confirm", "object.tt_new_prop", "OK", False) in typo and ("label", "Pick one of your own meshes") in typo
    assert ("confirm", "object.tt_new_prop", "OK", True) in picked, picked
    assert ("confirm", "object.tt_new_event", " ", True) in picked, "no + next to the event"
    bpy.data.objects.remove(fence)


def form(cls, **values):
    layout = Recorder()
    rna = getattr(bpy.ops.object, cls.bl_idname.split(".")[1]).get_rna_type()
    cls.draw(types.SimpleNamespace(layout=layout, bl_idname=cls.bl_idname, bl_label=cls.bl_label,
                                   properties=types.SimpleNamespace(bl_rna=rna), **values), bpy.context)
    return layout.log


@test
def the_skin_and_decoration_forms_and_the_list_rows_offer_what_they_should():
    load_building("vikings", "quarters")
    skin = dict(skin_name="", item="", mesh="", snap=True, make_texture=True, event="ALL_YEAR")
    assert ("confirm", "object.tt_new_skin", "OK", False) in form(addon.NewSkin, **skin)
    assert ("confirm", "object.tt_new_skin", "OK", True) in form(addon.NewSkin, **{**skin, "skin_name": "red"})
    assert bpy.ops.wm.tt_load_unit(group="misc", sprite="oak_tree_crown") == {"FINISHED"}
    assert ("confirm", "object.tt_new_skin", "OK", False) in form(addon.NewSkin, **{**skin, "skin_name": "red"}), \
        "a skin nobody can own went through without an event"
    assert ("confirm", "object.tt_new_skin", "OK", True) in form(addon.NewSkin, **{**skin, "skin_name": "red",
                                                                                    "event": "halloween"})
    decoration = dict(mesh="", sprite_name="", scatter=True, group="misc", low_detail="", half_built="",
                      half_built_low="", start="", start_low="", grass=True, dirt=False, beach=False, snow=False, land=False, count=20,
                      event="ALL_YEAR")
    assert ("confirm", "object.tt_register_model", "OK", False) in form(addon.RegisterModel, **decoration)
    own = fixture_mesh("test_form_patch", fixture_image("test_form_patch_tex"), kind="cube")
    picked = form(addon.RegisterModel, **{**decoration, "mesh": own.name, "sprite_name": own.name})
    assert ("confirm", "object.tt_register_model", "OK", True) in picked and \
           ("confirm", "object.tt_new_event", " ", True) in picked, picked
    assert ("label", "Building stages (optional)") not in picked, "a scattered model offered building stages"
    model = form(addon.RegisterModel, **{**decoration, "mesh": own.name, "sprite_name": own.name, "scatter": False})
    assert ("confirm", "object.tt_register_model", "OK", True) in model and ("label", "Static model") in model
    assert not [x for x in model if x[:2] == ("confirm", "object.tt_new_event")], "a plain model offered an event"
    bpy.data.objects.remove(own)
    wm.tt_category = "BUILDINGS"
    row = Recorder()
    addon.TT_UL_units.draw_item(None, bpy.context, row, wm, wm.tt_units[0], 0, None, "", 0)
    assert [x[0] for x in row.log] == ["wm.tt_pick_unit"]
    assert bpy.ops.wm.tt_pick_unit(group="vikings", sprite="quarters") == {"FINISHED"}
    assert bpy.ops.object.tt_paint_skin(skin="") == {"FINISHED"}
    assert bpy.context.mode == "PAINT_TEXTURE" and addon.loaded_body()["tt_sprite"] == "quarters"
    assert bpy.ops.object.tt_done_painting() == {"FINISHED"}
    row = Recorder()
    flag = next(o for o in addon.building_props(addon.prop_body(bpy.context)) if o["tt_sprite"] == "quarters_test_flag")
    addon.TT_UL_items.draw_item(None, bpy.context, row, bpy.data, flag, 0, None, "", 0)
    assert [x[0] for x in row.log] == ["object.tt_show_item", "label", "label", "object.tt_paint_item",
                                       "object.tt_remove_from_registry"], row.log
    wm.tt_category = "UNITS"


@test
def the_new_prop_form_publishes_on_the_showing_stage_with_a_picked_event():
    body = load_building("vikings", "quarters_start")
    listed = drawn(addon.VIEW3D_PT_tt_attachments)
    assert "object.tt_new_prop" in [idname for idname, *_ in listed] and ("list", "objects") in listed, listed
    fence = fixture_mesh("test_fence", fixture_image("test_fence_tex"), z=1.0, kind="cube")
    assert bpy.ops.object.tt_new_prop(mesh=fence.name, event="halloween") == {"FINISHED"}
    e = entry("vikings", "quarters_start_test_fence")
    assert (e["base"], e["slot"], e["event"]) == ("quarters_start", "prop", "halloween"), e
    assert [addon.prop_tag(o) for o in addon.building_props(body)] == ["Start, halloween"]


@test
def a_building_has_no_panel_of_its_own_and_lists_its_props_in_the_props_panel():
    body = load_building("vikings", "quarters")
    assert not hasattr(addon, "VIEW3D_PT_tt_building") and addon.VIEW3D_PT_tt_attachments.poll(bpy.context)
    fake = type("List", (), {"bitflag_filter_item": 1 << 30})()
    flags, _ = addon.TT_UL_items.filter_items(fake, bpy.context, bpy.data, "objects")
    listed = sorted(o["tt_sprite"] for o, flag in zip(bpy.data.objects, flags) if flag)
    assert listed == ["quarters_test_flag", "quarters_test_lantern"], listed
    listed = [idname for idname, *_ in drawn(addon.VIEW3D_PT_tt_attachments)]
    assert "object.tt_new_prop" in listed and "object.tt_new_item" not in listed and "object.tt_save_props" not in listed
    flag = next(o for o in addon.building_props(body) if o["tt_sprite"] == "quarters_test_flag")
    assert bpy.ops.object.tt_show_item(item=flag.name) == {"FINISHED"} and not addon.item_shown(flag)
    assert bpy.ops.object.tt_show_item(item=flag.name) == {"FINISHED"} and addon.item_shown(flag)
    wm.tt_item_index = list(bpy.data.objects).index(flag)
    assert addon.open_item(bpy.context) == flag
    detail = drawn(addon.VIEW3D_PT_tt_attachments)
    assert detail[0] == ("object.tt_close_item", None) and ("label", "Skins of quarters_test_flag") in detail, detail
    assert ("label", "Built, All year") in detail and ("object.tt_new_skin", None) in detail, detail
    assert bpy.ops.object.tt_close_item() == {"FINISHED"}


@test
def register_new_model_uses_the_picked_mesh_not_the_active_one():
    addon.clear_browser_objects()
    picked = fixture_mesh("test_picked", fixture_image("test_picked_tex"), 0, "cube")
    active = fixture_mesh("test_active", fixture_image("test_active_tex"), 3, "cube")
    select_only(active)
    assert bpy.ops.object.tt_register_model(mesh=picked.name, sprite_name="test_picked_model",
                                            group="misc") == {"FINISHED"}
    assert entry("misc", "test_picked_model")["textures"][0] == [("test_picked_tex", "")]
    assert bpy.context.active_object == active
    expect_error(lambda: bpy.ops.object.tt_register_model(sprite_name="test_nothing", group="misc"), "own meshes")
    for o in (picked, active):
        bpy.data.objects.remove(o)


@test
def new_model_scatters_a_decoration_only_when_asked():
    addon.clear_browser_objects()
    mesh = fixture_mesh("test_either", fixture_image("test_either_tex"), 0, "cube")
    assert "object.tt_register_model" in [idname for idname, *_ in drawn(addon.VIEW3D_PT_tt_units)]
    assert addon.RegisterModel.bl_label == "New Model..." and not hasattr(addon, "NewDecoration")
    assert addon.NewItem.bl_label == addon.NewProp.bl_label == "New Prop..."
    assert bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="test_either_model", group="misc",
                                            snow=True, count=5) == {"FINISHED"}
    assert bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="test_either_scatter", scatter=True,
                                            grass=False, dirt=False, beach=False, snow=True, land=False,
                                            count=5) == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="test_either_model">' in text, "a model that is not scattered got a decoration"
    assert '<sprite name="test_either_scatter" decoration="snow" count="5">' in text
    bpy.data.objects.remove(mesh)


@test
def the_models_panel_comes_first():
    panels = sorted((cls.bl_order, cls.__name__) for cls in addon.classes
                    if issubclass(cls, bpy.types.Panel) and getattr(cls, "bl_category", "") == "Tribal Trouble")
    assert [name for _, name in panels] == ["VIEW3D_PT_tt_units", "VIEW3D_PT_tt_skins",
                                            "VIEW3D_PT_tt_preview", "VIEW3D_PT_tt_attachments",
                                            "VIEW3D_PT_tt_attachments_more"], panels
    assert len({order for order, _ in panels}) == len(panels)
    return ", ".join(f"{order} {name}" for order, name in panels)

@test
def one_publish_writes_the_mesh_a_new_prop_and_the_clips_without_a_form():
    a = load("natives", "peon")
    bpy.context.view_layer.objects.active = a
    listed = [idname for idname, *_ in drawn(addon.VIEW3D_PT_tt_units)]
    assert "wm.tt_publish_model" not in listed and "object.tt_preflight" in listed, listed
    listed = [idname for idname, *_ in drawn(addon.VIEW3D_PT_tt_preview)]
    assert "object.tt_new_clip" in listed, listed
    item_form = form(addon.NewItem, point="HEAD", mesh="", snap=True, make_texture=True, event="ALL_YEAR",
                     on_by_default=False)
    assert ("confirm", "object.tt_new_event", " ", True) in item_form, "no event in the New Prop form"
    assert "invoke" not in vars(addon.PublishModel) and "draw" not in vars(addon.PublishModel)
    body = addon.browsed_unit(a)
    clips = {name: os.path.join(GEOMETRY, relative) for name, (_, _, relative) in entry("natives", "peon")["clip_info"].items()}
    edited = next(iter(clips))
    action = next(x for x in addon.armature_actions(a) if x["tt_clip"] == os.path.basename(clips[edited]))
    assert bpy.ops.object.tt_set_clip(clip=action.name) == {"FINISHED"}
    bone = a.pose.bones[0]
    bone.rotation_mode = "QUATERNION"
    bone.rotation_quaternion = (0.9, 0.0, 0.0, 0.43)
    bone.keyframe_insert("rotation_quaternion", frame=2)
    body.data.vertices[0].co.z += 0.1
    hat = fixture_mesh("test_one_publish_hat", fixture_image("test_one_publish_hat_tex"))
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_item(point="HEAD", mesh=hat.name, event="halloween",
                                      on_by_default=True) == {"FINISHED"}
    assert bpy.ops.object.tt_new_clip(clip_name="bow", kind="plain", wpc=2.0) == {"FINISHED"}
    for path in list(clips.values()) + [body["tt_source"]]:
        os.utime(path, (1, 1))
    assert bpy.ops.wm.tt_publish_model("INVOKE_DEFAULT") == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="peon_test_one_publish_hat" base="peon" slot="hat" event="halloween" default="true">' in text
    assert os.path.getmtime(body["tt_source"]) != 1, "the changed mesh was not written"
    assert [name for name, path in clips.items() if os.path.getmtime(path) != 1] == [edited]
    assert entry("natives", "peon")["clip_info"]["bow"][:2] == ("2", "plain")
    for path in list(clips.values()) + [body["tt_source"]]:
        os.utime(path, (1, 1))
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert all(os.path.getmtime(path) == 1 for path in list(clips.values()) + [body["tt_source"]]), "wrote again"
    assert bpy.ops.object.tt_delete_clip(clip=a.animation_data.action.name) == {"FINISHED"}


@test
def publish_writes_paint_on_a_loaded_item_but_never_paint_a_skin_was_made_with():
    load("vikings", "peon")
    image = addon.mesh_texture_image(hammer())
    png = os.path.join(MODELS, "viking_peon_hammer.png")
    before = open(png, "rb").read()

    def paint():
        image.pixels = [0.1, 0.8, 0.1, 1.0] * (image.size[0] * image.size[1])
        image.update()
        assert image.is_dirty

    try:
        assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="scratched") == {"FINISHED"}
        paint()
        assert bpy.ops.wm.tt_publish_model() == {"FINISHED"} and open(png, "rb").read() == before, \
            "the paint of a skin being made went into the default texture"
        assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"} and not image.is_dirty, "Cancel kept the paint"
        paint()
        assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
        assert open(png, "rb").read() != before and not image.is_dirty, "the item's paint was not written"
    finally:
        with open(png, "wb") as f:
            f.write(before)
        image.reload()


@test
def loading_another_model_clears_the_checks_of_the_last_one():
    load("vikings", "warrior")
    horn = fixture_mesh("test_stale_check_horn", None)
    put_on_head(horn)
    expect_error(lambda: bpy.ops.object.tt_preflight(), "problem(s) to fix")
    assert len(wm.tt_checks)
    load("vikings", "peon")
    assert not wm.tt_checked and not len(wm.tt_checks), [c.name for c in wm.tt_checks]
    bpy.data.objects.remove(horn)


@test
def an_action_editor_copy_of_a_clip_publishes_and_deletes_as_a_clip_of_its_own():
    a = load("vikings", "peon")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    run_path = os.path.join(GEOMETRY, "vikings", "peon", "peon_run.xml")
    stock, before = open(run_path, "rb").read(), clip_lines("vikings", "peon")
    untouched, sprint = run.copy(), run.copy()
    sprint.name = "peon_sprint"
    assert sprint["tt_clip"] == "peon_run.xml"
    assert bpy.ops.object.tt_set_clip(clip=sprint.name) == {"FINISHED"}
    head = a.pose.bones["peon Head"]
    head.rotation_mode = "QUATERNION"
    head.rotation_quaternion = (0.9, 0.0, 0.0, 0.43)
    head.keyframe_insert("rotation_quaternion", frame=5)
    assert addon.publish_changed_clips(bpy.context, a, raise_errors) == ["peon_sprint.xml"]
    assert open(run_path, "rb").read() == stock, "the copy was written over the clip it was copied from"
    assert clip_lines("vikings", "peon") == before + [("sprint", ("1", "loop", "vikings/peon/peon_sprint.xml"))]
    assert bpy.ops.object.tt_delete_clip(clip=untouched.name) == {"FINISHED"}
    assert clip_lines("vikings", "peon")[:-1] == before and os.path.isfile(run_path), "deleting a copy took the original"
    assert bpy.ops.object.tt_delete_clip(clip="peon_sprint") == {"FINISHED"}
    assert clip_lines("vikings", "peon") == before


@test
def publish_while_a_skin_is_being_made_leaves_the_default_model_alone():
    a = load("vikings", "warrior")
    body = addon.browsed_unit(a)
    stock = file_bytes(WARRIOR_FILES)
    assert bpy.ops.object.tt_new_skin(skin_name="lumpy") == {"FINISHED"}
    body.data.vertices[0].co.z += 0.1
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert file_bytes(WARRIOR_FILES) == stock, "Publish wrote a skin being made into the default body"
    assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"}


@test
def a_new_prop_leaves_the_files_of_unchanged_props_alone():
    body = load_building("vikings", "quarters")
    shown = [o for o in addon.building_props(body) if addon.item_shown(o)]
    assert shown, "needs the props saved by the earlier tests"
    for o in shown:
        os.utime(o["tt_source"], (1, 1))
    assert publish_prop(fixture_mesh("test_bell", fixture_image("test_bell_tex"), z=6.0, kind="cube")) == {"FINISHED"}
    assert all(os.path.getmtime(o["tt_source"]) == 1 for o in shown), "a new prop rewrote the ones already there"


@test
def a_vertex_group_that_is_no_bone_is_never_written_as_one():
    body = addon.browsed_unit(load("vikings", "warrior"))
    body.vertex_groups.new(name="mask").add(list(range(len(body.data.vertices))), 1.0, "REPLACE")
    text, _ = addon.export_texts(bpy.context, arm(), [body])[body]
    assert "mask" not in set(re.findall(r'<skin bone="([^"]+)"', text))
    hut = load_building("vikings", "quarters")
    hut.vertex_groups.new(name="mask").add([0, 1, 2], 1.0, "REPLACE")
    text, _ = addon.item_export(hut, bpy.context.evaluated_depsgraph_get())
    assert set(re.findall(r'<skin bone="([^"]+)"', text)) == {addon.STATIC_BONE}


@test
def picking_the_same_row_after_a_category_change_loads_the_model_in_it():
    wm.tt_category = "UNITS"
    assert bpy.ops.wm.tt_pick_unit(group="vikings", sprite="peon") == {"FINISHED"}
    row = wm.tt_unit_index
    assert addon.loaded_body()["tt_sprite"] == "peon"
    wm.tt_category = "BUILDINGS"
    assert wm.tt_unit_index == -1 and addon.loaded_body()["tt_sprite"] == "peon"
    group, sprite = wm.tt_units[row].group, wm.tt_units[row].sprite
    assert bpy.ops.wm.tt_pick_unit(group=group, sprite=sprite) == {"FINISHED"}
    assert addon.loaded_body()["tt_sprite"] == sprite, "the row showed a model that never loaded"
    wm.tt_category = "ALL"
    assert (wm.tt_units[wm.tt_unit_index].group, wm.tt_units[wm.tt_unit_index].sprite) == (group, sprite)
    wm.tt_category = "UNITS"


@test
def loading_another_model_ends_the_skin_an_artists_mesh_stood_in_for():
    a = load("vikings", "peon")
    own = fixture_mesh("stand_in_hammer", None, kind="cube")
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="stale", mesh=own.name) == {"FINISHED"}
    assert own.get("tt_skin_mesh") == hammer().name
    load("vikings", "peon")
    assert "tt_skin_mesh" not in own and addon.skin_mesh(hammer()) is None
    bpy.data.objects.remove(own)


@test
def split_by_bone_keeps_a_bone_parented_part_where_it_was():
    a = load("vikings", "peon")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    bpy.context.scene.frame_set(5)
    cube = fixture_mesh("test_split_cube", None, kind="cube")
    put_on_head(cube)
    cube.vertex_groups.new(name="top").add([v.index for v in cube.data.vertices if v.co.z > 0], 1.0, "REPLACE")
    select_only(cube)
    bpy.context.view_layer.update()
    world = cube.matrix_world.copy()
    assert bpy.ops.object.tt_split_by_bone(bone="top", part_name="test_split_top") == {"FINISHED"}
    part = bpy.data.objects["test_split_top"]
    bpy.context.view_layer.update()
    assert part.parent == cube.parent and part.parent_bone == cube.parent_bone
    worst = max(abs(x - y) for r1, r2 in zip(part.matrix_world, world) for x, y in zip(r1, r2))
    assert worst < 1e-5, worst


@test
def a_model_with_skins_cannot_leave_the_registry_before_them():
    expect_error(lambda: bpy.ops.object.tt_remove_from_registry(group="vikings", sprite="peon_hammer"), "has the skins")
    assert entry("vikings", "peon_hammer") is not None


@test
def an_edited_copy_still_named_after_its_clip_is_refused_and_never_written_over_it():
    a = load("vikings", "peon")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    run_path = os.path.join(GEOMETRY, "vikings", "peon", "peon_run.xml")
    stock, before = open(run_path, "rb").read(), clip_lines("vikings", "peon")
    copy = run.copy()
    assert copy.name == "peon_run.001"
    assert bpy.ops.object.tt_set_clip(clip=copy.name) == {"FINISHED"}
    head = a.pose.bones["peon Head"]
    head.rotation_mode = "QUATERNION"
    head.rotation_quaternion = (0.9, 0.0, 0.0, 0.43)
    head.keyframe_insert("rotation_quaternion", frame=5)
    expect_error(lambda: addon.publish_changed_clips(bpy.context, a, raise_errors), "Rename the clip 'run.001'")
    assert open(run_path, "rb").read() == stock and clip_lines("vikings", "peon") == before


@test
def a_body_skin_refuses_the_artists_own_mesh_with_a_reason():
    a = load("vikings", "warrior")
    own = fixture_mesh("test_new_body", None, kind="cube")
    bpy.context.view_layer.objects.active = a
    expect_error(lambda: bpy.ops.object.tt_new_skin(skin_name="blocky", mesh=own.name), "can only reshape it")
    bpy.data.objects.remove(own)


def file_bounds(path):
    obj = addon.import_mesh_file(bpy.context, path, False, False, lambda k, m: None)
    points = [v.co for v in obj.data.vertices]
    bpy.data.objects.remove(obj)
    return [(min(p[i] for p in points), max(p[i] for p in points)) for i in range(3)]


def own_rig(name, clips=("run",)):
    """The peon's mesh bound to a copy of its rig the registry never saw, with these clips."""
    addon.clear_browser_objects()
    src = os.path.join(GEOMETRY, "vikings", "peon")
    mesh = addon.import_mesh_file(bpy.context, os.path.join(src, "peon_mesh.xml"), False, False, lambda k, m: None)
    mesh.name = name
    mesh["tt_texture"] = "native_warrior_rock"
    parents, rest = addon.read_skeleton(os.path.join(src, "peon_skeleton.xml"))
    rig = addon.build_armature(bpy.context, name, parents, rest)
    addon.bind_meshes(rig, [mesh])
    for clip in clips:
        addon.apply_clip(bpy.context, rig, f"{name}_{clip}", addon.read_animation(os.path.join(src, f"peon_{clip}.xml")))
    select_only(mesh)
    return mesh, rig


@test
def a_new_rig_moved_in_the_scene_writes_its_mesh_where_its_skeleton_is():
    mesh, rig = own_rig("imp")
    rig.location.x = 5.0
    assert bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="imp", group="misc") == {"FINISHED"}
    written = file_bounds(os.path.join(GEOMETRY, "misc", "imp", "imp.xml"))
    stock = file_bounds(os.path.join(GEOMETRY, "vikings", "peon", "peon_mesh.xml"))
    assert all(abs(a - b) < 1e-4 for w, s in zip(written, stock) for a, b in zip(w, s)), (written, stock)


@test
def a_new_rig_registers_every_action_keyed_on_its_bones():
    mesh, rig = own_rig("ogre", ("run", "attack"))
    for action in bpy.data.actions:
        if action.name.startswith("ogre_"):
            del action["tt_armature"]  # actions made in Blender carry no tag
    assert bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="ogre", group="misc") == {"FINISHED"}
    assert sorted(entry("misc", "ogre")["clips"]) == ["misc/ogre/ogre_attack.xml", "misc/ogre/ogre_run.xml"]
    for action in [a for a in bpy.data.actions if a.name.startswith("ogre_")]:
        bpy.data.actions.remove(action)  # untagged, they would count for any later rig on the same bone names


@test
def a_new_rig_without_any_action_is_refused():
    mesh, rig = own_rig("wraith", ())
    expect_error(lambda: bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="wraith", group="misc"),
                 "has no action")
    assert entry("misc", "wraith") is None


@test
def an_image_made_in_blender_keeps_its_pixels_in_the_blend_file_once_saved():
    image = fixture_image("test_saved_here")
    target = os.path.join(MODELS, "test_saved_here.png")
    for colour in ((1.0, 0.5, 0.0, 1.0), (0.0, 0.5, 1.0, 1.0)):
        image.pixels = colour * (64 * 64)
        addon.save_png(image, target)
        assert image.filepath_raw == "" and image.packed_file is not None and os.path.isfile(target)
        image.reload()
        assert tuple(round(x, 2) for x in image.pixels[:4]) == colour, "the packed pixels are not the saved ones"


@test
def a_model_gone_from_geometry_xml_says_so_when_loaded_or_added():
    row = wm.tt_units.add()  # a list filled before geometry.xml lost the model
    row.name, row.group, row.sprite = "vikings / test_gone", "vikings", "test_gone"
    expect_error(lambda: bpy.ops.wm.tt_load_unit(group="vikings", sprite="test_gone"), "no longer in geometry.xml")
    expect_error(lambda: bpy.ops.wm.tt_add_to_scene(group="vikings", sprite="test_gone"), "no longer in geometry.xml")
    addon.refresh_units(bpy.context)


@test
def split_by_bone_keeps_the_groups_that_are_no_bone():
    body = addon.browsed_unit(load("vikings", "warrior"))
    weighted = [body.vertex_groups[g.group].name for v in body.data.vertices for g in v.groups if g.weight >= 0.5]
    bone = max(set(weighted), key=weighted.count)  # earlier tests split some bones off for good
    body.vertex_groups.new(name="mask").add(list(range(len(body.data.vertices))), 1.0, "REPLACE")
    select_only(body)
    assert bpy.ops.object.tt_split_by_bone(bone=bone, part_name="test_split_part") == {"FINISHED"}
    part = bpy.data.objects["test_split_part"]
    assert body.vertex_groups.get("mask") is not None and part.vertex_groups.get("mask") is not None


@test
def moving_the_loaded_rig_gives_publish_nothing_to_write():
    a = load("vikings", "warrior")
    assert any(o.parent_type == "BONE" for objs in addon.unit_items(a).values() for o in objs), "needs a bone item"
    a.location.x = 5.0
    before = mtimes()
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert changed_files(before) == [], changed_files(before)


@test
def publishing_a_new_item_while_a_skin_is_painted_leaves_the_default_texture_alone():
    a = load("vikings", "warrior")
    body = addon.browsed_unit(a)
    image = addon.mesh_texture_image(body)
    cap = fixture_mesh("test_atlas_cap", None)
    cap.data.materials.append(body.data.materials[0])  # an item painted on the unit's atlas
    put_on_head(cap)
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert addon.item_shown(cap) and addon.mesh_texture_image(cap) == image
    png = os.path.join(MODELS, addon.image_texture_name(image) + ".png")
    stock = file_bytes([png])
    assert bpy.ops.object.tt_new_skin(skin_name="glossy") == {"FINISHED"}
    image.pixels[0] = 0.25
    assert image.is_dirty
    put_on_head(fixture_mesh("test_glossy_hat", fixture_image("test_glossy_hat_tex")))
    try:
        assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
        assert file_bytes([png]) == stock, "the skin's paint went into the default texture"
    finally:
        bpy.ops.object.tt_cancel_skin()


@test
def clicking_a_row_whose_model_left_geometry_xml_refreshes_the_list():
    load("vikings", "peon")
    row = wm.tt_units.add()
    row.name, row.group, row.sprite = "vikings / test_gone", "vikings", "test_gone"
    wm.tt_unit_index = len(wm.tt_units) - 1
    assert "test_gone" not in [u.sprite for u in wm.tt_units], "the row out of date is still listed"
    assert addon.loaded_body()["tt_sprite"] == "peon"


@test
def a_skin_that_changes_nothing_is_refused_even_when_the_model_has_warnings():
    load("vikings", "warrior")
    skins = sys.modules["io_tribaltrouble.skins"]
    check_mesh = skins.check_mesh
    skins.check_mesh = lambda obj, is_new, body_triangles: [("WARNING", "a warning the stock model has")]
    try:
        expect_error(lambda: save_skin("same"), "nothing differs from the default model")
        assert entry("vikings", "warrior_same") is None
    finally:
        skins.check_mesh = check_mesh
        bpy.ops.object.tt_cancel_skin()


@test
def the_folder_install_asks_for_the_same_blender_as_the_extension():
    text = open(os.path.join(REPO, "tools", "blender", "build_extension.py"), encoding="utf-8").read()
    minimum = tuple(int(x) for x in re.search(r'blender_version_min = "([\d.]+)"', text).group(1).split("."))
    assert addon.bl_info["blender"] == minimum, (addon.bl_info["blender"], minimum)


@test
def save_skin_gives_the_artists_mesh_back_its_own_texture_name():
    a = load("vikings", "peon")
    own = fixture_mesh("heavy_hammer", fixture_image("heavy_hammer_tex"), kind="cube")
    own["tt_texture"] = "heavy_hammer_tex"
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_new_skin(item=hammer().name, skin_name="heavy", mesh=own.name) == {"FINISHED"}
    assert bpy.ops.object.tt_save_skin() == {"FINISHED"}
    assert own.get("tt_texture") == "heavy_hammer_tex"


@test
def an_artists_mesh_on_the_rig_keeps_its_place_when_another_model_loads():
    a = load("vikings", "peon")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    bpy.context.scene.frame_set(5)
    horn = fixture_mesh("test_kept_horn", None)
    put_on_head(horn)
    bpy.context.view_layer.update()
    world = horn.matrix_world.copy()
    load("vikings", "warrior")
    bpy.context.view_layer.update()
    worst = max(abs(x - y) for r1, r2 in zip(horn.matrix_world, world) for x, y in zip(r1, r2))
    assert horn.parent is None and worst < 1e-5, worst
    bpy.data.objects.remove(horn)


@test
def a_new_mesh_whose_image_is_named_like_another_models_texture_is_refused():
    load("vikings", "peon")
    image = fixture_image("test_clash_tex")
    image.filepath_raw, image.file_format = os.path.join(TEMP, "viking_peon_hammer.png"), "PNG"
    image.save()  # the artist's own file, named like the repo's hammer texture
    stock = file_bytes([os.path.join(MODELS, "viking_peon_hammer.png")])
    hat = fixture_mesh("test_clash_hat", image)
    put_on_head(hat)
    expect_error(lambda: bpy.ops.wm.tt_publish_model(), "problem(s)")
    assert any("another model's texture" in c.name for c in wm.tt_checks), [c.name for c in wm.tt_checks]
    assert file_bytes([os.path.join(MODELS, "viking_peon_hammer.png")]) == stock
    bpy.data.objects.remove(hat)


@test
def a_mesh_bent_by_a_rig_it_is_not_parented_to_registers_as_a_unit():
    mesh, rig = own_rig("gnome")
    mesh.parent = None  # an Armature modifier alone
    assert bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="gnome", group="misc") == {"FINISHED"}
    assert entry("misc", "gnome")["skeleton"] == "misc/gnome/gnome_skeleton.xml"
    bones = set(re.findall(r'<skin bone="([^"]+)"', open(os.path.join(GEOMETRY, "misc", "gnome", "gnome.xml")).read()))
    assert addon.STATIC_BONE not in bones and len(bones) > 1, bones


@test
def one_mesh_on_two_points_is_refused():
    a = load("vikings", "peon")
    horns = fixture_mesh("test_twice_horns", fixture_image("test_twice_horns_tex"))
    put_on_head(horns)
    next(s for s in a.tt_attachments if s.point != "HEAD").obj = horns
    expect_error(lambda: bpy.ops.object.tt_preflight(), "problem(s) to fix")
    assert any("is on two points" in c.name for c in wm.tt_checks), [c.name for c in wm.tt_checks]
    bpy.data.objects.remove(horns)


@test
def a_texture_made_for_one_mesh_leaves_a_mesh_sharing_its_material_alone():
    first, second = fixture_mesh("test_twin_a", None), fixture_mesh("test_twin_b", None)
    shared = bpy.data.materials.new("test_twin_mat")
    first.data.materials.append(shared)
    second.data.materials.append(shared)
    assert bpy.ops.object.tt_make_texture(target=first.name) == {"FINISHED"}
    assert addon.mesh_texture_image(first) is not None and addon.mesh_texture_image(second) is None
    assert second.active_material == shared
    for o in (first, second):
        bpy.data.objects.remove(o)


@test
def an_image_made_in_blender_and_named_like_another_models_texture_is_refused():
    load("vikings", "warrior")
    image = fixture_image("viking_peon_hammer")
    assert addon.image_texture_name(image) == "viking_peon_hammer", image.name
    stock = file_bytes([os.path.join(MODELS, "viking_peon_hammer.png")])
    hat = fixture_mesh("test_made_clash_hat", image)
    put_on_head(hat)
    expect_error(lambda: bpy.ops.wm.tt_publish_model(), "problem(s)")
    assert any("another model's texture" in c.name for c in wm.tt_checks), [c.name for c in wm.tt_checks]
    assert file_bytes([os.path.join(MODELS, "viking_peon_hammer.png")]) == stock
    bpy.data.objects.remove(hat)
    bpy.data.images.remove(image)


@test
def a_new_action_named_like_a_clip_of_the_unit_is_refused_and_never_written_over_it():
    a = load("vikings", "peon")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    other, (_, _, relative) = next((n, info) for n, info in entry("vikings", "peon")["clip_info"].items() if n != "run")
    stock = file_bytes([os.path.join(GEOMETRY, relative)])
    copy = run.copy()
    copy.name = other
    assert bpy.ops.object.tt_set_clip(clip=copy.name) == {"FINISHED"}
    head = a.pose.bones["peon Head"]
    head.rotation_mode = "QUATERNION"
    head.rotation_quaternion = (0.9, 0.0, 0.0, 0.43)
    head.keyframe_insert("rotation_quaternion", frame=5)
    expect_error(lambda: addon.publish_changed_clips(bpy.context, a, raise_errors),
                 f"already has a clip called {other}")
    assert file_bytes([os.path.join(GEOMETRY, relative)]) == stock


@test
def a_new_item_on_the_units_atlas_never_carries_a_skins_paint_into_it():
    a = load("vikings", "warrior")
    body = addon.browsed_unit(a)
    image = addon.mesh_texture_image(body)
    png = os.path.join(MODELS, addon.image_texture_name(image) + ".png")
    stock = file_bytes([png])
    assert bpy.ops.object.tt_new_skin(skin_name="matte") == {"FINISHED"}
    image.pixels[0] = 0.3
    assert image.is_dirty
    visor = fixture_mesh("test_atlas_visor", None)
    visor.data.materials.append(body.data.materials[0])  # a new item painted on the unit's atlas
    put_on_head(visor)
    try:
        assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
        assert file_bytes([png]) == stock, "the skin's paint went into the default texture"
    finally:
        bpy.ops.object.tt_cancel_skin()


@test
def a_new_rig_whose_actions_make_bad_or_doubled_clip_names_is_refused():
    mesh, rig = own_rig("troll")
    run = bpy.data.actions["troll_run"]
    run.copy().name = "run"  # a second clip called run
    run.copy().name = "Walk"
    expect_error(lambda: bpy.ops.object.tt_register_model(mesh=mesh.name, sprite_name="troll", group="misc"),
                 "Rename the actions Walk, run, troll_run")
    assert entry("misc", "troll") is None and not os.path.exists(os.path.join(GEOMETRY, "misc", "troll"))


@test
def the_checks_look_at_the_uv_map_the_file_gets():
    cube = fixture_mesh("test_two_maps", fixture_image("test_two_maps_tex"), kind="cube")
    unwrapped = cube.data.uv_layers.new(name="unwrapped")  # a copy of the cube's own unwrap
    cube.data.uv_layers[0].data.foreach_set("uv", [0.5] * (2 * len(cube.data.loops)))
    cube.data.uv_layers.active = unwrapped
    assert any("collapsed" in text for _, text in addon.check_mesh(cube, False, 0))
    bpy.data.objects.remove(cube)


@test
def work_not_yet_published_is_listed_before_another_model_loads():
    a = load("vikings", "warrior")
    assert addon.unpublished_changes(bpy.context) == []
    assert bpy.ops.object.tt_new_clip(clip_name="wave") == {"FINISHED"}
    body = addon.browsed_unit(a)
    body.data.vertices[0].co.z += 0.1
    addon.mesh_texture_image(body).pixels[0] = 0.2
    hat = fixture_mesh("test_pending_hat", fixture_image("test_pending_hat_tex"))
    put_on_head(hat)
    expected = ["1 mesh", "1 clip", "1 new item", "paint"]
    assert addon.unpublished_changes(bpy.context) == expected, addon.unpublished_changes(bpy.context)
    assert bpy.ops.wm.tt_pick_unit(group="vikings", sprite="warrior") == {"FINISHED"}
    assert addon.unpublished_changes(bpy.context) == expected, "picking the model on screen loaded it again"
    assert bpy.ops.object.tt_new_skin(skin_name="pending") == {"FINISHED"}
    assert addon.unpublished_changes(bpy.context)[0] == "the skin 'pending'"
    assert bpy.ops.object.tt_cancel_skin() == {"FINISHED"}
    bpy.data.objects.remove(hat)


print("\n==== ADDON TESTS (Blender %s, addon %s) ====" % (bpy.app.version_string, ".".join(map(str, addon.bl_info["version"]))))
for name, status, detail in results:
    print(f"{status}  {name}" + (f": {detail}" if detail else ""))
failed = sum(1 for r in results if r[1] == "FAIL")
print(f"{len(results) - failed} passed, {failed} failed")
shutil.rmtree(TEMP, ignore_errors=True)
sys.exit(1 if failed else 0)

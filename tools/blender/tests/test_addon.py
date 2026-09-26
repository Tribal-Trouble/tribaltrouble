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
                "viking_buildings_hi"):
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

spec = importlib.util.spec_from_file_location("io_tribaltrouble", os.path.join(REPO, "tools", "blender", "io_tribaltrouble.py"))
addon = importlib.util.module_from_spec(spec)
sys.modules["io_tribaltrouble"] = addon
spec.loader.exec_module(addon)
addon.register()
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
    expect_error(bpy.ops.object.tt_save_items, "already has a file named after warrior_skeleton")
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
    assert bpy.ops.object.tt_register_model(sprite_name="test_hut", group="vikings", low_detail=far.name, kind="BUILDING",
                                            half_built=half.name, start=site.name) == {"FINISHED"}
    assert entry("vikings", "test_hut")["models"] == ["vikings/test_hut/test_hut.xml", "vikings/test_hut/test_hut_lo.xml"]
    assert entry("vikings", "test_hut_halfbuilt")["models"] == ["vikings/test_hut/test_hut_halfbuilt.xml"]
    assert entry("vikings", "test_hut_start")["models"] == ["vikings/test_hut/test_hut_start.xml"]
    assert all(os.path.isfile(os.path.join(MODELS, t + ".png")) for t in ("test_hut_hi", "test_hut_lo"))
    select_only(built)
    expect_error(lambda: bpy.ops.object.tt_register_model(sprite_name="test_hut", group="vikings"), "already has")
    expect_error(lambda: bpy.ops.object.tt_register_model(sprite_name="bad name", group="vikings"), "letters")
    wm.tt_category = "BUILDINGS"
    assert "vikings / test_hut_start" in [u.name for u in wm.tt_units]
    wm.tt_category = "UNITS"
    assert bpy.ops.wm.tt_load_unit(group="vikings", sprite="test_hut") == {"FINISHED"}
    for o in (built, far, half, site):
        bpy.data.objects.remove(o)


@test
def register_a_map_decoration_with_terrain_count_and_event():
    addon.clear_browser_objects()
    patch = fixture_mesh("pumpkin_patch", fixture_image("test_patch_tex"), 0, "cube")
    stage = fixture_mesh("patch_stage", fixture_image("test_patch_stage"), 3, "cube")
    select_only(patch)
    assert bpy.ops.object.tt_register_model(sprite_name="test_patch", group="misc", kind="DECORATION", ground="grass",
                                            count=12, event=" Halloween", half_built=stage.name) == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="test_patch" decoration="grass" count="12" event="halloween">' in text
    assert entry("misc", "test_patch")["models"] == ["misc/test_patch/test_patch.xml"]
    assert entry("misc", "test_patch_halfbuilt") is None, "a decoration has no building stages"
    select_only(patch)
    expect_error(lambda: bpy.ops.object.tt_register_model(sprite_name="test_patch2", group="misc", kind="DECORATION",
                                                          event="bad name"), "letters")
    wm.tt_category = "DECORATIONS"
    assert [u.name for u in wm.tt_units] == ["misc / test_patch"], [u.name for u in wm.tt_units]
    wm.tt_category = "UNITS"
    for o in (patch, stage):
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
    assert bpy.ops.object.tt_register_model(sprite_name="warrior_variant") == {"FINISHED"}
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
    assert bpy.ops.object.tt_register_model(sprite_name="goblin", group="misc") == {"FINISHED"}
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
    source = os.path.join(REPO, "tools", "blender", "io_tribaltrouble.py")
    assert addon.version_from_source(source) == tuple(addon.bl_info["version"])
    repo_copy = os.path.join(TEMP, "tools", "blender", "io_tribaltrouble.py")
    os.makedirs(os.path.dirname(repo_copy))
    text = open(source, encoding="utf-8").read()
    newer = re.sub(r'"version": \(\d+, \d+, \d+\)', '"version": (99, 0, 0)', text, count=1)
    open(repo_copy, "w", encoding="utf-8").write(newer)
    installed = os.path.join(TEMP, "installed_addon.py")
    shutil.copy(source, installed)
    real_file, addon.__file__ = addon.__file__, installed
    try:
        assert addon.update_available(bpy.context) == (99, 0, 0)
        assert addon.install_repo_addon(bpy.context) == (99, 0, 0)
        assert addon.version_from_source(installed) == (99, 0, 0), "the installed file was not replaced"
        open(repo_copy, "w", encoding="utf-8").write(text)
        os.utime(repo_copy, (1, 1))
        assert addon.update_available(bpy.context) is None, "an equal or older repo copy must not offer an update"
    finally:
        addon.__file__ = real_file


def load_building(group, sprite):
    wm.tt_category = "BUILDINGS"
    assert bpy.ops.wm.tt_load_unit(group=group, sprite=sprite) == {"FINISHED"}
    return addon.browsed_building(bpy.context)


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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
    assert sorted(items()["hat"]) == [("peon_test_crown", False), ("peon_test_crown_b", True)], items()["hat"]
    a = load("natives", "peon")
    assert [name for name, _ in items()["hat"]] == ["peon_test_crown", "peon_test_crown_b"], "lost on reload"
    bare = fixture_mesh("test_bare_hat", None)
    put_on_head(bare)
    expect_error(bpy.ops.object.tt_save_items, "problem(s)")
    assert entry("natives", "peon_test_bare_hat") is None, "a refused item still reached the registry"
    bpy.data.objects.remove(bare)


@test
def a_new_item_can_belong_to_an_event_and_be_on_by_default():
    load("natives", "peon")
    put_on_head(fixture_mesh("test_pumpkin_hat", fixture_image("test_pumpkin_hat_tex")))
    assert bpy.ops.object.tt_save_items(event="halloween", on_by_default=True) == {"FINISHED"}
    put_on_head(fixture_mesh("test_plain_hat", fixture_image("test_plain_hat_tex")))
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
    text = open(registry_path, encoding="utf-8").read()
    assert '<sprite name="peon_test_pumpkin_hat" base="peon" slot="hat" event="halloween" default="true">' in text
    assert '<sprite name="peon_test_plain_hat" base="peon" slot="hat">' in text
    assert not entry("natives", "peon_test_plain_hat")["default"] and entry("natives", "peon_test_plain_hat")["event"] == ""
    put_on_head(fixture_mesh("test_odd_hat", fixture_image("test_odd_hat_tex")))
    expect_error(lambda: bpy.ops.object.tt_save_items(event="bad name"), "letters")
    assert entry("natives", "peon_test_odd_hat") is None


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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert addon.VIEW3D_PT_tt_attachments.bl_label == "Carried Items"


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
    assert bpy.ops.object.tt_save_clip(clip_name="dance", kind="loop", wpc=1.0) == {"FINISHED"}
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
def saving_an_existing_clip_replaces_it_in_place():
    a = load("vikings", "warrior")
    run = next(x for x in addon.armature_actions(a) if x.name.endswith("run"))
    assert bpy.ops.object.tt_set_clip(clip=run.name) == {"FINISHED"}
    before = clip_lines("vikings", "warrior")
    wpc, kind, relative = dict(before)["run"]
    path = os.path.join(GEOMETRY, relative)
    frames = len(ET.parse(path).getroot().findall("frame"))
    os.remove(path)
    assert bpy.ops.object.tt_save_clip(clip_name="run", kind=kind, wpc=float(wpc)) == {"FINISHED"}
    assert len(ET.parse(path).getroot().findall("frame")) == frames
    assert clip_lines("vikings", "warrior") == before
    assert bpy.ops.object.tt_save_clip(clip_name="run", kind="loop", wpc=4.5) == {"FINISHED"}
    assert dict(clip_lines("vikings", "warrior"))["run"] == ("4.5", "loop", relative)
    assert [n for n, _ in clip_lines("vikings", "warrior")] == [n for n, _ in before], "clip order changed"
    assert bpy.ops.object.tt_save_clip(clip_name="run", kind=kind, wpc=float(wpc)) == {"FINISHED"}


@test
def a_unit_on_a_borrowed_rig_cannot_change_the_clips():
    load("natives", "warrior_variant")
    expect_error(lambda: bpy.ops.object.tt_save_clip(clip_name="wave", kind="loop", wpc=1.0), "borrows the warrior rig")


def publish_prop(obj, event=""):
    wm.tt_new_prop, wm.tt_prop_event = obj, event
    return bpy.ops.object.tt_save_props()


@test
def props_on_a_building_save_register_and_come_back_on_reload():
    body = load_building("vikings", "quarters")
    assert body is not None and body["tt_sprite"] == "quarters"
    flag = fixture_mesh("test_flag", fixture_image("test_flag_tex"), z=8.0, kind="cube")
    lantern = fixture_mesh("test_lantern", fixture_image("test_lantern_tex"), z=2.0, kind="cube")
    lantern.location.x = 4.0
    assert publish_prop(lantern, " Halloween") == {"FINISHED"}
    assert wm.tt_new_prop is None and wm.tt_prop_event == "", "the picker was not cleared for the next prop"
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
    expect_error(lambda: publish_prop(fixture_mesh("test_bare", None, kind="cube")), "no Image Texture")
    wm.tt_new_prop = None


@test
def a_prop_picked_on_a_stage_is_published_on_that_stage_with_its_event():
    body = load_building("vikings", "quarters_halfbuilt")
    assert addon.building_stage(body["tt_sprite"]) == ("quarters", "Half built")
    assert addon.building_stage("quarters_start") == ("quarters", "Start")
    assert addon.building_props(body) == []
    registered = fixture_mesh("test_crane", fixture_image("test_crane_tex"), z=6.0, kind="cube")
    wm.tt_new_prop = body
    assert addon.new_props(bpy.context, body) == [], "the building itself was taken as a prop"
    expect_error(lambda: publish_prop(registered, "Bad Event"), "letters, digits")
    assert entry("vikings", "quarters_halfbuilt_test_crane") is None
    assert publish_prop(registered, "harvest") == {"FINISHED"}
    e = entry("vikings", "quarters_halfbuilt_test_crane")
    assert (e["base"], e["slot"], e["event"]) == ("quarters_halfbuilt", "prop", "harvest"), e
    assert [addon.prop_tag(o) for o in addon.building_props(body)] == ["Half built, harvest"]
    assert not addon.attachment_obj_poll(None, registered), "a published prop is still offered by the picker"


@test
def event_texture_is_a_copy_listed_on_every_model_that_shares_the_atlas():
    body = load_building("vikings", "quarters")
    before = open(os.path.join(MODELS, "viking_buildings_hi.png"), "rb").read()
    assert not bpy.ops.object.tt_save_event_texture.poll(), "nothing was started yet"
    expect_error(lambda: bpy.ops.object.tt_new_event_texture(event="Hallo ween"), "letters, digits")
    assert bpy.ops.object.tt_new_event_texture(event="halloween", scope="ALL") == {"FINISHED"}
    assert bpy.context.mode == "PAINT_TEXTURE", "the dialog's OK did not start painting"
    image = addon.mesh_texture_image(body)
    assert image.name.startswith("viking_buildings_hi_halloween"), image.name
    assert addon.building_events(body, entry("vikings", "quarters")) == [("halloween", "viking_buildings_hi_halloween")]
    image.pixels[0] = 0.25  # paint
    assert image.is_dirty
    assert bpy.ops.object.tt_save_event_texture() == {"FINISHED"}
    assert not bpy.ops.object.tt_save_event_texture.poll(), "published textures still count as unsaved"
    assert "halloween" in addon.known_events(bpy.context)
    assert open(os.path.join(MODELS, "viking_buildings_hi.png"), "rb").read() == before, "the original was painted"
    assert open(os.path.join(MODELS, "viking_buildings_hi_halloween.png"), "rb").read() != before
    quarters, armory = entry("vikings", "quarters"), entry("vikings", "armory")
    assert quarters["textures"][0] == [("viking_buildings_hi", ""), ("viking_buildings_hi_halloween", "halloween")]
    # Far away the usual texture is kept, in the same place in the list, because nobody painted a low one.
    assert quarters["textures"][1] == [("viking_buildings_lo", ""), ("viking_buildings_lo", "halloween")]
    assert armory["textures"][0][-1] == ("viking_buildings_hi_halloween", "halloween")
    text = open(registry_path, "rb").read().decode("utf-8")
    assert 'name="viking_buildings_hi_halloween" team="viking_buildings_hi_team" event="halloween"' in text
    assert entry("vikings", "warrior")["textures"][0][-1][1] == "", "a model on another atlas was touched"
    count = text.count('event="halloween"/>')
    image.pixels[1] = 0.5  # paint again
    assert bpy.ops.object.tt_save_event_texture() == {"FINISHED"}
    assert open(registry_path, "rb").read().decode("utf-8").count('event="halloween"/>') == count, "listed twice"
    assert bpy.ops.object.tt_show_event_texture(texture="viking_buildings_hi") == {"FINISHED"}
    assert addon.mesh_texture_image(body).name.startswith("viking_buildings_hi.png")
    assert bpy.ops.object.tt_remove_event_texture(event="halloween") == {"FINISHED"}
    assert 'event="halloween"/>' not in open(registry_path, "rb").read().decode("utf-8")


@test
def event_texture_for_one_model_only():
    load_building("vikings", "quarters")
    assert bpy.ops.object.tt_new_event_texture(event="winter", scope="ONLY") == {"FINISHED"}
    assert bpy.ops.object.tt_save_event_texture() == {"FINISHED"}
    assert entry("vikings", "quarters")["textures"][0][-1] == ("viking_buildings_hi_winter", "winter")
    assert all(event == "" for _, event in entry("vikings", "armory")["textures"][0])


@test
def the_event_texture_banner_cancels_unsaved_paint_and_writes_nothing():
    body = load_building("vikings", "quarters")
    registry_before = open(registry_path, "rb").read()
    assert not bpy.ops.object.tt_cancel_event_texture.poll(), "nothing to cancel yet"
    assert bpy.ops.object.tt_new_event_texture(event="spring", scope="ALL") == {"FINISHED"}
    assert list(addon.unsaved_events(body, entry("vikings", "quarters"))) == ["spring"]
    addon.mesh_texture_image(body).pixels[0] = 0.25
    assert bpy.ops.object.tt_cancel_event_texture() == {"FINISHED"}
    assert bpy.context.mode == "OBJECT"
    assert not addon.unsaved_events(body, entry("vikings", "quarters"))
    assert "spring" not in dict(addon.building_events(body, entry("vikings", "quarters")))
    assert addon.mesh_texture_image(body).name.startswith("viking_buildings_hi.png")
    assert open(registry_path, "rb").read() == registry_before
    assert not bpy.ops.object.tt_save_event_texture.poll()


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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_props() == {"FINISHED"}
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


@test
def the_building_panel_shows_only_for_a_building_stage():
    assert bpy.ops.wm.tt_load_unit(group="misc", sprite="jungle_tree_crown") == {"FINISHED"}
    assert addon.loaded_models() and not addon.VIEW3D_PT_tt_building.poll(bpy.context)
    assert bpy.ops.object.tt_save_props.poll() is False
    assert load_building("vikings", "quarters") is not None and addon.VIEW3D_PT_tt_building.poll(bpy.context)


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
    body = addon.browsed_building(bpy.context)
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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
    assert os.path.getmtime(path) == 1, "publishing an untouched item on several bones wrote it"

    assert addon.shares_unit_texture(a, blend)
    assert bpy.ops.object.tt_own_texture(target=blend.name) == {"FINISHED"}
    assert blend["tt_texture"] == "native_chieftain_test_blend"
    bpy.context.view_layer.objects.active = a
    assert bpy.ops.object.tt_save_items() == {"FINISHED"}
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


def converter_skins(geometry):
    """skins.txt as the game's geometry converter writes it for this geometry folder; None without a built
    converter and a JDK that runs it."""
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
            return open(os.path.join(out, "skins.txt"), encoding="utf-8").read().splitlines()
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
    harvest = sorted(line.rsplit(" ", 1)[0] for line in lines if line.split(" ")[1] == "harvest")
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
    assert bpy.ops.object.tt_show_skin(sprite="warrior_bald") == {"FINISHED"}
    assert body["tt_skin"] == low["tt_skin"] == "warrior_bald"
    for o, name in ((body, "warrior_bald.xml"), (low, "warrior_bald_lo.xml")):
        record = addon.mesh_record_from_xml(ET.parse(os.path.join(GEOMETRY, "vikings", "warrior", name)).getroot(),
                                            False)
        assert np.allclose([v.co[:] for v in o.data.vertices], record.verts), f"{o.name} is not showing {name}"
    for path in WARRIOR_FILES:
        os.utime(path, (1, 1))
    assert bpy.ops.wm.tt_publish_model() == {"FINISHED"}
    assert all(os.path.getmtime(path) == 1 for path in WARRIOR_FILES), "Publish wrote a previewed skin as stock"
    assert bpy.ops.object.tt_show_skin(sprite="warrior_gold") == {"FINISHED"}
    assert body.data.materials[0].name == "tt_warrior_gold_rock" and body["tt_texture"].startswith("warrior_gold_rock")
    assert low.data.materials[0].name == "tt_viking_warrior_rock"
    assert bpy.ops.object.tt_new_skin(skin_name="again") == {"FINISHED"}
    assert body["tt_skin_editing"] == "again" and "tt_skin" not in body, "a new skin did not start from Default"
    assert bpy.ops.object.tt_show_skin(sprite="") == {"FINISHED"}
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


print("\n==== ADDON TESTS (Blender %s, addon %s) ====" % (bpy.app.version_string, ".".join(map(str, addon.bl_info["version"]))))
for name, status, detail in results:
    print(f"{status}  {name}" + (f": {detail}" if detail else ""))
failed = sum(1 for r in results if r[1] == "FAIL")
print(f"{len(results) - failed} passed, {failed} failed")
shutil.rmtree(TEMP, ignore_errors=True)
sys.exit(1 if failed else 0)

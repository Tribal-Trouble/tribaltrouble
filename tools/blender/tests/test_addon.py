"""Headless tests for the Tribal Trouble Blender addon. Run from the repo root:

    blender -b --factory-startup --python tools/blender/tests/test_addon.py

--factory-startup keeps an installed copy of the addon from loading next to the one under test. Everything is done
in a temporary copy of assets/geometry, so the repo is never written to. Exit code 0 when every test passes.

The shapes and images the tests make are throwaway fixtures inside that temporary folder. They are never art for
the game and must never be copied into the repo.
"""
import importlib.util
import os
import re
import shutil
import sys
import tempfile
import traceback
import xml.etree.ElementTree as ET

import bpy

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
wm.tt_auto_load = False
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
    wm.tt_units_only = False
    everything = len(wm.tt_units)
    wm.tt_units_only = True
    assert everything > len(units)
    return f"{len(units)} units, {everything} models"


@test
def load_unit_with_default_attachment():
    a = load("vikings", "warrior")
    assert items() == {"weapon": [("warrior_axe_held", True)]}, items()
    assert a.animation_data.action.name.endswith("idle")
    return f"{len(a.data.bones)} bones, {len(addon.armature_actions(a))} clips, axe on by default"


@test
def loading_another_unit_replaces_the_first_but_keeps_user_objects():
    keep = fixture_mesh("my_own_cube", None, kind="cube")
    load("natives", "warrior")
    names = [o.name for o in bpy.data.objects]
    assert "my_own_cube" in names and not any(n.startswith("warrior") for n in names), names
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
    assert items() == {"hat": [("warrior_pumpkin", False), ("warrior_witch", False)]}, items()
    by_sprite = {o["tt_sprite"]: o for o in addon.unit_items(arm())["hat"]}
    assert by_sprite["warrior_pumpkin"].parent_bone == "Head"
    bpy.ops.object.tt_show_item(item=by_sprite["warrior_pumpkin"].name)
    bpy.ops.object.tt_show_item(item=by_sprite["warrior_witch"].name)
    assert items() == {"hat": [("warrior_pumpkin", False), ("warrior_witch", True)]}, items()
    bpy.ops.object.tt_show_item(item=by_sprite["warrior_witch"].name)
    assert items() == {"hat": [("warrior_pumpkin", False), ("warrior_witch", False)]}, items()


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
    for fragment in ("letters, digits and underscores", "no UV map", "no Image Texture", "negative scale", "tint"):
        assert fragment in text, f"missing '{fragment}' in: {text}"
    expect_error(bpy.ops.export_mesh.tt_to_repo, "Not saved")
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
    assert bpy.ops.object.tt_register_model(sprite_name="test_hut", group="vikings", low_detail=far.name,
                                            half_built=half.name, start=site.name) == {"FINISHED"}
    assert entry("vikings", "test_hut")["models"] == ["vikings/test_hut/test_hut.xml", "vikings/test_hut/test_hut_lo.xml"]
    assert entry("vikings", "test_hut_halfbuilt")["models"] == ["vikings/test_hut/test_hut_halfbuilt.xml"]
    assert entry("vikings", "test_hut_start")["models"] == ["vikings/test_hut/test_hut_start.xml"]
    assert all(os.path.isfile(os.path.join(MODELS, t + ".png")) for t in ("test_hut_hi", "test_hut_lo"))
    select_only(built)
    expect_error(lambda: bpy.ops.object.tt_register_model(sprite_name="test_hut", group="vikings"), "already has")
    expect_error(lambda: bpy.ops.object.tt_register_model(sprite_name="bad name", group="vikings"), "letters")
    wm.tt_units_only = False
    assert "vikings / test_hut_start" in [u.name for u in wm.tt_units]
    wm.tt_units_only = True
    assert bpy.ops.wm.tt_load_unit(group="vikings", sprite="test_hut") == {"FINISHED"}
    for o in (built, far, half, site):
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
    wm.tt_units_only = False
    assert bpy.ops.wm.tt_load_unit(group=group, sprite=sprite) == {"FINISHED"}
    return addon.browsed_building(bpy.context)


@test
def carried_items_load_on_the_peon_and_leave_the_model_list():
    wm.tt_units_only = True
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
    assert bpy.ops.object.tt_new_clip(clip_name="dance") == {"FINISHED"}
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
    for sprite in ("peon", "rock_resource", "wood_resource", "rubber_resource", "left_paddle", "right_paddle"):
        lines = clip_lines("vikings", sprite)
        assert lines[:-1] == before, f"{sprite}: existing clip numbers moved"
        assert lines[-1] == ("dance", ("1", "loop", "vikings/peon/peon_dance.xml")), (sprite, lines[-1])
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
    for sprite in ("peon", "rock_resource", "wood_resource", "rubber_resource", "left_paddle", "right_paddle"):
        assert "dance" not in entry("vikings", sprite)["clip_info"], sprite
        assert len(entry("vikings", sprite)["clip_info"]) == 8, sprite
    assert not os.path.exists(os.path.join(GEOMETRY, "vikings", "peon", "peon_dance.xml"))
    assert "peon_dance" not in bpy.data.actions


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


@test
def props_on_a_building_save_register_and_come_back_on_reload():
    body = load_building("vikings", "quarters")
    assert body is not None and body["tt_sprite"] == "quarters"
    wm.tt_event = ""
    flag = fixture_mesh("test_flag", fixture_image("test_flag_tex"), z=8.0, kind="cube")
    wm.tt_event = "Halloween"
    lantern = fixture_mesh("test_lantern", fixture_image("test_lantern_tex"), z=2.0, kind="cube")
    lantern.location.x = 4.0
    select_only(lantern)
    assert bpy.ops.object.tt_save_props() == {"FINISHED"}
    wm.tt_event = ""
    select_only(flag)
    assert bpy.ops.object.tt_save_props() == {"FINISHED"}
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
    xs = [v.co.x for v in props["quarters_test_lantern"].data.vertices]
    assert 3.4 < min(xs) and max(xs) < 4.6, "the prop did not keep its place next to the building"


@test
def a_prop_with_a_taken_name_or_no_texture_is_refused():
    body = load_building("vikings", "quarters")
    for o in addon.building_props(body):
        bpy.data.objects.remove(o)  # frees the object name; the registry entry stays
    select_only(fixture_mesh("test_flag", fixture_image("test_flag_tex2"), kind="cube"))
    expect_error(bpy.ops.object.tt_save_props, "already has a sprite named quarters_test_flag")
    select_only(fixture_mesh("test_bare", None, kind="cube"))
    expect_error(bpy.ops.object.tt_save_props, "no Image Texture")


@test
def event_texture_is_a_copy_listed_on_every_model_that_shares_the_atlas():
    body = load_building("vikings", "quarters")
    wm.tt_event = "halloween"
    before = open(os.path.join(MODELS, "viking_buildings_hi.png"), "rb").read()
    assert bpy.ops.object.tt_new_event_texture() == {"FINISHED"}
    image = addon.mesh_texture_image(body)
    assert image.name.startswith("viking_buildings_hi_halloween"), image.name
    image.pixels[0] = 0.25  # paint
    assert image.is_dirty
    assert bpy.ops.object.tt_save_event_texture(everywhere=True) == {"FINISHED"}
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
    assert bpy.ops.object.tt_save_event_texture(everywhere=True) == {"FINISHED"}
    assert open(registry_path, "rb").read().decode("utf-8").count('event="halloween"/>') == count, "listed twice"
    assert bpy.ops.object.tt_show_event_texture(texture="viking_buildings_hi") == {"FINISHED"}
    assert addon.mesh_texture_image(body).name.startswith("viking_buildings_hi.png")
    assert bpy.ops.object.tt_remove_event_texture() == {"FINISHED"}
    assert 'event="halloween"/>' not in open(registry_path, "rb").read().decode("utf-8")


@test
def event_texture_for_one_model_only():
    load_building("vikings", "quarters")
    wm.tt_event = "winter"
    assert bpy.ops.object.tt_new_event_texture() == {"FINISHED"}
    assert bpy.ops.object.tt_save_event_texture(everywhere=False) == {"FINISHED"}
    assert entry("vikings", "quarters")["textures"][0][-1] == ("viking_buildings_hi_winter", "winter")
    assert all(event == "" for _, event in entry("vikings", "armory")["textures"][0])
    wm.tt_event = ""


@test
def geometry_xml_is_only_ever_appended_to():
    text = open(registry_path, "rb").read().decode("utf-8")
    assert text.startswith(PRISTINE[:PRISTINE.index("<geometry>")]), "DOCTYPE was touched"
    assert ("\r\n" in text) == ("\r\n" in PRISTINE), "line endings changed"
    ET.fromstring(text.encode("utf-8"))
    for line in PRISTINE.splitlines():
        assert line in text, f"an original line went missing: {line[:60]}"


print("\n==== ADDON TESTS (Blender %s, addon %s) ====" % (bpy.app.version_string, ".".join(map(str, addon.bl_info["version"]))))
for name, status, detail in results:
    print(f"{status}  {name}" + (f": {detail}" if detail else ""))
failed = sum(1 for r in results if r[1] == "FAIL")
print(f"{len(results) - failed} passed, {failed} failed")
shutil.rmtree(TEMP, ignore_errors=True)
sys.exit(1 if failed else 0)

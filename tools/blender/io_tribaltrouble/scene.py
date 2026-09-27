"""The models loaded in the scene: loading, what shows, attachments on bones, references and skins."""

import hashlib
import os

import bpy
from bpy.props import StringProperty, BoolProperty, PointerProperty
from mathutils import Vector

from .textures import apply_team_preview, object_texture
from .mesh_io import bone_tail_matrices, import_mesh_file, mesh_xml_text, rest_pose_armatures, STATIC_BONE, write_text
from .rig import (apply_clip, armature_from_file, assign_action, bind_meshes, build_armature, clip_keys, read_animation,
                  read_skeleton, shown_bones)
from .registry import GEOMETRY_DIR, level_textures, read_registry, repo_root, rig_entry, sprite_category, sprite_skins


def attach_object(arm, obj, bone_name, visible):
    """Bone-parent obj keeping its placement relative to the bone as posed right now, expressed against the rest
    pose so the bind pose is well defined whatever frame the user was scrubbing."""
    rest_tail, posed_tail = bone_tail_matrices(arm, bone_name)
    local = posed_tail.inverted() @ obj.matrix_world
    obj.parent = arm
    obj.parent_type = "BONE"
    obj.parent_bone = bone_name
    obj.matrix_parent_inverse = rest_tail.inverted()
    obj.matrix_basis = rest_tail @ local
    obj["tt_bone"] = bone_name
    set_item_visible(obj, visible)


def detach_object(obj):
    world = obj.matrix_world.copy()
    obj.parent = None
    obj.matrix_world = world
    obj.pop("tt_bone", None)
    set_item_visible(obj, True)


def attachment_obj_poll(self, obj):
    """Only the artist's own meshes: items loaded from the registry already have their place and their own buttons."""
    return obj.type == "MESH" and not obj.get(BROWSER_TAG) and not obj.get(REFERENCE_TAG)


def slot_obj_update(self, context):
    arm = self.id_data
    previous = bpy.data.objects.get(self.prev_name) if self.prev_name else None
    if previous is not None and previous != self.obj and previous.parent == arm:
        detach_object(previous)
    if self.obj is not None:
        attach_object(arm, self.obj, self.bone, self.visible)
    self.prev_name = self.obj.name if self.obj is not None else ""


def slot_visible_update(self, context):
    if self.obj is not None:
        set_item_visible(self.obj, self.visible)


class TTAttachmentSlot(bpy.types.PropertyGroup):
    point: StringProperty()
    bone: StringProperty()
    prev_name: StringProperty()
    obj: PointerProperty(type=bpy.types.Object, name="Object", poll=attachment_obj_poll, update=slot_obj_update,
                         description="Mesh to hang off this attachment point")
    visible: BoolProperty(name="Visible", default=True, update=slot_visible_update,
                          description="Show or hide this attachment; only visible ones are exported")


def visible_attachments(arm):
    return [slot.obj for slot in arm.tt_attachments if slot.obj is not None and slot.visible]


BROWSER_TAG = "tt_browser"


def refresh_units(context):
    wm = context.window_manager
    wm.tt_units.clear()
    refresh_skins(context)
    root = repo_root(context)
    if not root:
        return 0
    registry = read_registry(root)
    # Skins are reached through the Skins panel of the model they replace.
    sprites = [(s, sprite_category(registry, s)) for s in registry if not s["slot"] and not s["skin"]]
    for sprite, category in sorted(sprites, key=lambda x: (x[0]["group"], x[0]["name"])):
        if wm.tt_category not in ("ALL", category):
            continue
        item = wm.tt_units.add()
        item.name = f"{sprite['group']} / {sprite['name']}"
        item.group = sprite["group"]
        item.sprite = sprite["name"]
        item.category = category
    body = loaded_body()
    shown = (body["tt_group"], body["tt_sprite"]) if body is not None else None
    wm.tt_unit_index = next((i for i, u in enumerate(wm.tt_units) if (u.group, u.sprite) == shown), -1)
    return len(wm.tt_units)


def clear_browser_objects():
    for obj in [o for o in bpy.data.objects if o.get(BROWSER_TAG)]:
        data = obj.data
        bpy.data.objects.remove(obj, do_unlink=True)
        if data is not None and data.users == 0:
            (bpy.data.meshes if isinstance(data, bpy.types.Mesh) else bpy.data.armatures).remove(data)
    # Clips keep a fake user so they survive saving; that is the only user left once the browsed rig is gone.
    for action in [a for a in bpy.data.actions if a.get(BROWSER_TAG) and a.users <= int(a.use_fake_user)]:
        bpy.data.actions.remove(action)
    for mesh in [o for o in bpy.data.objects if "tt_skin_mesh" in o]:
        del mesh["tt_skin_mesh"]  # the skin it stood in for went with the model


def unit_items(arm):
    """Registry attachments loaded with this unit, grouped by game slot."""
    slots = {}
    for obj in bpy.data.objects:
        if obj.get("tt_slot") and not obj.get("tt_detail") and obj.parent == arm:
            slots.setdefault(obj["tt_slot"], []).append(obj)
    return slots


def model_levels(obj):
    """Every detail level loaded for obj's sprite, high detail first; just obj for anything else."""
    if obj.get("tt_levels", 1) <= 1:
        return [obj]
    return sorted((o for o in bpy.data.objects if o.get(BROWSER_TAG) and "tt_detail" in o
                   and o.get("tt_sprite") == obj["tt_sprite"] and o.get("tt_group") == obj.get("tt_group")),
                  key=lambda o: o["tt_detail"])


def set_item_visible(obj, visible):
    """Show or hide a model; of its detail levels only the one the Detail toggle picks ever shows."""
    levels = model_levels(obj)
    shown = levels[-1] if bpy.context.window_manager.tt_detail == "LOW" else levels[0]
    for level in levels:
        level.hide_set(not (visible and level == shown))
        level.hide_render = level.hide_get()


def item_shown(obj):
    return any(not level.hide_get() for level in model_levels(obj))


def item_export(o, depsgraph):
    """The file text Publish writes for one loaded or new model, and its hash."""
    # A model saved back keeps its file's own texture attribute, which the converter reads from the registry anyway.
    texture = o["tt_file_texture"] if o.get("tt_source") and "tt_file_texture" in o else object_texture(o)
    bone = o.get("tt_bone") or (None if rest_pose_armatures([o]) else STATIC_BONE)
    text = mesh_xml_text([o], [bone], texture, False, depsgraph)
    return text, hashlib.sha1(text.encode("utf-8")).hexdigest()


def export_texts(context, arm, objs):
    """{object: (text, hash)} as Publish would write them, taken with the rig at rest."""
    previous = arm.data.pose_position if arm is not None else None
    if arm is not None:
        arm.data.pose_position = "REST"
    # At rest an Armature modifier gives the mesh back unchanged but for float noise in the last digit written.
    skinning = [m for o in objs for m in o.modifiers if m.type == "ARMATURE" and m.show_viewport]
    for modifier in skinning:
        modifier.show_viewport = False
    context.view_layer.update()
    try:
        depsgraph = context.evaluated_depsgraph_get()
        return {o: item_export(o, depsgraph) for o in objs}
    finally:
        for modifier in skinning:
            modifier.show_viewport = True
        if arm is not None:
            arm.data.pose_position = previous
        context.view_layer.update()


def write_changed(context, arm, targets):
    """Write each {object: path} whose export differs from the one it was loaded or last written as; the objects
    written, so untouched files in the repo stay as they are."""
    written = []
    editing = {level for o in bpy.data.objects if o.get("tt_skin_editing") for level in model_levels(o)}
    for o, (text, digest) in export_texts(context, arm, targets).items():
        if o.get("tt_skin") or o in editing:
            continue  # showing a skin's look or being made into one, which is not what its own file holds
        if not o.get("tt_source") or o.get("tt_export_hash") != digest:
            write_text(targets[o], text)
            o["tt_export_hash"] = digest
            written.append(o)
    return written


def file_hash(path):
    with open(path, "rb") as f:
        return hashlib.sha1(f.read()).hexdigest()


def file_clashes(targets):
    """Names of new objects whose (object, path) file is already on disk and not their own last export, or is also
    the file of another one of them."""
    seen, clashes = set(), []
    for o, path in targets:
        key = os.path.normcase(os.path.abspath(path))
        if key in seen or os.path.isfile(path) and file_hash(path) != o.get("tt_export_hash"):
            clashes.append(o.name)
        seen.add(key)
    return clashes


def load_sprite_models(context, geometry, sprite, report):
    """Every model of a registry sprite as browser objects, high detail first; the lower levels start hidden."""
    levels = []
    for model, textures in zip(sprite["models"], sprite["textures"]):
        path = os.path.join(geometry, model)
        registry_texture = ",".join(name for name, event in textures if not event)
        obj = import_mesh_file(context, path, False, True, report, registry_texture)
        if obj is None:
            if not levels:
                return []
            continue
        if levels:
            obj.name = f"{levels[0].name}_lod{len(levels)}"
        obj[BROWSER_TAG] = True
        obj["tt_group"], obj["tt_sprite"], obj["tt_source"] = sprite["group"], sprite["name"], path
        obj["tt_detail"] = len(levels)
        levels.append(obj)
    for obj in levels:
        obj["tt_levels"] = len(levels)
    set_item_visible(levels[0], True)
    return levels


def load_unit(context, group, name, report):
    """Replace the previously browsed unit with this one: mesh, skeleton, clips, and its registry attachments."""
    root = repo_root(context)
    geometry = os.path.join(root, GEOMETRY_DIR)
    registry = read_registry(root)
    entry = next((s for s in registry if s["group"] == group and s["name"] == name), None)
    if entry is None:
        report({"ERROR"}, f"{group} / {name} is no longer in geometry.xml: press Refresh")
        return None
    quiet = lambda kind, message: report(kind, message) if kind != {"INFO"} else None

    # Deleting the object a paint or edit mode is working on leaves Blender's scene in a state it can crash on.
    if context.mode != "OBJECT" and context.view_layer.objects.active is not None:
        bpy.ops.object.mode_set(mode="OBJECT")
    clear_browser_objects()
    wm = context.window_manager
    wm.tt_open_item = ""
    wm.tt_checks.clear()  # they name the meshes of the model loaded before
    wm.tt_checked = False
    for o in context.selected_objects:
        o.select_set(False)
    loaded = load_sprite_models(context, geometry, entry, quiet)
    if not loaded:
        return None
    body = loaded[0]
    for obj in loaded:
        obj["tt_category"] = sprite_category(registry, entry)
    arm = None
    rig = rig_entry(registry, entry)
    if rig["skeleton"]:
        arm = armature_from_file(context, os.path.join(geometry, rig["skeleton"]))
        arm[BROWSER_TAG] = True
        bind_meshes(arm, loaded)
        idle = None
        for clip in rig["clips"]:
            clip_name = os.path.splitext(os.path.basename(clip))[0]
            frames = read_animation(os.path.join(geometry, clip))
            action = apply_clip(context, arm, clip_name, frames)
            action["tt_clip"] = os.path.basename(clip)
            action["tt_keys"] = clip_keys(action)
            action["tt_shown_bones"] = shown_bones(frames)
            action[BROWSER_TAG] = True
            if idle is None or "idle" in clip_name:
                idle = action
        if idle is not None:
            assign_action(arm, idle)

    items = []
    for sprite in registry:
        if sprite["group"] != group or sprite["base"] != name or not sprite["slot"] or sprite["skin"]:
            continue
        levels = load_sprite_models(context, geometry, sprite, quiet)
        for obj in levels:
            obj["tt_slot"] = sprite["slot"]
            obj["tt_event"] = sprite["event"]
            bones = [g.name for g in obj.vertex_groups]
            if arm is None:
                obj.parent = body
            elif len(bones) == 1 and bones[0] in arm.data.bones:
                attach_object(arm, obj, bones[0], sprite["default"])
            else:
                bind_meshes(arm, [obj])
                set_item_visible(obj, sprite["default"])
        items += levels[:1]
        loaded += levels

    # Publish skips a model whose export still matches this, so untouched files are not rewritten.
    for obj, (_, digest) in export_texts(context, arm, loaded).items():
        obj["tt_export_hash"] = digest

    for o in context.selected_objects:
        o.select_set(False)
    active = arm if arm is not None else body
    context.view_layer.objects.active = active
    active.select_set(True)
    apply_team_preview(context)
    refresh_skins(context)
    report({"INFO"}, f"{group} / {name}: {len(rig['clips'])} clip(s), {len(items)} registry attachment(s)")
    return active


@bpy.app.handlers.persistent
def refresh_units_on_load(_file=None):
    """The list lives on the window manager, which a file load resets."""
    refresh_units(bpy.context)


def unit_index_update(self, context):
    wm = context.window_manager
    body = loaded_body()
    if 0 <= wm.tt_unit_index < len(wm.tt_units):
        item = wm.tt_units[wm.tt_unit_index]
        if body is None or (body["tt_group"], body["tt_sprite"]) != (item.group, item.sprite):
            load_unit(context, item.group, item.sprite, lambda kind, message: None)


def root_update(self, context):
    refresh_units(context)


def loaded_models():
    return [o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.type == "MESH" and o.get("tt_source")
            and "tt_export_hash" in o]


DETAIL_ITEMS = (("HIGH", "High", "The mesh the game draws close up"),
                ("LOW", "Low", "The mesh the game draws from far away"))


def has_low_detail():
    return any(o.get("tt_detail") for o in bpy.data.objects if o.get(BROWSER_TAG))


def detail_update(self, context):
    for obj in [o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.get("tt_levels", 1) > 1
                and not o.get("tt_detail")]:
        set_item_visible(obj, item_shown(obj))


REFERENCE_TAG = "tt_reference"
REFERENCE_GAP = 0.2  # of the wider model's width


def references():
    return [o for o in bpy.data.objects if o.get(REFERENCE_TAG)]


def x_extent(context, objs):
    """(left, right) world x of these meshes as drawn, posed by their armature; None for none."""
    context.view_layer.update()
    depsgraph = context.evaluated_depsgraph_get()
    xs = [(o.matrix_world @ Vector(c)).x for o in objs for c in o.evaluated_get(depsgraph).bound_box]
    return (min(xs), max(xs)) if xs else None


def add_reference(context, group, name, report):
    """The high detail mesh of a registry sprite, in its idle pose, set down right of everything on screen. It can be
    moved but is never saved."""
    root = repo_root(context)
    geometry = os.path.join(root, GEOMETRY_DIR)
    registry = read_registry(root)
    entry = next((s for s in registry if s["group"] == group and s["name"] == name), None)
    if entry is None:
        report({"ERROR"}, f"{group} / {name} is no longer in geometry.xml: press Refresh")
        return None
    shown = [o for o in bpy.data.objects if o.type == "MESH" and (o.get(BROWSER_TAG) or o.get(REFERENCE_TAG))
             and not o.hide_get()]
    before = x_extent(context, shown)
    previous = context.view_layer.objects.active
    texture = ",".join(level_textures(entry, 0))
    obj = import_mesh_file(context, os.path.join(geometry, entry["models"][0]), False, True, lambda *_: None, texture)
    if obj is None:
        report({"ERROR"}, f"{group} / {name} has no mesh to show")
        return None
    obj.name = obj.data.name = f"ref_{name}"
    obj[REFERENCE_TAG] = obj.data[REFERENCE_TAG] = f"{group} / {name}"
    moved = obj
    rig = rig_entry(registry, entry)
    if rig["skeleton"]:
        moved = build_armature(context, obj.name + "_rig", *read_skeleton(os.path.join(geometry, rig["skeleton"])))
        moved[REFERENCE_TAG] = moved.data[REFERENCE_TAG] = obj[REFERENCE_TAG]
        bind_meshes(moved, [obj])
        idle = next((c for c in rig["clips"] if "idle" in os.path.basename(c)), None)
        if idle is not None:
            apply_clip(context, moved, moved.name + "_idle", read_animation(os.path.join(geometry, idle)))[
                REFERENCE_TAG] = True
            context.scene.frame_set(1)
    extent = x_extent(context, [obj])
    if before is not None:
        gap = REFERENCE_GAP * max(before[1] - before[0], extent[1] - extent[0])
        moved.location.x += before[1] + gap - extent[0]
    for o in (obj, moved):
        o.select_set(False)
    context.view_layer.objects.active = previous
    return obj


def clear_references():
    for obj in references():
        bpy.data.objects.remove(obj, do_unlink=True)
    # Also what an object deleted by hand left behind.
    for datas in (bpy.data.meshes, bpy.data.armatures, bpy.data.actions):
        for data in [d for d in datas if d.get(REFERENCE_TAG) and d.users <= int(d.use_fake_user)]:
            datas.remove(data)


FIT_SHARE = 0.3  # an oversized item is shrunk to this share of the unit's height


def world_box(obj):
    corners = [obj.matrix_world @ Vector(c) for c in obj.bound_box]
    low = Vector(min(c[i] for c in corners) for i in range(3))
    high = Vector(max(c[i] for c in corners) for i in range(3))
    return low, high


def unit_height(arm):
    body = browsed_unit(arm) or next(iter(unit_meshes(arm)), None)
    if body is None:
        return 0.0
    low, high = world_box(body)
    return high.z - low.z


def top_of_part(context, arm, bone_name):
    """World point on top of the body where it follows this bone: the middle of those vertices, at the height
    three quarters of them sit under. Measured on all six units that is the crown of the skull, below helmet
    horns and feathers, which would otherwise lift a hat into the air."""
    body = browsed_unit(arm)
    group = body.vertex_groups.get(bone_name) if body is not None else None
    if group is None:
        return None
    evaluated = body.evaluated_get(context.evaluated_depsgraph_get())
    mesh = evaluated.to_mesh()
    try:
        points = [body.matrix_world @ mesh.vertices[v.index].co for v in body.data.vertices
                  if any(g.group == group.index and g.weight > 0.5 for g in v.groups)]
    finally:
        evaluated.to_mesh_clear()
    if not points:
        return None
    heights = sorted(point.z for point in points)
    return Vector((sum(point.x for point in points) / len(points), sum(point.y for point in points) / len(points),
                   heights[int(0.75 * (len(heights) - 1))]))


def snap_to_bone(context, arm, obj, bone, on_top, fit=True):
    """Move obj onto the bone the way Snap To Bone does; a note on how much it shrank, or ''."""
    note = ""
    height = unit_height(arm)
    low, high = world_box(obj)
    largest = max(high - low)
    if fit and height > 0.0 and largest > height:
        factor = FIT_SHARE * height / largest
        obj.scale = obj.scale * factor
        context.view_layer.update()
        note = f", shrunk to {factor:.0%} of its size because it was bigger than the unit"
    pose_bone = arm.pose.bones[bone]
    target = arm.matrix_world @ (pose_bone.tail if on_top else pose_bone.head)
    if on_top:
        target = top_of_part(context, arm, bone) or target
    low, high = world_box(obj)
    anchor = Vector(((low.x + high.x) / 2, (low.y + high.y) / 2, low.z)) if on_top \
        else obj.matrix_world.translation.copy()
    moved = obj.matrix_world.copy()
    moved.translation += target - anchor
    obj.matrix_world = moved
    context.view_layer.update()
    return note


def unit_meshes(arm):
    return [o for o in bpy.data.objects if o.type == "MESH" and o.parent == arm and o.get("tt_texture")]


def browsed_unit(arm):
    """The mesh loaded from the Models list onto this armature."""
    return next((o for o in unit_meshes(arm) if o.get("tt_group") and not o.get("tt_slot") and not o.get("tt_detail")),
                None)


def loaded_body():
    """The mesh of the model loaded from the Models list, high detail."""
    return next((o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.type == "MESH" and o.get("tt_group")
                 and o.get("tt_source") and not o.get("tt_slot") and not o.get("tt_detail")), None)


def skin_body(context):
    """The model loaded from the Models list, and its registry entry: what a skin stands in for."""
    root = repo_root(context)
    body = loaded_body()
    if body is None or not root:
        return None, None
    entry = next((s for s in read_registry(root) if s["group"] == body["tt_group"]
                  and s["name"] == body["tt_sprite"]), None)
    if entry is None or entry["skin"]:
        return None, None
    return body, entry


def item_entry(registry, obj):
    """The registry entry of a loaded item or prop a skin can stand in for, or None."""
    if obj is None or not obj.get(BROWSER_TAG) or not obj.get("tt_source") or obj.get("tt_detail") \
            or not obj.get("tt_slot"):
        return None
    entry = next((s for s in registry if s["group"] == obj.get("tt_group") and s["name"] == obj.get("tt_sprite")),
                 None)
    return entry if entry is not None and entry["slot"] and not entry["skin"] else None


def skin_item(context, name):
    root = repo_root(context)
    obj = bpy.data.objects.get(name) if name else None
    entry = item_entry(read_registry(root), obj) if root else None
    return (obj, entry) if entry is not None else (None, None)


def skin_parts(body, entry, registry):
    """(object, registry entry) for the loaded model, then each item loaded with it: what one skin name covers."""
    items = sorted((o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.get("tt_slot") and not o.get("tt_detail")),
                   key=lambda o: o["tt_sprite"])
    return [(body, entry)] + [(o, e) for o in items for e in [item_entry(registry, o)] if e is not None]


def fill_skins(skins, registry, entry, item="", parts=()):
    skins.clear()
    row = skins.add()
    row.name, row.item = "Default", item
    for sprite in sprite_skins(registry, entry):
        covered = [e["name"] for _, e in parts if any(s["skin"] == sprite["skin"] for s in sprite_skins(registry, e))]
        row = skins.add()
        row.name = sprite["skin"] + (f" (+ {', '.join(covered)})" if covered else "")
        row.skin, row.group, row.sprite, row.item = sprite["skin"], sprite["group"], sprite["name"], item
        row.tag = sprite["event"] or "Owned"


def refresh_skins(context):
    """The rows of the Skins panel and of the open item's skins, read again from the registry."""
    wm = context.window_manager
    wm.tt_skins.clear()
    wm.tt_item_skins.clear()
    body, entry = skin_body(context)
    if body is None:
        return
    registry = read_registry(repo_root(context))
    # Item skins show in their item's view in the Props panel; here only the ones the model's own skin brings.
    fill_skins(wm.tt_skins, registry, entry, parts=skin_parts(body, entry, registry)[1:])
    item, item_sprite = skin_item(context, wm.tt_open_item)
    if item is not None:
        fill_skins(wm.tt_item_skins, registry, item_sprite, item.name)

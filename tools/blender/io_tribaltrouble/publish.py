"""Writing changed models, items, clips and textures to the repo, and the checks before that."""

import os
import re
from xml.sax.saxutils import escape

import bpy

from .textures import (emission_image, ensure_emission_in_repo, ensure_texture_in_repo, image_texture_name,
                       mesh_texture_image, models_texture_path, object_texture, save_png, texture_names)
from .rig import (armature_actions, clip_copy, clip_keys, clip_prefix, clip_short_name, read_animation, shown_bones,
                  write_animation_xml)
from .registry import (append_registry_entries, find_base_sprite, GEOMETRY_DIR, item_slot, read_registry,
                       registry_entries, REGISTRY_FILE, repo_root, rig_in_repo, rig_registry, set_clip_line,
                       set_sprite_textures)
from .scene import (browsed_unit, BROWSER_TAG, file_clashes, item_shown, model_levels, set_item_visible, unit_height,
                    unit_items, unit_meshes, visible_attachments, world_box, write_changed)


def export_visible(context, arm, report):
    """Write every visible item on the unit to its file, with its texture, and every detail level of the unit and
    of those items that changed since loading. False when nothing was written."""
    unit_dir = os.path.dirname(arm["tt_skeleton"])
    # An item a skin is being made for holds the skin's edits, not its own.
    shown = [o for items in unit_items(arm).values() for o in items if item_shown(o) and not o.get("tt_skin_editing")]
    objs = visible_attachments(arm) + shown
    body = browsed_unit(arm)
    bodies = model_levels(body) if body is not None else []
    levels = [o for o in bodies + [x for o in shown for x in model_levels(o)[1:]] if o.get("tt_source")]
    if not objs and not levels:
        report({"ERROR"}, "Nothing visible to export")
        return False
    paths = {o: o.get("tt_source") or os.path.join(unit_dir, o.name + ".xml") for o in objs}
    clashes = file_clashes([(o, path) for o, path in paths.items() if not o.get("tt_source")])
    if clashes:
        report({"ERROR"}, f"Not published: {unit_dir} already has a file named after {', '.join(clashes)}: rename it")
        return False
    errors = store_findings(context, preflight(context, arm))
    if errors:
        report({"ERROR"}, f"Not published: {errors} problem(s) listed in the panel")
        return False
    written = write_changed(context, arm, {**paths, **{o: o["tt_source"] for o in levels}})
    missing = []
    # Paint on loaded items is left to publish_paint, which knows what a skin being made may not write.
    editing = skin_edit_images()
    for o in [o for o in objs if not o.get("tt_source") and mesh_texture_image(o) not in editing]:
        for name in texture_names(o):
            if not ensure_texture_in_repo(repo_root(context), o, name):
                missing.append(name)
        ensure_emission_in_repo(repo_root(context), o)
    publish_own_textures(repo_root(context), objs)
    publish_paint(repo_root(context))
    saved = [os.path.basename(o["tt_source"]) for o in written if o in bodies]
    note = f"; unit mesh: {', '.join(saved)}" if saved else ""
    if missing:
        report({"WARNING"}, f"Exported {len(written)} file(s) into {unit_dir}{note}, but no texture image for "
                            f"{', '.join(sorted(set(missing)))}: give the material an Image Texture")
    else:
        report({"INFO"}, f"Exported {len(written)} changed file(s) of {len(objs) + len(levels)} with their textures "
                         f"into {unit_dir}{note}")
    return True


def publish_items(context, arm, report):
    """Check, write every visible item into the unit's folder with its texture, and list the new ones in
    geometry.xml with the event and default their New Prop form picked. A new item then becomes one of the unit's
    buttons; its file in the repo is the real copy from here on. The new sprite names, or None when refused."""
    if not rig_in_repo(context, arm):
        report({"ERROR"}, "Load the unit from the Models list of your repo folder")
        return None
    group, base = find_base_sprite(arm["tt_skeleton"])
    if base is None:
        report({"ERROR"}, "Could not find this unit's sprite in geometry.xml")
        return None
    taken = {x["name"] for x in read_registry(repo_root(context)) if x["group"] == group}
    fresh = [x for x in arm.tt_attachments if x.obj is not None and x.visible]
    clash = [f"{base}_{x.obj.name}" for x in fresh if f"{base}_{x.obj.name}" in taken]
    if clash:
        report({"ERROR"}, f"{group} already has {', '.join(clash)}: rename your mesh")
        return None
    entries = registry_entries(context, arm, base, group)
    if not export_visible(context, arm, report):
        return None
    append_registry_entries(os.path.join(repo_root(context), REGISTRY_FILE), group, entries)
    unit_dir = os.path.dirname(arm["tt_skeleton"])
    for x in fresh:
        obj = x.obj
        obj["tt_group"] = group
        obj["tt_slot"] = item_slot(x.point, group)
        obj["tt_event"] = obj.get("tt_event", "")
        obj["tt_sprite"] = f"{base}_{obj.name}"
        obj["tt_source"] = os.path.join(unit_dir, obj.name + ".xml")
        obj["tt_texture"] = object_texture(obj)
        obj[BROWSER_TAG] = True
        x.prev_name = ""  # hand the mesh over to the unit's buttons instead of letting it go
        x.obj = None
        for other in unit_items(arm).get(obj["tt_slot"], []):
            set_item_visible(other, other == obj)
    return [name for name, _ in entries]


def check_mesh(obj, is_new, body_triangles):
    """(level, message) findings for one mesh, covering what goes wrong in game rather than in Blender."""
    found = []
    me = obj.data
    if len(me.polygons) == 0:
        return [("ERROR", "has no faces")]
    if is_new and not re.fullmatch(r"[A-Za-z0-9_]+", obj.name):
        found.append(("ERROR", "name becomes a file and sprite name: use only letters, digits and underscores"))
    uv = me.uv_layers[0] if me.uv_layers else None  # the one the file gets, whichever is active
    if uv is None:
        found.append(("ERROR", "has no UV map, so the texture cannot be placed"))
    else:
        coords = [0.0] * (2 * len(me.loops))
        uv.data.foreach_get("uv", coords)
        if max(coords) - min(coords) < 1e-6:
            found.append(("ERROR", "UV map is collapsed to one point: unwrap it"))
        elif min(coords) < -1e-4 or max(coords) > 1.0001:
            found.append(("WARNING", "UVs leave the 0 to 1 square; the game does not tile textures"))
    image = mesh_texture_image(obj)
    if image is None and not obj.get("tt_texture"):
        found.append(("ERROR", "material has no Image Texture, so there is no texture to name in game"))
    elif image is not None and image.size[0] > 0:
        w, h = image.size
        if w != h or w & (w - 1):
            found.append(("WARNING", f"texture {image.name} is {w}x{h}: use a square power of two such as 256 or 512"))
        if not re.fullmatch(r"[A-Za-z0-9_-]+", image_texture_name(image)):
            found.append(("ERROR", f"image name '{image.name}' becomes the texture name: letters, digits, underscores, "
                                   f"hyphens"))
    glow = emission_image(obj)
    if glow is not None and not re.fullmatch(r"[A-Za-z0-9_-]+", image_texture_name(glow)):
        found.append(("ERROR", f"emission image name '{glow.name}' becomes a texture name: letters, digits, "
                               f"underscores, hyphens"))
    colors = me.color_attributes.active_color if len(me.color_attributes) else None
    if colors is not None and colors.domain == "CORNER" and colors.data_type in ("FLOAT_COLOR", "BYTE_COLOR"):
        values = [0.0] * (4 * len(colors.data))
        colors.data.foreach_get("color", values)
        if min(values) < 0.999:
            found.append(("WARNING", "vertex colors are not white and will tint the texture in game"))
    if any(m.type == "ARMATURE" for m in obj.modifiers):
        loose = sum(1 for v in me.vertices if not v.groups)
        if loose:
            found.append(("WARNING", f"{loose} vertices have no bone weight and will stay at the unit's origin"))
    triangles = sum(len(p.vertices) - 2 for p in me.polygons)
    if body_triangles and is_new and triangles > body_triangles:
        found.append(("WARNING", f"{triangles} triangles, more than the unit itself ({body_triangles})"))
    return found


def texture_clashes(root, objs):
    """Errors for new meshes whose image is named like a repo texture that did not come from it: Publish would save
    over another model's texture, or take it for theirs."""
    found = []
    for o in objs:
        image = mesh_texture_image(o)
        if image is None or not root:
            continue
        texture = image_texture_name(image)
        target = os.path.normcase(os.path.abspath(models_texture_path(root, texture)))
        source = os.path.normcase(os.path.abspath(bpy.path.abspath(image.filepath))) if image.filepath else ""
        if os.path.isfile(target) and source != target and image.get("tt_repo_texture") != texture:
            found.append(("ERROR", f"{o.name}: {texture}.png in the repo is another model's texture: rename the image"))
    return found


def preflight(context, arm):
    """Findings for the unit's visible items, new and loaded, as Publish writes them."""
    body = browsed_unit(arm) or next((o for o in unit_meshes(arm) if not o.get("tt_slot")), None)
    body_triangles = sum(len(p.vertices) - 2 for p in body.data.polygons) if body is not None else 0
    new = visible_attachments(arm)
    existing = [o for items in unit_items(arm).values() for o in items if item_shown(o)]
    findings = [("ERROR", f"{name}: is on two points; one mesh can only go on one")
                for name in sorted({o.name for o in new if new.count(o) > 1})]
    for obj in new + existing:
        findings += [(level, f"{obj.name}: {text}") for level, text in check_mesh(obj, obj in new, body_triangles)]
    height = unit_height(arm)
    for obj in new:
        low, high = world_box(obj)
        if height > 0.0 and max(high - low) > height:
            findings.append(("WARNING", f"{obj.name}: is bigger than the unit itself ({max(high - low):.1f} against "
                                        f"{height:.1f} tall)"))
    root = repo_root(context)
    findings += texture_clashes(root, new)
    group, base = find_base_sprite(arm.get("tt_skeleton", ""))
    if root and base is not None:
        names = {s["name"] for s in read_registry(root) if s["group"] == group}
        for obj in new:
            if f"{base}_{obj.name}" not in names:
                findings.append(("INFO", f"{obj.name}: new, Publish adds it to the registry"))
    return findings


def store_findings(context, findings):
    checks = context.window_manager.tt_checks
    checks.clear()
    for level, text in findings:
        item = checks.add()
        item.level = level
        item.name = text
    context.window_manager.tt_checked = True
    return sum(1 for level, _ in findings if level == "ERROR")


def borrowed_rig_problem(arm, base):
    """Why the loaded unit cannot change the clips of the base rig it borrows, or None."""
    body = browsed_unit(arm)
    if body is not None and body["tt_sprite"] != base:
        return f"{body['tt_sprite']} borrows the {base} rig and its clips: load {base} to change them"
    return None


def publish_clip(context, arm, action, name, kind, wpc, report):
    """Write a clip into the unit's folder and list it in geometry.xml. An existing clip is replaced in place and the
    game plays it straight away; a brand new clip also needs code that asks for it. False when refused."""
    root = repo_root(context)
    name = name.strip().lower()
    if not re.fullmatch(r"[a-z0-9_]+", name):
        report({"ERROR"}, f"Rename the clip '{name}': use only letters, digits and underscores")
        return False
    group, base, rig = rig_registry(context, arm)
    if rig is None:
        report({"ERROR"}, "Could not find this unit's sprite in geometry.xml")
        return False
    borrowed = borrowed_rig_problem(arm, base)
    if borrowed is not None:
        report({"ERROR"}, borrowed)
        return False
    start, end = action.frame_range
    if int(round(end)) - int(round(start)) < 1:
        report({"ERROR"}, f"The clip {name} has fewer than two frames")
        return False
    geometry = os.path.join(root, GEOMETRY_DIR)
    is_new = name not in rig["clip_info"]
    if is_new:
        path = os.path.join(os.path.dirname(arm["tt_skeleton"]), clip_prefix(arm) + name + ".xml")
        listed = {os.path.normcase(os.path.join(geometry, p)) for _, _, p in rig["clip_info"].values()}
        if os.path.isfile(path) or os.path.normcase(path) in listed:
            report({"ERROR"}, f"{os.path.basename(path)} already exists: give the clip another name")
            return False
    else:
        path = os.path.join(geometry, rig["clip_info"][name][2])
    write_animation_xml(context, arm, action, path)
    relative = os.path.relpath(path, geometry).replace(os.sep, "/")
    line = f'<animation name="{name}" wpc="{wpc:g}" type="{kind}">{escape(relative)}</animation>'
    touched = set_clip_line(os.path.join(root, REGISTRY_FILE), group, rig["skeleton"].replace("\\", "/"), name, line)
    action.name = os.path.splitext(os.path.basename(path))[0]
    action["tt_clip"], action["tt_armature"], action[BROWSER_TAG] = os.path.basename(path), arm.name, True
    action["tt_shown_bones"] = shown_bones(read_animation(path))
    action["tt_keys"] = clip_keys(action)
    action.use_fake_user = True
    frames = int(round(end)) - int(round(start)) + 1
    if is_new:
        report({"WARNING"}, f"Published new clip {name} ({frames} frames) on {', '.join(touched)}. It shows in game "
                            f"only once code asks for it")
    else:
        report({"INFO"}, f"Replaced clip {name} ({frames} frames); the game plays it after the next build")
    return True


def publish_changed_clips(context, arm, report):
    """Publish every clip of the unit that is new or whose keys changed since loading; the clip files written."""
    if not rig_in_repo(context, arm):
        return []
    rig = rig_registry(context, arm)[2]
    listed = {os.path.basename(path): (name, kind, float(wpc))
              for name, (wpc, kind, path) in (rig["clip_info"].items() if rig else [])}
    written = []
    for action in armature_actions(arm):
        if action.get("tt_clip") and action.get("tt_keys") in (None, clip_keys(action)):
            continue
        if clip_copy(arm, action):
            del action["tt_clip"]  # edited since it was copied: a new clip under its own name
        name, kind, wpc = listed.get(action.get("tt_clip"), (clip_short_name(arm, action), action.get("tt_kind", "loop"),
                                                             action.get("tt_wpc", 1.0)))
        if not action.get("tt_clip") and rig and name.strip().lower() in rig["clip_info"]:
            report({"ERROR"}, f"{action.name}: the unit already has a clip called {name}; rename the action")
            continue
        if publish_clip(context, arm, action, name, kind, wpc, report):
            written.append(action["tt_clip"])
    return written


def publish_own_textures(root, objs):
    """Write the images Give It Its Own Texture made for these items, team decals too, and list them for a registry
    item in place of the unit's."""
    for o in objs:
        if not o.get("tt_own_texture"):
            continue
        for name in texture_names(o):
            ensure_texture_in_repo(root, o, name)
            if name + "_team" in bpy.data.images:
                ensure_texture_in_repo(root, o, name + "_team", "teamdecals")
        if o.get("tt_group"):
            set_sprite_textures(root, o["tt_group"], o["tt_sprite"], texture_names(o))
        del o["tt_own_texture"]


def skin_edit_images():
    """The images of the models a skin is being made for: their paint belongs to the skin."""
    return {mesh_texture_image(level) for o in bpy.data.objects if o.get("tt_skin_editing")
            for level in model_levels(o)}


def publish_paint(root):
    """Write paint on the textures of the loaded models and the skins showing. A texture a skin shares with the
    default look, and the textures of a model a skin is being made for, are never written this way. The file names
    written."""
    editing = skin_edit_images()
    written = []
    for o in bpy.data.objects:
        image = mesh_texture_image(o) if o.get(BROWSER_TAG) and o.type == "MESH" else None
        texture = image_texture_name(image) if image is not None else ""
        if image is None or not image.is_dirty or image in editing or texture not in texture_names(o) \
                or o.get("tt_skin") and texture in o.get("tt_stock_texture", "").split(","):
            continue
        save_png(image, models_texture_path(root, texture))
        written.append(texture + ".png")
    return written

"""Skins panel: previewing, making, painting and saving skins."""

import os
import re
import xml.etree.ElementTree as ET

import bpy
from bpy.props import StringProperty, BoolProperty

from .textures import (apply_team_preview, ensure_texture_in_repo, get_atlas_material, image_texture_name,
                       mesh_texture_image, models_texture_path, race_texture_name, save_png, short_labels)
from .mesh_io import body_rig, item_point, mesh_record_from_xml, POINT_LABELS, replace_mesh_data, write_text
from .registry import (append_registry_entries, GEOMETRY_DIR, level_textures, read_registry, REGISTRY_FILE, repo_root,
                       sprite_skins, sprite_text, team_attribute)
from .scene import (attach_object, attachment_obj_poll, detach_object, export_texts, loaded_body, model_levels,
                    refresh_units, set_item_visible, skin_body, skin_editing, skin_item, skin_mesh, skin_parts,
                    skins_owned, snap_to_bone)
from .publish import check_mesh, store_findings
from .forms import (chosen_event, draw_confirm, draw_event, event_property, form_title, mesh_problem, name_problem,
                    open_form, own_mesh_search)
from .models import RemoveFromRegistry


def show_skin(context, body, entry, skin):
    """Put a skin sprite's meshes and textures on every detail level of the loaded model; with no skin, put the
    stock files back and let Publish save the model again."""
    root = repo_root(context)
    shown = skin or entry
    arm = body_rig(body)
    tier = arm.get("tt_tier", 0) if arm is not None else 0
    levels = model_levels(body)
    for level, o in enumerate(levels):
        path = os.path.join(root, GEOMETRY_DIR, shown["models"][min(level, len(shown["models"]) - 1)]) if skin \
            else o["tt_source"]
        replace_mesh_data(o, mesh_record_from_xml(ET.parse(path).getroot(), False), o.data.name)
        textures = level_textures(shown, level)
        texture = textures[min(tier, len(textures) - 1)] if textures else ""
        if os.path.isfile(models_texture_path(root, texture)):
            o.data.materials.clear()
            o.data.materials.append(get_atlas_material(texture, models_texture_path(root, texture)))
        if skin is not None:
            o["tt_stock_texture"] = o.get("tt_stock_texture", o.get("tt_texture", ""))
            o["tt_texture"], o["tt_skin"], o["tt_skin_name"] = ",".join(textures), skin["name"], skin["skin"]
        elif o.get("tt_skin"):
            o["tt_texture"] = o.pop("tt_stock_texture")
            del o["tt_skin"]
            o.pop("tt_skin_name", None)
    if skin is None:
        for o, (_, digest) in export_texts(context, arm, levels).items():
            o["tt_export_hash"] = digest
    active = context.view_layer.objects.active
    if active in levels and mesh_texture_image(active) is not None:
        context.scene.tool_settings.image_paint.canvas = mesh_texture_image(active)
    apply_team_preview(context)


def leave_texture_paint(context):
    """Texture Paint draws the paint canvas instead of the material, so a swapped look only shows outside it."""
    if context.mode == "PAINT_TEXTURE":
        bpy.ops.object.mode_set(mode="OBJECT")


def end_skin_edit(context, target, entry, mesh_shown):
    """Stop making a skin: the artist's mesh comes off the unit and the model or item shows its default look."""
    del target["tt_skin_editing"]
    target.pop("tt_skin_event", None)
    mesh = skin_mesh(target)
    if mesh is not None:
        del mesh["tt_skin_mesh"]
        detach_object(mesh)
        mesh.hide_set(not mesh_shown)
        mesh.hide_render = not mesh_shown
        set_item_visible(target, True)
    show_skin(context, target, entry, None)


class ShowSkin(bpy.types.Operator):
    """Show the loaded model and its items in this skin, every detail level. Default shows their own files again
    and drops edits"""
    bl_idname = "object.tt_show_skin"
    bl_label = "Preview Skin"
    bl_options = {"REGISTER", "UNDO"}
    skin: StringProperty(options={"SKIP_SAVE"})
    item: StringProperty(options={"SKIP_SAVE"}, description="Only this item; blank for the model and its items")

    def execute(self, context):
        body, entry = skin_body(context)
        if body is None:
            return {"CANCELLED"}
        leave_texture_paint(context)
        registry = read_registry(repo_root(context))
        parts = [p for p in skin_parts(body, entry, registry) if not self.item or p[0].name == self.item]
        if not parts:
            self.report({"ERROR"}, f"{self.item} is not an item of {entry['name']} a skin can cover")
            return {"CANCELLED"}
        covered = {o: next((s for s in sprite_skins(registry, e) if s["skin"] == self.skin), None) for o, e in parts}
        if self.skin and not any(covered.values()):
            self.report({"ERROR"}, f"No {self.skin} skin covers {self.item or entry['name']}")
            return {"CANCELLED"}
        target, target_entry = skin_editing(context)
        if target is not None:
            end_skin_edit(context, target, target_entry, True)
        for o, e in parts:
            if covered[o] is not None or o.get("tt_skin") or o == body:
                show_skin(context, o, e, covered[o])
        return {"FINISHED"}


def skin_name_search(self, context, edit_text):
    """Skin names geometry.xml already uses anywhere, so one name can cover many models."""
    root = repo_root(context)
    names = {s["skin"] for s in read_registry(root)} if root else set()
    return sorted(n for n in names if n and edit_text.strip().lower() in n.lower())


def skin_name_problem(registry, entry, skin):
    if not re.fullmatch(r"[A-Za-z0-9_]+", skin):
        return "Name the skin with letters, digits and underscores"
    # One skin name covers many models, each with its own entry; only this model's entry must be new.
    if any(s["skin"] == skin for s in sprite_skins(registry, entry)):
        return f"{entry['name']} already has a {skin} skin"
    if any(s["group"] == entry["group"] and s["name"] == f"{entry['name']}_{skin}" for s in registry):
        return f"{entry['group']} already has a sprite named {entry['name']}_{skin}"
    return None


class NewSkin(bpy.types.Operator):
    """Start a new skin for the loaded model or one of its items: change its mesh or texture, then Save Skin. Without
    an event, players who own it see it; with one, everyone sees it during that event"""
    bl_idname = "object.tt_new_skin"
    bl_label = "New Skin..."
    skin_name: StringProperty(name="Skin name", search=skin_name_search, options={"SKIP_SAVE"},
                              description="Letters, digits and underscores. Pick a name other models already use "
                                          "to add this model to that skin")
    item: StringProperty(options={"SKIP_SAVE"}, description="The item to make the skin for; blank for the model")
    mesh: StringProperty(name="Mesh", search=own_mesh_search, options={"SKIP_SAVE"},
                         description="Blank to reshape or repaint the model itself. One of your own meshes to use "
                                     "as the skin's shape instead")
    snap: BoolProperty(name="Snap", default=True, options={"SKIP_SAVE"},
                       description="Move the mesh onto the part of the unit that holds the item")
    make_texture: BoolProperty(name="Make a texture for it", default=True, options={"SKIP_SAVE"},
                               description="Give the mesh a UV map and an image named after it to paint")
    event: event_property(skins_owned)

    @classmethod
    def poll(cls, context):
        return skin_body(context)[0] is not None

    def invoke(self, context, event):
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "skin_name")
        problem = name_problem(self.skin_name)
        layout.prop(self, "mesh", icon="MESH_DATA")
        if self.mesh.strip() and problem is None:
            problem = mesh_problem(self.mesh)
        mesh = bpy.data.objects.get(self.mesh)
        target = bpy.data.objects.get(self.item) if self.item else loaded_body()
        if mesh is not None and target is not None and body_rig(target) is not None and target.get("tt_bone"):
            point = item_point(body_rig(target), target.get("tt_bone"))
            layout.prop(self, "snap", text=f"Snap to {POINT_LABELS.get(point, target.get('tt_bone', '')).lower()}")
        if mesh is not None and mesh.type == "MESH" and mesh_texture_image(mesh) is None:
            layout.prop(self, "make_texture")
        draw_event(layout, self)
        if problem is None and not chosen_event(self) and not skins_owned(context):
            problem = ""
        draw_confirm(layout, self, problem)

    def execute(self, context):
        target, entry = skin_item(context, self.item) if self.item else skin_body(context)
        if target is None:
            self.report({"ERROR"}, f"{self.item} is not an item of the loaded model a skin can cover")
            return {"CANCELLED"}
        editing = skin_editing(context)[0]
        if editing is not None and (editing != target or skin_mesh(target) is not None):
            self.report({"ERROR"}, f"Save or cancel skin '{editing['tt_skin_editing']}' for {editing['tt_sprite']} "
                                   f"first")
            return {"CANCELLED"}
        skin = self.skin_name.strip()
        registry = read_registry(repo_root(context))
        problem = skin_name_problem(registry, entry, skin)
        if problem:
            self.report({"ERROR"}, problem)
            return {"CANCELLED"}
        event = chosen_event(self)
        if not event and not skins_owned(context):
            self.report({"ERROR"}, "Nobody owns this model: pick the event its skin is for")
            return {"CANCELLED"}
        arm, mesh = body_rig(target), None
        if self.mesh.strip():
            mesh = bpy.data.objects.get(self.mesh.strip())
            if mesh is None or not attachment_obj_poll(self, mesh):
                self.report({"ERROR"}, "Pick one of your own meshes, or leave Mesh blank")
                return {"CANCELLED"}
            if arm is not None and target.get("tt_bone") not in arm.data.bones:
                self.report({"ERROR"}, f"{entry['name']} bends with the unit, so its skin can only reshape it")
                return {"CANCELLED"}
        body, body_entry = skin_body(context)
        for o, e in skin_parts(body, body_entry, registry):
            if o.get("tt_skin"):
                show_skin(context, o, e, None)
        if mesh is not None:
            if arm is not None:
                bone = target["tt_bone"]
                attach_object(arm, mesh, bone, True)
                if self.snap:
                    snap_to_bone(context, arm, mesh, bone, item_point(arm, bone) == "HEAD")
            mesh["tt_skin_mesh"] = target.name
            if self.make_texture and mesh_texture_image(mesh) is None:
                bpy.ops.object.tt_make_texture(target=mesh.name)
            set_item_visible(target, False)
        target["tt_skin_editing"] = skin
        target["tt_skin_event"] = event
        return {"FINISHED"}


class CancelSkin(bpy.types.Operator):
    """Stop making this skin: the model or item shows its default look again and nothing is written"""
    bl_idname = "object.tt_cancel_skin"
    bl_label = "Cancel"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return skin_editing(context)[0] is not None

    def execute(self, context):
        leave_texture_paint(context)
        target, entry = skin_editing(context)
        end_skin_edit(context, target, entry, True)
        return {"FINISHED"}


def skin_level_suffix(level):
    return "" if level == 0 else "_lo" if level == 1 else f"_lo{level}"


def item_skin_texture(entry, textures, at, skin):
    """A new texture for an item's skin, named with the race like its own textures: viking_peon_hammer_gold."""
    name = race_texture_name(entry["group"], entry["name"])
    if len(textures) > 1:
        name = f"{name}_{short_labels(textures)[at]}"
    return f"{name}_{skin}"


class SaveSkin(bpy.types.Operator):
    """Save the model's or item's look as a skin: a player who has it, or everyone during its event, sees it on all
    of these models. A detail level whose mesh you did not change keeps using the default file. The default files are never written,
    and the model shows its default look again afterwards"""
    bl_idname = "object.tt_save_skin"
    bl_label = "Save Skin"

    @classmethod
    def poll(cls, context):
        return skin_editing(context)[0] is not None

    def execute(self, context):
        leave_texture_paint(context)
        root = repo_root(context)
        body, entry = skin_editing(context)
        skin = body["tt_skin_editing"]
        group, sprite = entry["group"], entry["name"]
        name = f"{sprite}_{skin}"
        if body.get("tt_skin"):
            self.report({"ERROR"}, f"Showing {body['tt_skin']}: press Default, then make your changes")
            return {"CANCELLED"}
        registry = read_registry(root)
        problem = skin_name_problem(registry, entry, skin)
        if problem:
            self.report({"ERROR"}, problem)
            return {"CANCELLED"}
        arm = body_rig(body)
        tier = arm.get("tt_tier", 0) if arm is not None else 0
        mesh = skin_mesh(body)
        levels = [mesh] if mesh is not None else model_levels(body)
        exports = export_texts(context, arm, levels)
        findings = [(level, f"{o.name}: {text}") for o in levels for level, text in check_mesh(o, False, 0)]
        paths, textures, images = {}, {}, {}
        for level, o in enumerate(levels):
            paths[o] = o.get("tt_source", "")
            if o == mesh or exports[o][1] != o.get("tt_export_hash"):
                paths[o] = os.path.join(os.path.dirname(body["tt_source"]), name + skin_level_suffix(level) + ".xml")
                if os.path.exists(paths[o]):
                    findings.append(("ERROR", f"{o.name}: {os.path.basename(paths[o])} is already on disk"))
            textures[o] = level_textures(entry, level)
            image = mesh_texture_image(o)
            if o == mesh:
                textures[o] = [item_skin_texture(entry, [], 0, skin)]
            if image is None or not textures[o]:
                continue
            at = min(tier, len(textures[o]) - 1)
            texture = image_texture_name(image)
            if o == mesh:
                texture = textures[o][at]
            elif texture == textures[o][at] and image.is_dirty:
                # Paint on the stock texture goes to a copy.
                texture = item_skin_texture(entry, textures[o], at, skin) if entry["slot"] else f"{texture}_{skin}"
            if texture == textures[o][at] and o != mesh:
                continue
            target = models_texture_path(root, texture)
            source = os.path.normcase(os.path.abspath(bpy.path.abspath(image.filepath))) if image.filepath else ""
            if texture not in images and os.path.isfile(target) and \
                    (image.is_dirty or source != os.path.normcase(os.path.abspath(target))):
                findings.append(("ERROR", f"{o.name}: {texture}.png is already in assets/textures/models"))
            images[texture] = (o, image, None if o == mesh else textures[o][at])
            textures[o][at] = texture
        if not findings and all(paths[o] == o.get("tt_source") and textures[o] == level_textures(entry, level)
                                for level, o in enumerate(levels)):
            findings.append(("ERROR", "nothing differs from the default model: change the mesh or its texture first"))
        errors = store_findings(context, findings)
        if errors:
            self.report({"ERROR"}, f"Not saved: {errors} problem(s): " +
                        "; ".join(text for level, text in findings if level == "ERROR"))
            return {"CANCELLED"}

        changed = [o for o in levels if paths[o] != o.get("tt_source")]
        stock_attributes = {o: o["tt_file_texture"] for o in changed if o.get("tt_file_texture")}
        for o in stock_attributes:
            o["tt_file_texture"] = ",".join(textures[o])
        if mesh is not None:
            mesh["tt_texture"] = ",".join(textures[mesh])
        try:
            for o, (text, _) in export_texts(context, arm, changed).items():
                write_text(paths[o], text)
        finally:
            for o, attribute in stock_attributes.items():
                o["tt_file_texture"] = attribute
            if mesh is not None:
                del mesh["tt_texture"]
        for texture, (o, image, replaced) in images.items():
            if image_texture_name(image) == texture:
                ensure_texture_in_repo(root, o, texture)
            else:
                save_png(image, models_texture_path(root, texture))
                if replaced is not None:
                    image.reload()
        # A new texture on a stock tier keeps that tier's team decal unless it has its own.
        stock = {texture: replaced for texture, (_, _, replaced) in images.items() if replaced is not None}
        geometry = os.path.join(root, GEOMETRY_DIR)
        models = [(os.path.relpath(paths[o], geometry).replace(os.sep, "/"),
                   [(t, team_attribute(root, t, False) or team_attribute(root, stock.get(t, t), False))
                    for t in textures[o]]) for o in levels]
        if entry["slot"]:
            attrs = [("name", name), ("base", entry["base"]), ("slot", entry["slot"]), ("skin", skin),
                     ("replaces", sprite)]
        else:
            attrs = [("name", name), ("skin", skin), ("replaces", sprite)] + \
                    ([("base", sprite)] if arm is not None else [])
        if body.get("tt_skin_event"):
            attrs.append(("event", body["tt_skin_event"]))
        append_registry_entries(os.path.join(root, REGISTRY_FILE), group, [(name, sprite_text(attrs, models))])
        end_skin_edit(context, body, entry, False)
        refresh_units(context)
        self.report({"INFO"}, f"Saved skin {skin} of {group} / {sprite}: {len(changed)} new mesh(es), "
                              f"{len(images)} new texture(s)")
        return {"FINISHED"}


def draw_skin_banner(layout, target, entry):
    banner = layout.box()
    banner.alert = True
    banner.label(text=f"Editing skin '{target['tt_skin_editing']}' for {entry['name']}", icon="BRUSH_DATA")
    row = banner.row(align=True)
    row.operator(SaveSkin.bl_idname, icon="EXPORT")
    row.operator(CancelSkin.bl_idname, icon="X")


class PaintSkin(bpy.types.Operator):
    """Show this skin and start painting its texture; Publish writes the paint. A texture the skin shares with the
    default look cannot be painted here: make a New Skin with its own"""
    bl_idname = "object.tt_paint_skin"
    bl_label = "Paint Skin"
    skin: StringProperty(options={"SKIP_SAVE"})
    item: StringProperty(options={"SKIP_SAVE"}, description="Only this item; blank for the model and its items")

    def execute(self, context):
        if bpy.ops.object.tt_show_skin(skin=self.skin, item=self.item) != {"FINISHED"}:
            return {"CANCELLED"}
        target, entry = skin_item(context, self.item) if self.item else skin_body(context)
        if not self.skin:
            return bpy.ops.object.tt_paint_item(target=target.name)
        image = mesh_texture_image(target)
        stock = {t for level in entry["textures"] for t, _ in level}
        if image is not None and image_texture_name(image) in stock:
            self.report({"ERROR"}, f"The {self.skin} skin uses the default texture {image_texture_name(image)} here: "
                                   f"make a New Skin with its own texture")
            return {"CANCELLED"}
        return bpy.ops.object.tt_paint_item(target=target.name)


class TTSkinEntry(bpy.types.PropertyGroup):
    skin: StringProperty()
    group: StringProperty()
    sprite: StringProperty()
    tag: StringProperty()
    item: StringProperty()


def shown_skin_index(skins, target):
    """The row of the skin target shows: the list's selection always follows what is shown."""
    shown = target.get("tt_skin_name", "") if target is not None else ""
    return next((i for i, row in enumerate(skins) if row.skin == shown), -1)


def pick_skin_row(skins, index):
    if 0 <= index < len(skins):
        bpy.ops.object.tt_pick_skin(skin=skins[index].skin, item=skins[index].item)


class TT_UL_skins(bpy.types.UIList):
    def draw_item(self, context, layout, data, item, icon, active_data, active_property, index):
        row = layout.row(align=True)
        on = index == getattr(data, active_property)
        eye = row.operator(ShowSkin.bl_idname, text="", icon="HIDE_OFF" if on else "HIDE_ON", emboss=False)
        eye.skin, eye.item = "" if on else item.skin, item.item
        row.label(text=item.name)
        tag = row.row()
        tag.alignment = "RIGHT"
        tag.enabled = False
        tag.label(text=item.tag)
        paint = row.operator(PaintSkin.bl_idname, text="", icon="BRUSH_DATA", emboss=False)
        paint.skin, paint.item = item.skin, item.item
        if item.skin:
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH", emboss=False)
            remove.group, remove.sprite = item.group, item.sprite
        else:
            row.label(text="", icon="BLANK1")


class PickSkin(bpy.types.Operator):
    """Show this skin on the loaded model and the items it covers, or on this item only"""
    bl_idname = "object.tt_pick_skin"
    bl_label = "Pick Skin"
    bl_options = {"REGISTER", "UNDO"}
    skin: StringProperty(options={"SKIP_SAVE"})
    item: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        target = skin_item(context, self.item)[0] if self.item else skin_body(context)[0]
        if target is None:
            return {"CANCELLED"}
        if target.get("tt_skin_name", "") == self.skin:
            return {"FINISHED"}
        return bpy.ops.object.tt_show_skin(skin=self.skin, item=self.item)


def draw_skin_list(layout, wm, item=""):
    skins, index = ("tt_item_skins", "tt_item_skin_index") if item else ("tt_skins", "tt_skin_index")
    layout.template_list("TT_UL_skins", skins, wm, skins, wm, index, rows=2)


class VIEW3D_PT_tt_skins(bpy.types.Panel):
    bl_label = "Skins"
    bl_order = 1
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return skin_body(context)[0] is not None

    def draw(self, context):
        layout = self.layout
        draw_skin_list(layout, context.window_manager)
        target, target_entry = skin_editing(context)
        if target is not None:
            draw_skin_banner(layout, target, target_entry)
        else:
            layout.operator(NewSkin.bl_idname, icon="ADD")

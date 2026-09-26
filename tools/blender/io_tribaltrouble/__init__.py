"""Blender import/export addon for Tribal Trouble mesh XML files.

Install: Edit > Preferences > Add-ons > Install... > pick this file, enable it.
Import: File > Import > Tribal Trouble Mesh (.xml), or Skeleton / Animation (.xml)
Export: File > Export > Tribal Trouble Mesh (.xml), or Skeleton / Animation (.xml)
Attachments: 3D view sidebar, "Tribal Trouble" tab, with an armature active.

Skeleton and clip files hold absolute model-space 4x4 matrices per bone (m<column><row>,
translation in m30..m32). Import builds an armature whose rest pose equals the file's and
solves each pose bone's basis from the file matrices; export samples pose_bone.matrix per
frame. tools/blender/validate_roundtrip.py is the numeric gate for that path.

Static props (plants, rocks, a torch) have no skeleton: every vertex is skinned
to "dummy_bone" with weight 1, which is what the exporter writes when an object
has no bone-named vertex groups. Attachments (hats, held items) are the same
thing skinned to one unit bone instead: pick an attachment point in the export
dialog, or set a tt_bone property on the object. Model attachments in the unit's
bind pose. Skinned units keep their weights as vertex groups and export from
them, always in the rest pose; Split Mesh by Bone separates a baked-in held
item into its own object.

The game is Z-up like Blender, so no axis conversion is needed. If the texture
looks vertically flipped on an imported model, re-import with "Flip V" checked
and also check it on export.
"""

import os
import re
import shutil
import xml.etree.ElementTree as ET

import bpy
import numpy as np
from bpy.props import (StringProperty, BoolProperty, CollectionProperty, EnumProperty, FloatProperty, IntProperty,
                       FloatVectorProperty)

from .textures import (apply_team_preview, crop_pixels, ensure_texture_in_repo, get_atlas_material, image_texture_name,
                       material_image_name, mesh_texture_image, MIP_PAD, models_texture_path, race_texture_name,
                       save_png, short_labels, team_preview_update, texture_names)
from .mesh_io import (active_armature, armature_actions, assign_action, body_rig, clip_short_name, item_hidden_here,
                      item_point, mesh_record_from_xml, POINT_LABELS, replace_mesh_data, setup_attachment_slots,
                      show_clip, shown_bones, write_animation_xml, write_mesh_xml, write_skeleton_xml, write_text)
from .registry import (append_registry_entries, CARRY_SLOT, CATEGORY_ICONS, CATEGORY_ITEMS, find_base_sprite,
                       GEOMETRY_DIR, level_textures, PROP_SLOT, read_registry, registry_entries, REGISTRY_FILE,
                       remove_registry_entry, repo_root, rig_in_repo, rig_registry, root_holder, SCENERY_GROUP,
                       set_clip_line, sprite_skins, sprite_text, team_attribute)
from .scene import (add_reference, attach_object, attachment_obj_poll, browsed_unit, BROWSER_TAG, building_props,
                    building_stage, clear_references, detach_object, DETAIL_ITEMS, detail_update, export_texts,
                    file_clashes, has_low_detail, item_shown, load_unit, loaded_body, loaded_models, model_levels,
                    prop_body, prop_tag, REFERENCE_TAG, references, refresh_skins, refresh_units, refresh_units_on_load,
                    root_update, set_item_visible, skin_body, skin_editing, skin_item, skin_mesh, skin_parts,
                    skins_owned, snap_to_bone, TTAttachmentSlot, unit_index_update, unit_items, unit_meshes, unit_tiers,
                    visible_attachments, write_changed)
from .publish import (check_mesh, export_visible, preflight, publish_changed_clips, publish_clip, publish_items,
                      publish_own_textures, publish_skin_paint, store_findings)
from .forms import (chosen_event, draw_confirm, draw_event, event_property, form_title, mesh_problem, name_problem,
                    NewEvent, open_form, own_mesh_search)
from .import_export import (ExportTTMesh, ExportTTSkeleton, ImportTTMesh, ImportTTSkeleton, menu_export, menu_import,
                            menu_object, SplitByBone)


bl_info = {
    "name": "Tribal Trouble Mesh (.xml)",
    "author": "Tribal Trouble tooling",
    "version": (1, 42, 0),
    "blender": (4, 1, 0),
    "location": "File > Import-Export",
    "description": "Import/export Tribal Trouble geometry XML meshes",
    "category": "Import-Export",
}


class SetupAttachments(bpy.types.Operator):
    """Create a slot for every attachment point this armature has bones for"""
    bl_idname = "object.tt_setup_attachments"
    bl_label = "Set Up Attachment Points"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def execute(self, context):
        arm = active_armature(context)
        added = setup_attachment_slots(arm)
        self.report({"INFO"}, f"{arm.name}: {len(arm.tt_attachments)} attachment points ({added} new)")
        return {"FINISHED"}


class ExportAttachments(bpy.types.Operator):
    """Export every visible attachment as its own mesh file skinned to its bone"""
    bl_idname = "export_mesh.tt_attachments"
    bl_label = "Export Visible Attachments"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(visible_attachments(arm))

    def execute(self, context):
        objs = visible_attachments(active_armature(context))
        for o in context.selected_objects:
            o.select_set(False)
        for o in objs:
            o.select_set(True)
        context.view_layer.objects.active = objs[0]
        return bpy.ops.export_mesh.tt_xml("INVOKE_DEFAULT", batch_per_object=True)


class CopyRegistrySnippet(bpy.types.Operator):
    """Copy geometry.xml sprite entries for the visible attachments to the clipboard"""
    bl_idname = "object.tt_registry_snippet"
    bl_label = "Copy Registry Snippet"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(visible_attachments(arm))

    def execute(self, context):
        arm = active_armature(context)
        group, base = find_base_sprite(arm.get("tt_skeleton", ""))
        if base is None:
            base = "BASE_SPRITE"
            self.report({"WARNING"}, "Could not find the unit's sprite in geometry.xml; fill in base by hand")
        entries = [text for _, text in registry_entries(context, arm, base, group=group)]
        context.window_manager.clipboard = "\n".join(entries) + "\n"
        where = f"group {group}" if group else "the unit's group"
        self.report({"INFO"}, f"Copied {len(entries)} sprite entr{'y' if len(entries) == 1 else 'ies'} for {where}")
        return {"FINISHED"}


SLOT_LABELS = {"hat": "Hats", "weapon": "Weapons", "carried": "Carried"}


def slot_label(game_slot):
    return SLOT_LABELS.get(game_slot, game_slot.replace("_", " ").title())


def item_rows(objects, arm, name_filter):
    """(visible, order) per object for the items list: this unit's registry items whose name or slot holds the typed
    text, sorted by slot then name."""
    wanted = name_filter.strip().lower()
    shown = [arm is not None and o.get("tt_slot") and not o.get("tt_detail") and o.parent == arm
             and (wanted in o.get("tt_sprite", "").lower() or wanted in slot_label(o["tt_slot"]).lower())
             for o in objects]
    ranked = sorted(range(len(objects)), key=lambda i: (not shown[i], objects[i].get("tt_slot", ""),
                                                        objects[i].get("tt_sprite", objects[i].name)))
    order = [0] * len(objects)
    for position, index in enumerate(ranked):
        order[index] = position
    return [bool(x) for x in shown], order


def item_tag(item):
    return prop_tag(item) if item["tt_slot"] == PROP_SLOT else slot_label(item["tt_slot"])


class TT_UL_items(bpy.types.UIList):
    """The loaded model's items or props. The list keeps its height and scrolls, however many there are"""

    def draw_item(self, context, layout, data, item, icon, active_data, active_property, index):
        row = layout.row(align=True)
        hidden = not item_shown(item)
        row.operator(ShowItem.bl_idname, text="", icon="HIDE_ON" if hidden else "HIDE_OFF", emboss=False).item = item.name
        row.label(text=item["tt_sprite"])
        if item.get("tt_skin"):
            row.label(text=item.get("tt_skin_name", item["tt_skin"]), icon="BRUSH_DATA")
        tag = row.row()
        tag.alignment = "RIGHT"
        tag.enabled = False
        tag.label(text=item_tag(item))
        if item_hidden_here(active_armature(context), item):
            row.operator(ShowItemClip.bl_idname, text="", icon="TIME", emboss=False).item = item.name
        row.operator(PaintItem.bl_idname, text="", icon="BRUSH_DATA", emboss=False).target = item.name
        if item["tt_slot"] != CARRY_SLOT:
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH", emboss=False)
            remove.group, remove.sprite = item.get("tt_group", ""), item["tt_sprite"]

    def draw_filter(self, context, layout):
        pass  # the search field sits above the list instead

    def filter_items(self, context, data, propname):
        objects = list(getattr(data, propname))
        wm = context.window_manager
        shown, order = item_rows(objects, props_holder(context), wm.tt_item_search)
        return [self.bitflag_filter_item if x else 0 for x in shown], order


def props_holder(context):
    """What the Props panel lists items of: the unit's armature, or the loaded model props hang on."""
    return active_armature(context) or prop_body(context)


def open_item(context):
    """The item the Props panel shows on its own, or None while it shows the list."""
    holder = props_holder(context)
    obj = bpy.data.objects.get(context.window_manager.tt_open_item)
    if holder is None or obj is None or obj not in unit_items(holder).get(obj.get("tt_slot", ""), []):
        return None
    return obj


def item_index_update(self, context):
    """Clicking a row opens that item and makes it the active object; nothing is loaded or changed."""
    wm = context.window_manager
    obj = bpy.data.objects[wm.tt_item_index] if 0 <= wm.tt_item_index < len(bpy.data.objects) else None
    holder = props_holder(context)
    if obj is None or holder is None or obj not in unit_items(holder).get(obj.get("tt_slot", ""), []):
        return
    wm.tt_open_item = obj.name
    refresh_skins(context)
    if item_shown(obj) and context.mode == "OBJECT":
        for o in context.selected_objects:
            o.select_set(False)
        obj.select_set(True)
        context.view_layer.objects.active = obj


class CloseItem(bpy.types.Operator):
    """Back to the list of this unit's items"""
    bl_idname = "object.tt_close_item"
    bl_label = "Back to items"

    def execute(self, context):
        wm = context.window_manager
        wm.tt_open_item = ""
        wm.tt_item_index = -1
        refresh_skins(context)
        return {"FINISHED"}


def draw_item_detail(context, layout, arm, item):
    layout.operator(CloseItem.bl_idname, icon="BACK")
    box = layout.box()
    row = box.row(align=True)
    row.operator(ShowItem.bl_idname, text="", icon="HIDE_OFF" if item_shown(item) else "HIDE_ON",
                 emboss=False).item = item.name
    row.label(text=item["tt_sprite"])
    tag = row.row()
    tag.alignment = "RIGHT"
    tag.enabled = False
    tag.label(text=item_tag(item))
    row.operator(PaintItem.bl_idname, text="", icon="BRUSH_DATA", emboss=False).target = item.name
    if item["tt_slot"] != CARRY_SLOT:
        remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH", emboss=False)
        remove.group, remove.sprite = item.get("tt_group", ""), item["tt_sprite"]
    if item_hidden_here(arm, item):
        clip = box.operator(ShowItemClip.bl_idname, text="Hidden in this clip: show one it is in", icon="TIME")
        clip.item = item.name
    if shares_unit_texture(arm, item):
        box.operator(OwnTexture.bl_idname, icon="IMAGE_DATA").target = item.name
    target, entry = skin_item(context, item.name)
    if target is None:
        return
    box = layout.box()
    box.label(text=f"Skins of {entry['name']}")
    draw_skin_list(box, context.window_manager, item.name)
    if item.get("tt_skin_editing"):
        draw_skin_banner(box, item, entry)
    else:
        box.operator(NewSkin.bl_idname, icon="ADD").item = item.name


_point_items = []


def point_items(self, context):
    arm = active_armature(context)
    slots = arm.tt_attachments if arm is not None else []
    _point_items[:] = [(slot.point, POINT_LABELS.get(slot.point, slot.point), "") for slot in slots] or [("HEAD", "Head", "")]
    return _point_items


class VIEW3D_PT_tt_attachments(bpy.types.Panel):
    bl_label = "Props"
    bl_order = 4
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return props_holder(context) is not None

    def draw(self, context):
        arm = active_armature(context)
        wm = context.window_manager
        layout = self.layout
        if arm is not None and not arm.tt_attachments:
            layout.operator(SetupAttachments.bl_idname)
            return
        item = open_item(context)
        if item is not None:
            draw_item_detail(context, layout, arm, item)
            if context.mode == "PAINT_TEXTURE":
                layout.operator(DonePainting.bl_idname, icon="CHECKMARK", depress=True)
            return
        layout.prop(wm, "tt_item_search", text="", icon="VIEWZOOM")
        layout.template_list("TT_UL_items", "", bpy.data, "objects", wm, "tt_item_index", rows=2, maxrows=12)
        layout.operator(NewItem.bl_idname if arm is not None else NewProp.bl_idname, icon="ADD")
        if context.mode == "PAINT_TEXTURE":
            layout.operator(DonePainting.bl_idname, icon="CHECKMARK", depress=True)
        for slot in (x for x in (arm.tt_attachments if arm is not None else []) if x.obj is not None):
            box = layout.box()
            row = box.row(align=True)
            row.label(text=f"{POINT_LABELS.get(slot.point, slot.point)}: {slot.obj.name}", icon="ADD")
            row.prop(slot, "obj", text="")
            row = box.row(align=True)
            row.operator(PutOnBone.bl_idname, icon="SNAP_ON", text="Snap again").point = slot.point
            if mesh_texture_image(slot.obj) is None:
                row.alert = True
                row.operator(MakeTexture.bl_idname, icon="TEXTURE").target = slot.obj.name
            else:
                row.operator(PaintItem.bl_idname, icon="BRUSH_DATA").target = slot.obj.name
            if shares_unit_texture(arm, slot.obj):
                box.operator(OwnTexture.bl_idname, icon="IMAGE_DATA").target = slot.obj.name


def draw_checks(layout, wm):
    if wm.tt_checked:
        box = layout.box()
        if not wm.tt_checks:
            box.label(text="Nothing to fix", icon="CHECKMARK")
        for check in wm.tt_checks:
            box.label(text=check.name, icon=CHECK_ICONS.get(check.level, "INFO"))


class VIEW3D_PT_tt_attachments_more(bpy.types.Panel):
    bl_label = "By hand"
    bl_order = 5
    bl_parent_id = "VIEW3D_PT_tt_attachments"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"
    bl_options = {"DEFAULT_CLOSED"}

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def draw(self, context):
        col = self.layout.column(align=True)
        col.operator(SetupAttachments.bl_idname, text="Refresh Points")
        col.operator(ExportToRepo.bl_idname)
        col.operator(AddToRegistry.bl_idname)
        col.operator(ExportAttachments.bl_idname)
        col.operator(CopyRegistrySnippet.bl_idname)


class TTPreferences(bpy.types.AddonPreferences):
    bl_idname = __package__
    repo_root: StringProperty(name="Repo Folder", subtype="DIR_PATH", update=root_update,
                              description="Your tribaltrouble checkout (the folder that holds assets)")

    def draw(self, context):
        draw_update_button(self.layout, context)
        self.layout.prop(self, "repo_root")


class TTUnitEntry(bpy.types.PropertyGroup):
    group: StringProperty()
    sprite: StringProperty()
    category: StringProperty()


class TT_UL_units(bpy.types.UIList):
    def draw_item(self, context, layout, data, item, icon, active_data, active_property, index):
        # A button, not a label, so a right click knows which row it is on.
        row = layout.row()
        name = row.row()
        name.alignment = "LEFT"
        pick = name.operator(PickUnit.bl_idname, text=item.name, icon=CATEGORY_ICONS.get(item.category, "MESH_CUBE"),
                             emboss=False)
        pick.group, pick.sprite = item.group, item.sprite


class PickUnit(bpy.types.Operator):
    """Load this model. Right click for Add To Scene"""
    bl_idname = "wm.tt_pick_unit"
    bl_label = "Pick Model"
    bl_options = {"REGISTER", "UNDO"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        wm = context.window_manager
        index = next((i for i, u in enumerate(wm.tt_units) if (u.group, u.sprite) == (self.group, self.sprite)), -1)
        if index < 0:
            return {"CANCELLED"}
        if index != wm.tt_unit_index:
            wm.tt_unit_index = index
        return {"FINISHED"}


class RefreshUnits(bpy.types.Operator):
    """Read geometry.xml again"""
    bl_idname = "wm.tt_refresh_units"
    bl_label = "Refresh"

    def execute(self, context):
        if not repo_root(context):
            self.report({"ERROR"}, "Set the repo folder first: it must contain assets/geometry/geometry.xml")
            return {"CANCELLED"}
        self.report({"INFO"}, f"{refresh_units(context)} models listed")
        return {"FINISHED"}


class LoadUnit(bpy.types.Operator):
    """Load the model picked in the list again from its files, dropping unsaved edits"""
    bl_idname = "wm.tt_load_unit"
    bl_label = "Reload"
    bl_options = {"REGISTER", "UNDO"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        wm = context.window_manager
        group, sprite = self.group, self.sprite
        if not sprite:
            if not 0 <= wm.tt_unit_index < len(wm.tt_units):
                return {"CANCELLED"}
            group, sprite = wm.tt_units[wm.tt_unit_index].group, wm.tt_units[wm.tt_unit_index].sprite
        if not repo_root(context):
            self.report({"ERROR"}, "Set the repo folder first")
            return {"CANCELLED"}
        return {"FINISHED"} if load_unit(context, group, sprite, self.report) is not None else {"CANCELLED"}


class PublishModel(bpy.types.Operator):
    """Save everything you changed on the model loaded from the list to the repo: every detail level, its items and
    props (new ones are listed in geometry.xml), its clips and painted textures. Files you did not touch are left
    alone"""
    bl_idname = "wm.tt_publish_model"
    bl_label = "Publish"

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context)) and bool(loaded_models())

    def execute(self, context):
        arm = next((o for o in bpy.data.objects if o.type == "ARMATURE" and o.get(BROWSER_TAG)), None)
        added = []
        if arm is not None and any(x.obj is not None and x.visible for x in arm.tt_attachments):
            added = publish_items(context, arm, self.report)
            if added is None:
                return {"CANCELLED"}
        written = write_changed(context, arm, {o: o["tt_source"] for o in loaded_models()})
        publish_own_textures(repo_root(context), loaded_models())
        painted = publish_skin_paint(repo_root(context))
        clips = publish_changed_clips(context, arm, self.report) if arm is not None else []
        saved = added + [os.path.basename(o["tt_source"]) for o in written] + painted + clips
        self.report({"INFO"}, f"Saved {', '.join(saved)}" if saved else "Nothing changed since loading")
        return {"FINISHED"}


class AddToScene(bpy.types.Operator):
    """Put this model beside what is on screen, at the same scale, to size against or compose with. Move it as you
    like; it is never published and stays when you load another model"""
    bl_idname = "wm.tt_add_to_scene"
    bl_label = "Add To Scene"
    bl_options = {"REGISTER", "UNDO"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context))

    def execute(self, context):
        if not any((u.group, u.sprite) == (self.group, self.sprite) for u in context.window_manager.tt_units):
            self.report({"ERROR"}, f"No model {self.group} / {self.sprite} in the list")
            return {"CANCELLED"}
        if context.mode != "OBJECT" and context.view_layer.objects.active is not None:
            bpy.ops.object.mode_set(mode="OBJECT")
        return {"FINISHED"} if add_reference(context, self.group, self.sprite, self.report) else {"CANCELLED"}


class RemoveAdded(bpy.types.Operator):
    """Remove every model added to the scene from the Models list"""
    bl_idname = "wm.tt_remove_added"
    bl_label = "Remove Added Objects"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return bool(references())

    def execute(self, context):
        clear_references()
        return {"FINISHED"}


def units_list_menu(self, context):
    """Right click on a row of the Models list."""
    op = getattr(context, "button_operator", None)
    if op is None or op.bl_rna.identifier != "WM_OT_tt_pick_unit":
        return
    layout = self.layout
    layout.separator()
    add = layout.operator(AddToScene.bl_idname, text=f"Add To Scene: {op.sprite}", icon="ADD")
    add.group, add.sprite = op.group, op.sprite
    layout.operator(RemoveAdded.bl_idname, icon="X")


class ShowItem(bpy.types.Operator):
    """Show or hide this item. Carried things and props switch on and off one by one. Hats and weapons swap, the
    way the game shows one per kind; hold Shift to keep the others showing"""
    bl_idname = "object.tt_show_item"
    bl_label = "Show Item"
    bl_options = {"REGISTER", "UNDO"}
    item: StringProperty()
    keep_others: BoolProperty(options={"SKIP_SAVE"})

    def invoke(self, context, event):
        self.keep_others = event.shift
        return self.execute(context)

    def execute(self, context):
        holder = props_holder(context)
        chosen = bpy.data.objects.get(self.item)
        if holder is None or chosen is None:
            return {"CANCELLED"}
        show = not item_shown(chosen)
        if self.keep_others or chosen["tt_slot"] in (CARRY_SLOT, PROP_SLOT):
            set_item_visible(chosen, show)
            return {"FINISHED"}
        for obj in unit_items(holder).get(chosen["tt_slot"], []):
            set_item_visible(obj, obj == chosen and show)
        return {"FINISHED"}


class ExportToRepo(bpy.types.Operator):
    """Write every visible attachment into the unit's folder in the repo: registry items back to their own
    file, new items next to the unit as <object name>.xml"""
    bl_idname = "export_mesh.tt_to_repo"
    bl_label = "Export Visible To Repo"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(arm.get("tt_skeleton")) and bool(repo_root(context))

    def execute(self, context):
        return {"FINISHED"} if export_visible(context, active_armature(context), self.report) else {"CANCELLED"}


class AddToRegistry(bpy.types.Operator):
    """Write the visible new attachments into geometry.xml so the game and this browser pick them up"""
    bl_idname = "object.tt_add_to_registry"
    bl_label = "Add To Registry"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return rig_in_repo(context, arm) and bool(visible_attachments(arm))

    def execute(self, context):
        arm = active_armature(context)
        group, base = find_base_sprite(arm.get("tt_skeleton", ""))
        if base is None:
            self.report({"ERROR"}, "Could not find this unit's sprite in geometry.xml")
            return {"CANCELLED"}
        added = append_registry_entries(os.path.join(repo_root(context), REGISTRY_FILE), group,
                                        registry_entries(context, arm, base, group=group))
        self.report({"INFO"}, f"Added {added} sprite entr{'y' if added == 1 else 'ies'} to group {group}")
        return {"FINISHED"}


class PutOnBone(bpy.types.Operator):
    """Move your mesh to the part of the unit picked under Where. On the head it sits on top; anywhere else its
    origin goes to the joint, so model a held item with its grip at the origin. Nudge it afterwards as you like"""
    bl_idname = "object.tt_put_on_bone"
    bl_label = "Snap To Bone"
    bl_options = {"REGISTER", "UNDO"}
    point: StringProperty(options={"SKIP_SAVE"})
    fit: BoolProperty(name="Shrink it if it is bigger than the unit", default=True)

    def execute(self, context):
        arm = active_armature(context)
        slot = next((x for x in arm.tt_attachments if x.point == self.point), None) if arm is not None else None
        if slot is None or slot.obj is None or slot.bone not in arm.pose.bones:
            self.report({"ERROR"}, "Pick where it goes and your mesh first")
            return {"CANCELLED"}
        note = snap_to_bone(context, arm, slot.obj, slot.bone, self.point == "HEAD", self.fit)
        self.report({"INFO"}, f"{slot.obj.name} snapped to the {POINT_LABELS.get(self.point, self.point).lower()}"
                              f"{note}")
        return {"FINISHED"}


def switch_workspace(context, name):
    workspace = bpy.data.workspaces.get(name)
    if workspace is not None and context.window is not None:
        context.window.workspace = workspace


def show_paint_canvas(image):
    """Put image on the paint canvas and in the Texture Paint workspace's image editors."""
    bpy.context.scene.tool_settings.image_paint.mode = "IMAGE"
    bpy.context.scene.tool_settings.image_paint.canvas = image
    workspace = bpy.data.workspaces.get("Texture Paint")
    for screen in workspace.screens if workspace is not None else []:
        for area in screen.areas:
            if area.type == "IMAGE_EDITOR":
                area.spaces.active.image = image


def area_to_split(areas):
    """The largest 3D view, split to make room for the image, or None when an image editor already shows."""
    if any(area.type == "IMAGE_EDITOR" for area in areas):
        return None
    views = [area for area in areas if area.type == "VIEW_3D"]
    return max(views, key=lambda area: area.width * area.height) if views else None


def show_image_view(workspace_name, image):
    """Give the window on this workspace an image editor showing image, splitting its 3D view if it has none."""
    window = next((w for w in bpy.context.window_manager.windows if w.workspace.name == workspace_name), None)
    if window is None:
        return
    screen = window.screen
    area = area_to_split(screen.areas)
    if area is not None:
        others = {a.as_pointer() for a in screen.areas if a != area}
        region = next(r for r in area.regions if r.type == "WINDOW")
        with bpy.context.temp_override(window=window, screen=screen, area=area, region=region):
            bpy.ops.screen.area_split(direction="VERTICAL", factor=0.4)
        left = min((a for a in screen.areas if a.as_pointer() not in others), key=lambda a: a.x)
        left.type = "IMAGE_EDITOR"
        if hasattr(left.spaces.active, "ui_mode"):
            left.spaces.active.ui_mode = "PAINT"
    for a in screen.areas:
        if a.type == "IMAGE_EDITOR":
            a.spaces.active.image = image


def paint_canvas_later(workspace_name, image_name):
    image = bpy.data.images.get(image_name)
    if image is None:
        return None
    show_paint_canvas(image)
    try:
        show_image_view(workspace_name, image)
    except Exception as e:
        message = f"Could not open an image view to paint in ({e}); open an Image Editor by hand"
        print("Tribal Trouble:", message)
        bpy.context.window_manager.popup_menu(lambda menu, _: menu.layout.label(text=message), title="Paint It",
                                              icon="ERROR")
    return None


class PaintItem(bpy.types.Operator):
    """Start painting this mesh's texture: selects it, picks its image as the canvas and opens Texture Paint"""
    bl_idname = "object.tt_paint_item"
    bl_label = "Paint It"
    target: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        obj = bpy.data.objects.get(self.target) or context.active_object
        image = mesh_texture_image(obj) if obj is not None and obj.type == "MESH" else None
        if image is None:
            self.report({"ERROR"}, "This mesh has no texture yet: press Make A Texture For It")
            return {"CANCELLED"}
        if context.mode != "OBJECT":
            bpy.ops.object.mode_set(mode="OBJECT")
        for o in context.selected_objects:
            o.select_set(False)
        obj.hide_set(False)
        obj.select_set(True)
        context.view_layer.objects.active = obj
        bpy.ops.object.mode_set(mode="TEXTURE_PAINT")
        show_paint_canvas(image)
        workspace = "Texture Paint" if "Texture Paint" in bpy.data.workspaces or context.window is None \
            else context.window.workspace.name
        switch_workspace(context, "Texture Paint")
        # The workspace switch lands after this operator and its first entry resets the canvas, so set it again then.
        name = image.name
        bpy.app.timers.register(lambda: paint_canvas_later(workspace, name), first_interval=0.1)
        self.report({"INFO"}, f"Painting {image.name}. Press Done Painting in the Tribal Trouble tab when finished")
        return {"FINISHED"}


class DonePainting(bpy.types.Operator):
    """Leave Texture Paint and go back to the unit. The paint is kept; Publish writes it"""
    bl_idname = "object.tt_done_painting"
    bl_label = "Done Painting"

    def execute(self, context):
        if context.mode != "OBJECT":
            bpy.ops.object.mode_set(mode="OBJECT")
        switch_workspace(context, "Layout")
        return {"FINISHED"}


class MakeTexture(bpy.types.Operator):
    """Give this mesh what the game needs to show it: a UV map if it has none, and a material with one image named
    after the mesh, filled with the color the material had. Then paint it in the Texture Paint tab"""
    bl_idname = "object.tt_make_texture"
    bl_label = "Make A Texture For It"
    bl_options = {"REGISTER", "UNDO"}
    target: StringProperty(options={"SKIP_SAVE"})
    size: EnumProperty(name="Size", default="256", items=(
        ("128", "128", "Tiny items"), ("256", "256", "Hats and hand items"), ("512", "512", "Large or detailed items"),
        ("1024", "1024", "As large as a whole unit's texture")))

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        obj = bpy.data.objects.get(self.target) or context.active_object
        if obj is None or obj.type != "MESH":
            self.report({"ERROR"}, "Pick a mesh first")
            return {"CANCELLED"}
        if mesh_texture_image(obj) is not None:
            self.report({"INFO"}, f"{obj.name} already has a texture: {mesh_texture_image(obj).name}")
            return {"CANCELLED"}
        name = re.sub(r"[^A-Za-z0-9_]+", "_", obj.name).strip("_").lower() or "item"
        if name != obj.name:
            obj.name = name  # it becomes the file and sprite name
            for holder in bpy.data.objects:
                for slot in holder.tt_attachments:
                    if slot.obj == obj:
                        slot.prev_name = obj.name
        root = repo_root(context)
        image_name, n = name, 1
        while image_name in bpy.data.images or root and os.path.isfile(models_texture_path(root, image_name)):
            n += 1
            image_name = f"{name}_{n}"  # never paint over another model's texture
        if not obj.data.uv_layers:
            previous = context.view_layer.objects.active
            for o in context.selected_objects:
                o.select_set(False)
            obj.select_set(True)
            context.view_layer.objects.active = obj
            bpy.ops.object.mode_set(mode="EDIT")
            bpy.ops.mesh.select_all(action="SELECT")
            bpy.ops.uv.smart_project(island_margin=0.02)
            bpy.ops.object.mode_set(mode="OBJECT")
            context.view_layer.objects.active = previous or obj
        mat = obj.active_material
        if mat is None:
            mat = bpy.data.materials.new("tt_" + name)
            obj.data.materials.append(mat)
        mat.use_nodes = True
        nodes, links = mat.node_tree.nodes, mat.node_tree.links
        bsdf = next((n for n in nodes if n.type == "BSDF_PRINCIPLED"), None)
        color = tuple(bsdf.inputs["Base Color"].default_value) if bsdf is not None else (0.6, 0.6, 0.6, 1.0)
        size = int(self.size)
        image = bpy.data.images.new(image_name, size, size, alpha=True)
        image.generated_color = color[:3] + (1.0,)
        tex = nodes.new("ShaderNodeTexImage")
        tex.image = image
        tex.location = (-350, 300)
        if bsdf is not None:
            links.new(bsdf.inputs["Base Color"], tex.outputs["Color"])
            bsdf.inputs["Roughness"].default_value = 1.0
        nodes.active = tex  # Texture Paint paints the active image node
        self.report({"INFO"}, f"{obj.name} now has the texture {image_name} ({size}x{size}). Paint it in the Texture "
                              f"Paint tab; Publish writes it into the repo")
        return {"FINISHED"}


class NewItem(bpy.types.Operator):
    """Hang one of your meshes on this unit: it follows that part straight away. Publish saves it"""
    bl_idname = "object.tt_new_item"
    bl_label = "New Prop..."
    bl_options = {"REGISTER", "UNDO"}
    point: EnumProperty(name="Where", items=point_items, description="The part of the unit your new mesh follows")
    mesh: StringProperty(name="Mesh", search=own_mesh_search, options={"SKIP_SAVE"},
                         description="One of your own meshes in this scene")
    snap: BoolProperty(name="Snap", default=True, options={"SKIP_SAVE"},
                       description="Move the mesh onto that part of the unit")
    make_texture: BoolProperty(name="Make a texture for it", default=True, options={"SKIP_SAVE"},
                               description="Give the mesh a UV map and an image named after it to paint")
    event: event_property()
    on_by_default: BoolProperty(name="On by default", options={"SKIP_SAVE"},
                                description="Every unit wears it without a player choosing it")

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(arm.tt_attachments)

    def invoke(self, context, event):
        picked = next((o for o in context.selected_objects if attachment_obj_poll(self, o)), None)
        if picked is not None:
            self.mesh = picked.name
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "point")
        layout.prop(self, "mesh", icon="MESH_DATA")
        layout.prop(self, "snap", text=f"Snap to {POINT_LABELS.get(self.point, self.point).lower()}")
        draw_event(layout, self)
        layout.prop(self, "on_by_default")
        obj = bpy.data.objects.get(self.mesh)
        if obj is not None and obj.type == "MESH" and mesh_texture_image(obj) is None:
            layout.prop(self, "make_texture")
        draw_confirm(layout, self, mesh_problem(self.mesh))

    def execute(self, context):
        arm = active_armature(context)
        slot = next((x for x in arm.tt_attachments if x.point == self.point), None)
        obj = bpy.data.objects.get(self.mesh)
        if slot is None or obj is None or not attachment_obj_poll(self, obj):
            self.report({"ERROR"}, "Pick where it goes and one of your own meshes")
            return {"CANCELLED"}
        slot.obj = obj
        obj["tt_event"], obj["tt_default"] = chosen_event(self), self.on_by_default
        context.view_layer.objects.active = arm
        if self.snap:
            bpy.ops.object.tt_put_on_bone(point=self.point)
        if self.make_texture and mesh_texture_image(obj) is None:
            bpy.ops.object.tt_make_texture(target=obj.name)
        self.report({"INFO"}, f"{obj.name} follows the {POINT_LABELS.get(self.point, self.point).lower()}. "
                              f"Publish to save it")
        return {"FINISHED"}


def shares_unit_texture(arm, obj):
    """True when obj is drawn from the loaded unit body's own texture atlas."""
    body = browsed_unit(arm) if arm is not None else None
    if body is None or obj is None or obj.type != "MESH" or obj in model_levels(body):
        return False
    return bool(set(texture_names(obj)) & set(texture_names(body)))


class OwnTexture(bpy.types.Operator):
    """Copy the part of the unit's texture this item uses into textures of its own, one per tier with its team
    decal, and move its UVs onto them. The unit and its texture are not changed; Publish writes the new images and
    lists them for the item"""
    bl_idname = "object.tt_own_texture"
    bl_label = "Give It Its Own Texture"
    bl_options = {"REGISTER", "UNDO"}
    target: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        arm = active_armature(context)
        obj = bpy.data.objects.get(self.target)
        root = repo_root(context)
        if not root or not shares_unit_texture(arm, obj):
            self.report({"ERROR"}, "Pick an item that uses the loaded unit's texture")
            return {"CANCELLED"}
        levels = model_levels(obj)
        name = race_texture_name(obj.get("tt_group", ""), obj.get("tt_sprite") or obj.name)
        textures = texture_names(obj)
        fresh = [name] if len(textures) == 1 else [f"{name}_{label}" for label in short_labels(textures)]
        decal_path = lambda texture: os.path.join(root, "assets", "textures", "teamdecals", texture + "_team.png")
        taken = [n for n in fresh if n in bpy.data.images or n + "_team" in bpy.data.images
                 or os.path.isfile(models_texture_path(root, n)) or os.path.isfile(decal_path(n))]
        if taken:
            self.report({"ERROR"}, f"A texture named {', '.join(taken)} already exists; nothing was changed, rename "
                                   f"the item first")
            return {"CANCELLED"}
        if any(not os.path.isfile(models_texture_path(root, t)) for t in textures):
            self.report({"ERROR"}, f"{', '.join(textures)} must all be in assets/textures/models")
            return {"CANCELLED"}
        atlases = [bpy.data.images.load(models_texture_path(root, t), check_existing=True) for t in textures]
        decals = [bpy.data.images.load(decal_path(t), check_existing=True) if os.path.isfile(decal_path(t)) else None
                  for t in textures]
        w, h = atlases[0].size
        step = next((w // d.size[0] for d in decals if d is not None), 1)
        if any(tuple(a.size) != (w, h) for a in atlases) or any(
                d is not None and tuple(d.size) != (w // step, h // step) for d in decals):
            self.report({"ERROR"}, "The tier textures (and their team decals) are not all the same size")
            return {"CANCELLED"}
        layers = [layer for o in levels for layer in o.data.uv_layers]
        uvs = [np.empty(2 * len(layer.data), np.float32) for layer in layers]
        for layer, uv in zip(layers, uvs):
            layer.data.foreach_get("uv", uv)
        if not layers or not all(len(uv) for uv in uvs):
            self.report({"ERROR"}, f"{obj.name} has no UV map")
            return {"CANCELLED"}
        us, vs = np.concatenate([uv[0::2] for uv in uvs]), np.concatenate([uv[1::2] for uv in uvs])
        if min(us.min(), vs.min()) < -1e-4 or max(us.max(), vs.max()) > 1.0001:
            self.report({"ERROR"}, f"{obj.name}: UVs leave the 0 to 1 square, so there is no part of the texture to "
                                   f"copy")
            return {"CANCELLED"}
        x0, y0 = max(0, int(np.floor(us.min() * w)) - MIP_PAD), max(0, int(np.floor(vs.min() * h)) - MIP_PAD)
        x1, y1 = min(w, int(np.ceil(us.max() * w)) + MIP_PAD), min(h, int(np.ceil(vs.max() * h)) + MIP_PAD)
        x0, y0 = x0 - x0 % step, y0 - y0 % step  # so the smaller team decal crops on whole pixels too
        size = max(step, 1 << (max(x1 - x0, y1 - y0) - 1).bit_length())
        materials = []
        for new, atlas, decal in zip(fresh, atlases, decals):
            image = bpy.data.images.new(new, size, size, alpha=True)
            image.pixels.foreach_set(crop_pixels(atlas, x0, y0, x1, y1, size).ravel())
            team = None
            if decal is not None:
                team = bpy.data.images.new(new + "_team", size // step, size // step, alpha=True)
                team.colorspace_settings.name = "Non-Color"
                team.pixels.foreach_set(crop_pixels(decal, x0 // step, y0 // step, -(-x1 // step), -(-y1 // step),
                                                    size // step).ravel())
            materials.append(get_atlas_material(new, models_texture_path(root, new), image, team))
        for layer, uv in zip(layers, uvs):
            uv[0::2] = (uv[0::2] * w - x0) / size
            uv[1::2] = (uv[1::2] * h - y0) / size
            layer.data.foreach_set("uv", uv)
        renamed = dict(zip(textures, fresh))
        for o in levels:
            o.data.update()
            o.data.materials.clear()
            o.data.materials.append(materials[0])
            file_textures = [t.strip() for t in o.get("tt_file_texture", "").split(",") if t.strip()]
            o["tt_texture"] = ",".join(fresh)
            o["tt_file_texture"] = ",".join(renamed.get(t, t) for t in file_textures) or fresh[0]
        obj["tt_own_texture"] = True
        apply_team_preview(context)
        self.report({"INFO"}, f"{obj.name} now has its own texture: {', '.join(fresh)} ({size}x{size}). Publish "
                              f"writes it into the repo")
        return {"FINISHED"}


class SaveItems(bpy.types.Operator):
    """Write the unit's visible items and list the new ones in geometry.xml, as Publish does"""
    bl_idname = "object.tt_save_items"
    bl_label = "Publish Items"

    @classmethod
    def poll(cls, context):
        if rig_in_repo(context, active_armature(context)):
            return True
        cls.poll_message_set("Load the unit from the Models list of your repo folder")
        return False

    def execute(self, context):
        added = publish_items(context, active_armature(context), self.report)
        if added is None:
            return {"CANCELLED"}
        self.report({"INFO"}, f"Published {len(added)} new item(s); the game shows them after the next build")
        return {"FINISHED"}


class SetClip(bpy.types.Operator):
    """Show this clip on the unit and fit the timeline to it"""
    bl_idname = "object.tt_set_clip"
    bl_label = "Show Clip"
    clip: StringProperty()

    def execute(self, context):
        arm = active_armature(context)
        action = bpy.data.actions.get(self.clip)
        if arm is None or action is None:
            return {"CANCELLED"}
        show_clip(context, arm, action)
        return {"FINISHED"}


class ShowItemClip(bpy.types.Operator):
    """Show a clip this item appears in"""
    bl_idname = "object.tt_show_item_clip"
    bl_label = "Show Item's Clip"
    item: StringProperty()

    @classmethod
    def description(cls, context, properties):
        arm = active_armature(context)
        obj = bpy.data.objects.get(properties.item)
        clips = item_hidden_here(arm, obj) if arm is not None and obj is not None else None
        if not clips:
            return cls.__doc__
        names = ", ".join(clip_short_name(arm, a) for a in clips)
        return f"Hidden in {clip_short_name(arm, arm.animation_data.action)}. Only visible in: {names}"

    def execute(self, context):
        arm = active_armature(context)
        obj = bpy.data.objects.get(self.item)
        clips = item_hidden_here(arm, obj) if arm is not None and obj is not None else None
        if not clips:
            return {"CANCELLED"}
        show_clip(context, arm, clips[0])
        return {"FINISHED"}


class SetTier(bpy.types.Operator):
    """Show the unit, and every attachment that has one texture per tier, in this tier's texture"""
    bl_idname = "object.tt_set_tier"
    bl_label = "Show Tier"
    bl_options = {"REGISTER", "UNDO"}
    index: IntProperty()

    def execute(self, context):
        arm = active_armature(context)
        root = repo_root(context)
        if arm is None or not root:
            return {"CANCELLED"}
        for obj in unit_meshes(arm):
            names = [t.strip() for t in obj["tt_texture"].split(",") if t.strip()]
            if self.index >= len(names):
                continue
            path = os.path.join(root, "assets", "textures", "models", names[self.index] + ".png")
            if not os.path.isfile(path) and "tt_" + names[self.index] not in bpy.data.materials:
                self.report({"WARNING"}, f"{names[self.index]}.png is not in assets/textures/models")
                continue
            obj.data.materials.clear()
            obj.data.materials.append(get_atlas_material(names[self.index], path))
        arm["tt_tier"] = self.index
        apply_team_preview(context)
        return {"FINISHED"}


class MaterialPreview(bpy.types.Operator):
    """Team color only shows in Material Preview shading; the first switch can take a minute to compile"""
    bl_idname = "view3d.tt_material_preview"
    bl_label = "Switch To Material Preview"

    def execute(self, context):
        context.space_data.shading.type = "MATERIAL"
        return {"FINISHED"}


CHECK_ICONS = {"ERROR": "CANCEL", "WARNING": "ERROR", "INFO": "INFO"}


class TTCheck(bpy.types.PropertyGroup):
    level: StringProperty()


class Preflight(bpy.types.Operator):
    """Check the visible attachments for what breaks in game: UVs, texture, naming, flipped faces, tint, weights"""
    bl_idname = "object.tt_preflight"
    bl_label = "Check Before Export"

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def execute(self, context):
        findings = preflight(context, active_armature(context))
        errors = store_findings(context, findings)
        self.report({"ERROR"} if errors else {"INFO"},
                    f"{errors} problem(s) to fix" if errors else f"Ready to export, {len(findings)} note(s)")
        return {"FINISHED"}


class RemoveFromRegistry(bpy.types.Operator):
    """Take this sprite out of geometry.xml. Its files stay on disk. Refused while another sprite is based on it"""
    bl_idname = "object.tt_remove_from_registry"
    bl_label = "Remove From Registry"
    bl_options = {"REGISTER"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context))

    def invoke(self, context, event):
        wm = context.window_manager
        if not self.sprite and 0 <= wm.tt_unit_index < len(wm.tt_units):
            self.group, self.sprite = wm.tt_units[wm.tt_unit_index].group, wm.tt_units[wm.tt_unit_index].sprite
        return wm.invoke_confirm(self, event)

    def execute(self, context):
        root = repo_root(context)
        dependents = [s["name"] for s in read_registry(root) if s["group"] == self.group and s["base"] == self.sprite]
        if dependents:
            self.report({"ERROR"}, f"{self.sprite} is the base of {', '.join(dependents)}: remove those first")
            return {"CANCELLED"}
        if not remove_registry_entry(os.path.join(root, REGISTRY_FILE), self.group, self.sprite):
            self.report({"ERROR"}, f"No sprite named {self.sprite} in group {self.group}")
            return {"CANCELLED"}
        for obj in [o for o in bpy.data.objects if o.get("tt_sprite") == self.sprite and o.get(BROWSER_TAG)
                    and o.get("tt_group", "") in ("", self.group)]:
            bpy.data.objects.remove(obj, do_unlink=True)
        refresh_units(context)
        self.report({"INFO"}, f"Removed {self.group} / {self.sprite} from the registry; its files are still on disk")
        return {"FINISHED"}


class VIEW3D_PT_tt_preview(bpy.types.Panel):
    bl_label = "Preview"
    bl_order = 3
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def draw(self, context):
        layout = self.layout
        wm = context.window_manager
        arm = active_armature(context)
        actions = sorted(armature_actions(arm), key=lambda a: a.name)
        current = arm.animation_data.action if arm.animation_data is not None else None
        if actions:
            grid = layout.grid_flow(row_major=True, columns=3, even_columns=True, align=True)
            for action, label in zip(actions, short_labels([a.name for a in actions])):
                grid.operator(SetClip.bl_idname, text=label, depress=action == current).clip = action.name
            playing = context.screen.is_animation_playing
            layout.operator("screen.animation_play", text="Pause" if playing else "Play",
                            icon="PAUSE" if playing else "PLAY")
        row = layout.row(align=True)
        row.operator(NewClip.bl_idname, icon="ADD")
        row.operator(DeleteClip.bl_idname, text="", icon="TRASH")
        tiers = unit_tiers(arm)
        if len(tiers) > 1:
            row = layout.row(align=True)
            for index, label in enumerate(short_labels(tiers)):
                row.operator(SetTier.bl_idname, text=label, depress=arm.get("tt_tier", 0) == index).index = index
        if has_low_detail():
            layout.row(align=True).prop(wm, "tt_detail", expand=True)
        row = layout.row(align=True)
        row.prop(wm, "tt_team_preview", toggle=True)
        row.prop(wm, "tt_team_color", text="")
        if wm.tt_team_preview and context.space_data.shading.type not in ("MATERIAL", "RENDERED"):
            layout.operator(MaterialPreview.bl_idname, icon="SHADING_TEXTURE")


class DeleteClip(bpy.types.Operator):
    """Delete this clip. One that was never saved just goes. A saved one also leaves geometry.xml and its file is
    deleted; only the last clip in the list can go, because the game finds clips by their number"""
    bl_idname = "object.tt_delete_clip"
    bl_label = "Delete Clip"
    bl_options = {"REGISTER"}
    clip: StringProperty(options={"SKIP_SAVE"})

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def target(self, context):
        arm = active_armature(context)
        current = arm.animation_data.action if arm.animation_data is not None else None
        return bpy.data.actions.get(self.clip) if self.clip else current

    def invoke(self, context, event):
        return context.window_manager.invoke_confirm(self, event) if self.target(context) is not None \
            else {"CANCELLED"}

    def execute(self, context):
        arm = active_armature(context)
        action = self.target(context)
        if action is None:
            return {"CANCELLED"}
        root = repo_root(context)
        group, base, rig = rig_registry(context, arm)
        saved = next((name for name, (_, _, path) in (rig["clip_info"].items() if rig else [])
                      if action.get("tt_clip") and os.path.basename(path) == action["tt_clip"]), None)
        label = clip_short_name(arm, action)
        if saved is not None:
            body = browsed_unit(arm)
            if body is not None and body["tt_sprite"] != base:
                self.report({"ERROR"}, f"{body['tt_sprite']} borrows the {base} rig and its clips: load {base} to "
                                       f"change them")
                return {"CANCELLED"}
            if list(rig["clip_info"])[-1] != saved:
                self.report({"ERROR"}, f"Only the last clip ({list(rig['clip_info'])[-1]}) can be deleted: the game "
                                       f"finds clips by their number, and removing {saved} would shift the ones after it")
                return {"CANCELLED"}
            relative = rig["clip_info"][saved][2]
            touched = set_clip_line(os.path.join(root, REGISTRY_FILE), group, rig["skeleton"].replace("\\", "/"), saved)
            still_used = any(relative in s["clips"] for s in read_registry(root))
            path = os.path.join(root, GEOMETRY_DIR, relative)
            if not still_used and os.path.isfile(path):
                os.remove(path)
            self.report({"INFO"}, f"Deleted clip {saved} from {', '.join(touched)} and removed {relative}")
        else:
            self.report({"INFO"}, f"Deleted unsaved clip {label}")
        others = [a for a in armature_actions(arm) if a != action]
        fallback = next((a for a in others if "idle" in a.name), others[0] if others else None)
        if arm.animation_data is not None and arm.animation_data.action == action:
            arm.animation_data.action = None
            if fallback is not None:
                assign_action(arm, fallback)
        bpy.data.actions.remove(action)
        return {"FINISHED"}


def clip_button_menu(self, context):
    """Right click on a clip button."""
    op = getattr(context, "button_operator", None)
    if op is not None and op.bl_rna.identifier == "OBJECT_OT_tt_set_clip":
        self.layout.separator()
        self.layout.operator(DeleteClip.bl_idname, icon="TRASH").clip = op.clip


CLIP_KINDS = (("loop", "Looping", "Repeats, like idle and run"),
              ("plain", "Once", "Plays once and holds, like attack and die"))
WPC_DESCRIPTION = ("For a walk or run: how far the unit travels in one loop, so feet do not slide. Leave at 1 for "
                   "anything that stays in place")


class NewClip(bpy.types.Operator):
    """Start a new clip for this unit. Game clips have a key on every frame for every bone, which is miserable to
    edit by hand, so the default starts from the pose on screen with only a first and a last key"""
    bl_idname = "object.tt_new_clip"
    bl_label = "New Clip"
    bl_options = {"REGISTER", "UNDO"}
    clip_name: StringProperty(name="Name", default="", description="Short name such as dance or wave")
    start: EnumProperty(name="Start From", items=(
        ("POSE", "This pose", "The pose on screen, keyed on the first and last frame only: easy to animate by hand"),
        ("COPY", "Copy of this clip", "Every frame of the clip showing: good for small fixes, hard to re-animate")))
    length: IntProperty(name="Frames", default=25, min=2, max=500, description="Length of a clip started from a pose")
    kind: EnumProperty(name="Plays", items=CLIP_KINDS)
    wpc: FloatProperty(name="Distance Per Loop", default=1.0, min=0.0001, description=WPC_DESCRIPTION)

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def invoke(self, context, event):
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        for prop in ("clip_name", "start", "length", "kind", "wpc"):
            layout.prop(self, prop)
        draw_confirm(layout, self, name_problem(self.clip_name))

    def execute(self, context):
        arm = active_armature(context)
        name = self.clip_name.strip().lower()
        if not re.fullmatch(r"[a-z0-9_]+", name):
            self.report({"ERROR"}, "Name the clip with letters, digits and underscores")
            return {"CANCELLED"}
        rig = rig_registry(context, arm)[2]
        taken = {clip_short_name(arm, a) for a in armature_actions(arm)} | set(rig["clip_info"] if rig else ())
        if name in taken:
            self.report({"ERROR"}, f"This unit already has a clip called {name}")
            return {"CANCELLED"}
        current = arm.animation_data.action if arm.animation_data is not None else None
        prefix = os.path.basename(arm.get("tt_skeleton", "")).replace("skeleton.xml", "")
        from_pose = self.start == "POSE" or current is None
        action = bpy.data.actions.new(prefix + name) if from_pose else current.copy()
        action.name = prefix + name
        action.use_fake_user = True
        action["tt_armature"] = arm.name
        if "tt_clip" in action:
            del action["tt_clip"]
        action["tt_kind"], action["tt_wpc"] = self.kind, self.wpc
        if arm.animation_data is not None:
            arm.animation_data.action = None  # a copied action brings its own slot; let assign_action bind it
        assign_action(arm, action)
        if from_pose:
            # Pose bones still hold the values of the pose that was showing.
            for pb in arm.pose.bones:
                pb.rotation_mode = "QUATERNION"
                for frame in (1, self.length):
                    for path in ("location", "rotation_quaternion", "scale"):
                        pb.keyframe_insert(path, frame=frame, group=pb.name)
            action["tt_shown_bones"] = shown_bones([{pb.name: pb.matrix for pb in arm.pose.bones}])
        context.scene.frame_start = 1
        context.scene.frame_end = max(1, int(round(action.frame_range[1])))
        context.scene.frame_set(1)
        return {"FINISHED"}


class SaveClip(bpy.types.Operator):
    """Publish the clip that is showing, with how it plays"""
    bl_idname = "object.tt_save_clip"
    bl_label = "Publish Clip"
    clip_name: StringProperty(name="Name", description="The clip's name in geometry.xml, such as run or dance")
    kind: EnumProperty(name="Plays", items=CLIP_KINDS)
    wpc: FloatProperty(name="Distance Per Loop", default=1.0, min=0.0001, description=WPC_DESCRIPTION)

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return rig_in_repo(context, arm) and arm.animation_data is not None and arm.animation_data.action is not None

    def execute(self, context):
        arm = active_armature(context)
        published = publish_clip(context, arm, arm.animation_data.action, self.clip_name, self.kind, self.wpc,
                                 self.report)
        return {"FINISHED"} if published else {"CANCELLED"}


ADDON_SOURCE = os.path.join("tools", "blender", "io_tribaltrouble", "__init__.py")
_repo_version_cache = {}


def version_from_source(path):
    """bl_info version of an add-on source file, read as text so nothing gets imported."""
    try:
        with open(path, encoding="utf-8") as f:
            match = re.search(r'"version":\s*\((\d+),\s*(\d+),\s*(\d+)\)', f.read(20000))
    except OSError:
        return None
    return tuple(int(g) for g in match.groups()) if match else None


def update_available(context):
    """Version of the add-on in the repo folder when it is newer than this installed copy, else None."""
    root = repo_root(context)
    if not root:
        return None
    source = os.path.join(root, ADDON_SOURCE)
    if os.path.normcase(os.path.abspath(source)) == os.path.normcase(os.path.abspath(__file__)):
        return None
    try:
        stamp = os.path.getmtime(source)
    except OSError:
        return None
    if _repo_version_cache.get(source, (None,))[0] != stamp:
        _repo_version_cache[source] = (stamp, version_from_source(source))
    version = _repo_version_cache[source][1]
    # Blender removes bl_info from extension modules, so the installed version is read from this file's text too.
    if __file__ not in _repo_version_cache:
        _repo_version_cache[__file__] = (None, version_from_source(__file__))
    installed = _repo_version_cache[__file__][1]
    return version if version is not None and installed is not None and version > installed else None


def draw_update_button(layout, context):
    newer = update_available(context)
    if newer is not None:
        row = layout.row()
        row.alert = True
        row.operator(UpdateAddon.bl_idname, text="Update add-on to " + ".".join(map(str, newer)), icon="FILE_REFRESH")


def install_repo_addon(context):
    """Copy the repo's add-on over this installed file. The running code is unchanged until a reload."""
    version = update_available(context)
    if version is not None:
        shutil.copyfile(os.path.join(repo_root(context), ADDON_SOURCE), __file__)
    return version


def reload_addon():
    import addon_utils
    addon_utils.disable(__package__)
    addon_utils.enable(__package__, default_set=True)
    return None


class UpdateAddon(bpy.types.Operator):
    """Replace the installed add-on with the newer copy in your repo folder and reload it"""
    bl_idname = "wm.tt_update_addon"
    bl_label = "Update Add-on"

    def execute(self, context):
        version = install_repo_addon(context)
        if version is None:
            self.report({"INFO"}, "The installed add-on is already as new as the one in the repo")
            return {"CANCELLED"}
        bpy.app.timers.register(reload_addon, first_interval=0.1)  # not from inside the module being replaced
        self.report({"INFO"}, "Updated to " + ".".join(map(str, version)))
        return {"FINISHED"}


PLAIN_CLIPS = ("attack", "die", "death", "throw")
_group_items = []


def registry_group_items(self, context):
    root = repo_root(context)
    names = sorted({s["group"] for s in read_registry(root)}) if root else []
    _group_items[:] = [(n, n, "") for n in names] or [("misc", "misc", "")]
    return _group_items


# Landscape.Ground names the game scatters decorations on; land is any of them.
DECORATION_GROUNDS = (
    ("grass", "Grass", ""),
    ("dirt", "Dirt", ""),
    ("beach", "Beach", "Sand on native maps, gravel on viking maps"),
    ("snow", "Snow", "Viking maps only"),
    ("land", "Any land", "Any ground above the sea"),
)


def mesh_names_sprite(self, context):
    self.sprite_name = self.mesh


class RegisterModel(bpy.types.Operator):
    """Export the picked mesh as a brand new model and add it to geometry.xml: a static building or prop, a unit on
    the rig it is bound to, a unit with its own new rig, or map scenery the game scatters by itself. Using a model
    that is not scattered still needs code"""
    bl_idname = "object.tt_register_model"
    bl_label = "New Model..."
    bl_options = {"REGISTER"}
    mesh: StringProperty(name="Mesh", search=own_mesh_search, options={"SKIP_SAVE"},
                         description="One of your own meshes in this scene", update=mesh_names_sprite)
    sprite_name: StringProperty(name="Name", options={"SKIP_SAVE"},
                                description="Sprite name in geometry.xml and the new folder's name")
    scatter: BoolProperty(name="Scatter on map", options={"SKIP_SAVE"},
                          description="Map scenery the game scatters over every map by itself, such as pumpkin patches")
    group: EnumProperty(name="Group", items=registry_group_items)
    low_detail: StringProperty(name="Low Detail", description="Optional mesh object shown at a distance")
    half_built: StringProperty(name="Half Built", description="Building only: mesh for the half built stage, "
                                                              "registered as <name>_halfbuilt")
    half_built_low: StringProperty(name="Half Built Low Detail")
    start: StringProperty(name="Start", description="Building only: mesh for the construction site, registered "
                                                    "as <name>_start")
    start_low: StringProperty(name="Start Low Detail")
    ground: EnumProperty(name="Terrain", items=DECORATION_GROUNDS, description="Ground the game scatters it on")
    count: IntProperty(name="Count", default=20, min=1, max=1000, description="How many the game scatters over a map")
    event: event_property()

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context))

    def invoke(self, context, event):
        active = context.active_object
        if active is not None and attachment_obj_poll(self, active):
            self.mesh = active.name
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "mesh", icon="MESH_DATA")
        layout.prop(self, "sprite_name")
        layout.prop(self, "scatter")
        if self.scatter:
            layout.prop(self, "ground")
            layout.prop(self, "count")
            draw_event(layout, self)
        else:
            obj = bpy.data.objects.get(self.mesh)
            arm = body_rig(obj) if obj is not None else None
            group, base, _ = rig_registry(context, arm)
            if base is not None:
                layout.label(text=f"Unit on the {group} / {base} rig", icon="ARMATURE_DATA")
            else:
                layout.prop(self, "group")
                layout.label(text="Unit with its own new rig" if arm is not None else "Static model",
                             icon="ARMATURE_DATA" if arm is not None else "MESH_CUBE")
            layout.prop_search(self, "low_detail", bpy.data, "objects")
            if arm is None:
                box = layout.box()
                box.label(text="Building stages (optional)")
                for prop in ("half_built", "half_built_low", "start", "start_low"):
                    box.prop_search(self, prop, bpy.data, "objects")
        problem = mesh_problem(self.mesh)
        draw_confirm(layout, self, name_problem(self.sprite_name) if problem is None else problem)

    def execute(self, context):
        root = repo_root(context)
        obj = bpy.data.objects.get(self.mesh)
        if obj is None or not attachment_obj_poll(self, obj):
            self.report({"ERROR"}, "Pick one of your own meshes")
            return {"CANCELLED"}
        name = self.sprite_name.strip()
        if not re.fullmatch(r"[A-Za-z0-9_]+", name):
            self.report({"ERROR"}, "Give the model a name made of letters, digits and underscores")
            return {"CANCELLED"}
        if self.scatter:
            arm, group, base = None, SCENERY_GROUP, None
        else:
            arm = body_rig(obj)
            group, base, _ = rig_registry(context, arm)
            if base is None:
                group = self.group

        def picked(prop):
            return bpy.data.objects.get(getattr(self, prop)) if getattr(self, prop) and not self.scatter else None

        stages = [("", obj, picked("low_detail"))]
        if arm is None:
            stages += [(suffix, picked(hi), picked(lo)) for suffix, hi, lo in
                       (("_halfbuilt", "half_built", "half_built_low"), ("_start", "start", "start_low"))
                       if picked(hi) is not None]
        meshes = [m for _, hi, lo in stages for m in (hi, lo) if m is not None]
        if any(m.type != "MESH" or m.get(REFERENCE_TAG) for m in meshes) or len(set(meshes)) != len(meshes):
            self.report({"ERROR"}, "Every stage and low detail pick must be a different mesh object")
            return {"CANCELLED"}
        taken = {s["name"] for s in read_registry(root) if s["group"] == group}
        clash = [name + suffix for suffix, _, _ in stages if name + suffix in taken]
        if clash:
            self.report({"ERROR"}, f"{group} already has a sprite named {', '.join(clash)}")
            return {"CANCELLED"}
        findings = [(level, f"{m.name}: {text}") for m in meshes for level, text in check_mesh(m, False, 0)]
        errors = store_findings(context, findings)
        if errors:
            self.report({"ERROR"}, f"Not registered: {errors} problem(s): " +
                        "; ".join(text for level, text in findings if level == "ERROR"))
            return {"CANCELLED"}

        geometry = os.path.join(root, GEOMETRY_DIR)
        folder = os.path.join(geometry, group, name)
        os.makedirs(folder, exist_ok=True)
        relative = lambda path: os.path.relpath(path, geometry).replace(os.sep, "/")
        skeleton, clips, missing, stage_models = None, [], [], []

        previous = arm.data.pose_position if arm is not None else None
        if arm is not None:
            arm.data.pose_position = "REST"
            context.view_layer.update()
        try:
            if arm is not None and base is None:
                skeleton_path = os.path.join(folder, name + "_skeleton.xml")
                write_skeleton_xml(arm, skeleton_path)
                skeleton = relative(skeleton_path)
            depsgraph = context.evaluated_depsgraph_get()
            for suffix, hi, lo in stages:
                models = []
                for mesh_obj, lod in ((hi, ""), (lo, "_lo")):
                    if mesh_obj is None:
                        continue
                    texture = mesh_obj.get("tt_texture", "").split(",")[0].strip() or material_image_name([mesh_obj])
                    if not ensure_texture_in_repo(root, mesh_obj, texture):
                        missing.append(texture)
                    path = os.path.join(folder, name + suffix + lod + ".xml")
                    write_mesh_xml([mesh_obj], [None], path, texture, False, depsgraph)
                    models.append((relative(path), [(texture, team_attribute(root, texture, False))]))
                stage_models.append((name + suffix, models))
        finally:
            if arm is not None:
                arm.data.pose_position = previous
                context.view_layer.update()

        if arm is not None and base is None:
            for action in armature_actions(arm):
                path = os.path.join(folder, action.name + ".xml")
                write_animation_xml(context, arm, action, path)
                clip = action.name[len(name) + 1:] if action.name.startswith(name + "_") else action.name
                kind = "plain" if any(word in clip for word in PLAIN_CLIPS) else "loop"
                clips.append((clip, kind, relative(path)))
            arm["tt_skeleton"] = os.path.join(folder, name + "_skeleton.xml")

        extra = [("base", base)] if base is not None else []
        if self.scatter:
            event = chosen_event(self)
            extra = [("decoration", self.ground), ("count", str(self.count))] + ([("event", event)] if event else [])
        entries = [(sprite, sprite_text([("name", sprite)] + extra, models, skeleton, clips))
                   for sprite, models in stage_models]
        append_registry_entries(os.path.join(root, REGISTRY_FILE), group, entries)
        refresh_units(context)
        note = f"; add {', '.join(m + '.png' for m in missing)} to assets/textures/models" if missing else ""
        sprites = ", ".join(sprite for sprite, _ in entries)
        kind = "decoration " if self.scatter else ""
        self.report({"WARNING"} if missing else {"INFO"},
                    f"Registered {kind}{group} / {sprites} under /geometry/{group}/ as .binsprite{note}")
        return {"FINISHED"}


class VIEW3D_PT_tt_units(bpy.types.Panel):
    bl_label = "Models"
    bl_order = 0
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    def draw(self, context):
        layout = self.layout
        wm = context.window_manager
        draw_update_button(layout, context)
        holder = root_holder(context)
        layout.prop(holder, "repo_root" if hasattr(holder, "repo_root") else "tt_repo_root", text="Repo")
        if not repo_root(context):
            layout.label(text="Pick your tribaltrouble folder", icon="INFO")
            return
        row = layout.row(align=True)
        row.operator(RefreshUnits.bl_idname, icon="FILE_REFRESH")
        row.prop(wm, "tt_category", text="")
        layout.template_list("TT_UL_units", "", wm, "tt_units", wm, "tt_unit_index", rows=10)
        if references():
            layout.operator(RemoveAdded.bl_idname, icon="X")
        layout.operator(LoadUnit.bl_idname, icon="FILE_REFRESH")
        row = layout.row(align=True)
        row.scale_y = 1.4
        row.operator(PublishModel.bl_idname, icon="EXPORT")
        if active_armature(context) is not None:
            row.operator(Preflight.bl_idname, text="", icon="CHECKMARK")
        body = loaded_body()
        if body is not None:
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH")
            remove.group, remove.sprite = body["tt_group"], body["tt_sprite"]
        draw_checks(layout, wm)
        # Units show it under Preview.
        if has_low_detail() and not any(o.type == "ARMATURE" and o.get(BROWSER_TAG) for o in bpy.data.objects):
            layout.row(align=True).prop(wm, "tt_detail", expand=True)
        layout.operator(RegisterModel.bl_idname, icon="ADD")


def publish_props(op, context, fresh, event, base=None):
    """Write the new props into the loaded model's folder, on base (the model showing unless given), and add them to
    geometry.xml. Props already listed are written back to their own files, and the model's own meshes when they
    changed."""
    body = prop_body(context)
    root = repo_root(context)
    group, base = body["tt_group"], base or body["tt_sprite"]
    existing = [o for o in building_props(body) if not o.hide_viewport and item_shown(o)]
    findings = [(level, f"{o.name}: {text}") for o in fresh + existing
                for level, text in check_mesh(o, o in fresh, 0)]
    taken = {s["name"] for s in read_registry(root) if s["group"] == group}
    findings += [("ERROR", f"{o.name}: {group} already has a sprite named {base}_{o.name}") for o in fresh
                 if f"{base}_{o.name}" in taken]
    folder = os.path.dirname(body["tt_source"])
    paths = {o: o.get("tt_source") or os.path.join(folder, o.name + ".xml") for o in fresh + existing}
    findings += [("ERROR", f"{name}: the model's folder already has a file with this name") for name in
                 file_clashes([(o, paths[o]) for o in fresh])]
    errors = store_findings(context, findings)
    if errors:
        op.report({"ERROR"}, f"Not published: {errors} problem(s): " +
                  "; ".join(text for level, text in findings if level == "ERROR"))
        return {"CANCELLED"}
    geometry = os.path.join(root, GEOMETRY_DIR)
    depsgraph = context.evaluated_depsgraph_get()
    entries, missing = [], []
    for o in fresh + existing:
        path = paths[o]
        texture = o.get("tt_texture", "").split(",")[0].strip() or material_image_name([o])
        write_mesh_xml([o], [None], path, texture, False, depsgraph, use_groups=False)
        if not ensure_texture_in_repo(root, o, texture):
            missing.append(texture)
        if o in existing:
            continue
        sprite = f"{base}_{o.name}"
        model = os.path.relpath(path, geometry).replace(os.sep, "/")
        entries.append((sprite, sprite_text([("name", sprite), ("base", base), ("slot", PROP_SLOT)] +
                                            ([("event", event)] if event else []),
                                            [(model, [(texture, team_attribute(root, texture, False))])])))
        world = o.matrix_world.copy()
        o.parent = body
        o.matrix_world = world
        o[BROWSER_TAG] = True
        o["tt_slot"], o["tt_sprite"], o["tt_source"], o["tt_event"] = PROP_SLOT, sprite, path, event
        o["tt_group"] = group
        o["tt_texture"] = texture
    append_registry_entries(os.path.join(root, REGISTRY_FILE), group, entries)
    for o, (_, digest) in export_texts(context, None, fresh).items():
        o["tt_export_hash"] = digest
    saved = write_changed(context, None, {o: o["tt_source"] for o in model_levels(body)})
    publish_skin_paint(root)
    note = f"; model mesh: {', '.join(os.path.basename(o['tt_source']) for o in saved)}" if saved else ""
    note += f"; no texture image for {', '.join(sorted(set(missing)))}" if missing else ""
    op.report({"WARNING"} if missing else {"INFO"},
              f"Published {len(fresh)} new and {len(existing)} existing prop(s) on {group} / {base}{note}")
    return {"FINISHED"}


_prop_bases = []


def prop_base_items(self, context):
    """The loaded model, and for a tree the other of its trunk and crown."""
    body = prop_body(context)
    name = body["tt_sprite"] if body is not None else ""
    other = re.sub(r"_(trunk|crown)$", lambda m: "_crown" if m.group(1) == "trunk" else "_trunk", name)
    root = repo_root(context)
    names = [name] + ([other] if other != name and root and any(
        s["group"] == body["tt_group"] and s["name"] == other for s in read_registry(root)) else [])
    _prop_bases[:] = [(n, n, "") for n in names]
    return _prop_bases


class NewProp(bpy.types.Operator):
    """Publish one of your meshes as a prop of the loaded model (the building stage showing): it is written into the
    model's folder, in place around it as you arranged it, and added to geometry.xml"""
    bl_idname = "object.tt_new_prop"
    bl_label = "New Prop..."
    mesh: StringProperty(name="Mesh", search=own_mesh_search, options={"SKIP_SAVE"},
                         description="One of your own meshes in this scene")
    base: EnumProperty(name="Sits on", items=prop_base_items, options={"SKIP_SAVE"},
                       description="The sprite the prop is drawn with")
    event: event_property()
    make_texture: BoolProperty(name="Make a texture for it", default=True, options={"SKIP_SAVE"},
                               description="Give the mesh a UV map and an image named after it to paint")

    @classmethod
    def poll(cls, context):
        return prop_body(context) is not None and bool(repo_root(context))

    def invoke(self, context, event):
        picked = next((o for o in context.selected_objects if attachment_obj_poll(self, o)), None)
        if picked is not None:
            self.mesh = picked.name
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "mesh", icon="MESH_DATA")
        body = prop_body(context)
        if body.get("tt_category") == "BUILDINGS":
            building, stage = building_stage(body["tt_sprite"])
            layout.label(text=f"Attaches to: {building} ({stage})")
        elif len(prop_base_items(self, context)) > 1:
            layout.prop(self, "base")
        draw_event(layout, self)
        obj = bpy.data.objects.get(self.mesh)
        if obj is not None and obj.type == "MESH" and mesh_texture_image(obj) is None:
            layout.prop(self, "make_texture")
        draw_confirm(layout, self, mesh_problem(self.mesh))

    def execute(self, context):
        obj = bpy.data.objects.get(self.mesh)
        if obj is None or not attachment_obj_poll(self, obj):
            self.report({"ERROR"}, "Pick one of your own meshes")
            return {"CANCELLED"}
        event = chosen_event(self)
        if self.make_texture and mesh_texture_image(obj) is None:
            bpy.ops.object.tt_make_texture(target=obj.name)
        return publish_props(self, context, [obj], event, self.base)


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


classes = (TTPreferences, ImportTTMesh, ExportTTMesh, SplitByBone, ImportTTSkeleton, ExportTTSkeleton,
           TTAttachmentSlot, TTUnitEntry, TT_UL_units, RefreshUnits, LoadUnit, PublishModel, PickUnit, AddToScene, RemoveAdded, ShowItem, ShowItemClip, ExportToRepo, AddToRegistry, RegisterModel, TTCheck, SetClip, SetTier, MaterialPreview, Preflight,
           RemoveFromRegistry, UpdateAddon, NewEvent, NewProp, ShowSkin, PaintSkin, TTSkinEntry, TT_UL_skins, PickSkin, NewSkin, CancelSkin, SaveSkin, CloseItem, NewClip, SaveClip, DeleteClip,
           SetupAttachments, ExportAttachments, CopyRegistrySnippet, SaveItems, MakeTexture, NewItem, OwnTexture, PutOnBone, PaintItem, DonePainting,
           TT_UL_items, VIEW3D_PT_tt_units, VIEW3D_PT_tt_skins,
           VIEW3D_PT_tt_preview,
           VIEW3D_PT_tt_attachments, VIEW3D_PT_tt_attachments_more)


def register():
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.Object.tt_attachments = CollectionProperty(type=TTAttachmentSlot)
    wm = bpy.types.WindowManager
    wm.tt_repo_root = StringProperty(name="Repo Folder", subtype="DIR_PATH", update=root_update)
    wm.tt_units = CollectionProperty(type=TTUnitEntry)
    wm.tt_unit_index = IntProperty(update=unit_index_update)
    wm.tt_category = EnumProperty(name="Category", items=CATEGORY_ITEMS, default="UNITS", update=root_update,
                                  description="Which kind of model the list shows")
    wm.tt_checks = CollectionProperty(type=TTCheck)
    wm.tt_detail = EnumProperty(name="Detail", items=DETAIL_ITEMS, update=detail_update,
                                description="Which of the model's meshes shows: the close up one or the one the game "
                                            "draws from far away")
    wm.tt_checked = BoolProperty()
    wm.tt_team_preview = BoolProperty(name="Team Color", default=False, update=team_preview_update,
                                      description="Blend the player's color in through the team decal, as in game")
    wm.tt_team_color = FloatVectorProperty(name="Team Color", subtype="COLOR", size=3, min=0.0, max=1.0,
                                           default=(0.8, 0.1, 0.1), update=team_preview_update)
    wm.tt_item_index = IntProperty(update=item_index_update)
    wm.tt_open_item = StringProperty()
    wm.tt_skins = CollectionProperty(type=TTSkinEntry)
    wm.tt_skin_index = IntProperty(get=lambda wm: shown_skin_index(wm.tt_skins, loaded_body()),
                                   set=lambda wm, index: pick_skin_row(wm.tt_skins, index))
    wm.tt_item_skins = CollectionProperty(type=TTSkinEntry)
    wm.tt_item_skin_index = IntProperty(
        get=lambda wm: shown_skin_index(wm.tt_item_skins, bpy.data.objects.get(wm.tt_open_item)),
        set=lambda wm, index: pick_skin_row(wm.tt_item_skins, index))
    wm.tt_item_search = StringProperty(name="Search", options={"TEXTEDIT_UPDATE"},
                                       description="Show only the items whose name contains this")
    bpy.app.handlers.load_post.append(refresh_units_on_load)
    bpy.app.timers.register(refresh_units_on_load, first_interval=0.5)
    bpy.types.TOPBAR_MT_file_import.append(menu_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_export)
    bpy.types.VIEW3D_MT_object.append(menu_object)
    if hasattr(bpy.types, "UI_MT_button_context_menu"):
        bpy.types.UI_MT_button_context_menu.append(clip_button_menu)
        bpy.types.UI_MT_button_context_menu.append(units_list_menu)


def unregister():
    if hasattr(bpy.types, "UI_MT_button_context_menu"):
        bpy.types.UI_MT_button_context_menu.remove(units_list_menu)
        bpy.types.UI_MT_button_context_menu.remove(clip_button_menu)
    bpy.types.VIEW3D_MT_object.remove(menu_object)
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_export)
    bpy.app.handlers.load_post.remove(refresh_units_on_load)
    del bpy.types.Object.tt_attachments
    for name in ("tt_repo_root", "tt_units", "tt_unit_index", "tt_category", "tt_checks",
                 "tt_checked", "tt_team_preview", "tt_team_color",
                 "tt_item_index", "tt_detail", "tt_item_search",
                 "tt_open_item", "tt_skins", "tt_skin_index", "tt_item_skins", "tt_item_skin_index"):
        delattr(bpy.types.WindowManager, name)
    for cls in classes:
        bpy.utils.unregister_class(cls)

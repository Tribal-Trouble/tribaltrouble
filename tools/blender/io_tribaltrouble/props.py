"""Props panel: the item and prop lists, an open item's view, New Prop, New Item and painting."""

import os
import re

import bpy
import numpy as np
from bpy.props import StringProperty, BoolProperty, EnumProperty

from .textures import (apply_team_preview, crop_pixels, decal_texture_path, emission_image, ensure_emission_in_repo,
                       ensure_texture_in_repo, get_atlas_material, material_image_name, mesh_texture_image, MIP_PAD,
                       models_texture_path, race_texture_name, short_labels, texture_names)
from .mesh_io import write_mesh_xml
from .rig import active_armature, item_hidden_here, POINT_LABELS
from .registry import (append_registry_entries, CARRY_SLOT, GEOMETRY_DIR, PROP_SLOT, read_registry, REGISTRY_FILE,
                       repo_root, sprite_text, team_attribute, emissive_attribute)
from .scene import (attachment_obj_poll, browsed_unit, BROWSER_TAG, export_texts, file_clashes, item_shown,
                    model_levels, refresh_skins, set_item_visible, skin_item, snap_to_bone, unit_items, write_changed)
from .publish import check_mesh, publish_paint, store_findings, texture_clashes
from .forms import (chosen_event, draw_confirm, draw_event, event_property, form_title, mesh_problem, open_form,
                    own_mesh_search)
from .models import RemoveFromRegistry
from .preview import ShowItemClip
from .by_hand import SetupAttachments
from .skins import draw_skin_banner, draw_skin_list, NewSkin
from .glow import draw_glow


PROP_CATEGORIES = ("BUILDINGS", "RESOURCES", "NATURE", "DECORATIONS", "OTHER")  # take props, besides units


def prop_body(context):
    """The loaded model props hang on, when it is not a unit."""
    return next((o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.get("tt_category") in PROP_CATEGORIES
                 and o.type == "MESH" and o.parent is None and not o.get("tt_detail")), None)


def building_props(body):
    return sorted((o for o in bpy.data.objects if o.parent == body and o.get("tt_slot") and not o.get("tt_detail")),
                  key=lambda o: o.name)


def building_stage(sprite):
    """(building name, stage label) of a building stage sprite."""
    for suffix, stage in (("_halfbuilt", "Half built"), ("_start", "Start")):
        if sprite.endswith(suffix):
            return sprite[:-len(suffix)], stage
    return sprite, "Built"


def prop_tag(obj):
    event = obj.get("tt_event") or "All year"
    if obj.parent.get("tt_category") != "BUILDINGS":
        return event
    return f"{building_stage(obj.parent['tt_sprite'])[1]}, {event}"


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
    draw_glow(box, context, item)
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
    glow: BoolProperty(options={"SKIP_SAVE"}, description="Paint the glow picture instead of the texture")

    def execute(self, context):
        obj = bpy.data.objects.get(self.target) or context.active_object
        mesh = obj is not None and obj.type == "MESH"
        image = (emission_image(obj) if self.glow else mesh_texture_image(obj)) if mesh else None
        if image is None:
            self.report({"ERROR"}, "This mesh has no glow yet: press Add Glow" if self.glow else
                        "This mesh has no texture yet: press Make A Texture For It")
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
        elif mat.users > 1:
            mat = obj.active_material = mat.copy()  # the other meshes on it keep their own look
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
        taken = [n for n in fresh if n in bpy.data.images or n + "_team" in bpy.data.images
                 or os.path.isfile(models_texture_path(root, n)) or os.path.isfile(decal_texture_path(root, n))]
        if taken:
            self.report({"ERROR"}, f"A texture named {', '.join(taken)} already exists; nothing was changed, rename "
                                   f"the item first")
            return {"CANCELLED"}
        if any(not os.path.isfile(models_texture_path(root, t)) for t in textures):
            self.report({"ERROR"}, f"{', '.join(textures)} must all be in assets/textures/models")
            return {"CANCELLED"}
        atlases = [bpy.data.images.load(models_texture_path(root, t), check_existing=True) for t in textures]
        decals = [bpy.data.images.load(decal_texture_path(root, t), check_existing=True)
                  if os.path.isfile(decal_texture_path(root, t)) else None for t in textures]
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


def publish_props(op, context, fresh, event, base=None):
    """Write the new props into the loaded model's folder, on base (the model showing unless given), and add them to
    geometry.xml. Props already listed, and the model's own meshes, are written back to their own files when they
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
    findings += texture_clashes(root, fresh)
    errors = store_findings(context, findings)
    if errors:
        op.report({"ERROR"}, f"Not published: {errors} problem(s): " +
                  "; ".join(text for level, text in findings if level == "ERROR"))
        return {"CANCELLED"}
    geometry = os.path.join(root, GEOMETRY_DIR)
    depsgraph = context.evaluated_depsgraph_get()
    entries, missing = [], []
    for o in fresh:
        path = paths[o]
        texture = o.get("tt_texture", "").split(",")[0].strip() or material_image_name([o])
        write_mesh_xml([o], [None], path, texture, False, depsgraph, use_groups=False)
        if not ensure_texture_in_repo(root, o, texture):
            missing.append(texture)
        ensure_emission_in_repo(root, o)
        sprite = f"{base}_{o.name}"
        model = os.path.relpath(path, geometry).replace(os.sep, "/")
        entries.append((sprite, sprite_text([("name", sprite), ("base", base), ("slot", PROP_SLOT)] +
                                            ([("event", event)] if event else []),
                                            [(model, [(texture, team_attribute(root, texture, False) +
                                                                emissive_attribute(o))])])))
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
    changed = write_changed(context, None, {o: o["tt_source"] for o in existing})
    publish_paint(root)
    note = f"; model mesh: {', '.join(os.path.basename(o['tt_source']) for o in saved)}" if saved else ""
    note += f"; no texture image for {', '.join(sorted(set(missing)))}" if missing else ""
    op.report({"WARNING"} if missing else {"INFO"},
              f"Published {len(fresh)} new and {len(changed)} changed prop(s) on {group} / {base}{note}")
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

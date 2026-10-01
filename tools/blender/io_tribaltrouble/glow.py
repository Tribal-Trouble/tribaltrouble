"""The Glow row: the picture wired into a mesh's Emission Color, which the game adds on top of the lit texture."""

import bpy
from bpy.props import StringProperty, EnumProperty

from .textures import emission_image, image_texture_name, mesh_texture_image, remove_emission, wire_emission
from .registry import read_registry, repo_root
from .forms import draw_confirm, form_title, open_form

PAINT_IT = "object.tt_paint_item"  # props.PaintItem, which imports this module

NEW_GLOW = "__NEW__"
_glow_items = []


def glow_image_items(self, context):
    _glow_items[:] = [(NEW_GLOW, "New black image", "The size of the texture; paint where it glows")] + [
        (image.name, image.name, "") for image in bpy.data.images if image.type == "IMAGE"]
    return _glow_items


def glow_users(context, glow, sprite):
    """Other registry sprites that glow with the same picture: painting it changes them too."""
    root = repo_root(context)
    return [s["name"] for s in read_registry(root) if glow in s.get("emissive", []) and s["name"] != sprite] \
        if root else []


def draw_glow(layout, context, obj):
    image = emission_image(obj)
    split = layout.split(factor=0.3)
    split.label(text="Glow", icon="LIGHT_SUN")
    row = split.row(align=True)
    if image is None:
        row.operator(AddGlow.bl_idname, icon="ADD").target = obj.name
        return
    name = image_texture_name(image)
    row.label(text=name)
    paint = row.operator(PAINT_IT, text="", icon="BRUSH_DATA", emboss=False)
    paint.target, paint.glow = obj.name, True
    row.operator(AddGlow.bl_idname, text="", icon="FILE_REFRESH", emboss=False).target = obj.name
    row.operator(RemoveGlow.bl_idname, text="", icon="TRASH", emboss=False).target = obj.name
    others = glow_users(context, name, obj.get("tt_sprite"))
    if others:
        note = layout.row()
        note.enabled = False
        note.label(text="Also used by " + ", ".join(others[:3]) + (" ..." if len(others) > 3 else ""), icon="INFO")


class AddGlow(bpy.types.Operator):
    """Make this mesh glow in game with a picture added on top of its lit texture, black where it does not glow"""
    bl_idname = "object.tt_add_glow"
    bl_label = "Add Glow..."
    target: StringProperty(options={"SKIP_SAVE", "HIDDEN"})
    image: EnumProperty(name="Image", items=glow_image_items)

    def invoke(self, context, event):
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "image")
        draw_confirm(layout, self)

    def execute(self, context):
        obj = bpy.data.objects.get(self.target)
        if obj is None or obj.type != "MESH":
            return {"CANCELLED"}
        if self.image == NEW_GLOW:
            base = mesh_texture_image(obj)
            width, height = base.size if base is not None and base.size[0] else (512, 512)
            name = f"{image_texture_name(base) if base is not None else obj.name}_glow"
            image = bpy.data.images.new(name, width, height, alpha=True)
            image.generated_color = (0.0, 0.0, 0.0, 1.0)
        else:
            image = bpy.data.images.get(self.image)
        if image is None:
            return {"CANCELLED"}
        wire_emission(obj, image)
        return {"FINISHED"}


class RemoveGlow(bpy.types.Operator):
    """Stop this mesh glowing; the picture itself stays"""
    bl_idname = "object.tt_remove_glow"
    bl_label = "Remove Glow"
    target: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        obj = bpy.data.objects.get(self.target)
        if obj is None:
            return {"CANCELLED"}
        remove_emission(obj)
        return {"FINISHED"}

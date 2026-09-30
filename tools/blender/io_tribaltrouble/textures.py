"""Materials, team color nodes and texture files."""

import os
import shutil
import xml.etree.ElementTree as ET

import bpy
import numpy as np


def find_up(start_path, relative):
    d = os.path.dirname(os.path.abspath(start_path))
    for _ in range(8):
        candidate = os.path.join(d, relative)
        if os.path.isfile(candidate):
            return candidate
        parent = os.path.dirname(d)
        if parent == d:
            break
        d = parent
    return None


def find_registry_texture(mesh_path):
    """Buildings carry no texture attribute; their atlas is assigned in the geometry.xml registry."""
    registry = find_up(mesh_path, "geometry.xml")
    if registry is None:
        return None
    try:
        root = ET.parse(registry).getroot()
    except ET.ParseError:
        return None
    mesh_norm = os.path.abspath(mesh_path).replace("\\", "/").lower()
    for model in root.iter("model"):
        text = (model.text or "").strip().replace("\\", "/").lower()
        if text and mesh_norm.endswith(text):
            tex = model.find("texture")
            if tex is not None:
                return tex.get("name")
    return None


def find_texture_image(mesh_path, texture):
    """Walk up from the mesh file looking for textures/models/<texture>.png (repo layout: assets/)."""
    if not texture:
        return None
    return find_up(mesh_path, os.path.join("textures", "models", texture + ".png"))


def get_atlas_material(texture, image_path, image=None, decal=None):
    mat_name = "tt_" + texture
    mat = bpy.data.materials.get(mat_name)
    if mat is not None:
        return mat
    mat = bpy.data.materials.new(mat_name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    tex_node = mat.node_tree.nodes.new("ShaderNodeTexImage")
    tex_node.image = image or bpy.data.images.load(image_path, check_existing=True)
    tex_node.location = (-350, 300)
    if bsdf is not None:
        mat.node_tree.links.new(bsdf.inputs["Base Color"], tex_node.outputs["Color"])
        mat.node_tree.links.new(bsdf.inputs["Alpha"], tex_node.outputs["Alpha"])
        bsdf.inputs["Roughness"].default_value = 1.0
    try:
        mat.blend_method = "CLIP"
    except AttributeError:
        pass
    add_team_nodes(mat, tex_node, image_path, decal)
    return mat


TEAM_MIX, TEAM_COLOR, TEAM_DECAL = "TT Team Mix", "TT Team Color", "TT Team Decal"
MIX_FACTOR, MIX_A, MIX_B, MIX_RESULT = 0, 6, 7, 2  # ShaderNodeMix sockets for the RGBA data type


def add_team_nodes(mat, tex_node, image_path, image=None):
    """The game blends base toward the player's color by the decal: mix(base, team, decal). Left unlinked
    until the preview is switched on."""
    models_dir, file_name = os.path.split(image_path)
    decal_path = os.path.join(os.path.dirname(models_dir), "teamdecals", os.path.splitext(file_name)[0] + "_team.png")
    if image is None and not os.path.isfile(decal_path):
        return
    image = image or bpy.data.images.load(decal_path, check_existing=True)
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    decal = nodes.new("ShaderNodeTexImage")
    decal.name = TEAM_DECAL
    decal.image = image
    if decal.image.colorspace_settings.name != "Non-Color":  # setting it again would blank an image made in Blender
        decal.image.colorspace_settings.name = "Non-Color"
    decal.location = (-350, 0)
    color = nodes.new("ShaderNodeRGB")
    color.name = TEAM_COLOR
    color.location = (-350, -300)
    mix = nodes.new("ShaderNodeMix")
    mix.name = TEAM_MIX
    mix.data_type = "RGBA"
    mix.location = (-100, 150)
    links.new(mix.inputs[MIX_FACTOR], decal.outputs["Color"])
    links.new(mix.inputs[MIX_A], tex_node.outputs["Color"])
    links.new(mix.inputs[MIX_B], color.outputs["Color"])


def image_texture_name(image):
    """The file's name when the image has one, since a copy of hat.png is named hat.png.001 in Blender."""
    return os.path.splitext(bpy.path.basename(image.filepath) if image.filepath else image.name)[0]


def material_image_name(objs):
    """Fall back to the name of an image used by the objects' materials, without extension."""
    for o in objs:
        for slot in o.material_slots:
            mat = slot.material
            if mat is None or not mat.use_nodes:
                continue
            for node in mat.node_tree.nodes:
                if node.type == "TEX_IMAGE" and node.image is not None:
                    return image_texture_name(node.image)
    return ""


def object_texture(o):
    return o.get("tt_texture") or material_image_name([o])


def texture_names(o):
    """The object's texture list, one per tier."""
    return [t.strip() for t in object_texture(o).split(",") if t.strip()]


MIP_PAD = 4  # pixels kept around a cropped item so mipmaps do not bleed in the rest of the atlas


def crop_pixels(image, x0, y0, x1, y1, size):
    """image[y0:y1, x0:x1] on a size x size canvas at the same density, the rest filled by repeating its edges."""
    w, h = image.size
    pixels = np.empty(w * h * 4, np.float32)
    image.pixels.foreach_get(pixels)
    part = pixels.reshape(h, w, 4)[y0:y1, x0:x1]
    return np.pad(part, ((0, size - part.shape[0]), (0, size - part.shape[1]), (0, 0)), mode="edge")


TEXTURE_PREFIXES = {"vikings": "viking", "natives": "native"}


def race_texture_name(group, name):
    """Texture names are shared by every group, so they carry the race the way viking_peon and native_warrior do."""
    prefix = TEXTURE_PREFIXES.get(group, group)
    return f"{prefix}_{name}" if prefix and not name.startswith(prefix + "_") else name


def apply_team_preview(context):
    """Route every atlas material through its team mix, or straight from the texture, per the preview toggle."""
    wm = context.window_manager
    for mat in bpy.data.materials:
        nodes = mat.node_tree.nodes if mat.use_nodes and mat.node_tree is not None else None
        if nodes is None or TEAM_MIX not in nodes:
            continue
        nodes[TEAM_COLOR].outputs[0].default_value = (*wm.tt_team_color, 1.0)
        bsdf = nodes.get("Principled BSDF")
        base = next((n for n in nodes if n.type == "TEX_IMAGE" and n.name != TEAM_DECAL), None)
        if bsdf is None or base is None:
            continue
        source = nodes[TEAM_MIX].outputs[MIX_RESULT] if wm.tt_team_preview else base.outputs["Color"]
        mat.node_tree.links.new(bsdf.inputs["Base Color"], source)


def team_preview_update(self, context):
    apply_team_preview(context)


def short_labels(names):
    """Names with their shared prefix removed: viking_warrior_rock, viking_warrior_iron -> rock, iron."""
    if len(names) < 2:
        return list(names)
    prefix = os.path.commonprefix(list(names))
    prefix = prefix[:prefix.rfind("_") + 1]
    return [n[len(prefix):] or n for n in names]


def mesh_texture_image(obj):
    for slot in obj.material_slots:
        nodes = slot.material.node_tree.nodes if slot.material is not None and slot.material.use_nodes else []
        for node in nodes:
            if node.type == "TEX_IMAGE" and node.image is not None and node.name != TEAM_DECAL:
                return node.image
    return None


def emission_image(obj):
    """The image wired into a material's Emission Color: the game adds it on top of the lit texture."""
    for slot in obj.material_slots:
        nodes = slot.material.node_tree.nodes if slot.material is not None and slot.material.use_nodes else []
        for node in nodes:
            socket = node.inputs.get("Emission Color") if node.type == "BSDF_PRINCIPLED" else None
            source = socket.links[0].from_node if socket is not None and socket.is_linked else None
            if source is not None and source.type == "TEX_IMAGE" and source.image is not None:
                return source.image
    return None


def ensure_emission_in_repo(root, obj):
    image = emission_image(obj)
    return image is None or ensure_texture_in_repo(root, obj, image_texture_name(image))


def show_emission(obj, image_path):
    """Glow obj in Blender the way the game will, on a copy of its material so others using the texture stay plain."""
    mat = obj.active_material
    if mat is None or not mat.use_nodes or not os.path.isfile(image_path):
        return
    name = f"{mat.name}+{os.path.splitext(os.path.basename(image_path))[0]}"
    glow = bpy.data.materials.get(name)
    if glow is None:
        glow = mat.copy()
        glow.name = name
        bsdf = glow.node_tree.nodes.get("Principled BSDF")
        if bsdf is not None:
            node = glow.node_tree.nodes.new("ShaderNodeTexImage")
            node.image = bpy.data.images.load(image_path, check_existing=True)
            node.location = (-350, -300)
            glow.node_tree.links.new(bsdf.inputs["Emission Color"], node.outputs["Color"])
            bsdf.inputs["Emission Strength"].default_value = 1.0
    obj.active_material = glow


def ensure_texture_in_repo(root, obj, texture, folder="models"):
    """Put the material's image, or the image of that name, at assets/textures/<folder>/<texture>.png: written when
    the repo lacks it or the image has unsaved paint, so a texture made inside Blender travels with the mesh. False
    when there is no image."""
    target = os.path.join(root, "assets", "textures", folder, texture + ".png")
    images = [node.image for slot in obj.material_slots if slot.material is not None and slot.material.use_nodes
              for node in slot.material.node_tree.nodes if node.type == "TEX_IMAGE"]
    for image in images + [bpy.data.images.get(texture)]:
        if image is None or image_texture_name(image) != texture:
            continue
        if os.path.isfile(target) and not image.is_dirty:
            return True
        source = bpy.path.abspath(image.filepath)
        if not image.is_dirty and os.path.isfile(source) and source.lower().endswith(".png"):
            if os.path.normcase(os.path.abspath(source)) != os.path.normcase(os.path.abspath(target)):
                shutil.copyfile(source, target)
                image["tt_repo_texture"] = texture
            return True
        save_png(image, target)
        image["tt_repo_texture"] = texture
        return True
    return os.path.isfile(target)


def save_png(image, target):
    previous = (image.filepath_raw, image.file_format)
    image.filepath_raw, image.file_format = target, "PNG"
    image.save()
    if not previous[0]:
        # Made in Blender, so packed: it must not point into the repo and has no other file.
        if image.packed_file is not None:
            image.unpack(method="REMOVE")
        image.pack()
    image.filepath_raw, image.file_format = previous


def models_texture_path(root, texture):
    return os.path.join(root, "assets", "textures", "models", texture + ".png")


def decal_texture_path(root, texture):
    return os.path.join(root, "assets", "textures", "teamdecals", texture + "_team.png")

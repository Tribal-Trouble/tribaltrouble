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

import hashlib
import os
import re
import shutil
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape, quoteattr

import bpy
import numpy as np
from bpy_extras.io_utils import ImportHelper, ExportHelper
from bpy.props import (StringProperty, BoolProperty, CollectionProperty, EnumProperty, PointerProperty, FloatProperty,
                       IntProperty, FloatVectorProperty)
from mathutils import Matrix, Vector

bl_info = {
    "name": "Tribal Trouble Mesh (.xml)",
    "author": "Tribal Trouble tooling",
    "version": (1, 27, 1),
    "blender": (4, 1, 0),
    "location": "File > Import-Export",
    "description": "Import/export Tribal Trouble geometry XML meshes",
    "category": "Import-Export",
}

DOCTYPE = """<!DOCTYPE mesh [
        <!ELEMENT mesh       (polygons, skeleton?)>
        <!ELEMENT polygons   (polygon+)>
        <!ELEMENT polygon    (vertex, vertex, vertex)>
        <!ELEMENT vertex     (skin+)>
        <!ELEMENT skin        EMPTY>
        <!ELEMENT skeleton   (bones, init_pose)>
        <!ELEMENT bones      (bone+)>
        <!ELEMENT init_pose  (transform+)>
        <!ELEMENT bone        EMPTY>
        <!ELEMENT transform   EMPTY>
        <!ATTLIST mesh texture CDATA #IMPLIED>
        <!ATTLIST vertex x CDATA #REQUIRED>
        <!ATTLIST vertex y CDATA #REQUIRED>
        <!ATTLIST vertex z CDATA #REQUIRED>
        <!ATTLIST vertex r CDATA #REQUIRED>
        <!ATTLIST vertex g CDATA #REQUIRED>
        <!ATTLIST vertex b CDATA #REQUIRED>
        <!ATTLIST vertex a CDATA #REQUIRED>
        <!ATTLIST vertex nx CDATA #REQUIRED>
        <!ATTLIST vertex ny CDATA #REQUIRED>
        <!ATTLIST vertex nz CDATA #REQUIRED>
        <!ATTLIST vertex u CDATA #REQUIRED>
        <!ATTLIST vertex v CDATA #REQUIRED>
        <!ATTLIST vertex u2 CDATA #IMPLIED>
        <!ATTLIST vertex v2 CDATA #IMPLIED>
        <!ATTLIST skin bone CDATA #REQUIRED>
        <!ATTLIST skin weight CDATA #REQUIRED>
        <!ATTLIST transform name CDATA #REQUIRED>
        <!ATTLIST transform m00 CDATA #REQUIRED>
        <!ATTLIST transform m01 CDATA #REQUIRED>
        <!ATTLIST transform m02 CDATA #REQUIRED>
        <!ATTLIST transform m03 CDATA #REQUIRED>
        <!ATTLIST transform m10 CDATA #REQUIRED>
        <!ATTLIST transform m11 CDATA #REQUIRED>
        <!ATTLIST transform m12 CDATA #REQUIRED>
        <!ATTLIST transform m13 CDATA #REQUIRED>
        <!ATTLIST transform m20 CDATA #REQUIRED>
        <!ATTLIST transform m21 CDATA #REQUIRED>
        <!ATTLIST transform m22 CDATA #REQUIRED>
        <!ATTLIST transform m23 CDATA #REQUIRED>
        <!ATTLIST transform m30 CDATA #REQUIRED>
        <!ATTLIST transform m31 CDATA #REQUIRED>
        <!ATTLIST transform m32 CDATA #REQUIRED>
        <!ATTLIST transform m33 CDATA #REQUIRED>
        ]>"""

STATIC_BONE = "dummy_bone"

SKELETONS = (
    ("PEON", "Peon (both races)", "vikings/peon and natives/peon skeletons share bone names"),
    ("VIKING_WARRIOR", "Viking warrior", ""),
    ("NATIVE_WARRIOR", "Native warrior", ""),
    ("VIKING_CHIEFTAIN", "Viking chieftain", ""),
    ("NATIVE_CHIEFTAIN", "Native chieftain", ""),
)

# Bone names must match the skeleton file exactly (note the double space in "warrior  Head").
# Game slot an attachment point feeds; H in game cycles the "hat" slot. Other points use their own name.
GAME_SLOTS = {"HEAD": "hat", "PROP1": "weapon"}

ATTACHMENT_POINTS = {
    "HEAD": {"PEON": "peon Head", "VIKING_WARRIOR": "warrior  Head", "NATIVE_WARRIOR": "Head",
             "VIKING_CHIEFTAIN": "Head", "NATIVE_CHIEFTAIN": "Head"},
    "BACK": {"PEON": "peon Spine2", "VIKING_WARRIOR": "warrior  Spine1", "NATIVE_WARRIOR": "Spine2",
             "VIKING_CHIEFTAIN": "Spine1", "NATIVE_CHIEFTAIN": "Spine2"},
    "HAND_R": {"PEON": "peon R Hand", "VIKING_WARRIOR": "warrior  R Hand", "NATIVE_WARRIOR": "R Hand",
               "VIKING_CHIEFTAIN": "R Hand", "NATIVE_CHIEFTAIN": "R Hand"},
    "HAND_L": {"PEON": "peon L Hand", "VIKING_WARRIOR": "warrior  L Hand", "NATIVE_WARRIOR": "L Hand",
               "VIKING_CHIEFTAIN": "L Hand", "NATIVE_CHIEFTAIN": "L Hand"},
    "PROP1": {"PEON": "peon Prop1", "VIKING_WARRIOR": "warrior  Prop1", "NATIVE_WARRIOR": "Prop1",
              "VIKING_CHIEFTAIN": "Prop1", "NATIVE_CHIEFTAIN": "Prop1"},
    "PROP2": {"VIKING_CHIEFTAIN": "Prop2", "NATIVE_CHIEFTAIN": "Prop2"},
    "PROP3": {"NATIVE_WARRIOR": "Prop3", "VIKING_CHIEFTAIN": "Prop3", "NATIVE_CHIEFTAIN": "Prop3"},
    "BELT": {"PEON": "peon Pelvis", "VIKING_WARRIOR": "warrior  Pelvis", "NATIVE_WARRIOR": "Pelvis",
             "VIKING_CHIEFTAIN": "Pelvis", "NATIVE_CHIEFTAIN": "Pelvis"},
}

ATTACHMENT_POINT_ITEMS = (
    ("NONE", "Static (dummy_bone)", "Static prop: every vertex skinned to dummy_bone"),
    ("HEAD", "Head", "Hats, masks, horns"),
    ("BACK", "Back", "Packs, capes, carried bundles"),
    ("HAND_R", "Right hand", "Held items"),
    ("HAND_L", "Left hand", "Shields, torches"),
    ("PROP1", "Prop 1", "Primary held item bone"),
    ("PROP2", "Prop 2", "Secondary held item bone (chieftains only)"),
    ("PROP3", "Prop 3", "Tertiary held item bone (chieftains and native warrior)"),
    ("BELT", "Belt", "Pouches, hanging items"),
    ("CUSTOM", "Custom bone", "Type the exact bone name"),
)


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


class MeshRecord:
    """Triangle mesh in game terms: shared vertex positions plus per-corner UV, colour and normal, per-vertex skins."""

    def __init__(self):
        self.verts = []        # (x, y, z)
        self.skins = []        # per vertex: [(bone, weight)]
        self.faces = []        # (i, j, k) into verts
        self.loop_uvs = []     # per corner (3 per face)
        self.loop_uv2s = None  # per corner or None when the file has no second UV set
        self.loop_cols = []    # per corner (r, g, b, a)
        self.loop_normals = []  # per corner (nx, ny, nz)


def mesh_record_from_xml(root, flip_v):
    record = MeshRecord()
    vert_index = {}
    seen_faces = set()
    has_uv2 = False
    uv2s = []

    def add_vertex(vertex, pos, dedup):
        skin = [(s.get("bone"), float(s.get("weight"))) for s in vertex]
        key = (pos, tuple(skin))  # same position with different weights stays a separate vertex
        idx = vert_index.get(key) if dedup else None
        if idx is None:
            idx = len(record.verts)
            if dedup:
                vert_index[key] = idx
            record.verts.append(pos)
            record.skins.append(skin)
        return idx

    for polygon in root.find("polygons"):
        vertices = list(polygon)
        positions = [(float(v.get("x")), float(v.get("y")), float(v.get("z"))) for v in vertices]
        face = [add_vertex(v, p, True) for v, p in zip(vertices, positions)]
        # Merging by position would make this face degenerate or a duplicate (double-sided cards),
        # and Blender's validate() would drop it; give it its own vertices instead.
        key = frozenset(face)
        if len(key) < len(face) or key in seen_faces:
            face = [add_vertex(v, p, False) for v, p in zip(vertices, positions)]
            key = frozenset(face)
        seen_faces.add(key)
        for vertex in vertices:
            v = float(vertex.get("v"))
            record.loop_uvs.append((float(vertex.get("u")), 1.0 - v if flip_v else v))
            if vertex.get("u2") is not None:
                has_uv2 = True
                v2 = float(vertex.get("v2"))
                uv2s.append((float(vertex.get("u2")), 1.0 - v2 if flip_v else v2))
            else:
                uv2s.append((0.0, 0.0))
            record.loop_cols.append((float(vertex.get("r")), float(vertex.get("g")),
                                     float(vertex.get("b")), float(vertex.get("a"))))
            # Source files carry normals of any length; the game normalises at load, Blender wants unit vectors.
            n = Vector((float(vertex.get("nx")), float(vertex.get("ny")), float(vertex.get("nz"))))
            n.normalize()
            record.loop_normals.append(tuple(n))
        record.faces.append(tuple(face))
    if has_uv2:
        record.loop_uv2s = uv2s
    return record


def build_mesh(name, record):
    """Mesh data block from a record; the file's corner normals become custom split normals."""
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(record.verts, [], record.faces)
    mesh.validate()
    if len(mesh.loops) != len(record.loop_uvs):
        return mesh
    uv_layer = mesh.uv_layers.new(name="UVMap")
    for i, uv in enumerate(record.loop_uvs):
        uv_layer.data[i].uv = uv
    if record.loop_uv2s is not None:
        uv2_layer = mesh.uv_layers.new(name="UVMap2")
        for i, uv in enumerate(record.loop_uv2s):
            uv2_layer.data[i].uv = uv
    col_layer = mesh.color_attributes.new(name="Col", type="FLOAT_COLOR", domain="CORNER")
    for i, col in enumerate(record.loop_cols):
        col_layer.data[i].color = col
    for poly in mesh.polygons:
        poly.use_smooth = True
    if record.loop_normals:
        mark_normal_seams(mesh, record.loop_normals)
        mesh.normals_split_custom_set(record.loop_normals)
    return mesh


def mark_normal_seams(mesh, loop_normals):
    """Blender shares one custom normal per vertex across a smooth fan, so edges where the file's corner normals
    disagree must be sharp for the normals to survive a round trip."""
    per_edge = {}
    for poly in mesh.polygons:
        loops = list(poly.loop_indices)
        for i, li in enumerate(loops):
            loop = mesh.loops[li]
            next_loop = mesh.loops[loops[(i + 1) % len(loops)]]
            per_edge.setdefault(loop.edge_index, []).append(
                {loop.vertex_index: loop_normals[li], next_loop.vertex_index: loop_normals[next_loop.index]})
    for edge_index, faces in per_edge.items():
        if len(faces) < 2:
            continue
        first = faces[0]
        for other in faces[1:]:
            if any(sum(a * b for a, b in zip(first[v], other[v])) < 0.999999 for v in first if v in other):
                mesh.edges[edge_index].use_edge_sharp = True
                break


def set_vertex_groups(obj, skins):
    obj.vertex_groups.clear()
    bones = sorted({bone for skin in skins for bone, _ in skin})
    groups = {bone: obj.vertex_groups.new(name=bone) for bone in bones}
    for idx, skin in enumerate(skins):
        for bone, weight in skin:
            # Source files can list a bone twice for one vertex; the game sums them, so add rather than replace.
            groups[bone].add([idx], weight, "ADD")


def mesh_record_from_mesh(me, obj, matrix, flip_v, rigid_bone, use_groups):
    """Triangulated record of a Mesh in the space given by matrix.

    rigid_bone skins every vertex to that bone with weight 1. Otherwise vertex groups named after bones are used
    (normalised) when use_groups is set; vertices without any fall back to dummy_bone.
    """
    me.calc_loop_triangles()
    normal_matrix = matrix.to_3x3().inverted().transposed()
    uv_layers = list(me.uv_layers)
    uv_layer = uv_layers[0] if uv_layers else None
    uv2_layer = uv_layers[1] if len(uv_layers) > 1 else None
    col_layer = me.color_attributes.active_color if len(me.color_attributes) else None
    group_names = [g.name for g in obj.vertex_groups]

    record = MeshRecord()
    record.verts = [tuple(matrix @ v.co) for v in me.vertices]
    for v in me.vertices:
        if rigid_bone is not None:
            record.skins.append([(rigid_bone, 1.0)])
            continue
        # Raw group weights, as the source files store them; the game sums them as given.
        weights = [(group_names[g.group], g.weight) for g in v.groups if use_groups and g.weight > 0.0]
        record.skins.append(weights if weights else [(STATIC_BONE, 1.0)])
    if uv2_layer is not None:
        record.loop_uv2s = []
    mirrored = matrix.determinant() < 0  # a mirroring matrix turns the winding inside out
    for tri in me.loop_triangles:
        corners = list(zip(tri.loops, tri.vertices))
        if mirrored:
            corners.reverse()
        record.faces.append(tuple(vi for _, vi in corners))
        for li, vi in corners:
            if uv_layer is not None:
                u, v = uv_layer.data[li].uv
            else:
                u, v = 0.0, 0.0
            record.loop_uvs.append((u, 1.0 - v if flip_v else v))
            if uv2_layer is not None:
                u2, v2 = uv2_layer.data[li].uv
                record.loop_uv2s.append((u2, 1.0 - v2 if flip_v else v2))
            if col_layer is None:
                record.loop_cols.append((1.0, 1.0, 1.0, 1.0))
            else:
                record.loop_cols.append(tuple(col_layer.data[li if col_layer.domain == "CORNER" else vi].color))
            n = normal_matrix @ me.corner_normals[li].vector
            n.normalize()
            record.loop_normals.append(tuple(n))
    return record


def import_mesh_file(context, filepath, flip_v, load_textures, report):
    """Build, link and select one mesh object from a mesh file; None when the file is not a mesh."""
    try:
        root = ET.parse(filepath).getroot()
    except ET.ParseError as e:
        report({"WARNING"}, f"{os.path.basename(filepath)}: XML parse error: {e}")
        return None
    if root.tag != "mesh":
        report({"WARNING"}, f"{os.path.basename(filepath)}: not a Tribal Trouble mesh file (no <mesh> root)")
        return None

    name = os.path.splitext(os.path.basename(filepath))[0]
    record = mesh_record_from_xml(root, flip_v)
    mesh = build_mesh(name, record)
    if len(mesh.loops) != len(record.loop_uvs):
        report({"WARNING"}, f"{os.path.basename(filepath)}: Blender dropped "
                            f"{len(record.faces) - len(mesh.polygons)} invalid faces; loop data skipped")
    obj = bpy.data.objects.new(name, mesh)
    obj["tt_texture"] = root.get("texture") or find_registry_texture(filepath) or ""
    obj["tt_file_texture"] = root.get("texture") or ""
    context.collection.objects.link(obj)
    set_vertex_groups(obj, record.skins)

    if load_textures:
        # Units can declare a comma-separated atlas list (one per weapon tier); preview with the first.
        primary = obj["tt_texture"].split(",")[0].strip()
        image_path = find_texture_image(filepath, primary)
        if image_path is not None:
            obj.data.materials.append(get_atlas_material(primary, image_path))

    context.view_layer.objects.active = obj
    obj.select_set(True)
    report({"INFO"}, f"Imported {len(record.verts)} verts, {len(record.faces)} tris, texture '{obj['tt_texture']}'")
    return obj


class ImportTTMesh(bpy.types.Operator, ImportHelper):
    bl_idname = "import_mesh.tt_xml"
    bl_label = "Import Tribal Trouble Mesh"
    bl_options = {"REGISTER", "UNDO"}
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    files: CollectionProperty(type=bpy.types.OperatorFileListElement, options={"HIDDEN", "SKIP_SAVE"})
    directory: StringProperty(subtype="DIR_PATH", options={"HIDDEN", "SKIP_SAVE"})
    flip_v: BoolProperty(name="Flip V", default=False,
                         description="Flip the vertical texture coordinate on import")
    load_textures: BoolProperty(name="Load Textures", default=True,
                                description="Find the model's atlas PNG under textures/models and build a material")

    def execute(self, context):
        paths = [os.path.join(self.directory, f.name) for f in self.files if f.name]
        if not paths:
            paths = [self.filepath]

        imported = 0
        for path in paths:
            if import_mesh_file(context, path, self.flip_v, self.load_textures, self.report) is not None:
                imported += 1
        if imported == 0:
            return {"CANCELLED"}
        remember_repo_root(context, paths[0])
        if len(paths) > 1:
            self.report({"INFO"}, f"Imported {imported} of {len(paths)} meshes")
        return {"FINISHED"}


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


def append_record_polygons(lines, record):
    li = 0
    for face in record.faces:
        lines.append("        <polygon>")
        for vi in face:
            x, y, z = record.verts[vi]
            r, g, b, a = record.loop_cols[li]
            nx, ny, nz = record.loop_normals[li]
            u, v = record.loop_uvs[li]
            extra = ""
            if record.loop_uv2s is not None:
                u2, v2 = record.loop_uv2s[li]
                extra = f' u2="{u2:.6g}" v2="{v2:.6g}"'
            lines.append(
                f'            <vertex x="{x:.6g}" y="{y:.6g}" z="{z:.6g}" '
                f'r="{r:.4g}" g="{g:.4g}" b="{b:.4g}" a="{a:.4g}" '
                f'nx="{nx:.6g}" ny="{ny:.6g}" nz="{nz:.6g}" '
                f'u="{u:.6g}" v="{v:.6g}"{extra}>')
            for bone, weight in record.skins[vi]:
                lines.append(f"                <skin bone={quoteattr(bone)} weight=\"{weight:.6g}\"/>")
            lines.append("            </vertex>")
            li += 1
        lines.append("        </polygon>")


def mesh_xml_text(objs, bones, texture, flip_v, depsgraph, use_groups=True):
    """objs, evaluated and in world space, as the text of one game mesh file.

    bones[i] is a bone name to skin every vertex of objs[i] to rigidly, or None to use its vertex groups.
    """
    lines = [XML_HEADER, "", DOCTYPE, ""]
    lines.append(f"<mesh texture={quoteattr(texture)}>" if texture else "<mesh>")
    lines.append("    <polygons>")
    for o, bone in zip(objs, bones):
        eval_obj = o.evaluated_get(depsgraph)
        me = eval_obj.to_mesh()
        try:
            append_record_polygons(lines, mesh_record_from_mesh(me, o, export_matrix(o), flip_v, bone, use_groups))
        finally:
            eval_obj.to_mesh_clear()
    lines.append("    </polygons>")
    lines.append("</mesh>")
    return "\n".join(lines) + "\n"


def write_mesh_xml(objs, bones, filepath, texture, flip_v, depsgraph, use_groups=True):
    write_text(filepath, mesh_xml_text(objs, bones, texture, flip_v, depsgraph, use_groups))


def rest_pose_armatures(objs):
    """Armatures that deform or carry these objects; their pose must be at rest while exporting."""
    arms = []
    for o in objs:
        if o.parent is not None and o.parent.type == "ARMATURE":
            arms.append(o.parent)
        for modifier in o.modifiers:
            if modifier.type == "ARMATURE" and modifier.object is not None:
                arms.append(modifier.object)
    return list(dict.fromkeys(arms))


class ExportTTMesh(bpy.types.Operator, ExportHelper):
    bl_idname = "export_mesh.tt_xml"
    bl_label = "Export Tribal Trouble Mesh"
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    texture: StringProperty(name="Texture", default="",
                            description="Texture atlas name (defaults to the object's tt_texture property)")
    flip_v: BoolProperty(name="Flip V", default=False,
                         description="Flip the vertical texture coordinate on export")
    batch_per_object: BoolProperty(name="One File Per Object", default=False,
                                   description="Export each selected object to its own <object name>.xml "
                                               "in the chosen folder instead of merging them into one file")
    attach_point: EnumProperty(name="Attach To", items=ATTACHMENT_POINT_ITEMS, default="NONE",
                               description="Skin every vertex to this attachment point's bone so the item "
                                           "follows it in game. An object's tt_bone property overrides this")
    skeleton: EnumProperty(name="Skeleton", items=SKELETONS, default="PEON",
                           description="Unit skeleton the attachment point is resolved against")
    custom_bone: StringProperty(name="Bone", default="",
                                description="Exact bone name from the unit's skeleton file")
    use_vertex_groups: BoolProperty(name="Skin From Vertex Groups", default=True,
                                    description="With Attach To set to Static, skin vertices by their bone-named "
                                                "vertex groups (imported units keep their weights); vertices "
                                                "without groups get dummy_bone")

    def draw(self, context):
        layout = self.layout
        layout.prop(self, "texture")
        layout.prop(self, "flip_v")
        layout.prop(self, "batch_per_object")
        layout.prop(self, "attach_point")
        if self.attach_point == "CUSTOM":
            layout.prop(self, "custom_bone")
        elif self.attach_point != "NONE":
            layout.prop(self, "skeleton")
        else:
            layout.prop(self, "use_vertex_groups")

    def invoke(self, context, event):
        objs = [o for o in context.selected_objects if o.type == "MESH"]
        if len(objs) == 1:
            self.filepath = objs[0].name + ".xml"
        return super().invoke(context, event)

    def resolve_bone(self, obj):
        """Bone to skin the whole object to, or None to use its vertex groups."""
        bone = obj.get("tt_bone")
        if bone:
            return bone
        if self.attach_point == "NONE":
            return None if self.use_vertex_groups else STATIC_BONE
        if self.attach_point == "CUSTOM":
            return self.custom_bone.strip() or STATIC_BONE
        bone = ATTACHMENT_POINTS[self.attach_point].get(self.skeleton)
        if bone is None:
            self.report({"WARNING"}, f"{self.attach_point} does not exist on the {self.skeleton} skeleton; "
                                     f"{obj.name} skinned to {STATIC_BONE}")
            return STATIC_BONE
        return bone

    def execute(self, context):
        objs = [o for o in context.selected_objects if o.type == "MESH"]
        if not objs and context.active_object is not None and context.active_object.type == "MESH":
            objs = [context.active_object]
        if not objs:
            self.report({"ERROR"}, "Select at least one mesh object to export")
            return {"CANCELLED"}

        # Files hold the bind pose: evaluate deforming and carrying armatures at rest, whatever frame is showing.
        arms = rest_pose_armatures(objs)
        previous = {arm: arm.data.pose_position for arm in arms}
        for arm in arms:
            arm.data.pose_position = "REST"
        if arms:
            context.view_layer.update()
        try:
            return self.write(context, objs)
        finally:
            for arm, position in previous.items():
                arm.data.pose_position = position
            if arms:
                context.view_layer.update()

    def write(self, context, objs):
        depsgraph = context.evaluated_depsgraph_get()
        bones = [self.resolve_bone(o) for o in objs]

        if self.batch_per_object and len(objs) > 1:
            out_dir = os.path.dirname(self.filepath)
            for o, bone in zip(objs, bones):
                texture = self.texture or object_texture(o)
                write_mesh_xml([o], [bone], os.path.join(out_dir, o.name + ".xml"), texture, self.flip_v,
                               depsgraph)
            self.report({"INFO"}, f"Exported {len(objs)} files into {out_dir}")
            return {"FINISHED"}

        texture = self.texture
        if not texture:
            for o in objs:
                if o.get("tt_texture"):
                    texture = o["tt_texture"]
                    break
        if not texture:
            texture = material_image_name(objs)
        write_mesh_xml(objs, bones, self.filepath, texture, self.flip_v, depsgraph)
        self.report({"INFO"}, f"Exported {len(objs)} object(s) merged into {os.path.basename(self.filepath)}")
        return {"FINISHED"}


XML_HEADER = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'

MATRIX_ATTLIST = "".join(f"        <!ATTLIST transform m{c}{r} CDATA #REQUIRED>\n" for c in range(4) for r in range(4))

SKELETON_DOCTYPE = """<!DOCTYPE skeleton [
        <!ELEMENT skeleton   (bones, init_pose)>
        <!ELEMENT bones      (bone+)>
        <!ELEMENT init_pose  (transform+)>
        <!ELEMENT bone        EMPTY>
        <!ELEMENT transform   EMPTY>
        <!ATTLIST transform name CDATA #REQUIRED>
""" + MATRIX_ATTLIST + """        <!ATTLIST bone name CDATA #REQUIRED>
        <!ATTLIST bone parent CDATA #REQUIRED>
        ]>"""

ANIMATION_DOCTYPE = """<!DOCTYPE animation [
        <!ELEMENT animation  (frame+)>
        <!ELEMENT frame      (transform+)>
        <!ELEMENT transform   EMPTY>
        <!ATTLIST frame index CDATA #REQUIRED>
        <!ATTLIST transform name CDATA #REQUIRED>
""" + MATRIX_ATTLIST + """        ]>"""


def matrix_from_element(t):
    """The XML stores each 4x4 as m<column><row>, translation in m30..m32; bones are absolute in model space."""
    m = Matrix.Identity(4)
    for c in range(4):
        for r in range(4):
            m[r][c] = float(t.get(f"m{c}{r}"))
    return m


def matrix_attrs(m):
    parts = []
    for c in range(4):
        for r in range(4):
            v = m[r][c]
            parts.append(f'm{c}{r}="{(0.0 if v == 0 else v):.6g}"')
    return " ".join(parts)


def write_text(filepath, text):
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def write_lines(filepath, lines):
    write_text(filepath, "\n".join(lines) + "\n")


def read_skeleton(path):
    """Return (parents, rest): bone name -> parent name ("" for roots), and bone name -> model-space rest Matrix."""
    root = ET.parse(path).getroot()
    if root.tag != "skeleton":
        raise ValueError("not a Tribal Trouble skeleton file (no <skeleton> root)")
    parents = {b.get("name"): b.get("parent") for b in root.find("bones")}
    rest = {t.get("name"): matrix_from_element(t) for t in root.find("init_pose")}
    return parents, rest


def read_animation(path):
    """Return frames in index order, each bone name -> model-space Matrix."""
    root = ET.parse(path).getroot()
    if root.tag != "animation":
        raise ValueError("not a Tribal Trouble animation file (no <animation> root)")
    frames = {}
    for frame in root.iter("frame"):
        frames[int(frame.get("index"))] = {t.get("name"): matrix_from_element(t) for t in frame.iter("transform")}
    return [frames[i] for i in sorted(frames)]


def normalized_rest(m):
    r = m.to_3x3()
    r.normalize()
    out = r.to_4x4()
    out.translation = m.to_translation()
    return out


def build_armature(context, name, parents, rest):
    """Armature whose bones' rest matrices equal the skeleton file's (bone Y axis follows the file's Y column)."""
    arm_data = bpy.data.armatures.new(name)
    arm_data.display_type = "STICK"
    arm = bpy.data.objects.new(name, arm_data)
    context.collection.objects.link(arm)
    for o in context.selected_objects:
        o.select_set(False)
    context.view_layer.objects.active = arm
    arm.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    children = {}
    for bone, parent in parents.items():
        children.setdefault(parent, []).append(bone)
    edit_bones = {}
    for bone, m in rest.items():
        eb = arm_data.edit_bones.new(bone)
        head = m.to_translation()
        length = min(((rest[k].to_translation() - head).length for k in children.get(bone, []) if k in rest),
                     default=0.1)
        eb.head = (0.0, 0.0, 0.0)
        eb.tail = (0.0, max(length, 0.02), 0.0)
        eb.matrix = normalized_rest(m)
        edit_bones[bone] = eb
    for bone, eb in edit_bones.items():
        eb.parent = edit_bones.get(parents.get(bone))
    bpy.ops.object.mode_set(mode="OBJECT")
    return arm


def assign_action(arm, action):
    anim = arm.animation_data or arm.animation_data_create()
    anim.action = action
    if hasattr(action, "slots") and getattr(anim, "action_slot", None) is None:
        # Blender 4.4+ slotted actions: bind the first slot, creating one for a fresh action.
        slot = action.slots[0] if len(action.slots) else action.slots.new(id_type="OBJECT", name=arm.name)
        anim.action_slot = slot
    return anim


def apply_clip(context, arm, name, frames):
    """One action keyed at frames 1..n. Pose = parent_pose @ parent_rest^-1 @ rest @ basis, so basis is solved
    per bone from the file's absolute matrices without touching the depsgraph."""
    action = bpy.data.actions.new(name)
    action.use_fake_user = True
    action["tt_armature"] = arm.name
    assign_action(arm, action)
    rest = {b.name: b.matrix_local for b in arm.data.bones}
    for pb in arm.pose.bones:
        pb.rotation_mode = "QUATERNION"
    for f, frame in enumerate(frames, start=1):
        for pb in arm.pose.bones:
            m = frame.get(pb.name)
            if m is None:
                continue
            parent = pb.parent
            if parent is not None and parent.name in frame:
                local_rest = rest[parent.name].inverted() @ rest[pb.name]
                basis = (frame[parent.name] @ local_rest).inverted() @ m
            else:
                basis = rest[pb.name].inverted() @ m
            loc, rot, scale = basis.decompose()
            pb.location = loc
            pb.rotation_quaternion = rot
            pb.scale = scale
            pb.keyframe_insert("location", frame=f, group=pb.name)
            pb.keyframe_insert("rotation_quaternion", frame=f, group=pb.name)
            pb.keyframe_insert("scale", frame=f, group=pb.name)
    context.scene.frame_start = 1
    context.scene.frame_end = max(context.scene.frame_end, len(frames))
    return action


def bind_meshes(arm, meshes):
    """Armature modifier plus parenting for meshes whose vertex groups name this armature's bones."""
    bone_names = {b.name for b in arm.data.bones}
    bound = 0
    for o in meshes:
        if not any(vg.name in bone_names for vg in o.vertex_groups):
            continue
        modifier = o.modifiers.new("Armature", "ARMATURE")
        modifier.object = arm
        o.parent = arm
        o.matrix_parent_inverse = arm.matrix_world.inverted()
        bound += 1
    return bound


def active_armature(context):
    o = context.active_object
    if o is None:
        return None
    if o.type == "ARMATURE":
        return o
    if o.parent is not None and o.parent.type == "ARMATURE":
        return o.parent
    return None


def armature_from_file(context, path):
    parents, rest = read_skeleton(path)
    name = os.path.splitext(os.path.basename(path))[0]
    if name.endswith("_skeleton"):
        name = name[:-len("_skeleton")]
    arm = build_armature(context, name, parents, rest)
    arm["tt_skeleton"] = path
    setup_attachment_slots(arm)
    return arm


class ImportTTSkeleton(bpy.types.Operator, ImportHelper):
    """Import a *_skeleton.xml as an armature and/or clip files (peon_run.xml ...) as actions on the active armature"""
    bl_idname = "import_scene.tt_skeleton"
    bl_label = "Import Tribal Trouble Skeleton / Animation"
    bl_options = {"REGISTER", "UNDO"}
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    files: CollectionProperty(type=bpy.types.OperatorFileListElement, options={"HIDDEN", "SKIP_SAVE"})
    directory: StringProperty(subtype="DIR_PATH", options={"HIDDEN", "SKIP_SAVE"})
    bind_selected: BoolProperty(name="Bind Selected Meshes", default=True,
                                description="Parent selected meshes whose vertex groups match the bones to the "
                                            "new armature, with an Armature modifier")

    def execute(self, context):
        paths = [os.path.join(self.directory, f.name) for f in self.files if f.name] or [self.filepath]
        skeletons, clips = [], []
        for path in paths:
            try:
                tag = ET.parse(path).getroot().tag
            except (ET.ParseError, OSError) as e:
                self.report({"WARNING"}, f"{os.path.basename(path)}: cannot read: {e}")
                continue
            if tag == "skeleton":
                skeletons.append(path)
            elif tag == "animation":
                clips.append(path)
            else:
                self.report({"WARNING"}, f"{os.path.basename(path)}: not a skeleton or animation file")

        meshes = [o for o in context.selected_objects if o.type == "MESH"]
        bound = 0
        if skeletons:
            if len(skeletons) > 1:
                self.report({"WARNING"}, f"Several skeleton files selected; using {os.path.basename(skeletons[0])}")
            arm = armature_from_file(context, skeletons[0])
            remember_repo_root(context, skeletons[0])
            if self.bind_selected:
                bound = bind_meshes(arm, meshes)
        else:
            arm = active_armature(context)
            if arm is None:
                self.report({"ERROR"}, "Select an armature to receive the clips, or include a *_skeleton.xml file")
                return {"CANCELLED"}

        loaded = 0
        for path in clips:
            name = os.path.splitext(os.path.basename(path))[0]
            try:
                action = apply_clip(context, arm, name, read_animation(path))
            except (ValueError, KeyError, TypeError, AttributeError, OSError) as e:
                self.report({"WARNING"}, f"{os.path.basename(path)}: skipped: {e}")
                continue
            action["tt_clip"] = os.path.basename(path)
            loaded += 1

        context.view_layer.objects.active = arm
        arm.select_set(True)
        self.report({"INFO"}, f"{arm.name}: {len(arm.data.bones)} bones, {loaded} clip(s), {bound} mesh(es) bound")
        return {"FINISHED"}


def write_skeleton_xml(arm, filepath):
    bones = sorted(arm.data.bones, key=lambda b: b.name)
    lines = [XML_HEADER, "", SKELETON_DOCTYPE, "", "<skeleton>", "    <bones>"]
    for b in bones:
        parent = b.parent.name if b.parent is not None else ""
        lines.append(f"        <bone name={quoteattr(b.name)} parent={quoteattr(parent)}/>")
    lines += ["    </bones>", "    <init_pose>"]
    for b in bones:
        lines.append(f"        <transform name={quoteattr(b.name)} {matrix_attrs(b.matrix_local)}/>")
    lines += ["    </init_pose>", "</skeleton>"]
    write_lines(filepath, lines)


def write_animation_xml(context, arm, action, filepath):
    """Sample the action at every whole frame of its range; bone matrices are armature space, as the game wants."""
    anim = arm.animation_data
    previous = anim.action if anim is not None else None
    previous_frame = context.scene.frame_current
    assign_action(arm, action)
    start, end = action.frame_range
    bones = sorted(arm.pose.bones, key=lambda b: b.name)
    lines = [XML_HEADER, "", ANIMATION_DOCTYPE, "", "<animation>"]
    for index, frame in enumerate(range(int(round(start)), int(round(end)) + 1)):
        context.scene.frame_set(frame)
        lines.append(f'    <frame index="{index}">')
        for pb in bones:
            lines.append(f"        <transform name={quoteattr(pb.name)} {matrix_attrs(pb.matrix)}/>")
        lines.append("    </frame>")
    lines.append("</animation>")
    write_lines(filepath, lines)
    if previous is not None:
        assign_action(arm, previous)
    context.scene.frame_set(previous_frame)


def armature_actions(arm):
    """Actions imported for this armature, or failing that the one currently assigned."""
    actions = [a for a in bpy.data.actions if a.get("tt_armature") == arm.name]
    if not actions and arm.animation_data is not None and arm.animation_data.action is not None:
        actions = [arm.animation_data.action]
    return actions


class ExportTTSkeleton(bpy.types.Operator, ExportHelper):
    """Export the active armature's rest pose as a skeleton file and its clips as animation files beside it"""
    bl_idname = "export_scene.tt_skeleton"
    bl_label = "Export Tribal Trouble Skeleton / Animation"
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    export_skeleton: BoolProperty(name="Skeleton", default=True,
                                  description="Write the rest pose and bone hierarchy to the chosen file")
    export_clips: BoolProperty(name="Animation Clips", default=True,
                               description="Write each of the armature's actions as <action name>.xml in the same "
                                           "folder, sampled at every frame of its range")

    def invoke(self, context, event):
        arm = active_armature(context)
        if arm is not None:
            self.filepath = arm.name + "_skeleton.xml"
        return super().invoke(context, event)

    def execute(self, context):
        arm = active_armature(context)
        if arm is None:
            self.report({"ERROR"}, "Select an armature (or a mesh parented to one)")
            return {"CANCELLED"}
        written = []
        if self.export_skeleton:
            write_skeleton_xml(arm, self.filepath)
            written.append(os.path.basename(self.filepath))
        if self.export_clips:
            out_dir = os.path.dirname(self.filepath)
            taken = set()
            # A copy made in the Action editor carries the original's tt_clip: the action still named after that file
            # keeps it, the others fall back to their own names.
            actions = sorted(armature_actions(arm),
                             key=lambda a: os.path.splitext(a.get("tt_clip") or "")[0] != a.name)
            for action in actions:
                path = os.path.join(out_dir, action.get("tt_clip") or action.name + ".xml")
                if os.path.normcase(path) in taken:
                    path = os.path.join(out_dir, action.name + ".xml")
                if os.path.normcase(path) in taken:
                    self.report({"WARNING"}, f"{action.name} not exported: {os.path.basename(path)} is another clip's")
                    continue
                taken.add(os.path.normcase(path))
                write_animation_xml(context, arm, action, path)
                written.append(os.path.basename(path))
        if not written:
            self.report({"WARNING"}, "Nothing selected to export")
            return {"CANCELLED"}
        self.report({"INFO"}, "Exported " + ", ".join(written))
        return {"FINISHED"}


POINT_LABELS = {identifier: label for identifier, label, _ in ATTACHMENT_POINT_ITEMS}


def resolve_attachment_bones(arm):
    """(point, bone) for every attachment point whose bone exists on this armature."""
    names = {b.name for b in arm.data.bones}
    found = []
    for point, _, _ in ATTACHMENT_POINT_ITEMS:
        bone = next((b for b in ATTACHMENT_POINTS.get(point, {}).values() if b in names), None)
        if bone is not None:
            found.append((point, bone))
    return found


def setup_attachment_slots(arm):
    existing = {slot.point for slot in arm.tt_attachments}
    added = 0
    for point, bone in resolve_attachment_bones(arm):
        if point in existing:
            continue
        slot = arm.tt_attachments.add()
        slot.point = point
        slot.bone = bone
        added += 1
    return added


def bone_tail_matrices(arm, bone_name):
    """(rest, posed) world matrices of the bone tail, which is what Blender parents bone children to."""
    bone = arm.data.bones[bone_name]
    tail = Matrix.Translation((0.0, bone.length, 0.0))
    return arm.matrix_world @ bone.matrix_local @ tail, arm.matrix_world @ arm.pose.bones[bone_name].matrix @ tail


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


def export_matrix(o):
    """World matrix with the armature at rest for bone-parented attachments, so scrubbing never leaks into a file."""
    parent = o.parent
    if parent is not None and parent.type == "ARMATURE" and o.parent_type == "BONE" \
            and o.parent_bone in parent.data.bones:
        rest_tail, _ = bone_tail_matrices(parent, o.parent_bone)
        return rest_tail @ o.matrix_parent_inverse @ o.matrix_basis
    return o.matrix_world


def detach_object(obj):
    world = obj.matrix_world.copy()
    obj.parent = None
    obj.matrix_world = world
    obj.pop("tt_bone", None)
    set_item_visible(obj, True)


def attachment_obj_poll(self, obj):
    """Only the artist's own meshes: items loaded from the registry already have their place and their own buttons."""
    return obj.type == "MESH" and not obj.get(BROWSER_TAG)


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


def find_base_sprite(skeleton_path):
    """(group, sprite name) of the registry entry that owns this skeleton and carries the clips."""
    registry = find_up(skeleton_path, "geometry.xml") if skeleton_path else None
    if registry is None:
        return None, None
    try:
        root = ET.parse(registry).getroot()
    except ET.ParseError:
        return None, None
    skeleton_norm = os.path.abspath(skeleton_path).replace("\\", "/").lower()
    for group in root.iter("group"):
        for sprite in group.iter("sprite"):
            skeleton = sprite.find("skeleton")
            if skeleton is None or not skeleton.text or sprite.find("animation") is None:
                continue
            if skeleton_norm.endswith(skeleton.text.strip().replace("\\", "/").lower()):
                return group.get("name"), sprite.get("name")
    return None, None


def sprite_text(attrs, models, skeleton=None, clips=()):
    """One geometry.xml sprite entry. attrs are (name, value) pairs in order, models (path, [(texture, team
    attribute)], one per tier) and clips (name, type, path)."""
    lines = ["        <sprite " + " ".join(f"{key}={quoteattr(value)}" for key, value in attrs) + ">"]
    if skeleton:
        lines.append(f"            <skeleton>{escape(skeleton)}</skeleton>")
    for path, textures in models:
        lines += ['            <model r="90" g="60" b="30">', f"                {escape(path)}"]
        lines += [f"                <texture name={quoteattr(texture)}{team}/>" for texture, team in textures]
        lines.append("            </model>")
    for name, kind, path in clips:
        lines.append(f'            <animation name={quoteattr(name)} wpc="1" type="{kind}">{escape(path)}</animation>')
    return "\n".join(lines + ["        </sprite>"])


def registry_entries(context, arm, base):
    """(sprite name, geometry.xml text) for every visible new attachment in the panel's point slots."""
    root = repo_root(context)
    entries = []
    for slot in arm.tt_attachments:
        if slot.obj is None or not slot.visible:
            continue
        obj = slot.obj
        game_slot = GAME_SLOTS.get(slot.point, slot.point.lower())
        textures = [(t, team_attribute(root, t, bool(obj.get("tt_texture"))))
                    for t in texture_names(obj) or ["TEXTURE"]]
        model = f"misc/{obj.name}.xml"
        if root and arm.get("tt_skeleton"):
            model = os.path.relpath(os.path.join(os.path.dirname(arm["tt_skeleton"]), obj.name + ".xml"),
                                    os.path.join(root, GEOMETRY_DIR)).replace(os.sep, "/")
        name = f"{base}_{obj.name}"
        entries.append((name, sprite_text([("name", name), ("base", base), ("slot", game_slot)],
                                          [(model, textures)])))
    return entries


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
        entries = [text for _, text in registry_entries(context, arm, base)]
        context.window_manager.clipboard = "\n".join(entries) + "\n"
        where = f"group {group}" if group else "the unit's group"
        self.report({"INFO"}, f"Copied {len(entries)} sprite entr{'y' if len(entries) == 1 else 'ies'} for {where}")
        return {"FINISHED"}


SLOT_LABELS = {"hat": "Hats", "weapon": "Weapons", "carried": "Carried"}
SLOT_HINTS = {"hat": "One at a time, as in game (H cycles). Shift click keeps others",
              "weapon": "One at a time, as in game. Shift click keeps others",
              "carried": "Locked: the game needs all five. Edit them, never remove"}
ALL_SLOTS = "ALL"
_slot_filter_items = []


def slot_label(game_slot):
    return SLOT_LABELS.get(game_slot, game_slot.replace("_", " ").title())


def slot_filter_items(self, context):
    arm = active_armature(context)
    slots = sorted(unit_items(arm)) if arm is not None else []
    _slot_filter_items[:] = [(ALL_SLOTS, "All", "Every item on this unit")] + [
        (game_slot, slot_label(game_slot), SLOT_HINTS.get(game_slot, "")) for game_slot in slots]
    return _slot_filter_items


def item_rows(objects, arm, slot_filter, name_filter):
    """(visible, order) per object for the items list: this unit's registry items, narrowed by slot and by typed
    text, sorted by slot then name."""
    wanted = name_filter.strip().lower()
    shown = [arm is not None and o.get("tt_slot") and not o.get("tt_detail") and o.parent == arm
             and slot_filter in (ALL_SLOTS, "", o["tt_slot"]) and wanted in o.get("tt_sprite", "").lower()
             for o in objects]
    ranked = sorted(range(len(objects)), key=lambda i: (not shown[i], objects[i].get("tt_slot", ""),
                                                        objects[i].get("tt_sprite", objects[i].name)))
    order = [0] * len(objects)
    for position, index in enumerate(ranked):
        order[index] = position
    return [bool(x) for x in shown], order


class TT_UL_items(bpy.types.UIList):
    """The unit's items. The list keeps its height and scrolls, however many there are"""

    def draw_item(self, context, layout, data, item, icon, active_data, active_property, index):
        row = layout.row(align=True)
        hidden = not item_shown(item)
        row.operator(ShowItem.bl_idname, text="", icon="HIDE_ON" if hidden else "HIDE_OFF", emboss=False).item = item.name
        row.label(text=item["tt_sprite"])
        tag = row.row()
        tag.alignment = "RIGHT"
        tag.enabled = False
        tag.label(text=slot_label(item["tt_slot"]))
        if shares_unit_texture(active_armature(context), item):
            row.operator(OwnTexture.bl_idname, text="", icon="IMAGE_DATA", emboss=False).target = item.name
        row.operator(PaintItem.bl_idname, text="", icon="BRUSH_DATA", emboss=False).target = item.name
        if item["tt_slot"] != CARRY_SLOT:
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH", emboss=False)
            remove.group = item.get("tt_group", "")
            remove.sprite = item["tt_sprite"]
        else:
            lock = row.row()
            lock.enabled = False
            lock.label(text="", icon="LOCKED")  # the game asks for these by name, so they cannot be removed

    def draw_filter(self, context, layout):
        pass  # the search field sits above the list instead

    def filter_items(self, context, data, propname):
        objects = list(getattr(data, propname))
        wm = context.window_manager
        shown, order = item_rows(objects, active_armature(context), wm.tt_item_filter, wm.tt_item_search)
        return [self.bitflag_filter_item if x else 0 for x in shown], order
_point_items = []


def point_items(self, context):
    arm = active_armature(context)
    slots = arm.tt_attachments if arm is not None else []
    _point_items[:] = [(slot.point, POINT_LABELS.get(slot.point, slot.point), "") for slot in slots] or [("HEAD", "Head", "")]
    return _point_items


class VIEW3D_PT_tt_attachments(bpy.types.Panel):
    bl_label = "Carried Items"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def draw(self, context):
        arm = active_armature(context)
        wm = context.window_manager
        layout = self.layout
        if not arm.tt_attachments:
            layout.operator(SetupAttachments.bl_idname)
            return
        layout.label(text="Hats, weapons and things this unit carries")
        items = unit_items(arm)
        if items:
            if len(items) > 1:
                layout.row(align=True).prop(wm, "tt_item_filter", expand=True)
            layout.prop(wm, "tt_item_search", text="", icon="VIEWZOOM")
            layout.template_list("TT_UL_items", "", bpy.data, "objects", wm, "tt_item_index", rows=6, maxrows=12)
            chosen = wm.tt_item_filter if wm.tt_item_filter != ALL_SLOTS else (next(iter(items)) if len(items) == 1
                                                                               else "")
            layout.label(text=SLOT_HINTS.get(chosen, "Click the eye to show or hide an item"))
        else:
            layout.label(text="Nothing yet: add one below", icon="INFO")

        box = layout.box()
        box.label(text="Add a new item", icon="ADD")
        box.prop(wm, "tt_new_point", text="Where")
        slot = next((x for x in arm.tt_attachments if x.point == wm.tt_new_point), arm.tt_attachments[0])
        box.prop(slot, "obj", text="Mesh")
        if slot.obj is not None:
            box.operator(PutOnBone.bl_idname, icon="SNAP_ON",
                         text=f"Snap To {POINT_LABELS.get(slot.point, slot.point)}").point = slot.point
        if shares_unit_texture(arm, slot.obj):
            box.operator(OwnTexture.bl_idname, icon="IMAGE_DATA").target = slot.obj.name
        if slot.obj is not None and mesh_texture_image(slot.obj) is None:
            row = box.row()
            row.alert = True
            row.operator(MakeTexture.bl_idname, icon="TEXTURE").target = slot.obj.name
        elif context.mode == "PAINT_TEXTURE":
            box.operator(DonePainting.bl_idname, icon="CHECKMARK", depress=True)
        elif slot.obj is not None:
            box.operator(PaintItem.bl_idname, icon="BRUSH_DATA").target = slot.obj.name
        waiting = [x for x in arm.tt_attachments if x.obj is not None and x != slot]
        for other in waiting:
            row = box.row(align=True)
            row.label(text=f"{POINT_LABELS.get(other.point, other.point)}: {other.obj.name}", icon="DOT")
            row.prop(other, "obj", text="")
        if slot.obj is None and not waiting:
            box.label(text="1. Pick where it goes and your mesh")
            box.label(text="2. Snap it, nudge it, Publish")
        row = layout.row(align=True)
        row.scale_y = 1.4
        row.operator(SaveItems.bl_idname, icon="EXPORT")
        row.operator(Preflight.bl_idname, text="", icon="CHECKMARK")
        if wm.tt_checked:
            box = layout.box()
            if not wm.tt_checks:
                box.label(text="Nothing to fix", icon="CHECKMARK")
            for check in wm.tt_checks:
                box.label(text=check.name, icon=CHECK_ICONS.get(check.level, "INFO"))


class VIEW3D_PT_tt_attachments_more(bpy.types.Panel):
    bl_label = "By hand"
    bl_parent_id = "VIEW3D_PT_tt_attachments"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"
    bl_options = {"DEFAULT_CLOSED"}

    def draw(self, context):
        col = self.layout.column(align=True)
        col.operator(SetupAttachments.bl_idname, text="Refresh Points")
        col.operator(ExportToRepo.bl_idname)
        col.operator(AddToRegistry.bl_idname)
        col.operator(ExportAttachments.bl_idname)
        col.operator(CopyRegistrySnippet.bl_idname)


GEOMETRY_DIR = os.path.join("assets", "geometry")
REGISTRY_FILE = os.path.join(GEOMETRY_DIR, "geometry.xml")
BROWSER_TAG = "tt_browser"


def resolve_repo_root(path):
    """Accepts the repo root or its assets or geometry folder; '' when geometry.xml is not under it."""
    path = bpy.path.abspath(path or "").rstrip("\\/")
    for candidate in (path, os.path.dirname(path), os.path.dirname(os.path.dirname(path))):
        if candidate and os.path.isfile(os.path.join(candidate, REGISTRY_FILE)):
            return candidate
    return ""


def root_holder(context):
    """The add-on preferences when the add-on is enabled, else the window manager for this session."""
    addon = context.preferences.addons.get(__name__)
    return addon.preferences if addon is not None else context.window_manager


def repo_root(context):
    holder = root_holder(context)
    return resolve_repo_root(getattr(holder, "repo_root", "") or getattr(holder, "tt_repo_root", ""))


def remember_repo_root(context, file_path):
    if repo_root(context):
        return
    registry = find_up(file_path, REGISTRY_FILE)
    if registry is None:
        return
    root = os.path.dirname(os.path.dirname(os.path.dirname(registry)))
    holder = root_holder(context)
    setattr(holder, "repo_root" if hasattr(holder, "repo_root") else "tt_repo_root", root)


def read_registry(root):
    """Every sprite in geometry.xml as a dict, in file order."""
    sprites = []
    for group in ET.parse(os.path.join(root, REGISTRY_FILE)).getroot().findall("group"):
        for sprite in group.findall("sprite"):
            skeleton = sprite.find("skeleton")
            sprites.append({
                "group": group.get("name"), "name": sprite.get("name"), "base": sprite.get("base") or "",
                "slot": sprite.get("slot") or "", "default": sprite.get("default") == "true",
                "event": sprite.get("event") or "", "skin": sprite.get("skin") or "",
                "replaces": sprite.get("replaces") or "",
                "textures": [[(t.get("name"), t.get("event") or "") for t in m.findall("texture")]
                             for m in sprite.findall("model")],
                "skeleton": skeleton.text.strip() if skeleton is not None and skeleton.text else "",
                "models": [(m.text or "").strip() for m in sprite.findall("model")],
                "clips": [(a.text or "").strip() for a in sprite.findall("animation")],
                "clip_info": {a.get("name"): (a.get("wpc"), a.get("type"), (a.text or "").strip())
                              for a in sprite.findall("animation")},
            })
    return sprites


def rig_entry(registry, entry):
    """The sprite that supplies the skeleton and clips: the entry itself, or its base."""
    if not entry["base"]:
        return entry
    return next((s for s in registry if s["group"] == entry["group"] and s["name"] == entry["base"]), entry)


def rig_in_repo(context, arm):
    """Registry edits go to the repo folder's geometry.xml, so the rig must have been loaded from under it."""
    root = repo_root(context)
    skeleton = arm.get("tt_skeleton", "") if arm is not None else ""
    if not root or not skeleton:
        return False
    geometry = os.path.normcase(os.path.abspath(os.path.join(root, GEOMETRY_DIR)))
    return os.path.normcase(os.path.abspath(skeleton)).startswith(geometry + os.sep)


def rig_registry(context, arm):
    """(group, base, rig sprite) of an armature loaded from the repo folder, Nones for any other."""
    if not rig_in_repo(context, arm):
        return None, None, None
    group, base = find_base_sprite(arm["tt_skeleton"])
    rig = next((s for s in read_registry(repo_root(context)) if s["group"] == group and s["name"] == base), None)
    return group, base, rig


CATEGORY_ITEMS = (
    ("UNITS", "Units", "Models with a skeleton"),
    ("BUILDINGS", "Buildings", "Buildings and their half built and construction stages"),
    ("RESOURCES", "Resources", "Rocks, wood piles and treasure"),
    ("NATURE", "Trees and Plants", "Trees, palms and plants"),
    ("OTHER", "Other", "Everything else"),
    ("ALL", "All", "Every model"),
)
CATEGORY_ICONS = {"UNITS": "ARMATURE_DATA", "BUILDINGS": "HOME"}


def sprite_category(registry, sprite):
    name = sprite["name"]
    if rig_entry(registry, sprite)["skeleton"]:
        return "UNITS"
    if name.endswith(("_halfbuilt", "_start")) or any(s["group"] == sprite["group"] and s["name"] == name + "_start"
                                                     for s in registry):
        return "BUILDINGS"
    if any(word in name for word in ("tree", "palm", "plant")):
        return "NATURE"
    if name.startswith(("rock_", "wood_", "treasure_")):
        return "RESOURCES"
    return "OTHER"


CARRY_SLOT = "carried"  # what a peon hauls or rows with; the game picks which one shows


def team_attribute(root, texture, fallback):
    """team="..." when the decal PNG exists or is an image Publish writes; without a repo folder, fall back to the
    caller's guess."""
    if root:
        decal = os.path.join(root, "assets", "textures", "teamdecals", texture + "_team.png")
        fallback = os.path.isfile(decal) or texture + "_team" in bpy.data.images
    return f' team="{texture}_team"' if fallback else ""


def refresh_units(context):
    wm = context.window_manager
    wm.tt_units.clear()
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
    text = mesh_xml_text([o], [o.get("tt_bone")], texture, False, depsgraph)
    return text, hashlib.sha1(text.encode("utf-8")).hexdigest()


def export_texts(context, arm, objs):
    """{object: (text, hash)} as Publish would write them, taken with the rig at rest."""
    previous = arm.data.pose_position if arm is not None else None
    if arm is not None:
        arm.data.pose_position = "REST"
    context.view_layer.update()
    try:
        depsgraph = context.evaluated_depsgraph_get()
        return {o: item_export(o, depsgraph) for o in objs}
    finally:
        if arm is not None:
            arm.data.pose_position = previous
            context.view_layer.update()


def write_changed(context, arm, targets):
    """Write each {object: path} whose export differs from the one it was loaded or last written as; the objects
    written, so untouched files in the repo stay as they are."""
    written = []
    for o, (text, digest) in export_texts(context, arm, targets).items():
        if o.get("tt_skin"):
            continue  # showing a skin's look, which is not what its own file holds
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
    for model in sprite["models"]:
        path = os.path.join(geometry, model)
        obj = import_mesh_file(context, path, False, True, report)
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
    entry = next(s for s in registry if s["group"] == group and s["name"] == name)
    quiet = lambda kind, message: report(kind, message) if kind != {"INFO"} else None

    clear_browser_objects()
    for o in context.selected_objects:
        o.select_set(False)
    loaded = load_sprite_models(context, geometry, entry, quiet)
    if not loaded:
        return None
    body = loaded[0]
    arm = None
    rig = rig_entry(registry, entry)
    if rig["skeleton"]:
        arm = armature_from_file(context, os.path.join(geometry, rig["skeleton"]))
        arm[BROWSER_TAG] = True
        bind_meshes(arm, loaded)
        idle = None
        for clip in rig["clips"]:
            clip_name = os.path.splitext(os.path.basename(clip))[0]
            action = apply_clip(context, arm, clip_name, read_animation(os.path.join(geometry, clip)))
            action["tt_clip"] = os.path.basename(clip)
            action[BROWSER_TAG] = True
            if idle is None or "idle" in clip_name:
                idle = action
        if idle is not None:
            assign_action(arm, idle)

    items = []
    for sprite in registry:
        if sprite["group"] != group or sprite["base"] != name or not sprite["slot"]:
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
    report({"INFO"}, f"{group} / {name}: {len(rig['clips'])} clip(s), {len(items)} registry attachment(s)")
    return active


@bpy.app.handlers.persistent
def refresh_units_on_load(_file=None):
    """The list lives on the window manager, which a file load resets."""
    refresh_units(bpy.context)


def unit_index_update(self, context):
    wm = context.window_manager
    if wm.tt_auto_load and 0 <= wm.tt_unit_index < len(wm.tt_units):
        item = wm.tt_units[wm.tt_unit_index]
        load_unit(context, item.group, item.sprite, lambda kind, message: None)


def root_update(self, context):
    refresh_units(context)


class TTPreferences(bpy.types.AddonPreferences):
    bl_idname = __name__
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
        layout.label(text=item.name, icon=CATEGORY_ICONS.get(item.category, "MESH_CUBE"))


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
    """Replace the browsed model with the one picked in the list, with its skeleton, clips and registry attachments"""
    bl_idname = "wm.tt_load_unit"
    bl_label = "Load"
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


def loaded_models():
    return [o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.type == "MESH" and o.get("tt_source")
            and "tt_export_hash" in o]


class PublishModel(bpy.types.Operator):
    """Save the model loaded from the list back to its own files in the repo: every detail level and every item or
    prop of it that you changed. Files you did not touch are left alone"""
    bl_idname = "wm.tt_publish_model"
    bl_label = "Publish Loaded Model"

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context)) and bool(loaded_models())

    def execute(self, context):
        arm = next((o for o in bpy.data.objects if o.type == "ARMATURE" and o.get(BROWSER_TAG)), None)
        written = write_changed(context, arm, {o: o["tt_source"] for o in loaded_models()})
        publish_own_textures(repo_root(context), loaded_models())
        if not written:
            self.report({"INFO"}, "Nothing changed since loading")
            return {"FINISHED"}
        self.report({"INFO"}, f"Saved {', '.join(os.path.basename(o['tt_source']) for o in written)}")
        return {"FINISHED"}


DETAIL_ITEMS = (("HIGH", "High", "The mesh the game draws close up"),
                ("LOW", "Low", "The mesh the game draws from far away"))


def has_low_detail():
    return any(o.get("tt_detail") for o in bpy.data.objects if o.get(BROWSER_TAG))


def detail_update(self, context):
    for obj in [o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.get("tt_levels", 1) > 1
                and not o.get("tt_detail")]:
        set_item_visible(obj, item_shown(obj))


class ShowItem(bpy.types.Operator):
    """Show or hide this item. Carried things switch on and off one by one. Hats and weapons swap, the way the
    game shows one per kind; hold Shift to keep the others showing"""
    bl_idname = "object.tt_show_item"
    bl_label = "Show Item"
    bl_options = {"REGISTER", "UNDO"}
    item: StringProperty()
    keep_others: BoolProperty(options={"SKIP_SAVE"})

    def invoke(self, context, event):
        self.keep_others = event.shift
        return self.execute(context)

    def execute(self, context):
        arm = active_armature(context)
        chosen = bpy.data.objects.get(self.item)
        if arm is None or chosen is None:
            return {"CANCELLED"}
        show = not item_shown(chosen)
        if self.keep_others or chosen["tt_slot"] == CARRY_SLOT:
            set_item_visible(chosen, show)
            return {"FINISHED"}
        for obj in unit_items(arm).get(chosen["tt_slot"], []):
            set_item_visible(obj, obj == chosen and show)
        return {"FINISHED"}


def export_visible(context, arm, report):
    """Write every visible item on the unit to its file, with its texture, and every detail level of the unit and
    of those items that changed since loading. False when nothing was written."""
    unit_dir = os.path.dirname(arm["tt_skeleton"])
    shown = [o for items in unit_items(arm).values() for o in items if item_shown(o)]
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
    for o in objs:
        for name in texture_names(o):
            if not ensure_texture_in_repo(repo_root(context), o, name):
                missing.append(name)
    publish_own_textures(repo_root(context), objs)
    saved = [os.path.basename(o["tt_source"]) for o in written if o in bodies]
    note = f"; unit mesh: {', '.join(saved)}" if saved else ""
    if missing:
        report({"WARNING"}, f"Exported {len(written)} file(s) into {unit_dir}{note}, but no texture image for "
                            f"{', '.join(sorted(set(missing)))}: give the material an Image Texture")
    else:
        report({"INFO"}, f"Exported {len(written)} changed file(s) of {len(objs) + len(levels)} with their textures "
                         f"into {unit_dir}{note}")
    return True


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


def append_registry_entries(registry_path, group, entries):
    """Insert sprite entries at the end of a group in geometry.xml, skipping names it already has."""
    with open(registry_path, "rb") as f:
        raw = f.read()
    newline = "\r\n" if b"\r\n" in raw else "\n"
    text = raw.decode("utf-8")
    start = text.find(f'<group name="{group}">')
    if start < 0:
        raise ValueError(f"No group named {group} in {registry_path}")
    end = text.index("</group>", start)
    line_start = text.rfind("\n", 0, end) + 1
    added = [entry for name, entry in entries if f'<sprite name="{name}"' not in text[start:end]]
    block = "".join(entry.replace("\n", newline) + newline for entry in added)
    with open(registry_path, "wb") as f:
        f.write((text[:line_start] + block + text[line_start:]).encode("utf-8"))
    return len(added)


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
                                        registry_entries(context, arm, base))
        self.report({"INFO"}, f"Added {added} sprite entr{'y' if added == 1 else 'ies'} to group {group}")
        return {"FINISHED"}


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
        obj = slot.obj
        note = ""
        height = unit_height(arm)
        low, high = world_box(obj)
        largest = max(high - low)
        if self.fit and height > 0.0 and largest > height:
            factor = FIT_SHARE * height / largest
            obj.scale = obj.scale * factor
            context.view_layer.update()
            note = f", shrunk to {factor:.0%} of its size because it was bigger than the unit"
        pose_bone = arm.pose.bones[slot.bone]
        on_top = self.point == "HEAD"
        target = arm.matrix_world @ (pose_bone.tail if on_top else pose_bone.head)
        if on_top:
            target = top_of_part(context, arm, slot.bone) or target
        low, high = world_box(obj)
        anchor = Vector(((low.x + high.x) / 2, (low.y + high.y) / 2, low.z)) if on_top \
            else obj.matrix_world.translation.copy()
        moved = obj.matrix_world.copy()
        moved.translation += target - anchor
        obj.matrix_world = moved
        context.view_layer.update()
        self.report({"INFO"}, f"{obj.name} snapped to the {POINT_LABELS.get(self.point, self.point).lower()}{note}")
        return {"FINISHED"}


def switch_workspace(context, name):
    workspace = bpy.data.workspaces.get(name)
    if workspace is not None and context.window is not None:
        context.window.workspace = workspace


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
        paint = context.scene.tool_settings.image_paint
        paint.mode = "IMAGE"
        paint.canvas = image
        switch_workspace(context, "Texture Paint")
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


MIP_PAD = 4  # pixels kept around a cropped item so mipmaps do not bleed in the rest of the atlas


def shares_unit_texture(arm, obj):
    """True when obj is drawn from the loaded unit body's own texture atlas."""
    body = browsed_unit(arm) if arm is not None else None
    if body is None or obj is None or obj.type != "MESH" or obj in model_levels(body):
        return False
    return bool(set(texture_names(obj)) & set(texture_names(body)))


def crop_pixels(image, x0, y0, x1, y1, size):
    """image[y0:y1, x0:x1] on a size x size canvas at the same density, the rest filled by repeating its edges."""
    w, h = image.size
    pixels = np.empty(w * h * 4, np.float32)
    image.pixels.foreach_get(pixels)
    part = pixels.reshape(h, w, 4)[y0:y1, x0:x1]
    return np.pad(part, ((0, size - part.shape[0]), (0, size - part.shape[1]), (0, 0)), mode="edge")


TEXTURE_PREFIXES = {"vikings": "viking", "natives": "native"}


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
        entry = next((s for s in read_registry(root) if s["group"] == obj.get("tt_group")
                      and s["name"] == obj.get("tt_sprite")), None)
        if entry is not None and any(event for model in entry["textures"] for _, event in model):
            self.report({"ERROR"}, f"{entry['name']} has event textures in geometry.xml; this cannot move those")
            return {"CANCELLED"}
        levels = model_levels(obj)
        # Texture names are shared by every group, so they carry the race the way viking_peon and native_warrior do.
        prefix = TEXTURE_PREFIXES.get(obj.get("tt_group", ""), obj.get("tt_group", ""))
        name = obj.get("tt_sprite") or obj.name
        if prefix and not name.startswith(prefix + "_"):
            name = f"{prefix}_{name}"
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
    """Check, write every visible item into the unit's folder with its texture, and list the new ones in
    geometry.xml. A new item then becomes one of the unit's buttons above; its file in the repo is the real copy
    from here on"""
    bl_idname = "object.tt_save_items"
    bl_label = "Publish"

    @classmethod
    def poll(cls, context):
        if rig_in_repo(context, active_armature(context)):
            return True
        cls.poll_message_set("Load the unit from the Models list of your repo folder")
        return False

    def execute(self, context):
        arm = active_armature(context)
        group, base = find_base_sprite(arm["tt_skeleton"])
        if base is None:
            self.report({"ERROR"}, "Could not find this unit's sprite in geometry.xml")
            return {"CANCELLED"}
        taken = {x["name"] for x in read_registry(repo_root(context)) if x["group"] == group}
        fresh = [x for x in arm.tt_attachments if x.obj is not None and x.visible]
        clash = [f"{base}_{x.obj.name}" for x in fresh if f"{base}_{x.obj.name}" in taken]
        if clash:
            self.report({"ERROR"}, f"{group} already has {', '.join(clash)}: rename your mesh")
            return {"CANCELLED"}
        entries = registry_entries(context, arm, base)
        if not export_visible(context, arm, self.report):
            return {"CANCELLED"}
        append_registry_entries(os.path.join(repo_root(context), REGISTRY_FILE), group, entries)
        unit_dir = os.path.dirname(arm["tt_skeleton"])
        for x in fresh:
            obj = x.obj
            obj["tt_group"] = group
            obj["tt_slot"] = GAME_SLOTS.get(x.point, x.point.lower())
            obj["tt_sprite"] = f"{base}_{obj.name}"
            obj["tt_source"] = os.path.join(unit_dir, obj.name + ".xml")
            obj["tt_texture"] = object_texture(obj)
            obj[BROWSER_TAG] = True
            x.prev_name = ""  # hand the mesh over to the unit's buttons instead of letting it go
            x.obj = None
            for other in unit_items(arm).get(obj["tt_slot"], []):
                set_item_visible(other, other == obj)
        self.report({"INFO"}, f"Published {len(fresh)} new item(s) on {group} / {base}; the game shows them after the "
                              f"next build")
        return {"FINISHED"}


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


def unit_meshes(arm):
    return [o for o in bpy.data.objects if o.type == "MESH" and o.parent == arm and o.get("tt_texture")]


def unit_tiers(arm):
    """The unit body's comma-separated atlas list: one entry per weapon tier. Items follow it, they do not set it."""
    lists = [[t.strip() for t in o["tt_texture"].split(",") if t.strip()] for o in unit_meshes(arm)
             if not o.get("tt_slot")]
    return max(lists, key=len, default=[])


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
        assign_action(arm, action)
        context.scene.frame_start = 1
        context.scene.frame_end = max(1, int(round(action.frame_range[1])))
        context.scene.frame_set(1)
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


def mesh_texture_image(obj):
    for slot in obj.material_slots:
        nodes = slot.material.node_tree.nodes if slot.material is not None and slot.material.use_nodes else []
        for node in nodes:
            if node.type == "TEX_IMAGE" and node.image is not None and node.name != TEAM_DECAL:
                return node.image
    return None


def check_mesh(obj, is_new, body_triangles):
    """(level, message) findings for one mesh, covering what goes wrong in game rather than in Blender."""
    found = []
    me = obj.data
    if len(me.polygons) == 0:
        return [("ERROR", "has no faces")]
    if is_new and not re.fullmatch(r"[A-Za-z0-9_]+", obj.name):
        found.append(("ERROR", "name becomes a file and sprite name: use only letters, digits and underscores"))
    uv = me.uv_layers.active
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
        if not re.fullmatch(r"[A-Za-z0-9_]+", image_texture_name(image)):
            found.append(("ERROR", f"image name '{image.name}' becomes the texture name: letters, digits, underscores"))
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


def preflight(context, arm):
    """Findings for everything Export Visible To Repo would write."""
    body = browsed_unit(arm) or next((o for o in unit_meshes(arm) if not o.get("tt_slot")), None)
    body_triangles = sum(len(p.vertices) - 2 for p in body.data.polygons) if body is not None else 0
    new = visible_attachments(arm)
    existing = [o for items in unit_items(arm).values() for o in items if item_shown(o)]
    findings = []
    for obj in new + existing:
        findings += [(level, f"{obj.name}: {text}") for level, text in check_mesh(obj, obj in new, body_triangles)]
    height = unit_height(arm)
    for obj in new:
        low, high = world_box(obj)
        if height > 0.0 and max(high - low) > height:
            findings.append(("WARNING", f"{obj.name}: is bigger than the unit itself ({max(high - low):.1f} against "
                                        f"{height:.1f} tall)"))
    root = repo_root(context)
    group, base = find_base_sprite(arm.get("tt_skeleton", ""))
    if root and base is not None:
        names = {s["name"] for s in read_registry(root) if s["group"] == group}
        for obj in new:
            if f"{base}_{obj.name}" not in names:
                findings.append(("INFO", f"{obj.name}: not in the registry yet, press Add To Registry after exporting"))
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


def remove_registry_entry(registry_path, group, name):
    """Cut one sprite entry out of a group. The mesh files stay on disk."""
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    start = text.find(f'<group name="{group}">')
    end = text.index("</group>", start) if start >= 0 else -1
    match = re.search(r'[ \t]*<sprite name="%s"[ >].*?</sprite>\r?\n' % re.escape(name), text[start:end], re.S) \
        if start >= 0 else None
    if match is None:
        return False
    with open(registry_path, "wb") as f:
        f.write((text[:start + match.start()] + text[start + match.end():]).encode("utf-8"))
    return True


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
        row.operator(SaveClip.bl_idname, text="Publish Clip", icon="EXPORT")
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


def browsed_unit(arm):
    """The mesh loaded from the Models list onto this armature."""
    return next((o for o in unit_meshes(arm) if o.get("tt_group") and not o.get("tt_slot") and not o.get("tt_detail")),
                None)


def clip_short_name(arm, action):
    """walk for an action called peon_walk on the peon rig; the action name otherwise."""
    stem = os.path.splitext(action.get("tt_clip") or action.name)[0]
    prefix = os.path.basename(arm.get("tt_skeleton", "")).replace("skeleton.xml", "")
    return stem[len(prefix):] if prefix and stem.startswith(prefix) and len(stem) > len(prefix) else stem


def set_clip_line(registry_path, group, skeleton, name, line=None):
    """Add or update <animation name=...> (or take it off, with no line) on every sprite in the group that lists
    this skeleton. Items and carried things inherit the unit's clips through base=. New clips go last, so the numbers
    the game already uses do not move. Returns the sprites touched."""
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    touched, out, cursor = [], [], 0
    for sprite_group, sprite, start, end in sprite_blocks(text):
        block = text[start:end]
        listed = re.search(r"<skeleton>\s*([^<]+?)\s*</skeleton>", block)
        if sprite_group != group or listed is None or listed.group(1).replace("\\", "/") != skeleton:
            continue
        if line is None:
            existing = re.search(r'[ \t]*<animation\s+name="%s"[^>]*>[^<]*</animation>[ \t]*\r?\n' % re.escape(name),
                                 block)
            if existing is None:
                continue
            edited = block[:existing.start()] + block[existing.end():]
        else:
            existing = re.search(r'<animation\s+name="%s"[^>]*>[^<]*</animation>' % re.escape(name), block)
            if existing is not None:
                edited = block[:existing.start()] + line + block[existing.end():]
            else:
                anchors = list(re.finditer(r"([ \t]*)<(?:animation|model)\b.*?</(?:animation|model)>[ \t]*(\r?\n)", block,
                                           re.S))
                last = anchors[-1]
                edited = block[:last.end()] + last.group(1) + line + last.group(2) + block[last.end():]
        if edited != block:
            out.append(text[cursor:start] + edited)
            cursor = end
        touched.append(sprite)
    if out:
        with open(registry_path, "wb") as f:
            f.write(("".join(out) + text[cursor:]).encode("utf-8"))
    return touched


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

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self)

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
        context.scene.frame_start = 1
        context.scene.frame_end = max(1, int(round(action.frame_range[1])))
        context.scene.frame_set(1)
        return {"FINISHED"}


class SaveClip(bpy.types.Operator):
    """Write the clip that is showing into the unit's folder and list it in geometry.xml. An existing clip is
    replaced in place and the game plays it straight away; a brand new clip also needs code that asks for it"""
    bl_idname = "object.tt_save_clip"
    bl_label = "Publish Clip"
    clip_name: StringProperty(name="Name", description="The clip's name in geometry.xml, such as run or dance")
    kind: EnumProperty(name="Plays", items=(("loop", "Looping", "Repeats, like idle and run"),
                                            ("plain", "Once", "Plays once and holds, like attack and die")))
    wpc: FloatProperty(name="Distance Per Loop", default=1.0, min=0.0001,
                       description="For a walk or run: how far the unit travels in one loop, so feet do not slide. "
                                   "Leave at 1 for anything that stays in place")

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return rig_in_repo(context, arm) and arm.animation_data is not None and arm.animation_data.action is not None

    def invoke(self, context, event):
        arm = active_armature(context)
        self.clip_name = clip_short_name(arm, arm.animation_data.action)
        rig = rig_registry(context, arm)[2]
        file_name = arm.animation_data.action.get("tt_clip")
        for name, (wpc, kind, path) in (rig["clip_info"].items() if rig else []):
            if file_name and os.path.basename(path) == file_name:
                self.clip_name, self.kind, self.wpc = name, kind, float(wpc)
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        arm = active_armature(context)
        root = repo_root(context)
        action = arm.animation_data.action
        name = self.clip_name.strip().lower()
        if not re.fullmatch(r"[a-z0-9_]+", name):
            self.report({"ERROR"}, "Name the clip with letters, digits and underscores")
            return {"CANCELLED"}
        group, base, rig = rig_registry(context, arm)
        if rig is None:
            self.report({"ERROR"}, "Could not find this unit's sprite in geometry.xml")
            return {"CANCELLED"}
        body = browsed_unit(arm)
        if body is not None and body["tt_sprite"] != base:
            self.report({"ERROR"}, f"{body['tt_sprite']} borrows the {base} rig and its clips: load {base} to "
                                   f"change them")
            return {"CANCELLED"}
        start, end = action.frame_range
        if int(round(end)) - int(round(start)) < 1:
            self.report({"ERROR"}, "The clip has fewer than two frames")
            return {"CANCELLED"}
        geometry = os.path.join(root, GEOMETRY_DIR)
        is_new = name not in rig["clip_info"]
        if is_new:
            prefix = os.path.basename(arm["tt_skeleton"]).replace("skeleton.xml", "")
            path = os.path.join(os.path.dirname(arm["tt_skeleton"]), prefix + name + ".xml")
            listed = {os.path.normcase(os.path.join(geometry, p)) for _, _, p in rig["clip_info"].values()}
            if os.path.isfile(path) or os.path.normcase(path) in listed:
                self.report({"ERROR"}, f"{os.path.basename(path)} already exists: give the clip another name")
                return {"CANCELLED"}
        else:
            path = os.path.join(geometry, rig["clip_info"][name][2])
        write_animation_xml(context, arm, action, path)
        relative = os.path.relpath(path, geometry).replace(os.sep, "/")
        line = f'<animation name="{name}" wpc="{self.wpc:g}" type="{self.kind}">{escape(relative)}</animation>'
        touched = set_clip_line(os.path.join(root, REGISTRY_FILE), group, rig["skeleton"].replace("\\", "/"), name, line)
        action.name = os.path.splitext(os.path.basename(path))[0]
        action["tt_clip"], action["tt_armature"], action[BROWSER_TAG] = os.path.basename(path), arm.name, True
        action.use_fake_user = True
        frames = int(round(end)) - int(round(start)) + 1
        if is_new:
            self.report({"WARNING"}, f"Published new clip {name} ({frames} frames) on {', '.join(touched)}. It shows "
                                     f"in game only once code asks for it")
        else:
            self.report({"INFO"}, f"Replaced clip {name} ({frames} frames); the game plays it after the next build")
        return {"FINISHED"}


ADDON_SOURCE = os.path.join("tools", "blender", "io_tribaltrouble.py")
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
    addon_utils.disable(__name__)
    addon_utils.enable(__name__, default_set=True)
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
            return True
        save_png(image, target)
        return True
    return os.path.isfile(target)


def save_png(image, target):
    previous = (image.filepath_raw, image.file_format)
    image.filepath_raw, image.file_format = target, "PNG"
    image.save()
    image.filepath_raw, image.file_format = previous


def set_sprite_textures(root, group, name, textures):
    """Replace the texture lines of every model of one sprite in geometry.xml; the rest of the file stays as it is."""
    registry_path = os.path.join(root, REGISTRY_FILE)
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    for sprite_group, sprite, start, end in sprite_blocks(text):
        if (sprite_group, sprite) != (group, name):
            continue
        block = text[start:end]
        for model in reversed(list(re.finditer(r"<model\b[^>]*>.*?</model>", block, re.S))):
            lines = list(TEXTURE_LINE.finditer(block, model.start(), model.end()))
            if not lines:
                continue
            first = lines[0].group(0)
            ending = first[len(first.rstrip("\r\n")):]
            new = "".join(f"{lines[0].group(1)}<texture name={quoteattr(t)}{team_attribute(root, t, False)}/>{ending}"
                          for t in textures)
            block = block[:lines[0].start()] + new + block[lines[-1].end():]
        with open(registry_path, "wb") as f:
            f.write((text[:start] + block + text[end:]).encode("utf-8"))
        return True
    return False


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


class RegisterModel(bpy.types.Operator):
    """Export the active mesh as a brand new model and add it to geometry.xml: a static building or prop, a
    unit on the rig it is bound to, or a unit with its own new rig. Using it in game still needs code"""
    bl_idname = "object.tt_register_model"
    bl_label = "Register New Model"
    bl_options = {"REGISTER"}
    sprite_name: StringProperty(name="Name", description="Sprite name in geometry.xml and the new folder's name")
    group: EnumProperty(name="Group", items=registry_group_items)
    low_detail: StringProperty(name="Low Detail", description="Optional mesh object shown at a distance")
    half_built: StringProperty(name="Half Built", description="Building only: mesh for the half built stage, "
                                                              "registered as <name>_halfbuilt")
    half_built_low: StringProperty(name="Half Built Low Detail")
    start: StringProperty(name="Start", description="Building only: mesh for the construction site, registered "
                                                    "as <name>_start")
    start_low: StringProperty(name="Start Low Detail")

    @classmethod
    def poll(cls, context):
        o = context.active_object
        return o is not None and o.type == "MESH" and bool(repo_root(context))

    def invoke(self, context, event):
        self.sprite_name = context.active_object.name
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop(self, "sprite_name")
        arm = active_armature(context)
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

    def execute(self, context):
        root = repo_root(context)
        obj = context.active_object
        name = self.sprite_name.strip()
        if not re.fullmatch(r"[A-Za-z0-9_]+", name):
            self.report({"ERROR"}, "Give the model a name made of letters, digits and underscores")
            return {"CANCELLED"}
        arm = active_armature(context)
        group, base, _ = rig_registry(context, arm)
        if base is None:
            group = self.group

        def picked(prop):
            return bpy.data.objects.get(getattr(self, prop)) if getattr(self, prop) else None

        stages = [("", obj, picked("low_detail"))]
        if arm is None:
            stages += [(suffix, picked(hi), picked(lo)) for suffix, hi, lo in
                       (("_halfbuilt", "half_built", "half_built_low"), ("_start", "start", "start_low"))
                       if picked(hi) is not None]
        meshes = [m for _, hi, lo in stages for m in (hi, lo) if m is not None]
        if any(m.type != "MESH" for m in meshes) or len(set(meshes)) != len(meshes):
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

        base_attr = [("base", base)] if base is not None else []
        entries = [(sprite, sprite_text([("name", sprite)] + base_attr, models, skeleton, clips))
                   for sprite, models in stage_models]
        append_registry_entries(os.path.join(root, REGISTRY_FILE), group, entries)
        refresh_units(context)
        note = f"; add {', '.join(m + '.png' for m in missing)} to assets/textures/models" if missing else ""
        sprites = ", ".join(sprite for sprite, _ in entries)
        self.report({"WARNING"} if missing else {"INFO"},
                    f"Registered {group} / {sprites} under /geometry/{group}/ as .binsprite{note}")
        return {"FINISHED"}


class VIEW3D_PT_tt_units(bpy.types.Panel):
    bl_label = "Models"
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
        row.prop(wm, "tt_auto_load", toggle=True)
        layout.template_list("TT_UL_units", "", wm, "tt_units", wm, "tt_unit_index", rows=10)
        layout.operator(LoadUnit.bl_idname, icon="IMPORT")
        layout.operator(PublishModel.bl_idname, icon="EXPORT")
        row = layout.row(align=True)
        row.operator(RegisterModel.bl_idname, icon="ADD")
        row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH")


PROP_SLOT = "prop"
TEXTURE_LINE = re.compile(r'([ \t]*)<texture\s+name="([^"]+)"([^>]*?)/>[ \t]*\r?\n')


def browsed_building(context):
    """The static model loaded from the Models list, when that is what is on screen."""
    return next((o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.get("tt_group") and o.type == "MESH"
                 and o.parent is None and not o.get("tt_detail")), None)


def building_props(body):
    return sorted((o for o in bpy.data.objects if o.parent == body and o.get("tt_slot") and not o.get("tt_detail")),
                  key=lambda o: o.name)


def new_props(context, body):
    return [o for o in context.selected_objects if o.type == "MESH" and o != body and not o.get(BROWSER_TAG)]


def models_texture_path(root, texture):
    return os.path.join(root, "assets", "textures", "models", texture + ".png")


def event_name(context):
    return context.window_manager.tt_event.strip().lower()


def valid_event(event):
    """Blank means all year; a name ends up in texture file names and geometry.xml."""
    return not event or re.fullmatch(r"[a-z0-9_]+", event) is not None


def sprite_blocks(text):
    """(group, name, start, end) of every sprite element in the registry text."""
    groups = [(m.start(), m.group(1)) for m in re.finditer(r'<group\s+name="([^"]+)"', text)]
    for m in re.finditer(r'<sprite\s+name="([^"]+)"[^>]*>.*?</sprite>', text, re.S):
        group = [name for start, name in groups if start < m.start()][-1]
        yield group, m.group(1), m.start(), m.end()


def edit_event_textures(root, original, event, only, remove):
    """Give every model of every sprite that uses the original atlas a texture for this event (or take them away).
    The game picks a texture by its place in the list, shared by all detail levels, so each model of a sprite gets
    one: <its texture>_<event> when that image is in the repo, its usual texture again when not. Returns the
    sprites touched."""
    registry_path = os.path.join(root, REGISTRY_FILE)
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    touched, out, cursor = [], [], 0
    for group, name, start, end in sprite_blocks(text):
        block = text[start:end]
        models = list(re.finditer(r"<model\b[^>]*>.*?</model>", block, re.S))
        firsts = [TEXTURE_LINE.search(m.group(0)) for m in models]
        if only is not None and (group, name) not in only:
            continue
        if not any(first is not None and first.group(2) == original for first in firsts):
            continue
        edited, offset = block, 0
        for model, first in zip(models, firsts):
            if first is None:
                continue
            body = model.group(0)
            tagged = [t for t in TEXTURE_LINE.finditer(body) if f'event="{event}"' in t.group(3)]
            if remove:
                new_body = body
                for t in reversed(tagged):
                    new_body = new_body[:t.start()] + new_body[t.end():]
            elif tagged:
                continue
            else:
                last = list(TEXTURE_LINE.finditer(body))[-1]
                variant = f"{first.group(2)}_{event}"
                texture = variant if os.path.isfile(models_texture_path(root, variant)) else first.group(2)
                team = re.search(r'\steam="[^"]+"', first.group(3))
                line = (f'{first.group(1)}<texture name="{texture}"{team.group(0) if team else ""} '
                        f'event="{event}"/>' + last.group(0)[len(last.group(0).rstrip("\r\n")):])
                new_body = body[:last.end()] + line + body[last.end():]
            if new_body != body:
                at = model.start() + offset
                edited = edited[:at] + new_body + edited[at + len(body):]
                offset += len(new_body) - len(body)
        if edited != block:
            touched.append(f"{group} / {name}")
            out.append(text[cursor:start] + edited)
            cursor = end
    if touched:
        with open(registry_path, "wb") as f:
            f.write(("".join(out) + text[cursor:]).encode("utf-8"))
    return touched


class NewEventTexture(bpy.types.Operator):
    """Put a copy of the building's texture on it, named for the event, so you paint the copy and never the
    original"""
    bl_idname = "object.tt_new_event_texture"
    bl_label = "Start Event Texture"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return browsed_building(context) is not None and bool(event_name(context)) and bool(repo_root(context))

    def execute(self, context):
        body = browsed_building(context)
        event = event_name(context)
        if not valid_event(event):
            self.report({"ERROR"}, "Name the event with letters, digits and underscores")
            return {"CANCELLED"}
        original = body["tt_texture"].split(",")[0].strip()
        variant = f"{original}_{event}"
        root = repo_root(context)
        if not os.path.isfile(models_texture_path(root, variant)):
            shutil.copyfile(models_texture_path(root, original), models_texture_path(root, variant))
        return bpy.ops.object.tt_show_event_texture(texture=variant)


class ShowEventTexture(bpy.types.Operator):
    """Show the building in this texture"""
    bl_idname = "object.tt_show_event_texture"
    bl_label = "Show Texture"
    bl_options = {"REGISTER", "UNDO"}
    texture: StringProperty()

    def execute(self, context):
        body = browsed_building(context)
        root = repo_root(context)
        if body is None or not root:
            return {"CANCELLED"}
        path = models_texture_path(root, self.texture)
        if not os.path.isfile(path):
            self.report({"ERROR"}, f"{self.texture}.png is not in assets/textures/models")
            return {"CANCELLED"}
        mat = bpy.data.materials.get("tt_" + self.texture)
        if mat is None:
            mat = get_atlas_material(self.texture, path)
            if mat.node_tree.nodes.get(TEAM_MIX) is None:
                # The game keeps the usual team decal for an event texture.
                original = body["tt_texture"].split(",")[0].strip()
                add_team_nodes(mat, next(n for n in mat.node_tree.nodes if n.type == "TEX_IMAGE"),
                               models_texture_path(root, original))
        body.data.materials.clear()
        body.data.materials.append(mat)
        apply_team_preview(context)
        return {"FINISHED"}


class SaveEventTexture(bpy.types.Operator):
    """Write the event texture shown on the building into the repo and list it in geometry.xml for the event.
    Models seen from far away keep their usual texture unless you also paint <their texture>_<event>.png"""
    bl_idname = "object.tt_save_event_texture"
    bl_label = "Publish Event Texture"
    everywhere: BoolProperty(name="Every model that shares this texture", default=True,
                             description="Buildings share one atlas; off means only the model on screen")

    @classmethod
    def poll(cls, context):
        return browsed_building(context) is not None and bool(event_name(context)) and bool(repo_root(context))

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        body = browsed_building(context)
        root = repo_root(context)
        event = event_name(context)
        original = body["tt_texture"].split(",")[0].strip()
        variant = f"{original}_{event}"
        image = mesh_texture_image(body)
        if image is None or image_texture_name(image) != variant:
            self.report({"ERROR"}, f"The building is not showing {variant}: press Start Event Texture first")
            return {"CANCELLED"}
        ensure_texture_in_repo(root, body, variant)
        only = None if self.everywhere else {(body["tt_group"], body["tt_sprite"])}
        touched = edit_event_textures(root, original, event, only, False)
        self.report({"INFO"}, f"Published {variant}.png; {len(touched)} model(s) now use it during {event}")
        return {"FINISHED"}


class RemoveEventTexture(bpy.types.Operator):
    """Take this event's texture off every model that shares the building's texture. The image stays on disk"""
    bl_idname = "object.tt_remove_event_texture"
    bl_label = "Remove Event Texture"

    @classmethod
    def poll(cls, context):
        return browsed_building(context) is not None and bool(event_name(context)) and bool(repo_root(context))

    def invoke(self, context, event):
        return context.window_manager.invoke_confirm(self, event)

    def execute(self, context):
        body = browsed_building(context)
        original = body["tt_texture"].split(",")[0].strip()
        touched = edit_event_textures(repo_root(context), original, event_name(context), None, True)
        self.report({"INFO"}, f"{len(touched)} model(s) no longer change for {event_name(context)}")
        return {"FINISHED"}


class SaveProps(bpy.types.Operator):
    """Write the selected meshes into the building's folder as props drawn with it in game, and add them to
    geometry.xml. Props already in the registry are written back to their own files, and the building's own
    meshes when they changed. With an event named, new props only show during that event"""
    bl_idname = "object.tt_save_props"
    bl_label = "Publish Props"

    @classmethod
    def poll(cls, context):
        return browsed_building(context) is not None and bool(repo_root(context))

    def execute(self, context):
        body = browsed_building(context)
        root = repo_root(context)
        event = event_name(context)
        if not valid_event(event):
            self.report({"ERROR"}, "Name the event with letters, digits and underscores")
            return {"CANCELLED"}
        group, base = body["tt_group"], body["tt_sprite"]
        fresh = new_props(context, body)
        existing = [o for o in building_props(body) if not o.hide_viewport and item_shown(o)]
        findings = [(level, f"{o.name}: {text}") for o in fresh + existing
                    for level, text in check_mesh(o, o in fresh, 0)]
        taken = {s["name"] for s in read_registry(root) if s["group"] == group}
        findings += [("ERROR", f"{o.name}: {group} already has a sprite named {base}_{o.name}") for o in fresh
                     if f"{base}_{o.name}" in taken]
        folder = os.path.dirname(body["tt_source"])
        paths = {o: o.get("tt_source") or os.path.join(folder, o.name + ".xml") for o in fresh + existing}
        findings += [("ERROR", f"{name}: the building's folder already has a file with this name") for name in
                     file_clashes([(o, paths[o]) for o in fresh])]
        errors = store_findings(context, findings)
        if errors:
            self.report({"ERROR"}, f"Not published: {errors} problem(s): " +
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
        saved = write_changed(context, None, {o: o["tt_source"] for o in model_levels(body)})
        note = f"; building mesh: {', '.join(os.path.basename(o['tt_source']) for o in saved)}" if saved else ""
        note += f"; no texture image for {', '.join(sorted(set(missing)))}" if missing else ""
        self.report({"WARNING"} if missing else {"INFO"},
                    f"Published {len(fresh)} new and {len(existing)} existing prop(s) on {group} / {base}{note}")
        return {"FINISHED"}


class VIEW3D_PT_tt_building(bpy.types.Panel):
    bl_label = "Building"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return browsed_building(context) is not None

    def draw(self, context):
        layout = self.layout
        wm = context.window_manager
        body = browsed_building(context)
        root = repo_root(context)
        layout.label(text=f"{body['tt_group']} / {body['tt_sprite']}", icon="MESH_CUBE")
        if has_low_detail():
            layout.row(align=True).prop(wm, "tt_detail", expand=True)
        layout.prop(wm, "tt_event")
        original = body["tt_texture"].split(",")[0].strip()
        box = layout.box()
        box.label(text="Texture")
        entry = next((s for s in read_registry(root) if s["group"] == body["tt_group"]
                      and s["name"] == body["tt_sprite"]), None) if root else None
        shown = mesh_texture_image(body)
        shown = image_texture_name(shown) if shown is not None else ""
        row = box.row(align=True)
        row.operator(ShowEventTexture.bl_idname, text="usual", depress=shown == original).texture = original
        for texture, event in (entry["textures"][0] if entry else []):
            if event:
                row.operator(ShowEventTexture.bl_idname, text=event, depress=shown == texture).texture = texture
        col = box.column(align=True)
        col.operator(NewEventTexture.bl_idname, icon="BRUSH_DATA")
        col.operator(SaveEventTexture.bl_idname, icon="EXPORT")
        col.operator(RemoveEventTexture.bl_idname, icon="TRASH")
        box = layout.box()
        box.label(text="Props in the registry")
        for obj in building_props(body):
            row = box.row(align=True)
            row.prop(obj, "hide_viewport", text=obj["tt_sprite"] + (f" ({obj['tt_event']})" if obj.get("tt_event") else ""),
                     toggle=True, invert_checkbox=True)
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH")
            remove.group, remove.sprite = body["tt_group"], obj["tt_sprite"]
        fresh = new_props(context, body)
        box.label(text=f"New: {', '.join(o.name for o in fresh)}" if fresh else "Select meshes to add as props",
                  icon="ADD" if fresh else "INFO")
        for o in fresh:
            if mesh_texture_image(o) is None:
                row = box.row()
                row.alert = True
                row.operator(MakeTexture.bl_idname, text=f"Make A Texture For {o.name}", icon="TEXTURE").target = o.name
        box.operator(SaveProps.bl_idname, icon="EXPORT")
        if wm.tt_checked and wm.tt_checks:
            box = layout.box()
            for check in wm.tt_checks:
                box.label(text=check.name, icon=CHECK_ICONS.get(check.level, "INFO"))


def skin_body(context):
    """The unit or building loaded from the Models list, and its registry entry: what a skin stands in for."""
    root = repo_root(context)
    body = next((o for o in bpy.data.objects if o.get(BROWSER_TAG) and o.type == "MESH" and o.get("tt_group")
                 and o.get("tt_source") and not o.get("tt_slot") and not o.get("tt_detail")), None)
    if body is None or not root:
        return None, None
    registry = read_registry(root)
    entry = next((s for s in registry if s["group"] == body["tt_group"] and s["name"] == body["tt_sprite"]), None)
    if entry is None or entry["skin"] or sprite_category(registry, entry) not in ("UNITS", "BUILDINGS"):
        return None, None
    return body, entry


def sprite_skins(registry, entry):
    return [s for s in registry if s["group"] == entry["group"] and s["skin"] and s["replaces"] == entry["name"]]


def body_rig(body):
    return body.parent if body.parent is not None and body.parent.type == "ARMATURE" else None


def level_textures(entry, level):
    """A model's texture list without its event textures: one per tier."""
    return [t for t, event in entry["textures"][min(level, len(entry["textures"]) - 1)] if not event]


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
            o["tt_texture"], o["tt_skin"] = ",".join(textures), skin["name"]
        elif o.get("tt_skin"):
            o["tt_texture"] = o.pop("tt_stock_texture")
            del o["tt_skin"]
    if skin is None:
        for o, (_, digest) in export_texts(context, arm, levels).items():
            o["tt_export_hash"] = digest
    apply_team_preview(context)


class ShowSkin(bpy.types.Operator):
    """Show the loaded model in this skin, every detail level. Stock shows its own files again and drops edits"""
    bl_idname = "object.tt_show_skin"
    bl_label = "Preview Skin"
    bl_options = {"REGISTER", "UNDO"}
    sprite: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        body, entry = skin_body(context)
        if body is None:
            return {"CANCELLED"}
        skin = next((s for s in sprite_skins(read_registry(repo_root(context)), entry) if s["name"] == self.sprite),
                    None)
        if self.sprite and skin is None:
            self.report({"ERROR"}, f"No skin sprite named {self.sprite} replaces {entry['name']}")
            return {"CANCELLED"}
        show_skin(context, body, entry, skin)
        return {"FINISHED"}


def skin_level_suffix(level):
    return "" if level == 0 else "_lo" if level == 1 else f"_lo{level}"


class SaveSkin(bpy.types.Operator):
    """Save the loaded model's look as a skin: a player who has it sees it on all of these units or buildings.
    A detail level whose mesh you did not change keeps using the stock file. The stock files are never written,
    and the model shows its stock look again afterwards"""
    bl_idname = "object.tt_save_skin"
    bl_label = "Save As Skin"

    @classmethod
    def poll(cls, context):
        return skin_body(context)[0] is not None

    def execute(self, context):
        root = repo_root(context)
        body, entry = skin_body(context)
        skin = context.window_manager.tt_skin_name.strip()
        group, sprite = entry["group"], entry["name"]
        name = f"{sprite}_{skin}"
        if not re.fullmatch(r"[A-Za-z0-9_]+", skin):
            self.report({"ERROR"}, "Name the skin with letters, digits and underscores")
            return {"CANCELLED"}
        if body.get("tt_skin"):
            self.report({"ERROR"}, f"Showing {body['tt_skin']}: press Stock, then make your changes")
            return {"CANCELLED"}
        registry = read_registry(root)
        if any(s["group"] == group and s["name"] == name for s in registry) or \
                any(s["skin"] == skin for s in sprite_skins(registry, entry)):
            self.report({"ERROR"}, f"{group} already has a sprite named {name} or a {skin} skin of {sprite}")
            return {"CANCELLED"}
        arm = body_rig(body)
        tier = arm.get("tt_tier", 0) if arm is not None else 0
        levels = model_levels(body)
        exports = export_texts(context, arm, levels)
        findings = [(level, f"{o.name}: {text}") for o in levels for level, text in check_mesh(o, False, 0)]
        paths, textures, images = {}, {}, {}
        for level, o in enumerate(levels):
            paths[o] = o["tt_source"]
            if exports[o][1] != o.get("tt_export_hash"):
                paths[o] = os.path.join(os.path.dirname(o["tt_source"]), name + skin_level_suffix(level) + ".xml")
                if os.path.exists(paths[o]):
                    findings.append(("ERROR", f"{o.name}: {os.path.basename(paths[o])} is already on disk"))
            textures[o] = level_textures(entry, level)
            image = mesh_texture_image(o)
            if image is None or not textures[o]:
                continue
            at = min(tier, len(textures[o]) - 1)
            texture = image_texture_name(image)
            if texture == textures[o][at] and image.is_dirty:
                texture = f"{texture}_{skin}"  # paint on the stock texture goes to a copy
            if texture == textures[o][at]:
                continue
            target = models_texture_path(root, texture)
            source = os.path.normcase(os.path.abspath(bpy.path.abspath(image.filepath))) if image.filepath else ""
            if texture not in images and os.path.isfile(target) and \
                    (image.is_dirty or source != os.path.normcase(os.path.abspath(target))):
                findings.append(("ERROR", f"{o.name}: {texture}.png is already in assets/textures/models"))
            images[texture] = (o, image, textures[o][at])
            textures[o][at] = texture
        if not findings and all(paths[o] == o["tt_source"] and textures[o] == level_textures(entry, level)
                                for level, o in enumerate(levels)):
            findings.append(("ERROR", "nothing differs from the stock model: change the mesh or its texture first"))
        errors = store_findings(context, findings)
        if errors:
            self.report({"ERROR"}, f"Not saved: {errors} problem(s): " +
                        "; ".join(text for level, text in findings if level == "ERROR"))
            return {"CANCELLED"}

        changed = [o for o in levels if paths[o] != o["tt_source"]]
        stock_attributes = {o: o["tt_file_texture"] for o in changed if o.get("tt_file_texture")}
        for o in stock_attributes:
            o["tt_file_texture"] = ",".join(textures[o])
        try:
            for o, (text, _) in export_texts(context, arm, changed).items():
                write_text(paths[o], text)
        finally:
            for o, attribute in stock_attributes.items():
                o["tt_file_texture"] = attribute
        for texture, (o, image, _) in images.items():
            if image_texture_name(image) == texture:
                ensure_texture_in_repo(root, o, texture)
            else:
                save_png(image, models_texture_path(root, texture))
                image.reload()
        # A new texture on a stock tier keeps that tier's team decal unless it has its own.
        stock = {texture: replaced for texture, (_, _, replaced) in images.items()}
        geometry = os.path.join(root, GEOMETRY_DIR)
        models = [(os.path.relpath(paths[o], geometry).replace(os.sep, "/"),
                   [(t, team_attribute(root, t, False) or team_attribute(root, stock.get(t, t), False))
                    for t in textures[o]]) for o in levels]
        attrs = [("name", name), ("skin", skin), ("replaces", sprite)] + ([("base", sprite)] if arm is not None else [])
        append_registry_entries(os.path.join(root, REGISTRY_FILE), group, [(name, sprite_text(attrs, models))])
        show_skin(context, body, entry, None)
        refresh_units(context)
        self.report({"INFO"}, f"Saved skin {skin} of {group} / {sprite}: {len(changed)} new mesh(es), "
                              f"{len(images)} new texture(s)")
        return {"FINISHED"}


class VIEW3D_PT_tt_skins(bpy.types.Panel):
    bl_label = "Skins"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return skin_body(context)[0] is not None

    def draw(self, context):
        layout = self.layout
        body, entry = skin_body(context)
        shown = body.get("tt_skin", "")
        layout.operator(ShowSkin.bl_idname, text="Stock", depress=not shown).sprite = ""
        for skin in sprite_skins(read_registry(repo_root(context)), entry):
            row = layout.row(align=True)
            row.operator(ShowSkin.bl_idname, text=skin["skin"], icon="HIDE_OFF",
                         depress=shown == skin["name"]).sprite = skin["name"]
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH")
            remove.group, remove.sprite = skin["group"], skin["name"]
        layout.prop(context.window_manager, "tt_skin_name")
        layout.operator(SaveSkin.bl_idname, icon="EXPORT")


def subset_record(record, face_indices):
    """New record holding only these triangles, with vertices compacted and sharing kept."""
    out = MeshRecord()
    out.loop_uv2s = [] if record.loop_uv2s is not None else None
    remap = {}
    for fi in face_indices:
        new_face = []
        for vi in record.faces[fi]:
            ni = remap.get(vi)
            if ni is None:
                ni = remap[vi] = len(out.verts)
                out.verts.append(record.verts[vi])
                out.skins.append(list(record.skins[vi]))
            new_face.append(ni)
        out.faces.append(tuple(new_face))
        for li in range(fi * 3, fi * 3 + 3):
            out.loop_uvs.append(record.loop_uvs[li])
            out.loop_cols.append(record.loop_cols[li])
            out.loop_normals.append(record.loop_normals[li])
            if out.loop_uv2s is not None:
                out.loop_uv2s.append(record.loop_uv2s[li])
    return out


def replace_mesh_data(obj, record, name):
    materials = list(obj.data.materials)
    old = obj.data
    obj.data = build_mesh(name, record)
    for m in materials:
        obj.data.materials.append(m)
    set_vertex_groups(obj, record.skins)
    if old.users == 0:
        bpy.data.meshes.remove(old)


def vertex_group_search(self, context, edit_text):
    o = context.active_object
    return [g.name for g in o.vertex_groups] if o is not None and o.type == "MESH" else []


class SplitByBone(bpy.types.Operator):
    """Move the faces weighted to one bone into a new object, turning a baked-in held item into an attachment"""
    bl_idname = "object.tt_split_by_bone"
    bl_label = "Split Mesh by Bone"
    bl_options = {"REGISTER", "UNDO"}
    bone: StringProperty(name="Bone", default="", search=vertex_group_search,
                         description="Vertex group (bone) whose faces move to the new object")
    threshold: FloatProperty(name="Min Weight", default=0.5, min=0.0, max=1.0,
                             description="A face moves when every corner carries at least this weight on the bone")

    @classmethod
    def poll(cls, context):
        o = context.active_object
        if o is not None and o.type == "MESH" and len(o.vertex_groups) > 0:
            return True
        cls.poll_message_set("Click the unit's mesh, not its bones" if o is not None and o.type == "ARMATURE"
                             else "Needs a mesh with bone weights")
        return False

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        o = context.active_object
        group = o.vertex_groups.get(self.bone)
        if group is None:
            self.report({"ERROR"}, f"{o.name} has no vertex group '{self.bone}'")
            return {"CANCELLED"}
        record = mesh_record_from_mesh(o.data, o, Matrix.Identity(4), False, None, True)
        weight = [next((g.weight for g in v.groups if g.group == group.index), 0.0) for v in o.data.vertices]
        part_faces = [i for i, face in enumerate(record.faces) if all(weight[vi] >= self.threshold for vi in face)]
        if not part_faces:
            self.report({"WARNING"}, f"No face has every corner weighted {self.threshold:g} or more to '{self.bone}'")
            return {"CANCELLED"}
        moving = set(part_faces)
        rest_faces = [i for i in range(len(record.faces)) if i not in moving]

        part_name = f"{o.name}_{self.bone.split()[-1]}"
        part_record = subset_record(record, part_faces)
        part = bpy.data.objects.new(part_name, build_mesh(part_name, part_record))
        for m in o.data.materials:
            part.data.materials.append(m)
        context.collection.objects.link(part)
        part.matrix_world = o.matrix_world.copy()
        if o.parent is not None:
            part.parent = o.parent
            part.parent_type = o.parent_type
            part.parent_bone = o.parent_bone
            part.matrix_parent_inverse = o.matrix_parent_inverse.copy()
        for modifier in o.modifiers:
            if modifier.type == "ARMATURE":
                part.modifiers.new(modifier.name, "ARMATURE").object = modifier.object
        for key in ("tt_texture",):
            if key in o:
                part[key] = o[key]
        set_vertex_groups(part, part_record.skins)

        replace_mesh_data(o, subset_record(record, rest_faces), o.data.name)

        for other in context.selected_objects:
            other.select_set(False)
        part.select_set(True)
        context.view_layer.objects.active = part
        self.report({"INFO"}, f"Moved {len(part_faces)} of {len(record.faces)} faces to {part.name}")
        return {"FINISHED"}


def menu_import(self, context):
    self.layout.operator(ImportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")
    self.layout.operator(ImportTTSkeleton.bl_idname, text="Tribal Trouble Skeleton / Animation (.xml)")


def menu_export(self, context):
    self.layout.operator(ExportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")
    self.layout.operator(ExportTTSkeleton.bl_idname, text="Tribal Trouble Skeleton / Animation (.xml)")


def menu_object(self, context):
    self.layout.separator()
    self.layout.operator(SplitByBone.bl_idname)


classes = (TTPreferences, ImportTTMesh, ExportTTMesh, SplitByBone, ImportTTSkeleton, ExportTTSkeleton,
           TTAttachmentSlot, TTUnitEntry, TT_UL_units, RefreshUnits, LoadUnit, PublishModel, ShowItem, ExportToRepo, AddToRegistry, RegisterModel, TTCheck, SetClip, SetTier, MaterialPreview, Preflight,
           RemoveFromRegistry, UpdateAddon, NewEventTexture, ShowEventTexture, SaveEventTexture, RemoveEventTexture,
           SaveProps, VIEW3D_PT_tt_building, ShowSkin, SaveSkin, VIEW3D_PT_tt_skins, NewClip, SaveClip, DeleteClip,
           SetupAttachments, ExportAttachments, CopyRegistrySnippet, SaveItems, MakeTexture, OwnTexture, PutOnBone, PaintItem, DonePainting,
           TT_UL_items, VIEW3D_PT_tt_units,
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
    wm.tt_auto_load = BoolProperty(name="Load On Click", default=True,
                                   description="Load a model as soon as it is picked in the list")
    wm.tt_checks = CollectionProperty(type=TTCheck)
    wm.tt_detail = EnumProperty(name="Detail", items=DETAIL_ITEMS, update=detail_update,
                                description="Which of the model's meshes shows: the close up one or the one the game "
                                            "draws from far away")
    wm.tt_checked = BoolProperty()
    wm.tt_team_preview = BoolProperty(name="Team Color", default=False, update=team_preview_update,
                                      description="Blend the player's color in through the team decal, as in game")
    wm.tt_team_color = FloatVectorProperty(name="Team Color", subtype="COLOR", size=3, min=0.0, max=1.0,
                                           default=(0.8, 0.1, 0.1), update=team_preview_update)
    wm.tt_item_filter = EnumProperty(name="Show", items=slot_filter_items)
    wm.tt_item_index = IntProperty()
    wm.tt_item_search = StringProperty(name="Search", options={"TEXTEDIT_UPDATE"},
                                       description="Show only the items whose name contains this")
    wm.tt_new_point = EnumProperty(name="Goes On", items=point_items,
                                   description="The part of the unit your new mesh follows")
    wm.tt_event = StringProperty(name="Event", description="Blank means all year. With a name such as halloween, "
                                                           "new props and the texture only show during that event")
    wm.tt_skin_name = StringProperty(name="Skin Name", description="Letters, digits and underscores. The skin's "
                                                                   "sprite is <model>_<skin name>")
    bpy.app.handlers.load_post.append(refresh_units_on_load)
    bpy.app.timers.register(refresh_units_on_load, first_interval=0.5)
    bpy.types.TOPBAR_MT_file_import.append(menu_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_export)
    bpy.types.VIEW3D_MT_object.append(menu_object)
    if hasattr(bpy.types, "UI_MT_button_context_menu"):
        bpy.types.UI_MT_button_context_menu.append(clip_button_menu)


def unregister():
    if hasattr(bpy.types, "UI_MT_button_context_menu"):
        bpy.types.UI_MT_button_context_menu.remove(clip_button_menu)
    bpy.types.VIEW3D_MT_object.remove(menu_object)
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_export)
    bpy.app.handlers.load_post.remove(refresh_units_on_load)
    del bpy.types.Object.tt_attachments
    for name in ("tt_repo_root", "tt_units", "tt_unit_index", "tt_category", "tt_auto_load", "tt_checks",
                 "tt_checked", "tt_team_preview", "tt_team_color", "tt_event", "tt_new_point", "tt_item_filter",
                 "tt_item_index", "tt_detail", "tt_item_search", "tt_skin_name"):
        delattr(bpy.types.WindowManager, name)
    for cls in classes:
        bpy.utils.unregister_class(cls)


if __name__ == "__main__":
    register()

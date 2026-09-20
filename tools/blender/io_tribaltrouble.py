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
import xml.etree.ElementTree as ET
from xml.sax.saxutils import quoteattr

import bpy
from bpy_extras.io_utils import ImportHelper, ExportHelper
from bpy.props import (StringProperty, BoolProperty, CollectionProperty, EnumProperty, PointerProperty, FloatProperty,
                       IntProperty)
from mathutils import Matrix, Vector

bl_info = {
    "name": "Tribal Trouble Mesh (.xml)",
    "author": "Tribal Trouble tooling",
    "version": (1, 8, 0),
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


def get_atlas_material(texture, image_path):
    mat_name = "tt_" + texture
    mat = bpy.data.materials.get(mat_name)
    if mat is not None:
        return mat
    mat = bpy.data.materials.new(mat_name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    tex_node = mat.node_tree.nodes.new("ShaderNodeTexImage")
    tex_node.image = bpy.data.images.load(image_path, check_existing=True)
    tex_node.location = (-350, 300)
    if bsdf is not None:
        mat.node_tree.links.new(bsdf.inputs["Base Color"], tex_node.outputs["Color"])
        mat.node_tree.links.new(bsdf.inputs["Alpha"], tex_node.outputs["Alpha"])
        bsdf.inputs["Roughness"].default_value = 1.0
    try:
        mat.blend_method = "CLIP"
    except AttributeError:
        pass
    return mat


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
    for tri in me.loop_triangles:
        record.faces.append(tuple(tri.vertices))
        for li, vi in zip(tri.loops, tri.vertices):
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


def material_image_name(objs):
    """Fall back to the name of an image used by the objects' materials, without extension."""
    for o in objs:
        for slot in o.material_slots:
            mat = slot.material
            if mat is None or not mat.use_nodes:
                continue
            for node in mat.node_tree.nodes:
                if node.type == "TEX_IMAGE" and node.image is not None:
                    return os.path.splitext(node.image.name)[0]
    return ""


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


def write_mesh_xml(objs, bones, filepath, texture, flip_v, depsgraph, use_groups=True):
    """Write objs, evaluated and in world space, as one game mesh file.

    bones[i] is a bone name to skin every vertex of objs[i] to rigidly, or None to use its vertex groups.
    """
    lines = [XML_HEADER, "", DOCTYPE, ""]
    lines.append(f'<mesh texture="{texture}">' if texture else "<mesh>")
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
    write_lines(filepath, lines)


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
                texture = self.texture or o.get("tt_texture", "")
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


def write_lines(filepath, lines):
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


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
            except ET.ParseError as e:
                self.report({"WARNING"}, f"{os.path.basename(path)}: XML parse error: {e}")
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

        for path in clips:
            name = os.path.splitext(os.path.basename(path))[0]
            action = apply_clip(context, arm, name, read_animation(path))
            action["tt_clip"] = os.path.basename(path)

        context.view_layer.objects.active = arm
        arm.select_set(True)
        self.report({"INFO"}, f"{arm.name}: {len(arm.data.bones)} bones, {len(clips)} clip(s), {bound} mesh(es) bound")
        return {"FINISHED"}


def skeleton_bone_search(self, context, edit_text):
    path = self.filepath
    if not path.lower().endswith(".xml") or not os.path.isfile(path):
        return []
    try:
        return sorted(read_skeleton(path)[1])
    except (ET.ParseError, ValueError, AttributeError):
        return []


class SnapToBone(bpy.types.Operator, ImportHelper):
    """Move the selected objects to a bone's rest position from a skeleton file, for authoring attachments in bind pose"""
    bl_idname = "object.tt_snap_to_bone"
    bl_label = "Snap to Tribal Trouble Bone"
    bl_options = {"REGISTER", "UNDO"}
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*_skeleton.xml", options={"HIDDEN"})
    bone: StringProperty(name="Bone", default="", search=skeleton_bone_search,
                         description="Bone from the chosen skeleton file")
    align_rotation: BoolProperty(name="Align Rotation", default=False,
                                 description="Also rotate to the bone's rest orientation. Biped bones point X "
                                             "along the bone, so an upright item usually wants this off")
    set_bone: BoolProperty(name="Set tt_bone", default=True,
                           description="Store the bone on the object so export skins to it")

    def execute(self, context):
        objs = list(context.selected_objects)
        if not objs and context.active_object is not None:
            objs = [context.active_object]
        if not objs:
            self.report({"ERROR"}, "Select at least one object to snap")
            return {"CANCELLED"}
        try:
            _, rest = read_skeleton(self.filepath)
        except (ET.ParseError, ValueError, AttributeError, OSError) as e:
            self.report({"ERROR"}, f"{os.path.basename(self.filepath)}: {e}")
            return {"CANCELLED"}
        m = rest.get(self.bone)
        if m is None:
            self.report({"ERROR"}, f"No bone '{self.bone}' in {os.path.basename(self.filepath)}")
            return {"CANCELLED"}

        for o in objs:
            world = o.matrix_world.copy()
            if self.align_rotation:
                scale = Matrix.Diagonal(world.to_scale()).to_4x4()
                world = m.to_3x3().to_4x4() @ scale
            world.translation = m.to_translation()
            o.matrix_world = world
            if self.set_bone:
                o["tt_bone"] = self.bone
        self.report({"INFO"}, f"Snapped {len(objs)} object(s) to '{self.bone}'")
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
            for action in armature_actions(arm):
                path = os.path.join(out_dir, action.name + ".xml")
                write_animation_xml(context, arm, action, path)
                written.append(action.name + ".xml")
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
    obj.hide_set(not visible)
    obj.hide_render = not visible


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
    obj.hide_set(False)
    obj.hide_render = False


def attachment_obj_poll(self, obj):
    return obj.type == "MESH" and obj != self.id_data


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
        self.obj.hide_set(not self.visible)
        self.obj.hide_render = not self.visible


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


def registry_entries(context, arm, base):
    """(sprite name, geometry.xml text) for every visible new attachment in the panel's point slots."""
    root = repo_root(context)
    entries = []
    for slot in arm.tt_attachments:
        if slot.obj is None or not slot.visible:
            continue
        obj = slot.obj
        game_slot = GAME_SLOTS.get(slot.point, slot.point.lower())
        # Only atlases imported from the game are known to have a team decal next to them.
        atlas = obj.get("tt_texture")
        texture = atlas or material_image_name([obj]) or "TEXTURE"
        team = f' team="{texture}_team"' if atlas else ""
        model = f"misc/{obj.name}.xml"
        if root and arm.get("tt_skeleton"):
            model = os.path.relpath(os.path.join(os.path.dirname(arm["tt_skeleton"]), obj.name + ".xml"),
                                    os.path.join(root, GEOMETRY_DIR)).replace(os.sep, "/")
        name = f"{base}_{obj.name}"
        entries.append((name, "\n".join([
            f'        <sprite name="{name}" base="{base}" slot="{game_slot}">',
            '            <model r="90" g="60" b="30">',
            f"                {model}",
            f'                <texture name="{texture}"{team}/>',
            "            </model>",
            "        </sprite>",
        ])))
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


class VIEW3D_PT_tt_attachments(bpy.types.Panel):
    bl_label = "Attachments"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def draw(self, context):
        arm = active_armature(context)
        layout = self.layout
        layout.label(text=arm.name, icon="ARMATURE_DATA")
        if not arm.tt_attachments:
            layout.operator(SetupAttachments.bl_idname)
            return
        for slot in arm.tt_attachments:
            row = layout.row(align=True)
            row.label(text=POINT_LABELS.get(slot.point, slot.point))
            row.prop(slot, "obj", text="")
            row.prop(slot, "visible", text="", icon="HIDE_OFF" if slot.visible else "HIDE_ON")
        for game_slot, items in sorted(unit_items(arm).items()):
            box = layout.box()
            box.label(text=f"In the registry: {game_slot}")
            for obj in sorted(items, key=lambda o: o.name):
                box.operator(ShowItem.bl_idname, text=obj["tt_sprite"], depress=not obj.hide_get(),
                             icon="HIDE_ON" if obj.hide_get() else "HIDE_OFF").item = obj.name
        col = layout.column(align=True)
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
                "skeleton": skeleton.text.strip() if skeleton is not None and skeleton.text else "",
                "models": [(m.text or "").strip() for m in sprite.findall("model")],
                "clips": [(a.text or "").strip() for a in sprite.findall("animation")],
            })
    return sprites


def refresh_units(context):
    wm = context.window_manager
    wm.tt_units.clear()
    root = repo_root(context)
    if not root:
        return 0
    sprites = [s for s in read_registry(root) if not s["base"] and (s["skeleton"] or not wm.tt_units_only)]
    for sprite in sorted(sprites, key=lambda s: (s["group"], s["name"])):
        item = wm.tt_units.add()
        item.name = f"{sprite['group']} / {sprite['name']}"
        item.group = sprite["group"]
        item.sprite = sprite["name"]
    return len(wm.tt_units)


def clear_browser_objects():
    for obj in [o for o in bpy.data.objects if o.get(BROWSER_TAG)]:
        data = obj.data
        bpy.data.objects.remove(obj, do_unlink=True)
        if data is not None and data.users == 0:
            (bpy.data.meshes if isinstance(data, bpy.types.Mesh) else bpy.data.armatures).remove(data)
    for action in [a for a in bpy.data.actions if a.get(BROWSER_TAG) and a.users == 0]:
        bpy.data.actions.remove(action)


def unit_items(arm):
    """Registry attachments loaded with this unit, grouped by game slot."""
    slots = {}
    for obj in bpy.data.objects:
        if obj.get("tt_slot") and obj.parent == arm:
            slots.setdefault(obj["tt_slot"], []).append(obj)
    return slots


def set_item_visible(obj, visible):
    obj.hide_set(not visible)
    obj.hide_render = not visible


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
    body = import_mesh_file(context, os.path.join(geometry, entry["models"][0]), False, True, quiet)
    if body is None:
        return None
    body[BROWSER_TAG] = True
    arm = None
    if entry["skeleton"]:
        arm = armature_from_file(context, os.path.join(geometry, entry["skeleton"]))
        arm[BROWSER_TAG] = True
        bind_meshes(arm, [body])
        idle = None
        for clip in entry["clips"]:
            clip_name = os.path.splitext(os.path.basename(clip))[0]
            action = apply_clip(context, arm, clip_name, read_animation(os.path.join(geometry, clip)))
            action["tt_clip"] = os.path.basename(clip)
            action[BROWSER_TAG] = True
            if idle is None or "idle" in clip_name:
                idle = action
        if idle is not None:
            assign_action(arm, idle)

    items = 0
    for sprite in registry:
        if arm is None or sprite["group"] != group or sprite["base"] != name or not sprite["slot"]:
            continue
        path = os.path.join(geometry, sprite["models"][0])
        obj = import_mesh_file(context, path, False, True, quiet)
        if obj is None:
            continue
        obj[BROWSER_TAG] = True
        obj["tt_slot"] = sprite["slot"]
        obj["tt_sprite"] = sprite["name"]
        obj["tt_source"] = path
        bones = [g.name for g in obj.vertex_groups]
        if len(bones) == 1 and bones[0] in arm.data.bones:
            attach_object(arm, obj, bones[0], sprite["default"])
        else:
            bind_meshes(arm, [obj])
            set_item_visible(obj, sprite["default"])
        items += 1

    for o in context.selected_objects:
        o.select_set(False)
    active = arm if arm is not None else body
    context.view_layer.objects.active = active
    active.select_set(True)
    report({"INFO"}, f"{group} / {name}: {len(entry['clips'])} clip(s), {items} registry attachment(s)")
    return active


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
        self.layout.prop(self, "repo_root")


class TTUnitEntry(bpy.types.PropertyGroup):
    group: StringProperty()
    sprite: StringProperty()


class TT_UL_units(bpy.types.UIList):
    def draw_item(self, context, layout, data, item, icon, active_data, active_property, index):
        layout.label(text=item.name, icon="ARMATURE_DATA" if item.group in ("vikings", "natives") else "MESH_CUBE")


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


class ShowItem(bpy.types.Operator):
    """Show this registry attachment and hide the others in its slot; pick the shown one again to hide it"""
    bl_idname = "object.tt_show_item"
    bl_label = "Show Attachment"
    bl_options = {"REGISTER", "UNDO"}
    item: StringProperty()

    def execute(self, context):
        arm = active_armature(context)
        chosen = bpy.data.objects.get(self.item)
        if arm is None or chosen is None:
            return {"CANCELLED"}
        show = chosen.hide_get()
        for obj in unit_items(arm).get(chosen["tt_slot"], []):
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
        arm = active_armature(context)
        unit_dir = os.path.dirname(arm["tt_skeleton"])
        objs = visible_attachments(arm) + [o for items in unit_items(arm).values() for o in items
                                           if not o.hide_get()]
        if not objs:
            self.report({"ERROR"}, "Nothing visible to export")
            return {"CANCELLED"}
        previous = arm.data.pose_position
        arm.data.pose_position = "REST"
        context.view_layer.update()
        try:
            depsgraph = context.evaluated_depsgraph_get()
            for o in objs:
                path = o.get("tt_source") or os.path.join(unit_dir, o.name + ".xml")
                texture = o.get("tt_texture") or material_image_name([o])
                write_mesh_xml([o], [o.get("tt_bone")], path, texture, False, depsgraph)
        finally:
            arm.data.pose_position = previous
            context.view_layer.update()
        self.report({"INFO"}, f"Exported {len(objs)} file(s) into {unit_dir}")
        return {"FINISHED"}


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
        return arm is not None and bool(visible_attachments(arm)) and bool(repo_root(context))

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


class VIEW3D_PT_tt_units(bpy.types.Panel):
    bl_label = "Models"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    def draw(self, context):
        layout = self.layout
        wm = context.window_manager
        holder = root_holder(context)
        layout.prop(holder, "repo_root" if hasattr(holder, "repo_root") else "tt_repo_root", text="Repo")
        if not repo_root(context):
            layout.label(text="Pick your tribaltrouble folder", icon="INFO")
            return
        row = layout.row(align=True)
        row.operator(RefreshUnits.bl_idname, icon="FILE_REFRESH")
        row.prop(wm, "tt_units_only", toggle=True)
        row.prop(wm, "tt_auto_load", toggle=True)
        layout.template_list("TT_UL_units", "", wm, "tt_units", wm, "tt_unit_index", rows=10)
        layout.operator(LoadUnit.bl_idname, icon="IMPORT")


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
        return o is not None and o.type == "MESH" and len(o.vertex_groups) > 0

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
    self.layout.operator(SnapToBone.bl_idname)
    self.layout.operator(SplitByBone.bl_idname)


classes = (TTPreferences, ImportTTMesh, ExportTTMesh, SnapToBone, SplitByBone, ImportTTSkeleton, ExportTTSkeleton,
           TTAttachmentSlot, TTUnitEntry, TT_UL_units, RefreshUnits, LoadUnit, ShowItem, ExportToRepo, AddToRegistry,
           SetupAttachments, ExportAttachments, CopyRegistrySnippet, VIEW3D_PT_tt_units, VIEW3D_PT_tt_attachments)


def register():
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.Object.tt_attachments = CollectionProperty(type=TTAttachmentSlot)
    wm = bpy.types.WindowManager
    wm.tt_repo_root = StringProperty(name="Repo Folder", subtype="DIR_PATH", update=root_update)
    wm.tt_units = CollectionProperty(type=TTUnitEntry)
    wm.tt_unit_index = IntProperty(update=unit_index_update)
    wm.tt_units_only = BoolProperty(name="Units Only", default=True, update=root_update,
                                    description="List only models with a skeleton")
    wm.tt_auto_load = BoolProperty(name="Load On Click", default=True,
                                   description="Load a model as soon as it is picked in the list")
    bpy.types.TOPBAR_MT_file_import.append(menu_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_export)
    bpy.types.VIEW3D_MT_object.append(menu_object)


def unregister():
    bpy.types.VIEW3D_MT_object.remove(menu_object)
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_export)
    del bpy.types.Object.tt_attachments
    for name in ("tt_repo_root", "tt_units", "tt_unit_index", "tt_units_only", "tt_auto_load"):
        delattr(bpy.types.WindowManager, name)
    for cls in classes:
        bpy.utils.unregister_class(cls)


if __name__ == "__main__":
    register()

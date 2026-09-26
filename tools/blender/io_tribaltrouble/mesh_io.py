"""Mesh, skeleton and clip XML: reading and writing them, and the meshes, armatures and actions built from them."""

import hashlib
import os
import xml.etree.ElementTree as ET
from xml.sax.saxutils import quoteattr

import bpy
import numpy as np
from mathutils import Matrix, Vector

from .textures import find_registry_texture, find_texture_image, get_atlas_material


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
    ("CHICKEN", "Chicken", ""),
)

# Bone names must match the skeleton file exactly (note the double space in "warrior  Head").
# Game slot an attachment point feeds; H in game cycles the "hat" slot. Other points use their own name.
GAME_SLOTS = {"HEAD": "hat", "PROP1": "weapon"}

ATTACHMENT_POINTS = {
    "HEAD": {"PEON": "peon Head", "VIKING_WARRIOR": "warrior  Head", "NATIVE_WARRIOR": "Head",
             "VIKING_CHIEFTAIN": "Head", "NATIVE_CHIEFTAIN": "Head", "CHICKEN": "chicken Head"},
    "BACK": {"PEON": "peon Spine2", "VIKING_WARRIOR": "warrior  Spine1", "NATIVE_WARRIOR": "Spine2",
             "VIKING_CHIEFTAIN": "Spine1", "NATIVE_CHIEFTAIN": "Spine2", "CHICKEN": "chicken Spine"},
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


def import_mesh_file(context, filepath, flip_v, load_textures, report, texture=None):
    """Build, link and select one mesh object from a mesh file; None when the file is not a mesh. A registry
    texture list, when given, wins over the file's own attribute, as it does in game."""
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
    obj["tt_texture"] = texture or root.get("texture") or find_registry_texture(filepath) or ""
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


def clip_keys(action):
    """Hash of every key of the action, to tell a clip edited since loading or publishing."""
    curves = getattr(action, "fcurves", None)
    if curves is None:
        curves = [c for layer in action.layers for strip in layer.strips for bag in strip.channelbags
                  for c in bag.fcurves]
    digest = hashlib.sha1()
    for curve in sorted(curves, key=lambda c: (c.data_path, c.array_index)):
        digest.update(f"{curve.data_path}[{curve.array_index}]".encode("utf-8"))
        for field in ("co", "handle_left", "handle_right"):
            values = np.empty(len(curve.keyframe_points) * 2, np.float32)
            curve.keyframe_points.foreach_get(field, values)
            digest.update(values.tobytes())
    return digest.hexdigest()


def armature_actions(arm):
    """Actions imported for this armature, or failing that the one currently assigned."""
    actions = [a for a in bpy.data.actions if a.get("tt_armature") == arm.name]
    if not actions and arm.animation_data is not None and arm.animation_data.action is not None:
        actions = [arm.animation_data.action]
    return actions


SHOWN_SCALE = 0.01


def shown_bones(frames):
    """Bones not shrunk to nothing on at least one frame; the game hides a held thing by scaling its bone to zero."""
    return sorted({bone for frame in frames for bone, m in frame.items()
                   if max(abs(s) for s in m.to_scale()) > SHOWN_SCALE})


def item_bones(obj):
    """The bones obj follows: the one it hangs off, or for an item that deforms with several, every bone its vertex
    groups weight, the one with the most weight first."""
    bone = obj.parent_bone if obj.parent_type == "BONE" and obj.parent_bone else obj.get("tt_bone")
    if bone:
        return [bone]
    arm = obj.parent if obj.parent is not None and obj.parent.type == "ARMATURE" else None
    if arm is None or obj.type != "MESH":
        return []
    names = [g.name for g in obj.vertex_groups]
    totals = {}
    for v in obj.data.vertices:
        for g in v.groups:
            if g.weight > 0.0 and names[g.group] in arm.data.bones:
                totals[names[g.group]] = totals.get(names[g.group], 0.0) + g.weight
    return sorted(totals, key=lambda b: -totals[b])


def item_clips(arm, obj):
    """The clips in which any of obj's bones shows, by name; None for an item on no bone."""
    bones = item_bones(obj)
    if not bones:
        return None
    return sorted((a for a in armature_actions(arm) if set(bones) & set(list(a.get("tt_shown_bones", ())))),
                  key=lambda a: a.name)


def item_hidden_here(arm, obj):
    """The clips obj shows in when the clip on the rig is not one of them; None otherwise."""
    if arm is None:
        return None
    current = arm.animation_data.action if arm.animation_data is not None else None
    clips = item_clips(arm, obj)
    if current is None or not clips or "tt_shown_bones" not in current or current in clips:
        return None
    return clips


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


def export_matrix(o):
    """World matrix with the armature at rest for bone-parented attachments, so scrubbing never leaks into a file."""
    parent = o.parent
    if parent is not None and parent.type == "ARMATURE" and o.parent_type == "BONE" \
            and o.parent_bone in parent.data.bones:
        rest_tail, _ = bone_tail_matrices(parent, o.parent_bone)
        return rest_tail @ o.matrix_parent_inverse @ o.matrix_basis
    return o.matrix_world


def clip_short_name(arm, action):
    """walk for an action called peon_walk on the peon rig; the action name otherwise."""
    stem = os.path.splitext(action.get("tt_clip") or action.name)[0]
    prefix = os.path.basename(arm.get("tt_skeleton", "")).replace("skeleton.xml", "")
    return stem[len(prefix):] if prefix and stem.startswith(prefix) and len(stem) > len(prefix) else stem


def item_point(arm, bone):
    return next((point for point, point_bone in resolve_attachment_bones(arm) if point_bone == bone), None)


def body_rig(body):
    return body.parent if body.parent is not None and body.parent.type == "ARMATURE" else None


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

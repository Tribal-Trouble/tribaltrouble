"""Mesh XML: reading and writing it, and the Blender meshes built from it."""

import os
import xml.etree.ElementTree as ET
from xml.sax.saxutils import quoteattr

import bpy
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


def mesh_record_from_mesh(me, obj, matrix, flip_v, rigid_bone, use_groups, bones_only=False):
    """Triangulated record of a Mesh in the space given by matrix.

    rigid_bone skins every vertex to that bone with weight 1. Otherwise vertex groups are used when use_groups is
    set, with bones_only just those named after a bone of the object's armature when it has one; vertices without
    any fall back to dummy_bone.
    """
    me.calc_loop_triangles()
    normal_matrix = matrix.to_3x3().inverted().transposed()
    uv_layers = list(me.uv_layers)
    uv_layer = uv_layers[0] if uv_layers else None
    uv2_layer = uv_layers[1] if len(uv_layers) > 1 else None
    col_layer = me.color_attributes.active_color if len(me.color_attributes) else None
    arms = rest_pose_armatures([obj]) if bones_only else []
    bones = {b.name for arm in arms for b in arm.data.bones} | {STATIC_BONE}
    group_names = [g.name if not arms or g.name in bones else None for g in obj.vertex_groups]

    record = MeshRecord()
    record.verts = [tuple(matrix @ v.co) for v in me.vertices]
    for v in me.vertices:
        if rigid_bone is not None:
            record.skins.append([(rigid_bone, 1.0)])
            continue
        # Raw group weights, as the source files store them; the game sums them as given.
        weights = [(group_names[g.group], g.weight) for g in v.groups
                   if use_groups and g.weight > 0.0 and group_names[g.group]]
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
            append_record_polygons(lines, mesh_record_from_mesh(me, o, export_matrix(o), flip_v, bone, use_groups,
                                                                bones_only=True))
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


def write_text(filepath, text):
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def write_lines(filepath, lines):
    write_text(filepath, "\n".join(lines) + "\n")


def bone_tail_matrices(arm, bone_name):
    """(rest, posed) world matrices of the bone tail, which is what Blender parents bone children to."""
    bone = arm.data.bones[bone_name]
    tail = Matrix.Translation((0.0, bone.length, 0.0))
    return arm.matrix_world @ bone.matrix_local @ tail, arm.matrix_world @ arm.pose.bones[bone_name].matrix @ tail


def export_matrix(o):
    """Armature space, as skeletons and clips are written, with the armature at rest for bone-parented attachments
    so scrubbing never leaks into a file; world space for a mesh without an armature."""
    parent = o.parent
    if parent is not None and parent.type == "ARMATURE" and o.parent_type == "BONE" \
            and o.parent_bone in parent.data.bones:
        rest_tail, _ = bone_tail_matrices(parent, o.parent_bone)
        return rest_tail @ o.matrix_parent_inverse @ o.matrix_basis
    arms = rest_pose_armatures([o])
    return arms[0].matrix_world.inverted() @ o.matrix_world if arms else o.matrix_world


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

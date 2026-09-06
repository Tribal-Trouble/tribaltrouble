"""Blender import/export addon for Tribal Trouble mesh XML files.

Install: Edit > Preferences > Add-ons > Install... > pick this file, enable it.
Import: File > Import > Tribal Trouble Mesh (.xml)
Export: File > Export > Tribal Trouble Mesh (.xml)  (exports the active object)

Static props (plants, rocks, a torch) have no skeleton: every vertex is skinned
to "dummy_bone" with weight 1, which is what the exporter writes by default.
Attachments (hats, held items) are the same thing skinned to one unit bone
instead: pick an attachment point in the export dialog, or set a tt_bone
property on the object. Model attachments in the unit's bind pose. Skinned unit
meshes import fine for viewing/editing; bone weights are preserved as vertex
groups but the exporter still writes a single bone per object, so do not
re-export animated units yet.

The game is Z-up like Blender, so no axis conversion is needed. If the texture
looks vertically flipped on an imported model, re-import with "Flip V" checked
and also check it on export.
"""

import os
import xml.etree.ElementTree as ET
from xml.sax.saxutils import quoteattr

import bpy
import bmesh
from bpy_extras.io_utils import ImportHelper, ExportHelper
from bpy.props import StringProperty, BoolProperty, CollectionProperty, EnumProperty
from mathutils import Matrix

bl_info = {
    "name": "Tribal Trouble Mesh (.xml)",
    "author": "Tribal Trouble tooling",
    "version": (1, 4, 0),
    "blender": (4, 0, 0),
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
            if self.import_one(context, path):
                imported += 1
        if imported == 0:
            return {"CANCELLED"}
        if len(paths) > 1:
            self.report({"INFO"}, f"Imported {imported} of {len(paths)} meshes")
        return {"FINISHED"}

    def import_one(self, context, filepath):
        try:
            root = ET.parse(filepath).getroot()
        except ET.ParseError as e:
            self.report({"WARNING"}, f"{os.path.basename(filepath)}: XML parse error: {e}")
            return False
        if root.tag != "mesh":
            self.report({"WARNING"}, f"{os.path.basename(filepath)}: not a Tribal Trouble mesh file (no <mesh> root)")
            return False

        name = os.path.splitext(os.path.basename(filepath))[0]
        verts = []           # deduped positions
        vert_index = {}      # (x, y, z) -> index
        faces = []
        loop_uvs = []
        loop_uv2s = []
        loop_cols = []
        skins = []           # per deduped vertex: list of (bone, weight)
        seen_faces = set()
        has_uv2 = False

        def add_vertex(vertex, pos, dedup):
            idx = vert_index.get(pos) if dedup else None
            if idx is None:
                idx = len(verts)
                if dedup:
                    vert_index[pos] = idx
                verts.append(pos)
                skins.append([(s.get("bone"), float(s.get("weight"))) for s in vertex])
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
                loop_uvs.append((float(vertex.get("u")), 1.0 - v if self.flip_v else v))
                if vertex.get("u2") is not None:
                    has_uv2 = True
                    v2 = float(vertex.get("v2"))
                    loop_uv2s.append((float(vertex.get("u2")), 1.0 - v2 if self.flip_v else v2))
                else:
                    loop_uv2s.append((0.0, 0.0))
                loop_cols.append((float(vertex.get("r")), float(vertex.get("g")),
                                  float(vertex.get("b")), float(vertex.get("a"))))
            faces.append(face)

        mesh = bpy.data.meshes.new(name)
        mesh.from_pydata(verts, [], faces)
        mesh.validate()
        if len(mesh.loops) != len(loop_uvs):
            self.report({"WARNING"}, f"{os.path.basename(filepath)}: Blender dropped "
                                     f"{len(faces) - len(mesh.polygons)} invalid faces; UVs and colours skipped")
            loop_uvs = loop_uv2s = loop_cols = []

        uv_layer = mesh.uv_layers.new(name="UVMap")
        for i, uv in enumerate(loop_uvs):
            uv_layer.data[i].uv = uv
        if has_uv2:
            uv2_layer = mesh.uv_layers.new(name="UVMap2")
            for i, uv in enumerate(loop_uv2s):
                uv2_layer.data[i].uv = uv
        col_layer = mesh.color_attributes.new(name="Col", type="FLOAT_COLOR", domain="CORNER")
        for i, col in enumerate(loop_cols):
            col_layer.data[i].color = col

        obj = bpy.data.objects.new(name, mesh)
        obj["tt_texture"] = root.get("texture") or find_registry_texture(filepath) or ""
        context.collection.objects.link(obj)

        # Preserve skinning as vertex groups for reference
        bones = sorted({bone for skin in skins for bone, _ in skin})
        groups = {bone: obj.vertex_groups.new(name=bone) for bone in bones}
        for idx, skin in enumerate(skins):
            for bone, weight in skin:
                groups[bone].add([idx], weight, "REPLACE")

        if self.load_textures:
            # Units can declare a comma-separated atlas list (one per weapon tier); preview with the first.
            primary = obj["tt_texture"].split(",")[0].strip()
            image_path = find_texture_image(filepath, primary)
            if image_path is not None:
                obj.data.materials.append(get_atlas_material(primary, image_path))

        context.view_layer.objects.active = obj
        obj.select_set(True)
        self.report({"INFO"}, f"Imported {len(verts)} verts, {len(faces)} tris, texture '{obj['tt_texture']}'")
        return True


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


def write_mesh_xml(objs, bones, filepath, texture, flip_v, depsgraph):
    """Merge objs in world space into one triangulated mesh and write it as game XML.

    bones[i] is the bone every vertex of objs[i] is skinned to with weight 1.
    """
    bm = bmesh.new()
    bone_layer = bm.verts.layers.int.new("tt_bone")
    for i, o in enumerate(objs):
        eval_obj = o.evaluated_get(depsgraph)
        me = eval_obj.to_mesh()
        me.transform(o.matrix_world)
        start = len(bm.verts)
        bm.from_mesh(me)
        eval_obj.to_mesh_clear()
        bm.verts.ensure_lookup_table()
        for v in bm.verts[start:]:
            v[bone_layer] = i
    bmesh.ops.triangulate(bm, faces=bm.faces)
    bm.normal_update()
    skin_lines = [f"                <skin bone={quoteattr(b)} weight=\"1\"/>" for b in bones]

    uv_layer = bm.loops.layers.uv.active
    # Import creates a float colour layer; older files may carry a byte colour layer.
    col_layer = bm.loops.layers.float_color.active or bm.loops.layers.color.active
    lines = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>', "", DOCTYPE, ""]
    lines.append(f'<mesh texture="{texture}">' if texture else "<mesh>")
    lines.append("    <polygons>")
    for face in bm.faces:
        lines.append("        <polygon>")
        for loop in face.loops:
            co = loop.vert.co
            # Smooth-shaded vertex normal; matches how the game lights static props
            n = loop.vert.normal if face.smooth else face.normal
            if uv_layer is not None:
                u, v = loop[uv_layer].uv
            else:
                u, v = 0.0, 0.0
            if flip_v:
                v = 1.0 - v
            if col_layer is not None:
                r, g, b, a = loop[col_layer]
            else:
                r, g, b, a = 1.0, 1.0, 1.0, 1.0
            lines.append(
                f'            <vertex x="{co.x:.6g}" y="{co.y:.6g}" z="{co.z:.6g}" '
                f'r="{r:.4g}" g="{g:.4g}" b="{b:.4g}" a="{a:.4g}" '
                f'nx="{n.x:.6g}" ny="{n.y:.6g}" nz="{n.z:.6g}" '
                f'u="{u:.6g}" v="{v:.6g}">')
            lines.append(skin_lines[loop.vert[bone_layer]])
            lines.append("            </vertex>")
        lines.append("        </polygon>")
    lines.append("    </polygons>")
    lines.append("</mesh>")
    bm.free()

    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


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

    def invoke(self, context, event):
        objs = [o for o in context.selected_objects if o.type == "MESH"]
        if len(objs) == 1:
            self.filepath = objs[0].name + ".xml"
        return super().invoke(context, event)

    def resolve_bone(self, obj):
        bone = obj.get("tt_bone")
        if bone:
            return bone
        if self.attach_point == "NONE":
            return STATIC_BONE
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


def read_skeleton(path):
    """Return (parents, rest): bone name -> parent name, and bone name -> model-space rest Matrix.

    The XML stores each 4x4 as m<column><row>, translation in m30..m32.
    """
    root = ET.parse(path).getroot()
    if root.tag != "skeleton":
        raise ValueError("not a Tribal Trouble skeleton file (no <skeleton> root)")
    parents = {b.get("name"): b.get("parent") for b in root.find("bones")}
    rest = {}
    for t in root.find("init_pose"):
        m = Matrix.Identity(4)
        for c in range(4):
            for r in range(4):
                m[r][c] = float(t.get(f"m{c}{r}"))
        rest[t.get("name")] = m
    return parents, rest


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


def menu_import(self, context):
    self.layout.operator(ImportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")


def menu_export(self, context):
    self.layout.operator(ExportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")


def menu_object(self, context):
    self.layout.separator()
    self.layout.operator(SnapToBone.bl_idname)


classes = (ImportTTMesh, ExportTTMesh, SnapToBone)


def register():
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.TOPBAR_MT_file_import.append(menu_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_export)
    bpy.types.VIEW3D_MT_object.append(menu_object)


def unregister():
    bpy.types.VIEW3D_MT_object.remove(menu_object)
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_export)
    for cls in classes:
        bpy.utils.unregister_class(cls)


if __name__ == "__main__":
    register()

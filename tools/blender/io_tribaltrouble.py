"""Blender import/export addon for Tribal Trouble mesh XML files.

Install: Edit > Preferences > Add-ons > Install... > pick this file, enable it.
Import: File > Import > Tribal Trouble Mesh (.xml)
Export: File > Export > Tribal Trouble Mesh (.xml)  (exports the active object)

Static props (plants, rocks, a torch) have no skeleton: every vertex is skinned
to "dummy_bone" with weight 1, which is what the exporter writes. Skinned unit
meshes import fine for viewing/editing; bone weights are preserved as vertex
groups but the exporter currently re-skins everything to dummy_bone, so use it
for static props, not for re-exporting animated units.

The game is Z-up like Blender, so no axis conversion is needed. If the texture
looks vertically flipped on an imported model, re-import with "Flip V" checked
and also check it on export.
"""

import os
import xml.etree.ElementTree as ET

import bpy
import bmesh
from bpy_extras.io_utils import ImportHelper, ExportHelper
from bpy.props import StringProperty, BoolProperty, CollectionProperty

bl_info = {
    "name": "Tribal Trouble Mesh (.xml)",
    "author": "Tribal Trouble tooling",
    "version": (1, 0, 0),
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


class ImportTTMesh(bpy.types.Operator, ImportHelper):
    bl_idname = "import_mesh.tt_xml"
    bl_label = "Import Tribal Trouble Mesh"
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    files: CollectionProperty(type=bpy.types.OperatorFileListElement, options={"HIDDEN", "SKIP_SAVE"})
    directory: StringProperty(subtype="DIR_PATH", options={"HIDDEN", "SKIP_SAVE"})
    flip_v: BoolProperty(name="Flip V", default=False,
                         description="Flip the vertical texture coordinate on import")

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
        has_uv2 = False

        for polygon in root.find("polygons"):
            face = []
            for vertex in polygon:
                pos = (float(vertex.get("x")), float(vertex.get("y")), float(vertex.get("z")))
                idx = vert_index.get(pos)
                if idx is None:
                    idx = len(verts)
                    vert_index[pos] = idx
                    verts.append(pos)
                    skins.append([(s.get("bone"), float(s.get("weight"))) for s in vertex])
                face.append(idx)
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
        obj["tt_texture"] = root.get("texture", "")
        context.collection.objects.link(obj)

        # Preserve skinning as vertex groups for reference
        bones = sorted({bone for skin in skins for bone, _ in skin})
        groups = {bone: obj.vertex_groups.new(name=bone) for bone in bones}
        for idx, skin in enumerate(skins):
            for bone, weight in skin:
                groups[bone].add([idx], weight, "REPLACE")

        context.view_layer.objects.active = obj
        obj.select_set(True)
        self.report({"INFO"}, f"Imported {len(verts)} verts, {len(faces)} tris, texture '{obj['tt_texture']}'")
        return True


class ExportTTMesh(bpy.types.Operator, ExportHelper):
    bl_idname = "export_mesh.tt_xml"
    bl_label = "Export Tribal Trouble Mesh"
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    texture: StringProperty(name="Texture", default="",
                            description="Texture atlas name (defaults to the object's tt_texture property)")
    flip_v: BoolProperty(name="Flip V", default=False,
                         description="Flip the vertical texture coordinate on export")

    def execute(self, context):
        obj = context.active_object
        if obj is None or obj.type != "MESH":
            self.report({"ERROR"}, "Select a mesh object to export")
            return {"CANCELLED"}

        texture = self.texture or obj.get("tt_texture", "")

        # Work on a triangulated evaluated copy so modifiers apply
        depsgraph = context.evaluated_depsgraph_get()
        bm = bmesh.new()
        bm.from_object(obj, depsgraph)
        bmesh.ops.triangulate(bm, faces=bm.faces)
        bm.normal_update()

        uv_layer = bm.loops.layers.uv.active
        col_layer = bm.loops.layers.color.active
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
                if self.flip_v:
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
                lines.append('                <skin bone="dummy_bone" weight="1"/>')
                lines.append("            </vertex>")
            lines.append("        </polygon>")
        lines.append("    </polygons>")
        lines.append("</mesh>")
        bm.free()

        with open(self.filepath, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lines) + "\n")
        self.report({"INFO"}, f"Exported {os.path.basename(self.filepath)}")
        return {"FINISHED"}


def menu_import(self, context):
    self.layout.operator(ImportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")


def menu_export(self, context):
    self.layout.operator(ExportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")


classes = (ImportTTMesh, ExportTTMesh)


def register():
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.TOPBAR_MT_file_import.append(menu_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_export)


def unregister():
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_export)
    for cls in classes:
        bpy.utils.unregister_class(cls)


if __name__ == "__main__":
    register()

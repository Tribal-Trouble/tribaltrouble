import bpy
import xml.etree.ElementTree as ET
from mathutils import Vector

XML_PATH = "/home/omar/projects/tribaltrouble/tt/geometry/vikings/peon/peon_mesh.xml"

mesh_tree = ET.parse(XML_PATH)
root = mesh_tree.getroot()

verts = []
faces = []
uvs = []

plys = root.find('polygons').findall('polygon')
face_offset = 0
for ply in plys:
    vs = ply.findall('vertex')
    f = ()
    for v in vs:
        x = float(v.get('x'))
        y = float(v.get('y'))
        z = float(v.get('z'))
        verts.append((x, y, z))
        u = float(v.get('u'))
        v = float(v.get('v'))
        uvs.append((u, v))
        f = f + (face_offset,)
        face_offset += 1
    faces.append(f)

mesh = bpy.data.meshes.new(name="ImportedMesh")
mesh.from_pydata(verts, [], faces)
mesh.update()

obj = bpy.data.objects.new("ImportedObject", mesh)
bpy.context.collection.objects.link(obj)

mesh.uv_layers.new(name="UVMap")
uv_layer = mesh.uv_layers.active.data

i = 0
for poly in mesh.polygons:
    for li in poly.loop_indices:
        uv_layer[li].uv = uvs[i]
        i += 1

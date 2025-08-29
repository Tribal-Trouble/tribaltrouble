import bpy

XML_PATH = "/home/omar/projects/tribaltrouble/tt/geometry/misc/ship/ship_built.xml"

obj = bpy.context.active_object
mesh = obj.data

depsgraph = bpy.context.evaluated_depsgraph_get()
obj_eval = obj.evaluated_get(depsgraph)
mesh = obj_eval.to_mesh()

bm = bmesh.new()
bm.from_mesh(mesh)

bmesh.ops.triangulate(
    bm,
    faces=bm.faces,
    quad_method='BEAUTY',
    ngon_method='BEAUTY'
)

bm.to_mesh(mesh)
bm.free()

f = open(XML_PATH, "w")

if not mesh.uv_layers:
    print("Mesh has no UVs.")
else:
    f.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n\
<!DOCTYPE mesh [\n\
    <!ELEMENT mesh       (polygons)>\n\
    <!ELEMENT polygons   (polygon+)>\n\
    <!ELEMENT polygon    (vertex+)>\n\
    <!ELEMENT vertex     (skin+)>\n\
    <!ELEMENT skin        EMPTY>\n\
    <!ATTLIST vertex x CDATA #REQUIRED>\n\
    <!ATTLIST vertex y CDATA #REQUIRED>\n\
    <!ATTLIST vertex z CDATA #REQUIRED>\n\
    <!ATTLIST vertex r CDATA #REQUIRED>\n\
    <!ATTLIST vertex g CDATA #REQUIRED>\n\
    <!ATTLIST vertex b CDATA #REQUIRED>\n\
    <!ATTLIST vertex a CDATA #REQUIRED>\n\
    <!ATTLIST vertex nx CDATA #REQUIRED>\n\
    <!ATTLIST vertex ny CDATA #REQUIRED>\n\
    <!ATTLIST vertex nz CDATA #REQUIRED>\n\
    <!ATTLIST vertex u CDATA #REQUIRED>\n\
    <!ATTLIST vertex v CDATA #REQUIRED>\n\
    <!ATTLIST skin bone CDATA #REQUIRED>\n\
    <!ATTLIST skin weight CDATA #REQUIRED>\n\
]>\n\
<mesh>\n\
    <polygons>\n\
")

    uv_layer = mesh.uv_layers.active.data

    for poly in mesh.polygons:
        f.write("<polygon>\n")
        for loop_index in poly.loop_indices:
            vert_index = mesh.loops[loop_index].vertex_index
            vertex = mesh.vertices[vert_index]
            normal = mesh.loops[loop_index].normal
            uv = uv_layer[loop_index].uv
            f.write(f"    <vertex x='{vertex.co.x}' y='{vertex.co.y}' z='{vertex.co.z}'\n")
            f.write(f"            r='1' g='1' b='1' a='1'\n")
            f.write(f"            nx='{normal.x}' ny='{normal.y}' nz='{normal.z}'\n")
            f.write(f"            u='{uv.x}' v='{uv.y}'>\n")
            f.write(f"        <skin bone='dummy_bone' weight='1'/>\n")
            f.write(f"    </vertex>\n")
        f.write("</polygon>\n")

    f.write("</polygons>\n</mesh>")

obj_eval.to_mesh_clear()

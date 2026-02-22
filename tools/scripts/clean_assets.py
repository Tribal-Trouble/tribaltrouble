import os
import xml.etree.ElementTree as ET
import re

# Reuse the serialization logic from tt_io.py for consistency
# Row-Major order (mRC)
DTD_ATTR_ORDER = {
    'sprite': ['name', 'scale'], 'group': ['name'], 'model': ['r', 'g', 'b'],
    'texture': ['name', 'team', 'normal'], 'mesh': ['texture'],
    'vertex': ['x', 'y', 'z', 'r', 'g', 'b', 'a', 'nx', 'ny', 'nz', 'u', 'v', 'u2', 'v2'],
    'skin': ['bone', 'weight'],
    'transform': ['name', 'm00', 'm01', 'm02', 'm03', 'm10', 'm11', 'm12', 'm13', 'm20', 'm21', 'm22', 'm23', 'm30', 'm31', 'm32', 'm33'],
    'bone': ['name', 'parent'], 'frame': ['index'], 'animation': ['wpc', 'type']
}

# vertex r,g,b,a and sprite scale must NOT be in DTD_DEFAULTS because the convert tool requires them explicitly
DTD_DEFAULTS = {}

DTD_EMPTY_TAGS = {'texture', 'skin', 'bone', 'transform'}

# Remap legacy mCR to mRC (Row-Major)
ATTR_REMAP = {
    'm10': 'm01', 'm20': 'm02', 'm30': 'm03',
    'm01': 'm10', 'm21': 'm12', 'm31': 'm13',
    'm02': 'm20', 'm12': 'm21', 'm32': 'm23',
    'm03': 'm30', 'm13': 'm31', 'm23': 'm32'
}

def format_float(v):
    try:
        f = float(v)
        if abs(f) < 1e-9: return '0'
        return "{:.9g}".format(f)
    except:
        return v

def tt_serialize_xml(elem, level=0):
    indent = "\t" * level
    tag = elem.tag
    attrs = []
    order = DTD_ATTR_ORDER.get(tag, [])
    defaults = DTD_DEFAULTS.get(tag, {})
    sorted_keys = [k for k in order if k in elem.attrib] + [k for k in elem.attrib if k not in order]
    
    for k in sorted_keys:
        val = elem.attrib[k]
        if k in defaults:
            try:
                if abs(float(val) - float(defaults[k])) < 1e-9: continue
            except: pass
        attrs.append(f'{k}="{format_float(val)}"')
        
    attr_str = " " + " ".join(attrs) if attrs else ""
    if not len(elem) and not elem.text:
        return f'{indent}<{tag}{attr_str}/>' if tag in DTD_EMPTY_TAGS else f'{indent}<{tag}{attr_str}></{tag}>'
            
    res = [f'{indent}<{tag}{attr_str}>']
    if elem.text and elem.text.strip(): res[-1] += elem.text.strip()
    for child in elem: res.append(tt_serialize_xml(child, level + 1))
    if len(elem): res.append(f'{indent}</{tag}>')
    else: res[-1] += f'</{tag}>'
    return "\n".join(res) if len(elem) else res[0]

def get_dtd(tag):
    if tag == 'geometry':
        return """<!DOCTYPE geometry [
	<!ELEMENT geometry  (group+)>
	<!ELEMENT group	 (sprite*)*>
	<!ELEMENT sprite	(skeleton?, model+, animation*)>
	<!ELEMENT animation (#PCDATA)>
	<!ELEMENT model	 (#PCDATA | texture)*>
	<!ELEMENT skeleton  (#PCDATA)>
	<!ELEMENT texture  EMPTY>
	<!ATTLIST sprite name CDATA #REQUIRED>
	<!ATTLIST sprite scale CDATA #REQUIRED>
	<!ATTLIST group name CDATA #REQUIRED>
	<!ATTLIST model r CDATA #REQUIRED>
	<!ATTLIST model g CDATA #REQUIRED>
	<!ATTLIST model b CDATA #REQUIRED>
	<!ATTLIST animation wpc CDATA #REQUIRED>
	<!ATTLIST animation type CDATA #REQUIRED>
	<!ATTLIST texture name CDATA #REQUIRED>
	<!ATTLIST texture team CDATA #IMPLIED>
	<!ATTLIST texture normal CDATA #IMPLIED>
]>"""
    elif tag == 'mesh':
        return """<!DOCTYPE mesh [
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
	<!ATTLIST bone name CDATA #REQUIRED>
	<!ATTLIST bone parent CDATA #REQUIRED>
]>"""
    elif tag == 'skeleton':
        return """<!DOCTYPE skeleton [
	<!ELEMENT skeleton   (bones, init_pose)>
	<!ELEMENT bones      (bone+)>
	<!ELEMENT init_pose  (transform+)>
	<!ELEMENT bone        EMPTY>
	<!ELEMENT transform   EMPTY>
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
	<!ATTLIST bone name CDATA #REQUIRED>
	<!ATTLIST bone parent CDATA #REQUIRED>
]>"""
    elif tag == 'animation':
        return """<!DOCTYPE animation [
	<!ELEMENT animation  (frame+)>
	<!ELEMENT frame      (transform+)>
	<!ELEMENT transform   EMPTY>
	<!ATTLIST frame index CDATA #REQUIRED>
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
    return ""

def clean_file(filepath):
    print(f"Cleaning: {filepath}")
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        
        # Helper to remap transform attributes
        def remap_transform(node):
            if node.tag != 'transform': return
            new_attrs = {}
            for k, v in node.attrib.items():
                if k in ATTR_REMAP:
                    new_attrs[ATTR_REMAP[k]] = v
                else:
                    new_attrs[k] = v
            node.attrib = new_attrs

        # 1. Enforce dummy_bone for mesh and remap embedded skeleton transforms
        if root.tag == 'mesh':
            polygons = root.find('polygons')
            if polygons is not None:
                for poly in polygons.findall('polygon'):
                    for vertex in poly.findall('vertex'):
                        if not vertex.findall('skin'):
                            ET.SubElement(vertex, 'skin', bone="dummy_bone", weight="1")
                        # Default mandatory colors if missing
                        for c in ['r', 'g', 'b', 'a']:
                            if c not in vertex.attrib: vertex.attrib[c] = "1"
            skel = root.find('skeleton')
            if skel is not None:
                init_pose = skel.find('init_pose')
                if init_pose is not None:
                    for t in init_pose.findall('transform'): remap_transform(t)
                            
        # 2. Sort elements for determinism, remap transforms, and default scale
        if root.tag == 'geometry':
            # Sort groups
            groups = sorted(root.findall('group'), key=lambda g: g.get('name', ''))
            for g in root.findall('group'): root.remove(g)
            for g in groups:
                root.append(g)
                # Sort sprites in group
                sprites = sorted(g.findall('sprite'), key=lambda s: s.get('name', ''))
                for s in g.findall('sprite'): g.remove(s)
                for s in sprites:
                    g.append(s)
                    # Mandatory scale if missing
                    if 'scale' not in s.attrib: s.attrib['scale'] = "1"
                    # Sort animations in sprite
                    anims = sorted(s.findall('animation'), key=lambda a: a.text.strip() if a.text else '')
                    for a in s.findall('animation'): s.remove(a)
                    for a in anims: s.append(a)
        elif root.tag == 'skeleton':
            bones_node = root.find('bones')
            init_pose = root.find('init_pose')
            if bones_node is not None:
                bones = sorted(bones_node.findall('bone'), key=lambda b: b.get('name', ''))
                for b in bones_node.findall('bone'): bones_node.remove(b)
                for b in bones: bones_node.append(b)
            if init_pose is not None:
                transforms = sorted(init_pose.findall('transform'), key=lambda t: t.get('name', ''))
                for t in init_pose.findall('transform'): 
                    remap_transform(t)
                    init_pose.remove(t)
                for t in transforms: init_pose.append(t)
        elif root.tag == 'animation':
            for frame in root.findall('frame'):
                transforms = sorted(frame.findall('transform'), key=lambda t: t.get('name', ''))
                for t in frame.findall('transform'): 
                    remap_transform(t)
                    frame.remove(t)
                for t in transforms: frame.append(t)

        # Write out with new DTD and deterministic serialization
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n\n')
            f.write(get_dtd(root.tag) + '\n\n')
            f.write(tt_serialize_xml(root))
            
    except Exception as e:
        print(f"Error cleaning {filepath}: {e}")
        import traceback
        traceback.print_exc()

def run_cleanup(root_dir):
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith(".xml") and "schemas" not in root:
                clean_file(os.path.join(root, file))

if __name__ == "__main__":
    assets_path = os.path.join(os.getcwd(), "assets")
    if os.path.exists(assets_path):
        run_cleanup(assets_path)
    
    export_path = os.path.join(os.getcwd(), "export")
    if os.path.exists(export_path):
        run_cleanup(export_path)
    else:
        print("Note: export directory not found.")

import os
import subprocess
import re
import xml.etree.ElementTree as ET

# Define strict attribute order for validation
ATTR_ORDER = {
    'sprite': ['name', 'scale'],
    'group': ['name'],
    'model': ['r', 'g', 'b'],
    'texture': ['name', 'team', 'normal'],
    'mesh': ['texture'],
    'vertex': ['x', 'y', 'z', 'r', 'g', 'b', 'a', 'nx', 'ny', 'nz', 'u', 'v', 'u2', 'v2'],
    'skin': ['bone', 'weight'],
    'transform': ['name', 'm00', 'm01', 'm02', 'm03', 'm10', 'm11', 'm12', 'm13', 'm20', 'm21', 'm22', 'm23', 'm30', 'm31', 'm32', 'm33'],
    'bone': ['name', 'parent'],
    'frame': ['index'],
    'animation': ['wpc', 'type']
}

def check_attribute_order(filepath):
    """Custom check for attribute ordering as standard parsers ignore it."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Regex to find tags and their attributes in raw text
    tag_pattern = re.compile(r'<(\w+)\s+([^>]+)>')
    attr_pattern = re.compile(r'(\w+)="[^"]*"')
    
    errors = []
    for match in tag_pattern.finditer(content):
        tag_name = match.group(1)
        if tag_name not in ATTR_ORDER:
            continue
            
        expected_order = ATTR_ORDER[tag_name]
        raw_attrs = attr_pattern.findall(match.group(2))
        
        # Filter expected order to only what's present in the file
        filtered_expected = [a for a in expected_order if a in raw_attrs]
        
        if raw_attrs != filtered_expected:
            errors.append(f"Tag <{tag_name}> in {filepath}: Attribute order mismatch.\nFound: {raw_attrs}\nExpected: {filtered_expected}")
            
    return errors

def validate_mesh_integrity(filepath):
    """Deep check for vertex weights and normalization."""
    tree = ET.parse(filepath); root = tree.getroot()
    errors = []
    v_idx = 0
    polygons = root.find("polygons")
    if polygons is not None:
        for polygon in polygons.findall("polygon"):
            for vertex in polygon.findall("vertex"):
                skins = vertex.findall("skin")
                if not skins:
                    errors.append(f"Vertex {v_idx} has no skin weights.")
                else:
                    total_w = sum(float(s.get("weight", 0)) for s in skins)
                    if abs(total_w - 1.0) > 1e-5:
                        errors.append(f"Vertex {v_idx} weights do not sum to 1.0 (Sum: {total_w})")
                    
                    has_dummy = any(s.get("bone") == "dummy_bone" for s in skins)
                    if len(skins) > 1 and has_dummy:
                        errors.append(f"Vertex {v_idx} mixes 'dummy_bone' with real bones.")
                v_idx += 1
    return errors

def validate_skeleton_integrity(filepath):
    """Check for hierarchy loops and deterministic sorting."""
    tree = ET.parse(filepath); root = tree.getroot()
    errors = []
    bones_node = root.find("bones")
    if bones_node is not None:
        bones = bones_node.findall("bone")
        bone_names = [b.get("name") for b in bones]
        
        # 1. Alphabetical Check
        if bone_names != sorted(bone_names):
            errors.append("Bones are not sorted alphabetically.")
            
        # 2. Hierarchy Check
        parent_map = {b.get("name"): b.get("parent") for b in bones}
        for bone in bone_names:
            visited = set()
            curr = bone
            while curr:
                if curr in visited:
                    errors.append(f"Circular dependency detected at bone '{curr}'")
                    break
                visited.add(curr)
                curr = parent_map.get(curr)
                if curr and curr not in parent_map:
                    errors.append(f"Bone '{bone}' has unknown parent '{curr}'")
                    break
    return errors

def validate_animation_integrity(filepath):
    """Check for frame continuity and strict ordering."""
    tree = ET.parse(filepath); root = tree.getroot()
    errors = []
    frames = root.findall("frame")
    indices = [int(f.get("index")) for f in frames]
    if indices != sorted(indices):
        errors.append("Animation frames are not in strict numerical order.")
    if len(indices) > 1 and indices != list(range(indices[0], indices[-1] + 1)):
        errors.append("Animation frame sequence has gaps.")
    return errors

def validate_manifest_links(filepath):
    """Ensure all paths in geometry.xml are valid."""
    base_dir = os.path.dirname(filepath)
    tree = ET.parse(filepath); root = tree.getroot()
    errors = []
    
    for node in root.findall(".//skeleton") + root.findall(".//model") + root.findall(".//animation"):
        path = node.text.strip() if node.text else ""
        if path and not os.path.exists(os.path.join(base_dir, path)):
            errors.append(f"Broken link: '{path}' referenced by <{node.tag}>")
    return errors

def validate_file(filepath, schema_path):
    root_tag = ET.parse(filepath).getroot().tag
    
    # Standard XML/Schema Validation
    try:
        subprocess.run(['xmllint', '--noout', '--valid', filepath], check=True, capture_output=True, text=True)
        subprocess.run(['xmllint', '--noout', '--relaxng', schema_path, filepath], check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as e:
        print(f"  [FAIL] Schema/DTD: {filepath}\n{e.stderr}"); return False

    # Deep Integrity Checks
    errors = []
    if root_tag == 'mesh': errors += validate_mesh_integrity(filepath)
    elif root_tag == 'skeleton': errors += validate_skeleton_integrity(filepath)
    elif root_tag == 'animation': errors += validate_animation_integrity(filepath)
    elif root_tag == 'geometry': errors += validate_manifest_links(filepath)
    
    errors += check_attribute_order(filepath)
    
    if errors:
        for err in errors: print(f"  [FAIL] {filepath}: {err}")
        return False
    return True

def run_validation():
    assets_dir = os.path.join(os.getcwd(), "assets")
    schemas_dir = os.path.join(assets_dir, "schemas")
    
    success_count = 0
    fail_count = 0
    
    print("Starting validation of all XML assets...")
    for root, dirs, files in os.walk(os.path.join(assets_dir, "geometry")):
        for file in files:
            if not file.endswith(".xml"):
                continue
                
            filepath = os.path.join(root, file)
            
            # Identify file type by reading root tag
            try:
                tree = ET.parse(filepath)
                root_tag = tree.getroot().tag
            except:
                print(f"Error parsing {filepath}")
                continue
                
            schema_file = f"{root_tag}.rng"
            schema_path = os.path.join(schemas_dir, schema_file)
            
            if not os.path.exists(schema_path):
                print(f"No schema found for {root_tag} in {filepath}")
                continue
                
            if validate_file(filepath, schema_path):
                success_count += 1
            else:
                fail_count += 1
                
    print(f"\nValidation Complete: {success_count} Passed, {fail_count} Failed.")

if __name__ == "__main__":
    run_validation()

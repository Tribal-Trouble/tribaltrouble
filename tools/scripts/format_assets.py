import os
import re

FLOAT_ATTRS = {
    'x', 'y', 'z', 'r', 'g', 'b', 'a', 'nx', 'ny', 'nz', 'u', 'v', 'u2', 'v2', 
    'weight', 'scale', 'wpc',
    'm00', 'm01', 'm02', 'm03', 'm10', 'm11', 'm12', 'm13', 
    'm20', 'm21', 'm22', 'm23', 'm30', 'm31', 'm32', 'm33'
}

def format_float_value(v_str):
    try:
        v = float(v_str)
        if abs(v) < 1e-9: return '0'
        # Format with up to 9 significant digits, stripping trailing zeros
        s = "{:.9g}".format(v)
        return s
    except ValueError:
        return v_str

def format_file(filepath):
    print(f"Formatting: {filepath}")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    def replace_attr(match):
        attr_name = match.group(1)
        attr_val = match.group(2)
        if attr_name in FLOAT_ATTRS:
            new_val = format_float_value(attr_val)
            return f'{attr_name}="{new_val}"'
        return match.group(0)

    # Regex to find attr="value"
    new_content = re.sub(r'(\w+)="([^"]+)"', replace_attr, content)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

def run_formatting(root_dir):
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith(".xml"):
                format_file(os.path.join(root, file))

if __name__ == "__main__":
    assets_path = os.path.join(os.getcwd(), "assets")
    if os.path.exists(assets_path):
        run_formatting(assets_path)
    else:
        print("Error: assets directory not found.")

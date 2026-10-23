"""Package the add-on as a Blender extension zip (Blender 4.2 and newer). Plain Python, no Blender needed:

    python tools/blender/build_extension.py

Writes tools/blender/dist/tribal_trouble_io-<version>.zip. Install it by dragging the zip into Blender, or with
Edit > Preferences > Get Extensions > Install from Disk. After that the add-on updates itself from the repo folder:
pull, then press "Update add-on" in the Models panel.

The io_tribaltrouble package stays the single source of truth; the zip is its modules plus a manifest.
"""
import os
import re
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "io_tribaltrouble")
EXTENSION_ID = "tribal_trouble_io"

MANIFEST = '''schema_version = "1.0.0"
id = "{id}"
version = "{version}"
name = "Tribal Trouble Models"
tagline = "Browse, edit and export Tribal Trouble models and attachments"
maintainer = "Tribal Trouble team"
type = "add-on"
website = "https://github.com/Tribal-Trouble/tribaltrouble"
tags = ["Import-Export"]
blender_version_min = "4.2.0"
license = ["SPDX:GPL-2.0-only"]

[permissions]
files = "Read and write model files in your Tribal Trouble checkout"
'''


def main():
    with open(os.path.join(SOURCE, "__init__.py"), encoding="utf-8") as f:
        source = f.read()
    version = ".".join(re.search(r'"version":\s*\((\d+),\s*(\d+),\s*(\d+)\)', source).groups())
    out_dir = os.path.join(HERE, "dist")
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, f"{EXTENSION_ID}-{version}.zip")
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        for name in sorted(os.listdir(SOURCE)):
            if name.endswith(".py"):
                z.write(os.path.join(SOURCE, name), name)
        z.writestr("blender_manifest.toml", MANIFEST.format(id=EXTENSION_ID, version=version))
    print(path)
    return path


if __name__ == "__main__":
    main()

"""The repo folder and geometry.xml: reading, editing entries, sprite text and skins."""

import os
import re
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape, quoteattr

import bpy

from .textures import decal_texture_path, emission_image, find_up, image_texture_name, texture_names
from .rig import GAME_SLOTS


def find_base_sprite(skeleton_path):
    """(group, sprite name) of the registry entry that owns this skeleton and carries the clips."""
    registry = find_up(skeleton_path, "geometry.xml") if skeleton_path else None
    if registry is None:
        return None, None
    try:
        root = ET.parse(registry).getroot()
    except ET.ParseError:
        return None, None
    skeleton_norm = os.path.abspath(skeleton_path).replace("\\", "/").lower()
    for group in root.iter("group"):
        for sprite in group.iter("sprite"):
            skeleton = sprite.find("skeleton")
            if skeleton is None or not skeleton.text or sprite.find("animation") is None:
                continue
            if skeleton_norm.endswith(skeleton.text.strip().replace("\\", "/").lower()):
                return group.get("name"), sprite.get("name")
    return None, None


def sprite_text(attrs, models, skeleton=None, clips=()):
    """One geometry.xml sprite entry. attrs are (name, value) pairs in order, models (path, [(texture, team
    attribute)], one per tier) and clips (name, type, path)."""
    lines = ["        <sprite " + " ".join(f"{key}={quoteattr(value)}" for key, value in attrs) + ">"]
    if skeleton:
        lines.append(f"            <skeleton>{escape(skeleton)}</skeleton>")
    for path, textures in models:
        lines += ['            <model r="90" g="60" b="30">', f"                {escape(path)}"]
        lines += [f"                <texture name={quoteattr(texture)}{team}/>" for texture, team in textures]
        lines.append("            </model>")
    for name, kind, path in clips:
        lines.append(f'            <animation name={quoteattr(name)} wpc="1" type="{kind}">{escape(path)}</animation>')
    return "\n".join(lines + ["        </sprite>"])


def item_slot(point, group):
    """The game slot an item on this attachment point goes in; everything on scenery, like the chicken, is a prop."""
    return PROP_SLOT if group == SCENERY_GROUP else GAME_SLOTS.get(point, point.lower())


def registry_entries(context, arm, base, group=""):
    """(sprite name, geometry.xml text) for every visible new attachment in the panel's point slots, with the event
    and default its New Prop form picked."""
    root = repo_root(context)
    entries = []
    for slot in arm.tt_attachments:
        if slot.obj is None or not slot.visible:
            continue
        obj = slot.obj
        game_slot = item_slot(slot.point, group)
        textures = [(t, team_attribute(root, t, bool(obj.get("tt_texture"))) + emissive_attribute(obj))
                    for t in texture_names(obj) or ["TEXTURE"]]
        model = f"misc/{obj.name}.xml"
        if root and arm.get("tt_skeleton"):
            model = os.path.relpath(os.path.join(os.path.dirname(arm["tt_skeleton"]), obj.name + ".xml"),
                                    os.path.join(root, GEOMETRY_DIR)).replace(os.sep, "/")
        name = f"{base}_{obj.name}"
        extra = ([("event", obj["tt_event"])] if obj.get("tt_event") else []) + (
            [("default", "true")] if obj.get("tt_default") else [])
        entries.append((name, sprite_text([("name", name), ("base", base), ("slot", game_slot)] + extra,
                                          [(model, textures)])))
    return entries


GEOMETRY_DIR = os.path.join("assets", "geometry")
REGISTRY_FILE = os.path.join(GEOMETRY_DIR, "geometry.xml")


def resolve_repo_root(path):
    """Accepts the repo root or its assets or geometry folder; '' when geometry.xml is not under it."""
    path = bpy.path.abspath(path or "").rstrip("\\/")
    for candidate in (path, os.path.dirname(path), os.path.dirname(os.path.dirname(path))):
        if candidate and os.path.isfile(os.path.join(candidate, REGISTRY_FILE)):
            return candidate
    return ""


def root_holder(context):
    """The add-on preferences when the add-on is enabled, else the window manager for this session."""
    addon = context.preferences.addons.get(__package__)
    return addon.preferences if addon is not None else context.window_manager


def repo_root(context):
    holder = root_holder(context)
    return resolve_repo_root(getattr(holder, "repo_root", "") or getattr(holder, "tt_repo_root", ""))


def remember_repo_root(context, file_path):
    if repo_root(context):
        return
    registry = find_up(file_path, REGISTRY_FILE)
    if registry is None:
        return
    root = os.path.dirname(os.path.dirname(os.path.dirname(registry)))
    holder = root_holder(context)
    setattr(holder, "repo_root" if hasattr(holder, "repo_root") else "tt_repo_root", root)


def read_registry(root):
    """Every sprite in geometry.xml as a dict, in file order."""
    sprites = []
    for group in ET.parse(os.path.join(root, REGISTRY_FILE)).getroot().findall("group"):
        for sprite in group.findall("sprite"):
            skeleton = sprite.find("skeleton")
            sprites.append({
                "group": group.get("name"), "name": sprite.get("name"), "base": sprite.get("base") or "",
                "slot": sprite.get("slot") or "", "default": sprite.get("default") == "true",
                "event": sprite.get("event") or "", "skin": sprite.get("skin") or "",
                "decoration": sprite.get("decoration") or "", "count": sprite.get("count") or "",
                "replaces": sprite.get("replaces") or "",
                "textures": [[(t.get("name"), t.get("event") or "") for t in m.findall("texture")]
                             for m in sprite.findall("model")],
                "emissive": [next((t.get("emissive") for t in m.findall("texture") if t.get("emissive")), "")
                             for m in sprite.findall("model")],
                "skeleton": skeleton.text.strip() if skeleton is not None and skeleton.text else "",
                "models": [(m.text or "").strip() for m in sprite.findall("model")],
                "clips": [(a.text or "").strip() for a in sprite.findall("animation")],
                "clip_info": {a.get("name"): (a.get("wpc"), a.get("type"), (a.text or "").strip())
                              for a in sprite.findall("animation")},
            })
    return sprites


def rig_entry(registry, entry):
    """The sprite that supplies the skeleton and clips: the entry itself, or its base."""
    if not entry["base"]:
        return entry
    return next((s for s in registry if s["group"] == entry["group"] and s["name"] == entry["base"]), entry)


def rig_in_repo(context, arm):
    """Registry edits go to the repo folder's geometry.xml, so the rig must have been loaded from under it."""
    root = repo_root(context)
    skeleton = arm.get("tt_skeleton", "") if arm is not None else ""
    if not root or not skeleton:
        return False
    geometry = os.path.normcase(os.path.abspath(os.path.join(root, GEOMETRY_DIR)))
    return os.path.normcase(os.path.abspath(skeleton)).startswith(geometry + os.sep)


def rig_registry(context, arm):
    """(group, base, rig sprite) of an armature loaded from the repo folder, Nones for any other."""
    if not rig_in_repo(context, arm):
        return None, None, None
    group, base = find_base_sprite(arm["tt_skeleton"])
    rig = next((s for s in read_registry(repo_root(context)) if s["group"] == group and s["name"] == base), None)
    return group, base, rig


CATEGORY_ITEMS = (
    ("UNITS", "Units", "Models with a skeleton"),
    ("BUILDINGS", "Buildings", "Buildings and their half built and construction stages"),
    ("RESOURCES", "Resources", "Rocks, wood piles and treasure"),
    ("NATURE", "Trees and Plants", "Trees, palms and plants"),
    ("DECORATIONS", "Decorations", "Map scenery the game scatters, such as pumpkin patches"),
    ("OTHER", "Other", "Everything else"),
    ("ALL", "All", "Every model"),
)
CATEGORY_ICONS = {"UNITS": "ARMATURE_DATA", "BUILDINGS": "HOME"}
SCENERY_GROUP = "misc"  # nobody owns what is in it: trees, rocks, the chicken, decorations


def sprite_category(registry, sprite):
    name = sprite["name"]
    if rig_entry(registry, sprite)["skeleton"]:
        return "UNITS"
    if sprite["decoration"]:
        return "DECORATIONS"
    if name.endswith(("_halfbuilt", "_start")) or any(s["group"] == sprite["group"] and s["name"] == name + "_start"
                                                     for s in registry):
        return "BUILDINGS"
    if any(word in name for word in ("tree", "palm", "plant")):
        return "NATURE"
    if name.startswith(("rock_", "wood_", "treasure_")):
        return "RESOURCES"
    return "OTHER"


CARRY_SLOT = "carried"  # what a peon hauls or rows with; the game picks which one shows


def team_attribute(root, texture, fallback):
    """team="..." when the decal PNG exists or is an image Publish writes; without a repo folder, fall back to the
    caller's guess."""
    if root:
        fallback = os.path.isfile(decal_texture_path(root, texture)) or texture + "_team" in bpy.data.images
    return f' team="{texture}_team"' if fallback else ""


def emissive_attribute(obj):
    image = emission_image(obj)
    return f" emissive={quoteattr(image_texture_name(image))}" if image is not None else ""


def append_registry_entries(registry_path, group, entries):
    """Insert sprite entries at the end of a group in geometry.xml, skipping names it already has."""
    with open(registry_path, "rb") as f:
        raw = f.read()
    newline = "\r\n" if b"\r\n" in raw else "\n"
    text = raw.decode("utf-8")
    start = text.find(f'<group name="{group}">')
    if start < 0:
        raise ValueError(f"No group named {group} in {registry_path}")
    end = text.index("</group>", start)
    line_start = text.rfind("\n", 0, end) + 1
    added = [entry for name, entry in entries if f'<sprite name="{name}"' not in text[start:end]]
    block = "".join(entry.replace("\n", newline) + newline for entry in added)
    with open(registry_path, "wb") as f:
        f.write((text[:line_start] + block + text[line_start:]).encode("utf-8"))
    return len(added)


def remove_registry_entry(registry_path, group, name):
    """Cut one sprite entry out of a group. The mesh files stay on disk."""
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    start = text.find(f'<group name="{group}">')
    end = text.index("</group>", start) if start >= 0 else -1
    match = re.search(r'[ \t]*<sprite name="%s"[ >].*?</sprite>\r?\n' % re.escape(name), text[start:end], re.S) \
        if start >= 0 else None
    if match is None:
        return False
    with open(registry_path, "wb") as f:
        f.write((text[:start + match.start()] + text[start + match.end():]).encode("utf-8"))
    return True


def set_clip_line(registry_path, group, skeleton, name, line=None):
    """Add or update <animation name=...> (or take it off, with no line) on every sprite in the group that lists
    this skeleton. Items and carried things inherit the unit's clips through base=. New clips go last, so the numbers
    the game already uses do not move. Returns the sprites touched."""
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    touched, out, cursor = [], [], 0
    for sprite_group, sprite, start, end in sprite_blocks(text):
        block = text[start:end]
        listed = re.search(r"<skeleton>\s*([^<]+?)\s*</skeleton>", block)
        if sprite_group != group or listed is None or listed.group(1).replace("\\", "/") != skeleton:
            continue
        if line is None:
            existing = re.search(r'[ \t]*<animation\s+name="%s"[^>]*>[^<]*</animation>[ \t]*\r?\n' % re.escape(name),
                                 block)
            if existing is None:
                continue
            edited = block[:existing.start()] + block[existing.end():]
        else:
            existing = re.search(r'<animation\s+name="%s"[^>]*>[^<]*</animation>' % re.escape(name), block)
            if existing is not None:
                edited = block[:existing.start()] + line + block[existing.end():]
            else:
                anchors = list(re.finditer(r"([ \t]*)<(?:animation|model)\b.*?</(?:animation|model)>[ \t]*(\r?\n)", block,
                                           re.S))
                last = anchors[-1]
                edited = block[:last.end()] + last.group(1) + line + last.group(2) + block[last.end():]
        if edited != block:
            out.append(text[cursor:start] + edited)
            cursor = end
        touched.append(sprite)
    if out:
        with open(registry_path, "wb") as f:
            f.write(("".join(out) + text[cursor:]).encode("utf-8"))
    return touched


def set_sprite_textures(root, group, name, textures):
    """Replace the texture lines of every model of one sprite in geometry.xml; the rest of the file stays as it is."""
    registry_path = os.path.join(root, REGISTRY_FILE)
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    for sprite_group, sprite, start, end in sprite_blocks(text):
        if (sprite_group, sprite) != (group, name):
            continue
        block = text[start:end]
        for model in reversed(list(re.finditer(r"<model\b[^>]*>.*?</model>", block, re.S))):
            lines = list(TEXTURE_LINE.finditer(block, model.start(), model.end()))
            if not lines:
                continue
            first = lines[0].group(0)
            ending = first[len(first.rstrip("\r\n")):]
            new = "".join(f"{lines[0].group(1)}<texture name={quoteattr(t)}{team_attribute(root, t, False)}/>{ending}"
                          for t in textures)
            block = block[:lines[0].start()] + new + block[lines[-1].end():]
        with open(registry_path, "wb") as f:
            f.write((text[:start] + block + text[end:]).encode("utf-8"))
        return True
    return False


def set_sprite_attributes(registry_path, group, name, attrs):
    """Set attributes on one sprite's opening tag, dropping those given a blank value; the rest of the file stays as
    it is."""
    with open(registry_path, "rb") as f:
        text = f.read().decode("utf-8")
    for sprite_group, sprite, start, end in sprite_blocks(text):
        if (sprite_group, sprite) != (group, name):
            continue
        tag_end = text.index(">", start)
        tag = text[start:tag_end]
        for key, value in attrs:
            existing = re.search(r'\s%s="[^"]*"' % re.escape(key), tag)
            new = f" {key}={quoteattr(value)}" if value else ""
            tag = tag[:existing.start()] + new + tag[existing.end():] if existing else tag + new
        with open(registry_path, "wb") as f:
            f.write((text[:start] + tag + text[tag_end:]).encode("utf-8"))
        return True
    return False


PROP_SLOT = "prop"
TEXTURE_LINE = re.compile(r'([ \t]*)<texture\s+name="([^"]+)"([^>]*?)/>[ \t]*\r?\n')


def sprite_blocks(text):
    """(group, name, start, end) of every sprite element in the registry text."""
    groups = [(m.start(), m.group(1)) for m in re.finditer(r'<group\s+name="([^"]+)"', text)]
    for m in re.finditer(r'<sprite\s+name="([^"]+)"[^>]*>.*?</sprite>', text, re.S):
        group = [name for start, name in groups if start < m.start()][-1]
        yield group, m.group(1), m.start(), m.end()


def sprite_skins(registry, entry):
    return [s for s in registry if s["group"] == entry["group"] and s["skin"] and s["replaces"] == entry["name"]]


def level_textures(entry, level):
    """A model's texture list without its event textures: one per tier."""
    return [t for t, event in entry["textures"][min(level, len(entry["textures"]) - 1)] if not event]

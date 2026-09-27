"""Skeleton and clip XML, and the armatures, actions and attachment points built from them."""

import hashlib
import os
import xml.etree.ElementTree as ET
from xml.sax.saxutils import quoteattr

import bpy
import numpy as np
from mathutils import Matrix

from .mesh_io import write_lines, XML_HEADER


SKELETONS = (
    ("PEON", "Peon (both races)", "vikings/peon and natives/peon skeletons share bone names"),
    ("VIKING_WARRIOR", "Viking warrior", ""),
    ("NATIVE_WARRIOR", "Native warrior", ""),
    ("VIKING_CHIEFTAIN", "Viking chieftain", ""),
    ("NATIVE_CHIEFTAIN", "Native chieftain", ""),
    ("CHICKEN", "Chicken", ""),
)

# Bone names must match the skeleton file exactly (note the double space in "warrior  Head").
# Game slot an attachment point feeds; H in game cycles the "hat" slot. Other points use their own name.
GAME_SLOTS = {"HEAD": "hat", "PROP1": "weapon"}

ATTACHMENT_POINTS = {
    "HEAD": {"PEON": "peon Head", "VIKING_WARRIOR": "warrior  Head", "NATIVE_WARRIOR": "Head",
             "VIKING_CHIEFTAIN": "Head", "NATIVE_CHIEFTAIN": "Head", "CHICKEN": "chicken Head"},
    "BACK": {"PEON": "peon Spine2", "VIKING_WARRIOR": "warrior  Spine1", "NATIVE_WARRIOR": "Spine2",
             "VIKING_CHIEFTAIN": "Spine1", "NATIVE_CHIEFTAIN": "Spine2", "CHICKEN": "chicken Spine"},
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

MATRIX_ATTLIST = "".join(f"        <!ATTLIST transform m{c}{r} CDATA #REQUIRED>\n" for c in range(4) for r in range(4))

SKELETON_DOCTYPE = """<!DOCTYPE skeleton [
        <!ELEMENT skeleton   (bones, init_pose)>
        <!ELEMENT bones      (bone+)>
        <!ELEMENT init_pose  (transform+)>
        <!ELEMENT bone        EMPTY>
        <!ELEMENT transform   EMPTY>
        <!ATTLIST transform name CDATA #REQUIRED>
""" + MATRIX_ATTLIST + """        <!ATTLIST bone name CDATA #REQUIRED>
        <!ATTLIST bone parent CDATA #REQUIRED>
        ]>"""

ANIMATION_DOCTYPE = """<!DOCTYPE animation [
        <!ELEMENT animation  (frame+)>
        <!ELEMENT frame      (transform+)>
        <!ELEMENT transform   EMPTY>
        <!ATTLIST frame index CDATA #REQUIRED>
        <!ATTLIST transform name CDATA #REQUIRED>
""" + MATRIX_ATTLIST + """        ]>"""


def matrix_from_element(t):
    """The XML stores each 4x4 as m<column><row>, translation in m30..m32; bones are absolute in model space."""
    m = Matrix.Identity(4)
    for c in range(4):
        for r in range(4):
            m[r][c] = float(t.get(f"m{c}{r}"))
    return m


def matrix_attrs(m):
    parts = []
    for c in range(4):
        for r in range(4):
            v = m[r][c]
            parts.append(f'm{c}{r}="{(0.0 if v == 0 else v):.6g}"')
    return " ".join(parts)


def read_skeleton(path):
    """Return (parents, rest): bone name -> parent name ("" for roots), and bone name -> model-space rest Matrix."""
    root = ET.parse(path).getroot()
    if root.tag != "skeleton":
        raise ValueError("not a Tribal Trouble skeleton file (no <skeleton> root)")
    parents = {b.get("name"): b.get("parent") for b in root.find("bones")}
    rest = {t.get("name"): matrix_from_element(t) for t in root.find("init_pose")}
    return parents, rest


def read_animation(path):
    """Return frames in index order, each bone name -> model-space Matrix."""
    root = ET.parse(path).getroot()
    if root.tag != "animation":
        raise ValueError("not a Tribal Trouble animation file (no <animation> root)")
    frames = {}
    for frame in root.iter("frame"):
        frames[int(frame.get("index"))] = {t.get("name"): matrix_from_element(t) for t in frame.iter("transform")}
    return [frames[i] for i in sorted(frames)]


def normalized_rest(m):
    r = m.to_3x3()
    r.normalize()
    out = r.to_4x4()
    out.translation = m.to_translation()
    return out


def build_armature(context, name, parents, rest):
    """Armature whose bones' rest matrices equal the skeleton file's (bone Y axis follows the file's Y column)."""
    arm_data = bpy.data.armatures.new(name)
    arm_data.display_type = "STICK"
    arm = bpy.data.objects.new(name, arm_data)
    context.collection.objects.link(arm)
    for o in context.selected_objects:
        o.select_set(False)
    context.view_layer.objects.active = arm
    arm.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    children = {}
    for bone, parent in parents.items():
        children.setdefault(parent, []).append(bone)
    edit_bones = {}
    for bone, m in rest.items():
        eb = arm_data.edit_bones.new(bone)
        head = m.to_translation()
        length = min(((rest[k].to_translation() - head).length for k in children.get(bone, []) if k in rest),
                     default=0.1)
        eb.head = (0.0, 0.0, 0.0)
        eb.tail = (0.0, max(length, 0.02), 0.0)
        eb.matrix = normalized_rest(m)
        edit_bones[bone] = eb
    for bone, eb in edit_bones.items():
        eb.parent = edit_bones.get(parents.get(bone))
    bpy.ops.object.mode_set(mode="OBJECT")
    return arm


def assign_action(arm, action):
    anim = arm.animation_data or arm.animation_data_create()
    anim.action = action
    if hasattr(action, "slots") and getattr(anim, "action_slot", None) is None:
        # Blender 4.4+ slotted actions: bind the first slot, creating one for a fresh action.
        slot = action.slots[0] if len(action.slots) else action.slots.new(id_type="OBJECT", name=arm.name)
        anim.action_slot = slot
    return anim


def apply_clip(context, arm, name, frames):
    """One action keyed at frames 1..n. Pose = parent_pose @ parent_rest^-1 @ rest @ basis, so basis is solved
    per bone from the file's absolute matrices without touching the depsgraph."""
    action = bpy.data.actions.new(name)
    action.use_fake_user = True
    action["tt_armature"] = arm.name
    assign_action(arm, action)
    rest = {b.name: b.matrix_local for b in arm.data.bones}
    for pb in arm.pose.bones:
        pb.rotation_mode = "QUATERNION"
    for f, frame in enumerate(frames, start=1):
        for pb in arm.pose.bones:
            m = frame.get(pb.name)
            if m is None:
                continue
            parent = pb.parent
            if parent is not None and parent.name in frame:
                local_rest = rest[parent.name].inverted() @ rest[pb.name]
                basis = (frame[parent.name] @ local_rest).inverted() @ m
            else:
                basis = rest[pb.name].inverted() @ m
            loc, rot, scale = basis.decompose()
            pb.location = loc
            pb.rotation_quaternion = rot
            pb.scale = scale
            pb.keyframe_insert("location", frame=f, group=pb.name)
            pb.keyframe_insert("rotation_quaternion", frame=f, group=pb.name)
            pb.keyframe_insert("scale", frame=f, group=pb.name)
    context.scene.frame_start = 1
    context.scene.frame_end = max(context.scene.frame_end, len(frames))
    return action


def bind_meshes(arm, meshes):
    """Armature modifier plus parenting for meshes whose vertex groups name this armature's bones."""
    bone_names = {b.name for b in arm.data.bones}
    bound = 0
    for o in meshes:
        if not any(vg.name in bone_names for vg in o.vertex_groups):
            continue
        modifier = o.modifiers.new("Armature", "ARMATURE")
        modifier.object = arm
        o.parent = arm
        o.matrix_parent_inverse = arm.matrix_world.inverted()
        bound += 1
    return bound


def active_armature(context):
    o = context.active_object
    if o is None:
        return None
    if o.type == "ARMATURE":
        return o
    if o.parent is not None and o.parent.type == "ARMATURE":
        return o.parent
    return None


def armature_from_file(context, path):
    parents, rest = read_skeleton(path)
    name = os.path.splitext(os.path.basename(path))[0]
    if name.endswith("_skeleton"):
        name = name[:-len("_skeleton")]
    arm = build_armature(context, name, parents, rest)
    arm["tt_skeleton"] = path
    setup_attachment_slots(arm)
    return arm


def write_skeleton_xml(arm, filepath):
    bones = sorted(arm.data.bones, key=lambda b: b.name)
    lines = [XML_HEADER, "", SKELETON_DOCTYPE, "", "<skeleton>", "    <bones>"]
    for b in bones:
        parent = b.parent.name if b.parent is not None else ""
        lines.append(f"        <bone name={quoteattr(b.name)} parent={quoteattr(parent)}/>")
    lines += ["    </bones>", "    <init_pose>"]
    for b in bones:
        lines.append(f"        <transform name={quoteattr(b.name)} {matrix_attrs(b.matrix_local)}/>")
    lines += ["    </init_pose>", "</skeleton>"]
    write_lines(filepath, lines)


def write_animation_xml(context, arm, action, filepath):
    """Sample the action at every whole frame of its range; bone matrices are armature space, as the game wants."""
    anim = arm.animation_data
    previous = anim.action if anim is not None else None
    previous_frame = context.scene.frame_current
    assign_action(arm, action)
    start, end = action.frame_range
    bones = sorted(arm.pose.bones, key=lambda b: b.name)
    lines = [XML_HEADER, "", ANIMATION_DOCTYPE, "", "<animation>"]
    for index, frame in enumerate(range(int(round(start)), int(round(end)) + 1)):
        context.scene.frame_set(frame)
        lines.append(f'    <frame index="{index}">')
        for pb in bones:
            lines.append(f"        <transform name={quoteattr(pb.name)} {matrix_attrs(pb.matrix)}/>")
        lines.append("    </frame>")
    lines.append("</animation>")
    write_lines(filepath, lines)
    if previous is not None:
        assign_action(arm, previous)
    context.scene.frame_set(previous_frame)


def clip_keys(action):
    """Hash of every key of the action, to tell a clip edited since loading or publishing."""
    curves = getattr(action, "fcurves", None)
    if curves is None:
        curves = [c for layer in action.layers for strip in layer.strips for bag in strip.channelbags
                  for c in bag.fcurves]
    digest = hashlib.sha1()
    for curve in sorted(curves, key=lambda c: (c.data_path, c.array_index)):
        digest.update(f"{curve.data_path}[{curve.array_index}]".encode("utf-8"))
        for field in ("co", "handle_left", "handle_right"):
            values = np.empty(len(curve.keyframe_points) * 2, np.float32)
            curve.keyframe_points.foreach_get(field, values)
            digest.update(values.tobytes())
    return digest.hexdigest()


def armature_actions(arm):
    """Actions imported for this armature, or failing that the one currently assigned."""
    actions = [a for a in bpy.data.actions if a.get("tt_armature") == arm.name]
    if not actions and arm.animation_data is not None and arm.animation_data.action is not None:
        actions = [arm.animation_data.action]
    return actions


SHOWN_SCALE = 0.01


def shown_bones(frames):
    """Bones not shrunk to nothing on at least one frame; the game hides a held thing by scaling its bone to zero."""
    return sorted({bone for frame in frames for bone, m in frame.items()
                   if max(abs(s) for s in m.to_scale()) > SHOWN_SCALE})


def item_bones(obj):
    """The bones obj follows: the one it hangs off, or for an item that deforms with several, every bone its vertex
    groups weight, the one with the most weight first."""
    bone = obj.parent_bone if obj.parent_type == "BONE" and obj.parent_bone else obj.get("tt_bone")
    if bone:
        return [bone]
    arm = obj.parent if obj.parent is not None and obj.parent.type == "ARMATURE" else None
    if arm is None or obj.type != "MESH":
        return []
    names = [g.name for g in obj.vertex_groups]
    totals = {}
    for v in obj.data.vertices:
        for g in v.groups:
            if g.weight > 0.0 and names[g.group] in arm.data.bones:
                totals[names[g.group]] = totals.get(names[g.group], 0.0) + g.weight
    return sorted(totals, key=lambda b: -totals[b])


def item_clips(arm, obj):
    """The clips in which any of obj's bones shows, by name; None for an item on no bone."""
    bones = item_bones(obj)
    if not bones:
        return None
    return sorted((a for a in armature_actions(arm) if set(bones) & set(list(a.get("tt_shown_bones", ())))),
                  key=lambda a: a.name)


def item_hidden_here(arm, obj):
    """The clips obj shows in when the clip on the rig is not one of them; None otherwise."""
    if arm is None:
        return None
    current = arm.animation_data.action if arm.animation_data is not None else None
    clips = item_clips(arm, obj)
    if current is None or not clips or "tt_shown_bones" not in current or current in clips:
        return None
    return clips


POINT_LABELS = {identifier: label for identifier, label, _ in ATTACHMENT_POINT_ITEMS}


def resolve_attachment_bones(arm):
    """(point, bone) for every attachment point whose bone exists on this armature."""
    names = {b.name for b in arm.data.bones}
    found = []
    for point, _, _ in ATTACHMENT_POINT_ITEMS:
        bone = next((b for b in ATTACHMENT_POINTS.get(point, {}).values() if b in names), None)
        if bone is not None:
            found.append((point, bone))
    return found


def setup_attachment_slots(arm):
    existing = {slot.point for slot in arm.tt_attachments}
    added = 0
    for point, bone in resolve_attachment_bones(arm):
        if point in existing:
            continue
        slot = arm.tt_attachments.add()
        slot.point = point
        slot.bone = bone
        added += 1
    return added


def clip_copy(arm, action):
    """True for a copy made in the Action editor: it carries the original's tt_clip, which belongs to the action
    named after the file, or failing that the first one."""
    clip = action.get("tt_clip")
    sharing = [a for a in armature_actions(arm) if clip and a.get("tt_clip") == clip]
    if action not in sharing:
        return False
    return next((a for a in sharing if a.name == os.path.splitext(clip)[0]), sharing[0]) != action


def clip_short_name(arm, action):
    """walk for an action called peon_walk on the peon rig; the action name otherwise."""
    stem = os.path.splitext(action["tt_clip"])[0] if action.get("tt_clip") else action.name
    prefix = os.path.basename(arm.get("tt_skeleton", "")).replace("skeleton.xml", "")
    return stem[len(prefix):] if prefix and stem.startswith(prefix) and len(stem) > len(prefix) else stem


def item_point(arm, bone):
    return next((point for point, point_bone in resolve_attachment_bones(arm) if point_bone == bone), None)


def body_rig(body):
    return body.parent if body.parent is not None and body.parent.type == "ARMATURE" else None

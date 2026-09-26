"""File > Import and File > Export operators, and Split Mesh by Bone."""

import os
import xml.etree.ElementTree as ET

import bpy
from bpy_extras.io_utils import ImportHelper, ExportHelper
from bpy.props import StringProperty, BoolProperty, CollectionProperty, EnumProperty, FloatProperty
from mathutils import Matrix

from .textures import material_image_name, object_texture
from .mesh_io import (build_mesh, import_mesh_file, mesh_record_from_mesh, replace_mesh_data, rest_pose_armatures,
                      set_vertex_groups, STATIC_BONE, subset_record, write_mesh_xml)
from .rig import (active_armature, apply_clip, armature_actions, armature_from_file, ATTACHMENT_POINT_ITEMS,
                  ATTACHMENT_POINTS, bind_meshes, clip_keys, read_animation, SKELETONS, write_animation_xml,
                  write_skeleton_xml)
from .registry import remember_repo_root
from .scene import REFERENCE_TAG


class ImportTTMesh(bpy.types.Operator, ImportHelper):
    bl_idname = "import_mesh.tt_xml"
    bl_label = "Import Tribal Trouble Mesh"
    bl_options = {"REGISTER", "UNDO"}
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
            if import_mesh_file(context, path, self.flip_v, self.load_textures, self.report) is not None:
                imported += 1
        if imported == 0:
            return {"CANCELLED"}
        remember_repo_root(context, paths[0])
        if len(paths) > 1:
            self.report({"INFO"}, f"Imported {imported} of {len(paths)} meshes")
        return {"FINISHED"}


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
    use_vertex_groups: BoolProperty(name="Skin From Vertex Groups", default=True,
                                    description="With Attach To set to Static, skin vertices by their bone-named "
                                                "vertex groups (imported units keep their weights); vertices "
                                                "without groups get dummy_bone")

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
        else:
            layout.prop(self, "use_vertex_groups")

    def invoke(self, context, event):
        objs = [o for o in context.selected_objects if o.type == "MESH"]
        if len(objs) == 1:
            self.filepath = objs[0].name + ".xml"
        return super().invoke(context, event)

    def resolve_bone(self, obj):
        """Bone to skin the whole object to, or None to use its vertex groups."""
        bone = obj.get("tt_bone")
        if bone:
            return bone
        if self.attach_point == "NONE":
            return None if self.use_vertex_groups else STATIC_BONE
        if self.attach_point == "CUSTOM":
            return self.custom_bone.strip() or STATIC_BONE
        bone = ATTACHMENT_POINTS[self.attach_point].get(self.skeleton)
        if bone is None:
            self.report({"WARNING"}, f"{self.attach_point} does not exist on the {self.skeleton} skeleton; "
                                     f"{obj.name} skinned to {STATIC_BONE}")
            return STATIC_BONE
        return bone

    def execute(self, context):
        objs = [o for o in context.selected_objects if o.type == "MESH" and not o.get(REFERENCE_TAG)]
        active = context.active_object
        if not objs and active is not None and active.type == "MESH" and not active.get(REFERENCE_TAG):
            objs = [active]
        if not objs:
            self.report({"ERROR"}, "Select at least one mesh object to export (added models are never exported)")
            return {"CANCELLED"}

        # Files hold the bind pose: evaluate deforming and carrying armatures at rest, whatever frame is showing.
        arms = rest_pose_armatures(objs)
        previous = {arm: arm.data.pose_position for arm in arms}
        for arm in arms:
            arm.data.pose_position = "REST"
        if arms:
            context.view_layer.update()
        try:
            return self.write(context, objs)
        finally:
            for arm, position in previous.items():
                arm.data.pose_position = position
            if arms:
                context.view_layer.update()

    def write(self, context, objs):
        depsgraph = context.evaluated_depsgraph_get()
        bones = [self.resolve_bone(o) for o in objs]

        if self.batch_per_object and len(objs) > 1:
            out_dir = os.path.dirname(self.filepath)
            for o, bone in zip(objs, bones):
                texture = self.texture or object_texture(o)
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


class ImportTTSkeleton(bpy.types.Operator, ImportHelper):
    """Import a *_skeleton.xml as an armature and/or clip files (peon_run.xml ...) as actions on the active armature"""
    bl_idname = "import_scene.tt_skeleton"
    bl_label = "Import Tribal Trouble Skeleton / Animation"
    bl_options = {"REGISTER", "UNDO"}
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    files: CollectionProperty(type=bpy.types.OperatorFileListElement, options={"HIDDEN", "SKIP_SAVE"})
    directory: StringProperty(subtype="DIR_PATH", options={"HIDDEN", "SKIP_SAVE"})
    bind_selected: BoolProperty(name="Bind Selected Meshes", default=True,
                                description="Parent selected meshes whose vertex groups match the bones to the "
                                            "new armature, with an Armature modifier")

    def execute(self, context):
        paths = [os.path.join(self.directory, f.name) for f in self.files if f.name] or [self.filepath]
        skeletons, clips = [], []
        for path in paths:
            try:
                tag = ET.parse(path).getroot().tag
            except (ET.ParseError, OSError) as e:
                self.report({"WARNING"}, f"{os.path.basename(path)}: cannot read: {e}")
                continue
            if tag == "skeleton":
                skeletons.append(path)
            elif tag == "animation":
                clips.append(path)
            else:
                self.report({"WARNING"}, f"{os.path.basename(path)}: not a skeleton or animation file")

        meshes = [o for o in context.selected_objects if o.type == "MESH"]
        bound = 0
        if skeletons:
            if len(skeletons) > 1:
                self.report({"WARNING"}, f"Several skeleton files selected; using {os.path.basename(skeletons[0])}")
            arm = armature_from_file(context, skeletons[0])
            remember_repo_root(context, skeletons[0])
            if self.bind_selected:
                bound = bind_meshes(arm, meshes)
        else:
            arm = active_armature(context)
            if arm is None:
                self.report({"ERROR"}, "Select an armature to receive the clips, or include a *_skeleton.xml file")
                return {"CANCELLED"}

        loaded = 0
        for path in clips:
            name = os.path.splitext(os.path.basename(path))[0]
            try:
                action = apply_clip(context, arm, name, read_animation(path))
            except (ValueError, KeyError, TypeError, AttributeError, OSError) as e:
                self.report({"WARNING"}, f"{os.path.basename(path)}: skipped: {e}")
                continue
            action["tt_clip"] = os.path.basename(path)
            action["tt_keys"] = clip_keys(action)
            loaded += 1

        context.view_layer.objects.active = arm
        arm.select_set(True)
        self.report({"INFO"}, f"{arm.name}: {len(arm.data.bones)} bones, {loaded} clip(s), {bound} mesh(es) bound")
        return {"FINISHED"}


class ExportTTSkeleton(bpy.types.Operator, ExportHelper):
    """Export the active armature's rest pose as a skeleton file and its clips as animation files beside it"""
    bl_idname = "export_scene.tt_skeleton"
    bl_label = "Export Tribal Trouble Skeleton / Animation"
    filename_ext = ".xml"
    filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
    export_skeleton: BoolProperty(name="Skeleton", default=True,
                                  description="Write the rest pose and bone hierarchy to the chosen file")
    export_clips: BoolProperty(name="Animation Clips", default=True,
                               description="Write each of the armature's actions as <action name>.xml in the same "
                                           "folder, sampled at every frame of its range")

    def invoke(self, context, event):
        arm = active_armature(context)
        if arm is not None:
            self.filepath = arm.name + "_skeleton.xml"
        return super().invoke(context, event)

    def execute(self, context):
        arm = active_armature(context)
        if arm is None:
            self.report({"ERROR"}, "Select an armature (or a mesh parented to one)")
            return {"CANCELLED"}
        written = []
        if self.export_skeleton:
            write_skeleton_xml(arm, self.filepath)
            written.append(os.path.basename(self.filepath))
        if self.export_clips:
            out_dir = os.path.dirname(self.filepath)
            taken = set()
            # A copy made in the Action editor carries the original's tt_clip: the action still named after that file
            # keeps it, the others fall back to their own names.
            actions = sorted(armature_actions(arm),
                             key=lambda a: os.path.splitext(a.get("tt_clip") or "")[0] != a.name)
            for action in actions:
                path = os.path.join(out_dir, action.get("tt_clip") or action.name + ".xml")
                if os.path.normcase(path) in taken:
                    path = os.path.join(out_dir, action.name + ".xml")
                if os.path.normcase(path) in taken:
                    self.report({"WARNING"}, f"{action.name} not exported: {os.path.basename(path)} is another clip's")
                    continue
                taken.add(os.path.normcase(path))
                write_animation_xml(context, arm, action, path)
                written.append(os.path.basename(path))
        if not written:
            self.report({"WARNING"}, "Nothing selected to export")
            return {"CANCELLED"}
        self.report({"INFO"}, "Exported " + ", ".join(written))
        return {"FINISHED"}


def vertex_group_search(self, context, edit_text):
    o = context.active_object
    if o is None or o.type != "MESH":
        return []
    chosen = edit_text.rpartition(",")[0]
    prefix = chosen + ", " if chosen else ""
    return [prefix + g.name for g in o.vertex_groups]


def split_bone_names(text):
    return [name.strip() for name in text.split(",") if name.strip()]


def split_rule_follows_bones(self, context):
    self.rule = "TOUCHES" if len(split_bone_names(self.bone)) > 1 else "MOSTLY"


class SplitByBone(bpy.types.Operator):
    """Move the faces weighted to some bones into a new object, turning a baked-in held item into an attachment"""
    bl_idname = "object.tt_split_by_bone"
    bl_label = "Split Mesh by Bone"
    bl_options = {"REGISTER", "UNDO"}
    bone: StringProperty(name="Bones", default="", search=vertex_group_search, update=split_rule_follows_bones,
                         description="Vertex groups (bones) whose faces move to the new object, separated by commas")
    rule: EnumProperty(name="Rule", default="MOSTLY", options={"SKIP_SAVE"}, items=(
        ("MOSTLY", "Mostly this bone", "A face moves when every corner carries at least Min Weight on the bones"),
        ("TOUCHES", "Touches these bones", "A face moves when any corner has any weight on any of the bones"),
    ))
    threshold: FloatProperty(name="Min Weight", default=0.5, min=0.0, max=1.0,
                             description="A face moves when every corner carries at least this weight on the bones")
    part_name: StringProperty(name="Name", default="", options={"SKIP_SAVE"},
                              description="Name of the new object; empty names it after the first bone")

    @classmethod
    def poll(cls, context):
        o = context.active_object
        if o is not None and o.type == "MESH" and len(o.vertex_groups) > 0:
            return True
        cls.poll_message_set("Click the unit's mesh, not its bones" if o is not None and o.type == "ARMATURE"
                             else "Needs a mesh with bone weights")
        return False

    def invoke(self, context, event):
        split_rule_follows_bones(self, context)
        return context.window_manager.invoke_props_dialog(self)

    def draw(self, context):
        layout = self.layout
        layout.prop(self, "bone")
        layout.prop(self, "rule")
        if self.rule == "MOSTLY":
            layout.prop(self, "threshold")
        layout.prop(self, "part_name")

    def execute(self, context):
        o = context.active_object
        bones = split_bone_names(self.bone)
        if not bones:
            self.report({"ERROR"}, "Name at least one bone")
            return {"CANCELLED"}
        missing = [b for b in bones if o.vertex_groups.get(b) is None]
        if missing:
            self.report({"ERROR"}, f"{o.name} has no vertex group '{missing[0]}'")
            return {"CANCELLED"}
        rule = self.rule if self.properties.is_property_set("rule") or len(bones) == 1 else "TOUCHES"
        record = mesh_record_from_mesh(o.data, o, Matrix.Identity(4), False, None, True)
        chosen = {o.vertex_groups[b].index for b in bones}
        weight = [sum(g.weight for g in v.groups if g.group in chosen) for v in o.data.vertices]
        if rule == "TOUCHES":
            part_faces = [i for i, face in enumerate(record.faces) if any(weight[vi] > 0.0 for vi in face)]
        else:
            part_faces = [i for i, face in enumerate(record.faces) if all(weight[vi] >= self.threshold for vi in face)]
        if not part_faces:
            self.report({"WARNING"}, f"No face has every corner weighted {self.threshold:g} or more to {', '.join(bones)}"
                        if rule == "MOSTLY" else f"No face has any weight on {', '.join(bones)}")
            return {"CANCELLED"}
        moving = set(part_faces)
        rest_faces = [i for i in range(len(record.faces)) if i not in moving]

        part_name = self.part_name.strip() or f"{o.name}_{bones[0].split()[-1]}"
        part_record = subset_record(record, part_faces)
        part = bpy.data.objects.new(part_name, build_mesh(part_name, part_record))
        for m in o.data.materials:
            part.data.materials.append(m)
        context.collection.objects.link(part)
        part.matrix_world = o.matrix_world.copy()
        if o.parent is not None:
            part.parent = o.parent
            part.parent_type = o.parent_type
            part.parent_bone = o.parent_bone
            part.matrix_parent_inverse = o.matrix_parent_inverse.copy()
        for modifier in o.modifiers:
            if modifier.type == "ARMATURE":
                part.modifiers.new(modifier.name, "ARMATURE").object = modifier.object
        if "tt_texture" in o:
            part["tt_texture"] = o["tt_texture"]
        set_vertex_groups(part, part_record.skins)

        replace_mesh_data(o, subset_record(record, rest_faces), o.data.name)

        for other in context.selected_objects:
            other.select_set(False)
        part.select_set(True)
        context.view_layer.objects.active = part
        self.report({"INFO"}, f"Moved {len(part_faces)} of {len(record.faces)} faces to {part.name}")
        return {"FINISHED"}


def menu_import(self, context):
    self.layout.operator(ImportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")
    self.layout.operator(ImportTTSkeleton.bl_idname, text="Tribal Trouble Skeleton / Animation (.xml)")


def menu_export(self, context):
    self.layout.operator(ExportTTMesh.bl_idname, text="Tribal Trouble Mesh (.xml)")
    self.layout.operator(ExportTTSkeleton.bl_idname, text="Tribal Trouble Skeleton / Animation (.xml)")


def menu_object(self, context):
    self.layout.separator()
    self.layout.operator(SplitByBone.bl_idname)

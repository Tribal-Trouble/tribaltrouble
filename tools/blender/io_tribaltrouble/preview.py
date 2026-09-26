"""Preview panel: clips, tiers, material preview, new, saved and deleted clips."""

import os
import re

import bpy
from bpy.props import StringProperty, EnumProperty, FloatProperty, IntProperty

from .textures import apply_team_preview, get_atlas_material, models_texture_path, short_labels
from .rig import active_armature, armature_actions, assign_action, clip_short_name, item_hidden_here, shown_bones
from .registry import GEOMETRY_DIR, read_registry, REGISTRY_FILE, repo_root, rig_registry, set_clip_line
from .scene import has_low_detail, unit_meshes
from .publish import borrowed_rig_problem
from .forms import draw_confirm, form_title, name_problem, open_form


def show_clip(context, arm, action):
    assign_action(arm, action)
    context.scene.frame_start = 1
    context.scene.frame_end = max(1, int(round(action.frame_range[1])))
    context.scene.frame_set(1)


def unit_tiers(arm):
    """The unit body's comma-separated atlas list: one entry per weapon tier. Items follow it, they do not set it."""
    lists = [[t.strip() for t in o["tt_texture"].split(",") if t.strip()] for o in unit_meshes(arm)
             if not o.get("tt_slot")]
    return max(lists, key=len, default=[])


class SetClip(bpy.types.Operator):
    """Show this clip on the unit and fit the timeline to it"""
    bl_idname = "object.tt_set_clip"
    bl_label = "Show Clip"
    clip: StringProperty()

    def execute(self, context):
        arm = active_armature(context)
        action = bpy.data.actions.get(self.clip)
        if arm is None or action is None:
            return {"CANCELLED"}
        show_clip(context, arm, action)
        return {"FINISHED"}


class ShowItemClip(bpy.types.Operator):
    """Show a clip this item appears in"""
    bl_idname = "object.tt_show_item_clip"
    bl_label = "Show Item's Clip"
    item: StringProperty()

    @classmethod
    def description(cls, context, properties):
        arm = active_armature(context)
        obj = bpy.data.objects.get(properties.item)
        clips = item_hidden_here(arm, obj) if arm is not None and obj is not None else None
        if not clips:
            return cls.__doc__
        names = ", ".join(clip_short_name(arm, a) for a in clips)
        return f"Hidden in {clip_short_name(arm, arm.animation_data.action)}. Only visible in: {names}"

    def execute(self, context):
        arm = active_armature(context)
        obj = bpy.data.objects.get(self.item)
        clips = item_hidden_here(arm, obj) if arm is not None and obj is not None else None
        if not clips:
            return {"CANCELLED"}
        show_clip(context, arm, clips[0])
        return {"FINISHED"}


class SetTier(bpy.types.Operator):
    """Show the unit, and every attachment that has one texture per tier, in this tier's texture"""
    bl_idname = "object.tt_set_tier"
    bl_label = "Show Tier"
    bl_options = {"REGISTER", "UNDO"}
    index: IntProperty()

    def execute(self, context):
        arm = active_armature(context)
        root = repo_root(context)
        if arm is None or not root:
            return {"CANCELLED"}
        for obj in unit_meshes(arm):
            names = [t.strip() for t in obj["tt_texture"].split(",") if t.strip()]
            if self.index >= len(names):
                continue
            path = models_texture_path(root, names[self.index])
            if not os.path.isfile(path) and "tt_" + names[self.index] not in bpy.data.materials:
                self.report({"WARNING"}, f"{names[self.index]}.png is not in assets/textures/models")
                continue
            obj.data.materials.clear()
            obj.data.materials.append(get_atlas_material(names[self.index], path))
        arm["tt_tier"] = self.index
        apply_team_preview(context)
        return {"FINISHED"}


class MaterialPreview(bpy.types.Operator):
    """Team color only shows in Material Preview shading; the first switch can take a minute to compile"""
    bl_idname = "view3d.tt_material_preview"
    bl_label = "Switch To Material Preview"

    def execute(self, context):
        context.space_data.shading.type = "MATERIAL"
        return {"FINISHED"}


class VIEW3D_PT_tt_preview(bpy.types.Panel):
    bl_label = "Preview"
    bl_order = 3
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def draw(self, context):
        layout = self.layout
        wm = context.window_manager
        arm = active_armature(context)
        actions = sorted(armature_actions(arm), key=lambda a: a.name)
        current = arm.animation_data.action if arm.animation_data is not None else None
        if actions:
            grid = layout.grid_flow(row_major=True, columns=3, even_columns=True, align=True)
            for action, label in zip(actions, short_labels([a.name for a in actions])):
                grid.operator(SetClip.bl_idname, text=label, depress=action == current).clip = action.name
            playing = context.screen.is_animation_playing
            layout.operator("screen.animation_play", text="Pause" if playing else "Play",
                            icon="PAUSE" if playing else "PLAY")
        row = layout.row(align=True)
        row.operator(NewClip.bl_idname, icon="ADD")
        row.operator(DeleteClip.bl_idname, text="", icon="TRASH")
        tiers = unit_tiers(arm)
        if len(tiers) > 1:
            row = layout.row(align=True)
            for index, label in enumerate(short_labels(tiers)):
                row.operator(SetTier.bl_idname, text=label, depress=arm.get("tt_tier", 0) == index).index = index
        if has_low_detail():
            layout.row(align=True).prop(wm, "tt_detail", expand=True)
        row = layout.row(align=True)
        row.prop(wm, "tt_team_preview", toggle=True)
        row.prop(wm, "tt_team_color", text="")
        if wm.tt_team_preview and context.space_data.shading.type not in ("MATERIAL", "RENDERED"):
            layout.operator(MaterialPreview.bl_idname, icon="SHADING_TEXTURE")


class DeleteClip(bpy.types.Operator):
    """Delete this clip. One that was never saved just goes. A saved one also leaves geometry.xml and its file is
    deleted; only the last clip in the list can go, because the game finds clips by their number"""
    bl_idname = "object.tt_delete_clip"
    bl_label = "Delete Clip"
    bl_options = {"REGISTER"}
    clip: StringProperty(options={"SKIP_SAVE"})

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def target(self, context):
        arm = active_armature(context)
        current = arm.animation_data.action if arm.animation_data is not None else None
        return bpy.data.actions.get(self.clip) if self.clip else current

    def invoke(self, context, event):
        return context.window_manager.invoke_confirm(self, event) if self.target(context) is not None \
            else {"CANCELLED"}

    def execute(self, context):
        arm = active_armature(context)
        action = self.target(context)
        if action is None:
            return {"CANCELLED"}
        root = repo_root(context)
        group, base, rig = rig_registry(context, arm)
        saved = next((name for name, (_, _, path) in (rig["clip_info"].items() if rig else [])
                      if action.get("tt_clip") and os.path.basename(path) == action["tt_clip"]), None)
        label = clip_short_name(arm, action)
        if saved is not None:
            borrowed = borrowed_rig_problem(arm, base)
            if borrowed is not None:
                self.report({"ERROR"}, borrowed)
                return {"CANCELLED"}
            if list(rig["clip_info"])[-1] != saved:
                self.report({"ERROR"}, f"Only the last clip ({list(rig['clip_info'])[-1]}) can be deleted: the game "
                                       f"finds clips by their number, and removing {saved} would shift the ones after it")
                return {"CANCELLED"}
            relative = rig["clip_info"][saved][2]
            touched = set_clip_line(os.path.join(root, REGISTRY_FILE), group, rig["skeleton"].replace("\\", "/"), saved)
            still_used = any(relative in s["clips"] for s in read_registry(root))
            path = os.path.join(root, GEOMETRY_DIR, relative)
            if not still_used and os.path.isfile(path):
                os.remove(path)
            self.report({"INFO"}, f"Deleted clip {saved} from {', '.join(touched)} and removed {relative}")
        else:
            self.report({"INFO"}, f"Deleted unsaved clip {label}")
        others = [a for a in armature_actions(arm) if a != action]
        fallback = next((a for a in others if "idle" in a.name), others[0] if others else None)
        if arm.animation_data is not None and arm.animation_data.action == action:
            arm.animation_data.action = None
            if fallback is not None:
                assign_action(arm, fallback)
        bpy.data.actions.remove(action)
        return {"FINISHED"}


def clip_button_menu(self, context):
    """Right click on a clip button."""
    op = getattr(context, "button_operator", None)
    if op is not None and op.bl_rna.identifier == "OBJECT_OT_tt_set_clip":
        self.layout.separator()
        self.layout.operator(DeleteClip.bl_idname, icon="TRASH").clip = op.clip


CLIP_KINDS = (("loop", "Looping", "Repeats, like idle and run"),
              ("plain", "Once", "Plays once and holds, like attack and die"))
WPC_DESCRIPTION = ("For a walk or run: how far the unit travels in one loop, so feet do not slide. Leave at 1 for "
                   "anything that stays in place")


class NewClip(bpy.types.Operator):
    """Start a new clip for this unit. Game clips have a key on every frame for every bone, which is miserable to
    edit by hand, so the default starts from the pose on screen with only a first and a last key"""
    bl_idname = "object.tt_new_clip"
    bl_label = "New Clip"
    bl_options = {"REGISTER", "UNDO"}
    clip_name: StringProperty(name="Name", default="", description="Short name such as dance or wave")
    start: EnumProperty(name="Start From", items=(
        ("POSE", "This pose", "The pose on screen, keyed on the first and last frame only: easy to animate by hand"),
        ("COPY", "Copy of this clip", "Every frame of the clip showing: good for small fixes, hard to re-animate")))
    length: IntProperty(name="Frames", default=25, min=2, max=500, description="Length of a clip started from a pose")
    kind: EnumProperty(name="Plays", items=CLIP_KINDS)
    wpc: FloatProperty(name="Distance Per Loop", default=1.0, min=0.0001, description=WPC_DESCRIPTION)

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def invoke(self, context, event):
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        for prop in ("clip_name", "start", "length", "kind", "wpc"):
            layout.prop(self, prop)
        draw_confirm(layout, self, name_problem(self.clip_name))

    def execute(self, context):
        arm = active_armature(context)
        name = self.clip_name.strip().lower()
        if not re.fullmatch(r"[a-z0-9_]+", name):
            self.report({"ERROR"}, "Name the clip with letters, digits and underscores")
            return {"CANCELLED"}
        rig = rig_registry(context, arm)[2]
        taken = {clip_short_name(arm, a) for a in armature_actions(arm)} | set(rig["clip_info"] if rig else ())
        if name in taken:
            self.report({"ERROR"}, f"This unit already has a clip called {name}")
            return {"CANCELLED"}
        current = arm.animation_data.action if arm.animation_data is not None else None
        prefix = os.path.basename(arm.get("tt_skeleton", "")).replace("skeleton.xml", "")
        from_pose = self.start == "POSE" or current is None
        action = bpy.data.actions.new(prefix + name) if from_pose else current.copy()
        action.name = prefix + name
        action.use_fake_user = True
        action["tt_armature"] = arm.name
        if "tt_clip" in action:
            del action["tt_clip"]
        action["tt_kind"], action["tt_wpc"] = self.kind, self.wpc
        if arm.animation_data is not None:
            arm.animation_data.action = None  # a copied action brings its own slot; let assign_action bind it
        assign_action(arm, action)
        if from_pose:
            # Pose bones still hold the values of the pose that was showing.
            for pb in arm.pose.bones:
                pb.rotation_mode = "QUATERNION"
                for frame in (1, self.length):
                    for path in ("location", "rotation_quaternion", "scale"):
                        pb.keyframe_insert(path, frame=frame, group=pb.name)
            action["tt_shown_bones"] = shown_bones([{pb.name: pb.matrix for pb in arm.pose.bones}])
        show_clip(context, arm, action)
        return {"FINISHED"}


"""By hand panel: attachment points, snippets and exports done one step at a time."""

import os

import bpy

from .rig import active_armature, setup_attachment_slots
from .registry import append_registry_entries, find_base_sprite, registry_entries, REGISTRY_FILE, repo_root, rig_in_repo
from .scene import visible_attachments
from .publish import export_visible


class SetupAttachments(bpy.types.Operator):
    """Create a slot for every attachment point this armature has bones for"""
    bl_idname = "object.tt_setup_attachments"
    bl_label = "Set Up Attachment Points"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def execute(self, context):
        arm = active_armature(context)
        added = setup_attachment_slots(arm)
        self.report({"INFO"}, f"{arm.name}: {len(arm.tt_attachments)} attachment points ({added} new)")
        return {"FINISHED"}


class ExportAttachments(bpy.types.Operator):
    """Export every visible attachment as its own mesh file skinned to its bone"""
    bl_idname = "export_mesh.tt_attachments"
    bl_label = "Export Visible Attachments"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(visible_attachments(arm))

    def execute(self, context):
        objs = visible_attachments(active_armature(context))
        for o in context.selected_objects:
            o.select_set(False)
        for o in objs:
            o.select_set(True)
        context.view_layer.objects.active = objs[0]
        return bpy.ops.export_mesh.tt_xml("INVOKE_DEFAULT", batch_per_object=True)


class CopyRegistrySnippet(bpy.types.Operator):
    """Copy geometry.xml sprite entries for the visible attachments to the clipboard"""
    bl_idname = "object.tt_registry_snippet"
    bl_label = "Copy Registry Snippet"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(visible_attachments(arm))

    def execute(self, context):
        arm = active_armature(context)
        group, base = find_base_sprite(arm.get("tt_skeleton", ""))
        if base is None:
            base = "BASE_SPRITE"
            self.report({"WARNING"}, "Could not find the unit's sprite in geometry.xml; fill in base by hand")
        entries = [text for _, text in registry_entries(context, arm, base, group=group)]
        context.window_manager.clipboard = "\n".join(entries) + "\n"
        where = f"group {group}" if group else "the unit's group"
        self.report({"INFO"}, f"Copied {len(entries)} sprite entr{'y' if len(entries) == 1 else 'ies'} for {where}")
        return {"FINISHED"}


class VIEW3D_PT_tt_attachments_more(bpy.types.Panel):
    bl_label = "By hand"
    bl_order = 5
    bl_parent_id = "VIEW3D_PT_tt_attachments"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"
    bl_options = {"DEFAULT_CLOSED"}

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def draw(self, context):
        col = self.layout.column(align=True)
        col.operator(SetupAttachments.bl_idname, text="Refresh Points")
        col.operator(ExportToRepo.bl_idname)
        col.operator(AddToRegistry.bl_idname)
        col.operator(ExportAttachments.bl_idname)
        col.operator(CopyRegistrySnippet.bl_idname)


class ExportToRepo(bpy.types.Operator):
    """Write every visible attachment into the unit's folder in the repo: registry items back to their own
    file, new items next to the unit as <object name>.xml"""
    bl_idname = "export_mesh.tt_to_repo"
    bl_label = "Export Visible To Repo"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return arm is not None and bool(arm.get("tt_skeleton")) and bool(repo_root(context))

    def execute(self, context):
        return {"FINISHED"} if export_visible(context, active_armature(context), self.report) else {"CANCELLED"}


class AddToRegistry(bpy.types.Operator):
    """Write the visible new attachments into geometry.xml so the game and this browser pick them up"""
    bl_idname = "object.tt_add_to_registry"
    bl_label = "Add To Registry"

    @classmethod
    def poll(cls, context):
        arm = active_armature(context)
        return rig_in_repo(context, arm) and bool(visible_attachments(arm))

    def execute(self, context):
        arm = active_armature(context)
        group, base = find_base_sprite(arm.get("tt_skeleton", ""))
        if base is None:
            self.report({"ERROR"}, "Could not find this unit's sprite in geometry.xml")
            return {"CANCELLED"}
        added = append_registry_entries(os.path.join(repo_root(context), REGISTRY_FILE), group,
                                        registry_entries(context, arm, base, group=group))
        self.report({"INFO"}, f"Added {added} sprite entr{'y' if added == 1 else 'ies'} to group {group}")
        return {"FINISHED"}

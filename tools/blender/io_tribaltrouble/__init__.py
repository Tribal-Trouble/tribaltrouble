"""Blender import/export addon for Tribal Trouble mesh XML files.

Install: copy this folder into Blender's add-ons folder and enable it (tools/blender/README.md has the steps).
Import: File > Import > Tribal Trouble Mesh (.xml), or Skeleton / Animation (.xml)
Export: File > Export > Tribal Trouble Mesh (.xml), or Skeleton / Animation (.xml)
Panels: 3D view sidebar, "Tribal Trouble" tab.

Skeleton and clip files hold absolute model-space 4x4 matrices per bone (m<column><row>,
translation in m30..m32). Import builds an armature whose rest pose equals the file's and
solves each pose bone's basis from the file matrices; export samples pose_bone.matrix per
frame. tools/blender/validate_roundtrip.py is the numeric gate for that path.

Static props (plants, rocks, a torch) have no skeleton: every vertex is skinned
to "dummy_bone" with weight 1, which is what the exporter writes when an object
has no bone-named vertex groups. Attachments (hats, held items) are the same
thing skinned to one unit bone instead: pick an attachment point in the export
dialog, or set a tt_bone property on the object. Model attachments in the unit's
bind pose. Skinned units keep their weights as vertex groups and export from
them, always in the rest pose; Split Mesh by Bone separates a baked-in held
item into its own object.

The game is Z-up like Blender, so no axis conversion is needed. If the texture
looks vertically flipped on an imported model, re-import with "Flip V" checked
and also check it on export.
"""

import bpy
from bpy.props import StringProperty, BoolProperty, CollectionProperty, EnumProperty, IntProperty, FloatVectorProperty

from .textures import team_preview_update
from .registry import CATEGORY_ITEMS
from .scene import (DETAIL_ITEMS, detail_update, loaded_body, refresh_units_on_load, root_update, TTAttachmentSlot,
                    unit_index_update)
from .forms import NewEvent
from .import_export import (ExportTTMesh, ExportTTSkeleton, ImportTTMesh, ImportTTSkeleton, menu_export, menu_import,
                            menu_object, SplitByBone)
from . import autosave
from .glow import AddGlow, RemoveGlow
from .models import (AddToScene, EditScatter, LoadUnit, PickUnit, Preflight, PublishModel, RefreshUnits,
                     RegisterModel, RemoveAdded, RemoveFromRegistry, TT_UL_units, TTCheck, TTPreferences, TTUnitEntry,
                     units_list_menu, UpdateAddon, VIEW3D_PT_tt_units)
from .preview import (clip_button_menu, DeleteClip, MaterialPreview, NewClip, SetClip, SetTier, ShowItemClip,
                      VIEW3D_PT_tt_preview)
from .by_hand import (AddToRegistry, CopyRegistrySnippet, ExportAttachments, ExportToRepo, SetupAttachments,
                      VIEW3D_PT_tt_attachments_more)
from .skins import (CancelSkin, NewSkin, PaintSkin, pick_skin_row, PickSkin, SaveSkin, shown_skin_index, ShowSkin,
                    TT_UL_skins, TTSkinEntry, VIEW3D_PT_tt_skins)
from .props import (CloseItem, DonePainting, item_index_update, MakeTexture, NewItem, NewProp, OwnTexture, PaintItem,
                    PutOnBone, ShowItem, TT_UL_items, VIEW3D_PT_tt_attachments)


bl_info = {
    "name": "Tribal Trouble Mesh (.xml)",
    "author": "Tribal Trouble tooling",
    "version": (2, 4, 0),
    "blender": (4, 2, 0),
    "location": "File > Import-Export",
    "description": "Import/export Tribal Trouble geometry XML meshes",
    "category": "Import-Export",
}


classes = (TTPreferences, ImportTTMesh, ExportTTMesh, SplitByBone, ImportTTSkeleton, ExportTTSkeleton, TTAttachmentSlot,
           TTUnitEntry, TT_UL_units, RefreshUnits, LoadUnit, PublishModel, PickUnit, AddToScene, RemoveAdded, ShowItem,
           ShowItemClip, ExportToRepo, AddToRegistry, RegisterModel, EditScatter, AddGlow, RemoveGlow, TTCheck, SetClip,
           SetTier, MaterialPreview, Preflight, RemoveFromRegistry, UpdateAddon, NewEvent, NewProp, ShowSkin, PaintSkin,
           TTSkinEntry, TT_UL_skins, PickSkin, NewSkin, CancelSkin, SaveSkin, CloseItem, NewClip, DeleteClip,
           SetupAttachments, ExportAttachments, CopyRegistrySnippet, MakeTexture, NewItem, OwnTexture, PutOnBone,
           PaintItem, DonePainting, TT_UL_items, VIEW3D_PT_tt_units, VIEW3D_PT_tt_skins, VIEW3D_PT_tt_preview,
           VIEW3D_PT_tt_attachments, VIEW3D_PT_tt_attachments_more)


def register():
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.Object.tt_attachments = CollectionProperty(type=TTAttachmentSlot)
    wm = bpy.types.WindowManager
    wm.tt_repo_root = StringProperty(name="Repo Folder", subtype="DIR_PATH", update=root_update)
    wm.tt_units = CollectionProperty(type=TTUnitEntry)
    wm.tt_unit_index = IntProperty(update=unit_index_update)
    wm.tt_category = EnumProperty(name="Category", items=CATEGORY_ITEMS, default="UNITS", update=root_update,
                                  description="Which kind of model the list shows")
    wm.tt_checks = CollectionProperty(type=TTCheck)
    wm.tt_detail = EnumProperty(name="Detail", items=DETAIL_ITEMS, update=detail_update,
                                description="Which of the model's meshes shows: the close up one or the one the game "
                                            "draws from far away")
    wm.tt_checked = BoolProperty()
    wm.tt_team_preview = BoolProperty(name="Team Color", default=False, update=team_preview_update,
                                      description="Blend the player's color in through the team decal, as in game")
    wm.tt_team_color = FloatVectorProperty(name="Team Color", subtype="COLOR", size=3, min=0.0, max=1.0,
                                           default=(0.8, 0.1, 0.1), update=team_preview_update)
    wm.tt_item_index = IntProperty(update=item_index_update)
    wm.tt_open_item = StringProperty()
    wm.tt_skins = CollectionProperty(type=TTSkinEntry)
    wm.tt_skin_index = IntProperty(get=lambda wm: shown_skin_index(wm.tt_skins, loaded_body()),
                                   set=lambda wm, index: pick_skin_row(wm.tt_skins, index))
    wm.tt_item_skins = CollectionProperty(type=TTSkinEntry)
    wm.tt_item_skin_index = IntProperty(
        get=lambda wm: shown_skin_index(wm.tt_item_skins, bpy.data.objects.get(wm.tt_open_item)),
        set=lambda wm, index: pick_skin_row(wm.tt_item_skins, index))
    wm.tt_item_search = StringProperty(name="Search", options={"TEXTEDIT_UPDATE"},
                                       description="Show only the items whose name contains this")
    bpy.app.handlers.load_post.append(refresh_units_on_load)
    bpy.app.timers.register(refresh_units_on_load, first_interval=0.5)
    autosave.register()
    bpy.types.TOPBAR_MT_file_import.append(menu_import)
    bpy.types.TOPBAR_MT_file_export.append(menu_export)
    bpy.types.VIEW3D_MT_object.append(menu_object)
    if hasattr(bpy.types, "UI_MT_button_context_menu"):
        bpy.types.UI_MT_button_context_menu.append(clip_button_menu)
        bpy.types.UI_MT_button_context_menu.append(units_list_menu)


def unregister():
    if hasattr(bpy.types, "UI_MT_button_context_menu"):
        bpy.types.UI_MT_button_context_menu.remove(units_list_menu)
        bpy.types.UI_MT_button_context_menu.remove(clip_button_menu)
    bpy.types.VIEW3D_MT_object.remove(menu_object)
    bpy.types.TOPBAR_MT_file_import.remove(menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(menu_export)
    bpy.app.handlers.load_post.remove(refresh_units_on_load)
    autosave.unregister()
    del bpy.types.Object.tt_attachments
    for name in ("tt_repo_root", "tt_units", "tt_unit_index", "tt_category", "tt_checks",
                 "tt_checked", "tt_team_preview", "tt_team_color",
                 "tt_item_index", "tt_detail", "tt_item_search",
                 "tt_open_item", "tt_skins", "tt_skin_index", "tt_item_skins", "tt_item_skin_index"):
        delattr(bpy.types.WindowManager, name)
    for cls in classes:
        bpy.utils.unregister_class(cls)

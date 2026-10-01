"""Models panel: the model list, load, publish, new model, checks and the add-on update button."""

import os
import re
import shutil
import sys

import bpy
from bpy.props import StringProperty, BoolProperty, EnumProperty, IntProperty

from .textures import ensure_emission_in_repo, ensure_texture_in_repo, material_image_name
from .mesh_io import STATIC_BONE, write_mesh_xml
from .rig import active_armature, armature_actions, body_rig, write_animation_xml, write_skeleton_xml
from .registry import (append_registry_entries, CATEGORY_ICONS, GEOMETRY_DIR, read_registry, REGISTRY_FILE,
                       remove_registry_entry, repo_root, rig_registry, root_holder, SCENERY_GROUP, sprite_skins,
                       set_sprite_attributes, sprite_text, team_attribute, emissive_attribute)
from .scene import (add_reference, attachment_obj_poll, BROWSER_TAG, clear_references, has_low_detail, load_unit,
                    loaded_body, loaded_models, REFERENCE_TAG, references, refresh_units, root_update,
                    unpublished_changes, write_changed)
from .publish import (check_mesh, preflight, publish_changed_clips, publish_items, publish_own_textures,
                      publish_glow, publish_paint, store_findings, texture_clashes)
from .glow import draw_glow
from .forms import (chosen_event, draw_confirm, draw_event, event_property, form_title, mesh_problem, name_problem,
                    NO_EVENT, open_form, own_mesh_search)


def draw_checks(layout, wm):
    if wm.tt_checked:
        box = layout.box()
        if not wm.tt_checks:
            box.label(text="Nothing to fix", icon="CHECKMARK")
        for check in wm.tt_checks:
            box.label(text=check.name, icon=CHECK_ICONS.get(check.level, "INFO"))


class TTPreferences(bpy.types.AddonPreferences):
    bl_idname = __package__
    repo_root: StringProperty(name="Repo Folder", subtype="DIR_PATH", update=root_update,
                              description="Your tribaltrouble checkout (the folder that holds assets)")

    def draw(self, context):
        draw_update_button(self.layout, context)
        self.layout.prop(self, "repo_root")


class TTUnitEntry(bpy.types.PropertyGroup):
    group: StringProperty()
    sprite: StringProperty()
    category: StringProperty()


class TT_UL_units(bpy.types.UIList):
    def draw_item(self, context, layout, data, item, icon, active_data, active_property, index):
        # A button, not a label, so a right click knows which row it is on.
        row = layout.row()
        name = row.row()
        name.alignment = "LEFT"
        pick = name.operator(PickUnit.bl_idname, text=item.name, icon=CATEGORY_ICONS.get(item.category, "MESH_CUBE"),
                             emboss=False)
        pick.group, pick.sprite = item.group, item.sprite


class PickUnit(bpy.types.Operator):
    """Load this model. Right click for Add To Scene"""
    bl_idname = "wm.tt_pick_unit"
    bl_label = "Pick Model"
    bl_options = {"REGISTER", "UNDO"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    def on_screen(self):
        body = loaded_body()
        return body is not None and (body["tt_group"], body["tt_sprite"]) == (self.group, self.sprite)

    def invoke(self, context, event):
        changes = [] if self.on_screen() else unpublished_changes(context)
        if changes:
            return context.window_manager.invoke_confirm(
                self, event, title=f"Load {self.sprite}?", confirm_text="Load", icon="WARNING",
                message=f"Not published on {loaded_body()['tt_sprite']}: {', '.join(changes)}")
        return self.execute(context)

    def execute(self, context):
        wm = context.window_manager
        index = next((i for i, u in enumerate(wm.tt_units) if (u.group, u.sprite) == (self.group, self.sprite)), -1)
        if index < 0:
            return {"CANCELLED"}
        if not self.on_screen() and load_unit(context, self.group, self.sprite, self.report) is None:
            refresh_units(context)
            return {"CANCELLED"}
        wm.tt_unit_index = index  # the model is loaded already, so this only moves the highlight
        return {"FINISHED"}


class RefreshUnits(bpy.types.Operator):
    """Read geometry.xml again"""
    bl_idname = "wm.tt_refresh_units"
    bl_label = "Refresh"

    def execute(self, context):
        if not repo_root(context):
            self.report({"ERROR"}, "Set the repo folder first: it must contain assets/geometry/geometry.xml")
            return {"CANCELLED"}
        self.report({"INFO"}, f"{refresh_units(context)} models listed")
        return {"FINISHED"}


class LoadUnit(bpy.types.Operator):
    """Load the model picked in the list again from its files, dropping unsaved edits"""
    bl_idname = "wm.tt_load_unit"
    bl_label = "Reload"
    bl_options = {"REGISTER", "UNDO"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    def execute(self, context):
        wm = context.window_manager
        group, sprite = self.group, self.sprite
        if not sprite:
            if not 0 <= wm.tt_unit_index < len(wm.tt_units):
                return {"CANCELLED"}
            group, sprite = wm.tt_units[wm.tt_unit_index].group, wm.tt_units[wm.tt_unit_index].sprite
        if not repo_root(context):
            self.report({"ERROR"}, "Set the repo folder first")
            return {"CANCELLED"}
        return {"FINISHED"} if load_unit(context, group, sprite, self.report) is not None else {"CANCELLED"}


class PublishModel(bpy.types.Operator):
    """Save everything you changed on the model loaded from the list to the repo: every detail level, its items and
    props (new ones are listed in geometry.xml), its clips and painted textures. Files you did not touch are left
    alone"""
    bl_idname = "wm.tt_publish_model"
    bl_label = "Publish"

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context)) and bool(loaded_models())

    def execute(self, context):
        arm = next((o for o in bpy.data.objects if o.type == "ARMATURE" and o.get(BROWSER_TAG)), None)
        added = []
        if arm is not None and any(x.obj is not None and x.visible for x in arm.tt_attachments):
            added = publish_items(context, arm, self.report)
            if added is None:
                return {"CANCELLED"}
        written = write_changed(context, arm, {o: o["tt_source"] for o in loaded_models()})
        publish_own_textures(repo_root(context), loaded_models())
        painted = publish_paint(repo_root(context)) + publish_glow(repo_root(context))
        clips = publish_changed_clips(context, arm, self.report) if arm is not None else []
        saved = added + [os.path.basename(o["tt_source"]) for o in written] + painted + clips
        self.report({"INFO"}, f"Saved {', '.join(saved)}" if saved else "Nothing changed since loading")
        return {"FINISHED"}


class AddToScene(bpy.types.Operator):
    """Put this model beside what is on screen, at the same scale, to size against or compose with. Move it as you
    like; it is never published and stays when you load another model"""
    bl_idname = "wm.tt_add_to_scene"
    bl_label = "Add To Scene"
    bl_options = {"REGISTER", "UNDO"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context))

    def execute(self, context):
        if not any((u.group, u.sprite) == (self.group, self.sprite) for u in context.window_manager.tt_units):
            self.report({"ERROR"}, f"No model {self.group} / {self.sprite} in the list")
            return {"CANCELLED"}
        if context.mode != "OBJECT" and context.view_layer.objects.active is not None:
            bpy.ops.object.mode_set(mode="OBJECT")
        return {"FINISHED"} if add_reference(context, self.group, self.sprite, self.report) else {"CANCELLED"}


class RemoveAdded(bpy.types.Operator):
    """Remove every model added to the scene from the Models list"""
    bl_idname = "wm.tt_remove_added"
    bl_label = "Remove Added Objects"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return bool(references())

    def execute(self, context):
        clear_references()
        return {"FINISHED"}


def units_list_menu(self, context):
    """Right click on a row of the Models list."""
    op = getattr(context, "button_operator", None)
    if op is None or op.bl_rna.identifier != "WM_OT_tt_pick_unit":
        return
    layout = self.layout
    layout.separator()
    add = layout.operator(AddToScene.bl_idname, text=f"Add To Scene: {op.sprite}", icon="ADD")
    add.group, add.sprite = op.group, op.sprite
    layout.operator(RemoveAdded.bl_idname, icon="X")


CHECK_ICONS = {"ERROR": "CANCEL", "WARNING": "ERROR", "INFO": "INFO"}


class TTCheck(bpy.types.PropertyGroup):
    level: StringProperty()


class Preflight(bpy.types.Operator):
    """Check the visible items for what breaks in game: UVs, texture, naming, tint, weights, size"""
    bl_idname = "object.tt_preflight"
    bl_label = "Check Before Export"

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def execute(self, context):
        findings = preflight(context, active_armature(context))
        errors = store_findings(context, findings)
        self.report({"ERROR"} if errors else {"INFO"},
                    f"{errors} problem(s) to fix" if errors else f"Ready to export, {len(findings)} note(s)")
        return {"FINISHED"}


class RemoveFromRegistry(bpy.types.Operator):
    """Take this sprite out of geometry.xml. Its files stay on disk. Refused while another sprite is based on it or
    is a skin of it"""
    bl_idname = "object.tt_remove_from_registry"
    bl_label = "Remove From Registry"
    bl_options = {"REGISTER"}
    group: StringProperty(options={"SKIP_SAVE"})
    sprite: StringProperty(options={"SKIP_SAVE"})

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context))

    def invoke(self, context, event):
        wm = context.window_manager
        if not self.sprite and 0 <= wm.tt_unit_index < len(wm.tt_units):
            self.group, self.sprite = wm.tt_units[wm.tt_unit_index].group, wm.tt_units[wm.tt_unit_index].sprite
        return wm.invoke_confirm(self, event)

    def execute(self, context):
        root = repo_root(context)
        registry = read_registry(root)
        dependents = [s["name"] for s in registry if s["group"] == self.group and s["base"] == self.sprite]
        if dependents:
            self.report({"ERROR"}, f"{self.sprite} is the base of {', '.join(dependents)}: remove those first")
            return {"CANCELLED"}
        skins = [s["name"] for s in sprite_skins(registry, {"group": self.group, "name": self.sprite})]
        if skins:
            self.report({"ERROR"}, f"{self.sprite} has the skins {', '.join(skins)}: remove those first")
            return {"CANCELLED"}
        if not remove_registry_entry(os.path.join(root, REGISTRY_FILE), self.group, self.sprite):
            self.report({"ERROR"}, f"No sprite named {self.sprite} in group {self.group}")
            return {"CANCELLED"}
        for obj in [o for o in bpy.data.objects if o.get("tt_skin") == self.sprite and not o.get("tt_detail")]:
            bpy.ops.object.tt_show_skin(item=obj.name)
        for obj in [o for o in bpy.data.objects if o.get("tt_sprite") == self.sprite and o.get(BROWSER_TAG)
                    and o.get("tt_group", "") in ("", self.group)]:
            bpy.data.objects.remove(obj, do_unlink=True)
        refresh_units(context)
        self.report({"INFO"}, f"Removed {self.group} / {self.sprite} from the registry; its files are still on disk")
        return {"FINISHED"}


ADDON_SOURCE = os.path.join("tools", "blender", "io_tribaltrouble")
ADDON_DIR = os.path.dirname(os.path.abspath(__file__))
OLD_SINGLE_FILE = "io_tribaltrouble.py"  # how 1.x was installed; Blender cannot load it next to this folder
_repo_version_cache = {}


def version_from_source(path):
    """bl_info version of an add-on's __init__.py, read as text so nothing gets imported."""
    try:
        with open(path, encoding="utf-8") as f:
            match = re.search(r'"version":\s*\((\d+),\s*(\d+),\s*(\d+)\)', f.read(20000))
    except OSError:
        return None
    return tuple(int(g) for g in match.groups()) if match else None


def update_available(context):
    """Version of the add-on in the repo folder when it is newer than this installed copy, else None."""
    root = repo_root(context)
    if not root:
        return None
    source = os.path.join(root, ADDON_SOURCE, "__init__.py")
    installed_source = os.path.join(ADDON_DIR, "__init__.py")
    if os.path.normcase(os.path.abspath(source)) == os.path.normcase(installed_source):
        return None
    try:
        stamp = os.path.getmtime(source)
    except OSError:
        return None
    if _repo_version_cache.get(source, (None,))[0] != stamp:
        _repo_version_cache[source] = (stamp, version_from_source(source))
    version = _repo_version_cache[source][1]
    # Blender removes bl_info from extension modules, so the installed version is read from the file's text too.
    if installed_source not in _repo_version_cache:
        _repo_version_cache[installed_source] = (None, version_from_source(installed_source))
    installed = _repo_version_cache[installed_source][1]
    return version if version is not None and installed is not None and version > installed else None


def draw_update_button(layout, context):
    newer = update_available(context)
    if newer is not None:
        row = layout.row()
        row.alert = True
        row.operator(UpdateAddon.bl_idname, text="Update add-on to " + ".".join(map(str, newer)), icon="FILE_REFRESH")


def install_repo_addon(context):
    """Copy the repo's add-on modules over this installed copy. The running code is unchanged until a reload."""
    version = update_available(context)
    if version is not None:
        source = os.path.join(repo_root(context), ADDON_SOURCE)
        for name in os.listdir(source):
            if name.endswith(".py"):
                shutil.copyfile(os.path.join(source, name), os.path.join(ADDON_DIR, name))
        old_single_file = os.path.join(os.path.dirname(ADDON_DIR), OLD_SINGLE_FILE)
        if os.path.isfile(old_single_file):
            os.remove(old_single_file)
    return version


def reload_addon():
    import addon_utils
    addon_utils.disable(__package__)
    # Enabling again would reload only __init__.py, so every module of the add-on is imported afresh.
    for name in [n for n in sys.modules if n == __package__ or n.startswith(__package__ + ".")]:
        del sys.modules[name]
    addon_utils.enable(__package__, default_set=True)
    return None


class UpdateAddon(bpy.types.Operator):
    """Replace the installed add-on with the newer copy in your repo folder and reload it"""
    bl_idname = "wm.tt_update_addon"
    bl_label = "Update Add-on"

    def execute(self, context):
        version = install_repo_addon(context)
        if version is None:
            self.report({"INFO"}, "The installed add-on is already as new as the one in the repo")
            return {"CANCELLED"}
        bpy.app.timers.register(reload_addon, first_interval=0.1)  # not from inside the module being replaced
        self.report({"INFO"}, "Updated to " + ".".join(map(str, version)))
        return {"FINISHED"}


PLAIN_CLIPS = ("attack", "die", "death", "throw")
_group_items = []


def registry_group_items(self, context):
    root = repo_root(context)
    names = sorted({s["group"] for s in read_registry(root)}) if root else []
    _group_items[:] = [(n, n, "") for n in names] or [(SCENERY_GROUP, SCENERY_GROUP, "")]
    return _group_items


# Landscape.Ground names the game scatters decorations on, one checkbox each; land is any of them.
DECORATION_GROUNDS = (
    ("grass", "Grass", ""),
    ("dirt", "Dirt", ""),
    ("beach", "Beach", "Sand on native maps, gravel on viking maps"),
    ("snow", "Snow", "Viking maps only"),
    ("land", "Any land", "Any ground above the sea"),
)


def chosen_terrain(op):
    """The decoration= value a form's terrain checkboxes pick: the ticked grounds, comma separated."""
    if op.land:
        return "land"
    return ",".join(key for key, _, _ in DECORATION_GROUNDS if key != "land" and getattr(op, key))


def draw_terrain(layout, op):
    split = layout.split(factor=0.4)
    split.label(text="Terrain:")
    column = split.column(align=True)
    for key, _, _ in DECORATION_GROUNDS:
        cell = column.row()
        cell.enabled = key == "land" or not op.land
        cell.prop(op, key)


def mesh_names_sprite(self, context):
    self.sprite_name = re.sub(r"[^A-Za-z0-9_]+", "_", self.mesh).strip("_").lower()


class RegisterModel(bpy.types.Operator):
    """Export the picked mesh as a brand new model and add it to geometry.xml: a static building or prop, a unit on
    the rig it is bound to, a unit with its own new rig, or map scenery the game scatters by itself. Using a model
    that is not scattered still needs code"""
    bl_idname = "object.tt_register_model"
    bl_label = "New Model..."
    bl_options = {"REGISTER"}
    mesh: StringProperty(name="Mesh", search=own_mesh_search, options={"SKIP_SAVE"},
                         description="One of your own meshes in this scene", update=mesh_names_sprite)
    sprite_name: StringProperty(name="Name", options={"SKIP_SAVE"},
                                description="Sprite name in geometry.xml and the new folder's name")
    scatter: BoolProperty(name="Scatter on map", options={"SKIP_SAVE"},
                          description="Map scenery the game scatters over every map by itself, such as pumpkin patches")
    group: EnumProperty(name="Group", items=registry_group_items)
    low_detail: StringProperty(name="Low Detail", description="Optional mesh object shown at a distance")
    half_built: StringProperty(name="Half Built", description="Building only: mesh for the half built stage, "
                                                              "registered as <name>_halfbuilt")
    half_built_low: StringProperty(name="Half Built Low Detail")
    start: StringProperty(name="Start", description="Building only: mesh for the construction site, registered "
                                                    "as <name>_start")
    start_low: StringProperty(name="Start Low Detail")
    grass: BoolProperty(name="Grass", default=True)
    dirt: BoolProperty(name="Dirt")
    beach: BoolProperty(name="Beach", description="Sand on native maps, gravel on viking maps")
    snow: BoolProperty(name="Snow", description="Viking maps only")
    land: BoolProperty(name="Any land", description="Any ground above the sea")
    count: IntProperty(name="Count", default=20, min=1, max=1000, description="How many the game scatters over a map")
    event: event_property()

    @classmethod
    def poll(cls, context):
        return bool(repo_root(context))

    def invoke(self, context, event):
        active = context.active_object
        if active is not None and attachment_obj_poll(self, active):
            self.mesh = active.name
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "mesh", icon="MESH_DATA")
        layout.prop(self, "sprite_name")
        layout.prop(self, "scatter")
        if self.scatter:
            draw_terrain(layout, self)
            layout.prop(self, "count")
            draw_event(layout, self)
        else:
            obj = bpy.data.objects.get(self.mesh)
            arm = body_rig(obj) if obj is not None else None
            group, base, _ = rig_registry(context, arm)
            if base is not None:
                layout.label(text=f"Unit on the {group} / {base} rig", icon="ARMATURE_DATA")
            else:
                layout.prop(self, "group")
                layout.label(text="Unit with its own new rig" if arm is not None else "Static model",
                             icon="ARMATURE_DATA" if arm is not None else "MESH_CUBE")
            layout.prop_search(self, "low_detail", bpy.data, "objects")
            if arm is None:
                box = layout.box()
                box.label(text="Building stages (optional)")
                for prop in ("half_built", "half_built_low", "start", "start_low"):
                    box.prop_search(self, prop, bpy.data, "objects")
        problem = mesh_problem(self.mesh)
        if problem is None and self.scatter and not chosen_terrain(self):
            problem = "Pick a terrain"
        draw_confirm(layout, self, name_problem(self.sprite_name) if problem is None else problem)

    def execute(self, context):
        root = repo_root(context)
        obj = bpy.data.objects.get(self.mesh)
        if obj is None or not attachment_obj_poll(self, obj):
            self.report({"ERROR"}, "Pick one of your own meshes")
            return {"CANCELLED"}
        name = self.sprite_name.strip()
        if not re.fullmatch(r"[A-Za-z0-9_]+", name):
            self.report({"ERROR"}, "Give the model a name made of letters, digits and underscores")
            return {"CANCELLED"}
        if self.scatter and not chosen_terrain(self):
            self.report({"ERROR"}, "Pick a terrain")
            return {"CANCELLED"}
        if self.scatter:
            arm, group, base = None, SCENERY_GROUP, None
        else:
            arm = body_rig(obj)
            group, base, _ = rig_registry(context, arm)
            if base is None:
                group = self.group
            if arm is not None and base is None and not armature_actions(arm):
                self.report({"ERROR"}, f"{arm.name} has no action: a new rig needs at least one clip")
                return {"CANCELLED"}
        new_clips = {a: a.name[len(name) + 1:] if a.name.startswith(name + "_") else a.name
                     for a in armature_actions(arm)} if arm is not None and base is None else {}
        names = list(new_clips.values())
        bad = [a.name for a, c in new_clips.items() if not re.fullmatch(r"[a-z0-9_]+", c) or names.count(c) > 1]
        if bad:
            self.report({"ERROR"}, f"Rename the actions {', '.join(sorted(bad))}: each clip needs its own name of "
                                   f"lowercase letters, digits and underscores")
            return {"CANCELLED"}

        def picked(prop):
            return bpy.data.objects.get(getattr(self, prop)) if getattr(self, prop) and not self.scatter else None

        stages = [("", obj, picked("low_detail"))]
        if arm is None:
            stages += [(suffix, picked(hi), picked(lo)) for suffix, hi, lo in
                       (("_halfbuilt", "half_built", "half_built_low"), ("_start", "start", "start_low"))
                       if picked(hi) is not None]
        meshes = [m for _, hi, lo in stages for m in (hi, lo) if m is not None]
        if any(m.type != "MESH" or m.get(REFERENCE_TAG) for m in meshes) or len(set(meshes)) != len(meshes):
            self.report({"ERROR"}, "Every stage and low detail pick must be a different mesh object")
            return {"CANCELLED"}
        taken = {s["name"] for s in read_registry(root) if s["group"] == group}
        clash = [name + suffix for suffix, _, _ in stages if name + suffix in taken]
        if clash:
            self.report({"ERROR"}, f"{group} already has a sprite named {', '.join(clash)}")
            return {"CANCELLED"}
        findings = [(level, f"{m.name}: {text}") for m in meshes for level, text in check_mesh(m, False, 0)]
        findings += texture_clashes(root, meshes)
        errors = store_findings(context, findings)
        if errors:
            self.report({"ERROR"}, f"Not registered: {errors} problem(s): " +
                        "; ".join(text for level, text in findings if level == "ERROR"))
            return {"CANCELLED"}

        geometry = os.path.join(root, GEOMETRY_DIR)
        folder = os.path.join(geometry, group, name)
        os.makedirs(folder, exist_ok=True)
        relative = lambda path: os.path.relpath(path, geometry).replace(os.sep, "/")
        skeleton, clips, missing, stage_models = None, [], [], []

        previous = arm.data.pose_position if arm is not None else None
        if arm is not None:
            arm.data.pose_position = "REST"
            context.view_layer.update()
        try:
            if arm is not None and base is None:
                skeleton_path = os.path.join(folder, name + "_skeleton.xml")
                write_skeleton_xml(arm, skeleton_path)
                skeleton = relative(skeleton_path)
            depsgraph = context.evaluated_depsgraph_get()
            for suffix, hi, lo in stages:
                models = []
                for mesh_obj, lod in ((hi, ""), (lo, "_lo")):
                    if mesh_obj is None:
                        continue
                    texture = mesh_obj.get("tt_texture", "").split(",")[0].strip() or material_image_name([mesh_obj])
                    if not ensure_texture_in_repo(root, mesh_obj, texture):
                        missing.append(texture)
                    ensure_emission_in_repo(root, mesh_obj)
                    path = os.path.join(folder, name + suffix + lod + ".xml")
                    write_mesh_xml([mesh_obj], [None if arm is not None else STATIC_BONE], path, texture, False,
                                   depsgraph)
                    models.append((relative(path), [(texture, team_attribute(root, texture, False) +
                                                                emissive_attribute(mesh_obj))]))
                stage_models.append((name + suffix, models))
        finally:
            if arm is not None:
                arm.data.pose_position = previous
                context.view_layer.update()

        if arm is not None and base is None:
            for action, clip in new_clips.items():
                path = os.path.join(folder, action.name + ".xml")
                write_animation_xml(context, arm, action, path)
                kind = "plain" if any(word in clip for word in PLAIN_CLIPS) else "loop"
                clips.append((clip, kind, relative(path)))
            arm["tt_skeleton"] = os.path.join(folder, name + "_skeleton.xml")

        extra = [("base", base)] if base is not None else []
        if self.scatter:
            event = chosen_event(self)
            extra = [("decoration", chosen_terrain(self)), ("count", str(self.count))] + ([("event", event)] if event else [])
        entries = [(sprite, sprite_text([("name", sprite)] + extra, models, skeleton, clips))
                   for sprite, models in stage_models]
        append_registry_entries(os.path.join(root, REGISTRY_FILE), group, entries)
        refresh_units(context)
        note = f"; add {', '.join(m + '.png' for m in missing)} to assets/textures/models" if missing else ""
        sprites = ", ".join(sprite for sprite, _ in entries)
        kind = "decoration " if self.scatter else ""
        self.report({"WARNING"} if missing else {"INFO"},
                    f"Registered {kind}{group} / {sprites} under /geometry/{group}/ as .binsprite{note}")
        return {"FINISHED"}


def loaded_decoration(context, body):
    """The registry entry of the loaded model when the game scatters it over maps, else None."""
    root = repo_root(context)
    entry = next((s for s in read_registry(root) if s["group"] == body.get("tt_group")
                  and s["name"] == body.get("tt_sprite")), None) if root else None
    return entry if entry is not None and entry["decoration"] else None


class EditScatter(bpy.types.Operator):
    """Change the ground the game scatters the loaded decoration on, how many it places and during which event"""
    bl_idname = "object.tt_edit_scatter"
    bl_label = "Scatter Settings..."
    grass: BoolProperty(name="Grass", options={"SKIP_SAVE"})
    dirt: BoolProperty(name="Dirt", options={"SKIP_SAVE"})
    beach: BoolProperty(name="Beach", options={"SKIP_SAVE"}, description="Sand on native maps, gravel on viking maps")
    snow: BoolProperty(name="Snow", options={"SKIP_SAVE"}, description="Viking maps only")
    land: BoolProperty(name="Any land", options={"SKIP_SAVE"}, description="Any ground above the sea")
    count: IntProperty(name="Count", default=20, min=1, max=1000, options={"SKIP_SAVE"},
                       description="How many the game scatters over a map")
    event: event_property()

    @classmethod
    def poll(cls, context):
        body = loaded_body()
        return body is not None and loaded_decoration(context, body) is not None

    def invoke(self, context, event):
        entry = loaded_decoration(context, loaded_body())
        grounds = entry["decoration"].split(",")
        for key, _, _ in DECORATION_GROUNDS:
            setattr(self, key, key in grounds)
        self.count = int(entry["count"] or 20)
        self.event = entry["event"] or NO_EVENT
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        draw_terrain(layout, self)
        layout.prop(self, "count")
        draw_event(layout, self)
        draw_confirm(layout, self, None if chosen_terrain(self) else "Pick a terrain")

    def execute(self, context):
        body = loaded_body()
        terrain = chosen_terrain(self)
        if body is None or loaded_decoration(context, body) is None or not terrain:
            self.report({"ERROR"}, "Load a decoration and pick a terrain")
            return {"CANCELLED"}
        set_sprite_attributes(os.path.join(repo_root(context), REGISTRY_FILE), body["tt_group"], body["tt_sprite"],
                              [("decoration", terrain), ("count", str(self.count)), ("event", chosen_event(self))])
        refresh_units(context)
        self.report({"INFO"}, f"{body['tt_sprite']} scatters on {terrain.replace(',', ', ')}, {self.count} per map, "
                              f"{chosen_event(self) or 'all year'}")
        return {"FINISHED"}


class VIEW3D_PT_tt_units(bpy.types.Panel):
    bl_label = "Models"
    bl_order = 0
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Tribal Trouble"

    def draw(self, context):
        layout = self.layout
        wm = context.window_manager
        draw_update_button(layout, context)
        holder = root_holder(context)
        layout.prop(holder, "repo_root" if hasattr(holder, "repo_root") else "tt_repo_root", text="Repo")
        if not repo_root(context):
            layout.label(text="Pick your tribaltrouble folder", icon="INFO")
            return
        row = layout.row(align=True)
        row.operator(RefreshUnits.bl_idname, icon="FILE_REFRESH")
        row.prop(wm, "tt_category", text="")
        layout.template_list("TT_UL_units", "", wm, "tt_units", wm, "tt_unit_index", rows=10)
        if references():
            layout.operator(RemoveAdded.bl_idname, icon="X")
        layout.operator(LoadUnit.bl_idname, icon="FILE_REFRESH")
        row = layout.row(align=True)
        row.scale_y = 1.4
        row.operator(PublishModel.bl_idname, icon="EXPORT")
        if active_armature(context) is not None:
            row.operator(Preflight.bl_idname, text="", icon="CHECKMARK")
        body = loaded_body()
        if body is not None:
            remove = row.operator(RemoveFromRegistry.bl_idname, text="", icon="TRASH")
            remove.group, remove.sprite = body["tt_group"], body["tt_sprite"]
        draw_checks(layout, wm)
        if body is not None:
            draw_glow(layout, context, body)
        if body is not None and loaded_decoration(context, body) is not None:
            layout.operator(EditScatter.bl_idname, icon="PREFERENCES")
        # Units show it under Preview.
        if has_low_detail() and not any(o.type == "ARMATURE" and o.get(BROWSER_TAG) for o in bpy.data.objects):
            layout.row(align=True).prop(wm, "tt_detail", expand=True)
        layout.operator(RegisterModel.bl_idname, icon="ADD")

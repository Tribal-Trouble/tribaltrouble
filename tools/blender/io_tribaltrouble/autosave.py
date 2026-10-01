"""Saving by itself: a change to the loaded model is written to the repo a moment after you stop."""

import bpy
from bpy.app.handlers import persistent
from bpy.props import StringProperty

from .publish import publish_all
from .registry import repo_root

QUIET_SECONDS = 2.0
PAINT_SECONDS = 10.0  # writing a big PNG after every stroke would stall painting
WATCHED = (bpy.types.Object, bpy.types.Mesh, bpy.types.Action, bpy.types.Image, bpy.types.Material)
_saving = [False]


def watched(update):
    if not isinstance(update.id, WATCHED):
        return False
    return not isinstance(update.id, bpy.types.Object) or update.is_updated_geometry or update.is_updated_transform


@persistent
def on_depsgraph_update(scene, depsgraph):
    screen = bpy.context.screen
    if _saving[0] or screen is not None and screen.is_animation_playing:
        return
    if any(watched(update) for update in depsgraph.updates):
        schedule()


def schedule():
    if bpy.app.timers.is_registered(save_later):
        bpy.app.timers.unregister(save_later)
    bpy.app.timers.register(save_later, first_interval=PAINT_SECONDS if bpy.context.mode == "PAINT_TEXTURE"
                            else QUIET_SECONDS)


def save_later():
    windows = bpy.context.window_manager.windows
    with bpy.context.temp_override(window=windows[0]) if windows else bpy.context.temp_override():
        save_now(bpy.context)
    return None


def save_now(context):
    """Write what changed on the loaded model; the names of what was written."""
    if _saving[0] or not repo_root(context):
        return []
    problems = []
    _saving[0] = True
    try:
        saved = publish_all(context, lambda level, message: problems.append(message) if "ERROR" in level else None)
    finally:
        _saving[0] = False
    wm = context.window_manager
    if problems:
        wm.tt_saved = problems[-1]
    elif saved:
        wm.tt_saved = "Saved " + ", ".join(saved)
    for window in wm.windows:
        for area in window.screen.areas:
            if area.type == "VIEW_3D":
                area.tag_redraw()
    return saved


@persistent
def save_before(_file=None):
    """Nothing pending is lost when the .blend is saved or another one opened."""
    if bpy.app.timers.is_registered(save_later):
        bpy.app.timers.unregister(save_later)
    save_now(bpy.context)


def draw_saved(layout, wm):
    if wm.tt_saved:
        row = layout.row()
        row.alert = not wm.tt_saved.startswith("Saved")
        row.enabled = row.alert
        row.label(text=wm.tt_saved, icon="ERROR" if row.alert else "CHECKMARK")


def register():
    bpy.types.WindowManager.tt_saved = StringProperty(options={"SKIP_SAVE"})
    bpy.app.handlers.depsgraph_update_post.append(on_depsgraph_update)
    bpy.app.handlers.save_pre.append(save_before)
    bpy.app.handlers.load_pre.append(save_before)


def unregister():
    for handlers, handler in ((bpy.app.handlers.depsgraph_update_post, on_depsgraph_update),
                              (bpy.app.handlers.save_pre, save_before), (bpy.app.handlers.load_pre, save_before)):
        if handler in handlers:
            handlers.remove(handler)
    if bpy.app.timers.is_registered(save_later):
        bpy.app.timers.unregister(save_later)
    del bpy.types.WindowManager.tt_saved

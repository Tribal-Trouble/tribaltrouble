"""Whether the loaded model has unpublished work, worked out a moment after changes stop, so drawing the panel never
exports meshes."""

import bpy
from bpy.app.handlers import persistent
from bpy.props import StringProperty

from .registry import repo_root
from .scene import adopt_shown_skins, BROWSER_TAG, unpublished_changes
from .textures import mesh_texture_image

QUIET_SECONDS = 0.5
PAINT_POLL_SECONDS = 1.0  # paint strokes do not always reach the depsgraph, so painting is looked at on a timer
WATCHED = (bpy.types.Object, bpy.types.Mesh, bpy.types.Action, bpy.types.Image, bpy.types.Material)


def watched(update):
    if not isinstance(update.id, WATCHED):
        return False
    return not isinstance(update.id, bpy.types.Object) or update.is_updated_geometry or update.is_updated_transform


@persistent
def on_depsgraph_update(scene, depsgraph):
    screen = bpy.context.screen
    if screen is not None and screen.is_animation_playing:
        return
    if any(watched(update) for update in depsgraph.updates):
        schedule()


def schedule():
    if bpy.app.timers.is_registered(refresh_later):
        bpy.app.timers.unregister(refresh_later)
    bpy.app.timers.register(refresh_later, first_interval=QUIET_SECONDS)


def refresh_later():
    windows = bpy.context.window_manager.windows
    with bpy.context.temp_override(window=windows[0]) if windows else bpy.context.temp_override():
        painting = bpy.context.mode == "PAINT_TEXTURE"
        if painting and "paint" in bpy.context.window_manager.tt_unpublished:
            return PAINT_POLL_SECONDS
        refresh(bpy.context, painting)
        return PAINT_POLL_SECONDS if painting else None


def refresh(context, paint_only=False):
    """Work out what is unpublished now; paint_only looks at painted images alone, which is cheap."""
    wm = context.window_manager
    if not repo_root(context):
        changes = []
    elif paint_only:
        dirty = any(image is not None and image.is_dirty for image in
                    (mesh_texture_image(o) for o in bpy.data.objects if o.get(BROWSER_TAG) and o.type == "MESH"))
        changes = [c for c in wm.tt_unpublished.split(", ") if c] + (["paint"] if dirty else [])
    else:
        changes = unpublished_changes(context)
    text = ", ".join(dict.fromkeys(changes))
    if text != wm.tt_unpublished:
        wm.tt_unpublished = text
        for window in wm.windows:
            for area in window.screen.areas:
                if area.type == "VIEW_3D":
                    area.tag_redraw()


@persistent
def on_load(_file=None):
    bpy.context.window_manager.tt_unpublished = ""
    if not bpy.app.timers.is_registered(adopt_later):
        bpy.app.timers.register(adopt_later, first_interval=0.2)


def adopt_later():
    windows = bpy.context.window_manager.windows
    with bpy.context.temp_override(window=windows[0]) if windows else bpy.context.temp_override():
        adopt_shown_skins(bpy.context)
        refresh(bpy.context)  # a reopened .blend may hold work that was never published
    return None


def register():
    bpy.types.WindowManager.tt_unpublished = StringProperty(options={"SKIP_SAVE"})
    bpy.app.handlers.depsgraph_update_post.append(on_depsgraph_update)
    bpy.app.handlers.load_post.append(on_load)
    bpy.app.timers.register(adopt_later, first_interval=0.5)  # a skin already on show when the add-on is (re)loaded


def unregister():
    for handlers, handler in ((bpy.app.handlers.depsgraph_update_post, on_depsgraph_update),
                              (bpy.app.handlers.load_post, on_load)):
        if handler in handlers:
            handlers.remove(handler)
    for timer in (refresh_later, adopt_later):
        if bpy.app.timers.is_registered(timer):
            bpy.app.timers.unregister(timer)
    del bpy.types.WindowManager.tt_unpublished

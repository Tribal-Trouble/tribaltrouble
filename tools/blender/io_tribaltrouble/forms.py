"""Pieces the panels' popup forms share: confirm rows, reopening, event dropdowns, name checks."""

import json
import re

import bpy
from bpy.props import StringProperty, EnumProperty

from .registry import read_registry, repo_root
from .scene import attachment_obj_poll


def own_mesh_search(self, context, edit_text):
    return [o.name for o in bpy.data.objects if attachment_obj_poll(self, o) and edit_text.lower() in o.name.lower()]


NO_EVENT = "ALL_YEAR"
_event_items = {True: [], False: []}
_new_events = []  # made with New Event this session; geometry.xml names an event only once something uses it


def known_events(context):
    """Every event geometry.xml already names, and those made with New Event, for the event fields to offer."""
    root = repo_root(context)
    registry = read_registry(root) if root else []
    events = {s["event"] for s in registry} | {e for s in registry for level in s["textures"] for _, e in level}
    return sorted(e for e in events | set(_new_events) if e)


def valid_event(event):
    """Blank means all year; a name ends up in geometry.xml."""
    return not event or re.fullmatch(r"[a-z0-9_]+", event) is not None


def event_items(context, optional):
    """The Event dropdown: All year where an event is optional, then every known event."""
    items = _event_items[optional]
    items[:] = (([(NO_EVENT, "All year", "No event: shows all year")] if optional else []) +
                [(e, e, f"Only during {e}") for e in known_events(context)])
    return items


def event_property(optional=True):
    """optional may also be a function of the context."""
    return EnumProperty(name="Event", items=lambda self, context: event_items(
        context, optional(context) if callable(optional) else optional),
                        options={"SKIP_SAVE"}, description="Pick an event, or + to name a new one")


def chosen_event(op):
    """The event an operator's form picked: '' for all year."""
    return "" if op.event == NO_EVENT else op.event


def form_values(op):
    return {key: getattr(op, key) for key in op.properties.bl_rna.properties.keys() if key != "rna_type"}


def draw_event(layout, op):
    row = layout.row(align=True)
    row.prop(op, "event")
    plus = row.row(align=True)
    plus.operator_context = "INVOKE_DEFAULT"
    plus.ui_units_x = 1.5
    # A confirm button closes the form, which New Event reopens with these values.
    new = plus.template_popup_confirm(NewEvent.bl_idname, text=" ", icon="ADD", cancel_text="")
    if new is not None:
        new.form, new.values = op.bl_idname, json.dumps(form_values(op))


_reopen = {}  # form bl_idname: the values New Event hands back to the form it closed


def open_form(op, context):
    """Show op's form as a popup, drawn with form_title and draw_confirm; a form New Event closed gets its values
    back."""
    for key, value in _reopen.pop(op.bl_idname, {}).items():
        setattr(op, key, value)
    return context.window_manager.invoke_popup(op)


def form_title(op):
    op.layout.label(text=op.bl_label.rstrip("."))
    return op.layout


def draw_confirm(layout, op, problem=None):
    """OK and Cancel for a form opened with open_form. OK stays greyed while problem is not None; a problem that is
    not blank is shown above them."""
    if problem:
        layout.label(text=problem, icon="ERROR")
    row = layout.row()
    row.operator_context = "EXEC_DEFAULT"
    ok = row.row()
    ok.enabled = problem is None
    props = ok.template_popup_confirm(op.bl_idname, text="OK", cancel_text="")
    if props is not None:
        for key, value in form_values(op).items():
            setattr(props, key, value)
    row.row().template_popup_confirm("", text="", cancel_text="Cancel")


def mesh_problem(name):
    """Why a Mesh field cannot be used: blank when nothing is picked, None when it names one of your own meshes."""
    obj = bpy.data.objects.get(name.strip())
    if obj is not None and attachment_obj_poll(None, obj):
        return None
    return "Pick one of your own meshes" if name.strip() else ""


def name_problem(name):
    if re.fullmatch(r"[A-Za-z0-9_]+", name.strip()):
        return None
    return "Letters, digits and underscores only" if name.strip() else ""


class NewEvent(bpy.types.Operator):
    """Name an event geometry.xml does not use yet and pick it in the form"""
    bl_idname = "object.tt_new_event"
    bl_label = "New Event"
    event_name: StringProperty(name="Name", options={"SKIP_SAVE"},
                               description="Such as halloween: letters, digits and underscores")
    form: StringProperty(options={"SKIP_SAVE", "HIDDEN"})
    values: StringProperty(options={"SKIP_SAVE", "HIDDEN"})

    def invoke(self, context, event):
        return open_form(self, context)

    def draw(self, context):
        layout = form_title(self)
        layout.prop(self, "event_name")
        draw_confirm(layout, self, name_problem(self.event_name))

    def execute(self, context):
        event = self.event_name.strip().lower()
        if not event or not valid_event(event):
            self.report({"ERROR"}, "Name the event with letters, digits and underscores")
            return {"CANCELLED"}
        if event not in _new_events:
            _new_events.append(event)
        if self.form and context.window is not None:
            _reopen[self.form] = {**json.loads(self.values or "{}"), "event": event}
            # A running operator's bl_idname reads OBJECT_OT_tt_new_prop, not object.tt_new_prop.
            module, _, name = self.form.partition("_OT_") if "_OT_" in self.form else self.form.partition(".")
            getattr(getattr(bpy.ops, module.lower()), name)("INVOKE_DEFAULT")
        return {"FINISHED"}

package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.CheckBoxListener;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.ToolTip;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class CheckBox extends GUIObject implements ToolTip {
    private final List<@NonNull CheckBoxListener> event_listeners = new java.util.ArrayList<>();

    private final String tool_tip;

    private boolean marked;
    private boolean pressed = false;

    public CheckBox(boolean marked, @NonNull String text) {
        this(marked, text, "");
    }

    public CheckBox(boolean marked, @NonNull String text, String tool_tip) {
        this.marked = marked;
        this.tool_tip = tool_tip;
        Label label = new Label(text, Skin.getSkin().getEditFont());
        addChild(label);
        label.setPos(Skin.getSkin().getCheckBoxMarked().quad(ModeIconQuads.Mode.NORMAL).getWidth(),
                (Skin.getSkin().getCheckBoxMarked().quad(
                        ModeIconQuads.Mode.NORMAL).getHeight() - label.getHeight()) / 2);
        setDim(Skin.getSkin().getCheckBoxMarked().quad(ModeIconQuads.Mode.NORMAL).getWidth() + label.getWidth(),
                Skin.getSkin().getCheckBoxMarked().quad(ModeIconQuads.Mode.NORMAL).getHeight());
        setCanFocus(true);
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        if (marked != this.marked) {
            this.marked = marked;
            checkedAll(marked);
        }
    }

    @Override
    public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
        tool_tip_box.append(tool_tip);
    }

    private void toggleMarked() {
        marked = !marked;
        checkedAll(marked);
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        toggleMarked();
    }

    @Override
    protected void mouseReleased(@NonNull MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mousePressed(@NonNull MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isActive()
                ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        // When marked, active, pressed, and hovered, it should show the unmarked state
        // When unmarked, active, pressed, and hovered, it should show the marked state
        ModeIconQuads quad_to_render = isMarked() ? (skinMode == ModeIconQuads.Mode.ACTIVE && pressed
                && isHovered() ? Skin.getSkin().getCheckBoxUnmarked() : Skin.getSkin().getCheckBoxMarked()) : (skinMode
                        == ModeIconQuads.Mode.ACTIVE
                        && pressed
                        && isHovered() ? Skin.getSkin().getCheckBoxMarked() : Skin.getSkin().getCheckBoxUnmarked());

        renderer.drawModeIcon(quad_to_render, skinMode, 0, 0);
    }

    public void checkedAll(boolean marked) {
        checked(marked);
        for (var listener : event_listeners) {
            listener.checked(marked);
        }
    }

    void checked(boolean marked) {
        /*
        		GUIObject parent = (GUIObject)getParent();
        		if (parent != null)
        			parent.checkedAll(marked);
        */
    }

    public void addCheckBoxListener(@NonNull CheckBoxListener listener) {
        event_listeners.add(listener);
    }

    public void removeCheckBoxListener(@NonNull CheckBoxListener listener) {
        event_listeners.remove(listener);
    }
}

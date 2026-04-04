package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public abstract class ButtonObject extends GUIObject {
    private boolean pressed = false;
    private final @NonNull Font font;

    public ButtonObject(@NonNull Font font) {
        this.font = font;
        setCanFocus(true);
    }

    protected @NonNull Font getFont() {
        return font;
    }

    final boolean isPressed() {
        return pressed;
    }

    @Override
    protected final void mouseReleased(@NonNull MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected final void mousePressed(@NonNull MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void mouseHeld(@NonNull MouseButton button, int x, int y) {
        if (pressed)
            mousePressedAll(button, x, y);
    }

    @Override
    public void setDisabled(boolean disabled) {
        if (disabled)
            pressed = false;
        super.setDisabled(disabled);
    }
}

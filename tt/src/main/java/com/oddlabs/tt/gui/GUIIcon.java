package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public class GUIIcon extends GUIObject {
    private final @NonNull IconQuad icon;

    public GUIIcon(@NonNull IconQuad icon) {
        this.icon = icon;
        setDim(icon.getWidth(), icon.getHeight());
        setCanFocus(false);
    }

    @Override
    public void renderGeometry(@NonNull GUIRenderer renderer) {
        renderer.drawIcon(icon, 0, 0, getWidth(), getHeight());
    }
}

package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class Diode extends GUIObject {
    private boolean lit;

    public Diode() {
        var normal = Skin.getSkin().getDiode().get(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
        lit = false;
    }

    public void setLit(boolean lit) {
        this.lit = lit;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : lit
                  ? ModeIconQuads.Mode.ACTIVE
                  : ModeIconQuads.Mode.NORMAL;

        renderer.drawModeIcon(Skin.getSkin().getDiode(), skinMode, 0, 0);
    }
}

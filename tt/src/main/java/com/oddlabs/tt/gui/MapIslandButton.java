package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public class MapIslandButton extends IconButton {
    private final int islandIndex;

    public MapIslandButton(@NonNull ModeIconQuads icon, int islandIndex) {
        super(icon, () -> "");
        this.islandIndex = islandIndex;
        setCanFocus(true);
    }

    public int getIslandIndex() {
        return islandIndex;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode;
        if (isDisabled()) {
            skinMode = ModeIconQuads.Mode.DISABLED;
        } else if (isHovered()) {
            skinMode = ModeIconQuads.Mode.ACTIVE;
        } else if (isActive()) {
            boolean otherIslandHovered = false;
            GUIRoot root = getParentGUIRoot();
            if (root != null) {
                GUIObject hoverObj = root.getCurrentGUIObject();
                if (hoverObj instanceof MapIslandButton && hoverObj != this) {
                    otherIslandHovered = true;
                }
            }
            skinMode = otherIslandHovered ? ModeIconQuads.Mode.NORMAL : ModeIconQuads.Mode.ACTIVE;
        } else {
            skinMode = ModeIconQuads.Mode.NORMAL;
        }
        renderer.drawModeIcon(getIcon(), skinMode, 0, 0);
    }
}

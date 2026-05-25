package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.ToolTip;
import org.jspecify.annotations.NonNull;

public class MapIslandButton extends IconButton implements ToolTip {
    private final @NonNull String tool_tip;
    private final int islandIndex;

    public MapIslandButton(@NonNull ModeIconQuads icon, @NonNull String tool_tip, int islandIndex) {
        super(icon);
        this.tool_tip = tool_tip;
        this.islandIndex = islandIndex;
        setCanFocus(true);
    }

    public int getIslandIndex() {
        return islandIndex;
    }

    @Override
    public void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
        if (!tool_tip.isEmpty()) {
            tool_tip_box.append(tool_tip);
        }
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

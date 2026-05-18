package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BorderGroup extends Group {
    private final @Nullable Label label;

    public BorderGroup() {
        label = null;
    }

    public BorderGroup(@NonNull String caption) {
        GroupData data = Skin.getSkin().getGroupData();
        label = new Label(caption, data.captionFont());
    }

    @Override
    public void compileCanvas() {
        GroupData data = Skin.getSkin().getGroupData();
        Box group = data.group();
        if (label != null) {
            super.compileCanvas(group.getLeftOffset(),
                    group.getBottomOffset(),
                    group.getRightOffset(),
                    group.getTopOffset() + data.captionOffset());
            label.setPos(data.captionLeft(), getHeight() - data.captionY());
            addChild(label);
        } else {
            super.compileCanvas(group.getLeftOffset(), group.getBottomOffset(), group.getRightOffset(),
                    group.getTopOffset());
        }
        setCanFocus(true);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Skin.getSkin().getGroupData().group().render(renderer, 0f, 0f, getWidth(), getHeight(),
                ModeIconQuads.Mode.NORMAL);
    }
}

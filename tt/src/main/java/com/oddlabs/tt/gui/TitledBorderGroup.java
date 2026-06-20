package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

/**
 * A GUI group with a bordered background and a title label embedded in the top border.
 */
public final class TitledBorderGroup extends Group {
    private final @NonNull Label label;
    private int fixedWidth = -1;

    public TitledBorderGroup(@NonNull String caption) {
        GroupData data = Skin.getSkin().getGroupData();
        label = new Label(caption, data.captionFont());
        // We don't add the label as a child because we'll render it manually
        // and we don't want it to affect the compileCanvas layout logic.
    }

    public void setFixedWidth(int width) {
        this.fixedWidth = width;
    }

    @Override
    public void compileCanvas() {
        GroupData data = Skin.getSkin().getGroupData();
        Box group = data.group();

        // Interior layout offsets - removed all extra padding
        int topOffset = group.getTopOffset();

        super.compileCanvas(group.getLeftOffset(),
                group.getBottomOffset(),
                group.getRightOffset(),
                topOffset);

        // Force fixed width if specified
        if (fixedWidth > 0) {
            setDim(fixedWidth, getHeight());
        }

        // Position label at the top - raised even higher
        label.setPos(data.captionLeft(), getHeight() - data.captionY() + 2);

        setCanFocus(true);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        GroupData groupData = Skin.getSkin().getGroupData();
        Box box = groupData.group();
        ModeIconQuads.Mode skinMode = ModeIconQuads.Mode.NORMAL;

        int width = getWidth();
        int height = getHeight();

        int leftWidth = box.getLeftWidth();
        int rightWidth = box.getRightWidth();
        int bottomHeight = box.getBottomHeight();
        int topHeight = box.getTopHeight();

        int centerWidth = width - leftWidth - rightWidth;
        int centerHeight = height - bottomHeight - topHeight;

        // 1. Render all parts except the top edge
        renderer.drawModeIcon(box.getLeftBottom(), skinMode, 0, 0);
        renderer.drawIcon(box.getBottom().quad(skinMode), leftWidth, 0, centerWidth, bottomHeight);
        renderer.drawModeIcon(box.getRightBottom(), skinMode, leftWidth + centerWidth, 0);
        renderer.drawIcon(box.getRight().quad(skinMode), leftWidth + centerWidth, bottomHeight, rightWidth,
                centerHeight);
        renderer.drawModeIcon(box.getRightTop(), skinMode, leftWidth + centerWidth, bottomHeight + centerHeight);
        renderer.drawModeIcon(box.getLeftTop(), skinMode, 0, bottomHeight + centerHeight);
        renderer.drawIcon(box.getLeft().quad(skinMode), 0, bottomHeight, leftWidth, centerHeight);
        renderer.drawIcon(box.getCenter().quad(skinMode), leftWidth, bottomHeight, centerWidth, centerHeight);

        // 2. Render top edge with a break for the label
        int labelX = label.getX();
        int labelWidth = label.getWidth();
        int padding = 2;

        // Left segment of top border
        int leftSegWidth = labelX - leftWidth - padding;
        if (leftSegWidth > 0) {
            renderer.drawIcon(box.getTop().quad(skinMode), leftWidth, bottomHeight + centerHeight, leftSegWidth,
                    topHeight);
        }

        // Right segment of top border
        int rightSegX = labelX + labelWidth + padding;
        int rightSegWidth = (leftWidth + centerWidth) - rightSegX;
        if (rightSegWidth > 0) {
            renderer.drawIcon(box.getTop().quad(skinMode), rightSegX, bottomHeight + centerHeight, rightSegWidth,
                    topHeight);
        }

        // 3. Render the label
        label.render(renderer);
    }
}

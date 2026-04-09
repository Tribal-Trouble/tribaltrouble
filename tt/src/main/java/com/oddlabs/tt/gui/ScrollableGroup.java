package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public class ScrollableGroup extends Group implements Scrollable {
    private @Nullable ScrollBar scroll_bar;
    private int content_height;
    private int total_content_height;
    private final int scroll_bar_left_margin;
    private int offset_y;

    public ScrollableGroup(int content_height, int scroll_bar_left_margin) {
        setCanFocus(true);
        this.content_height = content_height;
        this.scroll_bar_left_margin = scroll_bar_left_margin;
        scroll_bar = new ScrollBar(content_height, this);
        addChild(scroll_bar);
        scroll_bar.setCanFocus(true);
        scroll_bar.place();
    }

    @Override
    public void compileCanvas() {
        // Temporarily remove scrollbar so bounding box reflects content only
        if (scroll_bar != null) removeChild(scroll_bar);
        super.compileCanvas(0, 0, 0, 0);
        total_content_height = getHeight();
        if (scroll_bar != null) addChild(scroll_bar);

        // Always reserve space for scrollbar width so parent Panel sizes correctly
        int scrollbar_width = scroll_bar != null ? scroll_bar.getWidth() : 0;

        if (total_content_height < content_height) {
            int remainingHeight = total_content_height % content_height;
            if (scroll_bar != null) {
                removeChild(scroll_bar);
            }
            GUIObject current = getFirstChild();
            while (current != null) {
                if (current != scroll_bar) {
                    current.setPos(current.getX(), current.getY() + content_height - remainingHeight);
                }
                current = current.getNext();
            }
            scroll_bar = null;
            setDim(getWidth() + scrollbar_width + scroll_bar_left_margin, content_height);
            return;
        }

        if (scroll_bar != null) {
            scroll_bar.update();
            setDim(getWidth() + scroll_bar.getWidth() + scroll_bar_left_margin, content_height);
            scroll_bar.setPos(getWidth() - scroll_bar.getWidth(), 0);
            scroll_bar.update();
        }

        GUIObject current = getFirstChild();
        while (current != null) {
            if (current != scroll_bar) {
                int offset = Math.max(0, total_content_height - content_height);
                current.setPos(current.getX(), current.getY() - offset);
            }
            current = current.getNext();
        }
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        // Flush pending draws, enable scissor for content area only (not scrollbar)
        renderer.flush();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float scale = getGlobalScale();
        GL11.glScissor((int) (getRootX() * scale), (int) (getRootY() * scale),
                (int) (getWidth() * scale), (int) (content_height * scale));
    }

    @Override
    protected void postRender(@NonNull GUIRenderer renderer) {
        renderer.flush();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void setContentHeight(int height) {
        content_height = height;
    }

    public int getContentHeight() {
        return content_height;
    }

    @Override
    public final int getOffsetY() {
        return offset_y;
    }

    @Override
    public final int getStepHeight() {
        return 28;
    }

    @Override
    public final void jumpPage(boolean up) {
        setOffsetY(offset_y + (up ? -getVisibleHeight() : getVisibleHeight()));
    }

    @Override
    protected final void mouseScrolled(int amount) {
        setOffsetY(offset_y + (amount > 0 ? -1 : 1) * getStepHeight() * 3);
    }

    @Override
    public final float getScrollBarRatio() {
        int totalHeight = getTotalContentHeight();
        int visibleHeight = getVisibleHeight();
        if (totalHeight <= visibleHeight) return 1.0f;
        return visibleHeight / (float) totalHeight;
    }

    @Override
    public final float getScrollBarOffset() {
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset <= 0) return 0.0f;
        return offset_y / (float) maxOffset;
    }

    @Override
    public final void setScrollBarOffset(float offset) {
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset > 0) {
            setOffsetY((int) (offset * maxOffset));
        } else {
            setOffsetY(0);
        }
    }

    @Override
    public final void setOffsetY(int new_offset) {
        if (scroll_bar == null) return;
        int diff = new_offset - offset_y;

        if (offset_y + diff <= 0) {
            diff = -offset_y;
            offset_y = 0;
        }

        int max_offset_y = getTotalContentHeight() - getVisibleHeight();
        if (offset_y + diff > max_offset_y) {
            diff = max_offset_y - offset_y;
        }

        offset_y = offset_y + diff;
        if (offset_y < 0) offset_y = 0;
        if (offset_y > max_offset_y) offset_y = max_offset_y;

        GUIObject current = getFirstChild();
        while (current != null) {
            if (current != scroll_bar) {
                current.setPos(current.getX(), current.getY() + diff);
            }
            current = current.getNext();
        }
        scroll_bar.update();
    }

    private int getTotalContentHeight() {
        return total_content_height;
    }

    private int getVisibleHeight() {
        return content_height;
    }
}

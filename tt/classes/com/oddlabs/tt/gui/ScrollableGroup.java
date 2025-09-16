package com.oddlabs.tt.gui;

import com.oddlabs.util.ListElement;

import org.lwjgl.opengl.GL11;

public class ScrollableGroup extends Group implements Scrollable {
    private ScrollBar scroll_bar;
    // The height for one 'page' of the scrollbar or all the content that can be seen by the user
    private int content_height = 0;
    private int total_content_height = 0;
    private int scroll_bar_left_margin = 0;
    // Where the scrollbar is currently
    private int offset_y = 0; // Tracks the vertical offset for scrolling
    // When true, invert the scrollbar's visual mapping (thumb/top vs bottom), keeping content logic the same
    private boolean inverted_scrollbar = true;

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
        // Reuse the way the group already aligns itself
        super.compileCanvas(0, 0, 0, 0);
        total_content_height = getHeight() - content_height + getStepHeight();

        // If added content is less than the set dimensions -
        // remove the scrollbar and adjust the positions of the children
        if (total_content_height < content_height) {
            int remainingHeight = total_content_height % content_height;
            this.removeChild(scroll_bar);
            ListElement current = getFirstChild();
            while (current != null) {
                GUIObject gui_object = (GUIObject) current;
                if (gui_object == scroll_bar) {
                    current = current.getNext();
                    continue;
                }

                int x = gui_object.getX();
                int y = gui_object.getY();
                // Items are placed bottom up. This pushes the items up to the top of the container
                // if they don't fill the expected space
                gui_object.setPos(x, y + content_height - remainingHeight);
                current = current.getNext();
            }
            scroll_bar = null;
            setDim(getWidth(), content_height);
            return;
        }

        scroll_bar.update();
        // Only show the amount that the control was told to (content_height)
        setDim(getWidth() + scroll_bar.getWidth() + scroll_bar_left_margin, content_height);
        System.out.println("dim: " + getWidth() + ", " + content_height);
        scroll_bar.setPos(getWidth() - scroll_bar.getWidth(), 0);
        scroll_bar.update();
        ListElement current = getFirstChild();
        while (current != null) {
            GUIObject gui_object = (GUIObject) current;
            if (gui_object == scroll_bar) {
                current = current.getNext();
                continue;
            }

            int x = gui_object.getX();
            int y = gui_object.getY();
            int offset = Math.max(0, total_content_height - content_height);
            gui_object.setPos(x, y - offset);
            current = current.getNext();
        }
    }

    /** Sets the height for the 'visible' content */
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
        int pageSize = getVisibleHeight();
        if (up) {
            setOffsetY(offset_y - pageSize);
        } else {
            setOffsetY(offset_y + pageSize);
        }
    }

    protected final void mouseScrolled(int amount) {
        if (amount > 0) {
            setOffsetY(offset_y - getStepHeight() * 3);
        } else {
            setOffsetY(offset_y + getStepHeight() * 3);
        }
    }

    /**
     * The ratio of the scroll bar button that can be dragged relative to the size of the bar (0.0f
     * - 1.0f)
     */
    @Override
    public final float getScrollBarRatio() {
        int totalHeight = getTotalContentHeight();
        int visibleHeight = getVisibleHeight();
        if (totalHeight <= visibleHeight) return 1.0f;
        return visibleHeight / (float) totalHeight;
    }

    /** Where the scrollbar is positioned relative to the total content height (0.0f - 1.0f) */
    @Override
    public final float getScrollBarOffset() {
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset <= 0) return 0.0f;
        float norm = offset_y / (float) maxOffset;
        return inverted_scrollbar ? (1.0f - norm) : norm;
    }

    @Override
    public final void setScrollBarOffset(float offset) {
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset > 0) {
            float norm = inverted_scrollbar ? (1.0f - offset) : offset;
            setOffsetY((int) (norm * maxOffset));
        } else {
            setOffsetY(0);
        }
    }

    @Override
    public final void setOffsetY(int new_offset) {
        if (scroll_bar == null) return;
        // Get the difference from the old offset to the requested new one
        int diff = new_offset - offset_y;
        // Calculate how far past the min offset we went to and correct it to min (0)
        if (offset_y + diff <= 0) {
            // diff = distance to zero from offset y
            diff = -offset_y;
            offset_y = 0;
        }

        int max_offset_y = getTotalContentHeight() - getVisibleHeight();
        // Calculate how far past the max offset we went and correct it to max (max_offset_y)
        if (offset_y + diff > max_offset_y) {
            diff = max_offset_y - offset_y;
        }

        offset_y = offset_y + diff;
        if (offset_y < 0) offset_y = 0;
        if (offset_y > max_offset_y) offset_y = max_offset_y;

        ListElement current = getFirstChild();
        while (current != null) {
            GUIObject gui_object = (GUIObject) current;
            if (gui_object == scroll_bar) {
                current = current.getNext();
                continue;
            }
            int x = gui_object.getX();
            int y = gui_object.getY();

            // Move each child object from its original position by the distance we scrolled
            gui_object.setPos(x, y + diff);
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

    @Override
    protected void renderGeometry() {
        // Enable scissor test to clip rendering to content area
        GL11.glEnd();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        // Get absolute screen coordinates for scissor rectangle
        int abs_x = (int) getRootX();
        int abs_y = (int) getRootY();
        GL11.glScissor(abs_x, abs_y, getWidth(), content_height);
        GL11.glBegin(GL11.GL_QUADS);
    }

    @Override
    protected void postRender() {
        // Disable scissor test after rendering
        GL11.glEnd();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glBegin(GL11.GL_QUADS);
    }

    /**
     * Invert the scrollbar thumb direction and default position visually,
     * without changing content ordering or scroll semantics.
     */
    public void setInvertedScrollbar(boolean inverted) {
        this.inverted_scrollbar = inverted;
        if (scroll_bar != null) scroll_bar.update();
    }
}

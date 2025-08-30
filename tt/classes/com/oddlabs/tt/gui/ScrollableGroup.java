package com.oddlabs.tt.gui;

import com.oddlabs.util.ListElement;

public class ScrollableGroup extends Group implements Scrollable {
    private ScrollBar scroll_bar;
    // The height for one 'page' of the scrollbar or all the content that can be seen by the user
    private int content_height = 0;
    private int total_content_height = 0;
    private int scroll_bar_left_margin = 0;
    // Where the scrollbar is currently
    private int offset_y = 0; // Tracks the vertical offset for scrolling

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
        total_content_height = getHeight() - content_height;        
        scroll_bar.update();
        setDim(getWidth() + scroll_bar.getWidth() + scroll_bar_left_margin, content_height);
        System.out.println("dim: " + getWidth() + ", " + content_height);
        scroll_bar.setPos(getWidth() - scroll_bar.getWidth(), 0);
        scroll_bar.update();
        System.out.println("ScrollableGroup updated. Scrollbar pos: x" + scroll_bar.getX() + ", y: " + scroll_bar.getY());
        ListElement current = getFirstChild();
        System.out.println("Height: " + getHeight());
        int count =0;
        while (current != null) {
            GUIObject gui_object = (GUIObject) current;
            if(gui_object == scroll_bar) {
                current = current.getNext();
                continue;
            }
            count++;
            int x = gui_object.getX();
            int y = gui_object.getY();
            System.out.println("Setting position of child " + count + " to: (" + gui_object.getX() + ", " + (gui_object.getY()) + ")");
            gui_object.setPos(x, y - (getHeight() + content_height));
            System.out.println("Set position of child " + count + " to: (" + gui_object.getX() + ", " + (gui_object.getY()) + ")");

            current = current.getNext();
        }
    }
    /**
     * Sets the height for the 'visible' content
     */
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
        return 30;
    }

    @Override
    public final void jumpPage(boolean up) {
        int pageSize = getVisibleHeight();
        System.out.println("Jumping page: " + (up ? "up" : "down") + " by " + pageSize);
        if (up) {
            setOffsetY(offset_y - pageSize);
        } else {
            setOffsetY(offset_y + pageSize);
        }
    }

    protected final void mouseScrolled(int amount) {
        System.out.println("Mouse scrolled: " + amount);
        if (amount > 0) {
            setOffsetY(offset_y - getStepHeight() * 3);
        } else {
            setOffsetY(offset_y + getStepHeight() * 3);
        }
    }

    /**
     * The ratio of the scroll bar button that can be dragged relative to the size
     * of the bar (0.0f
     * - 1.0f)
     */
    @Override
    public final float getScrollBarRatio() {
        int totalHeight = getTotalContentHeight();
        int visibleHeight = getVisibleHeight();
        if (totalHeight <= visibleHeight)
            return 1.0f;
        return visibleHeight / (float) totalHeight;
    }

    /**
     * Where the scrollbar is positioned relative to the total content height (0.0f
     * - 1.0f)
     */
    @Override
    public final float getScrollBarOffset() {
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset <= 0)
            return 0.0f;
        return offset_y / (float) maxOffset;
    }

    @Override
    public final void setScrollBarOffset(float offset) {
        System.out.println("Setting scrollbar offset: " + offset);
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset > 0) {
            setOffsetY((int) (offset * maxOffset));
        } else {
            setOffsetY(0);
        }
    }

        // Added missing @Override annotation
    @Override
    public final void setOffsetY(int new_offset) {
        // Get the difference from the old offset to the requested new one
        int diff = new_offset - offset_y;
        // Calculate how far past the min offset we went to and correct it to min (0)
        if(offset_y + diff <= 0) {
            // diff = distance to zero from offset y
            diff = -offset_y;
            offset_y = 0;
        }

        int max_offset_y = getTotalContentHeight() - getVisibleHeight();
        // Calculate how far past the max offset we went and correct it to max (max_offset_y)
        if(offset_y + diff > max_offset_y) {
            diff = max_offset_y - offset_y;
        }

        offset_y = offset_y + diff;
        if(offset_y < 0)
            offset_y = 0;
        if(offset_y > max_offset_y)
            offset_y = max_offset_y;
            
        // new_offset = Math.max(0, Math.min(new_offset, max_offset_y)); // Clamp to range
        // new_offset = (new_offset / normalizedItemHeight) * normalizedItemHeight; // Snap to nearest multiple
        System.out.println("Setting offset_y to: " + offset_y);
        // offset_y = new_offset;
        // Update item positions based on new offset
        // updateItemPositions();
        int count = 0;
        ListElement current = getFirstChild();
        while (current != null) {
            GUIObject gui_object = (GUIObject) current;
            if(gui_object == scroll_bar) {
                current = current.getNext();
                continue;
            }
            count++;
            int x = gui_object.getX();
            int y = gui_object.getY();
            //System.out.println("position of child " + count + ": (" + x + ", " + (y) + ")");
            gui_object.setPos(x, y + diff);
            //System.out.println("Set position of child " + count + " to: (" + x + ", " + (y + offset_y) + ")");

            current = current.getNext();
        }
        System.out.println("children count : " + count);
        scroll_bar.update();
    }

    private int getTotalContentHeight() {
        return total_content_height + 40; // add a buffer?
    }

    private int getVisibleHeight() {
        return content_height;
    }

}

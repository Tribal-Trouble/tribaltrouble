package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public final strictfp class ScrollablePulldownMenu extends PulldownMenu implements Scrollable {
    private ScrollBar scroll_bar;
    private int offset_y = 0; // Tracks the vertical offset for scrolling
    private int render_amount = 6;

    public ScrollablePulldownMenu(int render_amount) {
        super();
        this.render_amount = render_amount;
    }

    @Override
    protected void renderGeometry() {
        // Render bottom edge
        Horizontal bot = Skin.getSkin().getPulldownData().getPulldownBottom();
        bot.render(0, 0, getWidth(), Skin.NORMAL);

        // Render top edge
        Horizontal top = Skin.getSkin().getPulldownData().getPulldownTop();
        top.render(0, getHeight() - top.getHeight() + 5, getWidth(), Skin.NORMAL);
    }

    @Override
    public final void setDim(int width, int height) {
        
        int min_content_width = 0;
        Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        // Adjust all items
        for (int i = 0; i < items.size(); i++) {
            PulldownItem item = (PulldownItem) items.get(i);
            if (item.getTextWidth() > min_content_width) {
                min_content_width = item.getTextWidth();
            }
        }
        int item_pos_count = 0;
        min_content_width = StrictMath.max(
                width,
                item_box.getLeftOffset() + min_content_width + item_box.getRightOffset());
        int item_height = 0;
        for (int i = 0; i < items.size(); i++) {
            PulldownItem item = (PulldownItem) items.get(i);
            item_height = item_box.getBottomOffset() + item.getTextHeight() + item_box.getTopOffset();
            item.setDim(min_content_width, item_height);
            item.setPos(0, item_pos_count);
            item_pos_count -= item_height;
        }

        int item_height_shown = item_height * render_amount;

        if (scroll_bar == null) {
            scroll_bar = new ScrollBar(item_height_shown, this);
            addChild(scroll_bar);
            offset_y = 0; // Reset to top when scroll bar is created
        }
        scroll_bar.setPos(min_content_width, 0);
        
        // We need to skirt the pullable menu super but still set width and height
        setDimSimple(min_content_width + scroll_bar.getWidth(), item_height_shown);

        updateItemPositions(); // Update positions when dimensions change
        scroll_bar.update();

    }   

    // Added missing @Override annotation
    @Override
    public final void setOffsetY(int new_offset) {
        // System.out.println("setOffsetY called - current offset: " + offset_y);
        // System.out.println("Setting offset to " + new_offset + ", total content height: " + getTotalContentHeight()
        //         + ", visible height: " + getVisibleHeight());
        offset_y = new_offset;

        // Clamp offset to valid range
        if (offset_y < 0)
            offset_y = 0;
        int max_offset_y = getTotalContentHeight() - getVisibleHeight();
        if (max_offset_y < 0)
            max_offset_y = 0;
        if (offset_y > max_offset_y)
            offset_y = max_offset_y;

        // Update item positions based on new offset
        updateItemPositions();
        scroll_bar.update();
    }

    @Override
    public final int getOffsetY() {
        return offset_y;
    }

    @Override
    public final int getStepHeight() {
        if (items.isEmpty())
            return 32; // Default fallback

        Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        PulldownItem firstItem = items.get(0);
        return item_box.getBottomOffset() + firstItem.getTextHeight() + item_box.getTopOffset();
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
        int maxOffset = getTotalContentHeight() - getVisibleHeight();
        if (maxOffset > 0) {
            setOffsetY((int) (offset * maxOffset));
        } else {
            setOffsetY(0);
        }
    }

    private int getTotalContentHeight() {
        if (items.isEmpty())
            return 0;

        Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        PulldownItem lastItem = items.get(items.size() - 1);
        int item_height = item_box.getBottomOffset() + lastItem.getTextHeight() + item_box.getTopOffset();
        return items.size() * item_height
                + Skin.getSkin().getPulldownData().getPulldownTop().getHeight();
    }

    private int getVisibleHeight() {
        Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        if (items.isEmpty())
            return 0;
        PulldownItem firstItem = items.get(0);
        int item_height = item_box.getBottomOffset() + firstItem.getTextHeight() + item_box.getTopOffset();
        return render_amount * item_height;
    }

    private void updateItemPositions() {
        // Reposition all items using the same logic as setDim() but with offset from scrolling applied
        Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        int item_pos_count = Skin.getSkin().getPulldownData().getPulldownBottom().getHeight() + offset_y;
        // System.out.println("Render amount * padding: " + (render_amount * item_box.getTopOffset()));
        for (int i = 0; i < items.size(); i++) {
            PulldownItem item = items.get(i);
            int item_height = item_box.getBottomOffset() + item.getTextHeight() + item_box.getTopOffset();
            item.setPos(0, item_pos_count + getHeight() - 32); // TODO: Figure out this offset mathematically?
            item_pos_count -= item_height;
        }
    }

}

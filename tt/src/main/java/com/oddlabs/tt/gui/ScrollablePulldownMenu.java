package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ScrollablePulldownMenu<T> extends PulldownMenu<T> implements Scrollable, Clipped {
    private @Nullable ScrollBar scroll_bar;
    private int offset_y;
    private final int render_amount;
    private int normalizedItemHeight;
    private int button_width;

    public ScrollablePulldownMenu(int render_amount) {
        super();
        this.render_amount = render_amount;
    }

    public int getScrollbarWidth() {
        return scroll_bar != null ? scroll_bar.getWidth() : 0;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Horizontal bot = Skin.getSkin().getPulldownData().pulldownBottom();
        bot.render(renderer, 0, 0, getWidth() - getScrollbarWidth(), ModeIconQuads.Mode.NORMAL);

        Horizontal top = Skin.getSkin().getPulldownData().pulldownTop();
        int addAmount = items.size() > render_amount ? top.getHeight() : 0;
        top.render(renderer, 0, getHeight() - top.getHeight() + addAmount, getWidth() - getScrollbarWidth(),
                ModeIconQuads.Mode.NORMAL);
    }

    public void setButtonWidth(int width) {
        this.button_width = width;
    }

    @Override
    public @NonNull ScrollablePulldownMenu<T> setDim(int width, int height) {
        // Track the button width from PulldownButton.setDim (first external call)
        if (button_width == 0 && width > 0 && items.isEmpty()) {
            button_width = width;
        }

        if (items.size() <= render_amount) {
            super.setDim(width, height);
            return this;
        }

        calculateNormalizedItemHeight();

        Box item_box = Skin.getSkin().getPulldownData().pulldownItem();
        int text_width = 0;
        for (PulldownItem<T> item : items) {
            if (item.getTextWidth() > text_width)
                text_width = item.getTextWidth();
        }
        int min_content_width = Math.max(button_width,
                item_box.getLeftOffset() + text_width + item_box.getRightOffset());

        int item_pos_count = 0;
        for (PulldownItem<T> item : items) {
            item.setDim(min_content_width, normalizedItemHeight);
            item.setPos(0, item_pos_count);
            item_pos_count -= normalizedItemHeight;
        }

        int item_height_shown = normalizedItemHeight * render_amount;

        if (scroll_bar == null) {
            scroll_bar = new ScrollBar(item_height_shown, this);
            addChild(scroll_bar);
            offset_y = 0;
        }
        scroll_bar.setDim(scroll_bar.getWidth(), item_height_shown);
        scroll_bar.setPos(min_content_width, 0);

        super_setDim(min_content_width + scroll_bar.getWidth(), item_height_shown);

        updateItemPositions();
        scroll_bar.update();
        return this;
    }

    private void calculateNormalizedItemHeight() {
        Box item_box = Skin.getSkin().getPulldownData().pulldownItem();
        normalizedItemHeight = 0;
        for (PulldownItem<T> item : items) {
            int h = item_box.getBottomOffset() + item.getTextHeight() + item_box.getTopOffset();
            if (h > normalizedItemHeight) normalizedItemHeight = h;
        }
    }

    private void updateItemPositions() {
        int item_pos_count = offset_y;
        for (PulldownItem<T> item : items) {
            item.setPos(0, item_pos_count + getHeight() - normalizedItemHeight - 2);
            item_pos_count -= normalizedItemHeight;
        }
    }

    @Override
    protected void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED && event.consumeAction(GameAction.UI_CANCEL)) {
            remove();
            return;
        }
        super.handleInput(event);
    }

    @Override
    protected void mouseScrolled(int amount) {
        setOffsetY(offset_y + (amount > 0 ? -1 : 1) * getStepHeight() * 3);
    }

    @Override
    public void setOffsetY(int new_offset) {
        if (items.size() <= render_amount) return;
        int max_offset = Math.max(0, getTotalContentHeight() - getVisibleHeight());
        new_offset = Math.max(0, Math.min(new_offset, max_offset));
        new_offset = (new_offset / normalizedItemHeight) * normalizedItemHeight;
        offset_y = new_offset;
        updateItemPositions();
        if (scroll_bar != null) scroll_bar.update();
    }

    @Override
    public int getOffsetY() {
        return offset_y;
    }

    @Override
    public int getStepHeight() {
        return normalizedItemHeight > 0 ? normalizedItemHeight : 16;
    }

    @Override
    public void jumpPage(boolean up) {
        setOffsetY(offset_y + (up ? -getVisibleHeight() : getVisibleHeight()));
    }

    @Override
    public float getScrollBarRatio() {
        int total = getTotalContentHeight();
        int visible = getVisibleHeight();
        if (total <= visible) return 1f;
        return (float) visible / total;
    }

    @Override
    public float getScrollBarOffset() {
        int max = getTotalContentHeight() - getVisibleHeight();
        if (max <= 0) return 0f;
        return (float) offset_y / max;
    }

    @Override
    public void setScrollBarOffset(float offset) {
        int max = getTotalContentHeight() - getVisibleHeight();
        if (max > 0) setOffsetY((int) (offset * max));
    }

    private int getTotalContentHeight() {
        return items.size() * normalizedItemHeight;
    }

    private int getVisibleHeight() {
        return render_amount * normalizedItemHeight;
    }
}

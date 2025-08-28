package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.util.ListElement;

import java.util.ArrayList;
import java.util.List;

public final strictfp class PulldownMenu extends Group implements Scrollable {
    private final List<ItemChosenListener> chosen_listeners = new ArrayList<>();

    private final List<PulldownItem> items = new ArrayList();
    private int chosen_item_index = -1;

    private ScrollBar scroll_bar;
    private int offset_y = 0; // Tracks the vertical offset for scrolling
    private int render_amount = 6;

    public PulldownMenu() {
        setCanFocus(true);
        setFocusCycle(true);
    }

    public final PulldownItem getItem(int index) {
        return items.get(index);
    }

    public final int getSize() {
        return items.size();
    }

    @Override
    protected final void renderGeometry() {
        // Render bottom edge
        Horizontal bot = Skin.getSkin().getPulldownData().getPulldownBottom();
        bot.render(0, 0, getWidth(), Skin.NORMAL);

        // Render top edge
        Horizontal top = Skin.getSkin().getPulldownData().getPulldownTop();
        top.render(0, getHeight() - top.getHeight(), getWidth(), Skin.NORMAL);
    }

    public final void addItem(PulldownItem item) {        
        items.add(item);
        addChild(item);
        setDim(0, getHeight()); // Let the control recalculate its width when an item is added
        item.addMouseClickListener(new ItemListener(items.size() - 1));
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
                System.out.println("item width at : " + i + "    " + item.getTextWidth());
            }
        }
        int item_pos_count = Skin.getSkin().getPulldownData().getPulldownBottom().getHeight();
        System.out.println("width: " + width);
        ;
        min_content_width = StrictMath.max(
                width, item_box.getLeftOffset() + min_content_width + item_box.getRightOffset());
        for (int i = 0; i < items.size(); i++) {
            PulldownItem item = (PulldownItem) items.get(items.size() - 1 - i);
            int item_height = item_box.getBottomOffset() + item.getTextHeight() + item_box.getTopOffset();
            item.setDim(min_content_width, item_height);
            item.setPos(0, item_pos_count);
            item_pos_count += item_height;
        }
        int min_height = item_pos_count + Skin.getSkin().getPulldownData().getPulldownTop().getHeight();
        int item_height = (item_box.getBottomOffset() + items.get(0).getTextHeight() + item_box.getTopOffset());
        int item_height_shown = item_height * render_amount;
        if (scroll_bar == null) {
            scroll_bar = new ScrollBar(item_height_shown, this);
            System.out.println(
                    "ScrollBar created with width: " + scroll_bar.getWidth() + " for " + items.size() + " items");
            addChild(scroll_bar);
        }
        scroll_bar.setPos(min_content_width, 0);
        System.out.println("Before super.setDim - ScrollBar width: " + scroll_bar.getWidth() + ", Items: "
                + items.size() + ", min_content_width: " + min_content_width);
        super.setDim(min_content_width + scroll_bar.getWidth(), item_height_shown);
        scroll_bar.update();
        System.out.println("After scroll_bar.update() - ScrollBar width: " + scroll_bar.getWidth());
    }

    public final int getChosenItemIndex() {
        return chosen_item_index;
    }

    public final void clearItems() {
        clearChildren();
        items.clear();
    }

    public final void chooseItem(int index) {
        chosen_item_index = index;
        itemChosenAll();
    }

    protected final void focusNotify(boolean focus) {
        if (!focus) {
            System.out.println("PulldownMenu lost focus, deactivating menu");
            remove();
        }
    }

    // Reverted to traditional switch syntax for Java 8 compatibility
    protected final void keyRepeat(KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case Keyboard.KEY_UP:
                focusPrior();
                break;
            case Keyboard.KEY_DOWN:
                focusNext();
                break;
            default:
                super.keyRepeat(event);
                break;
        }
    }

    // Sending click on to appropiate item when PulldownButton has been pressed and
    // released on an
    // item
    protected final void clickItem(int button, int x, int y, int clicks) {
        for (int i = 0; i < items.size(); i++) {
            PulldownItem item = getItem(i);
            if (item.isHovered())
                item.mouseClickedAll(button, x, y, clicks);
        }
    }

    public final void itemChosenAll() {
        for (int i = 0; i < chosen_listeners.size(); i++) {
            ItemChosenListener listener = (ItemChosenListener) chosen_listeners.get(i);
            if (listener != null)
                listener.itemChosen(this, chosen_item_index);
        }
    }

    public final void addItemChosenListener(ItemChosenListener listener) {
        chosen_listeners.add(listener);
    }

    public final void removeItemChosenListener(ItemChosenListener listener) {
        chosen_listeners.remove(listener);
    }

    public final strictfp class ItemListener implements MouseClickListener {
        private final int index;

        public ItemListener(int index) {
            this.index = index;
        }

        public final void mouseClicked(int button, int x, int y, int clicks) {
            chooseItem(index);
        }
    }

    // Added missing @Override annotation
    @Override
    public final void setOffsetY(int new_offset) {
        // offset_y = new_offset;
        // System.out.println("New offset Y: " + new_offset);
        // if (offset_y < 0) offset_y = 0;
        // offset_y = 0;
        offset_y = 0;
        scroll_bar.update();
    }

    @Override
    public final int getOffsetY() {
        return offset_y;
    }

    @Override
    public final int getStepHeight() {
        // Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        // System.out.println("getStepHeight called" + (item_box.getTopOffset() +
        // item_box.getBottomOffset()));
        // return item_box.getTopOffset() + item_box.getBottomOffset();
        return 32;
    }

    @Override
    public final void jumpPage(boolean up) {
        // if (up) setOffsetY(offset_y - getHeight());
        // else setOffsetY(offset_y + getHeight());
    }

    @Override
    public final float getScrollBarRatio() {
        // int item_height = (item_box.getBottomOffset() + items.get(0).getTextHeight()
        // + item_box.getTopOffset());
        // int item_height_shown = item_height * render_amount;
        // System.out.println("getScrollBarRatio called" + getHeight() + " " +
        // offset_y);
        return .2f;
    }

    @Override
    public final float getScrollBarOffset() {
        // Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
        // int text_height = items != null && !items.isEmpty() ?
        // items.get(0).getTextHeight() : 0;
        // int item_height = (item_box.getBottomOffset() + text_height +
        // item_box.getTopOffset());
        // int length = StrictMath.max(item_height * items.size(), offset_y +
        // getHeight());
        // int item_height_shown = item_height * render_amount;
        // return offset_y / (float) (length - item_height_shown);
        return .3f;
    }

    @Override
    public final void setScrollBarOffset(float offset) {
        // int length = StrictMath.max(getHeight(), offset_y + getHeight());
        // setOffsetY((int) (offset * (length - getHeight())));
        setOffsetY(0);
    }
}

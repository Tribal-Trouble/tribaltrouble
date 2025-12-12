package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class PulldownMenu<T> extends Group {
	private final Set<@NonNull ItemChosenListener<T>> chosen_listeners = new CopyOnWriteArraySet<>();

	private final List<@NonNull PulldownItem<T>> items = new ArrayList<>();
	private int chosen_item_index = -1;
	
	public PulldownMenu() {
		setCanFocus(true);
		setFocusCycle(true);
	}

	public @NonNull PulldownItem<T> getItem(int index) {
		return items.get(index);
	}

	public int getSize() {
		return items.size();
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		// Render bottom edge
		Horizontal bot = Skin.getSkin().getPulldownData().getPulldownBottom();
		bot.render(renderer, 0, 0, getWidth(), ModeIconQuads.Mode.NORMAL);

		// Render top edge
        Horizontal top = Skin.getSkin().getPulldownData().getPulldownTop();
        top.render(renderer, 0, getHeight() - top.getHeight(), getWidth(), ModeIconQuads.Mode.NORMAL);
	}

	public void addItem(@NonNull PulldownItem<T> item) {
		items.add(item);
		addChild(item);
		item.addMouseClickListener(new ItemListener(items.size() - 1));
		setDim(getWidth(), getHeight());
	}

	@Override
	public void setDim(int width, int height) {
		int min_width = 0;
		Box item_box = Skin.getSkin().getPulldownData().getPulldownItem();
		// Adjust all items
            for (PulldownItem<T> item : items) {
                if (item.getTextWidth() > min_width)
                    min_width = item.getTextWidth();
            }
		int item_pos_count = Skin.getSkin().getPulldownData().getPulldownBottom().getHeight();
		min_width = Math.max(width, item_box.getLeftOffset() + min_width + item_box.getRightOffset());
		for (int i = 0; i < items.size(); i++) {
			PulldownItem<T> item = items.get(items.size() - 1 - i);
			int item_height = item_box.getBottomOffset() + item.getTextHeight() + item_box.getTopOffset();
			item.setDim(min_width, item_height);
			item.setPos(0, item_pos_count);
			item_pos_count += item_height;
		}
		int min_height = Math.max(height, item_pos_count + Skin.getSkin().getPulldownData().getPulldownTop().getHeight());
		super.setDim(min_width, min_height);
	}

	public int getChosenItemIndex() {
		return chosen_item_index;
	}

	public void chooseItem(int index) {
		chosen_item_index = index;
		itemChosenAll();
	}

	@Override
	protected void focusNotify(boolean focus) {
		if (!focus) {
			remove();
		}
	}

	@Override
	protected void keyRepeat(@NonNull KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case UP -> focusPrior();
            case DOWN -> focusNext();
            default -> super.keyRepeat(event);
        }
	}

	// Sending click on to appropriate item when PulldownButton has been pressed and released on an item
	void clickItem (@NonNull MouseButton button, int x, int y, int clicks) {
        for (PulldownItem<T> item : items) {
            if (item.isHovered())
                item.mouseClickedAll(button, x, y, clicks);
        }
	}

	public void itemChosenAll() {
        for (ItemChosenListener <T> listener : chosen_listeners) {
            listener.itemChosen(this, chosen_item_index);
        }
	}

	public void addItemChosenListener(@NonNull ItemChosenListener<T> listener) {
		chosen_listeners.add(listener);
	}

	public void removeItemChosenListener(@NonNull ItemChosenListener<T> listener) {
		chosen_listeners.remove(listener);
	}

	public final class ItemListener implements MouseClickListener {
		private final int index;

		public ItemListener(int index) {
			this.index = index;
		}

		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			chooseItem(index);
		}
	}
}

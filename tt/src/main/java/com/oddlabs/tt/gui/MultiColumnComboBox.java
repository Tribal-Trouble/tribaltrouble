package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class MultiColumnComboBox<T> extends GUIObject implements Scrollable {
	private final @NonNull ColumnInfo @NonNull [] column_infos;
	private final RadioButtonGroup group = new RadioButtonGroup();
	private final Group focus_group = new Group();
	private final RowCollection<T> rows = new RowCollection<>(this, 0, true);
	private final @NonNull ScrollBar scroll_bar;
	private final boolean use_buttons;
	private final @NonNull GUIRoot gui_root;
	private int offset_y = 0;
	private @Nullable PulldownMenu<T> pulldown_menu = null;
	private @Nullable T right_clicked_row_data;

	public MultiColumnComboBox(@NonNull GUIRoot gui_root, @NonNull ColumnInfo @NonNull [] column_infos, int height) {
		this(gui_root, column_infos, height, true);
	}

	public MultiColumnComboBox(@NonNull GUIRoot gui_root, @NonNull ColumnInfo @NonNull [] column_infos, int height, boolean use_buttons) {
		this.gui_root = gui_root;
		this.column_infos = column_infos;
		this.use_buttons = use_buttons;
		Box box = Skin.getSkin().getMultiColumnComboBoxData().box();
		int width = 0;
		for (int i = 0; i < column_infos.length; i++) {
			ColumnButton<T> column_button = new ColumnButton<>(group, rows, column_infos[i], i, true);
			if (use_buttons) {
				column_button.setPos(width, height - column_button.getHeight());
				focus_group.addChild(column_button);
			}
			width += column_button.getWidth();
		}
		scroll_bar = new ScrollBar(height, this);
		scroll_bar.setPos(width, 0);
		scroll_bar.setTabStop(false);
		focus_group.addChild(scroll_bar);
		setDim(width + scroll_bar.getWidth(), height);
		focus_group.setDim(getWidth(), getHeight());
		focus_group.setPos(0, 0);
		addChild(focus_group);
		if (use_buttons)
			rows.setDim(width - box.getLeftOffset() - box.getRightOffset(), height - box.getBottomOffset() - box.getTopOffset() - group.getMarked().getHeight());
		else
			rows.setDim(width - box.getLeftOffset() - box.getRightOffset(), height - box.getBottomOffset() - box.getTopOffset());
		rows.setPos(box.getLeftOffset(), box.getBottomOffset());
		focus_group.addChild(rows);
		setCanFocus(true);
		scroll_bar.update();
	}

	public int getSize() {
		return rows.getSize();
	}
	
	public void clickedRow() {
		T selected = rows.getSelected();
		if (null != selected) {
			listeners.forEach(listener -> {
				//noinspection rawtypes
				if (listener instanceof RowListener rowListener) //noinspection unchecked
					rowListener.rowChosen(selected);
			});
		}
	}

	@Override
	protected void handleInput(@NonNull InputEvent event) {
		if (event.getPhase() == InputPhase.RELEASED) {
			if (event.consumeAction(GameAction.UI_ACTIVATE)) {
				doubleClickedRow();
				event.consume();
				return;
			}
		}
		if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
			boolean consumed = true;
            boolean navigated = false;
			if (event.consumeAction(GameAction.UI_NAV_UP)) {
				rows.selectPrior();
				clickedRow();
                navigated = true;
			} else if (event.consumeAction(GameAction.UI_NAV_DOWN)) {
				rows.selectNext();
				clickedRow();
                navigated = true;
			} else if (event.consumeAction(GameAction.UI_NAV_HOME)) {
				rows.selectFirst();
				clickedRow();
                navigated = true;
			} else if (event.consumeAction(GameAction.UI_NAV_END)) {
				rows.selectLast();
				clickedRow();
                navigated = true;
			} else if (event.consumeAction(GameAction.UI_NAV_PAGE_UP)) {
				jumpPage(true);
                navigated = true;
			} else if (event.consumeAction(GameAction.UI_NAV_PAGE_DOWN)) {
				jumpPage(false);
                navigated = true;
			} else {
				consumed = false;
			}
			
            if (navigated) rows.setFocus();
			
			if (consumed) {
				event.consume();
				return;
			}
		}
		super.handleInput(event);
	}

	public void addRowListener(@NonNull RowListener<T> listener) {
		listeners.add(listener);
	}

	public void doubleClickedRow() {
		T selected = rows.getSelected();
		if (null != selected) {
			listeners.forEach(listener -> {
                //noinspection rawtypes
                if (listener instanceof RowListener rowListener)
                    //noinspection unchecked
                    rowListener.rowDoubleClicked(selected);
			});
		}
	}

	public void setPulldownMenu(PulldownMenu<T> pulldown_menu) {
		this.pulldown_menu = pulldown_menu;
	}

	public void rightClickedRow(int x, int y) {
		if (pulldown_menu != null) {
			int pulldown_x = Math.clamp(x, 0, gui_root.getWidth() - pulldown_menu.getWidth());
			int pulldown_y = Math.clamp(y - pulldown_menu.getHeight(), 0, gui_root.getHeight() - pulldown_menu.getHeight());
			pulldown_menu.setPos(pulldown_x, pulldown_y);
			gui_root.getDelegate().addChild(pulldown_menu);
			pulldown_menu.setFocus();
			right_clicked_row_data = getSelected();
		}
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Box box = Skin.getSkin().getMultiColumnComboBoxData().box();
        var mode = (isActive() || getFocusedChild() != null) ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;
		box.render(renderer, 0f, 0f, getWidth() - scroll_bar.getWidth(), getHeight() - (use_buttons ? group.getMarked().getHeight() : 0), mode);
	}

	@Override
	public void setFocus() {
		focus_group.setGroupFocus(Renderer.getLocalInput().isShiftDownCurrently() ? -1 : 1);
	}

	public void clear() {
		rows.clear();
	}

	public void addRow(@NonNull Row<T,?> row) {
		row.setColumnInfos(column_infos);
		rows.addRow(row);
		scroll_bar.update();
	}

	public @Nullable T getSelected() {
		return rows.getSelected();
	}

	public @Nullable T getRightClickedRowData() {
		return right_clicked_row_data;
	}

	public void selectRow(@NonNull Row<T,?> row) {
		rows.selectRow(row);
	}

	@Override
	protected void mouseScrolled(int amount) {
        setOffsetY(offset_y + (amount > 0 ? - 3 : 3) * getStepHeight());
	}

	@Override
	public void setOffsetY(int new_offset) {
		offset_y = Math.clamp(new_offset, 0, Math.max(rows.getContentHeight() - rows.getHeight(), 0));
		rows.replaceRows();
		scroll_bar.update();
	}

	@Override
	public int getOffsetY() {
		return offset_y;
	}

	@Override
	public int getStepHeight() {
		return Skin.getSkin().getMultiColumnComboBoxData().font().getHeight();
	}

	@Override
	public void jumpPage(boolean up) {
		if (up)
			setOffsetY(offset_y - rows.getHeight());
		else
			setOffsetY(offset_y + rows.getHeight());
	}

	@Override
	public float getScrollBarRatio() {
		return rows.getHeight()/(float)Math.max(rows.getContentHeight(), offset_y + rows.getHeight());
	}

	@Override
	public float getScrollBarOffset() {
		int length = Math.max(rows.getContentHeight(), offset_y + rows.getHeight());
		return offset_y/(float)(length - rows.getHeight());
	}

	@Override
	public void setScrollBarOffset(float offset) {
		int length = Math.max(rows.getContentHeight(), offset_y + rows.getHeight());
		setOffsetY((int)(offset*(length - rows.getHeight())));
	}
}

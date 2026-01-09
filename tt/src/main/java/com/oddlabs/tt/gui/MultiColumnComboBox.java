package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MultiColumnComboBox<T> extends GUIObject implements Scrollable {
	private final @NonNull ColumnInfo @NonNull [] column_infos;
	private final RadioButtonGroup group = new RadioButtonGroup();
	private final Group focus_group = new Group();
	private final RowCollection<T> rows = new RowCollection<>(this, 0, true);
	private final List<@NonNull RowListener<T>> row_listeners = new ArrayList<>();
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
		this.column_infos = column_infos;
		this.use_buttons = use_buttons;
		this.gui_root = gui_root;
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
		addChild(rows);
		setCanFocus(true);
		scroll_bar.update();
	}

	public int getSize() {
		return rows.getSize();
	}
	
	public void clickedRow() {
        row_listeners.forEach(listener -> listener.rowChosen(rows.getSelected()));
	}

	@Override
	protected void keyRepeat(@NonNull KeyboardEvent event) {
		switch (event.keyCode()) {
			case UP:
				rows.selectPrior();
				clickedRow();
				break;
			case DOWN:
				rows.selectNext();
				clickedRow();
				break;
			case HOME:
				rows.selectFirst();
				clickedRow();
				break;
			case END:
				rows.selectLast();
				clickedRow();
				break;
			case PRIOR:
				jumpPage(true);
				break;
			case NEXT:
				jumpPage(false);
				break;
			default:
				super.keyRepeat(event);
				break;
		}
	}

	public void addRowListener(@NonNull RowListener<T> listener) {
		row_listeners.add(listener);
	}

	public void doubleClickedRow() {
        row_listeners.forEach(listener -> listener.rowDoubleClicked(rows.getSelected()));
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

	public void selectRow(Row<T,?> row) {
		rows.selectRow(row);
	}

	@Override
	protected void mouseScrolled(int amount) {
        setOffsetY(offset_y + (amount > 0 ? - 3 : 3) * getStepHeight());
	}

	@Override
	public void setOffsetY(int new_offset) {
		offset_y = new_offset;

		if (offset_y < 0)
			offset_y = 0;
		int max_offset_y = rows.getContentHeight() - rows.getHeight();
		if (max_offset_y < 0)
			max_offset_y = 0;
		if (offset_y > max_offset_y)
			offset_y = max_offset_y;
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

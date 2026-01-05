package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class RowCollection<T> extends GUIObject implements Clipped {
	private final List<@NonNull Row<T,?>> rows = new ArrayList<>();
	private final @NonNull MultiColumnComboBox<T> multi_box;
	private @Nullable Row<T,?> selected_row;
	private int sort_index;
	private boolean sorted_descending;

    RowCollection(@NonNull MultiColumnComboBox<T> multi_box, int sort_index, boolean sorted_descending) {
		this.multi_box = multi_box;
		this.sort_index = sort_index;
		this.sorted_descending = sorted_descending;
		setCanFocus(true);
		setTabStop(false);
	}

	void selectNext() {
		if (rows.isEmpty()) return;
		int index = selected_row == null ? -1 : rows.indexOf(selected_row);
		if (sorted_descending) {
			index++;
		} else {
			index--;
		}
		if (index >= 0 && index < rows.size()) {
			selectRow(rows.get(index));
			ensureVisible(selected_row);
		}
	}

	void selectPrior() {
		if (rows.isEmpty()) return;
		int index = selected_row == null ? -1 : rows.indexOf(selected_row);
		if (sorted_descending) {
			index--;
		} else {
			index++;
		}
		if (index >= 0 && index < rows.size()) {
			selectRow(rows.get(index));
			ensureVisible(selected_row);
		}
	}

	void selectFirst() {
		if (rows.isEmpty()) return;
		ensureVisible(selectRow(sorted_descending ? rows.getFirst() : rows.getLast()));
	}

	void selectLast() {
		if (rows.isEmpty()) return;
		ensureVisible(selectRow(sorted_descending ? rows.getLast() : rows.getFirst()));
	}

	private void ensureVisible(@NonNull Row<T,?> row) {
		Scrollable scrollable = (Scrollable) getParent();
		int row_top = row.getY() + row.getHeight();
		int row_bottom = row.getY();
		int view_top = getHeight() + scrollable.getOffsetY();
		int view_bottom = scrollable.getOffsetY();

		if (row_top > view_top) {
			scrollable.setOffsetY(row_top - getHeight());
		} else if (row_bottom < view_bottom) {
			scrollable.setOffsetY(row_bottom);
		}
	}

	public void clear() {
        for (Row<T,?> row : rows) {
            row.remove();
        }
		rows.clear();
		selected_row = null;
		replaceRows();
	}

	void addRow(@NonNull Row<T,?> row) {
		rows.add(row);
		row.addMouseClickListener((@NonNull MouseButton button, int x, int y, int clicks) -> {
            selectRow(row);
            if (button == MouseButton.RIGHT) {
                multi_box.clickedRow();
                multi_box.rightClickedRow((int)(row.getRootX() + x), (int)(row.getRootY() + y));
            } else if (clicks == 1) {
                multi_box.clickedRow();
            } else if (clicks == 2) {
                multi_box.doubleClickedRow();
            }
        });
		row.setSortIndex(sort_index);
		addChild(row);
        rows.sort(null);
		replaceRows();
	}

	public int getSize() {
		return rows.size();
	}

	void markChanged(int index, boolean sorted_descending) {
		sort_index = index;
		this.sorted_descending = sorted_descending;
        for (Row<T,?> row : rows) {
            row.setSortIndex(sort_index);
        }
        rows.sort(null);
		replaceRows();
	}

	void replaceRows() {
		int y = getHeight() + ((Scrollable)getParent()).getOffsetY();
		for (int i = 0; i < rows.size(); i++) {
			Row<T,?> row;
			if (sorted_descending)
				row = rows.get(i);
			else
				row = rows.get(rows.size() - i - 1);
			y -= row.getHeight();
			row.setPos(0, y);
            var data = Skin.getSkin().getMultiColumnComboBoxData();
            row.setColor(i % 2 == 0 ? data.color1() : data.color2());
		}
	}

	int getContentHeight() {
        return rows.stream().mapToInt(Row::getHeight).sum();
	}

	public @Nullable T getSelected() {
        return selected_row != null ? selected_row.getContentObject() : null;
	}

	@NonNull Row<T,?> selectRow(@NonNull Row<T,?> row) {
		assert rows.contains(row); 
		if (selected_row != null)
			selected_row.mark(false);
		selected_row = row;
		selected_row.mark(true);
		return selected_row;
	}
}

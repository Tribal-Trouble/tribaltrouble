package com.oddlabs.tt.gui;

import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class Row<T,C extends GUIObject & Comparable<C>> extends GUIObject implements Comparable<Row<T,C>> {
	private final @NonNull C @NonNull [] columns;
	private final @Nullable T content_object;
	private int sort_index;
	private int color = com.oddlabs.util.Color.TRANSPARENT_INT;
	private boolean marked = false;

	public Row(@NonNull C @NonNull [] columns, @Nullable T content_object) {
		this.columns = columns;
		this.content_object = content_object;
		setDim(0, columns[0].getHeight());
		setCanFocus(true);
	}

	public @NonNull C getColumn(int index) {
		return columns[index];
	}

	public void setColumnInfos(@NonNull ColumnInfo @NonNull [] column_infos) {
		int x = 0;
		for (int i = 0; i < column_infos.length; i++) {
			C gui_object = getColumn(i);
			gui_object.setPos(x, 0);
			addChild(gui_object);
			x += column_infos[i].getWidth();

			// if left most column, correct for the radio button starting without left_offset
			if (i == 0)
				x -= Skin.getSkin().getMultiColumnComboBoxData().getBox().getLeftOffset(); 
			// if right most column, correct for the radio button extending over right_offset
			if (i == column_infos.length - 1)
				x -= Skin.getSkin().getMultiColumnComboBoxData().getBox().getRightOffset(); 
		}
		setDim(x, getHeight());
	}

	public void setSortIndex(int sort_index) {
		this.sort_index = sort_index;
	}

	@Override
	public int compareTo(@NonNull Row<T,C> o) {
		return getColumn(sort_index).compareTo(o.getColumn(sort_index));
	}

	public void setColor(int color) {
		this.color = color;
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        var c = marked ? Skin.getSkin().getMultiColumnComboBoxData().getColorMarked() : color;
		if (c != Color.TRANSPARENT_INT) {
			float[] ca = Color.argb4f(c);
			GL11.glColor4f(ca[0], ca[1], ca[2], ca[3]);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(0, 0);
			GL11.glVertex2f(0, getHeight());
			GL11.glVertex2f(getWidth(), getHeight());
			GL11.glVertex2f(getWidth(), 0);
			GL11.glEnd();
		}
	}

	public @Nullable T getContentObject() {
		return content_object;
	}

	public void mark(boolean marked) {
		this.marked = marked;
	}
}

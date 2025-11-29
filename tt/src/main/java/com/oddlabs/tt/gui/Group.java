package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public class Group extends GUIObject {
	public Group() {
		this(true);
	}
	
	public Group(boolean can_focus) {
		setCanFocus(can_focus);
	}

	public void compileCanvas() {
		compileCanvas(0, 0, 0, 0);
	}

	public void compileCanvas(int left_offset, int bottom_offset, int right_offset, int top_offset) {
		// tl = Top Left and br = Bottom Right
		int min_x_tl = 0;
		int min_y_tl = 0;
		int max_x_tl = 0;
		int max_y_tl = 0;
		int min_x_br = 0;
		int min_y_br = 0;
		int max_x_br = 0;
		int max_y_br = 0;
		boolean origin_top_left = false;
		boolean origin_bottom_right = false;

		// Calculate the width an height of the top_left- and bottom_right blocks.
		GUIObject current = getFirstChild();
		while (current != null) {
			if (current.getOrigin() == Origin.AT_START) {
				origin_top_left = true;
				int x = current.getX();
				int y = current.getY();
				if (x < min_x_tl)
					min_x_tl = x;
				if (y < min_y_tl)
					min_y_tl = y;
				x += current.getWidth();
				y += current.getHeight();
				if (x > max_x_tl)
					max_x_tl = x;
				if (y > max_y_tl)
					max_y_tl = y;
			} else {
				origin_bottom_right = true;
				int x = current.getX();
				int y = current.getY();
				if (x < min_x_br)
					min_x_br = x;
				if (y < min_y_br)
					min_y_br = y;
				x += current.getWidth();
				y += current.getHeight();
				if (x > max_x_br)
					max_x_br = x;
				if (y > max_y_br)
					max_y_br = y;
			}
			current = current.getNext();
		}

		// find the width and height of the group
		int top_left_width = max_x_tl - min_x_tl + left_offset + right_offset;
		int bottom_right_width = max_x_br - min_x_br + left_offset + right_offset;
		int width = Math.max(top_left_width, bottom_right_width);
		int height = (max_y_tl - min_y_tl) + (max_y_br - min_y_br) + top_offset + bottom_offset;
		if (origin_top_left && origin_bottom_right)
			height += Skin.getSkin().getFormData().getSectionSpacing();
		setDim(width, height);

		// correct the objects positions.
		current = getFirstChild();
		while (current != null) {
			GUIObject gui_object = current;

			if (gui_object.getOrigin() == Origin.AT_START) {
				gui_object.correctPos(-min_x_tl + left_offset,
									  height - max_y_tl - top_offset);
			} else {
				gui_object.correctPos(width - max_x_br - right_offset,
									  -min_y_br + bottom_offset);
			}
			current = current.getNext();
		}
		current = null;
	}

	@Override
	protected void keyRepeat(@NonNull KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_TAB:
				switchFocus(event.isShiftDown() ? -1 : 1);
				break;
			default:
				super.keyRepeat(event);
				break;
		}
	}

	public void setGroupFocus(int dir) {
		setFocus();
		switchFocus(dir);
	}

/*
	public final void correctPos(int dx, int dy) {
		setPos(getX() + dx, getY() + dy);
		ListElement current = getLastChild();
		while (current != null) {
			((GUIObject)current).correctPosRecurseGroup(getX(), getY());
			current = current.getNext();
		}
	}
*/
}

package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.util.Quad;

public final class PulldownData {
	private final Horizontal pulldown_top;
	private final Horizontal pulldown_bottom;
	private final Box pulldown_item;
	private final Horizontal pulldown_button;
	private final Quad[] arrow;
	private final int arrow_offset_right;
	private final int text_offset_left;
	private final Font font;

	public PulldownData(Horizontal pulldown_top,
						Horizontal pulldown_bottom,
						Box pulldown_item,
						Horizontal pulldown_button,
						Quad[] arrow,
						int arrow_offset_right,
						int text_offset_left,
						Font font) {
		this.pulldown_top = pulldown_top;
		this.pulldown_bottom = pulldown_bottom;
		this.pulldown_item = pulldown_item;
		this.pulldown_button = pulldown_button;
		this.arrow = arrow;
		this.arrow_offset_right = arrow_offset_right;
		this.text_offset_left = text_offset_left;
		this.font = font;
	}

	public Horizontal getPulldownTop() {
		return pulldown_top;
	}

	public Horizontal getPulldownBottom() {
		return pulldown_bottom;
	}

	public Box getPulldownItem() {
		return pulldown_item;
	}

	public Horizontal getPulldownButton() {
		return pulldown_button;
	}

	public Quad[] getArrow() {
		return arrow;
	}

	public int getArrowOffsetRight() {
		return arrow_offset_right;
	}

	public int getTextOffsetLeft() {
		return text_offset_left;
	}

	public Font getFont() {
		return font;
	}
}

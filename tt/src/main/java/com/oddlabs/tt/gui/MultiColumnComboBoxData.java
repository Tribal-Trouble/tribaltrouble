package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.util.Quad;

public final class MultiColumnComboBoxData {
	private final Box box;
	private final Horizontal button_pressed;
	private final Horizontal button_unpressed;
	private final Quad[] descending;
	private final Quad[] ascending;
	private final Color color1;
	private final Color color2;
	private final Color color_marked;
	private final Font font;
	private final int caption_offset;

	public MultiColumnComboBoxData(Box box,
								   Horizontal button_pressed,
								   Horizontal button_unpressed,
								   Quad[] descending,
								   Quad[] ascending,
								   Color color1,
								   Color color2,
								   Color color_marked,
								   Font font,
								   int caption_offset) {
		this.box = box;
		this.button_pressed = button_pressed;
		this.button_unpressed = button_unpressed;
		this.descending = descending;
		this.ascending = ascending;
		this.color1 = color1;
		this.color2 = color2;
		this.color_marked = color_marked;
		this.font = font;
		this.caption_offset = caption_offset;
	}

	public Box getBox() {
		return box;
	}

	public Horizontal getButtonPressed() {
		return button_pressed;
	}

	public Horizontal getButtonUnpressed() {
		return button_unpressed;
	}

	public Quad[] getDescending() {
		return descending;
	}

	public Quad[] getAscending() {
		return ascending;
	}

	public Color getColor1() {
		return color1;
	}

	public Color getColor2() {
		return color2;
	}

	public Color getColorMarked() {
		return color_marked;
	}

	public Font getFont() {
		return font;
	}

	public int getCaptionOffset() {
		return caption_offset;
	}
}

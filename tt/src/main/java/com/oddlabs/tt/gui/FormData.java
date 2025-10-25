package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.util.Quad;

public final class FormData {
	private final Box form;
	private final Box slim_form;
	private final Quad[] form_close;
	private final int object_spacing;
	private final int section_spacing;
	private final int caption_left;
	private final int caption_y;
	private final int close_right;
	private final int close_top;
	private final Font caption_font;

	public FormData(Box form, Box slim_form, Quad[] form_close, int object_spacing, int section_spacing, int caption_left, int caption_y, int close_right, int close_top, Font caption_font) {
		this.form = form;
		this.slim_form = slim_form;
		this.form_close = form_close;
		this.object_spacing = object_spacing;
		this.section_spacing = section_spacing;
		this.caption_left = caption_left;
		this.caption_y = caption_y;
		this.close_right = close_right;
		this.close_top = close_top;
		this.caption_font = caption_font;
	}

	public Box getForm() {
		return form;
	}

	public Box getSlimForm() {
		return slim_form;
	}

	public Quad[] getFormClose() {
		return form_close;
	}

	public int getObjectSpacing() {
		return object_spacing;
	}

	public int getSectionSpacing() {
		return section_spacing;
	}

	public int getCaptionLeft() {
		return caption_left;
	}

	public int getCaptionY() {
		return caption_y;
	}

	public int getCloseRight() {
		return close_right;
	}

	public int getCloseTop() {
		return close_top;
	}

	public Font getCaptionFont() {
		return caption_font;
	}
}

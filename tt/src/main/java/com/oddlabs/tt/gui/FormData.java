package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public final class FormData {
	private final @NonNull Box form;
	private final @NonNull Box slim_form;
	private final @NonNull ModeIconQuads form_close;
	private final int object_spacing;
	private final int section_spacing;
	private final int caption_left;
	private final int caption_y;
	private final int close_right;
	private final int close_top;
	private final @NonNull Font caption_font;

	FormData(@NonNull Box form, @NonNull Box slim_form, @NonNull ModeIconQuads form_close, int object_spacing, int section_spacing, int caption_left, int caption_y, int close_right, int close_top, @NonNull Font caption_font) {
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

	public @NonNull Box getForm() {
		return form;
	}

    @NonNull Box getSlimForm() {
		return slim_form;
	}

    @NonNull ModeIconQuads getFormClose() {
		return form_close;
	}

	public int getObjectSpacing() {
		return object_spacing;
	}

	public int getSectionSpacing() {
		return section_spacing;
	}

	int getCaptionLeft() {
		return caption_left;
	}

	int getCaptionY() {
		return caption_y;
	}

	int getCloseRight() {
		return close_right;
	}

	int getCloseTop() {
		return close_top;
	}

	@NonNull Font getCaptionFont() {
		return caption_font;
	}
}

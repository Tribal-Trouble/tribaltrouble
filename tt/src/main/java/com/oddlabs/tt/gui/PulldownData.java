package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public final class PulldownData {
	private final @NonNull Horizontal pulldown_top;
	private final @NonNull Horizontal pulldown_bottom;
	private final @NonNull Box pulldown_item;
	private final @NonNull Horizontal pulldown_button;
	private final @NonNull ModeIconQuads arrow;
	private final int arrow_offset_right;
	private final int text_offset_left;
	private final @NonNull Font font;

	public PulldownData(@NonNull Horizontal pulldown_top,
                        @NonNull Horizontal pulldown_bottom,
                        @NonNull Box pulldown_item,
                        @NonNull Horizontal pulldown_button,
                        @NonNull ModeIconQuads arrow,
                        int arrow_offset_right,
                        int text_offset_left,
                        @NonNull Font font) {
		this.pulldown_top = pulldown_top;
		this.pulldown_bottom = pulldown_bottom;
		this.pulldown_item = pulldown_item;
		this.pulldown_button = pulldown_button;
		this.arrow = arrow;
		this.arrow_offset_right = arrow_offset_right;
		this.text_offset_left = text_offset_left;
		this.font = font;
	}

	public @NonNull Horizontal getPulldownTop() {
		return pulldown_top;
	}

	public @NonNull Horizontal getPulldownBottom() {
		return pulldown_bottom;
	}

	public @NonNull Box getPulldownItem() {
		return pulldown_item;
	}

	public @NonNull Horizontal getPulldownButton() {
		return pulldown_button;
	}

	public @NonNull ModeIconQuads getArrow() {
		return arrow;
	}

	public int getArrowOffsetRight() {
		return arrow_offset_right;
	}

	public int getTextOffsetLeft() {
		return text_offset_left;
	}

	public @NonNull Font getFont() {
		return font;
	}
}

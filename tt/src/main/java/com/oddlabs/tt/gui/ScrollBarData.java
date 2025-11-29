package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public final class ScrollBarData {
	private final @NonNull Vertical scroll_bar;
	private final @NonNull ModeIconQuads scroll_down_button_pressed;
	private final @NonNull ModeIconQuads scroll_down_button_unpressed;
	private final @NonNull ModeIconQuads scroll_down_arrow;
	private final @NonNull ModeIconQuads scroll_up_button_pressed;
	private final @NonNull ModeIconQuads scroll_up_button_unpressed;
	private final @NonNull ModeIconQuads scroll_up_arrow;
	private final @NonNull Vertical scroll_button;
	private final int left_offset;
	private final int bottom_offset;
	private final int top_offset;

	public ScrollBarData(@NonNull Vertical scroll_bar,
                         @NonNull ModeIconQuads scroll_down_button_pressed,
                         @NonNull ModeIconQuads scroll_down_button_unpressed,
                         @NonNull ModeIconQuads scroll_down_arrow,
                         @NonNull ModeIconQuads scroll_up_button_pressed,
                         @NonNull ModeIconQuads scroll_up_button_unpressed,
                         @NonNull ModeIconQuads scroll_up_arrow,
                         @NonNull Vertical scroll_button,
						 int left_offset,
						 int bottom_offset,
						 int top_offset) {
		 this.scroll_bar = scroll_bar;
		 this.scroll_down_button_pressed = scroll_down_button_pressed;
		 this.scroll_down_button_unpressed = scroll_down_button_unpressed;
		 this.scroll_down_arrow = scroll_down_arrow;
		 this.scroll_up_button_pressed = scroll_up_button_pressed;
		 this.scroll_up_button_unpressed = scroll_up_button_unpressed;
		 this.scroll_up_arrow = scroll_up_arrow;
		 this.scroll_button = scroll_button;
		 this.left_offset = left_offset;
		 this.bottom_offset = bottom_offset;
		 this.top_offset = top_offset;
	}

	public @NonNull Vertical getScrollBar() {
		return scroll_bar;
	}

	public @NonNull ModeIconQuads getScrollDownButtonPressed() {
		return scroll_down_button_pressed;
	}

	public @NonNull ModeIconQuads getScrollDownButtonUnpressed() {
		return scroll_down_button_unpressed;
	}

	public @NonNull ModeIconQuads getScrollDownArrow() {
		return scroll_down_arrow;
	}

	public @NonNull ModeIconQuads getScrollUpButtonPressed() {
		return scroll_up_button_pressed;
	}

	public @NonNull ModeIconQuads getScrollUpButtonUnpressed() {
		return scroll_up_button_unpressed;
	}

	public @NonNull ModeIconQuads getScrollUpArrow() {
		return scroll_up_arrow;
	}

	public @NonNull Vertical getScrollButton() {
		return scroll_button;
	}

	public int getLeftOffset() {
		return left_offset;
	}

	public int getBottomOffset() {
		return bottom_offset;
	}

	public int getTopOffset() {
		return top_offset;
	}
}

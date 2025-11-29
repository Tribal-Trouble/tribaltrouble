package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public final class SliderData {
	private final @NonNull Horizontal slider;
	private final @NonNull ModeIconQuads button;
	private final int left_offset;
	private final int right_offset;

	public SliderData(@NonNull Horizontal slider,
                      @NonNull ModeIconQuads button,
                      int left_offset,
                      int right_offset) {
		 this.slider = slider;
		 this.button = button;
		 this.left_offset = left_offset;
		 this.right_offset = right_offset;
	}

	public @NonNull Horizontal getSlider() {
		return slider;
	}

	public @NonNull ModeIconQuads getButton() {
		return button;
	}

	public int getLeftOffset() {
		return left_offset;
	}

	public int getRightOffset() {
		return right_offset;
	}
}

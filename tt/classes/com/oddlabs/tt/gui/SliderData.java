package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;

public final class SliderData {
	private final Horizontal slider;
	private final Quad[] button;
	private final int left_offset;
	private final int right_offset;

	public SliderData(Horizontal slider,
						 Quad[] button,
						 int left_offset,
						 int right_offset) {
		 this.slider = slider;
		 this.button = button;
		 this.left_offset = left_offset;
		 this.right_offset = right_offset;
	}

	public Horizontal getSlider() {
		return slider;
	}

	public Quad[] getButton() {
		return button;
	}

	public int getLeftOffset() {
		return left_offset;
	}

	public int getRightOffset() {
		return right_offset;
	}
}

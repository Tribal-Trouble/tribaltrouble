package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;

public final class Vertical {
	private final Quad @NonNull [] bottom;
	private final Quad[] center;
	private final Quad @NonNull [] top;
	private final int bottom_height;
	private final int top_height;
	private final int width;

	public Vertical(Quad @NonNull [] bottom, Quad[] center, Quad @NonNull [] top) {
		this.bottom = bottom;
		this.center = center;
		this.top = top;
		bottom_height = bottom[Skin.NORMAL].getHeight();
		top_height = top[Skin.NORMAL].getHeight();
		width = bottom[Skin.NORMAL].getWidth();
	}

	public void render(float x, float y, int height, int type) {
		int center_height = height - bottom_height - top_height;
		bottom[type].render(x, y);
		center[type].render(x, y + bottom_height, width, center_height);
		top[type].render(x, y + bottom_height + center_height);
	}

	public int getWidth() {
		return width;
	}

	public int getMinHeight() {
		return bottom[Skin.NORMAL].getHeight() + top[Skin.NORMAL].getHeight();
	}
}

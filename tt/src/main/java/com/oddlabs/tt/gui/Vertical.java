package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class Vertical {
	private final @NonNull ModeIconQuads bottom;
	private final @NonNull ModeIconQuads center;
	private final @NonNull ModeIconQuads top;
	private final int bottom_height;
	private final int top_height;
	private final int width;

	public Vertical(@NonNull ModeIconQuads bottom, @NonNull ModeIconQuads center, @NonNull ModeIconQuads top) {
		this.bottom = bottom;
		this.center = center;
		this.top = top;
		bottom_height = bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight();
		top_height = top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
		width = bottom.quad(ModeIconQuads.Mode.NORMAL).getWidth();
	}

	public void render(float x, float y, int height, ModeIconQuads.@NonNull Mode skinMode) {
		int center_height = height - bottom_height - top_height;

		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, bottom.quad(skinMode).getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		bottom.quad(skinMode).render(x, y);
		center.quad(skinMode).render(x, y + bottom_height, width, center_height);
		top.quad(skinMode).render(x, y + bottom_height + center_height);
		GL11.glEnd();
	}

	public int getWidth() {
		return width;
	}

	public int getMinHeight() {
		return bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight() + top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
	}
}

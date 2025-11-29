package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class Horizontal {
	private final @NonNull ModeIconQuads left;
	private final @NonNull ModeIconQuads center;
	private final @NonNull ModeIconQuads right;
	private final int height;
	private final int left_width;
	private final int right_width;

	public Horizontal(@NonNull ModeIconQuads left, @NonNull ModeIconQuads center, @NonNull ModeIconQuads right) {
		this.left = left;
		this.center = center;
		this.right = right;
		height = left.quad(ModeIconQuads.Mode.NORMAL).getHeight();
		left_width = left.quad(ModeIconQuads.Mode.NORMAL).getWidth();
		right_width = right.quad(ModeIconQuads.Mode.NORMAL).getWidth();
	}

	public void render(float x, float y, int width, ModeIconQuads.@NonNull Mode skinMode) {
		int center_width = width - left_width - right_width;

		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, left.quad(skinMode).getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		left.quad(skinMode).render(x, y);
		center.quad(skinMode).render(x + left_width, y, center_width, height);
		right.quad(skinMode).render(x + left_width + center_width, y);
		GL11.glEnd();
	}

	public int getHeight() {
		return height;
	}
}

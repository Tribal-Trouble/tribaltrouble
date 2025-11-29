package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class IconLabel extends GUIObject implements Comparable<IconLabel> {
	private final @NonNull IconQuad icon;
	private final @NonNull Label label;

	public IconLabel(@NonNull IconQuad icon, @NonNull Label label) {
		this.icon = icon;
		this.label = label;
		label.setPos(icon.getWidth(), 0);
		addChild(label);
		int width = icon.getWidth() + label.getWidth();
		int height = Math.max(icon.getHeight(), label.getHeight());
		setDim(width, height);
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, icon.getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		icon.render(0, 0);
		GL11.glEnd();
	}

	private @NonNull Label getLabel() {
		return label;
	}

	@Override
	public int compareTo(@NonNull IconLabel o) {
		return label.compareTo(o.getLabel());
	}
}

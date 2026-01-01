package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

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
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		renderer.drawIcon(icon, 0, 0);
	}

	private @NonNull Label getLabel() {
		return label;
	}

	@Override
	public int compareTo(@NonNull IconLabel o) {
		return label.compareTo(o.getLabel());
	}
}

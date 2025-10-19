package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;

public final class IconLabel extends GUIObject implements Comparable<IconLabel> {
	private final Quad icon;
	private final Label label;

	public IconLabel(Quad icon, Label label) {
		this.icon = icon;
		this.label = label;
		label.setPos(icon.getWidth(), 0);
		addChild(label);
		int width = icon.getWidth() + label.getWidth();
		int height = StrictMath.max(icon.getHeight(), label.getHeight());
		setDim(width, height);
	}

        @Override
	protected void renderGeometry() {
		icon.render(0, 0);
	}

	private Label getLabel() {
		return label;
	}

        @Override
	public int compareTo(IconLabel o) {
		return label.compareTo(o.getLabel());
	}
}

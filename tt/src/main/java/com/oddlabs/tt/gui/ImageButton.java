package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public class ImageButton extends ButtonObject {
	private final @NonNull GUIObject normal;
	private final GUIObject hovered;
	private final GUIObject disabled;

	public ImageButton(@NonNull GUIObject normal, GUIObject hovered, GUIObject disabled) {
		setDim(normal.getWidth(), normal.getHeight());
		this.normal = normal;
		this.hovered = hovered;
		this.disabled = disabled;
	}

        @Override
	public final void setPos(int x, int y) {
		super.setPos(x, y);
		normal.setPos(x, y);
		hovered.setPos(x, y);
		disabled.setPos(x, y);
	}

        @Override
	protected final void renderGeometry() {
		if (isDisabled())
			disabled.renderGeometry();
		else if (isHovered() || isActive())
			hovered.renderGeometry();
		else
			normal.renderGeometry();
	}

        @Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
	}
}

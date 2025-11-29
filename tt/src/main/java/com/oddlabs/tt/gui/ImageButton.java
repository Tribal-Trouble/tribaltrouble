package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public class ImageButton extends ButtonObject {
	private final @NonNull GUIObject normal;
	private final GUIObject hovered;
	private final GUIObject disabled;

	public ImageButton(@NonNull GUIObject normal, GUIObject hovered, GUIObject disabled) {
		super(Skin.getSkin().getEditFont());
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
	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        var render = isDisabled()
                ? disabled
                : isHovered() || isActive() ? hovered : normal;
        render.renderGeometry(clip_left, clip_right, clip_bottom, clip_top);
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
	}
}

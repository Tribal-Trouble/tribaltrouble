package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;

public class GUIIcon extends GUIObject {
	private final @NonNull Quad icon_quad;
	
	public GUIIcon(@NonNull Quad icon) {
		setDim(icon.getWidth(), icon.getHeight());
		setCanFocus(false);
		this.icon_quad = icon;
	}

	@Override
	public void renderGeometry() {
		icon_quad.render(0, 0);
	}
}

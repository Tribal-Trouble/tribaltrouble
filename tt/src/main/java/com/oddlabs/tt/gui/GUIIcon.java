package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public class GUIIcon extends GUIObject {
	private final @NonNull IconQuad icon;

    public GUIIcon(@NonNull IconQuad icon) {
        this.icon = icon;
		setDim(icon.getWidth(), icon.getHeight());
		setCanFocus(false);
	}

	@Override
	public void renderGeometry() {
		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, icon.getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		icon.render(0, 0);
		GL11.glEnd();
	}
}

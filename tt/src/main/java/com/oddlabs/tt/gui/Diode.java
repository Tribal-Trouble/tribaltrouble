package com.oddlabs.tt.gui;

import org.lwjgl.opengl.GL11;

public final class Diode extends GUIObject {
	private boolean lit;

	public Diode() {
        var normal = Skin.getSkin().getDiode().get(ModeIconQuads.Mode.NORMAL);
		setDim(normal.getWidth(), normal.getHeight());
		lit = false;
	}

	public void setLit(boolean lit) {
		this.lit = lit;
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		ModeIconQuads.Mode skinMode =  isDisabled()
            ? ModeIconQuads.Mode.DISABLED
            : lit
                ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, Skin.getSkin().getDiode().get(skinMode).getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		Skin.getSkin().getDiode().get(skinMode).render(0, 0);
		GL11.glEnd();
	}
}

package com.oddlabs.tt.delegate;


import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.RenderQueues;
import org.lwjgl.opengl.GL11;

public abstract class Delegate extends GUIObject {
	public Delegate() {
		setPos(0, 0);
		setCanFocus(true);
		setDim(LocalInput.getViewWidth(), LocalInput.getViewHeight());
	}

    @Override
	public void displayChangedNotify(int width, int height) {
		setDim(width, height);
	}

    @Override
	protected void doAdd() {
		super.doAdd();
		setFocus();
	}

	public void render3D(LandscapeRenderer renderer, RenderQueues render_queues) {
	}

	public void render2D() {
	}

        @Override
	protected void renderGeometry() {
	}

	public boolean keyboardBlocked() {
		return false;
	}

	protected final void renderBackgroundAlpha() {
		GL11.glEnd();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(0f, 0f, 0f, .3f);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3f(0, 0, 0f);
		GL11.glVertex3f(getWidth(), 0, 0f);
		GL11.glVertex3f(getWidth(), getHeight(), 0f);
		GL11.glVertex3f(0, getHeight(), 0f);
		GL11.glEnd();
		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBegin(GL11.GL_QUADS);
	}
}

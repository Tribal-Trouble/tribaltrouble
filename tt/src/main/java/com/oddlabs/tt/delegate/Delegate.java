package com.oddlabs.tt.delegate;


import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public abstract class Delegate extends GUIObject {
	Delegate() {
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

	public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues render_queues) {
	}

	public void render2D() {
	}

    public boolean keyboardBlocked() {
		return false;
	}

	final void renderBackgroundAlpha() {
		float[] color = Color.argb4f(Color.argbi(0f, 0f, 0f, .3f));
		GL11.glColor4f(color[0], color[1], color[2], color[3]);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(0, 0);
		GL11.glVertex2f(0, getHeight());
		GL11.glVertex2f(getWidth(), getHeight());
		GL11.glVertex2f(getWidth(), 0);
		GL11.glEnd();
	}
}

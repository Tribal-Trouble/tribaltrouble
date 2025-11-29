package com.oddlabs.tt.delegate;


import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

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

	public void render2D(@NonNull GUIRenderer renderer) {
	}

    public boolean keyboardBlocked() {
		return false;
	}

	final void renderBackgroundAlpha(@NonNull GUIRenderer renderer) {
		int color = Color.argbi(0f, 0f, 0f, .3f);
		renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), color);
	}
}

package com.oddlabs.tt.delegate;


import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.RenderQueues;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public abstract class Delegate extends GUIObject {
	private static final Vector4fc BACKGROUND_ALPHA = new Vector4f(0f, 0f, 0f, .3f);
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
        displayChanged(LocalInput.getViewWidth(), LocalInput.getViewHeight());
		setFocus();
	}

	public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues render_queues, @NonNull CameraState state,  @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
	}

	public void render2D(@NonNull GUIRenderer renderer) {
	}

    public boolean keyboardBlocked() {
		return false;
	}

	final void renderBackgroundAlpha(@NonNull GUIRenderer renderer) {
		renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), BACKGROUND_ALPHA);
	}
}

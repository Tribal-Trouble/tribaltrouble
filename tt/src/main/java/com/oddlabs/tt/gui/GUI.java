package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class GUI implements Animated {
	private final @NonNull GUIRenderer guiRenderer;
	private @NonNull GUIRoot current_root;
	private @Nullable Fade fade;
	private @Nullable UIRenderer renderer;

	public GUI() {
		this.guiRenderer = new GUIRenderer();
		this.current_root = createRoot();
	}

	public @NonNull GUIRoot newFade() {
		return newFade(null, null);
	}

	public @NonNull GUIRoot newFade(@Nullable Fadable fadable, @Nullable UIRenderer renderer) {
		GUIRoot gui_root = createRoot();
		newFade(fadable, gui_root, renderer);
		return gui_root;
	}

	public void newFade(@Nullable Fadable fadable, @NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
		fade = new Fade(fadable, gui_root, renderer);
		LocalEventQueue.getQueue().getManager().registerAnimation(this);
	}

	public @NonNull GUIRoot createRoot() {
		GUIRoot gui_root = new GUIRoot(this);
		gui_root.displayChanged();
		return gui_root;
	}

	@Override
	public void animate(float t) {
		if (fade != null) {
			fade.animate(this, t);
		}
	}

	void stopFade() {
        LocalEventQueue.getQueue().getManager().removeAnimation(this);
		fade = null;
	}

	@Override
	public void updateChecksum(@NonNull StateChecksum checksum) {
	}

	void switchRoot(@NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
		current_root.removeTree();
		current_root = gui_root;
		this.renderer = renderer;
	}

	public @NonNull GUIRoot getGUIRoot() {
		return current_root;
	}

    private void setupGUIView(int width, int height) {
        GL11.glViewport(0, 0, width, height);
        guiRenderer.beginFrame(width, height);
    }

    private void finishGUIView() {
        guiRenderer.endFrame();
    }

	@Nullable Fade getFade() {
		return fade;
	}

	public void render(@NonNull AmbientAudio ambient, @NonNull CameraState frustum_state) {
		boolean clear_color = renderer == null || renderer.clearColorBuffer();
        GL11.glClear(clear_color ? GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT : GL11.GL_DEPTH_BUFFER_BIT);
		if (renderer != null)
			renderer.render(ambient, frustum_state, current_root);
		renderGUI();
	}

	public void pickHover() {
		CameraState camera = getGUIRoot().getDelegate().getCamera().getState();
		GUIObject gui_hit = getGUIRoot().getCurrentGUIObject();
		if (renderer != null)
			renderer.pickHover(gui_hit.canHoverBehind(), camera, LocalInput.getMouseX(), LocalInput.getMouseY());
	}

    /** Renders the 2D GUI atop the 3D scene */
	private void renderGUI() {
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
		GLStateStack.pushState();
        GUIRoot guiRoot = getGUIRoot();
        setupGUIView(guiRoot.getWidth(), guiRoot.getHeight());
		try {
            guiRoot.render(guiRenderer);

            guiRoot.renderTopmost(guiRenderer, null != renderer ? renderer.getToolTip() : null, null != renderer && renderer.isCheater());
		} finally {
            finishGUIView();
			GLStateStack.popState();
			GL11.glPopClientAttrib();
			GL11.glPopAttrib();
		}
	}
}

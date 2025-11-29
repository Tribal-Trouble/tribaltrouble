package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public final class GUI implements Animated {
	private @NonNull GUIRoot current_root;
	private @Nullable Fade fade;
	private @Nullable UIRenderer renderer;

	public GUI() {
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
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glViewport(0, 0, width, height);
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

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL); // Added this
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GLState.activeTexture(GL13.GL_TEXTURE1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GLState.activeTexture(GL13.GL_TEXTURE0);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GUIRoot guiRoot = getGUIRoot();
        setupGUIView(guiRoot.getWidth(), guiRoot.getHeight());
		try {
            guiRoot.render();
            guiRoot.renderTopmost(null != renderer ? renderer.getToolTip() : null, null != renderer && renderer.isCheater());
		} finally {
			GLStateStack.popState();
			GL11.glPopClientAttrib();
			GL11.glPopAttrib();
		}
	}
}

package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.AmbientAudio;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class GUI implements Animated {
	private GUIRoot current_root = createRoot();
	private @Nullable Fade fade;
	private UIRenderer renderer;

	public GUI() {
	}

	public @NonNull GUIRoot newFade() {
		return newFade(null, null);
	}

	public @NonNull GUIRoot newFade(Fadable fadable, UIRenderer renderer) {
		GUIRoot gui_root = createRoot();
		newFade(fadable, gui_root, renderer);
		return gui_root;
	}

	public void newFade(Fadable fadable, GUIRoot gui_root, UIRenderer renderer) {
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
		fade.animate(this, t);
	}

	void stopFade() {
		fade = null;
		LocalEventQueue.getQueue().getManager().removeAnimation(this);
	}

	@Override
	public void updateChecksum(StateChecksum checksum) {
	}

	void switchRoot(GUIRoot gui_root, UIRenderer renderer) {
		if (current_root != null)
			current_root.removeTree();
		current_root = gui_root;
		this.renderer = renderer;
	}

	public GUIRoot getGUIRoot() {
		return current_root;
	}

	public void render(AmbientAudio ambient, CameraState frustum_state) {
		boolean clear_color = renderer != null ? renderer.clearColorBuffer() : true;
		if (clear_color)
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		else
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
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

	public void renderGUI() {
		current_root.setupGUIView();
		current_root.render();
		if (current_root.showToolTip()) {
			ToolTip tooltip = current_root.getToolTip();
			if (tooltip == null && renderer != null)
				tooltip = renderer.getToolTip();
			if (tooltip != null)
				current_root.renderToolTip(tooltip);
		}
		if (renderer != null)
			renderer.renderGUI(current_root);
		current_root.renderTopmost();
		if (fade != null)
			fade.render();
		GUIRoot.resetGUIView();
	}
}

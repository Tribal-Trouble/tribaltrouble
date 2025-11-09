package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

public final class QuitScreen extends CameraDelegate implements Updatable {
	private final static float DELAY = 5f;

	private final static int overlay_texture_width = 1024;
	private final static int overlay_texture_height = 1024;
	private final static int overlay_image_width = 800;
	private final static int overlay_image_height = 600;
	private final static String overlay_texture_name = "/textures/gui/quitscreen";

	private final @NonNull GUIImage overlay;
	private final TimerAnimation delay_timer = new TimerAnimation(this, DELAY);
	private boolean key_pressed = false;
	private boolean time_out = false;

	public QuitScreen(@NonNull GUIRoot gui_root, Camera camera) {
		super(gui_root, camera);
		setCanFocus(true);
		setFocusCycle(true);

		int screen_width = LocalInput.getViewWidth();
		int screen_height = LocalInput.getViewHeight();
		overlay = new GUIImage(screen_width, screen_height, 0f, 0f, (float)overlay_image_width/overlay_texture_width, (float)overlay_image_height/overlay_texture_height, overlay_texture_name);
		overlay.setPos(0, 0);
		addChild(overlay);

		GUIRoot quit_root = gui_root.getGUI().newFade();

		delay_timer.start();

		quit_root.pushDelegate(this);
	}

	@Override
	public void displayChangedNotify(int width, int height) {
		setDim(width, height);
		overlay.setDim(width, height);
	}

	@Override
	public void update(Object anim) {
		delay_timer.stop();
		time_out = true;
		quit();
	}

	private void quit() {
		if (key_pressed && time_out)
			Renderer.shutdown();
	}

	@Override
	protected void keyPressed(@NonNull KeyboardEvent event) {
		key_pressed = true;
		quit();
	}

	@Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
		key_pressed = true;
		quit();
	}
}

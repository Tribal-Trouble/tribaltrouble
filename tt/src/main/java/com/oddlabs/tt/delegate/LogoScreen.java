package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.gui.Fadable;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.UIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class LogoScreen extends CameraDelegate implements Updatable {
	private static final float DELAY = 4f;
	private static final int overlay_texture_width = 1024;
	private static final int overlay_texture_height = 1024;
	private static final int overlay_image_width = 800;
	private static final int overlay_image_height = 600;

	private final @Nullable GUIIcon overlay;
	private final TimerAnimation delay_timer = new TimerAnimation(this, DELAY);
	private final GUIRoot client_root;
	private final Fadable fadable;
	private final UIRenderer renderer;
	private boolean fade_started = false;
	
	public LogoScreen(@NonNull GUIRoot gui_root, @Nullable Texture logo, Fadable fadable, GUIRoot client_root, UIRenderer renderer) {
		super(gui_root, new StaticCamera(new CameraState()));
		this.client_root = client_root;
		this.fadable = fadable;
		this.renderer = renderer;
		setCanFocus(true);
		setFocusCycle(true);
		
		int screen_width = LocalInput.getViewWidth();
		int screen_height = LocalInput.getViewHeight();
		if (logo != null) { 
			overlay = new GUIIcon(new IconQuad(screen_width, screen_height, 0f, 0f, overlay_image_width/overlay_texture_width, overlay_image_height/overlay_texture_height, logo));
			overlay.setPos(0, 0);
			addChild(overlay);
		} else
			overlay = null;

		delay_timer.start();
		gui_root.pushDelegate(this);
	}

	@Override
	public void displayChangedNotify(int width, int height) {
		setDim(width, height);
		if (overlay != null)
			overlay.setDim(width, height);
	}

	@Override
	public void update(Object anim) {
		delay_timer.stop();
		fade();
	}

	public void fade() {
		if (!fade_started) {
			fade_started = true;
			getGUIRoot().getGUI().newFade(fadable, client_root, renderer);
		}
	}

	public void switchImage(GUIRoot gui_root) {
		pop();
	}

	public void fadingDone(GUIRoot gui_root) {
	}
	
	@Override
	protected void keyPressed(@NonNull KeyboardEvent event) {
		fade();
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		fade();
	}
}

package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

final class Fade {
	private static final float FADE_TIME = 1f;

	private final @Nullable Fadable fadable;
	private final @NonNull GUIRoot gui_root;
	private final @Nullable UIRenderer renderer;

	private float time = 0;
	private boolean image_switched = false;
	
	Fade(@Nullable Fadable fadable, @NonNull GUIRoot gui_root, @Nullable UIRenderer renderer) {
		this.fadable = fadable;
		this.gui_root = gui_root;
		this.renderer = renderer;
	}

	public void animate(@NonNull GUI gui, float t) {
		time += t;
		if (!image_switched && time >= FADE_TIME/2) {
			image_switched = true;
			gui.switchRoot(gui_root, renderer);
		}
		
		if (time >= FADE_TIME) {
			gui.stopFade();
			if (fadable != null)
				fadable.fadingDone();
		}
	}

	public void updateChecksum(StateChecksum checksum) {
	}

	void render() {
		float alpha = (float)Math.sin(Math.PI*time/FADE_TIME);
		float[] color = Color.argb4f(Color.argbi(0f, 0f, 0f, alpha));
		GL11.glColor4f(color[0], color[1], color[2], color[3]);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(0, 0);
		GL11.glVertex2f(0, LocalInput.getViewHeight());
		GL11.glVertex2f(LocalInput.getViewWidth(), LocalInput.getViewHeight());
		GL11.glVertex2f(LocalInput.getViewWidth(), 0);
		GL11.glEnd();
	}
}

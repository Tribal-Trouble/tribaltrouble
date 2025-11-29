package com.oddlabs.tt.font;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 *  Text insertion point.
 */
public final class Index implements Updatable {
	public static final int INDEX_WIDTH = 1;
	private static final float BLINK_INTERVAL = .5f;

	private static final Index index = new Index();

	private final TimerAnimation timer = new TimerAnimation(this, BLINK_INTERVAL);
	private boolean blink_on;

	private Index() {
		timer.start();
		blink_on = true;
	}

	public static void resetBlinking() {
		index.doResetBlinking();
	}

	private void doResetBlinking() {
		blink_on = true;
		timer.resetTime();
	}

	public static void renderIndex(int render_x, int render_y, @NonNull Font font) {
		index.doRenderIndex(render_x, render_y, font);
	}

	private void doRenderIndex(int render_x, int render_y, @NonNull Font font) {
		if (blink_on) {
			float[] c = Color.argb4f(Color.WHITE_INT);
			GL11.glColor4f(c[0], c[1], c[2], c[3]);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(render_x, render_y + 3);
			GL11.glVertex2f(render_x, render_y + font.getHeight() - 6);
			GL11.glVertex2f(render_x + INDEX_WIDTH, render_y + font.getHeight() - 6);
			GL11.glVertex2f(render_x + INDEX_WIDTH, render_y + 3);
			GL11.glEnd();
		}
	}

	@Override
	public void update(Object anim) {
		blink_on = !blink_on;
	}
}

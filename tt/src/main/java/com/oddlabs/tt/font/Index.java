package com.oddlabs.tt.font;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

/**
 *  Text insertion point.
 */
public final class Index implements Updatable<TimerAnimation> {
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

	public static void renderIndex(@NonNull GUIRenderer renderer, int render_x, int render_y, @NonNull Font font, @NonNull Vector4fc color) {
		index.doRenderIndex(renderer, render_x, render_y, font, color);
	}

	private void doRenderIndex(@NonNull GUIRenderer renderer, int render_x, int render_y, @NonNull Font font, @NonNull Vector4fc color) {
		if (blink_on) {
			renderer.drawColoredQuad(render_x, render_y + 3, INDEX_WIDTH, font.getHeight() - 6, color);
		}
	}

	@Override
	public void update(@NonNull TimerAnimation anim) {
		blink_on = !blink_on;
	}
}

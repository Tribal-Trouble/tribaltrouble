package com.oddlabs.tt.font;

import com.oddlabs.util.*;
import org.lwjgl.opengl.*;

import com.oddlabs.tt.animation.*;

public final strictfp class Index implements Updatable {
	public final static int INDEX_WIDTH = 1;
	private final static float BLINK_INTERVAL = .5f;
    private static Quad quad = new Quad(0f, 1f, 0f, 1f, 10, 10);

	private final static Index index = new Index();

	private final TimerAnimation timer;
	private boolean blink_on;

	private Index() {
		timer = new TimerAnimation(this, BLINK_INTERVAL);
		timer.start();
		blink_on = true;
	}

	public final static void resetBlinking() {
		index.doResetBlinking();
	}

	private final void doResetBlinking() {
		blink_on = true;
		timer.resetTime();
	}

	public final static void renderIndex(int render_x, int render_y, Font font) {
		index.doRenderIndex(render_x, render_y, font);
	}

	private final void doRenderIndex(int render_x, int render_y, Font font) {
		if (blink_on) {
            TrafoState.setColor(1f, 1f, 1f, 1f);
            GL33.glLineWidth(INDEX_WIDTH);
		    quad.render(render_x, render_y + 3, 1, font.getHeight() - 3);
		}
	}

	public final void update(Object anim) {
		blink_on = !blink_on;
	}
}

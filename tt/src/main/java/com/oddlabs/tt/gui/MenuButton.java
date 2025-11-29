package com.oddlabs.tt.gui;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class MenuButton extends ButtonObject {
	private static final float SECONDS_PER_HOVER_CYCLE = 1.5f;
	private static final float HOVER_SCALE_FACTOR = 0.06f;

	private final @NonNull CharSequence text;
	private final int color_normal;
	private final int color_active;
	
	private float start_hover_time;

	public MenuButton(@NonNull String caption, int color_normal, int color_active) {
		this(caption, Skin.getSkin().getHeadlineFont(), color_normal, color_active);
	}

	private MenuButton(@NonNull CharSequence text, @NonNull Font font, int color_normal, int color_active) {
		super(font);
		setDim(font.getWidth(text), font.getHeight());
		this.text = text;
		this.color_normal = color_normal;
		this.color_active = color_active;
	}

	private void scaleHovered(@NonNull GUIRenderer renderer) {
		float time = (LocalEventQueue.getQueue().getTime() - start_hover_time)%SECONDS_PER_HOVER_CYCLE;
		float cycle_position = time/SECONDS_PER_HOVER_CYCLE;
		float scale = 1f + HOVER_SCALE_FACTOR*(float)Math.sin(cycle_position*2*Math.PI);
		renderer.getMatrixStack().scale(scale, scale, 1f);
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		renderer.getMatrixStack().push()
                .translate(getWidth()/2f, getHeight()/2f, 0);
		int c;
		if (isActive()) {
			c = color_active;
			scaleHovered(renderer);
		} else if (isDisabled()) {
			c = Label.DISABLED_COLOR;
		} else {
			c = color_normal;
		}

		TextLineRenderer.render(renderer, getFont(), text, -getWidth()/2f, -getHeight()/2f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, c);
		renderer.getMatrixStack().pop();
	}

	@Override
	protected void mouseEntered() {
		if (!isActive()) {
			start_hover_time = LocalEventQueue.getQueue().getTime()%SECONDS_PER_HOVER_CYCLE;
			setFocus();
		}
	}
}

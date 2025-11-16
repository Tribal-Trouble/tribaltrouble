package com.oddlabs.tt.gui;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class MenuButton extends ButtonObject {
	private static final float SECONDS_PER_HOVER_CYCLE = 1.5f;
	private static final float HOVER_SCALE_FACTOR = 0.06f;

	private final @NonNull TextLineRenderer text_renderer;
	private final @NonNull CharSequence text;
	private final float[] color_normal;
	private final float[] color_active;
	
	private float start_hover_time;

	public MenuButton(@NonNull String caption, int color_normal, int color_active) {
		this(caption, Skin.getSkin().getHeadlineFont(), color_normal, color_active);
	}

	private MenuButton(@NonNull CharSequence text, @NonNull Font font, int color_normal, int color_active) {
		setDim(font.getWidth(text), font.getHeight());
		this.text = text;
		this.color_normal = Color.rgb3f(color_normal);
		this.color_active = Color.rgb3f(color_active);
		text_renderer = new TextLineRenderer(font);
	}

	private void scaleHovered() {
		float time = (LocalEventQueue.getQueue().getTime() - start_hover_time)%SECONDS_PER_HOVER_CYCLE;
		float cycle_position = time/SECONDS_PER_HOVER_CYCLE;
		float scale = 1f + HOVER_SCALE_FACTOR*(float)Math.sin(cycle_position*2*Math.PI);
		GL11.glScalef(scale, scale, 1f);
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		GL11.glEnd();
		GL11.glPushMatrix();
		GL11.glTranslatef(getWidth()/2, getHeight()/2, 0);
		clip_left -= getWidth()/2;
		clip_right -= getWidth()/2;
		clip_top -= getHeight()/2;
		clip_bottom -= getHeight()/2;
		if (isActive()) {
			GL11.glColor3f(color_active[0], color_active[1], color_active[2]);
			scaleHovered();
		} else if (isDisabled()) {
			GL11.glColor4f(Label.DISABLED_COLOR[0], Label.DISABLED_COLOR[1], Label.DISABLED_COLOR[2], Label.DISABLED_COLOR[3]);
		} else {
			GL11.glColor3f(color_normal[0], color_normal[1], color_normal[2]);
		}
		GL11.glBegin(GL11.GL_QUADS);

		text_renderer.render(-getWidth()/2, -getHeight()/2, clip_left, clip_right, clip_bottom, clip_top, text);
		GL11.glEnd();
		GL11.glPopMatrix();
		GL11.glColor3f(1f, 1f, 1f);
		GL11.glBegin(GL11.GL_QUADS);
	}

	@Override
	protected void mouseEntered() {
		if (!isActive()) {
			start_hover_time = LocalEventQueue.getQueue().getTime()%SECONDS_PER_HOVER_CYCLE;
			setFocus();
		}
	}
}

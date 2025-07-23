package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.*;
import org.lwjgl.opengl.*;

public strictfp class Label extends TextField implements Comparable {
	public final static int ALIGN_LEFT		= 0;
	public final static int ALIGN_CENTER	= 1;
	public final static int ALIGN_RIGHT		= 2;
	public final static float[] DEFAULT_COLOR = new float[]{1f, 1f, 1f, 1f};
	public final static float[] DISABLED_COLOR = new float[]{.7f, .7f, .7f, .7f};

	private final int align;
	private final TextLineRenderer text_renderer;

	private float[] color = DEFAULT_COLOR;

	public Label(CharSequence text, Font font) {
		this(text, font, font.getWidth(text), ALIGN_LEFT);
	}

	public Label(CharSequence text, Font font, int width) {
		this(text, font, width, ALIGN_LEFT);
	}

	public Label(CharSequence text, Font font, int width, int align) {
		super(text, font, Integer.MAX_VALUE);
		this.align = align;
		text_renderer = new TextLineRenderer(font);
		setDim(width, font.getHeight());
	}

	public final void setColor(float[] color) {
		this.color = color;
	}

	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		if (isDisabled()) {
			TrafoState.setColor(DISABLED_COLOR[0], DISABLED_COLOR[1], DISABLED_COLOR[2], DISABLED_COLOR[3]);
		} else {
			TrafoState.setColor(color[0], color[1], color[2], color[3]);
		}
		if (align == ALIGN_LEFT) {
			text_renderer.renderCropped(0, 0, clip_left, clip_right, clip_bottom, clip_top, getText());
		} else if (align == ALIGN_CENTER) {
			text_renderer.render(0, 0, (getWidth() - getFont().getWidth(getText()))/2, clip_left, clip_right, clip_bottom, clip_top, getText(), -1);
		} else if (align == ALIGN_RIGHT) {
			text_renderer.render(0, 0, getWidth() - getFont().getWidth(getText()), clip_left, clip_right, clip_bottom, clip_top, getText(), -1);
		}
		TrafoState.setColor(1f, 1f, 1f, 1f);
	}

	public int compareTo(Object o) {
		return getText().toString().compareToIgnoreCase(((Label)o).getText().toString());
	}
}


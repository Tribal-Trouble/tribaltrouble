package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextBoxRenderer;
import com.oddlabs.util.*;
import org.lwjgl.opengl.*;

public strictfp class LabelBox extends TextField implements Comparable {
	private final TextBoxRenderer text_renderer;

	private float[] color = new float[]{1f, 1f, 1f, 1f};

	public LabelBox(CharSequence text, Font font, int width) {
		super(text, font, Integer.MAX_VALUE);
		text_renderer = new TextBoxRenderer(font, width, LocalInput.getViewHeight());
		setDim(width, text_renderer.getTextHeight(text));
	}

	public final void setDim(int width, int height) {
		super.setDim(width, height);
		text_renderer.setDim(width, height);
	}

	public final void setColor(float[] color) {
		this.color = color;
	}

	protected void renderGeometry() {
		if (isDisabled()) {
			TrafoState.setColor(Label.DISABLED_COLOR[0], Label.DISABLED_COLOR[1], Label.DISABLED_COLOR[2], Label.DISABLED_COLOR[3]);
		} else {
			TrafoState.setColor(color[0], color[1], color[2], color[3]);
		}
		text_renderer.render(0, 0, 0, getText());
		TrafoState.setColor(1f, 1f, 1f, 1f);
	}

	public int compareTo(Object o) {
		return getText().toString().compareToIgnoreCase(((LabelBox)o).getText().toString());
	}
}


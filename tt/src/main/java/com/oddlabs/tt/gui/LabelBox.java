package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextBoxRenderer;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public class LabelBox extends TextField implements Comparable<LabelBox> {
	private final @NonNull TextBoxRenderer text_renderer;

	private float[] color = com.oddlabs.util.Color.argb4f(0xFF_FF_FF_FF);

	public LabelBox(@NonNull CharSequence text, Font font, int width) {
		super(text, font, Integer.MAX_VALUE);
		text_renderer = new TextBoxRenderer(font, width, LocalInput.getViewHeight());
		setDim(width, text_renderer.getTextHeight(text));
	}

	@Override
	public final void setDim(int width, int height) {
		super.setDim(width, height);
		text_renderer.setDim(width, height);
	}

	public final void setColor(float[] color) {
		this.color = color;
	}

	@Override
	protected void renderGeometry() {
		// Radeon 9200 problem
		GL11.glEnd();
		if (isDisabled()) {
			GL11.glColor4f(Label.DISABLED_COLOR[0], Label.DISABLED_COLOR[1], Label.DISABLED_COLOR[2], Label.DISABLED_COLOR[3]);
		} else {
			GL11.glColor4f(color[0], color[1], color[2], color[3]);
		}
		GL11.glBegin(GL11.GL_QUADS);
		text_renderer.render(0, 0, 0, getText());
		GL11.glEnd();
		GL11.glColor3f(1f, 1f, 1f);
		GL11.glBegin(GL11.GL_QUADS);
	}

	@Override
	public int compareTo(@NonNull LabelBox o) {
		return getText().toString().compareToIgnoreCase(o.getText().toString());
	}
}


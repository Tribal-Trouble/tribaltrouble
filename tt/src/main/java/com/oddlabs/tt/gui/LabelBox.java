package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLayout;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

public class LabelBox extends TextField implements Comparable<LabelBox>, Clipped {
	private @NonNull TextLayout textLayout;

	private int color = Color.WHITE_INT;

	public LabelBox(@NonNull CharSequence text, @NonNull Font font, int width) {
		super(text, font, Integer.MAX_VALUE);
		textLayout = new TextLayout(font, text, width);
		setDim(width, textLayout.getTextHeight());
    }

	private void updateLayout() {
		textLayout = new TextLayout(getFont(), getText(), getWidth());
		setDim(getWidth(), textLayout.getTextHeight());
	}

	@Override
	public void setText(@NonNull CharSequence text) {
		super.setText(text);
		updateLayout();
	}

	@Override
	public final void setDim(int width, int height) {
		super.setDim(width, height);
	}

	public final void setColor(int color) {
		this.color = color;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		int c = isDisabled() ? Label.DISABLED_COLOR : color;
		TextLineRenderer.render(renderer, textLayout, 0, getHeight() - getFont().getHeight(), c);
	}

	@Override
	public int compareTo(@NonNull LabelBox o) {
		return getText().toString().compareToIgnoreCase(o.getText().toString());
	}
}

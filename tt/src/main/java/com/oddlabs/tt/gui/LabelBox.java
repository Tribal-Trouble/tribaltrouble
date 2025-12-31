package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLayout;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public class LabelBox extends TextField implements Comparable<LabelBox>, Clipped {
	private @NonNull TextLayout textLayout;

	private @NonNull Vector4fc color = Color.WHITE;

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
	public LabelBox setText(@NonNull CharSequence text) {
		super.setText(text);
		updateLayout();
		return this;
	}

	@Override
	public final LabelBox setDim(int width, int height) {
		super.setDim(width, height);
		return this;
	}

	public final LabelBox setColor(@NonNull Vector4fc color) {
		this.color = color;
		return this;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		var c = isDisabled() ? Label.DISABLED_COLOR : color;
		TextLineRenderer.render(renderer, textLayout, 0, getHeight() - getFont().getHeight(), c);
	}

	@Override
	public int compareTo(@NonNull LabelBox o) {
		return getText().toString().compareToIgnoreCase(o.getText().toString());
	}
}

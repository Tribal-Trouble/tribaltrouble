package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public class Label extends TextField implements Comparable<Label> {
	public static final int DEFAULT_COLOR = Color.WHITE_INT;
	public static final int DISABLED_COLOR = 0xB2B2B2B2;

	private final @NonNull Origin align;

	private int color = DEFAULT_COLOR;

	public Label(@NonNull CharSequence text, @NonNull Font font) {
		this(text, font, font.getWidth(text), Origin.AT_START);
	}

	public Label(@NonNull CharSequence text, @NonNull Font font, int width) {
		this(text, font, width, Origin.AT_START);
	}

	public Label(@NonNull CharSequence text, @NonNull Font font, int width, @NonNull Origin align) {
		super(text, font, Integer.MAX_VALUE);
		this.align = align;
		setDim(width, font.getHeight());
	}

	public final void setColor(int color) {
		this.color = color;
	}

	@Override
	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		int c = isDisabled() ? DISABLED_COLOR : color;
		int textWidth = getFont().getWidth(getText());
		int x = switch (align) {
			case AT_START -> 0;
			case AT_MIDDLE -> (getWidth() - Math.min(getWidth(), textWidth)) / 2;
			case AT_END -> getWidth() - textWidth;
		};
		TextLineRenderer.render(getFont(), getText(), x, 0, clip_left, clip_right, c);
		GL11.glColor4f(1f, 1f, 1f, 1f);
	}

	@Override
	public int compareTo(@NonNull Label o) {
		return getText().toString().compareToIgnoreCase(o.getText().toString());
	}
}

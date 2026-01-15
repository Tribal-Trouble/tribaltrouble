package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public class Label extends TextField implements Comparable<Label> {
	public static final Vector4fc DEFAULT_COLOR = Color.WHITE;
	public static final Vector4fc DISABLED_COLOR = Color.argb4v(0xB2_B2_B2_B2);
	private static final int INSET = 2;

	private final @NonNull Origin align;

	private final Vector4f color = new Vector4f(DEFAULT_COLOR);

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

	public final @NonNull Label setColor(@NonNull Vector4fc color) {
		this.color.set(color);
		return this;
	}
    
    public @NonNull Vector4fc getColor() {
        return color;
    }

	@Override
	protected final void renderGeometry(@NonNull GUIRenderer renderer) {
		var c = isDisabled() ? DISABLED_COLOR : color;
		int textWidth = getFont().getWidth(getText());
		int x = switch (align) {
			case AT_START -> 0;
			case AT_MIDDLE -> (getWidth() - Math.min(getWidth(), textWidth)) / 2;
			case AT_END -> getWidth() - textWidth - INSET;
		};
		TextLineRenderer.render(renderer, getFont(), getText(), x, 0, 0, getWidth() - INSET, c);
	}

	@Override
	public int compareTo(@NonNull Label o) {
		return getText().toString().compareToIgnoreCase(o.getText().toString());
	}
}

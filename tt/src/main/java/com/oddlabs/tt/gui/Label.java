package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/** A GUI object that is used to provide a text label another GUI object. */
public class Label extends TextField implements Comparable<Label> {
    public static final Color.Linear DEFAULT_COLOR = Color.Linear.WHITE;
    private static final int INSET = 2;

    private final @NonNull Origin align;

    private Color.@NonNull Linear color = DEFAULT_COLOR;

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

    public final @NonNull Label setColor(@NonNull Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
        return this;
    }

    public @NonNull Color getColor() {
        return color;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        var c = isDisabled() ? color.desaturate(0.3f).mul(0.5f).alpha(color.a() * 0.8f) : color;
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

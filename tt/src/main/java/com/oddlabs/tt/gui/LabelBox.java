package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLayout;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/** A GUI Label that provides a text label drawn in a box. */
public class LabelBox extends TextField implements Comparable<LabelBox>, Clipped {
    private @NonNull TextLayout textLayout;

    private Color.@NonNull Linear color = Color.Linear.WHITE;

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
    public @NonNull LabelBox setText(@NonNull CharSequence text) {
        super.setText(text);
        updateLayout();
        return this;
    }

    @Override
    public final @NonNull LabelBox setDim(int width, int height) {
        super.setDim(width, height);
        return this;
    }

    public final @NonNull LabelBox setColor(@NonNull Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
        return this;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        var c = isDisabled() ? color.desaturate(0.3f).mul(0.5f).alpha(color.a() * 0.8f) : color;
        TextLineRenderer.render(renderer, textLayout, 0, getHeight() - getFont().getHeight(), c);
    }

    @Override
    public int compareTo(@NonNull LabelBox o) {
        return getText().toString().compareToIgnoreCase(o.getText().toString());
    }
}

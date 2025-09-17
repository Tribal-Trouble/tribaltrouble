package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextBoxRenderer;

import org.lwjgl.opengl.GL11;

/**
 * Simple multi-line label with word wrapping based on TextBoxRenderer.
 */
public final class MultiLineLabel extends TextField {
    private final TextBoxRenderer textRenderer;
    private final int minHeight;
    private float[] color = Label.DEFAULT_COLOR;

    public MultiLineLabel(CharSequence text, Font font, int width) {
        this(text, font, width, font.getHeight());
    }

    public MultiLineLabel(CharSequence text, Font font, int width, int minHeight) {
        super(text, font, Integer.MAX_VALUE);
        this.minHeight = StrictMath.max(minHeight, font.getHeight());
        textRenderer = new TextBoxRenderer(font, width, this.minHeight);
        setDim(width, this.minHeight);
        autoSize();
    }

    public void setColor(float[] color) {
        this.color = color;
    }

    @Override
    public void setDim(int width, int height) {
        super.setDim(width, height);
        textRenderer.setDim(width, height);
    }

    // Use appendNotify hook instead of overriding final set() and individual append() methods.
    @Override
    protected void appendNotify(CharSequence str) {
        autoSize();
    }

    @Override
    public void clear() {
        super.clear();
        autoSize();
    }

    private void autoSize() {
        int width = textRenderer.getWidth();
        textRenderer.setDim(width, StrictMath.max(minHeight, getFont().getHeight()));
        int textHeight = textRenderer.getTotalTextHeight(getText());
        if (textHeight < minHeight) textHeight = minHeight;
        super.setDim(width, textHeight);
        textRenderer.setDim(width, textHeight);
    }

    @Override
    protected void renderGeometry(float clipLeft, float clipRight, float clipBottom, float clipTop) {
        GL11.glEnd();
        float[] rgba = isDisabled() ? Label.DISABLED_COLOR : color;
        GL11.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        GL11.glBegin(GL11.GL_QUADS);
        textRenderer.render(0, 0, getText());
        GL11.glEnd();
        GL11.glColor3f(1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
    }
}


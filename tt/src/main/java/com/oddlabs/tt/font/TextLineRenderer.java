package com.oddlabs.tt.font;

import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Quad;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class TextLineRenderer {

    private TextLineRenderer() {
        // private constructor for utility class
    }

    public static void render(@NonNull GUIRenderer renderer, @NonNull TextLayout layout, float x, float y, @NonNull Vector4fc color) {
        render(renderer, layout, x, y, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, color);
    }

    public static void render(@NonNull GUIRenderer renderer, @NonNull TextLayout layout, float x, float y, float clipLeft, float clipRight, @NonNull Vector4fc color) {
        float currentY = y;
        for (TextLayout.Line line : layout.getLines()) {
            render(renderer, layout.getFont(), line.content(), x, currentY, clipLeft, clipRight, color);
            currentY -= layout.getFont().getHeight();
        }
    }

    /**
     * Render a single line of text with the provided renderer using the provided font, location and color. The text will be clipped to the specified left and right bounds.
     */
    public static float render(@NonNull GUIRenderer renderer, @NonNull Font font, @NonNull CharSequence text,
                               float x, float y, float clipLeft, float clipRight,
                               @NonNull Vector4fc color) {
        return (float) text.codePoints().asDoubleStream().reduce(x, (currentX, codePointAsDouble) -> {
            int codePoint = (int) codePointAsDouble;

            if (codePoint == '\n') {
                // This renderer doesn't handle newlines, that's the layout's job
                return currentX;
            }

            Quad quad = font.getQuad(codePoint);
            if (quad != null) {
                float quadWidth = quad.getWidth();
                float charAdvance = quadWidth - font.getXBorder();

                // Check if the character is completely outside the clipping region
                if (currentX + quadWidth < clipLeft || currentX > clipRight) {
                    return currentX + charAdvance;
                }

                // By this point, we know at least part of the character is visible.
                // We need to calculate the visible portion and adjust texture coordinates.

                float renderX = (float) currentX;
                float renderWidth = quadWidth;
                float u1 = quad.getU1();
                float u2 = quad.getU2();
                float textureUWidth = u2 - u1;

                // Handle left clipping
                if (renderX < clipLeft) {
                    float leftClipPixels = clipLeft - renderX;
                    float leftClipRatio = leftClipPixels / quadWidth;
                    u1 += textureUWidth * leftClipRatio;
                    renderWidth -= leftClipPixels;
                    renderX = clipLeft;
                }

                // Handle right clipping
                if (renderX + renderWidth > clipRight) {
                    float rightClipPixels = (renderX + renderWidth) - clipRight;
                    float rightClipRatio = rightClipPixels / quadWidth;
                    u2 -= textureUWidth * rightClipRatio;
                    renderWidth -= rightClipPixels;
                }

                if (renderWidth > 0) {
                    renderer.drawTexture(font.getTexture(), renderX, y, renderWidth, quad.getHeight(), u1, quad.getV1(), u2, quad.getV2(), color);
                }
                return currentX + charAdvance;
            }
            return currentX;
        });
    }
}

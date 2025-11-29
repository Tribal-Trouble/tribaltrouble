package com.oddlabs.tt.font;

import com.oddlabs.util.Color;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class TextLineRenderer {

    private TextLineRenderer() {
        // no instances
    }

    public static void render(@NonNull TextLayout layout, float x, float y, int color) {
        float currentY = y;
        for (TextLayout.Line line : layout.getLines()) {
            render(layout.getFont(), line.content(), x, currentY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, color);
            currentY -= layout.getFont().getHeight();
        }
    }

    public static float render(@NonNull Font font, @NonNull CharSequence text, float x, float y, float clipLeft, float clipRight, int color) {
        float[] c = Color.argb4f(color);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, font.getTexture().getHandle());
        GL11.glBegin(GL11.GL_QUADS);

        float finalX = (float) text.codePoints().asDoubleStream().reduce(x, (currentX, codePointAsDouble) -> {
            int codePoint = (int) codePointAsDouble;

            if (codePoint == '\n') {
                return currentX;
            }

            Quad quad = font.getQuad(codePoint);
            if (quad != null) {
                float quadWidth = quad.getWidth();
                float charAdvance = quadWidth - font.getXBorder();

                if (currentX + quadWidth >= clipLeft && currentX <= clipRight) {
                    float renderX = (float) currentX;
                    float renderWidth = quadWidth;
                    float u1 = quad.getU1();
                    float u2 = quad.getU2();
                    float textureUWidth = u2 - u1;

                    if (renderX < clipLeft) {
                        float leftClipPixels = clipLeft - renderX;
                        u1 += textureUWidth * (leftClipPixels / quadWidth);
                        renderWidth -= leftClipPixels;
                        renderX = clipLeft;
                    }

                    if (renderX + renderWidth > clipRight) {
                        float rightClipPixels = (renderX + renderWidth) - clipRight;
                        u2 -= textureUWidth * (rightClipPixels / quadWidth);
                        renderWidth -= rightClipPixels;
                    }

                    if (renderWidth > 0) {
						GL11.glTexCoord2f(u1, quad.getV1());
						GL11.glVertex2f(renderX, y);
						GL11.glTexCoord2f(u1, quad.getV2());
						GL11.glVertex2f(renderX, y + quad.getHeight());
						GL11.glTexCoord2f(u2, quad.getV2());
						GL11.glVertex2f(renderX + renderWidth, y + quad.getHeight());
						GL11.glTexCoord2f(u2, quad.getV1());
						GL11.glVertex2f(renderX + renderWidth, y);
                    }
                }
                return currentX + charAdvance;
            }
            return currentX;
        });

        GL11.glEnd();
        return finalX;
    }
}

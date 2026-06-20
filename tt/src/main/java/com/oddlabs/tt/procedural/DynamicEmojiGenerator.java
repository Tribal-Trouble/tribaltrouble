package com.oddlabs.tt.procedural;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates an OpenGL texture for a single emoji codepoint string using the platform colour-emoji font.
 * Uses AWT rendering on a background thread to avoid the macOS AppKit/LWJGL main-thread conflict.
 */
public final class DynamicEmojiGenerator extends TextureGenerator {
    private static final Logger logger = Logger.getLogger(DynamicEmojiGenerator.class.getName());
    private static final int TEXT_SIZE = 48;
    private static final int TEXTURE_SIZE = 64;
    private static final List<String> EMOJI_FONT_NAMES = List.of("Apple Color Emoji", "Segoe UI Emoji",
            "Noto Color Emoji", "Google Sans", "Arial");
    private static final List<@NonNull Font> EMOJI_FONTS = EMOJI_FONT_NAMES.stream()
            .map(name -> new Font(name, Font.PLAIN, TEXT_SIZE))
            .toList();
    private static final @NonNull Font FALLBACK_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, TEXT_SIZE);

    private final int codepoint;
    private final Thread worker;
    private final @NonNull GLIntImage glIntImage;

    public DynamicEmojiGenerator(int codepoint) {
        this.codepoint = codepoint;
        this.glIntImage = new GLIntImage(TEXTURE_SIZE, TEXTURE_SIZE, GL11.GL_RGBA);
        this.worker = Thread.startVirtualThread(() -> renderEmoji(new String(Character.toChars(codepoint)),
                glIntImage));
    }

    public DynamicEmojiGenerator(@NonNull CharSequence sequence) {
        assert Character.codePointCount(sequence, 0, sequence.length()) == 1;
        this(Character.codePointAt(sequence, 0));
    }

    private static void renderEmoji(@NonNull String emoji, @NonNull GLIntImage glIntImage) {
        BufferedImage bufferedImage = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            Font selectedFont = selectEmojiFont(emoji);

            g2d.setFont(selectedFont);
            java.awt.FontMetrics metrics = g2d.getFontMetrics(selectedFont);
            int x = (TEXTURE_SIZE - metrics.stringWidth(emoji)) / 2;
            int y = ((TEXTURE_SIZE - metrics.getHeight()) / 2) + metrics.getAscent();

            // Draw a soft black border for contrast
            g2d.setColor(new java.awt.Color(0, 0, 0, 160));
            g2d.drawString(emoji, x + 1, y + 1);
            g2d.drawString(emoji, x - 1, y - 1);
            g2d.drawString(emoji, x + 1, y - 1);
            g2d.drawString(emoji, x - 1, y + 1);

            g2d.setColor(java.awt.Color.WHITE);
            g2d.drawString(emoji, x, y);
        } finally {
            g2d.dispose();
        }

        for (int py = 0; py < TEXTURE_SIZE; py++) {
            for (int px = 0; px < TEXTURE_SIZE; px++) {
                // flip y-axis
                int argb = bufferedImage.getRGB(px, TEXTURE_SIZE - 1 - py);
                glIntImage.putPixel(px, py, argb);
            }
        }
    }

    private static final @NonNull Font selectEmojiFont(@NonNull String emoji) {
        for (int i = 0; i < EMOJI_FONTS.size(); i++) {
            Font font = EMOJI_FONTS.get(i);
            if (font.canDisplayUpTo(emoji) == -1) {
                return font;
            }
        }
        return FALLBACK_FONT;
    }

    @Override
    protected @NonNull Texture @NonNull [] generate() {
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Use uncompressed RGBA8 to avoid driver issues with small dynamic textures
        Texture texture = new Texture(glIntImage, GL11.GL_RGBA8,
                GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        return new Texture[]{texture};
    }

    @Override
    public int hashCode() {
        return codepoint;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || (o instanceof DynamicEmojiGenerator deg && codepoint == deg.codepoint);
    }
}

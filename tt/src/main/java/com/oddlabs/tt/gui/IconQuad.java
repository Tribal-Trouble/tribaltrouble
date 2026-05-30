package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * A quadrilateral region of a texture, typically defined by pixel
 * coordinates. This is useful for defining icons from a texture atlas.
 */
public final class IconQuad extends Quad {
    private final @NonNull Texture atlas;

    /**
     * Creates an IconQuad from explicit normalized texture coordinates.
     *
     * @param u1      the starting U coordinate
     * @param v1      the starting V coordinate
     * @param u2      the ending U coordinate
     * @param v2      the ending V coordinate
     * @param width   the width of the quad in pixels
     * @param height  the height of the quad in pixels
     * @param texture the source texture
     * @throws IllegalArgumentException if width or height are negative, if texture coordinates are not finite, or if
     *                                  u1/v1 are greater than u2/v2.
     * @throws NullPointerException     if the texture is null
     */
    public IconQuad(float u1, float v1, float u2, float v2, int width, int height,
            @NonNull Texture texture) throws IllegalArgumentException, NullPointerException {
        if (!Float.isFinite(u1) || !Float.isFinite(v1) || !Float.isFinite(u2) || !Float.isFinite(v2)) {
            throw new IllegalArgumentException("Texture coordinates must be finite numbers.");
        }
        if (u1 > u2 || v1 > v2) {
            throw new IllegalArgumentException(
                    "u1/v1 must be less than or equal to u2/v2, but got: u1=" + u1 + ", v1=" + v1 + ", u2=" + u2
                            + ", v2=" + v2);
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Width and height must be non-negative. Got: width=" + width + ", height=" + height);
        }
        super(u1, v1, u2, v2, width, height);
        this.atlas = texture;
    }

    /**
     * Creates an IconQuad from pixel coordinates within a texture.
     *
     * @param x       the x-offset in pixels from the top-left of the texture
     * @param y       the y-offset in pixels from the top-left of the texture
     * @param width   the width of the icon in pixels
     * @param height  the height of the icon in pixels
     * @param texture the source texture atlas
     * @throws IndexOutOfBoundsException if the defined area is outside the texture bounds
     * @throws NullPointerException      if the texture is null
     */
    public IconQuad(int x, int y, int width, int height,
            @NonNull Texture texture) throws IndexOutOfBoundsException, NullPointerException {
        this((float) x / getCheckedTexture(x, y, width, height,
                texture).getWidth(), (float) y / texture.getHeight(), (float) (x + width) / texture.getWidth(),
                (float) (y + height) / texture.getHeight(), width, height, texture);
    }

    private static @NonNull Texture getCheckedTexture(int x, int y, int width, int height, @NonNull Texture texture) {
        Objects.requireNonNull(texture, "Texture cannot be null");
        Objects.checkFromIndexSize(x, width, texture.getWidth());
        Objects.checkFromIndexSize(y, height, texture.getHeight());
        return texture;
    }

    public @NonNull Texture getTexture() {
        return atlas;
    }
}

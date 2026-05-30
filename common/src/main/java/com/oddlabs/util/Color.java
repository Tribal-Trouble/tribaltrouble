package com.oddlabs.util;

import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

/**
 * Converts integer colors to/from floating-point representations for OpenGL.
 */
public final class Color {
    public static final int TRANSPARENT_INT = 0x00_00_00_00;
    public static final int BLACK_INT = 0xFF_00_00_00;
    public static final int WHITE_INT = 0xFF_FF_FF_FF;
    public static final int RED_INT = 0xFF_FF_00_00;
    public static final int GREEN_INT = 0xFF_00_FF_00;
    public static final int BLUE_INT = 0xFF_00_00_FF;
    public static final int YELLOW_INT = 0xFF_FF_FF_00;
    public static final int MAGENTA_INT = 0xFF_FF_00_FF;

    public static final Vector4fc BLACK = argb4v(BLACK_INT);
    public static final Vector4fc WHITE = argb4v(WHITE_INT);
    public static final Vector4fc TRANSPARENT = argb4v(TRANSPARENT_INT);

    /**
     * The normalization factor for converting 8-bit color components to/from floats.
     */
    private static final float NORMALIZE_8_BIT = 255.0f;

    private Color() {
        // no instances
    }

    /**
     * Converts a {@code Vector4fc} color to a 32-bit packed integer in AARRGGBB format
     *
     * @return A 32-bit packed integer in AARRGGBB format
     */
    public static int argbi(@NonNull Vector4fc color) {
        return argbi(color.w(), color.x(), color.y(), color.z());
    }

    /**
     * Converts float colors to a 32-bit packed integer in AARRGGBB format
     *
     * @return A 32-bit packed integer in AARRGGBB format
     */
    public static int argbi(float a, float r, float g, float b) {
        return argbi((byte) (a * NORMALIZE_8_BIT),
                (byte) (r * NORMALIZE_8_BIT),
                (byte) (g * NORMALIZE_8_BIT),
                (byte) (b * NORMALIZE_8_BIT));
    }

    /**
     * Converts byte colors to a 32-bit packed integer in AARRGGBB format
     *
     * @return A 32-bit packed integer in AARRGGBB format
     */
    public static int argbi(byte a, byte r, byte g, byte b) {
        return (Byte.toUnsignedInt(a) << 24) | (Byte.toUnsignedInt(r) << 16) | (Byte.toUnsignedInt(
                g) << 8) | Byte.toUnsignedInt(b);
    }

    /**
     * Converts a {@code Vector4fc} color to packed abgr for storing little endian byte buffer
     *
     * @param color The 32-bit float color vector
     * @return A 32-bit packed integer in AABBGGRR format
     */
    public static int abgri(@NonNull Vector4fc color) {
        return abgri(color.w(), color.z(), color.y(), color.x());
    }

    public static int abgri(float a, float b, float g, float r) {
        return ((int) (a * NORMALIZE_8_BIT) << 24) | ((int) (b * NORMALIZE_8_BIT) << 16) | ((int) (g * NORMALIZE_8_BIT)
                << 8) | ((int) (r * NORMALIZE_8_BIT));
    }

    /**
     * Converts a 32-bit ARGB integer (0xAARRGGBB) to a {@link Vector4fc}.
     * The output vector components are in (r, g, b, a) order as expected by OpenGL.
     *
     * @param color The 32-bit ARGB integer color.
     * @return A new {@link Vector4f} with components (r, g, b, a), normalized to [0.0, 1.0].
     */
    public static @NonNull Vector4f argb4v(int color) {
        return new Vector4f(
                ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
                (color & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 24) & 0xFF) / NORMALIZE_8_BIT);
    }

    /**
     * Converts a 32-bit RGBA integer (0xRRGGBBAA) to a {@link Vector4fc}.
     *
     * @param color The 32-bit RGBA integer color.
     * @return A new {@link Vector4f} with components (r, g, b, a), normalized to [0.0, 1.0].
     */
    public static @NonNull Vector4f rgba4v(int color) {
        return new Vector4f(
                ((color >> 24) & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
                (color & 0xFF) / NORMALIZE_8_BIT);
    }
}

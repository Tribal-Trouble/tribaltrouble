package com.oddlabs.util;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

/** Converts integer colors to floating-point representations for OpenGL. */
public final class Color {
    /** The normalization factor for converting 8-bit color components to floats. */
    private static final float NORMALIZE_8_BIT = 255.0f;

    private Color() {
        // no instances
    }


    public static int rgbi(byte r, byte g, byte b) {
        return (Byte.toUnsignedInt(r) << 16) | (Byte.toUnsignedInt(g) << 8) | Byte.toUnsignedInt(b);
    }

    public static int rgbai(byte r, byte g, byte b, byte a) {
        return (Byte.toUnsignedInt(a) << 24) | (Byte.toUnsignedInt(r) << 16) | (Byte.toUnsignedInt(g) << 8) | Byte.toUnsignedInt(b);
    }

    /**
     * Converts a 24-bit RGB integer (0xRRGGBB) to a float array [r, g, b].
     * 
     * @param color The 24-bit integer color.
     * @return A new float array with 3 elements: [red, green, blue], normalized to [0.0, 1.0].
     */
    public static float @NonNull[] rgb3f(int color) {
        return new float[] {
            ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
            (color & 0xFF) / NORMALIZE_8_BIT
        };
    }

    /**
     * Converts a 24-bit RGB integer (0xRRGGBB) to a float array [r, g, b, 1.0].
     *
     * @param color The 24-bit integer color.
     * @return A new float array with 4 elements: [red, green, blue, alpha], normalized to [0.0, 1.0].
     */
    public static float @NonNull[] rgb4f(int color) {
        return argb4f(color | 0xFF000000);
    }

    /**
     * Converts a 32-bit ARGB integer (0xAARRGGBB) to a float array [r, g, b, a].
     * The output order is RGBA as expected by OpenGL.
     *
     * @param color The 32-bit ARGB integer color.
     * @return A new float array with 4 elements: [red, green, blue, alpha], normalized to [0.0, 1.0].
     */
    public static float @NonNull[] argb4f(int color) {
        return new float[] {
            ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
            (color & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 24) & 0xFF) / NORMALIZE_8_BIT
        };
    }

    /**
     * Converts a 32-bit RGBA integer (0xRRGGBBAA) to a float array [r, g, b, a].
     * 
     * @param color The 32-bit RGBA integer color.
     * @return A new float array with 4 elements: [red, green, blue, alpha], normalized to [0.0, 1.0].
     */
    public static float @NonNull[] rgba4f(int color) {
        return new float[] {
            ((color >> 24) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
            (color & 0xFF) / NORMALIZE_8_BIT
        };
    }

    /**
     * Converts a 24-bit RGB integer (0xRRGGBB) to a {@link Vector3fc}.
     * 
     * @param color The 24-bit integer color.
     * @return A new {@link Vector3f} with components (r, g, b), normalized to [0.0, 1.0].
     */
    public static @NonNull Vector3fc rgb3v(int color) {
        return new Vector3f(
            ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
            (color & 0xFF) / NORMALIZE_8_BIT);
    }

    /**
     * Converts a 32-bit ARGB integer (0xAARRGGBB) to a {@link Vector4fc}.
     * The output vector components are in (r, g, b, a) order as expected by OpenGL.
     *
     * @param color The 32-bit ARGB integer color.
     * @return A new {@link Vector4f} with components (r, g, b, a), normalized to [0.0, 1.0].
     */
    public static @NonNull Vector4fc argb4v(int color) {
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
    public static @NonNull Vector4fc rgba4v(int color) {
        return new Vector4f(
            ((color >> 24) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
            ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
            (color & 0xFF) / NORMALIZE_8_BIT);
    }
}

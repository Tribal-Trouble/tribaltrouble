package com.oddlabs.tt.audio.openal;

import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

/**
 * A utility class to isolate all LWJGL2 BufferUtils calls.
 * This will be the single point of change when migrating to LWJGL3's memory management.
 */
public final class ALBufferUtils {
    private ALBufferUtils() {
        // no instances
    }

    public static @NonNull FloatBuffer createFloatBuffer(int size) {
        return Objects.requireNonNull(BufferUtils.createFloatBuffer(size));
    }

    public static @NonNull IntBuffer createIntBuffer(int size) {
        return Objects.requireNonNull(BufferUtils.createIntBuffer(size));
    }

    public static @NonNull ByteBuffer createByteBuffer(int size) {
        return Objects.requireNonNull(BufferUtils.createByteBuffer(size));
    }
}

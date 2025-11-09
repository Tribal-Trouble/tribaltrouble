package com.oddlabs.tt.audio.openal;

import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * A utility class to isolate all LWJGL2 BufferUtils calls.
 * This will be the single point of change when migrating to LWJGL3's memory management.
 */
public final class ALBufferUtils {
    private ALBufferUtils() {
        // no instances
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return BufferUtils.createFloatBuffer(size);
    }

    public static IntBuffer createIntBuffer(int size) {
        return BufferUtils.createIntBuffer(size);
    }

    public static @NonNull ByteBuffer createByteBuffer(int size) {
        return BufferUtils.createByteBuffer(size);
    }
}

package com.oddlabs.tt.vbo;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;


/**
 * Factory for creating VertexArray instances based on available OpenGL capabilities.
 */
public final class VertexArrays {
    public enum Availability {
        GL30,
        ARB,
        COMPAT
    }

    private static class Holder {
        private static final @NonNull Availability availability;

        static {
            GLCapabilities caps = GL.getCapabilities();
            availability = caps.OpenGL30
                    ? Availability.GL30
                    : caps.GL_ARB_vertex_array_object
                        ? Availability.ARB
                        : Availability.COMPAT;
        }
    }

    private VertexArrays() {}

    public static boolean isSupported() {
        return Holder.availability != Availability.COMPAT;
    }

    public static @NonNull VertexArray create() {
        return switch (Holder.availability) {
            case GL30 -> new GL30VertexArray();
            case ARB -> new ARBVertexArray();
            default -> new NoOpVertexArray();
        };
    }

    /**
     * Binds the default VAO (0).
     */
    public static void unbind() {
        switch (Holder.availability) {
            case GL30 -> org.lwjgl.opengl.GL30.glBindVertexArray(0);
            case ARB -> org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray(0);
        }
    }
}

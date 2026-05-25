package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

/**
 * describes a vertex attribute used by a shader
 */
public interface VertexAttribute {
    @NonNull
    String getName();

    int getComponentCount();

    int getGlType();

    boolean isNormalized();

    default int getSizeBytes() {
        return switch (getGlType()) {
            case GL11.GL_FLOAT -> getComponentCount() * Float.BYTES;
            case GL11.GL_UNSIGNED_BYTE -> getComponentCount();
            case GL12.GL_UNSIGNED_INT_8_8_8_8_REV -> Integer.BYTES;
            default -> throw new IllegalStateException("Unsupported GL type: " + getGlType());
        };
    }

    default void enable(int location) {
        GL20.glEnableVertexAttribArray(location);
    }

    default void disable(int location) {
        GL20.glDisableVertexAttribArray(location);
    }

    default void setPointer(int location, int stride, int offset) {
        GL20.glVertexAttribPointer(location, getComponentCount(), getGlType(), isNormalized(), stride, offset);
    }
}

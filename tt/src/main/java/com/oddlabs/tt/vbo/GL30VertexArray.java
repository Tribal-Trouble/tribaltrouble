package com.oddlabs.tt.vbo;

import org.lwjgl.opengl.GL30;

/**
 * Implementation of VertexArray using OpenGL 3.0 core functions.
 */
final class GL30VertexArray implements VertexArray {
    private final int id;

    GL30VertexArray() {
        this.id = GL30.glGenVertexArrays();
    }

    @Override
    public void bind() {
        GL30.glBindVertexArray(id);
    }

    @Override
    public void unbind() {
        GL30.glBindVertexArray(0);
    }

    @Override
    public void delete() {
        GL30.glDeleteVertexArrays(id);
    }
}

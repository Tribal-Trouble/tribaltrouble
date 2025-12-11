package com.oddlabs.tt.vbo;

import org.lwjgl.opengl.ARBVertexArrayObject;

/**
 * Implementation of VertexArray using the GL_ARB_vertex_array_object extension.
 */
final class ARBVertexArray implements VertexArray {
    private final int id;

    ARBVertexArray() {
        this.id = ARBVertexArrayObject.glGenVertexArrays();
    }

    @Override
    public void bind() {
        ARBVertexArrayObject.glBindVertexArray(id);
    }

    @Override
    public void unbind() {
        ARBVertexArrayObject.glBindVertexArray(0);
    }

    @Override
    public void delete() {
        ARBVertexArrayObject.glDeleteVertexArrays(id);
    }
}

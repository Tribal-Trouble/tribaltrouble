package com.oddlabs.tt.vbo;

/**
 * Fallback implementation of VertexArray that does nothing.
 * Used when neither OpenGL 3.0 nor ARB_vertex_array_object is available.
 */
final class NoOpVertexArray implements VertexArray {
    @Override
    public void bind() {
        // No-op
    }

    @Override
    public void unbind() {
        // No-op
    }

    @Override
    public void delete() {
        // No-op
    }
}

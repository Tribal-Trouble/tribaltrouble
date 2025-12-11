package com.oddlabs.tt.vbo;

/**
 * Abstraction for Vertex Array Objects (VAOs) to support both OpenGL 3.0+
 * core profile (GL30) and OpenGL 2.1 with extensions (GL_ARB_vertex_array_object).
 */
public interface VertexArray {
    /**
     * Binds this VAO. Subsequent vertex attribute pointer settings and draw calls
     * will be recorded into or affect this VAO.
     */
    void bind();

    /**
     * Unbinds this VAO.
     */
    void unbind();

    /**
     * Deletes this VAO.
     */
    void delete();
}

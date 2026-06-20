package com.oddlabs.tt.vbo;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

public final class QuadVBO implements AutoCloseable {
    private static final float[] QUAD_VERTICES = new float[]{
            // Triangle 1
            -1f, -1f, 0f, 0f, // bottom-left
            1f, -1f, 1f, 0f, // bottom-right
            1f, 1f, 1f, 1f, // top-right
            // Triangle 2
            1f, 1f, 1f, 1f, // top-right
            -1f, 1f, 0f, 1f, // top-left
            -1f, -1f, 0f, 0f  // bottom-left
    };
    private final VertexArray vao = new VertexArray();
    private final @NonNull FloatVBO vbo;

    public QuadVBO() {
        try (var stack = MemoryStack.stackPush()) {
            var vertices = stack.floats(QUAD_VERTICES);
            vbo = new FloatVBO(GL15.GL_STATIC_DRAW, vertices);

            vao.bind();
            vbo.bind();

            // Position attribute
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);

            // TexCoord attribute
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

            vao.unbind();
        }
    }

    public void render() {
        vao.bind();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        vao.unbind();
    }

    @Override
    public void close() {
        vbo.close();
        vao.close();
    }
}

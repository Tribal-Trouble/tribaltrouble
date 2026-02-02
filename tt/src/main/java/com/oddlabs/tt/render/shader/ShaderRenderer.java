package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.Objects;

/**
 * Renders geometry using a shader program abstracting away the OpenGL VBO and shader setup.
 */
public class ShaderRenderer implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 12; // pos(3) + normal(3) + color(4) + uv(2)
    private static final int INITIAL_VERTEX_CAPACITY = 1024;

    private final @NonNull ShaderProgram shader;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final VertexArray vao = new VertexArray();

    private final FloatBuffer vertexBuffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(INITIAL_VERTEX_CAPACITY * FLOATS_PER_VERTEX));
    private int vboHandle = 0;
    private int vertexCount = 0;
    private int mode = GL11.GL_TRIANGLES;

    public ShaderRenderer(@NonNull ShaderProgram shader, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.shader = shader;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;

        vao.bind();
        this.vboHandle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) vertexBuffer.capacity() * Float.BYTES, GL15.GL_STREAM_DRAW);

        var layout = new VertexLayout<>(
                DebugMeshShader.Attribute.POSITION,
                DebugMeshShader.Attribute.NORMAL,
                DebugMeshShader.Attribute.COLOR,
                DebugMeshShader.Attribute.TEX_COORD_0
        );
        layout.bind(shader);

        vao.unbind();
    }

    public @NonNull ShaderProgram getShader() {
        return shader;
    }

    protected final @NonNull MatrixStack getModelViewStack() {
        return modelViewStack;
    }

    public void begin(int glMode) {
        vertexBuffer.clear();
        vertexCount = 0;
        this.mode = glMode;
    }

    public void vertex(float x, float y, float z,
                       float nx, float ny, float nz,
                       float r, float g, float b, float a,
                       float u, float v) {
        if (vertexBuffer.remaining() < FLOATS_PER_VERTEX) {
            flush();
            vertexBuffer.clear();
        }

        vertexBuffer.put(x).put(y).put(z);
        vertexBuffer.put(nx).put(ny).put(nz);
        vertexBuffer.put(r).put(g).put(b).put(a);
        vertexBuffer.put(u).put(v);
        vertexCount++;
    }

    public void end() {
        flush();
    }

    protected void flush() {
        flush(1.0f);
    }

    protected void flush(float pointSize) {
        if (vertexCount == 0) {
            return;
        }

        vertexBuffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        try (var _ = shader.use()) {
            shader.setUniformMatrix4(DebugMeshShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniform(DebugMeshShader.Uniforms.ENABLE_LIGHTING, false);
            shader.setUniform(DebugMeshShader.Uniforms.ENABLE_TEXTURE, false);
            shader.setUniform(DebugMeshShader.Uniforms.ALPHA_CUTOFF, 0.0f);
            shader.setUniform(DebugMeshShader.Uniforms.REPLACE_MODE, false);
            shader.setUniform(DebugMeshShader.Uniforms.POINT_SIZE, pointSize);

            vao.bind();
            GL11.glDrawArrays(mode, 0, vertexCount);
            vao.unbind();
        }

        vertexCount = 0;
    }

    @Override
    public void close() {
        if (vboHandle != 0) {
            GL15.glDeleteBuffers(vboHandle);
            vboHandle = 0;
        }
        vao.close();
    }
}

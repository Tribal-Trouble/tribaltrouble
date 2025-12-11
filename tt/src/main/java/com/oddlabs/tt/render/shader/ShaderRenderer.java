package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.tt.vbo.VertexArrays;
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
    private final @NonNull VertexLayout<FixedFunctionShader.Attribute> layout;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final @NonNull VertexArray vao;

    private final FloatBuffer vertexBuffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(INITIAL_VERTEX_CAPACITY * FLOATS_PER_VERTEX));
    private int vboHandle = 0;
    private int vertexCount = 0;
    private int mode = GL11.GL_TRIANGLES;

    public ShaderRenderer(@NonNull ShaderProgram shader, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.shader = shader;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.layout = new VertexLayout<>(
                FixedFunctionShader.Attribute.POSITION,
                FixedFunctionShader.Attribute.NORMAL,
                FixedFunctionShader.Attribute.COLOR,
                FixedFunctionShader.Attribute.TEX_COORD_0
        );
        this.vao = VertexArrays.create();
        if (VertexArrays.isSupported()) {
            vao.bind();
            if (vboHandle == 0) {
                vboHandle = GL15.glGenBuffers();
            }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
            layout.bind(shader);
            vao.unbind();
        }
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

    private void flush() {
        if (vertexCount == 0) {
            return;
        }

        vertexBuffer.flip();

        if (vboHandle == 0) {
            vboHandle = GL15.glGenBuffers();
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STREAM_DRAW);

        try (var _ = shader.use()) {
            shader.setUniformMatrix4(FixedFunctionShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniformMatrix4(FixedFunctionShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_LIGHTING, 0); // Debug rendering usually unlit
            shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_TEXTURE, 0); // Debug rendering usually untextured
            shader.setUniform(FixedFunctionShader.Uniforms.ALPHA_CUTOFF, 0.0f); // Disable alpha test for debug
            shader.setUniform(FixedFunctionShader.Uniforms.REPLACE_MODE, false); // Default to modulate
            shader.setUniform(FogShader.FOG_MODE, -1); // Disable fog for debug rendering

            if (VertexArrays.isSupported()) {
                vao.bind();
            } else {
                layout.bind(shader);
            }

            GL11.glDrawArrays(mode, 0, vertexCount);

            if (VertexArrays.isSupported()) {
                vao.unbind();
            } else {
                layout.unbind(shader);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
        }

        vertexCount = 0;
    }

    @Override
    public void close() {
        if (vboHandle != 0) {
            GL15.glDeleteBuffers(vboHandle);
            vboHandle = 0;
        }
        if (VertexArrays.isSupported()) {
            vao.delete();
        }
    }
}

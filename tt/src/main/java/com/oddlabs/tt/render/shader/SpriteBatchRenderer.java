package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.tt.vbo.VertexArrays;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

public final class SpriteBatchRenderer implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 12;
    private static final int VERTICES_PER_SPRITE = 4;
    private static final int INDICES_PER_SPRITE = 6;
    private static final int INITIAL_BATCH_SIZE = 256;

    private final @NonNull ShaderProgram shader;
    private final VertexLayout<FixedFunctionShader.Attribute> layout = new VertexLayout<>(
            FixedFunctionShader.Attribute.POSITION,
            FixedFunctionShader.Attribute.NORMAL,
            FixedFunctionShader.Attribute.COLOR,
            FixedFunctionShader.Attribute.TEX_COORD_0
    );
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final @NonNull VertexArray vao;

    private final FloatBuffer vertexBuffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(INITIAL_BATCH_SIZE * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX));
    private final ShortBuffer indexBuffer = Objects.requireNonNull(BufferUtils.createShortBuffer(INITIAL_BATCH_SIZE * INDICES_PER_SPRITE));
    private int vboHandle = 0;
    private int iboHandle = 0;
    private int spriteCount = 0;

    private @Nullable Texture currentTexture;
    private boolean drawing = false;
    private boolean useDepth;
    private boolean useFog;

    public SpriteBatchRenderer(@NonNull ShaderProgram shader, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.shader = shader;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.vao = VertexArrays.create();

        if (VertexArrays.isSupported()) {
            vao.bind();
        }

        if (vboHandle == 0) {
            vboHandle = GL15.glGenBuffers();
            iboHandle = GL15.glGenBuffers();
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboHandle);

        if (VertexArrays.isSupported()) {
            layout.bind(shader);
            vao.unbind();
        } else {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    public @NonNull MatrixStack getModelViewStack() {
        return modelViewStack;
    }

    public @NonNull MatrixStack getProjectionStack() {
        return projectionStack;
    }

    public void begin(@Nullable Texture texture, boolean useDepth, boolean useFog) {
        if (drawing) {
            throw new IllegalStateException("Already drawing");
        }

        spriteCount = 0;
        currentTexture = texture;
        this.useDepth = useDepth;
        this.useFog = useFog;
        drawing = true;
    }

    public void drawQuad(float x, float y, float z, float width, float height,
                         float r, float g, float b, float a,
                         float u0, float v0, float u1, float v1) {
        if (!drawing) {
            throw new IllegalStateException("Must call begin() first");
        }

        if (vertexBuffer.remaining() < VERTICES_PER_SPRITE * FLOATS_PER_VERTEX) {
            flush();
        }

        short baseIndex = (short) (spriteCount * VERTICES_PER_SPRITE);

        addVertex(x, y, z, 0, 0, 1, r, g, b, a, u0, v0);
        addVertex(x + width, y, z, 0, 0, 1, r, g, b, a, u1, v0);
        addVertex(x + width, y + height, z, 0, 0, 1, r, g, b, a, u1, v1);
        addVertex(x, y + height, z, 0, 0, 1, r, g, b, a, u0, v1);

        indexBuffer.put(baseIndex);
        indexBuffer.put((short) (baseIndex + 1));
        indexBuffer.put((short) (baseIndex + 2));
        indexBuffer.put(baseIndex);
        indexBuffer.put((short) (baseIndex + 2));
        indexBuffer.put((short) (baseIndex + 3));

        spriteCount++;
    }

    private void addVertex(float x, float y, float z, float nx, float ny, float nz,
                           float r, float g, float b, float a, float u, float v) {
        vertexBuffer.put(x).put(y).put(z);
        vertexBuffer.put(nx).put(ny).put(nz);
        vertexBuffer.put(r).put(g).put(b).put(a);
        if (currentTexture != null) {
            vertexBuffer.put(u).put(v);
        } else {
            vertexBuffer.put(0f).put(0f);
        }
    }

    public void end() {
        if (!drawing) {
            throw new IllegalStateException("Not drawing");
        }
        flush();
        drawing = false;
    }

    private void flush() {
        if (spriteCount == 0) {
            return;
        }

        vertexBuffer.flip();
        indexBuffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STREAM_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboHandle);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STREAM_DRAW);

        try (var _ = shader.use()) {
            shader.setUniformMatrix4(FixedFunctionShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniformMatrix4(FixedFunctionShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_LIGHTING, 0);
            shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_TEXTURE, currentTexture != null ? 1 : 0);
            shader.setUniform(FixedFunctionShader.Uniforms.ALPHA_CUTOFF, 0.1f);
            shader.setUniform(FixedFunctionShader.Uniforms.REPLACE_MODE, false);

            if (useFog) {
                // Assuming a default fog setup if not provided explicitly
            } else {
                shader.setUniform(FogShader.FOG_MODE, -1); // Disable fog
            }

            if (useDepth) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(true);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
            }

            if (currentTexture != null) {
                shader.setUniform(FixedFunctionShader.Uniforms.TEXTURE_0, 0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture.getHandle());
            }

            if (VertexArrays.isSupported()) {
                vao.bind();
            } else {
                layout.bind(shader);
            }

            GL11.glDrawElements(GL11.GL_TRIANGLES, indexBuffer.limit(), GL11.GL_UNSIGNED_SHORT, 0);

            if (VertexArrays.isSupported()) {
                vao.unbind();
            } else {
                layout.unbind(shader);
            }

            if (currentTexture != null) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            }

            if (!VertexArrays.isSupported()) {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            }
        }

        vertexBuffer.clear();
        indexBuffer.clear();
        spriteCount = 0;
    }

    public void close() {
        if (vboHandle != 0) {
            GL15.glDeleteBuffers(vboHandle);
            GL15.glDeleteBuffers(iboHandle);
            vboHandle = 0;
            iboHandle = 0;
        }
        if (VertexArrays.isSupported()) {
            vao.delete();
        }
    }
}

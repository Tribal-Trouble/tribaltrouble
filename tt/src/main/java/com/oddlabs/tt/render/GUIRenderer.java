package com.oddlabs.tt.render;

import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.ModeIconQuads;
import com.oddlabs.tt.render.shader.GUIShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import com.oddlabs.util.Quad;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

/**
 * A renderer for drawing 2D GUI elements using a shader-based batching system.
 */
public final class GUIRenderer {

    private static final int MAX_QUADS = 2048;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;

    private final @NonNull ShaderProgram shader;
    private final MatrixStack matrixStack = new MatrixStack(_ -> flush());
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final @NonNull VertexLayout<GUIShader.Attribute> layout;

    private final @NonNull VertexArray vao;
    private final int vbo;
    private final @NonNull ByteBuffer vertexBuffer;
    private final @NonNull Texture whiteTexture;

    private int quadCount = 0;
    private @Nullable Texture currentTexture;

    public GUIRenderer() {
        this.shader = new GUIShader();
        this.layout = new VertexLayout<>(
                GUIShader.Attribute.POSITION,
                GUIShader.Attribute.COLOR,
                GUIShader.Attribute.TEX_COORD
        );

        this.vao = new VertexArray();
        this.vbo = GL15.glGenBuffers();
        int ibo = GL15.glGenBuffers();
        this.vertexBuffer = BufferUtils.createByteBuffer(MAX_QUADS * VERTICES_PER_QUAD * layout.getStride());

        setupBuffers(ibo);

        ByteBuffer whitePixel = BufferUtils.createByteBuffer(Integer.BYTES);
        whitePixel.putInt(Color.WHITE_INT);
        whitePixel.flip();
        GLImage whiteImage = new GLByteImage(1, 1, whitePixel, GL11.GL_RGBA);
        whiteTexture = new Texture(new GLImage[]{whiteImage}, GL11.GL_RGBA, GL11.GL_NEAREST, GL11.GL_NEAREST, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
    }

    private void setupBuffers(int ibo) {
        vao.bind();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer.capacity(), GL15.GL_STREAM_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        ByteBuffer indexBuffer = BufferUtils.createByteBuffer(MAX_QUADS * INDICES_PER_QUAD * Short.BYTES);
        for (int i = 0; i < MAX_QUADS; i++) {
            int offset = i * VERTICES_PER_QUAD;
            indexBuffer.putShort((short) (offset));
            indexBuffer.putShort((short) (offset + 1));
            indexBuffer.putShort((short) (offset + 2));
            indexBuffer.putShort((short) (offset + 2));
            indexBuffer.putShort((short) (offset + 3));
            indexBuffer.putShort((short) (offset));
        }
        indexBuffer.flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        layout.bind(shader);

        vao.unbind();
    }

    public void renderFrame(float width, float height, @NonNull Runnable frameCommands) {
        GLUtils.checkGLError("Before GUI Render");
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullFaceEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        try (var _ = shader.use()) {
            if (!blendEnabled) GL11.glEnable(GL11.GL_BLEND);
            if (depthTestEnabled) GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (cullFaceEnabled) GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            projectionMatrix.identity().ortho(0, width, 0, height, -1, 1);
            shader.setUniformMatrix4(GUIShader.Uniforms.PROJECTION_MATRIX, false, projectionMatrix);
            shader.setUniform(GUIShader.Uniforms.TEXTURE, 0);

            matrixStack.clear();

            frameCommands.run();

            flush();
        } finally {
            if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
            if (depthTestEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (cullFaceEnabled) GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    public void drawColoredQuad(float x, float y, float w, float h, @NonNull Vector4fc color) {
        drawTexture(whiteTexture, x, y, w, h, 0, 0, 1, 1, color);
    }

    public void drawModeIcon(@NonNull ModeIconQuads iconQuad, ModeIconQuads.@NonNull Mode skinMode, float x, float y) {
        drawIcon(iconQuad.quad(skinMode), x, y);
    }

    public void drawIcon(@NonNull IconQuad iconQuad, float x, float y) {
        drawTexture(iconQuad.getTexture(), x, y, iconQuad.getWidth(), iconQuad.getHeight(), iconQuad.getU1(), iconQuad.getV1(), iconQuad.getU2(), iconQuad.getV2(), Color.WHITE);
    }

    public void drawIcon(@NonNull IconQuad iconQuad, float x, float y, @NonNull Vector4fc color) {
        drawTexture(iconQuad.getTexture(), x, y, iconQuad.getWidth(), iconQuad.getHeight(), iconQuad.getU1(), iconQuad.getV1(), iconQuad.getU2(), iconQuad.getV2(), color);
    }

    public void drawIcon(@NonNull IconQuad iconQuad, float x, float y, float w, float h) {
        drawTexture(iconQuad.getTexture(), x, y, w, h, iconQuad.getU1(), iconQuad.getV1(), iconQuad.getU2(), iconQuad.getV2(), Color.WHITE);
    }

    public void drawTexture(@NonNull Texture texture, float x, float y, float w, float h, float u1, float v1, float u2, float v2, @NonNull Vector4fc tint) {
        if (currentTexture != null && texture.getHandle() != currentTexture.getHandle() || quadCount >= MAX_QUADS) {
            flush();
        }

        if (currentTexture == null || currentTexture.getHandle() != texture.getHandle()) {
            this.currentTexture = texture;
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
        }

        // pack color to ABGR (0xAABBGGRR) for Little Endian byte buffer
        int tintInt = Color.abgri(tint);

        vertexBuffer.putFloat(x).putFloat(y).putFloat(0).putInt(tintInt).putFloat(u1).putFloat(v1);
        vertexBuffer.putFloat(x + w).putFloat(y).putFloat(0).putInt(tintInt).putFloat(u2).putFloat(v1);
        vertexBuffer.putFloat(x + w).putFloat(y + h).putFloat(0).putInt(tintInt).putFloat(u2).putFloat(v2);
        vertexBuffer.putFloat(x).putFloat(y + h).putFloat(0).putInt(tintInt).putFloat(u1).putFloat(v2);

        quadCount++;
    }

    public void flush() {
        if (quadCount == 0) return;

        shader.setUniformMatrix4(GUIShader.Uniforms.MODEL_VIEW_MATRIX, false, matrixStack.current());

        vertexBuffer.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        vao.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, quadCount * INDICES_PER_QUAD, GL11.GL_UNSIGNED_SHORT, 0);
        vao.unbind();

        quadCount = 0;
        currentTexture = null;
        vertexBuffer.clear();
    }

    @NonNull
    public MatrixStack getMatrixStack() {
        return matrixStack;
    }
}

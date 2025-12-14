package com.oddlabs.tt.render;

import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.ModeIconQuads;
import com.oddlabs.tt.render.shader.GUIShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.util.Color;
import com.oddlabs.util.Quad;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
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

    private final int vbo;
    private final int ibo;
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

        this.vbo = GL15.glGenBuffers();
        this.ibo = GL15.glGenBuffers();
        this.vertexBuffer = BufferUtils.createByteBuffer(MAX_QUADS * VERTICES_PER_QUAD * layout.getStride());

        setupBuffers();

        ByteBuffer whitePixel = BufferUtils.createByteBuffer(Integer.BYTES);
        whitePixel.putInt(Color.WHITE_INT);
        whitePixel.flip();
        GLImage whiteImage = new GLByteImage(1, 1, whitePixel, GL11.GL_RGBA);
        whiteTexture = new Texture(new GLImage[]{whiteImage}, GL11.GL_RGBA, GL11.GL_NEAREST, GL11.GL_NEAREST, GL11.GL_CLAMP, GL11.GL_CLAMP);
    }

    private void setupBuffers() {
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

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void renderFrame(float width, float height, @NonNull Runnable frameCommands) {
        try (var _ = shader.use();
             var _ = GLStateHelper.blend(true);
             var _ = GLStateHelper.depthTest(false);
             var _ = GLStateHelper.cullFace(false)) {

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            projectionMatrix.identity().ortho(0, width, 0, height, -1, 1);
            shader.setUniformMatrix4(GUIShader.Uniforms.PROJECTION_MATRIX, false, projectionMatrix);
            shader.setUniform(GUIShader.Uniforms.TEXTURE, 0);

            matrixStack.clear();

            frameCommands.run();

            flush();
        }
    }

    public void drawTexture(@NonNull Texture texture, float x, float y, float w, float h) {
        drawQuad(texture, x, y, w, h, 0, 0, 1, 1, Color.WHITE_INT);
    }

    public void drawTexture(@NonNull Texture texture, float x, float y, float w, float h, int tint) {
        drawQuad(texture, x, y, w, h, 0, 0, 1, 1, tint);
    }

    public void drawColoredQuad(float x, float y, float w, float h, int color) {
        drawQuad(whiteTexture, x, y, w, h, 0, 0, 1, 1, color);
    }

    public void drawQuad(@NonNull Texture texture, @NonNull Quad quad, float x, float y, int color) {
        drawQuad(texture, x, y, quad.getWidth(), quad.getHeight(), quad.getU1(), quad.getV1(), quad.getU2(), quad.getV2(), color);
    }

    public void drawQuad(@NonNull IconQuad iconQuad, float x, float y, int color) {
        drawQuad(iconQuad.getTexture(), x, y, iconQuad.getWidth(), iconQuad.getHeight(), iconQuad.getU1(), iconQuad.getV1(), iconQuad.getU2(), iconQuad.getV2(), color);
    }

    public void drawQuad(@NonNull IconQuad iconQuad, float x, float y, float w, float h, int color) {
        drawQuad(iconQuad.getTexture(), x, y, w, h, iconQuad.getU1(), iconQuad.getV1(), iconQuad.getU2(), iconQuad.getV2(), color);
    }

    public void drawQuad(@NonNull ModeIconQuads iconQuad, float x, float y, ModeIconQuads.@NonNull Mode skinMode, int color) {
        drawQuad(iconQuad.quad(skinMode), x, y, color);
    }

    public void drawQuad(@NonNull Texture texture, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int tint) {
        if (currentTexture != null && texture.getHandle() != currentTexture.getHandle() || quadCount >= MAX_QUADS) {
            flush();
        }

        if (currentTexture == null) {
            this.currentTexture = texture;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
        }

        vertexBuffer.putFloat(x).putFloat(y).putFloat(0).putInt(tint).putFloat(u1).putFloat(v1);
        vertexBuffer.putFloat(x + w).putFloat(y).putFloat(0).putInt(tint).putFloat(u2).putFloat(v1);
        vertexBuffer.putFloat(x + w).putFloat(y + h).putFloat(0).putInt(tint).putFloat(u2).putFloat(v2);
        vertexBuffer.putFloat(x).putFloat(y + h).putFloat(0).putInt(tint).putFloat(u1).putFloat(v2);

        quadCount++;
    }

    public void flush() {
        if (quadCount == 0) return;

        shader.setUniformMatrix4(GUIShader.Uniforms.MODEL_VIEW_MATRIX, false, matrixStack.current());

        vertexBuffer.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        layout.bind(shader);
        GL11.glDrawElements(GL11.GL_TRIANGLES, quadCount * INDICES_PER_QUAD, GL11.GL_UNSIGNED_SHORT, 0);
        layout.unbind(shader);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        quadCount = 0;
        currentTexture = null;
        vertexBuffer.clear();
    }

    @NonNull
    public MatrixStack getMatrixStack() {
        return matrixStack;
    }
}

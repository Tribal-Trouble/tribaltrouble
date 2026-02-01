package com.oddlabs.tt.render;

import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.ModeIconQuads;
import com.oddlabs.tt.render.shader.GUIShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A renderer for drawing 2D GUI elements using a shader-based batching system.
 * Supports multi-texturing to batch draw calls across different textures.
 */
public final class GUIRenderer {

    private static final int MAX_QUADS = 2048;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int MAX_TEXTURES = 8;

    private final @NonNull ShaderProgram shader;
    private final MatrixStack matrixStack = new MatrixStack(_ -> flush());
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final @NonNull VertexLayout<GUIShader.Attribute> layout;

    private final @NonNull VertexArray vao;
    private final int vbo;
    private final @NonNull ByteBuffer vertexBuffer;
    
    // Texture batching state
    private final Texture[] currentTextures = new Texture[MAX_TEXTURES];
    private int textureCount = 0;
    private int quadCount = 0;
    
    private RenderContext currentContext;

    public GUIRenderer() {
        this.shader = new GUIShader();
        this.layout = new VertexLayout<>(
                GUIShader.Attribute.POSITION,
                GUIShader.Attribute.COLOR,
                GUIShader.Attribute.TEX_COORD,
                GUIShader.Attribute.TEX_INDEX
        );

        this.vao = new VertexArray();
        this.vbo = GL15.glGenBuffers();
        int ibo = GL15.glGenBuffers();
        this.vertexBuffer = BufferUtils.createByteBuffer(MAX_QUADS * VERTICES_PER_QUAD * layout.getStride());

        setupBuffers(ibo);
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

    public void renderFrame(@NonNull RenderContext context, float width, float height, @NonNull Runnable frameCommands) {
        GLUtils.checkGLError("Before GUI Render");
        this.currentContext = context;

        try (var _ = shader.use();
             var _ = context.withBlendMode(BlendMode.ALPHA);
             var _ = context.withDepthMode(DepthMode.NONE);
             var _ = context.withCullMode(CullMode.NONE)) {

            projectionMatrix.identity().ortho(0, width, 0, height, -1, 1);
            shader.setUniformMatrix4(GUIShader.Uniforms.PROJECTION_MATRIX, false, projectionMatrix);
            
            // Set texture unit indices [0, 1, ... 7]
            int[] units = new int[MAX_TEXTURES];
            for (int i = 0; i < MAX_TEXTURES; i++) units[i] = i;
            GL20.glUniform1iv(shader.getUniformLocation(GUIShader.Uniforms.TEXTURES), units);

            matrixStack.clear();

            frameCommands.run();

            flush();
        } finally {
            this.currentContext = null;
        }
    }

    public void drawColoredQuad(float x, float y, float w, float h, @NonNull Vector4fc color) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }
        // Use -1 for "no texture"
        putQuad(x, y, w, h, -1, -1, -1, -1, -1f, Color.abgri(color));
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
        if (quadCount >= MAX_QUADS) {
            flush();
        }

        float texIndex = getTextureIndex(texture);
        putQuad(x, y, w, h, u1, v1, u2, v2, texIndex, Color.abgri(tint));
    }
    
    private float getTextureIndex(@NonNull Texture texture) {
        for (int i = 0; i < textureCount; i++) {
            if (currentTextures[i].getHandle() == texture.getHandle()) {
                return (float) i;
            }
        }
        
        if (textureCount >= MAX_TEXTURES) {
            flush();
            return getTextureIndex(texture); // Try again in empty batch
        }
        
        currentTextures[textureCount] = texture;
        return (float) textureCount++;
    }

    private void putQuad(float x, float y, float w, float h, float u1, float v1, float u2, float v2, float texIndex, int color) {
        vertexBuffer.putFloat(x).putFloat(y).putFloat(0).putInt(color).putFloat(u1).putFloat(v1).putFloat(texIndex);
        vertexBuffer.putFloat(x + w).putFloat(y).putFloat(0).putInt(color).putFloat(u2).putFloat(v1).putFloat(texIndex);
        vertexBuffer.putFloat(x + w).putFloat(y + h).putFloat(0).putInt(color).putFloat(u2).putFloat(v2).putFloat(texIndex);
        vertexBuffer.putFloat(x).putFloat(y + h).putFloat(0).putInt(color).putFloat(u1).putFloat(v2).putFloat(texIndex);

        quadCount++;
    }

    public void flush() {
        if (quadCount == 0) return;

        shader.setUniformMatrix4(GUIShader.Uniforms.MODEL_VIEW_MATRIX, false, matrixStack.current());
        
        // Bind all active textures
        for (int i = 0; i < textureCount; i++) {
            if (currentContext != null) {
                currentContext.setTexture(i, currentTextures[i]);
            } else {
                // Fallback if context missing (shouldn't happen in normal flow)
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextures[i].getHandle());
            }
        }

        vertexBuffer.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        vao.bind();
        GL11.glDrawElements(GL11.GL_TRIANGLES, quadCount * INDICES_PER_QUAD, GL11.GL_UNSIGNED_SHORT, 0);
        vao.unbind();

        quadCount = 0;
        textureCount = 0;
        Arrays.fill(currentTextures, null);
        vertexBuffer.clear();
    }

    @NonNull
    public MatrixStack getMatrixStack() {
        return matrixStack;
    }
}
package com.oddlabs.tt.render;

import com.oddlabs.tt.render.shader.DecalShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * Renders textured decals such as dynamic shadows and selection halos using hardware instancing.
 */
public final class DecalRenderer implements AutoCloseable {

    private final DecalShader shader = new DecalShader();
    private final @NonNull VertexArray vao;
    private final @NonNull FloatVBO meshVBO;
    private final @NonNull ShortVBO meshIBO;
    private final @NonNull FloatVBO instanceVBO;

    public static final int HALO_LUT_RESOLUTION = 256;

    private static final int MAX_INSTANCES = 2048;
    private static final int FLOATS_PER_INSTANCE = 2 + 1 + 4 + 1 + 1 + 1 + 1 + 1; // Pos(2)+Size(1)+Color(4)+Pat(1)+Off(1)+Slot(1)+Flags(1)+ShadowOp(1)
    private final @NonNull FloatBuffer instanceBuffer;

    private int instanceCount = 0;
    private final TextureBatcher textureBatcher = new TextureBatcher(14);

    private static final int GRID_SIZE = 32; // 32x32 grid
    private static final int VERTEX_COUNT = GRID_SIZE * GRID_SIZE;
    private static final int INDEX_COUNT = (GRID_SIZE - 1) * (GRID_SIZE - 1) * 6;

    public DecalRenderer() {
        this.vao = new VertexArray();
        this.vao.bind();

        // 1. Setup Mesh (Grid)
        // Position (2 floats)
        this.meshVBO = new FloatVBO(GL15.GL_STATIC_DRAW, VERTEX_COUNT * 2);
        FloatBuffer vertices = BufferUtils.createFloatBuffer(VERTEX_COUNT * 2);
        float step = 1.0f / (GRID_SIZE - 1);
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                vertices.put(x * step - 0.5f);
                vertices.put(y * step - 0.5f);
            }
        }
        vertices.flip();
        this.meshVBO.put(vertices);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);

        // Indices
        this.meshIBO = new ShortVBO(GL15.GL_STATIC_DRAW, INDEX_COUNT);
        ShortBuffer indices = BufferUtils.createShortBuffer(INDEX_COUNT);
        for (int y = 0; y < GRID_SIZE - 1; y++) {
            for (int x = 0; x < GRID_SIZE - 1; x++) {
                short topLeft = (short) (y * GRID_SIZE + x);
                short topRight = (short) (topLeft + 1);
                short bottomLeft = (short) ((y + 1) * GRID_SIZE + x);
                short bottomRight = (short) (bottomLeft + 1);

                indices.put(topLeft).put(bottomLeft).put(topRight);
                indices.put(topRight).put(bottomLeft).put(bottomRight);
            }
        }
        indices.flip();
        this.meshIBO.put(indices);

        // 2. Setup Instance Buffer
        this.instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, MAX_INSTANCES * FLOATS_PER_INSTANCE);
        this.instanceBuffer = BufferUtils.createFloatBuffer(MAX_INSTANCES * FLOATS_PER_INSTANCE);

        // Instance Attributes
        int stride = FLOATS_PER_INSTANCE * Float.BYTES;

        // in_InstancePos (Loc 4, 2 floats)
        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 2, GL11.GL_FLOAT, false, stride, 0);
        GL33.glVertexAttribDivisor(4, 1);

        // in_InstanceSize (Loc 5, 1 float)
        GL20.glEnableVertexAttribArray(5);
        GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, stride, 2 * Float.BYTES);
        GL33.glVertexAttribDivisor(5, 1);

        // in_InstanceColor (Loc 3, 4 floats)
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        GL33.glVertexAttribDivisor(3, 1);

        // in_InstancePattern (Loc 6, 1 float)
        GL20.glEnableVertexAttribArray(6);
        GL20.glVertexAttribPointer(6, 1, GL11.GL_FLOAT, false, stride, 7 * Float.BYTES);
        GL33.glVertexAttribDivisor(6, 1);

        // in_InstanceOffsetScale (Loc 7, 1 float)
        GL20.glEnableVertexAttribArray(7);
        GL20.glVertexAttribPointer(7, 1, GL11.GL_FLOAT, false, stride, 8 * Float.BYTES);
        GL33.glVertexAttribDivisor(7, 1);

        // in_InstanceTextureSlot (Loc 8, 1 float)
        GL20.glEnableVertexAttribArray(8);
        GL20.glVertexAttribPointer(8, 1, GL11.GL_FLOAT, false, stride, 9 * Float.BYTES);
        GL33.glVertexAttribDivisor(8, 1);

        // in_InstanceFlags (Loc 9, 1 float)
        GL20.glEnableVertexAttribArray(9);
        GL20.glVertexAttribPointer(9, 1, GL11.GL_FLOAT, false, stride, 10 * Float.BYTES);
        GL33.glVertexAttribDivisor(9, 1);

        // in_InstanceShadowOpacity (Loc 10, 1 float)
        GL20.glEnableVertexAttribArray(10);
        GL20.glVertexAttribPointer(10, 1, GL11.GL_FLOAT, false, stride, 11 * Float.BYTES);
        GL33.glVertexAttribDivisor(10, 1);

        this.vao.unbind();
    }

    public void clear() {
        instanceCount = 0;
        textureBatcher.clear();
        instanceBuffer.clear();
    }

    private int setupCount = 0;
    private ScopedState shaderState;
    private ScopedState blendState;
    private ScopedState depthState;
    private ScopedState cullState;

    public @NonNull ScopedState setup(@NonNull RenderContext context, @NonNull LandscapeRenderer landscape,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (setupCount == 0) {
            shaderState = shader.use();

            shader.setUniform(DecalShader.Uniforms.MODEL_VIEW_MATRIX, modelViewStack.current());

            shader.setUniform(DecalShader.Uniforms.WORLD_SIZE, (float) landscape.getHeightMap().getMetersPerWorld());
            shader.setUniform(DecalShader.Uniforms.DEPTH_BIAS, 0.05f);

            context.setTexture(1, landscape.getHeightMap().getClientState(HeightMapVisual.class).map(
                    HeightMapVisual::getHeightTexture).orElseThrow());
            shader.setUniform(DecalShader.Uniforms.HEIGHT_MAP, 1);

            int[] textureUnits = new int[14];
            for (int i = 0; i < 14; i++) textureUnits[i] = i + 2; // Offset by 2 (0=DecalUnit, 1=HeightMap)
            shader.setUniform(DecalShader.Uniforms.TEXTURES, textureUnits);
            // Render State: Use Premultiplied blending for stable color/shadow combination.
            blendState = context.withBlendMode(BlendMode.PREMULTIPLIED);
            depthState = context.withDepthMode(DepthMode.READ_ONLY);
            cullState = context.withCullMode(CullMode.NONE);

            // Bias to prevent Z-fighting with terrain
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-16.0f, -32.0f);
        }
        setupCount++;

        return () -> {
            setupCount--;
            if (setupCount == 0) {
                flush(context);
                shaderState.close();
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

                cullState.close();
                depthState.close();
                blendState.close();
            }
        };
    }

    /**
     * Draws the specified decal texture with the provided tint and pattern at the specified position and size.
     */
    public void draw(@NonNull RenderContext context, @NonNull Texture texture, float x, float y, float size,
            Color.@NonNull Linear color, float patternVal, float shadowOffsetScale, boolean radial,
            float shadowOpacity) {
        int slot = textureBatcher.getOrAssignSlot(texture);
        if (slot == -1 || instanceCount >= MAX_INSTANCES) {
            flush(context);
            slot = textureBatcher.getOrAssignSlot(texture);
        }

        instanceBuffer.put(x);
        instanceBuffer.put(y);
        instanceBuffer.put(size);
        instanceBuffer.put(color.r());
        instanceBuffer.put(color.g());
        instanceBuffer.put(color.b());
        instanceBuffer.put(color.a());
        instanceBuffer.put(patternVal);
        instanceBuffer.put(shadowOffsetScale);
        instanceBuffer.put((float) slot);
        instanceBuffer.put(radial ? 1.0f : 0.0f);
        instanceBuffer.put(shadowOpacity);
        instanceCount++;
    }

    private void flush(@NonNull RenderContext context) {
        if (instanceCount == 0) return;

        textureBatcher.bindTextures(context, 2);

        vao.bind();
        instanceVBO.bind(context);
        instanceBuffer.flip();
        instanceVBO.put(instanceBuffer);

        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, INDEX_COUNT, GL11.GL_UNSIGNED_SHORT, 0, instanceCount);

        vao.unbind();

        // Reset for next batch
        instanceCount = 0;
        textureBatcher.clear();
        instanceBuffer.clear();
    }


    @Override
    public void close() {
        shader.close(); // Dispose shader program
        vao.close();
        meshVBO.close();
        meshIBO.close();
        instanceVBO.close();
    }
}

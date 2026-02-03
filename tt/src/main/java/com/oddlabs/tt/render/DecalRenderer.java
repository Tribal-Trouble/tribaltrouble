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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public final class DecalRenderer implements AutoCloseable {

    private final DecalShader shader = new DecalShader();
    private final @NonNull VertexArray vao;
    private final @NonNull FloatVBO meshVBO;
    private final @NonNull ShortVBO meshIBO;
    private final @NonNull FloatVBO instanceVBO;
    
    private static final int MAX_INSTANCES = 1024;
    private static final int FLOATS_PER_INSTANCE = 2 + 1 + 4; // Pos(2) + Size(1) + Color(4)
    private final @NonNull FloatBuffer instanceBuffer;
    
    private int instanceCount = 0;
    private @Nullable Texture currentTexture;

    private static final int GRID_SIZE = 8; // 8x8 grid
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

        this.vao.unbind();
    }

    public @NonNull ScopedState setup(@NonNull RenderContext context, @NonNull LandscapeRenderer landscape, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        var shaderUseState = shader.use();
        
        shader.setUniformMatrix4(DecalShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
        
        shader.setUniform(DecalShader.Uniforms.WORLD_SIZE, (float) landscape.getHeightMap().getMetersPerWorld());
        shader.setUniform(DecalShader.Uniforms.DEPTH_BIAS, 0.05f);
        
        context.setTexture(1, landscape.getHeightMap().getHeightTexture());
        shader.setUniform(DecalShader.Uniforms.HEIGHT_MAP, 1);

        // Render State
        var blend = context.withBlendMode(BlendMode.ALPHA);
        var depth = context.withDepthMode(DepthMode.READ_ONLY);
        var cull = context.withCullMode(CullMode.NONE);

        // Bias to prevent Z-fighting with terrain
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-16.0f, -32.0f);
        
        // Disable writing to Mask Buffer (Attachment 1) since DecalShader doesn't output to it
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);

        return () -> {
            flush(context);
            shaderUseState.close();
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            
            cull.close();
            depth.close();
            blend.close();
            
            // Restore draw buffers (Color + Mask)
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                java.nio.IntBuffer buffers = stack.mallocInt(2);
                buffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();
                GL20.glDrawBuffers(buffers);
            }
            this.currentTexture = null;
        };
    }

    public void draw(@NonNull RenderContext context, @NonNull Texture texture, float x, float y, float size, float r, float g, float b, float a) {
        if (currentTexture != texture) {
            flush(context);
            currentTexture = texture;
        }

        if (instanceCount >= MAX_INSTANCES) {
            flush(context);
        }

        instanceBuffer.put(x);
        instanceBuffer.put(y);
        instanceBuffer.put(size);
        instanceBuffer.put(r);
        instanceBuffer.put(g);
        instanceBuffer.put(b);
        instanceBuffer.put(a);
        instanceCount++;
    }

    private void flush(@NonNull RenderContext context) {
        if (instanceCount == 0 || currentTexture == null) return;

        context.setTexture(0, currentTexture.getHandle());
        shader.setUniform(DecalShader.Uniforms.TEXTURE, 0);

        vao.bind();
        instanceVBO.makeCurrent();
        instanceBuffer.flip();
        instanceVBO.put(instanceBuffer); // Or glBufferSubData if partial
        instanceBuffer.clear(); // Reset for writing

        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, INDEX_COUNT, GL11.GL_UNSIGNED_SHORT, 0, instanceCount);
        
        vao.unbind();
        instanceCount = 0;
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
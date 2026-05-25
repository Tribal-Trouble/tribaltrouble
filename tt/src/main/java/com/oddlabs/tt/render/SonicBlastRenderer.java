package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.render.shader.SonicBlastShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.Queue;

public final class SonicBlastRenderer implements AutoCloseable {
    private static final VertexLayout<SonicBlastShader.Attribute> LAYOUT = new VertexLayout<>(
            SonicBlastShader.Attribute.POSITION,
            SonicBlastShader.Attribute.TEX_COORD
    );
    private final @NonNull SonicBlastShader shader;
    private final @NonNull FloatVBO vbo;
    private final VertexArray vao = new VertexArray();

    public SonicBlastRenderer() {
        shader = new SonicBlastShader();

        // Create a simple quad centered at 0,0 on XY plane, scaled to 1x1
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4 * 5); // 4 verts * (3 pos + 2 uv)
        float s = 0.5f;
        // Pos (x,y,z), UV (u,v)
        buffer.put(-s).put(-s).put(0).put(0).put(0);
        buffer.put(s).put(-s).put(0).put(1).put(0);
        buffer.put(-s).put(s).put(0).put(0).put(1);
        buffer.put(s).put(s).put(0).put(1).put(1);
        buffer.flip();

        vbo = new FloatVBO(GL15.GL_STATIC_DRAW, buffer);

        vao.bind();
        vbo.makeCurrent();
        LAYOUT.bind(shader);
        vao.unbind();
    }

    public void render(@NonNull RenderContext context, @NonNull RenderQueues render_queues,
            @NonNull Queue<@NonNull SonicBlastEffect> queue, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack,
            @NonNull TextureKey noiseTextureKey) {
        if (queue.isEmpty()) return;

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ADDITIVE); var _ = context.withDepthMode(
                DepthMode.NONE); var _ = context.withCullMode(CullMode.NONE)) {

            shader.setUniform(SonicBlastShader.Uniforms.COLOR, 0.7f, 0.85f, 1.0f); // Electric blue/white

            vao.bind();

            for (SonicBlastEffect effect : queue) {
                if (effect.isDead()) continue;

                modelViewStack.push();

                float x = effect.getPositionX();
                float y = effect.getPositionY();
                float z = effect.getPositionZ();
                // Visual radius is 20% larger than damage radius ("felt but no damage")
                float visualRadius = effect.getMaxRadius() * 1.2f;
                float r = visualRadius * 2.0f; // Quad size (diameter)

                // Position and scale the quad to be parallel to the ground
                modelViewStack.translate(x, y, z);
                modelViewStack.scale(r, r, 1.0f);

                shader.setUniformMatrix4(SonicBlastShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
                shader.setUniform(SonicBlastShader.Uniforms.TIME, effect.getTime());
                shader.setUniform(SonicBlastShader.Uniforms.MAX_RADIUS, visualRadius);
                shader.setUniform(SonicBlastShader.Uniforms.EXPANSION_SPEED, visualRadius / Math.max(
                        effect.getDuration(), 0.001f));

                GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);

                modelViewStack.pop();
            }

            vao.unbind();
            context.setActiveTexture(0);
        }
    }

    @Override
    public void close() {
        shader.close();
        vbo.close();
        vao.close();
    }
}

package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Particle;
import com.oddlabs.tt.render.shader.ParticleShader;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.tt.vbo.VertexArrays;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Objects;

public final class EmitterRenderer {

    private final Vector3f right_plus_up = new Vector3f();
    private final Vector3f right_minus_up = new Vector3f();

    private final Matrix4f view_matrix = new Matrix4f();

    private static final int MAX_PARTICLES = 10000;
    private static final int FLOATS_PER_VERTEX = 9; // x,y,z,u,v,r,g,b,a
    private static final int VERTICES_PER_PARTICLE = 6;
    private static final int FLOATS_PER_PARTICLE = VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX;

    private final FloatBuffer particle_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(MAX_PARTICLES * FLOATS_PER_PARTICLE));
    private final FloatVBO particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());

    private final ParticleShader shader = new ParticleShader();
    private final SpriteShader spriteShader = new SpriteShader();
    private final VertexLayout<ParticleShader.Attribute> layout = new VertexLayout<>(
            ParticleShader.Attribute.POSITION,
            ParticleShader.Attribute.TEX_COORD,
            ParticleShader.Attribute.COLOR
    );
    private final @NonNull VertexArray vao;

    public EmitterRenderer() {
        vao = VertexArrays.create();
        if (VertexArrays.isSupported()) {
            vao.bind();
            particle_vbo.makeCurrent();
            layout.bind(shader);
            vao.unbind();
        }
    }

    public void render(@NonNull RenderQueues render_queues, @NonNull List<Emitter> emitter_queue, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (emitter_queue.isEmpty()) return;

        view_matrix.set(state.getModelView());
        float rx = view_matrix.m00(); float ry = view_matrix.m10(); float rz = view_matrix.m20();
        float upx = view_matrix.m01(); float upy = view_matrix.m11(); float upz = view_matrix.m21();
        right_plus_up.set(rx + upx, ry + upy, rz + upz);
        right_minus_up.set(rx - upx, ry - upy, rz - upz);

        try (var _ = shader.use();
             var _ = state.getFog().setup(shader, state.getCurrentZ());
             var _ = GLStateHelper.blend(true);
             var _ = new GLStateHelper.DepthMask(false)) {

            shader.setUniformMatrix4(ParticleShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniformMatrix4(ParticleShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniform(ParticleShader.Uniforms.TEXTURE_0, 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);

            if (VertexArrays.isSupported()) {
                vao.bind();
            } else {
                particle_vbo.makeCurrent();
                layout.bind(shader);
            }

            for (Emitter emitter : emitter_queue) {
                if (Globals.draw_particles)
                    renderInternal(render_queues, emitter, state, modelViewStack, projectionStack);
            }

        } finally {
            if (VertexArrays.isSupported()) {
                vao.unbind();
            } else {
                layout.unbind(shader);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
        }
    }

    private void render2DParticle(@NonNull Particle particle, @NonNull Emitter emitter) {
        float x = particle.getPosX();
        float y = particle.getPosY();
        float z = particle.getPosZ();
        float radius_x = particle.getRadiusX() * emitter.getScaleX();
        float radius_y = particle.getRadiusY() * emitter.getScaleY();
        float radius_z = particle.getRadiusZ() * emitter.getScaleZ();

        float r = particle.getColorR();
        float g = particle.getColorG();
        float b = particle.getColorB();
        float a = particle.getColorA();

        float p1x = x - right_plus_up.x * radius_x; float p1y = y - right_plus_up.y * radius_y; float p1z = z - right_plus_up.z * radius_z;
        float p2x = x + right_minus_up.x * radius_x; float p2y = y + right_minus_up.y * radius_y; float p2z = z + right_minus_up.z * radius_z;
        float p3x = x + right_plus_up.x * radius_x; float p3y = y + right_plus_up.y * radius_y; float p3z = z + right_plus_up.z * radius_z;
        float p4x = x - right_minus_up.x * radius_x; float p4y = y - right_minus_up.y * radius_y; float p4z = z - right_minus_up.z * radius_z;

        // Triangle 1
        particle_buffer.put(p1x).put(p1y).put(p1z).put(particle.getU1()).put(particle.getV1()).put(r).put(g).put(b).put(a);
        particle_buffer.put(p2x).put(p2y).put(p2z).put(particle.getU2()).put(particle.getV2()).put(r).put(g).put(b).put(a);
        particle_buffer.put(p3x).put(p3y).put(p3z).put(particle.getU3()).put(particle.getV3()).put(r).put(g).put(b).put(a);

        // Triangle 2
        particle_buffer.put(p1x).put(p1y).put(p1z).put(particle.getU1()).put(particle.getV1()).put(r).put(g).put(b).put(a);
        particle_buffer.put(p3x).put(p3y).put(p3z).put(particle.getU3()).put(particle.getV3()).put(r).put(g).put(b).put(a);
        particle_buffer.put(p4x).put(p4y).put(p4z).put(particle.getU4()).put(particle.getV4()).put(r).put(g).put(b).put(a);
    }

    private void renderInternal(@NonNull RenderQueues render_queues, @NonNull Emitter emitter, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        TextureKey[] textures = emitter.getTextures();
        List<Particle>[] particles = emitter.getParticles();
        SpriteKey[] sprite_renderers = emitter.getSpriteRenderers();
        if (textures != null) {
            GL11.glBlendFunc(emitter.getSrcBlendFunc(), emitter.getDstBlendFunc());

            for (int j = 0; j < particles.length; j++) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, render_queues.getTexture(textures[j]).getHandle());
                particle_buffer.clear();
                for (int i = particles[j].size() - 1; i >= 0; i--) {
                    Particle particle = particles[j].get(i);
                    render2DParticle(particle, emitter);
                }
                particle_buffer.flip();
                particle_vbo.put(particle_buffer);

                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, particles[j].size() * VERTICES_PER_PARTICLE);
            }
            // Restore default blend func after custom one
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else if (sprite_renderers != null) {
            for (int j = 0; j < particles.length; j++) {
                SpriteRenderer renderer = render_queues.getRenderer(sprite_renderers[j]);
                for (Particle particle : particles[j]) {
                    renderer.addToRenderList(PolyDetail.LOW_POLY, new ParticleModelState(particle, modelViewStack), false);
                }
            }
        }
    }

    public void debugRender(@NonNull List<@NonNull Emitter> emitter_queue) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (Emitter emitter : emitter_queue) {
                RenderTools.draw(emitter, 1f, 1f, 1f);
            }
        }
    }
}

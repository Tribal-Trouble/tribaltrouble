package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.StretchParticle;
import com.oddlabs.tt.render.shader.ParticleShader;
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

public final class LightningRenderer {

    private final Vector3f right_vector = new Vector3f();

    private final Matrix4f view_matrix = new Matrix4f();
    private final CameraState tmp_camera = new CameraState();

    private static final int MAX_PARTICLES = 1000;
    private static final int FLOATS_PER_PARTICLE = 72; // 8 vertices * 9 floats (x,y,z,u,v,r,g,b,a)

    private final FloatBuffer particle_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(MAX_PARTICLES * FLOATS_PER_PARTICLE));
    private final FloatVBO particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());

    private final @NonNull ParticleShader shader;
    private final VertexLayout<ParticleShader.Attribute> layout = new VertexLayout<>(
            ParticleShader.Attribute.POSITION,
            ParticleShader.Attribute.TEX_COORD,
            ParticleShader.Attribute.COLOR
    );
    private final @NonNull VertexArray vao;

    public LightningRenderer() {
        shader = new ParticleShader();
        vao = VertexArrays.create();
        if (VertexArrays.isSupported()) {
            vao.bind();
            particle_vbo.makeCurrent();
            layout.bind(shader);
            vao.unbind();
        }
    }

    public void render(@NonNull RenderQueues render_queues, @NonNull List<Lightning> emitter_queue, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (emitter_queue.isEmpty()) return;

        tmp_camera.set(state);
        view_matrix.identity();
        tmp_camera.setView(view_matrix);
        float rx = tmp_camera.getModelView().m00();
        float ry = tmp_camera.getModelView().m10();
        float rz = tmp_camera.getModelView().m20();
        right_vector.set(rx, ry, rz);

        try (var _ = shader.use();
             var _ = state.getFog().setup(shader, state.getCurrentZ());
             var _ = GLStateHelper.cullFace(false);
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

            if (Globals.draw_particles) {
                for (Lightning emitter : emitter_queue) {
                    renderInternal(render_queues, emitter);
                }
            }
        } finally {
            emitter_queue.clear();
            if (VertexArrays.isSupported()) {
                vao.unbind();
            } else {
                layout.unbind(shader);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
        }
    }

    private void render2DParticle(@NonNull StretchParticle particle) {
        float src_x = particle.getSrcX();
        float src_y = particle.getSrcY();
        float src_z = particle.getSrcZ();
        float dst_x = particle.getDstX();
        float dst_y = particle.getDstY();
        float dst_z = particle.getDstZ();

        float r = particle.getColorR();
        float g = particle.getColorG();
        float b = particle.getColorB();
        float a = particle.getColorA();

        particle_buffer.put(dst_x - particle.getDstWidth()).put(dst_y).put(dst_z).put(0f).put(0f).put(r).put(g).put(b).put(a);
        particle_buffer.put(dst_x + particle.getDstWidth()).put(dst_y).put(dst_z).put(1f).put(0f).put(r).put(g).put(b).put(a);
        particle_buffer.put(src_x + particle.getSrcWidth()).put(src_y).put(src_z).put(1f).put(1f).put(r).put(g).put(b).put(a);
        particle_buffer.put(src_x - particle.getSrcWidth()).put(src_y).put(src_z).put(0f).put(1f).put(r).put(g).put(b).put(a);

        particle_buffer.put(dst_x).put(dst_y - particle.getDstWidth()).put(dst_z).put(0f).put(0f).put(r).put(g).put(b).put(a);
        particle_buffer.put(dst_x).put(dst_y + particle.getDstWidth()).put(dst_z).put(1f).put(0f).put(r).put(g).put(b).put(a);
        particle_buffer.put(src_x).put(src_y + particle.getSrcWidth()).put(src_z).put(1f).put(1f).put(r).put(g).put(b).put(a);
        particle_buffer.put(src_x).put(src_y - particle.getSrcWidth()).put(src_z).put(0f).put(1f).put(r).put(g).put(b).put(a);
    }

    private void renderInternal(@NonNull RenderQueues render_queues, @NonNull Lightning lightning) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, render_queues.getTexture(lightning.getTexture()).getHandle());
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        particle_buffer.clear();
        List<StretchParticle> particles = lightning.getParticles();
        for (int i = particles.size() - 1; i >= 0; i--) {
            StretchParticle particle = particles.get(i);
            render2DParticle(particle);
        }
        particle_buffer.flip();
        particle_vbo.put(particle_buffer);

        GL11.glDrawArrays(GL11.GL_QUADS, 0, particles.size() * 8);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void debugRender(@NonNull List<Lightning> emitter_queue) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (Lightning emitter : emitter_queue) {
                RenderTools.draw(emitter, 1f, 1f, 1f);
            }
        }
    }
}

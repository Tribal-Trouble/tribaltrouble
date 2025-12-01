package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.StretchParticle;
import com.oddlabs.tt.render.shader.ParticleShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.vbo.FloatVBO;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.List;

final class LightningRenderer {

    private static final float SQRT_2 = (float) Math.sqrt(2f);
    private static final Vector3f right_vector = new Vector3f();

    private static final Matrix4f view_matrix = new Matrix4f();
    private static final CameraState tmp_camera = new CameraState();

    private static final int MAX_PARTICLES = 1000;
    private static final int FLOATS_PER_PARTICLE = 72; // 8 vertices * 9 floats (x,y,z,u,v,r,g,b,a)

	private static final FloatBuffer particle_buffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * FLOATS_PER_PARTICLE);
    private static final FloatVBO particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());

    private static ParticleShader shader;
    private static final VertexLayout<ParticleShader.Attribute> layout = new VertexLayout<>(
            ParticleShader.Attribute.POSITION,
            ParticleShader.Attribute.TEX_COORD,
            ParticleShader.Attribute.COLOR
    );
    private static final FloatBuffer matrix_buffer = BufferUtils.createFloatBuffer(16);

    public static void render(@NonNull RenderQueues render_queues, @NonNull List<Lightning> emitter_queue, @NonNull CameraState state) {
        if (shader == null) {
            shader = new ParticleShader();
        }

        tmp_camera.set(state);
        view_matrix.identity();
        tmp_camera.setView(view_matrix);
        float rx = tmp_camera.getModelView().m00();
        float ry = tmp_camera.getModelView().m10();
        float rz = tmp_camera.getModelView().m20();
        right_vector.set(rx, ry, rz);

        shader.use();
        state.getModelView().get(matrix_buffer);
        shader.setUniformMatrix4(ParticleShader.Uniforms.MODEL_VIEW_MATRIX, false, matrix_buffer);
        state.getProjectionMatrix().get(matrix_buffer);
        shader.setUniformMatrix4(ParticleShader.Uniforms.PROJECTION_MATRIX, false, matrix_buffer);
        shader.setUniform(ParticleShader.Uniforms.TEXTURE_0, 0);

        // Save GL State
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        // Legacy Alpha test: GL_GREATER, 0. Shader does this implicitly by blending 0 alpha, but pure 0 alpha discard might be needed?
        // For soft particles, usually we don't discard.
        GL11.glDepthMask(false);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D); // Needed? Shader uses sampler. But legacy code enabled it. Core profile doesn't need it. Compatibility might.

        // Bind VBO first so layout pointers refer to it
        particle_vbo.makeCurrent();
        layout.bind(shader);

        for (Lightning emitter : emitter_queue) {
            if (Globals.draw_particles)
                render(render_queues, emitter);
        }
        emitter_queue.clear();

        layout.unbind(shader);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        GL11.glPopAttrib();
        ShaderProgram.unbind();
    }

    private static void render2DParticle(@NonNull StretchParticle particle) {
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

    private static void render(@NonNull RenderQueues render_queues, @NonNull Lightning lightning) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            RenderTools.draw(lightning, 1f, 1f, 1f);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, render_queues.getTexture(lightning.getTexture()).getHandle());
        // Legacy Env Mode MODULATE is handled by shader (Color * Texture).
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		particle_buffer.clear();
        List<StretchParticle> particles = lightning.getParticles();
        for (int i = particles.size() - 1; i >= 0; i--) {
            StretchParticle particle = particles.get(i);
            render2DParticle(particle);
        }
		particle_buffer.flip();
		particle_vbo.put(particle_buffer);

        // Pointers are handled by VertexLayout
		GL11.glDrawArrays(GL11.GL_QUADS, 0, particles.size() * 8);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private LightningRenderer() {
    }
}

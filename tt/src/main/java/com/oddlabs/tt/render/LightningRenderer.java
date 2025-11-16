package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.StretchParticle;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.vbo.FloatVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

    public static void render(@NonNull RenderQueues render_queues, @NonNull List<Lightning> emitter_queue, @NonNull CameraState state) {
        tmp_camera.set(state);
        view_matrix.identity();
        tmp_camera.setView(view_matrix);
        float rx = tmp_camera.getModelView().m00();
        float ry = tmp_camera.getModelView().m10();
        float rz = tmp_camera.getModelView().m20();
        right_vector.set(rx, ry, rz);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GLStateStack.pushState();

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0f);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);

		GLStateStack.switchState(GLState.VERTEX_ARRAY | GLState.TEXCOORD0_ARRAY | GLState.COLOR_ARRAY);

        for (Lightning emitter : emitter_queue) {
            if (Globals.draw_particles)
                render(render_queues, emitter);
        }
        emitter_queue.clear();

        GLStateStack.popState();
        GL11.glPopAttrib();
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
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

		particle_buffer.clear();
        List<StretchParticle> particles = lightning.getParticles();
        for (int i = particles.size() - 1; i >= 0; i--) {
            StretchParticle particle = particles.get(i);
            render2DParticle(particle);
        }
		particle_buffer.flip();
		particle_vbo.put(particle_buffer);

		particle_vbo.vertexPointer(3, 36, 0);
		particle_vbo.texCoordPointer(2, 36, 3);
		particle_vbo.colorPointer(4, 36, 5);
		GL11.glDrawArrays(GL11.GL_QUADS, 0, particles.size() * 8);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
    }

    private LightningRenderer() {
    }
}

package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Particle;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.vbo.FloatVBO;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.List;

final class EmitterRenderer {
	private static final float SQRT_2 = (float)Math.sqrt(2f);
	private static final float ROTATION_FACTOR = 60f;

	private static final Vector3f right_plus_up = new Vector3f();
	private static final Vector3f right_minus_up = new Vector3f();
	private static final FloatBuffer color_buffer = BufferUtils.createFloatBuffer(4);

	private static final Matrix4f view_matrix = new Matrix4f();
	private static final CameraState tmp_camera = new CameraState();

    private static final int MAX_PARTICLES = 10000;
    private static final int FLOATS_PER_PARTICLE = 36; // 4 vertices * 9 floats (x,y,z,u,v,r,g,b,a)

    private static final FloatBuffer particle_buffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * FLOATS_PER_PARTICLE);
    private static final FloatVBO particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());

	public static void render(@NonNull RenderQueues render_queues, @NonNull List<Emitter> emitter_queue, @NonNull CameraState state) {
		tmp_camera.set(state);
		view_matrix.identity();
		tmp_camera.setView(view_matrix);
		float rx = tmp_camera.getModelView().m00(); float ry = tmp_camera.getModelView().m10(); float rz = tmp_camera.getModelView().m20();
		float upx = tmp_camera.getModelView().m01(); float upy = tmp_camera.getModelView().m11(); float upz = tmp_camera.getModelView().m21();
		right_plus_up.set(rx + upx, ry + upy, rz + upz);
		right_minus_up.set(rx - upx, ry - upy, rz - upz);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GLStateStack.pushState();

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0f);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glDepthMask(false);

		GLStateStack.switchState(GLState.VERTEX_ARRAY | GLState.TEXCOORD0_ARRAY | GLState.COLOR_ARRAY);

        for (Emitter emitter : emitter_queue) {
            if (Globals.draw_particles)
                render(render_queues, emitter);
        }
		emitter_queue.clear();

		GLStateStack.popState();
		GL11.glPopAttrib();
	}

	private static void render2DParticle(@NonNull Particle particle, @NonNull Emitter emitter) {
		float x = particle.getPosX();
		float y = particle.getPosY();
		float z = particle.getPosZ();
		float radius_x = particle.getRadiusX()*emitter.getScaleX();
		float radius_y = particle.getRadiusY()*emitter.getScaleY();
		float radius_z = particle.getRadiusZ()*emitter.getScaleZ();

		float r = particle.getColorR();
		float g = particle.getColorG();
		float b = particle.getColorB();
		float a = particle.getColorA();

		particle_buffer.put(x - right_plus_up.x*radius_x).put(y - right_plus_up.y*radius_y).put(z - right_plus_up.z*radius_z).put(particle.getU1()).put(particle.getV1()).put(r).put(g).put(b).put(a);
		particle_buffer.put(x + right_minus_up.x*radius_x).put(y + right_minus_up.y*radius_y).put(z + right_minus_up.z*radius_z).put(particle.getU2()).put(particle.getV2()).put(r).put(g).put(b).put(a);
		particle_buffer.put(x + right_plus_up.x*radius_x).put(y + right_plus_up.y*radius_y).put(z + right_plus_up.z*radius_z).put(particle.getU3()).put(particle.getV3()).put(r).put(g).put(b).put(a);
		particle_buffer.put(x - right_minus_up.x*radius_x).put(y - right_minus_up.y*radius_y).put(z - right_minus_up.z*radius_z).put(particle.getU4()).put(particle.getV4()).put(r).put(g).put(b).put(a);
	}

	private static void render(@NonNull RenderQueues render_queues, @NonNull Emitter emitter) {
		if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
			RenderTools.draw(emitter, 1f, 1f, 1f);
		}

		TextureKey[] textures = emitter.getTextures();
		List<Particle>[] particles = emitter.getParticles();
		SpriteKey[] sprite_renderers = emitter.getSpriteRenderers();
		if (textures != null) {
			GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
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

				particle_vbo.vertexPointer(3, 36, 0);
				particle_vbo.texCoordPointer(2, 36, 3);
				particle_vbo.colorPointer(4, 36, 5);
				GL11.glDrawArrays(GL11.GL_QUADS, 0, particles[j].size() * 4);
			}

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
		} else if (sprite_renderers != null) {
                    for (List<Particle> particle1 : particles) {
                        for (int i = particle1.size() - 1; i >= 0; i--) {
                            Particle particle = particle1.get(i);
                            int index = particle.getType();
                            color_buffer.put(0, particle.getColorR());
                            color_buffer.put(1, particle.getColorG());
                            color_buffer.put(2, particle.getColorB());
                            color_buffer.put(3, Math.min(particle.getColorA(), 1f));
                            SpriteRenderer sprite_renderer = render_queues.getRenderer(sprite_renderers[index]);
                            sprite_renderer.setupWithColor(0, color_buffer, false, false);
                            //					sprite_renderer.setup(0, false);
                            float x = particle.getPosX();
                            float y = particle.getPosY();
                            float z = particle.getPosZ();
                            GL11.glPushMatrix();
                            GL11.glTranslatef(x, y, z);
                            GL11.glRotatef(ROTATION_FACTOR*(y + x), SQRT_2, SQRT_2, 0f);
                            //					GL11.glScalef(scale_x, scale_y, scale_z);
                            sprite_renderer.getSpriteList().render(0, 0, 0f);
                            sprite_renderer.getSpriteList().reset(0, false, false);
                            GL11.glPopMatrix();
                        }
                    }
		}
	}

    private EmitterRenderer() {
    }
}

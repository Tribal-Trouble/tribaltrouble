package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Particle;
import com.oddlabs.tt.render.shader.ParticleShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EmitterRenderer {

    private static final int MAX_PARTICLES = 10000;
    private static final VertexLayout<ParticleShader.Attribute> VERTEX_LAYOUT = new VertexLayout<>(
            ParticleShader.Attribute.CENTER_POSITION,
            ParticleShader.Attribute.SIZE,
            ParticleShader.Attribute.COLOR,
            ParticleShader.Attribute.UV_INFO
    );

    private final FloatBuffer particle_buffer;
    private final FloatVBO particle_vbo;

    private final ParticleShader shader = new ParticleShader();

    private final @NonNull VertexArray vao;
    private int vbo_offset = 0;

    private record BatchEntry(@NonNull Emitter emitter, @NonNull List<@NonNull Particle> particles) {}
    private final Map<@NonNull Texture, @NonNull List<@NonNull BatchEntry>> texture_batches = new HashMap<>();

    public EmitterRenderer() {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;
        particle_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(MAX_PARTICLES * floatsPerParticle));
        particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());
        
        vao = new VertexArray();
        vao.bind();
        particle_vbo.makeCurrent();
        VERTEX_LAYOUT.bind(shader);
        vao.unbind();
    }

    public void render(@NonNull RenderQueues render_queues, @NonNull List<Emitter> emitter_queue, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (emitter_queue.isEmpty()) return;

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMaskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        texture_batches.clear();

        try (var _ = shader.use();
             var _ = state.getFog().setup(shader, state.getCurrentZ())) {

            if (!blendEnabled) GL11.glEnable(GL11.GL_BLEND);
            if (depthMaskEnabled) GL11.glDepthMask(false);
            
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            shader.setUniformMatrix4(ParticleShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniformMatrix4(ParticleShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniform(ParticleShader.Uniforms.TEXTURE_0, 0);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            vao.bind();

            for (Emitter emitter : emitter_queue) {
                if (Globals.draw_particles)
                    collectParticles(render_queues, emitter, state, modelViewStack, projectionStack);
            }

            flushBatches();

        } finally {
            vao.unbind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
            if (depthMaskEnabled) GL11.glDepthMask(true);
        }
    }

    private void renderParticle(@NonNull Particle particle, @NonNull Emitter emitter) {
        particle_buffer.put(particle.getPosX()).put(particle.getPosY()).put(particle.getPosZ()); // Center Position
        particle_buffer.put(particle.getRadiusX() * emitter.getScaleX()).put(particle.getRadiusY() * emitter.getScaleY()); // Size
        particle_buffer.put(particle.getColorR()).put(particle.getColorG()).put(particle.getColorB()).put(Math.min(particle.getColorA(), 1.0f)); // Color
        particle_buffer.put(particle.getU1()).put(particle.getV1()).put(particle.getU2()).put(particle.getV3()); // UV Info
    }

    private void collectParticles(@NonNull RenderQueues render_queues, @NonNull Emitter emitter, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        TextureKey[] textures = emitter.getTextures();
        List<@NonNull Particle>[] particles = emitter.getParticles();
        SpriteKey[] sprite_renderers = emitter.getSpriteRenderers();
        
        if (textures != null) {
            for (int j = 0; j < particles.length; j++) {
                if (particles[j].isEmpty()) continue;
                Texture texture = render_queues.getTexture(textures[j]);
                texture_batches.computeIfAbsent(texture, k -> new ArrayList<>()).add(new BatchEntry(emitter, particles[j]));
            }
        } else if (sprite_renderers != null) {
            for (int j = 0; j < particles.length; j++) {
                SpriteRenderer renderer = render_queues.getRenderer(sprite_renderers[j]);
                for (Particle particle : particles[j]) {
                    renderer.addToRenderList(PolyDetail.LOW_POLY, new ParticleModelState(particle, modelViewStack), false);
                }
            }
        }
    }

    private void flushBatches() {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;

        for (Map.Entry<Texture, List<BatchEntry>> entry : texture_batches.entrySet()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, entry.getKey().getHandle());
            particle_buffer.clear();
            int particleCount = 0;

            for (BatchEntry batch : entry.getValue()) {
                List<Particle> particles = batch.particles();
                Emitter emitter = batch.emitter();

                // Iterate backwards as per original logic (maybe for correct depth order within the list?)
                for (int i = particles.size() - 1; i >= 0; i--) {
                    if (particle_buffer.remaining() < floatsPerParticle) {
                        flush(particleCount);
                        particle_buffer.clear();
                        particleCount = 0;
                    }
                    renderParticle(particles.get(i), emitter);
                    particleCount++;
                }
            }
            flush(particleCount);
        }
    }
    
    private void flush(int particleCount) {
        if (particleCount == 0) return;
        particle_buffer.flip();
        
        if (vbo_offset + particleCount > MAX_PARTICLES) {
            particle_vbo.orphan();
            vbo_offset = 0;
        }
        
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;
        particle_vbo.putSubData(vbo_offset * floatsPerParticle, particle_buffer);
        GL11.glDrawArrays(GL11.GL_POINTS, vbo_offset, particleCount);
        
        vbo_offset += particleCount;
    }

    public void debugRender(@NonNull List<@NonNull Emitter> emitter_queue) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (Emitter emitter : emitter_queue) {
                RenderTools.draw(emitter, 1f, 1f, 1f);
            }
        }
    }
}

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
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public final class EmitterRenderer implements AutoCloseable {
    private static final int MAX_PARTICLES = 10000;
    private static final VertexLayout<ParticleShader.Attribute> VERTEX_LAYOUT = new VertexLayout<>(
            ParticleShader.Attribute.CENTER_POSITION,
            ParticleShader.Attribute.SIZE,
            ParticleShader.Attribute.COLOR,
            ParticleShader.Attribute.UV_COORDS_1,
            ParticleShader.Attribute.UV_COORDS_2
    );

    private final @NonNull FloatBuffer particle_buffer;
    private final @NonNull FloatVBO particle_vbo;

    private final ParticleShader shader = new ParticleShader();

    private final VertexArray vao = new VertexArray();
    private int vbo_offset = 0;

    private record BatchKey(@NonNull Texture texture, int srcBlend, int dstBlend) {}
    private record BatchEntry<P extends Particle>(@NonNull Emitter<P> emitter, @NonNull List<@NonNull P> particles) {}
    /** LinkedHashMap so that insertion order matches drawing order. Better sorting may be needed. */
    private final Map<@NonNull BatchKey, @NonNull List<@NonNull BatchEntry<?>>> batches = new LinkedHashMap<>();

    public EmitterRenderer() {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;
        particle_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(MAX_PARTICLES * floatsPerParticle));
        particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());
        
        vao.bind();
        particle_vbo.makeCurrent();
        VERTEX_LAYOUT.bind(shader);
        vao.unbind();
    }

    public void render(@NonNull RenderQueues render_queues, @NonNull Queue<Emitter<?>> emitter_queue, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (emitter_queue.isEmpty()) return;

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthMaskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        batches.clear();

        try (var _ = shader.use();
             var _ = state.getFog().setup(shader, state.getCurrentZ())) {

            if (!blendEnabled) GL11.glEnable(GL11.GL_BLEND);
            if (depthMaskEnabled) GL11.glDepthMask(false);
            
            shader.setUniformMatrix4(ParticleShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniformMatrix4(ParticleShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            shader.setUniform(ParticleShader.Uniforms.TEXTURE_0, 0);
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            vao.bind();

            for (Emitter<?> emitter : emitter_queue) {
                if (Globals.draw_particles)
                    collectParticles(render_queues, emitter, state, modelViewStack, projectionStack);
            }

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0f, -1.0f);
            flushBatches();
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        } finally {
            vao.unbind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
            if (depthMaskEnabled) GL11.glDepthMask(true);
        }
    }

    private <P extends Particle> void renderParticle(@NonNull P particle, @NonNull Emitter<P> emitter) {
        particle_buffer.put(particle.getPosX()).put(particle.getPosY()).put(particle.getPosZ()); // Center Position
        particle_buffer.put(particle.getRadiusX() * emitter.getScaleX()).put(particle.getRadiusY() * emitter.getScaleY()).put(particle.getRadiusZ() * emitter.getScaleZ()); // Size (3D)
        particle_buffer.put(particle.getColorR()).put(particle.getColorG()).put(particle.getColorB()).put(Math.min(particle.getColorA(), 1.0f)); // Color
        // UV Info 1: u1, v1, u2, v2
        particle_buffer.put(particle.getU1()).put(particle.getV1()).put(particle.getU2()).put(particle.getV2());
        // UV Info 2: u3, v3, u4, v4
        particle_buffer.put(particle.getU3()).put(particle.getV3()).put(particle.getU4()).put(particle.getV4());
    }

    private <P extends Particle> void collectParticles(@NonNull RenderQueues render_queues, @NonNull Emitter<P> emitter, @NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        TextureKey[] textures = emitter.getTextures();
        List<@NonNull P>[] particles = emitter.getParticles();
        SpriteKey[] sprite_renderers = emitter.getSpriteRenderers();
        
        if (textures != null) {
            for (int j = 0; j < particles.length; j++) {
                if (particles[j].isEmpty()) continue;
                Texture texture = render_queues.getTexture(textures[j]);
                BatchKey key = new BatchKey(texture, emitter.getSrcBlendFunc(), emitter.getDstBlendFunc());
                batches.computeIfAbsent(key, k -> new ArrayList<>()).add(new BatchEntry<>(emitter, particles[j]));
            }
        } else if (sprite_renderers != null) {
            for (int j = 0; j < particles.length; j++) {
                SpriteRenderer renderer = render_queues.getRenderer(sprite_renderers[j]);
                for (Particle particle : particles[j]) {
                    renderer.addToRenderList(PolyDetail.LOW_POLY, new ParticleModelState(particle, state.getModelView()), false);
                }
            }
        }
    }

    private void flushBatches() {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;

        for (var entry : batches.entrySet()) {
            BatchKey key = entry.getKey();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, key.texture().getHandle());
            GL11.glBlendFunc(key.srcBlend(), key.dstBlend());
            shader.setUniform(ParticleShader.Uniforms.IS_ADDITIVE, key.dstBlend() == GL11.GL_ONE ? 1.0f : 0.0f);
            particle_buffer.clear();
            int particleCount = 0;

            for (var batch : entry.getValue()) {
                particleCount = processBatch(batch, floatsPerParticle, particleCount);
            }
            flush(particleCount);
        }
    }

    private <P extends Particle> int processBatch(@NonNull BatchEntry<P> batch, int floatsPerParticle, int particleCount) {
        var particles = batch.particles();
        var emitter = batch.emitter();

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
        return particleCount;
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

    public void debugRender(@NonNull Queue<@NonNull Emitter<?>> emitter_queue) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (Emitter<?> emitter : emitter_queue) {
                RenderTools.draw(emitter, 1f, 1f, 1f);
            }
        }
    }

    @Override
    public void close() {
        vao.close();
        particle_vbo.close();
        shader.close();
    }
}

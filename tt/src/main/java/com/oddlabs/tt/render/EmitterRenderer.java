package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Particle;
import com.oddlabs.tt.render.shader.ParticleShader;
import com.oddlabs.tt.render.shader.VertexLayout;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Specialized renderer for handling particle emitters.
 */
public final class EmitterRenderer implements AutoCloseable {
    private static final int MAX_PARTICLES = 50000;
    private static final VertexLayout<ParticleShader.Attribute> VERTEX_LAYOUT = new VertexLayout<>(
            ParticleShader.Attribute.CENTER_POSITION,
            ParticleShader.Attribute.SIZE,
            ParticleShader.Attribute.COLOR,
            ParticleShader.Attribute.UV_COORDS_1,
            ParticleShader.Attribute.UV_COORDS_2,
            ParticleShader.Attribute.TEX_SLOT
    );

    private final @NonNull FloatBuffer particle_buffer;
    private final @NonNull FloatVBO particle_vbo;

    private final ParticleShader shader = new ParticleShader();

    private final VertexArray vao = new VertexArray();
    private int vbo_offset = 0;

    private record BatchKey(int srcBlend, int dstBlend, boolean fogEnabled) {
    }

    private record BatchEntry<P extends Particle>(@NonNull Emitter<P> emitter, @NonNull List<@NonNull P> particles,
                                                  @NonNull Texture texture) {
    }

    /**
     * Grouping by blend modes. Inside each blend mode group, we will batch.
     */
    private final Map<@NonNull BatchKey, @NonNull List<@NonNull BatchEntry<?>>> batches = new LinkedHashMap<>();

    public EmitterRenderer() {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;
        particle_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(MAX_PARTICLES * floatsPerParticle));
        particle_vbo = new FloatVBO(GL15.GL_STREAM_DRAW, particle_buffer.capacity());

        vao.bind();
        particle_vbo.bind();
        VERTEX_LAYOUT.bind(shader);

        // Configure all attributes as instance attributes
        for (ParticleShader.Attribute attr : ParticleShader.Attribute.values()) {
            int loc = shader.getAttributeLocation(attr.getName());
            if (loc >= 0) {
                GL33.glVertexAttribDivisor(loc, 1);
            }
        }

        vao.unbind();
    }

    public void clear() {
        batches.clear();
    }

    public void prepare(@NonNull RenderQueues render_queues, @NonNull Queue<? extends Emitter<?>> emitters,
            @NonNull CameraState state, @NonNull MatrixStack modelViewStack) {
        clear();
        if (Globals.draw_particles)
            for (Emitter<?> emitter : emitters) {
                collectParticles(render_queues, emitter, state, modelViewStack);
            }
    }

    public void render(@NonNull RenderContext context, @NonNull RenderQueues render_queues, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack, @NonNull Texture depthTexture) {
        if (batches.isEmpty()) return;

        // Reset offset and orphan at start of frame to prevent flickering
        vbo_offset = 0;
        particle_vbo.orphan();

        vao.bind();
        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.ALPHA); var _ = context.withDepthMode(
                DepthMode.READ_ONLY)) {

            shader.setUniform(ParticleShader.Uniforms.MODEL_VIEW_MATRIX, modelViewStack.current());

            // Bind global effect texture array to unit 2
            context.setTexture(2, render_queues.getEffectTextureArray().getHandle(),
                    render_queues.getEffectTextureArray().getTarget());
            shader.setUniform(ParticleShader.Uniforms.TEXTURE_ARRAY, 2);

            context.setActiveTexture(1);
            context.setTexture(1, depthTexture.getHandle());
            shader.setUniform(ParticleShader.Uniforms.DEPTH_MAP, 1);

            shader.setUniform(ParticleShader.Uniforms.NEAR_FAR, Globals.VIEW_MIN, Globals.VIEW_MAX);
            shader.setUniform(ParticleShader.Uniforms.SOFT_RANGE, 2.0f); // Adjust default as needed

            flushBatches(context);
        } finally {
            vao.unbind();
            context.setTexture(1, 0);
            context.setTexture(2, 0);
        }
    }

    private <P extends Particle> void renderParticle(@NonNull P particle, @NonNull Emitter<P> emitter, float layer) {
        particle_buffer.put(particle.getPosX()).put(particle.getPosY()).put(particle.getPosZ()); // World Position
        particle_buffer.put(particle.getRadiusX() * emitter.getScaleX()).put(particle.getRadiusY() * emitter
                .getScaleY()).put(particle.getRadiusZ() * emitter.getScaleZ()); // Size (3D)

        particle_buffer.put(particle.getColorR()).put(particle.getColorG()).put(particle.getColorB())
                .put(particle.getColorA());

        // UV Info 1: u1, v1, u2, v2
        particle_buffer.put(particle.getU1()).put(particle.getV1()).put(particle.getU2()).put(particle.getV2());
        // UV Info 2: u3, v3, u4, v4
        particle_buffer.put(particle.getU3()).put(particle.getV3()).put(particle.getU4()).put(particle.getV4());

        particle_buffer.put(layer);
    }

    private <P extends Particle> void collectParticles(@NonNull RenderQueues render_queues, @NonNull Emitter<P> emitter,
            @NonNull CameraState state, @NonNull MatrixStack modelViewStack) {
        TextureKey[] textures = emitter.getTextures();
        List<@NonNull P>[] particles = emitter.getParticles();
        SpriteKey[] sprite_renderers = emitter.getSpriteRenderers();

        if (textures != null) {
            for (int j = 0; j < particles.length; j++) {
                if (particles[j].isEmpty()) continue;
                Texture texture = render_queues.getTexture(textures[j]);
                BatchKey key = new BatchKey(emitter.getSrcBlendFunc(), emitter.getDstBlendFunc(), emitter
                        .isFogEnabled());
                batches.computeIfAbsent(key, k -> new ArrayList<>()).add(new BatchEntry<>(emitter, particles[j],
                        texture));
            }
        } else if (sprite_renderers != null) {
            for (int j = 0; j < particles.length; j++) {
                SpriteRenderer renderer = render_queues.getRenderer(sprite_renderers[j]);
                for (Particle particle : particles[j]) {
                    // Sprite path needs the actual View matrix for billboarding.
                    // Must clone it because the stack will be mutated before the render pass.
                    Matrix4f viewMatrix = new Matrix4f(modelViewStack.current());
                    renderer.addToRenderList(PolyDetail.LOW_POLY, new ParticleModelState(particle, viewMatrix), false);
                }
            }
        }
    }

    private void flushBatches(@NonNull RenderContext context) {
        int floatsPerParticle = VERTEX_LAYOUT.getStride() / Float.BYTES;

        for (var entry : batches.entrySet()) {
            BatchKey key = entry.getKey();
            context.setBlendFunc(key.srcBlend(), key.dstBlend());
            shader.setUniform(ParticleShader.Uniforms.IS_ADDITIVE, key.dstBlend() == GL11.GL_ONE ? 1.0f : 0.0f);
            shader.setUniform(ParticleShader.Uniforms.FOG_ENABLED, key.fogEnabled());

            var batchEntries = entry.getValue();
            particle_buffer.clear();
            int particleCount = 0;

            for (var batchEntry : batchEntries) {
                float layer = (float) batchEntry.texture.getLayer();
                particleCount = processBatchEntry(batchEntry, layer, particleCount, floatsPerParticle);
            }
            flush(particleCount);
        }
    }

    private <P extends Particle> int processBatchEntry(@NonNull BatchEntry<P> batch, float layer, int particleCount,
            int floatsPerParticle) {
        var particles = batch.particles();
        var emitter = batch.emitter();

        // Iterate backwards as per original logic
        for (int i = particles.size() - 1; i >= 0; i--) {
            if (particleCount >= MAX_PARTICLES || particle_buffer.remaining() < floatsPerParticle) {
                flush(particleCount);
                particle_buffer.clear();
                particleCount = 0;
            }
            renderParticle(particles.get(i), emitter, layer);
            particleCount++;
        }
        return particleCount;
    }

    private void flush(int particleCount) {
        if (particleCount == 0) return;
        particle_buffer.flip();

        if (vbo_offset + particleCount > MAX_PARTICLES) {
            // This case should be rare since we reset at start of frame
            particle_vbo.orphan();
            vbo_offset = 0;
        }

        int stride = VERTEX_LAYOUT.getStride();
        int floatsPerParticle = stride / Float.BYTES;
        particle_vbo.putSubData(vbo_offset * floatsPerParticle, particle_buffer);

        // Shifting attribute pointers to account for vbo_offset since we use instanced rendering
        for (ParticleShader.Attribute attr : ParticleShader.Attribute.values()) {
            int loc = shader.getAttributeLocation(attr.getName());
            if (loc >= 0) {
                attr.setPointer(loc, stride, VERTEX_LAYOUT.getOffset(attr) + vbo_offset * stride);
            }
        }

        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, particleCount);

        vbo_offset += particleCount;
    }

    public void debugRender(@NonNull Queue<@NonNull Emitter<?>> emitter_queue) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (Emitter<?> emitter : emitter_queue) {
                emitter.debugRender();
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

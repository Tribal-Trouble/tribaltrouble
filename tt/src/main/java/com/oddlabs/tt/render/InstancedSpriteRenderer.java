package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.InstancedSpriteShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized renderer that handles high-performance 3D sprite rendering using hardware instancing.
 * Batches sprites by texture and render state to minimize draw calls and state changes.
 */
public final class InstancedSpriteRenderer implements AutoCloseable {

    private final InstancedSpriteShader shader = new InstancedSpriteShader();
    private final Map<@NonNull BatchKey, @NonNull RenderBatch> batches = new HashMap<>();
    private final @NonNull Texture whiteTexture;

    public InstancedSpriteRenderer() {
        GLImage whiteImage = new GLIntImage(1, 1, GL11.GL_RGBA);
        whiteImage.putPixel(0, 0, Color.WHITE_INT);
        whiteTexture = new Texture(new GLImage[]{whiteImage}, GL11.GL_RGBA8, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
    }

    @NonNull
    Texture getWhiteTexture() {
        return whiteTexture;
    }

    public void add(@NonNull SpriteList spriteList, int spriteIndex, int animation, float animTicks,
            @NonNull Texture texture, @Nullable Texture teamTexture, @Nullable Texture bumpTexture,
            boolean respond, boolean blend, boolean depthWrite, boolean depthTest, @NonNull Matrix4f modelMatrix,
            @NonNull Color color, @NonNull Color decalColor) {
        Sprite sprite = spriteList.getSprite(spriteIndex);
        Sprite.FrameState frameState = sprite.getAnimationState(animation, animTicks);

        BatchKey key = new BatchKey(spriteList, texture, teamTexture, bumpTexture, respond, blend,
                depthWrite, depthTest);
        RenderBatch batch = batches.computeIfAbsent(key, RenderBatch::new);
        batch.addInstance(spriteIndex, frameState.pos1(), frameState.norm1(), frameState.pos2(), frameState.norm2(),
                frameState
                        .tween(), modelMatrix, color, decalColor);
    }

    public void renderAll(@NonNull RenderContext context, @NonNull CameraState cameraState,
            @NonNull MatrixStack projectionStack) {
        if (batches.isEmpty()) return;

        try (var _ = shader.use()) {
            // Set TBO texture unit
            shader.setUniform(InstancedSpriteShader.Uniforms.VERT_BUFFER, 5);

            RenderState state = new RenderState();
            List<RenderBatch> sortedBatches = new ArrayList<>(batches.values());
            sortedBatches.sort(RenderBatch.COMPARATOR);

            for (RenderBatch batch : sortedBatches) {
                batch.render(context, shader, whiteTexture, state);
            }
        } finally {
            // Restore default state to prevent leakage to other renderers (Sky, Landscape, etc.)
            context.bindVertexArray(0);
            context.setDepthMode(DepthMode.READ_WRITE);
            context.setBlendMode(BlendMode.NONE);
            context.setCullMode(CullMode.BACK);
            context.setSampleAlphaToCoverage(false);
            context.setDrawBuffers(true);
            context.setColorMask(true, true, true, true);
            clear();
        }
    }

    public void clear() {
        for (RenderBatch batch : batches.values()) {
            batch.clear();
        }
    }

    @Override
    public void close() {
        for (RenderBatch batch : batches.values()) {
            batch.close();
        }
        batches.clear();
        shader.close();
        whiteTexture.close();
    }

    private record BatchKey(@NonNull SpriteList spriteList, @NonNull Texture texture,
                            @Nullable Texture teamTexture, @Nullable Texture bumpTexture, boolean respond,
                            boolean blend, boolean depthWrite, boolean depthTest) {
    }

    private static class RenderState {
        int boundTBO = -1;
    }

    private static class RenderBatch implements AutoCloseable {
        private final @NonNull BatchKey key;
        private final @NonNull Map<@NonNull Integer, @NonNull InstanceGroup> groups = new HashMap<>();

        // mat4 (16) + color (4) + decalColor (4) + pos1(1) + norm1(1) + pos2(1) + norm2(1) + tween (1)
        private static final int FLOATS_PER_INSTANCE = 16 + 4 + 4 + 1 + 1 + 1 + 1 + 1;

        private static class InstanceGroup implements AutoCloseable {
            private final int spriteIndex;
            private final @NonNull SpriteList spriteList;
            private FloatVBO vbo;
            private final @NonNull VertexArray vao;
            private @NonNull FloatBuffer buffer;
            private int count = 0;
            private int capacity = 32;

            InstanceGroup(int spriteIndex, @NonNull BatchKey key, int floatsPerInstance) {
                this.spriteIndex = spriteIndex;
                this.spriteList = key.spriteList;
                this.buffer = BufferUtils.createFloatBuffer(capacity * floatsPerInstance);
                this.vbo = new FloatVBO(GL15.GL_STREAM_DRAW, capacity * floatsPerInstance);

                this.vao = new VertexArray();
                vao.bind();

                ShortVBO ibo = spriteList.getIndices();
                FloatVBO texCoordVBO = spriteList.getTexcoords();

                ibo.bind();
                texCoordVBO.bind();

                GL20.glEnableVertexAttribArray(2); // TexCoord
                Sprite sprite = spriteList.getSprite(spriteIndex);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 0, sprite.texcoords_offset * 4L);

                setupInstanceAttributes();

                vao.unbind();
            }

            private void setupInstanceAttributes() {
                vbo.bind();
                int instanceStride = FLOATS_PER_INSTANCE * Float.BYTES;

                // Model Matrix (Locations 4-7)
                for (int i = 0; i < 4; i++) {
                    int loc = 4 + i;
                    GL20.glEnableVertexAttribArray(loc);
                    GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, instanceStride, (long) i * 4
                            * Float.BYTES);
                    GL33.glVertexAttribDivisor(loc, 1);
                }

                // Color (Location 8)
                int colorLoc = 8;
                GL20.glEnableVertexAttribArray(colorLoc);
                GL20.glVertexAttribPointer(colorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 16 * Float.BYTES);
                GL33.glVertexAttribDivisor(colorLoc, 1);

                // Decal Color (Location 9)
                int decalColorLoc = 9;
                GL20.glEnableVertexAttribArray(decalColorLoc);
                GL20.glVertexAttribPointer(decalColorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 20 * Float.BYTES);
                GL33.glVertexAttribDivisor(decalColorLoc, 1);

                // Animation Offsets & Tween (Locations 10, 11, 12, 13, 14)
                int pos1Loc = 10;
                GL20.glEnableVertexAttribArray(pos1Loc);
                GL20.glVertexAttribPointer(pos1Loc, 1, GL11.GL_FLOAT, false, instanceStride, 24 * Float.BYTES);
                GL33.glVertexAttribDivisor(pos1Loc, 1);

                int norm1Loc = 11;
                GL20.glEnableVertexAttribArray(norm1Loc);
                GL20.glVertexAttribPointer(norm1Loc, 1, GL11.GL_FLOAT, false, instanceStride, 25 * Float.BYTES);
                GL33.glVertexAttribDivisor(norm1Loc, 1);

                int pos2Loc = 12;
                GL20.glEnableVertexAttribArray(pos2Loc);
                GL20.glVertexAttribPointer(pos2Loc, 1, GL11.GL_FLOAT, false, instanceStride, 26 * Float.BYTES);
                GL33.glVertexAttribDivisor(pos2Loc, 1);

                int norm2Loc = 13;
                GL20.glEnableVertexAttribArray(norm2Loc);
                GL20.glVertexAttribPointer(norm2Loc, 1, GL11.GL_FLOAT, false, instanceStride, 27 * Float.BYTES);
                GL33.glVertexAttribDivisor(norm2Loc, 1);

                int tweenLoc = 14;
                GL20.glEnableVertexAttribArray(tweenLoc);
                GL20.glVertexAttribPointer(tweenLoc, 1, GL11.GL_FLOAT, false, instanceStride, 28 * Float.BYTES);
                GL33.glVertexAttribDivisor(tweenLoc, 1);
            }

            void add(int pos1, int norm1, int pos2, int norm2, float tween, @NonNull Matrix4f modelMatrix,
                    @NonNull Color color, @NonNull Color decalColor) {
                if (count >= capacity) {
                    int newCapacity = capacity * 2;
                    FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity * FLOATS_PER_INSTANCE);
                    buffer.position(0);
                    buffer.limit(count * FLOATS_PER_INSTANCE);
                    newBuffer.put(buffer);
                    buffer = newBuffer;

                    vbo.close();
                    vbo = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity * FLOATS_PER_INSTANCE);
                    vbo.orphan();

                    vao.bind();
                    setupInstanceAttributes();
                    vao.unbind();

                    capacity = newCapacity;
                }

                int base = count * FLOATS_PER_INSTANCE;
                modelMatrix.get(base, buffer);
                color.get(base + 16, buffer);
                decalColor.get(base + 20, buffer);

                buffer.put(base + 24, (float) pos1);
                buffer.put(base + 25, (float) norm1);
                buffer.put(base + 26, (float) pos2);
                buffer.put(base + 27, (float) norm2);
                buffer.put(base + 28, tween);

                count++;
            }

            void upload(@NonNull RenderContext context) {
                vbo.bind(context);
                vbo.orphan();
                buffer.limit(count * FLOATS_PER_INSTANCE).position(0);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);
            }

            void draw(@NonNull RenderContext context) {
                vao.bind(context);
                Sprite sprite = spriteList.getSprite(spriteIndex);
                context.setCullMode(sprite.culled ? CullMode.BACK : CullMode.NONE);
                GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, sprite.getTriangleCount() * 3, GL11.GL_UNSIGNED_SHORT,
                        (long) sprite.indices_offset * Short.BYTES, count);
            }

            void clear() {
                count = 0;
                buffer.clear();
            }

            @Override
            public void close() {
                vao.close();
                vbo.close();
            }
        }

        private static final Comparator<@NonNull RenderBatch> COMPARATOR = Comparator
                .comparing((RenderBatch b) -> b.key.blend)
                .thenComparingInt(b -> b.key.texture.getHandle())
                .thenComparingInt(b -> b.key.spriteList.getTBOTextureHandle())
                .thenComparingInt(b -> b.key.teamTexture != null ? b.key.teamTexture.getHandle() : 0)
                .thenComparingInt(b -> b.key.bumpTexture != null ? b.key.bumpTexture.getHandle() : 0);

        RenderBatch(@NonNull BatchKey key) {
            this.key = key;
        }

        void addInstance(int spriteIndex, int pos1, int norm1, int pos2, int norm2, float tween,
                @NonNull Matrix4f modelMatrix, @NonNull Color color, @NonNull Color decalColor) {
            InstanceGroup group = groups.computeIfAbsent(spriteIndex, k -> new InstanceGroup(k, key,
                    FLOATS_PER_INSTANCE));
            group.add(pos1, norm1, pos2, norm2, tween, modelMatrix, color, decalColor);
        }

        void render(@NonNull RenderContext context, @NonNull InstancedSpriteShader shader, Texture whiteTexture,
                @NonNull RenderState state) {
            boolean hasInstances = false;
            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    hasInstances = true;
                    break;
                }
            }
            if (!hasInstances) return;

            InstanceGroup representativeGroup = null;
            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    representativeGroup = group;
                    break;
                }
            }
            if (representativeGroup == null) return;

            SpriteList spriteList = key.spriteList;
            Sprite representativeSprite = spriteList.getSprite(representativeGroup.spriteIndex);
            setupTextures(context, shader, representativeSprite, whiteTexture, state);

            context.setActiveTexture(5);
            if (state.boundTBO != spriteList.getTBOTextureHandle()) {
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, spriteList.getTBOTextureHandle());
                state.boundTBO = spriteList.getTBOTextureHandle();
            }

            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    group.upload(context);
                }
            }

            if (key.respond) {
                try (var _ = context.withColorMask(false, false, false, false); var _ = context.withDepthMode(
                        DepthMode.READ_WRITE); var _ = context.withDepthFunc(GL11.GL_LEQUAL); var _ = context
                                .withBlendMode(BlendMode.NONE); var _ = context.withSampleAlphaToCoverage(false); var _
                                        = context.withDrawBuffers(false)) {
                    drawAll(context);
                }

                try (var _ = context.withColorMask(true, true, true, true); var _ = context.withDepthMode(
                        DepthMode.READ_ONLY); var _ = context.withDepthFunc(GL11.GL_EQUAL); var _ = context
                                .withBlendMode(BlendMode.ALPHA); var _ = context.withSampleAlphaToCoverage(false); var _
                                        = context.withDrawBuffers(true)) {
                    drawAll(context);
                }
            } else {
                context.setDepthMode(key.depthTest ? key.depthWrite ? DepthMode.READ_WRITE : DepthMode.READ_ONLY
                        : DepthMode.NONE);

                if (key.blend) {
                    context.setBlendMode(BlendMode.ALPHA);
                    context.setSampleAlphaToCoverage(false);
                } else {
                    context.setBlendMode(BlendMode.NONE);
                    context.setSampleAlphaToCoverage(true);
                }
                drawAll(context);
            }
        }

        private void drawAll(@NonNull RenderContext context) {
            for (InstanceGroup group : groups.values()) {
                if (group.count > 0) {
                    group.draw(context);
                }
            }
        }

        private void setupTextures(@NonNull RenderContext context, @NonNull InstancedSpriteShader shader,
                @NonNull Sprite sprite, Texture whiteTexture, @NonNull RenderState state) {
            context.setTexture(0, key.texture);
            shader.setUniform(InstancedSpriteShader.Uniforms.TEXTURE_0, 0);

            boolean useLighting = Globals.draw_light && sprite.lighted;
            shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_LIGHTING, useLighting);
            shader.setUniform(InstancedSpriteShader.Uniforms.REPLACE_MODE, !useLighting && !sprite.modulate_color);
            shader.setUniform(InstancedSpriteShader.Uniforms.DESATURATE, key.respond ? 0.5f : 0.0f);

            if (sprite.modulate_color) {
                shader.setUniform(InstancedSpriteShader.Uniforms.MODULATE_COLOR, true);
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
                shader.setUniform(InstancedSpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.0f);
            } else {
                shader.setUniform(InstancedSpriteShader.Uniforms.MODULATE_COLOR, false);
                shader.setUniform(InstancedSpriteShader.Uniforms.ALPHA_TEST_VALUE, key.respond ? 0.5f : 0.1f);
                if (key.teamTexture != null || key.respond) {
                    shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, true);
                    Texture teamTexture = key.respond ? sprite.respond_texture : key.teamTexture;
                    context.setTexture(1, teamTexture);
                    shader.setUniform(InstancedSpriteShader.Uniforms.TEXTURE_1, 1);
                } else {
                    shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
                }
            }

            if (key.bumpTexture != null) {
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_NORMAL_MAP, true);
                context.setTexture(2, key.bumpTexture);
                shader.setUniform(InstancedSpriteShader.Uniforms.NORMAL_MAP, 2);
            } else {
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_NORMAL_MAP, false);
            }
        }

        void clear() {
            groups.values().forEach(InstanceGroup::clear);
        }

        @Override
        public void close() {
            groups.values().forEach(InstanceGroup::close);
            groups.clear();
        }
    }
}

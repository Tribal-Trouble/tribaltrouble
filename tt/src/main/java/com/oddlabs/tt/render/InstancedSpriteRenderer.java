package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.InstancedSpriteShader;
import com.oddlabs.tt.render.shader.LitShader;
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
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public final class InstancedSpriteRenderer implements AutoCloseable {

    private final InstancedSpriteShader shader = new InstancedSpriteShader();
    private final Map<@NonNull BatchKey, @NonNull RenderBatch> batches = new HashMap<>();
    private final @NonNull Texture whiteTexture;

    public InstancedSpriteRenderer() {
        GLImage whiteImage = new GLIntImage(1, 1, GL11.GL_RGBA);
        whiteImage.putPixel(0, 0, Color.WHITE_INT);
        whiteTexture = new Texture(new GLImage[]{whiteImage}, GL11.GL_RGBA8, GL11.GL_NEAREST, GL11.GL_NEAREST, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
    }

    public void add(@NonNull SpriteList spriteList, int spriteIndex, int animation, float animTicks, int texIndex, boolean respond, boolean blend, boolean depthWrite, boolean depthTest, @NonNull Matrix4f modelMatrix, @NonNull Vector4fc color, @NonNull Vector4fc decalColor) {
        Sprite sprite = spriteList.getSprite(spriteIndex);
        Sprite.FrameState frameState = sprite.getAnimationState(animation, animTicks);
        
        BatchKey key = new BatchKey(spriteList, spriteIndex, texIndex, respond, blend, depthWrite, depthTest);
        RenderBatch batch = batches.computeIfAbsent(key, RenderBatch::new);
        batch.addInstance(frameState.pos1(), frameState.norm1(), frameState.pos2(), frameState.norm2(), frameState.tween(), modelMatrix, color, decalColor);
    }

    public void renderAll(@NonNull RenderContext context, @NonNull CameraState cameraState, @NonNull MatrixStack projectionStack) {
        if (batches.isEmpty()) return;

        try (var _ = shader.use();
             var _ = cameraState.getFog().setup(shader, cameraState)) {
            
            Matrix4f pm = projectionStack.current();
            shader.setUniformMatrix4(InstancedSpriteShader.Uniforms.PROJECTION_MATRIX, false, pm);
            shader.setUniformMatrix4(InstancedSpriteShader.Uniforms.VIEW_MATRIX, false, cameraState.getModelView());

            // Transform light direction to View Space
            Vector4f lightDir = new Vector4f(-1f, 0f, 1f, 0f);
            cameraState.getModelView().transform(lightDir);
            lightDir.normalize();
            shader.setUniform(LitShader.Uniforms.LIGHT_DIR, lightDir.x, lightDir.y, lightDir.z); 
            shader.setUniform(LitShader.Uniforms.GLOBAL_AMBIENT, 0.4f, 0.4f, 0.4f);
            
            // Set TBO texture unit
            shader.setUniform(InstancedSpriteShader.Uniforms.VERT_BUFFER, 5);

            RenderState state = new RenderState();
            for (RenderBatch batch : batches.values()) {
                batch.render(context, shader, whiteTexture, state);
            }
        } finally {
            // Restore default state to prevent leakage to other renderers (Sky, Landscape)
            context.setDepthMode(DepthMode.READ_WRITE);
            context.setBlendMode(BlendMode.NONE);
            context.setCullMode(CullMode.BACK);
            GL11.glDisable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
            GL30.glBindVertexArray(0);
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

    private record BatchKey(@NonNull SpriteList spriteList, int spriteIndex, int texIndex, boolean respond, boolean blend, boolean depthWrite, boolean depthTest) {}

    private static class RenderState {
        int boundTBO = -1;
    }

    private static class RenderBatch implements AutoCloseable {
        private final @NonNull BatchKey key;
        private FloatVBO vbo;
        private final @NonNull VertexArray vao;
        private FloatBuffer instanceBuffer;
        private int totalInstances = 0;
        private int capacity = 128;

        // mat4 (16) + color (4) + decalColor (4) + pos1(1) + norm1(1) + pos2(1) + norm2(1) + tween (1)
        private static final int FLOATS_PER_INSTANCE = 16 + 4 + 4 + 1 + 1 + 1 + 1 + 1; 

        RenderBatch(@NonNull BatchKey key) {
            this.key = key;
            this.instanceBuffer = BufferUtils.createFloatBuffer(capacity * FLOATS_PER_INSTANCE);
            this.vbo = new FloatVBO(GL15.GL_STREAM_DRAW, capacity * FLOATS_PER_INSTANCE);

            this.vao = new VertexArray();
            vao.bind();

            SpriteList spriteList = key.spriteList;
            ShortVBO ibo = spriteList.getIndices();
            FloatVBO texCoordVBO = spriteList.getTexcoords();
            
            ibo.makeCurrent();
            
            texCoordVBO.makeCurrent();
            GL20.glEnableVertexAttribArray(2); // TexCoord
            Sprite sprite = spriteList.getSprite(key.spriteIndex);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 0, sprite.texcoords_offset * 4L);

            setupInstanceAttributes();

            vao.unbind();
        }

        private void setupInstanceAttributes() {
            vbo.makeCurrent();
            int instanceStride = FLOATS_PER_INSTANCE * Float.BYTES;
            
            // Model Matrix (Locations 3-6)
            for (int i = 0; i < 4; i++) {
                int loc = 3 + i;
                GL20.glEnableVertexAttribArray(loc);
                GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, instanceStride, (long)i * 4 * Float.BYTES);
                GL33.glVertexAttribDivisor(loc, 1);
            }
            
            // Color (Location 7)
            int colorLoc = 7;
            GL20.glEnableVertexAttribArray(colorLoc);
            GL20.glVertexAttribPointer(colorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 16 * Float.BYTES);
            GL33.glVertexAttribDivisor(colorLoc, 1);

            // Decal Color (Location 8)
            int decalColorLoc = 8;
            GL20.glEnableVertexAttribArray(decalColorLoc);
            GL20.glVertexAttribPointer(decalColorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 20 * Float.BYTES);
            GL33.glVertexAttribDivisor(decalColorLoc, 1);
            
            // Animation Offsets & Tween (Locations 9, 10, 11, 12, 13)
            int pos1Loc = 9;
            GL20.glEnableVertexAttribArray(pos1Loc);
            GL20.glVertexAttribPointer(pos1Loc, 1, GL11.GL_FLOAT, false, instanceStride, 24 * Float.BYTES);
            GL33.glVertexAttribDivisor(pos1Loc, 1);
            
            int norm1Loc = 10;
            GL20.glEnableVertexAttribArray(norm1Loc);
            GL20.glVertexAttribPointer(norm1Loc, 1, GL11.GL_FLOAT, false, instanceStride, 25 * Float.BYTES);
            GL33.glVertexAttribDivisor(norm1Loc, 1);
            
            int pos2Loc = 11;
            GL20.glEnableVertexAttribArray(pos2Loc);
            GL20.glVertexAttribPointer(pos2Loc, 1, GL11.GL_FLOAT, false, instanceStride, 26 * Float.BYTES);
            GL33.glVertexAttribDivisor(pos2Loc, 1);
            
            int norm2Loc = 12;
            GL20.glEnableVertexAttribArray(norm2Loc);
            GL20.glVertexAttribPointer(norm2Loc, 1, GL11.GL_FLOAT, false, instanceStride, 27 * Float.BYTES);
            GL33.glVertexAttribDivisor(norm2Loc, 1);
            
            int tweenLoc = 13;
            GL20.glEnableVertexAttribArray(tweenLoc);
            GL20.glVertexAttribPointer(tweenLoc, 1, GL11.GL_FLOAT, false, instanceStride, 28 * Float.BYTES);
            GL33.glVertexAttribDivisor(tweenLoc, 1);
        }

        void addInstance(int pos1, int norm1, int pos2, int norm2, float tween, @NonNull Matrix4f modelMatrix, @NonNull Vector4fc color, @NonNull Vector4fc decalColor) {
            if (totalInstances >= capacity) {
                int newCapacity = capacity * 2;
                FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity * FLOATS_PER_INSTANCE);
                instanceBuffer.flip();
                newBuffer.put(instanceBuffer);
                instanceBuffer = newBuffer;
                
                vbo.close();
                vbo = new FloatVBO(GL15.GL_STREAM_DRAW, newCapacity * FLOATS_PER_INSTANCE);
                
                // Rebind VAO to update VBO binding point
                vao.bind();
                setupInstanceAttributes();
                vao.unbind();
                
                capacity = newCapacity;
            }
            
            modelMatrix.get(instanceBuffer);
            instanceBuffer.position(instanceBuffer.position() + 16);
            color.get(instanceBuffer);
            instanceBuffer.position(instanceBuffer.position() + 4);
            decalColor.get(instanceBuffer);
            instanceBuffer.position(instanceBuffer.position() + 4);
            instanceBuffer.put(pos1);
            instanceBuffer.put(norm1);
            instanceBuffer.put(pos2);
            instanceBuffer.put(norm2);
            instanceBuffer.put(tween);
            
            totalInstances++;
        }

        void render(@NonNull RenderContext context, @NonNull InstancedSpriteShader shader, Texture whiteTexture, @NonNull RenderState state) {
            if (totalInstances == 0) return;

            context.setDepthMode(key.depthTest ? key.depthWrite ? DepthMode.READ_WRITE : DepthMode.READ_ONLY : DepthMode.NONE);

            Sprite sprite = key.spriteList.getSprite(key.spriteIndex);
            context.setCullMode(sprite.culled ? CullMode.BACK : CullMode.NONE);

            if (key.blend) {
                context.setBlendMode(BlendMode.ALPHA);
                GL11.glDisable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
            } else {
                context.setBlendMode(BlendMode.NONE);
                GL11.glEnable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
            }

            setupTextures(context, shader, sprite, whiteTexture, state);
            
            // Upload data
            vbo.makeCurrent();
            instanceBuffer.flip();
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
            
            // Bind TBO
            if (state.boundTBO != key.spriteList.getTBOTextureHandle()) {
                context.setActiveTexture(5);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, key.spriteList.getTBOTextureHandle());
                state.boundTBO = key.spriteList.getTBOTextureHandle();
            }
            
            vao.bind();
            
            // Use glDrawElementsInstanced
            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, sprite.getTriangleCount() * 3, GL11.GL_UNSIGNED_SHORT, (long)sprite.indices_offset * Short.BYTES, totalInstances);
            
            vao.unbind();
        }
        
        private void setupTextures(@NonNull RenderContext context, @NonNull InstancedSpriteShader shader, @NonNull Sprite sprite, Texture whiteTexture, @NonNull RenderState state) {
             Texture texture = (sprite.textures.length > key.texIndex && sprite.textures[key.texIndex].length > 0 && sprite.textures[key.texIndex][0] != null) 
                    ? sprite.textures[key.texIndex][0] : whiteTexture;
            
            context.setTexture(0, texture);
            shader.setUniform(InstancedSpriteShader.Uniforms.TEXTURE_0, 0);
            
            boolean useLighting = Globals.draw_light && sprite.lighted;
            shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_LIGHTING, useLighting);
            shader.setUniform(InstancedSpriteShader.Uniforms.REPLACE_MODE, !useLighting && !sprite.modulate_color);
            
            if (sprite.modulate_color) {
                shader.setUniform(InstancedSpriteShader.Uniforms.MODULATE_COLOR, true);
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
                shader.setUniform(InstancedSpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.0f);
            } else {
                shader.setUniform(InstancedSpriteShader.Uniforms.MODULATE_COLOR, false);
                shader.setUniform(InstancedSpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.1f);
                if (sprite.hasTeamDecal() || key.respond) {
                    shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, true);
                    Texture teamTexture = key.respond ? sprite.respond_texture : sprite.textures[key.texIndex][Sprite.TEXTURE_TEAM];
                    context.setTexture(1, teamTexture);
                    shader.setUniform(InstancedSpriteShader.Uniforms.TEXTURE_1, 1);
                } else {
                    shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
                }
            }

            if (sprite.hasBumpMap(key.texIndex)) {
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_NORMAL_MAP, true);
                context.setTexture(2, sprite.textures[key.texIndex][Sprite.TEXTURE_BUMP]);
                shader.setUniform(InstancedSpriteShader.Uniforms.NORMAL_MAP, 2);
            } else {
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_NORMAL_MAP, false);
            }
        }

        void clear() {
            totalInstances = 0;
            instanceBuffer.clear();
        }

        @Override
        public void close() {
            vao.close();
            vbo.close();
        }
    }
}
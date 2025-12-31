package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.InstancedSpriteShader;
import com.oddlabs.tt.render.shader.LitShader;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
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

    public void add(@NonNull SpriteList spriteList, int spriteIndex, int animation, float animTicks, int texIndex, boolean respond, boolean depthTest, @NonNull Matrix4f modelMatrix, @NonNull Vector4fc color, @NonNull Vector4fc decalColor) {
        Sprite sprite = spriteList.getSprite(spriteIndex);
        int vertexOffset = sprite.getVertexOffset(animation, animTicks);
        
        BatchKey key = new BatchKey(spriteList, spriteIndex, texIndex, respond, depthTest);
        RenderBatch batch = batches.computeIfAbsent(key, RenderBatch::new);
        batch.addInstance(vertexOffset, modelMatrix, color, decalColor);
    }

    public void renderAll(@NonNull CameraState cameraState, @NonNull MatrixStack projectionStack) {
        try (var _ = shader.use();
             var _ = cameraState.getFog().setup(shader, cameraState.getCurrentZ())) {
            
            shader.setUniformMatrix4(InstancedSpriteShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            shader.setUniformMatrix4(InstancedSpriteShader.Uniforms.VIEW_MATRIX, false, cameraState.getModelView());

            shader.setUniform(LitShader.LIGHT_DIR, -1f, 0f, 1f); 
            shader.setUniform(LitShader.GLOBAL_AMBIENT, 0.4f, 0.4f, 0.4f);

            for (RenderBatch batch : batches.values()) {
                batch.render(shader, whiteTexture);
            }
        }
        clear();
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

    private record BatchKey(@NonNull SpriteList spriteList, int spriteIndex, int texIndex, boolean respond, boolean depthTest) {}

    private static class RenderBatch implements AutoCloseable {
        private final @NonNull BatchKey key;
        private final Map<@NonNull Integer, @NonNull SubBatch> subBatches = new HashMap<>();
        private FloatVBO vbo;
        private final @NonNull VertexArray vao;
        private int totalInstances = 0;
        private int vboCapacity = 1024 * FLOATS_PER_INSTANCE;

        // mat4 (16) + color (4) + decalColor (4)
        private static final int FLOATS_PER_INSTANCE = 16 + 4 + 4; 

        RenderBatch(@NonNull BatchKey key) {
            this.key = key;
            this.vbo = new FloatVBO(GL15.GL_STREAM_DRAW, vboCapacity);

            this.vao = new VertexArray();
            vao.bind();

            SpriteList spriteList = key.spriteList;
            ShortVBO ibo = spriteList.getIndices();
            FloatVBO vertexVBO = spriteList.getVerticesAndNormals();
            FloatVBO texCoordVBO = spriteList.getTexcoords();
            
            ibo.makeCurrent();

            vertexVBO.makeCurrent();
            // Initial setup, will be overridden in render loop
            GL20.glEnableVertexAttribArray(0); // Position
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
            
            GL20.glEnableVertexAttribArray(1); // Normal
            GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
            
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
            
            for (int i = 0; i < 4; i++) {
                int loc = 3 + i;
                GL20.glEnableVertexAttribArray(loc);
                GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, instanceStride, (long)i * 4 * Float.BYTES);
                GL33.glVertexAttribDivisor(loc, 1);
            }
            
            int colorLoc = 7;
            GL20.glEnableVertexAttribArray(colorLoc);
            GL20.glVertexAttribPointer(colorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 16 * Float.BYTES);
            GL33.glVertexAttribDivisor(colorLoc, 1);

            int decalColorLoc = 8;
            GL20.glEnableVertexAttribArray(decalColorLoc);
            GL20.glVertexAttribPointer(decalColorLoc, 4, GL11.GL_FLOAT, false, instanceStride, 20 * Float.BYTES);
            GL33.glVertexAttribDivisor(decalColorLoc, 1);
        }

        void addInstance(int vertexOffset, @NonNull Matrix4f modelMatrix, @NonNull Vector4fc color, @NonNull Vector4fc decalColor) {
            SubBatch sub = subBatches.computeIfAbsent(vertexOffset, k -> new SubBatch());
            sub.add(modelMatrix, color, decalColor);
            totalInstances++;
        }

        void render(@NonNull InstancedSpriteShader shader, Texture whiteTexture) {
            if (totalInstances == 0) return;

            if (!key.depthTest) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }

            int requiredFloats = totalInstances * FLOATS_PER_INSTANCE;
            if (requiredFloats > vboCapacity) {
                vboCapacity = Math.max(vboCapacity * 2, requiredFloats);
                vbo.close();
                vbo = new FloatVBO(GL15.GL_STREAM_DRAW, vboCapacity);
                vao.bind();
                setupInstanceAttributes();
                vao.unbind();
            } else {
                // Orphan the buffer to prevent synchronization stalls/flickering
                vbo.orphan();
            }

            Sprite sprite = key.spriteList.getSprite(key.spriteIndex);
            setupTextures(shader, sprite, whiteTexture);
            
            vao.bind();
            
            long currentOffset = 0;
            for (Map.Entry<Integer, SubBatch> entry : subBatches.entrySet()) {
                SubBatch sub = entry.getValue();
                if (sub.count == 0) continue;
                
                // Update instance data
                vbo.makeCurrent();
                sub.buffer.flip();
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, currentOffset, sub.buffer);
                
                int instanceStride = FLOATS_PER_INSTANCE * Float.BYTES;
                
                // Re-bind instance attribute pointers with updated offset
                for (int i = 0; i < 4; i++) {
                    GL20.glVertexAttribPointer(3 + i, 4, GL11.GL_FLOAT, false, instanceStride, currentOffset + (long)i * 4 * Float.BYTES);
                }
                GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, instanceStride, currentOffset + 16 * Float.BYTES);
                GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, instanceStride, currentOffset + 20 * Float.BYTES);
                
                // Update vertex attribute pointers for the current frame
                key.spriteList.getVerticesAndNormals().makeCurrent();
                long vertexOffsetBytes = entry.getKey() * 4L;
                long normalOffsetBytes = vertexOffsetBytes + sprite.getNormalOffset(0) * 4L; // getNormalOffset(0) returns num_vertices * 3
                
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, vertexOffsetBytes);
                GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, normalOffsetBytes);
                
                // Use glDrawElementsInstanced instead of BaseVertex because texcoords are static
                GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, sprite.getTriangleCount() * 3, GL11.GL_UNSIGNED_SHORT, (long)sprite.indices_offset * Short.BYTES, sub.count);
                
                currentOffset += (long) sub.count * instanceStride;
                sub.clear();
            }
            
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            vao.unbind();
            totalInstances = 0;

            if (!key.depthTest) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
        }
        
        private void setupTextures(@NonNull InstancedSpriteShader shader, @NonNull Sprite sprite, Texture whiteTexture) {
             Texture texture = (sprite.textures.length > key.texIndex && sprite.textures[key.texIndex].length > 0 && sprite.textures[key.texIndex][0] != null) 
                    ? sprite.textures[key.texIndex][0] : whiteTexture;
            
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
            shader.setUniform(InstancedSpriteShader.Uniforms.TEXTURE_0, 0);
            
            boolean useLighting = Globals.draw_light && sprite.lighted;
            shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_LIGHTING, useLighting);
            shader.setUniform(InstancedSpriteShader.Uniforms.REPLACE_MODE, !useLighting && !sprite.modulate_color);
            
            if (sprite.modulate_color) {
                shader.setUniform(InstancedSpriteShader.Uniforms.MODULATE_COLOR, true);
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
            } else {
                shader.setUniform(InstancedSpriteShader.Uniforms.MODULATE_COLOR, false);
                if (sprite.hasTeamDecal() || key.respond) {
                    shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, true);
                    GL13.glActiveTexture(GL13.GL_TEXTURE1);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, key.respond ? sprite.respond_texture.getHandle() : sprite.textures[key.texIndex][Sprite.TEXTURE_TEAM].getHandle());
                    shader.setUniform(InstancedSpriteShader.Uniforms.TEXTURE_1, 1);
                } else {
                    shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
                }
            }

            if (sprite.hasBumpMap(key.texIndex)) {
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_NORMAL_MAP, true);
                GL13.glActiveTexture(GL13.GL_TEXTURE2);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.textures[key.texIndex][Sprite.TEXTURE_BUMP].getHandle());
                shader.setUniform(InstancedSpriteShader.Uniforms.NORMAL_MAP, 2);
            } else {
                shader.setUniform(InstancedSpriteShader.Uniforms.ENABLE_NORMAL_MAP, false);
            }
        }

        void clear() {
            totalInstances = 0;
            subBatches.values().forEach(SubBatch::clear);
        }

        @Override
        public void close() {
            vao.close();
            vbo.close();
        }
    }
    
    private static class SubBatch {
        @NonNull FloatBuffer buffer;
        int count = 0;
        int capacity = 128;
        
        SubBatch() {
            buffer = BufferUtils.createFloatBuffer(capacity * RenderBatch.FLOATS_PER_INSTANCE);
        }
        
        void add(@NonNull Matrix4fc modelMatrix, @NonNull Vector4fc color, @NonNull Vector4fc decalColor) {
            if (count >= capacity) {
                int newCapacity = capacity * 2;
                FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity * RenderBatch.FLOATS_PER_INSTANCE);
                buffer.flip();
                newBuffer.put(buffer);
                buffer = newBuffer;
                capacity = newCapacity;
            }
            int position = buffer.position();
            modelMatrix.get(position, buffer);
            color.get((position += 16), buffer);
            decalColor.get((position += 4), buffer);
            buffer.position(position + 4);
            count++;
        }
        
        void clear() {
            count = 0;
            buffer.clear();
        }
    }
}

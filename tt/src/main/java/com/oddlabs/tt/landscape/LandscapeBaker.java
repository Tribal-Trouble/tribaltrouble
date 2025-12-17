package com.oddlabs.tt.landscape;

import com.oddlabs.tt.render.FBO;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.BlendLighting;
import com.oddlabs.tt.resource.StructureBlend;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.vbo.QuadVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import java.nio.IntBuffer;

public final class LandscapeBaker {

    private static final String VERTEX_SHADER = """
        #version 120
        attribute vec2 a_position;
        attribute vec2 a_texCoord0;
        varying vec2 v_texCoord;
        void main() {
            gl_Position = vec4(a_position, 0.0, 1.0);
            v_texCoord = a_texCoord0;
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 120
        uniform sampler2D u_BaseDiffuse;
        uniform sampler2D u_LayerDiffuse;
        uniform sampler2D u_BaseNormal;
        uniform sampler2D u_LayerNormal;
        uniform sampler2D u_AlphaMap;
        uniform int u_Mode; // 0 = Blend, 1 = Light
        uniform float u_TextureScale;
        
        varying vec2 v_texCoord;

        void main() {
            vec4 baseDiff = texture2D(u_BaseDiffuse, v_texCoord);
            vec4 baseNorm = texture2D(u_BaseNormal, v_texCoord);
            float alpha = texture2D(u_AlphaMap, v_texCoord).a;

            if (u_Mode == 0) { // Structure Blend
                vec4 layerDiff = texture2D(u_LayerDiffuse, v_texCoord * u_TextureScale);
                vec4 layerNorm = texture2D(u_LayerNormal, v_texCoord * u_TextureScale);
                
                gl_FragData[0] = mix(baseDiff, layerDiff, alpha);
                gl_FragData[1] = mix(baseNorm, layerNorm, alpha);
            } else { // Lighting Blend
                // Simple modulate for now
                gl_FragData[0] = baseDiff * alpha; // Apply light to diffuse
                gl_FragData[1] = baseNorm;         // Keep normals
            }
        }
        """;

    private static class BlendShader extends ShaderProgram {
        public BlendShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER, programId -> {
                GL20.glBindAttribLocation(programId, 0, "a_position");
                GL20.glBindAttribLocation(programId, 1, "a_texCoord0");
            });
        }
    }

    public WorldInfo.@NonNull Maps bake(int colormapSize, float textureScale, BlendInfo[] blendInfos) {
        // Save current viewport
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        // Create Ping-Pong textures
        Texture[] diffuse = new Texture[2];
        Texture[] normal = new Texture[2];
        
        for (int i = 0; i < 2; i++) {
            diffuse[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
            normal[i] = new Texture(colormapSize, colormapSize, GL11.GL_RGBA, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
        }

        try (FBO fbo = new FBO(colormapSize, colormapSize);
             BlendShader shader = new BlendShader()) {
             
            QuadVBO quad = new QuadVBO();
            
            int current = 0; 
            
            try (var _ = shader.use()) {
                shader.setUniform("u_BaseDiffuse", 0);
                shader.setUniform("u_LayerDiffuse", 1);
                shader.setUniform("u_BaseNormal", 2);
                shader.setUniform("u_LayerNormal", 3);
                shader.setUniform("u_AlphaMap", 4);
                shader.setUniform("u_TextureScale", textureScale);
                
                IntBuffer drawBuffers = BufferUtils.createIntBuffer(2);
                drawBuffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();
    
                for (int i = 0; i < blendInfos.length; i++) {
                    BlendInfo info = blendInfos[i];
                    int src = current;
                    int dst = 1 - current;
                    
                    fbo.bind();
                    fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, diffuse[dst]);
                    fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, normal[dst]);
                    GL20.glDrawBuffers(drawBuffers);
                    fbo.checkStatus();
                    
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                    
                    // Bind Previous (Base)
                    GL13.glActiveTexture(GL13.GL_TEXTURE0);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[src].getHandle());
                    GL13.glActiveTexture(GL13.GL_TEXTURE2);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[src].getHandle());
                    
                    // Bind Alpha
                    GL13.glActiveTexture(GL13.GL_TEXTURE4);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, info.getAlphaMap().getHandle());
                    
                    if (info instanceof StructureBlend) {
                        StructureBlend sb = (StructureBlend) info;
                        shader.setUniform("u_Mode", 0);
                        // Bind Layer
                        GL13.glActiveTexture(GL13.GL_TEXTURE1);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sb.getStructureMap().getHandle());
                        GL13.glActiveTexture(GL13.GL_TEXTURE3);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sb.getNormalMap().getHandle());
                    } else if (info instanceof BlendLighting) {
                        shader.setUniform("u_Mode", 1);
                    }
                    
                    quad.render(shader);
                    
                    current = dst; // Flip
                }
            }
            
            fbo.unbind();
            quad.delete();
            
            // Restore viewport
            GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            
            boolean gl30 = GLContext.getCapabilities().OpenGL30;
            
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuse[current].getHandle());
            if (gl30) {
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            } else {
                EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
            }
            
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, normal[current].getHandle());
            if (gl30) {
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            } else {
                EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
            }
            
            // Delete the unused pair
            diffuse[1 - current].close();
            normal[1 - current].close();
            
            return new WorldInfo.Maps(diffuse[current], normal[current]);
        }
    }
}
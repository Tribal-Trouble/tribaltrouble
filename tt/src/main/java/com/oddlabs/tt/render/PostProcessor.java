package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.shader.PostProcessShader;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * Manages the full-screen post-processing pipeline.
 * Handles rendering the scene to an FBO and applying effects via PostProcessShader.
 */
public final class PostProcessor implements AutoCloseable {
    private final @NonNull PostProcessShader shader;
    private final @NonNull VertexArray vao;
    private final @NonNull FloatVBO quadVBO;
    private @NonNull FBO sceneFBO;
    private int currentWidth;
    private int currentHeight;

    public PostProcessor(int width, int height) {
        this.currentWidth = width;
        this.currentHeight = height;
        this.shader = new PostProcessShader();
        this.sceneFBO = FBO.createSceneFBO(width, height);

        // Setup Full Screen Quad
        this.vao = new VertexArray();
        this.vao.bind();

        float[] quadVertices = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        };
        FloatBuffer buffer = BufferUtils.createFloatBuffer(quadVertices.length).put(quadVertices).flip();
        this.quadVBO = new FloatVBO(GL15.GL_STATIC_DRAW, buffer);
        
        int posLoc = shader.getAttributeLocation(PostProcessShader.Attributes.POSITION);
        if (posLoc >= 0) {
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(posLoc);
            quadVBO.vertexAttribPointer(posLoc, 2, 0, 0);
        }
        
        this.vao.unbind();
    }

    public void resize(int width, int height) {
        if (this.currentWidth == width && this.currentHeight == height) return;
        this.currentWidth = width;
        this.currentHeight = height;
        sceneFBO.resize(width, height);
    }

    public void bindSceneFBO() {
        sceneFBO.bind();
    }

    public void unbindSceneFBO() {
        sceneFBO.unbind();
    }

    public void renderComposite() {
        // Render to default framebuffer (screen)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, currentWidth, currentHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Disable depth test and culling for full screen pass
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (depthTest) GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (cullFace) GL11.glDisable(GL11.GL_CULL_FACE);

        try (var _ = shader.use()) {
            Settings settings = Settings.getSettings();
            shader.setUniform(PostProcessShader.Uniforms.CVD_MODE, settings.cvd_mode);
            shader.setUniform(PostProcessShader.Uniforms.CVD_INTENSITY, settings.cvd_intensity);
            shader.setUniform(PostProcessShader.Uniforms.HIGH_CONTRAST, settings.high_contrast);
            shader.setUniform(PostProcessShader.Uniforms.CONTRAST_INTENSITY, settings.contrast_intensity);
            shader.setUniform(PostProcessShader.Uniforms.TEAM_STENCIL, settings.team_stencil);
            shader.setUniform(PostProcessShader.Uniforms.SCENE_TEXTURE, 0);
            shader.setUniform(PostProcessShader.Uniforms.MASK_TEXTURE, 1);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            if (sceneFBO.getColorTexture() != null) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.getColorTexture().getHandle());
            }
            
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            if (sceneFBO.getMaskTexture() != null) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFBO.getMaskTexture().getHandle());
            }

            vao.bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            vao.unbind();
        } finally {
            if (depthTest) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (cullFace) GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    @Override
    public void close() {
        shader.close();
        sceneFBO.close();
        vao.close();
        quadVBO.close();
    }
}

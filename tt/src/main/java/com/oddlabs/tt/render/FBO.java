package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

public final class FBO implements AutoCloseable {
    private int id;
    private int width;
    private int height;
    private @Nullable Texture colorTexture;
    private @Nullable Texture maskTexture;
    private @Nullable Texture depthTexture;

    public FBO(int width, int height) {
        this.width = width;
        this.height = height;
        this.id = GL30.glGenFramebuffers();
    }

    public static @NonNull FBO createSceneFBO(int width, int height) {
        FBO fbo = new FBO(width, height);
        fbo.bind();

        // HDR Color Texture (Float16 for high dynamic range)
        Texture color = new Texture(width, height, GL30.GL_RGBA16F, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE);
        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT0, color);
        fbo.colorTexture = color;

        // Mask Texture (Standard RGBA for team color/stencil)
        Texture mask = new Texture(width, height, GL11.GL_RGBA, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE);
        fbo.attachTexture(GL30.GL_COLOR_ATTACHMENT1, mask);
        fbo.maskTexture = mask;

        // Depth Texture (24-bit depth)
        Texture depth = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE);
        fbo.attachTexture(GL30.GL_DEPTH_ATTACHMENT, depth);
        fbo.depthTexture = depth;

        // Explicitly declare draw buffers
        GL30.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});

        fbo.checkStatus();
        fbo.unbind();
        return fbo;
    }

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (this.width == width && this.height == height) return;
        this.width = width;
        this.height = height;

        if (colorTexture != null) {
            colorTexture.close();
            // Re-create color texture with new dimensions
            colorTexture = new Texture(width, height, GL30.GL_RGBA16F, GL11.GL_LINEAR, GL11.GL_LINEAR,
                    GL12.GL_CLAMP_TO_EDGE);
        }
        if (maskTexture != null) {
            maskTexture.close();
            maskTexture = new Texture(width, height, GL11.GL_RGBA, GL11.GL_NEAREST, GL11.GL_NEAREST,
                    GL12.GL_CLAMP_TO_EDGE);
        }
        if (depthTexture != null) {
            depthTexture.close();
            // Re-create depth texture with new dimensions
            depthTexture = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST, GL11.GL_NEAREST,
                    GL12.GL_CLAMP_TO_EDGE);
        }

        bind();
        if (colorTexture != null) attachTexture(GL30.GL_COLOR_ATTACHMENT0, colorTexture);
        if (maskTexture != null) attachTexture(GL30.GL_COLOR_ATTACHMENT1, maskTexture);
        if (depthTexture != null) attachTexture(GL30.GL_DEPTH_ATTACHMENT, depthTexture);

        // Restore draw buffers state after resize/rebind
        GL30.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});

        checkStatus();
        unbind();
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
        GL11.glViewport(0, 0, width, height);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public @Nullable Texture getColorTexture() {
        return colorTexture;
    }

    public @Nullable Texture getMaskTexture() {
        return maskTexture;
    }

    public @Nullable Texture getDepthTexture() {
        return depthTexture;
    }

    public void attachTexture(int attachmentPoint, @NonNull Texture texture) {
        attachTexture(attachmentPoint, texture, 0);
    }

    public void attachTexture(int attachmentPoint, @NonNull Texture texture, int level) {
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachmentPoint, GL11.GL_TEXTURE_2D, texture.getHandle(),
                level);
    }

    public void checkStatus() {
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is incomplete: " + status);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void close() {
        if (id != 0) {
            GL30.glDeleteFramebuffers(id);
            id = 0;
        }
    }
}

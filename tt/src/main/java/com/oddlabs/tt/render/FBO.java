package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class FBO implements AutoCloseable {
    private int id;
    private final int width;
    private final int height;

    public FBO(int width, int height) {
        this.width = width;
        this.height = height;
        this.id = GL30.glGenFramebuffers();
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
        GL11.glViewport(0, 0, width, height);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void attachTexture(int attachmentPoint, @NonNull Texture texture) {
        attachTexture(attachmentPoint, texture, 0);
    }

    public void attachTexture(int attachmentPoint, @NonNull Texture texture, int level) {
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachmentPoint, GL11.GL_TEXTURE_2D, texture.getHandle(), level);
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

package com.oddlabs.tt.render;

import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * Wraps a native Framebuffer Object
 */
public final class FBO extends NativeResource<FBO.Buffer> {

    static final class Buffer extends NativeResource.NativeState {

        private final int handle;

        Buffer() {
            handle = GL30.glGenFramebuffers();
        }

        @Override
        public void close() {
            Renderer.getRenderer().getRenderContext().invalidateFramebuffer(handle);
            GL30.glDeleteFramebuffers(handle);
        }
    }

    private int width;
    private int height;
    private @Nullable Texture colorTexture;
    private @Nullable Texture maskTexture;
    private @Nullable Texture depthTexture;

    public FBO(int width, int height) {
        super(new Buffer());
        this.width = width;
        this.height = height;
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
        Renderer.getRenderer().getRenderContext().setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_COLOR_ATTACHMENT1});

        fbo.checkStatus();
        fbo.unbind();
        return fbo;
    }

    public void resize(int width, int height) {
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
        var context = Renderer.getRenderer().getRenderContext();
        if (colorTexture != null) attachTexture(GL30.GL_COLOR_ATTACHMENT0, colorTexture);
        if (maskTexture != null) attachTexture(GL30.GL_COLOR_ATTACHMENT1, maskTexture);
        if (depthTexture != null) attachTexture(GL30.GL_DEPTH_ATTACHMENT, depthTexture);

        // Restore draw buffers state after resize/rebind (if we have color attachments)
        if (colorTexture != null || maskTexture != null) {
            context.setDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
        } else {
            context.setDrawBuffers(new int[]{GL11.GL_NONE});
            GL11.glReadBuffer(GL11.GL_NONE);
        }

        checkStatus();
        unbind();
    }

    public void bind(@NonNull RenderContext context) {
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, getHandle());
        context.setViewport(0, 0, width, height);
    }

    public void bind() {
        bind(Renderer.getRenderer().getRenderContext());
    }

    public void unbind() {
        unbind(Renderer.getRenderer().getRenderContext());
    }

    public void unbind(@NonNull RenderContext context) {
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void detachAll() {
        bind();
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, 0, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
        colorTexture = null;
        maskTexture = null;
        depthTexture = null;
        unbind();
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

    public void blitDepthTo(@NonNull FBO target) {
        var context = Renderer.getRenderer().getRenderContext();
        context.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, getHandle());
        context.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.getHandle());
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, target.width, target.height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        // Bind back to original target
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, getHandle());
    }

    public void attachTexture(int attachmentPoint, @NonNull Texture texture) {
        attachTexture(attachmentPoint, texture, 0);
    }

    public void attachTexture(int attachmentPoint, @NonNull Texture texture, int level) {
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachmentPoint, GL11.GL_TEXTURE_2D, texture.getHandle(),
                level);
        if (attachmentPoint == GL30.GL_COLOR_ATTACHMENT0) colorTexture = texture;
        else if (attachmentPoint == GL30.GL_COLOR_ATTACHMENT1) maskTexture = texture;
        else if (attachmentPoint == GL30.GL_DEPTH_ATTACHMENT) depthTexture = texture;
    }

    public void checkStatus() {
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer is incomplete: " + status);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getHandle() {
        return state.handle;
    }

    @Override
    public void close() {
        super.close();

        if (colorTexture != null) {
            colorTexture.close();
            colorTexture = null;
        }
        if (maskTexture != null) {
            maskTexture.close();
            maskTexture = null;
        }
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }
    }
}

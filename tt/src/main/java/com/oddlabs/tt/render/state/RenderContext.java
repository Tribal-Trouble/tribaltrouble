package com.oddlabs.tt.render.state;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.vbo.VBO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface RenderContext {
    // State Management
    void setBlendMode(@NonNull BlendMode mode);

    void setDepthMode(@NonNull DepthMode mode);

    void setCullMode(@NonNull CullMode mode);

    // Alpha to Coverage
    void setSampleAlphaToCoverage(boolean enabled);

    // Low-level state
    void setDepthTest(boolean enabled);

    void setDepthMask(boolean enabled);

    void setBlend(boolean enabled);

    void setCullFace(boolean enabled);

    void setCullFaceMode(int mode);

    // Depth Func
    void setDepthFunc(int func);

    // Color Mask
    void setColorMask(boolean r, boolean g, boolean b, boolean a);

    // Texture Management
    void setActiveTexture(int unit);

    void setTexture(int unit, int textureHandle);

    void setTexture(int unit, int textureHandle, int target);

    default void setTexture(int unit, @Nullable Texture texture) {
        if (texture != null) {
            setTexture(unit, texture.getHandle(), texture.getTarget());
        } else {
            setTexture(unit, 0);
        }
    }

    // Viewport
    void setViewport(int x, int y, int w, int h);

    int getViewportWidth();

    int getViewportHeight();

    // VAO
    void bindVertexArray(int vao);

    // VBO / FBO
    void bindBuffer(int target, int buffer);

    void bindFramebuffer(int target, int framebuffer);

    void invalidateTexture(int handle);

    void invalidateBuffer(int handle);

    void invalidateFramebuffer(int handle);

    void invalidateVertexArray(int handle);

    default void bindBuffer(int target, @Nullable VBO vbo) {
        bindBuffer(target, vbo != null ? vbo.getHandle() : 0);
    }

    // Clearing
    void clearColor(float r, float g, float b, float a);

    void clear(boolean color, boolean depth);

    // Scoped State (Try-with-resources)
    // These return a Closeable that restores the PREVIOUS state.
    @NonNull
    ScopedState withBlendMode(@NonNull BlendMode mode);

    @NonNull
    ScopedState withDepthMode(@NonNull DepthMode mode);

    @NonNull
    ScopedState withCullMode(@NonNull CullMode mode);

    @NonNull
    ScopedState withSampleAlphaToCoverage(boolean enabled);

    @NonNull
    ScopedState withColorMask(boolean r, boolean g, boolean b, boolean a);

    @NonNull
    ScopedState withDepthFunc(int func);

    @NonNull
    ScopedState withDrawBuffers(boolean mask);

    @NonNull
    ScopedState withFramebufferSrgb(boolean enabled);

    // UBO Management
    void updateGlobalState(java.nio.@NonNull ByteBuffer data);

    // Custom State
    void setBlendFunc(int src, int dst);

    void setBlendEquation(int equation);

    /**
     * Resets the blend functions for all draw buffers to the current global setBlendFunc state.
     * This is useful to clear per-buffer blend states (e.g. from glBlendFunci) after complex composite operations.
     */
    void resetBlendFunc();

    void setDrawBuffers(boolean mask);

    void setDrawBuffers(int @NonNull [] attachments);

    void setFramebufferSrgb(boolean enabled);

    // Lifecycle & Debug
    void init();

    void applyDefaults();

    /**
     * Verifies that the tracked state matches the actual OpenGL state.
     *
     * @throws IllegalStateException if a mismatch is found.
     */
    void validate();
}

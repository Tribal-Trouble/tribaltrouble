package com.oddlabs.tt.render.state;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface RenderContext {
    // State Management
    void setBlendMode(@NonNull BlendMode mode);
    void setDepthMode(@NonNull DepthMode mode);
    void setCullMode(@NonNull CullMode mode);
    
    // Depth Func
    void setDepthFunc(int func);
    
    // Color Mask
    void setColorMask(boolean r, boolean g, boolean b, boolean a);
    
    // Texture Management
    void setActiveTexture(int unit);
    void setTexture(int unit, int textureHandle);
    default void setTexture(int unit, @Nullable Texture texture) {
        setTexture(unit, texture != null ? texture.getHandle() : 0);
    }
    
    // Scissor / Viewport
    void setScissor(int x, int y, int w, int h);
    void clearScissor();
    
    // Clearing
    void clearColor(float r, float g, float b, float a);
    void clear(boolean color, boolean depth);
    
    // Scoped State (Try-with-resources)
    // These return a Closeable that restores the PREVIOUS state.
    @NonNull ScopedState withBlendMode(@NonNull BlendMode mode);
    @NonNull ScopedState withDepthMode(@NonNull DepthMode mode);
    @NonNull ScopedState withCullMode(@NonNull CullMode mode);
    @NonNull ScopedState withColorMask(boolean r, boolean g, boolean b, boolean a);
    @NonNull ScopedState withDepthFunc(int func);
    
    // Custom State
    void setBlendFunc(int src, int dst);
    
    // Lifecycle & Debug
    void applyDefaults();
    
    /**
     * Verifies that the tracked state matches the actual OpenGL state.
     * @throws IllegalStateException if a mismatch is found.
     */
    void validate();
}

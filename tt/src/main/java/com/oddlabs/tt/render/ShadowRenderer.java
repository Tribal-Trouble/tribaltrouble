package com.oddlabs.tt.render;

import com.oddlabs.tt.util.GLState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class ShadowRenderer {
    private final DecalRenderer decalRenderer = new DecalRenderer();
    private float r = 1f, g = 1f, b = 1f, a = 1f;
    private @Nullable Texture currentTexture;

    protected @NonNull GLState setupShadows(@NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        var decalState = decalRenderer.setup(renderer, modelViewStack, projectionStack);
        
        return () -> {
            decalState.close();
            currentTexture = null;
        };
    }

    protected void setShadowColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    protected void bindShadowTexture(@NonNull Texture texture) {
        this.currentTexture = texture;
    }

    protected final void renderShadow(@NonNull LandscapeRenderer renderer, float shadow_size, float f_x, float f_y) {
        if (currentTexture != null) {
            decalRenderer.draw(currentTexture, f_x, f_y, shadow_size, r, g, b, a);
        }
    }
    
    public void close() {
        decalRenderer.close();
    }
}

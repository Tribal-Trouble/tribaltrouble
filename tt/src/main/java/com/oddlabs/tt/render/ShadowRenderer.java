package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Shadowable;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class for rendering dynamic shadow decals for world objects.
 */
abstract class ShadowRenderer {
    private Color.@NonNull Linear color = Color.Linear.WHITE;
    private float patternVal = 0.0f;
    private @Nullable Texture currentTexture;
    private boolean radial = false;
    private @Nullable DecalRenderer sharedRenderer;

    protected @NonNull ScopedState setupShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        this.sharedRenderer = queues.getDecalRenderer();
        var decalState = sharedRenderer.setup(context, renderer, modelViewStack, projectionStack);

        return () -> {
            decalState.close();
            this.sharedRenderer = null;
            this.currentTexture = null;
            this.patternVal = 0.0f;
        };
    }

    protected void setShadowColor(@NonNull Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    protected void setPattern(Selectable.@NonNull VisualPattern pattern) {
        this.patternVal = (float) pattern.ordinal();
    }

    protected void setPatternVal(float patternVal) {
        this.patternVal = patternVal;
    }

    protected void bindShadowTexture(@NonNull Texture texture) {
        this.currentTexture = texture;
    }

    protected void setRadial(boolean radial) {
        this.radial = radial;
    }

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull Shadowable model) {
        if (currentTexture != null && sharedRenderer != null) {
            renderShadow(context, renderer, model.getPositionX(), model.getPositionY(), model.getShadowDiameter(),
                    color, model.getShadowVerticalCenter(), model.getShadowOpacity());
        }
    }

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            float f_x, float f_y, float shadow_size) {
        renderShadow(context, renderer, f_x, f_y, shadow_size, color, 0.6f, 1.0f);
    }

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            float f_x, float f_y, float shadow_size, Color.@NonNull Linear color, float shadowOffsetScale,
            float shadowOpacity) {
        if (currentTexture != null && sharedRenderer != null) {
            // Expand the quad for radial halos to provide room for the offset shadow blob and animation padding.
            float size = radial ? shadow_size * 2.5f : shadow_size;
            sharedRenderer.draw(context, currentTexture, f_x, f_y, size, color, patternVal, shadowOffsetScale, radial,
                    shadowOpacity);
        }
    }


    public void close() {
    }
}

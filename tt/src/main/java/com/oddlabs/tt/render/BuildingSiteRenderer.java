package com.oddlabs.tt.render;

import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class BuildingSiteRenderer extends ShadowRenderer {
    private final @NonNull Texture texture;
    private boolean sea = false;


    public BuildingSiteRenderer() {
        GLIntImage img = new GLIntImage(16, 16, GL11.GL_RGBA);
        img.clear(1, 1, img.getWidth() - 2, img.getHeight() - 2, Color.WHITE_INT);
        texture = new Texture(new GLIntImage[]{img}, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
    }

    public final void setSea(boolean sea) {
        this.sea = sea;
    }

    public void renderSites(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack,
            @NonNull List<? extends @NonNull Target> targets, float center_x, float center_y, float max_radius) {
        try (var _ = setupShadows(context, renderer, modelViewStack, projectionStack)) {
            bindShadowTexture(texture);
            float radius_sqr = max_radius * max_radius;
            for (Target target : targets) {
                float dx = target.getPositionX() - center_x;
                float dy = target.getPositionY() - center_y;
                float a = (dx * dx + dy * dy) / radius_sqr;
                if (dx == 0f && dy == 0f) {
                    setShadowColor(1f, 1f, 1f, 1f);
                } else {
                    if (sea) {
                        setShadowColor(0f, 0f, 1f, Math.max(0f, 1 - a * a));
                    } else {
                        setShadowColor(0f, 1f, 0f, Math.max(0f, 1 - a * a));
                    }
                }
                renderShadow(context, renderer, 2f, target.getPositionX(), target.getPositionY());
            }
        }
    }
}

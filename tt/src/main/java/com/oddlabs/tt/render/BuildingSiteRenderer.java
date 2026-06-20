package com.oddlabs.tt.render;

import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.model.Target;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.List;

public final class BuildingSiteRenderer extends ShadowRenderer {

    private final @NonNull Texture green;

    public BuildingSiteRenderer() {
        GLIntImage img = new GLIntImage(16, 16, GL11.GL_RGBA);
        img.clear(1, 1, img.getWidth() - 2, img.getHeight() - 2, Color.WHITE_INT);
        green = new Texture(new GLIntImage[]{img}, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
    }

    public void renderSites(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack, @NonNull List<? extends @NonNull Target> targets, float center_x,
            float center_y, float max_radius) {
        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            bindShadowTexture(green);
            float radius_sqr = max_radius * max_radius;
            for (Target target : targets) {
                float dx = target.getPositionX() - center_x;
                float dy = target.getPositionY() - center_y;
                float a = (dx * dx + dy * dy) / radius_sqr;
                if (dx == 0f && dy == 0f)
                    setShadowColor(Color.Linear.WHITE);
                else
                    setShadowColor(Color.Linear.GREEN.alpha(Math.max(0f, 1 - a * a)));
                renderShadow(context, renderer, target.getPositionX(), target.getPositionY(), 2f);
            }
        }
    }
}

package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.render.DecalRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Draws the brush outline as a ring of soft dots laid on the ground. Small dots follow the terrain closely where
 * one large ring texture would float over or sink into hills.
 */
final class BrushRenderer {
    private static final int DOT_TEXTURE_SIZE = 32;
    private static final float DOT_SPACING = 1.5f;
    private static final float DOT_SIZE = 1.6f;
    private static final int MIN_DOTS = 24;
    // Enough for a full brush ring at the largest radius without gaps between the dots.
    private static final int MAX_DOTS = 2400;

    private final DecalRenderer decal_renderer = new DecalRenderer();
    private final @NonNull Texture dot;

    BrushRenderer() {
        GLIntImage image = new GLIntImage(DOT_TEXTURE_SIZE, DOT_TEXTURE_SIZE, GL11.GL_RGBA);
        float center = (DOT_TEXTURE_SIZE - 1) / 2f;
        for (int y = 0; y < DOT_TEXTURE_SIZE; y++) {
            for (int x = 0; x < DOT_TEXTURE_SIZE; x++) {
                float dx = (x - center) / center;
                float dy = (y - center) / center;
                float alpha = Math.clamp(1f - (float) Math.sqrt(dx * dx + dy * dy), 0f, 1f);
                int a = (int) (Math.min(1f, alpha * 2f) * 255f + .5f);
                image.putPixel(x, y, (a << 24) | 0x00_FF_FF_FF);
            }
        }
        dot = new Texture(new GLIntImage[]{image}, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
    }

    /** A batch of dots; close it to draw them. */
    final class Batch implements AutoCloseable {
        private final @NonNull RenderContext context;
        private final @NonNull ScopedState state;

        private Batch(@NonNull LandscapeRenderer landscape, @NonNull MatrixStack model_view,
                @NonNull MatrixStack projection, float min_height) {
            context = Renderer.getRenderer().getRenderContext();
            state = decal_renderer.setup(context, landscape, model_view, projection, min_height);
        }

        /** A circle of dots, in meters. */
        void circle(float cx, float cy, float radius, float r, float g, float b, float a) {
            int count = Math.clamp((int) (2 * Math.PI * radius / DOT_SPACING), MIN_DOTS, MAX_DOTS);
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                dot(cx + radius * (float) Math.cos(angle), cy + radius * (float) Math.sin(angle), r, g, b, a);
            }
        }

        /** A dotted line, in meters. */
        void line(float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            int count = Math.clamp((int) (Math.sqrt(dx * dx + dy * dy) / (DOT_SPACING * 2)), 1, MAX_DOTS);
            for (int i = 0; i <= count; i++) {
                float t = i / (float) count;
                dot(x0 + dx * t, y0 + dy * t, r, g, b, a);
            }
        }

        /** The outline of a rectangle with corners at two points, in meters. */
        void rectangle(float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
            line(x0, y0, x1, y0, r, g, b, a);
            line(x1, y0, x1, y1, r, g, b, a);
            line(x1, y1, x0, y1, r, g, b, a);
            line(x0, y1, x0, y0, r, g, b, a);
        }

        void dot(float x, float y, float r, float g, float b, float a) {
            decal_renderer.draw(context, dot, x, y, DOT_SIZE, r, g, b, a);
        }

        @Override
        public void close() {
            state.close();
        }
    }

    @NonNull
    Batch begin(@NonNull LandscapeRenderer landscape, @NonNull MatrixStack model_view,
            @NonNull MatrixStack projection) {
        return new Batch(landscape, model_view, projection, -Float.MAX_VALUE);
    }

    /** A batch of dots that lie on the sea's surface where the ground is below it. */
    @NonNull
    Batch beginAbove(@NonNull LandscapeRenderer landscape, @NonNull MatrixStack model_view,
            @NonNull MatrixStack projection, float sea_level) {
        return new Batch(landscape, model_view, projection, sea_level);
    }
}

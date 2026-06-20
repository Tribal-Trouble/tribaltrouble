package com.oddlabs.tt.gui;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

/**
 * An animated arrow that points toward a specific 3D coordinate in the game world,
 * constrained to the edges of the screen if the target is off-screen.
 */
public final class Arrow extends GUIObject {
    private static final float SECONDS_PER_FLASH = .5f;
    private static final float COLOR_DELTA = .5f;

    private final float target_x;
    private final float target_y;
    private final float target_z;
    private final Color.Linear color;
    private final boolean show_always;
    private final @NonNull GUIRoot gui_root;

    public Arrow(@NonNull HeightMap heightmap, @NonNull GUIRoot gui_root, float target_x, float target_y,
            @NonNull Color color, boolean show_always) {
        this.gui_root = gui_root;
        this.target_x = target_x;
        this.target_y = target_y;
        this.target_z = heightmap.getNearestHeight(target_x, target_y);
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
        this.show_always = show_always;
        displayChangedNotify(gui_root.getWidth(), gui_root.getHeight());
    }

    @Override
    protected void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    private @NonNull Vector4f project3DTo2D(@NonNull Vector4f point) {
        gui_root.getDelegate().getCamera().getState().getProjectionModelView().transform(point, point);
        if (point.w < .1f)
            point.w = .1f;
        float inv_w = 1 / point.w;
        point.set((point.x * inv_w + 1) * .5f * gui_root.getWidth(), (point.y * inv_w + 1) * .5f * gui_root.getHeight(),
                0, 0);
        return point;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        int screen_width = gui_root.getWidth();
        int screen_height = gui_root.getHeight();
        Vector4f point = project3DTo2D(new Vector4f(target_x, target_y, target_z, 1));
        float dx = point.x - screen_width / 2f;
        float dy = point.y - screen_height / 2f;
        float dist_sqr = dx * dx + dy * dy;
        if (dist_sqr < 1f) {
            dx = 1f;
            dy = 0f;
        } else {
            float inv_dist = 1f / (float) Math.sqrt(dist_sqr);
            dx *= inv_dist;
            dy *= inv_dist;
        }

        float angle = (float) Math.toDegrees(Math.acos(dx));
        if (dy < 0f)
            angle = 360f - angle;
        float real_t = (point.x - screen_width / 2f) / dx;
        float t = real_t;
        float t_min_x = (-screen_width / 2f) / dx;
        float t_max_x = (screen_width / 2f) / dx;
        float t_x = Math.max(t_min_x, t_max_x);
        t = Math.min(t, t_x);
        float t_min_y = (-screen_height / 2f) / dy;
        float t_max_y = (screen_height / 2f) / dy;
        float t_y = Math.max(t_min_y, t_max_y);
        t = Math.min(t, t_y);
        if (show_always || gui_root.getDelegate().getCamera().getState().inNoDetailMode() || t < real_t) {
            var data = GUIIcons.getIcons().getNotifyArrowData();
            float head_x = data.headX();
            float head_y = data.headY();
            renderer.getMatrixStack().push();
            renderer.getMatrixStack().translate(screen_width / 2f + dx * t, screen_height / 2f + dy * t, 0f);
            renderer.getMatrixStack().rotate(angle, 0f, 0f, 1f);
            float val = (Renderer.getRenderer().getEventQueue().getTime() % SECONDS_PER_FLASH) / (SECONDS_PER_FLASH
                    * .5f);
            if (val > 1f)
                val = 2f - val;
            val = COLOR_DELTA * val;
            IconQuad arrow = data.arrow();
            renderer.drawIcon(arrow, -head_x, -head_y, color.alpha(1f - val));
            renderer.getMatrixStack().pop();
        }
    }
}

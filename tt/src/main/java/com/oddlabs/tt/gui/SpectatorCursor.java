package com.oddlabs.tt.gui;

import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.viewer.PlayerView;
import com.oddlabs.tt.viewer.SpectatorView;
import com.oddlabs.tt.viewer.WorldViewer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.function.BooleanSupplier;

/** The watched player's mouse pointer, drawn where their cursor touches the ground, in their color. */
public final class SpectatorCursor extends GUIObject {
    private static final String POINTER_TEXTURE = "/textures/gui/pointer_32_8";
    private static final int HOT_X = 2;
    private static final int HOT_Y = 2;
    private static final float SMOOTHNESS_FACTOR = 15f;

    private final @NonNull WorldViewer viewer;
    private final @NonNull GUIRoot gui_root;
    private final @NonNull BooleanSupplier visible;
    private final @NonNull Texture texture;
    private final @NonNull Vector4f point = new Vector4f();
    private final @NonNull Vector4f color = new Vector4f();
    private boolean shown;
    private long time;
    private float x;
    private float y;

    public SpectatorCursor(@NonNull WorldViewer viewer, @NonNull GUIRoot gui_root, @NonNull BooleanSupplier visible) {
        this.viewer = viewer;
        this.gui_root = gui_root;
        this.visible = visible;
        this.texture = Resources.findResource(new TextureFile(POINTER_TEXTURE, GL11.GL_RGBA, GL11.GL_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT));
        displayChangedNotify(gui_root.getWidth(), gui_root.getHeight());
    }

    @Override
    protected void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        SpectatorView view = viewer.getSpectatorView();
        Player followed = view != null && visible.getAsBoolean() ? view.getFollowedPlayer() : null;
        PlayerView player_view = followed != null ? view.getView(followed) : null;
        if (player_view == null || !player_view.isCursorOnMap()) {
            shown = false;
            return;
        }
        long now = System.nanoTime();
        if (shown) {
            float k = Math.min((now - time) / 1e9f * SMOOTHNESS_FACTOR, 1f);
            x += (player_view.getCursorX() - x) * k;
            y += (player_view.getCursorY() - y) * k;
        } else {
            x = player_view.getCursorX();
            y = player_view.getCursorY();
            shown = true;
        }
        time = now;
        float z = viewer.getWorld().getHeightMap().getNearestHeight(x, y);
        gui_root.projectToScreen(x, y, z, point);
        if (point.w < GUIRoot.MIN_PROJECTED_W)
            return;
        float screen_x = point.x;
        float screen_y = point.y;
        Vector4fc player_color = followed.getColor();
        color.set(player_color.x(), player_color.y(), player_color.z(), 1f);
        // A PNG texture has its top row at v=0, so the quad is drawn downwards from the hot spot.
        renderer.drawTexture(texture, screen_x - HOT_X, screen_y + HOT_Y, texture.getWidth(), -texture.getHeight(),
                0f, 0f, 1f, 1f, color);
    }
}

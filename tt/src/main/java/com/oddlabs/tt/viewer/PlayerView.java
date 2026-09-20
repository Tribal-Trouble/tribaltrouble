package com.oddlabs.tt.viewer;

import com.oddlabs.tt.model.Selectable;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Set;

public final class PlayerView {
    private final @NonNull Set<Selectable<?>> selection = new HashSet<>();
    private boolean cursor_on_map;
    private float cursor_x;
    private float cursor_y;
    private boolean has_camera;
    private float camera_x;
    private float camera_y;
    private float camera_z;
    private float camera_horiz_angle;
    private float camera_vert_angle;

    void setCamera(float x, float y, float z, float horiz_angle, float vert_angle) {
        has_camera = true;
        camera_x = x;
        camera_y = y;
        camera_z = z;
        camera_horiz_angle = horiz_angle;
        camera_vert_angle = vert_angle;
    }

    public boolean hasCamera() {
        return has_camera;
    }

    public float getCameraX() {
        return camera_x;
    }

    public float getCameraY() {
        return camera_y;
    }

    public float getCameraZ() {
        return camera_z;
    }

    public float getCameraHorizAngle() {
        return camera_horiz_angle;
    }

    public float getCameraVertAngle() {
        return camera_vert_angle;
    }

    void setCursor(float x, float y, boolean on_map) {
        cursor_on_map = on_map;
        cursor_x = x;
        cursor_y = y;
    }

    public boolean isCursorOnMap() {
        return cursor_on_map;
    }

    public float getCursorX() {
        return cursor_x;
    }

    public float getCursorY() {
        return cursor_y;
    }

    void setSelection(Selectable<?> @NonNull [] selected) {
        selection.clear();
        for (Selectable<?> s : selected) {
            if (s != null)
                selection.add(s);
        }
    }

    public boolean isSelected(@NonNull Selectable<?> selectable) {
        return !selectable.isDead() && selection.contains(selectable);
    }
}

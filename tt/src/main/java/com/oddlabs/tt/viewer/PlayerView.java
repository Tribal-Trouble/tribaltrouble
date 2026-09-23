package com.oddlabs.tt.viewer;

import com.oddlabs.tt.model.Selectable;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PlayerView {
    private final @NonNull Set<Selectable<?>> selection = new HashSet<>();
    private int selection_version;
    private int panel_submenu;
    private boolean map_mode;
    private boolean targeting;
    private boolean placing;
    private int placing_building_index;
    private int placing_grid_x;
    private int placing_grid_y;
    private boolean box_active;
    private float box_x1;
    private float box_y1;
    private float box_x2;
    private float box_y2;
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

    void setMapMode(boolean on) {
        map_mode = on;
    }

    public boolean isMapMode() {
        return map_mode;
    }

    void setSelectionBox(float x1, float y1, float x2, float y2, boolean active) {
        box_active = active;
        box_x1 = x1;
        box_y1 = y1;
        box_x2 = x2;
        box_y2 = y2;
    }

    public boolean isSelectionBoxActive() {
        return box_active;
    }

    /** Box corners as fractions of the player's viewport. */
    public float getSelectionBoxX1() {
        return box_x1;
    }

    public float getSelectionBoxY1() {
        return box_y1;
    }

    public float getSelectionBoxX2() {
        return box_x2;
    }

    public float getSelectionBoxY2() {
        return box_y2;
    }

    void setPlacing(int building_index, int grid_x, int grid_y, boolean on) {
        placing = on;
        placing_building_index = building_index;
        placing_grid_x = grid_x;
        placing_grid_y = grid_y;
    }

    public boolean isPlacing() {
        return placing;
    }

    public int getPlacingBuildingIndex() {
        return placing_building_index;
    }

    public int getPlacingGridX() {
        return placing_grid_x;
    }

    public int getPlacingGridY() {
        return placing_grid_y;
    }

    void setTargeting(boolean on) {
        targeting = on;
    }

    /** True while the player is picking a spot for a move, attack, rally point or beacon. */
    public boolean isTargeting() {
        return targeting;
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
        selection_version++;
        for (Selectable<?> s : selected) {
            if (s != null)
                selection.add(s);
        }
    }

    /** Changes whenever the selection is replaced, so a copy knows when to refresh. */
    public int getSelectionVersion() {
        return selection_version;
    }

    public @NonNull Set<Selectable<?>> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    void setPanelSubmenu(int submenu) {
        panel_submenu = submenu;
    }

    public int getPanelSubmenu() {
        return panel_submenu;
    }

    public boolean isSelected(@NonNull Selectable<?> selectable) {
        return !selectable.isDead() && selection.contains(selectable);
    }
}

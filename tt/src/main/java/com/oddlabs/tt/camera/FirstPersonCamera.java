package com.oddlabs.tt.camera;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public final class FirstPersonCamera extends Camera {
    private static final float SCALE_HORIZ = .002f;
    private static final float SCALE_VERT = .002f;

    private final int base_x;
    private final int base_y;
    private final @NonNull WorldViewer viewer;

    public FirstPersonCamera(@NonNull WorldViewer viewer, HeightMap heightmap, @NonNull CameraState camera) {
        super(heightmap, camera);
        this.viewer = viewer;
        var window = Renderer.getRenderer().getWindow();
        this.base_x = window.getWidth() / 2;
        this.base_y = window.getHeight() / 2;
        Renderer.getLocalInput().getPointerInput().setCursorPosition(base_x, base_y);
    }

    @Override
    public void doAnimate(float t) {
        float dir_x = (float) Math.cos(getState().getTargetHorizAngle());
        float dir_y = (float) Math.sin(getState().getTargetHorizAngle());
        float left_dir_x = -dir_y;
        float left_dir_y = dir_x;

        float scrolling_x = 0;
        float scrolling_y = 0;
        var inputManager = Renderer.getLocalInput().getInputManager();
        if (inputManager.isActive(GameAction.CAMERA_PAN_LEFT) && !inputManager.isActive(GameAction.CAMERA_PAN_RIGHT))
            scrolling_x = -1f;
        else if (inputManager.isActive(GameAction.CAMERA_PAN_RIGHT) && !inputManager.isActive(GameAction.CAMERA_PAN_LEFT))
            scrolling_x = 1f;

        if (inputManager.isActive(GameAction.CAMERA_PAN_DOWN) && !inputManager.isActive(GameAction.CAMERA_PAN_UP))
            scrolling_y = -1f;
        else if (inputManager.isActive(GameAction.CAMERA_PAN_UP) && !inputManager.isActive(GameAction.CAMERA_PAN_DOWN))
            scrolling_y = 1f;

        float scroll_factor = getState().getTargetZ() * t;
        float new_x = getState().getTargetX() - (scrolling_x * left_dir_x + scrolling_y * -left_dir_y) * scroll_factor;
        float new_y = getState().getTargetY() - (scrolling_x * left_dir_y + scrolling_y * left_dir_x) * scroll_factor;

        if (new_x != getState().getTargetX() || new_y != getState().getTargetY()) {
            getState().setTargetX(new_x);
            getState().setTargetY(new_y);
            checkPosition();
        }
    }

    @Override
    public void mouseMoved(int x, int y) {
        // Ignore logical x/y; use physical coordinates from LocalInput to maintain constant
        // rotation sensitivity and match PointerInput locking requirements.
        var localInput = Renderer.getLocalInput();
        int dx = localInput.getMouseX() - base_x;
        int dy = localInput.getMouseY() - base_y;
        getState().setTargetHorizAngle(getState().getTargetHorizAngle() - dx * SCALE_HORIZ);
        if (Settings.getSettings().invert_camera_pitch)
            getState().setTargetVertAngle(getState().getTargetVertAngle() - dy * SCALE_VERT);
        else
            getState().setTargetVertAngle(getState().getTargetVertAngle() + dy * SCALE_VERT);

        Renderer.getLocalInput().getPointerInput().setCursorPosition(base_x, base_y);
    }
}

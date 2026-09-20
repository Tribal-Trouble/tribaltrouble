package com.oddlabs.tt.camera;

import com.oddlabs.tt.viewer.PlayerView;
import com.oddlabs.tt.viewer.SpectatorView;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

/** Game camera that follows the watched player's camera until the spectator takes manual control. */
public final class SpectatorGameCamera extends GameCamera {
    private final @NonNull SpectatorView view;

    public SpectatorGameCamera(
            @NonNull WorldViewer viewer, @NonNull CameraState camera, @NonNull SpectatorView view) {
        super(viewer, camera);
        this.view = view;
    }

    @Override
    public void manualControl() {
        view.freeCamera();
    }

    @Override
    protected void doControl(float t) {
        if (!followView())
            super.doControl(t);
    }

    private boolean followView() {
        PlayerView followed = view.getFollowedView();
        if (followed == null)
            return false;
        CameraState state = getState();
        state.setMaxVertAngle(CameraState.MAX_ANGLE_UNLOCKED);
        state.setTargetX(followed.getCameraX());
        state.setTargetY(followed.getCameraY());
        state.setTargetZ(followed.getCameraZ());
        state.setTargetHorizAngle(nearestAngle(followed.getCameraHorizAngle(), state.getHorizAngle()));
        state.setTargetVertAngle(followed.getCameraVertAngle());
        if (view.consumeSnap())
            state.snapToTarget();
        return true;
    }

    private static float nearestAngle(float angle, float reference) {
        double turns = Math.rint((angle - reference) / (2 * Math.PI));
        return (float) (angle - turns * 2 * Math.PI);
    }
}

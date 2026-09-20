package com.oddlabs.tt.camera;

import com.oddlabs.tt.viewer.PlayerView;
import com.oddlabs.tt.viewer.SpectatorView;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

/** Game camera that follows the watched player's camera until anything else moves it. */
public final class SpectatorGameCamera extends GameCamera {
    private final @NonNull SpectatorView view;
    private boolean following;
    private float followed_x;
    private float followed_y;
    private float followed_z;
    private float followed_horiz_angle;
    private float followed_vert_angle;

    public SpectatorGameCamera(
            @NonNull WorldViewer viewer, @NonNull CameraState camera, @NonNull SpectatorView view) {
        super(viewer, camera);
        this.view = view;
    }

    @Override
    protected boolean limitsUnlocked() {
        return following || super.limitsUnlocked();
    }

    @Override
    protected boolean edgeScrollEnabled() {
        return !following;
    }

    @Override
    protected void doControl(float t) {
        PlayerView followed = view.getFollowedView();
        boolean snap = followed != null && view.consumeSnap();
        if (followed != null && following && !snap && !atFollowedTarget()) {
            view.freeCamera();
            followed = null;
        }
        following = followed != null;
        if (following)
            follow(followed);
        super.doControl(t);
        if (!following)
            return;
        if (!atFollowedTarget()) {
            view.freeCamera();
            following = false;
        } else if (snap) {
            getState().snapToTarget();
        }
    }

    private void follow(@NonNull PlayerView followed) {
        CameraState state = getState();
        state.setMaxVertAngle(CameraState.MAX_ANGLE_UNLOCKED);
        state.setTargetX(followed.getCameraX());
        state.setTargetY(followed.getCameraY());
        state.setTargetZ(followed.getCameraZ());
        state.setTargetHorizAngle(nearestAngle(followed.getCameraHorizAngle(), state.getHorizAngle()));
        state.setTargetVertAngle(followed.getCameraVertAngle());
        followed_x = state.getTargetX();
        followed_y = state.getTargetY();
        followed_z = state.getTargetZ();
        followed_horiz_angle = state.getTargetHorizAngle();
        followed_vert_angle = state.getTargetVertAngle();
    }

    private boolean atFollowedTarget() {
        CameraState state = getState();
        return state.getTargetX() == followed_x && state.getTargetY() == followed_y
                && state.getTargetZ() == followed_z && state.getTargetHorizAngle() == followed_horiz_angle
                && state.getTargetVertAngle() == followed_vert_angle;
    }

    private static float nearestAngle(float angle, float reference) {
        double turns = Math.rint((angle - reference) / (2 * Math.PI));
        return (float) (angle - turns * 2 * Math.PI);
    }
}

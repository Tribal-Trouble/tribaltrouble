package com.oddlabs.tt.viewer;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.delegate.SelectionDelegate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Takes the spectator in and out of map mode with the watched player; the spectator's own map key frees the camera. */
final class SpectatorMapMode implements Animated {
    private final @NonNull WorldViewer viewer;
    private final @NonNull SpectatorView view;
    private @Nullable PlayerView last_followed;
    private boolean last_followed_map_mode;
    private boolean last_own_map_mode;
    private boolean driving;
    private boolean drove_in;

    SpectatorMapMode(@NonNull WorldViewer viewer, @NonNull SpectatorView view) {
        this.viewer = viewer;
        this.view = view;
    }

    @Override
    public void animate(float t) {
        SelectionDelegate delegate = viewer.getDelegate();
        boolean own_map_mode = delegate.isOnMap();
        PlayerView followed = view.getFollowedView();
        if (followed == null && own_map_mode && drove_in && delegate.getCamera() instanceof MapCamera map_camera) {
            map_camera.leaveMap();
            drove_in = false;
        }
        if (followed == null || viewer.getGUIRoot().getDelegate() != delegate) {
            last_followed = followed;
            last_followed_map_mode = followed != null && followed.isMapMode();
            last_own_map_mode = own_map_mode;
            driving = false;
            return;
        }
        boolean followed_map_mode = followed.isMapMode();
        boolean followed_changed = followed != last_followed || followed_map_mode != last_followed_map_mode;
        last_followed = followed;
        if (own_map_mode == followed_map_mode) {
            driving = false;
        } else if (followed_changed || driving) {
            driving = true;
            if (own_map_mode) {
                ((MapCamera) delegate.getCamera()).leaveMap();
                drove_in = false;
            } else {
                delegate.enterMapMode();
                drove_in = true;
            }
        } else if (own_map_mode != last_own_map_mode) {
            view.freeCamera();
        }
        last_followed_map_mode = followed_map_mode;
        last_own_map_mode = own_map_mode;
    }
}

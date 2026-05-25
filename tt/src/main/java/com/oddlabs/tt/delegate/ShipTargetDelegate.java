package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.tt.gui.CursorType;

import org.jspecify.annotations.NonNull;

public final class ShipTargetDelegate extends ControllableCameraDelegate {
    private static final int GRID_RADIUS = 20;

    private final @NonNull Action action;

    public ShipTargetDelegate(
            @NonNull WorldViewer viewer,
            @NonNull GameCamera camera,
            @NonNull Action action) {
        super(viewer, camera);
        this.action = action;
    }

    public boolean canHoverBehind() {
        return true;
    }

    @Override
    protected final @NonNull CursorType getCursorType() {
        return CursorType.TARGET;
    }

    public final void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.LEFT) {
            getViewer().getPicker().pickSailingTarget(
                    getViewer().getSelection().getCurrentSelection(),
                    getViewer().getGUIRoot().getDelegate().getCamera().getState(),
                    getViewer().getPeerHub().getPlayerInterface(),
                    x,
                    y);
            pop();
        } else {
            super.mousePressed(button, x, y);
        }
    }
}

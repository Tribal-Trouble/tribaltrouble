package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public final class RallyPointDelegate extends TargetDelegate {
    private final @NonNull Building building;

    public RallyPointDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera camera, @NonNull Building building) {
        super(viewer, camera, Action.DEFAULT);
        this.building = building;
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (building.isDead()) {
            pop();
            return;
        }
        Target target = getViewer().getPicker().pickRallyPoint(
                getViewer().getGUIRoot().getDelegate().getCamera().getState(), x, y, building);
        if (target == null)
            return;
        if (building.isValidRallyPoint(target)) {
            getViewer().getPeerHub().getPlayerInterface().setRallyPoint(building, target);
        } else {
            getViewer().getPeerHub().getPlayerInterface().setRallyPoint(building, target.getGridX(), target.getGridY());
        }
        pop();
    }
}

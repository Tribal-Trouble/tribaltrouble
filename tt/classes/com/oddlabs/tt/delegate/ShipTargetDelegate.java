package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.ShipTargetScanFilter;
import com.oddlabs.tt.render.BuildingSiteRenderer;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.viewer.WorldViewer;

public final strictfp class ShipTargetDelegate extends ControllableCameraDelegate {
    private static final int GRID_RADIUS = 20;
    private static final LandscapeLocation landscape_hit = new LandscapeLocation();

    private final BuildingSiteRenderer site_renderer = new BuildingSiteRenderer();
    private final Ship ship;
    private final int action;

    public ShipTargetDelegate(WorldViewer viewer, GameCamera camera, Ship ship, int action) {
        super(viewer, camera);
        this.ship = ship;
        this.action = action;
    }

    public boolean canHoverBehind() {
        return true;
    }

    protected final int getCursorIndex() {
        return GUIRoot.CURSOR_TARGET;
    }

    public final void keyPressed(KeyboardEvent event) {
        getCamera().keyPressed(event);
        switch (event.getKeyCode()) {
            case Keyboard.KEY_ESCAPE:
                pop();
                break;
            case Keyboard.KEY_SPACE:
            case Keyboard.KEY_RETURN:
                break;
            default:
                super.keyPressed(event);
                break;
        }
    }

    public final void keyReleased(KeyboardEvent event) {
        if (event.getKeyCode() != Keyboard.KEY_SPACE || event.getKeyCode() != Keyboard.KEY_RETURN)
            getCamera().keyReleased(event);
    }

    public final void mousePressed(int button, int x, int y) {
        if (button == LocalInput.LEFT_BUTTON) {
            getViewer()
                    .getPicker()
                    .pickSailingTarget(
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

    public final void render3D(LandscapeRenderer renderer, RenderQueues queues) {
        if (ship.isDead()) return;
        if (!getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit)) return;

        UnitGrid unit_grid = getViewer().getWorld().getUnitGrid();
        int target_grid_x = UnitGrid.toGridCoordinate(landscape_hit.x);
        int target_grid_y = UnitGrid.toGridCoordinate(landscape_hit.y);
        float center_x = UnitGrid.coordinateFromGrid(target_grid_x);
        float center_y = UnitGrid.coordinateFromGrid(target_grid_y);

        ShipTargetScanFilter filter = new ShipTargetScanFilter(unit_grid, ship, GRID_RADIUS, false);
        unit_grid.scan(filter, target_grid_x, target_grid_y, UnitGrid.SEA);
        site_renderer.setSea(false);
        site_renderer.renderSites(
                renderer, filter.getResult(), center_x, center_y, 2 * GRID_RADIUS);

        ShipTargetScanFilter filter2 = new ShipTargetScanFilter(unit_grid, ship, GRID_RADIUS, true);
        unit_grid.scan(filter2, target_grid_x, target_grid_y, UnitGrid.SEA);
        site_renderer.setSea(true);
        site_renderer.renderSites(
                renderer, filter2.getResult(), center_x, center_y, 2 * GRID_RADIUS);
    }
}

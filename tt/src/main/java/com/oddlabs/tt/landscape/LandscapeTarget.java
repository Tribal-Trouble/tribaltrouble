package com.oddlabs.tt.landscape;

import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.model.Target;
import org.jspecify.annotations.NonNull;

public record LandscapeTarget(int grid_x, int grid_y) implements Target {

    @Override
    public float getPositionX() {
        return UnitGrid.coordinateFromGrid(grid_x);
    }

    @Override
    public float getPositionY() {
        return UnitGrid.coordinateFromGrid(grid_y);
    }

    @Override
    public int getGridX() {
        return grid_x;
    }

    @Override
    public int getGridY() {
        return grid_y;
    }

    @Override
    public float getSize() {
        return 0;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public @NonNull String toString() {
        return "LandscapeTarget: grid_x = " + grid_x + " | grid_y = " + grid_y;
    }
}

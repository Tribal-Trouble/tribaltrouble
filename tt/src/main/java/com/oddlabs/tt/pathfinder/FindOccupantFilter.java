package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.model.Selectable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class FindOccupantFilter<S extends Selectable> implements ScanFilter {

    private final float x;
    private final float y;
    private final float radius;
    private final Selectable src;
    private final Class<S> type;
    private final @NonNull List<@NonNull S> result;

    public FindOccupantFilter(float x, float y, float radius, Selectable src, Class<S> type) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.src = src;
        this.type = type;
        result = new ArrayList<>();
    }

    @Override
    public int getMinRadius() {
        return 0;
    }

    @Override
    public int getMaxRadius() {
        return UnitGrid.toGridCoordinate(radius);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean filter(int grid_x, int grid_y, Occupant occ) {
        if (occ != src && type.isInstance(occ)) {
            S s = (S) occ;
            float dx = s.getPositionX() - x;
            float dy = s.getPositionY() - y;
            float squared_dist = dx * dx + dy * dy;
            if (!result.contains(s) && squared_dist < radius * radius) {
                result.add(s);
                return true;
            }
        }
        return false;
    }

    public @NonNull List<@NonNull S> getResult() {
        return result;
    }
}

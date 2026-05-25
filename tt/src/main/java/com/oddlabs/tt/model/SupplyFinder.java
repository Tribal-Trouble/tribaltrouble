package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.FinderFilter;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.RegionBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SupplyFinder<S extends Supply> implements FinderFilter<S> {
    private final @NonNull Unit unit;
    private final @NonNull Class<S> supply_class;
    private final List<@NonNull List<@NonNull S>> region_list = new ArrayList<>();
    private int max_region_dist_sqr;

    public SupplyFinder(@NonNull Unit unit, @NonNull Class<S> supply_class) {
        this.unit = unit;
        this.supply_class = supply_class;
    }

    @Override
    public @Nullable S getOccupantFromRegion(@NonNull Region region, boolean one_region) {
        List<S> supplies = region.getObjects(supply_class);
        if (one_region) {
            if (!supplies.isEmpty()) {
                S supply = findClosest(supplies);
                assert !supply.isEmpty();
                return supply;
            }
        } else {
            int dx = region.getGridX() - unit.getGridX();
            int dy = region.getGridY() - unit.getGridY();
            int region_dist_sqr = dx * dx + dy * dy;
            if (!supplies.isEmpty()) {
                if (region_list.isEmpty()) {
                    int region_dist = (int) Math.sqrt(region_dist_sqr);
                    int max_region_dist = region_dist + RegionBuilder.REGION_PATH_MAX_COST / 2;
                    max_region_dist_sqr = max_region_dist * max_region_dist;
                }
                region_list.add(supplies);
            }
            if (!region_list.isEmpty() && region_dist_sqr > max_region_dist_sqr) {
                S supply = findClosest();
                assert !supply.isEmpty();
                return supply;
            }
        }
        return null;
    }

    @Override
    public S getBest() {
        return findClosest();
    }

    private @Nullable S findClosest(@NonNull List<S> supplies) {
        return supplies.stream().min(Comparator.comparingInt(this::distanceSquared)).orElse(null);
    }

    private @Nullable S findClosest() {
        S closest = region_list.stream().flatMap(List::stream).min(Comparator.comparingInt(
                this::distanceSquared)).orElse(null);
        region_list.clear();
        return closest;
    }

    private int distanceSquared(@NonNull S supply) {
        int dx = supply.getGridX() - unit.getGridX();
        int dy = supply.getGridY() - unit.getGridY();
        return dx * dx + dy * dy;
    }

    @Override
    public boolean acceptOccupant(@NonNull Occupant occ) {
        if (supply_class.isInstance(occ)) {
            Supply supply = (Supply) occ;
            assert !supply.isEmpty();
            return true;
        } else
            return false;
    }
}

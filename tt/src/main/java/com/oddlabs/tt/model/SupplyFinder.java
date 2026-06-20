package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.FinderFilter;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.RegionBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Finder filter that searches for closest available resources of a specific type.
 */
public final class SupplyFinder<S extends Supply> implements FinderFilter<S> {
    private final @NonNull Unit unit;
    private final @NonNull SupplyType supplyType;
    private final Set<@NonNull Set<@NonNull S>> regions = new CopyOnWriteArraySet<>();
    private int max_region_dist_sqr;

    public SupplyFinder(@NonNull Unit unit, @NonNull SupplyType supplyType) {
        this.unit = unit;
        this.supplyType = supplyType;
    }

    @Override
    public @Nullable S getOccupantFromRegion(@NonNull Region region, boolean one_region) {
        @SuppressWarnings("unchecked") Class<S> supplyClass = (Class<S>) supplyType.getSupplyClass();
        Set<S> supplies = region.getObjects(supplyClass);
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
                if (regions.isEmpty()) {
                    int region_dist = (int) Math.sqrt(region_dist_sqr);
                    int max_region_dist = region_dist + RegionBuilder.REGION_PATH_MAX_COST / 2;
                    max_region_dist_sqr = max_region_dist * max_region_dist;
                }
                regions.add(supplies);
            }
            if (!regions.isEmpty() && region_dist_sqr > max_region_dist_sqr) {
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

    private @Nullable S findClosest(@NonNull Set<S> supplies) {
        return supplies.stream()
                .min(Comparator.comparingInt(this::distanceSquared))
                .orElse(null);
    }

    private @Nullable S findClosest() {
        S closest = regions.stream()
                .flatMap(Set::stream)
                .min(Comparator.comparingInt(this::distanceSquared))
                .orElse(null);
        regions.clear();
        return closest;
    }

    private int distanceSquared(@NonNull S supply) {
        int dx = supply.getGridX() - unit.getGridX();
        int dy = supply.getGridY() - unit.getGridY();
        return dx * dx + dy * dy;
    }

    @Override
    public boolean acceptOccupant(@NonNull Occupant occ) {
        if (supplyType.getSupplyClass().isInstance(occ)) {
            Supply supply = (Supply) occ;
            assert !supply.isEmpty();
            return true;
        } else
            return false;
    }
}

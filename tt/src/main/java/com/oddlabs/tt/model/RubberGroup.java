package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RubberGroup {
    private static final int MIN_CHICKENS_PER_GROUP = 3;
    private static final int MAX_CHICKENS_PER_GROUP = 7;

    private static final float SPAWN_TIME = 2f;

    private final @NonNull World world;
    private final List<Supply> supplies = new ArrayList<>();

    public RubberGroup(@NonNull World world) {
        this.world = world;
        int[] group_position = getGroupPosition();
        if (group_position != null) {
            int num_supplies = world.getRandom().nextInt(MIN_CHICKENS_PER_GROUP,
                    MAX_CHICKENS_PER_GROUP + 1);
            Target[] supply_positions = world.getUnitGrid().findGridTargets(group_position[0], group_position[1],
                    num_supplies, true);
            float spawn_x = UnitGrid.coordinateFromGrid(group_position[0]);
            float spawn_y = UnitGrid.coordinateFromGrid(group_position[1]);
            for (int i = 0; i < num_supplies; i++) {
                Target target = supply_positions[i];
                if (target == null) continue;
                int grid_x = target.getGridX();
                int grid_y = target.getGridY();
                float x = UnitGrid.coordinateFromGrid(grid_x);
                float y = UnitGrid.coordinateFromGrid(grid_y);
                RubberSupply supply = new RubberSupply(world, grid_x, grid_y, x, y, this, spawn_x, spawn_y);
                supplies.add(supply);
                new SupplySpawnAnimation(supply, supply.getSpawnTime());
            }
            ((RubberSupplyManager) world.getSupplyManager(SupplyType.RUBBER)).newGroup();
        }
    }

    private int @Nullable [] getGroupPosition() {
        List<int[]> tree_positions = world.getHeightMap().getTrees();
        int start_index = world.getRandom().nextInt(tree_positions.size());
        int index = (start_index + 1) % tree_positions.size();
        while (index != start_index) {
            int[] coords = tree_positions.get(index);
            Occupant occ = world.getUnitGrid().getOccupant(coords[0], coords[1]);
            if (occ instanceof TreeSupply)
                return coords;
            index = (index + 1) % tree_positions.size();
        }
        return null;
    }

    public void remove(RubberSupply supply) {
        boolean in_list = supplies.remove(supply);
        assert in_list;
        if (supplies.isEmpty())
            ((RubberSupplyManager) world.getSupplyManager(SupplyType.RUBBER)).emptyGroup();
    }
}

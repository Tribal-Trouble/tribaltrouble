package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.util.LinkedList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Abstract base class for spatial partition tree nodes in the world entity grid.
 */
public abstract sealed class AbstractElementNode<T extends Element<T>> extends BoundingBox permits ElementNode,
        ElementLeaf {
    private final LinkedList<T> models = new LinkedList<>();

    private int child_count = 0;

    private final @Nullable AbstractElementNode<T> owner;

    protected AbstractElementNode(@Nullable AbstractElementNode<T> owner) {
        this.owner = owner;
    }

    protected final int getChildCount() {
        return child_count - models.size();
    }

    protected final AbstractElementNode<T> insertElement(@NonNull T model) {
        checkBoundsZ(model.bmin_z);
        checkBoundsZ(model.bmax_z);
        return doInsertElement(model);
    }

    protected abstract AbstractElementNode<T> doInsertElement(T model);

    public final void removeElement(@NonNull T model) {
        models.remove(model);
    }

    protected final void incElementCount() {
        child_count++;
    }

    protected final AbstractElementNode<T> reinsertElement(@NonNull T model) {
        child_count--;
        assert child_count >= 0;
        if (contains(model) || owner == null) {
            return insertElement(model);
        } else {
            return owner.reinsertElement(model);
        }
    }

    private int getDepth() {
        if (owner != null)
            return 1 + owner.getDepth();
        else
            return 0;
    }

    protected final @NonNull AbstractElementNode<T> addElement(@NonNull T model) {
        models.addLast(model);
        return this;
    }

    public final @NonNull LinkedList<T> getModels() {
        return models;
    }

    public static <T extends Element<T>> @NonNull AbstractElementNode<T> newRoot(@NonNull HeightMap heightmap) {
        AbstractElementNode<T> root = new ElementNode<>(null, heightmap.getGridUnitsPerWorld(), 0, 0);
        return root;
    }

    public static void buildSupplies(@NonNull World world, @NonNull List<int[]> iron_positions, @NonNull List<
            int[]> rock_positions, float @NonNull [] @NonNull [] plants, @NonNull Terrain terrain,
            boolean insertPlants) {
        buildRockSupplies(world, rock_positions);
        buildIronSupplies(world, iron_positions);
        if (insertPlants) {
            addPlants(world, plants, terrain);
        }
    }

    private static void buildRockSupplies(@NonNull World world, @NonNull List<int[]> positions) {
        IO.println("num_rocks = " + positions.size());
        positions.forEach(coords -> {
            int grid_x = coords[0];
            int grid_y = coords[1];
            float x = UnitGrid.coordinateFromGrid(grid_x) + world.getRandom().nextFloat(-.5f, .5f);
            float y = UnitGrid.coordinateFromGrid(grid_y) + world.getRandom().nextFloat(-.5f, .5f);
            new RockSupply(world, grid_x, grid_y, x, y, true).spawnComplete();
        });
    }

    private static void buildIronSupplies(@NonNull World world, @NonNull List<int[]> positions) {
        IO.println("num_iron = " + positions.size());
        positions.forEach(coords -> {
            int grid_x = coords[0];
            int grid_y = coords[1];
            float x = UnitGrid.coordinateFromGrid(grid_x) + world.getRandom().nextFloat(-.5f, .5f);
            float y = UnitGrid.coordinateFromGrid(grid_y) + world.getRandom().nextFloat(-.5f, .5f);
            new IronSupply(world, grid_x, grid_y, x, y, true).spawnComplete();
        });
    }

    public static void addPlants(@NonNull World world, float @NonNull [] @NonNull [] plants,
            @NonNull Terrain terrain) {
        int num_plants = 0;
        for (int t = 0; t < plants.length; t++) {
            num_plants += plants[t].length / 2;
            for (int p = 0; p < plants[t].length >> 1; p++) {
                float dir_x = java.util.concurrent.ThreadLocalRandom.current().nextFloat();
                float dir_y = java.util.concurrent.ThreadLocalRandom.current().nextFloat();
                float len_sqr = dir_x * dir_x + dir_y * dir_y;
                if (len_sqr < .001) {
                    dir_x = 1f;
                    dir_y = 0f;
                } else {
                    float inv_len = 1f / (float) Math.sqrt(len_sqr);
                    dir_x *= inv_len;
                    dir_y *= inv_len;
                }
                new Plants(world, plants[t][2 * p], plants[t][2 * p + 1], dir_x, dir_y,
                        world.getLandscapeResources().getPlantBounds(terrain, t));
            }
        }
        IO.println("num_plants = " + num_plants);
    }
}

package com.oddlabs.tt.landscape;
/**/

import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.model.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Base class representing hierarchical groupings of trees and individual tree supplies.
 */
public abstract sealed class AbstractTreeGroup extends BoundingBox permits TreeGroup, TreeLeaf, TreeSupply {

    public enum TreeType {
        JUNGLE(16.0f, 0.5f, 0.6f, 0.9f),
        PALM(20.0f, 0.3f, 1.0f, 0.95f),
        OAK(20.0f, 0.5f, 0.6f, 0.7f),
        PINE(12.0f, 0.6f, 0.3f, 0.65f);

        final float shadowDiameter;
        final float shadowOpacity;
        final float shadowVerticalCenter;
        public final float heightScale;

        TreeType(float shadowDiameter, float shadowOpacity, float shadowVerticalCenter, float heightScale) {
            this.shadowDiameter = shadowDiameter;
            this.shadowOpacity = shadowOpacity;
            this.shadowVerticalCenter = shadowVerticalCenter;
            this.heightScale = heightScale;
        }
    }

    private final @Nullable AbstractTreeGroup parent;

    private int num_responding_trees = 0;

    public AbstractTreeGroup(@Nullable AbstractTreeGroup parent) {
        this.parent = parent;
    }

    protected final @Nullable AbstractTreeGroup getParent() {
        return parent;
    }

    public final void changeRespondingTrees(int delta) {
        num_responding_trees += delta;
        if (parent != null)
            parent.changeRespondingTrees(delta);
    }

    public final boolean hasRespondingTrees() {
        return num_responding_trees > 0;
    }

    public static @NonNull AbstractTreeGroup newRoot(@NonNull World world, @NonNull List<int[]> tree_positions,
            @NonNull List<int[]> palm_tree_positions, @NonNull Terrain terrain) {
        AbstractTreeGroup root = new TreeGroup(null, 0);

        switch (terrain) {
            case NATIVE -> {
                root.buildTrees(world, TreeType.JUNGLE, 3, 2.3f, tree_positions, 0.25f, 0.75f);
                root.buildTrees(world, TreeType.PALM, 1, 1.6f, palm_tree_positions, 0.5f, 1f);
            }
            case VIKING -> {
                root.buildTrees(world, TreeType.OAK, 3, 2.3f, tree_positions, 0.5f, 1f);
                root.buildTrees(world, TreeType.PINE, 1, 1.6f, palm_tree_positions, 0.5f, 1f);
            }
        }

        root.initBounds();
        return root;
    }

    private void buildTrees(final @NonNull World world, final @NonNull TreeType tree_type, final int grid_size,
            final float radius, @NonNull List<int[]> tree_positions, float scale_factor, float min_size) {
        Matrix4f matrix2 = new Matrix4f();
        Vector3f vector = new Vector3f();
        // Generate dummy bounding box vertices for culling (Radius + Height 15m)
        float h = 15f;
        final float[] tree_low_vertices = new float[]{-radius, -radius, 0, radius, -radius, 0, radius, radius, 0, -radius, radius, 0, -radius, -radius, h, radius, -radius, h, radius, radius, h, -radius, radius, h
        };

        for (int[] coords : tree_positions) {
            final Matrix4f matrix = new Matrix4f();
            final int center_grid_x = coords[0];
            final int center_grid_y = coords[1];
            final float tree_x = UnitGrid.coordinateFromGrid(center_grid_x);
            final float tree_y = UnitGrid.coordinateFromGrid(center_grid_y);
            float rotation = world.getRandom().nextFloat(0f, 360f);
            float scale_base = scale_factor > 0f ? world.getRandom().nextFloat(min_size, min_size + scale_factor)
                    : min_size;
            float scale_x = scale_base + world.getRandom().nextFloat(-0.1f, 0.1f);
            float scale_y = scale_base + world.getRandom().nextFloat(-0.1f, 0.1f);
            float scale_z = scale_base + world.getRandom().nextFloat(-0.1f, 0.1f);
            matrix.identity();
            matrix.scale(scale_x, scale_y, scale_z);
            vector.set(0f, 0f, 1f);
            matrix.rotate((float) Math.toRadians(rotation), vector);
            matrix2.identity();
            matrix2.translate(tree_x, tree_y, world.getHeightMap().getNearestHeight(tree_x, tree_y));
            matrix2.mul(matrix, matrix);

            insertTreeRecursive(this, world, tree_type, grid_size, radius, matrix, tree_low_vertices, tree_x, tree_y,
                    center_grid_x, center_grid_y, world.getHeightMap().getMetersPerWorld(), 0, 0);
        }
    }

    private void insertTreeRecursive(@NonNull AbstractTreeGroup node, @NonNull World world, @NonNull TreeType tree_type,
            int grid_size, float radius, @NonNull Matrix4f matrix, float @NonNull [] vertices, float tree_x,
            float tree_y, int center_grid_x, int center_grid_y, int size, int x, int y) {
        switch (node) {
            case TreeLeaf leaf -> {
                TreeSupply tree = new TreeSupply(world, leaf, tree_x, tree_y, center_grid_x, center_grid_y, grid_size,
                        radius, matrix, tree_type, vertices);
                leaf.insertTree(tree);
            }
            case TreeGroup group -> {
                int child_size = size >> 1;
                int child_index = (tree_x < x + child_size ? 0 : 1) | (tree_y < y + child_size ? 0 : 2);
                int next_x = x + (child_index & 1) * child_size;
                int next_y = y + ((child_index >> 1) & 1) * child_size;
                insertTreeRecursive(group.child(child_index), world, tree_type, grid_size, radius, matrix, vertices,
                        tree_x, tree_y, center_grid_x, center_grid_y, child_size, next_x, next_y);
            }
            case TreeSupply _ -> throw new IllegalStateException("Unexpected TreeSupply node in tree hierarchy");
        }
    }

    protected boolean initBounds() {
        return true;
    }
}

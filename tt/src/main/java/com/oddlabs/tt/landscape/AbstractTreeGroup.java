package com.oddlabs.tt.landscape;
/**/

import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class AbstractTreeGroup extends BoundingBox {

    public enum TreeType {
        JUNGLE,
        PALM,
        OAK,
        PINE
    }

    private final AbstractTreeGroup parent;

    private int num_responding_trees = 0;

    public AbstractTreeGroup(AbstractTreeGroup parent) {
        this.parent = parent;
    }

    protected final AbstractTreeGroup getParent() {
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
            @NonNull List<int[]> palm_tree_positions, Landscape.@NonNull TerrainType terrain) {
        AbstractTreeGroup root = new TreeGroup(null, 0);

        switch (terrain) {
            case NATIVE:
                root.buildTrees(world, TreeType.JUNGLE, tree_positions);
                root.buildTrees(world, TreeType.PALM, palm_tree_positions);
                break;
            case VIKING:
                root.buildTrees(world, TreeType.OAK, tree_positions);
                root.buildTrees(world, TreeType.PINE, palm_tree_positions);
                break;
        }

        root.initBounds();
        return root;
    }

    /** How big a kind of tree grows and how much ground it takes. */
    private record TreeKind(int grid_size, float radius, float scale_factor, float min_size) {
        static @NonNull TreeKind of(@NonNull TreeType tree_type) {
            return switch (tree_type) {
                case JUNGLE -> new TreeKind(3, 2.3f, 0.25f, 0.75f);
                case OAK -> new TreeKind(3, 2.3f, 0.5f, 1f);
                case PALM, PINE -> new TreeKind(1, 1.6f, 0.5f, 1f);
            };
        }
    }

    private void buildTrees(final @NonNull World world, final @NonNull TreeType tree_type,
            @NonNull List<int[]> tree_positions) {
        for (int[] coords : tree_positions)
            plant(world, tree_type, coords[0], coords[1]);
    }

    /**
     * Plants a tree on a grid position of an island that is already built, the way the island's own trees were
     * planted. Call this on the root, then {@link #refreshBounds} once done planting.
     */
    public final @NonNull TreeSupply plantTree(@NonNull World world, @NonNull TreeType tree_type, int grid_x,
            int grid_y) {
        return plant(world, tree_type, grid_x, grid_y);
    }

    /** Takes a tree out of the island for good. Call this on the root, then {@link #refreshBounds} once done. */
    public final void removeTree(@NonNull TreeSupply tree) {
        findLeaf(tree.getWorld(), tree.getPositionX(), tree.getPositionY()).takeOut(tree);
        tree.leaveWorld();
    }

    /** Brings the culling bounds up to date after trees were planted or removed. Call this on the root. */
    public final void refreshBounds() {
        initBounds();
    }

    private @NonNull TreeSupply plant(final @NonNull World world, final @NonNull TreeType tree_type,
            final int center_grid_x, final int center_grid_y) {
        TreeKind kind = TreeKind.of(tree_type);
        float radius = kind.radius();
        Matrix4f matrix2 = new Matrix4f();
        Vector3f vector = new Vector3f();
        // Generate dummy bounding box vertices for culling (Radius + Height 15m)
        float h = 15f;
        final float[] tree_low_vertices = new float[]{-radius, -radius, 0, radius, -radius, 0, radius, radius, 0, -radius, radius, 0, -radius, -radius, h, radius, -radius, h, radius, radius, h, -radius, radius, h
        };

        final Matrix4f matrix = new Matrix4f();
        final float tree_x = UnitGrid.coordinateFromGrid(center_grid_x);
        final float tree_y = UnitGrid.coordinateFromGrid(center_grid_y);
        float rotation = world.getRandom().nextFloat() * 360f;
        float scale_base = world.getRandom().nextFloat() * kind.scale_factor() + kind.min_size();
        float scale_x = scale_base + world.getRandom().nextFloat() * 0.2f - 0.1f;
        float scale_y = scale_base + world.getRandom().nextFloat() * 0.2f - 0.1f;
        float scale_z = scale_base + world.getRandom().nextFloat() * 0.2f - 0.1f;
        matrix.identity();
        matrix.scale(scale_x, scale_y, scale_z);
        vector.set(0f, 0f, 1f);
        matrix.rotate((float) Math.toRadians(rotation), vector);
        matrix2.identity();
        matrix2.translate(tree_x, tree_y, world.getHeightMap().getNearestHeight(tree_x, tree_y));
        matrix2.mul(matrix, matrix);
        TreeLeaf tree_leaf = findLeaf(world, tree_x, tree_y);
        TreeSupply tree = new TreeSupply(world, tree_leaf, tree_x, tree_y, center_grid_x, center_grid_y,
                kind.grid_size(), radius, matrix, tree_type, tree_low_vertices);
        tree_leaf.insertTree(tree);
        return tree;
    }

    /** The leaf holding the given position. Call this on the root, which spans the whole world. */
    private @NonNull TreeLeaf findLeaf(final @NonNull World world, float tree_x, float tree_y) {
        TreeLeaf[] found = new TreeLeaf[1];
        visit(new TreeNodeVisitor() {
            private int child_size = world.getHeightMap().getMetersPerWorld();
            private int x;
            private int y;

            @Override
            public void visitLeaf(@NonNull TreeLeaf tree_leaf) {
                found[0] = tree_leaf;
            }

            @Override
            public void visitNode(@NonNull TreeGroup tree_group) {
                int old_x = x;
                int old_y = y;
                int old_size = child_size;
                child_size >>= 1;
                if (tree_x < x + child_size) {
                    if (tree_y < y + child_size) {
                        tree_group.getChild0().visit(this);
                    } else {
                        y += child_size;
                        tree_group.getChild2().visit(this);
                    }
                } else {
                    if (tree_y < y + child_size) {
                        x += child_size;
                        tree_group.getChild1().visit(this);
                    } else {
                        x += child_size;
                        y += child_size;
                        tree_group.getChild3().visit(this);
                    }
                }
                x = old_x;
                y = old_y;
                child_size = old_size;
            }

            @Override
            public void visitTree(TreeSupply tree_supply) {
                throw new RuntimeException();
            }
        });
        return found[0];
    }

    public abstract void visit(TreeNodeVisitor visitor);

    protected boolean initBounds() {
        return true;
    }
}

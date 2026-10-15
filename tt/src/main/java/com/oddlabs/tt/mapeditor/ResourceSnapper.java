package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeNodeVisitor;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.AbstractElementNode;
import com.oddlabs.tt.model.ElementLeaf;
import com.oddlabs.tt.model.ElementNode;
import com.oddlabs.tt.model.ElementNodeVisitor;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts trees, rocks, iron and plants back on the ground after the terrain under them changed.
 *
 * <p>Rocks, iron and plants are models, which work out their height whenever they are placed, so they are simply
 * placed again where they stand. Trees keep their height in their transform, so it is moved directly and the tree
 * quadtree's bounds, which culling relies on, are rebuilt for the branches that were touched.
 */
final class ResourceSnapper {
    /** Meters around the edited area to include, since a model's slope offset looks at the ground around it. */
    private static final float MARGIN = 4f;

    private final @NonNull World world;
    private final @NonNull HeightMap height_map;
    private final List<@NonNull Model> models = new ArrayList<>();

    // The area being snapped, in meters.
    private float x0;
    private float y0;
    private float x1;
    private float y1;

    ResourceSnapper(@NonNull World world) {
        this.world = world;
        this.height_map = world.getHeightMap();
    }

    /** Snaps everything standing in the given rectangle of grid cells (inclusive). */
    void snap(int grid_x0, int grid_y0, int grid_x1, int grid_y1) {
        x0 = grid_x0 * HeightMap.METERS_PER_UNIT_GRID - MARGIN;
        y0 = grid_y0 * HeightMap.METERS_PER_UNIT_GRID - MARGIN;
        x1 = grid_x1 * HeightMap.METERS_PER_UNIT_GRID + MARGIN;
        y1 = grid_y1 * HeightMap.METERS_PER_UNIT_GRID + MARGIN;
        snapModels();
        snapTrees(world.getTreeRoot());
    }

    private boolean inside(float x, float y) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1;
    }

    private boolean overlaps(@NonNull BoundingBox box) {
        return box.bmin_x <= x1 && box.bmax_x >= x0 && box.bmin_y <= y1 && box.bmax_y >= y0;
    }

    // ---- Rocks, iron and plants ----

    @SuppressWarnings("unchecked")
    private void snapModels() {
        AbstractElementNode<Model> root = world.getElementRoot();
        root.visit(new ElementNodeVisitor<>() {
            @Override
            public void visitNode(ElementNode<Model> node) {
                if (overlaps(node)) {
                    node.visitElements(this);
                    node.visitChildren(this);
                }
            }

            @Override
            public void visitLeaf(ElementLeaf<Model> leaf) {
                if (overlaps(leaf))
                    leaf.visitElements(this);
            }

            @Override
            public void visit(Model model) {
                if ((model instanceof SupplyModel || model instanceof SceneryModel)
                        && inside(model.getPositionX(), model.getPositionY()))
                    models.add(model);
            }
        });
        // Placing a model moves it within the tree, so do it after the walk.
        for (Model model : models)
            model.setPosition(model.getPositionX(), model.getPositionY());
        models.clear();
    }

    // ---- Trees ----

    /**
     * Snaps the trees under a branch and refreshes its bounds.
     *
     * @return whether the branch holds any trees, and so has bounds
     */
    private boolean snapTrees(@NonNull AbstractTreeGroup group) {
        boolean has_bounds = group.bmin_x <= group.bmax_x;
        if (!has_bounds || !overlaps(group))
            return has_bounds;
        if (group instanceof TreeLeaf leaf)
            return snapLeaf(leaf);
        TreeGroup node = (TreeGroup) group;
        AbstractTreeGroup[] children = {node.getChild0(), node.getChild1(), node.getChild2(), node.getChild3()};
        boolean node_bounds = false;
        for (AbstractTreeGroup child : children) {
            if (!snapTrees(child))
                continue;
            if (node_bounds) {
                node.checkBounds(child);
            } else {
                node.setBounds(child);
                node_bounds = true;
            }
        }
        return node_bounds;
    }

    private boolean snapLeaf(@NonNull TreeLeaf leaf) {
        boolean[] leaf_bounds = {false};
        leaf.visitTrees(new TreeNodeVisitor() {
            @Override
            public void visitTree(TreeSupply tree) {
                if (inside(tree.getPositionX(), tree.getPositionY())) {
                    // Trees stand on the ground as it was generated, below the sea too, so no sea level clamp.
                    float z = height_map.getNearestHeight(tree.getPositionX(), tree.getPositionY());
                    float dz = z - tree.getMatrix().m32();
                    tree.getMatrix().m32(z);
                    tree.bmin_z += dz;
                    tree.bmax_z += dz;
                }
                if (leaf_bounds[0]) {
                    leaf.checkBounds(tree);
                } else {
                    leaf.setBounds(tree);
                    leaf_bounds[0] = true;
                }
            }

            @Override
            public void visitLeaf(TreeLeaf tree_leaf) {
                throw new IllegalStateException();
            }

            @Override
            public void visitNode(TreeGroup tree_group) {
                throw new IllegalStateException();
            }
        });
        return leaf_bounds[0];
    }
}

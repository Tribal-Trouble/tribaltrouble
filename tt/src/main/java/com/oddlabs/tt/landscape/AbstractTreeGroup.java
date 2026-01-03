package com.oddlabs.tt.landscape;

import com.oddlabs.tt.global.Globals;
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

	public static @NonNull AbstractTreeGroup newRoot(@NonNull World world, @NonNull List<int[]> tree_positions, @NonNull List<int[]> palm_tree_positions, Landscape.@NonNull TerrainType terrain) {
		AbstractTreeGroup root = new TreeGroup(null, 0);

		switch (terrain) {
			case NATIVE:
				root.buildTrees(world, TreeType.JUNGLE, 3, 2.3f, tree_positions, 0.25f, 0.75f);
				root.buildTrees(world, TreeType.PALM, 1, 1.6f, palm_tree_positions, 0.5f, 1f);
				break;
			case VIKING:
				root.buildTrees(world, TreeType.OAK, 3, 2.3f, tree_positions, 0.5f, 1f);
				root.buildTrees(world, TreeType.PINE, 1, 1.6f, palm_tree_positions, 0.5f, 1f);
				break;
		}

		root.initBounds();
		return root;
	}

	private void buildTrees(final @NonNull World world, final @NonNull TreeType tree_type, final int grid_size, final float radius, @NonNull List<int[]> tree_positions, float scale_factor, float min_size) {
		Matrix4f matrix2 = new Matrix4f();
		Vector3f vector = new Vector3f();
        // Generate dummy bounding box vertices for culling (Radius + Height 15m)
        float h = 15f;
        final float[] tree_low_vertices = new float[] {
            -radius, -radius, 0,
             radius, -radius, 0,
             radius,  radius, 0,
            -radius,  radius, 0,
            -radius, -radius, h,
             radius, -radius, h,
             radius,  radius, h,
            -radius,  radius, h
        };

		for (int[] coords : tree_positions) {
			final Matrix4f matrix = new Matrix4f();
			final int center_grid_x = coords[0];
			final int center_grid_y = coords[1];
			final float tree_x = UnitGrid.coordinateFromGrid(center_grid_x);
			final float tree_y = UnitGrid.coordinateFromGrid(center_grid_y);
			float rotation = world.getRandom().nextFloat()*360f;
			float scale_base = world.getRandom().nextFloat()*scale_factor + min_size;
			float scale_x = scale_base + world.getRandom().nextFloat()*0.2f - 0.1f;
			float scale_y = scale_base + world.getRandom().nextFloat()*0.2f - 0.1f;
			float scale_z = scale_base + world.getRandom().nextFloat()*0.2f - 0.1f;
			matrix.identity();
			matrix.scale(scale_x, scale_y, scale_z);
			vector.set(0f, 0f, 1f);
			matrix.rotate((float) Math.toRadians(rotation), vector);
			matrix2.identity();
			matrix2.translate(tree_x, tree_y, world.getHeightMap().getNearestHeight(tree_x, tree_y));
            matrix2.mul(matrix, matrix);
			visit(new TreeNodeVisitor() {
				private int child_size = world.getHeightMap().getMetersPerWorld();
				private int x;
				private int y;

				@Override
				public void visitLeaf(@NonNull TreeLeaf tree_leaf) {
					TreeSupply tree = new TreeSupply(world, tree_leaf, tree_x, tree_y, center_grid_x, center_grid_y, grid_size, radius, matrix, tree_type, tree_low_vertices);
					tree_leaf.insertTree(tree);
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
		}
	}

	public abstract void visit(TreeNodeVisitor visitor);

	protected boolean initBounds() {
		return true;
	}
}

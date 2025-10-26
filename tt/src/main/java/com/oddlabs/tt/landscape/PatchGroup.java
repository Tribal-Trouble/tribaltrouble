package com.oddlabs.tt.landscape;

import org.jspecify.annotations.NonNull;

public final class PatchGroup extends AbstractPatchGroup {
	/*
	 * child2 | child3
	 * ----------------
	 * child0 | child1
	 *
	 */

	private final @NonNull AbstractPatchGroup child0;
	private final @NonNull AbstractPatchGroup child1;
	private final @NonNull AbstractPatchGroup child2;
	private final @NonNull AbstractPatchGroup child3;

	public PatchGroup(@NonNull World world) {
		this(world, world.getHeightMap().getPatchesPerWorld(), 0, 0, 0, null);
	}

	public PatchGroup(@NonNull World world, int size, int x, int y, int level, AbstractPatchGroup parent) {
		super(world.getHeightMap(), size, x, y, parent);
		int child_size = size >> 1;
		child0 = createChild(world, child_size, x, y, level);
		child1 = createChild(world, child_size, x + child_size, y, level);
		child2 = createChild(world, child_size, x, y + child_size, level);
		child3 = createChild(world, child_size, x + child_size, y + child_size, level);

		setBounds(child0);
		checkBounds(child1);
		checkBounds(child2);
		checkBounds(child3);
	}

    @Override
	public void visit(@NonNull PatchGroupVisitor visitor) {
		visitor.visitGroup(this);
	}

	public void visitChildren(PatchGroupVisitor visitor) {
		child0.visit(visitor);
		child1.visit(visitor);
		child2.visit(visitor);
		child3.visit(visitor);
	}

	private @NonNull AbstractPatchGroup createChild(@NonNull World world, int size, int x, int y, int level) {
		if (size == 1) {
			LandscapeLeaf leaf = new LandscapeLeaf(world, x, y, this);
			return leaf;
		} else {
			PatchGroup group = new PatchGroup(world, size, x, y, level + 1, this);
			return group;
		}
	}
}

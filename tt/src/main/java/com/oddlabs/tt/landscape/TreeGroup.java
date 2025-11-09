package com.oddlabs.tt.landscape;

import org.jspecify.annotations.NonNull;

public final class TreeGroup extends AbstractTreeGroup {
	private final static int LANDSCAPE_TREES_MAX_LEVEL = 5;

	/*
	 * child2 | child3
	 * ----------------
	 * child0 | child1
	 *
	 */
	private final @NonNull AbstractTreeGroup child0;
	private final @NonNull AbstractTreeGroup child1;
	private final @NonNull AbstractTreeGroup child2;
	private final @NonNull AbstractTreeGroup child3;


	public TreeGroup(AbstractTreeGroup parent, int level) {
		super(parent);
		child0 = createChild(level);
		child1 = createChild(level);
		child2 = createChild(level);
		child3 = createChild(level);
	}

	private @NonNull AbstractTreeGroup createChild(int level) {
		if (level < LANDSCAPE_TREES_MAX_LEVEL) {
			return new TreeGroup(this, level + 1);
		} else {
			return new TreeLeaf(this);
		}
	}

	@Override
	public void visit(@NonNull TreeNodeVisitor visitor) {
		visitor.visitNode(this);
	}

	public @NonNull AbstractTreeGroup getChild0() {
		return child0;
	}

	public @NonNull AbstractTreeGroup getChild1() {
		return child1;
	}

	public @NonNull AbstractTreeGroup getChild2() {
		return child2;
	}

	public @NonNull AbstractTreeGroup getChild3() {
		return child3;
	}

	public void visitChildren(TreeNodeVisitor visitor) {
		child0.visit(visitor);
		child1.visit(visitor);
		child2.visit(visitor);
		child3.visit(visitor);
	}

	@Override
	protected boolean initBounds() {
		boolean child0_bounds = child0.initBounds();
		boolean child1_bounds = child1.initBounds();
		boolean child2_bounds = child2.initBounds();
		boolean child3_bounds = child3.initBounds();
		boolean node_bounds = false;
		node_bounds = checkBounds(child0, child0_bounds, node_bounds);
		node_bounds = checkBounds(child1, child1_bounds, node_bounds);
		node_bounds = checkBounds(child2, child2_bounds, node_bounds);
		node_bounds = checkBounds(child3, child3_bounds, node_bounds);
		super.initBounds();
		return node_bounds;
	}

	private boolean checkBounds(@NonNull AbstractTreeGroup child, boolean child_bounds, boolean node_bounds) {
		if (!child_bounds)
			return node_bounds;
		if (node_bounds)
			checkBounds(child);
		else
			setBounds(child);
		return true;
	}
}

package com.oddlabs.tt.pathfinder;

public final class GridPathNode implements PathNode {
	private final GridPathNode parent;
	private final DirectionNode dir;

	public GridPathNode(GridPathNode parent, DirectionNode dir) {
		this.parent = parent;
		this.dir = dir;
	}

	public DirectionNode getDirection() {
		return dir;
	}

	@Override
	public PathNode getParent() {
		return parent;
	}
}

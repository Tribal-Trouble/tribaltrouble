package com.oddlabs.tt.pathfinder;

public final class RegionNode implements PathNode {
	private final RegionNode parent;
	private final Region region;

	public RegionNode(RegionNode parent, Region region) {
		this.parent = parent;
		this.region = region;
	}

	public Region getRegion() {
		return region;
	}

	@Override
	public PathNode getParent() {
		return parent;
	}
}

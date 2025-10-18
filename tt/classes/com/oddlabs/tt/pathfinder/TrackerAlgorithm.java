package com.oddlabs.tt.pathfinder;

public interface TrackerAlgorithm {
	boolean isDone(int x, int y);
	boolean acceptRegion(Region region);
	Region findPathRegion(int src_x, int src_y);
	GridPathNode findPathGrid(Region target_region, Region next_region, int src_x, int src_y, boolean allow_secondary_targets);
}

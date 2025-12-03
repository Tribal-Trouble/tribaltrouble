package com.oddlabs.tt.pathfinder;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface TrackerAlgorithm {
	boolean isDone(int x, int y);
	boolean acceptRegion(@NonNull Region region);
	@Nullable Region findPathRegion(int src_x, int src_y);
	@Nullable GridPathNode findPathGrid(@NonNull Region target_region, @NonNull Region next_region, int src_x, int src_y, boolean allow_secondary_targets);
}

package com.oddlabs.tt.pathfinder;

import org.jspecify.annotations.Nullable;

public interface PathFinderAlgorithm {
    @Nullable NodeResult touchNode(Node node);

    @Nullable NodeResult getBestNode();

    int computeEstimatedCost(Node node);

    boolean touchNeighbour(Occupant occ);
}

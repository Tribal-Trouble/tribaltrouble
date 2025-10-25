package com.oddlabs.tt.pathfinder;

public interface PathFinderAlgorithm {
	NodeResult touchNode(Node node);
	NodeResult getBestNode();
	int computeEstimatedCost(Node node);
	boolean touchNeighbour(Occupant occ);
}

package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.util.PocketList;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PathFinder {
    private static final PocketList<@NonNull Node> open_list = new PocketList<>(RegionBuilder.MAX_PATH_COST);
    private static final List<@NonNull Node> visited_list = new ArrayList<>();
    public static int stat_pathfinder_per_frame = 0;

    public static Region findPathRegion(@NonNull UnitGrid unit_grid, @NonNull Region src_region, @NonNull Region dst_region) {
/*		Node src_region = unit_grid.getRegion(src_grid_x, src_grid_y);
		Node dst_region = unit_grid.getRegion(dst_grid_x, dst_grid_y);*/
        assert src_region != null;// : "src_grid_x = " + src_grid_x + " | src_grid_y = " + src_grid_y;
        assert dst_region != null;// : "dst_grid_x = " + dst_grid_x + " | dst_grid_y = " + dst_grid_y;
        PathFinderAlgorithm finder = new RegionPathFinder(unit_grid, dst_region);
        return (Region) doFindPath(finder, src_region, unit_grid);
    }

    public static Region findPathRegion(@NonNull UnitGrid unit_grid, @NonNull PathFinderAlgorithm finder, @NonNull Region current_region) {
//		Node current_region = UnitGrid.getGrid().getRegion(src_grid_x, src_grid_y);
        assert current_region != null;// : "src_grid_x = " + src_grid_x + " | src_grid_y = " + src_grid_y + " | occupant " + UnitGrid.getGrid().getOccupant(src_grid_x, src_grid_y);
        return (Region) doFindPath(finder, current_region, unit_grid);
    }

    public static @Nullable GridPathNode findPathGrid(UnitGrid unit_grid, @NonNull PathFinderAlgorithm finder, int src_grid_x, int src_grid_y) {
        GridNode.Offset offset = GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null)
            return null;
        Node current_node = GridNode.getPathfinderNode(offset, src_grid_x, src_grid_y);
        Node grid_node = doFindPath(finder, current_node, unit_grid);
        return grid_node != null ? (GridPathNode) grid_node.newPath() : null;
    }

    public static @Nullable GridPathNode findPathGrid(@NonNull UnitGrid unit_grid, Region dst_region, Region dst_region2, int src_grid_x, int src_grid_y, int dst_grid_x, int dst_grid_y, Target target, float max_dist, boolean allow_second_best) {
        GridNode.Offset offset = GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null)
            return null;
        Node current_node = GridNode.getPathfinderNode(offset, src_grid_x, src_grid_y);
        PathFinderAlgorithm finder = new TargetGridPathFinder(unit_grid, max_dist, dst_region, dst_region2, dst_grid_x, dst_grid_y, target, allow_second_best);
        Node grid_node = doFindPath(finder, current_node, unit_grid);
        return grid_node != null ? (GridPathNode) grid_node.newPath() : null;
    }

    private static Node doFindPath(@NonNull PathFinderAlgorithm finder, @Nullable Node start_node, UnitGrid unit_grid) {
        if (start_node == null)
            return null;
        Node current_node = start_node;
        stat_pathfinder_per_frame++;
        initSearch();
        current_node.setPathInitial(finder.computeEstimatedCost(current_node));
        addToLists(current_node);
        while (!open_list.isEmpty()) {
            current_node = open_list.removeBest();
            NodeResult result = finder.touchNode(current_node);
            if (result != null)
                return result.get();
            boolean neighbour_result = current_node.addNeighbours(finder, unit_grid);
            if (neighbour_result)
                return current_node;
        }
        NodeResult result = finder.getBestNode();
        return result != null ? result.get() : null;
    }

    public static void addToOpenList(@NonNull PathFinderAlgorithm finder, @NonNull Node current_node, @NonNull Node parent, int cost) {
        current_node.setPath(parent, cost, finder.computeEstimatedCost(current_node));
        addToLists(current_node);
    }

    private static void addToLists(@NonNull Node current_node) {
        open_list.add(current_node.getTotalCost(), current_node);
        visited_list.add(current_node);
    }

    private static void initSearch() {
        open_list.clear();
        for (Node node : visited_list) {
            node.reset();
        }
        visited_list.clear();
    }

    private PathFinder() {
        // no instances
    }
}

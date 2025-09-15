package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.util.PocketList;
import com.oddlabs.tt.util.Target;

import java.util.ArrayList;
import java.util.List;

public final strictfp class PathFinder {
    private static final PocketList open_list = new PocketList(RegionBuilder.MAX_PATH_COST);
    public static final List visited_list = new ArrayList();
    public static int stat_pathfinder_per_frame = 0;

    public static final Region findPathRegion(
            UnitGrid unit_grid, Region src_region, Region dst_region, int layer) {
        // TODO: This shouldn't happen. Remove workaround when sailing is handled
        if (src_region == null || dst_region == null) return null;
        assert src_region
                != null; // : "src_grid_x = " + src_grid_x + " | src_grid_y = " + src_grid_y;
        assert dst_region
                != null; // : "dst_grid_x = " + dst_grid_x + " | dst_grid_y = " + dst_grid_y;
        PathFinderAlgorithm finder = new RegionPathFinder(unit_grid, dst_region, layer);
        return (Region) doFindPath(finder, src_region, unit_grid, layer);
    }

    public static final Region findPathRegion(
            UnitGrid unit_grid, PathFinderAlgorithm finder, Region current_region, int layer) {
        if (current_region == null) return null;
        assert current_region
                != null; // : "src_grid_x = " + src_grid_x + " | src_grid_y = " + src_grid_y + " |
        return (Region) doFindPath(finder, current_region, unit_grid, layer);
    }

    public static final GridPathNode findPathGrid(
            UnitGrid unit_grid,
            PathFinderAlgorithm finder,
            int src_grid_x,
            int src_grid_y,
            int layer) {
        GridNode.Offset offset =
                GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null) return null;
        Node current_node = GridNode.getPathfinderNode(offset, src_grid_x, src_grid_y);
        Node grid_node = doFindPath(finder, current_node, unit_grid, layer);
        if (grid_node != null) return (GridPathNode) grid_node.newPath();
        else return null;
    }

    public static final GridPathNode findPathGrid(
            UnitGrid unit_grid,
            Region dst_region,
            Region dst_region2,
            int src_grid_x,
            int src_grid_y,
            int dst_grid_x,
            int dst_grid_y,
            Target target,
            float max_dist,
            boolean allow_second_best,
            int layer) {
        GridNode.Offset offset =
                GridNode.setupPathFinding(src_grid_x, src_grid_y, src_grid_x, src_grid_y);
        if (offset == null) return null;
        Node current_node = GridNode.getPathfinderNode(offset, src_grid_x, src_grid_y);
        PathFinderAlgorithm finder =
                new TargetGridPathFinder(
                        unit_grid,
                        max_dist,
                        dst_region,
                        dst_region2,
                        dst_grid_x,
                        dst_grid_y,
                        target,
                        allow_second_best,
                        layer);
        Node grid_node = doFindPath(finder, current_node, unit_grid, layer);
        if (grid_node != null) return (GridPathNode) grid_node.newPath();
        else return null;
    }

    private static final Node doFindPath(
            PathFinderAlgorithm finder, Node start_node, UnitGrid unit_grid, int layer) {
        if (start_node == null) return null;
        Node current_node = start_node;
        stat_pathfinder_per_frame++;
        initSearch();
        current_node.setPathInitial(finder.computeEstimatedCost(current_node));
        addToLists(current_node);
        while (open_list.size() != 0) {
            current_node = (Node) open_list.removeBest();
            NodeResult result = finder.touchNode(current_node);
            if (result != null) return result.get();
            boolean neighbour_result = current_node.addNeighbours(finder, unit_grid);
            if (neighbour_result) return current_node;
        }
        NodeResult result = finder.getBestNode();
        if (result != null) return result.get();
        else return null;
    }

    public static final void addToOpenList(
            PathFinderAlgorithm finder, Node current_node, Node parent, int cost) {
        current_node.setPath(parent, cost, finder.computeEstimatedCost(current_node));
        addToLists(current_node);
    }

    private static final void addToLists(Node current_node) {
        open_list.add(current_node.getTotalCost(), current_node);
        visited_list.add(current_node);
    }

    private static final void initSearch() {
        open_list.clear();
        for (int i = 0; i < visited_list.size(); i++) {
            Node node = (Node) visited_list.get(i);
            node.reset();
        }
        visited_list.clear();
    }
}

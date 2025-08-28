package com.oddlabs.tt.pathfinder;

abstract strictfp class GridPathFinder extends AStarAlgorithm {
    private final Node dst_region;
    private final Node dst_region2;

    public GridPathFinder(
            UnitGrid unit_grid,
            Node dst_region,
            Node dst_region2,
            int dst_x,
            int dst_y,
            boolean allow_second_best,
            int layer) {
        super(unit_grid, dst_x, dst_y, allow_second_best, layer);
        this.dst_region = dst_region;
        this.dst_region2 = dst_region2;
    }

    protected boolean isPathComplete(int dist_squared, Node node) {
        GridNode grid_node = (GridNode) node;
        Region region =
                getUnitGrid().getRegion(grid_node.getGridX(), grid_node.getGridY(), getLayer());
        return region == dst_region || region == dst_region2;
    }
}

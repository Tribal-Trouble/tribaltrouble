package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.util.PocketList;

import java.util.*;

public final strictfp class RegionBuilder {
    public static final int MAX_EXAMINED_NODES_PER_PATH = 600;
    public static final int REGION_PATH_MAX_COST = 70;
    public static final int MAX_PATH_COST = 2048;
    public static final int GRID_SIZE = 128;

    public static final int DIAGONAL = 3;
    public static final int STRAIGHT = 2;

    private static final Occupant unreachable_obj = new StaticOccupant();

    public static final void buildRegions(UnitGrid unit_grid, float start_x_f, float start_y_f) {
        boolean[][] access_grid = unit_grid.getHeightMap().getAccessGrid();
        boolean[][] dock_grid = unit_grid.getHeightMap().getDockGrid();
        boolean[][] water_grid = unit_grid.getHeightMap().getWaterGrid();
        ArrayList island_locations = unit_grid.getHeightMap().getIslandLocations();
        int grid_size = access_grid.length;
        int start_x = UnitGrid.toGridCoordinate(start_x_f);
        int start_y = UnitGrid.toGridCoordinate(start_y_f);

        RegionBuilderNode[][] dir_finder_grid = new RegionBuilderNode[grid_size][grid_size];
        RegionBuilderNode[][] dir_finder_water_grid = new RegionBuilderNode[grid_size][grid_size];
        int num_occupied = 0;
        for (int y = 0; y < grid_size; y++) {
            for (int x = 0; x < grid_size; x++) {
                RegionBuilderNode finder_node = new RegionBuilderNode(x, y);
                dir_finder_grid[y][x] = finder_node;
                if (!access_grid[y][x] && !dock_grid[y][x]) {
                    unit_grid.occupyGrid(
                            finder_node.getGridX(),
                            finder_node.getGridY(),
                            unreachable_obj,
                            UnitGrid.LAND);
                    num_occupied++;
                }

                RegionBuilderNode finder_water_node = new RegionBuilderNode(x, y);
                dir_finder_water_grid[y][x] = finder_water_node;
                if (!water_grid[y][x] && !dock_grid[y][x]) {
                    unit_grid.occupyGrid(
                            finder_water_node.getGridX(),
                            finder_water_node.getGridY(),
                            unreachable_obj,
                            UnitGrid.SEA);
                }
            }

            unit_grid.occupyGrid(y, 0, unreachable_obj, UnitGrid.SEA);
            unit_grid.occupyGrid(y, grid_size - 1, unreachable_obj, UnitGrid.SEA);
            unit_grid.occupyGrid(0, y, unreachable_obj, UnitGrid.SEA);
            unit_grid.occupyGrid(grid_size - 1, y, unreachable_obj, UnitGrid.SEA);
        }

        RegionBuilderNode water_start_node = dir_finder_water_grid[2][2];
        QueueArray water_start_nodes = new QueueArray(grid_size * grid_size);
        PocketList water_region_nodes = new PocketList(grid_size);
        water_start_nodes.addLast(water_start_node);
        while ((water_start_node =
                        findStartNode(
                                unit_grid, water_region_nodes, water_start_nodes, UnitGrid.SEA))
                != null) {
            Region region = new Region();
            addRegionNodes(
                    unit_grid,
                    dir_finder_water_grid,
                    water_start_nodes,
                    region,
                    water_start_node.getGridX(),
                    water_start_node.getGridY(),
                    water_region_nodes,
                    UnitGrid.SEA);
        }
        for (int y = 0; y < grid_size; y++) {
            for (int x = 0; x < grid_size; x++) {
                Region region = unit_grid.getRegion(x, y, UnitGrid.SEA);
                if (region != null) updateRegionNeighbours(unit_grid, x, y, region, UnitGrid.SEA);
            }
        }
        ProgressForm.progress(.5f);

        int actual_num_regions = 0;
        for (Object item : island_locations) {
            int[] pos = (int[]) item;
            RegionBuilderNode start_node = dir_finder_grid[pos[1]][pos[0]];
            QueueArray start_nodes = new QueueArray(grid_size * grid_size);
            PocketList region_nodes = new PocketList(grid_size);
            start_nodes.addLast(start_node);
            while ((start_node = findStartNode(unit_grid, region_nodes, start_nodes, UnitGrid.LAND))
                    != null) {
                assert !unit_grid.isGridOccupied(
                                start_node.getGridX(), start_node.getGridY(), UnitGrid.LAND)
                        : "Starting location (" + pos[0] + "," + pos[1] + ") occupied";
                Region region = new Region();
                addRegionNodes(
                        unit_grid,
                        dir_finder_grid,
                        start_nodes,
                        region,
                        start_node.getGridX(),
                        start_node.getGridY(),
                        region_nodes,
                        UnitGrid.LAND);
                actual_num_regions++;
            }
        }
        for (int y = 0; y < grid_size; y++) {
            for (int x = 0; x < grid_size; x++) {
                Region region = unit_grid.getRegion(x, y, UnitGrid.LAND);
                if (region != null) updateRegionNeighbours(unit_grid, x, y, region, UnitGrid.LAND);
            }
        }
        ProgressForm.progress(.5f);
        System.out.println("actual_num_regions = " + actual_num_regions);
    }

    private static final void testNeighbour(
            UnitGrid unit_grid, int grid_x, int grid_y, Region region, int layer) {
        int max = unit_grid.getHeightMap().getAccessGrid().length - 1;
        if (grid_x < 0 || grid_x > max || grid_y < 0 || grid_y > max) return;
        Region neighbour_region = unit_grid.getRegion(grid_x, grid_y, layer);
        Region.link(neighbour_region, region);
    }

    private static final void updateRegionNeighbours(
            UnitGrid unit_grid, int grid_x, int grid_y, Region region, int layer) {
        testNeighbour(unit_grid, grid_x + 1, grid_y, region, layer);
        testNeighbour(unit_grid, grid_x + 1, grid_y + 1, region, layer);
        testNeighbour(unit_grid, grid_x, grid_y + 1, region, layer);
        testNeighbour(unit_grid, grid_x - 1, grid_y + 1, region, layer);
        testNeighbour(unit_grid, grid_x - 1, grid_y, region, layer);
        testNeighbour(unit_grid, grid_x - 1, grid_y - 1, region, layer);
        testNeighbour(unit_grid, grid_x, grid_y - 1, region, layer);
        testNeighbour(unit_grid, grid_x + 1, grid_y - 1, region, layer);
    }

    private static final void addRegionNodes(
            UnitGrid unit_grid,
            RegionBuilderNode[][] dir_finder_grid,
            QueueArray start_nodes,
            Region region,
            int start_x,
            int start_y,
            PocketList region_nodes,
            int layer) {
        int min_x = start_x;
        int max_x = start_x;
        int min_y = start_y;
        int max_y = start_y;
        while (region_nodes.size() > 0) {
            RegionBuilderNode node = (RegionBuilderNode) region_nodes.removeBest();
            if (unit_grid.getRegion(node.getGridX(), node.getGridY(), layer) != null) continue;
            if (node.getTotalCost() > REGION_PATH_MAX_COST) {
                start_nodes.addLast(node);
                continue;
            }

            int nx = node.getGridX();
            int ny = node.getGridY();
            if (max_x < nx) max_x = nx;
            if (min_x > nx) min_x = nx;
            if (max_y < ny) max_y = ny;
            if (min_y > ny) min_y = ny;

            unit_grid.setRegion(node.getGridX(), node.getGridY(), region, layer);
            addNeighbours(unit_grid, dir_finder_grid, region_nodes, node, layer);
        }
        region.setPosition((max_x + min_x) / 2, (max_y + min_y) / 2);
    }

    private static final void addNeighbour(
            UnitGrid unit_grid,
            RegionBuilderNode[][] dir_finder_grid,
            PocketList region_nodes,
            int x,
            int y,
            int cost,
            int layer) {
        int max = unit_grid.getHeightMap().getGridUnitsPerWorld();
        if (x < 0 || x >= max || y < 0 || y >= max) {
            return;
        }
        RegionBuilderNode node = dir_finder_grid[y][x];
        if (unit_grid.getRegion(node.getGridX(), node.getGridY(), layer) != null) return;
        node.setTotalCost(cost);
        if (!unit_grid.isGridOccupied(node.getGridX(), node.getGridY(), layer))
            region_nodes.add(node.getTotalCost(), node);
    }

    private static final void addNeighbours(
            UnitGrid unit_grid,
            RegionBuilderNode[][] dir_finder_grid,
            PocketList region_nodes,
            RegionBuilderNode node,
            int layer) {
        int x = node.getGridX();
        int y = node.getGridY();
        int cost = node.getTotalCost();
        addNeighbour(
                unit_grid, dir_finder_grid, region_nodes, x - 1, y - 1, cost + DIAGONAL, layer);
        addNeighbour(unit_grid, dir_finder_grid, region_nodes, x - 1, y, cost + STRAIGHT, layer);
        addNeighbour(
                unit_grid, dir_finder_grid, region_nodes, x - 1, y + 1, cost + DIAGONAL, layer);
        addNeighbour(unit_grid, dir_finder_grid, region_nodes, x, y - 1, cost + STRAIGHT, layer);
        addNeighbour(unit_grid, dir_finder_grid, region_nodes, x, y + 1, cost + STRAIGHT, layer);
        addNeighbour(
                unit_grid, dir_finder_grid, region_nodes, x + 1, y - 1, cost + DIAGONAL, layer);
        addNeighbour(unit_grid, dir_finder_grid, region_nodes, x + 1, y, cost + STRAIGHT, layer);
        addNeighbour(
                unit_grid, dir_finder_grid, region_nodes, x + 1, y + 1, cost + DIAGONAL, layer);
    }

    private static final RegionBuilderNode findStartNode(
            UnitGrid unit_grid, PocketList region_nodes, QueueArray start_nodes, int layer) {
        region_nodes.clear();
        while (!start_nodes.isEmpty()) {
            RegionBuilderNode node = start_nodes.removeFirst();
            if (unit_grid.getRegion(node.getGridX(), node.getGridY(), layer) == null) {
                node.setTotalCost(0);
                region_nodes.add(node.getTotalCost(), node);
                return node;
            }
        }
        return null;
    }
}

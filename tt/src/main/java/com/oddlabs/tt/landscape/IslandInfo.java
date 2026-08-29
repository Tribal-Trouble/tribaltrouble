package com.oddlabs.tt.landscape;

import com.oddlabs.procedural.Channel;

import java.util.ArrayList;
import java.util.List;

public class IslandInfo {
    private int id;
    private int area;
    private int trees;
    private int rocks;
    private int iron;
    private int min_x;
    private int min_y;
    private int max_x;
    private int max_y;
    private int start_x;
    private int start_y;
    private List<int[]> contour_points = new ArrayList<>();

    public IslandInfo(int id, int area, int min_x, int min_y, int max_x, int max_y, int start_x, int start_y,
            byte[][] dock_grid, Channel island_ids) {
        this.id = id;
        this.area = area;
        this.min_x = min_x;
        this.min_y = min_y;
        this.max_x = max_x;
        this.max_y = max_y;
        this.start_x = start_x;
        this.start_y = start_y;
        this.rocks = 0;
        this.iron = 0;
        this.trees = 0;
        generateContourPoints(dock_grid, island_ids);
    }

    private void generateContourPoints(byte[][] dock_grid, Channel island_ids) {
        for (int y = min_y; y <= max_y; y++) {
            for (int x = min_x; x <= max_x; x++) {
                if (dock_grid[y][x] == 1 && StrictMath.round(island_ids.getPixel(x, y)) == id) {
                    contour_points.add(new int[]{x, y});
                }
            }
        }
    }

    public List<int[]> contourPoints() {
        return contour_points;
    }

    public void setTrees(int trees) {
        this.trees = trees;
    }

    public void setIron(int iron) {
        this.iron = iron;
    }

    public void setRocks(int rocks) {
        this.rocks = rocks;
    }

    public int rocks() {
        return rocks;
    }

    public int iron() {
        return iron;
    }

    public int trees() {
        return trees;
    }

    public int minX() {
        return min_x;
    }

    public int minY() {
        return min_y;
    }

    public int maxX() {
        return max_x;
    }

    public int maxY() {
        return max_y;
    }

    public int centerX() {
        return (min_x + max_x) / 2;
    }

    public int centerY() {
        return (min_y + max_y) / 2;
    }

    public int startX() {
        return start_x;
    }

    public int startY() {
        return start_y;
    }

    public int area() {
        return area;
    }

    public int id() {
        return id;
    }
}

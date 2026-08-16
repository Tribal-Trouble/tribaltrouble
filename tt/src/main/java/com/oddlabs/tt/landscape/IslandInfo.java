package com.oddlabs.tt.landscape;

public class IslandInfo {
    private int id;
    private int area;
    private int trees;
    private int rocks;
    private int iron;
    private int gridX;
    private int gridY;

    public IslandInfo(int id, int area, int x, int y) {
        this.id = id;
        this.area = area;
        this.gridX = x;
        this.gridY = y;
        this.rocks = 0;
        this.iron = 0;
        this.trees = 0;
    }

    public IslandInfo(int id, int area, int trees, int rocks, int iron, int x, int y) {
        this.id = id;
        this.area = area;
        this.trees = trees;
        this.rocks = rocks;
        this.iron = iron;
        this.gridX = x;
        this.gridY = y;
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

    public int x() {
        return gridX;
    }

    public int y() {
        return gridY;
    }

    public int area() {
        return area;
    }

    public int id() {
        return id;
    }
}

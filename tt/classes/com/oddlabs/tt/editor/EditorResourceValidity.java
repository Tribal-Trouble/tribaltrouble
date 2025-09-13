package com.oddlabs.tt.editor;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;

/**
 * Computes simple resource placement validity at editor runtime.
 * Criteria (v1): not water, not dock, buildable (build grid != 0), and unoccupied LAND.
 * Recomputed per-ROI after terrain edits.
 */
public final class EditorResourceValidity {
    private EditorResourceValidity() {}

    private static World worldRef;
    private static boolean[][] placementValid; // [y][x]

    public static synchronized void ensure(World w) {
        if (worldRef != w || placementValid == null) {
            worldRef = w;
            int n = w.getHeightMap().getGridUnitsPerWorld();
            placementValid = new boolean[n][n];
            // initialize once
            recomputeROI(w, 0, 0, n - 1, n - 1);
        }
    }

    public static synchronized void recomputeROI(World w, int x0, int y0, int x1, int y1) {
        ensure(w);
        HeightMap hm = w.getHeightMap();
        com.oddlabs.tt.pathfinder.UnitGrid ug = w.getUnitGrid();
        boolean[][] water = hm.getWaterGrid();
        boolean[][] dock = hm.getDockGrid();
        boolean[][] access = hm.getAccessGrid();
        int n = hm.getGridUnitsPerWorld();
        int cx0 = StrictMath.max(0, x0);
        int cy0 = StrictMath.max(0, y0);
        int cx1 = StrictMath.min(n - 1, x1);
        int cy1 = StrictMath.min(n - 1, y1);
        for (int y = cy0; y <= cy1; y++) {
            for (int x = cx0; x <= cx1; x++) {
                boolean isWater = water != null && water[y][x];
                boolean isDock = dock != null && dock[y][x];
                boolean accessible = access != null && access[y][x];
                com.oddlabs.tt.pathfinder.Occupant occ = ug.getOccupant(x, y, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                boolean occupied = (occ != null) && !(occ instanceof com.oddlabs.tt.pathfinder.StaticOccupant);
                placementValid[y][x] = (!isWater && !isDock && accessible && !occupied);
            }
        }
    }

    public static synchronized boolean[][] getPlacementGrid(World w) {
        ensure(w);
        return placementValid;
    }

    public static synchronized boolean isValid(World w, int gx, int gy) {
        ensure(w);
        if (gx < 0 || gy < 0 || gy >= placementValid.length || gx >= placementValid.length) return false;
        return placementValid[gy][gx];
    }
}

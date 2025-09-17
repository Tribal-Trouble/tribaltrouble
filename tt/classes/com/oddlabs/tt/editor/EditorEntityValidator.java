package com.oddlabs.tt.editor;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;

/**
 * Unified editor entity placement validator.
 * Input: world, kind (building/unit), template sizes, target center grid coords.
 * Output: enum result with reason for invalidity.
 */
public final class EditorEntityValidator {
    private EditorEntityValidator() {}

    public enum Kind { BUILDING, UNIT }
    public enum Reason { OK, OUT_OF_BOUNDS, TERRAIN_BUILD, TERRAIN_DOCK, ACCESS, OCCUPIED }

    public static final class Result {
        public final boolean valid; public final Reason reason;
        private Result(boolean v, Reason r) { valid = v; reason = r; }
        public static Result ok() { return new Result(true, Reason.OK); }
        public static Result fail(Reason r) { return new Result(false, r); }
    }

    /** Validate a building footprint centered at (gx,gy) using live BuildGrid + occupancy. placingSize is template.getPlacingSize(). */
    public static Result validateBuilding(World w, int gx, int gy, int placingSize, boolean nearSea) {
        HeightMap hm = w.getHeightMap();
        int n = hm.getGridUnitsPerWorld();
        if (gx < 0 || gy < 0 || gx >= n || gy >= n) return Result.fail(Reason.OUT_OF_BOUNDS);
        // Build/dock rule mirrors Building.doIsPlacingLegal, but we check occupancy separately
        // In in-game code Building.isPlacingLegal passes template.getPlacingSize() - PLACING_BORDER (PLACING_BORDER=1)
        // so we mirror that to avoid requiring one extra ring of build_grid clearance in editor.
        int buildCheckSize = nearSea ? 1 : Math.max(1, placingSize - 1);
        if (!nearSea && !hm.canBuild(gx, gy, buildCheckSize)) return Result.fail(Reason.TERRAIN_BUILD);
        if (nearSea && !hm.canDock(gx, gy)) return Result.fail(Reason.TERRAIN_DOCK);
        int size = nearSea ? 1 : placingSize; // footprint area still uses full placingSize like original footprint (size*2-1)
        // Occupancy: entire footprint must be free
        if (!EditorEntityOccupancy.isFreeBuilding(w, gx, gy, size)) return Result.fail(Reason.OCCUPIED);
        // Also ensure access grid true for all footprint cells? Spec says: buildings base filter build grid. Access not required for building placement inside editor besides BuildGrid—they can block access. We'll skip extra access checks.
        return Result.ok();
    }

    /** Validate a unit spawn at (gx,gy): requires access + free occupancy + not water/dock. */
    public static Result validateUnit(World w, int gx, int gy) {
        HeightMap hm = w.getHeightMap();
        int n = hm.getGridUnitsPerWorld();
        if (gx < 0 || gy < 0 || gx >= n || gy >= n) return Result.fail(Reason.OUT_OF_BOUNDS);
        boolean[][] access = hm.getAccessGrid();
        if (access != null && !access[gy][gx]) return Result.fail(Reason.ACCESS);
        // Water/dock implicitly filtered by access grid; if we want explicit, we can check
        if (!EditorEntityOccupancy.isFreeUnit(w, gx, gy)) return Result.fail(Reason.OCCUPIED);
        return Result.ok();
    }
}

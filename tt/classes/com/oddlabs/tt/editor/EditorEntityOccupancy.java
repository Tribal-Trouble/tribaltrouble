package com.oddlabs.tt.editor;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.UnitGrid;

/**
 * Lightweight editor-only occupancy index unifying units, buildings, and resource models
 * for placement-time overlap checks. Backed by a byte grid (0 = free, 1 = occupied).
 *
 * Rationale: We need a fast, queryable, up-to-date occupancy mask that treats all
 * editor-placeable entities (units, buildings, resources) uniformly. Runtime UnitGrid
 * already stores occupants, but scanning a building footprint repeatedly allocates
 * or branches on occupant types. This index is updated incrementally on spawn/despawn
 * and during bulk revalidation/erase operations.
 *
 * Footprint semantics:
 *  - Units occupy a single cell at (gridX, gridY).
 *  - Buildings occupy a (size*2-1)x(size*2-1) square centered at (gridX, gridY) where
 *    size = placingSize or placingSize-1 depending on template rules (mirrors Building.doIsPlacingLegal logic).
 *  - Resources (trees, supplies) occupy their single grid cell if present and visible.
 */
public final class EditorEntityOccupancy {
    private static World worldRef;
    private static byte[][] occ; // [y][x] 0=free,1=occupied

    private EditorEntityOccupancy() {}

    public static synchronized void ensure(World w) {
        if (worldRef != w || occ == null) {
            worldRef = w;
            int n = w.getHeightMap().getGridUnitsPerWorld();
            occ = new byte[n][n];
            rebuildFull(w); // initialize from current world state
        }
    }

    /** Full rebuild (slow path) used on first ensure or rare global resets. */
    public static synchronized void rebuildFull(World w) {
        ensure(w);
        int n = occ.length;
        for (int y=0;y<n;y++) for (int x=0;x<n;x++) occ[y][x] = 0;
        UnitGrid ug = w.getUnitGrid();
        int gridSize = ug.getGridSize();
        for (int y=0;y<gridSize;y++) {
            for (int x=0;x<gridSize;x++) {
                com.oddlabs.tt.pathfinder.Occupant o = ug.getOccupant(x, y, UnitGrid.LAND);
                if (o == null) continue;
                // Skip static placeholder occupants used for path blocking (StaticOccupant) so they don't block placement more than existing rules.
                if (o instanceof com.oddlabs.tt.pathfinder.StaticOccupant) continue;
                claimOccupant(o);
            }
        }
    }

    /** Claim occupancy for an occupant (unit/building/resource) if not already marked. */
    public static synchronized void claimOccupant(Object occObj) {
        if (occObj == null) return;
        if (occObj instanceof com.oddlabs.tt.model.Unit) {
            com.oddlabs.tt.model.Unit u = (com.oddlabs.tt.model.Unit) occObj;
            claimCell(u.getGridX(), u.getGridY());
        } else if (occObj instanceof com.oddlabs.tt.model.Building) {
            com.oddlabs.tt.model.Building b = (com.oddlabs.tt.model.Building) occObj;
            int size = b.getBuildingTemplate().getPlacingSize();
            int r = Math.max(0, size - 1);
            for (int gy = b.getGridY() - r; gy <= b.getGridY() + r; gy++) {
                for (int gx = b.getGridX() - r; gx <= b.getGridX() + r; gx++) {
                    claimCell(gx, gy);
                }
            }
        } else if (occObj instanceof com.oddlabs.tt.landscape.TreeSupply) {
            com.oddlabs.tt.landscape.TreeSupply t = (com.oddlabs.tt.landscape.TreeSupply) occObj;
            if (!t.isHidden()) claimCell(t.getGridX(), t.getGridY());
        } else if (occObj instanceof com.oddlabs.tt.model.SupplyModel) {
            com.oddlabs.tt.model.SupplyModel s = (com.oddlabs.tt.model.SupplyModel) occObj;
            claimCell(s.getGridX(), s.getGridY());
        }
    }

    /** Release occupancy for an occupant. */
    public static synchronized void releaseOccupant(Object occObj) {
        if (occObj == null) return;
        if (occObj instanceof com.oddlabs.tt.model.Unit) {
            com.oddlabs.tt.model.Unit u = (com.oddlabs.tt.model.Unit) occObj;
            releaseCell(u.getGridX(), u.getGridY());
        } else if (occObj instanceof com.oddlabs.tt.model.Building) {
            com.oddlabs.tt.model.Building b = (com.oddlabs.tt.model.Building) occObj;
            int size = b.getBuildingTemplate().getPlacingSize();
            int r = Math.max(0, size - 1);
            for (int gy = b.getGridY() - r; gy <= b.getGridY() + r; gy++) {
                for (int gx = b.getGridX() - r; gx <= b.getGridX() + r; gx++) {
                    releaseCell(gx, gy);
                }
            }
        } else if (occObj instanceof com.oddlabs.tt.landscape.TreeSupply) {
            com.oddlabs.tt.landscape.TreeSupply t = (com.oddlabs.tt.landscape.TreeSupply) occObj;
            releaseCell(t.getGridX(), t.getGridY());
        } else if (occObj instanceof com.oddlabs.tt.model.SupplyModel) {
            com.oddlabs.tt.model.SupplyModel s = (com.oddlabs.tt.model.SupplyModel) occObj;
            releaseCell(s.getGridX(), s.getGridY());
        }
    }

    public static synchronized boolean isFreeUnit(World w, int gx, int gy) {
        ensure(w);
        if (!inBounds(gx, gy)) return false;
        return occ[gy][gx] == 0;
    }

    public static synchronized boolean isFreeBuilding(World w, int centerGX, int centerGY, int placingSize) {
        ensure(w);
        int r = Math.max(0, placingSize - 1);
        for (int gy = centerGY - r; gy <= centerGY + r; gy++) {
            for (int gx = centerGX - r; gx <= centerGX + r; gx++) {
                if (!inBounds(gx, gy) || occ[gy][gx] != 0) return false;
            }
        }
        return true;
    }

    /** Single-cell occupancy query used by editor placement grid reader. */
    public static synchronized boolean isCellFree(World w, int gx, int gy) {
        ensure(w);
        if (!inBounds(gx, gy)) return false; // treat OOB as occupied for safety
        return occ[gy][gx] == 0;
    }

    private static boolean inBounds(int gx, int gy) {
        return gy >= 0 && gy < occ.length && gx >= 0 && gx < occ.length;
    }

    private static void claimCell(int gx, int gy) {
        if (!inBounds(gx, gy)) return;
        occ[gy][gx] = 1;
    }

    private static void releaseCell(int gx, int gy) {
        if (!inBounds(gx, gy)) return;
        occ[gy][gx] = 0;
    }

    /** Recompute occupancy for a rectangle (used after terrain edits that may remove invalidated entities). */
    public static synchronized void rebuildROI(World w, int minGX, int minGY, int maxGX, int maxGY) {
        ensure(w);
        UnitGrid ug = w.getUnitGrid();
        int x0 = Math.max(0, minGX); int y0 = Math.max(0, minGY);
        int x1 = Math.min(occ.length - 1, maxGX); int y1 = Math.min(occ.length - 1, maxGY);
        for (int gy=y0; gy<=y1; gy++) for (int gx=x0; gx<=x1; gx++) occ[gy][gx] = 0;
        for (int gy=y0; gy<=y1; gy++) {
            for (int gx=x0; gx<=x1; gx++) {
                com.oddlabs.tt.pathfinder.Occupant o = ug.getOccupant(gx, gy, UnitGrid.LAND);
                if (o == null || (o instanceof com.oddlabs.tt.pathfinder.StaticOccupant)) continue;
                // Claim (idempotent for multi-cell footprints because we re-scan each cell)
                claimOccupant(o);
            }
        }
    }
}

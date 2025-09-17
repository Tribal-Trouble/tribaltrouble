package com.oddlabs.tt.model;

/**
 * Minimal ad-hoc harness (run manually) to sanity check editor/runtime placement rule parity.
 * Not a formal JUnit test because the project doesn't expose a test framework in Ant build.
 */
public final class PlacementRulesHarness {
    public static void main(String[] args) {
        System.out.println("PlacementRulesHarness: basic smoke test (no assertions available without world scaffolding)." );
        // Intentionally minimal: A full test would need a mock World/UnitGrid; documenting intent instead.
        // Developers can extend this with a lightweight fake implementing PlacementGridReader.
        PlacementGridReader fake = new PlacementGridReader() {
            int n=8; boolean occ=false; boolean canBuild=true; boolean canDock=false;
            public int getGridSize() { return n; }
            public boolean canBuildCenter(int gx,int gy,int size){ return canBuild; }
            public boolean canDock(int gx,int gy){ return canDock; }
            public boolean isOccupied(int gx,int gy){ return occ; }
        };
        boolean ok = PlacementRules.isPlacingLegal(fake, 3,3,2,false);
        System.out.println("Expected true got="+ok);
        if(!ok) throw new IllegalStateException("Expected legal placement");
        System.out.println("Harness complete.");
    }
}

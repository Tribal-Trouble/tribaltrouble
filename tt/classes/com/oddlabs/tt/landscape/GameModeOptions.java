package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Race;

public final class GameModeOptions {
    private static final int UNIT_TYPES = 5; // rock/iron/rubber warriors, peon, chieftain
    private static final int BUILDING_TYPES = Race.NUM_BUILDINGS; // quarters, armory, tower, ship
    public final boolean peaceEnabled;
    // Duration in seconds for peace mode
    public final int peaceSeconds;
    // Max buildings per player for this match
    public final int maxBuildings;
    // Allowlist masks (by race indices)
    // Units indices per Race: 0..4
    public final boolean[] allowedUnits;
    // Buildings indices per Race: 0..(Race.NUM_BUILDINGS-1)
    public final boolean[] allowedBuildings;

    public GameModeOptions(
            boolean peaceEnabled,
            int peaceSeconds,
            int maxBuildings,
            boolean[] allowedUnits,
            boolean[] allowedBuildings) {
        this.peaceEnabled = peaceEnabled;
        this.peaceSeconds = StrictMath.max(0, peaceSeconds);
        this.maxBuildings = StrictMath.max(0, maxBuildings);
        // Defensive copies to avoid external mutation
        this.allowedUnits = new boolean[UNIT_TYPES];
        this.allowedBuildings = new boolean[BUILDING_TYPES];
        if (allowedUnits != null) {
            int len = StrictMath.min(this.allowedUnits.length, allowedUnits.length);
            System.arraycopy(allowedUnits, 0, this.allowedUnits, 0, len);
            for (int i = len; i < this.allowedUnits.length; i++) this.allowedUnits[i] = true;
        } else {
            for (int i = 0; i < this.allowedUnits.length; i++) this.allowedUnits[i] = true;
        }
        if (allowedBuildings != null) {
            int len = StrictMath.min(this.allowedBuildings.length, allowedBuildings.length);
            System.arraycopy(allowedBuildings, 0, this.allowedBuildings, 0, len);
            for (int i = len; i < this.allowedBuildings.length; i++) this.allowedBuildings[i] = true;
        } else {
            for (int i = 0; i < this.allowedBuildings.length; i++) this.allowedBuildings[i] = true;
        }
    }

    public static GameModeOptions defaults(int defaultMaxBuildings) {
        boolean[] allUnits = new boolean[] {true, true, true, true, true};
        boolean[] allBuildings = new boolean[BUILDING_TYPES];
        for (int i = 0; i < allBuildings.length; i++) allBuildings[i] = true;
        return new GameModeOptions(false, 0, defaultMaxBuildings, allUnits, allBuildings);
    }
}

package com.oddlabs.tt.model;

import com.oddlabs.util.Vector2f;
import com.oddlabs.util.Vector3f;

public final strictfp class ShipHR {

    private static final int NUM_LOWER_DECK_ROWS = 13;
    private static final int NUM_LOWER_DECK_COLS = 4;
    private static final int NUM_UPPER_DECK_ROWS = 12;
    private static final int NUM_UPPER_DECK_COLS = 2;
    private static final int NUM_LOWER_DECK = NUM_LOWER_DECK_ROWS * NUM_LOWER_DECK_COLS;
    private static final int NUM_UPPER_DECK = NUM_UPPER_DECK_ROWS * NUM_UPPER_DECK_COLS;
    private static final int NUM_UNITS = NUM_LOWER_DECK + NUM_UPPER_DECK;
    private static final int LOWER_DECK_START = 0;
    private static final int UPPER_DECK_START = NUM_LOWER_DECK;
    private static final ShipAllocation[] allocations = new ShipAllocation[NUM_UNITS];

    private Unit[] units = new Unit[NUM_UNITS];

    static {
        float lower_deck_left_y = 1.88f;
        float lower_deck_right_y = -1.72f;
        float lower_deck_z = +0.57f;

        float[] x_positions = new float[NUM_LOWER_DECK_ROWS];
        x_positions[0] = -8.18f;
        x_positions[1] = -6.97f;
        x_positions[2] = -5.62f;
        x_positions[3] = -4.31f;
        x_positions[4] = -3.08f;
        x_positions[5] = -1.81f;
        x_positions[6] = -0.50f;
        x_positions[7] = +0.84f;
        x_positions[8] = +2.29f;
        x_positions[9] = +3.70f;
        x_positions[10] = +5.09f;
        x_positions[11] = +6.53f;
        x_positions[12] = +8.00f;

        Vector2f fwd = new Vector2f(1.0f, 0.0f);
        Vector2f left = new Vector2f(0.0f, 1.0f);
        Vector2f right = new Vector2f(0.0f, -1.0f);

        int index = LOWER_DECK_START;
        float range_y = lower_deck_right_y - lower_deck_left_y;
        for (int r = 0; r < NUM_LOWER_DECK_ROWS; r++) {
            for (int j = 0; j < 2; j++) {
                int c = j * (NUM_LOWER_DECK_COLS - 1);
                float x = x_positions[r];
                float y = lower_deck_left_y + range_y * (((float) c) / (NUM_LOWER_DECK_COLS - 1));
                float z = lower_deck_z;
                int role = ShipAllocation.ROWING_LEFT;
                if (j == 0) {
                    role = ShipAllocation.ROWING_RIGHT;
                }
                allocations[index] = new ShipAllocation(new Vector3f(x, y, z), fwd, role);
                index++;
            }
        }
        for (int r = 0; r < NUM_LOWER_DECK_ROWS; r++) {
            for (int c = 1; c < 3; c++) {
                float x = x_positions[r];
                float y = lower_deck_left_y + range_y * (((float) c) / (NUM_LOWER_DECK_COLS - 1));
                float z = lower_deck_z;
                allocations[index] =
                        new ShipAllocation(new Vector3f(x, y, z), fwd, ShipAllocation.SITTING);
                index++;
            }
        }

        float upper_deck_min_x = -8.67407f;
        float upper_deck_max_x = +8.25568f;
        float upper_deck_min_y = -1.65051f;
        float upper_deck_max_y = +1.54435f;
        float upper_deck_z = +3.19313f;

        index = UPPER_DECK_START;
        range_y = upper_deck_max_y - upper_deck_min_y;
        float range_x = upper_deck_max_x - upper_deck_min_x;
        for (int r = 0; r < NUM_UPPER_DECK_ROWS; r++) {
            for (int c = 0; c < NUM_UPPER_DECK_COLS; c++) {
                float x = upper_deck_min_x + range_x * (((float) r) / (NUM_UPPER_DECK_ROWS - 1));
                float y = upper_deck_min_y + range_y * (((float) c) / (NUM_UPPER_DECK_COLS - 1));
                float z = upper_deck_z;
                allocations[index] =
                        new ShipAllocation(
                                new Vector3f(x, y, z),
                                c == 0 ? right : left,
                                ShipAllocation.FIGHTING);
                index++;
            }
        }
    }

    public ShipHR() {}

    public boolean canAllocate(Unit unit) {
        if (!unit.isWarrior()) {
            for (int i = LOWER_DECK_START; i < LOWER_DECK_START + NUM_LOWER_DECK; i++) {
                if (units[i] == null) {
                    return true;
                }
            }
        } else {
            for (int i = UPPER_DECK_START; i < UPPER_DECK_START + NUM_UPPER_DECK; i++) {
                if (units[i] == null) {
                    return true;
                }
            }
        }
        return false;
    }

    public ShipAllocation tryAllocate(Unit unit) {
        if (!unit.isWarrior()) {
            for (int i = LOWER_DECK_START; i < LOWER_DECK_START + NUM_LOWER_DECK; i++) {
                if (units[i] == null) {
                    units[i] = unit;
                    return allocations[i];
                }
            }
        } else {
            for (int i = UPPER_DECK_START; i < UPPER_DECK_START + NUM_UPPER_DECK; i++) {
                if (units[i] == null) {
                    units[i] = unit;
                    unit.increaseRange(16f);
                    return allocations[i];
                }
            }
        }
        return null;
    }

    public void killCrew() {
        for (int i = 0; i < NUM_UNITS; i++) {
            if (units[i] != null) {
                units[i].setReference(null);
                units[i].enable();
                units[i].startDying();
            }
        }
    }

    public Unit exitUnit(UnitTemplate template) {
        for (int i = NUM_UNITS - 1; i >= 0; i--) {
            Unit unit = units[i];
            if (unit != null && unit.getUnitTemplate() == template) {
                unit.setReference(null);
                unit.unmount();
                units[i] = null;
                return unit;
            }
        }
        return null;
    }

    public int countPeons() {
        int result = 0;
        for (int i = 0; i < NUM_UNITS; i++) {
            Unit unit = units[i];
            if (unit != null && unit.isWarrior() == false) {
                result++;
            }
        }
        return result;
    }
}

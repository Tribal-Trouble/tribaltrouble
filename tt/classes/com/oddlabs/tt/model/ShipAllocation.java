package com.oddlabs.tt.model;

import com.oddlabs.util.Vector3f;

public final strictfp class ShipAllocation {

    public static final int SITTING = 0;
    public static final int ROWING_LEFT = 1;
    public static final int ROWING_RIGHT = 2;
    public static final int FIGHTING = 3;

    private final int role;
    private final Vector3f offset;

    public ShipAllocation(Vector3f offset, int role) {
        this.role = role;
        this.offset = offset;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public int getRole() {
        return role;
    }
}

package com.oddlabs.tt.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

public final class ShipAllocation {

    public static final int SITTING = 0;
    public static final int ROWING_LEFT = 1;
    public static final int ROWING_RIGHT = 2;
    public static final int FIGHTING = 3;

    private final int role;
    private final Vector3f offset;
    private final Vector2f rotation;

    public ShipAllocation(Vector3f offset, Vector2f rotation, int role) {
        this.role = role;
        this.offset = offset;
        this.rotation = rotation;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public Vector2f getRotation() {
        return rotation;
    }

    public int getRole() {
        return role;
    }
}

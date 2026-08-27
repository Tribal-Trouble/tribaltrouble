package com.oddlabs.tt.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

public final class ShipAllocation {

    public static final int SITTING = 0;
    public static final int ROWING_LEFT = 1;
    public static final int ROWING_RIGHT = 2;
    public static final int FIGHTING = 3;
    public static final int STEERING = 4;

    private int role = SITTING;
    private Vector3f offset = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector2f rotation = new Vector2f(0.0f, 1.0f);

    public ShipAllocation() {
    }

    public ShipAllocation(Vector3f offset, Vector2f rotation, int role) {
        this.role = role;
        this.offset = offset;
        this.rotation = rotation;
    }

    public void setOffset(float x, float y, float z) {
        offset = new Vector3f(x, y, z);
    }

    public void setRotation(float x, float y) {
        rotation = new Vector2f(x, y);
    }

    public void setRole(int role) {
        this.role = role;
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

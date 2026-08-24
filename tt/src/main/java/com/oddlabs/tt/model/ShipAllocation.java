package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.pathfinder.ShipTrajectoryPoint;
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

    public void updateFinal(Unit unit, Ship ship) {
        float x = ship.getPositionX();
        float y = ship.getPositionY();
        float dx = ship.getDirectionX();
        float dy = ship.getDirectionY();
        float ox = offset.x;
        float oy = offset.y;
        float gx = x + dx * ox - dy * oy;
        float gy = y + dy * ox + dx * oy;
        int gridSize = ship.getUnitGrid().getGridSize();
        int gridX = Math.clamp(UnitGrid.toGridCoordinate(gx), 0, gridSize - 1);
        int gridY = Math.clamp(UnitGrid.toGridCoordinate(gy), 0, gridSize - 1);
        unit.setReference(ship);
        unit.setPosition(gx, gy);
        unit.setGridPosition(gridX, gridY);
        unit.setDirection(-dy, dx);
        unit.setMountOffset(offset.z);
    }

    public void updateIntermediate(Unit unit, Ship ship, float progress) {
        float x = ship.getPositionX();
        float y = ship.getPositionY();
        float dx = ship.getDirectionX();
        float dy = ship.getDirectionY();
        float ox = offset.x;
        float oy = offset.y;
        float gx = x + dx * ox - dy * oy;
        float gy = y + dy * ox + dx * oy;
        var proxy = ship.getEntrance();
        ShipTrajectoryPoint p0 = new ShipTrajectoryPoint(proxy.getPositionX(), proxy.getPositionY());
        ShipTrajectoryPoint p1 = new ShipTrajectoryPoint(gx, gy);
        p0.setDirectionTo(p1);
        float d = p0.distanceTo(p1) * progress;
        p0.move(d);
        unit.setPosition(p0.positionX, p0.positionY);
        unit.setGridPosition(p0.gridX, p0.gridY);
        unit.setDirection(p0.directionX, p0.directionY);
        float z = progress * offset.z;
        unit.setMountOffset(z);
    }
}

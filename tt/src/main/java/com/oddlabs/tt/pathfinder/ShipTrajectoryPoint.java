package com.oddlabs.tt.pathfinder;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.util.Target;

public final class ShipTrajectoryPoint {
    public int gridX;
    public int gridY;
    public float positionX;
    public float positionY;
    public float directionX;
    public float directionY;

    public ShipTrajectoryPoint() {
    }

    public ShipTrajectoryPoint(int x, int y) {
        gridX = x;
        gridY = y;
        positionX = UnitGrid.coordinateFromGrid(gridX);
        positionY = UnitGrid.coordinateFromGrid(gridY);
        directionX = 0.0f;
        directionY = 1.0f;
    }

    public ShipTrajectoryPoint(int x, int y, float dx, float dy) {
        gridX = x;
        gridY = y;
        positionX = UnitGrid.coordinateFromGrid(gridX);
        positionY = UnitGrid.coordinateFromGrid(gridY);
        directionX = dx;
        directionY = dy;
    }

    public ShipTrajectoryPoint(Target t) {
        gridX = t.getGridX();
        gridY = t.getGridY();
        positionX = t.getPositionX();
        positionY = t.getPositionY();

        if (t instanceof Selectable) {
            Selectable selectable = (Selectable) t;
            directionX = selectable.getDirectionX();
            directionY = selectable.getDirectionY();
        } else {
            directionX = 0.0f;
            directionY = 1.0f;
        }
    }

    public ShipTrajectoryPoint(Ship s) {
        gridX = s.getGridX();
        gridY = s.getGridY();
        positionX = s.getPositionX();
        positionY = s.getPositionY();
        directionX = s.getDirectionX();
        directionY = s.getDirectionY();
    }

    public void setPosition(float x, float y) {
        this.positionX = x;
        this.positionY = y;
        this.gridX = UnitGrid.toGridCoordinate(this.positionX);
        this.gridY = UnitGrid.toGridCoordinate(this.positionY);
    }

    public void setPosition(ShipTrajectoryPoint pt) {
        this.positionX = pt.positionX;
        this.positionY = pt.positionY;
        this.gridX = UnitGrid.toGridCoordinate(this.positionX);
        this.gridY = UnitGrid.toGridCoordinate(this.positionY);
    }

    public void copyFrom(ShipTrajectoryPoint pt) {
        this.positionX = pt.positionX;
        this.positionY = pt.positionY;
        this.gridX = pt.gridX;
        this.gridY = pt.gridY;
        this.directionX = pt.directionX;
        this.directionY = pt.directionY;
    }

    public void copyDirection(ShipTrajectoryPoint pt) {
        this.directionX = pt.directionX;
        this.directionY = pt.directionY;
    }

    public ShipTrajectoryPoint moved(int distance) {
        ShipTrajectoryPoint moved = new ShipTrajectoryPoint();
        moved.gridX = gridX + (int) StrictMath.round(directionX * distance);
        moved.gridY = gridY + (int) StrictMath.round(directionY * distance);
        moved.positionX = UnitGrid.coordinateFromGrid(moved.gridX);
        moved.positionY = UnitGrid.coordinateFromGrid(moved.gridY);
        moved.directionX = directionX;
        moved.directionY = directionY;
        return moved;
    }

    public ShipTrajectoryPoint moved(float distance) {
        ShipTrajectoryPoint moved = new ShipTrajectoryPoint();
        moved.positionX = positionX + directionX * distance;
        moved.positionY = positionY + directionY * distance;
        moved.gridX = UnitGrid.toGridCoordinate(moved.positionX);
        moved.gridY = UnitGrid.toGridCoordinate(moved.positionY);
        moved.directionX = directionX;
        moved.directionY = directionY;
        return moved;
    }

    public void move(int distance) {
        ShipTrajectoryPoint moved = moved(distance);
        gridX = moved.gridX;
        gridY = moved.gridY;
        positionX = moved.positionX;
        positionY = moved.positionY;
        directionX = moved.directionX;
        directionY = moved.directionY;
    }

    public void move(float distance) {
        ShipTrajectoryPoint moved = moved(distance);
        gridX = moved.gridX;
        gridY = moved.gridY;
        positionX = moved.positionX;
        positionY = moved.positionY;
        directionX = moved.directionX;
        directionY = moved.directionY;
    }

    public ShipTrajectoryPoint rotated(float angle) {
        ShipTrajectoryPoint rotated = new ShipTrajectoryPoint();
        rotated.gridX = gridX;
        rotated.gridY = gridY;
        rotated.positionX = positionX;
        rotated.positionY = positionY;

        float radians = (float) StrictMath.toRadians(angle);
        float sin = (float) StrictMath.sin(radians);
        float cos = (float) StrictMath.cos(radians);

        rotated.directionX = directionX * cos - directionY * sin;
        rotated.directionY = directionX * sin + directionY * cos;

        float len = (float) StrictMath.sqrt(
                rotated.directionX * rotated.directionX + rotated.directionY * rotated.directionY);
        if (len > 0.0001f) {
            rotated.directionX /= len;
            rotated.directionY /= len;
        } else {
            rotated.directionX = directionX;
            rotated.directionY = directionY;
        }

        return rotated;
    }

    public void rotate(float angle) {
        ShipTrajectoryPoint rotated = rotated(angle);
        gridX = rotated.gridX;
        gridY = rotated.gridY;
        positionX = rotated.positionX;
        positionY = rotated.positionY;
        directionX = rotated.directionX;
        directionY = rotated.directionY;
    }

    public float gridDistanceTo(ShipTrajectoryPoint p) {
        int dx = p.gridX - gridX;
        int dy = p.gridY - gridY;
        return (float) StrictMath.sqrt(dx * dx + dy * dy);
    }

    public float distanceTo(ShipTrajectoryPoint p) {
        float dx = p.positionX - positionX;
        float dy = p.positionY - positionY;
        return (float) StrictMath.sqrt(dx * dx + dy * dy);
    }

    public void setDirectionTo(ShipTrajectoryPoint pt) {
        directionX = pt.positionX - this.positionX;
        directionY = pt.positionY - this.positionY;
        float d = (float) StrictMath.sqrt(directionX * directionX + directionY * directionY);
        directionX /= d;
        directionY /= d;
    }

    public ShipTrajectoryPoint intersection(ShipTrajectoryPoint pt) {
        float cross = pt.directionX * this.directionY - pt.directionY * this.directionX;
        if (StrictMath.abs(cross) < 0.0001f) {
            return null;
        }

        ShipTrajectoryPoint intr = new ShipTrajectoryPoint();
        float t = (pt.positionY * this.directionX - this.positionY * this.directionX - pt.positionX * this.directionY + this.positionX * this.directionY) / cross;
        intr.positionX = pt.positionX + t * pt.directionX;
        intr.positionY = pt.positionY + t * pt.directionY;
        intr.gridX = UnitGrid.toGridCoordinate(intr.positionX);
        intr.gridY = UnitGrid.toGridCoordinate(intr.positionY);
        intr.directionX = 0.0f;
        intr.directionY = 1.0f;
        return intr;
    }

    public ShipTrajectoryPoint clone() {
        ShipTrajectoryPoint pt = new ShipTrajectoryPoint();
        pt.positionX = this.positionX;
        pt.positionY = this.positionY;
        pt.directionX = this.directionX;
        pt.directionY = this.directionY;
        pt.gridX = this.gridX;
        pt.gridY = this.gridY;
        return pt;
    }

    public float angleRadTo(ShipTrajectoryPoint pt) {
        return (float) StrictMath.atan2(pt.positionY - positionY, pt.positionX - positionX);
    }

    public float angleRad() {
        return (float) StrictMath.atan2(directionY, directionX);
    }

    public float angleDeg() {
        return (float) StrictMath.toDegrees(angleRad());
    }

    @Override
    public String toString() {
        return gridX + " " + gridY;
    }
}

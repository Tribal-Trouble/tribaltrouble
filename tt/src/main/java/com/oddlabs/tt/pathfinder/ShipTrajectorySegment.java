package com.oddlabs.tt.pathfinder;

public final class ShipTrajectorySegment {
    public final ShipTrajectoryPoint p0;
    public final ShipTrajectoryPoint p1;
    public final ShipTrajectoryPoint center;
    public final boolean isStraight;
    public final boolean isPivot;
    public final float radius;
    public final float deltaAngle;
    public final float length;
    public final float cost;
    public final int angle_sign;
    public float progress = 0.0f;

    public ShipTrajectorySegment(ShipTrajectoryPoint p0, ShipTrajectoryPoint p1) {
        this.p0 = p0.clone();
        this.p1 = p1.clone();
        if (p0.gridX == p1.gridX && p0.gridY == p1.gridY) {
            this.isPivot = true;
            this.isStraight = false;
            this.length = 0.0f;
            float delta = p1.angleDeg() - p0.angleDeg();
            if (delta > 180.0f) {
                delta -= 360.0f;
            } else if (delta < -180.0f) {
                delta += 360.0f;
            }
            this.deltaAngle = delta;
            this.cost = Math.abs(delta) * 0.1f;
            this.angle_sign = delta >= 0.0f ? 1 : -1;
        } else {
            this.isStraight = true;
            this.isPivot = false;
            this.length = p0.distanceTo(p1);
            this.cost = this.length;
            this.angle_sign = 1;
            this.deltaAngle = 0.0f;
        }
        this.radius = 0.0f;
        this.center = null;
    }

    public ShipTrajectorySegment(
            ShipTrajectoryPoint p0,
            ShipTrajectoryPoint p1,
            float radius,
            ShipTrajectoryPoint center) {
        this.p0 = p0;
        this.p1 = p1;
        this.isStraight = false;
        this.isPivot = false;
        this.radius = radius;
        this.center = center;
        float swept = wrapToPi(center.angleRadTo(p1) - center.angleRadTo(p0));
        this.angle_sign = swept >= 0.0f ? +1 : -1;
        this.length = StrictMath.abs(swept) * radius;
        this.cost = length;
        this.deltaAngle = 0.0f;
    }

    private static float wrapToPi(float angle) {
        return (float) StrictMath.IEEEremainder(angle, 2.0 * StrictMath.PI);
    }

    public float advance(float distance, ShipTrajectoryPoint pose) {
        if (progress + distance >= cost) {
            pose.copyFrom(p1);
            progress = length;
            return progress + distance - length;
        }

        progress += distance;
        if (isStraight) {
            pose.setPosition(p0);
            pose.setDirectionTo(p1);
            pose.move(progress);
        } else if (isPivot) {
            float percent = progress / this.cost;
            pose.copyFrom(p0);
            pose.rotate(percent * deltaAngle);
        } else {
            float percent = progress / this.cost;
            pose.copyFrom(center);
            pose.setDirectionTo(p0);
            float deltaAngle = (float) StrictMath.toDegrees(length * percent / radius);
            pose.rotate(angle_sign * deltaAngle);
            pose.move(radius);
            pose.rotate(angle_sign * 90.0f);
        }
        return 0.0f;
    }
}

package com.oddlabs.tt.pathfinder;

public final class ShipTrajectorySegment {
    public final ShipTrajectoryPoint p0;
    public final ShipTrajectoryPoint p1;
    public final ShipTrajectoryPoint center;
    public final boolean isStraight;
    public final float radius;
    public final float length;
    public final float cost;
    public final int angle_sign;
    public float progress = 0.0f;

    public ShipTrajectorySegment(ShipTrajectoryPoint p0, ShipTrajectoryPoint p1) {
        this.p0 = p0;
        this.p1 = p1;
        this.isStraight = true;
        this.radius = 0.0f;
        this.center = null;
        this.length = p0.distanceTo(p1);
        this.cost = this.length;
        this.angle_sign = 1;
    }

    public ShipTrajectorySegment(
            ShipTrajectoryPoint p0,
            ShipTrajectoryPoint p1,
            float radius,
            ShipTrajectoryPoint center) {
        this.p0 = p0;
        this.p1 = p1;
        this.isStraight = false;
        this.radius = radius;
        this.center = center;
        float swept = wrapToPi(center.angleRadTo(p1) - center.angleRadTo(p0));
        this.angle_sign = swept >= 0.0f ? +1 : -1;
        float speed_factor = 1.0f;
        float cost_offset = 0.0f;
        if (radius < 10.0f) {
            speed_factor = 0.1f;
        }
        this.length = StrictMath.abs(swept) * radius;
        this.cost = length / speed_factor;
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

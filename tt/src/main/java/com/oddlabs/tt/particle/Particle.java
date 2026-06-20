package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Particle extends Model {
    private final float u1;
    private final float v1;
    private final float u2;
    private final float v2;
    private final float u3;
    private final float v3;
    private final float u4;
    private final float v4;
    private float angle;
    private float angularVelocity = 0f;

    private final Vector3f position = new Vector3f();
    private Color.@NonNull Linear color = Color.Linear.TRANSPARENT;
    private Color.@NonNull LinearDelta deltaColor = Color.LinearDelta.ZERO;
    private final Vector3f growthRate = new Vector3f();
    private final Vector3f radius = new Vector3f();

    private int type;
    private float energy;

    public Particle(@NonNull World world) {
        this(world, 0f);
    }

    public Particle(@NonNull World world, float angle) {
        super(world);
        this.angle = angle;
        Matrix4f rotMatrix = new Matrix4f();
        Vector3f axis = new Vector3f(0f, 0f, 1f);
        Vector4f uvVector = new Vector4f();

        rotMatrix.rotate(angle, axis);

        uvVector.set(-.5f, -.5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u1 = uvVector.x() + .5f;
        v1 = .5f - uvVector.y();

        uvVector.set(.5f, -.5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u2 = uvVector.x() + .5f;
        v2 = .5f - uvVector.y();

        uvVector.set(.5f, .5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u3 = uvVector.x() + .5f;
        v3 = .5f - uvVector.y();

        uvVector.set(-.5f, .5f, 0f, 0f);
        rotMatrix.transform(uvVector);
        u4 = uvVector.x() + .5f;
        v4 = .5f - uvVector.y();
    }

    public final float getU1() {
        return u1;
    }

    public final float getV1() {
        return v1;
    }

    public final float getU2() {
        return u2;
    }

    public final float getV2() {
        return v2;
    }

    public final float getU3() {
        return u3;
    }

    public final float getV3() {
        return v3;
    }

    public final float getU4() {
        return u4;
    }

    public final float getV4() {
        return v4;
    }

    public final float getAngle() {
        return angle;
    }

    public void update(float t) {
        color = color.add(deltaColor.mul(t));
        radius.add(growthRate.x() * t, growthRate.y() * t, growthRate.z() * t);
        angle += angularVelocity * t;
        energy -= t;
    }

    public final void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public final float getAngularVelocity() {
        return angularVelocity;
    }

    public final void setPos(float x, float y, float z) {
        position.set(x, y, z);
    }

    public final float getPosX() {
        return position.x();
    }

    public final float getPosY() {
        return position.y();
    }

    public final float getPosZ() {
        return position.z();
    }

    final void setColor(Color.@NonNull Linear color) {
        this.color = color;
    }

    public final Color.@NonNull Linear getColor() {
        return color;
    }

    public float getColorR() {
        return color.r();
    }

    public float getColorG() {
        return color.g();
    }

    public float getColorB() {
        return color.b();
    }

    public final float getColorA() {
        return color.a();
    }

    public final Color.@NonNull LinearDelta getDeltaColor() {
        return deltaColor;
    }

    public final void setDeltaColor(Color.@NonNull LinearDelta delta) {
        this.deltaColor = delta;
    }

    public final void setEnergy(float energy) {
        this.energy = energy;
    }

    public final float getEnergy() {
        return energy;
    }

    public final void setType(int type) {
        this.type = type;
    }

    public final int getType() {
        return type;
    }

    public final void setGrowthRate(float growth_rate_x, float growth_rate_y, float growth_rate_z) {
        this.growthRate.set(growth_rate_x, growth_rate_y, growth_rate_z);
    }

    public final float getGrowthRateX() {
        return growthRate.x();
    }

    public final float getGrowthRateY() {
        return growthRate.y();
    }

    public final float getGrowthRateZ() {
        return growthRate.z();
    }

    public final void setRadius(float radius_x, float radius_y, float radius_z) {
        this.radius.set(radius_x, radius_y, radius_z);
    }

    public final float getRadiusX() {
        return radius.x();
    }

    public final float getRadiusY() {
        return radius.y();
    }

    public final float getRadiusZ() {
        return radius.z();
    }


    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }
}

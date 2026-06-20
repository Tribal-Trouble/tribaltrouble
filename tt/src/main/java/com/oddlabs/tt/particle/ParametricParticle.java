package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

/**
 * A particle whose movement and position are defined by a {@link ParametricFunction}.
 * Used for complex visual effects like clouds or expanding rings.
 */
final class ParametricParticle extends Particle {
    private final @NonNull ParametricFunction function;
    private final float offset_x;
    private final float offset_y;
    private final float offset_z;

    private float velocity_u = 0f;
    private float velocity_v = 0f;
    private float u;
    private float v;

    private float localZ = 0f;
    private float heightLightingIntensity = 0f;
    private float maxLocalZ = 1f;

    public ParametricParticle(@NonNull World world, @NonNull ParametricFunction function, float u, float v,
            float offset_x, float offset_y, float offset_z) {
        super(world);
        this.function = function;
        this.u = u;
        this.v = v;
        this.offset_x = offset_x;
        this.offset_y = offset_y;
        this.offset_z = offset_z;
    }

    public void update(float t, float scale_x, float scale_y, float scale_z) {
        super.update(t);
        u += velocity_u * t;
        v += velocity_v * t;

        float x = offset_x + scale_x * function.getX(u, v);
        float y = offset_y + scale_y * function.getY(u, v);
        localZ = offset_z + scale_z * function.getZ(u, v);
        setPos(x, y, localZ);
    }

    public void setVelocity(float u, float v) {
        velocity_u = u;
        velocity_v = v;
    }

    public float getVelocityU() {
        return velocity_u;
    }

    public float getVelocityV() {
        return velocity_v;
    }

    public void setHeightLighting(float intensity, float maxLocalZ) {
        this.heightLightingIntensity = intensity;
        this.maxLocalZ = maxLocalZ;
    }

    @Override
    public float getColorR() {
        if (heightLightingIntensity <= 0.0f) {
            return super.getColorR();
        }
        float factor = localZ / maxLocalZ;
        float multiplier = 1.0f + factor * heightLightingIntensity;
        return Math.clamp(super.getColorR() * multiplier, 0f, 1f);
    }

    @Override
    public float getColorG() {
        if (heightLightingIntensity <= 0.0f) {
            return super.getColorG();
        }
        float factor = localZ / maxLocalZ;
        float multiplier = 1.0f + factor * heightLightingIntensity;
        return Math.clamp(super.getColorG() * multiplier, 0f, 1f);
    }

    @Override
    public float getColorB() {
        if (heightLightingIntensity <= 0.0f) {
            return super.getColorB();
        }
        float factor = localZ / maxLocalZ;
        float multiplier = 1.0f + factor * heightLightingIntensity;
        return Math.clamp(super.getColorB() * multiplier, 0f, 1f);
    }
}

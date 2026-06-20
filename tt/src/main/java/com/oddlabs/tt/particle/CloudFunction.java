package com.oddlabs.tt.particle;

public final class CloudFunction implements ParametricFunction {
    public static final float TOP_PUFFINESS_BASE = 1.0f;
    public static final float TOP_PUFFINESS_PEAK = 1.5f;
    public static final float BOTTOM_FLATNESS_FACTOR = 0.5f;

    private final float radius_xy;
    private final float radius_z;

    public CloudFunction(float radius_xy, float radius_z) {
        this.radius_xy = radius_xy;
        this.radius_z = radius_z;
    }

    public float getRadiusZ() {
        return radius_z;
    }

    @Override
    public float getX(float u, float v) {
        return radius_xy * (float) Math.sin(u) * (float) Math.cos(v);
    }

    @Override
    public float getY(float u, float v) {
        return radius_xy * (float) Math.sin(u) * (float) Math.sin(v);
    }

    @Override
    public float getZ(float u, float v) {
        float cosU = (float) Math.cos(u);
        if (cosU >= 0f) {
            float puffiness = TOP_PUFFINESS_BASE + cosU * (TOP_PUFFINESS_PEAK - TOP_PUFFINESS_BASE);
            return radius_z * cosU * puffiness;
        } else {
            return radius_z * cosU * BOTTOM_FLATNESS_FACTOR;
        }
    }
}

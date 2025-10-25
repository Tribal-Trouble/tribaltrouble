package com.oddlabs.tt.particle;

public final class StunFunction implements ParametricFunction {
	private final float radius;
	private final float height;

	public StunFunction(float radius, float height) {
		this.radius = radius;
		this.height = height;
	}

    @Override
	public float getX(float u, float v) {
		return radius*(float)StrictMath.cos(u);
	}

    @Override
	public float getY(float u, float v) {
		return radius*(float)StrictMath.sin(u);
	}

    @Override
	public float getZ(float u, float v) {
		return height*(float)StrictMath.cos(v);
	}
}

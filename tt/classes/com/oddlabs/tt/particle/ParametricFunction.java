package com.oddlabs.tt.particle;

public interface ParametricFunction {
	public float getX(float u, float v);
	public float getY(float u, float v);
	public float getZ(float u, float v);
}

package com.oddlabs.tt.landscape;

final class Errors {
	final float[] errors;
	final boolean intersects_water;

	Errors(float[] errors, boolean intersects_water) {
		this.errors = errors;
		this.intersects_water = intersects_water;
	}
}

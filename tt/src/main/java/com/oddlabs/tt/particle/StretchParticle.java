package com.oddlabs.tt.particle;

public final class StretchParticle extends Particle {
	private float src_x = 0f;
	private float src_y = 0f;
	private float src_z = 0f;
	private float dst_x = 0f;
	private float dst_y = 0f;
	private float dst_z = 0f;
	private float src_width = 0f;
	private float dst_width = 0f;

    @Override
	public void update(float t) {
		super.update(t);
	}

	public void setSrc(float x, float y, float z) {
		src_x = x;
		src_y = y;
		src_z = z;
	}

	public float getSrcX() {
		return src_x;
	}

	public float getSrcY() {
		return src_y;
	}

	public float getSrcZ() {
		return src_z;
	}

	public void setDst(float x, float y, float z) {
		dst_x = x;
		dst_y = y;
		dst_z = z;
	}

	public float getDstX() {
		return dst_x;
	}

	public float getDstY() {
		return dst_y;
	}

	public float getDstZ() {
		return dst_z;
	}

	public void setSrcWidth(float width) {
		src_width = width;
	}

	public float getSrcWidth() {
		return src_width;
	}

	public void setDstWidth(float width) {
		dst_width = width;
	}

	public float getDstWidth() {
		return dst_width;
	}
}

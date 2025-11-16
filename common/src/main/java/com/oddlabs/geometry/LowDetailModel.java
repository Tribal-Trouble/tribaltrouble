package com.oddlabs.geometry;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class LowDetailModel implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final float[] vertices;
	private final float[] tex_coords;
	private final short @NonNull [] indices;
	private final int poly_count;

	public LowDetailModel(short @NonNull [] indices, float[] vertices, float[] tex_coords) {
		this.indices = indices;
		this.vertices = vertices;
		this.tex_coords = tex_coords;
		this.poly_count = indices.length/3;
	}

	public short[] getIndices() {
		return indices;
	}

	public float[] getVertices() {
		return vertices;
	}

	public float[] getTexCoords() {
		return tex_coords;
	}

	public int getPolyCount() {
		return poly_count;
	}
}

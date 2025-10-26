package com.oddlabs.geometry;

import com.oddlabs.util.ByteCompressedFloatArray;
import com.oddlabs.util.ShortCompressedFloatArray;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;

public final class SpriteInfo implements Serializable {
	private final static long serialVersionUID = 1;

	private final short[] indices;
	private final @NonNull ShortCompressedFloatArray vertices;
	private final @NonNull ByteCompressedFloatArray normals;
	private final @NonNull ShortCompressedFloatArray texcoords;
	private final byte[][] skin_names;
	private final float[][] skin_weights;
	private final String[][] textures;
	private final float[] clear_color;

	public SpriteInfo(String[][] textures, short[] indices, float @NonNull [] vertices, float @NonNull [] normals, float @NonNull [] texcoords, byte[][] skin_names, float[][] skin_weights, float[] clear_color) {
		this.textures = textures;
		this.indices = indices;
		this.vertices = new ShortCompressedFloatArray(vertices, 3);
		this.normals = new ByteCompressedFloatArray(normals, 3);
		this.texcoords = new ShortCompressedFloatArray(texcoords, 2);
		this.skin_names = skin_names;
		this.skin_weights = skin_weights;
		this.clear_color = clear_color;
	}

	public short[] getIndices() {
		return indices;
	}

	public float[] getVertices() {
		return vertices.getFloatArray();
	}

	public float[] getNormals() {
		return normals.getFloatArray();
	}

	public float[] getTexCoords() {
		return texcoords.getFloatArray();
	}

	public byte[][] getSkinNames() {
		return skin_names;
	}

	public float[][] getSkinWeights() {
		return skin_weights;
	}

	public String[][] getTextures() {
		return textures;
	}

/*	public final BoundingBox getBounds() {
		return bounds;
	}
*/
	public float[] getClearColor() {
		return clear_color;
	}
}

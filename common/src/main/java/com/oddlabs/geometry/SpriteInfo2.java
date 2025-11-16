package com.oddlabs.geometry;

import com.oddlabs.util.ByteCompressedFloatArray;
import com.oddlabs.util.ShortCompressedFloatArray;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class SpriteInfo2 implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final short[] indices;
	private final @NonNull ShortCompressedFloatArray vertices;
	private final @NonNull ByteCompressedFloatArray normals;
	private final @NonNull ShortCompressedFloatArray texcoords;
	private final @Nullable ShortCompressedFloatArray texcoords2;
	private final byte[][] skin_names;
	private final float[][] skin_weights;
	private final String[][] textures;
	private final float[] clear_color;

	public SpriteInfo2(String[][] textures, short[] indices, float @NonNull [] vertices, float @NonNull [] normals, float @NonNull [] texcoords, float @Nullable [] texcoords2, byte[][] skin_names, float[][] skin_weights, float[] clear_color) {
		this.textures = textures;
		this.indices = indices;
		this.vertices = new ShortCompressedFloatArray(vertices, 3);
		this.normals = new ByteCompressedFloatArray(normals, 3);
		this.texcoords = new ShortCompressedFloatArray(texcoords, 2);
		if (texcoords2 != null)
			this.texcoords2 = new ShortCompressedFloatArray(texcoords2, 2);
		else
			this.texcoords2 = null;
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

	public float[] getTexCoords2() {
		if (texcoords2 != null)
			return texcoords2.getFloatArray();
		else
			return null;
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

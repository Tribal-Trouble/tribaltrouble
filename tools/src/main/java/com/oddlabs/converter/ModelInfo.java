package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ModelInfo {
	public final short @NonNull[] indices;
	public final float @NonNull[] normals;
	public final float @NonNull[] vertices;
	public final float @NonNull[] colors;
	public final float @NonNull[] texcoords;
	public final float @Nullable[] texcoords2;
	public final byte @NonNull[] @NonNull[] skin_names;
	public final float @NonNull[] @NonNull[] skin_weights;
//	public final String tex_name;

    public ModelInfo(/*String tex_name, */short @NonNull [] indices, float @NonNull [] vertices, float @NonNull[] normals, float @NonNull[] colors, float @NonNull[] texcoords, byte @NonNull[] @NonNull[] skin_names, float @NonNull[] @NonNull[] skin_weights) {
        this(indices, vertices, normals, colors, texcoords, null, skin_names, skin_weights);
    }

	public ModelInfo(/*String tex_name, */short @NonNull[] indices, float @NonNull[] vertices, float @NonNull[] normals, float @NonNull[] colors, float @NonNull [] texcoords, float @Nullable[] texcoords2, byte @NonNull[] @NonNull[] skin_names, float @NonNull[] @NonNull[] skin_weights) {
		this.normals = normals;
		this.vertices = vertices;
		this.indices = indices;
		this.colors = colors;
		this.texcoords = texcoords;
		this.texcoords2 = texcoords2;
		this.skin_names = skin_names;
		this.skin_weights = skin_weights;
//		this.tex_name = tex_name;
	}
}

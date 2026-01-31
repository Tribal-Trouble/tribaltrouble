package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ModelInfo(short @NonNull [] indices, float @NonNull [] vertices, float @NonNull [] normals,
						float @NonNull [] colors, float @NonNull [] texcoords, float @Nullable [] texcoords2,
						byte @NonNull [] @NonNull [] skin_names, float @NonNull [] @NonNull [] skin_weights) {
	//	public final String tex_name;

	public ModelInfo(/*String tex_name, */short @NonNull [] indices, float @NonNull [] vertices, float @NonNull [] normals, float @NonNull [] colors, float @NonNull [] texcoords, byte @NonNull [] @NonNull [] skin_names, float @NonNull [] @NonNull [] skin_weights) {
		this(indices, vertices, normals, colors, texcoords, null, skin_names, skin_weights);
	}

	/*String tex_name, */
	//		this.tex_name = tex_name;
}

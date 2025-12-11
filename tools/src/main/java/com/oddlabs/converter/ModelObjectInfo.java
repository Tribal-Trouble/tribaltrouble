package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;

import java.io.File;

public final class ModelObjectInfo extends ObjectInfo {
	private final float @NonNull [] clear_color;
	private final @NonNull String @NonNull [] @NonNull [] textures;

	public ModelObjectInfo(@NonNull File file, @NonNull String @NonNull [] @NonNull [] textures, float @NonNull [] clear_color) {
		super(file);
		this.textures = textures;
		this.clear_color = clear_color;
	}

	public @NonNull String @NonNull [] @NonNull [] getTextures() {
		return textures;
	}
	
	public float @NonNull [] getClearColor() {
		return clear_color;
	}
}

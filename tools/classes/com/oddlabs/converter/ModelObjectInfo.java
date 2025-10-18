package com.oddlabs.converter;

import java.io.File;

public final class ModelObjectInfo extends ObjectInfo {
	private final float[] clear_color;
	private final String[][] textures;

	public ModelObjectInfo(File file, String[][] textures, float[] clear_color) {
		super(file);
		this.textures = textures;
		this.clear_color = clear_color;
	}

	public String[][] getTextures() {
		return textures;
	}
	
	public float[] getClearColor() {
		return clear_color;
	}
}

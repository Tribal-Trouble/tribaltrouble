package com.oddlabs.converter;

import java.io.File;

public final class AnimObjectInfo extends ObjectInfo {
	private final float wpc;
	private final int type;

	public AnimObjectInfo(File file, float wpc, int type) {
		super(file);
		this.wpc = wpc;
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public float getWPC() {
		return wpc;
	}
}

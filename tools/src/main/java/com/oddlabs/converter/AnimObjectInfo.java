package com.oddlabs.converter;

import com.oddlabs.geometry.AnimationInfo;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

public final class AnimObjectInfo extends ObjectInfo {
	private final float wpc;
	private final AnimationInfo.@NonNull AnimationType type;

	public AnimObjectInfo(@NonNull Path file, float wpc, AnimationInfo.@NonNull AnimationType type) {
		super(file);
		this.wpc = wpc;
		this.type = type;
	}

	public AnimationInfo.@NonNull AnimationType getType() {
		return type;
	}

	public float getWPC() {
		return wpc;
	}
}

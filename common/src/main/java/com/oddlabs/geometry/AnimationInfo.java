package com.oddlabs.geometry;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class AnimationInfo implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	public enum AnimationType {
		LOOP,
		PLAIN
	}

	private final float @NonNull [] @NonNull [] frames;
	private final @NonNull AnimationType type;
	private final float wpc;
	private final @NonNull String name;

	public AnimationInfo(float @NonNull [] @NonNull [] frames, @NonNull AnimationType type, float wpc, @NonNull String name) {
		this.frames = frames;
		this.type = type;
		this.wpc = wpc;
		this.name = name;
	}

	public float @NonNull [] @NonNull [] getFrames() {
		return frames;
	}

	public @NonNull AnimationType getType() {
		return type;
	}

	public float getWPC() {
		return wpc;
	}

	public @NonNull String getName() {
		return name;
	}
}

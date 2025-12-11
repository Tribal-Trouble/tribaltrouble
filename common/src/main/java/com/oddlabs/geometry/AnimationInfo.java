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

	private final float[] @NonNull [] frames;
	private final @NonNull AnimationType type;
	private final float wpc;

	public AnimationInfo(float @NonNull [] @NonNull [] frames, @NonNull AnimationType type, float wpc) {
		this.frames = frames;
		this.type = type;
		this.wpc = wpc;
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
}

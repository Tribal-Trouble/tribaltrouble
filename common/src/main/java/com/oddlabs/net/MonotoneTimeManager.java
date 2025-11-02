package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

public final class MonotoneTimeManager implements TimeManager {
	private final @NonNull TimeManager source;
	private long last_time;

	public MonotoneTimeManager(@NonNull TimeManager source) {
		this.source = source;
		this.last_time = source.getMillis();
	}

    @Override
	public long getMillis() {
		long new_time = source.getMillis();
		this.last_time = Math.max(last_time, new_time);
		return last_time;
	}
}

package com.oddlabs.net;

public final class MonotoneTimeManager implements TimeManager {
	private final TimeManager source;
	private long last_time;

	public MonotoneTimeManager(TimeManager source) {
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

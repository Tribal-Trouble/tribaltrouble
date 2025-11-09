package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.trigger.IntervalTrigger;
import org.jspecify.annotations.NonNull;

public final class TimeTrigger extends IntervalTrigger {
	private final Runnable runnable;

	public TimeTrigger(@NonNull World world, float time, Runnable runnable) {
		super(time, 0f, world.getAnimationManagerGameTime());
		this.runnable = runnable;
	}

	@Override
	protected void check() {
		triggered();
	}

	@Override
	protected void done() {
		runnable.run();
	}
}

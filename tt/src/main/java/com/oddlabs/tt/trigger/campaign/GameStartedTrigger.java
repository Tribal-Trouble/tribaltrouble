package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.trigger.IntervalTrigger;
import org.jspecify.annotations.NonNull;

public final class GameStartedTrigger extends IntervalTrigger {
	private final Runnable runnable;

	public GameStartedTrigger(@NonNull World world, Runnable runnable) {
		super(world, .25f, 0f);
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

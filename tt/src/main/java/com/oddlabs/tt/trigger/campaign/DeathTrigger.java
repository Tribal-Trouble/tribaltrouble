package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.trigger.IntervalTrigger;
import org.jspecify.annotations.NonNull;

public final class DeathTrigger extends IntervalTrigger {
	private final @NonNull Selectable<?> selectable;
	private final @NonNull Runnable runnable;

	public DeathTrigger(@NonNull Selectable<?> selectable, @NonNull Runnable runnable) {
		super(selectable.getOwner().getWorld(), .5f, 0f);
		this.selectable = selectable;
		this.runnable = runnable;
	}

	@Override
	protected void check() {
		if (selectable.isDead())
			triggered();
	}

	@Override
	protected void done() {
		runnable.run();
	}
}

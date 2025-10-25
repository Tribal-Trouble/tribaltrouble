package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.trigger.IntervalTrigger;

public final class NearPointTrigger extends IntervalTrigger {
	private final int grid_x;
	private final int grid_y;
	private final int r;
	private final Unit unit;
	private final Runnable runnable;

	public NearPointTrigger(int grid_x, int grid_y, int r, Unit unit, Runnable runnable) {
		super(unit.getOwner().getWorld(), .25f, 0f);
		this.grid_x = grid_x;
		this.grid_y = grid_y;
		this.r = r;
		this.unit = unit;
		this.runnable = runnable;
	}

        @Override
	protected void check() {
		if (!unit.isDead()) {
			int dx = unit.getGridX() - grid_x;
			int dy = unit.getGridY() - grid_y;
			int squared_dist = dx*dx + dy*dy;
			if (squared_dist < r*r)
				triggered();
		}
	}

        @Override
	protected void done() {
		runnable.run();
	}
}

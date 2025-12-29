package com.oddlabs.tt.trigger.campaign;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.trigger.IntervalTrigger;
import org.jspecify.annotations.NonNull;

public final class NearArmyTrigger extends IntervalTrigger {
	private final @NonNull Unit[] src;
	private final float r;
	private final @NonNull Player player;
	private final @NonNull Runnable runnable;

	public NearArmyTrigger(Unit[] src, float r, @NonNull Player player, @NonNull Runnable runnable) {
		super(player.getWorld(), .25f, 0f);
		this.src = src;
		this.r = r;
		this.player = player;
		this.runnable = runnable;
	}

	@Override
	protected void check() {
		for (Unit unit : src) {
			if (unit.isDead()) {
				continue;
			}
			FindOccupantFilter<Unit> filter = new FindOccupantFilter<>(unit.getPositionX(), unit.getPositionY(), r, unit, Unit.class);
			player.getWorld().getUnitGrid().scan(filter, unit.getGridX(), unit.getGridY());
			for (Unit filtered : filter.getResult()) {
				if (!filtered.isDead() && filtered.getOwner() == player) {
					triggered();
					return;
				}
			}
		}
	}

	@Override
	protected void done() {
		runnable.run();
	}
}

package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

public final class RubberSupplyManager extends SupplyManager {
	private final static float SLEEP_TICKS = 60;
	private final static int MAX_NUM_GROUPS = 3;

	private int current_groups = 0;
	
	public RubberSupplyManager(@NonNull World world) {
		super(world);
	}

	@Override
	protected float getSleepTime() {
		return SLEEP_TICKS;
	}

	@Override
	protected boolean shouldSpawn() {
		return current_groups < MAX_NUM_GROUPS;
	}

	@Override
	protected void insertSupply() {
		new RubberGroup(getWorld());
	}

	public void newGroup() {
		current_groups++;
	}

	public void emptyGroup() {
		current_groups--;
	}
}

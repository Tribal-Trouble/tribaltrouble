package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SupplyManager implements Animated {
	private final static float SLEEP_TIME = 10f;
	private final static float SPAWN_TIME = 3f;
	private final static float MAX_EMPTY_SUPPLIES = .75f;
	
	private final List<Supply> empty_supplies = new ArrayList<>();
	private final World world;

	private int total_num_supplies = 0;
	private float time;

	
	public SupplyManager(@NonNull World world) {
		this.world = world;
		world.getAnimationManagerGameTime().registerAnimation(this);
		resetCounter();
	}

	protected final World getWorld() {
		return world;
	}

	private void resetCounter() {
		time = getSleepTime();
	}

	protected float getSleepTime() {
		return SLEEP_TIME;
	}

	public final void debugSpawnSupply() {
		if (!empty_supplies.isEmpty())
			insertSupply();
	}

	public final void newSupply() {
		total_num_supplies++;
	}

	public final void emptySupply(Supply supply) {
		empty_supplies.add(supply);
	}

        @Override
	public final void animate(float t) {
		if (time < 0) {
			resetCounter();
			if (shouldSpawn())
				insertSupply();
		}
		time -= t;
	}

	protected boolean shouldSpawn() {
		return (int)(total_num_supplies*MAX_EMPTY_SUPPLIES) < empty_supplies.size();
	}

        @Override
	public void updateChecksum(StateChecksum checksum) {
	}

	protected void insertSupply() {
		int index = world.getRandom().nextInt(empty_supplies.size());
		Supply supply = empty_supplies.get(index);
		boolean occupied = world.getUnitGrid().isGridOccupied(supply.getGridX(), supply.getGridY());
		if (!occupied) {
			empty_supplies.remove(supply);
			Supply new_supply = supply.respawn();
			new SupplySpawnAnimation(new_supply, SPAWN_TIME);
		}
	}
}

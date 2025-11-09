package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;


public class SupplySpawnAnimation implements Animated {
	private final @NonNull Supply supply;
	private final float limit;

	private float time = 0;
	
	public SupplySpawnAnimation(@NonNull Supply supply, float limit) {
		this.supply = supply;
		this.limit = limit;
		supply.getWorld().getAnimationManagerGameTime().registerAnimation(this);
		supply.animateSpawn(0, 0);
	}

	@Override
	public final void animate(float t) {
		time = Math.min(time + t, limit);
		supply.animateSpawn(t, time/limit);
		if (time >= limit) {
			supply.getWorld().getAnimationManagerGameTime().removeAnimation(this);
			supply.spawnComplete();
		}
	}

	@Override
	public void updateChecksum(StateChecksum checksum) {
	}
}

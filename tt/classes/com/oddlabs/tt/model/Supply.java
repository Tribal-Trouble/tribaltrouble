package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;

public interface Supply extends Occupant {
	public final static int HITS_PER_HARVEST = 10;
		
	boolean isEmpty();
	boolean hit();
	Supply respawn();
	void animateSpawn(float t, float progress);
	void spawnComplete();
	World getWorld();
}

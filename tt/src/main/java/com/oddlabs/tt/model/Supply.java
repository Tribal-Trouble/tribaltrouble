package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import org.jspecify.annotations.NonNull;

public interface Supply extends Occupant {
    int HITS_PER_HARVEST = 10;

    boolean isEmpty();

    boolean hit();

    @NonNull Supply respawn();

    void animateSpawn(float t, float progress);

    void spawnComplete();

    @NonNull World getWorld();
}

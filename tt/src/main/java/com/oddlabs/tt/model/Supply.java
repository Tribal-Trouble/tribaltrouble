package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Interface representing a harvestable resource supply in the simulation.
 */
public interface Supply extends Occupant {
    int HITS_PER_HARVEST = 10;

    @NonNull
    default String getName() {
        return Utils.getBundleString(ResourceBundle.getBundle(getClass().getName()), "name");
    }


    boolean isEmpty();

    boolean hit();

    @NonNull
    Supply respawn();

    void animateSpawn(float t, float progress);

    void spawnComplete();

    @NonNull
    World getWorld();
}

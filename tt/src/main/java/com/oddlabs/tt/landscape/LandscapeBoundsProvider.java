package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.BoundsProvider;
import com.oddlabs.tt.model.Terrain;
import org.jspecify.annotations.NonNull;

/**
 * Provides access to visual bounds of landscape elements (rocks, iron, plants, chickens)
 * for collision and bounding box calculations in the simulation.
 * <p/>
 * For now, this interface is also used to smuggle a SpriteKey and downcasted.
 */
public interface LandscapeBoundsProvider {
    /** Iron/Rock supply fragment variations */
    int SUPPLY_FRAGMENT_COUNT = 5;

    @NonNull
    BoundsProvider getRockBounds(int index);

    @NonNull
    BoundsProvider getIronBounds(int index);

    @NonNull
    BoundsProvider getPlantBounds(Terrain terrain, int index);

    @NonNull
    BoundsProvider getChickenBounds();
}

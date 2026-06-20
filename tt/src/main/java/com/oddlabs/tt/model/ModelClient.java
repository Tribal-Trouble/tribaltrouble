package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

/**
 * Callbacks that the simulation can trigger on the client-side state.
 */
public interface ModelClient extends ClientState, AutoCloseable {
    float DURATION_CHICKEN_CLUCK = 0.8f;
    float DURATION_UNIT_DEATH = 1.5f;
    float DURATION_HARVEST = 1.0f;
    float DURATION_REPAIR = 1.0f;

    /**
     * Spawns a visual sound emoji/thought accessory on the model.
     *
     * @param emoji The emoji type of the thought.
     * @param duration The duration to display the thought.
     * @param audioDistance The maximum distance at which the sound/visual is audible.
     */
    void addVisualSound(@NonNull EmojiType emoji, float duration, float audioDistance);

    /**
     * Spawns a visual lightning strike from this model to the specified destination coordinate.
     *
     * @param targetX The destination X coordinate.
     * @param targetY The destination Y coordinate.
     * @param targetZ The destination Z coordinate.
     */
    void addLightningStrike(float targetX, float targetY, float targetZ);

    /**
     * Cleans up any resources (like active audio loops) associated with the client-side model.
     */
    @Override
    void close();
}

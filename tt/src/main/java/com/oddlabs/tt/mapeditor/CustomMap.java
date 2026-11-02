package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.resource.WorldGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Random;

/** A saved map chosen to play, with what the skirmish menu needs to start a game on it. */
public final class CustomMap {
    private final @NonNull String name;
    private final @NonNull MapSettings settings;
    private final @NonNull Path path;
    // The hash the map is shared on the server by, for a multiplayer game, or null.
    private final @Nullable String shared_hash;
    private final @NonNull SpawnChoice spawn_choice;
    // Picked here, by the host, so everyone in the game shuffles the spawns alike.
    private final long spawn_seed = new Random().nextLong();

    CustomMap(@NonNull String name, @NonNull MapSettings settings, @NonNull Path path,
            @NonNull SpawnChoice spawn_choice) {
        this(name, settings, path, null, spawn_choice);
    }

    /** @param spawn_choice how the players start at the map's spawns, if it has any */
    CustomMap(@NonNull String name, @NonNull MapSettings settings, @NonNull Path path,
            @Nullable String shared_hash, @NonNull SpawnChoice spawn_choice) {
        this.name = name;
        this.settings = settings;
        this.path = path;
        this.shared_hash = shared_hash;
        this.spawn_choice = spawn_choice;
    }

    public @NonNull String getName() {
        return name;
    }

    /** The island size, as an index into the skirmish menu's sizes. */
    public int getSizeIndex() {
        return settings.size();
    }

    public int getTerrain() {
        return settings.terrain();
    }

    public int getHills() {
        return settings.hills();
    }

    public int getTrees() {
        return settings.trees();
    }

    public int getSupplies() {
        return settings.supplies();
    }

    public @NonNull String getMapcode() {
        return settings.toMapcode();
    }

    /** The hash the map is shared by on the server, or null when it is played from its file. */
    public @Nullable String getSharedHash() {
        return shared_hash;
    }

    /**
     * A generator building the island from the settings, with the map's saved heights and resources in place and the
     * players starting as chosen. A shared map is read from the maps downloaded from the server, where every player in
     * the game has it.
     */
    public @NonNull WorldGenerator createGenerator() {
        return settings.createGenerator(shared_hash != null ? new SharedMapSource(shared_hash, spawn_choice,
                spawn_seed) : new SavedMapSource(path, spawn_choice, spawn_seed));
    }
}

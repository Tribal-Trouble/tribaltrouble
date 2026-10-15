package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.procedural.LandscapeOverride;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

/**
 * Builds an island from a map shared on the matchmaking server. Only the map's hash travels with the generator,
 * from the host of a game to the players joining it, who download the map by it before they can be ready.
 */
final class SharedMapSource implements LandscapeOverride.Source {
    @Serial
    private static final long serialVersionUID = 2;

    private final @NonNull String hash;
    private final @NonNull SpawnChoice spawn_choice;
    private final long spawn_seed;

    /**
     * @param spawn_choice how the players start at the map's spawns, if it has any
     * @param spawn_seed   the order of chance of shuffled spawns, the same for everyone as the host picked it
     */
    SharedMapSource(@NonNull String hash, @NonNull SpawnChoice spawn_choice, long spawn_seed) {
        this.hash = hash;
        this.spawn_choice = spawn_choice;
        this.spawn_seed = spawn_seed;
    }

    @NonNull
    String getHash() {
        return hash;
    }

    @NonNull
    SpawnChoice getSpawnChoice() {
        return spawn_choice;
    }

    @Override
    public @NonNull LandscapeOverride load() throws IOException {
        Path path = SharedMaps.cachePath(hash);
        if (path == null)
            throw new IOException("There is no folder to keep maps in");
        return SavedMapSource.override(MapFile.load(path), spawn_choice, spawn_seed);
    }
}

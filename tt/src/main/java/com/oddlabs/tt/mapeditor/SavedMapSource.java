package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.procedural.LandscapeOverride;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

/**
 * Builds an island from a saved map's heights, resources and spawns. Only the file's path travels with the
 * generator, so this works where the client can read the file, as in a single player game on this computer.
 */
final class SavedMapSource implements LandscapeOverride.Source {
    @Serial
    private static final long serialVersionUID = 2;

    private final @NonNull String path;
    private final @NonNull SpawnChoice spawn_choice;
    private final long spawn_seed;

    /**
     * @param spawn_choice how the players start at the map's spawns, if it has any
     * @param spawn_seed   the order of chance of shuffled spawns
     */
    SavedMapSource(@NonNull Path path, @NonNull SpawnChoice spawn_choice, long spawn_seed) {
        this.path = path.toAbsolutePath().toString();
        this.spawn_choice = spawn_choice;
        this.spawn_seed = spawn_seed;
    }

    @NonNull
    SpawnChoice getSpawnChoice() {
        return spawn_choice;
    }

    @Override
    public @NonNull LandscapeOverride load() throws IOException {
        return override(MapFile.load(Path.of(path)), spawn_choice, spawn_seed);
    }

    /** The heights and resources a map builds its island with, and where the players start on it. */
    static @NonNull LandscapeOverride override(@NonNull MapFile map, @NonNull SpawnChoice spawn_choice,
            long spawn_seed) {
        MapFile.Resources resources = map.resources();
        return new LandscapeOverride(map.heights(), resources == null ? null : new LandscapeOverride.Resources(
                resources.of(Resource.TREE), resources.of(Resource.PALM), resources.of(Resource.ROCK),
                resources.of(Resource.IRON)), spawn_choice.apply(map.spawns(), spawn_seed));
    }
}

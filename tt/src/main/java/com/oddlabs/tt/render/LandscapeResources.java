package com.oddlabs.tt.render;

import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.model.Terrain;
import com.oddlabs.tt.resource.SpriteFile;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.stream.IntStream;

/**
 * Client-side resource manager that loads landscape-associated sprites (rocks, iron, plants, chickens)
 * and exposes their physical bounds to the simulation via {@link LandscapeBoundsProvider}.
 */
public final class LandscapeResources implements LandscapeBoundsProvider {
    public static final int SUPPLY_FRAGMENT_COUNT = 5;

    private final @NonNull SpriteKey @NonNull [] rock_fragment_sprites;
    private final @NonNull SpriteKey @NonNull [] iron_fragment_sprites;
    private final @NonNull EnumMap<Terrain, SpriteKey[]> plant_sprites = new EnumMap<>(Terrain.class);
    private final @NonNull SpriteKey chicken;

    public LandscapeResources(@NonNull RenderQueues queues) {
        int num_progress = 13;
        ProgressForm.progress(10f / num_progress);

        var fragments = IntStream.rangeClosed(1, SUPPLY_FRAGMENT_COUNT)
                .mapToObj(i -> String.format("/geometry/misc/rock_%d.binsprite", i))
                .map(rsrc -> new SpriteFile(rsrc, Globals.NO_MIPMAP_CUTOFF, true, true, true, false))
                .toArray(SpriteFile[]::new);

        rock_fragment_sprites = Arrays.stream(fragments)
                .map(queues::register)
                .toArray(SpriteKey[]::new);

        iron_fragment_sprites = Arrays.stream(fragments)
                .map(spriteFile -> queues.register(spriteFile, 1))
                .toArray(SpriteKey[]::new);
        ProgressForm.progress(1f / num_progress);

        plant_sprites.put(
                Terrain.NATIVE, IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> String.format("/geometry/misc/plant_%d.binsprite", i))
                        .map(rsrc -> new SpriteFile(rsrc, Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true))
                        .map(queues::register)
                        .toArray(SpriteKey[]::new));
        plant_sprites.put(
                Terrain.VIKING, IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> String.format("/geometry/misc/viking_plant_%d.binsprite", i))
                        .map(rsrc -> new SpriteFile(rsrc, Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true))
                        .map(queues::register)
                        .toArray(SpriteKey[]::new));
        ProgressForm.progress(1f / num_progress);

        SpriteFile sprite_list_chicken = new SpriteFile("/geometry/misc/chicken.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        chicken = queues.register(sprite_list_chicken);

        ProgressForm.progress(1f / num_progress);
    }

    public @NonNull SpriteKey getChicken() {
        return chicken;
    }

    @Override
    public @NonNull SpriteKey getRockBounds(int index) {
        return rock_fragment_sprites[index % rock_fragment_sprites.length];
    }

    @Override
    public @NonNull SpriteKey getIronBounds(int index) {
        return iron_fragment_sprites[index % iron_fragment_sprites.length];
    }

    @Override
    public @NonNull SpriteKey getPlantBounds(Terrain terrain, int index) {
        var sprites = plant_sprites.get(terrain);
        return sprites[index % sprites.length];
    }

    @Override
    public @NonNull SpriteKey getChickenBounds() {
        return chicken;
    }
}

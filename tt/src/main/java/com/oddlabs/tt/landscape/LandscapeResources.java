package com.oddlabs.tt.landscape;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public final class LandscapeResources {
    private final @NonNull SpriteKey @NonNull [] rock_fragment_sprites;
    private final @NonNull SpriteKey @NonNull [] iron_fragment_sprites;
    private final @NonNull SpriteKey @NonNull [] @NonNull [] plant_sprites;
    private final @NonNull SpriteKey chicken;
    private final @NonNull Audio @NonNull [] bird_idle_sound;
    private final @NonNull Audio bird_peck_sound;
    private final @NonNull Audio bird_death_sound;

    public LandscapeResources(@NonNull RenderQueues queues) {
        int num_progress = 13;
        ProgressForm.progress(10f / num_progress);

        var fragments = IntStream.rangeClosed(1, 5).mapToObj(i -> String.format("/geometry/misc/rock_%d.binsprite",
                i)).map(rsrc -> new SpriteFile(rsrc, Globals.NO_MIPMAP_CUTOFF, true, true, true, false)).toArray(
                        SpriteFile[]::new);

        rock_fragment_sprites = Arrays.stream(fragments).map(queues::register).toArray(SpriteKey[]::new);

        iron_fragment_sprites = Arrays.stream(fragments).map(spriteFile -> queues.register(spriteFile, 1)).toArray(
                SpriteKey[]::new);
        ProgressForm.progress(1f / num_progress);

        plant_sprites = new SpriteKey[][]{IntStream.rangeClosed(1, 4).mapToObj(i -> String.format(
                "/geometry/misc/plant_%d.binsprite", i)).map(rsrc -> new SpriteFile(rsrc, Globals.NO_MIPMAP_CUTOFF,
                        true, false, true, true, true)).map(queues::register).toArray(
                                SpriteKey[]::new), IntStream.rangeClosed(1, 4).mapToObj(i -> String.format(
                                        "/geometry/misc/viking_plant_%d.binsprite", i)).map(rsrc -> new SpriteFile(rsrc,
                                                Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true)).map(
                                                        queues::register).toArray(SpriteKey[]::new)
        };
        ProgressForm.progress(1f / num_progress);

        SpriteFile sprite_list_chicken = new SpriteFile("/geometry/misc/chicken.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        chicken = queues.register(sprite_list_chicken);

        bird_idle_sound = IntStream.rangeClosed(1, 4).mapToObj(i -> String.format("/sfx/chicken_idle%d.ogg", i)).map(
                AudioFile::new).map(Resources::findResource).toArray(Audio[]::new);
        bird_peck_sound = Resources.findResource(new AudioFile("/sfx/chicken_peck.ogg"));
        bird_death_sound = Resources.findResource(new AudioFile("/sfx/chicken_death.ogg"));
        ProgressForm.progress(1f / num_progress);
    }

    public @NonNull SpriteKey @NonNull [] getRockFragments() {
        return rock_fragment_sprites;
    }

    public @NonNull SpriteKey @NonNull [] getIronFragments() {
        return iron_fragment_sprites;
    }

    public @NonNull SpriteKey @NonNull [] @NonNull [] getPlants() {
        return plant_sprites;
    }

    public @NonNull SpriteKey getChicken() {
        return chicken;
    }

    public @NonNull Audio getBirdIdleSound(@NonNull Random random) {
        return bird_idle_sound[random.nextInt(bird_idle_sound.length)];
    }

    public @NonNull Audio getBirdPeckSound() {
        return bird_peck_sound;
    }

    public @NonNull Audio getBirdDeathSound() {
        return bird_death_sound;
    }
}

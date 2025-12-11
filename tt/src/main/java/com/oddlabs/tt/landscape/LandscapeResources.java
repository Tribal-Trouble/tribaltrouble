package com.oddlabs.tt.landscape;

import com.oddlabs.geometry.LowDetailModel;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public final class LandscapeResources {
	private final SpriteKey[] rock_fragment_sprites = new SpriteKey[5];
	private final SpriteKey[] iron_fragment_sprites = new SpriteKey[5];
	private final SpriteKey@NonNull [] @NonNull [] plant_sprites;
	private final @NonNull SpriteKey chicken;
	private final Audio @NonNull [] bird_idle_sound;
	private final @NonNull Audio bird_peck_sound;
	private final @NonNull Audio bird_death_sound;

	public LandscapeResources(@NonNull RenderQueues queues) {
		int num_progress = 13;
		ProgressForm.progress(10f/num_progress);

		SpriteFile fragment1 = new SpriteFile("/geometry/misc/rock_1.binsprite", Globals.NO_MIPMAP_CUTOFF, true, true, true, false);
		SpriteFile fragment2 = new SpriteFile("/geometry/misc/rock_2.binsprite", Globals.NO_MIPMAP_CUTOFF, true, true, true, false);
		SpriteFile fragment3 = new SpriteFile("/geometry/misc/rock_3.binsprite", Globals.NO_MIPMAP_CUTOFF, true, true, true, false);
		SpriteFile fragment4 = new SpriteFile("/geometry/misc/rock_4.binsprite", Globals.NO_MIPMAP_CUTOFF, true, true, true, false);
		SpriteFile fragment5 = new SpriteFile("/geometry/misc/rock_5.binsprite", Globals.NO_MIPMAP_CUTOFF, true, true, true, false);

		rock_fragment_sprites[0] = queues.register(fragment1);
		rock_fragment_sprites[1] = queues.register(fragment2);
		rock_fragment_sprites[2] = queues.register(fragment3);
		rock_fragment_sprites[3] = queues.register(fragment4);
		rock_fragment_sprites[4] = queues.register(fragment5);

		iron_fragment_sprites[0] = queues.register(fragment1, 1);
		iron_fragment_sprites[1] = queues.register(fragment2, 1);
		iron_fragment_sprites[2] = queues.register(fragment3, 1);
		iron_fragment_sprites[3] = queues.register(fragment4, 1);
		iron_fragment_sprites[4] = queues.register(fragment5, 1);
        ProgressForm.progress(1f/num_progress);

        SpriteFile native_plant1 = new SpriteFile("/geometry/misc/plant_1.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile native_plant2 = new SpriteFile("/geometry/misc/plant_2.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile native_plant3 = new SpriteFile("/geometry/misc/plant_3.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile native_plant4 = new SpriteFile("/geometry/misc/plant_4.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile viking_plant1 = new SpriteFile("/geometry/misc/viking_plant_1.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile viking_plant2 = new SpriteFile("/geometry/misc/viking_plant_2.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile viking_plant3 = new SpriteFile("/geometry/misc/viking_plant_3.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		SpriteFile viking_plant4 = new SpriteFile("/geometry/misc/viking_plant_4.binsprite", Globals.NO_MIPMAP_CUTOFF, true, false, true, true, true);
		ProgressForm.progress(1f/num_progress);

		plant_sprites = new SpriteKey[][]{
            {
			queues.register(native_plant1),
			queues.register(native_plant2),
			queues.register(native_plant3),
			queues.register(native_plant4)
            },
            {
			queues.register(viking_plant1),
			queues.register(viking_plant2),
			queues.register(viking_plant3),
			queues.register(viking_plant4)
            }
        };

		SpriteFile sprite_list_chicken = new SpriteFile("/geometry/misc/chicken.binsprite",
				Globals.NO_MIPMAP_CUTOFF,
				true, true, true, false);
		chicken = queues.register(sprite_list_chicken);

		bird_idle_sound = new Audio[4];
		bird_idle_sound[0] = Resources.findResource(new AudioFile("/sfx/chicken_idle1.ogg"));
		bird_idle_sound[1] = Resources.findResource(new AudioFile("/sfx/chicken_idle2.ogg"));
		bird_idle_sound[2] = Resources.findResource(new AudioFile("/sfx/chicken_idle3.ogg"));
		bird_idle_sound[3] = Resources.findResource(new AudioFile("/sfx/chicken_idle4.ogg"));
		bird_peck_sound = Resources.findResource(new AudioFile("/sfx/chicken_peck.ogg"));
		bird_death_sound = Resources.findResource(new AudioFile("/sfx/chicken_death.ogg"));
        ProgressForm.progress(1f/num_progress);
	}

	public static @NonNull Map<AbstractTreeGroup.@NonNull TreeType,@NonNull LowDetailModel> loadTreeLowDetails() {
		LowDetailModel jungle_lowdetail = Utils.loadObject(Utils.makeURL("/geometry/misc/tree_low.binlowdetail"));
		LowDetailModel palm_lowdetail = Utils.loadObject(Utils.makeURL("/geometry/misc/palm_low.binlowdetail"));
		LowDetailModel oak_lowdetail = Utils.loadObject(Utils.makeURL("/geometry/misc/oak_tree_low.binlowdetail"));
		LowDetailModel pine_lowdetail = Utils.loadObject(Utils.makeURL("/geometry/misc/pine_tree_low.binlowdetail"));

        var trees = new EnumMap<AbstractTreeGroup.TreeType,@NonNull LowDetailModel>(AbstractTreeGroup.TreeType.class);
        trees.put(AbstractTreeGroup.TreeType.JUNGLE, jungle_lowdetail);
        trees.put(AbstractTreeGroup.TreeType.PALM, palm_lowdetail);
        trees.put(AbstractTreeGroup.TreeType.OAK, oak_lowdetail);
        trees.put(AbstractTreeGroup.TreeType.PINE, pine_lowdetail);
        return Collections.unmodifiableMap(trees);
	}

	public SpriteKey @NonNull [] getRockFragments() {
		return rock_fragment_sprites;
	}

	public SpriteKey @NonNull [] getIronFragments() {
		return iron_fragment_sprites;
	}

	public SpriteKey[][] getPlants() {
		return plant_sprites;
	}

	public @NonNull SpriteKey getChicken() {
		return chicken;
	}

	public Audio getBirdIdleSound(@NonNull Random random) {
		return bird_idle_sound[random.nextInt(bird_idle_sound.length)];
	}

	public @NonNull Audio getBirdPeckSound() {
		return bird_peck_sound;
	}

	public @NonNull Audio getBirdDeathSound() {
		return bird_death_sound;
	}
}

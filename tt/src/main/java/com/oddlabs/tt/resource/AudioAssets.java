package com.oddlabs.tt.resource;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.model.SupplyType;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Constants for all the game audio assets and parameters */
public class AudioAssets {

    // Sound effects
    public static final AudioFile SFX_AMBIENT_BEACH = new AudioFile("/sfx/ambient_beach.ogg");
    public static final AudioFile SFX_AMBIENT_FOREST = new AudioFile("/sfx/ambient_forest.ogg");
    public static final AudioFile SFX_AMBIENT_WIND = new AudioFile("/sfx/ambient_wind.ogg");
    public static final AudioFile SFX_ARMORY = new AudioFile("/sfx/armory.ogg");
    public static final AudioFile SFX_ATTACKNOTIFY_NATIVE = new AudioFile("/sfx/attacknotify_native.ogg");
    public static final AudioFile SFX_ATTACKNOTIFY_VIKING = new AudioFile("/sfx/attacknotify_viking.ogg");
    public static final @NonNull AudioFile @NonNull [] SFX_AXE_CUTTING_STONES = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> String.format("/sfx/axe_cutting_stone%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final @NonNull AudioFile @NonNull [] SFX_AXE_CUTTING_WOODS = IntStream.rangeClosed(1, 6)
            .mapToObj(i -> String.format("/sfx/axe_cutting_wood%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_BUBBLING = new AudioFile("/sfx/bubbling.ogg");
    public static final AudioFile SFX_BUILDINGNOTIFY_NATIVE = new AudioFile("/sfx/buildingnotify_native.ogg");
    public static final AudioFile SFX_BUILDINGNOTIFY_VIKING = new AudioFile("/sfx/buildingnotify_viking.ogg");
    public static final AudioFile SFX_BUILDING_CRASH = new AudioFile("/sfx/building_crash.ogg");
    public static final AudioFile SFX_CHICKEN_DEATH = new AudioFile("/sfx/chicken_death.ogg");
    public static final @NonNull AudioFile[] SFX_CHICKEN_IDLES = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/sfx/chicken_idle%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_CHICKEN_PECK = new AudioFile("/sfx/chicken_peck.ogg");
    public static final AudioFile SFX_CRACKLING_CLOUD = new AudioFile("/sfx/crackling_cloud.ogg");
    public static final @NonNull AudioFile @NonNull [] SFX_DEATH_NATIVE_WARRIORS = IntStream.rangeClosed(1, 2)
            .mapToObj(i -> String.format("/sfx/death_native_warrior%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_DEATH_PEON = new AudioFile("/sfx/death_peon.ogg");
    public static final @NonNull AudioFile @NonNull [] SFX_DEATH_VIKING_WARRIORS = IntStream.rangeClosed(1, 2)
            .mapToObj(i -> String.format("/sfx/death_viking_warrior%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_FELLING_PALMTREE = new AudioFile("/sfx/felling_palmtree.ogg");
    public static final AudioFile SFX_FELLING_TREE = new AudioFile("/sfx/felling_tree.ogg");
    public static final AudioFile SFX_FLASH = new AudioFile("/sfx/flash.ogg");
    public static final AudioFile SFX_GAS = new AudioFile("/sfx/gas.ogg");
    public static final AudioFile SFX_RUMBLE = new AudioFile("/sfx/rumble.ogg");
    public static final AudioFile SFX_WEAPON_AXE = new AudioFile("/sfx/weapon_axe.ogg");
    public static final AudioFile SFX_WEAPON_SPEAR = new AudioFile("/sfx/weapon_spear.ogg");

    public static final @NonNull AudioFile @NonNull [] SFX_HITS = IntStream.rangeClosed(1, 7)
            .mapToObj(i -> String.format("/sfx/hit%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final @NonNull AudioFile @NonNull [] SFX_VIKING_CHIEFTAIN_HITS = new AudioFile[]{
            SFX_HITS[0], SFX_HITS[1], SFX_HITS[5], SFX_HITS[6]
    };
    public static final @NonNull AudioFile @NonNull [] SFX_NATIVE_CHIEFTAIN_HITS = new AudioFile[]{
            SFX_HITS[2], SFX_HITS[3], SFX_HITS[4], SFX_HITS[5]
    };
    public static final @NonNull AudioFile @NonNull [] SFX_IMPACT_MEATS = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> String.format("/sfx/impact_meat%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);

    public static final @NonNull AudioFile @NonNull [] SFX_IMPACT_WOODS = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/sfx/impact_wood%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);
    public static final AudioFile SFX_LURBLAST = new AudioFile("/sfx/lurblast.ogg");
    public static final @NonNull AudioFile[] SFX_LURBLASTS = IntStream.rangeClosed(1, 3)
            .mapToObj(i -> String.format("/sfx/lur_blast%d.ogg", i)).map(AudioFile::new).toArray(AudioFile[]::new);
    public static final @NonNull AudioFile @NonNull [] SFX_LUR_STUNS = IntStream.rangeClosed(1, 3)
            .mapToObj(i -> String.format("/sfx/lur_stun%d.ogg", i))
            .map(AudioFile::new).toArray(AudioFile[]::new);

    public static final @NonNull Map<@NonNull SupplyType, @NonNull AudioFile[]> SFX_HARVEST_SOUNDS;

    static {
        var map = new EnumMap<SupplyType, AudioFile[]>(SupplyType.class);
        map.put(SupplyType.WOOD, SFX_AXE_CUTTING_WOODS);
        map.put(SupplyType.ROCK, SFX_AXE_CUTTING_STONES);
        map.put(SupplyType.IRON, SFX_AXE_CUTTING_STONES);
        map.put(SupplyType.RUBBER, SFX_IMPACT_MEATS);
        SFX_HARVEST_SOUNDS = Collections.unmodifiableMap(map);
    }


    // Sound priority rankings
    public static final int AUDIO_RANK_AMBIENT = AudioParameters.RANK_AMBIENT;
    public static final int AUDIO_RANK_MUSIC = AudioParameters.RANK_MUSIC;
    public static final int AUDIO_RANK_NOTIFICATION = AudioParameters.RANK_NOTIFICATION;
    public static final int AUDIO_RANK_BUILDING_COLLAPSE = 20;
    public static final int AUDIO_RANK_DEATH = 10;
    public static final int AUDIO_RANK_MAGIC = 8;
    public static final int AUDIO_RANK_WEAPON_HIT = 7;
    public static final int AUDIO_RANK_WEAPON_ATTACK = 6;
    public static final int AUDIO_RANK_SUPPLY_ACTION = 5;
    public static final int AUDIO_RANK_GAS = 4;
    public static final int AUDIO_RANK_ARMORY = 3;
    public static final int AUDIO_RANK_HARVEST = 2;
    public static final int AUDIO_RANK_CHICKEN = 1;
    public static final int AUDIO_RANK_NOT_INITIALIZED = AudioParameters.RANK_NOT_INITIALIZED;

    // Sound distance parameters
    public static final float AUDIO_DISTANCE_MUSIC = AudioParameters.DISTANCE_AMBIENT;
    public static final float AUDIO_DISTANCE_AMBIENT = AudioParameters.DISTANCE_AMBIENT;
    public static final float AUDIO_DISTANCE_NOTIFICATION = AudioParameters.DISTANCE_AMBIENT;
    public static final float AUDIO_DISTANCE_BUILDING_COLLAPSE = 150f;
    public static final float AUDIO_DISTANCE_DEATH = 100f;
    public static final float AUDIO_DISTANCE_MAGIC = AudioParameters.DISTANCE_AMBIENT;
    public static final float AUDIO_DISTANCE_WEAPON_HIT = 75f;
    public static final float AUDIO_DISTANCE_WEAPON_ATTACK = 75f;
    public static final float AUDIO_DISTANCE_SUPPLY_ACTION = 80f;
    public static final float AUDIO_DISTANCE_ARMORY = 120f;
    public static final float AUDIO_DISTANCE_HARVEST = 40f;
    public static final float AUDIO_DISTANCE_CHICKEN = 25f;

    // Music
    public static final AudioParameters MUSIC_MENU = new AudioParameters(
            new AudioFile("/music/menu.ogg"), AUDIO_RANK_MUSIC,
            AUDIO_DISTANCE_MUSIC, 1.0f, 1f,
            1f, true, true, false);
    public static final AudioParameters MUSIC_NATIVE = new AudioParameters(
            new AudioFile("/music/native.ogg"), AUDIO_RANK_MUSIC,
            AUDIO_DISTANCE_MUSIC, 1.0f, 1f,
            1f, true, true, false);
    public static final AudioParameters MUSIC_VIKING = new AudioParameters(
            new AudioFile("/music/viking.ogg"), AUDIO_RANK_MUSIC,
            AUDIO_DISTANCE_MUSIC, 1.0f, 1f,
            1f, true, true, false);

    // Sound gain parameters
    public static final float AUDIO_GAIN_AMBIENT_FOREST = .01f;
    public static final float AUDIO_GAIN_AMBIENT_BEACH = .05f;
    public static final float AUDIO_GAIN_AMBIENT_WIND = .01f;
    public static final float AUDIO_GAIN_NOTIFICATION = 0.25f;
    public static final float AUDIO_GAIN_BUILDING_COLLAPSE = 1f;
    public static final float AUDIO_GAIN_WEAPON_HIT = .5f;
    public static final float AUDIO_GAIN_WEAPON_ATTACK = 1f;
    public static final float AUDIO_GAIN_HARVEST = 1f;
    public static final float AUDIO_GAIN_CHICKEN_IDLE = .25f;
    public static final float AUDIO_GAIN_CHICKEN_PECK = .25f;
    public static final float AUDIO_GAIN_CHICKEN_DEATH = .25f;
    public static final float AUDIO_GAIN_DEATH = 1f;
    public static final float AUDIO_GAIN_SUPPLY_ACTION = 1f;
    public static final float AUDIO_GAIN_LIGHTNING = 1f;
    public static final float AUDIO_GAIN_CLOUD = .4f;
    public static final float AUDIO_GAIN_BUBBLING = 1f;
    public static final float AUDIO_GAIN_GAS = .25f;
    public static final float AUDIO_GAIN_STUN_LUR = 1f;
    public static final float AUDIO_GAIN_BLAST_LUR = 1f;
    public static final float AUDIO_GAIN_BLAST_RUMBLE = 1f;
    public static final float AUDIO_GAIN_BLAST_BLAST = 1f;
    public static final float AUDIO_GAIN_ARMORY = 1f;

    // Sound radius parameters
    public static final float AUDIO_RADIUS_AMBIENT_FOREST = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_BEACH = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_WIND = 1f;
    public static final float AUDIO_RADIUS_NOTIFICATION = 1f;
    public static final float AUDIO_RADIUS_BUILDING_COLLAPSE = 5f;
    public static final float AUDIO_RADIUS_WEAPON_HIT = 1f;
    public static final float AUDIO_RADIUS_WEAPON_ATTACK = 1f;
    public static final float AUDIO_RADIUS_HARVEST = .5f;
    public static final float AUDIO_RADIUS_CHICKEN_IDLE = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_PECK = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_DEATH = .1f;
    public static final float AUDIO_RADIUS_DEATH = 1f;
    public static final float AUDIO_RADIUS_SUPPLY_ACTION = 2f;
    public static final float AUDIO_RADIUS_LIGHTNING = 5f;
    public static final float AUDIO_RADIUS_CLOUD = 5f;
    public static final float AUDIO_RADIUS_BUBBLING = 1f;
    public static final float AUDIO_RADIUS_GAS = .5f;
    public static final float AUDIO_RADIUS_STUN_LUR = 1f;
    public static final float AUDIO_RADIUS_BLAST_LUR = 1f;
    public static final float AUDIO_RADIUS_BLAST_RUMBLE = 1f;
    public static final float AUDIO_RADIUS_BLAST_BLAST = 1f;
    public static final float AUDIO_RADIUS_ARMORY = 5f;

    public static @NonNull AudioParameters getHarvestSound(@NonNull SupplyType key) {
        AudioFile[] sounds = SFX_HARVEST_SOUNDS.get(key);
        var audioFile = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        return new AudioParameters(audioFile, AUDIO_RANK_HARVEST,
                AUDIO_DISTANCE_HARVEST, AUDIO_GAIN_HARVEST, AUDIO_RADIUS_HARVEST);
    }

    public static final AudioParameters ERROR_SOUND = new AudioParameters(
            SFX_CHICKEN_PECK, AUDIO_RANK_NOTIFICATION,
            AUDIO_DISTANCE_NOTIFICATION, 0.5f, 1f, 0.5f, false, true);

    public static final AudioParameters BUILDING_COLLAPSE = new AudioParameters(
            SFX_BUILDING_CRASH, AUDIO_RANK_BUILDING_COLLAPSE,
            AUDIO_DISTANCE_BUILDING_COLLAPSE, AUDIO_GAIN_BUILDING_COLLAPSE,
            AUDIO_RADIUS_BUILDING_COLLAPSE);

    public static final @NonNull AudioParameters[] BUILDING_HITS = Arrays.stream(SFX_IMPACT_WOODS)
            .map(rsrc -> new AudioParameters(rsrc, AUDIO_RANK_WEAPON_HIT,
                    AUDIO_DISTANCE_WEAPON_HIT, AUDIO_GAIN_WEAPON_HIT,
                    AUDIO_RADIUS_WEAPON_HIT))
            .toArray(AudioParameters[]::new);

    public static final AudioParameters CHICKEN_PECK = new AudioParameters(
            SFX_CHICKEN_PECK, AUDIO_RANK_CHICKEN,
            AUDIO_DISTANCE_CHICKEN, AUDIO_GAIN_CHICKEN_PECK,
            AUDIO_RADIUS_CHICKEN_PECK);

    public static final AudioParameters CHICKEN_DEATH = new AudioParameters(
            SFX_CHICKEN_DEATH, AUDIO_RANK_DEATH,
            AUDIO_DISTANCE_DEATH, AUDIO_GAIN_CHICKEN_DEATH,
            AUDIO_RADIUS_CHICKEN_DEATH);

    public static final @NonNull AudioParameters[] CHICKEN_IDLES = Arrays.stream(SFX_CHICKEN_IDLES)
            .map(rsrc -> new AudioParameters(rsrc, AUDIO_RANK_CHICKEN,
                    AUDIO_DISTANCE_CHICKEN, AUDIO_GAIN_CHICKEN_IDLE,
                    AUDIO_RADIUS_CHICKEN_IDLE))
            .toArray(AudioParameters[]::new);

    public static final AudioParameters WEAPONS_PRODUCTION = new AudioParameters(
            SFX_ARMORY, AUDIO_RANK_ARMORY,
            AUDIO_DISTANCE_ARMORY, AUDIO_GAIN_ARMORY, AUDIO_RADIUS_ARMORY,
            1f, true, false);

    public static final AudioParameters BUBBLING = new AudioParameters(
            SFX_BUBBLING, AUDIO_RANK_MAGIC,
            AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_BUBBLING, AUDIO_RADIUS_BUBBLING,
            1f, true, false);

    public static final AudioParameters LIGHTNING_CLOUD = new AudioParameters(
            SFX_CRACKLING_CLOUD, AUDIO_RANK_MAGIC,
            AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_CLOUD, AUDIO_RADIUS_CLOUD,
            1f, true, false);

    public static final AudioParameters POISON_GAS = new AudioParameters(
            SFX_GAS, AUDIO_RANK_GAS,
            AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_GAS, AUDIO_RADIUS_GAS);

    public static final @NonNull AudioParameters[] SONIC_BLAST_LUR = Arrays.stream(SFX_LURBLASTS)
            .map(rsrc -> new AudioParameters(rsrc, AUDIO_RANK_MAGIC,
                    AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_BLAST_LUR,
                    AUDIO_RADIUS_BLAST_LUR))
            .toArray(AudioParameters[]::new);

    public static final AudioParameters SONIC_BLAST_RUMBLE = new AudioParameters(
            SFX_RUMBLE, AUDIO_RANK_MAGIC,
            AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_BLAST_RUMBLE,
            AUDIO_RADIUS_BLAST_RUMBLE);

    public static final AudioParameters SONIC_BLAST = new AudioParameters(
            SFX_LURBLAST, AUDIO_RANK_MAGIC,
            AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_BLAST_BLAST,
            AUDIO_RADIUS_BLAST_BLAST);

    public static final @NonNull AudioParameters[] STUN_LUR = Arrays.stream(SFX_LUR_STUNS)
            .map(audio -> new AudioParameters(audio, AUDIO_RANK_MAGIC,
                    AUDIO_DISTANCE_MAGIC, AUDIO_GAIN_STUN_LUR,
                    AUDIO_RADIUS_STUN_LUR))
            .toArray(AudioParameters[]::new);

    public static final AudioParameters AMBIENT_FOREST = new AudioParameters(
            SFX_AMBIENT_FOREST, AUDIO_RANK_AMBIENT,
            AUDIO_DISTANCE_AMBIENT, AUDIO_GAIN_AMBIENT_FOREST,
            AUDIO_RADIUS_AMBIENT_FOREST,
            1f, true, true, true);

    public static final AudioParameters AMBIENT_BEACH = new AudioParameters(
            SFX_AMBIENT_BEACH, AUDIO_RANK_AMBIENT,
            AUDIO_DISTANCE_AMBIENT, AUDIO_GAIN_AMBIENT_BEACH,
            AUDIO_RADIUS_AMBIENT_BEACH,
            1f, true, true, true);

    public static final AudioParameters AMBIENT_WIND = new AudioParameters(
            SFX_AMBIENT_WIND, AUDIO_RANK_AMBIENT,
            AUDIO_DISTANCE_AMBIENT, AUDIO_GAIN_AMBIENT_WIND,
            AUDIO_RADIUS_AMBIENT_WIND,
            1f, true, true, true);

    public static final @NonNull AudioParameters[] TREE_FALL = Stream.of(SFX_FELLING_TREE,
            SFX_FELLING_PALMTREE)
            .map(rsrc -> new AudioParameters(rsrc, AUDIO_RANK_SUPPLY_ACTION,
                    AUDIO_DISTANCE_SUPPLY_ACTION, AUDIO_GAIN_SUPPLY_ACTION,
                    AUDIO_RADIUS_SUPPLY_ACTION
            ))
            .toArray(AudioParameters[]::new);

    private AudioAssets() {
        // No instances
    }
}

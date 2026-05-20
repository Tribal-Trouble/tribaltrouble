package com.oddlabs.tt.audio;

import com.oddlabs.tt.audio.openal.OpenALAudioSource;
import com.oddlabs.tt.audio.openal.OpenALManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AudioPlayer extends AbstractAudioPlayer {
    public static final int AUDIO_RANK_AMBIENT = 75;
    public static final int AUDIO_RANK_MUSIC = 50;
    public static final int AUDIO_RANK_NOTIFICATION = 40;
    public static final int AUDIO_RANK_BUILDING_COLLAPSE = 20;
    public static final int AUDIO_RANK_DEATH = 10;
    public static final int AUDIO_RANK_MAGIC = 8;
    public static final int AUDIO_RANK_WEAPON_HIT = 7;
    public static final int AUDIO_RANK_WEAPON_ATTACK = 6;
    public static final int AUDIO_RANK_TREE_FALL = 5;
    public static final int AUDIO_RANK_GAS = 4;
    public static final int AUDIO_RANK_ARMORY = 3;
    public static final int AUDIO_RANK_HARVEST = 2;
    public static final int AUDIO_RANK_CHICKEN = 1;
    public static final int AUDIO_RANK_NOT_INITIALIZED = 0;

    public static final float AUDIO_DISTANCE_MUSIC = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_AMBIENT = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_NOTIFICATION = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_BUILDING_COLLAPSE = 150f;
    public static final float AUDIO_DISTANCE_DEATH = 150f;
    public static final float AUDIO_DISTANCE_MAGIC = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_WEAPON_HIT = 150f;
    public static final float AUDIO_DISTANCE_WEAPON_ATTACK = 150f;
    public static final float AUDIO_DISTANCE_TREE_FALL = 150f;
    public static final float AUDIO_DISTANCE_ARMORY = Float.MAX_VALUE;
    public static final float AUDIO_DISTANCE_HARVEST = 150f;
    public static final float AUDIO_DISTANCE_CHICKEN = 150f;

    public static final float AUDIO_GAIN_AMBIENT_FOREST = .01f;
    public static final float AUDIO_GAIN_AMBIENT_BEACH = .05f;
    public static final float AUDIO_GAIN_AMBIENT_WIND = .01f;
    public static final float AUDIO_GAIN_BUILDING_COLLAPSE = 1f;
    public static final float AUDIO_GAIN_WEAPON_HIT = .5f;
    public static final float AUDIO_GAIN_WEAPON_ATTACK = 1f;
    public static final float AUDIO_GAIN_HARVEST = 1f;
    public static final float AUDIO_GAIN_CHICKEN_IDLE = .25f;
    public static final float AUDIO_GAIN_CHICKEN_PECK = .25f;
    public static final float AUDIO_GAIN_CHICKEN_DEATH = .25f;
    public static final float AUDIO_GAIN_DEATH = 1f;
    public static final float AUDIO_GAIN_TREE_FALL = 1f;
    public static final float AUDIO_GAIN_LIGHTNING = 1f;
    public static final float AUDIO_GAIN_CLOUD = .4f;
    public static final float AUDIO_GAIN_BUBBLING = 1f;
    public static final float AUDIO_GAIN_GAS = .25f;
    public static final float AUDIO_GAIN_STUN_LUR = 1f;
    public static final float AUDIO_GAIN_BLAST_LUR = 1f;
    public static final float AUDIO_GAIN_BLAST_RUMBLE = 1f;
    public static final float AUDIO_GAIN_BLAST_BLAST = 1f;
    public static final float AUDIO_GAIN_ARMORY = 1f;

    public static final float AUDIO_RADIUS_AMBIENT_FOREST = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_BEACH = 1f;
    public static final float AUDIO_RADIUS_AMBIENT_WIND = 1f;
    public static final float AUDIO_RADIUS_BUILDING_COLLAPSE = 1f;
    public static final float AUDIO_RADIUS_WEAPON_HIT = .5f;
    public static final float AUDIO_RADIUS_WEAPON_ATTACK = .5f;
    public static final float AUDIO_RADIUS_HARVEST = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_IDLE = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_PECK = .1f;
    public static final float AUDIO_RADIUS_CHICKEN_DEATH = .1f;
    public static final float AUDIO_RADIUS_DEATH = .5f;
    public static final float AUDIO_RADIUS_TREE_FALL = .1f;
    public static final float AUDIO_RADIUS_LIGHTNING = 1f;
    public static final float AUDIO_RADIUS_CLOUD = 1f;
    public static final float AUDIO_RADIUS_BUBBLING = 1f;
    public static final float AUDIO_RADIUS_GAS = .2f;
    public static final float AUDIO_RADIUS_STUN_LUR = 1f;
    public static final float AUDIO_RADIUS_BLAST_LUR = 1f;
    public static final float AUDIO_RADIUS_BLAST_RUMBLE = 1f;
    public static final float AUDIO_RADIUS_BLAST_BLAST = 1f;
    public static final float AUDIO_RADIUS_ARMORY = .05f;

    private static final float MAX_HEARING_DIST = 150f;


    AudioPlayer(@Nullable AudioSource source, @NonNull AudioParameters<Audio> params) {
        super(source, params);
        if (this.source == null) {
            return;
        }
        source.setLooping(params.looping);
        source.setRelative(params.relative);

        setGain(params.gain);
        setPos(params.x, params.y, params.z);
        var state = source.getState();
        assert state == AudioSource.State.STOPPED || state == AudioSource.State.INITIAL;

        source.setRolloff(getRolloffFactor());
        source.setDistance(params.radius);
        source.setMinGain(0f);
        source.setMaxGain(1f);
        source.setPitch(params.pitch);

        if (!CLASSIC_AUDIO && source instanceof OpenALAudioSource alSource
                && AudioManager.getManager() instanceof OpenALManager alManager) {
            EFXManager efx = alManager.getEfxManager();
            if (efx.isSupported()) {
                boolean useReverb = params.rank != AUDIO_RANK_MUSIC && params.rank != AUDIO_RANK_NOTIFICATION;
                alSource.setAuxiliarySend(useReverb ? efx.getEffectSlot() : 0, 0);

                if (useReverb) {
                    float[] listenerPos = AudioManager.getManager().getListenerPosition();
                    float dx = params.x - listenerPos[0];
                    float dy = params.y - listenerPos[1];
                    float dz = params.z - listenerPos[2];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                    // Simple air absorption: brighter up close, muffled far away
                    // Clamp to [0.1, 1.0] to avoid total silence in HF
                    float gainHF = Math.clamp(1.0f - (dist / MAX_HEARING_DIST), 0.1f, 1.0f);

                    alSource.setDirectFilterGainHF(gainHF);
                } else {
                    alSource.setDirectFilterGainHF(1.0f); // Reset to full brightness
                }
            }
        }

        if (params.music || AudioManager.getManager().startPlaying()) {
            source.play();
        }
    }
}

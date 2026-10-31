package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;

import java.net.URL;
import java.nio.FloatBuffer;

/** Stands in when no audio device could be opened, so the game runs without sound instead of failing to start. */
final class SilentAudioManager extends AudioManager {
    private static final Audio NO_AUDIO = new Audio() {
    };

    SilentAudioManager() {
        super(new AudioSource[0]);
    }

    @Override
    public boolean hasDevice() {
        return false;
    }

    @Override
    public @NonNull AudioManager masterGain(float gain) {
        return this;
    }

    @Override
    public @NonNull AudioManager updateOrientation(@NonNull FloatBuffer fu) {
        return this;
    }

    @Override
    public @NonNull AudioManager updatePosition(float x, float y, float z) {
        return this;
    }

    @Override
    public float[] getListenerPosition() {
        return new float[3];
    }

    @Override
    public @NonNull Audio createAudio(@NonNull URL file) {
        return NO_AUDIO;
    }
}

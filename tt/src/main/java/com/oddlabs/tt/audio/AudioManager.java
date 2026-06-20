package com.oddlabs.tt.audio;

import com.oddlabs.tt.camera.CameraState;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URL;


/**
 * Manages audio playback, including positional audio sources, music, and ambient sounds.
 * Responsible for initializing the audio backend, allocating sources, and controlling global audio properties like
 * listener orientation and master gain.
 */
public interface AudioManager {

    @NonNull
    Vector3fc getListenerPosition();

    float getMasterGain();

    float getMusicGain();

    float getSfxGain();

    boolean isEFXSupported();

    boolean isHRTFSupported();

    boolean isSfxEnabled();

    @NonNull
    AudioManager setHeadphoneMode(boolean enabled);

    @NonNull
    AudioManager setListenerOrientation(@NonNull Vector3fc forward, @NonNull Vector3fc up);

    @NonNull
    AudioManager setListenerPosition(float x, float y, float z);

    @NonNull
    AudioManager setMasterGain(float gain);

    @NonNull
    AudioManager setMusicGain(float gain);

    @NonNull
    AudioManager setSfxGain(float gain);

    @NonNull
    AudioManager setSfxEnabled(boolean enabled);

    boolean startPlaying();

    @NonNull
    Audio createAudio(@NonNull URL file) throws IOException;

    @NonNull
    AudioPlayer newAudio(@NonNull CameraState camera_state, float x, float y, float z,
            @NonNull AudioParameters params);

    @NonNull
    AudioPlayer newAudio(float x, float y, float z, @NonNull AudioParameters params);

    /**
     * Sets the active environmental reverb preset.
     *
     * @param type The reverb type to apply.
     */
    default void setReverb(@NonNull ReverbType type) {
    }

    /**
     * Blends between two environmental reverb presets.
     *
     * @param from The source reverb environment.
     * @param to The target reverb environment.
     * @param factor Blending factor [0.0 - 1.0].
     */
    default void setReverb(@NonNull ReverbType from, @NonNull ReverbType to, float factor) {
    }
}

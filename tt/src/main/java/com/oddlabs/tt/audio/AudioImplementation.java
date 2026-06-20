package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;

/**
 * Interface for audio backend implementations to create audio players.
 */
@FunctionalInterface
public interface AudioImplementation {
    @NonNull
    AudioPlayer newAudio(float x, float y, float z, @NonNull AudioParameters params);
}

package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Manages the playback of a single audio instance associated with an {@link AudioSource}.
 */
public interface AudioPlayer {

    @NonNull
    AudioParameters getParameters();

    @Nullable
    AudioSource getSource();

    boolean isPlaying();

    void setGain(float gain);

    void setPosition(float x, float y, float z);

    @NonNull
    AudioPlayer stop();

    /**
     * Stops playback of this audio instance by fading the volume out exponentially.
     * <p>
     * The gain decays according to the formula: {@code G(t) = G(0) * e^(-decayRate * t)},
     * where {@code t} is the elapsed time in seconds. Perceptual silence is reached when
     * the gain drops below a low threshold, at which point the audio source is stopped.
     * <p>
     * For reference:
     * <ul>
     * <li>A decay rate of {@code 15.0} results in a very fast fade-out (~0.2 seconds).</li>
     * <li>A decay rate of {@code 1.2} results in a slow, gradual fade-out (~2.5 seconds).</li>
     * </ul>
     *
     * @param decayRate the rate of exponential decay (higher values result in a faster fade-out)
     * @return this player instance for chaining
     */
    @NonNull
    AudioPlayer stop(float decayRate);
}

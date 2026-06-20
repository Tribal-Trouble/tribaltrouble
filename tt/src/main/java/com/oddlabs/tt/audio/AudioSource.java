package com.oddlabs.tt.audio;

import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A point in 3D space that emits sound, abstracting the underlying audio implementation.
 */
public interface AudioSource {

    enum State {
        INITIAL,
        PLAYING,
        PAUSED,
        STOPPED
    }

    /**
     * {@return the current state of the source}
     */
    @NonNull
    State getState();

    /**
     * Sets the audio associated with this audio source.
     *
     * @param audio the audio to be played
     */
    void setAudio(@NonNull Audio audio);

    /**
     * Sets the pitch adjustment of the audio source.
     *
     * @param pitch The relative pitch. 1 is default.
     */
    void setPitch(float pitch);

    /**
     * Sets the gain (volume) of the audio source.
     *
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setGain(float gain);

    /**
     * Sets the minimum gain (volume) of the audio source.
     *
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setMinGain(float gain);

    /**
     * Sets the maximum gain (volume) of the audio source.
     *
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setMaxGain(float gain);

    /**
     * Sets the rolloff factor of the audio source.
     *
     * @param rolloff The rolloff value. 1 is the default.
     */
    void setRolloff(float rolloff);

    /**
     * Sets the reference distance of the audio source.
     *
     * @param distance the reference distance.
     */
    void setDistance(float distance);

    /**
     * {@return the current rolloff factor}
     */
    float getRolloff();

    /**
     * {@return the current reference distance (radius)}
     */
    float getDistance();

    /**
     * Sets the position of the audio source in 3D space.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     */
    void setPosition(float x, float y, float z);

    /**
     * Sets whether the audio source is relative.
     *
     * @param relative true if relative otherwise false
     */
    void setRelative(boolean relative);

    /**
     * Sets whether the audio source should loop.
     *
     * @param looping true if looping otherwise false
     */
    void setLooping(boolean looping);

    /**
     * Stops playback of the audio source.
     */
    void stop();

    /**
     * Pauses playback of the audio source.
     */
    void pause();

    /**
     * Starts or resumes playback of the audio source.
     */
    void play();

    /**
     * Associates an audio buffer with the source.
     *
     * @param bufferId The ID of the buffer to attach.
     */
    void setBuffer(int bufferId);

    /**
     * Rewinds the audio source to the beginning.
     */
    void rewind();

    /**
     * Retrieves the position of the audio source.
     *
     * @return The position (x, y, z).
     */
    @NonNull
    Vector3f getPosition();

    /**
     * Gets the priority rank of the audio source.
     *
     * @return The rank.
     */
    int getRank();

    /**
     * Gets the audio player currently associated with this source.
     *
     * @return The associated AbstractAudioPlayer.
     */
    @Nullable
    AudioPlayer getAudioPlayer();

    /**
     * Associates an audio player with this source.
     *
     * @param audioPlayer The audio player to associate.
     */
    void setAudioPlayer(@Nullable AudioPlayer audioPlayer);

    /**
     * Sets the auxiliary effect slot to send audio to (e.g., for reverb).
     *
     * @param slotId The effect slot ID.
     * @param filterId The filter ID to apply to the send, or 0 for none.
     */
    void setAuxiliarySend(int slotId, int filterId);

    /**
     * Sets the gain of the direct path high-frequency filter (air absorption).
     *
     * @param gainHF The gain value [0.0, 1.0].
     */
    void setDirectFilterGainHF(float gainHF);
}

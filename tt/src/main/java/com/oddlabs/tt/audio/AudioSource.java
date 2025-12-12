package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An audio source, which is a point in 3D space that emits sound.
 * This abstracts the underlying audio implementation (e.g., OpenAL).
 */
public interface AudioSource extends AutoCloseable {

    enum State {
        INITIAL,
        PLAYING,
        PAUSED,
        STOPPED
    }

    /** @return the current state of the source
     */
    @NonNull State getState();

    /** Sets the audio associated with this audio source.
     * @param audio the audio to be played
     */
    void setAudio(@NonNull Audio audio);

    /**
     * Sets the pitch adjustment of the audio source.
     * @param pitch The relative pitch. 1 is default.
     */
    void setPitch(float pitch);
    /**
     * Sets the gain (volume) of the audio source.
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setGain(float gain);

    /**
     * Sets the minimum gain (volume) of the audio source for relative sources.
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setMinGain(float gain);

    /**
     * Sets the maximum gain (volume) of the audio source for relative sources.
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setMaxGain(float gain);

    /**
     * Sets the rolloff factor of the audio source.for relative sources.
     * @param rolloff The rolloff value. 1 is the default.
     */
    void setRolloff(float rolloff);

    /**
     * Sets the minimum gain (volume) of the audio source for relative sources.
     * @param gain The gain value. 0 is mute, 1 is full volume.
     */
    void setDistance(float gain);

    /**
     * Sets the position of the audio source in 3D space.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     */
    void setPosition(float x, float y, float z);

    /**
     * Sets whether the audio source is relative.
     * @param relative true if relative otherwise false
     */
    void setRelative(boolean relative);

    /**
     * Sets whether the audio source should loop.
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
     * @param bufferId The ID of the buffer to attach.
     */
    void setBuffer(int bufferId);

    /**
     * Rewinds the audio source to the beginning.
     */
    void rewind();

    /**
     * Gets the current state of the audio source (e.g., playing, stopped, paused).
     * @return The state of the source, as defined by the underlying audio library's constants.
     */
    int getSourceState();

    /**
     * Retrieves the position of the audio source.
     * @return A float array containing the position (x, y, z).
     */
    float @NonNull [] getPosition();

    /**
     * Gets the priority rank of the audio source.
     * @return The rank.
     */
    int getRank();

    /**
     * Gets the audio player currently associated with this source.
     * @return The associated AbstractAudioPlayer.
     */
    @Nullable AbstractAudioPlayer getAudioPlayer();

    /**
     * Associates an audio player with this source.
     * @param audioPlayer The audio player to associate.
     */
    void setAudioPlayer(AbstractAudioPlayer audioPlayer);

    /**
     * Closes the audio source and releases its native resources.
     */
    @Override
    void close();
}

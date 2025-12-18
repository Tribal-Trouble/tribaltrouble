package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;

public final class OpenALAudioSource extends NativeResource<OpenALAudioSource.Source> implements AudioSource {
    private static final Logger logger = Logger.getLogger(OpenALAudioSource.class.getSimpleName());
    static final class Source extends NativeResource.NativeState {

        final int sourceId;

        Source() {
            sourceId = AL10.alGenSources();
            checkALError("alGenSources");
        }

        @Override
        public int hashCode() {
            return sourceId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Source source && sourceId == source.sourceId;
        }

        @Override
        public void close() {
            if (ALC10.alcGetCurrentContext() != 0) {
                // Check if the source is valid before trying to stop it
                if (AL10.alIsSource(sourceId)) {
                    // Stop the source before deleting it, to be safe
                    AL10.alSourceStop(sourceId);
                    checkALError("alSourceStop before deleting source");

                    AL10.alDeleteSources(sourceId);
                    checkALError("alDeleteSources");
                } else {
                    logger.warning("Attempted to close invalid source");
                }
            }
        }
    }

    private @Nullable AbstractAudioPlayer audio_player;
    private final FloatBuffer positionBuffer = org.lwjgl.BufferUtils.createFloatBuffer(3);

    public OpenALAudioSource() {
        super(new Source());
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OpenALAudioSource source && state.equals(source.state);
    }

    @Override
    public @NonNull State getState() {
        int state = AL10.alGetSourcei(getSource(), AL10.AL_SOURCE_STATE);
        checkALError("alGetSourcei(AL_SOURCE_STATE)");

        return switch (state) {
            case AL10.AL_INITIAL -> State.INITIAL;
            case AL10.AL_PLAYING -> State.PLAYING;
            case AL10.AL_PAUSED -> State.PAUSED;
            case AL10.AL_STOPPED -> State.STOPPED;
            default -> throw new IllegalStateException("Unknown state: " + state);
        };
    }

    @Override
    public void setAudio(@NonNull Audio audio) {
        if (audio instanceof OpenALAudio alAudio) {
            setAudio(alAudio);
        } else {
            throw new IllegalArgumentException("Unsupported audio type: " + audio.getClass().getName());
        }
    }

    public void setAudio(@NonNull OpenALAudio audio) {
        int buffer = audio.getBuffer();
        assert buffer != AL10.AL_NONE;
        AL10.alSourcei(getSource(), AL10.AL_BUFFER, audio.getBuffer());
        checkALError("alSourcei AL_BUFFER");
    }

    public void queue(@NonNull IntBuffer al_buffers) {
        AL10.alSourceQueueBuffers(getSource(), al_buffers);
        checkALError("alSourceQueueBuffers");
    }

    public int processed() {
        int processed = AL10.alGetSourcei(getSource(), AL10.AL_BUFFERS_PROCESSED);
        checkALError("alGetSourcei AL_BUFFERS_PROCESSED");
        return processed;
    }

    public void unqueued(@NonNull IntBuffer al_buffers) {
        AL10.alSourceUnqueueBuffers(getSource(), al_buffers);
    }

    @Override
    public void setPitch(float pitch) {
        AL10.alSourcef(getSource(), AL10.AL_PITCH, pitch);
        checkALError("alSourcef AL_PITCH");
    }

    @Override
    public void setGain(float gain) {
        AL10.alSourcef(getSource(), AL10.AL_GAIN, gain);
        checkALError("alSourcef AL_GAIN");
    }

    @Override
    public void setMinGain(float gain) {
        AL10.alSourcef(getSource(), AL10.AL_MIN_GAIN, gain);
        checkALError("alSourcef AL_MIN_GAIN");
    }

    @Override
    public void setMaxGain(float gain) {
        AL10.alSourcef(getSource(), AL10.AL_MAX_GAIN, gain);
        checkALError("alSourcef AL_MAX_GAIN");
    }

    @Override
    public void setRolloff(float rolloff) {
        AL10.alSourcef(getSource(), AL10.AL_ROLLOFF_FACTOR, rolloff);
        checkALError("alSourcef AL_ROLLOFF_FACTOR");
    }

    @Override
    public void setDistance(float distance) {
        AL10.alSourcef(getSource(), AL10.AL_REFERENCE_DISTANCE, distance);
        checkALError("alSourcef AL_REFERENCE_DISTANCE");
    }

    @Override
    public void setPosition(float x, float y, float z) {
        AL10.alSource3f(getSource(), AL10.AL_POSITION, x, y, z);
        checkALError("alSource3f AL_POSITION");
    }

    @Override
    public void setRelative(boolean relative) {
        AL10.alSourcei(getSource(), AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    @Override
    public void setLooping(boolean looping) {
        AL10.alSourcei(getSource(), AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    @Override
    public void stop() {
        AL10.alSourceStop(getSource());
        checkALError("alSourceStop");
    }

    @Override
    public void pause() {
        AL10.alSourcePause(getSource());
        checkALError("alSourcePause");
    }

    @Override
    public void play() {
        AL10.alSourcePlay(getSource());
        checkALError("alSourcePlay");
    }

    @Override
    public void setBuffer(int bufferId) {
        AL10.alSourcei(getSource(), AL10.AL_BUFFER, bufferId);
        checkALError("alSourcei AL_BUFFER");
    }

    @Override
    public void rewind() {
        AL10.alSourceRewind(getSource());
        checkALError("alSourceRewind");
    }

    @Override
    public int getSourceState() {
        int state = AL10.alGetSourcei(getSource(), AL10.AL_SOURCE_STATE);
        checkALError("alGetSourcei AL_SOURCE_STATE");
        return state;
    }

    @Override
    public float @NonNull [] getPosition() {
        AL10.alGetSourcefv(getSource(), AL10.AL_POSITION, positionBuffer);
        checkALError("alGetSource AL_POSITION");
        return new float[]{positionBuffer.get(0), positionBuffer.get(1), positionBuffer.get(2)};
    }

    public int getSource() {
        return state.sourceId;
    }

    @Override
    public int getRank() {
        return audio_player != null ? audio_player.getParameters().rank : AudioPlayer.AUDIO_RANK_NOT_INITIALIZED;
    }

    @Override
    public @Nullable AbstractAudioPlayer getAudioPlayer() {
        return audio_player;
    }

    @Override
    public void setAudioPlayer(@Nullable AbstractAudioPlayer audio_player) {
        if (this.audio_player != null)
            this.audio_player.stop();
        this.audio_player = audio_player;
    }
}

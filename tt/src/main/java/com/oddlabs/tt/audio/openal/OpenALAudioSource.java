package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;

public final class OpenALAudioSource extends NativeResource<OpenALAudioSource.Source> implements AudioSource {
    static final class Source extends NativeResource.NativeState {
        private final IntBuffer source;

        Source() {
            source = ALBufferUtils.createIntBuffer(1);
        }

        @Override
        public void close() {
            if (source != null && AL.isCreated()) {
                int sourceId = source.get(0);
                // Check if the source is valid before trying to stop it
                if (AL10.alIsSource(sourceId)) {
                    // Stop the source before deleting it, to be safe
                    AL10.alSourceStop(sourceId);
                    checkALError("alSourceStop before deleting source");
                }

                AL10.alDeleteSources(source);
                checkALError("alDeleteSources");
            }
        }
    }

    private @Nullable AbstractAudioPlayer audio_player;
    private final FloatBuffer positionBuffer = ALBufferUtils.createFloatBuffer(3);

    public OpenALAudioSource() {
        super(new Source());
        AL10.alGenSources(state.source);
        checkALError("alGenSources");
    }

    @Override
    public @NonNull State getState() {
        int state = AL10.alGetSourcei(getSource(), AL10.AL_SOURCE_STATE);

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
        AL10.alGetSource(getSource(), AL10.AL_POSITION, positionBuffer);
        checkALError("alGetSource AL_POSITION");
        return new float[]{positionBuffer.get(0), positionBuffer.get(1), positionBuffer.get(2)};
    }

    public int getSource() {
        return state.source.get(0);
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

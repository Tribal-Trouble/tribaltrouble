package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.resource.NativeResource;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;
import static org.lwjgl.openal.EXTEfx.AL_AUXILIARY_SEND_FILTER;

/**
 * OpenAL implementation of {@link AudioSource} managing a native OpenAL source.
 */
final class OpenALAudioSource extends NativeResource<OpenALAudioSource.Source> implements AudioSource {
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
        public boolean equals(@Nullable Object obj) {
            return obj instanceof Source source && sourceId == source.sourceId;
        }

        @Override
        public void close() {
            if (ALC10.alcGetCurrentContext() != 0) {
                AL10.alGetError(); // Clear any sticky error from previous operations
                // Check if the source is valid before trying to stop it
                if (AL10.alIsSource(sourceId)) {
                    // Stop the source before deleting it, to be safe
                    AL10.alSourceStop(sourceId);
                    checkALError("alSourceStop before deleting source");

                    // Explicitly detach any buffers (static or queued) from the source.
                    // This is required before deleting the buffers.
                    AL10.alSourcei(sourceId, AL10.AL_BUFFER, AL10.AL_NONE);
                    checkALError("alSourcei AL_BUFFER AL_NONE before deleting source");

                    // Reset any auxiliary sends to free up effect slots
                    AL11.alSource3i(sourceId, AL_AUXILIARY_SEND_FILTER, 0, 0, 0);
                    checkALError("alSource3i AL_AUXILIARY_SEND_FILTER AL_NONE before deleting source");

                    // Detach the direct filter to free up the filter object
                    AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
                    checkALError("alSourcei AL_DIRECT_FILTER AL_FILTER_NULL before deleting source");

                    AL10.alDeleteSources(sourceId);
                    checkALError("alDeleteSources");
                } else {
                    logger.warning("Attempted to close invalid source");
                }
            }
        }
    }

    private final @NonNull OpenALManager manager;
    private @Nullable AudioPlayer audio_player;
    private @Nullable OpenALFilter directFilter;
    private float rolloff;
    private float reference_distance;

    OpenALAudioSource(@NonNull OpenALManager manager) {
        super(new Source(), manager::enqueueCleanup);
        this.manager = manager;
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof OpenALAudioSource source && state.equals(source.state);
    }

    @Override
    public void close() {
        try {
            stop();
            if (directFilter != null) {
                if (ALC10.alcGetCurrentContext() != 0) {
                    int sourceId = getSource();
                    if (AL10.alIsSource(sourceId)) {
                        AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
                        checkALError("alSourcei AL_DIRECT_FILTER AL_FILTER_NULL");
                    }
                }
            }
            super.close();
        } finally {
            if (directFilter != null) {
                directFilter.close();
                directFilter = null;
            }
        }
    }

    @Override
    public float getRolloff() {
        return rolloff;
    }

    @Override
    public float getDistance() {
        return reference_distance;
    }

    @Override
    public void setDirectFilterGainHF(float gainHF) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        try {
            if (directFilter == null) {
                directFilter = new OpenALFilter(manager::enqueueCleanup);
            }
            directFilter.setLowPassGainHF(gainHF);
            int sourceId = getSource();
            if (AL10.alIsSource(sourceId)) {
                AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, directFilter.getFilterId());
                checkALError("alSourcei AL_DIRECT_FILTER");
            }
        } catch (Exception e) {
            // EFX not supported or an error creating the filter, ignore
        }
    }

    @Override
    public @NonNull State getState() {
        if (ALC10.alcGetCurrentContext() == 0) return State.STOPPED;
        return switch (getSourceState()) {
            case AL10.AL_INITIAL -> State.INITIAL;
            case AL10.AL_PLAYING -> State.PLAYING;
            case AL10.AL_PAUSED -> State.PAUSED;
            case AL10.AL_STOPPED -> State.STOPPED;
            default -> throw new IllegalStateException("Unknown state");
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

    void setAudio(@NonNull OpenALAudio audio) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int buffer = audio.getBuffer();
        assert buffer != AL10.AL_NONE;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcei(sourceId, AL10.AL_BUFFER, audio.getBuffer());
            checkALError("alSourcei AL_BUFFER");
        }
    }

    void queue(@NonNull IntBuffer al_buffers) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            assert al_buffers.remaining() > 0 : "al_buffers is empty";
            AL10.alSourceQueueBuffers(sourceId, al_buffers);
            checkALError("alSourceQueueBuffers");
        }
    }

    int processed() {
        if (ALC10.alcGetCurrentContext() == 0) return 0;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            int processed = AL10.alGetSourcei(sourceId, AL10.AL_BUFFERS_PROCESSED);
            checkALError("alGetSourcei AL_BUFFERS_PROCESSED");
            return processed;
        }
        return 0;
    }

    void unqueued(@NonNull IntBuffer al_buffers) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourceUnqueueBuffers(sourceId, al_buffers);
        }
    }

    @Override
    public void setPitch(float pitch) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcef(sourceId, AL10.AL_PITCH, pitch);
            checkALError("alSourcef AL_PITCH");
        }
    }

    @Override
    public void setGain(float gain) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcef(sourceId, AL10.AL_GAIN, gain);
            checkALError("alSourcef AL_GAIN");
        }
    }

    @Override
    public void setMinGain(float gain) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcef(sourceId, AL10.AL_MIN_GAIN, gain);
            checkALError("alSourcef AL_MIN_GAIN");
        }
    }

    @Override
    public void setMaxGain(float gain) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcef(sourceId, AL10.AL_MAX_GAIN, gain);
            checkALError("alSourcef AL_MAX_GAIN");
        }
    }

    @Override
    public void setRolloff(float rolloff) {
        this.rolloff = rolloff;
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, rolloff);
            checkALError("alSourcef AL_ROLLOFF_FACTOR");
        }
    }

    @Override
    public void setDistance(float distance) {
        this.reference_distance = distance;
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, distance);
            checkALError("alSourcef AL_REFERENCE_DISTANCE");
        }
    }

    @Override
    public void setPosition(float x, float y, float z) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSource3f(sourceId, AL10.AL_POSITION, x, y, z);
            checkALError("alSource3f AL_POSITION");
        }
    }

    @Override
    public void setRelative(boolean relative) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
    }

    @Override
    public void setLooping(boolean looping) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
    }

    @Override
    public void stop() {
        if (ALC10.alcGetCurrentContext() != 0) {
            AL10.alGetError(); // clear any previous error
            int sourceId = getSource();
            if (AL10.alIsSource(sourceId)) {
                AL10.alSourceStop(sourceId);
                checkALError("alSourceStop");

                // Detach any buffers (static or queued) from the source.
                // This is required before deleting the buffers.
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, AL10.AL_NONE);
                checkALError("alSourcei AL_BUFFER AL_NONE");

                AL10.alSourceRewind(sourceId);
                checkALError("alSourceRewind");
            }
        }
    }

    @Override
    public void pause() {
        if (ALC10.alcGetCurrentContext() != 0) {
            int sourceId = getSource();
            if (AL10.alIsSource(sourceId)) {
                AL10.alSourcePause(sourceId);
                checkALError("alSourcePause");
            }
        }
    }

    @Override
    public void play() {
        if (ALC10.alcGetCurrentContext() != 0) {
            int sourceId = getSource();
            if (AL10.alIsSource(sourceId)) {
                // Only play if not already playing to avoid OpenAL source stealing/restarting
                if (getState() != State.PLAYING) {
                    AL10.alSourcePlay(sourceId);
                    checkALError("alSourcePlay");
                }
            }
        }
    }

    @Override
    public void setBuffer(int bufferId) {
        if (ALC10.alcGetCurrentContext() != 0) {
            int sourceId = getSource();
            if (AL10.alIsSource(sourceId)) {
                AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
                checkALError("alSourcei AL_BUFFER");
            }
        }
    }

    @Override
    public void rewind() {
        if (ALC10.alcGetCurrentContext() != 0) {
            int sourceId = getSource();
            if (AL10.alIsSource(sourceId)) {
                AL10.alSourceRewind(sourceId);
                checkALError("alSourceRewind");
            }
        }
    }

    int getSourceState() {
        if (ALC10.alcGetCurrentContext() == 0) return AL10.AL_STOPPED;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
            checkALError("alGetSourcei AL_SOURCE_STATE");
            return state;
        }
        return AL10.AL_STOPPED;
    }

    @Override
    public @NonNull Vector3f getPosition() {
        if (ALC10.alcGetCurrentContext() == 0) return new Vector3f();
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            try (var stack = MemoryStack.stackPush()) {
                FloatBuffer positionBuffer = stack.mallocFloat(3);
                AL10.alGetSourcefv(sourceId, AL10.AL_POSITION, positionBuffer);
                checkALError("alGetSource AL_POSITION");
                return new Vector3f(positionBuffer);
            }
        }
        return new Vector3f();
    }

    int getSource() {
        return state.sourceId;
    }

    @Override
    public int getRank() {
        return audio_player != null ? audio_player.getParameters().rank() : AudioParameters.RANK_NOT_INITIALIZED;
    }

    @Override
    public @Nullable AudioPlayer getAudioPlayer() {
        return audio_player;
    }

    @Override
    public void setAudioPlayer(@Nullable AudioPlayer audio_player) {
        if (this.audio_player != null)
            this.audio_player.stop();
        this.audio_player = audio_player;
    }

    @Override
    public void setAuxiliarySend(int slotId, int filterId) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        int sourceId = getSource();
        if (AL10.alIsSource(sourceId)) {
            AL11.alSource3i(sourceId, AL_AUXILIARY_SEND_FILTER, slotId, 0, filterId);
            checkALError("alSource3i AL_AUXILIARY_SEND_FILTER");
        }
    }
}

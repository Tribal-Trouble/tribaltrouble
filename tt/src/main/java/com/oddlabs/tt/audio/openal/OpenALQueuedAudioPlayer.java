package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.audio.OGGStream;
import com.oddlabs.tt.audio.QueuedAudioPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * OpenAL implementation of {@link QueuedAudioPlayer} using a multi-buffer queue for streaming.
 */
final class OpenALQueuedAudioPlayer extends QueuedAudioPlayer<OpenALManager, OpenALAudioSource> {
    private static final Logger logger = Logger.getLogger(OpenALQueuedAudioPlayer.class.getSimpleName());
    private static final int NUM_BUFFERS = 12;

    private volatile int al_format;
    private volatile int al_rate;

    OpenALQueuedAudioPlayer(@NonNull OpenALManager manager, @Nullable OpenALAudioSource source, float x, float y,
            float z,
            @NonNull AudioParameters params) {
        super(manager, source, x, y, z, params);
    }

    @Override
    protected void initThread() {
        synchronized (manager) {
            ALC10.alcMakeContextCurrent(manager.getContext());
            ALCCapabilities alcCapabilities = ALC.createCapabilities(manager.getDevice());
            AL.createCapabilities(alcCapabilities);
        }
    }

    @Override
    public @NonNull OpenALQueuedAudioPlayer stop() {
        super.stop();
        synchronized (this) {
            this.notifyAll(); // Wake up the filler thread.
            // The filler thread will see playing == false and exit, running its finally block to cleanup.
        }

        return this;
    }

    @Override
    protected @Nullable OpenALAudio initAsync(@NonNull OGGStream stream) {
        if (this.source == null || !isPlaying()) {
            return null;
        }

        int format = Wave.getFormat(stream.getChannels(), Short.SIZE);
        int rate = stream.getRate();

        long bufferduration = (long) NUM_BUFFERS * (PCM_SAMPLES / stream.getChannels()) / (rate
                / TimeUnit.SECONDS
                        .toMillis(1));
        logger.info("Creating OpenAL audio with " + NUM_BUFFERS + " buffers for " + Duration.ofMillis(
                bufferduration));

        OpenALAudio audio;
        synchronized (manager) {
            if (ALC10.alcGetCurrentContext() == 0) return null;
            audio = new OpenALAudio(manager, NUM_BUFFERS);
            this.al_format = format;
            this.al_rate = rate;
        }

        IntBuffer alBuffers = audio.getBuffers();
        int buffersLoaded = 0;
        for (int i = 0; i < NUM_BUFFERS; i++) {
            int alBufferId = alBuffers.get(i);
            int shortsRead = readPCM(stream, pcmBuffer, getParameters().looping());
            if (shortsRead > 0) {
                synchronized (manager) {
                    if (ALC10.alcGetCurrentContext() != 0) {
                        AL10.alBufferData(alBufferId, al_format, pcmBuffer, al_rate);
                    }
                }
                buffersLoaded++;
            } else {
                break;
            }
        }

        synchronized (manager) {
            if (ALC10.alcGetCurrentContext() != 0 && isPlaying() && buffersLoaded > 0) {
                alBuffers.limit(buffersLoaded);
                ((OpenALAudioSource) source).queue(alBuffers);
            }
        }

        return audio;
    }

    @Override
    protected int getBufferCount() {
        Audio audio = this.audio;
        assert audio != null : "Audio not initialized";
        return ((OpenALAudio) audio).getBufferCount();
    }

    @Override
    public void refill(@NonNull OGGStream stream) {
        if (source == null || !isPlaying()) return;

        int processed;
        synchronized (manager) {
            if (ALC10.alcGetCurrentContext() == 0) return;
            processed = ((OpenALAudioSource) source).processed();
        }

        try (var stack = MemoryStack.stackPush()) {
            var al_return_buffers = stack.mallocInt(1);
            while (processed > 0 && isPlaying()) {
                int shortsRead = readPCM(stream, pcmBuffer, getParameters().looping());
                if (shortsRead == 0) {
                    synchronized (manager) {
                        stop();
                    }
                    return;
                }

                synchronized (manager) {
                    if (ALC10.alcGetCurrentContext() == 0) return;
                    ((OpenALAudioSource) source).unqueued(al_return_buffers);
                    int alBufferId = al_return_buffers.get(0);
                    AL10.alBufferData(alBufferId, al_format, pcmBuffer, al_rate);
                    if (isPlaying()) {
                        ((OpenALAudioSource) source).queue(al_return_buffers);
                    }
                }
                processed--;
            }
        }

        synchronized (manager) {
            if (ALC10.alcGetCurrentContext() == 0) return;
            if (isPlaying() && source.getState() != AudioSource.State.PLAYING) {
                source.play();
            }
        }
    }

    @Override
    protected void cleanupAsync() throws Exception {
        synchronized (manager) {
            if (manager.isClosed()) {
                return;
            }
            if (audio instanceof AutoCloseable toClose) {
                toClose.close();
            }
        }
    }
}

package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An audio player that streams audio data from an OGG stream into multiple queued buffers.
 */
public abstract class QueuedAudioPlayer<AM extends AbstractAudioManager<AM, AS>, AS extends AudioSource>
        extends AbstractAudioPlayer<AM, AS> {
    private static final Logger logger = Logger.getLogger(QueuedAudioPlayer.class.getSimpleName());
    protected static final int PCM_SAMPLES = 16384;

    protected final ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(PCM_SAMPLES);

    /**
     * The audio data that is currently being played.
     * We can't use audioParams.sound() because we handle buffering ourselves (for now).
     */
    protected volatile @Nullable Audio audio;

    protected QueuedAudioPlayer(@NonNull AM manager, @Nullable AS source, float x, float y, float z,
            @NonNull AudioParameters params) {
        super(manager, source, x, y, z, params);
        if (!isPlaying() || this.source == null) {
            return;
        }

        // Queued audio does not loop via source setting, it loops internally during buffer refill
        source.setLooping(false);

        Thread.startVirtualThread(() -> refiller(params.audio().getURL()));
    }

    /** {@return The audio associated with this player.} */
    @Override
    protected @NonNull Audio getAudio() {
        Audio audio = this.audio;
        if (null == audio) {
            throw new IllegalStateException("Audio not initialized");
        }
        return audio;
    }

    private void refiller(@NonNull URL source) {
        initThread();
        try (OGGStream stream = new OGGStream(source)) {
            this.audio = initAsync(stream);
            if (this.audio == null) {
                return;
            }

            if (manager.startPlaying()) {
                this.source.play();
            }

            int channels = stream.getChannels();
            int rate = stream.getRate();

            // Calculate the sleep interval based on total queued time across all buffers.
            // We wait for approximately half of the total buffers to be empty before waking up.
            long totalSamplesPerChannel = (long) PCM_SAMPLES * getBufferCount() / channels;
            long sleepInterval = Math.max(10, (TimeUnit.SECONDS.toMillis(1) * totalSamplesPerChannel / rate) / 2);

            synchronized (this) {
                while (isPlaying()) {
                    try {
                        long start = System.currentTimeMillis();
                        refill(stream);
                        var sleep = sleepInterval - (System.currentTimeMillis() - start);
                        if (sleep > 0) {
                            QueuedAudioPlayer.this.wait(sleep);
                        }
                    } catch (InterruptedException | IOException e) {
                        break;
                    }
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Failed to read OGG stream " + source, ioe);
        } catch (Exception _) {
            // Failed to load, init, or read. Exit silently.
        } finally {
            try {
                cleanupAsync();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failure during cleanup", e);
            }
        }
    }

    /** Run by the Refiller thread */
    protected abstract @Nullable Audio initAsync(@NonNull OGGStream stream) throws Exception;

    /** Run by the Refiller thread to initialize thread-local context/capabilities. */
    protected void initThread() {
    }

    /** Run by the Refiller thread */
    protected abstract void refill(@NonNull OGGStream stream) throws IOException;

    /** Run by the Refiller thread */
    protected abstract void cleanupAsync() throws Exception;

    protected static int readPCM(@NonNull OGGStream stream, @NonNull ShortBuffer pcmBuffer, boolean looping) {
        pcmBuffer.clear(); // Position 0, Limit PCM_SAMPLES

        int shortsRead = stream.read(pcmBuffer);

        if (shortsRead <= 0 && looping) {
            // End of ogg stream reached, but we are looping.
            stream.seek(0);
            shortsRead = stream.read(pcmBuffer);
        }

        // Explicitly set the buffer's position and limit for OpenAL.
        // Some native wrappers might not update the buffer position automatically.
        pcmBuffer.position(0);
        pcmBuffer.limit(shortsRead);

        return shortsRead;
    }

    @Override
    public @NonNull QueuedAudioPlayer<AM, AS> stop() {
        if (manager.removeQueuedPlayer(this)) {
            super.stop(); // Sets playing = false and stops the source.
        }

        return this;
    }
}

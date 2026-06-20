package com.oddlabs.tt.resource;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;

/**
 * A resource handle for an audio file.
 */
public final class AudioFile extends File<Audio> {
    private final boolean streaming;

    private @Nullable SoftReference<Audio> audio;

    public AudioFile(@NonNull String location) {
        this(location, location.contains("/music/"));
    }

    public AudioFile(@NonNull String location, boolean streaming) {
        super(location);
        this.streaming = streaming;
    }

    @Override
    public synchronized @NonNull Audio get() throws UncheckedIOException {
        Audio audio = null == this.audio ? null : this.audio.get();
        if (null == audio) {
            try {
                audio = Renderer.getRenderer().getAudioManager().createAudio(getURL());
                this.audio = new SoftReference<>(audio);
            } catch (IOException ex) {
                throw new UncheckedIOException("Could not load " + this.getURL(), ex);
            }
        }

        return audio;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof AudioFile audioFile && super.equals(o) && audioFile.streaming == streaming;
    }

    public boolean isStreaming() {
        return streaming;
    }
}

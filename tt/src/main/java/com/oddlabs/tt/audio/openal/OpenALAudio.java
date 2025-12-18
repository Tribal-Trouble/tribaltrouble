package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.OGGStream;
import com.oddlabs.tt.audio.Wave;
import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.util.ByteBufferOutputStream;
import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;

/**
 * OpenAL buffered audio
 */
public final class OpenALAudio extends NativeResource<OpenALAudio.Buffers> implements Audio {
    static final class Buffers extends NativeState {
        private final @NonNull IntBuffer al_buffers;

        Buffers(int num_buffers) {
            al_buffers = org.lwjgl.BufferUtils.createIntBuffer(num_buffers);
            AL10.alGenBuffers(al_buffers);
            checkALError("alGenBuffers " + num_buffers);
        }

        @Override
        public void close() {
            AL10.alDeleteBuffers(al_buffers);
            checkALError("alDeleteBuffers");
            al_buffers.clear();
        }
    }

    public OpenALAudio(int num_buffers) {
        super(new Buffers(num_buffers));
    }

    OpenALAudio(@NonNull URL file) throws IOException {
		this(1);
		Wave wave;
		try {
			wave = new Wave(file);
		} catch (UnsupportedAudioFileException | IOException _) {
			// Assume it's an ogg vorbis file
			wave = loadOGG(file);
		}
		AL10.alBufferData(getBuffer(), wave.getFormat(), wave.getData(), wave.getSampleRate());
	}

	private static @NonNull Wave loadOGG(@NonNull URL file) throws IOException {
		ByteBufferOutputStream output = new ByteBufferOutputStream(true);
		OGGStream ogg_stream = new OGGStream(file);
		int channels = ogg_stream.getChannels();
		int rate = ogg_stream.getRate();
		int bytes;
		int total_bytes = 0;
		do {
			bytes = ogg_stream.read(output);
			total_bytes += bytes;
		} while (bytes > 0);
		output.buffer().rewind();
		output.buffer().limit(total_bytes);
		return new Wave(output.buffer(), channels, 16, rate);
	}

	public @NonNull IntBuffer getBuffers() {
		return state.al_buffers;
	}

	public int getBuffer() {
		return state.al_buffers.get(0);
	}
}

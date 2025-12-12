package com.oddlabs.tt.audio;

import com.oddlabs.tt.audio.openal.ALBufferUtils;
import com.oddlabs.tt.audio.openal.OpenALAudio;
import com.oddlabs.tt.audio.openal.OpenALAudioSource;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.util.ByteBufferOutputStream;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;

final class QueuedAudioPlayer extends AbstractAudioPlayer {
	private static final int NUM_BUFFERS = 12;
	private final @NonNull ByteBufferOutputStream buffer_stream;
	private final @Nullable OpenALAudio audio;
	private final IntBuffer al_return_buffers = ALBufferUtils.createIntBuffer(1);
	private final @NonNull URL url;
	private final int channels;

	private @Nullable OGGStream ogg_stream;
	private int oldest_buffer = 0;

	QueuedAudioPlayer(@Nullable AudioSource source, @NonNull AudioParameters<@NonNull String> params) throws IOException {
		super(source, params);
		this.url = Utils.makeURL(params.sound);
		this.buffer_stream = new ByteBufferOutputStream(true);
		if ((!params.music && !Settings.getSettings().play_sfx) || this.source == null) {
			this.ogg_stream = null;
			this.channels = 0;
			this.audio = null;
			return;
		}

        source.setRelative(params.relative);

		setGain(params.gain);
		setPos(params.x, params.y, params.z);

        audio = new OpenALAudio(NUM_BUFFERS);
		IntBuffer al_buffers = audio.getBuffers();
		this.ogg_stream = new OGGStream(url);
		this.channels = ogg_stream.getChannels();
		for (int i = 0; i < al_buffers.capacity(); i++) {
			fillBuffer(al_buffers.get(i));
		}

		source.setLooping(false);
		source.setRolloff(ROLLOFF_FACTOR);
		source.setDistance(params.radius);
		source.setMinGain(0f);
        source.setMaxGain(1f);
		source.setPitch(params.pitch);
        ((OpenALAudioSource)source).queue(al_buffers);
		if (params.music || AudioManager.getManager().startPlaying())
			source.play();

		AudioManager.getManager().registerQueuedPlayer(this);
	}

	private void fillBufferFromStream(int al_buffer) {
		buffer_stream.buffer().flip();
/*		buffer_stream.buffer().limit(256);
		buffer_stream.buffer().position(0);
		for (int i = 0; i < buffer_stream.buffer().remaining(); i++)
			buffer_stream.buffer().put(i, (byte)0);*/
//System.out.println("al_buffer = " + al_buffer);
//		assert buffer_stream.buffer().remaining()%512 == 0;
		AL10.alBufferData(al_buffer, Wave.getFormat(channels, 16), buffer_stream.buffer(), ogg_stream.getRate());
	}

	private int fillBuffer(int al_buffer) throws IOException {
		buffer_stream.reset();
		int bytes = ogg_stream.read(buffer_stream);
		if (bytes > 0) {
			fillBufferFromStream(al_buffer);
		} else if (getParameters().looping) {
			ogg_stream = new OGGStream(url);
			bytes = ogg_stream.read(buffer_stream);
			fillBufferFromStream(al_buffer);
		}
		return bytes;
	}

	public void refill() throws IOException { // Run by the Refiller thread
		int processed = ((OpenALAudioSource)source).processed();
//System.out.println("this = " + this + " | processed = " + processed);
		while (processed > 0) {
//			assert processed <= al_buffers.capacity();
//			al_buffers.position(oldest_buffer);
//			al_buffers.limit(oldest_buffer + 1);
//			assert AL10.alIsBuffer(al_buffers.get(al_buffers.position())): al_buffers.get(al_buffers.position()) + " is not a buffer";
            ((OpenALAudioSource)source).unqueued(al_return_buffers);
//			assert al_return_buffers.get(0) == al_buffers.get(al_buffers.position()): "Unexpected buffer removed: " + al_return_buffers.get(0) + " should be " + al_buffers.get(al_buffers.position());
			int bytes = fillBuffer(al_return_buffers.get(0));
			if (bytes == 0) {
				stop();
				return;
			}
//			assert AL10.alIsBuffer(al_buffers.get(al_buffers.position())): al_buffers.get(al_buffers.position()) + " is not a buffer";
            ((OpenALAudioSource)source).queue(al_return_buffers);
//System.out.println("oldest_buffer = " + oldest_buffer + " | processed = " + processed + " | capacity = " + buffer_streams[oldest_buffer].buffer().capacity() + " | position " + buffer_streams[oldest_buffer].buffer().position() + " | limit " + buffer_streams[oldest_buffer].buffer().limit() + " al_size = " + AL10.alGetBufferi(al_buffers.get(oldest_buffer), AL10.AL_SIZE));
			oldest_buffer = (oldest_buffer + 1)%NUM_BUFFERS;
			processed--;
/*			int test_processed = AL10.alGetSourcei(source.getSource(), AL10.AL_BUFFERS_PROCESSED);
			assert test_processed >= processed: test_processed + " " + processed;*/
		}
		if (source.getState() == AudioSource.State.STOPPED)
			source.play();
//System.out.println("		AL10.alGetSourcei(source_index,AL10.AL_SOURCE_STATE) = " + 		AL10.alGetSourcei(source.getSource(),AL10.AL_SOURCE_STATE) + " | AL10.AL_STOPPED = " + AL10.AL_STOPPED + " | AL10.AL_PLAYING = " + AL10.AL_PLAYING);
	}

	@Override
	public void stop() {
		if (isPlaying()) {
			AudioManager.getManager().removeQueuedPlayer(this);
			super.stop();
		}
	}
}

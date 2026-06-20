package com.oddlabs.tt.audio;

import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * A stream used to decode OGG Vorbis audio data.
 */
public final class OGGStream extends NativeResource<OGGStream.Decoder> {

    protected static class Decoder extends NativeResource.NativeState {
        // STBVorbis JNI wrapper doesn't appear to correctly hold a reference to the buffer, so we must hold one.
        @SuppressWarnings("FieldCanBeLocal")
        private final @NonNull ByteBuffer decoderData;
        private final long decoder;
        private final int channels;
        private final int sampleRate;

        private Decoder(byte @NonNull [] vorbis) throws IOException {
            decoderData = BufferUtils.createByteBuffer(vorbis.length);
            decoderData.put(vorbis);
            decoderData.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                decoder = STBVorbis.stb_vorbis_open_memory(decoderData, error, null);
                if (decoder == 0) {
                    throw new IOException("Failed to open OGG Vorbis file. Error: " + error.get(0));
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);
                this.channels = info.channels();
                this.sampleRate = info.sample_rate();
            }
        }


        @Override
        public void close() {
            STBVorbis.stb_vorbis_close(decoder);
        }
    }

    public OGGStream(@NonNull URL source) throws IOException {
        this(source.openStream());
    }

    /** Reads OGG from the stream and in all cases closes the stream */
    public OGGStream(@NonNull InputStream stream) throws IOException {
        byte[] bytes;
        try (stream) {
            bytes = stream.readAllBytes();
        }
        this(bytes);
    }

    public OGGStream(byte @NonNull [] bytes) throws IOException {
        super(new Decoder(bytes));
    }

    public int getChannels() {
        return state.channels;
    }

    public int getRate() {
        return state.sampleRate;
    }

    public void seek(int sample) {
        STBVorbis.stb_vorbis_seek(state.decoder, sample);
    }

    /**
     * Decodes samples directly into the provided ShortBuffer.
     *
     * @param buffer Destination buffer. Must be direct.
     * @return The number of short values written to the buffer.
     */
    public int read(@NonNull ShortBuffer buffer) {
        assert buffer.position() == 0 && buffer.hasRemaining()
                : "Buffer must have remaining space and be at position 0";
        int samplesPerChannelRequest = buffer.remaining() / state.channels;
        int samplesRead = STBVorbis.stb_vorbis_get_samples_short_interleaved(state.decoder, state.channels, buffer);
        buffer.position(samplesRead * state.channels);
        return samplesRead * state.channels;
    }

    @Override
    public void close() {
        super.close();
    }
}

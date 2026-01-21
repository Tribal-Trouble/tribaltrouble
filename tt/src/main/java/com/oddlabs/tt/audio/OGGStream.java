package com.oddlabs.tt.audio;

import com.oddlabs.util.ByteBufferOutputStream;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public final class OGGStream implements AutoCloseable {
    private final long decoder;
    private final int channels;
    private final int sampleRate;
    private final @NonNull ByteBuffer vorbisData; // Keep reference to prevent GC

    // Temp buffer for reading samples (4096 samples * 2 channels usually)
    private final @NonNull ShortBuffer pcmBuffer;

	public OGGStream(@NonNull URL file) throws IOException {
        byte[] bytes = readAllBytes(file);
        vorbisData = BufferUtils.createByteBuffer(bytes.length);
        vorbisData.put(bytes);
        vorbisData.flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            decoder = STBVorbis.stb_vorbis_open_memory(vorbisData, error, null);
            if (decoder == 0) {
                throw new IOException("Failed to open OGG Vorbis file. Error: " + error.get(0));
            }

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            this.channels = info.channels();
            this.sampleRate = info.sample_rate();
        }
        
        // Allocate reasonable chunk size (e.g. 4096 frames)
        pcmBuffer = BufferUtils.createShortBuffer(4096 * channels);
	}

    private static byte[] readAllBytes(@NonNull URL url) throws IOException {
        try (InputStream is = url.openStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

	public int getChannels() {
		return channels;
	}

	public int getRate() {
		return sampleRate;
	}

	public int read(@NonNull ByteBufferOutputStream output) {
        int samplesRead = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcmBuffer);
        
        if (samplesRead > 0) {
            int totalSamples = samplesRead * channels;
            // Write to output stream
            // ByteBufferOutputStream writes bytes. We need to convert shorts to bytes in Native Order.
            boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
            
            for (int i = 0; i < totalSamples; i++) {
                short val = pcmBuffer.get(i);
                if (littleEndian) {
                    output.write((byte) (val & 0xFF));
                    output.write((byte) ((val >> 8) & 0xFF));
                } else {
                    output.write((byte) ((val >> 8) & 0xFF));
                    output.write((byte) (val & 0xFF));
                }
            }
            return totalSamples * 2; // bytes written
        }
        
		return 0;
	}

    /**
     * Decodes samples directly into the provided ShortBuffer.
     * @param buffer Destination buffer. Must be direct.
     * @return The number of short values written to the buffer.
     */
    public int read(@NonNull ShortBuffer buffer) {
        int samplesPerChannelRequest = buffer.remaining() / channels;
        int samplesRead = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, buffer);
        return samplesRead * channels;
    }

    @Override
    public void close() {
        if (decoder != 0) {
            STBVorbis.stb_vorbis_close(decoder);
        }
    }
}

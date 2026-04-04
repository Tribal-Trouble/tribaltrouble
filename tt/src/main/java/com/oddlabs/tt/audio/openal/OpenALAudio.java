package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.Wave;
import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

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
        try {
            Wave wave = new Wave(file);
            AL10.alBufferData(getBuffer(), wave.getFormat(), wave.getData(), wave.getSampleRate());
        } catch (UnsupportedAudioFileException e) {
            // Assume it's an ogg vorbis file
            loadOGG(file, getBuffer());
        }
    }

    private static void loadOGG(@NonNull URL file, int bufferId) throws IOException {
        ByteBuffer vorbisData = Utils.ioResourceToByteBuffer(file);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);

            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(vorbisData, channels, sampleRate);
            if (pcm == null) {
                throw new IOException("Failed to decode OGG Vorbis: " + file);
            }

            int format = Wave.getFormat(channels.get(0), 16);
            AL10.alBufferData(bufferId, format, pcm, sampleRate.get(0));

            LibCStdlib.free(pcm);
        }
    }

    public @NonNull IntBuffer getBuffers() {
        return state.al_buffers;
    }

    public int getBuffer() {
        return state.al_buffers.get(0);
    }
}

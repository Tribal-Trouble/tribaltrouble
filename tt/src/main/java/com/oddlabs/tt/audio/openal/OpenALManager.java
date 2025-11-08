package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioManager;
import org.jspecify.annotations.NonNull;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.OpenALException;

import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenALAudio Manager implementation using OpenAL
 */
public final class OpenALManager extends AudioManager {
    private static final Logger logger = Logger.getLogger(OpenALManager.class.getName());
    private final static int MAX_NUM_SOURCES = 32;

    public OpenALManager() throws LWJGLException {
        AL.create(null, -1, -1, false);
        logger.info("OpenAL version: " + AL10.alGetString(AL10.AL_VERSION));
        logger.info("OpenAL vendor: " + AL10.alGetString(AL10.AL_VENDOR));
        logger.info("OpenAL renderer: " + AL10.alGetString(AL10.AL_RENDERER));
        AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE);
        checkALError("alDistanceModel");
        super(generateSources(MAX_NUM_SOURCES));
    }

    private static @NonNull OpenALAudioSource @NonNull [] generateSources(int max) {
        List<@NonNull OpenALAudioSource> list = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            try {
                OpenALAudioSource source = new OpenALAudioSource();
                list.add(source);
            } catch (OpenALException _) {
                // If source generation fails, stop trying to create more
                break;
            }
        }
        return list.toArray(new OpenALAudioSource[0]);
    }

    @Override
    public @NonNull AudioManager masterGain(float gain) {
        AL10.alListenerf(AL10.AL_GAIN, gain);
        checkALError("alListener3f AL_GAIN");
        return this;
    }

    @Override
    public @NonNull AudioManager updateOrientation(@NonNull FloatBuffer fu) {
        AL10.alListener(AL10.AL_ORIENTATION, fu);
        checkALError("alListener3f AL_ORIENTATION");
        return this;
    }

    @Override
    public @NonNull AudioManager updatePosition(float x, float y, float z) {
        AL10.alListener3f(AL10.AL_POSITION, x, y, z);
        checkALError("alListener3f AL_POSITION");
        return this;
    }

    @Override
    public @NonNull Audio createAudio(@NonNull URL file) throws IOException {
        return new OpenALAudio(file);
    }

    @Override
    public void destroy() {
        super.destroy();
        AL.destroy();
    }

    /**
     * Checks for OpenAL errors and logs them
     * @param message A descriptive message for the context of the OpenAL call.
     */
    static void checkALError(String message) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            logger.log(Level.WARNING, "OpenAL Error (" + message + "): " + errorToString(error), new Throwable("stacktrace"));
        }
    }

    private static @NonNull String errorToString(int error) {
        return switch (error) {
            case AL10.AL_NO_ERROR -> "AL_NO_ERROR";
            case AL10.AL_INVALID_NAME -> "AL_INVALID_NAME";
            case AL10.AL_INVALID_ENUM -> "AL_INVALID_ENUM";
            case AL10.AL_INVALID_VALUE -> "AL_INVALID_VALUE";
            case AL10.AL_INVALID_OPERATION -> "AL_INVALID_OPERATION";
            case AL10.AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY";
            default -> "Unknown OpenAL Error: " + error;
        };
    }
}

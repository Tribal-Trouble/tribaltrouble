package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.AbstractAudioManager;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.AudioSource;
import com.oddlabs.tt.audio.ReverbType;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.ALC_FALSE;
import static org.lwjgl.openal.ALC10.ALC_TRUE;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC10.alcIsExtensionPresent;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.SOFTHRTF.ALC_HRTF_SOFT;
import static org.lwjgl.openal.SOFTHRTF.alcResetDeviceSOFT;

/**
 * Audio Manager implementation using OpenAL
 */
public final class OpenALManager extends AbstractAudioManager<OpenALManager, OpenALAudioSource> {
    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final Logger logger = Logger.getLogger(OpenALManager.class.getName());
    private static final int MAX_NUM_SOURCES = 32;

    private record ALData(long device, long context) implements AutoCloseable {
        @Override
        public void close() {
            alcDestroyContext(context);
            alcCloseDevice(device);
        }
    }

    private final @NonNull ALData data;
    private final EFXManager efxManager = new EFXManager();
    private final @NonNull OpenALAudioSource @NonNull [] sources;
    private final @NonNull Iterable<@NonNull OpenALAudioSource> sharedSources;

    // Queue for OpenAL cleanup tasks to be executed when this manager's context is current
    private final Queue<@NonNull Runnable> alCleanupTasks = new ConcurrentLinkedQueue<>();

    public OpenALManager(boolean headphoneMode) {
        this(initAL(headphoneMode));
    }

    private OpenALManager(@NonNull ALData data) {
        this.data = data;
        this.efxManager.init(data.device);
        this.sources = Stream.generate(() -> {
            try {
                return new OpenALAudioSource(this);
            } catch (Exception _) {
                // If source generation fails, stop trying to create more
                return null;
            }
        }).takeWhile(Objects::nonNull)
                .limit(MAX_NUM_SOURCES)
                .toArray(OpenALAudioSource[]::new);
        this.sharedSources = List.of(sources);

        logger.info("OpenAL version: " + AL10.alGetString(AL10.AL_VERSION));
        logger.info("OpenAL vendor: " + AL10.alGetString(AL10.AL_VENDOR));
        logger.info("OpenAL renderer: " + AL10.alGetString(AL10.AL_RENDERER));
        AL10.alDistanceModel(AL11.AL_INVERSE_DISTANCE_CLAMPED);
        checkALError("alDistanceModel");
    }

    private static @NonNull ALData initAL(boolean headphoneMode) {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        long device = alcOpenDevice(defaultDeviceName);
        if (device == 0) {
            throw new IllegalStateException("Failed to open default OpenAL device");
        }

        int[] attributes = headphoneMode && alcIsExtensionPresent(device, "ALC_SOFT_HRTF")
                ? new int[]{ALC_HRTF_SOFT, ALC_TRUE, 0}
                : new int[]{0};

        long context = alcCreateContext(device, attributes);
        if (context == 0) {
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        alcMakeContextCurrent(context);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        AL.createCapabilities(alcCapabilities);

        return new ALData(device, context);
    }

    @Override
    public void enqueueCleanup(@NonNull Runnable task) {
        alCleanupTasks.add(task);
    }

    /**
     * Processes all pending OpenAL cleanup tasks. This method must be called from a thread
     * that has this manager's OpenAL context current.
     */
    @Override
    protected void processCleanupTasks() {
        if (ALC10.alcGetCurrentContext() == 0) return;
        Runnable task;
        int count = 0;
        while ((task = alCleanupTasks.poll()) != null) {
            try {
                task.run();
                count++;
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error during OpenAL cleanup task execution", t);
            }
        }
        if (count > 0) {
            logger.info("Processed " + count + " OpenAL cleanup tasks");
        }
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    @Override
    protected @NonNull Iterable<@NonNull OpenALAudioSource> getSources() {
        return sharedSources;
    }

    long getContext() {
        return data.context;
    }

    long getDevice() {
        return data.device;
    }

    @Override
    public boolean isEFXSupported() {
        return efxManager.isSupported();
    }

    @Override
    public int getEFXEffectSlot() {
        return efxManager.getEffectSlot();
    }

    @Override
    public boolean isHRTFSupported() {
        return alcIsExtensionPresent(data.device, "ALC_SOFT_HRTF");
    }

    @Override
    public @NonNull OpenALManager setHeadphoneMode(boolean enabled) {
        if (isHRTFSupported()) {
            int[] attrs = {ALC_HRTF_SOFT, enabled ? ALC_TRUE : ALC_FALSE, 0};
            if (!alcResetDeviceSOFT(data.device, attrs)) {
                logger.warning("Failed to reset device for HRTF change: " + errorToString(AL10.alGetError()));
            }
        } else {
            logger.warning("ALC_SOFT_HRTF not supported");
        }

        return this;
    }

    @Override
    public @NonNull OpenALManager setMasterGain(float gain) {
        super.setMasterGain(gain);
        AL10.alListenerf(AL10.AL_GAIN, gain);
        checkALError("alListenerf AL_GAIN");
        return this;
    }

    @Override
    public @NonNull OpenALManager setListenerOrientation(@NonNull Vector3fc forward, @NonNull Vector3fc up) {
        super.setListenerOrientation(forward, up);
        try (var stack = MemoryStack.stackPush()) {
            var fb = stack.mallocFloat(6);
            fb.put(forward.x()).put(forward.y()).put(forward.z());
            fb.put(up.x()).put(up.y()).put(up.z());
            fb.flip();
            AL10.alListenerfv(AL10.AL_ORIENTATION, fb);
            checkALError("alListenerfv AL_ORIENTATION");
        }
        return this;
    }

    @Override
    public @NonNull OpenALManager setListenerPosition(float x, float y, float z) {
        super.setListenerPosition(x, y, z);
        AL10.alListener3f(AL10.AL_POSITION, x, y, z);
        checkALError("alListener3f AL_POSITION");
        return this;
    }

    @Override
    protected @NonNull AudioPlayer createPlayer(@Nullable AudioSource source, float x, float y, float z,
            @NonNull AudioParameters params) {
        return createPlayer((OpenALAudioSource) source, x, y, z, params);
    }

    private @NonNull AudioPlayer createPlayer(@Nullable OpenALAudioSource source, float x, float y, float z,
            @NonNull AudioParameters params) {
        AudioPlayer player;
        if (!params.audio().isStreaming()) {
            var ourPlayer = new OpenALAudioPlayer(this, source, x, y, z, params);
            if (params.ambient()) {
                registerAmbient(ourPlayer);
            }
            player = ourPlayer;
        } else {
            var queuedPlayer = new OpenALQueuedAudioPlayer(this, source, x, y, z, params);
            addQueuedPlayer(queuedPlayer);
            player = queuedPlayer;
        }

        return player;
    }

    @Override
    public @NonNull Audio createAudio(@NonNull URL file) throws IOException {
        return new OpenALAudio(this, file);
    }

    @Override
    public synchronized void close() {
        if (isClosed()) return;
        if (!ALC10.alcMakeContextCurrent(data.context)) {
            logger.warning("Failed to make OpenAL context current for shutdown: " + data.context);
        }
        try {
            processCleanupTasks();
            super.close();
            logger.info("AudioManager closing sources...");
            for (OpenALAudioSource source : sources) {
                try {
                    source.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error closing audio source", e);
                }
            }
            Arrays.fill(sources, null);
            processCleanupTasks();
            efxManager.close();
        } finally {
            data.close();
        }
    }

    public @NonNull EFXManager getEfxManager() {
        return efxManager;
    }

    @Override
    public void setReverb(@NonNull ReverbType type) {
        efxManager.setReverb(type);
    }

    @Override
    public void setReverb(@NonNull ReverbType from, @NonNull ReverbType to, float factor) {
        efxManager.setReverb(from, to, factor);
    }

    /**
     * Checks for OpenAL errors and logs them
     *
     * @param message A descriptive message for the context of the OpenAL call.
     */
    public static void checkALError(@NonNull String message) {
        if (DEBUG) {
            long context = ALC10.alcGetCurrentContext();
            if (context != 0) {
                int error = AL10.alGetError();
                if (error != AL10.AL_NO_ERROR) {
                    logger.log(Level.WARNING, "OpenAL Error (" + message + ") [Context: " + context + "]: "
                            + errorToString(error), new Throwable("stacktrace"));
                }
            } else {
                logger.log(Level.WARNING, "OpenAL Error (" + message + "): no current context");
            }
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

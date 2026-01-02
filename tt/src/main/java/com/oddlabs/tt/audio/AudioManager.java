package com.oddlabs.tt.audio;

import com.oddlabs.tt.audio.openal.OpenALManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.AudioImplementation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages audio playback, including positional audio sources and ambient sounds.
 * This class is responsible for initializing the audio backend, allocating sources,
 * and controlling global audio properties like listener orientation and master gain.
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class AudioManager implements AudioImplementation, AutoCloseable {
    private static final Logger logger = Logger.getLogger(AudioManager.class.getName());

    private static final Holder SINGLETON = new Holder();

    private static class Holder {
        @Nullable
        final AudioManager manager;

        Holder() {
            AudioManager instance = null;
            try {
                instance = new OpenALManager();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create audio manager", e);
            }
            manager = instance;
        }
    }

    private final Set<@NonNull AudioSource> ambients = new CopyOnWriteArraySet<>();
    private final RefillerList queued_players = new RefillerList();
    private final @NonNull AudioSource @NonNull [] sources;

    private int sound_play_counter = Settings.getSettings().play_sfx ? 1 : 0;

    /**
     * @return The singleton AudioManager instance.
     * @throws IllegalStateException if the audio manager could not be initialized.
     */
    public static @NonNull AudioManager getManager() throws IllegalStateException {
        if (SINGLETON.manager == null) {
            throw new IllegalStateException("Audio manager is not available. Check logs for initialization errors.");
        }
        return SINGLETON.manager;
    }

    protected AudioManager(@NonNull AudioSource @NonNull [] sources) {
        this.sources = sources;
    }

    /**
     * Controls the gain for ALL audio sources.
     *
     * @param gain the master gain for ALL audio sources.
     * @return this
     */
    public abstract @NonNull AudioManager masterGain(float gain);

    /**
     * Update the listener orientation using forward and up vectors
     *
     * @param fu listener orientation
     * @return this
     */
    public abstract @NonNull AudioManager updateOrientation(@NonNull FloatBuffer fu);

    /**
     * Update the listener position.
     *
     * @param x listener x position
     * @param y listener y position
     * @param z listener z position
     * @return this
     */
    public abstract @NonNull AudioManager updatePosition(float x, float y, float z);

    /**
     * Create Audio instance for the specified file.
     *
     * @param file The audio resource file.
     * @return the created instance
     */
    public abstract @NonNull Audio createAudio(@NonNull URL file) throws IOException;

    public void stopSources() {
        sound_play_counter--;
        if (sound_play_counter == 0) {
            for (AudioSource source : sources) {
                int rank = source.getRank();
                switch (rank) {
                    case AudioPlayer.AUDIO_RANK_MUSIC: // can't stop the music
                        continue;
                    case AudioPlayer.AUDIO_RANK_AMBIENT:
                        source.pause();
                        break;
                    default:
                        source.stop();
                        break;
                }
            }
        }
    }

    public @NonNull AbstractAudioPlayer newAudio(@NonNull CameraState camera_state, @NonNull AudioParameters<?> params) {
        AudioSource source = getSource(camera_state, params);
        if (source == null) {
            return createPlayer(null, params);
        }
        return doNewAudio(source, params);
    }

    @Override
    public @NonNull AbstractAudioPlayer newAudio(@NonNull AudioParameters<?> params) {
        AudioSource source = getSource(params);
        if (source == null) {
            return createPlayer(null, params);
        }
        return doNewAudio(source, params);
    }

    private static @NonNull AbstractAudioPlayer doNewAudio(@NonNull AudioSource source, @NonNull AudioParameters<?> params) {
        // Bind the audio to the source before creating the player.
        if (params.sound instanceof Audio) {
            source.setAudio((Audio) params.sound);
        }
        return createPlayer(source, params);
    }

    @SuppressWarnings("unchecked")
    private static @NonNull AbstractAudioPlayer createPlayer(@Nullable AudioSource source, @NonNull AudioParameters<?> params) {
        if (params.sound instanceof Audio) {
            return new AudioPlayer(source, (AudioParameters<Audio>) params);
        } else if (params.sound instanceof String) {
            try {
                return new QueuedAudioPlayer(source, (AudioParameters<String>) params);
            } catch (IOException ex) {
                throw new IllegalArgumentException("Could not load " + params.sound, ex);
            }
        } else {
            throw new IllegalArgumentException("Unrecognized audio parameters : " + params.sound.getClass().getSimpleName());
        }
    }

    public void startSources() {
        if (sound_play_counter == 0) {
            for (AudioSource ambient : ambients) {
                ambient.play();
            }
        }
        sound_play_counter++;
    }

    public void registerAmbient(@NonNull AudioSource source) {
        ambients.add(source);
    }

    public void removeAmbient(@NonNull AudioSource source) {
        ambients.remove(source);
    }

    boolean startPlaying() {
        return sound_play_counter > 0;
    }

    private @Nullable AudioSource findSource(@NonNull AudioParameters<?> params) {
        // Check for free sources
        int worst_rank = Integer.MAX_VALUE;
        for (AudioSource source : sources) {
            var sourceState = source.getState();
            if ((sourceState == AudioSource.State.INITIAL || sourceState == AudioSource.State.STOPPED) && source.getRank() < AudioPlayer.AUDIO_RANK_AMBIENT) {
                if (source.getAudioPlayer() != null)
                    source.getAudioPlayer().stop();
                return source;
            }
            if (worst_rank > source.getRank())
                worst_rank = source.getRank();
        }

        if (params.rank > worst_rank) {
            for (AudioSource source : sources) {
                if (source.getRank() == worst_rank) {
                    return source;
                }
            }
        }
        return null;
    }

    private @Nullable AudioSource getSource(@NonNull AudioParameters<?> params) {
        AudioSource best_source = findSource(params);
        stopSource(best_source);
        return best_source;
    }

    private @Nullable AudioSource getSource(@NonNull CameraState camera_state, @NonNull AudioParameters<?> params) {
        float this_dist_squared;
        if (params.relative)
            this_dist_squared = params.x * params.x + params.y * params.y + params.z * params.z;
        else
            this_dist_squared = getCamDistSquared(camera_state, params.x, params.y, params.z);

        if (this_dist_squared > params.distance * params.distance) {
            return null;
        }

        AudioSource best_source = findSource(params);

        if (best_source == null) {
            float max_dist_squared = this_dist_squared;
            for (AudioSource source : sources) {
                if (source.getRank() == params.rank) {
                    float[] position = source.getPosition();
                    float dist_squared = getCamDistSquared(camera_state, position[0], position[1], position[2]);
                    if (dist_squared > max_dist_squared) {
                        max_dist_squared = dist_squared;
                        best_source = source;
                    }
                }
            }
        }
        stopSource(best_source);
        return best_source;
    }

    private static void stopSource(@Nullable AudioSource source) {
        AbstractAudioPlayer player;
        if (source != null && (player = source.getAudioPlayer()) != null) {
            player.stop();
        }
    }

    private static float getCamDistSquared(@NonNull CameraState camera_state, float x, float y, float z) {
        float dx = x - camera_state.getCurrentX();
        float dy = y - camera_state.getCurrentY();
        float dz = z - camera_state.getCurrentZ();
        return dx * dx + dy * dy + dz * dz;
    }

    void registerQueuedPlayer(@NonNull QueuedAudioPlayer q) {
        queued_players.registerQueuedPlayer(q);
    }

    void removeQueuedPlayer(@NonNull QueuedAudioPlayer q) {
        queued_players.removeQueuedPlayer(q);
    }

    @Override
    public void close() {
        logger.info("AudioManager closing queued players...");
        queued_players.close();
        logger.info("AudioManager closing sources...");
        for (AudioSource source : sources) try {
            if (null != source) source.close(); // Ensure all sources are closed
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error closing audio source", e);
        } finally {
            Arrays.fill(sources, null);
        }
        logger.info("AudioManager closed.");
    }
}
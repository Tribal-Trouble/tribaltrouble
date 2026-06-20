package com.oddlabs.tt.audio;

import com.oddlabs.tt.camera.CameraState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Base class for {@link AudioManager} implementations.
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class AbstractAudioManager<AM extends AbstractAudioManager<AM, AS>, AS extends AudioSource> implements
        AudioManager, AutoCloseable {
    private static final Logger logger = Logger.getLogger(AudioManager.class.getSimpleName());

    /** The interval (in seconds) between ambient sound proximity checks. */
    private static final float AMBIENT_UPDATE_INTERVAL = 0.1f;

    private final Set<@NonNull AS> ambients = new CopyOnWriteArraySet<>();
    private final Set<@NonNull AmbientAudioSource> active_ambient = new CopyOnWriteArraySet<>();
    private final Set<@NonNull QueuedAudioPlayer<AM, AS>> queued_players = new CopyOnWriteArraySet<>();
    private final Set<@NonNull AbstractAudioPlayer<AM, AS>> fading_players = new CopyOnWriteArraySet<>();
    private float updateTime = 0f;
    private volatile boolean closed = false;

    private volatile boolean isPlaying = false;
    private float masterGain = 1f;
    private float sfxGain = 1f;
    private float musicGain = 1f;
    private boolean sfxEnabled = true;

    private final Vector3f listenerPosition = new Vector3f();
    private final Vector3f listenerForward = new Vector3f(0, 0, -1);
    private final Vector3f listenerUp = new Vector3f(0, 1, 0);

    private final AtomicInteger sound_play_counter = new AtomicInteger(0);

    protected AbstractAudioManager() {
    }

    @SuppressWarnings("unchecked")
    protected @NonNull AM self() {
        return (AM) this;
    }

    public final boolean isClosed() {
        return closed;
    }

    protected abstract @NonNull Iterable<@NonNull AS> getSources();

    /**
     * Controls the gain for ALL audio sources.
     *
     * @param gain the master gain for ALL audio sources.
     * @return this
     */
    @Override
    public @NonNull AM setMasterGain(float gain) {
        this.masterGain = gain;
        return self();
    }

    @Override
    public float getMasterGain() {
        return masterGain;
    }

    @Override
    public @NonNull AM setSfxGain(float gain) {
        this.sfxGain = gain;
        for (AS source : getSources()) {
            AudioPlayer player = source.getAudioPlayer();
            if (player != null && !player.getParameters().audio().isStreaming()) {
                player.setGain(player.getParameters().gain());
            }
        }
        resetVolumes();

        return self();
    }

    @Override
    public float getSfxGain() {
        return sfxGain;
    }

    @Override
    public @NonNull AM setMusicGain(float gain) {
        this.musicGain = gain;
        for (AS source : getSources()) {
            AudioPlayer player = source.getAudioPlayer();
            if (player != null && player.getParameters().audio().isStreaming()) {
                player.setGain(player.getParameters().gain());
            }
        }

        return self();
    }

    @Override
    public float getMusicGain() {
        return musicGain;
    }

    @Override
    public synchronized @NonNull AM setSfxEnabled(boolean enabled) {
        if (this.sfxEnabled == enabled) return self();
        this.sfxEnabled = enabled;
        if (sound_play_counter.get() > 0) {
            if (enabled) {
                updateAmbientSources();
            } else {
                for (AS source : getSources()) {
                    if (source.getRank() != AudioParameters.RANK_MUSIC) {
                        if (source.getRank() == AudioParameters.RANK_AMBIENT) {
                            source.pause();
                        } else {
                            source.stop();
                        }
                    }
                }
                active_ambient.clear();
            }
        }

        return self();
    }

    @Override
    public boolean isSfxEnabled() {
        return !closed && sfxEnabled;
    }

    @Override
    public abstract @NonNull AM setHeadphoneMode(boolean enabled);

    @Override
    public abstract boolean isHRTFSupported();

    @Override
    public abstract boolean isEFXSupported();

    public abstract int getEFXEffectSlot();

    /**
     * Update the listener orientation using forward and up vectors
     *
     * @param forward listener forward vector
     * @param up listener up vector
     * @return this
     */
    @Override
    public @NonNull AudioManager setListenerOrientation(@NonNull Vector3fc forward, @NonNull Vector3fc up) {
        listenerForward.set(forward);
        listenerUp.set(up);
        return self();
    }

    public @NonNull Vector3fc getListenerForward() {
        return listenerForward;
    }

    public @NonNull Vector3fc getListenerUp() {
        return listenerUp;
    }

    @Override
    public @NonNull Vector3fc getListenerPosition() {
        return listenerPosition;
    }

    /**
     * Update the listener position.
     *
     * @param x listener x position
     * @param y listener y position
     * @param z listener z position
     * @return this
     */
    @Override
    public @NonNull AM setListenerPosition(float x, float y, float z) {
        listenerPosition.set(x, y, z);
        return self();
    }

    /**
     * Create an Audio instance for the specified file.
     *
     * @param file The audio resource file.
     * @return the created instance
     */
    @Override
    public abstract @NonNull Audio createAudio(@NonNull URL file) throws IOException;

    private void resetVolumes() {
        for (AmbientAudioSource anActive_ambient : active_ambient) {
            anActive_ambient.resetVolume();
        }
    }

    public final synchronized void update(float t) {
        processCleanupTasks();
        if (closed || t == 0f) {
            return;
        }

        updateTime += t;
        if (updateTime >= AMBIENT_UPDATE_INTERVAL) {
            updateTime -= AMBIENT_UPDATE_INTERVAL;
            if (sfxEnabled) {
                updateAmbientSources();
            }
        }

        // We only want to play ambient sounds around us at the moment...
        if (sfxEnabled) {
            for (AmbientAudioSource anActive_ambient : active_ambient) {
                anActive_ambient.update(t);
            }
        }

        fading_players.removeIf(player -> {
            return !player.updateFade(t);
        });
    }

    private void updateAmbientSources() {
        var listenerPos = getListenerPosition();

        // Mark all current slots as "potential removals" by setting target to 0
        active_ambient.forEach(a -> a.setGainTarget(0f));

        float max_dist_sq = AudioParameters.DISTANCE_AMBIENT * AudioParameters.DISTANCE_AMBIENT;

        for (AS ambientSource : ambients) {
            var player = ambientSource.getAudioPlayer();
            if (player != null && player.isPlaying()) {
                Vector3fc position = ambientSource.getPosition();
                float dist_sq = position.distanceSquared(listenerPos);

                if (dist_sq < max_dist_sq) {
                    active_ambient.stream()
                            .filter(a -> a.isUsing(ambientSource))
                            .findFirst()
                            .orElseGet(() -> {
                                var wrapper = new AmbientAudioSource(ambientSource);
                                active_ambient.add(wrapper);
                                return wrapper;
                            })
                            .setGainTarget(1f);
                }
            }
        }
    }

    /** {@return true if sound effects are enabled and global mute is not enabled.} */
    @Override
    public final boolean startPlaying() {
        return sfxEnabled && sound_play_counter.intValue() > 0;
    }

    public final synchronized void play() {
        if (closed) return;
        if (!isPlaying) {
            isPlaying = true;
            for (AS s : getSources()) {
                if (s.getAudioPlayer() != null) {
                    s.play();
                }
            }
            active_ambient.forEach(AmbientAudioSource::play);
        }
    }

    public final synchronized void pause() {
        if (closed) return;
        if (isPlaying) {
            for (AS s : getSources()) {
                s.pause();
            }
            active_ambient.forEach(AmbientAudioSource::pause);
            isPlaying = false;
        }
    }

    public final synchronized void stop() {
        if (closed) return;
        isPlaying = false;
        for (AS s : getSources()) {
            s.stop();
        }
        active_ambient.forEach(AmbientAudioSource::stop);
        active_ambient.clear();
    }

    @Override
    public final @NonNull AudioPlayer newAudio(@NonNull CameraState camera_state, float x, float y, float z,
            @NonNull AudioParameters params) {
        AS source = getSource(camera_state, x, y, z, params);
        return newAudio(source, x, y, z, params);
    }

    @Override
    public final @NonNull AudioPlayer newAudio(float x, float y, float z, @NonNull AudioParameters params) {
        AudioSource source = getSource(x, y, z, params);
        return newAudio(source, x, y, z, params);
    }

    protected @NonNull AudioPlayer newAudio(@Nullable AudioSource source, float x, float y, float z,
            @NonNull AudioParameters params) {
        if (null != source && !params.audio().isStreaming()) {
            // Bind the audio to the source before creating the player.
            source.setAudio(params.audio().get());
        }
        return createPlayer(source, x, y, z, params);
    }

    public @NonNull AM startSources() {
        if (sound_play_counter.getAndIncrement() == 0) {
            if (sfxEnabled) {
                ambients.forEach(AudioSource::play);
            }
            for (AudioSource source : getSources()) {
                if (source.getAudioPlayer() != null && source.getRank() == AudioParameters.RANK_MUSIC) {
                    source.play();
                }
            }
        }

        return self();
    }

    public @NonNull AM stopSources() {
        if (sound_play_counter.decrementAndGet() == 0) {
            for (AS source : getSources()) {
                int rank = source.getRank();
                switch (rank) {
                    case AudioParameters.RANK_MUSIC, AudioParameters.RANK_AMBIENT -> source.pause();
                    default -> source.stop();
                }
            }
        }
        if (sound_play_counter.intValue() < 0) sound_play_counter.set(0);

        return self();
    }

    protected boolean addQueuedPlayer(@NonNull QueuedAudioPlayer<AM, AS> player) {
        return queued_players.add(player);
    }

    boolean removeQueuedPlayer(@NonNull QueuedAudioPlayer<AM, AS> player) {
        return queued_players.remove(player);
    }

    protected boolean registerAmbient(@NonNull AbstractAudioPlayer<AM, AS> player) {
        var source = player.getSource();
        return source != null && ambients.add(source);
    }

    boolean removeAmbient(@NonNull AbstractAudioPlayer<AM, AS> player) {
        var source = player.getSource();

        var removed = null != source && ambients.remove(source);
        if (removed) {
            updateAmbientSources();
        }
        return removed;
    }

    public final void registerFadingPlayer(@NonNull AbstractAudioPlayer<AM, AS> player) {
        fading_players.add(player);
    }

    private @Nullable AS findSource(float x, float y, float z, @NonNull AudioParameters params) {
        float lowest_perceived_gain = Float.MAX_VALUE;
        int lowest_rank = Integer.MAX_VALUE;
        var listenerPosition = getListenerPosition();

        AS best_candidate = null;
        for (AS source : getSources()) {
            var sourceState = source.getState();
            if ((sourceState == AudioSource.State.INITIAL || sourceState == AudioSource.State.STOPPED) && source
                    .getRank() < AudioParameters.RANK_AMBIENT) {
                if (source.getAudioPlayer() != null)
                    source.getAudioPlayer().stop();
                return source;
            }

            int sourceRank = source.getRank();
            float perceivedGain = calculatePerceivedGain(source, listenerPosition);

            if (sourceRank < lowest_rank) {
                lowest_rank = sourceRank;
                lowest_perceived_gain = perceivedGain;
                best_candidate = source;
            } else if (sourceRank == lowest_rank) {
                if (perceivedGain < lowest_perceived_gain) {
                    lowest_perceived_gain = perceivedGain;
                    best_candidate = source;
                }
            }
        }

        // Steal source if it's lower priority OR same priority but quieter
        if (best_candidate != null && (params.rank() > lowest_rank || (params.rank() == lowest_rank
                && calculatePerceivedGain(x, y, z, params, listenerPosition) > lowest_perceived_gain))) {
            return best_candidate;
        }

        return null;
    }

    private float calculatePerceivedGain(float x, float y, float z, @NonNull AudioParameters p,
            @NonNull Vector3fc listenerPosition) {
        if (p.relative()) return p.gain();

        float dist = listenerPosition.distance(x, y, z);

        float refDist = p.radius();
        float maxDist = p.distance();
        float rolloff = (maxDist > refDist)
                ? (refDist / AbstractAudioPlayer.SILENCE_THRESHOLD - refDist) / (maxDist - refDist)
                : 1.0f;

        // AL_INVERSE_DISTANCE_CLAMPED model
        return p.gain() * (refDist / (refDist + rolloff * Math.max(0, dist - refDist)));
    }

    private float calculatePerceivedGain(@NonNull AudioSource source, @NonNull Vector3fc listenerPosition) {
        AudioPlayer player = source.getAudioPlayer();
        if (player == null) return 0f;

        AudioParameters p = player.getParameters();
        if (p.relative()) return p.gain();

        float dist = listenerPosition.distance(source.getPosition());

        float refDist = source.getDistance();
        float rolloff = source.getRolloff();

        // AL_INVERSE_DISTANCE_CLAMPED model
        return p.gain() * (refDist / (refDist + rolloff * Math.max(0, dist - refDist)));
    }

    private synchronized @Nullable AudioSource getSource(float x, float y, float z, @NonNull AudioParameters params) {
        if (closed) return null;
        AudioSource best_source = findSource(x, y, z, params);
        stopSource(best_source);
        return best_source;
    }

    private synchronized @Nullable AS getSource(@NonNull CameraState camera_state, float x, float y, float z,
            @NonNull AudioParameters params) {
        if (closed) return null;
        float this_dist_squared = params.relative()
                ? x * x + y * y + z * z
                : getCamDistSquared(camera_state, x, y, z);

        if (this_dist_squared > params.distance() * params.distance()) {
            return null;
        }

        AS best_source = findSource(x, y, z, params);

        if (best_source == null) {
            float max_dist_squared = this_dist_squared;
            for (AS source : getSources()) {
                if (source.getRank() == params.rank()) {
                    Vector3fc position = source.getPosition();
                    float dist_squared = getCamDistSquared(camera_state, position.x(), position.y(), position.z());
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
        AudioPlayer player;
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

    /**
     * Enqueues a task to be executed when the audio implementation's native context is current.
     * This is used by native resources (like buffers or sources) to ensure context-safe cleanup.
     *
     * @param task The cleanup task to enqueue.
     */
    public abstract void enqueueCleanup(@NonNull Runnable task);

    protected abstract void processCleanupTasks();

    protected abstract @NonNull AudioPlayer createPlayer(@Nullable AudioSource source, float x, float y, float z,
            @NonNull AudioParameters params);

    @Override
    public synchronized void close() {
        if (closed) return;
        logger.info("AudioManager stopping queued players...");
        queued_players.forEach(QueuedAudioPlayer::stop);
        closed = true;
        processCleanupTasks();
        logger.info("AudioManager closed.");
    }

    private class AmbientAudioSource {
        private final @NonNull AS source;
        private float gain_target = 0f;
        private float gain = 0f;
        private boolean playing = false;

        AmbientAudioSource(@NonNull AS source) {
            this.source = source;
            source.setGain(0f);
        }

        boolean isUsing(@NonNull AS s) {
            return source == s;
        }

        boolean isPlaying() {
            var p = source.getAudioPlayer();
            if (p == null) return false;
            return p.isPlaying();
        }

        void play() {
            if (playing) {
                source.play();
            }
        }

        void pause() {
            source.pause();
        }

        void stop() {
            source.stop();
        }

        @NonNull
        Vector3f getPosition() {
            return source.getPosition();
        }

        void setGainTarget(float gainTarget) {
            gain_target = gainTarget;
        }

        void resetVolume() {
            AudioPlayer player = source.getAudioPlayer();
            if (player != null) {
                float volume = sfxGain * gain * player.getParameters().gain();
                source.setGain(volume);
            }
        }

        void update(float t) {
            if (gain != gain_target) {
                gain += (gain_target - gain) * Math.min(1f, t * .25f);
                if (Math.abs(gain - gain_target) < .001f) {
                    gain = gain_target;
                }
                resetVolume();

                if (gain == 0f) {
                    source.pause();
                    playing = false;
                } else if (!playing) {
                    playing = true;
                    if (startPlaying())
                        source.play();
                }
            }
        }
    }
}

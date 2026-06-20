package com.oddlabs.tt.audio;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** base implementation of {@link AudioPlayer} */
public abstract class AbstractAudioPlayer<AM extends AbstractAudioManager<AM, AS>, AS extends AudioSource> implements
        AudioPlayer {

    /** The volume threshold at which a sound is considered silent (reached at max distance). */
    static final float SILENCE_THRESHOLD = 0.032f;

    protected final @NonNull AM manager;
    protected final @Nullable AS source;
    protected final @NonNull AudioParameters parameters;
    protected volatile boolean playing = false;

    private float decay_rate;
    private boolean is_fading = false;
    private float current_gain;

    protected AbstractAudioPlayer(@NonNull AM manager, @Nullable AS source,
            float x, float y, float z, @NonNull AudioParameters params) {
        this.manager = manager;
        this.source = source;
        this.parameters = params;
        this.current_gain = params.gain();
        if (source == null || (!params.audio().isStreaming() && !manager.isSfxEnabled())) {
            return;
        }
        source.setAudioPlayer(this);
        playing = true;

        source.setLooping(params.looping());
        source.setRelative(params.relative());

        setGain(params.gain());

        // Calculate rolloff so the sound reaches SILENCE_THRESHOLD at params.distance
        float refDist = params.radius();
        float maxDist = params.distance();
        float rolloff = (maxDist > refDist)
                ? (refDist / SILENCE_THRESHOLD - refDist) / (maxDist - refDist)
                : 1.0f;

        source.setRolloff(rolloff);
        source.setDistance(refDist);
        source.setMinGain(0f);
        source.setMaxGain(1f);
        source.setPitch(params.pitch());

        updateEnvironmentalEffects();
        setPosition(x, y, z);

        var state = source.getState();
        assert state == AudioSource.State.STOPPED || state == AudioSource.State.INITIAL;
    }

    @Override
    public final boolean isPlaying() {
        return playing;
    }

    @Override
    public @Nullable AS getSource() {
        return source;
    }

    @Override
    public final @NonNull AudioParameters getParameters() {
        return parameters;
    }

    /** {@return The audio associated with this player} */
    protected @NonNull Audio getAudio() {
        return parameters.audio().get();
    }

    /** {@return The number of buffers allocated for this player} */
    protected int getBufferCount() {
        return 0;
    }

    @Override
    public final void setGain(float gain) {
        this.current_gain = gain;
        if (playing && source != null) {
            source.setGain(gain * (parameters.audio().isStreaming() ? manager.getMusicGain() : manager.getSfxGain()));
        }
    }

    @Override
    public final void setPosition(float x, float y, float z) {
        if (playing && source != null) {
            source.setPosition(x, y, z);
            updateAirAbsorption(x, y, z);
        }
    }

    private void updateEnvironmentalEffects() {
        if (source == null) return;

        // Music and notifications don't get environmental effects/reverb
        boolean useEFX = parameters.rank() != AudioParameters.RANK_MUSIC && parameters.rank()
                != AudioParameters.RANK_NOTIFICATION;

        if (manager.isEFXSupported()) {
            int slot = useEFX ? manager.getEFXEffectSlot() : 0;
            source.setAuxiliarySend(slot, 0);
        }
    }

    private void updateAirAbsorption(float x, float y, float z) {
        if (source == null) return;

        // Music doesn't get muffled by distance
        if (parameters.rank() == AudioParameters.RANK_MUSIC || parameters.rank() == AudioParameters.RANK_NOTIFICATION) {
            source.setDirectFilterGainHF(1.0f);
            return;
        }

        if (manager.isEFXSupported()) {
            float dist = manager.getListenerPosition().distance(x, y, z);

            // Simple air absorption: brighter up close, muffled far away
            // Clamp to [0.1, 1.0] to avoid total silence in HF
            float maxDist = parameters.distance() != Float.MAX_VALUE ? parameters.distance() : 1000f;
            float gainHF = Math.clamp(1.0f - (dist / maxDist), 0.1f, 1.0f);

            source.setDirectFilterGainHF(gainHF);
        }
    }

    @Override
    public @NonNull AudioPlayer stop() {
        if (playing) {
            if (source != null) {
                source.stop();
            }
            if (parameters.ambient()) {
                manager.removeAmbient(this);
            }
            playing = false;
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final @NonNull AudioPlayer stop(float decayRate) {
        assert decayRate > 0.0f : "decayRate must be positive";
        if (!playing) return this;
        this.decay_rate = decayRate;
        this.is_fading = true;
        manager.registerFadingPlayer(this);

        return this;
    }

    /** {@return true if the player is currently fading out otherwise false.} */
    final boolean updateFade(float t) {
        if (!is_fading || !playing) return false;
        float fadeout_gain = current_gain * (float) Math.exp(-decay_rate * t);
        if (fadeout_gain < 0.01f) {
            stop();
            return is_fading = false;
        } else {
            setGain(fadeout_gain);
            return true;
        }
    }
}

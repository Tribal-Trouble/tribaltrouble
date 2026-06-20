package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.audio.ReverbType;
import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

import java.util.logging.Logger;

import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DECAY_TIME;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DENSITY;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_DIFFUSION;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_GAIN;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_GAINHF;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY;
import static org.lwjgl.openal.EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN;
import static org.lwjgl.openal.EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO;
import static org.lwjgl.openal.EXTEfx.AL_EFFECTSLOT_EFFECT;
import static org.lwjgl.openal.EXTEfx.AL_EFFECT_EAXREVERB;
import static org.lwjgl.openal.EXTEfx.AL_EFFECT_NULL;
import static org.lwjgl.openal.EXTEfx.AL_EFFECT_TYPE;
import static org.lwjgl.openal.EXTEfx.alAuxiliaryEffectSloti;
import static org.lwjgl.openal.EXTEfx.alDeleteAuxiliaryEffectSlots;
import static org.lwjgl.openal.EXTEfx.alDeleteEffects;
import static org.lwjgl.openal.EXTEfx.alEffectf;
import static org.lwjgl.openal.EXTEfx.alEffecti;
import static org.lwjgl.openal.EXTEfx.alGenAuxiliaryEffectSlots;
import static org.lwjgl.openal.EXTEfx.alGenEffects;

/**
 * Manages OpenAL EFX extension features, including environmental reverb effects.
 * Handles initialization, blending between reverb presets, and cleanup of EFX resources.
 */
public final class EFXManager implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(EFXManager.class.getSimpleName());

    private int effectSlot;
    private int reverbEffect;
    private boolean supported = false;


    // Reverb Snapshot record for parameter interpolation
    private record ReverbSnapshot(
                                  float density,
                                  float diffusion,
                                  float gain,
                                  float gainHF,
                                  float decayTime,
                                  float reflectionsGain,
                                  float reflectionsDelay,
                                  float lateReverbDelay,
                                  float airAbsorption
    ) {
        static final ReverbSnapshot NONE = new ReverbSnapshot(0f, 0f, 0f, 0f, 0.1f, 0f, 0f, 0f, 0.994f);
        static final ReverbSnapshot GENERIC = new ReverbSnapshot(0.1f, 0.1f, 0.05f, 0.1f, 0.3f, 0.05f, 0.02f, 0.0f,
                0.994f);
        static final ReverbSnapshot FOREST = new ReverbSnapshot(0.1f, 0.1f, 0.15f, 0.2f, 0.4f, 0.1f, 0.02f, 0.0f, 0.9f);
        static final ReverbSnapshot VALLEY = new ReverbSnapshot(0.2f, 0.3f, 0.4f, 0.5f, 1.2f, 0.3f, 0.1f, 0.1f, 0.994f);
        static final ReverbSnapshot UNDERWATER = new ReverbSnapshot(1.0f, 0.1f, 0.8f, 0.1f, 1.2f, 0.01f, 0.01f, 0.0f,
                0.9f);

        static ReverbSnapshot get(@NonNull ReverbType type) {
            return switch (type) {
                case NONE -> NONE;
                case GENERIC -> GENERIC;
                case FOREST -> FOREST;
                case VALLEY -> VALLEY;
                case UNDERWATER -> UNDERWATER;
            };
        }

        static ReverbSnapshot blend(@NonNull ReverbSnapshot a, @NonNull ReverbSnapshot b, float factor) {
            float f = Math.clamp(factor, 0f, 1f);
            return new ReverbSnapshot(
                    lerp(a.density, b.density, f),
                    lerp(a.diffusion, b.diffusion, f),
                    lerp(a.gain, b.gain, f),
                    lerp(a.gainHF, b.gainHF, f),
                    lerp(a.decayTime, b.decayTime, f),
                    lerp(a.reflectionsGain, b.reflectionsGain, f),
                    lerp(a.reflectionsDelay, b.reflectionsDelay, f),
                    lerp(a.lateReverbDelay, b.lateReverbDelay, f),
                    lerp(a.airAbsorption, b.airAbsorption, f)
            );
        }

        private static float lerp(float a, float b, float f) {
            return a + (b - a) * f;
        }
    }

    private @NonNull ReverbType currentFrom = ReverbType.NONE;
    private @NonNull ReverbType currentTo = ReverbType.NONE;
    private float currentFactor = -1f;

    public void init(long device) {
        if (!EFXManager.isEfxSupported(device)) {
            logger.warning("OpenAL EFX extension not supported. Environmental audio disabled.");
            supported = false;
            return;
        }

        try {
            // Create Auxiliary Effect Slot
            effectSlot = alGenAuxiliaryEffectSlots();
            OpenALManager.checkALError("alGenAuxiliaryEffectSlots");

            // Create Effect
            reverbEffect = alGenEffects();
            OpenALManager.checkALError("alGenEffects");

            // Configure slot
            alAuxiliaryEffectSloti(effectSlot, AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, 1);
            OpenALManager.checkALError("alAuxiliaryEffectSloti SEND_AUTO");

            supported = true;
            logger.info("OpenAL EFX initialized successfully.");
        } catch (Exception e) {
            logger.severe("Failed to initialize OpenAL EFX: " + e.getMessage());
            supported = false;
        }
    }

    public void setReverb(@NonNull ReverbType type) {
        setReverb(type, type, 1.0f);
    }

    /**
     * Blends between two reverb environments.
     *
     * @param from The source reverb environment.
     * @param to The target reverb environment.
     * @param factor Blending factor [0.0 - 1.0]. 0.0 is fully 'from', 1.0 is fully 'to'.
     */
    public void setReverb(@NonNull ReverbType from, @NonNull ReverbType to, float factor) {
        if (!supported || ALC10.alcGetCurrentContext() == 0) return;
        // Small epsilon for factor comparison to avoid redundant GL updates
        if (from == currentFrom && to == currentTo && Math.abs(factor - currentFactor) < 0.005f) return;

        currentFrom = from;
        currentTo = to;
        currentFactor = factor;

        if (from == ReverbType.NONE && to == ReverbType.NONE) {
            alAuxiliaryEffectSloti(effectSlot, AL_EFFECTSLOT_EFFECT, AL_EFFECT_NULL);
            return;
        }

        alEffecti(reverbEffect, AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
        // We don't check errors for every param for performance, but we check here.
        OpenALManager.checkALError("alEffecti AL_EFFECT_TYPE");

        ReverbSnapshot sFrom = ReverbSnapshot.get(from);
        ReverbSnapshot sTo = ReverbSnapshot.get(to);
        ReverbSnapshot blended = ReverbSnapshot.blend(sFrom, sTo, factor);

        applySnapshot(blended);

        // Bind effect to slot
        alAuxiliaryEffectSloti(effectSlot, AL_EFFECTSLOT_EFFECT, reverbEffect);
        OpenALManager.checkALError("alAuxiliaryEffectSloti AL_EFFECTSLOT_EFFECT");
    }

    private void applySnapshot(@NonNull ReverbSnapshot s) {
        if (ALC10.alcGetCurrentContext() == 0) return;
        alEffectf(reverbEffect, AL_EAXREVERB_DENSITY, s.density);
        alEffectf(reverbEffect, AL_EAXREVERB_DIFFUSION, s.diffusion);
        alEffectf(reverbEffect, AL_EAXREVERB_GAIN, s.gain);
        alEffectf(reverbEffect, AL_EAXREVERB_GAINHF, s.gainHF);
        alEffectf(reverbEffect, AL_EAXREVERB_DECAY_TIME, s.decayTime);
        alEffectf(reverbEffect, AL_EAXREVERB_REFLECTIONS_GAIN, s.reflectionsGain);
        alEffectf(reverbEffect, AL_EAXREVERB_REFLECTIONS_DELAY, s.reflectionsDelay);
        alEffectf(reverbEffect, AL_EAXREVERB_LATE_REVERB_DELAY, s.lateReverbDelay);
        alEffectf(reverbEffect, AL_EAXREVERB_AIR_ABSORPTION_GAINHF, s.airAbsorption);
    }

    public int getEffectSlot() {
        return effectSlot;
    }

    public boolean isSupported() {
        return supported;
    }

    @Override
    public void close() {
        if (supported && ALC10.alcGetCurrentContext() != 0) {
            alDeleteEffects(reverbEffect);
            alDeleteAuxiliaryEffectSlots(effectSlot);
        }
    }

    /**
     * Checks if the OpenAL EFX extension is supported.
     *
     * @param device The OpenAL device.
     * @return True if EFX is supported, false otherwise.
     */
    public static boolean isEfxSupported(long device) {
        ALCCapabilities caps = ALC.createCapabilities(device);
        return caps.ALC_EXT_EFX;
    }
}

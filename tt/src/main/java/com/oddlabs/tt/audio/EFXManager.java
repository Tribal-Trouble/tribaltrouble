package com.oddlabs.tt.audio;

import com.oddlabs.tt.audio.openal.OpenALManager;
import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.ALC;
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

public final class EFXManager {
    private static final Logger logger = Logger.getLogger(EFXManager.class.getName());

    private int effectSlot;
    private int reverbEffect;
    private boolean supported = false;

    // Reverb Presets
    public enum ReverbType {
        NONE,
        GENERIC,    // General outdoors
        FOREST,     // Dampened
        VALLEY,     // Echoey
        UNDERWATER  // Muffled
    }

    private @NonNull ReverbType currentType = ReverbType.NONE;

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
        if (!supported || type == currentType) return;

        currentType = type;

        if (type == ReverbType.NONE) {
            alAuxiliaryEffectSloti(effectSlot, AL_EFFECTSLOT_EFFECT, AL_EFFECT_NULL);
            return;
        }

        alEffecti(reverbEffect, AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
        OpenALManager.checkALError("alEffecti AL_EFFECT_TYPE");

        // Apply presets (Tuned EAX Reverb parameters with Linear Gain [0.0 - 1.0])
        switch (type) {
            case NONE -> {
                // Already handled above, but exhaustive switch required.
            }
            case GENERIC -> // Open Plains: Very light reverb, short decay
                    setEAXReverb(0.1f, 0.1f, 0.1f, 0.8f, 0.5f, 0.1f, 0.02f, 0.0f, 0.994f);
            case FOREST -> // Forest: Absorptive, very short decay, dampened
                    setEAXReverb(0.1f, 0.1f, 0.15f, 0.2f, 0.4f, 0.1f, 0.02f, 0.0f, 0.9f);
            case VALLEY -> // Valley: Echoey but not overwhelming
                    setEAXReverb(0.2f, 0.3f, 0.4f, 0.5f, 1.2f, 0.3f, 0.1f, 0.1f, 0.994f);
            case UNDERWATER -> // Underwater: Muffled, boomy
                    setEAXReverb(1.0f, 0.1f, 0.8f, 0.1f, 1.2f, 0.01f, 0.01f, 0.0f, 0.9f);
        }

        // Bind effect to slot
        alAuxiliaryEffectSloti(effectSlot, AL_EFFECTSLOT_EFFECT, reverbEffect);
        OpenALManager.checkALError("alAuxiliaryEffectSloti AL_EFFECTSLOT_EFFECT");
    }

    // Helper to set EAX Reverb properties (simplified subset)
    private void setEAXReverb(float density, float diffusion, float gain, float gainHF, float decayTime, float reflectionsGain, float reflectionsDelay, float lateReverbDelay, float airAbsorption) {
        alEffectf(reverbEffect, AL_EAXREVERB_DENSITY, density);
        alEffectf(reverbEffect, AL_EAXREVERB_DIFFUSION, diffusion);
        alEffectf(reverbEffect, AL_EAXREVERB_GAIN, gain);
        alEffectf(reverbEffect, AL_EAXREVERB_GAINHF, gainHF);
        alEffectf(reverbEffect, AL_EAXREVERB_DECAY_TIME, decayTime);
        alEffectf(reverbEffect, AL_EAXREVERB_REFLECTIONS_GAIN, reflectionsGain);
        alEffectf(reverbEffect, AL_EAXREVERB_REFLECTIONS_DELAY, reflectionsDelay);
        alEffectf(reverbEffect, AL_EAXREVERB_LATE_REVERB_DELAY, lateReverbDelay);
        alEffectf(reverbEffect, AL_EAXREVERB_AIR_ABSORPTION_GAINHF, airAbsorption);
        // ... set other params as needed
        OpenALManager.checkALError("setEAXReverb");
    }

    public int getEffectSlot() {
        return effectSlot;
    }

    public boolean isSupported() {
        return supported;
    }

    public void cleanup() {
        if (supported) {
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

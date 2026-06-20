package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.particle.RandomAccelerationEmitter;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An accessory that manages the chimney smoke during building production.
 */
public final class BuildingProductionAccessory implements EmitterAccessory {
    private static final float EMITTER_ENERGY = 5.0f;
    private static final float EMITTER_ALPHA = 0.75f;
    private static final float GO_IDLE_DURATION = 1.0f;
    private static final float IDLE_STOP_THRESHOLD = 2.5f;
    private static final float WARMUP_DURATION = 1.5f;
    private static final float WARM_DURATION = 4.0f;

    private static final Color.Linear START_DARK = new Color.Linear(0.3f, 1.0f);
    private static final Color.Linear END_BASE = Color.Linear.WHITE;
    private static final Color.Linear STARTUP_BROWN = new Color.Linear(0.8f, 0.4f, 0.1f, 1.0f);
    private static final Color.Linear PRODUCTION_BLUE = new Color.Linear(1.5f, 1.6f, 1.9f, 1.0f);
    private static final Color.Linear IDLE_GREY = new Color.Linear(2.0f, 1.0f);

    private enum State {
        IDLE,
        PRODUCING,
        GOING_IDLE
    }

    private final @NonNull Building building;
    private final @NonNull LinearEmitter emitter;
    private final @NonNull Vector3fc chimneyOffset;

    private @NonNull State state = State.IDLE;
    private float productionTimer = 0f;
    private float idleTimer = 0f;
    private float goingIdleTimer = 0f;
    private @Nullable AudioPlayer productionPlayer = null;

    public BuildingProductionAccessory(@NonNull Building building) {
        this.building = building;
        this.chimneyOffset = building.getTemplate().getChimney();
        this.emitter = new RandomAccelerationEmitter(building.getOwner().getWorld(), new Vector3f(0f, 0f, 0f),
                0f, 0.15f, 0.05f, 1.5f, 0.1f, -1, 15.0f,
                new Vector3f(0.15f, 0.05f, 0.5f), new Vector3f(0f, 0f, 1.3f), 0.4f,
                new Color.Linear(1.0f, 1.0f, 1.0f, EMITTER_ALPHA),
                Color.LinearDelta.ZERO.alpha(-EMITTER_ALPHA / EMITTER_ENERGY),
                new Vector3f(0.3f, 0.3f, 0.3f), new Vector3f(0.5f, 0.5f, 0.6f), EMITTER_ENERGY, 0.7f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                VisualRegistry.getInstance().getSmokeTextures());
        this.emitter.stop();
        this.emitter.setBaseColor(new Color.Linear(0.0992f, 0.0992f, 0.0992f, EMITTER_ALPHA));
        this.emitter.setSpectrumRange(0.0f, 1.0f);
        this.emitter.setJitterIntensity(0.08f);

        this.emitter.setColorSpectrum((spectrum, baseColor) -> {
            Random rand = ThreadLocalRandom.current();
            Color.Linear factor;
            float alpha;

            // Phase 1: Startup transition (spectrum 0.0 to 0.5)
            // Gradually transitions from startup soot/orange-brown to standard production grey/blue.
            if (spectrum < 0.5f) {
                float t = spectrum / 0.5f;
                alpha = 1.0f - 0.3f * t; // Alpha fades slightly from 1.0 to 0.7
                factor = rand.nextFloat() < 0.65f
                        ? START_DARK.lerp(END_BASE, t)
                        : STARTUP_BROWN.lerp(PRODUCTION_BLUE, t);
            } else {
                // Phase 2: Going Idle transition (spectrum 0.5 to 1.0)
                // Gradually transitions from production grey/blue to light idle grey before disappearing.
                float t = (spectrum - 0.5f) / 0.5f;
                alpha = 0.7f - 0.3f * t; // Alpha fades from 0.7 to 0.4
                factor = rand.nextFloat() < 0.65f
                        ? END_BASE.lerp(IDLE_GREY, t)
                        : PRODUCTION_BLUE.lerp(IDLE_GREY, t);
            }
            return baseColor.mul(factor).alpha(alpha);
        });
    }

    @Override
    public void animate(float t) {
        boolean isProducing = building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)
                && building.isAlive() && building.isProducing();

        if (isProducing) {
            if (productionPlayer == null) {
                productionPlayer = building.getOwner().getWorld().getAudio().newAudio(
                        building.getPositionX(), building.getPositionY(), building.getPositionZ(),
                        AudioAssets.WEAPONS_PRODUCTION);
            }
        } else {
            if (productionPlayer != null) {
                productionPlayer.stop();
                productionPlayer = null;
            }
        }

        if (!building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
            state = State.IDLE;
            productionTimer = 0f;
            emitter.stop();
            if (emitter.hasActiveParticles()) {
                emitter.animate(t);
            }
            return;
        }

        if (isProducing) {
            idleTimer = 0f;
            goingIdleTimer = 0f;
            state = State.PRODUCING;
            emitter.start();
            productionTimer = Math.min(1.0f, productionTimer + t / WARMUP_DURATION);
        } else {
            if (state == State.PRODUCING) {
                idleTimer += t;
                if (idleTimer >= IDLE_STOP_THRESHOLD) {
                    state = State.GOING_IDLE;
                    goingIdleTimer = 0f;
                }
            } else if (state == State.GOING_IDLE) {
                goingIdleTimer += t;
                if (goingIdleTimer >= GO_IDLE_DURATION) {
                    state = State.IDLE;
                    emitter.stop();
                }
            } else if (state == State.IDLE) {
                productionTimer = Math.max(0f, productionTimer - t / WARM_DURATION);
            }
        }

        float spectrum = 0.5f;
        if (state == State.PRODUCING) {
            spectrum = productionTimer * 0.5f;
        } else if (state == State.GOING_IDLE) {
            spectrum = 0.5f + (goingIdleTimer / GO_IDLE_DURATION) * 0.5f;
        }
        emitter.setSpectrum(spectrum);

        if (state != State.IDLE || emitter.hasActiveParticles()) {
            emitter.animate(t);
        }
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        if (parent instanceof Building b) {
            return b.getAbilities().hasAbilities(Abilities.BUILD_ARMIES) &&
                    (state != State.IDLE || !emitter.isFinished());
        }
        return false;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
        dest.translate(chimneyOffset);
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public @NonNull LinearEmitter getEmitter() {
        return emitter;
    }

    @Override
    public void close() {
        if (productionPlayer != null) {
            productionPlayer.stop();
            productionPlayer = null;
        }
    }
}

package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.particle.RingEmitter;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Client-side visual accessory for iron supply meteors.
 * Manages the falling trail, impact effects, and cooling smoke entirely on the client.
 */
public final class IronSupplyVisualAccessory implements AnimatedAccessory {
    private static final float FALL_DURATION_RATIO = 0.12f;
    private static final float TRAIL_OFFSET_Z = 10.0f;
    private static final float SMOKE_PARTICLES_PER_SECOND = 30.0f;

    private static final Color.Linear COLOR_WHITE_HOT = new Color.Linear(2.0f, 2.0f, 2.0f, 1.0f);

    private final @NonNull IronSupply ironSupply;

    private boolean landed = false;
    private boolean cooling = false;
    private boolean airBurstPlayed = false;
    private @Nullable RandomVelocityEmitter trailEmitter = null;
    private @Nullable RandomVelocityEmitter coolingEmitter = null;
    private final @NonNull List<@NonNull Emitter<?>> oneShotEmitters = new ArrayList<>();

    public IronSupplyVisualAccessory(@NonNull IronSupply ironSupply) {
        this.ironSupply = ironSupply;
    }

    @Override
    public void animate(float t) {
        if (ironSupply.isSpawning()) {
            float progress = ironSupply.getSpawnProgress();
            if (progress < FALL_DURATION_RATIO) {
                // falling
                float fallProgress = progress / FALL_DURATION_RATIO;
                var emitter = ensureTrailEmitter();

                // Dynamic Parameters:
                // PPS: Scaling 800 to 2400 (Tripled at landing)
                float pps = 800f + 1600f * fallProgress;
                emitter.setParticlesPerSecond(pps);

                // Size/Stretch:
                // Start: 1.6 X/Z, 2.0 Y (Double size and moderate stretch)
                // Landing: 0.8 X/Z, 0.75 Y (Baseline size, halved stretch)
                float radiusXZ = 1.6f - 0.8f * fallProgress;
                float radiusY = 2.0f - 1.25f * fallProgress;
                emitter.setParticleRadius(radiusXZ, radiusY, radiusXZ);

                // Growth scales proportionally
                float growthXZ = 2.4f - 1.2f * fallProgress;
                float growthY = 3.0f - 2.25f * fallProgress;
                emitter.setGrowthRate(growthXZ, growthY, growthXZ);

                if (!airBurstPlayed) {
                    airBurstPlayed = true;
                    ironSupply.getWorld().getAudio().newAudio(ironSupply.getPositionX(), ironSupply.getPositionY(),
                            ironSupply.getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_LURBLAST, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.8f
                            ));
                }

                emitter.getPosition().set(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                        .getPositionZ()
                        + TRAIL_OFFSET_Z);
                emitter.animate(t);
            } else {
                if (trailEmitter != null) {
                    trailEmitter.done();
                    trailEmitter = null;
                }

                if (!landed) {
                    landed = true;

                    Vector3f landingPos = new Vector3f(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                            .getPositionZ() + 0.15f);
                    World world = ironSupply.getWorld();

                    // Bright impact flash (additive)
                    RingEmitter flash = new RingEmitter(
                            world, landingPos, 0.0f,
                            0.0f, 0.0f,
                            1, 1000f,
                            new Vector3f(0f, 0f, 0.0f),
                            new Vector3f(0f, 0f, 0.0f),
                            COLOR_WHITE_HOT, Color.LinearDelta.ZERO.alpha(-10.0f), // Very fast fade
                            new Vector3f(10.0f, 10.0f, 10.0f),
                            new Vector3f(0.0f, 0.0f, 0.0f),
                            0.1f, 0.0f,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE,
                            VisualRegistry.getInstance().getSmokeTextures()
                    );
                    oneShotEmitters.add(flash);

                    // Primary energetic dust puff (high speed, explosive)
                    RingEmitter puff = new RingEmitter(
                            world, landingPos, 0.0f,
                            0.5f, 0.0f, // Lowered emitter height
                            48, 10f,
                            new Vector3f(0f, 0f, 40.0f), // Balanced expansion
                            new Vector3f(0f, 0f, 0.0f), // No lift
                            Color.Linear.WHITE.alpha(0.30f), Color.LinearDelta.ZERO,
                            new Vector3f(1.0f, 1.0f, 0.0f), // Flat (radius.z)
                            new Vector3f(15.0f, 15.0f, 0.0f), // Flat (growth.z)
                            0.6f, 0.1f, // Shorter energy
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            VisualRegistry.getInstance().getSmokeTextures()
                    );

                    puff.setSpectrumRange(0.0f, 0.9f);
                    puff.setBaseColor(Landscape.getDustColor(world.getTerrainType()));
                    puff.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum * 0.5f));
                    puff.setTransition(0.1f, 0.1f, 0.0f, 0.5f);
                    puff.setJitterIntensity(0.15f);

                    oneShotEmitters.add(puff);

                    // Secondary lingering debris cloud (ground sweeping)
                    RingEmitter debris = new RingEmitter(
                            world, landingPos, 0.0f,
                            0.5f, 0.0f, // Halved emitter radius
                            64, 10f,
                            new Vector3f(0f, 0f, 6.0f), // Halved radial expansion speed
                            new Vector3f(0f, 0f, 0.0f), // No lift
                            Color.Linear.WHITE.alpha(0.15f), Color.LinearDelta.ZERO,
                            new Vector3f(1.0f, 0.2f, 0.0f), // Halved particle radius
                            new Vector3f(4.0f, 0.25f, 0.0f), // Halved growth rate
                            1.0f, 0.45f, // Halved lifetime/energy (adjusts fadeout)
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            VisualRegistry.getInstance().getSmokeTextures()
                    );
                    debris.setBaseColor(Landscape.getDustColor(world.getTerrainType()));
                    debris.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum
                            * 0.4f));
                    debris.setSpectrumRange(0.1f, 0.9f);
                    debris.setJitterIntensity(0.20f);
                    oneShotEmitters.add(debris);
                }

                float coolProgress = Math.min(1.0f, (progress - FALL_DURATION_RATIO) / (0.85f - FALL_DURATION_RATIO));

                if (!cooling) {
                    cooling = true;
                    ironSupply.getWorld().getAudio().newAudio(ironSupply.getPositionX(), ironSupply.getPositionY(),
                            ironSupply.getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_GAS, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION, 0.65f
                            )).stop(1.3f);
                }

                var emitter = ensureCoolingEmitter();
                emitter.getPosition().set(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                        .getPositionZ());
                emitter.setParticlesPerSecond(SMOKE_PARTICLES_PER_SECOND * (1.0f - coolProgress));
                emitter.animate(t);
            }
        } else {
            cleanupEmitters();
        }

        oneShotEmitters.forEach(e -> e.animate(t));
        oneShotEmitters.removeIf(Emitter::isFinished);
    }

    private void cleanupEmitters() {
        landed = false;
        cooling = false;
        airBurstPlayed = false;
        if (trailEmitter != null) {
            trailEmitter.done();
            trailEmitter = null;
        }
        if (coolingEmitter != null) {
            coolingEmitter.done();
            coolingEmitter = null;
        }
        oneShotEmitters.clear();
    }

    private RandomVelocityEmitter ensureTrailEmitter() {
        if (trailEmitter == null) {
            World world = ironSupply.getWorld();
            Vector3f pos = new Vector3f(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply.getPositionZ()
                    + TRAIL_OFFSET_Z);
            trailEmitter = new RandomVelocityEmitter(
                    world, pos, 0.0f, 0.0f,
                    0.02f, 5.0f,
                    0.1f, 0.02f,
                    -1, 800f,
                    new Vector3f(0f, 0f, 10.0f), new Vector3f(0f, 0f, 5.0f),
                    new Color.Linear(0.08f, 0.6f), new Color.LinearDelta(0f, -0.6f),
                    new Vector3f(1.6f, 2.0f, 1.6f), new Vector3f(2.4f, 3.0f, 2.4f),
                    1.2f, 0.1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    VisualRegistry.getInstance().getSmokeTextures(),
                    null, VisualRegistry.getInstance().getSmokeTextures().length,
                    true, true
            );
            trailEmitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            trailEmitter.setSpectrumRange(0.2f, 0.2f);
            trailEmitter.setJitterIntensity(0.5f);
        }
        return trailEmitter;
    }

    private RandomVelocityEmitter ensureCoolingEmitter() {
        if (coolingEmitter == null) {
            World world = ironSupply.getWorld();
            Vector3f pos = new Vector3f(ironSupply.getPositionX(), ironSupply.getPositionY(), ironSupply
                    .getPositionZ());
            coolingEmitter = new RandomVelocityEmitter(
                    world, pos, 0.0f, 0.0f,
                    ironSupply.getSize() * 0.5f, 0.1f, 0.2f, 0.1f,
                    -1, SMOKE_PARTICLES_PER_SECOND,
                    new Vector3f(0f, 0f, 1.5f), new Vector3f(0f, 0f, -0.3f),
                    new Color.Linear(0.15f, 0.5f), new Color.LinearDelta(0f, -0.4f),
                    new Vector3f(0.8f, 0.8f, 0.8f), new Vector3f(2.5f, 2.5f, 2.5f),
                    1.2f, 0.1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    VisualRegistry.getInstance().getSmokeTextures()
            );
            coolingEmitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            coolingEmitter.setSpectrumRange(0.2f, 0.8f);
            coolingEmitter.setJitterIntensity(0.02f);
            coolingEmitter.setTransition(0f, 3.0f, 0.8f, 0f);
        }
        return coolingEmitter;
    }

    @Override
    public void addEmitters(@NonNull Collection<@NonNull Emitter<?>> dest) {
        if (trailEmitter != null) {
            dest.add(trailEmitter);
        }
        if (coolingEmitter != null) {
            dest.add(coolingEmitter);
        }
        dest.addAll(oneShotEmitters);
    }

    @Override
    public boolean isExpired() {
        return ironSupply.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !ironSupply.isDead();
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public void close() {
        cleanupEmitters();
    }
}

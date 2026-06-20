package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Collection;

/**
 * Client-side visual accessory for rock supply eruptions.
 * Manages the rising eruption smoke entirely on the client.
 */
public final class RockSupplyVisualAccessory implements AnimatedAccessory {
    private final @NonNull RockSupply rockSupply;
    private @Nullable RandomVelocityEmitter smokeEmitter = null;
    private boolean soundPlayed = false;

    public RockSupplyVisualAccessory(@NonNull RockSupply rockSupply) {
        this.rockSupply = rockSupply;
    }

    @Override
    public void animate(float t) {
        if (rockSupply.isSpawning()) {
            float progress = rockSupply.getSpawnProgress();
            if (progress < 0.3f) {
                ensureSmokeEmitter().setTransition(0.0f, 1.8f, 0.2f, 0.3f);
                if (!soundPlayed) {
                    soundPlayed = true;
                    rockSupply.getWorld().getAudio().newAudio(rockSupply.getPositionX(), rockSupply.getPositionY(),
                            rockSupply.getPositionZ(),
                            new AudioParameters(AudioAssets.SFX_RUMBLE, AudioAssets.AUDIO_RANK_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_DISTANCE_SUPPLY_ACTION, AudioAssets.AUDIO_GAIN_SUPPLY_ACTION,
                                    AudioAssets.AUDIO_RADIUS_SUPPLY_ACTION));
                }
            } else if (progress < 0.7f) {
                if (progress > 0.65f) {
                    ensureSmokeEmitter().setTransition(0.0f, 0.5f, 1.0f, 1.0f);
                }
            } else {
                cleanupEmitters();
            }

            if (smokeEmitter != null) {
                smokeEmitter.getPosition().set(rockSupply.getPositionX(), rockSupply.getPositionY(), rockSupply
                        .getPositionZ());
                smokeEmitter.animate(t);
            }
        } else {
            cleanupEmitters();
            soundPlayed = false;
        }
    }

    private void cleanupEmitters() {
        if (smokeEmitter != null) {
            smokeEmitter.done();
            smokeEmitter = null;
        }
    }

    private RandomVelocityEmitter ensureSmokeEmitter() {
        if (smokeEmitter == null) {
            World world = rockSupply.getWorld();
            Vector3f pos = new Vector3f(rockSupply.getPositionX(), rockSupply.getPositionY(), rockSupply
                    .getPositionZ());
            smokeEmitter = new RandomVelocityEmitter(
                    world, pos, 0.0f, 0.0f,
                    rockSupply.getSize() * 0.4f, 0.1f, 0.2f, 0.1f,
                    -1, 15.0f,
                    new Vector3f(0f, 0f, 2.0f), new Vector3f(0f, 0f, -0.5f),
                    new Color.Linear(0.1f, 0.75f), new Color.LinearDelta(0f, -0.6f),
                    new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(1.0f, 1.0f, 1.0f),
                    1.0f, 0.2f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    VisualRegistry.getInstance().getSmokeTextures()
            );
            smokeEmitter.setColorSpectrum((spectrum, baseColor) -> baseColor.lerp(Color.Linear.BLACK, spectrum));
            smokeEmitter.setSpectrumRange(0.2f, 1.0f);
        }
        return smokeEmitter;
    }

    @Override
    public void addEmitters(@NonNull Collection<@NonNull Emitter<?>> dest) {
        if (smokeEmitter != null) {
            dest.add(smokeEmitter);
        }
    }

    @Override
    public boolean isExpired() {
        return rockSupply.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !rockSupply.isDead();
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

package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.weapon.LightningCloud;
import com.oddlabs.tt.particle.CloudFunction;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.ParametricEmitter;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/**
 * Client-side visual accessory for the lightning cloud magical effect.
 * Manages the persistent cloud puffiness and handles lightning strike visual/sound effects.
 */
public final class LightningCloudVisualAccessory implements EmitterAccessory {
    private static final float BRIGHTNESS = Color.toLinear(.2f);
    private static final Color.LinearDelta BRIGHTNESS_DELTA = new Color.LinearDelta(BRIGHTNESS, 0);
    private static final float LIGHTNING_TIME = .1f;
    private static final Color.LinearDelta DELTA_COLOR = Color.LinearDelta.ZERO.alpha(-1f / LIGHTNING_TIME);

    private static final float CLOUD_RADIUS_XY = 2.5f;
    private static final float CLOUD_RADIUS_Z = 0.7f;
    private static final float PARTICLE_RADIUS_XY = 2.0f;
    private static final float PARTICLE_RADIUS_Z = 0.7f;
    private static final float LIGHTING_INTENSITY = 0.10f;

    private final @NonNull LightningCloud cloud;
    private final @NonNull ParametricEmitter emitter;
    private final @NonNull AudioPlayer bubblingSound;
    private @Nullable AudioPlayer cloudSound;

    private float lightningTimer = 0f;
    private boolean lighted = false;
    private boolean firstRun = true;
    private float strikeAudioCooldown = 0f;

    public LightningCloudVisualAccessory(@NonNull LightningCloud cloud) {
        this.cloud = cloud;
        World world = cloud.getWorld();
        Vector3f pos = new Vector3f(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());

        float energy = cloud.getSecondsToLive() + cloud.getSecondsToInit();
        var function = new CloudFunction(CLOUD_RADIUS_XY, CLOUD_RADIUS_Z);
        this.emitter = new ParametricEmitter(world, function, pos,
                0f, cloud.getCloudOffsetZ(), .5f, .5f, .2f,
                45, 150f,
                Color.Linear.WHITE.alpha(0.8f), Color.LinearDelta.ZERO,
                new Vector3f(PARTICLE_RADIUS_XY, PARTICLE_RADIUS_XY, PARTICLE_RADIUS_Z), new Vector3f(0f, 0f, 0f),
                energy,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                VisualRegistry.getInstance().getSmokeTextures());

        this.emitter.setBaseColor(new Color.Standard(.3f, 1f).linear());
        this.emitter.setColorSpectrum((spectrum, baseColor) -> baseColor);
        this.emitter.setFogEnabled(false);
        this.emitter.setJitterIntensity(0.05f);
        this.emitter.setRandomizeScale(true);

        float maxLocalZ = CLOUD_RADIUS_Z * CloudFunction.TOP_PUFFINESS_PEAK;
        this.emitter.setHeightLighting(LIGHTING_INTENSITY, maxLocalZ);

        this.bubblingSound = world.getAudio().newAudio(cloud.getPositionX(), cloud.getPositionY(),
                world.getHeightMap().getNearestHeight(cloud.getPositionX(), cloud.getPositionY()),
                AudioAssets.BUBBLING);
    }

    public void triggerStrike(float tx, float ty, float tz) {
        if (lightningTimer <= 0f) {
            emitter.adjustColor(BRIGHTNESS_DELTA);
            lightningTimer = LIGHTNING_TIME;
            lighted = true;

            if (strikeAudioCooldown <= 0f) {
                var params = new AudioParameters(
                        AudioAssets.SFX_FLASH, AudioAssets.AUDIO_RANK_MAGIC,
                        AudioAssets.AUDIO_DISTANCE_MAGIC, AudioAssets.AUDIO_GAIN_LIGHTNING,
                        AudioAssets.AUDIO_RADIUS_LIGHTNING);
                cloud.getWorld().getAudio().newAudio(tx, ty, tz, params);
                strikeAudioCooldown = 0.8f;
            }
        }

        Vector3f cloudPos = new Vector3f(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());
        Lightning lightning = new Lightning(cloud.getWorld(), cloudPos, new Vector3f(tx, ty, tz), .5f,
                15, Color.Linear.WHITE, DELTA_COLOR,
                VisualRegistry.getInstance().getLightningTexture(), LIGHTNING_TIME,
                cloud.getWorld().getAnimationManagerGameTime());
        lightning.register();
    }

    @Override
    public void animate(float t) {
        if (firstRun) {
            cloudSound = cloud.getWorld().getAudio().newAudio(cloud.getPositionX(), cloud.getPositionY(), cloud
                    .getPositionZ(),
                    AudioAssets.LIGHTNING_CLOUD);
            firstRun = false;
            bubblingSound.stop(15.0f);
        }
        if (cloudSound != null) {
            cloudSound.setPosition(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());
        }
        if (strikeAudioCooldown > 0f) {
            strikeAudioCooldown -= t;
        }

        // Update emitter position
        emitter.getPosition().set(cloud.getPositionX(), cloud.getPositionY(), cloud.getPositionZ());

        // Handle cloud fade out in the last 2 seconds
        float secondsToLive = cloud.getSecondsToLive();
        if (secondsToLive <= 2.0f) {
            emitter.adjustColor(new Color.LinearDelta(0f, -0.8f * t / 2.0f));
        }

        // Handle lightning strike cloud flash dimming
        if (lighted) {
            lightningTimer -= t;
            if (lightningTimer <= 0) {
                emitter.adjustColor(BRIGHTNESS_DELTA.negate());
                lighted = false;
            }
        }

        emitter.animate(t);
    }

    @Override
    public @NonNull Emitter<?> getEmitter() {
        return emitter;
    }

    @Override
    public boolean isExpired() {
        return cloud.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !cloud.isDead();
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
        if (bubblingSound != null) {
            bubblingSound.stop(15.0f);
        }
        if (cloudSound != null) {
            cloudSound.stop(15.0f);
            cloudSound = null;
        }
    }
}

package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.PointEmitterModel;
import com.oddlabs.tt.model.weapon.PoisonFog;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side visual accessory for the poison fog magical effect.
 * Periodically spawns poison gas puffs inside the fog region on the client.
 */
public final class PoisonFogVisualAccessory implements AnimatedAccessory {
    private static final int PARTICLES_PER_BURST = 4;
    private static final float SECONDS_BETWEEN_BURSTS = .15f;
    private static final float BURST_RADIUS = 2f;
    private static final float GAUSSIAN_LIMIT = 2.5f;
    private static final int MIN_BURSTS_PER_SOUND = 2;

    private final @NonNull PoisonFog poisonFog;
    private final @NonNull AudioPlayer bubblingSound;

    private float time = 0f;
    private int bursts = 0;
    private boolean firstRun = true;
    private int nextSound = 1;

    public PoisonFogVisualAccessory(@NonNull PoisonFog poisonFog) {
        this.poisonFog = poisonFog;
        World world = poisonFog.getWorld();
        this.bubblingSound = world.getAudio().newAudio(poisonFog.getPositionX(), poisonFog.getPositionY(),
                world.getHeightMap().getNearestHeight(poisonFog.getPositionX(), poisonFog.getPositionY()),
                AudioAssets.BUBBLING);
    }

    @Override
    public void animate(float t) {
        if (firstRun) {
            bubblingSound.stop(15.0f);
            firstRun = false;
        }

        time += t;
        World world = poisonFog.getWorld();
        Random random = world.getRandom();

        if (bursts * SECONDS_BETWEEN_BURSTS < time) {
            float gaussian = (float) (GAUSSIAN_LIMIT - Math.abs(Math.clamp(random.nextGaussian(),
                    -GAUSSIAN_LIMIT, GAUSSIAN_LIMIT))) / GAUSSIAN_LIMIT;
            float r = gaussian * (poisonFog.getHitRadius() - BURST_RADIUS - 5f);
            float a = random.nextFloat(0f, (float) Math.PI * 2);
            float x = poisonFog.getPositionX() + (float) Math.cos(a) * r;
            float y = poisonFog.getPositionY() + (float) Math.sin(a) * r;
            float z = world.getHeightMap().getNearestHeight(x, y);
            float alpha = 8f;
            float energy = 2f;

            RandomVelocityEmitter emitter = new RandomVelocityEmitter(world, new Vector3f(x, y, z), poisonFog
                    .getCloudOffsetZ(),
                    random.nextFloat(0f, (float) Math.PI * 2),
                    BURST_RADIUS, 0f, 0f, 0f,
                    PARTICLES_PER_BURST, PARTICLES_PER_BURST,
                    new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f),
                    new Color.Linear(1f, 1f, 1f, alpha), new Color.LinearDelta(0f, 0f, 0f, -alpha / energy),
                    new Vector3f(0f, 0f, .25f), new Vector3f(3.5f, 3.5f, 0f), energy, 1f,
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    VisualRegistry.getInstance().getPoisonTextures());
            new PointEmitterModel(world, emitter);

            if (bursts % nextSound == 0) {
                nextSound = MIN_BURSTS_PER_SOUND + ThreadLocalRandom.current().nextInt(5);
                world.getAudio().newAudio(x, y, z, AudioAssets.POISON_GAS);
            }
            bursts++;
        }
    }

    @Override
    public boolean isExpired() {
        return poisonFog.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !poisonFog.isDead();
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
    }
}

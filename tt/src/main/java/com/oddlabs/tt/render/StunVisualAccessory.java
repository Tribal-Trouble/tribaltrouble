package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.weapon.Stun;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side visual accessory for the stun spell.
 * Spawns musical note particles entirely on the client.
 */
public final class StunVisualAccessory implements EmitterAccessory {
    private final @NonNull Stun stun;
    private final @NonNull RandomVelocityEmitter emitter;
    private final @NonNull AudioPlayer sound;

    private float age = 0f;

    public StunVisualAccessory(@NonNull Stun stun) {
        this.stun = stun;
        World world = stun.getWorld();
        Vector3f pos = new Vector3f(stun.getPositionX(), stun.getPositionY(), stun.getPositionZ());

        float alpha = 12f;
        float energy = 4f;
        this.emitter = new RandomVelocityEmitter(world, pos, 0f, 0f,
                .001f, .001f, .5f, (float) Math.PI,
                -1, 35f,
                new Vector3f(0f, 0f, 6f), new Vector3f(0f, 0f, -2f),
                new Color.Linear(1f, 1f, 1f, alpha), new Color.LinearDelta(0f, 0f, 0f, -alpha / energy),
                new Vector3f(.3f, .3f, .3f), new Vector3f(.025f, .025f, .025f), energy, 1f,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                VisualRegistry.getInstance().getNoteTextures());

        this.sound = world.getAudio().newAudio(stun.getPositionX(), stun.getPositionY(), stun.getPositionZ(),
                AudioAssets.STUN_LUR[ThreadLocalRandom.current().nextInt(AudioAssets.STUN_LUR.length)]);
    }

    @Override
    public void animate(float t) {
        emitter.getPosition().set(stun.getPositionX(), stun.getPositionY(), stun.getPositionZ());

        age += t;
        if (age > 1.5f) {
            emitter.done();
        }

        emitter.animate(t);
    }

    @Override
    public @NonNull Emitter<?> getEmitter() {
        return emitter;
    }

    @Override
    public boolean isExpired() {
        return stun.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !stun.isDead();
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
        if (sound != null) {
            sound.stop(10.0f);
        }
    }
}

package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An additive flame effect for torches. Draws only from its own position-seeded Random,
 * never the sim RNG, so it is safe on the real-time animation manager.
 */
public final class TorchEmitter extends LinearEmitter {
    private static final float ENERGY = 0.7f;
    private static final float ALPHA = 0.85f;
    private static final float GLOW_RADIUS = 2f;
    private static final float GLOW_ALPHA = 0.12f;
    private static final float GLOW_ENERGY = 0.5f;

    // Render-side registry of live torches so the renderer can turn them into point lights.
    private static final List<TorchEmitter> active_torches = new ArrayList<>();

    private static final float LIGHT_RADIUS = 16f;

    private final @NonNull Random random;
    private final float scale;
    private int spawn_counter = 0;

    public static @NonNull List<TorchEmitter> getActiveTorches() {
        return active_torches;
    }

    public static void purge(@NonNull World world) {
        active_torches.removeIf(torch -> torch.getWorld() != world);
    }

    public void dispose() {
        done();
        active_torches.remove(this);
    }

    public TorchEmitter(@NonNull World world, @NonNull Vector3f position, float scale,
            @NonNull TextureKey @NonNull [] textures, @NonNull AnimationManager manager) {
        super(world, position, 0f, 0.15f * scale, 0.2f * scale, -1, 30f, new Vector3f(0f, 0f,
                2.2f * scale), new Vector3f(0f, 0f, 1.8f * scale), new Vector4f(1f, 0.55f, 0.15f, ALPHA), new Vector4f(
                        -0.4f / ENERGY, -0.45f / ENERGY, -0.12f / ENERGY, -ALPHA / ENERGY), new Vector3f(scale, scale,
                                scale), new Vector3f(-0.8f * scale, -0.8f * scale,
                                        -0.8f * scale), ENERGY, 0f, GL11.GL_SRC_ALPHA, GL11.GL_ONE, textures, null, textures.length, manager);
        this.scale = scale;
        this.random = new Random((long) (position.x() * 31 + position.y() * 7 + position.z()));
        active_torches.add(this);
    }

    public float getLightRadius() {
        return LIGHT_RADIUS * scale;
    }

    @Override
    protected int initParticle(@NonNull Vector3f position, @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy) {
        LinearParticle particle = new LinearParticle(getWorld());
        Vector3f pos = randomPosition();
        particle.setPos(pos.x(), pos.y(), pos.z());
        if (++spawn_counter % 4 == 0) {
            // A stationary, large, faint halo layered under the flame makes the torch read as a light source.
            float flicker = 0.8f + random.nextFloat() * 0.2f;
            particle.setVelocity(0f, 0f, 0f);
            particle.setAcceleration(0f, 0f, 0f);
            particle.setColor(1f * flicker, 0.6f * flicker, 0.25f * flicker, GLOW_ALPHA);
            particle.setDeltaColor(0f, 0f, 0f, -GLOW_ALPHA / GLOW_ENERGY);
            particle.setRadius(GLOW_RADIUS * scale, GLOW_RADIUS * scale, GLOW_RADIUS * scale);
            particle.setGrowthRate(0f, 0f, 0f);
            particle.setEnergy(GLOW_ENERGY);
        } else {
            particle.setVelocity(velocity.x() + (random.nextFloat() - 0.5f) * 0.5f,
                    velocity.y() + (random.nextFloat() - 0.5f) * 0.5f,
                    velocity.z() * (0.8f + random.nextFloat() * 0.4f));
            particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
            float flicker = 0.85f + random.nextFloat() * 0.15f;
            particle.setColor(color.x() * flicker, color.y() * flicker, color.z() * flicker, color.w());
            particle.setDeltaColor(delta_color.x(), delta_color.y(), delta_color.z(), delta_color.w());
            particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
            particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
            particle.setEnergy(energy * (0.7f + random.nextFloat() * 0.6f));
        }
        particle.setType(random.nextInt(getTypes()));
        add(particle);
        return 1;
    }
}

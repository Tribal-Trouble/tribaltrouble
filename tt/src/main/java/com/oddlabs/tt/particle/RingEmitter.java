package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A linear emitter that spawns particles in a ring formation.
 */
public final class RingEmitter extends LinearEmitter {
    private final int num_particles;

    /**
     * Constructs a new RingEmitter that spawns particles in a radial ring formation.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param num_particles number of particles to spawn in a single ring burst
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity base initial velocity vector; velocity.z is the radial velocity magnitude
     * @param acceleration acceleration vector applied to particles
     * @param color base color of the particles
     * @param delta_color color delta applied to particles per second
     * @param particle_radius initial 3D size radius of the particles
     * @param growth_rate rate at which particle size changes per second
     * @param energy starting energy (lifetime) of each particle
     * @param friction damping factor applied to particles colliding with terrain
     * @param src_blend_func OpenGL source blend function
     * @param dst_blend_func OpenGL destination blend function
     * @param textures textures to assign to spawned particles
     */
    public RingEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height,
            int num_particles, float particles_per_second,
            @NonNull Vector3f velocity, @NonNull Vector3f acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3f particle_radius, @NonNull Vector3f growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            @NonNull TextureKey @NonNull [] textures) {
        super(world, position,
                offset_z,
                emitter_radius,
                emitter_height,
                num_particles,
                particles_per_second,
                velocity,
                acceleration,
                color,
                delta_color,
                particle_radius,
                growth_rate,
                energy,
                friction,
                src_blend_func,
                dst_blend_func,
                textures,
                null,
                textures.length);
        this.num_particles = num_particles;
    }

    @Override
    protected int initParticle(@NonNull Vector3f position, @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear templateColor, Color.@NonNull LinearDelta templateDeltaColor,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy) {
        float baseAngle = 2 * (float) Math.PI / num_particles;
        for (int i = 0; i < num_particles; i++) {
            LinearParticle particle = new LinearParticle(getWorld());
            Vector3f pos = position;
            particle.setPos(pos.x(), pos.y(), pos.z());

            // Randomize radial velocity and energy for "blotchy" expansion
            float velocityMultiplier = ThreadLocalRandom.current().nextFloat(0.7f, 1.3f);
            float energyMultiplier = ThreadLocalRandom.current().nextFloat(0.4f, 1.6f);

            // Per-particle color (called per-particle for individual variation within the ring)
            Color.Linear particleColor = nextParticleColor(this.color);
            float actualEnergy = energy * energyMultiplier;
            float baseFadeRate = actualEnergy > 0f ? -particleColor.a() / actualEnergy : 0f;
            float fadeMultiplier = ThreadLocalRandom.current().nextFloat(0.8f, 1.2f);
            Color.LinearDelta particleDeltaColor = templateDeltaColor.alpha(baseFadeRate * fadeMultiplier);

            // Jitter the angle to break up perfect rings
            float angle = baseAngle * i + ThreadLocalRandom.current().nextFloat(-0.1f, 0.1f);

            // in this special case velocity.getZ() is the actual velocity. not the velocity in the z direction
            particle.setVelocity(velocity.z() * velocityMultiplier * (float) Math.cos(angle),
                    velocity.z() * velocityMultiplier * (float) Math.sin(angle),
                    0);
            particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
            particle.setColor(particleColor);
            particle.setDeltaColor(particleDeltaColor);
            particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
            particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
            particle.setEnergy(energy * energyMultiplier);
            particle.setType(nextType());
            add(particle);
        }
        return num_particles;
    }
}

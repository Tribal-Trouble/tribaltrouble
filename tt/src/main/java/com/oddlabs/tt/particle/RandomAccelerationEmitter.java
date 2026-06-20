package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A linear emitter that applies randomized acceleration to its particles.
 */
public final class RandomAccelerationEmitter extends LinearEmitter {

    private final float angle_bound;
    private final float angle_max_jump;
    private final @NonNull Vector3f current_acceleration;
    private final @NonNull Vector3fc base_acceleration;
    private final float acceleration_factor;

    private float x_angle = 0;
    private float y_angle = 0;

    /**
     * Constructs a new RandomAccelerationEmitter with fully specified parameters, including custom sprite renderers and
     * type counts.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of acceleration
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity initial velocity vector of spawned particles
     * @param acceleration base acceleration vector applied to particles
     * @param acceleration_factor scaling factor applied to the horizontal random acceleration components
     * @param color base color of the particles
     * @param delta_color color delta applied to particles per second
     * @param particle_radius initial 3D size radius of the particles
     * @param growth_rate rate at which particle size changes per second
     * @param energy starting energy (lifetime) of each particle
     * @param friction damping factor applied to particles colliding with terrain
     * @param src_blend_func OpenGL source blend function
     * @param dst_blend_func OpenGL destination blend function
     * @param textures textures to assign to spawned particles
     * @param sprite_renderers sprite renderers to assign to spawned particles
     * @param types number of different particle types/textures
     */
    public RandomAccelerationEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            float acceleration_factor,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures, SpriteKey[] sprite_renderers, int types) {
        super(world, position, offset_z, emitter_radius, emitter_height, num_particles, particles_per_second,
                velocity, acceleration, color, delta_color, particle_radius, growth_rate, energy, friction,
                src_blend_func, dst_blend_func, textures, sprite_renderers, types);
        this.base_acceleration = acceleration;
        this.current_acceleration = new Vector3f(acceleration);
        this.angle_bound = angle_bound;
        this.angle_max_jump = angle_max_jump;
        this.acceleration_factor = acceleration_factor;
    }

    /**
     * Constructs a new RandomAccelerationEmitter with a simplified texture array, defaulting sprite renderers to null.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of acceleration
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity initial velocity vector of spawned particles
     * @param acceleration base acceleration vector applied to particles
     * @param acceleration_factor scaling factor applied to the horizontal random acceleration components
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
    public RandomAccelerationEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            float acceleration_factor,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures) {
        this(world, position, offset_z, emitter_radius, emitter_height, angle_bound, angle_max_jump, num_particles,
                particles_per_second, velocity, acceleration, acceleration_factor, color, delta_color,
                particle_radius, growth_rate, energy, friction, src_blend_func, dst_blend_func, textures, null,
                textures.length);
    }

    @Override
    protected int initParticle(@NonNull Vector3f position, @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy) {
        randomizeAcceleration();

        LinearParticle particle = new LinearParticle(getWorld());
        Vector3f pos = randomPosition();
        particle.setPos(pos.x(), pos.y(), pos.z());
        Random random = ThreadLocalRandom.current();
        float vx = velocity.x() + random.nextFloat(-0.2f, 0.2f);
        float vy = velocity.y() + random.nextFloat(-0.2f, 0.2f);
        float vz = velocity.z() + random.nextFloat(-0.1f, 0.1f);
        particle.setVelocity(vx, vy, vz);
        particle.setAcceleration(current_acceleration.x(), current_acceleration.y(), current_acceleration.z());
        particle.setColor(color);
        particle.setDeltaColor(delta_color);
        particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
        particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
        particle.setEnergy(energy);
        particle.setType(nextType());
        add(particle);
        return 1;
    }

    private void randomizeAcceleration() {
        Random random = ThreadLocalRandom.current();
        float halfJump = .5f * angle_max_jump;
        float dx_angle = halfJump > 0f ? random.nextFloat(-halfJump, halfJump) : 0f;
        float dy_angle = halfJump > 0f ? random.nextFloat(-halfJump, halfJump) : 0f;

        if ((x_angle + dx_angle < -angle_bound) || (x_angle + dx_angle > angle_bound))
            x_angle -= dx_angle;
        else
            x_angle += dx_angle;

        if ((y_angle + dy_angle < -angle_bound) || (y_angle + dy_angle > angle_bound))
            y_angle -= dy_angle;
        else
            y_angle += dy_angle;

        float x = base_acceleration.x() + acceleration_factor * (float) Math.sin(x_angle);
        float y = base_acceleration.y() + acceleration_factor * (float) Math.sin(y_angle);
        current_acceleration.set(x, y, base_acceleration.z());
    }
}

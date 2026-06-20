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
 * A linear emitter that applies randomized initial velocities to its particles.
 */
public final class RandomVelocityEmitter extends LinearEmitter {

    private final float uv_angle;
    private final float angle_bound;
    private final float angle_max_jump;
    private final @NonNull Vector3f current_velocity;
    private final boolean randomizeRotation;
    private final boolean randomizeScale;

    private float x_angle = 0;
    private float y_angle = 0;

    /**
     * Constructs a new RandomVelocityEmitter with fully specified parameters, including custom sprite renderers and
     * type counts.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param uv_angle initial rotation angle for the particle sprite textures
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of velocity
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity base initial velocity vector of spawned particles
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
     * @param sprite_renderers sprite renderers to assign to spawned particles
     * @param types number of different particle types/textures
     * @param randomizeRotation whether starting particle rotations should be randomized
     * @param randomizeScale whether starting particle scales should be randomized
     */
    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z, float uv_angle,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures, SpriteKey[] sprite_renderers, int types,
            boolean randomizeRotation, boolean randomizeScale) {
        super(world, position, offset_z, emitter_radius, emitter_height, num_particles, particles_per_second,
                velocity, acceleration, color, delta_color, particle_radius, growth_rate, energy, friction,
                src_blend_func, dst_blend_func, textures, sprite_renderers, types);
        this.uv_angle = uv_angle;
        this.current_velocity = new Vector3f(velocity);
        this.angle_bound = angle_bound;
        this.angle_max_jump = angle_max_jump;
        this.randomizeRotation = randomizeRotation;
        this.randomizeScale = randomizeScale;
    }

    /**
     * Constructs a new RandomVelocityEmitter with fully specified parameters, defaulting randomization flags to false.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param uv_angle initial rotation angle for the particle sprite textures
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of velocity
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity base initial velocity vector of spawned particles
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
     * @param sprite_renderers sprite renderers to assign to spawned particles
     * @param types number of different particle types/textures
     */
    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z, float uv_angle,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures, SpriteKey[] sprite_renderers, int types) {
        this(world, position, offset_z, uv_angle, emitter_radius, emitter_height, angle_bound, angle_max_jump,
                num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius,
                growth_rate, energy, friction, src_blend_func, dst_blend_func, textures, sprite_renderers, types,
                false, false);
    }

    /**
     * Constructs a new RandomVelocityEmitter with a simplified texture array, defaulting sprite renderers to null.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param uv_angle initial rotation angle for the particle sprite textures
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of velocity
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity base initial velocity vector of spawned particles
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
    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z, float uv_angle,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures) {
        this(world, position, offset_z, uv_angle, emitter_radius, emitter_height, angle_bound, angle_max_jump,
                num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius,
                growth_rate, energy, friction, src_blend_func, dst_blend_func, textures, null, textures.length,
                false, false);
    }

    /**
     * Constructs a new RandomVelocityEmitter with a simplified sprite renderer array, defaulting texture keys to null
     * and blending functions to zero.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of velocity
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity base initial velocity vector of spawned particles
     * @param acceleration acceleration vector applied to particles
     * @param color base color of the particles
     * @param delta_color color delta applied to particles per second
     * @param particle_radius initial 3D size radius of the particles
     * @param growth_rate rate at which particle size changes per second
     * @param energy starting energy (lifetime) of each particle
     * @param friction damping factor applied to particles colliding with terrain
     * @param sprite_renderers sprite renderers to assign to spawned particles
     */
    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            SpriteKey @NonNull [] sprite_renderers) {
        this(world, position, offset_z, emitter_radius, emitter_height, angle_bound, angle_max_jump,
                num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius,
                growth_rate, energy, friction, sprite_renderers, false, false);
    }

    /**
     * Constructs a new RandomVelocityEmitter with a simplified sprite renderer array and randomization flags.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param angle_bound maximum angle bounds (in radians) for the random walk of velocity
     * @param angle_max_jump maximum angle step in radians per spawned particle
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity base initial velocity vector of spawned particles
     * @param acceleration acceleration vector applied to particles
     * @param color base color of the particles
     * @param delta_color color delta applied to particles per second
     * @param particle_radius initial 3D size radius of the particles
     * @param growth_rate rate at which particle size changes per second
     * @param energy starting energy (lifetime) of each particle
     * @param friction damping factor applied to particles colliding with terrain
     * @param sprite_renderers sprite renderers to assign to spawned particles
     * @param randomizeRotation whether starting particle rotations should be randomized
     * @param randomizeScale whether starting particle scales should be randomized
     */
    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            SpriteKey @NonNull [] sprite_renderers, boolean randomizeRotation, boolean randomizeScale) {
        this(world, position, offset_z, 0f, emitter_radius, emitter_height, angle_bound, angle_max_jump,
                num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius,
                growth_rate, energy, friction, 0, 0, null, sprite_renderers, sprite_renderers.length,
                randomizeRotation, randomizeScale);
    }

    @Override
    protected int initParticle(@NonNull Vector3f position, @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
            float energy) {
        randomizeVelocity();

        float angle = randomizeRotation ? (float) ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI) : uv_angle;
        LinearParticle particle = new LinearParticle(getWorld(), angle);
        if (randomizeRotation) {
            particle.setAngularVelocity(ThreadLocalRandom.current().nextFloat(-10f, 10f));
        }
        Vector3f pos = randomPosition();
        particle.setPos(pos.x(), pos.y(), pos.z());
        particle.setVelocity(current_velocity.x(), current_velocity.y(), current_velocity.z());
        particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
        particle.setColor(color);
        particle.setDeltaColor(delta_color);
        if (randomizeScale) {
            float scale = ThreadLocalRandom.current().nextFloat(0.4f, 1.5f);
            particle.setRadius(particle_radius.x() * scale, particle_radius.y() * scale, particle_radius.z() * scale);
        } else {
            particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
        }
        particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
        particle.setEnergy(energy);
        particle.setType(nextType());
        add(particle);
        return 1;
    }

    private void randomizeVelocity() {
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

        float x = velocity.x() + velocity.z() * (float) Math.sin(x_angle);
        float y = velocity.y() + velocity.z() * (float) Math.sin(y_angle);
        current_velocity.set(x, y, velocity.z());
    }

}

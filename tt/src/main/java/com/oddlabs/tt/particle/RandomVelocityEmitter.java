package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public final class RandomVelocityEmitter extends LinearEmitter {
    private final @NonNull Random random;

    private final float uv_angle;
    private final float angle_bound;
    private final float angle_max_jump;
    private final @NonNull Vector3f velocity;
    private final @NonNull Vector3f offset_velocity;

    private float x_angle = 0;
    private float y_angle = 0;

    private RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z, float uv_angle,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3f velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures, SpriteKey[] sprite_renderers, int types,
            @NonNull AnimationManager manager) {
        super(world, position, offset_z, emitter_radius, emitter_height, num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius, growth_rate, energy, friction, src_blend_func, dst_blend_func, textures, sprite_renderers, types, manager);
        this.random = world.getRandom();
        this.uv_angle = uv_angle;
        this.velocity = velocity;
        offset_velocity = new Vector3f(velocity.x(), velocity.y(), velocity.z());
        this.angle_bound = angle_bound;
        this.angle_max_jump = angle_max_jump;
    }

    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z, float uv_angle,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3f velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            TextureKey @NonNull [] textures, @NonNull AnimationManager manager) {
        this(world, position, offset_z, uv_angle, emitter_radius, emitter_height, angle_bound, angle_max_jump, num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius, growth_rate, energy, friction, src_blend_func, dst_blend_func, textures, null, textures.length, manager);
    }

    public RandomVelocityEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
            int num_particles, float particles_per_second,
            @NonNull Vector3f velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            SpriteKey @NonNull [] sprite_renderers, @NonNull AnimationManager manager) {
        this(world, position, offset_z, 0f, emitter_radius, emitter_height, angle_bound, angle_max_jump, num_particles, particles_per_second, velocity, acceleration, color, delta_color, particle_radius, growth_rate, energy, friction, 0, 0, null, sprite_renderers, sprite_renderers.length, manager);
    }

    @Override
    protected int initParticle(@NonNull Vector3f position, @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
            float energy) {
        randomizeVelocity();

        LinearParticle particle = new LinearParticle(getWorld(), uv_angle);
        Vector3f pos = randomPosition();
        particle.setPos(pos.x(), pos.y(), pos.z());
        particle.setVelocity(velocity.x(), velocity.y(), velocity.z());
        particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
        particle.setColor(color.x(), color.y(), color.z(), color.w());
        particle.setDeltaColor(delta_color.x(), delta_color.y(), delta_color.z(), delta_color.w());
        particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
        particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
        particle.setEnergy(energy);
        particle.setType(random.nextInt(getTypes()));
        add(particle);
        return 1;
    }

    private void randomizeVelocity() {
        float dx_angle = random.nextFloat() * angle_max_jump - .5f * angle_max_jump;
        float dy_angle = random.nextFloat() * angle_max_jump - .5f * angle_max_jump;

        if ((x_angle + dx_angle < -angle_bound) || (x_angle + dx_angle > angle_bound))
            x_angle -= dx_angle;
        else
            x_angle += dx_angle;

        if ((y_angle + dy_angle < -angle_bound) || (y_angle + dy_angle > angle_bound))
            y_angle -= dy_angle;
        else
            y_angle += dy_angle;

        float x = offset_velocity.x() + offset_velocity.z() * (float) Math.sin(x_angle);
        float y = offset_velocity.y() + offset_velocity.z() * (float) Math.sin(y_angle);
        velocity.set(x, y, offset_velocity.z());
    }

}

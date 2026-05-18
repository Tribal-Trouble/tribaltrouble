package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public abstract class LinearEmitter extends Emitter<LinearParticle> {
    private static final float SQRT_2 = (float) Math.sqrt(2f);

    private final @NonNull Random random;
    private final Vector3f randomized_position = new Vector3f();
    private final float offset_z;
    private final float emitter_radius;
    private final float emitter_height;
    private final float particles_per_second;
    private final @NonNull Vector3fc velocity;
    private final @NonNull Vector3fc acceleration;
    private final @NonNull Vector4fc color;
    private final @NonNull Vector3fc particle_radius;
    private final @NonNull Vector3fc growth_rate;
    private final float friction;

    private @NonNull Vector4fc delta_color;
    private float energy;
    private int num_particles;
    private float particle_counter = 0;
    private boolean started = true;

    protected LinearEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            @NonNull TextureKey @NonNull [] textures, @NonNull SpriteKey @Nullable [] sprite_renderers, int types,
            @NonNull AnimationManager manager) {
        super(world, position, src_blend_func, dst_blend_func, textures, sprite_renderers, types, manager);
        this.offset_z = offset_z;
        this.emitter_radius = emitter_radius;
        this.emitter_height = emitter_height;
        this.num_particles = num_particles;
        this.particles_per_second = particles_per_second;
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.color = color;
        this.delta_color = delta_color;
        this.particle_radius = particle_radius;
        this.growth_rate = growth_rate;
        this.energy = energy;
        this.friction = friction;
        random = new Random((long) (position.x() * position.y() * position.z()));
        position.set(position.x(), position.y(), position.z() + offset_z);

        register();
    }

    public final void setDeltaColor(@NonNull Vector4f delta_color) {
        this.delta_color = delta_color;
    }

    public final void setEnergy(float energy) {
        this.energy = energy;
    }

    public final void start() {
        started = true;
    }

    public final void stop() {
        started = false;
    }

    public final void done() {
        num_particles = 0;
    }

    @Override
    public final void animate(float t) {
        if (started)
            particle_counter += particles_per_second * t;

        while (particle_counter >= 1 && (num_particles == -1 || num_particles != 0) && started) {
            int initiated = initParticle(getPosition(), velocity, acceleration, color, delta_color, particle_radius,
                    growth_rate, energy);
            assert initiated <= num_particles || num_particles == -1 : "Too many particles initiated";
            particle_counter -= initiated;
            if (num_particles > 0)
                num_particles -= initiated;
        }

        float x_min = Float.POSITIVE_INFINITY;
        float x_max = Float.NEGATIVE_INFINITY;
        float y_min = Float.POSITIVE_INFINITY;
        float y_max = Float.NEGATIVE_INFINITY;
        float z_min = Float.POSITIVE_INFINITY;
        float z_max = Float.NEGATIVE_INFINITY;

        int size = 0;
        for (List<LinearParticle> particles : getParticles()) {
            Iterator<LinearParticle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                LinearParticle particle = iterator.next();
                if (particle.getEnergy() <= 0f) {
                    iterator.remove();
                    continue;
                }

                particle.update(t);

                float x = particle.getPosX();
                float y = particle.getPosY();
                float z = particle.getPosZ();
                float landscape_z = getWorld().getHeightMap().getNearestHeight(x, y);
                if (z < landscape_z + particle.getRadiusZ() + offset_z) {
                    particle.setPos(x, y, landscape_z + particle.getRadiusZ() + offset_z);
                    particle.setVelocity(particle.getVelocityX() * friction, particle.getVelocityY() * friction,
                            -particle.getVelocityZ() * friction);
                }

                float radius_x = particle.getRadiusX() * SQRT_2;
                float radius_y = particle.getRadiusY() * SQRT_2;
                float radius_z = particle.getRadiusZ() * SQRT_2;
                x_min = Math.min(x_min, x - radius_x);
                x_max = Math.max(x_max, x + radius_x);
                y_min = Math.min(y_min, y - radius_y);
                y_max = Math.max(y_max, y + radius_y);
                z_min = Math.min(z_min, z - radius_z);
                z_max = Math.max(z_max, z + radius_z);
            }
            size += particles.size();
        }
        setBounds(x_min, x_max, y_min, y_max, z_min, z_max);
        reregister();
        if (size == 0 && num_particles == 0)
            remove();
    }

    protected abstract int initParticle(@NonNull Vector3f position,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
            float energy);

    protected final @NonNull Vector3f randomPosition() {
        float r = emitter_radius * (float) (1 - random.nextGaussian());
        float a = random.nextFloat() * (float) Math.PI * 2;
        float x = (float) Math.cos(a) * r;
        float y = (float) Math.sin(a) * r;
        float z = random.nextFloat() * emitter_height;

        randomized_position.set(getX() + x, getY() + y, getZ() + z);
        return randomized_position;
    }
}

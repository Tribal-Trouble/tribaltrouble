package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.RenderTools;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An emitter that spawns particles with linear movement properties (velocity, acceleration).
 */
public abstract class LinearEmitter extends Emitter<LinearParticle> {
    private static final float SQRT_2 = (float) Math.sqrt(2f);

    private final Vector3f randomized_position = new Vector3f();
    private final float offset_z;
    private final float emitter_radius;
    private final float emitter_height;
    protected final @NonNull Vector3f velocity;
    protected final @NonNull Vector3f acceleration;
    protected final Color.@NonNull Linear color;
    protected final @NonNull Vector3f particle_radius;
    protected final @NonNull Vector3f growth_rate;
    protected final float friction;
    private final BoundingBox bounds = new BoundingBox();

    protected Color.@NonNull LinearDelta delta_color;
    protected float energy;

    /**
     * Constructs a new LinearEmitter with linear particle dynamics.
     *
     * @param world game world this emitter belongs to
     * @param position base 3D position of the emitter
     * @param offset_z vertical offset applied to the emitter position
     * @param emitter_radius radius of the horizontal spawning region
     * @param emitter_height height of the vertical spawning region
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param velocity initial velocity vector of spawned particles
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
    protected LinearEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
            float emitter_radius, float emitter_height,
            int num_particles, float particles_per_second,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy, float friction,
            int src_blend_func, int dst_blend_func,
            @NonNull TextureKey @NonNull [] textures, @NonNull SpriteKey @Nullable [] sprite_renderers, int types) {
        super(world, position, src_blend_func, dst_blend_func, textures, sprite_renderers, types, num_particles,
                particles_per_second);
        this.offset_z = offset_z;
        this.emitter_radius = emitter_radius;
        this.emitter_height = emitter_height;
        this.velocity = new Vector3f(velocity);
        this.acceleration = new Vector3f(acceleration);
        this.color = color;
        this.delta_color = delta_color;
        this.particle_radius = new Vector3f(particle_radius);
        this.growth_rate = new Vector3f(growth_rate);
        this.energy = energy;
        this.friction = friction;
        position.set(position.x(), position.y(), position.z() + offset_z);
    }

    public final void setParticleRadius(@NonNull Vector3fc radius) {
        this.particle_radius.set(radius);
    }

    public final void setParticleRadius(float x, float y, float z) {
        this.particle_radius.set(x, y, z);
    }

    public final void setGrowthRate(@NonNull Vector3fc growth_rate) {
        this.growth_rate.set(growth_rate);
    }

    public final void setGrowthRate(float x, float y, float z) {
        this.growth_rate.set(x, y, z);
    }

    public final void setVelocity(@NonNull Vector3fc velocity) {
        this.velocity.set(velocity);
    }

    public final void setVelocity(float x, float y, float z) {
        this.velocity.set(x, y, z);
    }

    public final void setAcceleration(@NonNull Vector3fc acceleration) {
        this.acceleration.set(acceleration);
    }

    public final void setAcceleration(float x, float y, float z) {
        this.acceleration.set(x, y, z);
    }

    public final void setDeltaColor(Color.@NonNull LinearDelta delta_color) {
        this.delta_color = delta_color;
    }

    public final void setEnergy(float energy) {
        this.energy = energy;
    }

    @Override
    public final void animate(float t) {
        updateSpawning(t);
        updateCluster(t);

        float x_min = Float.POSITIVE_INFINITY;
        float x_max = Float.NEGATIVE_INFINITY;
        float y_min = Float.POSITIVE_INFINITY;
        float y_max = Float.NEGATIVE_INFINITY;
        float z_min = Float.POSITIVE_INFINITY;
        float z_max = Float.NEGATIVE_INFINITY;

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

                x = particle.getPosX();
                y = particle.getPosY();
                z = particle.getPosZ();
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
        }
        bounds.setBounds(x_min, x_max, y_min, y_max, z_min, z_max);
    }

    @Override
    public void debugRender() {
        RenderTools.draw(bounds, 1f, 1f, 1f);
    }

    @Override
    protected int initParticles(int count) {
        int initiated = 0;
        for (int i = 0; i < count; i++) {
            Color.Linear particleColor = nextParticleColor(color);
            float baseFadeRate = energy > 0f ? -particleColor.a() / energy : 0f;
            float multiplier = ThreadLocalRandom.current().nextFloat(0.85f, 1.15f);
            Color.LinearDelta particleDeltaColor = delta_color.alpha(baseFadeRate * multiplier);
            initiated += initParticle(getPosition(), velocity, acceleration, particleColor, particleDeltaColor,
                    particle_radius, growth_rate, energy);
        }
        return initiated;
    }

    protected abstract int initParticle(@NonNull Vector3f position,
            @NonNull Vector3fc velocity, @NonNull Vector3fc acceleration,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
            float energy);

    protected final @NonNull Vector3f randomPosition() {
        Random random = ThreadLocalRandom.current();
        float r = emitter_radius * (float) (1 - random.nextGaussian());
        float a = random.nextFloat(0f, (float) Math.PI * 2);
        float x = (float) Math.cos(a) * r;
        float y = (float) Math.sin(a) * r;
        float z = emitter_height > 0f ? random.nextFloat(0f, emitter_height) : 0f;

        randomized_position.set(getX() + x, getY() + y, getZ() + z);
        return randomized_position;
    }
}

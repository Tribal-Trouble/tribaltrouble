package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A specialized parametric emitter that maintains a balanced distribution of particles.
 */
public final class BalancedParametricEmitter extends ParametricEmitter {
    private final int num_particles;
    private final float dist_u;
    private final float dist_v;
    private final float margin_u;
    private final float margin_v;

    /**
     * Constructs a new BalancedParametricEmitter that spawns a balanced distribution of particles along a parametric
     * curve.
     *
     * @param world game world this emitter belongs to
     * @param function parametric function defining particle trajectories
     * @param position base 3D position of the emitter
     * @param velocity_u initial u-velocity component along the parametric function
     * @param velocity_v initial v-velocity component along the parametric function
     * @param dist_u parametric u-distance interval over which particles are distributed
     * @param dist_v parametric v-distance interval over which particles are distributed
     * @param num_particles number of particles to distribute and budget
     * @param margin_u random margin of deviation applied to the u-velocity component
     * @param margin_v random margin of deviation applied to the v-velocity component
     * @param color base color of the particles
     * @param delta_color color delta applied to particles per second
     * @param particle_radius initial 3D size radius of the particles
     * @param growth_rate rate at which particle size changes per second
     * @param energy starting energy (lifetime) of each particle
     * @param src_blend_func OpenGL source blend function
     * @param dst_blend_func OpenGL destination blend function
     * @param textures textures to assign to spawned particles
     */
    public BalancedParametricEmitter(@NonNull World world, @NonNull ParametricFunction function,
            @NonNull Vector3f position,
            float velocity_u, float velocity_v, float dist_u, float dist_v,
            int num_particles, float margin_u, float margin_v,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy,
            int src_blend_func, int dst_blend_func, TextureKey @NonNull [] textures) {
        super(world, function, position,
                0f, 0f, velocity_u, velocity_v, 0f,
                num_particles, Float.MAX_VALUE,
                color, delta_color,
                particle_radius, growth_rate, energy,
                src_blend_func, dst_blend_func, textures);
        this.num_particles = num_particles;
        this.dist_u = dist_u;
        this.dist_v = dist_v;
        this.margin_u = margin_u;
        this.margin_v = margin_v;
    }

    @Override
    protected int initParticle(@NonNull ParametricFunction function,
            float velocity_u, float velocity_v,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
            float energy) {

        for (int i = 0; i < num_particles; i++) {
            float u = dist_u * i / num_particles;
            float v = dist_v * i / num_particles;
            ParametricParticle particle = new ParametricParticle(getWorld(), function, u, v, 0f, 0f, 0f);
            Vector3f offset = randomOffset(margin_u, margin_v, 0f);
            particle.setVelocity(velocity_u + offset.x(), velocity_v + offset.y());
            particle.setColor(color);
            particle.setDeltaColor(delta_color);
            particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
            particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
            particle.setEnergy(energy);
            particle.setType(ThreadLocalRandom.current().nextInt(getTypes()));
            particle.update(0);
            add(particle);
        }
        return num_particles;
    }
}

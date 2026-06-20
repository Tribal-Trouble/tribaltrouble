package com.oddlabs.tt.particle;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.RenderTools;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An emitter that spawns particles whose movement is defined by a parametric function.
 */
public class ParametricEmitter extends Emitter<ParametricParticle> {
    private static final float SQRT_2 = (float) Math.sqrt(2f);
    private final Vector3f randomized_offset = new Vector3f();
    protected final @NonNull ParametricFunction function;
    protected final float area_xy;
    protected final float area_z;
    protected final float velocity_u;
    protected final float velocity_v;
    protected final float velocity_random_margin;
    protected final Color.@NonNull Linear color;
    protected final @NonNull Vector3fc particle_radius;
    protected final @NonNull Vector3fc growth_rate;
    private final BoundingBox bounds = new BoundingBox();
    private boolean randomizeScale = false;
    private float heightLightingIntensity = 0.0f;
    private float maxLocalZ = 1.0f;

    protected Color.@NonNull LinearDelta delta_color;
    protected float energy;

    /**
     * Constructs a new ParametricEmitter using the specified parametric function for particle movement.
     *
     * @param world game world this emitter belongs to
     * @param function parametric function defining particle trajectories
     * @param position base 3D position of the emitter
     * @param area_xy horizontal bounds for random spawn offsets
     * @param area_z vertical bounds for random spawn offsets
     * @param velocity_u initial u-velocity component along the parametric function
     * @param velocity_v initial v-velocity component along the parametric function
     * @param velocity_random_margin random deviation added to velocity components
     * @param num_particles maximum particle budget, or -1 for infinite
     * @param particles_per_second rate at which particles spawn per second
     * @param color base color of the particles
     * @param delta_color color delta applied to particles per second
     * @param particle_radius initial 3D size radius of the particles
     * @param growth_rate rate at which particle size changes per second
     * @param energy starting energy (lifetime) of each particle
     * @param src_blend_func OpenGL source blend function
     * @param dst_blend_func OpenGL destination blend function
     * @param textures textures to assign to spawned particles
     */
    public ParametricEmitter(@NonNull World world, @NonNull ParametricFunction function, @NonNull Vector3f position,
            float area_xy, float area_z, float velocity_u, float velocity_v, float velocity_random_margin,
            int num_particles, float particles_per_second,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate, float energy,
            int src_blend_func, int dst_blend_func, TextureKey @NonNull [] textures) {
        super(world, position, src_blend_func, dst_blend_func, textures, null, textures.length, num_particles,
                particles_per_second);
        this.function = function;
        this.area_xy = area_xy;
        this.area_z = area_z;
        this.velocity_u = velocity_u;
        this.velocity_v = velocity_v;
        this.velocity_random_margin = velocity_random_margin;
        this.color = color;
        this.delta_color = delta_color;
        this.particle_radius = particle_radius;
        this.growth_rate = growth_rate;
        this.energy = energy;
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

        for (List<ParametricParticle> list : getParticles()) {
            Iterator<ParametricParticle> particles = list.iterator();
            while (particles.hasNext()) {
                ParametricParticle particle = particles.next();
                if (particle.getEnergy() <= 0f) {
                    particles.remove();
                    continue;
                }

                particle.update(t, getScaleX(), getScaleY(), getScaleZ());
                float x = particle.getPosX();
                float y = particle.getPosY();
                float z = particle.getPosZ();
                particle.setPos(getX() + x, getY() + y, getZ() + z);

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
            initiated += initParticle(function, velocity_u, velocity_v, particleColor, delta_color,
                    particle_radius, growth_rate, energy);
        }
        return initiated;
    }

    protected int initParticle(@NonNull ParametricFunction function,
            float velocity_u, float velocity_v,
            Color.@NonNull Linear color, Color.@NonNull LinearDelta delta_color,
            @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
            float energy) {

        Vector3f offset = randomOffset(area_xy, area_xy, area_z);
        Random random = ThreadLocalRandom.current();
        float twoPi = (float) Math.PI * 2f;
        ParametricParticle particle = new ParametricParticle(getWorld(), function,
                random.nextFloat(0f, twoPi), random.nextFloat(0f, twoPi),
                offset.x(), offset.y(), offset.z());
        offset = randomOffset(velocity_random_margin, velocity_random_margin, 0f);
        particle.setVelocity(velocity_u + offset.x(), velocity_v + offset.y());
        particle.setColor(color);
        particle.setDeltaColor(delta_color);
        float scale = 1.0f;
        if (randomizeScale) {
            scale = ThreadLocalRandom.current().nextFloat(0.5f, 1.5f);
        }
        particle.setRadius(particle_radius.x() * scale, particle_radius.y() * scale, particle_radius.z() * scale);
        particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
        if (heightLightingIntensity > 0.0f) {
            particle.setHeightLighting(heightLightingIntensity, maxLocalZ);
        }
        particle.setEnergy(energy);
        particle.setType(nextType());
        particle.update(0);
        add(particle);
        return 1;
    }

    protected final @NonNull Vector3f randomOffset(float a, float b, float c) {
        Random random = ThreadLocalRandom.current();
        float x = a > 0f ? random.nextFloat(-a, a) : 0f;
        float y = b > 0f ? random.nextFloat(-b, b) : 0f;
        float z = c > 0f ? random.nextFloat(-c, c) : 0f;

        randomized_offset.set(x, y, z);
        return randomized_offset;
    }

    public final void setRandomizeScale(boolean randomizeScale) {
        this.randomizeScale = randomizeScale;
    }

    public final boolean isRandomizeScale() {
        return randomizeScale;
    }

    public final void setHeightLighting(float intensity, float maxLocalZ) {
        this.heightLightingIntensity = intensity;
        this.maxLocalZ = maxLocalZ;
    }
}

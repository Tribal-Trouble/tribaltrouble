package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class BalancedParametricEmitter extends ParametricEmitter {
	private final int num_particles;
	private final float dist_u;
	private final float dist_v;
	private final float margin_u;
	private final float margin_v;

	public BalancedParametricEmitter(@NonNull World world, ParametricFunction function, @NonNull Vector3f position,
                                     float velocity_u, float velocity_v, float dist_u, float dist_v,
                                     int num_particles, float margin_u, float margin_v,
                                     Vector4fc color, Vector4fc delta_color,
                                     Vector3fc particle_radius, Vector3fc growth_rate, float energy,
                                     int src_blend_func, int dst_blend_func, TextureKey @NonNull [] textures,
                                     @NonNull AnimationManager manager) {
		super(world, function, position,
				0f, 0f, velocity_u, velocity_v, 0f,
				num_particles, Float.MAX_VALUE,
				color, delta_color,
				particle_radius, growth_rate, energy,
				src_blend_func, dst_blend_func, textures,
				manager);
		this.num_particles = num_particles;
		this.dist_u = dist_u;
		this.dist_v = dist_v;
		this.margin_u = margin_u;
		this.margin_v = margin_v;

//		register();
	}

	@Override
	protected int initParticle(ParametricFunction function,
							   float velocity_u, float velocity_v,
							   @NonNull Vector4fc color, @NonNull Vector4fc delta_color,
							   @NonNull Vector3fc particle_radius, @NonNull Vector3fc growth_rate,
							   float energy) {

		for (int i = 0; i < num_particles; i++) {
			float u = dist_u*i/num_particles;
			float v = dist_v*i/num_particles;
			ParametricParticle particle = new ParametricParticle(getWorld(), function, u, v, 0f, 0f, 0f);
			Vector3f offset = randomOffset(margin_u, margin_v, 0f);
			particle.setVelocity(velocity_u + offset.x(), velocity_v + offset.y());
			particle.setColor(color.x(), color.y(), color.z(), color.w());
			particle.setDeltaColor(delta_color.x(), delta_color.y(), delta_color.z(), delta_color.w());
			particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
			particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
			particle.setEnergy(energy);
			particle.setType(getWorld().getRandom().nextInt(getTypes()));
			particle.update(0);
			add(particle);
		}
		return num_particles;
	}
}

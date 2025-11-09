package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.TextureKey;
import org.jspecify.annotations.NonNull;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class RingEmitter extends LinearEmitter {
	private final int num_particles;

	public RingEmitter(@NonNull World world, @NonNull Vector3f position, float offset_z,
                       float emitter_radius, float emitter_height,
                       int num_particles, float particles_per_second,
                       Vector3f velocity, Vector3f acceleration,
                       Vector4f color, Vector4f delta_color,
                       Vector3f particle_radius, Vector3f growth_rate, float energy, float friction,
                       int src_blend_func, int dst_blend_func,
                       TextureKey @NonNull [] textures, AnimationManager manager) {
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
				textures.length,
				manager);
		this.num_particles = num_particles;
	}

    @Override
	protected int initParticle(Vector3f position, @NonNull Vector3f velocity, @NonNull Vector3f acceleration, @NonNull Vector4f color, @NonNull Vector4f delta_color, @NonNull Vector3f particle_radius, @NonNull Vector3f growth_rate, float energy) {
		float angle = 2*(float)Math.PI/num_particles;
		for (int i = 0; i < num_particles; i++) {
			LinearParticle particle = new LinearParticle();
			Vector3f pos = position;
			particle.setPos(pos.x(), pos.y(), pos.z());
			// in this special case velocity.getZ() is the actual velocity. not the velocity in the z direction
			particle.setVelocity(velocity.z()*(float)Math.cos(angle*i), velocity.z()*(float)Math.sin(angle*i), 0);
			particle.setAcceleration(acceleration.x(), acceleration.y(), acceleration.z());
			particle.setColor(color.x(), color.y(), color.z(), color.w());
			particle.setDeltaColor(delta_color.x(), delta_color.y(), delta_color.z(), delta_color.w());
			particle.setRadius(particle_radius.x(), particle_radius.y(), particle_radius.z());
			particle.setGrowthRate(growth_rate.x(), growth_rate.y(), growth_rate.z());
			particle.setEnergy(energy);
			particle.setType(0);
			add(particle);
		}
		return num_particles;
	}
}

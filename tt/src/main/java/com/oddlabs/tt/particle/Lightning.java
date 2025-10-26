package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Lightning extends Element<Lightning> implements Animated {
	private final static float SQRT_2 = (float)Math.sqrt(2f);

	private final AnimationManager manager;
	private final List<StretchParticle> particles = new ArrayList<>();
	private final Vector3f src;
	private final Vector3f dst;
	private final float width;
	private final int num_particles;
	private final Vector4f color;
	private final Vector4f delta_color;
	private final TextureKey texture;
	private final @NonNull World world;

	private final float energy;

	public Lightning(@NonNull World world, Vector3f src, Vector3f dst, float width,
                     int num_particles, Vector4f color, Vector4f delta_color,
                     TextureKey texture, float energy,
                     AnimationManager manager) {
		super(world.getElementRoot());
		this.world = world;
		this.src = src;
		this.dst = dst;
		this.width = width;
		this.num_particles = num_particles;
		this.color = color;
		this.delta_color = delta_color;
		this.texture = texture;
		this.energy = energy;
		this.manager = manager;
		initParticles();
		register();
	}

	public @NonNull List<StretchParticle> getParticles() {
		return particles;
	}

	public TextureKey getTexture() {
		return texture;
	}

	private void initParticles() {
		Random random = world.getRandom();
		random.nextFloat();
		float x = src.getX();
		float y = src.getY();
		float z = src.getZ();
		float height = dst.getZ() - src.getZ();
		float random_limit = height/6f;
		float dz = (height)/num_particles;

		for (int i = 0; i < num_particles; i++) {
			float base_dx = (dst.getX() - x)/(num_particles - i);
			float base_dy = (dst.getY() - y)/(num_particles - i);
			float dx = base_dx + (random.nextFloat() - .5f)*random_limit;
			float dy = base_dy + (random.nextFloat() - .5f)*random_limit;
			StretchParticle particle = new StretchParticle();
			particle.setSrc(x, y, z);

			if (i == num_particles - 1) {
				x = dst.getX();
				y = dst.getY();
				z = dst.getZ();
				particle.setDstWidth(width/2);
			} else {
				x += dx;
				y += dy;
				z += dz;
				particle.setDstWidth(width);
			}
			particle.setDst(x, y, z);
			initParticle(particle);
			particles.add(particle);
		}
	}

	private void initParticle(@NonNull StretchParticle particle) {
		particle.setSrcWidth(width);
		particle.setColor(color.getX(), color.getY(), color.getZ(), color.getW());
		particle.setDeltaColor(delta_color.getX(), delta_color.getY(), delta_color.getZ(), delta_color.getW());
		particle.setRadius(0f, 0f, 0f);
		particle.setGrowthRate(0f, 0f, 0f);
		particle.setEnergy(energy);
	}

    @Override
	public void animate(float t) {
		float x_min = Float.POSITIVE_INFINITY;
		float x_max = Float.NEGATIVE_INFINITY;
		float y_min = Float.POSITIVE_INFINITY;
		float y_max = Float.NEGATIVE_INFINITY;
		float z_min = Float.POSITIVE_INFINITY;
		float z_max = Float.NEGATIVE_INFINITY;

		for (int i = 0; i < particles.size(); i++) {
			StretchParticle particle = particles.get(i);
			if (particle.getEnergy() > 0f) {
				particle.update(t);
				float x = particle.getSrcX();
				float y = particle.getSrcY();
				float z = particle.getSrcZ();
				float radius_x = particle.getRadiusX()*SQRT_2;
				float radius_y = particle.getRadiusY()*SQRT_2;
				float radius_z = particle.getRadiusZ()*SQRT_2;
				x_min = Math.min(x_min, x - radius_x);
				x_max = Math.max(x_max, x + radius_x);
				y_min = Math.min(y_min, y - radius_y);
				y_max = Math.max(y_max, y + radius_y);
				z_min = Math.min(z_min, z - radius_z);
				z_max = Math.max(z_max, z + radius_z);
			} else {
				particles.remove(i);
			}
		}
		setBounds(x_min, x_max, y_min, y_max, z_min, z_max);
		reregister();
		if (particles.isEmpty()) {
			remove();
		}
	}

    @Override
	protected void register() {
		super.register();
		manager.registerAnimation(this);
	}

    @Override
	public void visit(@NonNull ElementVisitor visitor) {
		visitor.visitLightning(this);
	}

    @Override
	protected void remove() {
		super.remove();
		manager.removeAnimation(this);
	}

    @Override
	public void updateChecksum(StateChecksum checksum) {
	}
}

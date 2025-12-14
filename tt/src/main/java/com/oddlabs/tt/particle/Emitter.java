package com.oddlabs.tt.particle;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.util.StateChecksum;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class Emitter extends Element<Emitter> implements Animated {
	private final @NonNull AnimationManager manager;
	private final @NonNull List<@NonNull Particle> @NonNull [] particles;
	private final @NonNull TextureKey @NonNull[] textures;
	private final @NonNull SpriteKey @NonNull [] sprite_renderers;
	private final int src_blend_func;
	private final int dst_blend_func;
	private final int types;
	private final @NonNull World world;

	private @NonNull Vector3f position;
	private float scale_x = 1f;
	private float scale_y = 1f;
	private float scale_z = 1f;

	@SuppressWarnings("unchecked")
	public Emitter(@NonNull World world, @NonNull Vector3f position, int src_blend_func, int dst_blend_func, @NonNull TextureKey @NonNull [] textures, @NonNull SpriteKey @NonNull [] sprite_renderers, int types, @NonNull AnimationManager manager) {
		super(world.getElementRoot());
		this.world = world;
		this.position = position;
		this.src_blend_func = src_blend_func;
		this.dst_blend_func = dst_blend_func;
		this.textures = textures;
		this.sprite_renderers = sprite_renderers;
		this.types = types;
		this.manager = manager;
		particles = Stream.generate(ArrayList::new).limit(types).toArray(List[]::new);
		register();
	}

    @Override
    protected @NonNull Emitter self() {
        return this;
    }

    public final @NonNull World getWorld() {
		return world;
	}

	public final @NonNull SpriteKey @Nullable[] getSpriteRenderers() {
		return sprite_renderers;
	}

	public final List<@NonNull Particle> @NonNull [] getParticles() {
		return particles;
	}

	public final @NonNull TextureKey @Nullable [] getTextures() {
		return textures;
	}

	public final int getSrcBlendFunc() {
		return src_blend_func;
	}

	public final int getDstBlendFunc() {
		return dst_blend_func;
	}

	protected final void add(@NonNull Particle particle) {
		particles[particle.getType()].add(particle);
	}

	public final void setPosition(@NonNull Vector3f position) {
		this.position = position;
	}

	public final @NonNull Vector3f getPosition() {
		return position;
	}

	final float getX() {
		return position.x();
	}

	final float getY() {
		return position.y();
	}

	final float getZ() {
		return position.z();
	}

	public final void scale(float scale_x, float scale_y, float scale_z) {
		this.scale_x = scale_x;
		this.scale_y = scale_y;
		this.scale_z = scale_z;
	}

	public final float getScaleX() {
		return scale_x;
	}

	public final float getScaleY() {
		return scale_y;
	}

	public final float getScaleZ() {
		return scale_z;
	}

	public final void forceColorChange(float dr, float dg, float db, float da) {
            for (List<Particle> particle1 : particles) {
                for (Particle particle : particle1) {
                    particle.setColor(particle.getColorR() + dr, particle.getColorG() + dg, particle.getColorB() + db, particle.getColorA() + da);
                }
            }
	}

	protected final int getTypes() {
		return types;
	}

	@Override
	protected void register() {
		super.register();
		manager.registerAnimation(this);
	}

	@Override
	public final void visit(@NonNull ElementVisitor visitor) {
		visitor.visitEmitter(this);
	}

	@Override
	protected final void remove() {
		super.remove();
		manager.removeAnimation(this);
	}

	@Override
	public final void updateChecksum(@NonNull StateChecksum checksum) {
	}
}

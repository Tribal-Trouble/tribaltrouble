package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public abstract class Model extends Element<Model> {
	private final @NonNull World world;

	protected Model(@NonNull World world) {
		super(Objects.requireNonNull(world, "world").getElementRoot());
        this.world = world ;
	}

	public abstract float getShadowDiameter();

	public abstract float getOffsetZ();
	public abstract int getAnimation();
	public abstract float getAnimationTicks();
	public abstract SpriteKey getSpriteRenderer();
	public abstract float getNoDetailSize();

	private void updateBounds() {
		float x = getPositionX();
		float y = getPositionY();
		float z = getPositionZ();
		BoundingBox unit_bounds = getSpriteRenderer().getBounds(getAnimation());
		float error = getZError();
		setBounds(unit_bounds.bmin_x + x, unit_bounds.bmax_x + x, unit_bounds.bmin_y + y, unit_bounds.bmax_y + y, unit_bounds.bmin_z + z - error, unit_bounds.bmax_z + z + error);
	}

	protected float getZError() {
		return 0f;
	}

	protected final float getLandscapeError() {
		return world.getHeightMap().getLeafFromCoordinates(getPositionX(), getPositionY()).getMaxError();
	}

	public final @NonNull World getWorld() {
		return world;
	}

    @Override
	public final void setPosition(float x, float y) {
		super.setPosition(x, y);
		reinsert();
	}

	protected final void reinsert() {
		if (isRegistered()) {
			setPositionZ(world.getHeightMap().getNearestHeight(getPositionX(), getPositionY()) + getOffsetZ());
			updateBounds();
			reregister();
		}
	}
}

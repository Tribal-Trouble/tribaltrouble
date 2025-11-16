package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

public abstract class SupplyModel extends Model implements Supply, Target, ModelToolTip {
	private static final float SPAWN_OFFSET_Z = -2f;

	private final SpriteKey sprite_renderer;

	private final float size;
	private final float rotation;

	private float offset_z = 0;

	private int grid_x;
	private int grid_y;

	private int num_supplies;
	private int hit_counter = 0;

	@SuppressWarnings("unchecked")
	public SupplyModel(@NonNull World world, SpriteKey sprite_renderer, float size, int grid_x, int grid_y, float x, float y, float rotation, int num_supplies, boolean increase_count) {
		super(world);
		this.sprite_renderer = sprite_renderer;
		this.size = size;
		this.grid_x = grid_x;
		this.grid_y = grid_y;
		this.rotation = rotation;
		this.num_supplies = num_supplies;
		setPosition(x, y);
		world.getNotificationListener().registerTarget(this);
		UnitGrid unit_grid = world.getUnitGrid();
		unit_grid.occupyGrid(grid_x, grid_y, this);
		Region region = unit_grid.getRegion(grid_x, grid_y);
		region.registerObject((Class<SupplyModel>) getClass(), this);
		register();
		reinsert();
		if (increase_count)
			world.getSupplyManager(getClass()).newSupply();
	}

	@Override
	public final void visit(@NonNull ToolTipVisitor visitor) {
		visitor.visitSupply(this);
	}

	public final float getRotation() {
		return rotation;
	}

	@Override
	public void animateSpawn(float t, float progress) {
		offset_z = SPAWN_OFFSET_Z*(1 - progress);
		reinsert();
	}

	@Override
	public void spawnComplete() {
	}

	@Override
	public final boolean isEmpty() {
		return num_supplies == 0;
	}

	@Override
	public boolean hit() {
		hit_counter++;
		if (hit_counter == Supply.HITS_PER_HARVEST) {
			hit_counter = 0;
			decreaseSupply();
			return true;
		} else
			return false;
	}

	@Override
	public final boolean isDead() {
		return isEmpty();
	}

	@SuppressWarnings("unchecked")
	private void decreaseSupply() {
		num_supplies--;
		if (isEmpty()) {
			UnitGrid unit_grid = getWorld().getUnitGrid();
			unit_grid.freeGrid(grid_x, grid_y, this);
			getWorld().getNotificationListener().unregisterTarget(this);
			Region region = unit_grid.getRegion(grid_x, grid_y);
			region.unregisterObject((Class<SupplyModel>) getClass(), this);
			remove();
			getWorld().getSupplyManager(getClass()).emptySupply(this);
		}
	}

	@Override
	public final float getNoDetailSize() {
		throw new RuntimeException();
	}

	@Override
	public final float getSize() {
		return size;
	}

	@Override
	public final int getGridX() {
		return grid_x;
	}

	@Override
	public final int getGridY() {
		return grid_y;
	}

	public void setGridPosition(int grid_x, int grid_y) {
		assert !isDead();
		this.grid_x = grid_x;
		this.grid_y = grid_y;
	}

	@Override
	public float getOffsetZ() {
		return offset_z;
	}

	@Override
	public int getAnimation() {
		return 0;
	}

	@Override
	public float getAnimationTicks() {
		return 0;
	}

	@Override
	public final SpriteKey getSpriteRenderer() {
		return sprite_renderer;
	}

	@Override
	public void visit(@NonNull ElementVisitor visitor) {
		visitor.visitSupplyModel(this);
	}

	@Override
	public float getShadowDiameter() {
		return 0f;
	}

	@Override
	public int getPenalty() {
		return Occupant.STATIC;
	}
}

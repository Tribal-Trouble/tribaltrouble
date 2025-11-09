package com.oddlabs.tt.landscape;

import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

public class LandscapeTarget implements Target {
	private final int grid_x;
	private final int grid_y;

	public LandscapeTarget(int grid_x, int grid_y) {
		this.grid_x = grid_x;
		this.grid_y = grid_y;
	}

	@Override
	public final float getPositionX() {
		return UnitGrid.coordinateFromGrid(grid_x);
	}

	@Override
	public final float getPositionY() {
		return UnitGrid.coordinateFromGrid(grid_y);
	}

	@Override
	public final int getGridX() {
		return grid_x;
	}

	@Override
	public final int getGridY() {
		return grid_y;
	}

	@Override
	public final float getSize() {
		return 0;
	}

	@Override
	public final boolean isDead() {
		return false;
	}

	@Override
	public final @NonNull String toString() {
		return "LandscapeTarget: grid_x = " + grid_x + " | grid_y = " + grid_y;
	}

	public final void startRespond() {
		throw new RuntimeException();
	}

	public final void stopRespond() {
		throw new RuntimeException();
	}
}

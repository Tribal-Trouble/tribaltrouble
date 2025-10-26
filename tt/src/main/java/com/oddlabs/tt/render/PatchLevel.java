package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.LandscapeTileTriangle;
import org.jspecify.annotations.NonNull;

final class PatchLevel {
	private PatchLevel right_neighbour;
	private PatchLevel left_neighbour;
	private PatchLevel top_neighbour;
	private PatchLevel bottom_neighbour;
	private int level;

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getBorderSet() {
		return addNeighbourBorderBit(right_neighbour, LandscapeTileTriangle.EAST) |
			addNeighbourBorderBit(left_neighbour, LandscapeTileTriangle.WEST) |
			addNeighbourBorderBit(top_neighbour, LandscapeTileTriangle.NORTH) |
			addNeighbourBorderBit(bottom_neighbour, LandscapeTileTriangle.SOUTH);
	}

	private int addNeighbourBorderBit(@NonNull PatchLevel neighbour, int bit) {
		return neighbour.level > level ? bit : 0;
	}

	public void adjustLevel() {
		level = getAdjustedLevel();
		adjustNeighbour(right_neighbour);
		adjustNeighbour(left_neighbour);
		adjustNeighbour(top_neighbour);
		adjustNeighbour(bottom_neighbour);
	}

	private void adjustNeighbour(@NonNull PatchLevel neighbour) {
		if (neighbour.level < level - 1)
			neighbour.adjustLevel();
	}

	private int getAdjustedLevel() {
		int adjusted_level = level;
		adjusted_level = Math.max(adjusted_level, right_neighbour.level - 1);
		adjusted_level = Math.max(adjusted_level, left_neighbour.level - 1);
		adjusted_level = Math.max(adjusted_level, top_neighbour.level - 1);
		adjusted_level = Math.max(adjusted_level, bottom_neighbour.level - 1);
		return adjusted_level;
	}

	public void init(@NonNull PatchLevel right, @NonNull PatchLevel top) {
		initTopNeighbour(top);
		initRightNeighbour(right);
	}

	private void initTopNeighbour(@NonNull PatchLevel top_neighbour) {
		this.top_neighbour = top_neighbour;
		top_neighbour.bottom_neighbour = this;
	}

	private void initRightNeighbour(@NonNull PatchLevel right_neighbour) {
		this.right_neighbour = right_neighbour;
		right_neighbour.left_neighbour = this;
	}

}

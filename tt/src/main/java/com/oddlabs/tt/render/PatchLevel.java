package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.LandscapeTileTriangle;

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

	private int addNeighbourBorderBit(PatchLevel neighbour, int bit) {
		return neighbour.level > level ? bit : 0;
	}

	public void adjustLevel() {
		level = getAdjustedLevel();
		adjustNeighbour(right_neighbour);
		adjustNeighbour(left_neighbour);
		adjustNeighbour(top_neighbour);
		adjustNeighbour(bottom_neighbour);
	}

	private void adjustNeighbour(PatchLevel neighbour) {
		if (neighbour.level < level - 1)
			neighbour.adjustLevel();
	}

	private int getAdjustedLevel() {
		int adjusted_level = level;
		adjusted_level = StrictMath.max(adjusted_level, right_neighbour.level - 1);
		adjusted_level = StrictMath.max(adjusted_level, left_neighbour.level - 1);
		adjusted_level = StrictMath.max(adjusted_level, top_neighbour.level - 1);
		adjusted_level = StrictMath.max(adjusted_level, bottom_neighbour.level - 1);
		return adjusted_level;
	}

	public void init(PatchLevel right, PatchLevel top) {
		initTopNeighbour(top);
		initRightNeighbour(right);
	}

	private void initTopNeighbour(PatchLevel top_neighbour) {
		this.top_neighbour = top_neighbour;
		top_neighbour.bottom_neighbour = this;
	}

	private void initRightNeighbour(PatchLevel right_neighbour) {
		this.right_neighbour = right_neighbour;
		right_neighbour.left_neighbour = this;
	}

}

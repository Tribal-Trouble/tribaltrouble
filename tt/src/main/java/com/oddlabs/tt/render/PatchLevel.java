package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.LandscapeTileTriangle;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ExecutionException;

final class PatchLevel {
	private final @NonNull PatchLevel right_neighbour;
	private final @NonNull PatchLevel left_neighbour;
	private final @NonNull PatchLevel top_neighbour;
	private final @NonNull PatchLevel bottom_neighbour;
	private int level;

    PatchLevel(LandscapeRenderer.@NonNull PatchFinder finder, int x, int y) throws ExecutionException, InterruptedException {
        finder.set(x, y, this);
        left_neighbour = finder.get(x - 1, y);
        right_neighbour = finder.get(x + 1, y);
        bottom_neighbour = finder.get(x, y - 1);
        top_neighbour = finder.get(x, y + 1);
    }

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
}

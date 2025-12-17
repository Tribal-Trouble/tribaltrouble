package com.oddlabs.tt.landscape;

import org.jspecify.annotations.NonNull;

public final class LandscapeLeaf extends AbstractPatchGroup {
	private final int patch_x;
	private final int patch_y;

	public float getMaxError() {
		return 0;
	}

	public LandscapeLeaf(@NonNull World world, int index_x, int index_y, AbstractPatchGroup parent) {
		super(world.getHeightMap(), 1, index_x, index_y, parent);
		HeightMap heightmap = world.getHeightMap();
		this.patch_x = index_x;
		this.patch_y = index_y;
		int patch_offset_x = index_x*heightmap.getMetersPerPatch();
		int patch_offset_y = index_y*heightmap.getMetersPerPatch();
		setBoundsFromLandscape(heightmap, index_x*heightmap.getGridUnitsPerPatch(), index_y*heightmap.getGridUnitsPerPatch(), heightmap.getGridUnitsPerPatch(), heightmap.getGridUnitsPerPatch());
		heightmap.registerLeaf(patch_x, patch_y, this);
	}

	public float[] getErrors() {
		return new float[0];
	}

	public int getPatchX() {
		return patch_x;
	}

	public int getPatchY() {
		return patch_y;
	}

	@Override
	public void visit(@NonNull PatchGroupVisitor visitor) {
		visitor.visitLeaf(this);
	}
}

package com.oddlabs.tt.landscape;

import org.jspecify.annotations.NonNull;

public final class LandscapeLeaf extends AbstractPatchGroup {
	private final float @NonNull [] errors;

	private final int patch_x;
	private final int patch_y;
	private final float max_error;

	public float getMaxError() {
		return max_error;
	}

	public LandscapeLeaf(@NonNull World world, int index_x, int index_y, AbstractPatchGroup parent) {
		super(world.getHeightMap(), 1, index_x, index_y, parent);
		HeightMap heightmap = world.getHeightMap();
		Errors errors_obj = world.getLandscapeIndices().computeErrors(index_x, index_y);
		float[] errors = errors_obj.errors;
		float max_error_tmp = 0;
		for (int i = 0; i < errors.length; i++) {
			float error = errors[i];
			if (error > max_error_tmp)
				max_error_tmp = error;
			if (errors_obj.intersects_water && i < HeightMap.MIN_INTERSECTING_LEVEL)
				error = Float.POSITIVE_INFINITY;

			errors[i] = transformError(error);
		}
		this.max_error = max_error_tmp;
		this.errors = errors;
		this.patch_x = index_x;
		this.patch_y = index_y;
		int patch_offset_x = index_x*heightmap.getMetersPerPatch();
		int patch_offset_y = index_y*heightmap.getMetersPerPatch();
		setBoundsFromLandscape(heightmap, index_x*heightmap.getGridUnitsPerPatch(), index_y*heightmap.getGridUnitsPerPatch(), heightmap.getGridUnitsPerPatch(), heightmap.getGridUnitsPerPatch());
		heightmap.registerLeaf(patch_x, patch_y, this);
	}

	public float[] getErrors() {
		return errors;
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

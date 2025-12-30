package com.oddlabs.tt.landscape;

import com.oddlabs.tt.global.Globals;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class HeightMap {
	public static final int METERS_PER_UNIT_GRID = 2;
	public static final int GRID_UNITS_PER_PATCH_EXP = 4;
	public static final int GRID_UNITS_PER_PATCH = 1 << GRID_UNITS_PER_PATCH_EXP;

	static final int MIN_INTERSECTING_LEVEL = 5;
	private static final Vector3f plane = new Vector3f();

	private final float @NonNull [] @NonNull [] world;
	private final LandscapeLeaf @NonNull [] @NonNull [] landscape_leaves;
	private final List<int @NonNull []> trees;
	private final boolean[][] access_grid;
	private final byte[][] build_grid;
	private final int meters_per_world;
	private final int patches_per_world;
	private final int meters_per_patch;
	private final int grid_units_per_world;
	private final float inv_meters_per_patch;
	private final float inv_meters_per_grid_unit;
	private final float sea_level_meters;
	private final int meters_per_chunk;
	private final int quadtree_min_level;
	private final int patches_per_chunk;
	private final float meters_per_chunk_border;
	private final float chunk_tex_scale;
	private final World world_instance;
    private final com.oddlabs.tt.render.@NonNull Texture heightTexture;

	public HeightMap(World world_instance, int meters_per_world, float sea_level_meters, int texels_per_colormap, int chunks_per_colormap, float@NonNull [] @NonNull [] world, List<int[]> trees, boolean[][] access_grid, byte[][] build_grid) {
		this.world = world;
		this.world_instance = world_instance;
		this.trees = trees;
		this.access_grid = access_grid;
		this.build_grid = build_grid;
		this.meters_per_world = meters_per_world;
		this.sea_level_meters = sea_level_meters;
		patches_per_world = world.length/GRID_UNITS_PER_PATCH;
		meters_per_patch = GRID_UNITS_PER_PATCH*METERS_PER_UNIT_GRID;
		grid_units_per_world = getPatchesPerWorld()*GRID_UNITS_PER_PATCH;
		inv_meters_per_patch = 1f/getMetersPerPatch();
		inv_meters_per_grid_unit = 1f/METERS_PER_UNIT_GRID;
		meters_per_chunk = getMetersPerWorld()/chunks_per_colormap;
		quadtree_min_level = (int)(Math.log(chunks_per_colormap)/Math.log(2));
		patches_per_chunk = meters_per_chunk/getMetersPerPatch();

		int texels_per_colormap_noborder = texels_per_colormap - 2*Globals.TEXELS_PER_CHUNK_BORDER*chunks_per_colormap;
		float meters_per_texel = (float)getMetersPerWorld()/texels_per_colormap_noborder;
		meters_per_chunk_border = meters_per_texel*Globals.TEXELS_PER_CHUNK_BORDER;
		chunk_tex_scale = 1f/(meters_per_chunk + 2f*meters_per_chunk_border);

		landscape_leaves = new LandscapeLeaf[getPatchesPerWorld()][getPatchesPerWorld()];
        
        // Create Height Texture
        java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(grid_units_per_world * grid_units_per_world);
        for (int y = 0; y < grid_units_per_world; y++) {
            for (int x = 0; x < grid_units_per_world; x++) {
                buffer.put(world[y][x]);
            }
        }
        buffer.flip();
        heightTexture = new com.oddlabs.tt.render.Texture(grid_units_per_world, grid_units_per_world, org.lwjgl.opengl.GL30.GL_R32F, org.lwjgl.opengl.GL11.GL_LINEAR, org.lwjgl.opengl.GL11.GL_LINEAR, org.lwjgl.opengl.GL11.GL_REPEAT);
        GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, heightTexture.getHandle());
        GL11.glPixelStorei(org.lwjgl.opengl.GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(org.lwjgl.opengl.GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexSubImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, 0, 0, grid_units_per_world, grid_units_per_world, GL11.GL_RED, GL11.GL_FLOAT, buffer);
	}
    
    public com.oddlabs.tt.render.@NonNull Texture getHeightTexture() {
        return heightTexture;
    }

	public boolean isGridInside(int x, int y) {
		boolean inside_world = x >= 0 && y >= 0 && x < getGridUnitsPerWorld() && y < getGridUnitsPerWorld();
		return inside_world;
	}

	public boolean isInside(float x, float y) {
		boolean inside_world = x >= 0 && y >= 0 && x <= getMetersPerWorld() && y <= getMetersPerWorld();
		return inside_world;
	}

	public int getMetersPerWorld() {
		return meters_per_world;
	}

	public int getGridUnitsPerPatch() {
		return GRID_UNITS_PER_PATCH;
	}

	public float getInvMetersPerGridUnit() {
		return inv_meters_per_grid_unit;
	}

	public int getPatchesPerWorld() {
		return patches_per_world;
	}

	public int getMetersPerPatch() {
		return meters_per_patch;
	}

	public int getGridUnitsPerWorld() {
		return grid_units_per_world;
	}

	public float getSeaLevelMeters() {
		return sea_level_meters;
	}

	public int getMetersPerChunk() {
		return meters_per_chunk;
	}

	public int getQuadtreeMinLevel() {
		return quadtree_min_level;
	}

	public int getPatchesPerChunk() {
		return patches_per_chunk;
	}

	public float getMetersPerChunkBorder() {
		return meters_per_chunk_border;
	}

	public float getChunkTexScale() {
		return chunk_tex_scale;
	}

	void registerLeaf(int x, int y, LandscapeLeaf leaf) {
		landscape_leaves[y][x] = leaf;
	}

	public List<int[]> getTrees() {
		return trees;
	}

	public boolean[][] getAccessGrid() {
		return access_grid;
	}

	void makePlaneVector(int x0, int y0, int x1, int y1, int x2, int y2, @NonNull Vector3f plane) {
		makePlaneVector(x0, y0, getWrappedHeight(x0, y0),
				x1, y1, getWrappedHeight(x1, y1),
				x2, y2, getWrappedHeight(x2, y2), plane);
	}

	private static void makePlaneVector(float h1x, float h1y, float h1z, float h2x, float h2y, float h2z, float h3x, float h3y, float h3z, @NonNull Vector3f plane) {
		float v1x = h2x - h1x;
		float v1y = h2y - h1y;
		float v1z = h2z - h1z;
		float v2x = h3x - h1x;
		float v2y = h3y - h1y;
		float v2z = h3z - h1z;

		Vector3f vec1 = new Vector3f(v1x, v1y, v1z);
        Vector3f vec2 = new Vector3f(v2x, v2y, v2z);
		vec2.cross(vec1);

		// Optimization for planeHeight!
		float inv_z = -1f/vec2.z;
		plane.set(vec2.x*inv_z, vec2.y*inv_z, (-vec2.x*h1x - vec2.y*h1y)*inv_z + h1z);
	}

	static float planeHeight(float x, float y, @NonNull Vector3f plane) {
		return plane.x*x + plane.y*y + plane.z;
	}

	private static float doPlane(float x, float y, float h1x, float h1y, float h1z, float h2x, float h2y, float h2z, float h3x, float h3y, float h3z) {
		makePlaneVector(h1x, h1y, h1z, h2x, h2y, h2z, h3x, h3y, h3z, plane);
		return planeHeight(x, y, plane);
	}

	public boolean isBelowSeaLevel(int patch_x, int patch_y) {
		int offset_x = patch_x*getGridUnitsPerPatch();
		int offset_y = patch_y*getGridUnitsPerPatch();
		for (int y = 0; y < getGridUnitsPerPatch(); y++) {
			for (int x = 0; x < getGridUnitsPerPatch(); x++) {
				float height = getWrappedHeight(offset_x + x, offset_y + y);
				if (height < getSeaLevelMeters())
					return true;
			}
		}
		return false;
	}

	public LandscapeLeaf getLeafFromCoordinates(float x_f, float y_f) {
		int patch_x = coordinateToPatch(x_f);
		int patch_y = coordinateToPatch(y_f);
		return landscape_leaves[patch_y][patch_x];
	}

	public int coordinateToPatch(float f) {
		return (int)(f*inv_meters_per_patch);
	}

	public float computeInterpolatedHeight(int lod, float x_f, float y_f) {
		x_f *= inv_meters_per_grid_unit;
		y_f *= inv_meters_per_grid_unit;
        
        int size = getGridUnitsPerWorld();
        x_f = (x_f % size + size) % size;
        y_f = (y_f % size + size) % size;
        
        int x0 = (int)x_f;
        int y0 = (int)y_f;
        int x1 = (x0 + 1) % size;
        int y1 = (y0 + 1) % size;
        
        float dx = x_f - x0;
        float dy = y_f - y0;
        
        float h00 = world[y0][x0];
        float h10 = world[y0][x1];
        float h01 = world[y1][x0];
        float h11 = world[y1][x1];
        
        float h0 = h00 * (1 - dx) + h10 * dx;
        float h1 = h01 * (1 - dx) + h11 * dx;
        
        return h0 * (1 - dy) + h1 * dy;
	}

	public float getNearestHeight(float x_f, float y_f) {
		return computeInterpolatedHeight(0, x_f, y_f);
	}

	public float getClampedHeight(int grid_x, int grid_y) {
		if (grid_x < 0 || grid_x >= world.length)
			grid_x = 0;
		if (grid_y < 0 || grid_y >= world.length)
			grid_y = 0;

		return getHeight(grid_x, grid_y);
	}

	public byte getBuildValue(int grid_x, int grid_y) {
		return build_grid[grid_y][grid_x];
	}

	public boolean canBuild(int grid_x, int grid_y, int val) {
		grid_x = wrapGridCoord(grid_x);
		grid_y = wrapGridCoord(grid_y);
		return build_grid[grid_y][grid_x] >= val;
	}

	public float getWrappedHeight(int grid_x, int grid_y) {
		grid_x = wrapGridCoord(grid_x);
		grid_y = wrapGridCoord(grid_y);
		return getHeight(grid_x, grid_y);
	}

	private int wrapGridCoord(int coord) {
		return (coord + getGridUnitsPerWorld())&(getGridUnitsPerWorld() - 1);
	}

	public float getHeight(int grid_x, int grid_y) {
		return world[grid_y][grid_x];
	}

	public void editHeight(int grid_x, int grid_y, float height) {
		grid_x = wrapGridCoord(grid_x);
		grid_y = wrapGridCoord(grid_y);
		world[grid_y][grid_x] = height;
        
        java.nio.FloatBuffer buf = org.lwjgl.BufferUtils.createFloatBuffer(1);
        buf.put(height).flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, heightTexture.getHandle());
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, grid_x, grid_y, 1, 1, GL11.GL_RED, GL11.GL_FLOAT, buf);

		int patch_x1 = grid_x/GRID_UNITS_PER_PATCH;
		int patch_y1 = grid_y/GRID_UNITS_PER_PATCH;
		boolean x_border = patch_x1*GRID_UNITS_PER_PATCH == grid_x;
		boolean y_border = patch_y1*GRID_UNITS_PER_PATCH == grid_y;
		int patch_x0 = (patch_x1 - (x_border ? 1 : 0) + patches_per_world)&(patches_per_world - 1);
		int patch_y0 = (patch_y1 - (y_border ? 1 : 0) + patches_per_world)&(patches_per_world - 1);

		for (int y = patch_y0; y <= patch_y1; y++) {
            for (int x = patch_x0; x <= patch_x1; x++) {
                landscape_leaves[y][x].editHeight(height);
            }
        }
		world_instance.getNotificationListener().patchesEdited(patch_x0, patch_y0, patch_x1, patch_y1);
	}
}

package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class WorldInfo {
	public final Texture[][] colormaps;
	public final Texture detail;
	public final float[][] heightmap;
	public final List<int[]> trees;
	public final List<int[]> palm_trees;
	public final List<int[]> rocks;
	public final List<int[]> iron;
	public final float[][] plants;
	public final boolean[][] access_grid;
	public final byte[][] build_grid;
	public final int meters_per_world;
	public final float sea_level_meters;
	public final int texels_per_colormap;
	public final int chunks_per_colormap;
	public final float[][] starting_locations;
	public final @NonNull BlendInfo @NonNull [] blend_infos;

	public WorldInfo(int meters_per_world, float sea_level_meters, int texels_per_colormap, int chunks_per_colormap, Texture[][] colormaps, Texture detail, float[][] heightmap, List<int[]> trees, List<int[]> palm_trees, List<int[]> rocks, List<int[]> iron, float[][] plants, boolean[][] access_grid, byte[][] build_grid, float[][] starting_locations, BlendInfo @NonNull [] blend_infos) {
		this.texels_per_colormap = texels_per_colormap;
		this.chunks_per_colormap = chunks_per_colormap;
		this.sea_level_meters = sea_level_meters;
		this.meters_per_world = meters_per_world;
		this.colormaps = colormaps;
		this.detail = detail;
		this.heightmap = heightmap;
		this.trees = trees;
		this.rocks = rocks;
		this.iron = iron;
		this.plants = plants;
		this.palm_trees = palm_trees;
		this.access_grid = access_grid;
		this.build_grid = build_grid;
		this.starting_locations = starting_locations;
		this.blend_infos = blend_infos;
	}
}

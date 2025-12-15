package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class WorldInfo {
	public final @NonNull Texture @NonNull [] @NonNull [] colormaps;
	public final @NonNull Texture detail;
	public final float @NonNull [] @NonNull [] heightmap;
	public final @NonNull List<int[]> trees;
	public final @NonNull List<int[]> palm_trees;
	public final @NonNull List<int[]> rocks;
	public final @NonNull List<int[]> iron;
	public final float @NonNull [] @NonNull [] plants;
	public final boolean @NonNull  [] @NonNull [] access_grid;
	public final byte @NonNull [] @NonNull [] build_grid;
	public final int meters_per_world;
	public final float sea_level_meters;
	public final int texels_per_colormap;
	public final int chunks_per_colormap;
	public final float @NonNull [] @NonNull [] starting_locations;
	public final @NonNull BlendInfo @NonNull [] blend_infos;

	public WorldInfo(int meters_per_world, float sea_level_meters, int texels_per_colormap, int chunks_per_colormap, @NonNull Texture @NonNull [] @NonNull [] colormaps, @NonNull Texture detail, float @NonNull [] @NonNull [] heightmap, @NonNull List<int[]> trees, @NonNull List<int[]> palm_trees, @NonNull List<int[]> rocks, @NonNull List<int[]> iron, float @NonNull [] @NonNull [] plants, boolean@NonNull [] @NonNull [] access_grid, byte @NonNull [] @NonNull [] build_grid, float @NonNull [] @NonNull [] starting_locations, @NonNull BlendInfo @NonNull [] blend_infos) {
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

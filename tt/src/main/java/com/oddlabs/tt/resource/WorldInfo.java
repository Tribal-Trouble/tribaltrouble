package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record WorldInfo(int meters_per_world, float sea_level_meters, int texels_per_colormap, int chunks_per_colormap,
						@NonNull Texture @Nullable [] @NonNull [] colormaps, Maps maps, @NonNull Texture detail,
						float @NonNull [] @NonNull [] heightmap, @NonNull List<int[]> trees,
						@NonNull List<int[]> palm_trees, @NonNull List<int[]> rocks, @NonNull List<int[]> iron,
						float @NonNull [] @NonNull [] plants, boolean @NonNull [] @NonNull [] access_grid,
						byte @NonNull [] @NonNull [] build_grid, float @NonNull [] @NonNull [] starting_locations,
						@NonNull BlendInfo @NonNull [] blend_infos) {
	public record Maps(Texture diffuse, Texture normal) {
	}

	public WorldInfo(int meters_per_world, float sea_level_meters, int texels_per_colormap, int chunks_per_colormap, @NonNull Texture @Nullable [] @NonNull [] colormaps, Maps maps, @NonNull Texture detail, float @NonNull [] @NonNull [] heightmap, @NonNull List<int @NonNull []> trees, @NonNull List<int @NonNull []> palm_trees, @NonNull List<int[]> rocks, @NonNull List<int[]> iron, float @NonNull [] @NonNull [] plants, boolean @NonNull [] @NonNull [] access_grid, byte @NonNull [] @NonNull [] build_grid, float @NonNull [] @NonNull [] starting_locations, BlendInfo @NonNull [] blend_infos) {
		this.texels_per_colormap = texels_per_colormap;
		this.chunks_per_colormap = chunks_per_colormap;
		this.sea_level_meters = sea_level_meters;
		this.meters_per_world = meters_per_world;
		this.colormaps = colormaps;
		this.maps = maps;
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

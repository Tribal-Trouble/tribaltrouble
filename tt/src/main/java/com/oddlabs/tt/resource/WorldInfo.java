package com.oddlabs.tt.resource;

import com.oddlabs.tt.model.Terrain;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * Configuration and resource details of a generated game world.
 * Generic type T represents the visual texture implementation type (e.g. Texture).
 */
public record WorldInfo<T>(
                           @NonNull Terrain terrain, int meters_per_world, float sea_level_meters,
                           int texels_per_colormap, int chunks_per_colormap,
                           @NonNull T @Nullable [] @NonNull [] colormaps, Maps<T> maps, @NonNull T detail,
                           @NonNull T detailNormal,
                           float @NonNull [] heightmap, @NonNull List<int @NonNull []> trees,
                           @NonNull List<int @NonNull []> palm_trees, @NonNull List<int @NonNull []> rocks,
                           @NonNull List<
                                   int @NonNull []> iron,
                           float @NonNull [] @NonNull [] plants, boolean @NonNull [] @NonNull [] access_grid,
                           byte @NonNull [] @NonNull [] build_grid, float @NonNull [] @NonNull [] starting_locations,
                           @NonNull FogInfo fog_info,
                           @NonNull BlendInfo @NonNull [] blend_infos) {
    public record Maps<T>(T diffuse, T normal) {
    }
}

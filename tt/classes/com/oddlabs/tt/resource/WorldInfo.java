package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.*;
import com.oddlabs.tt.resource.GLIntImage;

import java.util.ArrayList;
import java.util.List;

public final strictfp class WorldInfo {
    public final Texture[][] colormaps;
    public final Texture detail;
    // Structure images used during world-gen for base/material/shadow/seabottom
    public final GLIntImage[] structures;
    public final float[][] heightmap;
    public final List trees;
    public final List palm_trees;
    public final List rocks;
    public final List iron;
    public final float[][] plants;
    public final boolean[][] access_grid;
    public final boolean[][] dock_grid;
    public final boolean[][] water_grid;
    public final byte[][] build_grid;
    public final int meters_per_world;
    public final float sea_level_meters;
    public final int texels_per_colormap;
    public final int chunks_per_colormap;
    public final ArrayList island_locations;
    public final float[][] starting_locations;

    public WorldInfo(
            int meters_per_world,
            float sea_level_meters,
            int texels_per_colormap,
            int chunks_per_colormap,
            Texture[][] colormaps,
            Texture detail,
            GLIntImage[] structures,
            float[][] heightmap,
            List trees,
            List palm_trees,
            List rocks,
            List iron,
            float[][] plants,
            boolean[][] access_grid,
            boolean[][] dock_grid,
            boolean[][] water_grid,
            byte[][] build_grid,
            ArrayList island_locations,
            float[][] starting_locations) {
        this.texels_per_colormap = texels_per_colormap;
        this.chunks_per_colormap = chunks_per_colormap;
        this.sea_level_meters = sea_level_meters;
        this.meters_per_world = meters_per_world;
        this.colormaps = colormaps;
    this.detail = detail;
    this.structures = structures;
        this.heightmap = heightmap;
        this.trees = trees;
        this.rocks = rocks;
        this.iron = iron;
        this.plants = plants;
        this.palm_trees = palm_trees;
        this.access_grid = access_grid;
        this.dock_grid = dock_grid;
        this.water_grid = water_grid;
        this.build_grid = build_grid;
        this.starting_locations = starting_locations;
        this.island_locations = island_locations;
    }
}

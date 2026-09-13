package com.oddlabs.tt.resource;

import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;

public final class IslandGenerator implements WorldGenerator {
    @Serial
    private static final long serialVersionUID = 1;

    private static final int TEXELS_PER_CHUNK = 512;
    private static final int IDEAL_TEXELS_PER_DETAIL = 256;
    private static final float IDEAL_DETAIL_ALPHA = .15f;

    private final int meters_per_world;
    private final Landscape.@NonNull TerrainType terrain;
    private final int grid_units;

    private final float hills;
    private final float vegetation_amount;
    private final float supplies_amount;
    private final int seed;
    private final boolean archipelago;

    public IslandGenerator(int meters_per_world, Landscape.@NonNull TerrainType terrain, float hills,
            float vegetation_amount, float supplies_amount, int seed, boolean archipelago) {
        this.hills = hills;
        this.vegetation_amount = vegetation_amount;
        this.supplies_amount = supplies_amount;
        this.seed = seed;
        this.archipelago = archipelago;
        this.grid_units = meters_per_world / HeightMap.METERS_PER_UNIT_GRID;
        this.meters_per_world = meters_per_world;
        this.terrain = terrain;
    }

    private static @NonNull Texture createDetail(@NonNull GLImage detail_image, int base_level) {
        GLImage[] detail_mipmaps = detail_image.buildMipMaps(base_level, Globals.LANDSCAPE_DETAIL_FADEOUT_FACTOR, true,
                false);
        return new Texture(detail_mipmaps, GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static int getTexelsPerGridUnit() {
        int texels_per_grid_unit = Globals.TEXELS_PER_GRID_UNIT / (int) Math.pow(2,
                Globals.TEXTURE_MIP_SHIFT[Settings.getSettings().graphic_detail]);
        return texels_per_grid_unit;
    }

    @Override
    public Landscape.@NonNull TerrainType getTerrainType() {
        return terrain;
    }

    @Override
    public int getMetersPerWorld() {
        return meters_per_world;
    }

    @Override
    public @NonNull FogInfo getFogInfo() {
        return Landscape.getFogInfo(terrain, meters_per_world);
    }

    private static int detailBaseLevel() {
        int base_level = Globals.LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL;
        int detail_mip_level = IDEAL_TEXELS_PER_DETAIL / Globals.DETAIL_SIZE - 1;
        base_level -= detail_mip_level;
        return Math.min(base_level, 1);
    }

    @Override
    public @NonNull WorldInfo generateTerrain(int num_players, int initial_unit_count, float random_start_pos) {
        int colormap_size = grid_units * getTexelsPerGridUnit();
        int chunks_per_colormap = colormap_size / TEXELS_PER_CHUNK;

        // Build landscape
        Instant time_before = Instant.now();
        int base_level = Globals.LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL;
        int detail_mip_level = IDEAL_TEXELS_PER_DETAIL / Globals.DETAIL_SIZE - 1;
        int detail_prefade_level = Math.max(detail_mip_level - base_level, 0);
        float detail_prefade = IDEAL_DETAIL_ALPHA * (float) Math.pow(Globals.LANDSCAPE_DETAIL_FADEOUT_FACTOR,
                detail_prefade_level);
        Landscape landscape = new Landscape(num_players, meters_per_world, terrain, detail_prefade, hills,
                vegetation_amount, supplies_amount, seed, initial_unit_count, random_start_pos, archipelago);
        Instant time_after = Instant.now();
        IO.println("Landscape created in " + Duration.between(time_before, time_after));
        return new WorldInfo(meters_per_world, landscape.getSeaLevelMeters(),
                colormap_size, chunks_per_colormap, null, null, null, landscape.getDetail(),
                landscape.getHeight(),
                landscape.getTrees(), landscape.getPalmtrees(), landscape.getRock(), landscape.getIron(),
                landscape.getPlants(),
                landscape.getAccessGrid(), landscape.getDockGrid(), landscape.getWaterGrid(),
                landscape.getBuildGrid(), landscape.getIslandIds(), landscape.getIslandInfos(),
                landscape.getStartingLocations(),
                landscape.getBlendInfos(), landscape.getGround(), seed, landscape.getTerrainShadows(),
                landscape.getSupplyShadows());
    }

    @Override
    public @NonNull WorldInfo bakeTextures(@NonNull WorldInfo terrain) {
        Texture detail = createDetail(terrain.detail_image(), detailBaseLevel());
        com.oddlabs.tt.landscape.LandscapeBaker baker = new com.oddlabs.tt.landscape.LandscapeBaker();
        // Original tiled structure textures at colormap_size/STRUCTURE_SIZE repeats.
        float textureScale = (float) terrain.texels_per_colormap() / Globals.STRUCTURE_SIZE;
        WorldInfo.Maps maps = baker.bake(terrain.texels_per_colormap(), textureScale,
                terrain.blend_infos()).withShadows(terrain.terrain_shadows(), terrain.supply_shadows());
        return terrain.withTextures(maps, detail);
    }

    @Override
    public @NonNull WorldInfo generate(int num_players, int initial_unit_count, float random_start_pos) {
        WorldInfo world_info = WorldGenerator.super.generate(num_players, initial_unit_count, random_start_pos);
        ProgressForm.progress();
        return world_info;
    }
}

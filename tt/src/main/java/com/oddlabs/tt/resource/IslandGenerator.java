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
import java.util.List;

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

	public IslandGenerator(int meters_per_world, Landscape.@NonNull TerrainType terrain, float hills, float vegetation_amount, float supplies_amount, int seed) {
		this.hills = hills;
		this.vegetation_amount = vegetation_amount;
		this.supplies_amount = supplies_amount;
		this.seed = seed;
		this.grid_units = meters_per_world/HeightMap.METERS_PER_UNIT_GRID;
		this.meters_per_world = meters_per_world;
		this.terrain = terrain;
	}

		private static @NonNull Texture createDetail(@NonNull GLImage detail_image, int base_level) {
			GLImage[] detail_mipmaps = detail_image.buildMipMaps(base_level, Globals.LANDSCAPE_DETAIL_FADEOUT_FACTOR, true, false);
			return new Texture(detail_mipmaps, GL11.GL_RGBA, GL11.GL_LINEAR_MIPMAP_LINEAR,
									   GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
		}
	private int getTexelsPerGridUnit() {
		int texels_per_grid_unit = Globals.TEXELS_PER_GRID_UNIT/(int)Math.pow(2, Globals.TEXTURE_MIP_SHIFT[Settings.getSettings().graphic_detail]);
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

	public @NonNull FogInfo createFogInfo() {
		return Landscape.getFogInfo(terrain, meters_per_world);
	}

	@Override
	public @NonNull WorldInfo generate(int num_players, int initial_unit_count, float random_start_pos) {
		int colormap_size = grid_units*getTexelsPerGridUnit();
		int chunks_per_colormap = colormap_size/TEXELS_PER_CHUNK;

		// Build landscape
		long time_before = System.currentTimeMillis();
		int base_level = Globals.LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL;
		int detail_mip_level = IDEAL_TEXELS_PER_DETAIL/Globals.DETAIL_SIZE - 1;
		int detail_prefade_level = Math.max(detail_mip_level - base_level, 0);
		float detail_prefade = IDEAL_DETAIL_ALPHA*(float)Math.pow(Globals.LANDSCAPE_DETAIL_FADEOUT_FACTOR, detail_prefade_level);
		base_level -= detail_mip_level;
		if (base_level < 1)
			base_level = 1;
		Landscape landscape = new Landscape(num_players, meters_per_world, terrain, detail_prefade, hills, vegetation_amount, supplies_amount, seed, initial_unit_count, random_start_pos);
		long time_after = System.currentTimeMillis();
			IO.println("Landscape created in = " + (time_after - time_before));
		BlendInfo[] blend_infos = landscape.getBlendInfos();
		Texture detail = createDetail(landscape.getDetail(), base_level);
		float[][] heightmap = landscape.getHeight();
/*for (int y = 0; y < heightmap.length; y++)
	for (int x = 0; x < heightmap[y].length; x++)
		heightmap[y][x] = y/10;*/
		List<int[]> trees = landscape.getTrees();
		List<int[]> palm_trees = landscape.getPalmtrees();
		List<int[]> rock = landscape.getRock();
		List<int[]> iron = landscape.getIron();
		float[][] plants = landscape.getPlants();
		boolean[][] access_grid = landscape.getAccessGrid();
		byte[][] build_grid= landscape.getBuildGrid();
		float[][] starting_locations = landscape.getStartingLocations();
		// int alpha_size = grid_units;
		// Texture[][] chunk_maps = blendTextures(chunks_per_colormap, blend_infos, alpha_size, Globals.STRUCTURE_SIZE, colormap_size/alpha_size);
        Texture[][] chunk_maps = null;
		ProgressForm.progress();
		return new WorldInfo(meters_per_world, landscape.getSeaLevelMeters(), colormap_size, chunks_per_colormap, chunk_maps, detail, heightmap, trees, palm_trees, rock, iron, plants, access_grid, build_grid, starting_locations, blend_infos);
	}

}

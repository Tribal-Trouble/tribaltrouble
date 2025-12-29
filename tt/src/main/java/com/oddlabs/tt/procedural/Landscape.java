package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.BlendLighting;
import com.oddlabs.tt.resource.DistanceFogInfo;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.resource.StructureBlend;
import com.oddlabs.util.Color;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Landscape {
	public static final boolean DEBUG = false;
	private static final int STRUCTURE_SEED = 42; // must be constant; otherwise distinct repeating patterns might appear

	private static final int NUM_PLANT_TYPES = 4;

	public static final float[] NATIVE_SEA_BOTTOM_COLOR = Color.argb4f(0xFF_73_40_99);
	public static final float[] VIKING_SEA_BOTTOM_COLOR = Color.argb4f(0xFF_1A_33_3D);  // (Dark Blue-Green)

	private static final float[] NATIVE_FOG_COLOR = Color.argb4f(0xFF_A5_BF_FF);
	private static final float[] VIKING_FOG_COLOR = Color.argb4f(0xFF_33_66_8C);
	private static final float NATIVE_FOG_DENSITY = 0.0012f;
	private static final float VIKING_FOG_DENSITY = 0.0016f;
	private static final float NATIVE_FOG_HEIGHT = 1.2f;
	private static final float VIKING_FOG_HEIGHT = 1.4f;

	// Native terrain colors (RGB)
	private static final float[] NATIVE_SAND_COLOR = Color.rgb3f(0xFF_E6_CC);
	private static final float[] NATIVE_DIRT_COLOR = Color.rgb3f(0xFF_B3_80);
	private static final float[] NATIVE_GRASS_COLOR = Color.rgb3f(0x33_73_00);
	private static final float[] NATIVE_ROCK_TINT = Color.rgb3f(0xFF_CC_99);

	// Viking terrain colors (RGB)
	private static final float[] VIKING_GRAVEL_COLOR = Color.rgb3f(0xB38C66);
	private static final float[] VIKING_SOIL_COLOR = Color.rgb3f(0xA68059);
	private static final float[] VIKING_GRASS_COLOR = Color.rgb3f(0x337300); // Same as native, but gets color-shifted
	private static final float[] VIKING_SNOW_COLOR = Color.rgb3f(0xF2F2F2);
	private static final float[] VIKING_CLIFF_GRASS_TINT = Color.rgb3f(0x337300);

	// Misc colors
	private static final float DETAIL_GREY = Color.rgb3f(0x808080)[0];
	private static final float[] BLEND_LIGHTING_COLOR = Color.rgb3f(0xFFE699); // 1.0, 0.9, 0.6

	public enum TerrainType { NATIVE, VIKING }

    private final @NonNull Random random;
	private final @NonNull BlendInfo @NonNull [] blend_infos;
    private static final class StructureLayers {
        final Layer diffuse;
        final Layer normal;
        
        StructureLayers(Layer diffuse, Layer normal) {
            this.diffuse = diffuse;
            this.normal = normal;
        }
    }

	private GLIntImage[] structures;
    private GLIntImage[] structure_normals;
	private GLIntImage detail;
	private @NonNull GLByteImage[] alpha_maps;

	private Channel height;
	private Channel slope;
	private Channel access;
	private Channel access_exported;
	private Channel relheight;
	private Channel highlight;
	private Channel shadow;
	private Channel trees;
	private Channel palmtrees;
	private Channel rock;
	private Channel iron;

    private final int num_players;
	private final int meters_per_world;
	private final int meters_per_height_unit;
	private final int unit_grids_per_world;
	private final int height_scale;
	private final float sea_level_meters;
	private final int detail_size;
	private final int structure_size;
	private final float detail_alpha_value;
	private final int features;
	private final float hills;
	private final float vegetation_amount;
	private final float supplies_amount;
	private final int seed;
	private final float area;
	private final int max_trees;
	private final int max_palmtrees;
	private final int max_rock;
	private final int max_iron;
	private final int max_plants;
	private final float access_threshold;
	private final float build_threshold;
	private final @NonNull TerrainType terrain;

	private byte @NonNull [] @NonNull [] build;
	private float @NonNull [] @NonNull [] player_locations;
	private int @NonNull [] @NonNull [] supply_locations;
	private float @NonNull [] @NonNull [] plants;

	public Landscape(int num_players, int meters_per_world, @NonNull TerrainType terrain, float detail_alpha_value, float hills, float vegetation_amount, float supplies_amount, int seed, int initial_unit_count, float random_start_pos) {
		this.terrain = terrain;
		hills = (float)Math.sqrt(hills);
		this.num_players = num_players;
		this.features = 4;
		this.hills = hills;
		this.vegetation_amount = 0.25f + 0.75f*vegetation_amount;
		this.supplies_amount = 0.25f + 0.75f*supplies_amount;
		this.seed = seed;
		this.meters_per_world = meters_per_world;
		this.unit_grids_per_world = meters_per_world/HeightMap.METERS_PER_UNIT_GRID;
		this.meters_per_height_unit = meters_per_world/unit_grids_per_world;
		int height_scale = 0;
		float access_threshold = 0f;
        int size_multiplier;
        switch (meters_per_world) {
            case 256 -> {
                size_multiplier = 1;
                height_scale = 32;
                access_threshold = 0.05f;
            }
            case 512 -> {
                size_multiplier = 4;
                height_scale = 48;
                access_threshold = 0.0375f;
            }
            case 1024 -> {
                size_multiplier = 16;
                height_scale = 64;
                access_threshold = 0.025f;
            }
            default -> {
                size_multiplier = 0;
                assert false : "illegal meters_per_world";
            }
        }
		this.height_scale = height_scale;
		this.access_threshold = access_threshold;
		this.build_threshold = access_threshold/2f;
		this.sea_level_meters = height_scale*Globals.SEA_LEVEL;
		this.detail_size = Globals.DETAIL_SIZE;
		this.structure_size = Globals.STRUCTURE_SIZE;
		this.detail_alpha_value = detail_alpha_value;

		area = size_multiplier *10000f;
		max_plants = size_multiplier *64;

		if (terrain == TerrainType.NATIVE) {
			max_trees = (int)Math.pow(2, 2*Utils.powerOf2Log2(meters_per_world) - 9);
			max_palmtrees = max_trees>>1;
		} else {
			max_trees = (int)(.75f*Math.pow(2, 2*Utils.powerOf2Log2(meters_per_world) - 9));
			max_palmtrees = max_trees;
		}

		max_rock = max_trees>>3;
		max_iron = max_trees>>4;
		random = new Random(seed);

		// generate shared voronoi and noise maps
		float c1 = -1f;
		float c2 = 1f;
		float c3 = 0f;
		Voronoi voronoi;
		Channel voronoi4 = new Voronoi(structure_size, 4, 4, 1, 1f, STRUCTURE_SEED).getDistance(c1, c2, c3);
		voronoi = new Voronoi(structure_size, 8, 8, 1, 1f, STRUCTURE_SEED);
		Channel voronoi8 = voronoi.getDistance(c1, c2, c3);
		Channel voronoi8_hit = voronoi.getHitpoint();
		voronoi = new Voronoi(structure_size, 16, 16, 1, 1f, STRUCTURE_SEED);
		Channel voronoi16 = voronoi.getDistance(c1, c2, c3);
		Channel voronoi16_hit = voronoi.getHitpoint();
		voronoi = new Voronoi(structure_size, 32, 32, 1, 1f, STRUCTURE_SEED);
		Channel voronoi32 = voronoi.getDistance(c1, c2, c3);
		Channel voronoi32_hit = voronoi.getHitpoint();
		Channel noise8 = new Midpoint(structure_size, 3, 0.45f, STRUCTURE_SEED).toChannel();
		Channel noise256 = new Midpoint(structure_size, 8, 1f, STRUCTURE_SEED).toChannel();

        StructureLayers[] layers = switch (terrain) {
            case NATIVE -> {
                var natives = generateStructuresNative(voronoi4, voronoi8, voronoi8_hit, voronoi16, voronoi16_hit, voronoi32, voronoi32_hit, noise8, noise256);
                ProgressForm.progress();
                generateTerrainNative();
				yield natives;
            }
            case VIKING -> {
                var vikings = generateStructuresViking(voronoi4, voronoi8, voronoi8_hit, voronoi16, voronoi16_hit, voronoi32, voronoi32_hit, noise8, noise256);
                ProgressForm.progress();
                generateTerrainViking();
				yield vikings;
            }
        };
        
        this.structures = new GLIntImage[layers.length];
        this.structure_normals = new GLIntImage[layers.length];
        for (int i = 0; i < layers.length; i++) {
            this.structures[i] = new GLIntImage(layers[i].diffuse);
            this.structure_normals[i] = new GLIntImage(layers[i].normal);
        }

		if (DEBUG) height.toLayer().saveAsPNG("height");
		ProgressForm.progress();
		Channel grass_alpha = generateAlphas();
		ProgressForm.progress();
		generateUnitLocations(initial_unit_count, random_start_pos);
		generateSupplies(grass_alpha);

		// scale height map vertically
		for (int y = 0; y < unit_grids_per_world; y++) {
			for (int x = 0; x < unit_grids_per_world; x++) {
				height.putPixel(x, y, height_scale*height.getPixel(x, y));
			}
		}

		if (DEBUG) access.toLayer().saveAsPNG("access_connected");

		// create blend infos
		blend_infos = new BlendInfo[]{
			new StructureBlend(structures[0], structure_normals[0], new GLByteImage(new Channel(1, 1).fill(1f), GL11.GL_RED)),
			new StructureBlend(structures[1], structure_normals[1], alpha_maps[0]),
			new StructureBlend(structures[2], structure_normals[2], alpha_maps[1]),
			new StructureBlend(structures[3], structure_normals[3], alpha_maps[2]),
			new StructureBlend(structures[4], structure_normals[4], alpha_maps[3]),
			new BlendLighting(alpha_maps[4], 1f, 0.9f, 0.6f),
			new StructureBlend(structures[5], structure_normals[5], alpha_maps[5]),
			new StructureBlend(structures[6], structure_normals[6], alpha_maps[6])
		};
	}

	// **************
	// * STRUCTURES *
	// **************
	private @NonNull StructureLayers @NonNull [] generateStructuresNative(@NonNull Channel voronoi4, @NonNull Channel voronoi8, Channel voronoi8_hit, @NonNull Channel voronoi16, Channel voronoi16_hit, @NonNull Channel voronoi32, Channel voronoi32_hit, @NonNull Channel noise8, @NonNull Channel noise256) {
		var structures = new StructureLayers[7];
		ProgressForm.progress(1/8f);

		structures[0] = Landscape.genSand(structure_size, noise8.copy(), noise256.copy());

		structures[1] = Landscape.genDirt(structure_size, noise8.copy(), noise256.copy(), voronoi32.copy());

		Layer structure_rubble = Landscape.genRubble(structure_size, noise8.copy(), voronoi4.copy(), voronoi8.copy(), voronoi16.copy(), structures[1].diffuse.copy()).diffuse; // Rubble uses dirt as base? Wait, check old code.
        // Old code: Layer structure_rubble = Landscape.genRubble(..., structure_dirt.copy());
        // genRubble logic: rubble.multiply(.9f).bump(...)
        // I need to pass the base layer.
        structures[2] = Landscape.genRubble(structure_size, noise8.copy(), voronoi4.copy(), voronoi8.copy(), voronoi16.copy(), structures[1].diffuse.copy());

		structures[4] = genGrass(structure_size, noise8.copy(), noise256.copy());

		        structures[3] = Landscape.genRock(structure_size, noise8.copy(), noise256.copy(), voronoi4.copy(), voronoi8.copy(), voronoi16.copy(), structures[2].diffuse.copy(), structures[4].diffuse.copy());
		
						structures[5] = Landscape.genBlack();
				
						structures[6] = Landscape.genSeabottom(terrain, structure_size, noise8.copy(), noise256.copy(), voronoi4.copy(), voronoi8.copy());
				
						StructureLayers structure_detail = Landscape.genDetail(detail_size, detail_alpha_value, STRUCTURE_SEED, noise8.copy());
						detail = new GLIntImage(structure_detail.diffuse);		return structures;
	}

	private @NonNull StructureLayers @NonNull [] generateStructuresViking(@NonNull Channel voronoi4, @NonNull Channel voronoi8, Channel voronoi8_hit, @NonNull Channel voronoi16, Channel voronoi16_hit, @NonNull Channel voronoi32, Channel voronoi32_hit, @NonNull Channel noise8, @NonNull Channel noise256) {
		var structures = new StructureLayers[7];
		ProgressForm.progress(1/8f);

		structures[0] = Landscape.genGravel(structure_size, noise8.copy(), noise256.copy());

		structures[1] = Landscape.genSoil(structure_size, noise8.copy(), noise256.copy(), voronoi32.copy());

		structures[3] = genGrass(structure_size, noise8.copy(), noise256.copy());
        structures[3].diffuse.multiply(.75f, .9f, 1.1f);

		structures[2] = genCliff(structure_size, noise8.copy(), noise256.copy(), voronoi4.copy(), voronoi8.copy(), voronoi16.copy(), structures[1].diffuse.copy(), structures[3].diffuse.copy());

		structures[4] = Landscape.genSnow(structure_size, noise8.copy(), noise256.copy());

		structures[5] = Landscape.genBlack();

		structures[6] = Landscape.genSeabottom(terrain, structure_size, noise8.copy(), noise256.copy(), voronoi4.copy(), voronoi8.copy());

		StructureLayers structure_detail = Landscape.genDetail(detail_size, detail_alpha_value, STRUCTURE_SEED, noise8.copy());
		detail = new GLIntImage(structure_detail.diffuse);

		return structures;
	}

	private static @NonNull StructureLayers genSand(int size, @NonNull Channel noise8, @NonNull Channel noise256) {
		Channel empty = new Channel(size, size).fill(1f);
		Channel sand_bump1 = noise8.brightness(0.75f);
		Channel sand_bump2 = noise256.brightness(0.15f);
		Layer sand = new Layer(empty.copy().fill(NATIVE_SAND_COLOR[0]), empty.copy().fill(NATIVE_SAND_COLOR[1]), empty.copy().fill(NATIVE_SAND_COLOR[2]));
        Channel bump = sand_bump1.channelAdd(sand_bump2);
		sand.bump(bump, size/1024f, 0f, 0.1f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) sand.saveAsPNG("structure_sand");
        
		return new StructureLayers(sand, getFlatNormal(size)); // Forced flat normal
	}

	private static @NonNull StructureLayers genGravel(int size, @NonNull Channel noise8, @NonNull Channel noise256) {
		Channel empty = new Channel(size, size).fill(1f);
		Channel gravel_bump1 = noise8.brightness(0.5f);
		Channel gravel_bump2 = noise256.brightness(0.5f);
		Layer gravel = new Layer(empty.copy().fill(VIKING_GRAVEL_COLOR[0]), empty.copy().fill(VIKING_GRAVEL_COLOR[1]), empty.copy().fill(VIKING_GRAVEL_COLOR[2]));
        Channel bump = gravel_bump1.channelAdd(gravel_bump2);
		gravel.bump(bump, size/1024f, 0f, 0.1f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) gravel.saveAsPNG("structure_gravel");
        
		return new StructureLayers(gravel, getFlatNormal(size)); // Forced flat normal
	}

	private static @NonNull StructureLayers genDirt(int size, @NonNull Channel noise8, @NonNull Channel noise256, @NonNull Channel voronoi32) {
		Channel empty = new Channel(size, size).fill(1f);
		Channel dirt_bump1 = noise8.brightness(0.8f);
		Channel dirt_bump2 = noise256.brightness(0.1f);
		Channel dirt_bump3 = voronoi32.brightness(0.1f);
		Layer dirt = new Layer(empty.copy().fill(NATIVE_DIRT_COLOR[0]), empty.copy().fill(NATIVE_DIRT_COLOR[1]), empty.copy().fill(NATIVE_DIRT_COLOR[2]));
		Channel dirt_bump = dirt_bump1.channelAdd(dirt_bump2).channelAdd(dirt_bump3);
		dirt_bump.perturb(noise8, 0.05f);
		dirt.bump(dirt_bump, size/128f, 0f, 0.5f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) dirt.saveAsPNG("structure_dirt");
        
		return new StructureLayers(dirt, getFlatNormal(size)); // Forced flat normal
	}

	private static @NonNull StructureLayers genSoil(int size, @NonNull Channel noise8, @NonNull Channel noise256, @NonNull Channel voronoi32) {
		Channel empty = new Channel(size, size).fill(1f);
		Channel soil_bump1 = noise8.brightness(0.8f);
		Channel soil_bump2 = noise256.brightness(0.1f);
		Channel soil_bump3 = voronoi32.brightness(0.1f);
		Layer soil = new Layer(empty.copy().fill(VIKING_SOIL_COLOR[0]), empty.copy().fill(VIKING_SOIL_COLOR[1]), empty.copy().fill(VIKING_SOIL_COLOR[2]));
		Channel soil_bump = soil_bump1.channelAdd(soil_bump2).channelAdd(soil_bump3);
		soil_bump.perturb(noise8, 0.05f);
		soil.bump(soil_bump, size/128f, 0f, 0.5f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) soil.saveAsPNG("structure_soil");
        
        Channel zero = new Channel(size, size).fill(0f);
		return new StructureLayers(soil, soil_bump.toNormalMap(1.5f, zero));
	}

	private @NonNull StructureLayers genGrass(int size, @NonNull Channel noise8, @NonNull Channel noise256) {
		Channel empty = new Channel(size, size).fill(1f);
		Channel grass_bump = noise8.copy().rotate(90).channelAdd(noise256.brightness(0.02f));
		float[] grassColor = terrain == TerrainType.NATIVE ? NATIVE_GRASS_COLOR : VIKING_GRASS_COLOR;
		Layer grass = new Layer(empty.copy().fill(grassColor[0]), empty.copy().fill(grassColor[1]), empty.copy().fill(grassColor[2]));
		grass.r.channelAdd(noise8.brightness(0.2f));
		grass.bump(grass_bump, size/256f, 0f, 0.6f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) grass.saveAsPNG("structure_grass");
        
        Channel zero = new Channel(size, size).fill(0f);
		return new StructureLayers(grass, grass_bump.toNormalMap(0.5f, zero));
	}

	private static @NonNull StructureLayers genRubble(int size, @NonNull Channel noise8, @NonNull Channel voronoi4, @NonNull Channel voronoi8, @NonNull Channel voronoi16, @NonNull Layer rubble) {
		Channel rubble_bump1 = voronoi4.multiply(0.4f);
		Channel rubble_bump2 = voronoi8.multiply(0.3f);
		Channel rubble_bump3 = voronoi16.multiply(0.2f);
		Channel rubble_bump = rubble_bump1.channelAdd(rubble_bump2).channelAdd(rubble_bump3).dynamicRange();
		rubble_bump.perturb(noise8, 0.1f);
		rubble.multiply(.9f).bump(rubble_bump, size/128f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) rubble.saveAsPNG("structure_rubble");
        
        Channel zero = new Channel(size, size).fill(0f);
		return new StructureLayers(rubble, rubble_bump.toNormalMap(2.5f, zero));
	}

	private static @NonNull StructureLayers genRock(int size, @NonNull Channel noise8, @NonNull Channel noise256, @NonNull Channel voronoi4, @NonNull Channel voronoi8, @NonNull Channel voronoi16, @NonNull Layer rubble, @NonNull Layer grass) {
		Channel rock_bump1 = voronoi4.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.35f);
		Channel rock_bump2 = voronoi8.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
		Channel rock_bump3 = voronoi16.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.2f);
		Channel rock_bump = rock_bump1.channelAdd(rock_bump2).channelAdd(rock_bump3).channelAdd(noise256.multiply(0.4f));
		rock_bump.perturb(noise8, 0.1f);
		Layer rock = rubble.copy();
		rock.toHSV();
		rock.r = noise8.copy().dynamicRange(0.05f, 0.1f);
		rock.toRGB();
		rock.layerBlend(rubble.multiply(NATIVE_ROCK_TINT[0], NATIVE_ROCK_TINT[1], NATIVE_ROCK_TINT[2]), noise8.gamma8().invert().contrast(4f));
		rock.layerBlend(grass.multiply(0.5f), noise8.rotate(90).multiply(0.5f)); // grass tint
		rock.bump(rock_bump, size/192f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f);
		rock.gamma2().multiply(0.9f);
		if (DEBUG) rock.saveAsPNG("structure_rock");
		
		Channel mica = noise256.copy().gamma(0.5f).threshold(0.6f, 1.0f).multiply(0.8f);
		Layer normalMapLayer = rock_bump.toNormalMap(3.0f, mica);
		if (DEBUG) normalMapLayer.saveAsPNG("structure_rock_normal");
		
		return new StructureLayers(rock, normalMapLayer);
	}

	private static @NonNull StructureLayers genCliff(int size, @NonNull Channel noise8, @NonNull Channel noise256, @NonNull Channel voronoi4, @NonNull Channel voronoi8, @NonNull Channel voronoi16, @NonNull Layer rubble, @NonNull Layer grass) {
		Channel cliff_bump1 = voronoi4.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
		Channel cliff_bump2 = voronoi8.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.3f);
		Channel cliff_bump3 = voronoi16.dynamicRange().contrast(1.1f).brightness(2f).gamma2().multiply(0.25f);
		Channel cliff_bump = cliff_bump1.channelAdd(cliff_bump2).channelAdd(cliff_bump3).channelAdd(noise256.multiply(0.15f));
		cliff_bump.dynamicRange().perturb(noise8, 0.1f);
		Layer cliff = rubble.copy();
		cliff.toHSV();
		cliff.r = noise8.copy().dynamicRange(0.05f, 0.1f);
		cliff.g.multiply(0.75f);
		cliff.toRGB();
		cliff.layerBlend(rubble, noise8.gamma8().invert().contrast(4f));
		cliff.layerBlend(grass.multiply(VIKING_CLIFF_GRASS_TINT[0], VIKING_CLIFF_GRASS_TINT[1], VIKING_CLIFF_GRASS_TINT[2]), noise8.rotate(90).multiply(0.75f));
		cliff.bump(cliff_bump, size/192f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f);
		cliff.gamma2().multiply(0.9f);
		if (DEBUG) cliff.saveAsPNG("structure_cliff");
        
        Channel zero = new Channel(size, size).fill(0f);
		return new StructureLayers(cliff, cliff_bump.toNormalMap(3.0f, zero));
	}

	private static @NonNull StructureLayers genSnow(int size, @NonNull Channel noise8, @NonNull Channel noise256) {
		Channel empty = new Channel(size, size).fill(1f);
		Channel snow_bump1 = noise8.brightness(0.75f);
		Channel snow_bump2 = noise256.brightness(0.25f);
		Layer snow = new Layer(empty.copy().fill(VIKING_SNOW_COLOR[0]), empty.copy().fill(VIKING_SNOW_COLOR[1]), empty.copy().fill(VIKING_SNOW_COLOR[2]));
        Channel bump = snow_bump1.channelAdd(snow_bump2);
		snow.bump(bump, size/1024f, 0f, 0.1f, 1f, 1f, 1f, 0f, 0f, 0f);
		if (DEBUG) snow.saveAsPNG("structure_snow");
        
        Channel snowSpecular = snow_bump1.copy().multiply(0.2f); // Subtle specular based on roughness
		return new StructureLayers(snow, bump.toNormalMap(0.5f, snowSpecular));
	}

	private static @NonNull StructureLayers genBlack() {
		return new StructureLayers(new Channel(1, 1).fill(0f).toLayer(), getFlatNormal(1));
	}

	private static @NonNull StructureLayers genSeabottom(@NonNull TerrainType terrain, int size, @NonNull Channel noise8, @NonNull Channel noise256, @NonNull Channel voronoi4, @NonNull Channel voronoi8) {
		float[] color = switch (terrain) {
			case NATIVE -> NATIVE_SEA_BOTTOM_COLOR;
			case VIKING -> VIKING_SEA_BOTTOM_COLOR;
		};
		Layer seabottom = new Layer(
			new Channel(size, size).fill(color[0]),
			new Channel(size, size).fill(color[1]),
			new Channel(size, size).fill(color[2]));
		if (DEBUG) seabottom.saveAsPNG("structure_seabottom");
        
        Channel zeroSpecular = new Channel(size, size).fill(0f);
        Layer normalMapLayer;

        switch (terrain) {
            case NATIVE -> {
                // Sand dunes/ripples
                Channel seabottom_bump = noise8.copy().channelAdd(noise256.brightness(0.1f)).dynamicRange();
                normalMapLayer = seabottom_bump.toNormalMap(0.3f, zeroSpecular); // Subtle sand dunes
            }
            case VIKING -> {
                // Slime-covered, rock-strewn
                // Combine Voronoi for larger rocks and noise for slime/undulations
                Channel rock_texture = voronoi4.copy().dynamicRange(0.2f, 1f).multiply(0.7f); // Larger rock shapes
                Channel slime_undulations = noise256.copy().brightness(0.08f); // Subtle slime undulations
                Channel viking_seabottom_bump = rock_texture.channelAdd(slime_undulations).dynamicRange();
                
                // Subtle wet specular for slime/wet rocks
                Channel wetSpecular = voronoi4.copy().dynamicRange(0.0f, 0.15f); // Slightly more visible specular
                normalMapLayer = viking_seabottom_bump.toNormalMap(1.2f, wetSpecular); // Stronger normal for rocks, subtle specular
            }
            default -> {
                normalMapLayer = getFlatNormal(size); // Fallback
            }
        }
		return new StructureLayers(seabottom, normalMapLayer);
	}

	private static @NonNull StructureLayers genDetail(int size, float detail_alpha_value, int seed, @NonNull Channel noise8) {
		Channel detail_noise = new Midpoint(size, 4, 0.4f, seed).toChannel();
		detail_noise.perturb(noise8.scale(size, size), 0.05f);
		Channel detail_grey = new Channel(size, size).fill(DETAIL_GREY);
		detail_grey.bump(detail_noise, size/64f, 0f, 0f, 1f, 0f).dynamicRange();
		Channel detail_alpha = new Channel(size, size).fill(detail_alpha_value);
		Layer detail = new Layer(detail_grey, detail_grey, detail_grey, detail_alpha);
		if (DEBUG) detail.saveAsPNG("structure_detail");
		return new StructureLayers(detail, getFlatNormal(size));
	}
    
    private static @NonNull Layer getFlatNormal(int size) {
        return new Layer(
            new Channel(size, size).fill(0.5f),
            new Channel(size, size).fill(0.5f),
            new Channel(size, size).fill(1.0f),
            new Channel(size, size).fill(0.0f) // Specular = 0
        );
    }


	// ***********
	// * TERRAIN *
	// ***********
	private void generateTerrainNative() {
		alpha_maps = new GLByteImage[7];

		// generate height map
		height = new Mountain(unit_grids_per_world, Utils.powerOf2Log2(unit_grids_per_world) - 6, 0.5f, seed).toChannel().multiply(0.67f);
		Voronoi voronoi = new Voronoi(unit_grids_per_world, features, features, 1, 1f, seed);
		Channel cliffs = voronoi.getDistance(-1f, 1f, 0f).brightness(1.5f).multiply(0.33f);
		height.channelAdd(cliffs);

		// Fist of God (tm)
		if (unit_grids_per_world > 128) {
			height.channelSubtract(voronoi.getDistance(1f, 0f, 0f).gamma(.5f).flipV().rotate(90));
		} else {
			height.channelSubtract(voronoi.getDistance(-1f, 1f, 0f).gamma(.5f).flipV().rotate(90));
		}

		height.perturb(new Midpoint(unit_grids_per_world, 2, 0.5f, seed).toChannel(), 0.25f);
		Channel shape = new Hill(unit_grids_per_world, Hill.OVAL).toChannel();
		height.channelAdd(shape.copy().multiply(0.15f));
		height.channelSubtract(shape.copy().invert().multiply(0.5f));
 		height.erode((24f - hills*12f)/unit_grids_per_world, unit_grids_per_world>>2);
		height.channelMultiply(shape.gamma2());
		height.smooth(1);
		height = Landscape.beaches(height);

		// fix edges
		for (int y = 0; y < unit_grids_per_world; y += (unit_grids_per_world - 1)) {
			for (int x = 0; x < unit_grids_per_world; x++) {
				height.putPixel(x, y, 0f);
			}
		}
		for (int y = 1; y < unit_grids_per_world - 1; y++) {
			for (int x = 0; x < unit_grids_per_world; x += (unit_grids_per_world - 1)) {
				height.putPixel(x, y, 0f);
			}
		}

		slope = height.copy().lineart();
		if (DEBUG) slope.copy().dynamicRange().toLayer().saveAsPNG("slope");
		relheight = height.copy().relativeIntensityNormalized(Math.max(1, unit_grids_per_world>>5));
		if (DEBUG) relheight.toLayer().saveAsPNG("relheight");
		access = generateThresholdMap(slope, access_threshold).largestConnected(1f);
		access_exported = access.copy();
		if (DEBUG) access.toLayer().saveAsPNG("access");
		build = Landscape.generateBuildMap(generateThresholdMap(slope, build_threshold).channelMultiply(access));
	}

	private void generateTerrainViking() {
		alpha_maps = new GLByteImage[7];

		// generate height map
		height = new Mountain(unit_grids_per_world, Utils.powerOf2Log2(unit_grids_per_world) - 6, 0.5f, seed).toChannel().add(.5f).dynamicRange().gamma2().multiply(0.67f);

		Voronoi voronoi = new Voronoi(unit_grids_per_world, 8, 8, 1, 1f, seed, true);
		Channel cliffs = voronoi.getDistance(-1f, 1f, 0f).brightness(1.25f).multiply(0.33f);
		height.channelAdd(cliffs).dynamicRange();

		// Fist of God (tm)
		Voronoi voronoi2 = new Voronoi(unit_grids_per_world, 4, 4, 1, 1f, seed);
		if (unit_grids_per_world > 128) {
			height.channelSubtract(voronoi2.getDistance(1f, 0f, 0f).gamma(.5f).multiply(.5f));
		} else {
			height.channelSubtract(voronoi2.getDistance(-1f, 1f, 0f).gamma(.5f).multiply(.5f));
		}

		Channel hitpoint = voronoi.getHitpoint().smooth(1);
		Channel hitpoint2 = hitpoint.copy().erodeThermal(4f/unit_grids_per_world, unit_grids_per_world>>3);
		Channel noise = new Midpoint(unit_grids_per_world, 3, 0.25f, seed).toChannel().threshold(0.75f*hills, 1f);
		Channel heightcut = hitpoint.channelMultiply(noise.copy().invert()).channelAdd(hitpoint2.copy().channelMultiply(noise));
		height.channelMultiply(heightcut);
		height.perturb(new Midpoint(unit_grids_per_world, 2, 0.5f, seed).toChannel(), 0.25f);
		height.erode((24f - hills*12f)/unit_grids_per_world, unit_grids_per_world>>2);

		Channel shape = new Hill(unit_grids_per_world, Hill.SQUARE).toChannel().smoothGain().gamma8();
		height.channelMultiply(shape);

		// add roughness to inaccessible areas
		slope = height.copy().lineart();
		Channel peakarea = slope.threshold(0f, access_threshold).largestConnected(1f).invert().channelMultiply(hitpoint2);
		Channel peaks = new Midpoint(unit_grids_per_world, 4, 0.75f, 42).toChannel().channelMultiply(peakarea).multiply(.1f);
		height.channelAdd(peaks);
		height.smooth(1);

		// fix edges
		for (int y = 0; y < unit_grids_per_world; y += (unit_grids_per_world - 1)) {
			for (int x = 0; x < unit_grids_per_world; x++) {
				height.putPixel(x, y, 0f);
			}
		}
		for (int y = 1; y < unit_grids_per_world - 1; y++) {
			for (int x = 0; x < unit_grids_per_world; x += (unit_grids_per_world - 1)) {
				height.putPixel(x, y, 0f);
			}
		}

		slope = height.copy().lineart();
		if (DEBUG) slope.copy().dynamicRange().toLayer().saveAsPNG("slope");
		relheight = height.copy().relativeIntensityNormalized(Math.max(1, unit_grids_per_world>>5));
		if (DEBUG) relheight.toLayer().saveAsPNG("relheight");
		access = generateThresholdMap(slope, access_threshold).largestConnected(1f);
		access_exported = access.copy();
		if (DEBUG) access.toLayer().saveAsPNG("access");
		build = Landscape.generateBuildMap(generateThresholdMap(slope, build_threshold).channelMultiply(access));
	}

	// shape beaches
	private static @NonNull Channel beaches(@NonNull Channel channel) {
		float sealevel = 1.1f*Globals.SEA_LEVEL;
		float threshold = 2f*sealevel;
		for (int y = 0; y < channel.height; y++) {
			for (int x = 0; x < channel.width; x++) {
				float value = channel.getPixel(x, y);
				if (value < sealevel) {
					value = Tools.interpolateSmooth(0, sealevel, value/sealevel);
					channel.putPixel(x, y, value);
				} else if (value < threshold) {
					value = Tools.interpolateSmooth(sealevel, 2f*threshold - sealevel, 0.5f*(value - sealevel)/(threshold - sealevel));
					channel.putPixel(x, y, value);
				}
			}
		}
		return channel;
	}

	// generate threshold map
	private @NonNull Channel generateThresholdMap(@NonNull Channel slopemap, float threshold) {
		Channel channel = slopemap.copy().threshold(0f, threshold).channelSubtract(height.copy().threshold(0f, Globals.SEA_LEVEL));
		// fix edges
		for (int y = 0; y < unit_grids_per_world; y += (unit_grids_per_world - 1)) {
			for (int x = 0; x < unit_grids_per_world; x++) {
				channel.putPixel(x, y, 0f);
			}
		}
		for (int y = 1; y < unit_grids_per_world - 1; y++) {
			for (int x = 0; x < unit_grids_per_world; x += (unit_grids_per_world - 1)) {
				channel.putPixel(x, y, 0f);
			}
		}
		return channel;
	}

	// generate build map
	private static byte@NonNull [] @NonNull [] generateBuildMap(@NonNull Channel thresholdmap) {
		if (DEBUG) thresholdmap.toLayer().saveAsPNG("build_tresholdmap");
		int size = thresholdmap.getWidth();
		boolean[][] build_grid = new boolean[size][size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				build_grid[y][x] = thresholdmap.getPixel(x, y) > 0;
			}
		}
		byte[][] byte_grid = new byte[build_grid.length][build_grid[0].length];
		byte max = (byte) RacesResources.MAX_BUILDING_SIZE;
		for (byte i = 0; i < max; i++) {
			for (int y = 1; y < build_grid.length - 1; y++) {
				for (int x = 1; x < build_grid[y].length - 1; x++) {
					if (!build_grid[y][x] && byte_grid[y][x] == i) {
						for (int k = -1; k <= 1; k++) {
							for (int l = -1; l <= 1;  l++) {
								if (build_grid[y + k][x + l]) {
									build_grid[y + k][x + l] = false;
									byte_grid[y + k][x + l] = (byte)(i + 1);
								}
							}
						}
					}
				}
			}
		}
		for (int y = 1; y < build_grid.length - 1; y++) {
			for (int x = 1; x < build_grid[y].length - 1; x++) {
				if (build_grid[y][x])
					byte_grid[y][x] = max;
			}
		}
		return byte_grid;
	}


	// **********
	// * ALPHAS *
	// **********
	private @NonNull Channel generateAlphas() {
		int seed = Globals.LANDSCAPE_SEED;
		Channel alpha0, alpha1, alpha2, alpha3;
		Channel grass_alpha = switch (terrain) {
            case NATIVE -> {
                alpha0 = generateDirtAlpha();
                if (DEBUG) alpha0.toLayer().saveAsPNG("alpha_dirt");
                alpha1 = generateRubbleAlpha();
                if (DEBUG) alpha1.toLayer().saveAsPNG("alpha_rubble");
                alpha2 = Landscape.generateRockAlpha(slope, access_threshold);
                if (DEBUG) alpha2.toLayer().saveAsPNG("alpha_rock");
                alpha3 = generateGrassAlpha(unit_grids_per_world, seed);
                if (DEBUG) alpha3.toLayer().saveAsPNG("alpha_grass");
                alpha_maps[0] = new GLByteImage(alpha0, GL11.GL_RED);
                alpha_maps[1] = new GLByteImage(alpha1, GL11.GL_RED);
                alpha_maps[2] = new GLByteImage(alpha2, GL11.GL_RED);
                alpha_maps[3] = new GLByteImage(alpha3, GL11.GL_RED);
                yield alpha3;
            }
            case VIKING -> {
                alpha0 = generateSoilAlpha();
                if (DEBUG) alpha0.toLayer().saveAsPNG("alpha_soil");
                alpha1 = Landscape.generateCliffAlpha(slope, access_threshold);
                if (DEBUG) alpha1.toLayer().saveAsPNG("alpha_cliff");
                alpha2 = generateGrassAlpha(unit_grids_per_world, seed);
                if (DEBUG) alpha2.toLayer().saveAsPNG("alpha_grass");
                alpha3 = Landscape.generateSnowAlpha(height, alpha1.copy());
                if (DEBUG) alpha3.toLayer().saveAsPNG("alpha_snow");
                alpha_maps[0] = new GLByteImage(alpha0, GL11.GL_RED);
                alpha_maps[1] = new GLByteImage(alpha1, GL11.GL_RED);
                alpha_maps[2] = new GLByteImage(alpha2, GL11.GL_RED);
                alpha_maps[3] = new GLByteImage(alpha3, GL11.GL_RED);
                yield alpha2;
            }
        };

		Channel seabottom_alpha = Landscape.generateSeabottomAlpha(terrain, height);
		if (DEBUG) seabottom_alpha.toLayer().saveAsPNG("alpha_seabottom");

		// generate shadow and highlight alpha
		shadow = new Channel(unit_grids_per_world, unit_grids_per_world);
		highlight = new Channel(unit_grids_per_world, unit_grids_per_world);
		float lx = 1;
		float lz = 1;
		float lnorm = 1f/(float)Math.sqrt(lx*lx + lz*lz);
		lx = lx*lnorm;
		lz = lz*lnorm;
		float threshold = (float)Math.sqrt(0.5f);
		float nz = 2f*meters_per_height_unit/height_scale;
		float nzlz = nz*lz;
		float nz2 = nz*nz;
		for (int y = 0; y < unit_grids_per_world; y++) {
			for (int x = 0; x < unit_grids_per_world; x++) {
				float nx = height.getPixelWrap(x + 1, y) - height.getPixelWrap(x - 1, y);
				float ny = height.getPixelWrap(x, y + 1) - height.getPixelWrap(x, y - 1);
				float light = (nx*lx + nzlz)/((float)Math.sqrt(nx*nx + ny*ny + nz2)); // Can use Math here - calculation is not game state affecting
				if (light > threshold) {
					highlight.putPixel(x, y, light);
					shadow.putPixel(x, y, threshold);
				} else {
					highlight.putPixel(x, y, threshold);
					shadow.putPixel(x, y, Math.max(0, light));
				}
			}
		}
		highlight.dynamicRange(0f, 0.25f);
		shadow.invert().dynamicRange(0f, 0.75f);
		ProgressForm.progress(1/14f);

		// generate shadowcasting
		Channel shadowcast = new Channel(unit_grids_per_world, unit_grids_per_world);
		float val = 0;
		float peak = 0;
		float descent = 8f/unit_grids_per_world;
		for (int y = 0; y < unit_grids_per_world; y++) {
			for (int x = 0; x < unit_grids_per_world; x++) {
				val = height.getPixel(x, y);
				peak = peak - descent;
				if (peak > val) {
					shadowcast.putPixel(x, y, 1f);
				} else {
					peak = val;
				}
			}
			peak = 0;
		}
		shadow.channelBrightest(shadowcast.smooth(1).brightness(0.67f));
		if (DEBUG) shadow.toLayer().saveAsPNG("alpha_shadow");
		ProgressForm.progress(1/14f);

		alpha_maps[6] = new GLByteImage(seabottom_alpha, GL11.GL_RED);

		return grass_alpha;
	}

	// generate dirt alpha
	private @NonNull Channel generateDirtAlpha() {
		Channel dirt_alpha = height.copy().dynamicRange(1.1f*Globals.SEA_LEVEL, 2f*Globals.SEA_LEVEL, 0f, 1f);
		dirt_alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
		return dirt_alpha;
	}

	// generate rubble alpha
	private @NonNull Channel generateRubbleAlpha() {
		Channel rubble_alpha = slope.copy().dynamicRange(build_threshold, access_threshold, 0f, 1f);
		rubble_alpha.channelSubtract(height.copy().invert().dynamicRange(0.8f, 1f, 0f, 1f));
		rubble_alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.65f, 0f, 0.5f));
		return rubble_alpha;
	}

	// generate rock alpha
	private static @NonNull Channel generateRockAlpha(@NonNull Channel slope, float access_threshold) {
		Channel rock_alpha = slope.copy().threshold(access_threshold, 1f);
		return rock_alpha;
	}

	// generate grass alpha
	private @NonNull Channel generateGrassAlpha(int size, int seed) {
		Channel grass_alpha = new Midpoint(size, 4, 0.45f, seed).toChannel().dynamicRange(1f - vegetation_amount, 1f, 0f, 1f).gamma2();
		grass_alpha.channelBrightest(slope.copy().dynamicRange(0f, access_threshold, 0f, 1f).invert().dynamicRange(1f - vegetation_amount, 1f, 0f, 1f).gamma2());
		grass_alpha.channelAdd(relheight.copy().invert().add(-0.5f).multiply(2f));
		grass_alpha.channelSubtract(height.copy().invert().dynamicRange(0.6f, 0.8f, 0f, 1f));
		grass_alpha.channelSubtract(slope.copy().threshold(0.75f*access_threshold, 1f).smooth(3));
		grass_alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.7f, 0f, 0.5f));
		return grass_alpha;
	}

	// generate soil alpha
	private @NonNull Channel generateSoilAlpha() {
		Channel soil_alpha = height.copy().dynamicRange(1.1f*Globals.SEA_LEVEL, 2f*Globals.SEA_LEVEL, 0f, 1f);
		soil_alpha.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
		return soil_alpha;
	}

	// generate cliff alpha
	private static @NonNull Channel generateCliffAlpha(@NonNull Channel slope, float access_threshold) {
		Channel cliff_alpha = slope.copy().threshold(access_threshold, 1f);
		return cliff_alpha;
	}

	// generate snow alpha
	private static @NonNull Channel generateSnowAlpha(@NonNull Channel height, @NonNull Channel cliff_alpha) {
		Channel snow_alpha = height.copy().dynamicRange(0.5f, 0.6f, 0f, 1f);
		snow_alpha.channelSubtract(cliff_alpha);
		snow_alpha.smooth(1).smooth(1);

		return snow_alpha;
	}

	// generate seabottom alpha
	private static @NonNull Channel generateSeabottomAlpha(@NonNull TerrainType terrain, @NonNull Channel height) {
		Channel seabottom_alpha = height.copy().invert().dynamicRange(1f - Globals.SEA_LEVEL, 1f, 0f, 1f);
		return switch (terrain) {
            case NATIVE -> seabottom_alpha.grow(0f, 1).gamma(0.5f);
            case VIKING -> seabottom_alpha.gamma(0.5f);

        };
	}

	// ************
	// * SUPPLIES *
	// ************
	private void generateSupplies(@NonNull Channel grass_alpha) {
		// generate overall probability map for rock and iron resources
		Channel centerprob = new Hill(unit_grids_per_world, Hill.CIRCLE).toChannel().addClip(-.5f).dynamicRange();

		// generate (oak)tree/palmtree(/pine) maps
		Channel noise = new Midpoint(unit_grids_per_world, Utils.powerOf2Log2(unit_grids_per_world) - 3, 0.33f, seed).toChannel();
		Channel tree_channel = grass_alpha.copy();
		Channel palmtree_channel = height.copy();
		ProgressForm.progress(1/14f);

        switch (terrain) {
            case NATIVE -> {
                tree_channel.threshold(0.5f, 1f);
                tree_channel.channelAdd(noise.rotate(90).copy().threshold(0.9f, 1f));
                tree_channel.channelSubtract(slope.copy().threshold(access_threshold, 1f));
                tree_channel.channelSubtract(noise.copy().dynamicRange(0.1f, 1f));

                palmtree_channel.invert().dynamicRange(0.6f, 0.89f, 0f, 1f);
                palmtree_channel.channelSubtract(height.copy().invert().dynamicRange(0.89f, 0.9f, 0f, 1f));
                palmtree_channel.channelSubtract(noise.rotate(90)).channelAdd(noise.rotate(90).copy().threshold(0.9f, 1f)).channelSubtract(slope.copy().threshold(access_threshold, 1f));
            }
            case VIKING -> {
                tree_channel.gamma8();
                tree_channel.channelMultiply(height.copy().dynamicRange(0.55f, 0.65f, 1f, 0f));
                tree_channel.channelSubtract(slope.copy().threshold(access_threshold, 1f));
                tree_channel.channelSubtract(noise.copy());

                palmtree_channel = grass_alpha.copy().channelMultiply(height.copy().dynamicRange(0.5f, 0.6f, 1f, 0f)).invert();
                palmtree_channel.channelSubtract(height.copy().invert().dynamicRange(0.8f, 0.875f, 0f, 1f));
                palmtree_channel.channelSubtract(slope.copy().threshold(access_threshold, 1f));
                palmtree_channel.channelSubtract(noise.rotate(90));
            }
        }

		if (DEBUG) tree_channel.toLayer().saveAsPNG("supplies_trees");
		if (DEBUG) palmtree_channel.toLayer().saveAsPNG("supplies_palmtrees");
		ProgressForm.progress(1/14f);

		// generate rock and iron supplies map
		Channel rock_channel = relheight.copy();

        switch (terrain) {
            case NATIVE -> rock_channel
					.invert()
					.threshold(0.5f, 1f)
					.channelMultiply(noise.rotate(90).copy().gamma8().invert())
					.channelMultiply(centerprob);
            case VIKING -> rock_channel
					.threshold(0f, 0.5f)
					.channelMultiply(noise.rotate(90).copy().gamma8().invert());
        }

		Channel iron_channel = rock_channel.copy().rotate(90).flipV();
		if (DEBUG) rock_channel.toLayer().saveAsPNG("supplies_rocks");
		if (DEBUG) iron_channel.toLayer().saveAsPNG("supplies_iron");

		Channel supplies = access.copy();
		float accessible = supplies.sum();

		// place trees
		trees = placeSupplies(tree_channel, supplies, 64, (int)(vegetation_amount*max_trees*(accessible/area)), 0.33f);
		access.channelSubtract(trees);
		if (DEBUG) trees.toLayer().saveAsPNG("supplies_trees_placed");

		// place palmtrees
		palmtrees = placeSupplies(palmtree_channel, supplies, 64, (int)(vegetation_amount*max_palmtrees*(accessible/area)), 0.25f);
		access.channelSubtract(palmtrees);
		if (DEBUG) palmtrees.toLayer().saveAsPNG("supplies_palmtrees_placed");

		// place rock
		rock = placeSupplies(rock_channel, supplies, 64, (int)(supplies_amount*max_rock), 0f);
		access.channelSubtract(rock);
		shadow.channelBrightest(rock.copy().multiply(0.5f));
		if (DEBUG) rock.toLayer().saveAsPNG("supplies_rock_placed");

		// place iron
		iron = placeSupplies(iron_channel, supplies, 64, (int)(supplies_amount*max_iron), 0f);
		access.channelSubtract(iron);
		shadow.channelBrightest(iron.copy().multiply(0.5f));
		if (DEBUG) iron.toLayer().saveAsPNG("supplies_iron_placed");

		if (DEBUG) {
			IO.println("Number of trees placed: " + trees.count(1f));
			IO.println("Number of palmtrees placed: " + palmtrees.count(1f));
			IO.println("Number of rocks placed: " + rock.count(1f));
			IO.println("Number of iron ore placed: " + iron.count(1f));
		}

		// place extra supplies around starting locations
		int num_rock = 2;
		int num_iron = 1;
		for (int p = 0; p < num_players; p++) {
			for (int r = 0; r < num_rock; r++) {
				int[] location = access.find((unit_grids_per_world>>1), supply_locations[p][0], supply_locations[p][1], 1f);
				rock.putPixel(location[0], location[1], 1f);
				access.putPixel(location[0], location[1], 0f);
			}
			for (int i = 0; i < num_iron; i++) {
				int[] location = access.find((unit_grids_per_world>>1), supply_locations[p][0], supply_locations[p][1], 1f);
				iron.putPixel(location[0], location[1], 1f);
				access.putPixel(location[0], location[1], 0f);
			}
		}

		// shadow and highlight are changed by supply placement
		alpha_maps[4] = new GLByteImage(highlight, GL11.GL_RED);
		alpha_maps[5] = new GLByteImage(shadow, GL11.GL_RED);
		ProgressForm.progress(1/14f);

		// generate plant maps
		plants = new float[NUM_PLANT_TYPES][max_plants<<1];
		tree_channel.channelBrightest(palmtree_channel).brightness(.5f);
		noise.scaleFast(noise.width>>1, noise.height>>1).tileDouble();

		Channel plantsmap = grass_alpha.copy();
		plantsmap.multiply(0.25f).add(0.75f);
		plantsmap.channelMultiply(slope.copy().threshold(0f, access_threshold));
		plantsmap.channelMultiply(height.copy().threshold(Globals.SEA_LEVEL, 1f));

		Channel plants1 = plantsmap.copy().channelMultiply(noise.copy());
		Channel plants2 = plantsmap.copy().channelMultiply(noise.rotate(90));
		Channel plants3 = plantsmap.copy().channelMultiply(noise.rotate(90));
		Channel plants4 = plantsmap.copy().channelMultiply(noise.rotate(90));
		Channel place = new Channel(unit_grids_per_world, unit_grids_per_world);
		place = placePlants(plants1, place, 64, max_plants>>2, 0);
		place = placePlants(plants2, place, 64, max_plants>>2, 1);
		place = placePlants(plants3, place, 64, max_plants>>2, 2);
		place = placePlants(plants4, place, 64, max_plants>>2, 3);
	}

	// place supplies on map
	private @NonNull Channel placeSupplies(@NonNull Channel probability, @NonNull Channel supplies, int intervals, int max_count, float shadow_alpha_val) {
		max_count = Math.min(probability.width*probability.height, max_count);
		int scaleshift = Utils.powerOf2Log2(unit_grids_per_world*meters_per_height_unit/meters_per_world);
		int i = 0;
		float interval_size = 1f/intervals;
		float upper_bound = 1f;
		float lower_bound = upper_bound - interval_size;
		int supplyshadow_size = Math.max(unit_grids_per_world>>7, 2);
		Channel supplyshadow_alpha = new Channel(supplyshadow_size<<1, supplyshadow_size<<1).place(new Channel(supplyshadow_size, supplyshadow_size).fill(1f), supplyshadow_size>>1, supplyshadow_size>>1).smoothFast();
		Channel supplyshadow = new Channel(supplyshadow_size<<1, supplyshadow_size<<1);
		Channel supplyshadow_alpha2 = supplyshadow_alpha.copy().brightness(shadow_alpha_val);
		Channel place = new Channel(probability.width, probability.height);

		// place supplies
		out:
		while (i < max_count && lower_bound > interval_size) {
			for (int y = 1; y < probability.height - 1; y++) {
				for (int x = 1; x < probability.width - 1; x++) {
					float val = probability.getPixel(x, y);
					if (val <= upper_bound && val > lower_bound) {
						// place resource if grid and its 4 neighbours are unoccupied
						if (supplies.getPixel(x, y) > 0
							&& supplies.getPixel(x - 1, y) > 0
							&& supplies.getPixel(x + 1, y) > 0
							&& supplies.getPixel(x, y - 1) > 0
							&& supplies.getPixel(x, y - 1) > 0
						) {
							place.putPixel(x, y, 1f);
							// place shadow
							if (shadow_alpha_val > 0f) {
								int x_pixel = (x - supplyshadow_size + 1)>>scaleshift;
								int y_pixel = (y - supplyshadow_size + 1)>>scaleshift;
								highlight.place(supplyshadow, supplyshadow_alpha, x_pixel, y_pixel);
								shadow.placeBrightest(supplyshadow_alpha2, x_pixel, y_pixel);
							}
							// make node neighbourhood inaccessible
							for (int k = -1; k <= 1; k++) {
								for (int l = -1; l <= 1; l++) {
									supplies.putPixelWrap(x + k, y + l, 0f);
								}
							}

							i++;
							if (i >= max_count) break out;
						}
					}
				}
			}
			lower_bound -= interval_size;
			upper_bound -= interval_size;
		}
		return place;
	}

	// place plants on map
	private @NonNull Channel placePlants(@NonNull Channel probability, @NonNull Channel place, int intervals, int max_count, int plant_type) {
		max_count = Math.min(probability.width*probability.height, max_count);
		int i = 0;
		float interval_size = 1f/intervals;
		float upper_bound = 1f;
		float lower_bound = upper_bound - interval_size;

		// place plants
		out:
		while (i < max_count && lower_bound > interval_size) {
			for (int y = 2; y < probability.height - 2; y++) {
				for (int x = 2; x < probability.width - 2; x++) {
					float val = probability.getPixel(x, y);
					if (val <= upper_bound && val > lower_bound && place.getPixel(x, y) < 2 && random.nextFloat() < .25) {
						// place plant
						plants[plant_type][2*i] = meters_per_height_unit*(x + random.nextFloat());
						plants[plant_type][2*i+1] = meters_per_height_unit*(y + random.nextFloat());

						place.putPixelWrap(x, y, place.getPixel(x, y) + 1f);

						/*
						// make node neighbourhood inaccessible
						for (int k = -1; k <= 1; k++) {
							for (int l = -1; l <= 1; l++) {
								place.putPixelWrap(x + k, y + l, place.getPixel(x + k, y + l) + 1f);
							}
						}
						*/

						i++;
						if (i >= max_count) break out;
					}
				}
			}
			lower_bound -= interval_size;
			upper_bound -= interval_size;
		}
		return place;
	}


	// ******************
	// * UNIT LOCATIONS *
	// ******************
	private void generateUnitLocations(int initial_unit_count, float random_start_pos) {
		// create building placement map
		Channel buildmap = new Channel(access.width, access.height);
		Channel buildmap_debug = new Channel(access.width, access.height);
		for (int y = 0; y < access.height; y++) {
			for (int x = 0; x < access.width; x++) {
				if (build[y][x] == RacesResources.QUARTERS_SIZE) {
					buildmap.putPixel(x, y, 1f);
				}
				if (DEBUG) buildmap_debug.putPixel(x, y, .2f*build[y][x]);
			}
		}
		if (DEBUG) buildmap.toLayer().saveAsPNG("buildmap");
		if (DEBUG) buildmap_debug.toLayer().saveAsPNG("buildmap_debug");
		// find initial starting locations
		player_locations = new float[num_players][2*initial_unit_count];
		supply_locations = new int[num_players][2];
		float angle = 0.5f*(float)Math.PI;
		angle += random_start_pos*(float)Math.PI*2; // random start for multiplayer games
		float angle_step = 2f*(float)Math.PI/num_players;
		float radius = 0.35f*unit_grids_per_world;
		int scale = meters_per_world/unit_grids_per_world;
		int[] location_quarters = new int[2];
		int[] location_armory = new int[2];
		for (int i = 0; i < num_players; i++) {
			int x = (int)(radius*(float)Math.cos(angle) + (unit_grids_per_world>>1) + 0.5f);
			int y = (int)(radius*(float)Math.sin(angle) + (unit_grids_per_world>>1) + 0.5f);
			angle += angle_step;
			location_quarters = buildmap.findNoWrap((unit_grids_per_world>>1), x, y, 1f);
			for (int k = -(RacesResources.QUARTERS_SIZE/* - 1*/); k <= (RacesResources.QUARTERS_SIZE/* - 1*/); k++) {
				for (int l = -(RacesResources.QUARTERS_SIZE/* - 1*/); l <= (RacesResources.QUARTERS_SIZE/* - 1*/); l++) {
					access.putPixelWrap(location_quarters[0] + k, location_quarters[1] + l, 0f);
					buildmap.putPixelWrap(location_quarters[0] + k, location_quarters[1] + l, 0f);
				}
			}
			location_armory = buildmap.find((unit_grids_per_world>>1), location_quarters[0], location_quarters[1], 1f);
			for (int k = -(RacesResources.ARMORY_SIZE/* - 1*/); k <= (RacesResources.ARMORY_SIZE/* - 1*/); k++) {
				for (int l = -(RacesResources.ARMORY_SIZE/* - 1*/); l <= (RacesResources.ARMORY_SIZE/* - 1*/); l++) {
					access.putPixelWrap(location_armory[0] + k, location_armory[1] + l, 0f);
					buildmap.putPixelWrap(location_armory[0] + k, location_armory[1] + l, 0f);
				}
			}
			int[] location_unit_start = access.find((unit_grids_per_world>>1), location_quarters[0], location_quarters[1], 1f);
			supply_locations[i][0] = location_armory[0];
			supply_locations[i][1] = location_armory[1];
			int[] location_unit = new int[2];
			for (int u = 0; u < initial_unit_count; u++) {
				location_unit = access.find((unit_grids_per_world>>1), location_unit_start[0], location_unit_start[1], 1f);
				access.putPixelWrap(location_unit[0], location_unit[1], 0f);
				player_locations[i][2*u] = (location_unit[0]*scale);
				player_locations[i][2*u + 1] = (location_unit[1]*scale);
			}
		}

		// shuffle player starting locations
		List<float[]> player_locations_list = Arrays.asList(player_locations);
		Collections.shuffle(player_locations_list, random);
	}


	// ***************
	// * GET METHODS *
	// ***************

	public static @NonNull FogInfo getFogInfo(@NonNull TerrainType terrain, int meters_per_world) {
        var color = switch (terrain) {
            case NATIVE -> NATIVE_FOG_COLOR;
            case VIKING -> VIKING_FOG_COLOR;
        };
		var density = switch (terrain) {
			case NATIVE -> NATIVE_FOG_DENSITY;
			case VIKING -> VIKING_FOG_DENSITY;
		};
		var height = switch (terrain) {
			case NATIVE -> NATIVE_FOG_HEIGHT;
			case VIKING -> VIKING_FOG_HEIGHT;
		};
        return new DistanceFogInfo(FogInfo.Mode.EXP2, color, density,  height * meters_per_world, 0f, meters_per_world >> 2);
	}

	public BlendInfo @NonNull [] getBlendInfos() {
		return blend_infos;
	}

	public GLIntImage getDetail() {
		return detail;
	}

	public float @NonNull [] @NonNull [] getHeight() {
		return height.getPixels();
	}

	public boolean[][] getAccessGrid() {
		int size = access_exported.getWidth();
		boolean[][] access_grid = new boolean[size][size];
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				access_grid[y][x] = access_exported.getPixel(x, y) > 0;
			}
		}
		return access_grid;
	}

	public byte[][] getBuildGrid() {
		return build;
	}

	public @NonNull List<int @NonNull []> getTrees() {
		return getPositions(trees);
	}

	public @NonNull List<int @NonNull[]> getPalmtrees() {
		return getPositions(palmtrees);
	}

	public @NonNull List<int @NonNull[]> getRock() {
		return getPositions(rock);
	}

	public @NonNull List<int @NonNull[]> getIron() {
		return getPositions(iron);
	}

    private @NonNull List<int @NonNull []> getPositions(@NonNull Channel channel) {
        int count = channel.count(1f);
        List<int[]> list = new ArrayList<>(count);
        for (int y = 0; y < channel.height; y++) {
            for (int x = 0; x < channel.width; x++) {
                if (channel.getPixel(x, y) == 1f) {
                    list.add(new int[]{x, y});
                }
            }
        }
		assert count == list.size();
        return list;
    }

	public float[][] getPlants() {
		return plants;
	}

	public float[][] getStartingLocations() {
		return player_locations;
	}

	public float getSeaLevelMeters() {
		return sea_level_meters;
	}

}

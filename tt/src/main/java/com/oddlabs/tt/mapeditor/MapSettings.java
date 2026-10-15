package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.registration.RegistrationKey;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.LandscapeOverride;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.util.WordsEncoding;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.Random;

/**
 * The generator inputs of an island: what the skirmish menu encodes into a map code.
 *
 * <p>The map code layout matches {@code TerrainMenu}, so a code copied from a skirmish game opens the same island
 * here and the other way around.
 */
record MapSettings(int size, int terrain, int hills, int trees, int supplies, int seed) {

    static final int SLIDER_MAX = 10;
    static final int NUM_SIZES = Globals.SHIPS_ENABLED ? 5 : 4;
    static final int NUM_TERRAINS = 2;

    private static final int[] METERS_PER_WORLD = new int[]{256, 512, 1024, 2048, 2048};
    private static final boolean[] ARCHIPELAGO = new boolean[]{false, false, false, false, true};

    // Mixed radix digits of a map code, least significant first.
    private static final int SEED_CARDINALITY = 40000;
    private static final int SLIDER_CARDINALITY = SLIDER_MAX + 1;
    private static final int TERRAIN_TYPE_CARDINALITY = 4;
    private static final int TERRAIN_TYPE_CARDINALITY_LEGACY = 2;
    private static final int SIZE_CARDINALITY = 7;
    private static final int SIZE_CARDINALITY_LEGACY = 4;

    MapSettings {
        size = Math.clamp(size, 0, NUM_SIZES - 1);
        terrain = Math.clamp(terrain, 0, NUM_TERRAINS - 1);
        hills = Math.clamp(hills, 0, SLIDER_MAX);
        trees = Math.clamp(trees, 0, SLIDER_MAX);
        supplies = Math.clamp(supplies, 0, SLIDER_MAX);
        seed = Math.floorMod(seed, SEED_CARDINALITY);
    }

    static @NonNull MapSettings random(@NonNull Random random) {
        return new MapSettings(Game.SIZE_MEDIUM, Game.TERRAIN_TYPE_NATIVE, random.nextInt(SLIDER_CARDINALITY),
                random.nextInt(SLIDER_CARDINALITY), random.nextInt(SLIDER_CARDINALITY),
                random.nextInt(SEED_CARDINALITY));
    }

    @NonNull
    MapSettings withSize(int size) {
        return new MapSettings(size, terrain, hills, trees, supplies, seed);
    }

    @NonNull
    MapSettings withTerrain(int terrain) {
        return new MapSettings(size, terrain, hills, trees, supplies, seed);
    }

    @NonNull
    MapSettings withHills(int hills) {
        return new MapSettings(size, terrain, hills, trees, supplies, seed);
    }

    @NonNull
    MapSettings withTrees(int trees) {
        return new MapSettings(size, terrain, hills, trees, supplies, seed);
    }

    @NonNull
    MapSettings withSupplies(int supplies) {
        return new MapSettings(size, terrain, hills, trees, supplies, seed);
    }

    int getMetersPerWorld() {
        return METERS_PER_WORLD[size];
    }

    /** Meters of height the generator's 0 to 1 heights are scaled to, per island size (Landscape's table). */
    float getHeightScale() {
        return switch (getMetersPerWorld()) {
            case 256 -> 32f;
            case 512 -> 48f;
            case 1024 -> 64f;
            default -> 56f;
        };
    }

    /**
     * The steepest slope units can walk, as the largest height step between neighbouring cells in 0 to 1 heights,
     * per island size (Landscape's table).
     */
    float getAccessThreshold() {
        return switch (getMetersPerWorld()) {
            case 256 -> 0.05f;
            case 512 -> 0.0375f;
            case 1024 -> 0.025f;
            default -> 0.0325f;
        };
    }

    boolean isArchipelago() {
        return ARCHIPELAGO[size] && Globals.SHIPS_ENABLED;
    }

    @NonNull
    IslandGenerator createGenerator() {
        return createGenerator(null);
    }

    /**
     * @param override saved heights and resources to build the island with, or null to generate them
     */
    @NonNull
    IslandGenerator createGenerator(LandscapeOverride.@Nullable Source override) {
        // Same scaling as a skirmish game started from TerrainMenu.
        return new IslandGenerator(getMetersPerWorld(), Landscape.TerrainType.values()[terrain],
                hills / (float) SLIDER_MAX, trees / (float) SLIDER_MAX, supplies / (float) SLIDER_MAX, seed * seed,
                isArchipelago(), override);
    }

    @NonNull
    String toMapcode() {
        BigInteger result = BigInteger.ZERO;
        BigInteger weight = BigInteger.ONE;
        int[] digits = new int[]{seed, hills, trees, supplies, terrain, size};
        int[] cardinalities = new int[]{SEED_CARDINALITY, SLIDER_CARDINALITY, SLIDER_CARDINALITY, SLIDER_CARDINALITY, TERRAIN_TYPE_CARDINALITY, SIZE_CARDINALITY};
        for (int i = 0; i < digits.length; i++) {
            result = result.add(BigInteger.valueOf(digits[i]).multiply(weight));
            weight = weight.multiply(BigInteger.valueOf(cardinalities[i]));
        }
        return WordsEncoding.encode(result);
    }

    /**
     * Reads a word based map code, or a legacy single word letter code. Options the editor does not have (sizes or
     * terrain types from a newer build) keep their current value, the same way the skirmish menu treats them.
     *
     * @return the decoded settings, or null when the text is not a map code
     */
    @Nullable
    MapSettings parseMapcode(@NonNull String text) {
        String code = text.trim();
        if (code.isEmpty())
            return null;
        try {
            if (code.indexOf(' ') == -1) {
                return fromDigits(RegistrationKey.parseBits(code.toUpperCase()), SIZE_CARDINALITY_LEGACY,
                        TERRAIN_TYPE_CARDINALITY_LEGACY);
            }
            return fromDigits(WordsEncoding.decode(code), SIZE_CARDINALITY, TERRAIN_TYPE_CARDINALITY);
        } catch (IllegalArgumentException _) {
            // Covers unknown words as well as bad legacy letters (NumberFormatException).
            return null;
        }
    }

    private @NonNull MapSettings fromDigits(@NonNull BigInteger value, int size_cardinality,
            int terrain_cardinality) {
        BigInteger[] qr = value.divideAndRemainder(BigInteger.valueOf(SEED_CARDINALITY));
        int new_seed = qr[1].intValue();
        qr = qr[0].divideAndRemainder(BigInteger.valueOf(SLIDER_CARDINALITY));
        int new_hills = qr[1].intValue();
        qr = qr[0].divideAndRemainder(BigInteger.valueOf(SLIDER_CARDINALITY));
        int new_trees = qr[1].intValue();
        qr = qr[0].divideAndRemainder(BigInteger.valueOf(SLIDER_CARDINALITY));
        int new_supplies = qr[1].intValue();
        qr = qr[0].divideAndRemainder(BigInteger.valueOf(terrain_cardinality));
        int new_terrain = qr[1].intValue();
        qr = qr[0].divideAndRemainder(BigInteger.valueOf(size_cardinality));
        int new_size = qr[1].intValue();
        return new MapSettings(new_size < NUM_SIZES ? new_size : size,
                new_terrain < NUM_TERRAINS ? new_terrain : terrain, new_hills, new_trees, new_supplies, new_seed);
    }
}

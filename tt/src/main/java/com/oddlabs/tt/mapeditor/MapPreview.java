package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.Landscape;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A small top-down picture of a map, north up: sea shaded by depth, land by height and slope with light from the
 * north-west, every resource as a dot, and each player's spawn as a numbered disc in the player's colour.
 *
 * @param size pixels along each side
 * @param rgb  three bytes per pixel, rows from south to north
 */
record MapPreview(int size, byte @NonNull [] rgb) {
    static final int MAX_SIZE = 256;

    private static final float[] NATIVE_SHALLOW = {.30f, .62f, .72f};
    private static final float[] NATIVE_DEEP = {.06f, .20f, .40f};
    private static final float[] NATIVE_BEACH = {.86f, .80f, .58f};
    private static final float[] NATIVE_LOW = {.42f, .62f, .28f};
    private static final float[] NATIVE_HIGH = {.55f, .50f, .40f};
    private static final float[] NATIVE_STEEP = {.52f, .47f, .42f};
    private static final float[] VIKING_SHALLOW = {.25f, .45f, .55f};
    private static final float[] VIKING_DEEP = {.05f, .14f, .28f};
    private static final float[] VIKING_BEACH = {.62f, .60f, .55f};
    private static final float[] VIKING_LOW = {.36f, .52f, .34f};
    private static final float[] VIKING_HIGH = {.33f, .45f, .32f};
    private static final float[] VIKING_STEEP = {.45f, .45f, .47f};
    private static final float[] SNOW = {.93f, .94f, .96f};
    private static final float[] JUNGLE = {.10f, .32f, .10f};
    private static final float[] PALM = {.30f, .50f, .12f};
    private static final float[] OAK = {.15f, .30f, .12f};
    private static final float[] PINE = {.06f, .22f, .12f};
    private static final float[] ROCK = {.82f, .82f, .80f};
    private static final float[] IRON = {.75f, .38f, .22f};
    private static final float[] OUTLINE = {.08f, .08f, .08f};
    private static final float[] LIGHT_DIGIT = {1f, 1f, 1f};
    // The digits 0 to 9, three pixels wide and five high, a row of bits from left to right per line, top line first.
    private static final int[][] DIGITS = {{7, 5, 5, 5, 7}, {2, 6, 2, 2, 7}, {7, 1, 7, 4, 7}, {7, 1, 7, 1, 7}, {5, 5, 7, 1, 1}, {7, 4, 7, 1, 7}, {7, 4, 7, 5, 7}, {7, 1, 1, 1, 1}, {7, 5, 7, 5, 7}, {7, 5, 7, 1, 7}};

    MapPreview {
        if (size <= 0 || rgb.length != size * size * 3)
            throw new IllegalArgumentException("Preview of size " + size + " has " + rgb.length + " bytes");
    }

    /**
     * Draws a map.
     *
     * @param heights   height map cells in meters, indexed [y][x]
     * @param resources the resources to mark, or null for none
     * @param spawns    the players' spawns to mark
     */
    static @NonNull MapPreview render(float @NonNull [] @NonNull [] heights, @NonNull MapSettings settings,
            MapFile.@Nullable Resources resources, @NonNull Spawns spawns) {
        int grid = heights.length;
        int size = Math.min(grid, MAX_SIZE);
        boolean viking = Landscape.TerrainType.values()[settings.terrain()] == Landscape.TerrainType.VIKING;
        float scale = settings.getHeightScale();
        float sea = Globals.SEA_LEVEL * scale;
        float threshold = settings.getAccessThreshold() * scale;
        byte[] rgb = new byte[size * size * 3];
        float[] color = new float[3];
        for (int py = 0; py < size; py++) {
            int y = py * grid / size;
            for (int px = 0; px < size; px++) {
                int x = px * grid / size;
                float h = heights[y][x];
                if (h <= sea) {
                    float depth = (float) Math.sqrt(Math.clamp((sea - h) / sea, 0f, 1f));
                    mix(viking ? VIKING_SHALLOW : NATIVE_SHALLOW, viking ? VIKING_DEEP : NATIVE_DEEP, depth, color);
                } else {
                    landColor(heights, x, y, h, sea, scale, threshold, viking, color);
                }
                put(rgb, size, px, py, color);
            }
        }
        if (resources != null) {
            mark(rgb, size, grid, resources.of(Resource.TREE), viking ? OAK : JUNGLE, 1);
            mark(rgb, size, grid, resources.of(Resource.PALM), viking ? PINE : PALM, 1);
            // Supplies last and larger, as there are few of them and trees must not hide them.
            mark(rgb, size, grid, resources.of(Resource.ROCK), ROCK, 2);
            mark(rgb, size, grid, resources.of(Resource.IRON), IRON, 2);
        }
        // Spawns on top of everything, as they matter most when choosing a map to play.
        for (int player = 0; player < Spawns.COUNT; player++) {
            int[] cell = spawns.get(player);
            if (cell != null)
                markSpawn(rgb, size, cell[0] * size / grid, cell[1] * size / grid, player);
        }
        return new MapPreview(size, rgb);
    }

    /**
     * A disc in the player's colour with a dark rim and the player's number in it, centred on a pixel. The colours
     * are the game's default team colours, so a map looks the same to everyone it is shared with.
     */
    private static void markSpawn(byte @NonNull [] rgb, int size, int cx, int cy, int player) {
        Vector4fc team = Settings.DEFAULT_TEAM_COLOURS[player % Settings.DEFAULT_TEAM_COLOURS.length];
        float[] fill = {team.x(), team.y(), team.z()};
        String number = Integer.toString(player + 1);
        // Wide enough for two digits from a radius of 5 on.
        int radius = Math.clamp(size / 24, number.length() > 1 ? 5 : 4, 8);
        for (int dy = -radius - 1; dy <= radius + 1; dy++) {
            for (int dx = -radius - 1; dx <= radius + 1; dx++) {
                int px = cx + dx;
                int py = cy + dy;
                if (px < 0 || py < 0 || px >= size || py >= size)
                    continue;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance <= radius - .5f)
                    put(rgb, size, px, py, fill);
                else if (distance <= radius + .7f)
                    put(rgb, size, px, py, OUTLINE);
            }
        }
        // Dark digits on a light colour, light ones on a dark colour.
        float luma = .3f * fill[0] + .59f * fill[1] + .11f * fill[2];
        float[] ink = luma > .55f ? OUTLINE : LIGHT_DIGIT;
        int width = number.length() * 4 - 1;
        int left = cx - width / 2;
        for (int d = 0; d < number.length(); d++) {
            int[] glyph = DIGITS[number.charAt(d) - '0'];
            for (int line = 0; line < glyph.length; line++) {
                // Rows run from south to north, so the top line is the highest row.
                int py = cy + 2 - line;
                for (int bit = 0; bit < 3; bit++) {
                    int px = left + d * 4 + bit;
                    if ((glyph[line] & (4 >> bit)) != 0 && px >= 0 && py >= 0 && px < size && py < size)
                        put(rgb, size, px, py, ink);
                }
            }
        }
    }

    private static void landColor(float @NonNull [] @NonNull [] heights, int x, int y, float h, float sea,
            float scale, float threshold, boolean viking, float @NonNull [] color) {
        int last = heights.length - 1;
        float west = heights[y][Math.max(x - 1, 0)];
        float east = heights[y][Math.min(x + 1, last)];
        float south = heights[Math.max(y - 1, 0)][x];
        float north = heights[Math.min(y + 1, last)][x];
        float rise = (h - sea) / (scale - sea);
        if (rise < .03f) {
            System.arraycopy(viking ? VIKING_BEACH : NATIVE_BEACH, 0, color, 0, 3);
        } else {
            mix(viking ? VIKING_LOW : NATIVE_LOW, viking ? VIKING_HIGH : NATIVE_HIGH, Math.clamp(rise, 0f, 1f), color);
        }
        // The generator's snow covers the heights between half and six tenths of the scale.
        if (viking)
            mix(color, SNOW, Math.clamp((h / scale - .5f) / .1f, 0f, 1f), color);
        // Too steep to walk shows as bare rock, as the generator textures it.
        float slope = Math.max(Math.max(Math.abs(h - west), Math.abs(h - east)),
                Math.max(Math.abs(h - south), Math.abs(h - north)));
        mix(color, viking ? VIKING_STEEP : NATIVE_STEEP, Math.clamp((slope - .75f * threshold) / (.5f * threshold),
                0f, 1f), color);
        // Lit from the north-west: ground rising to the east or falling to the north faces the light.
        float facing = ((east - west) - (north - south)) / (4f * HeightMap.METERS_PER_UNIT_GRID);
        float light = Math.clamp(1f + facing, .55f, 1.35f);
        for (int i = 0; i < 3; i++)
            color[i] = Math.min(1f, color[i] * light);
    }

    private static void mark(byte @NonNull [] rgb, int size, int grid, @NonNull List<int @NonNull []> positions,
            float @NonNull [] color, int dot) {
        for (int[] position : positions) {
            int px = Math.min(position[0] * size / grid, size - dot);
            int py = Math.min(position[1] * size / grid, size - dot);
            for (int dy = 0; dy < dot; dy++)
                for (int dx = 0; dx < dot; dx++)
                    put(rgb, size, px + dx, py + dy, color);
        }
    }

    private static void mix(float @NonNull [] a, float @NonNull [] b, float t, float @NonNull [] out) {
        for (int i = 0; i < 3; i++)
            out[i] = a[i] + (b[i] - a[i]) * t;
    }

    private static void put(byte @NonNull [] rgb, int size, int px, int py, float @NonNull [] color) {
        int i = (py * size + px) * 3;
        for (int c = 0; c < 3; c++)
            rgb[i + c] = (byte) Math.round(Math.clamp(color[c], 0f, 1f) * 255f);
    }
}

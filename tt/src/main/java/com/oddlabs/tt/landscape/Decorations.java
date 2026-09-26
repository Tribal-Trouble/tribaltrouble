package com.oddlabs.tt.landscape;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.SpriteFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// Render-only scenery scattered by its own random, so it never touches the simulation or its checksum.
public final class Decorations {
    private static final String DECORATIONS_FILE = "/geometry/decorations.txt";
    private static final String ANY_GROUND = "land";

    private record Decoration(@NonNull SpriteKey sprite, Landscape.@Nullable Ground ground, int count) {
    }

    private final @NonNull List<Decoration> decorations = new ArrayList<>();

    // Lines of "group name ground count event" written by the geometry converter.
    public Decorations(@NonNull RenderQueues queues) {
        try (var reader = new BufferedReader(new InputStreamReader(
                com.oddlabs.util.Utils.makeURL(DECORATIONS_FILE).openStream(), StandardCharsets.UTF_8))) {
            reader.lines().map(line -> line.split(" ")).filter(f -> RacesResources.isEventActive(f[4])).forEach(
                    f -> decorations.add(new Decoration(queues.register(new SpriteFile(
                            "/geometry/" + f[0] + "/" + f[1] + ".binsprite", Globals.NO_MIPMAP_CUTOFF, true, false,
                            true, true, true)),
                            ground(f[2]), Integer.parseInt(f[3]))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Landscape.@Nullable Ground ground(@NonNull String name) {
        return name.equals(ANY_GROUND) ? null : Landscape.Ground.valueOf(name.toUpperCase(Locale.ROOT));
    }

    public void place(@NonNull World world, byte @NonNull [] @NonNull [] ground, int seed) {
        if (decorations.isEmpty())
            return;
        HeightMap heightmap = world.getHeightMap();
        boolean[][] access = heightmap.getAccessGrid();
        boolean[][] open = new boolean[access.length][access.length];
        for (int y = 0; y < access.length; y++) {
            for (int x = 0; x < access.length; x++) {
                open[y][x] = access[y][x] && heightmap.getHeight(x, y) > heightmap.getSeaLevelMeters()
                        && !world.getUnitGrid().isGridOccupied(x, y);
            }
        }
        Random random = new Random(seed);
        for (Decoration decoration : decorations) {
            for (float[] spot : scatter(random, ground, open, decoration.ground(), decoration.count()))
                new Plants(world, spot[0], spot[1], spot[2], spot[3], decoration.sprite());
        }
    }

    // Each spot is x, y, dir_x, dir_y; the cells it takes are closed in open.
    static @NonNull List<float @NonNull []> scatter(@NonNull Random random,
            byte @NonNull [] @NonNull [] ground, boolean @NonNull [] @NonNull [] open,
            Landscape.@Nullable Ground wanted, int count) {
        int size = open.length;
        int[] cells = new int[size * size];
        int num_cells = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (open[y][x] && (wanted == null || ground[y][x] == wanted.ordinal()))
                    cells[num_cells++] = y * size + x;
            }
        }
        List<float[]> spots = new ArrayList<>();
        for (int i = 0; i < count && i < num_cells; i++) {
            int pick = i + random.nextInt(num_cells - i);
            int cell = cells[pick];
            cells[pick] = cells[i];
            int grid_x = cell % size;
            int grid_y = cell / size;
            open[grid_y][grid_x] = false;
            float x = UnitGrid.coordinateFromGrid(grid_x) + (random.nextFloat() - .5f);
            float y = UnitGrid.coordinateFromGrid(grid_y) + (random.nextFloat() - .5f);
            double angle = random.nextDouble() * 2 * Math.PI;
            spots.add(new float[]{x, y, (float) Math.cos(angle), (float) Math.sin(angle)});
        }
        return spots;
    }
}

package com.oddlabs.tt.procedural;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Heights and resources an island is built with in place of the generated ones, as a map editor saves them, and
 * where the players start. What is left out is generated as usual.
 *
 * @param heights   height map cells in meters, indexed [y][x], one per grid unit of the island
 * @param resources grid positions of every resource, placed instead of the generated ones
 * @param spawns    where the players start instead of the generated places, or null to generate them all
 */
public record LandscapeOverride(float @Nullable [] @NonNull [] heights, @Nullable Resources resources,
                                @Nullable Spawns spawns) {
    public LandscapeOverride(float @Nullable [] @NonNull [] heights, @Nullable Resources resources) {
        this(heights, resources, null);
    }

    /** Grid positions ({x, y}) of each kind of resource. */
    public record Resources(@NonNull List<int @NonNull []> trees, @NonNull List<int @NonNull []> palm_trees,
                            @NonNull List<int @NonNull []> rocks, @NonNull List<int @NonNull []> iron) {
    }

    /**
     * The places a map's maker picked for the players to start at.
     *
     * @param cells    the grid position ({x, y}) of each player slot's spawn, or null for a slot without one
     * @param shuffled whether the players take the spawns in an order of chance, rather than each on its own slot's
     * @param seed     the order of chance, which every player in a game must share
     */
    public record Spawns(int @Nullable [] @NonNull [] cells, boolean shuffled, long seed) {
        /**
         * Where each player taking part starts. In order, each player gets its own slot's spawn, and those whose slot
         * has none get the spawns no one took, in order; shuffled, the players take the spawns in an order of chance
         * instead. Players left over when the spawns run out get none.
         *
         * @param slots the lobby slot of each player taking part, in the order the game has them
         * @return the grid position each player starts at, in the same order, or null for one left to the generator
         */
        public int @Nullable [] @NonNull [] arrange(int @NonNull [] slots) {
            int[][] starts = new int[slots.length][];
            List<int[]> free = new ArrayList<>();
            for (int[] cell : cells) {
                if (cell != null)
                    free.add(cell);
            }
            if (shuffled) {
                Collections.shuffle(free, new Random(seed));
                for (int i = 0; i < slots.length && i < free.size(); i++)
                    starts[i] = free.get(i);
                return starts;
            }
            for (int i = 0; i < slots.length; i++) {
                int[] cell = slots[i] >= 0 && slots[i] < cells.length ? cells[slots[i]] : null;
                if (cell != null) {
                    starts[i] = cell;
                    free.remove(cell);
                }
            }
            for (int i = 0; i < slots.length && !free.isEmpty(); i++) {
                if (starts[i] == null)
                    starts[i] = free.removeFirst();
            }
            return starts;
        }
    }

    /**
     * Where a generator gets its override when it builds the world. It travels with the generator from the server
     * to the client, so it names the data rather than carrying it.
     */
    public interface Source extends Serializable {
        @NonNull
        LandscapeOverride load() throws IOException;
    }
}

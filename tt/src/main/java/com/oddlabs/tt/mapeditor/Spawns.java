package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.MapFileHeader;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.procedural.LandscapeOverride;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Where a map's maker wants each player to start: a grid cell for each player slot that has one. Player 1 is slot 0.
 * Spawns never change; {@link #with} makes new ones.
 *
 * <p>In a game on the map each player's units start by its spawn, with room for quarters and an armory, as they would
 * at a generated place.
 */
final class Spawns {
    /** One spawn for each player slot a game can have. */
    static final int COUNT = MatchmakingServerInterface.MAX_PLAYERS;
    static final @NonNull Spawns NONE = new Spawns(new int[COUNT][]);

    private final int @Nullable [] @NonNull [] cells;

    private Spawns(int @Nullable [] @NonNull [] cells) {
        this.cells = cells;
    }

    /** The grid cell ({x, y}) of a player's spawn, or null when it has none. */
    int @Nullable [] get(int player) {
        int[] cell = cells[player];
        return cell != null ? cell.clone() : null;
    }

    /** These spawns with a player's moved to a cell, or taken away when the cell is null. */
    @NonNull
    Spawns with(int player, int @Nullable [] cell) {
        int[][] changed = cells.clone();
        changed[player] = cell != null ? new int[]{cell[0], cell[1]} : null;
        return new Spawns(changed);
    }

    /** How many players have a spawn. */
    int count() {
        int count = 0;
        for (int[] cell : cells) {
            if (cell != null)
                count++;
        }
        return count;
    }

    boolean isEmpty() {
        return count() == 0;
    }

    /** The player whose spawn is nearest a cell within a distance in cells, or -1 when there is none. */
    int nearest(float x, float y, float within) {
        int nearest = -1;
        float best = within * within;
        for (int player = 0; player < COUNT; player++) {
            int[] cell = cells[player];
            if (cell == null)
                continue;
            float dx = cell[0] - x;
            float dy = cell[1] - y;
            float distance = dx * dx + dy * dy;
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * Where the island generator starts the players.
     *
     * @param shuffled whether the players take the spawns in an order of chance rather than each its own slot's
     * @param seed     the order of chance, the same for everyone in the game
     */
    LandscapeOverride.@NonNull Spawns toOverride(boolean shuffled, long seed) {
        return new LandscapeOverride.Spawns(cells.clone(), shuffled, seed);
    }

    /** Writes the count, then the player slot and the cell of each spawn, as {@link MapFileHeader} skips them. */
    void write(@NonNull DataOutputStream out) throws IOException {
        out.writeByte(count());
        for (int player = 0; player < COUNT; player++) {
            int[] cell = cells[player];
            if (cell == null)
                continue;
            out.writeByte(player);
            out.writeShort(cell[0]);
            out.writeShort(cell[1]);
        }
    }

    /** Reads what {@link #write} wrote, for an island of the given cells along a side. */
    static @NonNull Spawns read(@NonNull DataInputStream in, int grid_size) throws IOException {
        int count = in.readUnsignedByte();
        if (count > COUNT)
            throw new IOException("Bad spawn count " + count);
        int[][] cells = new int[COUNT][];
        for (int i = 0; i < count; i++) {
            int player = in.readUnsignedByte();
            int x = in.readShort();
            int y = in.readShort();
            if (player >= COUNT || cells[player] != null)
                throw new IOException("Bad spawn player " + player);
            if (x < 0 || y < 0 || x >= grid_size || y >= grid_size)
                throw new IOException("Spawn outside the island");
            cells[player] = new int[]{x, y};
        }
        return new Spawns(cells);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Spawns spawns && Arrays.deepEquals(cells, spawns.cells);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(cells);
    }
}

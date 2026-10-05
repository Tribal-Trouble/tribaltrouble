package com.oddlabs.tt.mapeditor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Keeps the players' spawns in step with the others in a shared session, as {@link SessionSync} keeps the island.
 *
 * <p>An edit carries the spawns of the players this player moved, as they are now. Another player's edit is laid,
 * except for a spawn this player moved and has not had acknowledged yet: the server put this player's edit after it,
 * so this player's spawn is the one everyone ends up with.
 */
final class SpawnSync {
    private final boolean @NonNull [] unsent = new boolean[Spawns.COUNT];
    // The players each edit sent and not yet acknowledged carries, oldest first.
    private final Deque<boolean @NonNull []> pending = new ArrayDeque<>();

    /** This player moved a player's spawn, to go out with the next edit. */
    void changed(int player) {
        unsent[player] = true;
    }

    /**
     * Takes the spawns this player moved since the last edit as a new edit, to send. It is kept until acknowledged.
     *
     * @return the edit, or null when nothing changed
     */
    byte @Nullable [] take(@NonNull Spawns spawns) {
        boolean[] players = unsent.clone();
        int count = 0;
        for (boolean player : players) {
            if (player)
                count++;
        }
        if (count == 0)
            return null;
        Arrays.fill(unsent, false);
        pending.add(players);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(count);
            for (int player = 0; player < players.length; player++) {
                if (!players[player])
                    continue;
                int[] cell = spawns.get(player);
                out.writeByte(player);
                out.writeBoolean(cell != null);
                out.writeShort(cell != null ? cell[0] : 0);
                out.writeShort(cell != null ? cell[1] : 0);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    /** The server put this player's oldest edit on its way in the session's order. */
    void acknowledged() {
        pending.poll();
    }

    /**
     * Lays another player's edit over the spawns, which the server put before this player's edits still on their way.
     *
     * @param grid_size cells along a side of the island
     * @return the spawns with the edit laid
     */
    @NonNull
    Spawns apply(@NonNull Spawns spawns, byte @NonNull [] edit, int grid_size) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(edit))) {
            int count = in.readUnsignedByte();
            if (count > Spawns.COUNT)
                throw new IOException("Bad spawn count " + count);
            Spawns result = spawns;
            for (int i = 0; i < count; i++) {
                int player = in.readUnsignedByte();
                boolean placed = in.readBoolean();
                int x = in.readShort();
                int y = in.readShort();
                if (player >= Spawns.COUNT)
                    throw new IOException("Bad spawn player " + player);
                if (placed && (x < 0 || y < 0 || x >= grid_size || y >= grid_size))
                    throw new IOException("Spawn outside the island");
                if (!isOwn(player))
                    result = result.with(player, placed ? new int[]{x, y} : null);
            }
            return result;
        }
    }

    /** Whether this player moved a player's spawn and everyone is still to have it. */
    private boolean isOwn(int player) {
        if (unsent[player])
            return true;
        for (boolean[] players : pending) {
            if (players[player])
                return true;
        }
        return false;
    }
}

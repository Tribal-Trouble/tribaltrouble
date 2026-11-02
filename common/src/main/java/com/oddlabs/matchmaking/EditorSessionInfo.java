package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * A shared map editor session as the multiplayer menu lists it: players edit one island together, each seeing the
 * others' cameras and edits as they happen. The matchmaking server relays everything between them; the island itself
 * lives with the players, and one of them hands it to each player who joins.
 */
public final class EditorSessionInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    /** Players editing one island at most; each has a colour of their own. */
    public static final int MAX_MEMBERS = 8;
    /** Bytes of an edit or of the island sent in one event, well under the limit of an ARMI event. */
    public static final int CHUNK_SIZE = 16000;
    /** The largest island handed to a joining player, as a map file. */
    public static final int MAX_SNAPSHOT_SIZE = 32 * 1024 * 1024;
    /** The longest session name; it is the map's name, which is as long as a shared map's may be. */
    public static final int MAX_NAME_LENGTH = SharedMap.MAX_NAME_LENGTH;
    /** The longest chat message passed on to the others in a session. */
    public static final int MAX_CHAT_LENGTH = 256;

    public static final int ERROR_NOT_ALLOWED = 1;
    public static final int ERROR_NO_SUCH_SESSION = 2;
    public static final int ERROR_FULL = 3;
    /** Nobody in the session could hand over the island. */
    public static final int ERROR_NO_MAP = 4;

    private final int id;
    private final @NonNull String name;
    private final @NonNull String host;
    private final byte size;
    private final byte terrain;
    private final byte members;

    public EditorSessionInfo(int id, @NonNull String name, @NonNull String host, int size, int terrain, int members) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.size = (byte) size;
        this.terrain = (byte) terrain;
        this.members = (byte) members;
    }

    public int getId() {
        return id;
    }

    public @NonNull String getName() {
        return name;
    }

    /** The player who opened the session, or who has been in it longest since they left. */
    public @NonNull String getHost() {
        return host;
    }

    /** The island size, as a {@code Game.SIZE_} index. */
    public int getSize() {
        return size;
    }

    public int getTerrainType() {
        return terrain;
    }

    public int getMembers() {
        return members;
    }
}

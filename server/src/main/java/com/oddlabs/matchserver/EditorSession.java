package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.EditorSessionInfo;
import com.oddlabs.matchmaking.MatchmakingClientInterface;
import com.oddlabs.matchmaking.SharedMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A shared map editor session: players editing one island together.
 *
 * <p>The island lives with the players; the server only relays between them. Edits are passed on in the order their
 * last pieces arrive, which is the one order every player lays them in, and each is acknowledged to its sender in
 * that order too. A player joining is sent every edit from the moment they join, and one of the players already
 * there is asked for the island as it is at that moment, on top of which those edits go.
 */
final class EditorSession {
    private static final Map<Integer, EditorSession> sessions = new LinkedHashMap<>();
    private static int next_id = 1;
    /** Island sizes and terrains a client may name, as Game.SIZE_ and Game.TERRAIN_TYPE_ indexes. */
    private static final int MAX_SIZE = 4;
    private static final int MAX_TERRAIN = 1;

    private static final class Member {
        final @NonNull Client client;
        final @NonNull String nick;
        final int slot;
        // Whether the member has the island, so can hand it on.
        boolean ready;
        // While joining: who was asked for the island, until the last of it is passed on.
        @Nullable
        Member provider;

        Member(@NonNull Client client, @NonNull String nick, int slot) {
            this.client = client;
            this.nick = nick;
            this.slot = slot;
        }

        @NonNull
        MatchmakingClientInterface out() {
            return client.getClientInterface();
        }
    }

    private final int id;
    private final @NonNull String name;
    private final int size;
    private final int terrain;
    // In the order they joined; the first is shown as the host.
    private final List<Member> members = new ArrayList<>();

    private EditorSession(int id, @NonNull String name, int size, int terrain) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.terrain = terrain;
    }

    static @NonNull Collection<EditorSession> all() {
        return sessions.values();
    }

    static @Nullable EditorSession get(int id) {
        return sessions.get(id);
    }

    /** Opens a session with its host in it, who has the island already. */
    static @NonNull EditorSession open(@NonNull Client host, @NonNull String nick, @Nullable String name, int size,
            int terrain) {
        String session_name = name != null && SharedMap.isValidName(name) && BannedWordFilter.isAllowed(
                name) ? name : nick;
        EditorSession session = new EditorSession(next_id++, session_name, Math.clamp(size, 0, MAX_SIZE),
                Math.clamp(terrain, 0, MAX_TERRAIN));
        sessions.put(session.id, session);
        Member member = new Member(host, nick, 0);
        member.ready = true;
        session.members.add(member);
        host.getClientInterface().editorSessionJoined(session.id, session.name, member.slot);
        MatchmakingServer.getLogger().info(nick + " opened editor session " + session.id + " \"" + session.name + "\"");
        return session;
    }

    @NonNull
    EditorSessionInfo info() {
        String host = members.isEmpty() ? "" : members.getFirst().nick;
        return new EditorSessionInfo(id, name, host, size, terrain, members.size());
    }

    /**
     * Takes a player into the session and asks a player there for the island for them.
     *
     * @return 0, or the {@code EditorSessionInfo.ERROR_} code it failed with
     */
    int join(@NonNull Client client, @NonNull String nick) {
        if (members.size() >= EditorSessionInfo.MAX_MEMBERS)
            return EditorSessionInfo.ERROR_FULL;
        Member provider = findProvider(null);
        if (provider == null)
            return EditorSessionInfo.ERROR_NO_MAP;
        Member member = new Member(client, nick, freeSlot());
        member.provider = provider;
        member.out().editorSessionJoined(id, name, member.slot);
        for (Member other : members) {
            member.out().editorMemberJoined(other.slot, other.nick);
            other.out().editorMemberJoined(member.slot, member.nick);
        }
        members.add(member);
        // Edits from here on reach the new member as well, to go on top of the island as it is now.
        provider.out().editorSnapshotRequested(nick);
        MatchmakingServer.getLogger().info(nick + " joined editor session " + id);
        return 0;
    }

    private int freeSlot() {
        for (int slot = 0;; slot++) {
            boolean taken = false;
            for (Member member : members)
                taken |= member.slot == slot;
            if (!taken)
                return slot;
        }
    }

    /** The longest standing member with the island, other than the one given. */
    private @Nullable Member findProvider(@Nullable Member except) {
        for (Member member : members)
            if (member.ready && member != except)
                return member;
        return null;
    }

    private @Nullable Member find(@NonNull Client client) {
        for (Member member : members)
            if (member.client == client)
                return member;
        return null;
    }

    void leave(@NonNull Client client) {
        Member member = find(client);
        if (member == null)
            return;
        members.remove(member);
        for (Member other : members)
            other.out().editorMemberLeft(member.slot);
        // Players waiting for the island from this one ask another.
        for (Member joining : List.copyOf(members)) {
            if (joining.provider != member)
                continue;
            Member provider = findProvider(joining);
            if (provider != null) {
                joining.provider = provider;
                provider.out().editorSnapshotRequested(joining.nick);
            } else {
                joining.out().editorSessionFailed(EditorSessionInfo.ERROR_NO_MAP);
                joining.client.editorSessionEnded(this);
                leave(joining.client);
            }
        }
        if (members.isEmpty()) {
            sessions.remove(id);
            MatchmakingServer.getLogger().info("Editor session " + id + " closed");
        }
    }

    void ready(@NonNull Client client) {
        Member member = find(client);
        if (member != null && member.provider == null)
            member.ready = true;
    }

    void snapshot(@NonNull Client from, @NonNull String nick, int total_size, int offset, byte @NonNull [] data) {
        Member sender = find(from);
        if (sender == null || data.length > EditorSessionInfo.CHUNK_SIZE || total_size <= 0
                || total_size > EditorSessionInfo.MAX_SNAPSHOT_SIZE || offset < 0 || offset + data.length > total_size)
            return;
        for (Member member : members) {
            if (member.provider != sender || !member.nick.equals(nick))
                continue;
            member.out().receiveEditorSnapshot(total_size, offset, data);
            if (offset + data.length == total_size)
                member.provider = null;
        }
    }

    void edit(@NonNull Client from, byte @NonNull [] data, boolean last) {
        Member sender = find(from);
        if (sender == null || data.length > EditorSessionInfo.CHUNK_SIZE)
            return;
        for (Member member : members)
            if (member != sender)
                member.out().receiveEditorEdit(sender.slot, data, last);
        if (last)
            sender.out().editorEditAcknowledged();
    }

    void presence(@NonNull Client from, float x, float y, float z, float horiz_angle, float vert_angle,
            float cursor_x, float cursor_y, float radius, int brush) {
        Member sender = find(from);
        if (sender == null)
            return;
        for (Member member : members)
            if (member != sender && member.ready)
                member.out().receiveEditorPresence(sender.slot, x, y, z, horiz_angle, vert_angle, cursor_x, cursor_y,
                        radius, brush);
    }

    /** Passes a chat message, filtered already, to everyone in the session, its writer too. */
    void chat(@NonNull Client from, @NonNull String message) {
        Member sender = find(from);
        if (sender == null)
            return;
        for (Member member : members)
            member.out().receiveEditorChat(sender.slot, message);
    }
}

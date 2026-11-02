package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.EditorSessionInfo;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.tt.net.ChatCommand;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.EditorSessionListener;
import com.oddlabs.tt.net.MatchmakingClient;
import com.oddlabs.tt.net.Network;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This player's place in a shared map editor session, through the matchmaking server: who else is in it, the edits
 * going back and forth as {@link SessionMessage}s, the chat, and, while joining, the island arriving from another
 * player.
 *
 * <p>The editor showing the island attaches itself once it is built. Until it is attached, edits and presences that
 * come in wait here, to be laid over the island in the order they came.
 */
final class EditorSession implements EditorSessionListener {
    /** What the session tells the editor showing the island. */
    interface Editor {
        void memberJoined(int slot, @NonNull String nick);

        void memberLeft(int slot, @NonNull String nick);

        /** Another player's edit, as a {@link SessionMessage}. */
        void received(int slot, byte @NonNull [] message);

        /**
         * The server put this player's oldest unacknowledged edit in the session's order.
         *
         * @param kind its {@link SessionMessage} kind
         */
        void acknowledged(byte kind);

        void presence(int slot, @NonNull Presence presence);

        /** The island as it is now, as a map file, for a player joining. */
        byte @NonNull [] snapshot() throws IOException;

        /** A chat message came, which the session's chat history now ends with. */
        void chatted();

        /** The session ended for this player, who keeps the island to go on with alone. */
        void ended(@NonNull String reason);
    }

    /** Hears how joining goes, until the island is here. */
    interface JoinListener {
        void progress(@NonNull String status);

        void arrived(@NonNull EditorSession session, @NonNull MapFile map);

        void failed(@NonNull String reason);
    }

    /**
     * Where a player's camera is and what their brush is doing.
     *
     * @param brush the brush ordinal, how it is held and whether it is over the ground, as {@link #pack} packs them
     */
    record Presence(float x, float y, float z, float horiz_angle, float vert_angle, float cursor_x, float cursor_y,
                    float radius, int brush) {

        private static final int HAS_CURSOR = 1;
        private static final int LEFT = 2;
        private static final int RIGHT = 4;
        private static final int MAP_MODE = 8;
        private static final int BRUSH_SHIFT = 8;

        static int pack(@NonNull Brush brush, boolean has_cursor, int stroke_sign, boolean map_mode) {
            return (has_cursor ? HAS_CURSOR : 0) | (stroke_sign > 0 ? LEFT : 0) | (stroke_sign < 0 ? RIGHT : 0) | (map_mode ? MAP_MODE : 0) | brush.ordinal() << BRUSH_SHIFT;
        }

        boolean hasCursor() {
            return (brush & HAS_CURSOR) != 0;
        }

        /** 1 while the left button paints, -1 while the right does, else 0. */
        int strokeSign() {
            return (brush & LEFT) != 0 ? 1 : (brush & RIGHT) != 0 ? -1 : 0;
        }

        boolean inMapMode() {
            return (brush & MAP_MODE) != 0;
        }

        @Nullable
        Brush getBrush() {
            int ordinal = brush >>> BRUSH_SHIFT;
            return ordinal < Brush.values().length ? Brush.values()[ordinal] : null;
        }
    }

    private record Waiting(int slot, byte @NonNull [] data) {
    }

    /** Chat messages kept to show when the chat is opened. */
    private static final int MAX_CHAT_HISTORY = 100;

    /** How a session reaches the matchmaking server: through the game's connection, or a stand-in. */
    interface Transport {
        /** The server, or null when not connected. */
        @Nullable
        MatchmakingServerInterface server();

        /** Hands what the server says about the session to a listener, or to nobody. */
        void listen(@Nullable EditorSessionListener listener);

        /** This player's nick on the server. */
        @NonNull
        String nick();
    }

    /** The game's connection to the matchmaking server. */
    private static final Transport NETWORK = new Transport() {
        @Override
        public @Nullable MatchmakingServerInterface server() {
            MatchmakingClient client = Network.getMatchmakingClient();
            return client.isConnected() ? client.getInterface() : null;
        }

        @Override
        public void listen(@Nullable EditorSessionListener listener) {
            Network.getMatchmakingClient().setEditorSessionListener(listener);
        }

        @Override
        public @NonNull String nick() {
            return localNick();
        }
    };

    private static @Nullable EditorSession current;

    private final @NonNull String name;
    private final boolean hosting;
    private int slot = -1;
    private final Map<Integer, String> members = new TreeMap<>();
    // Pieces of edits on their way, by the slot of the player making them.
    private final Map<Integer, ByteArrayOutputStream> parts = new HashMap<>();
    private final List<Waiting> waiting = new ArrayList<>();
    private final Map<Integer, Presence> presences = new HashMap<>();
    // The kinds of this player's edits on their way, oldest first.
    private final Deque<@NonNull Byte> unacknowledged = new ArrayDeque<>();
    // Players a snapshot was asked for while no editor was attached.
    private final List<@NonNull String> snapshot_requests = new ArrayList<>();
    private final List<@NonNull String> chat = new ArrayList<>();
    private @Nullable Editor editor;
    private boolean ended;
    private @NonNull String end_reason = "";

    private @Nullable JoinListener join_listener;
    private @Nullable ByteArrayOutputStream snapshot;
    private final @NonNull Transport transport;

    EditorSession(@NonNull String name, boolean hosting, @NonNull Transport transport) {
        this.name = name;
        this.hosting = hosting;
        this.transport = transport;
        transport.listen(this);
    }

    private @Nullable MatchmakingServerInterface server() {
        return transport.server();
    }

    /** Whether this player could open a session now: logged in to the multiplayer server with a profile. */
    static boolean canHost() {
        MatchmakingClient client = Network.getMatchmakingClient();
        return client.isConnected() && client.getProfile() != null;
    }

    /** This player's nick on the multiplayer server. */
    static @NonNull String localNick() {
        Profile profile = Network.getMatchmakingClient().getProfile();
        return profile != null ? profile.getNick() : Network.getMatchmakingClient().getUsername();
    }

    /**
     * Opens a session on the island the editor shows, for others to join from the multiplayer menu.
     *
     * @return the session, or null when not logged in to the multiplayer server
     */
    static @Nullable EditorSession host(@NonNull String name, @NonNull MapSettings settings, @NonNull Editor editor) {
        if (NETWORK.server() == null || !canHost())
            return null;
        leaveCurrent();
        EditorSession session = new EditorSession(name, true, NETWORK);
        current = session;
        session.open(editor, settings.size(), settings.terrain());
        return session;
    }

    /**
     * Asks the server to open this session, with the editor attached at once since it has the island.
     *
     * @param size_index the island's size as a {@code Game.SIZE_} index
     */
    void open(@NonNull Editor editor, int size_index, int terrain) {
        attach(editor);
        MatchmakingServerInterface server = server();
        if (server != null)
            server.hostEditorSession(name, size_index, terrain);
    }

    /**
     * Joins a session; the listener hears when the island is here to build the editor on.
     *
     * @return the session being joined, or null when not connected to the multiplayer server
     */
    static @Nullable EditorSession join(@NonNull EditorSessionInfo info, @NonNull JoinListener listener) {
        if (NETWORK.server() == null) {
            listener.progress(MapEditor.i18n("shared_not_connected"));
            return null;
        }
        leaveCurrent();
        EditorSession session = new EditorSession(info.getName(), false, NETWORK);
        current = session;
        session.join(info.getId(), listener);
        return session;
    }

    /** Asks the server to join this session; the listener hears when the island is here. */
    void join(int session_id, @NonNull JoinListener listener) {
        join_listener = listener;
        MatchmakingServerInterface server = server();
        if (server != null)
            server.joinEditorSession(session_id);
        listener.progress(MapEditor.i18n("session_waiting"));
    }

    private static void leaveCurrent() {
        EditorSession session = current;
        if (session != null)
            session.leave();
    }

    @NonNull
    String getName() {
        return name;
    }

    /** This player's colour slot, or -1 until the server has taken them in. */
    int getSlot() {
        return slot;
    }

    /** The chat messages so far, oldest first. */
    @NonNull
    List<@NonNull String> getChat() {
        return Collections.unmodifiableList(chat);
    }

    boolean isEnded() {
        return ended;
    }

    /** Hands the session to the editor showing the island, laying over it what came in meanwhile. */
    void attach(@NonNull Editor editor) {
        if (ended) {
            // It ended while the island was being built.
            editor.ended(end_reason);
            return;
        }
        this.editor = editor;
        join_listener = null;
        snapshot = null;
        for (Map.Entry<Integer, String> member : members.entrySet())
            editor.memberJoined(member.getKey(), member.getValue());
        for (Map.Entry<Integer, Presence> presence : presences.entrySet())
            editor.presence(presence.getKey(), presence.getValue());
        MatchmakingServerInterface server = server();
        if (!hosting && server != null)
            server.editorSessionReady();
        for (Waiting edit : waiting)
            editor.received(edit.slot(), edit.data());
        waiting.clear();
        for (String nick : snapshot_requests)
            editorSnapshotRequested(nick);
        snapshot_requests.clear();
    }

    /** Leaves the session; the server tells the others. */
    void leave() {
        if (ended)
            return;
        MatchmakingServerInterface server = server();
        if (server != null)
            server.leaveEditorSession();
        end();
    }

    private void end() {
        ended = true;
        editor = null;
        join_listener = null;
        if (current == this)
            current = null;
        transport.listen(null);
    }

    /**
     * Sends an edit of this player's, a {@link SessionMessage}, in pieces the size the server takes.
     *
     * @return the bytes it took
     */
    int sendEdit(byte @NonNull [] data) {
        MatchmakingServerInterface server = server();
        if (server == null || ended || data.length == 0)
            return 0;
        unacknowledged.add(data[0]);
        for (int offset = 0;; offset += EditorSessionInfo.CHUNK_SIZE) {
            int end = Math.min(data.length, offset + EditorSessionInfo.CHUNK_SIZE);
            server.sendEditorEdit(Arrays.copyOfRange(data, offset, end), end == data.length);
            if (end == data.length)
                return data.length;
        }
    }

    /** Sends a chat message to everyone in the session; it comes back from the server like the others' do. */
    void sendChat(@NonNull String message) {
        MatchmakingServerInterface server = server();
        String text = message.strip();
        if (server == null || ended || text.isEmpty())
            return;
        server.sendEditorChat(text.length() > EditorSessionInfo.MAX_CHAT_LENGTH ? text.substring(0,
                EditorSessionInfo.MAX_CHAT_LENGTH) : text);
    }

    void sendPresence(@NonNull Presence presence) {
        MatchmakingServerInterface server = server();
        if (server == null || ended)
            return;
        server.sendEditorPresence(presence.x(), presence.y(), presence.z(), presence.horiz_angle(),
                presence.vert_angle(), presence.cursor_x(), presence.cursor_y(), presence.radius(), presence.brush());
    }

    private void fail(@NonNull String reason) {
        end_reason = reason;
        Editor attached = editor;
        JoinListener joining = join_listener;
        end();
        if (attached != null)
            attached.ended(reason);
        else if (joining != null)
            joining.failed(reason);
    }

    // ---- From the server ----

    @Override
    public void editorSessionJoined(int session_id, @NonNull String session_name, int slot) {
        this.slot = slot;
        String nick = transport.nick();
        members.put(slot, nick);
        if (editor != null)
            editor.memberJoined(slot, nick);
    }

    @Override
    public void editorSessionFailed(int error_code) {
        int code = error_code >= EditorSessionInfo.ERROR_NOT_ALLOWED
                && error_code <= EditorSessionInfo.ERROR_NO_MAP ? error_code : EditorSessionInfo.ERROR_NO_SUCH_SESSION;
        fail(MapEditor.i18n("session_error_" + code));
    }

    @Override
    public void editorMemberJoined(int member_slot, @NonNull String nick) {
        members.put(member_slot, nick);
        if (editor != null)
            editor.memberJoined(member_slot, nick);
    }

    @Override
    public void editorMemberLeft(int member_slot) {
        String nick = members.remove(member_slot);
        parts.remove(member_slot);
        presences.remove(member_slot);
        if (editor != null && nick != null)
            editor.memberLeft(member_slot, nick);
    }

    @Override
    public void editorSnapshotRequested(@NonNull String nick) {
        Editor attached = editor;
        MatchmakingServerInterface server = server();
        if (server == null || ended)
            return;
        // Asked while the island is still being built: handed over once it is, with what came in meanwhile on it.
        if (attached == null) {
            if (!snapshot_requests.contains(nick))
                snapshot_requests.add(nick);
            return;
        }
        byte[] file;
        try {
            file = attached.snapshot();
        } catch (IOException e) {
            IO.println("Could not hand the island to " + nick + ": " + e);
            return;
        }
        if (file.length > EditorSessionInfo.MAX_SNAPSHOT_SIZE)
            return;
        for (int offset = 0; offset < file.length; offset += EditorSessionInfo.CHUNK_SIZE)
            server.sendEditorSnapshot(nick, file.length, offset, Arrays.copyOfRange(file, offset, Math.min(
                    file.length, offset + EditorSessionInfo.CHUNK_SIZE)));
    }

    @Override
    public void receiveEditorSnapshot(int total_size, int offset, byte @NonNull [] data) {
        JoinListener joining = join_listener;
        if (joining == null || total_size <= 0 || total_size > EditorSessionInfo.MAX_SNAPSHOT_SIZE)
            return;
        // A piece at the start begins the island over, as when the player sending it left.
        if (offset == 0)
            snapshot = new ByteArrayOutputStream(total_size);
        ByteArrayOutputStream received = snapshot;
        if (received == null || offset != received.size() || offset + data.length > total_size)
            return;
        received.writeBytes(data);
        joining.progress(MapEditor.i18n("session_receiving", Math.round(100f * received.size() / total_size)));
        if (received.size() < total_size)
            return;
        snapshot = null;
        try {
            MapFile map = MapFile.fromBytes(received.toByteArray(), name);
            join_listener = null;
            joining.arrived(this, map);
        } catch (IOException | RuntimeException e) {
            leave();
            joining.failed(MapEditor.i18n("load_failed", e.getMessage()));
        }
    }

    @Override
    public void receiveEditorEdit(int member_slot, byte @NonNull [] data, boolean last) {
        ByteArrayOutputStream pieces = parts.computeIfAbsent(member_slot, _ -> new ByteArrayOutputStream());
        pieces.writeBytes(data);
        if (!last)
            return;
        parts.remove(member_slot);
        // Kept for the editor when the island is still being built.
        if (editor != null)
            editor.received(member_slot, pieces.toByteArray());
        else
            waiting.add(new Waiting(member_slot, pieces.toByteArray()));
    }

    @Override
    public void editorEditAcknowledged() {
        Byte kind = unacknowledged.poll();
        if (kind != null && editor != null)
            editor.acknowledged(kind);
    }

    @Override
    public void receiveEditorPresence(int member_slot, float x, float y, float z, float horiz_angle, float vert_angle,
            float cursor_x, float cursor_y, float radius, int brush) {
        Presence presence = new Presence(x, y, z, horiz_angle, vert_angle, cursor_x, cursor_y, radius, brush);
        presences.put(member_slot, presence);
        if (editor != null)
            editor.presence(member_slot, presence);
    }

    @Override
    public void receiveEditorChat(int member_slot, @NonNull String message) {
        String nick = members.getOrDefault(member_slot, "?");
        if (ChatCommand.isIgnoring(nick))
            return;
        ChatMessage chat_message = new ChatMessage(nick, message, ChatMessage.Type.NORMAL);
        chat.add(chat_message.formatShort());
        if (chat.size() > MAX_CHAT_HISTORY)
            chat.removeFirst();
        // The editor's info lines show it, as they show chat in a game.
        Network.getChatHub().chat(chat_message);
        if (editor != null)
            editor.chatted();
    }

    @Override
    public void connectionClosed() {
        if (!ended)
            fail(MapEditor.i18n("shared_not_connected"));
    }
}

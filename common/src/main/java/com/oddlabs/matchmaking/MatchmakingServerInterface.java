package com.oddlabs.matchmaking;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.HostSequenceID;

public interface MatchmakingServerInterface {
    int TYPE_NONE = 0;
    int TYPE_GAME = 1;
    int TYPE_CHAT_ROOM_LIST = 2;
    int TYPE_RANKING_LIST = 3;
    int TYPE_OPENSKILL_RANKING_LIST = 4;
    int TYPE_OPENSKILL_PERSONAL_RANKING = 5;
    int TYPE_MAP_LIST = 6;
    int TYPE_EDITOR_SESSION_LIST = 7;

    int MATCHMAKING_SERVER_PORT = 33214;

    int MAX_PLAYERS = 12;
    int MIN_PLAYERS = 1;
    int MIN_ROOM_NAME_LENGTH = 1;
    int MAX_ROOM_NAME_LENGTH = 20;
    int MAX_ROOM_USERS = 50;
    String ALLOWED_ROOM_CHARS = "abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ0123456789èéêëìíîïðñòóôõöùúûüýÿ-_,.:;?+={}[]()/&%#!<\\>'*";

    void setProfile(String nick);

    void createProfile(String nick);

    void deleteProfile(String nick);

    void requestProfiles();

    void logPriority(String nick, int priority);

    void registerGame(Game game);

    void unregisterGame();

    void sendMessageToRoom(String msg);

    void sendPrivateMessage(String nick, String msg);

    void joinRoom(String name);

    void leaveRoom();

    void requestInfo(String nick);

    void requestList(int type, int update_key);

    void acceptTunnel(HostSequenceID host_seq);

    void openTunnel(int address_to, int seq);

    void closeTunnel(HostSequenceID address_to);

    void routeEvent(HostSequenceID from, ARMIEvent event);

    void multicastEvent(ARMIEvent event);

    void setMulticast(HostSequenceID[] addresses);

    void gameStartedNotify(GameSession game_session);

    void gameQuitNotify(String nick);

    void freeQuitStopNotify();

    void gameLostNotify();

    void gameWonNotify();

    void updateGameStatus(int tick, int[] status);

    void updateSpectatorInfo(int tick, String info);

    void requestSpectate(String nick);

    void updateCommandEvent(int tick, int client_id, short event_size, byte[] event_data);

    void updateWorldParams(byte[] world_params_data);

    void requestSpectatorEventLog();

    /**
     * Starts uploading a map file, named by its SHA-256 in lower case hex. The server answers with
     * {@link MatchmakingClientInterface#mapUploaded} at once when it already has the file, else with
     * {@link MatchmakingClientInterface#mapUploadProgress} for each chunk it takes.
     */
    void beginMapUpload(String name, String hash, int file_size);

    /** Sends the next {@link SharedMap#CHUNK_SIZE} bytes of the map being uploaded, in order from chunk 0. */
    void uploadMapChunk(int chunk_index, byte[] data);

    /** Asks for one chunk of a shared map's file, answered with a chunk or a failed download. */
    void requestMapChunk(String hash, int chunk_index);

    /** Asks for a shared map's preview picture. */
    void requestMapPreview(String hash);

    /** Takes a shared map off the server; only the profile that uploaded it may. */
    void deleteMap(String hash);

    /**
     * Opens a shared map editor session on the island being edited, answered with
     * {@link MatchmakingClientInterface#editorSessionJoined} or {@link MatchmakingClientInterface#editorSessionFailed}.
     * Leaves any session the player is in.
     *
     * @param size the island size, as a {@code Game.SIZE_} index
     */
    void hostEditorSession(String name, int size, int terrain);

    /**
     * Joins a shared map editor session. The server asks a player in it for the island, which arrives with
     * {@link MatchmakingClientInterface#receiveEditorSnapshot}; edits made from then on come too, to be laid over it.
     */
    void joinEditorSession(int session_id);

    /** The island arrived and is shown, so this player can hand it on to the next to join. */
    void editorSessionReady();

    void leaveEditorSession();

    /**
     * A piece of the island for a player joining, as {@link MatchmakingClientInterface#editorSnapshotRequested} asks:
     * at most {@link EditorSessionInfo#CHUNK_SIZE} bytes at an offset into a map file of the given size, in order.
     */
    void sendEditorSnapshot(String nick, int total_size, int offset, byte[] data);

    /**
     * A piece of an edit, at most {@link EditorSessionInfo#CHUNK_SIZE} bytes. The server puts the edits of the session
     * in one order, passes each on to the others once its last piece is in and acknowledges it to the sender.
     */
    void sendEditorEdit(byte[] data, boolean last);

    /**
     * Where this player's camera is and what their brush is doing, passed on to the others in the session.
     *
     * @param brush the brush, how it is held and whether it is over the ground, packed by the client
     */
    void sendEditorPresence(float x, float y, float z, float horiz_angle, float vert_angle, float cursor_x,
            float cursor_y, float radius, int brush);

    /**
     * A chat message to everyone in the session, this player too, at most {@link EditorSessionInfo#MAX_CHAT_LENGTH}
     * characters. It comes back filtered as the chat rooms filter it.
     */
    void sendEditorChat(String message);
}

package com.oddlabs.matchmaking;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.HostSequenceID;

import java.net.InetAddress;

public interface MatchmakingClientInterface {
    // error codes
    int PROFILE_ERROR_GUEST = 2;

    int USER_ERROR_INVALID_EMAIL = 7;
    int USER_ERROR_NO_SUCH_USER = 8;
    int USER_ERROR_VERSION_TOO_OLD = 9;

    int USERNAME_ERROR_TOO_MANY = 1;
    int USERNAME_ERROR_ALREADY_EXISTS = 3;
    int USERNAME_ERROR_INVALID_CHARACTERS = 4;
    int USERNAME_ERROR_TOO_LONG = 5;
    int USERNAME_ERROR_TOO_SHORT = 6;

    int USER_ERROR_STEAM_REQUIRED = 13;

    int CHAT_ERROR_TOO_MANY_USERS = 10;
    int CHAT_ERROR_INVALID_NAME = 11;
    int CHAT_ERROR_NO_SUCH_NICK = 12;
    int CHAT_ERROR_SPECTATE_FAILED = 14;

    void updateProfileList(Profile[] profiles, String last_profile_nick);

    void updateProfile(Profile profiles);

    void createProfileError(int error_code);

    void createProfileSuccess();

    void joiningChatRoom(String room_name);

    void error(int error_code);

    void receiveChatRoomUsers(ChatRoomUser[] users);

    void receiveChatRoomMessage(String nick, String msg);

    void receivePrivateMessage(String nick, String msg);

    void receiveInfo(Profile profile);

    void updateStart(int type);

    void updateList(int type, Object[] names);

    void updateComplete(int next_update_key);

    void gameWonAck();

    void tunnelOpened(HostSequenceID from, InetAddress inet_address, InetAddress local_inet_address, Profile name);

    void tunnelClosed(HostSequenceID from);

    void tunnelAccepted(HostSequenceID from);

    void receiveRoutedEvent(HostSequenceID from, ARMIEvent event);

    void loginOK(String username, TunnelAddress address);

    void loginError(int error_code);

    void receiveSpectatorData(byte[] world_params_data);

    void receiveSpectatorEventLog(byte[] chunk, int chunk_index, int total_chunks, int current_tick);

    /** The server took the chunks of the map being uploaded up to, but not including, this one. */
    void mapUploadProgress(String hash, int chunks_received);

    /** The server has the uploaded map, now listed. */
    void mapUploaded(SharedMap map);

    /** The server refused the map being uploaded, with one of the {@code SharedMap.ERROR_} codes. */
    void mapUploadFailed(String hash, int error_code);

    void receiveMapChunk(String hash, int chunk_index, int total_chunks, byte[] data);

    /** The server has no map by that hash, or could not read it. */
    void mapDownloadFailed(String hash);

    /**
     * One chunk of a shared map's preview picture: gzipped rows of three bytes per pixel, from south to north. A
     * map without a preview is answered with one empty chunk and a size of 0.
     */
    void receiveMapPreview(String hash, int size, int chunk_index, int total_chunks, byte[] gzipped_rgb);

    /** This player is in a shared map editor session, with a colour slot of their own. */
    void editorSessionJoined(int session_id, String name, int slot);

    /**
     * Hosting or joining a session failed, or the session ended, with one of the {@code EditorSessionInfo.ERROR_}
     * codes.
     */
    void editorSessionFailed(int error_code);

    /** Someone is in the session: told for each player already there on joining, and for each who joins later. */
    void editorMemberJoined(int slot, String nick);

    void editorMemberLeft(int slot);

    /**
     * Asks for the island as it is now, for a player joining; answer with
     * {@link MatchmakingServerInterface#sendEditorSnapshot}.
     */
    void editorSnapshotRequested(String nick);

    /** A piece of the island, while joining. A piece at offset 0 starts the island over, as when its sender left. */
    void receiveEditorSnapshot(int total_size, int offset, byte[] data);

    /** A piece of another player's edit, from the slot of the player who made it. */
    void receiveEditorEdit(int slot, byte[] data, boolean last);

    /** The server put this player's oldest unacknowledged edit in the session's order. */
    void editorEditAcknowledged();

    void receiveEditorPresence(int slot, float x, float y, float z, float horiz_angle, float vert_angle,
            float cursor_x, float cursor_y, float radius, int brush);

    /** A chat message in the session, from the slot of the player who wrote it. */
    void receiveEditorChat(int slot, String message);
}

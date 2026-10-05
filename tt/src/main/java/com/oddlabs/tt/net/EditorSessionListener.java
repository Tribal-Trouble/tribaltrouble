package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;

/** Hears the matchmaking server about the shared map editor session this player is in or joining. */
public interface EditorSessionListener {
    void editorSessionJoined(int session_id, @NonNull String name, int slot);

    void editorSessionFailed(int error_code);

    void editorMemberJoined(int slot, @NonNull String nick);

    void editorMemberLeft(int slot);

    void editorSnapshotRequested(@NonNull String nick);

    void receiveEditorSnapshot(int total_size, int offset, byte @NonNull [] data);

    void receiveEditorEdit(int slot, byte @NonNull [] data, boolean last);

    void editorEditAcknowledged();

    void receiveEditorPresence(int slot, float x, float y, float z, float horiz_angle, float vert_angle,
            float cursor_x, float cursor_y, float radius, int brush);

    void receiveEditorChat(int slot, @NonNull String message);

    /** The connection to the matchmaking server closed, ending the session. */
    void connectionClosed();
}

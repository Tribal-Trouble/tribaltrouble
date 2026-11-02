package com.oddlabs.tt.mapeditor;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * What one edit of a shared session carries, as the server passes it on: a kind, and what the kind says.
 *
 * <ul>
 * <li>{@link #TERRAIN}: heights and resources, as an {@link EditOp}.</li>
 * <li>{@link #SPAWNS}: players' spawns moved or taken away, as {@link SpawnSync} writes them.</li>
 * </ul>
 */
final class SessionMessage {
    static final byte TERRAIN = 1;
    static final byte SPAWNS = 2;

    private static final int HEADER_SIZE = 1;

    private SessionMessage() {
    }

    static byte @NonNull [] encode(byte kind, byte @NonNull [] payload) {
        return ByteBuffer.allocate(HEADER_SIZE + payload.length).put(kind).put(payload).array();
    }

    static byte kind(byte @NonNull [] message) throws IOException {
        check(message);
        return message[0];
    }

    static byte @NonNull [] payload(byte @NonNull [] message) throws IOException {
        check(message);
        return Arrays.copyOfRange(message, HEADER_SIZE, message.length);
    }

    private static void check(byte @NonNull [] message) throws IOException {
        if (message.length < HEADER_SIZE)
            throw new IOException("Edit too short");
    }
}

package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * A map made in the map editor and uploaded to the matchmaking server, as the map browser lists it. The map is
 * known by the SHA-256 of its file, so the same file uploaded twice is one map, and a game names its map by the
 * hash for the players joining it to download.
 */
public final class SharedMap implements Serializable {
    @Serial
    private static final long serialVersionUID = 2;

    /** Bytes of a map file sent in one event, well under the limit of an ARMI event. */
    public static final int CHUNK_SIZE = 16000;
    /** The largest map file the server takes; an edited enormous island is a few megabytes. */
    public static final int MAX_FILE_SIZE = 16 * 1024 * 1024;
    public static final int MAX_NAME_LENGTH = 48;

    public static final int ERROR_NOT_ALLOWED = 1;
    public static final int ERROR_INVALID_NAME = 2;
    public static final int ERROR_TOO_LARGE = 3;
    public static final int ERROR_INVALID_FILE = 4;
    public static final int ERROR_TOO_MANY = 5;
    public static final int ERROR_SERVER = 6;

    private final @NonNull String hash;
    private final @NonNull String name;
    private final @NonNull String author;
    private final byte size;
    private final byte terrain;
    private final boolean edited;
    private final int file_size;
    private final long uploaded;
    private final int downloads;
    private final @NonNull String description;

    public SharedMap(@NonNull String hash, @NonNull String name, @NonNull String author, int size, int terrain,
            boolean edited, int file_size, long uploaded, int downloads, @NonNull String description) {
        this.hash = hash;
        this.name = name;
        this.author = author;
        this.size = (byte) size;
        this.terrain = (byte) terrain;
        this.edited = edited;
        this.file_size = file_size;
        this.uploaded = uploaded;
        this.downloads = downloads;
        this.description = description;
    }

    /** Whether a name can be shown and used as a file name on every platform the game ships on. */
    public static boolean isValidName(@NonNull String name) {
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || name.startsWith(".") || name.endsWith(".")
                || name.endsWith(" ") || name.startsWith(" "))
            return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < ' ' || "/\\:*?\"<>|".indexOf(c) != -1)
                return false;
        }
        return true;
    }

    /** Whether text looks like a map hash: 64 lower case hexadecimal digits. */
    public static boolean isValidHash(@NonNull String hash) {
        if (hash.length() != 64)
            return false;
        for (int i = 0; i < hash.length(); i++) {
            if (Character.digit(hash.charAt(i), 16) == -1 || Character.isUpperCase(hash.charAt(i)))
                return false;
        }
        return true;
    }

    public static int chunkCount(int file_size) {
        return Math.max(1, (file_size + CHUNK_SIZE - 1) / CHUNK_SIZE);
    }

    public @NonNull String getHash() {
        return hash;
    }

    public @NonNull String getName() {
        return name;
    }

    /** The nick of the profile that uploaded the map. */
    public @NonNull String getAuthor() {
        return author;
    }

    /** The island size, as in {@link Game#getSize()}. */
    public byte getSize() {
        return size;
    }

    public byte getTerrainType() {
        return terrain;
    }

    /** Whether the map has edited heights or resources, rather than only generator settings. */
    public boolean isEdited() {
        return edited;
    }

    public int getFileSize() {
        return file_size;
    }

    /** When the map was uploaded, in milliseconds since the epoch. */
    public long getUploaded() {
        return uploaded;
    }

    public int getDownloads() {
        return downloads;
    }

    /** What the map's maker wrote about it, as the server lets others read it; empty when there is nothing. */
    public @NonNull String getDescription() {
        return description;
    }
}

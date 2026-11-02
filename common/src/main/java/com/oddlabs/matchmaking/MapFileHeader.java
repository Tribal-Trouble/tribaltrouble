package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * The start of a map editor file (the client's {@code MapFile}), as far as the matchmaking server needs to read
 * it: the island settings, whether it was edited, and its preview picture.
 *
 * <p>A map file is a gzipped stream of the magic, a version, six settings, whether heights, resources (version 2
 * on) and a preview (version 3 on) follow, the description (version 4 on, in modified UTF-8 as
 * {@link java.io.DataOutput#writeUTF} writes it), the players' spawns (version 5 on: a count, then a player slot
 * byte and two shorts of grid position each), then the preview as a side length and three bytes per pixel.
 *
 * @param size         the island size, as in {@link Game#getSize()}
 * @param terrain      the terrain type, as in {@link Game#getTerrainType()}
 * @param edited       whether the map keeps heights, resources or spawns of its own, beyond its settings
 * @param spawns       how many players' spawns the map's maker picked
 * @param preview_size pixels along each side of the preview, or 0 when the file has none
 * @param description  what the map's maker wrote about it; empty when the file has none
 * @param preview_rgb  three bytes per pixel, rows from south to north, or null when the file has no preview
 */
public record MapFileHeader(int version, int size, int terrain, int hills, int trees, int supplies, int seed,
                            boolean edited, @NonNull String description, int spawns, int preview_size,
                            byte @Nullable [] preview_rgb) {

    public static final int MAGIC = 0x54_54_4D_50; // "TTMP"
    public static final int VERSION = 5;
    /** Bytes each spawn takes: the player slot, then x and y as shorts. */
    public static final int SPAWN_SIZE = 5;
    public static final int MAX_PREVIEW_SIZE = 256;
    /** The longest description a map may have, in characters. */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    /** Reads the header of a whole map file. */
    public static @NonNull MapFileHeader read(byte @NonNull [] file) throws IOException {
        try (var in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(file)))) {
            if (in.readInt() != MAGIC)
                throw new IOException("Not a map file");
            int version = in.readInt();
            if (version < 1 || version > VERSION)
                throw new IOException("Unsupported map version " + version);
            int size = in.readInt();
            int terrain = in.readInt();
            int hills = in.readInt();
            int trees = in.readInt();
            int supplies = in.readInt();
            int seed = in.readInt();
            if (size < Game.SIZE_SMALL || size > Game.SIZE_ARCHIPELAGO || terrain < Game.TERRAIN_TYPE_NATIVE
                    || terrain > Game.TERRAIN_TYPE_VIKING)
                throw new IOException("Bad map settings");
            boolean edited = in.readBoolean();
            if (version >= 2 && in.readBoolean())
                edited = true;
            boolean has_preview = version >= 3 && in.readBoolean();
            String description = version >= 4 ? in.readUTF() : "";
            if (description.length() > MAX_DESCRIPTION_LENGTH)
                throw new IOException("Description too long");
            int spawns = version >= 5 ? in.readUnsignedByte() : 0;
            if (spawns > MatchmakingServerInterface.MAX_PLAYERS)
                throw new IOException("Bad spawn count " + spawns);
            in.skipNBytes((long) spawns * SPAWN_SIZE);
            if (spawns > 0)
                edited = true;
            int preview_size = 0;
            byte[] rgb = null;
            if (has_preview) {
                preview_size = in.readShort();
                if (preview_size <= 0 || preview_size > MAX_PREVIEW_SIZE)
                    throw new IOException("Bad preview size " + preview_size);
                rgb = new byte[preview_size * preview_size * 3];
                in.readFully(rgb);
            }
            return new MapFileHeader(version, size, terrain, hills, trees, supplies, seed, edited, description,
                    spawns, preview_size, rgb);
        }
    }

    /**
     * Checks that a whole map file inflates cleanly, without inflating more than a limit.
     *
     * @throws IOException if the file is broken or inflates to more than the limit
     */
    public static void checkInflates(byte @NonNull [] file, long max_inflated) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(file))) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > max_inflated)
                    throw new IOException("Map file inflates to more than " + max_inflated + " bytes");
            }
        }
    }
}

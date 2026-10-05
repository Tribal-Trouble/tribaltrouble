package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.MapFileHeader;
import com.oddlabs.matchmaking.SharedMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * The maps players have uploaded from the map editor, kept as files in a folder: each map's file as
 * {@code <hash>.ttmap}, next to a {@code <hash>.properties} with its name, author and counts. The folder is read
 * once at start up and the list kept in memory.
 */
final class SharedMapStore {
    private static final String MAP_EXTENSION = ".ttmap";
    private static final String INFO_EXTENSION = ".properties";
    private static final String DEFAULT_DIR = "/var/games/maps";
    private static final int MAX_MAPS_PER_AUTHOR = 50;
    // Listed maps, newest first; the rest stay downloadable by hash.
    private static final int MAX_LISTED = 500;
    // An edited enormous island is 4 MB of heights and some resources; anything far past that is not a map.
    private static final long MAX_INFLATED_SIZE = 64L * 1024 * 1024;

    private static @Nullable SharedMapStore instance;

    /** A refused upload: the file is not a map, or not the one it claims to be. */
    static final class InvalidMapException extends Exception {
        InvalidMapException(@NonNull String message) {
            super(message);
        }
    }

    /** A preview as sent to clients. */
    record Preview(int size, byte @NonNull [] gzipped_rgb) {
    }

    private final @NonNull Path dir;
    private final Map<String, SharedMap> maps = new HashMap<>();
    private final Map<String, Preview> previews = new HashMap<>();

    static @NonNull SharedMapStore getInstance() {
        if (instance == null) {
            String dir = ServerConfiguration.getInstance().get(ServerConfiguration.MAP_DATA_DIR);
            instance = new SharedMapStore(Path.of(dir == null || dir.isEmpty() ? DEFAULT_DIR : dir));
        }
        return instance;
    }

    private SharedMapStore(@NonNull Path dir) {
        this.dir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            MatchmakingServer.getLogger().log(Level.WARNING, "Could not create the map folder " + dir, e);
        }
        load();
        MatchmakingServer.getLogger().info("Shared maps: " + maps.size() + " in " + dir);
    }

    private void load() {
        if (!Files.isDirectory(dir))
            return;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path info : (Iterable<Path>) files::iterator) {
                String file_name = info.getFileName().toString();
                if (!file_name.endsWith(INFO_EXTENSION))
                    continue;
                String hash = file_name.substring(0, file_name.length() - INFO_EXTENSION.length());
                if (!SharedMap.isValidHash(hash) || !Files.isRegularFile(mapPath(hash)))
                    continue;
                try {
                    maps.put(hash, readInfo(hash, info));
                } catch (IOException | RuntimeException e) {
                    MatchmakingServer.getLogger().warning("Skipping unreadable shared map " + info + ": " + e);
                }
            }
        } catch (IOException e) {
            MatchmakingServer.getLogger().log(Level.WARNING, "Could not list the map folder " + dir, e);
        }
    }

    private static @NonNull SharedMap readInfo(@NonNull String hash, @NonNull Path path) throws IOException {
        Properties info = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            info.load(in);
        }
        return new SharedMap(hash, info.getProperty("name"), info.getProperty("author"),
                Integer.parseInt(info.getProperty("size")), Integer.parseInt(info.getProperty("terrain")),
                Boolean.parseBoolean(info.getProperty("edited")), Integer.parseInt(info.getProperty("file_size")),
                Long.parseLong(info.getProperty("uploaded")), Integer.parseInt(info.getProperty("downloads", "0")),
                info.getProperty("description", ""));
    }

    private void writeInfo(@NonNull SharedMap map) throws IOException {
        Properties info = new Properties();
        info.setProperty("name", map.getName());
        info.setProperty("author", map.getAuthor());
        info.setProperty("size", Integer.toString(map.getSize()));
        info.setProperty("terrain", Integer.toString(map.getTerrainType()));
        info.setProperty("edited", Boolean.toString(map.isEdited()));
        info.setProperty("file_size", Integer.toString(map.getFileSize()));
        info.setProperty("uploaded", Long.toString(map.getUploaded()));
        info.setProperty("downloads", Integer.toString(map.getDownloads()));
        info.setProperty("description", map.getDescription());
        Path temp = Files.createTempFile(dir, map.getHash(), ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                info.store(out, null);
            }
            Files.move(temp, infoPath(map.getHash()), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private @NonNull Path mapPath(@NonNull String hash) {
        return dir.resolve(hash + MAP_EXTENSION);
    }

    private @NonNull Path infoPath(@NonNull String hash) {
        return dir.resolve(hash + INFO_EXTENSION);
    }

    @Nullable
    SharedMap get(@NonNull String hash) {
        return maps.get(hash);
    }

    /** The newest maps, newest first. */
    @NonNull
    List<SharedMap> list() {
        List<SharedMap> list = new ArrayList<>(maps.values());
        list.sort(Comparator.comparingLong(SharedMap::getUploaded).reversed());
        return list.size() > MAX_LISTED ? list.subList(0, MAX_LISTED) : list;
    }

    /** Whether a profile may upload another map. */
    boolean canUpload(@NonNull String author) {
        int count = 0;
        for (SharedMap map : maps.values()) {
            if (map.getAuthor().equalsIgnoreCase(author))
                count++;
        }
        return count < MAX_MAPS_PER_AUTHOR;
    }

    /**
     * Checks an uploaded file and keeps it.
     *
     * @throws InvalidMapException if the file is not a map, or its hash is not the one given
     * @throws IOException         if the map could not be stored
     */
    @NonNull
    SharedMap add(@NonNull String name, @NonNull String author, @NonNull String hash,
            byte @NonNull [] file) throws InvalidMapException, IOException {
        if (!hash.equals(sha256(file)))
            throw new InvalidMapException("The file's hash does not match");
        MapFileHeader header;
        try {
            header = MapFileHeader.read(file);
            MapFileHeader.checkInflates(file, MAX_INFLATED_SIZE);
        } catch (IOException e) {
            throw new InvalidMapException(e.getMessage());
        }
        Path temp = Files.createTempFile(dir, hash, ".tmp");
        try {
            Files.write(temp, file);
            Files.move(temp, mapPath(hash), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        SharedMap map = new SharedMap(hash, name, author, header.size(), header.terrain(), header.edited(),
                file.length, System.currentTimeMillis(), 0, censor(header.description()));
        writeInfo(map);
        maps.put(hash, map);
        return map;
    }

    /**
     * Reads one chunk of a map's file. Asking for the first counts as a download.
     *
     * @return the chunk, or null when there is no such map or chunk
     */
    byte @Nullable [] readChunk(@NonNull String hash, int chunk_index) {
        SharedMap map = maps.get(hash);
        if (map == null || chunk_index < 0 || chunk_index >= SharedMap.chunkCount(map.getFileSize()))
            return null;
        long offset = (long) chunk_index * SharedMap.CHUNK_SIZE;
        byte[] data = new byte[(int) Math.min(SharedMap.CHUNK_SIZE, map.getFileSize() - offset)];
        try (RandomAccessFile file = new RandomAccessFile(mapPath(hash).toFile(), "r")) {
            file.seek(offset);
            file.readFully(data);
        } catch (IOException e) {
            MatchmakingServer.getLogger().log(Level.WARNING, "Could not read shared map " + hash, e);
            return null;
        }
        if (chunk_index == 0)
            countDownload(map);
        return data;
    }

    private void countDownload(@NonNull SharedMap map) {
        SharedMap counted = new SharedMap(map.getHash(), map.getName(), map.getAuthor(), map.getSize(),
                map.getTerrainType(), map.isEdited(), map.getFileSize(), map.getUploaded(), map.getDownloads() + 1,
                map.getDescription());
        maps.put(map.getHash(), counted);
        try {
            writeInfo(counted);
        } catch (IOException e) {
            MatchmakingServer.getLogger().log(Level.WARNING, "Could not count a download of " + map.getHash(), e);
        }
    }

    /**
     * A map's preview, gzipped to send.
     *
     * @return the preview, or null when there is no such map or it has no preview
     */
    @Nullable
    Preview getPreview(@NonNull String hash) {
        if (!maps.containsKey(hash))
            return null;
        Preview cached = previews.get(hash);
        if (cached != null)
            return cached;
        MapFileHeader header;
        try {
            header = MapFileHeader.read(Files.readAllBytes(mapPath(hash)));
        } catch (IOException e) {
            MatchmakingServer.getLogger().log(Level.WARNING, "Could not read the preview of " + hash, e);
            return null;
        }
        byte[] rgb = header.preview_rgb();
        if (rgb == null)
            return null;
        Preview preview = new Preview(header.preview_size(), gzip(rgb));
        previews.put(hash, preview);
        return preview;
    }

    /** Takes a map off the server, if the profile uploaded it. */
    boolean delete(@NonNull String hash, @NonNull String author) {
        SharedMap map = maps.get(hash);
        if (map == null || !map.getAuthor().equalsIgnoreCase(author))
            return false;
        maps.remove(hash);
        previews.remove(hash);
        try {
            Files.deleteIfExists(infoPath(hash));
            Files.deleteIfExists(mapPath(hash));
        } catch (IOException e) {
            MatchmakingServer.getLogger().log(Level.WARNING, "Could not delete shared map " + hash, e);
        }
        return true;
    }

    /** A description as others may read it: banned words starred out, line by line as the filter splits on spaces. */
    private static @NonNull String censor(@NonNull String description) {
        String[] lines = description.split("\n", -1);
        for (int i = 0; i < lines.length; i++)
            lines[i] = BannedWordFilter.censorChatMessage(lines[i]);
        return String.join("\n", lines);
    }

    static @NonNull String sha256(byte @NonNull [] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte @NonNull [] gzip(byte @NonNull [] data) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }
}

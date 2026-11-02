package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.MapFileHeader;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.SharedMap;
import com.oddlabs.tt.net.MapTransferListener;
import com.oddlabs.tt.net.MatchmakingClient;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.procedural.LandscapeOverride;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * Moves maps to and from the matchmaking server: uploads, downloads and previews.
 *
 * <p>Maps from the server are kept by hash in a hidden folder among the saved maps, where a game played on one
 * reads it (see {@link SharedMapSource}). A file goes over in chunks, a few at a time and each answered, so the
 * connection's other traffic, its pings among them, never waits behind a whole map.
 */
public final class SharedMaps implements MapTransferListener {
    // Chunks sent or asked for before the first is answered.
    private static final int WINDOW = 4;
    private static final String CACHE_DIR = ".server";
    private static final String DOWNLOADED_DIR = "downloaded";

    private static final SharedMaps instance = new SharedMaps();

    /** Hears how a download goes. */
    public interface DownloadListener {
        default void progress(float fraction) {
        }

        /** @param path where the map is kept among the maps downloaded from the server */
        void downloaded(@NonNull Path path);

        void failed(@NonNull String reason);
    }

    /** Hears how an upload goes. */
    public interface UploadListener {
        default void progress(float fraction) {
        }

        void uploaded(@NonNull SharedMap map);

        void failed(@NonNull String reason);
    }

    private static final class Download {
        final List<DownloadListener> listeners = new ArrayList<>();
        byte @Nullable [] @Nullable [] chunks;
        int requested;
        int received;
    }

    private static final class Upload {
        final @NonNull String hash;
        final byte @NonNull [] file;
        final @NonNull UploadListener listener;
        int sent;

        Upload(@NonNull String hash, byte @NonNull [] file, @NonNull UploadListener listener) {
            this.hash = hash;
            this.file = file;
            this.listener = listener;
        }
    }

    private final Map<String, Download> downloads = new HashMap<>();
    private final Map<String, List<Consumer<@Nullable MapPreview>>> preview_requests = new HashMap<>();
    // Chunks of previews on their way, by map.
    private final Map<String, ByteArrayOutputStream> preview_chunks = new HashMap<>();
    // Previews already read or received; empty when the map has none.
    private final Map<String, Optional<MapPreview>> previews = new HashMap<>();
    private @Nullable Upload upload;

    private SharedMaps() {
    }

    /** The one instance, listening to the matchmaking client. */
    public static @NonNull SharedMaps get() {
        Network.getMatchmakingClient().setMapTransferListener(instance);
        return instance;
    }

    private static @Nullable MatchmakingServerInterface server() {
        MatchmakingClient client = Network.getMatchmakingClient();
        return client.isConnected() ? client.getInterface() : null;
    }

    /** Where a map from the server is kept, or null when the game has no folder for maps. */
    static @Nullable Path cachePath(@NonNull String hash) {
        Path dir = MapEditor.getMapsDir();
        return dir == null ? null : dir.resolve(CACHE_DIR).resolve(hash + MapFile.EXTENSION);
    }

    /** Whether a map from the server is here to play. */
    public static boolean isCached(@NonNull String hash) {
        Path path = cachePath(hash);
        return path != null && Files.isRegularFile(path);
    }

    /** The hash of the shared map a generator builds its island from, or null when it builds no shared map. */
    public static @Nullable String sharedMapOf(@Nullable WorldGenerator generator) {
        if (generator instanceof IslandGenerator island) {
            LandscapeOverride.Source source = island.getOverride();
            if (source instanceof SharedMapSource shared)
                return shared.getHash();
        }
        return null;
    }

    static @NonNull String sha256(byte @NonNull [] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Downloads a map from the server unless it is here already, then hands over where it is kept. A map that
     * cannot be read is not kept.
     */
    public void download(@NonNull String hash, @NonNull DownloadListener listener) {
        Path path = cachePath(hash);
        if (path == null) {
            listener.failed(MapEditor.i18n("no_maps_dir"));
            return;
        }
        if (Files.isRegularFile(path)) {
            listener.downloaded(path);
            return;
        }
        Download download = downloads.get(hash);
        if (download != null) {
            download.listeners.add(listener);
            return;
        }
        MatchmakingServerInterface server = server();
        if (server == null) {
            listener.failed(MapEditor.i18n("shared_not_connected"));
            return;
        }
        download = new Download();
        download.listeners.add(listener);
        downloads.put(hash, download);
        // The first chunk tells how many there are.
        download.requested = 1;
        server.requestMapChunk(hash, 0);
    }

    /** Stops telling a listener about a download; the download itself goes on for any others. */
    public void forget(@NonNull DownloadListener listener) {
        for (Download download : downloads.values())
            download.listeners.remove(listener);
    }

    @Override
    public void receiveMapChunk(@NonNull String hash, int chunk_index, int total_chunks, byte @NonNull [] data) {
        Download download = downloads.get(hash);
        if (download == null)
            return;
        if (download.chunks == null) {
            if (total_chunks <= 0 || total_chunks > SharedMap.chunkCount(SharedMap.MAX_FILE_SIZE)) {
                failDownload(hash, MapEditor.i18n("shared_download_failed"));
                return;
            }
            download.chunks = new byte[total_chunks][];
        }
        byte[][] chunks = download.chunks;
        if (total_chunks != chunks.length || chunk_index < 0 || chunk_index >= chunks.length) {
            failDownload(hash, MapEditor.i18n("shared_download_failed"));
            return;
        }
        if (chunks[chunk_index] == null) {
            chunks[chunk_index] = data;
            download.received++;
        }
        MatchmakingServerInterface server = server();
        while (server != null && download.requested < chunks.length
                && download.requested - download.received < WINDOW) {
            server.requestMapChunk(hash, download.requested++);
        }
        float fraction = download.received / (float) chunks.length;
        for (DownloadListener listener : List.copyOf(download.listeners))
            listener.progress(fraction);
        if (download.received == chunks.length)
            finishDownload(hash, chunks);
    }

    private void finishDownload(@NonNull String hash, byte @NonNull [] @NonNull [] chunks) {
        int size = 0;
        for (byte[] chunk : chunks)
            size += chunk.length;
        byte[] file = new byte[size];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, file, offset, chunk.length);
            offset += chunk.length;
        }
        if (!hash.equals(sha256(file))) {
            failDownload(hash, MapEditor.i18n("shared_download_failed"));
            return;
        }
        Path path = cachePath(hash);
        try {
            if (path == null)
                throw new IOException(MapEditor.i18n("no_maps_dir"));
            store(path, file);
        } catch (IOException e) {
            failDownload(hash, MapEditor.i18n("save_failed", e.getMessage()));
            return;
        }
        try {
            // Read it all now, so a broken map is reported here rather than once the game loads.
            MapFile.load(path);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException _) {
                // Left to be read, and refused, again.
            }
            failDownload(hash, MapEditor.i18n("load_failed", e.getMessage()));
            return;
        }
        Download download = downloads.remove(hash);
        if (download != null) {
            for (DownloadListener listener : download.listeners)
                listener.downloaded(path);
        }
    }

    @Override
    public void mapDownloadFailed(@NonNull String hash) {
        failDownload(hash, MapEditor.i18n("shared_not_found"));
    }

    private void failDownload(@NonNull String hash, @NonNull String reason) {
        Download download = downloads.remove(hash);
        if (download != null) {
            for (DownloadListener listener : download.listeners)
                listener.failed(reason);
        }
    }

    /**
     * Copies a map from the server in among the player's own maps, in the folder for downloaded maps, under its
     * name. A map already there under that name is kept, and the copy is given another name.
     *
     * @return where the copy is
     */
    static @NonNull Path keep(@NonNull Path cached, @NonNull String name) throws IOException {
        Path maps = MapEditor.getMapsDir();
        if (maps == null)
            throw new IOException(MapEditor.i18n("no_maps_dir"));
        Path dir = maps.resolve(DOWNLOADED_DIR);
        Files.createDirectories(dir);
        byte[] file = Files.readAllBytes(cached);
        String base = MapFile.isValidName(name) ? name : "map";
        for (int n = 1;; n++) {
            String candidate = n == 1 ? base : base + " (" + n + ")";
            Path target = MapFile.pathFor(dir, candidate);
            if (!Files.exists(target)) {
                store(target, file);
                return target;
            }
            if (Arrays.equals(Files.readAllBytes(target), file))
                return target;
        }
    }

    private static void store(@NonNull Path target, byte @NonNull [] file) throws IOException {
        Path dir = target.getParent();
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "download", ".tmp");
        try {
            Files.write(temp, file);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Uploads a saved map under a name. A map the server has already is not sent again. The map is also kept with
     * the maps from the server, as a game played on it reads it from there.
     */
    public void upload(@NonNull Path path, @NonNull String name, @NonNull UploadListener listener) {
        MatchmakingServerInterface server = server();
        if (server == null) {
            listener.failed(MapEditor.i18n("shared_not_connected"));
            return;
        }
        byte[] file;
        try {
            file = fileToShare(path);
        } catch (IOException | RuntimeException e) {
            listener.failed(MapEditor.i18n("load_failed", e.getMessage()));
            return;
        }
        if (file.length > SharedMap.MAX_FILE_SIZE) {
            listener.failed(MapEditor.i18n("shared_error_" + SharedMap.ERROR_TOO_LARGE));
            return;
        }
        String hash = sha256(file);
        Path cached = cachePath(hash);
        try {
            if (cached == null)
                throw new IOException(MapEditor.i18n("no_maps_dir"));
            if (!Files.isRegularFile(cached))
                store(cached, file);
        } catch (IOException e) {
            listener.failed(MapEditor.i18n("save_failed", e.getMessage()));
            return;
        }
        if (upload != null)
            upload.listener.failed(MapEditor.i18n("shared_upload_replaced"));
        upload = new Upload(hash, file, listener);
        server.beginMapUpload(name, hash, file.length);
    }

    /** Stops an upload; the server drops what it has of it when the next one begins. */
    public void cancelUpload(@NonNull UploadListener listener) {
        if (upload != null && upload.listener == listener)
            upload = null;
    }

    /**
     * A saved map's file as it is shared: as saved when that is how this build writes it, so a map downloaded
     * and shared again keeps its hash, and written again otherwise, which gives older maps their preview.
     */
    private static byte @NonNull [] fileToShare(@NonNull Path path) throws IOException {
        byte[] raw = Files.readAllBytes(path);
        MapFileHeader header = MapFileHeader.read(raw);
        // This build writes a map without spawns as version 4 and one with them as version 5.
        if (header.version() >= 4 && (header.preview_rgb() != null || !header.edited())) {
            MapFile.load(path);
            return raw;
        }
        return MapFile.load(path).toBytes();
    }

    @Override
    public void mapUploadProgress(@NonNull String hash, int chunks_received) {
        Upload current = upload;
        MatchmakingServerInterface server = server();
        if (current == null || !current.hash.equals(hash) || server == null)
            return;
        int total = SharedMap.chunkCount(current.file.length);
        while (current.sent < total && current.sent - chunks_received < WINDOW) {
            int offset = current.sent * SharedMap.CHUNK_SIZE;
            int length = Math.min(SharedMap.CHUNK_SIZE, current.file.length - offset);
            server.uploadMapChunk(current.sent++, Arrays.copyOfRange(current.file, offset, offset + length));
        }
        current.listener.progress(chunks_received / (float) total);
    }

    @Override
    public void mapUploaded(@NonNull SharedMap map) {
        Upload current = upload;
        if (current == null || !current.hash.equals(map.getHash()))
            return;
        upload = null;
        current.listener.uploaded(map);
    }

    @Override
    public void mapUploadFailed(@NonNull String hash, int error_code) {
        Upload current = upload;
        if (current == null || !current.hash.equals(hash))
            return;
        upload = null;
        String key = error_code >= SharedMap.ERROR_NOT_ALLOWED
                && error_code <= SharedMap.ERROR_SERVER ? "shared_error_" + error_code : "shared_error_" + SharedMap.ERROR_SERVER;
        current.listener.failed(MapEditor.i18n(key));
    }

    /** Fetches a map's preview, from the map when it is here, else from the server; null when it has none. */
    void requestPreview(@NonNull String hash, @NonNull Consumer<@Nullable MapPreview> consumer) {
        Optional<MapPreview> known = previews.get(hash);
        if (known != null) {
            consumer.accept(known.orElse(null));
            return;
        }
        Path path = cachePath(hash);
        if (path != null && Files.isRegularFile(path)) {
            MapPreview preview;
            try {
                preview = MapFile.loadPreview(path);
            } catch (IOException e) {
                preview = null;
            }
            previews.put(hash, Optional.ofNullable(preview));
            consumer.accept(preview);
            return;
        }
        MatchmakingServerInterface server = server();
        if (server == null) {
            consumer.accept(null);
            return;
        }
        List<Consumer<@Nullable MapPreview>> waiting = preview_requests.computeIfAbsent(hash,
                _ -> new ArrayList<>());
        waiting.add(consumer);
        if (waiting.size() == 1)
            server.requestMapPreview(hash);
    }

    @Override
    public void receiveMapPreview(@NonNull String hash, int size, int chunk_index, int total_chunks,
            byte @NonNull [] gzipped_rgb) {
        if (!preview_requests.containsKey(hash))
            return;
        ByteArrayOutputStream chunks = chunk_index == 0 ? new ByteArrayOutputStream() : preview_chunks.get(hash);
        if (chunks == null)
            return;
        chunks.writeBytes(gzipped_rgb);
        if (chunk_index < total_chunks - 1) {
            preview_chunks.put(hash, chunks);
            return;
        }
        preview_chunks.remove(hash);
        MapPreview preview = null;
        if (size > 0 && size <= MapPreview.MAX_SIZE) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(chunks.toByteArray()))) {
                byte[] rgb = in.readNBytes(size * size * 3 + 1);
                if (rgb.length == size * size * 3)
                    preview = new MapPreview(size, rgb);
            } catch (IOException e) {
                IO.println("Could not read the preview of shared map " + hash + ": " + e);
            }
        }
        previews.put(hash, Optional.ofNullable(preview));
        List<Consumer<@Nullable MapPreview>> waiting = preview_requests.remove(hash);
        if (waiting != null) {
            for (Consumer<@Nullable MapPreview> consumer : waiting)
                consumer.accept(preview);
        }
    }

    /** Forgets a map's preview, as when it is taken off the server. */
    void forgetPreview(@NonNull String hash) {
        previews.remove(hash);
    }

    @Override
    public void connectionClosed() {
        String reason = MapEditor.i18n("shared_not_connected");
        for (String hash : List.copyOf(downloads.keySet()))
            failDownload(hash, reason);
        Upload current = upload;
        upload = null;
        if (current != null)
            current.listener.failed(reason);
        Map<String, List<Consumer<@Nullable MapPreview>>> waiting = new HashMap<>(preview_requests);
        preview_requests.clear();
        preview_chunks.clear();
        for (List<Consumer<@Nullable MapPreview>> consumers : waiting.values()) {
            for (Consumer<@Nullable MapPreview> consumer : consumers)
                consumer.accept(null);
        }
    }
}

package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.SharedMap;
import org.jspecify.annotations.NonNull;

/** Hears the matchmaking server's answers about shared maps: uploads, downloads and previews. */
public interface MapTransferListener {
    void mapUploadProgress(@NonNull String hash, int chunks_received);

    void mapUploaded(@NonNull SharedMap map);

    void mapUploadFailed(@NonNull String hash, int error_code);

    void receiveMapChunk(@NonNull String hash, int chunk_index, int total_chunks, byte @NonNull [] data);

    void mapDownloadFailed(@NonNull String hash);

    void receiveMapPreview(@NonNull String hash, int size, int chunk_index, int total_chunks,
            byte @NonNull [] gzipped_rgb);

    /** The connection to the matchmaking server closed, ending every transfer. */
    void connectionClosed();
}

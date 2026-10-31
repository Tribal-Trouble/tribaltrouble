package com.oddlabs.geometry.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Every registry entry that is more than a plain model, written by the geometry converter next to the binary sprites
 * and read by the game at load. One file, JSON, so each value carries its name and the two sides cannot drift.
 */
public record Registry(@NonNull List<AttachmentEntry> attachments, @NonNull List<SkinEntry> skins,
                       @NonNull List<DecorationEntry> decorations, @NonNull List<LightEntry> lights) {

    public static final String FILE = "registry.json";
    /** The event of an entry that is loaded all year. */
    public static final String NO_EVENT = "-";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(@NonNull Path file) throws IOException {
        MAPPER.writeValue(file.toFile(), this);
    }

    public static @NonNull Registry read(@NonNull InputStream in) throws IOException {
        return MAPPER.readValue(in, Registry.class);
    }
}

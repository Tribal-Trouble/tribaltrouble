package com.oddlabs.tt.gamemode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.matchmaking.Preset;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User-saved presets, in memory plus JSON load/save. No built-in presets are shipped — Phase 2's preset grid is empty
 * until the user saves their own.
 */
public final class PresetLibrary {
    private static final Logger logger = Logger.getLogger(PresetLibrary.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<List<Preset>> PRESET_LIST_TYPE = new TypeReference<>() {
    };

    private final @NonNull List<@NonNull Preset> presets = new ArrayList<>();

    public @NonNull List<@NonNull Preset> all() {
        return Collections.unmodifiableList(presets);
    }

    public @NonNull List<@NonNull Preset> forMode(@NonNull GameMode mode) {
        List<Preset> filtered = new ArrayList<>();
        for (Preset preset : presets) {
            if (preset.getMode() == mode) {
                filtered.add(preset);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public @Nullable Preset findById(@NonNull String id) {
        for (Preset preset : presets) {
            if (preset.getId().equals(id)) {
                return preset;
            }
        }
        return null;
    }

    public boolean hasName(@NonNull String name) {
        for (Preset preset : presets) {
            if (preset.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void add(@NonNull Preset preset) {
        presets.add(preset);
    }

    public boolean remove(@NonNull Preset preset) {
        return presets.removeIf(p -> p.getId().equals(preset.getId()));
    }

    public void load(@NonNull Path file) {
        presets.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<Preset> loaded = MAPPER.readValue(file.toFile(), PRESET_LIST_TYPE);
            presets.addAll(loaded);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read presets from " + file + "; starting empty.", e);
        }
    }

    public void save(@NonNull Path file) {
        try {
            MAPPER.writeValue(file.toFile(), presets);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to write presets to " + file, e);
        }
    }
}

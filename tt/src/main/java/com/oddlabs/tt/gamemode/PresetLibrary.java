package com.oddlabs.tt.gamemode;

import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.matchmaking.Preset;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User-saved presets, in memory. JSON load/save is layered on top. No built-in presets are shipped — Phase 2's preset
 * grid is empty until the user saves their own.
 */
public final class PresetLibrary {
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
}

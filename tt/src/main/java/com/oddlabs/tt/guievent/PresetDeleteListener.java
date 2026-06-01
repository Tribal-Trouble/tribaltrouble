package com.oddlabs.tt.guievent;

import com.oddlabs.matchmaking.Preset;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface PresetDeleteListener extends EventListener {
    void presetDeleted(@NonNull Preset preset);
}

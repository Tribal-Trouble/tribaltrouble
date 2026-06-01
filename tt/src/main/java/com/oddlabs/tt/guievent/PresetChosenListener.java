package com.oddlabs.tt.guievent;

import com.oddlabs.matchmaking.Preset;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface PresetChosenListener extends EventListener {
    void presetChosen(@NonNull Preset preset);
}

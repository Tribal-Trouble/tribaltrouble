package com.oddlabs.tt.guievent;

import com.oddlabs.matchmaking.GameMode;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface ModeChosenListener extends EventListener {
    void modeChosen(@NonNull GameMode mode);
}

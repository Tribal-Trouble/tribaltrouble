package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Named bundle of mode options and roster, applied as a one-tap configuration in the create-game dialog.
 */
public final class Preset implements Serializable {
    @Serial
    private static final long serialVersionUID = 2;

    private final @NonNull String id;
    private final @NonNull String name;
    private final @NonNull GameModeOptions mode_options;
    private final @NonNull RosterTemplate roster;
    private final boolean built_in;

    public Preset(@NonNull String id, @NonNull String name, @NonNull GameModeOptions mode_options,
            @NonNull RosterTemplate roster, boolean built_in) {
        this.id = id;
        this.name = name;
        this.mode_options = mode_options;
        this.roster = roster;
        this.built_in = built_in;
    }

    public @NonNull String getId() {
        return id;
    }

    public @NonNull String getName() {
        return name;
    }

    public @NonNull GameMode getMode() {
        return mode_options.getMode();
    }

    public @NonNull GameModeOptions getModeOptions() {
        return mode_options;
    }

    public @NonNull RosterTemplate getRoster() {
        return roster;
    }

    public boolean isBuiltIn() {
        return built_in;
    }
}

package com.oddlabs.matchmaking;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Named bundle of world config, mode options, and roster, applied as a one-tap configuration in the create-game
 * dialog.
 */
public final class Preset implements Serializable {
    @Serial
    private static final long serialVersionUID = 3;

    private final @NonNull String id;
    private final @NonNull String name;
    private final @NonNull WorldConfig world;
    private final @NonNull GameModeOptions mode_options;
    private final @NonNull RosterTemplate roster;
    private final boolean built_in;

    @JsonCreator
    public Preset(@JsonProperty("id") @NonNull String id,
            @JsonProperty("name") @NonNull String name,
            @JsonProperty("world") @Nullable WorldConfig world,
            @JsonProperty("modeOptions") @NonNull GameModeOptions mode_options,
            @JsonProperty("roster") @NonNull RosterTemplate roster,
            @JsonProperty("builtIn") boolean built_in) {
        this.id = id;
        this.name = name;
        this.world = world != null ? world : WorldConfig.defaults();
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

    public @NonNull WorldConfig getWorld() {
        return world;
    }

    @JsonIgnore
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

package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Slot layout that a preset stamps onto the lobby. Rosters live inside presets; no standalone roster library.
 * Open slots that nobody joins stay open. Host is expected to manage slots before hitting start.
 */
public final class RosterTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public enum Fill {
        HOST,
        OPEN,
        CLOSED,
        EASY_AI,
        NORMAL_AI,
        HARD_AI,
    }

    public static final class Slot implements Serializable {
        @Serial
        private static final long serialVersionUID = 1;

        private final @NonNull Fill fill;
        private final @Nullable Integer race;
        private final @Nullable Integer team;

        public Slot(@NonNull Fill fill, @Nullable Integer race, @Nullable Integer team) {
            this.fill = fill;
            this.race = race;
            this.team = team;
        }

        public @NonNull Fill getFill() {
            return fill;
        }

        public @Nullable Integer getRace() {
            return race;
        }

        public @Nullable Integer getTeam() {
            return team;
        }
    }

    private final @NonNull Slot @NonNull [] slots;

    public RosterTemplate(@NonNull Slot @NonNull [] slots) {
        this.slots = Arrays.copyOf(slots, slots.length);
    }

    public @NonNull Slot @NonNull [] getSlots() {
        return Arrays.copyOf(slots, slots.length);
    }
}

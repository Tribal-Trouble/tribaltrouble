package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;

public final class StandardOptions implements GameModeOptions {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean rated;

    private StandardOptions(@NonNull Builder b) {
        this.rated = b.rated;
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public static @NonNull StandardOptions defaults() {
        return new Builder().build();
    }

    @Override
    public @NonNull GameMode getMode() {
        return GameMode.STANDARD;
    }

    public boolean isRated() {
        return rated;
    }

    public static final class Builder {
        private boolean rated = false;

        private Builder() {
        }

        public @NonNull Builder rated(boolean rated) {
            this.rated = rated;
            return this;
        }

        public @NonNull StandardOptions build() {
            return new StandardOptions(this);
        }
    }
}

package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class Game implements Serializable {
    @Serial
    private static final long serialVersionUID = 4;

    // Map size not applicable (tutorials, campaigns use fixed terrain sizes)
    public static final int SIZE_NONE = -1;
    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_LARGE = 2;
    public static final int SIZE_ENORMOUS = 3;

    public static final int TERRAIN_TYPE_NATIVE = 0;
    public static final int TERRAIN_TYPE_VIKING = 1;

    public static final int GAMESPEED_PAUSE = 0;
    public static final int GAMESPEED_SLOW = 1;
    public static final int GAMESPEED_NORMAL = 2;
    public static final int GAMESPEED_FAST = 3;
    public static final int GAMESPEED_LUDICROUS = 4;

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 30;

    private final @NonNull String game_name;
    private final byte size;
    private final byte terrain;
    private final byte hills;
    private final byte trees;
    private final byte supplies;
    private final boolean rated;
    private final byte gamespeed;
    private final String mapcode;
    private final float random_start_pos;
    private final int max_unit_count;
    private final @NonNull GameMode mode;

    private int database_id;

    private Game(@NonNull Builder b) {
        this.game_name = b.game_name;
        this.size = b.size;
        this.terrain = b.terrain;
        this.hills = b.hills;
        this.trees = b.trees;
        this.supplies = b.supplies;
        this.rated = b.rated;
        this.gamespeed = b.gamespeed;
        this.mapcode = b.mapcode;
        this.random_start_pos = b.random_start_pos;
        this.max_unit_count = b.max_unit_count;
        this.mode = b.mode;
        assert isValid() : game_name.length();
    }

    public static @NonNull Builder builder() {
        return new Builder();
    }

    public boolean isValid() {
        return game_name != null && game_name.length() >= MIN_LENGTH && game_name.length() <= MAX_LENGTH;
    }

    public boolean isValidGuestGame() {
        return true;
    }

    public @NonNull String getName() {
        return game_name;
    }

    public byte getSize() {
        return size;
    }

    public byte getTerrainType() {
        return terrain;
    }

    public byte getHills() {
        return hills;
    }

    public byte getTrees() {
        return trees;
    }

    public byte getSupplies() {
        return supplies;
    }

    public boolean isRated() {
        return rated;
    }

    public byte getGamespeed() {
        return gamespeed;
    }

    public String getMapcode() {
        return mapcode;
    }

    public float getRandomStartPos() {
        return random_start_pos;
    }

    public int getMaxUnitCount() {
        return max_unit_count;
    }

    public @NonNull GameMode getMode() {
        return mode;
    }

    public void setDatabaseID(int database_id) {
        this.database_id = database_id;
    }

    public int getDatabaseID() {
        return database_id;
    }

    public static final class Builder {
        private @NonNull String game_name = "";
        private byte size;
        private byte terrain;
        private byte hills;
        private byte trees;
        private byte supplies;
        private boolean rated;
        private byte gamespeed;
        private String mapcode;
        private float random_start_pos;
        private int max_unit_count;
        private @NonNull GameMode mode = GameMode.STANDARD;

        private Builder() {
        }

        public @NonNull Builder name(@NonNull String name) {
            this.game_name = name;
            return this;
        }

        public @NonNull Builder size(byte size) {
            this.size = size;
            return this;
        }

        public @NonNull Builder terrain(byte terrain) {
            this.terrain = terrain;
            return this;
        }

        public @NonNull Builder hills(byte hills) {
            this.hills = hills;
            return this;
        }

        public @NonNull Builder trees(byte trees) {
            this.trees = trees;
            return this;
        }

        public @NonNull Builder supplies(byte supplies) {
            this.supplies = supplies;
            return this;
        }

        public @NonNull Builder rated(boolean rated) {
            this.rated = rated;
            return this;
        }

        public @NonNull Builder gamespeed(byte gamespeed) {
            this.gamespeed = gamespeed;
            return this;
        }

        public @NonNull Builder mapcode(String mapcode) {
            this.mapcode = mapcode;
            return this;
        }

        public @NonNull Builder randomStartPos(float random_start_pos) {
            this.random_start_pos = random_start_pos;
            return this;
        }

        public @NonNull Builder maxUnitCount(int max_unit_count) {
            this.max_unit_count = max_unit_count;
            return this;
        }

        public @NonNull Builder mode(@NonNull GameMode mode) {
            this.mode = mode;
            return this;
        }

        public @NonNull Game build() {
            return new Game(this);
        }
    }
}

package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class Game implements Serializable {
	@Serial
	private static final long serialVersionUID = 3;

	public static final int SIZE_SMALL = 0;
	public static final int SIZE_MEDIUM = 1;
	public static final int SIZE_LARGE = 2;

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
	
	private int database_id;

	public Game(@NonNull String game_name, byte size, byte terrain, byte hills, byte trees, byte supplies, boolean rated, byte gamespeed, String mapcode, float random_start_pos, int max_unit_count) {
		this.game_name = game_name;
		this.size = size;
		this.terrain = terrain;
		this.hills = hills;
		this.trees = trees;
		this.supplies = supplies;
		this.rated = rated;
		this.gamespeed = gamespeed;
		this.mapcode = mapcode;
		this.random_start_pos = random_start_pos;
		this.max_unit_count = max_unit_count;
		assert isValid(): game_name.length();
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

	public void setDatabaseID(int database_id) {
		this.database_id = database_id;
	}

	public int getDatabaseID() {
		return database_id;
	}
}

package com.oddlabs.tt.util;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.registration.RegistrationKeyFormatException;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class ServerMessageBundler {
	private static final ResourceBundle bundle = ResourceBundle.getBundle(ServerMessageBundler.class.getName());
	
	public static @NonNull String getSizeString(int index) {
        return switch (index) {
            case Game.SIZE_SMALL -> Utils.getBundleString(bundle, "size_small");
            case Game.SIZE_MEDIUM -> Utils.getBundleString(bundle, "size_medium");
            case Game.SIZE_LARGE -> Utils.getBundleString(bundle, "size_large");
            default -> throw new RuntimeException();
        };
	}

	public static @NonNull String getTerrainTypeString(int index) {
        return switch (index) {
            case Game.TERRAIN_TYPE_NATIVE -> Utils.getBundleString(bundle, "terrain_type_native");
            case Game.TERRAIN_TYPE_VIKING -> Utils.getBundleString(bundle, "terrain_type_viking");
            default -> throw new RuntimeException();
        };
	}

	public static @NonNull String getRatedString(boolean rated) {
		if (rated)
			return Utils.getBundleString(bundle, "rated_yes");
		else
			return Utils.getBundleString(bundle, "rated_no");
	}

	public static @NonNull String getGamespeedString(int index) {
        return switch (index) {
            case Game.GAMESPEED_PAUSE -> Utils.getBundleString(bundle, "gamespeed_pause");
            case Game.GAMESPEED_SLOW -> Utils.getBundleString(bundle, "gamespeed_slow");
            case Game.GAMESPEED_NORMAL -> Utils.getBundleString(bundle, "gamespeed_normal");
            case Game.GAMESPEED_FAST -> Utils.getBundleString(bundle, "gamespeed_fast");
            case Game.GAMESPEED_LUDICROUS -> Utils.getBundleString(bundle, "gamespeed_ludicrous");
            default -> throw new RuntimeException();
        };
	}

	public static @NonNull String getHillsString(int index) {
		return (10*index) + "%";
	}

	public static @NonNull String getTreesString(int index) {
		return (10*index) + "%";
	}

	public static @NonNull String getSuppliesString(int index) {
		return (10*index) + "%";
	}

	public static @NonNull String getRegistrationKeyFormatExceptionMessage(@NonNull RegistrationKeyFormatException e) {
        return switch (e.getType()) {
            case RegistrationKeyFormatException.TYPE_INVALID_CHAR ->
                    Utils.getBundleString(bundle, "invalid_char", e.getInvalidChar());
            case RegistrationKeyFormatException.TYPE_INVALID_LENGTH ->
                    Utils.getBundleString(bundle, "invalid_length", e.getStrippedLength());
            case RegistrationKeyFormatException.TYPE_INVALID_KEY -> Utils.getBundleString(bundle, "invalid_key");
            default -> throw new RuntimeException();
        };
	}

    private ServerMessageBundler() {
    }
}

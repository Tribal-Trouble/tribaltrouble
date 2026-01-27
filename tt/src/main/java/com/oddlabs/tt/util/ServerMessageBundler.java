package com.oddlabs.tt.util;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.registration.RegistrationKeyFormatException;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class ServerMessageBundler {
	private static final ResourceBundle bundle = ResourceBundle.getBundle(ServerMessageBundler.class.getName());
    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static @NonNull String getSizeString(int index) {
        return i18n(switch (index) {
            case Game.SIZE_SMALL -> "size_small";
            case Game.SIZE_MEDIUM -> "size_medium";
            case Game.SIZE_LARGE -> "size_large";
            default -> throw new IllegalArgumentException("unexpected size: " + index);
        });
	}

	public static @NonNull String getTerrainTypeString(int index) {
        return i18n(switch (index) {
            case Game.TERRAIN_TYPE_NATIVE -> "terrain_type_native";
            case Game.TERRAIN_TYPE_VIKING -> "terrain_type_viking";
            default -> throw new IllegalArgumentException("unexpected terrain_type: " + index);
        });
	}

	public static @NonNull String getRatedString(boolean rated) {
        return i18n(rated ? "rated_yes" : "rated_no");
	}

	public static @NonNull String getGamespeedString(int index) {
        return i18n(switch (index) {
            case Game.GAMESPEED_PAUSE -> "gamespeed_pause";
            case Game.GAMESPEED_SLOW -> "gamespeed_slow";
            case Game.GAMESPEED_NORMAL -> "gamespeed_normal";
            case Game.GAMESPEED_FAST -> "gamespeed_fast";
            case Game.GAMESPEED_LUDICROUS -> "gamespeed_ludicrous";
            default -> throw new IllegalArgumentException("unexpected gamespeed: " + index);
        });
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
                    i18n("invalid_char", e.getInvalidChar());
            case RegistrationKeyFormatException.TYPE_INVALID_LENGTH ->
                    i18n("invalid_length", e.getStrippedLength());
            case RegistrationKeyFormatException.TYPE_INVALID_KEY -> i18n("invalid_key");
            default -> throw new RuntimeException();
        };
	}

    private ServerMessageBundler() {
    }
}

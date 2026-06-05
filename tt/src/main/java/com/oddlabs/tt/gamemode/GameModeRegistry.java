package com.oddlabs.tt.gamemode;

import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.tt.gamemode.standard.StandardModeRules;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lookup from {@link GameMode} to its {@link GameModeRules}. Built-in modes register in the static initializer;
 * adding a mode means adding a put() here plus the rules class.
 */
public final class GameModeRegistry {
    private static final @NonNull Map<@NonNull GameMode, @NonNull GameModeRules> REGISTRY = new EnumMap<>(
            GameMode.class);

    static {
        REGISTRY.put(GameMode.STANDARD, new StandardModeRules());
    }

    private GameModeRegistry() {
    }

    public static @NonNull GameModeRules get(@NonNull GameMode mode) {
        GameModeRules rules = REGISTRY.get(mode);
        if (rules == null) {
            throw new IllegalStateException("No rules registered for mode: " + mode);
        }
        return rules;
    }

    public static boolean isRegistered(@NonNull GameMode mode) {
        return REGISTRY.containsKey(mode);
    }
}

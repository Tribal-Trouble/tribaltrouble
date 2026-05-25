package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;

import java.io.Serializable;

/**
 * Mode-specific options. Each game mode has its own implementation with its own fields.
 * <p>
 * Example: {@link StandardOptions} has {@code rated}. A future {@code WaveDefenseOptions} would have
 * {@code wavesToWin}, {@code difficulty}, etc. Fields are per-mode, not shared across modes.
 * <p>
 * Only {@link #getMode()} is required across implementations.
 * <p>
 * Sealed: new implementations must be listed in {@code permits} below. This lets the compiler verify that any
 * {@code switch (options) {...}} statement handles every mode.
 */
public sealed interface GameModeOptions extends Serializable permits StandardOptions {
    @NonNull
    GameMode getMode();
}

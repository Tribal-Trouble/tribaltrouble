package com.oddlabs.matchmaking;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Schema entry for a single mode-level option. The descriptor lists these; the lobby UI auto-renders a control per
 * entry. Values are stored on the typed {@link GameModeOptions} implementation; this is just the metadata describing
 * one field.
 */
public final class GameModeOption implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public enum Type {
        BOOL,
        INT,
        FLOAT,
        ENUM,
        STRING,
    }

    private final @NonNull String key;
    private final @NonNull Type type;
    private final @NonNull Object default_value;
    private final @NonNull String i18n_key;
    private final @Nullable Object min;
    private final @Nullable Object max;
    private final @Nullable String @Nullable [] enum_values;

    public GameModeOption(@NonNull String key, @NonNull Type type, @NonNull Object default_value,
            @NonNull String i18n_key) {
        this(key, type, default_value, i18n_key, null, null, null);
    }

    public GameModeOption(@NonNull String key, @NonNull Type type, @NonNull Object default_value,
            @NonNull String i18n_key, @Nullable Object min, @Nullable Object max,
            @Nullable String @Nullable [] enum_values) {
        this.key = key;
        this.type = type;
        this.default_value = default_value;
        this.i18n_key = i18n_key;
        this.min = min;
        this.max = max;
        this.enum_values = enum_values;
    }

    public @NonNull String getKey() {
        return key;
    }

    public @NonNull Type getType() {
        return type;
    }

    public @NonNull Object getDefaultValue() {
        return default_value;
    }

    public @NonNull String getI18nKey() {
        return i18n_key;
    }

    public @Nullable Object getMin() {
        return min;
    }

    public @Nullable Object getMax() {
        return max;
    }

    public @Nullable String @Nullable [] getEnumValues() {
        return enum_values;
    }
}

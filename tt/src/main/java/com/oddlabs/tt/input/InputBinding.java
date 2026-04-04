package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Binds a key (with modifiers) to an action.
 */
public record InputBinding(@NonNull Key key, @NonNull Set<@NonNull Modifier> modifiers, @NonNull GameAction action) {
    public InputBinding {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(modifiers, "modifiers");
        Objects.requireNonNull(action, "action");

        modifiers = EnumSet.copyOf(modifiers);
    }

    public boolean matches(@NonNull KeyboardEvent event) {
        return event.keyCode() == key && modifiers.equals(event.modifiers());
    }

    public boolean shift() {
        return modifiers.contains(Modifier.SHIFT);
    }

    public boolean control() {
        return modifiers.contains(Modifier.CONTROL);
    }

    public boolean alt() {
        return modifiers.contains(Modifier.ALT);
    }

    public boolean meta() {
        return modifiers.contains(Modifier.META);
    }
}

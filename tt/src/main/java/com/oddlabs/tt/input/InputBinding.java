package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Binds a key (with modifiers) to an action.
 */
public record InputBinding(@NonNull Key key, @NonNull Set<@NonNull Modifier> modifiers,
                           @NonNull GameAction action) implements Comparable<InputBinding> {

    private static final boolean IS_MACOS = System.getProperty("os.name", "").toLowerCase().contains("mac");

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

    @Override
    public @NonNull String toString() {
        String s = "";
        if (IS_MACOS) {
            if (control()) s = s + "⌃";
            if (alt()) s = s + "⌥";
            if (shift()) s = s + "⇧";
            if (meta()) s = s + "⌘";
        } else {
            if (control()) s = s + "Ctrl+";
            if (alt()) s = s + "Alt+";
            if (shift()) s = s + "Shift+";
            if (meta()) s = s + "Meta+";
        }
        return s + key().getDisplayName();
    }

    @Override
    public int compareTo(@NonNull InputBinding o) {
        if (this.control() != o.control()) {
            return this.control() ? 1 : -1;
        }
        if (this.alt() != o.alt()) {
            return this.alt() ? 1 : -1;
        }
        if (this.shift() != o.shift()) {
            return this.shift() ? 1 : -1;
        }
        if (this.meta() != o.meta()) {
            return this.meta() ? 1 : -1;
        }
        int keyCompare = this.key().getDisplayName().compareTo(o.key().getDisplayName());
        if (keyCompare != 0) {
            return keyCompare;
        }

        return this.action().compareTo(o.action());
    }
}

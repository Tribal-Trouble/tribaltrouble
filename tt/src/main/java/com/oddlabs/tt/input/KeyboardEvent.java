package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record KeyboardEvent(@NonNull Key keyCode, char keyChar, @NonNull Set<@NonNull Modifier> modifiers, int clicks) {
    public KeyboardEvent {
        Objects.requireNonNull(keyCode, "keyCode");
        Objects.requireNonNull(modifiers, "modifiers");

        modifiers = EnumSet.copyOf(modifiers);
    }

    public KeyboardEvent(@NonNull Key keyCode, char keyChar, boolean shiftDown, boolean controlDown, boolean altDown,
            boolean metaDown, int clicks) {
        Set<Modifier> set = EnumSet.noneOf(Modifier.class);
        if (shiftDown) set.add(Modifier.SHIFT);
        if (controlDown) set.add(Modifier.CONTROL);
        if (altDown) set.add(Modifier.ALT);
        if (metaDown) set.add(Modifier.META);

        this(keyCode, keyChar, set, clicks);
    }

    public boolean shiftDown() {
        return modifiers.contains(Modifier.SHIFT);
    }

    public boolean controlDown() {
        return modifiers.contains(Modifier.CONTROL);
    }

    public boolean altDown() {
        return modifiers.contains(Modifier.ALT);
    }

    public boolean metaDown() {
        return modifiers.contains(Modifier.META);
    }
}

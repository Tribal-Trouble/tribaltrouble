package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/** Binds a key (with modifiers) to an action.
 * @param meta aka super aka command aka windows */
public record InputBinding(@NonNull Key key, boolean shift, boolean control, boolean alt, boolean meta, @NonNull GameAction action) {
    public InputBinding {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(action, "action");
    }

    public boolean matches(@NonNull KeyboardEvent event) {
        return event.keyCode() == key &&
               event.shiftDown() == shift &&
               event.controlDown() == control &&
               event.altDown() == alt &&
               event.metaDown() == meta;
    }
}
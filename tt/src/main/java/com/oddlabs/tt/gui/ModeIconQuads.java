package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

import java.util.EnumMap;

/**
 * A related set of icons for different {@link ModeIconQuads.Mode}s.
 */
public class ModeIconQuads extends EnumMap<ModeIconQuads.@NonNull Mode, @NonNull IconQuad> {

    public enum Mode {
        NORMAL,
        ACTIVE,
        DISABLED
    }

    public ModeIconQuads(@NonNull IconQuad normal, @NonNull IconQuad active, @NonNull IconQuad disabled) {
        super(Mode.class);
        put(Mode.NORMAL, normal);
        put(Mode.ACTIVE, active);
        put(Mode.DISABLED, disabled);
    }

    public @NonNull IconQuad quad(@NonNull Mode skinMode) {
        return get(skinMode);
    }
}

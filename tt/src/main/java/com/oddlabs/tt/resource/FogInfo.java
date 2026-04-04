package com.oddlabs.tt.resource;

import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public class FogInfo {

    public enum Mode {
        /**
         * No fog should be applied.
         */
        NONE,
        /**
         * Standard distance-based fog (linear, exp, exp2).
         */
        LINEAR,
        EXP,
        EXP2,
        /**
         * Screen-space radial fog for map view.
         */
        RADIAL
    }

    protected final @NonNull Mode mode;
    protected final @NonNull Vector4fc color;
    protected final float density;
    private boolean enabled = true;

    public FogInfo(@NonNull Mode mode, @NonNull Vector4fc color, float density) {
        this.mode = mode;
        this.color = color;
        this.density = density;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NonNull Vector4fc getColor() {
        return color;
    }

    public float getDensity() {
        return density;
    }

    public @NonNull Mode getMode() {
        return mode;
    }
}

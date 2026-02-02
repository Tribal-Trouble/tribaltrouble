package com.oddlabs.tt.resource;

import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class DistanceFogInfo extends FogInfo {

    private final float start;
    private final float end;
    private final float height_factor;

    public DistanceFogInfo(FogInfo.@NonNull Mode mode, @NonNull Vector4fc color, float density, float height_factor, float start, float end) {
        super(mode, color, density);
        this.height_factor = height_factor;
        this.start = start;
        this.end = end;
    }

    public float getStart() {
        return start;
    }

    public float getEnd() {
        return end;
    }

    public float getHeightFactor() {
        return height_factor;
    }
}

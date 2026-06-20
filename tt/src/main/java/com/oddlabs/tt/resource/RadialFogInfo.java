package com.oddlabs.tt.resource;

import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

public final class RadialFogInfo extends FogInfo {
    private final float radiusScale;

    public RadialFogInfo(@NonNull Color color, float density) {
        this(color, density, 1.0f);
    }

    public RadialFogInfo(@NonNull Color color, float density, float radiusScale) {
        super(Mode.RADIAL, color, density);
        this.radiusScale = radiusScale;
    }

    public float getRadiusScale() {
        return radiusScale;
    }
}

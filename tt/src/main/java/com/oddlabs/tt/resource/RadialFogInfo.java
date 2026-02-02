package com.oddlabs.tt.resource;

import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class RadialFogInfo extends FogInfo {
    private final float radiusScale;

    public RadialFogInfo(@NonNull Vector4fc color, float density) {
        this(color, density, 1.0f);
    }

    public RadialFogInfo(@NonNull Vector4fc color, float density, float radiusScale) {
        super(Mode.RADIAL, color, density);
        this.radiusScale = radiusScale;
    }

    public float getRadiusScale() {
        return radiusScale;
    }
}

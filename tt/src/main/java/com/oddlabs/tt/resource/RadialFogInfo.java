package com.oddlabs.tt.resource;

import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class RadialFogInfo extends FogInfo {

    public RadialFogInfo(@NonNull Vector4fc color, float density) {
        super(Mode.RADIAL, color, density);
    }
}

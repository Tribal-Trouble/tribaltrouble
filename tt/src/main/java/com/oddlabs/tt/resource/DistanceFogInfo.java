package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.util.GLState;
import org.jspecify.annotations.NonNull;

public final class DistanceFogInfo extends FogInfo {

    private final float fog_start;
    private final float fog_end;
    private final float height_factor;

    public DistanceFogInfo(FogInfo.@NonNull Mode fog_mode, float @NonNull [] fog_color, float fog_density, float height_factor, float fog_start, float fog_end) {
        super(fog_mode, fog_color, fog_density);
        this.height_factor = height_factor;
        this.fog_start = fog_start;
        this.fog_end = fog_end;
    }

    public float getFogStart() {
        return fog_start;
    }

    public float getFogEnd() {
        return fog_end;
    }

    public float getHeightFactor() {
        return height_factor;
    }

    @Override
    public @NonNull GLState setup(@NonNull FogShader shader, float camera_z) {
        GLState superState = super.setup(shader, camera_z);
        if (!isEnabled() || fog_mode == Mode.NONE) {
            return superState;
        }

        shader.setUniform(FogShader.FOG_MODE, switch (fog_mode) {
            case EXP -> FogShader.FOG_MODE_EXP;
            case EXP2 -> FogShader.FOG_MODE_EXP2;
            default -> FogShader.FOG_MODE_LINEAR;
        });
        shader.setUniform(FogShader.FOG_COLOR, fog_color[0], fog_color[1], fog_color[2], fog_color[3]);
        shader.setUniform(FogShader.FOG_PARAMS, fog_density, fog_start, fog_end);
        shader.setUniform(FogShader.FOG_HEIGHT_FACTOR, height_factor);
        shader.setUniform(FogShader.CAMERA_HEIGHT, camera_z);
        
        return superState; // The close action is the same as the parent
    }
}

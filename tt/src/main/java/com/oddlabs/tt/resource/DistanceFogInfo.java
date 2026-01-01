package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.util.GLState;
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

    @Override
    public @NonNull GLState setup(@NonNull FogShader shader, float camera_z) {
        GLState superState = super.setup(shader, camera_z);
        if (!isEnabled() || mode == Mode.NONE) {
            return superState;
        }

        shader.setUniform(FogShader.FOG_MODE, switch (mode) {
            case EXP -> FogShader.FOG_MODE_EXP;
            case EXP2 -> FogShader.FOG_MODE_EXP2;
            default -> FogShader.FOG_MODE_LINEAR;
        });
        shader.setUniform(FogShader.FOG_COLOR, color.x(), color.y(), color.z(), color.w());
        shader.setUniform(FogShader.FOG_PARAMS, density, start, end);
        shader.setUniform(FogShader.FOG_HEIGHT_FACTOR, height_factor);
        shader.setUniform(FogShader.CAMERA_HEIGHT, camera_z);
        
        return superState; // The close action is the same as the parent
    }
}

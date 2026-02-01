package com.oddlabs.tt.resource;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.render.state.ScopedState;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class RadialFogInfo extends FogInfo {

    public RadialFogInfo(@NonNull Vector4fc color, float density) {
        super(Mode.RADIAL, color, density);
    }

    @Override
    public @NonNull ScopedState setup(@NonNull FogShader shader, @NonNull CameraState camera_state) {
        ScopedState superState = super.setup(shader, camera_state);
        if (!isEnabled() || mode == Mode.NONE) {
            return superState;
        }

        shader.setUniform(FogShader.FOG_COLOR, color.x(), color.y(), color.z(), color.w());
        shader.setUniform(FogShader.FOG_MODE, FogShader.FOG_MODE_RADIAL);
        shader.setUniform(FogShader.FOG_PARAMS, (float) camera_state.getWidth(), (float) camera_state.getHeight(), density);

        return superState;
    }
}

package com.oddlabs.tt.resource;

import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.util.GLState;
import org.jspecify.annotations.NonNull;

public final class RadialFogInfo extends FogInfo {

    public RadialFogInfo(int fog_color, float fog_density) {
        super(Mode.RADIAL, fog_color, fog_density);
    }

    @Override
    public @NonNull GLState setup(@NonNull FogShader shader, float camera_z) {
        GLState superState = super.setup(shader, camera_z);
        if (!isEnabled() || fog_mode == Mode.NONE) {
            return superState;
        }

        int viewWidth = LocalInput.getViewWidth();
        int viewHeight = LocalInput.getViewHeight();

        shader.setUniform(FogShader.FOG_COLOR, fog_color[0], fog_color[1], fog_color[2], fog_color[3]);
        shader.setUniform(FogShader.FOG_MODE, FogShader.FOG_MODE_RADIAL);
        shader.setUniform(FogShader.FOG_PARAMS, (float) viewWidth, (float) viewHeight, fog_density);

        return superState;
    }
}

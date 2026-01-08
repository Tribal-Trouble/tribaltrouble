package com.oddlabs.tt.resource;

import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.util.GLState;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class RadialFogInfo extends FogInfo {

    public RadialFogInfo(@NonNull Vector4fc color, float density) {
        super(Mode.RADIAL, color, density);
    }

    @Override
    public @NonNull GLState setup(@NonNull FogShader shader, float camera_z) {
        GLState superState = super.setup(shader, camera_z);
        if (!isEnabled() || mode == Mode.NONE) {
            return superState;
        }

        int viewWidth = Renderer.getRenderer().getWindow().getWidth();
        int viewHeight = Renderer.getRenderer().getWindow().getHeight();

        shader.setUniform(FogShader.FOG_COLOR, color.x(), color.y(), color.z(), color.w());
        shader.setUniform(FogShader.FOG_MODE, FogShader.FOG_MODE_RADIAL);
        shader.setUniform(FogShader.FOG_PARAMS, (float) viewWidth, (float) viewHeight, density);

        return superState;
    }
}

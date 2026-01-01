package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.util.GLState;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public class FogInfo {

    public enum Mode {
        /** No fog should be applied. */
        NONE,
        /** Standard distance-based fog (linear, exp, exp2). */
        LINEAR,
        EXP,
        EXP2,
        /** Screen-space radial fog for map view. */
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

    /**
     * Sets up fog uniforms for the provided shader and returns a GLState object
     * that will disable the fog when closed.
     *
     * @return A GLState resource to be used in a try-with-resources block.
     */
    public @NonNull GLState setup(@NonNull FogShader shader, float camera_z) {
        assert shader.inUse();

        if (!enabled || mode == Mode.NONE) {
            shader.setUniform(FogShader.FOG_MODE, -1);
            return () -> {}; // Return a no-op state object
        }

        // Actual setup logic will be in subclasses
        return () -> shader.setUniform(FogShader.FOG_MODE, -1);
    }
}

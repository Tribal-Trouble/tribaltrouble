package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.util.Color;
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
        RADIAL;
    }

    protected final @NonNull Mode fog_mode;
    protected final float @NonNull [] fog_color;
    protected final float fog_density;
    private boolean enabled = true;

    public FogInfo(@NonNull Mode fog_mode, int fog_color, float fog_density) {
        this.fog_mode = fog_mode;
        this.fog_color = Color.argb4f(fog_color);
        this.fog_density = fog_density;
    }

    public FogInfo(@NonNull Mode fog_mode, float @NonNull [] fog_color, float fog_density) {
        this.fog_mode = fog_mode;
        this.fog_color = fog_color;
        this.fog_density = fog_density;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float[] getFogColor() {
        return fog_color;
    }

    public float getFogDensity() {
        return fog_density;
    }

    /**
     * Sets up fog uniforms for the provided shader and returns a GLState object
     * that will disable the fog when closed.
     *
     * @return A GLState resource to be used in a try-with-resources block.
     */
    public @NonNull GLState setup(@NonNull FogShader shader, float camera_z) {
        assert shader.inUse();

        if (!enabled || fog_mode == Mode.NONE) {
            shader.setUniform(FogShader.FOG_MODE, -1);
            return () -> {}; // Return a no-op state object
        }

        // Actual setup logic will be in subclasses
        return () -> shader.setUniform(FogShader.FOG_MODE, -1);
    }
}

package com.oddlabs.tt.render.state;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.resource.DistanceFogInfo;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.RadialFogInfo;
import com.oddlabs.tt.scenery.Water;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Helper class to pack global uniform data into a ByteBuffer according to std140 layout.
 */
public final class GlobalUniforms {

    public void update(@NonNull CameraState camera, float time, float seaLevel, @Nullable Water water,
            @NonNull ByteBuffer buffer) {
        buffer.clear();

        // 0: mat4 projection (64)
        camera.getProjectionMatrix().get(0, buffer);

        // 64: mat4 view (64)
        camera.getModelView().get(64, buffer);

        // 128: vec4 fogColor (16)
        buffer.position(128);
        FogInfo fog = camera.getFog();
        var linearColor = fog.getColor();
        buffer.putFloat(linearColor.r());
        buffer.putFloat(linearColor.g());
        buffer.putFloat(linearColor.b());
        buffer.putFloat(linearColor.a());

        // 144: vec4 fogParams (16)
        // 160: float cameraHeight (4) -- Must be 16-aligned after vec3 in std140
        // 164: float fogHeightFactor (4)
        // 168: float globalTime (4)
        // 172: int fogMode (4)

        int mode = -1;
        float hf = 0f;
        float ch = camera.getCurrentZ();
        float p1 = 0, p2 = 0, p3 = 0;

        if (fog.isEnabled()) {
            if (fog instanceof DistanceFogInfo df) {
                mode = switch (df.getMode()) {
                    case EXP -> FogShader.FOG_MODE_EXP;
                    case EXP2 -> FogShader.FOG_MODE_EXP2;
                    default -> FogShader.FOG_MODE_LINEAR;
                };
                p1 = df.getDensity();
                p2 = df.getStart();
                p3 = df.getEnd();
                hf = df.getHeightFactor();
            } else if (fog instanceof RadialFogInfo rf) {
                mode = FogShader.FOG_MODE_RADIAL;
                p1 = (float) camera.getWidth();
                p2 = (float) camera.getHeight();
                p3 = rf.getDensity();
                hf = rf.getRadiusScale();
            }
        }

        buffer.putFloat(p1);
        buffer.putFloat(p2);
        buffer.putFloat(p3);
        buffer.putFloat(seaLevel);

        buffer.putFloat(ch);
        buffer.putFloat(hf);
        buffer.putFloat(time);
        buffer.putInt(mode);

        buffer.position(176); // Ensure position before water params

        if (water != null) {
            water.putGlobalUniforms(buffer, !camera.inNoDetailMode());
        } else {
            // Pad 128 bytes (Water params size) if not present
            buffer.position(176 + 128);
        }
    }
}

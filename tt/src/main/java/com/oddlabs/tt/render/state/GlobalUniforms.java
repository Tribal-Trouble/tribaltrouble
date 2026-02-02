package com.oddlabs.tt.render.state;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.resource.DistanceFogInfo;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.RadialFogInfo;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Helper class to pack global uniform data into a ByteBuffer according to std140 layout.
 */
public final class GlobalUniforms {
    private final ByteBuffer buffer = BufferUtils.createByteBuffer(256);

    public @NonNull ByteBuffer getBuffer() {
        buffer.flip();
        return buffer;
    }

    public void update(
            @NonNull CameraState camera,
            @NonNull Vector3fc lightDir,
            @NonNull Vector3fc skyAmbient,
            @NonNull Vector3fc groundAmbient,
            float time
    ) {
        buffer.clear();
        
        // 0: mat4 projection (64)
        camera.getProjectionMatrix().get(0, buffer);
        
        // 64: mat4 view (64)
        camera.getModelView().get(64, buffer);
        
        // 128: vec3 lightDir (16 aligned)
        buffer.position(128);
        buffer.putFloat(lightDir.x());
        buffer.putFloat(lightDir.y());
        buffer.putFloat(lightDir.z());
        buffer.putFloat(0f); // padding
        
        // 144: vec3 skyAmbient (16 aligned)
        buffer.putFloat(skyAmbient.x());
        buffer.putFloat(skyAmbient.y());
        buffer.putFloat(skyAmbient.z());
        buffer.putFloat(0f); // padding

        // 160: vec3 groundAmbient (16 aligned)
        buffer.putFloat(groundAmbient.x());
        buffer.putFloat(groundAmbient.y());
        buffer.putFloat(groundAmbient.z());
        buffer.putFloat(0f); // padding
        
        // 176: vec4 fogColor (16)
        FogInfo fog = camera.getFog();
        Vector4fc color = fog.getColor();
        buffer.putFloat(color.x());
        buffer.putFloat(color.y());
        buffer.putFloat(color.z());
        buffer.putFloat(color.w());
        
        // 192: vec3 fogParams (16 aligned)
        // 204: float cameraHeight (4) -- Packed tightly after vec3
        // 208: float fogHeightFactor (4)
        // 212: float globalTime (4)
        // 216: int fogMode (4)
        
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
            }
        }

        buffer.putFloat(p1);
        buffer.putFloat(p2);
        buffer.putFloat(p3);
        // NO padding here; vec3 takes 12 bytes, next float starts at 12 bytes offset (align 4)
        
        buffer.putFloat(ch);
        buffer.putFloat(hf);
        buffer.putFloat(time);
        buffer.putInt(mode);
        
        buffer.position(224); // End of data (220 used, pad to 224 for 16-byte alignment)
    }
}

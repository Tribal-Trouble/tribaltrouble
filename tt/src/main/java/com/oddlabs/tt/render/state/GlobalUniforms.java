package com.oddlabs.tt.render.state;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.resource.DistanceFogInfo;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.RadialFogInfo;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Helper class to pack global uniform data into a ByteBuffer according to std140 layout.
 */
public final class GlobalUniforms {
    public static final int MAX_POINT_LIGHTS = 8;

    private final ByteBuffer buffer = BufferUtils.createByteBuffer(512);

    public @NonNull ByteBuffer getBuffer() {
        buffer.flip();
        return buffer;
    }

    /**
     * @param pointLights up to MAX_POINT_LIGHTS entries of 8 floats each:
     *                    world x, y, z, radius, r, g, b, unused. Positions are transformed to view space here.
     */
    public void update(
            @NonNull CameraState camera,
            @NonNull Vector3fc lightDir,
            @NonNull Vector3fc skyAmbient,
            @NonNull Vector3fc groundAmbient,
            @NonNull Vector3fc sunColor,
            @NonNull Vector3fc sceneTint,
            float time,
            float @NonNull [] pointLights,
            int numPointLights
    ) {
        buffer.clear();

        // 0: mat4 projection (64)
        camera.getProjectionMatrix().get(0, buffer);

        // 64: mat4 view (64)
        camera.getModelView().get(64, buffer);

        // 128: vec3 lightDir (16 aligned)
        // The original FFP code set the light direction via glLightfv(GL_LIGHT0, GL_POSITION),
        // which automatically multiplied by the current modelview matrix, placing it in view space.
        // Shaders compute normals in view space, so we must do the same transform here.
        Matrix4fc viewMatrix = camera.getModelView();
        Vector3f viewLightDir = new Vector3f();
        viewMatrix.transformDirection(lightDir, viewLightDir);
        viewLightDir.normalize();
        buffer.position(128);
        buffer.putFloat(viewLightDir.x);
        buffer.putFloat(viewLightDir.y);
        buffer.putFloat(viewLightDir.z);
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
        // Fog is tinted by the scene tint so night fog darkens with the rest of the scene.
        FogInfo fog = camera.getFog();
        Vector4fc color = fog.getColor();
        buffer.putFloat(color.x() * sceneTint.x());
        buffer.putFloat(color.y() * sceneTint.y());
        buffer.putFloat(color.z() * sceneTint.z());
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
                hf = rf.getRadiusScale();
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

        // 224: vec3 sunColor (16 aligned)
        buffer.position(224);
        buffer.putFloat(sunColor.x());
        buffer.putFloat(sunColor.y());
        buffer.putFloat(sunColor.z());
        buffer.putFloat(0f); // padding

        // 240: vec3 sceneTint, then float numPointLights packed into the same 16-byte slot
        buffer.putFloat(sceneTint.x());
        buffer.putFloat(sceneTint.y());
        buffer.putFloat(sceneTint.z());
        int count = Math.min(numPointLights, MAX_POINT_LIGHTS);
        buffer.putFloat(count);

        // 256: vec4 pointLightPos[8] (view-space xyz + radius), 384: vec4 pointLightColor[8]
        Vector3f viewLightPos = new Vector3f();
        for (int i = 0; i < count; i++) {
            int base = i * 8;
            viewLightPos.set(pointLights[base], pointLights[base + 1], pointLights[base + 2]);
            viewMatrix.transformPosition(viewLightPos);
            buffer.position(256 + i * 16);
            buffer.putFloat(viewLightPos.x);
            buffer.putFloat(viewLightPos.y);
            buffer.putFloat(viewLightPos.z);
            buffer.putFloat(pointLights[base + 3]);
            buffer.position(384 + i * 16);
            buffer.putFloat(pointLights[base + 4]);
            buffer.putFloat(pointLights[base + 5]);
            buffer.putFloat(pointLights[base + 6]);
            buffer.putFloat(0f);
        }
        buffer.position(512);
    }
}

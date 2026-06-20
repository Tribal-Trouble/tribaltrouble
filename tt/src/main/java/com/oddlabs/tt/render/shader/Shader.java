package com.oddlabs.tt.render.shader;

import com.oddlabs.util.Color;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;

/**
 * Base interface for OpenGL shaders.
 */
public interface Shader {
    // Standard Global Header
    String SHADER_HEADER = """
            #version 410 core
            precision highp float;
            precision highp int;
            """;

    // Standard Attribute Locations
    int POSITION_LOC = 0;
    int NORMAL_LOC = 1;
    int TEX_COORD_LOC = 2;
    int COLOR_LOC = 3;

    // Standard Uniform Names
    String PROJECTION_MATRIX = "u_projectionMatrix";
    String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
    String VIEW_MATRIX = "u_viewMatrix";

    // Standard Attribute Names
    String POSITION = "in_Position";
    String NORMAL = "in_Normal";
    String TEX_COORD = "in_TexCoord";
    String COLOR = "in_Color";

    String GLOBAL_STATE_BLOCK = """
            layout(std140) uniform GlobalState {
                mat4 u_projectionMatrix;
                mat4 u_viewMatrix;
                vec4 u_fogColor;
                vec4 u_fogParams;
                float u_cameraHeight;
                float u_fogHeightFactor;
                float u_globalTime;
                int u_fogMode;

                // --- Water Params ---
                vec4 u_waveDirLength[3];       // xy = dir, z = length
                vec4 u_waveAmpSteep[3];        // x = amp, y = steepness
                vec4 u_scrollOffsets;          // xy = offset0, zw = offset1
                float u_waveTime;
                float u_waterRepeatRate;
                float u_waterDetailRepeatRate;
                float _pad2;
            };
            """;

    String COLOR_SPACE_FUNCTIONS = """
            vec3 toLinear(vec3 srgb) {
                return mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), srgb));
            }
            vec4 toLinear(vec4 srgb) {
                return vec4(toLinear(srgb.rgb), srgb.a);
            }
            vec3 toSRGB(vec3 linear) {
                return mix(linear * 12.92, 1.055 * pow(linear, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), linear));
            }
            vec4 toSRGB(vec4 linear) {
                return vec4(toSRGB(linear.rgb), linear.a);
            }
            """;

    boolean inUse();

    int getAttributeLocation(@NonNull String name);

    int getUniformLocation(@NonNull String name);

    void setUniform(@NonNull String name, int @NonNull [] values);

    void setUniform(@NonNull String name, int value);

    void setUniform(@NonNull String name, float value);

    void setUniform(@NonNull String name, boolean value);

    void setUniform(@NonNull String name, float x, float y);

    void setUniform(@NonNull String name, float x, float y, float z);

    /**
     * set the named uniform to the specified color.
     *
     * @param color should be in linear space but is converted if necessary
     */
    void setUniform(@NonNull String name, @NonNull Color color);

    void setUniform(@NonNull String name, @NonNull Matrix4fc matrix);

    void setUniform(@NonNull String name, boolean transpose, @NonNull Matrix4fc matrix);
}

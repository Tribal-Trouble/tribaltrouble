package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;

import java.nio.FloatBuffer;

public interface Shader {
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
            vec3 u_lightDirection;
            vec3 u_globalAmbient;
            vec4 u_fogColor;
            vec3 u_fogParams;
            float u_cameraHeight;
            float u_fogHeightFactor;
            float u_globalTime;
            int u_fogMode;
        };
        """;

    boolean inUse();

    int getAttributeLocation(@NonNull String name);

    int getUniformLocation(@NonNull String name);

    void setUniform(@NonNull String name, int value);

    void setUniform(@NonNull String name, float value);

    void setUniform(@NonNull String name, boolean value);

    void setUniform(@NonNull String name, float x, float y);

    void setUniform(@NonNull String name, float x, float y, float z);

    void setUniform(@NonNull String name, float x, float y, float z, float w);

    void setUniform(@NonNull String name, float @NonNull [] value);

    void setUniformMatrix4(@NonNull String name, boolean transpose, @NonNull FloatBuffer matrix);
}


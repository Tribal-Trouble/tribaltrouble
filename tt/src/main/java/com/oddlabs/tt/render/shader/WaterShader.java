package com.oddlabs.tt.render.shader;

/** A shader for rendering water surfaces. */
public final class WaterShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String TEXTURE_0 = "u_texture0"; // Base water texture
        String TEXTURE_1 = "u_texture1"; // Detail water texture
        String WATER_REPEAT_RATE = "u_waterRepeatRate";
        String WATER_DETAIL_REPEAT_RATE = "u_waterDetailRepeatRate";
        String ENABLE_DETAIL = "u_enableDetail";
        String SCROLL_OFFSET_0 = "u_scrollOffset0";
        String SCROLL_OFFSET_1 = "u_scrollOffset1";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "a_position";
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform float u_waterRepeatRate;
        uniform float u_waterDetailRepeatRate;
        uniform vec2 u_scrollOffset0;
        uniform vec2 u_scrollOffset1;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;
        varying float v_fogDist; // Pass view-space dist for fog calculation

        void main() {
            vec4 worldPosition = u_modelViewMatrix * vec4(a_position, 1.0);
            gl_Position = u_projectionMatrix * worldPosition;
            v_texCoord0 = a_position.xy * u_waterRepeatRate + u_scrollOffset0;
            v_texCoord1 = a_position.xy * u_waterDetailRepeatRate + u_scrollOffset1;
            v_fogDist = length(worldPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 120
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture0;
        uniform sampler2D u_texture1;
        uniform bool u_enableDetail;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams; // x = density/width, y = start/height, z = end/start
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;
        
        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;
        varying float v_fogDist;

        void main() {
            vec4 baseColor = texture2D(u_texture0, v_texCoord0);
            vec4 finalColor = baseColor;

            if (u_enableDetail) {
                vec4 detailColor = texture2D(u_texture1, v_texCoord1);
                finalColor = mix(baseColor, detailColor, detailColor.a);
            }
            
            gl_FragColor = finalColor;

            // Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            
            // Mix RGB with fog, preserve original alpha
            gl_FragColor.rgb = mix(u_fogColor.rgb, gl_FragColor.rgb, fogFactor);
        }
        """;

    public WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

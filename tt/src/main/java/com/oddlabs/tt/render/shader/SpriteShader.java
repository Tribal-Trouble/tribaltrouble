package com.oddlabs.tt.render.shader;

/** Shader for rendering animated 3D sprites (units, buildings). */
public final class SpriteShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String TEXTURE_0 = "u_texture0";
        String TEXTURE_1 = "u_texture1";
        String ENABLE_LIGHTING = "u_enableLighting";
        String ENABLE_TEAM_COLOR = "u_enableTeamColor"; // Decal/Blend mode
        String MODULATE_COLOR = "u_modulateColor"; // Modulate mode
        String REPLACE_MODE = "u_replaceMode";
        String COLOR = "u_color"; // Material/Diffuse color
        String DECAL_COLOR = "u_decalColor"; // Team color
        String DESATURATE = "u_desaturate";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String NORMAL = "a_normal";
        String TEX_COORD = "a_texCoord0";
    }

    private static final String VERTEX_SHADER =
        """
        #version 120
        """ +
        LIGHTING_FUNCTION +
        """
        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec2 a_texCoord0;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform bool u_enableLighting;
        uniform vec4 u_color;
        uniform vec3 u_lightDirection;
        uniform vec3 u_globalAmbient;

        varying vec2 v_texCoord0;
        varying vec4 v_color;
        varying float v_fogDist; // Pass view-space dist for fog calculation

        void main() {
            vec4 viewPosition = u_modelViewMatrix * vec4(a_position, 1.0);
            gl_Position = u_projectionMatrix * viewPosition;
            v_texCoord0 = a_texCoord0;

            if (u_enableLighting) {
                v_color = calculateLighting(a_normal, u_color, u_modelViewMatrix, u_lightDirection, u_globalAmbient);
            } else {
                v_color = u_color;
            }
            v_fogDist = length(viewPosition.xyz);
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
        uniform bool u_enableTeamColor;
        uniform bool u_modulateColor;
        uniform bool u_replaceMode;
        uniform vec4 u_decalColor;
        uniform float u_desaturate;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams; // x = density, y = start, z = end
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;


        varying vec2 v_texCoord0;
        varying vec4 v_color;
        varying float v_fogDist;

        void main() {
            vec4 base = texture2D(u_texture0, v_texCoord0);

            if (u_desaturate > 0.0) {
                // Force texture color to white based on desaturation factor.
                // This ensures a uniform look for ghost buildings, regardless of texture content.
                base.rgb = mix(base.rgb, vec3(1.0), u_desaturate);
            }

            vec4 finalColor;
            if (u_replaceMode) {
                finalColor = base;
            } else if (u_modulateColor) {
                // Modulate: Color * Texture
                finalColor = v_color * base;
                if (finalColor.a <= 0.0) discard;
            } else {
                // Normal/Lit
                finalColor = v_color * base;

                if (u_enableTeamColor) {
                    // Apply Team Color Decal
                    vec4 tex1 = texture2D(u_texture1, v_texCoord0);
                    finalColor.rgb = mix(finalColor.rgb, u_decalColor.rgb, tex1.rgb); 
                }

                if (finalColor.a <= 0.3) discard; // Alpha test
            }

            // Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            gl_FragColor = vec4(mix(u_fogColor.rgb, finalColor.rgb, fogFactor), finalColor.a);
        }
        """;

    public SpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

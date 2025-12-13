package com.oddlabs.tt.render.shader;

/** A simple shader for rendering low-detail trees (position, texture). */
public final class TreeLowDetailShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MVP_MATRIX = "u_mvpMatrix";
        String TEXTURE_0 = "u_texture0";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String TEX_COORD = "a_texCoord0";
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec2 a_texCoord0;

        uniform mat4 u_mvpMatrix;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode; // GL_EXP2, etc. (we'll map to custom enum in shader)
        uniform vec3 u_fogParams; // x = density, y = start, z = end
        // uniform float u_fogHeightFactor; // Unused


        varying vec2 v_texCoord0;
        // varying float v_fogHeight; // Pass model-space height for fog calculation
        varying float v_fogDist; // Pass view-space dist for fog calculation

        void main() {
            vec4 modelPosition = vec4(a_position, 1.0);
            gl_Position = u_mvpMatrix * modelPosition;
            v_texCoord0 = a_texCoord0;
            // v_fogHeight = modelPosition.y; // Pass model-space y
            
            // For fog dist, we need View Space position.
            // u_mvpMatrix is ModelViewProjection. We don't have ModelView separate?
            // If we don't have ModelView, we can't get View Space Z easily without decomposing matrix.
            // BUT, usually MVP = P * MV.
            // Maybe we can use gl_Position.w? In clip space, w is usually -viewZ.
            v_fogDist = gl_Position.w;
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 120
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture0;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams; // x = density, y = start, z = end
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;


        varying vec2 v_texCoord0;
        // varying float v_fogHeight;
        varying float v_fogDist;

        void main() {
            vec4 treeColor = texture2D(u_texture0, v_texCoord0);
            if (treeColor.a <= 0.3) discard; // Hardcoded alpha test for trees

            // Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            gl_FragColor = mix(u_fogColor, treeColor, fogFactor);
        }
        """;

    public TreeLowDetailShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER, STANDARD_ATTRIBUTES);
    }
}

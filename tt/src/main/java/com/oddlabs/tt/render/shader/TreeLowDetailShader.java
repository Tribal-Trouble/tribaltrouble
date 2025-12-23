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
        String POSITION = "in_Position";
        String TEX_COORD = "in_TexCoord";
    }

    private static final String VERTEX_SHADER = """
        #version 410 core

        layout(location = 0) in vec3 in_Position;
        layout(location = 1) in vec2 in_TexCoord;

        uniform mat4 u_mvpMatrix;
        uniform mat4 u_modelViewMatrix; // For fog calculation

        out vec2 v_texCoord0;
        out float v_fogDist;

        void main() {
            gl_Position = u_mvpMatrix * vec4(in_Position, 1.0);
            v_texCoord0 = in_TexCoord;
            
            // For fog dist, we need View Space position.
            vec4 viewPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
            v_fogDist = length(viewPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 410 core
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


        in vec2 v_texCoord0;
        in float v_fogDist;
        
        layout(location = 0) out vec4 out_FragColor;

        void main() {
            vec4 treeColor = texture(u_texture0, v_texCoord0);
            if (treeColor.a <= 0.3) discard; // Hardcoded alpha test for trees

            // Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            out_FragColor = vec4(mix(u_fogColor.rgb, treeColor.rgb, fogFactor), treeColor.a);
        }
        """;

    public TreeLowDetailShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

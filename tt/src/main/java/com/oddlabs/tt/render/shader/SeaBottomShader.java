package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the sea bottom, including a detail texture and fog.
 */
public final class SeaBottomShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String TEXTURE_1 = "u_texture1"; // Detail texture
        String BASE_COLOR = "u_baseColor";
        String DETAIL_SCALE = "u_detailScale";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String TEX_COORD_0 = "a_texCoord0"; // Base tex coords, if needed
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec2 a_texCoord0;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform float u_detailScale;

        varying vec2 v_texCoordDetail;
        varying float v_fogDist;

        void main() {
            vec4 worldPosition = u_modelViewMatrix * vec4(a_position, 1.0);
            gl_Position = u_projectionMatrix * worldPosition;
        
            // Generate detail coords from world position: s = x * scale, t = y * scale
            v_texCoordDetail = a_position.xy * u_detailScale;
        
            v_fogDist = length(worldPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 120
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture1; // Detail texture
        uniform vec4 u_baseColor;
        uniform float u_detailScale;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams; // x = density, y = start, z = end
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;

        varying vec2 v_texCoordDetail;
        varying float v_fogDist;

        void main() {
            vec4 color = u_baseColor;
        
            // If we have a detail texture (indicated by a non-zero scale)
            if (u_detailScale > 0.0001) {
               vec4 detail = texture2D(u_texture1, v_texCoordDetail);
               // Replicates GL_DECAL: Mixes the RGB of the base color and detail
               // color using the detail texture's alpha as the interpolant.
               color.rgb = mix(color.rgb, detail.rgb, detail.a);
            }
        
            gl_FragColor = color;

            // Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            gl_FragColor = vec4(mix(u_fogColor.rgb, gl_FragColor.rgb, fogFactor), gl_FragColor.a);
        }
        """;

    public SeaBottomShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

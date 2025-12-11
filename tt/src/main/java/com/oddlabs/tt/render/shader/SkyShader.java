package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the sky dome with two scrolling cloud layers and height-based fog.
 */
public final class SkyShader extends ShaderProgram {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String TEXTURE_0 = "u_texture0"; // Inner clouds
        String TEXTURE_1 = "u_texture1"; // Outer clouds
        String INNER_OFFSET = "u_innerOffset";
        String OUTER_OFFSET = "u_outerOffset";
        String SKY_COLOR = "u_skyColor";

        // Fog Uniforms
        String FOG_COLOR = "u_fogColor";
        String FOG_FADE_START = "u_fogFadeStart"; // The normal.z where fog is at maximum (horizon)
        String FOG_FADE_END = "u_fogFadeEnd";   // The normal.z where fog is at zero (zenith)
        String CAMERA_HEIGHT = "u_cameraHeight";
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String NORMAL = "a_normal";
        String TEX_COORD_0 = "a_texCoord0";
        String TEX_COORD_1 = "a_texCoord1";
        String COLOR = "a_color";
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec2 a_texCoord0;
        attribute vec2 a_texCoord1;
        attribute vec3 a_color;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform vec2 u_innerOffset;
        uniform vec2 u_outerOffset;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;
        varying vec4 v_color;
        varying vec3 v_normal;

        void main() {
            gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(a_position, 1.0);
            
            // Pass the normal to the fragment shader for fog calculation.
            // Since the sky dome is not rotated, the model-space normal is also world-space.
            v_normal = a_normal;
            
            // Apply scrolling offsets for cloud textures
            v_texCoord0 = a_texCoord0 + u_innerOffset;
            v_texCoord1 = a_texCoord1 + u_outerOffset;
            v_color = vec4(a_color, 1.0); 
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 120

        uniform sampler2D u_texture0;
        uniform sampler2D u_texture1;
        uniform vec4 u_skyColor;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform float u_fogFadeStart;
        uniform float u_fogFadeEnd;
        uniform float u_cameraHeight;
        uniform float u_fogHeightFactor;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;
        varying vec4 v_color; // Vertex color (gradient for sky)
        varying vec3 v_normal;

        void main() {
            vec4 tex0 = texture2D(u_texture0, v_texCoord0);
            vec4 tex1 = texture2D(u_texture1, v_texCoord1);
            
            // Layer 0 (Inner Clouds): Mix vertex color with sky color based on texture
            vec3 color0 = mix(v_color.rgb, u_skyColor.rgb, tex0.rgb); 
            
            // Layer 1 (Outer Clouds): Mix previous result with sky color based on texture
            vec3 color1 = mix(color0, u_skyColor.rgb, tex1.rgb);
            
            vec4 finalColor = vec4(color1, 1.0);

            // Apply fog based on the world-space normal's Z component.
            float fogFactor = 1.0 - smoothstep(u_fogFadeStart, u_fogFadeEnd, v_normal.z);
            
            // Reduce fog effect as camera gains altitude
            if (u_fogHeightFactor > 0.0) {
                fogFactor *= (1.0 - clamp(u_cameraHeight / u_fogHeightFactor, 0.0, 1.0));
            }
            
            // Mix the final sky color with the fog color.
            gl_FragColor.rgb = mix(finalColor.rgb, u_fogColor.rgb, fogFactor * 0.25);
            gl_FragColor.a = finalColor.a;
        }
        """;

    public SkyShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

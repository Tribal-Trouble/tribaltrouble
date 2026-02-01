package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the sky dome with two scrolling cloud layers and height-based fog.
 */
public final class SkyShader extends ShaderProgram {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String TEXTURE_0 = "u_texture0"; // Inner clouds
        String TEXTURE_1 = "u_texture1"; // Outer clouds
        String INNER_OFFSET = "u_innerOffset";
        String OUTER_OFFSET = "u_outerOffset";
        String SKY_COLOR = "u_skyColor";
        String INNER_CLOUD_DENSITY = "u_innerCloudDensity";
        String OUTER_CLOUD_DENSITY = "u_outerCloudDensity";

        // Fog Uniforms
        String FOG_COLOR = "u_fogColor";
        String FOG_FADE_START = "u_fogFadeStart"; // The normal.z where fog is at maximum (horizon)
        String FOG_FADE_END = "u_fogFadeEnd";   // The normal.z where fog is at zero (zenith)
        String CAMERA_HEIGHT = "u_cameraHeight";
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String NORMAL = Shader.NORMAL;
        String TEX_COORD_0 = "in_TexCoord0";
        String TEX_COORD_1 = "in_TexCoord1";
        String COLOR = Shader.COLOR;
    }

    private static final String VERTEX_SHADER = """
        #version 410 core

        layout(location = 0) in vec3 in_Position;
        layout(location = 1) in vec3 in_Normal;
        layout(location = 2) in vec2 in_TexCoord0;
        layout(location = 3) in vec2 in_TexCoord1;
        layout(location = 4) in vec3 in_Color;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform vec2 u_innerOffset;
        uniform vec2 u_outerOffset;

        out vec2 v_texCoord0;
        out vec2 v_texCoord1;
        out vec4 v_color;
        out vec3 v_normal;

        void main() {
            gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(in_Position, 1.0);
            
            v_normal = in_Normal;
            
            v_texCoord0 = in_TexCoord0 + u_innerOffset;
            v_texCoord1 = in_TexCoord1 + u_outerOffset;
            v_color = vec4(in_Color, 1.0); 
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 410 core

        uniform sampler2D u_texture0;
        uniform sampler2D u_texture1;
        uniform vec4 u_skyColor;
        uniform float u_innerCloudDensity;
        uniform float u_outerCloudDensity;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform float u_fogFadeStart;
        uniform float u_fogFadeEnd;
        uniform float u_cameraHeight;
        uniform float u_fogHeightFactor;

        in vec2 v_texCoord0;
        in vec2 v_texCoord1;
        in vec4 v_color; // Vertex color (gradient for sky)
        in vec3 v_normal;
        
        layout(location = 0) out vec4 out_FragColor;

        void main() {
            vec4 tex0 = texture(u_texture0, v_texCoord0);
            vec4 tex1 = texture(u_texture1, v_texCoord1);
        
            float exp0 = exp(-u_innerCloudDensity * 2.0);
            float exp1 = exp(-u_outerCloudDensity * 2.0);
            
            vec3 cloud0 = pow(vec3(tex0.r), vec3(exp0));
            vec3 cloud1 = pow(vec3(tex1.r), vec3(exp1));
            
            vec3 color0 = mix(v_color.rgb, u_skyColor.rgb, cloud0); 
            vec3 color1 = mix(color0, u_skyColor.rgb, cloud1);
            
            vec4 finalColor = vec4(color1, 1.0);

            float fogFactor = 1.0 - smoothstep(u_fogFadeStart, u_fogFadeEnd, v_normal.z);
            
            if (u_fogHeightFactor > 0.0) {
                fogFactor *= (1.0 - clamp(u_cameraHeight / u_fogHeightFactor, 0.0, 1.0));
            }
            
            out_FragColor.rgb = mix(finalColor.rgb, u_fogColor.rgb, fogFactor * 0.25);
            out_FragColor.a = finalColor.a;
        }
        """;

    public SkyShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

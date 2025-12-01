package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/** Shader for rendering animated 3D sprites (units, buildings). */
public final class SpriteShader extends ShaderProgram {

    public static final class Uniforms {
        public static final String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        public static final String PROJECTION_MATRIX = "u_projectionMatrix";
        public static final String TEXTURE_0 = "u_texture0";
        public static final String TEXTURE_1 = "u_texture1";
        public static final String ENABLE_LIGHTING = "u_enableLighting";
        public static final String ENABLE_TEAM_COLOR = "u_enableTeamColor"; // Decal/Blend mode
        public static final String MODULATE_COLOR = "u_modulateColor"; // Modulate mode
        public static final String COLOR = "u_color"; // Material/Diffuse color
        public static final String DECAL_COLOR = "u_decalColor"; // Team color
        public static final String LIGHT_DIR = "u_lightDirection";

        private Uniforms() {}
    }

    public static final class Attributes {
        public static final String POSITION = "a_position";
        public static final String NORMAL = "a_normal";
        public static final String TEX_COORD = "a_texCoord0";

        private Attributes() {}
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec2 a_texCoord0;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform bool u_enableLighting;
        uniform vec4 u_color;
        uniform vec3 u_lightDirection;

        varying vec2 v_texCoord0;
        varying vec4 v_color;

        void main() {
            gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(a_position, 1.0);
            v_texCoord0 = a_texCoord0;

            if (u_enableLighting) {
                // Transform normal to view space
                vec3 normal = normalize((u_modelViewMatrix * vec4(a_normal, 0.0)).xyz);
                // Light direction is assumed to be in View Space already or we transform it.
                // DefaultRenderer sets light pos {-1, 0, 1, 0}.
                // If we pass it as is, we assume it's View Space.
                // Diffuse calculation
                float diffuse = max(dot(normal, normalize(u_lightDirection)), 0.0);
                v_color = u_color * (0.3 + 0.7 * diffuse);
            } else {
                v_color = u_color;
            }
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 120

        uniform sampler2D u_texture0;
        uniform sampler2D u_texture1;
        uniform bool u_enableTeamColor;
        uniform bool u_modulateColor;
        uniform vec4 u_decalColor;

        varying vec2 v_texCoord0;
        varying vec4 v_color;

        void main() {
            vec4 base = texture2D(u_texture0, v_texCoord0);

            if (u_modulateColor) {
                // Modulate: Color * Texture
                gl_FragColor = v_color * base;
                if (gl_FragColor.a <= 0.0) discard;
            } else {
                // Normal/Lit
                vec4 color = v_color * base;

                if (u_enableTeamColor) {
                    // Apply Team Color Decal
                    // GL_BLEND env mode: C = Cf*(1-Ct) + Cc*Ct
                    // Ct = texture1 color/alpha.
                    vec4 tex1 = texture2D(u_texture1, v_texCoord0);
                    // Assuming tex1.rgb/a controls the blend factor.
                    // Since textures can be diverse, let's assume alpha or intensity.
                    // Typically team decals are white with alpha? Or greyscale?
                    // Let's try using tex1.a first, fallback to intensity if issues.
                    // Actually, standard GL_BLEND with GL_TEXTURE_2D uses Texture Alpha if GL_ALPHA format,
                    // or Texture RGB if RGB format.
                    // Let's stick to RGB for now as general intensity.
                    float intensity = tex1.r; // Assuming greyscale/red channel
                    
                    // However, FixedFunctionShader usually implies:
                    // If GL_TEXTURE_2D is enabled on unit 1 with GL_BLEND...
                    // Let's assume standard mix.
                    
                    color.rgb = mix(color.rgb, u_decalColor.rgb, tex1.rgb); 
                }

                if (color.a <= 0.3) discard; // Alpha test
                gl_FragColor = color;
            }
        }
        """;

    public SpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

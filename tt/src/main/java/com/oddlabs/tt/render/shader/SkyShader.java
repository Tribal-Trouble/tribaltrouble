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
        String MOON_TEXTURE = "u_moonTexture";
        String INNER_OFFSET = "u_innerOffset";
        String OUTER_OFFSET = "u_outerOffset";
        String SKY_COLOR = "u_skyColor";
        String NIGHT_FACTOR = "u_nightFactor";
        String MOON_DIRECTION = "u_moonDirection";
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
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec3 in_Position;
            layout(location = 1) in vec3 in_Normal;
            layout(location = 2) in vec2 in_TexCoord0;
            layout(location = 4) in vec2 in_TexCoord1;
            layout(location = 3) in vec3 in_Color;

            uniform mat4 u_modelViewMatrix;
            uniform vec2 u_innerOffset;
            uniform vec2 u_outerOffset;

            out vec2 v_texCoord0;
            out vec2 v_texCoord1;
            out vec4 v_color;
            out vec3 v_direction;

            void main() {
                gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(in_Position, 1.0);

                v_direction = in_Normal;
                v_texCoord0 = in_TexCoord0 + u_innerOffset;
                v_texCoord1 = in_TexCoord1 + u_outerOffset;
                v_color = vec4(in_Color, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            uniform sampler2D u_texture0;
            uniform sampler2D u_texture1;
            uniform sampler2D u_moonTexture;
            uniform vec4 u_skyColor;
            uniform float u_nightFactor;
            uniform vec3 u_moonDirection;

            in vec2 v_texCoord0;
            in vec2 v_texCoord1;
            in vec4 v_color; // Vertex color (gradient for sky)
            in vec3 v_direction;

            layout(location = 0) out vec4 out_FragColor;

            const float MOON_SIN_RADIUS = 0.0323;

            void main() {
                vec4 tex0 = texture(u_texture0, v_texCoord0);
                vec4 tex1 = texture(u_texture1, v_texCoord1);

                vec3 dir = normalize(v_direction);
                vec3 moonRight = normalize(cross(u_moonDirection, vec3(0.0, 0.0, 1.0)));
                vec3 moonUp = cross(moonRight, u_moonDirection);
                vec2 moonCoord = vec2(dot(dir, moonRight), dot(dir, moonUp)) / MOON_SIN_RADIUS;
                vec3 moon = vec3(0.0);
                if (u_nightFactor > 0.5 && dot(dir, u_moonDirection) > 0.0 && all(lessThanEqual(abs(moonCoord), vec2(1.0)))) {
                    moon = texture(u_moonTexture, moonCoord * 0.5 + 0.5).rgb;
                }

                // Match original fixed-function GL_BLEND using single-channel cloud textures
                // Cloud textures are luminance stored as R-only in modern GL
                vec3 vc = clamp(v_color.rgb, 0.0, 1.0);
                vec3 sc = clamp(u_skyColor.rgb, 0.0, 1.0) + moon * 10.0;
                float c0 = tex0.r;
                float c1 = tex1.r;
                vec3 color0 = vc * (1.0 - c0) + sc * c0;
                vec3 color1 = color0 * (1.0 - c1) + sc * c1;

                // Night replaces the day palette's hue entirely (the day horizon can be a warm
                // sunset) by remapping sky luminance onto a deep night blue.
                float lum = dot(color1, vec3(0.299, 0.587, 0.114));
                vec3 nightSky = lum * vec3(0.20, 0.26, 0.48);
                out_FragColor = vec4(mix(color1, nightSky, u_nightFactor), 1.0);
            }
            """;

    public SkyShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

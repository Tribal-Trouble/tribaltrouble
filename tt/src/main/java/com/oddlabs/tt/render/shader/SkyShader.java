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
        String SKY_BODY_DIRECTION = "u_skyBodyDirection";
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
            uniform vec3 u_skyBodyDirection;

            in vec2 v_texCoord0;
            in vec2 v_texCoord1;
            in vec4 v_color; // Vertex color (gradient for sky)
            in vec3 v_direction;

            layout(location = 0) out vec4 out_FragColor;

            const float SKY_BODY_SIN_RADIUS = 0.0323;

            void main() {
                vec4 tex0 = texture(u_texture0, v_texCoord0);
                vec4 tex1 = texture(u_texture1, v_texCoord1);

                vec3 dir = normalize(v_direction);
                vec3 skyBodyRight = normalize(cross(u_skyBodyDirection, vec3(0.0, 0.0, 1.0)));
                vec3 skyBodyUp = cross(skyBodyRight, u_skyBodyDirection);
                vec2 skyBodyCoord = vec2(dot(dir, skyBodyRight), dot(dir, skyBodyUp)) / SKY_BODY_SIN_RADIUS;
                vec3 skyBodyColor = vec3(0.0);
                vec3 moonColor = vec3(0.0);
                vec3 sunColor = vec3(0.0);
                if (dot(dir, u_skyBodyDirection) > 0.0 && all(lessThanEqual(abs(skyBodyCoord), vec2(1.0)))) {
                    moonColor = texture(u_moonTexture, skyBodyCoord * 0.5 + 0.5).rgb;
                    sunColor = 0.5 * (1.0 - tanh((length(skyBodyCoord) - 0.95) / 0.0465)) * vec3(1.0, 1.0, 1.0);
                }
                skyBodyColor = moonColor * u_nightFactor + (1.0 - u_nightFactor) * sunColor;
                float skyBodyLum = 0.2126 * skyBodyColor.r + 0.7152 * skyBodyColor.g + skyBodyColor.b;

                // Match original fixed-function GL_BLEND using single-channel cloud textures
                // Cloud textures are luminance stored as R-only in modern GL
                vec3 vc = clamp(v_color.rgb, 0.0, 1.0);
                vec3 sc = clamp(u_skyColor.rgb, 0.0, 1.0) + skyBodyColor * 3.0;
                float c0 = tex0.r;
                float c1 = tex1.r;
                vec3 color0 = vc * (1.0 - c0) + sc * c0;
                vec3 color1 = color0 * (1.0 - c1) + sc * c1;

                // Night replaces the day palette's hue entirely (the day horizon can be a warm
                // sunset) by remapping sky luminance onto a deep night blue.
                float lum = dot(color1, vec3(0.299, 0.587, 0.114)) * (1.0 + skyBodyLum);
                vec3 nightSky = lum * vec3(0.20, 0.26, 0.48);
                vec3 sky = mix(color1 * (1.0 + skyBodyLum), nightSky, u_nightFactor);

                float skyBodyDist = acos(clamp(dot(dir, u_skyBodyDirection), -1.0, 1.0)) / asin(SKY_BODY_SIN_RADIUS);
                float coronaIntensity = 0.35 * u_nightFactor + (1.0 - u_nightFactor) * 0.25;
                float corona = coronaIntensity * exp(-max(skyBodyDist - 1.0, 0.0) * 0.55);
                float halo = 0.19 * exp(-skyBodyDist * 0.55);
                float cloudCover = max(c0, c1);
                vec3 glow = vec3(0.75, 0.82, 1.0) * (corona + halo) * (1.0 + cloudCover);

                float sunRayAngle = atan(skyBodyCoord.y, skyBodyCoord.x);
                vec3 sunRayColor = vec3(1.0, 1.0, 1.0) * max((sin(sunRayAngle * 15.0) + sin(sunRayAngle * 34.0 + 0.5) + sin(sunRayAngle * 60.0 - 1.7)) * 0.4 - 0.25, 0.0) * 4.0 * (1.0 - u_nightFactor) / exp(skyBodyDist + 1.0);

                out_FragColor = vec4(sky + glow + sunRayColor, 1.0);
            }
            """;

    public SkyShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

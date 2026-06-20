package com.oddlabs.tt.render.shader;

/**
 * Rendering of building placement.
 * Uses a two-pass rendering to render the ghost buildings.
 */
public final class PlacingShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String TEXTURE_0 = "u_texture0";
        String TEXTURE_1 = "u_texture1";
        String NORMAL_MAP = "u_normalMap";
        String ENABLE_LIGHTING = "u_enableLighting";
        String ENABLE_TEAM_COLOR = "u_enableTeamColor"; // Decal/Blend mode
        String ENABLE_NORMAL_MAP = "u_enableNormalMap";
        String MODULATE_COLOR = "u_modulateColor"; // Modulate mode
        String REPLACE_MODE = "u_replaceMode";
        String COLOR = "u_color"; // Material/Diffuse color
        String DECAL_COLOR = "u_decalColor"; // Team color
        String DESATURATE = "u_desaturate";
        String ALPHA_TEST_VALUE = "u_alphaTestValue";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = FogShader.FOG_HEIGHT_FACTOR;
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String NORMAL = Shader.NORMAL;
        String TEX_COORD = Shader.TEX_COORD;
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;
                    layout(location = 1) in vec3 in_Normal;
                    layout(location = 2) in vec2 in_TexCoord;

                    uniform mat4 u_modelViewMatrix;
                    uniform bool u_enableLighting;
                    uniform vec4 u_color;

                    out VS_OUT {
                        vec2 texCoord0;
                        vec4 color;
                        float fogDist;
                        vec3 viewPosition;
                        vec3 viewNormal;
                        vec3 worldNormal;
                    } vs_out;

                    void main() {
                        vec4 viewPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                        gl_Position = u_projectionMatrix * viewPosition;
                        vs_out.texCoord0 = in_TexCoord;
                        vs_out.color = u_color;
                        vs_out.fogDist = length(viewPosition.xyz);

                        vs_out.viewPosition = viewPosition.xyz;
                        vs_out.viewNormal = normalize((u_modelViewMatrix * vec4(in_Normal, 0.0)).xyz);
                        vs_out.worldNormal = normalize((transpose(u_viewMatrix) * vec4(vs_out.viewNormal, 0.0)).xyz);
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
            FOG_FUNCTION +
            PERTURB_NORMAL_FUNC +
            FRAGMENT_LIGHTING_FUNCTION +
            """
                    uniform sampler2D u_texture0;
                    uniform sampler2D u_texture1;
                    uniform sampler2D u_normalMap;
                    uniform bool u_enableTeamColor;
                    uniform bool u_enableNormalMap;
                    uniform bool u_enableLighting;
                    uniform bool u_modulateColor;
                    uniform bool u_replaceMode;
                    uniform vec4 u_decalColor;
                    uniform float u_desaturate;
                    uniform float u_alphaTestValue;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec4 color;
                        float fogDist;
                        vec3 viewPosition;
                        vec3 viewNormal;
                        vec3 worldNormal;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;
                    layout(location = 1) out vec4 out_MaskColor;

                    void main() {
                        vec4 base = texture(u_texture0, fs_in.texCoord0);
                        out_MaskColor = vec4(0.0);

                        if (u_desaturate > 0.0) {
                            float gray = dot(base.rgb, vec3(0.2126, 0.7152, 0.0722));
                            vec3 ghostTarget = mix(vec3(gray), vec3(1.0), 0.9);
                            base.rgb = mix(base.rgb, ghostTarget, u_desaturate);
                        }

                        vec4 finalColor;
                        if (u_replaceMode) {
                            finalColor = base;
                        } else if (u_modulateColor) {
                            finalColor = fs_in.color * base;
                        } else {
                            // Apply lighting
                            vec3 normal = normalize(fs_in.viewNormal);
                            float specularStrength = 0.0;

                            if (u_enableNormalMap) {
                                vec4 normalMapVal = texture(u_normalMap, fs_in.texCoord0);
                                normal = perturbNormal(normal, normalize(fs_in.viewPosition), fs_in.texCoord0, normalMapVal.rgb);
                                specularStrength = normalMapVal.a;
                            }

                            vec3 lightIntensity = vec3(1.0);
                            if (u_enableLighting) {
                                lightIntensity = calculateLighting(normal, fs_in.worldNormal, fs_in.viewPosition, specularStrength);
                            }

                            finalColor = vec4(fs_in.color.rgb * base.rgb * lightIntensity, fs_in.color.a * base.a);

                            if (u_enableTeamColor) {
                                vec4 tex1 = texture(u_texture1, fs_in.texCoord0);
                                // Mix decal color
                                vec3 mixedColor = mix(finalColor.rgb, u_decalColor.rgb * lightIntensity, tex1.rgb);
                                finalColor.rgb = mixedColor;

                                // Write to Mask Buffer (Team Color)
                                if (base.a > 0.1) {
                                    out_MaskColor = u_decalColor;
                                }
                            }
                        }

                        if (finalColor.a <= u_alphaTestValue) discard;

                        float fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
                        vec3 litColor = mix(u_fogColor.rgb, finalColor.rgb, fogFactor);
                        out_FragColor = vec4(litColor, finalColor.a);
                    }
                    """;

    public PlacingShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

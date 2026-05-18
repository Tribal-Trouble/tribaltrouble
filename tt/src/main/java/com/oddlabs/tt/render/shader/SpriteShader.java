package com.oddlabs.tt.render.shader;

/**
 * Shader for rendering animated 3D sprites (units, buildings).
 */
public final class SpriteShader extends ShaderProgram implements FogShader, LitShader {

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

        String CLASSIC_LIGHTING = "u_classicLighting";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = FogShader.FOG_HEIGHT_FACTOR;
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String NORMAL = Shader.NORMAL;
        String TEX_COORD = Shader.TEX_COORD;
    }

    private static final String VERTEX_SHADER =
            """
                    #version 410 core
                    """ +
                    GLOBAL_STATE_BLOCK +
                    """
                            layout(location = 0) in vec3 in_Position;
                            layout(location = 1) in vec3 in_Normal;
                            layout(location = 2) in vec2 in_TexCoord;
                            
                            uniform mat4 u_modelViewMatrix;
                            uniform bool u_enableLighting;
                            uniform vec4 u_color;
                            
                            out vec2 v_texCoord0;
                            out vec4 v_color;
                            out float v_fogDist;
                            out vec3 v_viewPosition;
                            out vec3 v_viewNormal;
                            out vec3 v_lightIntensity;
                            
                            void main() {
                                vec4 viewPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
                                gl_Position = u_projectionMatrix * viewPosition;
                                v_texCoord0 = in_TexCoord;
                                v_color = u_color;
                                v_fogDist = length(viewPosition.xyz);
                            
                                v_viewPosition = viewPosition.xyz;
                                v_viewNormal = normalize((u_modelViewMatrix * vec4(in_Normal, 0.0)).xyz);
                            
                                if (u_enableLighting) {
                                    vec3 lightDir = normalize(u_lightDirection);
                                    float diff = max(dot(v_viewNormal, lightDir), 0.0);
                                    v_lightIntensity = u_globalAmbient + vec3(diff);
                                } else {
                                    v_lightIntensity = vec3(1.0);
                                }
                            }
                            """;

    private static final String FRAGMENT_SHADER =
            """
                    #version 410 core
                    """ +
                    GLOBAL_STATE_BLOCK +
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
                            uniform bool u_classicLighting;
                            uniform vec4 u_decalColor;
                            uniform float u_desaturate;
                            uniform float u_alphaTestValue;
                            
                            in vec2 v_texCoord0;
                            in vec4 v_color;
                            in float v_fogDist;
                            in vec3 v_viewPosition;
                            in vec3 v_viewNormal;
                            in vec3 v_lightIntensity;
                            
                            layout(location = 0) out vec4 out_FragColor;
                            layout(location = 1) out vec4 out_MaskColor;
                            
                            void main() {
                                vec4 base = texture(u_texture0, v_texCoord0);
                                out_MaskColor = vec4(0.0);
                            
                                if (u_desaturate > 0.0) {
                                    base.rgb = mix(base.rgb, vec3(1.0), u_desaturate);
                                }
                            
                                vec4 finalColor;
                                if (u_replaceMode) {
                                    finalColor = base;
                                } else if (u_modulateColor) {
                                    finalColor = v_color * base;
                                } else {
                                    vec3 lightIntensity;
                                    if (u_classicLighting) {
                                        lightIntensity = clamp(v_lightIntensity, 0.0, 1.0);
                                    } else {
                                        vec3 normal = normalize(v_viewNormal);
                                        float specularStrength = 0.0;
                                        if (u_enableNormalMap) {
                                            vec4 normalMapVal = texture(u_normalMap, v_texCoord0);
                                            normal = perturbNormal(normal, normalize(v_viewPosition), v_texCoord0, normalMapVal.rgb);
                                            specularStrength = normalMapVal.a;
                                        }
                                        if (u_enableLighting) {
                                            lightIntensity = calculateLighting(normal, v_viewPosition, specularStrength);
                                        } else {
                                            lightIntensity = vec3(1.0);
                                        }
                                    }
                            
                                    finalColor = vec4(v_color.rgb * base.rgb * lightIntensity, v_color.a * base.a);
                            
                                    if (u_enableTeamColor) {
                                        vec4 tex1 = texture(u_texture1, v_texCoord0);
                                        vec3 mixedColor = mix(finalColor.rgb, u_decalColor.rgb * lightIntensity, tex1.rgb);
                                        finalColor.rgb = mixedColor;
                            
                                        if (base.a > 0.1) {
                                            out_MaskColor = u_decalColor;
                                        }
                                    }
                                }
                            
                                if (finalColor.a <= u_alphaTestValue) discard;
                            
                                float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                                out_FragColor = vec4(mix(u_fogColor.rgb, finalColor.rgb, fogFactor), finalColor.a);
                            }
                            """;

    public SpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

package com.oddlabs.tt.render.shader;

/**
 * Shader for rendering animated 3D sprites using hardware instancing.
 * Fetches per-frame vertex data from a Texture Buffer Object (TBO).
 */
public final class InstancedSpriteShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String VIEW_MATRIX = Shader.VIEW_MATRIX;
        String TEXTURE_0 = "u_texture0";
        String TEXTURE_1 = "u_texture1";
        String NORMAL_MAP = "u_normalMap";
        String VERT_BUFFER = "u_VertBuffer";
        String ENABLE_LIGHTING = "u_enableLighting";
        String ENABLE_TEAM_COLOR = "u_enableTeamColor";
        String ENABLE_NORMAL_MAP = "u_enableNormalMap";
        String MODULATE_COLOR = "u_modulateColor";
        String REPLACE_MODE = "u_replaceMode";
        String DESATURATE = "u_desaturate";
        String ALPHA_TEST_VALUE = "u_alphaTestValue";
    }

    public interface Attributes {
        // Per-vertex attributes (now fetched via TBO)
        String TEX_COORD = Shader.TEX_COORD;

        // Per-instance attributes
        String INSTANCE_MODEL_MATRIX = "in_InstanceModelMatrix"; // Occupies 4 locations (4,5,6,7)
        String INSTANCE_COLOR = "in_InstanceColor"; // Location 8
        String INSTANCE_DECAL_COLOR = "in_InstanceDecalColor"; // Location 9

        // Animation attributes
        String INSTANCE_POS_1 = "in_Pos1"; // Location 10
        String INSTANCE_NORM_1 = "in_Norm1"; // Location 11
        String INSTANCE_POS_2 = "in_Pos2"; // Location 12
        String INSTANCE_NORM_2 = "in_Norm2"; // Location 13
        String INSTANCE_TWEEN = "in_Tween"; // Location 14
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                        layout(location = 2) in vec2 in_TexCoord;

                        // Per-instance
                        layout(location = 4) in mat4 in_InstanceModelMatrix;
                        layout(location = 8) in vec4 in_InstanceColor;
                        layout(location = 9) in vec4 in_InstanceDecalColor;
                        layout(location = 10) in float in_Pos1;
                        layout(location = 11) in float in_Norm1;
                        layout(location = 12) in float in_Pos2;
                        layout(location = 13) in float in_Norm2;
                        layout(location = 14) in float in_Tween;

                        uniform samplerBuffer u_VertBuffer;

                        out VS_OUT {
                            vec2 texCoord0;
                            vec4 color;
                            vec4 decalColor;
                            float fogDist;
                            vec3 viewPosition;
                            vec3 viewNormal;
                            vec3 worldNormal;
                        } vs_out;

                        void main() {
                            // Fetch vertex data for both frames
                            // Layout: [Pos...][Norm...] per frame. TBO uses RGB32F (1 texel = 1 vec3).

                            int basePos1 = int(round(in_Pos1));
                            int baseNorm1 = int(round(in_Norm1));
                            int basePos2 = int(round(in_Pos2));
                            int baseNorm2 = int(round(in_Norm2));

                            vec3 pos1 = texelFetch(u_VertBuffer, basePos1 + gl_VertexID).xyz;
                            vec3 norm1 = texelFetch(u_VertBuffer, baseNorm1 + gl_VertexID).xyz;

                            vec3 pos2 = texelFetch(u_VertBuffer, basePos2 + gl_VertexID).xyz;
                            vec3 norm2 = texelFetch(u_VertBuffer, baseNorm2 + gl_VertexID).xyz;

                            vec3 position = mix(pos1, pos2, in_Tween);
                            vec3 normal = normalize(mix(norm1, norm2, in_Tween));

                            // Use the instance matrix (Model Matrix) and global View Matrix
                            vec4 worldPosition = in_InstanceModelMatrix * vec4(position, 1.0);
                            vec4 viewPosition = u_viewMatrix * worldPosition;
                            gl_Position = u_projectionMatrix * viewPosition;

                            vs_out.texCoord0 = in_TexCoord;
                            vs_out.color = in_InstanceColor;
                            vs_out.decalColor = in_InstanceDecalColor;
                            vs_out.fogDist = length(viewPosition.xyz);

                            vs_out.viewPosition = viewPosition.xyz;
                            vs_out.viewNormal = normalize((u_viewMatrix * in_InstanceModelMatrix * vec4(normal, 0.0)).xyz);
                            vs_out.worldNormal = normalize((in_InstanceModelMatrix * vec4(normal, 0.0)).xyz);
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
                    // u_decalColor is now v_decalColor
                    uniform float u_desaturate;
                    uniform float u_alphaTestValue;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec4 color;
                        vec4 decalColor;
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

                            // fs_in.color is the instance color (e.g. material color)
                            finalColor = vec4(fs_in.color.rgb * base.rgb * lightIntensity, fs_in.color.a * base.a);

                            if (u_enableTeamColor) {
                                vec4 tex1 = texture(u_texture1, fs_in.texCoord0);
                                // Mix decal color
                                vec3 mixedColor = mix(finalColor.rgb, fs_in.decalColor.rgb * lightIntensity, tex1.rgb);
                                finalColor.rgb = mixedColor;

                                // Write to Mask Buffer (Team Color)
                                if (base.a > 0.1) {
                                    out_MaskColor = fs_in.decalColor;
                                }
                            }
                        }

                        if (finalColor.a <= u_alphaTestValue) discard;

                        float fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
                        vec3 litColor = mix(u_fogColor.rgb, finalColor.rgb, fogFactor);
                        out_FragColor = vec4(litColor, finalColor.a);
                    }
                    """;

    public InstancedSpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

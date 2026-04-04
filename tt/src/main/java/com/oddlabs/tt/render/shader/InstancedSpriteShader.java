package com.oddlabs.tt.render.shader;

/**
 * Shader for rendering animated 3D sprites using hardware instancing.
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
        String INSTANCE_MODEL_MATRIX = "in_InstanceModelMatrix"; // Occupies 4 locations (3,4,5,6)
        String INSTANCE_COLOR = "in_InstanceColor"; // Location 7
        String INSTANCE_DECAL_COLOR = "in_InstanceDecalColor"; // Location 8

        // Animation attributes
        String INSTANCE_POS_1 = "in_Pos1"; // Location 9
        String INSTANCE_NORM_1 = "in_Norm1"; // Location 10
        String INSTANCE_POS_2 = "in_Pos2"; // Location 11
        String INSTANCE_NORM_2 = "in_Norm2"; // Location 12
        String INSTANCE_TWEEN = "in_Tween"; // Location 13
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ +
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
                    
                        out vec2 v_texCoord0;
                        out vec4 v_color;
                        out vec4 v_decalColor;
                        out float v_fogDist;
                        out vec3 v_viewPosition;
                        out vec3 v_viewNormal;
                    
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
                    
                            v_texCoord0 = in_TexCoord;
                            v_color = in_InstanceColor;
                            v_decalColor = in_InstanceDecalColor;
                            v_fogDist = length(viewPosition.xyz);
                    
                            v_viewPosition = viewPosition.xyz;
                            v_viewNormal = normalize((u_viewMatrix * in_InstanceModelMatrix * vec4(normal, 0.0)).xyz);
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
                            // u_decalColor is now v_decalColor
                            uniform float u_desaturate;
                            uniform float u_alphaTestValue;
                            
                            in vec2 v_texCoord0;
                            in vec4 v_color; // Instance color
                            in vec4 v_decalColor; // Instance decal color
                            in float v_fogDist;
                            in vec3 v_viewPosition;
                            in vec3 v_viewNormal;
                            
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
                                    if (finalColor.a <= u_alphaTestValue) discard;
                                } else if (u_modulateColor) {
                                    finalColor = v_color * base;
                                    if (finalColor.a <= 0.0) discard;
                                } else {
                                    // Apply lighting
                                    vec3 normal = normalize(v_viewNormal);
                                    float specularStrength = 0.0;
                            
                                    if (u_enableNormalMap) {
                                        vec4 normalMapVal = texture(u_normalMap, v_texCoord0);
                                        normal = perturbNormal(normal, normalize(v_viewPosition), v_texCoord0, normalMapVal.rgb);
                                        specularStrength = normalMapVal.a;
                                    }
                            
                                    vec3 lightIntensity = vec3(1.0);
                                    if (u_enableLighting) {
                                        lightIntensity = calculateLighting(normal, v_viewPosition, specularStrength);
                                    }
                            
                                    // v_color is the instance color (e.g. material color)
                                    finalColor = vec4(v_color.rgb * base.rgb * lightIntensity, v_color.a * base.a);
                            
                                    if (u_enableTeamColor) {
                                        vec4 tex1 = texture(u_texture1, v_texCoord0);
                                        // Mix decal color
                                        vec3 mixedColor = mix(finalColor.rgb, v_decalColor.rgb * lightIntensity, tex1.rgb);
                                        finalColor.rgb = mixedColor;
                            
                                        // Write to Mask Buffer (Team Color)
                                        if (base.a > 0.1) {
                                            out_MaskColor = v_decalColor;
                                        }
                                    }
                            
                                    if (finalColor.a <= u_alphaTestValue) discard;
                                }
                            
                                float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                                out_FragColor = vec4(mix(u_fogColor.rgb, finalColor.rgb, fogFactor), finalColor.a);
                            }
                            """;

    public InstancedSpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

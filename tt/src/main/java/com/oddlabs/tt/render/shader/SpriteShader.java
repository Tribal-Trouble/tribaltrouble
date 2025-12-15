package com.oddlabs.tt.render.shader;

/** Shader for rendering animated 3D sprites (units, buildings). */
public final class SpriteShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
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

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String NORMAL = "a_normal";
        String TEX_COORD = "a_texCoord0";
    }

        private static final String PERTURB_NORMAL_FUNC = """
            mat3 cotangent_frame(vec3 N, vec3 p, vec2 uv) {
                // get edge vectors of the pixel triangle
                vec3 dp1 = dFdx(p);
                vec3 dp2 = dFdy(p);
                vec2 duv1 = dFdx(uv);
                vec2 duv2 = dFdy(uv);
            
                // solve the linear system
                vec3 dp2perp = cross(dp2, N);
                vec3 dp1perp = cross(N, dp1);
                vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
                vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
            
                // construct a scale-invariant frame 
                float invmax = inversesqrt(max(dot(T,T), dot(B,B)));
                return mat3(T * invmax, B * invmax, N);
            }
            
            vec3 perturbNormal(vec3 N, vec3 V, vec2 texcoord, vec3 map) {
                // assume N, the interpolated vertex normal and 
                // V, the view vector (vertex to eye)
                map = map * 255./127. - 128./127.;
                mat3 TBN = cotangent_frame(N, -V, texcoord);
                return normalize(TBN * map);
            }
            """;
    
        private static final String VERTEX_SHADER =
            """
            #version 120
            """ +
            """
            attribute vec3 a_position;
            attribute vec3 a_normal;
            attribute vec2 a_texCoord0;
    
            uniform mat4 u_modelViewMatrix;
            uniform mat4 u_projectionMatrix;
            uniform bool u_enableLighting;
            uniform vec4 u_color;
            
            varying vec2 v_texCoord0;
            varying vec4 v_color;
            varying float v_fogDist;
            varying vec3 v_viewPosition;
            varying vec3 v_viewNormal;
    
            void main() {
                vec4 viewPosition = u_modelViewMatrix * vec4(a_position, 1.0);
                gl_Position = u_projectionMatrix * viewPosition;
                v_texCoord0 = a_texCoord0;
                v_color = u_color;
                v_fogDist = length(viewPosition.xyz);
                
                v_viewPosition = viewPosition.xyz;
                v_viewNormal = normalize((u_modelViewMatrix * vec4(a_normal, 0.0)).xyz);
            }
            """;
    
        private static final String FRAGMENT_SHADER =
            """
            #version 120
            """ +
            FOG_FUNCTION +
            PERTURB_NORMAL_FUNC +
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
            uniform vec3 u_lightDirection;
            uniform vec3 u_globalAmbient;
    
            // Fog uniforms
            uniform vec4 u_fogColor;
            uniform int u_fogMode;
            uniform vec3 u_fogParams;
            uniform float u_fogHeightFactor;
            uniform float u_cameraHeight;
    
            varying vec2 v_texCoord0;
            varying vec4 v_color;
            varying float v_fogDist;
            varying vec3 v_viewPosition;
            varying vec3 v_viewNormal;
    
            void main() {
                vec4 base = texture2D(u_texture0, v_texCoord0);
    
                if (u_desaturate > 0.0) {
                    base.rgb = mix(base.rgb, vec3(1.0), u_desaturate);
                }
    
                vec4 finalColor;
                if (u_replaceMode) {
                    finalColor = base;
                    if (finalColor.a <= 0.3) discard;
                } else if (u_modulateColor) {
                    finalColor = v_color * base;
                    if (finalColor.a <= 0.0) discard;
                } else {
                    // Apply lighting
                    vec3 normal = normalize(v_viewNormal);
                    float specularStrength = 0.0;
                    
                    if (u_enableNormalMap) {
                        vec4 normalMapVal = texture2D(u_normalMap, v_texCoord0);
                        normal = perturbNormal(normal, normalize(v_viewPosition), v_texCoord0, normalMapVal.rgb);
                        specularStrength = normalMapVal.a;
                    }
                    
                    vec3 lightIntensity = vec3(1.0);
                    vec3 specular = vec3(0.0);
                    
                    if (u_enableLighting) {
                        vec3 L = normalize(u_lightDirection);
                        float diffuse = max(dot(normal, L), 0.0);
                        lightIntensity = u_globalAmbient + vec3(diffuse);
                        
                                            if (specularStrength > 0.0 && diffuse > 0.0) {
                                                vec3 V = normalize(-v_viewPosition);
                                                vec3 H = normalize(L + V);
                                                float spec = pow(max(dot(normal, H), 0.0), 16.0);
                                                specular = vec3(0.4) * spec * specularStrength;
                                            }                    }
                    
                    finalColor = vec4(v_color.rgb * base.rgb * clamp(lightIntensity, 0.0, 1.0) + specular, v_color.a * base.a);
    
                    if (u_enableTeamColor) {
                        vec4 tex1 = texture2D(u_texture1, v_texCoord0);
                        // Mix decal color but keep specular on top
                        vec3 mixedColor = mix(finalColor.rgb - specular, u_decalColor.rgb * clamp(lightIntensity, 0.0, 1.0), tex1.rgb);
                        finalColor.rgb = mixedColor + specular;
                    }
    
                    if (finalColor.a <= 0.3) discard;
                }
    
                float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
                gl_FragColor = vec4(mix(u_fogColor.rgb, finalColor.rgb, fogFactor), finalColor.a);
            }
            """;
    public SpriteShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER, STANDARD_ATTRIBUTES);
    }
}

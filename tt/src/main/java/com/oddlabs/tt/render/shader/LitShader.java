package com.oddlabs.tt.render.shader;

/**
 * Interface for shaders that support a common, simple lighting model.
 */
public interface LitShader extends Shader {
    interface Uniforms {
        String LIGHT_DIR = "u_lightDirection";
        String GLOBAL_AMBIENT = "u_globalAmbient";
    }

    String PERTURB_NORMAL_FUNC = """
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

    /**
     * Simple vertex-based diffuse lighting (Legacy/FFP emulation).
     */
    String VERTEX_LIGHTING_FUNCTION = """
            vec4 calculateVertexLighting(
                vec3 normal, 
                vec4 materialColor, 
                mat4 modelViewMatrix
            ) {
                // Transform normal to view space
                vec3 transformedNormal = normalize((modelViewMatrix * vec4(normal, 0.0)).xyz);
            
                // Calculate diffuse lighting component
                float diffuse = max(dot(transformedNormal, normalize(u_lightDirection)), 0.0);
            
                // Combine ambient and diffuse
                vec3 light = u_globalAmbient + vec3(diffuse);
            
                // Apply lighting to material color
                return vec4(materialColor.rgb * clamp(light, 0.0, 1.0), materialColor.a);
            }
            """;

    /**
     * Advanced fragment-based lighting with hemispheric ambient and specular.
     */
    String FRAGMENT_LIGHTING_FUNCTION = """
            vec3 calculateLighting(vec3 normal, vec3 viewPos, float specularStrength) {
                vec3 lightDir = normalize(u_lightDirection);
            
                // Diffuse
                float diff = max(dot(normal, lightDir), 0.0);
            
                // Hemispheric Ambient (mix based on normal Y in View Space)
                float skyWeight = 0.5 * (normal.y + 1.0);
                vec3 ambient = mix(u_groundAmbient, u_globalAmbient, skyWeight);
            
                // Specular (Blinn-Phong)
                vec3 viewDir = normalize(-viewPos);
                vec3 halfDir = normalize(lightDir + viewDir);
                float spec = pow(max(dot(normal, halfDir), 0.0), 32.0);
                vec3 specular = specularStrength * spec * vec3(1.0);
            
                return ambient + diff * vec3(1.0) + specular;
            }
            """;
}

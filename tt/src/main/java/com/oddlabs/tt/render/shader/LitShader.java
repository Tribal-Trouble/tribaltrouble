package com.oddlabs.tt.render.shader;

/**
 * Interface for shaders that support a common, simple lighting model.
 */
public interface LitShader extends Shader {
    String LIGHTING_CONSTANTS = """
            const vec3 u_lightDirection = vec3(-0.70710678, 0.0, 0.70710678);
            const vec3 u_globalAmbient = vec3(0.132866, 0.132866, 0.170656); // Linearized (0.4, 0.4, 0.45)
            const vec3 u_groundAmbient = vec3(0.019472, 0.012726, 0.008518); // Linearized (0.15, 0.12, 0.1)
            """;

    interface Uniforms {
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
                vec3 worldNormal = normalize((transpose(u_viewMatrix) * vec4(transformedNormal, 0.0)).xyz);
                vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection, 0.0)).xyz);

                // Wrap Lighting (Half-Lambert)
                float diff = dot(transformedNormal, lightDir) * 0.5 + 0.5;
                diff = diff * diff;

                // Hemispheric Ambient (mix based on normal Z in World Space)
                float skyWeight = clamp(worldNormal.z * 0.5 + 0.5, 0.0, 1.0);
                vec3 ambient = mix(u_groundAmbient, u_globalAmbient, skyWeight);

                // Rim Lighting
                vec3 viewDir = normalize(-(modelViewMatrix * vec4(normal, 1.0)).xyz); // Approximate
                float rim = 1.0 - max(dot(viewDir, transformedNormal), 0.0);
                rim = smoothstep(0.8, 1.0, rim);
                vec3 rimLight = rim * u_globalAmbient * 0.25;

                // Combine and apply exposure
                float exposure = 1.1;
                vec3 light = (ambient + vec3(diff) + rimLight) * exposure;

                // Apply lighting to material color
                return vec4(materialColor.rgb * clamp(light, 0.0, 1.0), materialColor.a);
            }
            """;

    /**
     * Advanced fragment-based lighting with specular support.
     */
    String FRAGMENT_LIGHT_DIR = "vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection, 0.0)).xyz);";

    String FRAGMENT_LIGHTING_FUNCTION = """
            vec3 calculateLighting(vec3 normal, vec3 worldNormal, vec3 viewPos, float specularStrength) {
                """ + FRAGMENT_LIGHT_DIR + """

                // Wrap Lighting (Half-Lambert)
                float diff = dot(normal, lightDir) * 0.5 + 0.5;
                diff = diff * diff;

                // Hemispheric Ambient (mix based on normal Z in World Space)
                float skyWeight = clamp(worldNormal.z * 0.5 + 0.5, 0.0, 1.0);
                vec3 ambient = mix(u_groundAmbient, u_globalAmbient, skyWeight);

                // Specular (Blinn-Phong)
                vec3 viewDir = normalize(-viewPos);
                vec3 halfDir = normalize(lightDir + viewDir);
                float spec = pow(max(dot(normal, halfDir), 0.0), 32.0);
                vec3 specular = specularStrength * spec * vec3(1.0);

                // Rim Lighting
                // Adds a subtle glow to edges to detach objects from the background.
                float rim = 1.0 - max(dot(viewDir, normal), 0.0);
                rim = smoothstep(0.8, 1.0, rim);
                vec3 rimLight = rim * u_globalAmbient * 0.25;

                // Overall light intensity scaler to prevent scenes from being too dark
                float exposure = 1.1;

                return (ambient + diff * vec3(1.0) + specular + rimLight) * exposure;
            }
            """;
}

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
     * Accumulated contribution of nearby point lights. View-space inputs; uses
     * half-Lambert wrap so billboard sprites with awkward normals still catch light.
     */
    String POINT_LIGHTS_FUNCTION = """
            vec3 calculatePointLights(vec3 viewPos, vec3 normal) {
                vec3 result = vec3(0.0);
                int count = int(u_numPointLights);
                for (int i = 0; i < count; i++) {
                    vec3 toLight = u_pointLightPos[i].xyz - viewPos;
                    float dist = length(toLight);
                    float atten = clamp(1.0 - dist / u_pointLightPos[i].w, 0.0, 1.0);
                    atten *= atten;
                    float wrap = clamp((dot(normal, toLight / max(dist, 0.0001)) + 0.6) / 1.6, 0.0, 1.0);
                    result += u_pointLightColor[i].rgb * (atten * wrap);
                }
                return result;
            }
            """;

    /**
     * A function for looking up dynamic shadow values from the shadow texture.
     */
    String SHADOW_FUNCTION = """
            const float SHADOW_INTENSITY = 0.72;
            const float SHADOW_BIAS_METERS = 0.1;
            const float SHADOW_SLOPE_BIAS_TEXELS = 1.5;
            const float SHADOW_MAX_BIAS_METERS = 3.0;
            const float SHADOW_BLUR_METERS = 0.2;
            const int SHADOW_TAPS = 12;
            const vec2 SHADOW_POISSON_DISK[SHADOW_TAPS] = vec2[](
                    vec2(-0.326, -0.406), vec2(-0.840, -0.074), vec2(-0.696, 0.457), vec2(-0.203, 0.621),
                    vec2(0.962, -0.195), vec2(0.473, -0.480), vec2(0.519, 0.767), vec2(0.185, -0.893),
                    vec2(0.507, 0.064), vec2(0.896, 0.412), vec2(-0.322, -0.933), vec2(-0.792, -0.598));

            bool dynamicShadows() {
                return u_shadowMode == 1;
            }

            float calculateShadow(vec2 shadowUV, float sunDistance, vec3 normal) {
                if (!dynamicShadows()) {
                    return 0.0;
                }

                if (shadowUV.x < 0.0 || shadowUV.y < 0.0 || shadowUV.x > 1.0 || shadowUV.y > 1.0 || sunDistance > 1.0) {
                    return 0.0;
                }

                vec2 uvPerMeter = vec2(length(vec3(u_shadowMatrix[0][0], u_shadowMatrix[1][0], u_shadowMatrix[2][0])),
                        length(vec3(u_shadowMatrix[0][1], u_shadowMatrix[1][1], u_shadowMatrix[2][1])));
                float distancePerMeter = length(vec3(u_shadowMatrix[0][2], u_shadowMatrix[1][2], u_shadowMatrix[2][2]));
                vec2 texelSize = 1.0 / (uvPerMeter * vec2(textureSize(u_shadowMap, 0)));
                float texelMeters = max(texelSize.x, texelSize.y);
                float nDotL = max(abs(dot(normal, normalize(u_lightDirection))), 0.05);
                float slope = sqrt(1.0 - nDotL * nDotL) / nDotL;
                float bias = min(SHADOW_BIAS_METERS + (SHADOW_SLOPE_BIAS_TEXELS * texelMeters + SHADOW_BLUR_METERS) * slope, SHADOW_MAX_BIAS_METERS) * distancePerMeter;
                float angle = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) * 6.2831853;
                mat2 rotation = mat2(cos(angle), sin(angle), -sin(angle), cos(angle));
                vec2 radius = SHADOW_BLUR_METERS * uvPerMeter;
                float lit = 0.0;
                for (int i = 0; i < SHADOW_TAPS; i++) {
                    vec2 uv = shadowUV + rotation * SHADOW_POISSON_DISK[i] * radius;
                    lit += texture(u_shadowMap, vec3(uv, sunDistance - bias));
                }
                return 1.0 - lit / float(SHADOW_TAPS);
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
                vec3 light = u_globalAmbient + u_sunColor * diffuse;

                // Apply lighting to material color
                return vec4(materialColor.rgb * clamp(light, 0.0, 1.0), materialColor.a);
            }
            """;

    /**
     * Advanced fragment-based lighting with hemispheric ambient and specular.
     */
    String FRAGMENT_LIGHTING_FUNCTION = """
            vec3 calculateLighting(vec3 normal, vec3 viewPos, float specularStrength, float sunVisibility) {
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
                vec3 specular = specularStrength * spec * u_sunColor;

                return ambient + (diff * u_sunColor + specular) * sunVisibility;
            }

            vec3 calculateLighting(vec3 normal, vec3 viewPos, float specularStrength) {
                return calculateLighting(normal, viewPos, specularStrength, 1.0);
            }
            """;
}

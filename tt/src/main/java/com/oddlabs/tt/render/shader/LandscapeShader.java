package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the dynamic 3D landscape with terrain texturing, normal mapping, and lighting.
 */
public final class LandscapeShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String HEIGHT_MAP = "u_HeightMap";
        String DIFFUSE_MAP = "u_DiffuseMap";
        String NORMAL_MAP = "u_NormalMap";
        String DETAIL_MAP = "u_DetailMap";
        String DETAIL_NORMAL_MAP = "u_DetailNormalMap";
        String WORLD_SIZE = "u_WorldSize";
        String DETAIL_SCALE = "u_DetailScale";
        String SEA_BOTTOM_COLOR = "u_SeaBottomColor";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_PATCH_OFFSET = "in_InstancePatchOffset";
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec2 in_Position;
                    layout(location = 4) in vec3 in_InstancePatchOffset; // xy = offset, z = wave scale

                    uniform float u_WorldSize;
                    uniform float u_DetailScale;
                    uniform sampler2D u_HeightMap;

                    out VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoordColormap;
                        vec2 texCoord1;
                        float fogDist;
                        vec3 viewPosition;
                        float height;
                        float waveScale;
                    } vs_out;

                    void main() {
                        vec2 worldPos = in_InstancePatchOffset.xy + in_Position;
                        // Add half-texel offset to align vertex-centered heightmap (1 grid unit = 2 meters)
                        vec2 uv = (worldPos + 1.0) / u_WorldSize;
                        float h = texture(u_HeightMap, uv).r;

                        vec4 worldPosition4 = vec4(worldPos.x, worldPos.y, h, 1.0);
                        vec4 viewPosition = u_viewMatrix * worldPosition4;
                        gl_Position = u_projectionMatrix * viewPosition;

                        vs_out.texCoord0 = uv;
                        vs_out.texCoordColormap = worldPos / u_WorldSize;
                        vs_out.texCoord1 = worldPos * u_DetailScale;
                        vs_out.fogDist = length(viewPosition.xyz);
                        vs_out.viewPosition = viewPosition.xyz;
                        vs_out.height = h;
                        vs_out.waveScale = in_InstancePatchOffset.z;
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            LIGHTING_CONSTANTS +
            FOG_FUNCTION +
            """
                    uniform sampler2D u_DiffuseMap;
                    uniform sampler2D u_NormalMap;
                    uniform sampler2D u_DetailMap;
                    uniform sampler2D u_DetailNormalMap;
                    uniform sampler2D u_HeightMap;
                    uniform vec3 u_SeaBottomColor;
                    uniform float u_WorldSize;
                    uniform float u_DetailScale;

                    in VS_OUT {
                        vec2 texCoord0;
                        vec2 texCoordColormap;
                        vec2 texCoord1;
                        float fogDist;
                        vec3 viewPosition;
                        float height;
                        float waveScale;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;

                    const float PI = 3.14159265358979;
                    const float GRAVITY = 9.81;

                    float getWaveHeight(vec2 worldPos) {
                        if (u_waveAmpSteep[0].x < 0.0001) {
                            return 0.0;
                        }
                        float waveZ = 0.0;
                        for (int i = 0; i < 3; i++) {
                            float waveLength = u_waveDirLength[i].z;
                            vec2 waveDir = u_waveDirLength[i].xy;
                            float waveAmplitude = u_waveAmpSteep[i].x;

                            float k = 2.0 * PI / waveLength;
                            float omega = sqrt(GRAVITY * k);
                            float phase = k * dot(waveDir, worldPos) - omega * u_waveTime;
                            waveZ += waveAmplitude * sin(phase);
                        }
                        return waveZ;
                    }

                    void main() {
                        vec3 viewDir = normalize(-fs_in.viewPosition);
                        vec3 lightDir = normalize((u_viewMatrix * vec4(u_lightDirection, 0.0)).xyz);

                        vec4 diffuseColor = texture(u_DiffuseMap, fs_in.texCoordColormap);
                        vec4 normalMapVal = texture(u_NormalMap, fs_in.texCoordColormap);
                        vec4 detailColor;
                        vec4 detailNormalColor;

                        // Surface roughness metadata baked into diffuse alpha (1.0 = rough, 0.0 = smooth)
                        float roughness = diffuseColor.a;

                        // Reconstruct world position and calculate dynamic wetness factor
                        vec2 worldPos = fs_in.texCoordColormap * u_WorldSize;
                        float waveHeight = getWaveHeight(worldPos) * fs_in.waveScale;
                        float u_seaLevel = u_fogParams.w;

                        // Add a slow tide oscillation to the water height for the wash effect
                        float tide = sin(u_waveTime * 0.25) * 0.15;
                        float waterHeight = u_seaLevel + waveHeight + tide;
                        float depth = waterHeight - fs_in.height;
                        float wetness = clamp((depth + 0.10) / 0.30, 0.0, 1.0);

                        // Calculate static depth (below sea level) for caustics and light attenuation
                        float depthStatic = u_seaLevel - fs_in.height;
                        float normalMapStrength;
                        if (depthStatic < 0.0) {
                            float t = clamp((depthStatic + 0.25) / 0.25, 0.0, 1.0);
                            normalMapStrength = mix(1.0, 0.50, t);
                        } else {
                            float t = clamp(depthStatic / 1.0, 0.0, 1.0);
                            normalMapStrength = mix(0.50, 0.25, t);
                        }

                        // Calculate edge blend factor (ranges from 0.0 at the edge to 1.0 at 2% inside the world)
                        float distToEdgeX = min(fs_in.texCoordColormap.x, 1.0 - fs_in.texCoordColormap.x);
                        float distToEdgeY = min(fs_in.texCoordColormap.y, 1.0 - fs_in.texCoordColormap.y);
                        float distToEdge = min(distToEdgeX, distToEdgeY);
                        float edgeBlend = smoothstep(0.0, 0.02, distToEdge);

                        // Blend diffuse color to linear sea bottom color, and normal map alpha (specular intensity) to 0.0
                        diffuseColor.rgb = mix(u_SeaBottomColor, diffuseColor.rgb, edgeBlend);
                        normalMapVal.a = mix(0.0, normalMapVal.a, edgeBlend);

                        // Compute view-space normal from heightmap slope
                        float h_plus_x = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(1, 0)).r;
                        float h_minus_x = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(-1, 0)).r;
                        float h_plus_y = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(0, 1)).r;
                        float h_minus_y = textureOffset(u_HeightMap, fs_in.texCoord0, ivec2(0, -1)).r;

                        // Calculate mathematically accurate normal for triplanar mapping and specular
                        vec3 worldNormalGeom = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 4.0));
                        vec3 worldNormal = normalize(vec3(h_minus_x - h_plus_x, h_minus_y - h_plus_y, 64.0));

                        // Smoothly blend worldNormal to flat (0,0,1) near the world edges to match the seabottom normal
                        worldNormalGeom = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormalGeom, edgeBlend));
                        worldNormal = normalize(mix(vec3(0.0, 0.0, 1.0), worldNormal, edgeBlend));

                        // Decode world-space normal from the baked colormap
                        vec3 bakedWorldNormal = normalMapVal.rgb * (255.0/127.0) - (128.0/127.0);
                        vec3 baseNormal = normalize((u_viewMatrix * vec4(bakedWorldNormal, 0.0)).xyz);

                        // Sample detail map and detail normal map using triplanar mapping for steep slopes (cliffs)
                        // This handles high-frequency noise tiling which is too dense to bake into the colormap.
                        vec3 blendWeights = pow(abs(worldNormalGeom), vec3(8.0));
                        blendWeights /= (blendWeights.x + blendWeights.y + blendWeights.z);

                        if (worldNormalGeom.z > 0.98) {
                            detailColor = texture(u_DetailMap, fs_in.texCoord1);
                            detailNormalColor.rgb = texture(u_DetailNormalMap, fs_in.texCoord1).rgb * 2.0 - 1.0;

                            // Dual-scale blending to break up tiling patterns
                            vec4 detailColor2 = texture(u_DetailMap, fs_in.texCoord1 * 0.237);
                            vec3 detailNormalColor2 = texture(u_DetailNormalMap, fs_in.texCoord1 * 0.237).rgb * 2.0 - 1.0;
                            float blend = smoothstep(0.3, 0.7, detailColor.a);
                            detailColor = mix(detailColor, detailColor2, blend * 0.5);
                            detailNormalColor.rgb = normalize(mix(detailNormalColor.rgb, detailNormalColor2, blend * 0.5));
                        } else {
                            // Uniform triplanar coordinates (tile every 16 meters)
                            vec3 coord = vec3(worldPos, fs_in.height) / 16.0;

                            // Calculate continuous derivatives from top-down UVs to prevent seams at projection boundaries
                            vec2 ddx = dFdx(fs_in.texCoord1);
                            vec2 ddy = dFdy(fs_in.texCoord1);

                            detailColor = textureGrad(u_DetailMap, fs_in.texCoord1, ddx, ddy) * blendWeights.z +
                                          textureGrad(u_DetailMap, coord.yz, ddx, ddy) * blendWeights.x +
                                          textureGrad(u_DetailMap, coord.xz, ddx, ddy) * blendWeights.y;

                            vec3 nXY = textureGrad(u_DetailNormalMap, fs_in.texCoord1, ddx, ddy).rgb * 2.0 - 1.0;
                            vec3 nYZ = textureGrad(u_DetailNormalMap, coord.yz, ddx, ddy).rgb * 2.0 - 1.0;
                            vec3 nXZ = textureGrad(u_DetailNormalMap, coord.xz, ddx, ddy).rgb * 2.0 - 1.0;

                            // Rotate side normals to world space based on projection plane and face direction
                            detailNormalColor.rgb = normalize(nXY * blendWeights.z +
                                                              vec3(nYZ.z * sign(worldNormalGeom.x), nYZ.x, nYZ.y) * blendWeights.x +
                                                              vec3(nXZ.x, nXZ.z * sign(worldNormalGeom.y), nXZ.y) * blendWeights.y);

                            // Dual-scale blending for triplanar (larger scale at ~67 meters)
                            vec3 coord2 = coord * 0.237;
                            vec4 detailColorLow = textureGrad(u_DetailMap, fs_in.texCoord1 * 0.237, ddx * 0.237, ddy * 0.237) * blendWeights.z +
                                                 textureGrad(u_DetailMap, coord2.yz, ddx * 0.237, ddy * 0.237) * blendWeights.x +
                                                 textureGrad(u_DetailMap, coord2.xz, ddx * 0.237, ddy * 0.237) * blendWeights.y;
                            float blend = smoothstep(0.3, 0.7, detailColor.a);
                            detailColor = mix(detailColor, detailColorLow, blend * 0.5);
                            // Normal map blending is complex; for simplicity and performance, we mainly blend the diffuse modulation
                        }

                        // Subtly modulate diffuse color with detail noise (masked by surface roughness)
                        float slope = 1.0 - worldNormalGeom.z;
                        vec3 srgbDiffuse = pow(diffuseColor.rgb, vec3(1.0 / 2.2));
                        float detailFade = clamp(detailColor.a / 0.15, 0.0, 1.0);

                        // Distance-based detail fade to prevent shimmering
                        float distFade = 1.0 - clamp((fs_in.fogDist - 100.0) / 400.0, 0.0, 1.0);

                        float detailStrength = mix(0.15, 0.35, slope) * normalMapStrength * detailFade * roughness;
                        float detailOffset = 1.0 - detailStrength * 0.5;
                        srgbDiffuse *= (detailColor.rgb * detailStrength + detailOffset);
                        diffuseColor.rgb = pow(srgbDiffuse, vec3(2.2));

                        // Apply dynamic wetness darkening (wet surfaces scatter less light)
                        diffuseColor.rgb = mix(diffuseColor.rgb, diffuseColor.rgb * 0.55, wetness);

                        // --- Dynamic Shoreline "Wet Line" (Wash/Foam) ---
                        // Brighten the leading edge of the water to simulate foam and bubbles
                        float wash = smoothstep(0.0, 0.08, depth) * (1.0 - smoothstep(0.12, 0.25, depth));
                        diffuseColor.rgb = mix(diffuseColor.rgb, diffuseColor.rgb + vec3(0.15, 0.2, 0.25), wash * 0.6 * fs_in.waveScale);

                        vec3 viewNormalGeom = normalize((u_viewMatrix * vec4(worldNormalGeom, 0.0)).xyz);
                        vec3 viewNormal = normalize((u_viewMatrix * vec4(worldNormal, 0.0)).xyz);

                        // Reduce the normal map perturbation strength under water to smooth the underwater terrain
                        baseNormal = normalize(mix(viewNormalGeom, baseNormal, normalMapStrength));

                        // Blend baseNormal back to the flat viewNormal near the boundary to eliminate TBN/derivative mismatches
                        baseNormal = normalize(mix(viewNormal, baseNormal, edgeBlend));

                        // Micro-detail normal perturbation from detail map color (adds tactile depth up close)
                        // Extract deviation from "flat" geometry normal and transform to view space
                        vec3 detailDeviation = detailNormalColor.rgb - worldNormalGeom;
                        vec3 vDetailNormal = (u_viewMatrix * vec4(detailDeviation, 0.0)).xyz;
                        float detailNormalStrength = mix(0.20, 0.02, wetness) * normalMapStrength * detailFade * roughness * distFade;
                        vec3 normal = normalize(baseNormal + vDetailNormal * detailNormalStrength);

                        // Dynamic specular (Blinn-Phong) & rim lighting
                        vec3 halfDir = normalize(lightDir + viewDir);

                        // For wet surfaces, blend to a sharper and more intense water-film specular highlight
                        float drySpecIntensity = normalMapVal.a * 0.05;
                        float wetSpecIntensity = 0.05;

                        // Specular exponent sharpened by surface smoothness (low roughness)
                        float specExponent = mix(128.0, 32.0, roughness);
                        specExponent = mix(specExponent, 80.0, wetness);

                        float specIntensity = mix(drySpecIntensity, wetSpecIntensity, wetness);

                        // Fresnel reflection (Schlick approximation)
                        // Smooth surfaces (snow/ice) gain intensity at grazing angles.
                        float fresnelBase = mix(0.04, 0.20, wetness); // Base reflectivity
                        float fresnel = fresnelBase + (1.0 - fresnelBase) * pow(clamp(1.0 - dot(normal, viewDir), 0.0, 1.0), 5.0);
                        specIntensity = mix(specIntensity, specIntensity * 2.0, fresnel * (1.0 - roughness));

                        float spec = pow(max(dot(normal, halfDir), 0.0), specExponent);
                        vec3 specular = specIntensity * spec * vec3(1.0);

                        float rim = 1.0 - max(dot(viewDir, normal), 0.0);
                        rim = smoothstep(0.8, 1.0, rim);
                        vec3 rimLight = rim * u_globalAmbient * 0.05;

                        // Hemispheric Ambient (mix based on normal Z in World Space)
                        float skyWeight = clamp(worldNormal.z * 0.5 + 0.5, 0.0, 1.0);
                        vec3 ambient = mix(u_groundAmbient, u_globalAmbient, skyWeight);

                        // Wrap Lighting (Half-Lambert)
                        float diff = dot(normal, lightDir) * 0.5 + 0.5;
                        diff = diff * diff;

                        float exposure = 1.1;
                        vec3 lightFactor = (ambient + diff * vec3(1.0) + rimLight) * exposure;
                        vec3 litColor = diffuseColor.rgb * lightFactor + specular * exposure;

                        // --- Underwater Caustics ---
                        if (depth > 0.0) {
                            // Project caustics by sampling overlapping waves at different frequencies
                            float causticsTime = u_waveTime * 0.15;
                            vec2 uv1 = worldPos * 0.4 + vec2(causticsTime * 0.1, causticsTime * 0.07);
                            vec2 uv2 = worldPos * 0.3 - vec2(causticsTime * 0.08, causticsTime * 0.13);

                            float h1 = getWaveHeight(uv1);
                            float h2 = getWaveHeight(uv2);

                            float c = 1.0 - abs(h1 - h2);
                            // Robust intensity (0.4) and sharpening (20) to ensure visibility on all terrains
                            float caustic = pow(max(0.0, c), 20.0) * 0.4;

                            // Viewing angle dependency: caustics fade out when looking straight down (realistic refraction)
                            float vdn = dot(viewDir, normal);
                            float angleFade = smoothstep(0.1, 0.5, 1.0 - vdn);

                            // Attenuate caustics with dynamic depth and surface roughness
                            float depthFade = smoothstep(0.0, 0.1, depth) * clamp(1.0 - depthStatic / 4.0, 0.0, 1.0);
                            
                            // Near-white cyan highlights
                            vec3 causticColor = vec3(0.95, 0.98, 1.0) * caustic * depthFade * angleFade * (1.0 - roughness * 0.5);
                            litColor += causticColor;
                        }

                        float fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
                        vec3 finalColor = mix(u_fogColor.rgb, litColor, fogFactor);
                        out_FragColor = vec4(finalColor, diffuseColor.a);
                    }
                    """;

    public LandscapeShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

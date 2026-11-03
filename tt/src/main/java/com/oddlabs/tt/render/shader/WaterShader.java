package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering water surfaces.
 */
public final class WaterShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String TEXTURE_0 = "u_texture0"; // Base water texture
        String TEXTURE_1 = "u_texture1"; // Detail water texture
        String REFLECTION_TEXTURE = "u_reflectionTexture";
        String REFLECTION_VP = "u_reflectionVP";
        String HAS_REFLECTION = "u_hasReflection";
        String WATER_REPEAT_RATE = "u_waterRepeatRate";
        String WATER_DETAIL_REPEAT_RATE = "u_waterDetailRepeatRate";
        String ENABLE_DETAIL = "u_enableDetail";
        String SCROLL_OFFSET_0 = "u_scrollOffset0";
        String SCROLL_OFFSET_1 = "u_scrollOffset1";
        String LIGHT_DIR = LitShader.Uniforms.LIGHT_DIR;
        String CAMERA_POS = "u_cameraPos";
        String WATER_HEIGHT = "u_waterHeight";

        String WAVE_DIR_LENGTH = "u_waveDirLength";
        String WAVE_AMP_STEEP = "u_waveAmpSteep";
        String WAVE_TIME = "u_waveTime";
        String ANGER = "u_anger";

        String HEIGHT_MAP = "u_HeightMap";
        String WORLD_SIZE = "u_WorldSize";
        String DEPTH_SCALE = "u_depthScale";
        String MIN_ALPHA = "u_minAlpha";
        String MAX_ALPHA = "u_maxAlpha";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = FogShader.FOG_HEIGHT_FACTOR;
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String INSTANCE_OFFSET = "in_InstanceOffset";
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec3 in_Position;
            layout(location = 4) in vec3 in_InstanceOffset;

            uniform mat4 u_modelViewMatrix;
            uniform mat4 u_reflectionVP;
            uniform float u_waterRepeatRate;
            uniform float u_waterDetailRepeatRate;
            uniform vec2 u_scrollOffset0;
            uniform vec2 u_scrollOffset1;
            uniform float u_waterHeight;
            uniform float u_WorldSize;
            uniform vec4 u_waveDirLength[3];
            uniform vec4 u_waveAmpSteep[3];
            uniform float u_waveTime;

            out vec2 v_texCoord0;
            out vec2 v_texCoord1;
            out vec2 v_texCoordHeightmap;
            out float v_fogDist;
            out vec3 v_worldPos;
            out vec4 v_reflectionClipPos;

            const float PI = 3.14159265358979;
            const float GRAVITY = 9.81;

            void addGerstnerWave(int i, vec2 baseXY, float waveScale, inout vec3 disp, inout vec3 normal) {
                float waveLength = u_waveDirLength[i].z;
                vec2 waveDir = u_waveDirLength[i].xy;
                float waveAmplitude = u_waveAmpSteep[i].x;
                float waveSteepness = u_waveAmpSteep[i].y;

                float k = 2.0 * PI / waveLength;
                float omega = sqrt(GRAVITY * k);
                float phase = k * dot(waveDir, baseXY) - omega * u_waveTime;
                float s = sin(phase);
                float c = cos(phase);
                float A = waveAmplitude * waveScale;

                disp.x += waveSteepness * A * waveDir.x * c;
                disp.y += waveSteepness * A * waveDir.y * c;
                disp.z += A * s;

                float WA = k * A;
                normal.x -= WA * waveDir.x * c;
                normal.y -= WA * waveDir.y * c;
                normal.z -= waveSteepness * WA * s;
            }

            void main() {
                vec2 baseXY = in_InstanceOffset.xy + in_Position.xy;
                float baseZ = u_waterHeight + in_Position.z;

                vec3 disp = vec3(0.0);
                vec3 normal = vec3(0.0, 0.0, 1.0);

                // If amplitude is 0, waves are effectively disabled for that channel
                if (u_waveAmpSteep[0].x > 0.0001 && in_InstanceOffset.z > 0.0) {
                    float distToEdgeX = min(baseXY.x, u_WorldSize - baseXY.x);
                    float distToEdgeY = min(baseXY.y, u_WorldSize - baseXY.y);
                    float distToEdge = min(distToEdgeX, distToEdgeY);

                    float waveScale = clamp(distToEdge / 16.0, 0.0, 1.0);
                    waveScale *= in_InstanceOffset.z;

                    addGerstnerWave(0, baseXY, waveScale, disp, normal);
                    addGerstnerWave(1, baseXY, waveScale, disp, normal);
                    addGerstnerWave(2, baseXY, waveScale, disp, normal);
                }

                vec3 worldPos = vec3(baseXY + disp.xy, baseZ + disp.z);
                v_worldPos = worldPos;

                vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, 1.0);
                gl_Position = u_projectionMatrix * viewPosition;

                // Scale up the UVs significantly to reduce "blob" size.
                float scaleFix = 4.0;

                v_texCoord0 = (worldPos.xy * u_waterRepeatRate * scaleFix) + u_scrollOffset0;
                v_texCoord1 = (worldPos.xy * u_waterRepeatRate * scaleFix * 1.3) + u_scrollOffset1;
                v_texCoordHeightmap = (baseXY + 1.0) / u_WorldSize;

                v_fogDist = length(viewPosition.xyz);
                v_reflectionClipPos = u_reflectionVP * vec4(worldPos, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + FOG_FUNCTION + """
            uniform sampler2D u_texture0;
            uniform sampler2D u_texture1;
            uniform sampler2D u_reflectionTexture;
            uniform sampler2D u_HeightMap;
            uniform bool u_enableDetail;
            uniform bool u_hasReflection;
            uniform vec3 u_cameraPos;
            uniform float u_WorldSize;
            uniform float u_depthScale;
            uniform float u_minAlpha;
            uniform float u_maxAlpha;
            uniform float u_waveTime;
            uniform float u_anger;

            in vec2 v_texCoord0;
            in vec2 v_texCoord1;
            in vec2 v_texCoordHeightmap;
            in float v_fogDist;
            in vec3 v_worldPos;
            in vec4 v_reflectionClipPos;

            layout(location = 0) out vec4 out_FragColor;

            float signedNoise(vec2 uv) {
                vec2 gb = texture(u_texture0, uv).gb;
                float ratio = (gb.x - gb.y) / max(gb.x + gb.y, 0.001);
                return clamp((ratio + 0.07) * 6.0, -1.0, 1.0);
            }

            vec2 calcRippleNormal() {
                float time = u_waveTime * (0.5 + u_anger) * 0.5;
                float scale = 0.04 / (u_anger + 0.5);
                float offset1 = signedNoise((v_worldPos.xy * scale + vec2(0.6, 0.35) * time) / 3000);
                float offset2 = signedNoise((v_worldPos.xy * scale - vec2(0.35, 0.6) * time) / 3600);
                float height = 0.18 + offset1 * 0.04;
                float angle = offset2 * 0.35;
                float drag = 0.1 + offset1 * 0.32;
                vec2 pos = v_worldPos.xy * scale + vec2(offset1, offset2) * 0.1;

                float footprint = length(fwidth(pos));
                float wavelen = 10.0 / (0.5 + u_anger);
                const float DETAIL = 75.0;
                float k = DETAIL / wavelen;
                float weight = 1.0;
                vec2 slope = vec2(0.0);
                const int NUM_SAMPLES = 10;
                for (int i = 0; i < NUM_SAMPLES; i++) {
                    vec2 dir = vec2(cos(angle), sin(angle));
                    float phase = k * dot(dir, pos) - sqrt(9.8 * k) * time;
                    float wave = exp(sin(phase) - 1.0);
                    float dWave = wave * cos(phase);
                    float lod = 1.0 - smoothstep(0.2, 0.45, k * footprint / DETAIL);
                    slope -= dir * (height * weight * k * dWave * lod);
                    pos += dir * (dWave * weight * drag / k);
                    angle += 2.39996323;
                    k *= 1.18;
                    weight *= 0.75;
                }
                return slope;
            }

            void main() {
                vec4 baseColor = texture(u_texture0, v_texCoord0);

                // Depth-based transparency: sample heightmap at closest point to determine water depth continuously
                vec2 closestPoint = clamp(v_texCoordHeightmap, 0.0, 1.0);
                float terrainHeight = texture(u_HeightMap, closestPoint).r;
                float distInMeters = distance(v_texCoordHeightmap, closestPoint) * u_WorldSize;
                float depth = v_worldPos.z - terrainHeight + distInMeters;
                float depthFade = smoothstep(0.0, 1.0, sqrt(clamp(depth / u_depthScale, 0.0, 1.0)));
                float finalAlpha = mix(u_minAlpha, u_maxAlpha, depthFade);

                vec2 rippleNormal = calcRippleNormal();
                vec3 normal = normalize(vec3(rippleNormal, 1.0));

                vec3 lightDir = normalize(u_lightDirection);
                vec3 viewDir = normalize(u_cameraPos - v_worldPos);
                vec3 halfDir = normalize(lightDir + viewDir);

                float specAngle = max(dot(normal, halfDir), 0.0);
                float specular = pow(specAngle, 40.0);

                float F0 = 0.02;
                float F = F0 + (1.0 - F0) * pow(1.0 - max(dot(normal, viewDir), 0.0), 5.0);

                vec3 reflectionColor = u_fogColor.rgb;
                if (u_hasReflection && v_reflectionClipPos.w > 0.0) {
                    vec2 reflUV = v_reflectionClipPos.xy / v_reflectionClipPos.w * 0.5 + 0.5;
                    reflUV += (mat3(u_viewMatrix) * vec3(rippleNormal, 0.0)).xy * 0.2;
                    vec2 edgeDist = min(reflUV, 1.0 - reflUV);
                    float edgeFade = clamp(min(edgeDist.x, edgeDist.y) / 0.1, 0.0, 1.0);
                    if (edgeFade > 0.0) {
                        vec3 sampled = texture(u_reflectionTexture, clamp(reflUV, 0.0, 1.0)).rgb;
                        reflectionColor = mix(reflectionColor, sampled, edgeFade);
                    }
                }

                vec3 waterColor = baseColor.rgb * 0.7 * u_sceneTint;

                vec3 finalRGB = mix(waterColor, reflectionColor, F * 0.9);
                finalRGB += vec3(specular) * 0.4;

                float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                out_FragColor = vec4(mix(u_fogColor.rgb, finalRGB, fogFactor), finalAlpha);
            }
            """;

    public WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

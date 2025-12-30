package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class SonicBlastShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String NOISE_TEXTURE = "u_noiseTexture";
        String TIME = "u_time";
        String MAX_RADIUS = "u_maxRadius";
        String COLOR = "u_color";
    }

    public interface Attributes {
        String POSITION = "in_Position";
        String TEX_COORD = "in_TexCoord";
    }

    public enum Attribute implements VertexAttribute {
        POSITION(Attributes.POSITION, 3, GL11.GL_FLOAT),
        TEX_COORD(Attributes.TEX_COORD, 2, GL11.GL_FLOAT);

        private final @NonNull String name;
        private final int componentCount;
        private final int glType;
        private final boolean normalized;

        Attribute(@NonNull String name, int componentCount, int glType) {
            this(name, componentCount, glType, false);
        }

        Attribute(@NonNull String name, int componentCount, int glType, boolean normalized) {
            this.name = name;
            this.componentCount = componentCount;
            this.glType = glType;
            this.normalized = normalized;
        }

        @Override public @NonNull String getName() { return name; }
        @Override public int getComponentCount() { return componentCount; }
        @Override public int getGlType() { return glType; }
        @Override public boolean isNormalized() { return normalized; }
    }

    private static final String VERTEX_SHADER = """
        #version 410 core

        layout(location = 0) in vec3 in_Position;
        layout(location = 1) in vec2 in_TexCoord;

        uniform mat4 u_projectionMatrix;
        uniform mat4 u_modelViewMatrix;

        out vec2 v_texCoord;
        out float v_fogDist;

        void main() {
            vec4 viewPos = u_modelViewMatrix * vec4(in_Position, 1.0);
            gl_Position = u_projectionMatrix * viewPos;
            v_texCoord = in_TexCoord;
            v_fogDist = length(viewPos.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 410 core
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_noiseTexture;
        uniform float u_time;
        uniform float u_maxRadius;
        uniform vec3 u_color;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams;
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;

        in vec2 v_texCoord;
        in float v_fogDist;
        
        layout(location = 0) out vec4 out_FragColor;

        const int NUM_RINGS = 4; // 1 initial + 3 looped
        const float SECONDS_AFTER_FIRST = 0.005;
        const float TIME_BETWEEN_RINGS = 0.01;
        const float RING_SPEED = 27.0;
        const float RING_WIDTH = 6.0; // Increased width for even softer look

        void main() {
            // Calculate distance from center in UV space (0.0 to 0.5) and scale to world space
            float dist_uv = distance(v_texCoord, vec2(0.5));
            float dist = dist_uv * u_maxRadius * 2.0;

            float totalIntensity = 0.0;

            // Main blast ring
            if (u_time > 0.0) {
                float ringTime = u_time;
                float currentRadius = ringTime * RING_SPEED;
                if (currentRadius <= u_maxRadius) {
                    float distToRing = abs(dist - currentRadius);
                    float ringIntensity = 1.0 - smoothstep(0.0, RING_WIDTH, distToRing);
                    float fade = 1.0 - smoothstep(0.0, u_maxRadius, currentRadius);
                    totalIntensity += ringIntensity * fade * 1.0; // Reduced brightness for first ring
                }
            }

            // Subsequent smaller rings
            for (int i = 1; i < NUM_RINGS; i++) {
                float startTime = SECONDS_AFTER_FIRST + float(i-1) * TIME_BETWEEN_RINGS;
                if (u_time < startTime) continue;

                float ringTime = u_time - startTime;
                float currentRadius = ringTime * (RING_SPEED - float(i) * 5.0);
                
                if (currentRadius > u_maxRadius) continue;

                float distToRing = abs(dist - currentRadius);
                float ringIntensity = 1.0 - smoothstep(0.0, RING_WIDTH * 0.5, distToRing);
                
                float fade = 1.0 - smoothstep(0.0, u_maxRadius, currentRadius);
                
                totalIntensity += ringIntensity * fade * 0.8;
            }

            if (totalIntensity <= 0.0) discard;

            // Add noise turbulence
            vec2 noiseUV = v_texCoord * 5.0 + vec2(u_time * 2.0);
            float noise = texture(u_noiseTexture, noiseUV).r;
            totalIntensity *= (0.6 + 0.4 * noise);

            vec3 finalColor = u_color * pow(clamp(totalIntensity, 0.0, 1.0), 2.0);
            
            // Apply fog (fade to black for additive)
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            finalColor *= fogFactor;

            out_FragColor = vec4(finalColor, clamp(totalIntensity, 0.0, 1.0));
        }
        """;

    public SonicBlastShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}

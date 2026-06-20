package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * A shader for rendering particles using hardware instancing.
 * Billboard expansion is performed in the vertex shader using gl_VertexID.
 */
public final class ParticleShader extends ShaderProgram implements FogShader {
    public interface Uniforms {
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String TEXTURE_ARRAY = "u_textureArray";
        String DEPTH_MAP = "u_depthMap";
        String IS_ADDITIVE = "u_isAdditive";
        String FOG_ENABLED = "u_fogEnabled";
        String NEAR_FAR = "u_nearFar"; // x = near, y = far
        String SOFT_RANGE = "u_softRange";
    }

    public interface Attributes {
        String CENTER_POSITION = "in_CenterPosition";
        String SIZE = "in_Size";
        String COLOR = Shader.COLOR;
        String UV_COORDS_1 = "in_UvCoords1"; // u1, v1, u2, v2
        String UV_COORDS_2 = "in_UvCoords2"; // u3, v3, u4, v4
        String TEX_SLOT = "in_TextureSlot";
    }

    public enum Attribute implements VertexAttribute {
        CENTER_POSITION(Attributes.CENTER_POSITION, 3, GL11.GL_FLOAT),
        SIZE(Attributes.SIZE, 3, GL11.GL_FLOAT), // radius_x, radius_y, radius_z
        COLOR(Attributes.COLOR, 4, GL11.GL_FLOAT),
        UV_COORDS_1(Attributes.UV_COORDS_1, 4, GL11.GL_FLOAT),
        UV_COORDS_2(Attributes.UV_COORDS_2, 4, GL11.GL_FLOAT),
        TEX_SLOT(Attributes.TEX_SLOT, 1, GL11.GL_FLOAT);

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

        @Override
        public @NonNull String getName() {
            return name;
        }

        @Override
        public int getComponentCount() {
            return componentCount;
        }

        @Override
        public int getGlType() {
            return glType;
        }

        @Override
        public boolean isNormalized() {
            return normalized;
        }
    }

    private static final String VERTEX_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_CenterPosition;
                    layout(location = 1) in vec3 in_Size;
                    layout(location = 3) in vec4 in_Color;
                    layout(location = 4) in vec4 in_UvCoords1;
                    layout(location = 5) in vec4 in_UvCoords2;
                    layout(location = 6) in float in_TextureSlot;

                    uniform mat4 u_modelViewMatrix;

                    out VS_OUT {
                        vec2 texCoord;
                        vec4 color;
                        float fogDist;
                        vec3 viewPos;
                        flat int texSlot;
                    } vs_out;

                    const vec2 OFFSETS[4] = vec2[](
                        vec2(-1.0, -1.0), // Bottom-left
                        vec2(1.0, -1.0),  // Bottom-right
                        vec2(-1.0, 1.0),  // Top-left
                        vec2(1.0, 1.0)    // Top-right
                    );

                    void main() {
                        vec3 center = in_CenterPosition;
                        vec3 radius = in_Size;
                        vs_out.color = in_Color;
                        vs_out.texSlot = int(in_TextureSlot + 0.5);

                        mat4 mv = u_modelViewMatrix;
                        vec3 right = vec3(mv[0][0], mv[1][0], mv[2][0]);
                        vec3 up = vec3(mv[0][1], mv[1][1], mv[2][1]);

                        vec3 scaledRight = right * radius.x;
                        vec3 scaledUp = up * radius.y;

                        vec4 viewCenter = mv * vec4(center, 1.0);
                        vs_out.fogDist = length(viewCenter.xyz);

                        vec2 uv_coords[4] = vec2[](
                            in_UvCoords1.xy, // Bottom-left
                            in_UvCoords1.zw, // Bottom-right
                            in_UvCoords2.zw, // Top-left
                            in_UvCoords2.xy  // Top-right
                        );

                        vec2 offset = OFFSETS[gl_VertexID];
                        vec3 p = center + (scaledRight * offset.x) + (scaledUp * offset.y);
                        vs_out.texCoord = uv_coords[gl_VertexID];

                        vec4 viewPosition = mv * vec4(p, 1.0);
                        vs_out.viewPos = viewPosition.xyz;
                        gl_Position = u_projectionMatrix * viewPosition;
                    }
                    """;

    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            GLOBAL_STATE_BLOCK +
            FOG_FUNCTION +
            """
                    uniform sampler2DArray u_textureArray;
                    uniform sampler2D u_depthMap;
                    uniform float u_isAdditive;
                    uniform int u_fogEnabled;
                    uniform vec2 u_nearFar;
                    uniform float u_softRange;

                    in VS_OUT {
                        vec2 texCoord;
                        vec4 color;
                        float fogDist;
                        vec3 viewPos;
                        flat int texSlot;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;

                    float getLinearDepth(float depth) {
                        float z = depth * 2.0 - 1.0;
                        return (2.0 * u_nearFar.x * u_nearFar.y) / (u_nearFar.y + u_nearFar.x - z * (u_nearFar.y - u_nearFar.x));
                    }

                    vec4 sampleParticle(vec2 uv) {
                        return texture(u_textureArray, vec3(uv, fs_in.texSlot));
                    }

                    void main() {
                        vec4 texColor = sampleParticle(fs_in.texCoord);
                        vec4 finalColor = fs_in.color * texColor;

                        if (finalColor.a <= 0.0) {
                            discard;
                        }

                        // Soft Particles (Depth Fading)
                        if (u_softRange > 0.0) {
                            vec2 screenUV = gl_FragCoord.xy / textureSize(u_depthMap, 0).xy;
                            float sceneDepth = getLinearDepth(texture(u_depthMap, screenUV).r);
                            float particleDepth = -fs_in.viewPos.z; // view-space Z is negative
                            float depthDiff = sceneDepth - particleDepth;
                            finalColor.a *= clamp(depthDiff / u_softRange, 0.0, 1.0);
                        }

                        float fogFactor = 1.0;
                        if (u_fogEnabled != 0) {
                            fogFactor = calculateFogFactor(fs_in.fogDist, gl_FragCoord.xy);
                        }
                        vec3 foggedColor = mix(u_fogColor.rgb, finalColor.rgb, fogFactor);
                        if (u_isAdditive > 0.5) {
                            foggedColor = finalColor.rgb * fogFactor;
                        }
                        out_FragColor = vec4(foggedColor, clamp(finalColor.a, 0.0, 1.0));
                    }
                    """;


    public ParticleShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

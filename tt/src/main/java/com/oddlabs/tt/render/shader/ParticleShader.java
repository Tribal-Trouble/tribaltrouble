package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/** A shader for rendering particles using a geometry shader to generate billboards. */
public final class ParticleShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String TEXTURE_0 = "u_texture0";
    }

    public interface Attributes {
        String CENTER_POSITION = "in_CenterPosition";
        String SIZE = "in_Size";
        String COLOR = "in_Color";
        String UV_INFO = "in_UvInfo"; // (u1, v1, u2, v2) for non-uniform sprites
    }

    public enum Attribute implements VertexAttribute {
        CENTER_POSITION(Attributes.CENTER_POSITION, 3, GL11.GL_FLOAT),
        SIZE(Attributes.SIZE, 2, GL11.GL_FLOAT), // width, height
        COLOR(Attributes.COLOR, 4, GL11.GL_FLOAT),
        UV_INFO(Attributes.UV_INFO, 4, GL11.GL_FLOAT);

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

        layout(location = 0) in vec3 in_CenterPosition;
        layout(location = 1) in vec2 in_Size;
        layout(location = 2) in vec4 in_Color;
        layout(location = 3) in vec4 in_UvInfo;

        uniform mat4 u_modelViewMatrix;

        out vec2 gs_Size;
        out vec4 gs_Color;
        out vec4 gs_UvInfo;

        void main() {
            // Transform center position to View Space
            gl_Position = u_modelViewMatrix * vec4(in_CenterPosition, 1.0);
            gs_Size = in_Size;
            gs_Color = in_Color;
            gs_UvInfo = in_UvInfo;
        }
        """;
        
    private static final String GEOMETRY_SHADER = """
        #version 410 core
        layout (points) in;
        layout (triangle_strip, max_vertices = 4) out;

        uniform mat4 u_projectionMatrix;

        in vec2[] gs_Size;
        in vec4[] gs_Color;
        in vec4[] gs_UvInfo;

        out vec2 v_texCoord;
        out vec4 v_color;
        out float v_fogDist;

        void main() {
            vec3 center = gl_in[0].gl_Position.xyz; // Center in View Space
            vec2 size = gs_Size[0];
            v_color = gs_Color[0];
            
            // In View Space, camera is at (0,0,0) looking down -Z.
            // Right is (1,0,0), Up is (0,1,0).
            vec3 right = vec3(1.0, 0.0, 0.0) * size.x;
            vec3 up = vec3(0.0, 1.0, 0.0) * size.y;

            // Bottom-left
            vec4 pos = vec4(center - right - up, 1.0);
            pos.z += 0.5; // Pull towards camera
            gl_Position = u_projectionMatrix * pos;
            v_texCoord = gs_UvInfo[0].xy;
            v_fogDist = length(center); // Use center distance for fog to be consistent per quad
            EmitVertex();

            // Bottom-right
            pos = vec4(center + right - up, 1.0);
            pos.z += 0.5; // Pull towards camera
            gl_Position = u_projectionMatrix * pos;
            v_texCoord = gs_UvInfo[0].zy;
            v_fogDist = length(center);
            EmitVertex();

            // Top-left
            pos = vec4(center - right + up, 1.0);
            pos.z += 0.5; // Pull towards camera
            gl_Position = u_projectionMatrix * pos;
            v_texCoord = gs_UvInfo[0].xw;
            v_fogDist = length(center);
            EmitVertex();

            // Top-right
            pos = vec4(center + right + up, 1.0);
            pos.z += 0.5; // Pull towards camera
            gl_Position = u_projectionMatrix * pos;
            
            v_texCoord = gs_UvInfo[0].zw;
            v_fogDist = length(center);
            EmitVertex();

            EndPrimitive();
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 410 core
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture0;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams;
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;

        in vec2 v_texCoord;
        in vec4 v_color;
        in float v_fogDist;
        
        layout(location = 0) out vec4 out_FragColor;

        void main() {
            vec4 texColor = texture(u_texture0, v_texCoord);
            vec4 finalColor = v_color * texColor;

            if (finalColor.a <= 0.0) {
                discard;
            }

            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            out_FragColor = vec4(mix(u_fogColor.rgb, finalColor.rgb, fogFactor), finalColor.a);
        }
        """;

    public ParticleShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER, GEOMETRY_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/** A simple shader for rendering particles (position, texture, color). */
public final class ParticleShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String TEXTURE_0 = "u_texture0";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String TEX_COORD = "a_texCoord0";
        String COLOR = "a_color";
    }

    public enum Attribute implements VertexAttribute {
        // Order determines layout in buffer!
        POSITION(Attributes.POSITION, 3, GL11.GL_FLOAT),
        TEX_COORD(Attributes.TEX_COORD, 2, GL11.GL_FLOAT),
        COLOR(Attributes.COLOR, 4, GL11.GL_FLOAT);

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

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec2 a_texCoord0;
        attribute vec4 a_color;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;

        varying vec2 v_texCoord0;
        varying vec4 v_color;
        varying float v_fogDist; // Pass view-space dist for fog calculation

        void main() {
            vec4 viewPosition = u_modelViewMatrix * vec4(a_position, 1.0);
            gl_Position = u_projectionMatrix * viewPosition;
            v_texCoord0 = a_texCoord0;
            v_color = a_color;
            v_fogDist = length(viewPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 120
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture0;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams; // x = density, y = start, z = end
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;

        varying vec2 v_texCoord0;
        varying vec4 v_color;
        varying float v_fogDist;

        void main() {
            vec4 texColor = texture2D(u_texture0, v_texCoord0);
            vec4 finalColor = vec4(v_color.rgb, v_color.a * texColor.a);

            if (finalColor.a <= 0.0) {
                discard;
            }

            // Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            gl_FragColor = vec4(mix(u_fogColor.rgb, finalColor.rgb, fogFactor), finalColor.a);
        }
        """;

    public ParticleShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

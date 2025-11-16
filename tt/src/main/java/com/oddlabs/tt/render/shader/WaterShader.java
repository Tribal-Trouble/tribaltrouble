package com.oddlabs.tt.render.shader;

/** A shader for rendering water surfaces. */
public final class WaterShader extends ShaderProgram {

    public static final class Uniforms {
        public static final String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        public static final String PROJECTION_MATRIX = "u_projectionMatrix";
        public static final String TEXTURE_0 = "u_texture0"; // Base water texture
        public static final String TEXTURE_1 = "u_texture1"; // Detail water texture
        public static final String WATER_REPEAT_RATE = "u_waterRepeatRate";
        public static final String WATER_DETAIL_REPEAT_RATE = "u_waterDetailRepeatRate";
        public static final String ENABLE_DETAIL = "u_enableDetail";

        private Uniforms() {}
    }

    public static final class Attributes {
        public static final String POSITION = "a_position";

        private Attributes() {}
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform float u_waterRepeatRate;
        uniform float u_waterDetailRepeatRate;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;

        void main() {
            gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(a_position, 1.0);
            v_texCoord0 = a_position.xy * u_waterRepeatRate;
            v_texCoord1 = a_position.xy * u_waterDetailRepeatRate;
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 120

        uniform sampler2D u_texture0;
        uniform sampler2D u_texture1;
        uniform bool u_enableDetail;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;

        void main() {
            vec4 baseColor = texture2D(u_texture0, v_texCoord0);
            vec4 finalColor = baseColor;

            if (u_enableDetail) {
                vec4 detailColor = texture2D(u_texture1, v_texCoord1);
                // Simple blending for now, can be improved later if needed
                finalColor = mix(baseColor, detailColor, detailColor.a); // Use alpha of detail for blending
            }
            gl_FragColor = finalColor;
        }
        """;

    public WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

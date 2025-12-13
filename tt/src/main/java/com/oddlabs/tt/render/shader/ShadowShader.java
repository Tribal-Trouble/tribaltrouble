package com.oddlabs.tt.render.shader;

public final class ShadowShader extends ShaderProgram {

    public static final class Uniforms {
        public static final String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        public static final String PROJECTION_MATRIX = "u_projectionMatrix";
        public static final String TEXTURE = "u_texture";
        public static final String COLOR = "u_color";
        public static final String SHADOW_PARAMS = "u_shadowParams"; // vec4(scaleX, scaleY, offsetX, offsetY)

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
        uniform vec4 u_shadowParams; // .xy = scale, .zw = offset

        varying vec2 v_texCoord;

        void main() {
            gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(a_position, 1.0);
            
            // Projective texturing logic
            // TexGen Object Linear: s = x * scale + offset, t = y * scale + offset
            // Legacy setupTexGen(scale, scale, -texture_x, -texture_y)
            // Implies s = x * scale - texture_x * scale ?
            // Actually setupTexGen usually sets Plane params.
            // GLUtils.setupTexGen(sx, sy, tx, ty) -> s = x*sx + tx, t = y*sy + ty?
            // Let's assume u_shadowParams contains the pre-calculated factors.
            
            v_texCoord.x = a_position.x * u_shadowParams.x + u_shadowParams.z;
            v_texCoord.y = a_position.y * u_shadowParams.y + u_shadowParams.w;
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 120

        uniform sampler2D u_texture;
        uniform vec4 u_color;

        varying vec2 v_texCoord;

        void main() {
            vec4 texColor = texture2D(u_texture, v_texCoord);
            
            // Modulate texture with color
            gl_FragColor = texColor * u_color;
            
            // Legacy: GL_DEPTH_FUNC(GL_EQUAL) used to be set.
            // This suggests rendering ON TOP of existing geometry with equal depth.
            // Z-fighting might be an issue without polygon offset or exact vertex match.
            // Since we use the same terrain vertices, GL_EQUAL should work if matrices match perfectly.
        }
        """;

    public ShadowShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER, STANDARD_ATTRIBUTES);
    }
}

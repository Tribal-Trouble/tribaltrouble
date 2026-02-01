package com.oddlabs.tt.render.shader;

public final class ShadowShader extends ShaderProgram {

    public static final class Uniforms {
        public static final String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        public static final String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        public static final String TEXTURE = "u_texture";
        public static final String COLOR = Shader.COLOR;
        public static final String SHADOW_PARAMS = "u_shadowParams"; // vec4(scaleX, scaleY, offsetX, offsetY)
        public static final String HEIGHT_MAP = "u_HeightMap";
        public static final String PATCH_OFFSET = "u_PatchOffset";
        public static final String WORLD_SIZE = "u_WorldSize";

        private Uniforms() {}
    }

    public static final class Attributes {
        public static final String POSITION = Shader.POSITION;

        private Attributes() {}
    }

    private static final String VERTEX_SHADER = """
        #version 410 core

        layout(location = 0) in vec2 in_Position;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform vec4 u_shadowParams; // .xy = scale, .zw = offset
        uniform vec2 u_PatchOffset;
        uniform float u_WorldSize;
        uniform sampler2D u_HeightMap;

        out vec2 v_texCoord;

        void main() {
            vec2 worldPos = u_PatchOffset + in_Position;
            vec2 uv = worldPos / u_WorldSize;
            float h = texture(u_HeightMap, uv).r;
         
            vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, h, 1.0);
            gl_Position = u_projectionMatrix * viewPosition;
            
            v_texCoord.x = worldPos.x * u_shadowParams.x + u_shadowParams.z;
            v_texCoord.y = worldPos.y * u_shadowParams.y + u_shadowParams.w;
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 410 core

        uniform sampler2D u_texture;
        uniform vec4 u_color;

        in vec2 v_texCoord;
        
        layout(location = 0) out vec4 out_FragColor;

        void main() {
            vec4 texColor = texture(u_texture, v_texCoord);
        
            // Modulate texture with color
            out_FragColor = texColor * u_color;
        }
        """;

    public ShadowShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

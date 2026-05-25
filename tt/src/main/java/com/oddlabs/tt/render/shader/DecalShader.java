package com.oddlabs.tt.render.shader;

public final class DecalShader extends ShaderProgram {

    public static final class Uniforms {
        public static final String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        public static final String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        public static final String TEXTURE = "u_texture";
        public static final String HEIGHT_MAP = "u_HeightMap";
        public static final String WORLD_SIZE = "u_WorldSize";
        public static final String DEPTH_BIAS = "u_DepthBias";

        private Uniforms() {
        }
    }

    public static final class Attributes {
        public static final String POSITION = Shader.POSITION;
        public static final String INSTANCE_POS = "in_InstancePos";
        public static final String INSTANCE_SIZE = "in_InstanceSize";
        public static final String INSTANCE_COLOR = "in_InstanceColor";

        private Attributes() {
        }
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec2 in_Position;      // Grid vertex (-0.5 to 0.5)
            layout(location = 4) in vec2 in_InstancePos;   // World X, Y
            layout(location = 5) in float in_InstanceSize; // Size in meters
            layout(location = 3) in vec4 in_InstanceColor; // RGBA

            uniform mat4 u_modelViewMatrix;
            uniform float u_WorldSize;
            uniform float u_DepthBias;
            uniform sampler2D u_HeightMap;

            out vec2 v_TexCoord;
            out vec4 v_Color;

            void main() {
                vec2 localPos = in_Position * in_InstanceSize;
                vec2 worldPos = in_InstancePos + localPos;

                // Map world position to heightmap UV
                // Add half-texel offset to align vertex-centered heightmap (1 grid unit = 2 meters)
                vec2 mapUV = (worldPos + 1.0) / u_WorldSize;
                float h = texture(u_HeightMap, mapUV).r;

                vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, h, 1.0);
                viewPosition.z += u_DepthBias;

                gl_Position = u_projectionMatrix * viewPosition;

                // Texture coordinates (0..1) based on grid position (-0.5..0.5)
                v_TexCoord = in_Position + 0.5;
                v_Color = in_InstanceColor;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core

            uniform sampler2D u_texture;

            in vec2 v_TexCoord;
            in vec4 v_Color;

            layout(location = 0) out vec4 out_FragColor;

            void main() {
                vec4 texColor = texture(u_texture, v_TexCoord);
                out_FragColor = texColor * v_Color;
            }
            """;

    public DecalShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}

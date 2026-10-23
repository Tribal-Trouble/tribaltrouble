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

                // The terrain is planar per triangle, so interpolate the same two triangles per grid cell
                // instead of sampling bilinearly, or the decal sinks below the terrain between vertices
                ivec2 texels = textureSize(u_HeightMap, 0);
                vec2 grid = clamp(worldPos * float(texels.x) / u_WorldSize, vec2(0.0), vec2(texels) - 1.0);
                ivec2 cell = min(ivec2(floor(grid)), texels - 2);
                vec2 f = grid - vec2(cell);
                float h0 = texelFetch(u_HeightMap, cell, 0).r;
                float h1 = texelFetch(u_HeightMap, cell + ivec2(1, 0), 0).r;
                float h2 = texelFetch(u_HeightMap, cell + ivec2(0, 1), 0).r;
                float h3 = texelFetch(u_HeightMap, cell + ivec2(1, 1), 0).r;
                float h = f.x + f.y <= 1.0
                        ? h0 + (h1 - h0) * f.x + (h2 - h0) * f.y
                        : h3 + (h2 - h3) * (1.0 - f.x) + (h1 - h3) * (1.0 - f.y);

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

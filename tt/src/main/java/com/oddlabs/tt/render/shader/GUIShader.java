package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * Defines the shader program for rendering the 2D user interface.
 * This shader handles basic model-view-projection transformation,
 * vertex colors, and texture modulation.
 */
public final class GUIShader extends ShaderProgram {

    /**
     * The vertex shader source code for UI rendering.
     */
    private static final String VERTEX_SHADER = SHADER_HEADER +
            """
                    uniform mat4 u_projectionMatrix;
                    uniform mat4 u_modelViewMatrix;

                    layout(location = 0) in vec3 in_Position;
                    layout(location = 3) in vec4 in_Color;
                    layout(location = 2) in vec2 in_TexCoord;
                    layout(location = 4) in float in_TexIndex;
                    layout(location = 5) in vec4 in_ClipRect;

                    out VS_OUT {
                        vec4 Color;
                        vec2 TexCoord;
                        flat int TexIndex;
                        vec2 LogicalPos;
                        flat vec4 ClipRect;
                    } vs_out;

                    void main() {
                        vec4 pos = u_modelViewMatrix * vec4(in_Position, 1.0);
                        gl_Position = u_projectionMatrix * pos;
                        vs_out.Color = in_Color;
                        vs_out.TexCoord = in_TexCoord;
                        vs_out.TexIndex = int(in_TexIndex);
                        vs_out.LogicalPos = pos.xy;
                        vs_out.ClipRect = in_ClipRect;
                    }
                    """;

    /**
     * The fragment shader source code for UI rendering.
     */
    private static final String FRAGMENT_SHADER = SHADER_HEADER +
            """
                    uniform sampler2D u_textures[8];

                    in VS_OUT {
                        vec4 Color;
                        vec2 TexCoord;
                        flat int TexIndex;
                        vec2 LogicalPos;
                        flat vec4 ClipRect;
                    } fs_in;

                    layout(location = 0) out vec4 out_FragColor;
                    layout(location = 1) out vec4 out_MaskColor;

                    void main() {
                        if (fs_in.LogicalPos.x < fs_in.ClipRect.x || fs_in.LogicalPos.x > fs_in.ClipRect.z ||
                            fs_in.LogicalPos.y < fs_in.ClipRect.y || fs_in.LogicalPos.y > fs_in.ClipRect.w) {
                            discard;
                        }

                        vec4 color;
                        if (fs_in.TexIndex < 0) {
                            color = fs_in.Color;
                        } else {
                            vec4 texColor;
                            switch (fs_in.TexIndex) {
                                case 0: texColor = texture(u_textures[0], fs_in.TexCoord); break;
                                case 1: texColor = texture(u_textures[1], fs_in.TexCoord); break;
                                case 2: texColor = texture(u_textures[2], fs_in.TexCoord); break;
                                case 3: texColor = texture(u_textures[3], fs_in.TexCoord); break;
                                case 4: texColor = texture(u_textures[4], fs_in.TexCoord); break;
                                case 5: texColor = texture(u_textures[5], fs_in.TexCoord); break;
                                case 6: texColor = texture(u_textures[6], fs_in.TexCoord); break;
                                case 7: texColor = texture(u_textures[7], fs_in.TexCoord); break;
                                default: texColor = texture(u_textures[0], fs_in.TexCoord); break;
                            }
                            color = fs_in.Color * texColor;
                        }

                        out_FragColor = vec4(color.rgb * color.a, color.a);

                        // Write a special marker to the mask alpha channel to indicate "GUI Pixel".
                        // Team objects write alpha=1.0. Clear color is alpha=0.0.
                        // We use alpha=0.5 to identify GUI pixels in the post-process shader.
                        // This allows us to exclude GUI pixels from the team outline effect
                        // while still applying CVD correction.
                        out_MaskColor = vec4(0.0, 0.0, 0.0, 0.5);
                    }
                    """;

    /**
     * Holds the names of the uniform variables used in the UI shader program.
     */
    public static final class Uniforms {
        private Uniforms() {
        }

        public static final String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        public static final String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        public static final String TEXTURES = "u_textures";
    }

    /**
     * Holds the names of the attribute variables used in the UI shader program.
     */
    static final class Attributes {
        private Attributes() {
        }

        public static final String POSITION = Shader.POSITION;
        public static final String COLOR = Shader.COLOR;
        public static final String TEX_COORD = Shader.TEX_COORD;
        public static final String TEX_INDEX = "in_TexIndex";
        public static final String CLIP_RECT = "in_ClipRect";
    }

    public enum Attribute implements VertexAttribute {
        POSITION(Attributes.POSITION, 3, GL11.GL_FLOAT),
        COLOR(Attributes.COLOR, 4, GL11.GL_FLOAT, false),
        TEX_COORD(Attributes.TEX_COORD, 2, GL11.GL_FLOAT),
        TEX_INDEX(Attributes.TEX_INDEX, 1, GL11.GL_FLOAT),
        CLIP_RECT(Attributes.CLIP_RECT, 4, GL11.GL_FLOAT);

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

    public GUIShader() {
        super(GUIShader.VERTEX_SHADER, GUIShader.FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor"); // Removed for GL 4.1 Core
        link();
    }
}

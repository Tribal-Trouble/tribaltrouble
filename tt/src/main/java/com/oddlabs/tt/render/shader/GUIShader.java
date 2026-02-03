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
    private static final String VERTEX_SHADER = """
            #version 410 core
            
            uniform mat4 u_projectionMatrix;
            uniform mat4 u_modelViewMatrix;
            
            layout(location = 0) in vec3 in_Position;
            layout(location = 3) in vec4 in_Color;
            layout(location = 2) in vec2 in_TexCoord;
            layout(location = 4) in float in_TexIndex;
            
            out vec4 v_Color;
            out vec2 v_TexCoord;
            flat out int v_TexIndex;
            
            void main() {
                gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(in_Position, 1.0);
                v_Color = in_Color;
                v_TexCoord = in_TexCoord;
                v_TexIndex = int(in_TexIndex);
            }
            """;

    /**
     * The fragment shader source code for UI rendering.
     */
    private static final String FRAGMENT_SHADER = """
            #version 410 core
            
            uniform sampler2D u_textures[8];
            
            in vec4 v_Color;
            in vec2 v_TexCoord;
            flat in int v_TexIndex;
            
            layout(location = 0) out vec4 out_FragColor;
            layout(location = 1) out vec4 out_MaskColor;
            
            void main() {
                if (v_TexIndex < 0) {
                    out_FragColor = v_Color;
                } else {
                    vec4 texColor;
                    switch (v_TexIndex) {
                        case 0: texColor = texture(u_textures[0], v_TexCoord); break;
                        case 1: texColor = texture(u_textures[1], v_TexCoord); break;
                        case 2: texColor = texture(u_textures[2], v_TexCoord); break;
                        case 3: texColor = texture(u_textures[3], v_TexCoord); break;
                        case 4: texColor = texture(u_textures[4], v_TexCoord); break;
                        case 5: texColor = texture(u_textures[5], v_TexCoord); break;
                        case 6: texColor = texture(u_textures[6], v_TexCoord); break;
                        case 7: texColor = texture(u_textures[7], v_TexCoord); break;
                        default: texColor = texture(u_textures[0], v_TexCoord); break;
                    }
                    out_FragColor = v_Color * texColor;
                }
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
    }

    public enum Attribute implements VertexAttribute {
        POSITION(Attributes.POSITION, 3, GL11.GL_FLOAT),
        COLOR(Attributes.COLOR, 4, GL11.GL_UNSIGNED_BYTE, true),
        TEX_COORD(Attributes.TEX_COORD, 2, GL11.GL_FLOAT),
        TEX_INDEX(Attributes.TEX_INDEX, 1, GL11.GL_FLOAT);

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

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
            layout(location = 1) in vec4 in_Color;
            layout(location = 2) in vec2 in_TexCoord;
            
            out vec4 v_Color;
            out vec2 v_TexCoord;
            
            void main() {
                gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(in_Position, 1.0);
                v_Color = in_Color;
                v_TexCoord = in_TexCoord;
            }
            """;

    /**
     * The fragment shader source code for UI rendering.
     */
    private static final String FRAGMENT_SHADER = """
            #version 410 core
            
            uniform sampler2D u_texture;
            
            in vec4 v_Color;
            in vec2 v_TexCoord;
            
            layout(location = 0) out vec4 out_FragColor;
            
            void main() {
                if (v_TexCoord.x < 0.0) {
                    out_FragColor = v_Color;
                } else {
                    out_FragColor = v_Color * texture(u_texture, v_TexCoord);
                }
            }
            """;

    /**
     * Holds the names of the uniform variables used in the UI shader program.
     */
    public static final class Uniforms {
        private Uniforms() {
        }

        public static final String PROJECTION_MATRIX = "u_projectionMatrix";
        public static final String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        public static final String TEXTURE = "u_texture";
    }

    /**
     * Holds the names of the attribute variables used in the UI shader program.
     */
    static final class Attributes {
        private Attributes() {
        }

        public static final String POSITION = "in_Position";
        public static final String COLOR = "in_Color";
        public static final String TEX_COORD = "in_TexCoord";
    }

    public enum Attribute implements VertexAttribute {
        POSITION(Attributes.POSITION, 3, GL11.GL_FLOAT),
        COLOR(Attributes.COLOR, 4, GL11.GL_UNSIGNED_BYTE, true),
        TEX_COORD(Attributes.TEX_COORD, 2, GL11.GL_FLOAT);

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

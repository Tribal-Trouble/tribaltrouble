package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class LightningShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String TEXTURE_0 = "u_texture0";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = FogShader.FOG_HEIGHT_FACTOR;
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
        String TEX_COORD = Shader.TEX_COORD;
        String COLOR = Shader.COLOR;
    }

    public enum Attribute implements VertexAttribute {
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
            #version 410 core
            """ +
            GLOBAL_STATE_BLOCK +
            """
                    layout(location = 0) in vec3 in_Position;
                    layout(location = 2) in vec2 in_TexCoord;
                    layout(location = 3) in vec4 in_Color;
                    
                    uniform mat4 u_modelViewMatrix;
                    
                    out vec2 v_texCoord;
                    out vec4 v_color;
                    out float v_fogDist;
                    
                    void main() {
                        vec4 viewPos = u_modelViewMatrix * vec4(in_Position, 1.0);
                        gl_Position = u_projectionMatrix * viewPos;
                        v_texCoord = in_TexCoord;
                        v_color = in_Color;
                        v_fogDist = length(viewPos.xyz);
                    }
                    """;

    private static final String FRAGMENT_SHADER =
            """
                    #version 410 core
                    """ +
                    GLOBAL_STATE_BLOCK +
                    FOG_FUNCTION +
                    """
                            uniform sampler2D u_texture0;
                            
                            in vec2 v_texCoord;
                            in vec4 v_color;
                            in float v_fogDist;
                            
                            layout(location = 0) out vec4 out_FragColor;
                            
                            void main() {
                                vec4 texColor = texture(u_texture0, v_texCoord);
                                vec4 finalColor = v_color * texColor;
                            
                                float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
                            
                                // Additive blending: fade to black (multiply by fogFactor)
                                out_FragColor = vec4(finalColor.rgb * fogFactor, finalColor.a);
                            }
                            """;

    public LightningShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
        int blockIndex = org.lwjgl.opengl.GL31.glGetUniformBlockIndex(state.programId, "GlobalState");
        if (blockIndex != -1) {
            org.lwjgl.opengl.GL31.glUniformBlockBinding(state.programId, blockIndex, 0);
        }
    }
}

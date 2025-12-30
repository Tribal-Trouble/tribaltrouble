package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class LightningShader extends ShaderProgram {

    public interface Uniforms {
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String TEXTURE_0 = "u_texture0";
    }

    public interface Attributes {
        String POSITION = "in_Position";
        String TEX_COORD = "in_TexCoord";
        String COLOR = "in_Color";
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

        @Override public @NonNull String getName() { return name; }
        @Override public int getComponentCount() { return componentCount; }
        @Override public int getGlType() { return glType; }
        @Override public boolean isNormalized() { return normalized; }
    }

    private static final String VERTEX_SHADER = """
        #version 410 core

        layout(location = 0) in vec3 in_Position;
        layout(location = 1) in vec2 in_TexCoord;
        layout(location = 2) in vec4 in_Color;

        uniform mat4 u_projectionMatrix;
        uniform mat4 u_modelViewMatrix;

        out vec2 v_texCoord;
        out vec4 v_color;

        void main() {
            vec4 viewPos = u_modelViewMatrix * vec4(in_Position, 1.0);
            gl_Position = u_projectionMatrix * viewPos;
            v_texCoord = in_TexCoord;
            v_color = in_Color;
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 410 core
        
        uniform sampler2D u_texture0;

        in vec2 v_texCoord;
        in vec4 v_color;
        
        layout(location = 0) out vec4 out_FragColor;

        void main() {
            vec4 texColor = texture(u_texture0, v_texCoord);
            out_FragColor = v_color * texColor;
        }
        """;

    public LightningShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}

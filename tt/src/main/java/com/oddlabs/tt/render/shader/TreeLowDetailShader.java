package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/** A simple shader for rendering low-detail trees (position, texture). */
public final class TreeLowDetailShader extends ShaderProgram {

    public static final class Uniforms {
        public static final String MVP_MATRIX = "u_mvpMatrix";
        public static final String TEXTURE_0 = "u_texture0";

        private Uniforms() {}
    }

    public static final class Attributes {
        public static final String POSITION = "a_position";
        public static final String TEX_COORD = "a_texCoord0";

        private Attributes() {}
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec2 a_texCoord0;

        uniform mat4 u_mvpMatrix;

        varying vec2 v_texCoord0;

        void main() {
            gl_Position = u_mvpMatrix * vec4(a_position, 1.0);
            v_texCoord0 = a_texCoord0;
        }
        """;

    private static final String FRAGMENT_SHADER = """
        #version 120

        uniform sampler2D u_texture0;

        varying vec2 v_texCoord0;

        void main() {
            gl_FragColor = texture2D(u_texture0, v_texCoord0);
            if (gl_FragColor.a <= 0.3) discard; // Hardcoded alpha test for trees
        }
        """;

    public TreeLowDetailShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}

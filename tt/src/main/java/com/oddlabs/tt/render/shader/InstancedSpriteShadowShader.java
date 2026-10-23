package com.oddlabs.tt.render.shader;

public final class InstancedSpriteShadowShader extends ShaderProgram implements Shader {

    public interface Uniforms {
        String TEXTURE_0 = InstancedSpriteShader.Uniforms.TEXTURE_0;
        String VERT_BUFFER = InstancedSpriteShader.Uniforms.VERT_BUFFER;
    }

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + GLOBAL_STATE_BLOCK + """
            layout(location = 2) in vec2 in_TexCoord;

            layout(location = 4) in mat4 in_InstanceModelMatrix;
            layout(location = 10) in float in_Pos1;
            layout(location = 12) in float in_Pos2;
            layout(location = 14) in float in_Tween;

            uniform samplerBuffer u_VertBuffer;

            out vec2 v_texCoord0;

            void main() {
                vec3 pos1 = texelFetch(u_VertBuffer, int(round(in_Pos1)) + gl_VertexID).xyz;
                vec3 pos2 = texelFetch(u_VertBuffer, int(round(in_Pos2)) + gl_VertexID).xyz;
                vec3 position = mix(pos1, pos2, in_Tween);

                gl_Position = u_projectionMatrix * u_viewMatrix * in_InstanceModelMatrix * vec4(position, 1.0);
                v_texCoord0 = in_TexCoord;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            uniform sampler2D u_texture0;

            in vec2 v_texCoord0;

            const float ALPHA_TEST_VALUE = 0.1;

            void main() {
                if (texture(u_texture0, v_texCoord0).a <= ALPHA_TEST_VALUE) {
                    discard;
                }
            }
            """;

    public InstancedSpriteShadowShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}

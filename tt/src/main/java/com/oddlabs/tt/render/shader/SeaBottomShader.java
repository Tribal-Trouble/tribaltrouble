package com.oddlabs.tt.render.shader;

/**
 * A shader for rendering the sea bottom, including a detail texture and fog.
 */
public final class SeaBottomShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = Shader.MODEL_VIEW_MATRIX;
        String PROJECTION_MATRIX = Shader.PROJECTION_MATRIX;
        String TEXTURE_1 = "u_texture1"; // Detail texture
        String BASE_COLOR = "u_baseColor";
        String DETAIL_SCALE = "u_detailScale";
    }

    public interface Attributes {
        String POSITION = Shader.POSITION;
    }

    private static final String VERTEX_SHADER = """
        #version 410 core
        """ +
        GLOBAL_STATE_BLOCK +
        """
        layout(location = 0) in vec3 in_Position;

        uniform mat4 u_modelViewMatrix;
        uniform float u_detailScale;

        out vec2 v_texCoordDetail;
        out float v_fogDist;

        void main() {
            vec4 worldPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
            gl_Position = u_projectionMatrix * worldPosition;
        
            v_texCoordDetail = in_Position.xy * u_detailScale;
        
            v_fogDist = length(worldPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 410 core
        """ +
        GLOBAL_STATE_BLOCK +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture1; // Detail texture
        uniform vec4 u_baseColor;
        uniform float u_detailScale;

        in vec2 v_texCoordDetail;
        in float v_fogDist;
        
        layout(location = 0) out vec4 out_FragColor;

        void main() {
            vec4 color = u_baseColor;
        
            if (u_detailScale > 0.0001) {
               vec4 detail = texture(u_texture1, v_texCoordDetail);
               color.rgb = mix(color.rgb, detail.rgb, detail.a);
            }
        
            float fogFactor = calculateFogFactor(v_fogDist, gl_FragCoord.xy);
            out_FragColor = vec4(mix(u_fogColor.rgb, color.rgb, fogFactor), color.a);
        }
        """;

    public SeaBottomShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

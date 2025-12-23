package com.oddlabs.tt.render.shader;

/** A shader for rendering water surfaces. */
public final class WaterShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String TEXTURE_0 = "u_texture0"; // Base water texture
        String TEXTURE_1 = "u_texture1"; // Detail water texture
        String WATER_REPEAT_RATE = "u_waterRepeatRate";
        String WATER_DETAIL_REPEAT_RATE = "u_waterDetailRepeatRate";
        String ENABLE_DETAIL = "u_enableDetail";
        String SCROLL_OFFSET_0 = "u_scrollOffset0";
        String SCROLL_OFFSET_1 = "u_scrollOffset1";
        String LIGHT_DIR = "u_lightDir";
        String CAMERA_POS = "u_cameraPos";

        // Fog Uniforms
        String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    }

    public interface Attributes {
        String POSITION = "in_Position";
    }

    private static final String VERTEX_SHADER = """
        #version 410 core

        layout(location = 0) in vec3 in_Position;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform float u_waterRepeatRate;
        uniform float u_waterDetailRepeatRate;
        uniform vec2 u_scrollOffset0;
        uniform vec2 u_scrollOffset1;

        out vec2 v_texCoord0;
        out vec2 v_texCoord1;
        out float v_fogDist;
        out vec3 v_worldPos;

        void main() {
            v_worldPos = in_Position;
            
            vec4 viewPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
            gl_Position = u_projectionMatrix * viewPosition;
            
            v_texCoord0 = in_Position.xy * u_waterRepeatRate + u_scrollOffset0;
            v_texCoord1 = in_Position.xy * u_waterDetailRepeatRate + u_scrollOffset1;
            v_fogDist = length(viewPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 410 core
        """ +
        FOG_FUNCTION +
        """
        uniform sampler2D u_texture0;
        uniform sampler2D u_texture1;
        uniform bool u_enableDetail;
        uniform vec3 u_lightDir;
        uniform vec3 u_cameraPos;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams;
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;
        
        in vec2 v_texCoord0;
        in vec2 v_texCoord1;
        in float v_fogDist;
        in vec3 v_worldPos;
        
        layout(location = 0) out vec4 out_FragColor;

        float getNoise(vec2 uv) {
            return texture(u_texture0, uv).r;
        }
        
        float getDetailNoise(vec2 uv) {
            return texture(u_texture1, uv).r;
        }

        void main() {
            vec4 baseColor = texture(u_texture0, v_texCoord0);
            vec4 finalColor = baseColor;
            
            float eps = 0.01;
            float h = getNoise(v_texCoord0);
            float h_x = getNoise(v_texCoord0 + vec2(eps, 0.0));
            float h_y = getNoise(v_texCoord0 + vec2(0.0, eps));
            
            if (u_enableDetail) {
                vec4 detailColor = texture(u_texture1, v_texCoord1);
                finalColor = mix(baseColor, detailColor, detailColor.a);
                
                float d = getDetailNoise(v_texCoord1);
                float d_x = getDetailNoise(v_texCoord1 + vec2(eps, 0.0));
                float d_y = getDetailNoise(v_texCoord1 + vec2(0.0, eps));
                
                h += d * 0.5;
                h_x += d_x * 0.5;
                h_y += d_y * 0.5;
            }
            
            vec3 normal = normalize(vec3((h - h_x) * 2.0, (h - h_y) * 2.0, 0.1));

            vec3 lightDir = normalize(u_lightDir);
            vec3 viewDir = normalize(u_cameraPos - v_worldPos);
            vec3 halfDir = normalize(lightDir + viewDir);
            
            float specAngle = max(dot(normal, halfDir), 0.0);
            float specular = pow(specAngle, 30.0);
            
            float F0 = 0.04; 
            float F = F0 + (1.0 - F0) * pow(1.0 - max(dot(halfDir, viewDir), 0.0), 5.0);
            
            finalColor.rgb += vec3(specular * F);
            
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            
            out_FragColor.rgb = mix(u_fogColor.rgb, finalColor.rgb, fogFactor);
            out_FragColor.a = finalColor.a;
        }
        """;

    public WaterShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}

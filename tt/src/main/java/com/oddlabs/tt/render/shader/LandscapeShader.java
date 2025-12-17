package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;

public final class LandscapeShader extends ShaderProgram implements FogShader, LitShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        String HEIGHT_MAP = "u_HeightMap";
        String DIFFUSE_MAP = "u_DiffuseMap";
        String NORMAL_MAP = "u_NormalMap";
        String DETAIL_MAP = "u_DetailMap";
        String PATCH_OFFSET = "u_PatchOffset";
        String WORLD_SIZE = "u_WorldSize";
        String DETAIL_SCALE = "u_DetailScale";
        String LIGHT_DIRECTION = "u_lightDirection";
        String GLOBAL_AMBIENT = "u_globalAmbient";
    }

    public interface Attributes {
        String POSITION = "a_position";
    }

    private static final String VERTEX_SHADER =
        """
        #version 120
        """ +
        """
        attribute vec2 a_position;

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform vec2 u_PatchOffset;
        uniform float u_WorldSize;
        uniform float u_DetailScale;
        uniform sampler2D u_HeightMap;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;
        varying float v_fogDist;
        varying vec3 v_viewPosition;

        void main() {
            vec2 worldPos = u_PatchOffset + a_position;
            vec2 uv = worldPos / u_WorldSize;
            float h = texture2D(u_HeightMap, uv).r;
            
            vec4 viewPosition = u_modelViewMatrix * vec4(worldPos, h, 1.0);
            gl_Position = u_projectionMatrix * viewPosition;
            
            v_texCoord0 = uv;
            v_texCoord1 = worldPos * u_DetailScale;
            v_fogDist = length(viewPosition.xyz);
            v_viewPosition = viewPosition.xyz;
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 120
        """ +
        FOG_FUNCTION +
        LitShader.LIGHTING_FUNCTION +
        """
        uniform sampler2D u_DiffuseMap;
        uniform sampler2D u_NormalMap;
        uniform sampler2D u_DetailMap;
        uniform vec3 u_lightDirection;
        uniform vec3 u_globalAmbient;
        uniform mat4 u_modelViewMatrix;

        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams;
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;

        varying vec2 v_texCoord0;
        varying vec2 v_texCoord1;
        varying float v_fogDist;
        varying vec3 v_viewPosition;

        void main() {
            vec4 diffuseColor = texture2D(u_DiffuseMap, v_texCoord0);
            vec4 detailColor = texture2D(u_DetailMap, v_texCoord1);
            
            // Apply detail map
            diffuseColor.rgb = mix(diffuseColor.rgb, detailColor.rgb, detailColor.a);
            
            vec3 normal = texture2D(u_NormalMap, v_texCoord0).xyz;
            normal = normalize(normal * 2.0 - 1.0);
            
            vec4 litColor = calculateLighting(normal, diffuseColor, u_modelViewMatrix, u_lightDirection, u_globalAmbient);
            
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            gl_FragColor = vec4(mix(u_fogColor.rgb, litColor.rgb, fogFactor), litColor.a);
        }
        """;

    public LandscapeShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER, programId -> {
            org.lwjgl.opengl.GL20.glBindAttribLocation(programId, 0, "a_position");
        });
    }
}

package com.oddlabs.tt.render.shader;

public final class LandscapeShader extends ShaderProgram implements FogShader {

    public interface Uniforms {
        String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
        String PROJECTION_MATRIX = "u_projectionMatrix";
        
        String LIGHT_DIR = "u_lightDirection";
        String GLOBAL_AMBIENT = "u_globalAmbient";

        // Structure Textures (up to 8 layers)
        String STRUCTURE_0 = "u_structure0";
        String STRUCTURE_1 = "u_structure1";
        String STRUCTURE_2 = "u_structure2";
        String STRUCTURE_3 = "u_structure3";
        String STRUCTURE_4 = "u_structure4";
        String STRUCTURE_5 = "u_structure5";
        String STRUCTURE_6 = "u_structure6";
        String STRUCTURE_7 = "u_structure7";

        // Alpha Maps for blending structures
        String ALPHA_MAP_0 = "u_alphaMap0";
        String ALPHA_MAP_1 = "u_alphaMap1";
        String ALPHA_MAP_2 = "u_alphaMap2";
        String ALPHA_MAP_3 = "u_alphaMap3";
        String ALPHA_MAP_4 = "u_alphaMap4";
        String ALPHA_MAP_5 = "u_alphaMap5";
        String ALPHA_MAP_6 = "u_alphaMap6";
        String ALPHA_MAP_7 = "u_alphaMap7";

        // Pre-baked light maps
        String HIGHLIGHT_MAP = "u_highlightMap";
        String SHADOW_MAP = "u_shadowMap";
    }

    public interface Attributes {
        String POSITION = "a_position";
        String NORMAL = "a_normal";
        String TEX_COORD_0 = "a_texCoord0"; // Colormap
        String TEX_COORD_1 = "a_texCoord1"; // Lightmap
    }

    private static final String VERTEX_SHADER = """
        #version 120

        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec2 a_texCoord0; // Colormap texcoord
        attribute vec2 a_texCoord1; // Lightmap texcoord

        uniform mat4 u_modelViewMatrix;
        uniform mat4 u_projectionMatrix;
        uniform vec3 u_lightDirection;

        varying vec2 v_texCoordMap;
        varying vec2 v_texCoordLight;
        varying float v_lightIntensity;
        varying float v_fogDist;

        void main() {
            vec4 worldPosition = u_modelViewMatrix * vec4(a_position, 1.0);
            gl_Position = u_projectionMatrix * worldPosition;
            
            // Pass through texcoords
            v_texCoordMap = a_texCoord0;
            v_texCoordLight = a_texCoord1;
            
            // Lighting (Diffuse)
            vec3 normal = normalize(a_normal); 
            vec3 lightDir = normalize(u_lightDirection);
            v_lightIntensity = max(dot(normal, lightDir), 0.0);
            
            // Pass view distance for fog calculation
            v_fogDist = length(worldPosition.xyz);
        }
        """;

    private static final String FRAGMENT_SHADER =
        """
        #version 120
        """ +
        FOG_FUNCTION +
        """
        // Structure Textures
        uniform sampler2D u_structure0;
        uniform sampler2D u_structure1;
        uniform sampler2D u_structure2;
        uniform sampler2D u_structure3;
        uniform sampler2D u_structure4;
        uniform sampler2D u_structure5;
        uniform sampler2D u_structure6;
        uniform sampler2D u_structure7;
        
        // Packed Alpha Maps
        // u_packedAlpha0: R=Alpha0, G=Alpha1, B=Alpha2, A=Alpha3
        // u_packedAlpha1: R=Alpha4, G=Highlight, B=Shadow(Alpha6), A=Alpha7
        uniform sampler2D u_packedAlpha0;
        uniform sampler2D u_packedAlpha1;

        uniform vec3 u_globalAmbient;
        
        // Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams;
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;

        varying vec2 v_texCoordMap;
        varying vec2 v_texCoordLight;
        varying float v_lightIntensity;
        varying float v_fogDist;

        void main() {
            vec4 packed0 = texture2D(u_packedAlpha0, v_texCoordLight);
            vec4 packed1 = texture2D(u_packedAlpha1, v_texCoordLight);

            // 1. Layer all the terrain textures using their packed alpha maps
            vec4 color = texture2D(u_structure0, v_texCoordMap);
            color = mix(color, texture2D(u_structure1, v_texCoordMap), packed0.g);
            color = mix(color, texture2D(u_structure2, v_texCoordMap), packed0.b);
            color = mix(color, texture2D(u_structure3, v_texCoordMap), packed0.a);
            color = mix(color, texture2D(u_structure4, v_texCoordMap), packed1.r);
            // Index 5 is highlight (packed1.g), skipping structure mixing for it
            color = mix(color, texture2D(u_structure6, v_texCoordMap), packed1.b);
            color = mix(color, texture2D(u_structure7, v_texCoordMap), packed1.a);
            
            // 2. Apply standard dynamic lighting
            vec3 light = u_globalAmbient + vec3(v_lightIntensity);
            vec3 litColor = color.rgb * clamp(light, 0.0, 1.0);
            
            // 3. Apply pre-baked highlight and shadow maps
            float highlight = packed1.g; // Highlight is in the green channel of packedAlpha1
            float shadow = packed1.b;    // Shadow is in the blue channel of packedAlpha1
            
            // Emulate FFP blending: Dst' = Dst*(1+Src) for highlight
            vec3 highlightedColor = litColor * (1.0 + highlight);
            vec3 finalColor = highlightedColor * (1.0 - shadow);

            gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
            
            // 4. Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
            gl_FragColor = vec4(mix(u_fogColor.rgb, gl_FragColor.rgb, fogFactor), gl_FragColor.a);
        }
        """;
    public LandscapeShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER, STANDARD_ATTRIBUTES);
    }
}

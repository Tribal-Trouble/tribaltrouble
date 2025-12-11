package com.oddlabs.tt.render.shader;

/**
 * Interface for shaders that support standard fog.
 */
public interface FogShader extends Shader {
    int FOG_MODE_LINEAR = 0;
    int FOG_MODE_EXP = 1;
    int FOG_MODE_EXP2 = 2;
    int FOG_MODE_RADIAL = 3;
    String FOG_COLOR = "u_fogColor";
    String FOG_MODE = "u_fogMode";
    String FOG_PARAMS = "u_fogParams";
    String FOG_HEIGHT_FACTOR = "u_fogHeightFactor";
    String CAMERA_HEIGHT = "u_cameraHeight";

    String FOG_FUNCTION = """
        float calculateFogFactor(
            int fogMode, 
            vec3 fogParams, 
            float fogHeightFactor, 
            float cameraHeight, 
            float dist, 
            vec2 fragCoord
        ) {
            if (fogMode == 3) { // Radial fog for map view
                vec2 resolution = fogParams.xy;
                float density = fogParams.z;
                float radius = max(resolution.x, resolution.y) / 2.0;
                
                // Center coordinates and correct for aspect ratio
                vec2 centeredCoords = fragCoord - resolution / 2.0;
                
                float distance = length(centeredCoords);
                return 1.0 - smoothstep(radius - (radius * density), radius, distance);
            }

            float fogFactor = 1.0;
            float effectiveDensity = fogParams.x;
            if (fogHeightFactor > 0.0) {
                 effectiveDensity *= (1.0 - clamp(cameraHeight / fogHeightFactor, 0.0, 1.0));
            }

            if (fogMode == 0) { // GL_LINEAR
                fogFactor = (fogParams.z - dist) / (fogParams.z - fogParams.y);
            } else if (fogMode == 1) { // GL_EXP
                fogFactor = exp(-dist * effectiveDensity);
            } else if (fogMode == 2) { // GL_EXP2
                fogFactor = exp(-pow(dist * effectiveDensity, 2.0));
            }
            return clamp(fogFactor, 0.0, 1.0);
        }
        """;
}

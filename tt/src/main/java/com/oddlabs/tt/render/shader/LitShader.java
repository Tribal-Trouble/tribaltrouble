package com.oddlabs.tt.render.shader;

/**
 * Interface for shaders that support a common, simple lighting model.
 */
public interface LitShader extends Shader {
    String LIGHT_DIR = "u_lightDirection";
    String GLOBAL_AMBIENT = "u_globalAmbient";

    String LIGHTING_FUNCTION = """
        vec4 calculateLighting(
            vec3 normal, 
            vec4 materialColor, 
            mat4 modelViewMatrix, 
            vec3 lightDirection, 
            vec3 globalAmbient
        ) {
            // Transform normal to view space
            vec3 transformedNormal = normalize((modelViewMatrix * vec4(normal, 0.0)).xyz);
            
            // Calculate diffuse lighting component
            float diffuse = max(dot(transformedNormal, normalize(lightDirection)), 0.0);
            
            // Combine ambient and diffuse
            vec3 light = globalAmbient + vec3(diffuse);
            
            // Apply lighting to material color
            return vec4(materialColor.rgb * clamp(light, 0.0, 1.0), materialColor.a);
        }
        """;
}

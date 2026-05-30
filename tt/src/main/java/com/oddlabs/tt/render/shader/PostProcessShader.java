package com.oddlabs.tt.render.shader;

/**
 * Shader for full-screen post-processing effects.
 * Handles Color Vision Deficiency (CVD) correction and High Contrast Mode.
 */
public final class PostProcessShader extends ShaderProgram {

    public interface Uniforms {
        String SCENE_TEXTURE = "u_sceneTexture";
        String MASK_TEXTURE = "u_maskTexture";
        String CVD_MODE = "u_cvdMode";
        String CVD_INTENSITY = "u_cvdIntensity";
        String HIGH_CONTRAST = "u_highContrast";
        String CONTRAST_INTENSITY = "u_contrastIntensity";
        String TEAM_STENCIL = "u_teamStencil";
    }

    public interface Attributes {
        String POSITION = "in_Position";
    }

    private static final String VERTEX_SHADER = """
            #version 410 core

            layout(location = 0) in vec2 in_Position;

            out vec2 v_texCoord;

            void main() {
                // Full screen quad coordinates: -1 to 1
                gl_Position = vec4(in_Position, 0.0, 1.0);
                v_texCoord = (in_Position + 1.0) * 0.5;
            }
            """;

    private static final String FRAGMENT_SHADER
            = """
                    #version 410 core

                    uniform sampler2D u_sceneTexture;
                    uniform sampler2D u_maskTexture;
                    uniform int u_cvdMode; // 0=None, 1=Protanopia, 2=Deuteranopia, 3=Tritanopia
                    uniform float u_cvdIntensity;
                    uniform bool u_highContrast;
                    uniform float u_contrastIntensity;
                    uniform bool u_teamStencil;

                    in vec2 v_texCoord;
                    layout(location = 0) out vec4 out_FragColor;

                    // --- CVD Logic ---
                    // LMS Color Space Matrices (Transposed for GLSL Column-Major)
                    const mat3 RGB_to_LMS = mat3(
                        17.8824, 3.45565, 0.0299566,
                        43.5161, 27.1554, 0.184309,
                        4.11935, 3.86714, 1.46709
                    );

                    const mat3 LMS_to_RGB = mat3(
                        0.0809, -0.0102, -0.0003,
                        -0.1305, 0.0540, -0.0041,
                        0.1167, -0.1136, 0.6935
                    );

                    // Simulation Matrices (Transposed for GLSL Column-Major)
                    const mat3 Protanopia_Sim = mat3(
                        0.0, 0.0, 0.0,
                        2.02344, 1.0, 0.0,
                        -2.52581, 0.0, 1.0
                    );

                    const mat3 Deuteranopia_Sim = mat3(
                        1.0, 0.494207, 0.0,
                        0.0, 0.0, 0.0,
                        0.0, 1.24827, 1.0
                    );

                    const mat3 Tritanopia_Sim = mat3(
                        1.0, 0.0, -0.395913,
                        0.0, 1.0, 0.801109,
                        0.0, 0.0, 0.0
                    );

                    vec3 daltonize(vec3 color) {
                        vec3 lms = RGB_to_LMS * color;
                        vec3 simulatedLMS;

                        if (u_cvdMode == 1) simulatedLMS = Protanopia_Sim * lms;
                        else if (u_cvdMode == 2) simulatedLMS = Deuteranopia_Sim * lms;
                        else if (u_cvdMode == 3) simulatedLMS = Tritanopia_Sim * lms;
                        else return color;

                        vec3 simulatedRGB = LMS_to_RGB * simulatedLMS;
                        vec3 error = color - simulatedRGB;

                        // Shift error to visible channels based on CVD type
                        vec3 correction = vec3(0.0);

                        if (u_cvdMode == 1 || u_cvdMode == 2) {
                            // Protanopia/Deuteranopia: Shift R/G error to Blue channel
                            correction.b = (error.r * 0.7) + (error.g * 0.7);
                        } else {
                            // Tritanopia: Shift B error to Red/Green channels
                            correction.r = error.b * 0.7;
                            correction.g = error.b * 0.7;
                        }

                        return color + correction * u_cvdIntensity;
                    }

                    // --- High Contrast Logic ---
                    vec3 applyHighContrast(vec3 color) {
                        // Convert to grayscale for luma
                        float luma = dot(color, vec3(0.299, 0.587, 0.114));

                        // Simple contrast curve
                        vec3 highContrast = (color - 0.5) * (1.0 + u_contrastIntensity) + 0.5;

                        // Edge detection could be added here with extra texture samples
                        // For now, boost saturation and value contrast
                        return mix(color, highContrast, u_contrastIntensity);
                    }

                    void main() {
                        vec4 sceneColor = texture(u_sceneTexture, v_texCoord);
                        vec3 finalColor = sceneColor.rgb;

                        if (u_highContrast) {
                            finalColor = applyHighContrast(finalColor);
                        }

                        if (u_teamStencil) {
                            // Team Stencil & Border
                            vec4 mask = texture(u_maskTexture, v_texCoord);

                            // Check for GUI marker (alpha ~ 0.5)
                            bool isGUI = abs(mask.a - 0.5) < 0.1;

                            if (!isGUI) {
                                // Check RGB intensity instead of Alpha, because GUI writes (0,0,0,1) to clear mask
                                if (dot(mask.rgb, vec3(1.0)) > 0.01) {
                                    // Stencil: blend 20% team color
                                    finalColor = mix(finalColor, mask.rgb, 0.2);
                                } else {
                                    // Border Detection (3-pixel thickness) with Despeckle
                                    vec2 texelSize = 1.0 / textureSize(u_maskTexture, 0);
                                    int maskCount = 0;
                                    vec3 accumulatedColor = vec3(0.0);

                                    // Search in a radius of 3 pixels
                                    for (int y = -3; y <= 3; y++) {
                                        for (int x = -3; x <= 3; x++) {
                                            if (x == 0 && y == 0) continue;
                                            if (abs(x) + abs(y) > 4) continue;

                                            vec4 neighbor = texture(u_maskTexture, v_texCoord + vec2(float(x) * texelSize.x, float(y) * texelSize.y));
                                            // Check neighbor RGB
                                            if (dot(neighbor.rgb, vec3(1.0)) > 0.01) {
                                                maskCount++;
                                                accumulatedColor += neighbor.rgb;
                                            }
                                        }
                                    }

                                    // Filter small features: 2x2 pixels = 4 pixels.
                                    if (maskCount > 4) {
                                        finalColor = accumulatedColor / float(maskCount);
                                    }
                                }
                            }
                        }

                        if (u_cvdMode > 0) {
                            finalColor = daltonize(finalColor);
                        }

                        out_FragColor = vec4(finalColor, sceneColor.a);
                    }
                    """;

    public PostProcessShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        link();
    }
}

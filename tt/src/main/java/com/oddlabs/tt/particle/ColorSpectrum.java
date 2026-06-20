package com.oddlabs.tt.particle;

import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Maps a spectrum position and base color to a Linear Color.
 */
@FunctionalInterface
public interface ColorSpectrum {
    /**
     * Resolves the color for a given spectrum value.
     *
     * @param spectrum The current spectrum value (typically between 0.0 and 1.0).
     * @param baseColor The base color of the emitter (must be in linear space).
     * @return The resolved linear Color value.
     */
    Color.@NonNull Linear getColor(float spectrum, Color.@NonNull Linear baseColor);
}

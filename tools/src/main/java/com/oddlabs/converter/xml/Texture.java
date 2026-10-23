package com.oddlabs.converter.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** One texture tier of a model, with the team decal and, when it glows, the glow image and its light. */
public record Texture(@NonNull String name, @Nullable String team, @Nullable String normal, @Nullable String emissive,
                      @JsonProperty("light_color") @Nullable String lightColor,
                      @JsonProperty("light_strength") @Nullable Float lightStrength,
                      @JsonProperty("light_reach") @Nullable Float lightReach) {

    private static final String LIGHT_COLOR_FORMAT = "#[0-9a-fA-F]{6}";
    private static final String LIGHT_EXAMPLE = "light_color=\"#ff8c26\" light_strength=\"1\" light_reach=\"8\"";

    public Texture {
        if (castsLight(lightColor, lightStrength, lightReach) && (emissive == null || lightColor == null
                || !lightColor.matches(LIGHT_COLOR_FORMAT) || lightStrength == null || lightStrength < 0
                || lightReach == null || lightReach < 0))
            throw new IllegalArgumentException(
                    "Texture " + name + " needs a glow (emissive=) and all three of " + LIGHT_EXAMPLE + " to cast light");
    }

    private static boolean castsLight(@Nullable String color, @Nullable Float strength, @Nullable Float reach) {
        return color != null || strength != null || reach != null;
    }

    public boolean castsLight() {
        return castsLight(lightColor, lightStrength, lightReach);
    }

    /** The list the game loads: name, team decal and, when there is one, the glow image. */
    public String @NonNull [] names() {
        return emissive != null ? new String[]{name, team, emissive} : new String[]{name, team};
    }
}

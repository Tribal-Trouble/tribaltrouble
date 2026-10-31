package com.oddlabs.geometry.registry;

import org.jspecify.annotations.NonNull;

import java.util.Comparator;

/**
 * A spot in a sprite's space where the glow of one of its textures lights the surroundings at night. The color is the
 * registry's #rrggbb string; reach is in meters and strength is 1 for normal.
 */
public record LightEntry(@NonNull String group, @NonNull String name, int texture, @NonNull String color,
                         float reach, float strength, float x, float y, float z) {
    public static final Comparator<LightEntry> ORDER = Comparator.comparing(LightEntry::group).thenComparing(
            LightEntry::name).thenComparingInt(LightEntry::texture).thenComparingDouble(
                    LightEntry::x).thenComparingDouble(LightEntry::y).thenComparingDouble(LightEntry::z);
}

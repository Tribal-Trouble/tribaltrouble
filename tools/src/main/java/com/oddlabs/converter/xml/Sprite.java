package com.oddlabs.converter.xml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oddlabs.geometry.registry.Registry;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * One drawable thing. Checks that need only the sprite itself run here; checks against other sprites of the group run
 * in the converter, which knows the group.
 */
public record Sprite(@NonNull String name, @NonNull Float scale, @Nullable String base, @Nullable String slot,
                     @JsonProperty("default") boolean defaultOn, @NonNull String event, @Nullable String skin,
                     @Nullable String replaces, @Nullable String decoration, @NonNull Integer count,
                     @Nullable SkeletonRef skeleton,
                     @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "model") @NonNull List<Model> models,
                     @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "animation") @NonNull List<Animation> animations) {

    private static final Set<String> DECORATION_GROUNDS = Set.of("beach", "dirt", "grass", "snow", "land");
    private static final float DEFAULT_SCALE = 1f;
    private static final int DEFAULT_DECORATION_COUNT = 20;
    private static final int MAX_DECORATION_COUNT = 1000;

    public Sprite {
        if (scale == null)
            scale = DEFAULT_SCALE;
        if (event == null)
            event = Registry.NO_EVENT;
        if (models == null)
            models = List.of();
        if (animations == null)
            animations = List.of();
        if (models.isEmpty())
            throw new IllegalArgumentException("Sprite " + name + " has no model");
        if (slot != null && base == null)
            throw new IllegalArgumentException("Sprite " + name + " has a slot but no base");
        if (decoration == null && count != null)
            throw new IllegalArgumentException("Sprite " + name + " has a count but no decoration");
        if (decoration != null) {
            if (!DECORATION_GROUNDS.containsAll(List.of(decoration.split(",", -1))))
                throw new IllegalArgumentException(
                        "Sprite " + name + " has decoration=\"" + decoration + "\"; use one or more of " + String.join(
                                ", ", DECORATION_GROUNDS.stream().sorted().toList()) + ", comma separated");
            if (slot != null || skin != null)
                throw new IllegalArgumentException(
                        "Sprite " + name + " cannot be a decoration and an attachment or skin");
        }
        if (count == null)
            count = DEFAULT_DECORATION_COUNT;
        if (count < 1 || count > MAX_DECORATION_COUNT)
            throw new IllegalArgumentException(
                    "Sprite " + name + " needs a count from 1 to " + MAX_DECORATION_COUNT + ", not " + count);
    }

    /** How many texture tiers the close-up model has. */
    public int tiers() {
        return models.get(0).textures().size();
    }
}

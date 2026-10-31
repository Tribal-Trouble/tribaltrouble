package com.oddlabs.geometry.registry;

import org.jspecify.annotations.NonNull;

import java.util.Comparator;

/** A sprite that stands in for another sprite of its group while the skin applies. */
public record SkinEntry(@NonNull String group, @NonNull String skin, @NonNull String replaces, @NonNull String name,
                        int textures, @NonNull String event) {
    public static final Comparator<SkinEntry> ORDER = Comparator.comparing(SkinEntry::group).thenComparing(
            SkinEntry::skin).thenComparing(SkinEntry::replaces).thenComparing(SkinEntry::name);
}

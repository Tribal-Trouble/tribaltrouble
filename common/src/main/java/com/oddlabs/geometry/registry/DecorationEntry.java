package com.oddlabs.geometry.registry;

import org.jspecify.annotations.NonNull;

import java.util.Comparator;

/** Scenery the game scatters on the named grounds, count per map. */
public record DecorationEntry(@NonNull String group, @NonNull String name, @NonNull String grounds, int count,
                              @NonNull String event) {
    public static final Comparator<DecorationEntry> ORDER = Comparator.comparing(DecorationEntry::group).thenComparing(
            DecorationEntry::name);
}

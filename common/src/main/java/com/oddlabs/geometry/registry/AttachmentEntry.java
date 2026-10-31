package com.oddlabs.geometry.registry;

import org.jspecify.annotations.NonNull;

import java.util.Comparator;

/** An item in a slot of its base sprite. */
public record AttachmentEntry(@NonNull String group, @NonNull String base, @NonNull String slot, boolean defaultOn,
                              @NonNull String name, int textures, @NonNull String event) {
    /** Items of a slot show default-on ones first, then by name. */
    public static final Comparator<AttachmentEntry> ORDER = Comparator.comparing(AttachmentEntry::group).thenComparing(
            AttachmentEntry::base).thenComparing(AttachmentEntry::slot).thenComparing(
                    entry -> !entry.defaultOn()).thenComparing(AttachmentEntry::name);
}

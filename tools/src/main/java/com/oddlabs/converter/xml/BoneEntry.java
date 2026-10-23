package com.oddlabs.converter.xml;

import org.jspecify.annotations.NonNull;

/** A bone and the bone it hangs from; the root names a parent that is not in the list. */
public record BoneEntry(@NonNull String name, @NonNull String parent) {
}

package com.oddlabs.converter.xml;

import org.jspecify.annotations.NonNull;

/** How much one bone moves a vertex. A vertex may list the same bone twice; the game sums the entries. */
public record SkinWeight(@NonNull String bone, float weight) {
}

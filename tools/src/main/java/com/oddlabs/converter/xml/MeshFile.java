package com.oddlabs.converter.xml;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A mesh file: triangles with skinned vertices. Older exports embed the skeleton and name a texture; both are ignored,
 * since the registry names the skeleton file and the textures.
 */
public record MeshFile(@Nullable String texture, @NonNull Polygons polygons, @Nullable SkeletonFile skeleton) {
}

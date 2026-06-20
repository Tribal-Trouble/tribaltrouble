package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

/**
 * A simulation-side provider of physical entity bounding boxes.
 * Implemented by render-side SpriteKeys to supply bounds without render dependencies.
 */
public interface BoundsProvider {
    @NonNull
    BoundingBox @NonNull [] bounds();
}

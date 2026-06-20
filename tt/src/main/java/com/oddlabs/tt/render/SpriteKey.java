package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.model.BoundsProvider;
import com.oddlabs.tt.model.BoundingBox;
import org.jspecify.annotations.NonNull;

/**
 * A render queue key identifying a sprite list, holding local bounds and animation types.
 */
public record SpriteKey(
                        int key,
                        @NonNull BoundingBox @NonNull [] bounds,
                        AnimationInfo.@NonNull AnimationType @NonNull [] animTypes
) implements RenderQueueKey, BoundsProvider {
}

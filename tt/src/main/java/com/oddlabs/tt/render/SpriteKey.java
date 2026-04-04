package com.oddlabs.tt.render;

import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;

public final class SpriteKey extends RenderQueueKey {
    private final @NonNull BoundingBox @NonNull [] bounds;
    private final int @NonNull [] anim_types;

    SpriteKey(int key, @NonNull BoundingBox @NonNull [] bounds, int @NonNull [] anim_types) {
        super(key);
        this.bounds = bounds;
        this.anim_types = anim_types;
    }

    public @NonNull BoundingBox getBounds(int anim_index) {
        return bounds[anim_index];
    }

    public int getAnimationType(int anim) {
        return anim_types[anim];
    }
}

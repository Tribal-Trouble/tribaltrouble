package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.particle.Emitter;
import java.util.Collection;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An accessory that is logically attached to a {@link Model}.
 * These accessories do not exist independently in the world quadtree and instead
 * share the lifecycle and visibility context of their parent.
 */
public sealed interface Accessory extends AutoCloseable permits StaticAccessory, AnimatedAccessory {
    /**
     * Returns the sprite to render, or null if this accessory is rendered via other means (e.g. emitters).
     *
     * @return The sprite renderer.
     */
    @Nullable
    SpriteKey getSpriteRenderer();

    /**
     * Returns true if the accessory should currently be drawn.
     *
     * @param parent The model this accessory is attached to.
     * @param camera The current camera state for distance/visibility checks.
     * @return visibility status.
     */
    boolean isVisible(@NonNull Model parent, @NonNull CameraState camera);

    /**
     * Returns true if this accessory has completed its lifecycle and should be removed.
     *
     * @return True if expired.
     */
    default boolean isExpired() {
        return false;
    }

    /**
     * Returns the animation index to use for this accessory.
     *
     * @return animation index.
     */
    default int getAnimation() {
        return 0;
    }

    /**
     * Returns the animation ticks to use for this accessory.
     *
     * @param parent The model this accessory is attached to.
     * @return animation ticks.
     */
    default float getAnimationTicks(@NonNull Model parent) {
        return parent.getAnimationTicks();
    }

    /**
     * Provides the transform relative to the parent's position.
     *
     * @param dest The matrix to populate with the relative transform.
     * @param parent The model this accessory is attached to.
     */
    void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent);

    /**
     * Appends any particle emitters managed by this accessory to the destination collection.
     *
     * @param dest The collection to append emitters to.
     */
    default void addEmitters(@NonNull Collection<@NonNull Emitter<?>> dest) {
    }

    /**
     * Cleans up any resources (like active audio players/loops) when this accessory is removed.
     */
    @Override
    default void close() {
    }
}

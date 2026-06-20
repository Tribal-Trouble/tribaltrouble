package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Unified interface for accessing the visual state of renderable world objects.
 */
interface ModelState<M extends Model> extends LODObject {
    @NonNull
    Matrix4f getTransform(@NonNull Matrix4f dest);

    int getAnimation();

    float getAnimationTicks();

    @NonNull
    Color getTeamColor();

    @NonNull
    Color getSelectionColor();

    @NonNull
    Color getColor();

    Selectable.@NonNull VisualPattern getPattern();

    @Nullable
    M getModel();
}

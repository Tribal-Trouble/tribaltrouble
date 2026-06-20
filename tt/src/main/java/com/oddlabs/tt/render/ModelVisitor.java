package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Visitor interface for applying specific logic to different types of models during world visitation.
 */
abstract class ModelVisitor<M extends Model> {
    public void markDetailPoint(@NonNull ElementRenderState<M> render_state) {
        getSpriteKey(render_state).ifPresent(sprite -> render_state.getRenderer(sprite).addToNoDetailList(
                render_state));
    }

    public void markDetailPolygon(@NonNull ElementRenderState<M> render_state, @NonNull PolyDetail detail) {
        getSpriteKey(render_state).ifPresent(sprite -> {
            M model = render_state.model;
            render_state.getRenderer(sprite).addToRenderList(detail, render_state,
                    render_state.render_state.isResponding(model));
        });
    }

    public final int getTriangleCount(@NonNull ElementRenderState<M> render_state, @NonNull PolyDetail detail) {
        return getSpriteKey(render_state)
                .map(sprite -> render_state.getRenderer(sprite).getTriangleCount(detail))
                .orElse(0);
    }

    public final float getEyeDistanceSquared(@NonNull ElementRenderState<M> render_state) {
        M model = render_state.model;
        CameraState camera = render_state.render_state.getCamera();
        return RenderTools.getEyeDistanceSquared(model, camera.getCurrentX(), camera.getCurrentY(), camera
                .getCurrentZ());
    }

    public abstract @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<M> render_state);

    public abstract void getTransform(@NonNull ElementRenderState<M> render_state, @NonNull Matrix4f dest);

    public abstract @NonNull Color getTeamColor(@NonNull ElementRenderState<M> render_state);

    public abstract @NonNull Color getSelectionColor(@NonNull ElementRenderState<M> render_state);

    public abstract Selectable.@NonNull VisualPattern getPattern(@NonNull ElementRenderState<M> render_state);
}

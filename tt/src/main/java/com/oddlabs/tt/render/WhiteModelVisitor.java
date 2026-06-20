package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * A specialized {@link ModelVisitor} that renders models with a neutral white team color.
 * Used as a base for custom visitors or for temporary visual effects.
 */
class WhiteModelVisitor<M extends Model> extends ModelVisitor<M> {
    private static final WhiteModelVisitor<Model> INSTANCE = new WhiteModelVisitor<>();

    @SuppressWarnings("unchecked")
    @NonNull
    public static <M extends Model> WhiteModelVisitor<M> getInstance() {
        return (WhiteModelVisitor<M>) INSTANCE;
    }

    @Override
    public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<M> render_state) {
        return Optional.empty();
    }

    @Override
    public @NonNull Color getSelectionColor(@NonNull ElementRenderState<M> render_state) {
        return Color.Linear.WHITE;
    }

    @Override
    public @NonNull Color getTeamColor(@NonNull ElementRenderState<M> render_state) {
        return Color.Linear.WHITE;
    }

    @Override
    public Selectable.@NonNull VisualPattern getPattern(@NonNull ElementRenderState<M> render_state) {
        return Selectable.VisualPattern.NONE;
    }

    @Override
    public void getTransform(@NonNull ElementRenderState<M> render_state, @NonNull Matrix4f dest) {
        Model model = render_state.getModel();
        float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
        dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ()).rotate(angle, 0f, 0f, 1f);
    }
}

package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

class SelectableVisitor<S extends Selectable<?>> extends ModelVisitor<S> {
    private static final Vector4fc COLOR_RED = Color.argb4v(0xC0_FF_00_00);
    private static final Vector4fc COLOR_RED_HOVER = Color.argb4v(0xC0_7f_00_00);
    private static final Vector4fc COLOR_GREEN = Color.argb4v(0xC0_00_FF_00);
    private static final Vector4fc COLOR_GREEN_HOVER = Color.argb4v(0xC0_00_7F_00);
    private static final Vector4fc COLOR_BLUE = Color.argb4v(0xC0_00_00_FF);
    private static final Vector4fc COLOR_BLUE_HOVER = Color.argb4v(0xC0_00_00_7F);

    @Override
    public void getTransform(@NonNull ElementRenderState<S> render_state, @NonNull Matrix4f dest) {
        Model model = render_state.model;
        float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
        dest.translation(model.getPositionX(), model.getPositionY(), render_state.f).rotate(angle, 0f, 0f, 1f);
    }

    static @NonNull Vector4fc getTeamColor(@NonNull Selectable<?> model) {
        return model.getOwner().getColor();
    }

    @Override
    public final @NonNull Vector4fc getTeamColor(@NonNull ElementRenderState<S> render_state) {
        return getTeamColor(render_state.getModel());
    }

    @Override
    public final @NonNull Vector4fc getSelectionColor(@NonNull ElementRenderState<S> render_state) {
        Player local_player = render_state.render_state.getLocalPlayer();
        S model = render_state.getModel();
        return render_state.render_state.isSelected(
                model) ? model.getOwner() == local_player ? COLOR_GREEN : local_player.isEnemy(
                        model.getOwner()) ? COLOR_RED : COLOR_BLUE : render_state.render_state.isHovered(
                                model) ? model.getOwner() == local_player ? COLOR_GREEN_HOVER : local_player.isEnemy(
                                        model.getOwner()) ? COLOR_RED_HOVER : COLOR_BLUE_HOVER : model.getOwner()
                                                .getColor();
    }

    @Override
    public final void markDetailPoint(@NonNull ElementRenderState<S> render_state) {
        S selectable = render_state.model;
        if (!selectable.isDead())
            super.markDetailPoint(render_state);
    }
}

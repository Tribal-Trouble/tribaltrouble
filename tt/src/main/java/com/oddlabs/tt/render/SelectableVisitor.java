package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

class SelectableVisitor<S extends Selectable> extends ModelVisitor<S> {
	private static final float[] COLOR_RED = Color.rgb3f(0xFF0000);
	private static final float[] COLOR_RED_HOVER = Color.rgb3f(0x7f0000);
	private static final float[] COLOR_GREEN = Color.rgb3f(0x00FF00);
	private static final float[] COLOR_GREEN_HOVER = Color.rgb3f(0x007f00);
	private static final float[] COLOR_BLUE = Color.rgb3f(0x0000FF);
	private static final float[] COLOR_BLUE_HOVER = Color.rgb3f(0x00007f);

	@Override
	public final void transform(@NonNull ElementRenderState<S> render_state) {
		Model model = render_state.model;
		RenderTools.translateAndRotate(model.getPositionX(), model.getPositionY(), render_state.f, model.getDirectionX(), model.getDirectionY(), render_state.getModelViewStack());
	}

    @Override
    public void getTransform(@NonNull ElementRenderState<S> render_state, @NonNull Matrix4f dest) {
        Model model = render_state.model;
        float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
        dest.translation(model.getPositionX(), model.getPositionY(), render_state.f)
            .rotate(angle, 0f, 0f, 1f);
    }

	static float @NonNull [] getTeamColor(@NonNull Selectable<?> model) {
		return model.getOwner().getColor();
	}

	@NotNull
    @Override
	public final float[] getTeamColor(@NonNull ElementRenderState<S> render_state) {
		return getTeamColor(render_state.getModel());
	}

	@NotNull
    @Override
	public final float[] getSelectionColor(@NonNull ElementRenderState<S> render_state) {
		Player local_player = render_state.render_state.getLocalPlayer();
		S model = render_state.getModel();
        return render_state.render_state.isSelected(model)
                ? model.getOwner() == local_player
                    ? COLOR_GREEN
                    : local_player.isEnemy(model.getOwner())
                        ? COLOR_RED : COLOR_BLUE
                : render_state.render_state.isHovered(model)
                    ? model.getOwner() == local_player
                        ? COLOR_GREEN_HOVER
                        : local_player.isEnemy(model.getOwner())
                            ? COLOR_RED_HOVER : COLOR_BLUE_HOVER
                            : model.getOwner().getColor();
	}

	@Override
	public final void markDetailPoint(@NonNull ElementRenderState<S> render_state) {
		Selectable selectable = (Selectable)render_state.model;
		if (!selectable.isDead())
			super.markDetailPoint(render_state);
	}
}

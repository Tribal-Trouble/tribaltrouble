package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

class SelectableVisitor extends ModelVisitor {
	private static final float[] COLOR_RED = Color.rgb3f(0xFF0000);
	private static final float[] COLOR_RED_HOVER = Color.rgb3f(0x7f0000);
	private static final float[] COLOR_GREEN = Color.rgb3f(0x00FF00);
	private static final float[] COLOR_GREEN_HOVER = Color.rgb3f(0x007f00);
	private static final float[] COLOR_BLUE = Color.rgb3f(0x0000FF);
	private static final float[] COLOR_BLUE_HOVER = Color.rgb3f(0x00007f);

	@Override
	public final void transform(@NonNull ElementRenderState render_state) {
		Model model = render_state.model;
		RenderTools.translateAndRotate(model.getPositionX(), model.getPositionY(), render_state.f, model.getDirectionX(), model.getDirectionY());
	}

	static float[] getTeamColor(@NonNull Selectable model) {
		return model.getOwner().getColor();
	}

	@Override
	public final float[] getTeamColor(@NonNull ElementRenderState render_state) {
		return getTeamColor((Selectable)render_state.getModel());
	}

	@Override
	public final float[] getSelectionColor(@NonNull ElementRenderState render_state) {
		Player local_player = render_state.render_state.getLocalPlayer();
		Selectable model = (Selectable)render_state.getModel();
		if (render_state.render_state.isSelected(model)) {
			if (model.getOwner() == local_player)
				return COLOR_GREEN;
			else if (local_player.isEnemy(model.getOwner()))
				return COLOR_RED;
			else
				return COLOR_BLUE;
		} else if (render_state.render_state.isHovered(model)) {
			if (model.getOwner() == local_player)
				return COLOR_GREEN_HOVER;
			else if (local_player.isEnemy(model.getOwner()))
				return COLOR_RED_HOVER;
			else
				return COLOR_BLUE_HOVER;
		} else
			return model.getOwner().getColor();
	}

	@Override
	public final void markDetailPoint(@NonNull ElementRenderState render_state) {
		Selectable selectable = (Selectable)render_state.model;
		if (!selectable.isDead())
			super.markDetailPoint(render_state);
	}
}

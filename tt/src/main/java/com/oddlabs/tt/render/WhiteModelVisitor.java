package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.util.Color;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

abstract class WhiteModelVisitor<M extends Model> extends ModelVisitor<M> {
	private static final float[] COLOR_TEAM = Color.argb4f(Color.WHITE_INT);

	@NotNull
    @Override
	public final float@NonNull  [] getSelectionColor(@NotNull ElementRenderState<M> render_state) {
		return COLOR_TEAM;
	}

	@NotNull
    @Override
	public final float @NonNull [] getTeamColor(@NotNull ElementRenderState<M> render_state) {
		return COLOR_TEAM;
	}
}
